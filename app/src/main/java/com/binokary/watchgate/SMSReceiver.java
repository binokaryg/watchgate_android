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

import java.text.DateFormat;
import java.util.Date;

import static android.content.Context.MODE_PRIVATE;

public class SMSReceiver extends BroadcastReceiver {
    private static final String TAG = Constants.MAINTAG + SMSReceiver.class.getSimpleName();
    private static final String PREF_STATS = "gate_stats";
    private SharedPreferences mSharedPreferences;
    private SharedPreferences prefs;
    private boolean isBalanceInfo = false;
    private boolean isSmsPackInfo = false;

    @Override
    public void onReceive(Context context, Intent intent) {
        //Log.d(TAG, "Result Code: " + getResultCode());
        SharedPreferences.Editor stats = context.getSharedPreferences(PREF_STATS, MODE_PRIVATE).edit();
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        Boolean checkSMSPack = mSharedPreferences.getBoolean("switch_sms_preference", false);
        Log.d(TAG, "sReceiver: Broadcast received");

        if (intent.getAction().equals(Telephony.Sms.Intents.SMS_RECEIVED_ACTION)) {
            String smsSender = "";
            String smsBody = "";
            long lastUserSMSInTime = 0;
            long lastBalanceSMSInTime = 0;
            Boolean isPostpaid = false;
            int balance = -1;
            int balanceDue = -1;
            int balanceCredit = -1;
            int countSMSIn = 0;
            int smsRemaining = -1;
            prefs = context.getSharedPreferences(Constants.PREF_STATS, MODE_PRIVATE);
            countSMSIn = prefs.getInt(PrefStrings.COUNT_SMS_IN, 0);

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
                    Log.e(TAG, "Getting default value for pref_sms_source");
                }
                if (smsSender.equals(mSharedPreferences.getString("pref_sms_source", "1415"))) {

                    String prepaidBalanceRegexp = mSharedPreferences.getString("pref_pre_balance_regex", "0");//".*current balance.*?([0-9,]+).*");
                    if (prepaidBalanceRegexp == "0") {
                        Log.e(TAG, "Getting default value for pref_pre_balance_regex");
                    }
                    String postpaidBalanceDueRegexp = mSharedPreferences.getString("pref_post_balance_due_regex", "0");// ".*your due.*?([0-9,]+).*");
                    if (postpaidBalanceDueRegexp == "0") {
                        Log.e(TAG, "Getting default value for pref_post_balance_due_regex");
                    }
                    String postpaidBalanceCreditRegexp = mSharedPreferences.getString("pref_post_balance_credit_regex", "0");// ".*available credit.*?([0-9,]+).*");
                    if (postpaidBalanceCreditRegexp == "0") {
                        Log.e(TAG, "Getting default value for pref_post_balance_credit_regex");
                    }
                    String smsPackQuery = mSharedPreferences.getString("pref_sms_query", "0");// "FR");
                    if (smsPackQuery == "0") {
                        Log.e(TAG, "Getting default value for pref_sms_query");
                    }
                    String smsRecipient = mSharedPreferences.getString("pref_sms_pack_destination", "0");// "1415");
                    if (smsRecipient == "0") {
                        Log.e(TAG, "Getting default value for pref_sms_pack_destination");
                    }

                    if (SMSHelper.patternMatches(smsBody, prepaidBalanceRegexp)
                            || SMSHelper.patternMatches(smsBody, postpaidBalanceCreditRegexp)) {
                        isBalanceInfo = true;
                        Toast.makeText(context, "Balance Info SMS Received: " + smsBody, Toast.LENGTH_LONG).show();
                        Log.d(TAG, "SMS detected: From " + smsSender + " With text " + smsBody);

                        lastBalanceSMSInTime = System.currentTimeMillis();
                        String currentDateTimeString = DateFormat.getDateTimeInstance().format(new Date());
                        isPostpaid = mSharedPreferences.getBoolean("switch_preference_1", false);

                        try {
                            if (isPostpaid) {
                                balanceDue = SMSHelper.getIntFromMsgBodyRegex(smsBody, postpaidBalanceDueRegexp);
                                balanceCredit = SMSHelper.getIntFromMsgBodyRegex(smsBody, postpaidBalanceCreditRegexp);
                                stats.putInt(PrefStrings.POSTPAID_BALANCE_DUE, balanceDue);
                                stats.putInt(PrefStrings.POSTPAID_BALANCE_CREDIT, balanceCredit);
                            } else //Prepaid
                            {
                                balance = SMSHelper.getIntFromMsgBodyRegex(smsBody, prepaidBalanceRegexp);
                                stats.putInt(PrefStrings.PREPAID_BALANCE, balance);
                            }
                        } catch (Exception ex) {
                            Log.e(TAG, "Error when reading balance info from SMS: " + smsBody + " Error: " + ex.getMessage());
                        }
                        stats.putLong(PrefStrings.BALANCE_DATE, lastBalanceSMSInTime);
                        stats.putBoolean(PrefStrings.IS_POSTPAID, isPostpaid);
                        stats.apply();

                        String balanceMsg = SMSHelper.getBalanceMsgFromParts(isPostpaid, balance, balanceDue, balanceCredit, lastBalanceSMSInTime);

                        try {
                            MainActivity.getInstance().updateBalanceView(balanceMsg);
                        } catch (Exception ex) {
                            Log.e(TAG, "Error when updating Main Activity view from SMS Receiver");
                        }

                        //if SMS Pack is enabled, check
                        if (checkSMSPack) {
                            Log.d(TAG, "Enqueuing SMS for SMS pack query");
                            WorkerUtils.enqueueOneTimeSMSSendingWork(smsRecipient, smsPackQuery);
                        }

                    }//balance msg

                }//msg from telecom for balance

                //SMS pack info source
                if (smsSender.equals(mSharedPreferences.getString("pref_sms_pack_source", "1415")) && checkSMSPack) {
                    String smsPackInfoRegexp = mSharedPreferences.getString("pref_sms_pack_info_regex", ".*free sms.*?([0-9,]+).*");
                    String smsPackNullRegexp = mSharedPreferences.getString("pref_sms_pack_null_regex", ".*no free resource.*");
                    String smsRecipient = mSharedPreferences.getString("pref_sms_pack_destination", "1415");
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
                    if (SMSHelper.patternMatches(smsBody, smsPackInfoRegexp)) { //SMS Pack Info
                        isSmsPackInfo = true;
                        try {
                            Log.d(TAG, "Found SMS with SMS pack info.");
                            smsRemaining = SMSHelper.getIntFromMsgBodyRegex(smsBody, smsPackInfoRegexp);
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
                    } else if (SMSHelper.patternMatches(smsBody, smsPackNullRegexp)) {//No SMS Pack subscribed
                        Log.d(TAG, "Found SMS with SMS pack info null.");
                        stats.putInt(PrefStrings.SMS_PACK_INFO, 0);
                        stats.apply();

                        /*
                        //SMS remaining should be below critical and balance should be equal or above critical
                        if (smsRemaining < criticalSMSRemaining && mainBalance >= criticalBalanceRemaining) {
                            WorkerUtils.enqueueOneTimeSMSSendingWork(smsRecipient, smsPackMsg);
                        }
                        */
                    }
                }
                //Make one time update report after getting message from Balance or SMS Pack info
                if (isBalanceInfo || isSmsPackInfo) {
                    String instanceName = mSharedPreferences.getString("instance_name", "none");
                    String reportOneIntervalMinString = mSharedPreferences.getString("pref_interval_report_one_min", "3");
                    int reportOneIntervalMin = 3;
                    try {
                        reportOneIntervalMin = Integer.parseInt(reportOneIntervalMinString);
                    } catch (Exception ex) {
                        Log.e("Error parsing integer: ", ex.getMessage());
                    }

                    //Disabling interval check for SMS Pack info reporting because it will immediately occur
                    // after balance info reporting
                    if (isSmsPackInfo) {
                        reportOneIntervalMin = 0;
                    }
                    WorkerUtils.enqueueOneTimeStitchReportingWork(instanceName, reportOneIntervalMin);
                }

                String lastSMSInTimeMsg = DateFormat.getDateTimeInstance().format(lastUserSMSInTime);
                try {
                    MainActivity.getInstance().updateLastSMSInView(lastSMSInTimeMsg);
                } catch (Exception ex) {
                    Log.e(TAG, "Error when updating Main Activity view from SMS Receiver");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error when processing broadcast" + e.getMessage());
            }

            stats.apply();
        }

    }
}
