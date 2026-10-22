package de.deadzonemc.kino;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Patrouillenboot Kilo-2 im Boot-Intro: Radar dreht, Flagge flattert, der Suchscheinwerfer streicht übers Wasser.
 * Schaukeln und Kentern setzt das Drehbuch über {@link #nick} und {@link #roll}.
 */
public class KinoBoot extends Entity implements GeoEntity {

    private static final RawAnimation FAHRT = RawAnimation.begin().thenLoop("animation.kino_boot.fahrt");
    private static final RawAnimation LICHT_AUS = RawAnimation.begin().thenPlayAndHold("animation.kino_boot.licht_aus");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    public float nick, nickO, roll, rollO;
    public boolean motorAn = true;

    public KinoBoot(EntityType<?> typ, Level level) {
        super(typ, level);
        noPhysics = true;
        setNoGravity(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    public void tick() {
        nickO = nick;
        rollO = roll;
        baseTick();
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double abstand) {
        return true;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "fahrt", 0, s -> motorAn ? s.setAndContinue(FAHRT) : PlayState.STOP));
        AnimationController<KinoBoot> licht = new AnimationController<>(this, "licht", 0, s -> PlayState.STOP);
        licht.triggerableAnim("aus", LICHT_AUS);
        controllers.add(licht);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
