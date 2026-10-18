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


def waffe(name, gewicht, voll=False):
    """Eine Schusswaffe, meist mit fast leerem Magazin."""
    munition, magazin = WAFFEN[name]
    schuss = magazin if voll else max(0, magazin // 4)
    return {"type": "minecraft:item", "name": "tacz:modern_kinetic_gun", "weight": gewicht,
            "functions": [{"function": "minecraft:set_custom_data",
                           "tag": f'{{GunId:"tacz:{name}",GunFireMode:"SEMI",GunCurrentAmmoCount:{schuss}}}'},
                          {"function": "minecraft:set_count", "count": 1}]}


def munition(art, gewicht, minimum, maximum):
    return {"type": "minecraft:item", "name": "tacz:ammo", "weight": gewicht,
            "functions": [{"function": "minecraft:set_custom_data", "tag": f'{{AmmoId:"tacz:{art}"}}'},
                          {"function": "minecraft:set_count",
                           "count": {"type": "minecraft:uniform", "min": minimum, "max": maximum}}]}


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
        waffe("glock_17", 1), munition("9mm", 1, 3, 10),
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
    print(f"{len(TABELLEN)} Beutetabellen geschrieben.")


if __name__ == "__main__":
    main()
