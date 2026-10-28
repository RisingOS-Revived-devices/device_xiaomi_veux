/*
 * Copyright (C) 2018 The LineageOS Project
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.display;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import org.lineageos.settings.utils.FileUtils;

import java.io.File;

/** DC dimming state shared by the dashboard, the QS tile and the boot receiver. */
public final class DcDimmingUtils {

    public static final String DC_DIMMING_ENABLE_KEY = "dc_dimming_enable";
    public static final String DC_DIMMING_NODE =
            "/sys/devices/platform/soc/soc:qcom,dsi-display-primary/dimlayer_exposure";
    // Panel HBM node that DC dimming forces off and locks while it is active.
    private static final String PANEL_HBM =
            "/sys/devices/platform/soc/soc:qcom,dsi-display-primary/hbm";
    private static final String HBM_KEY = "hbm";

    private DcDimmingUtils() {}

    public static boolean isSupported() {
        return FileUtils.fileExists(DC_DIMMING_NODE);
    }

    public static boolean isEnabled(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(DC_DIMMING_ENABLE_KEY, false);
    }

    public static void setEnabled(Context context, boolean enabled) {
        FileUtils.writeLine(DC_DIMMING_NODE, enabled ? "1" : "0");
        if (enabled) {
            disableHbm(context);
        } else {
            new File(PANEL_HBM).setWritable(true);
        }
        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putBoolean(DC_DIMMING_ENABLE_KEY, enabled).apply();
    }

    static void disableHbm(Context context) {
        FileUtils.writeLine(PANEL_HBM, "0");
        new File(PANEL_HBM).setReadOnly();
        SharedPreferences.Editor editor =
                PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putBoolean(HBM_KEY, false).apply();
    }
}
