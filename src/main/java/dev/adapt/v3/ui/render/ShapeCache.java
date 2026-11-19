package dev.adapt.v3.ui.render;

import com.mojang.blaze3d.platform.NativeImage;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

public final class ShapeCache {
    public static final Map<String, ShapeCache> INSTANCES = new HashMap<>();
    public final Identifier textureId;
    public SmoothTexture texture;
    public NativeImage image;
    public int texWidth;
    public int texHeight;
    public long bakedHash = Long.MIN_VALUE;
    public long warmupHash = Long.MIN_VALUE;
    public ShapeCache.Baked cachedResult;
    public int[] pixelBuffer = new int[0];

    public ShapeCache(String text) {
        this.textureId = Identifier.fromNamespaceAndPath("adaptv3", "generated/panel_" + text);
    }

    public static ShapeCache get(String text) {
        return INSTANCES.computeIfAbsent(text, ShapeCache::new);
    }

    public ShapeCache.Baked bake(Render2D.Scanlines scanlines, int value, int value2, float value3, boolean flag2) {
        boolean flag = value3 > 0.0F && value2 >>> 24 != 0;
        long i = 17L;
        i = i * 31L + scanlines.startY;
        i = i * 31L + scanlines.height;

        for (int j = 0; j < scanlines.height; j++) {
            i = i * 31L + Float.floatToIntBits(scanlines.leftX[j]);
            i = i * 31L + Float.floatToIntBits(scanlines.rightX[j]);
        }

        i = i * 31L + value;
        i = i * 31L + (flag ? value2 : 0);
        i = i * 31L + Float.floatToIntBits(flag ? value3 : 0.0F);
        i = i * 31L + (flag && flag2 ? 1 : 0);
        if (i == this.bakedHash && this.cachedResult != null) {
            return this.cachedResult;
        }

        if (i != this.warmupHash) {
            this.warmupHash = i;
            return null;
        }

        int i2 = (flag && flag2 ? (int)Math.ceil(value3) : 0) + 2;
        float f = Float.MAX_VALUE;
        float f1 = -Float.MAX_VALUE;

        for (int k = 0; k < scanlines.height; k++) {
            f = Math.min(f, scanlines.leftX[k]);
            f1 = Math.max(f1, scanlines.rightX[k]);
        }

        int j2 = (int)Math.floor(f) - i2;
        int l = scanlines.startY - i2;
        int i1 = (int)Math.ceil(f1) + i2 - j2;
        int j1 = scanlines.height + i2 * 2;
        if (i1 > 0 && j1 > 0) {
            if (this.pixelBuffer.length < i1 * j1) {
                this.pixelBuffer = new int[i1 * j1];
            }

            Arrays.fill(this.pixelBuffer, 0, i1 * j1, 0);
            Render2D.FillSink render2d$fillsink = (value4, value22, value32, value42, value5) -> this.blendRect(
                value4 - j2, value22 - l, value32 - j2, value42 - l, i1, j1, value5
            );
            if (value >>> 24 != 0) {
                Render2D.rasterizeContourFill(scanlines, value, render2d$fillsink);
            }

            if (flag) {
                if (flag2) {
                    Render2D.rasterizeContourOuterGlow(scanlines, value3, value2, render2d$fillsink);
                } else {
                    Render2D.rasterizeContourInnerBorder(scanlines, value3, value2, render2d$fillsink);
                }
            }

            this.ensureCapacity(i1, j1);

            for (int k1 = 0; k1 < j1; k1++) {
                for (int l1 = 0; l1 < i1; l1++) {
                    this.image.setPixel(l1, k1, this.pixelBuffer[k1 * i1 + l1]);
                }
            }

            if (this.texWidth > i1) {
                this.image.fillRect(i1, 0, this.texWidth - i1, Math.min(this.texHeight, j1), 0);
            }

            if (this.texHeight > j1) {
                this.image.fillRect(0, j1, this.texWidth, this.texHeight - j1, 0);
            }

            this.texture.upload();
            this.bakedHash = i;
            this.cachedResult = new ShapeCache.Baked(this.textureId, j2, l, i1, j1, this.texWidth, this.texHeight);
            return this.cachedResult;
        } else {
            this.warmupHash = Long.MIN_VALUE;
            return null;
        }
    }

    public void blendRect(int value, int value2, int value3, int value4, int value5, int value6, int value7) {
        int i = value7 >>> 24;
        if (i != 0) {
            int j = value7 >> 16 & 0xFF;
            int k = value7 >> 8 & 0xFF;
            int l = value7 & 0xFF;
            int i1 = Math.max(0, value);
            int j1 = Math.min(value5, value3);
            int k1 = Math.max(0, value2);
            int l1 = Math.min(value6, value4);

            for (int i2 = k1; i2 < l1; i2++) {
                int j2 = i2 * value5;

                for (int k2 = i1; k2 < j1; k2++) {
                    int l2 = this.pixelBuffer[j2 + k2];
                    int i3 = l2 >>> 24;
                    if (i3 != 0 && i != 255) {
                        float f = i / 255.0F;
                        float f1 = i3 / 255.0F * (1.0F - f);
                        float f2 = f + f1;
                        int j3 = Math.round((j * f + (l2 >> 16 & 0xFF) * f1) / f2);
                        int k3 = Math.round((k * f + (l2 >> 8 & 0xFF) * f1) / f2);
                        int l3 = Math.round((l * f + (l2 & 0xFF) * f1) / f2);
                        this.pixelBuffer[j2 + k2] = Math.round(f2 * 255.0F) << 24 | j3 << 16 | k3 << 8 | l3;
                    } else {
                        this.pixelBuffer[j2 + k2] = value7;
                    }
                }
            }
        }
    }

    public void ensureCapacity(int first, int second) {
        if (this.texture == null || first > this.texWidth || second > this.texHeight) {
            int i = Math.max(first, this.texWidth) + 64;
            int j = Math.max(second, this.texHeight) + 64;
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

    record Baked(Identifier id, int x, int y, int w, int h, int texW, int texH) {
    }
}
