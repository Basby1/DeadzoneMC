package de.deadzonemc.monster;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Im dichten Nebel ist es auch tagsüber gefährlich: In regelmäßigen Abständen tauchen außer Sicht der
 * Spieler Wanderer, Nebelwesen und selten ein Imitator auf.
 */
public final class NebelSpawner {

    private NebelSpawner() {
    }

    public static void tick(ServerTickEvent.Post event) {
        ServerLevel welt = event.getServer().overworld();
        if (event.getServer().getTickCount() % 300 != 0 || !DeadzoneMonster.dichterNebel(welt)
                || !welt.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_DOMOBSPAWNING)) {
            return;
        }
        for (ServerPlayer p : welt.players()) {
            if (!DeadzoneMonster.echterSpieler(p)) {
                continue;
            }
            int nah = welt.getEntitiesOfClass(DeadzoneMonster.class, p.getBoundingBox().inflate(48)).size();
            if (nah >= 6) {
                continue;
            }
            int wurf = welt.random.nextInt(10);
            EntityType<? extends DeadzoneMonster> typ = wurf < 6 ? MonsterTypen.WANDERER.get()
                    : wurf < 9 ? MonsterTypen.NEBELWESEN.get() : MonsterTypen.IMITATOR.get();
            for (int versuch = 0; versuch < 8; versuch++) {
                double winkel = welt.random.nextDouble() * Math.PI * 2;
                double abstand = 24 + welt.random.nextDouble() * 16;
                int x = Mth.floor(p.getX() + Math.cos(winkel) * abstand);
                int z = Mth.floor(p.getZ() + Math.sin(winkel) * abstand);
                if (!welt.hasChunkAt(new BlockPos(x, 0, z))) {
                    continue;
                }
                int y = welt.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                BlockPos pos = new BlockPos(x, y, z);
                if (!welt.getBlockState(pos.below()).isSolid() || !welt.getFluidState(pos.below()).isEmpty()
                        || Math.abs(y - p.getY()) > 12) {
                    continue;
                }
                var e = typ.create(welt);
                if (e == null) {
                    break;
                }
                e.moveTo(x + 0.5, y, z + 0.5, welt.random.nextFloat() * 360, 0);
                if (p.hasLineOfSight(e)) {
                    continue; // nie vor den Augen der Spieler auftauchen
                }
                e.finalizeSpawn(welt, welt.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null);
                welt.addFreshEntity(e);
                break;
            }
        }
    }
}
