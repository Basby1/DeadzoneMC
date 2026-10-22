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

    public enum Art { HUBSCHRAUBER, FLUGZEUG, FRACHTER, PANZER, KONVOI, EVAKUIERUNG, RASTSTAETTE, FUNKTURM, CHECKPOINT, STAUSEEDORF, BUNKERLABOR }

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
        int y = bodenHoehe(w, cx, cz);
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
            case STAUSEEDORF -> stauseedorf(f, r);
            case BUNKERLABOR -> bunkerlabor(f, r);
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

    /**
     * Stausee-Dorf: Hinter einer Staumauer ist das Tal vollgelaufen. Häuser stehen bis zu den Fenstern im Wasser,
     * die Dachböden sind noch trocken (Beute). Ein Kirchturm ragt heraus. Im Wasser treiben Ertrunkene.
     */
    private static void stauseedorf(Rahmen f, RandomSource r) {
        BlockState wasser = Blocks.WATER.defaultBlockState();
        BlockState schlamm = Blocks.MUD.defaultBlockState();
        BlockState beton = Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState();
        int R = 13;
        // Becken: 3 tief, Rand aus Schlamm, Wasser bis h -1
        for (int l = -R; l <= R; l++) {
            for (int b = -R; b <= R; b++) {
                double d = Math.sqrt(l * l + b * b);
                if (d > R) {
                    continue;
                }
                boolean rand = d > R - 1.5;
                f.setzen(l, b, -4, schlamm);
                for (int h = -3; h <= -1; h++) {
                    f.setzen(l, b, h, rand ? schlamm : wasser);
                }
                for (int h = 0; h <= 8; h++) {
                    f.setzen(l, b, h, Bau.LUFT);
                }
                if (!rand && r.nextInt(14) == 0) {
                    f.setzen(l, b, -3, Blocks.SEAGRASS.defaultBlockState());
                }
            }
        }
        // Staumauer an einer Seite, mit Überlauf in der Mitte
        for (int b = -R; b <= R; b++) {
            for (int h = -3; h <= 3; h++) {
                boolean ueberlauf = Math.abs(b) <= 1 && h >= 0;
                f.setzen(R, b, h, ueberlauf ? Bau.LUFT : beton);
                f.setzen(R + 1, b, h, ueberlauf ? Bau.LUFT : beton);
            }
            f.stuetzen(R, b, -3, beton);
            f.stuetzen(R + 1, b, -3, beton);
            f.setzen(R + 1, b, 4, Blocks.IRON_BARS.defaultBlockState()); // Geländer
        }
        // Häuser im Wasser: Erdgeschoss geflutet, Dachboden trocken
        int[][] haeuser = {{-7, -6}, {-6, 4}, {1, -8}, {3, 5}};
        for (int[] hs : haeuser) {
            int l0 = hs[0];
            int b0 = hs[1];
            BlockState mauer = r.nextBoolean() ? Blocks.BRICKS.defaultBlockState() : Blocks.STONE_BRICKS.defaultBlockState();
            for (int l = l0; l <= l0 + 4; l++) {
                for (int b = b0; b <= b0 + 4; b++) {
                    boolean aussen = l == l0 || l == l0 + 4 || b == b0 || b == b0 + 4;
                    for (int h = -3; h <= 2; h++) {
                        if (aussen) {
                            boolean fenster = h == 1 && (l == l0 + 2 || b == b0 + 2);
                            f.setzen(l, b, h, fenster ? (r.nextBoolean() ? Bau.LUFT : Blocks.GLASS_PANE.defaultBlockState()) : mauer);
                        }
                    }
                    f.setzen(l, b, -4, Blocks.STONE_BRICKS.defaultBlockState());
                    if (!aussen) {
                        f.setzen(l, b, 3, Blocks.SPRUCE_PLANKS.defaultBlockState()); // Dachbodenboden
                    }
                    // Satteldach
                    int dachH = 4 + Math.min(Math.abs(b - b0), Math.abs(b0 + 4 - b)) ;
                    if (dachH <= 6 && r.nextInt(8) > 0) {
                        f.setzen(l, b, Math.min(dachH, 6), Blocks.DARK_OAK_PLANKS.defaultBlockState());
                    }
                }
            }
            f.setzen(l0 + 1, b0 + 1, 4, Bau.LUFT);
            f.kiste(l0 + 2, b0 + 2, 4, r.nextBoolean() ? "kleiderschrank" : "stadt_wohnung", r, false);
            f.setzen(l0 + 2, b0, 0, Bau.LUFT); // Tür zum Hineinschwimmen
            f.setzen(l0 + 2, b0, -1, wasser);
        }
        // Kirchturm ragt aus dem Wasser
        for (int h = -3; h <= 10; h++) {
            for (int l = -2; l <= 0; l++) {
                for (int b = -1; b <= 1; b++) {
                    boolean aussen = l == -2 || l == 0 || Math.abs(b) == 1;
                    boolean schall = h == 8 && (l == -1 || b == 0);
                    if (aussen && !schall) {
                        f.setzen(l, b, h, Blocks.COBBLESTONE.defaultBlockState());
                    } else if (!aussen && h >= 0) {
                        f.setzen(l, b, h, h == 8 ? Blocks.BELL.defaultBlockState() : Bau.LUFT);
                    }
                }
            }
        }
        f.setzen(-1, 0, 11, Blocks.COBBLESTONE_WALL.defaultBlockState());
        f.kiste(-1, 0, 7, "kirche", r, true);
        f.setzen(-1, 0, 6, Blocks.SPRUCE_SLAB.defaultBlockState());
        // Bootssteg am Ufer
        for (int l = -12; l <= -9; l++) {
            f.setzen(l, 10, -1, Blocks.SPRUCE_SLAB.defaultBlockState());
        }
        spawnen(f, MonsterTypen.ERTRUNKENER::get, 3, r);
    }

    /**
     * Bunker-Labor: An der Oberfläche nur eine rostige Luke im Betonsockel. Darunter ein Schacht, 14 Blöcke tief,
     * ein Gang mit drei Laborräumen: Versuchstanks, Rechner, Kühlschränke, Akten. Notbeleuchtung flackert, und
     * etwas aus den Tanks ist entkommen.
     */
    private static void bunkerlabor(Rahmen f, RandomSource r) {
        BlockState wand = Blocks.WHITE_CONCRETE.defaultBlockState();
        BlockState boden = Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState();
        BlockState decke = Blocks.SMOOTH_STONE.defaultBlockState();
        BlockState licht = Blocks.SEA_LANTERN.defaultBlockState();
        int T = -14; // Boden des Labors
        // Betonsockel mit Luke
        for (int l = -1; l <= 1; l++) {
            for (int b = -1; b <= 1; b++) {
                f.setzen(l, b, -1, Blocks.GRAY_CONCRETE.defaultBlockState());
                f.setzen(l, b, 0, Bau.LUFT);
            }
        }
        f.setzen(0, 0, -1, Blocks.IRON_TRAPDOOR.defaultBlockState()
                .setValue(net.minecraft.world.level.block.TrapDoorBlock.HALF, net.minecraft.world.level.block.state.properties.Half.TOP));
        f.setzen(1, 1, 0, Blocks.LIGHTNING_ROD.defaultBlockState()); // Lüftungsrohr
        // Gang: l -2..12, b -2..2, innen 3 hoch
        raum(f, r, -2, 12, -2, 2, T, wand, boden, decke, licht);
        // Schacht mit Leiter: oberhalb des Gangs gemauert, im Gang nur die Leiter an einer Stütze
        for (int h = T + 1; h <= -2; h++) {
            f.setzen(0, 0, h, Blocks.LADDER.defaultBlockState().setValue(net.minecraft.world.level.block.LadderBlock.FACING, f.quer()));
            if (h >= T + 4) {
                for (int l = -1; l <= 1; l++) {
                    for (int b = -1; b <= 1; b++) {
                        if (l != 0 || b != 0) {
                            f.setzen(l, b, h, Blocks.STONE_BRICKS.defaultBlockState());
                        }
                    }
                }
            } else {
                f.setzen(0, -1, h, Blocks.STONE_BRICKS.defaultBlockState());
            }
        }
        // Drei Laborräume seitlich am Gang
        int[][] raeume = {{2, 3}, {7, 3}, {4, -9}};
        String[] art = {"tanks", "rechner", "lager"};
        for (int i = 0; i < raeume.length; i++) {
            int l0 = raeume[i][0];
            int b0 = raeume[i][1];
            raum(f, r, l0, l0 + 5, b0, b0 + 6, T, wand, boden, decke, licht);
            // Durchgang zum Gang
            int tb = b0 > 0 ? b0 : b0 + 6;
            for (int h = T + 1; h <= T + 2; h++) {
                f.setzen(l0 + 2, tb, h, Bau.LUFT);
                f.setzen(l0 + 2, b0 > 0 ? 2 : -2, h, Bau.LUFT);
            }
            int mb = b0 + 3;
            switch (art[i]) {
                case "tanks" -> {
                    for (int l = l0 + 1; l <= l0 + 4; l += 3) {
                        for (int h = T + 1; h <= T + 3; h++) {
                            boolean kaputt = r.nextInt(3) == 0 && h == T + 2;
                            f.setzen(l, mb + 2, h, kaputt ? Bau.LUFT : Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState());
                        }
                        f.setzen(l, mb + 2, T + 4, Blocks.IRON_BLOCK.defaultBlockState());
                    }
                    f.kiste(l0 + 3, mb - 2, T + 1, "station_null", r, false);
                }
                case "rechner" -> {
                    for (int l = l0 + 1; l <= l0 + 4; l++) {
                        f.setzen(l, b0 + 5, T + 1, Blocks.POLISHED_DEEPSLATE.defaultBlockState());
                        f.setzen(l, b0 + 5, T + 2, r.nextBoolean() ? Blocks.BLACK_STAINED_GLASS_PANE.defaultBlockState()
                                : Blocks.LIME_STAINED_GLASS_PANE.defaultBlockState());
                    }
                    f.kiste(l0 + 1, b0 + 1, T + 1, "buero", r, true);
                    f.kiste(l0 + 4, b0 + 1, T + 1, "bunker", r, false);
                }
                default -> {
                    f.kiste(l0 + 1, b0 + 1, T + 1, "apotheke", r, false);
                    f.kiste(l0 + 4, b0 + 1, T + 1, "kuehlschrank", r, false);
                    f.kiste(l0 + 1, b0 + 5, T + 1, "bunker", r, true);
                    f.kiste(l0 + 4, b0 + 5, T + 1, r.nextInt(3) == 0 ? "waffenkammer" : "militaer", r, true);
                }
            }
        }
        // Entkommene Versuchsobjekte
        net.minecraft.world.level.WorldGenLevel w = f.w();
        for (int i = 0; i < 2; i++) {
            var typ = MonsterTypen.DIMENSION.get(de.deadzonemc.monster.dimension.DimArt.VERSUCHSOBJEKT).get();
            net.minecraft.world.entity.Mob m = typ.create(w.getLevel());
            if (m != null) {
                BlockPos p = f.pos(4 + i * 4, 0, T + 1);
                m.moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, r.nextFloat() * 360, 0);
                m.setPersistenceRequired();
                m.finalizeSpawn(w, w.getCurrentDifficultyAt(p), MobSpawnType.STRUCTURE, null);
                w.addFreshEntityWithPassengers(m);
            }
        }
    }

    /** Ein Raum mit Hülle: Wände, Boden, Decke mit (teils kaputter) Notbeleuchtung, innen frei. */
    private static void raum(Rahmen f, RandomSource r, int l1, int l2, int b1, int b2, int T,
                             BlockState wand, BlockState boden, BlockState decke, BlockState licht) {
        for (int l = l1; l <= l2; l++) {
            for (int b = b1; b <= b2; b++) {
                boolean rand = l == l1 || l == l2 || b == b1 || b == b2;
                f.setzen(l, b, T, boden);
                f.setzen(l, b, T + 4, !rand && (l + b) % 4 == 0 ? (r.nextInt(3) == 0 ? Blocks.REDSTONE_LAMP.defaultBlockState() : licht) : decke);
                for (int h = T + 1; h <= T + 3; h++) {
                    f.setzen(l, b, h, rand ? (r.nextInt(14) == 0 ? Blocks.CRACKED_STONE_BRICKS.defaultBlockState() : wand) : Bau.LUFT);
                }
            }
        }
    }

    private static boolean ohneGegner;

    /**
     * Das Wrack am Startplatz: derselbe abgestürzte Hubschrauber, aber ohne Gegner, und ohne Rotormast aus Eisen
     * (so erscheint am Start nicht gleich der Pilot). Das Triebwerk raucht noch – man sieht, wo man herkommt.
     */
    public static void startWrack(WorldGenLevel w, RandomSource r, BlockPos boden) {
        // Der Absturz hat die Bäume ringsum umgerissen: Laub und Stämme weg, ein paar Stämme liegen am Boden
        for (BlockPos q : BlockPos.betweenClosed(boden.offset(-12, -1, -12), boden.offset(12, 28, 12))) {
            int dx = q.getX() - boden.getX();
            int dz = q.getZ() - boden.getZ();
            if (dx * dx + dz * dz > 144) {
                continue;
            }
            BlockState b = w.getBlockState(q);
            if (b.is(net.minecraft.tags.BlockTags.LEAVES) || b.is(net.minecraft.tags.BlockTags.LOGS)
                    || b.is(Blocks.VINE) || b.is(Blocks.BEE_NEST)) {
                w.setBlock(q, Bau.LUFT, Block.UPDATE_CLIENTS);
            }
        }
        for (int i = 0; i < 3; i++) {
            int x0 = boden.getX() + r.nextInt(19) - 9;
            int z0 = boden.getZ() + r.nextInt(19) - 9;
            boolean entlangX = r.nextBoolean();
            for (int k = 0; k < 4 + r.nextInt(3); k++) {
                int x = x0 + (entlangX ? k : 0);
                int z = z0 + (entlangX ? 0 : k);
                int y = w.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                w.setBlock(new BlockPos(x, y, z), Blocks.OAK_LOG.defaultBlockState()
                        .setValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS,
                                entlangX ? Direction.Axis.X : Direction.Axis.Z), Block.UPDATE_CLIENTS);
            }
        }
        boden = new BlockPos(boden.getX(), w.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, boden.getX(), boden.getZ()),
                boden.getZ());
        ohneGegner = true;
        try {
            testBauen(w, r, boden, Art.HUBSCHRAUBER);
        } finally {
            ohneGegner = false;
        }
        for (BlockPos q : BlockPos.betweenClosed(boden.offset(-12, -2, -12), boden.offset(12, 8, 12))) {
            if (w.getBlockState(q).is(Blocks.IRON_BLOCK)) {
                w.setBlock(q, Blocks.POLISHED_BLACKSTONE.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
        }
    }

    /** Nach der Weltgenerierung (Startwrack, Selbsttest) gibt es die _WG-Höhenkarten nicht mehr. */
    private static boolean nachWeltgen;

    private static int bodenHoehe(WorldGenLevel w, int x, int z) {
        return w.getHeight(nachWeltgen ? Heightmap.Types.MOTION_BLOCKING_NO_LEAVES : Heightmap.Types.WORLD_SURFACE_WG, x, z);
    }

    /** Für den Selbsttest: ohne Ortsprüfung an {@code boden} bauen. */
    public static void testBauen(WorldGenLevel w, RandomSource r, BlockPos boden, Art art) {
        testBauen(w, r, boden, art, Direction.Plane.HORIZONTAL.getRandomDirection(r));
    }

    /** Wie {@link #testBauen(WorldGenLevel, RandomSource, BlockPos, Art)}, mit fester Längsrichtung. */
    public static void testBauen(WorldGenLevel w, RandomSource r, BlockPos boden, Art art, Direction lang) {
        nachWeltgen = true;
        try {
            testBauenIntern(w, r, boden, art, lang);
        } finally {
            nachWeltgen = false;
        }
    }

    private static void testBauenIntern(WorldGenLevel w, RandomSource r, BlockPos boden, Art art, Direction lang) {
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
            case STAUSEEDORF -> stauseedorf(f, r);
            case BUNKERLABOR -> bunkerlabor(f, r);
        }
    }

    // ---- Hubschrauber ---------------------------------------------------------------------

    private static void hubschrauber(Rahmen f, RandomSource r) {
        // Transporthubschrauber Q-7 (wie im Intro): gealtertes Kupferblech, runde Kanten aus Treppen, Glaskanzel,
        // offene Schiebetür, Triebwerk auf dem Dach, Heckausleger abgeknickt, Rotorblätter verstreut.
        BlockState blech = Blocks.WAXED_WEATHERED_CUT_COPPER.defaultBlockState();
        BlockState platte = Blocks.WAXED_WEATHERED_CUT_COPPER_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM);
        BlockState platteOben = Blocks.WAXED_WEATHERED_CUT_COPPER_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP);
        BlockState dunkel = Blocks.POLISHED_BLACKSTONE.defaultBlockState();
        BlockState russ = Blocks.BLACKSTONE.defaultBlockState();
        BlockState kufe = Blocks.POLISHED_BLACKSTONE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM);
        BlockState blatt = Blocks.DEEPSLATE_TILE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM);
        BlockState scheibe = Blocks.TINTED_GLASS.defaultBlockState();
        BlockState fenster = Blocks.GRAY_STAINED_GLASS_PANE.defaultBlockState();
        BlockState boden = Blocks.POLISHED_DEEPSLATE.defaultBlockState();
        BlockState gelb = Blocks.YELLOW_TERRACOTTA.defaultBlockState();
        BlockState schwarz = Blocks.BLACK_CONCRETE.defaultBlockState();
        BlockState eisen = Blocks.IRON_BLOCK.defaultBlockState();
        java.util.function.BiFunction<Direction, net.minecraft.world.level.block.state.properties.Half, BlockState> treppe =
                (d, h) -> Blocks.WAXED_WEATHERED_CUT_COPPER_STAIRS.defaultBlockState().setValue(StairBlock.FACING, d)
                        .setValue(StairBlock.HALF, h);
        var unten = net.minecraft.world.level.block.state.properties.Half.BOTTOM;
        var oben = net.minecraft.world.level.block.state.properties.Half.TOP;
        krater(f, r, 8);
        // Kufen (die rechte ist eingeknickt)
        for (int l = -5; l <= 4; l++) {
            f.setzen(l, -3, 0, kufe);
            if (l < 1 || r.nextInt(3) > 0) {
                f.setzen(l, 3, 0, kufe);
            }
        }
        // Kabine l -4..4, b -2..2, h 1..5 – runde Kanten oben und unten
        for (int l = -4; l <= 4; l++) {
            for (int b = -2; b <= 2; b++) {
                for (int h = 1; h <= 5; h++) {
                    boolean rand = Math.abs(b) == 2 || h == 1 || h == 5 || l == 4;
                    BlockState st = Bau.LUFT;
                    if (rand) {
                        st = blech;
                        if (Math.abs(b) == 2 && h == 5) {
                            st = treppe.apply(b > 0 ? f.quer().getOpposite() : f.quer(), unten);
                        } else if (Math.abs(b) == 2 && h == 1) {
                            st = treppe.apply(b > 0 ? f.quer().getOpposite() : f.quer(), oben);
                        } else if (h == 1) {
                            st = boden;
                        } else if (Math.abs(b) == 2 && h == 3 && l % 2 == 0 && l != 4) {
                            st = fenster;
                        } else if (r.nextInt(14) == 0) {
                            st = russ;
                        }
                    }
                    // offene Schiebetür links (b = -2, l 0..2)
                    if (b == -2 && l >= 0 && l <= 2 && h >= 2 && h <= 4) {
                        st = Bau.LUFT;
                    }
                    f.setzen(l, b, h, st);
                }
                f.stuetzen(l, b, 1, Blocks.COARSE_DIRT.defaultBlockState());
            }
        }
        // Warnstreifen an der Türkante, die herausgerissene Tür liegt daneben
        for (int h = 2; h <= 4; h++) {
            f.setzen(-1, -2, h, (h & 1) == 0 ? gelb : schwarz);
            f.setzen(3, -2, h, (h & 1) == 0 ? schwarz : gelb);
        }
        for (int l = 0; l <= 2; l++) {
            f.setzen(l, -5, 0, Blocks.IRON_TRAPDOOR.defaultBlockState());
        }
        // Innen: Klappsitze an der rechten Wand, Netz unter der Decke
        for (int l = -3; l <= 3; l += 2) {
            f.setzen(l, 1, 2, Blocks.DARK_OAK_TRAPDOOR.defaultBlockState()
                    .setValue(net.minecraft.world.level.block.TrapDoorBlock.HALF, oben));
        }
        for (int l = -3; l <= 3; l++) {
            if (r.nextBoolean()) {
                f.setzen(l, 0, 4, Blocks.COBWEB.defaultBlockState());
            }
        }
        // Cockpit: Glaskanzel, vorn abgeschrägt, Scheiben zum Teil zersplittert
        for (int b = -2; b <= 2; b++) {
            for (int h = 1; h <= 5; h++) {
                boolean rand = Math.abs(b) == 2 || h == 1 || h == 5;
                BlockState st = rand ? (h >= 3 && h <= 4 ? scheibe : blech) : Bau.LUFT;
                if (Math.abs(b) == 2 && h == 5) {
                    st = treppe.apply(b > 0 ? f.quer().getOpposite() : f.quer(), unten);
                }
                f.setzen(-5, b, h, st);
            }
        }
        for (int b = -1; b <= 1; b++) {
            f.setzen(-6, b, 1, blech);
            f.setzen(-6, b, 2, blech);
            f.setzen(-6, b, 3, r.nextInt(3) == 0 ? Bau.LUFT : scheibe);
            f.setzen(-6, b, 4, treppe.apply(f.lang(), unten));
            f.setzen(-7, b, 1, treppe.apply(f.lang(), unten));
            f.setzen(-7, b, 2, r.nextInt(2) == 0 ? Bau.LUFT : platte);
        }
        f.setzen(-5, 0, 2, Blocks.LEVER.defaultBlockState());
        // Triebwerk auf dem Dach, Auspuff hinten rußig
        for (int l = -2; l <= 3; l++) {
            for (int b = -1; b <= 1; b++) {
                f.setzen(l, b, 6, l == 3 ? russ : (b == 0 ? blech : platte));
            }
        }
        f.setzen(4, 0, 6, dunkel);
        // Rotormast und Nabe, ein Blatt hängt schräg herab
        f.setzen(0, 0, 7, eisen);
        f.setzen(0, 0, 8, dunkel);
        for (int k = 1; k <= 7; k++) {
            f.setzen(-k, 0, 8 - (k > 3 ? 1 : 0) - (k > 6 ? 1 : 0), blatt);
        }
        for (int k = 1; k <= 4; k++) {
            f.setzen(0, k, 8 - k / 2, blatt);
        }
        // Heckausleger: zuerst kräftig, dann schmal, bei l 10 abgeknickt
        for (int l = 5; l <= 9; l++) {
            int breit = l <= 6 ? 1 : 0;
            for (int b = -breit; b <= breit; b++) {
                f.setzen(l, b, 4, l <= 6 ? blech : platteOben);
                f.setzen(l, b, 3, l <= 6 ? blech : Bau.LUFT);
            }
        }
        // abgerissenes Heckstück mit Leitwerk liegt daneben im Gras
        for (int l = 11; l <= 15; l++) {
            f.setzen(l, 2, 0, l == 11 ? russ : platte);
            f.stuetzen(l, 2, 0, Blocks.COARSE_DIRT.defaultBlockState());
        }
        f.setzen(15, 3, 0, blech);
        f.setzen(15, 4, 0, blech);
        f.setzen(15, 5, 0, platte);
        f.setzen(16, 3, 0, Blocks.IRON_BARS.defaultBlockState());
        f.setzen(16, 4, 0, Blocks.IRON_BARS.defaultBlockState());
        // die anderen Rotorblätter liegen verstreut
        for (int i = 0; i < 3; i++) {
            int l0 = -9 + r.nextInt(18);
            int b0 = r.nextBoolean() ? 5 + r.nextInt(5) : -6 - r.nextInt(5);
            for (int k = 0; k < 6; k++) {
                BlockPos p = f.pos(l0 + (i % 2 == 0 ? k : 0), b0 + (i % 2 == 0 ? 0 : k), 0);
                int y = bodenHoehe(f.w(), p.getX(), p.getZ());
                f.w().setBlock(new BlockPos(p.getX(), y, p.getZ()), blatt, Block.UPDATE_CLIENTS);
            }
        }
        // Splitter und Blechteile im Krater
        for (int i = 0; i < 8; i++) {
            BlockPos p = f.pos(-8 + r.nextInt(17), -7 + r.nextInt(15), 0);
            int y = bodenHoehe(f.w(), p.getX(), p.getZ());
            BlockPos q = new BlockPos(p.getX(), y, p.getZ());
            if (f.w().getBlockState(q).isAir()) {
                f.w().setBlock(q, r.nextBoolean() ? platte : fenster, Block.UPDATE_CLIENTS);
            }
        }
        // Brennendes Triebwerk: Rauchsäule, die man von weit her sieht
        f.setzen(3, 0, 6, Blocks.HAY_BLOCK.defaultBlockState());
        f.setzen(3, 0, 7, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true)
                .setValue(CampfireBlock.SIGNAL_FIRE, true));
        // Beute: Militärkisten, mit Glück die Waffenkammer
        f.kiste(-2, 1, 2, "militaer", r, true);
        f.kiste(2, 1, 2, r.nextInt(3) == 0 ? "waffenkammer" : "militaer", r, false);
        f.kiste(-3, -1, 2, "apotheke", r, true);
        spawnen(f, MonsterTypen.GEFALLENER_SOLDAT::get, 1 + r.nextInt(2), r);
    }

    // ---- Hilfen für Blockzustände ----------------------------------------------------------------

    /** Treppe, deren hohe Seite nach {@code hoch} zeigt; {@code kopf} = kopfüber. */
    private static BlockState tr(Block b, Direction hoch, boolean kopf) {
        return b.defaultBlockState().setValue(StairBlock.FACING, hoch).setValue(StairBlock.HALF,
                kopf ? net.minecraft.world.level.block.state.properties.Half.TOP : net.minecraft.world.level.block.state.properties.Half.BOTTOM);
    }

    private static BlockState st(Block b, boolean oben) {
        return b.defaultBlockState().setValue(SlabBlock.TYPE, oben ? SlabType.TOP : SlabType.BOTTOM);
    }

    /** Scheibe, Gitter oder Zaun, der sich entlang einer Achse verbindet. */
    private static BlockState laengs(Block b, Direction d) {
        BlockState s = b.defaultBlockState();
        var p = net.minecraft.world.level.block.PipeBlock.PROPERTY_BY_DIRECTION;
        if (s.hasProperty(p.get(d))) {
            s = s.setValue(p.get(d), true).setValue(p.get(d.getOpposite()), true);
        }
        return s;
    }

    // ---- Flugzeug -------------------------------------------------------------------------

    /**
     * Abgestürztes Passagierflugzeug: runder Rumpf (Treppen an den Kanten), in der Mitte durchgebrochen, das Heck
     * versetzt. Cockpit mit dunklen Scheiben, Sitzreihen, Gepäckfächer, Teppich im Gang, offene Tür mit
     * Notrutsche, ein Flügel mit Triebwerk und Positionslicht, der andere abgerissen, Höhen- und Seitenleitwerk.
     */
    private static void flugzeug(Rahmen f, RandomSource r) {
        BlockState weiss = Blocks.SMOOTH_QUARTZ.defaultBlockState();
        BlockState grau = Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState();
        BlockState rot = Blocks.RED_CONCRETE.defaultBlockState();
        BlockState schwarz = Blocks.POLISHED_BLACKSTONE.defaultBlockState();
        BlockState fenster = laengs(Blocks.GRAY_STAINED_GLASS_PANE, f.lang());
        BlockState fluegel = st(Blocks.SMOOTH_STONE_SLAB, true);
        Direction vor = f.lang().getOpposite();
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
                Direction innen = b < 0 ? f.quer() : f.quer().getOpposite();
                for (int h = 0; h <= 4; h++) {
                    boolean rand = Math.abs(b) == 2 || h == 0 || h == 4;
                    BlockState s;
                    if (!rand) {
                        s = Bau.LUFT;
                    } else if (Math.abs(b) == 2 && h == 4) {
                        s = tr(Blocks.SMOOTH_QUARTZ_STAIRS, innen, false); // runde Oberkante
                    } else if (Math.abs(b) == 2 && h == 0) {
                        s = tr(Blocks.POLISHED_DIORITE_STAIRS, innen, true); // runde Unterkante
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
            // Gepäckfächer oben, Teppich im Gang, Sitzreihen
            if (l > -10 && l < 10) {
                for (int b : new int[]{-1, 1}) {
                    if (r.nextInt(5) > 0) {
                        f.setzen(l, b + versatz, 3, st(Blocks.SMOOTH_STONE_SLAB, true));
                    }
                }
                if (r.nextInt(4) > 0) {
                    f.setzen(l, versatz, 1, Blocks.RED_CARPET.defaultBlockState());
                }
            }
            if (l % 2 != 0 && l > -9 && l < 10) {
                for (int b : new int[]{-1, 1}) {
                    f.setzen(l, b + versatz, 1, r.nextInt(7) == 0 ? Bau.LUFT : tr(Blocks.DARK_PRISMARINE_STAIRS, f.lang(), false));
                }
            }
        }
        // Nase: verjüngt, Cockpitscheiben, schräge Oberseite
        for (int b = -1; b <= 1; b++) {
            f.setzen(-12, b, 0, grau);
            f.setzen(-12, b, 1, weiss);
            f.setzen(-12, b, 2, weiss);
            f.setzen(-12, b, 3, laengs(Blocks.BLACK_STAINED_GLASS_PANE, f.quer()));
            f.setzen(-12, b, 4, tr(Blocks.SMOOTH_QUARTZ_STAIRS, f.lang(), false));
            f.stuetzen(-12, b, 0, Blocks.COARSE_DIRT.defaultBlockState());
        }
        f.setzen(-13, 0, 0, grau);
        f.setzen(-13, 0, 1, weiss);
        f.setzen(-13, 0, 2, tr(Blocks.SMOOTH_QUARTZ_STAIRS, f.lang(), false));
        f.setzen(-13, -1, 1, tr(Blocks.SMOOTH_QUARTZ_STAIRS, f.quer(), false));
        f.setzen(-13, 1, 1, tr(Blocks.SMOOTH_QUARTZ_STAIRS, f.quer().getOpposite(), false));
        f.stuetzen(-13, 0, 0, Blocks.COARSE_DIRT.defaultBlockState());
        // Leitwerk am Heck: Seitenruder mit rotem Streifen, Höhenleitwerk zu beiden Seiten
        for (int h = 5; h <= 8; h++) {
            f.setzen(11, 1, h, h == 7 ? rot : weiss);
            f.setzen(10, 1, h, h <= 6 ? weiss : (h == 7 ? tr(Blocks.SMOOTH_QUARTZ_STAIRS, f.lang(), false) : Bau.LUFT));
        }
        for (int l = 10; l <= 11; l++) {
            for (int b = 2; b <= 4; b++) {
                if (l == 10 && b == 4) {
                    continue;
                }
                f.setzen(l, 1 + b, 4, st(Blocks.SMOOTH_QUARTZ_SLAB, true));
                f.setzen(l, 1 - b, 4, st(Blocks.SMOOTH_QUARTZ_SLAB, true));
            }
        }
        // Flügel: links ganz, rechts abgerissen
        for (int l = -5; l <= -2; l++) {
            for (int b = 3; b <= 11; b++) {
                if (b > 11 - (l + 5)) {
                    continue;
                }
                f.setzen(l, -b, 1, fluegel);
            }
            for (int b = 3; b <= 6; b++) {
                f.setzen(l, b, 1, fluegel);
            }
        }
        f.setzen(-5, -11, 1, Blocks.RED_STAINED_GLASS.defaultBlockState()); // Positionslicht an der Spitze
        f.setzen(-5, -11, 2, st(Blocks.SMOOTH_STONE_SLAB, false)); // Winglet
        // Triebwerk unter dem Flügel: Einlauf, Gondel, Schubdüse
        f.setzen(-7, -6, 0, schwarz);
        for (int l = -6; l <= -3; l++) {
            f.setzen(l, -6, 0, l == -6 ? Blocks.IRON_BLOCK.defaultBlockState() : grau);
            f.stuetzen(l, -6, 0, Blocks.COARSE_DIRT.defaultBlockState());
        }
        f.setzen(-2, -6, 0, laengs(Blocks.IRON_BARS, f.quer()));
        // abgerissener Flügel liegt daneben, mit dem zweiten Triebwerk
        for (int l = 4; l <= 8; l++) {
            for (int b = 7; b <= 9; b++) {
                BlockPos p = f.pos(l, b, 0);
                int y = bodenHoehe(f.w(), p.getX(), p.getZ());
                f.w().setBlock(new BlockPos(p.getX(), y, p.getZ()), Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
        }
        for (int l = 5; l <= 7; l++) {
            BlockPos p = f.pos(l, 11, 0);
            int y = bodenHoehe(f.w(), p.getX(), p.getZ());
            f.w().setBlock(new BlockPos(p.getX(), y, p.getZ()), l == 5 ? schwarz : grau, Block.UPDATE_CLIENTS);
        }
        // offene Tür mit Notrutsche
        f.setzen(-9, -2, 1, Bau.LUFT);
        f.setzen(-9, -2, 2, Bau.LUFT);
        f.setzen(-9, -3, 0, Blocks.YELLOW_WOOL.defaultBlockState());
        for (int b = 4; b <= 7; b++) {
            for (int l = -10; l <= -9; l++) {
                BlockPos p = f.pos(l, -b, 0);
                int y = bodenHoehe(f.w(), p.getX(), p.getZ());
                f.w().setBlock(new BlockPos(p.getX(), y, p.getZ()), Blocks.YELLOW_CARPET.defaultBlockState(), Block.UPDATE_CLIENTS);
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

    /**
     * Gestrandeter Frachter mit Schlagseite: rostiger Rumpf, Reling, Ladekran am Bug, Container auf Deck,
     * Brücke mit Schornstein, Radarmast und Rettungsboot, Ankerkette.
     */
    private static void frachter(Rahmen f, RandomSource r, int wassertiefe) {
        BlockState rost = Blocks.TERRACOTTA.defaultBlockState();
        BlockState rost2 = Blocks.BROWN_TERRACOTTA.defaultBlockState();
        BlockState rumpf = Blocks.RED_TERRACOTTA.defaultBlockState();
        BlockState deck = Blocks.EXPOSED_CUT_COPPER.defaultBlockState();
        BlockState weiss = Blocks.WHITE_TERRACOTTA.defaultBlockState();
        BlockState gelb = Blocks.YELLOW_CONCRETE.defaultBlockState();
        int deckH = Math.max(4, wassertiefe + 3);
        int d = deckH;
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
                    if (aussen && h == deckH && Math.abs(b) == halb) {
                        s = weiss; // weißer Wasserpass unter der Reling
                    }
                    f.setzen(l, b, h + neig, s);
                }
                f.setzen(l, b, deckH + 1 + neig, deck);
                f.stuetzen(l, b, neig, rost2);
                // Reling am Rand (hinten an der Brücke frei), mit Lücken
                if (Math.abs(b) == halb && l < 8 && r.nextInt(4) > 0) {
                    f.setzen(l, b, deckH + 2 + neig, laengs(Blocks.IRON_BARS, halb == 4 ? f.lang() : f.quer()));
                }
            }
        }
        // Brücke am Heck
        for (int l = 8; l <= 12; l++) {
            for (int b = -3; b <= 3; b++) {
                for (int h = deckH + 2; h <= deckH + 6; h++) {
                    boolean rand = Math.abs(b) == 3 || l == 8 || l == 12 || h == deckH + 6;
                    BlockState s = weiss;
                    if (!rand) {
                        s = Bau.LUFT;
                    } else if (h == deckH + 5 && (l == 8 || Math.abs(b) == 3) && !(l == 8 && Math.abs(b) == 3)) {
                        s = r.nextInt(5) == 0 ? Bau.LUFT : laengs(Blocks.BLACK_STAINED_GLASS_PANE, l == 8 ? f.quer() : f.lang());
                    }
                    f.setzen(l, b, h + (b < 0 ? 1 : 0), s);
                }
            }
        }
        f.kiste(10, 0, deckH + 2, "werkzeug", r, false);
        f.kiste(11, -1, deckH + 3, "militaer", r, true);
        // Schornstein mit rotem Band, Radarmast
        for (int h = d + 7; h <= d + 10; h++) {
            for (int l = 10; l <= 11; l++) {
                f.setzen(l, 0, h, h == d + 9 ? Blocks.RED_CONCRETE.defaultBlockState() : Blocks.BLACK_CONCRETE.defaultBlockState());
            }
        }
        for (int h = d + 7; h <= d + 11; h++) {
            f.setzen(9, 0, h, Blocks.IRON_BARS.defaultBlockState());
        }
        f.setzen(9, -1, d + 10, laengs(Blocks.IRON_BARS, f.quer()));
        f.setzen(9, 1, d + 10, laengs(Blocks.IRON_BARS, f.quer()));
        f.setzen(9, 0, d + 10, laengs(Blocks.IRON_BARS, f.quer()));
        f.setzen(9, 0, d + 12, Blocks.LIGHTNING_ROD.defaultBlockState());
        // Rettungsboot in den Davits an der Steuerbordseite
        for (int l = 8; l <= 12; l += 4) {
            for (int h = d + 2; h <= d + 4; h++) {
                f.setzen(l, 4, h, Blocks.IRON_BARS.defaultBlockState());
            }
        }
        for (int l = 9; l <= 11; l++) {
            f.setzen(l, 4, d + 3, l == 10 ? Blocks.ORANGE_CONCRETE.defaultBlockState() : Blocks.ORANGE_TERRACOTTA.defaultBlockState());
            f.setzen(l, 4, d + 4, st(Blocks.SMOOTH_QUARTZ_SLAB, false));
        }
        // Ladekran am Bug: gelbe Säule, Ausleger übers Wasser, Kette mit Haken
        for (int h = d + 2; h <= d + 7; h++) {
            f.setzen(-10, 0, h, gelb);
        }
        for (int b = 1; b <= 6; b++) {
            f.setzen(-10, b, d + 7, b == 6 ? tr(Blocks.SMOOTH_SANDSTONE_STAIRS, f.quer().getOpposite(), true) : gelb);
        }
        f.setzen(-10, 0, d + 8, Blocks.BLACK_CONCRETE.defaultBlockState()); // Kranführerhaus
        for (int h = d + 3; h <= d + 6; h++) {
            f.setzen(-10, 6, h, Blocks.CHAIN.defaultBlockState());
        }
        f.setzen(-10, 6, d + 2, Blocks.TRIPWIRE_HOOK.defaultBlockState());
        // Ankerkette hängt am Bug ins Wasser
        for (int h = 1; h <= d + 1; h++) {
            f.setzen(-14, 0, h, Blocks.CHAIN.defaultBlockState());
        }
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
                // Türseite mit Verriegelungsstangen, eine Tür offen
                f.setzen(l0 + 4, b0, deckH + 2 + (b0 < 0 ? 1 : 0), Bau.LUFT);
                f.setzen(l0 + 4, b0 + 1, deckH + 2 + (b0 + 1 < 0 ? 1 : 0), laengs(Blocks.IRON_BARS, f.quer()));
                f.kiste(l0 + 2, b0, deckH + 2 + (b0 < 0 ? 1 : 0), ware[r.nextInt(ware.length)], r, true);
            }
        }
        spawnen(f, MonsterTypen.WANDERER::get, 2 + r.nextInt(2), r);
    }

    // ---- Panzerwrack und Konvoi ----------------------------------------------------------------

    private static void panzer(Rahmen f, RandomSource r, int l0, int b0, boolean einzeln) {
        int z = r.nextInt(20);
        panzer(f, r, l0, b0, einzeln, z < 12 ? 0 : z < 17 ? 1 : 2);
    }

    /**
     * Ein alter Kampfpanzer, Jahrzehnte alt oder in den ersten Tagen der Abriegelung liegen geblieben: Wanne mit
     * schrägem Bug, Ketten mit Laufrollen und runden Enden, Kettenschürzen, Turm mit Blende und Kanone
     * (vorn, zur Seite oder nach hinten), offene Luke, Antenne, Staukorb, Ersatzkettenglieder, Zusatztanks.
     * Überwuchert (Moos, Ranken) oder ausgebrannt (Ruß, Schädel). Im Inneren: Munitionskisten.
     *
     * @param rohr 0 = nach vorn, 1 = zur Seite, 2 = nach hinten
     */
    private static void panzer(Rahmen f, RandomSource r, int l0, int b0, boolean einzeln, int rohr) {
        boolean alt = r.nextInt(3) > 0; // alt = überwuchert, sonst ausgebrannt
        Block blech = alt ? Blocks.WAXED_WEATHERED_CUT_COPPER : Blocks.POLISHED_BLACKSTONE;
        Block blechStufe = alt ? Blocks.WAXED_WEATHERED_CUT_COPPER_SLAB : Blocks.POLISHED_BLACKSTONE_SLAB;
        Block blechTreppe = alt ? Blocks.WAXED_WEATHERED_CUT_COPPER_STAIRS : Blocks.POLISHED_BLACKSTONE_STAIRS;
        BlockState kette = Blocks.POLISHED_DEEPSLATE.defaultBlockState();
        BlockState rolle = Blocks.DEEPSLATE_TILES.defaultBlockState();
        BlockState erde = Blocks.COARSE_DIRT.defaultBlockState();
        Direction vor = f.lang().getOpposite();
        Direction zur = f.lang();
        java.util.function.Supplier<BlockState> haut = () -> {
            int w = r.nextInt(20);
            if (!alt) {
                return (w < 14 ? Blocks.POLISHED_BLACKSTONE : w < 18 ? Blocks.BLACKSTONE : Blocks.BROWN_TERRACOTTA).defaultBlockState();
            }
            return (w < 14 ? Blocks.WAXED_WEATHERED_CUT_COPPER : w < 16 ? Blocks.GREEN_TERRACOTTA
                    : w < 19 ? Blocks.BROWN_TERRACOTTA : Blocks.MOSS_BLOCK).defaultBlockState();
        };
        // Schreibt relativ zur Panzermitte
        interface P {
            void s(int l, int b, int h, BlockState x);
        }
        P p = (l, b, h, x) -> f.setzen(l0 + l, b0 + b, h, x);
        if (einzeln) {
            krater(f, r, 6);
        }
        // Wanne l -4..4, b -2..2 (Ketten auf b = ±2)
        for (int l = -4; l <= 4; l++) {
            for (int b = -2; b <= 2; b++) {
                if (Math.abs(b) == 2) {
                    boolean ende = l == -4 || l == 4;
                    Direction innen = l == -4 ? zur : vor;
                    p.s(l, b, 0, ende ? tr(Blocks.POLISHED_DEEPSLATE_STAIRS, innen, false) : (l % 2 == 0 ? rolle : kette));
                    p.s(l, b, 1, ende ? tr(Blocks.POLISHED_DEEPSLATE_STAIRS, innen, true) : kette);
                    // Kettenschürze
                    p.s(l, b, 2, l == -4 || (!alt && r.nextInt(4) == 0) ? Bau.LUFT : st(blechStufe, false));
                } else {
                    p.s(l, b, 0, kette);
                    p.s(l, b, 1, haut.get());
                    p.s(l, b, 2, l == -4 ? tr(blechTreppe, zur, false) : haut.get());
                }
                f.stuetzen(l0 + l, b0 + b, 0, erde);
            }
        }
        // Bug: schräge untere Platte, Abschlepphaken
        for (int b = -1; b <= 1; b++) {
            p.s(-5, b, 0, haut.get());
            p.s(-5, b, 1, tr(blechTreppe, zur, false));
            f.stuetzen(l0 - 5, b0 + b, 0, erde);
        }
        // Heck: zwei Zusatztanks (leere Fässer, liegend)
        for (int b = -1; b <= 1; b += 2) {
            p.s(5, b, 1, Blocks.BARREL.defaultBlockState().setValue(net.minecraft.world.level.block.BarrelBlock.FACING, f.quer()));
            p.s(5, b, 0, haut.get());
            f.stuetzen(l0 + 5, b0 + b, 0, erde);
        }
        // Ersatzkettenglieder quer auf dem Bug
        for (int b = -1; b <= 1; b += 2) {
            p.s(-3, b, 3, Blocks.CHAIN.defaultBlockState().setValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS,
                    f.quer().getAxis()));
        }
        // Innenraum mit Munitionskiste (unter der Luke)
        p.s(1, 0, 2, Bau.LUFT);
        p.s(1, 0, 1, Bau.LUFT);
        f.kiste(l0, b0, 1, "militaer", r, true);
        // Turm l 0..2, b -1..1: vorn abgeschrägt, Luke in der Mitte
        for (int l = 0; l <= 2; l++) {
            for (int b = -1; b <= 1; b++) {
                BlockState s = haut.get();
                if (l == 0 && b != 0) {
                    s = tr(blechTreppe, zur, false);
                }
                if (l == 1 && b == 0) {
                    s = Blocks.IRON_TRAPDOOR.defaultBlockState().setValue(net.minecraft.world.level.block.TrapDoorBlock.OPEN, true);
                }
                p.s(l, b, 3, s);
            }
        }
        // Kommandantenkuppel und Antenne
        p.s(2, -1, 4, st(blechStufe, false));
        p.s(2, 1, 4, Blocks.LIGHTNING_ROD.defaultBlockState());
        p.s(2, 1, 5, Blocks.IRON_BARS.defaultBlockState());
        if (alt) {
            p.s(2, 1, 6, Blocks.IRON_BARS.defaultBlockState());
        }
        // Kanone: Blende, Rohr, Mündungsbremse
        BlockState blende = blech.defaultBlockState();
        BlockState muendung = Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
        if (rohr == 1) {
            net.minecraft.core.Direction.Axis achse = f.quer().getAxis();
            BlockState rohrS = Blocks.POLISHED_BASALT.defaultBlockState().setValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS, achse);
            p.s(1, 2, 3, blende);
            for (int i = 3; i <= 7; i++) {
                p.s(1, i, 3, rohrS);
            }
            p.s(1, 8, 3, muendung);
        } else {
            int s = rohr == 0 ? -1 : 1;
            int start = rohr == 0 ? -1 : 3;
            BlockState rohrS = Blocks.POLISHED_BASALT.defaultBlockState().setValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS,
                    f.lang().getAxis());
            p.s(start, 0, 3, blende);
            for (int i = 1; i <= 5; i++) {
                p.s(start + s * i, 0, 3, rohrS);
            }
            p.s(start + s * 6, 0, 3, muendung);
        }
        // Staukorb am Turmheck (nicht, wenn das Rohr nach hinten zeigt)
        if (rohr != 2) {
            p.s(3, -1, 3, laengs(Blocks.IRON_BARS, f.lang()));
            p.s(3, 1, 3, laengs(Blocks.IRON_BARS, f.lang()));
            if (r.nextInt(5) < 2) {
                f.kiste(l0 + 3, b0, 3, "militaer", r, true);
            } else {
                p.s(3, 0, 3, Blocks.BROWN_WOOL.defaultBlockState()); // zusammengerollte Plane
            }
        }
        if (alt) {
            for (int i = 0; i < 6; i++) {
                int l = r.nextInt(7) - 3;
                int b = r.nextBoolean() ? 3 : -3;
                p.s(l, b, 1, Blocks.VINE.defaultBlockState()
                        .setValue(b > 0 ? net.minecraft.world.level.block.VineBlock.getPropertyForFace(f.quer().getOpposite())
                                : net.minecraft.world.level.block.VineBlock.getPropertyForFace(f.quer()), true));
            }
            p.s(4, -1, 3, Blocks.MOSS_CARPET.defaultBlockState());
            p.s(4, 1, 3, Blocks.MOSS_CARPET.defaultBlockState());
            p.s(-2, 1, 3, Blocks.MOSS_CARPET.defaultBlockState());
        } else {
            p.s(0, 1, 4, Blocks.SKELETON_SKULL.defaultBlockState());
            p.s(1, 0, 2, Blocks.COBWEB.defaultBlockState());
        }
    }

    /**
     * Militärlastwagen (9 lang, 3 breit): Stoßstange mit Tarnscheinwerfern, Kühlergrill, gerundete Motorhaube,
     * Fahrerhaus mit Frontscheibe und Türfenstern, Tank, Doppelachse hinten, Pritsche mit Holzboden, Spriegeln
     * und zerrissener Plane. Mitte bei l = 0.
     */
    private static void lastwagen(Rahmen f, RandomSource r, int l0) {
        BlockState blech = Blocks.WAXED_WEATHERED_CUT_COPPER.defaultBlockState();
        BlockState flach = st(Blocks.WAXED_WEATHERED_CUT_COPPER_SLAB, false);
        BlockState rad = Blocks.BLACK_CONCRETE.defaultBlockState();
        BlockState plane = Blocks.GREEN_WOOL.defaultBlockState();
        BlockState holz = Blocks.SPRUCE_PLANKS.defaultBlockState();
        BlockState spriegel = Blocks.SPRUCE_FENCE.defaultBlockState();
        Block glas = Blocks.GRAY_STAINED_GLASS_PANE;
        for (int l = -4; l <= 4; l++) {
            for (int b = -1; b <= 1; b++) {
                boolean istRad = (l == -3 || l == 2 || l == 3) && b != 0;
                BlockState unten = istRad ? rad : blech;
                if (l == -1 && b == 1) {
                    unten = Blocks.IRON_BLOCK.defaultBlockState(); // Tank
                }
                f.setzen(l0 + l, b, 0, unten);
                f.stuetzen(l0 + l, b, 0, Blocks.COARSE_DIRT.defaultBlockState());
            }
        }
        // Front: Grill, Tarnscheinwerfer, gerundete Haube
        f.setzen(l0 - 4, 0, 1, laengs(Blocks.IRON_BARS, f.quer()));
        f.setzen(l0 - 4, -1, 1, Blocks.REDSTONE_LAMP.defaultBlockState());
        f.setzen(l0 - 4, 1, 1, Blocks.REDSTONE_LAMP.defaultBlockState());
        for (int b = -1; b <= 1; b++) {
            f.setzen(l0 - 4, b, 2, tr(Blocks.WAXED_WEATHERED_CUT_COPPER_STAIRS, f.lang(), false));
            f.setzen(l0 - 3, b, 1, blech);
            f.setzen(l0 - 3, b, 2, flach);
        }
        // Fahrerhaus
        for (int b = -1; b <= 1; b++) {
            f.setzen(l0 - 2, b, 1, blech);
            f.setzen(l0 - 2, b, 2, r.nextInt(3) == 0 ? Bau.LUFT : laengs(glas, f.quer()));
            f.setzen(l0 - 2, b, 3, flach);
            f.setzen(l0 - 1, b, 3, flach);
        }
        f.setzen(l0 - 1, -1, 1, blech);
        f.setzen(l0 - 1, 1, 1, r.nextInt(3) == 0 ? Bau.LUFT : blech); // Tür offen?
        f.setzen(l0 - 1, 0, 1, tr(Blocks.SPRUCE_STAIRS, f.lang(), false)); // Sitzbank
        f.setzen(l0 - 1, -1, 2, laengs(glas, f.lang()));
        f.setzen(l0 - 1, 1, 2, r.nextInt(2) == 0 ? Bau.LUFT : laengs(glas, f.lang()));
        f.setzen(l0 - 1, 0, 2, Bau.LUFT);
        // Pritsche: Holzboden, Stirnwand, Spriegel, Plane mit Rissen
        for (int l = 0; l <= 4; l++) {
            for (int b = -1; b <= 1; b++) {
                f.setzen(l0 + l, b, 1, holz);
                boolean seite = b != 0;
                BlockState wand;
                if (l == 0) {
                    wand = holz;
                } else if (seite) {
                    wand = r.nextInt(5) < 3 ? plane : (l == 2 || l == 4 ? spriegel : Bau.LUFT);
                } else {
                    wand = Bau.LUFT;
                }
                f.setzen(l0 + l, b, 2, wand);
                BlockState dach = r.nextInt(4) > 0 ? plane : (seite && l % 2 == 0 ? spriegel : Bau.LUFT);
                f.setzen(l0 + l, b, 3, dach);
            }
        }
        f.kiste(l0 + 1, 0, 2, "militaer", r, true);
        f.kiste(l0 + 2, 0, 2, r.nextInt(3) == 0 ? "waffenkammer" : "apotheke", r, false);
        f.setzen(l0 + 4, 0, 2, Bau.LUFT);
        f.setzen(l0 + 4, 0, 3, r.nextBoolean() ? plane : Bau.LUFT);
    }

    /** Liegengebliebener Konvoi: zwei Panzer und ein Lastwagen hintereinander, dazwischen Kisten und Sandsäcke. */
    private static void konvoi(Rahmen f, RandomSource r) {
        krater(f, r, 10);
        panzer(f, r, -11, 0, false, r.nextInt(3) == 0 ? 1 : 0);
        lastwagen(f, r, 0);
        panzer(f, r, 10, 1, false, r.nextBoolean() ? 1 : 2);
        // Sandsäcke
        for (int i = 0; i < 6; i++) {
            BlockPos p = f.pos(-4 + r.nextInt(9), r.nextBoolean() ? 4 : -4, 0);
            int y = bodenHoehe(f.w(), p.getX(), p.getZ());
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
                int y = bodenHoehe(f.w(), p.getX(), p.getZ()) - 1;
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
                int y = bodenHoehe(w, x, z) - 1;
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
        if (ohneGegner) {
            return;
        }
        WorldGenLevel w = f.w();
        for (int i = 0; i < anzahl; i++) {
            Mob m = typ.get().create(w.getLevel());
            if (m == null) {
                return;
            }
            BlockPos p = f.pos(-6 + r.nextInt(12), r.nextBoolean() ? 3 + r.nextInt(3) : -3 - r.nextInt(3), 0);
            int y = bodenHoehe(w, p.getX(), p.getZ());
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
