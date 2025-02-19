/*
* Copyright (C) 2023 Paranoid Android
*
* SPDX-License-Identifier: Apache-2.0
*/

package co.aospa.xiaomiparts.touch;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.display.AmbientDisplayConfiguration;
import android.os.IBinder;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Log;

import java.util.Arrays;

public class TouchNonUiService extends Service {

    private static final String TAG = "TouchNonUiService";

    // from kernel drivers/input/touchscreen/xiaomi/xiaomi_touch.h
    private static final int MODE_TOUCH_NONUI = 17;

    private static final int SENSOR_TYPE_NONUI = 33171027; // xiaomi.sensor.nonui

    private boolean mListening;
    private SensorManager mSensorManager;
    private Sensor mNonUiSensor;
    private AmbientDisplayConfiguration mAmbientConfig;

    private final SensorEventListener mSensorListener = new SensorEventListener() {
        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) { }

        @Override
        public void onSensorChanged(SensorEvent event) {
            dlog("onSensorChanged: values=" + Arrays.toString(event.values));
            TfWrapper.setModeValue(MODE_TOUCH_NONUI, Math.round(event.values[0]));
        }
    };

    private final BroadcastReceiver mScreenStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            dlog("onReceive: " + intent.getAction());
            switch (intent.getAction()) {
                case Intent.ACTION_SCREEN_ON:
                    if (mListening) {
                        mSensorManager.unregisterListener(mSensorListener, mNonUiSensor);
                        mListening = false;
                        dlog("stopped listening");

                        // ensure to reset nonui mode
                        TfWrapper.setModeValue(MODE_TOUCH_NONUI, 0);
                    }
                    break;
                case Intent.ACTION_SCREEN_OFF:
                    if (!mListening) {
                        final boolean doubleTapEnabled = Settings.System.getInt(
                                getContentResolver(), Settings.System.GESTURE_DOUBLE_TAP,
                                getResources().getInteger(
                                    com.android.internal.R.integer.config_doubleTapDefault)) > 0;
                        final boolean singleTapEnabled = Settings.System.getInt(
                                getContentResolver(), Settings.System.GESTURE_SINGLE_TAP,
                                getResources().getInteger(
                                    com.android.internal.R.integer.config_singleTapDefault)) > 0;
                        final boolean udfpsEnabled =
                                mAmbientConfig.screenOffUdfpsEnabled(UserHandle.myUserId());
                        dlog("doubleTapEnabled=" + doubleTapEnabled + " singleTapEnabled="
                                + singleTapEnabled + " udfpsEnabled=" + udfpsEnabled);

                        if (doubleTapEnabled || singleTapEnabled || udfpsEnabled) {
                            mSensorManager.registerListener(mSensorListener,
                                    mNonUiSensor, SensorManager.SENSOR_DELAY_NORMAL);
                            mListening = true;
                            dlog("started listening");
                        }
                    }
                    break;
            }
        }
    };

    public static void startService(Context context) {
        context.startServiceAsUser(new Intent(context, TouchNonUiService.class),
                UserHandle.CURRENT);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        dlog("Creating service");
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mNonUiSensor = mSensorManager.getDefaultSensor(SENSOR_TYPE_NONUI, true /* wakeUp */);
        if (mNonUiSensor == null) {
            Log.e(TAG, "failed to get nonui sensor, bailing!");
            stopSelf();
            return;
        }

        mAmbientConfig = new AmbientDisplayConfiguration(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        dlog("onStartCommand");
        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(mScreenStateReceiver, filter);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        dlog("onDestroy");
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private static void dlog(String msg) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, msg);
        }
    }

}
