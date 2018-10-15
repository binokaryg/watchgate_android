package com.binokary.watchgate.toilers;

import android.content.Context;
import android.content.SharedPreferences;
import android.telephony.SmsManager;
import android.util.Log;

import com.binokary.watchgate.Constants;
import com.binokary.watchgate.PrefStrings;
import com.binokary.watchgate.StatsHelper;

import androidx.work.WorkManager;
import androidx.work.Worker;

import static android.content.Context.MODE_PRIVATE;

public class SMSSender extends Worker {
    public static final String TAG = Constants.MAINTAG + SMSSender.class.getSimpleName();
    // Define the parameter keys:
    public static final String KEY_RECIPIENT_ARG = "RECIPIENT";
    public static final String KEY_MSG_ARG = "MSG";
    SharedPreferences prefs;

    @Override
    public Worker.Result doWork() {
        Context applicationContext = getApplicationContext();
        try {
            prefs = applicationContext.getSharedPreferences(Constants.PREF_STATS, MODE_PRIVATE);
            // Fetch the arguments (and specify default values):
            String recipient = getInputData().getString(KEY_RECIPIENT_ARG);
            String msg = getInputData().getString(KEY_MSG_ARG);
            Integer minInterval = getInputData().getInt("MIN", 10) * 60 * 1000;
            Boolean oneTime = getInputData().getBoolean("ONETIMEREQUEST", false);
            Long lastBalanceDate = prefs.getLong(PrefStrings.BALANCE_DATE, 0);
            Long date = System.currentTimeMillis();
            if (oneTime || (date - lastBalanceDate > minInterval)) {
                Log.d(TAG, "Sending balance query sms as a " + (oneTime ? "One Time" : "Periodic") + " request");
                SmsManager smsManager = SmsManager.getDefault();
                smsManager.sendTextMessage(recipient, null, msg, null, null);
            } else {
                Log.d(TAG, "Not sending as SMS was recently sent at: " + StatsHelper.DateStringFromMS(lastBalanceDate) + " and now it is " + StatsHelper.DateStringFromMS(date));
                return Result.SUCCESS;
            }
            return Worker.Result.SUCCESS;
        } catch (Throwable throwable) {
            Log.e(TAG, "Error sending SMS", throwable);
            return Worker.Result.FAILURE;
        }
    }
}
