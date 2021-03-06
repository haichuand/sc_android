package com.mono;

import android.graphics.drawable.Drawable;
import android.support.v4.view.ViewPager;
import android.view.View.OnClickListener;
import android.widget.AdapterView.OnItemSelectedListener;

import com.mono.model.Event;

/**
 * An interface used primarily by the Main Activity to centralize common actions to be used by
 * other classes.
 *
 * @author Gary Ng
 */
public interface MainInterface {

    void setToolbarTitle(int resId);

    void setToolbarTitle(CharSequence resId);

    void setToolbarSpinner(CharSequence[] items, int position, OnItemSelectedListener listener);

    void setTabLayoutViewPager(ViewPager viewPager);

    void setTabLayoutBadge(int position, int color, String value);

    void setDockLayoutViewPager(ViewPager viewPager);

    void setDockLayoutDrawable(int position, Drawable drawable);

    void setActionButton(int resId, int color, OnClickListener listener);

    void showSnackBar(int resId, int actionResId, int actionColor, OnClickListener listener);

    void requestSync(boolean force);

    void showHome();

    void showIntro();

    void showLogin();

    void showContacts();

    void showSettings();

    void showEventDetails(Event event);

    void showChat(String eventId);

    //used to get conversation not associated with any event
    void showExistingChat(String conversationId);

    void showLocationSetting();

    void setEditMode(EditModeListener listener);

    interface EditModeListener {

        void onFinish();
    }
}
