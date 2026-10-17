package de.deadzonemc.monster;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;

/**
 * Stalker: bewegt sich nur, wenn kein Spieler hinsieht. Steht nach jedem Umdrehen näher.
 * Kommt nur nachts oder im dichten Nebel und verschwindet, wenn beides vorbei ist.
 */
public class DerStille extends DeadzoneMonster {

    private static final EntityDataAccessor<Boolean> GESEHEN =
            SynchedEntityData.defineId(DerStille.class, EntityDataSerializers.BOOLEAN);

    private float festeDrehung;
    private int springen = 200;

    public DerStille(EntityType<? extends Monster> typ, Level level) {
        super(typ, level, "der_stille");
    }

    public static AttributeSupplier.Builder attribute() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 40)
                .add(Attributes.MOVEMENT_SPEED, 0.42)
                .add(Attributes.ATTACK_DAMAGE, 9)
                .add(Attributes.FOLLOW_RANGE, 64)
                .add(Attributes.ARMOR, 6)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    protected boolean hatLauf() {
        return false;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(GESEHEN, false);
    }

    public boolean gesehen() {
        return entityData.get(GESEHEN);
    }

    /** Ob gerade irgendein Spieler in seine Richtung schaut und freie Sicht hat. */
    private boolean wirdBeobachtet() {
        for (Player p : level().players()) {
            if (!echterSpieler(p) || p.distanceToSqr(this) > 80 * 80) {
                continue;
            }
            Vec3 blick = p.getViewVector(1.0F).normalize();
            Vec3 zuMir = new Vec3(getX() - p.getX(), getEyeY() - p.getEyeY(), getZ() - p.getZ()).normalize();
            if (blick.dot(zuMir) > 0.55 && p.hasLineOfSight(this)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void customServerAiStep() {
        boolean gesehen = wirdBeobachtet();
        if (gesehen && !gesehen()) {
            festeDrehung = getYRot();
        }
        entityData.set(GESEHEN, gesehen);
        if (gesehen) {
            getNavigation().stop();
            setDeltaMovement(0, Math.min(0, getDeltaMovement().y), 0);
            setYRot(festeDrehung);
            setYHeadRot(festeDrehung);
            yBodyRot = festeDrehung;
            return;
        }
        super.customServerAiStep();
        // Nur nachts oder im dichten Nebel
        if (!isPersistenceRequired() && level().isDay() && !dichterNebel(level()) && tickCount > 100) {
            discard();
            return;
        }
        // Ungesehen und weit weg: taucht hinter dem Spieler wieder auf
        if (--springen <= 0) {
            springen = 160 + random.nextInt(160);
            Player p = level().getNearestPlayer(this, 64);
            if (p != null && echterSpieler(p) && distanceTo(p) > 24) {
                Vec3 hinter = p.getViewVector(1.0F).multiply(-1, 0, -1).normalize().scale(10 + random.nextInt(6));
                BlockPos ziel = BlockPos.containing(p.getX() + hinter.x, p.getY(), p.getZ() + hinter.z);
                int y = level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ziel.getX(), ziel.getZ());
                if (Math.abs(y - p.getY()) < 6) {
                    randomTeleport(ziel.getX() + 0.5, y, ziel.getZ() + 0.5, false);
                }
            }
        }
        Player nah = level().getNearestPlayer(this, 6);
        if (nah != null && echterSpieler(nah) && random.nextInt(40) == 0) {
            level().playSound(null, getX(), getY(), getZ(), MonsterSounds.holen(art, "naehe"), SoundSource.HOSTILE, 0.6F, 0.7F);
        }
    }

    @Override
    protected PlayState bewegung(AnimationState<DeadzoneMonster> zustand) {
        if (gesehen()) {
            return PlayState.STOP; // erstarrt wie eine Statue
        }
        return super.bewegung(zustand);
    }

    @Override
    public boolean isPushable() {
        return !gesehen();
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected float getSoundVolume() {
        return 0.6F;
    }
}
