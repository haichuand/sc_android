package com.mono.events;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;

import com.mono.EventManager;
import com.mono.EventManager.EventAction;
import com.mono.EventManager.EventActionCallback;
import com.mono.EventManager.EventBroadcastListener;
import com.mono.MainInterface;
import com.mono.R;
import com.mono.events.ListFragment.ListListener;
import com.mono.model.Event;
import com.mono.util.SimpleTabLayout.TabPagerCallback;
import com.mono.util.SimpleTabPagerAdapter;
import com.mono.util.SimpleViewPager;

import java.util.List;

public class EventsFragment extends Fragment implements OnPageChangeListener, ListListener,
        EventBroadcastListener, TabPagerCallback {

    public static final int TAB_ALL = 0;
    public static final int TAB_FAVORITE = 1;

    private MainInterface mainInterface;
    private EventManager eventManager;

    private SimpleTabPagerAdapter tabPagerAdapter;
    private SimpleViewPager viewPager;

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
        args.putInt(ListFragment.EXTRA_POSITION, TAB_ALL);

        ListFragment fragment = new ListFragment();
        fragment.setArguments(args);
        tabPagerAdapter.add(0, getString(R.string.all), fragment);

        args = new Bundle();
        args.putInt(ListFragment.EXTRA_POSITION, TAB_FAVORITE);

        fragment = new ListFragment();
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
        viewPager.setSwipeEnabled(true);

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
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
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
    public void onClick(int position, long id) {
        switch (position) {
            case TAB_ALL:
                ListFragment fragment = (ListFragment) tabPagerAdapter.getItem(TAB_FAVORITE);
                fragment.insert(0, eventManager.getEvent(id, false), true);

                mainInterface.setTabLayoutBadge(TAB_FAVORITE, 0,
                    String.valueOf(fragment.getCount()));
                break;
            case TAB_FAVORITE:
                eventManager.removeEvent(EventAction.ACTOR_SELF, id, new EventActionCallback() {
                    @Override
                    public void onEventAction(EventAction data) {
                        ListFragment fragment =
                            (ListFragment) tabPagerAdapter.getItem(TAB_FAVORITE);

                        int count = fragment.getCount();
                        mainInterface.setTabLayoutBadge(TAB_FAVORITE, 0,
                            count > 0 ? String.valueOf(count) : null);
                    }
                });
                break;
        }
    }

    @Override
    public List<? extends Event> onRefresh(int position, long startTime, long endTime) {
        switch (position) {
            case TAB_ALL:
                return eventManager.getEvents(startTime, endTime);
            case TAB_FAVORITE:
                break;
        }

        return null;
    }

    @Override
    public List<? extends Event> onMore(int position, long startTime, int limit) {
        switch (position) {
            case TAB_ALL:
                return eventManager.getEvents(startTime, limit);
            case TAB_FAVORITE:
                break;
        }

        return null;
    }

    @Override
    public void onEventBroadcast(EventAction data) {
        EventBroadcastListener listener =
            (EventBroadcastListener) tabPagerAdapter.getItem(TAB_ALL);
        listener.onEventBroadcast(data);

        listener = (EventBroadcastListener) tabPagerAdapter.getItem(TAB_FAVORITE);
        if (data.getAction() != EventAction.ACTION_CREATE) {
            listener.onEventBroadcast(data);
        }
    }

    @Override
    public void onPageSelected() {
        if (mainInterface != null) {
            ActionButton actionButton = getActionButton();
            if (actionButton != null) {
                mainInterface.setActionButton(actionButton.resId, actionButton.color,
                    actionButton.listener);
            } else {
                mainInterface.setActionButton(0, 0, null);
            }
        }
    }

    @Override
    public ViewPager getTabLayoutViewPager() {
        return viewPager;
    }

    @Override
    public ActionButton getActionButton() {
        if (viewPager.getCurrentItem() == TAB_ALL) {
            return new ActionButton(R.drawable.ic_add_white, 0, new OnClickListener() {
                @Override
                public void onClick(View view) {
                    EventManager.createDummyEvent(getContext(), null);
                }
            });
        }

        return null;
    }
}
