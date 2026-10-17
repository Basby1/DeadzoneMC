package de.deadzonemc.monster.boss;

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
import net.minecraft.world.phys.Vec3;

/** Blutmond in der Stadt: Fleischerhaken an der Kette. Wer in Sichtweite ist, wird herangezogen. */
public class Schlaechter extends Boss {

    public Schlaechter(EntityType<? extends Monster> typ, Level level) {
        super(typ, level, "schlaechter");
    }

    public static AttributeSupplier.Builder attribute() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 280)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.ATTACK_DAMAGE, 18)
                .add(Attributes.FOLLOW_RANGE, 48)
                .add(Attributes.ARMOR, 6)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    protected boolean hatSpezial() {
        return true;
    }

    @Override
    protected boolean hatLauf() {
        return false;
    }

    @Override
    protected int auftrittDauer() {
        return 40;
    }

    @Override
    protected String untertitel() {
        return "Der Blutmond hat Hunger.";
    }

    @Override
    protected int faehigkeitTakt() {
        return 160;
    }

    @Override
    protected void faehigkeit(ServerLevel level, LivingEntity ziel) {
        double d = distanceTo(ziel);
        if (d < 5 || d > 22 || !getSensing().hasLineOfSight(ziel)) {
            return; // Sichtlinie brechen schützt vor dem Haken
        }
        triggerAnim("aktion", "special");
        playSound(de.deadzonemc.monster.MonsterSounds.holen(art, "kette"), 3.0F, 0.8F);
        Vec3 von = getEyePosition();
        Vec3 nach = ziel.getEyePosition();
        for (int i = 1; i < 20; i++) {
            Vec3 p = von.lerp(nach, i / 20.0);
            level.sendParticles(ParticleTypes.CRIT, p.x, p.y, p.z, 1, 0, 0, 0, 0);
        }
        Vec3 zug = position().subtract(ziel.position()).normalize().scale(Math.min(2.4, d * 0.18));
        ziel.setDeltaMovement(zug.x, 0.45, zug.z);
        ziel.hurtMarked = true;
        ziel.hurt(damageSources().mobAttack(this), 4);
        ziel.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 2));
    }

    @Override
    public boolean doHurtTarget(Entity ziel) {
        boolean ok = super.doHurtTarget(ziel);
        if (ok && ziel instanceof LivingEntity l) {
            l.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0));
        }
        return ok;
    }
}
