package com.mono.dashboard;

import android.content.Context;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.mono.R;
import com.mono.util.Colors;
import com.mono.util.Common;
import com.mono.util.SimpleSlideView;
import com.mono.util.SimpleViewHolder;

/**
 * This adapter holder class is used to define the binding behavior of events.
 *
 * @author Gary Ng
 */
public class EventHolder extends SimpleViewHolder {

    private static final Typeface TYPEFACE;
    private static final Typeface TYPEFACE_BOLD;

    private CheckBox checkbox;
    private ImageView icon;
    private TextView title;
    private TextView description;
    private ViewGroup date;
    private TextView startTime;
    private TextView endTime;

    private EventItemListener listener;

    static {
        TYPEFACE = Typeface.create("sans-serif-light", Typeface.NORMAL);
        TYPEFACE_BOLD = Typeface.create("sans-serif", Typeface.NORMAL);
    }

    public EventHolder(View itemView, EventItemListener listener) {
        super(itemView);

        checkbox = (CheckBox) itemView.findViewById(R.id.checkbox);
        icon = (ImageView) itemView.findViewById(R.id.icon);
        title = (TextView) itemView.findViewById(R.id.title);
        description = (TextView) itemView.findViewById(R.id.description);
        date = (ViewGroup) itemView.findViewById(R.id.date);
        startTime = (TextView) itemView.findViewById(R.id.start_time);
        endTime = (TextView) itemView.findViewById(R.id.end_time);

        this.listener = listener;
    }

    @Override
    public void onBind(HolderItem holderItem) {
        final EventItem item = (EventItem) holderItem;

        SimpleSlideView tempView = (SimpleSlideView) itemView;
        tempView.clear();

        Context context = tempView.getContext();

        tempView.addLeftButton(Colors.getColor(context, R.color.lavender),
            R.drawable.ic_chat);
        tempView.addLeftButton(Colors.getColor(context, R.color.brown_light),
            R.drawable.ic_star_border);
        tempView.addRightButton(Colors.getColor(context, R.color.red),
            R.drawable.ic_trash);

        checkbox.setOnCheckedChangeListener(null);
        checkbox.setChecked(item.selected);
        checkbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                listener.onSelectClick(itemView, isChecked);
            }
        });

        checkbox.setVisibility(item.isSelectable ? View.VISIBLE : View.GONE);

        icon.setImageResource(item.iconResId);
        icon.setColorFilter(item.iconColor | 0xFF000000);

        title.setText(item.title);
        title.setTextColor(item.titleColor);
        title.setTypeface(item.titleBold ? TYPEFACE_BOLD : TYPEFACE);

        description.setText(item.description);

        if (!Common.isEmpty(item.startDateTime) || !Common.isEmpty(item.endDateTime)) {
            if (!Common.isEmpty(item.startDateTime)) {
                startTime.setText(item.startDateTime);
                startTime.setTextColor(item.startDateTimeColor);
                startTime.setTypeface(item.dateTimeBold ? TYPEFACE_BOLD : TYPEFACE);
            }

            if (!Common.isEmpty(item.endDateTime)) {
                endTime.setText(item.endDateTime);
                endTime.setTextColor(item.endDateTimeColor);
                endTime.setTypeface(item.dateTimeBold ? TYPEFACE_BOLD : TYPEFACE);
            }

            date.setVisibility(View.VISIBLE);
        } else {
            date.setVisibility(View.GONE);
        }
    }

    public interface ListListener {

        void onClick(int tab, String id, View view);

        void onLongClick(int tab, String id, View view);

        void onChatClick(int tab, String id);

        void onFavoriteClick(int tab, String id);

        void onDeleteClick(int tab, String id);
    }

    public interface EventItemListener extends SimpleSlideView.SimpleSlideViewListener {

        void onSelectClick(View view, boolean value);
    }
}
