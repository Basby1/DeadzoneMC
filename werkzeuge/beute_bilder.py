#!/usr/bin/env python3
"""Texturen und Modelle für Tresor, Brecheisen und Nachtsichtgerät.

  python3 werkzeuge/beute_bilder.py
"""
import json
import os
import random

from PIL import Image, ImageDraw

A = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "deadzonemc")


def c(s, a=255):
    return tuple(int(s[i:i + 2], 16) for i in (0, 2, 4)) + (a,)


def rauschen(b, basis, staerke, rng):
    for y in range(b.height):
        for x in range(b.width):
            v = rng.randint(-staerke, staerke)
            b.putpixel((x, y), tuple(max(0, min(255, basis[i] + v)) for i in range(3)) + (255,))


def tresor():
    rng = random.Random(3)
    seite = Image.new("RGBA", (16, 16))
    rauschen(seite, c("4a4e52"), 6, rng)
    d = ImageDraw.Draw(seite)
    d.rectangle((0, 0, 15, 15), outline=c("2e3134"))
    for x, y in ((1, 1), (14, 1), (1, 14), (14, 14)):
        seite.putpixel((x, y), c("8a8e92"))
    vorne = seite.copy()
    d = ImageDraw.Draw(vorne)
    d.rectangle((2, 2, 13, 13), outline=c("2a2c2e"), fill=c("55595d"))
    d.ellipse((5, 4, 10, 9), outline=c("1e1e1e"), fill=c("8a8e92"))  # Drehrad
    vorne.putpixel((7, 6), c("1e1e1e"))
    vorne.putpixel((8, 7), c("1e1e1e"))
    d.rectangle((10, 10, 12, 11), fill=c("b8a040"))  # Griff
    for _ in range(6):
        x, y = rng.randrange(2, 14), rng.randrange(2, 14)
        vorne.putpixel((x, y), c("6a3a1a"))
    offen = seite.copy()
    d = ImageDraw.Draw(offen)
    d.rectangle((2, 2, 13, 13), fill=c("141414"))
    d.rectangle((2, 8, 13, 8), fill=c("3a3a3a"))
    for x in (4, 9):
        d.rectangle((x, 10, x + 2, 12), fill=c("c8a83a"))  # Goldbarren
    d.line((2, 2, 2, 13), fill=c("8a8e92"))
    oben = Image.new("RGBA", (16, 16))
    rauschen(oben, c("42464a"), 5, rng)
    ImageDraw.Draw(oben).rectangle((0, 0, 15, 15), outline=c("2e3134"))
    p = os.path.join(A, "textures", "block")
    os.makedirs(p, exist_ok=True)
    for n, b in (("tresor_seite", seite), ("tresor_vorne", vorne), ("tresor_offen", offen), ("tresor_oben", oben)):
        b.save(os.path.join(p, n + ".png"))
    m = os.path.join(A, "models", "block")
    for n, front in (("tresor", "tresor_vorne"), ("tresor_offen", "tresor_offen")):
        with open(os.path.join(m, n + ".json"), "w") as f:
            json.dump({"parent": "minecraft:block/orientable", "textures": {
                "front": f"deadzonemc:block/{front}", "side": "deadzonemc:block/tresor_seite",
                "top": "deadzonemc:block/tresor_oben"}}, f, indent=1)
    v = {}
    for offen_ in ("false", "true"):
        for facing, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
            e = {"model": "deadzonemc:block/tresor" + ("_offen" if offen_ == "true" else "")}
            if y:
                e["y"] = y
            v[f"facing={facing},offen={offen_}"] = e
    with open(os.path.join(A, "blockstates", "tresor.json"), "w") as f:
        json.dump({"variants": v}, f, indent=1)
    with open(os.path.join(A, "models", "item", "tresor.json"), "w") as f:
        json.dump({"parent": "deadzonemc:block/tresor"}, f, indent=1)


def item_bild(name, pixel, farben):
    b = Image.new("RGBA", (16, 16))
    for y, z in enumerate(pixel):
        for x, ch in enumerate(z):
            if ch != ".":
                b.putpixel((x, y), c(farben[ch]))
    b.save(os.path.join(A, "textures", "item", name + ".png"))
    with open(os.path.join(A, "models", "item", name + ".json"), "w") as f:
        json.dump({"parent": "minecraft:item/handheld" if name == "brecheisen" else "minecraft:item/generated",
                   "textures": {"layer0": f"deadzonemc:item/{name}"}}, f, indent=1)


BRECHEISEN = [
    "............kk..",
    "...........krrk.",
    "...........kr.k.",
    "..........krk...",
    ".........krk....",
    "........krk.....",
    ".......krk......",
    "......krk.......",
    ".....krk........",
    "....krk.........",
    "...krk..........",
    "..krk...........",
    ".krrk...........",
    ".krrrk..........",
    "..kkrk..........",
    "....k...........",
]
NACHTSICHT = [
    "................",
    "................",
    "...kkkkkkkkkk...",
    "..kddddddddddk..",
    "..kdggdddggdk...",
    ".kdgGGgdgGGgdk..",
    ".kdgGgggdgGggdk.",
    ".kdgggg.dggggdk.",
    "..kdggdd.dggdk..",
    "..skkkk...kkkks.",
    ".ss...........ss",
    "ss.............s",
    "................",
    "................",
    "................",
    "................",
]


def nachtsicht_ruestung():
    """Rüstungsschicht: Band um den Kopf, zwei Röhren vor den Augen."""
    b = Image.new("RGBA", (64, 32))
    d = ImageDraw.Draw(b)
    # Kopf: vorne x8-15, rechts 0-7, links 16-23, hinten 24-31, y 8-15
    d.rectangle((0, 10, 31, 11), fill=c("2a2a26"))
    d.rectangle((9, 10, 11, 12), fill=c("1e2a1e"))
    d.rectangle((12, 10, 14, 12), fill=c("1e2a1e"))
    b.putpixel((10, 11), c("6aff6a"))
    b.putpixel((13, 11), c("6aff6a"))
    d.rectangle((8, 0, 15, 7), fill=None)
    p = os.path.join(A, "textures", "models", "armor")
    os.makedirs(p, exist_ok=True)
    b.save(os.path.join(p, "nachtsicht_layer_1.png"))
    Image.new("RGBA", (64, 32)).save(os.path.join(p, "nachtsicht_layer_2.png"))


def main():
    for p in ("blockstates", "models/block", "models/item", "textures/item"):
        os.makedirs(os.path.join(A, p), exist_ok=True)
    tresor()
    item_bild("brecheisen", BRECHEISEN, {"k": "1e1414", "r": "a82a1e"})
    item_bild("nachtsichtgeraet", NACHTSICHT, {"k": "1a1a18", "d": "3a4032", "g": "2e5a2e", "G": "7aff7a", "s": "2a2a26"})
    nachtsicht_ruestung()
    print("Tresor, Brecheisen, Nachtsichtgerät")


if __name__ == "__main__":
    main()
