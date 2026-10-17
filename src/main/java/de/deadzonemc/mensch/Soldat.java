package de.deadzonemc.mensch;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/** Soldat der Wacht: patrouilliert, schießt auf Monster und Aschekinder – und auf euch, wenn Krieg herrscht. */
public class Soldat extends Mensch {

    public Soldat(EntityType<? extends PathfinderMob> typ, Level level) {
        super(typ, level);
    }

    public static AttributeSupplier.Builder attribute() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 30)
                .add(Attributes.MOVEMENT_SPEED, 0.28)
                .add(Attributes.ATTACK_DAMAGE, 4)
                .add(Attributes.FOLLOW_RANGE, 40)
                .add(Attributes.ARMOR, 6);
    }

    @Override
    public Fraktion fraktion() {
        return Fraktion.WACHT;
    }

    @Override
    protected void getoetetVon(Player p) {
        Ansehen.aendern(p, Fraktion.WACHT, -25);
        Ansehen.aendern(p, Fraktion.ASCHEKINDER, 5);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new Schiessen(this));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.7));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 12.0F));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this, Soldat.class).setAlertOthers());
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Monster.class, 10, true, false, e -> !(e instanceof de.deadzonemc.monster.boss.Kommandant)));
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Kultist.class, true));
        targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
                e -> e instanceof Player p && !p.isCreative() && feindlich(p)));
    }

    /** Hält Abstand und schießt (Treffer per Sichtlinie, Trefferchance sinkt mit der Entfernung). */
    static class Schiessen extends Goal {
        private final Soldat s;
        private int abklingen = 20;

        Schiessen(Soldat s) {
            this.s = s;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity z = s.getTarget();
            return z != null && z.isAlive() && s.distanceToSqr(z) < 32 * 32;
        }

        @Override
        public void start() {
            s.setAggressive(true);
        }

        @Override
        public void stop() {
            s.setAggressive(false);
            s.getNavigation().stop();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity z = s.getTarget();
            if (z == null) {
                return;
            }
            s.getLookControl().setLookAt(z, 30, 30);
            double d = s.distanceTo(z);
            boolean sicht = s.getSensing().hasLineOfSight(z);
            if (d > 16 || !sicht) {
                s.getNavigation().moveTo(z, 1.0);
            } else if (d < 6) {
                Vec3 weg = s.position().subtract(z.position()).normalize().scale(4);
                s.getNavigation().moveTo(s.getX() + weg.x, s.getY(), s.getZ() + weg.z, 1.0);
            } else {
                s.getNavigation().stop();
            }
            if (--abklingen <= 0 && sicht) {
                abklingen = 22 + s.getRandom().nextInt(16);
                schiessen(z, d);
            }
        }

        private void schiessen(LivingEntity z, double d) {
            if (!(s.level() instanceof ServerLevel sl)) {
                return;
            }
            s.triggerAnim("aktion", "special");
            sl.playSound(null, s.getX(), s.getY(), s.getZ(), SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.HOSTILE, 3.0F, 0.55F);
            Vec3 von = s.getEyePosition();
            Vec3 nach = z.getEyePosition().subtract(0, 0.4, 0);
            for (int i = 1; i < 10; i++) {
                Vec3 p = von.lerp(nach, i / 10.0);
                sl.sendParticles(ParticleTypes.CRIT, p.x, p.y, p.z, 1, 0, 0, 0, 0);
            }
            double treffer = Math.max(0.35, Math.min(0.92, 1.0 - d / 40.0));
            if (s.getRandom().nextDouble() < treffer) {
                z.hurt(s.damageSources().mobAttack(s), 5.0F + s.getRandom().nextInt(3));
            }
        }
    }
}
