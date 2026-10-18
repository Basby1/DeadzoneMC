package de.deadzonemc.falle;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Bärenfalle: schnappt zu, sobald etwas hineintritt – hält es fest und verletzt es schwer. Mit Rechtsklick
 * wieder spannen. Schleichende Spieler lösen sie nicht aus.
 */
public class BaerenfalleBlock extends HorizontalDirectionalBlock {

    public static final MapCodec<BaerenfalleBlock> CODEC = simpleCodec(BaerenfalleBlock::new);
    public static final BooleanProperty GESPANNT = BooleanProperty.create("gespannt");
    private static final VoxelShape OFFEN = Block.box(1, 0, 1, 15, 2, 15);
    private static final VoxelShape ZU = Block.box(3, 0, 3, 13, 6, 13);

    public BaerenfalleBlock(Properties eigenschaften) {
        super(eigenschaften);
        registerDefaultState(stateDefinition.any().setValue(GESPANNT, true).setValue(FACING, net.minecraft.core.Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(GESPANNT, FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection());
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), net.minecraft.core.Direction.UP);
    }

    @Override
    protected BlockState updateShape(BlockState state, net.minecraft.core.Direction richtung, BlockState nachbar,
                                     net.minecraft.world.level.LevelAccessor level, BlockPos pos, BlockPos nachbarPos) {
        return !canSurvive(state, level, pos) ? net.minecraft.world.level.block.Blocks.AIR.defaultBlockState()
                : super.updateShape(state, richtung, nachbar, level, pos, nachbarPos);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return state.getValue(GESPANNT) ? OFFEN : ZU;
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity e) {
        if (level.isClientSide || !(e instanceof LivingEntity le) || !state.getValue(GESPANNT)) {
            return;
        }
        if (e instanceof Player p && (p.isCreative() || p.isShiftKeyDown())) {
            return;
        }
        if (le.getBbWidth() < 0.4F) {
            return; // zu klein (Ratten, Hühner)
        }
        level.setBlock(pos, state.setValue(GESPANNT, false), 3);
        level.playSound(null, pos, SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 1.5F, 0.6F);
        level.playSound(null, pos, SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.BLOCKS, 1.0F, 0.5F);
        le.hurt(level.damageSources().generic(), le instanceof Player ? 6.0F : 10.0F);
        le.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, 9, false, false, true));
        le.setDeltaMovement(Vec3.ZERO);
        le.teleportTo(pos.getX() + 0.5, le.getY(), pos.getZ() + 0.5);
        if (level instanceof ServerLevel sl) {
            sl.sendParticles(net.minecraft.core.particles.ParticleTypes.CRIT, pos.getX() + 0.5, pos.getY() + 0.3,
                    pos.getZ() + 0.5, 12, 0.3, 0.2, 0.3, 0.2);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player p, BlockHitResult treffer) {
        if (state.getValue(GESPANNT)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            level.setBlock(pos, state.setValue(GESPANNT, true), 3);
            level.playSound(null, pos, SoundEvents.CROSSBOW_LOADING_END.value(), SoundSource.BLOCKS, 1.0F, 0.7F);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
