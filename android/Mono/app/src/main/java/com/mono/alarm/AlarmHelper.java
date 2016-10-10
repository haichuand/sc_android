package com.mono.alarm;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.os.Build;
import android.support.v7.app.NotificationCompat;

import com.mono.EventManager;
import com.mono.MainActivity;
import com.mono.R;
import com.mono.model.Event;
import com.mono.model.Reminder;
import com.mono.settings.Settings;
import com.mono.util.Colors;
import com.mono.util.Constants;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * This class is used to provide helper functions to create alarms for events to trigger
 * notifications. Notifications are created based off reminders set by the event. In addition,
 * tapping on a notification will bring up the event details view to display additional
 * information about the event.
 *
 * @author Gary Ng
 */
public class AlarmHelper {

    private static final long INTERVAL = 2 * Constants.WEEK_MS;

    private static final SimpleDateFormat TIME_FORMAT;

    private static final Map<String, List<PendingIntent>> ALARMS = new HashMap<>();

    static {
        TIME_FORMAT = new SimpleDateFormat("h:mm a", Locale.getDefault());
    }

    private AlarmHelper() {}

    /**
     * Create a notification alarm for a given event. Alarms will trigger notifications at the
     * designated time.
     *
     * @param context Context of the application.
     * @param eventId Event ID for notification to reference event.
     * @param alarmTime Time in milliseconds for the alarm to go off.
     * @param title Title displayed in the notification.
     * @param eventTime Actual time in milliseconds of the event.
     */
    public static void createAlarm(Context context, String eventId, long alarmTime,
            String title, long eventTime) {
        if (alarmTime < System.currentTimeMillis()) {
            return;
        }

        Notification notification = createNotification(context, eventId, title, eventTime);
        // Intent to Trigger Notification
        Intent intent = new Intent(context, AlarmNotificationReceiver.class);
        intent.setAction(AlarmNotificationReceiver.ACTION_NOTIFY);
        intent.putExtra(AlarmNotificationReceiver.EXTRA_NOTIFICATION, notification);

        PendingIntent alarmIntent =
            PendingIntent.getBroadcast(context, eventId.hashCode(), intent, 0);

        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        manager.setExact(AlarmManager.RTC_WAKEUP, alarmTime, alarmIntent);
        // Store Reference to Alarm
        List<PendingIntent> alarms = ALARMS.get(eventId);
        if (alarms == null) {
            alarms = new LinkedList<>();
            ALARMS.put(eventId, alarms);
        }
        alarms.add(alarmIntent);
    }

    /**
     * Remove all alarms belonging to a specific event.
     *
     * @param context Context of the application.
     * @param eventId Event ID for alarms to be removed.
     */
    public static void removeAlarms(Context context, String eventId) {
        if (!ALARMS.containsKey(eventId)) {
            return;
        }

        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        for (PendingIntent intent : ALARMS.remove(eventId)) {
            manager.cancel(intent);
        }
    }

    /**
     * Enable all existing alarms starting from now to a specific end time.
     *
     * @param context Context of the application.
     */
    public static void startAll(Context context) {
        clearAll(context);

        long startTime = System.currentTimeMillis();
        long endTime = startTime + INTERVAL;
        // Retrieve Reminders
        List<Event> events =
            EventManager.getInstance(context).getEventsWithReminders(startTime, endTime);
        // Create Alarms from Reminders
        for (Event event : events) {
            for (Reminder reminder : event.reminders) {
                long alarmTime = event.startTime - reminder.minutes * Constants.MINUTE_MS;
                createAlarm(context, event.id, alarmTime, event.title, event.startTime);
            }
        }
    }

    /**
     * Remove all existing alarms.
     *
     * @param context Context of the application.
     */
    public static void clearAll(Context context) {
        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        for (List<PendingIntent> values : ALARMS.values()) {
            for (PendingIntent intent : values) {
                manager.cancel(intent);
            }
        }

        ALARMS.clear();
    }

    /**
     * Create a notification object using the given details.
     *
     * @param context Context of the application.
     * @param eventId Event ID for notification to reference event.
     * @param title Title displayed in the notification.
     * @param eventTime The actual time in milliseconds of the event.
     * @return an instance of the notification.
     */
    public static Notification createNotification(Context context, String eventId, String title,
            long eventTime) {
        if (title == null || title.isEmpty()) {
            title = "(" + context.getString(R.string.no_subject) + ")";
        }
        // Intent to Show Event Details
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra(MainActivity.EXTRA_EVENT_ID, eventId);
        intent.putExtra(AlarmNotificationReceiver.EXTRA_NOTIFICATION_ID, eventId.hashCode());

        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT);
        // Create Notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
        builder.setSmallIcon(R.drawable.ic_calendar);
        builder.setContentTitle(title);

        builder.setAutoCancel(true);
        builder.setContentIntent(pendingIntent);
        builder.setPriority(Notification.PRIORITY_MAX);
        builder.setShowWhen(false);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder.setColor(Colors.getColor(context, R.color.colorPrimary));
        }
        // Notification Vibration
        if (Settings.getInstance(context).getNotificationVibrate()) {
            builder.setDefaults(Notification.DEFAULT_VIBRATE);
        }
        // Event Time
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(eventTime);
        builder.setContentText(TIME_FORMAT.format(calendar.getTime()));
        // Notification Sound
        Notification notification = builder.build();
        notification.sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        return notification;
    }
}
