/*
 * SPDX-FileCopyrightText: 2023-2025 Paranoid Android
 * SPDX-License-Identifier: Apache-2.0
 */

package co.aospa.xiaomiparts.gestures

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.SensorManager
import android.os.UserHandle
import android.provider.Settings
import android.util.Log
import co.aospa.xiaomiparts.utils.dlog
import vendor.xiaomi.hardware.fingerprintextension.V1_0.IXiaomiFingerprint

/** Helper utilities related to our gestures */
object GestureUtils {
    private const val TAG = "GestureUtils"

    const val ACTION_GESTURE_TRIGGER = "co.aospa.xiaomiparts.ACTION_GESTURE_TRIGGER"
    const val EXTRA_GESTURE_ACTION_ID = "action_id"

    const val SETTING_KEY_FP_DOUBLE_TAP_ENABLE = "fp_double_tap_enable"
    const val SETTING_KEY_FP_DOUBLE_TAP_ACTION = "fp_double_tap_action"
    private const val FINGERPRINT_CMD_LOCKOUT_MODE = 12
    private const val POWERFP_DISABLE_NAVIGATION = 0
    private const val POWERFP_ENABLE_NAVIGATION = 2

    const val PREF_KEY_BACK_DOUBLE_TAP = "back_double_tap"
    const val PREF_KEY_BACK_TRIPLE_TAP = "back_triple_tap"
    private const val SENSOR_TYPE_BACK_TAP = 33171045 // xiaomi.sensor.dbtap

    fun setFingerprintNavigation(enable: Boolean) {
        runCatching { IXiaomiFingerprint.getService() }
            .onSuccess {
                it.extCmd(
                    FINGERPRINT_CMD_LOCKOUT_MODE,
                    if (enable) POWERFP_ENABLE_NAVIGATION else POWERFP_DISABLE_NAVIGATION,
                )
                dlog(TAG, "setFingerprintNavigation($enable) success")
            }
            .onFailure { e -> Log.e(TAG, "setFingerprintNavigation($enable) failed!", e) }
    }

    fun onBootCompleted(context: Context) {
        val fpTapAvailable = isFpDoubleTapAvailable(context)
        val backTapAvailable = isBackTapAvailable(context)
        dlog(
            TAG,
            "onBootCompleted: fpTapAvailable=$fpTapAvailable, backTapAvailable=$backTapAvailable"
        )

        if (!fpTapAvailable && !backTapAvailable) {
            dlog(TAG, "Disabling gesture settings")
            context.packageManager.setComponentEnabledSetting(
                ComponentName(context, GestureSettingsActivity::class.java),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP,
            )
            return
        }

        if (fpTapAvailable) {
            setFingerprintNavigation(isFpDoubleTapEnabled(context))
        }
        if (backTapAvailable) {
            BackTapService.startService(context)
        }
    }

    fun isFpDoubleTapAvailable(context: Context) =
        context.resources.getBoolean(com.android.internal.R.bool.config_is_powerbutton_fps)

    fun isFpDoubleTapEnabled(context: Context) =
        Settings.System.getIntForUser(
            context.contentResolver,
            SETTING_KEY_FP_DOUBLE_TAP_ENABLE,
            0,
            UserHandle.USER_CURRENT,
        ) == 1

    fun getFpDoubleTapAction(context: Context) =
        Settings.System.getIntForUser(
            context.contentResolver,
            SETTING_KEY_FP_DOUBLE_TAP_ACTION,
            1,
            UserHandle.USER_CURRENT,
        )

    fun getBackTapSensor(context: Context) =
        context.getSystemService(SensorManager::class.java)!!.getDefaultSensor(SENSOR_TYPE_BACK_TAP)

    fun isBackTapAvailable(context: Context) = getBackTapSensor(context) != null
}
