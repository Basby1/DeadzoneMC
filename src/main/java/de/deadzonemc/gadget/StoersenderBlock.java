package de.deadzonemc.gadget;

import com.mojang.serialization.MapCodec;
import de.deadzonemc.ModRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Störsender: mit Batterien betrieben (je Batterie 3 Minuten). Infizierte im Umkreis verlieren immer wieder ihr
 * Ziel, Drohnen und Wachkugeln stürzen ab. Techniker bauen ihn mit größerer Reichweite auf.
 */
public class StoersenderBlock extends BaseEntityBlock {

    public static final MapCodec<StoersenderBlock> CODEC = simpleCodec(StoersenderBlock::new);
    public static final BooleanProperty AN = BooleanProperty.create("an");
    private static final VoxelShape FORM = Block.box(3, 0, 3, 13, 12, 13);

    public StoersenderBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(AN, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(AN);
    }

    @Override
    protected VoxelShape getShape(BlockState s, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return FORM;
    }

    @Override
    protected RenderShape getRenderShape(BlockState s) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState s) {
        return new StoersenderBlockEntity(pos, s);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState s, net.minecraft.world.entity.LivingEntity wer, ItemStack stack) {
        if (!level.isClientSide && wer instanceof Player p && level.getBlockEntity(pos) instanceof StoersenderBlockEntity be) {
            be.techniker = de.deadzonemc.klasse.Klassen.klasse(p) == de.deadzonemc.klasse.Klasse.TECHNIKER;
        }
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState s, Level level, BlockPos pos, Player p,
                                              InteractionHand hand, BlockHitResult t) {
        if (!stack.is(ModRegistry.BATTERIE.get()) || !(level.getBlockEntity(pos) instanceof StoersenderBlockEntity be)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide) {
            be.laden(3600);
            if (!p.getAbilities().instabuild) {
                stack.shrink(1);
            }
            level.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 0.6F, 1.6F);
            p.displayClientMessage(Component.translatable("block.deadzonemc.stoersender.laufzeit", be.restMinuten())
                    .withStyle(ChatFormatting.AQUA), true);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState s, Level level, BlockPos pos, Player p, BlockHitResult t) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof StoersenderBlockEntity be) {
            p.displayClientMessage(Component.translatable(be.restMinuten() > 0 ? "block.deadzonemc.stoersender.laufzeit"
                    : "block.deadzonemc.stoersender.leer", be.restMinuten()).withStyle(ChatFormatting.AQUA), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState s, BlockEntityType<T> typ) {
        return level.isClientSide ? null : createTickerHelper(typ, Gadgets.STOERSENDER_BE.get(), StoersenderBlockEntity::tick);
    }
}
