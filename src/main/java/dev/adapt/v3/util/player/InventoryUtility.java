package dev.adapt.v3.util.player;

import java.util.List;
import java.util.stream.IntStream;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.BeaconBlock;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BrewingStandBlock;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.CakeBlock;
import net.minecraft.world.level.block.CandleCakeBlock;
import net.minecraft.world.level.block.CartographyTableBlock;
import net.minecraft.world.level.block.CaveVinesBlock;
import net.minecraft.world.level.block.CaveVinesPlantBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.ChiseledBookShelfBlock;
import net.minecraft.world.level.block.CommandBlock;
import net.minecraft.world.level.block.ComparatorBlock;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.CraftingTableBlock;
import net.minecraft.world.level.block.DaylightDetectorBlock;
import net.minecraft.world.level.block.DecoratedPotBlock;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.DragonEggBlock;
import net.minecraft.world.level.block.EnchantingTableBlock;
import net.minecraft.world.level.block.EnderChestBlock;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.GrindstoneBlock;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.JigsawBlock;
import net.minecraft.world.level.block.JukeboxBlock;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.LoomBlock;
import net.minecraft.world.level.block.NoteBlock;
import net.minecraft.world.level.block.RedstoneWireBlock;
import net.minecraft.world.level.block.RepeaterBlock;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.SlimeBlock;
import net.minecraft.world.level.block.SmithingTableBlock;
import net.minecraft.world.level.block.StonecutterBlock;
import net.minecraft.world.level.block.StructureBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.WebBlock;
import net.minecraft.world.level.block.piston.MovingPistonBlock;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;

public final class InventoryUtility {
    public static final Minecraft mc = Minecraft.getInstance();
    public static List<Block> INVALID_BLOCKS = BuiltInRegistries.BLOCK
        .stream()
        .filter(
            block -> block instanceof TrapDoorBlock
                || block instanceof SweetBerryBushBlock
                || block instanceof AbstractFurnaceBlock
                || block instanceof SignBlock
                || block instanceof AnvilBlock
                || block instanceof BarrelBlock
                || block instanceof BeaconBlock
                || block instanceof BedBlock
                || block instanceof BellBlock
                || block instanceof BrewingStandBlock
                || block instanceof ButtonBlock
                || block instanceof CakeBlock
                || block instanceof CandleCakeBlock
                || block instanceof CartographyTableBlock
                || block instanceof CaveVinesPlantBlock
                || block instanceof CaveVinesBlock
                || block instanceof ChestBlock
                || block instanceof ChiseledBookShelfBlock
                || block instanceof CommandBlock
                || block instanceof ComparatorBlock
                || block instanceof ComposterBlock
                || block instanceof CraftingTableBlock
                || block instanceof DaylightDetectorBlock
                || block instanceof DecoratedPotBlock
                || block instanceof DispenserBlock
                || block instanceof DoorBlock
                || block instanceof DragonEggBlock
                || block instanceof EnchantingTableBlock
                || block instanceof EnderChestBlock
                || block instanceof FenceBlock
                || block instanceof FenceGateBlock
                || block instanceof FlowerPotBlock
                || block instanceof GrindstoneBlock
                || block instanceof HopperBlock
                || block instanceof JigsawBlock
                || block instanceof JukeboxBlock
                || block instanceof LecternBlock
                || block instanceof LeverBlock
                || block instanceof LightBlock
                || block instanceof LoomBlock
                || block instanceof NoteBlock
                || block instanceof MovingPistonBlock
                || block instanceof RedstoneWireBlock
                || block instanceof RepeaterBlock
                || block instanceof RespawnAnchorBlock
                || block instanceof ShulkerBoxBlock
                || block instanceof SmithingTableBlock
                || block instanceof StonecutterBlock
                || block instanceof FlowerBlock
                || block instanceof StructureBlock
                || block instanceof SlimeBlock
                || block instanceof WebBlock
        )
        .toList();

    public static int findHotbarItem(Item item) {
        return IntStream.range(0, 9).filter(value -> {
            ItemStack itemstack = mc.player.getInventory().getNonEquipmentItems().get(value);
            return itemstack.getItem() == item && itemstack.getCount() > 0;
        }).findFirst().orElse(-1);
    }

    public static boolean isInventoryFull() {
        return mc.player.getInventory().getNonEquipmentItems().stream().noneMatch(ItemStack::isEmpty);
    }

    public static boolean isValidPlaceableBlock(Block block) {
        return !isInteractiveBlock(block)
            && block.defaultBlockState().getShape(EmptyBlockGetter.INSTANCE, mc.player.blockPosition(), CollisionContext.of(mc.player)) == Shapes.block()
            && !(block instanceof TntBlock)
            && !(block instanceof FallingBlock);
    }

    public static boolean isInteractiveBlock(Block block) {
        return block != null && INVALID_BLOCKS.contains(block);
    }
}
