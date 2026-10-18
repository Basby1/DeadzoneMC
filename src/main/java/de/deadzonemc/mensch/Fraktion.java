package de.deadzonemc.mensch;

/** Die Fraktionen der Deadzone. */
public enum Fraktion {
    WACHT, ASCHEKINDER, KARAWANE, ZUFLUCHT,
    // Neue Fraktionen (je eigene Questreihe, Ansehen und Vorteil)
    BANDITEN, TOTENFLUESTERER, WALDKINDER, SANDWANDERER, TIEFENVOLK, HIMMELSHUETER;

    public String id() {
        return name().toLowerCase();
    }
}
