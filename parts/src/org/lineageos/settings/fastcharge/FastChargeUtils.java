/*
 * Copyright (C) 2025 KamiKaonashi
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.fastcharge;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.preference.PreferenceManager;

import org.lineageos.settings.utils.FileUtils;

public class FastChargeUtils {

    private static final String TAG = "FastChargeUtils";

    // veux kernel: drivers/misc/fastchgtoggle.c and drivers/misc/fastchg.c
    public static final String MODE_NODE = "/sys/kernel/fastchgtoggle/mode";
    public static final String USB_NODE = "/sys/kernel/fast_charge/force_fast_charge";

    public static final String KEY_MODE = "fastcharge_mode";
    public static final String KEY_USB = "fastcharge_usb";

    // 0 normal (no charge pump), 1 18W, 2 33W, 3 67W
    public static final String MODE_DEFAULT = "3";

    private final SharedPreferences mSharedPrefs;

    public FastChargeUtils(Context context) {
        mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public static boolean isModeSupported() {
        return FileUtils.isFileWritable(MODE_NODE);
    }

    public static boolean isUsbSupported() {
        return FileUtils.isFileWritable(USB_NODE);
    }

    public String getMode() {
        String value = FileUtils.readOneLine(MODE_NODE);
        return value != null ? value : mSharedPrefs.getString(KEY_MODE, MODE_DEFAULT);
    }

    public boolean isUsbEnabled() {
        return "1".equals(FileUtils.readOneLine(USB_NODE));
    }

    public boolean setMode(String mode) {
        if (!FileUtils.writeLine(MODE_NODE, mode)) {
            Log.e(TAG, "Failed to write charging mode " + mode);
            return false;
        }
        mSharedPrefs.edit().putString(KEY_MODE, mode).apply();
        return true;
    }

    public boolean setUsbEnabled(boolean enabled) {
        if (!FileUtils.writeLine(USB_NODE, enabled ? "1" : "0")) {
            Log.e(TAG, "Failed to write USB fast charge " + enabled);
            return false;
        }
        mSharedPrefs.edit().putBoolean(KEY_USB, enabled).apply();
        return true;
    }

    // Kernel nodes reset on every boot; reapply the saved choices.
    public static void restore(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (isModeSupported()) {
            FileUtils.writeLine(MODE_NODE, prefs.getString(KEY_MODE, MODE_DEFAULT));
        }
        if (isUsbSupported()) {
            FileUtils.writeLine(USB_NODE, prefs.getBoolean(KEY_USB, false) ? "1" : "0");
        }
    }
}
