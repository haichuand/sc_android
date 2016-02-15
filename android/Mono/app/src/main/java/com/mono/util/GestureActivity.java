package com.mono.util;

import android.os.Bundle;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;

import com.mono.R;

public class GestureActivity extends AppCompatActivity {

    private static final int AREA_WIDTH_DP = 10;
    private static final int FLING_DELTA_X = 120;
    private static final int FLING_VELOCITY = 240;

    private GestureDetectorCompat detector;

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        enableBackGesture();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_enter_left, R.anim.slide_exit_right);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void enableBackGesture() {
        detector = new GestureDetectorCompat(this, new GestureListener());

        View view = new View(this);
        view.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                detector.onTouchEvent(event);
                return true;
            }
        });

        addContentView(view, new ViewGroup.LayoutParams(
            Math.round(AREA_WIDTH_DP * getResources().getDisplayMetrics().density),
            ViewGroup.LayoutParams.MATCH_PARENT
        ));
    }

    private class GestureListener extends SimpleOnGestureListener {
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            float deltaX = e2.getX() - e1.getX();

            if (deltaX > FLING_DELTA_X && Math.abs(velocityX) > FLING_VELOCITY) {
                onBackPressed();
                return true;
            }

            return false;
        }
    }
}
