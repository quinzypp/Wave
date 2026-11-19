package dev.adapt.v3.hud;

import dev.adapt.v3.Adapt;
import dev.adapt.v3.feature.module.Module;
import dev.adapt.v3.feature.module.impl.client.NotificationsModule;
import dev.adapt.v3.notification.NotificationRenderer;
import dev.adapt.v3.ui.render.Fonts;
import dev.adapt.v3.ui.render.Render2D;
import dev.adapt.v3.ui.screen.HudEditorScreen;
import dev.adapt.v3.util.Profiler;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class HudManager {
    public List<HudModule> getHudModules() {
        List<HudModule> arraylist = new ArrayList<>();

        for (Module module : Adapt.getInstance().getModuleManager().getAll()) {
            if (module instanceof HudModule hudmodule) {
                arraylist.add(hudmodule);
            }
        }

        return arraylist;
    }

    public void renderHud(GuiGraphicsExtractor graphics, DeltaTracker delta) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!(minecraft.gui.screen() instanceof HudEditorScreen)) {
            if (!minecraft.gui.hud.isHidden()) {
                this.renderModules(graphics, delta.getGameTimeDeltaPartialTick(false), false);
                NotificationsModule notificationsmodule = Adapt.getInstance().getModuleManager().getByClass(NotificationsModule.class);
                if (notificationsmodule != null && notificationsmodule.isEnabled()) {
                    long i = Profiler.beginSection();
                    NotificationRenderer.render(graphics);
                    Profiler.endSection("notifications", i);
                }
            }
        }
    }

    public void renderModules(GuiGraphicsExtractor graphics, float value, boolean flag2) {
        int i = graphics.guiWidth();
        int j = graphics.guiHeight();

        for (HudModule hudmodule : this.getHudModules()) {
            hudmodule.visibilitySpring.target(hudmodule.isEnabled() ? 1.0F : 0.0F);
            float f = hudmodule.visibilitySpring.update();
            if (!(f < 0.02F) && (flag2 || hudmodule.shouldRender())) {
                float f1 = hudmodule.resolveX(i);
                float f2 = hudmodule.resolveY(j);
                float f3 = hudmodule.getEffectiveScale() * (0.85F + 0.15F * Math.min(1.0F, f));
                graphics.pose().pushMatrix();
                if (Math.abs(f3 - 1.0F) < 0.001F) {
                    graphics.pose().translate(Render2D.snapToPixel(f1), Render2D.snapToPixel(f2));
                } else {
                    graphics.pose().translate(f1 + hudmodule.getScaledWidth() / 2.0F, f2 + hudmodule.getScaledHeight() / 2.0F);
                    graphics.pose().scale(f3, f3);
                    graphics.pose().translate(-hudmodule.getWidth() / 2.0F, -hudmodule.getHeight() / 2.0F);
                }

                boolean flag = Math.abs(f3 - 1.0F) >= 0.001F && Math.abs(f3 - hudmodule.getEffectiveScale()) < 0.001F;
                Fonts.setNativeTransformScale(flag);
                long k = Profiler.beginSection();

                try {
                    hudmodule.render(graphics, value, flag2);
                } finally {
                    Profiler.endSection(hudmodule.getId(), k);
                    Fonts.setNativeTransformScale(false);
                }

                graphics.pose().popMatrix();
            }
        }
    }
}
