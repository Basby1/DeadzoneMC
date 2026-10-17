package de.deadzonemc.monster;

import de.deadzonemc.DeadzoneMC;
import de.deadzonemc.ModRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/** Registriert die Monster: Typen, Werte, Spawnregeln und Spawn-Eier. */
public final class MonsterTypen {

    private MonsterTypen() {
    }

    public static final DeferredRegister<EntityType<?>> TYPEN = DeferredRegister.create(Registries.ENTITY_TYPE, DeadzoneMC.MODID);

    /** Name -> Typ, in der Reihenfolge des Kreativ-Tabs. */
    public static final Map<String, DeferredHolder<EntityType<?>, EntityType<? extends DeadzoneMonster>>> ALLE = new LinkedHashMap<>();
    public static final List<DeferredItem<DeferredSpawnEggItem>> EIER = new ArrayList<>();

    public static final DeferredHolder<EntityType<?>, EntityType<Wanderer>> WANDERER = typ("wanderer", Wanderer::new, 0.6F, 1.9F, 0x8e9883, 0x3b4759);
    public static final DeferredHolder<EntityType<?>, EntityType<Schreier>> SCHREIER = typ("schreier", Schreier::new, 0.6F, 2.1F, 0xa2a08f, 0x6e1515);
    public static final DeferredHolder<EntityType<?>, EntityType<Lauerer>> LAUERER = typ("lauerer", Lauerer::new, 0.8F, 1.4F, 0xcfcabd, 0x1e1d1f);
    public static final DeferredHolder<EntityType<?>, EntityType<Kriecher>> KRIECHER = typ("kriecher", Kriecher::new, 1.2F, 0.9F, 0x8a7b72, 0x5e1818);
    public static final DeferredHolder<EntityType<?>, EntityType<DerStille>> DER_STILLE = typ("der_stille", DerStille::new, 0.6F, 2.6F, 0x1c1b1e, 0xe9e4d8);
    public static final DeferredHolder<EntityType<?>, EntityType<Imitator>> IMITATOR = typ("imitator", Imitator::new, 0.6F, 2.4F, 0x4f5a3b, 0xa8e6b0);
    public static final DeferredHolder<EntityType<?>, EntityType<Nebelwesen>> NEBELWESEN = typ("nebelwesen", Nebelwesen::new, 0.7F, 2.1F, 0xcdd8da, 0x0b0e0f);
    public static final DeferredHolder<EntityType<?>, EntityType<Schwarm>> SCHWARM = typ("schwarm", Schwarm::new, 0.5F, 0.45F, 0x9b827c, 0xff3b2b);
    public static final DeferredHolder<EntityType<?>, EntityType<Brut>> BRUT = typ("brut", Brut::new, 1.0F, 1.5F, 0xa7a27a, 0x9cff4a);
    public static final DeferredHolder<EntityType<?>, EntityType<Juggernaut>> JUGGERNAUT = typ("juggernaut", Juggernaut::new, 1.6F, 2.9F, 0x585d62, 0xff8a2a);

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T extends DeadzoneMonster> DeferredHolder<EntityType<?>, EntityType<T>> typ(
            String name, EntityType.EntityFactory<T> fabrik, float breite, float hoehe, int farbe1, int farbe2) {
        DeferredHolder<EntityType<?>, EntityType<T>> h = TYPEN.register(name, () -> EntityType.Builder.of(fabrik, MobCategory.MONSTER)
                .sized(breite, hoehe)
                .clientTrackingRange(10)
                .build(DeadzoneMC.MODID + ":" + name));
        ALLE.put(name, (DeferredHolder) h);
        Supplier<EntityType<T>> s = h;
        EIER.add(ModRegistry.ITEMS.register(name + "_spawn_egg",
                () -> new DeferredSpawnEggItem(s, farbe1, farbe2, new Item.Properties())));
        return h;
    }

    public static void registrieren(IEventBus modBus) {
        TYPEN.register(modBus);
        MonsterSounds.SOUNDS.register(modBus);
        modBus.addListener(MonsterTypen::attribute);
        modBus.addListener(MonsterTypen::spawnRegeln);
    }

    private static void attribute(EntityAttributeCreationEvent e) {
        e.put(WANDERER.get(), Wanderer.attribute().build());
        e.put(SCHREIER.get(), Schreier.attribute().build());
        e.put(LAUERER.get(), Lauerer.attribute().build());
        e.put(KRIECHER.get(), Kriecher.attribute().build());
        e.put(DER_STILLE.get(), DerStille.attribute().build());
        e.put(IMITATOR.get(), Imitator.attribute().build());
        e.put(NEBELWESEN.get(), Nebelwesen.attribute().build());
        e.put(SCHWARM.get(), Schwarm.attribute().build());
        e.put(BRUT.get(), Brut.attribute().build());
        e.put(JUGGERNAUT.get(), Juggernaut.attribute().build());
    }

    private static <T extends Monster> void normal(RegisterSpawnPlacementsEvent e, EntityType<T> typ) {
        e.register(typ, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkMonsterSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    private static void spawnRegeln(RegisterSpawnPlacementsEvent e) {
        normal(e, WANDERER.get());
        normal(e, SCHREIER.get());
        normal(e, LAUERER.get());
        normal(e, KRIECHER.get());
        normal(e, IMITATOR.get());
        normal(e, SCHWARM.get());
        normal(e, BRUT.get());
        normal(e, JUGGERNAUT.get());
        e.register(DER_STILLE.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (typ, level, grund, pos, zufall) -> Monster.checkMonsterSpawnRules(typ, level, grund, pos, zufall)
                        && (level.getLevel().isNight() || DeadzoneMonster.dichterNebel(level.getLevel()))
                        && zufall.nextInt(3) == 0,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        e.register(NEBELWESEN.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (typ, level, grund, pos, zufall) -> Monster.checkAnyLightMonsterSpawnRules(typ, level, grund, pos, zufall)
                        && DeadzoneMonster.dichterNebel(level.getLevel()),
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }
}
