package com.mono.dashboard;

import com.mono.EventManager;
import com.mono.model.Event;

import java.util.List;

/**
 * A fragment that displays a list of favorite events. Events selected can be viewed or edited.
 * Using a sliding gesture of left or right on the event will reveal additional options to trigger
 * a chat conversation or no longer want an event to be a favorite.
 *
 * @author Gary Ng
 */
public class FavoritesFragment extends EventsFragment {

    @Override
    public void onPreCreate() {
        super.onPreCreate();

        position = DashboardFragment.TAB_FAVORITE;
    }

    @Override
    public boolean checkEvent(Event event) {
        return !dataSource.contains(event);
    }

    @Override
    public void update(List<Event> events, int scrollToPosition) {
        for (Event event : events) {
            if (checkEvent(event)) {
                if (event.favorite) {
                    insert(event, false);
                }
            } else {
                if (event.favorite) {
                    super.update(events, scrollToPosition);
                } else {
                    remove(event.id, false);
                }
            }
        }
    }

    @Override
    public List<Event> retrieveEvents() {
        if (!dataSource.isEmpty()) {
            return null;
        }

        return EventManager.getInstance(getContext()).getFavoriteEvents();
    }
}
