package de.deadzonemc.welt.gebaeude;

import de.deadzonemc.welt.bau.Baukasten;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;

import java.util.Random;

import static de.deadzonemc.welt.bau.Baukasten.H;
import static de.deadzonemc.welt.bau.Baukasten.L;
import static de.deadzonemc.welt.bau.Baukasten.R;
import static de.deadzonemc.welt.bau.Baukasten.V;
import static de.deadzonemc.welt.gebaeude.Hausbau.LUFT;

/**
 * Dorfgasthof: Schankraum mit Theke und Tischen, Küche, oben Fremdenzimmer, daneben ein Biergarten.
 */
public class Gasthaus implements Gebaeude {

    private static final String[] TIERE = {"Hirsch", "Löwen", "Adler", "Ochsen", "Schwan", "Bären", "Lamm", "Raben"};

    @Override
    public int hoehe() {
        return 20;
    }

    @Override
    public void bauen(Baukasten b, Info info) {
        Random r = new Random(b.saat);
        int B = b.breite;
        int T = b.tiefe;
        double verfall = 0.2 + r.nextDouble() * 0.35;
        Fassade f = r.nextBoolean() ? Fassade.FACHWERK : Fassade.ZIEGEL;
        b.boden(0, 0, B - 1, T - 1, Blocks.GRASS_BLOCK.defaultBlockState(), 18);

        int x1 = 1;
        int x2 = Math.min(B - 2, x1 + 12);
        int z1 = 3;
        int z2 = Math.min(T - 3, z1 + 11);
        int xm = (x1 + x2) / 2;
        Block treppe = Hausbau.treppeZu(f.boden());
        Hausbau.rohbau(b, f, x1, z1, x2, z2, 0, 2, verfall);
        Hausbau.fenster(b, x1, z1, x2, z2, 0, true, Hausbau.feld(xm, z1), Hausbau.feld(xm + 1, z1),
                Hausbau.feld(xm - 1, z1));
        Hausbau.fenster(b, x1, z1, x2, z2, 4, false);
        Hausbau.satteldach(b, f, x1, z1, x2, z2, 8, true, verfall);
        Hausbau.haustuer(b, f, xm, z1, 0, V);
        b.schild(xm + 1, 3, z1 - 1, V, Blocks.DARK_OAK_WALL_SIGN, DyeColor.YELLOW, "Gasthof", "Zum " + TIERE[r.nextInt(TIERE.length)],
                "Warme Küche", "bis 22 Uhr");

        // Theke an der hinteren Wand des Schankraums
        int theke = z2 - 4;
        for (int x = x1 + 1; x <= xm + 1; x++) {
            b.setze(x, 1, theke, Hausbau.treppe(Blocks.SPRUCE_STAIRS, H, true));
            if (x % 2 == 0) {
                b.setze(x, 1, theke - 1, Blocks.SPRUCE_FENCE); // Barhocker
            }
        }
        for (int x = x1 + 1; x <= xm; x++) {
            b.fass(x, 1, z2 - 1, V, "kneipe");
            if (b.chance(x, 2, z2 - 1, 0.5)) {
                b.fass(x, 2, z2 - 1, V, "kneipe");
            }
        }
        // Tische mit Stühlen
        for (int x = x1 + 2; x <= x2 - 3; x += 4) {
            int z = z1 + 3;
            if (Math.abs(x - xm) <= 1) {
                continue;
            }
            Hausbau.tisch(b, Blocks.SPRUCE_FENCE, Blocks.SPRUCE_PRESSURE_PLATE, x, 1, z);
            Hausbau.stuhl(b, treppe, x - 1, 1, z, L);
            Hausbau.stuhl(b, treppe, x + 1, 1, z, R);
            if (b.chance(x, 1, z + 1, 0.4)) {
                b.setze(x + 1, 1, z, Hausbau.treppe(treppe, H, false)); // umgekippter Stuhl
            }
        }
        // Kamin rechts
        b.setze(x2 - 1, 1, z1 + 4, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, false));
        b.setze(x2 - 1, 1, z1 + 3, Blocks.BRICKS);
        b.setze(x2 - 1, 1, z1 + 5, Blocks.BRICKS);
        b.setze(x2 - 1, 2, z1 + 3, Blocks.BRICKS);
        b.setze(x2 - 1, 2, z1 + 5, Blocks.BRICKS);
        b.setze(x2 - 1, 3, z1 + 4, Blocks.BRICKS);
        // Küche hinten rechts
        Hausbau.innenwandZ(b, f.boden(), xm + 2, theke, z2 - 1, 0);
        Hausbau.durchgang(b, xm + 2, theke + 1, 0);
        Hausbau.kuehlschrank(b, x2 - 1, 1, z2 - 1, L);
        b.setze(x2 - 1, 1, z2 - 2, Blocks.SMOKER.defaultBlockState()
                .setValue(net.minecraft.world.level.block.SmokerBlock.FACING, L));
        b.setze(x2 - 1, 1, z2 - 3, Blocks.CAULDRON);
        b.fass(x2 - 2, 1, z2 - 1, V, "kueche");

        // Treppe nach oben, Fremdenzimmer
        Hausbau.innentreppe(b, treppe, f.zaun(), x1 + 1, z1 + 5, 0, H);
        for (int i = 0; i < 3; i++) {
            int zx = x1 + 3 + i * 3;
            if (zx + 1 >= x2) {
                break;
            }
            Hausbau.innenwandZ(b, f.boden(), zx + 2, z1 + 1, z2 - 4, 4);
            b.bett(zx, 5, z1 + 2, V, Einfamilienhaus.BETTEN[r.nextInt(Einfamilienhaus.BETTEN.length)]);
            b.fass(zx + 1, 5, z1 + 1, H, "nachttisch");
        }
        Hausbau.innenwandX(b, f.boden(), x1 + 3, x2 - 1, z2 - 3, 4);
        for (int x = x1 + 4; x < x2; x += 3) {
            Hausbau.durchgang(b, x, z2 - 3, 4);
        }
        Hausbau.spinnweben(b, x1 + 1, z1 + 1, x2 - 1, z2 - 1, 3, 0.1);
        Hausbau.spinnweben(b, x1 + 1, z1 + 1, x2 - 1, z2 - 1, 7, 0.15);
        Hausbau.unordnung(b, x1 + 1, z1 + 1, x2 - 1, z2 - 1, 1, verfall + 0.1);
        Hausbau.unordnung(b, x1 + 1, z1 + 1, x2 - 1, z2 - 1, 5, verfall);

        // Biergarten neben dem Haus
        if (B - x2 >= 6) {
            for (int z = z1 + 1; z < z2; z += 3) {
                int bx = x2 + 3;
                b.setze(bx, 1, z, Blocks.SPRUCE_FENCE);
                b.setze(bx, 2, z, Hausbau.stufe(Blocks.SPRUCE_SLAB, false));
                b.setze(bx - 1, 1, z, Hausbau.treppe(Blocks.SPRUCE_STAIRS, L, false));
                b.setze(bx + 1, 1, z, Hausbau.treppe(Blocks.SPRUCE_STAIRS, R, false));
            }
            Hausbau.toterBaum(b, x2 + 3, z2 + 1, r);
        }
        Hausbau.verwildern(b, 0, 0, B - 1, T - 1);
        Hausbau.weg(b, xm - 1, 0, z1 - 2, 3);
    }
}
