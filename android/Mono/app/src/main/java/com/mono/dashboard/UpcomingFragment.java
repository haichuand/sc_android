package com.mono.dashboard;

import android.os.AsyncTask;
import android.os.Bundle;

import com.mono.EventManager;
import com.mono.R;
import com.mono.model.Event;

import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;

import java.util.Comparator;
import java.util.List;

/**
 * A fragment that displays a list of upcoming events. Events selected can be viewed or edited.
 * Using a sliding gesture of left or right on the event will reveal additional options to trigger
 * a chat conversation or perform a quick deletion of unwanted events.
 *
 * @author Gary Ng
 */
public class UpcomingFragment extends EventsFragment {

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
    }

    /**
     * Check if event is valid to be displayed within this fragment.
     *
     * @param event Event to check.
     * @return whether event is valid.
     */
    @Override
    protected boolean checkEvent(Event event) {
        if (events.contains(event)) {
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

    /**
     * Retrieve and append events at the bottom of the list.
     */
    @Override
    protected void append() {
        if (task != null) {
            task.cancel(true);
            task = null;
        }

        task = new AsyncTask<Void, Void, List<Event>>() {
            @Override
            protected List<Event> doInBackground(Void... params) {
                EventManager manager = EventManager.getInstance(getContext());

                List<Event> result = manager.getEventsFromProviderByOffset(startTime,
                    offsetProvider, PRECACHE_AMOUNT, 1);
                offsetProvider += result.size();

                List<Event> events = manager.getEventsByOffset(startTime, offset,
                    PRECACHE_AMOUNT, 1);
                offset += events.size();

                combine(result, events);

                return result;
            }

            @Override
            protected void onPostExecute(List<Event> result) {
                if (!result.isEmpty()) {
                    insert(events.size(), result);
                }

                task = null;
            }
        }.execute();
    }
}
