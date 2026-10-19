package de.deadzonemc.bossmod;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Trophäenbrett: ein Holzbrett für die Wand, an das man einen Boss-Kopf (oder sonst etwas) hängt. */
public class TrophaeenbrettBlock extends BaseEntityBlock {

    public static final MapCodec<TrophaeenbrettBlock> CODEC = simpleCodec(TrophaeenbrettBlock::new);
    private static final VoxelShape N = Block.box(1, 1, 14, 15, 15, 16);
    private static final VoxelShape S = Block.box(1, 1, 0, 15, 15, 2);
    private static final VoxelShape W = Block.box(14, 1, 1, 16, 15, 15);
    private static final VoxelShape O = Block.box(0, 1, 1, 2, 15, 15);

    public TrophaeenbrettBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(HorizontalDirectionalBlock.FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Direction d = ctx.getClickedFace().getAxis().isHorizontal() ? ctx.getClickedFace() : ctx.getHorizontalDirection().getOpposite();
        return defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, d);
    }

    @Override
    protected VoxelShape getShape(BlockState s, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return switch (s.getValue(HorizontalDirectionalBlock.FACING)) {
            case SOUTH -> S;
            case WEST -> W;
            case EAST -> O;
            default -> N;
        };
    }

    @Override
    protected boolean canSurvive(BlockState s, LevelReader level, BlockPos pos) {
        Direction d = s.getValue(HorizontalDirectionalBlock.FACING);
        BlockPos hinten = pos.relative(d.getOpposite());
        return level.getBlockState(hinten).isFaceSturdy(level, hinten, d);
    }

    @Override
    protected RenderShape getRenderShape(BlockState s) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState s) {
        return new TrophaeenbrettBlockEntity(pos, s);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState s, Level level, BlockPos pos, Player p,
                                              InteractionHand hand, BlockHitResult treffer) {
        if (!(level.getBlockEntity(pos) instanceof TrophaeenbrettBlockEntity be) || !be.ding().isEmpty() || stack.isEmpty()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide) {
            be.setzen(stack.copyWithCount(1));
            if (!p.getAbilities().instabuild) {
                stack.shrink(1);
            }
            level.playSound(null, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 1.0F, 0.8F);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState s, Level level, BlockPos pos, Player p, BlockHitResult treffer) {
        if (!(level.getBlockEntity(pos) instanceof TrophaeenbrettBlockEntity be) || be.ding().isEmpty()) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            ItemStack d = be.ding();
            be.setzen(ItemStack.EMPTY);
            if (!p.addItem(d)) {
                p.drop(d, false);
            }
            level.playSound(null, pos, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 1.0F, 0.8F);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onRemove(BlockState s, Level level, BlockPos pos, BlockState neu, boolean bewegt) {
        if (!s.is(neu.getBlock()) && level.getBlockEntity(pos) instanceof TrophaeenbrettBlockEntity be && !be.ding().isEmpty()) {
            Block.popResource(level, pos, be.ding());
        }
        super.onRemove(s, level, pos, neu, bewegt);
    }

    @Override
    protected BlockState updateShape(BlockState s, Direction richtung, BlockState nachbar, net.minecraft.world.level.LevelAccessor level,
                                     BlockPos pos, BlockPos nachbarPos) {
        return richtung == s.getValue(HorizontalDirectionalBlock.FACING).getOpposite() && !canSurvive(s, level, pos)
                ? net.minecraft.world.level.block.Blocks.AIR.defaultBlockState()
                : super.updateShape(s, richtung, nachbar, level, pos, nachbarPos);
    }
}
