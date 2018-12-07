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

    // Name of Notification Channel for verbose notifications of background work
    public static final CharSequence VERBOSE_NOTIFICATION_CHANNEL_NAME =
            "Verbose WorkManager Notifications";
    public static String VERBOSE_NOTIFICATION_CHANNEL_DESCRIPTION =
            "Shows notifications whenever work starts";
    public static final CharSequence NOTIFICATION_TITLE = "WorkRequest Starting";
    public static final String CHANNEL_ID = "VERBOSE_NOTIFICATION";
    public static final int NOTIFICATION_ID = 1;
    public static final String API_URL = "http://10.0.2.2:3000/api/";
    public static final String SMSTAG = "SMS";
    public static final String REPORTTAG = "REPORT";
    public static final String SMSONETAG = "SMS.ONE";
    public static final String REPORTONETAG = "REPORT.ONE";
    public static final String REPORTONEWAITTAG = "REPORT.ONE.WAIT";
    public static final int STITCH_REPORT_DELAY_INTERVAL_MINS = 15;
    public static final int SEND_SMS_MIN_INTERVAL_MINS = 15;
    public static final String PREF_STATS = "gate_stats";
    public static final String MAINTAG = "gatewatch_";

    // Ensures this class is never instantiated
    private Constants() {
    }
}