"""Alle Texte von Deadzone MC (Sprachdateien).

Aufruf aus dem Projektordner:  python3 werkzeuge/texte.py
Geschrieben wird de_de.json. en_us.json bekommt denselben Inhalt, damit nirgends ein roher
Schlüssel auftaucht, falls das Spiel auf Englisch läuft.
"""
import json
import os

ZIEL = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "deadzonemc", "lang")

TEXTE = {
    "itemGroup.deadzonemc": "Deadzone MC",

    # Items
    "item.deadzonemc.konservendose": "Konservendose",
    "item.deadzonemc.verband": "Verband",
    "item.deadzonemc.verband.info": "Gedrückt halten zum Anlegen: heilt 3 Herzen",
    "item.deadzonemc.schrott": "Schrott",
    "item.deadzonemc.stoffreste": "Stoffreste",
    "item.deadzonemc.batterie": "Batterie",
    "item.deadzonemc.elektronik": "Elektronikteile",
    "item.deadzonemc.notiz": "Notiz",
    "item.deadzonemc.notiz.lesen": "Rechtsklick zum Lesen",

    # Gebiete
    "biome.deadzonemc.vorstadt": "Vorstadt",
    "biome.deadzonemc.toter_wald": "Toter Wald",
    "biome.deadzonemc.oedland": "Ödland",
    "biome.deadzonemc.verseuchtes_moor": "Verseuchtes Moor",
    "biome.deadzonemc.totes_meer": "Totes Meer",
    "generator.deadzonemc.deadzone": "Deadzone",

    # Wetter
    "wetter.deadzonemc.klar": "Klarer Tag",
    "wetter.deadzonemc.leichter_nebel": "Leichter Nebel",
    "wetter.deadzonemc.dichter_nebel": "Dichter Nebel",
    "wetter.deadzonemc.saurer_regen": "Saurer Regen",
    "wetter.deadzonemc.aschesturm": "Aschesturm",
    "wetter.deadzonemc.klar.meldung": "Der Nebel lichtet sich. Selten sieht man so weit.",
    "wetter.deadzonemc.leichter_nebel.meldung": "Grauer Dunst legt sich über das Land.",
    "wetter.deadzonemc.dichter_nebel.meldung": "Der Nebel zieht sich zu. Man sieht kaum die eigene Hand.",
    "wetter.deadzonemc.saurer_regen.meldung": "Es beginnt zu regnen. Der Regen brennt auf der Haut.",
    "wetter.deadzonemc.aschesturm.meldung": "Asche fällt vom Himmel. Jeder Atemzug kratzt.",

    # Befehle
    "befehl.deadzonemc.wetter.info": "Aktuelles Wetter: %s (noch etwa %s Minuten)",
    "befehl.deadzonemc.wetter.gesetzt": "Wetter gesetzt: %s",
    "befehl.deadzonemc.wetter.unbekannt": "Unbekanntes Wetter: %s",

    # Tod
    "death.attack.deadzonemc.saurer_regen": "%1$s wurde vom sauren Regen verätzt",
    "death.attack.deadzonemc.saurer_regen.player": "%1$s wurde vom sauren Regen verätzt, während %2$s zusah",
    "death.attack.deadzonemc.asche": "%1$s ist in der Asche erstickt",
    "death.attack.deadzonemc.asche.player": "%1$s ist in der Asche erstickt, auf der Flucht vor %2$s",

    # Start
    "start.deadzonemc.ankunft": "Du bist allein. Irgendwo hier draußen sind die anderen.",

    # Notizen
    "notiz.deadzonemc.unleserlich": "Die Schrift ist verwischt und nicht mehr zu lesen.",

    "notiz.deadzonemc.start.titel": "Funkspruch (Mitschrift)",
    "notiz.deadzonemc.start.1": "Mitschrift eines Funkspruchs, mit Bleistift auf die Rückseite eines Kassenbons gekritzelt:\n\n„... an alle, die das hören. Bleibt zusammen. Sucht euch einen festen Ort, bevor es dunkel wird. Macht Licht, aber nicht zu viel.“",
    "notiz.deadzonemc.start.2": "„Der Nebel ist nicht nur Nebel. Wenn ihr Schritte hört und niemanden seht: Bleibt stehen. Wenn jemand eure Namen ruft: Antwortet nicht.\n\nEnde der Übertragung.“",

    "notiz.deadzonemc.evakuierung.titel": "Evakuierungsbescheid",
    "notiz.deadzonemc.evakuierung.1": "AMTLICHE BEKANNTMACHUNG\n\nAufgrund eines Störfalls in einer Industrieanlage wird das Gebiet vorsorglich geräumt.\n\nSammelpunkt: Sporthalle.\nMitnehmen: Ausweis, Medikamente, ein Gepäckstück.",
    "notiz.deadzonemc.evakuierung.2": "Haustiere bleiben zurück.\nFenster und Türen schließen.\nNicht in den Nebel gehen.\n\nEs besteht keine Gefahr für die Bevölkerung.\n\n– Krisenstab, im Auftrag der Bundeswehr",

    "notiz.deadzonemc.einkaufszettel.titel": "Einkaufszettel",
    "notiz.deadzonemc.einkaufszettel.1": "Wasser (VIEL!)\nKonserven\nBatterien\nKerzen\nKlebeband\nMedizin für Oma\n\nNICHT nach Sonnenuntergang raus.\nNICHT nach Sonnenuntergang raus.\nNICHT nach Sonnenuntergang raus.",

    "notiz.deadzonemc.abschied.titel": "Zettel am Kühlschrank",
    "notiz.deadzonemc.abschied.1": "Lena,\n\nwir sind zum Sammelpunkt an der Sporthalle. Komm nach, sobald du das liest.\n\nNimm die Taschenlampe aus der Schublade. Geh nicht durch den Wald.\n\nWir warten auf dich.\n– Mama",

    "notiz.deadzonemc.tagebuch_1.titel": "Tagebuch, Tag 3",
    "notiz.deadzonemc.tagebuch_1.1": "Die Busse sind nicht gekommen. Der Nebel liegt jetzt seit drei Tagen über der Siedlung.\n\nHerr Brandt von gegenüber ist gestern rausgegangen, um nach seinem Hund zu suchen. Er ist nicht zurückgekommen.",
    "notiz.deadzonemc.tagebuch_1.2": "Heute Nacht habe ich ihn gehört. Er stand vor dem Haus und hat meinen Namen gerufen. Immer wieder, immer gleich, wie eine Aufnahme.\n\nIch habe nicht aufgemacht.\n\nAls es hell wurde, war niemand da. Keine Fußspuren.",

    "notiz.deadzonemc.tagebuch_2.titel": "Tagebuch, Tag 9",
    "notiz.deadzonemc.tagebuch_2.1": "Das Wasser aus der Leitung riecht nach Metall. Ich trinke nur noch abgekochtes.\n\nIch habe die Fenster mit Brettern zugenagelt. Wenn es dunkel wird, kratzt etwas an der Hauswand.",
    "notiz.deadzonemc.tagebuch_2.2": "Es hört auf, sobald ich die Taschenlampe anmache. Es fängt wieder an, sobald ich sie ausmache.\n\nDie Batterien reichen noch für zwei Nächte.\n\nIch gehe in den Keller.",

    "notiz.deadzonemc.halden_flyer.titel": "Prospekt der Halden-Werke",
    "notiz.deadzonemc.halden_flyer.1": "HALDEN-WERKE\nKlare Luft für alle.\n\nUnser Projekt AURA reinigt die Luft Ihrer Region: natürlich, sicher und unsichtbar.\n\nWinzige Helfer im Nebel binden Schadstoffe und machen sie unschädlich.",
    "notiz.deadzonemc.halden_flyer.2": "Ab Herbst testen wir AURA in unserer Forschungsanlage Station Null.\n\nSie werden den Unterschied atmen.\n\n(Jemand hat mit Kugelschreiber darunter geschrieben: „Sie haben es gewusst.“)",

    "notiz.deadzonemc.kinderzettel.titel": "Kinderzeichnung",
    "notiz.deadzonemc.kinderzettel.1": "Eine Wachsmalzeichnung: ein Haus, ein graues Gekritzel drumherum, und darin eine lange, schwarze Gestalt ohne Gesicht.\n\nDarunter, in Kinderschrift:",
    "notiz.deadzonemc.kinderzettel.2": "„Ich hab den großen Mann im Nebel gesehen. Er hat kein Gesicht. Er bewegt sich nicht, wenn man hinguckt. Nur wenn man wegguckt.\n\nMama sagt, ich soll nicht lügen.“",

    "notiz.deadzonemc.funkspruch.titel": "Abgefangener Funkspruch",
    "notiz.deadzonemc.funkspruch.1": "AN: alle Posten Sektor 4\n\nSektor 4 ist abgeriegelt. Keine weiteren Evakuierungen. Zivilisten im Sperrgebiet verbleiben bis auf Weiteres vor Ort.\n\nWiederhole: Niemand verlässt die Zone.",
    "notiz.deadzonemc.funkspruch.2": "Bei Kontakt mit Personen, die aus dem Nebel kommen: nicht ansprechen. Abstand halten. Feuer frei nach Ermessen.\n\nBefehl bestätigt.\n\n– Die Wacht",
}


def main():
    os.makedirs(ZIEL, exist_ok=True)
    for datei in ("de_de.json", "en_us.json"):
        with open(os.path.join(ZIEL, datei), "w", encoding="utf-8") as f:
            json.dump(TEXTE, f, indent=2, ensure_ascii=False)
            f.write("\n")
    print(f"{len(TEXTE)} Texte geschrieben.")


if __name__ == "__main__":
    main()
