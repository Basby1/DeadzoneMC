package de.deadzonemc.welt.feature;

import de.deadzonemc.welt.Bau;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * Verlassene Autos – manchmal liegt im Inneren noch etwas Brauchbares.
 *
 * Gebaut wird mit {@link de.deadzonemc.welt.bau.Fahrzeuge#auto}: Limousine, Kombi, Kleinwagen, Pickup oder Lieferwagen.
 */
public class AutowrackFeature extends Feature<NoneFeatureConfiguration> {

    public AutowrackFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        BlockPos p = context.origin();
        if (de.deadzonemc.welt.plan.PlanSpeicher.belegt(context.level().getSeed(), p.getX(), p.getZ(), 8)) {
            return false;
        }
        return bauen(context.level(), context.random(), p, null);
    }

    /**
     * Baut ein Auto mit der Unterkante auf {@code boden}. Liefert {@code false}, wenn der Platz nicht passt.
     *
     * @param bereich nur hier darf gebaut werden (null = überall, für Deko-Features)
     */
    public static boolean bauen(WorldGenLevel level, RandomSource zufall, BlockPos boden, BoundingBox bereich) {
        boolean entlangX = zufall.nextBoolean();
        int laenge = 5;
        int breite = 3;
        // Passt das Auto hierhin? Fester Boden unter allen Feldern, nichts Festes im Weg.
        for (int l = 0; l < laenge; l++) {
            for (int b = 0; b < breite; b++) {
                for (int h = 0; h < 3; h++) {
                    BlockPos p = versetzt(boden, entlangX, l, b, h);
                    if (bereich != null && !bereich.isInside(p)) {
                        return false;
                    }
                    if (!Bau.ersetzbar(level.getBlockState(p))) {
                        return false;
                    }
                }
                if (!Bau.fest(level.getBlockState(versetzt(boden, entlangX, l, b, -1)))) {
                    return false;
                }
            }
        }
        boolean ausgebrannt = zufall.nextInt(4) == 0;
        long saat = zufall.nextLong();
        de.deadzonemc.welt.bau.Fahrzeuge.auto(de.deadzonemc.welt.bau.Fahrzeuge.kasten(level, bereich, boden.getX(), boden.getY(),
                boden.getZ(), entlangX, 5, 3, saat), saat, ausgebrannt);
        return true;
    }

    private static void setzen(WorldGenLevel level, BlockPos p, BlockState zustand) {
        level.setBlock(p, zustand, Block.UPDATE_CLIENTS);
    }

    private static BlockPos versetzt(BlockPos basis, boolean entlangX, int l, int b, int h) {
        return entlangX ? basis.offset(l, h, b) : basis.offset(b, h, l);
    }
}
