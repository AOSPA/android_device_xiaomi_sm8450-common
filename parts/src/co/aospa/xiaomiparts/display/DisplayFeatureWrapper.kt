/*
 * SPDX-FileCopyrightText: 2023-2025 Paranoid Android
 * SPDX-License-Identifier: Apache-2.0
 */

package co.aospa.xiaomiparts.display

import android.os.IHwBinder
import android.util.Log
import co.aospa.xiaomiparts.utils.dlog
import vendor.xiaomi.hardware.displayfeature.V1_0.IDisplayFeature

/** Convenient wrapper around xiaomi displayfeature interface. */
object DisplayFeatureWrapper {
    private const val TAG = "DisplayFeatureWrapper"

    @Volatile private var displayFeature: IDisplayFeature? = null

    private val deathRecipient =
        IHwBinder.DeathRecipient { _ ->
            dlog(TAG, "serviceDied")
            displayFeature = null
        }

    @Synchronized
    private fun getDisplayFeature(): IDisplayFeature? =
        displayFeature
            ?: runCatching { IDisplayFeature.getService() }
                .onSuccess {
                    displayFeature = it.apply { asBinder().linkToDeath(deathRecipient, 0) }
                }
                .onFailure { e -> Log.e(TAG, "getDisplayFeature failed!", e) }
                .getOrNull()

    fun setFeature(mode: Int, value: Int, cookie: Int) {
        val displayFeature =
            getDisplayFeature()
                ?: run {
                    Log.e(TAG, ": displayFeature is null!")
                    return
                }
        dlog(TAG, "setFeature: mode=$mode, value=$value, cookie=$cookie")
        runCatching { displayFeature.setFeature(/*displayId*/ 0, mode, value, cookie) }
            .onFailure { e -> Log.e(TAG, "setFeature failed!", e) }
    }
}
