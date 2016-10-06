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

public class DashboardFragment extends Fragment implements OnBackPressedListener,
        OnPageChangeListener, ListListener, EventBroadcastListener, TabPagerCallback, Scrollable {

    public static final int TAB_ALL = 0;
    public static final int TAB_FAVORITE = 1;

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

        Bundle args = new Bundle();
        args.putInt(EventsFragment.EXTRA_POSITION, TAB_ALL);

        Fragment fragment = new EventsFragment();
        fragment.setArguments(args);
        tabPagerAdapter.add(0, getString(R.string.all), fragment);

        args = new Bundle();
        args.putInt(EventsFragment.EXTRA_POSITION, TAB_FAVORITE);

        fragment = new FavoritesFragment();
        fragment.setArguments(args);
        tabPagerAdapter.add(0, getString(R.string.favorite), fragment);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_events, container, false);

        viewPager = (SimpleViewPager) view.findViewById(R.id.container);
        viewPager.setAdapter(tabPagerAdapter);
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
            case R.id.action_search:
                return true;
            case R.id.action_today:
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
    public void onClick(int position, String id, View view) {
        Event event = eventManager.getEvent(id, false);
        mainInterface.showEventDetails(event);
    }

    @Override
    public void onLongClick(int position, String id, View view) {

    }

    @Override
    public void onChatClick(int position, String id) {
        mainInterface.showChat(id);
    }

    @Override
    public void onFavoriteClick(int position, String id) {
        FavoritesFragment fragment = (FavoritesFragment) tabPagerAdapter.getItem(TAB_FAVORITE);

        switch (position) {
            case TAB_ALL:
                fragment.insert(eventManager.getEvent(id, false), false);

                mainInterface.setTabLayoutBadge(TAB_FAVORITE, 0,
                    String.valueOf(fragment.getCount()));
                break;
            case TAB_FAVORITE:
                fragment.remove(eventManager.getEvent(id, false));

                int count = fragment.getCount();
                mainInterface.setTabLayoutBadge(TAB_FAVORITE, 0,
                    count > 0 ? String.valueOf(count) : null);
                break;
        }
    }

    @Override
    public void onDeleteClick(int position, final String id) {
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

    @Override
    public void onEventBroadcast(final EventAction data) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                EventBroadcastListener listener =
                    (EventBroadcastListener) tabPagerAdapter.getItem(TAB_ALL);
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
