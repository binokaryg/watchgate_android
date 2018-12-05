package com.binokary.watchgate;

import android.util.Log;

import org.junit.Test;

import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {

    String SMSQueryMsg_Prepaid = "Dear Customer, your current balance is Rs 614.98,Expiry Date is 2020-06-13 23:59:59. Please dial *1415# to subscribe and query data package.-NT";
    String SMSQueryMsg_Postpaid = "Dear customer, your due amount is Rs 6,709.69, and your available credit is Rs 2,869.19.  Please dial *1415# to subscribe and query data package.-NT";
    String SMSQueryMsg_Postpaid2 = "Dear customer, your due amount is Rs 464.38, and your available credit is Rs 2,044.70.  Please dial *1415# to subscribe and query data package.-NT";
    String SMSPackInfo = "Dear customer, your current free sms is 1357 piece";
    String SMSPackInfo0 = "Dear customer, your current free sms is 0 piece";
    String SMSPackNo = "Dear Customer, you have no free resource available. ";
    @Test
    public void get_balance_info_from_SMS_prepaid() throws Exception {
        assertEquals(614, SMSHelper.getPrepaidBalanceFromMsgBodyRegex(SMSQueryMsg_Prepaid));
    }

    @Test
    public void get_balance_info_from_SMS_postpaid() throws Exception {
        int expectedDue = 6709;
        int expectedCredit = 2869;
        assertEquals (expectedDue, (int) SMSHelper.getPostpaidBalanceFromMsgBodyRegex(SMSQueryMsg_Postpaid).get(0));
        assertEquals(expectedCredit, (int) SMSHelper.getPostpaidBalanceFromMsgBodyRegex(SMSQueryMsg_Postpaid).get(1));
    }

    @Test
    public void checkIntParse() throws Exception {
        assertEquals(23, (int) Float.parseFloat("23.32"));
    }

    @Test
    public void checkDate() throws Exception {
        Long dateL = System.currentTimeMillis();
        Date date = new Date(dateL);
        Date now = new Date();
        Long diff = now.getTime() - date.getTime();
        //System.out.println(diff.toString());
        assertTrue(diff < 100L);
    }

    @Test
    public void getPostpaidDue() throws Exception {
        String mydata = "Dear customer, your due amount is Rs 0.0, and your available credit is Rs 0.0. Please dial *1415# to subscribe and query data package.-NT";
        Pattern pattern = Pattern.compile("Dear customer, your due.*?([0-9,]+).*");
        Matcher matcher = pattern.matcher(mydata);
        while (matcher.find())
        {
            //System.out.println(matcher.group(1));
            assertEquals(matcher.group(1),"0");
        }
    }

    @Test
    public void getPostpaidDue2() throws Exception {
        String mydata = "Dear customer, your due amount is Rs 7368.56, and your available credit is Rs 37282.72. Please dial *1415# to subscribe and query data package.-NT";
        Pattern pattern = Pattern.compile(".*your due.*?([0-9,]+).*");
        Matcher matcher = pattern.matcher(mydata);
        while (matcher.find())
        {
            //System.out.println(matcher.group(1));
            assertEquals(matcher.group(1),"7368");
        }
    }

    @Test
    public void getPostpaidCredit() throws Exception {
        String mydata = "Dear customer, your due amount is Rs 0.0, and your available credit is Rs 0.0. Please dial *1415# to subscribe and query data package.-NT";
        Pattern pattern = Pattern.compile(".*available credit.*?([0-9,]+).*");
        Matcher matcher = pattern.matcher(mydata);
        while (matcher.find())
        {
            //System.out.println(matcher.group(1));
            assertEquals(matcher.group(1),"0");
        }
    }

    @Test
    public void getPostpaidCredit2() throws Exception {
        String mydata = "Dear customer, your due amount is Rs 8201.20, and your available credit is Rs 2822.45. Please dial *1415# to subscribe and query data package.-NT";
        Pattern pattern = Pattern.compile(".*available credit.*?([0-9,]+).*");
        Matcher matcher = pattern.matcher(mydata);
        while (matcher.find())
        {
            //System.out.println(matcher.group(1));
            assertEquals(matcher.group(1),"2822");
        }
    }

    @Test
    public void getPrepaid() throws Exception {
        String mydata = SMSQueryMsg_Prepaid;
        Pattern pattern = Pattern.compile(".*current balance.*?([0-9,]+).*");
        Matcher matcher = pattern.matcher(mydata);
        while (matcher.find())
        {
            //System.out.println(matcher.group(1));
            assertEquals(matcher.group(1),"614");
        }
    }

    @Test
    public void matchPrepaid() throws Exception {
        String mydata = SMSQueryMsg_Prepaid;
        Pattern pattern = Pattern.compile(".*current balance.*?([0-9,]+).*");
        Matcher matcher = pattern.matcher(mydata);
        assertTrue(matcher.matches());
    }

    @Test
    public void matchSMSPackMsg() throws Exception {
        String mydata = SMSPackInfo;
        Pattern pattern = Pattern.compile(".*free sms.*?([0-9,]+).*");
        Matcher matcher = pattern.matcher(mydata);
        assertTrue(matcher.matches());
    }

    @Test
    public void getSMSPackInfo() throws Exception {
        String mydata = SMSPackInfo;
        Pattern pattern = Pattern.compile(".*free sms.*?([0-9,]+).*");
        Matcher matcher = pattern.matcher(mydata);
        while (matcher.find())
        {
            //System.out.println(matcher.group(1));
            assertEquals(matcher.group(1),"1357");
        }
    }
}