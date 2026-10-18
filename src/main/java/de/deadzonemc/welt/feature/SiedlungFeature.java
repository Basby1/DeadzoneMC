package de.deadzonemc.welt.feature;

import de.deadzonemc.mensch.MenschTypen;
import de.deadzonemc.welt.Bau;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Siedlungen der sechs neuen Fraktionen: Geier-Lager (Palisade, Zelte), Schrein der Totenflüsterer (Knochenkreis,
 * Seelenfeuer, ein Infizierter im Käfig), Baumhaus der Kinder des Waldes, Nomadenlager der Sandwanderer,
 * Glaskuppel des Tiefenvolks am Meeresgrund (mit Schacht nach oben) und Horst der Hüter des Himmels hoch über
 * den Gipfeln.
 */
public class SiedlungFeature extends Feature<NoneFeatureConfiguration> {

    public enum Art { LAGER, SCHREIN, BAUMHAUS, NOMADEN, KUPPEL, HORST }

    private final Art art;

    public SiedlungFeature(Art art) {
        super(NoneFeatureConfiguration.CODEC);
        this.art = art;
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel w = ctx.level();
        RandomSource r = ctx.random();
        int cx = (ctx.origin().getX() & ~15) + 8;
        int cz = (ctx.origin().getZ() & ~15) + 8;
        if (de.deadzonemc.welt.plan.PlanSpeicher.belegt(w.getSeed(), cx, cz, 20)
                || de.deadzonemc.welt.Staedte.stadtImUmkreis(cx, cz, 48)) {
            return false;
        }
        int y = w.getHeight(Heightmap.Types.WORLD_SURFACE_WG, cx, cz);
        int grund = w.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, cx, cz);
        BlockPos m = new BlockPos(cx, y, cz);
        if (art == Art.KUPPEL) {
            if (y - grund < 10) {
                return false; // zu flach für eine Kuppel
            }
            kuppel(w, r, new BlockPos(cx, grund, cz), y);
            return true;
        }
        if (!w.getFluidState(m.below()).isEmpty() || !flach(w, cx, cz, art == Art.HORST ? 12 : 3)) {
            return false;
        }
        switch (art) {
            case LAGER -> lager(w, r, m);
            case SCHREIN -> schrein(w, r, m);
            case BAUMHAUS -> baumhaus(w, r, m);
            case NOMADEN -> nomaden(w, r, m);
            case HORST -> horst(w, r, m);
            default -> {
            }
        }
        return true;
    }

    /** Für den Selbsttest: ohne Ortsprüfung bauen (Kuppel auf Land: zur Kontrolle der Blöcke). */
    public static void testBauen(WorldGenLevel w, RandomSource r, BlockPos m, Art art) {
        switch (art) {
            case LAGER -> lager(w, r, m);
            case SCHREIN -> schrein(w, r, m);
            case BAUMHAUS -> baumhaus(w, r, m);
            case NOMADEN -> nomaden(w, r, m);
            case KUPPEL -> kuppel(w, r, m, m.getY() + 12);
            case HORST -> horst(w, r, m);
        }
    }

    private static boolean flach(WorldGenLevel w, int cx, int cz, int toleranz) {
        int min = 999;
        int max = -999;
        for (int dx = -6; dx <= 6; dx += 6) {
            for (int dz = -6; dz <= 6; dz += 6) {
                int h = w.getHeight(Heightmap.Types.WORLD_SURFACE_WG, cx + dx, cz + dz);
                min = Math.min(min, h);
                max = Math.max(max, h);
            }
        }
        return max - min <= toleranz;
    }

    private static void set(WorldGenLevel w, BlockPos p, BlockState s) {
        w.setBlock(p, s, Block.UPDATE_CLIENTS);
    }

    private static BlockPos boden(WorldGenLevel w, int x, int z) {
        return new BlockPos(x, w.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z), z);
    }

    private static void zelt(WorldGenLevel w, BlockPos fuss, Block stoff, boolean entlangX) {
        for (int l = -2; l <= 2; l++) {
            for (int h = 0; h <= 2; h++) {
                for (int s : new int[]{-(2 - h), 2 - h}) {
                    BlockPos p = entlangX ? fuss.offset(l, h, s) : fuss.offset(s, h, l);
                    set(w, p, stoff.defaultBlockState());
                }
            }
            BlockPos innen = entlangX ? fuss.offset(l, 0, 0) : fuss.offset(0, 0, l);
            set(w, innen, Bau.LUFT);
            set(w, innen.above(), Bau.LUFT);
        }
        // offene Vorderseite
        for (int h = 0; h <= 1; h++) {
            for (int s = -1; s <= 1; s++) {
                set(w, entlangX ? fuss.offset(-2, h, s) : fuss.offset(s, h, -2), Bau.LUFT);
            }
        }
    }

    private static void leute(WorldGenLevel w, RandomSource r, String art, BlockPos mitte, int anzahl, int radius) {
        var h = MenschTypen.FRAKTIONEN.get(art);
        if (h == null) {
            return;
        }
        for (int i = 0; i < anzahl; i++) {
            Mob m = h.get().create(w.getLevel());
            if (m == null) {
                return;
            }
            m.moveTo(mitte.getX() + 0.5 + r.nextInt(3) - 1, mitte.getY(), mitte.getZ() + 0.5 + r.nextInt(3) - 1, r.nextFloat() * 360, 0);
            if (m instanceof de.deadzonemc.mensch.Mensch me) {
                me.heimatSetzen(mitte, radius);
            }
            m.finalizeSpawn(w, w.getCurrentDifficultyAt(mitte), MobSpawnType.STRUCTURE, null);
            w.addFreshEntityWithPassengers(m);
        }
    }

    private static void kiste(WorldGenLevel w, BlockPos p, String tabelle, RandomSource r, boolean fass) {
        Bau.kiste(w, null, p.getX(), p.getY(), p.getZ(), fass ? Blocks.BARREL.defaultBlockState() : Blocks.CHEST.defaultBlockState(),
                Bau.beute(tabelle), r.nextLong());
    }

    // ---- Geier-Lager ---------------------------------------------------------------------------

    private static void lager(WorldGenLevel w, RandomSource r, BlockPos m) {
        int R = 8;
        for (int dx = -R; dx <= R; dx++) {
            for (int dz = -R; dz <= R; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz);
                BlockPos b = boden(w, m.getX() + dx, m.getZ() + dz);
                if (d < R - 0.5 && r.nextInt(3) == 0) {
                    set(w, b.below(), Blocks.COARSE_DIRT.defaultBlockState());
                }
                if (d >= R - 0.5 && d < R + 0.5 && !(Math.abs(dx) <= 1 && dz > 0)) {
                    set(w, b, Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState());
                    set(w, b.above(), r.nextInt(4) == 0 ? Blocks.SPRUCE_FENCE.defaultBlockState() : Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState());
                    if (r.nextInt(5) == 0) {
                        set(w, b.above(2), Blocks.SKELETON_SKULL.defaultBlockState());
                    }
                }
            }
        }
        set(w, boden(w, m.getX(), m.getZ()), Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true));
        zelt(w, boden(w, m.getX() - 4, m.getZ() - 3), Blocks.BROWN_WOOL, true);
        zelt(w, boden(w, m.getX() + 4, m.getZ() - 3), Blocks.GRAY_WOOL, true);
        zelt(w, boden(w, m.getX(), m.getZ() - 5), Blocks.BLACK_WOOL, false);
        kiste(w, boden(w, m.getX() + 3, m.getZ() + 3), r.nextInt(4) == 0 ? "waffenkammer" : "stadt_laden", r, false);
        kiste(w, boden(w, m.getX() - 3, m.getZ() + 3), "autowrack", r, true);
        leute(w, r, "bandit", boden(w, m.getX(), m.getZ() + 2), 3, 9);
    }

    // ---- Schrein der Totenflüsterer ---------------------------------------------------------------

    private static void schrein(WorldGenLevel w, RandomSource r, BlockPos m) {
        for (int i = 0; i < 10; i++) {
            double wk = 2 * Math.PI * i / 10;
            BlockPos b = boden(w, m.getX() + (int) Math.round(Math.cos(wk) * 6), m.getZ() + (int) Math.round(Math.sin(wk) * 6));
            int h = 1 + r.nextInt(3);
            for (int k = 0; k < h; k++) {
                set(w, b.above(k), Blocks.BONE_BLOCK.defaultBlockState());
            }
            set(w, b.above(h), r.nextBoolean() ? Blocks.SKELETON_SKULL.defaultBlockState() : Blocks.CANDLE.defaultBlockState()
                    .setValue(net.minecraft.world.level.block.CandleBlock.LIT, true));
        }
        BlockPos c = boden(w, m.getX(), m.getZ());
        set(w, c.below(), Blocks.SOUL_SOIL.defaultBlockState());
        set(w, c, Blocks.SOUL_CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true));
        // Käfig mit einem Infizierten
        BlockPos k = boden(w, m.getX() + 3, m.getZ());
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                for (int h = 0; h <= 2; h++) {
                    boolean rand = Math.abs(dx) == 1 || Math.abs(dz) == 1 || h == 2;
                    set(w, k.offset(dx, h, dz), rand ? Blocks.IRON_BARS.defaultBlockState() : Bau.LUFT);
                }
            }
        }
        Mob z = de.deadzonemc.monster.MonsterTypen.WANDERER.get().create(w.getLevel());
        if (z != null) {
            z.moveTo(k.getX() + 0.5, k.getY(), k.getZ() + 0.5, 0, 0);
            z.setPersistenceRequired();
            w.addFreshEntity(z);
        }
        kiste(w, boden(w, m.getX() - 3, m.getZ() + 1), "ruine", r, true);
        leute(w, r, "totenfluesterer", boden(w, m.getX() - 2, m.getZ() - 2), 2, 8);
    }

    // ---- Baumhaus der Kinder des Waldes -------------------------------------------------------------

    private static void baumhaus(WorldGenLevel w, RandomSource r, BlockPos m) {
        int y = m.getY() + 7;
        BlockState holz = Blocks.SPRUCE_PLANKS.defaultBlockState();
        BlockState stamm = Blocks.SPRUCE_LOG.defaultBlockState();
        for (int[] s : new int[][]{{-3, -3}, {3, -3}, {-3, 3}, {3, 3}, {0, 0}}) {
            BlockPos u = boden(w, m.getX() + s[0], m.getZ() + s[1]);
            for (int yy = u.getY(); yy < y; yy++) {
                set(w, new BlockPos(u.getX(), yy, u.getZ()), stamm);
            }
        }
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                set(w, new BlockPos(m.getX() + dx, y, m.getZ() + dz), holz);
                boolean rand = Math.abs(dx) == 4 || Math.abs(dz) == 4;
                set(w, new BlockPos(m.getX() + dx, y + 1, m.getZ() + dz), rand && !(dx == 0 && dz == 4)
                        ? Blocks.SPRUCE_FENCE.defaultBlockState() : Bau.LUFT);
                for (int h = 2; h <= 4; h++) {
                    set(w, new BlockPos(m.getX() + dx, y + h, m.getZ() + dz), Bau.LUFT);
                }
                if (Math.abs(dx) <= 3 && Math.abs(dz) <= 3) {
                    set(w, new BlockPos(m.getX() + dx, y + 4, m.getZ() + dz), Math.abs(dx) == 3 || Math.abs(dz) == 3
                            ? Blocks.OAK_LEAVES.defaultBlockState().setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT, true)
                            : Blocks.SPRUCE_SLAB.defaultBlockState());
                }
            }
        }
        // Stützen für das Dach und Leiter
        for (int h = 1; h <= 3; h++) {
            for (int[] s : new int[][]{{-3, -3}, {3, -3}, {-3, 3}, {3, 3}}) {
                set(w, new BlockPos(m.getX() + s[0], y + h, m.getZ() + s[1]), stamm);
            }
        }
        BlockPos fuss = boden(w, m.getX(), m.getZ() + 5);
        for (int yy = fuss.getY(); yy <= y + 1; yy++) {
            set(w, new BlockPos(m.getX(), yy, m.getZ() + 5), Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.SOUTH));
        }
        set(w, new BlockPos(m.getX(), y, m.getZ() + 5), Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.SOUTH));
        set(w, new BlockPos(m.getX() + 4, y, m.getZ() + 5), holz);
        kiste(w, new BlockPos(m.getX() + 2, y + 1, m.getZ() - 2), "scheune", r, false);
        set(w, new BlockPos(m.getX() - 2, y + 1, m.getZ() - 2), Blocks.COMPOSTER.defaultBlockState());
        set(w, new BlockPos(m.getX() - 2, y + 1, m.getZ() + 2), Blocks.LANTERN.defaultBlockState());
        leute(w, r, "waldkind", new BlockPos(m.getX() + 1, y + 1, m.getZ() + 1), 2, 3);
    }

    // ---- Nomadenlager der Sandwanderer ----------------------------------------------------------

    private static void nomaden(WorldGenLevel w, RandomSource r, BlockPos m) {
        zelt(w, boden(w, m.getX() - 4, m.getZ()), Blocks.WHITE_WOOL, false);
        zelt(w, boden(w, m.getX() + 4, m.getZ()), Blocks.ORANGE_WOOL, false);
        zelt(w, boden(w, m.getX(), m.getZ() + 5), Blocks.YELLOW_WOOL, true);
        BlockPos c = boden(w, m.getX(), m.getZ() - 2);
        set(w, c, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true));
        BlockPos wasser = boden(w, m.getX() + 2, m.getZ() - 4);
        set(w, wasser, Blocks.WATER_CAULDRON.defaultBlockState().setValue(net.minecraft.world.level.block.LayeredCauldronBlock.LEVEL, 3));
        kiste(w, boden(w, m.getX() - 2, m.getZ() - 4), "laden_lager", r, true);
        var kamel = EntityType.CAMEL.create(w.getLevel());
        if (kamel != null) {
            BlockPos k = boden(w, m.getX() + 6, m.getZ() - 4);
            kamel.moveTo(k.getX() + 0.5, k.getY(), k.getZ() + 0.5, 0, 0);
            kamel.setPersistenceRequired();
            w.addFreshEntity(kamel);
        }
        leute(w, r, "sandwanderer", boden(w, m.getX(), m.getZ()), 2, 8);
    }

    // ---- Kuppel des Tiefenvolks --------------------------------------------------------------------

    private static void kuppel(WorldGenLevel w, RandomSource r, BlockPos grund, int oberflaeche) {
        int R = 6;
        BlockState glas = Blocks.GLASS.defaultBlockState();
        for (int dx = -R; dx <= R; dx++) {
            for (int dz = -R; dz <= R; dz++) {
                for (int dy = 0; dy <= R; dy++) {
                    double d = Math.sqrt(dx * dx + dy * dy + dz * dz);
                    BlockPos p = grund.offset(dx, dy, dz);
                    if (dy == 0 && Math.sqrt(dx * dx + dz * dz) <= R) {
                        set(w, p, (dx % 3 == 0 && dz % 3 == 0) ? Blocks.SEA_LANTERN.defaultBlockState() : Blocks.PRISMARINE_BRICKS.defaultBlockState());
                    } else if (d <= R + 0.4 && d > R - 1.3) {
                        set(w, p, glas);
                    } else if (d <= R - 1.3) {
                        set(w, p, Bau.LUFT);
                    }
                }
            }
        }
        // Schacht nach oben bis über die Wasseroberfläche, mit Leiter
        int top = grund.getY() + R;
        for (int y = top; y <= oberflaeche + 1; y++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos p = new BlockPos(grund.getX() + dx, y, grund.getZ() + dz);
                    boolean rand = Math.abs(dx) == 1 || Math.abs(dz) == 1;
                    set(w, p, rand ? glas : Bau.LUFT);
                }
            }
            set(w, new BlockPos(grund.getX(), y, grund.getZ() - 1), Blocks.PRISMARINE_BRICKS.defaultBlockState());
            set(w, new BlockPos(grund.getX(), y, grund.getZ()), Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.SOUTH));
        }
        for (int y = grund.getY() + 1; y < top; y++) {
            set(w, new BlockPos(grund.getX(), y, grund.getZ() - 1), Blocks.PRISMARINE_BRICKS.defaultBlockState());
            set(w, new BlockPos(grund.getX(), y, grund.getZ()), Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.SOUTH));
        }
        // Plattform an der Oberfläche
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (Math.abs(dx) == 2 || Math.abs(dz) == 2) {
                    set(w, new BlockPos(grund.getX() + dx, oberflaeche, grund.getZ() + dz), Blocks.DARK_PRISMARINE.defaultBlockState());
                }
            }
        }
        kiste(w, grund.offset(3, 1, 2), "werkstatt", r, false);
        kiste(w, grund.offset(-3, 1, 2), "kueche", r, true);
        leute(w, r, "tiefenvolk", grund.offset(2, 1, -2), 2, 4);
    }

    // ---- Horst der Hüter des Himmels -----------------------------------------------------------------

    private static void horst(WorldGenLevel w, RandomSource r, BlockPos m) {
        int y = Math.min(w.getMaxBuildHeight() - 12, m.getY() + 26);
        int R = 8;
        for (int dx = -R; dx <= R; dx++) {
            for (int dz = -R; dz <= R; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz);
                if (d > R + 0.4) {
                    continue;
                }
                BlockPos p = new BlockPos(m.getX() + dx, y, m.getZ() + dz);
                set(w, p, d < 2 ? Blocks.SEA_LANTERN.defaultBlockState() : Blocks.SMOOTH_QUARTZ.defaultBlockState());
                if (d > R - 0.6) {
                    set(w, p.above(), Blocks.WHITE_STAINED_GLASS_PANE.defaultBlockState());
                }
            }
        }
        // Lichtsäule und Fernrohr-Pult
        for (int h = 1; h <= 4; h++) {
            set(w, new BlockPos(m.getX(), y + h, m.getZ()), h == 4 ? Blocks.END_ROD.defaultBlockState() : Blocks.QUARTZ_PILLAR.defaultBlockState());
        }
        set(w, new BlockPos(m.getX() + 4, y + 1, m.getZ()), Blocks.LECTERN.defaultBlockState());
        kiste(w, new BlockPos(m.getX() - 4, y + 1, m.getZ()), "militaer", r, false);
        kiste(w, new BlockPos(m.getX(), y + 1, m.getZ() + 4), "station_null", r, true);
        leute(w, r, "himmelshueter", new BlockPos(m.getX() + 2, y + 1, m.getZ() + 2), 2, 6);
    }
}
