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

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.mono.chat.GcmMessage;
import com.mono.chat.MyGcmListenerService;
import com.mono.chat.RegistrationIntentService;
import com.mono.model.Account;
import com.mono.network.GCMHelper;

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

    public void submitLogin(String username, String password) {
        if (!hasToken) {
            return;
        }

        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);

        GcmMessage gcmMessage = GcmMessage.getInstance(this);
        Bundle args = GCMHelper.getLoginMessage(username, password);
        gcmMessage.sendMessage(args, gcm);
    }

    public void onLogin(Bundle data) {
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

    public void submitRegister(String firstName, String lastName, String email, String password) {
        if (!hasToken) {
            return;
        }

        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);

        GcmMessage gcmMessage = GcmMessage.getInstance(this);
        Bundle args = GCMHelper.getRegisterMessage(firstName, lastName, email, password);
        gcmMessage.sendMessage(args, gcm);
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