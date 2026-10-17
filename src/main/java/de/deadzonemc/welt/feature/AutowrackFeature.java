package de.deadzonemc.welt.feature;

import de.deadzonemc.welt.Bau;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * Verlassene Autos – manchmal liegt im Inneren noch etwas Brauchbares.
 *
 * <p>Seitenansicht (vorne links), fünf Blöcke lang und drei breit:</p>
 * <pre>
 *   .  _  _  _  .     Dach: Stufen in Lackfarbe
 *   _  G  G  G  _     Haube und Kofferraum als Stufen, dazwischen Scheiben
 *   K  R  K  R  K     Karosserie, R = Räder
 * </pre>
 */
public class AutowrackFeature extends Feature<NoneFeatureConfiguration> {

    /** Lackfarbe: voller Block und passende Stufe. */
    private record Lack(Block voll, Block stufe) {
    }

    private static final Lack[] LACKE = {
            new Lack(Blocks.POLISHED_ANDESITE, Blocks.POLISHED_ANDESITE_SLAB),
            new Lack(Blocks.SMOOTH_QUARTZ, Blocks.SMOOTH_QUARTZ_SLAB),
            new Lack(Blocks.RED_NETHER_BRICKS, Blocks.RED_NETHER_BRICK_SLAB),
            new Lack(Blocks.POLISHED_DEEPSLATE, Blocks.POLISHED_DEEPSLATE_SLAB),
            new Lack(Blocks.EXPOSED_CUT_COPPER, Blocks.EXPOSED_CUT_COPPER_SLAB),
            new Lack(Blocks.WEATHERED_CUT_COPPER, Blocks.WEATHERED_CUT_COPPER_SLAB),
            new Lack(Blocks.OXIDIZED_CUT_COPPER, Blocks.OXIDIZED_CUT_COPPER_SLAB),
            new Lack(Blocks.SMOOTH_STONE, Blocks.SMOOTH_STONE_SLAB),
    };
    private static final Lack AUSGEBRANNT = new Lack(Blocks.POLISHED_BLACKSTONE, Blocks.POLISHED_BLACKSTONE_SLAB);
    private static final Block[] GLAS = {Blocks.GRAY_STAINED_GLASS, Blocks.BLACK_STAINED_GLASS, Blocks.LIGHT_GRAY_STAINED_GLASS};

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
        Lack lack = ausgebrannt ? AUSGEBRANNT : Bau.waehlen(zufall, LACKE);
        BlockState voll = lack.voll().defaultBlockState();
        BlockState stufe = lack.stufe().defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM);
        BlockState glas = Bau.waehlen(zufall, GLAS).defaultBlockState();
        BlockState rad = Blocks.BLACK_CONCRETE.defaultBlockState();

        for (int l = 0; l < laenge; l++) {
            for (int b = 0; b < breite; b++) {
                // Karosserie mit vier Rädern an den Seiten
                boolean istRad = (l == 1 || l == 3) && (b == 0 || b == 2);
                setzen(level, versetzt(boden, entlangX, l, b, 0), istRad ? rad : voll);

                // Haube, Kofferraum und Scheiben
                BlockState mitte;
                if (l == 0 || l == 4) {
                    mitte = stufe;
                } else if (b == 1 && l >= 2) {
                    mitte = Bau.LUFT; // Innenraum
                } else {
                    boolean kaputt = ausgebrannt || zufall.nextInt(10) < 3;
                    mitte = kaputt ? Bau.LUFT : glas;
                }
                setzen(level, versetzt(boden, entlangX, l, b, 1), mitte);

                // Dach über dem Innenraum
                if (l >= 1 && l <= 3 && !(ausgebrannt && zufall.nextInt(3) == 0)) {
                    setzen(level, versetzt(boden, entlangX, l, b, 2), stufe);
                }
            }
        }
        // Hintere Dachsäulen in Lackfarbe, damit das Dach nicht schwebt
        setzen(level, versetzt(boden, entlangX, 3, 0, 1), voll);
        setzen(level, versetzt(boden, entlangX, 3, 2, 1), voll);
        // Manchmal liegt auf dem Rücksitz noch etwas
        if (zufall.nextInt(5) < 2) {
            BlockPos kiste = versetzt(boden, entlangX, 3, 1, 1);
            Bau.kiste(level, bereich, kiste.getX(), kiste.getY(), kiste.getZ(),
                    Blocks.BARREL.defaultBlockState(), Bau.beute("autowrack"), zufall.nextLong());
        }
        return true;
    }

    private static void setzen(WorldGenLevel level, BlockPos p, BlockState zustand) {
        level.setBlock(p, zustand, Block.UPDATE_CLIENTS);
    }

    private static BlockPos versetzt(BlockPos basis, boolean entlangX, int l, int b, int h) {
        return entlangX ? basis.offset(l, h, b) : basis.offset(b, h, l);
    }
}
