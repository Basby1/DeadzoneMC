package de.deadzonemc.kino;

import de.deadzonemc.DeadzoneMC;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Darsteller des Kino-Intros. Sie existieren nur auf dem Client, solange das Intro läuft. */
public final class KinoTypen {

    private KinoTypen() {
    }

    public static final DeferredRegister<EntityType<?>> TYPEN = DeferredRegister.create(Registries.ENTITY_TYPE, DeadzoneMC.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<KinoFigur>> FIGUR = TYPEN.register("kino_figur",
            () -> EntityType.Builder.<KinoFigur>of(KinoFigur::new, MobCategory.MISC).sized(0.6F, 1.8F).noSave().noSummon()
                    .clientTrackingRange(0).build(DeadzoneMC.MODID + ":kino_figur"));
    public static final DeferredHolder<EntityType<?>, EntityType<Transporter>> TRANSPORTER = TYPEN.register("kino_transporter",
            () -> EntityType.Builder.<Transporter>of(Transporter::new, MobCategory.MISC).sized(3.5F, 3.4F).noSave().noSummon()
                    .clientTrackingRange(0).build(DeadzoneMC.MODID + ":kino_transporter"));
    public static final DeferredHolder<EntityType<?>, EntityType<UrsprungSchatten>> SCHATTEN = TYPEN.register("kino_schatten",
            () -> EntityType.Builder.<UrsprungSchatten>of(UrsprungSchatten::new, MobCategory.MISC).sized(40F, 90F).noSave().noSummon()
                    .clientTrackingRange(0).build(DeadzoneMC.MODID + ":kino_schatten"));

    public static final DeferredHolder<EntityType<?>, EntityType<KinoBoot>> BOOT = TYPEN.register("kino_boot",
            () -> EntityType.Builder.<KinoBoot>of(KinoBoot::new, MobCategory.MISC).sized(4.5F, 3F).noSave().noSummon()
                    .clientTrackingRange(0).build(DeadzoneMC.MODID + ":kino_boot"));
    public static final DeferredHolder<EntityType<?>, EntityType<KinoRuinen>> RUINEN = TYPEN.register("kino_ruinen",
            () -> EntityType.Builder.<KinoRuinen>of(KinoRuinen::new, MobCategory.MISC).sized(60F, 40F).noSave().noSummon()
                    .clientTrackingRange(0).build(DeadzoneMC.MODID + ":kino_ruinen"));

    public static void registrieren(IEventBus modBus) {
        TYPEN.register(modBus);
        modBus.addListener((EntityAttributeCreationEvent e) -> e.put(FIGUR.get(), LivingEntity.createLivingAttributes().build()));
    }
}
