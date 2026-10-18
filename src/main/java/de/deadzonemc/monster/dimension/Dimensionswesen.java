package de.deadzonemc.monster.dimension;

import de.deadzonemc.monster.DeadzoneMonster;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Gemeinsame Klasse der Dimensionswesen. Jede Art hat eine eigene Fähigkeit (siehe {@link DimArt.Faehigkeit}),
 * Werte und Größe; Modell, Textur, Animationen und Stimme kommen aus den Werkzeugen.
 */
public class Dimensionswesen extends DeadzoneMonster {

    protected final DimArt dim;
    private int takt;
    private int schild;
    /** Trugbild: platzt beim ersten Treffer, macht keinen Schaden. */
    private boolean trug;
    private boolean erwacht;

    public Dimensionswesen(EntityType<? extends Monster> typ, Level level, DimArt dim) {
        super(typ, level, dim.id);
        this.dim = dim;
        this.schild = dim.faehigkeit == DimArt.Faehigkeit.LASER ? 20 : 0;
        this.erwacht = dim.faehigkeit != DimArt.Faehigkeit.FRIEREN;
        if (dim.faehigkeit == DimArt.Faehigkeit.SCHUBSEN) {
            setInvisible(true);
        }
        if (dim.faehigkeit == DimArt.Faehigkeit.SCHEU) {
            // scheu: greift nur an, wenn man ihn angreift
            targetSelector.removeAllGoals(g -> g instanceof net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal);
        }
    }

    public static AttributeSupplier.Builder attribute(DimArt d) {
        AttributeSupplier.Builder b = Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, d.leben)
                .add(Attributes.MOVEMENT_SPEED, d.tempo)
                .add(Attributes.ATTACK_DAMAGE, d.schaden)
                .add(Attributes.ARMOR, d.panzer)
                .add(Attributes.FOLLOW_RANGE, 32);
        if (d.schwebt) {
            b.add(Attributes.FLYING_SPEED, d.tempo * 1.6);
        }
        return b;
    }

    @Override
    protected boolean hatLauf() {
        return false;
    }

    @Override
    protected boolean hatSpezial() {
        return true;
    }

    public void alsTrugbild() {
        trug = true;
    }


    @Override
    public boolean onClimbable() {
        return (dim != null && dim.faehigkeit == DimArt.Faehigkeit.KLETTERN && horizontalCollision) || super.onClimbable();
    }

    @Override
    public boolean hurt(DamageSource quelle, float menge) {
        if (trug && !level().isClientSide) {
            if (level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.POOF, getX(), getY() + 0.6, getZ(), 12, 0.3, 0.3, 0.3, 0.02);
            }
            discard();
            return false;
        }
        switch (dim.faehigkeit) {
            case KRISTALL -> {
                if (quelle.is(net.minecraft.tags.DamageTypeTags.IS_PROJECTILE)) {
                    playSound(SoundEvents.AMETHYST_BLOCK_HIT, 1.0F, 1.4F);
                    return false; // Kugeln prallen ab
                }
                if (quelle.is(net.minecraft.tags.DamageTypeTags.IS_EXPLOSION)) {
                    menge *= 2;
                }
            }
            case ENTREISSEN -> {
                if (quelle.is(net.minecraft.tags.DamageTypeTags.IS_PROJECTILE) && random.nextBoolean()) {
                    playSound(SoundEvents.SHIELD_BLOCK, 1.0F, 1.2F);
                    return false;
                }
            }
            case LASER -> {
                if (schild > 0) {
                    schild -= (int) menge;
                    playSound(SoundEvents.BEACON_DEACTIVATE, 0.6F, 2.0F);
                    return false;
                }
            }
            case HOLO -> {
                menge *= 0.6F;
                if (!level().isClientSide && random.nextFloat() < 0.5F) {
                    teleportieren(8);
                }
            }
            case SCHEU -> {
            }
            default -> {
            }
        }
        boolean r = super.hurt(quelle, menge);
        if (r && dim.faehigkeit == DimArt.Faehigkeit.FRIEREN) {
            erwacht = true;
        }
        return r;
    }

    @Override
    public boolean doHurtTarget(Entity ziel) {
        if (trug) {
            return false;
        }
        boolean r = super.doHurtTarget(ziel);
        if (r && ziel instanceof LivingEntity le) {
            switch (dim.faehigkeit) {
                case FRIEREN -> le.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 3));
                case KLAMMERN -> le.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 70, 0));
                case KLETTERN -> le.igniteForSeconds(2);
                default -> {
                }
            }
        }
        return r;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!(level() instanceof ServerLevel sl)) {
            return;
        }
        LivingEntity ziel = getTarget();
        if (dim.faehigkeit == DimArt.Faehigkeit.FRIEREN && !erwacht) {
            getNavigation().stop();
            Player nah = sl.getNearestPlayer(this, 5);
            if (nah != null && !nah.isCreative()) {
                erwacht = true;
                playSound(SoundEvents.GLASS_BREAK, 1.2F, 0.6F);
                triggerAnim("aktion", "special");
            }
            return;
        }
        if (dim.faehigkeit == DimArt.Faehigkeit.SCHUBSEN) {
            Player nah = sl.getNearestPlayer(this, 6);
            setInvisible(nah == null);
            if (tickCount % 30 == 0) {
                sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, getX(), getY() + 0.5, getZ(), 3, 0.3, 0.3, 0.3, 0.02);
            }
        }
        if (dim.faehigkeit == DimArt.Faehigkeit.STIM && tickCount % 100 == 0) {
            for (Monster m : sl.getEntitiesOfClass(Monster.class, getBoundingBox().inflate(10), m -> m != this)) {
                m.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, 0));
                m.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 200, 0));
                sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, m.getX(), m.getY() + 1, m.getZ(), 4, 0.3, 0.3, 0.3, 0);
            }
        }
        if (dim.faehigkeit == DimArt.Faehigkeit.RENNER && ziel != null && tickCount % 40 == 0) {
            addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 1, false, false));
        }
        if (dim.faehigkeit == DimArt.Faehigkeit.SCHWEREL && tickCount % 40 == 0) {
            addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 60, 0, false, false));
        }
        if (dim.faehigkeit == DimArt.Faehigkeit.ECHO && ziel instanceof Player p && tickCount % 40 == 0) {
            ItemStack h = p.getMainHandItem();
            setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, h.isEmpty() ? ItemStack.EMPTY : h.copyWithCount(1));
            setDropChance(net.minecraft.world.entity.EquipmentSlot.MAINHAND, 0);
        }
        if (ziel == null || ++takt < 120) {
            return;
        }
        takt = random.nextInt(40);
        double d = distanceTo(ziel);
        boolean sicht = hasLineOfSight(ziel);
        switch (dim.faehigkeit) {
            case ZIEHEN -> {
                if (d < 16 && sicht) {
                    triggerAnim("aktion", "special");
                    Vec3 zuMir = position().subtract(ziel.position()).normalize().scale(0.9);
                    ziel.push(zuMir.x, 0.25, zuMir.z);
                    ziel.hurtMarked = true;
                    ziel.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 80, 0));
                    playSound(SoundEvents.ILLUSIONER_CAST_SPELL, 1.5F, 0.6F);
                }
            }
            case SCHUBSEN -> {
                if (d < 8) {
                    Vec3 weg = ziel.position().subtract(position()).normalize().scale(1.2);
                    ziel.push(weg.x, 0.4, weg.z);
                    ziel.hurtMarked = true;
                    ziel.hurt(damageSources().mobAttack(this), 3);
                    playSound(SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR, 1.2F, 0.7F);
                }
            }
            case TRUGBILD -> {
                if (!trug) {
                    triggerAnim("aktion", "special");
                    for (int i = 0; i < 2; i++) {
                        Entity e = getType().create(sl);
                        if (e instanceof Dimensionswesen k) {
                            k.alsTrugbild();
                            k.moveTo(getX() + random.nextInt(5) - 2, getY(), getZ() + random.nextInt(5) - 2, getYRot(), 0);
                            k.setTarget(ziel);
                            sl.addFreshEntity(k);
                        }
                    }
                }
            }
            case ENTREISSEN -> {
                if (d < 10 && sicht && ziel instanceof Player p && !p.getMainHandItem().isEmpty() && random.nextInt(3) == 0) {
                    triggerAnim("aktion", "special");
                    ItemStack h = p.getMainHandItem().copy();
                    p.getInventory().removeItemNoUpdate(p.getInventory().selected);
                    ItemEntity ie = new ItemEntity(sl, p.getX(), p.getEyeY(), p.getZ(), h);
                    Vec3 v = position().subtract(p.position()).normalize().scale(0.35);
                    ie.setDeltaMovement(v.x, 0.3, v.z);
                    ie.setPickUpDelay(30);
                    sl.addFreshEntity(ie);
                    playSound(SoundEvents.ENDERMAN_TELEPORT, 1.0F, 1.5F);
                }
            }
            case SPRUNG -> {
                if (d > 4 && d < 14 && onGround()) {
                    triggerAnim("aktion", "special");
                    Vec3 v = ziel.position().subtract(position()).normalize();
                    setDeltaMovement(v.x * 1.4, 0.6, v.z * 1.4);
                    playSound(SoundEvents.RAVAGER_ROAR, 1.0F, 1.4F);
                }
            }
            case HEBEN -> {
                if (d < 12 && sicht) {
                    triggerAnim("aktion", "special");
                    ziel.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 45, 1));
                    playSound(SoundEvents.SHULKER_SHOOT, 1.2F, 0.6F);
                }
            }
            case BLITZ -> {
                if (d > 3) {
                    triggerAnim("aktion", "special");
                    teleportieren(-1);
                    sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, getX(), getY() + 1, getZ(), 30, 0.4, 0.8, 0.4, 0.1);
                    playSound(SoundEvents.LIGHTNING_BOLT_THUNDER, 0.6F, 1.8F);
                }
            }
            case LASER -> {
                if (sicht && d < 20) {
                    triggerAnim("aktion", "special");
                    Vec3 a = getEyePosition();
                    Vec3 b = ziel.getEyePosition();
                    DustParticleOptions rot = new DustParticleOptions(new Vector3f(1, 0.1F, 0.1F), 0.8F);
                    for (int i = 0; i <= 20; i++) {
                        Vec3 q = a.lerp(b, i / 20.0);
                        sl.sendParticles(rot, q.x, q.y, q.z, 1, 0, 0, 0, 0);
                    }
                    ziel.hurt(damageSources().indirectMagic(this, this), 4);
                    playSound(SoundEvents.BEACON_POWER_SELECT, 0.8F, 2.0F);
                    takt = 80;
                }
            }
            case HOLO -> {
                if (d > 6) {
                    teleportieren(-1);
                }
            }
            default -> {
            }
        }
    }

    /** Teleport: -1 = zum Ziel hin, sonst zufällig im Umkreis. */
    private void teleportieren(int weite) {
        LivingEntity ziel = getTarget();
        for (int i = 0; i < 12; i++) {
            double x;
            double z;
            if (weite < 0 && ziel != null) {
                x = ziel.getX() + random.nextInt(7) - 3;
                z = ziel.getZ() + random.nextInt(7) - 3;
            } else {
                x = getX() + random.nextInt(weite * 2 + 1) - weite;
                z = getZ() + random.nextInt(weite * 2 + 1) - weite;
            }
            if (randomTeleport(x, (ziel != null ? ziel.getY() : getY()), z, true)) {
                return;
            }
        }
    }

    @Override
    public boolean isSensitiveToWater() {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Trug", trug);
        tag.putBoolean("Erwacht", erwacht);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        trug = tag.getBoolean("Trug");
        erwacht = tag.getBoolean("Erwacht") || dim.faehigkeit != DimArt.Faehigkeit.FRIEREN;
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource quelle, boolean spieler) {
        if (!trug) {
            super.dropCustomDeathLoot(level, quelle, spieler);
        }
    }

    @Override
    public boolean shouldDropExperience() {
        return !trug && super.shouldDropExperience();
    }

    @SuppressWarnings("unused")
    private static SoundSource quelle() {
        return SoundSource.HOSTILE;
    }
}
