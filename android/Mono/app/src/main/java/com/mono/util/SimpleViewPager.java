package com.mono.util;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

public class SimpleViewPager extends ViewPager {

    private boolean isSwipeEnabled;

    public SimpleViewPager(Context context) {
        this(context, null);
    }

    public SimpleViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (isSwipeEnabled) {
            return super.onTouchEvent(event);
        }

        return false;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        if (isSwipeEnabled) {
            return super.onInterceptTouchEvent(event);
        }

        return false;
    }

    public void setSwipeEnabled(boolean enabled) {
        isSwipeEnabled = enabled;
    }
}
