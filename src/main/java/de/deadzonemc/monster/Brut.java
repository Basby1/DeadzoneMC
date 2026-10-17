package de.deadzonemc.monster;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

/** Aufgeblähter Körper: schwillt an, wenn ein Opfer nah ist, und platzt in einer Wolke aus Säure und Sporen. */
public class Brut extends DeadzoneMonster {

    private static final int ZUENDZEIT = 30;
    private int zuendung = -1;
    private boolean geplatzt = false;

    public Brut(EntityType<? extends Monster> typ, Level level) {
        super(typ, level, "brut");
    }

    public static AttributeSupplier.Builder attribute() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 20)
                .add(Attributes.MOVEMENT_SPEED, 0.18)
                .add(Attributes.ATTACK_DAMAGE, 3)
                .add(Attributes.FOLLOW_RANGE, 24);
    }

    @Override
    protected boolean hatLauf() {
        return false;
    }

    @Override
    protected boolean hatSpezial() {
        return true;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        LivingEntity ziel = getTarget();
        if (zuendung < 0) {
            if (ziel != null && distanceToSqr(ziel) < 3.2 * 3.2) {
                zuendung = ZUENDZEIT;
                triggerAnim("aktion", "special");
                playSound(MonsterSounds.holen(art, "ambient"), 1.5F, 0.6F);
            }
        } else {
            getNavigation().stop();
            if (--zuendung <= 0) {
                platzen();
                discard();
            }
        }
    }

    private void platzen() {
        if (geplatzt || !(level() instanceof ServerLevel sl)) {
            return;
        }
        geplatzt = true;
        sl.playSound(null, getX(), getY(), getZ(), MonsterSounds.holen(art, "platzen"), SoundSource.HOSTILE, 2.0F, 1.0F);
        sl.sendParticles(ParticleTypes.ITEM_SLIME, getX(), getY() + 1, getZ(), 60, 1.2, 0.8, 1.2, 0.15);
        sl.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, getX(), getY() + 1, getZ(), 80, 2.0, 1.0, 2.0, 0.02);
        AreaEffectCloud wolke = new AreaEffectCloud(sl, getX(), getY(), getZ());
        wolke.setRadius(3.5F);
        wolke.setRadiusOnUse(-0.3F);
        wolke.setWaitTime(5);
        wolke.setDuration(220);
        wolke.setRadiusPerTick(-wolke.getRadius() / wolke.getDuration());
        wolke.addEffect(new MobEffectInstance(MobEffects.POISON, 120, 1));
        wolke.addEffect(new MobEffectInstance(MobEffects.HUNGER, 200, 1));
        sl.addFreshEntity(wolke);
        for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(3))) {
            if (e != this) {
                e.hurt(damageSources().mobAttack(this), 6.0F);
            }
        }
    }

    @Override
    public void die(DamageSource quelle) {
        super.die(quelle);
        platzen();
    }
}
