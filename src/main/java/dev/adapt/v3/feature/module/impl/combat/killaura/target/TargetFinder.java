package dev.adapt.v3.feature.module.impl.combat.killaura.target;

import dev.adapt.v3.feature.helper.target.TargetHelper;
import dev.adapt.v3.feature.helper.target.TargetProperties;
import dev.adapt.v3.feature.helper.target.impl.LivingTarget;
import dev.adapt.v3.feature.module.impl.combat.killaura.KillAuraSettings;
import dev.adapt.v3.util.player.PlayerUtility;
import dev.adapt.v3.util.player.RaytracedRotation;
import dev.adapt.v3.util.player.RotationUtility;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public final class TargetFinder {
    public static final Minecraft mc = Minecraft.getInstance();
    public final KillAuraSettings properties;
    public @Nullable AimedTarget attackTarget;
    public @Nullable AimedTarget rotationTarget;
    public double closestDistance = Double.MAX_VALUE;
    public @Nullable Map<Integer, CombatTarget> trackedTargets;

    public TargetFinder(KillAuraSettings killAuraSettings) {
        this.properties = killAuraSettings;
    }

    public void update() {
        this.closestDistance = Double.MAX_VALUE;
        this.updateAttackTarget();
        this.updateRotationTarget();
    }

    public void updateAttackTarget() {
        double d0 = mc.player.entityInteractionRange();
        List list = this.collectCandidates(d0);
        if (list != null && !list.isEmpty()) {
            this.attackTarget = this.selectBestTarget(list, d0, false);
        } else {
            this.attackTarget = null;
        }
    }

    public void updateRotationTarget() {
        if (this.attackTarget != null) {
            this.rotationTarget = this.attackTarget;
        } else {
            double d0 = this.properties.getRotationRange();
            List list = this.collectCandidates(d0);
            if (list != null && !list.isEmpty()) {
                this.rotationTarget = this.selectBestTarget(list, d0, true);
            } else {
                this.rotationTarget = null;
            }
        }
    }

    public List<LivingTarget> collectCandidates(double value) {
        if (mc.player == null) {
            return null;
        }

        TargetProperties targetproperties = this.properties.getTargetProperties();
        List list = TargetHelper.getInstance().getTargets(targetproperties.getFlags(), LivingTarget.class);
        Map<Integer, CombatTarget> hashmap = new HashMap<>();
        if (this.trackedTargets == null) {
            this.trackedTargets = new HashMap<>();
        }

        Iterator iterator = list.iterator();

        while (iterator.hasNext()) {
            LivingTarget livingtarget = (LivingTarget)iterator.next();
            if (livingtarget.isLocalPlayer()) {
                iterator.remove();
            } else {
                LivingEntity livingentity = livingtarget.getEntity();
                if (!livingentity.isDeadOrDying() && livingentity.isAttackable() && RotationUtility.isWithinFov(livingentity, this.properties.getFov())) {
                    double d0 = PlayerUtility.getDistanceToEntityBox(livingentity);
                    if (d0 < this.closestDistance) {
                        this.closestDistance = d0;
                    }

                    if (d0 > value) {
                        iterator.remove();
                    } else {
                        hashmap.put(livingtarget.getEntityId(), this.getOrCreateCombatTarget(livingtarget));
                    }
                } else {
                    iterator.remove();
                }
            }
        }

        this.trackedTargets = hashmap;
        return list;
    }

    public AimedTarget selectBestTarget(List<LivingTarget> livingTargets, double value, boolean flag) {
        livingTargets.sort(Comparator.comparingDouble(target -> {
            double d0 = flag ? target.getEntity().distanceToSqr(mc.player) : target.getEffectiveHealth();
            return !target.matches(1) ? d0 * 2.0 : d0;
        }));
        List<AimedTarget> list = this.raytraceCandidates(livingTargets, value);
        if (!flag && this.properties.getMode() == KillAuraSettings.Mode.SWITCH && list.size() > 1) {
            list.sort(
                Comparator.comparingDouble(
                    target2 -> -(
                        target2.getTarget().getLivingTarget().getEffectiveHealth() * 25.0
                            + (target2.getTarget().getHitTracker() == null ? 0L : target2.getTarget().getHitTracker().getElapsedMs())
                    )
                )
            );
        }

        return list.isEmpty() ? null : (AimedTarget)list.getFirst();
    }

    public List<AimedTarget> raytraceCandidates(List<LivingTarget> livingTargets, double value) {
        Vec3 vec3 = mc.player.getEyePosition();
        List<AimedTarget> arraylist = new ArrayList<>();

        for (LivingTarget livingtarget : livingTargets) {
            LivingEntity livingentity = livingtarget.getEntity();
            Vec3 vec31 = PlayerUtility.getClosestPointOnEntity(vec3, livingentity);
            RaytracedRotation raytracedrotation = RotationUtility.findEntityRotation(livingentity, vec31, value);
            if (raytracedrotation != null) {
                arraylist.add(new AimedTarget(this.getOrCreateCombatTarget(livingtarget), raytracedrotation));
            }
        }

        return arraylist;
    }

    public CombatTarget getOrCreateCombatTarget(LivingTarget target) {
        if (this.trackedTargets.containsKey(target.getEntityId())) {
            return this.trackedTargets.get(target.getEntityId());
        }

        CombatTarget combattarget = new CombatTarget(target);
        this.trackedTargets.put(target.getEntityId(), combattarget);
        return combattarget;
    }

    public void reset() {
        this.closestDistance = Double.MAX_VALUE;
        this.trackedTargets = null;
        this.attackTarget = null;
        this.rotationTarget = null;
    }

    public @Nullable AimedTarget getAttackTarget() {
        return this.attackTarget;
    }

    public @Nullable AimedTarget getRotationTarget() {
        return this.rotationTarget;
    }

    public double getClosestDistance() {
        return this.closestDistance;
    }

    public boolean hasAttackTarget() {
        return this.getAttackTarget() != null;
    }
}
