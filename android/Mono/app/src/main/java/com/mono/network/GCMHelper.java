package com.mono.network;

import android.os.Bundle;

import com.mono.util.Common;

import java.util.List;

public class GCMHelper {

    public static final String ACTION = "action";
    public static final String ACTION_MESSAGE = "MESSAGE";
    public static final String ACTION_LOGIN = "LOGIN";
    public static final String ACTION_REGISTER = "REGISTER";
    public static final String ACTION_REGISTER_M = "REGISTER_M";
    public static final String ACTION_SHARE_EVENT = "SHARE_EVENT";
    public static final String ACTION_UPDATE_EVENT = "UPDATE_EVENT";
    public static final String ACTION_START_CONVERSATION= "START_CONVERSATION";
    public static final String ACTION_START_EVENT_CONVERSATION = "startEventConversation";
    public static final String ACTION_CONVERSATION_MESSAGE = "CONVERSATION_MESSAGE";
    public static final String ACTION_ADD_CONVERSATION_ATTENDEES = "ADD_CONVERSATION_ATTENDEES";
    public static final String ACTION_DROP_CONVERSATION_ATTENDEES = "DROP_CONVERSATION_ATTENDEES";
    public static final String ACTION_UPDATE_CONVERSATION_TITLE = "UPDATE_CONVERSATION_TITLE";
    public static final String ACTION_UPDATE_GCM_ID = "UPDATE_GCM_ID";
    public static final String ACTION_LEAVE_CONVERSATION = "LEAVE_CONVERSATION";

    public static final String CONVERSATION_ID = "conversationId";
    public static final String SENDER_ID = "senderId";
    public static final String MESSAGE = "message";
    public static final String MESSAGE_ID = "messageId";
    public static final String RECIPIENTS = "recipients";
    public static final String GCM_ID = "gcmId";
    public static final String CREATOR_ID = "creatorId";
    public static final String EVENT_ID = "eventId";
    public static final String USER_IDS = "userIds";
    public static final String CONVERSATION_TITLE = "conversationTitle";
    public static final String START_TIME = "startTime";
    public static final String END_TIME = "endTime";
    public static final String TITLE = "title";
    public static final String ATTENDEES = "attendees";
    public static final String ATTENDEES_ID = "attendeesId";
    public static final String ATTACHMENTS = "attachments";
    public static final String TIMESTAMP = "timestamp";

    public static final String USERNAME = "username";
    public static final String EMAIL = "email";
    public static final String PHONE = "phone";
    public static final String PASSWORD = "password";
    public static final String FIRST_NAME = "first_name";
    public static final String LAST_NAME = "last_name";

    private GCMHelper() {}

    public static Bundle getChatMessage(String conversationId, String senderId, String message) {
        Bundle args = new Bundle();
        args.putString(ACTION, ACTION_MESSAGE);
        args.putString(CONVERSATION_ID, conversationId);
        args.putString(SENDER_ID, senderId);
        args.putString(MESSAGE, message);

        return args;
    }

    public static Bundle getChatMessage(String conversationId, String senderId, String message,
            List<String> recipients) {
        Bundle args = getChatMessage(conversationId, senderId, message);
        args.putString(RECIPIENTS, Common.implode(",", recipients));

        return args;
    }

    public static Bundle getLoginMessage(String username, String password) {
        Bundle args = new Bundle();
        args.putString(ACTION, ACTION_LOGIN);
        args.putString(USERNAME, username);
        args.putString(PASSWORD, password);

        return args;
    }

    public static Bundle getRegisterMessage(String firstName, String lastName, String email,
            String password) {
        Bundle args = new Bundle();
        args.putString(ACTION, ACTION_REGISTER);
        args.putString(FIRST_NAME, firstName);
        args.putString(LAST_NAME, lastName);
        args.putString(EMAIL, email);
        args.putString(PASSWORD, password);

        return args;
    }

    public static Bundle getRegisterByMobileMessage(String firstName, String lastName,
            String phone, String password) {
        Bundle args = new Bundle();
        args.putString(ACTION, ACTION_REGISTER_M);
        args.putString(FIRST_NAME, firstName);
        args.putString(LAST_NAME, lastName);
        args.putString(PHONE, phone);
        args.putString(PASSWORD, password);

        return args;
    }

    /*******************new added functions for generating gcm message payload********************/

    public static Bundle getRegisterPayload(String senderId, String gcmId) {
        Bundle args = new Bundle();
        args.putString(ACTION, ACTION_REGISTER);
        args.putString(SENDER_ID, senderId);
        args.putString(GCM_ID, gcmId);

        return args;
    }

    public static Bundle getStartConversationPayload(String creatorId, String conversationId,
                                                     String recipients) {
        Bundle args = new Bundle();
        args.putString(ACTION, ACTION_START_CONVERSATION);
        args.putString(CREATOR_ID, creatorId);
        args.putString(CONVERSATION_ID, conversationId);
        args.putString(RECIPIENTS, recipients);

        return args;
    }

//    public static Bundle getStartEventConversationPayload(String creatorId, String conversationId, String conversationTitle, String recipients,
//                                                          long  startTime, long endTime, String eventTitle, String eventAttendees) {
//        Bundle args = new Bundle();
//        args.putString(ACTION, ACTION_START_EVENT_CONVERSATION);
//        args.putString(CREATOR_ID, creatorId);
//        args.putString(CONVERSATION_ID, conversationId);
//        args.putString(CONVERSATION_TITLE, conversationTitle);
//        args.putString(RECIPIENTS, recipients);
//        args.putLong(START_TIME, startTime);
//        args.putLong(END_TIME, endTime);
//        args.putString(TITLE, eventTitle);
//        args.putString(ATTENDEES, eventAttendees);
//        return args;
//    }

    public static Bundle getStartEventConversationPayload (String creatorId, String eventId, String recipients) {
        Bundle args = new Bundle();
        args.putString(ACTION, ACTION_START_EVENT_CONVERSATION);
        args.putString(CREATOR_ID, creatorId);
        args.putString(EVENT_ID, eventId);
        args.putString(RECIPIENTS, recipients);
        return args;
    }

    public static Bundle getShareEventPayload(String creatorId, String eventId, String recipients) {
        Bundle args = new Bundle();
        args.putString(ACTION, ACTION_SHARE_EVENT);
        args.putString(CREATOR_ID, creatorId);
        args.putString(EVENT_ID, eventId);
        args.putString(RECIPIENTS, recipients);

        return args;
    }

    public static Bundle getUpdateGcmIdPayload(String senderId, String newGcmId) {
        Bundle args = new Bundle();
        args.putString(ACTION, ACTION_UPDATE_GCM_ID);
        args.putString(SENDER_ID, senderId);
        args.putString(GCM_ID, newGcmId);

        return args;
    }

    public static Bundle getConversationMessagePayload(String senderId, String conversationId,
            List<String> recipients, String message, String messageId, List<String> attachments) {
        Bundle args = new Bundle();
        args.putString(ACTION, ACTION_CONVERSATION_MESSAGE);
        args.putString(SENDER_ID, senderId);
        args.putString(CONVERSATION_ID, conversationId);
        args.putString(RECIPIENTS, Common.implode(",", recipients));
        args.putString(MESSAGE, message);
        args.putString(MESSAGE_ID, messageId);

        if (attachments != null) {
            args.putString(ATTACHMENTS, Common.implode(",", attachments));
        }

        return args;
    }

    public static Bundle getLeaveConversationPayload(String senderId, String conversationId,
                                                     String recipients) {
        Bundle args = new Bundle();
        args.putString(ACTION, ACTION_LEAVE_CONVERSATION);
        args.putString(SENDER_ID, senderId);
        args.putString(CONVERSATION_ID, conversationId);
        args.putString(RECIPIENTS, recipients);

        return args;
    }

    public static Bundle getAddConversationAttendeesPayload(String senderId, String conversationId, String targetUserIds, String recipientIds) {
        Bundle args = new Bundle();
        args.putString(ACTION, ACTION_ADD_CONVERSATION_ATTENDEES);
        args.putString(SENDER_ID, senderId);
        args.putString(USER_IDS, targetUserIds);
        args.putString(CONVERSATION_ID, conversationId);
        args.putString(RECIPIENTS, recipientIds);

        return args;
    }

    public static Bundle getDropConversationAttendeesPayload(String senderId, String conversationId,
                                                            String targetUserIds, String recipientIds) {
        Bundle args = new Bundle();
        args.putString(ACTION, ACTION_DROP_CONVERSATION_ATTENDEES);
        args.putString(SENDER_ID, senderId);
        args.putString(USER_IDS, targetUserIds);
        args.putString(CONVERSATION_ID, conversationId);
        args.putString(RECIPIENTS, recipientIds);

        return args;
    }

    public static Bundle getUpdateConversationTitlePayload(String senderId, String conversationId, String newTitle, String recipientIds) {
        Bundle args = new Bundle();
        args.putString(ACTION, ACTION_UPDATE_CONVERSATION_TITLE);
        args.putString(SENDER_ID, senderId);
        args.putString(CONVERSATION_ID, conversationId);
        args.putString(CONVERSATION_TITLE, newTitle);
        args.putString(RECIPIENTS, recipientIds);

        return args;
    }
}
