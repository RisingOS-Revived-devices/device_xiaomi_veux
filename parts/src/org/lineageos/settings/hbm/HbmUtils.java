/*
 * Copyright (C) 2016 The OmniROM Project
 *               2018-2021 crDroid Android Project
 *               2019-2022 Evolution X
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.hbm;

import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;

import androidx.preference.PreferenceManager;

import org.lineageos.settings.display.DcDimmingUtils;
import org.lineageos.settings.utils.FileUtils;

/** HBM and Auto HBM state shared by the dashboard, the QS tile and AutoHBMService. */
public final class HbmUtils {

    public static final String KEY_HBM_SWITCH = "hbm";
    public static final String KEY_AUTO_HBM_SWITCH = "auto_hbm";
    // Stored as strings: AutoHBMService parses them with getString().
    public static final String KEY_AUTO_HBM_THRESHOLD = "auto_hbm_threshold";
    public static final String KEY_HBM_DISABLE_TIME = "hbm_disable_time";
    private static final String KEY_LAST_BRIGHTNESS = "last_brightness";

    public static final int THRESHOLD_MIN = 0;
    public static final int THRESHOLD_MAX = 60000;
    public static final int THRESHOLD_STEP = 1000;
    public static final int THRESHOLD_DEFAULT = 20000;
    public static final int DISABLE_TIME_MIN = 2;
    public static final int DISABLE_TIME_MAX = 15;
    public static final int DISABLE_TIME_DEFAULT = 5;

    private static final String HBM =
            "/sys/devices/platform/soc/5e00000.qcom,mdss_mdp/drm/card0/card0-DSI-1/hbm";
    private static final String BACKLIGHT = "/sys/class/backlight/panel0-backlight/brightness";

    private HbmUtils() {}

    private static SharedPreferences prefs(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    public static boolean isSupported() {
        return FileUtils.isFileWritable(HBM);
    }

    public static boolean isHbmEnabled(Context context) {
        return prefs(context).getBoolean(KEY_HBM_SWITCH, false);
    }

    /** Returns false (and changes nothing) while DC dimming is on. */
    public static boolean setHbmEnabled(Context context, boolean enabled) {
        if (DcDimmingUtils.isEnabled(context)) {
            return false;
        }
        SharedPreferences sharedPrefs = prefs(context);
        if (enabled) {
            int currentBrightness = Settings.System.getInt(context.getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS, 128);
            sharedPrefs.edit().putInt(KEY_LAST_BRIGHTNESS, currentBrightness).apply();
            FileUtils.writeLine(HBM, "1");
            FileUtils.writeLine(BACKLIGHT, "2047");
            Settings.System.putInt(context.getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS, 255);
        } else {
            FileUtils.writeLine(HBM, "0");
            int lastBrightness = sharedPrefs.getInt(KEY_LAST_BRIGHTNESS, 128);
            Settings.System.putInt(context.getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS, lastBrightness);
        }
        sharedPrefs.edit().putBoolean(KEY_HBM_SWITCH, enabled).commit();
        return true;
    }

    public static boolean isAutoHbmEnabled(Context context) {
        return prefs(context).getBoolean(KEY_AUTO_HBM_SWITCH, false);
    }

    public static void setAutoHbmEnabled(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_AUTO_HBM_SWITCH, enabled).commit();
        FileUtils.enableService(context);
    }

    public static int getThreshold(Context context) {
        return readInt(context, KEY_AUTO_HBM_THRESHOLD, THRESHOLD_DEFAULT);
    }

    public static void setThreshold(Context context, int lux) {
        prefs(context).edit().putString(KEY_AUTO_HBM_THRESHOLD, String.valueOf(lux)).commit();
    }

    public static int getDisableTime(Context context) {
        return readInt(context, KEY_HBM_DISABLE_TIME, DISABLE_TIME_DEFAULT);
    }

    public static void setDisableTime(Context context, int seconds) {
        prefs(context).edit().putString(KEY_HBM_DISABLE_TIME, String.valueOf(seconds)).commit();
    }

    private static int readInt(Context context, String key, int defaultValue) {
        try {
            return Integer.parseInt(prefs(context).getString(key, String.valueOf(defaultValue)));
        } catch (NumberFormatException | ClassCastException e) {
            return defaultValue;
        }
    }
}
