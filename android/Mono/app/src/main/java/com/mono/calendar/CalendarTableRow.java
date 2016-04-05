package com.mono.calendar;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.widget.TableRow;

import com.mono.R;
import com.mono.util.Colors;
import com.mono.util.Pixels;

public class CalendarTableRow extends TableRow {

    private static final int MARGIN_START_DP = 4;
    private static final int TEXT_SIZE_DP = 12;

    private Paint textPaint;
    private float marginStart;

    private String text;
    private int textColor;

    public CalendarTableRow(Context context) {
        this(context, null);
    }

    public CalendarTableRow(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize(context, attrs);
    }

    private void initialize(Context context, AttributeSet attrs) {
        setWillNotDraw(false);

        textPaint = new Paint();
        textPaint.setAntiAlias(true);
        textPaint.setTextSize(Pixels.pxFromDp(context, TEXT_SIZE_DP));

        textColor = Colors.getColor(context, R.color.colorPrimary);

        marginStart = Pixels.pxFromDp(context, MARGIN_START_DP);
    }

    @Override
    public void onDraw(Canvas canvas) {
        if (text == null) {
            return;
        }

        float x = marginStart;
        float y = textPaint.descent() - textPaint.ascent();

        textPaint.setColor(textColor);
        canvas.drawText(text, x, y, textPaint);
    }

    public void setText(String text) {
        this.text = text;
    }

    public void setTextColor(int color) {
        this.textColor = color;
    }
}
