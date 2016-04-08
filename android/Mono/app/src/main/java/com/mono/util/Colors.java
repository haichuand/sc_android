package com.mono.util;

import android.content.Context;
import android.graphics.Color;

public class Colors {

    private Colors() {}

    public static final int BEIGE = 0xEEEBE7;
    public static final int BROWN = 0x91725F;
    public static final int BROWN_DARK = 0x52413C;
    public static final int BROWN_LIGHT = 0xCBBBAD;
    public static final int LAVENDAR = 0x8C8BA1;

    public static int getColor(Context context, int colorId) {
        return context.getResources().getColor(colorId);
    }

    public static float getLuma(int color) {
        return 0.2126f * Color.red(color) + 0.7152f * Color.green(color) +
            0.0722f * Color.blue(color);
    }
}
