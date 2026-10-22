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


# ==== Boot-Intro „Kilo-2“ ===========================================================================

def kennung_k2():
    """Schablonenschrift „K-2“ am Bug."""
    weiss = farbe("c8c4b0")

    def deko(m, rect, flaeche, w):
        u, v, bb, hh = rect
        if bb < 24 or hh < 12:
            return
        zeilen = ["X..X.......XXX",
                  "X.X..........X",
                  "XX....XXX..XXX",
                  "X.X........X..",
                  "X..X.......XXX"]
        x0, y0 = u + bb // 2 - 7, v + hh // 3
        for j, z in enumerate(zeilen):
            for i, c in enumerate(z):
                if c == "X":
                    m.setze(x0 + i, y0 + j, weiss)
    return deko


def strahl(basis="ffffff", alpha=40):
    """Lichtkegel des Suchscheinwerfers: nur als schwaches, durchscheinendes Leuchten."""
    b = farbe(basis, alpha)

    def malen(m, rect, flaeche, w):
        u, v, bb, hh = rect
        m.bild[v:v + hh, u:u + bb] = 0
        n = rauschen_einfach(bb, hh, m.rng)
        # hell, aber fast durchsichtig: wirkt wie ein Lichtkegel im Regen
        m.glow[v:v + hh, u:u + bb, :3] = np.array([255, 244, 214]) * (0.85 + 0.15 * n[..., None])
        m.glow[v:v + hh, u:u + bb, 3] = b[3] * (0.8 + 0.2 * n)
    return malen


def rauschen_einfach(b, h, rng):
    x = rng.random((h, b))
    return x


def boot():
    """Patrouillenboot Kilo-2: grauer Rumpf mit rotem Unterwasseranstrich, spitzer Bug, Steuerhaus mit Fensterband,
    Mast mit Radar und Positionslicht, Suchscheinwerfer mit Lichtkegel, Bänke an Deck, Kisten, zwei Außenborder,
    Flagge am Heck. Wasserlinie bei y = 0, das Boot schaut nach Norden (-z)."""
    m = Modell("kino_boot", 1024, 1024)
    grau = blech("3d4248", fuge=20)
    rot = blech("4a1e1a", fuge=20)
    deck = einfarbig("2a2d30", streuung=10)
    dunkel = einfarbig("1c1d20")
    glas = einfarbig("141c22", streuung=5)
    rumpf = m.k("rumpf", drehpunkt=(0, 0, 0))
    # Rumpf: unten rot, oben grau; der Bug läuft in vier Stufen spitz zu und steigt leicht an
    rumpf.w((-36, -16, -64), (72, 14, 168), rot)
    rumpf.w((-36, -2, -64), (72, 18, 168), grau, deko=kennung_k2())
    for (halb, y0, y1, z0, z1) in ((30, -14, 18, -80, -64), (22, -10, 20, -94, -80), (14, -6, 22, -104, -94), (7, -2, 23, -110, -104)):
        rumpf.w((-halb, y0, z0), (2 * halb, y1 - y0, z1 - z0), grau)
    rumpf.w((-34, 16, -62), (68, 1, 164), deck)
    # Reling: Pfosten und Handlauf
    for x in (-35, 33):
        for z in range(-56, 101, 16):
            rumpf.w((x, 17, z), (2, 12, 2), einfarbig("6a6e70"))
        rumpf.w((x, 28, -60), (2, 2, 162), einfarbig("7a7e80"))
    rumpf.w((-28, 28, -68), (56, 2, 2), einfarbig("7a7e80"))
    # Fender (alte Reifen)
    for z in (-40, 0, 40, 80):
        rumpf.w((-38, 2, z), (2, 9, 9), dunkel)
        rumpf.w((36, 2, z), (2, 9, 9), dunkel)
    # Steuerhaus mit Fensterband, Tür und Rettungsring
    rumpf.w((-24, 17, -30), (48, 41, 44), grau)
    rumpf.w((-20, 40, -31), (40, 12, 1), glas)
    rumpf.w((-25, 40, -26), (1, 12, 34), glas)
    rumpf.w((24, 40, -26), (1, 12, 34), glas)
    rumpf.w((-8, 42, -31.5), (6, 8, 0.5), leuchtend("6a4a20"))  # schwaches Licht im Steuerhaus
    rumpf.w((24.5, 18, 0), (0.5, 22, 10), dunkel)  # Tür
    rumpf.w((-8, 26, 14), (16, 16, 1), einfarbig("c86020"))
    rumpf.w((-4, 30, 14.5), (8, 8, 1), dunkel)
    rumpf.w((-26, 58, -32), (52, 3, 48), einfarbig("2e3236"))
    # Mast mit Rahe und Positionslicht
    rumpf.w((-2, 61, 0), (4, 60, 4), einfarbig("4a4e52"))
    rumpf.w((-12, 108, 1), (24, 2, 2), einfarbig("4a4e52"))
    rumpf.w((-2, 121, 0), (4, 4, 4), leuchtend("ff2a1a"))
    rumpf.w((-13, 110, 1), (2, 2, 2), leuchtend("f0f0e0"))
    rumpf.w((11, 110, 1), (2, 2, 2), leuchtend("30ff60"))
    radar = m.k("radar", "rumpf", drehpunkt=(0, 100, 2))
    radar.w((-14, 98, 0), (28, 3, 4), einfarbig("2a2c2e"))
    # Suchscheinwerfer auf dem Dach, mit Lichtkegel nach vorn
    licht = m.k("scheinwerfer", "rumpf", drehpunkt=(0, 66, -24))
    licht.w((-5, 61, -30), (10, 10, 12), einfarbig("2a2c2e"))
    licht.w((-4, 62, -31), (8, 8, 1), leuchtend("fff2c0"))
    kegel = m.k("kegel", "scheinwerfer", drehpunkt=(0, 66, -31))
    kegel.w((-4, 62, -191), (8, 8, 160), strahl("ffffff", 46))
    kegel.w((-7, 59, -351), (14, 14, 160), strahl("ffffff", 22))
    # Bänke an Deck (hier sitzen die Spieler), Rückenlehnen an der Reling
    for x0, lehne in ((-32, -32), (22, 30)):
        rumpf.w((x0, 17, 22), (10, 8, 72), einfarbig("3a3226"))
        rumpf.w((lehne, 25, 22), (2, 9, 72), einfarbig("3a3226"))
    # Ladung: Kisten und Fässer
    rumpf.w((-10, 17, 70), (20, 14, 18), metall("4a5232", nieten=True))
    rumpf.w((-7, 31, 73), (14, 10, 12), metall("3e4630"))
    for x in (-16, 10):
        rumpf.w((x, 17, 56), (6, 12, 6), einfarbig("1e3a5a"))
    # Heck: zwei Außenborder, Flaggenstock
    for x in (-24, 12):
        rumpf.w((x, -20, 104), (12, 40, 12), dunkel)
        rumpf.w((x - 1, 20, 103), (14, 10, 14), einfarbig("4a4e52"))
    rumpf.w((-1, 17, 98), (2, 54, 2), einfarbig("6a6e70"))
    flagge = m.k("flagge", "rumpf", drehpunkt=(1, 62, 99))
    flagge.w((1, 52, 98.5), (26, 18, 1), einfarbig("8a8676", streuung=14))
    return m


def boot_animationen():
    fahrt = AN.Anim("fahrt", 6.0)
    fahrt.tasten("radar", "rotation", {0: (0, 0, 0), 3.0: (0, 180, 0), 6.0: (0, 360, 0)})
    fahrt.tasten("scheinwerfer", "rotation", {0: (0, 0, 0), 1.5: (4, 28, 0), 3.0: (0, 0, 0), 4.5: (3, -32, 0), 6.0: (0, 0, 0)})
    fahrt.tasten("flagge", "rotation", {0: (0, 0, 0), 0.75: (0, 14, 0), 1.5: (0, -6, 0), 2.25: (0, 18, 0), 3.0: (0, 0, 0),
                                        3.75: (0, 12, 0), 4.5: (0, -8, 0), 5.25: (0, 16, 0), 6.0: (0, 0, 0)})
    # Der Scheinwerfer zerspringt: kippt weg, der Kegel verschwindet
    aus = AN.Anim("licht_aus", 1.0, schleife=None)
    aus.tasten("scheinwerfer", "rotation", {0: (0, 0, 0), 0.2: (-25, 40, 10), 1.0: (-60, 70, 20)})
    aus.tasten("kegel", "scale", {0: (1, 1, 1), 0.1: (1.2, 1.2, 1), 0.15: (0.01, 0.01, 0.01), 1.0: (0.01, 0.01, 0.01)})
    daten = {"format_version": "1.8.0", "animations": {f"animation.kino_boot.{a.name}": a.json() for a in (fahrt, aus)}}
    with open(os.path.join(AN.ZIEL, "kino_boot.animation.json"), "w") as f:
        json.dump(daten, f)


def fensterraster():
    """Leere, dunkle Fensterreihen; manche Felder sind ausgebrochen (heller Beton dahinter fehlt)."""
    def deko(m, rect, flaeche, w):
        u, v, bb, hh = rect
        if bb < 6 or hh < 8 or flaeche in ("oben", "unten", "up", "down"):
            return
        for y in range(v + 2, v + hh - 2, 4):
            for x in range(u + 1, u + bb - 1, 3):
                if m.rng.random() < 0.85:
                    m.setze(x, y, farbe("121314"))
                    m.setze(x, y + 1, farbe("121314"))
        # Risse
        for _ in range(max(1, bb * hh // 300)):
            x0, y0 = u + int(m.rng.integers(0, bb)), v + int(m.rng.integers(0, hh))
            for k in range(int(m.rng.integers(4, 12))):
                m.setze(min(u + bb - 1, x0 + k // 2), min(v + hh - 1, y0 + k), farbe("0c0c0d"))
    return deko


def ruinen():
    """Kulisse: zerstörte Hochhäuser am Ufer, im Viertelmaßstab (der Renderer vergrößert x4). Ein paar Fenster
    leuchten noch, auf einem Dach brennt etwas."""
    m = Modell("kino_ruinen", 512, 512)
    beton = [einfarbig(c, streuung=16) for c in ("3a3c3e", "34363a", "403e3a", "2e3034")]
    warm = leuchtend("ffb050")
    kalt = leuchtend("9ac8ff")
    feuer = leuchtend("ff6a1a")
    rng = np.random.default_rng(41)
    k = m.k("stadt", drehpunkt=(0, 0, 0))
    x = -130
    while x < 130:
        b = int(rng.integers(14, 34))
        h = int(rng.integers(40, 130))
        t = int(rng.integers(14, 26))
        z0 = int(rng.integers(-8, 10))
        mat = beton[int(rng.integers(0, len(beton)))]
        k.w((x, 0, z0), (b, h, t), mat, deko=fensterraster())
        # abgebrochene Krone: zwei, drei Stufen
        oben = h
        for stufe in range(int(rng.integers(1, 4))):
            bb = int(b * rng.uniform(0.3, 0.7))
            hh = int(rng.integers(4, 14))
            xx = x + int(rng.integers(0, max(1, b - bb)))
            k.w((xx, oben, z0 + 2), (bb, hh, t - 4), mat)
            oben += hh
        # Fenster: wenige leuchten noch
        for _ in range(int(rng.integers(1, 6))):
            fx = x + int(rng.integers(2, b - 5))
            fy = int(rng.integers(6, h - 6))
            k.w((fx, fy, z0 - 0.5), (3, 4, 0.5), warm if rng.random() > 0.2 else kalt)
            if rng.random() < 0.5:
                k.w((fx, fy - 6, z0 - 0.5), (3, 4, 0.5), warm)
        if rng.random() < 0.18:
            k.w((x + b // 2 - 2, h, z0 + t // 2 - 2), (4, 3, 4), feuer)
        x += b + int(rng.integers(2, 12))
    # ein umgeknickter Turm und ein Baukran
    knick = m.k("knick", "stadt", drehpunkt=(60, 70, 0), drehung=(0, 0, -24))
    knick.w((52, 70, 0), (16, 50, 16), beton[1], deko=fensterraster())
    kran = m.k("kran", "stadt", drehpunkt=(-40, 0, 30))
    kran.w((-42, 0, 28), (3, 150, 3), einfarbig("2a2622"))
    kran.w((-70, 146, 28.5), (80, 3, 2), einfarbig("2a2622"))
    kran.w((-66, 120, 29), (1, 26, 1), einfarbig("1a1816"))
    kran.w((-42, 151, 28), (2, 2, 2), leuchtend("ff2a1a"))
    return m


def bauen_boot():
    for modell, saat, name in ((boot(), 91, "kino_boot"), (ruinen(), 92, "kino_ruinen")):
        while True:
            try:
                modell.uv_packen()
                break
            except ValueError:
                if modell.th < modell.tb:
                    modell.th *= 2
                else:
                    modell.tb *= 2
        maler = Maler(modell, saat=saat)
        maler.alles_malen()
        modell.speichern(os.path.join(E.ASSETS, "geo", "entity", f"{name}.geo.json"))
        maler.speichern(os.path.join(E.ASSETS, "textures", "entity", f"{name}.png"),
                        os.path.join(E.ASSETS, "textures", "entity", f"{name}_glowmask.png"))
        os.makedirs(E.VORSCHAU, exist_ok=True)
        if name == "kino_boot":
            b = blatt(modell, maler.bild, maler.glow, "Patrouillenboot Kilo-2", "Boot-Intro", groesse_bloecke=(4.8, 7.8, 14),
                      ansichten=((-40, 14), (90, 8), (215, 16)))
        else:
            b = blatt(modell, maler.bild, maler.glow, "Ruinen am Ufer", "Kulisse (im Spiel x4)", ansichten=((0, 4), (-30, 10), (30, 10)))
        b.save(os.path.join(E.VORSCHAU, f"{name}.png"))
        print(name, "Textur", modell.tb, "x", modell.th)
    boot_animationen()


if __name__ == "__main__":
    if "boot" in sys.argv:
        bauen_boot()
    else:
        bauen()
        bauen_boot()
