package com.mono.util;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.mono.R;

public class SimpleQuickAction extends FrameLayout {

    private static final int ARROW_SPACING_DP = 30;
    private static final int MARGIN_DP = 4;
    private static final int PADDING_DP = 10;
    private static final float TEXT_SIZE = 16;

    private static SimpleQuickAction instance;

    private SimpleQuickActionListener listener;

    private ViewGroup container;
    private View arrow;
    private ViewGroup buttonLayout;

    private OnGlobalLayoutListener layoutListener;

    public SimpleQuickAction(Context context) {
        this(context, null);
    }

    public SimpleQuickAction(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SimpleQuickAction(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public SimpleQuickAction(Context context, AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initialize(context, attrs, defStyleAttr, defStyleRes);
    }

    private void initialize(Context context, AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        setLayoutParams(new LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ));

        setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
            }
        });

        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.simple_quick_action, this, false);

        container = (ViewGroup) view;
        arrow = view.findViewById(R.id.arrow);
        buttonLayout = (ViewGroup) view.findViewById(R.id.buttons);

        addView(view);
    }

    public static SimpleQuickAction newInstance(Context context) {
        if (instance == null) {
            instance = new SimpleQuickAction(context);
        } else {
            instance.dismiss();
        }

        instance.setVisibility(INVISIBLE);

        return instance;
    }

    public void setListener(SimpleQuickActionListener listener) {
        this.listener = listener;
    }

    public void setColor(int color) {
        arrow.getBackground().setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
        buttonLayout.getBackground().setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
    }

    public void setActions(CharSequence[] actions) {
        buttonLayout.removeAllViews();

        int padding = Pixels.pxFromDp(getContext(), PADDING_DP);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );

        for (int i = 0; i < actions.length; i++) {
            TextView button = new TextView(getContext());
            button.setPadding(padding, padding, padding, padding);
            button.setText(actions[i]);
            button.setTextColor(Color.WHITE);
            button.setTextSize(TEXT_SIZE);

            final int position = i;
            button.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (listener != null) {
                        listener.onActionClick(position);
                    }

                    dismiss();
                }
            });

            buttonLayout.addView(button, params);
        }
    }

    public void setPosition(final int left, final int top, final int offsetX, final int offsetY) {
        if (layoutListener != null) {
            getViewTreeObserver().removeOnGlobalLayoutListener(layoutListener);
        }

        getViewTreeObserver().addOnGlobalLayoutListener(
            layoutListener = new OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    layoutListener = null;
                    // Calculate Position
                    int x = left - container.getWidth() / 2;
                    int y = top;

                    ViewGroup parent = (ViewGroup) getParent();
                    int margin = Pixels.pxFromDp(getContext(), MARGIN_DP);

                    int maxY = parent.getHeight() - container.getHeight() - margin;
                    int spacing = Pixels.pxFromDp(getContext(), ARROW_SPACING_DP);
                    boolean top;
                    // Determine Above or Below
                    if (y + offsetY < maxY) {
                        y += offsetY - spacing;
                        top = true;
                    } else {
                        y -= container.getHeight() - spacing;
                        top = false;
                    }

                    x = Common.clamp(x, margin, parent.getWidth() - container.getWidth() - margin);
                    // Position Container
                    container.setX(x);
                    container.setY(y);
                    // Position Arrow
                    container.removeView(arrow);
                    arrow.setX(offsetX - x - arrow.getWidth() / 2);

                    if (top) {
                        arrow.setRotation(0);
                        container.addView(arrow, 0);
                    } else {
                        arrow.setRotation(180);
                        container.addView(arrow);
                    }
                    // Finish
                    setVisibility(VISIBLE);
                }
            }
        );
    }

    public void dismiss() {
        ViewGroup parent = (ViewGroup) getParent();
        if (parent != null) {
            parent.removeView(this);
        }

        if (listener != null) {
            listener.onDismiss();
            listener = null;
        }
    }

    public interface SimpleQuickActionListener {

        void onActionClick(int position);

        void onDismiss();
    }
}
