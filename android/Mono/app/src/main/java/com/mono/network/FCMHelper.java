package com.mono.network;

import com.mono.util.Common;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * This helper class is used to return encapsulated message information for FCM upstream
 * messaging. All existing FCM action constants belong here.
 *
 * @author Gary Ng, Xuejing Dong, Haichuan Duan
 */
public class FCMHelper {

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
    public static final String ACTION_UPDATE_FCM_ID = "UPDATE_FCM_ID";
    public static final String ACTION_LEAVE_CONVERSATION = "LEAVE_CONVERSATION";

    public static final String CONVERSATION_ID = "conversationId";
    public static final String SENDER_ID = "senderId";
    public static final String MESSAGE = "message";
    public static final String MESSAGE_ID = "messageId";
    public static final String RECIPIENTS = "recipients";
    public static final String FCM_ID = "fcmId";
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

    private FCMHelper() {}

    public static Map<String, String> getChatMessage(String conversationId, String senderId,
            String message) {
        Map<String, String> data = new LinkedHashMap<>();
        data.put(ACTION, ACTION_MESSAGE);
        data.put(CONVERSATION_ID, conversationId);
        data.put(SENDER_ID, senderId);
        data.put(MESSAGE, message);

        return data;
    }

    public static Map<String, String> getChatMessage(String conversationId, String senderId,
            String message, List<String> recipients) {
        Map<String, String> data = getChatMessage(conversationId, senderId, message);
        data.put(RECIPIENTS, Common.implode(",", recipients));

        return data;
    }

    public static Map<String, String> getLoginMessage(String username, String password) {
        Map<String, String> data = new LinkedHashMap<>();
        data.put(ACTION, ACTION_LOGIN);
        data.put(USERNAME, username);
        data.put(PASSWORD, password);

        return data;
    }

    public static Map<String, String> getRegisterMessage(String firstName, String lastName,
            String email, String password) {
        Map<String, String> data = new LinkedHashMap<>();
        data.put(ACTION, ACTION_REGISTER);
        data.put(FIRST_NAME, firstName);
        data.put(LAST_NAME, lastName);
        data.put(EMAIL, email);
        data.put(PASSWORD, password);

        return data;
    }

    public static Map<String, String> getRegisterByMobileMessage(String firstName, String lastName,
            String phone, String password) {
        Map<String, String> data = new LinkedHashMap<>();
        data.put(ACTION, ACTION_REGISTER_M);
        data.put(FIRST_NAME, firstName);
        data.put(LAST_NAME, lastName);
        data.put(PHONE, phone);
        data.put(PASSWORD, password);

        return data;
    }

    public static Map<String, String> getRegisterPayload(String senderId, String fcmId) {
        Map<String, String> data = new LinkedHashMap<>();
        data.put(ACTION, ACTION_REGISTER);
        data.put(SENDER_ID, senderId);
        data.put(FCM_ID, fcmId);

        return data;
    }

    public static Map<String, String> getStartConversationPayload(String creatorId,
            String conversationId, String recipients) {
        Map<String, String> data = new LinkedHashMap<>();
        data.put(ACTION, ACTION_START_CONVERSATION);
        data.put(CREATOR_ID, creatorId);
        data.put(CONVERSATION_ID, conversationId);
        data.put(RECIPIENTS, recipients);

        return data;
    }

    public static Map<String, String> getStartEventConversationPayload(String creatorId,
            String eventId, String recipients) {
        Map<String, String> data = new LinkedHashMap<>();
        data.put(ACTION, ACTION_START_EVENT_CONVERSATION);
        data.put(CREATOR_ID, creatorId);
        data.put(EVENT_ID, eventId);
        data.put(RECIPIENTS, recipients);

        return data;
    }

    public static Map<String, String> getShareEventPayload(String creatorId, String eventId,
            String recipients) {
        Map<String, String> data = new LinkedHashMap<>();
        data.put(ACTION, ACTION_SHARE_EVENT);
        data.put(CREATOR_ID, creatorId);
        data.put(EVENT_ID, eventId);
        data.put(RECIPIENTS, recipients);

        return data;
    }

    public static Map<String, String> getUpdateFcmIdPayload(String senderId, String newFcmId) {
        Map<String, String> data = new LinkedHashMap<>();
        data.put(ACTION, ACTION_UPDATE_FCM_ID);
        data.put(SENDER_ID, senderId);
        data.put(FCM_ID, newFcmId);

        return data;
    }

    public static Map<String, String> getConversationMessagePayload(String senderId,
            String conversationId, List<String> recipients, String message, String messageId,
            List<String> attachments) {
        Map<String, String> data = new LinkedHashMap<>();
        data.put(ACTION, ACTION_CONVERSATION_MESSAGE);
        data.put(SENDER_ID, senderId);
        data.put(CONVERSATION_ID, conversationId);
        data.put(RECIPIENTS, Common.implode(",", recipients));
        data.put(MESSAGE, message);
        data.put(MESSAGE_ID, messageId);

        if (attachments != null) {
            data.put(ATTACHMENTS, Common.implode(",", attachments));
        }

        return data;
    }

    public static Map<String, String> getLeaveConversationPayload(String senderId,
            String conversationId, String recipients) {
        Map<String, String> data = new LinkedHashMap<>();
        data.put(ACTION, ACTION_LEAVE_CONVERSATION);
        data.put(SENDER_ID, senderId);
        data.put(CONVERSATION_ID, conversationId);
        data.put(RECIPIENTS, recipients);

        return data;
    }

    public static Map<String, String> getAddConversationAttendeesPayload(String senderId,
            String conversationId, String targetUserIds, String recipientIds) {
        Map<String, String> data = new LinkedHashMap<>();
        data.put(ACTION, ACTION_ADD_CONVERSATION_ATTENDEES);
        data.put(SENDER_ID, senderId);
        data.put(USER_IDS, targetUserIds);
        data.put(CONVERSATION_ID, conversationId);
        data.put(RECIPIENTS, recipientIds);

        return data;
    }

    public static Map<String, String> getDropConversationAttendeesPayload(String senderId,
            String conversationId, String targetUserIds, String recipientIds) {
        Map<String, String> data = new LinkedHashMap<>();
        data.put(ACTION, ACTION_DROP_CONVERSATION_ATTENDEES);
        data.put(SENDER_ID, senderId);
        data.put(USER_IDS, targetUserIds);
        data.put(CONVERSATION_ID, conversationId);
        data.put(RECIPIENTS, recipientIds);

        return data;
    }

    public static Map<String, String> getUpdateConversationTitlePayload(String senderId,
            String conversationId, String newTitle, String recipientIds) {
        Map<String, String> data = new LinkedHashMap<>();
        data.put(ACTION, ACTION_UPDATE_CONVERSATION_TITLE);
        data.put(SENDER_ID, senderId);
        data.put(CONVERSATION_ID, conversationId);
        data.put(CONVERSATION_TITLE, newTitle);
        data.put(RECIPIENTS, recipientIds);

        return data;
    }
}
