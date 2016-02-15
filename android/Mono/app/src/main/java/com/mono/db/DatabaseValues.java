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
        public static final int INDEX_TITLE = 2;
        public static final int INDEX_DESC = 3;
        public static final int INDEX_LOCATION = 4;
        public static final int INDEX_COLOR = 5;
        public static final int INDEX_START_TIME = 6;
        public static final int INDEX_END_TIME = 7;
        public static final int INDEX_CREATE_TIME = 8;

        public static final String CREATE_TABLE;
        public static final String DROP_TABLE;

        static {
            String[] parameters = {
                ID + " INTEGER PRIMARY KEY AUTOINCREMENT",
                EXTERNAL_ID + " INTEGER",
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
}
