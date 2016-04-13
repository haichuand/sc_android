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

    public static final String CONVERSATION_ID = "conversation_id";
    public static final String SENDER_ID = "sender_id";
    public static final String MESSAGE = "message";
    public static final String RECIPIENTS = "recipients";

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
}
