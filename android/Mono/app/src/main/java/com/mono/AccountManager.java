package com.mono;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.mono.model.Account;
import com.mono.util.Common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * This manager class is used to centralize all account related actions such as saving and
 * retrieving account information stored in the shared preferences.
 *
 * @author Gary Ng
 */
public class AccountManager {

    public static final String PREF_ACCOUNT = "pref_account";
    public static final String PREF_GCM_TOKEN = "pref_gcm_token";

    private static final String ACCOUNT_ID_KEY = "id";
    private static final String ACCOUNT_USERNAME_KEY = "username";
    private static final String ACCOUNT_FIRST_NAME_KEY = "first_name";
    private static final String ACCOUNT_LAST_NAME_KEY = "last_name";
    private static final String ACCOUNT_EMAIL_KEY = "email";
    private static final String ACCOUNT_PHONE_KEY = "phone";
    private static final String ACCOUNT_STATUS_KEY = "status";

    private static AccountManager instance;

    private Context context;
    private SharedPreferences preferences;

    private Account account;

    private List<AccountListener> listeners = new ArrayList<>();

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

    /**
     * Add listener to observe the status of logging in.
     *
     * @param listener The callback listener.
     */
    public void addListener(AccountListener listener) {
        listeners.add(listener);
    }

    /**
     * Remove listener from observing any future status of logging in.
     *
     * @param listener The callback listener.
     */
    public void removeListener(AccountListener listener) {
        Iterator<AccountListener> iterator = listeners.iterator();

        while (iterator.hasNext()) {
            if (iterator.next() == listener) {
                iterator.remove();
            }
        }
    }

    /**
     * Retrieve the account from cache or shared preferences. Account will need to be parsed
     * when reading from shared preferences.
     *
     * @return an instance of the account.
     */
    public Account getAccount() {
        if (account != null) {
            return account;
        }

        String value = preferences.getString(PREF_ACCOUNT, null);
        if (value != null) {
            Map<String, String> values = new HashMap<>();

            for (String pair : Common.explode(",", value)) {
                String[] temp = Common.explode(":", pair);
                values.put(temp[0], temp.length > 1 ? temp[1] : null);
            }

            account = new Account(Long.parseLong(values.get(ACCOUNT_ID_KEY)));
            account.username = values.get(ACCOUNT_USERNAME_KEY);
            account.firstName = values.get(ACCOUNT_FIRST_NAME_KEY);
            account.lastName = values.get(ACCOUNT_LAST_NAME_KEY);
            account.email = values.get(ACCOUNT_EMAIL_KEY);
            account.phone = values.get(ACCOUNT_PHONE_KEY);
            account.status = Integer.parseInt(values.get(ACCOUNT_STATUS_KEY));
        }

        return account;
    }

    /**
     * Set the account as reference for future use as well as updating the shared preferences with
     * the account.
     *
     * @param account an instance of the account.
     */
    private void setAccount(Account account) {
        this.account = account;

        String[] values = {
            ACCOUNT_ID_KEY + ":" + account.id,
            ACCOUNT_USERNAME_KEY + ":" + account.username,
            ACCOUNT_FIRST_NAME_KEY + ":" + account.firstName,
            ACCOUNT_LAST_NAME_KEY + ":" + account.lastName,
            ACCOUNT_EMAIL_KEY + ":" + account.email,
            ACCOUNT_PHONE_KEY + ":" + account.phone,
            ACCOUNT_STATUS_KEY + ":" + account.status
        };

        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(PREF_ACCOUNT, Common.implode(",", values));
        editor.apply();
    }

    /**
     * Remove the account from memory as well as from the shared preferences.
     */
    public void removeAccount() {
        this.account = null;

        SharedPreferences.Editor editor = preferences.edit();
        editor.remove(PREF_ACCOUNT);
        editor.apply();
    }

    /**
     * Retrieve the GCM token used by this account.
     *
     * @return the value of the GCM token.
     */
    public String getGCMToken() {
        return preferences.getString(PREF_GCM_TOKEN, null);
    }

    /**
     * Store the GCM token to be used by this account in the shared preferences.
     *
     * @param value The value of the GCM token.
     */
    public void setGCMToken(String value) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(PREF_GCM_TOKEN, value);
        editor.apply();
    }

    /**
     * Handle the action of logging in and inform any listeners.
     *
     * @param account The instance of the account.
     */
    public void login(Account account) {
        if (this.account != null && this.account.id != account.id) {
            logout();
        }

        account.status = Account.STATUS_ONLINE;
        setAccount(account);

        for (AccountListener listener : listeners) {
            listener.onLogin(account);
        }
    }

    /**
     * Handle the action of logging out and inform any listeners.
     */
    public void logout() {
        if (account == null) {
            return;
        }

        account.status = Account.STATUS_NONE;
        setAccount(account);

        for (AccountListener listener : listeners) {
            listener.onLogout();
        }
    }

    public interface AccountListener {

        void onLogin(Account account);

        void onLogout();
    }
}
