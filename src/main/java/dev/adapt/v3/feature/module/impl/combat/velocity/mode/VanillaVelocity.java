package dev.adapt.v3.feature.module.impl.combat.velocity.mode;

import dev.adapt.v3.event.impl.VelocityUpdateEvent;
import dev.adapt.v3.event.system.Subscribe;
import dev.adapt.v3.feature.module.impl.combat.velocity.VelocityMode;
import dev.adapt.v3.feature.module.impl.combat.velocity.VelocityModule;
import dev.adapt.v3.feature.property.impl.NumberProperty;
import net.minecraft.client.Minecraft;

public final class VanillaVelocity extends VelocityMode {
    public static final Minecraft mc = Minecraft.getInstance();
    public final NumberProperty horizontal = (NumberProperty)new NumberProperty("Horizontal", 0.0, 0.0, 100.0, 1.0)
        .hideIf(() -> this.module.getCurrentMode() != this);
    public final NumberProperty vertical = (NumberProperty)new NumberProperty("Vertical", 100.0, 0.0, 100.0, 1.0)
        .hideIf(() -> this.module.getCurrentMode() != this);

    public VanillaVelocity(VelocityModule velocityModule) {
        super(velocityModule);
        velocityModule.addProperties(this.horizontal, this.vertical);
    }

    @Override
    public String getSuffix() {
        return this.horizontal.getValue().intValue() + " " + this.vertical.getValue().intValue();
    }

    @Subscribe
    public void onVelocityUpdate(VelocityUpdateEvent event) {
        if (mc.player != null && !this.getContext().shouldSkipVelocity()) {
            double d0 = this.horizontal.getValue() / 100.0;
            double d1 = this.vertical.getValue() / 100.0;
            event.cancel();
            if (event.isExplosion() || d0 != 0.0 || d1 != 0.0) {
                double d2 = event.getX() * d0;
                double d3 = event.getY() * d1;
                double d4 = event.getZ() * d0;
                if (d0 != 0.0) {
                    mc.player.setDeltaMovement(d2, mc.player.getDeltaMovement().y, d4);
                }

                if (d1 != 0.0) {
                    mc.player.setDeltaMovement(mc.player.getDeltaMovement().x, d3, mc.player.getDeltaMovement().z);
                }
            }
        }
    }

    @Override
    public Enum getMode() {
        return VelocityModule.Mode.VANILLA;
    }
}
