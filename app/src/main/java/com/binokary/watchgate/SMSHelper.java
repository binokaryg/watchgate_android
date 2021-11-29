package com.binokary.watchgate;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.telephony.SmsManager;

import androidx.core.content.ContextCompat;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SMSHelper {

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

    public static String getBalanceMsgFromParts(boolean isPostpaid, int balance, int balanceDue, int balanceCredit, long dateInMS) {
        String dateTimeString = DateFormat.getDateTimeInstance().format(dateInMS);
        if (isPostpaid) {
            return String.format(Locale.US, "Due: Rs %d, Credit: Rs %d", balanceDue, balanceCredit, dateTimeString);
        } else {
            return String.format(Locale.US, "Rs %d (%s)", balance, dateTimeString);
        }
    }

    public static Integer getIntFromMsgBodyRegex(String msgBody, String regexp) {
        Pattern pattern = Pattern.compile(regexp);
        Matcher matcher = pattern.matcher(msgBody);
        if (matcher.find())
        {
            String matched = Objects.requireNonNull(matcher.group(1)).replace(",", "").replace(".","");
            return Integer.parseInt(matched);
        }
        return -1;
    }

    public static Boolean patternMatches(String msgBody, String regexp) {
        Pattern pattern = Pattern.compile(regexp, Pattern.DOTALL);
        Matcher matcher = pattern.matcher(msgBody);
        return matcher.matches();
    }


}
