package de.deadzonemc.ausruestung;

import de.deadzonemc.DeadzoneMC;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemNameBlockItem;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Ausrüstung aus der Deadzone: Materialien (Monster-Drops), Pflanzen, Nahkampfwaffen und Rüstungssätze für
 * jede Klasse. Alles ist herstellbar – aus Beute, Ernte und dem, was Infizierte fallen lassen.
 */
public final class Ausruestung {

    private Ausruestung() {
    }

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(DeadzoneMC.MODID);
    public static final DeferredRegister.Blocks BLOECKE = DeferredRegister.createBlocks(DeadzoneMC.MODID);
    public static final DeferredRegister<ArmorMaterial> RUESTUNG = DeferredRegister.create(Registries.ARMOR_MATERIAL, DeadzoneMC.MODID);

    // ---- Materialien ----------------------------------------------------------------------
    public static final List<String> MATERIALIEN = List.of("knochensplitter", "verseuchtes_gewebe", "sehne", "mutantenhaut",
            "chitinplatte", "panzerplatte", "strahlenkristall", "sporenbeutel", "drohnenteile", "mottenstaub", "glutkern",
            "mutagen", "heilkraut", "baumwolle", "naegel", "klebeband", "gummi");
    public static final Map<String, DeferredItem<Item>> MAT = new LinkedHashMap<>();

    static {
        for (String m : MATERIALIEN) {
            Rarity r = switch (m) {
                case "mutagen" -> Rarity.EPIC;
                case "strahlenkristall", "glutkern", "panzerplatte" -> Rarity.UNCOMMON;
                default -> Rarity.COMMON;
            };
            MAT.put(m, ITEMS.registerSimpleItem(m, new Item.Properties().rarity(r)));
        }
    }

    public static Item mat(String id) {
        return MAT.get(id).get();
    }

    // ---- Pflanzen -------------------------------------------------------------------------
    public static final DeferredBlock<Ernte> HEILKRAUT_PFLANZE = BLOECKE.register("heilkraut_pflanze",
            () -> new Ernte(() -> Ausruestung.HEILKRAUT_SAMEN.get(), pflanze()));
    public static final DeferredBlock<Ernte> BAUMWOLLE_PFLANZE = BLOECKE.register("baumwolle_pflanze",
            () -> new Ernte(() -> Ausruestung.BAUMWOLLSAMEN.get(), pflanze()));
    public static final DeferredItem<ItemNameBlockItem> HEILKRAUT_SAMEN = ITEMS.register("heilkraut_samen",
            () -> new ItemNameBlockItem(HEILKRAUT_PFLANZE.get(), new Item.Properties()));
    public static final DeferredItem<ItemNameBlockItem> BAUMWOLLSAMEN = ITEMS.register("baumwollsamen",
            () -> new ItemNameBlockItem(BAUMWOLLE_PFLANZE.get(), new Item.Properties()));

    private static BlockBehaviour.Properties pflanze() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).noCollission().randomTicks().instabreak()
                .sound(SoundType.CROP).pushReaction(PushReaction.DESTROY);
    }

    // ---- Nahkampf -------------------------------------------------------------------------
    public record Nahkampf(String id, Tier tier, int schaden, float tempo, int haltbarkeit, Rarity seltenheit) {
    }

    public static final List<Nahkampf> NAHKAMPF = List.of(
            new Nahkampf("kampfmesser", Tiers.IRON, 2, -1.8F, 400, Rarity.COMMON),
            new Nahkampf("machete", Tiers.IRON, 4, -2.4F, 450, Rarity.COMMON),
            new Nahkampf("baseballschlaeger", Tiers.WOOD, 4, -2.6F, 260, Rarity.COMMON),
            new Nahkampf("nagelschlaeger", Tiers.WOOD, 6, -2.7F, 320, Rarity.COMMON),
            new Nahkampf("feuerwehraxt", Tiers.IRON, 7, -3.0F, 600, Rarity.UNCOMMON),
            new Nahkampf("vorschlaghammer", Tiers.IRON, 10, -3.35F, 700, Rarity.UNCOMMON),
            new Nahkampf("speer", Tiers.STONE, 4, -2.5F, 240, Rarity.COMMON),
            new Nahkampf("knochenkeule", Tiers.STONE, 4, -2.7F, 300, Rarity.COMMON),
            new Nahkampf("mutantenklinge", Tiers.NETHERITE, 6, -2.3F, 1600, Rarity.EPIC));
    public static final Map<String, DeferredItem<SwordItem>> WAFFE = new LinkedHashMap<>();

    static {
        for (Nahkampf n : NAHKAMPF) {
            WAFFE.put(n.id, ITEMS.register(n.id, () -> new SwordItem(n.tier, new Item.Properties().durability(n.haltbarkeit)
                    .rarity(n.seltenheit).attributes(SwordItem.createAttributes(n.tier, n.schaden, n.tempo)))));
        }
    }

    // ---- Rüstungssätze --------------------------------------------------------------------
    public enum Satz {
        //            id               Kopf Brust Bein Fuß  Härte  Rückstoßfest  Seltenheit  Klasse
        PLUENDERER("pluenderer", 1, 3, 2, 1, 0F, 0F, Rarity.COMMON, ""),
        SCHROTTPANZER("schrottpanzer", 3, 7, 5, 2, 1.5F, 0.1F, Rarity.UNCOMMON, "brecher"),
        TAKTISCH("taktisch", 2, 6, 4, 2, 1F, 0F, Rarity.UNCOMMON, "schuetze"),
        SANITAETER("sanitaeter", 2, 5, 3, 1, 0.5F, 0F, Rarity.UNCOMMON, "sanitaeter"),
        SCHUTZANZUG("schutzanzug", 2, 4, 3, 1, 0.5F, 0F, Rarity.UNCOMMON, "techniker"),
        TARNANZUG("tarnanzug", 1, 4, 3, 1, 0F, 0F, Rarity.UNCOMMON, "spaeher"),
        MUTANTENPANZER("mutantenpanzer", 4, 8, 6, 3, 3F, 0.15F, Rarity.EPIC, "");

        public final String id;
        public final int kopf, brust, bein, fuss;
        public final float haerte, rueckstoss;
        public final Rarity seltenheit;
        public final String klasse;

        Satz(String id, int kopf, int brust, int bein, int fuss, float haerte, float rueckstoss, Rarity seltenheit, String klasse) {
            this.id = id;
            this.kopf = kopf;
            this.brust = brust;
            this.bein = bein;
            this.fuss = fuss;
            this.haerte = haerte;
            this.rueckstoss = rueckstoss;
            this.seltenheit = seltenheit;
            this.klasse = klasse;
        }
    }

    public static final Map<Satz, DeferredHolder<ArmorMaterial, ArmorMaterial>> MATERIAL = new EnumMap<>(Satz.class);
    public static final Map<Satz, List<DeferredItem<ArmorItem>>> TEILE = new EnumMap<>(Satz.class);

    static {
        for (Satz s : Satz.values()) {
            DeferredHolder<ArmorMaterial, ArmorMaterial> mat = RUESTUNG.register(s.id, () -> new ArmorMaterial(
                    Map.of(ArmorItem.Type.HELMET, s.kopf, ArmorItem.Type.CHESTPLATE, s.brust, ArmorItem.Type.LEGGINGS, s.bein,
                            ArmorItem.Type.BOOTS, s.fuss, ArmorItem.Type.BODY, s.brust),
                    12, s == Satz.SCHROTTPANZER || s == Satz.MUTANTENPANZER ? SoundEvents.ARMOR_EQUIP_IRON : SoundEvents.ARMOR_EQUIP_LEATHER,
                    () -> Ingredient.EMPTY, List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(DeadzoneMC.MODID, s.id))),
                    s.haerte, s.rueckstoss));
            MATERIAL.put(s, mat);
            List<DeferredItem<ArmorItem>> teile = new ArrayList<>();
            int faktor = switch (s) {
                case PLUENDERER -> 8;
                case MUTANTENPANZER -> 37;
                default -> 18;
            };
            for (ArmorItem.Type t : List.of(ArmorItem.Type.HELMET, ArmorItem.Type.CHESTPLATE, ArmorItem.Type.LEGGINGS, ArmorItem.Type.BOOTS)) {
                String name = s.id + "_" + switch (t) {
                    case HELMET -> "kopf";
                    case CHESTPLATE -> "brust";
                    case LEGGINGS -> "beine";
                    default -> "stiefel";
                };
                teile.add(ITEMS.register(name, () -> new ArmorItem(mat, t, new Item.Properties().rarity(s.seltenheit)
                        .durability(t.getDurability(faktor)))));
            }
            TEILE.put(s, teile);
        }
    }

    /** Wie viele Teile dieses Satzes der Spieler trägt (0–4). */
    public static int traegt(net.minecraft.world.entity.LivingEntity e, Satz s) {
        int n = 0;
        for (net.minecraft.world.item.ItemStack st : e.getArmorSlots()) {
            if (st.getItem() instanceof ArmorItem a && a.getMaterial().is(MATERIAL.get(s).getKey())) {
                n++;
            }
        }
        return n;
    }

    public static void registrieren(IEventBus modBus) {
        ITEMS.register(modBus);
        BLOECKE.register(modBus);
        RUESTUNG.register(modBus);
        NeoForge.EVENT_BUS.addListener(Satzboni::schaden);
        NeoForge.EVENT_BUS.addListener(Satzboni::zielen);
        NeoForge.EVENT_BUS.addListener(Satzboni::tick);
    }

    public static void tabFuellen(net.minecraft.world.item.CreativeModeTab.Output ausgabe) {
        MAT.values().forEach(i -> ausgabe.accept(i.get()));
        ausgabe.accept(HEILKRAUT_SAMEN.get());
        ausgabe.accept(BAUMWOLLSAMEN.get());
        WAFFE.values().forEach(i -> ausgabe.accept(i.get()));
        TEILE.values().forEach(l -> l.forEach(i -> ausgabe.accept(i.get())));
    }

    @SuppressWarnings("unused")
    private static Holder<ArmorMaterial> h(Supplier<Holder<ArmorMaterial>> s) {
        return s.get();
    }

    @SuppressWarnings("unused")
    private static Block b() {
        return null;
    }
}
