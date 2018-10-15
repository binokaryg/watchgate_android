package com.binokary.watchgate;

import android.Manifest;
import android.app.AlertDialog;
import android.arch.lifecycle.LiveData;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.preference.PreferenceManager;
import android.provider.Telephony;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.binokary.watchgate.toilers.WorkerUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.util.List;
import java.util.UUID;

import androidx.work.WorkManager;
import androidx.work.WorkStatus;


public class MainActivity extends AppCompatActivity {
    private static final String TAG = Constants.MAINTAG + "MainActivity";
    public static final String PREF_FILE_NAME = "pref_general";
    private static final String PREF_USER_MOBILE_PHONE = "edit_number_preference";
    private static final String PREF_STATS = "gate_stats";
    private static final int SMS_PERMISSION_CODE = 0;
    private static final int MY_PERMISSIONS_REQUEST_READ_PHONE_STATE = 1;

    private static MainActivity mainActivityInstance;
    private String mUserMobilePhone;
    private SharedPreferences mSharedPreferences;

    TextView textView;
    ProgressBar progressBar;

    WorkManager mWorkManager;
    LiveData<List<WorkStatus>> mWorkLiveData;

    private UUID workId;

    TextView levelView, healthView, tempView, pluggedView, wifiView, mobileView, networkView, spaceView, balanceView;

    static final String API_URL = "http://10.0.2.2:3000/api/";
    private long lastSmsInTime = 0;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.settings:
                Intent intent = new Intent(this, SettingsActivity.class);
                this.startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainActivityInstance = this;
        setContentView(R.layout.activity_main);
        textView = (TextView) findViewById(R.id.textView);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);

        levelView = (TextView) findViewById(R.id.textViewLevel);
        healthView = (TextView) findViewById(R.id.textViewHealth);
        tempView = (TextView) findViewById(R.id.textViewTemperature);
        pluggedView = (TextView) findViewById(R.id.textViewPlugged);
        wifiView = (TextView) findViewById(R.id.textViewWifi);
        mobileView = (TextView) findViewById(R.id.textViewMobile);
        networkView = (TextView) findViewById(R.id.textViewNetwork);
        spaceView = (TextView) findViewById(R.id.textViewFreeSpace);
        balanceView = (TextView) findViewById(R.id.textViewBalance);
        //mReceiver = new BatteryBroadcastReceiver();
        //aReceiver = new AllBroadcastReceiver();
        //sReceiver = new SMSBroadcastReceiver();

        Button normalSMSBtn = (Button) findViewById(R.id.btn_normal_sms);
        Button startButton = (Button) findViewById(R.id.buttonStart);
        Button stopButton = (Button) findViewById(R.id.buttonStop);
        Button infoButton = (Button) findViewById(R.id.buttonInfo);

        PreferenceManager.setDefaultValues(this, R.xml.pref_general, false);
        PreferenceManager.setDefaultValues(this, R.xml.pref_interval, false);
        PreferenceManager.setDefaultValues(this, R.xml.pref_variables, true);
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        //setTitle("Keep BroadcastReceiver Running After App Exit.");

        //Intent backgroundService = new Intent(getApplicationContext(), GateBackgroundService.class);
        //startService(backgroundService);

        //Log.d(TAG, "Activity onCreate: Background service started");

        normalSMSBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!hasValidPreConditions()) return;
                progressBar.setVisibility(View.VISIBLE);
                StatsHelper.CheckAndUpdateStats(getApplicationContext());
                UpdateViews();
                String smsQueryMsg = mSharedPreferences.getBoolean("switch_preference_1", false) ? "cb" : "bl";
                SMSHelper.sendSms("1415", smsQueryMsg);
                Toast.makeText(getApplicationContext(), R.string.toast_sending_sms + (mSharedPreferences.getBoolean("switch_preference_1", true) ? "Postpaid" : "Prepaid"), Toast.LENGTH_SHORT).show();
            }
        });

        startButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                JSONObject jsonData = new JSONObject();
                try {
                    jsonData.put("level", "25");
                    jsonData.put("timestamp", System.currentTimeMillis());
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                String smsQueryMsg = mSharedPreferences.getBoolean("switch_preference_1", false) ? "cb" : "bl";
                String smsIntervalString = mSharedPreferences.getString("pref_interval_sms", "240");
                String smsIntervalMinString = mSharedPreferences.getString("pref_interval_sms_min", "10");
                String reportIntervalString = mSharedPreferences.getString("pref_interval_report", "30");
                String reportIntervalMinString = mSharedPreferences.getString("pref_interval_report_min", "10");
                String reportOneIntervalMinString = mSharedPreferences.getString("pref_interval_report_one_min", "3");

                String instanceName = mSharedPreferences.getString("instance_name", "none");

                int smsInterval = 240;
                int smsIntervalMin = 10;
                int reportInterval = 30;
                int reportIntervalMin = 10;
                int reportOneIntervalMin = 3;
                try {
                    smsInterval = Integer.parseInt(smsIntervalString);
                    reportInterval = Integer.parseInt(reportIntervalString);
                    smsIntervalMin = Integer.parseInt(smsIntervalMinString);
                    reportIntervalMin = Integer.parseInt(reportIntervalMinString);
                    reportOneIntervalMin = Integer.parseInt(reportOneIntervalMinString);

                } catch (Exception ex) {
                    Log.e(TAG, "Error parsing intervals: " + ex.getMessage());
                }
                //Log.d(TAG, "sending report");
                mWorkManager = WorkManager.getInstance();

                WorkerUtils.enqueueSMSSendingWork("1415", smsQueryMsg, smsInterval, smsIntervalMin);
                WorkerUtils.enqueueStitchReportingWork(instanceName, reportInterval, reportIntervalMin, reportOneIntervalMin);
            }

        });


        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, " gatewatch stopping worker");
                if (mWorkManager == null) {
                    mWorkManager = WorkManager.getInstance();
                }
                if (mWorkManager != null) {
                    mWorkLiveData = mWorkManager.getStatusesByTag(Constants.SMSTAG);
                    mWorkLiveData.observe(MainActivity.this, listOfWorkStatuses -> {
                        for (int i = 0; i < listOfWorkStatuses.size(); i++) {
                            Log.d(TAG, "Work state of gatewatch" + i + " : " + listOfWorkStatuses.get(i).getState());
                        }
                    });
                    int clearStatus = WorkerUtils.ClearTasks(Constants.SMSTAG);
                    Log.d(TAG, " SMS sending tasks cleared " + clearStatus);

                    mWorkLiveData = mWorkManager.getStatusesByTag(Constants.REPORTTAG);
                    mWorkLiveData.observe(MainActivity.this, listOfWorkStatuses -> {
                        for (int i = 0; i < listOfWorkStatuses.size(); i++) {
                            Log.d(TAG, "Work state of gatewatch" + i + " : " + listOfWorkStatuses.get(i).getState());
                        }
                    });
                    clearStatus = WorkerUtils.ClearTasks(Constants.SMSTAG);
                    Log.d(TAG, " Stitch reporting tasks cleared " + clearStatus);
                }
            }
        });

        infoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                StringBuilder infoBuilder = new StringBuilder("Info:\n");


                if (mWorkManager == null) {
                    mWorkManager = WorkManager.getInstance();
                }
                if (mWorkManager != null) {
                    //SMS Workers
                    Log.d(TAG, "Getting workers with Tag: " + Constants.SMSTAG + "\n");
                    mWorkLiveData = mWorkManager.getStatusesByTag(Constants.SMSTAG);
                    mWorkLiveData.observe(MainActivity.this, listOfWorkStatuses -> {
                        for (int i = 0; i < listOfWorkStatuses.size(); i++) {
                            Log.d(TAG, "Work state of SMS Senders " + i + " : " + listOfWorkStatuses.get(i).getState());
                            infoBuilder.append("SMSS" + i);
                            infoBuilder.append(": ");
                            infoBuilder.append(listOfWorkStatuses.get(i).getState());
                            infoBuilder.append("\n");
                        }
                        textView.setText(infoBuilder);

                    });

                    //Report Workers
                    Log.d(TAG, "Getting workers with Tag: " + Constants.REPORTTAG + "\n");
                    mWorkLiveData = mWorkManager.getStatusesByTag(Constants.REPORTTAG);
                    mWorkLiveData.observe(MainActivity.this, listOfWorkStatuses -> {
                        for (int i = 0; i < listOfWorkStatuses.size(); i++) {
                            Log.d(TAG, "Work state of Stitch Reporters " + i + " : " + listOfWorkStatuses.get(i).getState());
                            infoBuilder.append("SR" + i);
                            infoBuilder.append(": ");
                            infoBuilder.append(listOfWorkStatuses.get(i).getState());
                            infoBuilder.append("\n");
                        }
                        textView.setText(infoBuilder);

                    });

                    //One Time SMS Workers
                    Log.d(TAG, "Getting workers with Tag: " + Constants.SMSONETAG + "\n");
                    mWorkLiveData = mWorkManager.getStatusesByTag(Constants.SMSONETAG);
                    mWorkLiveData.observe(MainActivity.this, listOfWorkStatuses -> {
                        for (int i = 0; i < listOfWorkStatuses.size(); i++) {
                            Log.d(TAG, "Work state of SMS Senders " + i + " : " + listOfWorkStatuses.get(i).getState());
                            infoBuilder.append("SMS1S" + i);
                            infoBuilder.append(": ");
                            infoBuilder.append(listOfWorkStatuses.get(i).getState());
                            infoBuilder.append("\n");
                        }
                        textView.setText(infoBuilder);

                    });

                    //One Time Report Workers
                    Log.d(TAG, "Getting workers with Tag: " + Constants.REPORTONETAG + "\n");
                    mWorkLiveData = mWorkManager.getStatusesByTag(Constants.REPORTONETAG);
                    mWorkLiveData.observe(MainActivity.this, listOfWorkStatuses -> {
                        for (int i = 0; i < listOfWorkStatuses.size(); i++) {
                            Log.d(TAG, "Work state of Stitch Reporters " + i + " : " + listOfWorkStatuses.get(i).getState());
                            infoBuilder.append("S1R" + i);
                            infoBuilder.append(": ");
                            infoBuilder.append(listOfWorkStatuses.get(i).getState());
                            infoBuilder.append("\n");
                        }
                        textView.setText(infoBuilder);
                    });

                }
                //Stats
                StatsHelper.CheckAndUpdateStats(getApplicationContext());
                UpdateViews();
            }
        });


        if (!SMSHelper.hasAllNecessaryPermissions(MainActivity.this)) {
            showRequestPermissionsInfoAlertDialog();
        }
        mUserMobilePhone = mSharedPreferences.getString(PREF_USER_MOBILE_PHONE, "");
    }

    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Activity onDestroy");
    }


    @Override
    protected void onStart() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Telephony.Sms.Intents.SMS_RECEIVED_ACTION);
        //Log.d(TAG, "gatewatch: not registering sReceiver");
        //registerReceiver(sReceiver, intentFilter); //registered in Manifest
        try {
            long freeMemory = freeMemory();
            long totalMemory = totalMemory();
            String freeSpaceMsg = String.format("%d MB free of %d MB total", freeMemory, totalMemory);
            spaceView.setText(freeSpaceMsg);
        } catch (Exception e) {
            throw e;
        }
        super.onStart();
    }

    @Override
    protected void onResume() {
        StatsHelper.CheckAndUpdateStats(getApplicationContext());
        UpdateViews();
        super.onResume();
    }

    @Override
    protected void onStop() {
        //Log.d(TAG, "gatewatch: not unregistering mReceiver");
        //unregisterReceiver(mReceiver);
        super.onStop();
    }

    public static MainActivity getInstance() {
        return mainActivityInstance;
    }

    /**
     * Checks if stored SharedPreferences value needs updating and updates \o/
     */
    private void checkAndUpdateUserPrefNumber() {
        mUserMobilePhone = mSharedPreferences.getString(PREF_USER_MOBILE_PHONE, "");

        Log.d(TAG, mUserMobilePhone);
    }


    /**
     * Validates if the app has readSmsPermissions and the mobile phone is valid
     *
     * @return boolean validation value
     */
    private boolean hasValidPreConditions() {
        if (!hasReadSmsPermission()) {
            requestReadAndSendSmsPermission();
            return false;
        }

        return true;
    }

    /**
     * Runtime permission shenanigans
     */
    private boolean hasReadSmsPermission() {
        return ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(MainActivity.this,
                        Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestReadAndSendSmsPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.RECEIVE_SMS)) {
            Log.d(TAG, "shouldShowRequestPermissionRationale(), no permission requested");
            return;
        }

        //ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.RECEIVE_SMS},
        //              SMS_PERMISSION_CODE);
        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_SMS,
                        Manifest.permission.RECEIVE_SMS, Manifest.permission.SEND_SMS,
                        Manifest.permission.READ_PHONE_STATE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.ACCESS_COARSE_LOCATION},
                SMS_PERMISSION_CODE);
    }


    public void showRequestPermissionsInfoAlertDialog() {
        showRequestPermissionsInfoAlertDialog(true);
    }

    /**
     * Displays an AlertDialog explaining the user why the SMS permission is going to be requests
     *
     * @param makeSystemRequest if set to true the system permission will be shown when the dialog is dismissed.
     */
    public void showRequestPermissionsInfoAlertDialog(final boolean makeSystemRequest) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.permission_alert_dialog_title); // Your own title
        builder.setMessage(R.string.permission_dialog_message); // Your own message

        builder.setPositiveButton(R.string.action_ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                // Display system runtime permission request?
                if (makeSystemRequest) {
                    requestReadAndSendSmsPermission();
                }
            }
        });

        builder.setCancelable(false);
        builder.show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case SMS_PERMISSION_CODE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // SMS related task you need to do.

                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }

            case MY_PERMISSIONS_REQUEST_READ_PHONE_STATE: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // SMS related task you need to do.

                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }
            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    private String ConvHealth(int health) {
        String result;
        switch (health) {
            case BatteryManager.BATTERY_HEALTH_COLD:
                result = "COLD";
                break;
            case BatteryManager.BATTERY_HEALTH_DEAD:
                result = "DEAD";
                break;
            case BatteryManager.BATTERY_HEALTH_GOOD:
                result = "GOOD";
                break;
            case BatteryManager.BATTERY_HEALTH_OVERHEAT:
                result = "OVERHEAT";
                break;
            case BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE:
                result = "OVER VOLTAGE";
                break;
            case BatteryManager.BATTERY_HEALTH_UNKNOWN:
                result = "UNKNOWN";
                break;
            case BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE:
                result = "UNSPECIFIED FAILURE";
                break;
            default:
                result = "unknown";
        }
        return result;
    }

    private long totalMemory() {
       /* long root, data, external;
        StatFs statFs = new StatFs(Environment.getDataDirectory().getAbsolutePath());
        data  = (statFs.getTotalBytes()/(1024*1024));
        statFs = new StatFs(Environment.getRootDirectory().getAbsolutePath());
        root = (statFs.getTotalBytes()/(1024*1024));
        statFs = new StatFs(Environment.getExternalStorageDirectory().getAbsolutePath());
        external = (statFs.getTotalBytes()/(1024*1024));*/

        StatFs statFs = new StatFs(Environment.getDataDirectory().getAbsolutePath());
        long total = (statFs.getTotalBytes() / (1024 * 1024));

        return total;
    }

    private long freeMemory() {
       /* long root, data, external;
        StatFs statFs = new StatFs(Environment.getDataDirectory().getAbsolutePath());
        data  = (statFs.getFreeBytes()/(1024*1024));
        statFs = new StatFs(Environment.getRootDirectory().getAbsolutePath());
        root = (statFs.getFreeBytes()/(1024*1024));
        statFs = new StatFs(Environment.getExternalStorageDirectory().getAbsolutePath());
        external = (statFs.getFreeBytes()/(1024*1024));*/

        StatFs statFs = new StatFs(Environment.getDataDirectory().getAbsolutePath());
        long free = (statFs.getFreeBytes() / (1024 * 1024));
        return free;
    }


    public void UpdateViews() {
        SharedPreferences prefs = getSharedPreferences(PREF_STATS, MODE_PRIVATE);
        Integer balance = prefs.getInt(PrefStrings.PREPAID_BALANCE, -1);
        Integer balanceDue = prefs.getInt(PrefStrings.POSTPAID_BALANCE_DUE, -1);
        Integer balanceCredit = prefs.getInt(PrefStrings.POSTPAID_BALANCE_CREDIT, -1);

        String wifi = prefs.getString(PrefStrings.WIFI_SSID, "N/A");
        Boolean data = prefs.getBoolean(PrefStrings.MOBILE_DATA, false);
        Integer temp = prefs.getInt(PrefStrings.TEMPERATURE, -1);
        Integer health = prefs.getInt(PrefStrings.HEALTH, -1);
        Boolean plugged = prefs.getBoolean(PrefStrings.PLUGGED, false);
        Integer battery = prefs.getInt(PrefStrings.BATTERY, -1);
        String carrier = prefs.getString(PrefStrings.MOBILE_CARRIER, "");
        //Integer mobileStrength = prefs.getInt(PrefStrings.MOBILE_STRENGTH, -1);
        Integer wifiStrength = prefs.getInt(PrefStrings.WIFI_STRENGTH, -1);
        Long upDate = prefs.getLong(PrefStrings.UPD_DATE, 0);
        Long balanceDate = prefs.getLong(PrefStrings.BALANCE_DATE, 0);
        Boolean isPostpaid = prefs.getBoolean(PrefStrings.IS_POSTPAID, false);
        String dateTimeString = DateFormat.getDateTimeInstance().format(balanceDate);
        String balanceMsg = "";
        if (isPostpaid) {
            balanceMsg = String.format("Due: Rs %d, Credit: Rs %d @ %s", balanceDue, balanceCredit, dateTimeString);
        } else {
            balanceMsg = String.format("Rs %d at %s", balance, dateTimeString);
        }

        balanceView.setText(balanceMsg);
        levelView.setText(battery.toString() + "%");
        tempView.setText(temp.toString() + " °C");
        healthView.setText(ConvHealth(health));
        pluggedView.setText(plugged ? "Yes" : "No");
        wifiView.setText(wifi + " (Signal: " + (wifiStrength + 1) + "/5)");
        mobileView.setText(data ? "Yes" : "No");
        networkView.setText(carrier);
        //networkView.setText(carrier + " (Signal: " + (mobileStrength + 1) + "/5)");

    }

    public void updateBalanceView(final String msg) {
        MainActivity.this.runOnUiThread(() -> {
            TextView textV1 = findViewById(R.id.textViewBalance);
            textV1.setText(msg);
            progressBar.setVisibility(View.INVISIBLE);
        });
    }

    public void updateLastSMSInView(final String msg) {
        MainActivity.this.runOnUiThread(() -> {
            TextView textV1 = findViewById(R.id.textViewSMSInTime);
            textV1.setText("Last SMS in: " + msg);
        });
    }


}