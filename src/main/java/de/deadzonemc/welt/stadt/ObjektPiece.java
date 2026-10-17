package de.deadzonemc.welt.stadt;

import de.deadzonemc.ModRegistry;
import de.deadzonemc.welt.bau.Baukasten;
import de.deadzonemc.welt.bau.Fahrzeuge;
import de.deadzonemc.welt.plan.Objekt;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/** Kleine Dinge an der Straße: Autos, Ortsschilder. */
public class ObjektPiece extends StructurePiece {

    private final String typ;
    private final int x;
    private final int z;
    private final int y;
    private final boolean entlangX;
    private final int seite;
    private final long saat;
    private final String text;

    public ObjektPiece(Objekt o) {
        super(ModRegistry.OBJEKT_TEIL.get(), 0, rahmen(o));
        this.typ = o.typ;
        this.x = o.x;
        this.z = o.z;
        this.y = o.y;
        this.entlangX = o.entlangX;
        this.seite = o.seite;
        this.saat = o.saat;
        this.text = o.text;
    }

    public ObjektPiece(CompoundTag tag) {
        super(ModRegistry.OBJEKT_TEIL.get(), tag);
        this.typ = tag.getString("typ");
        this.x = tag.getInt("x");
        this.z = tag.getInt("z");
        this.y = tag.getInt("y");
        this.entlangX = tag.getBoolean("ex");
        this.seite = tag.getInt("seite");
        this.saat = tag.getLong("saat");
        this.text = tag.getString("text");
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putString("typ", typ);
        tag.putInt("x", x);
        tag.putInt("z", z);
        tag.putInt("y", y);
        tag.putBoolean("ex", entlangX);
        tag.putInt("seite", seite);
        tag.putLong("saat", saat);
        tag.putString("text", text);
    }

    private static BoundingBox rahmen(Objekt o) {
        int[] f = o.flaeche();
        return new BoundingBox(f[0], o.y + 1, f[1], f[2], o.y + 6, f[3]);
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator,
                            RandomSource random, BoundingBox bereich, ChunkPos chunkPos, BlockPos pivot) {
        switch (typ) {
            case "auto", "auto_brand" -> {
                Baukasten b = Fahrzeuge.kasten(level, bereich, x, y + 1, z, entlangX, 5, 3, saat);
                Fahrzeuge.auto(b, saat, typ.equals("auto_brand"));
            }
            case "ortsschild" -> ortsschild(level, bereich);
            default -> {
            }
        }
    }

    /** Gelbes Ortsschild auf zwei Pfosten; die Schrift zeigt den Ankommenden entgegen. */
    private void ortsschild(WorldGenLevel level, BoundingBox bereich) {
        Direction blick = entlangX ? (seite == 0 ? Direction.EAST : Direction.WEST)
                : (seite == 0 ? Direction.SOUTH : Direction.NORTH);
        // Schild 3 breit, quer zur Straße
        int x1 = entlangX ? x : x - 1;
        int x2 = entlangX ? x : x + 1;
        int z1 = entlangX ? z - 1 : z;
        int z2 = entlangX ? z + 1 : z;
        Baukasten b = new Baukasten(level, bereich, x1, z1, x2, z2, y, blick, saat);
        b.setze(0, 1, 0, Blocks.IRON_BARS.defaultBlockState());
        b.setze(2, 1, 0, Blocks.IRON_BARS.defaultBlockState());
        b.setze(0, 2, 0, Blocks.IRON_BARS.defaultBlockState());
        b.setze(2, 2, 0, Blocks.IRON_BARS.defaultBlockState());
        for (int lx = 0; lx < 3; lx++) {
            b.setze(lx, 3, 0, Blocks.YELLOW_CONCRETE.defaultBlockState());
            b.setze(lx, 4, 0, Blocks.YELLOW_CONCRETE.defaultBlockState());
        }
        String[] zeilen = umbrechen(text);
        b.schild(1, 3, -1, Baukasten.V, Blocks.BAMBOO_WALL_SIGN, DyeColor.BLACK, "", zeilen[0], zeilen[1], "");
        b.schild(0, 3, -1, Baukasten.V, Blocks.BAMBOO_WALL_SIGN, DyeColor.BLACK);
        b.schild(2, 3, -1, Baukasten.V, Blocks.BAMBOO_WALL_SIGN, DyeColor.BLACK);
        b.schild(1, 4, -1, Baukasten.V, Blocks.BAMBOO_WALL_SIGN, DyeColor.BLACK, "", "", "", "");
    }

    private static String[] umbrechen(String name) {
        if (name.length() <= 13) {
            return new String[]{name, ""};
        }
        int leer = name.lastIndexOf(' ', 13);
        if (leer > 0) {
            return new String[]{name.substring(0, leer), name.substring(leer + 1)};
        }
        int strich = name.lastIndexOf('-', 13);
        if (strich > 0) {
            return new String[]{name.substring(0, strich + 1), name.substring(strich + 1)};
        }
        int mitte = Math.min(12, name.length() / 2);
        return new String[]{name.substring(0, mitte) + "-", name.substring(mitte)};
    }
}
