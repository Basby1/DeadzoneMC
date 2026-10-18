package de.deadzonemc.zuflucht;

import de.deadzonemc.mensch.Auftraggeber;
import de.deadzonemc.mensch.Bewohner;
import de.deadzonemc.mensch.Haendler;
import de.deadzonemc.mensch.Mensch;
import de.deadzonemc.mensch.MenschTypen;
import de.deadzonemc.mensch.Wache;
import de.deadzonemc.welt.bau.Baukasten;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.function.Consumer;

/** Baut eine Zuflucht in einen 64×64-Baukasten. Das Tor liegt vorne (lz = 0), die Mitte bei 31/31. */
public final class ZufluchtBau {

    public static final String[] NAMEN = {"Morgenrot", "Letzte Hoffnung", "Fort Asche", "Camp Eiche", "Neue Heimat",
            "Bastion Sieben", "Lichtblick", "Graufeld", "Rabenstein", "Zum Anker", "Funkenflug", "Haldenblick"};
    private static final String[] VORNAMEN = {"Greta", "Jonas", "Mila", "Henrik", "Ida", "Paul", "Lotte", "Emil", "Frieda",
            "Karl", "Nora", "Theo", "Hanne", "Bruno", "Ella", "Oskar"};

    private static final Direction V = Baukasten.V;
    private static final Direction H = Baukasten.H;
    private static final Direction R = Baukasten.R;
    private static final Direction L = Baukasten.L;

    private ZufluchtBau() {
    }

    public static String name(long saat) {
        return NAMEN[(int) Math.floorMod(saat, (long) NAMEN.length)];
    }

    public static void bauen(Baukasten b, WorldGenLevel level, BoundingBox bereich, long saat, boolean stadt) {
        gelaende(b, stadt);
        mauer(b);
        tor(b, saat);
        for (int[] e : new int[][]{{1, 1}, {58, 1}, {1, 58}, {58, 58}}) {
            turm(b, e[0], e[1]);
        }
        wege(b);
        platz(b);
        markt(b);
        buero(b);
        sanitaet(b);
        zelte(b);
        halle(b);
        feld(b);
        brunnen(b, 54, 22);
        bewohnerSetzen(b, level, bereich, saat);
    }

    // ---- Gelände ------------------------------------------------------------------------------

    private static void gelaende(Baukasten b, boolean stadt) {
        // In der Stadt: Ruinen bis weit nach oben abtragen (die Mauer steht dann zwischen den Hochhäusern)
        b.boden(0, 0, 63, 63, Blocks.DIRT.defaultBlockState(), stadt ? 48 : 14);
        for (int x = 0; x < 64; x++) {
            for (int z = 0; z < 64; z++) {
                double r = b.zufall(x, 0, z);
                b.setze(x, 0, z, r < 0.55 ? Blocks.GRASS_BLOCK : r < 0.8 ? Blocks.COARSE_DIRT : r < 0.93 ? Blocks.PODZOL : Blocks.GRAVEL);
            }
        }
    }

    // ---- Mauer, Tor, Türme --------------------------------------------------------------------

    private static boolean torLuecke(int x, int z) {
        return z == 1 && x >= 30 && x <= 33;
    }

    private static void mauer(Baukasten b) {
        for (int i = 1; i <= 62; i++) {
            mauerstueck(b, i, 1, true);
            mauerstueck(b, i, 62, true);
            mauerstueck(b, 1, i, false);
            mauerstueck(b, 62, i, false);
        }
    }

    private static void mauerstueck(Baukasten b, int x, int z, boolean entlangX) {
        if (torLuecke(x, z)) {
            return;
        }
        int abschnitt = (entlangX ? x : z) / 7 + (entlangX ? z : x);
        boolean blech = abschnitt % 3 == 1;
        BlockState stamm = Blocks.SPRUCE_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
        BlockState wand = blech ? Blocks.WAXED_WEATHERED_CUT_COPPER.defaultBlockState()
                : ((entlangX ? x : z) % 2 == 0 ? stamm : Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState());
        b.setze(x, 1, z, Blocks.SANDSTONE_WALL.defaultBlockState());
        for (int y = 1; y <= 4; y++) {
            b.setze(x, y, z, y == 1 ? Blocks.MUD_BRICKS.defaultBlockState() : wand);
        }
        if (!blech && b.chance(x, 3, z, 0.12)) {
            b.setze(x, 3, z, b.scheibe(Blocks.IRON_BARS, entlangX)); // Schießscharte
        }
        if ((entlangX ? x : z) % 2 == 0) {
            b.setze(x, 5, z, b.zaun(Blocks.SPRUCE_FENCE, entlangX)); // Spitzen
        }
        if (b.chance(x, 0, z, 0.08)) {
            b.setze(x, 1, z + (z == 1 ? -1 : z == 62 ? 1 : 0), Blocks.COBWEB); // Stacheldraht davor
        }
    }

    private static void tor(Baukasten b, long saat) {
        for (int x = 29; x <= 34; x++) {
            b.luft(x, 1, 0, x, 4, 2);
        }
        BlockState balken = Blocks.STRIPPED_DARK_OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X);
        for (int x = 29; x <= 34; x++) {
            b.setze(x, 5, 1, balken);
        }
        for (int y = 1; y <= 5; y++) {
            b.setze(29, y, 1, Blocks.DARK_OAK_LOG);
            b.setze(34, y, 1, Blocks.DARK_OAK_LOG);
        }
        for (int x = 30; x <= 33; x++) {
            b.setze(x, 1, 1, Blocks.SPRUCE_FENCE_GATE.defaultBlockState().setValue(FenceGateBlock.FACING, V));
        }
        b.schild(31, 4, 0, V, Blocks.DARK_OAK_WALL_SIGN, DyeColor.WHITE, "ZUFLUCHT", name(saat), "Waffen runter.", "Keine Gebissenen!");
        b.schild(32, 4, 0, V, Blocks.DARK_OAK_WALL_SIGN, DyeColor.YELLOW, "Handel", "Aufträge", "Schlafplätze", "Arzt");
        laterne(b, 28, 5, 0);
        laterne(b, 35, 5, 0);
        b.setze(28, 1, 0, Blocks.HAY_BLOCK);
        b.setze(35, 1, 0, Blocks.BARREL);
    }

    private static void turm(Baukasten b, int x0, int z0) {
        for (int dx = 0; dx < 5; dx++) {
            for (int dz = 0; dz < 5; dz++) {
                boolean ecke = (dx == 0 || dx == 4) && (dz == 0 || dz == 4);
                for (int y = 1; y <= 9; y++) {
                    if (ecke) {
                        b.setze(x0 + dx, y, z0 + dz, Blocks.SPRUCE_LOG);
                    } else if (y <= 6 && (dx == 0 || dx == 4 || dz == 0 || dz == 4)) {
                        b.setze(x0 + dx, y, z0 + dz, y <= 2 ? Blocks.MUD_BRICKS.defaultBlockState()
                                : Blocks.SPRUCE_PLANKS.defaultBlockState());
                    } else if (y <= 6) {
                        b.setze(x0 + dx, y, z0 + dz, Blocks.AIR);
                    }
                }
                if (!ecke) {
                    b.setze(x0 + dx, 7, z0 + dz, Blocks.SPRUCE_PLANKS);
                    if (dx == 0 || dx == 4 || dz == 0 || dz == 4) {
                        b.setze(x0 + dx, 8, z0 + dz, b.zaun(Blocks.SPRUCE_FENCE, dz == 0 || dz == 4));
                    }
                }
                b.setze(x0 + dx, 10, z0 + dz, Blocks.SPRUCE_SLAB.defaultBlockState());
            }
        }
        b.leiter(x0 + 2, 1, 6, z0 + 3, V);
        b.setze(x0 + 2, 1, z0 + (z0 < 30 ? 4 : 0), Blocks.AIR);
        b.setze(x0 + 2, 2, z0 + (z0 < 30 ? 4 : 0), Blocks.AIR);
        b.leiter(x0 + 2, 7, 7, z0 + 3, V);
        laterne(b, x0 + 1, 9, z0 + 1, true);
        laterne(b, x0 + 3, 9, z0 + 3, true);
        b.setze(x0 + 3, 8, z0 + 1, Blocks.BARREL);
    }

    private static void laterne(Baukasten b, int x, int y, int z) {
        laterne(b, x, y, z, false);
    }

    private static void laterne(Baukasten b, int x, int y, int z, boolean haengend) {
        b.setze(x, y, z, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, haengend));
    }

    private static void laternenpfahl(Baukasten b, int x, int z) {
        for (int y = 1; y <= 3; y++) {
            b.setze(x, y, z, Blocks.SPRUCE_FENCE);
        }
        laterne(b, x, 4, z);
    }

    // ---- Wege und Platz -----------------------------------------------------------------------

    private static void wege(Baukasten b) {
        for (int z = 0; z <= 58; z++) {
            for (int x = 30; x <= 33; x++) {
                b.setze(x, 0, z, b.chance(x, 0, z, 0.3) ? Blocks.GRAVEL : Blocks.DIRT_PATH);
            }
        }
        for (int x = 4; x <= 59; x++) {
            for (int z = 30; z <= 33; z++) {
                b.setze(x, 0, z, b.chance(x, 0, z, 0.3) ? Blocks.GRAVEL : Blocks.DIRT_PATH);
            }
        }
        for (int i = 6; i <= 56; i += 10) {
            laternenpfahl(b, 29, i);
            laternenpfahl(b, i, 29);
        }
    }

    private static void platz(Baukasten b) {
        for (int x = 26; x <= 37; x++) {
            for (int z = 26; z <= 37; z++) {
                double r = b.zufall(x, 0, z);
                b.setze(x, 0, z, r < 0.5 ? Blocks.COBBLESTONE : r < 0.8 ? Blocks.MOSSY_COBBLESTONE : Blocks.GRAVEL);
            }
        }
        b.setze(31, 0, 31, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true));
        b.setze(31, 1, 31, Blocks.AIR);
        b.setze(31, 0, 31, Blocks.STONE_BRICKS);
        b.setze(31, 1, 31, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true));
        BlockState bank = Blocks.SPRUCE_STAIRS.defaultBlockState();
        for (int i = 29; i <= 33; i++) {
            b.setze(i, 1, 28, bank.setValue(StairBlock.FACING, V));
            b.setze(i, 1, 34, bank.setValue(StairBlock.FACING, H));
        }
        for (int i = 29; i <= 33; i++) {
            b.setze(28, 1, i, bank.setValue(StairBlock.FACING, L));
            b.setze(34, 1, i, bank.setValue(StairBlock.FACING, R));
        }
        for (int[] e : new int[][]{{26, 26}, {37, 26}, {26, 37}, {37, 37}}) {
            laternenpfahl(b, e[0], e[1]);
        }
    }

    // ---- Gebäude ------------------------------------------------------------------------------

    /** Ein Marktstand mit Stoffdach. Der Händler steht hinter der Theke (lz0 + 2). */
    private static void stand(Baukasten b, int x0, int z0, Block dach, String beschriftung) {
        for (int[] e : new int[][]{{0, 0}, {4, 0}, {0, 3}, {4, 3}}) {
            for (int y = 1; y <= 3; y++) {
                b.setze(x0 + e[0], y, z0 + e[1], Blocks.SPRUCE_FENCE);
            }
        }
        for (int x = 0; x <= 4; x++) {
            for (int z = 0; z <= 3; z++) {
                b.setze(x0 + x, 4, z0 + z, dach);
            }
            b.setze(x0 + x, 1, z0, x == 2 ? Blocks.AIR.defaultBlockState() : Blocks.BARREL.defaultBlockState());
        }
        b.setze(x0 + 2, 1, z0, Blocks.SPRUCE_TRAPDOOR.defaultBlockState().setValue(TrapDoorBlock.HALF, Half.TOP));
        b.setze(x0 + 1, 1, z0 + 3, Blocks.CHEST);
        b.setze(x0 + 3, 1, z0 + 3, Blocks.CRAFTING_TABLE);
        b.schild(x0 + 2, 3, z0 - 1, V, Blocks.SPRUCE_WALL_SIGN, DyeColor.WHITE, beschriftung);
        laterne(b, x0 + 2, 3, z0 + 1, true);
    }

    private static void markt(Baukasten b) {
        stand(b, 8, 8, Blocks.RED_WOOL, "Versorger");
        stand(b, 16, 8, Blocks.GREEN_WOOL, "Waffen");
        stand(b, 8, 17, Blocks.BLUE_WOOL, "Technik");
        b.setze(15, 1, 18, Blocks.HAY_BLOCK);
        b.setze(16, 1, 19, Blocks.BARREL);
        b.setze(17, 1, 18, Blocks.COMPOSTER);
        b.setze(20, 1, 20, Blocks.CAULDRON);
    }

    private static void haus(Baukasten b, int x1, int z1, int x2, int z2, int hoehe, Block wand, Block boden) {
        b.fuelle(x1, 0, z1, x2, 0, z2, boden.defaultBlockState());
        b.waende(x1, z1, x2, z2, 1, hoehe, wand.defaultBlockState());
        b.luft(x1 + 1, 1, z1 + 1, x2 - 1, hoehe, z2 - 1);
        for (int y = 1; y <= hoehe; y++) {
            for (int[] e : new int[][]{{x1, z1}, {x2, z1}, {x1, z2}, {x2, z2}}) {
                b.setze(e[0], y, e[1], Blocks.STRIPPED_SPRUCE_LOG);
            }
        }
        b.fuelle(x1 - 1, hoehe + 1, z1 - 1, x2 + 1, hoehe + 1, z2 + 1,
                Blocks.SPRUCE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM));
        for (int x = x1 + 2; x <= x2 - 2; x += 3) {
            b.setze(x, 2, z1, b.scheibe(Blocks.GLASS_PANE, true));
            b.setze(x, 2, z2, b.scheibe(Blocks.GLASS_PANE, true));
        }
    }

    private static void buero(Baukasten b) {
        haus(b, 38, 6, 49, 15, 4, Blocks.SPRUCE_PLANKS, Blocks.SPRUCE_PLANKS);
        b.tuer(38, 1, 11, L, Blocks.SPRUCE_DOOR, false);
        b.luft(38, 1, 11, 38, 2, 11);
        b.tuer(38, 1, 11, L, Blocks.SPRUCE_DOOR, false);
        b.setze(44, 1, 13, Blocks.LECTERN);
        b.setze(46, 1, 13, Blocks.CARTOGRAPHY_TABLE);
        b.setze(47, 1, 13, Blocks.BARREL);
        b.setze(42, 1, 8, Blocks.SPRUCE_FENCE);
        b.setze(42, 2, 8, Blocks.SPRUCE_PRESSURE_PLATE);
        b.setze(41, 1, 8, Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, R));
        b.setze(48, 1, 7, Blocks.CHEST);
        laterne(b, 43, 4, 10, true);
        laterne(b, 46, 4, 12, true);
        b.schild(37, 3, 11, L, Blocks.SPRUCE_WALL_SIGN, DyeColor.YELLOW, "AUFTRÄGE", "Arbeit gegen", "Patronen");
    }

    private static void sanitaet(Baukasten b) {
        for (int x = 52; x <= 59; x++) {
            for (int z = 6; z <= 14; z++) {
                b.setze(x, 0, z, Blocks.WHITE_CONCRETE_POWDER);
                int seite = Math.min(x - 52, 59 - x);
                int h = Math.min(4, seite + 1);
                b.setze(x, h, z, Blocks.WHITE_WOOL);
                if (z == 6 || z == 14) {
                    for (int y = 1; y < h; y++) {
                        b.setze(x, y, z, (z == 6 && (x == 55 || x == 56)) ? Blocks.AIR : Blocks.WHITE_WOOL);
                    }
                }
            }
        }
        b.setze(55, 3, 5, Blocks.RED_WOOL);
        b.setze(56, 3, 5, Blocks.RED_WOOL);
        b.bett(53, 1, 12, H, Blocks.WHITE_BED);
        b.bett(58, 1, 12, H, Blocks.WHITE_BED);
        b.setze(54, 1, 8, Blocks.BREWING_STAND);
        b.setze(57, 1, 8, Blocks.CAULDRON);
        b.setze(58, 1, 8, Blocks.BARREL);
        laterne(b, 55, 3, 10, true);
    }

    private static void zelt(Baukasten b, int x0, int z0, Block stoff, Direction kopf) {
        for (int x = 0; x <= 4; x++) {
            int h = Math.min(x, 4 - x) + 1;
            for (int z = 0; z <= 5; z++) {
                b.setze(x0 + x, h, z0 + z, stoff);
                if (z == 5) {
                    for (int y = 1; y < h; y++) {
                        b.setze(x0 + x, y, z0 + z, stoff);
                    }
                }
            }
        }
        b.bett(x0 + 1, 1, z0 + 4, kopf, Blocks.BROWN_BED);
        b.setze(x0 + 3, 1, z0 + 4, Blocks.BARREL);
        laterne(b, x0 + 2, 1, z0 + 1);
    }

    private static void zelte(Baukasten b) {
        zelt(b, 6, 40, Blocks.GREEN_WOOL, H);
        zelt(b, 13, 40, Blocks.BROWN_WOOL, H);
        zelt(b, 20, 40, Blocks.GREEN_WOOL, H);
        zelt(b, 6, 50, Blocks.BROWN_WOOL, H);
        zelt(b, 13, 50, Blocks.LIGHT_GRAY_WOOL, H);
        zelt(b, 20, 50, Blocks.GREEN_WOOL, H);
        b.setze(16, 0, 47, Blocks.STONE_BRICKS);
        b.setze(16, 1, 47, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true));
    }

    private static void halle(Baukasten b) {
        haus(b, 38, 40, 57, 54, 5, Blocks.SPRUCE_PLANKS, Blocks.SPRUCE_PLANKS);
        b.fuelle(38, 1, 40, 57, 1, 40, Blocks.MUD_BRICKS.defaultBlockState());
        b.tuer(46, 1, 40, V, Blocks.SPRUCE_DOOR, false);
        b.tuer(47, 1, 40, V, Blocks.SPRUCE_DOOR, false);
        b.schild(45, 3, 39, V, Blocks.SPRUCE_WALL_SIGN, DyeColor.WHITE, "HALLE", "Essen · Schlafen", "Ruhe bewahren");
        for (int x = 41; x <= 54; x += 4) {
            b.setze(x, 1, 45, Blocks.SPRUCE_FENCE);
            b.setze(x, 2, 45, Blocks.SPRUCE_PRESSURE_PLATE);
            b.setze(x + 1, 1, 45, Blocks.SPRUCE_FENCE);
            b.setze(x + 1, 2, 45, Blocks.SPRUCE_PRESSURE_PLATE);
            b.setze(x, 1, 44, Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, V));
            b.setze(x + 1, 1, 46, Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, H));
        }
        for (int x = 40; x <= 55; x += 3) {
            b.bett(x, 1, 52, H, Blocks.GRAY_BED);
        }
        b.setze(39, 1, 43, Blocks.FURNACE);
        b.setze(39, 1, 44, Blocks.SMOKER);
        b.setze(39, 1, 45, Blocks.CRAFTING_TABLE);
        b.setze(56, 1, 43, Blocks.CHEST);
        b.setze(56, 1, 44, Blocks.CHEST);
        b.setze(56, 1, 46, Blocks.BARREL);
        for (int x = 41; x <= 54; x += 4) {
            laterne(b, x, 5, 47, true);
        }
    }

    private static void feld(Baukasten b) {
        for (int x = 5; x <= 20; x++) {
            for (int z = 23; z <= 27; z++) {
                if (z == 25) {
                    b.setze(x, 0, z, Blocks.WATER);
                    continue;
                }
                b.setze(x, 0, z, Blocks.FARMLAND);
                Block frucht = x < 13 ? Blocks.WHEAT : Blocks.POTATOES;
                b.setze(x, 1, z, frucht.defaultBlockState().setValue(CropBlock.AGE, (int) (b.zufall(x, 1, z) * 8)));
            }
        }
        for (int x = 4; x <= 21; x++) {
            b.setze(x, 1, 22, b.zaun(Blocks.OAK_FENCE, true));
            b.setze(x, 1, 28, b.zaun(Blocks.OAK_FENCE, true));
        }
        b.setze(21, 1, 25, Blocks.COMPOSTER);
        // Vogelscheuche
        b.setze(12, 1, 24, Blocks.OAK_FENCE);
        b.setze(12, 2, 24, Blocks.OAK_FENCE);
        b.setze(12, 3, 24, Blocks.CARVED_PUMPKIN);
    }

    private static void brunnen(Baukasten b, int x0, int z0) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                b.setze(x0 + dx, 0, z0 + dz, dx == 0 && dz == 0 ? Blocks.WATER.defaultBlockState() : Blocks.COBBLESTONE.defaultBlockState());
                if (dx != 0 || dz != 0) {
                    b.setze(x0 + dx, 1, z0 + dz, Blocks.COBBLESTONE_WALL);
                }
            }
        }
        b.setze(x0, -1, z0, Blocks.WATER);
        b.setze(x0, -2, z0, Blocks.WATER);
        b.setze(x0 - 1, 2, z0, Blocks.OAK_FENCE);
        b.setze(x0 + 1, 2, z0, Blocks.OAK_FENCE);
        b.fuelle(x0 - 1, 3, z0, x0 + 1, 3, z0, Blocks.OAK_SLAB.defaultBlockState());
        b.setze(x0, 2, z0, Blocks.CHAIN);
    }

    // ---- Bewohner -----------------------------------------------------------------------------

    private static <T extends Mob> void setzen(Baukasten b, WorldGenLevel level, BoundingBox bereich, EntityType<T> typ,
                                               int x, int y, int z, Consumer<T> einrichten) {
        BlockPos pos = b.pos(x, y, z);
        if (!bereich.isInside(pos)) {
            return;
        }
        T e = typ.create(level.getLevel());
        if (e == null) {
            return;
        }
        e.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, level.getRandom().nextFloat() * 360, 0);
        e.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.STRUCTURE, null);
        einrichten.accept(e);
        level.addFreshEntity(e);
    }

    private static void benennen(Mensch m, long saat, int nr) {
        m.setCustomName(Component.literal(VORNAMEN[(int) Math.floorMod(saat + nr * 7L, (long) VORNAMEN.length)]));
    }

    private static void bewohnerSetzen(Baukasten b, WorldGenLevel level, BoundingBox bereich, long saat) {
        BlockPos mitte = b.pos(31, 1, 31);
        // Wachen: auf den Türmen und am Tor
        int[][] wachen = {{3, 8, 3}, {60, 8, 3}, {3, 8, 60}, {60, 8, 60}, {28, 1, 3}, {35, 1, 3}};
        for (int i = 0; i < wachen.length; i++) {
            int[] w = wachen[i];
            setzen(b, level, bereich, MenschTypen.WACHE.get(), w[0], w[1], w[2],
                    (Wache e) -> e.heimatSetzen(b.pos(w[0], w[1], w[2]), w[1] > 1 ? 1 : 5));
        }
        // Händler an ihren Ständen, Arzt im Sanitätszelt
        Object[][] haendler = {{10, 10, Sortimente.VERSORGER}, {18, 10, Sortimente.WAFFEN}, {10, 19, Sortimente.TECHNIK},
                {55, 10, Sortimente.ARZT}};
        for (Object[] h : haendler) {
            int x = (int) h[0];
            int z = (int) h[1];
            setzen(b, level, bereich, MenschTypen.HAENDLER.get(), x, 1, z,
                    (Haendler e) -> e.zufluchtSetzen((String) h[2], b.pos(x, 1, z)));
        }
        // Auftraggeber im Büro
        setzen(b, level, bereich, MenschTypen.AUFTRAGGEBER.get(), 43, 1, 11, (Auftraggeber e) -> {
            e.heimatSetzen(b.pos(43, 1, 11), 3);
            e.setCustomName(Component.literal("Verwalter " + VORNAMEN[(int) Math.floorMod(saat, (long) VORNAMEN.length)]));
        });
        // Bewohner rund um Platz, Zelte und Halle
        int[][] leute = {{30, 1, 27}, {35, 1, 33}, {16, 1, 45}, {47, 1, 48}, {44, 1, 32}, {12, 1, 38}};
        for (int i = 0; i < leute.length; i++) {
            int[] l = leute[i];
            int nr = i;
            setzen(b, level, bereich, MenschTypen.BEWOHNER.get(), l[0], l[1], l[2], (Bewohner e) -> {
                e.heimatSetzen(mitte, 24);
                benennen(e, saat, nr);
            });
        }
    }
}
