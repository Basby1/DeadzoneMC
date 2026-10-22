package de.deadzonemc.orbit;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Macht aus der Arena eine echte, zerstörte Orbitalstation: Reaktorring über dem Krater, Gitterträger mit
 * Solarflügeln, Habitat-Modul mit Kojen und Logbüchern, ein abgerissener Arm mit Rauch, Andockring, Antennenmast
 * und Trümmer, die um die Insel schweben. Alles liegt außerhalb der Kampffläche oder hoch über ihr.
 */
final class Stationsausbau {

    private Stationsausbau() {
    }

    private static final BlockState LUFT = Blocks.AIR.defaultBlockState();
    private static final BlockState WEISS = Blocks.WHITE_CONCRETE.defaultBlockState();
    private static final BlockState GRAU = Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState();
    private static final BlockState DUNKEL = Blocks.GRAY_CONCRETE.defaultBlockState();
    private static final BlockState EISEN = Blocks.IRON_BLOCK.defaultBlockState();
    private static final BlockState GITTER = Blocks.IRON_BARS.defaultBlockState();
    private static final BlockState GLAS = Blocks.CYAN_STAINED_GLASS.defaultBlockState();
    private static final BlockState LICHT = Blocks.SEA_LANTERN.defaultBlockState();
    private static final BlockState ORANGE = Blocks.ORANGE_CONCRETE.defaultBlockState();
    private static final BlockState SCHWARZ = Blocks.BLACK_CONCRETE.defaultBlockState();
    private static final BlockState GELB = Blocks.YELLOW_CONCRETE.defaultBlockState();
    private static final BlockState PANEL = Blocks.BLUE_STAINED_GLASS.defaultBlockState();
    private static final BlockState RAHMEN = Blocks.GRAY_CONCRETE.defaultBlockState();
    private static final BlockState RING = Blocks.POLISHED_BLACKSTONE.defaultBlockState();
    private static final BlockState VIOLETT = Blocks.PURPLE_STAINED_GLASS.defaultBlockState();
    private static final BlockState KERN = Blocks.CRYING_OBSIDIAN.defaultBlockState();

    private static ServerLevel w;
    private static int cx, cy, cz;

    private static void s(int x, int y, int z, BlockState b) {
        w.setBlock(new BlockPos(cx + x, cy + y, cz + z), b, Block.UPDATE_CLIENTS);
    }

    /** Nur in Luft setzen (für Trümmer und Anbauten, die nichts überschreiben sollen). */
    private static void sl(int x, int y, int z, BlockState b) {
        BlockPos p = new BlockPos(cx + x, cy + y, cz + z);
        if (w.getBlockState(p).isAir()) {
            w.setBlock(p, b, Block.UPDATE_CLIENTS);
        }
    }

    private static void lampe(int x, int y, int z) {
        s(x, y, z, Blocks.REDSTONE_LAMP.defaultBlockState().setValue(RedstoneLampBlock.LIT, true));
        s(x, y + 1, z, Blocks.REDSTONE_BLOCK.defaultBlockState());
    }

    static void bauen(ServerLevel welt, int y, int mx, int mz, int r) {
        w = welt;
        cx = mx;
        cy = y;
        cz = mz;
        RandomSource zufall = RandomSource.create(4711L);
        wandkrone(r);
        reaktorring(r);
        traeger(r);
        habitat(r, zufall);
        abgerissenerArm(r, zufall);
        andockring(r);
        antenne(r);
        truemmer(r, zufall);
        w = null;
    }

    /**
     * Unsichtbare Lichtquellen rund um die Station: Im End ist es sonst so dunkel, dass man von Hülle, Flügeln und
     * Modulen kaum etwas sieht. Lichtblöcke sind unsichtbar und nicht anfassbar.
     */
    static void beleuchten(ServerLevel welt, int y, int mx, int mz, int r) {
        w = welt;
        cx = mx;
        cy = y;
        cz = mz;
        BlockState l = Blocks.LIGHT.defaultBlockState().setValue(net.minecraft.world.level.block.LightBlock.LEVEL, 13);
        BlockState schwach = Blocks.LIGHT.defaultBlockState().setValue(net.minecraft.world.level.block.LightBlock.LEVEL, 9);
        // außen um die Arenawand und unter der Plattform
        for (int i = 0; i < 48; i++) {
            double wk = 2 * Math.PI * i / 48;
            int x = (int) Math.round(Math.cos(wk) * (r + 3));
            int z = (int) Math.round(Math.sin(wk) * (r + 3));
            sl(x, 3, z, l);
            sl(x, 9, z, schwach);
            sl((int) Math.round(Math.cos(wk) * (r - 4)), -3, (int) Math.round(Math.sin(wk) * (r - 4)), schwach);
        }
        // auf der Kampffläche (dezent, damit es unheimlich bleibt)
        for (int x = -r + 4; x <= r - 4; x += 8) {
            for (int z = -r + 4; z <= r - 4; z += 8) {
                if (x * x + z * z < (r - 3) * (r - 3)) {
                    sl(x, 4, z, schwach);
                }
            }
        }
        // Reaktorring und Gitterträger
        for (int i = 0; i < 16; i++) {
            double wk = 2 * Math.PI * i / 16;
            sl((int) Math.round(Math.cos(wk) * 10.5), 21, (int) Math.round(Math.sin(wk) * 10.5), l);
        }
        for (int x = -(r + 40); x <= r + 40; x += 6) {
            sl(x, 27, 0, l);
            sl(x, 33, 0, l);
        }
        // Solarflügel: von beiden Seiten angestrahlt
        for (int seite : new int[]{1, -1}) {
            for (int x = r + 8; x <= r + 40; x += 4) {
                for (int z = -24; z <= 24; z += 6) {
                    sl(seite * x, 30, z + (z == 0 ? 3 : 0), l);
                }
            }
        }
        // Module, Arme, Andockring, Antenne
        for (int x = r; x <= r + 30; x += 4) {
            for (int z : new int[]{-9, 9}) {
                sl(x, 4, z, l);
                sl(-x, 4, z, l);
            }
            sl(x, 10, 0, l);
        }
        for (int z = r; z <= r + 12; z += 4) {
            sl(-5, 4, z, l);
            sl(5, 4, z, l);
        }
        for (int y2 = 0; y2 <= 34; y2 += 6) {
            sl(5, y2, -(r + 4), l);
            sl(9, y2, -(r + 4), l);
        }
        w = null;
    }

    /** Leuchtende Krone auf der Außenwand: von Weitem sieht man die Station als Lichterkreis. */
    private static void wandkrone(int r) {
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz);
                if (d > r - 0.5 && d <= r + 0.5 && Math.abs(dx) > 2) {
                    s(dx, 6, dz, (dx + dz) % 4 == 0 ? LICHT : WEISS);
                }
            }
        }
    }

    /** Der Reaktorring schwebt über dem Krater, gehalten von vier Streben; in der Mitte glimmt der Kern. */
    private static void reaktorring(int r) {
        int h = 24;
        for (int dx = -12; dx <= 12; dx++) {
            for (int dz = -12; dz <= 12; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz);
                if (d >= 9.5 && d <= 11.5) {
                    s(dx, h, dz, (dx * 7 + dz * 3) % 5 == 0 ? VIOLETT : RING);
                    if (d >= 10.3 && d <= 10.8 && (dx + dz) % 3 == 0) {
                        s(dx, h - 1, dz, Blocks.END_ROD.defaultBlockState());
                    }
                    if (d > 11) {
                        s(dx, h + 1, dz, EISEN);
                    }
                }
            }
        }
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                s(dx, h + 2, dz, Math.abs(dx) + Math.abs(dz) == 2 ? VIOLETT : KERN);
            }
        }
        s(0, h + 3, 0, KERN);
        s(0, h + 1, 0, VIOLETT);
        // vier Streben von der Wandkrone zum Ring
        for (int i = 0; i < 4; i++) {
            double wk = Math.PI / 4 + i * Math.PI / 2;
            for (int k = 0; k <= 40; k++) {
                double t = k / 40.0;
                double rad = r - 1 + (11 - (r - 1)) * t;
                int x = (int) Math.round(Math.cos(wk) * rad);
                int z = (int) Math.round(Math.sin(wk) * rad);
                int y = 7 + (int) Math.round((h - 7) * t);
                s(x, y, z, k % 8 == 0 ? LICHT : EISEN);
            }
        }
    }

    /** Gitterträger quer über die Station, an beiden Enden Solarflügel (der westliche halb abgerissen). */
    private static void traeger(int r) {
        int h = 30;
        int laenge = r + 40;
        for (int x = -laenge; x <= laenge; x++) {
            for (int a = -1; a <= 1; a++) {
                for (int b = -1; b <= 1; b++) {
                    boolean ecke = Math.abs(a) == 1 && Math.abs(b) == 1;
                    if (ecke) {
                        s(x, h + a, b, x % 6 == 0 ? LICHT : EISEN);
                    } else if (x % 3 == 0 && (a == 0) != (b == 0)) {
                        s(x, h + a, b, GITTER);
                    }
                }
            }
        }
        fluegel(r, h);
    }

    /** Solarflügel: flache Paneelfelder an den Trägerenden, außerhalb der Arena, nach Norden und Süden ausgefahren. */
    private static void fluegel(int r, int h) {
        for (int seite : new int[]{1, -1}) {
            for (int f = 0; f < 2; f++) {
                int x0 = seite * (r + 8 + f * 16);
                for (int dx = 0; dx < 13; dx++) {
                    int x = x0 + seite * dx;
                    for (int z = -26; z <= 26; z++) {
                        if (Math.abs(z) < 3) {
                            continue;
                        }
                        // der westliche äußere Flügel ist halb abgerissen: unregelmäßige Kante, ein Stück hängt schief
                        if (seite < 0 && f == 1 && z > 4 + ((dx * 7) % 9)) {
                            if (z < 20 && dx > 3 && dx < 10) {
                                s(x, h - 2 - (z - 4) / 3, z, PANEL);
                            }
                            continue;
                        }
                        boolean rand = dx == 0 || dx == 12 || Math.abs(z) % 8 == 3 || Math.abs(z) == 26;
                        s(x, h, z, rand ? RAHMEN : PANEL);
                        if (rand && (dx == 0 || dx == 12) && Math.abs(z) % 8 == 3) {
                            s(x, h + 1, z, LICHT);
                        }
                    }
                    s(x, h, -2, EISEN);
                    s(x, h, 2, EISEN);
                }
            }
        }
    }

    /** Ältere Fassung hatte massive Glasquader statt Paneelen: wegräumen und neu setzen. */
    static void fluegelErneuern(ServerLevel welt, int y, int mx, int mz, int r) {
        w = welt;
        cx = mx;
        cy = y;
        cz = mz;
        int h = 30;
        for (int seite : new int[]{1, -1}) {
            for (int x = r + 8; x <= r + 44; x++) {
                for (int z = -26; z <= 26; z++) {
                    if (Math.abs(z) < 3) {
                        continue;
                    }
                    for (int dy = -6; dy <= 6; dy++) {
                        BlockPos p = new BlockPos(cx + seite * x, cy + h + dy, cz + z);
                        BlockState alt = w.getBlockState(p);
                        if (alt.is(Blocks.BLUE_STAINED_GLASS) || alt.is(Blocks.GRAY_CONCRETE)) {
                            w.setBlock(p, LUFT, Block.UPDATE_CLIENTS);
                        }
                    }
                }
            }
        }
        fluegel(r, h);
        w = null;
    }

    /** Habitat-Modul im Osten: Gang durch die Wand, Kojen, Spinde mit Logbüchern, Warnleuchten. */
    private static void habitat(int r, RandomSource zufall) {
        // Gang
        for (int x = r - 1; x <= r + 16; x++) {
            for (int z = -3; z <= 3; z++) {
                for (int y = 0; y <= 6; y++) {
                    boolean huelle = Math.abs(z) == 3 || y == 0 || y == 6;
                    BlockState b = huelle ? (y == 3 && Math.abs(z) == 3 && x % 3 != 0 ? GLAS : (y == 0 ? GRAU : WEISS)) : LUFT;
                    if (x <= r && !huelle) {
                        b = LUFT; // Durchgang in die Arena
                    }
                    s(x, y, z, b);
                }
            }
            if (x % 4 == 0) {
                s(x, 5, 0, LICHT);
            }
        }
        // Modul
        int x0 = r + 17;
        for (int x = x0; x <= x0 + 12; x++) {
            for (int z = -7; z <= 7; z++) {
                for (int y = 0; y <= 8; y++) {
                    boolean huelle = x == x0 || x == x0 + 12 || Math.abs(z) == 7 || y == 0 || y == 8;
                    BlockState b = LUFT;
                    if (huelle) {
                        b = y == 2 ? ORANGE : (y == 4 || y == 5) && (x + z) % 3 != 0 && y != 0 && y != 8 ? GLAS : WEISS;
                        if (y == 0) {
                            b = GRAU;
                        }
                    }
                    if (x == x0 && Math.abs(z) <= 2 && y >= 1 && y <= 5) {
                        b = LUFT; // Zugang vom Gang
                    }
                    s(x, y, z, b);
                }
            }
        }
        // Kojen an der Nordwand, Spinde an der Südwand
        for (int i = 0; i < 4; i++) {
            int x = x0 + 2 + i * 3;
            s(x, 1, -6, Blocks.WHITE_WOOL.defaultBlockState());
            s(x + 1, 1, -6, Blocks.WHITE_WOOL.defaultBlockState());
            s(x, 4, -6, Blocks.LIGHT_GRAY_WOOL.defaultBlockState());
            s(x + 1, 4, -6, Blocks.LIGHT_GRAY_WOOL.defaultBlockState());
            s(x, 3, -6, Blocks.IRON_TRAPDOOR.defaultBlockState());
        }
        String[] logs = {"orbit_log_3", "orbit_log_4", "orbit_log_5"};
        for (int i = 0; i < 6; i++) {
            BlockPos p = new BlockPos(cx + x0 + 2 + i * 2, cy + 1, cz + 6);
            w.setBlock(p, Blocks.BARREL.defaultBlockState(), Block.UPDATE_CLIENTS);
            if (i % 2 == 0 && w.getBlockEntity(p) instanceof net.minecraft.world.level.block.entity.BarrelBlockEntity fass) {
                fass.setItem(0, de.deadzonemc.item.NotizItem.erstellen(logs[i / 2]));
                fass.setItem(2, new net.minecraft.world.item.ItemStack(de.deadzonemc.ModRegistry.ELEKTRONIK.get(), 1 + zufall.nextInt(2)));
            }
            s(x0 + 2 + i * 2, 2, 6, Blocks.IRON_BLOCK.defaultBlockState());
        }
        // Tisch in der Mitte, Konsole mit Bildschirm
        s(x0 + 6, 1, 0, Blocks.POLISHED_DEEPSLATE.defaultBlockState());
        s(x0 + 7, 1, 0, Blocks.POLISHED_DEEPSLATE.defaultBlockState());
        s(x0 + 6, 2, 0, Blocks.LIGHT_GRAY_STAINED_GLASS_PANE.defaultBlockState());
        // rote Warnleuchten unter der Decke (dauerhaft an)
        lampe(x0 + 3, 7, 0);
        lampe(x0 + 9, 7, 0);
        // ein Leck: Wand aufgerissen, Rauch steigt auf
        for (int y = 3; y <= 6; y++) {
            s(x0 + 12, y, 3 + (y % 2), LUFT);
            s(x0 + 12, y, 4, LUFT);
        }
        s(x0 + 13, -1, 4, Blocks.HAY_BLOCK.defaultBlockState());
        s(x0 + 13, 0, 4, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.SIGNAL_FIRE, true));
    }

    /** Westarm: nach wenigen Metern abgerissen, die Stücke treiben noch in der Nähe. Es raucht. */
    private static void abgerissenerArm(int r, RandomSource zufall) {
        for (int x = r - 1; x <= r + 9; x++) {
            int bis = r + 5 + (x * 13 % 5);
            for (int z = -3; z <= 3; z++) {
                for (int y = 0; y <= 6; y++) {
                    boolean huelle = Math.abs(z) == 3 || y == 0 || y == 6;
                    int xx = -x;
                    if (x > bis && (y + z) % 2 == 0) {
                        continue; // gezackte Bruchkante
                    }
                    s(xx, y, z, huelle ? (y == 3 && Math.abs(z) == 3 ? GLAS : WEISS) : LUFT);
                }
            }
        }
        // treibende Stücke
        for (int i = 0; i < 6; i++) {
            int x0 = -(r + 14 + i * 4);
            int y0 = 2 + zufall.nextInt(9) - 3 + i;
            int z0 = zufall.nextInt(13) - 6;
            for (int a = 0; a < 3; a++) {
                for (int b = 0; b < 3; b++) {
                    if (zufall.nextFloat() < 0.75F) {
                        sl(x0 + a, y0 + (a + b) / 3, z0 + b, zufall.nextBoolean() ? WEISS : GRAU);
                    }
                }
            }
        }
        s(-(r + 6), -1, 0, Blocks.HAY_BLOCK.defaultBlockState());
        s(-(r + 6), 0, 0, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.SIGNAL_FIRE, true));
        s(-(r + 3), -1, 2, Blocks.HAY_BLOCK.defaultBlockState());
        s(-(r + 3), 0, 2, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.SIGNAL_FIRE, true));
    }

    /** Andockring vor dem Südtor: kurzer Tunnel, gelb-schwarzer Rahmen. */
    private static void andockring(int r) {
        for (int z = r; z <= r + 7; z++) {
            for (int x = -3; x <= 3; x++) {
                for (int y = 0; y <= 6; y++) {
                    boolean huelle = Math.abs(x) == 3 || y == 0 || y == 6;
                    s(x, y, z, huelle ? (y == 3 && Math.abs(x) == 3 ? GLAS : WEISS) : LUFT);
                }
            }
        }
        int z = r + 8;
        for (int x = -5; x <= 5; x++) {
            for (int y = -2; y <= 8; y++) {
                boolean rahmen = Math.abs(x) >= 4 || y <= -1 || y >= 7;
                if (rahmen) {
                    s(x, y, z, ((x + y) & 1) == 0 ? GELB : SCHWARZ);
                } else {
                    s(x, y, z, LUFT);
                }
            }
        }
        lampe(-5, 9, z);
        lampe(5, 9, z);
    }

    /** Antennenmast neben dem Nordtor mit Schüssel und Warnlicht. */
    private static void antenne(int r) {
        int x = 7;
        int z = -(r + 4);
        for (int y = -6; y <= 34; y++) {
            s(x, y, z, y % 6 == 0 ? EISEN : Blocks.LIGHTNING_ROD.defaultBlockState());
        }
        s(x, 35, z, Blocks.REDSTONE_LAMP.defaultBlockState().setValue(RedstoneLampBlock.LIT, true));
        s(x, 36, z, Blocks.REDSTONE_BLOCK.defaultBlockState());
        // Schüssel (nach Norden gekippt)
        for (int a = -4; a <= 4; a++) {
            for (int b = -4; b <= 4; b++) {
                if (a * a + b * b <= 17) {
                    int tiefe = (a * a + b * b) / 6;
                    s(x + a, 26 + b, z - 2 - tiefe + (b > 0 ? 0 : 0), WEISS);
                }
            }
        }
        s(x, 26, z - 1, EISEN);
        s(x, 26, z - 5, Blocks.END_ROD.defaultBlockState().setValue(net.minecraft.world.level.block.EndRodBlock.FACING, Direction.NORTH));
    }

    /** Trümmer, die um die Station treiben: Hüllenplatten, Glas, Kisten. */
    private static void truemmer(int r, RandomSource zufall) {
        BlockState[] stoffe = {WEISS, GRAU, DUNKEL, EISEN, GLAS, Blocks.OBSIDIAN.defaultBlockState(), PANEL};
        for (int i = 0; i < 70; i++) {
            double wk = zufall.nextDouble() * Math.PI * 2;
            double d = r + 14 + zufall.nextDouble() * 55;
            int x0 = (int) (Math.cos(wk) * d);
            int z0 = (int) (Math.sin(wk) * d);
            int y0 = -10 + zufall.nextInt(50);
            BlockState st = stoffe[zufall.nextInt(stoffe.length)];
            int groesse = 1 + zufall.nextInt(i % 7 == 0 ? 6 : 3);
            boolean platte = zufall.nextBoolean();
            for (int a = 0; a < groesse; a++) {
                for (int b = 0; b < (platte ? groesse : 1); b++) {
                    if (zufall.nextFloat() < 0.8F) {
                        sl(x0 + a, y0 + (platte ? 0 : b), z0 + (platte ? b : 0), st);
                    }
                }
            }
            if (i % 9 == 0) {
                for (int k = 1; k <= 3; k++) {
                    sl(x0, y0 - k, z0, Blocks.CHAIN.defaultBlockState());
                }
            }
            if (i % 11 == 0) {
                sl(x0, y0 + 1, z0, LICHT);
            }
        }
    }
}
