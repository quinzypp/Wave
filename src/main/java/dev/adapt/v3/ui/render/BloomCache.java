package dev.adapt.v3.ui.render;

import com.mojang.blaze3d.platform.NativeImage;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

public final class BloomCache {
    public static final Map<String, BloomCache> INSTANCES = new HashMap<>();
    public final Identifier textureId;
    public SmoothTexture texture;
    public NativeImage image;
    public int texWidth;
    public int texHeight;
    public long lastHash = Long.MIN_VALUE;
    public static final long SETTLE_DELAY_MS = 120L;
    public long lastChangeMs;
    public boolean coarsePass;
    public int downsampleScale = 1;
    public int contentWidth;
    public int contentHeight;

    public BloomCache(String text) {
        this.textureId = Identifier.fromNamespaceAndPath("adaptv3", "generated/bloom_" + text);
    }

    public static BloomCache get(String text) {
        return INSTANCES.computeIfAbsent(text, BloomCache::new);
    }

    public int getTexWidth() {
        return this.texWidth;
    }

    public int getTexHeight() {
        return this.texHeight;
    }

    public int getDownsampleScale() {
        return this.downsampleScale;
    }

    public int getContentWidth() {
        return this.contentWidth;
    }

    public int getContentHeight() {
        return this.contentHeight;
    }

    public Identifier bake(int[][] intArrays, int first, int second, float[][] floatArrays) {
        long i = 17L;

        for (int[] aint : intArrays) {
            for (int j : aint) {
                i = i * 31L + j;
            }
        }

        i = i * 31L + first;
        i = i * 31L + second;

        for (float[] afloat5 : floatArrays) {
            for (float f2 : afloat5) {
                i = i * 31L + Float.floatToIntBits(f2);
            }
        }

        long j2 = System.currentTimeMillis();
        boolean flag = i == this.lastHash && this.coarsePass && j2 - this.lastChangeMs > 120L;
        if (i == this.lastHash && this.texture != null && !flag) {
            return this.textureId;
        }

        boolean flag1 = i != this.lastHash && j2 - this.lastChangeMs < 120L;
        if (i != this.lastHash) {
            this.lastChangeMs = j2;
        }

        float f1 = Float.MAX_VALUE;

        for (float[] afloat : floatArrays) {
            f1 = Math.min(f1, afloat[0]);
        }

        int k2 = Math.max(1, Math.min(4, (int)(f1 / 2.5F)));
        int l2 = flag1 ? Math.min(8, k2 * 2) : k2;
        int i3 = (first + l2 - 1) / l2;
        int j3 = (second + l2 - 1) / l2;
        this.ensureCapacity(i3, j3);
        int k = intArrays.length;
        int l = floatArrays.length;
        float[][][] afloat1 = new float[l][k][i3];
        float[][][] afloat2 = new float[l][k][j3];

        for (int i1 = 0; i1 < l; i1++) {
            double d0 = floatArrays[i1][0] * Math.sqrt(2.0);
            float f = floatArrays[i1][1];
            int k1 = (int)Math.ceil(d0 * 4.0 + Math.abs(f)) + l2 + 2;
            float[] afloat3 = buildErfcLut(d0, -l2 * 0.5, k1);
            float[] afloat4 = buildErfcLut(d0, -l2 * 0.5 + f, k1);

            for (int l1 = 0; l1 < k; l1++) {
                int[] aint1 = intArrays[l1];

                for (int i2 = 0; i2 < i3; i2++) {
                    afloat1[i1][l1][i2] = 0.5F * (sampleErfcLut(afloat3, k1, aint1[0] - i2 * l2) - sampleErfcLut(afloat3, k1, aint1[1] - i2 * l2));
                }

                for (int i5 = 0; i5 < j3; i5++) {
                    afloat2[i1][l1][i5] = 0.5F * (sampleErfcLut(afloat4, k1, aint1[2] - i5 * l2) - sampleErfcLut(afloat4, k1, aint1[3] - i5 * l2));
                }
            }
        }

        int[][] aint2 = new int[l][k];
        int[] aint3 = new int[l];

        for (int j1 = 0; j1 < j3; j1++) {
            for (int k3 = 0; k3 < l; k3++) {
                aint3[k3] = 0;

                for (int i4 = 0; i4 < k; i4++) {
                    if (afloat2[k3][i4][j1] > 5.0E-4F) {
                        aint2[k3][aint3[k3]++] = i4;
                    }
                }
            }

            for (int l3 = 0; l3 < i3; l3++) {
                float f3 = 1.0F;

                for (int j4 = 0; j4 < l; j4++) {
                    float f4 = 0.0F;
                    int[] aint4 = aint2[j4];

                    for (int l4 = 0; l4 < aint3[j4]; l4++) {
                        int j5 = aint4[l4];
                        f4 += afloat2[j4][j5][j1] * afloat1[j4][j5][l3];
                    }

                    f3 *= 1.0F - Math.min(1.0F, f4) * floatArrays[j4][2];
                }

                int k4 = Math.max(0, Math.min(255, Math.round((1.0F - f3) * 255.0F)));
                this.image.setPixel(l3, j1, k4 << 24 | 16777215);
            }
        }

        if (i3 < this.texWidth) {
            this.image.fillRect(i3, 0, 1, Math.min(this.texHeight, j3 + 1), 16777215);
        }

        if (j3 < this.texHeight) {
            this.image.fillRect(0, j3, Math.min(this.texWidth, i3 + 1), 1, 16777215);
        }

        this.texture.upload();
        this.lastHash = i;
        this.coarsePass = flag1;
        this.downsampleScale = l2;
        this.contentWidth = i3;
        this.contentHeight = j3;
        return this.textureId;
    }

    public static float[] buildErfcLut(double value, double value2, int value3) {
        float[] afloat = new float[value3 * 2 + 1];

        for (int i = -value3; i <= value3; i++) {
            afloat[i + value3] = (float)TextureCache.erfc((i + value2) / value);
        }

        return afloat;
    }

    public static float sampleErfcLut(float[] floats, int first, int second) {
        if (second < -first) {
            return 2.0F;
        } else {
            return second > first ? 0.0F : floats[second + first];
        }
    }

    public void ensureCapacity(int first, int second) {
        boolean flag = this.texture != null && (long)this.texWidth * this.texHeight > 3L * (first + 64) * (second + 64);
        if (this.texture == null || first > this.texWidth || second > this.texHeight || flag) {
            if (flag) {
                Minecraft.getInstance().getTextureManager().release(this.textureId);
                this.texture = null;
                this.texWidth = 0;
                this.texHeight = 0;
            }

            int i = Math.max(first, this.texWidth) + 64;
            int j = Math.max(second, this.texHeight) + 64;
            this.image = new NativeImage(i, j, true);
            this.texture = new SmoothTexture(this.textureId.toString(), this.image);
            Minecraft.getInstance().getTextureManager().register(this.textureId, this.texture);
            this.texWidth = i;
            this.texHeight = j;
        }
    }
}
