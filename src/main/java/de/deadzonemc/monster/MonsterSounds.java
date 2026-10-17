package de.deadzonemc.monster;

import de.deadzonemc.DeadzoneMC;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Geräusche der Monster. Welche Dateien dahinterstecken (und wie hoch oder tief sie abgespielt werden),
 * steht in assets/deadzonemc/sounds.json – erzeugt von werkzeuge/monster/sounds.py.
 */
public final class MonsterSounds {

    private MonsterSounds() {
    }

    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, DeadzoneMC.MODID);

    /** Alle Geräusche: Monsterart -> Arten von Geräuschen. Muss zu werkzeuge/monster/sounds.py passen. */
    public static final Map<String, List<String>> LISTE = Map.of(
            "wanderer", List.of("ambient", "hurt", "death", "step"),
            "schreier", List.of("ambient", "hurt", "death", "schrei"),
            "lauerer", List.of("hurt", "death", "atmen", "schreck"),
            "kriecher", List.of("ambient", "hurt", "death", "step"),
            "der_stille", List.of("hurt", "death", "naehe"),
            "imitator", List.of("hurt", "death"),
            "nebelwesen", List.of("ambient", "hurt", "death"),
            "schwarm", List.of("ambient", "hurt", "death", "step"),
            "brut", List.of("ambient", "hurt", "death", "platzen"),
            "juggernaut", List.of("ambient", "hurt", "death", "step", "brullen", "panzer")
    );

    private static final Map<String, DeferredHolder<SoundEvent, SoundEvent>> ALLE = new HashMap<>();

    static {
        LISTE.forEach((art, arten) -> {
            for (String a : arten) {
                String name = "monster." + art + "." + a;
                ALLE.put(art + "." + a, SOUNDS.register(name, () ->
                        SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(DeadzoneMC.MODID, name))));
            }
        });
    }

    /** Das Geräusch oder null, wenn es für dieses Monster keins gibt. */
    public static SoundEvent holen(String art, String was) {
        DeferredHolder<SoundEvent, SoundEvent> h = ALLE.get(art + "." + was);
        return h == null ? null : h.get();
    }
}
