package dev.adapt.v3.feature.module.impl.world.scaffold;

import dev.adapt.v3.event.impl.BlockPlaceEvent;
import dev.adapt.v3.event.impl.MouseInputEvent;
import dev.adapt.v3.event.impl.MoveInputEvent;
import dev.adapt.v3.event.impl.PostTickEvent;
import dev.adapt.v3.event.impl.PreTickEvent;
import dev.adapt.v3.event.impl.ReceivePacketEvent;
import dev.adapt.v3.event.system.Subscribe;
import dev.adapt.v3.feature.helper.mouse.MouseButton;
import dev.adapt.v3.feature.helper.mouse.MouseHelper;
import dev.adapt.v3.feature.helper.render.GizmoHelper;
import dev.adapt.v3.feature.helper.rotation.RotationHelper;
import dev.adapt.v3.feature.helper.simulation.SimulatedPlayer;
import dev.adapt.v3.feature.helper.slot.SlotHelper;
import dev.adapt.v3.feature.helper.swing.SwingHelper;
import dev.adapt.v3.feature.module.Module;
import dev.adapt.v3.feature.module.ModuleCategory;
import dev.adapt.v3.feature.module.impl.world.scaffold.mode.WatchdogScaffold;
import dev.adapt.v3.feature.module.mode.ModuleMode;
import dev.adapt.v3.feature.property.impl.EnumProperty;
import dev.adapt.v3.mixin.LivingEntityAccessor;
import dev.adapt.v3.ui.theme.Theme;
import dev.adapt.v3.util.player.InventoryUtility;
import dev.adapt.v3.util.player.PlayerUtility;
import dev.adapt.v3.util.player.RaytraceUtility;
import dev.adapt.v3.util.player.RaytracedRotation;
import dev.adapt.v3.util.player.RotationUtility;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.network.protocol.game.ClientboundTakeItemEntityPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class ScaffoldModule extends Module {
    public final List<ModuleMode<?>> modes = new ArrayList<>();
    public final ScaffoldSettings settings = new ScaffoldSettings(this);
    public ScaffoldModule.BlockPlacement targetPlacement;
    public int keepY;
    public Vec3 savedPlayerPos;
    public RaytracedRotation targetRotation;
    public Map<Integer, Integer> predictedSlotCounts;
    public int lastPlaceTick;
    public static final int MAX_RAYTRACE_CANDIDATES = 16;
    public static final int SAMPLE_GRID_CAPACITY = 625;

    public ScaffoldModule() {
        super("Scaffold", "Automatically places blocks under you.", ModuleCategory.WORLD);
    }

    @SafeVarargs
    public final void registerModes(EnumProperty property, ModuleMode<?>... modes) {
        this.modes.addAll(Arrays.asList(modes));
        property.addListener((oldValue, newValue) -> {
            if (this.isEnabled()) {
                this.modes.forEach(ModuleMode::onDisable);
                this.enableActiveMode();
            }
        });
    }

    public ModuleMode getActiveMode() {
        for (ModuleMode modulemode : this.modes) {
            if (modulemode.getMode() == this.settings.getModeProperty().getValue()) {
                return modulemode;
            }
        }

        return null;
    }

    public WatchdogScaffold getWatchdogMode() {
        return this.getActiveMode() instanceof WatchdogScaffold watchdogscaffold ? watchdogscaffold : null;
    }

    public void enableActiveMode() {
        ModuleMode modulemode = this.getActiveMode();
        if (modulemode != null) {
            modulemode.onEnable();
        }
    }

    @Override
    public String getSuffix() {
        return this.settings.getModeProperty().getValue().getDisplayName();
    }

    @Override
    public void onDisable() {
        this.modes.forEach(ModuleMode::onDisable);
        this.predictedSlotCounts = null;
        this.lastPlaceTick = 0;
        this.targetPlacement = null;
        this.targetRotation = null;
        if (this.savedPlayerPos != null && mc.player != null) {
            mc.player.setPos(this.savedPlayerPos.x, this.savedPlayerPos.y, this.savedPlayerPos.z);
            this.savedPlayerPos = null;
        }
    }

    @Override
    public void onEnable() {
        this.enableActiveMode();
        this.targetPlacement = null;
        this.targetRotation = null;
        this.predictedSlotCounts = new HashMap<>();
        if (mc.player != null) {
            this.keepY = Mth.floor(mc.player.getY());
        }
    }

    @Subscribe
    public void onBlockPlaced(BlockPlaceEvent event) {
        if (mc.gameMode != null && !mc.gameMode.getPlayerMode().isCreative()) {
            int i = mc.player.getInventory().getSelectedSlot();
            this.predictedSlotCounts.put(i, this.predictedSlotCounts.getOrDefault(i, mc.player.getMainHandItem().getCount() + 1) - 1);
        }
    }

    @Subscribe
    public void onEndTick(PostTickEvent event) {
        if (mc.player != null && mc.level != null) {
            if (mc.hitResult instanceof BlockHitResult blockhitresult
                && this.targetRotation != null
                && this.settings.isBlockOverlay()
                && !mc.level.getBlockState(blockhitresult.getBlockPos()).isAir()) {
                GizmoHelper.drawFilledBlock(blockhitresult.getBlockPos(), getOverlayColor());
            }
        }
    }

    public static int getOverlayColor() {
        return 1056964608 | Theme.getAccentColor() & 16777215;
    }

    @Subscribe(priority = 1)
    public void onMoveInput(MoveInputEvent event) {
        boolean flag = event.getForward() != 0.0F || event.getSideways() != 0.0F;
        if (this.settings.isSameY() && this.settings.isAutoJump() && (this.getWatchdogMode() == null || flag)) {
            ((LivingEntityAccessor)mc.player).adapt$setNoJumpDelay(0);
            event.setJump(true);
        }
    }

    @Subscribe(priority = 1)
    public void onHandleKeybinds(MouseInputEvent event) {
        if (mc.player != null) {
            WatchdogScaffold watchdogscaffold = this.getWatchdogMode();
            if (watchdogscaffold == null || this.targetPlacement != null && watchdogscaffold.canRotate() && watchdogscaffold.canPlace()) {
                boolean flag = mc.player.getMainHandItem().getItem() instanceof BlockItem || mc.player.getOffhandItem().getItem() instanceof BlockItem;
                if (this.targetPlacement != null) {
                    if (this.targetRotation != null && this.settings.isOverrideRaycast()) {
                        mc.hitResult = this.targetRotation.hitResult();
                    }

                    Block block = PlayerUtility.getTargetedBlock();
                    if (!InventoryUtility.isInteractiveBlock(block) && flag) {
                        MouseButton mousebutton = MouseHelper.getUseButton();
                        mousebutton.click();
                        if (this.settings.getSwingModeProperty().getValue() == ScaffoldSettings.SwingMode.SERVER) {
                            mousebutton.setShouldSwing(false);
                        }

                        this.lastPlaceTick = mc.player.tickCount;
                        if (this.settings.isBlockOverlay()) {
                            GizmoHelper.getInstance()
                                .drawCuboid(new GizmoHelper.Cuboid(this.targetPlacement.blockWithDirection.blockPos, 0, getOverlayColor(), 300L));
                        }
                    }
                } else if (!flag || !this.tryClutchInteract()) {
                    MouseHelper.getUseButton().suppress();
                }

                if (this.savedPlayerPos != null) {
                    mc.player.setPos(this.savedPlayerPos.x, this.savedPlayerPos.y, this.savedPlayerPos.z);
                    this.savedPlayerPos = null;
                }
            } else {
                MouseHelper.getUseButton().suppress();
                if (this.savedPlayerPos != null) {
                    mc.player.setPos(this.savedPlayerPos.x, this.savedPlayerPos.y, this.savedPlayerPos.z);
                    this.savedPlayerPos = null;
                }
            }
        }
    }

    public boolean tryClutchInteract() {
        if (!(mc.hitResult instanceof BlockHitResult blockhitresult)) {
            return false;
        } else {
            BlockPos blockpos1 = blockhitresult.getBlockPos();
            Block block = mc.level.getBlockState(blockpos1).getBlock();
            if (InventoryUtility.isInteractiveBlock(block)) {
                return false;
            }

            InteractionHand interactionhand;
            BlockItem blockitem;
            if (mc.player.getMainHandItem().getItem() instanceof BlockItem blockitem2) {
                interactionhand = InteractionHand.MAIN_HAND;
                blockitem = blockitem2;
            } else {
                if (!(mc.player.getOffhandItem().getItem() instanceof BlockItem blockitem1)) {
                    return false;
                }

                interactionhand = InteractionHand.OFF_HAND;
                blockitem = blockitem1;
            }

            UseOnContext useoncontext = new UseOnContext(mc.player, interactionhand, blockhitresult);
            BlockPlaceContext blockplacecontext = blockitem.updatePlacementContext(new BlockPlaceContext(useoncontext));
            if (blockplacecontext == null) {
                return false;
            } else {
                BlockPos blockpos = blockpos1.relative(blockhitresult.getDirection());
                BlockState blockstate = block.getStateForPlacement(blockplacecontext);
                Block block1 = blockitem.getBlock();
                VoxelShape voxelshape = block1.defaultBlockState().getCollisionShape(mc.level, blockpos);
                if (voxelshape.isEmpty()) {
                    return false;
                } else {
                    AABB aabb = voxelshape.bounds().move(blockpos);
                    if (blockstate != null && (!blockstate.canSurvive(mc.level, blockpos1) || mc.player.getBoundingBox().intersects(aabb))) {
                        MouseHelper.getUseButton().click();
                        this.settings.getInteractCps().rollNextDelay();
                        SwingHelper.resetTimer();
                        return true;
                    } else {
                        return false;
                    }
                }
            }
        }
    }

    @Subscribe(priority = 1)
    public void onStartTick(PreTickEvent event) {
        if (mc.player != null && mc.level != null) {
            Vec3 vec3 = null;
            if (vec3 != null) {
                this.savedPlayerPos = mc.player.position();
                mc.player.setPos(mc.player.getX() + vec3.x, mc.player.getY() + vec3.y, mc.player.getZ() + vec3.z);
            }

            int i = this.findBlockSlot();
            if (i != -1 || mc.player.getOffhandItem().getItem() instanceof BlockItem blockitem && InventoryUtility.isValidPlaceableBlock(blockitem.getBlock())) {
                SlotHelper.SpoofMode slothelper$spoofmode = switch ((ScaffoldSettings.SwitchMode)this.settings.getSwitchModeProperty().getValue()) {
                    case NORMAL -> SlotHelper.SpoofMode.NONE;
                    case FULL -> SlotHelper.SpoofMode.FULL;
                    default -> SlotHelper.SpoofMode.DEFAULT;
                };
                SlotHelper.spoof(i).withMode(slothelper$spoofmode);
                WatchdogScaffold watchdogscaffold = this.getWatchdogMode();
                boolean flag = watchdogscaffold == null
                    ? mc.options.keyJump.isDown()
                    : this.settings.isAutoJump() && PlayerUtility.isPhysicalKeyDown(mc.options.keyJump);
                boolean flag1 = !this.settings.isSameY()
                    || mc.options.keyUse.isDown()
                    || flag
                    || mc.player.onGround()
                    || Math.abs(Math.floor(mc.player.getY() - this.keepY)) > 2.0;
                if (flag1) {
                    this.keepY = Mth.floor(mc.player.getY());
                }

                if (watchdogscaffold != null) {
                    this.tickWatchdog(watchdogscaffold, i);
                } else {
                    this.updatePlacementAndPredict();
                    if (mc.player.input.keyPresses.shift()
                        && (int)(mc.player.getY() + mc.player.getDeltaMovement().y()) == (int)mc.player.getY()
                        && !(mc.player.getDeltaMovement().y() >= 0.2)
                        && !flag1) {
                    }

                    if (this.targetRotation != null && (!this.settings.isSnapRotations() || this.targetPlacement != null && !mc.player.onGround())) {
                        this.applyRotation(this.targetRotation.rotation());
                    }
                }
            }
        } else {
            this.targetPlacement = null;
        }
    }

    public void applyRotation(Vec2 rotation) {
        RotationHelper.getUpdater().setRotation(rotation, this.settings.getRotationModel());
    }

    public void holdCurrentRotation() {
        RotationHelper.getUpdater().setRotation(getCurrentCameraRotation(), this.settings.getRotationModel());
    }

    public void tickWatchdog(WatchdogScaffold watchdogScaffold, int value) {
        watchdogScaffold.updateSlotSync(value);
        if (!watchdogScaffold.canRotate()) {
            this.targetPlacement = null;
            this.targetRotation = null;
            this.holdCurrentRotation();
        } else {
            this.updatePlacementAndPredict();
            if (this.targetRotation == null || this.settings.isSnapRotations() && this.targetPlacement == null) {
                this.holdCurrentRotation();
            } else {
                this.applyRotation(this.targetRotation.rotation());
            }
        }
    }

    public static Vec2 getCurrentCameraRotation() {
        return new Vec2(RotationHelper.getState().getCameraYaw(mc.player.getYRot()), RotationHelper.getState().getCameraPitch(mc.player.getXRot()));
    }

    public int getKeepY() {
        return this.keepY;
    }

    @Subscribe
    public void onReceivePacket(ReceivePacketEvent event) {
        if (event.getPacket() instanceof ClientboundTakeItemEntityPacket clientboundtakeitementitypacket
            && mc.player != null
            && this.predictedSlotCounts != null
            && clientboundtakeitementitypacket.getPlayerId() == mc.player.getId()) {
            int i = mc.player.getInventory().getSelectedSlot();
            this.predictedSlotCounts
                .put(
                    i,
                    this.predictedSlotCounts.getOrDefault(i, mc.player.getMainHandItem().getCount() - clientboundtakeitementitypacket.getAmount())
                        + clientboundtakeitementitypacket.getAmount()
                );
        }
    }

    public int findBlockSlot() {
        for (int i = 0; i < 9; i++) {
            ItemStack itemstack = mc.player.getInventory().getNonEquipmentItems().get(i);
            if (itemstack.getItem() instanceof BlockItem blockitem
                && this.predictedSlotCounts.getOrDefault(i, itemstack.getCount()) > 0
                && InventoryUtility.isValidPlaceableBlock(blockitem.getBlock())) {
                return i;
            }
        }

        return -1;
    }

    public boolean updatePlacementAndPredict() {
        this.targetPlacement = this.findCurrentPlacement();
        if (this.targetPlacement != null) {
            this.targetRotation = this.targetPlacement.rotation;
            return true;
        }

        SimulatedPlayer simulatedplayer = new SimulatedPlayer(mc.player);
        RemotePlayer remoteplayer = simulatedplayer.getSimulatedPlayer();

        for (int i = 0; i < 10; i++) {
            simulatedplayer.simulateTick();
            ScaffoldModule.BlockPlacement scaffoldmodule$blockplacement = this.findPlacementAt(remoteplayer.blockPosition().below(), remoteplayer);
            if (scaffoldmodule$blockplacement != null) {
                this.targetRotation = scaffoldmodule$blockplacement.rotation;
                break;
            }
        }

        return this.targetPlacement != null;
    }

    public ScaffoldModule.BlockPlacement findCurrentPlacement() {
        return this.findPlacementAt(mc.player.blockPosition().atY(this.keepY).below(), mc.player);
    }

    public ScaffoldModule.BlockPlacement findPlacementAt(BlockPos pos, Player player) {
        if (!mc.level.getBlockState(pos).canBeReplaced()) {
            return null;
        }

        Vec3 vec3 = player.position().add(0.0, player.getDimensions(Pose.STANDING).eyeHeight(), 0.0);
        List<ScaffoldModule.BlockWithDirection> arraylist = new ArrayList<>();
        MutableBlockPos mutableblockpos = new MutableBlockPos();
        byte b0 = 4;

        for (int i = -4; i <= 4; i++) {
            for (int j = -4; j <= 4; j++) {
                for (int k = -4; k <= 4; k++) {
                    mutableblockpos.set(pos.getX() + j, pos.getY() + i, pos.getZ() + k);
                    if (mc.level.getBlockState(mutableblockpos).canBeReplaced()) {
                        for (Direction direction : Direction.values()) {
                            BlockPos blockpos = mutableblockpos.relative(direction);
                            if (!mc.level.getBlockState(blockpos).canBeReplaced()) {
                                Direction direction1 = direction.getOpposite();
                                double d0 = blockpos.getX() + 0.5 + direction1.getStepX() * 0.5;
                                double d1 = blockpos.getY() + 0.5 + direction1.getStepY() * 0.5;
                                double d2 = blockpos.getZ() + 0.5 + direction1.getStepZ() * 0.5;
                                if (!(
                                    (vec3.x - d0) * direction1.getStepX() + (vec3.y - d1) * direction1.getStepY() + (vec3.z - d2) * direction1.getStepZ()
                                        <= 0.0
                                )) {
                                    arraylist.add(new ScaffoldModule.BlockWithDirection(blockpos.immutable(), direction1));
                                }
                            }
                        }
                    }
                }
            }
        }

        if (arraylist.isEmpty()) {
            return null;
        }

        double d3 = mc.player.getX();
        double d4 = mc.player.getZ();
        arraylist.sort(
            Comparator.<ScaffoldModule.BlockWithDirection>comparingDouble(blockWithDirection -> blockWithDirection.blockPos.relative(blockWithDirection.direction).distSqr(pos))
                .thenComparingInt(blockWithDirection2 -> blockWithDirection2.blockPos.relative(blockWithDirection2.direction).getY() == pos.getY() ? 0 : 1)
                .thenComparingDouble(blockWithDirection3 -> {
                    BlockPos blockpos1 = blockWithDirection3.blockPos.relative(blockWithDirection3.direction);
                    double d5 = blockpos1.getX() + 0.5 - d3;
                    double d6 = blockpos1.getZ() + 0.5 - d4;
                    return d5 * d5 + d6 * d6;
                })
        );
        int l = 0;

        for (ScaffoldModule.BlockWithDirection scaffoldmodule$blockwithdirection : arraylist) {
            if (l++ >= 16) {
                break;
            }

            Vec2 vec2 = this.targetRotation != null ? this.targetRotation.rotation() : RotationUtility.getPlayerRotation();
            RaytracedRotation raytracedrotation = raytraceFace(
                scaffoldmodule$blockwithdirection.blockPos, scaffoldmodule$blockwithdirection.direction, vec2, vec3
            );
            if (raytracedrotation != null) {
                return new ScaffoldModule.BlockPlacement(scaffoldmodule$blockwithdirection, raytracedrotation);
            }
        }

        return null;
    }

    public static RaytracedRotation raytraceFace(BlockPos pos, Direction direction, Vec2 rotation, Vec3 vec) {
        double d0 = pos.getX() + 0.5 + direction.getStepX() * 0.5;
        double d1 = pos.getY() + 0.5 + direction.getStepY() * 0.5;
        double d2 = pos.getZ() + 0.5 + direction.getStepZ() * 0.5;
        Axis axis = direction.getAxis();
        double d3 = 0.42;
        double d4 = 0.035;
        double d5 = mc.player.blockInteractionRange();
        List<Vec2> arraylist = new ArrayList<>(625);

        for (double d6 = -0.42; d6 <= 0.420000001; d6 += 0.035) {
            for (double d7 = -0.42; d7 <= 0.420000001; d7 += 0.035) {
                Vec3 vec3 = switch (axis) {
                    case X -> new Vec3(d0, d1 + d6, d2 + d7);
                    case Y -> new Vec3(d0 + d6, d1, d2 + d7);
                    case Z -> new Vec3(d0 + d6, d1 + d7, d2);
                };
                arraylist.add(RotationUtility.fixRotation(RotationUtility.getRotationsBetween(vec, vec3)));
            }
        }

        arraylist.sort(Comparator.comparingDouble(rotation2 -> RotationUtility.getAngleDifference(rotation2, rotation)));

        for (Vec2 vec2 : arraylist) {
            if (RaytraceUtility.raytraceBlockFromVec(d5, false, vec2.x, vec2.y, vec) instanceof BlockHitResult blockhitresult
                && blockhitresult.getType() == Type.BLOCK
                && pos.equals(blockhitresult.getBlockPos())
                && blockhitresult.getDirection() == direction
                && isWithinFaceMargin(blockhitresult.getLocation(), pos, axis, 0.06)) {
                return new RaytracedRotation(vec2, blockhitresult);
            }
        }

        return null;
    }

    public static boolean isWithinFaceMargin(Vec3 vec, BlockPos pos, Axis axis, double value) {
        double d0 = vec.x - pos.getX();
        double d1 = vec.y - pos.getY();
        double d2 = vec.z - pos.getZ();
        boolean flag = axis == Axis.X || d0 > value && d0 < 1.0 - value;
        boolean flag1 = axis == Axis.Y || d1 > value && d1 < 1.0 - value;
        boolean flag2 = axis == Axis.Z || d2 > value && d2 < 1.0 - value;
        return flag && flag1 && flag2;
    }

    public ScaffoldSettings getSettings() {
        return this.settings;
    }

    public int getLastPlaceTick() {
        return this.lastPlaceTick;
    }

    public record BlockPlacement(ScaffoldModule.BlockWithDirection blockWithDirection, RaytracedRotation rotation) {
    }

    public record BlockWithDirection(BlockPos blockPos, Direction direction) {
    }
}
