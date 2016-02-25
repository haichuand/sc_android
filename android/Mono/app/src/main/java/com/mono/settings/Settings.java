package com.mono.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;

import com.mono.util.Constants;

import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

public class Settings {

    public static final String PREF_DAY_ONE = "pref_day_one";
    public static final long DEFAULT_DAY_ONE = 0;

    public static final String PREF_CALENDARS = "pref_calendars";
    public static final Set<String> DEFAULT_CALENDARS = new HashSet<>();

    public static final String PREF_CALENDARS_START_TIME = "pref_calendars_start_time";
    public static final long DEFAULT_CALENDARS_START_TIME = -52 * Constants.WEEK_MS;

    public static final String PREF_CALENDARS_END_TIME = "pref_calendars_end_time";
    public static final long DEFAULT_CALENDARS_END_TIME = 10 * Constants.WEEK_MS;

    public static final String PREF_CALENDAR_START_TIME = "pref_calendar_start_time";
    public static final long DEFAULT_CALENDAR_START_TIME = 0;

    public static final String PREF_CALENDAR_END_TIME = "pref_calendar_end_time";
    public static final long DEFAULT_CALENDAR_END_TIME = 0;

    public static final String PREF_MAP_TYPE = "pref_map_type";
    public static final int DEFAULT_MAP_TYPE = 1;

    public static final String PREF_PERMISSION_CHECK = "pref_permission_check";
    public static final boolean DEFAULT_PERMISSION_CHECK = false;

    private static Settings instance;

    private Context context;

    private Settings(Context context) {
        this.context = context;
    }

    public static void initialize(Context context) {
        instance = new Settings(context);
    }

    private SharedPreferences getPreferences() {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    private Editor getEditor() {
        return getPreferences().edit();
    }

    public static long getDayOne() {
        return instance.getPreferences().getLong(PREF_DAY_ONE, DEFAULT_DAY_ONE);
    }

    public static void setDayOne(long milliseconds) {
        Editor editor = instance.getEditor();
        editor.putLong(PREF_DAY_ONE, milliseconds);
        editor.apply();
    }

    public static Set<String> getCalendars() {
        return instance.getPreferences().getStringSet(PREF_CALENDARS, DEFAULT_CALENDARS);
    }

    public static long getCalendarsStartTime() {
        long milliseconds = Calendar.getInstance().getTimeInMillis();
        milliseconds += DEFAULT_CALENDARS_START_TIME;

        return instance.getPreferences().getLong(Settings.PREF_CALENDARS_START_TIME, milliseconds);
    }

    public static long getCalendarsEndTime() {
        long milliseconds = Calendar.getInstance().getTimeInMillis();
        milliseconds += DEFAULT_CALENDARS_END_TIME;

        return instance.getPreferences().getLong(Settings.PREF_CALENDARS_END_TIME, milliseconds);
    }

    public static long getCalendarStartTime(String id, long defaultTime) {
        String key = String.format("%s_%s", Settings.PREF_CALENDAR_START_TIME, id);
        return instance.getPreferences().getLong(key, defaultTime);
    }

    public static void setCalendarStartTime(String id, long milliseconds) {
        String key = String.format("%s_%s", Settings.PREF_CALENDAR_START_TIME, id);

        Editor editor = instance.getEditor();
        editor.putLong(key, milliseconds);
        editor.apply();
    }

    public static long getCalendarEndTime(String id, long defaultTime) {
        String key = String.format("%s_%s", Settings.PREF_CALENDAR_END_TIME, id);
        return instance.getPreferences().getLong(key, defaultTime);
    }

    public static void setCalendarEndTime(String id, long milliseconds) {
        String key = String.format("%s_%s", Settings.PREF_CALENDAR_END_TIME, id);

        Editor editor = instance.getEditor();
        editor.putLong(key, milliseconds);
        editor.apply();
    }

    public static int getMapType() {
        return instance.getPreferences().getInt(PREF_MAP_TYPE, DEFAULT_MAP_TYPE);
    }

    public static void setMapType(int value) {
        Editor editor = instance.getEditor();
        editor.putInt(PREF_MAP_TYPE, value);
        editor.apply();
    }

    public static boolean getPermissionCheck() {
        return instance.getPreferences().getBoolean(PREF_PERMISSION_CHECK,
            DEFAULT_PERMISSION_CHECK);
    }

    public static void setPermissionCheck(boolean value) {
        Editor editor = instance.getEditor();
        editor.putBoolean(PREF_PERMISSION_CHECK, value);
        editor.apply();
    }
}
