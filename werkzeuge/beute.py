"""Beutetabellen für Kisten und Fässer in Häusern und Autowracks.

Aufruf aus dem Projektordner:  python3 werkzeuge/beute.py
"""
import json
import os

MOD = "deadzonemc"
ZIEL = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "data", MOD, "loot_table", "kiste")


def item(name, gewicht, minimum=1, maximum=1, schaden=None):
    eintrag = {"type": "minecraft:item", "name": name, "weight": gewicht}
    funktionen = []
    if maximum > 1:
        funktionen.append({"function": "minecraft:set_count",
                           "count": {"type": "minecraft:uniform", "min": minimum, "max": maximum}})
    if schaden:
        funktionen.append({"function": "minecraft:set_damage",
                           "damage": {"type": "minecraft:uniform", "min": schaden[0], "max": schaden[1]}})
    if funktionen:
        eintrag["functions"] = funktionen
    return eintrag


def m(name):
    return f"{MOD}:{name}"


def notiz(id_, gewicht=1):
    return {"type": "minecraft:item", "name": m("notiz"), "weight": gewicht,
            "functions": [{"function": "minecraft:set_components", "components": {f"{MOD}:notiz": id_}}]}


def leer(gewicht):
    return {"type": "minecraft:empty", "weight": gewicht}


def pool(minimum, maximum, eintraege):
    return {"rolls": {"type": "minecraft:uniform", "min": minimum, "max": maximum}, "entries": eintraege}


def notizen_pool(chance_prozent, ids):
    """Höchstens eine Notiz pro Kiste, mit der angegebenen Wahrscheinlichkeit."""
    gewicht_notizen = len(ids)
    rest = max(0, round(gewicht_notizen * (100 - chance_prozent) / chance_prozent))
    return {"rolls": 1, "entries": [notiz(i) for i in ids] + [leer(rest)]}


TABELLEN = {
    "haus_kueche": [
        pool(2, 5, [
            item(m("konservendose"), 15, 1, 2),
            item("minecraft:bread", 5),
            item("minecraft:potato", 6, 1, 3),
            item("minecraft:carrot", 4, 1, 2),
            item("minecraft:apple", 3),
            item("minecraft:rotten_flesh", 6, 1, 3),
            item("minecraft:sugar", 3, 1, 2),
            item("minecraft:glass_bottle", 6, 1, 2),
            item("minecraft:bowl", 4),
            item("minecraft:paper", 4, 1, 3),
            item("minecraft:string", 3, 1, 2),
            item("minecraft:iron_nugget", 3, 1, 4),
            item("minecraft:torch", 2, 1, 2),
            leer(10),
        ]),
        notizen_pool(20, ["einkaufszettel", "abschied", "evakuierung"]),
    ],
    "haus_wohnraum": [
        pool(2, 5, [
            item("minecraft:paper", 6, 1, 4),
            item("minecraft:book", 3),
            item(m("stoffreste"), 6, 1, 3),
            item("minecraft:string", 4, 1, 3),
            item("minecraft:leather", 3),
            item("minecraft:candle", 3, 1, 2),
            item(m("konservendose"), 4),
            item(m("batterie"), 3),
            item(m("schrott"), 5, 1, 3),
            item("minecraft:clock", 1),
            item("minecraft:compass", 1),
            item("minecraft:flint", 2, 1, 2),
            item("minecraft:stick", 4, 1, 4),
            leer(10),
        ]),
        notizen_pool(30, ["tagebuch_1", "halden_flyer", "kinderzettel", "evakuierung"]),
    ],
    "haus_schlafzimmer": [
        pool(2, 4, [
            item(m("stoffreste"), 10, 1, 3),
            item("minecraft:white_wool", 4, 1, 2),
            item("minecraft:leather", 3),
            item(m("verband"), 5),
            item("minecraft:leather_helmet", 2, schaden=(0.2, 0.7)),
            item("minecraft:leather_chestplate", 1, schaden=(0.2, 0.7)),
            item("minecraft:leather_boots", 2, schaden=(0.2, 0.7)),
            item(m("konservendose"), 3),
            item("minecraft:book", 2),
            item("minecraft:candle", 2),
            item(m("batterie"), 2),
            leer(10),
        ]),
        notizen_pool(35, ["tagebuch_1", "tagebuch_2", "kinderzettel", "abschied"]),
    ],
    # Im dunklen Keller ist die Beute besser – wer sich hinuntertraut, wird belohnt.
    "haus_keller": [
        pool(3, 6, [
            item(m("schrott"), 10, 1, 4),
            item(m("elektronik"), 4, 1, 2),
            item(m("batterie"), 5, 1, 2),
            item("minecraft:iron_ingot", 4, 1, 3),
            item("minecraft:iron_nugget", 6, 2, 6),
            item("minecraft:coal", 6, 1, 4),
            item("minecraft:torch", 3, 1, 3),
            item("minecraft:arrow", 3, 2, 8),
            item("minecraft:bow", 1, schaden=(0.3, 0.8)),
            item("minecraft:crossbow", 1, schaden=(0.4, 0.9)),
            item("minecraft:iron_axe", 1, schaden=(0.3, 0.8)),
            item("minecraft:stone_axe", 2, schaden=(0.2, 0.6)),
            item("minecraft:shears", 1),
            item("minecraft:bucket", 2),
            item("minecraft:lead", 1),
            item(m("konservendose"), 6, 1, 3),
            item(m("verband"), 3, 1, 2),
            leer(6),
        ]),
        notizen_pool(30, ["tagebuch_2", "funkspruch", "halden_flyer"]),
    ],
    "autowrack": [
        pool(1, 4, [
            item(m("schrott"), 10, 1, 3),
            item(m("batterie"), 4),
            item(m("elektronik"), 3),
            item("minecraft:leather", 2),
            item(m("stoffreste"), 3, 1, 2),
            item(m("konservendose"), 2),
            item("minecraft:paper", 2),
            item("minecraft:iron_nugget", 3, 1, 3),
            leer(8),
        ]),
        notizen_pool(15, ["funkspruch", "halden_flyer", "evakuierung"]),
    ],
}


def main():
    os.makedirs(ZIEL, exist_ok=True)
    for name, pools in TABELLEN.items():
        with open(os.path.join(ZIEL, f"{name}.json"), "w", encoding="utf-8") as f:
            json.dump({"type": "minecraft:chest", "pools": pools}, f, indent=2, ensure_ascii=False)
            f.write("\n")
    print(f"{len(TABELLEN)} Beutetabellen geschrieben.")


if __name__ == "__main__":
    main()
