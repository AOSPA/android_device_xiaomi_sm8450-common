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
import android.database.ContentObserver
import android.os.Handler
import android.os.IBinder
import android.os.PowerManager
import android.os.UserHandle
import android.provider.Settings
import android.util.Log
import co.aospa.xiaomiparts.utils.dlog

/* 
 * Service used to relay high touch polling rate setting to touch panel.
 */
class HighTouchPollingService : Service() {

    private var isEnabled = false
        set(value) {
            if (field == value) return
            field = value
            dlog(TAG, "isEnabled=$isEnabled")
            writeCurrentValue()
        }

    private var isPowerSaveMode = false
        set(value) {
            if (field == value) return
            field = value
            dlog(TAG, "isPowerSaveMode=$isEnabled")
            writeCurrentValue()
        }

    private lateinit var powerManager: PowerManager

    private val settingObserver = object : ContentObserver(Handler()) {
        override fun onChange(selfChange: Boolean) {
            isEnabled = Settings.Secure.getInt(contentResolver, SETTING_KEY, 0) == 1
        }
    }

    private val intentReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                PowerManager.ACTION_POWER_SAVE_MODE_CHANGED -> {
                    isPowerSaveMode = powerManager.isPowerSaveMode
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        dlog(TAG, "onCreate")
        powerManager = getSystemService(PowerManager::class.java)!!
        contentResolver.registerContentObserver(
            Settings.Secure.getUriFor(SETTING_KEY),
            false,
            settingObserver
        )
        registerReceiver(
            intentReceiver,
            IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
        )
        isPowerSaveMode = powerManager.isPowerSaveMode
        isEnabled = Settings.Secure.getInt(contentResolver, SETTING_KEY, 0) == 1
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        dlog(TAG, "onStartCommand")
        return START_STICKY
    }

    override fun onDestroy() {
        dlog(TAG, "onDestroy")
        contentResolver.unregisterContentObserver(settingObserver)
        unregisterReceiver(intentReceiver)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun writeCurrentValue() {
        dlog(TAG, "writeCurrentValue: isEnabled=$isEnabled isPowerSave=$isPowerSaveMode")
        TouchFeatureWrapper.setModeValue(
            MODE_TOUCH_REPORT_RATE,
            if (isEnabled && !isPowerSaveMode) 1 else 0
        )
    }

    companion object {
        private const val TAG = "HighTouchPollingService"
        private const val SETTING_KEY = "touch_polling_enabled"
        private const val MODE_TOUCH_REPORT_RATE = 9 // from kernel xiaomi_touch.h

        fun startService(context: Context) {
            context.startServiceAsUser(
                Intent(context, HighTouchPollingService::class.java),
                UserHandle.CURRENT
            )
        }
    }
}
