package de.deadzonemc;

import com.mojang.serialization.Codec;
import de.deadzonemc.item.HandbuchItem;
import de.deadzonemc.item.NotizItem;
import de.deadzonemc.item.VerbandItem;
import de.deadzonemc.welt.stadt.GebaeudePiece;
import de.deadzonemc.welt.stadt.KachelStruktur;
import de.deadzonemc.welt.stadt.ObjektPiece;
import de.deadzonemc.welt.stadt.StrassenPiece;
import de.deadzonemc.welt.feature.AutowrackFeature;
import de.deadzonemc.welt.feature.SchuttFeature;
import de.deadzonemc.welt.feature.StrommastFeature;
import de.deadzonemc.welt.feature.ToterBaumFeature;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** Registriert alles, was Deadzone MC neu ins Spiel bringt. */
public final class ModRegistry {

    private ModRegistry() {
    }

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(DeadzoneMC.MODID);
    public static final DeferredRegister.Blocks BLOECKE = DeferredRegister.createBlocks(DeadzoneMC.MODID);
    public static final DeferredRegister<net.minecraft.world.item.ArmorMaterial> RUESTUNG =
            DeferredRegister.create(Registries.ARMOR_MATERIAL, DeadzoneMC.MODID);
    public static final DeferredRegister<DataComponentType<?>> KOMPONENTEN =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, DeadzoneMC.MODID);
    public static final DeferredRegister<AttachmentType<?>> ANHAENGE =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, DeadzoneMC.MODID);
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, DeadzoneMC.MODID);
    public static final DeferredRegister<Feature<?>> FEATURES =
            DeferredRegister.create(Registries.FEATURE, DeadzoneMC.MODID);
    public static final DeferredRegister<StructureType<?>> STRUKTUR_TYPEN =
            DeferredRegister.create(Registries.STRUCTURE_TYPE, DeadzoneMC.MODID);
    public static final DeferredRegister<StructurePieceType> STRUKTUR_TEILE =
            DeferredRegister.create(Registries.STRUCTURE_PIECE, DeadzoneMC.MODID);

    // ---- Daten -----------------------------------------------------------------------------

    /** Welche Notiz ein Notiz-Item zeigt. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> NOTIZ =
            KOMPONENTEN.register("notiz", () -> DataComponentType.<String>builder()
                    .persistent(Codec.STRING)
                    .networkSynchronized(ByteBufCodecs.STRING_UTF8)
                    .build());

    /** Ob ein Spieler schon einmal in der Deadzone angekommen ist. */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Boolean>> GESTARTET =
            ANHAENGE.register("gestartet", () -> AttachmentType.builder(() -> false)
                    .serialize(Codec.BOOL)
                    .copyOnDeath()
                    .build());

    /** Wie viele Ticks Schutz der Filter einer Gasmaske noch hat. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> FILTER =
            KOMPONENTEN.register("filter", () -> DataComponentType.<Integer>builder()
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT)
                    .build());

    // ---- Blöcke ----------------------------------------------------------------------------

    public static final net.neoforged.neoforge.registries.DeferredBlock<de.deadzonemc.strahlung.StrahlenfassBlock> STRAHLENFASS =
            BLOECKE.register("strahlenfass", () -> new de.deadzonemc.strahlung.StrahlenfassBlock(
                    net.minecraft.world.level.block.state.BlockBehaviour.Properties.of()
                            .strength(2.0F, 6.0F)
                            .sound(net.minecraft.world.level.block.SoundType.METAL)
                            .lightLevel(z -> 6)
                            .noOcclusion()));
    public static final DeferredItem<net.minecraft.world.item.BlockItem> STRAHLENFASS_ITEM = ITEMS.registerSimpleBlockItem(STRAHLENFASS);

    /** Aufgenommene Strahlendosis eines Spielers (0 bis 1000). Nach dem Tod wieder 0. */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Float>> STRAHLENDOSIS =
            ANHAENGE.register("strahlendosis", () -> AttachmentType.builder(() -> 0.0F)
                    .serialize(Codec.FLOAT)
                    .build());

    // ---- Items -----------------------------------------------------------------------------

    public static final DeferredHolder<net.minecraft.world.item.ArmorMaterial, net.minecraft.world.item.ArmorMaterial> GASMASKE_MATERIAL =
            RUESTUNG.register("gasmaske", () -> new net.minecraft.world.item.ArmorMaterial(
                    java.util.Map.of(net.minecraft.world.item.ArmorItem.Type.HELMET, 1,
                            net.minecraft.world.item.ArmorItem.Type.CHESTPLATE, 1,
                            net.minecraft.world.item.ArmorItem.Type.LEGGINGS, 1,
                            net.minecraft.world.item.ArmorItem.Type.BOOTS, 1,
                            net.minecraft.world.item.ArmorItem.Type.BODY, 1),
                    5, net.minecraft.sounds.SoundEvents.ARMOR_EQUIP_LEATHER,
                    () -> net.minecraft.world.item.crafting.Ingredient.of(net.minecraft.world.item.Items.LEATHER),
                    java.util.List.of(new net.minecraft.world.item.ArmorMaterial.Layer(
                            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(DeadzoneMC.MODID, "gasmaske"))),
                    0.0F, 0.0F));
    public static final DeferredItem<de.deadzonemc.strahlung.GasmaskeItem> GASMASKE = ITEMS.register("gasmaske",
            () -> new de.deadzonemc.strahlung.GasmaskeItem(GASMASKE_MATERIAL, new Item.Properties()
                    .durability(net.minecraft.world.item.ArmorItem.Type.HELMET.getDurability(12))));
    public static final DeferredItem<de.deadzonemc.strahlung.FilterItem> FILTER_ITEM = ITEMS.register("filter",
            () -> new de.deadzonemc.strahlung.FilterItem(new Item.Properties().stacksTo(8)));
    public static final DeferredItem<de.deadzonemc.strahlung.GeigerzaehlerItem> GEIGERZAEHLER = ITEMS.register("geigerzaehler",
            () -> new de.deadzonemc.strahlung.GeigerzaehlerItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<de.deadzonemc.item.MedizinItem> ANTI_RAD = ITEMS.register("anti_rad",
            () -> new de.deadzonemc.item.MedizinItem(de.deadzonemc.item.MedizinItem.Art.ANTI_RAD, new Item.Properties().stacksTo(8)));
    public static final DeferredItem<de.deadzonemc.item.MedizinItem> WODKA = ITEMS.register("wodka",
            () -> new de.deadzonemc.item.MedizinItem(de.deadzonemc.item.MedizinItem.Art.WODKA, new Item.Properties().stacksTo(4)));

    public static final DeferredItem<Item> KONSERVENDOSE = ITEMS.registerSimpleItem("konservendose",
            new Item.Properties().stacksTo(16).food(new FoodProperties.Builder()
                    .nutrition(6).saturationModifier(0.6F).build()));
    public static final DeferredItem<VerbandItem> VERBAND = ITEMS.register("verband",
            () -> new VerbandItem(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<de.deadzonemc.item.MedizinItem> ANTIBIOTIKA = ITEMS.register("antibiotika",
            () -> new de.deadzonemc.item.MedizinItem(de.deadzonemc.item.MedizinItem.Art.ANTIBIOTIKA, new Item.Properties().stacksTo(8)));
    public static final DeferredItem<de.deadzonemc.item.MedizinItem> SCHMERZMITTEL = ITEMS.register("schmerzmittel",
            () -> new de.deadzonemc.item.MedizinItem(de.deadzonemc.item.MedizinItem.Art.SCHMERZMITTEL, new Item.Properties().stacksTo(16)));
    public static final DeferredItem<Item> SCHROTT = ITEMS.registerSimpleItem("schrott");
    public static final DeferredItem<Item> STOFFRESTE = ITEMS.registerSimpleItem("stoffreste");
    public static final DeferredItem<Item> BATTERIE = ITEMS.registerSimpleItem("batterie", new Item.Properties().stacksTo(16));
    public static final DeferredItem<Item> ELEKTRONIK = ITEMS.registerSimpleItem("elektronik");
    public static final DeferredItem<NotizItem> NOTIZ_ITEM = ITEMS.register("notiz",
            () -> new NotizItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<HandbuchItem> HANDBUCH = ITEMS.register("handbuch",
            () -> new HandbuchItem(new Item.Properties().stacksTo(1)));

    // ---- Weltgenerierung -------------------------------------------------------------------

    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> TOTER_BAUM =
            FEATURES.register("toter_baum", ToterBaumFeature::new);
    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> AUTOWRACK =
            FEATURES.register("autowrack", AutowrackFeature::new);
    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> STRAHLENFAESSER =
            FEATURES.register("strahlenfaesser", de.deadzonemc.strahlung.StrahlenfassFeature::new);
    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> SCHUTT =
            FEATURES.register("schutt", SchuttFeature::new);
    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> STROMMAST =
            FEATURES.register("strommast", StrommastFeature::new);

    public static final DeferredHolder<StructureType<?>, StructureType<KachelStruktur>> KACHEL =
            STRUKTUR_TYPEN.register("siedlungskachel", () -> () -> KachelStruktur.CODEC);

    public static final DeferredHolder<StructurePieceType, StructurePieceType> STRASSEN_TEIL =
            STRUKTUR_TEILE.register("strassenstueck", () -> (StructurePieceType.ContextlessType) StrassenPiece::new);
    public static final DeferredHolder<StructurePieceType, StructurePieceType> GEBAEUDE_TEIL =
            STRUKTUR_TEILE.register("gebaeude", () -> (StructurePieceType.ContextlessType) GebaeudePiece::new);
    public static final DeferredHolder<StructurePieceType, StructurePieceType> OBJEKT_TEIL =
            STRUKTUR_TEILE.register("objekt", () -> (StructurePieceType.ContextlessType) ObjektPiece::new);

    // ---- Kreativ-Tab -----------------------------------------------------------------------

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("deadzonemc",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.deadzonemc"))
                    .icon(() -> new ItemStack(KONSERVENDOSE.get()))
                    .displayItems((params, ausgabe) -> {
                        ausgabe.accept(HANDBUCH.get());
                        ausgabe.accept(KONSERVENDOSE.get());
                        ausgabe.accept(VERBAND.get());
                        ausgabe.accept(ANTIBIOTIKA.get());
                        ausgabe.accept(SCHMERZMITTEL.get());
                        ausgabe.accept(ANTI_RAD.get());
                        ausgabe.accept(WODKA.get());
                        ausgabe.accept(GASMASKE.get());
                        ausgabe.accept(FILTER_ITEM.get());
                        ausgabe.accept(GEIGERZAEHLER.get());
                        ausgabe.accept(STRAHLENFASS_ITEM.get());
                        ausgabe.accept(SCHROTT.get());
                        ausgabe.accept(STOFFRESTE.get());
                        ausgabe.accept(BATTERIE.get());
                        ausgabe.accept(ELEKTRONIK.get());
                        de.deadzonemc.monster.MonsterTypen.EIER.forEach(ei -> ausgabe.accept(ei.get()));
                        for (String id : NotizItem.ALLE) {
                            ausgabe.accept(NotizItem.erstellen(id));
                        }
                    })
                    .build());

    public static void registrieren(IEventBus modBus) {
        ITEMS.register(modBus);
        BLOECKE.register(modBus);
        RUESTUNG.register(modBus);
        KOMPONENTEN.register(modBus);
        ANHAENGE.register(modBus);
        TABS.register(modBus);
        FEATURES.register(modBus);
        STRUKTUR_TYPEN.register(modBus);
        STRUKTUR_TEILE.register(modBus);
    }
}
