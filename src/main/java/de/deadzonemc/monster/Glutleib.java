package de.deadzonemc.monster;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

/** Verkohltes Brandopfer: setzt in Brand, was es trifft; Wasser schadet ihm, Feuer nicht. */
public class Glutleib extends DeadzoneMonster {

    public Glutleib(EntityType<? extends Monster> typ, Level level) {
        super(typ, level, "glutleib");
    }

    public static AttributeSupplier.Builder attribute() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 34)
                .add(Attributes.MOVEMENT_SPEED, 0.23)
                .add(Attributes.ATTACK_DAMAGE, 7)
                .add(Attributes.FOLLOW_RANGE, 32)
                .add(Attributes.ARMOR, 4);
    }

    @Override
    protected boolean hatSpezial() {
        return true;
    }

    @Override
    public boolean doHurtTarget(Entity ziel) {
        boolean ok = super.doHurtTarget(ziel);
        if (ok) {
            ziel.igniteForSeconds(5);
        }
        return ok;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!(level() instanceof ServerLevel sl)) {
            return;
        }
        if (tickCount % 8 == 0) {
            sl.sendParticles(random.nextBoolean() ? ParticleTypes.SMALL_FLAME : ParticleTypes.SMOKE,
                    getX(), getY() + 1.4, getZ(), 2, 0.4, 0.5, 0.4, 0.01);
        }
        if (isInWaterRainOrBubble() && tickCount % 20 == 0) {
            hurt(damageSources().drown(), 2);
            sl.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 1.5, getZ(), 6, 0.3, 0.3, 0.3, 0.02);
        }
        if (getTarget() != null && tickCount % 300 == 0) {
            triggerAnim("aktion", "special");
            playSound(MonsterSounds.holen(art, "glut"), 1.5F, 0.8F);
            sl.sendParticles(ParticleTypes.FLAME, getX(), getY() + 1.6, getZ(), 40, 0.6, 0.6, 0.6, 0.05);
            if (distanceToSqr(getTarget()) < 9) {
                getTarget().igniteForSeconds(4);
            }
        }
    }
}
