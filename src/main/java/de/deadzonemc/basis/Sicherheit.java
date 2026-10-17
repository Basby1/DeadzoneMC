package de.deadzonemc.basis;

/** Wie sicher die Basis ist (Weltweite Einstellung, mit /deadzone basis stufe änderbar). */
public enum Sicherheit {
    /** Im Basisbereich spawnt nichts, und nichts kommt hinein. */
    SICHER,
    /** Normalerweise sicher; in Hordennächten und bei dichtem Nebel nachts greifen sie an. */
    BELAGERUNG,
    /** Angriffe jederzeit; nur direkt am Generator spawnt nichts. */
    GNADENLOS;

    public String id() {
        return name().toLowerCase();
    }

    public static Sicherheit ausId(String id) {
        for (Sicherheit s : values()) {
            if (s.id().equals(id)) {
                return s;
            }
        }
        return null;
    }
}
