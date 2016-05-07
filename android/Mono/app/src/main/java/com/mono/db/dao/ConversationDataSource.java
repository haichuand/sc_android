package com.mono.db.dao;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;

import com.mono.db.Database;
import com.mono.db.DatabaseValues;
import com.mono.model.Attendee;
import com.mono.model.Conversation;
import com.mono.model.Event;
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
    public String createConversationFromEvent (String name, Event event, String creatorId) {
        String id = DataSource.UniqueIdGenerator(this.getClass().getSimpleName());
        ContentValues conversationValues = new ContentValues();
        conversationValues.put(DatabaseValues.Conversation.C_ID, id);
        conversationValues.put(DatabaseValues.Conversation.C_NAME, name);
        conversationValues.put(DatabaseValues.Conversation.C_CREATOR, creatorId);

        ContentValues conversationEventValues = new ContentValues();
        conversationEventValues.put(DatabaseValues.EventConversation.C_ID, id);
        conversationEventValues.put(DatabaseValues.EventConversation.EVENT_ID, event.id);

        ArrayList<String> attendeeIds = new ArrayList<>();
        for (Attendee attendee : event.attendees) {
            attendeeIds.add(attendee.id);
        }
        try {
            database.insert(DatabaseValues.Conversation.TABLE,conversationValues);
            database.insert(DatabaseValues.EventConversation.TABLE,conversationEventValues);
            addAttendeesToConversation(id, attendeeIds);
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

    public String createConversationWithSelectedAttendees (String name, Event event, List<String> attendeesID, String creatorId) {
        String conversationId = createConversationFromEvent(name, event, creatorId);

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

    public boolean createConversation(String conversationID, String creatorId, String name, List<String> attendeesID ) {
        try {
            ContentValues values = new ContentValues();
            values.put(DatabaseValues.Conversation.C_ID, conversationID);
            values.put(DatabaseValues.Conversation.C_NAME, name);
            values.put(DatabaseValues.Conversation.C_CREATOR, creatorId);
            database.insert(DatabaseValues.Conversation.TABLE, values);

            for(String id: attendeesID) {
                values = new ContentValues();
                values.put(DatabaseValues.ConversationAttendee.C_ID, conversationID);
                values.put(DatabaseValues.ConversationAttendee.ATTENDEE_ID, id);
                database.insert(DatabaseValues.ConversationAttendee.TABLE, values);
            }
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean setConversationSyncNeeded(String conversationId, boolean isSyncNeeded) {
        ContentValues values = new ContentValues();
        values.put(DatabaseValues.Conversation.C_SYNC_NEEDED, isSyncNeeded ? 1 : 0);
        if (database.update(DatabaseValues.Conversation.TABLE, values, DatabaseValues.Conversation.C_ID + "=" + conversationId, null) == 1) {
            return true;
        }
        return false;
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

    public int clearConversationAttendees(String conversationId) {
        return database.delete(DatabaseValues.ConversationAttendee.TABLE, DatabaseValues.ConversationAttendee.C_ID + "='" + conversationId + "'", null);
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

    public void addAttendeeToConversation(String conversationId, String attendeeId) {
        ContentValues values = new ContentValues();
        values.put(DatabaseValues.ConversationAttendee.C_ID, conversationId);
        values.put(DatabaseValues.ConversationAttendee.ATTENDEE_ID, attendeeId);
        try {
            database.insert(DatabaseValues.ConversationAttendee.TABLE, values);
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public void addAttendeesToConversation(String conversationId, List<String> attendeeIds) {
        ContentValues values = new ContentValues();
        values.put(DatabaseValues.ConversationAttendee.C_ID, conversationId);
        try {
            for (String id : attendeeIds) {
                values.put(DatabaseValues.ConversationAttendee.ATTENDEE_ID, id);
                database.insert(DatabaseValues.ConversationAttendee.TABLE, values);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
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
        String conversationName = "", creatorId = "";
        Cursor cursor = database.select(
                DatabaseValues.Conversation.TABLE,
                new String[]{
                        DatabaseValues.Conversation.C_NAME,
                        DatabaseValues.Conversation.C_CREATOR
                },
                DatabaseValues.Conversation.C_ID + " =?",
                new String[]{
                        String.valueOf(conversationId)
                }
        );

        if(cursor.moveToNext()) {
            conversationName = cursor.getString(0);
            creatorId = cursor.getString(1);
        }

        cursor.close();

        List<Attendee> attendees = getConversationAttendees(conversationId);
        List<Message> messages = getConversationMessages(conversationId);

        return new Conversation(conversationId, creatorId, conversationName, attendees, messages);
    }

    public List<Conversation> getConversations() {
        return getConversations(null);
    }

    public List<Conversation> getConversations(String eventId) {
        List<Conversation> conversations = new ArrayList<>();

        String[] projection = {
            "c." + DatabaseValues.Conversation.C_ID,
            "ec." + DatabaseValues.EventConversation.EVENT_ID,
            "c." + DatabaseValues.Conversation.C_NAME,
                "c." + DatabaseValues.Conversation.C_CREATOR
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
            conversation.creatorId = cursor.getString(3);

            conversations.add(conversation);
        }

        cursor.close();

        return conversations;
    }

    /**
     * Get all conversations in database, including those that do not associate with an event
     * @return
     */
    public List<Conversation> getAllConversations() {
        List<Conversation> conversations = new ArrayList<>();

        String[] projection = {
                DatabaseValues.Conversation.C_ID,
                DatabaseValues.Conversation.C_NAME,
                DatabaseValues.Conversation.C_CREATOR
        };

        String query =
                " SELECT " + Common.implode(", ", projection) +
                        " FROM " + DatabaseValues.Conversation.TABLE +
                        " ORDER BY " + DatabaseValues.Conversation.C_ID + " DESC";

        Cursor cursor = database.rawQuery(query, null);

        while (cursor.moveToNext()) {
            Conversation conversation = new Conversation(cursor.getString(0));
            conversation.name = cursor.getString(1);
            conversation.creatorId = cursor.getString(2);

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
                            DatabaseValues.User.MEDIA_ID,
                            DatabaseValues.User.EMAIL,
                            DatabaseValues.User.PHONE_NUMBER,
                            DatabaseValues.User.FIRST_NAME,
                            DatabaseValues.User.LAST_NAME,
                            DatabaseValues.User.USER_NAME,
                            DatabaseValues.User.IS_FRIEND
                    },
                    DatabaseValues.User.U_ID + " = ?",
                    new String[]{
                            String.valueOf(id)
                    }
            );

            if(cursor.moveToNext()) {
                Attendee attendee = new Attendee(cursor.getString(0), cursor.getString(1), cursor.getString(2),
                        cursor.getString(3), cursor.getString(4), cursor.getString(5), cursor.getString(6), true);
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
