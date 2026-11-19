package dev.adapt.v3.feature.module.impl.combat;

import dev.adapt.v3.Adapt;
import dev.adapt.v3.event.impl.MouseInputEvent;
import dev.adapt.v3.event.impl.PreTickEvent;
import dev.adapt.v3.event.impl.SendPacketEvent;
import dev.adapt.v3.event.system.Subscribe;
import dev.adapt.v3.feature.helper.mouse.MouseHelper;
import dev.adapt.v3.feature.helper.projectile.Box;
import dev.adapt.v3.feature.helper.slot.SlotHelper;
import dev.adapt.v3.feature.helper.target.TargetHelper;
import dev.adapt.v3.feature.helper.target.TargetProperties;
import dev.adapt.v3.feature.helper.target.impl.LivingTarget;
import dev.adapt.v3.feature.helper.target.impl.PlayerTarget;
import dev.adapt.v3.feature.module.Module;
import dev.adapt.v3.feature.module.ModuleCategory;
import dev.adapt.v3.feature.module.impl.world.scaffold.ScaffoldModule;
import dev.adapt.v3.feature.property.impl.BooleanProperty;
import dev.adapt.v3.feature.property.impl.EnumProperty;
import dev.adapt.v3.feature.property.impl.NumberProperty;
import dev.adapt.v3.mixin.KeyMappingAccessor;
import dev.adapt.v3.util.Displayable;
import dev.adapt.v3.util.PacketUtility;
import dev.adapt.v3.util.player.InventoryUtility;
import dev.adapt.v3.util.player.PlayerUtility;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.protocol.Packet;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;

public final class BlockModule extends Module {
    public final BooleanProperty autoBlock = new BooleanProperty("Auto Block", false);
    public final EnumProperty mode = (EnumProperty)new EnumProperty<>("Mode", BlockModule.Mode.ALWAYS).hideIf(() -> !this.autoBlock.getValue());
    public final NumberProperty range = (NumberProperty)new NumberProperty("Range", 3.0, 3.0, 8.0, 0.1)
        .hideIf(() -> !this.autoBlock.getValue() || this.isLegitMode());
    public final BooleanProperty requireAttackKey = (BooleanProperty)new BooleanProperty("Require Attack Key", false).hideIf(() -> !this.autoBlock.getValue());
    public final BooleanProperty optimalTiming = (BooleanProperty)new BooleanProperty("Optimal Timing", true)
        .hideIf(() -> !this.autoBlock.getValue() || !this.isLegitMode());
    public final BooleanProperty blink = (BooleanProperty)new BooleanProperty("Blink", false).hideIf(() -> !this.autoBlock.getValue() || !this.isLegitMode());
    public final BooleanProperty allowSwinging = new BooleanProperty("Allow Swinging", false);
    public final TargetProperties targetSettings;
    public boolean alwaysBlocking;
    public static final double AIM_BOX_EXPAND = 0.4;
    public final Box playerBox = new Box();
    public final Box expandedPlayerBox = new Box();
    public boolean holdingUse;
    public boolean legitBlocking;
    public boolean unblockingForAttack;
    public boolean wasUnblockingForAttack;
    public boolean suppressManualUse;
    public boolean queuedAttackClick;
    public final List<Packet<?>> blinkedPackets = new ArrayList<>();
    public boolean blinking;

    public BlockModule() {
        super("Block", "Edits blocking actions or blocks for you automatically", ModuleCategory.COMBAT);
        this.addProperties(this.autoBlock, this.mode, this.range, this.requireAttackKey, this.optimalTiming, this.blink, this.allowSwinging);
        this.targetSettings = new TargetProperties(this, true, false, false, true, true, true);
    }

    public boolean isLegitMode() {
        return this.mode.getValue() == BlockModule.Mode.LEGIT;
    }

    public boolean isLegitAutoBlockActive() {
        return this.isEnabled() && this.autoBlock.getValue() && this.isLegitMode();
    }

    public boolean isBlinkActive() {
        return this.isLegitAutoBlockActive() && this.blink.getValue();
    }

    @Override
    public String getSuffix() {
        return this.autoBlock.getValue() ? ((BlockModule.Mode)this.mode.getValue()).getDisplayName() : null;
    }

    @Subscribe(priority = 2)
    public void onHandleInputAlways(MouseInputEvent event) {
        this.alwaysBlocking = false;
        if (mc.player != null) {
            if (!isScaffoldEnabled()) {
                TargetHelper targethelper = TargetHelper.getInstance();
                SlotHelper slothelper = SlotHelper.getInstance();
                ItemStack itemstack = slothelper.getMode() == SlotHelper.SpoofMode.FULL
                    ? slothelper.getVisualMainHandItem(mc.player)
                    : mc.player.getMainHandItem();
                if (targethelper != null
                    && this.autoBlock.getValue()
                    && this.mode.getValue() == BlockModule.Mode.ALWAYS
                    && (itemstack.is(ItemTags.SWORDS) || mc.player.getOffhandItem().getItem() instanceof ShieldItem)) {
                    if (!this.requireAttackKey.getValue() || mc.options.keyAttack.isDown()) {
                        for (LivingTarget livingtarget : targethelper.getTargets(this.targetSettings.getFlags(), LivingTarget.class)) {
                            if (!livingtarget.isLocalPlayer()) {
                                LivingEntity livingentity = livingtarget.getEntity();
                                if (PlayerUtility.getDistanceToEntityBox(livingentity) <= this.range.getValue()) {
                                    Block block = PlayerUtility.getTargetedBlock();
                                    if (InventoryUtility.isInteractiveBlock(block)) {
                                        return;
                                    }

                                    MouseHelper.getUseButton().click();
                                    this.alwaysBlocking = true;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Subscribe(priority = 1)
    public void onPreTick(PreTickEvent event) {
        if (this.isLegitMode() && this.autoBlock.getValue()) {
            this.tickLegitBlock();
        } else {
            this.resetLegitState();
        }
    }

    @Override
    public void onDisable() {
        this.alwaysBlocking = false;
        this.resetLegitState();
    }

    public void tickLegitBlock() {
        this.wasUnblockingForAttack = this.unblockingForAttack;
        this.unblockingForAttack = false;
        boolean flag = mc.player != null
            && mc.level != null
            && mc.gui.screen() == null
            && !isScaffoldEnabled()
            && !isAutoProjectileActive()
            && this.canBlockWithHeldItems()
            && (!this.requireAttackKey.getValue() || mc.options.keyAttack.isDown())
            && this.hasThreateningEnemy();
        boolean flag1 = !this.wasUnblockingForAttack && flag;
        this.legitBlocking = this.holdingUse || flag1;
        this.suppressManualUse = flag && !flag1;
        if (this.isBlinkActive() && flag && !flag1) {
            this.blinking = true;
        } else {
            this.flushBlinkPackets();
        }

        this.setHoldingUse(flag1);
    }

    @Subscribe(priority = -10)
    public void onHandleInputLegit(MouseInputEvent event) {
        if (this.isLegitAutoBlockActive() && mc.player != null) {
            if (this.wasUnblockingForAttack && this.queuedAttackClick) {
                MouseHelper.getAttackButton().click();
            }

            this.queuedAttackClick = false;
            if (this.legitBlocking && !this.unblockingForAttack && mc.player.isUsingItem()) {
                boolean flag = MouseHelper.getAttackButton().hasPendingClick();
                boolean flag1 = ((KeyMappingAccessor)mc.options.keyAttack).adapt$getClickCount() > 0;
                if (flag || flag1) {
                    this.unblockingForAttack = true;
                    this.queuedAttackClick = flag;
                    this.setHoldingUse(false);
                    if (this.isBlinkActive()) {
                        this.blinking = true;
                    }
                }
            }
        } else {
            this.queuedAttackClick = false;
        }
    }

    @Subscribe(priority = -1000)
    public void onSendPacket(SendPacketEvent event) {
        if (!event.isCancelled() && mc.player != null) {
            if (this.blinking && this.isBlinkActive()) {
                event.cancel();
                this.blinkedPackets.add(event.getPacket());
            }
        }
    }

    public void flushBlinkPackets() {
        this.blinking = false;
        if (!this.blinkedPackets.isEmpty()) {
            List<Packet<?>> arraylist = new ArrayList<>(this.blinkedPackets);
            this.blinkedPackets.clear();
            if (mc.getConnection() != null) {
                for (Packet<?> packet : arraylist) {
                    PacketUtility.sendNoEvent(packet);
                }
            }
        }
    }

    public void setHoldingUse(boolean holdingUse) {
        if (holdingUse != this.holdingUse) {
            this.holdingUse = holdingUse;
            if (holdingUse) {
                MouseHelper.getUseButton().click();
            }
        }
    }

    public void resetLegitState() {
        this.flushBlinkPackets();
        this.holdingUse = false;
        this.legitBlocking = false;
        this.unblockingForAttack = false;
        this.wasUnblockingForAttack = false;
        this.suppressManualUse = false;
        this.queuedAttackClick = false;
    }

    public boolean canBlockWithHeldItems() {
        return SlotHelper.getInstance().getVisualMainHandItem(mc.player).is(ItemTags.SWORDS) || mc.player.getOffhandItem().getItem() instanceof ShieldItem;
    }

    public static boolean isAutoProjectileActive() {
        AutoProjectileModule autoprojectilemodule = Adapt.getInstance().getModuleManager().getByClass(AutoProjectileModule.class);
        return autoprojectilemodule != null && autoprojectilemodule.isActive();
    }

    public boolean hasThreateningEnemy() {
        boolean flag = this.optimalTiming.getValue();
        AABB aabb = mc.player.getBoundingBox();
        this.playerBox.set(aabb.minX, aabb.minY, aabb.minZ, aabb.maxX, aabb.maxY, aabb.maxZ);
        if (flag) {
            this.expandedPlayerBox
                .set(this.playerBox.minX, this.playerBox.minY, this.playerBox.minZ, this.playerBox.maxX, this.playerBox.maxY, this.playerBox.maxZ)
                .inflate(0.4);
        }

        for (PlayerTarget playertarget : TargetHelper.getInstance().getTargets(this.targetSettings.getFlags(), PlayerTarget.class)) {
            if (!playertarget.isLocalPlayer()) {
                Player player = (Player)playertarget.getEntity();
                if (player.isAlive() && !player.isSpectator()) {
                    double d0 = player.entityInteractionRange();
                    if (this.playerBox.distanceTo(player.getX(), player.getEyeY(), player.getZ()) <= d0 && (!flag || this.isEnemyAimingAtUs(player, d0))) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public boolean isEnemyAimingAtUs(Player player, double value) {
        double d0 = Math.toRadians(player.getYRot());
        double d1 = Math.toRadians(player.getXRot());
        double d2 = Math.cos(d1);
        double d3 = this.expandedPlayerBox.intersectRay(player.getX(), player.getEyeY(), player.getZ(), -Math.sin(d0) * d2, -Math.sin(d1), Math.cos(d0) * d2);
        return d3 >= 0.0 && d3 <= value;
    }

    public static boolean shouldForceUseDown() {
        BlockModule blockmodule = getInstance();
        return blockmodule != null && blockmodule.isLegitAutoBlockActive() && blockmodule.holdingUse;
    }

    public static boolean shouldSuppressUseDown() {
        BlockModule blockmodule = getInstance();
        return blockmodule != null
            && blockmodule.isLegitAutoBlockActive()
            && (blockmodule.unblockingForAttack || blockmodule.wasUnblockingForAttack || blockmodule.suppressManualUse);
    }

    public static boolean shouldCancelAttackClick() {
        BlockModule blockmodule = getInstance();
        return blockmodule != null && blockmodule.isLegitAutoBlockActive() && blockmodule.unblockingForAttack;
    }

    public static boolean isLegitBlocking() {
        BlockModule blockmodule = getInstance();
        return blockmodule != null && blockmodule.isLegitAutoBlockActive() && blockmodule.legitBlocking;
    }

    public static BlockModule getInstance() {
        Adapt adapt = Adapt.getInstance();
        return adapt == null ? null : adapt.getModuleManager().getByClass(BlockModule.class);
    }

    public static boolean isScaffoldEnabled() {
        ScaffoldModule scaffoldmodule = Adapt.getInstance().getModuleManager().getByClass(ScaffoldModule.class);
        return scaffoldmodule != null && scaffoldmodule.isEnabled();
    }

    public boolean isAllowSwinging() {
        return this.allowSwinging.getValue();
    }

    public boolean isAlwaysBlocking() {
        return this.alwaysBlocking;
    }

    public static boolean canAttackWhileBlocking() {
        BlockModule blockmodule = getInstance();
        return blockmodule != null && blockmodule.isEnabled() && blockmodule.isAllowSwinging();
    }

    public enum Mode implements Displayable {
        ALWAYS("Always"),
        LEGIT("Legit");

        public final String displayName;

        Mode(String text) {
            this.displayName = text;
        }

        @Override
        public String getDisplayName() {
            return this.displayName;
        }

        @Override
        public String toString() {
            return this.displayName;
        }
    }
}
