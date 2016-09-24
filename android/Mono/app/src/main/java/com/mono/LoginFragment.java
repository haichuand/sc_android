package com.mono;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.telephony.TelephonyManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;

import com.mono.model.Account;
import com.mono.network.HttpServerManager;
import com.mono.settings.Settings;
import com.mono.util.Colors;
import com.mono.util.Common;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * A fragment that displays the login screen to input user credentials.
 *
 * @author Gary Ng
 */
public class LoginFragment extends Fragment {

    public static final int REQUEST_GOOGLE_LOGIN = 1;

    private static final int INDEX_USERNAME = 0;
    private static final int INDEX_PASSWORD = 1;

    private LoginActivity activity;

    private EditText[] fields;
    private CheckBox remember;
    private Button submit;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (context instanceof LoginActivity) {
            activity = (LoginActivity) context;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_login, container, false);

        fields = new EditText[2];
        fields[INDEX_USERNAME] = (EditText) view.findViewById(R.id.username);
        fields[INDEX_PASSWORD] = (EditText) view.findViewById(R.id.password);

        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                boolean isEnabled = verify();
                submit.setEnabled(isEnabled);

                int colorId = isEnabled ? android.R.color.white : R.color.translucent_50;
                submit.setTextColor(Colors.getColor(getContext(), colorId));
            }
        };

        for (EditText editText : fields) {
            editText.addTextChangedListener(textWatcher);
        }

        remember = (CheckBox) view.findViewById(R.id.remember);
        remember.setChecked(Settings.getInstance(getContext()).getRememberMe());
        remember.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (!isChecked) {
                    Settings.getInstance(getContext()).setRememberMe(false);
                }
            }
        });

        submit = (Button) view.findViewById(R.id.submit);
        submit.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                onSubmit();
            }
        });

        Button google = (Button) view.findViewById(R.id.google);
        google.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                onGoogleLogin();
            }
        });

        textWatcher.afterTextChanged(null);

        TextView register = (TextView) view.findViewById(R.id.register);
        register.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                onRegister();
            }
        });

        initialize();

        return view;
    }

    /**
     * Initialize the username with a default value such as email or phone. If no account is
     * present, it will try to retrieve the phone number used by the device as the default.
     */
    public void initialize() {
        boolean remember = Settings.getInstance(getContext()).getRememberMe();
        Account account = AccountManager.getInstance(getContext()).getAccount();

        if (remember && account != null) {
            String value = account.email != null ? account.email : account.phone;
            fields[INDEX_USERNAME].setText(value);
        } else {
            TelephonyManager manager =
                (TelephonyManager) getActivity().getSystemService(Context.TELEPHONY_SERVICE);

            String phoneNumber = manager.getLine1Number();
            if (phoneNumber != null) {
                fields[INDEX_USERNAME].setText(phoneNumber);
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_GOOGLE_LOGIN:
                handleGoogleLogin(resultCode, data);
                break;
        }
    }

    /**
     * Check if all required fields are filled.
     *
     * @return the status of the check.
     */
    public boolean verify() {
        boolean result = true;

        for (EditText editText : fields) {
            if (editText.getText().toString().trim().isEmpty()) {
                result = false;
                break;
            }
        }

        return result;
    }

    /**
     * Handle the action of clicking on the submit button. Passwords will be hashed before being
     * sent to the server.
     */
    public void onSubmit() {
        if (!verify()) {
            return;
        }

        String username = fields[INDEX_USERNAME].getText().toString().trim();
        String password = fields[INDEX_PASSWORD].getText().toString().trim();

        password = Common.md5(password);
        activity.submitLogin(username, password, remember.isChecked());
    }

    /**
     * Handle the action of clicking on the Google login button.
     */
    public void onGoogleLogin() {
        Intent intent = new Intent(getContext(), GoogleLoginActivity.class);
        startActivityForResult(intent, REQUEST_GOOGLE_LOGIN);
    }

    /**
     * Handle the results returned from logging in with Google.
     *
     * @param resultCode Result code.
     * @param data Data returned.
     */
    public void handleGoogleLogin(int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            String email = data.getStringExtra(GoogleLoginActivity.EXTRA_EMAIL);
            String name = data.getStringExtra(GoogleLoginActivity.EXTRA_NAME);

            String firstName = null, lastName = null;
            if (name != null) {
                int index = name.indexOf(" ");

                if (index > 0) {
                    firstName = name.substring(0, index);
                    lastName = name.substring(index + 1);
                } else {
                    firstName = name;
                }
            }

            HttpServerManager manager = HttpServerManager.getInstance(getContext());
            JSONObject json = manager.getUserByEmail(email);

            try {
                if (json.has(HttpServerManager.STATUS) &&
                        json.getInt(HttpServerManager.STATUS) == HttpServerManager.STATUS_NO_USER) {
                    CookieManager.getInstance().removeAllCookies(null);
                    RegisterDialog.create(activity, email, firstName, lastName);
                } else {
                    LoginDialog.create(activity, email);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Handle the action of clicking on the register button. Upon doing so, it will switch to the
     * register screen.
     */
    public void onRegister() {
        activity.showRegister();
    }
}
