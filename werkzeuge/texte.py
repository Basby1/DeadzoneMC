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

    # Monster
    "entity.deadzonemc.wanderer": "Wanderer",
    "item.deadzonemc.wanderer_spawn_egg": "Spawn-Ei: Wanderer",
    "subtitles.deadzonemc.wanderer.ambient": "Wanderer stöhnt",
    "subtitles.deadzonemc.wanderer.hurt": "Wanderer wird verletzt",
    "subtitles.deadzonemc.wanderer.death": "Wanderer stirbt",
    "subtitles.deadzonemc.wanderer.step": "Schlurfende Schritte",
    "entity.deadzonemc.schreier": "Schreier",
    "item.deadzonemc.schreier_spawn_egg": "Spawn-Ei: Schreier",
    "subtitles.deadzonemc.schreier.ambient": "Schreier keucht",
    "subtitles.deadzonemc.schreier.hurt": "Schreier wird verletzt",
    "subtitles.deadzonemc.schreier.death": "Schreier stirbt",
    "subtitles.deadzonemc.schreier.schrei": "Schreier schreit",
    "entity.deadzonemc.lauerer": "Lauerer",
    "item.deadzonemc.lauerer_spawn_egg": "Spawn-Ei: Lauerer",
    "subtitles.deadzonemc.lauerer.hurt": "Lauerer wird verletzt",
    "subtitles.deadzonemc.lauerer.death": "Lauerer stirbt",
    "subtitles.deadzonemc.lauerer.atmen": "Leises Atmen",
    "subtitles.deadzonemc.lauerer.schreck": "Etwas springt hervor",
    "entity.deadzonemc.kriecher": "Kriecher",
    "item.deadzonemc.kriecher_spawn_egg": "Spawn-Ei: Kriecher",
    "subtitles.deadzonemc.kriecher.ambient": "Kriecher zischt",
    "subtitles.deadzonemc.kriecher.hurt": "Kriecher wird verletzt",
    "subtitles.deadzonemc.kriecher.death": "Kriecher stirbt",
    "subtitles.deadzonemc.kriecher.step": "Kratzen an der Wand",
    "entity.deadzonemc.der_stille": "Der Stille",
    "item.deadzonemc.der_stille_spawn_egg": "Spawn-Ei: Der Stille",
    "subtitles.deadzonemc.der_stille.hurt": "Porzellan knackt",
    "subtitles.deadzonemc.der_stille.death": "Der Stille zerbricht",
    "subtitles.deadzonemc.der_stille.naehe": "Etwas ist ganz nah",
    "entity.deadzonemc.imitator": "Imitator",
    "item.deadzonemc.imitator_spawn_egg": "Spawn-Ei: Imitator",
    "subtitles.deadzonemc.imitator.hurt": "Jemand wird verletzt",
    "subtitles.deadzonemc.imitator.death": "Imitator stirbt",
    "entity.deadzonemc.nebelwesen": "Nebelwesen",
    "item.deadzonemc.nebelwesen_spawn_egg": "Spawn-Ei: Nebelwesen",
    "subtitles.deadzonemc.nebelwesen.ambient": "Flüstern im Nebel",
    "subtitles.deadzonemc.nebelwesen.hurt": "Nebelwesen wird verletzt",
    "subtitles.deadzonemc.nebelwesen.death": "Nebelwesen vergeht",
    "entity.deadzonemc.schwarm": "Schwärmling",
    "item.deadzonemc.schwarm_spawn_egg": "Spawn-Ei: Schwärmling",
    "subtitles.deadzonemc.schwarm.ambient": "Fiepen",
    "subtitles.deadzonemc.schwarm.hurt": "Schwärmling wird verletzt",
    "subtitles.deadzonemc.schwarm.death": "Schwärmling stirbt",
    "subtitles.deadzonemc.schwarm.step": "Trippeln",
    "entity.deadzonemc.brut": "Brut",
    "item.deadzonemc.brut_spawn_egg": "Spawn-Ei: Brut",
    "subtitles.deadzonemc.brut.ambient": "Blubbern",
    "subtitles.deadzonemc.brut.hurt": "Brut wird verletzt",
    "subtitles.deadzonemc.brut.death": "Brut stirbt",
    "subtitles.deadzonemc.brut.platzen": "Brut platzt",
    "entity.deadzonemc.juggernaut": "Juggernaut",
    "item.deadzonemc.juggernaut_spawn_egg": "Spawn-Ei: Juggernaut",
    "subtitles.deadzonemc.juggernaut.ambient": "Schweres Schnaufen",
    "subtitles.deadzonemc.juggernaut.hurt": "Juggernaut wird verletzt",
    "subtitles.deadzonemc.juggernaut.death": "Juggernaut fällt",
    "subtitles.deadzonemc.juggernaut.step": "Donnernde Schritte",
    "subtitles.deadzonemc.juggernaut.brullen": "Juggernaut brüllt",
    "subtitles.deadzonemc.juggernaut.panzer": "Metall kreischt",

    # Items
    "item.deadzonemc.konservendose": "Konservendose",
    "item.deadzonemc.verband": "Verband",
    "item.deadzonemc.verband.info": "Gedrückt halten zum Anlegen: heilt 3 Herzen",
    "item.deadzonemc.antibiotika": "Antibiotika",
    "item.deadzonemc.antibiotika.info": "Heilt Infektion und Vergiftung",
    "item.deadzonemc.schmerzmittel": "Schmerzmittel",
    "item.deadzonemc.schmerzmittel.info": "Lässt 15 Sekunden lang heilen und dämpft Schaden",
    "item.deadzonemc.schrott": "Schrott",
    "item.deadzonemc.stoffreste": "Stoffreste",
    "item.deadzonemc.batterie": "Batterie",
    "item.deadzonemc.elektronik": "Elektronikteile",
    "item.deadzonemc.notiz": "Notiz",
    "item.deadzonemc.notiz.lesen": "Rechtsklick zum Lesen",

    "item.deadzonemc.handbuch": "Handbuch für Überlebende",
    "item.deadzonemc.handbuch.info": "Rechtsklick zum Lesen",
    "item.deadzonemc.startkarte": "Karte der Umgebung",

    # Neue Notizen
    "notiz.deadzonemc.brief.titel": "Brief ohne Briefmarke",
    "notiz.deadzonemc.brief.1": "Liebe Anne,\n\nhier draußen heißt es, die Zone sei nur abgesperrt, bis die Luft wieder sauber ist. Zwei Wochen, sagen sie.\n\nWir haben Pakete an die Absperrung gebracht. Die Soldaten haben sie nicht angenommen.",
    "notiz.deadzonemc.brief.2": "Ein Soldat hat mir leise gesagt, ich soll aufhören zu warten.\n\nIch schreibe trotzdem. Irgendwer wird diesen Brief schon finden.\n\nDeine Schwester",

    "notiz.deadzonemc.pfarrer.titel": "Letzter Eintrag im Kirchenbuch",
    "notiz.deadzonemc.pfarrer.1": "Elf Menschen haben in der Kirche Zuflucht gesucht. Wir haben die Türen verriegelt und gebetet.\n\nIn der dritten Nacht hat jemand an das Portal geklopft. Ganz ruhig. Dreimal.",
    "notiz.deadzonemc.pfarrer.2": "Die Stimme draußen klang wie die von Frau Kessler, die wir am ersten Tag begraben haben.\n\nHerr Wagner hat aufgemacht.\n\nMöge Gott uns vergeben. Er hat diesen Ort längst verlassen.",

    "notiz.deadzonemc.feuerwehr_einsatz.titel": "Einsatzprotokoll",
    "notiz.deadzonemc.feuerwehr_einsatz.1": "EINSATZPROTOKOLL\n\n06:12 Alarm: Brand in Industrieanlage, Halden-Werke, Station Null.\n06:40 Anfahrt abgebrochen. Dichter Nebel, Sicht unter 5 Meter.\n07:15 Kontakt zu Trupp 2 verloren.",
    "notiz.deadzonemc.feuerwehr_einsatz.2": "07:52 Trupp 2 zurück. Nicht ansprechbar. Starren in den Nebel.\n08:30 Militär übernimmt. Wir sollen abrücken und nicht darüber reden.\n\nUnterschrift unleserlich.",

    # Handbuch (eine Seite fasst nur wenig Text)
    "handbuch.deadzonemc.1": "HANDBUCH FÜR ÜBERLEBENDE\n\nDie Zone ist abgeriegelt. Niemand kommt, um dich zu holen.\n\nDieses Buch sammelt, was man wissen muss. Es wird mit der Zeit dicker.",
    "handbuch.deadzonemc.2": "GRUNDREGELN\n\n1. Bleib nicht allein.\n2. Sei vor der Nacht in Deckung.\n3. Nachts und in Höhlen ist der Nebel schwarz. Nimm Licht mit.\n4. Plane jeden Ausflug.",
    "handbuch.deadzonemc.3": "DIE KARTE\n\nDeine Karte ist am Anfang leer. Sie zeichnet die Gegend, in der du sie in der Hand hältst.\n\nOrtsschilder verraten, wo du bist. Straßen führen zu Orten.",
    "handbuch.deadzonemc.4": "WETTER I\n\nLeichter Nebel: der Normalfall.\n\nDichter Nebel: Sicht nur wenige Meter. Bleib bei der Gruppe.\n\nKlarer Tag: selten. Nutze ihn für weite Wege.",
    "handbuch.deadzonemc.5": "WETTER II\n\nSaurer Regen: verätzt die Haut. Such ein Dach oder trag einen Helm.\n\nAschesturm: man sieht nichts und bekommt kaum Luft. Ein Helm hilft ein wenig.",
    "handbuch.deadzonemc.6": "GEBIETE\n\nVorstadt: Dörfer und Höfe.\nToter Wald: kahl, dunkel, dicht.\nÖdland: trocken, Schutt, Masten.\nMoor: giftiges Wasser.\nTotes Meer: graue Küste, Wracks.",
    "handbuch.deadzonemc.7": "ORTE\n\nDörfer liegen an Straßen. Folge einer Straße und du findest Menschen – oder was von ihnen übrig ist.\n\nGerüchte sprechen von Kleinstädten und einer großen Stadt.",
    "handbuch.deadzonemc.8": "PLÜNDERN I\n\nJeder Behälter hat eigene Beute:\n\nKühlschrank: Essen\nKüchenschrank: Vorräte\nSpiegelschrank: Verbände\nKleiderschrank: Stoff, Kleidung",
    "handbuch.deadzonemc.9": "PLÜNDERN II\n\nWerkzeugkiste: Werkzeug, Schrott\nBriefkasten: Post, Notizen\nKeller: oft die beste Beute – aber dunkel.\n\nFeuerwehr, Werkstatt, Laden und Tankstelle lohnen sich.",
    "handbuch.deadzonemc.10": "GEGENSTÄNDE I\n\nKonservendose: sättigt gut und verdirbt nicht.\n\nVerband: zwei Sekunden anlegen, heilt drei Herzen. Aus drei Stoffresten herstellbar.",
    "handbuch.deadzonemc.11": "GEGENSTÄNDE II\n\nSchrott, Batterien, Elektronikteile und Stoffreste: Material für später. Heb sie auf.\n\nNotizen: Rechtsklick zum Lesen. Jede erzählt ein Stück der Geschichte.",
    "handbuch.deadzonemc.12": "MONSTER\n\nBisher keine Einträge.\n\nWas du gesehen hast, wird hier festgehalten – falls du es überlebst.",

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
    "befehl.deadzonemc.nebel.an": "Nebel ist an.",
    "befehl.deadzonemc.nebel.aus": "Nebel ist aus (nur die Sicht – das Wetter wirkt weiter).",

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
    "notiz.deadzonemc.abschied.1": "Lena,\n\nwir sind zum Sammelpunkt an der Sporthalle. Komm nach, sobald du das liest.\n\nNimm die Taschenlampe aus der Schublade.",
    "notiz.deadzonemc.abschied.2": "Geh nicht durch den Wald.\n\nWir warten auf dich.\n\n– Mama",

    "notiz.deadzonemc.tagebuch_1.titel": "Tagebuch, Tag 3",
    "notiz.deadzonemc.tagebuch_1.1": "Die Busse sind nicht gekommen. Der Nebel liegt jetzt seit drei Tagen über der Siedlung.\n\nHerr Brandt von gegenüber ist gestern rausgegangen, um nach seinem Hund zu suchen. Er ist nicht zurückgekommen.",
    "notiz.deadzonemc.tagebuch_1.2": "Heute Nacht habe ich ihn gehört. Er stand vor dem Haus und hat meinen Namen gerufen. Immer wieder, immer gleich, wie eine Aufnahme.",
    "notiz.deadzonemc.tagebuch_1.3": "Ich habe nicht aufgemacht.\n\nAls es hell wurde, war niemand da.\n\nKeine Fußspuren.",

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
