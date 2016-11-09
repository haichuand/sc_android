package com.mono.dashboard;

import android.view.View;

import com.mono.util.SimpleSlideView;

/**
 * This interface contain methods to be used to handle event item actions.
 *
 * @author Gary Ng
 */
public interface EventItemListener extends SimpleSlideView.SimpleSlideViewListener {

    void onSelectClick(View view, boolean value);
}
