package com.mono.util;

import android.content.Context;
import android.content.res.Resources;

public class Log {

    private static final String PREFIX = "Mono:%s";

    private static Context context;

    private Log() {}

    public static void initialize(Context context) {
        Log.context = context;
    }

    public static void debug(String tag, String msg, Object... args) {
        android.util.Log.d(String.format(PREFIX, tag), String.format(msg, args));
    }

    public static void debug(String tag, String msg, Throwable tr, Object... args) {
        android.util.Log.d(String.format(PREFIX, tag), String.format(msg, args), tr);
    }

    public static void debug(String tag, int id, Object... args) {
        debug(tag, getString(id), args);
    }

    public static void debug(String tag, int id, Throwable tr, Object... args) {
        debug(tag, getString(id), tr, args);
    }

    public static void error(String tag, String msg, Object... args) {
        android.util.Log.e(String.format(PREFIX, tag), String.format(msg, args));
    }

    public static void error(String tag, String msg, Throwable tr, Object... args) {
        android.util.Log.e(String.format(PREFIX, tag), String.format(msg, args), tr);
    }

    public static void error(String tag, int id, Object... args) {
        error(tag, getString(id), args);
    }

    public static void error(String tag, int id, Throwable tr, Object... args) {
        error(tag, getString(id), tr, args);
    }

    public static void log(String tag, String msg, Object... args) {
        android.util.Log.d(String.format(PREFIX, tag), String.format(msg, args));
    }

    public static void log(String tag, String msg, Throwable tr, Object... args) {
        android.util.Log.d(String.format(PREFIX, tag), String.format(msg, args), tr);
    }

    public static void log(String tag, int id, Object... args) {
        log(tag, getString(id), args);
    }

    public static void log(String tag, int id, Throwable tr, Object... args) {
        log(tag, getString(id), tr, args);
    }

    private static String getString(int id) {
        String msg;

        Resources resources = context.getResources();
        if (resources == null) {
            msg = Strings.STRING_RESOLVE_FAILED;
        } else {
            msg = resources.getString(id);
        }

        return msg;
    }
}
