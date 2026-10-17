package de.deadzonemc.monster;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WallClimberNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

/** Jäger: klettert an Wänden und Decken, springt von oben auf seine Beute, nimmt keinen Fallschaden. */
public class Kriecher extends DeadzoneMonster {

    private static final EntityDataAccessor<Boolean> KLETTERT =
            SynchedEntityData.defineId(Kriecher.class, EntityDataSerializers.BOOLEAN);

    public Kriecher(EntityType<? extends Monster> typ, Level level) {
        super(typ, level, "kriecher");
    }

    public static AttributeSupplier.Builder attribute() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 20)
                .add(Attributes.MOVEMENT_SPEED, 0.34)
                .add(Attributes.ATTACK_DAMAGE, 5)
                .add(Attributes.FOLLOW_RANGE, 32);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(2, new LeapAtTargetGoal(this, 0.5F));
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new WallClimberNavigation(this, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(KLETTERT, false);
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide) {
            entityData.set(KLETTERT, horizontalCollision);
        }
    }

    @Override
    public boolean onClimbable() {
        return entityData.get(KLETTERT);
    }

    @Override
    public boolean causeFallDamage(float strecke, float faktor, DamageSource quelle) {
        return false;
    }
}
