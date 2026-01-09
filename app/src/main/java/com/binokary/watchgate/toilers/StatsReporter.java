package com.binokary.watchgate.toilers;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.RequestFuture;
import com.android.volley.toolbox.Volley;
import com.binokary.watchgate.Constants;
import com.binokary.watchgate.PrefStrings;
import com.binokary.watchgate.StatsHelper;

import org.json.JSONObject;
import org.json.JSONException;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static android.content.Context.MODE_PRIVATE;

public class StatsReporter extends Worker {

    public static final String TAG = Constants.MAIN_TAG + StatsReporter.class.getSimpleName();


    public StatsReporter(@androidx.annotation.NonNull Context context, @androidx.annotation.NonNull WorkerParameters workerParams) {
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
            //don't even allow frequent oneTime requests to keep the number of backend connections low
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

                    // Build JSON object for the backend API

                    JSONObject statusData = new JSONObject();
                    try {
                        statusData.put("date", new Date(dateL).toString());
                        statusData.put("lastSMSInDate", new Date(lastSMSInDateL).toString());
                        statusData.put("id", instance);
                        if (balanceDateL > 0) { //Only if there is balance date
                            statusData.put("balanceDate", new Date(balanceDateL).toString());
                            if (isPostpaid) {
                                statusData.put("balanceDue", balanceDue);
                                statusData.put("balanceCredit", balanceCredit);
                            } else {
                                statusData.put("balance", balance);
                            }
                        }
                        statusData.put("battery", battery);
                        statusData.put("temp", temp);
                        statusData.put("wifi", wifi);
                        statusData.put("plugged", plugged);
                        statusData.put("data", data);
                        statusData.put("wifiStrength", wifiStrength);
                        statusData.put("carrier", carrierName);

                        if (remainingSMS > -1) {
                            statusData.put("remainingSMS", remainingSMS);
                            statusData.put("smsPackInfoDate", new Date(smsPackInfoDateL).toString());
                        }

                        // Get backend URL from preferences

                        String backendUrl = mPrefs.getString("pref_backend_url", "");
                        String apiKey = mPrefs.getString("pref_api_key", "");

                        if (backendUrl.trim().isEmpty()) {
                            Log.e(TAG, "Backend URL is not configured. Please set it in settings.");
                            Toast.makeText(getApplicationContext(), "Backend URL not configured", Toast.LENGTH_LONG).show();
                            return Result.failure();
                        } else if (apiKey.trim().isEmpty()) {
                            Log.e(TAG, "API Key is not configured. Please set it in settings.");
                            Toast.makeText(getApplicationContext(), "API Key not configured", Toast.LENGTH_LONG).show();
                            return Result.failure();
                        }
                        // Use Volley to make HTTP POST request with synchronous execution

                        RequestFuture<JSONObject> future = RequestFuture.newFuture();
                        RequestQueue queue = Volley.newRequestQueue(applicationContext);

                        JsonObjectRequest request = new JsonObjectRequest(
                                Request.Method.POST,
                                backendUrl,
                                statusData,
                                future,
                                future
                        ) {
                            @Override
                            public Map<String, String> getHeaders() {
                                Map<String, String> headers = new HashMap<>();
                                headers.put("Content-Type", "application/json");
                                if (!apiKey.isEmpty()) {
                                    headers.put("Authorization", "Bearer " + apiKey.trim());
                                }
                                return headers;
                            }
                        };

                        queue.add(request);

                        try {
                            JSONObject response = future.get(30, TimeUnit.SECONDS);

                            Log.v(TAG, "Updated instance successfully: " + response.toString());
                            stats.putLong(PrefStrings.UPD_DATE, System.currentTimeMillis());
                            stats.apply();
                            return Result.success();

                        } catch (InterruptedException | ExecutionException | TimeoutException e) {
                            Log.e(TAG, "Sync Volley Request Failed: " + e.getMessage());
                            return Result.retry(); // Now you can actually retry on failure!
                        }

                        // Note: Volley runs asynchronously. For a production app, consider using
                        // a synchronous HTTP client or implementing proper callback handling.
                        // The current implementation queues the request and returns success.
                        // Actual success/failure will be logged but not affect the Worker result.

                    } catch (JSONException e) {
                        Log.e(TAG, "Error creating JSON object", e);
                        return Result.failure();
                    }

                } catch (Exception e) {
                    Log.e(TAG, e.getMessage(), e);
                    return Result.failure();
                }
            }
        }
        return Result.success(); //or FAILURE or RETRY?
    }
}
