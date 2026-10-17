package de.deadzonemc.welt.gebaeude;

import de.deadzonemc.welt.bau.Baukasten;
import de.deadzonemc.welt.bau.Fahrzeuge;
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
import static de.deadzonemc.welt.gebaeude.Hausbau.feld;

/**
 * Einfamilienhaus mit Vorgarten, Zaun und Briefkasten. Erdgeschoss: Wohnzimmer links, Küche und Bad rechts.
 * Oft ein Obergeschoss mit Schlaf- und Kinderzimmer (echte Treppe), manchmal ein Keller (Leiter) und eine Garage.
 * Hinten ein verwilderter Garten mit Schuppen, Schaukel oder totem Baum.
 */
public class Einfamilienhaus implements Gebaeude {

    static final String[] FAMILIEN = {"Müller", "Schmidt", "Schneider", "Fischer", "Weber", "Meyer", "Wagner",
            "Becker", "Schulz", "Hoffmann", "Koch", "Richter", "Klein", "Wolf", "Neumann", "Schwarz", "Braun",
            "Zimmermann", "Krüger", "Hartmann", "Lange", "Werner", "Krause", "Lehmann", "Köhler", "Brandt"};

    static final Block[] BETTEN = {Blocks.RED_BED, Blocks.WHITE_BED, Blocks.GRAY_BED, Blocks.BROWN_BED,
            Blocks.LIGHT_BLUE_BED, Blocks.GREEN_BED};
    static final Block[] KINDERBETTEN = {Blocks.PINK_BED, Blocks.LIGHT_BLUE_BED, Blocks.YELLOW_BED, Blocks.LIME_BED};
    static final Block[] TEPPICHE = {Blocks.BROWN_CARPET, Blocks.GRAY_CARPET, Blocks.RED_CARPET, Blocks.LIGHT_GRAY_CARPET,
            Blocks.GREEN_CARPET};

    @Override
    public int hoehe() {
        return 20;
    }

    @Override
    public void bauen(Baukasten b, Info info) {
        Random r = new Random(b.saat);
        int B = b.breite;
        int T = b.tiefe;
        Fassade f = Fassade.wohnhaus(r);
        double verfall = 0.15 + r.nextDouble() * 0.4;
        int stock = r.nextDouble() < 0.55 ? 2 : 1;
        boolean keller = r.nextDouble() < 0.35;
        int hb = 9 + 2 * r.nextInt(3);
        int gb = 6;
        boolean garage = B - hb >= gb + 4 && r.nextDouble() < 0.6;
        while (hb + (garage ? gb : 0) > B - 4 && hb > 7) {
            hb -= 2;
        }
        int ht = 8 + r.nextInt(4);
        int vz = 4 + r.nextInt(3);
        while (vz + ht > T - 4 && ht > 7) {
            ht--;
        }
        if (vz + ht > T - 3) {
            vz = Math.max(2, T - 3 - ht);
        }
        boolean garageLinks = r.nextBoolean();
        int frei = B - hb - (garage ? gb : 0);
        int x1 = (garage && garageLinks ? gb : 0) + frei / 2;
        int x2 = x1 + hb - 1;
        int z1 = vz;
        int z2 = vz + ht - 1;
        int xm = x1 + hb / 2;

        // Grundstück
        b.boden(0, 0, B - 1, T - 1, Blocks.GRASS_BLOCK.defaultBlockState(), 14);

        haus(b, r, f, x1, z1, hb, ht, stock, keller, verfall, info);

        // Garage
        int einfahrtVon = -1;
        if (garage) {
            int gx1 = garageLinks ? x1 - gb : x2 + 1;
            int gx2 = gx1 + gb - 1;
            int gz2 = Math.min(z2, z1 + 7);
            garageBauen(b, f, r, gx1, z1, gx2, gz2, verfall);
            einfahrtVon = gx1 + 1;
            for (int z = 0; z < z1; z++) {
                for (int x = gx1 + 1; x <= gx2 - 1; x++) {
                    b.setze(x, 0, z, b.chance(x, 0, z, 0.3) ? Blocks.COARSE_DIRT : Blocks.GRAVEL);
                    b.setze(x, 1, z, LUFT);
                }
            }
        }

        // Vorgarten: Zaun, Tor, Weg, Briefkasten
        Block zaun = r.nextInt(5) == 0 ? Blocks.COBBLESTONE_WALL : f.zaun();
        if (r.nextInt(6) > 0) {
            int[] tore = garage ? new int[]{xm, einfahrtVon, einfahrtVon + 1, einfahrtVon + 2, einfahrtVon + 3} : new int[]{xm};
            Hausbau.zaunVorne(b, zaun, 0, B - 1, 0, verfall, tore);
            if (r.nextBoolean()) {
                Hausbau.zaunSeite(b, zaun, 0, 1, T - 1, verfall);
                Hausbau.zaunSeite(b, zaun, B - 1, 1, T - 1, verfall);
            }
        }
        Hausbau.weg(b, xm, 0, z1 - 2, 1);
        Hausbau.briefkasten(b, xm + 1, 0);
        Hausbau.verwildern(b, 0, 0, B - 1, T - 1);

        // Hinterhof
        garten(b, r, B, T, z2 + 2);
    }

    /**
     * Das Wohnhaus selbst (ohne Grundstück): Rohbau, Räume, Treppe, Dach, Keller, Einrichtung, Hausnummer.
     * Wird auch vom Bauernhof benutzt.
     */
    static void haus(Baukasten b, Random r, Fassade f, int x1, int z1, int hb, int ht, int stock, boolean keller,
                     double verfall, Info info) {
        int x2 = x1 + hb - 1;
        int z2 = z1 + ht - 1;
        int xm = x1 + hb / 2;
        int xw = xm + 1;
        boolean mitBad = ht >= 9;
        int zb = z1 + ht / 2;
        Block treppe = Hausbau.treppeZu(f.boden());
        Block holzStufe = Hausbau.stufeZu(f.boden());

        // Rohbau, Innenwände, Türen, Fenster
        Hausbau.rohbau(b, f, x1, z1, x2, z2, 0, stock, verfall);
        for (int s = 0; s < stock; s++) {
            int basis = 4 * s;
            Hausbau.innenwandZ(b, f.boden(), xw, z1 + 1, z2 - 1, basis);
            if (s == 0) {
                Hausbau.durchgang(b, xw, z1 + 2, basis);
                if (mitBad) {
                    Hausbau.innenwandX(b, f.boden(), xw + 1, x2 - 1, zb, basis);
                    Hausbau.durchgang(b, xw, zb + 1, basis);
                }
            } else {
                Hausbau.durchgang(b, xw, z2 - 1, basis);
            }
            int[] frei1 = {feld(xm, z1), feld(xm + 1, z1), feld(xm - 1, z1)};
            Hausbau.fenster(b, x1, z1, x2, z2, basis, s == 0, frei1);
        }
        Hausbau.haustuer(b, f, xm, z1, 0, V);
        // Vordach über der Tür
        b.setze(xm - 1, 4, z1 - 1, Hausbau.stufe(f.dachStufe(), false));
        b.setze(xm, 4, z1 - 1, Hausbau.stufe(f.dachStufe(), false));
        b.setze(xm + 1, 4, z1 - 1, Hausbau.stufe(f.dachStufe(), false));
        b.schild(xm + 1, 3, z1 - 1, V, Blocks.SPRUCE_WALL_SIGN, DyeColor.WHITE,
                info.strasse(), "Nr. " + info.hausnummer(), "Fam. " + FAMILIEN[r.nextInt(FAMILIEN.length)]);

        if (stock == 2) {
            Hausbau.innentreppe(b, treppe, f.zaun(), xm, z2 - 4, 0, H);
        }

        // Dach
        if (f == Fassade.BETON) {
            Hausbau.flachdach(b, f, x1, z1, x2, z2, 4 * stock, verfall);
        } else {
            int first = Hausbau.satteldach(b, f, x1, z1, x2, z2, 4 * stock, r.nextDouble() < 0.75, verfall);
            if (r.nextBoolean()) {
                Hausbau.schornstein(b, x1 + 2, z1 + ht / 2, 4 * stock + 1, first);
            }
        }

        // Keller (nach dem Rohbau, sonst würde das Fundament ihn wieder füllen)
        if (keller) {
            kellerBauen(b, f, x1, z1, x2, z2, verfall, r);
        }

        // Einrichtung
        wohnzimmer(b, f, r, x1, z1, xm, z2, keller, stock == 2, holzStufe, treppe);
        kueche(b, f, r, xw, z1, x2, mitBad ? zb - 1 : z2 - 1, treppe);
        if (mitBad) {
            bad(b, xw, zb, x2, z2);
        }
        if (stock == 2) {
            schlafzimmer(b, r, x1, z1, xm, z2);
            kinderzimmer(b, r, xw, z1, x2, z2);
        }
        for (int s = 0; s < stock; s++) {
            Hausbau.spinnweben(b, x1 + 1, z1 + 1, x2 - 1, z2 - 1, 4 * s + 3, 0.08 + verfall * 0.15);
            Hausbau.unordnung(b, x1 + 1, z1 + 1, x2 - 1, z2 - 1, 4 * s + 1, verfall);
        }

    }

    private static void kellerBauen(Baukasten b, Fassade f, int x1, int z1, int x2, int z2, double verfall, Random r) {
        for (int x = x1; x <= x2; x++) {
            for (int z = z1; z <= z2; z++) {
                boolean rand = x == x1 || x == x2 || z == z1 || z == z2;
                b.setze(x, -4, z, Blocks.STONE);
                for (int y = -3; y <= -1; y++) {
                    b.setze(x, y, z, rand ? (b.chance(x, y, z, 0.3) ? Blocks.MOSSY_COBBLESTONE : Blocks.COBBLESTONE)
                            : Blocks.AIR);
                }
            }
        }
        // Leiter hinten links, durch ein Loch im Boden
        b.leiter(x1 + 1, -3, 0, z2 - 1, V);
        // Regale mit Vorräten und Werkzeug
        for (int z = z1 + 1; z < z2; z += 2) {
            b.fass(x2 - 1, -3, z, L, "keller");
            if (r.nextBoolean()) {
                b.fass(x2 - 1, -2, z, L, "keller");
            }
        }
        b.truhe(x1 + 1, -3, z1 + 1, H, "keller");
        b.setze(x1 + 2, -3, z1 + 1, Blocks.FURNACE.defaultBlockState()
                .setValue(net.minecraft.world.level.block.FurnaceBlock.FACING, H)); // alte Heizung
        if (x2 - x1 > 6) {
            b.setze(x1 + 3, -3, z1 + 1, Blocks.CRAFTING_TABLE);
        }
        Hausbau.spinnweben(b, x1 + 1, z1 + 1, x2 - 1, z2 - 1, -1, 0.25);
        Hausbau.unordnung(b, x1 + 1, z1 + 1, x2 - 1, z2 - 1, -3, verfall + 0.2);
    }

    private static void wohnzimmer(Baukasten b, Fassade f, Random r, int x1, int z1, int xm, int z2, boolean keller,
                            boolean treppeDa, Block holzStufe, Block treppe) {
        int links = x1 + 1;
        int rechts = xm - 1;
        int mitte = (z1 + z2) / 2;
        // Sofa an der Vorderwand, Couchtisch davor
        int sofaLaenge = Math.min(3, rechts - links + 1);
        Hausbau.sofa(b, treppe, links, 1, z1 + 1, V, sofaLaenge, R);
        b.setze(links + sofaLaenge / 2, 1, z1 + 3, Hausbau.stufe(holzStufe, false));
        // Kamin oder Bücherregal an der linken Wand
        if (r.nextBoolean()) {
            b.setze(links, 1, mitte, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, false));
            b.setze(links, 1, mitte - 1, Blocks.BRICKS);
            b.setze(links, 1, mitte + 1, Blocks.BRICKS);
            b.setze(links, 2, mitte - 1, Blocks.BRICKS);
            b.setze(links, 2, mitte + 1, Blocks.BRICKS);
            b.setze(links, 3, mitte, Blocks.BRICKS);
            b.setze(links, 2, mitte, Blocks.AIR);
        } else {
            b.setze(links, 1, mitte, Blocks.BOOKSHELF);
            b.setze(links, 2, mitte, Blocks.BOOKSHELF);
            b.setze(links, 1, mitte + 1, Blocks.BOOKSHELF);
        }
        // Schrank mit Beute
        int schrankZ = keller ? z2 - 2 : z2 - 1;
        if (!treppeDa || rechts != xm) {
            b.fass(links, 1, schrankZ, R, "wohnzimmer");
        }
        b.setze(rechts, 1, z2 - 1 - (treppeDa ? 0 : 1), Blocks.POTTED_DEAD_BUSH);
        Hausbau.teppich(b, TEPPICHE[r.nextInt(TEPPICHE.length)], links + 1, z1 + 2, rechts, z2 - 2, 1);
    }

    private static void kueche(Baukasten b, Fassade f, Random r, int xw, int z1, int x2, int zEnde, Block treppe) {
        int wand = x2 - 1;
        int z = z1 + 1;
        if (z <= zEnde) {
            Hausbau.kuehlschrank(b, wand, 1, z++, L);
        }
        if (z <= zEnde) {
            b.setze(wand, 1, z++, Blocks.SMOKER.defaultBlockState()
                    .setValue(net.minecraft.world.level.block.SmokerBlock.FACING, L)); // Herd
        }
        if (z <= zEnde) {
            b.setze(wand, 1, z++, Blocks.CAULDRON); // Spüle
        }
        while (z <= zEnde) {
            b.fass(wand, 1, z, L, "kueche");
            Hausbau.haengeschrank(b, f.fensterladen(), wand, 3, z, L);
            z++;
        }
        // Esstisch, wenn die Küche breit genug ist
        if (x2 - 1 - (xw + 1) >= 2 && zEnde - z1 >= 4) {
            int tx = xw + 1;
            int tz = z1 + 3;
            Hausbau.tisch(b, f.zaun(), Blocks.OAK_PRESSURE_PLATE, tx, 1, tz);
            if (tz + 1 <= zEnde) {
                Hausbau.stuhl(b, treppe, tx, 1, tz + 1, H);
            }
        }
    }

    private static void bad(Baukasten b, int xw, int zb, int x2, int z2) {
        int wand = x2 - 1;
        b.setze(wand, 1, zb + 1, Blocks.CAULDRON); // Waschbecken
        b.fass(wand, 2, zb + 1, L, "badezimmer"); // Spiegelschrank
        b.setze(wand, 1, z2 - 1, Hausbau.treppe(Blocks.QUARTZ_STAIRS, R, false)); // Toilette
        if (wand - 1 > xw && z2 - 1 > zb + 2) {
            b.setze(wand - 1, 1, z2 - 1, Blocks.CAULDRON); // Wanne
        }
    }

    private static void schlafzimmer(Baukasten b, Random r, int x1, int z1, int xm, int z2) {
        int y = 5;
        int links = x1 + 1;
        int mitte = (z1 + z2) / 2 - 1;
        Block bett = BETTEN[r.nextInt(BETTEN.length)];
        if (xm - 1 - links >= 1) {
            b.bett(links, y, mitte, L, bett);
            b.bett(links, y, mitte + 1, L, bett);
            if (mitte + 2 <= z2 - 2) {
                b.fass(links, y, mitte + 2, R, "nachttisch");
            }
        }
        b.fass(links, y, z1 + 1, R, "kleiderschrank");
        b.fass(links, y + 1, z1 + 1, R, "kleiderschrank");
        Hausbau.teppich(b, TEPPICHE[r.nextInt(TEPPICHE.length)], links, z1 + 2, xm - 1, z2 - 2, y);
    }

    private static void kinderzimmer(Baukasten b, Random r, int xw, int z1, int x2, int z2) {
        int y = 5;
        int wand = x2 - 1;
        if (wand - (xw + 1) >= 1) {
            b.bett(wand, y, z1 + 2, R, KINDERBETTEN[r.nextInt(KINDERBETTEN.length)]);
        }
        b.truhe(wand, y, z2 - 2, L, "kinderzimmer");
        Hausbau.teppich(b, r.nextBoolean() ? Blocks.YELLOW_CARPET : Blocks.LIGHT_BLUE_CARPET, xw + 1, z1 + 1, wand, z2 - 1, y);
    }

    private void garageBauen(Baukasten b, Fassade f, Random r, int gx1, int gz1, int gx2, int gz2, double verfall) {
        Hausbau.rohbau(b, f, gx1, gz1, gx2, gz2, 0, 1, verfall);
        Hausbau.flachdach(b, f, gx1, gz1, gx2, gz2, 4, verfall);
        // Tor: vorne offen, oben ein halb heruntergelassenes Rolltor
        for (int x = gx1 + 1; x <= gx2 - 1; x++) {
            b.setze(x, 0, gz1, Blocks.STONE_BRICKS);
            b.setze(x, 1, gz1, LUFT);
            b.setze(x, 2, gz1, LUFT);
            b.setze(x, 3, gz1, r.nextBoolean() ? Hausbau.luke(Blocks.IRON_TRAPDOOR, V, true, false) : LUFT);
        }
        for (int x = gx1 + 1; x <= gx2 - 1; x++) {
            for (int z = gz1 + 1; z <= gz2 - 1; z++) {
                b.setze(x, 0, z, Blocks.SMOOTH_STONE);
            }
        }
        if (r.nextDouble() < 0.5 && gz2 - gz1 >= 6) {
            Baukasten auto = b.teil(gx1 + 1, gz1 + 1, gx1 + 3, gz1 + 5, 1, V, b.saat ^ 0xA770L);
            Fahrzeuge.auto(auto, b.saat ^ 0xA771L, false);
        }
        b.truhe(gx2 - 1, 1, gz2 - 1, V, "werkzeug");
        b.fass(gx2 - 1, 1, gz2 - 2, L, "werkzeug");
        Hausbau.spinnweben(b, gx1 + 1, gz1 + 1, gx2 - 1, gz2 - 1, 3, 0.2);
    }

    private void garten(Baukasten b, Random r, int B, int T, int zStart) {
        int platz = T - 2 - zStart;
        if (platz < 3) {
            return;
        }
        int wahl = r.nextInt(5);
        switch (wahl) {
            case 0 -> schuppen(b, r, 1, zStart, Math.min(B - 2, 4), Math.min(T - 2, zStart + 3));
            case 1 -> schaukel(b, B / 2, zStart + 1);
            case 2 -> Hausbau.toterBaum(b, B / 3, zStart + platz / 2, r);
            case 3 -> {
                // Gartengrill und Gartenstühle
                b.setze(B / 2, 1, zStart + 1, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, false));
                b.setze(B / 2 - 2, 1, zStart + 1, Hausbau.treppe(Blocks.SPRUCE_STAIRS, L, false));
                b.setze(B / 2 + 2, 1, zStart + 1, Hausbau.treppe(Blocks.SPRUCE_STAIRS, R, false));
            }
            default -> {
                // Hundehütte mit leerem Napf
                int x = B - 4;
                b.fuelle(x, 1, zStart, x + 1, 1, zStart + 1, Blocks.SPRUCE_PLANKS);
                b.setze(x, 1, zStart, LUFT);
                b.setze(x, 2, zStart, Hausbau.stufe(Blocks.SPRUCE_SLAB, false));
                b.setze(x + 1, 2, zStart, Hausbau.stufe(Blocks.SPRUCE_SLAB, false));
                b.setze(x, 2, zStart + 1, Hausbau.stufe(Blocks.SPRUCE_SLAB, false));
                b.setze(x + 1, 2, zStart + 1, Hausbau.stufe(Blocks.SPRUCE_SLAB, false));
                b.setze(x - 1, 1, zStart, Blocks.BONE_BLOCK);
            }
        }
    }

    static void schuppen(Baukasten b, Random r, int x1, int z1, int x2, int z2) {
        if (x2 - x1 < 2 || z2 - z1 < 2) {
            return;
        }
        for (int x = x1; x <= x2; x++) {
            for (int z = z1; z <= z2; z++) {
                boolean rand = x == x1 || x == x2 || z == z1 || z == z2;
                b.setze(x, 0, z, Blocks.SPRUCE_PLANKS);
                for (int y = 1; y <= 2; y++) {
                    b.setze(x, y, z, rand ? Blocks.SPRUCE_PLANKS : Blocks.AIR);
                }
                b.setze(x, 3, z, Hausbau.stufe(Blocks.SPRUCE_SLAB, false));
            }
        }
        b.tuer((x1 + x2) / 2, 1, z1, V, Blocks.SPRUCE_DOOR, r.nextBoolean());
        b.truhe(x1 + 1, 1, z2 - 1, V, "schuppen");
        b.setze(x2 - 1, 1, z2 - 1, Blocks.COBWEB);
    }

    static void schaukel(Baukasten b, int x, int z) {
        b.setze(x - 1, 1, z, Blocks.SPRUCE_FENCE);
        b.setze(x - 1, 2, z, Blocks.SPRUCE_FENCE);
        b.setze(x + 1, 1, z, Blocks.SPRUCE_FENCE);
        b.setze(x + 1, 2, z, Blocks.SPRUCE_FENCE);
        b.setze(x - 1, 3, z, b.zaun(Blocks.SPRUCE_FENCE, true));
        b.setze(x, 3, z, b.zaun(Blocks.SPRUCE_FENCE, true));
        b.setze(x + 1, 3, z, b.zaun(Blocks.SPRUCE_FENCE, true));
        b.setze(x, 2, z, Blocks.CHAIN);
        b.setze(x, 1, z, Hausbau.stufe(Blocks.SPRUCE_SLAB, true));
    }
}
