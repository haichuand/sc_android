package com.mono;

import android.graphics.drawable.Drawable;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.view.View.OnClickListener;
import android.widget.AdapterView.OnItemSelectedListener;

import com.mono.model.Event;

public interface MainInterface {

    void setToolbarTitle(int resId);

    void setToolbarSpinner(CharSequence[] items, int position, OnItemSelectedListener listener);

    void setTabLayoutViewPager(ViewPager viewPager);

    void setTabLayoutBadge(int position, int color, String value);

    void setDockLayoutViewPager(ViewPager viewPager);

    void setDockLayoutDrawable(int position, Drawable drawable);

    void setActionButton(int resId, int color, OnClickListener listener);

    void showSnackBar(int resId, int actionResId, int actionColor, OnClickListener listener);

    void showHome();

    void showSettings();

    void showWebActivity(Fragment fragment, int requestCode);

    void showEventDetails(Event event);

    void showChat(String eventId);
}
