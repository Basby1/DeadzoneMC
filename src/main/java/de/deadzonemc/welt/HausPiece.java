package de.deadzonemc.welt;

import de.deadzonemc.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FurnaceBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.resources.ResourceKey;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Ein verlassenes Wohnhaus: ein oder zwei Stockwerke, manchmal ein Keller, Satteldach oder Flachdach.
 * Grundriss, Material, Einrichtung und Verfall werden aus einem festen Startwert abgeleitet, damit das
 * Haus über Chunk-Grenzen hinweg zusammenpasst.
 */
public class HausPiece extends StructurePiece {

    /** Baustoffe eines Hauses. */
    private record Material(Block wand, Block alt, Block ecke, Block boden, Block dachTreppe, Block dachVoll,
                            Block kellerwand, boolean flach) {
    }

    private static final Material[] MATERIALIEN = {
            new Material(Blocks.BRICKS, Blocks.PACKED_MUD, Blocks.STRIPPED_DARK_OAK_LOG, Blocks.SPRUCE_PLANKS,
                    Blocks.DARK_OAK_STAIRS, Blocks.DARK_OAK_PLANKS, Blocks.COBBLESTONE, false),
            new Material(Blocks.WHITE_TERRACOTTA, Blocks.LIGHT_GRAY_TERRACOTTA, Blocks.POLISHED_ANDESITE,
                    Blocks.OAK_PLANKS, Blocks.BRICK_STAIRS, Blocks.BRICKS, Blocks.STONE_BRICKS, false),
            new Material(Blocks.STONE_BRICKS, Blocks.CRACKED_STONE_BRICKS, Blocks.POLISHED_ANDESITE,
                    Blocks.DARK_OAK_PLANKS, Blocks.DEEPSLATE_TILE_STAIRS, Blocks.DEEPSLATE_TILES,
                    Blocks.MOSSY_COBBLESTONE, false),
            new Material(Blocks.SPRUCE_PLANKS, Blocks.DARK_OAK_PLANKS, Blocks.STRIPPED_SPRUCE_LOG,
                    Blocks.SPRUCE_PLANKS, Blocks.SPRUCE_STAIRS, Blocks.SPRUCE_PLANKS, Blocks.COBBLESTONE, false),
            new Material(Blocks.LIGHT_GRAY_CONCRETE, Blocks.ANDESITE, Blocks.STONE_BRICKS, Blocks.BIRCH_PLANKS,
                    Blocks.STONE_BRICK_STAIRS, Blocks.STONE_BRICKS, Blocks.STONE_BRICKS, true),
            new Material(Blocks.MUD_BRICKS, Blocks.PACKED_MUD, Blocks.STRIPPED_OAK_LOG, Blocks.OAK_PLANKS,
                    Blocks.MUD_BRICK_STAIRS, Blocks.MUD_BRICKS, Blocks.COBBLESTONE, false),
    };

    private static final Block[] BETTEN = {Blocks.RED_BED, Blocks.WHITE_BED, Blocks.GRAY_BED, Blocks.BROWN_BED,
            Blocks.LIGHT_BLUE_BED};
    private static final Block[] TEPPICHE = {Blocks.BROWN_CARPET, Blocks.GRAY_CARPET, Blocks.RED_CARPET,
            Blocks.LIGHT_GRAY_CARPET};

    private final int x0;
    private final int y;
    private final int z0;
    private final int breite;
    private final int tiefe;
    private final int stockwerke;
    private final boolean keller;
    /** 0 = Norden, 1 = Süden, 2 = Westen, 3 = Osten. */
    private final int tuer;
    private final int material;
    private final float verfall;
    private final long saat;

    public HausPiece(int x0, int y, int z0, int breite, int tiefe, int stockwerke, boolean keller, int tuer,
                     int material, float verfall, long saat) {
        super(ModRegistry.HAUS_TEIL.get(), 0, rahmen(x0, y, z0, breite, tiefe, stockwerke, keller));
        this.x0 = x0;
        this.y = y;
        this.z0 = z0;
        this.breite = breite;
        this.tiefe = tiefe;
        this.stockwerke = stockwerke;
        this.keller = keller;
        this.tuer = tuer;
        this.material = material;
        this.verfall = verfall;
        this.saat = saat;
    }

    public HausPiece(CompoundTag tag) {
        super(ModRegistry.HAUS_TEIL.get(), tag);
        this.x0 = tag.getInt("x0");
        this.y = tag.getInt("y");
        this.z0 = tag.getInt("z0");
        this.breite = tag.getInt("breite");
        this.tiefe = tag.getInt("tiefe");
        this.stockwerke = tag.getInt("stockwerke");
        this.keller = tag.getBoolean("keller");
        this.tuer = tag.getInt("tuer");
        this.material = tag.getInt("material");
        this.verfall = tag.getFloat("verfall");
        this.saat = tag.getLong("saat");
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putInt("x0", x0);
        tag.putInt("y", y);
        tag.putInt("z0", z0);
        tag.putInt("breite", breite);
        tag.putInt("tiefe", tiefe);
        tag.putInt("stockwerke", stockwerke);
        tag.putBoolean("keller", keller);
        tag.putInt("tuer", tuer);
        tag.putInt("material", material);
        tag.putFloat("verfall", verfall);
        tag.putLong("saat", saat);
    }

    private static BoundingBox rahmen(int x0, int y, int z0, int breite, int tiefe, int stockwerke, boolean keller) {
        int dach = (Math.max(breite, tiefe) + 3) / 2 + 1;
        return new BoundingBox(x0 - 1, y - (keller ? 5 : 1), z0 - 1,
                x0 + breite, y + 4 * stockwerke + dach, z0 + tiefe);
    }

    // ---- Abmessungen -------------------------------------------------------------------------

    private int x1() {
        return x0 + breite - 1;
    }

    private int z1() {
        return z0 + tiefe - 1;
    }

    private boolean rand(int x, int z) {
        return x == x0 || x == x1() || z == z0 || z == z1();
    }

    private boolean ecke(int x, int z) {
        return (x == x0 || x == x1()) && (z == z0 || z == z1());
    }

    private Material mat() {
        return MATERIALIEN[Math.floorMod(material, MATERIALIEN.length)];
    }

    // ---- Bauen -------------------------------------------------------------------------------

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator,
                            RandomSource random, BoundingBox bereich, net.minecraft.world.level.ChunkPos chunkPos,
                            BlockPos pivot) {
        fundament(level, bereich);
        if (keller) {
            kellerBauen(level, bereich);
        }
        for (int s = 0; s < stockwerke; s++) {
            stockwerk(level, bereich, s);
        }
        decke(level, bereich, y + 4 * stockwerke);
        if (mat().flach()) {
            flachdach(level, bereich, y + 4 * stockwerke);
        } else {
            satteldach(level, bereich, y + 4 * stockwerke);
        }
        tuerSetzen(level, bereich);
        leitern(level, bereich);
        einrichten(level, bereich);
    }

    /** Füllt unter dem Haus bis zum Boden auf, damit nichts in der Luft schwebt. */
    private void fundament(WorldGenLevel level, BoundingBox bereich) {
        int start = keller ? y - 5 : y - 1;
        for (int x = x0; x <= x1(); x++) {
            for (int z = z0; z <= z1(); z++) {
                for (int yy = start; yy > start - 14; yy--) {
                    BlockState da = Bau.holen(level, x, yy, z);
                    if (!(da.isAir() || da.canBeReplaced() || !da.getFluidState().isEmpty())) {
                        break;
                    }
                    Bau.setzen(level, bereich, x, yy, z, Blocks.COBBLESTONE.defaultBlockState());
                }
            }
        }
    }

    private void kellerBauen(WorldGenLevel level, BoundingBox bereich) {
        BlockState wand = mat().kellerwand().defaultBlockState();
        for (int x = x0; x <= x1(); x++) {
            for (int z = z0; z <= z1(); z++) {
                for (int yy = y - 4; yy <= y - 1; yy++) {
                    BlockState zustand;
                    if (rand(x, z)) {
                        zustand = Bau.chance(saat, x, yy, z, 0.25) ? Blocks.MOSSY_COBBLESTONE.defaultBlockState() : wand;
                    } else if (yy == y - 4) {
                        zustand = Bau.chance(saat, x, yy, z, 0.3) ? Blocks.GRAVEL.defaultBlockState()
                                : Blocks.STONE.defaultBlockState();
                    } else {
                        zustand = Bau.LUFT;
                    }
                    Bau.setzen(level, bereich, x, yy, z, zustand);
                }
            }
        }
    }

    private BlockState wandBlock(int x, int yy, int z) {
        Material m = mat();
        double hoehe = (yy - y) / (4.0 * stockwerke);
        double w = Bau.zufall(saat, x, yy, z);
        if (w < verfall * (0.06 + 0.25 * hoehe)) {
            return Bau.LUFT;
        }
        if (w < verfall * 0.9) {
            return m.alt().defaultBlockState();
        }
        return m.wand().defaultBlockState();
    }

    private void stockwerk(WorldGenLevel level, BoundingBox bereich, int s) {
        int basis = y + 4 * s;
        Material m = mat();
        // Boden
        for (int x = x0; x <= x1(); x++) {
            for (int z = z0; z <= z1(); z++) {
                BlockState boden;
                if (rand(x, z)) {
                    boden = s == 0 ? Blocks.COBBLESTONE.defaultBlockState() : m.wand().defaultBlockState();
                } else if (s > 0 && Bau.chance(saat, x, basis, z, verfall * 0.07)) {
                    boden = Bau.LUFT; // eingebrochene Dielen
                } else {
                    boden = m.boden().defaultBlockState();
                }
                Bau.setzen(level, bereich, x, basis, z, boden);
            }
        }
        // Wände und Innenraum
        for (int yy = basis + 1; yy <= basis + 3; yy++) {
            for (int x = x0; x <= x1(); x++) {
                for (int z = z0; z <= z1(); z++) {
                    BlockState zustand;
                    if (ecke(x, z)) {
                        zustand = m.ecke().defaultBlockState();
                    } else if (rand(x, z)) {
                        zustand = wandBlock(x, yy, z);
                    } else {
                        zustand = Bau.LUFT;
                    }
                    Bau.setzen(level, bereich, x, yy, z, zustand);
                }
            }
        }
        fenster(level, bereich, basis + 2);
        innenwand(level, bereich, basis);
    }

    private void fenster(WorldGenLevel level, BoundingBox bereich, int yy) {
        // Wände entlang X (Norden und Süden)
        for (int x = x0 + 1; x < x1(); x++) {
            if ((x - x0) % 3 != 2) {
                continue;
            }
            for (int z : new int[]{z0, z1()}) {
                if (!istTuerSpalte(x, z)) {
                    Bau.setzen(level, bereich, x, yy, z, fensterBlock(x, yy, z, true));
                }
            }
        }
        // Wände entlang Z (Westen und Osten)
        for (int z = z0 + 1; z < z1(); z++) {
            if ((z - z0) % 3 != 2) {
                continue;
            }
            for (int x : new int[]{x0, x1()}) {
                if (!istTuerSpalte(x, z)) {
                    Bau.setzen(level, bereich, x, yy, z, fensterBlock(x, yy, z, false));
                }
            }
        }
    }

    private BlockState fensterBlock(int x, int yy, int z, boolean entlangX) {
        double w = Bau.zufall(saat ^ 0x51L, x, yy, z);
        if (w < 0.45) {
            return Bau.LUFT; // eingeschlagen
        }
        if (w < 0.62) {
            return Blocks.SPRUCE_PLANKS.defaultBlockState(); // zugenagelt
        }
        BlockState scheibe = Blocks.GLASS_PANE.defaultBlockState();
        if (entlangX) {
            return scheibe.setValue(CrossCollisionBlock.EAST, true).setValue(CrossCollisionBlock.WEST, true);
        }
        return scheibe.setValue(CrossCollisionBlock.NORTH, true).setValue(CrossCollisionBlock.SOUTH, true);
    }

    private int tuerX() {
        return switch (tuer) {
            case 2 -> x0;
            case 3 -> x1();
            default -> x0 + breite / 2;
        };
    }

    private int tuerZ() {
        return switch (tuer) {
            case 0 -> z0;
            case 1 -> z1();
            default -> z0 + tiefe / 2;
        };
    }

    private Direction tuerRichtung() {
        return switch (tuer) {
            case 0 -> Direction.NORTH;
            case 1 -> Direction.SOUTH;
            case 2 -> Direction.WEST;
            default -> Direction.EAST;
        };
    }

    private boolean istTuerSpalte(int x, int z) {
        return x == tuerX() && z == tuerZ();
    }

    private void tuerSetzen(WorldGenLevel level, BoundingBox bereich) {
        int x = tuerX();
        int z = tuerZ();
        if (Bau.chance(saat, x, y, z, 0.5)) {
            BlockState tuerBlock = Blocks.SPRUCE_DOOR.defaultBlockState().setValue(DoorBlock.FACING, tuerRichtung())
                    .setValue(DoorBlock.OPEN, Bau.chance(saat, x, y + 1, z, 0.5));
            Bau.setzen(level, bereich, x, y + 1, z, tuerBlock.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
            Bau.setzen(level, bereich, x, y + 2, z, tuerBlock.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
        } else {
            Bau.setzen(level, bereich, x, y + 1, z, Bau.LUFT);
            Bau.setzen(level, bereich, x, y + 2, z, Bau.LUFT);
        }
        // Stufe vor der Tür
        Direction raus = tuerRichtung();
        Bau.setzen(level, bereich, x + raus.getStepX(), y, z + raus.getStepZ(), Blocks.COBBLESTONE.defaultBlockState());
        Bau.setzen(level, bereich, x + raus.getStepX(), y + 1, z + raus.getStepZ(), Bau.LUFT);
        Bau.setzen(level, bereich, x + raus.getStepX(), y + 2, z + raus.getStepZ(), Bau.LUFT);
    }

    /** Teilt große Häuser in zwei Räume; die Wand verläuft parallel zur Eingangswand. */
    private boolean hatInnenwand() {
        return (tuer <= 1 && tiefe >= 9) || (tuer >= 2 && breite >= 9);
    }

    private boolean istInnenwand(int x, int z) {
        if (!hatInnenwand() || rand(x, z)) {
            return false;
        }
        return tuer <= 1 ? z == z0 + tiefe / 2 : x == x0 + breite / 2;
    }

    private boolean istDurchgang(int x, int z) {
        return istInnenwand(x, z) && (tuer <= 1 ? x == tuerX() : z == tuerZ());
    }

    private void innenwand(WorldGenLevel level, BoundingBox bereich, int basis) {
        if (!hatInnenwand()) {
            return;
        }
        for (int x = x0 + 1; x < x1(); x++) {
            for (int z = z0 + 1; z < z1(); z++) {
                if (!istInnenwand(x, z)) {
                    continue;
                }
                for (int yy = basis + 1; yy <= basis + 3; yy++) {
                    boolean durchgang = istDurchgang(x, z) && yy <= basis + 2;
                    BlockState zustand = durchgang || Bau.chance(saat ^ 0x77L, x, yy, z, verfall * 0.2)
                            ? Bau.LUFT : mat().boden().defaultBlockState();
                    Bau.setzen(level, bereich, x, yy, z, zustand);
                }
            }
        }
    }

    private void decke(WorldGenLevel level, BoundingBox bereich, int yy) {
        for (int x = x0; x <= x1(); x++) {
            for (int z = z0; z <= z1(); z++) {
                BlockState zustand = rand(x, z) ? mat().wand().defaultBlockState() : mat().boden().defaultBlockState();
                if (!rand(x, z) && Bau.chance(saat ^ 0x99L, x, yy, z, verfall * 0.12)) {
                    zustand = Bau.LUFT;
                }
                Bau.setzen(level, bereich, x, yy, z, zustand);
            }
        }
    }

    private void flachdach(WorldGenLevel level, BoundingBox bereich, int oben) {
        for (int x = x0; x <= x1(); x++) {
            for (int z = z0; z <= z1(); z++) {
                if (rand(x, z) && !Bau.chance(saat, x, oben + 1, z, verfall * 0.4)) {
                    Bau.setzen(level, bereich, x, oben + 1, z, mat().wand().defaultBlockState());
                }
            }
        }
    }

    private void satteldach(WorldGenLevel level, BoundingBox bereich, int oben) {
        Material m = mat();
        boolean firstEntlangX = breite >= tiefe;
        int spannweite = (firstEntlangX ? tiefe : breite) + 2;
        int schichten = (spannweite + 1) / 2;
        for (int k = 0; k < schichten; k++) {
            int yy = oben + k;
            int a = (firstEntlangX ? z0 : x0) - 1 + k;
            int b = (firstEntlangX ? z1() : x1()) + 1 - k;
            int laengeVon = (firstEntlangX ? x0 : z0) - 1;
            int laengeBis = (firstEntlangX ? x1() : z1()) + 1;
            for (int l = laengeVon; l <= laengeBis; l++) {
                boolean giebelEnde = l == laengeVon || l == laengeBis;
                for (int q = a; q <= b; q++) {
                    int x = firstEntlangX ? l : q;
                    int z = firstEntlangX ? q : l;
                    BlockState zustand;
                    if (a == b) {
                        zustand = m.dachVoll().defaultBlockState();
                    } else if (q == a || q == b) {
                        Direction hoch = firstEntlangX
                                ? (q == a ? Direction.SOUTH : Direction.NORTH)
                                : (q == a ? Direction.EAST : Direction.WEST);
                        zustand = m.dachTreppe().defaultBlockState()
                                .setValue(StairBlock.FACING, hoch).setValue(StairBlock.HALF, Half.BOTTOM);
                    } else if (k == 0) {
                        continue; // Unterkante: dort liegt schon die Decke
                    } else if (!giebelEnde && (l == laengeVon + 1 || l == laengeBis - 1)) {
                        zustand = m.wand().defaultBlockState(); // Giebelwand
                    } else {
                        zustand = Bau.LUFT; // Dachboden
                    }
                    if (!giebelEnde && (q == a || q == b || a == b) && k > 0
                            && Bau.chance(saat ^ 0x33L, x, yy, z, verfall * 0.45)) {
                        zustand = Bau.LUFT; // Loch im Dach
                    }
                    if (giebelEnde && zustand.isAir()) {
                        continue;
                    }
                    Bau.setzen(level, bereich, x, yy, z, zustand);
                }
            }
        }
    }

    private void leitern(WorldGenLevel level, BoundingBox bereich) {
        BlockState nachOsten = Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.EAST);
        BlockState nachWesten = Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.WEST);
        for (int s = 0; s < stockwerke - 1; s++) {
            int basis = y + 4 * s;
            for (int yy = basis + 1; yy <= basis + 4; yy++) {
                Bau.setzen(level, bereich, x0 + 1, yy, z0 + 1, nachOsten);
            }
        }
        if (keller) {
            for (int yy = y - 3; yy <= y; yy++) {
                Bau.setzen(level, bereich, x1() - 1, yy, z1() - 1, nachWesten);
            }
        }
    }

    // ---- Einrichtung -------------------------------------------------------------------------

    private void einrichten(WorldGenLevel level, BoundingBox bereich) {
        for (int s = 0; s < stockwerke; s++) {
            ResourceKey<LootTable> beute = s == 0
                    ? (Bau.chance(saat, s, 1, 0, 0.5) ? Bau.beute("haus_kueche") : Bau.beute("haus_wohnraum"))
                    : Bau.beute("haus_schlafzimmer");
            raumEinrichten(level, bereich, y + 4 * s, s, beute, true);
        }
        if (keller) {
            raumEinrichten(level, bereich, y - 4, 9, Bau.beute("haus_keller"), false);
        }
    }

    private boolean reserviert(int x, int z, int stufe) {
        if (stufe < stockwerke - 1 && stufe != 9 && x == x0 + 1 && z == z0 + 1) {
            return true; // Leiter nach oben
        }
        if (stufe > 0 && stufe != 9 && x == x0 + 1 && z == z0 + 1) {
            return true; // Leiter von unten kommt hier an
        }
        if (keller && (stufe == 0 || stufe == 9) && x == x1() - 1 && z == z1() - 1) {
            return true; // Kellerleiter
        }
        if (stufe == 0) {
            Direction rein = tuerRichtung().getOpposite();
            if (x == tuerX() + rein.getStepX() && z == tuerZ() + rein.getStepZ()) {
                return true;
            }
        }
        if (hatInnenwand()) {
            if (istInnenwand(x, z)) {
                return true;
            }
            // vor und hinter dem Durchgang frei lassen
            for (Direction d : Direction.Plane.HORIZONTAL) {
                if (istDurchgang(x + d.getStepX(), z + d.getStepZ())) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Richtung von der Wand weg in den Raum hinein, oder null, wenn das Feld nicht an einer Außenwand liegt. */
    private Direction vonDerWand(int x, int z) {
        if (x == x0 + 1) {
            return Direction.EAST;
        }
        if (x == x1() - 1) {
            return Direction.WEST;
        }
        if (z == z0 + 1) {
            return Direction.SOUTH;
        }
        if (z == z1() - 1) {
            return Direction.NORTH;
        }
        return null;
    }

    private void raumEinrichten(WorldGenLevel level, BoundingBox bereich, int basis, int stufe,
                                ResourceKey<LootTable> beute, boolean wohnraum) {
        RandomSource r = RandomSource.create(saat * 31 + stufe * 7919L);
        Set<Long> belegt = new HashSet<>();
        List<int[]> plaetze = new ArrayList<>();
        for (int x = x0 + 1; x < x1(); x++) {
            for (int z = z0 + 1; z < z1(); z++) {
                if (vonDerWand(x, z) != null && !reserviert(x, z, stufe)) {
                    plaetze.add(new int[]{x, z});
                }
            }
        }
        // Mischen (fester Zufall)
        for (int i = plaetze.size() - 1; i > 0; i--) {
            int j = r.nextInt(i + 1);
            int[] tmp = plaetze.get(i);
            plaetze.set(i, plaetze.get(j));
            plaetze.set(j, tmp);
        }

        int kisten = wohnraum ? 1 + r.nextInt(2) : 2 + r.nextInt(2);
        int moebel = Math.min(plaetze.size(), 3 + r.nextInt(4));
        int y1 = basis + 1;
        for (int i = 0; i < moebel; i++) {
            int x = plaetze.get(i)[0];
            int z = plaetze.get(i)[1];
            if (!belegt.add(schluessel(x, z))) {
                continue;
            }
            Direction rein = vonDerWand(x, z);
            if (kisten > 0) {
                kisten--;
                BlockState behaelter = r.nextBoolean()
                        ? Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, rein)
                        : Blocks.BARREL.defaultBlockState();
                Bau.kiste(level, bereich, x, y1, z, behaelter, beute, r.nextLong());
                continue;
            }
            int wahl = r.nextInt(wohnraum ? 10 : 6);
            BlockState teil;
            switch (wahl) {
                case 0, 1 -> {
                    if (wohnraum && bettVersuchen(level, bereich, x, y1, z, rein, r, belegt)) {
                        continue;
                    }
                    teil = Blocks.COBWEB.defaultBlockState();
                }
                case 2 -> teil = Blocks.CRAFTING_TABLE.defaultBlockState();
                case 3 -> teil = Blocks.FURNACE.defaultBlockState().setValue(FurnaceBlock.FACING, rein);
                case 4 -> teil = Blocks.GRAVEL.defaultBlockState();
                case 5 -> teil = Blocks.COBWEB.defaultBlockState();
                case 6 -> teil = Blocks.BOOKSHELF.defaultBlockState();
                case 7 -> teil = Blocks.CAULDRON.defaultBlockState();
                case 8 -> teil = Blocks.POTTED_DEAD_BUSH.defaultBlockState();
                default -> teil = Blocks.SPRUCE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP);
            }
            Bau.setzen(level, bereich, x, y1, z, teil);
        }

        // Spinnweben unter der Decke, Teppichreste, Schutt – und manchmal ein Schädel
        for (int x = x0 + 1; x < x1(); x++) {
            for (int z = z0 + 1; z < z1(); z++) {
                if (reserviert(x, z, stufe) || belegt.contains(schluessel(x, z))) {
                    continue;
                }
                long s = saat ^ (stufe * 0x9E37L);
                if (Bau.chance(s, x, basis + 3, z, vonDerWand(x, z) != null ? 0.12 : 0.04)) {
                    Bau.setzen(level, bereich, x, basis + 3, z, Blocks.COBWEB.defaultBlockState());
                }
                if (Bau.holen(level, x, basis, z).isAir()) {
                    continue; // eingebrochener Boden
                }
                double w = Bau.zufall(s, x, y1, z);
                if (wohnraum && w < 0.08) {
                    Bau.setzen(level, bereich, x, y1, z, TEPPICHE[(int) (w * 1000) % TEPPICHE.length].defaultBlockState());
                } else if (w < 0.08 + verfall * 0.12) {
                    Bau.setzen(level, bereich, x, y1, z, Blocks.GRAVEL.defaultBlockState());
                } else if (w > 0.993) {
                    Bau.setzen(level, bereich, x, y1, z, Blocks.SKELETON_SKULL.defaultBlockState()
                            .setValue(SkullBlock.ROTATION, (int) (w * 100000) % 16));
                }
            }
        }
    }

    private boolean bettVersuchen(WorldGenLevel level, BoundingBox bereich, int x, int y1, int z, Direction rein,
                                  RandomSource r, Set<Long> belegt) {
        int fx = x + rein.getStepX();
        int fz = z + rein.getStepZ();
        if (rand(fx, fz) || istInnenwand(fx, fz) || belegt.contains(schluessel(fx, fz))) {
            return false;
        }
        belegt.add(schluessel(fx, fz));
        Block bett = BETTEN[r.nextInt(BETTEN.length)];
        Direction zurWand = rein.getOpposite();
        Bau.setzen(level, bereich, x, y1, z, bett.defaultBlockState()
                .setValue(BedBlock.FACING, zurWand).setValue(BedBlock.PART, BedPart.HEAD));
        Bau.setzen(level, bereich, fx, y1, fz, bett.defaultBlockState()
                .setValue(BedBlock.FACING, zurWand).setValue(BedBlock.PART, BedPart.FOOT));
        return true;
    }

    private static long schluessel(int x, int z) {
        return ((long) x << 32) ^ (z & 0xffffffffL);
    }
}
