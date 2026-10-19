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
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;

/**
 * Hinterhalt im Grünen: Rinde, Moos und Zweige statt Haut. Hockt reglos und sieht aus wie ein Stück Landschaft –
 * nicht unsichtbar, nur gut getarnt. Wer ihn mit Taschenlampe anleuchtet oder mit Nachtsicht ansieht, lässt ihn
 * aufglimmen. Kommt jemand zu nah, schnellt er hervor und packt zu.
 */
public class Tarnfresser extends DeadzoneMonster {

    private static final EntityDataAccessor<Boolean> VERSTECKT =
            SynchedEntityData.defineId(Tarnfresser.class, EntityDataSerializers.BOOLEAN);

    private int knackPause = 200;
    private int ohneZiel;

    public Tarnfresser(EntityType<? extends Monster> typ, Level level) {
        super(typ, level, "tarnfresser");
    }

    public static AttributeSupplier.Builder attribute() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 28)
                .add(Attributes.MOVEMENT_SPEED, 0.32)
                .add(Attributes.ATTACK_DAMAGE, 7)
                .add(Attributes.ARMOR, 4)
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
            if (--knackPause <= 0) {
                knackPause = 160 + random.nextInt(240);
                level().playSound(null, getX(), getY(), getZ(), MonsterSounds.holen(art, "knacken"), SoundSource.HOSTILE,
                        0.4F, 0.7F + random.nextFloat() * 0.2F);
            }
            if (tickCount % 10 == 0) {
                for (Player p : level().players()) {
                    if (echterSpieler(p) && p.distanceToSqr(this) < 16 * 16 && angeleuchtet(p)) {
                        addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0, false, false));
                        if (p.distanceToSqr(this) < 7 * 7) {
                            hervorspringen(p);
                            return;
                        }
                    }
                }
            }
            Player p = level().getNearestPlayer(this, 3.5);
            if (p != null && echterSpieler(p)) {
                hervorspringen(p);
            }
            return;
        }
        if (getTarget() == null) {
            if (++ohneZiel > 600) {
                ohneZiel = 0;
                verstecken(true);
            }
        } else {
            ohneZiel = 0;
        }
    }

    /** Sieht der Spieler ihn gerade an – mit Nachtsicht oder während er hell angeleuchtet wird? */
    private boolean angeleuchtet(Player p) {
        if (!p.hasLineOfSight(this)) {
            return false;
        }
        Vec3 blick = p.getViewVector(1.0F);
        Vec3 zuMir = getBoundingBox().getCenter().subtract(p.getEyePosition()).normalize();
        if (blick.dot(zuMir) < 0.94) {
            return false;
        }
        return p.hasEffect(MobEffects.NIGHT_VISION)
                || level().getBrightness(LightLayer.BLOCK, blockPosition().above()) >= 9;
    }

    private void hervorspringen(Player p) {
        verstecken(false);
        setTarget(p);
        triggerAnim("aktion", "special");
        level().playSound(null, getX(), getY(), getZ(), MonsterSounds.holen(art, "schreck"), SoundSource.HOSTILE, 2.2F, 1.0F);
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1));
        if (p instanceof net.minecraft.server.level.ServerPlayer sp) {
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(sp, new de.deadzonemc.netz.SchreckPayload(1));
        }
        Vec3 richtung = p.position().subtract(position()).normalize();
        setDeltaMovement(richtung.x * 0.9, 0.38, richtung.z * 0.9);
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
        return null; // verrät sich nur durch knackendes Holz
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
