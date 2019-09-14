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

import org.bson.BsonDateTime;
import org.bson.BsonInt32;
import org.bson.BsonInt64;
import org.bson.BsonNull;
import org.bson.BsonString;
import org.bson.BsonValue;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import static android.content.Context.MODE_PRIVATE;
import static android.content.Context.TELEPHONY_SERVICE;
import static android.content.Context.WIFI_SERVICE;
import static com.binokary.watchgate.Constants.PREF_STATS;

public final class StatsHelper {
    public static final String TAG = Constants.MAINTAG + StatsHelper.class.getSimpleName();
    private static Context applicationContext;
    protected static ConnectivityManager cm;
    protected static TelephonyManager tm;
    static SharedPreferences.Editor stats;

    public StatsHelper() {
    }

    public static void CheckAndUpdateStats(Context appContext) {
        applicationContext = appContext;

        stats = applicationContext.getSharedPreferences(PREF_STATS, MODE_PRIVATE).edit();
        Long date = System.currentTimeMillis();
        int battery = -1;
        boolean plugged = false;
        boolean data = false;
        int temp = -1;
        int health = -1;
        String wifi = "N/A";
        String carrierName = "";
        int wifiSignalStrength = -1;
        int mobileSignalStrength = -1;

        try {
            //battery
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(Intent.ACTION_BATTERY_CHANGED);
            Intent batteryStatus = applicationContext.registerReceiver(null, intentFilter);
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
                    (ConnectivityManager) applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE);

            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            if (activeNetwork != null) {
                boolean isConnected = activeNetwork != null &&
                        activeNetwork.isConnectedOrConnecting();
                boolean isWiFi = activeNetwork.getType() == ConnectivityManager.TYPE_WIFI;
                boolean isData = activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE;
                //Wifi
                if (isWiFi) {
                    WifiManager wifiManager = (WifiManager) applicationContext.getApplicationContext().getSystemService(WIFI_SERVICE);
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
            tm = (TelephonyManager) applicationContext.getSystemService(TELEPHONY_SERVICE);
            carrierName = tm.getNetworkOperatorName();
            //Log.d(TAG, "Carrier: " + carrierName);
            stats.putString(PrefStrings.MOBILE_CARRIER, carrierName);

            //Mobile Signal Strength not working in Huawei phone (checked Huawei LMO-N31)
            // using PhoneStateListener & onSignalStrengthsChanged, signalStrength gave
            // value of 0 in getGsmSignalStrength. Signal value was given by mWcdmaRscpasu but may be
            // applicable to WCDMA network only
            /*
            if (ActivityCompat.checkSelfPermission(applicationContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // do nothing
                Log.d(TAG, "No Location Access");
            } else if(tm.getAllCellInfo().size() > 0){
                if (tm.getAllCellInfo().get(0).getClass() == CellInfoGsm.class) {
                    mobileSignalStrength = (((CellInfoGsm) tm.getAllCellInfo().get(0)).getCellSignalStrength().getLevel());
                } else if (tm.getAllCellInfo().get(0).getClass() == CellInfoWcdma.class) {
                    mobileSignalStrength = (((CellInfoWcdma) tm.getAllCellInfo().get(0)).getCellSignalStrength().getLevel());
                } else if (tm.getAllCellInfo().get(0).getClass() == CellInfoLte.class) {
                    mobileSignalStrength = (((CellInfoLte) tm.getAllCellInfo().get(0)).getCellSignalStrength().getLevel());
                }
                stats.putInt(PrefStrings.MOBILE_STRENGTH, mobileSignalStrength);
                //Log.d(TAG, "Mobile Signal:" + mobileSignalStrength);
            }
            else {
                Log.d(TAG, "Can not read cell info");
            }
            */
        } catch (Exception ex) {
            Log.e(TAG, "Error checking mobile network: " + ex.getMessage());
        }
        stats.apply();
    }

    public static String DateStringFromMS(long ld) {
        Date d = new Date(ld);
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        df.setTimeZone(TimeZone.getTimeZone("Asia/Kathmandu"));
        String dateString = df.format(d);
        return dateString;
    }

    public static BsonValue objToBsonValue(Object obj) {
        if (obj instanceof Integer) {
            return new BsonInt32((Integer) obj);
        }

        if (obj instanceof String) {
            return new BsonString((String) obj);
        }

        if (obj instanceof Long) {
            return new BsonInt64((Long) obj);
        }

        if (obj instanceof Date) {
            return new BsonDateTime(((Date) obj).getTime());
        }
        return new BsonNull();
    }

}



