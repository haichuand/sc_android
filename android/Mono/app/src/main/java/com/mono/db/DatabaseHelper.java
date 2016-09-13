package com.mono.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.mono.db.dao.DataSource;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

/**
 * This class is used to manage the database by performing a series of create table queries.
 * References to all database objects will be cached here for efficiency purposes.
 *
 * @author Gary Ng
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "database.db";
    public static final int DATABASE_VERSION = 1;

    private static DatabaseHelper instance;

    private SQLiteDatabase database;
    private Database wrapper;

    private final Map<Class, DataSource> dataSources = new HashMap<>();

    private DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public static DatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseHelper(context);
        }
        return instance;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        executeQueries(db, new String[]{
            DatabaseValues.Event.CREATE_TABLE,
            DatabaseValues.User.CREATE_TABLE,
            DatabaseValues.Conversation.CREATE_TABLE,
            DatabaseValues.Media.CREATE_TABLE,
            DatabaseValues.Location.CREATE_TABLE,
            DatabaseValues.EventConversation.CREATE_TABLE,
            DatabaseValues.ConversationContent.CREATE_TABLE,
            DatabaseValues.ConversationAttachments.CREATE_TABLE,
            DatabaseValues.EventLocationCandidates.CREATE_TABLE,
            DatabaseValues.EventAttendee.CREATE_TABLE,
            DatabaseValues.EventMedia.CREATE_TABLE,
            DatabaseValues.Alarm.CREATE_TABLE,
            DatabaseValues.EventAlarm.CREATE_TABLE,
            DatabaseValues.ConversationAttendee.CREATE_TABLE,
            DatabaseValues.RecurringEvent.CREATE_TABLE,
            DatabaseValues.RecurringEventRules.CREATE_TABLE,
            DatabaseValues.CommuteEventEndLocation.CREATE_TABLE
        });
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        executeQueries(db, new String[]{
            DatabaseValues.Event.DROP_TABLE,
            DatabaseValues.User.DROP_TABLE,
            DatabaseValues.Conversation.DROP_TABLE,
            DatabaseValues.Media.DROP_TABLE,
            DatabaseValues.Location.DROP_TABLE,
            DatabaseValues.EventConversation.DROP_TABLE,
            DatabaseValues.ConversationContent.DROP_TABLE,
            DatabaseValues.ConversationAttachments.DROP_TABLE,
            DatabaseValues.EventLocationCandidates.DROP_TABLE,
            DatabaseValues.EventAttendee.DROP_TABLE,
            DatabaseValues.EventMedia.DROP_TABLE,
            DatabaseValues.Alarm.DROP_TABLE,
            DatabaseValues.EventAlarm.DROP_TABLE,
            DatabaseValues.ConversationAttendee.DROP_TABLE,
            DatabaseValues.RecurringEvent.DROP_TABLE,
            DatabaseValues.RecurringEventRules.DROP_TABLE,
            DatabaseValues.CommuteEventEndLocation.DROP_TABLE
        });

        onCreate(db);
    }

    private void executeQueries(SQLiteDatabase db, String[] queries) {
        for (String query : queries) {
            db.execSQL(query);
        }
    }

    /**
     * Connect to the existing database.
     *
     * @return a reference of the database for further transactions.
     */
    public Database connect() {
        if (database == null || !database.isOpen()) {
            database = getWritableDatabase();
            wrapper = new Database(database);
        }

        return wrapper;
    }

    /**
     * Close database connection.
     */
    public void close() {
        if (database != null && database.isOpen()) {
            database.close();
        }

        dataSources.clear();
    }

    /**
     * Retrieve database access object from cache.
     *
     * @param key Database access object class.
     * @param <T> Class type.
     * @return the database access object.
     */
    private <T extends DataSource> T getDataSource(Class<T> key) {
        T dataSource = null;

        if (dataSources.containsKey(key)) {
            dataSource = key.cast(dataSources.get(key));
        } else {
            try {
                Constructor<T> constructor = key.getDeclaredConstructor(Database.class);
                constructor.setAccessible(true);

                dataSource = constructor.newInstance(connect());
                dataSources.put(key, dataSource);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return dataSource;
    }

    /**
     * Retrieve database access object from cache.
     *
     * @param context Context of the application.
     * @param key Database access object class.
     * @param <T> Class type.
     * @return the database access object.
     */
    public static <T extends DataSource> T getDataSource(Context context, Class<T> key) {
        return getInstance(context).getDataSource(key);
    }
}
