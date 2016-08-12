package com.mono.util;

import android.content.Context;
import android.graphics.Color;

/**
 * This class is used to provide helper functions to anything related to colors such as retrieving
 * the color value of a color resource, luma value of any color, lighten or darken a specific
 * color, etc.
 *
 * @author Gary Ng
 */
public class Colors {

    private Colors() {}

    /**
     * Wrapper function that invokes the deprecated function to retrieve the color value of a
     * color resource.
     *
     * @param context The value of the context.
     * @param colorId The color resource ID.
     * @return a color value.
     */
    public static int getColor(Context context, int colorId) {
        return context.getResources().getColor(colorId);
    }

    /**
     * Return a darker shade of a specific color.
     *
     * @param color The color value.
     * @param percent The amount to darken.
     * @return a color value.
     */
    public static int getDarker(int color, float percent) {
        percent = Common.clamp(percent, 0, 1);

        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);

        return Color.argb(
            Color.alpha(color),
            red - Math.round((255 - red) * percent),
            green - Math.round((255 - green) * percent),
            blue - Math.round((255 - blue) * percent)
        );
    }

    /**
     * Return a lighter shade of a specific color.
     *
     * @param color The color value.
     * @param percent The amount to lighten.
     * @return a color value.
     */
    public static int getLighter(int color, float percent) {
        percent = Common.clamp(percent, 0, 1);

        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);

        return Color.argb(
            Color.alpha(color),
            red + Math.round((255 - red) * percent),
            green + Math.round((255 - green) * percent),
            blue + Math.round((255 - blue) * percent)
        );
    }

    /**
     * Return the luma value of a specific color to determine its brightness.
     *
     * @param color The color value.
     * @return a luma value.
     */
    public static float getLuma(int color) {
        return 0.2126f * Color.red(color) + 0.7152f * Color.green(color) +
            0.0722f * Color.blue(color);
    }

    /**
     * Change the alpha value of a specific color.
     *
     * @param color The color value.
     * @param percent The amount to change.
     * @return a color value.
     */
    public static int setAlpha(int color, float percent) {
        percent = Common.clamp(percent, 0, 1);

        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);

        return Color.argb((int) (255 * percent), red, green, blue);
    }
}
