package de.deadzonemc.monster;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

/** Kleine, schnelle Kreaturen, die in Gruppen aus Kanälen, Kellern und dem Moor strömen. */
public class Schwarm extends DeadzoneMonster {

    public Schwarm(EntityType<? extends Monster> typ, Level level) {
        super(typ, level, "schwarm");
    }

    public static AttributeSupplier.Builder attribute() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 4)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 2)
                .add(Attributes.FOLLOW_RANGE, 24);
    }

    @Override
    protected boolean hatLauf() {
        return false;
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(2, new LeapAtTargetGoal(this, 0.35F));
    }

    @Override
    public int getMaxSpawnClusterSize() {
        return 16;
    }
}
