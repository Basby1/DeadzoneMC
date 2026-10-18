package de.deadzonemc.welt.schild;

import com.mojang.serialization.MapCodec;
import de.deadzonemc.DeadzoneMC;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Leuchtschilder an Stadtgebäuden: POLIZEI, APOTHEKE, WAFFEN … Ein Schild besteht aus mehreren Blöcken
 * nebeneinander (teil 0..4), jeder zeigt ein Stück der Schrift auf seiner Vorderseite. So sieht man schon von
 * Weitem, wo es sich zu plündern lohnt. Schwach beleuchtet – nachts gerade noch zu erkennen.
 */
public class Ladenschild extends HorizontalDirectionalBlock {

    public enum Art implements StringRepresentable {
        POLIZEI, WAFFEN, APOTHEKE, KRANKENHAUS, SUPERMARKT, TANKSTELLE, WERKSTATT, BAUMARKT, MILITAER, BANK;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    public static final EnumProperty<Art> ART = EnumProperty.create("art", Art.class);
    public static final IntegerProperty TEIL = IntegerProperty.create("teil", 0, 4);
    public static final MapCodec<Ladenschild> CODEC = simpleCodec(Ladenschild::new);

    public Ladenschild(BlockBehaviour.Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(FACING, net.minecraft.core.Direction.NORTH)
                .setValue(ART, Art.POLIZEI).setValue(TEIL, 0));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(FACING, ART, TEIL);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    // ---- Registrierung ------------------------------------------------------------------------

    public static final DeferredRegister.Blocks BLOECKE = DeferredRegister.createBlocks(DeadzoneMC.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(DeadzoneMC.MODID);

    public static final DeferredBlock<Ladenschild> SCHILD = BLOECKE.register("ladenschild",
            () -> new Ladenschild(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).strength(2.0F, 6.0F)
                    .sound(SoundType.METAL).lightLevel(z -> 4)));
    public static final DeferredItem<BlockItem> SCHILD_ITEM = ITEMS.registerSimpleBlockItem(SCHILD);

    public static void registrieren(IEventBus modBus) {
        BLOECKE.register(modBus);
        ITEMS.register(modBus);
    }
}
