package com.mono.provider;

import android.provider.CalendarContract.Attendees;
import android.provider.CalendarContract.Calendars;
import android.provider.CalendarContract.Events;
import android.provider.CalendarContract.Instances;
import android.provider.CalendarContract.Reminders;

public class CalendarValues {

    public static class Calendar {

        public static final String[] PROJECTION = {
            Calendars._ID,
            Calendars.CALENDAR_DISPLAY_NAME,
            Calendars.CALENDAR_COLOR,
            Calendars.CALENDAR_TIME_ZONE,
            Calendars.OWNER_ACCOUNT,
            Calendars.ACCOUNT_NAME,
            Calendars.ACCOUNT_TYPE,
            Calendars.IS_PRIMARY
        };

        public static final int INDEX_ID = 0;
        public static final int INDEX_NAME = 1;
        public static final int INDEX_COLOR = 2;
        public static final int INDEX_TIME_ZONE = 3;
        public static final int INDEX_OWNER_ACCOUNT = 4;
        public static final int INDEX_ACCOUNT_NAME = 5;
        public static final int INDEX_ACCOUNT_TYPE = 6;
        public static final int INDEX_PRIMARY = 7;
    }

    public static class Event {

        public static final String[] PROJECTION = {
            Events._ID,
            Events.CALENDAR_ID,
            Events.TITLE,
            Events.DESCRIPTION,
            Events.EVENT_LOCATION,
            Events.EVENT_COLOR,
            Events.SYNC_DATA1,
            Events.SYNC_DATA5,
            Events.DTSTART,
            Events.DTEND,
            Events.EVENT_TIMEZONE,
            Events.EVENT_END_TIMEZONE,
            Events.ALL_DAY,
            Events.LAST_DATE
        };

        public static final int INDEX_ID = 0;
        public static final int INDEX_CALENDAR_ID = 1;
        public static final int INDEX_TITLE = 2;
        public static final int INDEX_DESCRIPTION = 3;
        public static final int INDEX_LOCATION = 4;
        public static final int INDEX_COLOR = 5;
        public static final int INDEX_REMOTE_ID = 6;
        public static final int INDEX_UPDATE_TIME = 7;
        public static final int INDEX_START_TIME = 8;
        public static final int INDEX_END_TIME = 9;
        public static final int INDEX_TIMEZONE = 10;
        public static final int INDEX_END_TIMEZONE = 11;
        public static final int INDEX_ALL_DAY = 12;
        public static final int INDEX_LAST_REPEAT_TIME = 13;
    }

    public static class Instance {

        public static final String[] PROJECTION = {
            Instances.CALENDAR_ID,
            Instances.EVENT_ID,
            Instances.BEGIN,
            Instances.END
        };

        public static final int INDEX_CALENDAR_ID = 0;
        public static final int INDEX_EVENT_ID = 1;
        public static final int INDEX_BEGIN = 2;
        public static final int INDEX_END = 3;
    }

    public static class Attendee {

        public static final String[] PROJECTION = {
            Attendees._ID,
            Attendees.EVENT_ID,
            Attendees.ATTENDEE_NAME,
            Attendees.ATTENDEE_EMAIL
        };

        public static final int INDEX_ID = 0;
        public static final int INDEX_EVENT_ID = 1;
        public static final int INDEX_NAME = 2;
        public static final int INDEX_EMAIL = 3;
    }

    public static class Reminder {

        public static final String[] PROJECTION = {
            Reminders._ID,
            Reminders.EVENT_ID,
            Reminders.MINUTES,
            Reminders.METHOD
        };

        public static final int INDEX_ID = 0;
        public static final int INDEX_EVENT_ID = 1;
        public static final int INDEX_MINUTES = 2;
        public static final int INDEX_METHOD = 3;
    }
}
