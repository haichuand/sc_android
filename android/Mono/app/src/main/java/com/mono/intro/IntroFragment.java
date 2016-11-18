package com.mono.intro;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.mono.PermissionManager;
import com.mono.R;
import com.mono.RequestCodes;
import com.mono.model.Calendar;
import com.mono.provider.CalendarProvider;
import com.mono.settings.Settings;
import com.mono.settings.SettingsActivity;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A fragment that displays a splash intro to ask user to set permissions as well as allowing
 * users to change settings.
 *
 * @author Gary Ng
 */
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

        Button settings = (Button) view.findViewById(R.id.settings);
        settings.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                onSettingsClick();
            }
        });

        TextView start = (TextView) view.findViewById(R.id.start);
        start.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                onStartClick();
            }
        });

        return view;
    }

    /**
     * Handle the action of clicking on the settings button.
     */
    public void onSettingsClick() {
        handleSettings();

        Intent intent = new Intent(getContext(), SettingsActivity.class);
        startActivityForResult(intent, RequestCodes.Activity.SETTINGS);
    }

    /**
     * Handle the action of clicking on the start button to return from this activity.
     */
    public void onStartClick() {
        handleSettings();

        getActivity().setResult(Activity.RESULT_OK);
        getActivity().finish();
    }

    /**
     * Perform essential functions to initialize the application such as calendars.
     */
    public void handleSettings() {
        Settings settings = Settings.getInstance(getContext());
        CalendarProvider provider = CalendarProvider.getInstance(getContext());

        try {
            // Initialize Calendars
            Set<Long> calendars = new HashSet<>();
            List<Calendar> calendarList = provider.getCalendars();
            // Set Default Calendar
            if (settings.getCalendarDefault() == 0) {
                for (Calendar calendar : calendarList) {
                    if (calendar.primary && !calendar.local) {
                        settings.setCalendarDefault(calendar.id);
                        break;
                    }
                }
            }
            // Save Calendar IDs
            for (Calendar calendar : calendarList) {
                calendars.add(calendar.id);
            }
            settings.setCalendars(calendars);
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }
}
