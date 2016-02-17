package com.mono.util;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.design.widget.TabLayout.OnTabSelectedListener;
import android.support.design.widget.TabLayout.Tab;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.mono.R;
import com.mono.util.SimpleTabPagerAdapter.TabPagerItem;

public class SimpleTabLayout extends LinearLayout {

    private TabLayout tabLayout;
    private View lineView;

    private SimpleTabPagerAdapter adapter;

    private int tabIconColor;
    private int tabSelectedIconColor;
    private int tabTextColor;
    private int tabSelectedTextColor;

    public SimpleTabLayout(Context context) {
        this(context, null);
    }

    public SimpleTabLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SimpleTabLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public SimpleTabLayout(Context context, AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initialize(context, attrs, defStyleAttr, defStyleRes);
    }

    private void initialize(Context context, AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.SimpleTabLayout);

        setOrientation(VERTICAL);

        lineView = new View(context);
        lineView.setBackgroundColor(array.getColor(R.styleable.SimpleTabLayout_borderColor,
            Color.WHITE));

        addView(lineView, new LayoutParams(
            LayoutParams.MATCH_PARENT,
            array.getDimensionPixelSize(R.styleable.SimpleTabLayout_borderHeight, 0)
        ));

        tabLayout = new TabLayout(context);
        tabLayout.setSelectedTabIndicatorColor(array.getColor(
            R.styleable.SimpleTabLayout_tabIndicatorColor, Color.WHITE));

        tabIconColor = array.getColor(R.styleable.SimpleTabLayout_tabIconColor, Color.LTGRAY);
        tabSelectedIconColor =
            array.getColor(R.styleable.SimpleTabLayout_tabSelectedIconColor, Color.WHITE);

        tabTextColor = array.getColor(R.styleable.SimpleTabLayout_tabTextColor, Color.LTGRAY);
        tabSelectedTextColor =
            array.getColor(R.styleable.SimpleTabLayout_tabSelectedTextColor, Color.WHITE);

        addView(tabLayout, new LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.WRAP_CONTENT
        ));

        array.recycle();
    }

    public boolean isVisible() {
        return getVisibility() == View.VISIBLE;
    }

    public void show() {
        setVisibility(VISIBLE);
    }

    public void hide() {
        setVisibility(GONE);
    }

    public void setupWithViewPager(@NonNull final ViewPager viewPager,
            final boolean smoothScroll) {
        tabLayout.setupWithViewPager(viewPager);
        tabLayout.setOnTabSelectedListener(new OnTabSelectedListener() {
            private int position;

            @Override
            public void onTabSelected(Tab tab) {
                setTabColor(tab, true);

                viewPager.setCurrentItem(tab.getPosition(), smoothScroll);
                position = tab.getPosition();
            }

            @Override
            public void onTabUnselected(Tab tab) {
                setTabColor(tab, false);
            }

            @Override
            public void onTabReselected(Tab tab) {
                if (tab.getPosition() == position) {
                    Fragment fragment = adapter.getItem(tab.getPosition());

                    if (fragment instanceof Scrollable) {
                        Scrollable scrollable = (Scrollable) fragment;
                        scrollable.scrollToTop();
                    }
                }
            }
        });

        adapter = (SimpleTabPagerAdapter) viewPager.getAdapter();

        for (int i = 0; i < adapter.getCount(); i++) {
            updateTab(i);
        }
    }

    private void setTabColor(Tab tab, boolean selected) {
        View view = tab.getCustomView();

        if (view != null) {
            int iconColor = selected ? tabSelectedIconColor : tabIconColor;
            int textColor = selected ? tabSelectedTextColor : tabTextColor;

            ImageView icon = (ImageView) view.findViewById(R.id.image);
            icon.setColorFilter(iconColor, PorterDuff.Mode.SRC_ATOP);

            TextView text = (TextView) view.findViewById(R.id.text);
            text.setTextColor(textColor);
        }
    }

    public void updateTab(int position) {
        Tab tab = tabLayout.getTabAt(position);

        if (tab != null) {
            tab.setCustomView(null);

            View view = adapter.getTabView(position);
            tab.setCustomView(view);

            setTabColor(tab, position == getSelectedTabPosition());
        }
    }

    public int getSelectedTabPosition() {
        return tabLayout.getSelectedTabPosition();
    }

    public Tab selectTab(int index) {
        Tab tab = tabLayout.getTabAt(index);
        if (tab != null) {
            tab.select();
        }

        return tab;
    }

    public void setBadge(int position, int color, String value) {
        TabPagerItem item = adapter.getTabItem(position);
        item.badgeColor = color;
        item.badgeValue = value;

        updateTab(position);
    }

    public void setBorderColor(int color) {
        lineView.setBackgroundColor(color);
    }

    public interface Scrollable {

        void scrollToTop();
    }

    public interface TabPagerCallback {

        void onPageSelected();

        ViewPager getTabLayoutViewPager();

        ActionButton getActionButton();

        class ActionButton {

            public int resId;
            public int color;
            public OnClickListener listener;

            public ActionButton(int resId, int color, OnClickListener listener) {
                this.resId = resId;
                this.color = color;
                this.listener = listener;
            }
        }
    }
}
