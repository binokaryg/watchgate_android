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
    SharedPreferences prefs;

    @Override
    public void onReceive(Context context, Intent intent) {
        //Log.d(TAG, "Result Code: " + getResultCode());
        SharedPreferences.Editor stats = context.getSharedPreferences(PREF_STATS, MODE_PRIVATE).edit();
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
            prefs = context.getSharedPreferences(Constants.PREF_STATS, MODE_PRIVATE);
            countSMSIn = prefs.getInt(PrefStrings.COUNT_SMS_IN, 0);

            mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
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
                if (smsSender.equals(mSharedPreferences.getString("pref_sms_source",""))) {

                    if (smsBody.startsWith(SMSHelper.SMS_CONDITION_PREPAID) || smsBody.startsWith(SMSHelper.SMS_CONDITION_POSTPAID)) {
                        Log.d(TAG, "Sms with condition detected");
                        Toast.makeText(context, "BroadcastReceiver caught conditional SMS: " + smsBody, Toast.LENGTH_LONG).show();
                        Log.d(TAG, "SMS detected: From " + smsSender + " With text " + smsBody);

                        lastBalanceSMSInTime = System.currentTimeMillis();
                        String currentDateTimeString = DateFormat.getDateTimeInstance().format(new Date());
                        isPostpaid = mSharedPreferences.getBoolean("switch_preference_1", false);
                        String prepaidBalanceRegexp = mSharedPreferences.getString("pref_prebalance_regex", "balance.*?([0-9]+,*[0-9]+)");
                        String postpaidBalanceDueRegexp = mSharedPreferences.getString("pref_postbalance_due_regex", "due.*?([0-9]+,*[0-9]+)");
                        String postpaidBalanceCreditRegexp = mSharedPreferences.getString("pref_postbalance_credit_regex", "credit.*?([0-9]+,*[0-9]+)");

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
                        }
                        catch(Exception ex){
                            Log.e(TAG, "Error when reading balance info from SMS: " + smsBody + " Error: " + ex.getMessage());
                        }
                        stats.putLong(PrefStrings.BALANCE_DATE, lastBalanceSMSInTime);
                        stats.putBoolean(PrefStrings.IS_POSTPAID, isPostpaid);
                        stats.apply();

                        //Make one time update report after getting Balance Message
                        String instanceName = mSharedPreferences.getString("instance_name", "none");
                        String reportOneIntervalMinString = mSharedPreferences.getString("pref_interval_report_one_min", "3");
                        int reportOneIntervalMin = 3;
                        try {
                            reportOneIntervalMin = Integer.parseInt(reportOneIntervalMinString);
                        } catch (Exception ex) {
                            Log.d("Error parsing integer: ", ex.getMessage());
                        }
                        WorkerUtils.enqueueOneTimeStitchReportingWork(instanceName, reportOneIntervalMin);
                    }
                }

                String balanceMsg = SMSHelper.getBalanceMsgFromParts(isPostpaid, balance, balanceDue, balanceCredit, lastBalanceSMSInTime);
                String lastSMSInTimeMsg = DateFormat.getDateTimeInstance().format(lastUserSMSInTime);
                try {
                    MainActivity.getInstance().updateBalanceView(balanceMsg);
                    MainActivity.getInstance().updateLastSMSInView(lastSMSInTimeMsg);
                } catch (Exception ex) {
                    Log.e(TAG, "Error when updating Main Activity view from SMS Receiver");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error when processing broadcast" + e.getMessage());
            }
        }

    }
}
