package dev.adapt.v3.feature.module.impl.combat.velocity.mode;

import com.mojang.blaze3d.platform.InputConstants;
import dev.adapt.v3.event.impl.ChatEvent;
import dev.adapt.v3.event.impl.MoveInputEvent;
import dev.adapt.v3.event.impl.PostMovementPacketEvent;
import dev.adapt.v3.event.impl.PreInputEvent;
import dev.adapt.v3.event.impl.ReceivePacketEvent;
import dev.adapt.v3.event.system.Subscribe;
import dev.adapt.v3.feature.module.impl.combat.velocity.PacketAttackHelper;
import dev.adapt.v3.feature.module.impl.combat.velocity.PlayerRaycastHelper;
import dev.adapt.v3.feature.module.impl.combat.velocity.VelocityContext;
import dev.adapt.v3.feature.module.impl.combat.velocity.VelocityMode;
import dev.adapt.v3.feature.module.impl.combat.velocity.VelocityModule;
import dev.adapt.v3.feature.property.impl.NumberProperty;
import dev.adapt.v3.mixin.LocalPlayerAccessor;
import java.util.concurrent.LinkedBlockingDeque;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundAnimatePacket;
import net.minecraft.network.protocol.game.ClientboundContainerClosePacket;
import net.minecraft.network.protocol.game.ClientboundHurtAnimationPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerChatPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerCombatKillPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ClientboundSetHealthPacket;
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSoundEntityPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public final class HypixelVelocity extends VelocityMode {
    public static final Minecraft mc = Minecraft.getInstance();
    public final NumberProperty delayTicks = (NumberProperty)new NumberProperty("Delay Ticks", 0.0, 0.0, 10.0, 1.0)
        .hideIf(() -> this.module.getCurrentMode() != this);
    public static final LinkedBlockingDeque<Packet<?>> delayedPackets = new LinkedBlockingDeque<>();
    public static boolean forcedForwardKey;
    public static boolean delayingVelocity;
    public static boolean forceSprintInput;
    public static boolean reducing;
    public static boolean jumpResetting;
    public static int reduceDurationTicks;
    public static int delayCounter = -1;
    public static int activeTicks = -1;
    public static Vec2 knockbackRotation;

    public HypixelVelocity(VelocityModule velocityModule) {
        super(velocityModule);
        velocityModule.addProperties(this.delayTicks);
        PacketAttackHelper.init();
    }

    @Subscribe
    public void onReceivePacket(ReceivePacketEvent event) {
        if (mc.player != null && mc.level != null) {
            if (event.getPacket() instanceof ClientboundSetEntityMotionPacket clientboundsetentitymotionpacket
                && clientboundsetentitymotionpacket.id() == mc.player.getId()
                && !this.getContext().shouldSkipVelocity()) {
                Vec3 vec31 = clientboundsetentitymotionpacket.movement();
                double d0 = vec31.x;
                double d1 = vec31.z;
                if (vec31.y <= 0.0 || Math.sqrt(d0 * d0 + d1 * d1) < 0.1 || mc.player.onClimbable() || mc.player.isInWater() || mc.player.isInLava()) {
                    return;
                }

                Vec3 vec3 = new Vec3(-d0, 0.0, -d1).normalize();
                reduceDurationTicks = Mth.clamp((int)Math.round(6.43153527E-4 * Math.hypot((int)(d0 * 8000.0), (int)(d1 * 8000.0)) + 2.9419087136), 1, 10);
                Vec2 vec2 = PlayerRaycastHelper.getRotationsTo(mc.player.position().add(vec3.x, 0.0, vec3.z));
                knockbackRotation = new Vec2(vec2.x, this.getContext().getPitch());
                if (!delayingVelocity) {
                    if (this.delayTicks.getValue().intValue() > 0) {
                        delayingVelocity = true;
                        delayCounter = 0;
                    } else {
                        this.startJumpAndReduce();
                    }
                }
            }

            if (delayingVelocity && event.getPacket() instanceof ClientboundPlayerPositionPacket) {
                this.flushDelayedPackets();
            }

            if (delayingVelocity && !shouldBypassDelay(event.getPacket())) {
                event.cancel();
                delayedPackets.add(event.getPacket());
            }
        }
    }

    public static boolean shouldBypassDelay(Packet packet) {
        return packet instanceof ClientboundSetHealthPacket
            || packet instanceof ClientboundPlayerPositionPacket
            || packet instanceof ClientboundSoundPacket
            || packet instanceof ClientboundSoundEntityPacket
            || packet instanceof ClientboundPlayerChatPacket
            || packet instanceof ClientboundSystemChatPacket
            || packet instanceof ClientboundPlayerCombatKillPacket
            || packet instanceof ClientboundContainerClosePacket
            || packet instanceof ClientboundHurtAnimationPacket
            || packet instanceof ClientboundSetTitleTextPacket
            || packet instanceof ClientboundSetSubtitleTextPacket
            || packet instanceof ClientboundSetPlayerTeamPacket
            || packet instanceof ClientboundAnimatePacket clientboundanimatepacket && clientboundanimatepacket.getId() != mc.player.getId();
    }

    public static boolean wasSprinting() {
        return ((LocalPlayerAccessor)mc.player).adapt$getWasSprinting();
    }

    public static boolean isAttackablePlayerInRange(LivingEntity living, double range) {
        return living != null && living != mc.player && living instanceof Player && PlayerRaycastHelper.distanceToBox(living) <= range;
    }

    public LivingEntity findMeleeTarget() {
        VelocityContext velocitycontext = this.getContext();
        double d0 = velocitycontext.getAttackRange();
        if (velocitycontext.isKillAuraEnabled()) {
            LivingEntity livingentity1 = velocitycontext.getKillAuraTarget();
            return isAttackablePlayerInRange(livingentity1, d0) && velocitycontext.isCurrentTarget(livingentity1) ? livingentity1 : null;
        } else {
            EntityHitResult entityhitresult = PlayerRaycastHelper.raycastEntity(velocitycontext.getYaw(), velocitycontext.getPitch(), d0);
            return entityhitresult != null && entityhitresult.getEntity() instanceof LivingEntity livingentity && isAttackablePlayerInRange(livingentity, d0)
                ? livingentity
                : null;
        }
    }

    public boolean hasMeleeTarget() {
        VelocityContext velocitycontext = this.getContext();
        double d0 = velocitycontext.getAttackRange();
        if (velocitycontext.isKillAuraEnabled()) {
            return isAttackablePlayerInRange(velocitycontext.getKillAuraTarget(), d0);
        }

        EntityHitResult entityhitresult = PlayerRaycastHelper.raycastEntity(velocitycontext.getYaw(), velocitycontext.getPitch(), d0);
        return entityhitresult != null && entityhitresult.getEntity() instanceof LivingEntity livingentity && isAttackablePlayerInRange(livingentity, d0);
    }

    public LivingEntity findClosestPlayer(double value) {
        Player player = null;

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity instanceof Player player1 && player1 != mc.player) {
                double d0 = PlayerRaycastHelper.distanceToBox(player1);
                if (d0 <= value) {
                    value = d0;
                    player = player1;
                }
            }
        }

        return player;
    }

    public LivingEntity findFarPlayer() {
        Player player = null;
        double d0 = 0.0;

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity instanceof Player player1 && player1 != mc.player) {
                double d1 = PlayerRaycastHelper.distanceToBox(player1);
                if (d1 >= 11.0 && d1 > d0) {
                    d0 = d1;
                    player = player1;
                }
            }
        }

        return player;
    }

    public void applyKnockbackRotation() {
        LivingEntity livingentity = this.findClosestPlayer(this.getContext().getAttackRange());
        float f = livingentity != null ? PlayerRaycastHelper.getRotationsTo(PlayerRaycastHelper.closestPointOnBox(livingentity)).y : knockbackRotation.y;
        this.getContext().setRotation(knockbackRotation.x, f, 360.0F, 360.0F, VelocityContext.RotationMode.STRICT);
    }

    public void startJumpAndReduce() {
        if (PlayerRaycastHelper.isOnGround() && !this.getContext().isScaffoldEnabled()) {
            this.applyKnockbackRotation();
            forceSprintInput = true;
        }

        reducing = true;
        activeTicks = 0;
    }

    @Subscribe
    public void onPostMotion(PostMovementPacketEvent event) {
        if (delayingVelocity && mc.player != null && mc.level != null) {
            if (delayCounter >= this.delayTicks.getValue().intValue() || mc.player.onClimbable() || mc.player.isInWater() || mc.player.isInLava()) {
                this.flushDelayedPackets();
            }
        }
    }

    public void flushDelayedPackets() {
        delayingVelocity = false;
        delayCounter = -1;

        while (!delayedPackets.isEmpty()) {
            handlePacket(delayedPackets.poll());
        }

        this.startJumpAndReduce();
    }

    public static void handlePacket(Packet packet) {
        packet.handle(mc.getConnection());
    }

    public static boolean isPhysicalKeyDown(KeyMapping keyMapping) {
        return InputConstants.isKeyDown(keyMapping.getDefaultKey().getValue());
    }

    public void updateJumpAndForwardKeys() {
        if (!(mc.gui.screen() instanceof AbstractContainerScreen)) {
            if (activeTicks >= 0 && !this.getContext().isScaffoldEnabled()) {
                jumpResetting = true;
                if (activeTicks <= 2 && PlayerRaycastHelper.isOnGround()) {
                    mc.options.keyJump.setDown(true);
                }

                if (activeTicks <= 3) {
                    mc.options.keyUp.setDown(true);
                    forcedForwardKey = true;
                }
            }

            if (activeTicks >= 4 && activeTicks <= 9) {
                mc.options.keyJump.setDown(isPhysicalKeyDown(mc.options.keyJump));
                if (forcedForwardKey) {
                    mc.options.keyUp.setDown(isPhysicalKeyDown(mc.options.keyUp));
                    forcedForwardKey = false;
                }

                jumpResetting = false;
            }
        }
    }

    @Subscribe(priority = 1)
    public void onPreInput(PreInputEvent event) {
        if (mc.player != null && mc.level != null) {
            if (activeTicks >= 0) {
                activeTicks++;
            }

            if (delayCounter >= 0) {
                delayCounter++;
            }

            if (activeTicks >= 10) {
                reducing = false;
                forceSprintInput = false;
                activeTicks = -1;
            }

            if (activeTicks >= reduceDurationTicks) {
                reducing = false;
                forceSprintInput = false;
            }

            this.updateJumpAndForwardKeys();
            if (delayingVelocity || jumpResetting) {
                forceSprintInput = true;
            }

            LivingEntity livingentity = this.findMeleeTarget();
            LivingEntity livingentity1 = this.findFarPlayer();
            if ((livingentity != null || livingentity1 != null) && reducing) {
                forceSprintInput = true;
            }

            if (reducing && (mc.player.isSprinting() || wasSprinting())) {
                if (livingentity != null) {
                    PacketAttackHelper.sendReduceAttack(
                        this.getContext().isKillAuraEnabled() && livingentity == this.getContext().getKillAuraTarget()
                            ? PacketAttackHelper.SwingMode.PACKET_SWING
                            : PacketAttackHelper.SwingMode.PACKET_ONLY,
                        livingentity
                    );
                } else if (livingentity1 != null) {
                    PacketAttackHelper.sendReduceAttack(PacketAttackHelper.SwingMode.PACKET_ONLY, livingentity1);
                }
            }
        }
    }

    @Subscribe
    public void onMoveInput(MoveInputEvent event) {
        if (jumpResetting || delayingVelocity || forceSprintInput) {
            event.setForwardKey(true);
        }
    }

    @Subscribe
    public void onChat(ChatEvent event) {
        String s = event.getMessage().getString();
        if (activeTicks >= 0 && s.contains("failed") && s.contains("Hitboxes")) {
            event.cancel();
        }
    }

    @Override
    public Enum getMode() {
        return VelocityModule.Mode.HYPIXEL;
    }
}
