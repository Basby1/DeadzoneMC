package de.deadzonemc.waffen.wurf;

import de.deadzonemc.DeadzoneMC;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.Map;

/** Handgranate, Brandsatz, Rauch-, Blendgranate und Lärmköder. */
public final class Wurfwaffen {

    private Wurfwaffen() {
    }

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(DeadzoneMC.MODID);
    public static final DeferredRegister<EntityType<?>> TYPEN = DeferredRegister.create(Registries.ENTITY_TYPE, DeadzoneMC.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<Wurfkoerper>> WURFKOERPER = TYPEN.register("wurfkoerper",
            () -> EntityType.Builder.<Wurfkoerper>of(Wurfkoerper::new, MobCategory.MISC).sized(0.3F, 0.3F)
                    .clientTrackingRange(6).updateInterval(5).build("wurfkoerper"));

    public static final Map<Wurfart, DeferredItem<WurfItem>> WURF = new EnumMap<>(Wurfart.class);

    static {
        for (Wurfart a : Wurfart.values()) {
            WURF.put(a, ITEMS.register(a.id, () -> new WurfItem(a, new Item.Properties().stacksTo(8))));
        }
    }

    public static void registrieren(IEventBus modBus) {
        ITEMS.register(modBus);
        TYPEN.register(modBus);
    }

    public static void tabFuellen(net.minecraft.world.item.CreativeModeTab.Output ausgabe) {
        WURF.values().forEach(w -> ausgabe.accept(w.get()));
    }
}
