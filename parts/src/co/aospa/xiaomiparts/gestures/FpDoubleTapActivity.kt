/*
 * SPDX-FileCopyrightText: 2023-2025 Paranoid Android
 * SPDX-License-Identifier: Apache-2.0
 */

package co.aospa.xiaomiparts.gestures

import android.os.Bundle
import com.android.settingslib.collapsingtoolbar.CollapsingToolbarBaseActivity
import com.android.settingslib.collapsingtoolbar.R

/** Double tap side-fingerprint sensor, settings activity. */
class FpDoubleTapActivity : CollapsingToolbarBaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fragmentManager
            .beginTransaction()
            .replace(R.id.content_frame, FpDoubleTapFragment(), TAG)
            .commit()
    }

    companion object {
        private const val TAG = "fp_double_tap"
    }
}
