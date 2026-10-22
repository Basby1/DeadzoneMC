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
        de.deadzonemc.monster.Verbote.registrieren();
        Lebensbilanz.registrieren();
        Schlafen.registrieren();
        de.deadzonemc.monster.Wiedergaenger.registrieren();
        de.deadzonemc.klasse.Klassenvorteile.registrieren();
        de.deadzonemc.geschichte.Archiv.registrieren();
        de.deadzonemc.monster.Rudel.registrieren();
        de.deadzonemc.fraktion.Fraktionen.registrieren();
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(de.deadzonemc.monster.Mimik::erscheinen);
        de.deadzonemc.mensch.MenschTypen.registrieren(modBus);
        de.deadzonemc.zone.Zone.registrieren(modBus);
        de.deadzonemc.verletzung.Verletzungen.registrieren(modBus);
        de.deadzonemc.falle.Fallen.registrieren(modBus);
        de.deadzonemc.bossmod.Bossmods.registrieren(modBus);
        de.deadzonemc.welt.deko.Wanddeko.registrieren(modBus);
        de.deadzonemc.funk.Funk.registrieren(modBus);
        de.deadzonemc.strom.Strom.registrieren(modBus);
        de.deadzonemc.gadget.Gadgets.registrieren(modBus);
        de.deadzonemc.gadget.Nachwirkung.registrieren();
        de.deadzonemc.welt.schild.Ladenschild.registrieren(modBus);
        de.deadzonemc.beute.Beute.registrieren(modBus);
        de.deadzonemc.waffen.feuer.Feuerwaffen.registrieren(modBus);
        de.deadzonemc.waffen.wurf.Wurfwaffen.registrieren(modBus);
        de.deadzonemc.ausruestung.Ausruestung.registrieren(modBus);
        de.deadzonemc.verzauberung.Zonenverzauberung.registrieren(modBus);
        de.deadzonemc.zelt.Zelte.registrieren(modBus);
        de.deadzonemc.geschichte.Geschichte.registrieren(modBus);
        de.deadzonemc.orbit.Orbitalstation.registrieren(modBus);
        de.deadzonemc.waffen.Waffenpflege.registrieren(modBus);
        de.deadzonemc.musik.Musik.registrieren(modBus);
        modBus.addListener(Netz::registrieren);
        modBus.addListener(de.deadzonemc.welt.Staedte::imc);

        NeoForge.EVENT_BUS.addListener(WetterSystem::serverGestartet);
        NeoForge.EVENT_BUS.addListener(de.deadzonemc.welt.StartGebiet::serverGestartet);
        NeoForge.EVENT_BUS.addListener(de.deadzonemc.welt.Staedte::weltGeladen);
        NeoForge.EVENT_BUS.addListener(WetterSystem::serverTick);
        NeoForge.EVENT_BUS.addListener(WetterSystem::spielerBetritt);
        NeoForge.EVENT_BUS.addListener(WetterSystem::dimensionGewechselt);
        NeoForge.EVENT_BUS.addListener(WetterSystem::befehleRegistrieren);
        NeoForge.EVENT_BUS.addListener(Spielstart::beimBetreten);
        NeoForge.EVENT_BUS.addListener(de.deadzonemc.monster.SpawnSchutz::betreten);
        NeoForge.EVENT_BUS.addListener(de.deadzonemc.monster.SpawnSchutz::respawn);
        NeoForge.EVENT_BUS.addListener(de.deadzonemc.monster.SpawnSchutz::erscheinen);
        NeoForge.EVENT_BUS.addListener(de.deadzonemc.monster.SpawnSchutz::zielen);
        NeoForge.EVENT_BUS.addListener(de.deadzonemc.monster.SpawnSchutz::schaden);
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.entity.player.AttackEntityEvent e) ->
                de.deadzonemc.monster.SpawnSchutz.aufhoeren(e.getEntity()));
        NeoForge.EVENT_BUS.addListener(Spielstart::nachRespawn);
        NeoForge.EVENT_BUS.addListener(de.deadzonemc.basis.Zonenanzeige::tick);
        NeoForge.EVENT_BUS.addListener(Spielstart::peilen);
        NeoForge.EVENT_BUS.addListener(Spielstart::ersteNacht);
        NeoForge.EVENT_BUS.addListener(de.deadzonemc.monster.NebelSpawner::tick);
        NeoForge.EVENT_BUS.addListener(de.deadzonemc.monster.StadtHorden::tick);
        NeoForge.EVENT_BUS.addListener(de.deadzonemc.monster.Herden::tick);
        NeoForge.EVENT_BUS.addListener(de.deadzonemc.monster.Laerm::explosion);
        NeoForge.EVENT_BUS.addListener(de.deadzonemc.monster.KeineSonne::tick);
        NeoForge.EVENT_BUS.addListener(de.deadzonemc.monster.Laerm::blockBricht);
        NeoForge.EVENT_BUS.addListener(de.deadzonemc.monster.Laerm::spieler);
        NeoForge.EVENT_BUS.addListener(de.deadzonemc.monster.boss.BossSpawner::tick);
        NeoForge.EVENT_BUS.addListener(de.deadzonemc.strahlung.Strahlung::tick);
        NeoForge.EVENT_BUS.addListener(de.deadzonemc.monster.Entdeckung::tick);
        NeoForge.EVENT_BUS.addListener(de.deadzonemc.basis.Basisschutz::spawnPruefen);
        NeoForge.EVENT_BUS.addListener(de.deadzonemc.basis.Basisschutz::tick);
        NeoForge.EVENT_BUS.addListener(de.deadzonemc.basis.Basisbefehle::registrieren);
        NeoForge.EVENT_BUS.addListener(de.deadzonemc.klasse.Klassen::betreten);
        NeoForge.EVENT_BUS.addListener(de.deadzonemc.klasse.Klassen::respawn);
        NeoForge.EVENT_BUS.addListener(de.deadzonemc.klasse.Klassen::verlassen);
        de.deadzonemc.klasse.Kombinationen.registrieren();
        de.deadzonemc.monster.Mutation.registrieren();
        NeoForge.EVENT_BUS.addListener(de.deadzonemc.mensch.Gebiete::tick);
        NeoForge.EVENT_BUS.addListener(de.deadzonemc.geschichte.Entscheidung::befehl);
        NeoForge.EVENT_BUS.addListener(de.deadzonemc.geschichte.Entscheidung::anmelden);
        NeoForge.EVENT_BUS.addListener(de.deadzonemc.geschichte.Tutorial::anmelden);
        NeoForge.EVENT_BUS.addListener(de.deadzonemc.klasse.Klassen::schaden);
        NeoForge.EVENT_BUS.addListener(de.deadzonemc.klasse.Talente::schaden);
        NeoForge.EVENT_BUS.addListener(de.deadzonemc.klasse.Talente::effekt);
        NeoForge.EVENT_BUS.addListener(de.deadzonemc.klasse.Talente::ziel);
        NeoForge.EVENT_BUS.addListener(de.deadzonemc.klasse.Klassen::tick);
        NeoForge.EVENT_BUS.addListener(Tod::gestorben);
        NeoForge.EVENT_BUS.addListener(de.deadzonemc.quest.Auftraege::getoetet);
        NeoForge.EVENT_BUS.addListener(de.deadzonemc.quest.Auftraege::tick);
        NeoForge.EVENT_BUS.addListener(de.deadzonemc.quest.Auftraege::befehle);
        NeoForge.EVENT_BUS.addListener(de.deadzonemc.zuflucht.ZufluchtSchutz::spawnPruefen);
        NeoForge.EVENT_BUS.addListener(de.deadzonemc.zuflucht.ZufluchtSchutz::tick);
        NeoForge.EVENT_BUS.addListener(de.deadzonemc.mensch.FraktionsSpawner::tick);
        NeoForge.EVENT_BUS.addListener(de.deadzonemc.monster.Entdeckung::betreten);
        NeoForge.EVENT_BUS.addListener(de.deadzonemc.monster.Entdeckung::respawn);
        NeoForge.EVENT_BUS.addListener(de.deadzonemc.welt.Ortsbefehle::registrieren);
        if (Selbsttest.AKTIV) {
            NeoForge.EVENT_BUS.addListener(Selbsttest::tick);
        }
    }
}
