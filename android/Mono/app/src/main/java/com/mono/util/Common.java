package com.mono.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.text.Html;
import android.text.Spanned;

import org.joda.time.LocalDate;

import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

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

    public static boolean isEmpty(CharSequence str) {
        return str == null || str.length() == 0;
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

    public static boolean isConnectedToInternet(Context context) {
        NetworkInfo activeNetwork = ((ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }
}
