package de.deadzonemc;

import de.deadzonemc.netz.Netz;
import de.deadzonemc.wetter.WetterSystem;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

/** Einstiegspunkt der Mod "Deadzone MC". */
@Mod(DeadzoneMC.MODID)
public class DeadzoneMC {
    public static final String MODID = "deadzonemc";

    public DeadzoneMC(IEventBus modBus, ModContainer container) {
        ModRegistry.registrieren(modBus);
        de.deadzonemc.monster.MonsterTypen.registrieren(modBus);
        modBus.addListener(Netz::registrieren);

        NeoForge.EVENT_BUS.addListener(WetterSystem::serverGestartet);
        NeoForge.EVENT_BUS.addListener(WetterSystem::serverTick);
        NeoForge.EVENT_BUS.addListener(WetterSystem::spielerBetritt);
        NeoForge.EVENT_BUS.addListener(WetterSystem::dimensionGewechselt);
        NeoForge.EVENT_BUS.addListener(WetterSystem::befehleRegistrieren);
        NeoForge.EVENT_BUS.addListener(Spielstart::beimBetreten);
        NeoForge.EVENT_BUS.addListener(de.deadzonemc.monster.NebelSpawner::tick);
        NeoForge.EVENT_BUS.addListener(de.deadzonemc.strahlung.Strahlung::tick);
        NeoForge.EVENT_BUS.addListener(de.deadzonemc.monster.Entdeckung::tick);
        NeoForge.EVENT_BUS.addListener(de.deadzonemc.basis.Basisschutz::spawnPruefen);
        NeoForge.EVENT_BUS.addListener(de.deadzonemc.basis.Basisschutz::tick);
        NeoForge.EVENT_BUS.addListener(de.deadzonemc.basis.Basisbefehle::registrieren);
        NeoForge.EVENT_BUS.addListener(de.deadzonemc.klasse.Klassen::betreten);
        NeoForge.EVENT_BUS.addListener(de.deadzonemc.klasse.Klassen::respawn);
        NeoForge.EVENT_BUS.addListener(de.deadzonemc.klasse.Klassen::schaden);
        NeoForge.EVENT_BUS.addListener(de.deadzonemc.klasse.Klassen::tick);
        NeoForge.EVENT_BUS.addListener(de.deadzonemc.monster.Entdeckung::betreten);
        NeoForge.EVENT_BUS.addListener(de.deadzonemc.monster.Entdeckung::respawn);
        NeoForge.EVENT_BUS.addListener(de.deadzonemc.welt.Ortsbefehle::registrieren);
        if (Selbsttest.AKTIV) {
            NeoForge.EVENT_BUS.addListener(Selbsttest::tick);
        }
    }
}
