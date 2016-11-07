package com.mono.dashboard;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.mono.EventManager;
import com.mono.EventManager.EventAction;
import com.mono.model.Event;

import java.util.ArrayList;
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
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        position = DashboardFragment.TAB_FAVORITE;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);

        events.addAll(EventManager.getInstance(getContext()).getFavoriteEvents());

        if (!events.isEmpty()) {
            text.setVisibility(View.INVISIBLE);
            adapter.notifyDataSetChanged();
        }

        return view;
    }

    /**
     * Handle all event changes being reported by the Event Manager.
     *
     * @param data Event action data.
     */
    @Override
    public void onEventBroadcast(EventAction... data) {
        for (int i = 0; i < data.length; i++) {
            int action = -1;
            int scrollToPosition = -1;

            List<Event> events = new ArrayList<>();
            for (; i < data.length; i++) {
                EventAction item = data[i];

                if (action != -1 && action != item.getAction()) {
                    break;
                }

                action = item.getAction();

                if (item.getStatus() == EventAction.STATUS_OK) {
                    if (scrollToPosition == -1 && item.getActor() == EventAction.ACTOR_SELF) {
                        scrollToPosition = events.size();
                    }

                    events.add(item.getEvent());
                }
            }

            if (events.isEmpty()) {
                continue;
            }

            switch (action) {
                case EventAction.ACTION_UPDATE:
                    update(events, scrollToPosition);
                    break;
                case EventAction.ACTION_REMOVE:
                    remove(events);
                    break;
            }
        }
    }

    /**
     * Check if event is valid to be displayed within this fragment.
     *
     * @param event Event to check.
     * @return whether event is valid.
     */
    protected boolean checkEvent(Event event) {
        return !events.contains(event);
    }

    @Override
    public void update(List<Event> items, int scrollToPosition) {
        for (Event event : items) {
            if (event.favorite && !events.contains(event)) {
                insert(event, false);
            } else if (!event.favorite && events.contains(event)) {
                remove(event);
            }
        }

        super.update(items, scrollToPosition);
    }

    /**
     * Retrieve and append events at the bottom of the list.
     */
    @Override
    protected void append() {

    }
}
