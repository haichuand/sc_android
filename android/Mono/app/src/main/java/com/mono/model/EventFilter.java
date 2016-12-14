package com.mono.model;

import com.mono.util.Common;

/**
 * This data structure is used to store information about a specific event filter.
 *
 * @author Gary Ng
 */
public class EventFilter {

    public String text;
    public boolean status;

    public EventFilter(String text, boolean status) {
        this.text = text;
        this.status = status;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof EventFilter)) {
            return false;
        }

        EventFilter filter = (EventFilter) object;

        if (!Common.compareStrings(text, filter.text)) {
            return false;
        }

        if (status != filter.status) {
            return false;
        }

        return true;
    }
}
