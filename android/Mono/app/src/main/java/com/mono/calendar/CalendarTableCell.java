package com.mono.calendar;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.mono.R;

public class CalendarTableCell extends RelativeLayout {

    private TextView textView;
    private View marker;

    private boolean isToday;

    public CalendarTableCell(Context context) {
        this(context, null);
    }

    public CalendarTableCell(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CalendarTableCell(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public CalendarTableCell(Context context, AttributeSet attrs, int defStyleAttr,
                             int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initialize(context, attrs, defStyleAttr, defStyleRes);
    }

    private void initialize(Context context, AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.calendar_cell, this, true);

        textView = (TextView) view.findViewById(R.id.text);
        marker = view.findViewById(R.id.marker);
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, widthMeasureSpec);
    }

    public void setToday(boolean state) {
        isToday = state;
    }

    public void setSelected(boolean selected) {
        int color;
        int textColor;
        int resId;

        if (selected) {
            color = getResources().getColor(R.color.colorPrimary);
            textColor = Color.WHITE;
            resId = R.drawable.calendar_day_selected;
        } else {
            if (isToday) {
                color = getResources().getColor(R.color.colorPrimary);
                textColor = color;
                resId = R.drawable.calendar_today;
            } else {
                color = 0;
                textColor = getResources().getColor(R.color.gray_dark);
                resId = 0;
            }
        }

        setBackgroundResource(resId);
        if (resId != 0) {
            getBackground().setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
        }

        textView.setTextColor(textColor);
    }

    public void setText(CharSequence text) {
        textView.setText(text);
    }

    public void setMarkerVisible(boolean visible) {
        marker.setVisibility(visible ? VISIBLE : INVISIBLE);
    }
}
