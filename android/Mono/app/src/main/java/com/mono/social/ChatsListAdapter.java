package com.mono.social;

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

public class ChatsListAdapter extends RecyclerView.Adapter<ChatsListAdapter.Holder> {

    public static final int BUTTON_FAVORITE_INDEX = 0;
    public static final int BUTTON_LEAVE_INDEX = 0;

    private static final int ITEM_HEIGHT_DP = 60;

    private SimpleDataSource<ListItem> dataSource;
    private SimpleSlideViewListener listener;

    public ChatsListAdapter(SimpleSlideViewListener listener) {
        this.listener = listener;
    }

    @Override
    public Holder onCreateViewHolder(ViewGroup parent, int viewType) {
        int height = Pixels.pxFromDp(parent.getContext(), ITEM_HEIGHT_DP);

        SimpleSlideView view = new SimpleSlideView(parent.getContext());
        view.setContent(R.layout.chats_item, height, listener);

        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(Holder holder, int position) {
        ListItem item = dataSource.getItem(position);
        holder.onBind(item);
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

        public Holder(View itemView) {
            super(itemView);

            icon = (ImageView) itemView.findViewById(R.id.icon);
            title = (TextView) itemView.findViewById(R.id.title);
            description = (TextView) itemView.findViewById(R.id.description);
        }

        public void onBind(ListItem holderItem) {
            SimpleSlideView tempView = (SimpleSlideView) itemView;
            tempView.clear();
            tempView.addLeftButton(Colors.getColor(tempView.getContext(), R.color.lavender),
                R.drawable.ic_star_border_white);
            tempView.addRightButton(Colors.getColor(tempView.getContext(), R.color.red),
                R.drawable.ic_trash_white);

            icon.setImageResource(holderItem.iconResId);
            icon.setColorFilter(holderItem.iconColor | 0xFF000000);

            title.setText(holderItem.title);
            description.setText(holderItem.description);
        }

        public void onViewRecycled() {

        }
    }

    public static class ListItem {

        public String id;
        public String eventId;
        public int iconResId;
        public int iconColor;
        public String title;
        public String description;

        public ListItem(String id) {
            this.id = id;
        }
    }
}
