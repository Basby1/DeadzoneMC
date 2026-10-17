package de.deadzonemc.welt.gebaeude;

import de.deadzonemc.welt.Bau;
import de.deadzonemc.welt.bau.Baukasten;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;

import java.util.Random;

import static de.deadzonemc.welt.bau.Baukasten.H;
import static de.deadzonemc.welt.bau.Baukasten.L;
import static de.deadzonemc.welt.bau.Baukasten.R;
import static de.deadzonemc.welt.bau.Baukasten.V;

/** Gemeinsame Bauteile für alle Gebäude: Rohbau, Fenster, Dächer, Treppen, Möbel, Verfall, Garten. */
public final class Hausbau {

    private Hausbau() {
    }

    public static final BlockState LUFT = Bau.LUFT;

    // ---- Hilfen für Blockzustände ------------------------------------------------------------

    public static BlockState treppe(Block block, Direction blick, boolean oben) {
        return block.defaultBlockState().setValue(StairBlock.FACING, blick)
                .setValue(StairBlock.HALF, oben ? Half.TOP : Half.BOTTOM);
    }

    public static BlockState stufe(Block block, boolean oben) {
        return block.defaultBlockState().setValue(SlabBlock.TYPE, oben ? SlabType.TOP : SlabType.BOTTOM);
    }

    public static BlockState luke(Block block, Direction blick, boolean oben, boolean offen) {
        return block.defaultBlockState().setValue(TrapDoorBlock.FACING, blick)
                .setValue(TrapDoorBlock.HALF, oben ? Half.TOP : Half.BOTTOM).setValue(TrapDoorBlock.OPEN, offen);
    }

    public static BlockState balken(Block log, Direction.Axis achse) {
        return log.defaultBlockState().hasProperty(RotatedPillarBlock.AXIS)
                ? log.defaultBlockState().setValue(RotatedPillarBlock.AXIS, achse) : log.defaultBlockState();
    }

    /** Passende Treppe zu einem Bretter-Block (für Innentreppen und Möbel). */
    public static Block treppeZu(Block bretter) {
        if (bretter == Blocks.OAK_PLANKS) return Blocks.OAK_STAIRS;
        if (bretter == Blocks.BIRCH_PLANKS) return Blocks.BIRCH_STAIRS;
        if (bretter == Blocks.DARK_OAK_PLANKS) return Blocks.DARK_OAK_STAIRS;
        if (bretter == Blocks.JUNGLE_PLANKS) return Blocks.JUNGLE_STAIRS;
        return Blocks.SPRUCE_STAIRS;
    }

    public static Block stufeZu(Block bretter) {
        if (bretter == Blocks.OAK_PLANKS) return Blocks.OAK_SLAB;
        if (bretter == Blocks.BIRCH_PLANKS) return Blocks.BIRCH_SLAB;
        if (bretter == Blocks.DARK_OAK_PLANKS) return Blocks.DARK_OAK_SLAB;
        if (bretter == Blocks.JUNGLE_PLANKS) return Blocks.JUNGLE_SLAB;
        return Blocks.SPRUCE_SLAB;
    }

    // ---- Rohbau ------------------------------------------------------------------------------

    /**
     * Wände, Böden und Decke eines rechteckigen Gebäudes mit {@code stockwerke} Etagen à 4 Blöcken.
     * Ebene ly0 ist der Erdgeschossboden; die Decke liegt auf ly0 + 4 * stockwerke.
     */
    public static void rohbau(Baukasten b, Fassade f, int x1, int z1, int x2, int z2, int ly0, int stockwerke,
                              double verfall) {
        b.fundament(x1, z1, x2, z2, f.sockel().defaultBlockState());
        for (int s = 0; s < stockwerke; s++) {
            int basis = ly0 + 4 * s;
            for (int x = x1; x <= x2; x++) {
                for (int z = z1; z <= z2; z++) {
                    boolean rand = x == x1 || x == x2 || z == z1 || z == z2;
                    BlockState boden;
                    if (rand) {
                        boden = (s == 0 ? f.sockel() : f.wand()).defaultBlockState();
                    } else if (s > 0 && b.chance(x, basis, z, verfall * 0.05)) {
                        boden = LUFT; // eingebrochene Dielen
                    } else {
                        boden = f.boden().defaultBlockState();
                    }
                    b.setze(x, basis, z, boden);
                    for (int y = basis + 1; y <= basis + 3; y++) {
                        BlockState st;
                        boolean ecke = (x == x1 || x == x2) && (z == z1 || z == z2);
                        if (ecke) {
                            st = balken(f.ecke(), Direction.Axis.Y);
                        } else if (rand) {
                            st = wandBlock(b, f, x, y, z, y - ly0, 4 * stockwerke, verfall);
                        } else {
                            st = LUFT;
                        }
                        b.setze(x, y, z, st);
                    }
                }
            }
        }
        decke(b, f, x1, z1, x2, z2, ly0 + 4 * stockwerke, verfall);
    }

    public static BlockState wandBlock(Baukasten b, Fassade f, int x, int y, int z, int hoehe, int gesamt, double verfall) {
        double w = b.zufall(x, y, z);
        double oben = hoehe / (double) Math.max(1, gesamt);
        if (w < verfall * (0.03 + 0.2 * oben)) {
            return LUFT;
        }
        if (w < verfall * 0.7) {
            return f.alt().defaultBlockState();
        }
        return f.wand().defaultBlockState();
    }

    public static void decke(Baukasten b, Fassade f, int x1, int z1, int x2, int z2, int ly, double verfall) {
        for (int x = x1; x <= x2; x++) {
            for (int z = z1; z <= z2; z++) {
                boolean rand = x == x1 || x == x2 || z == z1 || z == z2;
                BlockState st = rand ? f.wand().defaultBlockState() : f.boden().defaultBlockState();
                if (!rand && b.chance(x, ly + 50, z, verfall * 0.08)) {
                    st = LUFT;
                }
                b.setze(x, ly, z, st);
            }
        }
    }

    /** Innenwand entlang lz (bei x) von z1 bis z2, Etage ab ly0. */
    public static void innenwandZ(Baukasten b, Block block, int x, int z1, int z2, int ly0) {
        for (int z = z1; z <= z2; z++) {
            for (int y = ly0 + 1; y <= ly0 + 3; y++) {
                b.setze(x, y, z, block);
            }
        }
    }

    /** Innenwand entlang lx (bei z) von x1 bis x2, Etage ab ly0. */
    public static void innenwandX(Baukasten b, Block block, int x1, int x2, int z, int ly0) {
        for (int x = x1; x <= x2; x++) {
            for (int y = ly0 + 1; y <= ly0 + 3; y++) {
                b.setze(x, y, z, block);
            }
        }
    }

    public static void durchgang(Baukasten b, int x, int z, int ly0) {
        b.setze(x, ly0 + 1, z, LUFT);
        b.setze(x, ly0 + 2, z, LUFT);
    }

    // ---- Fenster -----------------------------------------------------------------------------

    /**
     * Fenster in allen vier Außenwänden einer Etage (ab ly0), jedes dritte Feld, zwei Blöcke hoch.
     * Felder in {@code frei} (als lx*1000+lz kodiert) bleiben Wand – z. B. neben Türen.
     */
    public static void fenster(Baukasten b, int x1, int z1, int x2, int z2, int ly0, boolean zweiHoch, int... frei) {
        for (int x = x1 + 1; x < x2; x++) {
            if ((x - x1) % 3 == 2) {
                fensterFeld(b, x, z1, ly0, true, zweiHoch, frei);
                fensterFeld(b, x, z2, ly0, true, zweiHoch, frei);
            }
        }
        for (int z = z1 + 1; z < z2; z++) {
            if ((z - z1) % 3 == 2) {
                fensterFeld(b, x1, z, ly0, false, zweiHoch, frei);
                fensterFeld(b, x2, z, ly0, false, zweiHoch, frei);
            }
        }
    }

    private static void fensterFeld(Baukasten b, int x, int z, int ly0, boolean entlangLx, boolean zweiHoch, int[] frei) {
        for (int f : frei) {
            if (f == x * 1000 + z) {
                return;
            }
        }
        for (int y = ly0 + 2; y <= ly0 + (zweiHoch ? 3 : 2); y++) {
            double w = b.zufall(x, y, z);
            BlockState st;
            if (w < 0.38) {
                st = LUFT; // eingeschlagen
            } else if (w < 0.55) {
                st = Blocks.SPRUCE_PLANKS.defaultBlockState(); // zugenagelt
            } else {
                st = b.scheibe(Blocks.GLASS_PANE, entlangLx);
            }
            b.setze(x, y, z, st);
        }
    }

    public static int feld(int x, int z) {
        return x * 1000 + z;
    }

    // ---- Dächer ------------------------------------------------------------------------------

    /**
     * Satteldach über dem Rechteck (x1..x2, z1..z2) mit einem Block Überstand. Die erste Treppenreihe liegt auf
     * Höhe {@code ly} (Deckenebene). {@code firstEntlangLx}: First läuft parallel zur Straße.
     *
     * @return Höhe des Firsts
     */
    public static int satteldach(Baukasten b, Fassade f, int x1, int z1, int x2, int z2, int ly,
                                 boolean firstEntlangLx, double verfall) {
        int qMin = (firstEntlangLx ? z1 : x1) - 1;
        int qMax = (firstEntlangLx ? z2 : x2) + 1;
        int lMin = (firstEntlangLx ? x1 : z1) - 1;
        int lMax = (firstEntlangLx ? x2 : z2) + 1;
        int schichten = (qMax - qMin + 2) / 2;
        int first = ly;
        for (int k = 0; k < schichten; k++) {
            int y = ly + k;
            int a = qMin + k;
            int e = qMax - k;
            if (a > e) {
                break;
            }
            first = y;
            for (int l = lMin; l <= lMax; l++) {
                boolean giebelUeberstand = l == lMin || l == lMax;
                for (int q = a; q <= e; q++) {
                    int x = firstEntlangLx ? l : q;
                    int z = firstEntlangLx ? q : l;
                    BlockState st;
                    if (a == e) {
                        st = f.dachVoll().defaultBlockState();
                    } else if (q == a || q == e) {
                        Direction hoch = firstEntlangLx ? (q == a ? H : V) : (q == a ? R : L);
                        st = treppe(f.dachTreppe(), hoch, false);
                    } else if (k == 0) {
                        continue;
                    } else if (!giebelUeberstand && (l == lMin + 1 || l == lMax - 1)) {
                        st = f.wand().defaultBlockState(); // Giebelwand
                    } else if (giebelUeberstand) {
                        continue;
                    } else {
                        st = LUFT; // Dachboden
                    }
                    boolean dachflaeche = q == a || q == e || a == e;
                    if (dachflaeche && k > 0 && !giebelUeberstand && b.chance(x, y + 100, z, verfall * 0.35)) {
                        st = LUFT; // Loch im Dach
                    }
                    b.setze(x, y, z, st);
                }
            }
        }
        // Rundes Giebelfenster
        int giebelMitte = (qMin + qMax) / 2;
        int yFenster = ly + Math.max(1, schichten / 2 - 1);
        if (schichten >= 4) {
            for (int l : new int[]{lMin + 1, lMax - 1}) {
                int x = firstEntlangLx ? l : giebelMitte;
                int z = firstEntlangLx ? giebelMitte : l;
                b.setze(x, yFenster, z, b.chance(x, yFenster, z, 0.5) ? LUFT : b.scheibe(Blocks.GLASS_PANE, !firstEntlangLx));
            }
        }
        return first;
    }

    /** Flachdach mit kleiner Brüstung. */
    public static void flachdach(Baukasten b, Fassade f, int x1, int z1, int x2, int z2, int ly, double verfall) {
        for (int x = x1; x <= x2; x++) {
            for (int z = z1; z <= z2; z++) {
                boolean rand = x == x1 || x == x2 || z == z1 || z == z2;
                if (rand && !b.chance(x, ly + 1, z, verfall * 0.3)) {
                    b.setze(x, ly + 1, z, stufe(f.dachStufe(), false));
                }
            }
        }
    }

    /** Schornstein von der Decke bis über den First. */
    public static void schornstein(Baukasten b, int x, int z, int von, int bis) {
        for (int y = von; y <= bis; y++) {
            b.setze(x, y, z, Blocks.BRICKS);
        }
        b.setze(x, bis + 1, z, Blocks.BRICK_WALL);
    }

    // ---- Treppen und Türen -------------------------------------------------------------------

    /**
     * Innentreppe: vier Stufen von Etage ly0 auf ly0 + 4, beginnend bei (x, z), aufsteigend in Richtung
     * {@code hoch}. Über den Stufen wird Platz geschaffen und die Öffnung im Boden oben mit Geländer gesichert.
     */
    public static void innentreppe(Baukasten b, Block treppenBlock, Block gelaender, int x, int z, int ly0, Direction hoch) {
        int dx = Baukasten.dx(hoch);
        int dz = Baukasten.dz(hoch);
        for (int k = 0; k < 4; k++) {
            int sx = x + dx * k;
            int sz = z + dz * k;
            b.setze(sx, ly0 + 1 + k, sz, treppe(treppenBlock, hoch, false));
            for (int y = ly0 + 2 + k; y <= ly0 + 4 + k && y < ly0 + 7; y++) {
                if (y != ly0 + 4 || k < 3) {
                    b.setze(sx, y, sz, LUFT);
                }
            }
        }
        // Geländer oben neben der Öffnung
        Direction seite = hoch.getClockWise();
        int gx = Baukasten.dx(seite);
        int gz = Baukasten.dz(seite);
        for (int k = 0; k < 3; k++) {
            b.setzeFrei(x + dx * k + gx, ly0 + 5, z + dz * k + gz, gelaender.defaultBlockState());
        }
    }

    /** Haustür mit Stufe davor; manchmal fehlt die Tür, manchmal steht sie offen. */
    public static void haustuer(Baukasten b, Fassade f, int x, int z, int ly0, Direction raus) {
        double w = b.zufall(x, ly0 + 1, z);
        if (w < 0.35) {
            b.setze(x, ly0 + 1, z, LUFT);
            b.setze(x, ly0 + 2, z, LUFT);
        } else {
            b.tuer(x, ly0 + 1, z, raus, f.tuer(), w < 0.6);
        }
        int sx = x + Baukasten.dx(raus);
        int sz = z + Baukasten.dz(raus);
        b.setze(sx, ly0, sz, f.sockel());
        b.setze(sx, ly0 + 1, sz, LUFT);
        b.setze(sx, ly0 + 2, sz, LUFT);
    }

    // ---- Möbel -------------------------------------------------------------------------------

    /** Sofa aus Treppen; Lehne zur Seite {@code lehne}. */
    public static void sofa(Baukasten b, Block holz, int x, int y, int z, Direction lehne, int laenge, Direction entlang) {
        for (int i = 0; i < laenge; i++) {
            b.setze(x + Baukasten.dx(entlang) * i, y, z + Baukasten.dz(entlang) * i, treppe(holz, lehne, false));
        }
    }

    /** Tisch: Zaunpfosten mit Druckplatte. */
    public static void tisch(Baukasten b, Block zaun, Block platte, int x, int y, int z) {
        b.setze(x, y, z, zaun);
        b.setze(x, y + 1, z, platte);
    }

    public static void stuhl(Baukasten b, Block holzTreppe, int x, int y, int z, Direction lehne) {
        b.setze(x, y, z, treppe(holzTreppe, lehne, false));
    }

    /** Kühlschrank: Kiste mit Beute, darüber ein weißer Block. */
    public static void kuehlschrank(Baukasten b, int x, int y, int z, Direction blick) {
        b.truhe(x, y, z, blick, "kuehlschrank");
        b.setze(x, y + 1, z, Blocks.SMOOTH_QUARTZ);
    }

    /** Hängeschrank an der Wand (Luke) über einer Arbeitsplatte. */
    public static void haengeschrank(Baukasten b, Block luke, int x, int y, int z, Direction blick) {
        b.setze(x, y, z, luke(luke, blick, true, false));
    }

    public static void spinnweben(Baukasten b, int x1, int z1, int x2, int z2, int ly, double chance) {
        for (int x = x1; x <= x2; x++) {
            for (int z = z1; z <= z2; z++) {
                if (b.hole(x, ly, z).isAir() && b.chance(x, ly + 300, z, chance)) {
                    b.setze(x, ly, z, Blocks.COBWEB);
                }
            }
        }
    }

    /** Schutt, Blutspuren und Unordnung auf dem Boden einer Etage (ly = Ebene über dem Boden). */
    public static void unordnung(Baukasten b, int x1, int z1, int x2, int z2, int ly, double verfall) {
        for (int x = x1; x <= x2; x++) {
            for (int z = z1; z <= z2; z++) {
                if (!b.hole(x, ly, z).isAir() || b.hole(x, ly - 1, z).isAir()) {
                    continue;
                }
                double w = b.zufall(x, ly + 400, z);
                if (w < verfall * 0.08) {
                    b.setze(x, ly, z, Blocks.GRAVEL);
                } else if (w < verfall * 0.08 + 0.015) {
                    b.setze(x, ly, z, Blocks.REDSTONE_WIRE); // getrocknetes Blut
                } else if (w > 0.996) {
                    b.setze(x, ly, z, Blocks.SKELETON_SKULL.defaultBlockState()
                            .setValue(net.minecraft.world.level.block.SkullBlock.ROTATION, (int) (w * 100000) % 16));
                }
            }
        }
    }

    public static void teppich(Baukasten b, Block teppich, int x1, int z1, int x2, int z2, int ly) {
        for (int x = x1; x <= x2; x++) {
            for (int z = z1; z <= z2; z++) {
                if (b.hole(x, ly, z).isAir() && !b.hole(x, ly - 1, z).isAir() && !b.chance(x, ly + 500, z, 0.15)) {
                    b.setze(x, ly, z, teppich);
                }
            }
        }
    }

    // ---- Grundstück und Garten ---------------------------------------------------------------

    /** Zaun entlang lz = z von x1 bis x2, mit Lücken an {@code tore} (lx-Werte). */
    public static void zaunVorne(Baukasten b, Block zaun, int x1, int x2, int z, double verfall, int... tore) {
        for (int x = x1; x <= x2; x++) {
            boolean tor = false;
            for (int t : tore) {
                if (t == x) {
                    tor = true;
                }
            }
            if (tor || b.chance(x, 1, z, verfall * 0.4)) {
                continue;
            }
            b.setzeFrei(x, 1, z, b.zaun(zaun, true));
        }
    }

    public static void zaunSeite(Baukasten b, Block zaun, int x, int z1, int z2, double verfall) {
        for (int z = z1; z <= z2; z++) {
            if (!b.chance(x, 1, z, verfall * 0.4)) {
                b.setzeFrei(x, 1, z, b.zaun(zaun, false));
            }
        }
    }

    public static void tor(Baukasten b, Block tor, int x, int z, boolean offen) {
        if (tor.defaultBlockState().hasProperty(FenceGateBlock.FACING)) {
            b.setzeFrei(x, 1, z, tor.defaultBlockState().setValue(FenceGateBlock.FACING, V).setValue(FenceGateBlock.OPEN, offen));
        }
    }

    /** Weg aus Platten von (x, z1) bis (x, z2). */
    public static void weg(Baukasten b, int x, int z1, int z2, int breite) {
        for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) {
            for (int i = 0; i < breite; i++) {
                double w = b.zufall(x + i, 0, z);
                Block platte = w < 0.15 ? Blocks.COARSE_DIRT : w < 0.3 ? Blocks.MOSSY_STONE_BRICKS : Blocks.STONE_BRICKS;
                b.setze(x + i, 0, z, platte);
                b.setze(x + i, 1, z, LUFT);
            }
        }
    }

    /** Verwilderter Garten: toter Rasen, Erdflecken, vertrocknete Büsche, ab und zu eine Wither-Rose. */
    public static void verwildern(Baukasten b, int x1, int z1, int x2, int z2) {
        for (int x = x1; x <= x2; x++) {
            for (int z = z1; z <= z2; z++) {
                if (!b.hole(x, 1, z).isAir()) {
                    continue;
                }
                Block boden = b.hole(x, 0, z).getBlock();
                if (boden != Blocks.GRASS_BLOCK && boden != Blocks.DIRT && boden != Blocks.COARSE_DIRT) {
                    continue;
                }
                double w = b.zufall(x, 1, z);
                if (w < 0.10) {
                    b.setze(x, 0, z, Blocks.COARSE_DIRT);
                } else if (w < 0.22) {
                    b.setze(x, 1, z, Blocks.SHORT_GRASS);
                } else if (w < 0.27) {
                    b.setze(x, 0, z, Blocks.COARSE_DIRT);
                    b.setze(x, 1, z, Blocks.DEAD_BUSH);
                } else if (w < 0.29) {
                    b.setze(x, 1, z, Blocks.TALL_GRASS.defaultBlockState());
                } else if (w > 0.994) {
                    b.setze(x, 1, z, Blocks.WITHER_ROSE);
                }
            }
        }
    }

    /** Ein kahler Baum im Garten. */
    public static void toterBaum(Baukasten b, int x, int z, Random r) {
        int h = 3 + r.nextInt(3);
        for (int y = 1; y <= h; y++) {
            b.setze(x, y, z, Blocks.DARK_OAK_LOG);
        }
        for (Direction d : new Direction[]{V, H, L, R}) {
            if (r.nextBoolean()) {
                b.setzeFrei(x + Baukasten.dx(d), h, z + Baukasten.dz(d), Blocks.DARK_OAK_FENCE.defaultBlockState());
            }
        }
        b.setzeFrei(x, h + 1, z, Blocks.DARK_OAK_FENCE.defaultBlockState());
    }

    /** Briefkasten am Tor – manchmal liegt noch Post darin. */
    public static void briefkasten(Baukasten b, int x, int z) {
        b.setze(x, 1, z, Blocks.SPRUCE_FENCE);
        b.fass(x, 2, z, V, "briefkasten");
    }
}
