package dev.adapt.v3.feature.module.impl.world.scaffold.mode;

import dev.adapt.v3.event.impl.BlockPlaceEvent;
import dev.adapt.v3.event.system.Subscribe;
import dev.adapt.v3.feature.module.impl.world.scaffold.ScaffoldModule;
import dev.adapt.v3.feature.module.impl.world.scaffold.ScaffoldSettings;
import dev.adapt.v3.feature.module.mode.ModuleMode;
import net.minecraft.client.Minecraft;
import net.minecraft.world.effect.MobEffects;

public final class VanillaScaffold extends ModuleMode<ScaffoldModule> {
    public static final Minecraft MC = Minecraft.getInstance();

    public VanillaScaffold(ScaffoldModule scaffoldModule) {
        super(scaffoldModule);
    }

    @Subscribe
    public void onBlockPlaced(BlockPlaceEvent event) {
        if (!MC.player.hasEffect(MobEffects.JUMP_BOOST) && this.module.getSettings().isTower() && !MC.options.keyUse.isDown()) {
            if (MC.options.keyJump.isDown()) {
                MC.player.jumpFromGround();
            }
        }
    }

    @Override
    public Enum getMode() {
        return ScaffoldSettings.Mode.VANILLA;
    }
}
