package de.deadzonemc.client;

import de.deadzonemc.musik.Musik;
import de.deadzonemc.wetter.WetterArt;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.Music;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.event.SelectMusicEvent;

/**
 * Wählt die Musik im Spiel je nach Lage: Bosskampf oder Emission → Gefecht, unter Tage → Unter Tage,
 * in einer Zuflucht → Lagerfeuer, nachts → Nachtwache, dichter Nebel oder Asche → Nebelwand, in der Stadt → Tote Stadt,
 * sonst draußen auf dem Land Weites Land, Grauer Morgen oder Sektor 4 (je nach Tag).
 * Zwischen den ruhigen Stücken liegen ein paar Minuten Stille – Musik soll Stimmung machen, nicht dudeln.
 */
public final class ClientMusik {

    private static final int RUHE_MIN = 60 * 20;
    private static final int RUHE_MAX = 3 * 60 * 20;

    private static Music gefecht;
    private static Music endkampf;
    private static Music tief;
    private static Music nacht;
    private static Music nebel;
    private static Music morgen;
    private static Music titel;
    private static Music stadt;
    private static Music wildnis;
    private static Music zuflucht;
    /** 0 = Land, 1 = Stadt, 2 = Zuflucht (vom Server gemeldet). */
    private static int ort;

    public static void ortSetzen(int o) {
        ort = o;
    }

    public static int ort() {
        return ort;
    }

    private ClientMusik() {
    }

    private static Music frost;
    private static Music fleisch;
    private static Music marsch;

    /** Jeder Boss hat sein Thema: Kälte/Nebel, Fleisch/Fäulnis, Befehl/Marsch – die Endbosse den großen Endkampf. */
    private static Music bossMusik(Minecraft mc) {
        if (frost == null) {
            frost = new Music(Musik.BOSS_FROST, 0, 0, true);
            fleisch = new Music(Musik.BOSS_FLEISCH, 0, 0, true);
            marsch = new Music(Musik.BOSS_MARSCH, 0, 0, true);
        }
        de.deadzonemc.monster.boss.Boss naechster = null;
        double best = 80 * 80;
        for (var b : mc.level.getEntitiesOfClass(de.deadzonemc.monster.boss.Boss.class, mc.player.getBoundingBox().inflate(80))) {
            double d = b.distanceToSqr(mc.player);
            if (d < best) {
                best = d;
                naechster = b;
            }
        }
        if (naechster == null) {
            return endkampf;
        }
        String art = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(naechster.getType()).getPath();
        return switch (art) {
            case "erfrorene", "nebelmutter" -> frost;
            case "schlaechter", "verschmolzener", "brutkoenigin", "schlund", "pilzvater", "schwarmmutter" -> fleisch;
            case "kommandant", "strahlende", "hirte", "pilot" -> marsch;
            default -> endkampf;
        };
    }

    private static long gefechtGeprueft;
    private static long gefechtBis;

    /** Mindestens fünf angriffslustige Monster in der Nähe → Gefecht; endet erst 15 s nach dem letzten Andrang. */
    private static boolean gefechtLage(Minecraft mc) {
        long jetzt = mc.level.getGameTime();
        if (jetzt - gefechtGeprueft >= 20 || jetzt < gefechtGeprueft) {
            gefechtGeprueft = jetzt;
            int n = mc.level.getEntitiesOfClass(net.minecraft.world.entity.monster.Monster.class,
                    mc.player.getBoundingBox().inflate(20, 10, 20), m -> m.isAlive() && m.isAggressive()).size();
            if (n >= 5) {
                gefechtBis = jetzt + 300;
            }
        }
        return jetzt < gefechtBis;
    }

    private static void anlegen() {
        if (gefecht == null) {
            gefecht = new Music(Musik.GEFECHT, 0, 0, true);
            endkampf = new Music(Musik.ENDKAMPF, 0, 0, true);
            tief = new Music(Musik.TIEF, RUHE_MIN, RUHE_MAX, false);
            nacht = new Music(Musik.NACHT, RUHE_MIN, RUHE_MAX, false);
            nebel = new Music(Musik.NEBEL, RUHE_MIN, RUHE_MAX, false);
            morgen = new Music(Musik.MORGEN, RUHE_MIN, RUHE_MAX, false);
            titel = new Music(Musik.TITEL, RUHE_MIN, RUHE_MAX, false);
            stadt = new Music(Musik.STADT, RUHE_MIN, RUHE_MAX, false);
            wildnis = new Music(Musik.WILDNIS, RUHE_MIN, RUHE_MAX, false);
            // In der Zuflucht darf es gemütlich sein: kürzere Pausen, ersetzt die Stimmung von draußen
            zuflucht = new Music(Musik.ZUFLUCHT, 600, 2400, true);
        }
    }

    public static void waehlen(SelectMusicEvent e) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }
        anlegen();
        // Bosskampf (auch in Nether und End): epische Kampfmusik
        if (mc.gui.getBossOverlay().shouldPlayMusic()) {
            e.setMusic(bossMusik(mc));
            return;
        }
        // Große Gefechte (Horde, Rudel, Überfall): treibende Kampfmusik, die nach dem Kampf noch etwas nachklingt
        if (gefechtLage(mc)) {
            e.setMusic(gefecht);
            return;
        }
        if (mc.level.dimension() != Level.OVERWORLD) {
            return;
        }
        if (ClientWetter.emission() > 0) {
            e.setMusic(gefecht);
            return;
        }
        BlockPos kopf = BlockPos.containing(mc.player.getEyePosition());
        if (!mc.level.canSeeSky(kopf) && kopf.getY() < mc.level.getSeaLevel() - 8) {
            e.setMusic(tief);
            return;
        }
        if (ort == 2) {
            e.setMusic(zuflucht);
            return;
        }
        long zeit = mc.level.getDayTime() % 24000L;
        if (zeit > 13000 && zeit < 23000) {
            e.setMusic(nacht);
            return;
        }
        WetterArt w = ClientWetter.art();
        if (w == WetterArt.DICHTER_NEBEL || w == WetterArt.ASCHESTURM) {
            e.setMusic(nebel);
            return;
        }
        if (ort == 1) {
            e.setMusic(stadt);
            return;
        }
        e.setMusic(wildnis); // Pool: Weites Land, Grauer Morgen, Sektor 4
    }
}
