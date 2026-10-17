package de.deadzonemc.monster;

import de.deadzonemc.strahlung.Strahlung;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
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
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** Arbeiter im Schutzanzug mit leckendem Tank: verstrahlt die Umgebung und sprüht grüne Brühe. */
public class Seuchentraeger extends DeadzoneMonster {

    private static final DustParticleOptions GIFT = new DustParticleOptions(new Vector3f(0.6F, 1.0F, 0.3F), 1.0F);
    private int spruehen;

    public Seuchentraeger(EntityType<? extends Monster> typ, Level level) {
        super(typ, level, "seuchentraeger");
    }

    public static AttributeSupplier.Builder attribute() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 28)
                .add(Attributes.MOVEMENT_SPEED, 0.19)
                .add(Attributes.ATTACK_DAMAGE, 5)
                .add(Attributes.FOLLOW_RANGE, 32)
                .add(Attributes.ARMOR, 4);
    }

    @Override
    protected boolean hatSpezial() {
        return true;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!(level() instanceof ServerLevel sl)) {
            return;
        }
        if (tickCount % 20 == 0) {
            for (Player p : sl.getEntitiesOfClass(Player.class, getBoundingBox().inflate(6))) {
                Strahlung.bestrahlen(p, 1.5F);
            }
            sl.sendParticles(GIFT, getX(), getY() + 0.8, getZ(), 3, 0.4, 0.4, 0.4, 0);
        }
        LivingEntity z = getTarget();
        if (z != null && ++spruehen >= 70 && distanceToSqr(z) < 7 * 7 && getSensing().hasLineOfSight(z)) {
            spruehen = 0;
            triggerAnim("aktion", "attack");
            playSound(MonsterSounds.holen(art, "spruehen"), 1.5F, 1.0F);
            Vec3 von = getEyePosition().subtract(0, 0.5, 0);
            Vec3 nach = z.position().add(0, 0.6, 0);
            for (int i = 1; i <= 12; i++) {
                Vec3 p = von.lerp(nach, i / 12.0);
                sl.sendParticles(GIFT, p.x, p.y, p.z, 3, 0.1, 0.1, 0.1, 0);
            }
            if (z instanceof Player p) {
                Strahlung.bestrahlen(p, 25);
            }
            z.addEffect(new MobEffectInstance(MobEffects.POISON, 80, 0));
        }
    }

    @Override
    public void die(DamageSource quelle) {
        super.die(quelle);
        if (level() instanceof ServerLevel sl) {
            AreaEffectCloud wolke = new AreaEffectCloud(sl, getX(), getY(), getZ());
            wolke.setRadius(3.0F);
            wolke.setDuration(120);
            wolke.setRadiusPerTick(-0.015F);
            wolke.setParticle(GIFT);
            wolke.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 0));
            sl.addFreshEntity(wolke);
        }
    }
}
