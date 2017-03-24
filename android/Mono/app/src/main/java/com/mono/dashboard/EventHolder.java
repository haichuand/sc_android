package com.mono.dashboard;

import android.content.Context;
import android.graphics.Typeface;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import com.mono.R;
import com.mono.util.Colors;
import com.mono.util.Common;
import com.mono.util.SimpleListItemView;
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

    protected SimpleListItemView contentView;

    private EventItemListener listener;

    static {
        TYPEFACE = Typeface.create("sans-serif-light", Typeface.NORMAL);
        TYPEFACE_BOLD = Typeface.create("sans-serif", Typeface.NORMAL);
    }

    public EventHolder(View itemView, SimpleListItemView contentView, EventItemListener listener) {
        super(itemView);

        this.contentView = contentView;
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

        CheckBox checkbox = contentView.getCheckBox();
        checkbox.setOnCheckedChangeListener(null);
        checkbox.setChecked(item.selected);
        checkbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                listener.onSelectClick(itemView, isChecked);
            }
        });

        checkbox.setVisibility(item.isSelectable ? View.VISIBLE : View.GONE);

        contentView.setIcon(item.iconResId, item.iconColor | 0xFF000000);

        contentView.setTitle(item.title);
        contentView.setTitleTextColor(item.titleColor);
        contentView.setTitleTypeface(item.titleBold ? TYPEFACE_BOLD : TYPEFACE);

        contentView.setDescription(item.description);
        contentView.setDescriptionTextColor(Colors.getColor(context, R.color.gray_light_3));

        if (!Common.isEmpty(item.startDateTime) || !Common.isEmpty(item.endDateTime)) {
            if (!Common.isEmpty(item.startDateTime)) {
                contentView.setStartTime(item.startDateTime);
                contentView.setStartTimeTextColor(item.startDateTimeColor);
            }

            if (!Common.isEmpty(item.endDateTime)) {
                contentView.setEndTimeText(item.endDateTime);
                contentView.setEndTimeTextColor(item.endDateTimeColor);
            }

            contentView.setDateTimeTypeface(item.dateTimeBold ? TYPEFACE_BOLD : TYPEFACE);
            contentView.setDateTimeVisible(true);
        } else {
            contentView.setDateTimeVisible(false);
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
