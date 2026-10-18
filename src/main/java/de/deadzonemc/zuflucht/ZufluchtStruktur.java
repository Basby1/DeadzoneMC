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
    /** Statistik für den Selbsttest: [Stadt, Wasser, steil, ok]. */
    public static final java.util.concurrent.atomic.AtomicIntegerArray GRUENDE = new java.util.concurrent.atomic.AtomicIntegerArray(4);

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
        // Erst die billige Prüfung (Stadt), dann Höhen – jede Höhenabfrage kostet eine Geländeberechnung
        if (inStadt(ctx.chunkPos().x, ctx.chunkPos().z)) {
            GRUENDE.incrementAndGet(0);
            return Optional.empty();
        }
        int mitte = ctx.chunkGenerator().getBaseHeight(mx, mz, Heightmap.Types.OCEAN_FLOOR_WG, ctx.heightAccessor(), ctx.randomState());
        if (mitte <= meer) {
            GRUENDE.incrementAndGet(1);
            return Optional.empty();
        }
        int[] hoehen = new int[3];
        hoehen[0] = mitte;
        int i = 1;
        for (int[] d : new int[][]{{-24, -24}, {24, 24}}) {
            int h = ctx.chunkGenerator().getBaseHeight(mx + d[0], mz + d[1], Heightmap.Types.OCEAN_FLOOR_WG,
                    ctx.heightAccessor(), ctx.randomState());
            if (h <= meer || Math.abs(h - mitte) > 14) {
                GRUENDE.incrementAndGet(h <= meer ? 1 : 2);
                return Optional.empty(); // Wasser oder zu steil
            }
            hoehen[i++] = h;
        }
        Arrays.sort(hoehen);
        int y = hoehen[1];
        GRUENDE.incrementAndGet(3);
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
            // Mitte und zwei Ecken: am Stadtrand darf die Mauer stehen, mitten in der Stadt nicht
            for (int[] d : new int[][]{{0, 0}, {-2, -2}, {2, 2}}) {
                if (Staedte.vielleichtStadt(server.overworld(), cx + d[0], cz + d[1])) {
                    return true;
                }
            }
        } catch (RuntimeException e) {
            return false;
        }
        return false;
    }
}
