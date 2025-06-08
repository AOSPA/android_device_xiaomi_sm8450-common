/*
 * SPDX-FileCopyrightText: 2023-2025 Paranoid Android
 * SPDX-License-Identifier: Apache-2.0
 */

package co.aospa.xiaomiparts.gestures

import android.os.Bundle
import android.os.UserHandle
import android.provider.Settings
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragment
import co.aospa.xiaomiparts.R
import co.aospa.xiaomiparts.utils.dlog

/** Double tap side-fingerprint sensor, settings fragment. */
class GestureSettingsFragment : PreferenceFragment(), Preference.OnPreferenceChangeListener {

    private val fpDoubleTapPref by lazy { findPreference<ListPreference>(KEY_FP_DOUBLE_TAP)!! }
    private val backDoubleTapPref by lazy { findPreference<ListPreference>(KEY_BACK_DOUBLE_TAP)!! }
    private val backTripleTapPref by lazy { findPreference<ListPreference>(KEY_BACK_TRIPLE_TAP)!! }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.gesture_settings)

        fpDoubleTapPref.apply {
            isEnabled = GestureUtils.isFpDoubleTapAvailable(activity)
            onPreferenceChangeListener = this@GestureSettingsFragment
            value =
                if (GestureUtils.isFpDoubleTapEnabled(context))
                    GestureUtils.getFpDoubleTapAction(activity).toString()
                else "0"
            if (!isEnabled) {
                summary = context.getString(R.string.gesture_unavailable)
            }
        }

        val isBackTapAvailable = GestureUtils.isBackTapAvailable(context)
        val backTapSummary =
            if (isBackTapAvailable) "%s" else context.getString(R.string.gesture_unavailable)

        backDoubleTapPref.apply {
            isEnabled = isBackTapAvailable
            summary = backTapSummary
        }

        backTripleTapPref.apply {
            isEnabled = isBackTapAvailable
            summary = backTapSummary
        }
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any?): Boolean =
        when (preference.key) {
            KEY_FP_DOUBLE_TAP -> {
                val action = newValue.toString().toInt()
                val enabled = action > 0
                dlog(TAG, "onPreferenceChange: action=$action")
                Settings.System.putIntForUser(
                    activity.contentResolver,
                    GestureUtils.SETTING_KEY_FP_DOUBLE_TAP_ENABLE,
                    if (enabled) 1 else 0,
                    UserHandle.USER_CURRENT,
                )
                Settings.System.putIntForUser(
                    activity.contentResolver,
                    GestureUtils.SETTING_KEY_FP_DOUBLE_TAP_ACTION,
                    action,
                    UserHandle.USER_CURRENT,
                )
                GestureUtils.setFingerprintNavigation(enabled)
                true
            }
            KEY_BACK_DOUBLE_TAP,
            KEY_BACK_TRIPLE_TAP -> {
                // BackTapService reads shared prefs directly so we don't need to do anything here
                true
            }
            else -> false
        }

    companion object {
        private const val TAG = "GestureSettingsFragment"
        private const val KEY_FP_DOUBLE_TAP = "fp_double_tap"
        private const val KEY_BACK_DOUBLE_TAP = "back_double_tap"
        private const val KEY_BACK_TRIPLE_TAP = "back_triple_tap"
    }
}
