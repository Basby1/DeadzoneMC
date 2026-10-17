package de.deadzonemc.welt.feature;

import de.deadzonemc.welt.Bau;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/** Ausgebrannte, verrostete Autos – manchmal liegt im Inneren noch etwas Brauchbares. */
public class AutowrackFeature extends Feature<NoneFeatureConfiguration> {

    private static final Block[] LACK = {
            Blocks.RED_CONCRETE, Blocks.WHITE_CONCRETE, Blocks.LIGHT_GRAY_CONCRETE, Blocks.GRAY_CONCRETE,
            Blocks.BLACK_CONCRETE, Blocks.BLUE_TERRACOTTA, Blocks.CYAN_TERRACOTTA, Blocks.GREEN_TERRACOTTA,
            Blocks.WHITE_TERRACOTTA
    };
    private static final Block[] ROST = {Blocks.BROWN_TERRACOTTA, Blocks.ORANGE_TERRACOTTA, Blocks.TERRACOTTA};
    private static final Block[] SCHEIBE = {
            Blocks.GRAY_STAINED_GLASS_PANE, Blocks.BLACK_STAINED_GLASS_PANE, Blocks.GLASS_PANE
    };

    public AutowrackFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        return bauen(context.level(), context.random(), context.origin(), null);
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
                BlockPos p = versetzt(boden, entlangX, l, b, 0);
                if (bereich != null && !bereich.isInside(p)) {
                    return false;
                }
                if (!Bau.fest(level.getBlockState(p.below())) || !Bau.ersetzbar(level.getBlockState(p))) {
                    return false;
                }
            }
        }
        BlockState lack = Bau.waehlen(zufall, LACK).defaultBlockState();
        BlockState scheibe = Bau.waehlen(zufall, SCHEIBE).defaultBlockState();
        boolean ausgebrannt = zufall.nextInt(3) == 0;
        if (ausgebrannt) {
            lack = Blocks.BLACKSTONE.defaultBlockState();
        }

        for (int l = 0; l < laenge; l++) {
            for (int b = 0; b < breite; b++) {
                boolean rad = (l == 1 || l == 3) && (b == 0 || b == 2);
                BlockState unten = rad ? Blocks.BLACK_CONCRETE.defaultBlockState() : rostig(zufall, lack);
                setzen(level, versetzt(boden, entlangX, l, b, 0), unten);
            }
        }
        // Fahrgastzelle
        for (int l = 1; l <= 3; l++) {
            for (int b = 0; b < breite; b++) {
                boolean rand = b == 0 || b == 2 || l == 1;
                BlockState mitte = rand && zufall.nextInt(3) > 0 ? scheibe : Bau.LUFT;
                setzen(level, versetzt(boden, entlangX, l, b, 1), mitte);
                if (zufall.nextInt(6) > 0) {
                    setzen(level, versetzt(boden, entlangX, l, b, 2), rostig(zufall, lack));
                }
            }
        }
        // Manchmal ist im Kofferraum oder auf dem Rücksitz noch etwas
        if (zufall.nextInt(5) < 2) {
            BlockPos kiste = versetzt(boden, entlangX, 2, 1, 1);
            level.setBlock(kiste, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            Bau.kiste(level, bereich, kiste.getX(), kiste.getY(), kiste.getZ(),
                    Blocks.BARREL.defaultBlockState(), Bau.beute("autowrack"), zufall.nextLong());
        }
        return true;
    }

    private static BlockState rostig(RandomSource zufall, BlockState lack) {
        return zufall.nextInt(4) == 0 ? Bau.waehlen(zufall, ROST).defaultBlockState() : lack;
    }

    private static void setzen(WorldGenLevel level, BlockPos p, BlockState zustand) {
        level.setBlock(p, zustand, Block.UPDATE_CLIENTS);
    }

    private static BlockPos versetzt(BlockPos basis, boolean entlangX, int l, int b, int h) {
        return entlangX ? basis.offset(l, h, b) : basis.offset(b, h, l);
    }
}
