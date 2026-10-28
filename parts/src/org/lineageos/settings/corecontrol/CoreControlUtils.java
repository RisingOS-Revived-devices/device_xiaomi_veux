/*
 * Copyright (C) 2025 The LineageOS Project
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.corecontrol;

import android.util.Log;

import java.io.File;
import java.nio.file.Files;

/** CPU core hotplug through /sys/devices/system/cpu/cpuN/online. */
public final class CoreControlUtils {

    private static final String TAG = "CoreControlUtils";
    public static final int NUM_CORES = 8;
    // Cores 0-5 are the little cluster; two of them must stay online.
    private static final int LAST_LITTLE_CORE = 5;
    private static final int MIN_LITTLE_ONLINE = 2;

    private CoreControlUtils() {}

    private static String path(int core) {
        return "/sys/devices/system/cpu/cpu" + core + "/online";
    }

    public static boolean isCoreOnline(int core) {
        return new File(path(core)).exists() && "1".equals(read(path(core)));
    }

    public static boolean canOffline(int core) {
        if (core < 0 || core > LAST_LITTLE_CORE) {
            return true;
        }
        int onlineCount = 0;
        for (int i = 0; i <= LAST_LITTLE_CORE; i++) {
            if (i != core && isCoreOnline(i)) onlineCount++;
        }
        return onlineCount >= MIN_LITTLE_ONLINE;
    }

    public static void setCoreState(int core, boolean online) {
        try {
            Files.write(new File(path(core)).toPath(), (online ? "1" : "0").getBytes());
        } catch (Exception e) {
            Log.e(TAG, "Failed to write " + path(core), e);
        }
    }

    private static String read(String path) {
        try {
            return new String(Files.readAllBytes(new File(path).toPath())).trim();
        } catch (Exception e) {
            Log.e(TAG, "Failed to read " + path, e);
            return "";
        }
    }
}
