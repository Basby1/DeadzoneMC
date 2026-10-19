package de.deadzonemc.waffen.feuer;

import java.util.Set;

/**
 * Alle Schusswaffen von Deadzone. Werte: Schaden je Treffer (halbe Herzen), Kopfschuss-Faktor, Abstand zwischen
 * Schüssen (Ticks), Magazin, Nachladezeit (Ticks; bei "einzeln" je Patrone), Streuung aus der Hüfte/beim
 * Zielen (Grad), Rückstoß (Grad), Reichweite (Blöcke), Lärmradius (Blöcke), Schrotkugeln je Schuss.
 */
public enum WaffenArt {
    //                 id                 Kaliber          Schaden Kopf  Takt Auto  Mag  Laden  einzeln Streu Ziel  Rück Reich Lärm Kugeln Aufsätze
    P9("p9", Kaliber.NEUN_MM, 5.5F, 2.0F, 4, false, 15, 32, false, 3.2F, 0.9F, 2.4F, 48, 48, 1, Set.of("schalldaempfer", "taschenlampe", "magazin")),
    REVOLVER("revolver", Kaliber.MAG357, 10F, 2.2F, 9, false, 6, 14, true, 2.8F, 0.6F, 5.5F, 56, 60, 1, Set.of("taschenlampe")),
    MP9("mp9", Kaliber.NEUN_MM, 4.0F, 1.8F, 2, true, 30, 40, false, 4.0F, 1.8F, 1.3F, 44, 48, 1, Set.of("schalldaempfer", "taschenlampe", "magazin", "griff")),
    PUMPGUN("pumpgun", Kaliber.SCHROT, 3.2F, 1.5F, 17, false, 6, 11, true, 6.5F, 4.5F, 7F, 24, 64, 8, Set.of("taschenlampe", "griff")),
    DOPPELFLINTE("doppelflinte", Kaliber.SCHROT, 3.6F, 1.5F, 7, false, 2, 30, false, 7.5F, 5.5F, 9F, 20, 70, 9, Set.of()),
    STURMGEWEHR("sturmgewehr", Kaliber.F556, 6.0F, 2.0F, 3, true, 30, 44, false, 3.0F, 0.6F, 1.6F, 96, 72, 1, Set.of("schalldaempfer", "zielfernrohr", "taschenlampe", "magazin", "griff")),
    KALASCHNIKOW("kalaschnikow", Kaliber.F762, 7.0F, 2.0F, 3, true, 30, 48, false, 3.6F, 1.0F, 2.3F, 88, 76, 1, Set.of("schalldaempfer", "zielfernrohr", "taschenlampe", "magazin", "griff")),
    JAGDGEWEHR("jagdgewehr", Kaliber.F762, 16F, 2.5F, 26, false, 5, 16, true, 4.5F, 0.3F, 6F, 128, 80, 1, Set.of("zielfernrohr", "schalldaempfer")),
    PRAEZISIONSGEWEHR("praezisionsgewehr", Kaliber.F762, 13F, 2.5F, 11, false, 10, 56, false, 5.0F, 0.15F, 4F, 160, 90, 1, Set.of("zielfernrohr", "schalldaempfer", "magazin")),
    MG("mg", Kaliber.F762, 7.0F, 1.8F, 2, true, 80, 110, false, 5.5F, 2.6F, 1.5F, 96, 88, 1, Set.of("griff", "taschenlampe")),
    ROHRPISTOLE("rohrpistole", Kaliber.ROHR, 7.0F, 1.8F, 6, false, 1, 34, false, 5.0F, 2.5F, 6F, 28, 56, 1, Set.of()),
    ROHRGEWEHR("rohrgewehr", Kaliber.ROHR, 12F, 2.0F, 6, false, 1, 44, false, 4.0F, 1.5F, 7F, 48, 64, 1, Set.of()),
    ARMBRUST("armbrust", Kaliber.BOLZEN, 11F, 2.2F, 6, false, 1, 36, false, 2.0F, 0.5F, 2F, 64, 6, 1, Set.of("zielfernrohr")),
    // Klassenwaffen (jeder darf sie benutzen, die Lieblingsklasse hat Vorteile)
    KARABINER("karabiner", Kaliber.F556, 9F, 2.3F, 7, false, 20, 40, false, 2.4F, 0.25F, 2.2F, 128, 80, 1, Set.of("schalldaempfer", "zielfernrohr", "magazin", "griff")),
    NAGELPISTOLE("nagelpistole", Kaliber.NAGEL, 3.5F, 1.6F, 3, true, 40, 50, false, 4.5F, 2.0F, 0.6F, 24, 14, 1, Set.of()),
    BETAEUBUNGSPISTOLE("betaeubungspistole", Kaliber.SPRITZE, 2F, 1.5F, 14, false, 4, 36, false, 2.5F, 0.6F, 1.0F, 40, 8, 1, Set.of("zielfernrohr"));

    public final String id;
    public final Kaliber kaliber;
    public final float schaden;
    public final float kopf;
    public final int takt;
    public final boolean auto;
    public final int magazin;
    public final int laden;
    public final boolean einzeln;
    public final float streuung;
    public final float streuungZiel;
    public final float rueckstoss;
    public final int reichweite;
    public final int laerm;
    public final int kugeln;
    public final Set<String> aufsaetze;

    WaffenArt(String id, Kaliber kaliber, float schaden, float kopf, int takt, boolean auto, int magazin, int laden,
              boolean einzeln, float streuung, float streuungZiel, float rueckstoss, int reichweite, int laerm, int kugeln,
              Set<String> aufsaetze) {
        this.id = id;
        this.kaliber = kaliber;
        this.schaden = schaden;
        this.kopf = kopf;
        this.takt = takt;
        this.auto = auto;
        this.magazin = magazin;
        this.laden = laden;
        this.einzeln = einzeln;
        this.streuung = streuung;
        this.streuungZiel = streuungZiel;
        this.rueckstoss = rueckstoss;
        this.reichweite = reichweite;
        this.laerm = laerm;
        this.kugeln = kugeln;
        this.aufsaetze = aufsaetze;
    }

    /** Lange Waffen werden mit beiden Händen gehalten. */
    public boolean lang() {
        return this != P9 && this != REVOLVER && this != ROHRPISTOLE;
    }
}
