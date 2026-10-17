package de.deadzonemc.mensch;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** Wache einer Zuflucht: bleibt auf ihrem Posten und schießt alles nieder, was sich den Mauern nähert. */
public class Wache extends Soldat {

    public Wache(EntityType<? extends PathfinderMob> typ, Level level) {
        super(typ, level);
    }

    public static AttributeSupplier.Builder attribute() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 40)
                .add(Attributes.MOVEMENT_SPEED, 0.27)
                .add(Attributes.ATTACK_DAMAGE, 5)
                .add(Attributes.FOLLOW_RANGE, 48)
                .add(Attributes.ARMOR, 8);
    }

    @Override
    public Fraktion fraktion() {
        return Fraktion.ZUFLUCHT;
    }

    @Override
    protected void getoetetVon(Player p) {
        Ansehen.aendern(p, Fraktion.ZUFLUCHT, -40);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new Schiessen(this));
        goalSelector.addGoal(4, new MoveTowardsRestrictionGoal(this, 1.0));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.5));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 12.0F));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this, Wache.class).setAlertOthers());
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Monster.class, true));
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
                e -> e instanceof Player p && !p.isCreative() && feindlich(p)));
    }
}
