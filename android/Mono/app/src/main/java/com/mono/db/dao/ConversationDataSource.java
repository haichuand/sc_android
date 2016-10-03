package com.mono.db.dao;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.net.Uri;

import com.mono.db.Database;
import com.mono.db.DatabaseValues;
import com.mono.model.Attendee;
import com.mono.model.Conversation;
import com.mono.model.Media;
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
    public boolean createEventConversation(String eventId, String conversationId, String name, String creatorId, List<String> conversationAttendeesId, boolean syncNeeded) {
        ContentValues conversationValues = new ContentValues();
        conversationValues.put(DatabaseValues.Conversation.C_ID, conversationId);
        conversationValues.put(DatabaseValues.Conversation.C_NAME, name);
        conversationValues.put(DatabaseValues.Conversation.C_CREATOR, creatorId);
        conversationValues.put(DatabaseValues.Conversation.ACK, syncNeeded ? 1 : 0);

        ContentValues conversationEventValues = new ContentValues();
        conversationEventValues.put(DatabaseValues.EventConversation.C_ID, conversationId);
        conversationEventValues.put(DatabaseValues.EventConversation.EVENT_ID, eventId);

        ContentValues convAttendeeValues = new ContentValues();
        convAttendeeValues.put(DatabaseValues.ConversationAttendee.C_ID, conversationId);

        try {
            database.insert(DatabaseValues.Conversation.TABLE,conversationValues);
            database.insert(DatabaseValues.EventConversation.TABLE,conversationEventValues);
            for (String id : conversationAttendeesId) {
                convAttendeeValues.put(DatabaseValues.ConversationAttendee.ATTENDEE_ID, id);
                database.insert(DatabaseValues.ConversationAttendee.TABLE, convAttendeeValues);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

//    public String getConversationFromEvent (String eventID) {
//        String conversationID = null;
//        try {
//            Cursor cursor = database.select(DatabaseValues.EventConversation.TABLE, new String[]{"*"},
//                    DatabaseValues.EventConversation.EVENT_ID + "=?", new String[]{eventID});
//            if (cursor.moveToFirst()) {
//                conversationID = cursor.getString(DatabaseValues.EventConversation.INDEX_C_ID);
//            }
//        } catch (SQLException e) {
//            e.printStackTrace();
//        }
//
//        return conversationID;
//    }

//    /**
//     * Does not copy event attendees to conversation. Conversation attendees only come from attendeesID list
//     * @param name
//     * @param eventId
//     * @param attendeesID
//     * @param creatorId
//     * @return
//     */
//    public String createConversationWithSelectedAttendees (String name, String eventId, List<String> attendeesID, String creatorId) {
//        String conversationId = getUniqueConversationId();
//        createConversationGivenId(conversationId, name, eventId, attendeesID, creatorId);
//        return conversationId;
//    }

    public String getUniqueConversationId () {
        return DataSource.UniqueIdGenerator(this.getClass().getSimpleName());
    }

//    public void createConversationGivenId (String conversationId, String name, String eventId, List<String> attendeesID, String creatorId) {
//        ContentValues conversationValues = new ContentValues();
//        conversationValues.put(DatabaseValues.Conversation.C_ID, conversationId);
//        conversationValues.put(DatabaseValues.Conversation.C_NAME, name);
//        conversationValues.put(DatabaseValues.Conversation.C_CREATOR, creatorId);
//
//        ContentValues conversationEventValues = new ContentValues();
//        conversationEventValues.put(DatabaseValues.EventConversation.C_ID, conversationId);
//        conversationEventValues.put(DatabaseValues.EventConversation.EVENT_ID, eventId);
//
//        try {
//            database.insert(DatabaseValues.Conversation.TABLE, conversationValues);
//            database.insert(DatabaseValues.EventConversation.TABLE, conversationEventValues);
//            for(String attendeeId: attendeesID) {
//                ContentValues values = new ContentValues();
//                values.put(DatabaseValues.ConversationAttendee.C_ID, conversationId);
//                values.put(DatabaseValues.ConversationAttendee.ATTENDEE_ID, attendeeId);
//                database.insert(DatabaseValues.ConversationAttendee.TABLE, values);
//            }
//        } catch (SQLException e) {
//            e.printStackTrace();
//        }
//    }

//    public boolean createConversation(String conversationID, String creatorId, String name, List<String> attendeesID ) {
//        try {
//            ContentValues values = new ContentValues();
//            values.put(DatabaseValues.Conversation.C_ID, conversationID);
//            values.put(DatabaseValues.Conversation.C_NAME, name);
//            values.put(DatabaseValues.Conversation.C_CREATOR, creatorId);
//            database.insert(DatabaseValues.Conversation.TABLE, values);
//
//            for(String id: attendeesID) {
//                values = new ContentValues();
//                values.put(DatabaseValues.ConversationAttendee.C_ID, conversationID);
//                values.put(DatabaseValues.ConversationAttendee.ATTENDEE_ID, id);
//                database.insert(DatabaseValues.ConversationAttendee.TABLE, values);
//            }
//            return true;
//        } catch (SQLException e) {
//            e.printStackTrace();
//        }
//        return false;
//    }

    public boolean setConversationSyncNeeded(String conversationId, boolean isSyncNeeded) {
        ContentValues values = new ContentValues();
        values.put(DatabaseValues.Conversation.ACK, isSyncNeeded ? 1 : 0);
        return database.update(DatabaseValues.Conversation.TABLE, values, DatabaseValues.Conversation.C_ID + "='" + conversationId + "'", null) == 1;
    }

    public boolean setConversationMessageAckAndTimestamp (long messageId, boolean ack, long timestamp) {
        ContentValues values = new ContentValues();
        values.put(DatabaseValues.ConversationContent.ACK, ack ? 1 : 0);
        values.put(DatabaseValues.ConversationContent.TIMESTAMP, timestamp);
        return database.update(DatabaseValues.ConversationContent.TABLE, values, DatabaseValues.ConversationContent.ID + "=" + messageId, null) == 1;
    }

//    public boolean createConversationWithConversationIdFromEvent(String name, String eventID, String conversationID) {
//        ContentValues conversationValues = new ContentValues();
//        conversationValues.put(DatabaseValues.Conversation.C_ID, conversationID);
//        conversationValues.put(DatabaseValues.Conversation.C_NAME, name);
//
//        ContentValues conversationEventValues = new ContentValues();
//        conversationEventValues.put(DatabaseValues.EventConversation.C_ID, eventID);
//        conversationEventValues.put(DatabaseValues.EventConversation.EVENT_ID, eventID);
//
//        try {
//            database.insert(DatabaseValues.Conversation.TABLE,conversationValues);
//            database.insert(DatabaseValues.EventConversation.TABLE,conversationEventValues);
//        } catch (SQLException e) {
//            e.printStackTrace();
//            return false;
//        }
//        return true;
//    }

//    public int clearConversationTable() {
//        return database.delete(DatabaseValues.Conversation.TABLE, null, null);
//    }

    public int clearConversationAttendees(String conversationId) {
        return database.delete(DatabaseValues.ConversationAttendee.TABLE, DatabaseValues.ConversationAttendee.C_ID + "='" + conversationId + "'", null);
    }

    /**
     * Saves a conversation message to database
     * @param msg
     * @return Database message id, or -1 if unsuccessful
     */
    public long addMessageToConversation (Message msg) {
        ContentValues values = new ContentValues();
        values.put(DatabaseValues.ConversationContent.C_ID, msg.getConversationId());
        values.put(DatabaseValues.ConversationContent.SENDER_ID, msg.getSenderId());
        values.put(DatabaseValues.ConversationContent.TEXT, msg.getMessageText());
        values.put(DatabaseValues.ConversationContent.TIMESTAMP, msg.getTimestamp());
        values.put(DatabaseValues.ConversationContent.ACK, msg.ack ? 1 : 0);

        try {
            long id = database.insert(DatabaseValues.ConversationContent.TABLE, values);
            if (msg.attachments != null) {
                setMessageAttachments(id, msg.attachments);
            }
            return id;
        }catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
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

    public void dropAttendeesFromConversation (String conversationId, List<String> userIds) {
        String where = DatabaseValues.ConversationAttendee.C_ID + "='"+ conversationId + "'";
        try {
            for (String id : userIds) {
                database.delete(DatabaseValues.ConversationAttendee.TABLE,
                        where + " AND " + DatabaseValues.ConversationAttendee.ATTENDEE_ID + "='" + id + "'", null);
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
                },
                DatabaseValues.ConversationContent.TIMESTAMP
        );

        while(cursor.moveToNext()) {
            long messageId = cursor.getLong(DatabaseValues.ConversationContent.INDEX_ID);
            String senderId = cursor.getString(DatabaseValues.ConversationContent.INDEX_SENDER_ID);
            String text = cursor.getString(DatabaseValues.ConversationContent.INDEX_TEXT);
            long timestamp = cursor.getLong(DatabaseValues.ConversationContent.INDEX_TIMESTAMP);
            boolean ack = cursor.getInt(DatabaseValues.ConversationContent.INDEX_ACK) == 1;

            Message msg = new Message(senderId, conversationId, text, timestamp);
            msg.ack = ack;
            msg.setMessageId(messageId);
            msg.attachments = getMessageAttachments(msg.getMessageId());
            messages.add(msg);
        }

        cursor.close();
        return messages;
    }

    /**
     * Check if the message ack flag has been set to true.
     * Also returns true if the message is not in ConversationContent table
     * @param messageId
     * @return
     */
    public boolean isMessageAcked (String messageId) {

        Cursor cursor = database.select(
                DatabaseValues.ConversationContent.TABLE,
                new String[]{DatabaseValues.ConversationContent.ACK},
                DatabaseValues.ConversationContent.ID + "=?",
                new String[]{messageId}
        );

        if (!cursor.moveToFirst() || cursor.getInt(0) == 1) {
            cursor.close();
            return true;
        }
        cursor.close();
        return false;
    }

    /**
     * Get a Message by its database message id (primary key)
     * @param messageId
     * @return
     */
    public Message getMessageByMessageId (String messageId) {
        Message message = null;

        Cursor cursor = database.select(
                DatabaseValues.ConversationContent.TABLE,
                DatabaseValues.ConversationContent.PROJECTION,
                DatabaseValues.ConversationContent.ID + "=?",
                new String[] { messageId }
        );

        if (cursor.moveToFirst()) {
            message = new Message(
                    cursor.getString(DatabaseValues.ConversationContent.INDEX_SENDER_ID),
                    cursor.getString(DatabaseValues.ConversationContent.INDEX_C_ID),
                    cursor.getString(DatabaseValues.ConversationContent.INDEX_TEXT),
                    cursor.getLong(DatabaseValues.ConversationContent.INDEX_TIMESTAMP)
            );
            message.setMessageId(cursor.getLong(DatabaseValues.ConversationContent.INDEX_ID));
            message.ack = cursor.getInt(DatabaseValues.ConversationContent.INDEX_ACK) == 1;
            message.attachments = getMessageAttachments(message.getMessageId());
        }

        cursor.close();
        return message;
    }

//    public List<Message> getConversationMessages (String conversationId, long startTime) {
//        List<Message> messages = new ArrayList<>();
//
//        Cursor cursor = database.select(
//                DatabaseValues.ConversationContent.TABLE,
//                DatabaseValues.ConversationContent.PROJECTION,
//                DatabaseValues.ConversationContent.C_ID + " = ? AND " +
//                DatabaseValues.ConversationContent.TIMESTAMP + " >= ?",
//                new String[] {
//                        String.valueOf(conversationId),
//                        String.valueOf(startTime)
//                }
//        );
//
//        while(cursor.moveToNext()) {
//            String senderId = cursor.getString(DatabaseValues.ConversationContent.INDEX_SENDER_ID);
//            String text = cursor.getString(DatabaseValues.ConversationContent.INDEX_TEXT);
//            long timestamp = cursor.getLong(DatabaseValues.ConversationContent.INDEX_TIMESTAMP);
//            Message msg = new Message(senderId, conversationId, text, timestamp);
//            messages.add(msg);
//        }
//
//        cursor.close();
//        return messages;
//
//    }

    public Conversation getConversation(String conversationId, boolean getAttendees, boolean getMessages) {
        String conversationName = "", creatorId = "", eventId = "";
        boolean syncNeeded = false;
        Cursor cursor = database.select(
                DatabaseValues.Conversation.TABLE,
                new String[]{
                        DatabaseValues.Conversation.C_NAME,
                        DatabaseValues.Conversation.C_CREATOR,
                        DatabaseValues.Conversation.ACK
                },
                DatabaseValues.Conversation.C_ID + " =?",
                new String[]{
                        conversationId
                }
        );

        if(cursor.moveToNext()) {
            conversationName = cursor.getString(0);
            creatorId = cursor.getString(1);
            syncNeeded = cursor.getInt(2) == 1;
        }

        cursor = database.select(
                DatabaseValues.EventConversation.TABLE,
                new String[] {
                        DatabaseValues.EventConversation.EVENT_ID
                },
                DatabaseValues.EventConversation.C_ID + "=?",
                new String[] {
                        conversationId
                }
        );
        if (cursor.moveToFirst()) {
            eventId = cursor.getString(0);
        }
        cursor.close();

        List<Attendee> attendees = null;
        if (getAttendees) {
            attendees = getConversationAttendees(conversationId);
        }

        List<Message> messages = null;
        if (getMessages) {
            messages = getConversationMessages(conversationId);
        }

        return new Conversation(conversationId, eventId, creatorId, conversationName, attendees, messages, syncNeeded);
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
                "c." + DatabaseValues.Conversation.C_CREATOR,
                "c." + DatabaseValues.Conversation.ACK
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
            conversation.syncNeeded = cursor.getInt(4) == 1;
            conversations.add(conversation);
        }

        cursor.close();

        return conversations;
    }

//    /**
//     * Get all conversations in database, including those that do not associate with an event
//     * @return
//     */
//    public List<Conversation> getAllConversations() {
//        List<Conversation> conversations = new ArrayList<>();
//
//        String[] projection = {
//                DatabaseValues.Conversation.C_ID,
//                DatabaseValues.Conversation.C_NAME,
//                DatabaseValues.Conversation.C_CREATOR,
//        };
//
//        String query =
//                " SELECT " + Common.implode(", ", projection) +
//                        " FROM " + DatabaseValues.Conversation.TABLE +
//                        " ORDER BY " + DatabaseValues.Conversation.C_ID + " DESC";
//
//        Cursor cursor = database.rawQuery(query, null);
//
//        while (cursor.moveToNext()) {
//            Conversation conversation = new Conversation(cursor.getString(0));
//            conversation.name = cursor.getString(1);
//            conversation.creatorId = cursor.getString(2);
//
//            conversations.add(conversation);
//        }
//
//        cursor.close();
//
//        return conversations;
//    }

    public List<Conversation> getAllConversationsOrderByLastMessageTime() {
        List<Conversation> conversations = new ArrayList<>();

        String[] projection = {
                DatabaseValues.Conversation.TABLE + "." + DatabaseValues.Conversation.C_ID,
                DatabaseValues.Conversation.C_NAME,
                DatabaseValues.Conversation.C_CREATOR,
                DatabaseValues.EventConversation.TABLE + "." + DatabaseValues.EventConversation.EVENT_ID,
                "MAX(" + DatabaseValues.ConversationContent.TIMESTAMP + ")",
                DatabaseValues.Conversation.TABLE + "." + DatabaseValues.Conversation.ACK
        };

        String query =
                " SELECT " + Common.implode(", ", projection) +
                " FROM " + DatabaseValues.Conversation.TABLE +
                        " LEFT JOIN " + DatabaseValues.EventConversation.TABLE + " ON " + DatabaseValues.Conversation.TABLE + "." + DatabaseValues.Conversation.C_ID + "=" + DatabaseValues.EventConversation.TABLE + "." + DatabaseValues.EventConversation.C_ID +
                        " LEFT JOIN " + DatabaseValues.ConversationContent.TABLE + " ON " + DatabaseValues.Conversation.TABLE + "." + DatabaseValues.Conversation.C_ID + "=" + DatabaseValues.ConversationContent.TABLE + "." + DatabaseValues.ConversationContent.C_ID +
                        " GROUP BY " + DatabaseValues.Conversation.TABLE + "." + DatabaseValues.Conversation.C_ID +
                        " ORDER BY MAX(" + DatabaseValues.ConversationContent.TABLE + "." + DatabaseValues.ConversationContent.TIMESTAMP + ") DESC";

        Cursor cursor = database.rawQuery(query, null);

        while (cursor.moveToNext()) {
            Conversation conversation = new Conversation(cursor.getString(0));
            conversation.name = cursor.getString(1);
            conversation.creatorId = cursor.getString(2);
            conversation.eventId = cursor.getString(3);
            conversation.lastMessageTime = cursor.getLong(4);
            conversation.syncNeeded = cursor.getInt(5) == 1;
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
                            DatabaseValues.User.FRIEND
                    },
                    DatabaseValues.User.U_ID + " = ?",
                    new String[]{
                            String.valueOf(id)
                    }
            );

            if(cursor.moveToNext()) {
                Attendee attendee = new Attendee(cursor.getString(0), cursor.getString(1), cursor.getString(2),
                        cursor.getString(3), cursor.getString(4), cursor.getString(5), cursor.getString(6), false, true);
                attendees.add(attendee);
            }
            cursor.close();
        }

        return attendees;
    }

//    private List<String> getEventAttendeesId (String eventId) {
//        List<String> attendeeList = new LinkedList<>();
//
//        Cursor cursor = database.select(
//                DatabaseValues.EventAttendee.TABLE,
//                new String[]{
//                        DatabaseValues.EventAttendee.ATTENDEE_ID
//                },
//                DatabaseValues.EventAttendee.EVENT_ID + " = ?",
//                new String[]{
//                        String.valueOf(eventId)
//                }
//        );
//
//        while (cursor.moveToNext()) {
//            attendeeList.add(cursor.getString(0));
//        }
//
//        cursor.close();
//
//        return attendeeList;
//    }

    public List<Message> getMessages(String query, int limit) {
        List<Message> messages = new ArrayList<>();

        String table = String.format(
            "%s cc LEFT JOIN %s c ON cc.%s = c.%s LEFT JOIN %s u ON cc.%s = u.%s",
            DatabaseValues.ConversationContent.TABLE,
            DatabaseValues.Conversation.TABLE,
            DatabaseValues.ConversationContent.C_ID,
            DatabaseValues.Conversation.C_ID,
            DatabaseValues.User.TABLE,
            DatabaseValues.ConversationContent.SENDER_ID,
            DatabaseValues.User.U_ID
        );

        String[] projection = {
            "cc." + DatabaseValues.ConversationContent.C_ID,
            "cc." + DatabaseValues.ConversationContent.SENDER_ID,
            "cc." + DatabaseValues.ConversationContent.TEXT,
            "cc." + DatabaseValues.ConversationContent.TIMESTAMP,
            "c." + DatabaseValues.Conversation.C_NAME,
            "u." + DatabaseValues.User.FIRST_NAME,
            "u." + DatabaseValues.User.LAST_NAME,
            "u." + DatabaseValues.User.USER_NAME
        };

        List<String> args = new ArrayList<>();

        String selection = "";

        String[] terms = Common.explode(" ", query);
        for (int i = 0; i < terms.length; i++) {
            if (i > 0) selection += " AND ";

            selection += "(";

            String[] fields = {
                "cc." + DatabaseValues.ConversationContent.TEXT,
                "c." + DatabaseValues.Conversation.C_NAME,
                "u." + DatabaseValues.User.FIRST_NAME,
                "u." + DatabaseValues.User.LAST_NAME,
                "u." + DatabaseValues.User.USER_NAME
            };

            for (int j = 0; j < fields.length; j++) {
                if (j > 0) selection += " OR ";
                selection += fields[j] + " LIKE '%' || ? || '%'";
                args.add(terms[i]);
            }

            selection += ")";
        }
        selection = String.format("(%s)", selection);

        String[] selectionArgs = args.toArray(new String[args.size()]);

        Cursor cursor = database.select(
            table,
            projection,
            selection,
            selectionArgs,
            null,
            DatabaseValues.ConversationContent.TIMESTAMP + " DESC",
            null,
            limit
        );

        while (cursor.moveToNext()) {
            String id = cursor.getString(0);
            String senderId = cursor.getString(1);
            String text = cursor.getString(2);
            long timestamp = cursor.getLong(3);

            Message msg = new Message(senderId, id, text, timestamp);
            msg.color = 0;
            msg.title = cursor.getString(4);
            msg.firstName = cursor.getString(5);
            msg.lastName = cursor.getString(6);
            msg.username = cursor.getString(7);

            messages.add(msg);
        }

        cursor.close();

        return messages;
    }

    /**
     * Retrieve a list of message attachments.
     *
     * @param messageId The message ID belonging to the attachments.
     * @return a list of attachments.
     */
    public List<Media> getMessageAttachments(long messageId) {
        List<Media> result = new ArrayList<>();

        Cursor cursor = database.select(
            DatabaseValues.ConversationAttachments.TABLE,
            DatabaseValues.ConversationAttachments.PROJECTION,
            DatabaseValues.ConversationAttachments.MESSAGE_ID + " = ?",
            new String[]{
                String.valueOf(messageId)
            }
        );

        while (cursor.moveToNext()) {
            String path = cursor.getString(DatabaseValues.ConversationAttachments.INDEX_PATH);
            String type = cursor.getString(DatabaseValues.ConversationAttachments.INDEX_TYPE);
            int status = cursor.getInt(DatabaseValues.ConversationAttachments.INDEX_STATUS);

            Media attachment = new Media(Uri.parse(path), type, status);
            result.add(attachment);
        }

        cursor.close();

        return result;
    }

    /**
     * Insert message attachments belonging to a message.
     *
     * @param messageId The message ID belonging to the attachments.
     * @param attachments List of attachments.
     */
    public void setMessageAttachments(long messageId, List<Media> attachments) {
        database.delete(
            DatabaseValues.ConversationAttachments.TABLE,
            DatabaseValues.ConversationAttachments.MESSAGE_ID + " = ?",
            new String[]{
                String.valueOf(messageId)
            }
        );

        for (Media attachment : attachments) {
            ContentValues values = new ContentValues();
            values.put(DatabaseValues.ConversationAttachments.MESSAGE_ID, messageId);
            values.put(DatabaseValues.ConversationAttachments.PATH, attachment.uri.toString());
            values.put(DatabaseValues.ConversationAttachments.TYPE, attachment.type);
            values.put(DatabaseValues.ConversationAttachments.STATUS, attachment.size);

            database.insert(DatabaseValues.ConversationAttachments.TABLE, values);
        }
    }

//    private Message cursorToMessage(Cursor cursor) {
//        String id = cursor.getString(DatabaseValues.ConversationContent.INDEX_C_ID);
//        String senderId = cursor.getString(DatabaseValues.ConversationContent.INDEX_SENDER_ID);
//        String text = cursor.getString(DatabaseValues.ConversationContent.INDEX_TEXT);
//        long timestamp = cursor.getLong(DatabaseValues.ConversationContent.INDEX_TIMESTAMP);
//
//        return new Message(senderId, id, text, timestamp);
//    }
}
