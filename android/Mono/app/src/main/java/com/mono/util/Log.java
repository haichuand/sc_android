package com.mono.util;

import android.content.Context;
import android.content.res.Resources;

public class Log {

    private static final String PREFIX = "Mono:%s";

    private static Log instance;

    private Context context;

    private Log(Context context) {
        this.context = context;
    }

    public static Log getInstance(Context context) {
        if (instance == null) {
            instance = new Log(context.getApplicationContext());
        }

        return instance;
    }

    public void debug(String tag, String msg, Object... args) {
        android.util.Log.d(String.format(PREFIX, tag), String.format(msg, args));
    }

    public void debug(String tag, String msg, Throwable tr, Object... args) {
        android.util.Log.d(String.format(PREFIX, tag), String.format(msg, args), tr);
    }

    public void debug(String tag, int id, Object... args) {
        debug(tag, getString(id), args);
    }

    public void debug(String tag, int id, Throwable tr, Object... args) {
        debug(tag, getString(id), tr, args);
    }

    public void error(String tag, String msg, Object... args) {
        android.util.Log.e(String.format(PREFIX, tag), String.format(msg, args));
    }

    public void error(String tag, String msg, Throwable tr, Object... args) {
        android.util.Log.e(String.format(PREFIX, tag), String.format(msg, args), tr);
    }

    public void error(String tag, int id, Object... args) {
        error(tag, getString(id), args);
    }

    public void error(String tag, int id, Throwable tr, Object... args) {
        error(tag, getString(id), tr, args);
    }

    public void log(String tag, String msg, Object... args) {
        android.util.Log.d(String.format(PREFIX, tag), String.format(msg, args));
    }

    public void log(String tag, String msg, Throwable tr, Object... args) {
        android.util.Log.d(String.format(PREFIX, tag), String.format(msg, args), tr);
    }

    public void log(String tag, int id, Object... args) {
        log(tag, getString(id), args);
    }

    public void log(String tag, int id, Throwable tr, Object... args) {
        log(tag, getString(id), tr, args);
    }

    private String getString(int id) {
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
