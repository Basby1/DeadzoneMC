package de.deadzonemc.welt.gebaeude;

import de.deadzonemc.welt.bau.Baukasten;

/** Eine Gebäudeart, die ein ganzes Grundstück gestaltet. */
public interface Gebaeude {

    /** Zusatzinfos vom Plan: Straßenname und Hausnummer. */
    record Info(String strasse, int hausnummer) {
    }

    /** Höchste Höhe über dem Boden, die das Gebäude erreichen kann. */
    int hoehe();

    void bauen(Baukasten b, Info info);
}
