#!/usr/bin/env -S PYTHONPATH=../../../tools/extract-utils python3
#
# SPDX-FileCopyrightText: 2024 The LineageOS Project
# SPDX-License-Identifier: Apache-2.0
#

from extract_utils.fixups_blob import blob_fixup, blob_fixups_user_type
from extract_utils.fixups_lib import (
    lib_fixup_remove,
    lib_fixups as base_lib_fixups,
    lib_fixups_user_type,
)
from extract_utils.main import ExtractUtils, ExtractUtilsModule

namespace_imports = [
    'device/xiaomi/sm8450-common',
    'hardware/qcom/display',
    'hardware/qcom/display/gralloc',
    'hardware/qcom/display/libdebug',
    'hardware/xiaomi',
    'vendor/qcom/common/vendor/adreno/s',
    'vendor/qcom/common/vendor/display/5.10',
    'vendor/qcom/common/vendor/media/5.10',
    'vendor/qcom/common/vendor/perf',
    'vendor/qcom/common/vendor/wlan',
    'vendor/xiaomi/sm8450-common',
]


def lib_fixup_vendor_suffix(lib: str, partition: str, *args, **kwargs):
    return f'{lib}_{partition}' if partition == 'vendor' else None


def lib_fixup_prebuilt_suffix(lib: str, *args, **kwargs):
    return f'{lib}_prebuilt'


def lib_fixup_xiaomi_suffix(lib: str, *args, **kwargs):
    return f'{lib}_xiaomi'


lib_fixups: lib_fixups_user_type = {
    **base_lib_fixups,
    'audio.primary.taro': lib_fixup_xiaomi_suffix,
    'libgrpc++_unsecure': lib_fixup_prebuilt_suffix,
    (
        'com.qualcomm.qti.dpm.api@1.0',
        'com.qualcomm.qti.imscmservice*',
        'com.qualcomm.qti.uceservice*',
        'vendor.qti.data.*',
        'vendor.qti.diaghal@1.0',
        'vendor.qti.hardware.data.*',
        'vendor.qti.hardware.dpmservice*',
        'vendor.qti.hardware.embmssl*',
        'vendor.qti.hardware.limits*',
        'vendor.qti.hardware.ListenSoundModel@1.0',
        'vendor.qti.hardware.mwqemadapter@1.0',
        'vendor.qti.hardware.qccsyshal*',
        'vendor.qti.hardware.qccvndhal@1.0',
        'vendor.qti.hardware.radio.*',
        'vendor.qti.hardware.slmadapter@1.0',
        'vendor.qti.hardware.wifidisplaysession@1.0',
        'vendor.qti.imsrtpservice@3.0',
        'vendor.qti.ims.*',
        'vendor.qti.latency*',
        'vendor.xiaomi.hardware.campostproc@1.0',
        'vendor.xiaomi.hardware.displayfeature@1.0',
    ): lib_fixup_vendor_suffix,
    'libwpa_client': lib_fixup_remove,
}

blob_fixups: blob_fixups_user_type = {
    ('vendor/bin/hw/android.hardware.identity-service-qti', 'vendor/lib64/libqtiidentitycredential.so'): blob_fixup()
        .replace_needed('android.hardware.identity-V3-ndk_platform.so', 'android.hardware.identity-V3-ndk.so')
        .replace_needed('android.hardware.keymaster-V3-ndk_platform.so', 'android.hardware.keymaster-V3-ndk.so'),
    ('vendor/bin/hw/android.hardware.security.keymint-service-qti', 'vendor/lib64/libqtikeymint.so'): blob_fixup()
        .replace_needed('android.hardware.security.keymint-V1-ndk_platform.so', 'android.hardware.security.keymint-V1-ndk.so')
        .replace_needed('android.hardware.security.secureclock-V1-ndk_platform.so', 'android.hardware.security.secureclock-V1-ndk.so')
        .replace_needed('android.hardware.security.sharedsecret-V1-ndk_platform.so', 'android.hardware.security.sharedsecret-V1-ndk.so')
        .add_needed('android.hardware.security.rkp-V1-ndk.so'),
    'vendor/etc/media_codecs_dolby_audio.xml': blob_fixup()
        .regex_replace(r'<MediaCodec name="c2\.dolby\.ac4\.decoder[\s\S]*?</MediaCodec>\n?', '')
        .regex_replace(r'.*software-codec.*\n?', ''),
    (
        'vendor/etc/media_codecs.xml',
        'vendor/etc/media_codecs_cape.xml',
        'vendor/etc/media_codecs_cape_vendor.xml',
        'vendor/etc/media_codecs_performance_cape.xml',
        'vendor/etc/media_codecs_performance_cape_vendor.xml',
        'vendor/etc/media_codecs_performance_taro.xml',
        'vendor/etc/media_codecs_performance_taro_vendor.xml',
        'vendor/etc/media_codecs_performance_ukee.xml',
        'vendor/etc/media_codecs_performance_ukee_vendor.xml',
        'vendor/etc/media_codecs_system_default.xml',
        'vendor/etc/media_codecs_taro.xml',
        'vendor/etc/media_codecs_taro_vendor.xml',
        'vendor/etc/media_codecs_ukee.xml',
        'vendor/etc/media_codecs_ukee_vendor.xml',
    ): blob_fixup()
        .regex_replace(r'.*media_codecs_(google_audio|google_c2|google_telephony|vendor_audio).*\n?', '')
        .regex_replace(r'.*media_codecs_with_dolby.*\n?', '')
        .regex_replace(r'<MediaCodec name="c2\.dolby\.[\s\S]*?</MediaCodec>\n?', ''),
    (
        'vendor/lib64/mediadrm/libwvdrmengine.so',
        'vendor/lib64/libcodec2_soft_ac4dec.so',
        'vendor/lib64/libcodec2_soft_ddpdec.so',
        'vendor/lib64/libdlbdsservice.so',
        'vendor/lib64/libdlbpreg.so',
        'vendor/lib64/soundfx/libdlbvol.so',
        'vendor/lib64/soundfx/libhwdap.so',
        'vendor/lib64/soundfx/libswspatializer.so',
    ): blob_fixup().replace_needed('libstagefright_foundation.so', 'libstagefright_foundation-v33.so'),
    ('vendor/lib64/libgarden.so', 'vendor/lib64/libgarden_haltests_e2e.so'): blob_fixup()
        .replace_needed('android.hardware.gnss-V1-ndk_platform.so', 'android.hardware.gnss-V1-ndk.so'),
    'vendor/lib64/libwvhidl.so': blob_fixup().add_needed('libcrypto_shim.so'),
    'vendor/lib64/vendor.libdpmframework.so': blob_fixup().add_needed('libhidlbase_shim.so'),
}  # fmt: skip


module = ExtractUtilsModule(
    'sm8450-common',
    'xiaomi',
    blob_fixups=blob_fixups,
    lib_fixups=lib_fixups,
    namespace_imports=namespace_imports,
)

if __name__ == '__main__':
    utils = ExtractUtils.device(module)
    utils.run()
