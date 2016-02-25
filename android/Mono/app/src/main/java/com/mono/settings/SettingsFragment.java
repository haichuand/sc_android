package com.mono.settings;

import android.os.Bundle;
import android.preference.MultiSelectListPreference;
import android.preference.PreferenceFragment;

import com.mono.R;
import com.mono.calendar.CalendarHelper;
import com.mono.model.Calendar;

import java.util.List;

public class SettingsFragment extends PreferenceFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings);

        initCalendar();
    }

    public void initCalendar() {
        MultiSelectListPreference calendarPreference =
            (MultiSelectListPreference) findPreference(Settings.PREF_CALENDARS);

        List<Calendar> calendars = CalendarHelper.getInstance(getActivity()).getCalendars();
        String[] entries = new String[calendars.size()];
        String[] values = new String[calendars.size()];

        for (int i = 0; i < calendars.size(); i++) {
            Calendar calendar = calendars.get(i);

            entries[i] = calendar.name;
            values[i] = String.valueOf(calendar.id);
        }

        calendarPreference.setEntries(entries);
        calendarPreference.setEntryValues(values);
    }
}
