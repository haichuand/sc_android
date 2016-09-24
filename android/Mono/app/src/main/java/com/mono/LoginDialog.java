package com.mono;

import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.CookieManager;
import android.widget.Button;
import android.widget.EditText;

import com.mono.util.Colors;
import com.mono.util.Common;

/**
 * A dialog used to handle the remaining login process when used with Google Timeline login.
 *
 * @author Gary Ng
 */
public class LoginDialog {

    private static final int INDEX_PASSWORD = 0;

    private LoginActivity activity;
    private AlertDialog dialog;

    private EditText[] fields;
    private Button submit;

    private String email;

    private LoginDialog(LoginActivity activity, String email) {
        this.activity = activity;
        this.email = email;
    }

    /**
     * Create and display an instance of this dialog.
     *
     * @param activity Activity containing methods to handle login.
     * @param email Email to be used to log in.
     * @return an instance of a dialog.
     */
    public static LoginDialog create(LoginActivity activity, String email) {
        LoginDialog dialog = new LoginDialog(activity, email);

        AlertDialog.Builder builder =
                new AlertDialog.Builder(activity, R.style.AppTheme_Dialog_Alert);
        builder.setView(dialog.onCreateView());
        builder.setCancelable(false);

        dialog.dialog = builder.create();
        dialog.dialog.show();

        return dialog;
    }

    /**
     * Create the view of this dialog.
     *
     * @return an instance of the view.
     */
    protected View onCreateView() {
        LayoutInflater inflater = LayoutInflater.from(activity);
        View view = inflater.inflate(R.layout.dialog_login, null, false);

        fields = new EditText[1];
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
                submit.setTextColor(Colors.getColor(activity, colorId));
            }
        };

        fields[INDEX_PASSWORD].addTextChangedListener(textWatcher);

        submit = (Button) view.findViewById(R.id.submit);
        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onSubmit();
            }
        });

        Button cancel = (Button) view.findViewById(R.id.cancel);
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onCancel();
            }
        });

        textWatcher.afterTextChanged(null);

        return view;
    }

    /**
     * Check if all required fields are filled.
     *
     * @return the status of the check.
     */
    private boolean verify() {
        return !fields[INDEX_PASSWORD].getText().toString().trim().isEmpty();
    }

    /**
     * Handle the action of clicking on submit.
     */
    private void onSubmit() {
        if (!verify()) {
            return;
        }

        String password = fields[INDEX_PASSWORD].getText().toString().trim();
        if (password.isEmpty()) {
            return;
        }

        password = Common.md5(password);
        activity.submitLogin(email, password, false);
    }

    /**
     * Handle the action of clicking on cancel.
     */
    private void onCancel() {
        // Remove Google Timeline Cookie
        CookieManager.getInstance().removeAllCookies(null);
        dialog.dismiss();
    }
}
