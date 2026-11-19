package dev.adapt.v3.feature.module.impl.hud;

import java.util.List;
import dev.adapt.v3.feature.property.impl.BooleanProperty;
import dev.adapt.v3.hud.HudModule;
import dev.adapt.v3.ui.render.Fonts;
import dev.adapt.v3.ui.render.Glass;
import dev.adapt.v3.ui.render.Render2D;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class WatermarkHud extends HudModule {
    public final BooleanProperty showFps = new BooleanProperty("Show FPS", true);
    public final BooleanProperty showTime = new BooleanProperty("Show Time", true);
    public static final float HEIGHT = 18.0F;
    public static final float RADIUS = 9.0F;
    public static final float PAD_X = 9.0F;
    public static final float DIVIDER_GAP = 7.0F;
    public static final int BRAND_COLOR = -218103809;
    public static final int VALUE_COLOR = -637534209;
    public static final int UNIT_COLOR = -2130706433;
    public static final int DIVIDER_COLOR = 536870911;
    public static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    public static final String BRAND_TEXT = "Adapt";

    public WatermarkHud() {
        super("Watermark", "AdaptV3 capsule with FPS and the time.", 0.006F, 0.01F);
        this.addProperties(this.showFps, this.showTime);
        this.setEnabled(true);
    }

    @Override
    public float getBaseScale() {
        return 0.95F;
    }

    @Override
    public float getWidth() {
        float f = 9.0F + Fonts.MEDIUM.getWidth("Adapt");

        for (String[] astring : this.buildSegments()) {
            f += 14.5F + measureSegment(astring);
        }

        return f + 9.0F + 1.0F;
    }

    @Override
    public float getHeight() {
        return 18.0F;
    }

    public String[][] buildSegments() {
        List<String[]> arraylist = new ArrayList<>(2);
        if (this.showFps.getValue()) {
            arraylist.add(new String[]{String.valueOf(mc.getFps()), "fps"});
        }

        if (this.showTime.getValue()) {
            arraylist.add(new String[]{LocalTime.now().format(TIME_FORMATTER), ""});
        }

        return arraylist.toArray(new String[0][]);
    }

    public static float measureSegment(String[] strings) {
        return Fonts.SMALL.getWidth(strings[0]) + (strings[1].isEmpty() ? 0.0F : 2.0F + Fonts.SMALL.getWidth(strings[1]));
    }

    @Override
    public void render(GuiGraphicsExtractor graphics, float partialTick, boolean editing) {
        float f = this.getWidth();
        float f1 = 9.0F;
        Glass.drawBorderlessPanel(graphics, 0.0F, 0.0F, f, 18.0F, 9.0F, 1.0F, 0.55F, 1.0F, -2146694128);
        float f2 = 9.0F;
        Fonts.MEDIUM.drawVCentered(graphics, "Adapt", f2, 9.0F, -218103809);
        f2 += Fonts.MEDIUM.getWidth("Adapt");

        for (String[] astring : this.buildSegments()) {
            f2 += 7.0F;
            Render2D.vlineHairline(graphics, f2, 5.0F, 8.0F, 536870911);
            f2 += 7.5F;
            Fonts.SMALL.drawVCentered(graphics, astring[0], f2, 9.0F, -637534209);
            if (!astring[1].isEmpty()) {
                Fonts.SMALL.drawVCentered(graphics, astring[1], f2 + Fonts.SMALL.getWidth(astring[0]) + 2.0F, 9.0F, -2130706433);
            }

            f2 += measureSegment(astring);
        }
    }
}
