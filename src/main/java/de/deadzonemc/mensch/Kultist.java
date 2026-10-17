package de.deadzonemc.mensch;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** Aschekind: verehrt den Nebel, wandert nachts und im Nebel. Die Monster lassen sie in Ruhe. */
public class Kultist extends Mensch {

    public Kultist(EntityType<? extends PathfinderMob> typ, Level level) {
        super(typ, level);
    }

    public static AttributeSupplier.Builder attribute() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 22)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 5)
                .add(Attributes.FOLLOW_RANGE, 32);
    }

    @Override
    public Fraktion fraktion() {
        return Fraktion.ASCHEKINDER;
    }

    @Override
    protected void getoetetVon(Player p) {
        Ansehen.aendern(p, Fraktion.ASCHEKINDER, -25);
        Ansehen.aendern(p, Fraktion.WACHT, 10);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, false));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.6));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 12.0F));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this, Kultist.class).setAlertOthers());
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Soldat.class, true));
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
                e -> e instanceof Player p && !p.isCreative() && feindlich(p)));
    }

    /** Aschekinder misstrauen allen, bis man ihnen geholfen hat: schon bei leicht negativem Ansehen feindlich. */
    @Override
    public boolean feindlich(Player p) {
        return Ansehen.holen(p, Fraktion.ASCHEKINDER) < -10;
    }
}
