package de.deadzonemc.monster;

import de.deadzonemc.DeadzoneMC;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.BreakDoorGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

/** Häufigster Gegner: schlurft ziellos, sprintet plötzlich los, bricht auf „Schwer“ Türen auf. */
public class Wanderer extends DeadzoneMonster {

    private static final ResourceLocation SPRINT = ResourceLocation.fromNamespaceAndPath(DeadzoneMC.MODID, "sprint");
    private int sprint = 0;

    public Wanderer(EntityType<? extends Monster> typ, Level level) {
        super(typ, level, "wanderer");
        if (getNavigation() instanceof GroundPathNavigation nav) {
            nav.setCanOpenDoors(true);
        }
    }

    public static AttributeSupplier.Builder attribute() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 22)
                .add(Attributes.MOVEMENT_SPEED, 0.21)
                .add(Attributes.ATTACK_DAMAGE, 4)
                .add(Attributes.FOLLOW_RANGE, 40)
                .add(Attributes.ARMOR, 2);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(2, new BreakDoorGoal(this, d -> d == Difficulty.HARD));
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        AttributeInstance tempo = getAttribute(Attributes.MOVEMENT_SPEED);
        if (tempo == null) {
            return;
        }
        if (sprint > 0) {
            if (--sprint == 0) {
                tempo.removeModifier(SPRINT);
            }
        } else if (getTarget() != null && random.nextInt(140) == 0) {
            // ohne Vorwarnung lossprinten
            sprint = 40 + random.nextInt(40);
            if (!tempo.hasModifier(SPRINT)) {
                tempo.addTransientModifier(new AttributeModifier(SPRINT, 0.9, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
            }
        }
    }
}
