package de.deadzonemc.client;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Einstellungen für den eigenen Rechner (Mods → Deadzone MC → Konfigurieren). Nur Darstellung und Klang –
 * nichts, was das Spiel leichter macht.
 */
public final class ClientEinstellungen {

    private ClientEinstellungen() {
    }

    private static final ModConfigSpec.Builder B = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue LAERM_ANZEIGE = B
            .comment("Lärm-Leiste neben der Schnellleiste anzeigen")
            .translation("deadzonemc.configuration.laermAnzeige")
            .define("laermAnzeige", true);
    public static final ModConfigSpec.BooleanValue FERNE_GERAEUSCHE = B
            .comment("Ferne Geräusche (Schüsse, Schreie, Sirenen, Flüstern …)")
            .translation("deadzonemc.configuration.ferneGeraeusche")
            .define("ferneGeraeusche", true);
    public static final ModConfigSpec.BooleanValue SCHRECK_EFFEKTE = B
            .comment("Schreckeffekte auf dem Bildschirm (Wackeln, Aufblitzen)")
            .translation("deadzonemc.configuration.schreckEffekte")
            .define("schreckEffekte", true);
    public static final ModConfigSpec.IntValue MUSIK_PAUSE = B
            .comment("Längste Pause zwischen zwei Musikstücken in Minuten (die kürzeste ist ein Drittel davon)")
            .translation("deadzonemc.configuration.musikPause")
            .defineInRange("musikPause", 3, 1, 20);

    public static final ModConfigSpec SPEC = B.build();

    /** Sicher lesen, auch bevor die Datei geladen ist. */
    public static boolean an(ModConfigSpec.BooleanValue wert) {
        try {
            return wert.get();
        } catch (IllegalStateException e) {
            return wert.getDefault();
        }
    }

    public static int musikPauseTicks() {
        try {
            return MUSIK_PAUSE.get() * 60 * 20;
        } catch (IllegalStateException e) {
            return 3 * 60 * 20;
        }
    }
}
