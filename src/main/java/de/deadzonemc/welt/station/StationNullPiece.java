package de.deadzonemc.welt.station;

import de.deadzonemc.ModRegistry;
import de.deadzonemc.welt.bau.Baukasten;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/** Das Bauteil von Station Null; gebaut wird Chunk für Chunk. */
public class StationNullPiece extends StructurePiece {

    private final int minX;
    private final int minZ;
    private final int y;
    private final Direction vorne;
    private final long saat;

    public StationNullPiece(int minX, int minZ, int y, Direction vorne, long saat) {
        super(ModRegistry.STATION_NULL_TEIL.get(), 0, new BoundingBox(minX, y - StationNullStruktur.TIEFE - 2, minZ,
                minX + StationNullStruktur.GROESSE - 1, y + 10, minZ + StationNullStruktur.GROESSE - 1));
        this.minX = minX;
        this.minZ = minZ;
        this.y = y;
        this.vorne = vorne;
        this.saat = saat;
    }

    public StationNullPiece(CompoundTag tag) {
        super(ModRegistry.STATION_NULL_TEIL.get(), tag);
        this.minX = tag.getInt("x");
        this.minZ = tag.getInt("z");
        this.y = tag.getInt("y");
        this.vorne = Direction.from2DDataValue(tag.getInt("vorne"));
        this.saat = tag.getLong("saat");
    }

    public int oberflaeche() {
        return y;
    }

    /** Weltposition zu lokalen Bau-Koordinaten {x, y, z}. */
    public BlockPos lokal(net.minecraft.world.level.WorldGenLevel level, int[] l) {
        int g = StationNullStruktur.GROESSE;
        return new Baukasten(level, null, minX, minZ, minX + g - 1, minZ + g - 1, y, vorne, saat).pos(l[0], l[1], l[2]);
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putInt("x", minX);
        tag.putInt("z", minZ);
        tag.putInt("y", y);
        tag.putInt("vorne", vorne.get2DDataValue());
        tag.putLong("saat", saat);
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator,
                            RandomSource random, BoundingBox bereich, ChunkPos chunkPos, BlockPos pivot) {
        int g = StationNullStruktur.GROESSE;
        Baukasten b = new Baukasten(level, bereich, minX, minZ, minX + g - 1, minZ + g - 1, y, vorne, saat);
        StationNullBau.bauen(b, level, bereich);
    }
}
