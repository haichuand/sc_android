package com.mono.provider;

import android.provider.CalendarContract.Attendees;
import android.provider.CalendarContract.Calendars;
import android.provider.CalendarContract.Events;
import android.provider.CalendarContract.Instances;
import android.provider.CalendarContract.Reminders;

/**
 * This class stores all constants to be used in conjunction with the Calendar Providers.
 *
 * @author Gary Ng
 */
public class CalendarValues {

    public static class Calendar {

        public static final String ID = Calendars._ID;
        public static final String NAME = Calendars.CALENDAR_DISPLAY_NAME;
        public static final String COLOR = Calendars.CALENDAR_COLOR;
        public static final String TIME_ZONE = Calendars.CALENDAR_TIME_ZONE;
        public static final String OWNER_ACCOUNT = Calendars.OWNER_ACCOUNT;
        public static final String ACCOUNT_NAME = Calendars.ACCOUNT_NAME;
        public static final String ACCOUNT_TYPE = Calendars.ACCOUNT_TYPE;
        public static final String PRIMARY = Calendars.IS_PRIMARY;

        public static final String[] PROJECTION = {
            ID, NAME, COLOR, TIME_ZONE, OWNER_ACCOUNT, ACCOUNT_NAME, ACCOUNT_TYPE, PRIMARY
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

        public static final String ID = Events._ID;
        public static final String CALENDAR_ID = Events.CALENDAR_ID;
        public static final String CALENDAR_NAME = Events.CALENDAR_DISPLAY_NAME;
        public static final String TITLE = Events.TITLE;
        public static final String DESC = Events.DESCRIPTION;
        public static final String LOCATION = Events.EVENT_LOCATION;
        public static final String COLOR = Events.EVENT_COLOR;
        public static final String REMOTE_ID = Events.SYNC_DATA1;
        public static final String UPDATE_TIME = Events.SYNC_DATA5;
        public static final String START_TIME = Events.DTSTART;
        public static final String END_TIME = Events.DTEND;
        public static final String TIMEZONE = Events.EVENT_TIMEZONE;
        public static final String END_TIMEZONE = Events.EVENT_END_TIMEZONE;
        public static final String ALL_DAY = Events.ALL_DAY;
        public static final String LAST_REPEAT_TIME = Events.LAST_DATE;

        public static final String[] PROJECTION = {
            ID, CALENDAR_ID, TITLE, DESC, LOCATION, COLOR, REMOTE_ID, UPDATE_TIME, START_TIME,
            END_TIME, TIMEZONE, END_TIMEZONE, ALL_DAY, LAST_REPEAT_TIME
        };

        public static final int INDEX_ID = 0;
        public static final int INDEX_CALENDAR_ID = 1;
        public static final int INDEX_TITLE = 2;
        public static final int INDEX_DESC = 3;
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

        public static final String CALENDAR_ID = Instances.CALENDAR_ID;
        public static final String EVENT_ID = Instances.EVENT_ID;
        public static final String BEGIN = Instances.BEGIN;
        public static final String END = Instances.END;

        public static final String[] PROJECTION = {
            CALENDAR_ID, EVENT_ID, BEGIN, END
        };

        public static final int INDEX_CALENDAR_ID = 0;
        public static final int INDEX_EVENT_ID = 1;
        public static final int INDEX_BEGIN = 2;
        public static final int INDEX_END = 3;
    }

    public static class Attendee {

        public static final String ID = Attendees._ID;
        public static final String EVENT_ID = Attendees.EVENT_ID;
        public static final String NAME = Attendees.ATTENDEE_NAME;
        public static final String EMAIL = Attendees.ATTENDEE_EMAIL;
        public static final String RELATIONSHIP = Attendees.ATTENDEE_RELATIONSHIP;
        public static final String TYPE = Attendees.ATTENDEE_TYPE;
        public static final String STATUS = Attendees.ATTENDEE_STATUS;

        public static final String[] PROJECTION = {
            ID, EVENT_ID, NAME, EMAIL, RELATIONSHIP, TYPE, STATUS
        };

        public static final int INDEX_ID = 0;
        public static final int INDEX_EVENT_ID = 1;
        public static final int INDEX_NAME = 2;
        public static final int INDEX_EMAIL = 3;
        public static final int INDEX_RELATIONSHIP = 4;
        public static final int INDEX_TYPE = 5;
        public static final int INDEX_STATUS = 6;
    }

    public static class Reminder {

        public static final String ID = Reminders._ID;
        public static final String EVENT_ID = Reminders.EVENT_ID;
        public static final String MINUTES = Reminders.MINUTES;
        public static final String METHOD = Reminders.METHOD;

        public static final String[] PROJECTION = {
            ID, EVENT_ID, MINUTES, METHOD
        };

        public static final int INDEX_ID = 0;
        public static final int INDEX_EVENT_ID = 1;
        public static final int INDEX_MINUTES = 2;
        public static final int INDEX_METHOD = 3;
    }
}
