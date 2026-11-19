package dev.adapt.v3.notification;

import dev.adapt.v3.Adapt;
import dev.adapt.v3.ui.render.Fonts;
import dev.adapt.v3.ui.render.Glass;
import dev.adapt.v3.ui.render.Render2D;
import dev.adapt.v3.ui.screen.DashboardScreen;
import dev.adapt.v3.ui.theme.Theme;
import dev.adapt.v3.util.animation.Spring;
import dev.adapt.v3.util.render.ColorUtility;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class NotificationRenderer {
    public static final float WIDTH = 160.0F;
    public static final float HEIGHT = 38.0F;
    public static final float STRIDE_GAP = 6.0F;
    public static final float TEXT_X = 36.0F;
    public static final float TITLE_Y = 12.0F;
    public static final float MESSAGE_Y = 22.5F;
    public static final float BAR_Y = 29.5F;
    public static final float ICON_RADIUS = 10.0F;
    public static final float SCREEN_MARGIN = 6.0F;
    public static final float SCALE = 0.9F;
    public static final float DASHBOARD_OFFSET_Y = 56.0F;
    public static final Spring dashboardOffsetSpring = new Spring(0.0F, 300.0F, 34.6F);
    public static final float BAR_HEIGHT = 2.0F;

    public static void render(GuiGraphicsExtractor graphics) {
        List list = Adapt.getInstance().getNotificationManager().getNotifications();
        boolean flag = Minecraft.getInstance().gui.screen() instanceof DashboardScreen;
        float f = dashboardOffsetSpring.target(flag ? 56.0F : 0.0F).update();
        if (!list.isEmpty()) {
            graphics.pose().pushMatrix();
            graphics.pose().translate(graphics.guiWidth() - 6.0F, graphics.guiHeight() - 6.0F - f);
            graphics.pose().scale(0.9F, 0.9F);
            graphics.pose().translate(-160.0F, -38.0F);
            Fonts.setNativeTransformScale(true);

            try {
                renderList(graphics, list);
            } finally {
                Fonts.setNativeTransformScale(false);
                graphics.pose().popMatrix();
            }
        }
    }

    public static void renderList(GuiGraphicsExtractor graphics, List list) {
        int i = 0;

        for (int j = list.size() - 1; j >= 0; j--) {
            Notification notification = (Notification)list.get(j);
            boolean flag = notification.isExpired();
            notification.slideSpring.target(flag ? 0.0F : 1.0F);
            float f = notification.slideSpring.update();
            if (flag && f < 0.02F) {
                Adapt.getInstance().getNotificationManager().remove(notification);
            } else {
                float f1 = -i * 44.0F;
                if (notification.ySpring.get() == -1.0F && notification.ySpring.getTarget() == -1.0F) {
                    notification.ySpring.snap(f1);
                }

                float f2 = notification.ySpring.target(f1).update();
                float f3 = (1.0F - f) * 184.0F;
                renderToast(graphics, notification, f3, f2, Math.max(0.0F, Math.min(1.0F, f)));
                i++;
            }
        }
    }

    public static void renderToast(GuiGraphicsExtractor graphics, Notification notification, float value, float value2, float value3) {
        float f = Theme.getCornerRadius();
        Glass.drawToastSurface(graphics, value, value2, 160.0F, 38.0F, f, value3);
        int i = notification.getType().getColor();
        float f1 = value + 19.0F;
        Render2D.fillCircleAligned(graphics, f1, value2 + 19.0F, 10.0F, ColorUtility.scaleAlpha(ColorUtility.withAlpha(i, 40), value3));
        Fonts.ICONS.drawIconCentered(graphics, notification.getType().getIcon(), f1, value2 + 19.0F, 1.0F, ColorUtility.scaleAlpha(i, value3));
        float f2 = 114.0F;
        Fonts.SEMIBOLD
            .drawCached(
                graphics,
                Fonts.SEMIBOLD.ellipsize(notification.getTitle(), 114.0F),
                value + 36.0F,
                value2 + 12.0F - Fonts.SEMIBOLD.getHalfPointSize(),
                ColorUtility.scaleAlpha(-1, value3)
            );
        Fonts.SMALL
            .drawCached(
                graphics,
                Fonts.SMALL.ellipsize(notification.getMessage(), 114.0F),
                value + 36.0F,
                value2 + 22.5F - Fonts.SMALL.getHalfPointSize(),
                ColorUtility.scaleAlpha(-1493172225, value3)
            );
        renderProgressBar(graphics, value + 36.0F, value2 + 29.5F, 114.0F, 1.0F - notification.getProgress(), i, value3);
    }

    public static void renderProgressBar(GuiGraphicsExtractor graphics, float x, float y, float width, float height, int startColor, float value) {
        float f = 1.0F;
        Render2D.fillSubpixelRoundedRect(graphics, x, y, width, 2.0F, 1.0F, ColorUtility.scaleAlpha(452984831, value));
        if (!(height <= 0.001F)) {
            float f1 = Math.max(2.0F, width * height);
            float f2 = value * Math.min(1.0F, width * height / 2.0F);
            Render2D.drawDropShadow(graphics, x, y, f1, 2.0F, 1.0F, 3.0F, ColorUtility.scaleAlpha(ColorUtility.withAlpha(startColor, 70), f2));
            Render2D.fillSubpixelRoundedRect(graphics, x, y, f1, 2.0F, 1.0F, ColorUtility.scaleAlpha(ColorUtility.withAlpha(startColor, 220), f2));
        }
    }
}
