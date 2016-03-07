package com.mono.util;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.mono.R;

public class SimpleLabelLayout extends LinearLayout {

    private TextView label;

    public SimpleLabelLayout(Context context) {
        this(context, null);
    }

    public SimpleLabelLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SimpleLabelLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public SimpleLabelLayout(Context context, AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initialize(context, attrs, defStyleAttr, defStyleRes);
    }

    private void initialize(Context context, AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.SimpleLabelLayout);

        setOrientation(VERTICAL);

        label = new TextView(context);
        label.setText(array.getText(R.styleable.SimpleLabelLayout_label));
        label.setTextColor(array.getColor(R.styleable.SimpleLabelLayout_labelColor, Color.BLACK));
        label.setTextSize(TypedValue.COMPLEX_UNIT_PX,
            array.getDimensionPixelSize(R.styleable.SimpleLabelLayout_labelSize, 15));

        addView(label);

        array.recycle();
    }
}
