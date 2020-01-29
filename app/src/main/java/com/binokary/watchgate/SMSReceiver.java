package com.binokary.watchgate;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.util.Log;
import android.widget.Toast;

import com.binokary.watchgate.toilers.WorkerUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;

import static android.content.Context.MODE_PRIVATE;

public class SMSReceiver extends BroadcastReceiver {
    private static final String TAG = Constants.MAINTAG + SMSReceiver.class.getSimpleName();
    private static final String PREF_STATS = "gate_stats";
    private SharedPreferences mSharedPreferences;
    private SharedPreferences prefs;
    private SharedPreferences.Editor stats;
    private boolean checkSMSPack = false;
    private boolean isBalanceInfo = false;
    private boolean isSmsPackInfo = false;
    private boolean isTopUpInfo = false;
    private boolean isPostpaid = false;

    @Override
    public void onReceive(Context context, Intent intent) {
        //Log.d(TAG, "Result Code: " + getResultCode());

        if (intent.getAction().equals(Telephony.Sms.Intents.SMS_RECEIVED_ACTION)) {

            Log.d(TAG, "sReceiver: Broadcast received");
            stats = context.getSharedPreferences(PREF_STATS, MODE_PRIVATE).edit();
            mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
            checkSMSPack = mSharedPreferences.getBoolean("switch_sms_preference", false);
            isPostpaid = mSharedPreferences.getBoolean("switch_preference_1", false);
            stats.putBoolean(PrefStrings.IS_POSTPAID, isPostpaid);
            String smsSender = "";
            String smsBody = "";
            long lastUserSMSInTime = 0;
            long lastBalanceSMSInTime = 0;
            int balance = -1;
            int balanceDue = -1;
            int balanceCredit = -1;
            int countSMSIn = 0;
            int smsRemaining = -1;
            prefs = context.getSharedPreferences(Constants.PREF_STATS, MODE_PRIVATE);
            countSMSIn = prefs.getInt(PrefStrings.COUNT_SMS_IN, 0);
            String countryCode = mSharedPreferences.getString("country_code", "+00");
            Log.d(TAG, "trying to read msg details");
            try {
                for (SmsMessage smsMessage : Telephony.Sms.Intents.getMessagesFromIntent(intent)) {
                    smsBody += smsMessage.getMessageBody();
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
                if (smsSource == "0") {
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

                    isTopUpInfo = SMSHelper.patternMatches(smsBody, prepaidBalanceTopUpRegexp) ||
                            SMSHelper.patternMatches(smsBody, postpaidBalanceTopUpRegexp);

                    if (isPostpaid && isTopUpInfo) { //Postpaid top up msg does not have new balance info
                        Log.d(TAG, "Enqueuing new balance query");
                        String postpaidBalanceQueryDestination = mSharedPreferences.getString("pref_sms_destination", "1415");
                        String postpaidBalanceQuery = mSharedPreferences.getString("pref_balance_query_postpaid", "CB");
                        WorkerUtils.enqueueOneTimeSMSSendingWork(postpaidBalanceQueryDestination, postpaidBalanceQuery);
                    }


                    if (SMSHelper.patternMatches(smsBody, prepaidBalanceRegexp) ||
                            SMSHelper.patternMatches(smsBody, postpaidBalanceCreditRegexp) ||
                            (isTopUpInfo && !isPostpaid) //Only prepaid top up sms has new balance information
                    ) {

                        isBalanceInfo = true;
                        Toast.makeText(context, "Balance Info SMS Received: " + smsBody, Toast.LENGTH_LONG).show();
                        Log.d(TAG, "SMS detected: From " + smsSender + " With text " + smsBody);

                        //lastBalanceSMSInTime = System.currentTimeMillis();
                        //String currentDateTimeString = DateFormat.getDateTimeInstance().format(new Date());


                        try {
                            if (isPostpaid) {
                                balanceDue = SMSHelper.getIntFromMsgBodyRegex(smsBody, postpaidBalanceDueRegexp);
                                balanceCredit = SMSHelper.getIntFromMsgBodyRegex(smsBody, postpaidBalanceCreditRegexp);
                                lastBalanceSMSInTime = System.currentTimeMillis();
                                stats.putLong(PrefStrings.BALANCE_DATE, lastBalanceSMSInTime);
                                stats.putInt(PrefStrings.POSTPAID_BALANCE_DUE, balanceDue);
                                stats.putInt(PrefStrings.POSTPAID_BALANCE_CREDIT, balanceCredit);
                                Log.d(TAG, "Balance is " + balanceCredit);
                                SMSNotification(balanceCredit);
                            } else //Prepaid
                            {
                                if (isTopUpInfo) { //Top up message with new balance information
                                    balance = SMSHelper.getIntFromMsgBodyRegex(smsBody, prepaidBalanceTopUpRegexp);

                                    Log.d(TAG, "Balance is " + balance);
                                } else {
                                    balance = SMSHelper.getIntFromMsgBodyRegex(smsBody, prepaidBalanceRegexp);
                                }
                                lastBalanceSMSInTime = System.currentTimeMillis();
                                stats.putLong(PrefStrings.BALANCE_DATE, lastBalanceSMSInTime);
                                stats.putInt(PrefStrings.PREPAID_BALANCE, balance);
                                SMSNotification(balance);
                            }

                            //Push to Slack Channel
                            if (mSharedPreferences.getBoolean("switch_slack", false)) {
                                JSONObject jsonBody = new JSONObject();
                                try {
                                    jsonBody.put(
                                            "text",
                                            mSharedPreferences.getString("instance_name", "none").toUpperCase() + " :red_circle: Balance is Rs. " + (isPostpaid ? balanceCredit : balance));
                                    SlackHelper.sendMessage(context, jsonBody);
                                } catch (
                                        JSONException e) {
                                    Log.e(TAG, e.getMessage());
                                }
                            }
                        } catch (Exception ex) {
                            Log.e(TAG, "Error when reading balance info from SMS: " + smsBody + " Error: " + ex.getMessage());
                        }
                        stats.apply();

                        String balanceMsg = SMSHelper.getBalanceMsgFromParts(isPostpaid, balance, balanceDue, balanceCredit, lastBalanceSMSInTime);

                        try {
                            MainActivity.getInstance().updateBalanceView(balanceMsg);
                        } catch (Exception ex) {
                            Log.e(TAG, "Error when updating Main Activity view from SMS Receiver: " + ex.getMessage());
                        }

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
                    String smsRecipient = mSharedPreferences.getString("pref_sms_pack_destination", "1415");
                    String smsPackQuery = mSharedPreferences.getString("pref_sms_query", "0");// "FR");
                    /* //For SMS Pack subscription, TODO_MAYBE_LATER
                    String smsPackMsg = mSharedPreferences.getString("pref_sms_sub", "SMS300");
                    int criticalSMSRemaining = -1;
                    int criticalBalanceRemaining = -1;
                    try {
                        criticalSMSRemaining = Integer.parseInt(mSharedPreferences.getString("pref_sms_min", "100"));
                        criticalBalanceRemaining = Integer.parseInt(mSharedPreferences.getString("pref_sms_bal_min", "100"));
                    } catch (Exception ex) {
                        Log.e(TAG, "Error when reading integer values for critical SMS and balance remaining");

                    }
                    int mainBalance = prefs.getInt(PrefStrings.PREPAID_BALANCE, -1);
                    */
                    if (SMSHelper.patternMatches(smsBody, smsPackInfoRegexp)) { //SMS Pack Info
                        isSmsPackInfo = true;
                        try {
                            Log.d(TAG, "Found SMS with SMS pack info.");
                            smsRemaining = SMSHelper.getIntFromMsgBodyRegex(smsBody, smsPackInfoRegexp);
                            stats.putLong(PrefStrings.SMS_PACK_INFO_DATE, System.currentTimeMillis());
                            stats.putInt(PrefStrings.SMS_PACK_INFO, smsRemaining);
                            stats.apply();

                            /*
                            //SMS remaining should be below critical and balance should be equal or above critical
                            if (smsRemaining < criticalSMSRemaining && mainBalance >= criticalBalanceRemaining) {
                                Log.d(TAG, "Enqueuing SMS for SMS Pack subscription.");
                                WorkerUtils.enqueueOneTimeSMSSendingWork(smsRecipient, smsPackMsg);
                            }
                            */
                        } catch (Exception ex) {
                            Log.e(TAG, "Error when reading SMS pack info: " + smsBody + " Error: " + ex.getMessage());
                        }

                        try {
                            MainActivity.getInstance().updateSMSPackView(smsRemaining + " remaining (" + DateFormat.getDateTimeInstance().format(System.currentTimeMillis()) + ")");
                        } catch (Exception ex) {
                            Log.e(TAG, "Error when updating SMS Pack in Main Activity view from SMS Receiver: " + ex.getMessage());
                        }
                    } else if (SMSHelper.patternMatches(smsBody, smsPackNullRegexp)) {//No SMS Pack subscribed
                        Log.d(TAG, "Found SMS with SMS pack info null.");
                        stats.putLong(PrefStrings.SMS_PACK_INFO_DATE, System.currentTimeMillis());
                        stats.putInt(PrefStrings.SMS_PACK_INFO, 0);
                        stats.apply();

                        /*
                        //SMS remaining should be below critical and balance should be equal or above critical
                        if (smsRemaining < criticalSMSRemaining && mainBalance >= criticalBalanceRemaining) {
                            WorkerUtils.enqueueOneTimeSMSSendingWork(smsRecipient, smsPackMsg);
                        }
                        */
                        try {
                            MainActivity.getInstance().updateSMSPackView("N/A");
                        } catch (Exception ex) {
                            Log.e(TAG, "Error when updating SMS Pack in Main Activity view from SMS Receiver: " + ex.getMessage());
                        }
                    } else if (SMSHelper.patternMatches(smsBody, smsPackActiveRegexp)) {
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

                        try {
                            MainActivity.getInstance().updateSMSPackView("Subscribed");
                        } catch (Exception ex) {
                            Log.e(TAG, "Error when updating SMS Pack in Main Activity view from SMS Receiver: " + ex.getMessage());
                        }
                    }
                }
                //Make one time update report after getting message from Balance or SMS Pack info
                if (isBalanceInfo || isSmsPackInfo) {
                    String instanceName = mSharedPreferences.getString("instance_name", "none");
                    String reportOneIntervalMinString = mSharedPreferences.getString("pref_interval_report_one_min", "3");
                    String reportOneWaitMinString = mSharedPreferences.getString("pref_wait_report_one_time", "180");
                    int reportOneIntervalMin = 3;
                    int reportOneWaitTime = 180;
                    int initialDelayInSeconds = 0;
                    try {
                        reportOneIntervalMin = Integer.parseInt(reportOneIntervalMinString);
                        reportOneWaitTime = Integer.parseInt(reportOneWaitMinString);

                    } catch (Exception ex) {
                        Log.e("Error parsing integer: ", ex.getMessage());
                    }

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
                    WorkerUtils.clearTasks(Constants.REPORTONEWAITTAG);
                    WorkerUtils.enqueueOneTimeStitchReportingWork(instanceName, reportOneIntervalMin, initialDelayInSeconds);


                }

                String lastSMSInTimeMsg = DateFormat.getDateTimeInstance().format(lastUserSMSInTime);
                try {
                    MainActivity.getInstance().updateLastSMSInView(lastSMSInTimeMsg);
                } catch (Exception ex) {
                    Log.e(TAG, "Error when updating Main Activity view from SMS Receiver");
                }
            } catch (Exception ex) {
                Log.e(TAG, "Error when processing broadcast" + ex.getMessage());
            }

            stats.apply();
        }

    }

    public void SMSNotification(int balanceInRs) {
        //TODO: Send SMS to user if balance is low than critical level
        Boolean sendSMS = mSharedPreferences.getBoolean("switch_sms_notification", false);
        if (sendSMS) {
            try {
                Log.d(TAG, "Preparing to send sms notification");
                String smsRecipients = mSharedPreferences.getString("edit_number_preference", "");
                String[] smsRecipientArray = smsRecipients.split(";");
                String smsIntervalStr = mSharedPreferences.getString("sms_notification_interval", "4");
                String criticalBalanceStr = mSharedPreferences.getString("edit_balance_limit", "500");
                String instanceName = mSharedPreferences.getString("instance_name", "none");
                Long lastNotificationTime = prefs.getLong(PrefStrings.SMS_NOTIFICATION_DATE, 0);
                Long currentTime = System.currentTimeMillis();
                String smsMsg = String.format("Balance in %s is %d.", instanceName.toUpperCase(), balanceInRs);
                Integer smsInterval = 2;
                Integer criticalBalance = 100;
                try {
                    smsInterval = Integer.parseInt(smsIntervalStr);
                    criticalBalance = Integer.parseInt(criticalBalanceStr);
                } catch (Exception ex) {
                    Log.e(TAG, "Error parsing smsInterval and criticalBalance: " + ex.getMessage());
                }

                Log.d(TAG, "Critical balance: " + criticalBalance + ", Actual balance: " + balanceInRs);
                if (balanceInRs <= criticalBalance) {
                    if (smsInterval < currentTime - lastNotificationTime) {
                        Log.d(TAG, "Interval OK, enqueuing SMS");
                        for (String smsRecipient : smsRecipientArray) {
                            WorkerUtils.enqueueOneTimeSMSSendingWork(smsRecipient, smsMsg);
                        }
                        stats.putLong(PrefStrings.SMS_NOTIFICATION_DATE, currentTime);
                        stats.apply();
                    }
                }
            } catch (Exception ex) {
                Log.e(TAG, "Error on SMS notification to user: " + ex.getMessage());
            }
        }
    }
}
