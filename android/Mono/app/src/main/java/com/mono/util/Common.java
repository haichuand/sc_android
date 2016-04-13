package com.mono.util;

import java.security.MessageDigest;
import java.util.List;

public class Common {

    private Common() {}

    public static boolean between(int value, int min, int max) {
        return value >= min && value <= max;
    }

    public static boolean between(float value, float min, float max) {
        return value >= min && value <= max;
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
        return value.split(delimiter);
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
}
