"""Texturen, Modelle, Rezepte und Beutetabellen für Glasscherben, Spießgrube, Brandgraben und Motorhaube.

  python3 werkzeuge/fallen2.py
"""
import json
import os

import numpy as np
from PIL import Image

WURZEL = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources")
A = os.path.join(WURZEL, "assets", "deadzonemc")
D = os.path.join(WURZEL, "data", "deadzonemc")
rng = np.random.default_rng(7)


def hexc(s, a=255):
    return [int(s[i:i + 2], 16) for i in (0, 2, 4)] + [a]


def speichern(name, arr):
    os.makedirs(os.path.join(A, "textures", "block"), exist_ok=True)
    Image.fromarray(np.clip(arr, 0, 255).astype(np.uint8), "RGBA").save(os.path.join(A, "textures", "block", name + ".png"))


def rauschen(basis, streu, a=255):
    b = np.array(hexc(basis, a), float)
    x = np.tile(b, (16, 16, 1))
    x[..., :3] += rng.normal(0, streu, (16, 16, 1))
    return x


def glasscherben():
    x = np.zeros((16, 16, 4))
    for _ in range(14):
        cx, cy = rng.integers(1, 15, 2)
        laenge = rng.integers(1, 4)
        d = rng.choice([(1, 0), (0, 1), (1, 1), (1, -1)])
        for i in range(laenge):
            px, py = cx + d[0] * i, cy + d[1] * i
            if 0 <= px < 16 and 0 <= py < 16:
                hell = rng.integers(170, 235)
                x[py, px] = [hell - 10, hell + 10, hell + 20, 210]
        x[cy, cx] = [235, 245, 255, 230]
    speichern("glasscherben", x)


def spiessfalle():
    x = np.zeros((16, 16, 4))
    for sx in (2, 6, 9, 13):
        oben = rng.integers(2, 5)
        for y in range(oben, 16):
            breite = 0 if y < oben + 2 else 1
            for dx in range(-breite, breite + 1):
                px = sx + dx
                if 0 <= px < 16:
                    if y < oben + 2:
                        c = [200, 180, 140, 255]  # helle angespitzte Spitze
                    else:
                        c = [110 + rng.integers(-12, 12), 80, 48, 255]
                    if y in (oben + 1, oben + 2) and rng.random() < 0.5:
                        c = [120, 30, 25, 255]  # Blut an der Spitze
                    x[y, px] = c
    for y in (11, 12):  # Querbrett
        for px in range(1, 15):
            x[y, px] = [92 + rng.integers(-8, 8), 66, 40, 255]
    speichern("spiessfalle", x)


def brandgraben():
    rand = rauschen("6e6c68", 10)
    rand[2:14, 2:14] = rauschen("1e1a18", 8)[2:14, 2:14]
    for _ in range(18):  # Holzkohle und Scheite
        px, py = rng.integers(3, 13, 2)
        rand[py, px, :3] = [60 + rng.integers(0, 20), 46, 36]
    speichern("brandgraben_oben", rand)
    an = rand.copy()
    for _ in range(40):
        px, py = rng.integers(2, 14, 2)
        t = rng.random()
        an[py, px, :3] = [255, 120 + 120 * t, 30 * t]
    speichern("brandgraben_oben_an", an)
    seite = rauschen("6e6c68", 10)
    for y in range(16):
        if y % 5 == 4:
            seite[y, :, :3] *= 0.75
    speichern("brandgraben_seite", seite)


def motorraum():
    oben = rauschen("5a5e62", 6)
    for px in range(16):
        oben[3, px, :3] *= 0.7
        oben[12, px, :3] *= 0.7
    for y in range(16):
        oben[y, 7:9, :3] *= 0.88  # Mittelsicke
    for _ in range(10):  # Rost
        px, py = rng.integers(0, 16, 2)
        oben[py, px, :3] = [130 + rng.integers(0, 30), 70, 40]
    speichern("motorraum", oben)
    offen = rauschen("1c1c1e", 6)
    for y in range(3, 13):
        offen[y, 3:13, :3] = [60, 60, 64]  # Motorblock
    for px in range(4, 12, 2):
        offen[5:11, px, :3] = [90, 90, 96]  # Zylinder
    offen[4, 4:12, :3] = [130, 60, 30]  # Schlauch
    speichern("motorraum_offen", offen)
    seite = rauschen("4a4e52", 6)
    seite[:8] = 0
    seite[8:, :, :3] = np.array(seite[8:, :, :3])
    seite[8:, :, 3] = 255
    speichern("motorraum_seite", seite)


def jdump(pfad, obj):
    os.makedirs(os.path.dirname(pfad), exist_ok=True)
    with open(pfad, "w", encoding="utf-8") as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)


def modelle():
    m = os.path.join(A, "models")
    jdump(os.path.join(m, "block", "glasscherben.json"), {
        "parent": "minecraft:block/carpet", "render_type": "minecraft:cutout",
        "textures": {"wool": "deadzonemc:block/glasscherben", "particle": "deadzonemc:block/glasscherben"}})
    jdump(os.path.join(m, "block", "spiessfalle.json"), {
        "parent": "minecraft:block/cross", "render_type": "minecraft:cutout",
        "textures": {"cross": "deadzonemc:block/spiessfalle", "particle": "deadzonemc:block/spiessfalle"}})
    for name, oben in (("brandgraben", "brandgraben_oben"), ("brandgraben_an", "brandgraben_oben_an")):
        jdump(os.path.join(m, "block", name + ".json"), {
            "parent": "minecraft:block/block",
            "textures": {"oben": f"deadzonemc:block/{oben}", "seite": "deadzonemc:block/brandgraben_seite",
                         "particle": "deadzonemc:block/brandgraben_seite"},
            "elements": [{"from": [0, 0, 0], "to": [16, 6, 16], "faces": {
                "up": {"texture": "#oben"}, "down": {"texture": "#seite", "cullface": "down"},
                "north": {"uv": [0, 10, 16, 16], "texture": "#seite", "cullface": "north"},
                "south": {"uv": [0, 10, 16, 16], "texture": "#seite", "cullface": "south"},
                "west": {"uv": [0, 10, 16, 16], "texture": "#seite", "cullface": "west"},
                "east": {"uv": [0, 10, 16, 16], "texture": "#seite", "cullface": "east"}}}]})
    for name, oben in (("motorraum", "motorraum"), ("motorraum_offen", "motorraum_offen")):
        jdump(os.path.join(m, "block", name + ".json"), {
            "parent": "minecraft:block/slab", "render_type": "minecraft:cutout",
            "textures": {"bottom": "deadzonemc:block/motorraum_seite", "top": f"deadzonemc:block/{oben}",
                         "side": "deadzonemc:block/motorraum_seite"}})
    jdump(os.path.join(m, "item", "glasscherben.json"), {"parent": "minecraft:item/generated",
                                                          "textures": {"layer0": "deadzonemc:block/glasscherben"}})
    jdump(os.path.join(m, "item", "spiessfalle.json"), {"parent": "minecraft:item/generated",
                                                         "textures": {"layer0": "deadzonemc:block/spiessfalle"}})
    jdump(os.path.join(m, "item", "brandgraben.json"), {"parent": "deadzonemc:block/brandgraben"})
    jdump(os.path.join(m, "item", "motorraum.json"), {"parent": "deadzonemc:block/motorraum"})
    b = os.path.join(A, "blockstates")
    jdump(os.path.join(b, "glasscherben.json"), {"variants": {"": {"model": "deadzonemc:block/glasscherben"}}})
    jdump(os.path.join(b, "spiessfalle.json"), {"variants": {"": {"model": "deadzonemc:block/spiessfalle"}}})
    jdump(os.path.join(b, "brandgraben.json"), {"variants": {"lit=false": {"model": "deadzonemc:block/brandgraben"},
                                                             "lit=true": {"model": "deadzonemc:block/brandgraben_an"}}})
    jdump(os.path.join(b, "motorraum.json"), {"variants": {"offen=false": {"model": "deadzonemc:block/motorraum"},
                                                           "offen=true": {"model": "deadzonemc:block/motorraum_offen"}}})


def daten():
    for name in ("glasscherben", "spiessfalle", "brandgraben"):
        jdump(os.path.join(D, "loot_table", "blocks", name + ".json"), {
            "type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"deadzonemc:{name}"}],
                                                  "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
    jdump(os.path.join(D, "loot_table", "blocks", "motorraum.json"), {
        "type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": "deadzonemc:schrott"}]}]})
    r = os.path.join(D, "recipe")
    jdump(os.path.join(r, "glasscherben.json"), {
        "type": "minecraft:crafting_shapeless", "category": "misc",
        "ingredients": [{"item": "minecraft:glass_pane"}, {"item": "minecraft:glass_pane"}],
        "result": {"id": "deadzonemc:glasscherben", "count": 6}})
    jdump(os.path.join(r, "spiessfalle.json"), {
        "type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["S S", "SNS", "PPP"],
        "key": {"S": {"item": "minecraft:stick"}, "N": {"item": "minecraft:iron_nugget"}, "P": {"tag": "minecraft:planks"}},
        "result": {"id": "deadzonemc:spiessfalle", "count": 2}})
    jdump(os.path.join(r, "brandgraben.json"), {
        "type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["K K", "CKC"],
        "key": {"C": {"item": "minecraft:cobblestone"}, "K": {"tag": "minecraft:coals"}},
        "result": {"id": "deadzonemc:brandgraben", "count": 2}})
    # Abbau-Werkzeuge
    for tag, neu in (("mineable/pickaxe", ["deadzonemc:brandgraben", "deadzonemc:motorraum"]),
                     ("mineable/axe", ["deadzonemc:spiessfalle"])):
        pfad = os.path.join(WURZEL, "data", "minecraft", "tags", "block", tag + ".json")
        alt = json.load(open(pfad)) if os.path.exists(pfad) else {"replace": False, "values": []}
        for n in neu:
            if n not in alt["values"]:
                alt["values"].append(n)
        jdump(pfad, alt)


if __name__ == "__main__":
    glasscherben()
    spiessfalle()
    brandgraben()
    motorraum()
    modelle()
    daten()
    print("Fallen und Motorhaube erzeugt.")
