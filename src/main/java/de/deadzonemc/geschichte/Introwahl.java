package de.deadzonemc.geschichte;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * Welches Intro? Spielregel „deadzoneIntro“ (beim Erstellen der Welt unter „Spielregeln“): 0 = Boot (Standard),
 * 1 = Hubschrauber, 2 = Zufall. Die Wahl gilt für die ganze Welt und wird beim ersten Betreten festgelegt. Für das Boot
 * braucht es eine Küste in der Nähe des Weltspawns – gibt es keine, kommt der Hubschrauber.
 */
public final class Introwahl {

    private Introwahl() {
    }

    public static final GameRules.Key<GameRules.IntegerValue> REGEL = GameRules.register("deadzoneIntro",
            GameRules.Category.MISC, GameRules.IntegerValue.create(0));

    /** Nur damit die Klasse (und damit die Spielregel) beim Laden der Mod registriert wird. */
    public static void registrieren() {
    }

    public record Kueste(BlockPos ufer, Vec3 meer) {
    }

    public static boolean boot(MinecraftServer s) {
        festlegen(s);
        return Fortschritt.von(s).hat("introart:boot");
    }

    /** Für das Kino-Paket: "boot:landX,landZ|" oder "heli:". */
    public static String praefix(MinecraftServer s) {
        Kueste k = boot(s) ? kueste(s) : null;
        if (k == null) {
            return "heli:";
        }
        return String.format(java.util.Locale.ROOT, "boot:%.4f,%.4f|", -k.meer().x, -k.meer().z);
    }

    @Nullable
    public static Kueste kueste(MinecraftServer s) {
        Fortschritt f = Fortschritt.von(s);
        for (String m : f.merker) {
            if (m.startsWith("kueste:")) {
                String[] t = m.substring(7).split(",");
                try {
                    return new Kueste(new BlockPos(Integer.parseInt(t[0]), Integer.parseInt(t[1]), Integer.parseInt(t[2])),
                            new Vec3(Integer.parseInt(t[3]) / 1000.0, 0, Integer.parseInt(t[4]) / 1000.0));
                } catch (Exception e) {
                    return null;
                }
            }
        }
        return null;
    }

    private static void festlegen(MinecraftServer s) {
        Fortschritt f = Fortschritt.von(s);
        if (f.hat("introart:boot") || f.hat("introart:heli")) {
            return;
        }
        ServerLevel w = s.overworld();
        int regel = w.getGameRules().getInt(REGEL);
        boolean boot = regel == 0 || (regel == 2 && w.random.nextBoolean());
        if (boot) {
            Kueste k = suchen(w);
            if (k != null) {
                f.merken(String.format(java.util.Locale.ROOT, "kueste:%d,%d,%d,%d,%d", k.ufer().getX(), k.ufer().getY(),
                        k.ufer().getZ(), Math.round(k.meer().x * 1000), Math.round(k.meer().z * 1000)));
                f.merken("introart:boot");
                return;
            }
        }
        f.merken("introart:heli");
    }

    private static Holder<Biome> biom(ServerLevel w, double x, double z) {
        var quelle = w.getChunkSource().getGenerator().getBiomeSource();
        return quelle.getNoiseBiome(QuartPos.fromBlock(Mth.floor(x)), QuartPos.fromBlock(w.getSeaLevel()),
                QuartPos.fromBlock(Mth.floor(z)), w.getChunkSource().randomState().sampler());
    }

    private static boolean meer(Holder<Biome> b) {
        return b.is(BiomeTags.IS_OCEAN) || b.is(BiomeTags.IS_DEEP_OCEAN);
    }

    /**
     * Sucht vom Weltspawn aus spiralförmig (bis 1600 Blöcke) einen Strand, hinter dem offenes Meer liegt. Nur über die
     * Biomkarte – es werden keine Chunks erzeugt. Dann wird die Uferlinie im echten Gelände bestimmt.
     */
    @Nullable
    static Kueste suchen(ServerLevel w) {
        BlockPos sp = w.getSharedSpawnPos();
        for (int r = 0; r <= 1600; r += 24) {
            int schritte = Math.max(8, (int) (2 * Math.PI * r / 24));
            for (int i = 0; i < schritte; i++) {
                double wi = i * 2 * Math.PI / schritte;
                double x = sp.getX() + Math.cos(wi) * r;
                double z = sp.getZ() + Math.sin(wi) * r;
                if (!biom(w, x, z).is(BiomeTags.IS_BEACH)) {
                    continue;
                }
                double mx = 0, mz = 0;
                int treffer = 0;
                for (int k = 0; k < 16; k++) {
                    double a = k * Math.PI / 8;
                    if (meer(biom(w, x + Math.cos(a) * 40, z + Math.sin(a) * 40))) {
                        mx += Math.cos(a);
                        mz += Math.sin(a);
                        treffer++;
                    }
                }
                if (treffer < 3 || treffer > 11) {
                    continue;
                }
                Vec3 m = new Vec3(mx, 0, mz).normalize();
                // offenes Wasser voraus (kein kleiner See), Land dahinter
                if (!meer(biom(w, x + m.x * 90, z + m.z * 90)) || !meer(biom(w, x + m.x * 130, z + m.z * 130))
                        || meer(biom(w, x - m.x * 40, z - m.z * 40))) {
                    continue;
                }
                BlockPos ufer = uferlinie(w, x, z, m);
                if (ufer != null) {
                    return new Kueste(ufer, m);
                }
            }
        }
        return null;
    }

    /** Im echten Gelände: vom Wasser her landeinwärts bis zum ersten trockenen Block. */
    @Nullable
    public static BlockPos uferlinie(ServerLevel w, double x, double z, Vec3 meer) {
        for (int d = 40; d >= -40; d--) {
            int bx = Mth.floor(x + meer.x * d);
            int bz = Mth.floor(z + meer.z * d);
            w.getChunk(bx >> 4, bz >> 4);
            int y = w.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, bx, bz);
            BlockPos unten = new BlockPos(bx, y - 1, bz);
            if (w.getFluidState(unten).isEmpty() && w.getBlockState(unten).isSolid()) {
                // nur, wenn das Wasser direkt davor wirklich Wasser ist (nicht ein Fluss im Hinterland)
                int wx = Mth.floor(x + meer.x * (d + 2));
                int wz = Mth.floor(z + meer.z * (d + 2));
                int wy = w.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, wx, wz);
                if (!w.getFluidState(new BlockPos(wx, wy - 1, wz)).isEmpty() && y <= w.getSeaLevel() + 4) {
                    return new BlockPos(bx, y, bz);
                }
            }
        }
        return null;
    }
}
