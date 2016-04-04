package com.mono.chat;

import android.content.Context;

import com.mono.db.DatabaseHelper;
import com.mono.db.dao.ConversationDataSource;
import com.mono.model.Attendee;
import com.mono.model.Message;

import java.util.List;

/**
 * Created by hduan on 3/28/2016.
 */
public class ConversationManager {

    private static ConversationManager instance;
    private Context context;
    private ConversationDataSource conversationDataSource;

    private ConversationManager(Context context) {
        this.context = context;
        conversationDataSource = DatabaseHelper.getDataSource(context, ConversationDataSource.class);
    }

    public static ConversationManager getInstance(Context context) {
        if (instance == null) {
            instance = new ConversationManager(context);
        }
        return instance;
    }

    public List<Message> getChatMessages(String conversationId) {
        return conversationDataSource.getConversationMessages(conversationId);
    }

    public ChatAttendeeMap getChatAttendeeMap(String conversationId) {
        List<Attendee> attendeeList = conversationDataSource.getConversationAttendees(conversationId);
        return new ChatAttendeeMap(attendeeList);
    }

    public void saveChatMessageToDB(Message message) {
        conversationDataSource.addMessageToConversation(message);
    }
}
