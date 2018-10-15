package com.binokary.watchgate.service;

import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.provider.Telephony;
import android.util.Log;

//The background service is disabled and not being used
public class GateBackgroundService extends Service {
    private static final String TAG = "GateBackground";
    //private SMSBroadcastReceiver smsReceiver = null;
    public GateBackgroundService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }
    @Override
    public void onCreate() {
        super.onCreate();

        // Create an IntentFilter instance.
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Telephony.Sms.Intents.SMS_RECEIVED_ACTION);

        // Create a network change broadcast receiver.
        //smsReceiver = new SMSBroadcastReceiver();

        // Register the broadcast receiver with the intent filter object.
        //registerReceiver(smsReceiver, intentFilter);

        Log.d(TAG, "Service onCreate: aReceiver is registered.");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // Unregister aReceiver when destroy.
        //if(smsReceiver!=null)
        {
    //        unregisterReceiver(smsReceiver);
            Log.d(TAG, "Service onDestroy: aReceiver is unregistered.");
        }
    }

}