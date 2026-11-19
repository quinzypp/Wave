package dev.adapt.v3.ui.render;

import java.util.List;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.textures.FilterMode;
import com.mojang.renderpearl.api.textures.GpuSampler;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import dev.adapt.v3.mixin.GuiGraphicsInvoker;
import dev.adapt.v3.ui.theme.Theme;
import dev.adapt.v3.util.render.ColorUtility;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.function.IntUnaryOperator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2fStack;
import org.joml.Vector2f;

public final class Render2D {
    public static float refractCenterX;
    public static float refractCenterY;
    public static float refractZoom = 1.0F;
    public static final Map<Integer, float[][]> FILL_CORNER_COVERAGE = new HashMap<>();
    public static final Map<Integer, float[][]> RING_CORNER_COVERAGE = new HashMap<>();
    public static float backdropAlpha = 1.0F;
    public static final Vector2f TMP_MIN = new Vector2f();
    public static final Vector2f TMP_MAX = new Vector2f();
    public static StripBatch activeBatch;
    public static final int FLAT_LEFT = 1;
    public static final int FLAT_RIGHT = 2;
    public static final int FLAT_TOP = 4;
    public static final int FLAT_BOTTOM = 8;
    public static int contourFlags;

    public static int getGuiScale() {
        return Math.max(1, Minecraft.getInstance().getWindow().getGuiScale());
    }

    public static int toPx(float first, int second) {
        return Math.round(first * second);
    }

    public static void pushPixelScale(GuiGraphicsExtractor graphics, int value) {
        graphics.pose().pushMatrix();
        graphics.pose().scale(1.0F / value, 1.0F / value);
    }

    public static void popPose(GuiGraphicsExtractor graphics) {
        graphics.pose().popMatrix();
    }

    public static float getEffectiveScale(GuiGraphicsExtractor graphics) {
        Matrix3x2fStack matrix3x2fstack = graphics.pose();
        float f = (float)Math.hypot(matrix3x2fstack.m00(), matrix3x2fstack.m01());
        return getGuiScale() * (f > 0.001F ? f : 1.0F);
    }

    public static int toScaledPx(float scale, float scale2) {
        return Math.round(scale * scale2);
    }

    public static void pushEffectiveScale(GuiGraphicsExtractor graphics, float scale) {
        graphics.pose().pushMatrix();
        graphics.pose().scale(1.0F / scale, 1.0F / scale);
    }

    public static void blit(
        GuiGraphicsExtractor graphics,
        Identifier id,
        int value,
        int value2,
        int value3,
        int value4,
        float value5,
        float value6,
        int value7,
        int value8,
        int value9,
        int value10,
        int value11
    ) {
        if (value3 > 0 && value4 > 0) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, id, value, value2, value5, value6, value3, value4, value7, value8, value9, value10, value11);
        }
    }

    public static void fillRect(GuiGraphicsExtractor graphics, float x, float y, float width, float height, int color) {
        if (ColorUtility.getAlpha(color) != 0 && !(width <= 0.0F) && !(height <= 0.0F)) {
            int i = getGuiScale();
            pushPixelScale(graphics, i);
            int j = toPx(x, i);
            int k = toPx(y, i);
            graphics.fill(j, k, toPx(x + width, i), toPx(y + height, i), color);
            popPose(graphics);
        }
    }

    public static void vlineHairline(GuiGraphicsExtractor graphics, float value, float value2, float value3, int value4) {
        if (ColorUtility.getAlpha(value4) != 0 && !(value3 <= 0.0F)) {
            float f = getEffectiveScale(graphics);
            pushEffectiveScale(graphics, f);
            int i = toScaledPx(value, f);
            graphics.fill(i, toScaledPx(value2, f), i + 1, toScaledPx(value2 + value3, f), value4);
            popPose(graphics);
        }
    }

    public static void fillVerticalGradient(GuiGraphicsExtractor graphics, float x, float y, float width, float height, int startColor, int color) {
        int i = getGuiScale();
        pushPixelScale(graphics, i);
        graphics.fillGradient(toPx(x, i), toPx(y, i), toPx(x + width, i), toPx(y + height, i), startColor, color);
        popPose(graphics);
    }

    public static void fillHorizontalGradient(GuiGraphicsExtractor graphics, float x, float y, float width, float height, int startColor, int color) {
        int i = getGuiScale();
        int j = toPx(x, i);
        int k = toPx(x + width, i);
        int l = toPx(y, i);
        int i1 = toPx(y + height, i);
        if (k > j && i1 > l) {
            pushPixelScale(graphics, i);
            fillHorizontalGradientPx(graphics, j, l, k, i1, startColor, color);
            popPose(graphics);
        }
    }

    public static void fillHorizontalGradientPx(GuiGraphicsExtractor graphics, int value, int value2, int value3, int value4, int value5, int value6) {
        if (value3 > value && value4 > value2) {
            graphics.pose().pushMatrix();
            graphics.pose().translate(value, value4);
            graphics.pose().rotate((float) (-Math.PI / 2));
            graphics.fillGradient(0, 0, value4 - value2, value3 - value, value5, value6);
            graphics.pose().popMatrix();
        }
    }

    public static void fillHorizontalRoundedGradient(
        GuiGraphicsExtractor graphics, float x, float y, float width, float height, float radius, int first, int startColor, float second
    ) {
        if (!(second <= 0.01F) && !(width <= 0.0F) && !(height <= 0.0F)) {
            int i = getGuiScale();
            int j = toPx(x, i);
            int k = toPx(x + width, i) - j;
            float f = y * i;
            float f1 = (y + height) * i;
            float f2 = f1 - f;
            if (k > 0 && !(f2 <= 0.0F)) {
                float f3 = Math.min(radius * i, Math.min(k, f2) / 2.0F);
                int l = Math.min(k / 2, (int)Math.ceil(f3));
                pushPixelScale(graphics, i);

                for (int i1 = 0; i1 < k; i1++) {
                    if (i1 == l && k - l > l) {
                        i1 = k - l;
                    }

                    float f4 = Math.min(i1 + 0.5F, k - i1 - 0.5F);
                    float f5 = f4 < f3 ? f3 - (float)Math.sqrt(Math.max(0.0F, f3 * f3 - (f3 - f4) * (f3 - f4))) : 0.0F;
                    float f6 = f + f5;
                    float f7 = f1 - f5;
                    int j1 = (int)Math.ceil(f6);
                    int k1 = (int)Math.floor(f7);
                    int l1 = ColorUtility.scaleAlpha(ColorUtility.lerpColor(first, startColor, (i1 + 0.5F) / k), second);
                    if (k1 > j1) {
                        graphics.fill(j + i1, j1, j + i1 + 1, k1, l1);
                        if (j1 - f6 > 0.02F) {
                            graphics.fill(j + i1, j1 - 1, j + i1 + 1, j1, ColorUtility.scaleAlpha(l1, j1 - f6));
                        }

                        if (f7 - k1 > 0.02F) {
                            graphics.fill(j + i1, k1, j + i1 + 1, k1 + 1, ColorUtility.scaleAlpha(l1, f7 - k1));
                        }
                    } else if (f7 > f6) {
                        graphics.fill(j + i1, (int)f6, j + i1 + 1, (int)f6 + 1, ColorUtility.scaleAlpha(l1, f7 - f6));
                    }
                }

                if (k - l > l) {
                    int i2 = j + l;
                    int j2 = j + k - l;
                    int k2 = ColorUtility.scaleAlpha(ColorUtility.lerpColor(first, startColor, (float)l / k), second);
                    int l2 = ColorUtility.scaleAlpha(ColorUtility.lerpColor(first, startColor, (float)(k - l) / k), second);
                    int i3 = (int)Math.ceil(f);
                    int j3 = (int)Math.floor(f1);
                    if (j3 > i3) {
                        fillHorizontalGradientPx(graphics, i2, i3, j2, j3, k2, l2);
                    }

                    float f8 = i3 - f;
                    float f9 = f1 - j3;
                    if (f8 > 0.02F) {
                        fillHorizontalGradientPx(graphics, i2, i3 - 1, j2, i3, ColorUtility.scaleAlpha(k2, f8), ColorUtility.scaleAlpha(l2, f8));
                    }

                    if (f9 > 0.02F) {
                        fillHorizontalGradientPx(graphics, i2, j3, j2, j3 + 1, ColorUtility.scaleAlpha(k2, f9), ColorUtility.scaleAlpha(l2, f9));
                    }
                }

                popPose(graphics);
            }
        }
    }

    public static void fillRoundedRect(GuiGraphicsExtractor graphics, float x, float y, float width, float height, float radius, int color) {
        if (ColorUtility.getAlpha(color) != 0 && !(width <= 0.0F) && !(height <= 0.0F)) {
            int i = getGuiScale();
            int j = toPx(x, i);
            int k = toPx(y, i);
            int l = toPx(x + width, i);
            int i1 = toPx(y + height, i);
            int j1 = l - j;
            int k1 = i1 - k;
            int l1 = Math.min(toPx(radius, i), Math.min(j1, k1) / 2);
            pushPixelScale(graphics, i);
            if (l1 <= 0) {
                graphics.fill(j, k, l, i1, color);
                popPose(graphics);
            } else {
                graphics.fill(j + l1, k, l - l1, i1, color);
                graphics.fill(j, k + l1, j + l1, i1 - l1, color);
                graphics.fill(l - l1, k + l1, l, i1 - l1, color);
                Identifier identifier = TextureCache.getCorner(l1);
                blit(graphics, identifier, j, k, l1, l1, 0.0F, 0.0F, l1, l1, l1, l1, color);
                blit(graphics, identifier, l - l1, k, l1, l1, l1, 0.0F, -l1, l1, l1, l1, color);
                blit(graphics, identifier, j, i1 - l1, l1, l1, 0.0F, l1, l1, -l1, l1, l1, color);
                blit(graphics, identifier, l - l1, i1 - l1, l1, l1, l1, l1, -l1, -l1, l1, l1, color);
                popPose(graphics);
            }
        }
    }

    public static void fillSubpixelRoundedRect(GuiGraphicsExtractor graphics, float x, float y, float width, float height, float radius, int color) {
        if (ColorUtility.getAlpha(color) != 0 && !(width <= 0.0F) && !(height <= 0.0F)) {
            int i = getGuiScale();
            float f = x * i;
            float f1 = (x + width) * i;
            float f2 = y * i;
            float f3 = (y + height) * i;
            int j = (int)Math.ceil(f);
            int k = (int)Math.floor(f1);
            int l = (int)Math.ceil(f2);
            int i1 = (int)Math.floor(f3);
            if (i1 - l >= 2 && k - j >= 2) {
                fillRoundedRect(graphics, (float)j / i, (float)l / i, (float)(k - j) / i, (float)(i1 - l) / i, radius, color);
                float f4 = l - f2;
                float f5 = f3 - i1;
                float f6 = j - f;
                float f7 = f1 - k;
                int j1 = Math.max(0, Math.min(toPx(radius, i), Math.min(k - j, i1 - l) / 2));
                pushPixelScale(graphics, i);
                if (f4 > 0.02F) {
                    graphics.fill(j + j1, l - 1, k - j1, l, ColorUtility.scaleAlpha(color, f4));
                }

                if (f5 > 0.02F) {
                    graphics.fill(j + j1, i1, k - j1, i1 + 1, ColorUtility.scaleAlpha(color, f5));
                }

                if (f6 > 0.02F) {
                    graphics.fill(j - 1, l + j1, j, i1 - j1, ColorUtility.scaleAlpha(color, f6));
                }

                if (f7 > 0.02F) {
                    graphics.fill(k, l + j1, k + 1, i1 - j1, ColorUtility.scaleAlpha(color, f7));
                }

                popPose(graphics);
            } else {
                fillRoundedRect(graphics, x, y, width, height, radius, color);
            }
        }
    }

    public static void fillCircle(GuiGraphicsExtractor graphics, float value, float value2, float value3, int value4) {
        if (ColorUtility.getAlpha(value4) != 0 && !(value3 <= 0.0F)) {
            withSubpixelCircleTransform(graphics, value, value2, value3, (value5, value22, value32) -> fillCircleAligned(graphics, value5, value22, value32, value4));
        }
    }

    public static void drawCircleShadow(GuiGraphicsExtractor graphics, float x, float y, float width, float height, int color) {
        if (ColorUtility.getAlpha(color) != 0 && !(width <= 0.0F)) {
            withSubpixelCircleTransform(
                graphics,
                x,
                y,
                width,
                (value, value2, value3) -> drawDropShadow(graphics, value - value3, value2 - value3, value3 * 2.0F, value3 * 2.0F, value3, height, color)
            );
        }
    }

    public static void withSubpixelCircleTransform(GuiGraphicsExtractor graphics, float value, float value2, float value3, Render2D.CircleDrawer circleDrawer) {
        int i = getGuiScale();
        float f = Math.max(1.0F, (float)Math.ceil(value3 * i));
        float f1 = f / i;
        float f2 = snapToPixel(value);
        float f3 = snapToPixel(value2);
        float f4 = value3 / f1;
        graphics.pose().pushMatrix();
        graphics.pose().translate(value, value2);
        graphics.pose().scale(f4, f4);
        graphics.pose().translate(-f2, -f3);
        circleDrawer.draw(f2, f3, f1);
        graphics.pose().popMatrix();
    }

    public static void drawLine(GuiGraphicsExtractor graphics, float x, float y, float width, float height, float value, int color) {
        drawLine(graphics, x, y, width, height, value, color, true);
    }

    public static void drawLine(GuiGraphicsExtractor graphics, float x, float y, float width, float height, float value, int startColor, boolean flag) {
        if (ColorUtility.getAlpha(startColor) != 0 && !(value <= 0.0F)) {
            drawTexturedSegment(graphics, TextureCache.getLine(value), TextureCache.getLineTexHeight(value), flag ? value / 2.0F : 0.0F, x, y, width, height, startColor);
        }
    }

    public static void drawGlowLine(GuiGraphicsExtractor graphics, float x, float y, float width, float height, int startColor, int color) {
        if (ColorUtility.getAlpha(color) != 0 && startColor > 0) {
            drawTexturedSegment(graphics, TextureCache.getGlowLine(startColor), Math.max(3, startColor), 0.0F, x, y, width, height, color);
        }
    }

    public static void drawTexturedSegment(
        GuiGraphicsExtractor graphics, Identifier id, int value, float value2, float value3, float value4, float value5, float value6, int value7
    ) {
        int i = getGuiScale();
        float f = value3 * i;
        float f1 = value4 * i;
        float f2 = (value5 - value3) * i;
        float f3 = (value6 - value4) * i;
        float f4 = (float)Math.hypot(f2, f3);
        if (!(f4 < 0.01F)) {
            pushPixelScale(graphics, i);
            graphics.pose().pushMatrix();
            graphics.pose().translate(f, f1);
            graphics.pose().rotate((float)Math.atan2(f3, f2));
            graphics.pose().translate(-value2, -value / 2.0F);
            blit(graphics, id, 0, 0, Math.max(1, Math.round(f4 + value2 * 2.0F)), value, 0.0F, 0.0F, 4, value, 4, value, value7);
            graphics.pose().popMatrix();
            popPose(graphics);
        }
    }

    public static void fillCircleAligned(GuiGraphicsExtractor graphics, float value, float value2, float value3, int value4) {
        fillRoundedRect(graphics, value - value3, value2 - value3, value3 * 2.0F, value3 * 2.0F, value3, value4);
    }

    public static void drawRing(GuiGraphicsExtractor graphics, float x, float y, float width, float height, int color) {
        if (ColorUtility.getAlpha(color) != 0 && !(width <= 0.0F)) {
            int i = getGuiScale();
            int j = Math.max(1, toPx(width, i));
            int k = Math.max(1, toPx(height, i));
            pushPixelScale(graphics, i);
            Identifier identifier = TextureCache.getRing(j, k);
            blit(graphics, identifier, toPx(x, i) - j, toPx(y, i) - j, j * 2, j * 2, 0.0F, 0.0F, j * 2, j * 2, j * 2, j * 2, color);
            popPose(graphics);
        }
    }

    public static void drawRoundedBorderTextured(GuiGraphicsExtractor graphics, float x, float y, float width, float height, float radius, float value, int color) {
        if (ColorUtility.getAlpha(color) != 0) {
            int i = getGuiScale();
            int j = toPx(x, i);
            int k = toPx(y, i);
            int l = toPx(x + width, i);
            int i1 = toPx(y + height, i);
            int j1 = Math.max(1, Math.min(toPx(radius, i), Math.min(l - j, i1 - k) / 2));
            float f = Math.max(1.0F, Math.min(value, j1));
            int k1 = (int)f;
            int l1 = ColorUtility.scaleAlpha(color, f - k1);
            pushPixelScale(graphics, i);
            graphics.fill(j + j1, k, l - j1, k + k1, color);
            graphics.fill(j + j1, k + k1, l - j1, k + k1 + 1, l1);
            graphics.fill(j + j1, i1 - k1, l - j1, i1, color);
            graphics.fill(j + j1, i1 - k1 - 1, l - j1, i1 - k1, l1);
            graphics.fill(j, k + j1, j + k1, i1 - j1, color);
            graphics.fill(j + k1, k + j1, j + k1 + 1, i1 - j1, l1);
            graphics.fill(l - k1, k + j1, l, i1 - j1, color);
            graphics.fill(l - k1 - 1, k + j1, l - k1, i1 - j1, l1);
            Identifier identifier = TextureCache.getRing(j1, f);
            int i2 = j1 * 2;
            blit(graphics, identifier, j, k, j1, j1, 0.0F, 0.0F, j1, j1, i2, i2, color);
            blit(graphics, identifier, l - j1, k, j1, j1, j1, 0.0F, j1, j1, i2, i2, color);
            blit(graphics, identifier, j, i1 - j1, j1, j1, 0.0F, j1, j1, j1, i2, i2, color);
            blit(graphics, identifier, l - j1, i1 - j1, j1, j1, j1, j1, j1, j1, i2, i2, color);
            popPose(graphics);
        }
    }

    public static void drawDropShadow(GuiGraphicsExtractor graphics, float x, float y, float width, float height, float radius, float value, int color) {
        if (ColorUtility.getAlpha(color) != 0 && !(width <= 0.0F) && !(height <= 0.0F)) {
            int i = getGuiScale();
            int j = toPx(x, i);
            int k = toPx(y, i);
            int l = toPx(x + width, i);
            int i1 = toPx(y + height, i);
            int j1 = Math.max(2, Math.min(l - j, i1 - k));
            int k1 = Math.max(1, Math.min(toPx(value, i), j1 / 2));
            int l1 = Math.max(0, Math.min(toPx(radius, i), j1 / 2));
            blitNineSliceShadow(graphics, i, j - k1, k - k1, l + k1, i1 + k1, l1, k1, color);
        }
    }

    public static void drawSteppedBloomSingle(GuiGraphicsExtractor graphics, float[][] floatArrays, float value, float value2, float value3, int value4) {
        drawMultiRectBloom(graphics, "stepped", floatArrays, new float[][]{{value2, value3, 1.0F}}, value4);
    }

    public static void drawSteppedBloom(GuiGraphicsExtractor graphics, float[][] floatArrays, float[][] floatArrays2, int value) {
        drawMultiRectBloom(graphics, "stepped", floatArrays, floatArrays2, value);
    }

    public static void drawPanelElevationShadow(GuiGraphicsExtractor graphics, String text, float value, float value2, float value3, float value4, float value5, float value6) {
        drawMultiRectBloom(
            graphics,
            text,
            new float[][]{{value, value + value3, value2, value2 + value4}},
            new float[][]{{22.0F * value5, 4.0F * value5, 0.4392157F}, {16.0F * value5, 0.0F, 0.21960784F}},
            ColorUtility.scaleAlpha(-16777216, value6)
        );
    }

    public static void drawMultiRectBloom(GuiGraphicsExtractor graphics, String text, float[][] floatArrays, float[][] floatArrays2, int value) {
        if (ColorUtility.getAlpha(value) != 0 && floatArrays.length != 0 && floatArrays2.length != 0) {
            int i = getGuiScale();
            float[][] afloat = new float[floatArrays2.length][];
            int j = 1;

            for (int k = 0; k < floatArrays2.length; k++) {
                float f = Math.max(0.5F, toPx(floatArrays2[k][0], i) / 2.6F);
                float f1 = floatArrays2[k][1] * i;
                afloat[k] = new float[]{f, f1, floatArrays2[k][2]};
                j = Math.max(j, (int)Math.ceil(f * 3.2F + Math.abs(f1)) + 1);
            }

            int i2 = Integer.MAX_VALUE;
            int j2 = Integer.MAX_VALUE;
            int k2 = Integer.MIN_VALUE;
            int l = Integer.MIN_VALUE;
            List<int[]> arraylist = new ArrayList<>();

            for (float[] afloat1 : floatArrays) {
                int i1 = toPx(afloat1[0], i);
                int j1 = toPx(afloat1[1], i);
                int k1 = toPx(afloat1[2], i);
                int l1 = toPx(afloat1[3], i);
                if (j1 > i1 && l1 > k1) {
                    arraylist.add(new int[]{i1, j1, k1, l1});
                    i2 = Math.min(i2, i1);
                    k2 = Math.max(k2, j1);
                    j2 = Math.min(j2, k1);
                    l = Math.max(l, l1);
                }
            }

            if (!arraylist.isEmpty()) {
                int l2 = i2 - j;
                int i3 = j2 - j;
                int j3 = k2 - i2 + 2 * j;
                int k3 = l - j2 + 2 * j;
                int[][] aint = new int[arraylist.size()][];

                for (int l3 = 0; l3 < aint.length; l3++) {
                    int[] aint1 = (int[])arraylist.get(l3);
                    aint[l3] = new int[]{aint1[0] - l2, aint1[1] - l2, aint1[2] - i3, aint1[3] - i3};
                }

                BloomCache bloomcache = BloomCache.get(text);
                Identifier identifier = bloomcache.bake(aint, j3, k3, afloat);
                int i4 = bloomcache.getDownsampleScale();
                pushPixelScale(graphics, i);
                blit(
                    graphics,
                    identifier,
                    l2,
                    i3,
                    bloomcache.getContentWidth() * i4,
                    bloomcache.getContentHeight() * i4,
                    0.0F,
                    0.0F,
                    bloomcache.getContentWidth(),
                    bloomcache.getContentHeight(),
                    bloomcache.getTexWidth(),
                    bloomcache.getTexHeight(),
                    value
                );
                popPose(graphics);
            }
        }
    }

    public static void blitNineSliceShadow(GuiGraphicsExtractor graphics, int value, int value2, int value3, int value4, int value5, int value6, int value7, int value8) {
        int i = TextureCache.getShadowPad(value6, value7);
        int j = i * 2 + 1;
        Identifier identifier = TextureCache.getShadowNineSlice(value6, value7);
        pushPixelScale(graphics, value);
        blit(graphics, identifier, value2, value3, i, i, 0.0F, 0.0F, i, i, j, j, value8);
        blit(graphics, identifier, value4 - i, value3, i, i, i + 1, 0.0F, i, i, j, j, value8);
        blit(graphics, identifier, value2, value5 - i, i, i, 0.0F, i + 1, i, i, j, j, value8);
        blit(graphics, identifier, value4 - i, value5 - i, i, i, i + 1, i + 1, i, i, j, j, value8);
        blit(graphics, identifier, value2 + i, value3, value4 - value2 - 2 * i, i, i, 0.0F, 1, i, j, j, value8);
        blit(graphics, identifier, value2 + i, value5 - i, value4 - value2 - 2 * i, i, i, i + 1, 1, i, j, j, value8);
        blit(graphics, identifier, value2, value3 + i, i, value5 - value3 - 2 * i, 0.0F, i, i, 1, j, j, value8);
        blit(graphics, identifier, value4 - i, value3 + i, i, value5 - value3 - 2 * i, i + 1, i, i, 1, j, j, value8);
        graphics.fill(value2 + i, value3 + i, value4 - i, value5 - i, value8);
        popPose(graphics);
    }

    public static void drawCenteredBlob(GuiGraphicsExtractor graphics, float value, float value2, float value3, int value4) {
        drawBlob(graphics, value - value3, value2 - value3, value3 * 2.0F, value3 * 2.0F, value4);
    }

    public static void drawBlob(GuiGraphicsExtractor graphics, float x, float y, float width, float height, int color) {
        if (ColorUtility.getAlpha(color) != 0 && !(width <= 0.0F) && !(height <= 0.0F)) {
            int i = getGuiScale();
            pushPixelScale(graphics, i);
            int j = toPx(x, i);
            int k = toPx(y, i);
            blit(
                graphics,
                TextureCache.getBlob(),
                j,
                k,
                Math.max(1, toPx(x + width, i) - j),
                Math.max(1, toPx(y + height, i) - k),
                0.0F,
                0.0F,
                128,
                128,
                128,
                128,
                color
            );
            popPose(graphics);
        }
    }

    public static int circleInsetPx(int first, int second) {
        double d0 = first - second - 0.5;
        return (int)Math.round(first - Math.sqrt(Math.max(0.0, (double)first * first - d0 * d0)));
    }

    public static float[][] getFilledCornerMask(int value) {
        return FILL_CORNER_COVERAGE.computeIfAbsent(value, value2 -> {
            float[][] afloat = new float[value][value];
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

                    afloat[i][j] = k / 16.0F;
                }
            }

            return afloat;
        });
    }

    public static void fillBatchedRoundedRect(GuiGraphicsExtractor graphics, float x, float y, float width, float height, float radius, int color) {
        if (ColorUtility.getAlpha(color) != 0 && !(width <= 0.0F) && !(height <= 0.0F)) {
            float f = getEffectiveScale(graphics);
            int i = toScaledPx(x, f);
            int j = toScaledPx(y, f);
            int k = toScaledPx(x + width, f);
            int l = toScaledPx(y + height, f);
            if (k > i && l > j) {
                int i1 = Math.max(0, Math.min(toScaledPx(radius, f), Math.min(k - i, l - j) / 2));
                pushEffectiveScale(graphics, f);
                StripBatch stripbatch = StripBatch.beginFill(graphics);
                Render2D.FillSink render2d$fillsink = stripbatch != null ? stripbatch::addFill : graphics::fill;
                if (i1 > 0) {
                    float[][] afloat = getFilledCornerMask(i1);

                    for (int j1 = 0; j1 < i1; j1++) {
                        int k1 = firstOpaqueIndex(afloat[j1]);
                        int l1 = j + j1;
                        int i2 = l - j1 - 1;
                        render2d$fillsink.addFill(i + k1, l1, k - k1, l1 + 1, color);
                        render2d$fillsink.addFill(i + k1, i2, k - k1, i2 + 1, color);

                        for (int j2 = 0; j2 < k1; j2++) {
                            float f1 = afloat[j1][j2];
                            if (!(f1 <= 0.001F)) {
                                int k2 = ColorUtility.scaleAlpha(color, f1);
                                render2d$fillsink.addFill(i + j2, l1, i + j2 + 1, l1 + 1, k2);
                                render2d$fillsink.addFill(k - j2 - 1, l1, k - j2, l1 + 1, k2);
                                render2d$fillsink.addFill(i + j2, i2, i + j2 + 1, i2 + 1, k2);
                                render2d$fillsink.addFill(k - j2 - 1, i2, k - j2, i2 + 1, k2);
                            }
                        }
                    }
                }

                render2d$fillsink.addFill(i, j + i1, k, l - i1, color);
                if (stripbatch != null) {
                    stripbatch.submit(graphics);
                }

                popPose(graphics);
            }
        }
    }

    public static int firstOpaqueIndex(float[] floats) {
        for (int i = 0; i < floats.length; i++) {
            if (floats[i] >= 0.999F) {
                return i;
            }
        }

        return floats.length;
    }

    public static float[][] getBorderCornerMask(int value) {
        return RING_CORNER_COVERAGE.computeIfAbsent(value, value2 -> {
            float[][] afloat = new float[value][value];
            byte b0 = 4;
            double d0 = value;
            double d1 = Math.max(0, value - 1);

            for (int i = 0; i < value; i++) {
                for (int j = 0; j < value; j++) {
                    int k = 0;

                    for (int l = 0; l < 4; l++) {
                        for (int i1 = 0; i1 < 4; i1++) {
                            double d2 = j + (i1 + 0.5) / 4.0 - value;
                            double d3 = i + (l + 0.5) / 4.0 - value;
                            double d4 = d2 * d2 + d3 * d3;
                            if (d4 <= d0 * d0 && d4 >= d1 * d1) {
                                k++;
                            }
                        }
                    }

                    afloat[i][j] = k / 16.0F;
                }
            }

            return afloat;
        });
    }

    public static void drawBatchedHairlineBorder(GuiGraphicsExtractor graphics, float x, float y, float width, float height, float value, int color) {
        if (ColorUtility.getAlpha(color) != 0) {
            float f = getEffectiveScale(graphics);
            int i = toScaledPx(x, f);
            int j = toScaledPx(y, f);
            int k = toScaledPx(x + width, f);
            int l = toScaledPx(y + height, f);
            if (k - i >= 2 && l - j >= 2) {
                int i1 = Math.max(1, Math.min(toScaledPx(value, f), Math.min(k - i, l - j) / 2));
                pushEffectiveScale(graphics, f);
                StripBatch stripbatch = StripBatch.beginFill(graphics);
                Render2D.FillSink render2d$fillsink = stripbatch != null ? stripbatch::addFill : graphics::fill;
                render2d$fillsink.addFill(i + i1, j, k - i1, j + 1, color);
                render2d$fillsink.addFill(i + i1, l - 1, k - i1, l, color);
                render2d$fillsink.addFill(i, j + i1, i + 1, l - i1, color);
                render2d$fillsink.addFill(k - 1, j + i1, k, l - i1, color);
                float[][] afloat = getBorderCornerMask(i1);

                for (int j1 = 0; j1 < i1; j1++) {
                    int k1 = j + j1;
                    int l1 = l - j1 - 1;

                    for (int i2 = 0; i2 < i1; i2++) {
                        float f1 = afloat[j1][i2];
                        if (!(f1 <= 0.001F)) {
                            int j2 = f1 >= 0.999F ? color : ColorUtility.scaleAlpha(color, f1);
                            render2d$fillsink.addFill(i + i2, k1, i + i2 + 1, k1 + 1, j2);
                            render2d$fillsink.addFill(k - i2 - 1, k1, k - i2, k1 + 1, j2);
                            render2d$fillsink.addFill(i + i2, l1, i + i2 + 1, l1 + 1, j2);
                            render2d$fillsink.addFill(k - i2 - 1, l1, k - i2, l1 + 1, j2);
                        }
                    }
                }

                if (stripbatch != null) {
                    stripbatch.submit(graphics);
                }

                popPose(graphics);
            }
        }
    }

    public static void drawBackdropBlur(GuiGraphicsExtractor graphics, float x, float y, float width, float height, float value) {
        drawBackdropBlurInset(graphics, x, y, width, height, value, 0);
    }

    public static void drawBackdropBlurInset(GuiGraphicsExtractor graphics, float x, float y, float width, float height, float value, int color) {
        drawBackdropBlur(graphics, x, y, width, height, value, color, 1.0F);
    }

    public static void drawBackdropBlur(GuiGraphicsExtractor graphics, float x, float y, float width, float height, float first, int startColor, float second) {
        backdropAlpha = Math.max(0.0F, Math.min(1.0F, second));

        try {
            drawBackdropBlurInternal(graphics, x, y, width, height, first, startColor);
        } finally {
            backdropAlpha = 1.0F;
        }
    }

    public static void drawBackdropBlurInternal(GuiGraphicsExtractor graphics, float x, float y, float width, float height, float value, int color) {
        GpuTextureView gputextureview = BackdropBlur.getTextureView();
        if (gputextureview != null && !(width <= 0.0F) && !(height <= 0.0F)) {
            GpuSampler gpusampler = BackdropBlur.getSampler();
            float f = getEffectiveScale(graphics);
            int i = toScaledPx(x, f) + color;
            int j = toScaledPx(y, f) + color;
            int k = toScaledPx(x + width, f) - color;
            int l = toScaledPx(y + height, f) - color;
            if (k > i && l > j) {
                int i1 = Math.max(0, Math.min(toScaledPx(value, f) - color, Math.min(k - i, l - j) / 2));
                float f1 = BackdropBlur.getWidth();
                float f2 = BackdropBlur.getHeight();
                pushEffectiveScale(graphics, f);
                Vector2f vector2f = graphics.pose().transformPosition((i + k) / 2.0F, (j + l) / 2.0F, new Vector2f());
                refractCenterX = vector2f.x * getGuiScale();
                refractCenterY = vector2f.y * getGuiScale();
                refractZoom = Theme.getLensZoom();
                beginBackdropBatch(graphics, gputextureview, gpusampler);

                try {
                    emitRoundedBackdropStrips(graphics, gputextureview, gpusampler, i, j, k, l, i1, f1, f2);
                } finally {
                    endBackdropBatch(graphics);
                }

                popPose(graphics);
            }
        }
    }

    public static void emitRoundedBackdropStrips(
        GuiGraphicsExtractor graphics, GpuTextureView gpuTextureView, GpuSampler gpuSampler, int value, int value2, int value3, int value4, int value5, float value6, float value7
    ) {
        if (value5 > 0) {
            float[][] afloat = getFilledCornerMask(value5);

            for (int i = 0; i < value5; i++) {
                int j = value5;

                for (int k = 0; k < value5; k++) {
                    if (afloat[i][k] >= 0.999F) {
                        j = k;
                        break;
                    }
                }

                int k1 = value2 + i;
                int l = value4 - i - 1;
                emitBackdropQuad(graphics, gpuTextureView, gpuSampler, value + j, k1, value3 - j, k1 + 1, value6, value7, -1);
                emitBackdropQuad(graphics, gpuTextureView, gpuSampler, value + j, l, value3 - j, l + 1, value6, value7, -1);

                for (int i1 = 0; i1 < j; i1++) {
                    float f = square(afloat[i][i1]);
                    if (!(f <= 0.001F)) {
                        int j1 = ColorUtility.withAlphaFloat(16777215, f);
                        emitBackdropQuad(graphics, gpuTextureView, gpuSampler, value + i1, k1, value + i1 + 1, k1 + 1, value6, value7, j1);
                        emitBackdropQuad(graphics, gpuTextureView, gpuSampler, value3 - i1 - 1, k1, value3 - i1, k1 + 1, value6, value7, j1);
                        emitBackdropQuad(graphics, gpuTextureView, gpuSampler, value + i1, l, value + i1 + 1, l + 1, value6, value7, j1);
                        emitBackdropQuad(graphics, gpuTextureView, gpuSampler, value3 - i1 - 1, l, value3 - i1, l + 1, value6, value7, j1);
                    }
                }
            }
        }

        emitBackdropQuad(graphics, gpuTextureView, gpuSampler, value, value2 + value5, value3, value4 - value5, value6, value7, -1);
    }

    public static float square(float value) {
        return value * value;
    }

    public static void emitBackdropQuad(
        GuiGraphicsExtractor graphics, GpuTextureView gpuTextureView, GpuSampler gpuSampler, int value, int value2, int value3, int value4, float value5, float value6, int value7
    ) {
        if (value3 > value && value4 > value2) {
            int i = getGuiScale();
            Vector2f vector2f = graphics.pose().transformPosition(value, value2, TMP_MIN);
            Vector2f vector2f1 = graphics.pose().transformPosition(value3, value4, TMP_MAX);
            float f = refractCenterX + (vector2f.x * i - refractCenterX) / refractZoom;
            float f1 = refractCenterX + (vector2f1.x * i - refractCenterX) / refractZoom;
            float f2 = refractCenterY + (vector2f.y * i - refractCenterY) / refractZoom;
            float f3 = refractCenterY + (vector2f1.y * i - refractCenterY) / refractZoom;
            float f4 = f / value5;
            float f5 = f1 / value5;
            float f6 = 1.0F - f2 / value6;
            float f7 = 1.0F - f3 / value6;
            int j = backdropAlpha >= 0.999F ? value7 : ColorUtility.scaleAlpha(value7, backdropAlpha);
            if (activeBatch != null) {
                activeBatch.add(value, value2, value3, value4, f4, f5, f6, f7, j);
            } else {
                ((GuiGraphicsInvoker)graphics).adapt$innerBlit(RenderPipelines.GUI_TEXTURED, gpuTextureView, gpuSampler, value, value2, value3, value4, f4, f5, f6, f7, j);
            }
        }
    }

    public static void beginBackdropBatch(GuiGraphicsExtractor graphics, GpuTextureView gpuTextureView, GpuSampler gpuSampler) {
        activeBatch = StripBatch.begin(graphics, RenderPipelines.GUI_TEXTURED, gpuTextureView, gpuSampler);
    }

    public static void endBackdropBatch(GuiGraphicsExtractor graphics) {
        StripBatch stripbatch = activeBatch;
        activeBatch = null;
        if (stripbatch != null) {
            stripbatch.submit(graphics);
        }
    }

    public static void blitRoundedTexture(
        GuiGraphicsExtractor graphics,
        Identifier id,
        float value,
        float value2,
        float value3,
        float value4,
        float value5,
        float value6,
        float value7,
        float value8,
        float value9,
        int value10
    ) {
        if (ColorUtility.getAlpha(value10) != 0 && !(value3 <= 0.0F) && !(value4 <= 0.0F)) {
            AbstractTexture abstracttexture = Minecraft.getInstance().getTextureManager().getTexture(id);
            GpuTextureView gputextureview = abstracttexture.getTextureView();
            if (gputextureview != null) {
                GpuSampler gpusampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST);
                int i = getGuiScale();
                int j = toPx(value, i);
                int k = toPx(value2, i);
                int l = toPx(value + value3, i);
                int i1 = toPx(value2 + value4, i);
                int j1 = l - j;
                int k1 = i1 - k;
                if (j1 > 0 && k1 > 0) {
                    int l1 = Math.max(0, Math.min(toPx(value5, i), Math.min(j1, k1) / 2));
                    pushPixelScale(graphics, i);
                    float[][] afloat = l1 > 0 ? getFilledCornerMask(l1) : null;

                    for (int i2 = 0; i2 < k1; i2++) {
                        int j2 = Math.min(i2, k1 - 1 - i2);
                        int k2 = k + i2;
                        if (afloat != null && j2 < l1) {
                            float[] afloat1 = afloat[j2];
                            int l2 = l1;

                            for (int i3 = 0; i3 < l1; i3++) {
                                if (afloat1[i3] >= 0.999F) {
                                    l2 = i3;
                                    break;
                                }
                            }

                            blitSubRectUv(graphics, gputextureview, gpusampler, j + l2, k2, l - l2, k2 + 1, j, k, j1, k1, value6, value7, value8, value9, value10);

                            for (int k3 = 0; k3 < l2; k3++) {
                                if (!(afloat1[k3] <= 0.001F)) {
                                    int j3 = ColorUtility.scaleAlpha(value10, afloat1[k3]);
                                    blitSubRectUv(graphics, gputextureview, gpusampler, j + k3, k2, j + k3 + 1, k2 + 1, j, k, j1, k1, value6, value7, value8, value9, j3);
                                    blitSubRectUv(graphics, gputextureview, gpusampler, l - k3 - 1, k2, l - k3, k2 + 1, j, k, j1, k1, value6, value7, value8, value9, j3);
                                }
                            }
                        } else {
                            blitSubRectUv(graphics, gputextureview, gpusampler, j, k2, l, k2 + 1, j, k, j1, k1, value6, value7, value8, value9, value10);
                        }
                    }

                    popPose(graphics);
                }
            }
        }
    }

    public static void drawPlayerHead(GuiGraphicsExtractor graphics, Identifier id, float value, float value2, float value3, int value4) {
        if (ColorUtility.getAlpha(value4) >= 4) {
            float f = getHeadCornerRadius(value3);
            blitRoundedTexture(graphics, id, value, value2, value3, value3, f, 0.125F, 0.125F, 0.25F, 0.25F, value4);
            graphics.nextStratum();
            blitRoundedTexture(graphics, id, value, value2, value3, value3, f, 0.625F, 0.125F, 0.75F, 0.25F, value4);
        }
    }

    public static float getHeadCornerRadius(float value) {
        return Math.max(1.5F, value * 0.1F);
    }

    public static void blitSubRectUv(
        GuiGraphicsExtractor graphics,
        GpuTextureView gpuTextureView,
        GpuSampler gpuSampler,
        int value,
        int value2,
        int value3,
        int value4,
        int value5,
        int value6,
        int value7,
        int value8,
        float value9,
        float value10,
        float value11,
        float value12,
        int value13
    ) {
        if (value3 > value && value4 > value2) {
            float f = value9 + (value11 - value9) * (value - value5) / value7;
            float f1 = value9 + (value11 - value9) * (value3 - value5) / value7;
            float f2 = value10 + (value12 - value10) * (value2 - value6) / value8;
            float f3 = value10 + (value12 - value10) * (value4 - value6) / value8;
            ((GuiGraphicsInvoker)graphics).adapt$innerBlit(RenderPipelines.GUI_TEXTURED, gpuTextureView, gpuSampler, value, value2, value3, value4, f, f1, f2, f3, value13);
        }
    }

    public static void fillSampledRoundedRect(
        GuiGraphicsExtractor graphics, float x, float y, float width, float height, float radius, float value, Render2D.GradientSampler gradientSampler
    ) {
        if (!(value <= 0.01F) && !(width <= 0.0F) && !(height <= 0.0F)) {
            int i = getGuiScale();
            int j = toPx(x, i);
            int k = toPx(x + width, i) - j;
            float f = y * i;
            float f1 = (y + height) * i;
            float f2 = f1 - f;
            if (k > 0 && !(f2 <= 0.0F)) {
                float f3 = Math.min(radius * i, Math.min(k, f2) / 2.0F);
                pushPixelScale(graphics, i);

                for (int l = 0; l < k; l++) {
                    float f4 = Math.min(l + 0.5F, k - l - 0.5F);
                    float f5 = f4 < f3 ? f3 - (float)Math.sqrt(Math.max(0.0F, f3 * f3 - (f3 - f4) * (f3 - f4))) : 0.0F;
                    float f6 = f + f5;
                    float f7 = f1 - f5;
                    int i1 = (int)Math.ceil(f6);
                    int j1 = (int)Math.floor(f7);
                    float f8 = (l + 0.5F) / k;
                    if (j1 > i1) {
                        graphics.fillGradient(
                            j + l,
                            i1,
                            j + l + 1,
                            j1,
                            ColorUtility.scaleAlpha(gradientSampler.sample(f8, (i1 - f) / f2), value),
                            ColorUtility.scaleAlpha(gradientSampler.sample(f8, (j1 - f) / f2), value)
                        );
                        float f9 = i1 - f6;
                        float f10 = f7 - j1;
                        if (f9 > 0.02F) {
                            graphics.fill(j + l, i1 - 1, j + l + 1, i1, ColorUtility.scaleAlpha(gradientSampler.sample(f8, (i1 - 0.5F - f) / f2), value * f9));
                        }

                        if (f10 > 0.02F) {
                            graphics.fill(j + l, j1, j + l + 1, j1 + 1, ColorUtility.scaleAlpha(gradientSampler.sample(f8, (j1 + 0.5F - f) / f2), value * f10));
                        }
                    } else if (f7 > f6) {
                        int k1 = (int)Math.floor(f6);
                        graphics.fill(j + l, k1, j + l + 1, k1 + 1, ColorUtility.scaleAlpha(gradientSampler.sample(f8, 0.5F), value * (f7 - f6)));
                    }
                }

                popPose(graphics);
            }
        }
    }

    public static void setContourFlags(int contourFlags2) {
        contourFlags = contourFlags2;
    }

    public static void emitAntialiasedSpan(float value, float value2, int value3, int value4, Render2D.SpanSink spanSink) {
        if (!(value2 - value <= 0.001F)) {
            int i = (int)Math.floor(value);
            int j = (int)Math.floor(value2);
            if (i == j) {
                spanSink.accept(i, value3, i + 1, value4, value2 - value);
            } else {
                float f = i + 1 - value;
                float f1 = value2 - j;
                if (f > 0.001F) {
                    spanSink.accept(i, value3, i + 1, value4, f);
                }

                if (j > i + 1) {
                    spanSink.accept(i + 1, value3, j, value4, 1.0F);
                }

                if (f1 > 0.001F) {
                    spanSink.accept(j, value3, j + 1, value4, f1);
                }
            }
        }
    }

    public static void forEachMergedScanline(Render2D.Scanlines scanlines, Render2D.SpanSink spanSink) {
        int i = 0;

        while (i < scanlines.height) {
            int j = i + 1;

            while (j < scanlines.height && scanlines.leftX[j] == scanlines.leftX[i] && scanlines.rightX[j] == scanlines.rightX[i]) {
                j++;
            }

            emitAntialiasedSpan(scanlines.leftX[i], scanlines.rightX[i], scanlines.startY + i, scanlines.startY + j, spanSink);
            i = j;
        }
    }

    public static void fillSteppedContour(GuiGraphicsExtractor graphics, float[][] floatArrays, float first, int second) {
        if (ColorUtility.getAlpha(second) != 0) {
            Render2D.Scanlines render2d$scanlines = new Render2D.Scanlines(floatArrays, first, getEffectiveScale(graphics));
            if (render2d$scanlines.height != 0) {
                pushEffectiveScale(graphics, getEffectiveScale(graphics));
                StripBatch stripbatch = StripBatch.beginFill(graphics);
                rasterizeContourFill(render2d$scanlines, second, stripbatch != null ? stripbatch::addFill : graphics::fill);
                if (stripbatch != null) {
                    stripbatch.submit(graphics);
                }

                popPose(graphics);
            }
        }
    }

    public static void blurSteppedContour(GuiGraphicsExtractor graphics, float[][] floatArrays, float first, float second) {
        GpuTextureView gputextureview = BackdropBlur.getTextureView();
        if (gputextureview != null && !(second <= 0.01F)) {
            Render2D.Scanlines render2d$scanlines = new Render2D.Scanlines(floatArrays, first, getEffectiveScale(graphics));
            if (render2d$scanlines.height != 0) {
                GpuSampler gpusampler = BackdropBlur.getSampler();
                float f = getEffectiveScale(graphics);
                float f1 = BackdropBlur.getWidth();
                float f2 = BackdropBlur.getHeight();
                float f3 = Float.MAX_VALUE;
                float f4 = -Float.MAX_VALUE;

                for (int i = 0; i < render2d$scanlines.height; i++) {
                    f3 = Math.min(f3, render2d$scanlines.leftX[i]);
                    f4 = Math.max(f4, render2d$scanlines.rightX[i]);
                }

                backdropAlpha = Math.max(0.0F, Math.min(1.0F, second));
                pushEffectiveScale(graphics, f);

                try {
                    Vector2f vector2f = graphics.pose()
                        .transformPosition((f3 + f4) / 2.0F, render2d$scanlines.startY + render2d$scanlines.height / 2.0F, new Vector2f());
                    refractCenterX = vector2f.x * getGuiScale();
                    refractCenterY = vector2f.y * getGuiScale();
                    refractZoom = Theme.getLensZoom();
                    beginBackdropBatch(graphics, gputextureview, gpusampler);
                    forEachMergedScanline(
                        render2d$scanlines,
                        (value, value2, value3, value4, value5) -> emitBackdropQuad(
                            graphics,
                            gputextureview,
                            gpusampler,
                            value,
                            value2,
                            value3,
                            value4,
                            f1,
                            f2,
                            value5 >= 0.999F ? -1 : ColorUtility.withAlphaFloat(16777215, square(value5))
                        )
                    );
                } finally {
                    endBackdropBatch(graphics);
                    popPose(graphics);
                    backdropAlpha = 1.0F;
                }
            }
        }
    }

    public static void strokeSteppedContourInner(GuiGraphicsExtractor graphics, float[][] floatArrays, float value, float value2, int value3) {
        strokeSteppedContour(graphics, floatArrays, value, value2, false, value3);
    }

    public static void strokeSteppedContour(GuiGraphicsExtractor graphics, float[][] floatArrays, float value, float value2, boolean flag, int value3) {
        if (ColorUtility.getAlpha(value3) != 0 && !(value2 <= 0.0F)) {
            Render2D.Scanlines render2d$scanlines = new Render2D.Scanlines(floatArrays, value, getEffectiveScale(graphics));
            if (render2d$scanlines.height != 0) {
                pushEffectiveScale(graphics, getEffectiveScale(graphics));
                StripBatch stripbatch = StripBatch.beginFill(graphics);
                Render2D.FillSink render2d$fillsink = stripbatch != null ? stripbatch::addFill : graphics::fill;
                if (flag) {
                    rasterizeContourOuterGlow(render2d$scanlines, value2, value3, render2d$fillsink);
                } else {
                    rasterizeContourInnerBorder(render2d$scanlines, value2, value3, render2d$fillsink);
                }

                if (stripbatch != null) {
                    stripbatch.submit(graphics);
                }

                popPose(graphics);
            }
        }
    }

    public static void rasterizeContourFill(Render2D.Scanlines scanlines, int value, Render2D.FillSink fillSink) {
        forEachMergedScanline(
            scanlines, (value2, value22, value3, value4, value5) -> fillSink.addFill(value2, value22, value3, value4, value5 >= 0.999F ? value : ColorUtility.scaleAlpha(value, value5))
        );
    }

    public static void rasterizeContourInnerBorder(Render2D.Scanlines scanlines, float first, int second, Render2D.FillSink fillSink) {
        int i = Math.max(1, Math.round(first));
        float[] afloat = new float[scanlines.height];
        float[] afloat1 = new float[scanlines.height];

        for (int j = 0; j < scanlines.height; j++) {
            float f = scanlines.leftX[j];
            float f1 = scanlines.rightX[j];
            float f2 = f + first;
            float f3 = f1 - first;
            if (j >= i && j < scanlines.height - i) {
                for (int k = j - i; k <= j + i; k++) {
                    if (k != j) {
                        f2 = Math.max(f2, scanlines.leftX[k]);
                        f3 = Math.min(f3, scanlines.rightX[k]);
                    }
                }

                if (f3 <= f2) {
                    f3 = Float.NaN;
                    f2 = Float.NaN;
                }
            } else {
                f3 = Float.NaN;
                f2 = Float.NaN;
            }

            afloat[j] = f2;
            afloat1[j] = f3;
        }

        Render2D.SpanSink render2d$spansink = (value, value2, value3, value4, value5) -> fillSink.addFill(
            value, value2, value3, value4, value5 >= 0.999F ? second : ColorUtility.scaleAlpha(second, value5)
        );
        int l = 0;

        while (l < scanlines.height) {
            int i1 = l + 1;

            while (
                i1 < scanlines.height
                    && scanlines.leftX[i1] == scanlines.leftX[l]
                    && scanlines.rightX[i1] == scanlines.rightX[l]
                    && Float.compare(afloat[i1], afloat[l]) == 0
                    && Float.compare(afloat1[i1], afloat1[l]) == 0
            ) {
                i1++;
            }

            int j1 = scanlines.startY + l;
            int k1 = scanlines.startY + i1;
            if (Float.isNaN(afloat[l])) {
                emitAntialiasedSpan(scanlines.leftX[l], scanlines.rightX[l], j1, k1, render2d$spansink);
            } else {
                emitAntialiasedSpan(scanlines.leftX[l], afloat[l], j1, k1, render2d$spansink);
                emitAntialiasedSpan(afloat1[l], scanlines.rightX[l], j1, k1, render2d$spansink);
            }

            l = i1;
        }
    }

    public static void rasterizeContourOuterGlow(Render2D.Scanlines scanlines, float first, int second, Render2D.FillSink fillSink) {
        int i = (int)Math.ceil(first);

        for (int j = -i; j < scanlines.height + i; j++) {
            boolean flag = j >= 0 && j < scanlines.height;
            int k = j < 0 ? -j : j - scanlines.height + 1;
            float f = flag ? 1.0F : Math.max(0.0F, Math.min(1.0F, first - k + 1.0F));
            if (!(f <= 0.01F)) {
                float f1 = Float.MAX_VALUE;
                float f2 = -Float.MAX_VALUE;

                for (int l = Math.max(0, j - i); l <= Math.min(scanlines.height - 1, j + i); l++) {
                    float f3 = l - j;
                    float f4 = (float)Math.sqrt(Math.max(0.0F, first * first - f3 * f3));
                    f1 = Math.min(f1, scanlines.leftX[l] - f4);
                    f2 = Math.max(f2, scanlines.rightX[l] + f4);
                }

                if (!(f2 <= f1)) {
                    int i1 = f >= 0.999F ? second : ColorUtility.scaleAlpha(second, f);
                    Render2D.SpanSink render2d$spansink = (value, value2, value3, value4, value5) -> fillSink.addFill(
                        value, value2, value3, value4, value5 >= 0.999F ? i1 : ColorUtility.scaleAlpha(i1, value5)
                    );
                    int j1 = scanlines.startY + j;
                    if (!flag) {
                        emitAntialiasedSpan(f1, f2, j1, j1 + 1, render2d$spansink);
                    } else {
                        emitAntialiasedSpan(f1, scanlines.leftX[j], j1, j1 + 1, render2d$spansink);
                        emitAntialiasedSpan(scanlines.rightX[j], f2, j1, j1 + 1, render2d$spansink);
                    }
                }
            }
        }
    }

    public static void drawCachedSteppedPanel(GuiGraphicsExtractor graphics, String text, float[][] floatArrays, float value, int value2, int value3, float value4, boolean flag) {
        float f = getEffectiveScale(graphics);
        Render2D.Scanlines render2d$scanlines = new Render2D.Scanlines(floatArrays, value, f);
        if (render2d$scanlines.height != 0) {
            ShapeCache.Baked shapecache$baked = ShapeCache.get(text).bake(render2d$scanlines, value2, value3, value4, flag);
            pushEffectiveScale(graphics, f);
            if (shapecache$baked != null) {
                blit(
                    graphics,
                    shapecache$baked.id(),
                    shapecache$baked.x(),
                    shapecache$baked.y(),
                    shapecache$baked.w(),
                    shapecache$baked.h(),
                    0.0F,
                    0.0F,
                    shapecache$baked.w(),
                    shapecache$baked.h(),
                    shapecache$baked.texW(),
                    shapecache$baked.texH(),
                    -1
                );
            } else {
                StripBatch stripbatch = StripBatch.beginFill(graphics);
                Render2D.FillSink render2d$fillsink = stripbatch != null ? stripbatch::addFill : graphics::fill;
                if (ColorUtility.getAlpha(value2) != 0) {
                    rasterizeContourFill(render2d$scanlines, value2, render2d$fillsink);
                }

                if (value4 > 0.0F && ColorUtility.getAlpha(value3) != 0) {
                    if (flag) {
                        rasterizeContourOuterGlow(render2d$scanlines, value4, value3, render2d$fillsink);
                    } else {
                        rasterizeContourInnerBorder(render2d$scanlines, value4, value3, render2d$fillsink);
                    }
                }

                if (stripbatch != null) {
                    stripbatch.submit(graphics);
                }
            }

            popPose(graphics);
        }
    }

    public static void drawChart(
        GuiGraphicsExtractor graphics, String text, float value, float value2, float value3, float value4, float[] floats, float value5, int value6, int value7, int value8
    ) {
        float f = getEffectiveScale(graphics);
        int i = toScaledPx(value, f);
        int j = toScaledPx(value2, f);
        int k = toScaledPx(value + value3, f) - i;
        int l = toScaledPx(value2 + value4, f) - j;
        ChartCache.Baked chartcache$baked = ChartCache.get(text).bake(k, l, floats, Math.max(1.0E-4F, value5), value6, value7, value8, Math.max(1.5F, 1.1F * f));
        if (chartcache$baked != null) {
            pushEffectiveScale(graphics, f);
            blit(
                graphics,
                chartcache$baked.id(),
                i,
                j,
                chartcache$baked.w(),
                chartcache$baked.h(),
                0.0F,
                0.0F,
                chartcache$baked.w(),
                chartcache$baked.h(),
                chartcache$baked.texW(),
                chartcache$baked.texH(),
                -1
            );
            popPose(graphics);
        }
    }

    public static void fillBottomClippedSampledRect(
        GuiGraphicsExtractor graphics, float x, float y, float width, float height, float value, float value2, float value3, Render2D.GradientSampler gradientSampler
    ) {
        if (!(value3 <= 0.01F) && !(width <= 0.0F) && !(height <= 0.0F) && !(value2 <= 0.0F)) {
            int i = getGuiScale();
            int j = toPx(x, i);
            int k = toPx(x + width, i) - j;
            int l = Math.min(k, toPx(x + value2, i) - j);
            float f = y * i;
            float f1 = (y + height) * i;
            float f2 = f1 - f;
            if (k > 0 && l > 0 && !(f2 <= 0.0F)) {
                float f3 = value * i;
                pushPixelScale(graphics, i);

                for (int i1 = 0; i1 < l; i1++) {
                    float f4 = Math.min(i1 + 0.5F, k - i1 - 0.5F);
                    float f5 = f4 < f3 ? f3 - (float)Math.sqrt(Math.max(0.0F, f3 * f3 - (f3 - f4) * (f3 - f4))) : 0.0F;
                    float f6 = f1 - f5;
                    if (!(f6 <= f)) {
                        int j1 = Math.round(f);
                        int k1 = (int)Math.floor(f6);
                        float f7 = (i1 + 0.5F) / k;
                        if (k1 > j1) {
                            graphics.fillGradient(
                                j + i1,
                                j1,
                                j + i1 + 1,
                                k1,
                                ColorUtility.scaleAlpha(gradientSampler.sample(f7, 0.0F), value3),
                                ColorUtility.scaleAlpha(gradientSampler.sample(f7, (k1 - f) / f2), value3)
                            );
                        }

                        float f8 = f6 - Math.max(k1, j1);
                        if (f8 > 0.02F) {
                            int l1 = Math.max(k1, j1);
                            graphics.fill(
                                j + i1,
                                l1,
                                j + i1 + 1,
                                l1 + 1,
                                ColorUtility.scaleAlpha(gradientSampler.sample(f7, Math.min(1.0F, (l1 + 0.5F - f) / f2)), value3 * Math.min(1.0F, f8))
                            );
                        }
                    }
                }

                popPose(graphics);
            }
        }
    }

    public static void fillBottomRoundedProgressBar(
        GuiGraphicsExtractor graphics, float x, float y, float width, float height, float radius, float value, int value2, int value3, float value4, float value5
    ) {
        if (!(value5 <= 0.01F) && !(width <= 0.0F) && !(height <= 0.0F) && !(value <= 0.0F)) {
            int i = getGuiScale();
            int j = toPx(x, i);
            int k = toPx(y, i);
            int l = toPx(x + width, i);
            int i1 = toPx(y + height, i);
            int j1 = l - j;
            int k1 = i1 - k;
            if (j1 > 0 && k1 > 0) {
                int l1 = Math.max(0, Math.min(toPx(radius, i), j1 / 2));
                int i2 = Math.min(l1, k1);
                int j2 = j + Math.max(1, Math.min(j1, toPx(value, i)));
                IntUnaryOperator intunaryoperator = value6 -> ColorUtility.lerpColor(value2, value3, (value6 - j + 0.5F) / j1);
                pushPixelScale(graphics, i);
                int k2 = Math.max(2, 2 * i);
                int[][] aint = new int[][]{{j, Math.min(j2, j + l1), 1}, {j + l1, Math.min(j2, l - l1), 0}, {l - l1, j2, 1}};

                for (int[] aint1 : aint) {
                    int l2 = aint1[2] == 1 ? i1 - i2 : i1;
                    if (l2 > k) {
                        for (int i3 = aint1[0]; i3 < aint1[1]; i3 += k2) {
                            int j3 = Math.min(aint1[1], i3 + k2);
                            int k3 = ColorUtility.scaleAlpha(intunaryoperator.applyAsInt((i3 + j3) / 2), value5);
                            int l3 = ColorUtility.lerpColor(k3, multiplyRgb(k3, value4), (float)(l2 - k) / k1);
                            graphics.fillGradient(i3, k, j3, l2, k3, l3);
                        }
                    }
                }

                if (l1 > 0) {
                    Identifier identifier = TextureCache.getCorner(l1);
                    int i4 = Math.min(l1, j2 - j);
                    int j4 = j2 - (l - l1);
                    int k4 = ColorUtility.scaleAlpha(intunaryoperator.applyAsInt(j + l1 / 2), value5);
                    int l4 = ColorUtility.scaleAlpha(intunaryoperator.applyAsInt(l - l1 / 2), value5);

                    for (int i5 = 0; i5 < i2; i5++) {
                        int j5 = i1 - i2 + i5;
                        float f = (j5 + 0.5F - k) / k1;
                        if (i4 > 0) {
                            blit(graphics, identifier, j, j5, i4, 1, 0.0F, i2 - i5, i4, -1, l1, l1, ColorUtility.lerpColor(k4, multiplyRgb(k4, value4), f));
                        }

                        if (j4 > 0) {
                            blit(graphics, identifier, l - l1, j5, j4, 1, l1, i2 - i5, -j4, -1, l1, l1, ColorUtility.lerpColor(l4, multiplyRgb(l4, value4), f));
                        }
                    }
                }

                popPose(graphics);
            }
        }
    }

    public static int multiplyRgb(int color, float value) {
        int i = (int)((color >> 16 & 0xFF) * value);
        int j = (int)((color >> 8 & 0xFF) * value);
        int k = (int)((color & 0xFF) * value);
        return color & 0xFF000000 | i << 16 | j << 8 | k;
    }

    public static void fillClippedRoundedBand(
        GuiGraphicsExtractor graphics, float x, float y, float width, float height, float radius, float first, float second, int color
    ) {
        fillClippedRoundedVerticalGradient(graphics, x, y, width, height, radius, first, second, color, color);
    }

    public static void fillClippedRoundedVerticalGradient(
        GuiGraphicsExtractor graphics, float x, float y, float width, float height, float radius, float first, float second, int startColor, int color
    ) {
        if ((ColorUtility.getAlpha(startColor) != 0 || ColorUtility.getAlpha(color) != 0) && !(second <= 0.0F)) {
            float f = getEffectiveScale(graphics);
            float f1 = x * f;
            float f2 = (x + width) * f;
            float f3 = y * f;
            float f4 = (y + height) * f;
            float f5 = Math.min(radius * f, Math.min(f2 - f1, f4 - f3) / 2.0F);
            float f6 = Math.max(f3, first * f);
            float f7 = Math.min(f4, (first + second) * f);
            if (!(f7 <= f6)) {
                pushEffectiveScale(graphics, f);

                for (int i = (int)Math.floor(f6); i < (int)Math.ceil(f7); i++) {
                    float f8 = Math.min(i + 1, f7) - Math.max(i, f6);
                    if (!(f8 <= 0.01F)) {
                        float f9 = i + 0.5F;
                        float f10 = Math.min(f9 - f3, f4 - f9);
                        float f11 = f10 < f5 ? f5 - (float)Math.sqrt(Math.max(0.0F, f5 * f5 - (f5 - f10) * (f5 - f10))) : 0.0F;
                        float f12 = f1 + f11;
                        float f13 = f2 - f11;
                        if (!(f13 <= f12)) {
                            float f14 = Math.max(0.0F, Math.min(1.0F, (f9 - first * f) / (second * f)));
                            int j = ColorUtility.lerpColor(startColor, color, f14);
                            int k = f8 >= 0.999F ? j : ColorUtility.scaleAlpha(j, f8);
                            int l = (int)Math.ceil(f12);
                            int i1 = (int)Math.floor(f13);
                            if (i1 > l) {
                                graphics.fill(l, i, i1, i + 1, k);
                            }

                            float f15 = l - f12;
                            float f16 = f13 - i1;
                            if (f15 > 0.02F) {
                                graphics.fill(l - 1, i, l, i + 1, ColorUtility.scaleAlpha(k, f15));
                            }

                            if (f16 > 0.02F) {
                                graphics.fill(i1, i, i1 + 1, i + 1, ColorUtility.scaleAlpha(k, f16));
                            }
                        }
                    }
                }

                popPose(graphics);
            }
        }
    }

    public static void fillClippedRoundedHorizontalHighlight(
        GuiGraphicsExtractor graphics, float x, float y, float width, float height, float radius, float value, float value2, float value3, float value4, int color
    ) {
        if (ColorUtility.getAlpha(color) != 0 && !(value2 <= 0.0F) && !(value4 <= 0.0F)) {
            float f = getEffectiveScale(graphics);
            float f1 = x * f;
            float f2 = (x + width) * f;
            float f3 = y * f;
            float f4 = (y + height) * f;
            float f5 = Math.min(radius * f, Math.min(f2 - f1, f4 - f3) / 2.0F);
            float f6 = Math.max(f3, value * f);
            float f7 = Math.min(f4, (value + value2) * f);
            float f8 = value3 * f;
            float f9 = value4 * f;
            if (!(f7 <= f6)) {
                pushEffectiveScale(graphics, f);

                for (int i = (int)Math.floor(f6); i < (int)Math.ceil(f7); i++) {
                    float f10 = Math.min(i + 1, f7) - Math.max(i, f6);
                    if (!(f10 <= 0.01F)) {
                        float f11 = i + 0.5F;
                        float f12 = Math.min(f11 - f3, f4 - f11);
                        float f13 = f12 < f5 ? f5 - (float)Math.sqrt(Math.max(0.0F, f5 * f5 - (f5 - f12) * (f5 - f12))) : 0.0F;
                        int j = Math.round(f1 + f13);
                        int k = Math.round(f2 - f13);

                        for (int l = 0; l < 2; l++) {
                            float f14 = l == 0 ? f8 - f9 : f8;
                            float f15 = l == 0 ? f8 : f8 + f9;
                            int i1 = Math.max(j, Math.round(f14));
                            int j1 = Math.min(k, Math.round(f15));
                            if (j1 > i1) {
                                float f16 = tentFalloff(i1, f8, f9);
                                float f17 = tentFalloff(j1, f8, f9);
                                fillHorizontalGradientPx(
                                    graphics, i1, i, j1, i + 1, ColorUtility.scaleAlpha(color, f16 * f10), ColorUtility.scaleAlpha(color, f17 * f10)
                                );
                            }
                        }
                    }
                }

                popPose(graphics);
            }
        }
    }

    public static float tentFalloff(float value, float value2, float value3) {
        return Math.max(0.0F, 1.0F - Math.abs(value - value2) / value3);
    }

    public static float snapToPixel(float value) {
        int i = getGuiScale();
        return (float)Math.round(value * i) / i;
    }

    public static boolean containsPoint(double px, double py, float x, float y, float width, float height) {
        return px >= x && px < x + width && py >= y && py < y + height;
    }

    @FunctionalInterface
    private interface CircleDrawer {
        void draw(float value, float value2, float value3);
    }

    @FunctionalInterface
    interface FillSink {
        void addFill(int value, int value2, int value3, int value4, int value5);
    }

    @FunctionalInterface
    public interface GradientSampler {
        int sample(float first, float second);
    }

    static final class Scanlines {
        public int startY;
        public int height;
        public float[] leftX;
        public float[] rightX;

        public Scanlines(float[][] floatArrays, float first, float second) {
            int i = Render2D.contourFlags;
            List<float[]> arraylist = new ArrayList<>();

            for (float[] afloat : floatArrays) {
                float f = afloat[0] * second;
                float f1 = afloat[1] * second;
                float f2 = Math.round(afloat[2] * second);
                float f3 = Math.round(afloat[3] * second);
                if (f1 - f > 0.5F && f3 > f2) {
                    arraylist.add(new float[]{f, f1, f2, f3});
                }
            }

            int k = arraylist.size();
            if (k != 0) {
                for (int l = 1; l < k; l++) {
                    ((float[])arraylist.get(l))[2] = ((float[])arraylist.get(l - 1))[3];
                }

                float f10 = Math.max(0.0F, first * second);
                this.startY = (int)((float[])arraylist.get(0))[2];
                this.height = (int)((float[])arraylist.get(k - 1))[3] - this.startY;
                this.leftX = new float[this.height];
                this.rightX = new float[this.height];

                for (int i1 = 0; i1 < k; i1++) {
                    float[] afloat3 = (float[])arraylist.get(i1);
                    float f11 = afloat3[0];
                    float f12 = afloat3[1];
                    float f13 = afloat3[2];
                    float f14 = afloat3[3];
                    float f4 = f14 - f13;
                    float[] afloat1 = i1 > 0 ? (float[])arraylist.get(i1 - 1) : null;
                    float[] afloat2 = i1 < k - 1 ? (float[])arraylist.get(i1 + 1) : null;
                    float f5 = afloat1 == null && (i & 4) != 0 ? 0.0F : f10;
                    float f6 = afloat2 == null && (i & 8) != 0 ? 0.0F : f10;
                    boolean flag = (i & 1) != 0;
                    boolean flag1 = (i & 2) != 0;

                    for (int j = (int)f13; j < (int)f14; j++) {
                        float f7 = j + 0.5F;
                        float f8 = f7 - f13;
                        float f9 = f14 - f7;
                        this.leftX[j - this.startY] = f11
                            + (flag ? 0.0F : cornerInset(f11, afloat1, true, f5, f4, f12 - f11, f8) + cornerInset(f11, afloat2, true, f6, f4, f12 - f11, f9));
                        this.rightX[j - this.startY] = f12
                            - (flag1 ? 0.0F : cornerInset(f12, afloat1, false, f5, f4, f12 - f11, f8) + cornerInset(f12, afloat2, false, f6, f4, f12 - f11, f9));
                    }
                }
            }
        }

        public static float cornerInset(float value, float[] floats, boolean flag, float value2, float value3, float value4, float value5) {
            if (floats == null) {
                float f2 = Math.min(value2, Math.min(value3, value4) / 2.0F);
                return capInset(f2, value5);
            } else {
                float f = flag ? floats[0] : floats[1];
                float f1 = flag ? f - value : value - f;
                return f1 <= 0.05F ? 0.0F : stepInset(Math.min(value2, f1), Math.min(value2, value3 / 2.0F), value5);
            }
        }

        public static float stepInset(float value, float value2, float value3) {
            if (!(value3 >= value2) && !(value2 <= 0.0F) && !(value <= 0.0F)) {
                float f = (value2 - value3) / value2;
                return value - value * (float)Math.sqrt(Math.max(0.0F, 1.0F - f * f));
            } else {
                return 0.0F;
            }
        }

        public static float capInset(float first, float second) {
            if (!(second >= first) && !(first <= 0.0F)) {
                float f = first - second;
                return first - (float)Math.sqrt(Math.max(0.0F, first * first - f * f));
            } else {
                return 0.0F;
            }
        }
    }

    @FunctionalInterface
    private interface SpanSink {
        void accept(int value, int value2, int value3, int value4, float value5);
    }
}
