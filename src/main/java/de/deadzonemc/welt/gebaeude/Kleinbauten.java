package de.deadzonemc.welt.gebaeude;

import de.deadzonemc.welt.bau.Baukasten;
import de.deadzonemc.welt.bau.Fahrzeuge;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Blocks;

import java.util.Random;

import static de.deadzonemc.welt.bau.Baukasten.H;
import static de.deadzonemc.welt.bau.Baukasten.L;
import static de.deadzonemc.welt.bau.Baukasten.R;
import static de.deadzonemc.welt.bau.Baukasten.V;
import static de.deadzonemc.welt.gebaeude.Hausbau.LUFT;

/** Kleinere Grundstücke: Brandruine, Kleingarten, Bushaltestelle und Tankstelle. */
public final class Kleinbauten {

    private Kleinbauten() {
    }

    /** Ein abgebranntes Haus: Mauerreste, verkohlte Balken, Asche. In den Trümmern liegt noch etwas. */
    public static class Ruine implements Gebaeude {
        @Override
        public int hoehe() {
            return 8;
        }

        @Override
        public void bauen(Baukasten b, Info info) {
            Random r = new Random(b.saat);
            int B = b.breite;
            int T = b.tiefe;
            b.boden(0, 0, B - 1, T - 1, Blocks.GRASS_BLOCK.defaultBlockState(), 10);
            int x1 = 2;
            int x2 = B - 3;
            int z1 = 3;
            int z2 = Math.min(T - 3, z1 + 9);
            b.fundament(x1, z1, x2, z2, Blocks.COBBLESTONE.defaultBlockState());
            for (int x = x1; x <= x2; x++) {
                for (int z = z1; z <= z2; z++) {
                    boolean rand = x == x1 || x == x2 || z == z1 || z == z2;
                    double w = b.zufall(x, 0, z);
                    b.setze(x, 0, z, w < 0.3 ? Blocks.COAL_BLOCK : w < 0.6 ? Blocks.BLACK_CONCRETE_POWDER : Blocks.COBBLESTONE);
                    if (rand) {
                        int hoehe = (int) (b.zufall(x, 1, z) * 4);
                        for (int y = 1; y <= hoehe; y++) {
                            b.setze(x, y, z, b.chance(x, y, z, 0.5) ? Blocks.BLACKSTONE : Blocks.CRACKED_STONE_BRICKS);
                        }
                    } else if (w > 0.85) {
                        b.setze(x, 1, z, Blocks.GRAVEL);
                    } else if (w > 0.8) {
                        b.setze(x, 1, z, Blocks.COAL_BLOCK);
                    }
                }
            }
            // Verkohlte Dachbalken quer über den Trümmern
            for (int i = 0; i < 3; i++) {
                int z = z1 + 2 + r.nextInt(Math.max(1, z2 - z1 - 3));
                for (int x = x1 + 1; x < x2; x++) {
                    if (b.chance(x, 2, z, 0.7)) {
                        b.setze(x, 1 + (x % 3 == 0 ? 1 : 0), z, Hausbau.balken(Blocks.BASALT, net.minecraft.core.Direction.Axis.X));
                    }
                }
            }
            b.truhe(x1 + 2, 1, z2 - 2, V, "ruine");
            b.setze(x1 + 3, 1, z2 - 2, Blocks.GRAVEL);
            Hausbau.verwildern(b, 0, 0, B - 1, T - 1);
            Hausbau.toterBaum(b, B - 2, T - 2, r);
        }
    }

    /** Kleingarten: Laube, Beete voller Unkraut, Komposter, Regentonne. */
    public static class Kleingarten implements Gebaeude {
        @Override
        public int hoehe() {
            return 7;
        }

        @Override
        public void bauen(Baukasten b, Info info) {
            Random r = new Random(b.saat);
            int B = b.breite;
            int T = b.tiefe;
            b.boden(0, 0, B - 1, T - 1, Blocks.GRASS_BLOCK.defaultBlockState(), 8);
            Hausbau.zaunVorne(b, Blocks.OAK_FENCE, 0, B - 1, 0, 0.3, B / 2);
            Hausbau.zaunSeite(b, Blocks.OAK_FENCE, 0, 1, T - 1, 0.3);
            Hausbau.zaunSeite(b, Blocks.OAK_FENCE, B - 1, 1, T - 1, 0.3);
            Hausbau.tor(b, Blocks.OAK_FENCE_GATE, B / 2, 0, true);
            // Beete
            for (int x = 2; x < B - 2; x++) {
                for (int z = 2; z < T / 2; z++) {
                    if ((x % 3) == 1) {
                        continue;
                    }
                    b.setze(x, 0, z, Blocks.COARSE_DIRT);
                    double w = b.zufall(x, 1, z);
                    b.setze(x, 1, z, w < 0.3 ? Blocks.DEAD_BUSH : w < 0.5 ? Blocks.SHORT_GRASS
                            : w < 0.55 ? Blocks.SWEET_BERRY_BUSH : Blocks.AIR);
                }
            }
            // Laube
            int lx1 = 2;
            int lz1 = T / 2 + 2;
            int lx2 = Math.min(B - 3, lx1 + 4);
            int lz2 = Math.min(T - 2, lz1 + 3);
            Einfamilienhaus.schuppen(b, r, lx1, lz1, lx2, lz2);
            b.setze(lx2 + 2, 1, lz1, Blocks.COMPOSTER);
            b.setze(lx2 + 2, 1, lz1 + 2, Blocks.CAULDRON);
            b.setze(lx2 + 3, 1, lz1 + 1, Hausbau.treppe(Blocks.OAK_STAIRS, R, false));
            Hausbau.verwildern(b, 0, 0, B - 1, T - 1);
        }
    }

    /** Bushaltestelle mit Wartehäuschen, Bank, Mülleimer und Fahrplan. */
    public static class Bushaltestelle implements Gebaeude {
        @Override
        public int hoehe() {
            return 5;
        }

        @Override
        public void bauen(Baukasten b, Info info) {
            Random r = new Random(b.saat);
            int B = b.breite;
            b.boden(0, 0, B - 1, b.tiefe - 1, Blocks.SMOOTH_STONE.defaultBlockState(), 5);
            for (int x = 0; x <= 4; x++) {
                for (int y = 1; y <= 2; y++) {
                    b.setze(x, y, 2, b.chance(x, y, 2, 0.4) ? LUFT : b.scheibe(Blocks.GLASS_PANE, true));
                }
                b.setze(x, 3, 0, Hausbau.stufe(Blocks.SMOOTH_STONE_SLAB, false));
                b.setze(x, 3, 1, Hausbau.stufe(Blocks.SMOOTH_STONE_SLAB, false));
                b.setze(x, 3, 2, Hausbau.stufe(Blocks.SMOOTH_STONE_SLAB, false));
            }
            for (int y = 1; y <= 2; y++) {
                b.setze(0, y, 0, Blocks.IRON_BARS);
                b.setze(0, y, 1, b.scheibe(Blocks.GLASS_PANE, false));
                b.setze(4, y, 0, Blocks.IRON_BARS);
                b.setze(4, y, 1, b.scheibe(Blocks.GLASS_PANE, false));
            }
            for (int x = 1; x <= 3; x++) {
                b.setze(x, 1, 1, Hausbau.treppe(Blocks.SPRUCE_STAIRS, H, false));
            }
            b.setze(5, 1, 1, Blocks.CAULDRON);
            b.setze(5, 1, 0, Blocks.IRON_BARS);
            b.setze(5, 2, 0, Blocks.IRON_BARS);
            b.setze(5, 3, 0, Blocks.YELLOW_CONCRETE);
            b.schild(5, 3, -1, V, Blocks.BAMBOO_WALL_SIGN, DyeColor.GREEN, "H", "Linie " + (3 + r.nextInt(30)),
                    info.strasse(), "");
            b.schild(2, 2, 3, H, Blocks.OAK_WALL_SIGN, DyeColor.BLACK, "Fahrplan", "Kein Verkehr", "bis auf", "Weiteres");
        }
    }

    /** Tankstelle: Dach auf Säulen, zwei Zapfinseln, kleiner Shop, Preisschild an der Straße. */
    public static class Tankstelle implements Gebaeude {
        @Override
        public int hoehe() {
            return 10;
        }

        @Override
        public void bauen(Baukasten b, Info info) {
            Random r = new Random(b.saat);
            int B = b.breite;
            int T = b.tiefe;
            double verfall = 0.2 + r.nextDouble() * 0.35;
            b.boden(0, 0, B - 1, T - 1, Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState(), 10);
            for (int x = 0; x < B; x++) {
                for (int z = 0; z < T; z++) {
                    double w = b.zufall(x, 0, z);
                    if (w < 0.05) {
                        b.setze(x, 0, z, Blocks.BLACK_CONCRETE_POWDER); // Ölflecken
                    } else if (w < 0.1) {
                        b.setze(x, 0, z, Blocks.CRACKED_STONE_BRICKS);
                    }
                }
            }
            // Dach über den Zapfsäulen
            int dx1 = 3;
            int dx2 = B - 4;
            int dz1 = 2;
            int dz2 = 10;
            for (int[] p : new int[][]{{dx1, dz1}, {dx2, dz1}, {dx1, dz2}, {dx2, dz2}}) {
                for (int y = 1; y <= 4; y++) {
                    b.setze(p[0], y, p[1], Blocks.QUARTZ_PILLAR);
                }
            }
            for (int x = dx1 - 1; x <= dx2 + 1; x++) {
                for (int z = dz1 - 1; z <= dz2 + 1; z++) {
                    boolean rand = x == dx1 - 1 || x == dx2 + 1 || z == dz1 - 1 || z == dz2 + 1;
                    if (b.chance(x, 5, z, verfall * 0.25)) {
                        continue;
                    }
                    b.setze(x, 5, z, rand ? (x % 2 == 0 ? Blocks.RED_CONCRETE : Blocks.WHITE_CONCRETE)
                            : Blocks.SMOOTH_QUARTZ);
                }
            }
            // Zwei Zapfinseln
            for (int ix : new int[]{B / 2 - 4, B / 2 + 3}) {
                for (int z = 5; z <= 7; z++) {
                    b.setze(ix, 1, z, Hausbau.stufe(Blocks.SMOOTH_STONE_SLAB, false));
                }
                b.setze(ix, 1, 6, Blocks.SMOOTH_QUARTZ);
                b.setze(ix, 2, 6, Blocks.WHITE_CONCRETE);
                b.setze(ix, 3, 6, Blocks.REDSTONE_LAMP);
                b.setze(ix, 2, 5, Blocks.TRIPWIRE_HOOK.defaultBlockState()
                        .setValue(net.minecraft.world.level.block.TripWireHookBlock.FACING, V));
            }
            if (r.nextBoolean()) {
                Fahrzeuge.auto(b.teil(B / 2 - 2, 4, B / 2, 8, 1, r.nextBoolean() ? V : H, b.saat ^ 3), b.saat ^ 4,
                        r.nextInt(3) == 0);
            }
            // Shop hinten
            Fassade f = Fassade.BETON;
            int sx1 = 3;
            int sx2 = Math.min(B - 4, sx1 + 12);
            int sz1 = 13;
            int sz2 = Math.min(T - 2, sz1 + 8);
            int sm = (sx1 + sx2) / 2;
            Hausbau.rohbau(b, f, sx1, sz1, sx2, sz2, 0, 1, verfall);
            Hausbau.flachdach(b, f, sx1, sz1, sx2, sz2, 4, verfall);
            for (int x = sx1 + 1; x < sx2; x++) {
                if (x == sm) {
                    continue;
                }
                for (int y = 1; y <= 3; y++) {
                    b.setze(x, y, sz1, b.chance(x, y, sz1, 0.5) ? LUFT : b.scheibe(Blocks.GLASS_PANE, true));
                }
            }
            Hausbau.haustuer(b, f, sm, sz1, 0, V);
            b.schild(sm, 4, sz1 - 1, V, Blocks.BIRCH_WALL_SIGN, DyeColor.RED, "TANKSTELLE", "24 h geöffnet", "", "");
            // Theke, Regale, Kühlschrank
            b.setze(sx1 + 1, 1, sz1 + 2, Hausbau.treppe(Blocks.BIRCH_STAIRS, R, true));
            b.setze(sx1 + 2, 1, sz1 + 2, Hausbau.treppe(Blocks.BIRCH_STAIRS, V, true));
            b.setze(sx1 + 2, 2, sz1 + 2, Blocks.LECTERN);
            b.truhe(sx1 + 1, 1, sz1 + 3, R, "tankstelle");
            for (int x = sm + 2; x < sx2; x += 2) {
                for (int z = sz1 + 2; z < sz2 - 1; z++) {
                    Dorfladen.regal(b, x, z, r);
                }
            }
            Hausbau.kuehlschrank(b, sx1 + 1, 1, sz2 - 1, R);
            b.truhe(sx1 + 2, 1, sz2 - 1, V, "laden_kuehlregal");
            b.truhe(sx2 - 1, 1, sz2 - 1, V, "tankstelle");
            Hausbau.unordnung(b, sx1 + 1, sz1 + 1, sx2 - 1, sz2 - 1, 1, verfall + 0.2);
            Hausbau.spinnweben(b, sx1 + 1, sz1 + 1, sx2 - 1, sz2 - 1, 3, 0.1);
            // Preisschild an der Straße: die Preise vom letzten Tag
            int px = B - 2;
            for (int y = 1; y <= 4; y++) {
                b.setze(px, y, 1, Blocks.ANDESITE_WALL);
            }
            b.setze(px, 5, 1, Blocks.WHITE_CONCRETE);
            b.setze(px, 6, 1, Blocks.WHITE_CONCRETE);
            b.schild(px, 5, 0, V, Blocks.BIRCH_WALL_SIGN, DyeColor.BLACK, "Super", "1,89", "", "");
            b.schild(px, 6, 0, V, Blocks.BIRCH_WALL_SIGN, DyeColor.BLACK, "Diesel", "1,79", "", "");
        }
    }

    /** Leeres, verwildertes Grundstück mit etwas Schutt (falls ein Typ unbekannt ist). */
    public static class Brache implements Gebaeude {
        @Override
        public int hoehe() {
            return 6;
        }

        @Override
        public void bauen(Baukasten b, Info info) {
            b.boden(0, 0, b.breite - 1, b.tiefe - 1, Blocks.GRASS_BLOCK.defaultBlockState(), 6);
            Hausbau.verwildern(b, 0, 0, b.breite - 1, b.tiefe - 1);
        }
    }
}
