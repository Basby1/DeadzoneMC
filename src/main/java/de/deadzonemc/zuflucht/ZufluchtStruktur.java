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
        Direction vorne = Direction.from2DDataValue((int) Math.floorMod(ctx.seed() ^ (mx * 31L + mz), 4L));
        long saat = ctx.seed() ^ (mx * 0x9E3779B97F4A7C15L) ^ (mz * 0xC2B2AE3D27D4EB4FL);
        int minX = mx - GROESSE / 2;
        int minZ = mz - GROESSE / 2;
        // Zufluchten liegen logisch: am Stadtrand an der Straße (ein freigeräumter, ummauerter Block auf
        // Straßenhöhe, wie Woodbury) oder draußen auf dem Land – nie mitten im Stadtkern.
        int[] rand = stadtrand(ctx.chunkPos().x, ctx.chunkPos().z);
        if (rand != null) {
            GRUENDE.incrementAndGet(0);
            int rx = rand[0] * 16 + 8;
            int rz = rand[1] * 16 + 8;
            int ys = rand[2];
            Direction zurStadt = Direction.from2DDataValue(rand[3]);
            long saat2 = saat ^ rand[0] * 31L ^ rand[1];
            return Optional.of(new GenerationStub(new BlockPos(rx, ys, rz),
                    b -> b.addPiece(new ZufluchtPiece(rx - GROESSE / 2, rz - GROESSE / 2, ys, zurStadt, saat2, true))));
        }
        if (stadtNah(ctx.chunkPos().x, ctx.chunkPos().z)) {
            GRUENDE.incrementAndGet(0);
            return Optional.empty(); // Stadtkern: hier lebt niemand mehr
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
        return Optional.of(new GenerationStub(new BlockPos(mx, y, mz),
                b -> b.addPiece(new ZufluchtPiece(minX, minZ, y, vorne, saat, false))));
    }

    /**
     * Sucht im Umkreis von 5 Chunks einen Chunk am Stadtrand (Stadtfaktor knapp über der Schwelle, mit Straße).
     * Rückgabe: {chunkX, chunkZ, Straßenhöhe, Richtung des Tors zur Stadt} oder null.
     */
    private static int[] stadtrand(int cx, int cz) {
        try {
            var server = ServerLifecycleHooks.getCurrentServer();
            if (server == null) {
                return null;
            }
            var ow = server.overworld();
            double s = Staedte.schwelle();
            int[] best = null;
            int bestAbstand = Integer.MAX_VALUE;
            for (int dx = -5; dx <= 5; dx++) {
                for (int dz = -5; dz <= 5; dz++) {
                    float f = Staedte.stadtFaktor(ow, cx + dx, cz + dz);
                    if (f <= s || f > s * 1.8) {
                        continue;
                    }
                    int ab = dx * dx + dz * dz;
                    if (ab >= bestAbstand) {
                        continue;
                    }
                    Integer h = Staedte.strassenhoehe(ow, cx + dx, cz + dz);
                    if (h == null) {
                        continue;
                    }
                    // Tor in Richtung des dichteren Stadtteils
                    int richtung = 0;
                    float hoechst = -1;
                    for (Direction d : Direction.Plane.HORIZONTAL) {
                        float g = Staedte.stadtFaktor(ow, cx + dx + d.getStepX() * 3, cz + dz + d.getStepZ() * 3);
                        if (g > hoechst) {
                            hoechst = g;
                            richtung = d.get2DDataValue();
                        }
                    }
                    best = new int[]{cx + dx, cz + dz, h, richtung};
                    bestAbstand = ab;
                }
            }
            return best;
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static boolean stadtNah(int cx, int cz) {
        try {
            var server = ServerLifecycleHooks.getCurrentServer();
            return server != null && Staedte.stadtFaktor(server.overworld(), cx, cz) > Staedte.schwelle();
        } catch (RuntimeException e) {
            return false;
        }
    }

    /** Straßenhöhe, falls die Mitte in einer Stadt liegt (sonst null). Erst billig schätzen, dann genau fragen. */
    private static Integer strassenhoehe(int cx, int cz) {
        try {
            var server = ServerLifecycleHooks.getCurrentServer();
            if (server == null || !Staedte.vielleichtStadt(server.overworld(), cx, cz)) {
                return null;
            }
            return Staedte.strassenhoehe(server.overworld(), cx, cz);
        } catch (RuntimeException e) {
            return null;
        }
    }
}
