package com.mono;

import android.Manifest;
import android.accounts.Account;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.CalendarContract;

import com.mono.contacts.SuggestionsTask;
import com.mono.model.Calendar;
import com.mono.provider.CalendarProvider;
import com.mono.provider.MainContentObserver;
import com.mono.settings.Settings;
import com.mono.util.Constants;

import java.util.ArrayList;
import java.util.List;

/**
 * This service is used to handle any background scheduling even when the app is not opened. It
 * is also responsible to observe any changes that occurred in the content providers through the
 * use of a content observer.
 *
 * @author Gary Ng
 */
public class ServiceScheduler extends BroadcastReceiver {

    public static final int REQUEST_CODE = 1;

    private static final int SYNC_DELAY = 10000;
    private static final long SUGGESTIONS_DELAY = Constants.DAY_MS;

    private AlarmManager manager;
    private long lastSyncRequest;

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        switch (action) {
            case Intent.ACTION_BOOT_COMPLETED:
                run(context);
                break;
        }
    }

    /**
     * Set up an alarm to trigger the service to run.
     *
     * @param context The value of the context.
     */
    public void run(Context context) {
        if (manager == null) {
            manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        }

        Intent intent = new Intent(context, MainService.class);
        PendingIntent alarmIntent = PendingIntent.getService(context, REQUEST_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT);

        try {
            alarmIntent.send();
        } catch (PendingIntent.CanceledException e) {
            e.printStackTrace();
        }

        long interval = Settings.getInstance(context).getSchedulerInterval();
        manager.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() +
            interval, interval, alarmIntent);
    }

    /**
     * Trigger the calendar provider to sync for latest calendar events.
     *
     * @param context The value of the context.
     * @param force The value used to bypass the sync delay.
     */
    public void requestSync(Context context, boolean force) {
        long currentTime = System.currentTimeMillis();
        // Prevent Repeating Syncing
        if (!force && currentTime - lastSyncRequest < SYNC_DELAY) {
            return;
        }

        lastSyncRequest = currentTime;

        List<Calendar> calendars = CalendarProvider.getInstance(context).getCalendars();
        if (calendars.isEmpty()) {
            return;
        }
        // Sync
        List<Account> accounts = new ArrayList<>();

        Bundle bundle = new Bundle();
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);

        for (Calendar calendar : calendars) {
            Account account = new Account(calendar.accountName, calendar.accountType);

            if (!accounts.contains(account)) {
                ContentResolver.requestSync(account, CalendarContract.AUTHORITY, bundle);
                accounts.add(account);
            }
        }
    }

    public static class MainService extends Service {

        private IBinder binder = new LocalBinder();
        private CalendarTask calendarTask;
        private SuggestionsTask suggestionsTask;
        private MainContentObserver contentObserver;

        @Override
        public int onStartCommand(Intent intent, int flags, int startId) {
            // Register Content Observer
            if (contentObserver == null) {
                contentObserver = new MainContentObserver(getApplicationContext(), null);
                contentObserver.register();
            }
            // Calendar Task
            if (calendarTask == null || calendarTask.getStatus() != AsyncTask.Status.RUNNING) {
                calendarTask = new CalendarTask(this);
                calendarTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
            // Suggestions Task
            requestSuggestions(false);

            return START_NOT_STICKY;
        }

        @Override
        public void onDestroy() {
            // Remove Content Observer
            if (contentObserver != null) {
                contentObserver.unregister();
                contentObserver = null;
            }
        }

        @Override
        public IBinder onBind(Intent intent) {
            return binder;
        }

        public class LocalBinder extends Binder {

            public MainService getService() {
                return MainService.this;
            }
        }

        public void requestSuggestions(boolean force) {
            if (!PermissionManager.checkPermission(this, Manifest.permission.READ_CONTACTS)) {
                return;
            }

            Settings settings = Settings.getInstance(this);

            long currentTime = System.currentTimeMillis();
            long milliseconds = settings.getContactsScan();

            if (!force && currentTime - milliseconds < SUGGESTIONS_DELAY) {
                return;
            }

            if (suggestionsTask == null || suggestionsTask.getStatus() != AsyncTask.Status.RUNNING) {
                long startId = settings.getContactsScanId();

                suggestionsTask = new SuggestionsTask(this, startId);
                suggestionsTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
        }
    }
}
