package com.mono.util;

import android.animation.Animator;
import android.content.Context;
import android.support.v4.view.GestureDetectorCompat;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.mono.R;

import java.util.ArrayList;
import java.util.List;

public class SimpleSlideView extends RelativeLayout implements View.OnTouchListener {

    private static final int DELTA_X = 10;
    private static final int SLIDE_DURATION = 300;

    private static final int INITIAL_ID = 1000;
    private static final int BUTTON_WIDTH_DP = 60;

    private static final int BOUND_THRESHOLD_DP = BUTTON_WIDTH_DP / 2;

    private static final int STATE_NONE = 0;
    private static final int STATE_LEFT = 1;
    private static final int STATE_RIGHT = 2;

    private ViewGroup buttonLayout;
    private ViewGroup content;

    private GestureDetectorCompat detector;
    private Animator animator;

    private SimpleSlideViewListener listener;

    private final List<ImageView> leftButtons = new ArrayList<>();
    private final List<ImageView> rightButtons = new ArrayList<>();

    private int buttonWidth;

    private float startX;
    private float leftBound;
    private float rightBound;
    private int boundThreshold;
    private int state;

    public SimpleSlideView(Context context) {
        this(context, null);
    }

    public SimpleSlideView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SimpleSlideView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public SimpleSlideView(Context context, AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initialize(context, attrs, defStyleAttr, defStyleRes);
    }

    private void initialize(Context context, AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.simple_slide_view, this, true);

        buttonLayout = (ViewGroup) view.findViewById(R.id.buttons);

        content = (ViewGroup) view.findViewById(R.id.content);
        detector = new GestureDetectorCompat(context, new GestureListener(this, content));
        content.setOnTouchListener(this);

        buttonWidth = Pixels.pxFromDp(context, BUTTON_WIDTH_DP);
        boundThreshold = Pixels.pxFromDp(context, BOUND_THRESHOLD_DP);
    }

    public void setContent(int resId, int height, SimpleSlideViewListener listener) {
        content.removeAllViews();

        LayoutInflater inflater = LayoutInflater.from(getContext());
        View view = inflater.inflate(resId, content, false);
        content.addView(view);

        ViewGroup.LayoutParams params = buttonLayout.getLayoutParams();
        params.height = height;
        buttonLayout.setLayoutParams(params);

        this.listener = listener;
    }

    public void clear() {
        buttonLayout.removeAllViews();
        leftButtons.clear();
        rightButtons.clear();

        resetPosition(false);
    }

    public void addLeftButton(int color, int resId) {
        int id = INITIAL_ID;
        if (!leftButtons.isEmpty()) {
            ImageView prev = leftButtons.get(leftButtons.size() - 1);
            id = prev.getId();
        }

        ImageView view = createButton(id - 1, color, resId);

        final int position = leftButtons.size();
        view.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                listener.onLeftButtonClick(SimpleSlideView.this, position);
                resetPosition(true);
            }
        });

        LayoutParams params = (RelativeLayout.LayoutParams) view.getLayoutParams();
        params.addRule(RelativeLayout.RIGHT_OF, id);
        view.setLayoutParams(params);

        buttonLayout.addView(view);
        leftButtons.add(view);

        rightBound = leftButtons.size() * buttonWidth;
    }

    public void addRightButton(int color, int resId) {
        int id = INITIAL_ID;
        if (!rightButtons.isEmpty()) {
            ImageView prev = rightButtons.get(rightButtons.size() - 1);
            id = prev.getId();
        }

        ImageView view = createButton(id + 1, color, resId);

        final int position = rightButtons.size();
        view.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                listener.onRightButtonClick(SimpleSlideView.this, position);
                resetPosition(true);
            }
        });

        LayoutParams params = (RelativeLayout.LayoutParams) view.getLayoutParams();
        params.addRule(RelativeLayout.ALIGN_PARENT_END);
        params.addRule(RelativeLayout.LEFT_OF, id);
        view.setLayoutParams(params);

        buttonLayout.addView(view);
        rightButtons.add(view);

        leftBound = -rightButtons.size() * buttonWidth;
    }

    private ImageView createButton(int id, int color, int resId) {
        ImageView view = new ImageView(getContext());
        view.setId(id);
        view.setBackgroundColor(color);
        view.setImageResource(resId);
        view.setScaleType(ImageView.ScaleType.CENTER);

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
            buttonWidth,
            ViewGroup.LayoutParams.MATCH_PARENT
        );
        view.setLayoutParams(params);

        return view;
    }

    @Override
    public boolean onTouch(View view, MotionEvent event) {
        boolean consumed = detector.onTouchEvent(event);

        int action = event.getAction();

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                startX = event.getX();
                break;
            case MotionEvent.ACTION_MOVE:
                if (listener != null) {
                    if (Math.abs(event.getX() - startX) > DELTA_X) {
                        listener.onGesture(view, false);
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (!consumed) {
                    float x = view.getX();

                    if (state == STATE_LEFT && x > leftBound + boundThreshold ||
                            state == STATE_RIGHT && x < rightBound - boundThreshold) {
                        resetPosition(true);
                    } else if (x < -boundThreshold) {
                        slideToPosition(view, leftBound, true);
                        state = STATE_LEFT;
                    } else if (x > boundThreshold) {
                        slideToPosition(view, rightBound, true);
                        state = STATE_RIGHT;
                    } else {
                        resetPosition(true);
                    }
                }

                if (listener != null) {
                    listener.onGesture(view, true);
                }
                break;
        }

        return false;
    }

    public void slideToPosition(View view, float x, boolean smooth) {
        if (animator != null) {
            animator.cancel();
        }

        if (smooth) {
            animator = Views.translateX(view, (int) x, SLIDE_DURATION, null);
        } else {
            view.setX(x);
        }
    }

    public void resetPosition(boolean smooth) {
        slideToPosition(content, 0, smooth);
        state = STATE_NONE;
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {

        private View root;
        private View view;

        public GestureListener(View root, View view) {
            this.root = root;
            this.view = view;
        }

        @Override
        public boolean onSingleTapUp(MotionEvent event) {
            if (state == STATE_NONE) {
                listener.onClick(root);
            } else {
                resetPosition(true);
            }

            return true;
        }

        @Override
        public void onLongPress(MotionEvent event) {
            if (state == STATE_NONE) {
                listener.onLongClick(root);
            }
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            float deltaX = e2.getX() - e1.getX();

            float x = Common.clamp(view.getX() + deltaX, leftBound, rightBound);
            slideToPosition(view, x, false);

            return false;
        }
    }

    public interface SimpleSlideViewListener {

        void onClick(View view);

        boolean onLongClick(View view);

        void onLeftButtonClick(View view, int index);

        void onRightButtonClick(View view, int index);

        void onGesture(View view, boolean state);
    }
}
