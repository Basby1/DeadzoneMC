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


    // ---- zweite Welle ----
    public static final DeferredHolder<EntityType<?>, EntityType<GefallenerSoldat>> GEFALLENER_SOLDAT = typ("gefallener_soldat", GefallenerSoldat::new, 0.6F, 1.95F, 0x4f5a3b, 0x7f8a78);
    public static final DeferredHolder<EntityType<?>, EntityType<Glutleib>> GLUTLEIB = typ("glutleib", Glutleib::new, 1.0F, 2.0F, 0x2a2220, 0xff8a2a, true);
    public static final DeferredHolder<EntityType<?>, EntityType<Seuchenhund>> SEUCHENHUND = typ("seuchenhund", Seuchenhund::new, 0.7F, 0.9F, 0x5a4a3c, 0x8a3a30);
    public static final DeferredHolder<EntityType<?>, EntityType<Wuehler>> WUEHLER = typ("wuehler", Wuehler::new, 0.9F, 1.3F, 0xc79a8a, 0x5a4636);
    public static final DeferredHolder<EntityType<?>, EntityType<Sauger>> SAUGER = typ("sauger", Sauger::new, 0.8F, 1.7F, 0x8a5a4a, 0xffd84a);
    public static final DeferredHolder<EntityType<?>, EntityType<Seuchentraeger>> SEUCHENTRAEGER = typ("seuchentraeger", Seuchentraeger::new, 0.9F, 2.0F, 0xc9b43a, 0x9cff4a);
    public static final DeferredHolder<EntityType<?>, EntityType<Ertrunkener>> ERTRUNKENER = typ("ertrunkener", Ertrunkener::new, 0.9F, 2.0F, 0x7a8a8e, 0x2e4a2a);
    public static final DeferredHolder<EntityType<?>, EntityType<Spaeherdrohne>> SPAEHERDROHNE = typ("spaeherdrohne", Spaeherdrohne::new, 0.9F, 0.6F, 0x4a5238, 0xff2a1a);
    public static final DeferredHolder<EntityType<?>, EntityType<Lichtmotte>> LICHTMOTTE = typ("lichtmotte", Lichtmotte::new, 1.2F, 0.8F, 0xa8987a, 0xd8ffd0);
    public static final DeferredHolder<EntityType<?>, EntityType<Scheintoter>> SCHEINTOTER = typ("scheintoter", Scheintoter::new, 0.6F, 1.9F, 0x9a9a88, 0x6a1414);

    // ---- Bosse ----
    public static final DeferredHolder<EntityType<?>, EntityType<de.deadzonemc.monster.boss.Verschmolzener>> VERSCHMOLZENER = typ("verschmolzener", de.deadzonemc.monster.boss.Verschmolzener::new, 2.4F, 4.0F, 0x8e9883, 0xff3a2a);
    public static final DeferredHolder<EntityType<?>, EntityType<de.deadzonemc.monster.boss.Nebelmutter>> NEBELMUTTER = typ("nebelmutter", de.deadzonemc.monster.boss.Nebelmutter::new, 1.2F, 6.8F, 0xd8dcd6, 0xd8fbff);
    public static final DeferredHolder<EntityType<?>, EntityType<de.deadzonemc.monster.boss.DasHerz>> DAS_HERZ = typ("das_herz", de.deadzonemc.monster.boss.DasHerz::new, 7.0F, 16.0F, 0x2a1a16, 0xff5a1a, true);
    public static final DeferredHolder<EntityType<?>, EntityType<de.deadzonemc.monster.boss.Strahlende>> STRAHLENDE = typ("strahlende", de.deadzonemc.monster.boss.Strahlende::new, 2.0F, 3.8F, 0x7a8460, 0x9cff4a);
    public static final DeferredHolder<EntityType<?>, EntityType<de.deadzonemc.monster.boss.Brutkoenigin>> BRUTKOENIGIN = typ("brutkoenigin", de.deadzonemc.monster.boss.Brutkoenigin::new, 2.6F, 2.2F, 0x3a2a26, 0xd8c8a0);
    public static final DeferredHolder<EntityType<?>, EntityType<de.deadzonemc.monster.boss.Schlaechter>> SCHLAECHTER = typ("schlaechter", de.deadzonemc.monster.boss.Schlaechter::new, 1.4F, 2.9F, 0xb89a88, 0x5a0a0a);
    public static final DeferredHolder<EntityType<?>, EntityType<de.deadzonemc.monster.boss.Kommandant>> KOMMANDANT = typ("kommandant", de.deadzonemc.monster.boss.Kommandant::new, 1.2F, 2.7F, 0x4a5238, 0xff8a2a);
    public static final DeferredHolder<EntityType<?>, EntityType<de.deadzonemc.monster.boss.Hirte>> HIRTE = typ("hirte", de.deadzonemc.monster.boss.Hirte::new, 0.8F, 3.3F, 0x3a3428, 0xb08a3a);
    public static final DeferredHolder<EntityType<?>, EntityType<de.deadzonemc.monster.boss.Schlund>> SCHLUND = typ("schlund", de.deadzonemc.monster.boss.Schlund::new, 2.0F, 5.0F, 0xb08a78, 0x6a5440);
    public static final DeferredHolder<EntityType<?>, EntityType<de.deadzonemc.monster.boss.Pilzvater>> PILZVATER = typ("pilzvater", de.deadzonemc.monster.boss.Pilzvater::new, 1.8F, 3.6F, 0x7a7064, 0x8a9aa8);
    public static final DeferredHolder<EntityType<?>, EntityType<de.deadzonemc.monster.boss.Erfrorene>> ERFRORENE = typ("erfrorene", de.deadzonemc.monster.boss.Erfrorene::new, 1.4F, 3.8F, 0xa8bccb, 0xc8ecff, true);

    private static <T extends DeadzoneMonster> DeferredHolder<EntityType<?>, EntityType<T>> typ(
            String name, EntityType.EntityFactory<T> fabrik, float breite, float hoehe, int farbe1, int farbe2) {
        return typ(name, fabrik, breite, hoehe, farbe1, farbe2, false);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T extends DeadzoneMonster> DeferredHolder<EntityType<?>, EntityType<T>> typ(
            String name, EntityType.EntityFactory<T> fabrik, float breite, float hoehe, int farbe1, int farbe2, boolean feuerfest) {
        DeferredHolder<EntityType<?>, EntityType<T>> h = TYPEN.register(name, () -> {
            EntityType.Builder<T> b = EntityType.Builder.of(fabrik, MobCategory.MONSTER)
                    .sized(breite, hoehe)
                    .clientTrackingRange(hoehe > 3 ? 16 : 10);
            if (feuerfest) {
                b = b.fireImmune();
            }
            return b.build(DeadzoneMC.MODID + ":" + name);
        });
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
        e.put(GEFALLENER_SOLDAT.get(), GefallenerSoldat.attribute().build());
        e.put(GLUTLEIB.get(), Glutleib.attribute().build());
        e.put(SEUCHENHUND.get(), Seuchenhund.attribute().build());
        e.put(WUEHLER.get(), Wuehler.attribute().build());
        e.put(SAUGER.get(), Sauger.attribute().build());
        e.put(SEUCHENTRAEGER.get(), Seuchentraeger.attribute().build());
        e.put(ERTRUNKENER.get(), Ertrunkener.attribute().build());
        e.put(SPAEHERDROHNE.get(), Spaeherdrohne.attribute().build());
        e.put(LICHTMOTTE.get(), Lichtmotte.attribute().build());
        e.put(SCHEINTOTER.get(), Scheintoter.attribute().build());
        e.put(VERSCHMOLZENER.get(), de.deadzonemc.monster.boss.Verschmolzener.attribute().build());
        e.put(NEBELMUTTER.get(), de.deadzonemc.monster.boss.Nebelmutter.attribute().build());
        e.put(STRAHLENDE.get(), de.deadzonemc.monster.boss.Strahlende.attribute().build());
        e.put(DAS_HERZ.get(), de.deadzonemc.monster.boss.DasHerz.attribute().build());
        e.put(BRUTKOENIGIN.get(), de.deadzonemc.monster.boss.Brutkoenigin.attribute().build());
        e.put(SCHLAECHTER.get(), de.deadzonemc.monster.boss.Schlaechter.attribute().build());
        e.put(KOMMANDANT.get(), de.deadzonemc.monster.boss.Kommandant.attribute().build());
        e.put(HIRTE.get(), de.deadzonemc.monster.boss.Hirte.attribute().build());
        e.put(SCHLUND.get(), de.deadzonemc.monster.boss.Schlund.attribute().build());
        e.put(PILZVATER.get(), de.deadzonemc.monster.boss.Pilzvater.attribute().build());
        e.put(ERFRORENE.get(), de.deadzonemc.monster.boss.Erfrorene.attribute().build());
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
        normal(e, GEFALLENER_SOLDAT.get());
        normal(e, GLUTLEIB.get());
        normal(e, SEUCHENHUND.get());
        normal(e, SAUGER.get());
        normal(e, SEUCHENTRAEGER.get());
        normal(e, SCHEINTOTER.get());
        e.register(WUEHLER.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (typ, level, grund, pos, zufall) -> Monster.checkMonsterSpawnRules(typ, level, grund, pos, zufall)
                        && (grund == net.minecraft.world.entity.MobSpawnType.SPAWNER
                        || (pos.getY() < 50 && !level.canSeeSky(pos) && Wuehler.natuerlich(level.getBlockState(pos.below())))),
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        e.register(ERTRUNKENER.get(), SpawnPlacementTypes.IN_WATER, Heightmap.Types.OCEAN_FLOOR,
                (typ, level, grund, pos, zufall) -> level.getDifficulty() != net.minecraft.world.Difficulty.PEACEFUL
                        && level.getFluidState(pos).is(net.minecraft.tags.FluidTags.WATER)
                        && (grund == net.minecraft.world.entity.MobSpawnType.SPAWNER || Monster.isDarkEnoughToSpawn(level, pos, zufall)),
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        e.register(SPAEHERDROHNE.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (typ, level, grund, pos, zufall) -> Monster.checkAnyLightMonsterSpawnRules(typ, level, grund, pos, zufall)
                        && zufall.nextInt(3) == 0,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        e.register(LICHTMOTTE.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (typ, level, grund, pos, zufall) -> Monster.checkMonsterSpawnRules(typ, level, grund, pos, zufall)
                        && level.getLevel().isNight(),
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
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
