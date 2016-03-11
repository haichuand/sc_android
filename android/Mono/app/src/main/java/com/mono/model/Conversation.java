package com.mono.model;

import java.util.List;

/**
 * Created by xuejing on 3/10/16.
 */
public class Conversation {
    String id;
    String name;
    List<String> attendees;
    List<Message> messages;

    public Conversation(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public Conversation(String id, String name, List<String> attendees, List<Message> messages) {
        this.id = id;
        this.name = name;
        this.attendees = attendees;
        this.messages = messages;
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

    public List<String> getAttendeesId() {
        return this.attendees;
    }

    public List<Message> getMessage() {
        return this.messages;
    }
}
