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

import android.content.Context;
import android.util.Log;

import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static com.binokary.watchgate.Constants.MAIN_TAG;
import static com.binokary.watchgate.Constants.REPORT_ONE_TAG;
import static com.binokary.watchgate.Constants.REPORT_ONE_WAIT_TAG;
import static com.binokary.watchgate.Constants.REPORT_TAG;
import static com.binokary.watchgate.Constants.SMS_ONE_TAG;
import static com.binokary.watchgate.Constants.SMS_TAG;

import com.binokary.watchgate.SlackWorker;

import org.json.JSONObject;


public final class WorkerUtils {
    private static final String TAG = MAIN_TAG + WorkerUtils.class.getSimpleName();

    private WorkerUtils() {
    }

    public static void enqueueSMSSendingWork(Context context, String recipient, String msg, Integer minutes, Integer minMinutes) {

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
        WorkManager.getInstance(Objects.requireNonNull(context)).enqueueUniquePeriodicWork(SMS_TAG, ExistingPeriodicWorkPolicy.KEEP, periodicSMSSendingRequest);

    }

    public static void enqueueOneTimeSMSSendingWork(String recipient, String msg) {
        Context context = com.binokary.watchgate.Application.getContext();
        if (context == null) {
            Log.e(TAG, "Context is null, cannot enqueue SMS sending work");
            return;
        }


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
        WorkManager.getInstance(Objects.requireNonNull(context)).enqueue(oneTimeSMSSendingRequest);
    }


    public static void enqueueReportingWork(Context context, String instance, Integer minutes, Integer minMinutes, Integer minOneMinutes) {

        Data reportData = new Data.Builder()
                .putString("INSTANCE", instance)
                .putInt("MIN", minMinutes)
                .putInt("MIN_ONE", minOneMinutes)
                .build();

        Constraints reportingConstraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        final PeriodicWorkRequest reportingRequest =
                new PeriodicWorkRequest.Builder(StatsReporter.class, minutes, TimeUnit.MINUTES)
                        .setConstraints(reportingConstraints)
                        .setInputData(reportData)
                        .addTag(REPORT_TAG)
                        .build();
        Log.d(TAG, "Enqueuing Unique Periodic Reporting Task for instance " + instance + " with TAG: " + REPORT_TAG);
        WorkManager.getInstance(Objects.requireNonNull(context)).enqueueUniquePeriodicWork(REPORT_TAG, ExistingPeriodicWorkPolicy.KEEP, reportingRequest);

    }

    public static void enqueueOneTimeReportingWork(Context context, String instance, Integer minOneMinutes, Integer initialDelayInSeconds) {

        Data reportData = new Data.Builder()
                .putString("INSTANCE", instance)
                .putBoolean("ONETIME", true)
                .putInt("MIN_ONE", minOneMinutes)
                .build();

        Constraints reportingConstraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        //Use waiting tag if initial Delay is more than 0
        String tag = initialDelayInSeconds > 0 ? REPORT_ONE_WAIT_TAG : REPORT_ONE_TAG;

        final OneTimeWorkRequest reportingRequest =
                new OneTimeWorkRequest.Builder(StatsReporter.class)
                        .setConstraints(reportingConstraints)
                        .setInputData(reportData)
                        .setInitialDelay(initialDelayInSeconds, TimeUnit.SECONDS)
                        .addTag(tag)
                        .build();
        Log.d(TAG, "Enqueuing One Time Reporting Task for instance " + instance + " with TAG: " + REPORT_ONE_TAG);
        WorkManager.getInstance(Objects.requireNonNull(context)).enqueue(reportingRequest);
    }

    public static void enqueueSlackWork(Context context, JSONObject jsonBody) {
        Data inputData = new Data.Builder()
                .putString("json_body", jsonBody.toString())
                .build();

        OneTimeWorkRequest slackWork = new OneTimeWorkRequest.Builder(SlackWorker.class)
                .setInputData(inputData)
                .setBackoffCriteria(BackoffPolicy.LINEAR, 10, TimeUnit.SECONDS)
                .build();

        WorkManager.getInstance(context).enqueue(slackWork);
    }

    public static int clearTasks(Context context, String taskTAG) {
        try {
            WorkManager.getInstance(Objects.requireNonNull(context)).cancelAllWorkByTag(taskTAG);
            WorkManager.getInstance(context).pruneWork();
            return 1;
        } catch (Exception ex) {
            return 0;
        }
    }

}