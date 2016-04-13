package com.mono;

import android.content.Context;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.mono.util.Colors;
import com.mono.util.Common;

public class RegisterFragment extends Fragment {

    private static final int INDEX_FIRST_NAME = 0;
    private static final int INDEX_LAST_NAME = 1;
    private static final int INDEX_EMAIL = 2;
    private static final int INDEX_PASSWORD = 3;
    private static final int INDEX_CONFIRM_PASSWORD = 4;

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
        View view = inflater.inflate(R.layout.fragment_register, container, false);

        fields = new EditText[5];
        fields[INDEX_FIRST_NAME] = (EditText) view.findViewById(R.id.first_name);
        fields[INDEX_LAST_NAME] = (EditText) view.findViewById(R.id.last_name);
        fields[INDEX_EMAIL] = (EditText) view.findViewById(R.id.email);
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
                boolean isEnabled = true;
                for (EditText editText : fields) {
                    if (editText.getText().toString().trim().isEmpty()) {
                        isEnabled = false;
                        break;
                    }
                }
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

        TextView cancel = (TextView) view.findViewById(R.id.cancel);
        cancel.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                onCancel();
            }
        });

        return view;
    }

    public void onSubmit() {
        for (EditText editText : fields) {
            if (editText.getText().toString().trim().isEmpty()) {
                return;
            }
        }

        String firstName = fields[INDEX_FIRST_NAME].getText().toString().trim();
        String lastName = fields[INDEX_LAST_NAME].getText().toString().trim();
        String email = fields[INDEX_EMAIL].getText().toString().trim();
        String password = fields[INDEX_PASSWORD].getText().toString().trim();
        String confirmPassword = fields[INDEX_CONFIRM_PASSWORD].getText().toString().trim();

        if (!password.equals(confirmPassword)) {
            int color = Colors.getColor(getContext(), R.color.red_1);

            fields[INDEX_PASSWORD].getBackground().setColorFilter(color,
                PorterDuff.Mode.SRC_ATOP);
            fields[INDEX_CONFIRM_PASSWORD].getBackground().setColorFilter(color,
                PorterDuff.Mode.SRC_ATOP);
            return;
        }

        password = Common.md5(password);
        activity.submitRegister(firstName, lastName, email, password);
    }

    public void onCancel() {
        activity.onBackPressed();
    }
}
