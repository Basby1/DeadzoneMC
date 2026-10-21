"""Funkstation: Texturen, Modell, Blockstate, Rezept, Beute und Texte (Zonenfunk und die zwölf Sendungen der Stimme).

  python3 werkzeuge/funk.py   (danach texte.py)
"""
import json
import os

from PIL import Image, ImageDraw

W = os.path.dirname(os.path.abspath(__file__))
A = os.path.join(W, "..", "src", "main", "resources", "assets", "deadzonemc")
D = os.path.join(W, "..", "src", "main", "resources", "data", "deadzonemc")


def c(h):
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4)) + (255,)


def blech(d):
    d.rectangle((0, 0, 15, 15), fill=c("4a5238"))
    d.rectangle((0, 0, 15, 15), outline=c("2e3324"))
    for x, y in ((1, 1), (14, 1), (1, 14), (14, 14)):
        d.point((x, y), fill=c("8a8e7a"))


def front():
    b = Image.new("RGBA", (16, 16))
    d = ImageDraw.Draw(b)
    blech(d)
    d.rectangle((2, 2, 13, 6), fill=c("1a2418"))  # Anzeige
    d.line([(3, 5), (5, 3), (7, 5), (9, 3), (11, 4), (12, 3)], fill=c("7aff6a"))
    for x in (3, 7, 11):  # Drehregler
        d.ellipse((x - 1, 8, x + 1, 10), fill=c("1e1e1e"), outline=c("9a9a8a"))
    for x in range(3, 13, 2):  # Lautsprechergitter
        d.line([(x, 12), (x, 14)], fill=c("2a2e22"))
    d.point((13, 9), fill=c("ff3a2a"))
    return b


def seite():
    b = Image.new("RGBA", (16, 16))
    d = ImageDraw.Draw(b)
    blech(d)
    for y in range(4, 13, 2):
        d.line([(3, y), (12, y)], fill=c("353b28"))
    return b


def oben():
    b = Image.new("RGBA", (16, 16))
    d = ImageDraw.Draw(b)
    blech(d)
    d.line([(12, 2), (12, 13)], fill=c("8a8e7a"))  # Antenne liegt oben auf
    d.rectangle((3, 3, 8, 8), fill=c("3a4030"))
    return b


STIMME = [
    "…wenn ihr das hört: Ihr seid nicht allein. Ich höre euch schon eine Weile zu. Sendet nicht zu lange – sie hören auch.",
    "Der Nebel ist kein Wetter. Er kam aus den Halden-Werken. Wir nannten den Ort Station Null.",
    "Die Infizierten folgen dem Lärm. Aber die Großen… die folgen etwas anderem. Ich glaube, sie folgen dem Kern.",
    "Die Wacht riegelt ab, weil sie Befehle hat. Die Befehle kommen von oben. Sehr weit oben.",
    "Ich war Techniker in Sektor 4. Wir haben Proben nach oben geschickt, in den Orbit. Keiner hat gefragt, warum.",
    "Heute Nacht war es still. Das ist schlimmer als die Schreie.",
    "Wenn eine Emission kommt: unter die Erde. Keller, Metro, Bunker. Alles andere ist ein Grab.",
    "In der Reaktorhalle steht etwas, das einmal ein Mensch war. Es leuchtet. Geht nicht ohne Schutz hinein.",
    "Das Herz schlägt. Unter der Zone, jenseits des Risses. Spürt ihr es nachts im Boden?",
    "Mein Gerät macht es nicht mehr lange. Hört zu: Der Ursprung ist nicht hier unten. Er war nie hier unten.",
    "Ich habe Lichter am Himmel gesehen, wo keine Sterne sein sollten. Die Station ist noch da oben. Sie wartet.",
    "Das ist meine letzte Sendung. Was auch immer ihr tut – lasst die Zone nicht gewinnen. Ende.",
]


def main():
    for name, fn in (("funkstation_front", front), ("funkstation_seite", seite), ("funkstation_oben", oben)):
        fn().save(os.path.join(A, "textures", "block", f"{name}.png"))
    with open(os.path.join(A, "models", "block", "funkstation.json"), "w") as f:
        json.dump({"parent": "minecraft:block/orientable", "textures": {
            "top": "deadzonemc:block/funkstation_oben", "front": "deadzonemc:block/funkstation_front",
            "side": "deadzonemc:block/funkstation_seite"}}, f, indent=1)
    with open(os.path.join(A, "blockstates", "funkstation.json"), "w") as f:
        json.dump({"variants": {f"facing={r}": ({"model": "deadzonemc:block/funkstation", "y": y} if y else
                                                {"model": "deadzonemc:block/funkstation"})
                                for r, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270))}}, f, indent=1)
    with open(os.path.join(A, "models", "item", "funkstation.json"), "w") as f:
        json.dump({"parent": "deadzonemc:block/funkstation"}, f, indent=1)
    with open(os.path.join(D, "loot_table", "blocks", "funkstation.json"), "w") as f:
        json.dump({"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": "deadzonemc:funkstation"}],
                                                        "conditions": [{"condition": "minecraft:survives_explosion"}]}]}, f, indent=1)
    with open(os.path.join(D, "recipe", "funkstation.json"), "w") as f:
        json.dump({"type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["IDI", "EBE", "III"],
                   "key": {"I": {"item": "minecraft:iron_ingot"}, "D": {"item": "deadzonemc:drohnenteile"},
                           "E": {"item": "deadzonemc:elektronik"}, "B": {"item": "deadzonemc:batterie"}},
                   "result": {"id": "deadzonemc:funkstation", "count": 1}}, f, indent=1)
    texte = {
        "block.deadzonemc.funkstation": "Funkstation",
        "item.deadzonemc.funkstation": "Funkstation",
        "funk.deadzonemc.kopf": "─── Funkstation · Zonenfunk ───",
        "funk.deadzonemc.emission.jetzt": "WARNUNG: Emission im Gange! Sofort unter die Erde!",
        "funk.deadzonemc.emission.bald": "Messstationen melden: nächste Emission in etwa %s Minuten.",
        "funk.deadzonemc.emission.ruhig": "Messstationen melden: keine Emission in Sicht.",
        "funk.deadzonemc.abwurf": "Versorgungsabwurf angekündigt, ungefähr bei %s, %s.",
        "funk.deadzonemc.stimme.morgen": "Auf der Frequenz der Stimme: nur Rauschen. Vielleicht morgen wieder.",
        "funk.deadzonemc.stimme.stille": "Auf der Frequenz der Stimme: Stille. Sie sendet nicht mehr.",
    }
    for i, t in enumerate(STIMME, 1):
        texte[f"funk.deadzonemc.stimme.{i}"] = f"[Die Stimme, Sendung {i}/12] „{t}“"
    with open(os.path.join(W, "funk_texte.json"), "w", encoding="utf-8") as f:
        json.dump(texte, f, indent=1, ensure_ascii=False)
    tag = os.path.join(D, "..", "minecraft", "tags", "block", "mineable", "pickaxe.json")
    t = json.load(open(tag))
    if "deadzonemc:funkstation" not in t["values"]:
        t["values"].append("deadzonemc:funkstation")
    with open(tag, "w") as f:
        json.dump(t, f, indent=1)
    print("Funkstation geschrieben")


if __name__ == "__main__":
    main()
