package de.deadzonemc.waffen.wurf;

import de.deadzonemc.monster.Laerm;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Geworfene Granate. Prallt ab, bis der Zünder abläuft (Brandsatz zerplatzt beim Aufprall). Rauch und
 * Lärmköder bleiben danach eine Weile liegen und wirken.
 */
public class Wurfkoerper extends ThrowableItemProjectile {

    private static final EntityDataAccessor<Integer> ART = SynchedEntityData.defineId(Wurfkoerper.class, EntityDataSerializers.INT);
    private int zeit;
    private int wirkung = -1;

    public Wurfkoerper(EntityType<? extends Wurfkoerper> typ, Level level) {
        super(typ, level);
    }

    public Wurfkoerper(Level level, LivingEntity werfer, Wurfart art) {
        super(Wurfwaffen.WURFKOERPER.get(), werfer, level);
        entityData.set(ART, art.ordinal());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(ART, 0);
    }

    public Wurfart art() {
        if (entityData == null) {
            return Wurfart.values()[0]; // während des Aufbaus (andere Mods erzeugen Probe-Exemplare)
        }
        return Wurfart.values()[entityData.get(ART)];
    }

    @Override
    protected Item getDefaultItem() {
        return Wurfwaffen.WURF.get(art()).get();
    }

    @Override
    protected double getDefaultGravity() {
        return 0.05;
    }

    @Override
    public void tick() {
        if (wirkung >= 0) {
            setDeltaMovement(Vec3.ZERO);
            nachwirken();
            return;
        }
        super.tick();
        zeit++;
        Wurfart a = art();
        if (level().isClientSide) {
            if (a == Wurfart.HANDGRANATE || a == Wurfart.BLENDGRANATE) {
                level().addParticle(ParticleTypes.SMOKE, getX(), getY() + 0.1, getZ(), 0, 0.02, 0);
            } else if (a == Wurfart.BRANDSATZ) {
                level().addParticle(ParticleTypes.SMALL_FLAME, getX(), getY() + 0.1, getZ(), 0, 0.01, 0);
            }
            return;
        }
        if (a.zuender > 0 && zeit >= a.zuender) {
            zuenden();
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult t) {
        super.onHitBlock(t);
        Wurfart a = art();
        if (a == Wurfart.BRANDSATZ) {
            if (!level().isClientSide) {
                zuenden();
            }
            return;
        }
        // Abprallen
        Vec3 v = getDeltaMovement();
        Direction d = t.getDirection();
        double daempf = 0.38;
        v = switch (d.getAxis()) {
            case X -> new Vec3(-v.x * daempf, v.y * 0.6, v.z * 0.6);
            case Y -> new Vec3(v.x * 0.55, -v.y * daempf, v.z * 0.55);
            case Z -> new Vec3(v.x * 0.6, v.y * 0.6, -v.z * daempf);
        };
        if (v.lengthSqr() < 0.004) {
            v = Vec3.ZERO;
        }
        setDeltaMovement(v);
        Vec3 h = t.getLocation().add(Vec3.atLowerCornerOf(d.getNormal()).scale(0.05));
        setPos(h.x, h.y, h.z);
        if (!level().isClientSide && v.lengthSqr() > 0.01) {
            level().playSound(null, getX(), getY(), getZ(), SoundEvents.CHAIN_HIT, SoundSource.NEUTRAL, 0.4F, 1.6F);
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult t) {
        if (art() == Wurfart.BRANDSATZ && !level().isClientSide) {
            zuenden();
            return;
        }
        setDeltaMovement(getDeltaMovement().scale(-0.2));
    }

    @Override
    protected void onHit(HitResult t) {
        if (t.getType() == HitResult.Type.BLOCK) {
            onHitBlock((BlockHitResult) t);
        } else if (t.getType() == HitResult.Type.ENTITY) {
            onHitEntity((EntityHitResult) t);
        }
    }

    private void zuenden() {
        if (!(level() instanceof ServerLevel sl) || isRemoved() || wirkung >= 0) {
            return;
        }
        Wurfart a = art();
        Vec3 p = position();
        if (a.laerm > 0) {
            Laerm.machen(sl, p, a.laerm);
        }
        switch (a) {
            case HANDGRANATE -> {
                sl.explode(this, getOwner() instanceof LivingEntity le ? sl.damageSources().explosion(this, le) : null, null,
                        p.x, p.y + 0.2, p.z, 3.2F, false, Level.ExplosionInteraction.NONE);
                discard();
            }
            case BRANDSATZ -> {
                sl.playSound(null, p.x, p.y, p.z, SoundEvents.GLASS_BREAK, SoundSource.NEUTRAL, 1.2F, 0.9F);
                sl.playSound(null, p.x, p.y, p.z, SoundEvents.FIRECHARGE_USE, SoundSource.NEUTRAL, 1.2F, 0.8F);
                BlockPos mitte = BlockPos.containing(p);
                for (BlockPos q : BlockPos.betweenClosed(mitte.offset(-2, -1, -2), mitte.offset(2, 1, 2))) {
                    if (q.distSqr(mitte) <= 6.5 && sl.random.nextFloat() < 0.75F && sl.getBlockState(q).isAir()
                            && BaseFireBlock.canBePlacedAt(sl, q, Direction.UP)
                            && !de.deadzonemc.zuflucht.ZufluchtSchutz.sicher(sl, q)) {
                        sl.setBlock(q, BaseFireBlock.getState(sl, q), 11);
                    }
                }
                for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, new AABB(mitte).inflate(3))) {
                    e.igniteForSeconds(6);
                }
                discard();
            }
            case BLENDGRANATE -> {
                sl.playSound(null, p.x, p.y, p.z, SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.NEUTRAL, 3.0F, 0.6F);
                sl.sendParticles(ParticleTypes.FLASH, p.x, p.y + 0.5, p.z, 1, 0, 0, 0, 0);
                sl.sendParticles(ParticleTypes.END_ROD, p.x, p.y + 0.5, p.z, 40, 1.5, 1.0, 1.5, 0.1);
                for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, new AABB(BlockPos.containing(p)).inflate(9))) {
                    if (e instanceof Player pl) {
                        Vec3 zu = p.subtract(pl.getEyePosition()).normalize();
                        if (pl.getLookAngle().dot(zu) > 0.3 && pl.distanceToSqr(p) < 81) {
                            pl.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 50, 0, false, false));
                        }
                    } else if (e instanceof Mob m && e instanceof Enemy) {
                        m.setTarget(null);
                        m.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 120, 0, false, false));
                        m.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, 3, false, false));
                        m.getNavigation().stop();
                    }
                }
                discard();
            }
            case RAUCHGRANATE -> {
                sl.playSound(null, p.x, p.y, p.z, SoundEvents.FIRE_EXTINGUISH, SoundSource.NEUTRAL, 1.2F, 0.6F);
                wirkung = 320;
            }
            case LAERMKOEDER -> wirkung = 240;
        }
    }

    /** Rauch verhüllt, Lärmköder piept und lockt. */
    private void nachwirken() {
        if (!(level() instanceof ServerLevel sl)) {
            if (art() == Wurfart.RAUCHGRANATE) {
                for (int i = 0; i < 6; i++) {
                    level().addAlwaysVisibleParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, true,
                            getX() + (random.nextDouble() - 0.5) * 4, getY() + random.nextDouble() * 2, getZ() + (random.nextDouble() - 0.5) * 4,
                            0, 0.02, 0);
                }
            }
            return;
        }
        wirkung--;
        Vec3 p = position();
        if (art() == Wurfart.RAUCHGRANATE) {
            if (wirkung % 5 == 0) {
                for (Mob m : sl.getEntitiesOfClass(Mob.class, new AABB(BlockPos.containing(p)).inflate(6), e -> e instanceof Enemy)) {
                    if (m.getTarget() != null && m.getTarget().distanceToSqr(p) < 49) {
                        m.setTarget(null);
                        m.getNavigation().stop();
                    }
                }
            }
        } else if (wirkung % 20 == 0) {
            sl.playSound(null, p.x, p.y, p.z, SoundEvents.NOTE_BLOCK_BIT.value(), SoundSource.NEUTRAL, 2.0F, 1.8F);
            Laerm.machen(sl, p, 56);
        }
        if (wirkung <= 0) {
            discard();
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("art", entityData.get(ART));
        tag.putInt("zeit", zeit);
        tag.putInt("wirkung", wirkung);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(ART, tag.getInt("art"));
        zeit = tag.getInt("zeit");
        wirkung = tag.contains("wirkung") ? tag.getInt("wirkung") : -1;
    }
}
