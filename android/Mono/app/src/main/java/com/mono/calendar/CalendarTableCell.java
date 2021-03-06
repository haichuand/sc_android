package com.mono.calendar;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.util.AttributeSet;
import android.widget.RelativeLayout;

import com.mono.R;
import com.mono.util.Colors;
import com.mono.util.Pixels;

public class CalendarTableCell extends RelativeLayout {

    public static final int MAX_MARKER_COLORS = 2;

    private static final int MARKER_MARGIN_TOP_DP = 4;
    private static final int MARKER_WIDTH_DP = 6;
    private static final int TEXT_SIZE_DP = 16;

    private Paint textPaint;
    private Paint markerPaint;
    private float markerMarginTop;
    private float markerWidth;

    private String text;
    private int textColor;
    private int[] markerColor;

    private boolean isToday;

    private int prevColor;
    private int prevTextColor;
    private int prevResId;

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
        setWillNotDraw(false);

        textPaint = new Paint();
        textPaint.setAntiAlias(true);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(Pixels.pxFromDp(context, TEXT_SIZE_DP));

        textColor = Colors.getColor(context, R.color.gray_dark);

        markerPaint = new Paint();
        markerPaint.setAntiAlias(true);

        markerMarginTop = Pixels.pxFromDp(context, MARKER_MARGIN_TOP_DP);
        markerWidth = Pixels.pxFromDp(context, MARKER_WIDTH_DP);
    }

    @Override
    public void onDraw(Canvas canvas) {
        float x = canvas.getWidth() / 2f;
        float y = (canvas.getHeight() - textPaint.descent() - textPaint.ascent()) / 2;

        textPaint.setColor(textColor);
        canvas.drawText(text, x, y, textPaint);

        if (markerColor != null && markerColor.length > 0) {
            float markerWidth = this.markerWidth;
            float offset = markerWidth * 0.69f;
            float left = x - (markerWidth - (markerColor.length - 1) * offset) / 2;
            float top = y + textPaint.descent() + markerMarginTop - markerWidth / 2;

            for (int i = markerColor.length - 1; i >= 0; i--) {
                markerPaint.setColor(markerColor[i]);
                canvas.drawOval(left, top, left + markerWidth, top + markerWidth, markerPaint);

                left -= offset;
            }
        }
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, widthMeasureSpec);
    }

    public void setToday(boolean state) {
        isToday = state;

        prevColor = Colors.getColor(getContext(), R.color.colorPrimary);
        prevTextColor = Colors.getColor(getContext(), R.color.colorPrimary);
        prevResId = R.drawable.calendar_today;

        setBackground(prevResId, prevColor);
        setTextColor(prevTextColor);
    }

    public void setSelected(boolean selected) {
        if (selected) {
            prevColor = Colors.getColor(getContext(), R.color.colorPrimary);
            prevTextColor = Color.WHITE;
            prevResId = R.drawable.calendar_day_selected;
        } else {
            if (isToday) {
                setToday(true);
                return;
            } else {
                prevColor = 0;
                prevTextColor = Colors.getColor(getContext(), R.color.gray_dark);
                prevResId = 0;
            }
        }

        setBackground(prevResId, prevColor);
        setTextColor(prevTextColor);
    }

    public void setBackground(int resId, int color) {
        setBackgroundResource(resId);
        if (resId != 0) {
            getBackground().setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
        }
    }

    public void setText(String text) {
        this.text = text;
    }

    public void setTextColor(int color) {
        textColor = color;
    }

    public void setMarkerColor(int[] colors) {
        markerColor = colors;
    }

    public void clearMarkerColor() {
        markerColor = null;
    }

    public void setLastStyle() {
        setBackground(prevResId, prevColor);
        setTextColor(prevTextColor);
    }
}
