package de.deadzonemc.monster.boss;

import de.deadzonemc.monster.MonsterTypen;
import de.deadzonemc.monster.Wanderer;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

/** Dutzende Infizierte, zu einem Fleischberg verschmolzen. Bei Treffern reißen sich Infizierte los. */
public class Verschmolzener extends Boss {

    private int abgeloest;

    public Verschmolzener(EntityType<? extends Monster> typ, Level level) {
        super(typ, level, "verschmolzener");
    }

    public static AttributeSupplier.Builder attribute() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 300)
                .add(Attributes.MOVEMENT_SPEED, 0.2)
                .add(Attributes.ATTACK_DAMAGE, 14)
                .add(Attributes.FOLLOW_RANGE, 48)
                .add(Attributes.ARMOR, 6)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.STEP_HEIGHT, 1.5);
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
        return 100;
    }

    @Override
    protected String untertitel() {
        return "Sie sind eins geworden.";
    }

    @Override
    protected int faehigkeitTakt() {
        return 300;
    }

    @Override
    protected void faehigkeit(ServerLevel level, LivingEntity ziel) {
        triggerAnim("aktion", "special");
        playSound(de.deadzonemc.monster.MonsterSounds.holen(art, "brullen"), 4.0F, 0.9F);
        for (Player p : level.getEntitiesOfClass(Player.class, getBoundingBox().inflate(14))) {
            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 1));
            p.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0));
        }
        for (Monster m : level.getEntitiesOfClass(Monster.class, getBoundingBox().inflate(40), m -> m != this && m.getTarget() == null)) {
            m.setTarget(ziel);
        }
    }

    @Override
    public boolean hurt(DamageSource quelle, float menge) {
        boolean ok = super.hurt(quelle, menge);
        if (ok && level() instanceof ServerLevel sl && !imAuftritt() && abgeloest < 12 && random.nextFloat() < 0.22F) {
            Wanderer w = MonsterTypen.WANDERER.get().create(sl);
            if (w != null) {
                abgeloest++;
                w.moveTo(getX() + random.nextGaussian(), getY() + 1.5, getZ() + random.nextGaussian(), random.nextFloat() * 360, 0);
                w.finalizeSpawn(sl, sl.getCurrentDifficultyAt(blockPosition()), MobSpawnType.MOB_SUMMONED, null);
                w.setTarget(getTarget());
                sl.addFreshEntity(w);
                sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.REDSTONE_BLOCK.defaultBlockState()),
                        w.getX(), w.getY() + 1, w.getZ(), 30, 0.4, 0.6, 0.4, 0.2);
                playSound(de.deadzonemc.monster.MonsterSounds.holen(art, "fusion"), 2.0F, 1.2F);
            }
        }
        return ok;
    }
}
