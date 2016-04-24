package com.mono.details;

import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.graphics.PorterDuff;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;

import com.mono.R;
import com.mono.util.Colors;
import com.mono.util.Pixels;

public class ColorPickerDialog extends AlertDialog implements OnClickListener,
        OnItemClickListener {

    private static final int[] colorIds = {
        R.color.blue,
        R.color.blue_dark,
        R.color.brown,
        R.color.green,
        R.color.lavender,
        R.color.orange,
        R.color.purple,
        R.color.red_1,
        R.color.yellow_1
    };

    private int color;
    private OnColorSetListener listener;
    private View selected;

    protected ColorPickerDialog(Context context, int color, OnColorSetListener listener) {
        super(context, R.style.AppTheme_Dialog_Alert);
        // Default Color Position
        int position = -1;
        for (int i = 0; i < colorIds.length; i++) {
            if (Colors.getColor(context, colorIds[i]) == color) {
                position = i;
                break;
            }
        }

        this.color = color;
        this.listener = listener;

        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.dialog_color_picker, null, false);

        GridView gridview = (GridView) view.findViewById(R.id.grid);
        gridview.setAdapter(new ColorPickerAdapter(context, colorIds, position));
        gridview.setOnItemClickListener(this);

        setView(view);

        setButton(BUTTON_POSITIVE, context.getString(R.string.okay), this);
        setButton(BUTTON_NEGATIVE, context.getString(R.string.cancel), this);
    }

    @Override
    public void show() {
        super.show();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
            case BUTTON_POSITIVE:
                if (listener != null) {
                    listener.onColorSet(color);
                }
                break;
            case BUTTON_NEGATIVE:
                cancel();
                break;
        }
    }

    @Override
    public void onItemClick(AdapterView parent, View view, int position, long id) {
        color = Colors.getColor(getContext(), colorIds[position]);
        select(view);
    }

    public void select(View view) {
        if (selected != null) {
            selected.setBackground(null);
            selected = null;
        }

        view.setBackgroundResource(R.drawable.ring);
        int color = Colors.getColor(getContext(), R.color.colorPrimary);
        view.getBackground().setColorFilter(color, PorterDuff.Mode.SRC_ATOP);

        selected = view;
    }

    public static int randomColor(Context context) {
        int colorId = colorIds[(int) (Math.random() * colorIds.length) % colorIds.length];
        return Colors.getColor(context, colorId);
    }

    public class ColorPickerAdapter extends BaseAdapter {

        private static final int DIMENSION_DP = 30;
        private static final int PADDING_DP = 6;

        private Context context;
        private int[] colorIds;
        private int position;

        public ColorPickerAdapter(Context context, int[] colorIds, int position) {
            this.context = context;
            this.colorIds = colorIds;
            this.position = position;
        }

        @Override
        public int getCount() {
            return colorIds.length;
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ImageView imageView;

            if (convertView == null) {
                imageView = new ImageView(context);

                int dimension = Pixels.pxFromDp(context, DIMENSION_DP + PADDING_DP * 2);
                imageView.setLayoutParams(new GridView.LayoutParams(dimension, dimension));

                int padding = Pixels.pxFromDp(context, PADDING_DP);
                imageView.setPadding(padding, padding, padding, padding);
            } else {
                imageView = (ImageView) convertView;
            }

            if (this.position >= 0 && position == this.position) {
                select(imageView);
            }

            imageView.setImageResource(R.drawable.circle);

            int color = Colors.getColor(context, colorIds[position]);
            imageView.setColorFilter(color);

            return imageView;
        }
    }

    public interface OnColorSetListener {

        void onColorSet(int color);
    }
}
