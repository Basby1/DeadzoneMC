package de.deadzonemc.welt.plan;

import net.minecraft.core.Direction;

/**
 * Ein Grundstück an einer Straße. {@code vorne} zeigt vom Grundstück zur Straße. Das Gebäude
 * (Typ) entscheidet selbst, wie es Haus, Garten und Zaun auf der Fläche verteilt.
 */
public final class Grundstueck {

    public final int minX;
    public final int minZ;
    public final int maxX;
    public final int maxZ;
    public final Direction vorne;
    public final String typ;
    public final long saat;
    public final String strasse;
    public final int hausnummer;
    /** Höhe des Bodens (Oberkante Gelände), wird beim Einpassen gesetzt. */
    public int y;
    /** An welcher Straße des Plans das Grundstück liegt, und wo (Längskoordinate der Mitte). */
    public int strassenIndex = -1;
    public int laengsMitte;

    public Grundstueck(int minX, int minZ, int maxX, int maxZ, Direction vorne, String typ, long saat,
                       String strasse, int hausnummer) {
        this.minX = minX;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxZ = maxZ;
        this.vorne = vorne;
        this.typ = typ;
        this.saat = saat;
        this.strasse = strasse;
        this.hausnummer = hausnummer;
    }

    public int[] flaeche() {
        return new int[]{minX, minZ, maxX, maxZ};
    }

    public int mitteX() {
        return (minX + maxX) / 2;
    }

    public int mitteZ() {
        return (minZ + maxZ) / 2;
    }

    /** Breite entlang der Straße. */
    public int breite() {
        return vorne.getAxis() == Direction.Axis.Z ? maxX - minX + 1 : maxZ - minZ + 1;
    }

    /** Tiefe von der Straße weg. */
    public int tiefe() {
        return vorne.getAxis() == Direction.Axis.Z ? maxZ - minZ + 1 : maxX - minX + 1;
    }
}
