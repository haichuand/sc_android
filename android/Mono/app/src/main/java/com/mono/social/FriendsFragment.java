package com.mono.social;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.mono.MainInterface;
import com.mono.R;
import com.mono.db.DatabaseHelper;
import com.mono.db.dao.AttendeeDataSource;
import com.mono.model.Attendee;
import com.mono.social.FriendsListAdapter.ListItem;
import com.mono.util.Colors;
import com.mono.util.Common;
import com.mono.util.SimpleDataSource;
import com.mono.util.SimpleLinearLayoutManager;
import com.mono.util.SimpleSlideView.SimpleSlideViewListener;
import com.mono.util.SimpleTabLayout.Scrollable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FriendsFragment extends Fragment implements SimpleDataSource<ListItem>,
        SimpleSlideViewListener, Scrollable {

    private MainInterface mainInterface;

    private RecyclerView recyclerView;
    private SimpleLinearLayoutManager layoutManager;
    private FriendsListAdapter adapter;
    private TextView text;

    private final Map<String, ListItem> items = new HashMap<>();
    private final List<Attendee> friends = new ArrayList<>();

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

        AttendeeDataSource dataSource =
            DatabaseHelper.getDataSource(getContext(), AttendeeDataSource.class);
        friends.addAll(dataSource.getAttendees());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_list_2, container, false);

        recyclerView = (RecyclerView) view.findViewById(R.id.list);
        recyclerView.setLayoutManager(layoutManager = new SimpleLinearLayoutManager(getActivity()));
        recyclerView.setAdapter(adapter = new FriendsListAdapter(this));

        adapter.setDataSource(this);

        text = (TextView) view.findViewById(R.id.text);
        text.setVisibility(friends.isEmpty() ? View.VISIBLE : View.INVISIBLE);

        return view;
    }

    @Override
    public ListItem getItem(int position) {
        ListItem item;

        Attendee friend = friends.get(position);
        String id = friend.id;

        if (items.containsKey(id)) {
            item = items.get(id);
        } else {
            item = new ListItem(id);

            item.iconResId = R.drawable.ic_account_circle_48dp;
            item.iconColor = Colors.getColor(getContext(), R.color.colorPrimary);

            item.name = friend.userName;

            items.put(id, item);
        }

        return item;
    }

    @Override
    public int getCount() {
        return friends.size();
    }

    @Override
    public void onClick(View view) {

    }

    @Override
    public boolean onLongClick(View view) {
        return false;
    }

    @Override
    public void onLeftButtonClick(View view, int index) {
        int position = recyclerView.getChildAdapterPosition(view);

        ListItem item = getItem(position);
        if (item == null) {
            return;
        }

        if (index == FriendsListAdapter.BUTTON_FAVORITE_INDEX) {
            onFavoriteClick(item.id);
        }
    }

    private void onFavoriteClick(String id) {

    }

    @Override
    public void onRightButtonClick(View view, int index) {
        int position = recyclerView.getChildAdapterPosition(view);

        ListItem item = getItem(position);
        if (item == null) {
            return;
        }

        if (index == FriendsListAdapter.BUTTON_DELETE_INDEX) {
            onDeleteClick(item.id);
        }
    }

    private void onDeleteClick(final String id) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext(),
            R.style.AppTheme_Dialog_Alert);
        builder.setMessage(R.string.confirm_friend_delete);

        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        AttendeeDataSource dataSource =
                            DatabaseHelper.getDataSource(getContext(), AttendeeDataSource.class);
                        dataSource.removeAttendee(id);

                        remove(new Attendee(id), true);
                        break;
                    case DialogInterface.BUTTON_NEGATIVE:
                        break;
                }

                dialog.dismiss();
            }
        };

        builder.setPositiveButton(R.string.yes, listener);
        builder.setNegativeButton(R.string.no, listener);

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    @Override
    public void onGesture(View view, boolean state) {
        layoutManager.setScrollEnabled(state);
    }

    public void insert(int index, Attendee friend, boolean notify) {
        if (!friends.contains(friend)) {
            index = Common.clamp(index, 0, friends.size());
            friends.add(index, friend);

            if (notify) {
                adapter.notifyItemInserted(index);
            }
        }

        text.setVisibility(View.INVISIBLE);
    }

    public void update(Attendee friend, boolean notify) {
        int index = friends.indexOf(friend);

        if (index >= 0) {
            if (notify) {
                adapter.notifyItemChanged(index);
            }
        }
    }

    public void remove(Attendee friend, boolean notify) {
        int index = friends.indexOf(friend);

        if (index >= 0) {
            friends.remove(index);

            if (notify) {
                adapter.notifyItemRemoved(index);
            }
        }

        if (friends.isEmpty()) {
            text.setVisibility(View.VISIBLE);
        }
    }

    public void scrollTo(Attendee friend) {
        int index = friends.indexOf(friend);

        if (index >= 0) {
            recyclerView.scrollToPosition(index);
        }
    }

    @Override
    public void scrollToTop() {
        recyclerView.smoothScrollToPosition(0);
    }
}
