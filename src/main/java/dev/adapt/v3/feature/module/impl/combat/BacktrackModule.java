package dev.adapt.v3.feature.module.impl.combat;

import dev.adapt.v3.Adapt;
import dev.adapt.v3.event.impl.AttackEvent;
import dev.adapt.v3.event.impl.PreTickEvent;
import dev.adapt.v3.event.impl.ReceivePacketEvent;
import dev.adapt.v3.event.system.Subscribe;
import dev.adapt.v3.feature.module.Module;
import dev.adapt.v3.feature.module.ModuleCategory;
import dev.adapt.v3.feature.property.impl.BooleanProperty;
import dev.adapt.v3.feature.property.impl.ColorProperty;
import dev.adapt.v3.feature.property.impl.NumberProperty;
import dev.adapt.v3.util.player.PlayerUtility;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundDisconnectPacket;
import net.minecraft.network.protocol.game.ClientboundDamageEventPacket;
import net.minecraft.network.protocol.game.ClientboundDisguisedChatPacket;
import net.minecraft.network.protocol.game.ClientboundEntityPositionSyncPacket;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import net.minecraft.network.protocol.game.ClientboundSetHealthPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.network.protocol.game.VecDeltaCodec;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class BacktrackModule extends Module {
    public final NumberProperty minRange = new NumberProperty("Min Range", 1.0, 0.0, 10.0, 0.1);
    public final NumberProperty maxRange = new NumberProperty("Max Range", 3.0, 0.0, 10.0, 0.1);
    public final NumberProperty minDelay = new NumberProperty("Min Delay", "ms", 100.0, 0.0, 1000.0, 10.0);
    public final NumberProperty maxDelay = new NumberProperty("Max Delay", "ms", 150.0, 0.0, 1000.0, 10.0);
    public final NumberProperty trackingBuffer = new NumberProperty("Tracking Buffer", "ms", 500.0, 0.0, 2000.0, 10.0);
    public final NumberProperty chance = new NumberProperty("Chance", "%", 100.0, 0.0, 100.0, 1.0);
    public final BooleanProperty resetOnHit = new BooleanProperty("Reset On Hit", false);
    public final NumberProperty lastAttackTimeToWork = new NumberProperty("Last Attack Time To Work", "ms", 1000.0, 0.0, 5000.0, 10.0);
    public final BooleanProperty showServerPos = new BooleanProperty("Show Server Pos", true);
    public final ColorProperty boxColor = (ColorProperty)new ColorProperty("Box Color", 2367635).hideIf(() -> !this.showServerPos.getValue());
    public final NumberProperty boxAlpha = (NumberProperty)new NumberProperty("Box Alpha", 87.0, 0.0, 255.0, 1.0).hideIf(() -> !this.showServerPos.getValue());
    public final ColorProperty boxOutlineColor = (ColorProperty)new ColorProperty("Box Outline Color", 2367635).hideIf(() -> !this.showServerPos.getValue());
    public final List<BacktrackModule.QueuedPacket> packetQueue = new ArrayList<>();
    public final BacktrackModule.Cooldown rangeBufferTimer = new BacktrackModule.Cooldown();
    public final BacktrackModule.Cooldown lastAttackTimer = new BacktrackModule.Cooldown();
    public final BacktrackModule.ServerPosTracker serverPosTracker = new BacktrackModule.ServerPosTracker();
    public Vec3 prevRenderPos;
    public Vec3 renderPos;
    public boolean chancePassed = this.rollChance();
    public Entity trackedTarget;
    public long currentDelayMs = this.rollDelayMs();
    public static final int LERP_STEPS = 3;
    public Vec3 targetRenderPos;
    public int lerpTicksRemaining;

    public BacktrackModule() {
        super("Backtrack", "Delays incoming packets so you hit players where they were.", ModuleCategory.COMBAT);
        this.addProperties(
            this.minRange,
            this.maxRange,
            this.minDelay,
            this.maxDelay,
            this.trackingBuffer,
            this.chance,
            this.resetOnHit,
            this.lastAttackTimeToWork,
            this.showServerPos,
            this.boxColor,
            this.boxAlpha,
            this.boxOutlineColor
        );
    }

    public static BacktrackModule getInstance() {
        Adapt adapt = Adapt.getInstance();
        return adapt == null ? null : adapt.getModuleManager().getByClass(BacktrackModule.class);
    }

    public BacktrackModule.EspData getEspData(float value) {
        Entity entity = this.trackedTarget;
        if (this.isEnabled() && this.showServerPos.getValue() && entity != null && this.renderPos != null) {
            Vec3 vec3 = this.prevRenderPos == null ? this.renderPos : this.prevRenderPos;
            Vec3 vec31 = vec3.lerp(this.renderPos, value);
            int i = this.boxAlpha.getIntValue() << 24 | this.boxColor.getValue() & 16777215;
            int j = 0xFF000000 | this.boxOutlineColor.getValue() & 16777215;
            return new BacktrackModule.EspData(entity, vec31, i, j);
        } else {
            return null;
        }
    }

    public void tickRenderPos() {
        if (this.trackedTarget == null) {
            this.prevRenderPos = this.renderPos = this.targetRenderPos = null;
        } else {
            Vec3 vec3 = this.serverPosTracker.getPos();
            if (this.renderPos == null) {
                this.prevRenderPos = this.renderPos = this.targetRenderPos = vec3;
                this.lerpTicksRemaining = 0;
            } else {
                if (!vec3.equals(this.targetRenderPos)) {
                    this.targetRenderPos = vec3;
                    this.lerpTicksRemaining = 3;
                }

                this.prevRenderPos = this.renderPos;
                if (this.lerpTicksRemaining > 0) {
                    this.renderPos = this.renderPos.lerp(this.targetRenderPos, 1.0 / this.lerpTicksRemaining);
                    this.lerpTicksRemaining--;
                }
            }
        }
    }

    public boolean isDelaying() {
        return this.isEnabled() && !this.packetQueue.isEmpty();
    }

    @Override
    public void onEnable() {
        this.resetState(false, false, true);
    }

    @Override
    public void onDisable() {
        this.resetState(true, false, true);
    }

    @Subscribe(priority = -1000)
    public void onReceivePacket(ReceivePacketEvent event) {
        if (!event.isCancelled() && mc.player != null && mc.level != null) {
            Packet packet = event.getPacket();
            boolean flag = this.isTrackingActive();
            boolean flag1 = !this.packetQueue.isEmpty();
            if (flag1 || flag) {
                if (!(packet instanceof ClientboundSystemChatPacket) && !(packet instanceof ClientboundDisguisedChatPacket)) {
                    if (!(packet instanceof ClientboundPlayerPositionPacket)
                        && !(packet instanceof ClientboundDisconnectPacket)
                        && !(packet instanceof ClientboundRespawnPacket)
                        && !(packet instanceof ClientboundLoginPacket)) {
                        if (!(
                            packet instanceof ClientboundSoundPacket clientboundsoundpacket
                                && clientboundsoundpacket.getSound().value() == SoundEvents.PLAYER_HURT
                        )) {
                            if (this.resetOnHit.getValue()
                                && packet instanceof ClientboundDamageEventPacket clientbounddamageeventpacket
                                && clientbounddamageeventpacket.entityId() == mc.player.getId()) {
                                this.flushAllPackets();
                                this.currentDelayMs = this.rollDelayMs();
                            } else if (packet instanceof ClientboundSetHealthPacket clientboundsethealthpacket
                                && clientboundsethealthpacket.getHealth() <= 0.0F) {
                                this.resetState(true, false, true);
                            } else {
                                Entity entity = this.trackedTarget;
                                if (entity != null) {
                                    Vec3 vec3 = this.serverPosTracker.updateFromPacket(packet, entity);
                                    if (vec3 != null && distanceToBoxSqr(entity, vec3) < distanceToBoxSqr(entity, entity.position())) {
                                        this.flushAllPackets();
                                        return;
                                    }
                                }

                                event.cancel();
                                this.packetQueue.add(new BacktrackModule.QueuedPacket(packet, System.currentTimeMillis()));
                            }
                        }
                    } else {
                        this.resetState(true, false, true);
                    }
                }
            }
        }
    }

    @Subscribe
    public void onPreTick(PreTickEvent event) {
        if (mc.player != null && mc.level != null && mc.getConnection() != null) {
            boolean flag = !this.packetQueue.isEmpty();
            if (this.isTrackingActive()) {
                long i = System.currentTimeMillis();
                this.flushExpiredPackets(i - this.currentDelayMs);
            } else if (flag) {
                this.flushAllPackets();
                this.resetState(true, false, true);
            }

            if (this.packetQueue.isEmpty()) {
                this.currentDelayMs = this.rollDelayMs();
            }

            this.tickRenderPos();
        } else {
            this.resetState(true, true, true);
        }
    }

    @Subscribe
    public void onAttack(AttackEvent event) {
        this.lastAttackTimer.reset();
        this.chancePassed = this.rollChance();
        if (event.getTarget() != null) {
            this.setTrackedTarget(event.getTarget());
        }
    }

    public void setTrackedTarget(Entity trackedTarget) {
        if (this.canTrack(trackedTarget)) {
            if (trackedTarget != this.trackedTarget) {
                this.resetState(true, false, false);
                this.serverPosTracker.initFromEntity(trackedTarget);
                this.prevRenderPos = this.renderPos = this.targetRenderPos = this.serverPosTracker.getPos();
                this.lerpTicksRemaining = 0;
            }

            this.trackedTarget = trackedTarget;
        }
    }

    public void resetState(boolean flag, boolean flag2, boolean flag3) {
        if (flag && !flag2) {
            this.flushAllPackets();
        } else if (flag2) {
            this.packetQueue.clear();
        }

        this.trackedTarget = null;
        this.serverPosTracker.setPos(Vec3.ZERO);
        this.prevRenderPos = this.renderPos = this.targetRenderPos = null;
    }

    public boolean canTrack(Entity entity) {
        double d0 = Math.sqrt(distanceToBoxSqr(entity, entity.position()));
        double d1 = Math.min(this.minRange.getValue(), this.maxRange.getValue());
        double d2 = Math.max(this.minRange.getValue(), this.maxRange.getValue());
        boolean flag = d0 >= d1 && d0 <= d2;
        if (flag) {
            this.rangeBufferTimer.reset();
        }

        return (flag || !this.rangeBufferTimer.hasElapsed(this.trackingBuffer.getValue().longValue()))
            && isValidPlayerTarget(entity)
            && mc.player.tickCount > 10
            && this.chancePassed
            && !this.lastAttackTimer.hasElapsed(this.lastAttackTimeToWork.getValue().longValue());
    }

    public boolean isTrackingActive() {
        Entity entity = this.trackedTarget;
        return entity != null && entity.isAlive() && this.canTrack(entity);
    }

    public static boolean isValidPlayerTarget(Entity entity) {
        return entity instanceof Player player
            && player != mc.player
            && player.isAlive()
            && !player.isSpectator()
            && !PlayerUtility.isSameTeam(mc.player, player);
    }

    public static double distanceToBoxSqr(Entity entity, Vec3 vec) {
        AABB aabb = entity.getBoundingBox().inflate(entity.getPickRadius()).move(vec.subtract(entity.position()));
        return aabb.distanceToSqr(mc.player.getEyePosition());
    }

    public void flushAllPackets() {
        List<BacktrackModule.QueuedPacket> arraylist = new ArrayList<>(this.packetQueue);
        this.packetQueue.clear();

        for (BacktrackModule.QueuedPacket backtrackmodule$queuedpacket : arraylist) {
            handlePacket(backtrackmodule$queuedpacket.packet());
        }
    }

    public void flushExpiredPackets(long delayMs) {
        List<BacktrackModule.QueuedPacket> arraylist = new ArrayList<>();
        Iterator iterator = this.packetQueue.iterator();

        while (iterator.hasNext()) {
            BacktrackModule.QueuedPacket backtrackmodule$queuedpacket = (BacktrackModule.QueuedPacket)iterator.next();
            if (backtrackmodule$queuedpacket.timestamp() <= delayMs) {
                arraylist.add(backtrackmodule$queuedpacket);
                iterator.remove();
            }
        }

        for (BacktrackModule.QueuedPacket backtrackmodule$queuedpacket1 : arraylist) {
            handlePacket(backtrackmodule$queuedpacket1.packet());
        }
    }

    public static void handlePacket(Packet packet) {
        if (mc.getConnection() != null) {
            packet.handle(mc.getConnection());
        }
    }

    public long rollDelayMs() {
        return randomBetween(this.minDelay, this.maxDelay);
    }

    public static long randomBetween(NumberProperty property, NumberProperty property2) {
        long i = (long)Math.min(property.getValue(), property2.getValue());
        long j = (long)Math.max(property.getValue(), property2.getValue());
        return i == j ? i : ThreadLocalRandom.current().nextLong(i, j + 1L);
    }

    public boolean rollChance() {
        float f = this.chance.getFloatValue();
        if (f <= 0.0F) {
            return false;
        } else {
            return f >= 100.0F ? true : ThreadLocalRandom.current().nextFloat(100.0F) < f;
        }
    }

    private static final class Cooldown {
        public long lastTimeMs;

        public Cooldown() {
        }

        public boolean hasElapsed(long delayMs) {
            return this.lastTimeMs + delayMs < System.currentTimeMillis();
        }

        public void reset() {
            this.lastTimeMs = System.currentTimeMillis();
        }
    }

    public record EspData(Entity entity, Vec3 pos, int boxFill, int boxOutline) {
    }

    private record QueuedPacket(Packet<?> packet, long timestamp) {
    }

    private static final class ServerPosTracker {
        public final VecDeltaCodec codec = new VecDeltaCodec();

        public ServerPosTracker() {
            this.codec.setBase(Vec3.ZERO);
        }

        public Vec3 getPos() {
            return this.codec.getBase();
        }

        public void setPos(Vec3 pos) {
            this.codec.setBase(pos);
        }

        public void initFromEntity(Entity entity) {
            this.codec.setBase(entity.getPositionCodec().getBase());
        }

        public Vec3 updateFromPacket(Packet packet, Entity entity) {
            Vec3 vec3;
            if (packet instanceof ClientboundMoveEntityPacket clientboundmoveentitypacket
                && clientboundmoveentitypacket.getEntity(BacktrackModule.mc.level) == entity) {
                vec3 = clientboundmoveentitypacket.getPositionDelta().decode(this.codec).endPosition();
            } else if (packet instanceof ClientboundTeleportEntityPacket clientboundteleportentitypacket && clientboundteleportentitypacket.id() == entity.getId()) {
                vec3 = clientboundteleportentitypacket.change().position();
            } else {
                if (!(packet instanceof ClientboundEntityPositionSyncPacket clientboundentitypositionsyncpacket)
                    || clientboundentitypositionsyncpacket.id() != entity.getId()) {
                    return null;
                }

                vec3 = clientboundentitypositionsyncpacket.position().endPosition();
            }

            this.codec.setBase(vec3);
            return vec3;
        }
    }
}
