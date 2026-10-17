package de.deadzonemc.welt.plan;

/**
 * Eine geplante Siedlung: Lage, Größe und Name. Rein aus dem Weltstartwert berechnet – ob dort wirklich
 * gebaut werden kann (kein Wasser, kein Steilhang), entscheidet erst der Ortsplan.
 */
public record Siedlung(SiedlungsTyp typ, int zelleX, int zelleZ, int x, int z, int radius, long saat, String name) {

    /** Wie weit Straßen und Bauten höchstens vom Zentrum entfernt liegen. */
    public int reichweite() {
        return radius + typ.rand();
    }

    public boolean beruehrt(int px, int pz, int extra) {
        int r = reichweite() + extra;
        return Math.abs(px - x) <= r && Math.abs(pz - z) <= r;
    }

    public double abstand(int px, int pz) {
        double dx = px - x;
        double dz = pz - z;
        return Math.sqrt(dx * dx + dz * dz);
    }

    public String schluessel() {
        return typ.name() + ":" + zelleX + ":" + zelleZ;
    }
}
