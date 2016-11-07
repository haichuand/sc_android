package com.mono.dashboard;

import android.os.Bundle;

import com.mono.R;
import com.mono.model.Event;

import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;

import java.util.Comparator;

/**
 * A fragment that displays a list of upcoming events as groups. Events selected can be viewed or
 * edited. Using a sliding gesture of left or right on the event will reveal additional options to
 * trigger a chat conversation or perform a quick deletion of unwanted events.
 *
 * @author Gary Ng
 */
public class UpcomingGroupsFragment extends EventGroupsFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        position = DashboardFragment.TAB_UPCOMING;

        comparator = new Comparator<Event>() {
            @Override
            public int compare(Event e1, Event e2) {
                int value = Long.compare(e1.startTime, e2.startTime);
                if (value != 0) {
                    return value;
                }

                return e2.id.compareToIgnoreCase(e1.id);
            }
        };

        defaultDateTimeColorId = R.color.green;
        direction = 1;
    }

    /**
     * Check if event is valid to be displayed within this fragment.
     *
     * @param event Event to check.
     * @return whether event is valid.
     */
    @Override
    protected boolean checkEvent(Event event) {
        if (dataSource.containsEvent(event.id)) {
            return false;
        }

        LocalDateTime currentTime = new LocalDateTime();

        DateTimeZone timeZone = event.allDay ? DateTimeZone.UTC : DateTimeZone.getDefault();
        LocalDateTime dateTime = new LocalDateTime(event.startTime, timeZone);

        if (dateTime.isBefore(currentTime)) {
            return false;
        }

        return true;
    }
}
