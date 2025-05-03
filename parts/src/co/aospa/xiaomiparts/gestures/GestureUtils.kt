/*
 * SPDX-FileCopyrightText: 2023-2025 Paranoid Android
 * SPDX-License-Identifier: Apache-2.0
 */

package co.aospa.xiaomiparts.gestures

import android.content.Context
import android.os.UserHandle
import android.provider.Settings
import android.util.Log
import co.aospa.xiaomiparts.utils.dlog
import vendor.xiaomi.hardware.fingerprintextension.V1_0.IXiaomiFingerprint

/** Helper utilities related to our gestures */
object GestureUtils {

    private const val TAG = "GestureUtils"
    const val SETTING_KEY_FP_DOUBLE_TAP_ENABLE = "fp_double_tap_enable"
    const val SETTING_KEY_FP_DOUBLE_TAP_ACTION = "fp_double_tap_action"
    private const val FINGERPRINT_CMD_LOCKOUT_MODE = 12
    private const val POWERFP_DISABLE_NAVIGATION = 0
    private const val POWERFP_ENABLE_NAVIGATION = 2

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
        if (isFpDoubleTapAvailable(context)) {
            setFingerprintNavigation(isFpDoubleTapEnabled(context))
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
}
