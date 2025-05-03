/*
 * SPDX-FileCopyrightText: 2023-2025 Paranoid Android
 * SPDX-License-Identifier: Apache-2.0
 */

package co.aospa.xiaomiparts.camera

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.nfc.INfcAdapter
import android.nfc.NfcAdapter
import android.os.Handler
import android.os.IBinder
import android.os.RemoteException
import android.os.ServiceManager
import android.os.SystemProperties
import android.os.UserHandle
import android.util.Log
import androidx.core.os.postDelayed
import co.aospa.xiaomiparts.utils.dlog

/**
 * Service to pause NFC polling when the front camera is active. Enabled only if system property
 * persist.nfc.camera.pause_polling is set to true.
 */
class NfcCameraService : Service() {

    private var nfcAdapter: INfcAdapter? = null
    private var cameraManager: CameraManager? = null
    private val handler = Handler()

    private var isFrontCamInUse = false
        set(value) {
            if (field == value) return
            field = value
            dlog(TAG, "isFrontCamInUse=$value")
            updateNfcPollingState()
        }

    private val cameraCallback =
        object : CameraManager.AvailabilityCallback() {
            override fun onCameraOpened(cameraId: String, packageId: String) {
                dlog(TAG, "onCameraOpened id=$cameraId package=$packageId")
                if (cameraId == FRONT_CAMERA_ID && !IGNORED_PACKAGES.contains(packageId)) {
                    isFrontCamInUse = true
                }
            }

            override fun onCameraClosed(cameraId: String) {
                dlog(TAG, "onCameraClosed id=$cameraId")
                if (cameraId == FRONT_CAMERA_ID && isFrontCamInUse) {
                    isFrontCamInUse = false
                }
            }
        }

    override fun onCreate() {
        super.onCreate()
        dlog(TAG, "onCreate")
        cameraManager = getSystemService(CameraManager::class.java)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        dlog(TAG, "onStartCommand")
        cameraManager?.registerAvailabilityCallback(cameraCallback, handler)
        return START_STICKY
    }

    override fun onDestroy() {
        dlog(TAG, "onDestroy")
        cameraManager?.unregisterAvailabilityCallback(cameraCallback)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    @Synchronized
    private fun getNfcAdapter(): INfcAdapter? =
        nfcAdapter
            ?: runCatching {
                    INfcAdapter.Stub.asInterface(ServiceManager.getService(Context.NFC_SERVICE))
                }
                .onSuccess { nfcAdapter = it }
                .onFailure { e -> Log.e(TAG, "Failed to get nfc adapter!", e) }
                .getOrNull()

    private fun updateNfcPollingState() {
        val adapter = getNfcAdapter() ?: return
        if (!isNfcEnabled()) {
            dlog(TAG, "updateNfcPollingState: nfc is disabled")
            return
        }
        if (isFrontCamInUse) {
            Log.i(TAG, "Front cam in use, pause polling")
            pausePolling()
        } else {
            Log.i(TAG, "Front cam not in use, resume polling")
            handler.removeCallbacksAndMessages(null)
            try {
                adapter.resumePolling()
            } catch (e: RemoteException) {
                Log.e(TAG, "Failed to resume polling!", e)
            }
        }
    }

    private fun pausePolling() {
        val adapter = getNfcAdapter() ?: return
        try {
            adapter.pausePolling(MAX_POLLING_PAUSE_TIMEOUT)
        } catch (e: RemoteException) {
            Log.e(TAG, "Failed to pause polling!", e)
            return
        }
        handler.postDelayed((MAX_POLLING_PAUSE_TIMEOUT + 100).toLong()) {
            if (isNfcEnabled()) {
                Log.i(TAG, "Front cam still in use, polling pause timed out, pausing again")
                pausePolling()
            } else {
                dlog(TAG, "pausePolling: nfc is disabled")
            }
        }
    }

    private fun isNfcEnabled(): Boolean =
        try {
            getNfcAdapter()?.state == NfcAdapter.STATE_ON
        } catch (e: RemoteException) {
            Log.e(TAG, "Failed to get nfc state!", e)
            false
        }

    companion object {
        private const val TAG = "NfcCameraService"
        private const val SYSPROP = "persist.nfc.camera.pause_polling"
        private const val MAX_POLLING_PAUSE_TIMEOUT = 40000 // matches NfcService
        private const val FRONT_CAMERA_ID = "1"
        private val IGNORED_PACKAGES =
            setOf(
                "co.aospa.sense", // face unlock
                "com.google.android.as", // auto rotate, screen attention etc
            )

        fun startService(context: Context) {
            if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_NFC)) {
                Log.i(TAG, "No nfc on this device")
            } else if (SystemProperties.getBoolean(SYSPROP, false)) {
                Log.i(TAG, "Disabled via system prop")
            } else {
                context.startServiceAsUser(
                    Intent(context, NfcCameraService::class.java),
                    UserHandle.CURRENT,
                )
            }
        }
    }
}
