package com.mono.util;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.LinearLayout;

import com.mono.R;

/**
 * This view class is used by an adapter to display event item information.
 *
 * @author Gary Ng
 */
public class SimpleListItemView extends LinearLayout {

    private static final int HEIGHT_DP = 60;
    private static final int PADDING_DP = 10;
    private static final int MARGIN_DP = 10;
    private static final int TITLE_TEXT_SIZE_DP = 16;
    private static final int TEXT_SIZE_DP = 14;
    private static final int ICON_WIDTH_DP = 30;
    private static final int DATE_WIDTH_DP = 70;
    private static final int LINE_SPACING_DP = 4;

    private TextPaint titleTextPaint;
    private TextPaint textPaint;
    private TextPaint dateTextPaint;

    private CheckBox checkBox;

    private Drawable icon;

    private CharSequence titleText;
    private int titleTextColor;

    private CharSequence descText;
    private int descTextColor;

    private boolean dateTimeVisible;

    private CharSequence startTimeText;
    private int startTimeTextColor;

    private CharSequence endTimeText;
    private int endTimeTextColor;

    private ViewGroup photos;

    public SimpleListItemView(Context context) {
        this(context, null);
    }

    public SimpleListItemView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SimpleListItemView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public SimpleListItemView(Context context, AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initialize(context, attrs, defStyleAttr, defStyleRes);
    }

    private void initialize(Context context, AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        setWillNotDraw(false);

        int height = Pixels.pxFromDp(context, HEIGHT_DP);
        setMinimumHeight(height);

        LayoutParams params = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.CENTER_VERTICAL;
        params.leftMargin = Pixels.pxFromDp(context, MARGIN_DP);

        checkBox = new CheckBox(context);
        checkBox.setPadding(0, 0, Pixels.pxFromDp(context, PADDING_DP), 0);
        addView(checkBox, params);

        setIcon(R.drawable.circle, 0);

        titleTextPaint = new TextPaint();
        titleTextPaint.setAntiAlias(true);
        titleTextPaint.setTextSize(Pixels.pxFromDp(context, TITLE_TEXT_SIZE_DP));

        textPaint = new TextPaint();
        textPaint.setAntiAlias(true);
        textPaint.setTextSize(Pixels.pxFromDp(context, TEXT_SIZE_DP));

        dateTextPaint = new TextPaint();
        dateTextPaint.setAntiAlias(true);
        dateTextPaint.setTextAlign(Paint.Align.RIGHT);
        dateTextPaint.setTextSize(Pixels.pxFromDp(context, TEXT_SIZE_DP));

        params = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height);
        params.topMargin = Pixels.pxFromDp(context, HEIGHT_DP);

        photos = new LinearLayout(context);
        photos.setBackgroundColor(Colors.getColor(context, R.color.gray_light));
        addView(photos, params);
    }

    @Override
    public void onDraw(Canvas canvas) {
        Context context = getContext();

        int height = Pixels.pxFromDp(context, HEIGHT_DP);
        int dateWidth = Pixels.pxFromDp(context, DATE_WIDTH_DP);
        int iconWidth = Pixels.pxFromDp(context, ICON_WIDTH_DP);
        int lineSpacing = Pixels.pxFromDp(context, LINE_SPACING_DP);
        int margin = Pixels.pxFromDp(context, MARGIN_DP);
        int padding = Pixels.pxFromDp(context, PADDING_DP);

        float x = padding + (checkBox.getVisibility() == VISIBLE ? checkBox.getWidth() : 0);
        float y = (height - iconWidth) / 2;

        // Icon
        icon.setBounds((int) x, (int) y, (int) (x + iconWidth), (int) (y + iconWidth));
        icon.draw(canvas);

        x += iconWidth;
        float textWidth = canvas.getWidth() - (x + margin * 2 + (dateTimeVisible ? dateWidth : 0));
        // Title
        if (!Common.isEmpty(titleText)) {
            titleTextPaint.setColor(titleTextColor);

            CharSequence text = TextUtils.ellipsize(titleText, titleTextPaint, textWidth,
                TextUtils.TruncateAt.END);
            y = padding + Math.abs(titleTextPaint.getFontMetrics().top);
            canvas.drawText(text, 0, text.length(), x + margin, y, titleTextPaint);
        }
        // Description
        if (!Common.isEmpty(descText)) {
            textPaint.setColor(descTextColor);

            CharSequence text = TextUtils.ellipsize(descText, textPaint, textWidth,
                TextUtils.TruncateAt.END);
            y = padding + Math.abs(titleTextPaint.getFontMetrics().top) + lineSpacing +
                Math.abs(textPaint.getFontMetrics().top);
            canvas.drawText(text, 0, text.length(), x + margin, y, textPaint);
        }

        if (dateTimeVisible) {
            x = canvas.getWidth() - padding;
            // Start Time
            if (!Common.isEmpty(startTimeText)) {
                dateTextPaint.setColor(startTimeTextColor);

                y = padding + Math.abs(dateTextPaint.getFontMetrics().top);
                canvas.drawText(startTimeText, 0, startTimeText.length(), x, y, dateTextPaint);
            }
            // End Time
            if (!Common.isEmpty(endTimeText)) {
                dateTextPaint.setColor(endTimeTextColor);

                y = padding + Math.abs(dateTextPaint.getFontMetrics().top) + lineSpacing +
                    Math.abs(dateTextPaint.getFontMetrics().top);
                canvas.drawText(endTimeText, 0, endTimeText.length(), x, y, dateTextPaint);
            }
        }
    }

    public CheckBox getCheckBox() {
        return checkBox;
    }

    public void setIcon(int resId, int color) {
        icon = getResources().getDrawable(resId, null);

        if (color != 0) {
            icon.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
        }
    }

    public void setTitle(String text) {
        this.titleText = text;
    }

    public void setTitleTextColor(int color) {
        this.titleTextColor = color;
    }

    public void setDescription(String text) {
        this.descText = text;
    }

    public void setDescriptionTextColor(int color) {
        this.descTextColor = color;
    }

    public void setDateTimeVisible(boolean visible) {
        this.dateTimeVisible = visible;
    }

    public void setStartTime(String startTime) {
        this.startTimeText = startTime;
    }

    public void setStartTimeTextColor(int color) {
        this.startTimeTextColor = color;
    }

    public void setEndTimeText(String endTime) {
        this.endTimeText = endTime;
    }

    public void setEndTimeTextColor(int color) {
        this.endTimeTextColor = color;
    }

    public void setTitleTypeface(Typeface typeface) {
        titleTextPaint.setTypeface(typeface);
    }

    public void setDateTimeTypeface(Typeface typeface) {
        dateTextPaint.setTypeface(typeface);
    }

    public ViewGroup getPhotos() {
        return photos;
    }

    public void setPhotosVisible(boolean visible) {
        photos.setVisibility(visible ? VISIBLE : GONE);
    }

    public void clearPhotos() {
        photos.removeAllViews();
    }

    public void addPhoto(View view, ViewGroup.LayoutParams params) {
        photos.addView(view, params);
    }
}
