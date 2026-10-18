package de.deadzonemc.zelt;

import de.deadzonemc.DeadzoneMC;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Zelte: tragbares Zwischenlager und Rückkehrpunkt. */
public final class Zelte {

    private Zelte() {
    }

    public static final DeferredRegister.Blocks BLOECKE = DeferredRegister.createBlocks(DeadzoneMC.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(DeadzoneMC.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BE = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, DeadzoneMC.MODID);

    public static final DeferredBlock<ZeltBlock> ZELT = BLOECKE.register("zelt",
            () -> new ZeltBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GREEN).strength(0.6F)
                    .sound(SoundType.WOOL).noOcclusion().ignitedByLava()));
    public static final DeferredItem<BlockItem> ZELT_ITEM = ITEMS.register("zelt",
            () -> new BlockItem(ZELT.get(), new Item.Properties().stacksTo(1)));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ZeltBlockEntity>> ZELT_BE = BE.register("zelt",
            () -> BlockEntityType.Builder.of(ZeltBlockEntity::new, ZELT.get()).build(null));

    public static void registrieren(IEventBus modBus) {
        BLOECKE.register(modBus);
        ITEMS.register(modBus);
        BE.register(modBus);
    }

    public static void tabFuellen(net.minecraft.world.item.CreativeModeTab.Output ausgabe) {
        ausgabe.accept(ZELT_ITEM.get());
    }
}
