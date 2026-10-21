package de.deadzonemc.welt.feature;

import de.deadzonemc.monster.MonsterTypen;
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
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import java.util.function.Supplier;

/**
 * Orte mit Geschichte (POIs) im Umland: abgestürzter Militärhubschrauber (Rauchsäule, Militärkisten, gefallene
 * Soldaten), zerbrochenes Passagierflugzeug (Gepäck, infizierte Passagiere) und gestrandeter Frachter an der Küste
 * (Container voller Lagerware). Alles bleibt in Reichweite des Chunks, damit die Erzeugung sauber bleibt.
 */
public class AbsturzFeature extends Feature<NoneFeatureConfiguration> {

    public enum Art { HUBSCHRAUBER, FLUGZEUG, FRACHTER, PANZER, KONVOI, EVAKUIERUNG, RASTSTAETTE, FUNKTURM, CHECKPOINT }

    private final Art art;

    public AbsturzFeature(Art art) {
        super(NoneFeatureConfiguration.CODEC);
        this.art = art;
    }

    /** Lokales Koordinatensystem: l entlang, b quer, h hoch – ausgehend von einem Ursprung am Boden. */
    private record Rahmen(WorldGenLevel w, BlockPos o, Direction lang, Direction quer) {
        BlockPos pos(int l, int b, int h) {
            return o.relative(lang, l).relative(quer, b).above(h);
        }

        void setzen(int l, int b, int h, BlockState s) {
            BlockPos p = pos(l, b, h);
            if (p.getY() > w.getMinBuildHeight() && p.getY() < w.getMaxBuildHeight()) {
                w.setBlock(p, s, Block.UPDATE_CLIENTS);
            }
        }

        void kiste(int l, int b, int h, String tabelle, RandomSource r, boolean fass) {
            BlockPos p = pos(l, b, h);
            BlockState s = fass ? Blocks.BARREL.defaultBlockState()
                    : Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, quer);
            Bau.kiste(w, null, p.getX(), p.getY(), p.getZ(), s, Bau.beute(tabelle), r.nextLong());
        }

        /** Füllt unter einem Block bis zum Boden auf (nichts schwebt). */
        void stuetzen(int l, int b, int h, BlockState s) {
            BlockPos p = pos(l, b, h).below();
            for (int i = 0; i < 6 && !Bau.fest(w.getBlockState(p)); i++) {
                w.setBlock(p, s, Block.UPDATE_CLIENTS);
                p = p.below();
            }
        }
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel w = ctx.level();
        RandomSource r = ctx.random();
        // Mitte des Chunks: so passt alles (bis 15 Blöcke) in den beschreibbaren Bereich
        int cx = (ctx.origin().getX() & ~15) + 8;
        int cz = (ctx.origin().getZ() & ~15) + 8;
        if (de.deadzonemc.welt.plan.PlanSpeicher.belegt(w.getSeed(), cx, cz, 24)
                || de.deadzonemc.welt.Staedte.stadtImUmkreis(cx, cz, 40)) {
            return false;
        }
        int y = w.getHeight(Heightmap.Types.WORLD_SURFACE_WG, cx, cz);
        int meer = w.getSeaLevel();
        // Flach genug? (Frachter: Küste – Boden nahe Meereshöhe)
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        for (int dx = -10; dx <= 10; dx += 5) {
            for (int dz = -10; dz <= 10; dz += 5) {
                int h = w.getHeight(art == Art.FRACHTER ? Heightmap.Types.OCEAN_FLOOR_WG : Heightmap.Types.WORLD_SURFACE_WG,
                        cx + dx, cz + dz);
                min = Math.min(min, h);
                max = Math.max(max, h);
            }
        }
        BlockPos boden = new BlockPos(cx, y, cz);
        if (art == Art.FRACHTER) {
            int grund = w.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, cx, cz);
            if (grund < meer - 5 || grund > meer + 2 || max - min > 7) {
                return false;
            }
            boden = new BlockPos(cx, grund, cz);
        } else {
            if (max - min > 4 || y <= meer || !w.getFluidState(boden.below()).isEmpty()) {
                return false;
            }
        }
        Direction lang = Direction.Plane.HORIZONTAL.getRandomDirection(r);
        Rahmen f = new Rahmen(w, boden, lang, lang.getClockWise());
        switch (art) {
            case HUBSCHRAUBER -> hubschrauber(f, r);
            case FLUGZEUG -> flugzeug(f, r);
            case FRACHTER -> frachter(f, r, meer - boden.getY());
            case PANZER -> panzer(f, r, 0, 0, true);
            case KONVOI -> konvoi(f, r);
            case EVAKUIERUNG -> evakuierung(f, r);
            case RASTSTAETTE -> raststaette(f, r);
            case FUNKTURM -> funkturm(f, r);
            case CHECKPOINT -> checkpoint(f, r);
        }
        return true;
    }

    /**
     * Raststätte an der Landstraße: Asphalt-Vorplatz, Tankdach mit Zapfsäulen (halb eingestürzt), Shop mit
     * Regalen, Kühlregal und Kasse, angebaute Werkstatt, hohes Schild, liegengebliebene Autos und Plakate.
     */
    private static void raststaette(Rahmen f, RandomSource r) {
        BlockState asphalt = Blocks.GRAY_CONCRETE.defaultBlockState();
        BlockState riss = Blocks.COBBLESTONE.defaultBlockState();
        BlockState linie = Blocks.WHITE_CONCRETE.defaultBlockState();
        BlockState wand = Blocks.WHITE_TERRACOTTA.defaultBlockState();
        BlockState sockel = Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState();
        BlockState rot = Blocks.RED_CONCRETE.defaultBlockState();
        BlockState stahl = Blocks.IRON_BLOCK.defaultBlockState();
        BlockState dach = Blocks.SMOOTH_STONE_SLAB.defaultBlockState();
        BlockState scheibe = Blocks.GLASS_PANE.defaultBlockState();
        // Gelände einebnen: Boden bei h -1, darüber frei
        for (int l = -12; l <= 12; l++) {
            for (int b = -10; b <= 10; b++) {
                boolean platz = b <= 3;
                f.setzen(l, b, -1, platz ? (r.nextInt(9) == 0 ? riss : asphalt) : sockel);
                f.stuetzen(l, b, -1, Blocks.DIRT.defaultBlockState());
                for (int h = 0; h <= 7; h++) {
                    f.setzen(l, b, h, Bau.LUFT);
                }
            }
        }
        // Parkmarkierungen
        for (int l = -10; l <= 10; l += 4) {
            for (int b = 0; b <= 2; b++) {
                f.setzen(l, b, -1, linie);
            }
        }
        // Tankdach: vier Stützen, Dach mit rotem Rand, eine Ecke eingestürzt
        int[][] stuetzen = {{-5, -8}, {5, -8}, {-5, -2}, {5, -2}};
        for (int[] st : stuetzen) {
            for (int h = 0; h <= 4; h++) {
                f.setzen(st[0], st[1], h, sockel);
            }
        }
        for (int l = -7; l <= 7; l++) {
            for (int b = -10; b <= 0; b++) {
                boolean eingestuerzt = l > 3 && b < -6;
                if (eingestuerzt) {
                    if (r.nextInt(3) == 0) {
                        f.setzen(l, b, 0, r.nextBoolean() ? dach : riss); // Trümmer am Boden
                    }
                    continue;
                }
                boolean rand = Math.abs(l) == 7 || b == -10 || b == 0;
                f.setzen(l, b, 5, rand ? rot : (r.nextInt(12) == 0 ? Bau.LUFT : dach));
            }
        }
        // Zapfsäulen
        for (int l : new int[]{-2, 2}) {
            for (int b : new int[]{-7, -3}) {
                f.setzen(l, b, 0, rot);
                f.setzen(l, b, 1, Blocks.WHITE_CONCRETE.defaultBlockState());
                f.setzen(l, b, 2, Blocks.SMOOTH_STONE_SLAB.defaultBlockState());
                f.setzen(l + 1, b, 0, Blocks.STONE_SLAB.defaultBlockState());
            }
        }
        f.setzen(0, -5, 0, Blocks.CHAIN.defaultBlockState()); // abgerissener Schlauch
        // Shop: l -10..2, b 4..10
        for (int l = -10; l <= 2; l++) {
            for (int b = 4; b <= 10; b++) {
                boolean aussen = l == -10 || l == 2 || b == 4 || b == 10;
                for (int h = 0; h <= 3; h++) {
                    if (!aussen) {
                        continue;
                    }
                    boolean fenster = b == 4 && h >= 1 && h <= 2 && l > -9 && l < 1;
                    boolean tuer = b == 4 && (l == 1) && h <= 1;
                    if (tuer) {
                        continue;
                    }
                    f.setzen(l, b, h, fenster ? (r.nextInt(3) == 0 ? Bau.LUFT : scheibe) : wand);
                }
                f.setzen(l, b, 4, r.nextInt(10) == 0 ? Bau.LUFT : Blocks.GRAY_CONCRETE.defaultBlockState());
                if (b == 4) {
                    f.setzen(l, 3, 4, rot); // rotes Band über den Fenstern
                }
            }
        }
        // Inneneinrichtung: Regalreihen, Kühlregal an der Rückwand, Kasse
        for (int l = -8; l <= -2; l += 3) {
            f.kiste(l, 7, 0, "laden_regal", r, true);
            f.setzen(l, 7, 1, Blocks.OAK_SLAB.defaultBlockState());
        }
        f.kiste(-9, 9, 0, "laden_kuehlregal", r, false);
        f.kiste(-6, 9, 0, "kuehlschrank", r, false);
        f.kiste(0, 6, 0, "tankstelle", r, false);
        f.setzen(0, 7, 0, Blocks.POLISHED_ANDESITE.defaultBlockState()); // Kassentresen
        f.setzen(-1, 7, 0, Blocks.POLISHED_ANDESITE.defaultBlockState());
        // Werkstatt: l 4..10, b 5..10, offenes Rolltor zum Vorplatz
        for (int l = 4; l <= 10; l++) {
            for (int b = 5; b <= 10; b++) {
                boolean aussen = l == 4 || l == 10 || b == 5 || b == 10;
                boolean tor = b == 5 && l >= 6 && l <= 8;
                for (int h = 0; h <= 3; h++) {
                    if (aussen && !(tor && h <= 2)) {
                        f.setzen(l, b, h, Blocks.STONE_BRICKS.defaultBlockState());
                    }
                }
                f.setzen(l, b, 4, Blocks.GRAY_CONCRETE.defaultBlockState());
            }
        }
        f.kiste(9, 9, 0, "werkstatt", r, true);
        f.kiste(5, 9, 0, "werkzeug", r, false);
        f.setzen(7, 8, 0, Blocks.ANVIL.defaultBlockState());
        // Schild an hohem Mast
        for (int h = 0; h <= 8; h++) {
            f.setzen(11, -9, h, Blocks.IRON_BARS.defaultBlockState());
        }
        for (int b = -10; b <= -8; b++) {
            f.setzen(11, b, 9, Blocks.YELLOW_CONCRETE.defaultBlockState());
            f.setzen(11, b, 10, rot);
        }
        // Plakate an der Shopfront, Kreidezeichen an der Werkstatt
        f.setzen(-9, 3, 1, de.deadzonemc.welt.deko.Wanddeko.PLAKAT.get().defaultBlockState()
                .setValue(net.minecraft.world.level.block.LadderBlock.FACING, f.quer().getOpposite())
                .setValue(de.deadzonemc.welt.deko.Wanddeko.Bild.MOTIV, r.nextInt(4)));
        f.setzen(5, 4, 1, de.deadzonemc.welt.deko.Wanddeko.KREIDEZEICHEN.get().defaultBlockState()
                .setValue(net.minecraft.world.level.block.LadderBlock.FACING, f.quer().getOpposite())
                .setValue(de.deadzonemc.welt.deko.Wanddeko.Bild.MOTIV, 2));
        // Liegengebliebene Autos auf dem Vorplatz
        for (int i = 0; i < 2; i++) {
            BlockPos p = f.pos(-9 + i * 10 + r.nextInt(3), -1 + r.nextInt(2), 0);
            AutowrackFeature.bauen(f.w(), r, p, null);
        }
        spawnen(f, MonsterTypen.WANDERER::get, 2 + r.nextInt(2), r);
    }

    /**
     * Funkturm: ein hoher Gittermast mit Plattformen und Warnlicht, unten eine Funkerhütte mit Funkstation,
     * Werkbankkiste und einem Vorratsfass. Ein paar gefallene Soldaten halten noch Wache.
     */
    private static void funkturm(Rahmen f, RandomSource r) {
        BlockState gitter = Blocks.IRON_BARS.defaultBlockState();
        BlockState platte = Blocks.SMOOTH_STONE_SLAB.defaultBlockState();
        BlockState fundament = Blocks.POLISHED_ANDESITE.defaultBlockState();
        // Fundament und freier Platz
        for (int l = -3; l <= 8; l++) {
            for (int b = -3; b <= 3; b++) {
                f.setzen(l, b, -1, r.nextInt(5) == 0 ? Blocks.GRAVEL.defaultBlockState() : fundament);
                f.stuetzen(l, b, -1, Blocks.DIRT.defaultBlockState());
                for (int h = 0; h <= 5; h++) {
                    f.setzen(l, b, h, Bau.LUFT);
                }
            }
        }
        int hoehe = 22 + r.nextInt(5);
        for (int h = 0; h <= hoehe; h++) {
            int halb = h < 8 ? 2 : h < 16 ? 1 : 0;
            for (int l = -halb; l <= halb; l += Math.max(1, halb * 2)) {
                for (int b = -halb; b <= halb; b += Math.max(1, halb * 2)) {
                    f.setzen(l, b, h, gitter);
                }
            }
            if (h == 7 || h == 15) {
                for (int l = -halb; l <= halb; l++) {
                    for (int b = -halb; b <= halb; b++) {
                        f.setzen(l, b, h, r.nextInt(6) == 0 ? Bau.LUFT : platte);
                    }
                }
            }
        }
        f.setzen(0, 0, hoehe + 1, Blocks.RED_STAINED_GLASS.defaultBlockState());
        f.setzen(0, 0, hoehe + 2, Blocks.LIGHTNING_ROD.defaultBlockState());
        f.setzen(0, 1, hoehe - 1, Blocks.SHROOMLIGHT.defaultBlockState()); // Warnlicht
        // Leiter innen bis zur ersten Plattform
        for (int h = 0; h <= 7; h++) {
            f.setzen(0, 0, h, Blocks.LADDER.defaultBlockState().setValue(net.minecraft.world.level.block.LadderBlock.FACING, f.quer()));
        }
        f.setzen(0, -1, 0, fundament);
        for (int h = 1; h <= 7; h++) {
            f.setzen(0, -1, h, Blocks.STONE_BRICKS.defaultBlockState());
        }
        // Funkerhütte: l 4..8, b -2..2
        for (int l = 4; l <= 8; l++) {
            for (int b = -2; b <= 2; b++) {
                boolean aussen = l == 4 || l == 8 || Math.abs(b) == 2;
                boolean tuer = l == 4 && b == 0;
                for (int h = 0; h <= 2; h++) {
                    if (aussen && !(tuer && h <= 1)) {
                        boolean fenster = h == 1 && Math.abs(b) == 2 && l == 6;
                        f.setzen(l, b, h, fenster ? Blocks.GLASS_PANE.defaultBlockState() : Blocks.STONE_BRICKS.defaultBlockState());
                    }
                }
                f.setzen(l, b, 3, r.nextInt(8) == 0 ? Bau.LUFT : Blocks.STONE_BRICK_SLAB.defaultBlockState());
            }
        }
        f.setzen(7, 1, 0, de.deadzonemc.funk.Funk.STATION.get().defaultBlockState()
                .setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING, f.quer().getOpposite()));
        f.kiste(7, -1, 0, "werkstatt", r, false);
        f.kiste(5, -1, 0, "versteck", r, true);
        f.setzen(5, 1, 0, Blocks.OAK_SLAB.defaultBlockState());
        // Kabeltrommeln und Sandsäcke draußen
        f.setzen(-3, 2, 0, Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState());
        f.setzen(-3, -2, 0, Blocks.MUD_BRICKS.defaultBlockState());
        f.setzen(-3, -3, 0, Blocks.MUD_BRICKS.defaultBlockState());
        spawnen(f, MonsterTypen.GEFALLENER_SOLDAT::get, 1 + r.nextInt(2), r);
    }

    /**
     * Checkpoint der Wacht: Straßensperre mit Schlagbaum, Sandsackstellungen, Stacheldraht, Wachhäuschen,
     * Suchscheinwerfer und Zelt. Besetzt von Soldaten der Wacht – freundlich, solange man mit ihr keinen Krieg hat.
     */
    private static void checkpoint(Rahmen f, RandomSource r) {
        BlockState strasse = Blocks.GRAY_CONCRETE.defaultBlockState();
        BlockState sand = Blocks.MUD_BRICKS.defaultBlockState();
        BlockState sandStufe = Blocks.MUD_BRICK_SLAB.defaultBlockState();
        for (int l = -11; l <= 11; l++) {
            for (int b = -7; b <= 7; b++) {
                boolean weg = Math.abs(b) <= 2;
                f.setzen(l, b, -1, weg ? (r.nextInt(10) == 0 ? Blocks.COBBLESTONE.defaultBlockState() : strasse)
                        : (r.nextInt(3) == 0 ? Blocks.COARSE_DIRT.defaultBlockState() : Blocks.GRAVEL.defaultBlockState()));
                f.stuetzen(l, b, -1, Blocks.DIRT.defaultBlockState());
                for (int h = 0; h <= 5; h++) {
                    f.setzen(l, b, h, Bau.LUFT);
                }
            }
        }
        // Mittellinie
        for (int l = -11; l <= 11; l += 3) {
            f.setzen(l, 0, -1, Blocks.YELLOW_CONCRETE.defaultBlockState());
        }
        // Schlagbaum quer über die Straße, rot-weiß
        f.setzen(0, -3, 0, Blocks.POLISHED_ANDESITE.defaultBlockState());
        f.setzen(0, -3, 1, Blocks.ANDESITE_WALL.defaultBlockState());
        for (int b = -2; b <= 2; b++) {
            f.setzen(0, b, 1, (b & 1) == 0 ? Blocks.RED_CONCRETE.defaultBlockState() : Blocks.WHITE_CONCRETE.defaultBlockState());
        }
        // Sandsackstellungen links und rechts der Sperre, mit Stacheldraht davor
        for (int seite : new int[]{-1, 1}) {
            for (int b = 3; b <= 6; b++) {
                int bb = seite * b;
                f.setzen(-3, bb, 0, sand);
                f.setzen(-3, bb, 1, sandStufe);
                f.setzen(3, bb, 0, sand);
                if (r.nextBoolean()) {
                    f.setzen(-5, bb, 0, Blocks.COBWEB.defaultBlockState());
                }
            }
        }
        for (int b = -2; b <= 2; b += 4) {
            f.setzen(-7, b, 0, Blocks.COBWEB.defaultBlockState()); // Draht auf der Straße, Durchgang in der Mitte
        }
        // Wachhäuschen: l 5..7, b 4..6
        for (int l = 5; l <= 7; l++) {
            for (int b = 4; b <= 6; b++) {
                boolean aussen = l == 5 || l == 7 || b == 4 || b == 6;
                boolean tuer = l == 6 && b == 6;
                for (int h = 0; h <= 2; h++) {
                    if (aussen && !(tuer && h <= 1)) {
                        boolean fenster = h == 1 && (b == 4 || l == 5);
                        f.setzen(l, b, h, fenster ? Blocks.GLASS_PANE.defaultBlockState() : Blocks.WHITE_CONCRETE.defaultBlockState());
                    }
                }
                f.setzen(l, b, 3, Blocks.GREEN_CONCRETE.defaultBlockState());
            }
        }
        f.kiste(6, 5, 0, "militaer", r, false);
        // Suchscheinwerfer
        for (int h = 0; h <= 3; h++) {
            f.setzen(9, -5, h, Blocks.IRON_BARS.defaultBlockState());
        }
        f.setzen(9, -5, 4, Blocks.SHROOMLIGHT.defaultBlockState());
        // Zelt: grünes Satteldach über Kisten
        for (int l = -10; l <= -7; l++) {
            for (int b = -6; b <= -4; b++) {
                int h = b == -5 ? 2 : 1;
                f.setzen(l, b, h, Blocks.GREEN_WOOL.defaultBlockState());
            }
        }
        f.kiste(-9, -5, 0, r.nextInt(4) == 0 ? "waffenkammer" : "militaer", r, true);
        f.kiste(-8, -5, 0, "apotheke", r, true);
        // Aufruf der Wacht an der Wachhütte
        f.setzen(5, 3, 1, de.deadzonemc.welt.deko.Wanddeko.PLAKAT.get().defaultBlockState()
                .setValue(net.minecraft.world.level.block.LadderBlock.FACING, f.quer().getOpposite())
                .setValue(de.deadzonemc.welt.deko.Wanddeko.Bild.MOTIV, 3));
        spawnen(f, de.deadzonemc.mensch.MenschTypen.SOLDAT::get, 2 + r.nextInt(2), r);
    }

    /** Für den Selbsttest: ohne Ortsprüfung an {@code boden} bauen. */
    public static void testBauen(WorldGenLevel w, RandomSource r, BlockPos boden, Art art) {
        Direction lang = Direction.Plane.HORIZONTAL.getRandomDirection(r);
        Rahmen f = new Rahmen(w, boden, lang, lang.getClockWise());
        switch (art) {
            case HUBSCHRAUBER -> hubschrauber(f, r);
            case FLUGZEUG -> flugzeug(f, r);
            case FRACHTER -> frachter(f, r, 1);
            case PANZER -> panzer(f, r, 0, 0, true);
            case KONVOI -> konvoi(f, r);
            case EVAKUIERUNG -> evakuierung(f, r);
            case RASTSTAETTE -> raststaette(f, r);
            case FUNKTURM -> funkturm(f, r);
            case CHECKPOINT -> checkpoint(f, r);
        }
    }

    // ---- Hubschrauber ---------------------------------------------------------------------

    private static void hubschrauber(Rahmen f, RandomSource r) {
        BlockState oliv = Blocks.GREEN_TERRACOTTA.defaultBlockState();
        BlockState dunkel = Blocks.POLISHED_BLACKSTONE.defaultBlockState();
        BlockState stufe = Blocks.POLISHED_BLACKSTONE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM);
        BlockState glas = Blocks.GRAY_STAINED_GLASS_PANE.defaultBlockState();
        BlockState eisen = Blocks.IRON_BLOCK.defaultBlockState();
        krater(f, r, 7);
        // Rumpf l -3..3, b -1..1, h 0..3 (leicht zur Seite gekippt: rechte Seite eine Stufe tiefer)
        for (int l = -3; l <= 3; l++) {
            for (int b = -1; b <= 1; b++) {
                int sink = b == 1 ? 0 : 0;
                f.setzen(l, b, 0 - sink, oliv);
                f.stuetzen(l, b, 0, Blocks.COARSE_DIRT.defaultBlockState());
                for (int h = 1; h <= 2; h++) {
                    boolean wand = b != 0 || l == 3;
                    boolean tuer = b == -1 && (l == 0 || l == 1); // offene Schiebetür
                    f.setzen(l, b, h, wand && !tuer ? (r.nextInt(6) == 0 ? dunkel : oliv) : Bau.LUFT);
                }
                f.setzen(l, b, 3, r.nextInt(7) == 0 ? Bau.LUFT : oliv);
            }
        }
        // Cockpit vorn (l = -4) mit gesplitterter Kanzel
        for (int b = -1; b <= 1; b++) {
            f.setzen(-4, b, 0, oliv);
            f.setzen(-4, b, 1, r.nextInt(3) == 0 ? Bau.LUFT : glas);
            f.setzen(-4, b, 2, r.nextInt(2) == 0 ? Bau.LUFT : glas);
            f.setzen(-5, b, 0, stufe);
        }
        // Heckausleger und Leitwerk
        for (int l = 4; l <= 10; l++) {
            f.setzen(l, 0, 2, l > 8 && r.nextBoolean() ? dunkel : oliv);
        }
        f.setzen(10, 0, 3, oliv);
        f.setzen(10, 0, 4, oliv);
        f.setzen(10, 1, 3, Blocks.IRON_BARS.defaultBlockState());
        f.setzen(10, -1, 3, Blocks.IRON_BARS.defaultBlockState());
        // Rotormast, ein Blatt noch dran, die anderen liegen verstreut
        f.setzen(0, 0, 4, eisen);
        for (int b = 1; b <= 5; b++) {
            f.setzen(0, b, 5 - (b > 3 ? 1 : 0), stufe);
        }
        for (int i = 0; i < 3; i++) {
            int l0 = -8 + r.nextInt(16);
            int b0 = r.nextBoolean() ? 4 + r.nextInt(5) : -4 - r.nextInt(5);
            for (int k = 0; k < 4; k++) {
                BlockPos p = f.pos(l0 + (i % 2 == 0 ? k : 0), b0 + (i % 2 == 0 ? 0 : k), 0);
                int y = f.w().getHeight(Heightmap.Types.WORLD_SURFACE_WG, p.getX(), p.getZ());
                f.w().setBlock(new BlockPos(p.getX(), y, p.getZ()), stufe, Block.UPDATE_CLIENTS);
            }
        }
        // Brennendes Triebwerk: Rauchsäule, die man von weit her sieht
        f.setzen(2, 0, 3, Blocks.HAY_BLOCK.defaultBlockState());
        f.setzen(2, 0, 4, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true)
                .setValue(CampfireBlock.SIGNAL_FIRE, true));
        // Beute: Militärkisten, mit Glück die Waffenkammer
        f.kiste(-2, 0, 1, "militaer", r, true);
        f.kiste(2, 0, 1, r.nextInt(3) == 0 ? "waffenkammer" : "militaer", r, false);
        f.kiste(-3, 1, 1, "apotheke", r, true);
        spawnen(f, MonsterTypen.GEFALLENER_SOLDAT::get, 1 + r.nextInt(2), r);
    }

    // ---- Flugzeug -------------------------------------------------------------------------

    private static void flugzeug(Rahmen f, RandomSource r) {
        BlockState weiss = Blocks.WHITE_CONCRETE.defaultBlockState();
        BlockState grau = Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState();
        BlockState rot = Blocks.RED_CONCRETE.defaultBlockState();
        BlockState schwarz = Blocks.POLISHED_BLACKSTONE.defaultBlockState();
        BlockState fenster = Blocks.LIGHT_BLUE_STAINED_GLASS_PANE.defaultBlockState();
        BlockState flUegel = Blocks.SMOOTH_STONE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP);
        BlockState sitz = Blocks.BLUE_TERRACOTTA.defaultBlockState();
        krater(f, r, 9);
        // Rumpf l -11..11 (Bruch bei l 0..1), Querschnitt b -2..2, h 0..4; das Heck ist versetzt
        for (int l = -11; l <= 11; l++) {
            if (l == 0 || l == 1) {
                continue; // abgebrochen
            }
            int versatz = l > 1 ? 1 : 0;
            boolean kaputt = l > 8 || l < -9;
            for (int b = -2; b <= 2; b++) {
                int bb = b + versatz;
                for (int h = 0; h <= 4; h++) {
                    boolean rand = Math.abs(b) == 2 || h == 0 || h == 4;
                    BlockState s;
                    if (!rand) {
                        s = Bau.LUFT;
                    } else if (h == 2 && Math.abs(b) == 2 && l % 2 == 0) {
                        s = r.nextInt(3) == 0 ? Bau.LUFT : fenster;
                    } else if (h == 1 && Math.abs(b) == 2) {
                        s = rot; // Zierstreifen
                    } else if (h == 0) {
                        s = grau;
                    } else {
                        s = kaputt && r.nextInt(3) == 0 ? Bau.LUFT : (r.nextInt(14) == 0 ? schwarz : weiss);
                    }
                    if ((Math.abs(b) == 2 || h == 4) && (l == 2 || l == -1) && r.nextInt(2) == 0) {
                        s = Bau.LUFT; // ausgefranste Bruchkanten
                    }
                    f.setzen(l, bb, h, s);
                }
                f.stuetzen(l, bb, 0, Blocks.COARSE_DIRT.defaultBlockState());
            }
            // Sitzreihen
            if (l % 2 != 0 && l > -9 && l < 10) {
                f.setzen(l, -1 + versatz, 1, sitz);
                f.setzen(l, 1 + versatz, 1, sitz);
            }
        }
        // Nase und Cockpit
        for (int b = -1; b <= 1; b++) {
            f.setzen(-12, b, 1, weiss);
            f.setzen(-12, b, 2, fenster);
            f.setzen(-12, b, 3, weiss);
        }
        // Leitwerk am Heck
        for (int h = 5; h <= 8; h++) {
            f.setzen(11, 1, h, h == 7 ? rot : weiss);
            f.setzen(10, 1, h, h <= 6 ? weiss : Bau.LUFT);
        }
        // Flügel: links ganz, rechts abgerissen; Triebwerke darunter
        for (int l = -5; l <= -2; l++) {
            for (int b = 3; b <= 11; b++) {
                if (b > 11 - (l + 5)) {
                    continue;
                }
                f.setzen(l, -b, 1, flUegel);
            }
            for (int b = 3; b <= 6; b++) {
                f.setzen(l, b, 1, flUegel);
            }
        }
        for (int l = -5; l <= -3; l++) {
            f.setzen(l, -6, 0, Blocks.IRON_BLOCK.defaultBlockState());
        }
        f.setzen(-6, -6, 0, schwarz);
        // abgerissener Flügel liegt daneben
        for (int l = 4; l <= 8; l++) {
            for (int b = 7; b <= 9; b++) {
                BlockPos p = f.pos(l, b, 0);
                int y = f.w().getHeight(Heightmap.Types.WORLD_SURFACE_WG, p.getX(), p.getZ());
                f.w().setBlock(new BlockPos(p.getX(), y, p.getZ()), Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
        }
        f.setzen(0, -3, 0, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true)
                .setValue(CampfireBlock.SIGNAL_FIRE, true));
        f.setzen(0, -3, -1, Blocks.HAY_BLOCK.defaultBlockState());
        // Gepäck
        String[] gepaeck = {"kleiderschrank", "nachttisch", "apotheke", "wohnzimmer", "badezimmer"};
        for (int i = 0; i < 4; i++) {
            int l = -8 + r.nextInt(17);
            if (l == 0 || l == 1) {
                l = 3;
            }
            f.kiste(l, (l > 1 ? 1 : 0), 1, gepaeck[r.nextInt(gepaeck.length)], r, false);
        }
        f.kiste(-11, 0, 1, "buero", r, true);
        spawnen(f, MonsterTypen.WANDERER::get, 3 + r.nextInt(3), r);
    }

    // ---- Frachter -------------------------------------------------------------------------

    private static void frachter(Rahmen f, RandomSource r, int wassertiefe) {
        BlockState rost = Blocks.TERRACOTTA.defaultBlockState();
        BlockState rost2 = Blocks.BROWN_TERRACOTTA.defaultBlockState();
        BlockState rumpf = Blocks.RED_TERRACOTTA.defaultBlockState();
        BlockState deck = Blocks.EXPOSED_CUT_COPPER.defaultBlockState();
        BlockState weiss = Blocks.WHITE_TERRACOTTA.defaultBlockState();
        BlockState fenster = Blocks.BLACK_STAINED_GLASS_PANE.defaultBlockState();
        int deckH = Math.max(4, wassertiefe + 3);
        // Rumpf l -13..13, b -4..4 – zum Bug hin schmaler, Schlagseite: linke Seite höher
        for (int l = -13; l <= 13; l++) {
            int halb = l < -9 ? 4 - (-9 - l) : 4;
            for (int b = -halb; b <= halb; b++) {
                int neig = b < 0 ? 1 : 0;
                for (int h = 0; h <= deckH; h++) {
                    boolean aussen = Math.abs(b) == halb || h == 0 || l == 13 || l == -13;
                    BlockState s = aussen ? (h < 2 ? rumpf : (r.nextInt(4) == 0 ? rost2 : rost)) : Bau.LUFT;
                    if (!aussen && h <= wassertiefe) {
                        s = Blocks.WATER.defaultBlockState();
                    }
                    if (aussen && h > 1 && r.nextInt(18) == 0) {
                        s = Bau.LUFT; // Rostlöcher
                    }
                    f.setzen(l, b, h + neig, s);
                }
                f.setzen(l, b, deckH + 1 + neig, deck);
                f.stuetzen(l, b, neig, rost2);
            }
        }
        // Brücke am Heck
        for (int l = 8; l <= 12; l++) {
            for (int b = -3; b <= 3; b++) {
                for (int h = deckH + 2; h <= deckH + 6; h++) {
                    boolean rand = Math.abs(b) == 3 || l == 8 || l == 12 || h == deckH + 6;
                    BlockState s = !rand ? Bau.LUFT : (h == deckH + 5 && (l == 8 || Math.abs(b) == 3)) ? fenster : weiss;
                    f.setzen(l, b, h + (b < 0 ? 1 : 0), s);
                }
            }
        }
        f.kiste(10, 0, deckH + 2, "werkzeug", r, false);
        f.kiste(11, -1, deckH + 3, "militaer", r, true);
        // Container auf Deck (2 breit, 2 hoch, 5 lang) mit Ware
        BlockState[] farben = {Blocks.RED_CONCRETE.defaultBlockState(), Blocks.BLUE_CONCRETE.defaultBlockState(),
                Blocks.ORANGE_CONCRETE.defaultBlockState(), Blocks.GREEN_CONCRETE.defaultBlockState(),
                Blocks.CYAN_CONCRETE.defaultBlockState()};
        String[] ware = {"laden_lager", "werkstatt", "baumarkt", "laden_regal", "militaer"};
        for (int reihe = 0; reihe < 2; reihe++) {
            for (int i = 0; i < 2; i++) {
                if (r.nextInt(5) == 0) {
                    continue; // über Bord gegangen
                }
                BlockState c = farben[r.nextInt(farben.length)];
                int l0 = -8 + i * 7;
                int b0 = reihe == 0 ? -3 : 1;
                for (int l = l0; l < l0 + 5; l++) {
                    for (int b = b0; b < b0 + 2; b++) {
                        for (int h = 0; h < 2; h++) {
                            boolean innen = l > l0 && l < l0 + 4 && h == 0;
                            f.setzen(l, b, deckH + 2 + h + (b < 0 ? 1 : 0), innen ? Bau.LUFT : c);
                        }
                    }
                }
                f.setzen(l0 + 4, b0, deckH + 2 + (b0 < 0 ? 1 : 0), Bau.LUFT); // Tür offen
                f.kiste(l0 + 2, b0, deckH + 2 + (b0 < 0 ? 1 : 0), ware[r.nextInt(ware.length)], r, true);
            }
        }
        spawnen(f, MonsterTypen.WANDERER::get, 2 + r.nextInt(2), r);
    }

    // ---- Panzerwrack und Konvoi ----------------------------------------------------------------

    /**
     * Ein alter Kampfpanzer, Jahrzehnte alt oder in den ersten Tagen der Abriegelung liegen geblieben: Wanne mit
     * Ketten und Laufrollen, Turm mit Kanone (oft zur Seite gedreht), offene Luke, Rost, Moos, Ranken.
     * Im Inneren: Munitionskisten.
     */
    private static void panzer(Rahmen f, RandomSource r, int l0, int b0, boolean einzeln) {
        boolean alt = r.nextInt(3) > 0; // alt = überwuchert, sonst ausgebrannt
        BlockState gruen = Blocks.GREEN_TERRACOTTA.defaultBlockState();
        BlockState oliv = Blocks.MOSS_BLOCK.defaultBlockState();
        BlockState rost = Blocks.BROWN_TERRACOTTA.defaultBlockState();
        BlockState russ = Blocks.POLISHED_BLACKSTONE.defaultBlockState();
        BlockState kette = Blocks.POLISHED_DEEPSLATE.defaultBlockState();
        BlockState rolle = Blocks.DEEPSLATE_TILES.defaultBlockState();
        BlockState stufeK = Blocks.POLISHED_DEEPSLATE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM);
        java.util.function.Supplier<BlockState> haut = () -> {
            if (!alt) {
                return r.nextInt(3) == 0 ? rost : russ;
            }
            int z = r.nextInt(10);
            return z < 6 ? gruen : z < 8 ? rost : oliv;
        };
        if (einzeln) {
            krater(f, r, 6);
        }
        // Wanne l -4..4, b -2..2 (Ketten auf b = ±2)
        for (int l = -4; l <= 4; l++) {
            for (int b = -2; b <= 2; b++) {
                int ll = l0 + l;
                int bb = b0 + b;
                boolean kettenseite = Math.abs(b) == 2;
                if (kettenseite) {
                    f.setzen(ll, bb, 0, (l == -4 || l == 4) ? stufeK : (l % 2 == 0 ? rolle : kette));
                    f.setzen(ll, bb, 1, (l == -4 || l == 4) ? Bau.LUFT : kette);
                } else {
                    f.setzen(ll, bb, 0, kette);
                    f.setzen(ll, bb, 1, haut.get());
                    f.setzen(ll, bb, 2, (l == 4) ? (alt ? Blocks.MOSSY_COBBLESTONE_SLAB : Blocks.BLACKSTONE_SLAB)
                            .defaultBlockState() : haut.get());
                }
                f.stuetzen(ll, bb, 0, Blocks.COARSE_DIRT.defaultBlockState());
            }
        }
        // Bug (schräge Front)
        for (int b = -1; b <= 1; b++) {
            f.setzen(l0 - 5, b0 + b, 1, (alt ? Blocks.MOSSY_STONE_BRICK_SLAB : Blocks.BLACKSTONE_SLAB).defaultBlockState());
            f.setzen(l0 - 5, b0 + b, 0, haut.get());
            f.stuetzen(l0 - 5, b0 + b, 0, Blocks.COARSE_DIRT.defaultBlockState());
        }
        // Innenraum mit Munitionskiste (unter der Luke)
        f.setzen(l0 + 1, b0, 2, Bau.LUFT);
        f.setzen(l0 + 1, b0, 1, Bau.LUFT);
        f.kiste(l0, b0, 1, "militaer", r, true);
        // Turm 3x3 mit Kanone, oft verdreht
        boolean quer = r.nextInt(3) == 0;
        for (int l = -1; l <= 1; l++) {
            for (int b = -1; b <= 1; b++) {
                f.setzen(l0 + 1 + l, b0 + b, 3, (l == 0 && b == 0) ? Blocks.IRON_TRAPDOOR.defaultBlockState()
                        .setValue(net.minecraft.world.level.block.TrapDoorBlock.OPEN, true) : haut.get());
            }
        }
        net.minecraft.core.Direction.Axis achse = quer ? f.quer().getAxis() : f.lang().getAxis();
        BlockState rohr = Blocks.POLISHED_BASALT.defaultBlockState().setValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS, achse);
        for (int i = 2; i <= 6; i++) {
            if (quer) {
                f.setzen(l0 + 1, b0 + i, 3, rohr);
            } else {
                f.setzen(l0 + 1 - i, b0, 3, rohr);
            }
        }
        if (alt) {
            for (int i = 0; i < 6; i++) {
                int l = l0 + r.nextInt(7) - 3;
                int b = b0 + (r.nextBoolean() ? 3 : -3);
                f.setzen(l, b, 1, Blocks.VINE.defaultBlockState()
                        .setValue(b > b0 ? net.minecraft.world.level.block.VineBlock.getPropertyForFace(f.quer().getOpposite())
                                : net.minecraft.world.level.block.VineBlock.getPropertyForFace(f.quer()), true));
            }
            f.setzen(l0 + 2, b0 - 1, 4, Blocks.MOSS_CARPET.defaultBlockState());
        } else {
            f.setzen(l0, b0 + 1, 4, Blocks.SKELETON_SKULL.defaultBlockState());
            f.setzen(l0 + 1, b0, 2, Blocks.COBWEB.defaultBlockState());
        }
    }

    /** Liegengebliebener Konvoi: zwei Panzer und ein Lastwagen hintereinander, dazwischen Kisten und Sandsäcke. */
    private static void konvoi(Rahmen f, RandomSource r) {
        krater(f, r, 9);
        panzer(f, r, -9, 0, false);
        panzer(f, r, 8, 1, false);
        // Lastwagen in der Mitte: Fahrerhaus + Ladefläche mit Plane
        BlockState lack = Blocks.GREEN_TERRACOTTA.defaultBlockState();
        BlockState plane = Blocks.GREEN_WOOL.defaultBlockState();
        for (int l = -2; l <= 3; l++) {
            for (int b = -1; b <= 1; b++) {
                f.setzen(l, b, 0, (l == -1 || l == 2) && b != 0 ? Blocks.BLACK_CONCRETE.defaultBlockState() : lack);
                f.stuetzen(l, b, 0, Blocks.COARSE_DIRT.defaultBlockState());
                if (l <= -1) {
                    // Fahrerhaus: unten Blech, oben Frontscheibe (zersplittert) und Dach
                    f.setzen(l, b, 1, lack);
                    f.setzen(l, b, 2, l == -2 ? (r.nextInt(3) == 0 ? Bau.LUFT : Blocks.GRAY_STAINED_GLASS_PANE.defaultBlockState())
                            : (b == 0 ? Bau.LUFT : Blocks.GRAY_STAINED_GLASS_PANE.defaultBlockState()));
                    f.setzen(l, b, 3, lack);
                } else {
                    // Ladefläche mit zerrissener Plane über Spriegeln
                    boolean rand = Math.abs(b) == 1 || l == 3;
                    f.setzen(l, b, 1, rand && r.nextInt(4) > 0 ? plane : Bau.LUFT);
                    f.setzen(l, b, 2, rand && r.nextInt(3) > 0 ? plane : Bau.LUFT);
                    f.setzen(l, b, 3, r.nextInt(4) > 0 ? plane : Bau.LUFT);
                }
            }
        }
        f.setzen(-3, -1, 1, Blocks.REDSTONE_LAMP.defaultBlockState()); // Scheinwerfer
        f.setzen(-3, 1, 1, Blocks.REDSTONE_LAMP.defaultBlockState());
        f.kiste(1, 0, 1, "militaer", r, true);
        f.kiste(2, 0, 1, r.nextInt(3) == 0 ? "waffenkammer" : "apotheke", r, false);
        // Sandsäcke
        for (int i = 0; i < 6; i++) {
            BlockPos p = f.pos(-4 + r.nextInt(9), r.nextBoolean() ? 4 : -4, 0);
            int y = f.w().getHeight(Heightmap.Types.WORLD_SURFACE_WG, p.getX(), p.getZ());
            f.w().setBlock(new BlockPos(p.getX(), y, p.getZ()), Blocks.MUD_BRICK_SLAB.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
        spawnen(f, de.deadzonemc.monster.MonsterTypen.GEFALLENER_SOLDAT::get, 2 + r.nextInt(2), r);
    }

    // ---- Evakuierungslager -----------------------------------------------------------------

    /**
     * Ein überrannter Evakuierungspunkt: Bauzaun mit Lücken, zwei Sanitätszelte mit rotem Kreuz, ein Wachturm,
     * Sandsäcke am Tor, Leichensäcke – und zwischen ihnen liegen nicht alle so still, wie sie aussehen.
     * Beute: Medizin in den Zelten, Militärkiste oben auf dem Turm, Vorräte an der Feldküche.
     */
    private static void evakuierung(Rahmen f, RandomSource r) {
        int R = 10;
        java.util.List<BlockPos> zaun = new java.util.ArrayList<>();
        // Boden: festgetretener Schotter und Erde
        for (int l = -R; l <= R; l++) {
            for (int b = -R; b <= R; b++) {
                BlockPos p = f.pos(l, b, 0);
                int y = f.w().getHeight(Heightmap.Types.WORLD_SURFACE_WG, p.getX(), p.getZ()) - 1;
                BlockPos u = new BlockPos(p.getX(), y, p.getZ());
                if (f.w().getBlockState(u).is(Blocks.GRASS_BLOCK) && r.nextInt(3) > 0) {
                    f.w().setBlock(u, r.nextInt(3) == 0 ? Blocks.GRAVEL.defaultBlockState() : Blocks.COARSE_DIRT.defaultBlockState(),
                            Block.UPDATE_CLIENTS);
                }
                BlockState oben = f.w().getBlockState(u.above());
                if (!oben.isAir() && oben.canBeReplaced()) {
                    f.w().setBlock(u.above(), Bau.LUFT, Block.UPDATE_CLIENTS);
                }
                // Bauzaun am Rand, mit Lücken und einem Tor
                boolean rand = Math.abs(l) == R || Math.abs(b) == R;
                boolean tor = l == -R && Math.abs(b) <= 1;
                if (rand && !tor && r.nextInt(5) > 0) {
                    // Der Zaun folgt dem Gelände
                    f.w().setBlock(u.above(), Blocks.IRON_BARS.defaultBlockState(), Block.UPDATE_CLIENTS);
                    zaun.add(u.above());
                    if (r.nextInt(3) > 0) {
                        f.w().setBlock(u.above(2), Blocks.IRON_BARS.defaultBlockState(), Block.UPDATE_CLIENTS);
                        zaun.add(u.above(2));
                    }
                }
            }
        }
        // Sandsäcke am Tor
        for (int b = -3; b <= 3; b++) {
            if (Math.abs(b) >= 2) {
                f.setzen(-R - 1, b, 0, Blocks.MUD_BRICKS.defaultBlockState());
                f.setzen(-R - 1, b, 1, Blocks.MUD_BRICK_SLAB.defaultBlockState());
            }
        }
        // Zwei Sanitätszelte
        sanizelt(f, r, -7, -1, -6);
        sanizelt(f, r, 1, 6, -6);
        // Leichensäcke zwischen den Zelten – einer davon lebt noch
        for (int i = 0; i < 5; i++) {
            int l = -6 + i * 3;
            f.setzen(l, 0, 0, Blocks.BLACK_CARPET.defaultBlockState());
            f.setzen(l + 1, 0, 0, Blocks.BLACK_CARPET.defaultBlockState());
        }
        // Wachturm in der Ecke
        for (int h = 0; h <= 4; h++) {
            for (int[] e : new int[][]{{6, 6}, {6, 9}, {9, 6}, {9, 9}}) {
                f.setzen(e[0], e[1], h, Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState());
                f.stuetzen(e[0], e[1], 0, Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState());
            }
        }
        for (int l = 6; l <= 9; l++) {
            for (int b = 6; b <= 9; b++) {
                f.setzen(l, b, 5, Blocks.SPRUCE_PLANKS.defaultBlockState());
                if (l == 6 || l == 9 || b == 6 || b == 9) {
                    f.setzen(l, b, 6, Blocks.SPRUCE_FENCE.defaultBlockState());
                    zaun.add(f.pos(l, b, 6));
                }
            }
        }
        for (int h = 0; h <= 5; h++) {
            f.setzen(7, 6, h, Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState()); // Leiterpfosten
        }
        f.stuetzen(7, 6, 0, Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState());
        f.setzen(7, 6, 6, Bau.LUFT); // Einstieg
        for (int h = 0; h <= 5; h++) {
            f.setzen(7, 5, h, Blocks.LADDER.defaultBlockState()
                    .setValue(net.minecraft.world.level.block.LadderBlock.FACING, f.quer().getOpposite()));
        }
        f.kiste(8, 8, 6, "militaer", r, true);
        f.setzen(8, 7, 6, Blocks.LANTERN.defaultBlockState());
        // Feldküche mit erloschenem Feuer
        f.setzen(-3, 6, 0, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, false));
        f.setzen(-2, 6, 0, Blocks.WATER_CAULDRON.defaultBlockState());
        f.kiste(-4, 7, 0, r.nextBoolean() ? "kueche" : "kuehlschrank", r, true);
        f.setzen(-2, 7, 0, Blocks.SPRUCE_SLAB.defaultBlockState());
        // Verbindungen von Zaun und Geländer nachziehen
        for (BlockPos p : zaun) {
            BlockState s = f.w().getBlockState(p);
            if (!s.isAir()) {
                f.w().setBlock(p, Block.updateFromNeighbourShapes(s, f.w(), p), Block.UPDATE_CLIENTS);
            }
        }
        spawnen(f, MonsterTypen.SCHEINTOTER::get, 1, r);
        spawnen(f, MonsterTypen.WANDERER::get, 3 + r.nextInt(3), r);
        if (r.nextInt(3) == 0) {
            spawnen(f, MonsterTypen.SEUCHENTRAEGER::get, 1, r);
        }
    }

    /** Sanitätszelt (Satteldach aus weißer Wolle) von l0 bis l1, Firstlinie bei b = c, mit rotem Kreuz. */
    private static void sanizelt(Rahmen f, RandomSource r, int l0, int l1, int c) {
        BlockState wolle = Blocks.WHITE_WOOL.defaultBlockState();
        for (int l = l0; l <= l1; l++) {
            for (int d = -2; d <= 2; d++) {
                int h = 2 - Math.abs(d);
                boolean riss = r.nextInt(9) == 0;
                f.setzen(l, c + d, h, riss ? Bau.LUFT : wolle);
                if (Math.abs(d) == 2 && (l == l0 || l == l1)) {
                    f.setzen(l, c + d, 0, wolle);
                }
            }
            // Pritschen innen
            if (l != l0 && l != l1 && (l - l0) % 2 == 1) {
                f.setzen(l, c - 1, 0, Blocks.WHITE_CARPET.defaultBlockState());
                f.setzen(l, c + 1, 0, Blocks.WHITE_CARPET.defaultBlockState());
            }
        }
        // Rotes Kreuz auf dem Dach
        int m = (l0 + l1) / 2;
        f.setzen(m, c, 2, Blocks.RED_WOOL.defaultBlockState());
        f.setzen(m - 1, c, 2, Blocks.RED_WOOL.defaultBlockState());
        f.setzen(m + 1, c, 2, Blocks.RED_WOOL.defaultBlockState());
        f.setzen(m, c - 1, 1, Blocks.RED_WOOL.defaultBlockState());
        f.setzen(m, c + 1, 1, Blocks.RED_WOOL.defaultBlockState());
        f.kiste(l1 - 1, c, 0, "apotheke", r, false);
    }

    // ---- Hilfen ---------------------------------------------------------------------------

    /** Aufgewühlter, verbrannter Boden rund um die Absturzstelle. */
    private static void krater(Rahmen f, RandomSource r, int radius) {
        WorldGenLevel w = f.w();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz > radius * radius || r.nextInt(3) == 0) {
                    continue;
                }
                int x = f.o().getX() + dx;
                int z = f.o().getZ() + dz;
                int y = w.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
                BlockPos p = new BlockPos(x, y, z);
                BlockState alt = w.getBlockState(p);
                if (alt.is(Blocks.GRASS_BLOCK) || alt.is(Blocks.DIRT) || alt.is(Blocks.PODZOL) || alt.is(Blocks.SAND)
                        || alt.is(Blocks.SNOW_BLOCK)) {
                    int z2 = r.nextInt(6);
                    w.setBlock(p, z2 < 3 ? Blocks.COARSE_DIRT.defaultBlockState() : z2 < 5 ? Blocks.GRAVEL.defaultBlockState()
                            : Blocks.BLACKSTONE.defaultBlockState(), Block.UPDATE_CLIENTS);
                    BlockState oben = w.getBlockState(p.above());
                    if (!oben.isAir() && oben.canBeReplaced()) {
                        w.setBlock(p.above(), Bau.LUFT, Block.UPDATE_CLIENTS);
                    }
                }
            }
        }
    }

    private static void spawnen(Rahmen f, Supplier<? extends EntityType<? extends Mob>> typ, int anzahl, RandomSource r) {
        WorldGenLevel w = f.w();
        for (int i = 0; i < anzahl; i++) {
            Mob m = typ.get().create(w.getLevel());
            if (m == null) {
                return;
            }
            BlockPos p = f.pos(-6 + r.nextInt(12), r.nextBoolean() ? 3 + r.nextInt(3) : -3 - r.nextInt(3), 0);
            int y = w.getHeight(Heightmap.Types.WORLD_SURFACE_WG, p.getX(), p.getZ());
            m.moveTo(p.getX() + 0.5, y, p.getZ() + 0.5, r.nextFloat() * 360, 0);
            m.setPersistenceRequired();
            m.finalizeSpawn(w, w.getCurrentDifficultyAt(m.blockPosition()), MobSpawnType.STRUCTURE, null);
            w.addFreshEntityWithPassengers(m);
        }
    }

    @SuppressWarnings("unused")
    private static BlockState treppe(Block b, Direction d) {
        return b.defaultBlockState().setValue(StairBlock.FACING, d);
    }
}
