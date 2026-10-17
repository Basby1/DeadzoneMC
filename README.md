# Deadzone MC – Horror-Survival für Minecraft 1.21.1 (NeoForge)

Eine postapokalyptische Welt voller Nebel. Plündern, überleben, die Basis ausbauen – und herausfinden,
was in Station Null geschehen ist.

**Stand: Etappe 1, Schritt 1** – Welt, Nebel und Wetter, Dörfer mit Straßennetz und zwölf Gebäudearten,
Beute je Behälter, Startkarte, Handbuch und Story-Notizen. Als Nächstes: Kleinstädte, dann Großstädte,
dann Monster, Waffen, Überlebenssystem, Basis und Klassen.

---

## 1. Mod herunterladen

Die Mod wird bei jeder Änderung automatisch auf GitHub gebaut.

1. Im Repository rechts auf **Releases** klicken (oder unter `…/releases` aufrufen).
2. Bei **„Deadzone MC – neueste Version“** die Datei `deadzonemc-0.1.0.jar` herunterladen.

## 2. In CurseForge installieren

1. In CurseForge ein **eigenes Profil** mit **Minecraft 1.21.1** und **NeoForge** anlegen.
   Am besten nicht das Petri-Heil-Profil nehmen: Deadzone MC verändert die Weltgenerierung.
2. Im Profil über das Drei-Punkte-Menü **„Ordner öffnen“**, dort in den Ordner `mods` gehen.
3. `deadzonemc-0.1.0.jar` hineinkopieren und das Spiel starten.

## 3. Neue Welt erstellen

Einfach eine **neue Welt** anlegen. Der normale Welttyp ist durch die Deadzone ersetzt –
jede neue Welt ist automatisch eine Deadzone. Bestehende Welten bleiben, wie sie sind.

---

## 4. Was es in Etappe 1 gibt

### Gebiete
| Gebiet | Was dich erwartet |
| --- | --- |
| Vorstadt | Verlassene Siedlungen mit Straßen, Autowracks, Strommasten |
| Toter Wald | Dicht stehende kahle Bäume, einzelne Häuser, viel Nebel |
| Ödland | Staub, Schutt, umgeknickte Masten, ab und zu ein Haus |
| Verseuchtes Moor | Schlamm, giftgrünes Wasser, tote Bäume |
| Totes Meer | Graues Wasser, Schiffswracks |

### Wetter
| Wetter | Wirkung |
| --- | --- |
| Leichter Nebel | Normalzustand, Sicht ca. 40 Blöcke |
| Dichter Nebel | Sicht nur noch 10–15 Blöcke |
| Saurer Regen | Schaden unter freiem Himmel – ein Helm nutzt sich stattdessen ab |
| Aschesturm | Kaum Sicht, Asche in der Luft; ohne Helm Schaden und langsamer |
| Klarer Tag | Selten: weite Sicht |

Nachts wird der Nebel fast schwarz, in Höhlen ebenfalls. Licht ist überlebenswichtig.

### Dörfer
Dörfer liegen an einer Hauptstraße, oft mit Querstraße. Gehwege, Mittellinie, Zebrastreifen, kaputte
Laternen, gelbe Ortsschilder mit dem Ortsnamen, parkende Autos, manchmal ein Unfall oder ein Stau an der
Ausfallstraße.

| Gebäude | Besonderheit |
| --- | --- |
| Einfamilienhaus | Küche, Bad, Wohn-, Schlaf- und Kinderzimmer, Treppe, oft Keller und Garage, Garten |
| Bungalow | Offener Wohnbereich, Terrasse, Carport |
| Bauernhof | Fachwerkhaus, Scheune mit Heuboden, Feld mit Vogelscheuche, Traktor, Brunnen |
| Kirche | Glockenturm, Bankreihen, Altar, Friedhof mit offenen Gräbern |
| Dorfladen | Regale, Kühlregal, Kasse, Lager |
| Gasthof | Theke, Küche, Fremdenzimmer, Biergarten |
| Feuerwehr | Löschfahrzeug, Spinde mit Ausrüstung, Schlauchturm |
| Werkstatt | Hebebühne mit Auto, Werkzeugkisten |
| Tankstelle | Zapfsäulen, Shop, Preisschild – meist vor dem Ort |
| Bushaltestelle, Kleingarten, Brandruine | Kleine Grundstücke dazwischen |

### Beute
Jeder Behälter hat eigene Beute: Kühlschrank (Essen), Küchenschrank (Vorräte), Spiegelschrank (Verbände),
Kleiderschrank (Stoff, Kleidung), Werkzeugkiste (Werkzeug, Schrott), Briefkasten (Post, Notizen), Keller
(oft die beste Beute). **Notizen** erzählen die Geschichte (Rechtsklick zum Lesen).

### Neue Items
Handbuch, Konservendose, Verband (heilt 3 Herzen), Schrott, Stoffreste, Batterie, Elektronikteile, Notizen.
Rezept: 3 Stoffreste ergeben 1 Verband.

### Spielstart
Alle starten im selben Gebiet, aber zufällig verteilt (höchstens rund 400 Blöcke voneinander entfernt).
Jeder bekommt eine **leere Karte**, die sich beim Erkunden füllt, und das **Handbuch für Überlebende**.

---

## 5. Zum Testen (Cheats an)

| Befehl | Was er macht |
| --- | --- |
| `/deadzone wetter` | Zeigt das aktuelle Wetter |
| `/deadzone wetter dichter_nebel` | Wetter setzen: `klar`, `leichter_nebel`, `dichter_nebel`, `saurer_regen`, `aschesturm` |
| `/deadzone wetter saurer_regen 5` | Wetter für 5 Minuten |
| `/deadzone nebel aus` | Nebel ausschalten (für alle; `an` schaltet ihn wieder ein) |
| `/deadzone orte` | Nächste Orte mit Name, Entfernung und Richtung |
| `/deadzone hin` | Direkt ins nächste Dorf |
| `/locate biome deadzonemc:toter_wald` | Nächstes Gebiet finden |
| `/time set night` | Nacht – um den schwarzen Nebel zu sehen |

Im Kreativmodus gibt es einen eigenen Tab **„Deadzone MC“** mit allen Items und Notizen.

**Falls etwas nicht stimmt:** Screenshot machen oder beschreiben, was passiert. Bei einem Absturz die
Datei `crash-reports/…txt` bzw. `logs/latest.log` aus dem Profilordner schicken.

---

## Für Entwickler

- Bauen: `./gradlew build` (Java 21). Ergebnis in `build/libs/`.
- Daten werden mit Skripten in `werkzeuge/` erzeugt: `welt.py` (Gebiete, Strukturen), `beute.py`,
  `texte.py`, `texturen.py`. Nach Änderungen das jeweilige Skript ausführen.
- Bei jedem Hochladen startet GitHub zusätzlich einen **Testserver** (`werkzeuge/servertest.py`), der eine
  Welt erzeugt, alle Befehle ausprobiert und bei Fehlern rot wird.
