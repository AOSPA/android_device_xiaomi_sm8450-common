/*
 * SPDX-FileCopyrightText: 2023-2025 Paranoid Android
 * SPDX-License-Identifier: Apache-2.0
 */

package co.aospa.xiaomiparts.touch

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.display.AmbientDisplayConfiguration
import android.os.IBinder
import android.os.UserHandle
import android.provider.Settings
import android.util.Log
import co.aospa.xiaomiparts.utils.dlog
import kotlin.math.roundToInt

/**
 * This service relays xiaomi nonui sensor readings to the touchscreen, when the screen is turned
 * off. The values reported by this sensor is used by the touchscreen driver to turn off gestures
 * such as tap-to-wake, when the device is in pocket.
 */
class TouchNonUiService : Service() {

    private var listening = false
    private lateinit var sensorManager: SensorManager
    private var nonUiSensor: Sensor? = null
    private lateinit var ambientConfig: AmbientDisplayConfiguration

    private val sensorListener =
        object : SensorEventListener {
            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}

            override fun onSensorChanged(event: SensorEvent) {
                dlog(TAG, "onSensorChanged: values=${event.values.joinToString()}")
                TouchFeatureWrapper.setModeValue(MODE_TOUCH_NONUI, event.values[0].roundToInt())
            }
        }

    private val screenStateReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                dlog(TAG, "onReceive: ${intent.action}")
                when (intent.action) {
                    Intent.ACTION_SCREEN_ON -> {
                        if (!listening) return
                        sensorManager.unregisterListener(sensorListener, nonUiSensor)
                        listening = false
                        dlog(TAG, "stopped listening")
                        // ensure to reset nonui mode
                        TouchFeatureWrapper.setModeValue(MODE_TOUCH_NONUI, 0)
                    }
                    Intent.ACTION_SCREEN_OFF -> {
                        if (listening) return
                        val doubleTapEnabled =
                            Settings.System.getInt(
                                contentResolver,
                                Settings.System.GESTURE_DOUBLE_TAP,
                                resources.getInteger(
                                    com.android.internal.R.integer.config_doubleTapDefault
                                ),
                            ) > 0
                        val singleTapEnabled =
                            Settings.System.getInt(
                                contentResolver,
                                Settings.System.GESTURE_SINGLE_TAP,
                                resources.getInteger(
                                    com.android.internal.R.integer.config_singleTapDefault
                                ),
                            ) > 0
                        val udfpsEnabled =
                            ambientConfig.screenOffUdfpsEnabled(UserHandle.myUserId())
                        dlog(
                            TAG,
                            "doubleTapEnabled=$doubleTapEnabled singleTapEnabled=$singleTapEnabled " +
                                "udfpsEnabled=$udfpsEnabled",
                        )
                        if (doubleTapEnabled || singleTapEnabled || udfpsEnabled) {
                            sensorManager.registerListener(
                                sensorListener,
                                nonUiSensor,
                                SensorManager.SENSOR_DELAY_NORMAL,
                            )
                            listening = true
                            dlog(TAG, "started listening")
                        }
                    }
                }
            }
        }

    override fun onCreate() {
        super.onCreate()
        dlog(TAG, "Creating service")
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        ambientConfig = AmbientDisplayConfiguration(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        dlog(TAG, "onStartCommand")
        nonUiSensor =
            sensorManager.getDefaultSensor(SENSOR_TYPE_NONUI, true)
                ?: run {
                    Log.e(TAG, "Failed to get nonui sensor, bailing!")
                    stopSelf()
                    return START_NOT_STICKY
                }
        registerReceiver(
            screenStateReceiver,
            IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_SCREEN_OFF)
            },
        )
        return START_STICKY
    }

    override fun onDestroy() {
        dlog(TAG, "onDestroy")
        runCatching { unregisterReceiver(screenStateReceiver) }
        if (listening) sensorManager.unregisterListener(sensorListener, nonUiSensor)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "TouchNonUiService"
        private const val MODE_TOUCH_NONUI = 17 // from kernel xiaomi_touch.h
        private const val SENSOR_TYPE_NONUI = 33171027 // xiaomi.sensor.nonui

        fun startService(context: Context) {
            context.startServiceAsUser(
                Intent(context, TouchNonUiService::class.java),
                UserHandle.CURRENT,
            )
        }
    }
}
