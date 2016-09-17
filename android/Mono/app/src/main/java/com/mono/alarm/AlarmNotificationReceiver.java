package com.mono.alarm;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.mono.settings.Settings;

/**
 * This receiver is used to enable all existing alarms after device boot as well as trigger
 * notifications set by previous existing alarms.
 *
 * @author Gary Ng
 */
public class AlarmNotificationReceiver extends BroadcastReceiver {

    public static final String ACTION_BOOT = "android.intent.action.BOOT_COMPLETED";
    public static final String ACTION_NOTIFY = "notify";

    public static final String EXTRA_NOTIFICATION = "notification";
    public static final String EXTRA_NOTIFICATION_ID = "notification_id";

    @Override
    public void onReceive(Context context, Intent intent) {
        switch (intent.getAction()) {
            case ACTION_BOOT:
                AlarmHelper.startAll(context);
                break;
            case ACTION_NOTIFY:
                if (!Settings.getInstance(context).getNotification()) {
                    return;
                }

                Notification notification = intent.getParcelableExtra(EXTRA_NOTIFICATION);
                int id = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0);

                NotificationManager manager =
                    (NotificationManager) context.getSystemService(Activity.NOTIFICATION_SERVICE);
                manager.notify(id, notification);
                break;
        }
    }
}
