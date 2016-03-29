package com.mono.chat;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.mono.db.DatabaseHelper;
import com.mono.db.dao.AttendeeDataSource;
import com.mono.db.dao.ConversationDataSource;
import com.mono.model.Attendee;
import com.mono.model.Message;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by hduan on 3/28/2016.
 */
public class DemoChat {

    public static String[] createTestData (Context context) {
        final String testChatIdKey = "TEST_CHAT_ID";
        final String myIdKey = "TEST_CHAT_MY_ID";
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String testChatId = preferences.getString(testChatIdKey, null);
        String myId = preferences.getString(myIdKey, null);
        if (testChatId != null && myId != null) {
            return new String[]{testChatId, myId};
        }

        //create chat attendees in User table
        AttendeeDataSource attendeeDataSource = DatabaseHelper.getDataSource(context, AttendeeDataSource.class);
        List<String> attendeeIdList = new ArrayList<>();
        String id1 = attendeeDataSource.createAttendee("", "jason@email.com", "4381034139", "Jason", "Lee", "jlee", true);
        attendeeIdList.add(id1);
        String id2 = attendeeDataSource.createAttendee("", "mjohnson@kmail.com", "7835038129", "Martha", "Johnson", "mjohnson", true);
        attendeeIdList.add(id2);
        String id3 = attendeeDataSource.createAttendee("", "ka234@abco.org", "6451983571", "Kathy", "Smith", "ksmith", true);
        attendeeIdList.add(id3);
        String id4 = attendeeDataSource.createAttendee("", "hmc@kdaid.com", "2353817510", "Henry", "McDouglas", "hmcdouglas", true);
        attendeeIdList.add(id4);
        //add attendees to Conversation_attendee table
        ConversationDataSource conversationDataSource = DatabaseHelper.getDataSource(context, ConversationDataSource.class);
        String conversationId = conversationDataSource.createConversationWithSelectedAttendees("Demo chat", "DemoEventId", attendeeIdList);
        List<Attendee> list = conversationDataSource.getConversationAttendees(conversationId);
        //add messages to Conversation_content table
        conversationDataSource.addMessageToConversation(new Message(id1, conversationId, "Hi!", new Date().getTime()));
        conversationDataSource.addMessageToConversation(new Message(id1, conversationId, "Are we meeting in Room 362?", new Date().getTime()));
        conversationDataSource.addMessageToConversation(new Message(id2, conversationId, "I think it's Room 462", new Date().getTime()));
        conversationDataSource.addMessageToConversation(new Message(id3, conversationId, "Really?", new Date().getTime()));
        conversationDataSource.addMessageToConversation(new Message(id1, conversationId, "Hmm...Let me check with Jason.", new Date().getTime()));
        conversationDataSource.addMessageToConversation(new Message(id1, conversationId, "Oh Martha you're right, it's room 462. Bad memory on my part!", new Date().getTime()));
        conversationDataSource.addMessageToConversation(new Message(id4, conversationId, "We need to make sure everyone knows this. The meeting will start soon.", new Date().getTime()));
        conversationDataSource.addMessageToConversation(new Message(id2, conversationId, "I will text Howard and Liz.", new Date().getTime()));
        conversationDataSource.addMessageToConversation(new Message(id3, conversationId, "Who else is coming?", new Date().getTime()));
        //update SharedPreferences
        preferences.edit().putString(testChatIdKey, conversationId).putString(myIdKey, id1).apply();
        return new String[]{conversationId, id1};
    }


}
