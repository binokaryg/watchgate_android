package com.binokary.watchgate.toilers;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.binokary.watchgate.Constants;
import com.binokary.watchgate.PrefStrings;
import com.binokary.watchgate.StatsHelper;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.mongodb.lang.NonNull;
import com.mongodb.stitch.android.core.Stitch;
import com.mongodb.stitch.android.core.StitchAppClient;
import com.mongodb.stitch.android.core.auth.StitchUser;
import com.mongodb.stitch.core.auth.providers.anonymous.AnonymousCredential;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.BsonArray;
import org.bson.BsonInt64;
import org.bson.BsonValue;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import androidx.work.Worker;

import static android.content.Context.MODE_PRIVATE;

public class StitchReporter extends Worker {

    public static final String TAG = Constants.MAINTAG + StitchReporter.class.getSimpleName();
    private SharedPreferences prefs;
    private SharedPreferences mPrefs;
    List<Object> arg = new ArrayList<>();
    BasicBSONObject bObj= new BasicBSONObject();
    static StitchAppClient client;

    @Override
    public Worker.Result doWork() {
        Context applicationContext = getApplicationContext();
        SharedPreferences.Editor stats = applicationContext.getSharedPreferences(Constants.PREF_STATS, MODE_PRIVATE).edit();
        Boolean oneTime = getInputData().getBoolean("ONETIME", false);
        Integer minInterval = getInputData().getInt("MIN", 10) * 60 * 1000;
        Integer minOneInterval = getInputData().getInt("MIN_ONE", 3) * 60 * 1000;
        prefs = applicationContext.getSharedPreferences(Constants.PREF_STATS, MODE_PRIVATE);
        mPrefs = PreferenceManager.getDefaultSharedPreferences(applicationContext);
        Long lastReportAttemptDate = prefs.getLong(PrefStrings.REPORT_ATTEMPT_DATE, 0);
        Long lastReportUpdateDate = prefs.getLong(PrefStrings.UPD_DATE, 0);
        Long now = System.currentTimeMillis();
        if (now - lastReportAttemptDate < minOneInterval) {
            //don't even allow frequent oneTime requests to keep the number of stitch connections low
            Log.d(TAG, String.format("Not allowing to attempt %s reporting request because" +
                            " last attempt was on %s and and now it is %s", oneTime ? "One Time" : "Periodic",
                    StatsHelper.DateStringFromMS(lastReportAttemptDate), StatsHelper.DateStringFromMS(now)));

            return Result.SUCCESS; //or FAILURE or RETRY?

        } else {
            stats.putLong(PrefStrings.REPORT_ATTEMPT_DATE, now);

            if (!oneTime && now - lastReportUpdateDate < minInterval) {
                //Allow one time but check periodic requests
                Log.d(TAG, String.format("Not allowing to update because" +
                                " last update was on %s and and now it is %s",
                        StatsHelper.DateStringFromMS(lastReportUpdateDate), StatsHelper.DateStringFromMS(now)));
                return Result.SUCCESS;
            } else {
                try {
                    StatsHelper.CheckAndUpdateStats(applicationContext);

                    String instance = getInputData().getString("INSTANCE");
                    Long balanceDateL = prefs.getLong(PrefStrings.BALANCE_DATE, 0);

                    boolean isPostpaid = mPrefs.getBoolean("switch_preference_1", false);
                    Log.d(TAG, "Is Postpaid ? " + isPostpaid);
                    Integer balance = prefs.getInt(PrefStrings.PREPAID_BALANCE, -1);
                    Integer balanceDue = prefs.getInt(PrefStrings.POSTPAID_BALANCE_DUE, -1);
                    Integer balanceCredit = prefs.getInt(PrefStrings.POSTPAID_BALANCE_CREDIT, -1);

                    String stitchUpdateFunctionName = mPrefs.getString("pref_stitch_update_func", "updateStatusForObj");
                    Long dateL = System.currentTimeMillis();
                    int battery = prefs.getInt(PrefStrings.BATTERY, -1);
                    boolean plugged = prefs.getBoolean(PrefStrings.PLUGGED, false);
                    boolean data = prefs.getBoolean(PrefStrings.MOBILE_DATA, false);
                    int temp = prefs.getInt(PrefStrings.TEMPERATURE, -1);
                    //int health = prefs.getInt(PrefStrings.HEALTH, -1);
                    String wifi = prefs.getString(PrefStrings.WIFI_SSID, "N/A");
                    int wifiStrength = prefs.getInt(PrefStrings.WIFI_STRENGTH, -1);
                    //int mobileStrength = prefs.getInt(PrefStrings.MOBILE_STRENGTH, -1);
                    Long lastSMSInDateL = prefs.getLong(PrefStrings.LAST_SMS_IN_DATE, 0);
                    String carrierName = prefs.getString(PrefStrings.MOBILE_CARRIER, "N/A");

                    //order is important
                    Date balanceDate = new Date(balanceDateL);
                    Date date = new Date(dateL);
                    Date lastSMSInDate = new Date(lastSMSInDateL);

                    bObj.append("date", date);
                    bObj.append("balanceDate", balanceDate);
                    bObj.append("lastSMSInDate", lastSMSInDate);
                    bObj.append("id", instance);
                    if (isPostpaid) {
                        bObj.append("balanceDue", balanceDue);
                        bObj.append("balanceCredit", balanceCredit);
                    } else {
                        bObj.append("balance", balance);
                    }

                    bObj.append("battery",battery);
                    bObj.append("temp", temp);
                    bObj.append("wifi", wifi);
                    bObj.append("plugged", plugged);
                    bObj.append("data", data);
                    bObj.append("wifiStrength", wifiStrength);
                    bObj.append("carrier", carrierName);

                    arg.add(bObj);


                    client = Stitch.getDefaultAppClient();
                    client.getAuth().loginWithCredential(new AnonymousCredential()).addOnCompleteListener(
                            new OnCompleteListener<StitchUser>() {
                                @Override
                                public void onComplete(@NonNull final Task<StitchUser> task) {
                                    if (task.isSuccessful()) {
                                        Log.d(TAG, String.format(
                                                "logged in as user %s with provider %s",
                                                task.getResult().getId(),
                                                task.getResult().getLoggedInProviderType()));
                                        Log.d(TAG, String.format("Calling function %s", stitchUpdateFunctionName + " arg: " + arg));

                                        client.callFunction(stitchUpdateFunctionName, arg, BsonValue.class)
                                                .addOnCompleteListener(new OnCompleteListener<BsonValue>() {
                                                    @Override
                                                    public void onComplete(@android.support.annotation.NonNull Task<BsonValue> upTask) {
                                                        if (upTask.isSuccessful()) {
                                                            Log.d(TAG, "Updated instance");
                                                            Log.d(TAG, upTask.getResult().toString());
                                                            stats.putLong(PrefStrings.UPD_DATE, dateL);
                                                            stats.apply();
                                                        } else {
                                                            Log.e(TAG, "failed to update for " + (oneTime ? "One Time" : "Periodic") + "request", upTask.getException());
                                                        }
                                                        try {
                                                            //client.close();
                                                            Log.d(TAG, "Not Closed Stitch Client for " + (oneTime ? "One Time" : "Periodic") + "request");
                                                        } catch (Exception ex) {
                                                            Log.e(TAG, "Error Closing Stitch Client for " + (oneTime ? "One Time" : "Periodic") + "request: " + ex.getMessage());
                                                        }
                                                    }
                                                });
                                    }
                                }
                            });
                } catch (Exception e) {
                    Log.e(TAG, e.getMessage(), e);
                    try {
                        //client.close();
                        Log.d(TAG, "Not Closed Stitch Client after error for " + (oneTime ? "One Time" : "Periodic") + "request");
                    } catch (Exception ex) {
                        Log.e(TAG, "Error Closing Stitch Client after error, new error: " + ex.getMessage());
                    }
                    return Result.FAILURE;

                }
                return Result.SUCCESS;
            }
        }
    }
}
