package com.mono;

import android.content.Context;
import android.os.AsyncTask;

import com.mono.EventManager.EventAction;
import com.mono.db.DatabaseHelper;
import com.mono.db.dao.AttendeeDataSource;
import com.mono.db.dao.EventAttendeeDataSource;
import com.mono.db.dao.EventDataSource;
import com.mono.model.Attendee;
import com.mono.model.Calendar;
import com.mono.model.Event;
import com.mono.provider.CalendarEventProvider;
import com.mono.provider.CalendarProvider;
import com.mono.settings.Settings;
import com.mono.util.Constants;
import com.mono.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * This class is used to import calendar events from the Calendar Provider to the local database.
 *
 * @author Gary Ng
 */
public class CalendarTask extends AsyncTask<Object, Event, Object> {

    private Context context;

    private CalendarEventProvider provider;
    private Settings settings;

    public CalendarTask(Context context) {
        this.context = context;

        provider = CalendarEventProvider.getInstance(context);
        settings = Settings.getInstance(context);
    }

    @Override
    protected Object doInBackground(Object... params) {
        Log.getInstance(context).debug(getClass().getSimpleName(), "Running");

        Set<Long> calendarIds = settings.getCalendars();
        if (calendarIds.isEmpty()) {
            return null;
        }

        boolean isRunning = false;
        long currentTime = System.currentTimeMillis();

        CalendarProvider calendarProvider = CalendarProvider.getInstance(context);

        for (long calendarId : calendarIds) {
            Calendar calendar = calendarProvider.getCalendar(calendarId);
            if (calendar == null || calendar.local) {
                continue;
            }

            long initialTime = settings.getCalendarInitialTime(calendarId, 0);

            if (initialTime == 0) {
                settings.setCalendarInitialTime(calendarId, initialTime = currentTime);
            } else if (initialTime == -1) {
                checkChanges(calendarId);
                continue;
            }

            long startTime = initialTime - settings.getCalendarsDaysPast() * Constants.DAY_MS;
            long startMax = settings.getCalendarStartTime(calendarId, initialTime);
            long startMin = Math.max(startTime, startMax - 30 * Constants.DAY_MS);

            long endTime = initialTime + settings.getCalendarsDaysFuture() * Constants.DAY_MS;
            long endMin = settings.getCalendarEndTime(calendarId, initialTime);
            long endMax = Math.min(endMin + 30 * Constants.DAY_MS, endTime);

            calendar = provider.getEvents(calendarId, startMin, startMax, endMin, endMax);
            if (calendar == null) {
                continue;
            }

            for (Event event : calendar.events) {
                event.calendarId = calendarId;
                if (event.color == 0) {
                    event.color = calendar.color;
                }

                publishProgress(event);
            }

            Settings settings = Settings.getInstance(context);
            settings.setCalendarStartTime(calendarId, startMin);
            settings.setCalendarEndTime(calendarId, endMax);

            if (!isRunning) {
                if (startMin == startTime && endMax == endTime) {
                    settings.setCalendarInitialTime(calendarId, -1);
                } else {
                    isRunning = true;
                }
            }
        }

        if (isRunning) {
            doInBackground(params);
        }

        return null;
    }

    @Override
    protected void onPostExecute(Object result) {

    }

    @Override
    protected void onProgressUpdate(Event... values) {
        final Event event = values[0];
        commit(event, new EventManager.EventActionCallback() {
            @Override
            public void onEventAction(EventAction... data) {
                if (data[0].getStatus() == EventAction.STATUS_OK) {
                    AttendeeDataSource dataSource =
                        DatabaseHelper.getDataSource(context, AttendeeDataSource.class);
                    EventAttendeeDataSource eventAttendeeDataSource =
                        DatabaseHelper.getDataSource(context, EventAttendeeDataSource.class);

                    for (Attendee attendee : event.attendees) {
                        String id = dataSource.createAttendee(
                            attendee.mediaId,
                            attendee.email,
                            attendee.phoneNumber,
                            attendee.firstName,
                            attendee.lastName,
                            attendee.userName,
                            attendee.isFavorite,
                            attendee.isFriend
                        );

                        eventAttendeeDataSource.setAttendee(data[0].getId(), id);
                    }

                    EventManager manager = EventManager.getInstance(context);
                    manager.getEvent(data[0].getId()).attendees = event.attendees;
                }
            }
        });
    }

    private void commit(Event event, EventManager.EventActionCallback callback) {
        EventDataSource dataSource =
            DatabaseHelper.getDataSource(context, EventDataSource.class);
        Event original = dataSource.getEvent(event.providerId, event.startTime, event.endTime);

        if (original == null) {

        } else {
//            EventManager.getInstance(context).updateEvent(
//                EventAction.ACTOR_NONE,
//                event,
//                callback
//            );
        }
    }

    @Override
    protected void onCancelled(Object result) {

    }

    public void checkChanges(long calendarId) {
        EventDataSource dataSource = DatabaseHelper.getDataSource(context, EventDataSource.class);
        EventManager manager = EventManager.getInstance(context);

        long defaultTime = settings.getCalendarStartTime(calendarId, 0);
        long startTime = settings.getCalendarUpdateTime(calendarId, defaultTime);
        long endTime = settings.getCalendarEndTime(calendarId, 0);
        // Check for Changes
        Calendar calendar = manager.getUpdates(calendarId, startTime, endTime);
        if (calendar == null) {
            return;
        }

        if (!calendar.events.isEmpty()) {
            long lastUpdateTime = 0;
            for (Event event : calendar.events) {
                event.calendarId = calendarId;
                if (event.color == 0) {
                    event.color = calendar.color;
                }

                publishProgress(event);

                lastUpdateTime = Math.max(lastUpdateTime, event.updateTime);
            }

            settings.setCalendarUpdateTime(calendarId, lastUpdateTime);
        }
        // Check for Deletions
        List<Event> events = dataSource.getEvents(0, endTime, true, calendarId);
        List<Event> remoteEvents = provider.getEvents(0, endTime, calendarId);

        if (!events.isEmpty() && !remoteEvents.isEmpty()) {
            List<EventComparable> tempEvents = new ArrayList<>(events.size());
            for (Event event : events) {
                tempEvents.add(new EventComparable(event.id, event.providerId, event.startTime,
                    event.endTime));
            }

            List<EventComparable> tempRemoteEvents = new ArrayList<>(remoteEvents.size());
            for (Event event : remoteEvents) {
                tempRemoteEvents.add(new EventComparable(event.id, event.providerId,
                    event.startTime, event.endTime));
            }

            tempEvents.removeAll(tempRemoteEvents);

            for (EventComparable event : tempEvents) {
                manager.removeEvent(EventAction.ACTOR_NONE, event.id, null);
            }
        }
    }

    private class EventComparable {

        public String id;
        public long providerId;
        public long startTime;
        public long endTime;

        public EventComparable(String id, long providerId, long startTime, long endTime) {
            this.id = id;
            this.providerId = providerId;
            this.startTime = startTime;
            this.endTime = endTime;
        }

        @Override
        public boolean equals(Object object) {
            if (!(object instanceof EventComparable)) {
                return false;
            }

            EventComparable event = (EventComparable) object;

            if (providerId != event.providerId) {
                return false;
            }

            if (startTime != event.startTime) {
                return false;
            }

            if (endTime != event.endTime) {
                return false;
            }

            return true;
        }
    }
}
