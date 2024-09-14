/*
 * Copyright (C) 2018 The LineageOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package co.aospa.xiaomiparts;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.display.DisplayManager;
import android.os.IBinder;
import android.util.Log;
import android.view.Display;
import android.view.Display.HdrCapabilities;

import co.aospa.xiaomiparts.camera.NfcCameraService;
import co.aospa.xiaomiparts.display.ColorService;
import co.aospa.xiaomiparts.display.DcDimmingService;
import co.aospa.xiaomiparts.doze.AodBrightnessService;
import co.aospa.xiaomiparts.doze.PocketService;
import co.aospa.xiaomiparts.gestures.GestureUtils;
import co.aospa.xiaomiparts.thermal.ThermalUtils;
import co.aospa.xiaomiparts.touch.TouchNonUiService;
import co.aospa.xiaomiparts.touch.TouchOrientationService;

public class BootCompletedReceiver extends BroadcastReceiver {

    private static final String TAG = "XiaomiParts-BCR";

    @Override
    public void onReceive(final Context context, Intent intent) {
        Log.d(TAG, "Received intent: " + intent.getAction());
        if (!intent.getAction().equals(Intent.ACTION_LOCKED_BOOT_COMPLETED)) {
            return;
        }

        Log.i(TAG, "Boot completed, starting services");
        ColorService.startService(context);
        DcDimmingService.startService(context);
        AodBrightnessService.startService(context);
        PocketService.startService(context);
        NfcCameraService.startService(context);
        TouchOrientationService.startService(context);
        TouchNonUiService.startService(context);
        ThermalUtils.getInstance(context).startService();
        GestureUtils.onBootCompleted(context);
        overrideHdrTypes(context);
    }

    private static void overrideHdrTypes(Context context) {
        // Override HDR types to enable Dolby Vision
        final DisplayManager dm = context.getSystemService(DisplayManager.class);
        dm.overrideHdrTypes(Display.DEFAULT_DISPLAY, new int[]{
                HdrCapabilities.HDR_TYPE_DOLBY_VISION, HdrCapabilities.HDR_TYPE_HDR10,
                HdrCapabilities.HDR_TYPE_HLG, HdrCapabilities.HDR_TYPE_HDR10_PLUS});
    }

}
