package de.deadzonemc.quest;

import java.util.List;

/**
 * Eine Auftragsvorlage der Zufluchten.
 *
 * @param ziel BRINGEN: Item-ID oder #Tag · JAGEN: Wesen-ID oder #Tag · ERKUNDEN: "stadt" oder "versteck"
 * @param kategorie Skillbaum, der Erfahrung bekommt (kampf, schusswaffen, ueberleben, handwerk, bewegung)
 */
public record Auftrag(String id, Art art, String titel, String text, String ziel, int anzahl, int patronen,
                      String kategorie, int xp, int ansehen) {

    public enum Art { BRINGEN, JAGEN, ERKUNDEN }

    public static final List<Auftrag> ALLE = List.of(
            new Auftrag("konserven", Art.BRINGEN, "Vorratskammer",
                    "Die Vorräte reichen keine Woche mehr. Bring uns Konserven.",
                    "deadzonemc:konservendose", 8, 12, "ueberleben", 80, 5),
            new Auftrag("stoff", Art.BRINGEN, "Verbandszeug",
                    "Unser Arzt reißt schon die Vorhänge in Streifen. Wir brauchen Stoffreste.",
                    "deadzonemc:stoffreste", 8, 8, "handwerk", 50, 4),
            new Auftrag("elektronik", Art.BRINGEN, "Ersatzteile",
                    "Der Generator hustet. Ohne Elektronik sitzen wir bald im Dunkeln.",
                    "deadzonemc:elektronik", 4, 15, "handwerk", 80, 5),
            new Auftrag("schrott", Art.BRINGEN, "Die Mauer",
                    "Letzte Nacht haben sie an der Ostmauer gerüttelt. Wir brauchen Schrott zum Verstärken.",
                    "deadzonemc:schrott", 16, 10, "handwerk", 60, 5),
            new Auftrag("batterien", Art.BRINGEN, "Funkstille",
                    "Das Funkgerät braucht Batterien. Vielleicht hört uns da draußen noch jemand.",
                    "deadzonemc:batterie", 3, 12, "handwerk", 60, 5),
            new Auftrag("holz", Art.BRINGEN, "Brennholz",
                    "Die Nächte werden kalt. Bring Holzstämme, egal welche.",
                    "#minecraft:logs", 32, 8, "handwerk", 50, 3),
            new Auftrag("antibiotika", Art.BRINGEN, "Fieber",
                    "Ein Kind hat Fieber, die Wunde ist entzündet. Wir brauchen Antibiotika – schnell.",
                    "deadzonemc:antibiotika", 2, 22, "ueberleben", 120, 10),
            new Auftrag("leder", Art.BRINGEN, "Stiefel",
                    "Unsere Wachen laufen auf Sohlen aus Pappe. Bring Leder.",
                    "minecraft:leather", 8, 8, "ueberleben", 50, 3),
            new Auftrag("munition", Art.BRINGEN, "Munition für die Wache",
                    "Jede Kugel zählt. Bring Munition, ganz gleich welches Kaliber.",
                    "#deadzonemc:munition", 30, 18, "schusswaffen", 100, 8),
            new Auftrag("artefakt", Art.BRINGEN, "Forschungsauftrag",
                    "Ein Wissenschaftler aus Station Null zahlt jeden Preis für Artefakte. Hol eins aus einem Anomalienfeld.",
                    "#deadzonemc:artefakte", 1, 35, "ueberleben", 250, 10),
            new Auftrag("bolzen", Art.BRINGEN, "Altmetall",
                    "Wir schmieden Bolzen für die Anomalienfelder. Bring Eisenbarren.",
                    "minecraft:iron_ingot", 8, 10, "handwerk", 60, 4),
            new Auftrag("saeuberung", Art.JAGEN, "Säuberung",
                    "Vor dem Tor sammeln sie sich. Erledige Wanderer, bevor es zu viele werden.",
                    "deadzonemc:wanderer", 15, 15, "kampf", 150, 6),
            new Auftrag("schreier", Art.JAGEN, "Ruhe",
                    "Diese Schreier locken jede Nacht ganze Horden zu uns. Bring sie zum Schweigen.",
                    "deadzonemc:schreier", 3, 15, "kampf", 120, 6),
            new Auftrag("kriecher", Art.JAGEN, "Wandkletterer",
                    "Etwas klettert nachts über die Dächer. Kriecher. Mach Jagd auf sie.",
                    "deadzonemc:kriecher", 4, 16, "kampf", 130, 6),
            new Auftrag("brut", Art.JAGEN, "Sporen",
                    "Die Brut verseucht unsere Felder. Vernichte sie – aus sicherer Entfernung.",
                    "deadzonemc:brut", 3, 18, "schusswaffen", 140, 6),
            new Auftrag("koloss", Art.JAGEN, "Der Koloss",
                    "Ein Juggernaut zieht durch die Stadt. Wer ihn erledigt, wird hier nie wieder hungern.",
                    "deadzonemc:juggernaut", 1, 45, "kampf", 400, 15),
            new Auftrag("infizierte", Art.JAGEN, "Ausdünnen",
                    "Es werden jede Woche mehr. Dünn sie aus – egal welche.",
                    "#deadzonemc:monster", 25, 20, "kampf", 200, 8),
            new Auftrag("kundschafter", Art.ERKUNDEN, "Kundschafter",
                    "Wir wissen nicht, was in der Stadt dort draußen los ist. Geh hin und sieh nach.",
                    "stadt", 1, 15, "bewegung", 200, 6),
            new Auftrag("versteck", Art.ERKUNDEN, "Verschollene Vorräte",
                    "Ein Späher hat ein Versteck gefunden und kam nie zurück. Finde es.",
                    "versteck", 1, 12, "bewegung", 150, 5)
    );

    /** Kopfgelder auf Bosse – jeden Tag hängt bei jedem Auftraggeber eines aus, mit Hinweis, wo und wann er auftaucht. */
    public static final List<Auftrag> KOPFGELDER = List.of(
            new Auftrag("kopfgeld_schlaechter", Art.JAGEN, "Kopfgeld: Der Schlächter",
                    "Bei Neumond zieht er durch die Städte und hängt seine Opfer an Haken. Wer ihn erledigt, bekommt alles, was wir haben.",
                    "deadzonemc:schlaechter", 1, 80, "kampf", 700, 20),
            new Auftrag("kopfgeld_nebelmutter", Art.JAGEN, "Kopfgeld: Die Nebelmutter",
                    "In nebligen Nächten, in verseuchten und toten Wäldern, gebiert sie ihre Kinder. Beende es.",
                    "deadzonemc:nebelmutter", 1, 80, "kampf", 700, 20),
            new Auftrag("kopfgeld_pilzvater", Art.JAGEN, "Kopfgeld: Der Pilzvater",
                    "Wenn es in den Sumpfwäldern regnet, wächst er aus dem Boden. Seine Sporen verderben unsere Ernte.",
                    "deadzonemc:pilzvater", 1, 70, "kampf", 600, 18),
            new Auftrag("kopfgeld_erfrorene", Art.JAGEN, "Kopfgeld: Der Erfrorene",
                    "Im Schneesturm, oben im Eis, wandert etwas, das nicht mehr friert. Zwei unserer Jäger kamen nicht zurück.",
                    "deadzonemc:erfrorene", 1, 70, "kampf", 600, 18),
            new Auftrag("kopfgeld_schlund", Art.JAGEN, "Kopfgeld: Der Schlund",
                    "In der Wüste hört er jeden, der rennt. Dann bricht er aus dem Sand. Lock ihn heraus und töte ihn.",
                    "deadzonemc:schlund", 1, 75, "schusswaffen", 650, 18),
            new Auftrag("kopfgeld_brutkoenigin", Art.JAGEN, "Kopfgeld: Die Brutkönigin",
                    "Tief unter der Erde, unter Meereshöhe, legt sie ihre Eier. Steig hinab und bring es zu Ende.",
                    "deadzonemc:brutkoenigin", 1, 75, "kampf", 650, 18),
            new Auftrag("kopfgeld_schwarmmutter", Art.JAGEN, "Kopfgeld: Die Schwarmmutter",
                    "In den U-Bahn-Tunneln unter der Stadt nistet sie. Nimm genug Fackeln mit – im Licht ist sie blind.",
                    "deadzonemc:schwarmmutter", 1, 80, "kampf", 700, 18),
            new Auftrag("kopfgeld_pilot", Art.JAGEN, "Kopfgeld: Der Pilot",
                    "Bei einem abgestürzten Transporthubschrauber steht er noch immer Wache. Sein Bordgeschütz trifft auf Abstand.",
                    "deadzonemc:pilot", 1, 80, "schusswaffen", 700, 18),
            new Auftrag("kopfgeld_strahlende", Art.JAGEN, "Kopfgeld: Der Strahlende",
                    "Wer sich in der Strahlenzone schwer verstrahlen lässt, dem erscheint er. Nimm Anti-Rad mit.",
                    "deadzonemc:strahlende", 1, 85, "ueberleben", 750, 20),
            new Auftrag("kopfgeld_hirte", Art.JAGEN, "Kopfgeld: Der Hirte",
                    "Er führt die großen Herden übers Land. Fällt er, zerstreut sich die Herde. Halte nach ihm Ausschau.",
                    "deadzonemc:hirte", 1, 70, "kampf", 600, 18)
    );

    public static Auftrag holen(String id) {
        for (Auftrag a : KOPFGELDER) {
            if (a.id.equals(id)) {
                return a;
            }
        }
        for (Auftrag a : ALLE) {
            if (a.id.equals(id)) {
                return a;
            }
        }
        return null;
    }
}
