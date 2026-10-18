package de.deadzonemc.monster;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * Tagsüber ist man nicht sicher: Infizierte (auch Zombies und Skelette anderer Mods) verbrennen nicht mehr in der
 * Sonne. Feuer von Waffen, Lava oder brennenden Blöcken wirkt weiterhin.
 */
public final class KeineSonne {

    private KeineSonne() {
    }

    public static void tick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Mob m) || !(m instanceof Enemy) || m.level().isClientSide
                || m.tickCount % 4 != 0 || !m.isOnFire()) {
            return;
        }
        if (m.isInLava() || !m.level().isDay()) {
            return;
        }
        BlockPos pos = m.blockPosition();
        if (!m.level().canSeeSky(pos)) {
            return;
        }
        var fuss = m.level().getBlockState(pos);
        if (fuss.is(Blocks.FIRE) || fuss.is(Blocks.SOUL_FIRE) || m.level().getBlockState(pos.below()).is(Blocks.MAGMA_BLOCK)) {
            return;
        }
        // Kürzlich von einem Spieler angezündet (Feueraspekt, Brandmunition)? Dann brennt es weiter.
        if (m.getLastHurtByMob() instanceof Player && m.tickCount - m.getLastHurtByMobTimestamp() < 100) {
            return;
        }
        m.clearFire();
    }
}
