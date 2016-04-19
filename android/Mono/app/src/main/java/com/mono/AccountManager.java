package com.mono;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.mono.model.Account;
import com.mono.util.Common;

import java.util.HashMap;
import java.util.Map;

public class AccountManager {

    public static final String PREF_ACCOUNT = "pref_account";
    public static final String PREF_GCM_TOKEN = "pref_gcm_token";

    private static final String ACCOUNT_ID_KEY = "id";
    private static final String ACCOUNT_USERNAME_KEY = "username";
    private static final String ACCOUNT_FIRST_NAME_KEY = "first_name";
    private static final String ACCOUNT_LAST_NAME_KEY = "last_name";

    private static AccountManager instance;

    private Context context;
    private SharedPreferences preferences;

    private Account account;

    private AccountManager(Context context) {
        this.context = context;
        preferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public static AccountManager getInstance(Context context) {
        if (instance == null) {
            instance = new AccountManager(context.getApplicationContext());
        }

        return instance;
    }

    public Account getAccount() {
        if (account != null) {
            return account;
        }

        String value = preferences.getString(PREF_ACCOUNT, null);
        if (value != null) {
            Map<String, String> values = new HashMap<>();

            for (String pair : Common.explode(",", value)) {
                String[] temp = pair.split(":");
                values.put(temp[0], temp[1]);
            }

            account = new Account(Long.parseLong(values.get(ACCOUNT_ID_KEY)));
            account.username = values.get(ACCOUNT_USERNAME_KEY);
            account.firstName = values.get(ACCOUNT_FIRST_NAME_KEY);
            account.lastName = values.get(ACCOUNT_LAST_NAME_KEY);
        }

        return account;
    }

    public void setAccount(Account account) {
        this.account = account;

        String[] values = {
            ACCOUNT_ID_KEY + ":" + account.id,
            ACCOUNT_USERNAME_KEY + ":" + account.username,
            ACCOUNT_FIRST_NAME_KEY + ":" + account.firstName,
            ACCOUNT_LAST_NAME_KEY + ":" + account.lastName
        };

        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(PREF_ACCOUNT, Common.implode(",", values));
        editor.apply();
    }

    public void removeAccount() {
        this.account = null;

        SharedPreferences.Editor editor = preferences.edit();
        editor.remove(PREF_ACCOUNT);
        editor.apply();
    }

    public String getGCMToken() {
        return preferences.getString(PREF_GCM_TOKEN, null);
    }

    public void setGCMToken(String value) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(PREF_GCM_TOKEN, value);
        editor.apply();
    }

    public void login() {

    }

    public void logout() {
        removeAccount();
    }
}
