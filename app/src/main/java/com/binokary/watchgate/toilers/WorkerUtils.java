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

package com.binokary.watchgate.toilers;

import android.util.Log;
import java.util.concurrent.TimeUnit;

import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import static com.binokary.watchgate.Constants.MAIN_TAG;
import static com.binokary.watchgate.Constants.REPORT_ONE_TAG;
import static com.binokary.watchgate.Constants.REPORT_ONE_WAIT_TAG;
import static com.binokary.watchgate.Constants.REPORT_TAG;
import static com.binokary.watchgate.Constants.SMS_ONE_TAG;
import static com.binokary.watchgate.Constants.SMS_TAG;


public final class WorkerUtils {
    private static final String TAG = MAIN_TAG + WorkerUtils.class.getSimpleName();

    private WorkerUtils() {
    }

    public static void enqueueSMSSendingWork(String recipient, String msg, Integer minutes, Integer minMinutes) {

        Data smsData = new Data.Builder()
                .putString("RECIPIENT", recipient)
                .putString("MSG", msg)
                .putInt("MIN", minMinutes)
                .build();

        final PeriodicWorkRequest periodicSMSSendingRequest =
                new PeriodicWorkRequest.Builder(SMSSender.class, minutes, TimeUnit.MINUTES)
                        .setInputData(smsData)
                        .addTag(SMS_TAG)
                        .build();
        Log.d(TAG, "Enqueuing Unique Periodic SMS Sending Task with TAG: " + SMS_TAG);
        WorkManager.getInstance().enqueueUniquePeriodicWork(SMS_TAG, ExistingPeriodicWorkPolicy.KEEP, periodicSMSSendingRequest);

    }

    public static void enqueueOneTimeSMSSendingWork(String recipient, String msg) {

        Data smsData = new Data.Builder()
                .putString("RECIPIENT", recipient)
                .putString("MSG", msg)
                .build();

        final OneTimeWorkRequest oneTimeSMSSendingRequest =
                new OneTimeWorkRequest.Builder(SMSSender.class)
                        .setInputData(smsData)
                        .addTag(SMS_ONE_TAG)
                        .build();
        Log.d(TAG, "Enqueuing One Time SMS Sending Task with TAG: " + SMS_ONE_TAG);
        WorkManager.getInstance().enqueue(oneTimeSMSSendingRequest);
    }


    public static void enqueueStitchReportingWork(String instance, Integer minutes, Integer minMinutes, Integer minOneMinutes) {

        Data stitchReportData = new Data.Builder()
                .putString("INSTANCE", instance)
                .putInt("MIN", minMinutes)
                .putInt("MIN_ONE", minOneMinutes)
                .build();

        Constraints stitchReportingConstraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        final PeriodicWorkRequest stitchReportingRequest =
                new PeriodicWorkRequest.Builder(StitchReporter.class, minutes, TimeUnit.MINUTES)
                        .setConstraints(stitchReportingConstraints)
                        .setInputData(stitchReportData)
                        .addTag(REPORT_TAG)
                        .build();
        Log.d(TAG, "Enqueuing Unique Periodic Stitch Reporting Task for instance " + instance + " with TAG: " + REPORT_TAG);
        WorkManager.getInstance().enqueueUniquePeriodicWork(REPORT_TAG, ExistingPeriodicWorkPolicy.KEEP, stitchReportingRequest);

    }

    public static void enqueueOneTimeStitchReportingWork(String instance, Integer minOneMinutes, Integer initialDelayInSeconds) {

        Data stitchReportData = new Data.Builder()
                .putString("INSTANCE", instance)
                .putBoolean("ONETIME", true)
                .putInt("MIN_ONE", minOneMinutes)
                .build();

        Constraints stitchReportingConstraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        //Use waiting tag if initial Delay is more than 0
        String tag = initialDelayInSeconds > 0 ? REPORT_ONE_WAIT_TAG : REPORT_ONE_TAG;

        final OneTimeWorkRequest stitchReportingRequest =
                new OneTimeWorkRequest.Builder(StitchReporter.class)
                        .setConstraints(stitchReportingConstraints)
                        .setInputData(stitchReportData)
                        .setInitialDelay(initialDelayInSeconds, TimeUnit.SECONDS)
                        .addTag(tag)
                        .build();
        Log.d(TAG, "Enqueuing One Time Stitch Reporting Task for instance " + instance + " with TAG: " + REPORT_ONE_TAG);
        WorkManager.getInstance().enqueue(stitchReportingRequest);
    }

    public static int clearTasks(String taskTAG) {
        try {
            WorkManager.getInstance().cancelAllWorkByTag(taskTAG);
            WorkManager.getInstance().pruneWork();
            return 1;
        } catch (Exception ex) {
            return 0;
        }
    }

}