package de.deadzonemc.welt.gebaeude;

import de.deadzonemc.welt.bau.Baukasten;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.Random;

import static de.deadzonemc.welt.bau.Baukasten.H;
import static de.deadzonemc.welt.bau.Baukasten.L;
import static de.deadzonemc.welt.bau.Baukasten.R;
import static de.deadzonemc.welt.bau.Baukasten.V;
import static de.deadzonemc.welt.gebaeude.Hausbau.LUFT;

/**
 * Kleiner Laden: Schaufenster mit Markise, Regalreihen mit Gängen, Kasse am Eingang und ein Lager
 * hinten. Darüber manchmal eine Wohnung. Die Regale sind teils schon geplündert.
 */
public class Dorfladen implements Gebaeude {

    private static final String[] NAMEN = {"Dorfladen", "Tante-Emma-Laden", "Kaufmann", "Lebensmittel", "Kiosk"};

    @Override
    public int hoehe() {
        return 18;
    }

    @Override
    public void bauen(Baukasten b, Info info) {
        Random r = new Random(b.saat);
        int B = b.breite;
        int T = b.tiefe;
        double verfall = 0.2 + r.nextDouble() * 0.35;
        Fassade f = r.nextBoolean() ? Fassade.PUTZ_WEISS : Fassade.ZIEGEL;
        b.boden(0, 0, B - 1, T - 1, Blocks.GRASS_BLOCK.defaultBlockState(), 16);

        int x1 = 1;
        int x2 = B - 2;
        int z1 = 3;
        int z2 = Math.min(T - 3, z1 + 12);
        int xm = (x1 + x2) / 2;
        boolean wohnung = r.nextBoolean();
        int stock = wohnung ? 2 : 1;
        Hausbau.rohbau(b, f, x1, z1, x2, z2, 0, stock, verfall);
        if (wohnung) {
            Hausbau.satteldach(b, f, x1, z1, x2, z2, 8, true, verfall);
            Hausbau.fenster(b, x1, z1, x2, z2, 4, false);
            // Wohnung oben: Bett, Schrank, Tisch (Zugang über eine Leiter hinten)
            b.leiter(x2 - 1, 1, 4, z2 - 1, V);
            b.bett(x1 + 1, 5, z1 + 2, L, Blocks.GRAY_BED);
            b.fass(x1 + 1, 5, z1 + 1, R, "kleiderschrank");
            b.fass(x1 + 1, 5, z2 - 1, R, "wohnzimmer");
            Hausbau.tisch(b, f.zaun(), Blocks.OAK_PRESSURE_PLATE, xm, 5, (z1 + z2) / 2);
        } else {
            Hausbau.flachdach(b, f, x1, z1, x2, z2, 4, verfall);
        }

        // Schaufenster über die ganze Front, Tür in der Mitte
        for (int x = x1 + 1; x < x2; x++) {
            if (Math.abs(x - xm) <= 0) {
                continue;
            }
            for (int y = 1; y <= 3; y++) {
                b.setze(x, y, z1, b.chance(x, y, z1, 0.5) ? LUFT : b.scheibe(Blocks.GLASS_PANE, true));
            }
        }
        Hausbau.haustuer(b, f, xm, z1, 0, V);
        // Markise
        for (int x = x1; x <= x2; x++) {
            if (!b.chance(x, 4, z1 - 1, verfall * 0.5)) {
                b.setze(x, 4, z1 - 1, (x - x1) % 2 == 0 ? Blocks.RED_WOOL : Blocks.WHITE_WOOL);
            }
        }
        b.schild(xm, 3, z1 - 1, V, Blocks.OAK_WALL_SIGN, DyeColor.BLACK, "", NAMEN[r.nextInt(NAMEN.length)],
                "Inh. " + Einfamilienhaus.FAMILIEN[r.nextInt(Einfamilienhaus.FAMILIEN.length)], "");

        // Kasse links neben dem Eingang
        b.setze(x1 + 1, 1, z1 + 2, Hausbau.treppe(Blocks.SPRUCE_STAIRS, R, true));
        b.setze(x1 + 2, 1, z1 + 2, Hausbau.treppe(Blocks.SPRUCE_STAIRS, V, true));
        b.setze(x1 + 2, 2, z1 + 2, Blocks.LECTERN);
        b.truhe(x1 + 1, 1, z1 + 3, R, "laden_lager");

        // Regalreihen (Gänge längs), hinten das Lager
        int lagerZ = z2 - 4;
        for (int x = x1 + 2; x <= x2 - 2; x += 3) {
            if (Math.abs(x - xm) <= 1) {
                continue;
            }
            for (int z = z1 + 5; z < lagerZ; z++) {
                regal(b, x, z, r);
            }
        }
        // Kühlregal an der rechten Wand
        for (int z = z1 + 2; z < lagerZ; z += 2) {
            b.truhe(x2 - 1, 1, z, L, "laden_kuehlregal");
            b.setze(x2 - 1, 2, z, b.scheibe(Blocks.GLASS_PANE, false));
        }
        Hausbau.innenwandX(b, f.boden(), x1 + 1, x2 - 1, lagerZ, 0);
        b.tuer(xm, 1, lagerZ, H, Blocks.SPRUCE_DOOR, true);
        for (int x = x1 + 1; x < x2; x += 2) {
            b.truhe(x, 1, z2 - 1, V, "laden_lager");
        }
        b.fass(x1 + 1, 1, z2 - 2, R, "laden_lager");
        b.fass(x1 + 1, 2, z2 - 2, R, "laden_lager");

        Hausbau.spinnweben(b, x1 + 1, z1 + 1, x2 - 1, z2 - 1, 3, 0.1);
        Hausbau.unordnung(b, x1 + 1, z1 + 1, x2 - 1, z2 - 1, 1, verfall + 0.2);
        // Einkaufswagen-Ersatz: umgekippte Kisten vor dem Laden
        Hausbau.verwildern(b, 0, 0, B - 1, T - 1);
        for (int x = 0; x < B; x++) {
            for (int z = 0; z < z1; z++) {
                b.setze(x, 0, z, b.chance(x, 0, z, 0.15) ? Blocks.CRACKED_STONE_BRICKS : Blocks.STONE_BRICKS);
                b.setze(x, 1, z, LUFT);
            }
        }
    }

    /** Ein Regalfach: Fass mit Waren unten, Ablage oben – manchmal leer geräumt oder umgestürzt. */
    static void regal(Baukasten b, int x, int z, Random r) {
        double w = b.zufall(x, 1, z);
        if (w < 0.15) {
            b.setze(x, 1, z, Blocks.SPRUCE_PLANKS); // leer
        } else if (w < 0.25) {
            return; // umgestürzt, weg
        } else {
            b.fass(x, 1, z, w < 0.6 ? L : R, "laden_regal");
        }
        b.setze(x, 2, z, Hausbau.stufe(Blocks.SPRUCE_SLAB, false));
    }
}
