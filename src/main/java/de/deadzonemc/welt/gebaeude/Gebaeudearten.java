package de.deadzonemc.welt.gebaeude;

import java.util.HashMap;
import java.util.Map;

/** Alle Gebäudearten nach ihrem Namen im Ortsplan. */
public final class Gebaeudearten {

    private static final Map<String, Gebaeude> ARTEN = new HashMap<>();
    private static final Gebaeude BRACHE = new Kleinbauten.Brache();

    static {
        ARTEN.put("einfamilienhaus", new Einfamilienhaus());
        ARTEN.put("bungalow", new Bungalow());
        ARTEN.put("bauernhof", new Bauernhof());
        ARTEN.put("kirche", new Kirche());
        ARTEN.put("dorfladen", new Dorfladen());
        ARTEN.put("gasthaus", new Gasthaus());
        ARTEN.put("feuerwehr", new Feuerwehr());
        ARTEN.put("werkstatt", new Werkstatt());
        ARTEN.put("ruine", new Kleinbauten.Ruine());
        ARTEN.put("garten", new Kleinbauten.Kleingarten());
        ARTEN.put("bushaltestelle", new Kleinbauten.Bushaltestelle());
        ARTEN.put("tankstelle", new Kleinbauten.Tankstelle());
    }

    private Gebaeudearten() {
    }

    public static Gebaeude holen(String typ) {
        return ARTEN.getOrDefault(typ, BRACHE);
    }
}
