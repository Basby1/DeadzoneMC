package de.deadzonemc.monster;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/** Infizierte Hunde im Rudel: umkreisen ihr Opfer und beißen zu, sobald es ihnen den Rücken zudreht. */
public class Seuchenhund extends DeadzoneMonster {

    public Seuchenhund(EntityType<? extends Monster> typ, Level level) {
        super(typ, level, "seuchenhund");
    }

    public static AttributeSupplier.Builder attribute() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 14)
                .add(Attributes.MOVEMENT_SPEED, 0.36)
                .add(Attributes.ATTACK_DAMAGE, 4)
                .add(Attributes.FOLLOW_RANGE, 40);
    }

    @Override
    protected boolean hatSpezial() {
        return true;
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(2, new Umkreisen(this));
    }

    @Override
    public void setTarget(LivingEntity ziel) {
        boolean neu = ziel != null && getTarget() == null;
        super.setTarget(ziel);
        if (neu && !level().isClientSide) {
            triggerAnim("aktion", "special");
            playSound(MonsterSounds.holen(art, "heulen"), 2.0F, 0.9F + random.nextFloat() * 0.2F);
            for (Seuchenhund h : level().getEntitiesOfClass(Seuchenhund.class, getBoundingBox().inflate(16), h -> h.getTarget() == null)) {
                h.setTarget(ziel);
            }
        }
    }

    /** Solange das Opfer herschaut, wird gekreist; dreht es sich weg, wird zugebissen (MeleeAttackGoal übernimmt). */
    static class Umkreisen extends Goal {
        private final Seuchenhund h;
        private double winkel;

        Umkreisen(Seuchenhund h) {
            this.h = h;
            this.winkel = h.getRandom().nextDouble() * Math.PI * 2;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        private boolean schautHer(LivingEntity z) {
            Vec3 blick = z.getViewVector(1.0F).multiply(1, 0, 1).normalize();
            Vec3 zuHund = h.position().subtract(z.position()).multiply(1, 0, 1).normalize();
            return blick.dot(zuHund) > 0.45;
        }

        @Override
        public boolean canUse() {
            LivingEntity z = h.getTarget();
            return z != null && z.isAlive() && h.distanceToSqr(z) < 14 * 14 && schautHer(z) && h.getRandom().nextInt(4) != 0;
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity z = h.getTarget();
            return z != null && z.isAlive() && schautHer(z) && h.getHealth() > 4;
        }

        @Override
        public void tick() {
            LivingEntity z = h.getTarget();
            winkel += 0.08;
            double r = 5.5;
            h.getNavigation().moveTo(z.getX() + Math.cos(winkel) * r, z.getY(), z.getZ() + Math.sin(winkel) * r, 1.3);
            h.getLookControl().setLookAt(z);
        }
    }
}
