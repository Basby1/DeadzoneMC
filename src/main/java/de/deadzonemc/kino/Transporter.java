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

/** Transporthubschrauber Q-7 im Intro: Rotor dreht, bis die Schwerkraft verschwindet; dann reißt der Rumpf auf. */
public class Transporter extends Entity implements GeoEntity {

    private static final RawAnimation FLUG = RawAnimation.begin().thenLoop("animation.transporter.flug");
    private static final RawAnimation AUSLAUFEN = RawAnimation.begin().thenPlayAndHold("animation.transporter.auslaufen");
    private static final RawAnimation ZERREISSEN = RawAnimation.begin().thenPlayAndHold("animation.transporter.zerreissen");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    public boolean rotorAn = true;

    public Transporter(EntityType<?> typ, Level level) {
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
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "flug", 0, s -> rotorAn ? s.setAndContinue(FLUG) : PlayState.STOP));
        AnimationController<Transporter> rotor = new AnimationController<>(this, "rotor", 0, s -> PlayState.STOP);
        rotor.triggerableAnim("auslaufen", AUSLAUFEN);
        controllers.add(rotor);
        AnimationController<Transporter> rumpf = new AnimationController<>(this, "rumpf", 0, s -> PlayState.STOP);
        rumpf.triggerableAnim("zerreissen", ZERREISSEN);
        controllers.add(rumpf);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
