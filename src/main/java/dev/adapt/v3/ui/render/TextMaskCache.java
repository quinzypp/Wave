package dev.adapt.v3.ui.render;

import com.mojang.blaze3d.platform.NativeImage;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

public final class TextMaskCache {
    public static final int MAX_ENTRIES = 256;
    public static int nextMaskId;
    public static final Map<TextMaskCache.Key, TextMaskCache.Mask> CACHE = new LinkedHashMap<TextMaskCache.Key, TextMaskCache.Mask>(64, 0.75F, true) {
        @Override
        public boolean removeEldestEntry(Entry<TextMaskCache.Key, TextMaskCache.Mask> entry) {
            if (this.size() <= 256) {
                return false;
            }

            TextMaskCache.Mask textmaskcache$mask = (TextMaskCache.Mask)entry.getValue();
            if (textmaskcache$mask != null) {
                Minecraft.getInstance().getTextureManager().release(textmaskcache$mask.id());
            }

            return true;
        }
    };
    public static final TextMaskCache.Mask EMPTY = new TextMaskCache.Mask(null, 0, 0, 0, 0);

    public static TextMaskCache.Mask get(GlyphPage glyphPage, String text) {
        TextMaskCache.Key textmaskcache$key = new TextMaskCache.Key(glyphPage, text);
        TextMaskCache.Mask textmaskcache$mask = CACHE.get(textmaskcache$key);
        if (textmaskcache$mask == null) {
            textmaskcache$mask = rasterize(glyphPage, text);
            CACHE.put(textmaskcache$key, textmaskcache$mask);
        }

        return textmaskcache$mask == EMPTY ? null : textmaskcache$mask;
    }

    public static TextMaskCache.Mask rasterize(GlyphPage glyphPage, String text) {
        int i = Integer.MAX_VALUE;
        int j = Integer.MAX_VALUE;
        int k = Integer.MIN_VALUE;
        int l = Integer.MIN_VALUE;
        float f = 0.0F;
        int i1 = 0;

        while (i1 < text.length()) {
            int j1 = text.codePointAt(i1);
            GlyphPage.Glyph glyphpage$glyph = glyphPage.getGlyph(j1);
            if (glyphpage$glyph.texture() != null) {
                int k1 = Math.round(f) + glyphpage$glyph.offX();
                int l1 = glyphpage$glyph.offY();
                i = Math.min(i, k1);
                j = Math.min(j, l1);
                k = Math.max(k, k1 + glyphpage$glyph.w());
                l = Math.max(l, l1 + glyphpage$glyph.h());
            }

            f += glyphpage$glyph.advance();
            i1 += Character.charCount(j1);
        }

        if (i == Integer.MAX_VALUE) {
            return EMPTY;
        }

        i1 = k - i;
        int i4 = l - j;
        NativeImage nativeimage1 = new NativeImage(i1, i4, true);
        nativeimage1.fillRect(0, 0, i1, i4, 16777215);
        f = 0.0F;
        int j4 = 0;

        while (j4 < text.length()) {
            int k4 = text.codePointAt(j4);
            GlyphPage.Glyph glyphpage$glyph1 = glyphPage.getGlyph(k4);
            NativeImage nativeimage = glyphpage$glyph1.texture() == null ? null : glyphPage.getAtlasImage(glyphpage$glyph1.texture());
            if (nativeimage != null) {
                int i2 = Math.round(f) + glyphpage$glyph1.offX() - i;
                int j2 = glyphpage$glyph1.offY() - j;

                for (int k2 = 0; k2 < glyphpage$glyph1.h(); k2++) {
                    for (int l2 = 0; l2 < glyphpage$glyph1.w(); l2++) {
                        int i3 = nativeimage.getPixel(glyphpage$glyph1.u() + l2, glyphpage$glyph1.v() + k2) >>> 24;
                        if (i3 != 0) {
                            int j3 = i2 + l2;
                            int k3 = j2 + k2;
                            int l3 = nativeimage1.getPixel(j3, k3) >>> 24;
                            if (i3 > l3) {
                                nativeimage1.setPixel(j3, k3, i3 << 24 | 16777215);
                            }
                        }
                    }
                }
            }

            f += glyphpage$glyph1.advance();
            j4 += Character.charCount(k4);
        }

        Identifier identifier = Identifier.fromNamespaceAndPath("adaptv3", "textmask/" + nextMaskId++);
        SmoothTexture smoothtexture = new SmoothTexture(identifier.toString(), nativeimage1);
        Minecraft.getInstance().getTextureManager().register(identifier, smoothtexture);
        smoothtexture.upload();
        return new TextMaskCache.Mask(identifier, i, j, i1, i4);
    }

    private record Key(GlyphPage page, String text) {
    }

    record Mask(Identifier id, int ox, int oy, int w, int h) {
    }
}
