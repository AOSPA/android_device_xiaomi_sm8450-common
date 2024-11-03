/*
 * Copyright (C) 2021 The LineageOS Project
 *           (C) 2024 Paranoid Android
 *
 * SPDX-License-Identifier: Apache-2.0
 */

#ifndef LIBINIT_VARIANT_H
#define LIBINIT_VARIANT_H

#include <string>
#include <vector>

using std::string;

typedef struct variant_info {
    string hwc;
    string brand;
    string device;
    string model;
    string name;
    string mod_device;
    string marketname;
} variant_info_t;

void search_set_variant_props(const std::vector<variant_info_t> variants);

#endif // LIBINIT_VARIANT_H
