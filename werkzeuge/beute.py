"""Beutetabellen für alle Behälter in Gebäuden und Autos.

Aufruf aus dem Projektordner:  python3 werkzeuge/beute.py

Jeder Behälter hat passende Beute (wie in 7 Days to Die): Kühlschrank -> Essen, Medizinschrank ->
Verbandszeug, Werkzeugkiste -> Werkzeug. Waffen und bessere Ausrüstung kommen in einer späteren Etappe.
"""
import json
import os
import shutil

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


def notizen(chance_prozent, ids):
    """Höchstens eine Notiz pro Behälter, mit der angegebenen Wahrscheinlichkeit."""
    rest = max(0, round(len(ids) * (100 - chance_prozent) / chance_prozent))
    return {"rolls": 1, "entries": [notiz(i) for i in ids] + [leer(rest)]}


ESSEN_KLEIN = [
    item(m("konservendose"), 10, 1, 2),
    item("minecraft:bread", 3),
    item("minecraft:rotten_flesh", 5, 1, 2),
    item("minecraft:sugar", 2, 1, 2),
    item("minecraft:dried_kelp", 3, 1, 4),
    item("minecraft:cookie", 2, 1, 3),
]

TABELLEN = {
    # ---- Wohnhaus ----
    "kuehlschrank": [pool(1, 3, [
        item("minecraft:rotten_flesh", 14, 1, 3),
        item(m("konservendose"), 6),
        item("minecraft:apple", 3),
        item("minecraft:carrot", 3, 1, 2),
        item("minecraft:potato", 3, 1, 2),
        item("minecraft:milk_bucket", 1),
        item("minecraft:glass_bottle", 4),
        item("minecraft:spider_eye", 2),
        leer(12),
    ])],
    "kueche": [pool(1, 3, ESSEN_KLEIN + [
        item("minecraft:bowl", 4),
        item("minecraft:glass_bottle", 4, 1, 2),
        item("minecraft:iron_nugget", 2, 1, 3),
        item("minecraft:candle", 2),
        leer(10),
    ]), notizen(10, ["einkaufszettel"])],
    "wohnzimmer": [pool(1, 3, [
        item("minecraft:paper", 6, 1, 3),
        item("minecraft:book", 3),
        item("minecraft:candle", 4, 1, 2),
        item(m("batterie"), 4),
        item(m("elektronik"), 2),
        item("minecraft:clock", 1),
        item("minecraft:compass", 1),
        item("minecraft:string", 3, 1, 2),
        item(m("konservendose"), 2),
        leer(10),
    ]), notizen(20, ["tagebuch_1", "halden_flyer", "evakuierung"])],
    "nachttisch": [pool(1, 2, [
        item("minecraft:candle", 4),
        item("minecraft:paper", 4, 1, 2),
        item("minecraft:book", 2),
        item(m("verband"), 3),
        item(m("batterie"), 3),
        item("minecraft:clock", 1),
        leer(8),
    ]), notizen(30, ["tagebuch_1", "tagebuch_2", "abschied"])],
    "kleiderschrank": [pool(1, 3, [
        item(m("stoffreste"), 12, 1, 3),
        item("minecraft:white_wool", 4, 1, 2),
        item("minecraft:leather", 3),
        item("minecraft:leather_helmet", 2, schaden=(0.1, 0.6)),
        item("minecraft:leather_chestplate", 2, schaden=(0.1, 0.6)),
        item("minecraft:leather_leggings", 2, schaden=(0.1, 0.6)),
        item("minecraft:leather_boots", 2, schaden=(0.1, 0.6)),
        leer(8),
    ])],
    "kinderzimmer": [pool(1, 3, [
        item("minecraft:paper", 4, 1, 3),
        item("minecraft:string", 3, 1, 2),
        item("minecraft:cookie", 4, 1, 3),
        item("minecraft:feather", 2, 1, 3),
        item("minecraft:snowball", 1),
        item(m("batterie"), 2),
        leer(8),
    ]), notizen(40, ["kinderzettel"])],
    "badezimmer": [pool(1, 3, [
        item(m("verband"), 10, 1, 2),
        item(m("stoffreste"), 4, 1, 2),
        item("minecraft:glass_bottle", 4),
        item("minecraft:honey_bottle", 1),
        item("minecraft:sugar", 2),
        leer(10),
    ])],
    "keller": [pool(2, 5, [
        item(m("konservendose"), 10, 1, 3),
        item(m("schrott"), 8, 1, 3),
        item(m("batterie"), 4, 1, 2),
        item(m("elektronik"), 3),
        item("minecraft:coal", 6, 1, 4),
        item("minecraft:torch", 4, 1, 3),
        item("minecraft:iron_nugget", 5, 2, 6),
        item("minecraft:iron_ingot", 2, 1, 2),
        item("minecraft:bucket", 2),
        item("minecraft:glass_bottle", 3, 1, 3),
        item("minecraft:stone_axe", 1, schaden=(0.2, 0.6)),
        item("minecraft:lead", 1),
        leer(6),
    ]), notizen(25, ["tagebuch_2", "funkspruch", "halden_flyer"])],
    "werkzeug": [pool(1, 4, [
        item(m("schrott"), 10, 1, 4),
        item("minecraft:iron_nugget", 6, 2, 6),
        item("minecraft:iron_ingot", 2),
        item("minecraft:shears", 2, schaden=(0.2, 0.7)),
        item("minecraft:flint_and_steel", 1, schaden=(0.3, 0.8)),
        item("minecraft:stone_axe", 2, schaden=(0.2, 0.7)),
        item("minecraft:stone_pickaxe", 2, schaden=(0.2, 0.7)),
        item("minecraft:iron_shovel", 1, schaden=(0.3, 0.8)),
        item("minecraft:string", 3, 1, 3),
        item(m("elektronik"), 2),
        item(m("batterie"), 2),
        leer(6),
    ])],
    "schuppen": [pool(1, 3, [
        item("minecraft:wooden_hoe", 3, schaden=(0.2, 0.7)),
        item("minecraft:stone_shovel", 3, schaden=(0.2, 0.7)),
        item("minecraft:bone_meal", 4, 1, 4),
        item("minecraft:wheat_seeds", 5, 2, 6),
        item("minecraft:beetroot_seeds", 3, 1, 4),
        item("minecraft:string", 3, 1, 3),
        item("minecraft:stick", 4, 2, 5),
        item(m("schrott"), 3),
        leer(8),
    ])],
    "briefkasten": [pool(1, 2, [
        item("minecraft:paper", 10, 1, 3),
        item("minecraft:book", 1),
        leer(8),
    ]), notizen(45, ["evakuierung", "brief", "halden_flyer", "abschied"])],

    # ---- Dorf ----
    "scheune": [pool(2, 4, [
        item("minecraft:wheat", 8, 2, 6),
        item("minecraft:wheat_seeds", 6, 2, 8),
        item("minecraft:hay_block", 2),
        item("minecraft:leather", 4, 1, 2),
        item("minecraft:lead", 3),
        item("minecraft:saddle", 1),
        item("minecraft:iron_hoe", 1, schaden=(0.3, 0.8)),
        item("minecraft:stone_axe", 2, schaden=(0.2, 0.6)),
        item("minecraft:string", 3, 1, 3),
        item("minecraft:bucket", 2),
        item(m("schrott"), 3, 1, 2),
        leer(6),
    ])],
    "kirche": [pool(1, 3, [
        item("minecraft:candle", 10, 1, 3),
        item("minecraft:white_candle", 4, 1, 2),
        item("minecraft:book", 4),
        item("minecraft:paper", 4, 1, 3),
        item("minecraft:gold_nugget", 2, 1, 3),
        item(m("verband"), 3, 1, 2),
        item(m("konservendose"), 3, 1, 2),
        leer(6),
    ]), notizen(45, ["pfarrer"])],
    "laden_regal": [pool(1, 3, ESSEN_KLEIN + [
        item(m("batterie"), 4),
        item("minecraft:candle", 3),
        item("minecraft:paper", 2),
        item("minecraft:honey_bottle", 1),
        leer(14),
    ])],
    "laden_kuehlregal": [pool(1, 2, [
        item("minecraft:rotten_flesh", 10, 1, 3),
        item("minecraft:milk_bucket", 1),
        item("minecraft:glass_bottle", 4),
        item(m("konservendose"), 3),
        leer(10),
    ])],
    "laden_lager": [pool(2, 5, [
        item(m("konservendose"), 12, 1, 4),
        item("minecraft:bread", 4, 1, 2),
        item("minecraft:dried_kelp", 4, 2, 6),
        item(m("batterie"), 5, 1, 2),
        item("minecraft:candle", 4, 1, 3),
        item("minecraft:torch", 3, 2, 4),
        item(m("stoffreste"), 3, 1, 3),
        item("minecraft:paper", 3, 2, 4),
        leer(6),
    ]), notizen(15, ["einkaufszettel", "halden_flyer"])],
    "kneipe": [pool(1, 3, [
        item("minecraft:glass_bottle", 10, 1, 3),
        item("minecraft:honey_bottle", 3),
        item(m("konservendose"), 3),
        item("minecraft:bread", 3),
        item("minecraft:paper", 2),
        leer(8),
    ]), notizen(10, ["funkspruch"])],
    "feuerwehr": [pool(2, 4, [
        item("minecraft:iron_axe", 2, schaden=(0.2, 0.6)),
        item("minecraft:iron_helmet", 2, schaden=(0.2, 0.6)),
        item("minecraft:leather_chestplate", 3, schaden=(0.1, 0.5)),
        item("minecraft:leather_boots", 3, schaden=(0.1, 0.5)),
        item(m("verband"), 5, 1, 3),
        item("minecraft:bucket", 3),
        item("minecraft:water_bucket", 1),
        item("minecraft:torch", 4, 2, 5),
        item(m("batterie"), 3, 1, 2),
        item("minecraft:string", 3, 2, 4),
        leer(6),
    ]), notizen(35, ["feuerwehr_einsatz", "funkspruch"])],
    "werkstatt": [pool(2, 5, [
        item(m("schrott"), 12, 2, 5),
        item(m("elektronik"), 5, 1, 2),
        item(m("batterie"), 5, 1, 2),
        item("minecraft:iron_ingot", 4, 1, 2),
        item("minecraft:iron_nugget", 6, 2, 8),
        item("minecraft:chain", 2, 1, 3),
        item("minecraft:iron_pickaxe", 1, schaden=(0.3, 0.8)),
        item("minecraft:shears", 2, schaden=(0.2, 0.7)),
        item("minecraft:flint_and_steel", 2, schaden=(0.2, 0.7)),
        item("minecraft:leather", 2),
        leer(5),
    ])],
    "tankstelle": [pool(2, 4, ESSEN_KLEIN + [
        item(m("batterie"), 6, 1, 2),
        item(m("elektronik"), 2),
        item("minecraft:map", 2),
        item("minecraft:paper", 3, 1, 3),
        item("minecraft:torch", 3, 1, 3),
        item("minecraft:flint_and_steel", 1, schaden=(0.2, 0.6)),
        leer(8),
    ]), notizen(25, ["halden_flyer", "funkspruch"])],
    "ruine": [pool(1, 3, [
        item(m("schrott"), 10, 1, 3),
        item("minecraft:charcoal", 6, 1, 4),
        item("minecraft:coal", 4, 1, 3),
        item("minecraft:iron_nugget", 4, 1, 4),
        item("minecraft:bone", 3, 1, 2),
        item(m("konservendose"), 2),
        leer(8),
    ]), notizen(20, ["tagebuch_2", "kinderzettel"])],
    "autowrack": [pool(1, 3, [
        item(m("schrott"), 10, 1, 3),
        item(m("batterie"), 4),
        item(m("elektronik"), 3),
        item("minecraft:leather", 2),
        item(m("stoffreste"), 3, 1, 2),
        item(m("konservendose"), 2),
        item("minecraft:paper", 2),
        item("minecraft:map", 1),
        item("minecraft:iron_nugget", 3, 1, 3),
        leer(8),
    ]), notizen(15, ["funkspruch", "halden_flyer", "evakuierung"])],
}


def main():
    if os.path.isdir(ZIEL):
        shutil.rmtree(ZIEL)
    os.makedirs(ZIEL, exist_ok=True)
    for name, pools in TABELLEN.items():
        with open(os.path.join(ZIEL, f"{name}.json"), "w", encoding="utf-8") as f:
            json.dump({"type": "minecraft:chest", "pools": pools}, f, indent=2, ensure_ascii=False)
            f.write("\n")
    print(f"{len(TABELLEN)} Beutetabellen geschrieben.")


if __name__ == "__main__":
    main()
