/*
 * Copyright (C) 2020 YAAP
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

package org.lineageos.settings.display;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

import androidx.preference.PreferenceManager;

import java.io.File;

public class FpsInfoTileService extends TileService {

    private boolean mIsShowing = false;
    private boolean mInternalStart = false;
    private boolean mReceiverRegistered;

    private final BroadcastReceiver mServiceStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (mInternalStart) {
                mInternalStart = false;
                return;
            }
            mIsShowing = intent.getBooleanExtra(FpsInfoService.EXTRA_FPS_STATE, false);
            updateTile();
        }
    };

    @Override
    public void onStartListening() {
        super.onStartListening();
        final Tile tile = getQsTile();

        if (!new File(FpsInfoService.FPS_PATH).exists()) {
            tile.setState(Tile.STATE_UNAVAILABLE);
            tile.updateTile();
            return;
        }
        mIsShowing = isRunning();
        updateTile();
        IntentFilter filter = new IntentFilter(FpsInfoService.ACTION_FPS_SERVICE_CHANGED);
        registerReceiver(mServiceStateReceiver, filter, Context.RECEIVER_EXPORTED);
        mReceiverRegistered = true;
    }

    @Override
    public void onStopListening() {
        super.onStopListening();
        if (mReceiverRegistered) {
            unregisterReceiver(mServiceStateReceiver);
            mReceiverRegistered = false;
        }
    }

    @Override
    public void onClick() {
        mInternalStart = true;
        Intent fpsinfo = new Intent(this, FpsInfoService.class);
        mIsShowing = isRunning();
        if (!mIsShowing) startService(fpsinfo);
        else stopService(fpsinfo);
        mIsShowing = !mIsShowing;
        updateTile();
    }

    private void updateTile() {
        final Tile tile = getQsTile();
        tile.setState(mIsShowing ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        tile.updateTile();
    }

    private boolean isRunning() {
        final SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        return sharedPrefs.getBoolean(FpsInfoService.PREF_KEY_FPS_STATE, false);
    }
}
