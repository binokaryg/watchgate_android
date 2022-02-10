package com.binokary.watchgate.toilers;

import android.content.Context;
import android.content.SharedPreferences;
import android.telephony.SmsManager;
import android.util.Log;

import com.binokary.watchgate.PrefStrings;
import com.binokary.watchgate.StatsHelper;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import static android.content.Context.MODE_PRIVATE;
import static com.binokary.watchgate.Constants.MAIN_TAG;
import static com.binokary.watchgate.Constants.PREF_STATS;
import static com.binokary.watchgate.Constants.SMS_TAG;

public class SMSSender extends Worker {
    public static final String TAG = MAIN_TAG + SMSSender.class.getSimpleName();
    // Define the parameter keys:
    public static final String KEY_RECIPIENT_ARG = "RECIPIENT";
    public static final String KEY_MSG_ARG = "MSG";
    SharedPreferences prefs;

    public SMSSender(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Worker.Result doWork() {
        Context applicationContext = getApplicationContext();
        try {
            prefs = applicationContext.getSharedPreferences(PREF_STATS, MODE_PRIVATE);
            // Fetch the arguments (and specify default values):
            String recipient = getInputData().getString(KEY_RECIPIENT_ARG);
            String msg = getInputData().getString(KEY_MSG_ARG);
            int minInterval = getInputData().getInt("MIN", 0) * 60 * 1000;
            boolean oneTime = getInputData().getBoolean(SMS_TAG, false);
            long lastBalanceDate = prefs.getLong(PrefStrings.BALANCE_DATE, 0);
            long date = System.currentTimeMillis();
            if (oneTime || (date - lastBalanceDate > minInterval)) {
                Log.d(TAG, "Sending sms as a " + (oneTime ? "One Time" : "Periodic") + " request");
                SmsManager smsManager = SmsManager.getDefault();
                smsManager.sendTextMessage(recipient, null, msg, null, null);
            } else {
                Log.d(TAG, "Not sending as SMS was recently sent at: " + StatsHelper.DateStringFromMS(lastBalanceDate) + " and now it is " + StatsHelper.DateStringFromMS(date));
                return Result.success();
            }
            return Worker.Result.success();
        } catch (Throwable throwable) {
            Log.e(TAG, "Error sending SMS", throwable);
            return Worker.Result.failure();
        }
    }
}
