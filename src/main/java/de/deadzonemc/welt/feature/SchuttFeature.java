package de.deadzonemc.welt.feature;

import de.deadzonemc.welt.Bau;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** Schutthaufen und Mauerreste – Überbleibsel von Gebäuden, die es nicht mehr gibt. */
public class SchuttFeature extends Feature<NoneFeatureConfiguration> {

    private static final Block[] SCHUTT = {
            Blocks.COBBLESTONE, Blocks.COBBLESTONE, Blocks.MOSSY_COBBLESTONE, Blocks.GRAVEL, Blocks.GRAVEL,
            Blocks.ANDESITE, Blocks.CRACKED_STONE_BRICKS, Blocks.TUFF, Blocks.STONE_BRICK_SLAB,
            Blocks.COBBLESTONE_SLAB
    };
    private static final Block[] MAUER = {
            Blocks.BRICKS, Blocks.STONE_BRICKS, Blocks.CRACKED_STONE_BRICKS, Blocks.MOSSY_STONE_BRICKS,
            Blocks.WHITE_TERRACOTTA
    };

    public SchuttFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource zufall = context.random();
        BlockPos mitte = context.origin();
        if (!Bau.fest(level.getBlockState(mitte.below()))) {
            return false;
        }
        haufen(level, zufall, mitte);
        if (zufall.nextInt(3) == 0) {
            mauerrest(level, zufall, mitte.offset(zufall.nextInt(5) - 2, 0, zufall.nextInt(5) - 2));
        }
        return true;
    }

    private void haufen(WorldGenLevel level, RandomSource zufall, BlockPos mitte) {
        int radius = 1 + zufall.nextInt(2);
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int abstand = Math.abs(dx) + Math.abs(dz);
                int hoehe = Math.max(0, radius + 1 - abstand - zufall.nextInt(2));
                BlockPos boden = bodenSuchen(level, mitte.offset(dx, 0, dz));
                if (boden == null) {
                    continue;
                }
                for (int h = 0; h < hoehe; h++) {
                    BlockPos p = boden.above(h);
                    if (Bau.ersetzbar(level.getBlockState(p))) {
                        setBlock(level, p, Bau.waehlen(zufall, SCHUTT).defaultBlockState());
                    }
                }
            }
        }
    }

    private void mauerrest(WorldGenLevel level, RandomSource zufall, BlockPos start) {
        Direction richtung = Direction.Plane.HORIZONTAL.getRandomDirection(zufall);
        Block stein = Bau.waehlen(zufall, MAUER);
        int laenge = 2 + zufall.nextInt(4);
        for (int i = 0; i < laenge; i++) {
            BlockPos boden = bodenSuchen(level, start.relative(richtung, i));
            if (boden == null) {
                continue;
            }
            int hoehe = 1 + zufall.nextInt(3);
            for (int h = 0; h < hoehe; h++) {
                BlockPos p = boden.above(h);
                if (Bau.ersetzbar(level.getBlockState(p))) {
                    setBlock(level, p, zufall.nextInt(5) == 0
                            ? Blocks.CRACKED_STONE_BRICKS.defaultBlockState()
                            : stein.defaultBlockState());
                }
            }
        }
    }

    /** Sucht in der Nähe der Höhe den ersten freien Platz über festem Boden. */
    private static BlockPos bodenSuchen(WorldGenLevel level, BlockPos p) {
        for (int dy = 2; dy >= -3; dy--) {
            BlockPos hier = p.above(dy);
            if (Bau.ersetzbar(level.getBlockState(hier)) && Bau.fest(level.getBlockState(hier.below()))) {
                return hier;
            }
        }
        return null;
    }
}
