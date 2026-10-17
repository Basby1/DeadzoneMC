package de.deadzonemc.welt.stadt;

import de.deadzonemc.ModRegistry;
import de.deadzonemc.welt.bau.Baukasten;
import de.deadzonemc.welt.gebaeude.Gebaeude;
import de.deadzonemc.welt.gebaeude.Gebaeudearten;
import de.deadzonemc.welt.plan.Grundstueck;
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

/** Ein Grundstück mit seinem Gebäude. Was genau gebaut wird, entscheidet die Gebäudeart. */
public class GebaeudePiece extends StructurePiece {

    private final String typ;
    private final int minX;
    private final int minZ;
    private final int maxX;
    private final int maxZ;
    private final int y;
    private final Direction vorne;
    private final long saat;
    private final String strasse;
    private final int hausnummer;

    public GebaeudePiece(Grundstueck g) {
        super(ModRegistry.GEBAEUDE_TEIL.get(), 0, rahmen(g.typ, g.minX, g.minZ, g.maxX, g.maxZ, g.y));
        this.typ = g.typ;
        this.minX = g.minX;
        this.minZ = g.minZ;
        this.maxX = g.maxX;
        this.maxZ = g.maxZ;
        this.y = g.y;
        this.vorne = g.vorne;
        this.saat = g.saat;
        this.strasse = g.strasse;
        this.hausnummer = g.hausnummer;
    }

    public GebaeudePiece(CompoundTag tag) {
        super(ModRegistry.GEBAEUDE_TEIL.get(), tag);
        this.typ = tag.getString("typ");
        this.minX = tag.getInt("x1");
        this.minZ = tag.getInt("z1");
        this.maxX = tag.getInt("x2");
        this.maxZ = tag.getInt("z2");
        this.y = tag.getInt("y");
        this.vorne = Direction.from2DDataValue(tag.getInt("vorne"));
        this.saat = tag.getLong("saat");
        this.strasse = tag.getString("strasse");
        this.hausnummer = tag.getInt("nr");
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putString("typ", typ);
        tag.putInt("x1", minX);
        tag.putInt("z1", minZ);
        tag.putInt("x2", maxX);
        tag.putInt("z2", maxZ);
        tag.putInt("y", y);
        tag.putInt("vorne", vorne.get2DDataValue());
        tag.putLong("saat", saat);
        tag.putString("strasse", strasse);
        tag.putInt("nr", hausnummer);
    }

    private static BoundingBox rahmen(String typ, int minX, int minZ, int maxX, int maxZ, int y) {
        Gebaeude art = Gebaeudearten.holen(typ);
        return new BoundingBox(minX, y, minZ, maxX, y + art.hoehe(), maxZ);
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator,
                            RandomSource random, BoundingBox bereich, ChunkPos chunkPos, BlockPos pivot) {
        Baukasten b = new Baukasten(level, bereich, minX, minZ, maxX, maxZ, y, vorne, saat);
        Gebaeudearten.holen(typ).bauen(b, new Gebaeude.Info(strasse, hausnummer));
    }
}
