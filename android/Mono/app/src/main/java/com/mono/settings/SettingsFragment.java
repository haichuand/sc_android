package com.mono.settings;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;
import android.widget.Toast;

import com.mono.AccountManager;
import com.mono.BuildConfig;
import com.mono.R;
import com.mono.model.Account;
import com.mono.model.Calendar;
import com.mono.network.HttpServerManager;
import com.mono.provider.CalendarProvider;
import com.mono.util.Common;

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
        setCalendarDefault();
        setCalendars();
        setCalendarWeekStart();
        setCalendarWeekNumber();
        setCrashReport();
        setVersion();
        setPassword();
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
     * Initialize the current default calendar.
     */
    public void setCalendarDefault() {
        String key = getString(R.string.pref_calendar_default_key);
        ListPreference preference = (ListPreference) findPreference(key);

        Context context = getActivity();

        List<Calendar> calendars = CalendarProvider.getInstance(context).getCalendars();
        calendars.add(0, new Calendar(0, getString(R.string.local_calendar)));

        long defaultId = Settings.getInstance(context).getCalendarDefault();

        Calendar primaryCalendar = null, secondaryCalendar = null;
        int primaryIndex = -1, secondaryIndex = -1;

        String[] entries = new String[calendars.size()];
        String[] values = new String[calendars.size()];

        for (int i = 0; i < calendars.size(); i++) {
            Calendar calendar = calendars.get(i);

            if (primaryIndex == -1 && calendar.id == defaultId) {
                primaryCalendar = calendar;
                primaryIndex = i;
            }

            if (secondaryIndex == -1 && calendar.primary && !calendar.local) {
                secondaryCalendar = calendar;
                secondaryIndex = i;
            }

            entries[i] = calendar.name;
            if (!Common.isEmpty(calendar.accountName)) {
                entries[i] += "\n(" + calendar.accountName + ")";
            }
            values[i] = String.valueOf(calendar.id);
        }

        preference.setEntries(entries);
        preference.setEntryValues(values);

        String summary;

        if (primaryIndex >= 0) {
            summary = primaryCalendar.name;
            if (!Common.isEmpty(primaryCalendar.accountName)) {
                summary += "\n(" + primaryCalendar.accountName + ")";
            }

            preference.setSummary(summary);
            preference.setValueIndex(primaryIndex);
        } else if (secondaryIndex >= 0) {
            Settings.getInstance(context).setCalendarDefault(secondaryCalendar.id);

            summary = secondaryCalendar.name;
            if (!Common.isEmpty(secondaryCalendar.accountName)) {
                summary += "\n(" + secondaryCalendar.accountName + ")";
            }
            preference.setSummary(summary);
            preference.setValueIndex(secondaryIndex);
        }

        preference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object value) {
                CalendarProvider provider = CalendarProvider.getInstance(preference.getContext());

                Calendar calendar = null;
                long calendarId = Long.parseLong(value.toString());

                if (calendarId > 0) {
                    calendar = provider.getCalendar(calendarId);
                }

                if (calendar == null) {
                    calendar = new Calendar(0, getString(R.string.local_calendar));
                }

                String summary = calendar.name;
                if (!Common.isEmpty(calendar.accountName)) {
                    summary += "\n(" + calendar.accountName + ")";
                }
                preference.setSummary(summary);

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

    /**
     * Initialize Change password feature
     */
    public void setPassword()
    {
        String key = "change_password_key";
        SharedPreferences sp = getPreferenceScreen().getSharedPreferences();
        EditTextPreference password = (EditTextPreference) findPreference(key);
        password.setSummary(sp.getString("change_password_key", "").replaceAll(".","*"));

        password.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object value) {
                Account account = AccountManager.getInstance(getActivity().getApplicationContext()).getAccount();
                if(account != null) {
                    long check = account.id;
                    HttpServerManager httpServerManager = HttpServerManager.getInstance(getActivity().getApplicationContext());
                    httpServerManager.editUser((int)account.id,account.username, account.email,account.firstName,AccountManager.getInstance(getActivity().getApplicationContext()).getFcmToken(),account.lastName,account.mediaId, account.phone, Common.md5(value.toString()));
                    preference.setSummary(value.toString().replaceAll(".","*"));
                }
                else
                {
                    Toast.makeText(getActivity().getApplicationContext(), "You need to login in order to change your Password.", Toast.LENGTH_SHORT).show();
                    preference.setSummary("");
                }

                getActivity().setResult(Activity.RESULT_OK);
                return true;
            }
        });
    }
}
