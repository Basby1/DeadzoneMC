package de.deadzonemc.zuflucht;

import com.mojang.serialization.MapCodec;
import de.deadzonemc.ModRegistry;
import de.deadzonemc.welt.Staedte;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.Arrays;
import java.util.Optional;

/**
 * Eine Zuflucht: befestigtes Lager der Überlebenden (64×64 Blöcke) mit Mauer, Wachtürmen, Markt,
 * Auftragsbüro, Sanitätszelt, Schlafzelten und Gemeinschaftshalle. Nur auf ebenem, trockenem Land und nie
 * mitten in einer Lost-Cities-Stadt.
 */
public class ZufluchtStruktur extends Structure {

    public static final MapCodec<ZufluchtStruktur> CODEC = simpleCodec(ZufluchtStruktur::new);
    public static final int GROESSE = 64;

    public ZufluchtStruktur(StructureSettings settings) {
        super(settings);
    }

    @Override
    public StructureType<?> type() {
        return ModRegistry.ZUFLUCHT.get();
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext ctx) {
        int mx = ctx.chunkPos().getMiddleBlockX();
        int mz = ctx.chunkPos().getMiddleBlockZ();
        int meer = ctx.chunkGenerator().getSeaLevel();
        int[] hoehen = new int[9];
        int i = 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                hoehen[i++] = ctx.chunkGenerator().getBaseHeight(mx + dx * 30, mz + dz * 30, Heightmap.Types.OCEAN_FLOOR_WG,
                        ctx.heightAccessor(), ctx.randomState());
            }
        }
        Arrays.sort(hoehen);
        if (hoehen[0] <= meer || hoehen[8] - hoehen[0] > 12) {
            return Optional.empty(); // Wasser oder zu steil
        }
        if (inStadt(ctx.chunkPos().x, ctx.chunkPos().z)) {
            return Optional.empty();
        }
        int y = hoehen[4];
        Direction vorne = Direction.from2DDataValue((int) Math.floorMod(ctx.seed() ^ (mx * 31L + mz), 4L));
        long saat = ctx.seed() ^ (mx * 0x9E3779B97F4A7C15L) ^ (mz * 0xC2B2AE3D27D4EB4FL);
        int minX = mx - GROESSE / 2;
        int minZ = mz - GROESSE / 2;
        return Optional.of(new GenerationStub(new BlockPos(mx, y, mz),
                b -> b.addPiece(new ZufluchtPiece(minX, minZ, y, vorne, saat))));
    }

    private static boolean inStadt(int cx, int cz) {
        try {
            var server = ServerLifecycleHooks.getCurrentServer();
            if (server == null) {
                return false;
            }
            for (int dx = -3; dx <= 3; dx += 3) {
                for (int dz = -3; dz <= 3; dz += 3) {
                    if (Staedte.istStadt(server.overworld(), cx + dx, cz + dz)) {
                        return true;
                    }
                }
            }
        } catch (RuntimeException e) {
            return false;
        }
        return false;
    }
}
