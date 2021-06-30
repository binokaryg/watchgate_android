package com.binokary.watchgate.service;

import android.app.ActivityManager;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import androidx.preference.PreferenceManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.binokary.watchgate.Constants;
import com.binokary.watchgate.MainActivity;
import com.binokary.watchgate.NotificationID;
import com.binokary.watchgate.R;
import com.binokary.watchgate.SlackHelper;
import com.binokary.watchgate.toilers.WorkerUtils;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Objects;

public class GateOnFirebaseMessagingService extends FirebaseMessagingService {

    private final String TAG = Constants.MAIN_TAG + "NOTIFY";
    private SharedPreferences mSharedPreferences;

    @Override
    public void onCreate() {
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
    }

    @Override
    public void onMessageReceived(RemoteMessage message) {

        Log.d(TAG, "Message received");
        String task = Objects.requireNonNull(message.getData().get("task"));
        String body = Objects.requireNonNull(message.getData().get("body"));
        String from = message.getFrom();
        String topic = from;
        try {
            assert from != null;
            topic = from.replace("/topics/", "");
        } catch (Exception ex) {
            Log.e(TAG, "Error parsing FCM sender: " + from + ex.getMessage());
        }

        boolean isPostpaid = mSharedPreferences.getBoolean("switch_preference_1", false);
        String packageNameFromMsg = Objects.requireNonNull(message.getData().get("package"));
        String regex = "^([A-Za-z][A-Za-z\\d_]*\\.)+[A-Za-z][A-Za-z\\d_]*$";
        String packageName = packageNameFromMsg.matches(regex) ?
                packageNameFromMsg : mSharedPreferences.getString("edit_text_preference_package", "none");

        Log.d(TAG, task);
        boolean monitorOnly = mSharedPreferences.getBoolean("switch_monitor_mode", false);
        if (!monitorOnly) {
            switch (task) {
                case "KILL": {
                    Log.d(TAG, "Attempting KILL");
                    ActivityManager activityManager = (ActivityManager) this.getSystemService(ACTIVITY_SERVICE);
                    activityManager.killBackgroundProcesses(packageName);
                    Log.d(TAG, "Attempted KILL");
                    //Toast.makeText(getApplicationContext(), "Killed!", Toast.LENGTH_LONG).show();
                    break;
                }
                case "START": {
                    Log.d(TAG, "Attempting START");
                    Intent launchIntent = getPackageManager().getLaunchIntentForPackage(packageName);
                    if (launchIntent != null) {
                        startActivity(launchIntent);//null pointer check in case package name was not found
                        Log.d(TAG, "Attempted START : " + launchIntent.toString());
                    } else {
                        Log.e(TAG, "Could not attempt START: " + "intent is null");
                    }
                    break;
                }
                case "RESTART": {
                    Log.d(TAG, "Attempting RESTART " + packageName);
                    bringAppToFront();
                    SystemClock.sleep(2000);//risky?

                    ActivityManager activityManager = (ActivityManager) this.getSystemService(ACTIVITY_SERVICE);
                    activityManager.killBackgroundProcesses(packageName);
                    Intent launchIntent = getPackageManager().getLaunchIntentForPackage(packageName);
                    if (launchIntent != null) {
                        startActivity(launchIntent);//null pointer check in case package name was not found
                        Log.d(TAG, "Attempted RESTART : " + launchIntent.toString());
                    } else {
                        Log.e(TAG, "Could not attempt RESTART: " + packageName + " : intent is null");
                    }

                    new Handler(Looper.getMainLooper()).postDelayed(this::bringAppToFront, 5000);
                    break;
                }
                case "CHECK":
                    if (isPostpaid) {
                        String postpaidBalanceQueryDestination = mSharedPreferences.getString("pref_sms_destination", "1415");
                        String postpaidBalanceQuery = mSharedPreferences.getString("pref_balance_query_postpaid", "CB");
                        WorkerUtils.enqueueOneTimeSMSSendingWork(postpaidBalanceQueryDestination, postpaidBalanceQuery);
                    } else {
                        String prepaidBalanceQueryDestination = mSharedPreferences.getString("pref_sms_destination", "1415");
                        String prepaidBalanceQuery = mSharedPreferences.getString("pref_balance_query_prepaid", "BL");
                        WorkerUtils.enqueueOneTimeSMSSendingWork(prepaidBalanceQueryDestination, prepaidBalanceQuery);
                    }
                    break;
                case "SUBSCRIBE":
                    String sms_pack_destination = mSharedPreferences.getString("pref_sms_pack_destination", "1415");
                    String sms_pack_sub_code = mSharedPreferences.getString("pref_sms_sub", "SMS20");
                    WorkerUtils.enqueueOneTimeSMSSendingWork(sms_pack_destination, sms_pack_sub_code);
                    break;
            }
        }

        // Create a new notification here to show in case the app is in the foreground.
        NotificationCompat.Builder notificationBuilder;
        NotificationManager manager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationBuilder = new NotificationCompat.Builder(getApplicationContext(), "channel_fcm");

        notificationBuilder.setContentTitle("Attempted: " + (task.equals("RESTART") ? task + " " + packageName : task) + " " + (monitorOnly ? " in " + topic : ""))
                .setContentText(body)
                .setSmallIcon(R.drawable.ic_notifications_black_24dp)
                .setShowWhen(true);

        notificationBuilder.setAutoCancel(true)
                .setDefaults(NotificationCompat.DEFAULT_ALL);

        manager.notify(NotificationID.getID(), notificationBuilder.build());

        //Push to Slack Channel
        if (mSharedPreferences.getBoolean("switch_slack", false)) {

            JSONObject jsonBody = new JSONObject();

            try {
                jsonBody.put("text", topic.toUpperCase() + ": " + ((task.equals("RESTART") ? task + " :large_blue_circle: " + packageName : task) + " (" + body + ")"));
                SlackHelper.sendMessage(getApplicationContext(), jsonBody);
            } catch (
                    JSONException e) {
                Log.e(TAG, e.getMessage());
            }
        }
    }


    @Override
    public void onNewToken(@NonNull String s) {
        super.onNewToken(s);
        Log.d("Token", s);
    }

    public void bringAppToFront() {
        Log.d(TAG, "Bringing watchgate to front");
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // You need this if starting
        //  the activity from a service
        intent.setAction(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        startActivity(intent);
        Log.d(TAG, "Brought watchgate to front");
    }

}
