/*
 * SPDX-FileCopyrightText: 2020 The LineageOS Project
 * SPDX-FileCopyrightText: 2025 Paranoid Android
 * SPDX-License-Identifier: Apache-2.0
 */

package co.aospa.xiaomiparts.thermal

import android.os.Bundle
import com.android.settingslib.collapsingtoolbar.CollapsingToolbarBaseActivity
import com.android.settingslib.collapsingtoolbar.R

/** Thermal profile settings activity. */
class ThermalSettingsActivity : CollapsingToolbarBaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fragmentManager
            .beginTransaction()
            .replace(R.id.content_frame, ThermalSettingsFragment(), TAG_THERMAL)
            .commit()
    }

    companion object {
        private const val TAG_THERMAL = "thermal"
    }
}
