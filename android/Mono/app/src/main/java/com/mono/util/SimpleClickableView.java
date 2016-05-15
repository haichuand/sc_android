package com.mono.util;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.mono.R;

public class SimpleClickableView extends FrameLayout {

    private ViewGroup container;

    public SimpleClickableView(Context context) {
        this(context, null);
    }

    public SimpleClickableView(Context context, AttributeSet attrs) {
        this(context, attrs, 0, 0);
    }

    public SimpleClickableView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public SimpleClickableView(Context context, AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initialize(context, attrs, defStyleAttr, defStyleRes);
    }

    private void initialize(Context context, AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        container = new LinearLayout(context);
        container.setClickable(true);

        super.addView(container, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT));

        int[] attributes = {R.attr.selectableItemBackground};
        TypedArray array = getContext().obtainStyledAttributes(attributes);
        container.setBackgroundResource(array.getResourceId(0, 0));
        array.recycle();
    }

    @Override
    public void setOnClickListener(final OnClickListener listener) {
        container.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                listener.onClick(SimpleClickableView.this);
            }
        });
    }

    @Override
    public void setOnTouchListener(final OnTouchListener listener) {
        container.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                return listener.onTouch(SimpleClickableView.this, event);
            }
        });
    }

    @Override
    public void addView(View view) {
        container.addView(view);
    }

    @Override
    public void addView(View view, ViewGroup.LayoutParams params) {
        container.addView(view, params);
    }

    @Override
    public void removeAllViews() {
        container.removeAllViews();
    }
}
