package com.mono.db.dao;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;

import com.mono.db.Database;
import com.mono.db.DatabaseValues;
import com.mono.model.Alarm;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by xuejing on 3/11/16.
 */
public class AlarmDataSource extends  DataSource{
    private AlarmDataSource(Database database) {
        super(database);
    }

    public String createAlarmForEvent(String eventId, long alarmTime, long createTime, boolean enabled) {
        String alarmId = DataSource.UniqueIdGenerator(this.getClass().getSimpleName());
        int enabledInt = enabled ? 1 : 0; //1: enabled; 0: disabled

        ContentValues alarmValues = new ContentValues();
        alarmValues.put(DatabaseValues.Alarm.ALARM_ID, alarmId);
        alarmValues.put(DatabaseValues.Alarm.ALARM_TIME, alarmTime);
        alarmValues.put(DatabaseValues.Alarm.CREATE_TIME, createTime);
        alarmValues.put(DatabaseValues.Alarm.ENABLE, enabledInt);

        try {
            database.insert(DatabaseValues.Alarm.TABLE, alarmValues);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        ContentValues eventAlarmValues = new ContentValues();
        eventAlarmValues.put(DatabaseValues.EventAlarm.E_ID, eventId);
        eventAlarmValues.put(DatabaseValues.EventAlarm.A_ID, alarmId);

        try {
            database.insert(DatabaseValues.EventAlarm.TABLE, eventAlarmValues);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return alarmId;
    }

    public void disableAlarm (String alarmId) {
        ContentValues values = new ContentValues();
        values.put(DatabaseValues.Alarm.ENABLE, 0);
        int rowAffected = -1;
        try {
            rowAffected = database.update(
                    DatabaseValues.Alarm.TABLE,
                    values,
                    DatabaseValues.Alarm.ALARM_ID + " = ? ",
                    new String[] {
                            String.valueOf(alarmId)
                    }
            );
        } catch (SQLException e) {
            e.printStackTrace();
        }
        System.out.println("disable alarm, row Affected: " + rowAffected);
    }

    public void enableAlarm (String alarmId) {
        ContentValues values = new ContentValues();
        values.put(DatabaseValues.Alarm.ENABLE, 1);
        int rowAffected = -1;
        try {
            rowAffected = database.update(
                    DatabaseValues.Alarm.TABLE,
                    values,
                    DatabaseValues.Alarm.ALARM_ID + " = ? ",
                    new String[] {
                            String.valueOf(alarmId)
                    }
            );
        } catch (SQLException e) {
            e.printStackTrace();
        }
        System.out.println("enable alarm, row Affected: " + rowAffected);
    }

    public Alarm getAlarmById (String alarmId) {
        Alarm alarm = null;
        Cursor cursor = database.select(
                DatabaseValues.Alarm.TABLE,
                DatabaseValues.Alarm.PROJECTION,
                DatabaseValues.Alarm.ALARM_ID + " =?",
                new String[]{
                        String.valueOf(alarmId)
                }
        );
        if(cursor.moveToNext()) {
            boolean enabled = cursor.getInt(DatabaseValues.Alarm.INDEX_ENABLE) == 1 ?  true: false;
            alarm = new Alarm(cursor.getString(DatabaseValues.Alarm.INDEX_ALARM_ID),
                    cursor.getLong(DatabaseValues.Alarm.INDEX_ALARM_TIME),
                    cursor.getLong(DatabaseValues.Alarm.INDEX_CREATE_TIME),
                    enabled);
        }
        return alarm;
    }

    public List<Alarm> getAlarmByEventId (String eventId) {
        List<String> alarmIds = new ArrayList<>();
        List<Alarm> alarmList = new ArrayList<>();
        Cursor cursor = database.select(
                DatabaseValues.EventAlarm.TABLE,
                new String[]{
                        DatabaseValues.EventAlarm.A_ID
                },
                DatabaseValues.EventAlarm.E_ID + " = ? ",
                new String[]{
                        String.valueOf(eventId)
                }
        );

        while(cursor.moveToNext()) {
            alarmIds.add(cursor.getString(0));
        }

        if(!alarmIds.isEmpty()) {
            for(String alarmId: alarmIds) {
                Alarm alarm = getAlarmById(alarmId);
                if(alarm != null)
                    alarmList.add(alarm);
            }
        }
        return alarmList;
    }

}
