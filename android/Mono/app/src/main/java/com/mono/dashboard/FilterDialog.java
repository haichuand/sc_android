package com.mono.dashboard;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.PorterDuff;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.mono.R;
import com.mono.model.EventFilter;
import com.mono.util.Colors;
import com.mono.util.Pixels;

import java.util.ArrayList;
import java.util.List;

/**
 * A dialog used to handle event filters to hide specific events from the Dashboard.
 *
 * @author Gary Ng
 */
public class FilterDialog implements DialogInterface.OnClickListener {

    private static final float MARGIN_DP = 2f;

    private Context context;
    private AlertDialog dialog;

    private ViewGroup items;
    private EditText input;
    private ImageView submit;

    private final List<EventFilter> filters = new ArrayList<>();
    private FilterDialogCallback callback;

    private FilterDialog(Context context, List<EventFilter> filters,
            FilterDialogCallback callback) {
        this.context = context;
        this.filters.addAll(filters);
        this.callback = callback;
    }

    /**
     * Create and display an instance of this dialog.
     *
     * @param context Context of application.
     * @param filters Initial filters.
     * @param callback Callback upon completion.
     * @return an instance of a dialog.
     */
    public static FilterDialog create(Context context, List<EventFilter> filters,
            FilterDialogCallback callback) {
        FilterDialog dialog = new FilterDialog(context, filters, callback);

        AlertDialog.Builder builder = new AlertDialog.Builder(context,
            R.style.AppTheme_Dialog_Alert);
        builder.setTitle(R.string.event_filters_title);
        builder.setView(dialog.onCreateView());
        builder.setCancelable(false);
        builder.setNegativeButton(R.string.close, dialog);

        dialog.dialog = builder.create();
        dialog.dialog.show();

        return dialog;
    }

    /**
     * Create the view of this dialog.
     *
     * @return an instance of the view.
     */
    protected View onCreateView() {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.dialog_filter, null, false);

        items = (ViewGroup) view.findViewById(R.id.items);

        for (EventFilter filter : filters) {
            createText(filter.text, filter.status);
        }

        input = (EditText) view.findViewById(R.id.input);

        submit = (ImageView) view.findViewById(R.id.submit);
        submit.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                onSubmit();
            }
        });

        return view;
    }

    /**
     * Create a filter text item.
     *
     * @param value Filter string.
     * @param enabled Enabled filter.
     */
    public void createText(String value, boolean enabled) {
        LayoutInflater inflater = LayoutInflater.from(context);
        final View itemView = inflater.inflate(R.layout.filter_item, null, false);

        TextView text = (TextView) itemView.findViewById(R.id.text);
        text.setText(value);

        int colorId = enabled ? R.color.gray_dark : R.color.gray_light_3;
        text.setTextColor(Colors.getColor(context, colorId));

        ImageView icon = (ImageView) itemView.findViewById(R.id.icon);
        icon.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                int index = items.indexOfChild(itemView);
                filters.remove(index);
                items.removeViewAt(index);
            }
        });

        ImageView status = (ImageView) itemView.findViewById(R.id.status);
        status.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                int index = items.indexOfChild(itemView);
                EventFilter filter = filters.get(index);
                filter.status = !filter.status;

                TextView text = (TextView) itemView.findViewById(R.id.text);
                int colorId = filter.status ? R.color.gray_dark : R.color.gray_light_3;
                text.setTextColor(Colors.getColor(context, colorId));

                ImageView status = (ImageView) itemView.findViewById(R.id.status);
                colorId = filter.status ? R.color.green : R.color.gray_light_3;
                status.setColorFilter(Colors.getColor(context, colorId), PorterDuff.Mode.SRC_ATOP);
            }
        });

        colorId = enabled ? R.color.green : R.color.gray_light_3;
        status.setColorFilter(Colors.getColor(context, colorId), PorterDuff.Mode.SRC_ATOP);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.bottomMargin = Pixels.pxFromDp(context, MARGIN_DP);

        items.addView(itemView, params);
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
            case DialogInterface.BUTTON_NEGATIVE:
                onCancel();
                break;
        }
    }

    /**
     * Handle the action of clicking on adding filter.
     */
    private void onSubmit() {
        String value = input.getText().toString().trim();

        if (!value.isEmpty()) {
            boolean exists = false;

            for (EventFilter filter : filters) {
                if (filter.text.equalsIgnoreCase(value)) {
                    exists = true;
                    break;
                }
            }

            if (!exists) {
                filters.add(new EventFilter(value, true));
                createText(value, true);
            }
        }

        input.setText("");
    }

    /**
     * Handle the action of clicking on cancel.
     */
    private void onCancel() {
        dialog.dismiss();

        if (callback != null) {
            callback.onFinish(filters);
        }
    }

    public interface FilterDialogCallback {

        void onFinish(List<EventFilter> filters);
    }
}
