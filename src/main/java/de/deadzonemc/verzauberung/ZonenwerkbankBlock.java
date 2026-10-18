package de.deadzonemc.verzauberung;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;

/** Die Zonenwerkbank: Hier werden Waffen und Rüstung mit Zonen-Verzauberungen verbessert. */
public class ZonenwerkbankBlock extends HorizontalDirectionalBlock {

    public static final MapCodec<ZonenwerkbankBlock> CODEC = simpleCodec(ZonenwerkbankBlock::new);

    public ZonenwerkbankBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(FACING, net.minecraft.core.Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
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
    protected InteractionResult useWithoutItem(BlockState zustand, Level level, BlockPos pos, Player spieler, BlockHitResult treffer) {
        if (!level.isClientSide) {
            spieler.openMenu(getMenuProvider(zustand, level, pos));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected MenuProvider getMenuProvider(BlockState zustand, Level level, BlockPos pos) {
        return new SimpleMenuProvider((id, inv, p) -> new ZonenwerkbankMenu(id, inv, ContainerLevelAccess.create(level, pos)),
                Component.translatable("container.deadzonemc.zonenwerkbank"));
    }
}
