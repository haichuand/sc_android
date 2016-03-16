package com.mono.chat;

import com.mono.model.Attendee;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by hduan on 3/15/2016.
 */
public class ChatAttendeeMap {
    private Map<String, Attendee> chatAttendeeMap;

    public ChatAttendeeMap() {
        chatAttendeeMap = new HashMap<>();
    }

    public Map<String, Attendee> getChatAttendeeMap() {
        return chatAttendeeMap;
    }

    public ChatAttendeeMap(List<Attendee> attendees) {
        chatAttendeeMap = new HashMap<>();
        Iterator<Attendee> iterator= attendees.iterator();
        while (iterator.hasNext()) {
            Attendee attendee = iterator.next();
            chatAttendeeMap.put(String.valueOf(attendee.id), attendee);
        }
    }

    public Map<String, Attendee> addAttendee(Attendee attendee) {
        chatAttendeeMap.put(String.valueOf(attendee.id), attendee);
        return chatAttendeeMap;
    }
}
