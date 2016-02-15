package com.mono.util;

import android.content.Context;
import android.graphics.PorterDuff;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.mono.R;

import java.util.ArrayList;
import java.util.List;

public class SimpleTabPagerAdapter extends FragmentPagerAdapter {

    private final Context context;
    private final List<TabPagerItem> items = new ArrayList<>();

    public SimpleTabPagerAdapter(FragmentManager manager, Context context) {
        super(manager);
        this.context = context;
    }

    @Override
    public Fragment getItem(int position) {
        TabPagerItem item = items.get(position);
        return item.fragment;
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public CharSequence getPageTitle(int position) {
        TabPagerItem item = items.get(position);
        return item.title;
    }

    public void add(int resId, String title, Fragment fragment) {
        items.add(new TabPagerItem(resId, title, fragment));
    }

    public TabPagerItem getTabItem(int position) {
        return items.get(position);
    }

    public View getTabView(int position) {
        TabPagerItem item = getTabItem(position);

        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.simple_tab, null, false);

        ImageView image = (ImageView) view.findViewById(R.id.image);
        if (item.resId != 0) {
            image.setImageResource(item.resId);
            image.setVisibility(View.VISIBLE);
        } else {
            image.setVisibility(View.GONE);
        }

        TextView text = (TextView) view.findViewById(R.id.text);
        if (item.title != null && !item.title.isEmpty()) {
            text.setText(item.title);
            text.setVisibility(View.VISIBLE);
        } else {
            text.setVisibility(View.GONE);
        }

        ViewGroup badge = (ViewGroup) view.findViewById(R.id.badge);
        badge.getBackground().setColorFilter(item.badgeColor, PorterDuff.Mode.SRC_ATOP);

        if (item.badgeValue != null && !item.badgeValue.isEmpty()) {
            TextView badgeLabel = (TextView) view.findViewById(R.id.badge_label);
            badgeLabel.setText(item.badgeValue);

            badge.setVisibility(View.VISIBLE);
        } else {
            badge.setVisibility(View.GONE);
        }

        return view;
    }

    public static class TabPagerItem {

        public int resId;
        public String title;
        public int badgeColor;
        public String badgeValue;
        public Fragment fragment;

        public TabPagerItem(int resId, String title, Fragment fragment) {
            this.resId = resId;
            this.title = title;
            this.fragment = fragment;
        }
    }
}
