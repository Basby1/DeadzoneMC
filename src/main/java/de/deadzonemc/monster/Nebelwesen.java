package de.deadzonemc.monster;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Existiert nur im dichten Nebel. Unsichtbar, außer es steht im Licht oder ein Spieler in der Nähe
 * hält eine Lichtquelle. Löst sich auf, sobald der Nebel aufklart.
 */
public class Nebelwesen extends DeadzoneMonster {

    public Nebelwesen(EntityType<? extends Monster> typ, Level level) {
        super(typ, level, "nebelwesen");
    }

    public static AttributeSupplier.Builder attribute() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 16)
                .add(Attributes.MOVEMENT_SPEED, 0.28)
                .add(Attributes.ATTACK_DAMAGE, 5)
                .add(Attributes.FOLLOW_RANGE, 32);
    }

    @Override
    protected boolean hatLauf() {
        return false;
    }

    private static boolean leuchtet(ItemStack s) {
        return s.getItem() instanceof BlockItem b && b.getBlock().defaultBlockState().getLightEmission() > 0;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (tickCount % 10 != 0) {
            return;
        }
        if (!dichterNebel(level()) && !isPersistenceRequired() && tickCount > 40) {
            if (level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 1, getZ(), 20, 0.4, 0.8, 0.4, 0.02);
            }
            discard();
            return;
        }
        boolean sichtbar = level().getMaxLocalRawBrightness(blockPosition()) >= 7;
        if (!sichtbar) {
            for (Player p : level().players()) {
                if (p.distanceToSqr(this) < 12 * 12 && (leuchtet(p.getMainHandItem()) || leuchtet(p.getOffhandItem()))) {
                    sichtbar = true;
                    break;
                }
            }
        }
        setInvisible(!sichtbar);
    }
}
