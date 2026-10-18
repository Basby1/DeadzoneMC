package de.deadzonemc.zone;

import de.deadzonemc.DeadzoneMC;
import de.deadzonemc.monster.Herden;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Eine Versorgungskiste am Fallschirm. Schwebt langsam herab und wird beim Aufsetzen zur echten Kiste –
 * mit einer roten Rauchsäule, die man weit sieht (und die Infizierten auch).
 */
public class Abwurfkiste extends Entity implements GeoEntity {

    public static final ResourceKey<LootTable> BEUTE = ResourceKey.create(Registries.LOOT_TABLE,
            ResourceLocation.fromNamespaceAndPath(DeadzoneMC.MODID, "kiste/luftabwurf"));
    private static final RawAnimation SCHWEBEN = RawAnimation.begin().thenLoop("animation.abwurfkiste.schweben");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public Abwurfkiste(EntityType<? extends Abwurfkiste> typ, Level level) {
        super(typ, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public void tick() {
        super.tick();
        Vec3 v = getDeltaMovement();
        double pendel = Math.sin(tickCount * 0.05) * 0.02;
        setDeltaMovement(pendel, Math.max(v.y - 0.01, -0.16), Math.cos(tickCount * 0.04) * 0.015);
        move(MoverType.SELF, getDeltaMovement());
        if (level().isClientSide) {
            if (tickCount % 3 == 0) {
                level().addParticle(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, getX(), getY() - 0.2, getZ(), 0, -0.02, 0);
            }
            if (tickCount % 2 == 0) {
                level().addParticle(ParticleTypes.FLAME, getX(), getY() - 0.3, getZ(), 0, -0.05, 0);
            }
            return;
        }
        if (tickCount % 60 == 1) {
            level().playSound(null, getX(), getY(), getZ(), SoundEvents.ELYTRA_FLYING, SoundSource.AMBIENT, 1.5F, 0.5F);
        }
        if (onGround() || isInWater() || tickCount > 2400) {
            aufsetzen((ServerLevel) level(), blockPosition());
            discard();
        }
    }

    /** Stellt die Kiste mit Signalfeuer auf den Boden. Auch für Abwürfe, die niemand fallen gesehen hat. */
    public static BlockPos aufsetzen(ServerLevel level, BlockPos wo) {
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, wo.getX(), wo.getZ());
        BlockPos kiste = new BlockPos(wo.getX(), y, wo.getZ());
        if (!level.getFluidState(kiste.below()).isEmpty()) {
            level.setBlock(kiste.below(), Blocks.OAK_PLANKS.defaultBlockState(), 3); // treibt auf dem Wasser
        }
        level.setBlock(kiste, Blocks.BARREL.defaultBlockState().setValue(net.minecraft.world.level.block.BarrelBlock.FACING, Direction.UP), 3);
        if (level.getBlockEntity(kiste) instanceof RandomizableContainerBlockEntity behaelter) {
            behaelter.setLootTable(BEUTE, level.random.nextLong());
        }
        // Rauchsignal: Lagerfeuer auf Heu steigt sehr hoch
        BlockPos feuer = kiste.relative(Direction.Plane.HORIZONTAL.getRandomDirection(level.random), 2);
        int fy = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, feuer.getX(), feuer.getZ());
        feuer = new BlockPos(feuer.getX(), fy, feuer.getZ());
        if (level.getFluidState(feuer.below()).isEmpty() && level.getBlockState(feuer).canBeReplaced()) {
            level.setBlock(feuer.below(), Blocks.HAY_BLOCK.defaultBlockState(), 3);
            level.setBlock(feuer, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.SIGNAL_FIRE, true), 3);
        }
        level.playSound(null, kiste, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 1.2F, 0.5F);
        level.sendParticles(ParticleTypes.CLOUD, kiste.getX() + 0.5, kiste.getY() + 0.5, kiste.getZ() + 0.5, 30, 1, 0.3, 1, 0.05);
        // Lärm und Rauch locken eine Herde an
        Herden.herdeSpawnen(level, Vec3.atCenterOf(kiste), null, true);
        return kiste;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double abstand) {
        return abstand < 256 * 256;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "schweben", 0, s -> s.setAndContinue(SCHWEBEN)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
