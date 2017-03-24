package com.mono.util;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;

import com.mono.LoginActivity;
import com.mono.R;
import com.mono.settings.Settings;


/**
 * Created by anu on 3/24/2017.
 */

public class ForgotPasswordFragment extends Fragment {

    private LoginActivity activity;
    private EditText[] fields;
    private static final int INDEX_USERNAME = 0;
    private static final int INDEX_PASSWORD = 1;
    private TextView reset;

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
        View view = inflater.inflate(R.layout.fragment_forgotpassword, container, false);

        fields = new EditText[2];
        fields[INDEX_USERNAME] = (EditText) view.findViewById(R.id.username);
        fields[INDEX_PASSWORD] = (EditText) view.findViewById(R.id.email);


        reset = (TextView) view.findViewById(R.id.reset);
        reset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
               Onreset();
            }
        });

        return view;
    }
    /**
     * Handle the action of clicking on the reset button.
     */
    public void Onreset()
    {

        if (!verify()) {
            return;
        }

        String username = fields[INDEX_USERNAME].getText().toString().trim();
        String email = fields[INDEX_PASSWORD].getText().toString().trim();
        activity.resetPassword(username, email);

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

}
