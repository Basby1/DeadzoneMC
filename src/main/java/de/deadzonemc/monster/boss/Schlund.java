package de.deadzonemc.monster.boss;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Riesiger Wurm unter Wüste und Ödland. Steht fest, beißt alles vor sich nieder, taucht ab und bricht kurz
 * darauf direkt neben dem Ziel wieder aus dem Boden. Vorher bebt der Boden – wer weiterläuft, entkommt.
 */
public class Schlund extends Boss {

    private int unten;      // >0: abgetaucht (unverwundbar, unsichtbar)
    private Vec3 auftauchen;
    private int biss;

    public Schlund(EntityType<? extends Monster> typ, Level level) {
        super(typ, level, "schlund");
    }

    public static AttributeSupplier.Builder attribute() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 350)
                .add(Attributes.MOVEMENT_SPEED, 0.0)
                .add(Attributes.ATTACK_DAMAGE, 20)
                .add(Attributes.FOLLOW_RANGE, 48)
                .add(Attributes.ARMOR, 8)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false, p -> p instanceof Player s && echterSpieler(s)));
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
        return 50;
    }

    @Override
    protected String untertitel() {
        return "Der Boden hat Zähne.";
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected int faehigkeitTakt() {
        return 260;
    }

    @Override
    protected void faehigkeit(ServerLevel level, LivingEntity ziel) {
        if (unten > 0) {
            return;
        }
        triggerAnim("aktion", "special");
        playSound(de.deadzonemc.monster.MonsterSounds.holen(art, "graben"), 4.0F, 0.8F);
        unten = 70;
        auftauchen = null;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!(level() instanceof ServerLevel sl)) {
            return;
        }
        LivingEntity ziel = getTarget();
        if (unten > 0) {
            unten--;
            setInvisible(unten < 40);
            if (unten == 35 && ziel != null) {
                // Ziel fürs Auftauchen festlegen und den Boden beben lassen
                auftauchen = ziel.position();
            }
            if (auftauchen != null && unten < 35) {
                BlockState boden = sl.getBlockState(BlockPos.containing(auftauchen).below());
                sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, boden), auftauchen.x, auftauchen.y + 0.1,
                        auftauchen.z, 25, 1.5, 0.1, 1.5, 0.1);
            }
            if (unten == 0 && auftauchen != null) {
                teleportTo(auftauchen.x, auftauchen.y, auftauchen.z);
                setInvisible(false);
                triggerAnim("auftritt", "spawn");
                playSound(de.deadzonemc.monster.MonsterSounds.holen(art, "auftauchen"), 4.0F, 1.0F);
                for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(2.5), e -> e != this)) {
                    e.hurt(damageSources().mobAttack(this), 16);
                    e.push(0, 0.9, 0);
                    e.hurtMarked = true;
                }
            } else if (unten == 0) {
                setInvisible(false);
            }
            return;
        }
        if (ziel != null) {
            getLookControl().setLookAt(ziel, 10, 10);
            if (distanceToSqr(ziel) < 6.5 * 6.5 && ++biss >= 30) {
                biss = 0;
                triggerAnim("aktion", "attack");
                for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(5.5), e -> e != this)) {
                    e.hurt(damageSources().mobAttack(this), 18);
                }
            } else if (distanceToSqr(ziel) > 20 * 20 && unten == 0) {
                faehigkeit(sl, ziel); // zu weit weg: abtauchen und nachkommen
            }
        }
    }

    @Override
    public boolean hurt(DamageSource quelle, float menge) {
        if (unten > 0 && unten < 60) {
            return false;
        }
        return super.hurt(quelle, menge);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Unten", unten);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        unten = tag.getInt("Unten");
    }

    @Override
    protected boolean wuchtangriffe() {
        return false;
    }
}
