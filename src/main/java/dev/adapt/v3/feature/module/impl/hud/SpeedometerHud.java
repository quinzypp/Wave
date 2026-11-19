package dev.adapt.v3.feature.module.impl.hud;

import dev.adapt.v3.Adapt;
import dev.adapt.v3.feature.property.impl.BooleanProperty;
import dev.adapt.v3.hud.HudModule;
import dev.adapt.v3.hud.HudStyle;
import dev.adapt.v3.ui.render.Fonts;
import dev.adapt.v3.ui.render.Render2D;
import dev.adapt.v3.ui.theme.Theme;
import dev.adapt.v3.util.render.ColorUtility;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class SpeedometerHud extends HudModule {
    public final BooleanProperty sparkline = new BooleanProperty("Sparkline", true);
    public float smoothedMaxSpeed = 7.0F;

    public SpeedometerHud() {
        super("Speedometer", "Blocks per second with a live sparkline.", 0.006F, 0.55F);
        this.addProperties(this.sparkline);
    }

    @Override
    public float getBaseScale() {
        return 0.8F;
    }

    @Override
    public float getWidth() {
        return 104.0F;
    }

    @Override
    public float getHeight() {
        return this.sparkline.getValue() ? 44.0F : 18.0F;
    }

    @Override
    public void render(GuiGraphicsExtractor graphics, float partialTick, boolean editing) {
        float f = this.getWidth();
        float f1 = this.getHeight();
        HudStyle.drawCard(graphics, 0.0F, 0.0F, f, f1);
        float f2 = Adapt.getInstance().getSpeedTracker().getCurrentBps();
        Fonts.ICONS.drawIconCentered(graphics, "\ue9e4", 12.0F, 9.0F, 1.0F, Theme.getAccentColor());
        Fonts.SMALL.draw(graphics, "Speed", 20.0F, 5.5F, -1493172225);
        String s = String.format("%.2f", Math.max(0.0F, f2));
        Fonts.SMALL.drawRightAligned(graphics, "b/s", f - 8.0F, 5.5F, 1946157055);
        Fonts.MEDIUM.drawRightAligned(graphics, s, f - 8.0F - Fonts.SMALL.getWidth("b/s") - 3.0F, 4.5F, -1);
        if (this.sparkline.getValue()) {
            float[] afloat = Adapt.getInstance().getSpeedTracker().getOrderedHistory();
            float[] afloat1 = new float[afloat.length];
            float f3 = 6.0F;

            for (int i = 0; i < afloat.length; i++) {
                float f4 = afloat[Math.max(0, i - 1)];
                float f5 = afloat[i];
                float f6 = afloat[Math.min(afloat.length - 1, i + 1)];
                afloat1[i] = Math.max(0.0F, (f4 + 2.0F * f5 + f6) / 4.0F);
                f3 = Math.max(f3, afloat1[i]);
            }

            float f10 = (float)Math.ceil(f3 * 1.15F * 4.0F) / 4.0F;
            this.smoothedMaxSpeed = this.smoothedMaxSpeed + (f10 - this.smoothedMaxSpeed) * 0.12F;
            if (Math.abs(f10 - this.smoothedMaxSpeed) < 0.02F) {
                this.smoothedMaxSpeed = f10;
            }

            float f11 = Math.round(this.smoothedMaxSpeed * 20.0F) / 20.0F;
            int j = Theme.getAccentColor();
            float f12 = 8.0F;
            float f7 = 19.0F;
            float f8 = f - 16.0F;
            float f9 = f1 - 25.0F;
            Render2D.drawChart(
                graphics,
                "speedometer",
                8.0F,
                19.0F,
                f8,
                f9,
                afloat1,
                f11,
                ColorUtility.withAlpha(j, 235),
                ColorUtility.withAlpha(j, 90),
                ColorUtility.withAlpha(j, 0)
            );
        }
    }
}
