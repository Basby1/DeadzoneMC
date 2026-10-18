package de.deadzonemc.zone;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Vector3f;

import javax.annotation.Nullable;

/**
 * Eine Anomalie: unsichtbar, ohne Hitbox, unzerstörbar. Verrät sich nur durch feine Partikel – wer
 * genau hinsieht (oder einen Detektor oder Bolzen benutzt), lebt länger.
 */
public class AnomalieBlock extends BaseEntityBlock {

    public static final EnumProperty<AnomalieArt> ART = EnumProperty.create("art", AnomalieArt.class);
    public static final MapCodec<AnomalieBlock> CODEC = simpleCodec(AnomalieBlock::new);

    public AnomalieBlock(Properties eigenschaften) {
        super(eigenschaften);
        registerDefaultState(stateDefinition.any().setValue(ART, AnomalieArt.STRUDEL));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ART);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return Shapes.empty();
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return Shapes.empty();
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AnomalieBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> typ) {
        return level.isClientSide ? null : createTickerHelper(typ, Zone.ANOMALIE_BE.get(), AnomalieBlockEntity::tick);
    }

    /** Feine Hinweise, je nach Art. Nur aus der Nähe zu sehen. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource z) {
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.5;
        double zz = pos.getZ() + 0.5;
        switch (state.getValue(ART)) {
            case STRUDEL -> {
                if (z.nextInt(2) == 0) {
                    double w = z.nextDouble() * Math.PI * 2;
                    double r = 1.2 + z.nextDouble() * 1.5;
                    level.addParticle(new DustParticleOptions(new Vector3f(0.45F, 0.42F, 0.38F), 0.8F),
                            x + Math.cos(w) * r, pos.getY() + 0.1 + z.nextDouble() * 1.6, zz + Math.sin(w) * r,
                            -Math.cos(w) * 0.12, 0.02, -Math.sin(w) * 0.12);
                }
                if (z.nextInt(14) == 0) {
                    level.addParticle(ParticleTypes.FALLING_SPORE_BLOSSOM, x, y + 1.2, zz, 0, 0, 0);
                }
            }
            case GLUTFELD -> {
                if (z.nextInt(3) == 0) {
                    level.addParticle(ParticleTypes.SMOKE, x + z.nextGaussian() * 0.6, pos.getY() + 0.05,
                            zz + z.nextGaussian() * 0.6, 0, 0.03, 0);
                }
                if (z.nextInt(20) == 0) {
                    level.addParticle(ParticleTypes.SMALL_FLAME, x + z.nextGaussian() * 0.4, pos.getY() + 0.1,
                            zz + z.nextGaussian() * 0.4, 0, 0.02, 0);
                }
            }
            case ENTLADUNG -> {
                if (z.nextInt(4) == 0) {
                    level.addParticle(ParticleTypes.ELECTRIC_SPARK, x + z.nextGaussian() * 0.8, y + z.nextGaussian() * 0.6,
                            zz + z.nextGaussian() * 0.8, 0, 0, 0);
                }
            }
            case SAEURE -> {
                if (z.nextInt(2) == 0) {
                    level.addParticle(new DustParticleOptions(new Vector3f(0.55F, 0.85F, 0.2F), 1.4F),
                            x + z.nextGaussian() * 1.4, pos.getY() + z.nextDouble() * 0.9, zz + z.nextGaussian() * 1.4,
                            0, 0.005, 0);
                }
            }
        }
    }
}
