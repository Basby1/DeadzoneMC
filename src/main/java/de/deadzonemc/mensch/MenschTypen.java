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

    public static final DeferredHolder<EntityType<?>, EntityType<Wache>> WACHE = TYPEN.register("wache",
            () -> EntityType.Builder.of(Wache::new, MobCategory.CREATURE).sized(0.6F, 1.95F).clientTrackingRange(10)
                    .build(DeadzoneMC.MODID + ":wache"));
    public static final DeferredHolder<EntityType<?>, EntityType<Bewohner>> BEWOHNER = TYPEN.register("bewohner",
            () -> EntityType.Builder.of(Bewohner::new, MobCategory.CREATURE).sized(0.6F, 1.95F).clientTrackingRange(10)
                    .build(DeadzoneMC.MODID + ":bewohner"));
    public static final DeferredHolder<EntityType<?>, EntityType<Auftraggeber>> AUFTRAGGEBER = TYPEN.register("auftraggeber",
            () -> EntityType.Builder.of(Auftraggeber::new, MobCategory.CREATURE).sized(0.6F, 1.95F).clientTrackingRange(10)
                    .build(DeadzoneMC.MODID + ":auftraggeber"));

    static {
        ei("wache", WACHE, 0x5a6a72, 0xd8c020);
        ei("bewohner", BEWOHNER, 0x8a7a5a, 0x4a6a3a);
        ei("auftraggeber", AUFTRAGGEBER, 0x3a3a4a, 0xd8b030);
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
        e.put(BEWOHNER.get(), Bewohner.attribute().build());
        e.put(AUFTRAGGEBER.get(), Auftraggeber.attribute().build());
        e.put(HAENDLER.get(), Mob.createMobAttributes().add(Attributes.MOVEMENT_SPEED, 0.5).build());
    }
}
