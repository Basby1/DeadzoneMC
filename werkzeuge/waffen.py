#!/usr/bin/env python3
"""Deadzone-Waffen: 3D-Modelle (aus Quadern), Texturen, Klänge, Rezepte.

  python3 werkzeuge/waffen.py            – alles
  python3 werkzeuge/waffen.py --vorschau – zusätzlich Vorschaubilder der Modelle (build/vorschau/waffen.png)
"""
import json
import math
import os
import random
import subprocess
import sys
import tempfile
import wave

import numpy as np
from PIL import Image, ImageDraw

WURZEL = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
A = os.path.join(WURZEL, "src", "main", "resources", "assets", "deadzonemc")
D = os.path.join(WURZEL, "src", "main", "resources", "data", "deadzonemc")

# ---------------------------------------------------------------------------------------------
# Textur: 16 Farbfelder je 4×4 Pixel
# ---------------------------------------------------------------------------------------------
FELDER = ["2a2c2e", "6a6e72", "4a2e1a", "8a5a32", "1c1c1c", "4a5228", "7a3e1c", "b8bcc0",
          "a8946a", "121212", "b8902a", "8a1e1e", "3a6a8a", "c86a1e", "4a4a4a", "d8d4c8"]
M = {n: i for i, n in enumerate(["metall", "stahl", "holz", "hell", "kunst", "oliv", "rost", "chrom",
                                  "sand", "gummi", "messing", "rot", "glas", "band", "grau", "weiss"])}


def feld_uv(i):
    x, y = (i % 4) * 4, (i // 4) * 4
    return [x + 0.5, y + 0.5, x + 3.5, y + 3.5]


def atlas():
    rng = random.Random(5)
    b = Image.new("RGBA", (16, 16))
    for i, f in enumerate(FELDER):
        r, g, bl = (int(f[k:k + 2], 16) for k in (0, 2, 4))
        for y in range(4):
            for x in range(4):
                v = rng.randint(-9, 9)
                b.putpixel(((i % 4) * 4 + x, (i // 4) * 4 + y), (max(0, min(255, r + v)), max(0, min(255, g + v)), max(0, min(255, bl + v)), 255))
    p = os.path.join(A, "textures", "item", "waffen")
    os.makedirs(p, exist_ok=True)
    b.save(os.path.join(p, "teile.png"))


def q(x0, x1, y0, y1, z0, z1, mat, dreh=None):
    """Quader in Modell-Einheiten; dreh = (achse, winkel, (ox, oy, oz))."""
    return (x0, x1, y0, y1, z0, z1, mat, dreh)


Z0, Z1 = 7.2, 8.8  # Standarddicke
W = {}
W["p9"] = [q(4, 13.5, 9, 11, 7.25, 8.75, "metall"), q(4, 12.5, 8, 9, 7.3, 8.7, "kunst"), q(13.5, 14, 9.5, 10.5, 7.6, 8.4, "chrom"),
           q(4.5, 6.8, 3.8, 8, 7.35, 8.65, "kunst", ("z", -22.5, (5.6, 8, 8))), q(6.8, 9, 7, 7.5, 7.6, 8.4, "kunst"),
           q(7.4, 7.8, 7.4, 8, 7.8, 8.2, "metall"), q(12.6, 13, 11, 11.5, 7.8, 8.2, "metall"), q(4.3, 4.9, 11, 11.5, 7.6, 8.4, "metall")]
W["revolver"] = [q(10, 17, 9.5, 10.6, 7.5, 8.5, "stahl"), q(6, 10, 8.4, 11.2, 7.3, 8.7, "chrom"), q(7.2, 9.8, 8.6, 10.9, 6.9, 9.1, "chrom"),
                 q(4.6, 7, 4.2, 8.6, 7.35, 8.65, "holz", ("z", -22.5, (5.8, 8.4, 8))), q(5.4, 6.4, 10.8, 11.8, 7.7, 8.3, "stahl"),
                 q(7, 8.6, 7.3, 7.8, 7.6, 8.4, "chrom"), q(16.4, 16.9, 10.6, 11.2, 7.8, 8.2, "stahl")]
W["mp9"] = [q(3, 13, 8, 10.6, 7.2, 8.8, "kunst"), q(13, 15.8, 9, 10, 7.6, 8.4, "metall"), q(8.2, 9.8, 3.6, 8, 7.4, 8.6, "metall"),
            q(4.6, 6.4, 4.6, 8, 7.35, 8.65, "kunst", ("z", -22.5, (5.5, 8, 8))), q(-1.5, 3, 8.6, 9.4, 7.65, 8.35, "metall"),
            q(-1.8, -0.8, 7.2, 10, 7.5, 8.5, "kunst"), q(4, 12, 10.6, 11.1, 7.7, 8.3, "metall")]
W["pumpgun"] = [q(6, 24, 9.6, 10.6, 7.5, 8.5, "metall"), q(8, 21, 8.4, 9.4, 7.55, 8.45, "metall"), q(14, 19, 8.0, 9.9, 7.2, 8.8, "kunst"),
                q(1, 8, 8, 11, 7.2, 8.8, "metall"), q(-7, 1, 6.6, 10.2, 7.3, 8.7, "kunst", ("z", 22.5, (1, 8.4, 8))),
                q(2, 4, 4.5, 8, 7.35, 8.65, "kunst", ("z", -22.5, (3, 8, 8))), q(23.4, 24, 10.6, 11, 7.85, 8.15, "chrom")]
W["doppelflinte"] = [q(6, 22, 9, 10, 7.0, 8.0, "metall"), q(6, 22, 9, 10, 8.0, 9.0, "metall"), q(1.5, 6, 8, 10.6, 7.2, 8.8, "stahl"),
                     q(-7, 1.5, 6.2, 9.8, 7.3, 8.7, "holz", ("z", 22.5, (1.5, 8, 8))), q(6, 12, 8, 9, 7.2, 8.8, "holz"),
                     q(3, 4.5, 7.2, 7.8, 7.6, 8.4, "chrom")]
W["sturmgewehr"] = [q(0, 13, 8, 11.4, 7.1, 8.9, "grau"), q(3, 11, 11.4, 12.6, 7.5, 8.5, "grau"), q(13, 20.5, 9.2, 10.2, 7.6, 8.4, "metall"),
                    q(10, 17, 8.4, 11, 7.2, 8.8, "kunst"), q(7.6, 9.8, 3.4, 8, 7.3, 8.7, "kunst", ("z", 22.5, (8.7, 8, 8))),
                    q(3.6, 5.4, 4.6, 8, 7.35, 8.65, "kunst", ("z", -22.5, (4.5, 8, 8))), q(-7, 0, 7.4, 11, 7.3, 8.7, "kunst"),
                    q(20, 21, 9.0, 10.4, 7.5, 8.5, "metall")]
W["kalaschnikow"] = [q(0, 11, 8.5, 11, 7.2, 8.8, "metall"), q(11, 17, 8.7, 10.8, 7.25, 8.75, "holz"), q(17, 22.5, 9.3, 10.1, 7.65, 8.35, "metall"),
                     q(11, 18, 10.8, 11.4, 7.7, 8.3, "metall"), q(8, 10.4, 3.6, 8.5, 7.35, 8.65, "rost", ("z", 22.5, (9.2, 8.5, 8))),
                     q(3.4, 5.2, 4.8, 8.5, 7.35, 8.65, "holz", ("z", -22.5, (4.3, 8.5, 8))), q(-7.5, 0, 7.0, 10.6, 7.3, 8.7, "holz"),
                     q(21.6, 22.2, 10.1, 10.8, 7.85, 8.15, "metall"), q(2, 9, 11, 11.4, 7.5, 8.5, "metall")]
W["jagdgewehr"] = [q(-8, 18, 7.6, 9.9, 7.3, 8.7, "hell"), q(-8, -2, 6.2, 9.9, 7.3, 8.7, "hell", ("z", 22.5, (-2, 8, 8))),
                   q(12, 26.5, 9.9, 10.7, 7.6, 8.4, "stahl"), q(2, 11, 9.9, 11.1, 7.4, 8.6, "metall"),
                   q(6.4, 7.2, 10.2, 10.9, 8.6, 10.4, "metall"), q(9.5, 10.5, 7.0, 7.6, 7.6, 8.4, "metall")]
W["praezisionsgewehr"] = [q(-8, 4, 7.0, 10.8, 7.2, 8.8, "sand"), q(4, 14, 8.4, 11.2, 7.2, 8.8, "sand"), q(14, 28, 9.4, 10.4, 7.55, 8.45, "metall"),
                          q(2, 12, 11.2, 12, 7.6, 8.4, "metall"), q(6, 7.8, 3.8, 8.4, 7.3, 8.7, "kunst"),
                          q(2.4, 4.2, 4.6, 8.4, 7.35, 8.65, "kunst", ("z", -22.5, (3.3, 8, 8))), q(27, 28.6, 9.0, 10.8, 7.4, 8.6, "kunst"),
                          q(13, 13.5, 4.5, 8.4, 7.0, 7.5, "metall", ("x", 22.5, (13, 8.4, 7.2))),
                          q(13, 13.5, 4.5, 8.4, 8.5, 9.0, "metall", ("x", -22.5, (13, 8.4, 8.8)))]
W["mg"] = [q(-1, 14, 8, 11.6, 7.0, 9.0, "metall"), q(14, 26, 9.0, 10.8, 7.3, 8.7, "grau"), q(26, 30, 9.5, 10.3, 7.65, 8.35, "metall"),
           q(5, 10, 4.0, 8, 6.4, 9.6, "oliv"), q(1.6, 3.4, 4.6, 8, 7.35, 8.65, "kunst", ("z", -22.5, (2.5, 8, 8))),
           q(-8, -1, 7.4, 11, 7.3, 8.7, "kunst"), q(22, 22.5, 4.4, 9, 6.9, 7.4, "metall", ("x", 22.5, (22, 9, 7.3))),
           q(22, 22.5, 4.4, 9, 8.6, 9.1, "metall", ("x", -22.5, (22, 9, 8.7))), q(3, 12, 11.6, 12.4, 7.6, 8.4, "metall")]
W["rohrpistole"] = [q(6, 15.5, 9, 10.6, 7.2, 8.8, "rost"), q(4.6, 8, 4.0, 9.4, 7.3, 8.7, "holz", ("z", -22.5, (6.3, 9, 8))),
                    q(7.4, 9.4, 8.8, 10.8, 7.1, 8.9, "band"), q(5.2, 6.4, 10.4, 11.4, 7.6, 8.4, "metall"), q(14.6, 15.5, 8.8, 10.8, 7.0, 9.0, "rost")]
W["rohrgewehr"] = [q(4, 22, 9.4, 10.8, 7.3, 8.7, "rost"), q(-8, 6, 7.4, 9.6, 7.35, 8.65, "hell"),
                   q(-8, -3, 5.6, 9.6, 7.35, 8.65, "hell", ("z", 22.5, (-3, 8, 8))), q(5, 7, 9.2, 11, 7.1, 8.9, "band"),
                   q(14, 16, 9.2, 11, 7.1, 8.9, "band"), q(2.4, 3.6, 9.6, 10.6, 7.7, 8.3, "metall")]
W["armbrust"] = [q(-2, 16, 8.0, 9.6, 7.3, 8.7, "holz"), q(12.4, 13.6, 8.8, 9.6, 0.5, 15.5, "metall"),
                 q(7.2, 12.6, 9.25, 9.45, 1.0, 1.4, "weiss", ("y", 45, (12.6, 9.3, 1.2))),
                 q(7.2, 12.6, 9.25, 9.45, 14.6, 15.0, "weiss", ("y", -45, (12.6, 9.3, 14.8))),
                 q(6, 18, 9.6, 10.0, 7.85, 8.15, "hell"), q(2.6, 4.4, 4.8, 8, 7.4, 8.6, "holz", ("z", -22.5, (3.5, 8, 8)))]
W["karabiner"] = [q(-7, 3, 7.2, 10.6, 7.3, 8.7, "oliv"), q(3, 13, 8.2, 11, 7.2, 8.8, "metall"), q(13, 23, 9.4, 10.3, 7.6, 8.4, "stahl"),
                  q(4, 11, 11, 12.2, 7.5, 8.5, "metall"), q(5, 10, 12.2, 13.0, 7.4, 8.6, "glas"),
                  q(7.6, 9.6, 4.2, 8.2, 7.35, 8.65, "kunst", ("z", 22.5, (8.6, 8.2, 8))),
                  q(3.2, 5.0, 4.6, 8.2, 7.35, 8.65, "oliv", ("z", -22.5, (4.1, 8, 8))), q(22.4, 23.2, 9.0, 10.6, 7.5, 8.5, "metall")]
W["nagelpistole"] = [q(4, 14, 8.6, 11.2, 7.1, 8.9, "rot"), q(14, 17, 9.4, 10.4, 7.6, 8.4, "metall"), q(6, 13, 7.6, 8.6, 7.4, 8.6, "metall"),
                     q(4.6, 7, 3.6, 8.6, 7.3, 8.7, "kunst", ("z", -22.5, (5.8, 8.6, 8))), q(10, 12.6, 4.4, 7.6, 7.45, 8.55, "messing"),
                     q(2.5, 4, 9, 10.8, 7.5, 8.5, "kunst")]
W["betaeubungspistole"] = [q(6, 15, 9.2, 10.6, 7.3, 8.7, "weiss"), q(15, 19, 9.6, 10.2, 7.7, 8.3, "chrom"), q(8, 12, 10.6, 11.6, 7.45, 8.55, "glas"),
                           q(4.6, 7.2, 4.2, 9.2, 7.3, 8.7, "kunst", ("z", -22.5, (5.9, 9, 8))), q(8.6, 10, 8.2, 9.2, 7.6, 8.4, "rot"),
                           q(4.2, 6, 10.2, 11.0, 7.6, 8.4, "chrom")]


def element(k):
    x0, x1, y0, y1, z0, z1, mat, dreh = k
    uv = feld_uv(M[mat])
    e = {"from": [x0, y0, z0], "to": [x1, y1, z1],
         "faces": {f: {"uv": uv, "texture": "#0"} for f in ("north", "south", "east", "west", "up", "down")}}
    if dreh:
        achse, winkel, ursprung = dreh
        e["rotation"] = {"angle": winkel, "axis": achse, "origin": list(ursprung)}
    return e


def modell(name, teile):
    xs = [v for t in teile for v in (t[0], t[1])]
    laenge = max(xs) - min(xs)
    mitte = (max(xs) + min(xs)) / 2
    gui = min(1.0, 15.0 / laenge)
    # Griff liegt bei x ≈ 5–8: Waffe so verschieben, dass sie in der Hand gut sitzt
    dx = 8 - mitte
    lang = laenge > 18
    return {
        "credit": "Deadzone MC",
        "textures": {"0": "deadzonemc:item/waffen/teile", "particle": "deadzonemc:item/waffen/teile"},
        "elements": [element(t) for t in teile],
        "display": {
            # Mündung zeigt vom Spieler weg (Rückmeldung 22.10.: vorher um 180° verdreht)
            "thirdperson_righthand": {"rotation": [0, 90, 0], "translation": [0, 2.5, 1.5 if lang else 1], "scale": [0.85, 0.85, 0.85]},
            "thirdperson_lefthand": {"rotation": [0, -90, 0], "translation": [0, 2.5, 1.5 if lang else 1], "scale": [0.85, 0.85, 0.85]},
            "firstperson_righthand": {"rotation": [0, 90, 0], "translation": [1.5, 2.5, -1 if lang else 0.5], "scale": [0.7, 0.7, 0.7]},
            "firstperson_lefthand": {"rotation": [0, -90, 0], "translation": [1.5, 2.5, -1 if lang else 0.5], "scale": [0.7, 0.7, 0.7]},
            "gui": {"rotation": [20, 0, 0], "translation": [dx * gui, 0, 0], "scale": [gui, gui, gui]},
            "ground": {"translation": [0, 2, 0], "scale": [0.45, 0.45, 0.45]},
            "fixed": {"translation": [dx * gui, 0, 0], "scale": [gui, gui, gui]},
            "head": {"translation": [0, 6, 0]},
        },
    }


# ---------------------------------------------------------------------------------------------
# Flache Item-Bilder: Munition, Aufsätze, Taschenlampe
# ---------------------------------------------------------------------------------------------
def c(s):
    return tuple(int(s[i:i + 2], 16) for i in (0, 2, 4)) + (255,)


def patronen(huelse, spitze, anzahl=3, kurz=False, schrot=False):
    b = Image.new("RGBA", (16, 16))
    d = ImageDraw.Draw(b)
    for i in range(anzahl):
        x = 3 + i * 4
        if schrot:
            d.rectangle((x, 4, x + 2, 11), fill=c(spitze), outline=c("4a0e0e"))
            d.rectangle((x, 11, x + 2, 13), fill=c(huelse), outline=c("5a4a14"))
        else:
            oben = 6 if kurz else 3
            d.rectangle((x, oben + 3, x + 2, 13), fill=c(huelse), outline=c("5a4a14"))
            d.rectangle((x, oben, x + 2, oben + 3), fill=c(spitze))
            b.putpixel((x + 1, oben - 1), c(spitze))
    return b


def bild(name, b):
    b.save(os.path.join(A, "textures", "item", name + ".png"))
    with open(os.path.join(A, "models", "item", name + ".json"), "w") as f:
        json.dump({"parent": "minecraft:item/generated", "textures": {"layer0": f"deadzonemc:item/{name}"}}, f, indent=1)


def zeichne(pixel, farben):
    b = Image.new("RGBA", (16, 16))
    for y, z in enumerate(pixel):
        for x, ch in enumerate(z):
            if ch != ".":
                b.putpixel((x, y), c(farben[ch]))
    return b


AUFSATZ_BILDER = {
    "schalldaempfer": ["................", "................", "................", "................", "................",
                       ".kkkkkkkkkkkk...", "kmmmmmmmmmmmmk..", "kmMMMMMMMMMMmkkk", "kmmmmmmmmmmmmkkk", ".kkkkkkkkkkkk...",
                       "................", "................", "................", "................", "................", "................"],
    "zielfernrohr": ["................", "................", "................", "................", ".kk.........kk..",
                     "kggk.kkkkk.kggk.", "kgGkkmmmmmkkgGk.", "kggkmmMMMmmkggk.", ".kk.kmmmmmk.kk..", "....kk...kk.....",
                     "................", "................", "................", "................", "................", "................"],
    "taschenlampe": ["................", "................", "................", "................", "................",
                     "........kkkk....", "kkkkkkkkmmmmk...", "kmmmmmmmmyyyk...", "kmrmmmmmmyYyk...", "kkkkkkkkmmmmk...",
                     "........kkkk....", "................", "................", "................", "................", "................"],
    "magazin": ["................", "......kkkk......", "......kmmk......", "......kmmk......", ".....kmmmk......", ".....kmmmk......",
                ".....kmmmk......", "....kmmmk.......", "....kmmmk.......", "....kmmmk.......", "...kmmmk........", "...kmmmk........",
                "...kMMMk........", "...kkkkk........", "................", "................"],
    "griff": ["................", "................", "................", "....kkkkkkk.....", "....kmmmmmk.....", ".....kmmmk......",
              ".....kgmgk......", ".....kmmmk......", ".....kgmgk......", ".....kmmmk......", ".....kgmgk......", ".....kmmmk......",
              ".....kkkkk......", "................", "................", "................"],
}
LAMPE = ["................", "................", "................", "...........kkk..", "..........kyyYk.", ".........kyYYyk.",
         "........kmmyyk..", ".......kmmmkk...", "......kmmmk.....", ".....kmrmk......", "....kmmmk.......", "...kmmmk........",
         "..kmmmk.........", "..kkkk..........", "................", "................"]


# ---------------------------------------------------------------------------------------------
# Klänge (synthetisch)
# ---------------------------------------------------------------------------------------------
RATE = 44100


def rauschen(n, rng):
    return rng.uniform(-1, 1, n)


def tiefpass(x, f):
    a = math.exp(-2 * math.pi * f / RATE)
    y = np.zeros_like(x)
    acc = 0.0
    for i in range(len(x)):
        acc = (1 - a) * x[i] + a * acc
        y[i] = acc
    return y


def huelle(n, anschlag, abklang):
    t = np.arange(n) / RATE
    return np.minimum(1, t / max(anschlag, 1e-4)) * np.exp(-t / abklang)


def schuss(dauer, knall_f, abklang, druck, hall, rng, scharf=0.6):
    n = int(dauer * RATE)
    t = np.arange(n) / RATE
    k = rauschen(n, rng) * huelle(n, 0.0005, abklang * 0.25)
    k = tiefpass(k, knall_f) * (1 - scharf) + k * scharf * huelle(n, 0.0003, abklang * 0.08)
    wumm = np.sin(2 * math.pi * (druck * (1 + 2 * np.exp(-t * 40))) * t) * huelle(n, 0.001, abklang * 0.6)
    schweif = tiefpass(rauschen(n, rng), 900) * huelle(n, 0.01, hall) * 0.35
    x = k * 1.0 + wumm * 0.8 + schweif
    return x / (np.max(np.abs(x)) + 1e-6) * 0.95


def klick(rng, tief=False, laenge=0.08):
    n = int(laenge * RATE)
    x = rauschen(n, rng) * huelle(n, 0.0002, 0.006)
    t = np.arange(n) / RATE
    x += np.sin(2 * math.pi * (1800 if not tief else 700) * t) * huelle(n, 0.0002, 0.012) * 0.5
    return x


def folge(teile, gesamt):
    n = int(gesamt * RATE)
    x = np.zeros(n)
    for start, teil, laut in teile:
        i = int(start * RATE)
        e = min(n, i + len(teil))
        x[i:e] += teil[:e - i] * laut
    return x / (np.max(np.abs(x)) + 1e-6) * 0.9


def speichern_ogg(name, x):
    p = os.path.join(A, "sounds", "waffe")
    os.makedirs(p, exist_ok=True)
    with tempfile.TemporaryDirectory() as tmp:
        wav = os.path.join(tmp, "a.wav")
        with wave.open(wav, "wb") as w:
            w.setnchannels(1)
            w.setsampwidth(2)
            w.setframerate(RATE)
            w.writeframes((np.clip(x, -1, 1) * 32000).astype(np.int16).tobytes())
        subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", wav, "-c:a", "libvorbis", "-q:a", "4",
                        os.path.join(p, name + ".ogg")], check=True)


SCHUSSKLANG = {
    #                  Dauer Knall  Abklang Druck Hall  Schärfe
    "p9": (0.7, 3200, 0.05, 90, 0.35, 0.65),
    "revolver": (1.0, 2400, 0.08, 70, 0.6, 0.55),
    "mp9": (0.5, 3400, 0.035, 95, 0.25, 0.7),
    "pumpgun": (1.3, 1500, 0.12, 55, 0.8, 0.45),
    "doppelflinte": (1.4, 1300, 0.14, 50, 0.9, 0.45),
    "sturmgewehr": (0.9, 4200, 0.05, 80, 0.55, 0.75),
    "kalaschnikow": (1.0, 3600, 0.06, 70, 0.6, 0.7),
    "jagdgewehr": (1.6, 3800, 0.08, 60, 1.1, 0.75),
    "praezisionsgewehr": (1.8, 4400, 0.08, 55, 1.3, 0.8),
    "mg": (0.8, 3800, 0.05, 75, 0.5, 0.72),
    "rohrpistole": (0.9, 1800, 0.07, 65, 0.4, 0.5),
    "rohrgewehr": (1.1, 1600, 0.09, 55, 0.6, 0.5),
    "karabiner": (1.0, 4000, 0.055, 75, 0.7, 0.75),
    "nagelpistole": (0.25, 5200, 0.012, 160, 0.08, 0.9),
    "betaeubungspistole": (0.3, 1200, 0.02, 120, 0.1, 0.25),
}


def klaenge():
    rng = np.random.default_rng(11)
    eintraege = {}
    for name, (dauer, knall, abkl, druck, hall, scharf) in SCHUSSKLANG.items():
        for v in range(3):
            x = schuss(dauer, knall * (0.92 + 0.08 * v), abkl, druck * (0.95 + 0.05 * v), hall, rng, scharf)
            speichern_ogg(f"{name}_{v + 1}", x)
        eintraege[f"waffe.{name}.schuss"] = [{"name": f"deadzonemc:waffe/{name}_{v + 1}", "attenuation_distance": 96}
                                            for v in range(3)]
    # Armbrust: Sehne schnalzt
    n = int(0.4 * RATE)
    t = np.arange(n) / RATE
    x = np.sin(2 * math.pi * 140 * t * (1 + np.exp(-t * 30))) * huelle(n, 0.001, 0.06) + rauschen(n, rng) * huelle(n, 0.0005, 0.02) * 0.5
    speichern_ogg("armbrust_1", x / np.max(np.abs(x)) * 0.8)
    eintraege["waffe.armbrust.schuss"] = [{"name": "deadzonemc:waffe/armbrust_1"}]
    # gedämpft
    for v in range(2):
        n = int(0.35 * RATE)
        x = tiefpass(rauschen(n, rng), 1100 + 300 * v) * huelle(n, 0.001, 0.03)
        speichern_ogg(f"gedaempft_{v + 1}", x / np.max(np.abs(x)) * 0.8)
    eintraege["waffe.gedaempft"] = [{"name": f"deadzonemc:waffe/gedaempft_{v + 1}"} for v in range(2)]
    speichern_ogg("laden", folge([(0.0, klick(rng, True), 0.8), (0.18, klick(rng), 0.6), (0.42, klick(rng, True, 0.12), 0.9)], 0.7))
    speichern_ogg("laden_fertig", folge([(0.0, klick(rng), 0.7), (0.09, klick(rng, True, 0.15), 1.0)], 0.4))
    speichern_ogg("patrone", folge([(0.0, klick(rng, True, 0.1), 0.9), (0.06, klick(rng), 0.4)], 0.25))
    speichern_ogg("leer", folge([(0.0, klick(rng, False, 0.05), 1.0)], 0.15))
    n = int(0.5 * RATE)
    t = np.arange(n) / RATE
    x = np.sin(2 * math.pi * (2600 - 1800 * t / 0.5) * t) * huelle(n, 0.001, 0.12) * 0.4 + rauschen(n, rng) * huelle(n, 0.0005, 0.015)
    speichern_ogg("einschlag", x / np.max(np.abs(x)) * 0.7)
    for k in ("laden", "laden_fertig", "patrone", "leer", "einschlag"):
        eintraege[f"waffe.{k}"] = [{"name": f"deadzonemc:waffe/{k}"}]
    return eintraege


def sounds_json_ergaenzen(eintraege):
    p = os.path.join(A, "sounds.json")
    daten = json.load(open(p))
    for k in list(daten):
        if k.startswith("waffe."):
            del daten[k]
    for k, v in eintraege.items():
        daten[k] = {"subtitle": f"subtitles.deadzonemc.{k}", "sounds": v}
    with open(p, "w") as f:
        json.dump(daten, f, indent=1)


# ---------------------------------------------------------------------------------------------
# Rezepte
# ---------------------------------------------------------------------------------------------
def geformt(name, muster, schluessel, ergebnis, anzahl=1):
    key = {k: ({"tag": v[1:]} if v.startswith("#") else {"item": v}) for k, v in schluessel.items()}
    with open(os.path.join(D, "recipe", name + ".json"), "w") as f:
        json.dump({"type": "minecraft:crafting_shaped", "category": "equipment", "pattern": muster, "key": key,
                   "result": {"id": ergebnis, "count": anzahl}}, f, indent=1)


def rezepte():
    os.makedirs(os.path.join(D, "recipe"), exist_ok=True)
    S, E, B = "deadzonemc:schrott", "deadzonemc:elektronik", "deadzonemc:batterie"
    geformt("munition_rohr", ["SG", "N "], {"S": S, "G": "minecraft:gunpowder", "N": "minecraft:iron_nugget"}, "deadzonemc:munition_rohr", 4)
    geformt("munition_bolzen", [" N", "/ ", "F "], {"N": "minecraft:iron_nugget", "/": "minecraft:stick", "F": "minecraft:feather"},
            "deadzonemc:munition_bolzen", 4)
    geformt("munition_9mm", ["NGN", " C "], {"N": "minecraft:iron_nugget", "G": "minecraft:gunpowder", "C": "minecraft:copper_ingot"},
            "deadzonemc:munition_9mm", 6)
    geformt("munition_schrot", ["P", "G", "N"], {"P": "minecraft:paper", "G": "minecraft:gunpowder", "N": "minecraft:iron_nugget"},
            "deadzonemc:munition_schrot", 3)
    # Gewehr- und Revolvermunition: Hülse aus Kupfer, Geschoss aus Eisen, viel Pulver
    N, G, C = "minecraft:iron_nugget", "minecraft:gunpowder", "minecraft:copper_ingot"
    geformt("munition_357", ["NNN", "GCG"], {"N": N, "G": G, "C": C}, "deadzonemc:munition_357", 4)
    geformt("munition_556", ["NGN", "GCG"], {"N": N, "G": G, "C": C}, "deadzonemc:munition_556", 6)
    geformt("munition_762", ["NGN", "GCG", "NCN"], {"N": N, "G": G, "C": C}, "deadzonemc:munition_762", 5)
    # Hülsen notfalls aus Schrott statt Kupfer (etwas weniger Ausbeute)
    geformt("munition_9mm_schrott", ["NGN", " S "], {"N": N, "G": G, "S": S}, "deadzonemc:munition_9mm", 4)
    geformt("munition_556_schrott", ["NGN", "GSG"], {"N": N, "G": G, "S": S}, "deadzonemc:munition_556", 4)
    # Schwarzpulver selbst mischen: Kohle, Knochenmehl (Salpeter), Feuerstein
    with open(os.path.join(D, "recipe", "schwarzpulver.json"), "w") as f:
        json.dump({"type": "minecraft:crafting_shapeless", "category": "misc",
                   "ingredients": [{"tag": "minecraft:coals"}, {"item": "minecraft:bone_meal"}, {"item": "minecraft:flint"}],
                   "result": {"id": "minecraft:gunpowder", "count": 2}}, f, indent=1)
    # Klassenwaffen und ihre Munition
    D_, P_ = "deadzonemc:drohnenteile", "#minecraft:planks"
    geformt("karabiner", ["IID", "PE "], {"I": "minecraft:iron_ingot", "D": D_, "P": P_, "E": E}, "deadzonemc:karabiner")
    geformt("nagelpistole", ["SSD", " B "], {"S": S, "D": D_, "B": B}, "deadzonemc:nagelpistole")
    geformt("betaeubungspistole", ["IGI", " H "], {"I": "minecraft:iron_ingot", "G": "minecraft:glass_bottle", "H": "deadzonemc:sehne"},
            "deadzonemc:betaeubungspistole")
    with open(os.path.join(D, "recipe", "munition_nagel.json"), "w") as f:
        json.dump({"type": "minecraft:crafting_shapeless", "category": "equipment",
                   "ingredients": [{"item": "deadzonemc:naegel"}, {"item": "deadzonemc:naegel"}, {"item": "deadzonemc:klebeband"}],
                   "result": {"id": "deadzonemc:munition_nagel", "count": 24}}, f, indent=1)
    with open(os.path.join(D, "recipe", "munition_spritze.json"), "w") as f:
        json.dump({"type": "minecraft:crafting_shapeless", "category": "equipment",
                   "ingredients": [{"item": "minecraft:glass_bottle"}, {"item": "deadzonemc:verseuchtes_gewebe"},
                                   {"item": "deadzonemc:heilkraut"}, {"item": "minecraft:iron_nugget"}],
                   "result": {"id": "deadzonemc:munition_spritze", "count": 4}}, f, indent=1)
    geformt("rohrpistole", ["SSS", " PT"], {"S": S, "P": "#minecraft:planks", "T": "minecraft:tripwire_hook"}, "deadzonemc:rohrpistole")
    geformt("rohrgewehr", ["SSS", "SST", "PP "], {"S": S, "P": "#minecraft:planks", "T": "minecraft:tripwire_hook"}, "deadzonemc:rohrgewehr")
    geformt("armbrust", ["IPI", "SPS", " P "], {"I": "minecraft:iron_ingot", "P": "#minecraft:planks", "S": "minecraft:string"},
            "deadzonemc:armbrust")
    geformt("aufsatz_schalldaempfer", [" I ", "IWI", " I "], {"I": "minecraft:iron_ingot", "W": "#minecraft:wool"}, "deadzonemc:aufsatz_schalldaempfer")
    geformt("aufsatz_zielfernrohr", ["IGI", " E "], {"I": "minecraft:iron_ingot", "G": "minecraft:glass", "E": E}, "deadzonemc:aufsatz_zielfernrohr")
    geformt("aufsatz_taschenlampe", ["IGB", " E "], {"I": "minecraft:iron_ingot", "G": "minecraft:glass_pane", "B": B, "E": E},
            "deadzonemc:aufsatz_taschenlampe")
    geformt("aufsatz_magazin", ["I", "I", "I"], {"I": "minecraft:iron_ingot"}, "deadzonemc:aufsatz_magazin")
    geformt("aufsatz_griff", [" I ", " L ", " L "], {"I": "minecraft:iron_ingot", "L": "minecraft:leather"}, "deadzonemc:aufsatz_griff")
    geformt("taschenlampe", ["IGI", "IBI", "IEI"], {"I": "minecraft:iron_nugget", "G": "minecraft:glass_pane", "B": B, "E": E},
            "deadzonemc:taschenlampe")


WURF_BILDER = {
    "handgranate": (["................", "......kk........", ".....kmmk.......", "......kkkk......", ".....kooook.....", "....kooOooook...",
                     "...koOoooooook..", "...kooooOoooook.", "...koooooooook..", "...kooOooooook..", "....koooooOok...", ".....kooooook...",
                     "......kkkkkk....", "................", "................", "................"],
                    {"k": "1a1e14", "m": "6a6e72", "o": "4a5428", "O": "5e6a34"}),
    "brandsatz": (["................", ".......rr.......", "......rYYr......", ".......ww.......", "......wwww......", "......kggk......",
                   ".....kggggk.....", ".....kgGggk.....", ".....kggggk.....", ".....kgggGk.....", ".....kggggk.....", ".....kggggk.....",
                   "......kkkk......", "................", "................", "................"],
                  {"k": "1a2a1a", "g": "4a7a3a", "G": "8ac87a", "w": "d8d0c0", "r": "e85a1e", "Y": "ffd84a"}),
    "rauchgranate": (["................", "......kk........", ".....kmmk.......", ".....kkkk.......", "....kssssk......", "....kSSSSk......",
                      "....kssssk......", "....kssssk......", "....kSSSSk......", "....kssssk......", "....kssssk......", "....kkkkkk......",
                      "................", "................", "................", "................"],
                     {"k": "1a1a1a", "m": "6a6e72", "s": "8a8e8a", "S": "d8d8d0"}),
    "blendgranate": (["................", "......kk........", ".....kmmk.......", ".....kkkk.......", "....kbbbbk......", "....kyyyyk......",
                      "....kbbbbk......", "....kbbbbk......", "....kyyyyk......", "....kbbbbk......", "....kbbbbk......", "....kkkkkk......",
                      "................", "................", "................", "................"],
                     {"k": "1a1a1a", "m": "6a6e72", "b": "2a3a5a", "y": "e8e04a"}),
    "laermkoeder": (["................", "................", "....kkkkkkk.....", "...kmmmmmmmk....", "...kmrkkkkmk....", "...kmkGGGkmk....",
                     "...kmkGgGkmk....", "...kmkkkkkmk....", "...kmmmmmmmk....", "...kmooomoomk...", "...kmmmmmmmk....", "....kkkkkkk.....",
                     "....k.....k.....", "................", "................", "................"],
                    {"k": "1a1a1a", "m": "5a5e62", "r": "e82a2a", "G": "6aff6a", "g": "2a8a2a", "o": "c8c8c0"}),
    "wurfmesser": (["................", "..............k.", ".............kM.", "............kMm.", "...........kMmk.", "..........kMmk..",
                    ".........kMmk...", "........kMmk....", ".......kMmk.....", "......kbkk......", ".....kbbk.......", "....kbbk........",
                    "...kbbk.........", "..kbbk..........", "..kkk...........", "................"],
                   {"k": "1a1a1a", "m": "9aa0a6", "M": "e0e4e8", "b": "4a3a2a"}),
    "sporenbombe": (["................", ".......kk.......", "......kwwk......", ".......kk.......", ".....kssssk.....", "....ksSssssk....",
                     "...ksssDsssDk...", "...kssssssssk...", "...ksDsssSssk...", "...kssssssssk...", "....ksssDssk....", ".....kssssk.....",
                     "......kkkk......", "................", "................", "................"],
                    {"k": "1a2014", "w": "d8d0c0", "s": "6a7a4a", "S": "a8c07a", "D": "d8f0a0"}),
}


def wurf_rezepte():
    S, E, B = "deadzonemc:schrott", "deadzonemc:elektronik", "deadzonemc:batterie"
    geformt("handgranate", [" I ", "SGS", " S "], {"I": "minecraft:iron_nugget", "S": S, "G": "minecraft:gunpowder"}, "deadzonemc:handgranate", 2)
    with open(os.path.join(D, "recipe", "brandsatz.json"), "w") as f:
        json.dump({"type": "minecraft:crafting_shapeless", "category": "equipment",
                   "ingredients": [{"item": "deadzonemc:wodka"}, {"item": "deadzonemc:stoffreste"}],
                   "result": {"id": "deadzonemc:brandsatz", "count": 1}}, f, indent=1)
    geformt("rauchgranate", [" I ", "CGC", " I "], {"I": "minecraft:iron_nugget", "C": "minecraft:charcoal", "G": "minecraft:gunpowder"},
            "deadzonemc:rauchgranate", 2)
    geformt("blendgranate", [" I ", "GSG", " I "], {"I": "minecraft:iron_nugget", "G": "minecraft:glowstone_dust", "S": "minecraft:gunpowder"},
            "deadzonemc:blendgranate", 2)
    geformt("laermkoeder", ["NEN", "NBN"], {"N": "minecraft:iron_nugget", "E": E, "B": B}, "deadzonemc:laermkoeder", 1)
    with open(os.path.join(D, "recipe", "wurfmesser.json"), "w") as f:
        json.dump({"type": "minecraft:crafting_shapeless", "category": "equipment",
                   "ingredients": [{"item": "minecraft:iron_ingot"}, {"item": "deadzonemc:sehne"}, {"item": "deadzonemc:knochensplitter"}],
                   "result": {"id": "deadzonemc:wurfmesser", "count": 3}}, f, indent=1)
    with open(os.path.join(D, "recipe", "sporenbombe.json"), "w") as f:
        json.dump({"type": "minecraft:crafting_shapeless", "category": "equipment",
                   "ingredients": [{"item": "deadzonemc:sporenbeutel"}, {"item": "minecraft:glass_bottle"}, {"item": "minecraft:string"}],
                   "result": {"id": "deadzonemc:sporenbombe", "count": 2}}, f, indent=1)


def main():
    for p in ("models/item", "textures/item"):
        os.makedirs(os.path.join(A, p), exist_ok=True)
    atlas()
    for name, teile in W.items():
        with open(os.path.join(A, "models", "item", name + ".json"), "w") as f:
            json.dump(modell(name, teile), f, indent=1)
    bild("munition_9mm", patronen("c8a03a", "b87a3a", 3, kurz=True))
    bild("munition_357", patronen("c8a03a", "a0a0a0", 3, kurz=True))
    bild("munition_schrot", patronen("c8a03a", "b82a2a", 3, schrot=True))
    bild("munition_556", patronen("c8a03a", "b87a3a", 3))
    bild("munition_762", patronen("a8902a", "6a5a2a", 3))
    bild("munition_nagel", zeichne(["................", "................", "................", "..kkkkkkkkkkkk..", "..kmmmmmmmmmmk..",
                                    "..kbbbbbbbbbbk..", "...m.m.m.m.m....", "...m.m.m.m.m....", "...m.m.m.m.m....", "...m.m.m.m.m....",
                                    "...M.M.M.M.M....", "................", "................", "................", "................",
                                    "................"], {"k": "2a2a2a", "m": "a8acb0", "M": "e0e4e8", "b": "c8a03a"}))
    bild("munition_spritze", zeichne(["................", "..............k.", ".............kr.", "............krk.", "...........kgk..",
                                      "..........kggk..", ".........kggk...", "........kggk....", ".......kGgk.....", "......kmmk......",
                                      ".....kmmk.......", "....kssk........", "...kssk.........", "..kkkk..........", ".k..............",
                                      "................"], {"k": "2a2a2a", "r": "c83a3a", "g": "8adaea", "G": "e8f8ff", "m": "a8acb0",
                                                           "s": "e0e0e0"}))
    bild("munition_rohr", zeichne(["................", "................", "................", "....kkk.........", "...kRRRk..kkk...",
                                   "...kRRRk.kRRRk..", "...kRrRk.kRRRk..", "...kRRRk.kRrRk..", "...kSSSk.kRRRk..", "...kSSSk.kSSSk..",
                                   "....kkk..kSSSk..", ".........kkkk...", "................", "................", "................",
                                   "................"], {"k": "2a1e14", "R": "7a3e1c", "r": "a85a2a", "S": "5a5a5a"}))
    bild("munition_bolzen", zeichne(["................", ".............k..", "............kk..", "...........kw...", "..........kw....",
                                     ".........kw.....", "........kw......", ".......kw.......", "......kw........", ".....kw.........",
                                     "....kw..........", "..fkw...........", ".ffk............", ".ff.............", "................",
                                     "................"], {"k": "6a4a2a", "w": "8a8e92", "f": "d8d4c8"}))
    farben = {"k": "1a1a1a", "m": "3a3c3e", "M": "5a5e62", "g": "3a6a8a", "G": "8ad8ff", "y": "e8e0a0", "Y": "fffff0", "r": "a82a1e"}
    for n, px in AUFSATZ_BILDER.items():
        bild("aufsatz_" + n, zeichne(px, farben))
    bild("taschenlampe", zeichne(LAMPE, farben))
    for n, (px, fb) in WURF_BILDER.items():
        bild(n, zeichne(px, fb))
    sounds_json_ergaenzen(klaenge())
    rezepte()
    wurf_rezepte()
    # Taschenlampe leuchtet auch in der Hand (LambDynamicLights)
    p = os.path.join(A, "dynamiclights", "item")
    os.makedirs(p, exist_ok=True)
    with open(os.path.join(p, "taschenlampe.json"), "w") as f:
        json.dump({"match": {"items": "deadzonemc:taschenlampe"}, "luminance": 10}, f, indent=1)
    print(len(W), "Waffenmodelle, Munition, Aufsätze, Klänge, Rezepte")
    if "--vorschau" in sys.argv:
        vorschau()


def vorschau():
    """Seitenansicht aller Modelle (einfach, ohne Drehungen) – zum Prüfen der Proportionen."""
    s = 8
    breite = 40 * s
    hoehe = 14 * s
    bilder = []
    for name, teile in W.items():
        b = Image.new("RGB", (breite, hoehe), (22, 24, 26))
        d = ImageDraw.Draw(b)
        for t in sorted(teile, key=lambda t: t[4]):
            x0, x1, y0, y1 = t[0] + 10, t[1] + 10, t[2], t[3]
            f = FELDER[M[t[6]]]
            d.rectangle((x0 * s, (14 - y1) * s, x1 * s, (14 - y0) * s), fill=c(f), outline=(10, 10, 10))
        d.text((6, 4), name, fill=(220, 220, 210))
        bilder.append(b)
    gesamt = Image.new("RGB", (breite * 2, hoehe * ((len(bilder) + 1) // 2)), (10, 10, 10))
    for i, b in enumerate(bilder):
        gesamt.paste(b, ((i % 2) * breite, (i // 2) * hoehe))
    os.makedirs(os.path.join(WURZEL, "build", "vorschau"), exist_ok=True)
    gesamt.save(os.path.join(WURZEL, "build", "vorschau", "waffen.png"))


if __name__ == "__main__":
    main()
