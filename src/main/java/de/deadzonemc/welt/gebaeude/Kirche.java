package de.deadzonemc.welt.gebaeude;

import de.deadzonemc.welt.bau.Baukasten;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.state.properties.BellAttachType;

import java.util.Random;

import static de.deadzonemc.welt.bau.Baukasten.H;
import static de.deadzonemc.welt.bau.Baukasten.L;
import static de.deadzonemc.welt.bau.Baukasten.R;
import static de.deadzonemc.welt.bau.Baukasten.V;
import static de.deadzonemc.welt.gebaeude.Hausbau.LUFT;

/**
 * Dorfkirche aus Stein mit Glockenturm und Spitzhelm, Bankreihen, Altar und bunten Fenstern.
 * Rundherum der Friedhof – einige Gräber stehen offen.
 */
public class Kirche implements Gebaeude {

    private static final Block[] BUNTGLAS = {Blocks.RED_STAINED_GLASS_PANE, Blocks.BLUE_STAINED_GLASS_PANE,
            Blocks.YELLOW_STAINED_GLASS_PANE, Blocks.PURPLE_STAINED_GLASS_PANE};

    @Override
    public int hoehe() {
        return 34;
    }

    @Override
    public void bauen(Baukasten b, Info info) {
        Random r = new Random(b.saat);
        int B = b.breite;
        int T = b.tiefe;
        double verfall = 0.2 + r.nextDouble() * 0.35;
        Fassade f = Fassade.STEIN;
        b.boden(0, 0, B - 1, T - 1, Blocks.GRASS_BLOCK.defaultBlockState(), 20);

        int nb = 11;
        int x1 = (B - nb) / 2;
        int x2 = x1 + nb - 1;
        int xm = (x1 + x2) / 2;
        int z1 = 9;
        int z2 = Math.min(T - 6, z1 + 22);
        int wandHoehe = 7;

        // Kirchenschiff
        b.fundament(x1, z1, x2, z2, Blocks.COBBLESTONE.defaultBlockState());
        for (int x = x1; x <= x2; x++) {
            for (int z = z1; z <= z2; z++) {
                boolean rand = x == x1 || x == x2 || z == z1 || z == z2;
                b.setze(x, 0, z, rand ? Blocks.STONE_BRICKS : (b.chance(x, 0, z, 0.2) ? Blocks.CRACKED_STONE_BRICKS : Blocks.POLISHED_ANDESITE));
                for (int y = 1; y <= wandHoehe; y++) {
                    b.setze(x, y, z, rand ? Hausbau.wandBlock(b, f, x, y, z, y, wandHoehe, verfall)
                            : LUFT);
                }
            }
        }
        // Stützpfeiler außen
        for (int z = z1; z <= z2; z += 4) {
            for (int y = 1; y <= 4; y++) {
                b.setze(x1 - 1, y, z, Blocks.STONE_BRICKS);
                b.setze(x2 + 1, y, z, Blocks.STONE_BRICKS);
            }
            b.setze(x1 - 1, 5, z, Hausbau.treppe(Blocks.STONE_BRICK_STAIRS, R, false));
            b.setze(x2 + 1, 5, z, Hausbau.treppe(Blocks.STONE_BRICK_STAIRS, L, false));
        }
        // Hohe Buntglasfenster
        for (int z = z1 + 2; z < z2 - 1; z += 4) {
            for (int y = 2; y <= 5; y++) {
                for (int x : new int[]{x1, x2}) {
                    Block glas = BUNTGLAS[(int) (b.zufall(x, 0, z) * BUNTGLAS.length)];
                    b.setze(x, y, z, b.chance(x, y, z, 0.35) ? LUFT : b.scheibe(glas, false));
                }
            }
        }
        // Rundfenster hinten
        b.setze(xm, 5, z2, b.scheibe(Blocks.RED_STAINED_GLASS_PANE, true));
        b.setze(xm, 6, z2, b.scheibe(Blocks.YELLOW_STAINED_GLASS_PANE, true));
        Hausbau.satteldach(b, f, x1, z1, x2, z2, wandHoehe + 1, false, verfall * 0.6);

        // Bankreihen mit Mittelgang, Blick zum Altar (hinten)
        for (int z = z1 + 3; z <= z2 - 7; z += 2) {
            for (int x = x1 + 1; x <= x2 - 1; x++) {
                if (Math.abs(x - xm) <= 1) {
                    continue;
                }
                if (!b.chance(x, 1, z, verfall * 0.3)) {
                    b.setze(x, 1, z, Hausbau.treppe(Blocks.SPRUCE_STAIRS, V, false));
                }
            }
        }
        // Altarraum: Stufe, Altar, Kerzen, Kreuz
        for (int x = x1 + 1; x <= x2 - 1; x++) {
            for (int z = z2 - 4; z <= z2 - 1; z++) {
                b.setze(x, 1, z, z == z2 - 4 ? Hausbau.treppe(Blocks.STONE_BRICK_STAIRS, H, false)
                        : Blocks.SMOOTH_STONE.defaultBlockState());
            }
        }
        b.setze(xm - 1, 2, z2 - 2, Blocks.QUARTZ_BLOCK);
        b.setze(xm, 2, z2 - 2, Blocks.QUARTZ_BLOCK);
        b.setze(xm + 1, 2, z2 - 2, Blocks.QUARTZ_BLOCK);
        b.setze(xm - 1, 3, z2 - 2, Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.CANDLES, 3));
        b.setze(xm + 1, 3, z2 - 2, Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.CANDLES, 2));
        b.setze(xm, 2, z2 - 3, Blocks.LECTERN);
        for (int y = 2; y <= 6; y++) {
            b.setze(xm, y, z2 - 1, Blocks.DARK_OAK_LOG);
        }
        b.setze(xm - 1, 5, z2 - 1, Hausbau.balken(Blocks.DARK_OAK_LOG, net.minecraft.core.Direction.Axis.X));
        b.setze(xm + 1, 5, z2 - 1, Hausbau.balken(Blocks.DARK_OAK_LOG, net.minecraft.core.Direction.Axis.X));
        // Sakristei-Truhe und Opferstock
        b.fass(x2 - 1, 2, z2 - 1, L, "kirche");
        b.truhe(x1 + 1, 2, z2 - 1, R, "kirche");
        Hausbau.spinnweben(b, x1 + 1, z1 + 1, x2 - 1, z2 - 1, wandHoehe, 0.2);
        Hausbau.unordnung(b, x1 + 1, z1 + 1, x2 - 1, z2 - 5, 1, verfall + 0.2);

        // Glockenturm vorne
        turm(b, r, xm - 2, z1 - 5, xm + 2, z1, verfall);

        // Friedhof rundherum
        friedhof(b, r, B, T, x1, x2, z1, z2);
        b.schild(xm + 2, 3, z1 - 6, V, Blocks.DARK_OAK_WALL_SIGN, DyeColor.WHITE, "Kirche", "St. " + heiliger(r),
                "Gottesdienst", "So. 10 Uhr");
    }

    private static String heiliger(Random r) {
        String[] namen = {"Marien", "Michael", "Martin", "Johannes", "Petrus", "Nikolaus", "Laurentius"};
        return namen[r.nextInt(namen.length)];
    }

    private void turm(Baukasten b, Random r, int x1, int z1, int x2, int z2, double verfall) {
        int hoehe = 16;
        b.fundament(x1, z1, x2, z2, Blocks.COBBLESTONE.defaultBlockState());
        for (int x = x1; x <= x2; x++) {
            for (int z = z1; z <= z2; z++) {
                boolean rand = x == x1 || x == x2 || z == z1 || z == z2;
                b.setze(x, 0, z, Blocks.STONE_BRICKS);
                for (int y = 1; y <= hoehe; y++) {
                    b.setze(x, y, z, rand ? (b.chance(x, y, z, 0.2) ? Blocks.MOSSY_STONE_BRICKS : Blocks.STONE_BRICKS)
                            : Blocks.AIR);
                }
            }
        }
        int xm = (x1 + x2) / 2;
        // Portal (zweiflügelig) und Durchgang ins Schiff
        b.tuer(xm, 1, z1, V, Blocks.DARK_OAK_DOOR, r.nextBoolean());
        b.setze(xm - 1, 1, z1, LUFT);
        b.setze(xm - 1, 2, z1, LUFT);
        b.setze(xm + 1, 1, z1, LUFT);
        b.setze(xm + 1, 2, z1, LUFT);
        b.tuer(xm - 1, 1, z1, V, Blocks.DARK_OAK_DOOR, true);
        for (int x = xm - 1; x <= xm + 1; x++) {
            for (int y = 1; y <= 3; y++) {
                b.setze(x, y, z2, LUFT);
            }
        }
        // Glockenstube mit Schallöffnungen
        int stube = hoehe - 4;
        for (int x = x1 + 1; x < x2; x++) {
            for (int z = z1 + 1; z < z2; z++) {
                b.setze(x, stube, z, Blocks.SPRUCE_PLANKS);
            }
        }
        b.leiter(x2 - 1, 1, stube, z2 - 1, V);
        b.setze(x2 - 1, stube, z2 - 1, Blocks.LADDER.defaultBlockState()
                .setValue(net.minecraft.world.level.block.LadderBlock.FACING, V));
        for (int y = stube + 1; y <= stube + 2; y++) {
            b.setze(xm, y, z1, LUFT);
            b.setze(xm, y, z2, LUFT);
            b.setze(x1, y, (z1 + z2) / 2, LUFT);
            b.setze(x2, y, (z1 + z2) / 2, LUFT);
        }
        b.setze(xm, stube + 3, (z1 + z2) / 2, Blocks.SPRUCE_PLANKS);
        b.setze(xm, stube + 2, (z1 + z2) / 2, Blocks.BELL.defaultBlockState()
                .setValue(BellBlock.ATTACHMENT, BellAttachType.CEILING).setValue(BellBlock.FACING, V));
        // Spitzhelm
        int y = hoehe + 1;
        for (int ring = 0; ring <= 2; ring++) {
            int a1 = x1 + ring;
            int a2 = x2 - ring;
            int c1 = z1 + ring;
            int c2 = z2 - ring;
            for (int x = a1; x <= a2; x++) {
                for (int z = c1; z <= c2; z++) {
                    if (x == a1 || x == a2 || z == c1 || z == c2) {
                        b.setze(x, y + ring, z, Blocks.DEEPSLATE_TILES);
                        b.setze(x, y + ring + 1, z, Blocks.DEEPSLATE_TILES);
                    }
                }
            }
            y++;
        }
        int spitze = hoehe + 1 + 2 + 3;
        b.setze(xm, spitze, (z1 + z2) / 2, Blocks.DEEPSLATE_TILE_WALL);
        b.setze(xm, spitze + 1, (z1 + z2) / 2, Blocks.LIGHTNING_ROD);
    }

    private void friedhof(Baukasten b, Random r, int B, int T, int x1, int x2, int z1, int z2) {
        // Gräberreihen links und rechts der Kirche und dahinter
        for (int z = 3; z < T - 2; z += 3) {
            for (int x = 1; x < B - 1; x += 3) {
                boolean neben = x < x1 - 3 || x > x2 + 2;
                boolean hinten = z > z2 + 1;
                if (!(neben || hinten) || z < 3) {
                    continue;
                }
                grab(b, x, z);
            }
        }
        // Eisenzaun ums Grundstück
        for (int x = 0; x < B; x++) {
            int xm = B / 2;
            if (Math.abs(x - xm) <= 1) {
                continue;
            }
            if (!b.chance(x, 1, 0, 0.15)) {
                b.setzeFrei(x, 1, 0, b.scheibe(Blocks.IRON_BARS, true));
            }
        }
        for (int z = 1; z < T; z++) {
            if (!b.chance(0, 1, z, 0.15)) {
                b.setzeFrei(0, 1, z, b.scheibe(Blocks.IRON_BARS, false));
            }
            if (!b.chance(B - 1, 1, z, 0.15)) {
                b.setzeFrei(B - 1, 1, z, b.scheibe(Blocks.IRON_BARS, false));
            }
        }
        // Weg zum Portal
        Hausbau.weg(b, B / 2 - 1, 0, 3, 3);
        Hausbau.verwildern(b, 0, 0, B - 1, T - 1);
        Hausbau.toterBaum(b, 2, T - 3, r);
        Hausbau.toterBaum(b, B - 3, 4, r);
    }

    /** Ein Grab: Grabstein hinten, Grabbeet davor – manche sind aufgerissen. */
    private void grab(Baukasten b, int x, int z) {
        double w = b.zufall(x, 0, z);
        b.setze(x, 1, z + 1, w < 0.5 ? Blocks.STONE_BRICK_WALL.defaultBlockState()
                : w < 0.8 ? Blocks.MOSSY_STONE_BRICK_WALL.defaultBlockState()
                : Hausbau.treppe(Blocks.STONE_BRICK_STAIRS, H, false));
        if (w > 0.82) {
            // Offenes Grab mit Erdhaufen
            b.setze(x, 0, z, LUFT);
            b.setze(x, -1, z, LUFT);
            b.setze(x, -2, z, Blocks.COARSE_DIRT);
            b.setze(x + 1, 1, z, Blocks.COARSE_DIRT);
            if (w > 0.95) {
                b.setze(x, -1, z, Blocks.SKELETON_SKULL);
            }
        } else {
            b.setze(x, 0, z, Blocks.PODZOL);
            if (w < 0.3) {
                b.setze(x, 1, z, Blocks.WITHER_ROSE);
            } else if (w < 0.45) {
                b.setze(x, 1, z, Blocks.CANDLE);
            } else if (w < 0.7) {
                b.setze(x, 1, z, Blocks.DEAD_BUSH);
            }
        }
    }
}
