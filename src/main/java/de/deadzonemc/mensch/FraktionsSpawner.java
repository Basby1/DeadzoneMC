package de.deadzonemc.mensch;

import de.deadzonemc.monster.DeadzoneMonster;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Lässt Menschen in der Welt auftauchen: Patrouillen der Wacht am Tag, Aschekinder nachts und im Nebel,
 * einzelne Überlebende und gelegentlich einen Händler der Karawane.
 */
public final class FraktionsSpawner {

    private FraktionsSpawner() {
    }

    private static int haendlerPause = 6000;

    public static void tick(ServerTickEvent.Post event) {
        int takt = event.getServer().getTickCount();
        if (takt % 1200 != 600) {
            return;
        }
        ServerLevel welt = event.getServer().overworld();
        if (!welt.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING) || welt.players().isEmpty()) {
            return;
        }
        var z = welt.random;
        haendlerPause -= 1200;
        for (ServerPlayer p : welt.players()) {
            if (!DeadzoneMonster.echterSpieler(p)) {
                continue;
            }
            int menschen = welt.getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(96),
                    e -> e instanceof Mensch || e instanceof Haendler).size();
            if (menschen >= 6) {
                continue;
            }
            boolean nachtOderNebel = welt.isNight() || DeadzoneMonster.dichterNebel(welt);
            if (!nachtOderNebel && z.nextInt(100) < 8) {
                gruppe(welt, p, MenschTypen.SOLDAT.get(), 2 + z.nextInt(2), 48, 64);
            } else if (nachtOderNebel && z.nextInt(100) < 7) {
                gruppe(welt, p, MenschTypen.KULTIST.get(), 2 + z.nextInt(2), 40, 56);
            } else if (z.nextInt(100) < 5) {
                gruppe(welt, p, MenschTypen.UEBERLEBENDER.get(), 1, 30, 48);
            }
        }
        if (haendlerPause <= 0 && welt.isDay()) {
            haendlerPause = 36000 + z.nextInt(24000);
            ServerPlayer p = welt.players().get(z.nextInt(welt.players().size()));
            if (gruppe(welt, p, MenschTypen.HAENDLER.get(), 1, 20, 32)) {
                for (ServerPlayer alle : welt.players()) {
                    alle.displayClientMessage(Component.translatable("haendler.deadzonemc.ankunft", p.getDisplayName())
                            .withStyle(ChatFormatting.GOLD), false);
                }
            }
        }
    }

    private static boolean gruppe(ServerLevel welt, ServerPlayer p, EntityType<? extends Mob> typ, int anzahl, int min, int max) {
        var z = welt.random;
        for (int versuch = 0; versuch < 10; versuch++) {
            double w = z.nextDouble() * Math.PI * 2;
            double d = min + z.nextDouble() * (max - min);
            int x = (int) (p.getX() + Math.cos(w) * d);
            int zz = (int) (p.getZ() + Math.sin(w) * d);
            if (!welt.hasChunkAt(new BlockPos(x, 0, zz))) {
                continue;
            }
            int y = welt.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, zz);
            BlockPos pos = new BlockPos(x, y, zz);
            if (!welt.getBlockState(pos.below()).isSolid() || !welt.getFluidState(pos.below()).isEmpty()
                    || Math.abs(y - p.getY()) > 16 || de.deadzonemc.basis.Basen.holen(welt).basisBei(x, zz, 0) != null) {
                continue;
            }
            for (int i = 0; i < anzahl; i++) {
                Mob m = typ.create(welt);
                if (m == null) {
                    return false;
                }
                m.moveTo(x + 0.5 + i, y, zz + 0.5, z.nextFloat() * 360, 0);
                m.finalizeSpawn(welt, welt.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null);
                welt.addFreshEntity(m);
            }
            return true;
        }
        return false;
    }
}
