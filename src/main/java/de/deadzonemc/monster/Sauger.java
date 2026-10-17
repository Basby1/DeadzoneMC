package de.deadzonemc.monster;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

/** Pirscht sich fast unsichtbar an, packt zu und saugt Leben aus – damit heilt er sich selbst. */
public class Sauger extends DeadzoneMonster {

    public Sauger(EntityType<? extends Monster> typ, Level level) {
        super(typ, level, "sauger");
    }

    public static AttributeSupplier.Builder attribute() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 30)
                .add(Attributes.MOVEMENT_SPEED, 0.28)
                .add(Attributes.ATTACK_DAMAGE, 6)
                .add(Attributes.FOLLOW_RANGE, 40)
                .add(Attributes.ARMOR, 2);
    }

    @Override
    protected boolean hatSpezial() {
        return true;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        LivingEntity z = getTarget();
        boolean tarnen = z != null && distanceToSqr(z) > 6 * 6 && hurtTime == 0;
        if (tarnen != isInvisible()) {
            setInvisible(tarnen);
        }
        if (tarnen && tickCount % 6 == 0 && level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.REVERSE_PORTAL, getX(), getY() + 1, getZ(), 2, 0.3, 0.6, 0.3, 0.01); // Flimmern
        }
    }

    @Override
    public boolean doHurtTarget(Entity ziel) {
        boolean ok = super.doHurtTarget(ziel);
        if (ok && ziel instanceof LivingEntity l) {
            triggerAnim("aktion", "special");
            playSound(MonsterSounds.holen(art, "saugen"), 1.2F, 1.0F);
            l.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 50, 2));
            l.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 0));
            heal(5);
        }
        return ok;
    }
}
