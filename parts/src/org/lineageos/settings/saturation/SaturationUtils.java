/*
 * Copyright (C) 2024 The LineageOS Project
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.saturation;

import android.content.Context;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;

import androidx.preference.PreferenceManager;

import org.lineageos.settings.Constants;

/** Display saturation through SurfaceFlinger, shared by the dashboard and Startup. */
public final class SaturationUtils {

    private static final String TAG = "SaturationUtils";
    public static final int MIN = 0;
    public static final int MAX = 200;
    public static final int DEFAULT = 100;

    private SaturationUtils() {}

    public static int get(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getInt(Constants.KEY_SATURATION, DEFAULT);
    }

    public static void set(Context context, int value) {
        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putInt(Constants.KEY_SATURATION, value).apply();
        apply(value);
    }

    public static void restore(Context context) {
        apply(get(context));
    }

    public static void apply(int value) {
        // 1.0 is treated as "no change" by SurfaceFlinger, so 100 % is sent as 1.001.
        float saturation = value == 100 ? 1.001f : value / 100.0f;
        IBinder surfaceFlinger = ServiceManager.getService("SurfaceFlinger");
        if (surfaceFlinger == null) {
            Log.e(TAG, "SurfaceFlinger service not found");
            return;
        }
        Parcel data = Parcel.obtain();
        try {
            data.writeInterfaceToken("android.ui.ISurfaceComposer");
            data.writeFloat(saturation);
            surfaceFlinger.transact(1022, data, null, 0);
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to apply saturation", e);
        } finally {
            data.recycle();
        }
    }
}
