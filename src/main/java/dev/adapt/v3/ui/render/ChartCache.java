package dev.adapt.v3.ui.render;

import com.mojang.blaze3d.platform.NativeImage;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

public final class ChartCache {
    public static final Map<String, ChartCache> INSTANCES = new HashMap<>();
    public final Identifier textureId;
    public SmoothTexture texture;
    public NativeImage image;
    public int texWidth;
    public int texHeight;
    public long lastHash = Long.MIN_VALUE;
    public ChartCache.Baked cachedResult;

    public ChartCache(String text) {
        this.textureId = Identifier.fromNamespaceAndPath("adaptv3", "generated/chart_" + text);
    }

    public static ChartCache get(String text) {
        return INSTANCES.computeIfAbsent(text, ChartCache::new);
    }

    public ChartCache.Baked bake(int value, int value2, float[] floats, float value3, int value4, int value5, int value6, float value7) {
        long i = 17L;
        i = i * 31L + value;
        i = i * 31L + value2;

        for (float f : floats) {
            i = i * 31L + Float.floatToIntBits(f);
        }

        i = i * 31L + Float.floatToIntBits(value3);
        i = i * 31L + value4;
        i = i * 31L + value5;
        i = i * 31L + value6;
        i = i * 31L + Float.floatToIntBits(value7);
        if (i == this.lastHash && this.cachedResult != null) {
            return this.cachedResult;
        }

        if (value > 1 && value2 > 1 && floats.length >= 2) {
            this.ensureCapacity(value, value2);
            this.image.fillRect(0, 0, this.texWidth, this.texHeight, 0);
            int i2 = floats.length;
            float f10 = value7 / 2.0F + 0.5F;
            float f11 = value2 - 0.5F;
            float f12 = f11 - f10;
            float[] afloat = new float[value];

            for (int j = 0; j < value; j++) {
                float f1 = (float)j / (value - 1) * (i2 - 1);
                int k = Math.min(i2 - 2, (int)f1);
                float f2 = catmullRom(floats, k, f1 - k);
                afloat[j] = f11 - Math.max(0.0F, Math.min(1.0F, f2 / value3)) * f12;
            }

            float f13 = value7 / 2.0F;

            for (int j2 = 0; j2 < value; j2++) {
                float f14 = j2 / (value * 0.18F);
                float f15 = f14 >= 1.0F ? 1.0F : f14 * f14 * (3.0F - 2.0F * f14);
                float f3 = (afloat[Math.min(value - 1, j2 + 1)] - afloat[Math.max(0, j2 - 1)]) / 2.0F;
                float f4 = (float)Math.sqrt(1.0F + f3 * f3);
                float f5 = afloat[j2];
                float f6 = f13 * f4 + 1.0F;
                int l = Math.max(0, (int)Math.floor(f5 - f6));
                int i1 = Math.min(value2 - 1, (int)Math.ceil(f5 + f6));

                for (int j1 = 0; j1 < value2; j1++) {
                    float f7 = j1 + 0.5F;
                    int k1 = 0;
                    float f8 = Math.max(0.0F, Math.min(1.0F, f7 + 0.5F - f5));
                    if (f8 > 0.0F) {
                        float f9 = (f7 - f5) / Math.max(1.0F, f11 - f5);
                        int l1 = lerpArgb(value5, value6, Math.max(0.0F, Math.min(1.0F, f9)));
                        k1 = scaleAlpha(l1, f8 * f15);
                    }

                    if (j1 >= l && j1 <= i1) {
                        float f16 = Math.abs(f7 - f5) / f4;
                        float f17 = Math.max(0.0F, Math.min(1.0F, f13 + 0.5F - f16));
                        if (f17 > 0.0F) {
                            k1 = alphaOver(scaleAlpha(value4, f17 * f15), k1);
                        }
                    }

                    if (k1 >>> 24 != 0) {
                        this.image.setPixel(j2, j1, k1);
                    }
                }
            }

            this.texture.upload();
            this.lastHash = i;
            this.cachedResult = new ChartCache.Baked(this.textureId, value, value2, this.texWidth, this.texHeight);
            return this.cachedResult;
        } else {
            return null;
        }
    }

    public static float catmullRom(float[] floats, int first, float second) {
        float f = floats[Math.max(0, first - 1)];
        float f1 = floats[first];
        float f2 = floats[first + 1];
        float f3 = floats[Math.min(floats.length - 1, first + 2)];
        float f4 = second * second;
        float f5 = f4 * second;
        return 0.5F * (2.0F * f1 + (-f + f2) * second + (2.0F * f - 5.0F * f1 + 4.0F * f2 - f3) * f4 + (-f + 3.0F * f1 - 3.0F * f2 + f3) * f5);
    }

    public static int scaleAlpha(int color, float alpha) {
        int i = Math.round((color >>> 24) * alpha);
        return i << 24 | color & 16777215;
    }

    public static int lerpArgb(int color, int color2, float value) {
        int i = color >>> 24;
        int j = color >> 16 & 0xFF;
        int k = color >> 8 & 0xFF;
        int l = color & 0xFF;
        int i1 = color2 >>> 24;
        int j1 = color2 >> 16 & 0xFF;
        int k1 = color2 >> 8 & 0xFF;
        int l1 = color2 & 0xFF;
        return Math.round(i + (i1 - i) * value) << 24
            | Math.round(j + (j1 - j) * value) << 16
            | Math.round(k + (k1 - k) * value) << 8
            | Math.round(l + (l1 - l) * value);
    }

    public static int alphaOver(int color, int color2) {
        int i = color >>> 24;
        int j = color2 >>> 24;
        if (i == 0) {
            return color2;
        } else if (j != 0 && i != 255) {
            float f = i / 255.0F;
            float f1 = j / 255.0F * (1.0F - f);
            float f2 = f + f1;
            int k = Math.round(((color >> 16 & 0xFF) * f + (color2 >> 16 & 0xFF) * f1) / f2);
            int l = Math.round(((color >> 8 & 0xFF) * f + (color2 >> 8 & 0xFF) * f1) / f2);
            int i1 = Math.round(((color & 0xFF) * f + (color2 & 0xFF) * f1) / f2);
            return Math.round(f2 * 255.0F) << 24 | k << 16 | l << 8 | i1;
        } else {
            return color;
        }
    }

    public void ensureCapacity(int first, int second) {
        if (this.texture == null || first > this.texWidth || second > this.texHeight) {
            int i = Math.max(first, this.texWidth) + 32;
            int j = Math.max(second, this.texHeight) + 16;
            if (this.texture != null) {
                Minecraft.getInstance().getTextureManager().release(this.textureId);
            }

            this.image = new NativeImage(i, j, true);
            this.image.fillRect(0, 0, i, j, 0);
            this.texture = new SmoothTexture(this.textureId.toString(), this.image, true);
            Minecraft.getInstance().getTextureManager().register(this.textureId, this.texture);
            this.texWidth = i;
            this.texHeight = j;
        }
    }

    record Baked(Identifier id, int w, int h, int texW, int texH) {
    }
}
