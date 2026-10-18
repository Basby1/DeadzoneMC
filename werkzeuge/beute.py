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


# ---- Waffen (TaCZ) ----------------------------------------------------------------------------
# Munition je Waffe und Magazingröße (aus dem TaCZ-Standardpaket ausgelesen, werkzeuge/tacz_munition.py)
WAFFEN = {
    "glock_17": ("9mm", 17), "m9a4": ("9mm", 17), "cz75": ("9mm", 16), "p320": ("45acp", 12),
    "m1911": ("45acp", 7), "hk_mk23": ("45acp", 12), "rhino357": ("357mag", 6), "deagle": ("50ae", 7),
    "hk_mp5a5": ("9mm", 30), "uzi": ("9mm", 20), "ump45": ("45acp", 25),
    "m870": ("12g", 5), "spas_12": ("12g", 5), "m1014": ("12g", 6), "db_short": ("12g", 2), "db_long": ("12g", 2),
    "kar98": ("792x57", 4), "m700": ("30_06", 5), "sks_tactical": ("762x39", 10),
    "g36k": ("556x45", 30), "hk416d": ("556x45", 30), "m4a1": ("556x45", 30), "hk_g3": ("308", 20),
    "ak47": ("762x39", 30), "mk14": ("308", 10), "m249": ("556x45", 75),
}


# Alte TaCZ-Namen -> eigene Deadzone-Waffen (Waffen/Feuerwaffen.java)
EIGENE = {
    "glock_17": "p9", "m9a4": "p9", "cz75": "p9", "p320": "p9", "m1911": "p9", "hk_mk23": "p9",
    "rhino357": "revolver", "deagle": "revolver", "hk_mp5a5": "mp9", "uzi": "mp9", "ump45": "mp9",
    "m870": "pumpgun", "spas_12": "pumpgun", "m1014": "pumpgun", "db_short": "doppelflinte", "db_long": "doppelflinte",
    "g36k": "sturmgewehr", "hk416d": "sturmgewehr", "m4a1": "sturmgewehr", "ak47": "kalaschnikow",
    "sks_tactical": "kalaschnikow", "hk_g3": "kalaschnikow", "kar98": "jagdgewehr", "m700": "jagdgewehr",
    "mk14": "praezisionsgewehr", "m249": "mg",
}
MAGAZIN = {"p9": 15, "revolver": 6, "mp9": 30, "pumpgun": 6, "doppelflinte": 2, "sturmgewehr": 30, "kalaschnikow": 30,
           "jagdgewehr": 5, "praezisionsgewehr": 10, "mg": 80, "rohrpistole": 1, "rohrgewehr": 1, "armbrust": 1}
KALIBER = {"9mm": "9mm", "45acp": "9mm", "357mag": "357", "50ae": "357", "12g": "schrot", "556x45": "556",
           "762x39": "762", "308": "762", "792x57": "762", "30_06": "762"}


def waffe(name, gewicht, voll=False):
    """Eine eigene Schusswaffe (alte TaCZ-Namen werden übersetzt), meist mit fast leerem Magazin und Gebrauchsspuren."""
    art = EIGENE.get(name, name)
    mag = MAGAZIN[art]
    schuss = mag if voll else max(0, mag // 4)
    komp = {f"{MOD}:magazin": schuss}
    if not voll:
        komp[f"{MOD}:verschleiss"] = 250
    return {"type": "minecraft:item", "name": f"{MOD}:{art}", "weight": gewicht,
            "functions": [{"function": "minecraft:set_components", "components": komp}]}


def munition(art, gewicht, minimum, maximum):
    kal = KALIBER.get(art, art)
    return item(f"{MOD}:munition_{kal}", gewicht, minimum, maximum)


def besonders(eintrag, name=None, lore=None, seltenheit=None, verzauberungen=None, farbe="gold"):
    """Ein Eintrag als besonderes Stück: eigener Name, Beschreibung, Seltenheit, Verzauberungen."""
    f = eintrag.setdefault("functions", [])
    if name:
        f.append({"function": "minecraft:set_name", "target": "custom_name",
                  "name": {"text": name, "italic": False, "color": farbe}})
    if lore:
        f.append({"function": "minecraft:set_lore", "mode": "append",
                  "lore": [{"text": z, "italic": True, "color": "gray"} for z in lore]})
    if seltenheit:
        f.append({"function": "minecraft:set_components", "components": {"minecraft:rarity": seltenheit}})
    if verzauberungen:
        f.append({"function": "minecraft:set_enchantments", "enchantments": verzauberungen})
    return eintrag


def tabelle(name, gewicht):
    return {"type": "minecraft:loot_table", "value": f"{MOD}:{name}", "weight": gewicht}


ESSEN_KLEIN = [
    item(m("konservendose"), 10, 1, 2),
    item("minecraft:bread", 3),
    item("minecraft:rotten_flesh", 5, 1, 2),
    item("minecraft:sugar", 2, 1, 2),
    item("minecraft:dried_kelp", 3, 1, 4),
    item("minecraft:cookie", 2, 1, 3),
]

TABELLEN = {

    # ---- neue Stadtgebäude ----
    "baumarkt": [pool(2, 5, [
        item("minecraft:oak_planks", 8, 4, 16), item("minecraft:spruce_planks", 6, 4, 16),
        item("minecraft:iron_ingot", 5, 1, 4), item("minecraft:iron_nugget", 6, 4, 12),
        item("minecraft:glass_pane", 5, 4, 12), item("minecraft:iron_bars", 4, 2, 8),
        item("minecraft:chain", 3, 2, 6), item("minecraft:lantern", 3, 1, 2), item("minecraft:torch", 5, 4, 12),
        item("minecraft:oak_door", 3), item("minecraft:iron_door", 1), item("minecraft:oak_fence", 4, 4, 10),
        item("minecraft:bricks", 3, 4, 16), item("minecraft:smooth_stone", 3, 4, 16),
        item(m("schrott"), 6, 2, 6), item(m("elektronik"), 3, 1, 2), item(m("brecheisen"), 2, schaden=(0.1, 0.5)),
        item("minecraft:shears", 2, schaden=(0.1, 0.5)), item("minecraft:iron_axe", 2, schaden=(0.2, 0.6)),
        item("minecraft:iron_shovel", 2, schaden=(0.2, 0.6)), item(m("stacheldraht"), 3, 2, 6),
        leer(4),
    ])],
    "militaer": [pool(2, 5, [
        waffe("g36k", 3), waffe("hk416d", 2), waffe("m4a1", 2), waffe("ak47", 2), waffe("hk_g3", 1), waffe("mk14", 1),
        waffe("m1014", 2), waffe("glock_17", 3),
        munition("556x45", 8, 15, 40), munition("762x39", 5, 15, 30), munition("308", 4, 8, 20), munition("12g", 4, 6, 16),
        munition("9mm", 5, 15, 30),
        item(m("verband"), 5, 2, 4), item(m("antibiotika"), 2), item(m("filter"), 3, 1, 2), item(m("gasmaske"), 2),
        item(m("nachtsichtgeraet"), 2), item(m("militaerpatrone"), 6, 5, 15), item(m("landmine"), 2, 1, 3),
        item("minecraft:iron_helmet", 2, schaden=(0.1, 0.4)), item("minecraft:iron_chestplate", 2, schaden=(0.1, 0.4)),
        tabelle("bonus/legendaer", 1),
    ]), notizen(25, ["funkspruch", "evakuierung"])],
    "bank": [pool(2, 4, [
        item("minecraft:gold_ingot", 8, 1, 4), item("minecraft:gold_nugget", 8, 3, 12), item("minecraft:emerald", 3, 1, 3),
        item("minecraft:diamond", 2), item(m("militaerpatrone"), 8, 4, 12), item(m("schatzkarte"), 4),
        item("minecraft:paper", 6, 4, 12), item("minecraft:clock", 2), item("minecraft:compass", 2),
        leer(3),
    ]), notizen(30, ["halden_flyer", "evakuierung", "brief"])],
    # Tresor: nur mit Brecheisen zu knacken – lohnt sich immer
    "tresor": [pool(3, 6, [
        item("minecraft:gold_ingot", 6, 2, 6), item("minecraft:diamond", 4, 1, 3), item("minecraft:emerald", 4, 2, 5),
        item(m("militaerpatrone"), 8, 10, 30), item(m("schatzkarte"), 4), item(m("antibiotika"), 3, 1, 3),
        item(m("nachtsichtgeraet"), 3), waffe("deagle", 2, voll=True), waffe("rhino357", 2, voll=True),
        munition("50ae", 3, 6, 14), munition("357mag", 3, 6, 14),
        tabelle("bonus/legendaer", 3),
    ])],
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
        item(m("schatzkarte"), 1),
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
    # ---- Städte (Lost Cities): welche Tabelle eine Kiste bekommt, steht in
    #      data/lostcities/lostcities/conditions/chestloot.json
    "stadt_wohnung": [pool(1, 4, ESSEN_KLEIN + [
        item("minecraft:paper", 4, 1, 3),
        item("minecraft:book", 2),
        item("minecraft:candle", 3, 1, 2),
        item(m("stoffreste"), 5, 1, 2),
        item(m("batterie"), 3),
        item(m("verband"), 3),
        item(m("schmerzmittel"), 1),
        item("minecraft:glass_bottle", 3, 1, 2),
        item("minecraft:leather_chestplate", 1, schaden=(0.2, 0.7)),
        item("minecraft:iron_nugget", 2, 1, 4),
        waffe("glock_17", 2), waffe("m1911", 1), waffe("db_short", 1),
        munition("9mm", 3, 4, 12), munition("45acp", 2, 3, 8), munition("12g", 2, 2, 6),
        leer(14),
    ]), notizen(15, ["tagebuch_1", "tagebuch_2", "abschied", "evakuierung", "einkaufszettel", "kinderzettel", "brief"])],
    "stadt_laden": [pool(2, 5, ESSEN_KLEIN + [
        item(m("konservendose"), 8, 1, 3),
        item(m("batterie"), 4, 1, 3),
        item(m("elektronik"), 3),
        item("minecraft:glass_bottle", 4, 1, 3),
        item("minecraft:honey_bottle", 1),
        item("minecraft:sweet_berries", 2, 1, 4),
        item(m("wodka"), 2),
        item(m("verband"), 2),
        leer(10),
    ]), notizen(8, ["halden_flyer", "einkaufszettel"])],
    "buero": [pool(1, 4, [
        item("minecraft:paper", 10, 2, 6),
        item("minecraft:book", 4),
        item("minecraft:writable_book", 1),
        item(m("batterie"), 4, 1, 2),
        item(m("elektronik"), 5, 1, 2),
        item("minecraft:clock", 1),
        item("minecraft:compass", 1),
        item("minecraft:map", 1),
        item(m("schatzkarte"), 2),
        item(m("konservendose"), 2),
        leer(10),
    ]), notizen(25, ["halden_flyer", "funkspruch", "evakuierung"])],
    "polizei": [pool(2, 4, [
        waffe("glock_17", 6), waffe("p320", 4), waffe("hk_mk23", 2), waffe("m870", 3), waffe("hk_mp5a5", 1),
        munition("9mm", 10, 6, 24), munition("45acp", 6, 4, 16), munition("12g", 6, 3, 10),
        item(m("verband"), 6, 1, 2),
        item("minecraft:chainmail_chestplate", 2, schaden=(0.1, 0.5)),
        item("minecraft:chainmail_helmet", 2, schaden=(0.1, 0.5)),
        item("minecraft:shield", 2, schaden=(0.2, 0.6)),
        item(m("batterie"), 4, 1, 2),
        leer(8),
    ]), notizen(20, ["funkspruch", "evakuierung"])],
    "waffenkammer": [pool(2, 4, [
        waffe("m1911", 4), waffe("rhino357", 2), waffe("deagle", 1), waffe("m870", 3), waffe("spas_12", 1),
        waffe("db_short", 3), waffe("db_long", 3), waffe("kar98", 2), waffe("m700", 2), waffe("sks_tactical", 2),
        munition("45acp", 6, 6, 20), munition("357mag", 3, 4, 12), munition("50ae", 2, 3, 8),
        munition("12g", 8, 4, 16), munition("792x57", 3, 4, 10), munition("30_06", 3, 3, 10), munition("762x39", 3, 5, 20),
        leer(6),
    ])],
    "apotheke": [pool(2, 4, [
        item(m("verband"), 12, 1, 3),
        item(m("schmerzmittel"), 8, 1, 2),
        item(m("antibiotika"), 4),
        item(m("anti_rad"), 3),
        item("minecraft:glass_bottle", 6, 1, 3),
        item("minecraft:honey_bottle", 2),
        item("minecraft:golden_carrot", 1),
        leer(8),
    ])],
    "bunker": [pool(2, 5, [
        item(m("gasmaske"), 2, schaden=(0.1, 0.5)),
        item(m("filter"), 5, 1, 2),
        item(m("geigerzaehler"), 1),
        item(m("anti_rad"), 4, 1, 2),
        item(m("konservendose"), 8, 1, 3),
        item(m("verband"), 5, 1, 2),
        item(m("batterie"), 4, 1, 3),
        item("minecraft:iron_helmet", 1, schaden=(0.2, 0.6)),
        waffe("g36k", 1), waffe("hk416d", 1), waffe("hk_g3", 1), waffe("ak47", 1),
        munition("556x45", 5, 10, 30), munition("308", 3, 6, 20), munition("762x39", 3, 10, 30),
        item(m("schatzkarte"), 2),
        leer(8),
    ]), notizen(30, ["funkspruch", "feuerwehr_einsatz"])],
    "versteck": [pool(3, 6, [
        item(m("konservendose"), 8, 2, 4),
        item(m("verband"), 6, 2, 4),
        item(m("antibiotika"), 3),
        item(m("schmerzmittel"), 4, 1, 2),
        item(m("anti_rad"), 3),
        item(m("filter"), 4, 1, 2),
        item(m("batterie"), 4, 2, 4),
        item(m("elektronik"), 4, 2, 5),
        item("minecraft:iron_ingot", 4, 2, 6),
        item("minecraft:diamond", 1),
        item("minecraft:iron_chestplate", 1, schaden=(0.1, 0.4)),
        item(m("schatzkarte"), 2),
        waffe("m1911", 2), waffe("m870", 2), waffe("sks_tactical", 1), waffe("hk_mp5a5", 1),
        munition("45acp", 3, 8, 20), munition("12g", 3, 6, 14), munition("762x39", 2, 10, 20), munition("9mm", 3, 10, 30),
    ]), notizen(40, ["tagebuch_2", "funkspruch", "abschied", "brief"])],
    # Versorgungsabwurf: lohnt sich, zieht aber eine Herde an
    "luftabwurf": [pool(2, 3, [
        waffe("g36k", 2, voll=True), waffe("hk416d", 2, voll=True), waffe("m4a1", 2, voll=True),
        waffe("mk14", 1, voll=True), waffe("m249", 1, voll=True), waffe("m1014", 2, voll=True),
        waffe("hk_mp5a5", 2, voll=True),
    ]), pool(3, 5, [
        munition("556x45", 6, 30, 60), munition("308", 4, 20, 40), munition("12g", 4, 12, 24),
        munition("9mm", 4, 30, 60), munition("762x39", 3, 30, 60),
    ]), pool(3, 6, [
        item(m("verband"), 8, 2, 5),
        item(m("antibiotika"), 5, 1, 2),
        item(m("schmerzmittel"), 5, 2, 4),
        item(m("anti_rad"), 5, 1, 3),
        item(m("filter"), 5, 1, 3),
        item(m("konservendose"), 8, 3, 6),
        item(m("militaerpatrone"), 6, 10, 25),
        item(m("bolzen"), 3, 4, 10),
        item(m("anomaliedetektor"), 1),
        item(m("gasmaske"), 2),
        item("minecraft:iron_chestplate", 2),
        item("minecraft:iron_helmet", 2),
    ])],
    # Station Null: Labor der Halden-Werke
    "station_null": [pool(2, 4, [
        item(m("anti_rad"), 6, 1, 3),
        item(m("gasmaske"), 3),
        item(m("filter"), 5, 2, 4),
        item(m("geigerzaehler"), 2),
        item(m("anomaliedetektor"), 2),
        item(m("bolzen"), 4, 6, 12),
        item(m("elektronik"), 6, 2, 6),
        item(m("batterie"), 5, 2, 4),
        item(m("militaerpatrone"), 6, 8, 20),
        item(m("artefakt_qualle"), 1),
        item(m("artefakt_steinblut"), 1),
        item("minecraft:diamond", 1, 1, 2),
        item(m("schatzkarte"), 2),
    ]), {"rolls": 1, "entries": [notiz("station_null_1"), notiz("station_null_2"), notiz("station_null_3"),
                                 notiz("station_null_4")]}],
    # Zuflucht: Vorräte der Bewohner (bescheiden)
    "zuflucht": [pool(1, 4, [
        item("minecraft:bread", 8, 1, 3),
        item("minecraft:wheat", 6, 2, 8),
        item("minecraft:potato", 6, 1, 4),
        item("minecraft:carrot", 4, 1, 4),
        item(m("konservendose"), 5, 1, 2),
        item(m("stoffreste"), 6, 1, 4),
        item(m("verband"), 3, 1, 2),
        item("minecraft:torch", 6, 2, 8),
        item("minecraft:oak_log", 5, 2, 6),
        item("minecraft:coal", 5, 1, 4),
        item(m("schrott"), 4, 1, 4),
        item(m("militaerpatrone"), 2, 1, 4),
        leer(6),
    ])],
    # Metro: Waggons und Lager in den Tunneln
    "metro": [pool(2, 5, [
        item(m("konservendose"), 10, 1, 3),
        item(m("verband"), 8, 1, 3),
        item(m("schmerzmittel"), 4, 1, 2),
        item(m("antibiotika"), 2),
        item(m("batterie"), 6, 1, 3),
        item(m("elektronik"), 5, 1, 3),
        item(m("schrott"), 6, 2, 5),
        item(m("militaerpatrone"), 5, 3, 10),
        item(m("filter"), 3),
        item(m("bolzen"), 2, 3, 8),
        item("minecraft:torch", 6, 2, 8),
        item("minecraft:bread", 4, 1, 3),
        waffe("m1911", 2), waffe("hk_mp5a5", 1), waffe("m870", 1),
        munition("9mm", 4, 8, 24), munition("45acp", 3, 6, 16), munition("12g", 3, 4, 10),
    ]), notizen(35, ["tagebuch_2", "funkspruch", "abschied", "evakuierung"])],
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

# Hauptgeschichte, Akt II: vier Funkprotokolle – Polizei, Krankenhaus/Apotheke, Militär, Waffenkammer/Bunker
for _t, _nr, _ch in (("polizei", 1, 30), ("apotheke", 2, 25), ("militaer", 3, 35), ("waffenkammer", 4, 35), ("bunker", 4, 25)):
    TABELLEN[_t].append(notizen(_ch, [f"funkprotokoll_{_nr}"]))


# ---- Bonus nach Gefahrenstufe (GefahrenBonus.java) und legendäre Stücke --------------------------
BONUS = {
    "gering": [{"rolls": 1, "entries": [
        item(m("konservendose"), 10, 1, 2), item(m("verband"), 8, 1, 2), item(m("batterie"), 6),
        item(m("schrott"), 8, 1, 3), item(m("elektronik"), 5), item(m("schmerzmittel"), 4),
        item(m("militaerpatrone"), 5, 1, 4), item(m("brecheisen"), 2, schaden=(0.2, 0.7)),
        item(m("stoffreste"), 6, 1, 3), munition("9mm", 4, 4, 12), munition("12g", 3, 2, 6),
    ]}],
    "mittel": [{"rolls": 1, "entries": [
        item(m("antibiotika"), 6), item(m("anti_rad"), 5), item(m("filter"), 5, 1, 2), item(m("gasmaske"), 2),
        item(m("nachtsichtgeraet"), 2), item(m("militaerpatrone"), 8, 3, 10), item(m("schatzkarte"), 3),
        waffe("glock_17", 3), waffe("m1911", 2), waffe("m870", 2), waffe("hk_mp5a5", 1),
        munition("9mm", 6, 10, 24), munition("45acp", 4, 6, 16), munition("12g", 5, 4, 12),
        item("minecraft:iron_chestplate", 2, schaden=(0.2, 0.6)), item("minecraft:iron_helmet", 2, schaden=(0.2, 0.6)),
        tabelle("bonus/artefakt", 2), tabelle("bonus/legendaer", 1),
    ]}],
    "hoch": [{"rolls": 1, "entries": [
        waffe("g36k", 3, voll=True), waffe("ak47", 3, voll=True), waffe("hk416d", 2, voll=True), waffe("m1014", 2, voll=True),
        waffe("kar98", 2, voll=True), munition("556x45", 6, 20, 45), munition("762x39", 5, 20, 40),
        munition("308", 3, 10, 20), item(m("nachtsichtgeraet"), 6), item("minecraft:diamond", 3, 1, 2),
        item(m("militaerpatrone"), 8, 8, 20), item(m("antibiotika"), 4, 1, 3), item(m("schatzkarte"), 4),
        tabelle("bonus/artefakt", 4), tabelle("bonus/legendaer", 6),
    ]}],
    "artefakt": [{"rolls": 1, "entries": [{"type": "minecraft:tag", "name": f"{MOD}:artefakte", "expand": True, "weight": 1}]}],
    "legendaer": [{"rolls": 1, "entries": [
        besonders(item("minecraft:diamond_chestplate", 3), "Panzerweste der Wacht",
                  ["Ein Einschussloch über dem Herzen.", "Der Träger hat es trotzdem nicht geschafft."], "epic",
                  {"minecraft:protection": 3, "minecraft:unbreaking": 3}),
        besonders(item("minecraft:iron_helmet", 3), "Stahlhelm der Wacht",
                  ["Innen eingeritzt: 23 Striche."], "epic", {"minecraft:protection": 4, "minecraft:unbreaking": 2}),
        besonders(item("minecraft:iron_sword", 3), "Letzter Ausweg",
                  ["Ein Kampfmesser. Die Klinge ist nie stumpf geworden."], "epic",
                  {"minecraft:sharpness": 4, "minecraft:looting": 2, "minecraft:unbreaking": 2}),
        besonders(item("minecraft:iron_axe", 2), "Feuerwehraxt von Löschzug 4",
                  ["Rote Farbe, die nicht nur Farbe ist."], "epic", {"minecraft:sharpness": 3, "minecraft:efficiency": 3}),
        besonders(item("minecraft:crossbow", 2), "Stille Nacht",
                  ["Kein Schuss, kein Lärm, kein Zeuge."], "epic", {"minecraft:quick_charge": 2, "minecraft:piercing": 3}),
        besonders(item("minecraft:leather_boots", 2), "Leise Sohlen",
                  ["Wer nicht gehört wird, wird nicht gejagt."], "epic", {"minecraft:feather_falling": 4, "minecraft:swift_sneak": 3}),
        besonders(item(m("nachtsichtgeraet"), 2), "Eule",
                  ["Militärisches Nachtsichtgerät, Restlicht-Verstärker der zweiten Generation."], "epic",
                  {"minecraft:unbreaking": 3}),
    ]}],
}


# Neue Materialien und Samen in passenden Behältern
ZUSATZ = {
    "werkzeug": [item(m("naegel"), 6, 2, 8), item(m("klebeband"), 4), item(m("brecheisen"), 1, schaden=(0.2, 0.6))],
    "werkstatt": [item(m("naegel"), 6, 2, 8), item(m("klebeband"), 5), item(m("gummi"), 4, 1, 2)],
    "baumarkt": [item(m("naegel"), 8, 4, 12), item(m("klebeband"), 6, 1, 2)],
    "autowrack": [item(m("gummi"), 6, 1, 2), item(m("klebeband"), 2)],
    "keller": [item(m("naegel"), 3, 2, 6), item(m("baumwollsamen"), 2, 1, 3)],
    "schuppen": [item(m("heilkraut_samen"), 4, 1, 3), item(m("baumwollsamen"), 4, 1, 3), item(m("naegel"), 3, 2, 5)],
    "scheune": [item(m("heilkraut_samen"), 4, 1, 3), item(m("baumwollsamen"), 5, 1, 3)],
    "kueche": [item(m("heilkraut"), 2, 1, 2)],
    "apotheke": [item(m("heilkraut"), 5, 1, 3), item(m("heilkraut_samen"), 2, 1, 2)],
    "stadt_laden": [item(m("taschenlampe"), 2)],
    "polizei": [item(m("taschenlampe"), 3), item(m("handgranate"), 1), item(m("rauchgranate"), 2)],
    "militaer": [item(m("handgranate"), 4, 1, 2), item(m("rauchgranate"), 3), item(m("blendgranate"), 3),
                 item(m("aufsatz_schalldaempfer"), 2), item(m("aufsatz_zielfernrohr"), 2), item(m("aufsatz_griff"), 2),
                 item(m("taktisch_brust"), 1, schaden=(0.2, 0.5)), item(m("taktisch_kopf"), 1, schaden=(0.2, 0.5))],
    "waffenkammer": [item(m("aufsatz_schalldaempfer"), 2), item(m("aufsatz_zielfernrohr"), 2), item(m("aufsatz_magazin"), 2),
                     item(m("aufsatz_taschenlampe"), 2)],
    "bunker": [item(m("schutzanzug_kopf"), 1, schaden=(0.2, 0.5)), item(m("schutzanzug_brust"), 1, schaden=(0.2, 0.5))],
    "versteck": [item(m("taschenlampe"), 2), item(m("handgranate"), 2)],
}
for _name, _eintraege in ZUSATZ.items():
    if _name in TABELLEN:
        TABELLEN[_name][0]["entries"].extend(_eintraege)

# Großzügiger als zu Beginn: Plündern soll sich lohnen (Rückmeldung nach dem ersten Testspiel).
GROSSZUEGIG = {"rolls_plus": 1, "rolls_faktor": 1.5, "leer_faktor": 0.4}
NICHT_ANHEBEN = {"luftabwurf", "station_null", "tresor"}


def anheben(name, pools):
    if name in NICHT_ANHEBEN:
        return
    for pl in pools:
        r = pl.get("rolls")
        if isinstance(r, dict) and r.get("type") == "minecraft:uniform":
            r["min"] = r["min"] + GROSSZUEGIG["rolls_plus"]
            r["max"] = round(r["max"] * GROSSZUEGIG["rolls_faktor"]) + GROSSZUEGIG["rolls_plus"]
            for e in pl["entries"]:
                if e.get("type") == "minecraft:empty":
                    e["weight"] = max(1, round(e["weight"] * GROSSZUEGIG["leer_faktor"]))


def main():
    for name, pools in TABELLEN.items():
        anheben(name, pools)
    if os.path.isdir(ZIEL):
        shutil.rmtree(ZIEL)
    os.makedirs(ZIEL, exist_ok=True)
    waffen_ziel = os.path.join(os.path.dirname(ZIEL), "waffen")
    if os.path.isdir(waffen_ziel):
        shutil.rmtree(waffen_ziel)
    for name, pools in TABELLEN.items():
        # Waffen und Munition (TaCZ) kommen in eine eigene Tabelle, die nur geladen wird, wenn TaCZ da ist
        for pl in pools:
            tacz = [e for e in pl["entries"] if e.get("name", "").startswith("tacz:")]
            if not tacz:
                continue
            pl["entries"] = [e for e in pl["entries"] if e not in tacz]
            pl["entries"].append({"type": "minecraft:loot_table", "value": f"{MOD}:waffen/{name}",
                                  "weight": sum(e["weight"] for e in tacz)})
            os.makedirs(waffen_ziel, exist_ok=True)
            with open(os.path.join(waffen_ziel, f"{name}.json"), "w", encoding="utf-8") as f:
                json.dump({"neoforge:conditions": [{"type": "neoforge:mod_loaded", "modid": "tacz"}],
                           "type": "minecraft:chest", "pools": [{"rolls": 1, "entries": tacz}]}, f, indent=2)
        with open(os.path.join(ZIEL, f"{name}.json"), "w", encoding="utf-8") as f:
            json.dump({"type": "minecraft:chest", "pools": pools}, f, indent=2, ensure_ascii=False)
            f.write("\n")
    bonus_ziel = os.path.join(os.path.dirname(ZIEL), "bonus")
    if os.path.isdir(bonus_ziel):
        shutil.rmtree(bonus_ziel)
    os.makedirs(bonus_ziel)
    for name, pools in BONUS.items():
        for pl in pools:
            tacz = [e for e in pl["entries"] if e.get("name", "").startswith("tacz:")]
            if tacz:
                pl["entries"] = [e for e in pl["entries"] if e not in tacz]
                pl["entries"].append({"type": "minecraft:loot_table", "value": f"{MOD}:waffen/bonus_{name}",
                                      "weight": sum(e["weight"] for e in tacz)})
                with open(os.path.join(waffen_ziel, f"bonus_{name}.json"), "w", encoding="utf-8") as f:
                    json.dump({"neoforge:conditions": [{"type": "neoforge:mod_loaded", "modid": "tacz"}],
                               "type": "minecraft:chest", "pools": [{"rolls": 1, "entries": tacz}]}, f, indent=2)
        with open(os.path.join(bonus_ziel, f"{name}.json"), "w", encoding="utf-8") as f:
            json.dump({"type": "minecraft:chest", "pools": pools}, f, indent=2, ensure_ascii=False)
    # Beute-Modifikator und Fund-Tag
    daten = os.path.dirname(os.path.dirname(ZIEL))
    os.makedirs(os.path.join(daten, "loot_modifiers"), exist_ok=True)
    with open(os.path.join(daten, "loot_modifiers", "gefahrenbonus.json"), "w") as f:
        json.dump({"type": f"{MOD}:gefahrenbonus", "conditions": []}, f, indent=1)
    neo = os.path.join(daten, "..", "neoforge", "loot_modifiers")
    os.makedirs(neo, exist_ok=True)
    with open(os.path.join(neo, "global_loot_modifiers.json"), "w") as f:
        json.dump({"replace": False, "entries": [f"{MOD}:gefahrenbonus"]}, f, indent=1)
    os.makedirs(os.path.join(daten, "tags", "item"), exist_ok=True)
    with open(os.path.join(daten, "tags", "item", "fund.json"), "w") as f:
        json.dump({"values": [f"#{MOD}:artefakte", "minecraft:diamond", f"{MOD}:schatzkarte", f"#{MOD}:schusswaffen"]}, f, indent=1)
    with open(os.path.join(daten, "tags", "item", "schusswaffen.json"), "w") as f:
        json.dump({"values": [f"{MOD}:{w}" for w in MAGAZIN]}, f, indent=1)
    with open(os.path.join(daten, "tags", "item", "munition.json"), "w") as f:
        json.dump({"values": [f"{MOD}:munition_{k}" for k in ("9mm", "357", "schrot", "556", "762", "rohr", "bolzen")]}, f, indent=1)
    print(f"{len(TABELLEN)} Beutetabellen, {len(BONUS)} Bonustabellen geschrieben.")


if __name__ == "__main__":
    main()
