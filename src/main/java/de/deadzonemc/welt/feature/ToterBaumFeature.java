package de.deadzonemc.welt.feature;

import de.deadzonemc.welt.Bau;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** Kahle, graue Bäume ohne Blätter – stehend, abgebrochen oder umgestürzt. */
public class ToterBaumFeature extends Feature<NoneFeatureConfiguration> {

    private static final Block[] HOLZ = {
            Blocks.DARK_OAK_LOG, Blocks.DARK_OAK_LOG, Blocks.SPRUCE_LOG, Blocks.OAK_LOG,
            Blocks.STRIPPED_DARK_OAK_LOG, Blocks.STRIPPED_SPRUCE_LOG, Blocks.MANGROVE_LOG
    };

    public ToterBaumFeature() {
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
        Block holz = Bau.waehlen(zufall, HOLZ);
        if (zufall.nextInt(5) == 0) {
            return umgestuerzt(level, zufall, fuss, holz);
        }
        return stehend(level, zufall, fuss, holz);
    }

    private boolean stehend(WorldGenLevel level, RandomSource zufall, BlockPos fuss, Block holz) {
        boolean abgebrochen = zufall.nextInt(4) == 0;
        int hoehe = abgebrochen ? 2 + zufall.nextInt(3) : 5 + zufall.nextInt(6);
        BlockState senkrecht = holz.defaultBlockState();
        for (int i = 0; i < hoehe; i++) {
            BlockPos p = fuss.above(i);
            if (!Bau.ersetzbar(level.getBlockState(p))) {
                hoehe = i;
                break;
            }
            setBlock(level, p, senkrecht);
        }
        if (abgebrochen || hoehe < 4) {
            return hoehe > 0;
        }
        // Äste: schräg nach außen und etwas nach oben
        int aeste = 2 + zufall.nextInt(3);
        for (int a = 0; a < aeste; a++) {
            Direction richtung = Direction.Plane.HORIZONTAL.getRandomDirection(zufall);
            int start = hoehe / 2 + zufall.nextInt(Math.max(1, hoehe / 2));
            BlockPos p = fuss.above(start);
            int laenge = 1 + zufall.nextInt(3);
            for (int i = 0; i < laenge; i++) {
                p = p.relative(richtung);
                if (zufall.nextInt(3) == 0) {
                    p = p.above();
                    if (Bau.ersetzbar(level.getBlockState(p))) {
                        setBlock(level, p, senkrecht);
                    }
                    continue;
                }
                if (!Bau.ersetzbar(level.getBlockState(p))) {
                    break;
                }
                setBlock(level, p, holz.defaultBlockState()
                        .setValue(RotatedPillarBlock.AXIS, richtung.getAxis()));
            }
        }
        // Spitze: ein dünner Zweig
        if (zufall.nextBoolean()) {
            BlockPos spitze = fuss.above(hoehe);
            if (Bau.ersetzbar(level.getBlockState(spitze))) {
                setBlock(level, spitze, Blocks.DARK_OAK_FENCE.defaultBlockState());
            }
        }
        return true;
    }

    private boolean umgestuerzt(WorldGenLevel level, RandomSource zufall, BlockPos fuss, Block holz) {
        Direction richtung = Direction.Plane.HORIZONTAL.getRandomDirection(zufall);
        int laenge = 3 + zufall.nextInt(4);
        BlockState liegend = holz.defaultBlockState().setValue(RotatedPillarBlock.AXIS, richtung.getAxis());
        // Stumpf
        setBlock(level, fuss, holz.defaultBlockState());
        BlockPos p = fuss.relative(richtung, 2);
        for (int i = 0; i < laenge; i++) {
            BlockPos hier = p.relative(richtung, i);
            if (!Bau.ersetzbar(level.getBlockState(hier)) || !Bau.fest(level.getBlockState(hier.below()))) {
                break;
            }
            setBlock(level, hier, liegend);
        }
        return true; // der Stumpf steht auf jeden Fall
    }
}
