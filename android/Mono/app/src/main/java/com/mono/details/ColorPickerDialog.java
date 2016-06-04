package com.mono.details;

import android.content.Context;
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

import java.util.ArrayList;
import java.util.List;

public class ColorPickerDialog extends AlertDialog implements OnItemClickListener {

    private static final int[] COLOR_IDS = {
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

    private List<Integer> colors = new ArrayList<>();

    private int color;
    private OnColorSetListener listener;

    protected ColorPickerDialog(Context context, int[] colors, int defaultColor,
            OnColorSetListener listener) {
        super(context, R.style.AppTheme_Dialog_Alert);

        for (int colorId : COLOR_IDS) {
            this.colors.add(Colors.getColor(getContext(), colorId) | 0xFF000000);
        }

        if (colors != null && colors.length > 0) {
            int index = 0;

            for (int color : colors) {
                color |= 0xFF000000;

                if (!this.colors.contains(color)) {
                    this.colors.add(index++, color);
                }
            }
        }

        defaultColor |= 0xFF000000;
        int position = this.colors.indexOf(defaultColor);

        this.color = defaultColor;
        this.listener = listener;

        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.dialog_color_picker, null, false);

        GridView gridview = (GridView) view.findViewById(R.id.grid);
        gridview.setAdapter(new ColorPickerAdapter(context, this.colors, position));
        gridview.setOnItemClickListener(this);

        setView(view);
    }

    @Override
    public void onItemClick(AdapterView parent, View view, int position, long id) {
        color = position == 0 ? 0 : colors.get(position);
        select(view);

        if (listener != null) {
            listener.onColorSet(color);
        }

        dismiss();
    }

    public void select(View view) {
        view.setBackgroundResource(R.drawable.ring);
        int color = Colors.getColor(getContext(), R.color.colorPrimary);
        view.getBackground().setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
    }

    public class ColorPickerAdapter extends BaseAdapter {

        private static final int DIMENSION_DP = 30;
        private static final int PADDING_DP = 6;

        private Context context;
        private List<Integer> colors;
        private int position;

        public ColorPickerAdapter(Context context, List<Integer> colors, int position) {
            this.context = context;
            this.colors = colors;
            this.position = position;
        }

        @Override
        public int getCount() {
            return colors.size();
        }

        @Override
        public Integer getItem(int position) {
            return colors.get(position);
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

            int color = getItem(position);
            imageView.setColorFilter(color);

            return imageView;
        }
    }

    public interface OnColorSetListener {

        void onColorSet(int color);
    }
}
