/*
 * Copyright (C) 2018,2020 The LineageOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.lineageos.settings.dirac;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.AudioPlaybackConfiguration;
import android.media.AudioRecordingConfiguration;
import android.media.MediaRecorder;
import android.media.audiofx.AudioEffect;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.SystemClock;
import android.os.UserHandle;
import android.util.Log;
import android.view.KeyEvent;

import androidx.preference.PreferenceManager;

import java.util.List;
import java.util.concurrent.Executor;

/**
 * Owns the vendor sound effect. Saved preferences are the source of truth and
 * are reapplied whenever the effect is (re)created: at boot, after an
 * audioserver restart and when control returns from another client. All DSP
 * calls run on one worker thread, never on the UI or tile thread.
 * Recovery logic ported from PocoF3Releases sm8250-common MiSoundUtils.
 */
public final class DiracUtils {

    private static final String TAG = "DiracUtils";

    static final String PREF_ENABLE = "dirac_enable";
    static final String PREF_HEADSET = "dirac_headset_pref";
    static final String PREF_PRESET = "dirac_preset_pref";
    static final String PREF_SCENE = "scenario_selection";
    static final String PREF_HIFI = "dirac_hifi_pref";
    static final String PREF_EQ = "dirac_eq_enabled";
    static final String PREF_PAUSE = "dirac_pause_during_calls";
    // Set once the enable state has been taken over from the native library.
    private static final String PREF_MIGRATED = "dirac_state_migrated";

    private static final String FLAT = "0,0,0,0,0,0,0";
    private static final int BAND_COUNT = 7;
    private static final int MAX_ATTEMPTS = 5;
    private static final long RETRY_DELAY_MS = 1000;

    private static DiracUtils sInstance;

    private final Context mContext;
    private final SharedPreferences mPrefs;
    private final AudioManager mAudioManager;
    private final MediaSessionManager mMediaSessionManager;
    private final Handler mHandler;
    private final Handler mMainHandler = new Handler(Looper.getMainLooper());
    private final Executor mExecutor;

    // Worker-thread state.
    private DiracSound mDiracSound;
    private Boolean mAppliedEnabled;
    private boolean mServerDown;
    private boolean mCallbacksRegistered;
    private int mCallbackAttempts;
    private int mAttempts;
    private volatile boolean mCommunicationActive;

    private final Runnable mCallbackTask = this::registerCallbacks;
    private final Runnable mRestoreTask = this::restore;
    private final Runnable mAudioTask = this::updateAudioState;

    private final AudioManager.OnModeChangedListener mModeListener =
            mode -> scheduleAudioUpdate();

    private final AudioManager.AudioPlaybackCallback mPlaybackCallback =
            new AudioManager.AudioPlaybackCallback() {
                @Override
                public void onPlaybackConfigChanged(List<AudioPlaybackConfiguration> configs) {
                    scheduleAudioUpdate();
                }
            };

    private final AudioManager.AudioRecordingCallback mRecordingCallback =
            new AudioManager.AudioRecordingCallback() {
                @Override
                public void onRecordingConfigChanged(List<AudioRecordingConfiguration> configs) {
                    scheduleAudioUpdate();
                }
            };

    private final AudioManager.AudioServerStateCallback mServerCallback =
            new AudioManager.AudioServerStateCallback() {
                @Override
                public void onAudioServerDown() {
                    mServerDown = true;
                    mHandler.removeCallbacks(mRestoreTask);
                    mHandler.removeCallbacks(mAudioTask);
                    releaseEffect();
                }

                @Override
                public void onAudioServerUp() {
                    mServerDown = false;
                    mAttempts = 0;
                    restore();
                }
            };

    private DiracUtils(Context context) {
        mContext = context.getApplicationContext();
        mPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        mAudioManager = mContext.getSystemService(AudioManager.class);
        mMediaSessionManager = mContext.getSystemService(MediaSessionManager.class);
        HandlerThread worker = new HandlerThread("DiracSound");
        worker.start();
        mHandler = new Handler(worker.getLooper());
        mExecutor = mHandler::post;
        mHandler.post(mCallbackTask);
    }

    public static synchronized DiracUtils getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new DiracUtils(context);
        }
        return sInstance;
    }

    // ----- Public API (any thread; DSP work is queued in call order) -----

    public boolean isDiracEnabled() {
        return safeBoolean(PREF_ENABLE, false);
    }

    public int getHeadsetType() {
        return safeInt(PREF_HEADSET, 0, 0, 255);
    }

    public int getScenario() {
        return safeInt(PREF_SCENE, 4, 0, 4);
    }

    public String getPreset() {
        return getSavedPreset();
    }

    public boolean getHifiMode() {
        return safeBoolean(PREF_HIFI, false);
    }

    public boolean isEqualizerEnabled() {
        return safeBoolean(PREF_EQ, true);
    }

    public boolean isPauseDuringCallsEnabled() {
        return safeBoolean(PREF_PAUSE, true);
    }

    public boolean isPausedForCommunication() {
        return isDiracEnabled() && isPauseDuringCallsEnabled() && mCommunicationActive;
    }

    public void setEnabled(boolean enable) {
        mPrefs.edit().putBoolean(PREF_ENABLE, enable).apply();
        submit(() -> {
            requireEffect();
            applyEnabled(false);
            if (enable) {
                applySettings();
                applyEnabled(effectiveEnabled(true));
                mMainHandler.post(this::refreshPlaybackIfNecessary);
            }
        });
    }

    public void setLevel(String preset) {
        parseLevels(preset);
        mPrefs.edit().putString(PREF_PRESET, preset).apply();
        submit(() -> {
            requireEffect();
            applyLevel(preset, isEqualizerEnabled());
        });
    }

    public void setEqualizerEnabled(boolean enabled) {
        mPrefs.edit().putBoolean(PREF_EQ, enabled).apply();
        submit(() -> {
            requireEffect();
            applyLevel(getSavedPreset(), enabled);
        });
    }

    public void setPauseDuringCallsEnabled(boolean enabled) {
        mPrefs.edit().putBoolean(PREF_PAUSE, enabled).apply();
        submit(() -> {
            requireEffect();
            applyEnabled(effectiveEnabled(isDiracEnabled()));
        });
    }

    public void setHeadsetType(int type) {
        mPrefs.edit().putString(PREF_HEADSET, String.valueOf(type)).apply();
        submit(() -> requireEffect().setHeadsetType(type));
    }

    public void setHifiMode(int mode) {
        mPrefs.edit().putBoolean(PREF_HIFI, mode == 1).apply();
        submit(() -> {
            requireEffect();
            applyHifi(mode == 1);
        });
    }

    public void setScenario(int scene) {
        mPrefs.edit().putString(PREF_SCENE, String.valueOf(scene)).apply();
        submit(() -> requireEffect().setScenario(scene));
    }

    private void submit(Runnable action) {
        mHandler.post(() -> {
            try {
                action.run();
            } catch (RuntimeException e) {
                Log.w(TAG, "Cannot apply sound setting", e);
                restoreAfterFailure();
            }
        });
    }

    // ----- Worker thread -----

    private void registerCallbacks() {
        try {
            mAudioManager.setAudioServerStateCallback(mExecutor, mServerCallback);
            mAudioManager.addOnModeChangedListener(mExecutor, mModeListener);
            mAudioManager.registerAudioPlaybackCallback(mPlaybackCallback, mHandler);
            mAudioManager.registerAudioRecordingCallback(mRecordingCallback, mHandler);
            mCallbacksRegistered = true;
        } catch (RuntimeException e) {
            cleanup(() -> mAudioManager.removeOnModeChangedListener(mModeListener));
            cleanup(() -> mAudioManager.unregisterAudioPlaybackCallback(mPlaybackCallback));
            cleanup(() -> mAudioManager.unregisterAudioRecordingCallback(mRecordingCallback));
            cleanup(() -> mAudioManager.clearAudioServerStateCallback());
            Log.w(TAG, "Cannot register audio callbacks", e);
            if (++mCallbackAttempts < MAX_ATTEMPTS) {
                mHandler.postDelayed(mCallbackTask, RETRY_DELAY_MS);
            }
            releaseEffect();
            return;
        }
        restore();
    }

    private static void cleanup(Runnable action) {
        try {
            action.run();
        } catch (RuntimeException ignored) {
            // Best effort while unwinding a failed registration.
        }
    }

    private void scheduleAudioUpdate() {
        mHandler.removeCallbacks(mAudioTask);
        mHandler.post(mAudioTask);
    }

    private void updateAudioState() {
        if (mServerDown || (mDiracSound == null && mAttempts >= MAX_ATTEMPTS)) return;
        try {
            reconcileEnabledState();
        } catch (RuntimeException e) {
            Log.w(TAG, "Cannot update call pause", e);
            restoreAfterFailure();
        }
    }

    private boolean effectiveEnabled(boolean requested) {
        int mode = mAudioManager.getMode();
        boolean active = mode != AudioManager.MODE_NORMAL && mode != AudioManager.MODE_RINGTONE;
        if (!active) {
            for (AudioPlaybackConfiguration config
                    : mAudioManager.getActivePlaybackConfigurations()) {
                if (config.getPlayerState() == AudioPlaybackConfiguration.PLAYER_STATE_STARTED
                        && config.getAudioAttributes().getUsage()
                                == AudioAttributes.USAGE_VOICE_COMMUNICATION) {
                    active = true;
                    break;
                }
            }
        }
        if (!active) {
            for (AudioRecordingConfiguration config
                    : mAudioManager.getActiveRecordingConfigurations()) {
                if (!config.isClientSilenced() && config.getClientAudioSource()
                        == MediaRecorder.AudioSource.VOICE_COMMUNICATION) {
                    active = true;
                    break;
                }
            }
        }
        mCommunicationActive = active;
        return requested && !(isPauseDuringCallsEnabled() && active);
    }

    private void reconcileEnabledState() {
        DiracSound effect = requireEffect();
        boolean enabled = effectiveEnabled(isDiracEnabled());
        // Another client may have changed the native state behind our handle.
        if (mAppliedEnabled == null || mAppliedEnabled != enabled
                || !effect.hasExpectedEnabledState(enabled)
                || effect.getMusic() != (enabled ? 1 : 0)) {
            applyEnabled(enabled);
        }
    }

    private void restore() {
        try {
            reconcileEnabledState();
            mAttempts = 0;
            mHandler.removeCallbacks(mRestoreTask);
        } catch (RuntimeException e) {
            Log.w(TAG, "Cannot restore sound effect", e);
            if (++mAttempts < MAX_ATTEMPTS && !mServerDown) {
                mHandler.removeCallbacks(mRestoreTask);
                mHandler.postDelayed(mRestoreTask, RETRY_DELAY_MS);
            }
        }
    }

    private void restoreAfterFailure() {
        // Keep a handle that lost control: its listener resumes recovery.
        if (mDiracSound == null || mDiracSound.hasControl()) releaseEffect();
        mAttempts = 0;
        restore();
    }

    private void releaseEffect() {
        DiracSound previous = mDiracSound;
        mDiracSound = null;
        mAppliedEnabled = null;
        if (previous == null) return;
        try {
            if (!mServerDown && previous.hasControl()) {
                try {
                    previous.setEnabled(false);
                } finally {
                    previous.setMusic(0);
                }
            }
        } catch (RuntimeException e) {
            Log.w(TAG, "Cannot disable effect before release", e);
        } finally {
            try {
                previous.release();
            } catch (RuntimeException e) {
                Log.w(TAG, "Cannot release effect", e);
            }
        }
    }

    private DiracSound requireEffect() {
        if (!mCallbacksRegistered) throw new IllegalStateException("Audio callbacks unavailable");
        if (mServerDown) throw new IllegalStateException("Audio server is down");
        if (mDiracSound != null) {
            if (!mDiracSound.hasControl()) {
                throw new IllegalStateException("Effect control unavailable");
            }
            return mDiracSound;
        }
        try {
            releaseEffect();
            DiracSound effect = new DiracSound(0, 0);
            mDiracSound = effect;
            effect.setControlStatusListener((e, granted) -> mHandler.post(() -> {
                if (mDiracSound != effect || mServerDown) return;
                mAppliedEnabled = null;
                if (granted) {
                    // The other owner may have changed tuning too; start clean.
                    mAttempts = 0;
                    releaseEffect();
                    restore();
                }
            }));
            if (!effect.hasControl()) {
                throw new IllegalStateException("Effect control unavailable");
            }
            migrateEnabledState(effect);
            applyEnabled(false);
            if (isDiracEnabled()) {
                applySettings();
                applyEnabled(effectiveEnabled(true));
            }
            return effect;
        } catch (RuntimeException e) {
            if (mDiracSound == null || mDiracSound.hasControl()) releaseEffect();
            throw e;
        }
    }

    // Before this version the tile did not save its state; take it from the library once.
    private void migrateEnabledState(DiracSound effect) {
        if (mPrefs.getBoolean(PREF_MIGRATED, false)) return;
        boolean enabled;
        try {
            enabled = effect.getMusic() == 1;
        } catch (RuntimeException e) {
            Log.w(TAG, "Cannot read previous enable state", e);
            enabled = isDiracEnabled();
        }
        mPrefs.edit()
                .putBoolean(PREF_ENABLE, enabled)
                .putBoolean(PREF_MIGRATED, true)
                .apply();
    }

    private void applySettings() {
        mDiracSound.setHeadsetType(safeInt(PREF_HEADSET, 0, 0, 255));
        applyLevel(getSavedPreset(), isEqualizerEnabled());
        mDiracSound.setScenario(safeInt(PREF_SCENE, 4, 0, 4));
        applyHifi(getHifiMode());
    }

    private void applyEnabled(boolean enable) {
        DiracSound effect = mDiracSound;
        if (effect == null) throw new IllegalStateException("No effect");
        if (enable) {
            effect.setMusic(1);
            if (effect.setEnabled(true) != AudioEffect.SUCCESS) {
                throw new IllegalStateException("Enable failed");
            }
        } else {
            RuntimeException failure = null;
            try {
                if (effect.setEnabled(false) != AudioEffect.SUCCESS) {
                    throw new IllegalStateException("Disable failed");
                }
            } catch (RuntimeException e) {
                failure = e;
            }
            try {
                effect.setMusic(0);
            } catch (RuntimeException e) {
                if (failure == null) failure = e;
                else if (failure != e) failure.addSuppressed(e);
            }
            if (failure != null) throw failure;
        }
        if (!effect.hasExpectedEnabledState(enable) || effect.getMusic() != (enable ? 1 : 0)) {
            throw new IllegalStateException("Enable state did not apply");
        }
        mAppliedEnabled = enable;
    }

    private void applyLevel(String preset, boolean enabled) {
        float[] levels = enabled ? parseLevels(preset) : new float[BAND_COUNT];
        for (int band = 0; band < BAND_COUNT; band++) {
            mDiracSound.setLevel(band, levels[band]);
        }
    }

    private void applyHifi(boolean enabled) {
        mDiracSound.setHifiMode(enabled ? 1 : 0);
        mAudioManager.setParameters("hifi_mode=" + enabled);
    }

    // ----- Preferences -----

    private static float[] parseLevels(String preset) {
        String[] values = preset.split("\\s*,\\s*");
        if (values.length != BAND_COUNT) {
            throw new IllegalArgumentException("Expected " + BAND_COUNT + " bands: " + preset);
        }
        float[] levels = new float[BAND_COUNT];
        for (int i = 0; i < BAND_COUNT; i++) {
            levels[i] = Float.parseFloat(values[i].trim());
            if (Float.isNaN(levels[i]) || Float.isInfinite(levels[i])) {
                throw new IllegalArgumentException("Invalid level in " + preset);
            }
        }
        return levels;
    }

    private String getSavedPreset() {
        String preset = safeString(PREF_PRESET, FLAT);
        try {
            parseLevels(preset);
            return preset;
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "Discarding invalid saved preset", e);
            mPrefs.edit().remove(PREF_PRESET).apply();
            return FLAT;
        }
    }

    private String safeString(String key, String fallback) {
        try {
            String value = mPrefs.getString(key, fallback);
            return value != null ? value : fallback;
        } catch (ClassCastException e) {
            mPrefs.edit().remove(key).apply();
            return fallback;
        }
    }

    private boolean safeBoolean(String key, boolean fallback) {
        try {
            return mPrefs.getBoolean(key, fallback);
        } catch (ClassCastException e) {
            mPrefs.edit().remove(key).apply();
            return fallback;
        }
    }

    private int safeInt(String key, int fallback, int min, int max) {
        try {
            int value = Integer.parseInt(safeString(key, String.valueOf(fallback)));
            if (value >= min && value <= max) return value;
        } catch (NumberFormatException ignored) {
            // Fall through to the default.
        }
        mPrefs.edit().remove(key).apply();
        return fallback;
    }

    // ----- Playback refresh (main thread) -----

    private void triggerPlayPause(MediaController controller) {
        long when = SystemClock.uptimeMillis();
        final KeyEvent evDownPause = new KeyEvent(when, when, KeyEvent.ACTION_DOWN,
                KeyEvent.KEYCODE_MEDIA_PAUSE, 0);
        final KeyEvent evUpPause = KeyEvent.changeAction(evDownPause, KeyEvent.ACTION_UP);
        final KeyEvent evDownPlay = new KeyEvent(when, when, KeyEvent.ACTION_DOWN,
                KeyEvent.KEYCODE_MEDIA_PLAY, 0);
        final KeyEvent evUpPlay = KeyEvent.changeAction(evDownPlay, KeyEvent.ACTION_UP);
        mMainHandler.post(() -> controller.dispatchMediaButtonEvent(evDownPause));
        mMainHandler.postDelayed(() -> controller.dispatchMediaButtonEvent(evUpPause), 20);
        mMainHandler.postDelayed(() -> controller.dispatchMediaButtonEvent(evDownPlay), 1000);
        mMainHandler.postDelayed(() -> controller.dispatchMediaButtonEvent(evUpPlay), 1020);
    }

    private int getMediaControllerPlaybackState(MediaController controller) {
        if (controller != null) {
            final PlaybackState playbackState = controller.getPlaybackState();
            if (playbackState != null) {
                return playbackState.getState();
            }
        }
        return PlaybackState.STATE_NONE;
    }

    private void refreshPlaybackIfNecessary() {
        if (mMediaSessionManager == null) return;

        final List<MediaController> sessions =
                mMediaSessionManager.getActiveSessionsForUser(null, UserHandle.ALL);
        for (MediaController aController : sessions) {
            if (PlaybackState.STATE_PLAYING == getMediaControllerPlaybackState(aController)) {
                triggerPlayPause(aController);
                break;
            }
        }
    }
}
