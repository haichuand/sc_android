package com.mono.intro;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;

import com.mono.PermissionManager;
import com.mono.R;
import com.mono.RequestCodes;

public class IntroFragment extends Fragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PermissionManager.checkPermissions(getActivity(),
            RequestCodes.Permission.PERMISSION_CHECK);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_intro, container, false);

        TextView start = (TextView) view.findViewById(R.id.start);
        start.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                onStartClick();
            }
        });

        return view;
    }

    public void onStartClick() {
        getActivity().setResult(Activity.RESULT_OK);
        getActivity().finish();
    }
}
