package de.deadzonemc.mensch;

/** Die Fraktionen der Deadzone. */
public enum Fraktion {
    WACHT, ASCHEKINDER, KARAWANE;

    public String id() {
        return name().toLowerCase();
    }
}
