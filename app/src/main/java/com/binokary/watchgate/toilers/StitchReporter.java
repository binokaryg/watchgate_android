package com.binokary.watchgate.toilers;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.binokary.watchgate.Constants;
import com.binokary.watchgate.PrefStrings;
import com.binokary.watchgate.R;
import com.binokary.watchgate.StatsHelper;

import org.bson.BasicBSONObject;
import org.bson.BsonValue;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import io.realm.mongodb.App;
import io.realm.mongodb.AppConfiguration;
import io.realm.mongodb.Credentials;
import io.realm.mongodb.User;
import io.realm.mongodb.functions.Functions;

import static android.content.Context.MODE_PRIVATE;

public class StitchReporter extends Worker {

    public static final String TAG = Constants.MAIN_TAG + StitchReporter.class.getSimpleName();
    static App app;
    List<Object> arg = new ArrayList<>();
    BasicBSONObject bObj = new BasicBSONObject();

    public StitchReporter(@androidx.annotation.NonNull Context context, @androidx.annotation.NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Worker.Result doWork() {
        Context applicationContext = getApplicationContext();
        SharedPreferences.Editor stats = applicationContext.getSharedPreferences(Constants.PREF_STATS, MODE_PRIVATE).edit();
        boolean oneTime = getInputData().getBoolean("ONETIME", false);
        int minInterval = getInputData().getInt("MIN", 10) * 60 * 1000;
        int minOneInterval = getInputData().getInt("MIN_ONE", 3) * 60 * 1000;
        SharedPreferences prefs = applicationContext.getSharedPreferences(Constants.PREF_STATS, MODE_PRIVATE);
        SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(applicationContext);
        long lastReportAttemptDate = prefs.getLong(PrefStrings.REPORT_ATTEMPT_DATE, 0);
        long lastReportUpdateDate = prefs.getLong(PrefStrings.UPD_DATE, 0);
        long now = System.currentTimeMillis();
        if (now - lastReportAttemptDate < minOneInterval) {
            //don't even allow frequent oneTime requests to keep the number of stitch connections low
            Log.d(TAG, String.format("Not allowing to attempt %s reporting request because" +
                            " last attempt was on %s and and now it is %s", oneTime ? "One Time" : "Periodic",
                    StatsHelper.DateStringFromMS(lastReportAttemptDate), StatsHelper.DateStringFromMS(now)));

        } else {
            stats.putLong(PrefStrings.REPORT_ATTEMPT_DATE, now);
            stats.apply();

            if (!oneTime && now - lastReportUpdateDate < minInterval) {
                //Allow one time but check periodic requests
                Log.d(TAG, String.format("Not allowing to update because" +
                                " last update was on %s and and now it is %s",
                        StatsHelper.DateStringFromMS(lastReportUpdateDate), StatsHelper.DateStringFromMS(now)));
            } else {
                try {
                    StatsHelper.CheckAndUpdateStats(applicationContext);

                    String instance = getInputData().getString("INSTANCE");
                    long balanceDateL = prefs.getLong(PrefStrings.BALANCE_DATE, 0);
                    long smsPackInfoDateL = prefs.getLong(PrefStrings.SMS_PACK_INFO_DATE, 0);

                    boolean isPostpaid = mPrefs.getBoolean("switch_preference_1", false);
                    Log.d(TAG, "Is Postpaid ? " + isPostpaid);
                    Integer balance = prefs.getInt(PrefStrings.PREPAID_BALANCE, -1);
                    Integer balanceDue = prefs.getInt(PrefStrings.POSTPAID_BALANCE_DUE, -1);
                    Integer balanceCredit = prefs.getInt(PrefStrings.POSTPAID_BALANCE_CREDIT, -1);
                    int remainingSMS = prefs.getInt(PrefStrings.SMS_PACK_INFO, -1);

                    String stitchUpdateFunctionName = mPrefs.getString("pref_stitch_update_func", "updateStatusForObj");
                    long dateL = System.currentTimeMillis();
                    int battery = prefs.getInt(PrefStrings.BATTERY, -1);
                    boolean plugged = prefs.getBoolean(PrefStrings.PLUGGED, false);
                    boolean data = prefs.getBoolean(PrefStrings.MOBILE_DATA, false);
                    int temp = prefs.getInt(PrefStrings.TEMPERATURE, -1);
                    //int health = prefs.getInt(PrefStrings.HEALTH, -1);
                    String wifi = prefs.getString(PrefStrings.WIFI_SSID, "N/A");
                    int wifiStrength = prefs.getInt(PrefStrings.WIFI_STRENGTH, -1);
                    //int mobileStrength = prefs.getInt(PrefStrings.MOBILE_STRENGTH, -1);
                    long lastSMSInDateL = prefs.getLong(PrefStrings.LAST_SMS_IN_DATE, 0);
                    String carrierName = prefs.getString(PrefStrings.MOBILE_CARRIER, "N/A");

                    //order is important
                    Date balanceDate = new Date(balanceDateL);
                    Date date = new Date(dateL);
                    Date lastSMSInDate = new Date(lastSMSInDateL);
                    Date smsPackInfoDate = new Date(smsPackInfoDateL);

                    bObj.append("date", date);
                    bObj.append("lastSMSInDate", lastSMSInDate);
                    bObj.append("id", instance);
                    if (balanceDateL > 0) { //Only if there is balance date
                        bObj.append("balanceDate", balanceDate);
                        if (isPostpaid) {
                            bObj.append("balanceDue", balanceDue);
                            bObj.append("balanceCredit", balanceCredit);
                        } else {
                            bObj.append("balance", balance);
                        }
                    }

                    bObj.append("battery", battery);
                    bObj.append("temp", temp);
                    bObj.append("wifi", wifi);
                    bObj.append("plugged", plugged);
                    bObj.append("data", data);
                    bObj.append("wifiStrength", wifiStrength);
                    bObj.append("carrier", carrierName);
                    if (remainingSMS > -1) {
                        bObj.append("remainingSMS", remainingSMS);
                        bObj.append("smsPackInfoDate", smsPackInfoDate);
                    }

                    arg.add(bObj);

                    app = new App(new AppConfiguration.Builder(applicationContext.getString(R.string.stitch_client_app_id))
                            .build());

                    String realmKey = mPrefs.getString("realm_key_override", "");
                    if(realmKey.trim().isEmpty()) {
                        realmKey = applicationContext.getResources().getString(R.string.realm_key_default);
                    }

                    Credentials credentials = Credentials.apiKey(realmKey);
                    try {
                        app.login(credentials);
                        User user = app.currentUser();
                        assert user != null;
                        Functions functionsManager = app.getFunctions(user);
                        try {
                            BsonValue result = functionsManager.callFunction(stitchUpdateFunctionName, arg, BsonValue.class);
                            Log.v(TAG, "Updated instance");
                            Log.v(TAG, result.toString());
                        } catch (Exception ex) {
                            Log.e(TAG, "failed to update for " + (oneTime ? "One Time" : "Periodic") + "request", ex);
                        }
                    } catch (Exception ex) {
                        Log.e(TAG, "Error logging into the Realm app. Make sure that API key is set. Error: " + ex.getMessage());
                        Toast.makeText(getApplicationContext(), "Error logging in with API Key", Toast.LENGTH_LONG).show();
                    }
                } catch (Exception e) {
                    Log.e(TAG, e.getMessage(), e);
                    try {
                        //client.close();
                        Log.d(TAG, "Not Closed Stitch Client after error for " + (oneTime ? "One Time" : "Periodic") + "request");
                    } catch (Exception ex) {
                        Log.e(TAG, "Error Closing Stitch Client after error, new error: " + ex.getMessage());
                    }
                    return Result.failure();

                }
            }
        }
        return Result.success(); //or FAILURE or RETRY?
    }
}
