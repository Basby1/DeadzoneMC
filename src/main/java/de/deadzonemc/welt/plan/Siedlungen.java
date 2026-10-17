package de.deadzonemc.welt.plan;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * Findet heraus, wo Siedlungen liegen – nur aus dem Startwert der Welt, ohne die Welt anzufassen.
 * Größere Siedlungen haben Vorrang: Ein Dorf, das in eine Stadt hineinragen würde, entfällt.
 */
public final class Siedlungen {

    /** Mindestabstand zwischen den Rändern zweier Siedlungen verschiedener Größe. */
    private static final int PUFFER = 160;

    private Siedlungen() {
    }

    /** Die Siedlung in genau dieser Rasterzelle (oder null). Berücksichtigt keinen Vorrang. */
    public static Siedlung roh(long weltSaat, SiedlungsTyp typ, int zx, int zz) {
        long saat = weltSaat * 341873128712L + zx * 132897987541L + zz * 0x9E3779B97F4A7C15L + typ.ordinal() * 7919L;
        Random r = new Random(saat);
        if (r.nextDouble() >= typ.chance()) {
            return null;
        }
        int radius = typ.radiusMin() + r.nextInt(typ.radiusMax() - typ.radiusMin() + 1);
        int rand = typ.radiusMax() + typ.rand();
        int spielraum = typ.zelle() - 2 * rand;
        int x = zx * typ.zelle() + rand + r.nextInt(Math.max(1, spielraum));
        int z = zz * typ.zelle() + rand + r.nextInt(Math.max(1, spielraum));
        return new Siedlung(typ, zx, zz, x, z, radius, r.nextLong(), Ortsnamen.erfinden(typ, saat));
    }

    /** Die Siedlung in dieser Zelle, sofern sie nicht von einer größeren verdrängt wird. */
    public static Siedlung mitVorrang(long weltSaat, SiedlungsTyp typ, int zx, int zz) {
        Siedlung s = roh(weltSaat, typ, zx, zz);
        if (s == null) {
            return null;
        }
        for (SiedlungsTyp groesser : SiedlungsTyp.values()) {
            if (groesser.ordinal() <= typ.ordinal()) {
                continue;
            }
            int gx = Math.floorDiv(s.x(), groesser.zelle());
            int gz = Math.floorDiv(s.z(), groesser.zelle());
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    Siedlung g = roh(weltSaat, groesser, gx + dx, gz + dz);
                    if (g != null && g.abstand(s.x(), s.z()) < g.radius() + s.radius() + PUFFER) {
                        return null;
                    }
                }
            }
        }
        return s;
    }

    /** Alle Siedlungen, deren Bereich den Punkt (plus Rand) berührt. */
    public static List<Siedlung> beiPunkt(long weltSaat, int x, int z, int extra) {
        List<Siedlung> liste = new ArrayList<>();
        for (SiedlungsTyp typ : SiedlungsTyp.values()) {
            int zx = Math.floorDiv(x, typ.zelle());
            int zz = Math.floorDiv(z, typ.zelle());
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    Siedlung s = mitVorrang(weltSaat, typ, zx + dx, zz + dz);
                    if (s != null && s.beruehrt(x, z, extra)) {
                        liste.add(s);
                    }
                }
            }
        }
        return liste;
    }

    /** Ob der Punkt innerhalb einer (möglichen) Siedlung liegt – für Deko, die dort nicht wachsen soll. */
    public static boolean innerhalb(long weltSaat, int x, int z) {
        for (Siedlung s : beiPunkt(weltSaat, x, z, 8)) {
            if (s.typ().wirdGebaut() && s.abstand(x, z) <= s.radius() + 24) {
                return true;
            }
        }
        return false;
    }

    /** Die nächsten Siedlungen eines Typs rund um einen Punkt, sortiert nach Entfernung. */
    public static List<Siedlung> naechste(long weltSaat, SiedlungsTyp typ, int x, int z, int ringe) {
        List<Siedlung> liste = new ArrayList<>();
        int zx = Math.floorDiv(x, typ.zelle());
        int zz = Math.floorDiv(z, typ.zelle());
        for (int dx = -ringe; dx <= ringe; dx++) {
            for (int dz = -ringe; dz <= ringe; dz++) {
                Siedlung s = mitVorrang(weltSaat, typ, zx + dx, zz + dz);
                if (s != null) {
                    liste.add(s);
                }
            }
        }
        liste.sort(Comparator.comparingDouble(s -> s.abstand(x, z)));
        return liste;
    }
}
