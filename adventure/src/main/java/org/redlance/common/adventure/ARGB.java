package org.redlance.common.adventure;

public class ARGB {
    public static int alpha(int color) {
        return color >>> 24;
    }

    public static int red(int color) {
        return color >> 16 & 255;
    }

    public static int green(int color) {
        return color >> 8 & 255;
    }

    public static int blue(int color) {
        return color & 255;
    }

    public static int color(int alpha, int red, int green, int blue) {
        return alpha << 24 | red << 16 | green << 8 | blue;
    }

    public static int scaleRGB(int color, float scale) {
        return scaleRGB(color, scale, scale, scale);
    }

    public static int scaleRGB(int color, float redScale, float greenScale, float blueScale) {
        return color(alpha(color), Math.clamp((int)((float)red(color) * redScale), 0, 255), Math.clamp((int)((float)green(color) * greenScale), 0, 255), Math.clamp((int)((float)blue(color) * blueScale), 0, 255));
    }

    public static int color(int alpha, int color) {
        return alpha << 24 | color & 16777215;
    }

    public static int as8BitChannel(float value) {
        return floor(value * 255.0F);
    }

    public static int floor(float value) {
        int i = (int)value;
        return value < (float)i ? i - 1 : i;
    }

    public static float alphaFloat(int color) {
        return from8BitChannel(alpha(color));
    }

    private static float from8BitChannel(int value) {
        return (float)value / 255.0F;
    }
}
