package de.deadzonemc.welt.plan;

import java.util.ArrayList;
import java.util.List;

/** Der fertige Plan einer Siedlung: Straßen, Grundstücke und kleine Objekte. */
public final class Ortsplan {

    public final Siedlung siedlung;
    public final List<Strasse> strassen = new ArrayList<>();
    public final List<Grundstueck> grundstuecke = new ArrayList<>();
    public final List<Objekt> objekte = new ArrayList<>();
    /** Ob die Siedlung gebaut werden kann (nicht im Meer, nicht am Steilhang). */
    public boolean gueltig = true;

    public Ortsplan(Siedlung siedlung) {
        this.siedlung = siedlung;
    }

    /** Ob an dieser Stelle etwas aus dem Plan liegt (Straße, Grundstück, Objekt). */
    public boolean belegt(int x, int z, int rand) {
        for (Strasse s : strassen) {
            int[] f = s.flaeche();
            if (x >= f[0] - rand && x <= f[2] + rand && z >= f[1] - rand && z <= f[3] + rand) {
                return true;
            }
        }
        for (Grundstueck g : grundstuecke) {
            if (x >= g.minX - rand && x <= g.maxX + rand && z >= g.minZ - rand && z <= g.maxZ + rand) {
                return true;
            }
        }
        for (Objekt o : objekte) {
            int[] f = o.flaeche();
            if (x >= f[0] - rand && x <= f[2] + rand && z >= f[1] - rand && z <= f[3] + rand) {
                return true;
            }
        }
        return false;
    }
}
