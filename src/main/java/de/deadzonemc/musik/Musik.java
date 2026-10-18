package de.deadzonemc.musik;

import de.deadzonemc.DeadzoneMC;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Die Musikstücke von Deadzone MC (Dateien unter assets/deadzonemc/sounds/musik, erzeugt von werkzeuge/musik.py). */
public final class Musik {

    private Musik() {
    }

    public static final DeferredRegister<SoundEvent> KLAENGE = DeferredRegister.create(Registries.SOUND_EVENT, DeadzoneMC.MODID);

    public static final DeferredHolder<SoundEvent, SoundEvent> TITEL = stueck("titel");
    public static final DeferredHolder<SoundEvent, SoundEvent> MORGEN = stueck("morgen");
    public static final DeferredHolder<SoundEvent, SoundEvent> NEBEL = stueck("nebel");
    public static final DeferredHolder<SoundEvent, SoundEvent> NACHT = stueck("nacht");
    public static final DeferredHolder<SoundEvent, SoundEvent> TIEF = stueck("tief");
    public static final DeferredHolder<SoundEvent, SoundEvent> GEFECHT = stueck("gefecht");
    public static final DeferredHolder<SoundEvent, SoundEvent> STADT = stueck("stadt");
    public static final DeferredHolder<SoundEvent, SoundEvent> WILDNIS = stueck("wildnis");
    public static final DeferredHolder<SoundEvent, SoundEvent> ZUFLUCHT = stueck("zuflucht");

    private static DeferredHolder<SoundEvent, SoundEvent> stueck(String name) {
        return KLAENGE.register("musik." + name, () -> SoundEvent.createVariableRangeEvent(
                ResourceLocation.fromNamespaceAndPath(DeadzoneMC.MODID, "musik." + name)));
    }

    public static void registrieren(IEventBus modBus) {
        KLAENGE.register(modBus);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(OrtMelder::tick);
    }
}
