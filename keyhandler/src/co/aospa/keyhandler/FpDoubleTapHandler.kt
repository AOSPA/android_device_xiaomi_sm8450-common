/*
 * SPDX-FileCopyrightText: 2023-2025 Paranoid Android
 * SPDX-License-Identifier: Apache-2.0
 */

package co.aospa.keyhandler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.UserHandle
import android.provider.Settings
import android.util.Log
import android.view.KeyEvent

class FpDoubleTapHandler(private val context: Context) {

    private var isEnabled = false
    private var action = 0
    private var screenOn = true
    private val screenOnRunnable = Runnable { screenOn = true }
    private val handler = Handler(Looper.getMainLooper())
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager

    private val settingsObserver =
        object : ContentObserver(handler) {
            override fun onChange(selfChange: Boolean) {
                dlog("SettingsObserver: onChange")
                updateSettings()
            }
        }

    private val screenStateReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                dlog("onReceive: ${intent.action}")
                when (intent.action) {
                    Intent.ACTION_SCREEN_OFF -> {
                        handler.removeCallbacks(screenOnRunnable)
                        screenOn = false
                    }
                    Intent.ACTION_USER_PRESENT -> {
                        handler.postDelayed(screenOnRunnable, UNLOCK_WAIT_MS)
                    }
                }
            }
        }

    private var registerReceiver = false
        set(value) {
            if (field == value) return
            field = value
            if (value) {
                context.registerReceiver(
                    screenStateReceiver,
                    IntentFilter().apply {
                        addAction(Intent.ACTION_USER_PRESENT)
                        addAction(Intent.ACTION_SCREEN_OFF)
                    }
                )
                dlog("Registered screen state receiver")
            } else {
                context.unregisterReceiver(screenStateReceiver)
                dlog("Unregistered screen state receiver")
            }
        }

    init {
        context.contentResolver.registerContentObserver(
            Settings.System.getUriFor(SETTING_KEY_ENABLE),
            false,
            settingsObserver,
            UserHandle.USER_CURRENT
        )
        context.contentResolver.registerContentObserver(
            Settings.System.getUriFor(SETTING_KEY_ACTION),
            false,
            settingsObserver,
            UserHandle.USER_CURRENT
        )
        updateSettings()
    }

    fun handleEvent(event: KeyEvent) {
        val interactive = powerManager.isInteractive()
        dlog(
            "handleEvent: isEnabled=$isEnabled action=$action " +
                "screenOn=$screenOn interactive=$interactive"
        )
        if (!screenOn || !isEnabled || !interactive || event.action != KeyEvent.ACTION_UP) {
            dlog("wont handle")
            return
        }
        context.sendBroadcastAsUser(
            Intent(ACTION_GESTURE_TRIGGER).apply {
                putExtra(EXTRA_GESTURE_ACTION_ID, this@FpDoubleTapHandler.action)
                setPackage(PACKAGE_XIAOMI_PARTS)
            },
            UserHandle.CURRENT
        )
    }

    private fun updateSettings() {
        isEnabled =
            Settings.System.getIntForUser(
                context.contentResolver,
                SETTING_KEY_ENABLE,
                0,
                UserHandle.USER_CURRENT
            ) == 1
        action =
            Settings.System.getIntForUser(
                context.contentResolver,
                SETTING_KEY_ACTION,
                1,
                UserHandle.USER_CURRENT
            )
        dlog("updateSettings: isEnabled=$isEnabled action=$action")
        registerReceiver = isEnabled
    }

    private companion object {
        const val TAG = "FpDoubleTapHandler"
        const val SETTING_KEY_ENABLE = "fp_double_tap_enable"
        const val SETTING_KEY_ACTION = "fp_double_tap_action"
        const val UNLOCK_WAIT_MS = 1000L
        const val ACTION_GESTURE_TRIGGER = "co.aospa.xiaomiparts.ACTION_GESTURE_TRIGGER"
        const val EXTRA_GESTURE_ACTION_ID = "action_id"
        const val PACKAGE_XIAOMI_PARTS = "co.aospa.xiaomiparts"

        fun dlog(msg: String) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, msg)
            }
        }
    }
}
