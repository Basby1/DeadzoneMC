package de.deadzonemc.welt;

import de.deadzonemc.ModRegistry;
import de.deadzonemc.welt.feature.AutowrackFeature;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/** Eine aufgerissene Straße durch die Siedlung, mit Laternenmasten und Autowracks. */
public class StrassePiece extends StructurePiece {

    private static final Block[] BELAG = {
            Blocks.GRAY_CONCRETE, Blocks.GRAY_CONCRETE, Blocks.GRAY_CONCRETE, Blocks.GRAY_CONCRETE,
            Blocks.ANDESITE, Blocks.ANDESITE, Blocks.GRAVEL, Blocks.GRAVEL, Blocks.COARSE_DIRT, Blocks.COBBLESTONE,
            Blocks.TUFF, Blocks.BLACK_CONCRETE_POWDER
    };

    private final boolean entlangX;
    /** Mittellinie quer zur Fahrtrichtung. */
    private final int mitte;
    private final int von;
    private final int bis;
    private final int hoehe;
    private final long saat;

    public StrassePiece(boolean entlangX, int mitte, int von, int bis, int hoehe, long saat) {
        super(ModRegistry.STRASSEN_TEIL.get(), 0, rahmen(entlangX, mitte, von, bis, hoehe));
        this.entlangX = entlangX;
        this.mitte = mitte;
        this.von = von;
        this.bis = bis;
        this.hoehe = hoehe;
        this.saat = saat;
    }

    public StrassePiece(CompoundTag tag) {
        super(ModRegistry.STRASSEN_TEIL.get(), tag);
        this.entlangX = tag.getBoolean("entlangX");
        this.mitte = tag.getInt("mitte");
        this.von = tag.getInt("von");
        this.bis = tag.getInt("bis");
        this.hoehe = tag.getInt("hoehe");
        this.saat = tag.getLong("saat");
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putBoolean("entlangX", entlangX);
        tag.putInt("mitte", mitte);
        tag.putInt("von", von);
        tag.putInt("bis", bis);
        tag.putInt("hoehe", hoehe);
        tag.putLong("saat", saat);
    }

    private static BoundingBox rahmen(boolean entlangX, int mitte, int von, int bis, int hoehe) {
        return entlangX
                ? new BoundingBox(von, hoehe - 12, mitte - 4, bis, hoehe + 16, mitte + 4)
                : new BoundingBox(mitte - 4, hoehe - 12, von, mitte + 4, hoehe + 16, bis);
    }

    private int x(int laengs, int quer) {
        return entlangX ? laengs : mitte + quer;
    }

    private int z(int laengs, int quer) {
        return entlangX ? mitte + quer : laengs;
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator,
                            RandomSource random, BoundingBox bereich, ChunkPos chunkPos, BlockPos pivot) {
        for (int l = von; l <= bis; l++) {
            for (int q = -2; q <= 2; q++) {
                int x = x(l, q);
                int z = z(l, q);
                if (!bereich.isInside(x, bereich.minY(), z)) {
                    continue;
                }
                int oben = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
                BlockState boden = Bau.holen(level, x, oben, z);
                if (!boden.getFluidState().isEmpty()) {
                    // Über Wasser: ein morscher Holzsteg
                    Bau.setzen(level, bereich, x, oben + 1, z, Blocks.SPRUCE_SLAB.defaultBlockState()
                            .setValue(SlabBlock.TYPE, SlabType.BOTTOM));
                    continue;
                }
                if (!Bau.fest(boden)) {
                    continue;
                }
                double w = Bau.zufall(saat, x, 0, z);
                BlockState belag;
                if (q == 0 && Math.floorMod(l, 5) < 2 && w < 0.6) {
                    belag = Blocks.WHITE_CONCRETE.defaultBlockState(); // verblasste Mittellinie
                } else if (w > 0.93) {
                    belag = Blocks.GRASS_BLOCK.defaultBlockState(); // Gras bricht durch
                } else {
                    belag = BELAG[(int) (w * 997) % BELAG.length].defaultBlockState();
                }
                Bau.setzen(level, bereich, x, oben, z, belag);
                // Pflanzen auf der Straße entfernen (aber Gras darf durchwachsen)
                BlockState darueber = Bau.holen(level, x, oben + 1, z);
                if (!darueber.isAir() && darueber.canBeReplaced() && w <= 0.93) {
                    Bau.setzen(level, bereich, x, oben + 1, z, Bau.LUFT);
                }
            }
            // Laternenmasten an beiden Seiten, alle zwölf Blöcke
            if (Math.floorMod(l - von, 12) == 6) {
                int seite = Math.floorMod(l / 12, 2) == 0 ? 3 : -3;
                mast(level, bereich, x(l, seite), z(l, seite));
            }
        }
        // Autowracks
        int anzahl = (bis - von) / 18;
        RandomSource r = RandomSource.create(saat);
        for (int i = 0; i < anzahl; i++) {
            int l = von + 3 + r.nextInt(Math.max(1, bis - von - 8));
            int q = r.nextBoolean() ? -2 : 0;
            int x = x(l, q);
            int z = z(l, q);
            if (!bereich.isInside(x, bereich.minY(), z)) {
                continue;
            }
            int oben = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
            AutowrackFeature.bauen(level, RandomSource.create(saat + i), new BlockPos(x, oben, z), bereich);
        }
    }

    private void mast(WorldGenLevel level, BoundingBox bereich, int x, int z) {
        if (!bereich.isInside(x, bereich.minY(), z)) {
            return;
        }
        int boden = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
        if (!Bau.fest(Bau.holen(level, x, boden - 1, z))) {
            return;
        }
        boolean umgeknickt = Bau.chance(saat, x, 1, z, 0.35);
        int hoehe = umgeknickt ? 2 : 5;
        for (int i = 0; i < hoehe; i++) {
            Bau.setzen(level, bereich, x, boden + i, z, Blocks.COBBLESTONE_WALL.defaultBlockState());
        }
        if (!umgeknickt) {
            Bau.setzen(level, bereich, x, boden + hoehe, z, Blocks.STONE_BRICK_SLAB.defaultBlockState());
        }
    }
}
