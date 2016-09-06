package com.mono;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.mono.util.Pixels;

import org.acra.dialog.CrashReportDialog;

/**
 * This activity is used to customize the default crash report dialog for ACRA.
 *
 * @author Gary Ng
 */
public class CrashReportActivity extends CrashReportDialog {

    @Override
    @NonNull
    protected View buildCustomView(@Nullable Bundle savedInstanceState) {
        View root = super.buildCustomView(savedInstanceState);

        int padding = Pixels.pxFromDp(this, 10);
        root.setPadding(padding, 0, padding, 0);

        return root;
    }

    @Override
    @NonNull
    protected View getMainView() {
        TextView text = (TextView) super.getMainView();

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = Pixels.pxFromDp(this, 10);

        text.setLayoutParams(params);

        return text;
    }

    @Override
    @Nullable
    protected View getCommentLabel() {
        TextView labelView = (TextView) super.getCommentLabel();

        if (labelView != null) {
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.topMargin = Pixels.pxFromDp(this, 6);

            labelView.setLayoutParams(params);
        }

        return labelView;
    }

    @Override
    @NonNull
    protected EditText getCommentPrompt(@Nullable CharSequence savedComment) {
        EditText userCommentView = super.getCommentPrompt(savedComment);
        userCommentView.setBackgroundResource(R.drawable.input);
        userCommentView.setGravity(Gravity.TOP);
        userCommentView.setLines(4);
        userCommentView.setTextSize(16f);

        int padding = Pixels.pxFromDp(this, 6);
        userCommentView.setPadding(padding, padding, padding, padding);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = Pixels.pxFromDp(this, 6);

        userCommentView.setLayoutParams(params);

        return userCommentView;
    }
}
