package de.deadzonemc.welt.plan;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Merkt sich berechnete Pläne, damit nicht jeder Chunk eines Dorfes alles neu ausrechnet.
 * Der Schlüssel enthält den Weltstartwert, weil im Einzelspieler nacheinander verschiedene Welten
 * laufen können.
 */
public final class PlanSpeicher {

    private static final int GROESSE = 256;

    private static final Map<String, Ortsplan> GEOMETRIE = lru();
    private static final Map<String, Ortsplan> FERTIG = lru();

    private static final int[][] AUSWEICHEN = {
            {70, 0}, {-70, 0}, {0, 70}, {0, -70}, {60, 60}, {-60, 60}, {60, -60}, {-60, -60}
    };

    private PlanSpeicher() {
    }

    /** Der fertige Plan, falls er schon berechnet wurde (sonst null). */
    public static Ortsplan schonFertig(long weltSaat, Siedlung s) {
        synchronized (FERTIG) {
            return FERTIG.get(schluessel(weltSaat, s));
        }
    }

    private static Map<String, Ortsplan> lru() {
        return new LinkedHashMap<>(64, 0.75F, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, Ortsplan> eldest) {
                return size() > GROESSE;
            }
        };
    }

    private static String schluessel(long weltSaat, Siedlung s) {
        return weltSaat + "/" + s.schluessel();
    }

    /** Plan ohne Höhen – reicht, um zu wissen, wo Straßen und Grundstücke liegen. */
    public static Ortsplan geometrie(long weltSaat, Siedlung s) {
        String k = schluessel(weltSaat, s);
        synchronized (GEOMETRIE) {
            Ortsplan p = GEOMETRIE.get(k);
            if (p != null) {
                return p;
            }
        }
        Ortsplan neu = Dorfplaner.geometrie(s);
        synchronized (GEOMETRIE) {
            GEOMETRIE.put(k, neu);
        }
        return neu;
    }

    /** Fertiger, ins Gelände eingepasster Plan. */
    public static Ortsplan fertig(long weltSaat, Siedlung s, Hoehen h, int meer) {
        String k = schluessel(weltSaat, s);
        synchronized (FERTIG) {
            Ortsplan p = FERTIG.get(k);
            if (p != null) {
                return p;
            }
        }
        Ortsplan neu = Dorfplaner.einpassen(s, h, meer);
        // Liegt der Platz im Wasser, sucht sich das Dorf einen trockeneren Platz in der Nähe.
        for (int[] v : AUSWEICHEN) {
            if (neu.gueltig) {
                break;
            }
            neu = Dorfplaner.einpassen(s.verschoben(v[0], v[1]), h, meer);
        }
        synchronized (FERTIG) {
            Ortsplan vorhanden = FERTIG.get(k);
            if (vorhanden != null) {
                return vorhanden;
            }
            FERTIG.put(k, neu);
        }
        return neu;
    }

    /** Ob hier eine gebaute Siedlung etwas hingestellt hat oder hinstellen wird. */
    public static boolean belegt(long weltSaat, int x, int z, int rand) {
        for (Siedlung s : Siedlungen.beiPunkt(weltSaat, x, z, rand)) {
            if (!s.typ().wirdGebaut()) {
                continue;
            }
            Ortsplan fertig = schonFertig(weltSaat, s);
            Ortsplan plan = fertig != null ? fertig : geometrie(weltSaat, s);
            if ((fertig == null || fertig.gueltig) && plan.belegt(x, z, rand)) {
                return true;
            }
        }
        return false;
    }
}
