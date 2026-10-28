/*
 * Copyright (C) 2018 The LineageOS Project
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

import android.media.audiofx.AudioEffect;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/** Client for vendor libmisoundfx (veux), parameter ids verified against its MiSound_command. */
public class DiracSound extends AudioEffect {

    private static final int DIRACSOUND_PARAM_HEADSET_TYPE = 1;
    private static final int DIRACSOUND_PARAM_EQ_LEVEL = 2;
    private static final int DIRACSOUND_PARAM_MUSIC = 4;
    private static final int DIRACSOUND_PARAM_HIFI = 8;
    private static final int DIRACSOUND_PARAM_SCENE = 15;
    // Native effect enable: issues EFFECT_CMD_ENABLE/DISABLE inside the library.
    private static final int DIRACSOUND_PARAM_EFFECT_ENABLE = 25;

    private static final UUID EFFECT_TYPE_DIRACSOUND =
            UUID.fromString("5b8e36a5-144a-4c38-b1d7-0002a5d5c51b");
    private static final String TAG = "DiracSound";

    public DiracSound(int priority, int audioSession) {
        super(EFFECT_TYPE_NULL, EFFECT_TYPE_DIRACSOUND, priority, audioSession);
    }

    @Override
    public boolean hasControl() {
        try {
            return super.hasControl();
        } catch (IllegalStateException e) {
            // Released or dead (audioserver restart) handle.
            return false;
        }
    }

    /**
     * Keeps the framework and native enable states together; a failed enable
     * is rolled back so the effect never stays half-enabled.
     */
    @Override
    public int setEnabled(boolean enabled) throws IllegalStateException {
        if (!enabled) {
            RuntimeException failure = null;
            try {
                checkStatus(super.setEnabled(false));
            } catch (RuntimeException e) {
                failure = e;
            }
            try {
                checkStatus(setParameter(DIRACSOUND_PARAM_EFFECT_ENABLE, 0));
            } catch (RuntimeException e) {
                failure = merge(failure, e);
            }
            if (failure != null) throw failure;
            return SUCCESS;
        }

        checkStatus(setParameter(DIRACSOUND_PARAM_EFFECT_ENABLE, 1));
        int status;
        try {
            status = super.setEnabled(true);
        } catch (RuntimeException e) {
            try {
                checkStatus(setParameter(DIRACSOUND_PARAM_EFFECT_ENABLE, 0));
            } catch (RuntimeException rollback) {
                merge(e, rollback);
            }
            throw e;
        }
        if (status != SUCCESS) {
            IllegalStateException failure =
                    new IllegalStateException("Framework enable failed: " + status);
            try {
                checkStatus(super.setEnabled(false));
            } catch (RuntimeException rollback) {
                merge(failure, rollback);
            }
            try {
                checkStatus(setParameter(DIRACSOUND_PARAM_EFFECT_ENABLE, 0));
            } catch (RuntimeException rollback) {
                merge(failure, rollback);
            }
            throw failure;
        }
        return SUCCESS;
    }

    private static RuntimeException merge(RuntimeException failure, RuntimeException other) {
        if (failure == null) return other;
        if (failure != other) failure.addSuppressed(other);
        return failure;
    }

    private int intParameter(int param) {
        int[] value = new int[1];
        int count = getParameter(param, value);
        checkStatus(count);
        if (count != 1) {
            throw new IllegalStateException("Incomplete parameter " + param + ": " + count);
        }
        return value[0];
    }

    @Override
    public boolean getEnabled() throws IllegalStateException {
        return intParameter(DIRACSOUND_PARAM_EFFECT_ENABLE) == 1 && super.getEnabled();
    }

    public boolean hasExpectedEnabledState(boolean enabled) {
        return intParameter(DIRACSOUND_PARAM_EFFECT_ENABLE) == (enabled ? 1 : 0)
                && super.getEnabled() == enabled;
    }

    public int getMusic() throws IllegalStateException,
            IllegalArgumentException, UnsupportedOperationException,
            RuntimeException {
        return intParameter(DIRACSOUND_PARAM_MUSIC);
    }

    public void setMusic(int enable) throws IllegalStateException,
            IllegalArgumentException, UnsupportedOperationException,
            RuntimeException {
        checkStatus(setParameter(DIRACSOUND_PARAM_MUSIC, enable));
    }

    public void setHeadsetType(int type) throws IllegalStateException,
            IllegalArgumentException, UnsupportedOperationException,
            RuntimeException {
        checkStatus(setParameter(DIRACSOUND_PARAM_HEADSET_TYPE, type));
    }

    public void setLevel(int band, float level) throws IllegalStateException,
            IllegalArgumentException, UnsupportedOperationException,
            RuntimeException {
        if (Float.isNaN(level) || Float.isInfinite(level)) {
            throw new IllegalArgumentException("Invalid level " + level);
        }
        // The library parses the value with strtof, so it must be a C string.
        checkStatus(setParameter(new int[]{DIRACSOUND_PARAM_EQ_LEVEL, band},
                (level + "\0").getBytes(StandardCharsets.US_ASCII)));
    }

    public void setHifiMode(int mode) throws IllegalStateException,
            IllegalArgumentException, UnsupportedOperationException,
            RuntimeException {
        checkStatus(setParameter(DIRACSOUND_PARAM_HIFI, mode));
    }

    public void setScenario(int scene) throws IllegalStateException,
            IllegalArgumentException, UnsupportedOperationException,
            RuntimeException {
        checkStatus(setParameter(DIRACSOUND_PARAM_SCENE, scene));
    }
}
