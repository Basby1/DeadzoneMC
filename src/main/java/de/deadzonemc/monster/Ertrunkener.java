package de.deadzonemc.monster;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.PathType;

/** Wasserleiche: läuft über den Grund von Flüssen und Kanälen und zieht Schwimmer in die Tiefe. */
public class Ertrunkener extends DeadzoneMonster {

    public Ertrunkener(EntityType<? extends Monster> typ, Level level) {
        super(typ, level, "ertrunkener");
        setPathfindingMalus(PathType.WATER, 0.0F);
    }

    public static AttributeSupplier.Builder attribute() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 32)
                .add(Attributes.MOVEMENT_SPEED, 0.17)
                .add(Attributes.ATTACK_DAMAGE, 5)
                .add(Attributes.FOLLOW_RANGE, 28)
                .add(Attributes.WATER_MOVEMENT_EFFICIENCY, 1.0)
                .add(Attributes.OXYGEN_BONUS, 1000);
    }

    @Override
    protected boolean hatSpezial() {
        return true;
    }

    @Override
    protected void registerGoals() {
        // kein FloatGoal: er sinkt und läuft über den Grund
        goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.2, false));
        goalSelector.addGoal(6, new RandomStrollGoal(this, 0.8));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 10.0F));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public boolean canBreatheUnderwater() {
        return true;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        LivingEntity z = getTarget();
        if (z != null && z.isInWater() && isInWater() && distanceToSqr(z) < 3 * 3 && level() instanceof ServerLevel sl) {
            // zieht nach unten und raubt die Luft
            z.setDeltaMovement(z.getDeltaMovement().multiply(0.5, 1, 0.5).add(0, -0.12, 0));
            z.hurtMarked = true;
            if (tickCount % 10 == 0) {
                z.setAirSupply(Math.max(-19, z.getAirSupply() - 30));
                sl.sendParticles(ParticleTypes.BUBBLE_COLUMN_UP, z.getX(), z.getY() + 1, z.getZ(), 8, 0.3, 0.3, 0.3, 0.05);
            }
        }
        if (isInWater() && tickCount % 40 == 0) {
            triggerAnim("aktion", "special");
        }
    }
}
