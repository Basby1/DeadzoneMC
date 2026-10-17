package de.deadzonemc.welt.gebaeude;

import de.deadzonemc.welt.bau.Baukasten;
import de.deadzonemc.welt.bau.Fahrzeuge;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Blocks;

import java.util.Random;

import static de.deadzonemc.welt.bau.Baukasten.L;
import static de.deadzonemc.welt.bau.Baukasten.R;
import static de.deadzonemc.welt.bau.Baukasten.V;
import static de.deadzonemc.welt.gebaeude.Hausbau.LUFT;

/** Autowerkstatt: Halle mit Hebebühne, Werkbänken, Reifenstapeln und Werkzeugkisten. */
public class Werkstatt implements Gebaeude {

    @Override
    public int hoehe() {
        return 12;
    }

    @Override
    public void bauen(Baukasten b, Info info) {
        Random r = new Random(b.saat);
        int B = b.breite;
        int T = b.tiefe;
        double verfall = 0.2 + r.nextDouble() * 0.35;
        Fassade f = r.nextBoolean() ? Fassade.BETON : Fassade.ZIEGEL;
        b.boden(0, 0, B - 1, T - 1, Blocks.GRASS_BLOCK.defaultBlockState(), 12);
        int x1 = 1;
        int x2 = B - 2;
        int z1 = 5;
        int z2 = Math.min(T - 2, z1 + 12);
        Hausbau.rohbau(b, f, x1, z1, x2, z2, 0, 1, verfall);
        for (int x = x1 + 1; x < x2; x++) {
            for (int z = z1 + 1; z < z2; z++) {
                b.setze(x, 0, z, b.chance(x, 0, z, 0.08) ? Blocks.BLACK_CONCRETE_POWDER : Blocks.SMOOTH_STONE);
            }
        }
        Hausbau.flachdach(b, f, x1, z1, x2, z2, 4, verfall);
        Hausbau.fenster(b, x1, z1, x2, z2, 0, false);
        // Großes Rolltor
        int mitte = (x1 + x2) / 2;
        for (int x = mitte - 2; x <= mitte + 2; x++) {
            b.setze(x, 1, z1, LUFT);
            b.setze(x, 2, z1, LUFT);
            b.setze(x, 3, z1, Hausbau.luke(Blocks.IRON_TRAPDOOR, V, true, false));
        }
        b.schild(mitte, 4, z1 - 1, V, Blocks.SPRUCE_WALL_SIGN, DyeColor.WHITE, "Kfz-Werkstatt",
                Einfamilienhaus.FAMILIEN[r.nextInt(Einfamilienhaus.FAMILIEN.length)], "Reparatur aller", "Marken");
        // Hebebühne mit Auto
        int hx = mitte - 1;
        for (int z : new int[]{z1 + 3, z1 + 7}) {
            b.setze(hx - 1, 1, z, Blocks.IRON_BLOCK);
            b.setze(hx + 3, 1, z, Blocks.IRON_BLOCK);
        }
        Fahrzeuge.auto(b.teil(hx, z1 + 3, hx + 2, z1 + 7, 1, V, b.saat ^ 7), b.saat ^ 8, r.nextInt(4) == 0);
        // Werkbänke und Werkzeug an den Wänden
        b.setze(x1 + 1, 1, z2 - 1, Blocks.CRAFTING_TABLE);
        b.setze(x1 + 2, 1, z2 - 1, Blocks.CHIPPED_ANVIL);
        b.setze(x1 + 3, 1, z2 - 1, Blocks.GRINDSTONE);
        b.truhe(x2 - 1, 1, z2 - 1, V, "werkstatt");
        b.truhe(x2 - 1, 1, z2 - 2, L, "werkstatt");
        b.fass(x1 + 1, 1, z1 + 2, R, "werkstatt");
        // Reifenstapel
        for (int y = 1; y <= 3; y++) {
            b.setze(x2 - 1, y, z1 + 1, Blocks.BLACK_CONCRETE);
        }
        b.setze(x2 - 2, 1, z1 + 1, Blocks.BLACK_CONCRETE);
        // Ölfässer vor der Halle
        b.fass(x2 - 1, 1, z1 - 2, V, "werkstatt");
        b.setze(x2 - 2, 1, z1 - 2, Blocks.BARREL);
        for (int x = x1; x <= x2; x++) {
            for (int z = 0; z < z1; z++) {
                b.setze(x, 0, z, b.chance(x, 0, z, 0.15) ? Blocks.COARSE_DIRT : Blocks.GRAVEL);
                if (!(x >= x2 - 2 && z == z1 - 2)) {
                    b.setze(x, 1, z, LUFT);
                }
            }
        }
        Hausbau.spinnweben(b, x1 + 1, z1 + 1, x2 - 1, z2 - 1, 3, 0.12);
        Hausbau.unordnung(b, x1 + 1, z1 + 1, x2 - 1, z2 - 1, 1, verfall);
        Hausbau.verwildern(b, 0, 0, B - 1, T - 1);
    }
}
