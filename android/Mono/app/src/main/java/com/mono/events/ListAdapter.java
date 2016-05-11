package com.mono.events;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.mono.R;
import com.mono.util.Colors;
import com.mono.util.Pixels;
import com.mono.util.SimpleDataSource;
import com.mono.util.SimpleSlideView;
import com.mono.util.SimpleSlideView.SimpleSlideViewListener;

public class ListAdapter extends RecyclerView.Adapter<ListAdapter.Holder> {

    public static final int BUTTON_CHAT_INDEX = 0;
    public static final int BUTTON_FAVORITE_INDEX = 1;
    public static final int BUTTON_DELETE_INDEX = 0;

    private static final int ITEM_HEIGHT_DP = 80;

    private SimpleDataSource<ListItem> dataSource;
    private SimpleSlideViewListener listener;

    public ListAdapter(SimpleSlideViewListener listener) {
        this.listener = listener;
    }

    @Override
    public Holder onCreateViewHolder(ViewGroup parent, int viewType) {
        int height = Pixels.pxFromDp(parent.getContext(), ITEM_HEIGHT_DP);

        SimpleSlideView view = new SimpleSlideView(parent.getContext());
        view.setContent(R.layout.list_item, height, listener);

        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(Holder holder, int position) {
        ListItem item = dataSource.getItem(position);
        holder.onBind(item);
    }

    @Override
    public int getItemViewType(int position) {
        ListItem item = dataSource.getItem(position);
        return item.type;
    }

    @Override
    public int getItemCount() {
        return dataSource == null ? 0 : dataSource.getCount();
    }

    @Override
    public void onViewRecycled(Holder holder) {
        holder.onViewRecycled();
    }

    public void setDataSource(SimpleDataSource<ListItem> dataSource) {
        this.dataSource = dataSource;
        notifyDataSetChanged();
    }

    public class Holder extends RecyclerView.ViewHolder {

        public ImageView icon;
        public TextView title;
        public TextView description;
        public TextView date;

        public Holder(View itemView) {
            super(itemView);

            icon = (ImageView) itemView.findViewById(R.id.icon);
            title = (TextView) itemView.findViewById(R.id.title);
            description = (TextView) itemView.findViewById(R.id.description);
            date = (TextView) itemView.findViewById(R.id.date);
        }

        public void onBind(ListItem holderItem) {
            SimpleSlideView tempView = (SimpleSlideView) itemView;
            tempView.clear();

            Context context = tempView.getContext();

            tempView.addLeftButton(Colors.getColor(context, R.color.lavender),
                R.drawable.ic_chat_white);
            tempView.addLeftButton(Colors.getColor(context, R.color.brown_light),
                R.drawable.ic_star_border_white);
            tempView.addRightButton(Colors.getColor(context, R.color.red),
                R.drawable.ic_trash_white);

            icon.setImageResource(holderItem.iconResId);
            icon.setColorFilter(holderItem.iconColor | 0xFF000000);

            if (holderItem.title != null && !holderItem.title.isEmpty()) {
                title.setText(holderItem.title);
                title.setTextColor(Colors.getColor(context, R.color.gray_dark));
            } else {
                title.setText(R.string.untitled);
                title.setTextColor(Colors.getColor(context, R.color.gray_light_3));
            }

            description.setText(holderItem.description);
            date.setText(holderItem.dateTime);

            if (holderItem.dateTimeColor != 0) {
                date.setTextColor(holderItem.dateTimeColor);
            } else {
                date.setTextColor(Colors.getColor(context, R.color.gray_light_3));
            }
        }

        public void onViewRecycled() {

        }
    }

    public static class ListItem {

        public static final int TYPE_EVENT = 0;

        public String id;
        public int type;
        public int iconResId;
        public int iconColor;
        public String title;
        public String description;
        public String dateTime;
        public int dateTimeColor;

        public ListItem(String id) {
            this.id = id;
        }
    }
}
