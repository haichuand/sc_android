package com.mono;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.mono.calendar.CalendarFragment;
import com.mono.events.EventsFragment;
import com.mono.map.MapFragment;
import com.mono.social.SocialFragment;
import com.mono.util.OnBackPressedListener;
import com.mono.util.SimpleTabLayout.TabPagerCallback;
import com.mono.util.SimpleTabPagerAdapter;
import com.mono.util.SimpleViewPager;

import java.util.Calendar;

public class MainFragment extends Fragment implements OnBackPressedListener, OnPageChangeListener {

    public static final int TAB_CALENDAR = 0;
    public static final int TAB_EVENTS = 1;
    public static final int TAB_SOCIAL = 2;
    public static final int TAB_MAPS = 3;

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

        Calendar calendar = Calendar.getInstance();
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        Drawable drawable = CalendarFragment.createCalendarIcon(getContext(), String.valueOf(day));
        tabPagerAdapter.add(drawable, null, new CalendarFragment());

        tabPagerAdapter.add(R.drawable.ic_list, null, new EventsFragment());
        tabPagerAdapter.add(R.drawable.ic_chat, null, new SocialFragment());
        tabPagerAdapter.add(R.drawable.ic_place, null, new MapFragment());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main, container, false);

        viewPager = (SimpleViewPager) view.findViewById(R.id.container);
        viewPager.setAdapter(tabPagerAdapter);
        viewPager.setOffscreenPageLimit(tabPagerAdapter.getCount());
        viewPager.addOnPageChangeListener(this);

        mainInterface.setDockLayoutViewPager(viewPager);
        onPageSelected(TAB_CALENDAR);

        return view;
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

            if (mainInterface != null) {
                int title = callback.getPageTitle();
                mainInterface.setToolbarTitle(title != 0 ? title : R.string.app_name);

                mainInterface.setTabLayoutViewPager(callback.getTabLayoutViewPager());

                TabPagerCallback.ActionButton actionButton = callback.getActionButton();
                if (actionButton != null) {
                    mainInterface.setActionButton(actionButton.resId, actionButton.color,
                        actionButton.listener);
                } else {
                    mainInterface.setActionButton(0, 0, null);
                }
            }

            callback.onPageSelected();
        }
    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }
}
