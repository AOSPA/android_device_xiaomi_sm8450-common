/*
 * SPDX-FileCopyrightText: 2022-2025 Paranoid Android
 * SPDX-License-Identifier: Apache-2.0
 */

package co.aospa.xiaomiparts.doze

import android.app.KeyguardManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import android.os.UserHandle
import android.util.Log
import co.aospa.xiaomiparts.utils.dlog

/**
 * This service turns off the screen if accidental touch is detected on lockscreen, based on
 * xiaomi's large-area-touch sensor reading. This prevents accidental triggering of lockscreen
 * controls in pocket.
 */
class PocketService : Service() {

    private lateinit var powerManager: PowerManager
    private lateinit var keyguardManager: KeyguardManager
    private lateinit var sensorManager: SensorManager
    private var touchSensor: Sensor? = null
    private var userPresent = false

    private val screenStateReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.action) {
                    Intent.ACTION_SCREEN_ON -> {
                        dlog(TAG, "Received ACTION_SCREEN_ON userPresent=$userPresent")
                        if (userPresent) return
                        sensorManager.registerListener(
                            sensorListener,
                            touchSensor,
                            SensorManager.SENSOR_DELAY_NORMAL,
                        )
                    }
                    Intent.ACTION_SCREEN_OFF -> {
                        dlog(TAG, "Received ACTION_SCREEN_OFF")
                        sensorManager.unregisterListener(sensorListener, touchSensor)
                        userPresent = false
                    }
                    Intent.ACTION_USER_PRESENT -> {
                        dlog(TAG, "Received ACTION_USER_PRESENT")
                        sensorManager.unregisterListener(sensorListener, touchSensor)
                        userPresent = true
                    }
                }
            }
        }

    private val sensorListener =
        object : SensorEventListener {
            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}

            override fun onSensorChanged(event: SensorEvent) {
                val isTouchDetected = event.values[0] == 1f
                val isOnKeyguard = keyguardManager.isKeyguardLocked

                dlog(
                    TAG,
                    "onSensorChanged type=${event.sensor.type} value=${event.values[0]} " +
                        "isTouchDetected=$isTouchDetected isOnKeyguard=$isOnKeyguard",
                )

                if (isTouchDetected && isOnKeyguard) {
                    Log.i(TAG, "In pocket, going to sleep")
                    powerManager.goToSleep(SystemClock.uptimeMillis())
                }
            }
        }

    override fun onCreate() {
        dlog(TAG, "Creating service")
        powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        dlog(TAG, "Starting service")
        touchSensor =
            sensorManager.getDefaultSensor(TYPE_LARGE_AREA_TOUCH_SENSOR)
                ?: run {
                    dlog(TAG, "LARGE_AREA_TOUCH_SENSOR not found, stopping service")
                    stopSelf()
                    return START_NOT_STICKY
                }
        registerReceiver(
            screenStateReceiver,
            IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_USER_PRESENT)
            },
        )
        return START_STICKY
    }

    override fun onDestroy() {
        dlog(TAG, "Destroying service")
        if (touchSensor != null) {
            unregisterReceiver(screenStateReceiver)
            sensorManager.unregisterListener(sensorListener, touchSensor)
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "PocketService"
        private const val TYPE_LARGE_AREA_TOUCH_SENSOR = 33171031

        fun startService(context: Context) {
            context.startServiceAsUser(
                Intent(context, PocketService::class.java),
                UserHandle.CURRENT,
            )
        }
    }
}
