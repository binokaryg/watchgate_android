package com.binokary.watchgate;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import static android.content.Context.MODE_PRIVATE;
import static android.content.Context.TELEPHONY_SERVICE;
import static android.content.Context.WIFI_SERVICE;
import static com.binokary.watchgate.Constants.PREF_STATS;

public final class StatsHelper {
    public static final String TAG = Constants.MAIN_TAG + StatsHelper.class.getSimpleName();
    static SharedPreferences.Editor stats;

    public StatsHelper() {
    }

    public static void CheckAndUpdateStats(Context appContext) {
        stats = appContext.getSharedPreferences(PREF_STATS, MODE_PRIVATE).edit();
        int battery = -1;
        boolean plugged;
        int temp;
        int health;
        String wifi = "N/A";
        String carrierName;
        int wifiSignalStrength = -1;

        try {
            // Battery (Sticky Intent - no need to unregister)
            IntentFilter intentFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            Intent batteryStatus = appContext.registerReceiver(null, intentFilter);

            if (batteryStatus != null) {
                int pluggedStatus = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
                plugged = pluggedStatus != 0;
                stats.putBoolean(PrefStrings.PLUGGED, plugged);

                int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                if (scale > 0) {
                    battery = Math.round(((float) level / scale) * 100);
                }
                stats.putInt(PrefStrings.BATTERY, battery);

                temp = batteryStatus.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1);
                stats.putInt(PrefStrings.TEMPERATURE, temp / 10);

                health = batteryStatus.getIntExtra(BatteryManager.EXTRA_HEALTH, -1);
                stats.putInt(PrefStrings.HEALTH, health);
            }
        } catch (Exception ex) {
            Log.e(TAG, "Error checking battery: " + ex.getMessage());
        }

        // Connection
        try {
            ConnectivityManager cm = (ConnectivityManager) appContext.getSystemService(Context.CONNECTIVITY_SERVICE);
            int result = 0; // 0: none; 1: mobile; 2: wifi; 3: vpn

            if (cm != null) {
                android.net.Network network = cm.getActiveNetwork();
                if (network != null) {
                    NetworkCapabilities capabilities = cm.getNetworkCapabilities(network);
                    if (capabilities != null) {
                        if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                            result = 2;
                        } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                            result = 1;
                        } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) {
                            result = 3;
                        }
                    }
                }
            }

            boolean isData = (result == 1);

            // WiFi Logic
            if (result == 2) {
                WifiManager wifiManager = (WifiManager) appContext.getApplicationContext().getSystemService(WIFI_SERVICE);
                WifiInfo info = wifiManager.getConnectionInfo();
                if (info != null) {
                    wifiSignalStrength = calculateSignalLevelOwn(info.getRssi());

                    // Permission Safe Check for SSID
                    String rawSsid = info.getSSID();
                    // Check if we have permission OR if the OS is old enough not to care
                    boolean hasLocPerm = Build.VERSION.SDK_INT < Build.VERSION_CODES.O_MR1 ||
                            (androidx.core.content.ContextCompat.checkSelfPermission(appContext, android.Manifest.permission.ACCESS_FINE_LOCATION)
                                    == android.content.pm.PackageManager.PERMISSION_GRANTED);

                    if (hasLocPerm && rawSsid != null && !rawSsid.contains("<unknown ssid>")) {
                        wifi = rawSsid.replace("\"", "");
                    }
                    Log.d(TAG, "Wifi SSID: " + wifi);
                }
            }

            stats.putBoolean(PrefStrings.MOBILE_DATA, isData);
            stats.putString(PrefStrings.WIFI_SSID, wifi);
            stats.putInt(PrefStrings.WIFI_STRENGTH, wifiSignalStrength);

        } catch (Exception ex) {
            Log.e(TAG, "Error checking wifi: " + ex.getMessage());
        }

        // Mobile Network
        int mobileSignalStrength; // Default to -1 (unknown)

        try {
            TelephonyManager tm = (TelephonyManager) appContext.getSystemService(TELEPHONY_SERVICE);
            if (tm != null) {
                // 1. Get Carrier Name
                carrierName = tm.getNetworkOperatorName();
                if (carrierName == null || carrierName.isEmpty()) carrierName = "N/A";
                stats.putString(PrefStrings.MOBILE_CARRIER, carrierName);

                // 2. Get Signal Strength (Requires Location Permission)
                if (androidx.core.content.ContextCompat.checkSelfPermission(appContext, android.Manifest.permission.ACCESS_FINE_LOCATION)
                        == android.content.pm.PackageManager.PERMISSION_GRANTED) {

                    // API 28+ (Android 9.0) allows synchronous signal retrieval
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        android.telephony.SignalStrength strength = tm.getSignalStrength();
                        if (strength != null) {
                            // Returns an integer from 0 (very poor) to 4 (excellent)
                            mobileSignalStrength = strength.getLevel();
                            stats.putInt(PrefStrings.MOBILE_STRENGTH, mobileSignalStrength);
                        }
                    }
                }
            }
        } catch (Exception ex) {
            Log.e(TAG, "Error checking mobile network: " + ex.getMessage());
        }

        stats.apply();
    }

    public static String DateStringFromMS(long ld) {
        Date d = new Date(ld);
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
        df.setTimeZone(TimeZone.getTimeZone("UTC"));
        return df.format(d);
    }

    private static int calculateSignalLevelOwn(int rssi) {
        final int MIN_RSSI = -100;
        final int MAX_RSSI = -55;
        final int numLevels = 5;

        if (rssi <= MIN_RSSI) {
            return 0;
        } else if (rssi >= MAX_RSSI) {
            return numLevels - 1;
        } else {
            float inputRange = (MAX_RSSI - MIN_RSSI);
            float outputRange = (numLevels - 1);
            return (int) ((float) (rssi - MIN_RSSI) * outputRange / inputRange);
        }
    }

}



