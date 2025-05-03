/*
 * SPDX-FileCopyrightText: 2023-2025 Paranoid Android
 * SPDX-License-Identifier: Apache-2.0
 */

package co.aospa.xiaomiparts.display

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.hardware.display.AmbientDisplayConfiguration
import android.os.Handler
import android.os.IBinder
import android.os.SystemProperties
import android.os.UserHandle
import android.provider.Settings
import android.util.Log
import androidx.core.os.postDelayed
import co.aospa.xiaomiparts.utils.dlog

/** Service to relay AOSP display color mode changes to xiaomi displayfeature. */
class ColorService : Service() {

    private val handler = Handler()
    private lateinit var ambientConfig: AmbientDisplayConfiguration
    private var isDozing = false

    private val settingObserver =
        object : ContentObserver(handler) {
            override fun onChange(selfChange: Boolean) {
                dlog(TAG, "SettingObserver: onChange")
                setCurrentColorMode()
            }
        }

    private val screenStateReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                dlog(TAG, "onReceive: ${intent.action}")
                when (intent.action) {
                    Intent.ACTION_SCREEN_ON -> {
                        if (isDozing) {
                            isDozing = false
                            handler.removeCallbacksAndMessages(null)
                            handler.postDelayed(100) {
                                dlog(TAG, "Was in AOD, restore color mode")
                                setCurrentColorMode()
                            }
                        }
                    }
                    Intent.ACTION_SCREEN_OFF -> {
                        if (!ambientConfig.alwaysOnEnabled(UserHandle.USER_CURRENT)) {
                            dlog(TAG, "AOD is not enabled")
                            isDozing = false
                            return
                        }
                        /**
                         * Use standard color mode in AOD to prevent black pixels from illuminating,
                         * thus reducing power consumption.
                         */
                        isDozing = true
                        handler.removeCallbacksAndMessages(null)
                        dlog(TAG, "Entered AOD, set color mode to standard")
                        ColorMode.STANDARD.setCurrent()
                    }
                }
            }
        }

    override fun onCreate() {
        super.onCreate()
        dlog(TAG, "onCreate")
        ambientConfig = AmbientDisplayConfiguration(this)
        contentResolver.registerContentObserver(
            Settings.System.getUriFor(Settings.System.DISPLAY_COLOR_MODE),
            false,
            settingObserver,
            UserHandle.USER_CURRENT,
        )
        val screenStateFilter =
            IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_SCREEN_OFF)
            }
        registerReceiver(screenStateReceiver, screenStateFilter)
        setCurrentColorMode()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        dlog(TAG, "onStartCommand")
        return START_STICKY
    }

    override fun onDestroy() {
        dlog(TAG, "onDestroy")
        contentResolver.unregisterContentObserver(settingObserver)
        unregisterReceiver(screenStateReceiver)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun setCurrentColorMode() {
        if (isDozing) {
            dlog(TAG, "setCurrentColorMode: skip in AOD")
            return
        }
        val colorMode =
            Settings.System.getIntForUser(
                contentResolver,
                Settings.System.DISPLAY_COLOR_MODE,
                DEFAULT_COLOR_MODE,
                UserHandle.USER_CURRENT,
            )
        val mode =
            ColorMode.fromId(colorMode)
                ?: run {
                    Log.e(TAG, "setCurrentColorMode: $colorMode is not in colorMap!")
                    return
                }
        dlog(TAG, "setCurrentColorMode: $mode")
        mode.setCurrent()
    }

    enum class ColorMode(
        val id: Int,
        val mode: Int,
        val value: Int,
        val cookie: Int,
        val isExpert: Boolean = false,
    ) {
        VIVID(258, 0, 2, 255),
        SATURATED(256, 1, 2, 255),
        STANDARD(257, 2, 2, 255),
        ORIGINAL(269, 26, 1, 0, true),
        P3(268, 26, 2, 0, true),
        SRGB(267, 26, 3, 0, true);

        fun setCurrent() {
            dlog(TAG, "set current mode $this")
            DisplayFeatureWrapper.setFeature(mode, value, cookie)
            if (isExpert) {
                DisplayFeatureWrapper.setFeature(EXPERT_MODE, EXPERT_VALUE, EXPERT_COOKIE)
            }
        }

        companion object {
            private const val EXPERT_MODE = 26
            private const val EXPERT_VALUE = 0
            private const val EXPERT_COOKIE = 10

            fun fromId(id: Int): ColorMode? {
                return values().find { it.id == id }
            }
        }
    }

    companion object {
        private const val TAG = "ColorService"

        private val DEFAULT_COLOR_MODE = SystemProperties.getInt("persist.sys.sf.native_mode", 0)

        fun startService(context: Context) {
            context.startServiceAsUser(
                Intent(context, ColorService::class.java),
                UserHandle.CURRENT,
            )
        }
    }
}
