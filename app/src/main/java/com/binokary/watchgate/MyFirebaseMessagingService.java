package com.binokary.watchgate;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.firebase.messaging.RemoteMessage;
import com.google.firebase.messaging.FirebaseMessagingService;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    @Override
    public void onMessageReceived(RemoteMessage message) {
        RemoteMessage.Notification notification = message.getNotification();
        if (notification == null) { return; }

        // By default, notifications will only appear if the app is in the background.
        // Create a new notification here to show in case the app is in the foreground.
        Notification.Builder notificationBuilder;
        NotificationManager manager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (android.os.Build.VERSION.SDK_INT >= 26) {
            notificationBuilder = new Notification.Builder(getApplicationContext(), "channel_id");
        } else {
            notificationBuilder = new Notification.Builder(getApplicationContext());
        }

        notificationBuilder.setContentTitle(notification.getTitle())
                .setContentText(notification.getBody())
                .setSmallIcon(R.mipmap.ic_launcher);

        notificationBuilder.setAutoCancel(true)
                .setDefaults(NotificationCompat.DEFAULT_ALL);

        manager.notify(1, notificationBuilder.build());
    }

    @Override
    public void onNewToken(String s) {
        super.onNewToken(s);
        String deviceToken = s;
        Log.d("Token", deviceToken);
        // Do whatever you want with your token now
        // i.e. store it on SharedPreferences or DB
        // or directly send it to server
    }
}
