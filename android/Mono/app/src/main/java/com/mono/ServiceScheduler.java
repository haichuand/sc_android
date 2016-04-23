package com.mono;

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

import com.mono.calendar.CalendarHelper;
import com.mono.model.Calendar;
import com.mono.settings.Settings;

import java.util.ArrayList;
import java.util.List;

public class ServiceScheduler extends BroadcastReceiver {

    public static final int REQUEST_CODE = 1;

    private static final int SYNC_DELAY = 10000;

    private AlarmManager manager;
    private long lastSyncRequest;

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        switch (action) {
            case Intent.ACTION_BOOT_COMPLETED:
            case Intent.ACTION_PROVIDER_CHANGED:
                run(context);
                break;
        }
    }

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

    public void requestSync(Context context, boolean force) {
        long currentTime = System.currentTimeMillis();

        if (!force && currentTime - lastSyncRequest < SYNC_DELAY) {
            return;
        }

        lastSyncRequest = currentTime;

        List<Calendar> calendars = CalendarHelper.getInstance(context).getCalendars();
        if (calendars.isEmpty()) {
            return;
        }

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

        @Override
        public int onStartCommand(Intent intent, int flags, int startId) {
            if (calendarTask == null || calendarTask.getStatus() != AsyncTask.Status.RUNNING) {
                calendarTask = new CalendarTask(this);
                calendarTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }

            return START_NOT_STICKY;
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
    }
}
