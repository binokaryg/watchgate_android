/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.binokary.watchgate;

public final class Constants {

    // Notification Channel constants

    public static final String SMS_TAG = "SMS";
    public static final String REPORT_TAG = "REPORT";
    public static final String SMS_ONE_TAG = "SMS.ONE";
    public static final String REPORT_ONE_TAG = "REPORT.ONE";
    public static final String REPORT_ONE_WAIT_TAG = "REPORT.ONE.WAIT";
    public static final String PREF_STATS = "gate_stats";
    public static final String MAIN_TAG = "gatewatch_";

    // Ensures this class is never instantiated
    private Constants() {
    }
}