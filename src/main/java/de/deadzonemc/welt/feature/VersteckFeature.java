package de.deadzonemc.welt.feature;

import de.deadzonemc.welt.Bau;
import de.deadzonemc.welt.Verstecke;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Baut das Versteck, falls eines in diesem Chunk liegt: auf freiem Boden vergraben unter einer Markierung,
 * auf Dächern oder Straßen offen hingestellt.
 */
public class VersteckFeature extends Feature<NoneFeatureConfiguration> {

    public VersteckFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        ChunkPos chunk = new ChunkPos(ctx.origin());
        BlockPos ziel = null;
        for (BlockPos p : Verstecke.umkreis(level.getSeed(), chunk.getMiddleBlockX(), chunk.getMiddleBlockZ(), 1)) {
            if (p.getX() >> 4 == chunk.x && p.getZ() >> 4 == chunk.z) {
                ziel = p;
                break;
            }
        }
        if (ziel == null) {
            return false;
        }
        int x = ziel.getX();
        int z = ziel.getZ();
        int y = level.getHeight(level instanceof net.minecraft.server.level.WorldGenRegion ? Heightmap.Types.OCEAN_FLOOR_WG : Heightmap.Types.OCEAN_FLOOR, x, z);
        BlockState boden = level.getBlockState(new BlockPos(x, y - 1, z));
        long saat = level.getSeed() ^ ziel.asLong();
        BlockState kiste = Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING,
                net.minecraft.core.Direction.Plane.HORIZONTAL.getRandomDirection(ctx.random()));
        boolean natur = boden.is(BlockTags.DIRT) || boden.is(BlockTags.SAND) || boden.is(Blocks.GRAVEL)
                || boden.is(BlockTags.BASE_STONE_OVERWORLD) || boden.is(Blocks.MUD) || boden.is(Blocks.CLAY);
        if (natur) {
            // vergraben: Kiste zwei Blöcke tief, darüber Erde, oben eine Markierung
            Bau.kiste(level, null, x, y - 2, z, kiste, Bau.beute("versteck"), saat);
            Bau.setzen(level, null, x, y - 1, z, Blocks.COARSE_DIRT.defaultBlockState());
            int art = Math.floorMod((int) saat, 3);
            if (level.getFluidState(new BlockPos(x, y, z)).isEmpty()) {
                switch (art) {
                    case 0 -> { // Steinmännchen
                        Bau.setzen(level, null, x + 1, y, z, Blocks.COBBLESTONE.defaultBlockState());
                        Bau.setzen(level, null, x + 1, y + 1, z, Blocks.COBBLESTONE_WALL.defaultBlockState());
                        Bau.setzen(level, null, x + 1, y + 2, z, Blocks.MOSSY_COBBLESTONE_WALL.defaultBlockState());
                    }
                    case 1 -> { // Pfahl mit rotem Tuch
                        Bau.setzen(level, null, x, y, z + 1, Blocks.SPRUCE_FENCE.defaultBlockState());
                        Bau.setzen(level, null, x, y + 1, z + 1, Blocks.SPRUCE_FENCE.defaultBlockState());
                        Bau.setzen(level, null, x, y + 2, z + 1, Blocks.RED_CARPET.defaultBlockState());
                    }
                    default -> { // Kreuz aus Kies
                        for (int d = -1; d <= 1; d++) {
                            Bau.setzen(level, null, x + d, y - 1, z + d, Blocks.GRAVEL.defaultBlockState());
                            Bau.setzen(level, null, x + d, y - 1, z - d, Blocks.GRAVEL.defaultBlockState());
                        }
                    }
                }
            }
        } else {
            // Auf einem Dach oder einer Straße: offen versteckt in einem Fass
            int oben = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            Bau.kiste(level, null, x, oben, z, Blocks.BARREL.defaultBlockState(), Bau.beute("versteck"), saat);
            Bau.setzen(level, null, x, oben + 1, z, Blocks.RED_CARPET.defaultBlockState());
        }
        return true;
    }
}
