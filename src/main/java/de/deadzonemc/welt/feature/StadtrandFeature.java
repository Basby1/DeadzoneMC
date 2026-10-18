package de.deadzonemc.welt.feature;

import de.deadzonemc.welt.Staedte;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import java.util.ArrayList;
import java.util.List;

/**
 * Weicher Stadtrand: Lost Cities setzt Städte als ebene Platte ins Gelände; nur der direkt angrenzende Chunk
 * wird angepasst, dahinter steigen Hügel als Steilwand an. Dieses Feature formt einen breiten Übergang: Im
 * Umkreis von {@link #BREITE} Blöcken um die Stadt wird das Gelände sanft auf die Straßenhöhe gezogen –
 * abgetragen, wo es zu hoch ist, aufgeschüttet, wo es zu tief ist. Läuft vor Bäumen und Bauten.
 */
public class StadtrandFeature extends Feature<NoneFeatureConfiguration> {

    /** Breite des Übergangs in Blöcken. */
    public static final int BREITE = 48;
    private static final int RING = (BREITE + 15) / 16 + 1;
    private static final int MAX_ABTRAG = 48;

    public StadtrandFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    private record Stadtchunk(int cx, int cz, int hoehe) {
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        ServerLevel welt = level.getLevel();
        int cx = ctx.origin().getX() >> 4;
        int cz = ctx.origin().getZ() >> 4;
        // Billige Vorprüfung: liegt überhaupt eine Stadt in der Nähe?
        boolean nah = false;
        for (int dx = -RING; dx <= RING && !nah; dx++) {
            for (int dz = -RING; dz <= RING && !nah; dz++) {
                nah = Staedte.vielleichtStadt(welt, cx + dx, cz + dz);
            }
        }
        if (!nah || Staedte.istStadt(welt, cx, cz)) {
            return false;
        }
        List<Stadtchunk> staedte = new ArrayList<>();
        for (int dx = -RING; dx <= RING; dx++) {
            for (int dz = -RING; dz <= RING; dz++) {
                if ((dx != 0 || dz != 0) && Staedte.vielleichtStadt(welt, cx + dx, cz + dz)) {
                    Integer h = Staedte.strassenhoehe(welt, cx + dx, cz + dz);
                    if (h != null) {
                        staedte.add(new Stadtchunk(cx + dx, cz + dz, h - 1));
                    }
                }
            }
        }
        if (staedte.isEmpty()) {
            return false;
        }
        int meer = level.getSeaLevel();
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        boolean geaendert = false;
        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                int x = (cx << 4) + lx;
                int z = (cz << 4) + lz;
                // Abstand zur nächsten Stadtkante und gewichtete Zielhöhe
                double best = Double.MAX_VALUE;
                double summeH = 0;
                double summeW = 0;
                for (Stadtchunk s : staedte) {
                    int x0 = s.cx << 4;
                    int z0 = s.cz << 4;
                    int ddx = x < x0 ? x0 - x : Math.max(0, x - (x0 + 15));
                    int ddz = z < z0 ? z0 - z : Math.max(0, z - (z0 + 15));
                    double d = Math.sqrt(ddx * ddx + ddz * ddz);
                    best = Math.min(best, d);
                    if (d < BREITE) {
                        double w = 1.0 / (1 + d * d);
                        summeH += s.hoehe * w;
                        summeW += w;
                    }
                }
                if (best >= BREITE || summeW == 0) {
                    continue;
                }
                int stadtH = (int) Math.round(summeH / summeW);
                int natur = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z) - 1;
                if (natur < meer - 1) {
                    continue; // Wasser lassen wir in Ruhe
                }
                double t = best / BREITE;
                double glatt = t * t * (3 - 2 * t);
                int ziel = (int) Math.round(stadtH + (natur - stadtH) * glatt);
                if (Math.abs(ziel - natur) < 1) {
                    continue;
                }
                p.set(x, natur, z);
                BlockState oben = level.getBlockState(p);
                if (!oben.is(BlockTags.DIRT) && !oben.is(BlockTags.SAND) && !oben.is(Blocks.GRAVEL)
                        && !oben.is(BlockTags.BASE_STONE_OVERWORLD) && !oben.is(BlockTags.TERRACOTTA)
                        && !oben.is(Blocks.SNOW_BLOCK) && !oben.is(Blocks.COARSE_DIRT) && !oben.is(Blocks.MUD)) {
                    continue; // keine Bauwerke, Wasser oder Eis anfassen
                }
                BlockState darunter = level.getBlockState(p.set(x, natur - 1, z));
                BlockState fuell = darunter.is(BlockTags.BASE_STONE_OVERWORLD) || darunter.isAir() ? Blocks.DIRT.defaultBlockState()
                        : darunter;
                if (oben.is(BlockTags.SAND)) {
                    fuell = Blocks.SANDSTONE.defaultBlockState();
                }
                if (ziel < natur) {
                    int von = Math.max(ziel + 1, natur - MAX_ABTRAG);
                    for (int y = natur + 3; y >= von; y--) {
                        BlockState b = level.getBlockState(p.set(x, y, z));
                        if (y > natur && !b.canBeReplaced()) {
                            continue;
                        }
                        level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
                    }
                    level.setBlock(p.set(x, von - 1, z), oben, 2);
                } else {
                    for (int y = natur; y < ziel; y++) {
                        level.setBlock(p.set(x, y, z), fuell, 2);
                    }
                    level.setBlock(p.set(x, ziel, z), oben, 2);
                }
                geaendert = true;
            }
        }
        return geaendert;
    }
}
