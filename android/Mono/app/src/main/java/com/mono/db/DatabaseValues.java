package com.mono.db;

import com.mono.util.Common;

/**
 * This class is used to store all constants related to database tables, fields, projections, etc.
 *
 * @author Gary Ng, Xuejing Dong
 */
public class DatabaseValues {

    private DatabaseValues() {}

    public static String createTableQuery(String table, String[] values) {
        return "CREATE TABLE IF NOT EXISTS " + table + " (" + Common.implode(", ", values) + ");";
    }

    public static String dropTableQuery(String table) {
        return "DROP TABLE IF EXISTS " + table + ";";
    }

    public static class Event {

        public static final String TABLE = "`event`";

        public static final String ID = "`id`";
        public static final String PROVIDER_ID = "`provider_id`";
        public static final String SYNC_ID = "`sync_id`";
        public static final String CALENDAR_ID = "`calendar_id`";
        public static final String TYPE = "`type`";
        public static final String TITLE = "`title`";
        public static final String DESC = "`description`";
        public static final String LOCATION_ID = "`location_id`";
        public static final String COLOR = "`color`";
        public static final String START_TIME = "`start_time`";
        public static final String END_TIME = "`end_time`";
        public static final String TIMEZONE = "`timezone`";
        public static final String END_TIMEZONE = "`end_timezone`";
        public static final String ALL_DAY = "`all_day`";
        public static final String REMINDERS = "`reminders`";
        public static final String FAVORITE = "`favorite`";
        public static final String CREATE_TIME = "`create_time`";
        public static final String MODIFY_TIME = "`modify_time`";
        public static final String VIEW_TIME = "`view_time`";
        public static final String SYNC_TIME = "`sync_time`";
        public static final String ACK = "`ack`"; //flag for ack from server; 1: acked; 0: non-acked

        public static final String[] PROJECTION = {
            ID, PROVIDER_ID, SYNC_ID, CALENDAR_ID, TYPE, TITLE, DESC, LOCATION_ID, COLOR,
            START_TIME, END_TIME, TIMEZONE, END_TIMEZONE, ALL_DAY, REMINDERS, FAVORITE,
            CREATE_TIME, MODIFY_TIME, VIEW_TIME, SYNC_TIME, ACK
        };

        public static final int INDEX_ID = 0;
        public static final int INDEX_PROVIDER_ID = 1;
        public static final int INDEX_SYNC_ID = 2;
        public static final int INDEX_CALENDAR_ID = 3;
        public static final int INDEX_TYPE = 4;
        public static final int INDEX_TITLE = 5;
        public static final int INDEX_DESC = 6;
        public static final int INDEX_LOCATION_ID = 7;
        public static final int INDEX_COLOR = 8;
        public static final int INDEX_START_TIME = 9;
        public static final int INDEX_END_TIME = 10;
        public static final int INDEX_TIMEZONE = 11;
        public static final int INDEX_END_TIMEZONE = 12;
        public static final int INDEX_ALL_DAY = 13;
        public static final int INDEX_REMINDERS = 14;
        public static final int INDEX_FAVORITE = 15;
        public static final int INDEX_CREATE_TIME = 16;
        public static final int INDEX_MODIFY_TIME = 17;
        public static final int INDEX_VIEW_TIME = 18;
        public static final int INDEX_SYNC_TIME = 19;
        public static final int INDEX_ACK = 20;

        public static final String CREATE_TABLE;
        public static final String DROP_TABLE;

        static {
            String[] parameters = {
                ID + " TEXT PRIMARY KEY",
                PROVIDER_ID + " INTEGER",
                SYNC_ID + " TEXT",
                CALENDAR_ID + " INTEGER",
                TYPE + " TEXT",
                TITLE + " TEXT",
                DESC + " TEXT",
                LOCATION_ID + " INTEGER",
//                LOCATION_ID + " INTEGER REFERENCES " + Location.TABLE + " (" + Location.ID + ")",
                COLOR + " INTEGER",
                START_TIME + " INTEGER",
                END_TIME + " INTEGER",
                TIMEZONE + " TEXT",
                END_TIMEZONE + " TEXT",
                ALL_DAY + " INTEGER",
                REMINDERS + " TEXT",
                FAVORITE + " INTEGER",
                CREATE_TIME + " INTEGER",
                MODIFY_TIME + " INTEGER",
                VIEW_TIME + " INTEGER",
                SYNC_TIME + " INTEGER",
                ACK + " INTEGER DEFAULT 1"
            };

            CREATE_TABLE = createTableQuery(TABLE, parameters);
            DROP_TABLE = dropTableQuery(TABLE);
        }
    }

    public static class User {

        public static final String TABLE = "`user`";

        public static final String U_ID = "`id`";
        public static final String MEDIA_ID = "`media_id`";
        public static final String PHONE_NUMBER = "`phone_number`";
        public static final String EMAIL = "`email`";
        public static final String FIRST_NAME = "`first_name`";
        public static final String LAST_NAME = "`last_name`";
        public static final String USER_NAME = "`user_name`";
        public static final String FAVORITE = "`favorite`";
        public static final String FRIEND = "`friend`";
        public static final String SUGGESTED = "`suggested`";

        public static final String[] PROJECTION = {
            User.U_ID,
            User.MEDIA_ID,
            User.PHONE_NUMBER,
            User.EMAIL,
            User.FIRST_NAME,
            User.LAST_NAME,
            User.USER_NAME,
            User.FAVORITE,
            User.FRIEND,
            User.SUGGESTED
        };

        public static final int INDEX_U_ID = 0;
        public static final int INDEX_MEDIA_ID = 1;
        public static final int INDEX_PHONE_NUMBER = 2;
        public static final int INDEX_EMAIL = 3;
        public static final int INDEX_FIRST_NAME = 4;
        public static final int INDEX_LAST_NAME = 5;
        public static final int INDEX_USER_NAME = 6;
        public static final int INDEX_FAVORITE = 7;
        public static final int INDEX_FRIEND = 8;
        public static final int INDEX_SUGGESTED = 9;

        public static final String CREATE_TABLE;
        public static final String DROP_TABLE;

        static {
            String[] parameters = {
                U_ID + " TEXT PRIMARY KEY",
                MEDIA_ID + " TEXT",
                PHONE_NUMBER + " TEXT",
                EMAIL + " TEXT",
                FIRST_NAME + " TEXT",
                LAST_NAME + " TEXT",
                USER_NAME + " TEXT",
                FAVORITE + " INTEGER DEFAULT 0",
                FRIEND + " INTEGER DEFAULT 0",
                SUGGESTED + " INTEGER DEFAULT 0"
            };

            CREATE_TABLE = createTableQuery(TABLE, parameters);
            DROP_TABLE = dropTableQuery(TABLE);
        }
    }

    public static class Location {

        public static final String TABLE = "`location`";

        public static final String ID = "`id`";
        public static final String GOOGLE_PLACE_ID = "`place_id`";
        public static final String NAME = "`name`";
        public static final String ADDRESS = "`address`";
        public static final String LATITUDE = "`latitude`";
        public static final String LONGITUDE = "`longitude`";
        public static final String BEEN_THERE = "`been_there`";

        public static final String[] PROJECTION = {
            Location.ID,
            Location.GOOGLE_PLACE_ID,
            Location.NAME,
            Location.ADDRESS,
            Location.LATITUDE,
            Location.LONGITUDE,
            Location.BEEN_THERE
        };

        public static final int INDEX_ID = 0;
        public static final int INDEX_GOOGLE_PLACE_ID = 1;
        public static final int INDEX_NAME = 2;
        public static final int INDEX_ADDRESS = 3;
        public static final int INDEX_LATITUDE = 4;
        public static final int INDEX_LONGITUDE = 5;
        public static final int INDEX_BEEN_THERE = 6;

        public static final String CREATE_TABLE;
        public static final String DROP_TABLE;

        static {
            String[] parameters = {
                ID + " INTEGER PRIMARY KEY AUTOINCREMENT",
                GOOGLE_PLACE_ID + " TEXT",
                NAME + " TEXT",
                ADDRESS + " TEXT",
                LATITUDE + " REAL",
                LONGITUDE + " REAL",
                BEEN_THERE + " INTEGER"
            };

            CREATE_TABLE = createTableQuery(TABLE, parameters);
            DROP_TABLE = dropTableQuery(TABLE);
        }
    }

    public static class EventLocationCandidates {

        public static final String TABLE = "`event_location_candidates`";

        public static final String EVENT_ID = "`event_id`";
        public static final String LOC_ID = "`loc_id`";

        public static final String[] PROJECTION = {
            EventLocationCandidates.EVENT_ID,
            EventLocationCandidates.LOC_ID
        };

        public static final int INDEX_EVENT_ID = 0;
        public static final int INDEX_LOC_ID = 1;

        public static final String CREATE_TABLE;
        public static final String DROP_TABLE;

        static {
            String[] parameters = {
                EVENT_ID + " TEXT REFERENCES " + Event.TABLE + " (" + Event.ID + ") ON UPDATE CASCADE ON DELETE CASCADE",
                LOC_ID
//                LOC_ID + " INTEGER REFERENCES " + Location.TABLE + " (" + Location.ID + ")"
            };

            CREATE_TABLE = createTableQuery(TABLE, parameters);
            DROP_TABLE = dropTableQuery(TABLE);
        }
    }

    public static class Conversation {
        public static final String TABLE = "`conversation`";

        public static final String C_ID = "`c_id`";
        public static final String C_NAME = "`c_name`";
        public static final String C_CREATOR = "`c_creator`"; //conversation's creator id
        public static final String ACK = "`ack`"; //flag for ack from server; 1: acked; 0: non-acked


        public static final String[] PROJECTION = {
                Conversation.C_ID,
                Conversation.C_NAME,
                Conversation.C_CREATOR,
                Conversation.ACK
        };

        public static final int INDEX_C_ID = 0;
        public static final int INDEX_C_NAME = 1;
        public static final int INDEX_C_CREATOR = 2;
        public static final int INDEX_ACK = 3;

        public static final String CREATE_TABLE;
        public static final String DROP_TABLE;

        static {
            String[] parameters = {
                    C_ID + " TEXT PRIMARY KEY",
                    C_NAME + " TEXT",
                    C_CREATOR + " TEXT",
                    ACK + "INTEGER DEFAULT 1"
            };

            CREATE_TABLE = createTableQuery(TABLE, parameters);
            DROP_TABLE = dropTableQuery(TABLE);
        }
    }

    public static class ConversationContent {
        public static final String TABLE = "`conversation_content`";

        public static final String ID = "`id`";
        public static final String C_ID = "`c_id`";
        public static final String SENDER_ID = "`sender_id`";
        public static final String TEXT = "`text`";
        public static final String TIMESTAMP = "`timestamp`";
        public static final String ACK = "`ack`"; //flag for ack from server; 1: acked; 0: non-acked


        public static final String[] PROJECTION = {
                ConversationContent.ID,
                ConversationContent.C_ID,
                ConversationContent.SENDER_ID,
                ConversationContent.TEXT,
                ConversationContent.TIMESTAMP,
                ConversationContent.ACK
        };

        public static final int INDEX_ID = 0;
        public static final int INDEX_C_ID = 1;
        public static final int INDEX_SENDER_ID = 2;
        public static final int INDEX_TEXT = 3;
        public static final int INDEX_TIMESTAMP = 4;
        public static final int INDEX_ACK = 5;


        public static final String CREATE_TABLE;
        public static final String DROP_TABLE;

        static {
            String[] parameters = {
                    ID + " INTEGER PRIMARY KEY AUTOINCREMENT",
                    C_ID + " TEXT REFERENCES " + Conversation.TABLE + " ( "+ Conversation.C_ID +" ) ON UPDATE CASCADE ON DELETE CASCADE",
                    SENDER_ID + " TEXT REFERENCES " + User.TABLE + " ( "+ User.U_ID +" ) ON UPDATE CASCADE ON DELETE CASCADE",
                    TEXT + " TEXT",
                    TIMESTAMP + " INTEGER",
                    ACK + "INTEGER DEFAULT 1"
            };

            CREATE_TABLE = createTableQuery(TABLE, parameters);
            DROP_TABLE = dropTableQuery(TABLE);
        }
    }

    public static class ConversationAttachments {

        public static final String TABLE = "`conversation_attachments`";

        public static final String MESSAGE_ID = "`message_id`";
        public static final String PATH = "`path`";
        public static final String TYPE = "`type`";
        public static final String STATUS = "`status`";

        public static final String[] PROJECTION = {
            ConversationAttachments.MESSAGE_ID,
            ConversationAttachments.PATH,
            ConversationAttachments.TYPE,
            ConversationAttachments.STATUS
        };

        public static final int INDEX_MESSAGE_ID = 0;
        public static final int INDEX_PATH = 1;
        public static final int INDEX_TYPE = 2;
        public static final int INDEX_STATUS = 3;

        public static final String CREATE_TABLE;
        public static final String DROP_TABLE;

        static {
            String[] parameters = {
                MESSAGE_ID + " TEXT REFERENCES " + ConversationContent.TABLE + " (" + ConversationContent.ID + ") ON UPDATE CASCADE ON DELETE CASCADE",
                PATH + " TEXT",
                TYPE + " TEXT",
                STATUS + " INTEGER"
            };

            CREATE_TABLE = createTableQuery(TABLE, parameters);
            DROP_TABLE = dropTableQuery(TABLE);
        }
    }

    public static class EventConversation {
        public static final String TABLE = "`event_conversation`";

        public static final String EVENT_ID = "`event_id`";
        public static final String C_ID = "`c_id`";



        public static final String[] PROJECTION = {
                EventConversation.EVENT_ID,
                EventConversation.C_ID
        };

        public static final int INDEX_EVENT_ID = 0;
        public static final int INDEX_C_ID = 1;


        public static final String CREATE_TABLE;
        public static final String DROP_TABLE;

        static {
            String[] parameters = {
                    EVENT_ID + " INTEGER REFERENCES " + Event.TABLE + " ( "+ Event.ID +" ) ON UPDATE CASCADE ON DELETE CASCADE",
                    C_ID + " TEXT REFERENCES " + Conversation.TABLE + " ( "+ Conversation.C_ID +" ) ON UPDATE CASCADE ON DELETE CASCADE"
            };

            CREATE_TABLE = createTableQuery(TABLE, parameters);
            DROP_TABLE = dropTableQuery(TABLE);
        }
    }

    public static class EventAttendee {
        public static final String TABLE = "`event_attendee`";

        public static final String EVENT_ID = "`event_id`";
        public static final String ATTENDEE_ID = "`attendee_id`";



        public static final String[] PROJECTION = {
                EventAttendee.EVENT_ID,
                EventAttendee.ATTENDEE_ID
        };

        public static final int INDEX_EVENT_ID = 0;
        public static final int INDEX_ATTENDEE_ID = 1;


        public static final String CREATE_TABLE;
        public static final String DROP_TABLE;

        static {
            String[] parameters = {
                    EVENT_ID + " INTEGER REFERENCES " + Event.TABLE + " ( "+ Event.ID +" ) ON UPDATE CASCADE ON DELETE CASCADE",
                    ATTENDEE_ID + " TEXT REFERENCES " + User.TABLE + " ( "+ User.U_ID +" ) ON UPDATE CASCADE ON DELETE CASCADE"
            };

            CREATE_TABLE = createTableQuery(TABLE, parameters);
            DROP_TABLE = dropTableQuery(TABLE);
        }
    }

    public static class Media {

        public static final String TABLE = "`media`";

        public static final String ID = "`id`";
        public static final String PATH = "`path`";
        public static final String TYPE = "`type`";
        public static final String SIZE = "`size`";
        public static final String THUMBNAIL = "`thumbnail`";

        public static final String[] PROJECTION = {
            Media.ID,
            Media.PATH,
            Media.TYPE,
            Media.SIZE,
            Media.THUMBNAIL
        };

        public static final int INDEX_ID = 0;
        public static final int INDEX_PATH = 1;
        public static final int INDEX_TYPE = 2;
        public static final int INDEX_SIZE = 3;
        public static final int INDEX_THUMBNAIL = 4;

        public static final String CREATE_TABLE;
        public static final String DROP_TABLE;

        static {
            String[] parameters = {
                ID + " INTEGER PRIMARY KEY AUTOINCREMENT",
                PATH + " TEXT",
                TYPE + " TEXT",
                SIZE + " INTEGER",
                THUMBNAIL + " BLOB"
            };

            CREATE_TABLE = createTableQuery(TABLE, parameters);
            DROP_TABLE = dropTableQuery(TABLE);
        }
    }

    public static class EventMedia {

        public static final String TABLE = "`event_media`";

        public static final String EVENT_ID = "`event_id`";
        public static final String MEDIA_ID = "`media_id`";

        public static final String[] PROJECTION = {
            EVENT_ID, MEDIA_ID
        };

        public static final int INDEX_EVENT_ID = 0;
        public static final int INDEX_MEDIA_ID = 1;

        public static final String CREATE_TABLE;
        public static final String DROP_TABLE;

        static {
            String[] parameters = {
                EVENT_ID + " TEXT REFERENCES " + Event.TABLE + " (" + Event.ID + ") ON UPDATE CASCADE ON DELETE CASCADE",
                MEDIA_ID + " INTEGER REFERENCES " + Media.TABLE + " (" + Media.ID + ") ON UPDATE CASCADE ON DELETE CASCADE"
            };

            CREATE_TABLE = createTableQuery(TABLE, parameters);
            DROP_TABLE = dropTableQuery(TABLE);
        }
    }

    public static class RecurringEvent {
        public static final String TABLE = "`recurring_event`";

        public static final String PARENT_ID = "`parent_id`";
        public static final String CHILD_ID = "`child_id`";



        public static final String[] PROJECTION = {
                RecurringEvent.PARENT_ID,
                RecurringEvent.CHILD_ID
        };

        public static final int INDEX_PARENT_ID = 0;
        public static final int INDEX_CHILD_ID = 1;


        public static final String CREATE_TABLE;
        public static final String DROP_TABLE;

        static {
            String[] parameters = {
                    PARENT_ID + " INTEGER REFERENCES " + Event.TABLE + " ( "+ Event.ID +" ) ON UPDATE CASCADE ON DELETE CASCADE",
                    CHILD_ID + " INTEGER REFERENCES " + Event.TABLE + " ( "+ Event.ID +" ) ON UPDATE CASCADE ON DELETE CASCADE"
            };

            CREATE_TABLE = createTableQuery(TABLE, parameters);
            DROP_TABLE = dropTableQuery(TABLE);
        }
    }

    public static class ConversationAttendee {
        public static final String TABLE = "`conversation_attendee`";

        public static final String C_ID = "`c_id`";
        public static final String ATTENDEE_ID = "`attendee_id`";



        public static final String[] PROJECTION = {
                ConversationAttendee.C_ID,
                ConversationAttendee.ATTENDEE_ID
        };

        public static final int INDEX_C_ID = 0;
        public static final int INDEX_ATTENDEE_ID = 1;


        public static final String CREATE_TABLE;
        public static final String DROP_TABLE;

        static {
            String[] parameters = {
                    C_ID + " TEXT REFERENCES " + Conversation.TABLE + " ( "+ Conversation.C_ID +" ) ON UPDATE CASCADE ON DELETE CASCADE",
                    ATTENDEE_ID + " TEXT REFERENCES " + User.TABLE + " ( "+ User.U_ID +" ) ON UPDATE CASCADE ON DELETE CASCADE"
            };

            CREATE_TABLE = createTableQuery(TABLE, parameters);
            DROP_TABLE = dropTableQuery(TABLE);
        }
    }

    public static class RecurringEventRules {
        public static final String TABLE = "`recurring_events_rules`";

        public static final String RULE_ID = "`rule_id`";
        public static final String EVENT_ID = "`event_id`";
        public static final String START_TIME = "`start_time`";
        public static final String END_TIME = "`end_time`";
        public static final String FREQUENCY = "frequency";

        public static final String[] PROJECTION = {
                RecurringEventRules.RULE_ID,
                RecurringEventRules.EVENT_ID,
                RecurringEventRules.START_TIME,
                RecurringEventRules.END_TIME,
                RecurringEventRules.FREQUENCY
        };

        public static final int INDEX_RULE_ID = 0;
        public static final int INDEX_EVENT_ID = 1;
        public static final int INDEX_START_TIME = 2;
        public static final int INDEX_END_TIME = 3;
        public static final int INDEX_FREQUENCY = 4;

        public static final String CREATE_TABLE;
        public static final String DROP_TABLE;

        static {
            String[] parameters = {
                    RULE_ID + " TEXT PRIMARY KEY",
                    EVENT_ID + " INTEGER REFERENCES " + Event.TABLE + " ( "+ Event.ID +" ) ON UPDATE CASCADE ON DELETE CASCADE",
                    START_TIME + " REAL",
                    END_TIME + " REAL",
                    FREQUENCY + " TEXT"
            };

            CREATE_TABLE = createTableQuery(TABLE, parameters);
            DROP_TABLE = dropTableQuery(TABLE);
        }
    }

    public static class CommuteEventEndLocation {
        public static final String TABLE = "`commute_event_endLocation`";

        public static final String EVENT_ID = "`event_id`";
        public static final String LOC_ID = "`loc_id`";



        public static final String[] PROJECTION = {
                CommuteEventEndLocation.EVENT_ID,
                CommuteEventEndLocation.LOC_ID
        };

        public static final int INDEX_EVENT_ID = 0;
        public static final int INDEX_LOC_ID = 1;


        public static final String CREATE_TABLE;
        public static final String DROP_TABLE;

        static {
            String[] parameters = {
                EVENT_ID + " INTEGER REFERENCES " + Event.TABLE + " (" + Event.ID + ") ON UPDATE CASCADE ON DELETE CASCADE",
                    LOC_ID
//                LOC_ID + " INTEGER REFERENCES " + Location.TABLE + " (" + Location.ID + ")"
            };

            CREATE_TABLE = createTableQuery(TABLE, parameters);
            DROP_TABLE = dropTableQuery(TABLE);
        }
    }

    /**
     * ServerSync table contains events, conversations and messages that need to be synced with http and/or chat servers
     */
    public static class ServerSync {
        public static final String TABLE = "`server_sync`";

        public static final String ID = "`id`";
        public static final String ITEM_ID = "`item_id`"; //id of the item; use table primary key
        public static final String ITEM_TYPE = "`item_type`"; //ITEM_TYPE must be one of the types in the item type section
        public static final String SERVER = "`server`"; //server to be synced with; must be from one of the server section

        //item type section
        public static final String TYPE_EVENT = "E";
        public static final String TYPE_CONVERSATION = "C";
        public static final String TYPE_EVENT_CONVERSATION = "EC";
        public static final String TYPE_MESSAGE = "M";

        //server section
        public static final short SERVER_HTTP = 1;
        public static final short SERVER_CHAT = 2;
        public static final short SERVER_BOTH = 3;

        public static final String[] PROJECTION = {
                ServerSync.ID,
                ServerSync.ITEM_ID,
                ServerSync.ITEM_TYPE,
                ServerSync.SERVER
        };

        public static final int INDEX_ID = 0;
        public static final int INDEX_ITEM_ID = 1; //id of the item; use table primary key
        public static final int INDEX_ITEM_TYPE = 2; //ITEM_TYPE must be one of the types in the item type section
        public static final int INDEXSERVER = 3;

        public static final String CREATE_TABLE;
        public static final String DROP_TABLE;

        static {
            String[] parameters = {
                    ID + " INTEGER PRIMARY KEY AUTOINCREMENT",
                    ITEM_ID + " TEXT NOT NULL",
                    ITEM_TYPE + " TEXT NOT NULL",
                    SERVER + " INTEGER NOT NULL"
            };

            CREATE_TABLE = createTableQuery(TABLE, parameters);
            DROP_TABLE = dropTableQuery(TABLE);
        }
    }
}
