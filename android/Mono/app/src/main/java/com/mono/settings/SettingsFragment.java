package com.mono.settings;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;

import com.mono.BuildConfig;
import com.mono.R;
import com.mono.model.Calendar;
import com.mono.provider.CalendarProvider;

import org.acra.ACRA;

import java.util.List;
import java.util.Set;

/**
 * A fragment that handles additional initialization for settings such as populating it with
 * calendars found on the device and adding callbacks to specific settings to respond to changes.
 *
 * @author Gary Ng
 */
public class SettingsFragment extends PreferenceFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings);

        setLocationService();
        setCalendars();
        setCalendarWeekStart();
        setCalendarWeekNumber();
        setCrashReport();
        setVersion();
    }

    /**
     * Initialize the current Location Service status.
     */
    public void setLocationService() {
        String key = getString(R.string.pref_location_service_key);
        SwitchPreference preference = (SwitchPreference) findPreference(key);

        preference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object value) {
                getActivity().setResult(Activity.RESULT_OK);
                return true;
            }
        });
    }

    /**
     * Populate settings with calendars found on device.
     */
    public void setCalendars() {
        String key = getString(R.string.settings_cat_calendars_key);
        PreferenceCategory category = (PreferenceCategory) findPreference(key);

        Context context = getActivity();

        List<Calendar> calendars = CalendarProvider.getInstance(context).getCalendars();
        Set<Long> calendarIds = Settings.getInstance(context).getCalendars();

        for (Calendar calendar : calendars) {
            final long id = calendar.id;

            CheckBoxPreference preference = new CheckBoxPreference(context);
            preference.setTitle(calendar.name);
            preference.setSummary(calendar.accountName);
            preference.setChecked(calendarIds.contains(id));

            preference.setOnPreferenceChangeListener(
                new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object value) {
                        Settings settings = Settings.getInstance(preference.getContext());

                        if (Boolean.parseBoolean(value.toString())) {
                            settings.setCalendarId(id);
                        } else {
                            settings.removeCalendarId(id);
                        }

                        getActivity().setResult(Activity.RESULT_OK);
                        return true;
                    }
                }
            );

            category.addPreference(preference);
        }
    }

    /**
     * Modifies the first day of the week.
     */
    public void setCalendarWeekStart() {
        String key = getString(R.string.pref_week_start_key);
        ListPreference preference = (ListPreference) findPreference(key);

        String[] entries =
            getResources().getStringArray(R.array.pref_week_start_entries);

        String summary = entries[Integer.parseInt(preference.getValue())];
        preference.setSummary(summary);

        preference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object value) {
                String[] entries =
                    getResources().getStringArray(R.array.pref_week_start_entries);

                String summary = entries[Integer.parseInt(value.toString())];
                preference.setSummary(summary);

                getActivity().setResult(Activity.RESULT_OK);
                return true;
            }
        });
    }

    /**
     * Add callback to detect changes to enabling week numbers.
     */
    public void setCalendarWeekNumber() {
        String key = getString(R.string.pref_week_num_key);
        SwitchPreference preference = (SwitchPreference) findPreference(key);

        preference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object value) {
                getActivity().setResult(Activity.RESULT_OK);
                return true;
            }
        });
    }

    /**
     * Added callback to detect changes to enabling crash reporting.
     */
    public void setCrashReport() {
        String key = getString(R.string.pref_crash_report_key);
        SwitchPreference preference = (SwitchPreference) findPreference(key);

        preference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object value) {
                ACRA.getErrorReporter().setEnabled((boolean) value);
                getActivity().setResult(Activity.RESULT_OK);
                return true;
            }
        });
    }

    /**
     * Updates the version number displayed in settings.
     */
    public void setVersion() {
        String key = getString(R.string.pref_version_key);
        Preference preference = findPreference(key);

        String summary = BuildConfig.VERSION_NAME;
        preference.setSummary(summary);
    }
}
