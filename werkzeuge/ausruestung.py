#!/usr/bin/env python3
"""Ausrüstung: Materialien, Pflanzen, Nahkampfwaffen, Rüstungssätze – Bilder, Modelle, Rezepte, Monster-Drops,
Better-Combat-Angriffe.

  python3 werkzeuge/ausruestung.py
"""
import json
import os
import random

from PIL import Image, ImageDraw

WURZEL = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
A = os.path.join(WURZEL, "src", "main", "resources", "assets", "deadzonemc")
D = os.path.join(WURZEL, "src", "main", "resources", "data", "deadzonemc")
MOD = "deadzonemc"


def c(s, a=255):
    return tuple(int(s[i:i + 2], 16) for i in (0, 2, 4)) + (a,)


def misch(f, d):
    return tuple(max(0, min(255, int(v + d))) for v in f[:3]) + (255,)


def speichern_item(name, b, hand=False):
    b.save(os.path.join(A, "textures", "item", name + ".png"))
    with open(os.path.join(A, "models", "item", name + ".json"), "w") as f:
        json.dump({"parent": "minecraft:item/handheld" if hand else "minecraft:item/generated",
                   "textures": {"layer0": f"{MOD}:item/{name}"}}, f, indent=1)


def raster(pixel, farben):
    b = Image.new("RGBA", (16, 16))
    for y, z in enumerate(pixel):
        for x, ch in enumerate(z):
            if ch != ".":
                b.putpixel((x, y), c(farben[ch]))
    return b


# ---------------------------------------------------------------------------------------------
# Materialien: kleine Formen, je eigene Farben
# ---------------------------------------------------------------------------------------------
FORMEN = {
    "splitter": ["................", "................", "..........k.....", ".........kwk....", "........kwwk....", ".......kwwk.....",
                 "......kwwk......", ".....kwwk..k....", "....kwwk..kwk...", "...kwwk..kwwk...", "...kwk..kwwk....", "....k..kwwk.....",
                 "......kwwk......", "......kkk.......", "................", "................"],
    "klumpen": ["................", "................", "................", ".....kkkk.......", "....kwwwwkk.....", "...kwWwwwwwk....",
                "...kwwwwWwwwk...", "..kwwwwwwwwwk...", "..kwwWwwwwwwk...", "..kwwwwwwwWwk...", "...kwwwwwwwk....", "....kkwwwwk.....",
                "......kkkk......", "................", "................", "................"],
    "faser": ["................", "................", "..kk............", "..kwk...........", "...kwk..........", "....kwk.........",
              ".....kwk........", "......kwkk......", ".......kwWk.....", "........kwwk....", ".........kwwk...", "..........kwk...",
              "...........kk...", "................", "................", "................"],
    "platte": ["................", "................", "................", "..kkkkkkkkkkk...", "..kwwwwwwwwwk...", "..kwWwwwwwWwk...",
               "..kwwwwwwwwwk...", "..kwwwwwwwwwk...", "..kwwwwwwwwwk...", "..kwWwwwwwWwk...", "..kwwwwwwwwwk...", "..kkkkkkkkkkk...",
               "................", "................", "................", "................"],
    "kristall": ["................", "........k.......", ".......kWk......", "......kWwk......", "......kwwk..k...", ".....kWwwk.kWk..",
                 ".....kwwwkkwwk..", "....kWwwwkwwwk..", "....kwwwwkwwk...", "....kwwwwwwwk...", ".....kwwwwwk....", "......kkkkk.....",
                 "................", "................", "................", "................"],
    "beutel": ["................", "................", ".......kk.......", "......kWwk......", ".....kwwwwk.....", "....kwwwwwwk....",
               "...kwwWwwwwwk...", "...kwwwwwWwwk...", "...kwwwwwwwwk...", "...kwwWwwwwwk...", "....kwwwwwwk....", ".....kkkkkk.....",
               "................", "................", "................", "................"],
    "teile": ["................", "................", "...kkk....kkk...", "...kWk....kwk...", "...kwkkkkkkwk...", "....kwwwwwwk....",
              "....kwkkkkwk....", "....kwkWWkwk....", "....kwkkkkwk....", "....kwwwwwwk....", "...kwkkkkkkwk...", "...kwk....kWk...",
              "...kkk....kkk...", "................", "................", "................"],
    "staub": ["................", "................", "......w...W.....", "...W.....w......", ".....wW......w..", "..w....wWw......",
              "......wWWWw..W..", "...W..wWWWw.....", ".....w.wWw...w..", "..w......w......", "......W....W....", "....w....w......",
              "................", "................", "................", "................"],
    "kern": ["................", "................", "................", "......kkkk......", ".....kWWwwk.....", "....kWWwwwwk....",
             "....kWwwwwwk....", "....kwwwwwwk....", "....kwwwwwwk....", ".....kwwwwk.....", "......kkkk......", "................",
             "................", "................", "................", "................"],
    "blatt": ["................", "................", ".........kk.....", "........kwwk....", ".......kwWwk....", "......kwwWwk....",
              ".....kwwWwwk....", "....kwwWwwk.....", "....kwWwwk......", "....kWwwk.......", "....kwkk........", "...kk...........",
              "..k.............", "................", "................", "................"],
    "flocke": ["................", "................", "................", ".....kkkk.......", "....kwWwwkk.....", "...kwwwwWwwk....",
               "...kwWwwwwwk....", "...kwwwwwWwk....", "....kwwWwwk.....", ".....kkwwk......", ".......kk.......", ".......k........",
               ".......k........", "................", "................", "................"],
    "naegel": ["................", "................", "...kk.....kk....", "...kwk....kwk...", "....kwk....kwk..", ".....kwk....kwk.",
               "......kk.....k..", "..kk.....kk.....", "..kwk....kwk....", "...kwk....kwk...", "....kwk....kwk..", ".....kk.....kk..",
               "................", "................", "................", "................"],
    "rolle": ["................", "................", "................", ".....kkkkk......", "....kwwwwwk.....", "...kwwkkkwwk....",
              "...kwk...kwk....", "...kwk...kwkkkk.", "...kwk...kwwwwk.", "...kwwkkkwwkkkk.", "....kwwwwwk.....", ".....kkkkk......",
              "................", "................", "................", "................"],
}
MATERIAL_BILD = {
    "knochensplitter": ("splitter", "2a2418", "d8ceb0", "f4ecd8"), "verseuchtes_gewebe": ("klumpen", "2a0e14", "7a2a3a", "9cff4a"),
    "sehne": ("faser", "3a1a1a", "b8786a", "e8b0a0"), "mutantenhaut": ("platte", "2a1a14", "8a6050", "a8806a"),
    "chitinplatte": ("platte", "141a14", "3a4a2a", "6a7a4a"), "panzerplatte": ("platte", "1a1a1a", "5a5e62", "8a8e92"),
    "strahlenkristall": ("kristall", "1a2a0a", "7ae84a", "e8ffb0"), "sporenbeutel": ("beutel", "2a2414", "8a8a5a", "c8c87a"),
    "drohnenteile": ("teile", "1a1a1a", "4a4e52", "ff3a2a"), "mottenstaub": ("staub", "000000", "c8b88a", "fff4d0"),
    "glutkern": ("kern", "2a0a00", "e85a1e", "ffd84a"), "mutagen": ("kristall", "1a0a2a", "a84ae8", "f0c8ff"),
    "heilkraut": ("blatt", "1a2a10", "4a8a3a", "8ad86a"), "baumwolle": ("flocke", "3a3a2a", "e8e4d8", "ffffff"),
    "naegel": ("naegel", "2a2a2a", "8a8e92", "c0c4c8"), "klebeband": ("rolle", "2a2a2a", "8a8a82", "b8b8b0"),
    "gummi": ("klumpen", "0a0a0a", "2a2a2a", "4a4a4a"),
}

# ---------------------------------------------------------------------------------------------
# Nahkampfwaffen: diagonal (Griff unten links, Klinge oben rechts)
# ---------------------------------------------------------------------------------------------
NAHKAMPF = {
    "kampfmesser": ("griff_kurz", "4a3a2a", "b8bcc0", "klinge_kurz"),
    "machete": ("griff_kurz", "2a2a2a", "9a9ea2", "klinge_lang"),
    "baseballschlaeger": ("griff_lang", "a8804a", "c8a06a", "keule"),
    "nagelschlaeger": ("griff_lang", "a8804a", "c8a06a", "nagelkeule"),
    "feuerwehraxt": ("griff_lang", "a82a1e", "b8bcc0", "axt"),
    "vorschlaghammer": ("griff_lang", "8a6a42", "4a4e52", "hammer"),
    "speer": ("griff_lang", "8a6a42", "d8ceb0", "spitze"),
    "knochenkeule": ("griff_lang", "6a4a2a", "d8ceb0", "knochen"),
    "mutantenklinge": ("griff_kurz", "3a1a2a", "a84ae8", "klinge_lang"),
}


def nahkampf_bild(griff, griff_f, kopf_f, kopf):
    b = Image.new("RGBA", (16, 16))
    gf, kf = c(griff_f), c(kopf_f)
    dunkel = c("1a1414")
    lang = griff == "griff_lang"
    # Griff: von (2,13) diagonal
    laenge = 8 if lang else 4
    for i in range(laenge):
        x, y = 2 + i, 13 - i
        b.putpixel((x, y), gf)
        b.putpixel((x + 1, y), misch(gf, -40))
        b.putpixel((x, y + 1), dunkel) if i == 0 else None
    sx, sy = 2 + laenge, 13 - laenge
    if kopf in ("klinge_kurz", "klinge_lang"):
        n = 5 if kopf == "klinge_kurz" else 8
        b.putpixel((sx - 1, sy + 1), c("3a3a3a"))
        b.putpixel((sx, sy + 1), c("3a3a3a"))
        for i in range(n):
            x, y = sx + i, sy - i
            if 0 <= x < 16 and 0 <= y < 16:
                b.putpixel((x, y), kf)
                if x + 1 < 16:
                    b.putpixel((x + 1, y), misch(kf, 40))
                if y + 1 < 16:
                    b.putpixel((x, y + 1), misch(kf, -50))
    elif kopf in ("keule", "nagelkeule", "knochen"):
        for i in range(6):
            x, y = sx + i, sy - i
            for d in (-1, 0, 1):
                if 0 <= x + d < 16 and 0 <= y < 16:
                    b.putpixel((x + d, y), kf if d == 0 else misch(kf, -30))
        if kopf == "nagelkeule":
            for (x, y) in ((sx + 1, sy - 3), (sx + 3, sy - 2), (sx + 4, sy - 5), (sx + 2, sy - 5), (sx + 5, sy - 3)):
                if 0 <= x < 16 and 0 <= y < 16:
                    b.putpixel((x, y), c("8a8e92"))
    elif kopf in ("axt", "hammer"):
        for i in range(4):
            b.putpixel((sx + i, sy - i), gf)
        hx, hy = sx + 3, sy - 3
        for dx in range(-3, 3):
            for dy in range(-2, 3):
                x, y = hx + dx - dy, hy + dy + dx // 2
                if 0 <= x < 16 and 0 <= y < 16 and (kopf == "hammer" or dx < 1):
                    b.putpixel((x, y), kf if (dx + dy) % 3 else misch(kf, 30))
    elif kopf == "spitze":
        for i in range(4):
            b.putpixel((sx + i, sy - i), gf)
        for i in range(3):
            x, y = sx + 4 + i, sy - 4 - i
            if 0 <= x < 16 and 0 <= y < 16:
                b.putpixel((x, y), kf)
                if x - 1 >= 0:
                    b.putpixel((x - 1, y), misch(kf, -40))
    return b


# ---------------------------------------------------------------------------------------------
# Rüstungen
# ---------------------------------------------------------------------------------------------
SAETZE = {
    #             Grund     Akzent    Dunkel    Muster
    "pluenderer": ("6a4a2e", "8a7a5a", "3a2a1a", "flicken"),
    "schrottpanzer": ("6a5e52", "7a3e1c", "2a2622", "nieten"),
    "taktisch": ("4a5228", "1e1e1a", "2a2e16", "riemen"),
    "sanitaeter": ("d8d4c8", "c01818", "8a8a82", "kreuz"),
    "schutzanzug": ("c8a82a", "2a2a2a", "7a6a1a", "visier"),
    "tarnanzug": ("3a4a2a", "5a6a3a", "1e2a14", "flecken"),
    "mutantenpanzer": ("5a2a3a", "c8b898", "2a1218", "rippen"),
}
TEIL_FORM = {
    "kopf": ["................", "................", "....kkkkkkk.....", "...kgggggggk....", "..kgaggggagk....", "..kgggggggggk...",
             "..kggkkkkkggk...", "..kgk.....kgk...", "..kk.......kk...", "................", "................", "................",
             "................", "................", "................", "................"],
    "brust": ["................", "..kkk.....kkk...", ".kgggk...kgggk..", ".kggggkkkggggk..", ".kgggggggggggk..", "..kkggggaggkk...",
              "....kgggggk.....", "....kggaggk.....", "....kgggggk.....", "....kgggggk.....", "....kggaggk.....", "....kgggggk.....",
              "....kkkkkkk.....", "................", "................", "................"],
    "beine": ["................", "................", "....kkkkkkkk....", "....kggaaggk....", "....kggggggk....", "....kgggkggk....",
              "....kgggkggk....", "....kggk.kggk...", "....kggk.kggk...", "....kggk.kggk...", "....kgak.kgak...", "....kggk.kggk...",
              "....kkkk.kkkk...", "................", "................", "................"],
    "stiefel": ["................", "................", "................", "................", "................", "....kkk..kkk....",
                "....kgk..kgk....", "....kgk..kgk....", "....kgk..kgk....", "...kggk..kggk...", "..kgagk..kgagk..", "..kgggk..kgggk..",
                "..kkkkk..kkkkk..", "................", "................", "................"],
}


def ruestung_icon(satz, teil):
    g, a, d, _ = SAETZE[satz]
    return raster(TEIL_FORM[teil], {"k": "141210", "g": g, "a": a, "d": d})


def ruestung_schicht(satz, zwei):
    g, a, d, muster = SAETZE[satz]
    rng = random.Random(satz + str(zwei))
    b = Image.new("RGBA", (64, 32))
    bereiche = [(16, 16, 40, 32), (0, 16, 16, 32)] if zwei else [(0, 0, 32, 16), (16, 16, 40, 32), (40, 16, 56, 32), (0, 16, 16, 32)]
    for (x0, y0, x1, y1) in bereiche:
        for y in range(y0, y1):
            for x in range(x0, x1):
                v = rng.randint(-10, 10)
                b.putpixel((x, y), misch(c(g), v))
    p = b.load()
    if muster == "flicken":
        for _ in range(14):
            x, y = rng.randrange(0, 56), rng.randrange(0, 32)
            if p[x, y][3]:
                for dx in range(3):
                    for dy in range(3):
                        if x + dx < 64 and y + dy < 32 and p[x + dx, y + dy][3]:
                            p[x + dx, y + dy] = misch(c(a), rng.randint(-8, 8))
    elif muster == "nieten":
        for y in range(0, 32, 4):
            for x in range(0, 64, 4):
                if p[x, y][3]:
                    p[x, y] = c("b8bcc0")
        for _ in range(40):
            x, y = rng.randrange(0, 56), rng.randrange(0, 32)
            if p[x, y][3]:
                p[x, y] = c(a)
    elif muster == "riemen":
        for y in range(16, 32):
            for x in (20, 21, 34, 35):
                if p[x, y][3]:
                    p[x, y] = c(a)
        for x in range(16, 40):
            for y in (24, 25):
                if p[x, y][3]:
                    p[x, y] = c(a)
    elif muster == "kreuz":
        for (cx, cy) in ((24, 24), (12, 12), (44, 22)):
            for i in range(-2, 3):
                for q in ((cx + i, cy), (cx, cy + i)):
                    if 0 <= q[0] < 64 and 0 <= q[1] < 32 and p[q][3]:
                        p[q] = c(a)
    elif muster == "visier":
        for x in range(9, 15):
            for y in range(10, 13):
                p[x, y] = c("2a3a4a")
        for y in range(16, 32, 3):
            for x in range(16, 40):
                if p[x, y][3]:
                    p[x, y] = misch(c(g), -25)
    elif muster == "flecken":
        for _ in range(60):
            x, y = rng.randrange(0, 56), rng.randrange(0, 32)
            f = c(a) if rng.random() < 0.5 else c(d)
            for dx in range(rng.randrange(1, 4)):
                for dy in range(rng.randrange(1, 3)):
                    if x + dx < 64 and y + dy < 32 and p[x + dx, y + dy][3]:
                        p[x + dx, y + dy] = f
    elif muster == "rippen":
        for y in range(16, 32, 2):
            for x in range(18, 38):
                if p[x, y][3]:
                    p[x, y] = c(a)
        for _ in range(30):
            x, y = rng.randrange(0, 56), rng.randrange(0, 32)
            if p[x, y][3]:
                p[x, y] = c("9cff4a")
    return b


# ---------------------------------------------------------------------------------------------
# Rezepte
# ---------------------------------------------------------------------------------------------
def zutat(v):
    return {"tag": v[1:]} if v.startswith("#") else {"item": v if ":" in v else f"{MOD}:{v}"}


def geformt(name, muster, schluessel, ergebnis, anzahl=1):
    with open(os.path.join(D, "recipe", name + ".json"), "w") as f:
        json.dump({"type": "minecraft:crafting_shaped", "category": "equipment", "pattern": muster,
                   "key": {k: zutat(v) for k, v in schluessel.items()},
                   "result": {"id": ergebnis if ":" in ergebnis else f"{MOD}:{ergebnis}", "count": anzahl}}, f, indent=1)


def formlos(name, zutaten, ergebnis, anzahl=1):
    with open(os.path.join(D, "recipe", name + ".json"), "w") as f:
        json.dump({"type": "minecraft:crafting_shapeless", "category": "misc", "ingredients": [zutat(z) for z in zutaten],
                   "result": {"id": ergebnis if ":" in ergebnis else f"{MOD}:{ergebnis}", "count": anzahl}}, f, indent=1)


SATZ_ZUTATEN = {
    # Satz: (Hauptmaterial, Zweitmaterial)
    "pluenderer": ("minecraft:leather", "stoffreste"),
    "schrottpanzer": ("schrott", "panzerplatte"),
    "taktisch": ("stoffreste", "panzerplatte"),
    "sanitaeter": ("baumwolle", "stoffreste"),
    "schutzanzug": ("gummi", "stoffreste"),
    "tarnanzug": ("sehne", "#minecraft:leaves"),
    "mutantenpanzer": ("chitinplatte", "mutantenhaut"),
}


def ruestungs_rezepte():
    for satz, (h, z) in SATZ_ZUTATEN.items():
        extra = {"pluenderer": None, "schrottpanzer": "minecraft:leather", "taktisch": "minecraft:iron_ingot",
                 "sanitaeter": "heilkraut", "schutzanzug": "filter", "tarnanzug": "mutantenhaut", "mutantenpanzer": "mutagen"}[satz]
        e = extra or z
        geformt(f"{satz}_kopf", ["HHH", "Z Z"] if satz != "schutzanzug" else ["HHH", "HEH"], {"H": h, "Z": z, "E": e}, f"{satz}_kopf")
        geformt(f"{satz}_brust", ["H H", "HEH", "ZHZ"], {"H": h, "Z": z, "E": e}, f"{satz}_brust")
        geformt(f"{satz}_beine", ["HZH", "H H", "H H"], {"H": h, "Z": z}, f"{satz}_beine")
        geformt(f"{satz}_stiefel", ["Z Z", "H H"], {"H": h, "Z": z}, f"{satz}_stiefel")


def sonstige_rezepte():
    geformt("kampfmesser", [" I", "S "], {"I": "minecraft:iron_ingot", "S": "sehne"}, "kampfmesser")
    geformt("machete", ["  I", " I ", "S  "], {"I": "minecraft:iron_ingot", "S": "sehne"}, "machete")
    geformt("baseballschlaeger", ["  P", " P ", "P  "], {"P": "#minecraft:planks"}, "baseballschlaeger")
    formlos("nagelschlaeger", ["baseballschlaeger", "naegel", "naegel"], "nagelschlaeger")
    geformt("feuerwehraxt", ["II ", "IS ", " S "], {"I": "minecraft:iron_ingot", "S": "minecraft:stick"}, "feuerwehraxt")
    geformt("vorschlaghammer", ["III", "III", " S "], {"I": "minecraft:iron_ingot", "S": "minecraft:stick"}, "vorschlaghammer")
    geformt("speer", ["  K", " S ", "S  "], {"K": "knochensplitter", "S": "minecraft:stick"}, "speer")
    geformt("knochenkeule", [" KK", " KK", "S  "], {"K": "knochensplitter", "S": "minecraft:stick"}, "knochenkeule")
    geformt("mutantenklinge", ["  C", " C ", "MS "], {"C": "chitinplatte", "M": "mutagen", "S": "sehne"}, "mutantenklinge")
    formlos("naegel", ["minecraft:iron_nugget", "minecraft:iron_nugget", "minecraft:iron_nugget"], "naegel", 4)
    formlos("stoffreste_aus_baumwolle", ["baumwolle", "baumwolle"], "stoffreste", 2)
    formlos("krautverband", ["verband", "heilkraut"], "verband", 2)
    formlos("knochenmehl_aus_splittern", ["knochensplitter"], "minecraft:bone_meal", 2)
    formlos("heilkraut_samen", ["heilkraut"], "heilkraut_samen", 2)
    formlos("baumwollsamen", ["baumwolle"], "baumwollsamen", 1)
    formlos("elektronik_aus_drohne", ["drohnenteile"], "elektronik", 2)
    formlos("blendgranate_aus_staub", ["mottenstaub", "minecraft:gunpowder", "minecraft:iron_nugget"], "blendgranate", 2)
    formlos("antibiotika", ["verseuchtes_gewebe", "heilkraut", "heilkraut", "minecraft:glass_bottle"], "antibiotika")
    formlos("anti_rad", ["strahlenkristall", "heilkraut", "minecraft:glass_bottle"], "anti_rad", 2)
    formlos("filter_aus_kohle", ["minecraft:charcoal", "stoffreste", "gummi"], "filter")
    formlos("gummi_aus_reifen", ["schrott", "minecraft:slime_ball"], "gummi", 2)


# ---------------------------------------------------------------------------------------------
# Monster-Drops
# ---------------------------------------------------------------------------------------------
def drop(item, chance, mn=1, mx=1):
    f = []
    if mx > 1:
        f.append({"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": mn, "max": mx}})
    f.append({"function": "minecraft:enchanted_count_increase", "enchantment": "minecraft:looting",
              "count": {"type": "minecraft:uniform", "min": 0, "max": 1}})
    return {"rolls": 1, "conditions": [{"condition": "minecraft:killed_by_player"},
                                       {"condition": "minecraft:random_chance_with_enchanted_bonus", "enchantment": "minecraft:looting",
                                        "unenchanted_chance": chance, "enchanted_chance": {"type": "minecraft:linear", "base": chance + 0.05,
                                                                                         "per_level_above_first": 0.05}}],
            "entries": [{"type": "minecraft:item", "name": item if ":" in item else f"{MOD}:{item}", "functions": f}]}


DROPS = {
    "wanderer": [("knochensplitter", 0.4, 1, 2), ("verseuchtes_gewebe", 0.25), ("stoffreste", 0.15), ("konservendose", 0.03)],
    "schreier": [("verseuchtes_gewebe", 0.5), ("sehne", 0.3)],
    "lauerer": [("sehne", 0.5), ("knochensplitter", 0.35)],
    "kriecher": [("chitinplatte", 0.45), ("sehne", 0.4)],
    "der_stille": [("mutantenhaut", 0.35), ("knochensplitter", 0.5, 1, 3)],
    "imitator": [("stoffreste", 0.4), ("militaerpatrone", 0.25, 1, 3)],
    "nebelwesen": [("verseuchtes_gewebe", 0.4), ("mottenstaub", 0.2)],
    "schwarm": [("knochensplitter", 0.2)],
    "brut": [("sporenbeutel", 0.6), ("verseuchtes_gewebe", 0.4)],
    "juggernaut": [("panzerplatte", 0.7, 1, 2), ("mutantenhaut", 0.5)],
    "gefallener_soldat": [("munition_556", 0.45, 2, 8), ("munition_9mm", 0.4, 3, 10), ("panzerplatte", 0.25), ("stoffreste", 0.3),
                          ("handgranate", 0.08)],
    "glutleib": [("glutkern", 0.5), ("mutantenhaut", 0.3)],
    "seuchenhund": [("sehne", 0.5), ("knochensplitter", 0.4)],
    "wuehler": [("chitinplatte", 0.4), ("knochensplitter", 0.4)],
    "sauger": [("mutantenhaut", 0.4), ("sehne", 0.4)],
    "seuchentraeger": [("strahlenkristall", 0.5), ("verseuchtes_gewebe", 0.5), ("filter", 0.2), ("gummi", 0.3)],
    "ertrunkener": [("verseuchtes_gewebe", 0.4), ("gummi", 0.2), ("stoffreste", 0.3)],
    "spaeherdrohne": [("drohnenteile", 0.8, 1, 2), ("elektronik", 0.4), ("batterie", 0.3)],
    "lichtmotte": [("mottenstaub", 0.8, 1, 2)],
    "scheintoter": [("stoffreste", 0.4), ("knochensplitter", 0.4), ("militaerpatrone", 0.1, 1, 2)],
}
BOSSE = {"verschmolzener": "mutantenhaut", "nebelmutter": "mottenstaub", "strahlende": "strahlenkristall",
         "brutkoenigin": "chitinplatte", "schlaechter": "sehne", "kommandant": "panzerplatte", "hirte": "knochensplitter",
         "schlund": "chitinplatte", "pilzvater": "sporenbeutel", "erfrorene": "mutantenhaut"}


def drops():
    z = os.path.join(D, "loot_table", "entities")
    os.makedirs(z, exist_ok=True)
    for mob, liste in DROPS.items():
        pools = [drop(*e) for e in liste]
        with open(os.path.join(z, mob + ".json"), "w") as f:
            json.dump({"type": "minecraft:entity", "pools": pools}, f, indent=1)
    for mob, mat in BOSSE.items():
        pools = [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"{MOD}:mutagen",
                                           "functions": [{"function": "minecraft:set_count",
                                                          "count": {"type": "minecraft:uniform", "min": 1, "max": 2}}]}]},
                 {"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"{MOD}:{mat}",
                                           "functions": [{"function": "minecraft:set_count",
                                                          "count": {"type": "minecraft:uniform", "min": 3, "max": 6}}]}]},
                 {"rolls": 1, "entries": [{"type": "minecraft:loot_table", "value": f"{MOD}:bonus/legendaer", "weight": 1},
                                          {"type": "minecraft:loot_table", "value": f"{MOD}:bonus/hoch", "weight": 2}]}]
        with open(os.path.join(z, mob + ".json"), "w") as f:
            json.dump({"type": "minecraft:entity", "pools": pools}, f, indent=1)


# ---------------------------------------------------------------------------------------------
# Pflanzen
# ---------------------------------------------------------------------------------------------
def pflanzen():
    rng = random.Random(4)
    for name, (stiel, blatt, bluete) in {"heilkraut": ("3a6a2a", "5aa84a", "e8e04a"), "baumwolle": ("4a5a2a", "6a8a3a", "f4f0e8")}.items():
        for stufe in range(4):
            b = Image.new("RGBA", (16, 16))
            hoehe = 4 + stufe * 3
            for x in (3, 7, 11, 13):
                for y in range(16 - hoehe, 16):
                    b.putpixel((x, y), c(stiel))
                    if rng.random() < 0.4 and x + 1 < 16:
                        b.putpixel((x + 1, y), c(blatt))
                    if rng.random() < 0.3 and x - 1 >= 0:
                        b.putpixel((x - 1, y), c(blatt))
                if stufe == 3:
                    for dx in (-1, 0, 1):
                        for dy in (0, 1):
                            if 0 <= x + dx < 16:
                                b.putpixel((x + dx, 16 - hoehe + dy), c(bluete))
            b.save(os.path.join(A, "textures", "block", f"{name}_{stufe}.png"))
            with open(os.path.join(A, "models", "block", f"{name}_{stufe}.json"), "w") as f:
                json.dump({"parent": "minecraft:block/crop", "textures": {"crop": f"{MOD}:block/{name}_{stufe}"}}, f, indent=1)
        with open(os.path.join(A, "blockstates", f"{name}_pflanze.json"), "w") as f:
            json.dump({"variants": {f"age={a}": {"model": f"{MOD}:block/{name}_{min(3, a // 2)}"} for a in range(8)}}, f, indent=1)
        samen = "heilkraut_samen" if name == "heilkraut" else "baumwollsamen"
        reif = {"condition": "minecraft:block_state_property", "block": f"{MOD}:{name}_pflanze", "properties": {"age": "7"}}
        z = os.path.join(D, "loot_table", "blocks")
        os.makedirs(z, exist_ok=True)
        with open(os.path.join(z, f"{name}_pflanze.json"), "w") as f:
            json.dump({"type": "minecraft:block", "pools": [
                {"rolls": 1, "entries": [{"type": "minecraft:alternatives", "children": [
                    {"type": "minecraft:item", "name": f"{MOD}:{name}", "conditions": [reif],
                     "functions": [{"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 1, "max": 3}}]},
                    {"type": "minecraft:item", "name": f"{MOD}:{samen}"}]}]},
                {"rolls": 1, "conditions": [reif], "entries": [{"type": "minecraft:item", "name": f"{MOD}:{samen}",
                                                                "functions": [{"function": "minecraft:set_count",
                                                                               "count": {"type": "minecraft:uniform", "min": 0, "max": 2}}]}]}]},
                      f, indent=1)
        speichern_item(samen, raster(["................", "................", "................", "................", "......k.........",
                                      ".....kwk..k.....", "......k..kwk....", "..k.......k.....", ".kwk...k........", "..k...kwk.......",
                                      ".......k...k....", "...........kwk..", "....k.......k...", "...kwk..........", "....k...........",
                                      "................"], {"k": "2a1e0e", "w": "a8884a" if name == "baumwolle" else "6a8a3a"}))


def better_combat():
    z = os.path.join(D, "..", "..", "data", MOD, "weapon_attributes")
    z = os.path.join(WURZEL, "src", "main", "resources", "data", MOD, "weapon_attributes")
    os.makedirs(z, exist_ok=True)
    art = {"kampfmesser": "bettercombat:dagger", "machete": "bettercombat:sword", "baseballschlaeger": "bettercombat:mace",
           "nagelschlaeger": "bettercombat:mace", "feuerwehraxt": "bettercombat:heavy_axe", "vorschlaghammer": "bettercombat:hammer",
           "speer": "bettercombat:spear", "knochenkeule": "bettercombat:mace", "mutantenklinge": "bettercombat:claymore",
           "rostiges_messer": "bettercombat:dagger", "brecheisen": "bettercombat:mace"}
    for n, p in art.items():
        with open(os.path.join(z, n + ".json"), "w") as f:
            json.dump({"parent": p}, f, indent=1)


def main():
    for p in ("models/item", "textures/item", "models/block", "textures/block", "blockstates", "textures/models/armor"):
        os.makedirs(os.path.join(A, p), exist_ok=True)
    os.makedirs(os.path.join(D, "recipe"), exist_ok=True)
    for name, (form, k, w, W) in MATERIAL_BILD.items():
        speichern_item(name, raster(FORMEN[form], {"k": k, "w": w, "W": W}))
    for name, (griff, gf, kf, kopf) in NAHKAMPF.items():
        speichern_item(name, nahkampf_bild(griff, gf, kf, kopf), hand=True)
    for satz in SAETZE:
        for teil in TEIL_FORM:
            speichern_item(f"{satz}_{teil}", ruestung_icon(satz, teil))
        ruestung_schicht(satz, False).save(os.path.join(A, "textures", "models", "armor", f"{satz}_layer_1.png"))
        ruestung_schicht(satz, True).save(os.path.join(A, "textures", "models", "armor", f"{satz}_layer_2.png"))
    pflanzen()
    ruestungs_rezepte()
    sonstige_rezepte()
    drops()
    better_combat()
    print(len(MATERIAL_BILD), "Materialien,", len(NAHKAMPF), "Nahkampfwaffen,", len(SAETZE), "Rüstungssätze,", len(DROPS) + len(BOSSE), "Monster-Drops")


if __name__ == "__main__":
    main()
