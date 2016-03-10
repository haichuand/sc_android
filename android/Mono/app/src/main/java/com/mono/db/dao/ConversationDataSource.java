package com.mono.db.dao;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;

import com.mono.db.Database;
import com.mono.db.DatabaseValues;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by xuejing on 3/9/16.
 */
public class ConversationDataSource extends DataSource{
    private ConversationDataSource(Database database) {
        super(database);
    }

    /*
    * Create a conversation from an event with
    * Params: name of the conversation, the id of associated event
    * */
    public String createConversationFromEvent (String name, String eventID) {
        String id = DataSource.UniqueIdGenerator(this.getClass().getName());
        ContentValues conversationValues = new ContentValues();
        conversationValues.put(DatabaseValues.Conversation.C_ID, id);
        conversationValues.put(DatabaseValues.Conversation.C_NAME, name);

        ContentValues conversationEventValues = new ContentValues();
        conversationEventValues.put(DatabaseValues.EventConversation.C_ID, id);
        conversationEventValues.put(DatabaseValues.EventConversation.EVENT_ID, eventID);

        try {
            database.insert(DatabaseValues.Conversation.TABLE,conversationValues);
            database.insert(DatabaseValues.EventConversation.TABLE,conversationEventValues);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return id;
    }

    public String createConversationWithSelectedAttendees (String name, String eventID, List<String> attendeesID) {
        String conversationId = createConversationFromEvent(name, eventID);

        for(String id: attendeesID) {
            ContentValues values = new ContentValues();
            values.put(DatabaseValues.ConversationAttendee.C_ID, conversationId);
            values.put(DatabaseValues.ConversationAttendee.ATTENDEE_ID, id);

            try{
                database.insert(DatabaseValues.ConversationAttendee.TABLE, values);
            }catch (SQLException e) {
                e.printStackTrace();
            }
        }

        return conversationId;
    }


    public List<String> getConversationAttendeesIds (String conversationId) {
        List<String> attendeeList = new LinkedList<>();

        Cursor cursor = database.select(
                DatabaseValues.ConversationAttendee.TABLE,
                DatabaseValues.ConversationAttendee.PROJECTION,
                DatabaseValues.ConversationAttendee.C_ID + " = ?",
                new String[]{
                        String.valueOf(conversationId)
                }
        );

        while (cursor.moveToNext()) {
            attendeeList.add(cursor.getString(DatabaseValues.ConversationAttendee.INDEX_ATTENDEE_ID));
        }

        cursor.close();

        return attendeeList;
    }

    private List<String> getEventAttendees (String eventId) {
        List<String> attendeeList = new LinkedList<>();

        Cursor cursor = database.select(
                DatabaseValues.EventAttendee.TABLE,
                DatabaseValues.EventAttendee.PROJECTION,
                DatabaseValues.EventAttendee.EVENT_ID + " = ?",
                new String[]{
                        String.valueOf(eventId)
                }
        );

        while (cursor.moveToNext()) {
            attendeeList.add(cursor.getString(DatabaseValues.EventAttendee.INDEX_ATTENDEE_ID));
        }

        cursor.close();

        return attendeeList;
    }

}
