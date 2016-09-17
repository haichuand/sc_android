package com.mono;

import android.content.ContentValues;
import android.content.Context;

import com.mono.alarm.AlarmHelper;
import com.mono.db.DatabaseHelper;
import com.mono.db.DatabaseValues;
import com.mono.db.dao.AttendeeDataSource;
import com.mono.db.dao.EventAttendeeDataSource;
import com.mono.db.dao.EventDataSource;
import com.mono.db.dao.EventMediaDataSource;
import com.mono.db.dao.LocationDataSource;
import com.mono.db.dao.MediaDataSource;
import com.mono.model.Attendee;
import com.mono.model.Event;
import com.mono.model.Location;
import com.mono.model.Media;
import com.mono.model.Reminder;
import com.mono.provider.CalendarEventProvider;
import com.mono.provider.CalendarReminderProvider;
import com.mono.util.Common;
import com.mono.util.Constants;
import com.mono.util.Log;
import com.mono.util.Strings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TimeZone;

/**
 * This manager class is used to centralize all events related actions such as retrieving events
 * from the database or Calendar Provider. Calendar events retrieved are also cached here to
 * improve efficiency.
 *
 * @author Gary Ng
 */
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

    /**
     * Add listener to observe changes in new and existing events.
     *
     * @param listener The callback listener.
     */
    public void addEventBroadcastListener(EventBroadcastListener listener) {
        listeners.add(listener);
    }

    /**
     * Remove listener from observing any future changes in new and existing events.
     *
     * @param listener The callback listener.
     */
    public void removeEventBroadcastListener(EventBroadcastListener listener) {
        Iterator<EventBroadcastListener> iterator = listeners.iterator();

        while (iterator.hasNext()) {
            if (iterator.next() == listener) {
                iterator.remove();
            }
        }
    }

    /**
     * Passes event changes to all listeners.
     *
     * @param data The event action data.
     */
    private void sendToListeners(EventAction data) {
        for (EventBroadcastListener listener : listeners) {
            listener.onEventBroadcast(data);
        }
    }

    /**
     * Insert an event into the cache and retrieve any additional event information.
     *
     * @param event The instance of an event.
     */
    private void add(Event event) {
        if (event.source == Event.SOURCE_DATABASE) {
            EventAttendeeDataSource dataSource =
                DatabaseHelper.getDataSource(context, EventAttendeeDataSource.class);
            event.attendees = dataSource.getAttendees(event.id);
        }

        if (event.location != null && event.location.id > 0) {
            LocationManager manager = LocationManager.getInstance(context);
            event.location = manager.getLocation(event.location.id);
        }

        EventMediaDataSource mediaDataSource =
            DatabaseHelper.getDataSource(context, EventMediaDataSource.class);
        event.photos = mediaDataSource.getMedia(event.id, Media.IMAGE);

        if (event.photos.isEmpty()) {
            MediaManager manager = MediaManager.getInstance(context);
            event.photos = manager.getImages(event.startTime, event.endTime);
        }

        cache.put(event.id, event);
    }

    /**
     * Retrieve an event using the ID.
     *
     * @param id The value of the event ID.
     * @param refresh The value to trigger a new retrieval from its original source.
     * @return an instance of the event.
     */
    public Event getEvent(String id, boolean refresh) {
        Event event = null;

        if (cache.containsKey(id) && !refresh) {
            event = cache.get(id);
        } else {
            // Calendar Provider Uses a Composite-like ID
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
            // Cache Event
            if (event != null) {
                add(event);
            }
        }

        return event;
    }

    /**
     * Retrieve all events from the database that starts at a given time.
     *
     * @param startTime The start time of events to return.
     * @param offset The value of the offset.
     * @param limit The value of the limit.
     * @param direction The value of ascending or descending.
     * @param calendarIds The calendars to retrieve events from.
     * @return a list of events.
     */
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

    /**
     * Retrieve all events from the Calendar Provider that starts at a given time.
     *
     * @param startTime The start time of events to return.
     * @param offset The value of the offset.
     * @param limit The value of the limit.
     * @param direction The value of ascending or descending.
     * @param calendarIds The calendars to retrieve events from.
     * @return a list of events.
     */
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

    /**
     * Retrieve all events belonging within a time range.
     *
     * @param startTime The start time of events to return.
     * @param endTime The end time of events to return.
     * @param calendarIds The calendars to retrieve events from.
     * @return a list of events.
     */
    public List<Event> getEvents(long startTime, long endTime, long... calendarIds) {
        List<Event> result = new ArrayList<>();
        // Events from Calendar Provider
        CalendarEventProvider provider = CalendarEventProvider.getInstance(context);
        result.addAll(provider.getEvents(startTime, endTime, calendarIds));
        // Events from Database
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

    /**
     * Retrieve all events belonging in a specific day.
     *
     * @param year The value of the year.
     * @param month The value of the month.
     * @param day The value of the day.
     * @param calendarIds The calendars to retrieve events from.
     * @return a list of events.
     */
    public List<Event> getEvents(int year, int month, int day, long... calendarIds) {
        List<Event> result = new ArrayList<>();
        // Events from Calendar Provider
        CalendarEventProvider provider = CalendarEventProvider.getInstance(context);
        result.addAll(provider.getEvents(year, month, day, calendarIds));
        // Events from Database
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

    /**
     * Retrieve all events containing terms belonging in a query.
     *
     * @param query The filtering query.
     * @param limit The value of the limit.
     * @param calendarIds The calendars to retrieve events from.
     * @return a list of events.
     */
    public List<Event> getEvents(String query, int limit, long... calendarIds) {
        List<Event> result = new ArrayList<>();

        long startTime = 0;
        long endTime = System.currentTimeMillis() + 10 * 365 * Constants.DAY_MS;
        // Events from Calendar Provider
        CalendarEventProvider provider = CalendarEventProvider.getInstance(context);
        result.addAll(provider.getEvents(startTime, endTime, query, limit, calendarIds));
        // Events from Database
        EventDataSource dataSource = DatabaseHelper.getDataSource(context, EventDataSource.class);
        List<Event> events = dataSource.getEvents(startTime, endTime, query, limit, calendarIds);

        for (Event event : events) {
            add(event);

            if (!result.contains(event)) {
                result.add(event);
            }
        }

        Collections.sort(result, new Comparator<Event>() {
            @Override
            public int compare(Event e1, Event e2) {
                return Long.compare(e2.startTime, e1.startTime);
            }
        });

        return result;
    }

    /**
     * Retrieve events with reminders belonging within a time range.
     *
     * @param startTime Start time of the events.
     * @param endTime End time of the events.
     * @param calendarIds Restrict events to these calendars.
     * @return a list of events.
     */
    public List<Event> getEventsWithReminders(long startTime, long endTime, long... calendarIds) {
        List<Event> result = new ArrayList<>();
        // Events from Calendar Provider
        CalendarEventProvider provider = CalendarEventProvider.getInstance(context);
        result.addAll(provider.getEventsWithReminders(startTime, endTime, calendarIds));
        // Events from Database
        EventDataSource dataSource = DatabaseHelper.getDataSource(context, EventDataSource.class);
        List<Event> events = dataSource.getEventsWithReminders(startTime, endTime, calendarIds);

        for (Event event : events) {
            add(event);

            if (!result.contains(event)) {
                result.add(event);
            }
        }

        return result;
    }

    /**
     * Retrieve all color markers for the specific month.
     *
     * @param year The value of the year.
     * @param month The value of the month.
     * @param calendarIds The calendars to retrieve events from.
     * @return a map of colors for each day of the month.
     */
    public Map<Integer, List<Integer>> getEventColorsByMonth(int year, int month,
            long... calendarIds) {
        Map<Integer, List<Integer>> result = new HashMap<>();
        // Event Colors from Calendar Provider
        CalendarEventProvider provider = CalendarEventProvider.getInstance(context);
        result.putAll(provider.getEventColors(year, month, calendarIds));
        // Event Colors from Database
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

    /**
     * Create an event into the database.
     *
     * @param actor The caller such as the user or system.
     * @param event The data describing the event.
     * @param callback The callback used once completed.
     * @return the event ID.
     */
    public String createEvent(int actor, Event event, EventActionCallback callback) {
        int status = EventAction.STATUS_OK;

        EventDataSource dataSource = DatabaseHelper.getDataSource(context, EventDataSource.class);

        if (event.timeZone == null) {
            event.timeZone = TimeZone.getDefault().getID();
        }

        String id = null;
        if (!dataSource.containsEvent(event.internalId, event.startTime, event.endTime)) {
            // Convert Reminders
            String reminders = null;
            if (!event.reminders.isEmpty()) {
                reminders = "";

                for (int i = 0; i < event.reminders.size(); i++) {
                    if (i > 0) reminders += ";";
                    Reminder reminder = event.reminders.get(i);
                    reminders += reminder.minutes + "," + reminder.method;
                }
            }
            // Create Event into Database
            id = dataSource.createEvent(
                event.calendarId,
                event.internalId,
                event.externalId,
                event.type,
                event.title,
                event.description,
                event.location != null ? event.location.id : null,
                event.color,
                event.startTime,
                event.endTime,
                event.timeZone,
                event.endTimeZone,
                event.allDay ? 1 : 0,
                reminders,
                System.currentTimeMillis()
            );
        }

        if (id != null) {
            // Handle Reminders
            if (event.reminders != null) {
                for (Reminder reminder : event.reminders) {
                    long alarmTime = event.startTime - reminder.minutes * Constants.MINUTE_MS;
                    AlarmHelper.createAlarm(context, id, alarmTime, event.title, event.startTime);
                }
            }
            // Create Location
            if (event.location != null) {
                updateEventLocation(id, event.location);
            }
            // Create Participants
            if (event.attendees != null) {
                updateEventAttendees(id, event.attendees);
            }
            // Create Photos
            if (event.photos != null) {
                updateEventPhotos(id, event.photos);
            }

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

        return id;
    }

    /**
     * Create an event into the Calendar Provider.
     *
     * @param actor The caller such as the user or system.
     * @param event The data describing the event.
     * @param callback The callback used once completed.
     * @return the event ID.
     */
    public String createSyncEvent(int actor, Event event, EventActionCallback callback) {
        int status = EventAction.STATUS_OK;

        CalendarEventProvider provider = CalendarEventProvider.getInstance(context);

        if (event.timeZone == null) {
            event.timeZone = TimeZone.getDefault().getID();
        }

        String id = null;
        // Create Event into Calendar Provider
        long eventId = provider.createEvent(
            event.calendarId,
            event.title,
            event.description,
            event.location != null ? event.location.name : null,
            event.color,
            event.startTime,
            event.endTime,
            event.timeZone,
            event.endTimeZone,
            event.allDay ? 1 : 0
        );

        if (eventId > 0) {
            id = CalendarEventProvider.createId(eventId, event.startTime, event.endTime);
            // Handle Reminders
            if (event.reminders != null) {
                for (Reminder reminder : event.reminders) {
                    CalendarReminderProvider reminderProvider =
                        CalendarReminderProvider.getInstance(context);

                    if (reminderProvider.createReminder(eventId, reminder.minutes, reminder.method)) {
                        long alarmTime = event.startTime - reminder.minutes * Constants.MINUTE_MS;
                        AlarmHelper.createAlarm(context, id, alarmTime, event.title, event.startTime);
                    }
                }
            }
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

        return id;
    }

    public boolean saveEventToDatabase (Event event, String eventId) {
        Event tempEvent = new Event(event);
        tempEvent.color = R.color.green;

        String id = createEvent(EventAction.ACTOR_NONE, tempEvent, null);
        if (id == null) {
            return false;
        }
        return updateEventId(id, eventId);
    }

    public boolean updateEventId (String originalId, String newId) {
        EventDataSource dataSource = DatabaseHelper.getDataSource(context, EventDataSource.class);
        return dataSource.updateEventId(originalId, newId) == 1;
    }

    /**
     * Update an existing event in the database.
     *
     * @param actor The caller such as user or system.
     * @param id The value of the event ID.
     * @param event The event information to update.
     * @param callback The callback used once completed.
     * @return a set of type of values updated.
     */
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
            updateEventLocation(id, event.location);
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
            EventAttendeeDataSource eventUserDataSource =
                DatabaseHelper.getDataSource(context, EventAttendeeDataSource.class);
            eventUserDataSource.clearAll(id);

            updateEventAttendees(id, event.attendees);

            original.attendees.clear();
            original.attendees.addAll(event.attendees);
        }

        if (!event.reminders.equals(original.reminders)) {
            String value = null;
            if (!event.reminders.isEmpty()) {
                value = "";

                for (int i = 0; i < event.reminders.size(); i++) {
                    if (i > 0) value += ";";
                    Reminder reminder = event.reminders.get(i);
                    value += reminder.minutes + "," + reminder.method;
                }
            }
            values.put(DatabaseValues.Event.REMINDERS, value);

            original.reminders.clear();
            original.reminders.addAll(event.reminders);

            AlarmHelper.removeAlarms(context, id);
            for (Reminder reminder : event.reminders) {
                long alarmTime = event.startTime - reminder.minutes * Constants.MINUTE_MS;
                AlarmHelper.createAlarm(context, id, alarmTime, event.title, event.startTime);
            }
        }

        if (!event.photos.equals(original.photos)) {
            EventMediaDataSource eventMediaDataSource =
                DatabaseHelper.getDataSource(context, EventMediaDataSource.class);
            eventMediaDataSource.clearAll(id);

            updateEventPhotos(id, event.photos);

            original.photos.clear();
            original.photos.addAll(event.photos);
        }

        EventDataSource dataSource = DatabaseHelper.getDataSource(context, EventDataSource.class);
        if (values.size() == 0 || dataSource.updateValues(id, values) > 0) {
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

    /**
     * Remove an event from the database or provider.
     *
     * @param actor The caller such as user or system.
     * @param id The value of the event ID.
     * @param callback The callback used once completed.
     */
    public void removeEvent(int actor, String id, EventActionCallback callback) {
        int status = EventAction.STATUS_OK;

        Event event = getEvent(id, false);

        boolean result = false;

        if (event.source == Event.SOURCE_DATABASE) {
            EventDataSource dataSource =
                DatabaseHelper.getDataSource(context, EventDataSource.class);
            result = dataSource.removeEvent(id) > 0;
        } else if (event.source == Event.SOURCE_PROVIDER) {
            CalendarEventProvider provider = CalendarEventProvider.getInstance(context);
            result = provider.removeEvent(event.internalId) > 0;
        }

        if (result) {
            AlarmHelper.removeAlarms(context, id);

            cache.remove(id);
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

    /**
     * Update event with the following location.
     *
     * @param id The value of the event ID.
     * @param location The value of the location.
     */
    private void updateEventLocation(String id, Location location) {
        LocationDataSource locationDataSource =
            DatabaseHelper.getDataSource(context, LocationDataSource.class);
        EventDataSource eventDataSource =
            DatabaseHelper.getDataSource(context, EventDataSource.class);

        Long locationId = null;

        if (location != null) {
            locationId = location.id;
            if (locationDataSource.getLocation(locationId) == null) {
                locationId = locationDataSource.createLocation(location.name, location.googlePlaceId,
                    location.getLatitude(), location.getLongitude(), location.getAddress());
            }
        }

        eventDataSource.updateLocation(id, locationId);
    }

    /**
     * Update event with the following attendees.
     *
     * @param id The value of the event ID.
     * @param attendees The list of attendees.
     */
    private void updateEventAttendees(String id, List<Attendee> attendees) {
        AttendeeDataSource userDataSource =
            DatabaseHelper.getDataSource(context, AttendeeDataSource.class);
        EventAttendeeDataSource eventUserDataSource =
            DatabaseHelper.getDataSource(context, EventAttendeeDataSource.class);

        for (Attendee user : attendees) {
            String userId = user.id;
            if (userDataSource.getAttendeeById(userId) == null) {
                userId = userDataSource.createAttendee(null, user.email, user.phoneNumber,
                    user.firstName, user.lastName, user.userName, false, false);
            }

            eventUserDataSource.setAttendee(id, userId);
        }
    }

    /**
     * Update event with the following photos.
     *
     * @param id The value of the event ID.
     * @param photos The list of photos.
     */
    private void updateEventPhotos(String id, List<Media> photos) {
        MediaDataSource mediaDataSource =
            DatabaseHelper.getDataSource(context, MediaDataSource.class);
        EventMediaDataSource eventMediaDataSource =
            DatabaseHelper.getDataSource(context, EventMediaDataSource.class);

        for (Media photo : photos) {
            long mediaId = photo.id;
            String path = photo.uri.toString();

            byte[] thumbnail;
            if (photo.thumbnail != null) {
                thumbnail = photo.thumbnail;
            } else if (Common.fileExists(path)) {
                thumbnail = MediaManager.createThumbnail(path);
            } else {
                continue;
            }

            if (mediaId == 0) {
                mediaId = mediaDataSource.createMedia(path, photo.type, photo.size, thumbnail);
            } else if (photo.thumbnail == null) {
                mediaDataSource.updateThumbnail(mediaId, thumbnail);
            }

            eventMediaDataSource.setMedia(id, mediaId);
        }
    }

    /**
     * Handle changes occurred in the Calendar Provider.
     */
    public void onProviderChange() {
        System.out.format("Calendar Provider has %d changes.\n", -1);
        AlarmHelper.startAll(context);
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
