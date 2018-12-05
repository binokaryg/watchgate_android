package com.binokary.watchgate;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.support.v4.content.ContextCompat;
import android.telephony.SmsManager;
import android.util.Log;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SMSHelper {
    public static final String SMS_CONDITION_PREPAID = "Dear Customer, your current balance";
    public static final String SMS_CONDITION_POSTPAID = "Dear customer, your due amount";
    private static final String TAG = Constants.MAINTAG + SMSHelper.class.getSimpleName();

    public SMSHelper() {
    }

    public static void sendSms(String number, String smsBody) {
        SmsManager smsManager = SmsManager.getDefault();
        smsManager.sendTextMessage(number, null, smsBody, null, null);
    }

    public static boolean hasAllNecessaryPermissions(Context mContext) {
        return ContextCompat.checkSelfPermission(mContext,
                Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(mContext,
                        Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(mContext,
                        Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(mContext,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(mContext,
                        Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED;
    }

    public static int getPrepaidBalanceFromMsgBody(String msgBody) {
        int balance = -1;
        try {
            int startIndex = msgBody.indexOf("is Rs") + 6;
            int endIndex = msgBody.indexOf("Expiry") - 1;
            balance = Math.round(Float.parseFloat(msgBody.substring(startIndex, endIndex)));
        } catch (Exception ex) {
            Log.e(TAG, "Error retrieving balance info: " + ex.getMessage());
        } finally {
            return balance;
        }
    }

    public static int[] getPostpaidBalanceFromMsgBody(String msgBody) {
        int balance[] = {-1, -1};
        int due = -1;
        int credit = -1;
        try {
            int startIndex = msgBody.indexOf("due amount is Rs ") + 17;
            int endIndex = msgBody.indexOf("and your available credit") - 2;
            int startIndex2 = endIndex + 34;
            int endIndex2 = msgBody.indexOf("Please dial") - 2;
            String dueS = msgBody.substring(startIndex, endIndex).replace(",","");
            String creditS = msgBody.substring(startIndex2, endIndex2).replace(",","");
            due = Math.round(Float.parseFloat(dueS));
            credit = Math.round(Float.parseFloat(creditS));
            balance[0] = due;
            balance[1] = credit;

        } catch (Exception ex) {
            Log.e(TAG, "Error retrieving balance info: " + ex.getMessage());
        } finally {
            return balance;
        }
    }

    public static String getBalanceMsgFromParts(boolean isPostpaid, int balance, int balanceDue, int balanceCredit, long dateinMS) {
        String dateTimeString = DateFormat.getDateTimeInstance().format(dateinMS);
        if (isPostpaid) {
            return String.format("Due: Rs %d, Credit: Rs %d", balanceDue, balanceCredit, dateTimeString);
        } else {
            return String.format("Rs %d @ %s", balance, dateTimeString);
        }
    }

    public static List<Integer> getPostpaidBalanceFromMsgBodyRegex(String msgBody){
        List<Integer> balanceData = new ArrayList<Integer>();
        Pattern pattern = Pattern.compile("Rs\\s(.*?)\\.");
        Matcher matcher = pattern.matcher(msgBody);
        while (matcher.find())
        {
            String matched = matcher.group(1).replace(",", "").replace(".","");
            int matchedInt = Integer.parseInt(matched);
            balanceData.add(matchedInt);
        }
        return balanceData;
    }

    public static int getPrepaidBalanceFromMsgBodyRegex(String msgBody){
        ArrayList balanceData = new ArrayList();
        Pattern pattern = Pattern.compile("Rs\\s(.*?)\\.");
        Matcher matcher = pattern.matcher(msgBody);
        if (matcher.find())
        {
            String matched = matcher.group(1).replace(",", "").replace(".","");
            return Integer.parseInt(matched);
        }
        return -1;
    }

    public static Integer getIntFromMsgBodyRegex(String msgBody, String regexp) {
        Pattern pattern = Pattern.compile(regexp);
        Matcher matcher = pattern.matcher(msgBody);
        if (matcher.find())
        {
            String matched = matcher.group(1).replace(",", "").replace(".","");
            return Integer.parseInt(matched);
        }
        return -1;
    }

    public static Boolean patternMatches(String msgBody, String regexp) {
        Pattern pattern = Pattern.compile(regexp);
        Matcher matcher = pattern.matcher(msgBody);
        return matcher.matches();
    }


}
