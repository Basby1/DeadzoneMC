package de.deadzonemc.welt.bau;

import net.minecraft.core.Direction;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.Random;

/**
 * Fahrzeugwracks. Alle werden im Baukasten entworfen: lz = Längsrichtung (0 = Front), lx = Breite.
 */
public final class Fahrzeuge {

    private Fahrzeuge() {
    }

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
            new Lack(Blocks.PRISMARINE_BRICKS, Blocks.PRISMARINE_BRICK_SLAB),
    };
    private static final Lack BRAND = new Lack(Blocks.POLISHED_BLACKSTONE, Blocks.POLISHED_BLACKSTONE_SLAB);
    private static final Block[] GLAS = {Blocks.GRAY_STAINED_GLASS, Blocks.BLACK_STAINED_GLASS, Blocks.LIGHT_GRAY_STAINED_GLASS};

    /** Baukasten für ein Fahrzeug mit der Ecke (minX, minZ), Länge entlang X oder Z. */
    public static Baukasten kasten(WorldGenLevel level, BoundingBox bereich, int minX, int y, int minZ,
                                   boolean entlangX, int laenge, int breite, long saat) {
        if (entlangX) {
            return new Baukasten(level, bereich, minX, minZ, minX + laenge - 1, minZ + breite - 1, y, Direction.WEST, saat);
        }
        return new Baukasten(level, bereich, minX, minZ, minX + breite - 1, minZ + laenge - 1, y, Direction.NORTH, saat);
    }

    /** Ein Auto: 5 lang, 3 breit, 3 hoch. ly = 0 steht auf dem Boden. */
    public static void auto(Baukasten b, long saat, boolean ausgebrannt) {
        Random r = new Random(saat);
        Lack lack = ausgebrannt ? BRAND : LACKE[r.nextInt(LACKE.length)];
        BlockState voll = lack.voll().defaultBlockState();
        BlockState stufe = lack.stufe().defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM);
        BlockState glas = GLAS[r.nextInt(GLAS.length)].defaultBlockState();
        BlockState rad = Blocks.BLACK_CONCRETE.defaultBlockState();
        boolean umgedreht = r.nextBoolean(); // Front mal vorne, mal hinten
        for (int l = 0; l < 5; l++) {
            int lz = umgedreht ? 4 - l : l;
            for (int lx = 0; lx < 3; lx++) {
                boolean istRad = (l == 1 || l == 3) && lx != 1;
                b.setze(lx, 0, lz, istRad ? rad : voll);
                BlockState mitte;
                if (l == 0 || l == 4) {
                    mitte = stufe;
                } else if (l == 3 && lx != 1) {
                    mitte = voll; // hintere Dachsäule
                } else if (lx == 1 && l >= 2) {
                    mitte = net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
                } else {
                    mitte = ausgebrannt || r.nextInt(10) < 3 ? Blocks.AIR.defaultBlockState() : glas;
                }
                b.setze(lx, 1, lz, mitte);
                if (l >= 1 && l <= 3) {
                    b.setze(lx, 2, lz, ausgebrannt && r.nextInt(3) == 0 ? Blocks.AIR.defaultBlockState() : stufe);
                } else {
                    b.setze(lx, 2, lz, Blocks.AIR.defaultBlockState());
                }
            }
        }
        // Motorhaube vorn in der Mitte (mit dem Brecheisen aufzuhebeln)
        b.setze(1, 1, umgedreht ? 4 : 0, de.deadzonemc.beute.Beute.MOTORRAUM.get().defaultBlockState());
        if (r.nextInt(5) < 2) {
            b.fass(1, 1, umgedreht ? 1 : 3, Baukasten.H, "autowrack");
        }
    }

    /** Ein Traktor: 4 lang, 3 breit, große Hinterräder, offene Kabine. */
    public static void traktor(Baukasten b, long saat) {
        Random r = new Random(saat);
        BlockState lack = (r.nextBoolean() ? Blocks.GREEN_TERRACOTTA : Blocks.RED_TERRACOTTA).defaultBlockState();
        BlockState rad = Blocks.BLACK_CONCRETE.defaultBlockState();
        // Front (Motor) lz 0..1 schmal, hinten lz 2..3 mit großen Rädern
        b.setze(1, 0, 0, lack);
        b.setze(1, 1, 0, lack);
        b.setze(1, 0, 1, lack);
        b.setze(1, 1, 1, lack);
        b.setze(0, 0, 0, rad);
        b.setze(2, 0, 0, rad);
        for (int lz = 2; lz <= 3; lz++) {
            b.setze(0, 0, lz, rad);
            b.setze(0, 1, lz, rad);
            b.setze(2, 0, lz, rad);
            b.setze(2, 1, lz, rad);
            b.setze(1, 0, lz, lack);
        }
        b.setze(1, 1, 3, Blocks.SPRUCE_STAIRS.defaultBlockState()
                .setValue(net.minecraft.world.level.block.StairBlock.FACING, Baukasten.H)); // Sitz
        b.setze(1, 2, 2, Blocks.IRON_BARS.defaultBlockState()); // Lenkrad
        b.setze(1, 2, 0, Blocks.IRON_BARS.defaultBlockState()); // Auspuff
    }
}
