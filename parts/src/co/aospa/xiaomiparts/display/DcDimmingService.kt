/*
 * SPDX-FileCopyrightText: 2023-2025 Paranoid Android
 * SPDX-License-Identifier: Apache-2.0
 */

package co.aospa.xiaomiparts.display

import android.app.Service
import android.content.Context
import android.content.Intent
import android.database.ContentObserver
import android.os.Handler
import android.os.IBinder
import android.os.SystemProperties
import android.os.UserHandle
import android.provider.Settings
import android.util.Log
import co.aospa.xiaomiparts.utils.dlog

/** Service to relay DC dimming setting to xiaomi displayfeature. */
class DcDimmingService : Service() {

    private val handler = Handler()

    private val settingObserver =
        object : ContentObserver(handler) {
            override fun onChange(selfChange: Boolean) {
                dlog(TAG, "SettingObserver: onChange")
                updateDcDimming()
            }
        }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        dlog(TAG, "Starting service")
        contentResolver.registerContentObserver(
            Settings.System.getUriFor(Settings.System.DC_DIMMING_STATE),
            false,
            settingObserver,
            UserHandle.USER_CURRENT,
        )
        updateDcDimming()
        return START_STICKY
    }

    override fun onDestroy() {
        dlog(TAG, "Destroying service")
        contentResolver.unregisterContentObserver(settingObserver)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun updateDcDimming() {
        val enabled = Settings.System.getInt(contentResolver, Settings.System.DC_DIMMING_STATE, 0)
        dlog(TAG, "updateDcDimming: enabled=$enabled")
        try {
            DisplayFeatureWrapper.setFeature(DC_DIMMING_MODE, enabled, 0)
        } catch (e: Exception) {
            Log.e(TAG, "updateDcDimming failed!", e)
        }
    }

    companion object {
        private const val TAG = "DcDimmingService"
        private const val DC_DIMMING_PROP = "ro.vendor.display.dc_dimming_supported"
        private const val DC_DIMMING_MODE = 20

        fun startService(context: Context) {
            if (!SystemProperties.getBoolean(DC_DIMMING_PROP, false)) {
                context.startServiceAsUser(
                    Intent(context, DcDimmingService::class.java),
                    UserHandle.CURRENT,
                )
            } else {
                Log.i(TAG, "dc dimming is not supported")
            }
        }
    }
}
