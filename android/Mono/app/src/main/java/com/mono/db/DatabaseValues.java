package com.mono.db;

import com.mono.util.Common;

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
        public static final String EXTERNAL_ID = "`external_id`";
        public static final String TYPE = "`type`";
        public static final String TITLE = "`title`";
        public static final String DESC = "`description`";
        public static final String LOCATION = "`location`";
        public static final String COLOR = "`color`";
        public static final String START_TIME = "`start_time`";
        public static final String END_TIME = "`end_time`";
        public static final String CREATE_TIME = "`create_time`";

        public static final String[] PROJECTION = {
            Event.ID,
            Event.EXTERNAL_ID,
            Event.TYPE,
            Event.TITLE,
            Event.DESC,
            Event.LOCATION,
            Event.COLOR,
            Event.START_TIME,
            Event.END_TIME,
            Event.CREATE_TIME
        };

        public static final int INDEX_ID = 0;
        public static final int INDEX_EXTERNAL_ID = 1;
        public static final int INDEX_TYPE = 2;
        public static final int INDEX_TITLE = 3;
        public static final int INDEX_DESC = 4;
        public static final int INDEX_LOCATION = 5;
        public static final int INDEX_COLOR = 6;
        public static final int INDEX_START_TIME = 7;
        public static final int INDEX_END_TIME = 8;
        public static final int INDEX_CREATE_TIME = 9;

        public static final String CREATE_TABLE;
        public static final String DROP_TABLE;

        static {
            String[] parameters = {
                ID + " INTEGER PRIMARY KEY AUTOINCREMENT",
                EXTERNAL_ID + " INTEGER",
                TYPE + " TEXT",
                TITLE + " TEXT",
                DESC + " TEXT",
                LOCATION + " TEXT",
                COLOR + " INTEGER",
                START_TIME + " INTEGER",
                END_TIME + " INTEGER",
                CREATE_TIME + " INTEGER"
            };

            CREATE_TABLE = createTableQuery(TABLE, parameters);
            DROP_TABLE = dropTableQuery(TABLE);
        }
    }

    public static class User {
        public static final String TABLE = "`user`";

        public static final String U_ID = "`u_id`";
        public static final String GOOGLE_REG_ID = "`GoogleRegID`";
        public static final String MEDIA_ID = "`media_id`";
        public static final String PHONE_NUMBER = "`phone_number`";
        public static final String EMAIL = "`email`";
        public static final String FIRST_NAME = "`first_name`";
        public static final String LAST_NAME = "`last_name`";
        public static final String USER_NAME = "`user_name`";
        public static final String IS_FRIEND = "`is_friend`";

        public static final String[] PROJECTION = {
                User.U_ID,
                User.GOOGLE_REG_ID,
                User.MEDIA_ID,
                User.PHONE_NUMBER,
                User.EMAIL,
                User.FIRST_NAME,
                User.LAST_NAME,
                User.USER_NAME,
                User.IS_FRIEND
        };

        public static final int INDEX_U_ID = 0;
        public static final int INDEX_GOOGLE_REG_ID = 1;
        public static final int INDEX_MEDIA_ID = 2;
        public static final int INDEX_PHONE_NUMBER = 3;
        public static final int INDEX_EMAIL = 4;
        public static final int INDEX_FIRST_NAME = 5;
        public static final int INDEX_LAST_NAME = 6;
        public static final int INDEX_USER_NAME = 7;
        public static final int INDEX_IS_FRIEND = 8;

        public static final String CREATE_TABLE;
        public static final String DROP_TABLE;

        static {
            String[] parameters = {
                    U_ID + " INTEGER PRIMARY KEY AUTOINCREMENT",
                    GOOGLE_REG_ID + " TEXT",
                    MEDIA_ID + " INTEGER",
                    PHONE_NUMBER + " TEXT",
                    EMAIL + " TEXT",
                    FIRST_NAME + " TEXT",
                    LAST_NAME + " TEXT",
                    USER_NAME + " TEXT",
                    IS_FRIEND + " INTEGER"
            };

            CREATE_TABLE = createTableQuery(TABLE, parameters);
            DROP_TABLE = dropTableQuery(TABLE);
        }
    }

    public static class Location {
        public static final String TABLE = "`location`";

        public static final String LOC_ID = "`loc_id`";
        public static final String GOOGLE_PLACE_ID = "`GooglePlaceID`";
        public static final String NAME = "`name`";
        public static final String ADDRESS = "`address`";
        public static final String LATITUDE = "`latitude`";
        public static final String LONGITUDE = "`longitude`";
        public static final String BEEN_THERE = "`been_there`";

        public static final String[] PROJECTION = {
                Location.LOC_ID,
                Location.GOOGLE_PLACE_ID,
                Location.NAME,
                Location.ADDRESS,
                Location.LATITUDE,
                Location.LONGITUDE,
                Location.BEEN_THERE
        };

        public static final int INDEX_LOC_ID = 0;
        public static final int INDEX_GOOGLE_PALCE_ID = 1;
        public static final int INDEX_NAME = 2;
        public static final int INDEX_ADDRESS = 3;
        public static final int INDEX_LATITUDE = 4;
        public static final int INDEX_LONGITUDE = 5;
        public static final int INDEX_BEEN_THERE = 6;

        public static final String CREATE_TABLE;
        public static final String DROP_TABLE;

        static {
            String[] parameters = {
                    LOC_ID + " INTEGER PRIMARY KEY AUTOINCREMENT",
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
                    EVENT_ID + " INTEGER REFERENCES " + Event.TABLE + " ( "+ Event.ID +" )",
                    LOC_ID + " INTEGER REFERENCES " + Location.TABLE + " ( "+ Location.LOC_ID +" )"
            };

            CREATE_TABLE = createTableQuery(TABLE, parameters);
            DROP_TABLE = dropTableQuery(TABLE);
        }
    }

    public static class Conversation {
        public static final String TABLE = "`conversation`";

        public static final String C_ID = "`c_id`";
        public static final String C_NAME = "`c_name`";



        public static final String[] PROJECTION = {
                Conversation.C_ID,
                Conversation.C_NAME
        };

        public static final int INDEX_C_ID = 0;
        public static final int INDEX_C_NAME = 1;


        public static final String CREATE_TABLE;
        public static final String DROP_TABLE;

        static {
            String[] parameters = {
                    C_ID + " INTEGER PRIMARY KEY AUTOINCREMENT",
                    C_NAME + " TEXT"
            };

            CREATE_TABLE = createTableQuery(TABLE, parameters);
            DROP_TABLE = dropTableQuery(TABLE);
        }
    }

    public static class ConversationContent {
        public static final String TABLE = "`conversation_content`";

        public static final String C_ID = "`c_id`";
        public static final String SENDER_ID = "`sender_id`";
        public static final String TEXT = "`text`";
        public static final String TIMESTAMP = "`timestamp`";


        public static final String[] PROJECTION = {
                ConversationContent.C_ID,
                ConversationContent.SENDER_ID,
                ConversationContent.TEXT,
                ConversationContent.TIMESTAMP
        };

        public static final int INDEX_C_ID = 0;
        public static final int INDEX_SENDER_ID = 1;
        public static final int INDEX_TEXT = 2;
        public static final int INDEX_TIMESTAMP = 3;


        public static final String CREATE_TABLE;
        public static final String DROP_TABLE;

        static {
            String[] parameters = {
                    C_ID + " INTEGER REFERENCES " + Conversation.TABLE + " ( "+ Conversation.C_ID +" )",
                    SENDER_ID + " INTEGER REFERENCES " + User.TABLE + " ( "+ User.U_ID +" )",
                    TEXT + " TEXT",
                    TIMESTAMP + " INTEGER"
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
                    EVENT_ID + " INTEGER REFERENCES " + Event.TABLE + " ( "+ Event.ID +" )",
                    C_ID + " INTEGER REFERENCES " + Conversation.TABLE + " ( "+ Conversation.C_ID +" )"
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
                    EVENT_ID + " INTEGER REFERENCES " + Event.TABLE + " ( "+ Event.ID +" )",
                    ATTENDEE_ID + " INTEGER REFERENCES " + User.TABLE + " ( "+ User.U_ID +" )"
            };

            CREATE_TABLE = createTableQuery(TABLE, parameters);
            DROP_TABLE = dropTableQuery(TABLE);
        }
    }

    public static class Media {
        public static final String TABLE = "`media`";

        public static final String MEDIA_ID = "`media_id`";
        public static final String CONTENT = "`content`";



        public static final String[] PROJECTION = {
                Media.MEDIA_ID,
                Media.CONTENT
        };

        public static final int INDEX_MEDIA_ID = 0;
        public static final int INDEX_CONTENT = 1;


        public static final String CREATE_TABLE;
        public static final String DROP_TABLE;

        static {
            String[] parameters = {
                    MEDIA_ID + " INTEGER PRIMARY KEY AUTOINCREMENT",
                    CONTENT + " BLOB"
            };

            CREATE_TABLE = createTableQuery(TABLE, parameters);
            DROP_TABLE = dropTableQuery(TABLE);
        }
    }

    public static class EventMedia {
        public static final String TABLE = "`event_media`";

        public static final String E_ID = "`e_id`";
        public static final String M_ID = "`m_id`";



        public static final String[] PROJECTION = {
                EventMedia.E_ID,
                EventMedia.M_ID
        };

        public static final int INDEX_E_ID = 0;
        public static final int INDEX_M_ID = 1;


        public static final String CREATE_TABLE;
        public static final String DROP_TABLE;

        static {
            String[] parameters = {
                    E_ID + " INTEGER REFERENCES " + Event.TABLE + " ( "+ Event.ID +" )",
                    M_ID + " INTEGER REFERENCES " + Media.TABLE + " ( "+ Media.MEDIA_ID +" )"
            };

            CREATE_TABLE = createTableQuery(TABLE, parameters);
            DROP_TABLE = dropTableQuery(TABLE);
        }
    }

    public static class Alarm {
        public static final String TABLE = "`alarm`";

        public static final String ALARM_ID = "`alarm_id`";
        public static final String ALARM_TIME = "`alarm_time`";
        public static final String CREATE_TIME = "`create_time`";
        public static final String ENABLE = "`enable`";


        public static final String[] PROJECTION = {
                Alarm.ALARM_ID,
                Alarm.ALARM_TIME,
                Alarm.CREATE_TIME,
                Alarm.ENABLE
        };

        public static final int INDEX_ALARM_ID = 0;
        public static final int INDEX_ALARM_TIME = 1;
        public static final int INDEX_CREATE_TIME = 2;
        public static final int INDEX_ENABLE = 3;


        public static final String CREATE_TABLE;
        public static final String DROP_TABLE;

        static {
            String[] parameters = {
                    ALARM_ID + " INTEGER PRIMARY KEY",
                    ALARM_TIME + " INTEGER",
                    CREATE_TIME + " INTEGER",
                    ENABLE + " INTEGER"
            };

            CREATE_TABLE = createTableQuery(TABLE, parameters);
            DROP_TABLE = dropTableQuery(TABLE);
        }
    }

    public static class EventAlarm {
        public static final String TABLE = "`event_alarm`";

        public static final String E_ID = "`e_id`";
        public static final String A_ID = "`a_id`";



        public static final String[] PROJECTION = {
                EventAlarm.E_ID,
                EventAlarm.A_ID
        };

        public static final int INDEX_E_ID = 0;
        public static final int INDEX_A_ID = 1;


        public static final String CREATE_TABLE;
        public static final String DROP_TABLE;

        static {
            String[] parameters = {
                    E_ID + " INTEGER REFERENCES " + Event.TABLE + " ( "+ Event.ID +" )",
                    A_ID + " INTEGER REFERENCES " + Alarm.TABLE + " ( "+ Alarm.ALARM_ID +" )"
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
                    PARENT_ID + " INTEGER REFERENCES " + Event.TABLE + " ( "+ Event.ID +" )",
                    CHILD_ID + " INTEGER REFERENCES " + Event.TABLE + " ( "+ Event.ID +" )"
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
                    C_ID + " INTEGER REFERENCES " + Conversation.TABLE + " ( "+ Conversation.C_ID +" )",
                    ATTENDEE_ID + " INTEGER REFERENCES " + User.TABLE + " ( "+ User.U_ID +" )"
            };

            CREATE_TABLE = createTableQuery(TABLE, parameters);
            DROP_TABLE = dropTableQuery(TABLE);
        }
    }
}
