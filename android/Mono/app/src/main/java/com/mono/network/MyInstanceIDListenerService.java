package com.mono.network;

import android.util.Log;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;
import com.mono.AccountManager;
import com.mono.model.Account;
import com.mono.settings.Settings;

/**
 * This service is used to retrieve the FCM registration token. This token is then registered with
 * the HTTP and XMPP servers to enable communication from device to server and vice versa.
 *
 * @author Xuejing Dong, Haichuan Duan, Gary Ng
 */
public class MyInstanceIDListenerService extends FirebaseInstanceIdService {

    private static final String TAG = "MyInstanceIDLS";

    /**
     * Called if InstanceID token is updated. This may occur if the security of
     * the previous token had been compromised. Note that this is also called
     * when the InstanceID token is initially generated, so this is where
     * you retrieve the token.
     */
    @Override
    public void onTokenRefresh() {
        String refreshedToken = FirebaseInstanceId.getInstance().getToken();
        Log.d(TAG, "Refreshed Token: " + refreshedToken);
        sendRegistrationToServer(refreshedToken);
    }

    private void sendRegistrationToServer(String token) {
        Settings.getInstance(this).setFcmTokenSent(token != null);
        if (token == null) {
            return;
        }

        AccountManager.getInstance(getApplicationContext()).setFcmToken(token);
        Account account = AccountManager.getInstance(this).getAccount();
        if (account != null) {
            int accountId = (int) (account.id);
            HttpServerManager.getInstance(this).updateUserFcmId(accountId, token);
            ChatServerManager.getInstance(this).updateUserFcmId(accountId, token);
        }
    }
}
