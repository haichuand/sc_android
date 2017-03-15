package com.mono.dashboard;

import com.mono.R;
import com.mono.model.Event;

/**
 * A fragment that displays a list of upcoming events. Events selected can be viewed or edited.
 * Using a sliding gesture of left or right on the event will reveal additional options to trigger
 * a chat conversation or perform a quick deletion of unwanted events.
 *
 * @author Gary Ng
 */
public class UpcomingFragment extends EventsFragment {

    @Override
    public void onPreCreate() {
        super.onPreCreate();

        position = DashboardFragment.TAB_UPCOMING;

        sortOrder = SORT_ORDER_ASC;

        dataSource = new EventDataSource(getContext(), R.color.green);
    }

    @Override
    public boolean checkEvent(Event event) {
        if (!super.checkEvent(event)) {
            return false;
        }

        if (dataSource.getCount() > MAX_VISIBLE_ITEMS) {
//            Event tempEvent = dataSource.last();
//            if (event.startTime > tempEvent.startTime) {
//                return false;
//            }
        }

        return true;
    }
}
