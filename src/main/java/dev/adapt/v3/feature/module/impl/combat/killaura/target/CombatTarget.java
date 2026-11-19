package dev.adapt.v3.feature.module.impl.combat.killaura.target;

import dev.adapt.v3.feature.helper.target.impl.LivingTarget;
import dev.adapt.v3.util.player.PlayerUtility;
import net.minecraft.client.Minecraft;
import org.jspecify.annotations.Nullable;

public final class CombatTarget {
    public final LivingTarget livingTarget;
    public @Nullable TargetHitTracker hitTracker;

    public CombatTarget(LivingTarget target) {
        this.livingTarget = target;
    }

    public LivingTarget getLivingTarget() {
        return this.livingTarget;
    }

    public void recordAttack(boolean flag) {
        double d0 = this.estimateAttackDamage();
        if (this.hitTracker == null) {
            this.hitTracker = new TargetHitTracker(d0);
        } else {
            this.hitTracker.update(flag, d0);
        }
    }

    public boolean canDealFullDamage() {
        if (this.hitTracker == null) {
            return true;
        }

        double d0 = this.estimateAttackDamage();
        return this.livingTarget.getEffectiveHealth() <= d0 ? true : this.hitTracker.getElapsedMs() >= 470L || d0 > this.hitTracker.getLastDamage();
    }

    public double estimateAttackDamage() {
        Minecraft minecraft = Minecraft.getInstance();
        double d0 = PlayerUtility.getItemAttackDamage(minecraft.player.getMainHandItem());
        if (d0 < 0.5) {
            d0 = 0.5;
        }

        if (PlayerUtility.canCrit() && minecraft.player.fallDistance > 0.0) {
            d0 *= 1.5;
        }

        return d0;
    }

    public @Nullable TargetHitTracker getHitTracker() {
        return this.hitTracker;
    }
}
