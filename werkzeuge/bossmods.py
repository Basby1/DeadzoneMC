"""Boss-Trophäen: Bilder, Itemmodelle und Texte (Namen, Wirkungen in Waffen und Rüstung).

  python3 werkzeuge/bossmods.py   (danach texte.py)
"""
import json
import os

from PIL import Image, ImageDraw

W = os.path.dirname(os.path.abspath(__file__))
A = os.path.join(W, "..", "src", "main", "resources", "assets", "deadzonemc")

T = {  # id: (Name, Waffe, Rüstung)
    "schlaechter": ("Schlächterhaken", "Lebensraub: heilt dich um 15 % des Schadens", "Blutrausch: nach jedem Kill kurz Stärke"),
    "erfrorene": ("Frostherz", "Treffer verlangsamen stark", "Angreifer werden verlangsamt, kein Erfrieren"),
    "pilzvater": ("Sporenherz", "Treffer vergiften", "Immun gegen Gift"),
    "nebelmutter": ("Nebelschleier", "Treffer blenden – Gegner verlieren dich manchmal aus den Augen",
                    "Im Dunkeln beim Schleichen unsichtbar"),
    "schlund": ("Schlundzahn", "+25 % Schaden gegen große Gegner und Bosse", "+10 % Rückstoßfestigkeit je Teil"),
    "brutkoenigin": ("Brutdrüse", "Säure: Treffer lassen Gegner verdorren", "−20 % Fallschaden je Teil"),
    "strahlende": ("Strahlenherz", "+2 Strahlenschaden je Treffer", "−20 % Strahlung je Teil"),
    "hirte": ("Hirtenglocke", "Treffer stoßen Gegner weit zurück", "+3 % Tempo je Teil"),
    "kommandant": ("Kommandantenabzeichen", "Schusswaffen laden 25 % schneller, +10 % Nahkampfschaden", "+1 Rüstung je Teil"),
    "verschmolzener": ("Verschmolzenes Herz", "Jeder Kill heilt 2 Herzen", "+1 Herz je Teil"),
    "das_herz": ("Glutherz", "Treffer setzen Gegner in Brand", "Feuerschaden halbiert, ab 2 Teilen immun"),
    "der_ursprung": ("Leerenkern", "+25 % Schaden gegen alles", "Fallschaden halbiert, ab 2 Teilen keiner"),
}


def c(h, a=255):
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4)) + (a,)


def herz(d, haupt, hell, dunkel):
    d.polygon([(3, 5), (5, 3), (8, 5), (11, 3), (13, 5), (13, 8), (8, 13), (3, 8)], fill=c(haupt), outline=c(dunkel))
    d.point([(5, 5), (6, 5), (5, 6)], fill=c(hell))


def bild(art):
    b = Image.new("RGBA", (16, 16))
    d = ImageDraw.Draw(b)
    if art == "schlaechter":  # Fleischerhaken mit Blut
        d.line([(9, 1), (9, 9)], fill=c("8a8e92"), width=2)
        d.arc((3, 7, 11, 15), 0, 200, fill=c("8a8e92"), width=2)
        d.point([(4, 10), (3, 11), (9, 12)], fill=c("8a1010"))
        d.rectangle((8, 0, 10, 1), fill=c("4a4e52"))
    elif art == "erfrorene":
        herz(d, "7ac8f0", "e8f8ff", "2a5a8a")
        d.line([(8, 6), (8, 11)], fill=c("e8f8ff"))
    elif art == "pilzvater":
        herz(d, "6a7a5a", "b8c8a0", "2a3a1a")
        d.point([(6, 8), (9, 7), (10, 9), (7, 10)], fill=c("c8f0a0"))
    elif art == "nebelmutter":  # wehender Schleier
        d.polygon([(4, 1), (12, 1), (13, 9), (11, 15), (9, 12), (7, 15), (5, 12), (3, 14), (3, 6)], fill=c("d8dcd6", 200),
                  outline=c("8a9088"))
        d.point([(6, 5), (10, 5)], fill=c("101818"))
    elif art == "schlund":  # Reißzahn
        d.polygon([(3, 2), (13, 2), (12, 6), (9, 15), (7, 15), (4, 6)], fill=c("e8e0c8"), outline=c("6a5a40"))
        d.line([(5, 3), (7, 10)], fill=c("fff8e8"))
        d.line([(4, 2), (12, 2)], fill=c("9a5a4a"))
    elif art == "brutkoenigin":  # Drüse
        d.ellipse((3, 3, 13, 14), fill=c("c87a8a"), outline=c("6a2a3a"))
        for p in [(6, 6), (9, 8), (7, 10), (10, 11)]:
            d.ellipse((p[0] - 1, p[1] - 1, p[0] + 1, p[1] + 1), fill=c("e8f060"))
    elif art == "strahlende":
        herz(d, "6aa83a", "d8ff8a", "2a4a1a")
        d.point([(8, 8), (7, 7), (9, 7)], fill=c("f0ffb0"))
    elif art == "hirte":  # Glocke
        d.polygon([(6, 3), (10, 3), (12, 11), (4, 11)], fill=c("b88a2a"), outline=c("5a3a0a"))
        d.rectangle((3, 11, 13, 12), fill=c("8a6a1a"))
        d.ellipse((7, 12, 9, 14), fill=c("4a3a1a"))
        d.line([(8, 1), (8, 3)], fill=c("6a5a3a"))
    elif art == "kommandant":  # Abzeichen
        d.polygon([(8, 1), (10, 6), (15, 6), (11, 9), (13, 15), (8, 11), (3, 15), (5, 9), (1, 6), (6, 6)], fill=c("c8a03a"),
                  outline=c("5a4a14"))
        d.point([(8, 7)], fill=c("8a1e1e"))
    elif art == "verschmolzener":
        herz(d, "a85a5a", "e8a0a0", "4a1a1a")
        d.line([(4, 7), (12, 7)], fill=c("6a2a2a"))
        d.line([(8, 4), (8, 12)], fill=c("6a2a2a"))
    elif art == "das_herz":
        herz(d, "e8601a", "ffe08a", "5a1a0a")
        d.point([(8, 8), (9, 9), (7, 9)], fill=c("fff0a0"))
    elif art == "der_ursprung":  # Leerenkern
        d.ellipse((3, 3, 13, 13), fill=c("3a1a4a"), outline=c("c060ff"))
        d.ellipse((6, 6, 10, 10), fill=c("c060ff"))
        d.point([(7, 7)], fill=c("f0d8ff"))
    return b


def main():
    texte = {"bossmod.deadzonemc.kopf": "Boss-Modifikation: %s",
             "bossmod.deadzonemc.in_waffe": "In einer Waffe:",
             "bossmod.deadzonemc.in_ruestung": "In einem Rüstungsteil:",
             "bossmod.deadzonemc.hinweis": "An der Zonenwerkbank einbauen (Trophäe ins Materialfeld)",
             "bossmod.deadzonemc.einbauen": "Boss-Modifikation einbauen",
             "bossmod.deadzonemc.kosten": "Kosten: %s Erfahrungsstufen und die Trophäe",
             "bossmod.deadzonemc.keine_trophaee": "Lege eine Boss-Trophäe ins Materialfeld.",
             "bossmod.deadzonemc.nicht_modbar": "Lege eine Waffe oder ein Rüstungsteil oben ein.",
             "bossmod.deadzonemc.ersetzt": "Ersetzt die vorhandene Modifikation (%s).",
             "bossmod.deadzonemc.fertig": "%s ist modifiziert."}
    os.makedirs(os.path.join(A, "textures", "item"), exist_ok=True)
    for art, (name, waffe, ruestung) in T.items():
        texte[f"item.deadzonemc.trophaee_{art}"] = name
        texte[f"bossmod.deadzonemc.{art}.waffe"] = waffe
        texte[f"bossmod.deadzonemc.{art}.ruestung"] = ruestung
        bild(art).save(os.path.join(A, "textures", "item", f"trophaee_{art}.png"))
        with open(os.path.join(A, "models", "item", f"trophaee_{art}.json"), "w") as f:
            json.dump({"parent": "minecraft:item/generated", "textures": {"layer0": f"deadzonemc:item/trophaee_{art}"}}, f, indent=1)
    with open(os.path.join(W, "bossmods_texte.json"), "w", encoding="utf-8") as f:
        json.dump(texte, f, indent=1, ensure_ascii=False)
    print(len(T), "Trophäen")


if __name__ == "__main__":
    main()
