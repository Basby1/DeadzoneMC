package de.deadzonemc.falle;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

/** Geschützturm – Block: Rechtsklick öffnet das Munitionsfach. */
public class GeschuetzturmBlock extends BaseEntityBlock {

    public static final MapCodec<GeschuetzturmBlock> CODEC = simpleCodec(GeschuetzturmBlock::new);
    private static final VoxelShape FORM = box(2, 0, 2, 14, 16, 14);

    public GeschuetzturmBlock(Properties p) {
        super(p);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
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
        return new GeschuetzturmBlockEntity(pos, zustand);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState zustand, BlockEntityType<T> typ) {
        return createTickerHelper(typ, Fallen.GESCHUETZTURM_BE.get(),
                level.isClientSide ? GeschuetzturmBlockEntity::clientTick : GeschuetzturmBlockEntity::serverTick);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState zustand, Level level, BlockPos pos, Player spieler, BlockHitResult treffer) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof GeschuetzturmBlockEntity t) {
            spieler.openMenu(t);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onRemove(BlockState zustand, Level level, BlockPos pos, BlockState neu, boolean bewegt) {
        if (!zustand.is(neu.getBlock()) && level.getBlockEntity(pos) instanceof GeschuetzturmBlockEntity t) {
            Containers.dropContents(level, pos, t);
        }
        super.onRemove(zustand, level, pos, neu, bewegt);
    }
}
