package de.deadzonemc.welt.station;

import de.deadzonemc.ModRegistry;
import de.deadzonemc.monster.MonsterTypen;
import de.deadzonemc.welt.bau.Baukasten;
import de.deadzonemc.zone.AnomalieArt;
import de.deadzonemc.zone.AnomalieBlock;
import de.deadzonemc.zone.Zone;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import static de.deadzonemc.welt.bau.Baukasten.H;
import static de.deadzonemc.welt.bau.Baukasten.L;
import static de.deadzonemc.welt.bau.Baukasten.R;
import static de.deadzonemc.welt.bau.Baukasten.V;

/**
 * Baut Station Null. Lokale Koordinaten 0..47, ly 0 = Boden an der Oberfläche, Laborboden bei ly -24.
 *
 * <pre>
 *            Anomalie-Labor (N)
 *   Wache  ──┐   │   ┌── Labor A
 *   ─────── Halle (Schacht) ───────
 *   Archiv ──┘   │   └── Schlafsaal
 *            Reaktorhalle (S)
 * </pre>
 */
public final class StationNullBau {

    private StationNullBau() {
    }

    private static final int U = -StationNullStruktur.TIEFE; // Laborboden

    private static final BlockState WAND = Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState();
    private static final BlockState SOCKEL = Blocks.POLISHED_ANDESITE.defaultBlockState();
    private static final BlockState DECKE = Blocks.SMOOTH_STONE.defaultBlockState();
    private static final BlockState HUELLE = Blocks.STONE.defaultBlockState();
    private static final BlockState FLIESE = Blocks.WHITE_CONCRETE.defaultBlockState();
    private static final BlockState FLIESE2 = Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState();
    private static final BlockState BETON = Blocks.GRAY_CONCRETE.defaultBlockState();

    /** Ein Raum: Innenmaße und Deckenhöhe (Anzahl Luftschichten). */
    private record Raum(int x1, int z1, int x2, int z2, int hoehe) {
    }

    private static final Raum HALLE = new Raum(20, 20, 27, 27, 4);
    private static final Raum GANG_W = new Raum(4, 23, 19, 25, 3);
    private static final Raum GANG_O = new Raum(28, 23, 43, 25, 3);
    private static final Raum GANG_N = new Raum(23, 14, 25, 19, 3);
    private static final Raum GANG_S = new Raum(23, 28, 25, 32, 3);
    private static final Raum WACHE = new Raum(5, 13, 15, 21, 4);
    private static final Raum LABOR = new Raum(32, 13, 42, 21, 4);
    private static final Raum ARCHIV = new Raum(5, 27, 15, 35, 4);
    private static final Raum SCHLAF = new Raum(32, 27, 42, 35, 4);
    private static final Raum ANOMALIE = new Raum(17, 4, 31, 12, 5);
    private static final Raum REAKTOR = new Raum(14, 33, 34, 45, 7);
    private static final Raum[] ALLE = {HALLE, GANG_W, GANG_O, GANG_N, GANG_S, WACHE, LABOR, ARCHIV, SCHLAF, ANOMALIE, REAKTOR};

    public static void bauen(Baukasten b, WorldGenLevel level, BoundingBox bereich) {
        oberflaeche(b);
        schacht(b);
        // Hülle und Wände, dann alle Räume aushöhlen
        b.fuelle(2, U - 1, 2, 45, U + 9, 46, HUELLE);
        for (Raum r : ALLE) {
            b.fuelle(r.x1 - 1, U, r.z1 - 1, r.x2 + 1, U + r.hoehe + 1, r.z2 + 1, WAND);
        }
        for (Raum r : ALLE) {
            aushoehlen(b, r);
        }
        // Durchgänge zwischen Räumen und Gängen
        tuer(b, 10, 22, true);
        tuer(b, 37, 22, true);
        tuer(b, 10, 26, true);
        tuer(b, 37, 26, true);
        b.luft(23, U + 1, 13, 25, U + 3, 13);      // zum Anomalie-Labor
        b.luft(22, U + 1, 33, 26, U + 4, 33);      // zur Reaktorhalle
        b.luft(23, U + 1, 28, 25, U + 3, 28);
        b.luft(19, U + 1, 23, 19, U + 3, 25);
        b.luft(28, U + 1, 23, 28, U + 3, 25);
        b.luft(23, U + 1, 19, 25, U + 3, 19);

        halle(b);
        wache(b, level);
        labor(b, level);
        archiv(b);
        schlafsaal(b);
        anomalieLabor(b);
        reaktor(b, level);
        verfall(b);
    }

    // ---- Oberfläche ------------------------------------------------------------------------

    private static void oberflaeche(Baukasten b) {
        b.boden(12, 12, 35, 35, Blocks.GRAVEL.defaultBlockState(), 6);
        for (int x = 12; x <= 35; x++) {
            for (int z = 12; z <= 35; z++) {
                if (b.chance(x, 0, z, 0.35)) {
                    b.setze(x, 0, z, b.chance(x, 1, z, 0.5) ? Blocks.COARSE_DIRT : Blocks.COBBLESTONE);
                }
            }
        }
        // Zaun mit Lücken und Stacheldraht
        for (int i = 12; i <= 35; i++) {
            for (int[] p : new int[][]{{i, 12}, {i, 35}, {12, i}, {35, i}}) {
                boolean tor = p[1] == 12 && p[0] >= 22 && p[0] <= 25;
                if (tor || b.chance(p[0], 2, p[1], 0.12)) {
                    continue;
                }
                boolean entlangLx = p[1] == 12 || p[1] == 35;
                b.setze(p[0], 1, p[1], b.scheibe(Blocks.IRON_BARS, entlangLx));
                b.setze(p[0], 2, p[1], b.chance(p[0], 3, p[1], 0.5)
                        ? de.deadzonemc.falle.Fallen.STACHELDRAHT.get().defaultBlockState()
                        : b.scheibe(Blocks.IRON_BARS, entlangLx));
            }
        }
        b.schild(21, 2, 11, V, Blocks.SPRUCE_WALL_SIGN, DyeColor.RED, "HALDEN-WERKE", "STATION NULL", "Zutritt nur für", "Befugte");
        b.schild(26, 2, 11, V, Blocks.SPRUCE_WALL_SIGN, DyeColor.YELLOW, "ACHTUNG", "Strahlung!", "Schutzausrüstung", "tragen");
        b.setze(21, 1, 11, Blocks.IRON_BARS);
        b.setze(26, 1, 11, Blocks.IRON_BARS);
        // Bunkerhäuschen über dem Schacht
        b.fuelle(19, 0, 18, 28, 0, 27, BETON);
        b.waende(19, 18, 28, 27, 1, 4, BETON);
        b.fuelle(19, 5, 18, 28, 5, 27, Blocks.SMOOTH_STONE_SLAB.defaultBlockState());
        b.luft(20, 1, 19, 27, 4, 26);
        b.luft(23, 1, 18, 24, 3, 18);
        b.tuer(23, 1, 18, V, Blocks.IRON_DOOR, true);
        b.setze(24, 1, 18, luft());
        b.setze(20, 3, 19, Blocks.REDSTONE_LAMP);
        b.setze(27, 3, 26, Blocks.LANTERN);
        // Strahlende Fässer und Schutt auf dem Gelände
        int[][] faesser = {{15, 15}, {16, 15}, {15, 16}, {32, 31}, {31, 32}, {14, 30}};
        for (int[] f : faesser) {
            b.setze(f[0], 1, f[1], ModRegistry.STRAHLENFASS.get().defaultBlockState());
        }
        b.setze(30, 1, 15, Blocks.IRON_BLOCK);
        b.setze(30, 2, 15, Blocks.IRON_BARS);
        b.setze(30, 3, 15, Blocks.REDSTONE_LAMP);
        b.setze(17, 1, 31, Blocks.CHAIN);
    }

    private static BlockState luft() {
        return Blocks.AIR.defaultBlockState();
    }

    private static void schacht(Baukasten b) {
        // Betonschacht mit Leiter, 4×4 innen
        for (int y = U + 1; y <= 0; y++) {
            for (int x = 21; x <= 26; x++) {
                for (int z = 21; z <= 26; z++) {
                    boolean innen = x >= 22 && x <= 25 && z >= 22 && z <= 25;
                    if (innen) {
                        b.setze(x, y, z, luft());
                    } else if (y < 0) {
                        b.setze(x, y, z, BETON);
                    }
                }
            }
            b.setze(23, y, 25, Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, V));
            b.setze(24, y, 25, Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, V));
            if (y % 6 == 0 && y < -2) {
                b.setze(22, y, 22, Blocks.LANTERN.defaultBlockState().setValue(BlockStateProperties.HANGING, false));
            }
        }
        // Geländer oben
        for (int x = 22; x <= 25; x++) {
            b.setze(x, 1, 21, b.scheibe(Blocks.IRON_BARS, true));
        }
        b.setze(23, 1, 21, luft());
        b.setze(24, 1, 21, luft());
    }

    private static void aushoehlen(Baukasten b, Raum r) {
        for (int x = r.x1; x <= r.x2; x++) {
            for (int z = r.z1; z <= r.z2; z++) {
                b.setze(x, U, z, b.chance(x, U, z, 0.18) ? FLIESE2 : FLIESE);
                for (int y = U + 1; y <= U + r.hoehe; y++) {
                    b.setze(x, y, z, luft());
                }
                boolean lampe = (x - r.x1) % 5 == 2 && (z - r.z1) % 5 == 2;
                b.setze(x, U + r.hoehe + 1, z, lampe ? (b.chance(x, 9, z, 0.4) ? Blocks.REDSTONE_LAMP.defaultBlockState()
                        : Blocks.SEA_LANTERN.defaultBlockState()) : DECKE);
            }
        }
        // Sockelleiste
        for (int x = r.x1 - 1; x <= r.x2 + 1; x++) {
            for (int z = r.z1 - 1; z <= r.z2 + 1; z++) {
                boolean rand = x == r.x1 - 1 || x == r.x2 + 1 || z == r.z1 - 1 || z == r.z2 + 1;
                if (rand && b.hole(x, U + 1, z).is(WAND.getBlock())) {
                    b.setze(x, U + 1, z, SOCKEL);
                }
            }
        }
    }

    /** Durchgang mit offener Eisentür in einer Wand bei (x, z). {@code querLx}: Wand verläuft entlang lx. */
    private static void tuer(Baukasten b, int x, int z, boolean querLx) {
        b.luft(x, U + 1, z, x, U + 2, z);
        b.tuer(x, U + 1, z, querLx ? V : R, Blocks.IRON_DOOR, b.chance(x, U, z, 0.6));
    }

    // ---- Räume -------------------------------------------------------------------------------

    private static void halle(Baukasten b) {
        b.schild(20, U + 3, 19, H, Blocks.SPRUCE_WALL_SIGN, DyeColor.WHITE, "STATION NULL", "Ebene -1", "Projekt AURA", "");
        b.schild(19, U + 3, 22, L, Blocks.SPRUCE_WALL_SIGN, DyeColor.WHITE, "<- Wache", "<- Archiv", "", "");
        b.schild(28, U + 3, 22, R, Blocks.SPRUCE_WALL_SIGN, DyeColor.WHITE, "Labor A ->", "Schlafsaal ->", "", "");
        b.schild(22, U + 3, 28, V, Blocks.SPRUCE_WALL_SIGN, DyeColor.RED, "REAKTOR", "Nur mit", "Freigabe", "");
        b.setze(26, U + 1, 20, Blocks.SMOOTH_QUARTZ_SLAB);
        b.setze(26, U + 1, 21, Blocks.SMOOTH_QUARTZ_SLAB);
        b.setze(27, U + 1, 27, Blocks.CAULDRON);
    }

    private static void wache(Baukasten b, WorldGenLevel level) {
        b.fass(5, U + 1, 13, H, "waffenkammer");
        b.fass(6, U + 1, 13, H, "waffenkammer");
        b.truhe(15, U + 1, 13, R, "polizei");
        b.bett(5, U + 1, 20, R, Blocks.GRAY_BED);
        b.bett(5, U + 1, 18, R, Blocks.GRAY_BED);
        b.setze(11, U + 1, 16, Blocks.SPRUCE_FENCE);
        b.setze(11, U + 2, 16, Blocks.SPRUCE_PRESSURE_PLATE);
        b.setze(12, U + 1, 16, Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, R));
        // Arrestzelle
        for (int z = 17; z <= 21; z++) {
            b.setze(13, U + 1, z, b.scheibe(Blocks.IRON_BARS, false));
            b.setze(13, U + 2, z, b.scheibe(Blocks.IRON_BARS, false));
        }
        b.setze(15, U + 1, 20, Blocks.SKELETON_SKULL);
        spawner(b, level, 9, U + 1, 14, MonsterTypen.GEFALLENER_SOLDAT.get());
    }

    private static void labor(Baukasten b, WorldGenLevel level) {
        for (int x = 33; x <= 41; x += 2) {
            b.setze(x, U + 1, 13, Blocks.SMOOTH_QUARTZ);
            b.setze(x, U + 2, 13, x % 4 == 1 ? Blocks.BREWING_STAND.defaultBlockState() : Blocks.GLASS.defaultBlockState());
        }
        b.truhe(42, U + 1, 16, R, "apotheke");
        b.truhe(42, U + 1, 18, R, "apotheke");
        b.setze(35, U + 1, 19, Blocks.CAULDRON);
        b.setze(36, U + 1, 19, Blocks.CAULDRON);
        // Fenster zum Gang
        for (int x = 33; x <= 41; x++) {
            if (x != 37) {
                b.setze(x, U + 2, 22, b.scheibe(Blocks.GLASS_PANE, true));
            }
        }
        spawner(b, level, 38, U + 1, 17, MonsterTypen.KRIECHER.get());
    }

    private static void archiv(Baukasten b) {
        for (int x = 5; x <= 15; x++) {
            if (x == 10) {
                continue;
            }
            for (int y = U + 1; y <= U + 3; y++) {
                b.setze(x, y, 35, Blocks.BOOKSHELF);
            }
        }
        for (int z = 28; z <= 33; z++) {
            for (int y = U + 1; y <= U + 3; y++) {
                b.setze(5, y, z, Blocks.BOOKSHELF);
            }
        }
        b.setze(10, U + 1, 31, Blocks.LECTERN.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, V));
        b.setze(13, U + 1, 31, Blocks.LECTERN.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, V));
        b.truhe(15, U + 1, 28, R, "station_null");
        b.truhe(15, U + 1, 33, R, "buero");
        b.setze(8, U + 3, 29, Blocks.COBWEB);
    }

    private static void schlafsaal(Baukasten b) {
        for (int i = 0; i < 4; i++) {
            int x = 33 + i * 3;
            b.bett(x, U + 1, 35, H, Blocks.WHITE_BED);
            b.kiste(x + 1, U + 1, 35, Blocks.BARREL.defaultBlockState()
                    .setValue(net.minecraft.world.level.block.BarrelBlock.FACING, Direction.UP), "nachttisch");
        }
        for (int x = 33; x <= 41; x += 2) {
            b.fass(x, U + 1, 27, H, "kleiderschrank");
        }
    }

    private static void anomalieLabor(Baukasten b) {
        // Glaszelle mit der Probe A-7
        b.waende(21, 6, 27, 10, U + 1, U + 4, Blocks.GLASS.defaultBlockState());
        b.fuelle(21, U + 5, 6, 27, U + 5, 10, Blocks.IRON_BLOCK.defaultBlockState());
        b.setze(24, U + 1, 8, Zone.ANOMALIE.get().defaultBlockState().setValue(AnomalieBlock.ART, AnomalieArt.ENTLADUNG));
        b.setze(24, U, 8, Blocks.OXIDIZED_COPPER);
        b.setze(23, U, 7, Blocks.WEATHERED_COPPER);
        b.setze(25, U, 9, Blocks.WEATHERED_COPPER);
        // eingeschlagene Scheibe
        b.setze(24, U + 1, 10, luft());
        b.setze(24, U + 2, 10, luft());
        // Steuerpult
        for (int x = 19; x <= 29; x++) {
            b.setze(x, U + 1, 12, Blocks.POLISHED_DEEPSLATE);
        }
        b.setze(20, U + 2, 12, Blocks.LEVER.defaultBlockState().setValue(BlockStateProperties.ATTACH_FACE,
                net.minecraft.world.level.block.state.properties.AttachFace.FLOOR));
        b.setze(28, U + 2, 12, Blocks.REDSTONE_LAMP);
        b.truhe(17, U + 1, 4, H, "station_null");
        b.schild(24, U + 3, 11, H, Blocks.SPRUCE_WALL_SIGN, DyeColor.RED, "PROBE A-7", "NICHT BETRETEN", "Feld instabil", "");
    }

    private static void reaktor(Baukasten b, WorldGenLevel level) {
        // Boden aus gerissenen Fliesen, Kern in der Mitte
        for (int x = REAKTOR.x1; x <= REAKTOR.x2; x++) {
            for (int z = REAKTOR.z1; z <= REAKTOR.z2; z++) {
                b.setze(x, U, z, b.chance(x, U, z, 0.3) ? Blocks.CRACKED_DEEPSLATE_TILES : Blocks.DEEPSLATE_TILES);
            }
        }
        for (int x = 21; x <= 27; x++) {
            for (int z = 36; z <= 42; z++) {
                boolean rand = x == 21 || x == 27 || z == 36 || z == 42;
                if (rand) {
                    b.setze(x, U + 1, z, Blocks.IRON_BLOCK);
                }
            }
        }
        for (int y = U + 1; y <= U + 6; y++) {
            b.setze(24, y, 39, y % 2 == 0 ? Blocks.CRYING_OBSIDIAN : Blocks.SEA_LANTERN);
            for (int[] d : new int[][]{{-1, 0}, {1, 0}, {0, -1}, {0, 1}}) {
                if (y <= U + 4) {
                    b.setze(24 + d[0], y, 39 + d[1], Blocks.IRON_BARS);
                }
            }
        }
        int[][] faesser = {{16, 35}, {17, 35}, {16, 36}, {32, 35}, {32, 44}, {31, 44}, {16, 44}};
        for (int[] f : faesser) {
            b.setze(f[0], U + 1, f[1], ModRegistry.STRAHLENFASS.get().defaultBlockState());
        }
        // Laufsteg
        for (int x = REAKTOR.x1; x <= REAKTOR.x2; x++) {
            b.setze(x, U + 5, 34, Blocks.IRON_TRAPDOOR.defaultBlockState()
                    .setValue(BlockStateProperties.HALF, net.minecraft.world.level.block.state.properties.Half.TOP));
        }
        b.truhe(14, U + 1, 45, V, "bunker");
        b.truhe(34, U + 1, 45, V, "station_null");
        b.setze(33, U + 1, 40, Zone.ANOMALIE.get().defaultBlockState().setValue(AnomalieBlock.ART, AnomalieArt.SAEURE));
        spawner(b, level, 15, U + 1, 40, MonsterTypen.LAUERER.get());
        spawner(b, level, 30, U + 1, 37, MonsterTypen.WANDERER.get());
        b.schild(24, U + 4, 33, H, Blocks.SPRUCE_WALL_SIGN, DyeColor.RED, "KERN", "Abschaltung", "fehlgeschlagen", "");
    }

    // ---- Verfall -----------------------------------------------------------------------------

    private static void verfall(Baukasten b) {
        for (Raum r : ALLE) {
            for (int x = r.x1; x <= r.x2; x++) {
                for (int z = r.z1; z <= r.z2; z++) {
                    int oben = U + r.hoehe;
                    if (b.hole(x, oben, z).isAir() && b.chance(x, oben, z, 0.07)) {
                        b.setze(x, oben, z, Blocks.COBWEB);
                    }
                    if (b.hole(x, U + 1, z).isAir()) {
                        double c = b.zufall(x, U + 1, z);
                        if (c < 0.025) {
                            b.setze(x, U + 1, z, Blocks.REDSTONE_WIRE); // Blutspuren
                        } else if (c < 0.04) {
                            b.setze(x, U + 1, z, Blocks.GRAVEL);
                        } else if (c < 0.045) {
                            b.setze(x, U + 1, z, Blocks.BONE_BLOCK);
                        }
                    }
                }
            }
        }
    }

    private static void spawner(Baukasten b, WorldGenLevel level, int x, int y, int z, EntityType<?> typ) {
        if (!b.imBereich(x, y, z)) {
            return;
        }
        b.setze(x, y, z, Blocks.SPAWNER);
        if (level.getBlockEntity(b.pos(x, y, z)) instanceof SpawnerBlockEntity s) {
            s.setEntityId(typ, level.getRandom());
        }
    }

    /** Lokale Position der Reaktormitte (für den Boss). */
    public static int[] reaktorMitte() {
        return new int[]{24, U + 1, 41};
    }
}
