package com.binokary.watchgate;

import org.junit.Test;

import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {

    final String SMSQueryMsg_Prepaid = "Dear Customer, your current balance is Rs 614.98,Expiry Date is 2020-06-13 23:59:59. Please dial *1415# to subscribe and query data package.-NT";
    String SMSQueryMsg_Postpaid = "Dear customer, your due amount is Rs 6,709.69, and your available credit is Rs 2,869.19.  Please dial *1415# to subscribe and query data package.-NT";
    String SMSQueryMsg_NCell = "Hi, Your Balance: RS. 828.53 ,Loan balance: Rs. 0.\n" +
            "You have 1971.22 MB Of Viber Data expiring on 2021-12-15 18:06:19,9799.08 MB Of All Time Data expiring on 2021-12-15 18:06:19,left.Ncell";
    final String SMSPackInfo = "Dear customer, your current free sms is 1357 piece";

    @Test
    public void checkIntParse() {
        assertEquals(23, (int) Float.parseFloat("23.32"));
    }

    @Test
    public void checkDate() {
        long dateL = System.currentTimeMillis();
        Date date = new Date(dateL);
        Date now = new Date();
        long diff = now.getTime() - date.getTime();
        //System.out.println(diff.toString());
        assertTrue(diff < 100L);
    }

    @Test
    public void getPostpaidDue() {
        String myData = "Dear customer, your due amount is Rs 0.0, and your available credit is Rs 0.0. Please dial *1415# to subscribe and query data package.-NT";
        Pattern pattern = Pattern.compile("Dear customer, your due.*?([0-9,]+).*");
        Matcher matcher = pattern.matcher(myData);
        while (matcher.find())
        {
            //System.out.println(matcher.group(1));
            assertEquals("0", matcher.group(1));
        }
    }

    @Test
    public void getPostpaidDue2() {
        String myData = SMSQueryMsg_Postpaid;
        Pattern pattern = Pattern.compile(".*your due.*?([0-9,]+).*");
        Matcher matcher = pattern.matcher(myData);
        while (matcher.find())
        {
            //System.out.println(matcher.group(1));
            assertEquals("6,709", matcher.group(1));
        }
    }

    @Test
    public void getPostpaidCredit() {
        String myData = "Dear customer, your due amount is Rs 0.0, and your available credit is Rs 0.0. Please dial *1415# to subscribe and query data package.-NT";
        Pattern pattern = Pattern.compile(".*available credit.*?([0-9,]+).*");
        Matcher matcher = pattern.matcher(myData);
        while (matcher.find())
        {
            //System.out.println(matcher.group(1));
            assertEquals("0", matcher.group(1));
        }
    }

    @Test
    public void getPostpaidCredit2() {
        String myData = "Dear customer, your due amount is Rs 8201.20, and your available credit is Rs 2822.45. Please dial *1415# to subscribe and query data package.-NT";
        Pattern pattern = Pattern.compile(".*available credit.*?([0-9,]+).*");
        Matcher matcher = pattern.matcher(myData);
        while (matcher.find())
        {
            //System.out.println(matcher.group(1));
            assertEquals("2822", matcher.group(1));
        }
    }

    @Test
    public void getPrepaid() {
        Pattern pattern = Pattern.compile(".*current balance.*?([0-9,]+).*");
        Matcher matcher = pattern.matcher(SMSQueryMsg_Prepaid);
        while (matcher.find())
        {
            //System.out.println(matcher.group(1));
            assertEquals("614", matcher.group(1));
        }
    }

    @Test
    public void getNCell() {
        Pattern pattern = Pattern.compile(".*Your Balance:.*?([0-9,]+).*");
        Matcher matcher = pattern.matcher(SMSQueryMsg_Prepaid);
        while (matcher.find())
        {
            //System.out.println(matcher.group(1));
            assertEquals("828", matcher.group(1));
        }
    }

    @Test
    public void matchPrepaid() {
        Pattern pattern = Pattern.compile(".*current balance.*?([0-9,]+).*");
        Matcher matcher = pattern.matcher(SMSQueryMsg_Prepaid);
        assertTrue(matcher.matches());
    }

    @Test
    public void matchSMSPackMsg() {
        Pattern pattern = Pattern.compile(".*free sms.*?([0-9,]+).*");
        Matcher matcher = pattern.matcher(SMSPackInfo);
        assertTrue(matcher.matches());
    }

    @Test
    public void getSMSPackInfo() {
        Pattern pattern = Pattern.compile(".*free sms.*?([0-9,]+).*");
        Matcher matcher = pattern.matcher(SMSPackInfo);
        while (matcher.find())
        {
            //System.out.println(matcher.group(1));
            assertEquals("1357", matcher.group(1));
        }
    }

    @Test
    public void getNumbersFromList() {
        String numbers = "9841424262;9841213243";
        String[] list = numbers.split(";");
        for (String x : list) {
            System.out.println(x.trim());
        }
    }

    @Test
    public void getNumbersFromListSingleItem() {
        String numbers = "9841424262";
        String[] list = numbers.split(";");
        for (String x : list) {
            System.out.println(x.trim());
        }
    }
}