# Deadzone MC – Horror-Survival für Minecraft 1.21.1 (NeoForge)

Eine postapokalyptische Welt voller Nebel. Plündern, überleben, die Basis ausbauen – und herausfinden,
was in Station Null geschehen ist.

**Stand: Etappe 1 von 9** – Welt, Nebel und Wetter, verlassene Häuser mit Beute, erste Story-Notizen.
Monster, Waffen, Überlebenssystem, Basis und Klassen folgen in den nächsten Etappen.

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

### Häuser und Beute
- Häuser mit ein oder zwei Stockwerken, manchmal mit **Keller** – dort ist es stockdunkel, aber die Beute ist besser.
- Kisten und Fässer in Küche, Wohnraum und Schlafzimmer, Fässer in Autowracks.
- **Notizen** erzählen Stück für Stück, was passiert ist (Rechtsklick zum Lesen).

### Neue Items
Konservendose, Verband (heilt 3 Herzen), Schrott, Stoffreste, Batterie, Elektronikteile, Notizen.
Rezept: 3 Stoffreste ergeben 1 Verband.

### Spielstart
Alle starten im selben Gebiet, aber zufällig verteilt (höchstens rund 400 Blöcke voneinander entfernt).

---

## 5. Zum Testen (Cheats an)

| Befehl | Was er macht |
| --- | --- |
| `/deadzone wetter` | Zeigt das aktuelle Wetter |
| `/deadzone wetter dichter_nebel` | Wetter setzen: `klar`, `leichter_nebel`, `dichter_nebel`, `saurer_regen`, `aschesturm` |
| `/deadzone wetter saurer_regen 5` | Wetter für 5 Minuten |
| `/locate structure deadzonemc:siedlung` | Nächste Siedlung finden |
| `/locate structure deadzonemc:einzelhaus` | Nächstes einzelnes Haus |
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
