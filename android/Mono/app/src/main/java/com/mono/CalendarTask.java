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
import com.mono.settings.Settings;
import com.mono.util.Constants;
import com.mono.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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

        for (long calendarId : calendarIds) {
            long initialTime = settings.getCalendarInitialTime(calendarId, 0);

            if (initialTime == 0) {
                settings.setCalendarInitialTime(calendarId, initialTime = currentTime);
            } else if (initialTime == -1) {
                checkChanges(calendarIds);
                continue;
            }

            long startTime = initialTime - settings.getCalendarsDaysPast() * Constants.DAY_MS;
            long startMax = settings.getCalendarStartTime(calendarId, initialTime);
            long startMin = Math.max(startTime, startMax - 30 * Constants.DAY_MS);

            long endTime = initialTime + settings.getCalendarsDaysFuture() * Constants.DAY_MS;
            long endMin = settings.getCalendarEndTime(calendarId, initialTime);
            long endMax = Math.min(endMin + 30 * Constants.DAY_MS, endTime);

            Calendar calendar = provider.getEvents(calendarId, startMin, startMax, endMin, endMax);
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
            public void onEventAction(EventAction data) {
                if (data.getStatus() == EventAction.STATUS_OK) {
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

                        eventAttendeeDataSource.setAttendee(data.getEvent().id, id);
                    }

                    data.getEvent().attendees = event.attendees;
                }
            }
        });
    }

    private void commit(Event event, EventManager.EventActionCallback callback) {
        EventDataSource dataSource =
            DatabaseHelper.getDataSource(context, EventDataSource.class);
        Event original = dataSource.getEvent(event.internalId, event.startTime, event.endTime);

        if (original == null) {
//            EventManager.getInstance(context).createEvent(
//                EventAction.ACTOR_NONE,
//                event.calendarId,
//                event.internalId,
//                event.externalId,
//                Event.TYPE_CALENDAR,
//                event.title,
//                event.description,
//                event.location != null ? event.location.name : null,
//                event.color,
//                event.startTime,
//                event.endTime,
//                event.timeZone,
//                event.endTimeZone,
//                event.allDay,
//                callback
//            );
        } else {
            EventManager.getInstance(context).updateEvent(
                EventAction.ACTOR_NONE,
                original.id,
                event,
                callback
            );
        }
    }

    @Override
    protected void onCancelled(Object result) {

    }

    public void checkChanges(Set<Long> calendarIds) {
        EventDataSource dataSource = DatabaseHelper.getDataSource(context, EventDataSource.class);
        EventManager manager = EventManager.getInstance(context);

        for (long calendarId : calendarIds) {
            long defaultTime = settings.getCalendarStartTime(calendarId, 0);
            long startTime = settings.getCalendarUpdateTime(calendarId, defaultTime);
            long endTime = settings.getCalendarEndTime(calendarId, 0);
            // Check for Changes
            Calendar calendar = provider.getUpdates(calendarId, startTime, endTime);
            if (calendar == null) {
                continue;
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
            List<Event> events = dataSource.getEvents(0, endTime, calendarId);
            List<Event> remoteEvents = provider.getEvents(calendarId, 0, endTime);

            if (!events.isEmpty() && !remoteEvents.isEmpty()) {
                List<EventComparable> tempEvents = new ArrayList<>(events.size());
                for (Event event : events) {
                    tempEvents.add(new EventComparable(event.id, event.internalId, event.startTime,
                        event.endTime));
                }

                List<EventComparable> tempRemoteEvents = new ArrayList<>(remoteEvents.size());
                for (Event event : remoteEvents) {
                    tempRemoteEvents.add(new EventComparable(event.id, event.internalId,
                        event.startTime, event.endTime));
                }

                tempEvents.removeAll(tempRemoteEvents);

                for (EventComparable event : tempEvents) {
                    manager.removeEvent(EventAction.ACTOR_NONE, event.id, null);
                }
            }
        }
    }

    private class EventComparable {

        public String id;
        public long externalId;
        public long startTime;
        public long endTime;

        public EventComparable(String id, long externalId, long startTime, long endTime) {
            this.id = id;
            this.externalId = externalId;
            this.startTime = startTime;
            this.endTime = endTime;
        }

        @Override
        public boolean equals(Object object) {
            if (!(object instanceof EventComparable)) {
                return false;
            }

            EventComparable event = (EventComparable) object;

            if (externalId != event.externalId) {
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
