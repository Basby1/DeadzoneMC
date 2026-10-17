package de.deadzonemc.welt.plan;

/**
 * Etwas Kleines, das an einer bestimmten Stelle steht: ein Auto, ein Ortsschild ...
 * (x, z) ist die Ecke mit den kleinsten Koordinaten; {@code entlangX} gibt die Ausrichtung an.
 */
public final class Objekt {

    public final String typ;
    public final int x;
    public final int z;
    public final boolean entlangX;
    /** Bei Schildern: in welche Richtung die Schrift zeigt (0 = +X/+Z, 1 = -X/-Z). */
    public final int seite;
    public final long saat;
    public final String text;
    public int y;

    public Objekt(String typ, int x, int z, boolean entlangX, int seite, long saat, String text) {
        this.typ = typ;
        this.x = x;
        this.z = z;
        this.entlangX = entlangX;
        this.seite = seite;
        this.saat = saat;
        this.text = text;
    }

    /** Grobe Fläche, die das Objekt belegt: {minX, minZ, maxX, maxZ}. */
    public int[] flaeche() {
        if (typ.equals("auto") || typ.equals("auto_brand")) {
            return entlangX ? new int[]{x, z, x + 4, z + 2} : new int[]{x, z, x + 2, z + 4};
        }
        return new int[]{x - 1, z - 1, x + 1, z + 1};
    }
}
