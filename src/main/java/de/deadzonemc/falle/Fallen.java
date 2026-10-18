package de.deadzonemc.falle;

import de.deadzonemc.DeadzoneMC;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Fallen zur Verteidigung der Basis: Stacheldraht, Bärenfalle, Landmine. */
public final class Fallen {

    private Fallen() {
    }

    public static final DeferredRegister.Blocks BLOECKE = DeferredRegister.createBlocks(DeadzoneMC.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(DeadzoneMC.MODID);

    public static final DeferredBlock<StacheldrahtBlock> STACHELDRAHT = BLOECKE.register("stacheldraht",
            () -> new StacheldrahtBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).noCollission()
                    .noOcclusion().strength(2.0F, 6.0F).sound(SoundType.CHAIN).requiresCorrectToolForDrops()));
    public static final DeferredBlock<BaerenfalleBlock> BAERENFALLE = BLOECKE.register("baerenfalle",
            () -> new BaerenfalleBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).noCollission()
                    .noOcclusion().strength(1.5F, 6.0F).sound(SoundType.METAL)));
    public static final DeferredBlock<LandmineBlock> LANDMINE = BLOECKE.register("landmine",
            () -> new LandmineBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GREEN).noCollission()
                    .noOcclusion().strength(0.8F, 0.0F).sound(SoundType.METAL)));

    public static final DeferredItem<BlockItem> STACHELDRAHT_ITEM = ITEMS.registerSimpleBlockItem(STACHELDRAHT);
    public static final DeferredItem<BlockItem> BAERENFALLE_ITEM = ITEMS.registerSimpleBlockItem(BAERENFALLE,
            new Item.Properties().stacksTo(8));
    public static final DeferredItem<BlockItem> LANDMINE_ITEM = ITEMS.registerSimpleBlockItem(LANDMINE,
            new Item.Properties().stacksTo(8));

    public static void registrieren(IEventBus modBus) {
        BLOECKE.register(modBus);
        ITEMS.register(modBus);
    }

    public static void tabFuellen(net.minecraft.world.item.CreativeModeTab.Output ausgabe) {
        ausgabe.accept(STACHELDRAHT_ITEM.get());
        ausgabe.accept(BAERENFALLE_ITEM.get());
        ausgabe.accept(LANDMINE_ITEM.get());
    }
}
