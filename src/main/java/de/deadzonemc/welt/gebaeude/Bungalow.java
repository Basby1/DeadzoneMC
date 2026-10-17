package de.deadzonemc.welt.gebaeude;

import de.deadzonemc.welt.bau.Baukasten;
import de.deadzonemc.welt.bau.Fahrzeuge;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.Random;

import static de.deadzonemc.welt.bau.Baukasten.H;
import static de.deadzonemc.welt.bau.Baukasten.L;
import static de.deadzonemc.welt.bau.Baukasten.R;
import static de.deadzonemc.welt.bau.Baukasten.V;
import static de.deadzonemc.welt.gebaeude.Hausbau.LUFT;
import static de.deadzonemc.welt.gebaeude.Hausbau.feld;

/**
 * Flacher Bungalow aus den Siebzigern: breite Fensterfront, offener Wohn- und Essbereich mit Küchenzeile,
 * Schlafzimmer und Bad, Terrasse hinter dem Haus und ein Carport.
 */
public class Bungalow implements Gebaeude {

    private static final Fassade[] FASSADEN = {Fassade.PUTZ_WEISS, Fassade.BETON, Fassade.KLINKER, Fassade.PUTZ_GELB};

    @Override
    public int hoehe() {
        return 9;
    }

    @Override
    public void bauen(Baukasten b, Info info) {
        Random r = new Random(b.saat);
        int B = b.breite;
        int T = b.tiefe;
        Fassade f = FASSADEN[r.nextInt(FASSADEN.length)];
        double verfall = 0.15 + r.nextDouble() * 0.4;
        boolean carport = B >= 21 && r.nextBoolean();
        int hb = Math.min(B - (carport ? 9 : 4), 13 + 2 * r.nextInt(2));
        int ht = 9 + r.nextInt(2);
        int vz = Math.min(5, Math.max(2, T - ht - 5));
        int x1 = carport ? 2 : (B - hb) / 2;
        int x2 = x1 + hb - 1;
        int z1 = vz;
        int z2 = vz + ht - 1;
        int xt = x1 + 2;
        int xw = x1 + hb * 3 / 5;
        int zb = z1 + ht / 2;
        Block treppe = Hausbau.treppeZu(f.boden());

        b.boden(0, 0, B - 1, T - 1, Blocks.GRASS_BLOCK.defaultBlockState(), 12);
        Hausbau.rohbau(b, f, x1, z1, x2, z2, 0, 1, verfall);
        Hausbau.flachdach(b, f, x1, z1, x2, z2, 4, verfall);

        // Breite Fensterfront im Wohnbereich
        for (int x = xt + 2; x < xw; x++) {
            for (int y = 1; y <= 3; y++) {
                b.setze(x, y, z1, b.chance(x, y, z1, 0.45) ? LUFT : b.scheibe(Blocks.GLASS_PANE, true));
            }
        }
        Hausbau.fenster(b, x1, z1, x2, z2, 0, false, feld(xt, z1), feld(xt + 1, z1), feld(xt - 1, z1));
        Hausbau.haustuer(b, f, xt, z1, 0, V);
        b.schild(xt - 1, 3, z1 - 1, V, Blocks.BIRCH_WALL_SIGN, DyeColor.BLACK, info.strasse(),
                "Nr. " + info.hausnummer(), "Fam. " + Einfamilienhaus.FAMILIEN[r.nextInt(Einfamilienhaus.FAMILIEN.length)]);

        // Innen: Wohnen/Essen/Küche links, Schlafzimmer und Bad rechts
        Hausbau.innenwandZ(b, f.boden(), xw, z1 + 1, z2 - 1, 0);
        Hausbau.durchgang(b, xw, z1 + 2, 0);
        Hausbau.innenwandX(b, f.boden(), xw + 1, x2 - 1, zb, 0);
        Hausbau.durchgang(b, xw, zb + 1, 0);

        // Küchenzeile an der Rückwand
        int kx = x1 + 1;
        Hausbau.kuehlschrank(b, kx, 1, z2 - 1, V);
        b.setze(kx + 1, 1, z2 - 1, Blocks.SMOKER.defaultBlockState()
                .setValue(net.minecraft.world.level.block.SmokerBlock.FACING, V));
        b.setze(kx + 2, 1, z2 - 1, Blocks.CAULDRON);
        for (int x = kx + 3; x < xw - 1; x++) {
            b.fass(x, 1, z2 - 1, V, "kueche");
            Hausbau.haengeschrank(b, f.fensterladen(), x, 3, z2 - 1, V);
        }
        // Esstisch mit Stühlen
        int tz = (z1 + z2) / 2 + 1;
        int tx = x1 + 3;
        Hausbau.tisch(b, f.zaun(), Blocks.OAK_PRESSURE_PLATE, tx, 1, tz);
        Hausbau.tisch(b, f.zaun(), Blocks.OAK_PRESSURE_PLATE, tx + 1, 1, tz);
        Hausbau.stuhl(b, treppe, tx - 1, 1, tz, L);
        Hausbau.stuhl(b, treppe, tx + 2, 1, tz, R);
        // Sofaecke vorne und Fernseher
        Hausbau.sofa(b, treppe, x1 + 1, 1, z1 + 2, L, 2, H);
        b.setze(xw - 1, 1, z1 + 2, Blocks.BLACK_CONCRETE);
        b.setze(xw - 1, 2, z1 + 2, Blocks.BLACK_STAINED_GLASS_PANE);
        b.fass(xw - 1, 1, z1 + 3, L, "wohnzimmer");
        Hausbau.teppich(b, Blocks.GRAY_CARPET, x1 + 2, z1 + 1, xw - 2, z1 + 3, 1);

        // Schlafzimmer vorne rechts
        int wand = x2 - 1;
        Block bett = Einfamilienhaus.BETTEN[r.nextInt(Einfamilienhaus.BETTEN.length)];
        b.bett(wand, 1, z1 + 2, R, bett);
        if (zb - 1 > z1 + 3) {
            b.bett(wand, 1, z1 + 3, R, bett);
        }
        b.fass(wand, 1, z1 + 1, L, "nachttisch");
        b.fass(xw + 1, 1, zb - 1, V, "kleiderschrank");
        b.fass(xw + 1, 2, zb - 1, V, "kleiderschrank");
        // Bad hinten rechts
        b.setze(wand, 1, zb + 1, Blocks.CAULDRON);
        b.fass(wand, 2, zb + 1, L, "badezimmer");
        b.setze(wand, 1, z2 - 1, Hausbau.treppe(Blocks.QUARTZ_STAIRS, R, false));

        Hausbau.spinnweben(b, x1 + 1, z1 + 1, x2 - 1, z2 - 1, 3, 0.1 + verfall * 0.15);
        Hausbau.unordnung(b, x1 + 1, z1 + 1, x2 - 1, z2 - 1, 1, verfall);

        // Terrasse hinten mit Gartenmöbeln
        if (T - z2 >= 5) {
            for (int x = x1 + 1; x <= Math.min(x2 - 1, x1 + 7); x++) {
                for (int z = z2 + 1; z <= z2 + 3; z++) {
                    b.setze(x, 0, z, Blocks.SMOOTH_STONE);
                    b.setze(x, 1, z, LUFT);
                }
            }
            Hausbau.tisch(b, Blocks.BIRCH_FENCE, Blocks.BIRCH_PRESSURE_PLATE, x1 + 3, 1, z2 + 2);
            Hausbau.stuhl(b, Blocks.BIRCH_STAIRS, x1 + 2, 1, z2 + 2, L);
            Hausbau.stuhl(b, Blocks.BIRCH_STAIRS, x1 + 4, 1, z2 + 2, R);
            // Sonnenschirm
            b.setze(x1 + 6, 1, z2 + 2, Blocks.BIRCH_FENCE);
            b.setze(x1 + 6, 2, z2 + 2, Blocks.BIRCH_FENCE);
            b.setze(x1 + 6, 3, z2 + 2, Blocks.WHITE_WOOL);
        }

        // Carport mit Auto
        if (carport) {
            int cx1 = x2 + 2;
            int cx2 = Math.min(B - 2, cx1 + 4);
            for (int x = cx1; x <= cx2; x++) {
                for (int z = 0; z <= Math.min(T - 2, z1 + 7); z++) {
                    b.setze(x, 0, z, b.chance(x, 0, z, 0.2) ? Blocks.COARSE_DIRT : Blocks.GRAVEL);
                    b.setze(x, 1, z, LUFT);
                }
            }
            for (int z : new int[]{z1, z1 + 6}) {
                for (int y = 1; y <= 3; y++) {
                    b.setze(cx1, y, z, Blocks.SPRUCE_FENCE);
                    b.setze(cx2, y, z, Blocks.SPRUCE_FENCE);
                }
            }
            for (int x = cx1; x <= cx2; x++) {
                for (int z = z1; z <= z1 + 6; z++) {
                    if (!b.chance(x, 4, z, verfall * 0.4)) {
                        b.setze(x, 4, z, Hausbau.stufe(Blocks.SPRUCE_SLAB, false));
                    }
                }
            }
            if (r.nextBoolean() && cx2 - cx1 >= 3) {
                Fahrzeuge.auto(b.teil(cx1 + 1, z1 + 1, cx1 + 3, z1 + 5, 1, V, b.saat ^ 77), b.saat ^ 78, false);
            }
        }

        // Vorgarten
        if (r.nextInt(4) > 0) {
            Block hecke = Blocks.SPRUCE_FENCE;
            Hausbau.zaunVorne(b, hecke, 0, B - 1, 0, verfall, xt, x2 + 2, x2 + 3, x2 + 4, x2 + 5, x2 + 6);
        }
        Hausbau.weg(b, xt, 0, z1 - 2, 1);
        Hausbau.briefkasten(b, xt + 1, 0);
        Hausbau.verwildern(b, 0, 0, B - 1, T - 1);
        if (T - z2 > 6) {
            Hausbau.toterBaum(b, B - 4, T - 3, r);
        }
    }
}
