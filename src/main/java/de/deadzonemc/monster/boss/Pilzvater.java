package de.deadzonemc.monster.boss;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.joml.Vector3f;

/** Von Pilzen überwucherter Riese: Sporenwolken, Wurzeln halten Spieler fest, im Regen heilt er sich. */
public class Pilzvater extends Boss {

    private static final DustParticleOptions SPORE = new DustParticleOptions(new Vector3f(0.55F, 1.0F, 0.88F), 1.2F);
    private boolean wurzeln;

    public Pilzvater(EntityType<? extends Monster> typ, Level level) {
        super(typ, level, "pilzvater");
    }

    public static AttributeSupplier.Builder attribute() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 280)
                .add(Attributes.MOVEMENT_SPEED, 0.2)
                .add(Attributes.ATTACK_DAMAGE, 15)
                .add(Attributes.FOLLOW_RANGE, 40)
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
        return 80;
    }

    @Override
    protected String untertitel() {
        return "Der Wald atmet Sporen.";
    }

    @Override
    protected int faehigkeitTakt() {
        return 180;
    }

    @Override
    protected void faehigkeit(ServerLevel level, LivingEntity ziel) {
        wurzeln = !wurzeln;
        triggerAnim("aktion", "special");
        if (wurzeln) {
            // Wurzeln brechen aus dem Boden und halten fest
            for (Player p : level.getEntitiesOfClass(Player.class, getBoundingBox().inflate(7))) {
                p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 5));
                p.hurt(damageSources().mobAttack(this), 4);
                level.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, p.getX(), p.getY() + 0.2, p.getZ(), 30, 0.6, 0.2, 0.6, 0);
            }
        } else {
            playSound(de.deadzonemc.monster.MonsterSounds.holen(art, "sporen"), 3.0F, 0.8F);
            AreaEffectCloud wolke = new AreaEffectCloud(level, ziel.getX(), ziel.getY(), ziel.getZ());
            wolke.setOwner(this);
            wolke.setRadius(4.5F);
            wolke.setDuration(160);
            wolke.setRadiusPerTick(-0.01F);
            wolke.setParticle(SPORE);
            wolke.addEffect(new MobEffectInstance(MobEffects.POISON, 80, 0));
            wolke.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 120, 0));
            level.addFreshEntity(wolke);
        }
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (level() instanceof ServerLevel sl && tickCount % 40 == 0) {
            if (sl.isRainingAt(blockPosition().above(3))) {
                heal(4);
            }
            sl.sendParticles(SPORE, getX(), getY() + 3.2, getZ(), 6, 1.2, 0.6, 1.2, 0);
        }
    }
}
