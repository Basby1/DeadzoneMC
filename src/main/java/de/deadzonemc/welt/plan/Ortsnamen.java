package de.deadzonemc.welt.plan;

import java.util.Random;

/** Erfindet deutsch klingende Ortsnamen – immer derselbe Name für denselben Ort. */
public final class Ortsnamen {

    private Ortsnamen() {
    }

    private static final String[] VORNE = {
            "Alten", "Neu", "Ober", "Nieder", "Hohen", "Kirch", "Grün", "Wald", "Stein", "Mühl", "Rot", "Schwarz",
            "Linden", "Birken", "Eichen", "Buchen", "Erlen", "Kalt", "Hagen", "Wester", "Oster", "Sonnen", "Wolfs",
            "Raben", "Krähen", "Moor", "Heide", "Tann", "Fichten", "Brunn", "Lauter", "Rosen", "Marien", "Kloster"
    };
    private static final String[] HINTEN = {
            "rode", "dorf", "hausen", "feld", "bach", "berg", "heim", "ingen", "stedt", "au", "brück", "hagen",
            "wald", "beck", "furt", "kirchen", "holz", "born", "weiler", "winkel", "grund", "mühle"
    };
    private static final String[] STADT_VORSATZ = {"", "", "", "", "Bad ", "Neu-", "Sankt ", "Alt-"};
    private static final String[] STADT_ZUSATZ = {"", "", "", "", " am See", " an der Lauter", " im Tal", " am Wald"};

    public static String erfinden(SiedlungsTyp typ, long saat) {
        Random r = new Random(saat ^ 0x5DEECE66DL);
        String kern = VORNE[r.nextInt(VORNE.length)] + HINTEN[r.nextInt(HINTEN.length)];
        return switch (typ) {
            case DORF -> kern;
            case KLEINSTADT -> STADT_VORSATZ[r.nextInt(STADT_VORSATZ.length)] + kern
                    + STADT_ZUSATZ[r.nextInt(STADT_ZUSATZ.length)];
            case GROSSSTADT -> kern;
        };
    }

    private static final String[] STRASSE_VORNE = {
            "Linden", "Kirch", "Schul", "Bahnhof", "Garten", "Wiesen", "Mühlen", "Feld", "Berg", "Wald", "Birken",
            "Rosen", "Ahorn", "Brunnen", "Markt", "Post", "Sonnen", "Eichen", "Tannen", "Bach", "Friedhof", "Dorf"
    };
    private static final String[] STRASSE_HINTEN = {"straße", "weg", "gasse", "allee", "ring", "pfad"};

    public static String strasse(long saat) {
        Random r = new Random(saat ^ 0x2545F4914F6CDD1DL);
        return STRASSE_VORNE[r.nextInt(STRASSE_VORNE.length)] + STRASSE_HINTEN[r.nextInt(STRASSE_HINTEN.length)];
    }
}
