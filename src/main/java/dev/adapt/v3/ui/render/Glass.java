package dev.adapt.v3.ui.render;

import dev.adapt.v3.ui.screen.BaseScreen;
import dev.adapt.v3.ui.screen.HudEditorScreen;
import dev.adapt.v3.ui.theme.Theme;
import dev.adapt.v3.util.render.ColorUtility;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;

public final class Glass {
    public static final int PANEL_FILL = -1509159920;
    public static final int PANEL_BORDER = 452984831;
    public static final int STRONG_BORDER = 536870911;
    public static final int HUD_FILL = -2146694128;
    public static final int ACCENT_GLASS_FILL = 1493961744;
    public static final int ACCENT_SHEEN_TOP = 285212671;
    public static final int ACCENT_GLASS_BORDER = 352321535;

    public static void drawThemedCard(GuiGraphicsExtractor graphics, float x, float y, float width, float height, float first, float second) {
        drawThemedCard(graphics, x, y, width, height, first, second, Theme.getCardBg());
    }

    public static void drawThemedCard(GuiGraphicsExtractor graphics, float x, float y, float width, float height, float first, float second, int color) {
        if (!(second <= 0.01F)) {
            float f = Render2D.snapToPixel(x);
            float f1 = Render2D.snapToPixel(y);
            float f2 = Render2D.snapToPixel(x + width) - f;
            float f3 = Render2D.snapToPixel(y + height) - f1;
            float f4 = Render2D.snapToPixel(first);
            Render2D.drawDropShadow(graphics, f, f1 + 1.5F, f2, f3, f4, 10.0F, ColorUtility.scaleAlpha(Theme.isDark() ? 1610612736 : 672800563, second));
            graphics.nextStratum();
            if (second > 0.55F && !isBackdropCoveredByScreen()) {
                Render2D.drawBackdropBlur(graphics, f, f1, f2, f3, f4);
            }

            graphics.nextStratum();
            Render2D.fillRoundedRect(graphics, f, f1, f2, f3, f4, ColorUtility.scaleAlpha(color, second));
            graphics.nextStratum();
            Render2D.drawRoundedBorderTextured(graphics, f, f1, f2, f3, f4, 1.0F, ColorUtility.scaleAlpha(Theme.getCardBorder(), second));
            graphics.nextStratum();
        }
    }

    public static void drawPanel(GuiGraphicsExtractor graphics, float x, float y, float width, float height, float radius, float first, float second) {
        drawPanel(graphics, x, y, width, height, radius, first, second, 1.0F);
    }

    public static void drawPanel(GuiGraphicsExtractor graphics, float x, float y, float width, float height, float radius, float value, float value2, float value3) {
        drawPanel(graphics, x, y, width, height, radius, value, value2, value3, -1509159920);
    }

    public static void drawPanel(
        GuiGraphicsExtractor graphics, float x, float y, float width, float height, float radius, float value, float value2, float value3, int color
    ) {
        drawBlurredSurface(graphics, x, y, width, height, radius, value, value2, value3, color, true);
    }

    public static void drawBorderlessPanel(
        GuiGraphicsExtractor graphics, float x, float y, float width, float height, float radius, float value, float value2, float value3, int color
    ) {
        drawBlurredSurface(graphics, x, y, width, height, radius, value, value2, value3, color, false);
    }

    public static void drawHudSurface(
        GuiGraphicsExtractor graphics, float x, float y, float width, float height, float value, float value2, float value3, float value4, int startColor, boolean flag
    ) {
        drawSurface(graphics, x, y, width, height, value, value2, value3, value4, startColor, false, flag);
    }

    public static void drawBlurredSurface(
        GuiGraphicsExtractor graphics, float x, float y, float width, float height, float value, float value2, float value3, float value4, int startColor, boolean flag
    ) {
        drawSurface(graphics, x, y, width, height, value, value2, value3, value4, startColor, flag, true);
    }

    public static void drawSurface(
        GuiGraphicsExtractor graphics,
        float x,
        float y,
        float width,
        float height,
        float value,
        float value2,
        float value3,
        float value4,
        int value5,
        boolean flag,
        boolean flag2
    ) {
        if (!(value2 <= 0.01F)) {
            float f = Render2D.snapToPixel(x);
            float f1 = Render2D.snapToPixel(y);
            float f2 = Render2D.snapToPixel(x + width) - f;
            float f3 = Render2D.snapToPixel(y + height) - f1;
            float f4 = Render2D.snapToPixel(value);
            if (value4 > 0.01F) {
                Render2D.drawDropShadow(graphics, f, f1 + 4.0F * value3, f2, f3, f4, 22.0F * value3, ColorUtility.scaleAlpha(1879048192, value2 * value4));
                Render2D.drawDropShadow(graphics, f, f1, f2, f3, f4, 16.0F * value3, ColorUtility.scaleAlpha(939524096, value2 * value4));
            }

            graphics.nextStratum();
            if (flag2 && value2 > 0.02F) {
                Render2D.drawBackdropBlur(graphics, f, f1, f2, f3, f4, 0, value2 * value2);
            }

            graphics.nextStratum();
            Render2D.fillBatchedRoundedRect(graphics, f, f1, f2, f3, f4, ColorUtility.scaleAlpha(value5, value2));
            graphics.nextStratum();
            if (flag) {
                Render2D.drawBatchedHairlineBorder(graphics, f, f1, f2, f3, f4, ColorUtility.scaleAlpha(452984831, value2));
            }

            graphics.nextStratum();
        }
    }

    public static void drawSheenPanel(GuiGraphicsExtractor graphics, float x, float y, float width, float height, float radius, float first, float second) {
        drawSheenPanel(graphics, x, y, width, height, radius, first, second, 1.0F);
    }

    public static void drawSheenPanel(GuiGraphicsExtractor graphics, float x, float y, float width, float height, float radius, float value, float value2, float value3) {
        if (!(value <= 0.01F)) {
            drawPanel(graphics, x, y, width, height, radius, value, value2, value3, 1493961744);
            float f = Render2D.snapToPixel(x);
            float f1 = Render2D.snapToPixel(y);
            float f2 = Render2D.snapToPixel(x + width) - f;
            float f3 = Render2D.snapToPixel(y + height) - f1;
            float f4 = Render2D.snapToPixel(radius);
            Render2D.fillSampledRoundedRect(graphics, f, f1, f2, f3, f4, value, (first, second) -> ColorUtility.scaleAlpha(285212671, 1.0F - second));
            graphics.nextStratum();
            Render2D.drawBatchedHairlineBorder(graphics, f, f1, f2, f3, f4, ColorUtility.scaleAlpha(352321535, value));
            graphics.nextStratum();
        }
    }

    public static boolean isBackdropCoveredByScreen() {
        Screen screen = Minecraft.getInstance().gui.screen();
        return screen instanceof BaseScreen && !(screen instanceof HudEditorScreen);
    }

    public static void drawToastSurface(GuiGraphicsExtractor graphics, float x, float y, float width, float height, float first, float second) {
        drawBorderlessPanel(graphics, x, y, width, height, first, second, 0.55F, 1.0F, -2146694128);
    }
}
