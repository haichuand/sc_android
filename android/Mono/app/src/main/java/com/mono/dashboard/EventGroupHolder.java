package com.mono.dashboard;

import android.content.Context;
import android.support.v7.widget.CardView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.mono.R;
import com.mono.util.Pixels;
import com.mono.util.SimpleListItemView;
import com.mono.util.SimpleSlideView;
import com.mono.util.SimpleViewHolder;

/**
 * This adapter holder class is used to define the binding behavior of event groups.
 *
 * @author Gary Ng
 */
public class EventGroupHolder extends SimpleViewHolder implements EventHolder.EventItemListener {

    private static final int ITEM_HEIGHT_DP = 60;
    private static final int ITEM_PHOTO_HEIGHT_DP = 120;

    private CardView card;
    private ViewGroup header;
    private TextView title;
    private TextView date;
    private ViewGroup container;

    private EventGroupsListListener listener;

    public EventGroupHolder(View itemView, EventGroupsListListener listener) {
        super(itemView);

        card = (CardView) itemView.findViewById(R.id.card_view);
        header = (ViewGroup) itemView.findViewById(R.id.header);
        title = (TextView) itemView.findViewById(R.id.card_title);
        date = (TextView) itemView.findViewById(R.id.card_date);
        container = (ViewGroup) itemView.findViewById(R.id.container);

        this.listener = listener;
    }

    @Override
    public void onBind(HolderItem holderItem) {
        EventGroupItem item = (EventGroupItem) holderItem;

        header.setBackgroundColor(item.color);
        title.setText(item.title);

        date.setText(item.date);
        date.setTextColor(item.dateColor);

        container.removeAllViews();

        Context context = itemView.getContext();
        LinearLayout.LayoutParams params;

        for (EventItem tempItem : item.items) {
            int height;
            EventHolder holder;

            if (tempItem instanceof PhotoEventItem) {
                height = Pixels.pxFromDp(context, ITEM_PHOTO_HEIGHT_DP);

                SimpleListItemView contentView = new SimpleListItemView(context);

                SimpleSlideView view = new SimpleSlideView(context);
                view.setContent(contentView, height, this);

                holder = new PhotoEventHolder(view, contentView, this);
            } else {
                height = Pixels.pxFromDp(context, ITEM_HEIGHT_DP);

                SimpleListItemView contentView = new SimpleListItemView(context);

                SimpleSlideView view = new SimpleSlideView(context);
                view.setContent(contentView, height, this);

                holder = new EventHolder(view, contentView, this);
            }

            holder.onBind(tempItem);

            params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height);
            container.addView(holder.itemView, params);
        }
    }

    @Override
    public void onClick(View view) {
        int position = container.indexOfChild(view);
        listener.onClick(itemView, position);
    }

    @Override
    public boolean onLongClick(View view) {
        int position = container.indexOfChild(view);
        listener.onLongClick(itemView, position);

        return true;
    }

    @Override
    public void onLeftButtonClick(View view, int index) {
        int position = container.indexOfChild(view);
        listener.onLeftButtonClick(itemView, position, index);
    }

    @Override
    public void onRightButtonClick(View view, int index) {
        int position = container.indexOfChild(view);
        listener.onRightButtonClick(itemView, position, index);
    }

    @Override
    public void onGesture(View view, boolean state) {
        listener.onGesture(itemView, state);
    }

    @Override
    public void onSelectClick(View view, boolean value) {
        int position = container.indexOfChild(view);
        listener.onSelectClick(itemView, position, value);
    }

    public interface EventGroupsListListener {

        void onClick(View view, int position);

        boolean onLongClick(View view, int position);

        void onLeftButtonClick(View view, int position, int option);

        void onRightButtonClick(View view, int position, int option);

        void onGesture(View view, boolean state);

        void onSelectClick(View view, int position, boolean value);
    }
}
