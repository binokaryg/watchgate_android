package com.binokary.watchgate.service;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.binokary.watchgate.Constants;
import com.binokary.watchgate.NotificationID;
import com.binokary.watchgate.R;
import com.google.firebase.messaging.RemoteMessage;
import com.google.firebase.messaging.FirebaseMessagingService;

import java.util.Map;

public class GateOnFirebaseMessagingService extends FirebaseMessagingService {

    private SharedPreferences mSharedPreferences;
    private String TAG = Constants.MAINTAG + "NOTIFY";

    @Override
    public void onCreate() {
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
    }

    @Override
    public void onMessageReceived(RemoteMessage message) {

        Log.d(TAG, "Message received");
        Map data = message.getData();
        String task = data.get("task").toString();
        String body = data.get("body").toString();
        String package_name = mSharedPreferences.getString("edit_text_preference_package", "none");
        Log.d(TAG, task);
        if (task.equals("KILL")) {
            Log.d(TAG, "Attempting KILL");
            ActivityManager activityManager = (ActivityManager) this.getSystemService(ACTIVITY_SERVICE);
            activityManager.killBackgroundProcesses("medic.gateway.alert");
            Log.d(TAG, "Attempted KILL");
            //Toast.makeText(getApplicationContext(), "Killed!", Toast.LENGTH_LONG).show();
        } else if (task.equals("RESTART")) {
            Log.d(TAG, "Attempting RESTART");
            ComponentName componentName = getPackageManager().getLaunchIntentForPackage(package_name).getComponent();
            Intent.makeRestartActivityTask(componentName);
            Log.d(TAG, "Attempted RESTART: " + componentName.toString());
        }

        // Create a new notification here to show in case the app is in the foreground.
        NotificationCompat.Builder notificationBuilder;
        NotificationManager manager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationBuilder = new NotificationCompat.Builder(getApplicationContext(), "channel_id");

        notificationBuilder.setContentTitle("Attempted: " + task)
                .setContentText(body)
                .setSmallIcon(R.drawable.ic_notifications_black_24dp)
                .setShowWhen(true);

        notificationBuilder.setAutoCancel(true)
                .setDefaults(NotificationCompat.DEFAULT_ALL);

        manager.notify(NotificationID.getID(), notificationBuilder.build());
    }

    public int getNotificationIDByTask(String task) {
        switch (task) {
            case "KILL":
                return 1;
            case "RESTART":
                return 2;
            default:
                return 0;
        }
    }

    @Override
    public void onNewToken(String s) {
        super.onNewToken(s);
        String deviceToken = s;
        Log.d("Token", deviceToken);
    }

}
