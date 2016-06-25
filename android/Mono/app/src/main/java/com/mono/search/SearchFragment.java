package com.mono.search;

import android.animation.Animator;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.SearchView.OnCloseListener;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;

import com.mono.EventManager;
import com.mono.MainInterface;
import com.mono.R;
import com.mono.model.Event;
import com.mono.search.SearchAdapter.SearchListener;
import com.mono.util.SimpleDataSource;
import com.mono.util.SimpleLinearLayoutManager;
import com.mono.util.SimpleViewHolder.HolderItem;
import com.mono.util.Views;

import java.util.ArrayList;
import java.util.List;

/**
 * A fragment that displays a list of events and chat messages that are filtered based on the
 * search query. Events selected can be viewed or edited. Chat messages will switch to the
 * chat conversation screen.
 *
 * @author Gary Ng
 */
public class SearchFragment extends Fragment implements SimpleDataSource<HolderItem>,
        SearchListener {

    private static final int FADE_DURATION = 300;

    private MainInterface mainInterface;

    private RecyclerView recyclerView;
    private SearchAdapter adapter;
    private TextView text;

    private List<HolderItem> items = new ArrayList<>();

    private Animator animator;
    private SearchView searchView;

    private boolean hasTabs;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (context instanceof MainInterface) {
            mainInterface = (MainInterface) context;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search, container, false);

        recyclerView = (RecyclerView) view.findViewById(R.id.search_list);
        recyclerView.setLayoutManager(new SimpleLinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter = new SearchAdapter(this));

        text = (TextView) view.findViewById(R.id.text);

        view.setVisibility(View.INVISIBLE);

        return view;
    }

    @Override
    public HolderItem getItem(int position) {
        return items.get(position);
    }

    @Override
    public int getCount() {
        return items.size();
    }

    /**
     * Handle the action of clicking on a search result item.
     *
     * @param id The value of the item ID.
     * @param type The type of item.
     */
    @Override
    public void onClick(String id, int type) {
        if (mainInterface == null) {
            return;
        }

        switch (type) {
            case SearchAdapter.TYPE_EVENT:
            case SearchAdapter.TYPE_PHOTO_EVENT:
                Event event = EventManager.getInstance(getContext()).getEvent(id, false);
                if (event != null) {
                    mainInterface.showEventDetails(event);
                }
                break;
            case SearchAdapter.TYPE_CHAT:
                mainInterface.showExistingChat(id);
                break;
        }
    }

    /**
     * Set the visibility of this fragment.
     *
     * @param visible The visibility status.
     */
    public void setVisible(boolean visible) {
        View view = getView();

        if (view != null) {
            View tabLayout = getActivity().findViewById(R.id.tab_layout);

            if (visible) {
                if (view.getVisibility() != View.VISIBLE) {
                    if (hasTabs = tabLayout.getVisibility() == View.VISIBLE) {
                        tabLayout.setVisibility(View.GONE);
                    }
                }

                view.setVisibility(View.VISIBLE);
            } else {
                view.setVisibility(View.INVISIBLE);

                if (hasTabs) {
                    tabLayout.setVisibility(View.VISIBLE);
                    hasTabs = false;
                }
            }
        }
    }

    /**
     * Set the visibility of the search results message.
     *
     * @param visible The visibility status.
     */
    private void setTextVisible(boolean visible) {
        text.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
    }

    /**
     * Set the events and chat message items to be displayed.
     *
     * @param items The list of items to be displayed.
     */
    public void setItems(List<HolderItem> items) {
        boolean isItemsEquals = this.items.equals(items);

        setTextVisible(items.isEmpty());

        this.items = items;
        adapter.setDataSource(this);

        if (isItemsEquals) {
            return;
        }

        if (animator != null) {
            animator.cancel();
            animator = null;
        }

        animator = Views.fade(recyclerView, 0, 1, FADE_DURATION, null);

        setVisible(true);
    }

    /**
     * Clear the search results.
     */
    public void clear() {
        adapter.setDataSource(null);
        setTextVisible(true);
    }

    /**
     * Set the appropriate action bar search view to be used as well as the handler to handle
     * the search results.
     *
     * @param view The search view.
     * @param handler The search result handler.
     */
    public void setSearchView(SearchView view, SearchHandler handler) {
        this.searchView = view;

        searchView.setOnQueryTextListener(handler);

        searchView.setOnSearchClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                setVisible(true);
            }
        });

        searchView.setOnCloseListener(new OnCloseListener() {
            @Override
            public boolean onClose() {
                searchView.onActionViewCollapsed();
                setVisible(false);
                return true;
            }
        });
    }
}
