package com.mono.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;

public class Settings {

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
