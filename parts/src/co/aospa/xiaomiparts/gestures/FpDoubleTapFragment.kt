/*
 * SPDX-FileCopyrightText: 2023-2025 Paranoid Android
 * SPDX-License-Identifier: Apache-2.0
 */

package co.aospa.xiaomiparts.gestures

import android.os.Bundle
import android.os.UserHandle
import android.provider.Settings
import android.widget.CompoundButton
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragment
import co.aospa.xiaomiparts.R
import co.aospa.xiaomiparts.utils.dlog
import com.android.settingslib.widget.MainSwitchPreference

/** Double tap side-fingerprint sensor, settings fragment. */
class FpDoubleTapFragment :
    PreferenceFragment(),
    Preference.OnPreferenceChangeListener,
    CompoundButton.OnCheckedChangeListener {

    private val switchBar by lazy { findPreference<MainSwitchPreference>(PREF_ENABLE)!! }
    private val actionPref by lazy { findPreference<ListPreference>(PREF_ACTION)!! }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.fp_double_tap_settings)

        val enabled = GestureUtils.isFpDoubleTapEnabled(activity)
        val action = GestureUtils.getFpDoubleTapAction(activity)

        switchBar.apply {
            addOnSwitchChangeListener(this@FpDoubleTapFragment)
            isChecked = enabled
        }

        actionPref.apply {
            isEnabled = enabled
            onPreferenceChangeListener = this@FpDoubleTapFragment
            value = action.toString()
        }
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any?): Boolean {
        return when (preference.key) {
            PREF_ACTION -> {
                val action = newValue.toString().toInt()
                dlog(TAG, "onPreferenceChange: action=$action")
                Settings.System.putIntForUser(
                    activity.contentResolver,
                    GestureUtils.SETTING_KEY_FP_DOUBLE_TAP_ACTION,
                    action,
                    UserHandle.USER_CURRENT,
                )
                true
            }
            else -> false
        }
    }

    override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
        actionPref.isEnabled = isChecked
        Settings.System.putIntForUser(
            activity.contentResolver,
            GestureUtils.SETTING_KEY_FP_DOUBLE_TAP_ENABLE,
            if (isChecked) 1 else 0,
            UserHandle.USER_CURRENT,
        )
        GestureUtils.setFingerprintNavigation(isChecked)
    }

    companion object {
        private const val TAG = "FpDoubleTapFragment"
        private const val PREF_ENABLE = "fp_double_tap_enable"
        private const val PREF_ACTION = "fp_double_tap_action"
    }
}
