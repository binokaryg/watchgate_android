package com.binokary.watchgate;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.util.Log;
import android.widget.Toast;

import androidx.preference.PreferenceManager;

import com.binokary.watchgate.toilers.WorkerUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

import static android.content.Context.MODE_PRIVATE;

public class SMSReceiver extends BroadcastReceiver {
    private static final String TAG = Constants.MAIN_TAG + SMSReceiver.class.getSimpleName();
    private static final String PREF_STATS = "gate_stats";
    private SharedPreferences mSharedPreferences;
    private SharedPreferences prefs;
    private SharedPreferences.Editor stats;
    private boolean isBalanceInfo = false;
    private boolean isSmsPackInfo = false;
    private boolean isTopUpInfo = false;

    @Override
    public void onReceive(Context context, Intent intent) {
        //Log.d(TAG, "Result Code: " + getResultCode());

        if (intent.getAction().equals(Telephony.Sms.Intents.SMS_RECEIVED_ACTION)) {
            Log.d(TAG, "sReceiver: Broadcast received");

            stats = context.getSharedPreferences(PREF_STATS, MODE_PRIVATE).edit();
            mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
            boolean checkSMSPack = mSharedPreferences.getBoolean("switch_sms_preference", false);
            boolean isPostpaid = mSharedPreferences.getBoolean("switch_preference_1", false);
            stats.putBoolean(PrefStrings.IS_POSTPAID, isPostpaid);
            String smsSender = "";
            StringBuilder smsBody = new StringBuilder();
            long lastUserSMSInTime;
            long lastBalanceSMSInTime;
            int balance;
            int balanceDue;
            int balanceCredit;
            int countSMSIn;
            int smsRemaining;
            prefs = context.getSharedPreferences(Constants.PREF_STATS, MODE_PRIVATE);
            countSMSIn = prefs.getInt(PrefStrings.COUNT_SMS_IN, 0);
            String countryCode = mSharedPreferences.getString("country_code", "+00");
            Log.d(TAG, "trying to read msg details");
            try {
                for (SmsMessage smsMessage : Telephony.Sms.Intents.getMessagesFromIntent(intent)) {
                    smsBody.append(smsMessage.getMessageBody());
                    smsSender = smsMessage.getOriginatingAddress();
                }

                if (smsSender.length() > 9) //SMS from user
                {
                    countSMSIn += 1;
                    lastUserSMSInTime = System.currentTimeMillis();
                    stats.putInt(PrefStrings.COUNT_SMS_IN, countSMSIn);
                    stats.putLong(PrefStrings.LAST_SMS_IN_DATE, lastUserSMSInTime);
                    stats.apply();
                }
                String smsSource = mSharedPreferences.getString("pref_sms_source", "0");
                if (smsSource.equals("0")) {
                    Log.w(TAG, "Getting default value for pref_sms_source!");
                }
                Log.d(TAG, "SMS source: " + smsSource + ", SMS sender: " + smsSender);
                if (smsSender.equals(smsSource) || smsSender.equals(countryCode + smsSource)) {
                    String prepaidBalanceRegexp = mSharedPreferences.getString("pref_pre_balance_regex", ".*current balance.*?([0-9,]+).*");
                    String postpaidBalanceDueRegexp = mSharedPreferences.getString("pref_post_balance_due_regex", ".*your due.*?([0-9,]+).*");
                    String postpaidBalanceCreditRegexp = mSharedPreferences.getString("pref_post_balance_credit_regex", ".*available credit.*?([0-9,]+).*");
                    String prepaidBalanceTopUpRegexp = mSharedPreferences.getString("pref_pre_balance_top_up_regex", ".*new balance.*?([0-9,]+).*");
                    String postpaidBalanceTopUpRegexp = mSharedPreferences.getString("pref_post_balance_top_up_regex", ".*has been credited.*");

                    String smsPackQuery = mSharedPreferences.getString("pref_sms_query", "FR");
                    String smsRecipient = mSharedPreferences.getString("pref_sms_pack_destination", "1415");

                    isTopUpInfo = SMSHelper.patternMatches(smsBody.toString(), prepaidBalanceTopUpRegexp) ||
                            SMSHelper.patternMatches(smsBody.toString(), postpaidBalanceTopUpRegexp);

                    if (isPostpaid && isTopUpInfo) { //Postpaid top up msg does not have new balance info
                        Log.d(TAG, "Enqueuing new balance query");
                        String postpaidBalanceQueryDestination = mSharedPreferences.getString("pref_sms_destination", "1415");
                        String postpaidBalanceQuery = mSharedPreferences.getString("pref_balance_query_postpaid", "CB");
                        WorkerUtils.enqueueOneTimeSMSSendingWork(postpaidBalanceQueryDestination, postpaidBalanceQuery);

                        if (mSharedPreferences.getBoolean("switch_slack", false)) {
                            JSONObject jsonBody = new JSONObject();
                            try {
                                jsonBody.put(
                                        "text",
                                        mSharedPreferences.getString("instance_name", "none").toUpperCase() +
                                                " :money_with_wings: Topped Up");
                                SlackHelper.sendMessage(context, jsonBody);
                            } catch (
                                    JSONException e) {
                                Log.e(TAG, e.getMessage());
                            }
                        }
                    }


                    if (SMSHelper.patternMatches(smsBody.toString(), prepaidBalanceRegexp) ||
                            SMSHelper.patternMatches(smsBody.toString(), postpaidBalanceCreditRegexp) ||
                            (isTopUpInfo && !isPostpaid) //Only prepaid top up sms has new balance information
                    ) {

                        isBalanceInfo = true;
                        Toast.makeText(context, "Balance Info SMS Received: " + smsBody, Toast.LENGTH_LONG).show();
                        Log.d(TAG, "SMS detected: From " + smsSender + " With text " + smsBody);

                        //lastBalanceSMSInTime = System.currentTimeMillis();
                        //String currentDateTimeString = DateFormat.getDateTimeInstance().format(new Date());


                        try {
                            if (isPostpaid) {
                                balanceDue = SMSHelper.getIntFromMsgBodyRegex(smsBody.toString(), postpaidBalanceDueRegexp);
                                balanceCredit = SMSHelper.getIntFromMsgBodyRegex(smsBody.toString(), postpaidBalanceCreditRegexp);
                                lastBalanceSMSInTime = System.currentTimeMillis();
                                stats.putLong(PrefStrings.BALANCE_DATE, lastBalanceSMSInTime);
                                stats.putInt(PrefStrings.POSTPAID_BALANCE_DUE, balanceDue);
                                stats.putInt(PrefStrings.POSTPAID_BALANCE_CREDIT, balanceCredit);
                                Log.d(TAG, "Balance is " + balanceCredit);
                                CheckCriticalBalanceAndSendNotifications(context, balanceCredit);
                            } else //Prepaid
                            {
                                if (isTopUpInfo) { //Top up message with new balance information
                                    balance = SMSHelper.getIntFromMsgBodyRegex(smsBody.toString(), prepaidBalanceTopUpRegexp);
                                    if (mSharedPreferences.getBoolean("switch_slack", false)) {
                                        JSONObject jsonBody = new JSONObject();
                                        try {
                                            jsonBody.put(
                                                    "text",
                                                    mSharedPreferences.getString("instance_name", "none").toUpperCase() +
                                                            " :green_circle: Topped Up. New balance is Rs. " + balance);
                                            SlackHelper.sendMessage(context, jsonBody);
                                        } catch (
                                                JSONException e) {
                                            Log.e(TAG, e.getMessage());
                                        }
                                    }
                                    Log.d(TAG, "Balance is " + balance);
                                } else {
                                    balance = SMSHelper.getIntFromMsgBodyRegex(smsBody.toString(), prepaidBalanceRegexp);
                                }
                                lastBalanceSMSInTime = System.currentTimeMillis();
                                stats.putLong(PrefStrings.BALANCE_DATE, lastBalanceSMSInTime);
                                stats.putInt(PrefStrings.PREPAID_BALANCE, balance);
                                CheckCriticalBalanceAndSendNotifications(context, balance);
                            }

                        } catch (Exception ex) {
                            Log.e(TAG, "Error when reading balance info from SMS: " + smsBody + " Error: " + ex.getMessage());
                        }
                        stats.apply();
                        //if SMS Pack is enabled, check remaining SMS
                        if (checkSMSPack) {
                            Log.d(TAG, "Enqueuing SMS for SMS pack query");
                            WorkerUtils.enqueueOneTimeSMSSendingWork(smsRecipient, smsPackQuery);
                        }

                    }//balance msg

                }//msg from telecom for balance

                //SMS pack info source
                String smsPackSource = mSharedPreferences.getString("pref_sms_pack_source", "1415");
                if ((smsSender.equals(smsPackSource) || smsSender.equals(countryCode + smsPackSource)) && checkSMSPack) {
                    String smsPackInfoRegexp = mSharedPreferences.getString("pref_sms_pack_info_regex", ".*free sms.*?([0-9,]+).*");
                    String smsPackNullRegexp = mSharedPreferences.getString("pref_sms_pack_null_regex", ".*no free resource.*");
                    String smsPackActiveRegexp = mSharedPreferences.getString("pref_sms_pack_active", ".*SMS.*activated.*");

                    if (SMSHelper.patternMatches(smsBody.toString(), smsPackInfoRegexp)) { //SMS Pack Info
                        isSmsPackInfo = true;
                        try {
                            Log.d(TAG, "Found SMS with SMS pack info.");
                            smsRemaining = SMSHelper.getIntFromMsgBodyRegex(smsBody.toString(), smsPackInfoRegexp);
                            stats.putLong(PrefStrings.SMS_PACK_INFO_DATE, System.currentTimeMillis());
                            stats.putInt(PrefStrings.SMS_PACK_INFO, smsRemaining);
                            stats.apply();

                        } catch (Exception ex) {
                            Log.e(TAG, "Error when reading SMS pack info: " + smsBody + " Error: " + ex.getMessage());
                        }

                    } else if (SMSHelper.patternMatches(smsBody.toString(), smsPackNullRegexp)) {//No SMS Pack subscribed
                        Log.d(TAG, "Found SMS with SMS pack info null.");
                        stats.putLong(PrefStrings.SMS_PACK_INFO_DATE, System.currentTimeMillis());
                        stats.putInt(PrefStrings.SMS_PACK_INFO, 0);
                        stats.apply();


                    } else if (SMSHelper.patternMatches(smsBody.toString(), smsPackActiveRegexp)) {
                        Log.d(TAG, "Found SMS with SMS Pack activation info");
                        Log.d(TAG, "Enqueuing SMS for Balance query, which should check SMS pack later");
                        if (isPostpaid) {
                            String postpaidBalanceQueryDestination = mSharedPreferences.getString("pref_sms_destination", "1415");
                            String postpaidBalanceQuery = mSharedPreferences.getString("pref_balance_query_postpaid", "CB");
                            WorkerUtils.enqueueOneTimeSMSSendingWork(postpaidBalanceQueryDestination, postpaidBalanceQuery);
                        } else {
                            String prepaidBalanceQueryDestination = mSharedPreferences.getString("pref_sms_destination", "1415");
                            String prepaidBalanceQuery = mSharedPreferences.getString("pref_balance_query_prepaid", "BL");
                            WorkerUtils.enqueueOneTimeSMSSendingWork(prepaidBalanceQueryDestination, prepaidBalanceQuery);
                        }

                    }
                }
                //Make one time update report after getting message from Balance or SMS Pack info
                if (isBalanceInfo || isSmsPackInfo) {
                    String instanceName = mSharedPreferences.getString("instance_name", "none");
                    int reportOneIntervalMin = mSharedPreferences.getInt("pref_interval_report_one_min", 3);
                    int reportOneWaitTime = mSharedPreferences.getInt("pref_wait_report_one_time", 180);
                    int initialDelayInSeconds = 0;

                    if (checkSMSPack) { //If SMS Pack check is enabled
                        if (isBalanceInfo || isTopUpInfo) { //If it is balance info or top up info
                            //Wait for 180 seconds for new SMS Pack message or new Balance info message before reporting,
                            initialDelayInSeconds = reportOneWaitTime;
                        }
                    }

                    //Disabling interval check for SMS Pack / Top up info reporting because it can occur
                    // immediately after balance info reporting
                    if (isSmsPackInfo || isTopUpInfo) {
                        reportOneIntervalMin = 0;
                    }

                    //Clear any waiting tasks before enqueuing new one
                    WorkerUtils.clearTasks(Constants.REPORT_ONE_WAIT_TAG);
                    WorkerUtils.enqueueOneTimeStitchReportingWork(instanceName, reportOneIntervalMin, initialDelayInSeconds);


                }

            } catch (Exception ex) {
                Log.e(TAG, "Error when processing broadcast" + ex.getMessage());
            }

            stats.apply();
        }

    }

    public void CheckCriticalBalanceAndSendNotifications(Context context, int balanceInRs) {
        int criticalBalance = mSharedPreferences.getInt("edit_balance_limit", 500);

        if (balanceInRs <= criticalBalance) {
            //Push to Slack Channel
            if (mSharedPreferences.getBoolean("switch_slack", false)) {
                JSONObject jsonBody = new JSONObject();
                try {
                    jsonBody.put(
                            "text",
                            mSharedPreferences.getString("instance_name", "none").toUpperCase()
                                    + mSharedPreferences.getString("pref_critical_balance_msg_slack_default", ": Low Balance: Rs ")
                                    + balanceInRs);
                    SlackHelper.sendMessage(context, jsonBody);
                } catch (
                        JSONException e) {
                    Log.e(TAG, e.getMessage());
                }
            }
            if (mSharedPreferences.getBoolean("switch_sms_notification", false)) {
                SMSNotification(balanceInRs);
            }
        }


    }

    public void SMSNotification(int balanceInRs) {
        try {
            Log.d(TAG, "Preparing to send sms notification");
            String smsRecipients = mSharedPreferences.getString("edit_number_preference", "");
            String[] smsRecipientArray = smsRecipients.split(";");
            int smsInterval = mSharedPreferences.getInt("sms_notification_interval", 2);
            String instanceName = mSharedPreferences.getString("instance_name", "none");
            long lastNotificationTime = prefs.getLong(PrefStrings.SMS_NOTIFICATION_DATE, 0);
            long currentTime = System.currentTimeMillis();
            String smsMsg = String.format(Locale.US, "Balance in %s is %d.", instanceName.toUpperCase(), balanceInRs);

            if (smsInterval < currentTime - lastNotificationTime) {
                Log.d(TAG, "Interval OK, enqueuing SMS");
                for (String smsRecipient : smsRecipientArray) {
                    WorkerUtils.enqueueOneTimeSMSSendingWork(smsRecipient, smsMsg);
                }
                stats.putLong(PrefStrings.SMS_NOTIFICATION_DATE, currentTime);
                stats.apply();
            }
        } catch (Exception ex) {
            Log.e(TAG, "Error on SMS notification to user: " + ex.getMessage());
        }
    }
}
