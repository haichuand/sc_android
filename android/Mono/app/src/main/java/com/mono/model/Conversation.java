package com.mono.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by xuejing on 3/10/16.
 */
public class Conversation {

    public final String id;
    public String creatorId;
    public String eventId;
    public String name;
    public List<Attendee> attendees;
    public List<Message> messages;
    public long lastMessageTime;
    public boolean syncNeeded;
    public int missCount;

    public Conversation(String id) {
        this.id = id;
    }

    public Conversation(String id, String creatorId, String name) {
        this.id = id;
        this.creatorId = creatorId;
        this.name = name;
    }

    public Conversation(String id, String creatorId, String name, List<Attendee> attendees, List<Message> messages) {
        this.id = id;
        this.creatorId = creatorId;
        this.name = name;
        this.attendees = attendees;
        this.messages = messages;
    }

    public Conversation(String id, String eventId, String creatorId, String name, List<Attendee> attendees, List<Message> messages) {
        this.id = id;
        this.eventId = eventId;
        this.creatorId = creatorId;
        this.name = name;
        this.attendees = attendees;
        this.messages = messages;
    }

    public Conversation(String id, String eventId, String creatorId, String name, List<Attendee> attendees, List<Message> messages, boolean syncNeeded, int missCount) {
        this.id = id;
        this.eventId = eventId;
        this.creatorId = creatorId;
        this.name = name;
        this.attendees = attendees;
        this.messages = messages;
        this.syncNeeded = syncNeeded;
        this.missCount = missCount;
    }

    public String getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Attendee> getAttendees() {
        return this.attendees;
    }

    public List<Message> getMessage() {
        return this.messages;
    }

    public List<String> getAttendeeIdList() {
        List<String> attendeeIdList = new ArrayList<>();
        if (attendees == null || attendees.isEmpty()) {
            return attendeeIdList;
        }
        for (Attendee attendee : attendees) {
            attendeeIdList.add(attendee.id);
        }
        return attendeeIdList;
    }

    public String getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(String creatorId) {
        this.creatorId = creatorId;
    }

    public String getEventId() {
        return eventId;
    }

    public void setAttendees(List<Attendee> attendees) {
        this.attendees = attendees;
    }

    public List<Message> getMessages() {
        return messages;
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }
}
