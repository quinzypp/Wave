package dev.adapt.v3.feature.module.impl.combat.killaura;

import dev.adapt.v3.Adapt;
import dev.adapt.v3.event.impl.MouseInputEvent;
import dev.adapt.v3.event.impl.PreTickEvent;
import dev.adapt.v3.event.system.Subscribe;
import dev.adapt.v3.feature.helper.mouse.MouseHelper;
import dev.adapt.v3.feature.helper.rotation.RotationHelper;
import dev.adapt.v3.feature.helper.slot.SlotHelper;
import dev.adapt.v3.feature.helper.swing.SwingHelper;
import dev.adapt.v3.feature.module.Module;
import dev.adapt.v3.feature.module.ModuleCategory;
import dev.adapt.v3.feature.module.impl.combat.BlockModule;
import dev.adapt.v3.feature.module.impl.combat.killaura.target.AimedTarget;
import dev.adapt.v3.feature.module.impl.combat.killaura.target.TargetFinder;
import dev.adapt.v3.feature.module.impl.world.scaffold.ScaffoldModule;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult.Type;

public final class KillAuraModule extends Module {
    public final KillAuraSettings settings = new KillAuraSettings(this);
    public final TargetFinder targetFinder = new TargetFinder(this.settings);
    public int missGraceTicks;

    public KillAuraModule() {
        super("KillAura", "Finds and attacks the most relevant nearby entities.", ModuleCategory.COMBAT);
    }

    public KillAuraSettings getSettings() {
        return this.settings;
    }

    public TargetFinder getTargetFinder() {
        return this.targetFinder;
    }

    @Override
    public String getSuffix() {
        return this.settings.getMode().getDisplayName();
    }

    @Subscribe
    public void onHandleInput(MouseInputEvent event) {
        if (mc.player != null) {
            AimedTarget aimedtarget = this.targetFinder.getAttackTarget();
            if (aimedtarget != null) {
                if (mc.hitResult != null && mc.hitResult.getType() != Type.MISS) {
                    if (!mc.player.isUsingItem() || BlockModule.canAttackWhileBlocking() || BlockModule.isLegitBlocking()) {
                        if (this.settings.isOverrideRaycast()) {
                            mc.hitResult = aimedtarget.getRaytracedRotation().hitResult();
                        }

                        if (mc.hitResult.getType() == Type.ENTITY) {
                            if (this.shouldAttack(aimedtarget)) {
                                EntityHitResult entityhitresult = (EntityHitResult)mc.hitResult;
                                if (entityhitresult.getEntity() == aimedtarget.getEntity()) {
                                    MouseHelper.getAttackButton().click();
                                    aimedtarget.getTarget().recordAttack(this.missGraceTicks == 0);
                                    this.settings.getAttackCps().rollNextDelay();
                                    SwingHelper.resetTimer();
                                    if (this.missGraceTicks > 0) {
                                        this.missGraceTicks--;
                                    }
                                }
                            } else {
                                this.missGraceTicks = 0;
                            }
                        }
                    }
                }
            }
        }
    }

    public boolean shouldAttack(AimedTarget target) {
        if (this.settings.getAttackCps().isModern()) {
            return SwingHelper.isAttackCooldownReady();
        } else {
            return !target.getTarget().canDealFullDamage() && this.missGraceTicks <= 0 ? SwingHelper.canAttack(this.settings.getAttackCps(), false) : true;
        }
    }

    @Subscribe(priority = 2)
    public void onPreTick(PreTickEvent event) {
        if (!this.canRun()) {
            this.targetFinder.reset();
        } else {
            this.targetFinder.update();
            AimedTarget aimedtarget = this.targetFinder.getRotationTarget();
            if (aimedtarget != null) {
                RotationHelper.getUpdater().setRotation(aimedtarget.getRaytracedRotation().rotation(), this.settings.getRotationModel());
            }
        }
    }

    public boolean canRun() {
        if (mc.player == null || mc.level == null) {
            return false;
        }

        if (this.settings.isRequireAttackKey() && !mc.options.keyAttack.isDown()) {
            return false;
        }

        ScaffoldModule scaffoldmodule = Adapt.getInstance().getModuleManager().getByClass(ScaffoldModule.class);
        if (scaffoldmodule != null && scaffoldmodule.isEnabled()) {
            return false;
        }

        ItemStack itemstack = SlotHelper.getInstance().getVisualMainHandItem(mc.player);
        return !this.settings.isRequireWeapon() || itemstack.is(ItemTags.SWORDS) || itemstack.is(ItemTags.AXES) || itemstack.is(ItemTags.PICKAXES);
    }

    @Override
    public void onDisable() {
        this.targetFinder.reset();
        this.missGraceTicks = 0;
    }
}
