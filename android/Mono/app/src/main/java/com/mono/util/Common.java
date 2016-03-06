package com.mono.util;

public class Common {

    private Common() {}

    public static boolean between(int value, int min, int max) {
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
}
