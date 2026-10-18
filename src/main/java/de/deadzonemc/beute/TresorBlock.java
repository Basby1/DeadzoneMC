package de.deadzonemc.beute;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Tresor: verschlossen, voll mit Wertvollem. Aufbrechen geht nur mit dem Brecheisen – und das dauert und
 * macht Lärm, der Infizierte anlockt.
 */
public class TresorBlock extends BaseEntityBlock {

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty OFFEN = BooleanProperty.create("offen");
    public static final MapCodec<TresorBlock> CODEC = simpleCodec(TresorBlock::new);
    private static final VoxelShape FORM = Block.box(0, 0, 0, 16, 16, 16);

    public TresorBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(OFFEN, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(FACING, OFFEN);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    protected RenderShape getRenderShape(BlockState s) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
        return FORM;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TresorBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player spieler, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof TresorBlockEntity t)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!t.istGeknackt()) {
            spieler.displayClientMessage(Component.translatable("block.deadzonemc.tresor.zu").withStyle(ChatFormatting.GRAY), true);
            return InteractionResult.CONSUME;
        }
        spieler.openMenu(t);
        return InteractionResult.CONSUME;
    }

    @Override
    protected void onRemove(BlockState alt, Level level, BlockPos pos, BlockState neu, boolean bewegt) {
        if (!alt.is(neu.getBlock()) && level.getBlockEntity(pos) instanceof TresorBlockEntity t && t.istGeknackt()) {
            net.minecraft.world.Containers.dropContents(level, pos, t);
        }
        super.onRemove(alt, level, pos, neu, bewegt);
    }
}
