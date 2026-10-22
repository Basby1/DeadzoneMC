package de.deadzonemc.welt.bau;

import de.deadzonemc.welt.Bau;
import net.minecraft.core.Direction;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RailBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.Random;

/**
 * Fahrzeugwracks. Alle werden im Baukasten entworfen: lz = Längsrichtung (0 = Front), lx = Breite, ly = 0 steht
 * auf dem Boden. In Entwurfsrichtungen zeigt +lx nach WEST, +lz nach SÜD.
 *
 * <p>Personenwagen sind 5 lang, 3 breit und höchstens 3 hoch (passen in jede Garage):</p>
 * <pre>
 *   Limousine       Kombi           Kleinwagen      Pickup          Lieferwagen
 *    . ▄ ▄ ▄ .       . ▄ ▄ ▄ ▄       . ◢ █ ◣ .       . ▄ ▄ . .       . ◢ █ █ █
 *    ▄ ╪ ░ █ ▄       ▄ ╪ ░ ░ ░       ▄ ╪ ░ ░ ▄       ▄ ╪ ░ ▄ ▄       ◢ ╪ ░ █ █
 *    o ● █ ● r       o ● █ ● r       o ● █ ● r       o ● █ ● r       o ● █ ● r
 * </pre>
 * <p>o = Scheinwerfer, r = Rücklicht, ● = Rad, ╪ = Frontscheibe, ░ = Seitenscheibe, ▄ = Stufe, ◢ = Treppe.</p>
 */
public final class Fahrzeuge {

    private Fahrzeuge() {
    }

    /** Lackfarbe: voller Block, Stufe und Treppe aus demselben Material. */
    private record Lack(Block voll, Block stufe, Block treppe) {
    }

    private static final Lack[] LACKE = {
            new Lack(Blocks.POLISHED_ANDESITE, Blocks.POLISHED_ANDESITE_SLAB, Blocks.POLISHED_ANDESITE_STAIRS),
            new Lack(Blocks.SMOOTH_QUARTZ, Blocks.SMOOTH_QUARTZ_SLAB, Blocks.SMOOTH_QUARTZ_STAIRS),
            new Lack(Blocks.RED_NETHER_BRICKS, Blocks.RED_NETHER_BRICK_SLAB, Blocks.RED_NETHER_BRICK_STAIRS),
            new Lack(Blocks.POLISHED_DEEPSLATE, Blocks.POLISHED_DEEPSLATE_SLAB, Blocks.POLISHED_DEEPSLATE_STAIRS),
            new Lack(Blocks.EXPOSED_CUT_COPPER, Blocks.EXPOSED_CUT_COPPER_SLAB, Blocks.EXPOSED_CUT_COPPER_STAIRS),
            new Lack(Blocks.WEATHERED_CUT_COPPER, Blocks.WEATHERED_CUT_COPPER_SLAB, Blocks.WEATHERED_CUT_COPPER_STAIRS),
            new Lack(Blocks.OXIDIZED_CUT_COPPER, Blocks.OXIDIZED_CUT_COPPER_SLAB, Blocks.OXIDIZED_CUT_COPPER_STAIRS),
            new Lack(Blocks.PRISMARINE_BRICKS, Blocks.PRISMARINE_BRICK_SLAB, Blocks.PRISMARINE_BRICK_STAIRS),
            new Lack(Blocks.DARK_PRISMARINE, Blocks.DARK_PRISMARINE_SLAB, Blocks.DARK_PRISMARINE_STAIRS),
            new Lack(Blocks.SMOOTH_SANDSTONE, Blocks.SMOOTH_SANDSTONE_SLAB, Blocks.SMOOTH_SANDSTONE_STAIRS),
            new Lack(Blocks.SMOOTH_RED_SANDSTONE, Blocks.SMOOTH_RED_SANDSTONE_SLAB, Blocks.SMOOTH_RED_SANDSTONE_STAIRS),
            new Lack(Blocks.POLISHED_TUFF, Blocks.POLISHED_TUFF_SLAB, Blocks.POLISHED_TUFF_STAIRS),
    };
    private static final Lack WEISS = LACKE[1];
    private static final Lack BRAND = new Lack(Blocks.POLISHED_BLACKSTONE, Blocks.POLISHED_BLACKSTONE_SLAB,
            Blocks.POLISHED_BLACKSTONE_STAIRS);
    private static final Block[] GLAS = {Blocks.GRAY_STAINED_GLASS_PANE, Blocks.BLACK_STAINED_GLASS_PANE,
            Blocks.LIGHT_GRAY_STAINED_GLASS_PANE, Blocks.GLASS_PANE};
    private static final Block[] STREIFEN = {Blocks.RED_CONCRETE, Blocks.BLUE_CONCRETE, Blocks.YELLOW_CONCRETE,
            Blocks.ORANGE_CONCRETE, Blocks.GREEN_CONCRETE, Blocks.CYAN_CONCRETE};

    private enum Art {LIMOUSINE, KOMBI, KLEINWAGEN, PICKUP, LIEFERWAGEN, POLIZEI, KRANKENWAGEN}

    /** Baukasten für ein Fahrzeug mit der Ecke (minX, minZ), Länge entlang X oder Z. */
    public static Baukasten kasten(WorldGenLevel level, BoundingBox bereich, int minX, int y, int minZ,
                                   boolean entlangX, int laenge, int breite, long saat) {
        if (entlangX) {
            return new Baukasten(level, bereich, minX, minZ, minX + laenge - 1, minZ + breite - 1, y, Direction.WEST, saat);
        }
        return new Baukasten(level, bereich, minX, minZ, minX + breite - 1, minZ + laenge - 1, y, Direction.NORTH, saat);
    }

    /** Ein Personenwagen (Garage, Einfahrt, Werkstatt, Landstraße): 5 lang, 3 breit, höchstens 3 hoch. */
    public static void auto(Baukasten b, long saat, boolean ausgebrannt) {
        Random r = new Random(saat);
        Art[] arten = {Art.LIMOUSINE, Art.LIMOUSINE, Art.KOMBI, Art.KOMBI, Art.KLEINWAGEN, Art.KLEINWAGEN, Art.PICKUP,
                Art.LIEFERWAGEN};
        bauen(b, r, arten[r.nextInt(arten.length)], ausgebrannt, false);
    }

    /** Ein Wrack auf der Straße einer Stadt: wie {@link #auto}, dazu Streifenwagen und Krankenwagen. */
    public static void strassenwrack(Baukasten b, long saat, boolean ausgebrannt) {
        Random r = new Random(saat);
        Art[] arten = {Art.LIMOUSINE, Art.LIMOUSINE, Art.KOMBI, Art.KOMBI, Art.KLEINWAGEN, Art.PICKUP, Art.LIEFERWAGEN,
                Art.LIEFERWAGEN, Art.POLIZEI, Art.KRANKENWAGEN};
        bauen(b, r, arten[r.nextInt(arten.length)], ausgebrannt, true);
    }

    /** Für die Fahrzeugschau im Selbsttest: jede Art einmal. */
    public static int anzahlArten() {
        return Art.values().length;
    }

    public static void testAuto(Baukasten b, int art, boolean ausgebrannt) {
        bauen(b, new Random(b.saat), Art.values()[art], ausgebrannt, true);
    }

    // ---- Werkzeug -------------------------------------------------------------------------------

    /** Schreibt in einen Baukasten, wahlweise mit vertauschtem Vorne/Hinten (Front mal vorne, mal hinten). */
    private record Wagen(Baukasten b, int laenge, boolean umgedreht) {
        void setze(int lx, int ly, int lz, BlockState s) {
            b.setze(lx, ly, umgedreht ? laenge - 1 - lz : lz, umgedreht ? s.mirror(Mirror.LEFT_RIGHT) : s);
        }

        void setze(int lx, int ly, int lz, Block s) {
            setze(lx, ly, lz, s.defaultBlockState());
        }

        void fass(int lx, int ly, int lz, Direction blick, String beute) {
            Direction d = umgedreht && blick.getAxis() == Direction.Axis.Z ? blick.getOpposite() : blick;
            b.fass(lx, ly, umgedreht ? laenge - 1 - lz : lz, d, beute);
        }
    }

    private static BlockState stufe(Block b) {
        return b.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM);
    }

    private static BlockState treppe(Block b, Direction hoch, boolean kopfueber) {
        return b.defaultBlockState().setValue(StairBlock.FACING, hoch).setValue(StairBlock.HALF, kopfueber ? Half.TOP : Half.BOTTOM);
    }

    /** Scheibe mit Verbindungen in Entwurfsrichtungen: v = zur Front (lz-1), h = nach hinten, r = lx+1, l = lx-1. */
    private static BlockState scheibe(Block glas, boolean v, boolean h, boolean r, boolean l) {
        return glas.defaultBlockState().setValue(CrossCollisionBlock.NORTH, v).setValue(CrossCollisionBlock.SOUTH, h)
                .setValue(CrossCollisionBlock.WEST, r).setValue(CrossCollisionBlock.EAST, l);
    }

    private static BlockState luke(Block b, Direction scharnier, boolean offen) {
        return b.defaultBlockState().setValue(TrapDoorBlock.FACING, scharnier).setValue(TrapDoorBlock.OPEN, offen)
                .setValue(TrapDoorBlock.HALF, Half.TOP);
    }

    // ---- Personenwagen --------------------------------------------------------------------------

    private static void bauen(Baukasten b, Random r, Art art, boolean brand, boolean stadt) {
        Lack lack = brand ? BRAND
                : switch (art) {
            case POLIZEI, KRANKENWAGEN -> WEISS;
            case LIEFERWAGEN -> r.nextInt(3) > 0 ? WEISS : LACKE[r.nextInt(LACKE.length)];
            default -> LACKE[r.nextInt(LACKE.length)];
        };
        Wagen w = new Wagen(b, 5, r.nextBoolean());
        BlockState voll = lack.voll().defaultBlockState();
        BlockState flach = stufe(lack.stufe());
        Block glas = GLAS[r.nextInt(GLAS.length)];
        // Schäden: zerbrochene Scheiben, offene Tür, platter Reifen, eingedrücktes Dach
        double bruch = brand ? 1.0 : 0.15 + r.nextDouble() * 0.35;
        int offeneTuer = brand || r.nextInt(4) > 0 ? -1 : r.nextInt(2) * 2;
        int platt = brand ? -1 : r.nextInt(6);

        // ---- Unterbau: Scheinwerfer, Kühlergrill, Räder, Schweller, Rücklichter, Stoßstange ----
        for (int lz = 0; lz < 5; lz++) {
            for (int lx = 0; lx < 3; lx++) {
                boolean rad = (lz == 1 || lz == 3) && lx != 1;
                BlockState s;
                if (rad) {
                    int nr = (lz == 1 ? 0 : 2) + (lx == 0 ? 0 : 1);
                    s = brand ? stufe(Blocks.BLACKSTONE_SLAB) // auf den Felgen
                            : nr == platt ? stufe(Blocks.POLISHED_BLACKSTONE_SLAB) : Blocks.BLACK_CONCRETE.defaultBlockState();
                } else if (lz == 0) {
                    s = lx == 1 ? (brand ? voll : scheibe(Blocks.IRON_BARS, false, false, true, true))
                            : brand ? Blocks.BLACKSTONE.defaultBlockState()
                            : (r.nextInt(5) == 0 ? Blocks.LIGHT_GRAY_STAINED_GLASS : Blocks.WHITE_STAINED_GLASS).defaultBlockState();
                } else if (lz == 4) {
                    s = lx == 1 ? voll : brand ? Blocks.BLACKSTONE.defaultBlockState() : Blocks.RED_STAINED_GLASS.defaultBlockState();
                } else if (lz == 2 && lx != 1 && art == Art.POLIZEI && !brand) {
                    s = Blocks.BLUE_CONCRETE.defaultBlockState(); // blaue Türen
                } else {
                    s = voll;
                }
                w.setze(lx, 0, lz, s);
            }
        }
        BlockState sitz = brand ? (r.nextBoolean() ? Blocks.IRON_BARS.defaultBlockState() : Blocks.COBWEB.defaultBlockState())
                : treppe(Blocks.POLISHED_BLACKSTONE_STAIRS, Baukasten.H, false);

        switch (art) {
            case LIMOUSINE, KOMBI, KLEINWAGEN, POLIZEI -> {
                boolean kombi = art == Art.KOMBI;
                boolean klein = art == Art.KLEINWAGEN;
                haube(w, flach);
                frontscheibe(w, glas, bruch, r);
                seitenscheiben(w, glas, bruch, r, 2, offeneTuer);
                if (art == Art.LIMOUSINE || art == Art.POLIZEI) {
                    BlockState saeule = art == Art.POLIZEI && !brand ? Blocks.BLUE_CONCRETE.defaultBlockState() : voll;
                    w.setze(0, 1, 3, saeule);
                    w.setze(2, 1, 3, saeule);
                } else {
                    seitenscheiben(w, glas, bruch, r, 3, -1);
                }
                // Rücksitz, manchmal mit etwas darauf
                if (r.nextInt(5) < 2) {
                    w.fass(1, 1, 3, Baukasten.H, art == Art.POLIZEI ? "polizei" : "autowrack");
                } else {
                    w.setze(1, 1, 3, sitz);
                }
                w.setze(1, 1, 2, sitz);
                // Heck: Kofferraumdeckel oder Heckscheibe
                if (kombi) {
                    for (int lx = 0; lx < 3; lx++) {
                        w.setze(lx, 1, 4, r.nextDouble() < bruch ? Bau.LUFT
                                : scheibe(glas, lx != 1, false, lx < 2, lx > 0));
                    }
                } else {
                    for (int lx = 0; lx < 3; lx++) {
                        w.setze(lx, 1, 4, flach);
                    }
                }
                // Dach
                for (int lx = 0; lx < 3; lx++) {
                    if (klein) {
                        w.setze(lx, 2, 1, treppe(lack.treppe(), Baukasten.H, false));
                        w.setze(lx, 2, 2, voll);
                        w.setze(lx, 2, 3, treppe(lack.treppe(), Baukasten.V, false));
                    } else {
                        for (int lz = 1; lz <= (kombi ? 4 : 3); lz++) {
                            boolean loch = brand ? r.nextInt(3) == 0 : lz == 2 && lx == 1 && r.nextInt(8) == 0;
                            w.setze(lx, 2, lz, loch ? Bau.LUFT : flach);
                        }
                    }
                }
                if (art == Art.POLIZEI && !brand) {
                    // Lichtbalken: zwei blaue Leuchten im Dach
                    w.setze(0, 2, 2, Blocks.BLUE_STAINED_GLASS);
                    w.setze(2, 2, 2, Blocks.BLUE_STAINED_GLASS);
                    w.setze(1, 2, 2, Blocks.SMOOTH_QUARTZ_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP));
                }
            }
            case PICKUP -> {
                haube(w, flach);
                frontscheibe(w, glas, bruch, r);
                seitenscheiben(w, glas, bruch, r, 2, offeneTuer);
                w.setze(1, 1, 2, sitz);
                for (int lx = 0; lx < 3; lx++) {
                    w.setze(lx, 2, 1, flach);
                    w.setze(lx, 2, 2, brand && r.nextInt(3) == 0 ? Bau.LUFT : flach);
                }
                // Ladefläche: niedrige Bordwände, Heckklappe, Ladung
                w.setze(0, 1, 3, flach);
                w.setze(2, 1, 3, flach);
                w.setze(0, 1, 4, flach);
                w.setze(2, 1, 4, flach);
                w.setze(1, 1, 4, r.nextInt(3) == 0 ? Bau.LUFT : luke(Blocks.IRON_TRAPDOOR, Baukasten.H, false));
                int ladung = r.nextInt(4);
                if (ladung == 0) {
                    w.fass(1, 1, 3, Direction.UP, r.nextBoolean() ? "werkzeug" : "baumarkt");
                } else if (ladung == 1) {
                    w.setze(1, 1, 3, Blocks.HAY_BLOCK);
                } else if (ladung == 2) {
                    w.setze(1, 1, 3, Blocks.CHAIN.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z));
                } else {
                    w.fass(1, 1, 3, Direction.UP, "autowrack");
                }
            }
            case LIEFERWAGEN, KRANKENWAGEN -> {
                boolean sani = art == Art.KRANKENWAGEN;
                BlockState streifen = brand ? voll
                        : (sani ? Blocks.RED_CONCRETE : STREIFEN[r.nextInt(STREIFEN.length)]).defaultBlockState();
                // schräge Front mit der Motorhaube in der Mitte
                w.setze(0, 1, 0, treppe(lack.treppe(), Baukasten.H, false));
                w.setze(2, 1, 0, treppe(lack.treppe(), Baukasten.H, false));
                w.setze(1, 1, 0, de.deadzonemc.beute.Beute.MOTORRAUM.get().defaultBlockState());
                frontscheibe(w, glas, bruch, r);
                seitenscheiben(w, glas, bruch, r, 2, offeneTuer);
                w.setze(1, 1, 2, sitz);
                // Laderaum: geschlossene Wände mit Farbstreifen, Hecktür manchmal offen
                for (int lz = 3; lz <= 4; lz++) {
                    w.setze(0, 1, lz, streifen);
                    w.setze(2, 1, lz, streifen);
                }
                w.fass(1, 1, 3, Baukasten.H, sani ? "apotheke" : r.nextBoolean() ? "laden_lager" : "werkstatt");
                w.setze(1, 1, 4, r.nextInt(5) < 2 ? Bau.LUFT : voll);
                for (int lx = 0; lx < 3; lx++) {
                    w.setze(lx, 2, 1, treppe(lack.treppe(), Baukasten.H, false));
                    for (int lz = 2; lz <= 4; lz++) {
                        w.setze(lx, 2, lz, brand && r.nextInt(4) == 0 ? Bau.LUFT : voll);
                    }
                }
                if (sani && !brand && stadt) {
                    // Blaulichtbalken über der Kabine
                    w.setze(0, 3, 2, scheibe(Blocks.BLUE_STAINED_GLASS_PANE, false, false, true, false));
                    w.setze(1, 3, 2, scheibe(Blocks.WHITE_STAINED_GLASS_PANE, false, false, true, true));
                    w.setze(2, 3, 2, scheibe(Blocks.BLUE_STAINED_GLASS_PANE, false, false, false, true));
                }
            }
        }
        if (brand) {
            // Ruß und Spinnweben im ausgebrannten Innenraum
            if (r.nextBoolean()) {
                w.setze(1, 1, 1, Blocks.COBWEB);
            }
        }
    }

    /** Motorhaube: flach, in der Mitte die aufhebelbare Haube (Fahrzeugteile). */
    private static void haube(Wagen w, BlockState flach) {
        w.setze(0, 1, 0, flach);
        w.setze(1, 1, 0, de.deadzonemc.beute.Beute.MOTORRAUM.get().defaultBlockState());
        w.setze(2, 1, 0, flach);
    }

    private static void frontscheibe(Wagen w, Block glas, double bruch, Random r) {
        for (int lx = 0; lx < 3; lx++) {
            boolean kaputt = r.nextDouble() < bruch * 0.8;
            w.setze(lx, 1, 1, kaputt ? Bau.LUFT : scheibe(glas, false, lx != 1, lx < 2, lx > 0));
        }
    }

    private static void seitenscheiben(Wagen w, Block glas, double bruch, Random r, int lz, int offeneTuer) {
        for (int lx = 0; lx <= 2; lx += 2) {
            boolean weg = lx == offeneTuer || r.nextDouble() < bruch;
            w.setze(lx, 1, lz, weg ? Bau.LUFT : scheibe(glas, true, true, false, false));
        }
    }

    // ---- Traktor --------------------------------------------------------------------------------

    /**
     * Ein Traktor: 4 lang, 3 breit. Schmale Motorhaube mit Auspuff, kleine Vorderräder, große Hinterräder (2 hoch)
     * mit Kotflügeln, Sitz und Lenkrad; manchmal mit Schutzdach.
     */
    public static void traktor(Baukasten b, long saat) {
        Random r = new Random(saat);
        boolean gruen = r.nextBoolean();
        BlockState lack = (gruen ? Blocks.GREEN_TERRACOTTA : Blocks.RED_TERRACOTTA).defaultBlockState();
        Block kotfluegel = gruen ? Blocks.MOSSY_COBBLESTONE_SLAB : Blocks.RED_NETHER_BRICK_SLAB;
        Block nase = gruen ? Blocks.MOSSY_COBBLESTONE_STAIRS : Blocks.RED_NETHER_BRICK_STAIRS;
        BlockState rad = Blocks.BLACK_CONCRETE.defaultBlockState();
        BlockState felge = (gruen ? Blocks.YELLOW_TERRACOTTA : Blocks.LIGHT_GRAY_CONCRETE).defaultBlockState();
        // Vorderachse und Motor
        b.setze(0, 0, 0, stufe(Blocks.POLISHED_BLACKSTONE_SLAB));
        b.setze(2, 0, 0, stufe(Blocks.POLISHED_BLACKSTONE_SLAB));
        b.setze(1, 0, 0, scheibe(Blocks.IRON_BARS, false, false, true, true)); // Kühlergrill
        b.setze(1, 1, 0, treppe(nase, Baukasten.H, false));
        b.setze(1, 0, 1, lack);
        b.setze(1, 1, 1, lack);
        b.setze(0, 1, 1, Blocks.LIGHTNING_ROD.defaultBlockState()); // Auspuff
        b.setze(0, 2, 1, Blocks.LIGHTNING_ROD.defaultBlockState());
        b.setze(2, 1, 1, Blocks.WHITE_STAINED_GLASS); // Scheinwerfer seitlich am Motor
        // Hinterräder 2x2 mit heller Felge in der Mitte
        for (int lz = 2; lz <= 3; lz++) {
            for (int lx = 0; lx <= 2; lx += 2) {
                b.setze(lx, 0, lz, rad);
                b.setze(lx, 1, lz, lz == 2 ? felge : rad);
                b.setze(lx, 2, lz, stufe(kotfluegel));
            }
            b.setze(1, 0, lz, lack);
        }
        // Armaturen, Lenkrad, Sitz
        b.setze(1, 1, 2, lack);
        b.setze(1, 2, 2, luke(Blocks.IRON_TRAPDOOR, Baukasten.V, false)); // Lenkrad
        b.setze(1, 1, 3, treppe(Blocks.SPRUCE_STAIRS, Baukasten.H, false));
        if (r.nextBoolean()) {
            // Schutzdach auf zwei Bügeln
            b.setze(0, 3, 3, Blocks.IRON_BARS);
            b.setze(2, 3, 3, Blocks.IRON_BARS);
            for (int lx = 0; lx < 3; lx++) {
                b.setze(lx, 4, 2, stufe(Blocks.SMOOTH_STONE_SLAB));
                b.setze(lx, 4, 3, stufe(Blocks.SMOOTH_STONE_SLAB));
            }
        }
    }

    // ---- Löschfahrzeug --------------------------------------------------------------------------

    /**
     * Rotes Löschfahrzeug: 7 lang, 3 breit, Frontlenker-Kabine mit Blaulicht, Geräteräume mit Rollläden
     * (einer offen), Leiter auf dem Dach, Schlauchhaspel am Heck.
     */
    public static void loeschfahrzeug(Baukasten b, long saat) {
        Random r = new Random(saat);
        BlockState rot = Blocks.RED_CONCRETE.defaultBlockState();
        BlockState weiss = Blocks.WHITE_CONCRETE.defaultBlockState();
        BlockState rollladen = Blocks.IRON_BLOCK.defaultBlockState();
        Block glas = Blocks.GRAY_STAINED_GLASS_PANE;
        for (int lz = 0; lz < 7; lz++) {
            for (int lx = 0; lx < 3; lx++) {
                boolean rad = (lz == 1 || lz == 4 || lz == 5) && lx != 1;
                BlockState s = rad ? Blocks.BLACK_CONCRETE.defaultBlockState() : rot;
                if (lz == 0) {
                    s = lx == 1 ? scheibe(Blocks.IRON_BARS, false, false, true, true) : Blocks.WHITE_STAINED_GLASS.defaultBlockState();
                } else if (lz == 6 && lx != 1) {
                    s = Blocks.RED_STAINED_GLASS.defaultBlockState();
                } else if ((lz == 2 || lz == 3) && lx != 1) {
                    s = weiss; // Trittstufe / Zierstreifen
                }
                b.setze(lx, 0, lz, s);
            }
        }
        // Kabine: Frontscheibe vorn, Türfenster, Sitzbank; Dach mit Blaulicht und Signalhorn
        for (int lx = 0; lx < 3; lx++) {
            b.setze(lx, 1, 0, scheibe(glas, false, lx != 1, lx < 2, lx > 0));
            b.setze(lx, 2, 0, rot);
            b.setze(lx, 2, 1, rot);
        }
        b.setze(0, 1, 1, scheibe(glas, true, true, false, false));
        b.setze(2, 1, 1, r.nextInt(3) == 0 ? Bau.LUFT : scheibe(glas, true, true, false, false));
        b.setze(1, 1, 1, treppe(Blocks.POLISHED_BLACKSTONE_STAIRS, Baukasten.H, false));
        b.setze(0, 3, 0, Blocks.BLUE_STAINED_GLASS);
        b.setze(2, 3, 0, Blocks.BLUE_STAINED_GLASS);
        b.setze(1, 3, 0, stufe(Blocks.SMOOTH_STONE_SLAB));
        // Aufbau: Geräteräume mit Rollläden, dazwischen der Wassertank
        for (int lz = 2; lz <= 6; lz++) {
            for (int lx = 0; lx < 3; lx++) {
                boolean seite = lx != 1;
                b.setze(lx, 1, lz, seite && lz >= 2 && lz <= 5 ? rollladen : rot);
                b.setze(lx, 2, lz, seite && lz >= 2 && lz <= 5 ? weiss : rot);
            }
        }
        // ein Rollladen steht offen: dahinter die Ausrüstung
        int offen = 2 + r.nextInt(4);
        b.fass(0, 1, offen, Baukasten.R, "feuerwehr");
        b.fass(2, 1, 7 - offen, Baukasten.L, "feuerwehr");
        // Leiter auf dem Dach (Schienen sehen von oben aus wie Sprossen), Schlauchhaspel hinten
        for (int lz = 2; lz <= 6; lz++) {
            b.setze(1, 3, lz, Blocks.RAIL.defaultBlockState().setValue(RailBlock.SHAPE, RailShape.NORTH_SOUTH));
        }
        b.setze(0, 3, 6, Blocks.WHITE_WOOL);
        b.setze(2, 3, 2, Blocks.CHAIN.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z));
    }
}
