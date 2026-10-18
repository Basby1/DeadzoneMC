package de.deadzonemc.waffen.feuer;

import com.mojang.serialization.Codec;
import de.deadzonemc.DeadzoneMC;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Eigene Schusswaffen, Munition und Aufsätze (ersetzt TaCZ). */
public final class Feuerwaffen {

    private Feuerwaffen() {
    }

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(DeadzoneMC.MODID);
    public static final DeferredRegister<DataComponentType<?>> KOMPONENTEN = DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, DeadzoneMC.MODID);
    public static final DeferredRegister<SoundEvent> KLAENGE = DeferredRegister.create(Registries.SOUND_EVENT, DeadzoneMC.MODID);

    /** Patronen im Magazin. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> MAGAZIN = KOMPONENTEN.register("magazin",
            () -> DataComponentType.<Integer>builder().persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT).build());
    /** Angebaute Aufsätze (IDs wie "schalldaempfer"). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<List<String>>> AUFSAETZE = KOMPONENTEN.register("aufsaetze",
            () -> DataComponentType.<List<String>>builder().persistent(Codec.STRING.listOf())
                    .networkSynchronized(ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list())).build());
    /** Taschenlampe an/aus. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> LICHT = KOMPONENTEN.register("licht",
            () -> DataComponentType.<Boolean>builder().persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL).build());

    public static final Map<WaffenArt, DeferredItem<SchusswaffeItem>> WAFFEN = new EnumMap<>(WaffenArt.class);
    public static final Map<Kaliber, DeferredItem<Item>> MUNITION = new EnumMap<>(Kaliber.class);
    public static final Map<String, DeferredItem<AufsatzItem>> AUFSATZ = new LinkedHashMap<>();
    public static final Map<WaffenArt, DeferredHolder<SoundEvent, SoundEvent>> SCHUSS = new EnumMap<>(WaffenArt.class);

    public static final DeferredHolder<SoundEvent, SoundEvent> GEDAEMPFT = klang("waffe.gedaempft");
    public static final DeferredHolder<SoundEvent, SoundEvent> LADEN = klang("waffe.laden");
    public static final DeferredHolder<SoundEvent, SoundEvent> LADEN_FERTIG = klang("waffe.laden_fertig");
    public static final DeferredHolder<SoundEvent, SoundEvent> PATRONE = klang("waffe.patrone");
    public static final DeferredHolder<SoundEvent, SoundEvent> LEER = klang("waffe.leer");
    public static final DeferredHolder<SoundEvent, SoundEvent> EINSCHLAG = klang("waffe.einschlag");

    public static final DeferredItem<TaschenlampeItem> TASCHENLAMPE = ITEMS.register("taschenlampe",
            () -> new TaschenlampeItem(new Item.Properties().stacksTo(1)));

    static {
        for (WaffenArt a : WaffenArt.values()) {
            Rarity r = switch (a) {
                case ROHRPISTOLE, ROHRGEWEHR, ARMBRUST, DOPPELFLINTE -> Rarity.COMMON;
                case P9, REVOLVER, PUMPGUN, MP9 -> Rarity.UNCOMMON;
                case STURMGEWEHR, KALASCHNIKOW, JAGDGEWEHR -> Rarity.RARE;
                case PRAEZISIONSGEWEHR, MG -> Rarity.EPIC;
            };
            WAFFEN.put(a, ITEMS.register(a.id, () -> new SchusswaffeItem(a, new Item.Properties().stacksTo(1).rarity(r))));
            SCHUSS.put(a, klang("waffe." + a.id + ".schuss"));
        }
        for (Kaliber k : Kaliber.values()) {
            MUNITION.put(k, ITEMS.register(k.itemId(), () -> new MunitionItem(k, new Item.Properties().stacksTo(k == Kaliber.SCHROT ? 32 : 64))));
        }
        for (String id : List.of("schalldaempfer", "zielfernrohr", "taschenlampe", "magazin", "griff")) {
            AUFSATZ.put(id, ITEMS.register("aufsatz_" + id, () -> new AufsatzItem(id, new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON))));
        }
    }

    private static DeferredHolder<SoundEvent, SoundEvent> klang(String name) {
        return KLAENGE.register(name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(DeadzoneMC.MODID, name)));
    }

    public static void registrieren(IEventBus modBus) {
        ITEMS.register(modBus);
        KOMPONENTEN.register(modBus);
        KLAENGE.register(modBus);
        NeoForge.EVENT_BUS.addListener(Nachladen::tick);
        NeoForge.EVENT_BUS.addListener(Lichtkegel::tick);
    }

    public static ItemStack waffe(WaffenArt a, int patronen) {
        ItemStack s = new ItemStack(WAFFEN.get(a).get());
        s.set(MAGAZIN.get(), Math.min(patronen, a.magazin));
        return s;
    }

    public static ItemStack munition(Kaliber k, int anzahl) {
        return new ItemStack(MUNITION.get(k).get(), anzahl);
    }

    /** Übersetzt alte TaCZ-Namen (Beute, Händler, Bosse) in eigene Waffen und Munition. */
    public static ItemStack ausTacz(String item, String wert, int menge) {
        if (item.equals("ammo")) {
            Kaliber k = switch (wert) {
                case "9mm", "45acp" -> Kaliber.NEUN_MM;
                case "357mag", "50ae" -> Kaliber.MAG357;
                case "12g" -> Kaliber.SCHROT;
                case "556x45" -> Kaliber.F556;
                default -> Kaliber.F762;
            };
            return munition(k, menge);
        }
        WaffenArt a = switch (wert) {
            case "glock_17", "m9a4", "cz75", "p320", "m1911", "hk_mk23" -> WaffenArt.P9;
            case "rhino357", "deagle" -> WaffenArt.REVOLVER;
            case "hk_mp5a5", "uzi", "ump45" -> WaffenArt.MP9;
            case "m870", "spas_12", "m1014" -> WaffenArt.PUMPGUN;
            case "db_short", "db_long" -> WaffenArt.DOPPELFLINTE;
            case "g36k", "hk416d", "m4a1" -> WaffenArt.STURMGEWEHR;
            case "ak47", "sks_tactical", "hk_g3" -> WaffenArt.KALASCHNIKOW;
            case "kar98", "m700" -> WaffenArt.JAGDGEWEHR;
            case "mk14" -> WaffenArt.PRAEZISIONSGEWEHR;
            case "m249" -> WaffenArt.MG;
            default -> WaffenArt.P9;
        };
        return waffe(a, a.magazin / 3);
    }

    public static boolean hatAufsatz(ItemStack s, String id) {
        List<String> l = s.get(AUFSAETZE.get());
        return l != null && l.contains(id);
    }

    public static int magazinGroesse(ItemStack s, WaffenArt a) {
        int n = hatAufsatz(s, "magazin") ? a.magazin + Math.max(1, a.magazin / 2) : a.magazin;
        // Trommelmagazin (+25 % je Stufe, nur bei Waffen mit echtem Magazin)
        if (a.magazin >= 5) {
            n += Math.round(a.magazin * 0.25F * de.deadzonemc.verzauberung.Zonenverzauberung.stufe(s, "trommel"));
        }
        return n;
    }

    public static void tabFuellen(net.minecraft.world.item.CreativeModeTab.Output ausgabe) {
        for (WaffenArt a : WaffenArt.values()) {
            ausgabe.accept(waffe(a, a.magazin));
        }
        MUNITION.values().forEach(m -> ausgabe.accept(m.get()));
        AUFSATZ.values().forEach(m -> ausgabe.accept(m.get()));
        ausgabe.accept(TASCHENLAMPE.get());
    }
}
