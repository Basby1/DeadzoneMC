package de.deadzonemc.wetter;

import java.util.Locale;

/**
 * Die Wetterlagen der Deadzone. Nebel ist der Normalzustand, alles andere macht Außeneinsätze
 * gefährlicher oder (selten) leichter.
 *
 * <p>Sichtweite und Farben gelten für den Tag in der Oberwelt; nachts und in Höhlen werden sie
 * auf dem Client noch einmal verdunkelt.</p>
 */
public enum WetterArt {
    //            Gewicht  min. Dauer  max. Dauer  Sicht  Nah   Rot    Grün   Blau   Regen
    KLAR(              6,   6_000,     10_000,    120F, 24F,  0.62F, 0.64F, 0.66F, false),
    LEICHTER_NEBEL(   40,  12_000,     24_000,     44F,  2F,  0.55F, 0.56F, 0.58F, false),
    DICHTER_NEBEL(    24,   6_000,     12_000,     14F,  0F,  0.50F, 0.51F, 0.52F, false),
    SAURER_REGEN(     18,   3_000,      8_000,     30F,  2F,  0.42F, 0.47F, 0.36F, true),
    ASCHESTURM(       12,   3_000,      6_000,     10F,  0F,  0.43F, 0.37F, 0.31F, false);

    private final int gewicht;
    private final int minDauer;
    private final int maxDauer;
    private final float sicht;
    private final float nah;
    private final float rot;
    private final float gruen;
    private final float blau;
    private final boolean regen;

    WetterArt(int gewicht, int minDauer, int maxDauer, float sicht, float nah,
              float rot, float gruen, float blau, boolean regen) {
        this.gewicht = gewicht;
        this.minDauer = minDauer;
        this.maxDauer = maxDauer;
        this.sicht = sicht;
        this.nah = nah;
        this.rot = rot;
        this.gruen = gruen;
        this.blau = blau;
        this.regen = regen;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public int gewicht() {
        return gewicht;
    }

    public int minDauer() {
        return minDauer;
    }

    public int maxDauer() {
        return maxDauer;
    }

    /** Ab dieser Entfernung (in Blöcken) ist alles im Nebel verschwunden. */
    public float sicht() {
        return sicht;
    }

    /** Bis zu dieser Entfernung ist noch gar kein Nebel. */
    public float nah() {
        return nah;
    }

    public float rot() {
        return rot;
    }

    public float gruen() {
        return gruen;
    }

    public float blau() {
        return blau;
    }

    /** Ob dabei richtiger Regen fällt. */
    public boolean regen() {
        return regen;
    }

    public static WetterArt ausId(String id) {
        for (WetterArt art : values()) {
            if (art.id().equals(id)) {
                return art;
            }
        }
        return null;
    }

    public static WetterArt ausNummer(int nummer) {
        WetterArt[] alle = values();
        return nummer >= 0 && nummer < alle.length ? alle[nummer] : LEICHTER_NEBEL;
    }
}
