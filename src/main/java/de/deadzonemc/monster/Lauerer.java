package de.deadzonemc.monster;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;

/**
 * Jumpscare: liegt unsichtbar und reglos auf der Lauer (Schrank, Bett, Decke). Nur leises Atmen verrät ihn.
 * Kommt ein Spieler zu nah, springt er heraus.
 */
public class Lauerer extends DeadzoneMonster {

    private static final EntityDataAccessor<Boolean> VERSTECKT =
            SynchedEntityData.defineId(Lauerer.class, EntityDataSerializers.BOOLEAN);

    private int atemPause = 60;
    private int ohneZiel = 0;

    public Lauerer(EntityType<? extends Monster> typ, Level level) {
        super(typ, level, "lauerer");
    }

    public static AttributeSupplier.Builder attribute() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 18)
                .add(Attributes.MOVEMENT_SPEED, 0.33)
                .add(Attributes.ATTACK_DAMAGE, 6)
                .add(Attributes.FOLLOW_RANGE, 24);
    }

    @Override
    protected boolean hatSpezial() {
        return true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(VERSTECKT, false);
    }

    public boolean versteckt() {
        return entityData.get(VERSTECKT);
    }

    private void verstecken(boolean ja) {
        entityData.set(VERSTECKT, ja);
        setInvisible(ja);
        for (Goal.Flag f : Goal.Flag.values()) {
            if (ja) {
                goalSelector.disableControlFlag(f);
                targetSelector.disableControlFlag(f);
            } else {
                goalSelector.enableControlFlag(f);
                targetSelector.enableControlFlag(f);
            }
        }
        if (ja) {
            setTarget(null);
            getNavigation().stop();
        }
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance schwierigkeit, MobSpawnType grund,
                                        SpawnGroupData daten) {
        SpawnGroupData erg = super.finalizeSpawn(level, schwierigkeit, grund, daten);
        if (grund != MobSpawnType.SPAWN_EGG && grund != MobSpawnType.COMMAND) {
            verstecken(true);
        }
        return erg;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (versteckt()) {
            setDeltaMovement(0, getDeltaMovement().y, 0);
            if (--atemPause <= 0) {
                atemPause = 90 + random.nextInt(90);
                level().playSound(null, getX(), getY(), getZ(), MonsterSounds.holen(art, "atmen"), SoundSource.HOSTILE,
                        0.5F, 0.8F + random.nextFloat() * 0.2F);
            }
            Player p = level().getNearestPlayer(this, 3.2);
            if (p != null && echterSpieler(p)) {
                hervorspringen(p);
            }
            return;
        }
        // Ohne Opfer legt er sich nach einer Weile wieder auf die Lauer
        if (getTarget() == null) {
            if (++ohneZiel > 600) {
                ohneZiel = 0;
                verstecken(true);
            }
        } else {
            ohneZiel = 0;
        }
    }

    private void hervorspringen(Player p) {
        verstecken(false);
        setTarget(p);
        triggerAnim("aktion", "special");
        level().playSound(null, getX(), getY(), getZ(), MonsterSounds.holen(art, "schreck"), SoundSource.HOSTILE, 2.5F, 1.0F);
        p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 50, 0));
        if (p instanceof net.minecraft.server.level.ServerPlayer sp) {
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(sp, new de.deadzonemc.netz.SchreckPayload(2));
        }
        Vec3 richtung = p.position().subtract(position()).normalize();
        setDeltaMovement(richtung.x * 0.8, 0.35, richtung.z * 0.8);
        hasImpulse = true;
    }

    @Override
    public boolean hurt(DamageSource quelle, float menge) {
        if (versteckt() && !level().isClientSide && quelle.getEntity() instanceof Player p) {
            hervorspringen(p);
        }
        return super.hurt(quelle, menge);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return null; // verrät sich nur durch Atmen
    }

    @Override
    public boolean isPushable() {
        return !versteckt() && super.isPushable();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Versteckt", versteckt());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.getBoolean("Versteckt")) {
            verstecken(true);
        }
    }
}
