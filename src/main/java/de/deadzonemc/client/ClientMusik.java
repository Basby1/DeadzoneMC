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

    private static final int RUHE_MIN = 3 * 60 * 20;
    private static final int RUHE_MAX = 8 * 60 * 20;

    private static Music gefecht;
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

    private ClientMusik() {
    }

    private static void anlegen() {
        if (gefecht == null) {
            gefecht = new Music(Musik.GEFECHT, 0, 0, true);
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
        if (mc.player == null || mc.level == null || mc.level.dimension() != Level.OVERWORLD) {
            return;
        }
        anlegen();
        if (mc.gui.getBossOverlay().shouldPlayMusic() || ClientWetter.emission() > 0) {
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
        long tag = mc.level.getDayTime() / 24000L;
        e.setMusic(switch ((int) (tag % 3)) {
            case 0 -> wildnis;
            case 1 -> morgen;
            default -> titel;
        });
    }
}
