package dev.adapt.v3.util.render;

import java.awt.Color;

public final class ColorUtility {
    public static int getAlpha(int color) {
        return color >>> 24;
    }

    public static int withAlpha(int color, int color2) {
        return Math.max(0, Math.min(255, color2)) << 24 | color & 16777215;
    }

    public static int withAlphaFloat(int color, float alpha) {
        return withAlpha(color, (int)(alpha * 255.0F));
    }

    public static int scaleAlpha(int color, float alpha) {
        return withAlpha(color, (int)(getAlpha(color) * Math.max(0.0F, Math.min(1.0F, alpha))));
    }

    public static int lerpColor(int color, int color2, float value) {
        value = Math.max(0.0F, Math.min(1.0F, value));
        int i = color >>> 24;
        int j = color >> 16 & 0xFF;
        int k = color >> 8 & 0xFF;
        int l = color & 0xFF;
        int i1 = color2 >>> 24;
        int j1 = color2 >> 16 & 0xFF;
        int k1 = color2 >> 8 & 0xFF;
        int l1 = color2 & 0xFF;
        return (int)(i + (i1 - i) * value) << 24 | (int)(j + (j1 - j) * value) << 16 | (int)(k + (k1 - k) * value) << 8 | (int)(l + (l1 - l) * value);
    }

    public static int fromHsb(float value, float value2, float value3) {
        return 0xFF000000 | Color.HSBtoRGB(value, value2, value3) & 16777215;
    }

    public static float[] toHsb(int value) {
        return Color.RGBtoHSB(value >> 16 & 0xFF, value >> 8 & 0xFF, value & 0xFF, null);
    }

    public static int lighten(int first, float second) {
        return lerpColor(first, first & 0xFF000000 | 16777215, second);
    }
}
