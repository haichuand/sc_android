package com.mono.chat;

import com.mono.model.Attendee;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by hduan on 3/15/2016.
 */
public class ChatAttendeeMap {

    //maps attendeeId (String) to Attendee
    private Map<String, Attendee> chatAttendeeMap;

    public ChatAttendeeMap() {
        chatAttendeeMap = new HashMap<>();
    }

    public Map<String, Attendee> getAttendeeMap() {
        return chatAttendeeMap;
    }

    public ChatAttendeeMap(List<Attendee> attendees) {
        chatAttendeeMap = new HashMap<>();
        for (Attendee attendee : attendees) {
            chatAttendeeMap.put(String.valueOf(attendee.id), attendee);
        }
    }

    public Map<String, Attendee> addAttendee(Attendee attendee) {
        chatAttendeeMap.put(String.valueOf(attendee.id), attendee);
        return chatAttendeeMap;
    }

    public List<Attendee> toAttendeeList() {
        List<Attendee> list = new ArrayList<>();
        for (Map.Entry entry : chatAttendeeMap.entrySet()) {
            list.add((Attendee) entry.getValue());
        }
        return list;
    }
}
