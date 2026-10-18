package de.deadzonemc.zuflucht;

import de.deadzonemc.ModRegistry;
import de.deadzonemc.welt.bau.Baukasten;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/** Das eine Bauteil einer Zuflucht. Gebaut wird Chunk für Chunk, jeweils nur der Teil im aktuellen Bereich. */
public class ZufluchtPiece extends StructurePiece {

    private final int minX;
    private final int minZ;
    private final int y;
    private final Direction vorne;
    private final long saat;
    private final boolean stadt;

    public ZufluchtPiece(int minX, int minZ, int y, Direction vorne, long saat, boolean stadt) {
        super(ModRegistry.ZUFLUCHT_TEIL.get(), 0, new BoundingBox(minX - 2, y - 12, minZ - 2,
                minX + ZufluchtStruktur.GROESSE + 1, y + 16, minZ + ZufluchtStruktur.GROESSE + 1));
        this.minX = minX;
        this.minZ = minZ;
        this.y = y;
        this.vorne = vorne;
        this.saat = saat;
        this.stadt = stadt;
    }

    public ZufluchtPiece(CompoundTag tag) {
        super(ModRegistry.ZUFLUCHT_TEIL.get(), tag);
        this.minX = tag.getInt("x");
        this.minZ = tag.getInt("z");
        this.y = tag.getInt("y");
        this.vorne = Direction.from2DDataValue(tag.getInt("vorne"));
        this.saat = tag.getLong("saat");
        this.stadt = tag.getBoolean("stadt");
    }

    public long saat() {
        return saat;
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putInt("x", minX);
        tag.putInt("z", minZ);
        tag.putInt("y", y);
        tag.putInt("vorne", vorne.get2DDataValue());
        tag.putLong("saat", saat);
        tag.putBoolean("stadt", stadt);
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator,
                            RandomSource random, BoundingBox bereich, ChunkPos chunkPos, BlockPos pivot) {
        int g = ZufluchtStruktur.GROESSE;
        Baukasten b = new Baukasten(level, bereich, minX, minZ, minX + g - 1, minZ + g - 1, y, vorne, saat);
        ZufluchtBau.bauen(b, level, bereich, saat, stadt);
    }
}
