package de.deadzonemc.client;

import java.util.ArrayList;
import java.util.List;

/** Was der Client über den eigenen Spieler wissen muss (z. B. entdeckte Monster fürs Handbuch). */
public final class ClientDaten {

    private ClientDaten() {
    }

    private static final List<String> ENTDECKT = new ArrayList<>();

    public static void entdecktSetzen(List<String> monster) {
        ENTDECKT.clear();
        ENTDECKT.addAll(monster);
    }

    public static List<String> entdeckt() {
        return ENTDECKT;
    }
}
