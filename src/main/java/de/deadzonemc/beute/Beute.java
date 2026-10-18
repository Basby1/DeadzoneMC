package de.deadzonemc.beute;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import de.deadzonemc.DeadzoneMC;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.List;
import java.util.Map;

/**
 * Das Beutesystem: Tresore mit Brecheisen, Nachtsichtgerät, Gefahrenbonus (je gefährlicher der Ort, desto
 * besser die Beute), Fundmeldungen bei seltenen Stücken und Nachschub (geplünderte Behälter füllen sich nach
 * einigen Tagen wieder – die Welt bleibt endlos spielbar).
 */
public final class Beute {

    private Beute() {
    }

    public static final DeferredRegister.Blocks BLOECKE = DeferredRegister.createBlocks(DeadzoneMC.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(DeadzoneMC.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BE = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, DeadzoneMC.MODID);
    public static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> GLM =
            DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, DeadzoneMC.MODID);
    public static final DeferredRegister<DataComponentType<?>> KOMPONENTEN = DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, DeadzoneMC.MODID);
    public static final DeferredRegister<ArmorMaterial> RUESTUNG = DeferredRegister.create(Registries.ARMOR_MATERIAL, DeadzoneMC.MODID);
    public static final DeferredRegister<SoundEvent> KLAENGE = DeferredRegister.create(Registries.SOUND_EVENT, DeadzoneMC.MODID);

    // ---- Komponenten ----
    /** Markiert frisch erzeugte, besondere Beute – beim ersten Öffnen gibt es eine Fundmeldung. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> FUND = KOMPONENTEN.register("fund",
            () -> DataComponentType.<Boolean>builder().persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL).build());
    /** Restlaufzeit des Nachtsichtgeräts in Ticks. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> LADUNG = KOMPONENTEN.register("ladung",
            () -> DataComponentType.<Integer>builder().persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT).build());

    // ---- Tresor ----
    public static final DeferredBlock<TresorBlock> TRESOR = BLOECKE.register("tresor",
            () -> new TresorBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(25.0F, 1200.0F)
                    .sound(SoundType.NETHERITE_BLOCK).requiresCorrectToolForDrops().noOcclusion()));
    public static final DeferredItem<BlockItem> TRESOR_ITEM = ITEMS.registerSimpleBlockItem(TRESOR);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TresorBlockEntity>> TRESOR_BE = BE.register("tresor",
            () -> BlockEntityType.Builder.of(TresorBlockEntity::new, TRESOR.get()).build(null));

    // ---- Werkzeuge ----
    /** Startwaffe: stumpf, rostig, besser als nichts. */
    public static final DeferredItem<SwordItem> ROSTIGES_MESSER = ITEMS.register("rostiges_messer",
            () -> new SwordItem(Tiers.STONE, new Item.Properties().durability(160)
                    .attributes(SwordItem.createAttributes(Tiers.STONE, 1, -2.0F))));
    public static final DeferredItem<BrecheisenItem> BRECHEISEN = ITEMS.register("brecheisen",
            () -> new BrecheisenItem(Tiers.IRON, new Item.Properties().durability(96)
                    .attributes(SwordItem.createAttributes(Tiers.IRON, 2, -2.9F))));

    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> NACHTSICHT_MATERIAL = RUESTUNG.register("nachtsicht",
            () -> new ArmorMaterial(Map.of(ArmorItem.Type.HELMET, 1, ArmorItem.Type.CHESTPLATE, 1,
                    ArmorItem.Type.LEGGINGS, 1, ArmorItem.Type.BOOTS, 1, ArmorItem.Type.BODY, 1), 5,
                    net.minecraft.sounds.SoundEvents.ARMOR_EQUIP_IRON, () -> net.minecraft.world.item.crafting.Ingredient.EMPTY,
                    List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(DeadzoneMC.MODID, "nachtsicht"))), 0F, 0F));
    public static final DeferredItem<NachtsichtItem> NACHTSICHT = ITEMS.register("nachtsichtgeraet",
            () -> new NachtsichtItem(NACHTSICHT_MATERIAL, new Item.Properties().stacksTo(1).rarity(Rarity.RARE)
                    .durability(ArmorItem.Type.HELMET.getDurability(10))));

    // ---- Beute-Modifikator ----
    public static final DeferredHolder<MapCodec<? extends IGlobalLootModifier>, MapCodec<GefahrenBonus>> GEFAHRENBONUS =
            GLM.register("gefahrenbonus", () -> GefahrenBonus.CODEC);

    // ---- Klänge ----
    public static final DeferredHolder<SoundEvent, SoundEvent> FUND_SELTEN = klang("fund.selten");
    public static final DeferredHolder<SoundEvent, SoundEvent> FUND_EPISCH = klang("fund.episch");
    public static final DeferredHolder<SoundEvent, SoundEvent> TRESOR_KNACKEN = klang("tresor.knacken");
    public static final DeferredHolder<SoundEvent, SoundEvent> TRESOR_AUF = klang("tresor.auf");

    private static DeferredHolder<SoundEvent, SoundEvent> klang(String name) {
        String voll = "monster." + name;
        return KLAENGE.register(voll, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(DeadzoneMC.MODID, voll)));
    }

    public static void registrieren(IEventBus modBus) {
        BLOECKE.register(modBus);
        ITEMS.register(modBus);
        BE.register(modBus);
        GLM.register(modBus);
        KOMPONENTEN.register(modBus);
        RUESTUNG.register(modBus);
        KLAENGE.register(modBus);
        NeoForge.EVENT_BUS.addListener(Fundmeldung::geoeffnet);
        NeoForge.EVENT_BUS.addListener(Nachschub::angeklickt);
        NeoForge.EVENT_BUS.addListener(Nachschub::tick);
        NeoForge.EVENT_BUS.addListener(NachtsichtItem::tick);
        NeoForge.EVENT_BUS.addListener(Beutestufe::tick);
    }

    public static void tabFuellen(net.minecraft.world.item.CreativeModeTab.Output ausgabe) {
        ausgabe.accept(ROSTIGES_MESSER.get());
        ausgabe.accept(TRESOR_ITEM.get());
        ausgabe.accept(BRECHEISEN.get());
        ausgabe.accept(NACHTSICHT.get());
    }
}
