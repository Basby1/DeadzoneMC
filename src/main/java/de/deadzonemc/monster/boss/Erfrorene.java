package de.deadzonemc.monster.boss;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Riese aus dem Schneesturm: Eisklinge, Frostatem lässt erstarren. Feuer ist seine Schwäche. */
public class Erfrorene extends Boss {

    public Erfrorene(EntityType<? extends Monster> typ, Level level) {
        super(typ, level, "erfrorene");
    }

    public static AttributeSupplier.Builder attribute() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 300)
                .add(Attributes.MOVEMENT_SPEED, 0.24)
                .add(Attributes.ATTACK_DAMAGE, 16)
                .add(Attributes.FOLLOW_RANGE, 48)
                .add(Attributes.ARMOR, 10)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.STEP_HEIGHT, 1.5);
    }

    @Override
    protected boolean hatSpezial() {
        return true;
    }

    @Override
    protected String untertitel() {
        return "Im Sturm erstarrt alles.";
    }

    @Override
    public boolean canFreeze() {
        return false;
    }

    @Override
    protected int faehigkeitTakt() {
        return 200;
    }

    @Override
    protected void faehigkeit(ServerLevel level, LivingEntity ziel) {
        triggerAnim("aktion", "special");
        playSound(de.deadzonemc.monster.MonsterSounds.holen(art, "atem"), 3.0F, 0.8F);
        Vec3 blick = getLookAngle();
        Vec3 mund = getEyePosition();
        for (int i = 1; i <= 16; i++) {
            Vec3 p = mund.add(blick.scale(i * 0.6));
            level.sendParticles(ParticleTypes.SNOWFLAKE, p.x, p.y, p.z, 6, i * 0.04, i * 0.04, i * 0.04, 0.02);
        }
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(10), e -> e != this)) {
            Vec3 zu = e.getEyePosition().subtract(mund);
            if (zu.length() < 10 && zu.normalize().dot(blick) > 0.75) {
                e.setTicksFrozen(Math.min(e.getTicksRequiredToFreeze() + 120, e.getTicksFrozen() + 200));
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 2));
                e.hurt(damageSources().freeze(), 6);
            }
        }
    }

    @Override
    public boolean doHurtTarget(Entity ziel) {
        boolean ok = super.doHurtTarget(ziel);
        if (ok && ziel instanceof LivingEntity l) {
            l.setTicksFrozen(Math.min(l.getTicksRequiredToFreeze() + 60, l.getTicksFrozen() + 80));
        }
        return ok;
    }

    @Override
    public boolean hurt(DamageSource quelle, float menge) {
        if (quelle.is(DamageTypeTags.IS_FIRE)) {
            menge *= 2.5F;
        }
        if (quelle.is(DamageTypeTags.IS_FREEZING)) {
            return false;
        }
        return super.hurt(quelle, menge);
    }
}
