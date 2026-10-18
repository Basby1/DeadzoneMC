package de.deadzonemc.welt.station;

import com.mojang.serialization.MapCodec;
import de.deadzonemc.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Arrays;
import java.util.Optional;

/**
 * Station Null: das unterirdische Forschungslabor der Halden-Werke, in dem Projekt AURA begann. Oben ein
 * eingezäunter Bunker, unten Labore, Archiv, Anomalie-Zelle und die strahlende Reaktorhalle.
 */
public class StationNullStruktur extends Structure {

    public static final MapCodec<StationNullStruktur> CODEC = simpleCodec(StationNullStruktur::new);
    public static final int GROESSE = 48;
    /** Wie tief der Laborboden unter der Oberfläche liegt. */
    public static final int TIEFE = 24;

    public StationNullStruktur(StructureSettings settings) {
        super(settings);
    }

    @Override
    public StructureType<?> type() {
        return ModRegistry.STATION_NULL.get();
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext ctx) {
        int mx = ctx.chunkPos().getMiddleBlockX();
        int mz = ctx.chunkPos().getMiddleBlockZ();
        int meer = ctx.chunkGenerator().getSeaLevel();
        int[] hoehen = new int[5];
        int i = 0;
        for (int[] d : new int[][]{{0, 0}, {-8, -8}, {8, -8}, {-8, 8}, {8, 8}}) {
            hoehen[i++] = ctx.chunkGenerator().getBaseHeight(mx + d[0], mz + d[1], Heightmap.Types.OCEAN_FLOOR_WG,
                    ctx.heightAccessor(), ctx.randomState());
        }
        Arrays.sort(hoehen);
        if (hoehen[0] <= meer || hoehen[4] - hoehen[0] > 8) {
            return Optional.empty();
        }
        int y = hoehen[2];
        if (y - TIEFE - 2 < ctx.heightAccessor().getMinBuildHeight() + 4) {
            return Optional.empty();
        }
        Direction vorne = Direction.from2DDataValue((int) Math.floorMod(ctx.seed() ^ (mx * 17L + mz * 5L), 4L));
        long saat = ctx.seed() ^ (mx * 0x9E3779B97F4A7C15L) ^ (mz * 0xC2B2AE3D27D4EB4FL) ^ 0x5717L;
        int minX = mx - GROESSE / 2;
        int minZ = mz - GROESSE / 2;
        return Optional.of(new GenerationStub(new BlockPos(mx, y, mz),
                b -> b.addPiece(new StationNullPiece(minX, minZ, y, vorne, saat))));
    }
}
