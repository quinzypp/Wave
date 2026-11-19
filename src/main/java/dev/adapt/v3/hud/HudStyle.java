package dev.adapt.v3.hud;

import dev.adapt.v3.ui.render.Glass;
import dev.adapt.v3.ui.theme.Theme;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class HudStyle {
    public static final int BG_COLOR = -1728053248;
    public static final int TEXT_COLOR = -1;
    public static final int MUTED_TEXT_COLOR = -1493172225;
    public static final int SUBTLE_TEXT_COLOR = 1946157055;
    public static final int SHADOW_COLOR = 1426063360;

    public static void drawCard(GuiGraphicsExtractor graphics, float x, float y, float width, float height) {
        Glass.drawToastSurface(graphics, x, y, width, height, Math.min(height / 2.0F, Theme.getCornerRadius()), 1.0F);
    }
}
