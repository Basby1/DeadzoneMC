package de.deadzonemc.monster.dimension;

/** Die 20 Wesen aus dem Herz der Zone (Nether) und der Orbitalstation (End). */
public enum DimArt {
    // Herz der Zone
    LENKER("lenker", 40, 0.24, 6, 4, Faehigkeit.ZIEHEN, false, 2.4F, 2.6F),
    SPUK("spuk", 24, 0.3, 4, 0, Faehigkeit.SCHUBSEN, true, 0.9F, 1.2F),
    TRUEGRUEDE("truegruede", 22, 0.33, 5, 2, Faehigkeit.TRUGBILD, false, 1.2F, 1.3F),
    ENTREISSER("entreisser", 34, 0.25, 6, 6, Faehigkeit.ENTREISSEN, false, 0.9F, 1.5F),
    CHIMAERE("chimaere", 46, 0.3, 9, 4, Faehigkeit.SPRUNG, false, 1.6F, 1.6F),
    GLASGAENGER("glasgaenger", 40, 0.24, 8, 10, Faehigkeit.KRISTALL, false, 0.9F, 2.3F),
    SCHWEBENDER("schwebender", 30, 0.25, 5, 2, Faehigkeit.HEBEN, true, 0.9F, 1.9F),
    ENTLADER("entlader", 30, 0.28, 7, 2, Faehigkeit.BLITZ, false, 0.8F, 2.1F),
    ECHO("echo", 32, 0.27, 7, 4, Faehigkeit.ECHO, false, 0.6F, 1.9F),
    ZONENHIRSCH("zonenhirsch", 40, 0.32, 8, 2, Faehigkeit.SCHEU, false, 1.4F, 2.4F),
    // Orbitalstation
    LEERLAEUFER("leerlaeufer", 30, 0.18, 6, 6, Faehigkeit.SCHWEREL, true, 0.7F, 2.2F),
    WARTUNGSSPINNE("wartungsspinne", 28, 0.3, 5, 8, Faehigkeit.KLETTERN, false, 1.8F, 1.0F),
    WACHKUGEL("wachkugel", 20, 0.3, 3, 10, Faehigkeit.LASER, true, 0.9F, 0.9F),
    VERSUCHSOBJEKT("versuchsobjekt", 32, 0.36, 7, 2, Faehigkeit.RENNER, false, 1.0F, 1.6F),
    KONGLOMERAT("konglomerat", 70, 0.2, 10, 6, Faehigkeit.KEINE, true, 1.6F, 2.5F),
    KRYOSCHLAEFER("kryoschlaefer", 34, 0.26, 6, 4, Faehigkeit.FRIEREN, false, 0.6F, 1.9F),
    LEERENAAL("leerenaal", 36, 0.32, 7, 2, Faehigkeit.KEINE, true, 1.2F, 1.0F),
    KLAMMERER("klammerer", 8, 0.36, 3, 0, Faehigkeit.KLAMMERN, false, 0.5F, 0.4F),
    HOLOGEIST("hologeist", 30, 0.28, 6, 0, Faehigkeit.HOLO, true, 0.7F, 2.3F),
    FORSCHER("forscher", 26, 0.26, 4, 2, Faehigkeit.STIM, false, 0.6F, 1.9F);

    public enum Faehigkeit {
        ZIEHEN, SCHUBSEN, TRUGBILD, ENTREISSEN, SPRUNG, KRISTALL, HEBEN, BLITZ, ECHO, SCHEU,
        SCHWEREL, KLETTERN, LASER, RENNER, FRIEREN, KLAMMERN, HOLO, STIM, KEINE
    }

    public final String id;
    public final double leben;
    public final double tempo;
    public final double schaden;
    public final double panzer;
    public final Faehigkeit faehigkeit;
    public final boolean schwebt;
    public final float breite;
    public final float hoehe;

    DimArt(String id, double leben, double tempo, double schaden, double panzer, Faehigkeit f, boolean schwebt,
           float breite, float hoehe) {
        this.id = id;
        this.leben = leben;
        this.tempo = tempo;
        this.schaden = schaden;
        this.panzer = panzer;
        this.faehigkeit = f;
        this.schwebt = schwebt;
        this.breite = breite;
        this.hoehe = hoehe;
    }

    public boolean nether() {
        return ordinal() < 10;
    }
}
