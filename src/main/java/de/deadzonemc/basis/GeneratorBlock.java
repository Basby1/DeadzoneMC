package de.deadzonemc.basis;

import com.mojang.serialization.MapCodec;
import de.deadzonemc.ModRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Der Notstromgenerator: Herz jeder Basis. Er legt den geschützten Bereich fest, ist der Respawnpunkt und
 * lässt sich mit Elektronikteilen ausbauen (größerer Radius).
 */
public class GeneratorBlock extends HorizontalDirectionalBlock {

    public static final MapCodec<GeneratorBlock> CODEC = simpleCodec(GeneratorBlock::new);
    public static final IntegerProperty STUFE = IntegerProperty.create("stufe", 1, 3);
    private static final VoxelShape FORM = Block.box(1, 0, 1, 15, 13, 15);
    /** Schutzradius je Ausbaustufe (Rückmeldung 18.10.: mindestens viermal so groß wie vorher). */
    public static final int[] RADIUS = {0, 96, 160, 256};
    private static final int[] KOSTEN = {0, 0, 8, 16};

    public GeneratorBlock(Properties eigenschaften) {
        super(eigenschaften);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(STUFE, 1));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(FACING, STUFE);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState zustand, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return FORM;
    }

    public static int radius(BlockState zustand) {
        return RADIUS[zustand.getValue(STUFE)];
    }

    @Override
    protected void onPlace(BlockState zustand, Level level, BlockPos pos, BlockState alt, boolean bewegt) {
        super.onPlace(zustand, level, pos, alt, bewegt);
        if (level instanceof ServerLevel sl) {
            Basen.holen(sl).setzen(pos, radius(zustand));
        }
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState zustand, LivingEntity wer, ItemStack stack) {
        if (level instanceof ServerLevel sl) {
            if (wer instanceof ServerPlayer p) {
                p.setRespawnPosition(sl.dimension(), pos.above(), 0, true, false);
                p.displayClientMessage(Component.translatable("basis.deadzonemc.gegruendet", radius(zustand))
                        .withStyle(ChatFormatting.GREEN), false);
            }
            sl.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.0F, 0.6F);
        }
    }

    @Override
    protected void onRemove(BlockState zustand, Level level, BlockPos pos, BlockState neu, boolean bewegt) {
        if (!zustand.is(neu.getBlock()) && level instanceof ServerLevel sl) {
            Basen.holen(sl).entfernen(pos);
            sl.playSound(null, pos, SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 1.0F, 0.6F);
        }
        super.onRemove(zustand, level, pos, neu, bewegt);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState zustand, Level level, BlockPos pos, Player spieler,
                                              InteractionHand hand, BlockHitResult treffer) {
        int stufe = zustand.getValue(STUFE);
        if (!stack.is(ModRegistry.ELEKTRONIK.get()) || stufe >= 3) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        int kosten = KOSTEN[stufe + 1];
        if (stack.getCount() < kosten && !spieler.isCreative()) {
            spieler.displayClientMessage(Component.translatable("basis.deadzonemc.zu_wenig", kosten), true);
            return ItemInteractionResult.FAIL;
        }
        if (level instanceof ServerLevel sl) {
            BlockState neu = zustand.setValue(STUFE, stufe + 1);
            sl.setBlock(pos, neu, 3);
            Basen.holen(sl).setzen(pos, radius(neu));
            stack.consume(kosten, spieler);
            sl.playSound(null, pos, SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 1.0F, 0.8F);
            spieler.displayClientMessage(Component.translatable("basis.deadzonemc.ausgebaut", stufe + 1, radius(neu))
                    .withStyle(ChatFormatting.GREEN), false);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState zustand, Level level, BlockPos pos, Player spieler, BlockHitResult treffer) {
        if (level instanceof ServerLevel sl && spieler instanceof ServerPlayer p) {
            p.setRespawnPosition(sl.dimension(), pos.above(), 0, true, false);
            int stufe = zustand.getValue(STUFE);
            Component naechste = stufe < 3
                    ? Component.translatable("basis.deadzonemc.naechste", KOSTEN[stufe + 1], RADIUS[stufe + 1])
                    : Component.translatable("basis.deadzonemc.max");
            p.displayClientMessage(Component.translatable("basis.deadzonemc.info", stufe, radius(zustand),
                    Component.translatable("basis.deadzonemc.stufe." + Basen.stufe(sl).id())), false);
            p.displayClientMessage(naechste.copy().withStyle(ChatFormatting.GRAY), false);
            sl.playSound(null, pos, SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.BLOCKS, 0.6F, 1.2F);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void animateTick(BlockState zustand, Level level, BlockPos pos, RandomSource z) {
        if (z.nextInt(4) == 0) {
            level.addParticle(ParticleTypes.SMOKE, pos.getX() + 0.75, pos.getY() + 1.0, pos.getZ() + 0.75, 0, 0.04, 0);
        }
        if (z.nextInt(80) == 0) {
            level.playLocalSound(pos, SoundEvents.BEACON_AMBIENT, SoundSource.BLOCKS, 0.5F, 0.5F, false);
        }
    }
}
