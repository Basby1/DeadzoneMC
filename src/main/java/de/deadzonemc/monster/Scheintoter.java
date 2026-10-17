package de.deadzonemc.monster;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;

/**
 * Liegt wie eine Leiche am Boden, ganz still. Kommt ihm jemand zu nahe (etwa zum Plündern), packt er zu,
 * hält fest und steht dann auf. Verrät sich nur durch leises Atmen und ein Zucken der Hand.
 */
public class Scheintoter extends DeadzoneMonster {

    private static final EntityDataAccessor<Boolean> WACH = SynchedEntityData.defineId(Scheintoter.class, EntityDataSerializers.BOOLEAN);
    private static final RawAnimation STEHEN = RawAnimation.begin().thenLoop("animation.scheintoter.stand");
    private static final RawAnimation STEHGANG = RawAnimation.begin().thenLoop("animation.scheintoter.standwalk");
    private int aufstehen;

    public Scheintoter(EntityType<? extends Monster> typ, Level level) {
        super(typ, level, "scheintoter");
    }

    public static AttributeSupplier.Builder attribute() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 24)
                .add(Attributes.MOVEMENT_SPEED, 0.23)
                .add(Attributes.ATTACK_DAMAGE, 5)
                .add(Attributes.FOLLOW_RANGE, 24);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(WACH, false);
    }

    public boolean wach() {
        return entityData.get(WACH);
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
    public EntityDimensions getDefaultDimensions(Pose pose) {
        return wach() ? super.getDefaultDimensions(pose) : EntityDimensions.scalable(1.4F, 0.5F);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> daten) {
        super.onSyncedDataUpdated(daten);
        if (WACH.equals(daten)) {
            refreshDimensions();
        }
    }

    @Override
    protected PlayState bewegung(AnimationState<DeadzoneMonster> zustand) {
        if (!wach()) {
            return zustand.setAndContinue(ruhe);
        }
        return zustand.setAndContinue(zustand.isMoving() ? STEHGANG : STEHEN);
    }

    @Override
    public void aiStep() {
        if (!wach()) {
            getNavigation().stop();
            setDeltaMovement(getDeltaMovement().multiply(0, 1, 0));
            setTarget(null);
        }
        super.aiStep();
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!(level() instanceof ServerLevel sl)) {
            return;
        }
        if (aufstehen > 0 && --aufstehen == 0) {
            entityData.set(WACH, true);
        }
        if (wach() || aufstehen > 0) {
            return;
        }
        Player p = sl.getNearestPlayer(this, 6);
        if (p != null && echterSpieler(p)) {
            if (tickCount % 90 == 0) {
                playSound(MonsterSounds.holen(art, "atmen"), 0.3F, 1.0F);
            }
            if (p.distanceToSqr(this) < 2.2 * 2.2) {
                zupacken(p);
            }
        }
    }

    private void zupacken(Player p) {
        triggerAnim("aktion", "attack");
        playSound(MonsterSounds.holen(art, "packen"), 1.5F, 1.0F);
        p.hurt(damageSources().mobAttack(this), 5);
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 50, 4));
        aufstehen = 25;
        setTarget(p);
    }

    @Override
    public boolean hurt(DamageSource quelle, float menge) {
        boolean ok = super.hurt(quelle, menge);
        if (ok && !wach() && aufstehen == 0 && isAlive()) {
            triggerAnim("aktion", "special");
            aufstehen = 20;
        }
        return ok;
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getAmbientSound() {
        return wach() ? MonsterSounds.holen("wanderer", "ambient") : null;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Wach", wach());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(WACH, tag.getBoolean("Wach"));
    }
}
