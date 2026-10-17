package de.deadzonemc.basis;

import de.deadzonemc.monster.DeadzoneMonster;
import de.deadzonemc.monster.MonsterTypen;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.List;

/**
 * Schutz der Basen je nach Sicherheitsstufe: kein Spawnen im Bereich, Monster werden vertrieben, und auf
 * „Belagerung“ kommen in zufälligen Überfallsnächten und in Nebelnächten Angriffswellen.
 */
public final class Basisschutz {

    private Basisschutz() {
    }

    /**
     * Ob in dieser Nacht die Basen angegriffen werden (Stufe Belagerung). Kein fester Rhythmus mehr:
     * Jede Nacht wird mit etwa 22 % Wahrscheinlichkeit zur Überfallsnacht (fest pro Nacht und Welt),
     * dazu jede Nebelnacht.
     */
    public static boolean belagerungsnacht(ServerLevel level) {
        return level.isNight() && (ueberfallTag(level, level.getDayTime() / 24000L) || DeadzoneMonster.dichterNebel(level));
    }

    /** Ob die Nacht dieses Tages eine Überfallsnacht ist (zufällig, aber fest pro Welt und Tag). */
    public static boolean ueberfallTag(ServerLevel level, long tag) {
        long h = level.getSeed() * 0x9E3779B97F4A7C15L + tag * 0xC2B2AE3D27D4EB4FL;
        h ^= (h >>> 29);
        return tag > 0 && Math.floorMod(h, 100L) < 22;
    }

    private static boolean angriffErlaubt(ServerLevel level, Sicherheit stufe) {
        return switch (stufe) {
            case SICHER -> false;
            case BELAGERUNG -> belagerungsnacht(level);
            case GNADENLOS -> true;
        };
    }

    public static void spawnPruefen(FinalizeSpawnEvent event) {
        if (!(event.getEntity() instanceof Enemy) || !(event.getLevel().getLevel() instanceof ServerLevel level)) {
            return;
        }
        MobSpawnType grund = event.getSpawnType();
        if (grund == MobSpawnType.SPAWN_EGG || grund == MobSpawnType.COMMAND || grund == MobSpawnType.SPAWNER) {
            return;
        }
        Basen basen = Basen.holen(level);
        if (basen.alle().isEmpty()) {
            return;
        }
        Sicherheit stufe = Basen.stufe(level);
        BlockPos zentrum = basen.basisBei(event.getX(), event.getZ(), 0);
        if (zentrum == null) {
            return;
        }
        boolean nahAmKern = zentrum.distToCenterSqr(event.getX(), zentrum.getY(), event.getZ()) < 16 * 16;
        if (stufe != Sicherheit.GNADENLOS || nahAmKern) {
            event.setSpawnCancelled(true);
        }
    }

    public static void tick(ServerTickEvent.Post event) {
        int takt = event.getServer().getTickCount();
        if (takt % 20 != 3) {
            return;
        }
        for (ServerLevel level : event.getServer().getAllLevels()) {
            Basen basen = Basen.holen(level);
            if (basen.alle().isEmpty()) {
                continue;
            }
            Sicherheit stufe = Basen.stufe(level);
            boolean erlaubt = angriffErlaubt(level, stufe);
            long zeit = level.getDayTime() % 24000L;
            for (var e : basen.alle().entrySet()) {
                BlockPos zentrum = BlockPos.of(e.getKey());
                int r = e.getValue();
                if (!level.isLoaded(zentrum)) {
                    continue;
                }
                List<ServerPlayer> anwesend = level.getEntitiesOfClass(ServerPlayer.class,
                        new AABB(zentrum).inflate(r + 32, 64, r + 32), DeadzoneMonster::echterSpieler);
                if (!erlaubt) {
                    vertreiben(level, zentrum, r);
                } else if (stufe == Sicherheit.BELAGERUNG && !anwesend.isEmpty() && takt % 600 == 3) {
                    welle(level, zentrum, r, anwesend);
                }
                // Vorwarnung bei Einbruch der Dämmerung
                if (stufe == Sicherheit.BELAGERUNG && zeit >= 12000 && zeit < 12020) {
                    long morgen = level.getDayTime() / 24000L;
                    if (ueberfallTag(level, morgen) || DeadzoneMonster.dichterNebel(level)) {
                        for (ServerPlayer p : anwesend) {
                            p.displayClientMessage(Component.translatable("basis.deadzonemc.warnung").withStyle(ChatFormatting.DARK_RED), false);
                            p.playNotifySound(SoundEvents.RAID_HORN.value(), SoundSource.HOSTILE, 0.8F, 0.7F);
                        }
                    }
                }
            }
        }
    }

    private static void vertreiben(ServerLevel level, BlockPos zentrum, int r) {
        AABB bereich = new AABB(zentrum).inflate(r, 48, r);
        for (Monster m : level.getEntitiesOfClass(Monster.class, bereich)) {
            double dx = m.getX() - zentrum.getX() - 0.5;
            double dz = m.getZ() - zentrum.getZ() - 0.5;
            if (dx * dx + dz * dz > (double) r * r) {
                continue;
            }
            level.sendParticles(ParticleTypes.POOF, m.getX(), m.getY() + 1, m.getZ(), 12, 0.3, 0.6, 0.3, 0.02);
            if (m.isPersistenceRequired()) {
                // Benannte Monster nach draußen setzen statt sie zu löschen
                double l = Math.max(0.1, Math.sqrt(dx * dx + dz * dz));
                int x = (int) (zentrum.getX() + dx / l * (r + 6));
                int z = (int) (zentrum.getZ() + dz / l * (r + 6));
                int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                m.teleportTo(x + 0.5, y, z + 0.5);
                m.setTarget(null);
            } else {
                m.discard();
            }
        }
    }

    private static void welle(ServerLevel level, BlockPos zentrum, int r, List<ServerPlayer> anwesend) {
        int schon = level.getEntitiesOfClass(DeadzoneMonster.class, new AABB(zentrum).inflate(r + 48, 48, r + 48)).size();
        int ziel = Math.min(24, 6 + anwesend.size() * 3);
        if (schon >= ziel) {
            return;
        }
        long tag = level.getDayTime() / 24000L;
        var z = level.random;
        int anzahl = Math.min(ziel - schon, 3 + anwesend.size());
        for (int i = 0; i < anzahl; i++) {
            int wurf = z.nextInt(100);
            EntityType<? extends DeadzoneMonster> typ =
                    wurf < 55 ? MonsterTypen.WANDERER.get()
                            : wurf < 70 ? MonsterTypen.SCHWARM.get()
                            : wurf < 82 ? MonsterTypen.KRIECHER.get()
                            : wurf < 92 ? MonsterTypen.BRUT.get()
                            : tag >= 14 ? MonsterTypen.JUGGERNAUT.get() : MonsterTypen.WANDERER.get();
            double w = z.nextDouble() * Math.PI * 2;
            int x = (int) (zentrum.getX() + Math.cos(w) * (r + 18));
            int zz = (int) (zentrum.getZ() + Math.sin(w) * (r + 18));
            if (!level.isLoaded(new BlockPos(x, 0, zz))) {
                continue;
            }
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, zz);
            DeadzoneMonster m = typ.create(level);
            if (m == null) {
                continue;
            }
            m.moveTo(x + 0.5, y, zz + 0.5, z.nextFloat() * 360, 0);
            m.finalizeSpawn(level, level.getCurrentDifficultyAt(m.blockPosition()), MobSpawnType.EVENT, null);
            ServerPlayer opfer = anwesend.get(z.nextInt(anwesend.size()));
            m.setTarget(opfer);
            level.addFreshEntity(m);
        }
    }
}
