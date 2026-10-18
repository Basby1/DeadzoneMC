package de.deadzonemc.mensch;

import de.deadzonemc.DeadzoneMC;
import de.deadzonemc.ModRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.List;

/** Registriert die Menschen der Deadzone. */
public final class MenschTypen {

    private MenschTypen() {
    }

    public static final DeferredRegister<EntityType<?>> TYPEN = DeferredRegister.create(Registries.ENTITY_TYPE, DeadzoneMC.MODID);
    public static final List<DeferredItem<DeferredSpawnEggItem>> EIER = new ArrayList<>();

    public static final DeferredHolder<EntityType<?>, EntityType<Soldat>> SOLDAT = TYPEN.register("soldat",
            () -> EntityType.Builder.of(Soldat::new, MobCategory.CREATURE).sized(0.6F, 1.95F).clientTrackingRange(10)
                    .build(DeadzoneMC.MODID + ":soldat"));
    public static final DeferredHolder<EntityType<?>, EntityType<Kultist>> KULTIST = TYPEN.register("kultist",
            () -> EntityType.Builder.of(Kultist::new, MobCategory.CREATURE).sized(0.6F, 1.95F).clientTrackingRange(10)
                    .build(DeadzoneMC.MODID + ":kultist"));
    public static final DeferredHolder<EntityType<?>, EntityType<Ueberlebender>> UEBERLEBENDER = TYPEN.register("ueberlebender",
            () -> EntityType.Builder.of(Ueberlebender::new, MobCategory.CREATURE).sized(0.6F, 1.95F).clientTrackingRange(10)
                    .build(DeadzoneMC.MODID + ":ueberlebender"));
    public static final DeferredHolder<EntityType<?>, EntityType<Haendler>> HAENDLER = TYPEN.register("haendler",
            () -> EntityType.Builder.of(Haendler::new, MobCategory.CREATURE).sized(0.6F, 1.95F).clientTrackingRange(10)
                    .build(DeadzoneMC.MODID + ":haendler"));

    public static final DeferredHolder<EntityType<?>, EntityType<Begleiter>> BEGLEITER = TYPEN.register("begleiter",
            () -> EntityType.Builder.of(Begleiter::new, MobCategory.CREATURE).sized(0.6F, 1.95F).clientTrackingRange(10)
                    .build(DeadzoneMC.MODID + ":begleiter"));

    public static final DeferredHolder<EntityType<?>, EntityType<Wache>> WACHE = TYPEN.register("wache",
            () -> EntityType.Builder.of(Wache::new, MobCategory.CREATURE).sized(0.6F, 1.95F).clientTrackingRange(10)
                    .build(DeadzoneMC.MODID + ":wache"));
    public static final DeferredHolder<EntityType<?>, EntityType<Bewohner>> BEWOHNER = TYPEN.register("bewohner",
            () -> EntityType.Builder.of(Bewohner::new, MobCategory.CREATURE).sized(0.6F, 1.95F).clientTrackingRange(10)
                    .build(DeadzoneMC.MODID + ":bewohner"));
    public static final DeferredHolder<EntityType<?>, EntityType<Auftraggeber>> AUFTRAGGEBER = TYPEN.register("auftraggeber",
            () -> EntityType.Builder.of(Auftraggeber::new, MobCategory.CREATURE).sized(0.6F, 1.95F).clientTrackingRange(10)
                    .build(DeadzoneMC.MODID + ":auftraggeber"));

    /** Die sechs neuen Fraktionen: Name des Modells -> Fraktion. */
    public static final java.util.Map<String, DeferredHolder<EntityType<?>, EntityType<de.deadzonemc.fraktion.Fraktionsmitglied>>>
            FRAKTIONEN = new java.util.LinkedHashMap<>();

    static {
        Object[][] liste = {{"bandit", Fraktion.BANDITEN, 0x3a3a38, 0x5a3a22}, {"totenfluesterer", Fraktion.TOTENFLUESTERER, 0x5a5652, 0x8aff9a},
                {"waldkind", Fraktion.WALDKINDER, 0x4a5a2a, 0xa8402a}, {"sandwanderer", Fraktion.SANDWANDERER, 0xd0c098, 0x6a8aa8},
                {"tiefenvolk", Fraktion.TIEFENVOLK, 0x1e3a44, 0x6affea}, {"himmelshueter", Fraktion.HIMMELSHUETER, 0xe8ecf0, 0x6ab0ff}};
        for (Object[] e : liste) {
            String n = (String) e[0];
            Fraktion f = (Fraktion) e[1];
            var h = TYPEN.register(n, () -> EntityType.Builder.<de.deadzonemc.fraktion.Fraktionsmitglied>of(
                            (t, l) -> new de.deadzonemc.fraktion.Fraktionsmitglied(t, l, f), MobCategory.CREATURE)
                    .sized(0.6F, 1.95F).clientTrackingRange(10).build(DeadzoneMC.MODID + ":" + n));
            FRAKTIONEN.put(n, h);
            ei(n, h, (Integer) e[2], (Integer) e[3]);
        }
    }

    static {
        ei("wache", WACHE, 0x5a6a72, 0xd8c020);
        ei("bewohner", BEWOHNER, 0x8a7a5a, 0x4a6a3a);
        ei("auftraggeber", AUFTRAGGEBER, 0x3a3a4a, 0xd8b030);
        ei("begleiter", BEGLEITER, 0x5a5a3a, 0x8ad0e0);
        ei("soldat", SOLDAT, 0x4f5a3b, 0x1e1f1d);
        ei("kultist", KULTIST, 0x5c5853, 0x9fd8ff);
        ei("ueberlebender", UEBERLEBENDER, 0x5a6a72, 0x7a2a2a);
        ei("haendler", HAENDLER, 0x6b5137, 0x4f5a4a);
    }

    @SuppressWarnings("unchecked")
    private static void ei(String name, DeferredHolder<EntityType<?>, ? extends EntityType<? extends Mob>> typ, int a, int b) {
        EIER.add(ModRegistry.ITEMS.register(name + "_spawn_egg",
                () -> new DeferredSpawnEggItem(() -> (EntityType<? extends Mob>) typ.get(), a, b, new Item.Properties())));
    }

    public static void registrieren(IEventBus modBus) {
        TYPEN.register(modBus);
        modBus.addListener(MenschTypen::attribute);
    }

    private static void attribute(EntityAttributeCreationEvent e) {
        e.put(SOLDAT.get(), Soldat.attribute().build());
        e.put(KULTIST.get(), Kultist.attribute().build());
        e.put(UEBERLEBENDER.get(), Ueberlebender.attribute().build());
        e.put(WACHE.get(), Wache.attribute().build());
        FRAKTIONEN.values().forEach(h -> e.put(h.get(), de.deadzonemc.fraktion.Fraktionsmitglied.attribute().build()));
        e.put(BEWOHNER.get(), Bewohner.attribute().build());
        e.put(AUFTRAGGEBER.get(), Auftraggeber.attribute().build());
        e.put(BEGLEITER.get(), Begleiter.attribute().build());
        e.put(HAENDLER.get(), Mob.createMobAttributes().add(Attributes.MOVEMENT_SPEED, 0.5).build());
    }
}
