"""Kino-Intro „Flug Q-7“: der Transporthubschrauber und der Schatten des Ursprungs.

  python3 kino.py            – transporter.geo.json, Textur, Animationen, Schatten-Textur des Ursprungs, Vorschau

Der Hubschrauber schaut nach Norden (-z), rechts (+x) ist die große Schiebetür. Die Sitzplätze (in Pixeln, Ursprung
am Boden in der Mitte) stehen in client/kino/ClientKino.java – bei Änderungen dort mitziehen.
"""
import json
import os
import sys

import numpy as np
from PIL import Image

import animationen as AN
import entwuerfe as E
from maler import Maler, einfarbig, farbe, leuchtend, metall
from modell import Modell
from render import blatt


def blech(basis, fuge=16):
    """Großflächiges Blech: ruhige Fläche, Plattenfugen mit Nieten, ein paar Ölspuren."""
    b = farbe(basis)
    dunkel = b * 0.72
    niete = b * 1.25

    def malen(m, rect, flaeche, w):
        m.fuellen(rect, b, streuung=5, skala=8, rand=0.15)
        u, v, bb, hh = rect
        for y in range(hh):
            for x in range(bb):
                if (x % fuge == 0 and bb > fuge) or (y % fuge == 0 and hh > fuge):
                    m.setze(u + x, v + y, dunkel)
                elif ((x % fuge == 2 and y % 4 == 2) or (y % fuge == 2 and x % 4 == 2)) and bb > fuge and hh > fuge:
                    m.setze(u + x, v + y, niete)
        for _ in range(max(1, bb * hh // 900)):
            x0 = u + int(m.rng.integers(0, bb))
            y0 = v + int(m.rng.integers(0, hh))
            for k in range(int(m.rng.integers(3, 10))):
                m.setze(min(u + bb - 1, x0), min(v + hh - 1, y0 + k), b * 0.6)
    return malen


def oliv():
    return blech("4a5232")


def innen():
    return blech("34372f", fuge=12)


def glas():
    return einfarbig("1a2426", streuung=6)


def warn():
    g, s = farbe("c8a020"), farbe("1e1e1a")

    def malen(m, rect, flaeche, w):
        u, v, bb, hh = rect
        for y in range(hh):
            for x in range(bb):
                m.setze(u + x, v + y, g if ((x + y) // 3) % 2 == 0 else s)
    return malen


def kennung():
    """Schablonenschrift „Q-7“ und ein verblasster Stern auf der Flanke."""
    weiss = farbe("c8c4b0")

    def deko(m, rect, flaeche, w):
        if flaeche not in ("links", "rechts", "west", "ost"):
            pass
        u, v, bb, hh = rect
        if bb < 30 or hh < 20:
            return
        zeilen = ["XXX..X..XXXX",
                  "X.X..X.....X",
                  "X.X.XXX...X.",
                  "X.X..X...X..",
                  "XXX.....X...",
                  "..X.....X..."]
        x0, y0 = u + bb // 2 - 6, v + hh // 3
        for j, z in enumerate(zeilen):
            for i, c in enumerate(z):
                if c == "X":
                    m.setze(x0 + i, y0 + j, weiss)
    return deko


def transporter():
    m = Modell("transporter", 1024, 1024)
    o, i_, g = oliv(), innen(), glas()
    rumpf = m.k("rumpf", drehpunkt=(0, 0, 0))
    # Boden, Dach (vorn), linke Wand, rechte Wand vorn, über/unter der Tür, Schott
    rumpf.w((-28, 0, -72), (56, 3, 144), i_)
    rumpf.w((-28, 50, -72), (56, 3, 80), o)
    rumpf.w((-28, 3, -72), (3, 47, 144), o, deko=kennung())
    rumpf.w((25, 3, -72), (3, 47, 52), o, deko=kennung())
    rumpf.w((25, 40, -20), (3, 10, 40), o)
    rumpf.w((25, 3, -20), (3, 4, 40), warn())
    rumpf.w((-25, 3, -74), (50, 47, 2), i_)
    # Innen: Bänke, Haltegurte, Lampen, Kisten
    rumpf.w((-25, 14, -64), (8, 3, 120), einfarbig("2c2a24"))
    rumpf.w((-25, 17, -64), (1, 16, 120), einfarbig("4a4032"))
    for z in range(-56, 60, 18):
        rumpf.w((-15, 38, z), (1, 12, 1), einfarbig("6a5a3a"))
        rumpf.w((14, 38, z + 9), (1, 12, 1), einfarbig("6a5a3a"))
    rumpf.w((-8, 3, 36), (16, 14, 16), metall("4a4a3a", nieten=True))
    rumpf.w((-6, 17, 40), (12, 10, 12), metall("3a4a3a"))
    lampe = m.k("lampe", "rumpf", drehpunkt=(0, 48, -10))
    lampe.w((-2, 46, -12), (4, 3, 4), leuchtend("ff2a1a"))
    lampe.w((-2, 46, 30), (4, 3, 4), leuchtend("ff2a1a"))
    # Kufen
    for x in (-26, 22):
        rumpf.w((x, -8, -56), (4, 3, 100), einfarbig("2a2a26"))
        rumpf.w((x + 1, -5, -40), (2, 5, 2), einfarbig("2a2a26"))
        rumpf.w((x + 1, -5, 30), (2, 5, 2), einfarbig("2a2a26"))
    # Cockpit
    cock = m.k("cockpit", "rumpf", drehpunkt=(0, 0, -74))
    cock.w((-24, 3, -104), (48, 24, 30), o)
    cock.w((-22, 27, -100), (44, 18, 26), g)
    cock.w((-20, 6, -114), (40, 22, 10), o)
    cock.w((-18, 28, -110), (36, 10, 8), g)
    # Triebwerk
    rumpf.w((-16, 53, -50), (32, 14, 56), o)
    rumpf.w((-10, 56, 6), (20, 9, 10), einfarbig("1e1e1c"))
    # Abreißteile
    dach = m.k("dach_hinten", "rumpf", drehpunkt=(0, 53, 8))
    dach.w((-28, 50, 8), (56, 3, 64), o)
    wand = m.k("wand_hinten", "rumpf", drehpunkt=(26, 3, 20))
    wand.w((25, 3, 20), (3, 47, 52), o, deko=kennung())
    bank = m.k("bank_rechts", "wand_hinten", drehpunkt=(18, 14, 20))
    bank.w((17, 14, 24), (8, 3, 44), einfarbig("2c2a24"))
    rampe = m.k("rampe", "rumpf", drehpunkt=(0, 3, 72))
    rampe.w((-28, 3, 70), (56, 47, 3), o)
    rampe.w((-20, 10, 72), (40, 4, 1), warn())
    heck = m.k("heck", "rumpf", drehpunkt=(0, 40, 72))
    heck.w((-7, 30, 72), (14, 18, 100), o)
    heck.w((-4, 34, 172), (8, 11, 40), o)
    heck.w((-1, 40, 200), (2, 40, 14), o)
    heckrotor = m.k("heckrotor", "heck", drehpunkt=(3, 66, 207))
    heckrotor.w((2, 46, 205), (2, 40, 4), einfarbig("1c1c1a"))
    heckrotor.w((2, 64, 187), (2, 4, 40), einfarbig("1c1c1a"))
    rotor = m.k("rotor", "rumpf", drehpunkt=(0, 70, -22))
    rotor.w((-4, 67, -26), (8, 8, 8), einfarbig("1c1c1a"))
    rotor.w((4, 72, -25), (120, 2, 6), einfarbig("22221e"))
    rotor.w((-124, 72, -25), (120, 2, 6), einfarbig("22221e"))
    rotor.w((-3, 72.5, -146), (6, 2, 120), einfarbig("22221e"))
    rotor.w((-3, 72.5, -18), (6, 2, 120), einfarbig("22221e"))
    return m


def animationen():
    flug = AN.Anim("flug", 0.5)
    flug.tasten("rotor", "rotation", {0: (0, 0, 0), 0.25: (0, 180, 0), 0.5: (0, 360, 0)})
    flug.tasten("heckrotor", "rotation", {0: (0, 0, 0), 0.125: (180, 0, 0), 0.25: (360, 0, 0), 0.375: (540, 0, 0),
                                          0.5: (720, 0, 0)})
    flug.tasten("rumpf", "position", {0: (0, 0, 0), 0.25: (0, 0.3, 0), 0.5: (0, 0, 0)})
    # Rotor läuft aus (Schwerelosigkeit): ganz langsam
    still = AN.Anim("auslaufen", 4.0, schleife=None)
    still.tasten("rotor", "rotation", {0: (0, 0, 0), 1.0: (0, 360, 0), 2.5: (0, 540, 0), 4.0: (0, 580, 0)})
    # Der Rumpf reißt auf: Wand, Dach, Rampe und Heck treiben davon
    reissen = AN.Anim("zerreissen", 5.0, schleife=None)
    reissen.tasten("wand_hinten", "position", {0: (0, 0, 0), 0.4: (2, 0, 0), 5.0: (60, 12, 26)})
    reissen.tasten("wand_hinten", "rotation", {0: (0, 0, 0), 0.4: (0, -4, 0), 5.0: (20, -50, 70)})
    reissen.tasten("dach_hinten", "position", {0: (0, 0, 0), 0.6: (0, 2, 0), 5.0: (14, 70, 20)})
    reissen.tasten("dach_hinten", "rotation", {0: (0, 0, 0), 0.6: (3, 0, 0), 5.0: (60, 20, -30)})
    reissen.tasten("rampe", "position", {0: (0, 0, 0), 0.8: (0, 0, 2), 5.0: (-10, -30, 70)})
    reissen.tasten("rampe", "rotation", {0: (0, 0, 0), 0.8: (-4, 0, 0), 5.0: (-80, 15, 10)})
    reissen.tasten("heck", "rotation", {0: (0, 0, 0), 1.0: (0, 0, 3), 5.0: (-20, 25, 30)})
    reissen.tasten("heck", "position", {0: (0, 0, 0), 1.0: (0, 0, 1), 5.0: (20, -25, 40)})
    daten = {"format_version": "1.8.0", "animations": {f"animation.transporter.{a.name}": a.json()
                                                      for a in (flug, still, reissen)}}
    with open(os.path.join(AN.ZIEL, "transporter.animation.json"), "w") as f:
        json.dump(daten, f)


def schatten():
    """Der Ursprung als Schatten im Nebel: fast schwarz, nur das Auge glimmt violett (Glowmask)."""
    pfad = os.path.join(E.ASSETS, "textures", "entity")
    tex = np.array(Image.open(os.path.join(pfad, "der_ursprung.png")).convert("RGBA")).astype(float)
    glow = np.array(Image.open(os.path.join(pfad, "der_ursprung_glowmask.png")).convert("RGBA")).astype(float)
    dunkel = tex.copy()
    dunkel[..., :3] = tex[..., :3] * 0.09 + np.array([5, 2, 9])
    Image.fromarray(dunkel.clip(0, 255).astype(np.uint8)).save(os.path.join(pfad, "der_ursprung_schatten.png"))
    hell = glow[..., :3].max(axis=2, keepdims=True) / 255.0
    neu = np.zeros_like(glow)
    neu[..., :3] = hell * np.array([200, 110, 255])
    neu[..., 3] = glow[..., 3]
    Image.fromarray(neu.clip(0, 255).astype(np.uint8)).save(os.path.join(pfad, "der_ursprung_schatten_glowmask.png"))


def bauen():
    m = transporter()
    while True:
        try:
            m.uv_packen()
            break
        except ValueError:
            if m.th < m.tb:
                m.th *= 2
            else:
                m.tb *= 2
    maler = Maler(m, saat=77)
    maler.alles_malen()
    m.speichern(os.path.join(E.ASSETS, "geo", "entity", "transporter.geo.json"))
    maler.speichern(os.path.join(E.ASSETS, "textures", "entity", "transporter.png"),
                    os.path.join(E.ASSETS, "textures", "entity", "transporter_glowmask.png"))
    animationen()
    schatten()
    os.makedirs(E.VORSCHAU, exist_ok=True)
    b = blatt(m, maler.bild, maler.glow, "Transporter Q-7", "Kino-Intro", groesse_bloecke=(3.5, 4.6, 14),
              ansichten=((-40, 12), (90, 8), (215, 14)))
    ziel = os.path.join(E.VORSCHAU, "transporter.png")
    b.save(ziel)
    print("Transporter geschrieben, Vorschau:", ziel, "Textur", m.tb, "x", m.th)


if __name__ == "__main__":
    bauen()
