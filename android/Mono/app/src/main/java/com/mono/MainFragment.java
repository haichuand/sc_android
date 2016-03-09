package com.mono;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.mono.calendar.CalendarFragment;
import com.mono.dummy.DummyFragment;
import com.mono.events.EventsFragment;
import com.mono.maps.MapsFragment;
import com.mono.util.OnBackPressedListener;
import com.mono.util.SimpleTabLayout.TabPagerCallback;
import com.mono.util.SimpleTabPagerAdapter;
import com.mono.util.SimpleViewPager;

public class MainFragment extends Fragment implements OnBackPressedListener, OnPageChangeListener {

    public static final int TAB_CALENDAR = 0;
    public static final int TAB_EVENTS = 1;
    public static final int TAB_MAPS = 2;
    public static final int TAB_DUMMY = 3;

    private MainInterface mainInterface;

    private SimpleTabPagerAdapter tabPagerAdapter;
    private SimpleViewPager viewPager;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (context instanceof MainInterface) {
            mainInterface = (MainInterface) context;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        tabPagerAdapter = new SimpleTabPagerAdapter(getChildFragmentManager(), getContext());
        tabPagerAdapter.add(R.drawable.ic_calendar_white, null, new CalendarFragment());
        tabPagerAdapter.add(R.drawable.ic_heart_border_white, null, new EventsFragment());
        tabPagerAdapter.add(R.drawable.ic_map_white, null, new MapsFragment());
        tabPagerAdapter.add(R.drawable.ic_chat_white, null, new DummyFragment());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main, container, false);

        viewPager = (SimpleViewPager) view.findViewById(R.id.container);
        viewPager.setAdapter(tabPagerAdapter);
        viewPager.addOnPageChangeListener(this);

        mainInterface.setDockLayoutViewPager(viewPager);
        onPageSelected(TAB_CALENDAR);

        return view;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case RequestCodes.Activity.DUMMY_WEB:
                Fragment fragment = tabPagerAdapter.getItem(TAB_DUMMY);
                fragment.onActivityResult(requestCode, resultCode, data);
                break;
        }
    }

    @Override
    public boolean onBackPressed() {
        Fragment fragment = tabPagerAdapter.getItem(viewPager.getCurrentItem());
        if (fragment instanceof OnBackPressedListener) {
            return ((OnBackPressedListener) fragment).onBackPressed();
        }

        return false;
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(int position) {
        Fragment fragment = tabPagerAdapter.getItem(position);
        if (fragment instanceof TabPagerCallback) {
            TabPagerCallback callback = (TabPagerCallback) fragment;
            callback.onPageSelected();

            if (mainInterface != null) {
                mainInterface.setTabLayoutViewPager(callback.getTabLayoutViewPager());

                TabPagerCallback.ActionButton actionButton = callback.getActionButton();
                if (actionButton != null) {
                    mainInterface.setActionButton(actionButton.resId, actionButton.color,
                        actionButton.listener);
                } else {
                    mainInterface.setActionButton(0, 0, null);
                }
            }
        }
    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }
}
