package com.mono;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.mono.chat.MyGcmListenerService;
import com.mono.chat.RegistrationIntentService;
import com.mono.db.DatabaseHelper;
import com.mono.db.dao.AttendeeDataSource;
import com.mono.model.Account;
import com.mono.network.ChatServerManager;
import com.mono.network.GCMHelper;
import com.mono.network.HttpServerManager;

import org.json.JSONObject;

public class LoginActivity extends AppCompatActivity {

    public static final String EXTRA_ACCOUNT = "account";

    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

    private BroadcastReceiver registrationReceiver;
    private BroadcastReceiver gcmReceiver;
    private boolean hasToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        registrationReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                hasToken = true;

                String token = AccountManager.getInstance(context).getGCMToken();
                System.out.println("New GCM Token: " + token);
            }
        };

        gcmReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Bundle data = intent.getBundleExtra(MyGcmListenerService.GCM_MESSAGE_DATA);

                String action = data.getString("action");
                if (action != null) {
                    switch (action) {
                        case GCMHelper.ACTION_LOGIN:
                            onLogin(data);
                            break;
                    }
                }
            }
        };

        String token = AccountManager.getInstance(this).getGCMToken();
        hasToken = token != null;

        if (!hasToken) {
            if (checkPlayServices()) {
                // Start IntentService to register this application with GCM.
                Intent intent = new Intent(this, RegistrationIntentService.class);
                startService(intent);
            }
        } else {
            System.out.println("Current GCM Token: " + token);
        }

        showLogin();
    }

    @Override
    protected void onResume() {
        super.onResume();

        LocalBroadcastManager.getInstance(this).registerReceiver(registrationReceiver,
                new IntentFilter(SuperCalyPreferences.REGISTRATION_COMPLETE));

        LocalBroadcastManager.getInstance(this).registerReceiver(gcmReceiver,
                new IntentFilter(MyGcmListenerService.GCM_INCOMING_INTENT));
    }

    @Override
    protected void onPause() {
        super.onPause();

        LocalBroadcastManager.getInstance(this).unregisterReceiver(registrationReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(gcmReceiver);
    }

    public void showFragment(Fragment fragment, String tag, boolean addToBackStack) {
        FragmentManager manager = getSupportFragmentManager();

        FragmentTransaction transaction = manager.beginTransaction();
        transaction.replace(R.id.fragment_container, fragment, tag);

        if (addToBackStack) {
            transaction.addToBackStack(tag);
        }

        transaction.commit();
    }

    public void showLogin() {
        String tag = getString(R.string.fragment_login);
        showFragment(new LoginFragment(), tag, false);
    }

    public void submitLogin(String emailOrPhone, String password) {
        if (!hasToken) {
            return;
        }
        HttpServerManager httpServerManager = new HttpServerManager(this);
        String toastMessage = null;
        switch (httpServerManager.loginUser(emailOrPhone, password)) {
            case 0:
                getUserInfoAndSetAccount(emailOrPhone, httpServerManager);
                resetUserTable(httpServerManager);
                finish();
                break;
            case 1:
            case 2:
                toastMessage = "Incorrect login information";
                break;
            case -1:
                toastMessage = "Server error. Please try again";
                break;
        }
        if (toastMessage != null) {
            Toast.makeText(this, toastMessage, Toast.LENGTH_LONG).show();
        }
    }

    private void getUserInfoAndSetAccount(String emailOrPhone, HttpServerManager httpServerManager) {
        JSONObject responseJson;
        if (emailOrPhone.contains("@")) {
            responseJson = httpServerManager.getUserByEmail(emailOrPhone);
        } else {
            responseJson = httpServerManager.getUserByPhone(emailOrPhone);
        }

        try {
            Account account = new Account(responseJson.getInt(HttpServerManager.UID));
            account.firstName = responseJson.getString(HttpServerManager.FIRST_NAME);
            account.lastName = responseJson.getString(HttpServerManager.LAST_NAME);
            account.username = responseJson.getString(HttpServerManager.USER_NAME);
            account.email = responseJson.getString(HttpServerManager.EMAIL);
            AccountManager accountManager = AccountManager.getInstance(this);
            accountManager.setAccount(account);

            //refresh GCM tokens on http and chat servers
            String token = accountManager.getGCMToken();
            if (httpServerManager.updateUserGcmId(account.id, token) != 0) {
                Toast.makeText(this, "Error updating GCM token on http server", Toast.LENGTH_LONG).show();
            }
            new ChatServerManager(this).updateUserGcmId(account.id, token);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void resetUserTable(HttpServerManager httpServerManager) {
        AttendeeDataSource attendeeDataSource = DatabaseHelper.getDataSource(this, AttendeeDataSource.class);
        attendeeDataSource.clearAttendeeTable();
        httpServerManager.addAllRegisteredUsersToUserTable(attendeeDataSource);
    }

    public void onLogin(Bundle data) {
//        Log.d("LoginActivity", "onLogin");
        int status = Integer.parseInt(data.getString("status"));
        String username = data.getString("username");

        if (status == 0) {
            Account account = new Account(-1);
            account.username = username;

            Intent intent = getIntent();
            if (intent == null) {
                intent = new Intent();
            }
            intent.putExtra(EXTRA_ACCOUNT, account);

            setResult(RESULT_OK, intent);
            finish();
        }
    }

    public void showRegister() {
        String tag = getString(R.string.fragment_register);
        showFragment(new RegisterFragment(), tag, true);
    }

    public void submitRegister(String email, String phone, String firstName, String lastName,
            String userName, String password) {
        if (!hasToken) {
            return;
        }

        String token = AccountManager.getInstance(this).getGCMToken();
        HttpServerManager httpServerManager = new HttpServerManager(this);
        String toastMessage;
        int uId = httpServerManager.createUser(email, firstName, token, lastName, null, phone, userName, password);
        if (uId > 0) {
            ChatServerManager chatServerManager = new ChatServerManager(this);
            chatServerManager.sendRegister(uId, token);
            toastMessage = "Registration successful.";
            finish();
        } else if (uId == 0){
            toastMessage = "The email address or phone number has already been used.";
        } else {
            toastMessage = "Registration unsuccessful. Please try again.";
        }

        Toast.makeText(this, toastMessage, Toast.LENGTH_LONG).show();
    }

    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */
    private boolean checkPlayServices() {
        GoogleApiAvailability instance = GoogleApiAvailability.getInstance();
        int resultCode = instance.isGooglePlayServicesAvailable(this);

        if (resultCode != ConnectionResult.SUCCESS) {
            if (instance.isUserResolvableError(resultCode)) {
                instance.getErrorDialog(this, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                finish();
            }

            return false;
        }

        return true;
    }
}