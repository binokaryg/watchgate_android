package com.binokary.watchgate;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import static android.content.Context.MODE_PRIVATE;
import static android.content.Context.TELEPHONY_SERVICE;
import static android.content.Context.WIFI_SERVICE;
import static com.binokary.watchgate.Constants.PREF_STATS;

public final class StatsHelper {
    public static final String TAG = Constants.MAIN_TAG + StatsHelper.class.getSimpleName();
    protected static ConnectivityManager cm;
    protected static TelephonyManager tm;
    static SharedPreferences.Editor stats;

    public StatsHelper() {
    }

    public static void CheckAndUpdateStats(Context appContext) {

        stats = appContext.getSharedPreferences(PREF_STATS, MODE_PRIVATE).edit();
        int battery;
        boolean plugged;
        int temp;
        int health;
        String wifi = "N/A";
        String carrierName;
        int wifiSignalStrength = -1;

        try {
            //battery
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(Intent.ACTION_BATTERY_CHANGED);
            Intent batteryStatus = appContext.registerReceiver(null, intentFilter);
            // Are we charging / charged?
            int pluggedStatus = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
            plugged = pluggedStatus != 0; // 0 means it is on battery, other constants are different types of power sources
            //Log.d(TAG, "Plugged: " + String.valueOf(plugged));
            stats.putBoolean(PrefStrings.PLUGGED, plugged);

            int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1); //max level
            battery = Math.round(((float) level / scale) * 100);
            stats.putInt(PrefStrings.BATTERY, battery);
            //Log.d(TAG, "Battery Level: " + String.valueOf(battery));

            //Temperature
            temp = batteryStatus.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1);
            stats.putInt(PrefStrings.TEMPERATURE, temp / 10);

            //Health
            health = batteryStatus.getIntExtra(BatteryManager.EXTRA_HEALTH, -1);
            stats.putInt(PrefStrings.HEALTH, health);
        } catch (Exception ex) {
            Log.e(TAG, "Error checking battery: " + ex.getMessage());
        }

        //Connection
        try {
            cm =
                    (ConnectivityManager) appContext.getSystemService(Context.CONNECTIVITY_SERVICE);

            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            if (activeNetwork != null) {
                boolean isWiFi = activeNetwork.getType() == ConnectivityManager.TYPE_WIFI;
                boolean isData = activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE;
                //Wifi
                if (isWiFi) {
                    WifiManager wifiManager = (WifiManager) appContext.getApplicationContext().getSystemService(WIFI_SERVICE);
                    int wifiState = wifiManager.getWifiState();
                    if (wifiState == WifiManager.WIFI_STATE_ENABLED) {
                        WifiInfo info = wifiManager.getConnectionInfo();
                        if (info != null) {
                            wifi = info.getSSID().replace("\"", "");
                            wifiSignalStrength = WifiManager.calculateSignalLevel(info.getRssi(), 5);
                            //Log.d(TAG, "Wifi Signal: " + wifiSignalStrength);
                            Log.d(TAG, "Wifi SSID: " + wifi);
                        }
                    }
                }
                stats.putBoolean(PrefStrings.MOBILE_DATA, isData);
                stats.putString(PrefStrings.WIFI_SSID, wifi);
                stats.putInt(PrefStrings.WIFI_STRENGTH, wifiSignalStrength);
            }
        } catch (Exception ex) {
            Log.e(TAG, "Error checking wifi: " + ex.getMessage());
        }

        //Mobile Network
        try {
            tm = (TelephonyManager) appContext.getSystemService(TELEPHONY_SERVICE);
            carrierName = tm.getNetworkOperatorName();
            //Log.d(TAG, "Carrier: " + carrierName);
            stats.putString(PrefStrings.MOBILE_CARRIER, carrierName);
        } catch (Exception ex) {
            Log.e(TAG, "Error checking mobile network: " + ex.getMessage());
        }
        stats.apply();
    }

    public static String DateStringFromMS(long ld) {
        Date d = new Date(ld);
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        df.setTimeZone(TimeZone.getTimeZone("Asia/Kathmandu"));
        return df.format(d);
    }

}



