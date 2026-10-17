package de.deadzonemc.zuflucht;

import de.deadzonemc.DeadzoneMC;
import de.deadzonemc.mensch.Wache;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Zufluchten sind sicher: Dort spawnen keine Monster, und was über die Mauer will, wird von den Wachen
 * niedergeschossen. Beim Betreten gibt es eine Meldung.
 */
public final class ZufluchtSchutz {

    public static final ResourceKey<Structure> STRUKTUR =
            ResourceKey.create(Registries.STRUCTURE, ResourceLocation.fromNamespaceAndPath(DeadzoneMC.MODID, "zuflucht"));
    private static final Set<UUID> DRIN = new HashSet<>();

    private ZufluchtSchutz() {
    }

    /** Die Zuflucht an dieser Stelle (oder null). Nur für geladene Chunks aufrufen. */
    public static StructureStart bei(ServerLevel level, BlockPos pos) {
        if (level.dimension() != net.minecraft.world.level.Level.OVERWORLD || !level.hasChunkAt(pos)) {
            return null;
        }
        Structure s = level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(STRUKTUR);
        if (s == null) {
            return null;
        }
        StructureStart start = level.structureManager().getStructureAt(pos, s);
        return start.isValid() ? start : null;
    }

    public static boolean sicher(ServerLevel level, BlockPos pos) {
        return bei(level, pos) != null;
    }

    public static void spawnPruefen(FinalizeSpawnEvent event) {
        if (!(event.getEntity() instanceof Enemy) || !(event.getLevel().getLevel() instanceof ServerLevel level)) {
            return;
        }
        MobSpawnType grund = event.getSpawnType();
        if (grund == MobSpawnType.SPAWN_EGG || grund == MobSpawnType.COMMAND || grund == MobSpawnType.SPAWNER) {
            return;
        }
        if (sicher(level, BlockPos.containing(event.getX(), event.getY(), event.getZ()))) {
            event.setSpawnCancelled(true);
        }
    }

    public static void tick(ServerTickEvent.Post event) {
        if (event.getServer().getTickCount() % 20 != 7) {
            return;
        }
        ServerLevel welt = event.getServer().overworld();
        Set<StructureStart> erledigt = new HashSet<>();
        for (ServerPlayer p : welt.players()) {
            StructureStart start = bei(welt, p.blockPosition());
            boolean drin = start != null;
            if (drin && DRIN.add(p.getUUID())) {
                String name = ZufluchtBau.name(zufluchtSaat(start));
                p.displayClientMessage(Component.literal("Zuflucht " + name + " – hier bist du sicher.")
                        .withStyle(ChatFormatting.GREEN), true);
                p.playNotifySound(SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.AMBIENT, 0.6F, 1.2F);
            } else if (!drin && DRIN.remove(p.getUUID())) {
                p.displayClientMessage(Component.literal("Du verlässt die Zuflucht. Pass auf dich auf.")
                        .withStyle(ChatFormatting.GRAY), true);
            }
            if (drin && erledigt.add(start)) {
                verteidigen(welt, start.getBoundingBox());
            }
        }
    }

    private static long zufluchtSaat(StructureStart start) {
        if (!start.getPieces().isEmpty() && start.getPieces().get(0) instanceof ZufluchtPiece z) {
            return z.saat();
        }
        return 0;
    }

    /** Die Wachen schießen Monster innerhalb der Mauern nieder. */
    private static void verteidigen(ServerLevel welt, BoundingBox bb) {
        AABB bereich = AABB.of(bb).inflate(0, 8, 0);
        List<Monster> monster = welt.getEntitiesOfClass(Monster.class, bereich, m -> m.isAlive() && !m.isPersistenceRequired());
        if (monster.isEmpty()) {
            return;
        }
        List<Wache> wachen = welt.getEntitiesOfClass(Wache.class, bereich.inflate(16), Wache::isAlive);
        for (Monster m : monster) {
            Wache w = wachen.isEmpty() ? null : wachen.get(welt.random.nextInt(wachen.size()));
            if (w != null) {
                w.getLookControl().setLookAt(m);
                w.triggerAnim("aktion", "special");
                Vec3 von = w.getEyePosition();
                Vec3 nach = m.getEyePosition();
                for (int i = 1; i < 12; i++) {
                    Vec3 pt = von.lerp(nach, i / 12.0);
                    welt.sendParticles(ParticleTypes.CRIT, pt.x, pt.y, pt.z, 1, 0, 0, 0, 0);
                }
                welt.playSound(null, w.getX(), w.getY(), w.getZ(), SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.HOSTILE, 3.0F, 0.6F);
                m.hurt(w.damageSources().mobAttack(w), 14.0F);
            } else {
                m.hurt(welt.damageSources().magic(), 14.0F);
            }
        }
    }
}
