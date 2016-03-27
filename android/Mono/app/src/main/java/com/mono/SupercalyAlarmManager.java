package com.mono;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.util.Log;

/**
 * Created by xuejing on 3/26/16.
 */
public class SupercalyAlarmManager {
    private static SupercalyAlarmManager instance;
    private Context context;
    private String TAG = "SupercalyAlarmManager";

    private SupercalyAlarmManager (Context context) {
        this.context = context;
    }

    public static SupercalyAlarmManager getInstance(Context context) {
        if (instance == null) {
            instance = new SupercalyAlarmManager(context);
        }

        return instance;
    }

    public void scheduleAlarm() {
        Log.d(TAG, "Scheduling an alarm");
        // Construct an intent that will execute the AlarmReceiver
        Intent intent = new Intent(context, SuperCalyAlarmReceiver.class);
        intent.setAction(SuperCalyAlarmReceiver.ACTION_ALARM_RECEIVER);
        // Create a PendingIntent to be triggered when the alarm goes off
        final PendingIntent pIntent = PendingIntent.getBroadcast(context, RequestCodes.Activity.ALARM_RECEIVER,
                intent, PendingIntent.FLAG_UPDATE_CURRENT);

        long uptimeMillis =  SystemClock.elapsedRealtime();;
        AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarm.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, uptimeMillis,
                240000, pIntent);
    }

    public void cancelAlarm() {
        Intent intent = new Intent(context, SuperCalyAlarmReceiver.class);
        final PendingIntent pIntent = PendingIntent.getBroadcast(context, RequestCodes.Activity.ALARM_RECEIVER,
                intent, PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarm.cancel(pIntent);
    }
}
