package com.mono.dashboard;

import com.mono.util.SimpleViewHolder;

import java.util.ArrayList;
import java.util.List;

/**
 * This data structure is used to store information about an event group item.
 *
 * @author Gary Ng
 */
public class EventGroupItem extends SimpleViewHolder.HolderItem {

    public String title;
    public String date;
    public int dateColor;
    public int color;
    public List<EventItem> items = new ArrayList<>();
    public boolean hasPhotos;

    public EventGroupItem(String id) {
        this.id = id;
    }
}
