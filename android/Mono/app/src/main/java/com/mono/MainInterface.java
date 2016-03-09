package com.mono;

import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.view.View.OnClickListener;

import com.mono.model.Event;

public interface MainInterface {

    void setTabLayoutViewPager(ViewPager viewPager);

    void setTabLayoutBadge(int position, int color, String value);

    void setDockLayoutViewPager(ViewPager viewPager);

    void setActionButton(int resId, int color, OnClickListener listener);

    void showHome();

    void showSettings();

    void showWebActivity(Fragment fragment, int requestCode);

    void showEventDetails(Event event);

    void showChat(long id);
}
