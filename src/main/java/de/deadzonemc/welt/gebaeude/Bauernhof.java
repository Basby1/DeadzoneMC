package de.deadzonemc.welt.gebaeude;

import de.deadzonemc.welt.bau.Baukasten;
import de.deadzonemc.welt.bau.Fahrzeuge;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CarvedPumpkinBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmBlock;

import java.util.Random;

import static de.deadzonemc.welt.bau.Baukasten.H;
import static de.deadzonemc.welt.bau.Baukasten.L;
import static de.deadzonemc.welt.bau.Baukasten.R;
import static de.deadzonemc.welt.bau.Baukasten.V;
import static de.deadzonemc.welt.gebaeude.Hausbau.LUFT;

/**
 * Bauernhof: Fachwerk-Wohnhaus vorne, große Scheune hinten, Hof mit Traktor und Brunnen, ein
 * verwildertes Feld mit Vogelscheuche und eine Weide mit Holzzaun.
 */
public class Bauernhof implements Gebaeude {

    @Override
    public int hoehe() {
        return 22;
    }

    @Override
    public void bauen(Baukasten b, Info info) {
        Random r = new Random(b.saat);
        int B = b.breite;
        int T = b.tiefe;
        double verfall = 0.2 + r.nextDouble() * 0.4;
        b.boden(0, 0, B - 1, T - 1, Blocks.GRASS_BLOCK.defaultBlockState(), 16);

        // Hofzufahrt und Hof (Schotter)
        int hofX1 = 14;
        int hofX2 = B - 3;
        int hofZ2 = T - 20;
        for (int x = hofX1; x <= hofX2; x++) {
            for (int z = 0; z <= hofZ2; z++) {
                b.setze(x, 0, z, b.chance(x, 0, z, 0.25) ? Blocks.COARSE_DIRT : Blocks.GRAVEL);
            }
        }

        // Wohnhaus (Fachwerk) vorne links
        int hz1 = 4;
        int ht = 9;
        Einfamilienhaus.haus(b, r, Fassade.FACHWERK, 2, hz1, 11, ht, 2, r.nextBoolean(), verfall, info);
        fachwerk(b, 2, hz1, 12, hz1 + ht - 1);

        // Scheune hinten rechts
        int sx1 = Math.max(hofX1, B - 17);
        int sx2 = B - 3;
        int sz1 = T - 18;
        int sz2 = T - 3;
        scheune(b, r, sx1, sz1, sx2, sz2, verfall);

        // Feld hinten links mit Vogelscheuche
        int fx1 = 2;
        int fx2 = Math.max(fx1 + 6, sx1 - 3);
        int fz1 = hz1 + ht + 4;
        int fz2 = T - 3;
        feld(b, r, fx1, fz1, fx2, fz2);

        // Traktor im Hof, Brunnen, Misthaufen
        Fahrzeuge.traktor(b.teil(hofX1 + 3, 6, hofX1 + 5, 9, 1, r.nextBoolean() ? V : H, b.saat ^ 5), b.saat ^ 6);
        brunnen(b, hofX2 - 4, 4);
        b.fuelle(hofX1 + 1, 1, hofZ2 - 2, hofX1 + 3, 1, hofZ2 - 1, Blocks.COARSE_DIRT);
        b.setze(hofX1 + 2, 2, hofZ2 - 2, Blocks.COMPOSTER);
        // Heuballen im Hof
        b.setze(hofX2 - 1, 1, hofZ2 - 1, Blocks.HAY_BLOCK);
        b.setze(hofX2 - 1, 1, hofZ2 - 2, Blocks.HAY_BLOCK);
        b.setze(hofX2 - 1, 2, hofZ2 - 1, Blocks.HAY_BLOCK);

        // Zaun vorne mit großem Hoftor
        Hausbau.zaunVorne(b, Blocks.SPRUCE_FENCE, 0, B - 1, 0, verfall, 7, hofX1 + 2, hofX1 + 3, hofX1 + 4, hofX1 + 5);
        Hausbau.zaunSeite(b, Blocks.SPRUCE_FENCE, 0, 1, T - 1, verfall);
        Hausbau.zaunSeite(b, Blocks.SPRUCE_FENCE, B - 1, 1, T - 1, verfall);
        Hausbau.weg(b, 7, 0, hz1 - 2, 1);
        Hausbau.briefkasten(b, 8, 0);
        Hausbau.verwildern(b, 0, 0, B - 1, T - 1);
        Hausbau.toterBaum(b, 1 + r.nextInt(3), hz1 + ht + 2, r);
    }

    /** Dunkle Holzbalken auf den Fassaden: waagerecht auf Höhe der Geschossdecke, senkrecht alle drei Blöcke. */
    private void fachwerk(Baukasten b, int x1, int z1, int x2, int z2) {
        var quer = Hausbau.balken(Blocks.DARK_OAK_LOG, Direction.Axis.X);
        var laengs = Hausbau.balken(Blocks.DARK_OAK_LOG, Direction.Axis.Z);
        var senkrecht = Blocks.DARK_OAK_LOG.defaultBlockState();
        for (int x = x1; x <= x2; x++) {
            setzeWenn(b, x, 4, z1, quer);
            setzeWenn(b, x, 4, z2, quer);
        }
        for (int z = z1; z <= z2; z++) {
            setzeWenn(b, x1, 4, z, laengs);
            setzeWenn(b, x2, 4, z, laengs);
        }
        for (int y = 1; y <= 7; y++) {
            for (int x = x1 + 3; x < x2; x += 3) {
                setzeWenn(b, x, y, z1, senkrecht);
                setzeWenn(b, x, y, z2, senkrecht);
            }
        }
    }

    /** Ersetzt nur Putz (keine Fenster, keine Löcher). */
    private static void setzeWenn(Baukasten b, int x, int y, int z, net.minecraft.world.level.block.state.BlockState st) {
        var da = b.hole(x, y, z);
        if (da.is(Blocks.WHITE_TERRACOTTA) || da.is(Blocks.CALCITE)) {
            b.setze(x, y, z, st);
        }
    }

    private void scheune(Baukasten b, Random r, int x1, int z1, int x2, int z2, double verfall) {
        Fassade f = Fassade.HOLZ;
        // Wände 6 hoch, Pfosten aus dunklem Holz
        b.fundament(x1, z1, x2, z2, Blocks.COBBLESTONE.defaultBlockState());
        for (int x = x1; x <= x2; x++) {
            for (int z = z1; z <= z2; z++) {
                boolean rand = x == x1 || x == x2 || z == z1 || z == z2;
                b.setze(x, 0, z, rand ? Blocks.COBBLESTONE : Blocks.COARSE_DIRT);
                for (int y = 1; y <= 6; y++) {
                    Block st;
                    boolean wandEntlangX = z == z1 || z == z2;
                    boolean pfosten = wandEntlangX ? ((x - x1) % 4 == 0 || x == x2) : ((z - z1) % 4 == 0 || z == z2);
                    if (!rand) {
                        st = Blocks.AIR;
                    } else if (pfosten) {
                        st = Blocks.DARK_OAK_LOG;
                    } else if (b.chance(x, y, z, verfall * 0.15 * y / 3.0)) {
                        st = Blocks.AIR;
                    } else {
                        st = y == 1 ? Blocks.COBBLESTONE : (b.chance(x, y, z, 0.25) ? Blocks.DARK_OAK_PLANKS : Blocks.SPRUCE_PLANKS);
                    }
                    b.setze(x, y, z, st);
                }
            }
        }
        // Großes Tor vorne
        int mitte = (x1 + x2) / 2;
        for (int x = mitte - 2; x <= mitte + 2; x++) {
            for (int y = 1; y <= 5; y++) {
                b.setze(x, y, z1, LUFT);
            }
        }
        // Heuboden über der hinteren Hälfte
        int zl = (z1 + z2) / 2;
        for (int x = x1 + 1; x < x2; x++) {
            for (int z = zl; z < z2; z++) {
                if (!b.chance(x, 4, z, verfall * 0.2)) {
                    b.setze(x, 4, z, Hausbau.stufe(Blocks.SPRUCE_SLAB, true));
                }
            }
        }
        b.leiter(x1 + 1, 1, 5, zl - 1, V);
        // Steiles Dach
        Hausbau.satteldach(b, f, x1, z1, x2, z2, 7, false, verfall);
        // Heu, Werkzeug, Kisten
        for (int z = z1 + 1; z < zl; z++) {
            if (b.chance(x1 + 1, 1, z, 0.6)) {
                b.setze(x1 + 1, 1, z, Blocks.HAY_BLOCK);
                if (b.chance(x1 + 1, 2, z, 0.5)) {
                    b.setze(x1 + 1, 2, z, Blocks.HAY_BLOCK);
                }
            }
        }
        for (int x = x1 + 2; x < x2 - 1; x += 2) {
            if (b.chance(x, 5, zl + 1, 0.7)) {
                b.setze(x, 5, zl + 1, Blocks.HAY_BLOCK);
            }
        }
        b.truhe(x2 - 1, 1, z2 - 1, L, "scheune");
        b.truhe(x2 - 1, 1, z2 - 2, L, "scheune");
        b.setze(x2 - 1, 1, z1 + 2, Blocks.GRINDSTONE);
        b.setze(x2 - 1, 1, z1 + 3, Blocks.CHIPPED_ANVIL);
        b.truhe(x1 + 2, 5, z2 - 1, V, "scheune");
        Hausbau.spinnweben(b, x1 + 1, z1 + 1, x2 - 1, z2 - 1, 6, 0.25);
        Hausbau.spinnweben(b, x1 + 1, z1 + 1, x2 - 1, zl - 1, 3, 0.08);
    }

    private void feld(Baukasten b, Random r, int x1, int z1, int x2, int z2) {
        if (x2 - x1 < 4 || z2 - z1 < 4) {
            return;
        }
        for (int x = x1; x <= x2; x++) {
            for (int z = z1; z <= z2; z++) {
                boolean furche = (x - x1) % 3 == 2;
                if (furche) {
                    b.setze(x, 0, z, Blocks.COARSE_DIRT);
                    b.setze(x, 1, z, b.chance(x, 1, z, 0.2) ? Blocks.DEAD_BUSH : Blocks.AIR);
                } else if (b.chance(x, 0, z, 0.65)) {
                    b.setze(x, 0, z, Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, 0));
                    // Nie geerntet: überreifes Getreide oder Kartoffeln
                    Block frucht = b.chance(x, 1, z, 0.7) ? Blocks.WHEAT : Blocks.POTATOES;
                    b.setze(x, 1, z, frucht.defaultBlockState().setValue(CropBlock.AGE, 7));
                } else {
                    b.setze(x, 0, z, Blocks.COARSE_DIRT);
                    b.setze(x, 1, z, b.chance(x, 2, z, 0.3) ? Blocks.SHORT_GRASS : Blocks.AIR);
                }
            }
        }
        // Vogelscheuche mitten im Feld, mit Kürbiskopf zur Straße
        int vx = (x1 + x2) / 2;
        int vz = (z1 + z2) / 2;
        b.setze(vx, 0, vz, Blocks.COARSE_DIRT);
        b.setze(vx, 1, vz, Blocks.SPRUCE_FENCE);
        b.setze(vx, 2, vz, Blocks.HAY_BLOCK);
        b.setze(vx - 1, 2, vz, Blocks.SPRUCE_FENCE);
        b.setze(vx + 1, 2, vz, Blocks.SPRUCE_FENCE);
        b.setze(vx, 3, vz, Blocks.CARVED_PUMPKIN.defaultBlockState().setValue(CarvedPumpkinBlock.FACING, V));
        // Feldrand mit Zaun
        for (int x = x1; x <= x2; x++) {
            b.setzeFrei(x, 1, z1 - 1, b.zaun(Blocks.SPRUCE_FENCE, true));
        }
    }

    private void brunnen(Baukasten b, int x, int z) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                boolean mitte = dx == 0 && dz == 0;
                b.setze(x + dx, 0, z + dz, mitte ? Blocks.WATER : Blocks.MOSSY_COBBLESTONE);
                b.setze(x + dx, 1, z + dz, mitte ? Blocks.AIR : Blocks.COBBLESTONE_WALL);
                b.setze(x + dx, -1, z + dz, mitte ? Blocks.WATER : Blocks.COBBLESTONE);
            }
        }
        b.setze(x - 1, 2, z, Blocks.SPRUCE_FENCE);
        b.setze(x + 1, 2, z, Blocks.SPRUCE_FENCE);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                b.setze(x + dx, 3, z + dz, Hausbau.stufe(Blocks.SPRUCE_SLAB, false));
            }
        }
        b.setze(x, 2, z, Blocks.CHAIN);
    }
}
