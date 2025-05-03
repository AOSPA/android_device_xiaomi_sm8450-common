/*
 * SPDX-FileCopyrightText: 2023-2025 Paranoid Android
 * SPDX-License-Identifier: Apache-2.0
 */

package co.aospa.xiaomiparts.touch

import android.os.IHwBinder
import android.util.Log
import co.aospa.xiaomiparts.utils.dlog
import vendor.xiaomi.hw.touchfeature.V1_0.ITouchFeature

/** Convenient wrapper around xiaomi touchfeature interface. */
object TouchFeatureWrapper {
    private const val TAG = "TouchFeatureWrapper"

    @Volatile private var touchFeature: ITouchFeature? = null

    private val deathRecipient =
        IHwBinder.DeathRecipient {
            dlog(TAG, "serviceDied")
            touchFeature = null
        }

    @Synchronized
    private fun getTouchFeature(): ITouchFeature? =
        touchFeature
            ?: runCatching { ITouchFeature.getService() }
                .onSuccess { touchFeature = it.apply { asBinder().linkToDeath(deathRecipient, 0) } }
                .onFailure { e -> Log.e(TAG, "getTouchFeature failed!", e) }
                .getOrNull()

    fun setModeValue(mode: Int, value: Int) {
        val touchFeature =
            getTouchFeature()
                ?: run {
                    Log.e(TAG, "setModeValue: touchFeature is null!")
                    return
                }
        dlog(TAG, "set mode=$mode value=$value")
        runCatching { touchFeature.setModeValue(0, mode, value) }
            .onFailure { e -> Log.e(TAG, "setModeValue failed!", e) }
    }
}
