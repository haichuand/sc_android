package com.mono.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.text.Html;
import android.text.Spanned;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;

import org.joda.time.LocalDate;

import java.io.File;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * This class is used to provide common helper functions such as determining if values are between
 * a range, applying bounds to a specific value, and many more.
 *
 * @author Gary Ng
 */
public class Common {

    private Common() {}

    public static boolean between(int value, int min, int max) {
        return value >= min && value <= max;
    }

    public static boolean between(float value, float min, float max) {
        return value >= min && value <= max;
    }

    public static boolean between(LocalDate value, LocalDate min, LocalDate max) {
        return value.isEqual(min) || value.isAfter(min) && value.isBefore(max) ||
            value.isEqual(max);
    }

    public static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(value, max));
    }

    public static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(value, max));
    }

    public static boolean compareStrings(String str1, String str2) {
        if (str1 != null && !str1.equals(str2) || str2 != null && !str2.equals(str1)) {
            return false;
        }

        return true;
    }

    public static boolean compareStringLists (List<String> list1, List<String> list2) {
        if (list1 == null || list2 == null || list1.size() != list2.size()) {
            return false;
        }
        Collections.sort(list1);
        Collections.sort(list2);
        for (int i = 0; i < list1.size(); i++) {
            if (!list1.get(i).equals(list2.get(i))) {
                return false;
            }
        }
        return true;
    }

    public static boolean isEmpty(CharSequence str) {
        return str == null || str.length() == 0;
    }

    public static boolean contains(String str, String[] values) {
        boolean result = false;

        str = str.toLowerCase();
        for (String value : values) {
            value = value.trim().toLowerCase();
            if (value.isEmpty()) {
                continue;
            }

            if (str.contains(value)) {
                result = true;
                break;
            }
        }

        return result;
    }

    public static boolean containsAll(String str, String[] values) {
        boolean result = true;

        str = str.toLowerCase();
        for (String value : values) {
            value = value.trim().toLowerCase();
            if (value.isEmpty()) {
                continue;
            }

            if (!str.contains(value)) {
                result = false;
                break;
            }
        }

        return result;
    }

    public static int diff(String str1, String str2) {
        int index = -1;

        int strLen1 = str1.length(), strLen2 = str2.length();
        int size = Math.max(strLen1, strLen2);

        for (int i = 0; i < size; i++) {
            if (i >= strLen1 || i >= strLen2 || str1.charAt(i) != str2.charAt(i)) {
                index = i;
                break;
            }
        }

        return index;
    }

    public static boolean fileExists(String path) {
        return new File(path).exists() && fileSize(path) > 0;
    }

    public static long fileSize(String path) {
        return new File(path).length();
    }

    public static String formatPhone(String phone) {
        PhoneNumberUtil instance = PhoneNumberUtil.getInstance();
        String region = Locale.getDefault().getCountry();

        try {
            phone = instance.format(instance.parse(phone, region),
                PhoneNumberUtil.PhoneNumberFormat.NATIONAL);
        } catch (NumberParseException e) {
            e.printStackTrace();
        }

        return phone;
    }

    public static String[] explode(String delimiter, String value) {
        return value.split("\\" + delimiter);
    }

    public static String implode(String delimiter, String[] values) {
        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < values.length; i++) {
            String value = values[i];
            if (i > 0) builder.append(delimiter);
            builder.append(value);
        }

        return builder.toString();
    }

    public static String implode(String delimiter, List<String> values) {
        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < values.size(); i++) {
            String value = values.get(i);
            if (i > 0) builder.append(delimiter);
            builder.append(value);
        }

        return builder.toString();
    }

    public static String repeat(String value, int length, String delimiter) {
        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < length; i++) {
            if (delimiter != null && i > 0) {
                builder.append(delimiter);
            }
            builder.append(value);
        }

        return builder.toString();
    }

    public static Spanned highlight(String text, String[] terms, int color) {
        String result = text != null ? text : "";

        if (terms != null && terms.length > 0) {
            Arrays.sort(terms, new Comparator<String>() {
                @Override
                public int compare(String s1, String s2) {
                    return Integer.compare(s2.length(), s1.length());
                }
            });

            for (String term : terms) {
                int length = term.length();

                int index, offset = 0;
                while ((index = result.toLowerCase().indexOf(term.toLowerCase(), offset)) != -1) {
                    result = String.format(
                        "%s!<%s!>%s",
                        result.substring(0, index),
                        result.substring(index, index + length),
                        result.substring(index + length)
                    );

                    offset = index + 2 + length + 2;
                }
            }
        }

        result = result.replace("!<", String.format("<font color=\"#%x\">", color & 0x00FFFFFF));
        result = result.replace("!>", "</font>");
        result = result.replace("\n", "<br>");

        return Html.fromHtml(result);
    }

    public static String md5(String str) {
        String result = null;

        try {
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            byte[] digest = messageDigest.digest(str.getBytes("UTF-8"));

            StringBuilder builder = new StringBuilder();
            for (byte b : digest) {
                builder.append(String.format("%02x", b & 0xFF));
            }

            result = builder.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    public static int random(int... values) {
        return values[(int) (Math.random() * values.length) % values.length];
    }

    public static String random(String... values) {
        return values[(int) (Math.random() * values.length) % values.length];
    }

    public static String sha1(String str) {
        String result = null;

        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-1");
            byte[] digest = messageDigest.digest(str.getBytes("UTF-8"));

            StringBuilder builder = new StringBuilder();
            for (byte b : digest) {
                builder.append(String.format("%02x", b & 0xFF));
            }

            result = builder.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    public static boolean isConnectedToInternet(Context context) {
        NetworkInfo activeNetwork = ((ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    public static List<Integer> convertIdList (List<String> idList) {
        if (idList == null) {
            return null;
        }
        List<Integer> idIntegerList = new ArrayList<>();
        for (String id : idList) {
            idIntegerList.add(Integer.valueOf(id));
        }
        return idIntegerList;
    }

    /**
     * For changing holiday timestamps from UTC to current time zone
     * @param ms
     * @return Timestamp of the holiday in current time zone
     */
    public static long convertHolidayMillis (long ms) {
        Calendar utcCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        utcCalendar.setTimeInMillis(ms);
        Calendar localCalendar = Calendar.getInstance();
        localCalendar.set(
                utcCalendar.get(Calendar.YEAR),
                utcCalendar.get(Calendar.MONTH),
                utcCalendar.get(Calendar.DAY_OF_MONTH),
                utcCalendar.get(Calendar.HOUR_OF_DAY),
                utcCalendar.get(Calendar.MINUTE),
                utcCalendar.get(Calendar.SECOND)
                );
        return localCalendar.getTimeInMillis() / 1000 * 1000;
    }
}
