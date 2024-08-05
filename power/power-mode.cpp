/*
 * Copyright (C) 2024 Paranoid Android
 *
 * SPDX-License-Identifier: Apache-2.0
 */

#include <aidl/android/hardware/power/BnPower.h>
#include <android-base/file.h>

namespace aidl {
namespace android {
namespace hardware {
namespace power {
namespace impl {

using ::aidl::android::hardware::power::Mode;

constexpr const char* THERMAL_CONFIG_PATH = "/sys/class/thermal/thermal_message/sconfig";
constexpr const char* THERMAL_STATE_DEFAULT = "0";
constexpr const char* THERMAL_STATE_GAMING = "19";

bool isDeviceSpecificModeSupported(Mode type, bool* _aidl_return) {
    switch (type) {
        case Mode::GAME:
            *_aidl_return = true;
            return true;
        default:
            return false;
    }
}

bool setDeviceSpecificMode(Mode type, bool enabled) {
    switch (type) {
        case Mode::GAME:
            ::android::base::WriteStringToFile(
                    enabled ? THERMAL_STATE_GAMING : THERMAL_STATE_DEFAULT, THERMAL_CONFIG_PATH);
            return true;
        default:
            return false;
    }
}

}  // namespace impl
}  // namespace power
}  // namespace hardware
}  // namespace android
}  // namespace aidl
