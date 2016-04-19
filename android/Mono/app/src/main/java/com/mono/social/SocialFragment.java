package com.mono.social;

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
import android.view.ViewGroup;

import com.mono.MainInterface;
import com.mono.R;
import com.mono.util.SimpleTabLayout.TabPagerCallback;
import com.mono.util.SimpleTabPagerAdapter;
import com.mono.util.SimpleViewPager;

public class SocialFragment extends Fragment implements OnPageChangeListener, TabPagerCallback {

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
        setHasOptionsMenu(true);

        tabPagerAdapter = new SimpleTabPagerAdapter(getChildFragmentManager(), getContext());
        tabPagerAdapter.add(0, getString(R.string.chats), new ChatsFragment());
        tabPagerAdapter.add(0, getString(R.string.friends), new FriendsFragment());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_events, container, false);

        viewPager = (SimpleViewPager) view.findViewById(R.id.container);
        viewPager.setAdapter(tabPagerAdapter);
        viewPager.addOnPageChangeListener(this);

        return view;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.social, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.action_add:
                return true;
            case R.id.action_search:
                return true;
        }

        return super.onOptionsItemSelected(item);
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
    public int getPageTitle() {
        return R.string.social;
    }

    @Override
    public ViewPager getTabLayoutViewPager() {
        return viewPager;
    }

    @Override
    public ActionButton getActionButton() {
        return null;
    }

    @Override
    public void onPageSelected() {

    }
}
