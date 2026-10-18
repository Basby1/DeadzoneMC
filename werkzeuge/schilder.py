#!/usr/bin/env python3
"""Ladenschilder für Stadtgebäude: Texturen (Pixelschrift), Modelle, Blockzustände.

Jedes Schild ist 1 Block hoch und mehrere Blöcke breit; jeder Block zeigt ein Stück der Schrift.

  python3 werkzeuge/schilder.py
"""
import json
import os
import random

from PIL import Image

A = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "deadzonemc")
D = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "data")

# Art -> (Text, Hintergrund, Schrift)
SCHILDER = {
    "polizei": ("POLIZEI", "1e3c8c", "f2f2f2"),
    "waffen": ("WAFFEN", "161616", "d02a2a"),
    "apotheke": ("APOTHEKE", "c41e26", "f4f4f4"),
    "krankenhaus": ("KRANKENHAUS", "e6e6e2", "c01818"),
    "supermarkt": ("SUPERMARKT", "d8701a", "fff6e0"),
    "tankstelle": ("TANKSTELLE", "e2b21c", "1a1a1a"),
    "werkstatt": ("WERKSTATT", "343434", "f08a1e"),
    "baumarkt": ("BAUMARKT", "e0561a", "ffffff"),
    "militaer": ("MILITÄR", "4a5628", "e4dcb0"),
    "bank": ("BANK", "1c4a30", "e8c656"),
}

# 5×7-Pixelschrift
GLYPHEN = {
    "A": ["01110", "10001", "10001", "11111", "10001", "10001", "10001"],
    "B": ["11110", "10001", "10001", "11110", "10001", "10001", "11110"],
    "E": ["11111", "10000", "10000", "11110", "10000", "10000", "11111"],
    "F": ["11111", "10000", "10000", "11110", "10000", "10000", "10000"],
    "H": ["10001", "10001", "10001", "11111", "10001", "10001", "10001"],
    "I": ["11111", "00100", "00100", "00100", "00100", "00100", "11111"],
    "K": ["10001", "10010", "10100", "11000", "10100", "10010", "10001"],
    "L": ["10000", "10000", "10000", "10000", "10000", "10000", "11111"],
    "M": ["10001", "11011", "10101", "10101", "10001", "10001", "10001"],
    "N": ["10001", "11001", "10101", "10011", "10001", "10001", "10001"],
    "O": ["01110", "10001", "10001", "10001", "10001", "10001", "01110"],
    "P": ["11110", "10001", "10001", "11110", "10000", "10000", "10000"],
    "R": ["11110", "10001", "10001", "11110", "10100", "10010", "10001"],
    "S": ["01111", "10000", "10000", "01110", "00001", "00001", "11110"],
    "T": ["11111", "00100", "00100", "00100", "00100", "00100", "00100"],
    "U": ["10001", "10001", "10001", "10001", "10001", "10001", "01110"],
    "W": ["10001", "10001", "10001", "10101", "10101", "11011", "10001"],
    "Z": ["11111", "00001", "00010", "00100", "01000", "10000", "11111"],
    "Ä": ["01010", "00000", "01110", "10001", "11111", "10001", "10001"],
}


def hexc(s, a=255):
    return tuple(int(s[i:i + 2], 16) for i in (0, 2, 4)) + (a,)


def mischen(c, d, t):
    return tuple(int(c[i] * (1 - t) + d[i] * t) for i in range(3)) + (255,)


def teile(text):
    return max(2, -(-(len(text) * 6 + 5) // 16))


def schild(art, text, hg, schrift):
    n = teile(text)
    breite = n * 16
    rng = random.Random(art)
    b = Image.new("RGBA", (breite, 16), hexc("2a2c2e"))
    hgc = hexc(hg)
    for y in range(2, 14):
        for x in range(1, breite - 1):
            c = hgc
            v = rng.uniform(-10, 10)
            c = tuple(max(0, min(255, int(c[i] + v))) for i in range(3)) + (255,)
            b.putpixel((x, y), c)
    # Rahmen
    rand = hexc("1a1c1e")
    for x in range(breite):
        b.putpixel((x, 1), rand)
        b.putpixel((x, 14), rand)
    for y in range(1, 15):
        b.putpixel((0, y), rand)
        b.putpixel((breite - 1, y), rand)
    # Schrift mittig
    w = len(text) * 6 - 1
    x0 = (breite - w) // 2
    sc = hexc(schrift)
    for i, ch in enumerate(text):
        g = GLYPHEN[ch]
        for gy, zeile in enumerate(g):
            for gx, bit in enumerate(zeile):
                if bit == "1":
                    b.putpixel((x0 + i * 6 + gx, 4 + gy), sc)
    # Verfall: Schmutz, Rost, ausgeblichene Stellen, ein kaputtes Stück
    for _ in range(breite // 2):
        x, y = rng.randrange(1, breite - 1), rng.randrange(2, 14)
        b.putpixel((x, y), mischen(b.getpixel((x, y)), hexc("3a2a1a"), rng.uniform(0.4, 0.8)))
    for _ in range(breite // 6):
        x, y = rng.randrange(1, breite - 1), rng.randrange(10, 14)
        for dy in range(rng.randrange(1, 3)):
            if y + dy < 14:
                b.putpixel((x, y + dy), mischen(b.getpixel((x, y + dy)), hexc("7a3a14"), 0.7))
    kx = rng.randrange(2, breite - 6)
    for dx in range(rng.randrange(2, 4)):
        for dy in range(rng.randrange(2, 4)):
            b.putpixel((kx + dx, 11 + dy), hexc("1e1e1e"))
    return b, n


def main():
    tex = os.path.join(A, "textures", "block", "schild")
    mod = os.path.join(A, "models", "block", "schild")
    for p in (tex, mod, os.path.join(A, "blockstates"), os.path.join(A, "models", "item")):
        os.makedirs(p, exist_ok=True)
    rand = Image.new("RGBA", (16, 16))
    rng = random.Random(7)
    for y in range(16):
        for x in range(16):
            v = rng.randint(-8, 8)
            rand.putpixel((x, y), (46 + v, 48 + v, 50 + v, 255))
    rand.save(os.path.join(tex, "rand.png"))
    varianten = {}
    breiten = {}
    for art, (text, hg, schrift) in SCHILDER.items():
        bild, n = schild(art, text, hg, schrift)
        breiten[art] = n
        for t in range(5):
            name = f"{art}_{t}"
            if t < n:
                bild.crop((t * 16, 0, t * 16 + 16, 16)).save(os.path.join(tex, f"{name}.png"))
                front = f"deadzonemc:block/schild/{name}"
            else:
                front = "deadzonemc:block/schild/rand"
            with open(os.path.join(mod, f"{name}.json"), "w") as f:
                json.dump({"parent": "minecraft:block/cube", "textures": {
                    "particle": "deadzonemc:block/schild/rand", "north": front,
                    "south": "deadzonemc:block/schild/rand", "east": "deadzonemc:block/schild/rand",
                    "west": "deadzonemc:block/schild/rand", "up": "deadzonemc:block/schild/rand",
                    "down": "deadzonemc:block/schild/rand"}}, f, indent=1)
            for facing, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
                v = {"model": f"deadzonemc:block/schild/{name}"}
                if y:
                    v["y"] = y
                varianten[f"art={art},facing={facing},teil={t}"] = v
    with open(os.path.join(A, "blockstates", "ladenschild.json"), "w") as f:
        json.dump({"variants": varianten}, f, indent=1)
    with open(os.path.join(A, "models", "item", "ladenschild.json"), "w") as f:
        json.dump({"parent": "deadzonemc:block/schild/polizei_0"}, f, indent=1)
    # abbaubar mit der Spitzhacke
    t = os.path.join(D, "minecraft", "tags", "block", "mineable")
    os.makedirs(t, exist_ok=True)
    pfad = os.path.join(t, "pickaxe.json")
    werte = json.load(open(pfad))["values"] if os.path.exists(pfad) else []
    if "deadzonemc:ladenschild" not in werte:
        werte.append("deadzonemc:ladenschild")
    with open(pfad, "w") as f:
        json.dump({"replace": False, "values": werte}, f, indent=1)
    with open(os.path.join(os.path.dirname(__file__), "schild_breiten.json"), "w") as f:
        json.dump(breiten, f)
    print(len(SCHILDER), "Schilder,", len(varianten), "Zustände")


if __name__ == "__main__":
    main()
