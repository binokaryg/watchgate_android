package com.binokary.watchgate;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.provider.Telephony;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.binokary.watchgate.toilers.WorkerUtils;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.messaging.FirebaseMessaging;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;


public class MainActivity extends AppCompatActivity {
    private static final String TAG = Constants.MAIN_TAG + "MainActivity";
    private static final String PREF_STATS = "gate_stats";
    private static final int SMS_PERMISSION_CODE = 0;
    //private static MainActivity mainActivityInstance;
    TextView textView;
    TextView titleView;
    ProgressBar progressBar;
    WorkManager mWorkManager;
    ListenableFuture<List<WorkInfo>> mWorkLiveData;
    TextView levelView, healthView, tempView, pluggedView, wifiView, mobileView, networkView, spaceView, balanceView;
    SharedPreferences prefs;
    SharedPreferences.OnSharedPreferenceChangeListener listener;
    private SharedPreferences mSharedPreferences;
    private NotificationManager notificationManager;
    private NotificationCompat.Builder persistentNotificationBuilder;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        if (item.getItemId() == R.id.settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            this.startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //mainActivityInstance = this;
        setContentView(R.layout.activity_main);
        textView = findViewById(R.id.textView);
        titleView = findViewById(R.id.textViewTitle);
        progressBar = findViewById(R.id.progressBar);

        levelView = findViewById(R.id.textViewLevel);
        healthView = findViewById(R.id.textViewHealth);
        tempView = findViewById(R.id.textViewTemperature);
        pluggedView = findViewById(R.id.textViewPlugged);
        wifiView = findViewById(R.id.textViewWifi);
        mobileView = findViewById(R.id.textViewMobile);
        networkView = findViewById(R.id.textViewNetwork);
        spaceView = findViewById(R.id.textViewFreeSpace);
        balanceView = findViewById(R.id.textViewBalance);

        Button normalSMSBtn = findViewById(R.id.btn_normal_sms);
        Button startButton = findViewById(R.id.buttonStart);
        Button stopButton = findViewById(R.id.buttonStop);
        Button infoButton = findViewById(R.id.buttonInfo);


        PreferenceManager.setDefaultValues(this, R.xml.pref_general, true);
        PreferenceManager.setDefaultValues(this, R.xml.pref_interval, true);
        PreferenceManager.setDefaultValues(this, R.xml.pref_network, true);
        PreferenceManager.setDefaultValues(this, R.xml.pref_notifications, true);
        PreferenceManager.setDefaultValues(this, R.xml.pref_smspacks, true);
        PreferenceManager.setDefaultValues(this, R.xml.pref_variables, true);

        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        prefs = getSharedPreferences(PREF_STATS, MODE_PRIVATE);
        listener = (sharedPreferences, key) -> {
            StatsHelper.CheckAndUpdateStats(getApplicationContext());
            UpdateViews();
        };
        prefs.registerOnSharedPreferenceChangeListener(listener);

        setTitle("WatchGate " + BuildConfig.VERSION_NAME);

        normalSMSBtn.setOnClickListener(v -> {
            if (!hasValidPreConditions()) return;
            progressBar.setVisibility(View.VISIBLE);
            StatsHelper.CheckAndUpdateStats(getApplicationContext());
            UpdateViews();
            String smsQueryMsg = mSharedPreferences.getBoolean("switch_preference_1", false)
                    ? mSharedPreferences.getString("pref_balance_query_postpaid", "CB")
                    : mSharedPreferences.getString("pref_balance_query_prepaid", "BL");
            SMSHelper.sendSms(mSharedPreferences.getString("pref_sms_destination", "1415"), smsQueryMsg);
            Toast.makeText(getApplicationContext(), R.string.toast_sending_sms + (mSharedPreferences.getBoolean("switch_preference_1", true) ? "Postpaid" : "Prepaid"), Toast.LENGTH_SHORT).show();
        });

        startButton.setOnClickListener(v -> {

            String smsQueryMsg = mSharedPreferences.getBoolean("switch_preference_1", false)
                    ? mSharedPreferences.getString("pref_balance_query_postpaid", "")
                    : mSharedPreferences.getString("pref_balance_query_prepaid", "");
            int smsInterval = mSharedPreferences.getInt("pref_interval_sms", 240);
            int smsIntervalMin = mSharedPreferences.getInt("pref_interval_sms_min", 10);
            int reportInterval = mSharedPreferences.getInt("pref_interval_report", 30);
            int reportIntervalMin = mSharedPreferences.getInt("pref_interval_report_min", 10);
            int reportOneIntervalMin = mSharedPreferences.getInt("pref_interval_report_one_min", 3);

            String instanceName = mSharedPreferences.getString("instance_name", "none");

            mWorkManager = WorkManager.getInstance(getApplicationContext());

            WorkerUtils.enqueueSMSSendingWork(mSharedPreferences.getString("pref_sms_destination", "1415"), smsQueryMsg, smsInterval, smsIntervalMin);
            WorkerUtils.enqueueStitchReportingWork(instanceName, reportInterval, reportIntervalMin, reportOneIntervalMin);
        });


        stopButton.setOnClickListener(v -> {
            Log.d(TAG, " gatewatch stopping worker");
            if (mWorkManager == null) {
                mWorkManager = WorkManager.getInstance(getApplicationContext());
            }
            mWorkLiveData = mWorkManager.getWorkInfosByTag(Constants.SMS_TAG);
            try {
                List<WorkInfo> workList = mWorkLiveData.get();
                for (int i = 0; i < workList.size(); i++) {
                    Log.d(TAG, "Work state of gatewatch" + i + " : " + workList.get(i).getState());
                }
            } catch (Exception ex) {
                Log.e(TAG, "Error when trying to get task list: " + ex.getMessage());
            }

            int clearStatus = WorkerUtils.clearTasks(Constants.SMS_TAG);
            Log.d(TAG, " SMS sending tasks cleared " + clearStatus);

            mWorkLiveData = mWorkManager.getWorkInfosByTag(Constants.REPORT_TAG);
            try {
                List<WorkInfo> workList = mWorkLiveData.get();
                for (int i = 0; i < workList.size(); i++) {
                    Log.d(TAG, "Work state of gatewatch" + i + " : " + workList.get(i).getState());
                }
            } catch (Exception ex) {
                Log.e(TAG, "Error when trying to get task list: " + ex.getMessage());
            }

            clearStatus = WorkerUtils.clearTasks(Constants.REPORT_TAG);
            Log.d(TAG, " Stitch reporting tasks cleared " + clearStatus);
        });

        infoButton.setOnClickListener(v -> {

            StringBuilder infoBuilder = new StringBuilder("Info:\n");


            if (mWorkManager == null) {
                mWorkManager = WorkManager.getInstance(getApplicationContext());
            }
            //SMS Workers
            Log.d(TAG, "Getting workers with Tag: " + Constants.SMS_TAG + "\n");
            mWorkLiveData = mWorkManager.getWorkInfosByTag(Constants.SMS_TAG);
            try {
                List<WorkInfo> workList = mWorkLiveData.get();
                for (int i = 0; i < workList.size(); i++) {
                    Log.d(TAG, "Work state of SMS Senders " + i + " : " + workList.get(i).getState());
                    infoBuilder.append("SMSS").append(i);
                    infoBuilder.append(": ");
                    infoBuilder.append(workList.get(i).getState());
                    infoBuilder.append("\n");
                }
                textView.setText(infoBuilder);
            } catch (Exception ex) {
                Log.e(TAG, "Error when trying to get task list: " + ex.getMessage());
            }
            //Report Workers
            Log.d(TAG, "Getting workers with Tag: " + Constants.REPORT_TAG + "\n");
            mWorkLiveData = mWorkManager.getWorkInfosByTag(Constants.REPORT_TAG);
            try {
                List<WorkInfo> workList = mWorkLiveData.get();
                for (int i = 0; i < workList.size(); i++) {
                    Log.d(TAG, "Work state of Stitch Reporters " + i + " : " + workList.get(i).getState());
                    infoBuilder
                            .append("SR")
                            .append(i)
                            .append(": ")
                            .append(workList.get(i).getState())
                            .append("\n");
                }
                textView.setText(infoBuilder);
            } catch (Exception ex) {
                Log.e(TAG, "Error when trying to get task list: " + ex.getMessage());
            }

            //One Time SMS Workers
            Log.d(TAG, "Getting workers with Tag: " + Constants.SMS_ONE_TAG + "\n");

            mWorkLiveData = mWorkManager.getWorkInfosByTag(Constants.SMS_ONE_TAG);
            try {
                List<WorkInfo> workList = mWorkLiveData.get();
                for (int i = 0; i < workList.size(); i++) {
                    Log.d(TAG, "Work state of one time SMS Senders " + i + " : " + workList.get(i).getState());
                    infoBuilder.append("SMS1S").append(i);
                    infoBuilder.append(": ");
                    infoBuilder.append(workList.get(i).getState());
                    infoBuilder.append("\n");
                }
                textView.setText(infoBuilder);
            } catch (Exception ex) {
                Log.e(TAG, "Error when trying to get task list: " + ex.getMessage());
            }


            //One Time Report Workers
            Log.d(TAG, "Getting workers with Tag: " + Constants.REPORT_ONE_TAG + "\n");

            mWorkLiveData = mWorkManager.getWorkInfosByTag(Constants.REPORT_ONE_TAG);
            try {
                List<WorkInfo> workList = mWorkLiveData.get();
                for (int i = 0; i < workList.size(); i++) {
                    Log.d(TAG, "Work state of one time Stitch reporters " + i + " : " + workList.get(i).getState());
                    infoBuilder.append("S1R").append(i);
                    infoBuilder.append(": ");
                    infoBuilder.append(workList.get(i).getState());
                    infoBuilder.append("\n");
                }
                textView.setText(infoBuilder);
            } catch (Exception ex) {
                Log.e(TAG, "Error when trying to get task list: " + ex.getMessage());
            }


            //One Time Report Workers with Wait time
            Log.d(TAG, "Getting workers with Tag: " + Constants.REPORT_ONE_WAIT_TAG + "\n");

            mWorkLiveData = mWorkManager.getWorkInfosByTag(Constants.REPORT_ONE_WAIT_TAG);
            try {
                List<WorkInfo> workList = mWorkLiveData.get();
                for (int i = 0; i < workList.size(); i++) {
                    Log.d(TAG, "Work state of Stitch reporters with wait time " + i + " : " + workList.get(i).getState());
                    infoBuilder.append("S1WR").append(i);
                    infoBuilder.append(": ");
                    infoBuilder.append(workList.get(i).getState());
                    infoBuilder.append("\n");
                }
                textView.setText(infoBuilder);
            } catch (Exception ex) {
                Log.e(TAG, "Error when trying to get task list: " + ex.getMessage());
            }

            //Stats
            StatsHelper.CheckAndUpdateStats(getApplicationContext());
            UpdateViews();
        });


        if (!SMSHelper.hasAllNecessaryPermissions(MainActivity.this)) {
            showRequestPermissionsInfoAlertDialog();
        }
        progressBar = findViewById(R.id.progressBar);
        progressBar.setIndeterminate(true);

        if (android.os.Build.VERSION.SDK_INT >= 26) {
            // For Android SDK 26 and above, it is necessary to create a channel to create notifications.
            notificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationChannel channel = new NotificationChannel("channel_fcm",
                    "FCM", NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }
        notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (android.os.Build.VERSION.SDK_INT >= 26) {
            // For Android SDK 26 and above, it is necessary to create a channel to create notifications.
            NotificationChannel channel = new NotificationChannel("channel_persistent",
                    "PERSISTENT", NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }
        String versionName = BuildConfig.VERSION_NAME;
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        persistentNotificationBuilder = new NotificationCompat.Builder(getApplicationContext(), "channel_persistent");

        persistentNotificationBuilder.setAutoCancel(false)
                .setDefaults(Notification.DEFAULT_ALL)
                .setWhen(System.currentTimeMillis())
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_i_see))
                .setSmallIcon(R.drawable.ic_remove_red_eye_black_24dp)
                .setTicker("Watchgate")
                .setContentTitle("WG" + versionName + ": " + mSharedPreferences.getString("instance_name", "unnamed").toUpperCase())
                .setContentText(getNotificationSummaryText())
                .setContentInfo("Info")
                .setContentIntent(pendingIntent)
                .setOngoing(true);

        notificationManager.notify(1, persistentNotificationBuilder.build());
    }

    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Activity onDestroy");
    }

    @Override
    protected void onStart() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Telephony.Sms.Intents.SMS_RECEIVED_ACTION);
        long freeMemory = freeMemory();
        long totalMemory = totalMemory();
        String freeSpaceMsg = String.format(Locale.US, "%d MB free of %d MB total", freeMemory, totalMemory);
        spaceView.setText(freeSpaceMsg);
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        StatsHelper.CheckAndUpdateStats(getApplicationContext());
        UpdateViews();
        prefs.registerOnSharedPreferenceChangeListener(listener);
    }

    @Override
    protected void onPause() {
        super.onPause();
        prefs.unregisterOnSharedPreferenceChangeListener(listener);

    }

    @Override
    protected void onStop() {
        super.onStop();
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

        ActivityCompat.requestPermissions(MainActivity.this, new String[]{
                        Manifest.permission.READ_SMS,
                        Manifest.permission.RECEIVE_SMS, Manifest.permission.SEND_SMS,
                        Manifest.permission.READ_PHONE_STATE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.CHANGE_WIFI_STATE
                },
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

        builder.setPositiveButton(R.string.action_ok, (dialog, which) -> {
            dialog.dismiss();
            // Display system runtime permission request?
            if (makeSystemRequest) {
                requestReadAndSendSmsPermission();
            }
        });

        builder.setCancelable(false);
        builder.show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
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

        StatFs statFs = new StatFs(Environment.getDataDirectory().getAbsolutePath());

        return (statFs.getTotalBytes() / (1024 * 1024));
    }

    private long freeMemory() {
        StatFs statFs = new StatFs(Environment.getDataDirectory().getAbsolutePath());
        return (statFs.getFreeBytes() / (1024 * 1024));
    }


    public void UpdateViews() {
        SharedPreferences prefs = getSharedPreferences(PREF_STATS, MODE_PRIVATE);
        titleView.setText(mSharedPreferences.getString("instance_name", "unnamed").toUpperCase());
        String wifi = prefs.getString(PrefStrings.WIFI_SSID, "N/A");
        boolean data = prefs.getBoolean(PrefStrings.MOBILE_DATA, false);
        int temp = prefs.getInt(PrefStrings.TEMPERATURE, -1);
        int health = prefs.getInt(PrefStrings.HEALTH, -1);
        boolean plugged = prefs.getBoolean(PrefStrings.PLUGGED, false);
        int battery = prefs.getInt(PrefStrings.BATTERY, -1);
        String carrier = prefs.getString(PrefStrings.MOBILE_CARRIER, "");
        //Integer mobileStrength = prefs.getInt(PrefStrings.MOBILE_STRENGTH, -1);
        int wifiStrength = prefs.getInt(PrefStrings.WIFI_STRENGTH, -1);
        levelView.setText(String.format(Locale.US, "%d%%", battery));
        tempView.setText(String.format(Locale.US, "%d°C", temp));
        healthView.setText(ConvHealth(health));
        pluggedView.setText(plugged ? "Yes" : "No");
        wifiView.setText(String.format(Locale.US, "%s (Signal: %d/5)", wifi, wifiStrength + 1));
        mobileView.setText(data ? "Yes" : "No");
        networkView.setText(carrier);

        updateBalanceInfoView(prefs);
        updateLastSMSInView(prefs);
        updateSMSPackView(prefs);

        updateNotificationSummary(getNotificationSummaryText());

    }

    public String getNotificationSummaryText() {
        boolean postPaid = prefs.getBoolean(PrefStrings.IS_POSTPAID, false);
        StringBuilder summaryText = new StringBuilder();
        if (postPaid) {
            int postPaidBalanceCredit = prefs.getInt(PrefStrings.POSTPAID_BALANCE_CREDIT, -1);
            summaryText.append("Rs ").append(postPaidBalanceCredit).append("*");
        } else {
            int prePaidBalance = prefs.getInt(PrefStrings.PREPAID_BALANCE, -1);
            summaryText.append("Rs ").append(prePaidBalance);
        }
        long balanceDate = prefs.getLong(PrefStrings.BALANCE_DATE, -1);
        SimpleDateFormat formatter = new SimpleDateFormat("MMM d HH:mm", Locale.US);
        String dateStringBalance = formatter.format(new Date(balanceDate));
        summaryText.append(" (").append(dateStringBalance).append("); ");
        int smsPack = prefs.getInt(PrefStrings.SMS_PACK_INFO, -1);
        long smsPackDate = prefs.getLong(PrefStrings.SMS_PACK_INFO_DATE, -1);
        String dateStringMsg = formatter.format(new Date(smsPackDate));
        summaryText.append("SMS: ").append(smsPack).append(" (").append(dateStringMsg).append(")");
        return summaryText.toString();
    }

    public void updateNotificationSummary(String summaryText) {
        persistentNotificationBuilder.setContentText(summaryText);
        notificationManager.notify(1, persistentNotificationBuilder.build());
    }

    public void toggleSubscription(View view) {
        CheckBox checkBox = (CheckBox) view;
        final String single_topic = mSharedPreferences.getString("instance_name", "none");
        String[] topics = mSharedPreferences.getString("pref_all_instances", "").split(",");
        boolean monitorOnly = mSharedPreferences.getBoolean("switch_monitor_mode", false);

        //Only allow multiple subscriptions in monitor mode
        if (!monitorOnly) {
            topics = new String[1];
            topics[0] = single_topic;

        }
        for (String topic : topics) {
            if (checkBox.isChecked()) {
                FirebaseMessaging.getInstance().subscribeToTopic(topic).addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.d(TAG, "Error subscribing to topic " + task.getException());
                        return;
                    }

                    Log.d(TAG, "Subscribed to topic " + topic);
                    Toast.makeText(getApplicationContext(), "Subscribed to topic " + topic, Toast.LENGTH_LONG).show();
                });
            } else {
                FirebaseMessaging.getInstance().unsubscribeFromTopic(topic).addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.d(TAG, "Error unsubscribing from topic " + task.getException());
                        return;
                    }

                    Log.d(TAG, "Unsubscribed from topic " + topic);
                    Toast.makeText(getApplicationContext(), "Unsubscribed from topic " + topic, Toast.LENGTH_LONG).show();
                });
            }
        }
    }

    private void updateBalanceInfoView(SharedPreferences sharedPreferences) {
        String text = SMSHelper.getBalanceMsgFromParts(
                sharedPreferences.getBoolean(PrefStrings.IS_POSTPAID, false),
                sharedPreferences.getInt(PrefStrings.PREPAID_BALANCE, -1),
                sharedPreferences.getInt(PrefStrings.POSTPAID_BALANCE_DUE, -1),
                sharedPreferences.getInt(PrefStrings.POSTPAID_BALANCE_CREDIT, -1),
                sharedPreferences.getLong(PrefStrings.BALANCE_DATE, -1)
        );
        ((TextView) findViewById(R.id.textViewBalance)).setText(text);
        updateNotificationSummary(getNotificationSummaryText());
        progressBar.setVisibility(View.INVISIBLE);
    }

    private void updateLastSMSInView(SharedPreferences sharedPreferences) {
        String text = String.format(
                getString(R.string.last_sms_display),
                DateFormat.getDateTimeInstance().format(sharedPreferences.getLong(PrefStrings.LAST_SMS_IN_DATE, 0))
        );
        ((TextView) findViewById(R.id.textViewSMSInTime)).setText(text);
    }

    private void updateSMSPackView(SharedPreferences sharedPreferences) {
        String text = String.format(
                getString(R.string.sms_pack_display),
                sharedPreferences.getInt(PrefStrings.SMS_PACK_INFO, 0),
                DateFormat.getDateTimeInstance().format(sharedPreferences.getLong(PrefStrings.SMS_PACK_INFO_DATE, 0))
        );
        ((TextView) findViewById(R.id.textViewSMSPack)).setText(text);
    }
}