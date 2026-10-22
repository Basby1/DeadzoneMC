package de.deadzonemc.client;

import de.deadzonemc.client.charakter.CharakterScreen;
import de.deadzonemc.client.kino.ClientKino;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.slf4j.Logger;

/**
 * Automatischer Client-Test (nur mit -Pclienttest, siehe .github/workflows/client.yml): legt eine Welt an, geht durch
 * Charakter-Editor, Kino-Intro, Erwachen und Klassenwahl und macht dabei Bildschirmfotos.
 */
public final class ClientSelbsttest {

    private ClientSelbsttest() {
    }

    private static final Logger LOG = com.mojang.logging.LogUtils.getLogger();
    private static final float[] KINO_BILDER = {2, 6, 9, 12, 15, 18, 22, 25, 28, 29.15F, 31, 32.5F, 34, 36.5F, 40, 42, 46.6F, 50};
    private static final int[] INTRO_BILDER = {12, 30, 60, 120, 200, 270};

    private static int phase;
    private static int t;
    private static int marke;
    private static int bild;
    private static int introTick;
    private static int gesamt;
    private static boolean kinoGemeldet;
    private static boolean introGemeldet;

    public static boolean an() {
        return Boolean.getBoolean("deadzonemc.clienttest");
    }

    private static void foto(Minecraft mc, String name) {
        String datei = String.format("%02d_%s.png", ++bild, name);
        Screenshot.grab(mc.gameDirectory, datei, mc.getMainRenderTarget(), c -> LOG.info("[Clienttest] Bild {}", datei));
    }

    public static void tick(ClientTickEvent.Post e) {
        Minecraft mc = Minecraft.getInstance();
        t++;
        if (++gesamt > 20 * 60 * 22) {
            LOG.error("[Clienttest] Zeit abgelaufen in Phase {}", phase);
            mc.stop();
            return;
        }
        try {
            schritt(mc);
        } catch (Exception ex) {
            LOG.error("[Clienttest] Fehler in Phase {}", phase, ex);
            foto(mc, "fehler");
            mc.stop();
        }
    }

    private static void weiter() {
        phase++;
        t = 0;
        LOG.info("[Clienttest] Phase {}", phase);
    }

    private static void schritt(Minecraft mc) {
        switch (phase) {
            case 0 -> {
                if (mc.screen instanceof TitleScreen && t > 60) {
                    foto(mc, "titel");
                    LevelSettings ls = new LevelSettings("kinotest" + System.currentTimeMillis() % 100000, GameType.SURVIVAL, false,
                            Difficulty.NORMAL, true, new GameRules(), WorldDataConfiguration.DEFAULT);
                    mc.createWorldOpenFlows().createFreshLevel(ls.levelName(), ls, new WorldOptions(424242L, true, false),
                            WorldPresets::createNormalWorldDimensions, mc.screen);
                    weiter();
                }
            }
            case 1 -> { // warten auf den Editor
                if (mc.screen instanceof CharakterScreen && mc.player != null) {
                    weiter();
                } else if (t > 20 * 60 * 6) {
                    foto(mc, "kein_editor");
                    phase = 5;
                }
            }
            case 2 -> {
                if (!(mc.screen instanceof CharakterScreen cs)) {
                    weiter();
                    return;
                }
                switch (t) {
                    case 60 -> foto(mc, "editor_kopf");
                    case 70 -> cs.testReiter(1);
                    case 90 -> foto(mc, "editor_kleidung");
                    case 100 -> cs.testReiter(2);
                    case 120 -> foto(mc, "editor_ausruestung");
                    case 130 -> {
                        cs.testReiter(0);
                        cs.testZufall();
                    }
                    case 150 -> foto(mc, "editor_zufall");
                    case 160 -> cs.testZufall();
                    case 175 -> foto(mc, "editor_zufall2");
                    case 190 -> cs.testFertig();
                    default -> {
                    }
                }
            }
            case 3 -> { // Kino
                if (t == 10) {
                    foto(mc, "warten");
                }
                if (ClientKino.aktiv()) {
                    if (!kinoGemeldet) {
                        kinoGemeldet = true;
                        LOG.info("[Clienttest] KINO_START {}", System.currentTimeMillis());
                    }
                    float s = t > 0 ? kinoZeit() : 0;
                    while (marke < KINO_BILDER.length && s >= KINO_BILDER[marke]) {
                        foto(mc, String.format("kino_%04.1f", KINO_BILDER[marke]).replace(',', '.'));
                        marke++;
                    }
                } else if (mc.screen instanceof IntroScreen) {
                    weiter();
                } else if (t > 20 * 120) {
                    foto(mc, "kein_kino");
                    weiter();
                }
            }
            case 4 -> { // Erwachen
                if (mc.screen instanceof IntroScreen) {
                    introTick++;
                    for (int z : INTRO_BILDER) {
                        if (introTick == z) {
                            foto(mc, "erwachen_" + z);
                        }
                    }
                } else if (mc.screen instanceof KlassenScreen) {
                    if (!introGemeldet) {
                        introGemeldet = true;
                        LOG.info("[Clienttest] INTRO_ENDE {}", System.currentTimeMillis());
                    }
                    if (t > 40) {
                        foto(mc, "klassenwahl");
                        mc.setScreen(null);
                        weiter();
                    }
                } else if (t > 20 * 60) {
                    foto(mc, "keine_klassenwahl");
                    weiter();
                }
            }
            case 5 -> {
                if (t == 20) {
                    mc.setScreen(null);
                    mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
                }
                if (t == 80) {
                    foto(mc, "skin_vorne");
                    mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
                }
                if (t == 120) {
                    foto(mc, "skin_hinten");
                    mc.options.setCameraType(CameraType.FIRST_PERSON);
                }
                // Das Wrack neben dem Startplatz (liegt 7 Blöcke östlich)
                if (t == 125 && mc.player != null) {
                    mc.options.hideGui = true;
                    mc.player.connection.sendCommand("gamemode spectator");
                    mc.player.connection.sendCommand("tp @s ~-4 ~7 ~-8 -54 27");
                }
                if (t == 220) {
                    foto(mc, "wrack_1");
                    mc.player.connection.sendCommand("tp @s ~22 ~-1 ~18 132 25");
                }
                if (t == 300) {
                    foto(mc, "wrack_2");
                    mc.player.connection.sendCommand("tp @s ~-11 ~-4 ~-17 -10 15");
                }
                if (t == 380) {
                    foto(mc, "wrack_3");
                }
                if (t == 390) {
                    weiter();
                }
            }
            case 6 -> fahrzeugeBesuchen(mc);
            case 7 -> endBesuchen(mc);
            default -> {
            }
        }
    }

    /** Die Fahrzeugschau aufbauen und jedes Fahrzeug fotografieren. */
    private static void fahrzeugeBesuchen(Minecraft mc) {
        if (mc.player == null) {
            return;
        }
        if (t == 5) {
            mc.options.hideGui = true;
            mc.player.connection.sendCommand("gamemode spectator");
            mc.player.connection.sendCommand("time set noon");
            mc.player.connection.sendCommand("weather clear");
            mc.player.connection.sendCommand("deadzone nebel aus");
        }
        if (t == 20) {
            mc.player.connection.sendCommand("deadzone fahrzeugschau");
        }
        int start = 20 * 35;
        int n = de.deadzonemc.welt.Fahrzeugschau.anzahlBlicke();
        int i = (t - start) / 240;
        int imSchritt = (t - start) % 240;
        if (t >= start && i < n) {
            if (imSchritt == 0) {
                mc.player.connection.sendCommand("deadzone fahrzeugschau " + i);
            }
            if (imSchritt == 220) {
                foto(mc, "fz_" + de.deadzonemc.welt.Fahrzeugschau.blickName(i));
            }
        }
        if (t >= start + n * 240 + 10) {
            weiter();
        }
    }

    /** Ins End zur Orbitalstation (Mitte 0/90) und von mehreren Seiten fotografieren: x, Höhe über der Plattform, z, Gier, Neigung. */
    private static final String[][] END_BLICKE = {
            {"0 26 18 0 20", "end_anflug"},
            {"60 20 90 90 15", "end_station_seite"},
            {"45 16 140 140 12", "end_station_schraeg"},
            {"0 95 90 0 90", "end_von_oben"},
            {"0 6 70 0 -40", "end_reaktorring"},
            {"-40 10 98 -110 20", "end_westarm"},
            {"10 3 98 180 5", "end_arena"},
    };
    private static int plattform = Integer.MIN_VALUE;

    private static void endBesuchen(Minecraft mc) {
        if (mc.player == null) {
            return;
        }
        if (t == 5) {
            mc.options.hideGui = true;
            mc.player.connection.sendCommand("gamemode spectator");
            mc.player.connection.sendCommand("execute in minecraft:the_end run tp @s 0 110 40 0 20");
        }
        int start = 20 * 30; // Station bauen lassen, Chunks laden
        if (t == start - 20 && mc.level != null) {
            plattform = mc.level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, 0, 104) - 1;
            LOG.info("[Clienttest] Plattform auf Höhe {}", plattform);
            if (plattform < 20) {
                plattform = 64;
            }
        }
        int i = (t - start) / 360;
        int imSchritt = (t - start) % 360;
        if (t >= start && i < END_BLICKE.length) {
            if (imSchritt == 0) {
                String[] w = END_BLICKE[i][0].split(" ");
                int y = plattform + Integer.parseInt(w[1]);
                mc.player.connection.sendCommand("execute in minecraft:the_end run tp @s " + w[0] + " " + y + " " + w[2] + " " + w[3] + " " + w[4]);
            }
            if (imSchritt == 340) {
                foto(mc, END_BLICKE[i][1]);
            }
        }
        if (t >= start + END_BLICKE.length * 360 + 20) {
            LOG.info("[Clienttest] fertig");
            mc.stop();
        }
    }

    private static float kinoZeit() {
        try {
            var m = ClientKino.class.getDeclaredMethod("zeit", float.class);
            m.setAccessible(true);
            return (float) m.invoke(null, 0F);
        } catch (Exception e) {
            return 0;
        }
    }
}
