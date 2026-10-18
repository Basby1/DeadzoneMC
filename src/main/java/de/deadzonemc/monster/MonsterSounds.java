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
    public static final Map<String, List<String>> LISTE = Map.ofEntries(
            Map.entry("wanderer", List.of("ambient", "hurt", "death", "step")),
            Map.entry("schreier", List.of("ambient", "hurt", "death", "schrei")),
            Map.entry("lauerer", List.of("hurt", "death", "atmen", "schreck")),
            Map.entry("kriecher", List.of("ambient", "hurt", "death", "step")),
            Map.entry("der_stille", List.of("hurt", "death", "naehe")),
            Map.entry("imitator", List.of("hurt", "death")),
            Map.entry("nebelwesen", List.of("ambient", "hurt", "death")),
            Map.entry("schwarm", List.of("ambient", "hurt", "death", "step")),
            Map.entry("brut", List.of("ambient", "hurt", "death", "platzen")),
            Map.entry("juggernaut", List.of("ambient", "hurt", "death", "step", "brullen", "panzer")),
            Map.entry("gefallener_soldat", List.of("ambient", "hurt", "death", "step")),
            Map.entry("glutleib", List.of("ambient", "hurt", "death", "step", "glut")),
            Map.entry("seuchenhund", List.of("ambient", "hurt", "death", "step", "heulen")),
            Map.entry("wuehler", List.of("ambient", "hurt", "death", "graben")),
            Map.entry("sauger", List.of("ambient", "hurt", "death", "saugen")),
            Map.entry("seuchentraeger", List.of("ambient", "hurt", "death", "spruehen")),
            Map.entry("ertrunkener", List.of("ambient", "hurt", "death", "step")),
            Map.entry("spaeherdrohne", List.of("ambient", "hurt", "death", "alarm")),
            Map.entry("lichtmotte", List.of("ambient", "hurt", "death", "flattern")),
            Map.entry("scheintoter", List.of("atmen", "hurt", "death", "packen")),
            Map.entry("verschmolzener", List.of("ambient", "hurt", "death", "step", "brullen", "fusion")),
            Map.entry("nebelmutter", List.of("ambient", "hurt", "death", "brullen")),
            Map.entry("strahlende", List.of("ambient", "hurt", "death", "step", "brullen", "puls")),
            Map.entry("brutkoenigin", List.of("ambient", "hurt", "death", "step", "brullen", "legen")),
            Map.entry("schlaechter", List.of("ambient", "hurt", "death", "step", "brullen", "kette")),
            Map.entry("kommandant", List.of("ambient", "hurt", "death", "step", "brullen")),
            Map.entry("hirte", List.of("ambient", "hurt", "death", "glocke")),
            Map.entry("schlund", List.of("ambient", "hurt", "death", "brullen", "graben", "auftauchen")),
            Map.entry("pilzvater", List.of("ambient", "hurt", "death", "step", "sporen")),
            Map.entry("erfrorene", List.of("ambient", "hurt", "death", "step", "brullen", "atem")),
            Map.entry("das_herz", List.of("ambient", "hurt", "death", "brullen", "herzschlag")),
            Map.entry("der_ursprung", List.of("ambient", "hurt", "death", "brullen", "puls")),
            Map.entry("lenker", List.of("ambient", "hurt", "death")),
            Map.entry("spuk", List.of("ambient", "hurt", "death")),
            Map.entry("truegruede", List.of("ambient", "hurt", "death")),
            Map.entry("entreisser", List.of("ambient", "hurt", "death")),
            Map.entry("chimaere", List.of("ambient", "hurt", "death")),
            Map.entry("glasgaenger", List.of("ambient", "hurt", "death")),
            Map.entry("schwebender", List.of("ambient", "hurt", "death")),
            Map.entry("entlader", List.of("ambient", "hurt", "death")),
            Map.entry("echo", List.of("ambient", "hurt", "death")),
            Map.entry("zonenhirsch", List.of("ambient", "hurt", "death")),
            Map.entry("leerlaeufer", List.of("ambient", "hurt", "death")),
            Map.entry("wartungsspinne", List.of("ambient", "hurt", "death")),
            Map.entry("wachkugel", List.of("ambient", "hurt", "death")),
            Map.entry("versuchsobjekt", List.of("ambient", "hurt", "death")),
            Map.entry("konglomerat", List.of("ambient", "hurt", "death")),
            Map.entry("kryoschlaefer", List.of("ambient", "hurt", "death")),
            Map.entry("leerenaal", List.of("ambient", "hurt", "death")),
            Map.entry("klammerer", List.of("ambient", "hurt", "death")),
            Map.entry("hologeist", List.of("ambient", "hurt", "death")),
            Map.entry("forscher", List.of("ambient", "hurt", "death"))
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
