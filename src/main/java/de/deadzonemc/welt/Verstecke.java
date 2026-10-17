package de.deadzonemc.welt;

import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Wo Verstecke (vergrabene Vorräte anderer Überlebender) liegen – nur aus dem Weltstartwert berechnet.
 * So kann eine Schatzkarte auf ein Versteck zeigen, dessen Chunk noch gar nicht erzeugt wurde.
 */
public final class Verstecke {

    public static final int ZELLE = 448;
    private static final int RAND = 40;

    private Verstecke() {
    }

    /** Das Versteck in dieser Rasterzelle (x/z, y = 0) oder null. */
    public static BlockPos inZelle(long weltSaat, int zx, int zz) {
        Random r = new Random(weltSaat * 0x5DEECE66DL + zx * 734287L + zz * 912367L + 0xBEEFL);
        if (r.nextDouble() > 0.8) {
            return null;
        }
        int x = zx * ZELLE + RAND + r.nextInt(ZELLE - 2 * RAND);
        int z = zz * ZELLE + RAND + r.nextInt(ZELLE - 2 * RAND);
        return new BlockPos(x, 0, z);
    }

    /** Alle Verstecke im Umkreis (in Zellen) um einen Punkt, nach Entfernung sortiert. */
    public static List<BlockPos> umkreis(long weltSaat, int x, int z, int zellen) {
        int cx = Math.floorDiv(x, ZELLE);
        int cz = Math.floorDiv(z, ZELLE);
        List<BlockPos> liste = new ArrayList<>();
        for (int dx = -zellen; dx <= zellen; dx++) {
            for (int dz = -zellen; dz <= zellen; dz++) {
                BlockPos p = inZelle(weltSaat, cx + dx, cz + dz);
                if (p != null) {
                    liste.add(p);
                }
            }
        }
        liste.sort((a, b) -> Double.compare(a.distToCenterSqr(x, 0, z), b.distToCenterSqr(x, 0, z)));
        return liste;
    }
}
