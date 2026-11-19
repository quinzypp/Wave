package dev.adapt.v3.feature.module.impl.combat.velocity;

import dev.adapt.v3.Adapt;
import dev.adapt.v3.feature.helper.rotation.MovementFixHelper;
import dev.adapt.v3.feature.helper.rotation.RotationHelper;
import dev.adapt.v3.feature.helper.rotation.model.RotationModel;
import dev.adapt.v3.feature.helper.rotation.model.impl.InstantRotationModel;
import dev.adapt.v3.feature.helper.rotation.model.impl.LinearRotationModel;
import dev.adapt.v3.feature.module.impl.combat.killaura.KillAuraModule;
import dev.adapt.v3.feature.module.impl.combat.killaura.target.AimedTarget;
import dev.adapt.v3.feature.module.impl.world.scaffold.ScaffoldModule;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec2;

public final class AdaptVelocityContext implements VelocityContext {
    public static final Minecraft mc = Minecraft.getInstance();

    public static KillAuraModule getKillAura() {
        return Adapt.getInstance().getModuleManager().getByClass(KillAuraModule.class);
    }

    @Override
    public boolean isKillAuraEnabled() {
        KillAuraModule killauramodule = getKillAura();
        return killauramodule != null && killauramodule.isEnabled();
    }

    @Override
    public LivingEntity getKillAuraTarget() {
        KillAuraModule killauramodule = getKillAura();
        if (killauramodule == null) {
            return null;
        }

        AimedTarget aimedtarget = killauramodule.getTargetFinder().getAttackTarget();
        return aimedtarget == null ? null : aimedtarget.getEntity();
    }

    @Override
    public float getAttackRange() {
        return mc.player == null ? 3.0F : (float)mc.player.entityInteractionRange();
    }

    @Override
    public boolean isCurrentTarget(LivingEntity living) {
        return living != null && living == this.getKillAuraTarget();
    }

    @Override
    public boolean shouldSkipVelocity() {
        return false;
    }

    @Override
    public boolean isScaffoldEnabled() {
        ScaffoldModule scaffoldmodule = Adapt.getInstance().getModuleManager().getByClass(ScaffoldModule.class);
        return scaffoldmodule != null && scaffoldmodule.isEnabled();
    }

    @Override
    public float getYaw() {
        return mc.player.getYRot();
    }

    @Override
    public float getPitch() {
        return mc.player.getXRot();
    }

    @Override
    public void setRotation(float value, float value2, float value3, float value4, VelocityContext.RotationMode rotationMode) {
        RotationModel rotationmodel = value3 >= 360.0F && value4 >= 360.0F ? InstantRotationModel.INSTANCE : new LinearRotationModel(value3);
        RotationHelper.getUpdater().setRotation(new Vec2(value, value2), rotationmodel);
        if (rotationMode == VelocityContext.RotationMode.STRICT) {
            MovementFixHelper.markForceFix();
        }
    }
}
