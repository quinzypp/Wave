package dev.adapt.v3.feature.module.impl.combat;

import dev.adapt.v3.Adapt;
import dev.adapt.v3.event.impl.AttackEvent;
import dev.adapt.v3.event.impl.PostMovementPacketEvent;
import dev.adapt.v3.event.impl.PreMovementPacketEvent;
import dev.adapt.v3.event.impl.PreTickEvent;
import dev.adapt.v3.event.impl.ReceivePacketEvent;
import dev.adapt.v3.event.system.Subscribe;
import dev.adapt.v3.feature.helper.projectile.Box;
import dev.adapt.v3.feature.helper.projectile.MotionPredictor;
import dev.adapt.v3.feature.helper.projectile.ProjectileSolver;
import dev.adapt.v3.feature.helper.rotation.MovementFixHelper;
import dev.adapt.v3.feature.helper.slot.SlotHelper;
import dev.adapt.v3.feature.module.Module;
import dev.adapt.v3.feature.module.ModuleCategory;
import dev.adapt.v3.feature.module.impl.combat.killaura.KillAuraModule;
import dev.adapt.v3.feature.module.impl.combat.killaura.target.AimedTarget;
import dev.adapt.v3.feature.module.impl.world.scaffold.ScaffoldModule;
import dev.adapt.v3.feature.property.impl.BooleanProperty;
import dev.adapt.v3.feature.property.impl.MultiBooleanProperty;
import dev.adapt.v3.feature.property.impl.NumberProperty;
import dev.adapt.v3.mixin.MultiPlayerGameModeAccessor;
import dev.adapt.v3.util.PacketUtility;
import dev.adapt.v3.util.player.ChatUtility;
import dev.adapt.v3.util.player.PlayerUtility;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.item.EggItem;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SnowballItem;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class AutoProjectileModule extends Module {
    public static final String ITEM_SNOWBALLS = "Snowballs";
    public static final String ITEM_EGGS = "Eggs";
    public static final String ITEM_ROD = "Rod";
    public static final String WHEN_COMBOED = "Getting comboed";
    public static final String WHEN_OPENER = "Opener";
    public static final String WHEN_CHASE = "Chase";
    public static final String FILTER_LOS = "Line of sight";
    public static final String FILTER_TEAMMATES = "Ignore teammates";
    public static final String FILTER_BOTS = "Ignore bots";
    public static final long NANOS_PER_MS = 1000000L;
    public static final float MS_PER_TICK = 50.0F;
    public static final int ROD_EXTRA_HOLD_TICKS = 2;
    public static final double MIN_KNOCKBACK_SPEED = 0.15;
    public static final double MAX_ATTACKER_DIST = 6.0;
    public static final double MIN_ATTACKER_DOT = 0.6;
    public static final long COMBO_RESET_NS = 1500000000L;
    public static final double MIN_CLOSING_DELTA = 0.3;
    public static final int CLOSING_LOOKAHEAD_TICKS = 5;
    public static final double TRACK_EXTRA_RANGE = 8.0;
    public static final float MAX_AIM_RECALC_DEG = 10.0F;
    public final MultiBooleanProperty items = new MultiBooleanProperty("Items", "Snowballs", true, "Eggs", true, "Rod", false);
    public final MultiBooleanProperty when = new MultiBooleanProperty("When", "Getting comboed", true, "Opener", true, "Chase", false);
    public final NumberProperty comboHits = (NumberProperty)new NumberProperty("Combo Hits", " hits", 2.0, 1.0, 5.0, 1.0)
        .hideIf(() -> !this.when.isEnabled("Getting comboed"));
    public final NumberProperty comboTime = (NumberProperty)new NumberProperty("Combo Time", " s", 3.0, 1.0, 10.0, 0.5)
        .hideIf(() -> !this.when.isEnabled("Chase"));
    public final NumberProperty pastReach = new NumberProperty("Past Reach", " blocks", 3.0, 0.5, 12.0, 0.5);
    public final NumberProperty minDelay = new NumberProperty("Min Delay", " ms", 400.0, 0.0, 2000.0, 10.0);
    public final NumberProperty maxDelay = new NumberProperty("Max Delay", " ms", 600.0, 0.0, 2000.0, 10.0);
    public final NumberProperty rodCooldown = (NumberProperty)new NumberProperty("Rod Cooldown", " s", 1.5, 0.0, 5.0, 0.25)
        .hideIf(() -> !this.items.isEnabled("Rod"));
    public final BooleanProperty pingOverride = new BooleanProperty("Ping Override", false);
    public final NumberProperty ping = (NumberProperty)new NumberProperty("Ping", " ms", 50.0, 0.0, 500.0, 5.0).hideIf(() -> !this.pingOverride.getValue());
    public final MultiBooleanProperty filters = new MultiBooleanProperty("Filters", "Line of sight", true, "Ignore teammates", true, "Ignore bots", true);
    public final BooleanProperty pauseWhenUsingItem = new BooleanProperty("Pause When Using Item", true);
    public final BooleanProperty debug = new BooleanProperty("Debug", false);
    public String lastDebugMessage;
    public long lastDebugNs;
    public final ProjectileSolver ballisticsSolver = new ProjectileSolver();
    public final MotionPredictor targetPredictor = new MotionPredictor();
    public final List<AutoProjectileModule.TargetSnapshot> snapshots = new ArrayList<>();
    public final float[] aimAngles = new float[2];
    public final Box predictedBox = new Box();
    public AutoProjectileModule.State state = AutoProjectileModule.State.IDLE;
    public Object lastPlayer;
    public Object lastLevel;
    public long nextThrowNs;
    public Entity lastAttackedTarget;
    public long lastAttackNs;
    public int projectileSlot;
    public ProjectileSolver.Type projectileType;
    public Entity activeTarget;
    public long throwComboHitNs;
    public int flightTicks;
    public int holdTicks;
    public Entity lastRodTarget;
    public long lastRodCastNs;
    public Entity lastAttacker;
    public int comboReceivedCount;
    public long lastHitByAttackerNs;
    public String triggerReason;
    public AutoProjectileModule.TargetSnapshot closestTrackingTarget;

    public AutoProjectileModule() {
        super("Auto Projectile", "Throws snowballs and eggs, or casts a rod, to break combos and start them.", ModuleCategory.COMBAT);
        this.addProperties(
            this.items,
            this.when,
            this.comboHits,
            this.comboTime,
            this.pastReach,
            this.minDelay,
            this.maxDelay,
            this.rodCooldown,
            this.pingOverride,
            this.ping,
            this.filters,
            this.pauseWhenUsingItem,
            this.debug
        );
    }

    public boolean isActive() {
        return this.isEnabled() && this.state != AutoProjectileModule.State.IDLE;
    }

    @Override
    public void onEnable() {
        this.state = AutoProjectileModule.State.IDLE;
        this.resetTracking();
    }

    @Override
    public void onDisable() {
        this.cancelAction();
    }

    public void resetTracking() {
        this.targetPredictor.reset();
        this.snapshots.clear();
        this.lastPlayer = this.lastLevel = null;
        this.lastAttackedTarget = this.lastAttacker = this.lastRodTarget = null;
        this.comboReceivedCount = 0;
        this.nextThrowNs = System.nanoTime();
    }

    @Subscribe
    public void onAttack(AttackEvent event) {
        this.lastAttackedTarget = event.getTarget();
        this.lastAttackNs = System.nanoTime();
        if (event.getTarget() == this.lastAttacker) {
            this.comboReceivedCount = 0;
        }
    }

    @Subscribe(priority = 1000)
    public void onReceivePacket(ReceivePacketEvent event) {
        if (mc.player != null
            && mc.level != null
            && event.getPacket() instanceof ClientboundSetEntityMotionPacket clientboundsetentitymotionpacket
            && clientboundsetentitymotionpacket.id() == mc.player.getId()) {
            Vec3 vec31 = clientboundsetentitymotionpacket.movement();
            double d0 = vec31.x;
            double d1 = vec31.z;
            double d2 = Math.sqrt(d0 * d0 + d1 * d1);
            if (!(d2 < 0.15)) {
                Vec3 vec3 = mc.player.getEyePosition();
                this.collectSnapshots();
                Entity entity = null;
                double d3 = 0.6;

                for (AutoProjectileModule.TargetSnapshot autoprojectilemodule$targetsnapshot : this.snapshots) {
                    double d4 = vec3.x - autoprojectilemodule$targetsnapshot.x;
                    double d5 = vec3.z - autoprojectilemodule$targetsnapshot.z;
                    double d6 = Math.sqrt(d4 * d4 + d5 * d5);
                    if (!(d6 > 6.0) && !(d6 < 0.001)) {
                        double d7 = (d4 * d0 + d5 * d1) / (d6 * d2);
                        if (d7 > d3) {
                            d3 = d7;
                            entity = autoprojectilemodule$targetsnapshot.entity;
                        }
                    }
                }

                if (entity != null) {
                    long i = System.nanoTime();
                    if (entity != this.lastAttacker || i - this.lastHitByAttackerNs > 1500000000L) {
                        this.comboReceivedCount = 0;
                    }

                    this.lastAttacker = entity;
                    this.comboReceivedCount++;
                    this.lastHitByAttackerNs = i;
                    this.logDebug("hit by " + entity.getName().getString() + " (" + this.comboReceivedCount + " in a row)", true);
                }
            }
        }
    }

    @Subscribe
    public void onPreTick(PreTickEvent event) {
        if (mc.player != null && mc.level != null && mc.gameMode != null) {
            if (mc.player != this.lastPlayer || mc.level != this.lastLevel) {
                this.cancelAction();
                this.resetTracking();
                this.lastPlayer = mc.player;
                this.lastLevel = mc.level;
            }

            if (mc.gui.screen() != null) {
                this.cancelAction();
                this.targetPredictor.reset();
            } else {
                Vec3 vec3 = mc.player.getEyePosition();
                AutoProjectileModule.TargetSnapshot autoprojectilemodule$targetsnapshot = this.selectTarget(vec3);
                AutoProjectileModule.TargetSnapshot autoprojectilemodule$targetsnapshot1 = autoprojectilemodule$targetsnapshot != null
                    ? autoprojectilemodule$targetsnapshot
                    : this.closestTrackingTarget;
                if (autoprojectilemodule$targetsnapshot1 == null) {
                    this.targetPredictor.reset();
                } else {
                    this.targetPredictor
                        .recordSample(
                            autoprojectilemodule$targetsnapshot1.entity,
                            autoprojectilemodule$targetsnapshot1.x,
                            autoprojectilemodule$targetsnapshot1.y,
                            autoprojectilemodule$targetsnapshot1.z,
                            autoprojectilemodule$targetsnapshot1.width,
                            autoprojectilemodule$targetsnapshot1.height
                        );
                }

                if (this.state == AutoProjectileModule.State.HOLD) {
                    this.tickRodHold();
                } else {
                    if (this.state == AutoProjectileModule.State.THROW) {
                        this.cancelAction();
                    }

                    Vec3 vec31 = mc.player.getDeltaMovement();
                    Vec3 vec32 = vec3.add(vec31.x, mc.player.onGround() ? 0.0 : vec31.y, vec31.z);
                    if (this.tryPrepareThrow(autoprojectilemodule$targetsnapshot, vec32)) {
                        this.aimAngles[0] = mc.player.getYRot() + Mth.wrapDegrees(this.aimAngles[0] - mc.player.getYRot());
                        MovementFixHelper.setMovementYaw(this.aimAngles[0]);
                    }
                }
            }
        } else {
            this.cancelAction();
            this.resetTracking();
        }
    }

    @Subscribe(priority = -100)
    public void onPreMotion(PreMovementPacketEvent event) {
        if (this.state == AutoProjectileModule.State.THROW && mc.player != null) {
            Vec3 vec3 = mc.player.getEyePosition();
            Vec3 vec31 = new Vec3(mc.player.getX() - mc.player.xo, mc.player.getY() - mc.player.yo, mc.player.getZ() - mc.player.zo);
            boolean flag = mc.player.onGround();
            double d0 = this.getPingTicks();
            boolean flag1 = this.filters.isEnabled("Line of sight");
            if (!this.ballisticsSolver.canHit(this.projectileType, vec3, vec31, flag, this.targetPredictor, d0, flag1, this.aimAngles[0], this.aimAngles[1])) {
                float f = this.aimAngles[0];
                if (!this.ballisticsSolver.solve(this.projectileType, vec3, vec31, flag, this.targetPredictor, d0, flag1, this.aimAngles)) {
                    this.logDebug("skip: missed after moving", true);
                    this.cancelAction();
                    return;
                }

                float f1 = Mth.wrapDegrees(this.aimAngles[0] - f);
                if (Math.abs(f1) > 10.0F) {
                    this.logDebug("skip: aim moved too far after moving", true);
                    this.cancelAction();
                    return;
                }

                this.aimAngles[0] = f + f1;
            }

            this.flightTicks = this.ballisticsSolver.getImpactTicks();
            event.setYaw(this.aimAngles[0]);
            event.setPitch(this.aimAngles[1]);
        }
    }

    public void tickRodHold() {
        this.holdTicks++;
        if (mc.player.fishing != null) {
            this.clearComboIfMatched();
        }

        boolean flag = this.holdTicks > this.flightTicks;
        if (mc.player.getInventory().getItem(this.projectileSlot).getItem() instanceof FishingRodItem
            && this.holdTicks <= this.flightTicks + 2
            && (!flag || !this.isKillAuraInRange(mc.player.getEyePosition()))) {
            SlotHelper.spoof(this.projectileSlot);
        } else {
            this.scheduleNextDelay();
            this.cancelAction();
        }
    }

    @Subscribe
    public void onPostMotion(PostMovementPacketEvent event) {
        if (this.state == AutoProjectileModule.State.THROW) {
            if (mc.player != null && mc.gameMode != null && mc.getConnection() != null) {
                boolean flag = this.projectileType == ProjectileSolver.Type.ROD;
                ItemStack itemstack = mc.player.getInventory().getItem(this.projectileSlot);
                if (flag ? this.isAllowedRod(itemstack) : this.isAllowedThrowable(itemstack, isSnowball(itemstack))) {
                    SlotHelper.spoof(this.projectileSlot);
                    ((MultiPlayerGameModeAccessor)mc.gameMode).adapt$ensureHasSentCarriedItem();
                    float f = this.aimAngles[0];
                    float f1 = this.aimAngles[1];
                    PacketUtility.sendPredicted(value -> new ServerboundUseItemPacket(InteractionHand.MAIN_HAND, value, f, f1));
                    if (flag) {
                        this.lastRodCastNs = System.nanoTime();
                        this.lastRodTarget = this.activeTarget;
                        this.holdTicks = 0;
                        this.state = AutoProjectileModule.State.HOLD;
                    } else {
                        this.clearComboIfMatched();
                        this.scheduleNextDelay();
                        this.state = AutoProjectileModule.State.IDLE;
                        this.activeTarget = null;
                    }
                } else {
                    this.cancelAction();
                }
            } else {
                this.cancelAction();
            }
        }
    }

    public static boolean isSnowball(ItemStack stack) {
        return stack.getItem() instanceof SnowballItem;
    }

    public double getPingTicks() {
        int i = 0;
        if (this.pingOverride.getValue()) {
            i = this.ping.getIntValue();
        } else if (mc.getConnection() != null) {
            PlayerInfo playerinfo = mc.getConnection().getPlayerInfo(mc.player.getUUID());
            if (playerinfo != null) {
                i = playerinfo.getLatency();
            }
        }

        return i / 50.0F;
    }

    public double getAttackReach() {
        return mc.player.entityInteractionRange();
    }

    public boolean tryPrepareThrow(AutoProjectileModule.TargetSnapshot targetSnapshot, Vec3 vec) {
        long i = System.nanoTime();
        boolean flag = "Getting comboed".equals(this.triggerReason);
        if (targetSnapshot == null) {
            return false;
        }

        if (!this.targetPredictor.isReady()) {
            return this.skipWithReason("tracking target");
        }

        if (i - this.nextThrowNs < 0L) {
            return this.skipWithReason("delay");
        }

        if (!mc.player.containerMenu.getCarried().isEmpty()) {
            return this.skipWithReason("item on cursor");
        }

        if (this.isUsingNonBlockingItem()) {
            return this.skipWithReason("using item");
        }

        if (isScaffoldEnabled()) {
            return this.skipWithReason("scaffold on");
        }

        this.projectileSlot = this.findProjectileSlot(targetSnapshot.entity, i);
        if (this.projectileSlot < 0) {
            return this.skipWithReason("no projectile in hotbar");
        }

        double d0 = this.getPingTicks();
        boolean flag1 = "Opener".equals(this.triggerReason);
        this.targetPredictor.simulate();
        if (flag1 && !this.isClosingIn(vec)) {
            return this.skipWithReason("opener: not closing in");
        }

        ProjectileSolver.Type projectilesolver$type = mc.player.getInventory().getItem(this.projectileSlot).getItem() instanceof FishingRodItem
            ? ProjectileSolver.Type.ROD
            : ProjectileSolver.Type.SNOWBALL;
        Vec3 vec3 = mc.player.getDeltaMovement();
        if (!this.ballisticsSolver
            .solve(projectilesolver$type, vec, vec3, mc.player.onGround(), this.targetPredictor, d0, this.filters.isEnabled("Line of sight"), this.aimAngles)) {
            return this.skipWithReason("no throw that hits");
        }

        if (targetSnapshot.hurtTime > d0 + this.ballisticsSolver.getImpactTicks()) {
            return this.skipWithReason("target still invulnerable");
        }

        if (!this.willHitBeforeMelee(targetSnapshot, flag1, d0, vec)) {
            return this.skipWithReason("opener: they'd reach you first");
        }

        if (!flag && this.isKillAuraInRange(vec)) {
            return this.skipWithReason("KillAura fighting");
        }

        this.flightTicks = this.ballisticsSolver.getImpactTicks();
        this.logDebug(
            "throwing "
                + projectilesolver$type.name().toLowerCase()
                + " at "
                + targetSnapshot.entity.getName().getString()
                + " ("
                + this.triggerReason
                + ", lands in "
                + this.flightTicks
                + " ticks)",
            true
        );
        this.activeTarget = targetSnapshot.entity;
        this.projectileType = projectilesolver$type;
        this.throwComboHitNs = flag ? this.lastHitByAttackerNs : 0L;
        this.state = AutoProjectileModule.State.THROW;
        return true;
    }

    public boolean willHitBeforeMelee(AutoProjectileModule.TargetSnapshot targetSnapshot, boolean flag2, double value, Vec3 vec) {
        int i = this.ballisticsSolver.getImpactTicks();
        if (targetSnapshot.hurtTime > value + i) {
            return false;
        }

        if (flag2) {
            double d0 = this.getAttackReach();
            Vec3 vec3 = mc.player.getDeltaMovement();
            boolean flag = mc.player.onGround();

            for (int j = 0; j <= i; j++) {
                this.targetPredictor.getPredictedBox(value + j, this.predictedBox);
                double d1 = vec.x + vec3.x * j;
                double d2 = vec.y + (flag ? 0.0 : vec3.y * j);
                double d3 = vec.z + vec3.z * j;
                if (this.predictedBox.distanceTo(d1, d2, d3) <= d0) {
                    return false;
                }
            }
        }

        return true;
    }

    public boolean isKillAuraInRange(Vec3 vec) {
        KillAuraModule killauramodule = Adapt.getInstance().getModuleManager().getByClass(KillAuraModule.class);
        if (killauramodule != null && killauramodule.isEnabled()) {
            AimedTarget aimedtarget = killauramodule.getTargetFinder().getAttackTarget();
            if (aimedtarget == null) {
                return false;
            }

            AABB aabb = aimedtarget.getEntity().getBoundingBox();
            return new Box().set(aabb.minX, aabb.minY, aabb.minZ, aabb.maxX, aabb.maxY, aabb.maxZ).distanceTo(vec.x, vec.y, vec.z) <= this.getAttackReach();
        } else {
            return false;
        }
    }

    public static boolean isScaffoldEnabled() {
        ScaffoldModule scaffoldmodule = Adapt.getInstance().getModuleManager().getByClass(ScaffoldModule.class);
        return scaffoldmodule != null && scaffoldmodule.isEnabled();
    }

    public boolean isUsingNonBlockingItem() {
        return this.pauseWhenUsingItem.getValue() && mc.player.isUsingItem() && !mc.player.getUseItem().has(DataComponents.BLOCKS_ATTACKS);
    }

    public boolean passesFilters(AutoProjectileModule.TargetSnapshot targetSnapshot) {
        return (!this.filters.isEnabled("Ignore teammates") || !targetSnapshot.teammate) && (!this.filters.isEnabled("Ignore bots") || !targetSnapshot.bot);
    }

    public boolean isClosingIn(Vec3 vec) {
        this.targetPredictor.getPredictedBox(0.0, this.predictedBox);
        double d0 = this.horizontalDistanceAtTick(this.predictedBox, 0, vec);
        this.targetPredictor.getPredictedBox(5.0, this.predictedBox);
        return d0 - this.horizontalDistanceAtTick(this.predictedBox, 5, vec) >= 0.3;
    }

    public double horizontalDistanceAtTick(Box box, int value, Vec3 vec) {
        Vec3 vec3 = mc.player.getDeltaMovement();
        double d0 = (box.minX + box.maxX) / 2.0 - (vec.x + vec3.x * value);
        double d1 = (box.minZ + box.maxZ) / 2.0 - (vec.z + vec3.z * value);
        return Math.sqrt(d0 * d0 + d1 * d1);
    }

    public AutoProjectileModule.TargetSnapshot selectTarget(Vec3 vec) {
        this.triggerReason = null;
        this.closestTrackingTarget = null;
        double d0 = this.getAttackReach();
        double d1 = d0 + this.pastReach.getValue();
        double d2 = d1 + 8.0;
        long i = System.nanoTime();
        boolean flag = this.when.isEnabled("Getting comboed")
            && this.lastAttacker != null
            && this.comboReceivedCount >= this.comboHits.getIntValue()
            && i - this.lastHitByAttackerNs <= 1500000000L;
        boolean flag1 = this.when.isEnabled("Chase")
            && this.lastAttackedTarget != null
            && i - this.lastAttackNs <= (long)(this.comboTime.getValue() * 1000.0 * 1000000.0);
        boolean flag2 = this.when.isEnabled("Opener");
        this.collectSnapshots();
        AutoProjectileModule.TargetSnapshot autoprojectilemodule$targetsnapshot = null;
        double d3 = Double.MAX_VALUE;

        for (AutoProjectileModule.TargetSnapshot autoprojectilemodule$targetsnapshot1 : this.snapshots) {
            if (this.passesFilters(autoprojectilemodule$targetsnapshot1)) {
                double d4 = autoprojectilemodule$targetsnapshot1.hitbox.distanceTo(vec.x, vec.y, vec.z);
                if (d4 < d2) {
                    this.closestTrackingTarget = autoprojectilemodule$targetsnapshot1;
                    d2 = d4;
                }

                if (!(d4 > d1)) {
                    if (flag && autoprojectilemodule$targetsnapshot1.entity == this.lastAttacker) {
                        this.triggerReason = "Getting comboed";
                        return autoprojectilemodule$targetsnapshot1;
                    }

                    if (!(d4 <= d0) && !"Chase".equals(this.triggerReason)) {
                        if (flag1 && autoprojectilemodule$targetsnapshot1.entity == this.lastAttackedTarget) {
                            this.triggerReason = "Chase";
                            autoprojectilemodule$targetsnapshot = autoprojectilemodule$targetsnapshot1;
                        } else if (flag2 && d4 < d3) {
                            this.triggerReason = "Opener";
                            autoprojectilemodule$targetsnapshot = autoprojectilemodule$targetsnapshot1;
                            d3 = d4;
                        }
                    }
                }
            }
        }

        return autoprojectilemodule$targetsnapshot;
    }

    public void collectSnapshots() {
        this.snapshots.clear();
        if (mc.level != null && mc.player != null && mc.getConnection() != null) {
            for (AbstractClientPlayer abstractclientplayer : mc.level.players()) {
                if (abstractclientplayer != mc.player && abstractclientplayer.isAlive() && !abstractclientplayer.isSpectator()) {
                    AABB aabb = abstractclientplayer.getBoundingBox();
                    AutoProjectileModule.TargetSnapshot autoprojectilemodule$targetsnapshot = new AutoProjectileModule.TargetSnapshot();
                    autoprojectilemodule$targetsnapshot.entity = abstractclientplayer;
                    autoprojectilemodule$targetsnapshot.hitbox
                        .set(aabb.minX, aabb.minY, aabb.minZ, aabb.maxX, aabb.maxY, aabb.maxZ)
                        .inflate(abstractclientplayer.getPickRadius());
                    autoprojectilemodule$targetsnapshot.hurtTime = abstractclientplayer.hurtTime;
                    InterpolationHandler interpolationhandler = abstractclientplayer.getInterpolation();
                    Vec3 vec3 = interpolationhandler != null && interpolationhandler.hasActiveInterpolation()
                        ? interpolationhandler.target().position()
                        : abstractclientplayer.position();
                    autoprojectilemodule$targetsnapshot.x = vec3.x;
                    autoprojectilemodule$targetsnapshot.y = vec3.y;
                    autoprojectilemodule$targetsnapshot.z = vec3.z;
                    autoprojectilemodule$targetsnapshot.width = aabb.getXsize();
                    autoprojectilemodule$targetsnapshot.height = aabb.getYsize();
                    autoprojectilemodule$targetsnapshot.teammate = PlayerUtility.isSameTeam(mc.player, abstractclientplayer);
                    autoprojectilemodule$targetsnapshot.bot = abstractclientplayer.getUUID().version() == 2
                        && mc.getConnection().getPlayerInfo(abstractclientplayer.getUUID()) == null;
                    this.snapshots.add(autoprojectilemodule$targetsnapshot);
                }
            }
        }
    }

    public int findProjectileSlot(Entity entity, long time) {
        long i = (long)(this.rodCooldown.getValue() * 1000.0 * 1000000.0);
        boolean flag = entity != this.lastRodTarget || time - this.lastRodCastNs >= i;

        for (int j = 0; j < 3; j++) {
            for (int k = 0; k < 9; k++) {
                ItemStack itemstack = mc.player.getInventory().getItem(k);
                if (j == 0
                    ? this.isAllowedThrowable(itemstack, true)
                    : (j == 1 ? this.isAllowedThrowable(itemstack, false) : flag && this.isAllowedRod(itemstack))) {
                    return k;
                }
            }
        }

        return -1;
    }

    public boolean isAllowedThrowable(ItemStack stack, boolean flag) {
        if (stack.isEmpty()) {
            return false;
        } else {
            return flag
                ? stack.getItem() instanceof SnowballItem && this.items.isEnabled("Snowballs")
                : stack.getItem() instanceof EggItem && this.items.isEnabled("Eggs");
        }
    }

    public boolean isAllowedRod(ItemStack stack) {
        return stack.getItem() instanceof FishingRodItem && this.items.isEnabled("Rod") && mc.player.fishing == null;
    }

    public void clearComboIfMatched() {
        if (this.throwComboHitNs != 0L && this.lastAttacker == this.activeTarget && this.lastHitByAttackerNs == this.throwComboHitNs) {
            this.comboReceivedCount = 0;
        }
    }

    public void scheduleNextDelay() {
        double d0 = Math.min(this.minDelay.getValue(), this.maxDelay.getValue());
        double d1 = Math.max(this.minDelay.getValue(), this.maxDelay.getValue());
        double d2 = (Math.random() + Math.random()) / 2.0;
        this.nextThrowNs = System.nanoTime() + (long)((d0 + (d1 - d0) * d2) * 1000000.0);
    }

    public boolean skipWithReason(String text) {
        this.logDebug("skip: " + text, false);
        return false;
    }

    public void logDebug(String text, boolean flag) {
        if (this.debug.getValue()) {
            long i = System.nanoTime();
            if (flag || !text.equals(this.lastDebugMessage) || i - this.lastDebugNs >= 1000000000L) {
                this.lastDebugMessage = text;
                this.lastDebugNs = i;
                ChatUtility.print("[Auto Projectile] " + text);
            }
        }
    }

    public void cancelAction() {
        if (this.state != AutoProjectileModule.State.IDLE) {
            boolean flag = this.state == AutoProjectileModule.State.HOLD;
            this.state = AutoProjectileModule.State.IDLE;
            this.activeTarget = null;
            if (flag && mc.player != null) {
                SlotHelper.getInstance().restoreNow();
            }
        }
    }

    private enum State {
        IDLE,
        THROW,
        HOLD;
    }

    private static final class TargetSnapshot {
        public Entity entity;
        public final Box hitbox = new Box();
        public int hurtTime;
        public double x;
        public double y;
        public double z;
        public double width;
        public double height;
        public boolean teammate;
        public boolean bot;

        public TargetSnapshot() {
        }
    }
}
