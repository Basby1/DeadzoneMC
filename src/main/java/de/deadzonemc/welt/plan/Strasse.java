package de.deadzonemc.welt.plan;

/**
 * Eine gerade Straße entlang der X- oder Z-Achse. "Längs" ist die Fahrtrichtung, "quer" die
 * Mittellinie. Innerhalb des Ortes (ortVon..ortBis) hat sie Gehwege, außerhalb ist sie Landstraße.
 */
public final class Strasse {

    public enum Typ {
        //             halbe Fahrbahn, Gehweg je Seite
        LANDSTRASSE(2, 0),
        DORFSTRASSE(2, 2),
        STADTSTRASSE(2, 2),
        HAUPTSTRASSE(3, 3);

        public final int halb;
        public final int gehweg;

        Typ(int halb, int gehweg) {
            this.halb = halb;
            this.gehweg = gehweg;
        }
    }

    public final boolean entlangX;
    public final int quer;
    public final int von;
    public final int bis;
    public final Typ typ;
    public final int ortVon;
    public final int ortBis;
    public final long saat;
    public final String name;
    /** Höhe der Fahrbahn je Block (Index 0 = von). Wird beim Einpassen ins Gelände gesetzt. */
    public int[] hoehe;

    public Strasse(boolean entlangX, int quer, int von, int bis, Typ typ, int ortVon, int ortBis, long saat, String name) {
        this.entlangX = entlangX;
        this.quer = quer;
        this.von = von;
        this.bis = bis;
        this.typ = typ;
        this.ortVon = ortVon;
        this.ortBis = ortBis;
        this.saat = saat;
        this.name = name;
    }

    public boolean imOrt(int laengs) {
        return laengs >= ortVon && laengs <= ortBis;
    }

    /** Halbe Gesamtbreite an dieser Stelle (Fahrbahn plus Gehweg, falls im Ort). */
    public int halbeBreite(int laengs) {
        return typ.halb + (imOrt(laengs) ? typ.gehweg : 0);
    }

    public int maxHalbeBreite() {
        return typ.halb + typ.gehweg;
    }

    public int hoeheBei(int laengs) {
        int i = Math.max(0, Math.min(hoehe.length - 1, laengs - von));
        return hoehe[i];
    }

    public int x(int laengs, int querVersatz) {
        return entlangX ? laengs : quer + querVersatz;
    }

    public int z(int laengs, int querVersatz) {
        return entlangX ? quer + querVersatz : laengs;
    }

    /** Fläche der ganzen Straße mit Gehwegen: {minX, minZ, maxX, maxZ}. */
    public int[] flaeche() {
        int h = maxHalbeBreite();
        return entlangX ? new int[]{von, quer - h, bis, quer + h} : new int[]{quer - h, von, quer + h, bis};
    }

    /** Nur die Fahrbahn: {minX, minZ, maxX, maxZ}. */
    public int[] fahrbahn() {
        int h = typ.halb;
        return entlangX ? new int[]{von, quer - h, bis, quer + h} : new int[]{quer - h, von, quer + h, bis};
    }

    public static boolean ueberlappt(int[] a, int[] b, int rand) {
        return a[0] - rand <= b[2] && a[2] + rand >= b[0] && a[1] - rand <= b[3] && a[3] + rand >= b[1];
    }

    public static boolean enthaelt(int[] r, int x, int z) {
        return x >= r[0] && x <= r[2] && z >= r[1] && z <= r[3];
    }
}
