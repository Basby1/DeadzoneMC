package de.deadzonemc.welt.feature;

import de.deadzonemc.welt.Bau;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** Alte Holz-Strommasten, manche umgeknickt. Die Leitungen sind längst gerissen. */
public class StrommastFeature extends Feature<NoneFeatureConfiguration> {

    public StrommastFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource zufall = context.random();
        BlockPos fuss = context.origin();
        if (de.deadzonemc.welt.plan.PlanSpeicher.belegt(level.getSeed(), fuss.getX(), fuss.getZ(), 6)) {
            return false; // in Ortschaften wächst keine Deko
        }
        if (!Bau.fest(level.getBlockState(fuss.below())) || !Bau.ersetzbar(level.getBlockState(fuss))) {
            return false;
        }
        BlockState mast = Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState();
        Direction quer = zufall.nextBoolean() ? Direction.EAST : Direction.SOUTH;

        if (zufall.nextInt(4) == 0) {
            // Umgeknickt: Stumpf und liegender Mast
            setBlock(level, fuss, mast);
            setBlock(level, fuss.above(), mast);
            Direction fall = Direction.Plane.HORIZONTAL.getRandomDirection(zufall);
            BlockState liegend = mast.setValue(RotatedPillarBlock.AXIS, fall.getAxis());
            for (int i = 2; i < 8; i++) {
                BlockPos p = fuss.relative(fall, i);
                if (!Bau.ersetzbar(level.getBlockState(p)) || !Bau.fest(level.getBlockState(p.below()))) {
                    break;
                }
                setBlock(level, p, liegend);
            }
            return true;
        }

        int hoehe = 7 + zufall.nextInt(3);
        for (int i = 0; i < hoehe; i++) {
            BlockPos p = fuss.above(i);
            if (!Bau.ersetzbar(level.getBlockState(p))) {
                return i > 0;
            }
            setBlock(level, p, mast);
        }
        // Querbalken mit Isolatoren
        BlockPos oben = fuss.above(hoehe - 1);
        BlockState balken = Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, quer.getAxis());
        for (int seite = -1; seite <= 1; seite += 2) {
            BlockPos p = oben.relative(quer, seite);
            if (Bau.ersetzbar(level.getBlockState(p))) {
                setBlock(level, p, balken);
                if (Bau.ersetzbar(level.getBlockState(p.above()))) {
                    setBlock(level, p.above(), Blocks.END_ROD.defaultBlockState());
                }
            }
        }
        // Ein Stück herabhängende Leitung
        if (zufall.nextBoolean()) {
            BlockPos p = oben.relative(quer, 1).below();
            for (int i = 0; i < 2 + zufall.nextInt(3) && Bau.ersetzbar(level.getBlockState(p)); i++) {
                setBlock(level, p, Blocks.CHAIN.defaultBlockState());
                p = p.below();
            }
        }
        return true;
    }
}
