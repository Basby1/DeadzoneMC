package de.deadzonemc.monster;

import de.deadzonemc.wetter.WetterArt;
import de.deadzonemc.wetter.WetterDaten;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Gemeinsame Grundlage aller Deadzone-Monster: GeckoLib-Animationen (Ruhe, Gehen, Rennen, Angriff,
 * Spezialbewegung), Standard-Verhalten und Geräusche.
 */
public abstract class DeadzoneMonster extends Monster implements GeoEntity {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    protected final String art;
    protected final RawAnimation ruhe;
    protected final RawAnimation gehen;
    protected final RawAnimation rennen;
    protected final RawAnimation angriff;
    protected final RawAnimation spezial;

    protected DeadzoneMonster(EntityType<? extends Monster> typ, Level level, String art) {
        super(typ, level);
        this.art = art;
        this.ruhe = RawAnimation.begin().thenLoop("animation." + art + ".idle");
        this.gehen = RawAnimation.begin().thenLoop("animation." + art + ".walk");
        this.rennen = RawAnimation.begin().thenLoop("animation." + art + "." + (hatLauf() ? "run" : "walk"));
        this.angriff = RawAnimation.begin().thenPlay("animation." + art + ".attack");
        this.spezial = RawAnimation.begin().thenPlay("animation." + art + ".special");
    }

    // ---- Lärm -------------------------------------------------------------------------------

    private Vec3 laermZiel;
    private int laermRest;

    /** Hat ein Geräusch gehört: läuft eine Weile dorthin (solange es kein Ziel hat). */
    public void laermGehoert(Vec3 ort) {
        if (this instanceof de.deadzonemc.monster.boss.Boss) {
            return;
        }
        laermZiel = ort.add(random.nextGaussian() * 2, 0, random.nextGaussian() * 2);
        laermRest = 600;
        getNavigation().moveTo(laermZiel.x, laermZiel.y, laermZiel.z, 1.15);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (laermZiel != null) {
            if (--laermRest <= 0 || getTarget() != null || position().distanceToSqr(laermZiel) < 4) {
                laermZiel = null;
            } else if (tickCount % 20 == 0 && getNavigation().isDone()) {
                getNavigation().moveTo(laermZiel.x, laermZiel.y, laermZiel.z, 1.15);
            }
        }
    }

    /** Ob das Modell eine eigene Renn-Animation hat. */
    protected boolean hatLauf() {
        return true;
    }

    /** Ob das Modell eine Spezial-Animation hat (Schrei, Hervorspringen, Platzen …). */
    protected boolean hatSpezial() {
        return false;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.15, false));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 10.0F));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public boolean doHurtTarget(Entity ziel) {
        boolean getroffen = super.doHurtTarget(ziel);
        if (getroffen) {
            triggerAnim("aktion", "attack");
        }
        return getroffen;
    }

    // ---- Animationen ------------------------------------------------------------------------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "bewegung", 5, this::bewegung));
        AnimationController<DeadzoneMonster> aktion = new AnimationController<>(this, "aktion", 2, s -> PlayState.STOP);
        aktion.triggerableAnim("attack", angriff);
        if (hatSpezial()) {
            aktion.triggerableAnim("special", spezial);
        }
        controllers.add(aktion);
    }

    protected PlayState bewegung(AnimationState<DeadzoneMonster> zustand) {
        if (zustand.isMoving()) {
            return zustand.setAndContinue(isAggressive() ? rennen : gehen);
        }
        return zustand.setAndContinue(ruhe);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    // ---- Geräusche --------------------------------------------------------------------------

    @Override
    protected SoundEvent getAmbientSound() {
        return MonsterSounds.holen(art, "ambient");
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource quelle) {
        return MonsterSounds.holen(art, "hurt");
    }

    @Override
    protected SoundEvent getDeathSound() {
        return MonsterSounds.holen(art, "death");
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState block) {
        SoundEvent schritt = MonsterSounds.holen(art, "step");
        if (schritt != null) {
            playSound(schritt, 0.35F, 1.0F);
        } else {
            super.playStepSound(pos, block);
        }
    }

    // ---- Hilfen -----------------------------------------------------------------------------

    /** Ob in der Oberwelt gerade dichter Nebel herrscht. */
    public static boolean dichterNebel(Level level) {
        if (level instanceof ServerLevel sl) {
            return WetterDaten.holen(sl.getServer().overworld()).art() == WetterArt.DICHTER_NEBEL;
        }
        return false;
    }

    /** Spieler, die als Ziel taugen (nicht im Kreativ- oder Zuschauermodus). */
    public static boolean echterSpieler(Player p) {
        return p.isAlive() && !p.isCreative() && !p.isSpectator();
    }
}
