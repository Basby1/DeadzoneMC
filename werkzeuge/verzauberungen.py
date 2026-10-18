"""Zonen-Verzauberungen: 26 eigene Verzauberungen, die es nur an der Zonenwerkbank gibt (nicht im Vanilla-Tisch,
nicht in Truhen, nicht beim Händler). Bezahlt wird mit Erfahrung und Material aus der Zone.

  python3 werkzeuge/verzauberungen.py
schreibt data/deadzonemc/enchantment/*.json, die Gegenstands-Tags, den Tag deadzonemc:zonentisch,
Block-Modell/Textur/Rezept/Beute der Werkbank und werkzeuge/verzauberungen_texte.json (für texte.py).
Die Wirkungen ohne Vanilla-Komponente stehen in Java (de.deadzonemc.verzauberung.Zonenverzauberung).
"""
import json
import os

import numpy as np
from PIL import Image

WURZEL = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(WURZEL, "src", "main", "resources")
D = os.path.join(RES, "data", "deadzonemc")
A = os.path.join(RES, "assets", "deadzonemc")
MOD = "deadzonemc"

WAFFEN = ["p9", "revolver", "mp9", "pumpgun", "doppelflinte", "sturmgewehr", "kalaschnikow", "jagdgewehr",
          "praezisionsgewehr", "mg", "rohrpistole", "rohrgewehr", "armbrust"]
NAHKAMPF = ["kampfmesser", "machete", "baseballschlaeger", "nagelschlaeger", "feuerwehraxt", "vorschlaghammer",
            "speer", "knochenkeule", "mutantenklinge", "rostiges_messer", "brecheisen"]
SAETZE = ["pluenderer", "schrottpanzer", "taktisch", "sanitaeter", "schutzanzug", "tarnanzug", "mutantenpanzer"]
TEIL = {"kopf": "head", "brust": "chest", "beine": "leg", "stiefel": "foot"}


def lin(basis, pro=None):
    return {"type": "minecraft:linear", "base": basis, "per_level_above_first": basis if pro is None else pro}


def attribut(name, attr, menge, op="add_value", slot_name=None):
    return {"minecraft:attributes": [{"id": f"{MOD}:verzauberung.{name}", "attribute": attr, "amount": menge,
                                      "operation": op}]}


# id: (Name, Beschreibung, Ziel-Tag, Slots, Max-Stufe, Material, Effekte)
VERZ = {
    # ---- Schusswaffen ----
    "kopfjaeger": ("Kopfjäger", "+20 % Kopfschuss-Schaden je Stufe", "schusswaffe", ["mainhand"], 3, "strahlenkristall", {}),
    "hohlspitz": ("Hohlspitz", "+12 % Schaden je Stufe", "schusswaffe", ["mainhand"], 3, "knochensplitter", {}),
    "durchschlag": ("Durchschlag", "Die Kugel durchschlägt je Stufe ein weiteres Ziel", "schusswaffe", ["mainhand"], 2,
                    "panzerplatte", {}),
    "lautlos": ("Lautlos", "Schüsse 25 % leiser je Stufe – lockt weniger Infizierte an", "schusswaffe", ["mainhand"], 3,
                "gummi", {}),
    "ruhige_hand": ("Ruhige Hand", "-15 % Streuung je Stufe", "schusswaffe", ["mainhand"], 3, "sehne", {}),
    "schnellladen": ("Schnellladen", "-15 % Nachladezeit je Stufe", "schusswaffe", ["mainhand"], 3, "klebeband", {}),
    "trommel": ("Trommelmagazin", "+25 % Magazin je Stufe", "schusswaffe", ["mainhand"], 2, "drohnenteile", {}),
    "brandmunition": ("Brandmunition", "Treffer setzen das Ziel 3 Sekunden je Stufe in Brand", "schusswaffe",
                      ["mainhand"], 2, "glutkern", {}),
    # ---- Nahkampf ----
    "wucht": ("Wucht", "Schläge werfen Gegner zurück", "nahkampf", ["mainhand"], 2, "panzerplatte",
              {"minecraft:knockback": [{"effect": {"type": "minecraft:add", "value": lin(1)}}]}),
    "schaedelbrecher": ("Schädelbrecher", "+2,5 Schaden gegen Infizierte und Mutanten je Stufe", "nahkampf", ["mainhand"],
                        5, "knochensplitter",
                        {"minecraft:damage": [{"requirements": {"condition": "minecraft:entity_properties", "entity": "this",
                                                                 "predicate": {"type": f"#{MOD}:monster"}},
                                               "effect": {"type": "minecraft:add", "value": lin(2.5)}}]}),
    "blutrausch": ("Blutrausch", "Jeder Kill heilt ein Herz je Stufe", "nahkampf", ["mainhand"], 3, "verseuchtes_gewebe", {}),
    "leise_hand": ("Leise Hand", "Geduckte Angriffe von hinten +40 % Schaden je Stufe", "nahkampf", ["mainhand"], 3,
                   "mottenstaub", {}),
    # ---- Alles mit Haltbarkeit ----
    "robust": ("Robust", "Nutzt sich langsamer ab (auch Schusswaffen)", "haltbar", ["any"], 3, "naegel",
               {"minecraft:item_damage": [{"effect": {"type": "minecraft:remove_binomial", "chance": {
                   "type": "minecraft:fraction", "numerator": lin(2.0), "denominator": lin(10.0, 5.0)}}}]}),
    # ---- Brust ----
    "zaehigkeit": ("Zähigkeit", "+1 Herz maximale Gesundheit je Stufe", "brust", ["chest"], 3, "mutantenhaut",
                   attribut("zaehigkeit", "minecraft:generic.max_health", lin(2.0))),
    "dornen_der_zone": ("Dornen der Zone", "Wer dich im Nahkampf trifft, nimmt 1,5 Schaden je Stufe", "brust", ["chest"], 3,
                        "chitinplatte", {}),
    "abwehrkraefte": ("Abwehrkräfte", "25 % je Stufe: Gift, Hunger und Verwesung prallen ab", "brust", ["chest"], 3,
                      "heilkraut", {}),
    # ---- ganze Rüstung ----
    "strahlenschutz": ("Strahlenschutz", "-8 % Strahlung je Stufe, über alle Teile zusammengezählt", "ruestung",
                       ["armor"], 4, "gummi", {}),
    # ---- Beine ----
    "schleicher": ("Schleicher", "Geduckt bemerken dich Monster erst aus der Nähe", "beine", ["legs"], 3, "mottenstaub", {}),
    "fluchtinstinkt": ("Fluchtinstinkt", "Unter 30 % Gesundheit: kurzer Tempo-Schub (alle 60 s)", "beine", ["legs"], 2,
                       "sehne", {}),
    # ---- Füße ----
    "leiser_lauf": ("Leiser Lauf", "Geduckt 15 % schneller je Stufe", "fuesse", ["feet"], 3, "baumwolle",
                    attribut("leiser_lauf", "minecraft:player.sneaking_speed", lin(0.15))),
    "polsterung": ("Polsterung", "Weniger Fallschaden", "fuesse", ["feet"], 4, "sporenbeutel",
                   {"minecraft:damage_protection": [{"requirements": {"condition": "minecraft:damage_source_properties",
                                                                      "predicate": {"tags": [
                                                                          {"id": "minecraft:is_fall", "expected": True},
                                                                          {"id": "minecraft:bypasses_invulnerability",
                                                                           "expected": False}]}},
                                                     "effect": {"type": "minecraft:add", "value": lin(3.0)}}]}),
    "kletterer": ("Kletterer", "Steigt ganze Blöcke ohne Springen hoch", "fuesse", ["feet"], 1, "sehne",
                  attribut("kletterer", "minecraft:generic.step_height", 0.5)),
    # ---- Kopf ----
    "dunkelsicht": ("Dunkelsicht", "Geduckt im Dunkeln siehst du wie mit Restlicht", "kopf", ["head"], 1,
                    "strahlenkristall", {}),
    "vorahnung": ("Vorahnung", "Monster, die dich jagen, leuchten durch Wände (8 Blöcke je Stufe)", "kopf", ["head"], 2,
                  "mottenstaub", {}),
    "ruhepuls": ("Ruhepuls", "Außerhalb des Kampfes heilst du schneller", "kopf", ["head"], 3, "heilkraut", {}),
    "glueckspilz": ("Glückspilz", "Mehr Beute in Behältern", "kopf", ["head"], 3, "mutagen", {}),
}

TAGS = {
    "schusswaffe": [f"{MOD}:{w}" for w in WAFFEN],
    "nahkampf": ["#minecraft:swords", "#minecraft:axes"] + [f"{MOD}:{w}" for w in NAHKAMPF],
    "kopf": ["#minecraft:head_armor"], "brust": ["#minecraft:chest_armor"], "beine": ["#minecraft:leg_armor"],
    "fuesse": ["#minecraft:foot_armor"],
    "ruestung": ["#minecraft:head_armor", "#minecraft:chest_armor", "#minecraft:leg_armor", "#minecraft:foot_armor"],
    "haltbar": ["#minecraft:enchantable/durability", f"#{MOD}:verzauberbar/schusswaffe", f"#{MOD}:verzauberbar/nahkampf"],
}


def schreiben(pfad, daten):
    os.makedirs(os.path.dirname(pfad), exist_ok=True)
    with open(pfad, "w", encoding="utf-8") as f:
        json.dump(daten, f, indent=1, ensure_ascii=False)


def textur(name, art):
    """16x16: Stahlplatte mit Werkbankfläche, grünlich leuchtende Kristalladern."""
    r = np.random.default_rng(sum(map(ord, name)))
    b = np.zeros((16, 16, 4), np.uint8)
    b[..., 3] = 255
    stahl = np.array([74, 78, 76])
    for y in range(16):
        for x in range(16):
            n = r.integers(-9, 10)
            b[y, x, :3] = np.clip(stahl + n, 0, 255)
    if art == "oben":
        b[1:15, 1:15, :3] = np.clip(np.array([96, 74, 52]) + r.integers(-10, 10, (14, 14, 1)), 0, 255)  # Holzplatte
        for x in range(1, 15):
            if x % 4 == 0:
                b[1:15, x, :3] = (70, 52, 36)
        for (y, x) in [(5, 6), (6, 7), (7, 7), (8, 8), (9, 9), (10, 9)]:
            b[y, x, :3] = (120, 230, 120)  # Kristallsplitter
        b[3, 11, :3] = (180, 255, 170)
        b[12, 3, :3] = (140, 210, 255)
    else:
        b[0, :, :3] = (40, 42, 40)
        b[15, :, :3] = (30, 32, 30)
        b[:, 0, :3] = (46, 48, 46)
        b[:, 15, :3] = (46, 48, 46)
        for x in (2, 13):
            for y in (2, 13):
                b[y, x, :3] = (130, 134, 130)  # Nieten
        if art == "vorne":
            b[5:11, 4:12, :3] = (24, 26, 24)
            for x in range(5, 11):
                b[8, x, :3] = (90, 220, 110) if x % 2 else (60, 160, 80)
            b[6, 6, :3] = (230, 200, 80)
    Image.fromarray(b, "RGBA").save(os.path.join(A, "textures", "block", f"zonenwerkbank_{art}.png"))


def main():
    texte = {}
    zonentisch = []
    for vid, (name, besch, ziel, slots, maxs, mat, effekte) in VERZ.items():
        d = {"description": {"translate": f"enchantment.{MOD}.{vid}"},
             "supported_items": f"#{MOD}:verzauberbar/{ziel}",
             "weight": 5, "max_level": maxs,
             "min_cost": {"base": 10, "per_level_above_first": 10}, "max_cost": {"base": 40, "per_level_above_first": 10},
             "anvil_cost": 4, "slots": slots}
        if effekte:
            eff = json.loads(json.dumps(effekte))
            for k, v in eff.items():
                if k == "minecraft:attributes":
                    for a in v:
                        if not isinstance(a["amount"], dict):
                            a["amount"] = lin(a["amount"])
            d["effects"] = eff
        schreiben(os.path.join(D, "enchantment", f"{vid}.json"), d)
        zonentisch.append(f"{MOD}:{vid}")
        texte[f"enchantment.{MOD}.{vid}"] = name
        texte[f"enchantment.{MOD}.{vid}.desc"] = besch
    schreiben(os.path.join(D, "tags", "enchantment", "zonentisch.json"), {"values": zonentisch})
    for n, werte in TAGS.items():
        schreiben(os.path.join(D, "tags", "item", "verzauberbar", f"{n}.json"), {"values": werte})
    # eigene Rüstung in die Vanilla-Rüstungstags (damit Rüstungs-Verzauberungen passen)
    for teil, mc in TEIL.items():
        schreiben(os.path.join(RES, "data", "minecraft", "tags", "item", f"{mc}_armor.json"),
                  {"replace": False, "values": [f"{MOD}:{s}_{teil}" for s in SAETZE]})
    # Block
    os.makedirs(os.path.join(A, "textures", "block"), exist_ok=True)
    for art in ("oben", "seite", "vorne"):
        textur("zonenwerkbank_" + art, art)
    schreiben(os.path.join(A, "models", "block", "zonenwerkbank.json"), {
        "parent": "minecraft:block/orientable_with_bottom",
        "textures": {"top": f"{MOD}:block/zonenwerkbank_oben", "bottom": f"{MOD}:block/zonenwerkbank_seite",
                     "side": f"{MOD}:block/zonenwerkbank_seite", "front": f"{MOD}:block/zonenwerkbank_vorne"}})
    schreiben(os.path.join(A, "models", "item", "zonenwerkbank.json"), {"parent": f"{MOD}:block/zonenwerkbank"})
    schreiben(os.path.join(A, "blockstates", "zonenwerkbank.json"), {"variants": {
        f"facing={f}": {"model": f"{MOD}:block/zonenwerkbank", **({"y": y} if y else {})}
        for f, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270))}})
    schreiben(os.path.join(D, "loot_table", "blocks", "zonenwerkbank.json"), {
        "type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"{MOD}:zonenwerkbank"}],
                                              "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
    schreiben(os.path.join(D, "recipe", "zonenwerkbank.json"), {
        "type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["KMK", "PWP", "III"],
        "key": {"K": {"item": f"{MOD}:strahlenkristall"}, "M": {"item": f"{MOD}:mutagen"},
                "P": {"item": f"{MOD}:panzerplatte"}, "W": {"item": "minecraft:crafting_table"},
                "I": {"item": "minecraft:iron_ingot"}},
        "result": {"id": f"{MOD}:zonenwerkbank", "count": 1}})
    texte.update({
        "block.deadzonemc.zonenwerkbank": "Zonenwerkbank",
        "container.deadzonemc.zonenwerkbank": "Zonenwerkbank",
        "verzauberung.deadzonemc.hinweis": "Gegenstand links oben, Material darunter",
        "verzauberung.deadzonemc.keine": "Für diesen Gegenstand gibt es hier nichts.",
        "verzauberung.deadzonemc.kosten": "%s Stufen · %sx %s",
        "verzauberung.deadzonemc.voll": "Höchste Stufe",
        "verzauberung.deadzonemc.unvertraeglich": "Verträgt sich nicht",
        "verzauberung.deadzonemc.fertig": "%s verbessert",
    })
    with open(os.path.join(WURZEL, "werkzeuge", "verzauberungen_texte.json"), "w", encoding="utf-8") as f:
        json.dump(texte, f, indent=1, ensure_ascii=False)
    print(f"{len(VERZ)} Verzauberungen geschrieben")


if __name__ == "__main__":
    main()
