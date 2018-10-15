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

    @Test
    public void read_balance_from_SMS_prepaid() throws Exception {
        assertEquals("614.98", SMSHelper.readBalanceInfoFromMsgBody(SMSQueryMsg_Prepaid, false));
    }

    @Test
    public void read_balance_from_SMS_postpaid() throws Exception {
        assertEquals("Due: 4,331.43, Available Credit: 6,774.26", SMSHelper.readBalanceInfoFromMsgBody(SMSQueryMsg_Postpaid, true));
    }

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
        System.out.println(diff.toString());
        assertTrue(diff < 100L);
    }

    @Test
    public void getPostpaid() throws Exception {
        String mydata = "Dear customer, your due amount is Rs 0.0, and your available credit is Rs 0.0. Please dial *1415# to subscribe and query data package.-NT";
        Pattern pattern = Pattern.compile("due.*?([0-9,]+)");
        Matcher matcher = pattern.matcher(mydata);
        while (matcher.find())
        {
            System.out.println(matcher.group(1));
            //System.out.println(matcher.group(2));
        }
    }

    @Test
    public void getPrepaid() throws Exception {
        String mydata = SMSQueryMsg_Prepaid;
        Pattern pattern = Pattern.compile("balance.*?([0-9,]+)");
        Matcher matcher = pattern.matcher(mydata);
        while (matcher.find())
        {
            System.out.println(matcher.group(1));
            //System.out.println(matcher.group(2));
        }
    }
}