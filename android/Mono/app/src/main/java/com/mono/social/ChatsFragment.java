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

import com.mono.EventManager;
import com.mono.MainInterface;
import com.mono.R;
import com.mono.chat.ConversationManager;
import com.mono.model.Conversation;
import com.mono.model.Event;
import com.mono.social.ChatsListAdapter.ListItem;
import com.mono.util.Colors;
import com.mono.util.Common;
import com.mono.util.SimpleDataSource;
import com.mono.util.SimpleLinearLayoutManager;
import com.mono.util.SimpleSlideView.SimpleSlideViewListener;
import com.mono.util.SimpleTabLayout.Scrollable;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ChatsFragment extends Fragment implements SimpleDataSource<ListItem>,
        SimpleSlideViewListener, Scrollable {

    private static final SimpleDateFormat DATE_FORMAT;
    private static final SimpleDateFormat TIME_FORMAT;

    private MainInterface mainInterface;

    private RecyclerView recyclerView;
    private SimpleLinearLayoutManager layoutManager;
    private ChatsListAdapter adapter;
    private TextView text;

    private final Map<String, ListItem> items = new HashMap<>();
    private final List<Conversation> chats = new ArrayList<>();

    static {
        DATE_FORMAT = new SimpleDateFormat("M/d/yyyy", Locale.getDefault());
        TIME_FORMAT = new SimpleDateFormat("h:mm a", Locale.getDefault());
    }

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
        chats.addAll(ConversationManager.getInstance(getContext()).getConversations());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_list_2, container, false);

        recyclerView = (RecyclerView) view.findViewById(R.id.list);
        recyclerView.setLayoutManager(layoutManager = new SimpleLinearLayoutManager(getActivity()));
        recyclerView.setAdapter(adapter = new ChatsListAdapter(this));

        adapter.setDataSource(this);

        text = (TextView) view.findViewById(R.id.text);
        text.setVisibility(chats.isEmpty() ? View.VISIBLE : View.INVISIBLE);

        return view;
    }

    @Override
    public ListItem getItem(int position) {
        ListItem item;

        Conversation chat = chats.get(position);
        String id = chat.id;

        if (items.containsKey(id)) {
            item = items.get(id);
        } else {
            item = new ListItem(id);

            item.eventId = chat.eventId;

            item.iconResId = R.drawable.ic_account_circle_48dp;
            item.iconColor = Colors.getColor(getContext(), R.color.colorPrimary);

            item.title = chat.name;

            Event event = EventManager.getInstance(getContext()).getEvent(chat.eventId, false);

            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(event.startTime);
            String date = DATE_FORMAT.format(calendar.getTime());
            String start = TIME_FORMAT.format(calendar.getTime());

            calendar.setTimeInMillis(event.endTime);
            String end = TIME_FORMAT.format(calendar.getTime());

            item.description = String.format("%s from %s to %s", date, start, end);

            items.put(id, item);
        }

        return item;
    }

    @Override
    public int getCount() {
        return chats.size();
    }

    @Override
    public void onClick(View view) {
        int position = recyclerView.getChildAdapterPosition(view);
        ListItem item = getItem(position);

        if (item != null) {
            if (mainInterface != null) {
                mainInterface.showChat(item.eventId);
            }
        }

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

        switch (index) {
            case ChatsListAdapter.BUTTON_FAVORITE_INDEX:
                onFavoriteClick(item.id);
                break;
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

        switch (index) {
            case ChatsListAdapter.BUTTON_LEAVE_INDEX:
                onLeaveClick(item.id);
                break;
        }
    }

    private void onLeaveClick(final String id) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext(),
            R.style.AppTheme_Dialog_Alert);
        builder.setMessage(R.string.confirm_chat_leave);

        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
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

    public void insert(int index, Conversation chat, boolean notify) {
        if (!chats.contains(chat)) {
            index = Common.clamp(index, 0, chats.size());
            chats.add(index, chat);

            if (notify) {
                adapter.notifyItemInserted(index);
            }
        }

        text.setVisibility(View.INVISIBLE);
    }

    public void update(Conversation chat, boolean notify) {
        int index = chats.indexOf(chat);

        if (index >= 0) {
            if (notify) {
                adapter.notifyItemChanged(index);
            }
        }
    }

    public void remove(Conversation chat, boolean notify) {
        int index = chats.indexOf(chat);

        if (index >= 0) {
            chats.remove(index);

            if (notify) {
                adapter.notifyItemRemoved(index);
            }
        }

        if (chats.isEmpty()) {
            text.setVisibility(View.VISIBLE);
        }
    }

    public void scrollTo(Conversation chat) {
        int index = chats.indexOf(chat);

        if (index >= 0) {
            recyclerView.scrollToPosition(index);
        }
    }

    @Override
    public void scrollToTop() {
        recyclerView.smoothScrollToPosition(0);
    }
}
