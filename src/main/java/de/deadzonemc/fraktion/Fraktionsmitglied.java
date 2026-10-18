package de.deadzonemc.fraktion;

import de.deadzonemc.mensch.Ansehen;
import de.deadzonemc.mensch.Fraktion;
import de.deadzonemc.mensch.Mensch;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** Mitglied einer der sechs neuen Fraktionen: spricht, vergibt Aufträge, verteidigt sich und seine Leute. */
public class Fraktionsmitglied extends Mensch {

    private final Fraktion fraktion;

    public Fraktionsmitglied(EntityType<? extends PathfinderMob> typ, Level level, Fraktion fraktion) {
        super(typ, level);
        this.fraktion = fraktion;
        if (fraktion == Fraktion.TIEFENVOLK) {
            setPathfindingMalus(net.minecraft.world.level.pathfinder.PathType.WATER, 0.0F);
        }
    }

    public static AttributeSupplier.Builder attribute() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 28)
                .add(Attributes.MOVEMENT_SPEED, 0.28)
                .add(Attributes.ATTACK_DAMAGE, 6)
                .add(Attributes.FOLLOW_RANGE, 32);
    }

    @Override
    public Fraktion fraktion() {
        return fraktion;
    }

    @Override
    public boolean feindlich(Player p) {
        return fraktion != null && Fraktionen.feindlich(p, fraktion);
    }

    @Override
    protected void getoetetVon(Player p) {
        Ansehen.aendern(p, fraktion, -25);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, false));
        goalSelector.addGoal(4, new MoveTowardsRestrictionGoal(this, 0.8));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.55));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 10.0F));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this, Fraktionsmitglied.class).setAlertOthers());
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Monster.class, 10, true, false,
                e -> !(e instanceof de.deadzonemc.monster.boss.Boss)));
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
                e -> e instanceof Player p && !p.isCreative() && feindlich(p)));
    }

    @Override
    protected InteractionResult mobInteract(Player p, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        if (p instanceof ServerPlayer sp) {
            getLookControl().setLookAt(sp);
            getNavigation().stop();
            Fraktionen.sprechen(sp, fraktion);
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    @Override
    public boolean removeWhenFarAway(double d) {
        return false;
    }
}
