package dev.adapt.v3.ui;

import dev.adapt.v3.ui.render.Fonts;
import dev.adapt.v3.ui.render.Glass;
import dev.adapt.v3.ui.render.Render2D;
import dev.adapt.v3.ui.theme.Theme;
import dev.adapt.v3.util.render.ColorUtility;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class Widgets {
    public static final float SWITCH_WIDTH = 18.0F;
    public static final float SWITCH_HEIGHT = 10.0F;

    public static void drawSwitch(GuiGraphicsExtractor graphics, float x, float y, float width, float height) {
        drawSwitch(graphics, x, y, width, 0.0F, 0.0F, height);
    }

    public static void drawSwitch(GuiGraphicsExtractor graphics, float x, float y, float width, float height, float first, float second) {
        float f = Math.max(0.0F, Math.min(1.0F, width));
        float f1 = Math.max(0.0F, Math.min(1.0F, height * 0.55F + first * 0.6F));
        int i = ColorUtility.lerpColor(Theme.getTrackColor(), Theme.getAccentColor(), f);
        Render2D.fillRoundedRect(graphics, x, y, 18.0F, 10.0F, 5.0F, ColorUtility.scaleAlpha(i, second));
        if (f1 > 0.01F) {
            Render2D.fillRoundedRect(graphics, x, y, 18.0F, 10.0F, 5.0F, ColorUtility.scaleAlpha(1090519039, second * f1));
        }

        if (f > 0.01F) {
            Render2D.drawDropShadow(
                graphics,
                x,
                y,
                18.0F,
                10.0F,
                5.0F,
                4.0F,
                ColorUtility.scaleAlpha(Theme.getAccentWithAlpha((int)((90.0F + 50.0F * height) * f * Theme.getBloom())), second)
            );
        }

        float f2 = 7.0F;
        float f3 = x + 1.5F + 8.0F * width;
        float f4 = f3 + 3.5F;
        float f5 = y + 5.0F;
        float f6 = 3.5F;
        Render2D.drawCircleShadow(graphics, f4, f5, 3.5F, 1.5F, ColorUtility.scaleAlpha(1073741824, second));
        Render2D.fillCircle(graphics, f4, f5, 3.5F, ColorUtility.scaleAlpha(-1, second));
    }

    public static void drawSlider(GuiGraphicsExtractor graphics, float x, float y, float width, float height, float first, float second) {
        drawSlider(graphics, x, y, width, height, first, 0.0F, second);
    }

    public static void drawSlider(GuiGraphicsExtractor graphics, float x, float y, float width, float height, float value, float value2, float value3) {
        float f = Math.max(0.0F, Math.min(1.0F, height));
        Render2D.fillRoundedRect(
            graphics, x, y - 1.0F, width, 2.0F, 1.0F, ColorUtility.scaleAlpha(ColorUtility.lerpColor(Theme.getTrackColor(), 1509949439, value2 * 0.5F), value3)
        );
        if (Theme.hasGradient()) {
            float f1 = Math.max(2.0F, width * f);
            Render2D.fillHorizontalGradient(
                graphics,
                x + 1.0F,
                y - 1.0F,
                Math.max(0.0F, f1 - 2.0F),
                2.0F,
                ColorUtility.scaleAlpha(ColorUtility.lighten(Theme.sampleGradient(0.0F), 0.1F * value2), value3),
                ColorUtility.scaleAlpha(ColorUtility.lighten(Theme.sampleGradient(f), 0.1F * value2), value3)
            );
            Render2D.fillCircle(graphics, x + 1.0F, y, 1.0F, ColorUtility.scaleAlpha(Theme.sampleGradient(0.0F), value3));
        } else {
            Render2D.fillRoundedRect(
                graphics,
                x,
                y - 1.0F,
                Math.max(2.0F, width * f),
                2.0F,
                1.0F,
                ColorUtility.scaleAlpha(ColorUtility.lighten(Theme.getAccentColor(), 0.1F * value2), value3)
            );
        }

        float f4 = x + width * f;
        float f2 = 3.5F + value * 1.0F + value2 * 0.5F;
        float f3 = Math.max(value, value2 * 0.5F);
        if (f3 > 0.01F) {
            Render2D.fillCircle(graphics, f4, y, f2 + 4.0F * f3, ColorUtility.scaleAlpha(Theme.getAccentWithAlpha((int)(50.0F * f3)), value3));
        }

        Render2D.drawCircleShadow(graphics, f4, y, f2, 2.0F, ColorUtility.scaleAlpha(1073741824, value3));
        Render2D.fillCircle(graphics, f4, y, f2, ColorUtility.scaleAlpha(-1, value3));
    }

    public static void drawHueSlider(GuiGraphicsExtractor graphics, float x, float y, float width, float height, float value) {
        drawHueSlider(graphics, x, y, width, height, 0.0F, value);
    }

    public static void drawHueSlider(GuiGraphicsExtractor graphics, float x, float y, float width, float height, float first, float second) {
        byte b0 = 12;

        for (int i = 0; i < 12; i++) {
            Render2D.fillHorizontalGradient(
                graphics,
                x + width * i / 12.0F,
                y - 1.5F,
                width / 12.0F + 0.3F,
                3.0F,
                ColorUtility.scaleAlpha(ColorUtility.fromHsb(i / 12.0F, 0.6F, 1.0F), second),
                ColorUtility.scaleAlpha(ColorUtility.fromHsb((i + 1) / 12.0F, 0.6F, 1.0F), second)
            );
        }

        float f1 = x + width * height;
        float f = 4.0F + 0.8F * first;
        Render2D.drawCircleShadow(graphics, f1, y, f, 2.0F, ColorUtility.scaleAlpha(1073741824, second));
        Render2D.fillCircle(graphics, f1, y, f, ColorUtility.scaleAlpha(-1, second));
        Render2D.fillCircle(graphics, f1, y, f - 1.5F, ColorUtility.scaleAlpha(ColorUtility.fromHsb(height, 0.6F, 1.0F), second));
    }

    public static float measureBadgeWidth(String text) {
        return Fonts.SMALL.getWidth(text) + 8.0F;
    }

    public static void drawBadge(GuiGraphicsExtractor graphics, String text, float value, float value2, int value3, int value4, float value5) {
        float f = measureBadgeWidth(text);
        Render2D.fillRoundedRect(graphics, value, value2, f, 10.0F, 3.0F, ColorUtility.scaleAlpha(value3, value5));
        Fonts.SMALL.draw(graphics, text, value + 4.0F, value2 + 5.0F - Fonts.SMALL.getHalfPointSize(), ColorUtility.scaleAlpha(value4, value5));
    }

    public static void drawCard(GuiGraphicsExtractor graphics, float x, float y, float width, float height, float first, float second) {
        Glass.drawThemedCard(graphics, x, y, width, height, first, second);
    }
}
