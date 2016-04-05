package com.mono.model;

import java.util.ArrayList;
import java.util.List;

public class Calendar {

    public final long id;
    public String name;
    public String description;
    public int color;

    public String accountName;
    public String accountType;

    public List<Event> events = new ArrayList<>();

    public Calendar(long id) {
        this.id = id;
    }
}
