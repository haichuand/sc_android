package com.mono.util;

import android.content.Context;

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
    }
}
