package com.mono;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.telephony.TelephonyManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.mono.model.Account;
import com.mono.util.Colors;
import com.mono.util.Common;

/**
 * A fragment that displays the login screen to input user credentials.
 *
 * @author Gary Ng
 */
public class LoginFragment extends Fragment {

    private static final int INDEX_USERNAME = 0;
    private static final int INDEX_PASSWORD = 1;

    private LoginActivity activity;

    private EditText[] fields;
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

        submit = (Button) view.findViewById(R.id.submit);
        submit.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                onSubmit();
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
        Account account = AccountManager.getInstance(getContext()).getAccount();

        if (account != null) {
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

    /**
     * Check if the input fields are not empty.
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
        activity.submitLogin(username, password);
    }

    /**
     * Handle the action of clicking on the register button. Upon doing so, it will switch to the
     * register screen.
     */
    public void onRegister() {
        activity.showRegister();
    }
}
