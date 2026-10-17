package de.deadzonemc.monster;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Tank: langsam, gepanzert, bricht durch Wände. Von vorn schützen Panzerplatten, die erst kaputtgeschossen
 * werden müssen. Der leuchtende Auswuchs am Rücken nimmt dreifachen Schaden.
 */
public class Juggernaut extends DeadzoneMonster {

    private static final EntityDataAccessor<Integer> PANZER =
            SynchedEntityData.defineId(Juggernaut.class, EntityDataSerializers.INT);
    private static final int PANZER_MAX = 80;

    private boolean gebruellt = false;

    public Juggernaut(EntityType<? extends Monster> typ, Level level) {
        super(typ, level, "juggernaut");
        xpReward = 40;
    }

    public static AttributeSupplier.Builder attribute() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 160)
                .add(Attributes.MOVEMENT_SPEED, 0.2)
                .add(Attributes.ATTACK_DAMAGE, 16)
                .add(Attributes.ATTACK_KNOCKBACK, 2.0)
                .add(Attributes.FOLLOW_RANGE, 48)
                .add(Attributes.ARMOR, 8)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    protected boolean hatSpezial() {
        return true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(PANZER, PANZER_MAX);
    }

    public int panzer() {
        return entityData.get(PANZER);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        LivingEntity ziel = getTarget();
        if (ziel != null && !gebruellt) {
            gebruellt = true;
            getNavigation().stop();
            triggerAnim("aktion", "special");
            level().playSound(null, getX(), getY(), getZ(), MonsterSounds.holen(art, "brullen"), SoundSource.HOSTILE, 4.0F, 0.8F);
        } else if (ziel == null) {
            gebruellt = false;
        }
        // Durch Wände brechen
        if (horizontalCollision && tickCount % 10 == 0 && level().getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
            Vec3 vorn = Vec3.directionFromRotation(0, getYRot()).scale(0.6);
            AABB bereich = getBoundingBox().move(vorn).deflate(0.05, 0, 0.05).move(0, 0.1, 0);
            boolean zerstoert = false;
            for (BlockPos pos : BlockPos.betweenClosed(BlockPos.containing(bereich.minX, bereich.minY, bereich.minZ),
                    BlockPos.containing(bereich.maxX, bereich.maxY, bereich.maxZ))) {
                BlockState b = level().getBlockState(pos);
                float haerte = b.getDestroySpeed(level(), pos);
                if (!b.isAir() && haerte >= 0 && haerte < 3.0F) {
                    level().destroyBlock(pos, true, this);
                    zerstoert = true;
                }
            }
            if (zerstoert) {
                triggerAnim("aktion", "attack");
            }
        }
    }

    @Override
    public boolean hurt(DamageSource quelle, float menge) {
        if (!level().isClientSide && !quelle.is(DamageTypeTags.BYPASSES_ARMOR)) {
            Vec3 von = quelle.getSourcePosition();
            if (von != null) {
                Vec3 blick = Vec3.directionFromRotation(0, yBodyRot);
                Vec3 zuQuelle = von.subtract(position()).multiply(1, 0, 1).normalize();
                double winkel = blick.dot(zuQuelle);
                if (winkel < -0.35) {
                    // Treffer in den Rücken: Schwachstelle
                    menge *= 3.0F;
                    if (level() instanceof ServerLevel sl) {
                        sl.sendParticles(ParticleTypes.LAVA, getX(), getY() + 1.8, getZ(), 6, 0.3, 0.3, 0.3, 0.1);
                    }
                } else if (panzer() > 0) {
                    int rest = Math.max(0, panzer() - Math.round(menge));
                    entityData.set(PANZER, rest);
                    level().playSound(null, getX(), getY(), getZ(), MonsterSounds.holen(art, "panzer"), SoundSource.HOSTILE, 1.2F,
                            rest == 0 ? 0.6F : 1.2F);
                    if (level() instanceof ServerLevel sl) {
                        sl.sendParticles(rest == 0 ? ParticleTypes.EXPLOSION : ParticleTypes.CRIT, getX(), getY() + 1.6, getZ(),
                                rest == 0 ? 3 : 10, 0.5, 0.5, 0.5, 0.2);
                    }
                    menge *= 0.2F;
                }
            }
        }
        return super.hurt(quelle, menge);
    }

    @Override
    protected float getSoundVolume() {
        return 2.0F;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Panzer", panzer());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Panzer")) {
            entityData.set(PANZER, tag.getInt("Panzer"));
        }
    }
}
