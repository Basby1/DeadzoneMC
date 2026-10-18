package de.deadzonemc.monster;

import de.deadzonemc.monster.boss.Boss;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Schonfrist nach dem Betreten und nach jedem Respawn: Gegner in der Nähe verschwinden, neue tauchen in der
 * Nähe nicht auf, niemand zielt auf dich. Wer kurz nach dem Respawn wieder stirbt, bekommt eine längere
 * Schonfrist und wird an einen ruhigeren Platz gesetzt. Kein Sterben im Sekundentakt mehr.
 */
public final class SpawnSchutz {

    private static final int DAUER = 30 * 20;
    private static final int RADIUS_LEEREN = 40;
    private static final int RADIUS_SPERRE = 56;
    private static final Map<UUID, Long> BIS = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> LETZTER_RESPAWN = new ConcurrentHashMap<>();

    private SpawnSchutz() {
    }

    public static boolean geschuetzt(Player p) {
        Long bis = BIS.get(p.getUUID());
        return bis != null && p.level().getGameTime() < bis;
    }

    private static void schuetzen(ServerPlayer p, int ticks) {
        ServerLevel w = p.serverLevel();
        BIS.put(p.getUUID(), w.getGameTime() + ticks);
        for (Mob m : w.getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(RADIUS_LEEREN),
                e -> e instanceof Enemy && !(e instanceof Boss) && !e.isPersistenceRequired())) {
            m.discard();
        }
        p.displayClientMessage(Component.translatable("schutz.deadzonemc.schonfrist", ticks / 20).withStyle(ChatFormatting.GRAY), true);
    }

    public static void betreten(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            schuetzen(p, DAUER);
            p.removeEffect(de.deadzonemc.verletzung.Verletzungen.BLUTUNG);
        }
    }

    public static void respawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || event.isEndConquered()) {
            return;
        }
        ServerLevel w = p.serverLevel();
        long jetzt = w.getGameTime();
        Long vorher = LETZTER_RESPAWN.put(p.getUUID(), jetzt);
        boolean schnellWiederTot = vorher != null && jetzt - vorher < 90 * 20;
        // Ohne Bett: nicht immer an derselben Stelle (dort warten sonst schon die Infizierten)
        if (p.getRespawnPosition() == null || schnellWiederTot) {
            BlockPos ruhig = ruhigerPlatz(w, p);
            if (ruhig != null) {
                p.teleportTo(ruhig.getX() + 0.5, ruhig.getY(), ruhig.getZ() + 0.5);
            }
        }
        schuetzen(p, schnellWiederTot ? DAUER * 2 : DAUER);
    }

    private static BlockPos ruhigerPlatz(ServerLevel w, ServerPlayer p) {
        BlockPos mitte = w.getSharedSpawnPos();
        BlockPos bester = null;
        int wenigste = Integer.MAX_VALUE;
        for (int versuch = 0; versuch < 10; versuch++) {
            int x = mitte.getX() + w.random.nextInt(301) - 150;
            int z = mitte.getZ() + w.random.nextInt(301) - 150;
            if (de.deadzonemc.welt.Staedte.vielleichtStadt(w, x >> 4, z >> 4)) {
                continue;
            }
            w.getChunk(x >> 4, z >> 4);
            int y = w.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos pos = new BlockPos(x, y, z);
            var unten = w.getBlockState(pos.below());
            if (!unten.getFluidState().isEmpty() || !unten.isSolid()) {
                continue;
            }
            int feinde = w.getEntitiesOfClass(Mob.class, new net.minecraft.world.phys.AABB(pos).inflate(48),
                    e -> e instanceof Enemy).size();
            if (feinde < wenigste) {
                wenigste = feinde;
                bester = pos;
            }
            if (feinde == 0) {
                break;
            }
        }
        return bester;
    }

    /** Neu auftauchende Gegner in der Nähe geschützter Spieler werden verhindert. */
    public static void erscheinen(EntityJoinLevelEvent event) {
        Entity e = event.getEntity();
        if (event.loadedFromDisk() || !(e instanceof Enemy) || !(e instanceof Mob) || e instanceof Boss
                || !(event.getLevel() instanceof ServerLevel w) || BIS.isEmpty()) {
            return;
        }
        for (ServerPlayer p : w.players()) {
            if (geschuetzt(p) && p.distanceToSqr(e) < RADIUS_SPERRE * RADIUS_SPERRE) {
                event.setCanceled(true);
                return;
            }
        }
    }

    public static void zielen(LivingChangeTargetEvent event) {
        if (event.getNewAboutToBeSetTarget() instanceof Player p && geschuetzt(p) && event.getEntity() instanceof Enemy) {
            event.setCanceled(true);
        }
    }

    public static void schaden(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Player p) || !geschuetzt(p)) {
            return;
        }
        Entity quelle = event.getSource().getEntity();
        Entity direkt = event.getSource().getDirectEntity();
        if (quelle instanceof Enemy || quelle instanceof de.deadzonemc.mensch.Mensch
                || (direkt instanceof Projectile pr && !(pr.getOwner() instanceof Player))) {
            event.setCanceled(true);
        }
        if (quelle instanceof Player) {
            return;
        }
        if (quelle instanceof LivingEntity) {
            event.setCanceled(true);
        }
    }

    public static void aufhoeren(Player p) {
        BIS.remove(p.getUUID());
    }
}
