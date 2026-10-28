/*
 * Copyright (C) 2019 The OmniROM Project
 *               2020 YAAP
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

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.service.dreams.DreamService;
import android.service.dreams.IDreamManager;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.util.Log;

import androidx.preference.PreferenceManager;

import java.io.BufferedReader;
import java.io.FileReader;
import java.lang.Math;

public class FpsInfoService extends Service {

    // veux: sde crtc exposes "fps: X.Y duration:.. frame_count:.." on this node.
    public static final String FPS_PATH = "/sys/class/drm/card0/sde-crtc-0/measured_fps";
    public static final String PREF_KEY_FPS_STATE = "fps_overlay_running";
    public static final String ACTION_FPS_SERVICE_CHANGED =
            "org.lineageos.settings.display.FPS_SERVICE_CHANGED";
    public static final String EXTRA_FPS_STATE = "fps_state";

    private View mView;
    private Thread mCurFPSThread;
    private final String TAG = "FpsInfoService";
    private String mFps = null;

    private IDreamManager mDreamManager;

    private class FPSView extends View {
        private final Paint mOnlinePaint;
        private final float mAscent;
        private final int mMaxWidth;

        private int mNeededWidth;
        private int mNeededHeight;
        private boolean mDataAvail;

        private final Handler mCurFPSHandler = new Handler(Looper.getMainLooper()) {
            public void handleMessage(Message msg) {
                if (msg.obj == null) return;
                if (msg.what == 1) {
                    String msgData = (String) msg.obj;
                    // node line: "fps: 59.4 duration:.." -> first 9 chars = "fps: 59.4"
                    msgData = msgData.substring(0, Math.min(msgData.length(), 9));
                    mFps = msgData;
                    mDataAvail = true;
                    updateDisplay();
                }
            }
        };

        FPSView(Context c) {
            super(c);
            final float density = c.getResources().getDisplayMetrics().density;
            final int paddingPx = Math.round(5 * density);
            setPadding(paddingPx, paddingPx, paddingPx, paddingPx);
            setBackgroundColor(Color.argb(0x60, 0, 0, 0));

            final int textSize = Math.round(12 * density);

            Typeface typeface = Typeface.create("monospace", Typeface.NORMAL);

            mOnlinePaint = new Paint();
            mOnlinePaint.setTypeface(typeface);
            mOnlinePaint.setAntiAlias(true);
            mOnlinePaint.setTextSize(textSize);
            mOnlinePaint.setColor(Color.WHITE);
            mOnlinePaint.setShadowLayer(5.0f, 0.0f, 0.0f, Color.BLACK);

            mAscent = mOnlinePaint.ascent();

            final String maxWidthStr = "fps: 60.1";
            mMaxWidth = (int) mOnlinePaint.measureText(maxWidthStr);

            updateDisplay();
        }

        @Override
        protected void onDetachedFromWindow() {
            super.onDetachedFromWindow();
            mCurFPSHandler.removeMessages(1);
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            setMeasuredDimension(resolveSize(mNeededWidth, widthMeasureSpec),
                    resolveSize(mNeededHeight, heightMeasureSpec));
        }

        @Override
        public void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            if (!mDataAvail || mFps == null) {
                return;
            }
            final int LEFT = getWidth() - 1;
            final int y = mPaddingTop - (int) mAscent;
            canvas.drawText(mFps, LEFT - mPaddingLeft - mMaxWidth, y - 1, mOnlinePaint);
        }

        void updateDisplay() {
            if (!mDataAvail) {
                return;
            }
            int neededWidth = mPaddingLeft + mPaddingRight + mMaxWidth;
            int neededHeight = mPaddingTop + mPaddingBottom + 40;
            if (neededWidth != mNeededWidth || neededHeight != mNeededHeight) {
                mNeededWidth = neededWidth;
                mNeededHeight = neededHeight;
                requestLayout();
            } else {
                invalidate();
            }
        }

        Handler getHandler2() {
            return mCurFPSHandler;
        }
    }

    protected static class CurFPSThread extends Thread {
        private boolean mInterrupt = false;
        private final Handler mHandler;

        CurFPSThread(Handler handler) {
            mHandler = handler;
        }

        public void interrupt() {
            mInterrupt = true;
        }

        @Override
        public void run() {
            try {
                while (!mInterrupt) {
                    sleep(1000);
                    String fpsVal = readOneLine(FPS_PATH);
                    mHandler.sendMessage(mHandler.obtainMessage(1, fpsVal));
                }
            } catch (InterruptedException ignored) { }
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mView = new FPSView(this);
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_SECURE_SYSTEM_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.END | Gravity.TOP;
        params.y = 50;
        params.setTitle("FPS Info");

        startThread();

        mDreamManager = IDreamManager.Stub.asInterface(
                ServiceManager.checkService(DreamService.DREAM_SERVICE));
        IntentFilter screenStateFilter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        screenStateFilter.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(mScreenStateReceiver, screenStateFilter, Context.RECEIVER_EXPORTED);

        WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        wm.addView(mView, params);

        final SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPrefs.edit().putBoolean(PREF_KEY_FPS_STATE, true).commit();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopThread();
        ((WindowManager) getSystemService(WINDOW_SERVICE)).removeView(mView);
        mView = null;
        unregisterReceiver(mScreenStateReceiver);

        final SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPrefs.edit().remove(PREF_KEY_FPS_STATE).commit();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private static String readOneLine(String path) {
        BufferedReader br;
        String line;
        try {
            br = new BufferedReader(new FileReader(path), 512);
            try {
                line = br.readLine();
            } finally {
                br.close();
            }
        } catch (Exception e) {
            return null;
        }
        return line;
    }

    private final BroadcastReceiver mScreenStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
                if (!isDozeMode()) {
                    startThread();
                    mView.setVisibility(View.VISIBLE);
                }
            } else if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                mView.setVisibility(View.GONE);
                stopThread();
            }
        }
    };

    private boolean isDozeMode() {
        try {
            if (mDreamManager != null && mDreamManager.isDreaming()) {
                return true;
            }
        } catch (RemoteException e) {
            return false;
        }
        return false;
    }

    private void startThread() {
        mCurFPSThread = new CurFPSThread(((FPSView) mView).getHandler2());
        mCurFPSThread.start();
        broadcastServiceState(true);
    }

    private void stopThread() {
        if (mCurFPSThread != null && mCurFPSThread.isAlive()) {
            mCurFPSThread.interrupt();
            try {
                mCurFPSThread.join();
            } catch (InterruptedException ignored) { }
        }
        mCurFPSThread = null;
        broadcastServiceState(false);
    }

    private void broadcastServiceState(boolean started) {
        Intent intent = new Intent(ACTION_FPS_SERVICE_CHANGED);
        intent.putExtra(EXTRA_FPS_STATE, started);
        intent.setFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY);
        sendBroadcastAsUser(intent, UserHandle.CURRENT);
    }
}
