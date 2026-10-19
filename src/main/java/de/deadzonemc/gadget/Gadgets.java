package de.deadzonemc.gadget;

import de.deadzonemc.DeadzoneMC;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Klassen-Ausrüstung (jeder darf sie benutzen, die Lieblingsklasse holt mehr heraus) und Lockrufe für die Endbosse.
 */
public final class Gadgets {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(DeadzoneMC.MODID);
    public static final DeferredRegister.Blocks BLOECKE = DeferredRegister.createBlocks(DeadzoneMC.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BE = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, DeadzoneMC.MODID);

    public static final DeferredItem<GluehhammerItem> GLUEHHAMMER = ITEMS.register("gluehhammer",
            () -> new GluehhammerItem(Tiers.IRON, new Item.Properties().durability(900).rarity(Rarity.RARE)
                    .attributes(net.minecraft.world.item.SwordItem.createAttributes(Tiers.IRON, 9, -3.3F))));
    public static final DeferredItem<AdrenalinItem> ADRENALIN = ITEMS.register("adrenalinspritze",
            () -> new AdrenalinItem(new Item.Properties().stacksTo(8).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<Item> PATRONENGURT = ITEMS.register("patronengurt",
            () -> new HinweisItem("patronengurt", new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<SanitaetstascheItem> SANITAETSTASCHE = ITEMS.register("sanitaetstasche",
            () -> new SanitaetstascheItem(new Item.Properties().durability(64).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<FernglasItem> FERNGLAS = ITEMS.register("fernglas",
            () -> new FernglasItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<LockrufItem> KERNRESONATOR = ITEMS.register("kernresonator",
            () -> new LockrufItem("das_herz", new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
    public static final DeferredItem<LockrufItem> LEERENSIGNAL = ITEMS.register("leerensignal",
            () -> new LockrufItem("der_ursprung", new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));

    public static final DeferredBlock<StoersenderBlock> STOERSENDER = BLOECKE.register("stoersender",
            () -> new StoersenderBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2.5F, 6.0F)
                    .sound(SoundType.METAL).noOcclusion().requiresCorrectToolForDrops()
                    .lightLevel(s -> s.getValue(StoersenderBlock.AN) ? 6 : 0)));
    public static final DeferredItem<BlockItem> STOERSENDER_ITEM = ITEMS.registerSimpleBlockItem(STOERSENDER);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<StoersenderBlockEntity>> STOERSENDER_BE =
            BE.register("stoersender", () -> BlockEntityType.Builder.of(StoersenderBlockEntity::new, STOERSENDER.get()).build(null));

    private Gadgets() {
    }

    public static void registrieren(IEventBus modBus) {
        ITEMS.register(modBus);
        BLOECKE.register(modBus);
        BE.register(modBus);
    }

    /** Hat der Spieler einen Patronengurt im Inventar? (schneller nachladen) */
    public static boolean hatGurt(net.minecraft.world.entity.player.Player p) {
        return p.getInventory().contains(new net.minecraft.world.item.ItemStack(PATRONENGURT.get()));
    }

    public static void tabFuellen(net.minecraft.world.item.CreativeModeTab.Output ausgabe) {
        ausgabe.accept(GLUEHHAMMER.get());
        ausgabe.accept(ADRENALIN.get());
        ausgabe.accept(PATRONENGURT.get());
        ausgabe.accept(SANITAETSTASCHE.get());
        ausgabe.accept(STOERSENDER_ITEM.get());
        ausgabe.accept(FERNGLAS.get());
        ausgabe.accept(KERNRESONATOR.get());
        ausgabe.accept(LEERENSIGNAL.get());
    }
}
