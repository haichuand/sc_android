package com.mono.dashboard;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.SearchView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.mono.EventManager;
import com.mono.EventManager.EventAction;
import com.mono.EventManager.EventBroadcastListener;
import com.mono.MainInterface;
import com.mono.R;
import com.mono.dashboard.EventsFragment.ListListener;
import com.mono.model.Calendar;
import com.mono.model.Event;
import com.mono.provider.CalendarProvider;
import com.mono.search.SearchFragment;
import com.mono.search.SearchHandler;
import com.mono.util.OnBackPressedListener;
import com.mono.util.SimpleTabLayout.Scrollable;
import com.mono.util.SimpleTabLayout.TabPagerCallback;
import com.mono.util.SimpleTabPagerAdapter;
import com.mono.util.SimpleViewPager;

import java.util.List;

/**
 * This fragment class is used to serve as the parent container of all views used to display a
 * list of events. Common methods used by these other views are placed here primarily to have
 * access to the MainActivity.
 *
 * @author Gary Ng
 */
public class DashboardFragment extends Fragment implements OnBackPressedListener,
        OnPageChangeListener, ListListener, EventBroadcastListener, TabPagerCallback, Scrollable {

    public static final int TAB_EVENTS = 0;
    public static final int TAB_UPCOMING = 1;
    public static final int TAB_FAVORITE = 2;

    private MainInterface mainInterface;
    private EventManager eventManager;

    private SimpleTabPagerAdapter tabPagerAdapter;
    private SimpleViewPager viewPager;

    private SearchView searchView;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        eventManager = EventManager.getInstance(context);

        if (context instanceof MainInterface) {
            mainInterface = (MainInterface) context;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        eventManager.addEventBroadcastListener(this);

        tabPagerAdapter = new SimpleTabPagerAdapter(getChildFragmentManager(), getContext());
        tabPagerAdapter.add(0, getString(R.string.events), new EventsFragment());
        tabPagerAdapter.add(0, getString(R.string.upcoming), new UpcomingFragment());
        tabPagerAdapter.add(0, getString(R.string.favorite), new FavoritesFragment());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_events, container, false);

        viewPager = (SimpleViewPager) view.findViewById(R.id.container);
        viewPager.setAdapter(tabPagerAdapter);
        viewPager.setOffscreenPageLimit(tabPagerAdapter.getCount() - 1);
        viewPager.addOnPageChangeListener(this);

        return view;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        eventManager.removeEventBroadcastListener(this);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.dashboard, menu);

        MenuItem item = menu.findItem(R.id.action_search);
        searchView = (SearchView) MenuItemCompat.getActionView(item);

        FragmentManager manager = getActivity().getSupportFragmentManager();
        SearchFragment fragment = (SearchFragment) manager.findFragmentById(R.id.search_fragment);
        if (fragment != null) {
            fragment.setSearchView(searchView, new SearchHandler(fragment, true, false));
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.action_add:
                Event event = new Event();
                event.type = Event.TYPE_CALENDAR;

                List<Calendar> calendars =
                    CalendarProvider.getInstance(getContext()).getCalendars();
                for (Calendar calendar : calendars) {
                    if (calendar.primary) {
                        event.calendarId = calendar.id;
                        break;
                    }
                }

                mainInterface.showEventDetails(event);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onBackPressed() {
        if (searchView != null && !searchView.isIconified()) {
            searchView.setIconified(true);
            searchView.onActionViewCollapsed();

            FragmentManager manager = getActivity().getSupportFragmentManager();
            SearchFragment fragment = (SearchFragment) manager.findFragmentById(R.id.search_fragment);
            if (fragment != null) {
                fragment.setVisible(false);
            }
            return true;
        }

        return false;
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(int position) {
        onPageSelected();
    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }

    @Override
    public void onClick(int tab, String id, View view) {
        Event event = eventManager.getEvent(id, false);
        mainInterface.showEventDetails(event);
    }

    @Override
    public void onLongClick(int tab, String id, View view) {

    }

    @Override
    public void onChatClick(int tab, String id) {
        mainInterface.showChat(id);
    }

    /**
     * Handle the action of clicking on the favorite button for an event.
     *
     * @param tab Tab where the event as clicked.
     * @param id ID of the event.
     */
    @Override
    public void onFavoriteClick(int tab, String id) {
        Event event = eventManager.getEvent(id, false);

        switch (tab) {
            case TAB_EVENTS:
            case TAB_UPCOMING:
                if (!event.favorite) {
                    onFavoriteInsert(event);
                } else {
                    onFavoriteRemove(event);
                }
                break;
            case TAB_FAVORITE:
                onFavoriteRemove(event);
                break;
        }
    }

    /**
     * Handle the action of setting an event as favorite.
     *
     * @param event Event to be used.
     */
    private void onFavoriteInsert(Event event) {
        eventManager.updateEventFavorite(event.id, true);

        FavoritesFragment fragment = (FavoritesFragment) tabPagerAdapter.getItem(TAB_FAVORITE);
        fragment.insert(event, false);

        mainInterface.showSnackBar(R.string.favorites_added, 0, 0, null);
    }

    /**
     * Handle the action of no longer setting an event as favorite.
     *
     * @param event Event to be used.
     */
    private void onFavoriteRemove(Event event) {
        eventManager.updateEventFavorite(event.id, false);

        FavoritesFragment fragment = (FavoritesFragment) tabPagerAdapter.getItem(TAB_FAVORITE);
        fragment.remove(event);

        mainInterface.showSnackBar(R.string.favorites_removed, 0, 0, null);
    }

    /**
     * Handle the action of clicking on the delete button for an event.
     *
     * @param tab Tab where the event as clicked.
     * @param id ID of the event.
     */
    @Override
    public void onDeleteClick(int tab, final String id) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext(),
            R.style.AppTheme_Dialog_Alert);
        builder.setMessage(R.string.confirm_event_delete);

        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        eventManager.removeEvent(EventAction.ACTOR_SELF, id,
                            new EventManager.EventActionCallback() {
                                @Override
                                public void onEventAction(EventAction data) {
                                    if (data.getStatus() == EventAction.STATUS_OK) {
                                        mainInterface.showSnackBar(R.string.event_action_delete,
                                            R.string.undo, 0, new View.OnClickListener() {
                                                @Override
                                                public void onClick(View view) {

                                                }
                                            }
                                        );
                                    }
                                }
                            }
                        );
                        break;
                    case DialogInterface.BUTTON_NEGATIVE:
                        break;
                }

                dialog.dismiss();
            }
        };

        builder.setPositiveButton(R.string.yes, listener);
        builder.setNegativeButton(R.string.no, listener);

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     * Handle all event changes being reported by the Event Manager and forward it to the correct
     * fragment to respond to these changes.
     *
     * @param data Event action data.
     */
    @Override
    public void onEventBroadcast(final EventAction data) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                EventBroadcastListener listener =
                    (EventBroadcastListener) tabPagerAdapter.getItem(TAB_EVENTS);
                listener.onEventBroadcast(data);

                listener = (EventBroadcastListener) tabPagerAdapter.getItem(TAB_UPCOMING);
                listener.onEventBroadcast(data);

                listener = (EventBroadcastListener) tabPagerAdapter.getItem(TAB_FAVORITE);
                listener.onEventBroadcast(data);
            }
        });
    }

    @Override
    public int getPageTitle() {
        return R.string.dashboard;
    }

    @Override
    public ViewPager getTabLayoutViewPager() {
        return viewPager;
    }

    @Override
    public ActionButton getActionButton() {
        return null;
    }

    @Override
    public void onPageSelected() {

    }

    @Override
    public void scrollToTop() {
        Fragment fragment = tabPagerAdapter.getItem(viewPager.getCurrentItem());
        if (fragment instanceof Scrollable) {
            ((Scrollable) fragment).scrollToTop();
        }
    }
}
