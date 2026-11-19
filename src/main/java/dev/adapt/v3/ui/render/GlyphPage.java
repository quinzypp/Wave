package dev.adapt.v3.ui.render;

import com.mojang.blaze3d.platform.NativeImage;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.font.LineMetrics;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

public final class GlyphPage {
    public static final int ATLAS_SIZE = 512;
    public static final int PADDING = 2;
    public final Font font;
    public final String pageKey;
    public final float blurRadius;
    public final FontRenderContext renderContext = new FontRenderContext(null, true, true);
    public final Map<Integer, GlyphPage.Glyph> glyphMap = new HashMap<>();
    public final List<GlyphPage.Atlas> atlases = new ArrayList<>();
    public final float ascent;
    public final float descent;
    public final float capHeight;
    public final GlyphPage.Glyph[] asciiGlyphs = new GlyphPage.Glyph[128];

    public GlyphPage(Font font, float value, String text) {
        this(font, value, text, 0.0F);
    }

    public GlyphPage(Font font, float first, String text, float second) {
        this.font = font.deriveFont(first);
        this.pageKey = text;
        this.blurRadius = second;
        LineMetrics linemetrics = this.font.getLineMetrics("Hg", this.renderContext);
        this.ascent = linemetrics.getAscent();
        this.descent = linemetrics.getDescent();
        Rectangle2D rectangle2d = this.font.createGlyphVector(this.renderContext, "H").getVisualBounds();
        this.capHeight = rectangle2d.getHeight() > 0.0 ? (float)rectangle2d.getHeight() : this.ascent * 0.7F;
    }

    public GlyphPage.Glyph getGlyph(int value) {
        if (value >= 0 && value < 128) {
            GlyphPage.Glyph glyphpage$glyph1 = this.asciiGlyphs[value];
            if (glyphpage$glyph1 == null) {
                glyphpage$glyph1 = this.rasterizeGlyph(value);
                this.asciiGlyphs[value] = glyphpage$glyph1;
                this.glyphMap.put(value, glyphpage$glyph1);
            }

            return glyphpage$glyph1;
        } else {
            GlyphPage.Glyph glyphpage$glyph = this.glyphMap.get(value);
            if (glyphpage$glyph == null) {
                glyphpage$glyph = this.rasterizeGlyph(value);
                this.glyphMap.put(value, glyphpage$glyph);
            }

            return glyphpage$glyph;
        }
    }

    public GlyphPage.Glyph rasterizeGlyph(int value) {
        String s = new String(Character.toChars(value));
        GlyphVector glyphvector = this.font.createGlyphVector(this.renderContext, s);
        float f = glyphvector.getGlyphMetrics(0).getAdvanceX();
        Rectangle2D rectangle2d = glyphvector.getVisualBounds();
        if (rectangle2d.isEmpty()) {
            return new GlyphPage.Glyph(null, 0, 0, 0, 0, f, 0, 0);
        }

        int i = (int)Math.floor(rectangle2d.getX());
        int j = (int)Math.floor(rectangle2d.getY());
        int k = 2 + (this.blurRadius > 0.0F ? (int)Math.ceil(this.blurRadius * 1.8F) : 0);
        int l = (int)Math.ceil(rectangle2d.getMaxX()) - i + k * 2;
        int i1 = (int)Math.ceil(rectangle2d.getMaxY()) - j + k * 2;
        BufferedImage bufferedimage = new BufferedImage(l, i1, 2);
        Graphics2D graphics2d = bufferedimage.createGraphics();
        graphics2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        graphics2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        graphics2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics2d.setColor(Color.WHITE);
        graphics2d.setFont(this.font);
        graphics2d.drawGlyphVector(glyphvector, k - i, k - j);
        graphics2d.dispose();
        float[] afloat = new float[l * i1];

        for (int j1 = 0; j1 < i1; j1++) {
            for (int k1 = 0; k1 < l; k1++) {
                afloat[j1 * l + k1] = (bufferedimage.getRGB(k1, j1) >>> 24) / 255.0F;
            }
        }

        if (this.blurRadius > 0.0F) {
            gaussianBlur(afloat, l, i1, this.blurRadius * 0.57735F);
        }

        GlyphPage.Atlas glyphpage$atlas = this.atlases.isEmpty() ? null : this.atlases.get(this.atlases.size() - 1);
        if (glyphpage$atlas != null && glyphpage$atlas.cursorX + l + 1 > 512) {
            glyphpage$atlas.cursorX = 1;
            glyphpage$atlas.cursorY = glyphpage$atlas.cursorY + glyphpage$atlas.rowHeight + 1;
            glyphpage$atlas.rowHeight = 0;
        }

        if (glyphpage$atlas == null || glyphpage$atlas.cursorY + i1 + 1 > 512) {
            glyphpage$atlas = new GlyphPage.Atlas(this.pageKey + "_" + this.atlases.size());
            this.atlases.add(glyphpage$atlas);
        }

        for (int j2 = 0; j2 < i1; j2++) {
            for (int l1 = 0; l1 < l; l1++) {
                float f1 = afloat[j2 * l + l1];
                int i2 = (int)Math.round(Math.min(1.0, this.blurRadius > 0.0F ? f1 : Math.pow(f1, 0.88)) * 255.0);
                glyphpage$atlas.image.setPixel(glyphpage$atlas.cursorX + l1, glyphpage$atlas.cursorY + j2, i2 << 24 | 16777215);
            }
        }

        GlyphPage.Glyph glyphpage$glyph = new GlyphPage.Glyph(glyphpage$atlas.id, glyphpage$atlas.cursorX, glyphpage$atlas.cursorY, l, i1, f, i - k, j - k);
        glyphpage$atlas.cursorX += l + 1;
        glyphpage$atlas.rowHeight = Math.max(glyphpage$atlas.rowHeight, i1);
        glyphpage$atlas.dirty = true;
        return glyphpage$glyph;
    }

    public static void gaussianBlur(float[] floats, int value, int value2, float value3) {
        int i = Math.max(1, (int)Math.ceil(value3 * 3.0F));
        float[] afloat = new float[i * 2 + 1];
        float f = 0.0F;

        for (int j = -i; j <= i; j++) {
            afloat[j + i] = (float)Math.exp(-(j * j) / (2.0 * value3 * value3));
            f += afloat[j + i];
        }

        for (int k1 = 0; k1 < afloat.length; k1++) {
            afloat[k1] /= f;
        }

        float[] afloat1 = new float[floats.length];

        for (int k = 0; k < value2; k++) {
            for (int l = 0; l < value; l++) {
                float f1 = 0.0F;

                for (int i1 = -i; i1 <= i; i1++) {
                    int j1 = l + i1;
                    if (j1 >= 0 && j1 < value) {
                        f1 += floats[k * value + j1] * afloat[i1 + i];
                    }
                }

                afloat1[k * value + l] = f1;
            }
        }

        for (int l1 = 0; l1 < value2; l1++) {
            for (int i2 = 0; i2 < value; i2++) {
                float f2 = 0.0F;

                for (int j2 = -i; j2 <= i; j2++) {
                    int k2 = l1 + j2;
                    if (k2 >= 0 && k2 < value2) {
                        f2 += afloat1[k2 * value + i2] * afloat[j2 + i];
                    }
                }

                floats[l1 * value + i2] = f2;
            }
        }
    }

    public void uploadDirty() {
        for (GlyphPage.Atlas glyphpage$atlas : this.atlases) {
            if (glyphpage$atlas.dirty) {
                glyphpage$atlas.texture.upload();
                glyphpage$atlas.dirty = false;
            }
        }
    }

    public NativeImage getAtlasImage(Identifier id) {
        for (GlyphPage.Atlas glyphpage$atlas : this.atlases) {
            if (glyphpage$atlas.id.equals(id)) {
                return glyphpage$atlas.image;
            }
        }

        return null;
    }

    public float measureWidth(String text) {
        float f = 0.0F;
        int i = 0;

        while (i < text.length()) {
            int j = text.codePointAt(i);
            f += this.getGlyph(j).advance();
            i += Character.charCount(j);
        }

        return f;
    }

    private static final class Atlas {
        public final NativeImage image = new NativeImage(512, 512, true);
        public final Identifier id;
        public final SmoothTexture texture;
        public int cursorX = 1;
        public int cursorY = 1;
        public int rowHeight;
        public boolean dirty;

        public Atlas(String text) {
            this.id = Identifier.fromNamespaceAndPath("adaptv3", "glyphs/" + text);
            this.texture = new SmoothTexture("adaptv3:" + text, this.image);
            Minecraft.getInstance().getTextureManager().register(this.id, this.texture);
        }
    }

    record Glyph(Identifier texture, int u, int v, int w, int h, float advance, int offX, int offY) {
    }
}
