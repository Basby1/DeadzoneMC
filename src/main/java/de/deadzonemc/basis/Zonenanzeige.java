package de.deadzonemc.basis;

import de.deadzonemc.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.joml.Vector3f;

import java.util.Map;

/**
 * Zeigt die Grenzen der Schutzzonen: Wer einen Notstromgenerator oder Elektronik in der Hand hält, sieht den
 * Rand aller Basen in der Nähe als leuchtende Linie am Boden – und beim Generator in der Hand eine Vorschau,
 * wie groß die neue Zone an der angepeilten Stelle würde.
 */
public final class Zonenanzeige {

    private static final DustParticleOptions RAND = new DustParticleOptions(new Vector3f(0.3F, 1.0F, 0.45F), 1.4F);
    private static final DustParticleOptions VORSCHAU = new DustParticleOptions(new Vector3f(1.0F, 0.85F, 0.25F), 1.4F);

    private Zonenanzeige() {
    }

    public static void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || p.tickCount % 10 != 0) {
            return;
        }
        ItemStack haupt = p.getMainHandItem();
        ItemStack neben = p.getOffhandItem();
        boolean generator = haupt.is(ModRegistry.GENERATOR_ITEM.get()) || neben.is(ModRegistry.GENERATOR_ITEM.get());
        boolean elektronik = haupt.is(ModRegistry.ELEKTRONIK.get());
        if (!generator && !elektronik) {
            return;
        }
        ServerLevel w = p.serverLevel();
        for (Map.Entry<Long, Integer> e : Basen.holen(w).alle().entrySet()) {
            BlockPos mitte = BlockPos.of(e.getKey());
            int r = e.getValue();
            double d = Math.sqrt(p.blockPosition().distSqr(mitte));
            if (d < r + 64) {
                kreis(w, p, mitte, r, RAND);
            }
        }
        if (generator) {
            HitResult blick = p.pick(8.0, 1.0F, false);
            if (blick.getType() == HitResult.Type.BLOCK) {
                kreis(w, p, ((BlockHitResult) blick).getBlockPos().above(), GeneratorBlock.RADIUS[1], VORSCHAU);
            }
        }
    }

    private static void kreis(ServerLevel w, ServerPlayer p, BlockPos mitte, int r, DustParticleOptions farbe) {
        int punkte = Math.max(24, (int) (2 * Math.PI * r / 1.2));
        for (int i = 0; i < punkte; i++) {
            double wnk = 2 * Math.PI * i / punkte;
            double x = mitte.getX() + 0.5 + Math.cos(wnk) * r;
            double z = mitte.getZ() + 0.5 + Math.sin(wnk) * r;
            if (p.distanceToSqr(x, p.getY(), z) > 80 * 80) {
                continue;
            }
            int y = w.hasChunkAt(BlockPos.containing(x, 0, z))
                    ? w.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(x), (int) Math.floor(z)) : mitte.getY();
            w.sendParticles(p, farbe, true, x, y + 0.15, z, 1, 0, 0, 0, 0);
            if (i % 6 == 0) {
                w.sendParticles(p, farbe, true, x, y + 1.2, z, 1, 0, 0.3, 0, 0);
            }
        }
    }
}
