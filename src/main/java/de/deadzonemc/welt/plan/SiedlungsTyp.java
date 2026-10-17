package de.deadzonemc.welt.plan;

/**
 * Die drei Größen von Siedlungen. Jede Größe hat ihr eigenes Raster: In jeder Zelle liegt höchstens
 * eine Siedlung, mit genug Abstand zum Zellrand, damit sich Nachbarn nie überschneiden.
 */
public enum SiedlungsTyp {
    //            Zelle  Radius min/max  Chance  Rand (Abstand zur Zellgrenze zusätzlich zum Radius)
    DORF(          640,      50,   78,   0.75,  100),
    KLEINSTADT(   1408,     150,  200,   0.85,  160),
    GROSSSTADT(   3200,     400,  600,   0.80,  200);

    private final int zelle;
    private final int radiusMin;
    private final int radiusMax;
    private final double chance;
    private final int rand;

    SiedlungsTyp(int zelle, int radiusMin, int radiusMax, double chance, int rand) {
        this.zelle = zelle;
        this.radiusMin = radiusMin;
        this.radiusMax = radiusMax;
        this.chance = chance;
        this.rand = rand;
    }

    public int zelle() {
        return zelle;
    }

    public int radiusMin() {
        return radiusMin;
    }

    public int radiusMax() {
        return radiusMax;
    }

    public double chance() {
        return chance;
    }

    /** So weit dürfen Ausfallstraßen über den Radius hinausreichen. */
    public int rand() {
        return rand;
    }

    /** Ob diese Größe schon gebaut wird (Kleinstädte und Großstädte folgen in den nächsten Schritten). */
    public boolean wirdGebaut() {
        return this == DORF;
    }
}
