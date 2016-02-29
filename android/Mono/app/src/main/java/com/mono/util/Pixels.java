package com.mono.util;

import android.content.Context;
import android.util.TypedValue;

public class Pixels {

    private Pixels() {}

    public static int dpFromPx(Context context, float px) {
        return Math.round(px / context.getResources().getDisplayMetrics().density);
    }

    public static int pxFromDp(Context context, float dp) {
        return Math.round(dp * context.getResources().getDisplayMetrics().density);
    }

    public static class Display {

        public static float getDensity(Context context) {
            return context.getResources().getDisplayMetrics().density;
        }

        public static int getDPI(Context context) {
            return context.getResources().getDisplayMetrics().densityDpi;
        }

        public static int getHeight(Context context) {
            return context.getResources().getDisplayMetrics().heightPixels;
        }

        public static int getHeightDp(Context context) {
            return dpFromPx(context, context.getResources().getDisplayMetrics().heightPixels);
        }

        public static int getWidth(Context context) {
            return context.getResources().getDisplayMetrics().widthPixels;
        }

        public static int getWidthDp(Context context) {
            return dpFromPx(context, context.getResources().getDisplayMetrics().widthPixels);
        }

        public static int getStatusBarHeight(Context context) {
            int height = 0;

            int resourceId =
                context.getResources().getIdentifier("status_bar_height", "dimen", "android");

            if (resourceId > 0) {
                height = context.getResources().getDimensionPixelSize(resourceId);
            }

            return height;
        }

        public static int getActionBarHeight(Context context) {
            int height = 0;

            TypedValue value = new TypedValue();

            if (context.getTheme().resolveAttribute(android.R.attr.actionBarSize, value, true)) {
                height = TypedValue.complexToDimensionPixelSize(value.data,
                    context.getResources().getDisplayMetrics());
            }

            return height;
        }
    }
}
