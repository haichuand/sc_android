package com.mono.events;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.mono.R;
import com.mono.util.SimpleDataSource;

public class ListAdapter extends RecyclerView.Adapter<ListAdapter.Holder> {

    private SimpleDataSource<ListItem> dataSource;
    private ListClickListener listener;

    public ListAdapter(ListClickListener listener) {
        this.listener = listener;
    }

    @Override
    public Holder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.list_item, parent, false);

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

        public ImageView circle;
        public TextView title;
        public TextView date;
        public TextView description;

        public Holder(View itemView) {
            super(itemView);

            circle = (ImageView) itemView.findViewById(R.id.circle);

            title = (TextView) itemView.findViewById(R.id.title);
            date = (TextView) itemView.findViewById(R.id.date);

            description = (TextView) itemView.findViewById(R.id.description);
        }

        public void onBind(ListItem holderItem) {
            itemView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    listener.onClick(itemView);
                }
            });

            circle.setColorFilter(holderItem.color | 0xFF000000);

            title.setText(holderItem.title);
            date.setText(holderItem.date);

            description.setText(holderItem.description);
        }

        public void onViewRecycled() {

        }
    }

    public static class ListItem {

        public static final int TYPE_EVENT = 0;

        public long id;
        public int type;
        public int color;
        public String title;
        public String date;
        public String description;

        public ListItem(long id) {
            this.id = id;
        }
    }

    public interface ListClickListener {

        void onClick(View view);
    }
}
