package de.deadzonemc.welt.plan;

/** Liefert die Geländehöhe (oberster fester Block oder Meeresgrund) an einer Stelle. */
@FunctionalInterface
public interface Hoehen {
    int boden(int x, int z);
}
