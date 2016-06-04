package com.mono.search;

import android.os.AsyncTask;
import android.support.v7.widget.SearchView.OnQueryTextListener;

import com.mono.AccountManager;
import com.mono.EventManager;
import com.mono.R;
import com.mono.chat.ConversationManager;
import com.mono.db.DatabaseHelper;
import com.mono.db.dao.AttendeeDataSource;
import com.mono.db.dao.ConversationDataSource;
import com.mono.model.Attendee;
import com.mono.model.Conversation;
import com.mono.model.Event;
import com.mono.model.Message;
import com.mono.util.Colors;
import com.mono.util.Common;
import com.mono.util.SimpleViewHolder.HolderItem;

import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class SearchHandler implements OnQueryTextListener {

    private static final int LIMIT = 10;

    private static final SimpleDateFormat DATE_FORMAT;
    private static final SimpleDateFormat DATE_FORMAT_2;
    private static final SimpleDateFormat TIME_FORMAT;

    private SearchFragment fragment;
    private boolean events;
    private boolean messages;

    private EventManager manager;
    private AsyncTask<String, Void, List<HolderItem>> task;

    static {
        DATE_FORMAT = new SimpleDateFormat("MMM d", Locale.getDefault());
        DATE_FORMAT_2 = new SimpleDateFormat("M/d/yy", Locale.getDefault());
        TIME_FORMAT = new SimpleDateFormat("h:mm a", Locale.getDefault());
    }

    public SearchHandler(SearchFragment fragment, boolean events, boolean messages) {
        this.fragment = fragment;
        this.events = events;
        this.messages = messages;

        manager = EventManager.getInstance(fragment.getContext());
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return onQuery(query);
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        return onQuery(newText);
    }

    private boolean onQuery(String query) {
        if (task != null) {
            task.cancel(true);
            task = null;
        }

        if (query.isEmpty()) {
            fragment.clear();
            return false;
        }

        task = new AsyncTask<String, Void, List<HolderItem>>() {
            @Override
            protected List<HolderItem> doInBackground(String... params) {
                String query = params[0];

                List<HolderItem> items = new ArrayList<>();
                if (events) {
                    getEvents(query, items);
                }
                if (messages) {
                    getConversations(query, items);
                }

                Collections.sort(items, new Comparator<HolderItem>() {
                    @Override
                    public int compare(HolderItem i1, HolderItem i2) {
                        return i2.sortValue.compareTo(i1.sortValue);
                    }
                });

                return items;
            }

            @Override
            protected void onPostExecute(List<HolderItem> result) {
                fragment.setItems(result);
                task = null;
            }
        }.execute(query);

        return false;
    }

    private void getEvents(String query, List<HolderItem> items) {
        String[] terms = Common.explode(" ", query);
        int color = Colors.getColor(fragment.getContext(), R.color.red);

        List<Event> events = manager.getEvents(query, LIMIT);
        for (Event event : events) {
            SearchAdapter.EventItem item = new SearchAdapter.EventItem(event.id);

            item.type = 0;
            item.iconResId = R.drawable.circle;
            item.iconColor = event.color;

            item.title = Common.highlight(event.title, terms, color);
            item.description = Common.highlight(event.description, terms, color);

            TimeZone timeZone = event.allDay ? TimeZone.getTimeZone("UTC") : TimeZone.getDefault();
            item.dateTime = getDateString(event.startTime, timeZone);
            item.dateTimeColor = getDateColor(event.startTime, event.endTime);

            item.sortValue = String.valueOf(event.startTime);

            items.add(item);
        }
    }

    private void getConversations(String query, List<HolderItem> items) {
        String[] terms = Common.explode(" ", query);
        int color = Colors.getColor(fragment.getContext(), R.color.red);

        ConversationDataSource dataSource =
            DatabaseHelper.getDataSource(fragment.getContext(), ConversationDataSource.class);
        List<Message> messages = dataSource.getMessages(query, LIMIT);

        ConversationManager manager = ConversationManager.getInstance(fragment.getContext());
        Map<String, Conversation> conversations = new HashMap<>();

        long selfId = AccountManager.getInstance(fragment.getContext()).getAccount().id;

        AttendeeDataSource attendeeDataSource =
            DatabaseHelper.getDataSource(fragment.getContext(), AttendeeDataSource.class);

        for (Message message : messages) {
            String conversationId = message.getConversationId();
            Conversation conversation = conversations.get(conversationId);

            if (conversation == null) {
                conversation = manager.getConversationById(conversationId);
                conversations.put(conversationId, conversation);
            }

            String name;
            int colorId;

            int[] colorIds = {
                R.color.blue,
                R.color.blue_dark,
                R.color.brown,
                R.color.green,
                R.color.lavender,
                R.color.orange,
                R.color.purple,
                R.color.red_1,
                R.color.yellow_1
            };

            Attendee user = attendeeDataSource.getAttendeeById(message.getUserId());
            if (user != null) {
                if (Common.compareStrings(user.id, String.valueOf(selfId))) {
                    name = fragment.getString(R.string.me);
                    colorId = R.color.blue_1;
                } else {
                    name = user.userName;
                    colorId = Common.random(colorIds);
                }
            } else {
                name = "???";
                colorId = Common.random(colorIds);
            }

            SearchAdapter.ChatItem item = new SearchAdapter.ChatItem(message.getConversationId());
            item.name = Common.highlight(name, terms, color);
            item.title = Common.highlight(message.title, terms, color);
            item.message = Common.highlight(message.getMessageText(), terms, color);
            item.dateTime = getDateString(message.getTimestamp(), TimeZone.getDefault());
            item.dateTimeColor = getDateColor(message.getTimestamp(), message.getTimestamp());
            item.color = Colors.getColor(fragment.getContext(), colorId);

            item.sortValue = String.valueOf(message.getTimestamp());

            items.add(item);
        }
    }

    private String getDateString(long time, TimeZone timeZone) {
        LocalDate currentDate = new LocalDate();

        LocalDateTime dateTime = new LocalDateTime(time);
        LocalDate date = dateTime.toLocalDate();

        SimpleDateFormat dateFormat;

        if (date.isEqual(currentDate)) {
            dateFormat = TIME_FORMAT;
        } else if (date.getYear() == currentDate.getYear()) {
            dateFormat = DATE_FORMAT;
        } else {
            dateFormat = DATE_FORMAT_2;
        }

        dateFormat.setTimeZone(timeZone);

        return dateFormat.format(dateTime.toDate());
    }

    private int getDateColor(long startTime, long endTime) {
        LocalDate currentDate = new LocalDate();
        LocalDate startDate = new LocalDate(startTime);
        LocalDate endDate = new LocalDate(endTime);

        int colorId;

        if (Common.between(currentDate, startDate, endDate)) {
            colorId = R.color.gray_dark;
        } else if (startTime > System.currentTimeMillis()) {
            colorId = R.color.green;
        } else {
            colorId = R.color.gray_light_3;
        }

        return Colors.getColor(fragment.getContext(), colorId);
    }
}
