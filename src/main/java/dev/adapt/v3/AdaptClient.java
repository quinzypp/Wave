package dev.adapt.v3;

// VENDOR-DISABLED: import dev.adapt.v3.chat.AdaptUsers;
import dev.adapt.v3.event.EventManager;
import dev.adapt.v3.event.impl.PostTickEvent;
import dev.adapt.v3.event.impl.PreTickEvent;
import dev.adapt.v3.feature.module.impl.hud.ModuleListHud;
import dev.adapt.v3.feature.module.impl.movement.TargetStrafeModule;
import dev.adapt.v3.feature.module.impl.visual.EspModule;
import dev.adapt.v3.feature.module.impl.visual.NametagsModule;
// VENDOR-DISABLED: import dev.adapt.v3.security.LaunchGuard;
import dev.adapt.v3.util.render.ProjectionUtility;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

public final class AdaptClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // VENDOR-DISABLED: LaunchGuard.init();
        Adapt.init();
        // VENDOR-DISABLED: AdaptUsers.init();
        ClientTickEvents.START_CLIENT_TICK.register(mc -> EventManager.post(PreTickEvent.INSTANCE));
        ClientTickEvents.END_CLIENT_TICK.register(mc -> EventManager.post(PostTickEvent.INSTANCE));
        ClientLifecycleEvents.CLIENT_STOPPING.register(mc -> Adapt.getInstance().getConfigManager().save());
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, Identifier.fromNamespaceAndPath("adaptv3", "hud"), (graphics, delta) -> {
            // VENDOR-DISABLED: LaunchGuard.isAuthorised()
            if (!Minecraft.getInstance().gui.hud.isHidden()) {
                float f = ProjectionUtility.getPartialTicks();
                EspModule espmodule = EspModule.getInstance();
                if (espmodule != null) {
                    espmodule.render(graphics, f);
                    graphics.nextStratum();
                }

                TargetStrafeModule targetstrafemodule = TargetStrafeModule.getInstance();
                if (targetstrafemodule != null) {
                    targetstrafemodule.renderRing(graphics, f);
                }

                NametagsModule nametagsmodule = NametagsModule.getInstance();
                if (nametagsmodule != null) {
                    nametagsmodule.render(graphics, f);
                }
            }

            Adapt.getInstance().getHudManager().renderHud(graphics, delta);
        });
        HudElementRegistry.replaceElement(VanillaHudElements.SCOREBOARD, original -> (graphics, delta) -> {
            ModuleListHud modulelisthud = Adapt.getInstance().getModuleManager().getByClass(ModuleListHud.class);
            float f = modulelisthud == null ? 0.0F : modulelisthud.getScoreboardOffset(graphics.guiWidth(), graphics.guiHeight());
            if (f <= 0.0F) {
                original.extractRenderState(graphics, delta);
            } else {
                graphics.pose().pushMatrix();
                graphics.pose().translate(0.0F, Math.round(f));
                original.extractRenderState(graphics, delta);
                graphics.pose().popMatrix();
            }
        });
    }
}
