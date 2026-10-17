package de.deadzonemc.welt.gebaeude;

import de.deadzonemc.welt.bau.Baukasten;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Blocks;

import java.util.Random;

import static de.deadzonemc.welt.bau.Baukasten.H;
import static de.deadzonemc.welt.bau.Baukasten.L;
import static de.deadzonemc.welt.bau.Baukasten.R;
import static de.deadzonemc.welt.bau.Baukasten.V;
import static de.deadzonemc.welt.gebaeude.Hausbau.LUFT;

/**
 * Feuerwehrhaus: Halle mit zwei Toren, darin ein Löschfahrzeug, Spinde mit Ausrüstung und ein
 * Schlauchturm. Hier gibt es später Äxte, Helme und Schutzkleidung.
 */
public class Feuerwehr implements Gebaeude {

    @Override
    public int hoehe() {
        return 18;
    }

    @Override
    public void bauen(Baukasten b, Info info) {
        Random r = new Random(b.saat);
        int B = b.breite;
        int T = b.tiefe;
        double verfall = 0.2 + r.nextDouble() * 0.3;
        Fassade f = Fassade.ROT;
        b.boden(0, 0, B - 1, T - 1, Blocks.GRASS_BLOCK.defaultBlockState(), 18);

        int x1 = 1;
        int x2 = Math.min(B - 5, x1 + 13);
        int z1 = 5;
        int z2 = Math.min(T - 3, z1 + 13);
        // Halle: hohe Wände (zwei Etagen hoch, ohne Zwischendecke)
        Hausbau.rohbau(b, f, x1, z1, x2, z2, 0, 2, verfall);
        for (int x = x1 + 1; x < x2; x++) {
            for (int z = z1 + 1; z < z2; z++) {
                b.setze(x, 4, z, LUFT);
                b.setze(x, 0, z, Blocks.SMOOTH_STONE);
            }
        }
        Hausbau.flachdach(b, f, x1, z1, x2, z2, 8, verfall);
        // Zwei Tore, eins offen, eins halb zu
        int breiteTor = 4;
        int t1 = x1 + 2;
        int t2 = x2 - 1 - breiteTor;
        for (int x = t1; x < t1 + breiteTor; x++) {
            for (int y = 1; y <= 5; y++) {
                b.setze(x, y, z1, LUFT);
            }
        }
        for (int x = t2; x < t2 + breiteTor; x++) {
            for (int y = 1; y <= 5; y++) {
                b.setze(x, y, z1, y >= 4 ? Hausbau.luke(Blocks.IRON_TRAPDOOR, V, y == 5, false) : LUFT);
            }
        }
        b.schild((x1 + x2) / 2, 6, z1 - 1, V, Blocks.SPRUCE_WALL_SIGN, DyeColor.WHITE, "Freiwillige", "Feuerwehr",
                info.strasse(), "");
        // Vorplatz
        for (int x = x1; x <= x2; x++) {
            for (int z = 0; z < z1; z++) {
                b.setze(x, 0, z, b.chance(x, 0, z, 0.1) ? Blocks.CRACKED_STONE_BRICKS : Blocks.SMOOTH_STONE);
                b.setze(x, 1, z, LUFT);
            }
        }
        // Löschfahrzeug in der ersten Box
        loeschfahrzeug(b.teil(t1, z1 + 2, t1 + 2, z1 + 8, 1, V, b.saat ^ 99), r);
        // Spinde mit Ausrüstung an der rechten Wand
        for (int z = z1 + 2; z < z2 - 1; z += 2) {
            b.fass(x2 - 1, 1, z, L, "feuerwehr");
            b.fass(x2 - 1, 2, z, L, "feuerwehr");
        }
        b.truhe(x1 + 1, 1, z2 - 1, V, "feuerwehr");
        // Schläuche (aufgerollt) und Helme an der Rückwand
        b.setze(x1 + 3, 2, z2 - 1, Blocks.CHAIN);
        b.setze(x1 + 4, 1, z2 - 1, Blocks.RED_WOOL);
        Hausbau.spinnweben(b, x1 + 1, z1 + 1, x2 - 1, z2 - 1, 7, 0.15);
        Hausbau.unordnung(b, x1 + 1, z1 + 1, x2 - 1, z2 - 1, 1, verfall);

        // Schlauchturm hinten rechts
        int tx1 = x2 + 1;
        int tz1 = z2 - 3;
        if (B - 1 - tx1 >= 3) {
            for (int y = 0; y <= 14; y++) {
                for (int x = tx1; x <= tx1 + 2; x++) {
                    for (int z = tz1; z <= tz1 + 2; z++) {
                        boolean rand = x == tx1 || x == tx1 + 2 || z == tz1 || z == tz1 + 2;
                        b.setze(x, y, z, y == 0 ? Blocks.STONE_BRICKS : rand ? Blocks.RED_TERRACOTTA : Blocks.AIR);
                    }
                }
            }
            b.leiter(tx1 + 1, 1, 13, tz1 + 1, V);
            b.setze(tx1 + 1, 1, tz1, LUFT);
            b.setze(tx1 + 1, 2, tz1, LUFT);
            for (int x = tx1 - 1; x <= tx1 + 3; x++) {
                for (int z = tz1 - 1; z <= tz1 + 3; z++) {
                    b.setze(x, 15, z, Hausbau.stufe(Blocks.DEEPSLATE_TILE_SLAB, false));
                }
            }
            b.setze(tx1 + 1, 13, tz1 + 1, Blocks.AIR);
            b.setze(tx1 + 1, 12, tz1, LUFT);
            b.setze(tx1 + 1, 12, tz1 + 2, LUFT);
        }
        Hausbau.verwildern(b, 0, 0, B - 1, T - 1);
    }

    /** Rotes Löschfahrzeug: 7 lang, 3 breit, Leiter auf dem Dach. */
    private void loeschfahrzeug(Baukasten b, Random r) {
        for (int lz = 0; lz < 7; lz++) {
            for (int lx = 0; lx < 3; lx++) {
                boolean rad = (lz == 1 || lz == 5) && lx != 1;
                b.setze(lx, 0, lz, rad ? Blocks.BLACK_CONCRETE : Blocks.RED_CONCRETE);
                if (lz == 0) {
                    b.setze(lx, 1, lz, Hausbau.stufe(Blocks.RED_NETHER_BRICK_SLAB, false));
                } else if (lz <= 2) {
                    b.setze(lx, 1, lz, lx == 1 ? Blocks.AIR : (lz == 1 ? Blocks.GRAY_STAINED_GLASS : Blocks.RED_CONCRETE));
                    b.setze(lx, 2, lz, Blocks.RED_CONCRETE);
                } else {
                    b.setze(lx, 1, lz, lx == 1 && lz == 4 ? Blocks.WHITE_CONCRETE : Blocks.RED_CONCRETE);
                    if (lx == 1) {
                        b.setze(lx, 2, lz, Blocks.LADDER.defaultBlockState()
                                .setValue(net.minecraft.world.level.block.LadderBlock.FACING, V));
                    }
                }
            }
        }
        b.setze(1, 3, 1, Blocks.BLUE_STAINED_GLASS); // Blaulicht
        b.fass(0, 1, 6, H, "feuerwehr");
    }
}
