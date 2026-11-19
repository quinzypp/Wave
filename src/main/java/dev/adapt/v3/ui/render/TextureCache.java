package dev.adapt.v3.ui.render;

import com.mojang.blaze3d.platform.NativeImage;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

public final class TextureCache {
    public static final Map<Long, Identifier> CACHE = new HashMap<>();

    public static Identifier registerGenerated(String text, NativeImage nativeImage) {
        Identifier identifier = Identifier.fromNamespaceAndPath("adaptv3", "generated/" + text);
        Minecraft.getInstance().getTextureManager().register(identifier, new SmoothTexture("adaptv3:" + text, nativeImage));
        return identifier;
    }

    public static int whiteWithAlpha(float alpha) {
        int i = Math.max(0, Math.min(255, Math.round(alpha * 255.0F)));
        return i << 24 | 16777215;
    }

    public static Identifier getCorner(int value) {
        return CACHE.computeIfAbsent(1099511627776L | value, time -> {
            NativeImage nativeimage = new NativeImage(value, value, false);
            byte b0 = 4;

            for (int i = 0; i < value; i++) {
                for (int j = 0; j < value; j++) {
                    int k = 0;

                    for (int l = 0; l < 4; l++) {
                        for (int i1 = 0; i1 < 4; i1++) {
                            double d0 = j + (i1 + 0.5) / 4.0 - value;
                            double d1 = i + (l + 0.5) / 4.0 - value;
                            if (d0 * d0 + d1 * d1 <= (double)value * value) {
                                k++;
                            }
                        }
                    }

                    nativeimage.setPixel(j, i, whiteWithAlpha(k / 16.0F));
                }
            }

            return registerGenerated("corner_" + value, nativeimage);
        });
    }

    public static Identifier getRing(int first, int second) {
        return getRing(first, (float)second);
    }

    public static Identifier getRing(int first, float second) {
        int i = Math.round(second * 100.0F);
        return CACHE.computeIfAbsent(2199023255552L | (long)first << 20 | i, time -> {
            int j = first * 2;
            NativeImage nativeimage = new NativeImage(j, j, false);
            byte b0 = 4;
            double d0 = Math.max(0.0F, first - second);

            for (int k = 0; k < j; k++) {
                for (int l = 0; l < j; l++) {
                    int i1 = 0;

                    for (int j1 = 0; j1 < 4; j1++) {
                        for (int k1 = 0; k1 < 4; k1++) {
                            double d1 = l + (k1 + 0.5) / 4.0 - first;
                            double d2 = k + (j1 + 0.5) / 4.0 - first;
                            double d3 = d1 * d1 + d2 * d2;
                            if (d3 <= (double)first * first && d3 >= d0 * d0) {
                                i1++;
                            }
                        }
                    }

                    nativeimage.setPixel(l, k, whiteWithAlpha(i1 / 16.0F));
                }
            }

            return registerGenerated("ring_" + first + "_" + i, nativeimage);
        });
    }

    public static int getLineTexHeight(float value) {
        return (int)Math.ceil(value) + 2;
    }

    public static Identifier getLine(float value) {
        int i = Math.round(value * 100.0F);
        return CACHE.computeIfAbsent(5497558138880L | i, time -> {
            byte b0 = 4;
            int j = getLineTexHeight(value);
            NativeImage nativeimage = new NativeImage(4, j, false);
            double d0 = j / 2.0;
            double d1 = d0 - value / 2.0;
            double d2 = d0 + value / 2.0;

            for (int k = 0; k < j; k++) {
                double d3 = Math.max(0.0, Math.min(k + 1, d2) - Math.max(k, d1));

                for (int l = 0; l < 4; l++) {
                    nativeimage.setPixel(l, k, whiteWithAlpha((float)d3));
                }
            }

            return registerGenerated("line_" + i, nativeimage);
        });
    }

    public static Identifier getGlowLine(int value) {
        return CACHE.computeIfAbsent(6597069766656L | value, time -> {
            byte b0 = 4;
            int i = Math.max(3, value);
            NativeImage nativeimage = new NativeImage(4, i, false);
            double d0 = i / 2.0;
            double d1 = i / 5.0;

            for (int j = 0; j < i; j++) {
                double d2 = j + 0.5 - d0;
                double d3 = Math.max(0.0, 1.0 - Math.abs(d2) / d0);
                float f = (float)(Math.exp(-d2 * d2 / (2.0 * d1 * d1)) * Math.min(1.0, d3 * 4.0));

                for (int k = 0; k < 4; k++) {
                    nativeimage.setPixel(k, j, whiteWithAlpha(f));
                }
            }

            return registerGenerated("glow_line_" + value, nativeimage);
        });
    }

    public static Identifier getBlob() {
        return CACHE.computeIfAbsent(4398046511104L, time -> {
            short short1 = 128;
            NativeImage nativeimage = new NativeImage(128, 128, false);
            double d0 = 64.0;
            double d1 = 19.692307692307693;

            for (int i = 0; i < 128; i++) {
                for (int j = 0; j < 128; j++) {
                    double d2 = j + 0.5 - 64.0;
                    double d3 = i + 0.5 - 64.0;
                    double d4 = d2 * d2 + d3 * d3;
                    double d5 = Math.max(0.0, 1.0 - Math.sqrt(d4) / 64.0);
                    nativeimage.setPixel(j, i, whiteWithAlpha((float)(Math.exp(-d4 / 775.5739644970415) * Math.min(1.0, d5 * 6.0))));
                }
            }

            return registerGenerated("blob", nativeimage);
        });
    }

    public static int getShadowPad(int first, int second) {
        return second + Math.max(first, second);
    }

    public static Identifier getShadowNineSlice(int first, int second) {
        return CACHE.computeIfAbsent(3298534883328L | (long)first << 16 | second, time -> {
            int i = getShadowPad(first, second);
            int j = i * 2 + 1;
            NativeImage nativeimage = new NativeImage(j, j, false);
            double d0 = (j - 2.0 * second) / 2.0;
            double d1 = j / 2.0;
            double d2 = Math.max(0.5, second / 2.6);

            for (int k = 0; k < j; k++) {
                for (int l = 0; l < j; l++) {
                    double d3 = Math.abs(l + 0.5 - d1) - (d0 - first);
                    double d4 = Math.abs(k + 0.5 - d1) - (d0 - first);
                    double d5 = Math.hypot(Math.max(d3, 0.0), Math.max(d4, 0.0));
                    double d6 = d5 + Math.min(Math.max(d3, d4), 0.0) - first;
                    nativeimage.setPixel(l, k, whiteWithAlpha((float)(0.5 * erfc(d6 / (d2 * Math.sqrt(2.0))))));
                }
            }

            return registerGenerated("shadow_" + first + "_" + second, nativeimage);
        });
    }

    public static double erfc(double value) {
        double d0 = Math.abs(value);
        double d1 = 1.0 / (1.0 + 0.5 * d0);
        double d2 = d1
            * Math.exp(
                -d0 * d0
                    - 1.26551223
                    + d1
                        * (
                            1.00002368
                                + d1
                                    * (
                                        0.37409196
                                            + d1
                                                * (
                                                    0.09678418
                                                        + d1
                                                            * (
                                                                -0.18628806
                                                                    + d1
                                                                        * (
                                                                            0.27886807
                                                                                + d1 * (-1.13520398 + d1 * (1.48851638 + d1 * (-0.82215223 + d1 * 0.17087277)))
                                                                        )
                                                            )
                                                )
                                    )
                        )
            );
        return value >= 0.0 ? d2 : 2.0 - d2;
    }
}
