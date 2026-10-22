package de.deadzonemc.client.charakter;

import net.minecraft.util.RandomSource;

/**
 * Alle Teile des Charakter-Editors. Reihenfolge und Dateinamen passen zu werkzeuge/charakter.py.
 * Ein Aussehen ist ein Code aus Zahlen mit Punkten: "1.haut.augen.gesicht.haare.haarfarbe.bart.oberteil.oberfarbe.
 * hose.hosenfarbe.schuhe.schuhfarbe.kopf.kopffarbe.extra.extrafarbe.schmal".
 */
public final class CharakterTeile {

    private CharakterTeile() {
    }

    public static final int VERSION = 1;

    // Indizes im Wahl-Feld
    public static final int HAUT = 0, AUGEN = 1, GESICHT = 2, HAARE = 3, HAARFARBE = 4, BART = 5, OBERTEIL = 6,
            OBERFARBE = 7, HOSE = 8, HOSENFARBE = 9, SCHUHE = 10, SCHUHFARBE = 11, KOPF = 12, KOPFFARBE = 13,
            EXTRA = 14, EXTRAFARBE = 15, SCHMAL = 16, ANZAHL = 17;

    public static final int[] HAUTFARBEN = {0xFFE2C8, 0xF0C4A0, 0xDEAA7E, 0xC48A5E, 0xA06A46, 0x7A5034, 0x5C3C28, 0x422C1E};
    public static final int[] AUGENFARBEN = {0x704828, 0x3E2818, 0x487AC0, 0x548648, 0x848E94, 0x927034};
    public static final int[] HAARFARBEN = {0x26201E, 0x482E1E, 0x764E2E, 0xDCBA74, 0xC0622E, 0x9C9C9A, 0xE8E6DE, 0x405EB0, 0xC46C96};
    public static final int[] STOFFFARBEN = {0x2E2E32, 0x54565A, 0x92928E, 0xD8D2C2, 0x626C3E, 0xAC986A, 0x724E2E, 0x982A24,
            0x2A3862, 0x4E6894, 0xDE7020, 0x366840};

    public static final String[] GESICHTER = {"ruhig", "muede", "narbe", "grimmig", "veraengstigt", "pflaster", "verschmiert"};
    public static final String[] GESICHTER_N = {"Ruhig", "Müde", "Narbe", "Grimmig", "Verängstigt", "Pflaster", "Verschmiert"};
    public static final String[] HAARE_ = {"glatze", "kurz", "undercut", "lang", "pferdeschwanz", "locken", "irokese", "dutt", "zoepfe", "stoppeln"};
    public static final String[] HAARE_N = {"Glatze", "Kurz", "Undercut", "Lang", "Pferdeschwanz", "Locken", "Irokese", "Dutt", "Zöpfe", "Stoppeln"};
    public static final String[] BAERTE = {"kein", "stoppeln", "vollbart", "schnurrbart", "kinnbart"};
    public static final String[] BAERTE_N = {"Kein Bart", "Stoppeln", "Vollbart", "Schnurrbart", "Kinnbart"};
    public static final String[] OBERTEILE = {"tshirt", "kapuzenpulli", "lederjacke", "militaerjacke", "flanellhemd", "sanitaeter",
            "weste", "regenjacke", "tanktop", "strickpulli"};
    public static final String[] OBERTEILE_N = {"T-Shirt", "Kapuzenpulli", "Lederjacke", "Militärjacke", "Flanellhemd",
            "Sanitäterjacke", "Weste", "Regenjacke", "Tanktop", "Strickpulli"};
    public static final String[] HOSEN = {"jeans", "cargo", "jogger", "militaer", "shorts", "arbeitshose"};
    public static final String[] HOSEN_N = {"Jeans", "Cargohose", "Jogginghose", "Tarnhose", "Shorts", "Arbeitshose"};
    public static final String[] SCHUHE_ = {"stiefel", "turnschuhe", "arbeitsschuhe", "gummistiefel"};
    public static final String[] SCHUHE_N = {"Stiefel", "Turnschuhe", "Arbeitsschuhe", "Gummistiefel"};
    public static final String[] KOEPFE = {"keine", "gasmaske", "atemschutz", "bandana", "muetze", "helm", "schutzbrille",
            "kopfverband", "kappe"};
    public static final String[] KOEPFE_N = {"Nichts", "Gasmaske", "Atemschutz", "Bandana", "Mütze", "Helm", "Schutzbrille",
            "Kopfverband", "Kappe"};
    public static final String[] EXTRAS = {"keine", "rucksack", "schutzweste", "patronengurt", "armbinde", "verbaende", "holster"};
    public static final String[] EXTRAS_N = {"Nichts", "Rucksack", "Schutzweste", "Patronengurt", "Armbinde", "Verbände", "Holster"};
    public static final String[] ARME_N = {"Breit", "Schmal"};

    /** Wie viele Möglichkeiten jede Stelle hat. */
    public static int anzahl(int i) {
        return switch (i) {
            case HAUT -> HAUTFARBEN.length;
            case AUGEN -> AUGENFARBEN.length;
            case GESICHT -> GESICHTER.length;
            case HAARE -> HAARE_.length;
            case HAARFARBE -> HAARFARBEN.length;
            case BART -> BAERTE.length;
            case OBERTEIL -> OBERTEILE.length;
            case HOSE -> HOSEN.length;
            case SCHUHE -> SCHUHE_.length;
            case KOPF -> KOEPFE.length;
            case EXTRA -> EXTRAS.length;
            case SCHMAL -> 2;
            default -> STOFFFARBEN.length;
        };
    }

    public static int[] standard() {
        return new int[]{1, 0, 0, 1, 1, 0, 3, 4, 0, 9, 0, 0, 0, 0, 0, 4, 0};
    }

    public static String kodieren(int[] w) {
        StringBuilder b = new StringBuilder().append(VERSION);
        for (int v : w) {
            b.append('.').append(v);
        }
        return b.toString();
    }

    /** null bei "eigen" oder ungültig. */
    public static int[] lesen(String code) {
        if (code == null || code.isEmpty() || code.equals("eigen")) {
            return null;
        }
        String[] t = code.split("\\.");
        if (t.length < 2) {
            return null;
        }
        int[] w = standard();
        try {
            for (int i = 0; i < ANZAHL && i + 1 < t.length; i++) {
                w[i] = Math.floorMod(Integer.parseInt(t[i + 1]), anzahl(i));
            }
        } catch (NumberFormatException e) {
            return null;
        }
        return w;
    }

    /** Zufälliger, halbwegs stimmiger Überlebender. */
    public static int[] zufall(RandomSource r) {
        int[] w = new int[ANZAHL];
        for (int i = 0; i < ANZAHL; i++) {
            w[i] = r.nextInt(anzahl(i));
        }
        // Ausrüstung seltener, damit nicht jeder wie ein Weihnachtsbaum aussieht
        if (r.nextFloat() < 0.5F) {
            w[KOPF] = 0;
        }
        if (r.nextFloat() < 0.45F) {
            w[EXTRA] = 0;
        }
        if (r.nextFloat() < 0.5F) {
            w[BART] = 0;
        }
        // grelle Haarfarben selten
        if (w[HAARFARBE] >= 7 && r.nextFloat() < 0.7F) {
            w[HAARFARBE] = r.nextInt(7);
        }
        return w;
    }
}
