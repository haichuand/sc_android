package com.mono.social;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.support.v7.widget.SearchView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.mono.AccountManager;
import com.mono.LoginActivity;
import com.mono.MainInterface;
import com.mono.R;
import com.mono.RequestCodes;
import com.mono.chat.CreateChatActivity;
import com.mono.model.Account;
import com.mono.search.SearchFragment;
import com.mono.search.SearchHandler;
import com.mono.util.OnBackPressedListener;
import com.mono.util.SimpleTabLayout.TabPagerCallback;
import com.mono.util.SimpleTabPagerAdapter;
import com.mono.util.SimpleViewPager;

public class SocialFragment extends Fragment implements OnBackPressedListener,
        OnPageChangeListener, TabPagerCallback {

    private MainInterface mainInterface;

    private SimpleTabPagerAdapter tabPagerAdapter;
    private SimpleViewPager viewPager;

    private SearchView searchView;

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

        MenuItem item = menu.findItem(R.id.action_search);
        searchView = (SearchView) MenuItemCompat.getActionView(item);

        FragmentManager manager = getActivity().getSupportFragmentManager();
        SearchFragment fragment = (SearchFragment) manager.findFragmentById(R.id.search_fragment);
        if (fragment != null) {
            fragment.setSearchView(searchView, new SearchHandler(fragment, false, true));
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.action_add:
                Account account = AccountManager.getInstance(getContext()).getAccount();
                if (account == null) {
                    Intent intent = new Intent(getContext(), LoginActivity.class);
                    startActivity(intent);
                } else {
                    Intent intent = new Intent(getContext(), CreateChatActivity.class);
                    startActivity(intent);
                }
                return true;
            case R.id.action_search:
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onBackPressed() {
        if (searchView != null && !searchView.isIconified()) {
            searchView.setIconified(true);
            searchView.onActionViewCollapsed();

            FragmentManager manager = getActivity().getSupportFragmentManager();
            SearchFragment fragment = (SearchFragment) manager.findFragmentById(R.id.search_fragment);
            if (fragment != null) {
                fragment.setVisible(false);
            }
            return true;
        }

        return false;
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
