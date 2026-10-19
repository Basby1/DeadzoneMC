package de.deadzonemc.beute;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Motorhaube eines Autowracks: Mit dem Brecheisen aufgehebelt gibt sie Fahrzeugteile her (Räder, Batterie,
 * Kolben, Kabel – für die Autos aus dem Ultimate Car Mod), dazu Schrott und Elektronik. Einmal pro Wrack.
 */
public class MotorraumBlock extends Block {

    public static final BooleanProperty OFFEN = BooleanProperty.create("offen");
    private static final VoxelShape FORM = Block.box(0, 0, 0, 16, 8, 16);
    private static final String[][] TEILE = {{"wheel", "0.45"}, {"battery", "0.35"}, {"engine_piston", "0.5"},
            {"cable_insulator", "0.4"}, {"repair_kit", "0.2"}, {"canister", "0.25"}, {"engine_3_cylinder", "0.05"}};

    public MotorraumBlock(Properties eigenschaften) {
        super(eigenschaften);
        registerDefaultState(stateDefinition.any().setValue(OFFEN, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(OFFEN);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return FORM;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player p, BlockHitResult treffer) {
        if (!level.isClientSide) {
            p.displayClientMessage(Component.translatable(state.getValue(OFFEN) ? "block.deadzonemc.motorraum.leer"
                    : "block.deadzonemc.motorraum.zu").withStyle(ChatFormatting.GRAY), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** Vom Brecheisen aufgerufen, wenn die Haube aufgehebelt ist. */
    public static void ausschlachten(ServerLevel sl, BlockPos pos, ServerPlayer p) {
        BlockState s = sl.getBlockState(pos);
        if (!(s.getBlock() instanceof MotorraumBlock) || s.getValue(OFFEN)) {
            return;
        }
        sl.setBlock(pos, s.setValue(OFFEN, true), Block.UPDATE_ALL);
        sl.playSound(null, pos, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.6F, 1.4F);
        var r = sl.random;
        java.util.List<ItemStack> beute = new java.util.ArrayList<>();
        beute.add(new ItemStack(de.deadzonemc.ModRegistry.SCHROTT.get(), 2 + r.nextInt(4)));
        if (r.nextFloat() < 0.45F) {
            beute.add(new ItemStack(de.deadzonemc.ModRegistry.ELEKTRONIK.get(), 1 + r.nextInt(2)));
        }
        if (net.neoforged.fml.ModList.get().isLoaded("car")) {
            for (String[] t : TEILE) {
                if (r.nextFloat() < Float.parseFloat(t[1])) {
                    BuiltInRegistries.ITEM.getOptional(ResourceLocation.fromNamespaceAndPath("car", t[0]))
                            .ifPresent(i -> beute.add(new ItemStack(i, t[0].equals("engine_piston") ? 1 + r.nextInt(3) : 1)));
                }
            }
        }
        for (ItemStack st : beute) {
            Block.popResource(sl, pos.above(), st);
        }
        p.displayClientMessage(Component.translatable("block.deadzonemc.motorraum.geschafft").withStyle(ChatFormatting.GOLD), true);
    }
}
