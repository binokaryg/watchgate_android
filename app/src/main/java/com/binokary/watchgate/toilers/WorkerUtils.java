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

import com.binokary.watchgate.Constants;

import java.util.concurrent.TimeUnit;

import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import static com.binokary.watchgate.Constants.REPORTONETAG;
import static com.binokary.watchgate.Constants.REPORTONEWAITTAG;
import static com.binokary.watchgate.Constants.REPORTTAG;
import static com.binokary.watchgate.Constants.SMSONETAG;
import static com.binokary.watchgate.Constants.SMSTAG;


public final class WorkerUtils {
    private static final String TAG = Constants.MAINTAG + WorkerUtils.class.getSimpleName();

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
                        .addTag(SMSTAG)
                        .build();
        Log.d(TAG, "Enqueuing Unique Periodic SMS Sending Task with TAG: " + SMSTAG);
        WorkManager.getInstance().enqueueUniquePeriodicWork(SMSTAG, ExistingPeriodicWorkPolicy.KEEP, periodicSMSSendingRequest);

    }

    public static void enqueueOneTimeSMSSendingWork(String recipient, String msg) {

        Data smsData = new Data.Builder()
                .putString("RECIPIENT", recipient)
                .putString("MSG", msg)
                .build();

        final OneTimeWorkRequest oneTimeSMSSendingRequest =
                new OneTimeWorkRequest.Builder(SMSSender.class)
                        .setInputData(smsData)
                        .addTag(SMSONETAG)
                        .build();
        Log.d(TAG, "Enqueuing One Time SMS Sending Task with TAG: " + SMSONETAG);
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
                        .addTag(REPORTTAG)
                        .build();
        Log.d(TAG, "Enqueuing Unique Periodic Stitch Reporting Task for instance " + instance + " with TAG: " + REPORTTAG);
        WorkManager.getInstance().enqueueUniquePeriodicWork(REPORTTAG, ExistingPeriodicWorkPolicy.KEEP, stitchReportingRequest);

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
        String tag = initialDelayInSeconds > 0 ? REPORTONEWAITTAG : REPORTONETAG;

        final OneTimeWorkRequest stitchReportingRequest =
                new OneTimeWorkRequest.Builder(StitchReporter.class)
                        .setConstraints(stitchReportingConstraints)
                        .setInputData(stitchReportData)
                        .setInitialDelay(initialDelayInSeconds, TimeUnit.SECONDS)
                        .addTag(tag)
                        .build();
        Log.d(TAG, "Enqueuing One Time Stitch Reporting Task for instance " + instance + " with TAG: " + REPORTONETAG);
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