package dev.adapt.v3.feature.module.impl.world;

import dev.adapt.v3.event.impl.JoinWorldEvent;
import dev.adapt.v3.event.impl.PostTickEvent;
import dev.adapt.v3.event.impl.ReceivePacketEvent;
import dev.adapt.v3.event.system.Subscribe;
import dev.adapt.v3.feature.module.Module;
import dev.adapt.v3.feature.module.ModuleCategory;
import dev.adapt.v3.feature.property.impl.BooleanProperty;
import dev.adapt.v3.feature.property.impl.EnumProperty;
import dev.adapt.v3.feature.property.impl.MultiBooleanProperty;
import dev.adapt.v3.feature.property.impl.NumberProperty;
import dev.adapt.v3.util.Displayable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.phys.Vec3;

public final class XRayModule extends Module {
    public static final Map<Block, XRayModule.Ore> BLOCK_TO_ORE = new IdentityHashMap<>();
    public static final int MAX_RENDERED_ORES = 400;
    public static final int CHUNKS_PER_TICK = 6;
    public final EnumProperty<XRayModule.Mode> mode = new EnumProperty<>("Mode", XRayModule.Mode.NORMAL);
    public final MultiBooleanProperty ores = new MultiBooleanProperty(
        "Ores",
        "Diamond",
        true,
        "Gold",
        true,
        "Iron",
        false,
        "Emerald",
        false,
        "Redstone",
        false,
        "Lapis",
        false,
        "Coal",
        false,
        "Copper",
        false,
        "Ancient Debris",
        true,
        "Quartz",
        false
    );
    public final NumberProperty range = new NumberProperty("Range", "blocks", 48.0, 16.0, 128.0, 8.0);
    public final BooleanProperty exposedOnly = (BooleanProperty)new BooleanProperty("Exposed Only", false)
        .hideIf(() -> this.mode.getValue() == XRayModule.Mode.WATCHDOG);
    public final NumberProperty refresh = (NumberProperty)new NumberProperty("Refresh", "s", 3.0, 0.5, 5.0, 0.1)
        .hideIf(() -> this.mode.getValue() != XRayModule.Mode.WATCHDOG);
    public final BooleanProperty interact = (BooleanProperty)new BooleanProperty("Interact", false)
        .hideIf(() -> this.mode.getValue() != XRayModule.Mode.WATCHDOG);
    public final NumberProperty interactBlocks = (NumberProperty)new NumberProperty("Interact Blocks", 3.0, 1.0, 8.0, 1.0)
        .hideIf(() -> this.mode.getValue() != XRayModule.Mode.WATCHDOG || !this.interact.getValue());
    public final BooleanProperty interactEsp = (BooleanProperty)new BooleanProperty("Interact ESP", true)
        .hideIf(() -> this.mode.getValue() != XRayModule.Mode.WATCHDOG || !this.interact.getValue());
    public final NumberProperty opacity = new NumberProperty("Opacity", "%", 60.0, 10.0, 100.0, 5.0);
    public final BooleanProperty tracers = new BooleanProperty("Tracers", false);
    public final Map<Long, List<XRayModule.FoundOre>> chunkOres = new HashMap<>();
    public final Set<BlockPos> revealedPositions = new HashSet<>();
    public final ArrayDeque<long[]> chunkScanQueue = new ArrayDeque<>();
    public long lastScanTime;
    public static final double MAX_INTERACT_Y = 25.0;
    public static final double INTERACT_RADIUS = 4.5;
    public static final long INTERACT_COOLDOWN_MS = 15000L;
    public final ArrayDeque<BlockPos> interactQueue = new ArrayDeque<>();
    public final Map<BlockPos, Long> interactedBlocks = new HashMap<>();
    public final Map<BlockPos, Long> recentInteracts = new HashMap<>();
    public static final long INTERACT_ESP_FADE_MS = 1000L;
    public static final int INTERACT_ESP_COLOR = -1;
    public long lastInteractBatchTime;

    public XRayModule() {
        super("XRay", "Highlights ores through walls.", ModuleCategory.WORLD);
        this.addProperties(
            this.mode, this.ores, this.range, this.exposedOnly, this.refresh, this.interact, this.interactBlocks, this.interactEsp, this.opacity, this.tracers
        );
        this.mode.addListener((mode, mode2) -> this.reset());
        this.ores.addListener((map, map2) -> this.reset());
        this.exposedOnly.addListener((flag, flag2) -> this.reset());
    }

    @Override
    public void onEnable() {
        this.reset();
    }

    @Override
    public void onDisable() {
        this.reset();
    }

    @Subscribe
    public void onWorldChange(JoinWorldEvent event) {
        this.reset();
    }

    public void reset() {
        this.chunkOres.clear();
        this.chunkScanQueue.clear();
        this.interactQueue.clear();
        this.interactedBlocks.clear();
        this.recentInteracts.clear();
        this.revealedPositions.clear();
        this.lastScanTime = 0L;
        this.lastInteractBatchTime = 0L;
    }

    @Override
    public String getSuffix() {
        return this.mode.getValue().getDisplayName();
    }

    public boolean isOreEnabled(XRayModule.Ore ore) {
        return ore != null && this.ores.isEnabled(ore.displayName);
    }

    public boolean shouldCheckExposed() {
        return this.mode.getValue() == XRayModule.Mode.WATCHDOG || this.exposedOnly.getValue();
    }

    public void enqueueChunks() {
        if (this.chunkScanQueue.isEmpty()) {
            long i = this.mode.getValue() == XRayModule.Mode.WATCHDOG ? (long)(this.refresh.getValue() * 1000.0) : 1000L;
            long j = System.currentTimeMillis();
            if (j - this.lastScanTime >= i) {
                this.lastScanTime = j;
                int k = mc.player.chunkPosition().x();
                int l = mc.player.chunkPosition().z();
                int i1 = (int)Math.ceil(this.range.getValue() / 16.0);
                List<long[]> arraylist = new ArrayList<>();

                for (int j1 = -i1; j1 <= i1; j1++) {
                    for (int k1 = -i1; k1 <= i1; k1++) {
                        arraylist.add(new long[]{k + j1, l + k1, (long)j1 * j1 + (long)k1 * k1});
                    }
                }

                arraylist.sort(Comparator.comparingLong(longs -> longs[2]));
                this.chunkScanQueue.addAll(arraylist);
                this.chunkOres.keySet().removeIf(time -> {
                    int l1 = (int)(time >> 32);
                    int i2 = (int)time.longValue();
                    return Math.abs(l1 - k) > i1 + 1 || Math.abs(i2 - l) > i1 + 1;
                });
                this.revealedPositions.removeIf(pos -> Math.abs((pos.getX() >> 4) - k) > i1 + 1 || Math.abs((pos.getZ() >> 4) - l) > i1 + 1);
            }
        }
    }

    @Subscribe
    public void onReceivePacket(ReceivePacketEvent event) {
        if (this.mode.getValue() == XRayModule.Mode.WATCHDOG && mc.level != null) {
            if (event.getPacket() instanceof ClientboundBlockUpdatePacket clientboundblockupdatepacket) {
                this.handleBlockUpdate(clientboundblockupdatepacket.getPos(), clientboundblockupdatepacket.getBlockState());
            } else if (event.getPacket() instanceof ClientboundSectionBlocksUpdatePacket clientboundsectionblocksupdatepacket) {
                clientboundsectionblocksupdatepacket.runUpdates(this::handleBlockUpdate);
            }
        }
    }

    public void handleBlockUpdate(BlockPos pos, BlockState state) {
        if (this.isOreEnabled(BLOCK_TO_ORE.get(state.getBlock()))) {
            if (this.revealedPositions.add(pos.immutable())) {
                this.chunkScanQueue.addFirst(new long[]{pos.getX() >> 4, pos.getZ() >> 4, 0L});
            }
        } else {
            this.revealedPositions.remove(pos);
        }
    }

    public void processChunkQueue() {
        for (int i = 0; i < 6 && !this.chunkScanQueue.isEmpty(); i++) {
            long[] along = this.chunkScanQueue.poll();
            int j = (int)along[0];
            int k = (int)along[1];
            long l = (long)j << 32 | k & 4294967295L;
            if (!mc.level.hasChunk(j, k)) {
                this.chunkOres.remove(l);
            } else {
                this.chunkOres.put(l, this.scanChunk(mc.level.getChunk(j, k)));
            }
        }
    }

    public List<XRayModule.FoundOre> scanChunk(LevelChunk chunk) {
        List<XRayModule.FoundOre> arraylist = new ArrayList<>();
        LevelChunkSection[] alevelchunksection = chunk.getSections();
        boolean flag = this.shouldCheckExposed();
        int i = chunk.getPos().getMinBlockX();
        int j = chunk.getPos().getMinBlockZ();
        MutableBlockPos mutableblockpos = new MutableBlockPos();

        for (int k = 0; k < alevelchunksection.length; k++) {
            LevelChunkSection levelchunksection = alevelchunksection[k];
            if (levelchunksection != null
                && !levelchunksection.hasOnlyAir()
                && levelchunksection.maybeHas(state -> this.isOreEnabled(BLOCK_TO_ORE.get(state.getBlock())))) {
                int l = mc.level.getSectionYFromSectionIndex(k) << 4;

                for (int i1 = 0; i1 < 16; i1++) {
                    for (int j1 = 0; j1 < 16; j1++) {
                        for (int k1 = 0; k1 < 16; k1++) {
                            XRayModule.Ore xraymodule$ore = BLOCK_TO_ORE.get(levelchunksection.getBlockState(k1, i1, j1).getBlock());
                            if (this.isOreEnabled(xraymodule$ore)) {
                                BlockPos blockpos = new BlockPos(i + k1, l + i1, j + j1);
                                if (!flag || this.revealedPositions.contains(blockpos) || this.isExposed(blockpos, mutableblockpos)) {
                                    arraylist.add(new XRayModule.FoundOre(blockpos, xraymodule$ore));
                                }
                            }
                        }
                    }
                }
            }
        }

        return arraylist;
    }

    public boolean isExposed(BlockPos pos, MutableBlockPos mutablePos) {
        for (Direction direction : Direction.values()) {
            BlockState blockstate = mc.level.getBlockState(mutablePos.setWithOffset(pos, direction));
            if (!blockstate.isSolidRender()) {
                return true;
            }
        }

        return false;
    }

    public void enqueueInteractBlocks() {
        if (this.mode.getValue() == XRayModule.Mode.WATCHDOG && this.interact.getValue() && this.interactQueue.isEmpty()) {
            if (!(mc.player.getY() > 25.0) && mc.gameMode != null && !mc.gameMode.getPlayerMode().isCreative()) {
                long i = System.currentTimeMillis();
                if (i - this.lastInteractBatchTime >= (long)(this.refresh.getValue() * 1000.0)) {
                    this.lastInteractBatchTime = i;
                    this.interactedBlocks.values().removeIf(time -> i - time > 15000L);
                    Vec3 vec3 = mc.player.getEyePosition();
                    BlockPos blockpos = BlockPos.containing(vec3);
                    int j = (int)Math.ceil(4.5);
                    List<BlockPos> arraylist = new ArrayList<>();
                    List<BlockPos> arraylist1 = new ArrayList<>();

                    for (BlockPos blockpos1 : BlockPos.betweenClosed(blockpos.offset(-j, -j, -j), blockpos.offset(j, j, j))) {
                        if (!(Vec3.atCenterOf(blockpos1).distanceToSqr(vec3) > 20.25) && !this.interactedBlocks.containsKey(blockpos1)) {
                            BlockState blockstate = mc.level.getBlockState(blockpos1);
                            if (blockstate.isSolidRender() && !(blockstate.getDestroySpeed(mc.level, blockpos1) < 1.0F)) {
                                if (this.isOreEnabled(BLOCK_TO_ORE.get(blockstate.getBlock())) && !this.revealedPositions.contains(blockpos1)) {
                                    arraylist1.add(blockpos1.immutable());
                                } else {
                                    arraylist.add(blockpos1.immutable());
                                }
                            }
                        }
                    }

                    Collections.shuffle(arraylist);
                    Collections.shuffle(arraylist1);
                    arraylist.addAll(0, arraylist1);

                    for (int k = 0; k < Math.min(this.interactBlocks.getValue().intValue(), arraylist.size()); k++) {
                        this.interactQueue.add((BlockPos)arraylist.get(k));
                        this.interactedBlocks.put((BlockPos)arraylist.get(k), i);
                    }
                }
            }
        }
    }

    public void sendNextInteract() {
        if (!this.interactQueue.isEmpty() && mc.getConnection() != null && mc.gameMode != null && !mc.gameMode.isDestroying()) {
            BlockPos blockpos = this.interactQueue.poll();
            mc.getConnection().send(new ServerboundPlayerActionPacket(Action.START_DESTROY_BLOCK, blockpos, Direction.UP));
            mc.getConnection().send(new ServerboundPlayerActionPacket(Action.ABORT_DESTROY_BLOCK, blockpos, Direction.UP));
            this.recentInteracts.put(blockpos, System.currentTimeMillis());
            if (this.interactQueue.isEmpty()) {
                this.lastScanTime = System.currentTimeMillis() - (long)(this.refresh.getValue() * 1000.0) + 500L;
            }
        }
    }

    public void renderInteractEsp() {
        if (!this.recentInteracts.isEmpty()) {
            long i = System.currentTimeMillis();
            this.recentInteracts.values().removeIf(time -> i - time > 1000L);
            if (this.interactEsp.getValue() && this.mode.getValue() == XRayModule.Mode.WATCHDOG && this.interact.getValue()) {
                float f = this.opacity.getValue().floatValue() / 100.0F;
                int j = 16777215;

                try {
                    for (Entry entry : this.recentInteracts.entrySet()) {
                        float f1 = 1.0F - (float)(i - (Long)entry.getValue()) / 1000.0F;
                        int k = Math.round(f * f1 * 255.0F);
                        int l = Math.round(f * f1 * 0.3F * 255.0F);
                        Gizmos.cuboid((BlockPos)entry.getKey(), GizmoStyle.strokeAndFill(k << 24 | 16777215, 1.5F, l << 24 | 16777215)).setAlwaysOnTop();
                    }
                } catch (IllegalStateException illegalstateexception) {
                }
            }
        }
    }

    @Subscribe
    public void onEndTick(PostTickEvent event) {
        if (mc.player != null && mc.level != null) {
            this.enqueueInteractBlocks();
            this.sendNextInteract();
            this.enqueueChunks();
            this.processChunkQueue();
            this.renderInteractEsp();
            Vec3 vec3 = mc.player.getEyePosition();
            double d0 = this.range.getValue() * this.range.getValue();
            List<XRayModule.FoundOre> arraylist = new ArrayList<>();

            for (List<XRayModule.FoundOre> list : this.chunkOres.values()) {
                for (XRayModule.FoundOre xraymodule$foundore : list) {
                    if (xraymodule$foundore.pos.distToCenterSqr(vec3) <= d0) {
                        arraylist.add(xraymodule$foundore);
                    }
                }
            }

            arraylist.sort(Comparator.comparingDouble(foundOre -> foundOre.pos.distToCenterSqr(vec3)));
            float f = this.opacity.getValue().floatValue() / 100.0F;
            int j = Math.round(f * 0.35F * 255.0F);
            int k = Math.round(f * 255.0F);
            int l = 0;

            for (XRayModule.FoundOre xraymodule$foundore1 : arraylist) {
                if (l >= 400) {
                    break;
                }

                if (BLOCK_TO_ORE.get(mc.level.getBlockState(xraymodule$foundore1.pos).getBlock()) == xraymodule$foundore1.ore) {
                    int i = xraymodule$foundore1.ore.color & 16777215;

                    try {
                        Gizmos.cuboid(xraymodule$foundore1.pos, GizmoStyle.strokeAndFill(k << 24 | i, 1.5F, j << 24 | i)).setAlwaysOnTop();
                        if (this.tracers.getValue()) {
                            Gizmos.line(vec3, Vec3.atCenterOf(xraymodule$foundore1.pos), k << 24 | i, 1.5F).setAlwaysOnTop();
                        }
                    } catch (IllegalStateException illegalstateexception) {
                        return;
                    }

                    l++;
                }
            }
        }
    }

    static {
        for (XRayModule.Ore xraymodule$ore : XRayModule.Ore.values()) {
            for (Block block : xraymodule$ore.blocks) {
                BLOCK_TO_ORE.put(block, xraymodule$ore);
            }
        }
    }

    private record FoundOre(BlockPos pos, XRayModule.Ore ore) {
    }

    public enum Mode implements Displayable {
        NORMAL("Normal"),
        WATCHDOG("Watchdog");

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

    private enum Ore {
        DIAMOND("Diamond", -16725816, Blocks.DIAMOND_ORE, Blocks.DEEPSLATE_DIAMOND_ORE),
        GOLD("Gold", -8909, Blocks.GOLD_ORE, Blocks.DEEPSLATE_GOLD_ORE, Blocks.NETHER_GOLD_ORE),
        IRON("Iron", -2570056, Blocks.IRON_ORE, Blocks.DEEPSLATE_IRON_ORE),
        EMERALD("Emerald", -16725456, Blocks.EMERALD_ORE, Blocks.DEEPSLATE_EMERALD_ORE),
        REDSTONE("Redstone", -54742, Blocks.REDSTONE_ORE, Blocks.DEEPSLATE_REDSTONE_ORE),
        LAPIS("Lapis", -13606657, Blocks.LAPIS_ORE, Blocks.DEEPSLATE_LAPIS_ORE),
        COAL("Coal", -11513776, Blocks.COAL_ORE, Blocks.DEEPSLATE_COAL_ORE),
        COPPER("Copper", -2065841, Blocks.COPPER_ORE, Blocks.DEEPSLATE_COPPER_ORE),
        DEBRIS("Ancient Debris", -5212064, Blocks.ANCIENT_DEBRIS),
        QUARTZ("Quartz", -988186, Blocks.NETHER_QUARTZ_ORE);

        public final String displayName;
        public final int color;
        public final Block[] blocks;

        Ore(String text, int value, Block... blocks) {
            this.displayName = text;
            this.color = value;
            this.blocks = blocks;
        }
    }
}
