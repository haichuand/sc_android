package com.mono.search;

import android.os.AsyncTask;
import android.support.v7.widget.SearchView.OnQueryTextListener;

import com.mono.EventManager;
import com.mono.R;
import com.mono.db.DatabaseHelper;
import com.mono.db.dao.ConversationDataSource;
import com.mono.model.Event;
import com.mono.model.Message;
import com.mono.util.Colors;
import com.mono.util.Common;
import com.mono.util.SimpleViewHolder;

import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class SearchHandler implements OnQueryTextListener {

    private static final int LIMIT = 10;

    private static final SimpleDateFormat DATE_FORMAT;
    private static final SimpleDateFormat DATE_FORMAT_2;
    private static final SimpleDateFormat TIME_FORMAT;
    private static final SimpleDateFormat DATE_TIME_FORMAT;

    private SearchFragment fragment;
    private EventManager manager;
    private AsyncTask<String, Void, List<SimpleViewHolder.HolderItem>> task;

    static {
        DATE_FORMAT = new SimpleDateFormat("MMM d", Locale.getDefault());
        DATE_FORMAT_2 = new SimpleDateFormat("M/d/yy", Locale.getDefault());
        TIME_FORMAT = new SimpleDateFormat("h:mm a", Locale.getDefault());
        DATE_TIME_FORMAT = new SimpleDateFormat("MMM d, h:mm a", Locale.getDefault());
    }

    public SearchHandler(SearchFragment fragment) {
        this.fragment = fragment;
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

        task = new AsyncTask<String, Void, List<SimpleViewHolder.HolderItem>>() {
            @Override
            protected List<SimpleViewHolder.HolderItem> doInBackground(String... params) {
                String query = params[0];

                List<SimpleViewHolder.HolderItem> items = new ArrayList<>();
                getEvents(query, items);
                getConversations(query, items);

                return items;
            }

            @Override
            protected void onPostExecute(List<SimpleViewHolder.HolderItem> result) {
                fragment.setItems(result);
                task = null;
            }
        }.execute(query);

        return false;
    }

    private void getEvents(String query, List<SimpleViewHolder.HolderItem> items) {
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
            item.dateTime = getEventDateString(event.startTime, timeZone);

            LocalDate currentDate = new LocalDate();
            LocalDate startDate = new LocalDate(event.startTime);
            LocalDate endDate = new LocalDate(event.endTime);

            int colorId;

            if (Common.between(currentDate, startDate, endDate)) {
                colorId = R.color.gray_dark;
            } else if (event.startTime > System.currentTimeMillis()) {
                colorId = R.color.green;
            } else {
                colorId = R.color.gray_light_3;
            }
            item.dateTimeColor = Colors.getColor(fragment.getContext(), colorId);

            items.add(item);
        }
    }

    private void getConversations(String query, List<SimpleViewHolder.HolderItem> items) {
        String[] terms = Common.explode(" ", query);
        int color = Colors.getColor(fragment.getContext(), R.color.red);

        ConversationDataSource dataSource =
            DatabaseHelper.getDataSource(fragment.getContext(), ConversationDataSource.class);
        List<Message> messages = dataSource.getMessages(query, LIMIT);

        for (Message message : messages) {
            SearchAdapter.ChatItem item = new SearchAdapter.ChatItem(message.getConversationId());
            item.title = Common.highlight(message.getConversationId(), terms, color);
            item.message = Common.highlight(message.getMessageText(), terms, color);
            item.dateTime = getChatDateString(message.getTimestamp());

            items.add(item);
        }
    }

    private String getEventDateString(long time, TimeZone timeZone) {
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

    private String getChatDateString(long time) {
        String dateString;

        LocalDate today = new LocalDate();
        LocalDate yesterday = today.minusDays(1);
        LocalDate date = new LocalDate(time);

        if (date.isEqual(today)) {
            dateString = "Today, " + TIME_FORMAT.format(time);
        } else if (date.isEqual(yesterday)) {
            dateString = "Yesterday, " + TIME_FORMAT.format(time);
        } else {
            dateString = DATE_TIME_FORMAT.format(time);
        }

        return dateString;
    }
}
