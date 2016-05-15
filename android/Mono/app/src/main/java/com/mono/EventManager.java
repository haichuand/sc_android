package com.mono;

import android.content.ContentValues;
import android.content.Context;

import com.mono.db.DatabaseHelper;
import com.mono.db.DatabaseValues;
import com.mono.db.dao.EventAttendeeDataSource;
import com.mono.db.dao.EventDataSource;
import com.mono.model.Event;
import com.mono.provider.CalendarEventProvider;
import com.mono.util.Common;
import com.mono.util.Constants;
import com.mono.util.Log;
import com.mono.util.Strings;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TimeZone;

public class EventManager {

    private static EventManager instance;

    private Context context;

    private final Map<String, Event> cache = new HashMap<>();
    private final List<EventBroadcastListener> listeners = new ArrayList<>();

    private Log log;

    private EventManager(Context context) {
        this.context = context;
        log = Log.getInstance(context);
    }

    public static EventManager getInstance(Context context) {
        if (instance == null) {
            instance = new EventManager(context.getApplicationContext());
        }

        return instance;
    }

    public void addEventBroadcastListener(EventBroadcastListener listener) {
        listeners.add(listener);
    }

    public void removeEventBroadcastListener(EventBroadcastListener listener) {
        Iterator<EventBroadcastListener> iterator = listeners.iterator();

        while (iterator.hasNext()) {
            if (iterator.next() == listener) {
                iterator.remove();
            }
        }
    }

    private void sendToListeners(EventAction data) {
        for (EventBroadcastListener listener : listeners) {
            listener.onEventBroadcast(data);
        }
    }

    private void add(Event event) {
        EventAttendeeDataSource eventAttendeeDataSource =
            DatabaseHelper.getDataSource(context, EventAttendeeDataSource.class);
        event.attendees = eventAttendeeDataSource.getAttendees(event.id);

        cache.put(event.id, event);
    }

    public Event getEvent(String id, boolean refresh) {
        Event event = null;

        if (cache.containsKey(id) && !refresh) {
            event = cache.get(id);
        } else {
            String[] values = Common.explode(".", id);
            if (values.length == 3) {
                long eventId = Long.parseLong(values[0]);
                long startTime = Long.parseLong(values[1]);
                long endTime = Long.parseLong(values[2]);

                CalendarEventProvider provider = CalendarEventProvider.getInstance(context);
                event = provider.getEvent(eventId, startTime, endTime);
            }

            if (event == null) {
                EventDataSource dataSource =
                    DatabaseHelper.getDataSource(context, EventDataSource.class);
                event = dataSource.getEvent(id);
            }

            if (event != null) {
                add(event);
            }
        }

        return event;
    }

    public List<Event> getEventsByOffset(long startTime, int offset, int limit, int direction,
            long... calendarIds) {
        List<Event> result = new ArrayList<>(limit);

        EventDataSource dataSource = DatabaseHelper.getDataSource(context, EventDataSource.class);
        List<Event> events = dataSource.getEvents(startTime, offset, limit, direction, calendarIds);

        for (Event event : events) {
            add(event);
            result.add(event);
        }

        return result;
    }

    public List<Event> getEventsFromProviderByOffset(long startTime, int offset, int limit,
            int direction, long... calendarIds) {
        List<Event> result = new ArrayList<>(limit);

        long start, end, range = 10 * 365 * Constants.DAY_MS;

        if (direction >= 0) {
            start = startTime;
            end = startTime + range;
        } else {
            start = Math.max(startTime - range, 0);
            end = startTime;
        }

        CalendarEventProvider provider = CalendarEventProvider.getInstance(context);
        List<Event> events = provider.getEvents(start, end, offset, limit, direction, calendarIds);

        for (Event event : events) {
            add(event);
            result.add(event);
        }

        return result;
    }

    public List<Event> getEvents(long startTime, long endTime, long... calendarIds) {
        List<Event> result = new ArrayList<>();

        CalendarEventProvider provider = CalendarEventProvider.getInstance(context);
        result.addAll(provider.getEvents(startTime, endTime, calendarIds));

        EventDataSource dataSource = DatabaseHelper.getDataSource(context, EventDataSource.class);
        List<Event> events = dataSource.getEvents(startTime, endTime, calendarIds);

        for (Event event : events) {
            add(event);

            if (!result.contains(event)) {
                result.add(event);
            }
        }

        return result;
    }

    public List<Event> getEvents(int year, int month, int day, long... calendarIds) {
        List<Event> result = new ArrayList<>();

        CalendarEventProvider provider = CalendarEventProvider.getInstance(context);
        result.addAll(provider.getEvents(year, month, day, calendarIds));

        EventDataSource dataSource = DatabaseHelper.getDataSource(context, EventDataSource.class);
        List<Event> events = dataSource.getEvents(year, month, day, calendarIds);

        for (Event event : events) {
            add(event);

            if (!result.contains(event)) {
                result.add(event);
            }
        }

        return result;
    }

    public Map<Integer, List<Integer>> getEventColorsByMonth(int year, int month,
            long... calendarIds) {
        Map<Integer, List<Integer>> result = new HashMap<>();

        CalendarEventProvider provider = CalendarEventProvider.getInstance(context);
        result.putAll(provider.getEventColors(year, month, calendarIds));

        EventDataSource dataSource = DatabaseHelper.getDataSource(context, EventDataSource.class);
        Map<Integer, List<Integer>> entries = dataSource.getEventColors(year, month, calendarIds);

        for (Entry<Integer, List<Integer>> entry : entries.entrySet()) {
            int day = entry.getKey();
            List<Integer> colors = entry.getValue();

            if (!result.containsKey(day)) {
                result.put(day, new ArrayList<Integer>());
            }

            result.get(day).addAll(colors);
        }

        return result;
    }

    public Event getUserstayEventByStartTime(long startTime) {
        Event event = null;

        EventDataSource dataSource = DatabaseHelper.getDataSource(context, EventDataSource.class);
        event = dataSource.getUserstayEventByStartTime(startTime);

        return event;
    }

    public Event getUserstayEventByEndTime (long endTime) {
        Event event = null;

        EventDataSource dataSource = DatabaseHelper.getDataSource(context, EventDataSource.class);
        event = dataSource.getUserstayEventByEndTime(endTime);

        return event;

    }

    public void updateEventTime (String eventId, long newStartTime, long newEndTime) {

        EventDataSource dataSource = DatabaseHelper.getDataSource(context, EventDataSource.class);
        dataSource.updateTime(eventId, newStartTime, newEndTime);

    }

    public void createEvent(int actor, long calendarId, long internalId, String externalId,
            String type, String title, String description, String location, int color,
            long startTime, long endTime, String timeZone, String endTimeZone, boolean allDay,
            EventActionCallback callback) {
        int status = EventAction.STATUS_OK;

        Event event = null;

        EventDataSource dataSource = DatabaseHelper.getDataSource(context, EventDataSource.class);

        if (timeZone == null) {
            timeZone = TimeZone.getDefault().getID();
        }

        String id = null;
        if (!dataSource.containsEvent(internalId, startTime, endTime)) {
            id = dataSource.createEvent(
                calendarId,
                internalId,
                externalId,
                type,
                title,
                description,
                location,
                color,
                startTime,
                endTime,
                timeZone,
                endTimeZone,
                allDay ? 1 : 0,
                System.currentTimeMillis()
            );
        }

        if (id != null) {
            log.debug(getClass().getSimpleName(), Strings.LOG_EVENT_CREATE, id);
            event = getEvent(id, false);
        } else {
            log.debug(getClass().getSimpleName(), Strings.LOG_EVENT_CREATE_FAILED);
            status = EventAction.STATUS_FAILED;
        }

        EventAction data = new EventAction(EventAction.ACTION_CREATE, actor, status, event);
        if (!listeners.isEmpty()) {
            sendToListeners(data);
        }
        if (callback != null) {
            callback.onEventAction(data);
        }
    }

    public void createSyncEvent(int actor, long calendarId, String title, String description,
            String location, int color, long startTime, long endTime, String timeZone,
            String endTimeZone, boolean allDay, EventActionCallback callback) {
        int status = EventAction.STATUS_OK;

        Event event = null;

        CalendarEventProvider provider = CalendarEventProvider.getInstance(context);

        if (timeZone == null) {
            timeZone = TimeZone.getDefault().getID();
        }

        String id = null;

        long eventId = provider.createEvent(
            calendarId,
            title,
            description,
            location,
            color,
            startTime,
            endTime,
            timeZone,
            endTimeZone,
            allDay ? 1 : 0
        );

        if (eventId > 0) {
            id = CalendarEventProvider.createId(eventId, startTime, endTime);
        }

        if (id != null) {
            log.debug(getClass().getSimpleName(), Strings.LOG_EVENT_CREATE, id);
            event = getEvent(id, false);
        } else {
            log.debug(getClass().getSimpleName(), Strings.LOG_EVENT_CREATE_FAILED);
            status = EventAction.STATUS_FAILED;
        }

        EventAction data = new EventAction(EventAction.ACTION_CREATE, actor, status, event);
        if (!listeners.isEmpty()) {
            sendToListeners(data);
        }
        if (callback != null) {
            callback.onEventAction(data);
        }
    }

    public String createEvent (long calendarId, long internalId, String externalId,
                               String type, String title, String description, String location, int color,
                               long startTime, long endTime, String timeZone, String endTimeZone, boolean allDay) {
        Event event = null;
        int status = EventAction.STATUS_OK;

        EventDataSource dataSource = DatabaseHelper.getDataSource(context, EventDataSource.class);

        String id = null;

        if (timeZone == null) {
            timeZone = TimeZone.getDefault().getID();
        }

        id = dataSource.createEvent(
                calendarId,
                internalId,
                externalId,
                type,
                title,
                description,
                location,
                color,
                startTime,
                endTime,
                timeZone,
                endTimeZone,
                allDay ? 1 : 0,
                System.currentTimeMillis()
        );

        if (id != null) {
            log.debug(getClass().getSimpleName(), Strings.LOG_EVENT_CREATE, id);
            event = getEvent(id, false);
        } else {
            log.debug(getClass().getSimpleName(), Strings.LOG_EVENT_CREATE_FAILED);
            status = EventAction.STATUS_FAILED;
        }
        return id;
    }

    public Set<String> updateEvent(int actor, String id, Event event,
            EventActionCallback callback) {
        int status = EventAction.STATUS_OK;

        Event original = getEvent(id, true);
        ContentValues values = new ContentValues();

        if (event.calendarId != original.calendarId) {
            values.put(DatabaseValues.Event.CALENDAR_ID, event.calendarId);
            original.calendarId = event.calendarId;
        }

        if (event.internalId != original.internalId) {
            values.put(DatabaseValues.Event.INTERNAL_ID, event.internalId);
            original.internalId = event.internalId;
        }

        if (!Common.compareStrings(event.externalId, original.externalId)) {
            values.put(DatabaseValues.Event.EXTERNAL_ID, event.externalId);
            original.externalId = event.externalId;
        }

        if (!Common.compareStrings(event.type, original.type)) {
            values.put(DatabaseValues.Event.TYPE, event.type);
            original.type = event.type;
        }

        if (!Common.compareStrings(event.title, original.title)) {
            values.put(DatabaseValues.Event.TITLE, event.title);
            original.title = event.title;
        }

        if (!Common.compareStrings(event.description, original.description)) {
            values.put(DatabaseValues.Event.DESC, event.description);
            original.description = event.description;
        }

        if (event.location != null && !event.location.equals(original.location) ||
                event.location == null && original.location != null) {
            String location = event.location != null ? event.location.name : null;
            values.put(DatabaseValues.Event.LOCATION, location);
            original.location = event.location;
        }

        if (event.color != original.color) {
            values.put(DatabaseValues.Event.COLOR, event.color);
            original.color = event.color;
        }

        if (event.startTime != original.startTime) {
            values.put(DatabaseValues.Event.START_TIME, event.startTime);
            original.startTime = event.startTime;
        }

        if (event.endTime != original.endTime)  {
            values.put(DatabaseValues.Event.END_TIME, event.endTime);
            original.endTime = event.endTime;
        }

        if (!Common.compareStrings(event.timeZone, original.timeZone)) {
            values.put(DatabaseValues.Event.TIMEZONE, event.timeZone);
            original.timeZone = event.timeZone;
        }

        if (!Common.compareStrings(event.endTimeZone, original.endTimeZone)) {
            values.put(DatabaseValues.Event.END_TIMEZONE, event.endTimeZone);
            original.endTimeZone = event.endTimeZone;
        }

        if (event.allDay != original.allDay) {
            values.put(DatabaseValues.Event.ALL_DAY, event.allDay);
            original.allDay = event.allDay;
        }

        if (!event.attendees.equals(original.attendees)) {

        }

        if (!event.reminders.equals(original.reminders)) {

        }

        EventDataSource dataSource = DatabaseHelper.getDataSource(context, EventDataSource.class);
        if (dataSource.updateValues(id, values) > 0) {
            log.debug(getClass().getSimpleName(), Strings.LOG_EVENT_UPDATE, id);
        } else {
            log.debug(getClass().getSimpleName(), Strings.LOG_EVENT_UPDATE_FAILED, id);
        }

        EventAction data = new EventAction(EventAction.ACTION_UPDATE, actor, status, original);
        if (!listeners.isEmpty()) {
            sendToListeners(data);
        }
        if (callback != null) {
            callback.onEventAction(data);
        }

        return values.keySet();
    }

    public void removeEvent(int actor, String id, EventActionCallback callback) {
        int status = EventAction.STATUS_OK;

        Event event = cache.remove(id);

        EventDataSource dataSource = DatabaseHelper.getDataSource(context, EventDataSource.class);
        if (dataSource.removeEvent(id) > 0) {
            log.debug(getClass().getSimpleName(), Strings.LOG_EVENT_REMOVE, id);
        } else {
            log.debug(getClass().getSimpleName(), Strings.LOG_EVENT_REMOVE_FAILED, id);
        }

        EventAction data = new EventAction(EventAction.ACTION_REMOVE, actor, status, event);
        if (!listeners.isEmpty()) {
            sendToListeners(data);
        }
        if (callback != null) {
            callback.onEventAction(data);
        }
    }

    public static class EventAction {

        public static final int ACTION_CREATE = 0;
        public static final int ACTION_UPDATE = 1;
        public static final int ACTION_REMOVE = 2;

        public static final int ACTOR_NONE = 0;
        public static final int ACTOR_SELF = 1;

        public static final int STATUS_OK = 0;
        public static final int STATUS_FAILED = 1;

        private final int action;
        private final int actor;
        private final int status;
        private final Event event;

        public EventAction(int action, int actor, int status, Event event) {
            this.action = action;
            this.actor = actor;
            this.status = status;
            this.event = event;
        }

        public int getAction() {
            return action;
        }

        public int getActor() {
            return actor;
        }

        public int getStatus() {
            return status;
        }

        public Event getEvent() {
            return event;
        }
    }

    public interface EventActionCallback {

        void onEventAction(EventAction data);
    }

    public interface EventBroadcastListener {

        void onEventBroadcast(EventAction data);
    }
}
