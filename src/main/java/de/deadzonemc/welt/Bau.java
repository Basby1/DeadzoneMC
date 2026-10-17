package de.deadzonemc.welt;

import de.deadzonemc.DeadzoneMC;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.storage.loot.LootTable;

/** Kleine Helfer zum Bauen in der Weltgenerierung. */
public final class Bau {

    private Bau() {
    }

    public static final BlockState LUFT = Blocks.AIR.defaultBlockState();

    public static ResourceKey<LootTable> beute(String name) {
        return ResourceKey.create(Registries.LOOT_TABLE,
                ResourceLocation.fromNamespaceAndPath(DeadzoneMC.MODID, "kiste/" + name));
    }

    /** Setzt einen Block, aber nur innerhalb des gerade erzeugten Bereichs (null = überall). */
    public static void setzen(WorldGenLevel level, BoundingBox bereich, int x, int y, int z, BlockState zustand) {
        if (bereich != null && !bereich.isInside(x, y, z)) {
            return;
        }
        if (y <= level.getMinBuildHeight() || y >= level.getMaxBuildHeight()) {
            return;
        }
        level.setBlock(new BlockPos(x, y, z), zustand, Block.UPDATE_CLIENTS);
    }

    public static BlockState holen(WorldGenLevel level, int x, int y, int z) {
        return level.getBlockState(new BlockPos(x, y, z));
    }

    /** Stellt eine Kiste (oder ein Fass) mit Beute auf. */
    public static void kiste(WorldGenLevel level, BoundingBox bereich, int x, int y, int z, BlockState behaelter,
                             ResourceKey<LootTable> tabelle, long saat) {
        if (bereich != null && !bereich.isInside(x, y, z)) {
            return;
        }
        BlockPos pos = new BlockPos(x, y, z);
        level.setBlock(pos, behaelter, Block.UPDATE_CLIENTS);
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof RandomizableContainer behaelterInhalt) {
            behaelterInhalt.setLootTable(tabelle, saat);
        }
    }

    /** Fester Zufallswert für eine Position – überall gleich, egal in welchem Chunk gerade gebaut wird. */
    public static long mischen(long saat, int x, int y, int z) {
        long h = saat ^ (x * 0x9E3779B97F4A7C15L) ^ (y * 0xC2B2AE3D27D4EB4FL) ^ (z * 0x165667B19E3779F9L);
        h ^= (h >>> 31);
        h *= 0xBF58476D1CE4E5B9L;
        h ^= (h >>> 29);
        h *= 0x94D049BB133111EBL;
        h ^= (h >>> 32);
        return h;
    }

    /** Fester Zufallswert zwischen 0 (einschließlich) und 1 (ausschließlich). */
    public static double zufall(long saat, int x, int y, int z) {
        return (mischen(saat, x, y, z) >>> 11) * 0x1.0p-53;
    }

    public static boolean chance(long saat, int x, int y, int z, double wahrscheinlichkeit) {
        return zufall(saat, x, y, z) < wahrscheinlichkeit;
    }

    public static <T> T waehlen(RandomSource zufall, T[] auswahl) {
        return auswahl[zufall.nextInt(auswahl.length)];
    }

    /** Ob hier Boden ist, auf dem man etwas aufstellen kann. */
    public static boolean fest(BlockState zustand) {
        return zustand.isSolid() && zustand.getFluidState().isEmpty() && !zustand.is(net.minecraft.tags.BlockTags.LEAVES);
    }

    /** Ob man diesen Block beim Bauen einfach überschreiben darf (Luft, Gras, Blumen ...). */
    public static boolean ersetzbar(BlockState zustand) {
        return zustand.isAir() || (zustand.canBeReplaced() && zustand.getFluidState().isEmpty());
    }
}
