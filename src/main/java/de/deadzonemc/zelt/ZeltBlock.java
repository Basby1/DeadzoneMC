package de.deadzonemc.zelt;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

/**
 * Kleines Zelt: Zwischenlager (18 Plätze, der Inhalt bleibt beim Abbauen drin) und – geduckt benutzt – ein
 * Rückkehrpunkt draußen in der Zone.
 */
public class ZeltBlock extends BaseEntityBlock {

    public static final MapCodec<ZeltBlock> CODEC = simpleCodec(ZeltBlock::new);
    public static final net.minecraft.world.level.block.state.properties.DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    private static final VoxelShape FORM = box(0, 0, 0, 16, 9, 16);

    public ZeltBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    protected RenderShape getRenderShape(BlockState zustand) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState zustand, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return FORM;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState zustand) {
        return new ZeltBlockEntity(pos, zustand);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState zustand, Level level, BlockPos pos, Player spieler, BlockHitResult treffer) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (spieler.isShiftKeyDown() && spieler instanceof ServerPlayer sp) {
            // Rückkehrpunkt neben dem Zelt (im Zelt selbst wäre kein Platz zum Aufstehen)
            BlockPos platz = pos.above();
            for (Direction d : Direction.Plane.HORIZONTAL) {
                BlockPos n = pos.relative(d);
                if (level.getBlockState(n).getCollisionShape(level, n).isEmpty()
                        && level.getBlockState(n.above()).getCollisionShape(level, n.above()).isEmpty()
                        && level.getBlockState(n.below()).isFaceSturdy(level, n.below(), Direction.UP)) {
                    platz = n;
                    break;
                }
            }
            sp.setRespawnPosition(level.dimension(), platz, sp.getYRot(), true, false);
            sp.displayClientMessage(Component.translatable("zelt.deadzonemc.rueckkehr").withStyle(ChatFormatting.GREEN), true);
            level.playSound(null, pos, SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 1.0F, 0.8F);
            return InteractionResult.CONSUME;
        }
        if (level.getBlockEntity(pos) instanceof ZeltBlockEntity z) {
            spieler.openMenu(z);
            level.playSound(null, pos, SoundEvents.ARMOR_EQUIP_LEATHER.value(), SoundSource.BLOCKS, 0.8F, 0.9F);
        }
        return InteractionResult.CONSUME;
    }
}
