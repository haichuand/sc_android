package com.mono.search;

import android.graphics.PorterDuff;
import android.support.v7.widget.RecyclerView;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.mono.R;
import com.mono.util.Colors;
import com.mono.util.SimpleClickableView;
import com.mono.util.SimpleDataSource;
import com.mono.util.SimpleViewHolder;
import com.mono.util.SimpleViewHolder.HolderItem;

public class SearchAdapter extends RecyclerView.Adapter<SimpleViewHolder> {

    public static final int TYPE_EVENT = 0;
    public static final int TYPE_CHAT = 1;

    private SimpleDataSource<HolderItem> dataSource;
    private SearchListener listener;

    public SearchAdapter(SearchListener listener) {
        this.listener = listener;
    }

    @Override
    public SimpleViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        SimpleViewHolder holder = null;

        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        SimpleClickableView container = new SimpleClickableView(parent.getContext());

        if (viewType == TYPE_EVENT) {
            View view = inflater.inflate(R.layout.search_event_item, container, false);
            container.addView(view);

            holder = new EventHolder(container);
        } else if (viewType == TYPE_CHAT) {
            View view = inflater.inflate(R.layout.search_chat_item, container, false);
            container.addView(view);

            holder = new ChatHolder(container);
        }

        return holder;
    }

    @Override
    public void onBindViewHolder(SimpleViewHolder holder, int position) {
        HolderItem item = dataSource.getItem(position);
        holder.onBind(item);
    }

    @Override
    public int getItemViewType(int position) {
        int viewType = -1;

        HolderItem item = dataSource.getItem(position);

        if (item instanceof EventItem) {
            viewType = TYPE_EVENT;
        } else if (item instanceof ChatItem) {
            viewType = TYPE_CHAT;
        }

        return viewType;
    }

    @Override
    public int getItemCount() {
        return dataSource != null ? dataSource.getCount() : 0;
    }

    @Override
    public void onViewRecycled(SimpleViewHolder holder) {
        holder.onViewRecycled();
    }

    public void setDataSource(SimpleDataSource<HolderItem> dataSource) {
        this.dataSource = dataSource;
        notifyDataSetChanged();
    }

    public static class EventItem extends HolderItem {

        public int type;
        public int iconResId;
        public int iconColor;
        public Spanned title;
        public Spanned description;
        public String dateTime;
        public int dateTimeColor;

        public EventItem(String id) {
            this.id = id;
        }
    }

    public class EventHolder extends SimpleViewHolder {

        public ImageView icon;
        public TextView title;
        public TextView description;
        public TextView date;

        public EventHolder(View itemView) {
            super(itemView);

            icon = (ImageView) itemView.findViewById(R.id.icon);
            title = (TextView) itemView.findViewById(R.id.title);
            description = (TextView) itemView.findViewById(R.id.description);
            date = (TextView) itemView.findViewById(R.id.date);
        }

        @Override
        public void onBind(HolderItem holderItem) {
            final EventItem item = (EventItem) holderItem;

            icon.setImageResource(item.iconResId);
            icon.setColorFilter(item.iconColor | 0xFF000000);

            if (item.title != null && item.title.length() > 0) {
                title.setText(item.title);
                title.setTextColor(Colors.getColor(itemView.getContext(), R.color.gray_dark));
            } else {
                title.setText(R.string.untitled);
                title.setTextColor(Colors.getColor(itemView.getContext(), R.color.gray_light_3));
            }

            description.setText(item.description);

            date.setText(item.dateTime);
            if (item.dateTimeColor != 0) {
                date.setTextColor(item.dateTimeColor);
            } else {
                date.setTextColor(Colors.getColor(itemView.getContext(), R.color.gray_light_3));
            }

            if (listener != null) {
                itemView.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        listener.onClick(item.id, TYPE_EVENT);
                    }
                });
            }
        }
    }

    public static class ChatItem extends HolderItem {

        public Spanned name;
        public Spanned title;
        public Spanned message;
        public String dateTime;
        public int dateTimeColor;
        public int direction;
        public int color;

        public ChatItem(String id) {
            this.id = id;
        }
    }

    public class ChatHolder extends SimpleViewHolder {

        public TextView title;
        public TextView date;
        public TextView name;
        public ImageView image;
        public ViewGroup bubble;
        public TextView text;

        public ChatHolder(View itemView) {
            super(itemView);

            title = (TextView) itemView.findViewById(R.id.title);
            date = (TextView) itemView.findViewById(R.id.date);
            name = (TextView) itemView.findViewById(R.id.senderName);
            image = (ImageView) itemView.findViewById(R.id.senderImage);
            bubble = (ViewGroup) itemView.findViewById(R.id.chat_bubble);
            text = (TextView) itemView.findViewById(R.id.text);
        }

        @Override
        public void onBind(HolderItem holderItem) {
            final ChatItem item = (ChatItem) holderItem;

            title.setText(item.title);

            date.setText(item.dateTime);
            if (item.dateTimeColor != 0) {
                date.setTextColor(item.dateTimeColor);
            } else {
                date.setTextColor(Colors.getColor(itemView.getContext(), R.color.gray_light_3));
            }

            int color = item.color;

            name.setText(item.name);
            image.getDrawable().setColorFilter(color, PorterDuff.Mode.SRC_ATOP);

            View arrow = bubble.findViewById(R.id.arrow);
            arrow.getBackground().setColorFilter(color, PorterDuff.Mode.SRC_ATOP);

            if (item.direction <= 0) {
                arrow.setRotation(-90);
            } else {
                itemView.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
                arrow.setRotation(90);
            }

            text.getBackground().setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
            text.setText(item.message);

            int textColor;
            if (Colors.getLuma(color) < 160) {
                textColor = Colors.getColor(itemView.getContext(), android.R.color.white);
            } else {
                textColor = Colors.getColor(itemView.getContext(), R.color.gray_dark);
            }
            text.setTextColor(textColor);

            if (listener != null) {
                itemView.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        listener.onClick(item.id, TYPE_CHAT);
                    }
                });
            }
        }
    }

    public interface SearchListener {

        void onClick(String id, int type);
    }
}
