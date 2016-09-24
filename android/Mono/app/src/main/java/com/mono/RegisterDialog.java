package com.mono;

import android.graphics.PorterDuff;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.mono.util.Colors;
import com.mono.util.Common;

/**
 * A dialog used to handle the remaining registration process when used with Google Timeline login.
 *
 * @author Gary Ng
 */
public class RegisterDialog {

    private static final int INDEX_FIRST_NAME = 0;
    private static final int INDEX_LAST_NAME = 1;
    private static final int INDEX_PASSWORD = 2;
    private static final int INDEX_CONFIRM_PASSWORD = 3;

    private LoginActivity activity;
    private AlertDialog dialog;

    private EditText[] fields;
    private Button submit;

    private String email;

    private RegisterDialog(LoginActivity activity, String email) {
        this.activity = activity;
        this.email = email;
    }

    /**
     * Create and display an instance of this dialog.
     *
     * @param activity Activity containing methods to handle login.
     * @param email Email to be used to log in.
     * @param firstName First name for this account.
     * @param lastName Last name for this account.
     * @return an instance of a dialog.
     */
    public static RegisterDialog create(LoginActivity activity, String email, String firstName,
            String lastName) {
        RegisterDialog dialog = new RegisterDialog(activity, email);

        AlertDialog.Builder builder =
                new AlertDialog.Builder(activity, R.style.AppTheme_Dialog_Alert);
        builder.setView(dialog.onCreateView(firstName, lastName));
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
    protected View onCreateView(String firstName, String lastName) {
        LayoutInflater inflater = LayoutInflater.from(activity);
        View view = inflater.inflate(R.layout.dialog_register, null, false);

        fields = new EditText[4];

        fields[INDEX_FIRST_NAME] = (EditText) view.findViewById(R.id.first_name);
        fields[INDEX_FIRST_NAME].setText(firstName);

        fields[INDEX_LAST_NAME] = (EditText) view.findViewById(R.id.last_name);
        fields[INDEX_LAST_NAME].setText(lastName);

        fields[INDEX_PASSWORD] = (EditText) view.findViewById(R.id.password);
        fields[INDEX_CONFIRM_PASSWORD] = (EditText) view.findViewById(R.id.confirm_password);

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

        int[] indexes = {INDEX_FIRST_NAME, INDEX_PASSWORD, INDEX_CONFIRM_PASSWORD};

        for (int index : indexes) {
            fields[index].addTextChangedListener(textWatcher);
        }

        submit = (Button) view.findViewById(R.id.submit);
        submit.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                onSubmit();
            }
        });

        TextView cancel = (TextView) view.findViewById(R.id.cancel);
        cancel.setOnClickListener(new OnClickListener() {
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
        boolean result = true;

        int[] indexes = {INDEX_FIRST_NAME, INDEX_PASSWORD, INDEX_CONFIRM_PASSWORD};

        for (int index : indexes) {
            if (fields[index].getText().toString().trim().isEmpty()) {
                result = false;
                break;
            }
        }

        return result;
    }

    /**
     * Handle the action of clicking on submit.
     */
    private void onSubmit() {
        if (!verify()) {
            return;
        }

        String firstName = fields[INDEX_FIRST_NAME].getText().toString().trim();
        String lastName = fields[INDEX_LAST_NAME].getText().toString().trim();
        String password = fields[INDEX_PASSWORD].getText().toString().trim();
        String confirmPassword = fields[INDEX_CONFIRM_PASSWORD].getText().toString().trim();
        String userName = (firstName + lastName).toLowerCase();

        if (!password.equals(confirmPassword)) {
            int color = Colors.getColor(activity, R.color.red_1);

            fields[INDEX_PASSWORD].getBackground().setColorFilter(color,
                PorterDuff.Mode.SRC_ATOP);
            fields[INDEX_CONFIRM_PASSWORD].getBackground().setColorFilter(color,
                PorterDuff.Mode.SRC_ATOP);
            return;
        }

        password = Common.md5(password);
        activity.submitRegister(email, null, firstName, lastName, userName, password);
    }

    /**
     * Handle the action of clicking on cancel.
     */
    private void onCancel() {
        dialog.dismiss();
    }
}
