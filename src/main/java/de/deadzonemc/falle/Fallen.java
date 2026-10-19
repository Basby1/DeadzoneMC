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

/** Fallen zur Verteidigung der Basis: Stacheldraht, Bärenfalle, Landmine, Glasscherben, Spießgrube, Brandgraben. */
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

    public static final DeferredBlock<GlasscherbenBlock> GLASSCHERBEN = BLOECKE.register("glasscherben",
            () -> new GlasscherbenBlock(BlockBehaviour.Properties.of().mapColor(MapColor.NONE).noCollission()
                    .noOcclusion().instabreak().sound(SoundType.GLASS)));
    public static final DeferredBlock<SpiessfalleBlock> SPIESSFALLE = BLOECKE.register("spiessfalle",
            () -> new SpiessfalleBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).noCollission()
                    .noOcclusion().strength(1.0F, 2.0F).sound(SoundType.WOOD)));
    public static final DeferredBlock<BrandgrabenBlock> BRANDGRABEN = BLOECKE.register("brandgraben",
            () -> new BrandgrabenBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).noOcclusion()
                    .strength(1.5F, 6.0F).sound(SoundType.STONE).requiresCorrectToolForDrops()
                    .lightLevel(s -> s.getValue(BrandgrabenBlock.BRENNT) ? 13 : 0)));
    public static final DeferredItem<BlockItem> GLASSCHERBEN_ITEM = ITEMS.registerSimpleBlockItem(GLASSCHERBEN);
    public static final DeferredItem<BlockItem> SPIESSFALLE_ITEM = ITEMS.registerSimpleBlockItem(SPIESSFALLE);
    public static final DeferredItem<BlockItem> BRANDGRABEN_ITEM = ITEMS.registerSimpleBlockItem(BRANDGRABEN);

    public static final DeferredRegister<net.minecraft.world.level.block.entity.BlockEntityType<?>> BE =
            DeferredRegister.create(net.minecraft.core.registries.Registries.BLOCK_ENTITY_TYPE, DeadzoneMC.MODID);
    public static final DeferredBlock<GeschuetzturmBlock> GESCHUETZTURM = BLOECKE.register("geschuetzturm",
            () -> new GeschuetzturmBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).noOcclusion()
                    .strength(4.0F, 8.0F).sound(SoundType.METAL).requiresCorrectToolForDrops()));
    public static final net.neoforged.neoforge.registries.DeferredHolder<net.minecraft.world.level.block.entity.BlockEntityType<?>,
            net.minecraft.world.level.block.entity.BlockEntityType<GeschuetzturmBlockEntity>> GESCHUETZTURM_BE =
            BE.register("geschuetzturm", () -> net.minecraft.world.level.block.entity.BlockEntityType.Builder
                    .of(GeschuetzturmBlockEntity::new, GESCHUETZTURM.get()).build(null));
    public static final DeferredItem<BlockItem> GESCHUETZTURM_ITEM = ITEMS.registerSimpleBlockItem(GESCHUETZTURM,
            new Item.Properties().stacksTo(4));
    /** Nur fürs Rendern des drehbaren Kopfes. */
    public static final DeferredItem<Item> GESCHUETZTURM_KOPF = ITEMS.registerSimpleItem("geschuetzturm_kopf");

    public static final DeferredItem<BlockItem> STACHELDRAHT_ITEM = ITEMS.registerSimpleBlockItem(STACHELDRAHT);
    public static final DeferredItem<BlockItem> BAERENFALLE_ITEM = ITEMS.registerSimpleBlockItem(BAERENFALLE,
            new Item.Properties().stacksTo(8));
    public static final DeferredItem<BlockItem> LANDMINE_ITEM = ITEMS.registerSimpleBlockItem(LANDMINE,
            new Item.Properties().stacksTo(8));

    public static void registrieren(IEventBus modBus) {
        BLOECKE.register(modBus);
        ITEMS.register(modBus);
        BE.register(modBus);
    }

    public static void tabFuellen(net.minecraft.world.item.CreativeModeTab.Output ausgabe) {
        ausgabe.accept(STACHELDRAHT_ITEM.get());
        ausgabe.accept(BAERENFALLE_ITEM.get());
        ausgabe.accept(LANDMINE_ITEM.get());
        ausgabe.accept(GESCHUETZTURM_ITEM.get());
        ausgabe.accept(GLASSCHERBEN_ITEM.get());
        ausgabe.accept(SPIESSFALLE_ITEM.get());
        ausgabe.accept(BRANDGRABEN_ITEM.get());
    }
}
