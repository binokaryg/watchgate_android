package com.binokary.watchgate.service;

import android.app.ActivityManager;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.SystemClock;
import android.preference.PreferenceManager;

import androidx.core.app.NotificationCompat;

import android.util.Log;

import com.binokary.watchgate.Constants;
import com.binokary.watchgate.MainActivity;
import com.binokary.watchgate.NotificationID;
import com.binokary.watchgate.R;
import com.binokary.watchgate.SlackHelper;
import com.binokary.watchgate.toilers.WorkerUtils;
import com.google.firebase.messaging.RemoteMessage;
import com.google.firebase.messaging.FirebaseMessagingService;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

public class GateOnFirebaseMessagingService extends FirebaseMessagingService {

    private SharedPreferences mSharedPreferences;
    private String SLACK_HOOK;
    private String TAG = Constants.MAINTAG + "NOTIFY";

    Handler handler = new Handler();

    @Override
    public void onCreate() {
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        SLACK_HOOK = getResources().getString(R.string.slack_url);
    }

    @Override
    public void onMessageReceived(RemoteMessage message) {

        Log.d(TAG, "Message received");
        Map data = message.getData();
        String task = data.get("task").toString();
        String body = data.get("body").toString();
        String from = message.getFrom();
        String topic = from;
        try {
            topic = from.replace("/topics/", "");
        } catch (Exception ex) {
            Log.e(TAG, "Error parsing FCM sender: " + from + ex.getMessage());
        }

        Boolean isPostpaid = mSharedPreferences.getBoolean("switch_preference_1", false);
        String packageNameFromMsg = data.get("package").toString();
        String regex = "^([A-Za-z]{1}[A-Za-z\\d_]*\\.)+[A-Za-z][A-Za-z\\d_]*$";
        String packageName = packageNameFromMsg.matches(regex) ?
                packageNameFromMsg : mSharedPreferences.getString("edit_text_preference_package", "none");

        Log.d(TAG, task);
        Boolean monitorOnly = mSharedPreferences.getBoolean("switch_monitor_mode", false);
        if (!monitorOnly) {
            if (task.equals("KILL")) {
                Log.d(TAG, "Attempting KILL");
                ActivityManager activityManager = (ActivityManager) this.getSystemService(ACTIVITY_SERVICE);
                activityManager.killBackgroundProcesses(packageName);
                Log.d(TAG, "Attempted KILL");
                //Toast.makeText(getApplicationContext(), "Killed!", Toast.LENGTH_LONG).show();
            } else if (task.equals("START")) {
                Log.d(TAG, "Attempting START");
                Intent launchIntent = getPackageManager().getLaunchIntentForPackage(packageName);
                if (launchIntent != null) {
                    startActivity(launchIntent);//null pointer check in case package name was not found
                    Log.d(TAG, "Attempted START : " + launchIntent.toString());
                } else {
                    Log.e(TAG, "Could not attempt START: " + "intent is null");
                }
            } else if (task.equals("RESTART")) {
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
                handler.postDelayed(new Runnable() {
                    public void run() {
                        bringAppToFront();
                    }
                }, 5000);   //5 seconds
            } else if (task.equals("CHECK")) {
                if (isPostpaid) {
                    String postpaidBalanceQueryDestination = mSharedPreferences.getString("pref_sms_destination", "1415");
                    String postpaidBalanceQuery = mSharedPreferences.getString("pref_balance_query_postpaid", "CB");
                    WorkerUtils.enqueueOneTimeSMSSendingWork(postpaidBalanceQueryDestination, postpaidBalanceQuery);
                } else {
                    String prepaidBalanceQueryDestination = mSharedPreferences.getString("pref_sms_destination", "1415");
                    String prepaidBalanceQuery = mSharedPreferences.getString("pref_balance_query_prepaid", "BL");
                    WorkerUtils.enqueueOneTimeSMSSendingWork(prepaidBalanceQueryDestination, prepaidBalanceQuery);
                }
            } else if (task.equals("SUBSCRIBE")) {
                String sms_pack_destination = mSharedPreferences.getString("pref_sms_pack_destination", "1415");
                String sms_pack_sub_code = mSharedPreferences.getString("pref_sms_sub", "SMS20");
                WorkerUtils.enqueueOneTimeSMSSendingWork(sms_pack_destination, sms_pack_sub_code);
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
    public void onNewToken(String s) {
        super.onNewToken(s);
        String deviceToken = s;
        Log.d("Token", deviceToken);
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
