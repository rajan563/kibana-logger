package com.rajantechies.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class Utilities {

    private Utilities() {

    }

    public static String readMultipleChars(BufferedReader reader) throws IOException {
        int length = 1024 * 10;
        char[] chars = new char[length];
        int charsRead = reader.read(chars, 0, length);
        String result;
        if (charsRead != -1) {
            result = new String(chars, 0, charsRead);
        } else {
            result = "";
        }
        try {
            if (result.contains("custom_logging\": \"logging_bigin:")) {
                result = result.substring(result.indexOf("custom_logging"), result.indexOf(":logging_end"));
                return result;
            }
        } catch (Exception e) {
            //System.out.println("result==############" + result);
        }

        return "";
    }

    public static String callBackendTransaction(BufferedReader reader)
            throws IOException {
        int length = 1024;
        char[] chars = new char[length];
        int charsRead = reader.read(chars, 0, length);
        String result;
        if (charsRead != -1) {
            result = new String(chars, 0, charsRead);
        } else {
            result = "";
        }
        try {
            if (!result.isBlank()) {
                result = result.substring(0, result.lastIndexOf("\""));
                if (result.contains("upstream:")) {
                    result = result.substring(result.indexOf("upstream:") + 9, result.indexOf("host:") - 2);
                    return result;
                }
            }
        } catch (Exception e) {
            //System.out.println("callBackendTransaction result=" + result);
        }
        return "";
    }

    public static String hitReceived(BufferedReader reader)
            throws IOException {
        int length = 1024;
        char[] chars = new char[length];
        int charsRead = reader.read(chars, 0, length);
        String result;
        if (charsRead != -1) {
            result = new String(chars, 0, charsRead);
        } else {
            result = "";
        }
        try {
            if (result.contains("configuration_store.lua")) {
                result = result.substring(result.indexOf("[info]")-21,result.lastIndexOf("\""));
                return result;
            }
        } catch (Exception e) {
            //System.out.println("callBackendTransaction result=" + result);
        }
        return "";
    }

    public static String utcToIst(String time) {
        DateFormat formatter = null;
        String ist = "";
        Date utc = new Date();
        try {
            formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss+00:00");
            formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
            utc = formatter.parse(time);
            formatter.setTimeZone(TimeZone.getTimeZone("IST"));
            ist = formatter.format(utc);
        } catch (ParseException e) {
            DateFormat formatter1 = new SimpleDateFormat("dd/MMM/yyyy'T'HH:mm:ss+00:00");
            formatter1.setTimeZone(TimeZone.getTimeZone("UTC"));
            try {
                utc = formatter1.parse(time);
            } catch (ParseException e1) {
                e1.printStackTrace();
            }
            formatter1.setTimeZone(TimeZone.getTimeZone("IST"));
            ist = formatter1.format(utc);
        }

        return ist;
    }

}
