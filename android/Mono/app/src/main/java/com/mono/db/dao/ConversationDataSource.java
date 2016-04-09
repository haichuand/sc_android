package com.mono.db.dao;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;

import com.mono.db.Database;
import com.mono.db.DatabaseValues;
import com.mono.model.Attendee;
import com.mono.model.Conversation;
import com.mono.model.Message;
import com.mono.util.Common;

import java.util.ArrayList;
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
        String id = DataSource.UniqueIdGenerator(this.getClass().getSimpleName());
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

    public String getConversationFromEvent (String eventID) {
        String conversationID = null;
        try {
            Cursor cursor = database.select(DatabaseValues.EventConversation.TABLE, new String[]{"*"},
                    DatabaseValues.EventConversation.EVENT_ID + "=?", new String[]{eventID});
            if (cursor.moveToFirst()) {
                conversationID = cursor.getString(DatabaseValues.EventConversation.INDEX_C_ID);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return conversationID;
    }

    //TODO: create a conversation initialized by other user
    public String createConversationWithConversation (Conversation conversation) {
        return conversation.getId();
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

    public boolean createConversationWithConversationIdAndAttendees(String name, String eventID, String conversationID, List<String> attendeesID ) {
        if (!createConversationWithConversationIdFromEvent(name, eventID, conversationID)) {
            return false;
        }

        for(String id: attendeesID) {
            ContentValues values = new ContentValues();
            values.put(DatabaseValues.ConversationAttendee.C_ID, conversationID);
            values.put(DatabaseValues.ConversationAttendee.ATTENDEE_ID, id);

            try{
                database.insert(DatabaseValues.ConversationAttendee.TABLE, values);
            }catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        }
        return true;
    }

    public boolean createConversationWithConversationIdFromEvent(String name, String eventID, String conversationID) {
        ContentValues conversationValues = new ContentValues();
        conversationValues.put(DatabaseValues.Conversation.C_ID, conversationID);
        conversationValues.put(DatabaseValues.Conversation.C_NAME, name);

        ContentValues conversationEventValues = new ContentValues();
        conversationEventValues.put(DatabaseValues.EventConversation.C_ID, eventID);
        conversationEventValues.put(DatabaseValues.EventConversation.EVENT_ID, eventID);

        try {
            database.insert(DatabaseValues.Conversation.TABLE,conversationValues);
            database.insert(DatabaseValues.EventConversation.TABLE,conversationEventValues);
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public int clearConversationTable() {
        return database.delete(DatabaseValues.Conversation.TABLE, null, null);
    }

    public void addMessageToConversation (Message msg) {
        ContentValues values = new ContentValues();
        values.put(DatabaseValues.ConversationContent.C_ID, msg.getConversationId());
        values.put(DatabaseValues.ConversationContent.SENDER_ID, msg.getUserId());
        values.put(DatabaseValues.ConversationContent.TEXT, msg.getMessageText());
        values.put(DatabaseValues.ConversationContent.TIMESTAMP, msg.getTimestamp());

        try {
            database.insert(DatabaseValues.ConversationContent.TABLE, values);
        }catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<String> getConversationAttendeesIds (String conversationId) {
        List<String> attendeeList = new LinkedList<>();

        Cursor cursor = database.select(
                DatabaseValues.ConversationAttendee.TABLE,
                new String[]{
                        DatabaseValues.ConversationAttendee.ATTENDEE_ID
                },
                DatabaseValues.ConversationAttendee.C_ID + " = ?",
                new String[]{
                        String.valueOf(conversationId)
                }
        );

        while (cursor.moveToNext()) {
            attendeeList.add(cursor.getString(0));
        }

        cursor.close();

        return attendeeList;
    }

    public List<Message> getConversationMessages (String conversationId) {
        List<Message> messages = new ArrayList<>();

        Cursor cursor = database.select(
                DatabaseValues.ConversationContent.TABLE,
                DatabaseValues.ConversationContent.PROJECTION,
                DatabaseValues.ConversationContent.C_ID + " =?",
                new String[] {
                        String.valueOf(conversationId)
                }
        );

        while(cursor.moveToNext()) {
            String senderId = cursor.getString(DatabaseValues.ConversationContent.INDEX_SENDER_ID);
            String text = cursor.getString(DatabaseValues.ConversationContent.INDEX_TEXT);
            long timestamp = cursor.getLong(DatabaseValues.ConversationContent.INDEX_TIMESTAMP);
            Message msg = new Message(senderId, conversationId, text, timestamp);
            messages.add(msg);
        }

        cursor.close();
        return messages;
    }

    public List<Message> getConversationMessages (String conversationId, long startTime) {
        List<Message> messages = new ArrayList<>();

        Cursor cursor = database.select(
                DatabaseValues.ConversationContent.TABLE,
                DatabaseValues.ConversationContent.PROJECTION,
                DatabaseValues.ConversationContent.C_ID + " = ? AND " +
                DatabaseValues.ConversationContent.TIMESTAMP + " >= ?",
                new String[] {
                        String.valueOf(conversationId),
                        String.valueOf(startTime)
                }
        );

        while(cursor.moveToNext()) {
            String senderId = cursor.getString(DatabaseValues.ConversationContent.INDEX_SENDER_ID);
            String text = cursor.getString(DatabaseValues.ConversationContent.INDEX_TEXT);
            long timestamp = cursor.getLong(DatabaseValues.ConversationContent.INDEX_TIMESTAMP);
            Message msg = new Message(senderId, conversationId, text, timestamp);
            messages.add(msg);
        }

        cursor.close();
        return messages;

    }

    public Conversation getConversation (String conversationId) {
        String conversationName = "";
        Cursor cursor = database.select(
                DatabaseValues.Conversation.TABLE,
                new String[]{
                        DatabaseValues.Conversation.C_NAME
                },
                DatabaseValues.Conversation.C_ID + " =?",
                new String[]{
                        String.valueOf(conversationId)
                }
        );

        if(cursor.moveToNext()) {
            conversationName = cursor.getString(0);
        }

        cursor.close();

        List<Attendee> attendees = getConversationAttendees(conversationId);
        List<Message> messages = getConversationMessages(conversationId);

        Conversation conversation = new Conversation(conversationId, conversationName, attendees, messages);

        return conversation;
    }

    public List<Conversation> getConversations() {
        return getConversations(null);
    }

    public List<Conversation> getConversations(String eventId) {
        List<Conversation> conversations = new ArrayList<>();

        String[] projection = {
            "c." + DatabaseValues.Conversation.C_ID,
            "ec." + DatabaseValues.EventConversation.EVENT_ID,
            "c." + DatabaseValues.Conversation.C_NAME
        };

        String query =
            " SELECT " + Common.implode(", ", projection) +
            " FROM " + DatabaseValues.EventConversation.TABLE + " ec" +
            " INNER JOIN " + DatabaseValues.Conversation.TABLE + " c" +
            " ON ec." + DatabaseValues.EventConversation.C_ID + " = c." + DatabaseValues.Conversation.C_ID;

        String[] args = null;

        if (eventId != null) {
            query += " WHERE ec." + DatabaseValues.EventConversation.EVENT_ID + " = ?";
            args = new String[]{
                eventId
            };
        }

        query += " ORDER BY c." + DatabaseValues.Conversation.C_NAME;

        Cursor cursor = database.rawQuery(query, args);

        while (cursor.moveToNext()) {
            Conversation conversation = new Conversation(cursor.getString(0));
            conversation.eventId = cursor.getString(1);
            conversation.name = cursor.getString(2);

            conversations.add(conversation);
        }

        cursor.close();

        return conversations;
    }

    public List<Attendee> getConversationAttendees (String conversationId) {
        List<String> attendeesId = getConversationAttendeesIds(conversationId);
        List<Attendee> attendees = new ArrayList<>();

        for(String id: attendeesId) {
            Cursor cursor = database.select(
                    DatabaseValues.User.TABLE,
                    new String[]{
                            DatabaseValues.User.U_ID,
                            DatabaseValues.User.USER_NAME,
                            DatabaseValues.User.EMAIL
                    },
                    DatabaseValues.User.U_ID + " = ?",
                    new String[]{
                            String.valueOf(id)
                    }
            );

            if(cursor.moveToNext()) {
                Attendee attendee = new Attendee(cursor.getString(0), cursor.getString(1),cursor.getString(2));
                attendees.add(attendee);
            }
            cursor.close();
        }

        return attendees;
    }

    private List<String> getEventAttendeesId (String eventId) {
        List<String> attendeeList = new LinkedList<>();

        Cursor cursor = database.select(
                DatabaseValues.EventAttendee.TABLE,
                new String[]{
                        DatabaseValues.EventAttendee.ATTENDEE_ID
                },
                DatabaseValues.EventAttendee.EVENT_ID + " = ?",
                new String[]{
                        String.valueOf(eventId)
                }
        );

        while (cursor.moveToNext()) {
            attendeeList.add(cursor.getString(0));
        }

        cursor.close();

        return attendeeList;
    }

}
