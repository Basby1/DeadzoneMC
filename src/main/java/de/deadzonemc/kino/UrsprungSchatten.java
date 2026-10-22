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

/** Der Ursprung als riesiger Schatten im Nebel – nur bei Blitzen zu sehen, beim zweiten öffnet er das Auge. */
public class UrsprungSchatten extends Entity implements GeoEntity {

    private static final RawAnimation RUHE = RawAnimation.begin().thenLoop("animation.der_ursprung.idle");
    private static final RawAnimation PULS = RawAnimation.begin().thenPlay("animation.der_ursprung.special");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    public boolean augeOffen;
    /** 0..1: wie sichtbar (der Renderer zeichnet nur, wenn > 0). */
    public float sichtbar;

    public UrsprungSchatten(EntityType<?> typ, Level level) {
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
        baseTick();
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double abstand) {
        return true;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "ruhe", 0, s -> s.setAndContinue(RUHE)));
        AnimationController<UrsprungSchatten> aktion = new AnimationController<>(this, "aktion", 4, s -> PlayState.STOP);
        aktion.triggerableAnim("puls", PULS);
        controllers.add(aktion);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
