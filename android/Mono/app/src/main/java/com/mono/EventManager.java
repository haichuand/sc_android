package com.mono;

import android.content.ContentValues;
import android.content.Context;

import com.mono.db.DatabaseHelper;
import com.mono.db.DatabaseValues;
import com.mono.db.dao.EventDataSource;
import com.mono.model.Event;
import com.mono.util.Common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class EventManager {

    private static EventManager instance;

    private Context context;

    private final Map<String, Event> cache = new HashMap<>();
    private final List<EventBroadcastListener> listeners = new ArrayList<>();

    private EventManager(Context context) {
        this.context = context;
    }

    public static EventManager getInstance(Context context) {
        if (instance == null) {
            instance = new EventManager(context);
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
        cache.put(event.id, event);
    }

    public Event getEvent(String id, boolean refresh) {
        Event event;

        if (cache.containsKey(id) && !refresh) {
            event = cache.get(id);
        } else {
            EventDataSource dataSource =
                DatabaseHelper.getDataSource(context, EventDataSource.class);
            event = dataSource.getEvent(id);

            if (event != null) {
                add(event);
            }
        }

        return event;
    }

    public List<Event> getEvents(long startTime, int limit) {
        List<Event> result = new ArrayList<>(limit);

        EventDataSource dataSource = DatabaseHelper.getDataSource(context, EventDataSource.class);
        List<Event> events = dataSource.getEvents(startTime, limit);

        for (Event event : events) {
            add(event);

            if (!result.contains(event)) {
                result.add(event);
            }
        }

        return result;
    }

    public List<Event> getEvents(long startTime, long endTime) {
        List<Event> result = new ArrayList<>();

        EventDataSource dataSource = DatabaseHelper.getDataSource(context, EventDataSource.class);
        List<Event> events = dataSource.getEventsByTimePeriod(startTime, endTime);

        for (Event event : events) {
            add(event);

            if (!result.contains(event)) {
                result.add(event);
            }
        }

        return result;
    }

    public void createEvent(int actor, long externalId, String type, String title,
            String description, String location, int color, long startTime, long endTime,
            EventActionCallback callback) {
        int status = EventAction.STATUS_OK;

        Event event = null;

        EventDataSource dataSource = DatabaseHelper.getDataSource(context, EventDataSource.class);

        String id = null;
        if (!dataSource.containsEventByExternalId(externalId)) {
            id = dataSource.createEvent(
                externalId,
                type,
                title,
                description,
                location,
                color,
                startTime,
                endTime,
                System.currentTimeMillis()
            );
        }

        if (id != null) {
            event = getEvent(id, false);
        } else {
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

    public void updateEvent(int actor, String id, Event event, EventActionCallback callback) {
        int status = EventAction.STATUS_OK;

        Event original = getEvent(id, false);
        ContentValues values = new ContentValues();

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

        if (!event.attendees.equals(original.attendees)) {

        }

        EventDataSource dataSource = DatabaseHelper.getDataSource(context, EventDataSource.class);
        dataSource.updateValues(id, values);

        EventAction data = new EventAction(EventAction.ACTION_UPDATE, actor, status, original);
        if (!listeners.isEmpty()) {
            sendToListeners(data);
        }
        if (callback != null) {
            callback.onEventAction(data);
        }
    }

    public void updateEventTime(int actor, String id, long startTime, long endTime,
            EventActionCallback callback) {
        int status = EventAction.STATUS_OK;

        EventDataSource dataSource = DatabaseHelper.getDataSource(context, EventDataSource.class);
        dataSource.updateTime(id, startTime, endTime);

        Event event = getEvent(id, true);

        EventAction data = new EventAction(EventAction.ACTION_UPDATE, actor, status, event);
        if (!listeners.isEmpty()) {
            sendToListeners(data);
        }
        if (callback != null) {
            callback.onEventAction(data);
        }
    }

    public void removeEvent(int actor, String id, EventActionCallback callback) {
        int status = EventAction.STATUS_OK;

        Event event = cache.remove(id);

        EventDataSource dataSource = DatabaseHelper.getDataSource(context, EventDataSource.class);
        dataSource.removeEvent(id);

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

        public EventAction(int action, int actor, Event event) {
            this(action, actor, STATUS_OK, event);
        }

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
