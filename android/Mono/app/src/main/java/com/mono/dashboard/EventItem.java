package com.mono.dashboard;

import com.mono.util.SimpleViewHolder;

/**
 * This data structure is used to store information about a specific event item.
 *
 * @author Gary Ng
 */
public class EventItem extends SimpleViewHolder.HolderItem {

    public static final int TYPE_EVENT = 0;

    public int type;
    public int iconResId;
    public int iconColor;
    public String title;
    public int titleColor;
    public boolean titleBold;
    public String description;
    public String dateTime;
    public boolean dateTimeBold;
    public int dateTimeColor;

    public EventItem(String id) {
        this.id = id;
    }
}
