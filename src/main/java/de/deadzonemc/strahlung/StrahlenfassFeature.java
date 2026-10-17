package de.deadzonemc.strahlung;

import de.deadzonemc.ModRegistry;
import de.deadzonemc.welt.plan.PlanSpeicher;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** Eine kleine Gruppe strahlender Fässer auf verseuchtem, abgestorbenem Boden. */
public class StrahlenfassFeature extends Feature<NoneFeatureConfiguration> {

    public StrahlenfassFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        RandomSource z = ctx.random();
        BlockPos start = ctx.origin();
        if (PlanSpeicher.belegt(level.getSeed(), start.getX(), start.getZ(), 4)) {
            return false;
        }
        int anzahl = 1 + z.nextInt(4);
        boolean gesetzt = false;
        for (int i = 0; i < anzahl + 6; i++) {
            int x = start.getX() + z.nextInt(7) - 3;
            int zz = start.getZ() + z.nextInt(7) - 3;
            int y = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, zz);
            BlockPos pos = new BlockPos(x, y, zz);
            BlockPos unten = pos.below();
            if (!level.getBlockState(unten).isSolid() || !level.getFluidState(unten).isEmpty() || !level.isEmptyBlock(pos)) {
                continue;
            }
            if (i < anzahl) {
                level.setBlock(pos, ModRegistry.STRAHLENFASS.get().defaultBlockState(), 2);
                gesetzt = true;
            }
            // verseuchter Boden drumherum
            level.setBlock(unten, z.nextBoolean() ? Blocks.COARSE_DIRT.defaultBlockState() : Blocks.PODZOL.defaultBlockState(), 2);
        }
        return gesetzt;
    }
}
