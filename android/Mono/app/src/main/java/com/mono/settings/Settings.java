package com.mono.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;

import com.mono.util.Constants;

import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

/**
 * This class is used to centralize all actions related to the shared preferences by providing
 * access and modifications to the numerous settings values being used by the app.
 *
 * @author Gary Ng
 */
public class Settings {

    public static final String PREF_CALENDARS = "pref_calendars";
    public static final Set<String> DEFAULT_CALENDARS = new HashSet<>();

    public static final String PREF_CALENDARS_DAYS_PAST = "pref_calendars_days_past";
    public static final int DEFAULT_CALENDARS_DAYS_PAST = 5 * 365;

    public static final String PREF_CALENDARS_DAYS_FUTURE = "pref_calendars_days_future";
    public static final int DEFAULT_CALENDARS_DAYS_FUTURE = 365;

    public static final String PREF_CALENDAR_INITIAL_TIME = "pref_calendar_initial_time";
    public static final String PREF_CALENDAR_START_TIME = "pref_calendar_start_time";
    public static final String PREF_CALENDAR_END_TIME = "pref_calendar_end_time";
    public static final String PREF_CALENDAR_UPDATE_TIME = "pref_calendar_update_time";

    public static final String PREF_CALENDAR_WEEK_START = "pref_calendar_week_start";
    public static final int DEFAULT_CALENDAR_WEEK_START = 1;

    public static final String PREF_CALENDAR_WEEK_NUMBER = "pref_calendar_week_number";
    public static final boolean DEFAULT_CALENDAR_WEEK_NUMBER = false;

    public static final String PREF_CONTACTS_SCAN = "pref_contacts_scan";
    public static final long DEFAULT_CONTACTS_SCAN = 0;

    public static final String PREF_CONTACTS_SCAN_ID = "pref_contacts_scan_id";
    public static final long DEFAULT_CONTACTS_SCAN_ID = 0;

    public static final String PREF_NOTIFICATION = "pref_notification";
    public static final boolean DEFAULT_NOTIFICATION = true;

    public static final String PREF_NOTIFICATION_VIBRATE = "pref_notification_vibrate";
    public static final boolean DEFAULT_NOTIFICATION_VIBRATE = false;

    public static final String PREF_CRASH_REPORT = "pref_crash_report";
    public static final boolean DEFAULT_CRASH_REPORT = true;

    public static final String PREF_DAY_ONE = "pref_day_one";
    public static final long DEFAULT_DAY_ONE = 0;

    public static final String PREF_MAP_TYPE = "pref_map_type";
    public static final int DEFAULT_MAP_TYPE = 1;

    public static final String PREF_PERMISSION_CHECK = "pref_permission_check";
    public static final boolean DEFAULT_PERMISSION_CHECK = false;

    public static final String PREF_REMEMBER_ME = "pref_remember_me";
    public static final boolean DEFAULT_REMEMBER_ME = false;

    public static final String PREF_SCHEDULER_INTERVAL = "pref_scheduler_interval";
    public static final long DEFAULT_SCHEDULER_INTERVAL = Constants.DAY_MS;

    private static Settings instance;

    private SharedPreferences preferences;

    private Settings(Context context) {
        preferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public static Settings getInstance(Context context) {
        if (instance == null) {
            instance = new Settings(context);
        }

        return instance;
    }

    private Editor getEditor() {
        return preferences.edit();
    }

    public Set<Long> getCalendars() {
        Set<Long> calendarIds = new HashSet<>();

        Set<String> tempIds = preferences.getStringSet(PREF_CALENDARS, DEFAULT_CALENDARS);
        for (String id : tempIds) {
            calendarIds.add(Long.parseLong(id));
        }

        return calendarIds;
    }

    public long[] getCalendarsArray() {
        long[] calendarIds = null;

        Set<Long> tempIds = getCalendars();
        if (!tempIds.isEmpty()) {
            calendarIds = new long[tempIds.size()];

            int i = 0;
            for (Long id : tempIds) {
                calendarIds[i++] = id;
            }
        }

        return calendarIds;
    }

    public void setCalendars(Set<Long> calendarIds) {
        Set<String> tempIds = new HashSet<>();
        for (long id : calendarIds) {
            tempIds.add(String.valueOf(id));
        }

        Editor editor = getEditor();
        editor.putStringSet(PREF_CALENDARS, tempIds);
        editor.apply();

        long currentTime = System.currentTimeMillis();

        for (long id : calendarIds) {
            if (getCalendarInitialTime(id, 0) == 0) {
                setCalendarUpdateTime(id, currentTime);
            }
        }
    }

    public boolean setCalendarId(long id) {
        Set<Long> calendarIds = getCalendars();

        if (calendarIds.add(id)) {
            setCalendars(calendarIds);
            return true;
        }

        return false;
    }

    public boolean removeCalendarId(long id) {
        Set<Long> calendarIds = getCalendars();

        if (calendarIds.remove(id)) {
            setCalendars(calendarIds);
            return true;
        }

        return false;
    }

    public int getCalendarsDaysPast() {
        return preferences.getInt(Settings.PREF_CALENDARS_DAYS_PAST, DEFAULT_CALENDARS_DAYS_PAST);
    }

    public void setCalendarsDaysPast(int days) {
        Editor editor = getEditor();
        editor.putInt(PREF_CALENDARS_DAYS_PAST, days);
        editor.apply();
    }

    public int getCalendarsDaysFuture() {
        return preferences.getInt(Settings.PREF_CALENDARS_DAYS_FUTURE,
            DEFAULT_CALENDARS_DAYS_FUTURE);
    }

    public void setCalendarsDaysFuture(int days) {
        Editor editor = getEditor();
        editor.putInt(PREF_CALENDARS_DAYS_FUTURE, days);
        editor.apply();
    }

    public long getCalendarInitialTime(long id, long defaultTime) {
        String key = String.format("%s_%d", Settings.PREF_CALENDAR_INITIAL_TIME, id);
        return preferences.getLong(key, defaultTime);
    }

    public void setCalendarInitialTime(long id, long milliseconds) {
        String key = String.format("%s_%d", Settings.PREF_CALENDAR_INITIAL_TIME, id);

        Editor editor = getEditor();
        editor.putLong(key, milliseconds);
        editor.apply();
    }

    public long getCalendarStartTime(long id, long defaultTime) {
        String key = String.format("%s_%d", Settings.PREF_CALENDAR_START_TIME, id);
        return preferences.getLong(key, defaultTime);
    }

    public void setCalendarStartTime(long id, long milliseconds) {
        String key = String.format("%s_%d", Settings.PREF_CALENDAR_START_TIME, id);

        Editor editor = getEditor();
        editor.putLong(key, milliseconds);
        editor.apply();
    }

    public long getCalendarEndTime(long id, long defaultTime) {
        String key = String.format("%s_%d", Settings.PREF_CALENDAR_END_TIME, id);
        return preferences.getLong(key, defaultTime);
    }

    public void setCalendarEndTime(long id, long milliseconds) {
        String key = String.format("%s_%d", Settings.PREF_CALENDAR_END_TIME, id);

        Editor editor = getEditor();
        editor.putLong(key, milliseconds);
        editor.apply();
    }

    public long getCalendarUpdateTime(long id, long defaultTime) {
        String key = String.format("%s_%d", Settings.PREF_CALENDAR_UPDATE_TIME, id);
        return preferences.getLong(key, defaultTime);
    }

    public void setCalendarUpdateTime(long id, long milliseconds) {
        String key = String.format("%s_%d", Settings.PREF_CALENDAR_UPDATE_TIME, id);

        Editor editor = getEditor();
        editor.putLong(key, milliseconds);
        editor.apply();
    }

    public int getCalendarWeekStart() {
        if (!preferences.contains(PREF_CALENDAR_WEEK_START)) {
            setCalendarWeekStart(Calendar.getInstance().getFirstDayOfWeek());
        }

        String value = preferences.getString(PREF_CALENDAR_WEEK_START,
            String.valueOf(DEFAULT_CALENDAR_WEEK_START));

        return Integer.parseInt(value);
    }

    public void setCalendarWeekStart(int value) {
        Editor editor = getEditor();
        editor.putString(PREF_CALENDAR_WEEK_START, String.valueOf(value));
        editor.apply();
    }

    public boolean getCalendarWeekNumber() {
        return preferences.getBoolean(PREF_CALENDAR_WEEK_NUMBER, DEFAULT_CALENDAR_WEEK_NUMBER);
    }

    public void setCalendarWeekNumber(boolean value) {
        Editor editor = getEditor();
        editor.putBoolean(PREF_CALENDAR_WEEK_NUMBER, value);
        editor.apply();
    }

    public long getContactsScan() {
        return preferences.getLong(PREF_CONTACTS_SCAN, DEFAULT_CONTACTS_SCAN);
    }

    public void setContactsScan(long milliseconds) {
        Editor editor = getEditor();
        editor.putLong(PREF_CONTACTS_SCAN, milliseconds);
        editor.apply();
    }

    public long getContactsScanId() {
        return preferences.getLong(PREF_CONTACTS_SCAN_ID, DEFAULT_CONTACTS_SCAN_ID);
    }

    public void setContactsScanId(long id) {
        Editor editor = getEditor();
        editor.putLong(PREF_CONTACTS_SCAN_ID, id);
        editor.apply();
    }

    public boolean getNotification() {
        return preferences.getBoolean(PREF_NOTIFICATION, DEFAULT_NOTIFICATION);
    }

    public void setNotification(boolean value) {
        Editor editor = getEditor();
        editor.putBoolean(PREF_NOTIFICATION, value);
        editor.apply();
    }

    public boolean getNotificationVibrate() {
        return preferences.getBoolean(PREF_NOTIFICATION_VIBRATE, DEFAULT_NOTIFICATION_VIBRATE);
    }

    public void setNotificationVibrate(boolean value) {
        Editor editor = getEditor();
        editor.putBoolean(PREF_NOTIFICATION_VIBRATE, value);
        editor.apply();
    }

    public boolean getCrashReport() {
        return preferences.getBoolean(PREF_CRASH_REPORT, DEFAULT_CRASH_REPORT);
    }

    public void setCrashReport(boolean value) {
        Editor editor = getEditor();
        editor.putBoolean(PREF_CRASH_REPORT, value);
        editor.apply();
    }

    public long getDayOne() {
        return preferences.getLong(PREF_DAY_ONE, DEFAULT_DAY_ONE);
    }

    public void setDayOne(long milliseconds) {
        Editor editor = getEditor();
        editor.putLong(PREF_DAY_ONE, milliseconds);
        editor.apply();
    }

    public int getMapType() {
        return preferences.getInt(PREF_MAP_TYPE, DEFAULT_MAP_TYPE);
    }

    public void setMapType(int value) {
        Editor editor = getEditor();
        editor.putInt(PREF_MAP_TYPE, value);
        editor.apply();
    }

    public boolean getPermissionCheck() {
        return preferences.getBoolean(PREF_PERMISSION_CHECK, DEFAULT_PERMISSION_CHECK);
    }

    public void setPermissionCheck(boolean value) {
        Editor editor = getEditor();
        editor.putBoolean(PREF_PERMISSION_CHECK, value);
        editor.apply();
    }

    public boolean getRememberMe() {
        return preferences.getBoolean(PREF_REMEMBER_ME, DEFAULT_REMEMBER_ME);
    }

    public void setRememberMe(boolean value) {
        Editor editor = getEditor();
        editor.putBoolean(PREF_REMEMBER_ME, value);
        editor.apply();
    }

    public long getSchedulerInterval() {
        return preferences.getLong(PREF_SCHEDULER_INTERVAL, DEFAULT_SCHEDULER_INTERVAL);
    }

    public void setSchedulerInterval(long milliseconds) {
        Editor editor = getEditor();
        editor.putLong(PREF_SCHEDULER_INTERVAL, milliseconds);
        editor.apply();
    }
}
