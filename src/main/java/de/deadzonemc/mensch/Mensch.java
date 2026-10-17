package de.deadzonemc.mensch;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/** Grundlage aller Menschen in der Deadzone: Soldaten, Kultisten, Überlebende. */
public abstract class Mensch extends PathfinderMob implements GeoEntity {

    protected static final RawAnimation RUHE = RawAnimation.begin().thenLoop("animation.mensch.idle");
    protected static final RawAnimation GEHEN = RawAnimation.begin().thenLoop("animation.mensch.walk");
    protected static final RawAnimation RENNEN = RawAnimation.begin().thenLoop("animation.mensch.run");
    protected static final RawAnimation ANGRIFF = RawAnimation.begin().thenPlay("animation.mensch.attack");
    protected static final RawAnimation SCHUSS = RawAnimation.begin().thenPlay("animation.mensch.special");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    protected Mensch(EntityType<? extends PathfinderMob> typ, Level level) {
        super(typ, level);
    }

    /** Zu welcher Fraktion er gehört (oder null). */
    public abstract Fraktion fraktion();

    /** Ob er diesen Spieler angreift. */
    public boolean feindlich(Player p) {
        Fraktion f = fraktion();
        return f != null && Ansehen.krieg(p, f);
    }

    @Override
    public boolean doHurtTarget(Entity ziel) {
        boolean ok = super.doHurtTarget(ziel);
        if (ok) {
            triggerAnim("aktion", "attack");
        }
        return ok;
    }

    @Override
    public void die(DamageSource quelle) {
        super.die(quelle);
        if (!level().isClientSide && quelle.getEntity() instanceof Player p && fraktion() != null) {
            getoetetVon(p);
        }
    }

    /** Was das Töten für das Ansehen bedeutet. */
    protected void getoetetVon(Player p) {
        Ansehen.aendern(p, fraktion(), -25);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar c) {
        c.add(new AnimationController<>(this, "bewegung", 5, s -> {
            if (s.isMoving()) {
                return s.setAndContinue(isAggressive() ? RENNEN : GEHEN);
            }
            return s.setAndContinue(RUHE);
        }));
        AnimationController<Mensch> aktion = new AnimationController<>(this, "aktion", 2, s -> PlayState.STOP);
        aktion.triggerableAnim("attack", ANGRIFF);
        aktion.triggerableAnim("special", SCHUSS);
        c.add(aktion);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource quelle) {
        return SoundEvents.PLAYER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.PLAYER_DEATH;
    }

    @Override
    public boolean removeWhenFarAway(double abstand) {
        return !hasCustomName() || abstand > 128 * 128;
    }
}
