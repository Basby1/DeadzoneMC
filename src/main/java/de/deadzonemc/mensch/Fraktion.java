package de.deadzonemc.mensch;

/** Die Fraktionen der Deadzone. */
public enum Fraktion {
    WACHT, ASCHEKINDER, KARAWANE, ZUFLUCHT;

    public String id() {
        return name().toLowerCase();
    }
}
