package dev.adapt.v3.ui.render;

import dev.adapt.v3.util.render.ColorUtility;
import java.awt.Font;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import org.joml.Matrix3x2fStack;
import org.joml.Vector2f;

public enum Fonts {
    LIGHT("poppins-light", 9.0F),
    REGULAR("poppins-regular", 9.0F),
    MEDIUM("poppins-medium", 9.0F),
    SEMIBOLD("poppins-semibold", 9.0F),
    SMALL("poppins-regular", 7.5F),
    ROW("poppins-regular", 8.0F),
    LIST("poppins-medium", 8.0F),
    TITLE("poppins-semibold", 12.0F),
    WORDMARK("poppins-bold", 18.338108F),
    DISPLAY("poppins-semibold", 34.0F),
    GREETING("poppins-semibold", 15.0F),
    GREETING_SUB("poppins-regular", 9.0F),
    GLOW_TITLE("inter-semibold", 15.0F),
    GLOW_TAG("inter-regular", 10.0F),
    GLOW_ROW("inter-regular", 9.0F),
    ICONS("materialicons", 10.0F),
    ICONS_LG("materialicons", 15.0F);

    public static final Map<String, Font> BASE_FONTS = new HashMap<>();
    public static final Map<String, GlyphPage> SHARED_PAGES = new HashMap<>();
    public final String fontName;
    public final float pointSize;
    public final Map<Integer, GlyphPage> pagesByPixelSize = new HashMap<>();
    public final Map<Long, GlyphPage> blurredPages = new HashMap<>();
    public float lastScale = Float.NaN;
    public int lastGuiScale = -1;
    public GlyphPage lastPage;
    public static boolean nativeTransformScale;

    Fonts(String text, float value) {
        this.fontName = text;
        this.pointSize = value;
    }

    public float getPointSize() {
        return this.pointSize;
    }

    public boolean isIconFont() {
        return this == ICONS || this == ICONS_LG;
    }

    public static int getGuiScale() {
        return Math.max(1, Minecraft.getInstance().getWindow().getGuiScale());
    }

    public static Font loadAwtFont(String text) {
        return BASE_FONTS.computeIfAbsent(text, text2 -> {
            try (InputStream inputstream = Fonts.class.getResourceAsStream("/assets/adaptv3/fonts/" + text2 + ".ttf")) {
                return Font.createFont(0, inputstream);
            } catch (Exception exception) {
                return new Font("SansSerif", 0, 12);
            }
        });
    }

    public GlyphPage getPage(float value) {
        int i = getGuiScale();
        if (value == this.lastScale && i == this.lastGuiScale && this.lastPage != null) {
            return this.lastPage;
        }

        float f = Math.max(4.0F, value == 1.0F ? Math.round(this.pointSize * i * 4.0F) / 4.0F : Math.round(this.pointSize * value * i));
        GlyphPage glyphpage = this.pagesByPixelSize.get(Float.floatToIntBits(f));
        if (glyphpage == null) {
            String s = this.fontName + "@" + f;
            glyphpage = SHARED_PAGES.computeIfAbsent(
                s, text -> new GlyphPage(loadAwtFont(this.fontName), f, this.fontName + "_" + String.valueOf(f).replace('.', '_'))
            );
            this.pagesByPixelSize.put(Float.floatToIntBits(f), glyphpage);
        }

        this.lastScale = value;
        this.lastGuiScale = i;
        this.lastPage = glyphpage;
        return glyphpage;
    }

    public float getWidth(String text) {
        return this.getWidth(text, 1.0F);
    }

    public float getWidth(String text, float value) {
        return text != null && !text.isEmpty() ? this.getPage(value).measureWidth(text) / getGuiScale() : 0.0F;
    }

    public float getLineHeight() {
        GlyphPage glyphpage = this.getPage(1.0F);
        return (glyphpage.ascent + glyphpage.descent) / getGuiScale();
    }

    public float getCapHeight() {
        return this.getPage(1.0F).capHeight / getGuiScale();
    }

    public float getHalfPointSize() {
        return this.pointSize * 0.5F;
    }

    public float getBaselineOffset(float value) {
        GlyphPage glyphpage = this.getPage(value);
        return this.pointSize * value * 0.5F + glyphpage.capHeight / 2.0F / getGuiScale();
    }

    public void drawVCentered(GuiGraphicsExtractor graphics, String text, float value, float value2, int value3) {
        this.draw(graphics, text, value, value2 - this.getHalfPointSize(), value3);
    }

    public void drawCentered(GuiGraphicsExtractor graphics, String text, float value, float value2, int value3) {
        this.draw(graphics, text, value - this.getWidth(text) / 2.0F, value2 - this.getHalfPointSize(), value3);
    }

    public void draw(GuiGraphicsExtractor graphics, String text, float value, float value2, int value3) {
        this.drawScaled(graphics, text, value, value2, 1.0F, value3);
    }

    public void drawScaled(GuiGraphicsExtractor graphics, String text, float scale, float scale2, float scale3, int value) {
        this.drawGlyphs(graphics, (first, second) -> this.getPage(first), text, scale, scale2, scale3, value);
    }

    public static void setNativeTransformScale(boolean nativeTransformScale2) {
        nativeTransformScale = nativeTransformScale2;
    }

    public GlyphPage getBlurredPage(float first, int second) {
        if (second <= 0) {
            return this.getPage(first);
        }

        int i = getGuiScale();
        float f = Math.max(4.0F, first == 1.0F ? Math.round(this.pointSize * i * 4.0F) / 4.0F : Math.round(this.pointSize * first * i));
        long j = (long)Float.floatToIntBits(f) << 32 | second & 4294967295L;
        GlyphPage glyphpage = this.blurredPages.get(j);
        if (glyphpage == null) {
            String s = this.fontName + "@" + f + "#b" + second;
            glyphpage = SHARED_PAGES.computeIfAbsent(
                s, text -> new GlyphPage(loadAwtFont(this.fontName), f, this.fontName + "_" + String.valueOf(f).replace('.', '_') + "_b" + second, second)
            );
            this.blurredPages.put(j, glyphpage);
        }

        return glyphpage;
    }

    public void drawWithGlowAndShadow(
        GuiGraphicsExtractor graphics,
        String text,
        float value,
        float value2,
        float value3,
        int value4,
        int value5,
        float value6,
        int value7,
        float value8,
        float value9,
        float value10
    ) {
        if (text != null && !text.isEmpty()) {
            int i = getGuiScale();
            if (ColorUtility.getAlpha(value5) >= 4) {
                this.drawGlyphs(graphics, (first, second) -> this.getBlurredPage(first, Math.round(value6 * i * second)), text, value, value2, value3, value5);
            }

            if (ColorUtility.getAlpha(value7) >= 4) {
                this.drawGlyphs(graphics, (first2, second2) -> this.getBlurredPage(first2, Math.round(value10 * i * second2)), text, value + value8, value2 + value9, value3, value7);
            }

            this.drawGlyphs(graphics, (first3, second3) -> this.getPage(first3), text, value, value2, value3, value4);
        }
    }

    public void drawCached(GuiGraphicsExtractor graphics, String text, float value, float value2, int value3) {
        this.drawInternal(graphics, (first, second) -> this.getPage(first), text, value, value2, 1.0F, value3, true);
    }

    public void drawCachedBlurred(GuiGraphicsExtractor graphics, String text, float value, float value2, int value3, float value4) {
        if (text != null && !text.isEmpty() && ColorUtility.getAlpha(value3) >= 4) {
            int i = getGuiScale();
            this.drawInternal(graphics, (first, second) -> this.getBlurredPage(first, Math.round(value4 * i * second)), text, value, value2, 1.0F, value3, true);
        }
    }

    public void drawGlyphs(GuiGraphicsExtractor graphics, Fonts.PageProvider pageProvider, String text, float value, float value2, float value3, int value4) {
        this.drawInternal(graphics, pageProvider, text, value, value2, value3, value4, false);
    }

    public void drawInternal(GuiGraphicsExtractor graphics, Fonts.PageProvider pageProvider, String text, float value, float value2, float value3, int value4, boolean flag) {
        if (text != null && !text.isEmpty() && ColorUtility.getAlpha(value4) != 0) {
            Matrix3x2fStack matrix3x2fstack = graphics.pose();
            float f = matrix3x2fstack.m00();
            if (nativeTransformScale
                && Math.abs(f - 1.0F) > 0.001F
                && f > 0.05F
                && Math.abs(matrix3x2fstack.m11() - f) < 1.0E-4F
                && Math.abs(matrix3x2fstack.m01()) < 1.0E-4F
                && Math.abs(matrix3x2fstack.m10()) < 1.0E-4F) {
                Vector2f vector2f = matrix3x2fstack.transformPosition(value, value2, new Vector2f());
                matrix3x2fstack.pushMatrix();
                matrix3x2fstack.identity();
                if (flag) {
                    this.blitTextMask(graphics, pageProvider.getPage(value3 * f, f), text, vector2f.x, vector2f.y, value3 * f, value4);
                } else {
                    this.blitGlyphs(graphics, pageProvider.getPage(value3 * f, f), text, vector2f.x, vector2f.y, value3 * f, value4);
                }

                matrix3x2fstack.popMatrix();
            } else {
                if (flag) {
                    this.blitTextMask(graphics, pageProvider.getPage(value3, 1.0F), text, value, value2, value3, value4);
                } else {
                    this.blitGlyphs(graphics, pageProvider.getPage(value3, 1.0F), text, value, value2, value3, value4);
                }
            }
        }
    }

    public void blitTextMask(GuiGraphicsExtractor graphics, GlyphPage glyphPage, String text, float value, float value2, float value3, int value4) {
        TextMaskCache.Mask textmaskcache$mask = TextMaskCache.get(glyphPage, text);
        if (textmaskcache$mask != null) {
            int i = getGuiScale();
            graphics.pose().pushMatrix();
            graphics.pose().scale(1.0F / i, 1.0F / i);
            int j = Math.round(value * i);
            int k = Math.round((value2 + this.pointSize * value3 * 0.5F) * i + glyphPage.capHeight / 2.0F);
            graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                textmaskcache$mask.id(),
                j + textmaskcache$mask.ox(),
                k + textmaskcache$mask.oy(),
                0.0F,
                0.0F,
                textmaskcache$mask.w(),
                textmaskcache$mask.h(),
                textmaskcache$mask.w(),
                textmaskcache$mask.h(),
                textmaskcache$mask.w(),
                textmaskcache$mask.h(),
                value4
            );
            graphics.pose().popMatrix();
        }
    }

    public void blitGlyphs(GuiGraphicsExtractor graphics, GlyphPage glyphPage, String text, float value, float value2, float value3, int value4) {
        int i = getGuiScale();
        int j = 0;

        while (j < text.length()) {
            int k = text.codePointAt(j);
            glyphPage.getGlyph(k);
            j += Character.charCount(k);
        }

        glyphPage.uploadDirty();
        graphics.pose().pushMatrix();
        graphics.pose().scale(1.0F / i, 1.0F / i);
        float f = Math.round(value * i);
        int j1 = Math.round((value2 + this.pointSize * value3 * 0.5F) * i + glyphPage.capHeight / 2.0F);
        int l = 0;

        while (l < text.length()) {
            int i1 = text.codePointAt(l);
            GlyphPage.Glyph glyphpage$glyph = glyphPage.getGlyph(i1);
            if (glyphpage$glyph.texture() != null) {
                graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    glyphpage$glyph.texture(),
                    Math.round(f) + glyphpage$glyph.offX(),
                    j1 + glyphpage$glyph.offY(),
                    glyphpage$glyph.u(),
                    glyphpage$glyph.v(),
                    glyphpage$glyph.w(),
                    glyphpage$glyph.h(),
                    glyphpage$glyph.w(),
                    glyphpage$glyph.h(),
                    512,
                    512,
                    value4
                );
            }

            f += glyphpage$glyph.advance();
            l += Character.charCount(i1);
        }

        graphics.pose().popMatrix();
    }

    public void drawCenteredX(GuiGraphicsExtractor graphics, String text, float value, float value2, int value3) {
        this.draw(graphics, text, value - this.getWidth(text) / 2.0F, value2, value3);
    }

    public void drawRightAligned(GuiGraphicsExtractor graphics, String text, float value, float value2, int value3) {
        this.draw(graphics, text, value - this.getWidth(text), value2, value3);
    }

    public void drawIconCentered(GuiGraphicsExtractor graphics, String text, float value, float value2, float value3, int value4) {
        if (text != null && !text.isEmpty()) {
            int i = getGuiScale();
            GlyphPage glyphpage = this.getPage(value3);
            float f;
            if (this.isIconFont()) {
                GlyphPage.Glyph glyphpage$glyph = glyphpage.getGlyph(text.codePointAt(0));
                float f1 = glyphpage$glyph.offY() + glyphpage$glyph.h() / 2.0F;
                f = value2 - (f1 + glyphpage.capHeight / 2.0F) / i - this.pointSize * value3 * 0.5F;
            } else {
                f = value2 - this.pointSize * value3 * 0.5F;
            }

            this.drawScaled(graphics, text, value - glyphpage.measureWidth(text) / i / 2.0F, f, value3, value4);
        }
    }

    public void drawWithGlowBlob(GuiGraphicsExtractor graphics, String text, float value, float value2, int value3, int value4) {
        if (text != null && !text.isEmpty()) {
            float f = this.getWidth(text);
            float f1 = this.getLineHeight();
            Render2D.drawBlob(graphics, value - f1 * 0.5F, value2 - f1 * 0.2F, f + f1, f1 * 1.4F, value4);
            this.draw(graphics, text, value, value2, value3);
        }
    }

    public String ellipsize(String text, float value) {
        if (this.getWidth(text) <= value) {
            return text;
        }

        String s = text;

        while (!s.isEmpty() && this.getWidth(s + "…") > value) {
            s = s.substring(0, s.length() - 1);
        }

        return s + "…";
    }

    @FunctionalInterface
    private interface PageProvider {
        GlyphPage getPage(float first, float second);
    }
}
