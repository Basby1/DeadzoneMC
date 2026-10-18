"""Die Monster von Deadzone MC als Modelle. Jede Funktion baut ein Modell samt Materialien.

  python3 entwuerfe.py [name …]   – erzeugt geo.json, Textur, Glowmask und ein Vorschaublatt
"""
import math
import os
import sys

from maler import (Maler, farbe, fleisch, haut, kombi, knochen, leuchtend, metall, nebel, stoff, einfarbig)
from modell import Modell
from render import blatt

WURZEL = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
ASSETS = os.path.join(WURZEL, "src", "main", "resources", "assets", "deadzonemc")
VORSCHAU = os.environ.get("VORSCHAU", os.path.join(WURZEL, "build", "vorschau"))


def gesicht(zeilen, palette, glow="", nur="vorne", dy=0, dx=0):
    def deko(m, rect, flaeche, w):
        if flaeche == nur:
            m.vorlage(rect, zeilen, palette, glow_zeichen=glow, dy=dy, dx=dx)
    return deko


def nur_flaeche(flaeche_name, fn):
    def deko(m, rect, flaeche, w):
        if flaeche == flaeche_name:
            fn(m, rect, flaeche, w)
    return deko


# ---------------------------------------------------------------------------
def wanderer():
    """Häufigster Gegner: ausgezehrter Infizierter in Bürokleidung, schlurft, sprintet plötzlich."""
    m = Modell("wanderer", 64, 64)
    hautf = "8e9883"
    h = haut(hautf, flecken_f="6c5a64", adern_f="4b4766", blut=1)
    hemd = stoff("b5ae98", haut_f=hautf, loecher=2, blut=2, falten=3)
    hemd_ueber = kombi(stoff("b5ae98", loecher=0, blut=3, falten=3, fransen=True))
    jeans = stoff("3b4759", haut_f=None, blut=1, falten=4, schmutz="2a2a22")
    schuh = einfarbig("2b2622", 6)

    def hemd_mit_loechern(mm, rect, fl, w):
        hemd_ueber(mm, rect, fl, w)
        if fl in ("vorne", "hinten", "rechts", "links"):
            mm.loecher(rect, None, anzahl=3, groesse=2, transparent=True)

    p = {"h": farbe("3a3a30"), "e": farbe("16120f"), "p": farbe("d8d6c4"), "n": farbe("4a3f38"),
         "t": farbe("c9c1a0"), "m": farbe("2a0d0d"), "b": farbe("5a1010")}
    kopf_gesicht = gesicht([
        "hh.hhhh",
        "h......",
        ".ee.ee.",
        ".ep.pe.",
        "b..n...",
        ".mtmtm.",
        ".mmmmm.",
    ], p)

    koerper = m.k("koerper", drehpunkt=(0, 12, 0))
    ober = m.k("oberkoerper", "koerper", drehpunkt=(0, 12, 0), drehung=(-16, 0, 4))
    ober.w((-3.5, 12, -1.75), (7, 11, 3.5), h)
    ober.w((-3.5, 12, -1.75), (7, 11, 3.5), hemd_mit_loechern, aufblasen=0.3)
    kopf = m.k("kopf", "oberkoerper", drehpunkt=(0, 23, 0), drehung=(12, -10, 14))
    kopf.w((-3.5, 23, -3.5), (7, 7, 7), h, deko=kopf_gesicht)
    kiefer = m.k("kiefer", "kopf", drehpunkt=(0, 24, -1), drehung=(28, 0, 0))
    kiefer.w((-2.5, 22, -3.6), (5, 2, 3.5), h, deko=gesicht([".ttt.", "mmmmm"], p, nur="oben"))
    ar = m.k("arm_rechts", "oberkoerper", drehpunkt=(4.75, 21.5, 0), drehung=(-8, 0, -6))
    ar.w((3.5, 9, -1.25), (2.5, 13, 2.5), h, deko=nur_flaeche("vorne", lambda mm, r, f, w: mm.spritzer(r, farbe("4a0c0c"), 3, 2)))
    ar.w((3.5, 17, -1.25), (2.5, 5, 2.5), stoff("b5ae98", loecher=0, blut=1, fransen=True), aufblasen=0.25)
    al = m.k("arm_links", "oberkoerper", drehpunkt=(-4.75, 21.5, 0), drehung=(-52, 8, 10))
    al.w((-6, 9.5, -1.25), (2.5, 12.5, 2.5), h)
    br = m.k("bein_rechts", "koerper", drehpunkt=(1.9, 12, 0), drehung=(6, 0, 0))
    br.w((0.15, 0, -1.6), (3.4, 12, 3.2), jeans)
    br.w((0.15, 0, -1.6), (3.4, 2, 3.2), schuh, aufblasen=0.2)
    bl = m.k("bein_links", "koerper", drehpunkt=(-1.9, 12, 0), drehung=(-14, 0, 2))
    bl.w((-3.55, 0, -1.6), (3.4, 12, 3.2), jeans)
    return m, "Wanderer", "Häufig · schlurft ziellos und sprintet ohne Vorwarnung los", "1,9 Blöcke"


def rippen(staerke=0.45, f="4e4a44"):
    def deko(m, rect, flaeche, w):
        if flaeche in ("vorne", "rechts", "links"):
            m.streifen(rect, farbe(f), abstand=2, senkrecht=False, staerke=staerke)
    return deko


def pusteln(anzahl=6, f="9cff4a"):
    gruen = farbe(f)
    rand = farbe("5f6b2a")

    def deko(m, rect, flaeche, w):
        u, v, b, h = rect
        if b < 3 or h < 3:
            return
        for _ in range(max(1, anzahl * b * h // 120)):
            x = u + m.rng.integers(1, b - 1)
            y = v + m.rng.integers(1, h - 1)
            for dx, dy in ((0, 0), (1, 0), (0, 1), (1, 1)):
                if (dx, dy) == (0, 0) or m.rng.random() < 0.35:
                    m.setze(x + dx, y + dy, gruen * (0.8 + 0.3 * m.rng.random()), glow=True)
            for dx, dy in ((-1, 0), (2, 0), (0, -1), (0, 2), (2, 1), (-1, 1)):
                m.setze(x + dx, y + dy, rand)
    return deko


def schreier():
    """Alarm: flieht, wenn man ihn sieht, und schreit alles in der Nähe herbei."""
    m = Modell("schreier", 64, 64)
    hautf = "a2a08f"
    h = haut(hautf, flecken_f="7d6e6a", adern_f="4f4a6a", blut=0)
    hemd = stoff("8fa7a1", loecher=0, blut=2, falten=2, fransen=True, schmutz="45463a")

    def kittel(mm, rect, fl, w):
        hemd(mm, rect, fl, w)
        if fl in ("hinten", "rechts", "links", "vorne"):
            mm.loecher(rect, None, anzahl=2, groesse=2, transparent=True)

    p = {"h": farbe("3b352e"), "x": farbe("2a2420"), "-": farbe("6d5a50"), "m": farbe("1a0606"),
         "r": farbe("6e1515"), "t": farbe("d9d2b4")}
    koerper = m.k("koerper", drehpunkt=(0, 14, 0))
    for seite, x0, rot in ((1, 0.5, (8, 0, 3)), (-1, -3, (-6, 0, -3))):
        b = m.k("bein_" + ("rechts" if seite > 0 else "links"), "koerper", drehpunkt=(seite * 1.75, 14, 0), drehung=rot)
        b.w((x0, 0, -1.25), (2.5, 14, 2.5), h)
    ober = m.k("oberkoerper", "koerper", drehpunkt=(0, 14, 0), drehung=(-34, 0, 0))
    ober.w((-3, 14, -1.5), (6, 11, 3), h, deko=rippen())
    ober.w((-3, 13, -1.5), (6, 12, 3), kittel, aufblasen=0.35)
    hals = m.k("hals", "oberkoerper", drehpunkt=(0, 25, 0), drehung=(-10, 0, 0))
    hals.w((-1, 25, -1), (2, 5, 2), h, deko=rippen(0.3, "5c544a"))
    kopf = m.k("kopf", "hals", drehpunkt=(0, 30, 0), drehung=(78, 0, -10))
    kopf.w((-3, 30, -3), (6, 7, 6), h, deko=gesicht([
        "hh.hhh",
        "x-xx-x",
        "tmmmmt",
        "tmrrmt",
        "tmrrmt",
        "tmrrmt",
        "mmmmmm",
    ], p))
    kiefer = m.k("kiefer", "kopf", drehpunkt=(0, 30, 0.5), drehung=(-38, 0, 0))
    kiefer.w((-2.5, 28, -3.2), (5, 2, 4), h, deko=gesicht(["ttttt", "mrrrm", "mrrrm", "ttttt"], p, nur="oben"))
    for seite in (1, -1):
        name = "rechts" if seite > 0 else "links"
        a = m.k("arm_" + name, "oberkoerper", drehpunkt=(seite * 3.75, 24, 0), drehung=(-12, 0, seite * 16))
        x0 = 3 if seite > 0 else -4.5
        a.w((x0, 7, -0.75), (1.5, 17, 1.5), h)
        f = m.k("finger_" + name, "arm_" + name, drehpunkt=(seite * 3.75, 7, 0), drehung=(-15, 0, -seite * 10))
        for i, dz in enumerate((-0.75, 0, 0.75)):
            f.w((x0 + 0.5, 3, dz - 0.25), (0.5, 4, 0.5), knochen("8a8170"))
    return m, "Schreier", "Alarm · flieht, wenn man ihn sieht, und schreit alles in der Nähe herbei", "2,1 Blöcke"


def lauerer():
    """Jumpscare: hockt in Schränken, unter Betten, an Decken und springt aus nächster Nähe heraus."""
    m = Modell("lauerer", 64, 64)
    hautf = "cfcabd"
    h = haut(hautf, flecken_f="9a8f88", adern_f="5b5470", blut=0, dunkel_unten=0.3)
    lumpen = stoff("1e1d1f", loecher=0, blut=1, falten=2, fransen=True, schmutz="121112")

    def lappen(mm, rect, fl, w):
        lumpen(mm, rect, fl, w)
        mm.loecher(rect, None, anzahl=3, groesse=2, transparent=True)

    p = {"e": farbe("050404"), "E": farbe("f2f0e6"), "t": farbe("e8e2c8"), "m": farbe("1c0a0a"), "g": farbe("7c1d1d")}
    koerper = m.k("koerper", drehpunkt=(0, 11, 0))
    for seite in (1, -1):
        name = "rechts" if seite > 0 else "links"
        x0 = 0.5 if seite > 0 else -2.5
        o = m.k("oberschenkel_" + name, "koerper", drehpunkt=(seite * 1.5, 11, 0), drehung=(78, 0, seite * 18))
        o.w((x0, 1, -1), (2, 10, 2), h)
        u = m.k("unterschenkel_" + name, "oberschenkel_" + name, drehpunkt=(seite * 1.5, 1, 0), drehung=(-150, 0, 0))
        u.w((x0, -9, -1), (2, 10, 2), h)
    ober = m.k("oberkoerper", "koerper", drehpunkt=(0, 11, 0), drehung=(-62, 0, 0))
    ober.w((-3, 11, -1.5), (6, 12, 3), h, deko=kombi(rippen(0.35), nur_flaeche("hinten", lambda mm, r, f, w: mm.streifen(r, farbe("e9e6dc"), 3, True, 0.0))))
    ober.w((-3, 15, -1.5), (6, 8, 3), lappen, aufblasen=0.3)
    kopf = m.k("kopf", "oberkoerper", drehpunkt=(0, 23, 0), drehung=(70, 0, 22))
    kopf.w((-3.5, 23, -3.5), (7, 6, 7), h, deko=gesicht([
        "eee.eee",
        "eEe.eEe",
        ".......",
        "ttttttt",
        "tmtmtmt",
        "gtttttg",
    ], p))
    for seite in (1, -1):
        name = "rechts" if seite > 0 else "links"
        x0 = 3 if seite > 0 else -5
        a = m.k("arm_" + name, "oberkoerper", drehpunkt=(seite * 4, 22, 0), drehung=(112, 0, seite * 14))
        a.w((x0, 4, -1), (2, 18, 2), h)
        k = m.k("krallen_" + name, "arm_" + name, drehpunkt=(seite * 4, 4, 0), drehung=(-25, 0, 0))
        for dx in (0, 0.75, 1.5):
            k.w((x0 + dx, -1, -1), (0.5, 5, 0.5), knochen("3a342c"))
    return m, "Lauerer", "Jumpscare · hockt in Schränken, unter Betten, an Decken", "1,4 Blöcke (geduckt)"


def kriecher():
    """Jäger: verdrehter Körper, läuft an Wänden und Decken, greift von oben an."""
    m = Modell("kriecher", 64, 64)
    hautf = "8a7b72"
    h = haut(hautf, flecken_f="5b3a3a", adern_f="3e2b3a", blut=2)
    roh = fleisch()
    p = {"m": farbe("150606"), "t": farbe("d8cfae"), "e": farbe("e8e4c8"), "r": farbe("6a1414")}
    rumpf = m.k("rumpf", drehpunkt=(0, 11, 0))
    rumpf.w((-3, 9, -6), (6, 4, 12), kombi(h, rippen(0.4)))
    rumpf.w((-2, 12.5, -5), (4, 1, 10), knochen("bdb39a"))  # Wirbelsäule (Bauch nach oben)
    rumpf.w((-3, 9, 2), (6, 4, 4), fleisch("5e1818", "8a3a30"), aufblasen=0.1)
    kopf = m.k("kopf", "rumpf", drehpunkt=(0, 10, -6), drehung=(-15, 0, 180))
    kopf.w((-3, 10, -11), (6, 6, 6), h, deko=gesicht([
        "......",
        ".e..e.",
        "......",
        "tttttt",
        "mrrrrm",
        "tttttt",
    ], p))
    for seite in (1, -1):
        for vorn, z in ((True, -4.5), (False, 4.5)):
            name = ("v_" if vorn else "h_") + ("rechts" if seite > 0 else "links")
            o = m.k("glied_o_" + name, "rumpf", drehpunkt=(seite * 3, 11, z),
                    drehung=(-25 if vorn else 25, 0, seite * 105))
            x0 = 2.25 if seite > 0 else -3.75
            o.w((x0, 1, z - 0.75), (1.5, 10, 1.5), h)
            u = m.k("glied_u_" + name, "glied_o_" + name, drehpunkt=(seite * 3, 1, z), drehung=(0, 0, -seite * 128))
            u.w((x0, -11, z - 0.75), (1.5, 12, 1.5), h)
            u.w((x0 - 0.25, -12, z - 1), (2, 2, 2), knochen("4a3f35"))
    return m, "Kriecher", "Jäger · läuft an Wänden und Decken, greift von oben an", "0,9 Blöcke hoch, 2 lang"


def der_stille():
    """Stalker: bewegt sich nur, wenn niemand hinsieht."""
    m = Modell("der_stille", 64, 64)
    mantel = stoff("1c1b1e", loecher=0, blut=0, falten=2, schmutz="0f0e10")
    mantel_unten = stoff("1c1b1e", loecher=0, blut=0, falten=2, fransen=True, schmutz="0f0e10")
    hand = haut("d9d4c8", flecken_f="b0a99a", blut=0, dunkel_unten=0.25)
    maske = haut("e9e4d8", flecken_f="cfc8b8", blut=0, dunkel_unten=0.1)
    p = {"c": farbe("4a4642"), "e": farbe("030303"), "s": farbe("8a847a")}
    koerper = m.k("koerper", drehpunkt=(0, 17, 0))
    for seite in (1, -1):
        b = m.k("bein_" + ("rechts" if seite > 0 else "links"), "koerper", drehpunkt=(seite * 1.5, 17, 0))
        b.w((0.25 if seite > 0 else -2.75, 0, -1.25), (2.5, 17, 2.5), einfarbig("141316", 6))
    rumpf = m.k("oberkoerper", "koerper", drehpunkt=(0, 17, 0), drehung=(-4, 0, 0))
    rumpf.w((-3.5, 17, -2), (7, 14, 4), mantel)
    rumpf.w((-4, 7, -2.5), (8, 12, 5), mantel_unten)
    rumpf.w((-3, 29, -2.25), (6, 3, 4.5), mantel, aufblasen=0.2)  # Kragen
    hals = m.k("hals", "oberkoerper", drehpunkt=(0, 31, 0), drehung=(0, 0, 4))
    hals.w((-1, 31, -1), (2, 3, 2), hand)
    kopf = m.k("kopf", "hals", drehpunkt=(0, 33, 0), drehung=(6, 0, 14))
    kopf.w((-3, 33, -3.5), (6, 8, 7), maske, deko=gesicht([
        "..c...",
        "..cc..",
        ".e.c..",
        "....c.",
        "....c.",
        "...c..",
        "......",
        "......",
    ], p))
    for seite in (1, -1):
        name = "rechts" if seite > 0 else "links"
        x0 = 3.5 if seite > 0 else -5.5
        a = m.k("arm_" + name, "oberkoerper", drehpunkt=(seite * 4.5, 30, 0), drehung=(-3, 0, seite * 3))
        a.w((x0, 12, -1), (2, 18, 2), mantel)
        a.w((x0 + 0.25, 9, -0.75), (1.5, 3, 1.5), hand)
        f = m.k("finger_" + name, "arm_" + name, drehpunkt=(seite * 4.5, 9, 0))
        for dz in (-0.75, 0, 0.75):
            f.w((x0 + 0.75, 4, dz - 0.25), (0.5, 5, 0.5), hand)
    return m, "Der Stille", "Stalker · bewegt sich nur, wenn niemand hinsieht", "2,6 Blöcke"


def imitator():
    """Psychologisch: sieht aus der Ferne aus wie ein Mitspieler, ahmt Schritte und Rufe nach."""
    m = Modell("imitator", 64, 64)
    hautf = "b9ab9a"
    h = haut(hautf, flecken_f="8f7f74", blut=0)
    jacke = stoff("4f5a3b", loecher=0, blut=1, falten=3, schmutz="2d3022")
    hose = stoff("4a4536", loecher=0, blut=0, falten=4)
    p = {"g": farbe("2c2e2b"), "l": farbe("a8e6b0"), "L": farbe("6fbf84"), "z": farbe("1d1e1c"), "r": farbe("626660")}
    koerper = m.k("koerper", drehpunkt=(0, 12, 0))
    for seite in (1, -1):
        b = m.k("bein_" + ("rechts" if seite > 0 else "links"), "koerper", drehpunkt=(seite * 2, 12, 0), drehung=(seite * 4, 0, 0))
        b.w((0 if seite > 0 else -4, 0, -2), (4, 12, 4), hose)
        b.w((0 if seite > 0 else -4, 0, -2), (4, 3, 4), einfarbig("2a2520", 6), aufblasen=0.25)
    rumpf = m.k("oberkoerper", "koerper", drehpunkt=(0, 12, 0))
    rumpf.w((-4, 12, -2), (8, 12, 4), jacke, deko=nur_flaeche("vorne", lambda mm, r, f, w: mm.streifen(r, farbe("1f231a"), 99, True, 0) or [mm.setze(r[0] + r[2] // 2, r[1] + y, farbe("8f9488")) for y in range(r[3])]))
    rumpf.w((-3.5, 13, 2), (7, 9, 3), stoff("5b4630", loecher=0, blut=0, falten=3))  # Rucksack
    hals = m.k("hals", "oberkoerper", drehpunkt=(0, 24, 0), drehung=(0, 0, 10))
    hals.w((-1, 24, -1), (2, 7, 2), h, deko=rippen(0.25, "8a7a6c"))
    kopf = m.k("kopf", "hals", drehpunkt=(0, 31, 0), drehung=(10, -15, 38))
    kopf.w((-4, 31, -4), (8, 8, 8), kombi(h, ), deko=gesicht([
        "gggggggg",
        "gggggggg",
        "gLlgglLg",
        "gllggllg",
        "gggggggg",
        "ggrzzrgg",
        "gggzzggg",
        ".gggggg.",
    ], p, glow="lL"))
    kopf.w((-1.5, 31.5, -5.5), (3, 3, 1.5), metall("3a3d3b", "5a3a22"))  # Filter
    for seite in (1, -1):
        name = "rechts" if seite > 0 else "links"
        x0 = 4 if seite > 0 else -7.5
        rot = (-8, 0, 4) if seite > 0 else (-95, 20, -8)
        a = m.k("arm_" + name, "oberkoerper", drehpunkt=(seite * 5.75, 22, 0), drehung=rot)
        a.w((x0, 8, -1.75), (3.5, 14, 3.5), jacke)
        a.w((x0, 6, -1.5), (3.5, 2, 3), h)
    return m, "Imitator", "Psychologisch · sieht aus der Ferne aus wie ein Mitspieler", "2,4 Blöcke"


def nebelwesen():
    """Wetter: existiert nur im dichten Nebel, sichtbar nur im Lichtkegel."""
    m = Modell("nebelwesen", 64, 64)
    n1 = nebel("cdd8da", 120, 230)
    n2 = nebel("b8c6c9", 70, 170)
    n3 = nebel("a9b8bb", 30, 120)
    kralle = knochen("2c3133")
    p = {"v": farbe("0b0e0f"), "a": farbe("d8fbff")}
    koerper = m.k("koerper", drehpunkt=(0, 18, 0), drehung=(-12, 0, 0))
    koerper.w((-3.5, 16, -2), (7, 10, 4), n1)
    koerper.w((-3, 10, -1.75), (6, 6, 3.5), n2)
    koerper.w((-2.5, 5, -1.5), (5, 5, 3), n3)
    koerper.w((-1.5, 1, -1), (3, 4, 2), n3)
    for i, (x, l) in enumerate(((-3, 10), (-1, 13), (1.5, 9), (3, 12))):
        f = m.k(f"faden_{i}", "koerper", drehpunkt=(x, 16, 0), drehung=(8 * (i - 1.5), 0, 6 * (i - 1.5)))
        f.w((x - 0.5, 16 - l, -0.5), (1, l, 1), n3)
    kopf = m.k("kopf", "koerper", drehpunkt=(0, 26, 0), drehung=(18, 0, -10))
    kopf.w((-3, 26, -3.5), (6, 8, 7), n1, deko=gesicht([
        "vvvvvv",
        "vvvvvv",
        "vavvav",
        "vvvvvv",
        "vvvvvv",
        "vvvvvv",
        "vvvvvv",
        "vvvvvv",
    ], p, glow="a"))
    kopf.w((-3, 26, -3.5), (6, 8, 7), n2, aufblasen=0.6)  # Kapuze
    for seite in (1, -1):
        name = "rechts" if seite > 0 else "links"
        x0 = 3.5 if seite > 0 else -5
        a = m.k("arm_" + name, "koerper", drehpunkt=(seite * 4.25, 25, 0), drehung=(-70, 0, seite * 6))
        a.w((x0, 9, -0.75), (1.5, 16, 1.5), n2)
        k = m.k("kralle_" + name, "arm_" + name, drehpunkt=(seite * 4.25, 9, 0), drehung=(-20, 0, 0))
        for dz in (-0.75, 0, 0.75):
            k.w((x0 + 0.5, 4, dz - 0.25), (0.5, 5, 0.5), kralle)
    return m, "Nebelwesen", "Wetter · existiert nur im dichten Nebel, sichtbar nur im Lichtkegel", "2,1 Blöcke"


def schwarm():
    """Masse: kleine, schnelle Kreaturen, die aus Nestern in Kanälen und Kellern strömen."""
    m = Modell("schwarm", 64, 64)
    hautf = "9b827c"
    h = haut(hautf, flecken_f="6e4b4b", adern_f="4a3040", blut=1)
    p = {"t": farbe("e3dcc0"), "m": farbe("1a0808"), "r": farbe("ff3b2b"), "g": farbe("6e1a1a")}
    koerper = m.k("koerper", drehpunkt=(0, 4, 0))
    koerper.w((-3, 2, -6), (6, 4, 12), h, deko=rippen(0.3))
    koerper.w((-2, 5.5, -4), (4, 1.5, 8), haut("8a716b", flecken_f="5a3e3e"))  # Buckel
    kopf = m.k("kopf", "koerper", drehpunkt=(0, 4, -6), drehung=(12, 0, 0))
    kopf.w((-2.5, 2.5, -10), (5, 3, 4), h, deko=gesicht([
        "r...r",
        "ttttt",
        "mmmmm",
    ], p, glow="r"))
    kiefer = m.k("kiefer", "kopf", drehpunkt=(0, 2.5, -7), drehung=(-30, 0, 0))
    kiefer.w((-2, 1.5, -9.8), (4, 1, 3.5), h, deko=gesicht(["tttt", "mmmm", "mmmm"], p, nur="oben"))
    schwanz = m.k("schwanz", "koerper", drehpunkt=(0, 5, 6), drehung=(-20, 15, 0))
    schwanz.w((-0.5, 4.5, 6), (1, 1, 10), h)
    for i, z in enumerate((-4, 0, 4)):
        for seite in (1, -1):
            b = m.k(f"bein_{i}_{'r' if seite > 0 else 'l'}", "koerper", drehpunkt=(seite * 3, 3, z),
                    drehung=((i - 1) * 20, 0, seite * 40))
            b.w((2.5 if seite > 0 else -3.5, -1, z - 0.5), (1, 4, 1), h)
    return m, "Schwarm", "Masse · viele kleine, schnelle Kreaturen aus Nestern", "0,4 Blöcke, in Gruppen von 8–20"


def brut():
    """Spezial: aufgeblähter Körper, platzt und verteilt Säure und Sporen."""
    m = Modell("brut", 64, 64)
    hautf = "a7a27a"
    h = haut(hautf, flecken_f="7a7a4a", adern_f="3a4422", blut=0)
    p = {"e": farbe("c8c8a0"), "d": farbe("2a2a18"), "m": farbe("3d3b1c"), "s": farbe("9cff4a")}
    koerper = m.k("koerper", drehpunkt=(0, 6, 0))
    for seite in (1, -1):
        b = m.k("bein_" + ("rechts" if seite > 0 else "links"), "koerper", drehpunkt=(seite * 3.5, 6, 0))
        b.w((1.5 if seite > 0 else -5.5, 0, -2), (4, 6, 4), h)
    koerper.w((-7, 4, -7), (14, 13, 13), h, deko=pusteln(2))
    ober = m.k("oberkoerper", "koerper", drehpunkt=(0, 16, 0), drehung=(-10, 0, 0))
    ober.w((-5, 16, -4), (10, 6, 8), h, deko=pusteln(1))
    kopf = m.k("kopf", "oberkoerper", drehpunkt=(0, 21, -2), drehung=(20, 0, 10))
    kopf.w((-2.5, 20, -6.5), (5, 5, 5), h, deko=gesicht([
        ".....",
        "dd.dd",
        "ed.de",
        ".....",
        ".mmm.",
    ], p, dy=0))
    for seite in (1, -1):
        name = "rechts" if seite > 0 else "links"
        a = m.k("arm_" + name, "oberkoerper", drehpunkt=(seite * 5.5, 21, 0), drehung=(-20, 0, seite * 35))
        a.w((5 if seite > 0 else -7, 13, -1), (2, 8, 2), h)
    return m, "Brut", "Spezial · aufgeblähter Körper, platzt und verteilt Säure und Sporen", "1,5 Blöcke"


def juggernaut():
    """Tank: langsam, gepanzert, bricht durch Wände. Schwachstelle am Rücken."""
    m = Modell("juggernaut", 128, 128)
    hautf = "8b7d70"
    h = haut("6f6258", flecken_f="4e3832", adern_f="3c2b33", blut=1)
    roh = fleisch("5e1818", "8a3a30")
    platte = metall("585d62", "7a3f1c")
    hose = stoff("3a3328", loecher=0, blut=2, falten=3, fransen=True)
    p = {"m": farbe("2e3134"), "o": farbe("ff8a2a"), "O": farbe("ffc46a"), "s": farbe("1a1b1c")}
    koerper = m.k("koerper", drehpunkt=(0, 16, 0))
    for seite in (1, -1):
        name = "rechts" if seite > 0 else "links"
        x0 = 1.5 if seite > 0 else -8.5
        b = m.k("bein_" + name, "koerper", drehpunkt=(seite * 5, 16, 0), drehung=(seite * 6, 0, seite * 3))
        b.w((x0, 0, -3.5), (7, 16, 7), hose)
        b.w((x0, 7, -3.5), (7, 5, 2), platte, aufblasen=0.4)
    ober = m.k("oberkoerper", "koerper", drehpunkt=(0, 16, 0), drehung=(-24, 0, 0))
    ober.w((-9, 16, -5), (18, 16, 10), kombi(h, rippen(0.15)))
    ober.w((-7, 22, -5), (14, 9, 2), platte, aufblasen=0.5)  # Brustplatte
    ober.w((-8, 17, -5), (6, 5, 2), roh, aufblasen=0.2)
    tumor = leuchtend("ff6a1a", 30)
    ober.w((-4, 21, 4.5), (8, 8, 3), kombi(fleisch("8a2a10", "ff8a2a"), tumor))  # Schwachstelle
    kopf = m.k("kopf", "oberkoerper", drehpunkt=(0, 31, -3), drehung=(22, 0, 0))
    kopf.w((-3.5, 30, -8), (7, 7, 7), platte, deko=gesicht([
        "mmmmmmm",
        "mmmmmmm",
        "sOoooOs",
        "mmmmmmm",
        "mmsmsmm",
        "mmmmmmm",
        "mmmmmmm",
    ], p, glow="oO"))
    for seite in (1, -1):
        name = "rechts" if seite > 0 else "links"
        x0 = 9 if seite > 0 else -16
        s = m.k("schulter_" + name, "oberkoerper", drehpunkt=(seite * 12, 30, 0), drehung=(0, 0, seite * 8))
        s.w((x0 - 0.5, 27, -5.5), (8, 6, 11), platte, aufblasen=0.3)
        a = m.k("arm_" + name, "oberkoerper", drehpunkt=(seite * 12.5, 29, 0), drehung=(-6, 0, seite * 6))
        a.w((x0, 7, -4), (8, 22, 8), h)
        a.w((x0, 14, -4), (8, 6, 8), platte, aufblasen=0.35)
        f = m.k("faust_" + name, "arm_" + name, drehpunkt=(seite * 12.5, 7, 0))
        f.w((x0 - 0.5, -1, -4.5), (9, 8, 9), kombi(h, ))
        f.w((x0 - 0.5, 3, -4.7), (9, 3, 2), metall("4a4e52", "6a3a1a"), aufblasen=0.3)  # Schlagring
    return m, "Juggernaut", "Tank · bricht durch Wände; Panzer erst abschießen, Schwachstelle am Rücken", "2,9 Blöcke"


ALLE = {"wanderer": wanderer, "schreier": schreier, "lauerer": lauerer, "kriecher": kriecher,
        "der_stille": der_stille, "imitator": imitator, "nebelwesen": nebelwesen, "schwarm": schwarm,
        "brut": brut, "juggernaut": juggernaut}


# ---------------------------------------------------------------------------
# Menschen: Fraktionen, Händler, Überlebende (gemeinsames Gerüst, eigene Kleidung)
def _mensch(name, hautf, gesicht_zeilen, palette, jacke, hose, schuhe, extras=None, glow="", tb=128, th=64,
            arm_pose=((-8, 0, 0), (-8, 0, 0))):
    m = Modell(name, tb, th)
    h = haut(hautf, flecken_f=None, blut=0, dunkel_unten=0.1)
    koerper = m.k("koerper", drehpunkt=(0, 12, 0))
    for seite in (1, -1):
        b = m.k("bein_" + ("rechts" if seite > 0 else "links"), "koerper", drehpunkt=(seite * 2, 12, 0))
        b.w((0 if seite > 0 else -4, 0, -2), (4, 12, 4), hose)
        b.w((0 if seite > 0 else -4, 0, -2), (4, 3, 4), schuhe, aufblasen=0.25)
    ober = m.k("oberkoerper", "koerper", drehpunkt=(0, 12, 0))
    ober.w((-4, 12, -2), (8, 12, 4), jacke)
    kopf = m.k("kopf", "oberkoerper", drehpunkt=(0, 24, 0))
    kopf.w((-4, 24, -4), (8, 8, 8), h, deko=gesicht(gesicht_zeilen, palette, glow=glow))
    for i, seite in enumerate((1, -1)):
        n = "rechts" if seite > 0 else "links"
        a = m.k("arm_" + n, "oberkoerper", drehpunkt=(seite * 5, 22, 0), drehung=arm_pose[i])
        x0 = 4 if seite > 0 else -7
        a.w((x0, 12, -1.5), (3, 12, 3), jacke)
        a.w((x0, 12, -1.5), (3, 2, 3), h)
    if extras:
        extras(m, {"koerper": koerper, "ober": ober, "kopf": kopf})
    return m


def kapuze_offen(m, rect, flaeche, w):
    """Vorn bleibt nur der Rand der Kapuze, damit das Gesicht zu sehen ist."""
    if flaeche == "vorne":
        u, v, b, h = rect
        m.bild[v + 1:v + h, u + 1:u + b - 1, 3] = 0


def soldat():
    """Die Wacht: Reste des Militärs. Helm, Gasmaske, Weste, Gewehr."""
    p = {"g": farbe("2c2e2b"), "l": farbe("b8c8b0"), "L": farbe("e8f0e0"), "z": farbe("1d1e1c"), "r": farbe("626660"),
         "s": farbe("3a3d38")}
    tarn = kombi(stoff("4f5a3b", loecher=0, blut=0, falten=4, schmutz="2d3022"),
                 lambda m, r, f, w: m.flecken(r, farbe("3a4128"), skala=2, schwelle=0.6, staerke=0.8))
    gesicht_ = ["ssssssss", "ssssssss", "gLlggLlg", "gllgglLg", "gggggggg", "ggrzzrgg", "gggzzggg", ".gggggg."]

    def extras(m, k):
        helm = metall("4a5238", "5a4a2a", nieten=False)
        k["kopf"].w((-4, 29, -4), (8, 3, 8), helm, aufblasen=0.6)
        k["kopf"].w((-1.5, 24.5, -5.5), (3, 3, 1.5), metall("3a3d3b", "5a3a22"))
        k["ober"].w((-4, 14, -2), (8, 9, 4), stoff("3a3f2c", loecher=0, blut=0, falten=2), aufblasen=0.5)  # Weste
        k["ober"].w((-3.5, 14, 2.4), (7, 8, 2.5), stoff("4a4630", loecher=0, blut=0, falten=3))
        gewehr = m.k("gewehr", "arm_rechts", drehpunkt=(5, 13, 0), drehung=(-80, 0, 0))
        gewehr.w((4.5, 12.5, -10), (1, 2, 12), einfarbig("1e1f1d", 6))
        gewehr.w((4.5, 10.5, -4), (1, 2, 1.5), einfarbig("2a2b28", 6))
    m = _mensch("soldat", "b39a85", gesicht_, p, tarn, stoff("4a5038", loecher=0, blut=0, falten=4),
                einfarbig("1c1a17", 6), extras, glow="lL", arm_pose=((-40, -10, 0), (-50, 30, 0)))
    return m, "Soldat der Wacht", "Fraktion · Reste des Militärs, streng und misstrauisch", "1,9 Blöcke"


def kultist():
    """Die Aschekinder: Sekte, die den Nebel verehrt. Kutte, Asche im Gesicht, Laterne."""
    p = {"a": farbe("6e6a64"), "w": farbe("e8ecee"), "k": farbe("1a1818"), "m": farbe("3a3030"), "s": farbe("4a4644")}
    kutte = stoff("5c5853", loecher=0, blut=0, falten=2, schmutz="3a3835")
    gesicht_ = ["........", "........", "aakaakaa", "awkaawka", "aaaaaaaa", "asaaaasa", "aammmmaa", "aaaaaaaa"]

    def extras(m, k):
        k["kopf"].w((-4, 24, -4), (8, 8, 8), stoff("4a4743", loecher=0, blut=0, falten=2), aufblasen=0.5, deko=kapuze_offen)
        k["ober"].w((-4.5, 3, -2.5), (9, 10, 5), stoff("5c5853", loecher=1, blut=0, falten=2, fransen=True))
        laterne = m.k("laterne", "arm_links", drehpunkt=(-5.5, 12, 0))
        laterne.w((-6.5, 6, -1), (2, 3, 2), kombi(metall("2a2a2a", "3a2a1a", nieten=False)))
        laterne.w((-6, 6.5, -0.5), (1, 2, 1), leuchtend("9fd8ff", 20))
    m = _mensch("kultist", "9a948c", gesicht_, p, kutte, kutte, einfarbig("2a2724", 5), extras, glow="w",
                arm_pose=((-5, 0, 4), (-30, 0, -6)))
    return m, "Aschekind", "Fraktion · Sekte, die den Nebel verehrt", "1,9 Blöcke"


def haendler():
    """Die Karawane: Händler mit riesigem Rucksack, Hut und Schal."""
    p = {"h": farbe("4a3020"), "e": farbe("1a1410"), "w": farbe("e8e0d0"), "b": farbe("6a4a30"), "m": farbe("5a2a20"),
         "s": farbe("4f5a4a"), "S": farbe("3e483a")}
    mantel = stoff("6b5137", loecher=0, blut=0, falten=3, schmutz="3a2a1a")
    gesicht_ = ["hhhhhhhh", "hh....hh", ".we..ew.", "...bb...", ".b....b.", "bbmmmmbb", "sSsSsSsS", "SsSsSsSs"]

    def extras(m, k):
        k["kopf"].w((-6, 31, -6), (12, 1, 12), stoff("3e2e20", loecher=0, blut=0, falten=4))  # Hutkrempe
        k["kopf"].w((-4, 32, -4), (8, 2, 8), stoff("3e2e20", loecher=0, blut=0, falten=4))
        ruck = m.k("rucksack", "oberkoerper", drehpunkt=(0, 20, 2))
        ruck.w((-5, 10, 2), (10, 16, 6), stoff("7a6040", loecher=0, blut=0, falten=3))
        ruck.w((-6, 26, 3), (12, 3, 5), stoff("5a4a30", loecher=0, blut=0, falten=2))  # Schlafsack
        ruck.w((5, 12, 4), (2, 3, 2), metall("6a6e70", "5a3a22", nieten=False))  # Topf
        ruck.w((-7, 14, 3), (2, 6, 2), einfarbig("3a3a30", 6))  # Feldflasche
    m = _mensch("haendler", "c4a084", gesicht_, p, mantel, stoff("4a4030", loecher=0, blut=0, falten=4),
                einfarbig("2a2018", 6), extras, arm_pose=((-6, 0, 3), (-6, 0, -3)))
    return m, "Händler der Karawane", "Neutral · tauscht Beute gegen Beute, Angebot wechselt", "1,9 Blöcke"


def ueberlebender():
    """Einzelne Überlebende: Kapuzenpulli, Halstuch, Jeans. Man weiß nie, was sie vorhaben."""
    p = {"h": farbe("3a2a1e"), "e": farbe("1a1410"), "w": farbe("e8e0d0"), "b": farbe("7a2a2a"), "B": farbe("9a3a3a")}
    pulli = stoff("5a6a72", loecher=1, blut=1, falten=3, haut_f="b8987a")
    gesicht_ = ["hhhhhhhh", "hh....hh", ".we..ew.", "........", "BbBbBbBb", "bBbBbBbB", "BbBbBbBb", "........"]

    def extras(m, k):
        k["kopf"].w((-4, 24, -4), (8, 8, 8), stoff("4a5a62", loecher=0, blut=0, falten=2), aufblasen=0.45, deko=kapuze_offen)  # Kapuze
        k["ober"].w((-3.5, 13, 2), (7, 8, 3), stoff("3a4a3a", loecher=0, blut=0, falten=3))
    m = _mensch("ueberlebender", "b8987a", gesicht_, p, pulli, stoff("3b4759", loecher=1, blut=1, falten=4, haut_f="b8987a"),
                einfarbig("2a2522", 6), extras, arm_pose=((-10, 0, 3), (-14, 0, -3)))
    return m, "Überlebender", "Unberechenbar · hilfsbereit, Dieb oder infiziert", "1,9 Blöcke"


ALLE.update({"soldat": soldat, "kultist": kultist, "haendler": haendler, "ueberlebender": ueberlebender})


# ---------------------------------------------------------------------------
# Zweite Welle: zehn weitere Gegner (zwei davon fliegen)

def glutrisse(anzahl=5, f="ff7a1a"):
    """Glühende Risse in verkohlter Haut."""
    glut = farbe(f)
    hell = farbe("ffd27a")

    def deko(m, rect, flaeche, w):
        u, v, b, h = rect
        if b < 2 or h < 2:
            return
        for _ in range(max(1, anzahl * b * h // 60)):
            x = u + int(m.rng.integers(0, b))
            y = v + int(m.rng.integers(0, h))
            for _ in range(int(m.rng.integers(2, 6))):
                if u <= x < u + b and v <= y < v + h:
                    m.setze(x, y, hell if m.rng.random() < 0.25 else glut, glow=True)
                x += int(m.rng.integers(-1, 2))
                y += int(m.rng.integers(0, 2))
    return deko


def zerfetzt(anzahl=3, groesse=2):
    def deko(m, rect, flaeche, w):
        if flaeche in ("oben", "unten"):
            m.loecher(rect, None, anzahl=anzahl, groesse=groesse, transparent=True)
    return deko


def skalieren(m, f):
    """Ganzes Modell um den Ursprung vergrößern (für Tiere, deren Proportionen sonst zu zierlich wirken)."""
    for kn in m.knochen:
        kn.drehpunkt = [v * f for v in kn.drehpunkt]
        for w in kn.wuerfel:
            w.ursprung = [v * f for v in w.ursprung]
            w.groesse = [v * f for v in w.groesse]
    return m


def gefallener_soldat():
    """Tank-light: infizierter Soldat mit Helm, Weste und Rucksack. Zieht sein Gewehr hinter sich her,
    ein Arm ist gebrochen. Körpertreffer prallen ab – nur der Kopf zählt."""
    m = Modell("gefallener_soldat", 128, 64)
    hautf = "7f8a78"
    h = haut(hautf, flecken_f="5a5560", adern_f="3b3a5a", blut=2)
    tarn = kombi(stoff("4f5a3b", haut_f=hautf, loecher=2, blut=3, falten=4, schmutz="2d3022"),
                 lambda mm, r, f, w: mm.flecken(r, farbe("3a4128"), skala=2, schwelle=0.6, staerke=0.8))
    weste = kombi(stoff("2f3326", loecher=0, blut=2, falten=2, schmutz="1e2018"),
                  lambda mm, r, f, w: mm.streifen(r, farbe("23261c"), abstand=3, senkrecht=False, staerke=0.4))
    tasche = stoff("3a3f2c", loecher=0, blut=0, falten=1)
    helm = metall("4a5238", "5a4a2a", nieten=False)
    stahl = einfarbig("1e1f1d", 6)
    p = {"e": farbe("d8dccc"), "E": farbe("9aa090"), "m": farbe("2a0d0d"), "t": farbe("c9c1a0"), "b": farbe("5a1010")}
    koerper = m.k("koerper", drehpunkt=(0, 12, 0), drehung=(0, 0, 5))
    br = m.k("bein_rechts", "koerper", drehpunkt=(2, 12, 0), drehung=(-6, 0, 0))
    br.w((0, 0, -2), (4, 12, 4), stoff("4a5038", loecher=1, blut=2, falten=4, haut_f=hautf))
    br.w((0, 0, -2), (4, 4, 4), stahl, aufblasen=0.25)
    bl = m.k("bein_links", "koerper", drehpunkt=(-2, 12, 0), drehung=(-24, 0, -4))  # wird hinterhergezogen
    bl.w((-4, 0, -2), (4, 12, 4), stoff("4a5038", loecher=2, blut=3, falten=4, haut_f=hautf))
    bl.w((-4, 0, -2), (4, 3, 4), stahl, aufblasen=0.25)
    ober = m.k("oberkoerper", "koerper", drehpunkt=(0, 12, 0), drehung=(-14, 0, -10))  # nach vorn gekrümmt
    ober.w((-4, 12, -2), (8, 12, 4), tarn)
    ober.w((-4, 14, -2), (8, 9, 4), weste, aufblasen=0.7)
    for x in (-3.5, -1, 1.5):
        ober.w((x, 14.5, -3.3), (2, 3, 1.2), tasche)
    ruck = m.k("rucksack", "oberkoerper", drehpunkt=(0, 18, 3))
    ruck.w((-4, 12, 2.6), (8, 10, 5), stoff("4a4630", loecher=1, blut=0, falten=3))
    ruck.w((-4.5, 22, 3), (9, 3, 4), stoff("3a4a2a", loecher=0, blut=0, falten=4))  # Schlafsack
    ruck.w((2.5, 22, 5), (0.5, 9, 0.5), stahl)  # Funkantenne
    kopf = m.k("kopf", "oberkoerper", drehpunkt=(0, 24, 0), drehung=(-20, 0, 26))  # hängt schief nach vorn
    kopf.w((-4, 24, -4), (8, 8, 8), h, deko=gesicht([
        "........",
        "........",
        ".eE..Ee.",
        "........",
        "b...b...",
        ".mtmtmt.",
        ".mmmmmm.",
        "........",
    ], p))
    kopf.w((-4, 29, -4), (8, 3, 8), helm, aufblasen=0.7)
    kopf.w((-4.5, 26, 1), (9, 4, 3.5), helm, aufblasen=0.3)
    kopf.w((-1.5, 22, -5), (3, 3, 1.5), metall("3a3d3b", "5a3a22"))  # Gasmaske hängt am Hals
    ar = m.k("arm_rechts", "oberkoerper", drehpunkt=(5, 22, 0), drehung=(6, 0, 10))
    ar.w((4, 10, -1.5), (3, 12, 3), tarn)
    ar.w((4, 9, -1.5), (3, 2, 3), h)
    gewehr = m.k("gewehr", "arm_rechts", drehpunkt=(5.5, 9.5, 0), drehung=(-28, 0, 0))
    gewehr.w((5, -6, -0.5), (1, 16, 2), stahl)  # am Lauf gepackt, Kolben schleift am Boden
    gewehr.w((5, -7, -0.75), (1, 4, 2.5), einfarbig("3a2a1a", 6))
    gewehr.w((5, 4, 1.5), (1, 3, 1.2), stahl)  # Magazin
    al = m.k("arm_links", "oberkoerper", drehpunkt=(-5, 22, 0), drehung=(0, 0, -6))
    al.w((-7, 15, -1.5), (3, 7, 3), tarn)
    ua = m.k("unterarm_links", "arm_links", drehpunkt=(-5.5, 15, 0), drehung=(-55, 0, 25))  # gebrochen
    ua.w((-7, 6, -1.5), (3, 9, 3), h)
    ua.w((-6.2, 13.5, 0.2), (1, 2.5, 1), knochen("e0d8c0"))  # Knochen ragt heraus
    return m, "Gefallener Soldat", "Tank · Helm und Weste schlucken Körpertreffer, nur Kopfschüsse zählen", "1,9 Blöcke"


def glutleib():
    """Brandopfer: verkohlter Koloss, geht auf den Knöcheln wie ein Gorilla, Glutschlote auf dem Rücken."""
    m = Modell("glutleib", 128, 64)
    asche = kombi(haut("2a2220", flecken_f="141010", blut=0, dunkel_unten=0.2), glutrisse(1))
    kruste = kombi(knochen("3a3028"), glutrisse(1))
    schlot = kombi(einfarbig("1a1412", 6), glutrisse(3))
    glut = leuchtend("ff8a2a", 25)
    p = {"o": farbe("ffb04a"), "O": farbe("ffe7a0"), "k": farbe("100c0a"), "z": farbe("6a5a48")}
    koerper = m.k("koerper", drehpunkt=(0, 10, 0))
    for seite in (1, -1):
        b = m.k("bein_" + ("rechts" if seite > 0 else "links"), "koerper", drehpunkt=(seite * 3, 10, 0),
                drehung=(-18, 0, seite * 10))
        b.w((seite * 3 - 2, 0, -2), (4, 10, 4), asche)
    ober = m.k("oberkoerper", "koerper", drehpunkt=(0, 10, 0), drehung=(-58, 0, 0))
    ober.w((-5, 10, -3), (10, 12, 6), kombi(asche, rippen(0.35, "100c0a")))
    ober.w((-6.5, 18, -3.5), (13, 5, 7), kruste)  # massiger Schulterbuckel
    for i, (x, y, hoehe) in enumerate(((-3, 15, 5), (0.5, 17, 7), (3, 13, 4))):
        s = m.k(f"schlot_{i}", "oberkoerper", drehpunkt=(x, y, 3), drehung=(-20 + i * 12, 0, (i - 1) * 15))
        s.w((x - 1, y, 2.5), (2, hoehe, 2), schlot)
        s.w((x - 0.75, y + hoehe - 0.5, 2.75), (1.5, 1, 1.5), glut)
    kopf = m.k("kopf", "oberkoerper", drehpunkt=(0, 19, -2), drehung=(62, 0, 0))
    kopf.w((-3, 16, -8), (6, 6, 6), kruste, deko=gesicht([
        "......",
        "kOkkOk",
        "kokkok",
        "......",
        "zoooz.",
        "kooook",
    ], p, glow="oO"))
    for seite in (1, -1):
        name = "rechts" if seite > 0 else "links"
        x0 = 5 if seite > 0 else -9
        a = m.k("arm_" + name, "oberkoerper", drehpunkt=(seite * 6.5, 20, 0), drehung=(56, 0, seite * -10))
        a.w((x0, 4, -2), (4, 16, 4), asche)
        f = m.k("faust_" + name, "arm_" + name, drehpunkt=(seite * 7, 4, 0))
        f.w((x0 - 0.5, -1, -2.5), (5, 5, 5), kruste)
    return m, "Glutleib", "Brandopfer · geht auf den Knöcheln, Glutschlote auf dem Rücken, setzt dich in Brand", "2 Blöcke"


def sauger():
    """Jäger: geduckt wie eine Gottesanbeterin, Klauen vor dem Gesicht gefaltet. Fast unsichtbar beim Anschleichen."""
    m = Modell("sauger", 128, 64)
    h = haut("8a5a4a", flecken_f="6a3a30", adern_f="4a1a2a", blut=1)
    roh = fleisch("7a2a24", "a8483a")
    klaue = knochen("2e2620")
    p = {"e": farbe("ffd84a"), "E": farbe("fff2b0"), "k": farbe("2a1410")}
    koerper = m.k("koerper", drehpunkt=(0, 12, 0))
    for seite in (1, -1):
        name = "rechts" if seite > 0 else "links"
        x0 = 0.5 if seite > 0 else -4
        o = m.k("schenkel_" + name, "koerper", drehpunkt=(seite * 2.2, 12, 0), drehung=(48, 0, seite * 8))
        o.w((x0, 5, -1.75), (3.5, 7, 3.5), h)
        u = m.k("schienbein_" + name, "schenkel_" + name, drehpunkt=(seite * 2.2, 5.5, 0), drehung=(-95, 0, 0))
        u.w((x0 + 0.25, -2, -1.5), (3, 8, 3), h)
        u.w((x0, -2.5, -3), (3.5, 1.5, 4.5), klaue)
    ober = m.k("oberkoerper", "koerper", drehpunkt=(0, 12, 0), drehung=(-48, 0, 0))
    ober.w((-4.5, 12, -2.5), (9, 11, 5), kombi(h, rippen(0.25, "6a3a30")))
    for i in range(4):
        ober.w((-0.75, 13 + i * 2.6, 2.3), (1.5, 1.5, 1.5 + (i % 2)), knochen("b8ae92"))  # Wirbeldornen
    kopf = m.k("kopf", "oberkoerper", drehpunkt=(0, 23, -1), drehung=(52, 0, 0))
    kopf.w((-3, 22, -5), (6, 6, 6), h, deko=gesicht([
        "......",
        "kk..kk",
        "Ee..eE",
        "......",
        ".kkkk.",
        "......",
    ], p, glow="eE"))
    for i, (x, l, r) in enumerate(((-2, 8, -10), (-0.7, 11, 4), (0.7, 10, -5), (2, 8, 11), (0, 6, 0))):
        t = m.k(f"tentakel_{i}", "kopf", drehpunkt=(x, 24, -5), drehung=(25 + i * 5, 0, r))
        t.w((x - 0.5, 24 - l, -5.6), (1, l, 1), roh)
    for seite in (1, -1):
        name = "rechts" if seite > 0 else "links"
        x0 = 4.5 if seite > 0 else -7.5
        oa = m.k("oberarm_" + name, "oberkoerper", drehpunkt=(seite * 6, 21, 0), drehung=(55, 0, seite * -12))
        oa.w((x0, 11, -1.5), (3, 10, 3), h)
        ua = m.k("unterarm_" + name, "oberarm_" + name, drehpunkt=(seite * 6, 11.5, 0), drehung=(168, 0, seite * 8))
        ua.w((x0 + 0.25, 1, -1.25), (2.5, 11, 2.5), h)
        for dz in (-1, 0, 1):
            ua.w((x0 + 0.9, -5, dz - 0.3), (0.8, 6.5, 0.8), klaue)
    return m, "Sauger", "Jäger · geduckt wie eine Gottesanbeterin, fast unsichtbar, saugt Leben aus", "1,7 Blöcke (geduckt)"


def seuchentraeger():
    """Strahlung: unförmiger Schutzanzug, riesiger Tank, Sprühlanze voller grüner Brühe. Verstrahlt die Umgebung."""
    m = Modell("seuchentraeger", 128, 128)
    anzug = kombi(stoff("c9b43a", haut_f="7f8a6a", loecher=2, blut=1, falten=4, schmutz="5a5020"),
                  lambda mm, r, f, w: mm.flecken(r, farbe("8a7a28"), skala=2, schwelle=0.62, staerke=0.6))
    gummi = einfarbig("1e1f1c", 6)
    tank = kombi(metall("4a5a3a", "6a4a1a"),
                 lambda mm, r, f, w: mm.streifen(r, farbe("d8c020"), abstand=4, senkrecht=False, staerke=0.0)
                 if f == "hinten" else None)
    gift = leuchtend("9cff4a", 30)
    p = {"v": farbe("1a2a1a"), "V": farbe("6aff6a"), "g": farbe("2a2a26"), "r": farbe("3a3a34")}
    koerper = m.k("koerper", drehpunkt=(0, 10, 0))
    for seite in (1, -1):
        b = m.k("bein_" + ("rechts" if seite > 0 else "links"), "koerper", drehpunkt=(seite * 3, 10, 0),
                drehung=(0, 0, seite * 6))
        b.w((seite * 3 - 2.5, 0, -2.5), (5, 10, 5), anzug)
        b.w((seite * 3 - 2.5, 0, -2.5), (5, 4, 5), gummi, aufblasen=0.4)
    ober = m.k("oberkoerper", "koerper", drehpunkt=(0, 10, 0), drehung=(-4, 0, 0))
    ober.w((-6, 10, -3.5), (12, 13, 7), anzug, aufblasen=0.4)
    ober.w((-6, 13, -3.5), (12, 1.5, 7), gummi, aufblasen=0.6)  # Gürtel
    t = m.k("tank", "oberkoerper", drehpunkt=(0, 17, 4))
    t.w((-4.5, 10, 4), (9, 14, 6), tank)
    t.w((-2, 12, 10.1), (4, 4, 0.6), gift)  # Leck
    t.w((-0.5, 8, 9.5), (1, 3, 1), gift)  # tropft
    kopf = m.k("kopf", "oberkoerper", drehpunkt=(0, 23, 0), drehung=(10, 0, -8))
    kopf.w((-5, 23, -5), (10, 9, 10), anzug, aufblasen=0.3, deko=gesicht([
        "..........",
        ".gggggggg.",
        ".gvVvvVvg.",
        ".gvvvvvvg.",
        ".gggggggg.",
        "...grrg...",
        "...gggg...",
    ], p, glow="V"))
    for seite in (1, -1):
        kopf.w((seite * 5.5 - 1, 24, -4), (2, 3, 2), metall("3a3d3b", "5a3a22"))  # Filterdosen
    ar = m.k("arm_rechts", "oberkoerper", drehpunkt=(7, 21, 0), drehung=(-42, 0, -10))
    ar.w((6, 11, -2), (4, 11, 4), anzug, aufblasen=0.3)
    ar.w((6, 10, -2), (4, 3, 4), gummi, aufblasen=0.4)
    lanze = m.k("lanze", "arm_rechts", drehpunkt=(8, 11, 0), drehung=(40, 0, 0))
    lanze.w((7.5, 10.5, -13), (1, 1, 13), metall("5a5e60", "4a3a2a", nieten=False))
    lanze.w((7, 10, -14.5), (2, 2, 2), gift)
    al = m.k("arm_links", "oberkoerper", drehpunkt=(-7, 21, 0), drehung=(18, 0, 14))
    al.w((-10, 11, -2), (4, 11, 4), anzug, aufblasen=0.3)
    al.w((-10, 10, -2), (4, 3, 4), gummi, aufblasen=0.4)
    schlauch = m.k("schlauch", "tank", drehpunkt=(4.5, 14, 6), drehung=(0, 40, 0))
    schlauch.w((4.5, 13.5, 5.5), (9, 1, 1), gummi)
    return m, "Seuchenträger", "Strahlung · unförmiger Schutzanzug, Sprühlanze, verstrahlt alles um sich", "2 Blöcke, breit"


def ertrunkener():
    """Wasser: aufgedunsene Wasserleiche mit riesigem Bauch, Kopf hängt zur Seite. Zieht dich unter Wasser."""
    m = Modell("ertrunkener", 128, 128)
    h = haut("7a8a8e", flecken_f="4a6a4a", adern_f="3a4a6a", blut=0, dunkel_unten=0.3)
    bauch = kombi(haut("8a9a98", flecken_f="5a7a5a", adern_f="3a4a7a", blut=0),
                  lambda mm, r, f, w: mm.flecken(r, farbe("6a7a8a"), skala=2.5, schwelle=0.6, staerke=0.7))
    algen = kombi(einfarbig("2e4a2a", 10), lambda mm, r, f, w: mm.flecken(r, farbe("1a301a"), skala=2, schwelle=0.55, staerke=0.8))
    kleid = stoff("4a5052", haut_f="7a8a8e", loecher=3, blut=0, falten=4, fransen=True, schmutz="2a3a2a")
    p = {"w": farbe("e8f0ea"), "W": farbe("b8c8c4"), "m": farbe("10181a"), "a": farbe("2e4a2a")}
    koerper = m.k("koerper", drehpunkt=(0, 11, 0))
    for seite in (1, -1):
        b = m.k("bein_" + ("rechts" if seite > 0 else "links"), "koerper", drehpunkt=(seite * 2.5, 11, 0),
                drehung=(0, 0, seite * -4))
        b.w((seite * 2.5 - 1.5, 0, -1.5), (3, 11, 3), kleid)
    ober = m.k("oberkoerper", "koerper", drehpunkt=(0, 11, 0), drehung=(8, 0, -6))
    ober.w((-4.5, 11, -2), (9, 13, 5), h)
    ober.w((-7, 8, -8), (14, 12, 10), bauch)  # aufgedunsener Bauch
    ober.w((-6, 7, -7), (12, 2, 8), bauch, aufblasen=0.2)  # hängende Haut
    ober.w((-4.5, 18, -2.3), (9, 6, 5), kleid, aufblasen=0.3)
    kopf = m.k("kopf", "oberkoerper", drehpunkt=(-1, 24, 0), drehung=(28, 0, 48))
    kopf.w((-4, 24, -4.5), (8, 7, 9), h, deko=gesicht([
        "aaaaaaaa",
        "a......a",
        ".Ww..wW.",
        ".ww..ww.",
        "........",
        "..mmmm..",
        "..mmmm..",
    ], p))
    kopf.w((-4, 30, -4.5), (8, 2, 9), algen, aufblasen=0.3)
    kiefer = m.k("kiefer", "kopf", drehpunkt=(0, 24.5, -2), drehung=(40, 0, 0))
    kiefer.w((-3, 21.5, -4.8), (6, 3, 5), h, deko=gesicht(["mmmm", "mmmm"], p, nur="oben"))
    for i, (x, y, z, l) in enumerate(((-3.5, 31, 3, 9), (-1, 31, 4, 12), (2, 31, 3, 7), (3.5, 31, 2, 10),
                                      (5, 23, 0, 8), (-5, 23, 0, 6))):
        s = m.k(f"strang_{i}", "kopf" if i < 4 else "oberkoerper", drehpunkt=(x, y, z), drehung=(12, 0, (i - 2) * 6))
        s.w((x - 0.5, y - l, z), (1, l, 1), algen)
    ar = m.k("arm_rechts", "oberkoerper", drehpunkt=(6, 22, 0), drehung=(6, 0, 18))  # hängt schlaff
    ar.w((5.5, 5, -1.5), (3, 17, 3), h)
    ar.w((5.5, 12, -1.5), (3, 4, 3), algen, aufblasen=0.15)
    al = m.k("arm_links", "oberkoerper", drehpunkt=(-6, 22, 0), drehung=(0, 0, -20))  # Stumpf
    al.w((-8.5, 17, -1.5), (3, 5, 3), h)
    al.w((-8.5, 16, -1.5), (3, 1.5, 3), fleisch("6a2a2a", "8a4a40"))
    return m, "Ertrunkener", "Wasser · aufgedunsen, Kopf hängt zur Seite; zieht dich unter Wasser", "2 Blöcke, breit"


def spaeherdrohne():
    """Fliegt: verwilderte Militärdrohne. Roter Suchscheinwerfer – erfasst er dich, kommen alle Infizierten."""
    m = Modell("spaeherdrohne", 128, 128)
    huelle = kombi(metall("4a5238", "6a4a22"),
                   lambda mm, r, f, w: mm.streifen(r, farbe("d8b820"), abstand=3, senkrecht=True, staerke=0.0)
                   if f in ("rechts", "links") else None)
    dunkel = metall("2a2c2a", "4a3a22", nieten=False)
    rotor = nebel("2a2a2a", 50, 110)
    blatt = einfarbig("1a1a1a", 4)
    rot = leuchtend("ff2a1a", 15)
    p = {"r": farbe("ff3a2a"), "R": farbe("ffc0a0"), "k": farbe("0a0a0a")}
    rumpf = m.k("rumpf", drehpunkt=(0, 8, 0))
    rumpf.w((-4, 7, -5), (8, 3, 10), huelle)
    rumpf.w((-3, 10, -4), (6, 1.5, 7), dunkel)
    rumpf.w((-1, 4, -2), (2, 3, 4), dunkel)  # Akku
    for i, winkel in enumerate((45, 135, 225, 315)):
        a = m.k(f"arm_{i}", "rumpf", drehpunkt=(0, 8.75, 0), drehung=(0, winkel, 0))
        a.w((0, 8, -0.75), (11, 1.5, 1.5), dunkel)
        r = m.k(f"rotor_{i}", f"arm_{i}", drehpunkt=(10.5, 10.5, 0))
        r.w((9.75, 9.5, -0.75), (1.5, 1.5, 1.5), dunkel)
        r.w((6, 11, -4.5), (9, 0.2, 9), rotor)
        r.w((5.5, 11.1, -0.5), (10, 0.3, 1), blatt)
        if i == 2:
            r.w((9.5, 11.2, -3), (1, 0.3, 2.5), blatt)  # abgebrochenes Blatt
        else:
            r.w((10, 11.1, -5), (1, 0.3, 10), blatt)
    kam = m.k("kamera", "rumpf", drehpunkt=(0, 7, -4), drehung=(25, 0, 0))
    kam.w((-1.5, 4, -6), (3, 3, 3), dunkel, deko=gesicht(["kkk", "kRk", "krk"], p, glow="rR"))
    kam.w((1.6, 4.5, -5.5), (1.5, 1.5, 1.2), rot)  # Suchscheinwerfer
    rumpf.w((2, 10, 3), (0.5, 7, 0.5), blatt)  # Antenne
    rumpf.w((1.6, 16.5, 2.6), (1.3, 1, 1.3), rot)
    for seite in (1, -1):
        rumpf.w((seite * 3 - 0.5, 4, -4), (1, 1, 8), dunkel)  # Kufen
        rumpf.w((seite * 3 - 0.5, 5, -2), (1, 2, 1), dunkel)
        rumpf.w((seite * 3 - 0.5, 5, 1), (1, 2, 1), dunkel)
    k = m.k("kabel", "rumpf", drehpunkt=(0, 7, 2), drehung=(15, 0, 10))
    k.w((-1, 2, 2), (0.4, 5, 0.4), einfarbig("a02020", 6))
    k.w((-0.3, 3, 2.3), (0.4, 4, 0.4), einfarbig("2040a0", 6))
    skalieren(m, 1.4)
    return m, "Späherdrohne", "Fliegt · Suchscheinwerfer; erfasst er dich, schlägt sie Alarm. Abschießen lohnt sich",\
        "1,8 Blöcke breit"


def augenfleck(m, rect, flaeche, w):
    """Großer Augenfleck auf der Flügeloberseite – zwei Flügel ergeben zusammen ein Gesicht."""
    if flaeche not in ("oben", "unten"):
        return
    zeilen = [
        "....wwwwww....",
        "..wwkkkkkkww..",
        ".wkkkrrrrkkkw.",
        "wkkrrRRRRrrkkw",
        "wkkrRRYYRRrkkw",
        "wkkrRRYYRRrkkw",
        "wkkrrRRRRrrkkw",
        ".wkkkrrrrkkkw.",
        "..wwkkkkkkww..",
        "....wwwwww....",
    ]
    pal = {"w": farbe("f0e6cc"), "k": farbe("120e0a"), "r": farbe("7a3a1a"), "R": farbe("d89a50"), "Y": farbe("fff0c0")}
    m.vorlage(rect, zeilen, pal, dx=1, dy=0)


def lichtmotte():
    """Fliegt: riesige Motte, die nachts aufs Licht zufliegt. Ihr Staub blendet. Zerbrechlich."""
    m = Modell("lichtmotte", 128, 128)
    pelz = stoff("c8bca0", loecher=0, blut=0, falten=3, fransen=True, schmutz="8a7a62")
    staub = kombi(stoff("a8987a", loecher=0, blut=0, falten=5, schmutz="6a5a48"),
                  lambda mm, r, f, w: mm.flecken(r, farbe("6a5a48"), skala=2, schwelle=0.55, staerke=0.8))
    vorne = kombi(staub, augenfleck, zerfetzt(1, 1))
    hinten = kombi(staub, lambda mm, r, f, w: mm.streifen(r, farbe("4a3a2a"), abstand=3, senkrecht=False, staerke=0.3))
    auge = leuchtend("d8ffd0", 12)
    bein = einfarbig("3a3028", 5)
    koerper = m.k("koerper", drehpunkt=(0, 12, 0), drehung=(-12, 0, 0))
    koerper.w((-2.5, 10, -3), (5, 5, 6), pelz)
    koerper.w((-3, 13, -3.5), (6, 2, 3), pelz, aufblasen=0.3)  # Pelzkragen
    hl = m.k("hinterleib", "koerper", drehpunkt=(0, 11.5, 2.5), drehung=(28, 0, 0))
    hl.w((-2, 9, 2.5), (4, 4, 9), kombi(pelz, lambda mm, r, f, w: mm.streifen(r, farbe("6a5a48"), 2, True, 0.3)))
    kopf = m.k("kopf", "koerper", drehpunkt=(0, 12, -3), drehung=(15, 0, 0))
    kopf.w((-2, 10.5, -6), (4, 4, 3), pelz)
    for seite in (1, -1):
        kopf.w((seite * 2.2 - 0.9, 11, -6.2), (1.8, 2.8, 2.2), auge)
        f = m.k("fuehler_" + ("rechts" if seite > 0 else "links"), "kopf", drehpunkt=(seite, 14, -5.5),
                drehung=(-35, 0, seite * -28))
        f.w((seite - 0.25, 14, -5.75), (0.5, 10, 0.5), bein)
        for j in range(5):
            f.w((seite - 1.25, 16 + j * 1.6, -5.65), (2.5, 0.3, 0.3), bein)
    for seite in (1, -1):
        name = "rechts" if seite > 0 else "links"
        fv = m.k("fluegel_" + name, "koerper", drehpunkt=(seite * 2.5, 14, -1), drehung=(0, seite * -8, seite * -22))
        fv.w((2.5 if seite > 0 else -18.5, 13.75, -7), (16, 0.5, 13), vorne)
        fh = m.k("hinterfluegel_" + name, "koerper", drehpunkt=(seite * 2.5, 13.5, 2), drehung=(0, seite * -30, seite * -14))
        fh.w((2.5 if seite > 0 else -13.5, 13.25, 1), (11, 0.5, 9), hinten)
    for i, z in enumerate((-2, 0, 2)):
        for seite in (1, -1):
            b = m.k(f"bein_{i}_{'r' if seite > 0 else 'l'}", "koerper", drehpunkt=(seite * 1.5, 10, z),
                    drehung=(10 * (i - 1), 0, seite * 30))
            b.w((seite * 1.5 - 0.25, 4, z - 0.25), (0.5, 6, 0.5), bein)
    return m, "Lichtmotte", "Fliegt · nachts vom Licht angelockt, ihr Staub blendet; Licht aus oder Köder-Laterne",\
        "2,3 Blöcke Spannweite"


def scheintoter():
    """Hinterhalt: liegt wie eine Leiche am Boden. Wer sich zum Plündern bückt, wird am Bein gepackt."""
    m = Modell("scheintoter", 128, 64)
    hautf = "9a9a88"
    h = haut(hautf, flecken_f="6a6070", adern_f="4a4a6a", blut=3, dunkel_unten=0.05)
    hemd = stoff("8a8070", haut_f=hautf, loecher=3, blut=6, falten=3, fransen=True)
    hose = stoff("3a3a3a", haut_f=hautf, loecher=1, blut=2, falten=4)
    roh = fleisch("6a1414", "9a3a30")
    p = {"e": farbe("d8d8c0"), "l": farbe("6a6458"), "m": farbe("1a0808"), "t": farbe("c9c1a0"), "b": farbe("5a1010")}
    # Liegt auf dem Rücken: das ganze Modell ist um 90° gekippt, Kopf nach Süden.
    koerper = m.k("koerper", drehpunkt=(0, 9.5, -6.5), drehung=(90, 0, 0))
    br = m.k("bein_rechts", "koerper", drehpunkt=(2, 12, 0), drehung=(0, 0, 8))
    br.w((0, 0, -2), (4, 12, 4), hose)
    br.w((0, 0, -2), (4, 2, 4), einfarbig("2b2622", 6), aufblasen=0.2)
    bl = m.k("bein_links", "koerper", drehpunkt=(-2, 12, 0), drehung=(0, 0, -14))
    bl.w((-4, 5, -2), (4, 7, 4), hose)  # Unterschenkel fehlt
    bl.w((-3, 3, -1), (2, 2, 2), knochen("e0d8c0"))
    bl.w((-4, 4.5, -2), (4, 1, 4), roh)
    ober = m.k("oberkoerper", "koerper", drehpunkt=(0, 12, 0))
    ober.w((-4, 12, -2), (8, 12, 4), h)
    ober.w((-4, 12, -2), (8, 12, 4), hemd, aufblasen=0.25)
    ober.w((-3, 13, -2.6), (5, 5, 1), roh)  # offener Bauch
    ober.w((-2.5, 14, -2.9), (4, 3, 0.5), knochen("d8d0b8"))
    kopf = m.k("kopf", "oberkoerper", drehpunkt=(0, 24, 0), drehung=(0, -65, 0))
    kopf.w((-4, 24, -4), (8, 8, 8), h, deko=gesicht([
        "........",
        "........",
        ".ll..ll.",
        ".le..el.",
        "b.......",
        "..mmmm..",
        "..tmtm..",
        "........",
    ], p))
    ar = m.k("arm_rechts", "oberkoerper", drehpunkt=(5, 22, 0), drehung=(0, 0, 165))  # über dem Kopf ausgestreckt
    ar.w((4, 10, -1.5), (3, 12, 3), h)
    hand = m.k("hand_rechts", "arm_rechts", drehpunkt=(5.5, 10, 0), drehung=(30, 0, 0))
    for dz in (-1, 0, 1):
        hand.w((5, 6, dz - 0.3), (0.8, 4, 0.8), h)
    al = m.k("arm_links", "oberkoerper", drehpunkt=(-5, 22, 0), drehung=(0, -30, 55))  # quer über der Brust
    al.w((-7, 10, -1.5), (3, 12, 3), hemd)
    al.w((-7, 9, -1.5), (3, 2, 3), h)
    return m, "Scheintoter", "Hinterhalt · liegt wie eine Leiche zwischen der Beute und packt dich am Bein",\
        "liegt flach, 2 Blöcke lang"


def seuchenhund():
    """Rudel: abgemagerte, infizierte Hunde. Umkreisen ihr Opfer und greifen von hinten an."""
    m = Modell("seuchenhund", 64, 64)
    fell = haut("5a4a3c", flecken_f="8a3a30", adern_f="3a2a2a", blut=1)
    roh = fleisch("6a1c1c", "9a4a3a")
    p = {"e": farbe("ffe08a"), "t": farbe("e0d8b8"), "m": farbe("1a0808"), "n": farbe("1a1414")}
    koerper = m.k("koerper", drehpunkt=(0, 10, 0))
    koerper.w((-2.5, 7, -6), (5, 5, 11), kombi(fell, rippen(0.45, "3a2e24")))
    koerper.w((-2, 7, -1), (4, 1, 4), roh, aufblasen=0.05)  # offener Bauch
    koerper.w((-1, 11.5, -5), (2, 1, 9), knochen("b0a68a"))  # Wirbel
    kopf = m.k("kopf", "koerper", drehpunkt=(0, 11, -6), drehung=(10, 0, 0))
    kopf.w((-2, 9, -10), (4, 4, 4), fell, deko=gesicht(["....", "e..e", "....", "...."], p, glow="e"))
    schnauze = m.k("schnauze", "kopf", drehpunkt=(0, 10, -10), drehung=(0, 0, 0))
    schnauze.w((-1.5, 10, -13.5), (3, 2, 3.5), fell, deko=gesicht(["nnn"], p, dy=-1))
    kiefer = m.k("kiefer", "kopf", drehpunkt=(0, 10, -10), drehung=(35, 0, 0))
    kiefer.w((-1.5, 8.6, -13.5), (3, 1.2, 3.5), roh, deko=gesicht(["ttt", "mmm"], p, nur="oben"))
    for seite in (1, -1):
        o = m.k("ohr_" + ("rechts" if seite > 0 else "links"), "kopf", drehpunkt=(seite * 1.5, 13, -8), drehung=(-20, 0, seite * 15))
        o.w((seite * 1.5 - 0.5, 13, -8.5), (1, 2, 1), fell)
    for i, (z, name) in enumerate(((-4.5, "v"), (3.5, "h"))):
        for seite in (1, -1):
            b = m.k(f"bein_{name}_{'r' if seite > 0 else 'l'}", "koerper", drehpunkt=(seite * 1.5, 7, z),
                    drehung=(12 if seite > 0 else -12, 0, 0))
            b.w((seite * 1.5 - 0.75, 0, z - 0.75), (1.5, 7, 1.5), fell)
            b.w((seite * 1.5 - 0.75, 0, z - 1), (1.5, 1, 2), knochen("3a3028"))
    schwanz = m.k("schwanz", "koerper", drehpunkt=(0, 11, 5), drehung=(-50, 0, 0))
    schwanz.w((-0.5, 10.5, 5), (1, 1, 5), fell)
    return m, "Seuchenhund", "Rudel · umkreist das Opfer, greift von hinten an", "1 Block, Rudel von 3–5"


def wuehler():
    """Lauert im Boden: gräbt sich ein und bricht direkt unter dir hervor. Blind, hört aber jeden Schritt."""
    m = Modell("wuehler", 128, 64)
    h = haut("c79a8a", flecken_f="9a6a62", adern_f="7a3a4a", blut=0, dunkel_unten=0.35)
    erde = kombi(haut("5a4636", flecken_f="3a2a20", blut=0), lambda mm, r, f, w: mm.spritzer(r, farbe("2a1e16"), 3, 2))
    p = {"t": farbe("f0e8c8"), "m": farbe("2a0808"), "r": farbe("7a2020"), "f": farbe("a87a6a")}
    koerper = m.k("koerper", drehpunkt=(0, 8, 0))
    for seite in (1, -1):
        b = m.k("bein_" + ("rechts" if seite > 0 else "links"), "koerper", drehpunkt=(seite * 3, 6, 2), drehung=(20, 0, seite * 10))
        b.w((seite * 3 - 1.5, 0, 0.5), (3, 6, 3), h)
    ober = m.k("oberkoerper", "koerper", drehpunkt=(0, 7, 2), drehung=(-50, 0, 0))
    ober.w((-4.5, 6, -3), (9, 11, 7), kombi(h, rippen(0.25, "9a6a62")))
    ober.w((-4.5, 6, -3), (9, 4, 7), erde, aufblasen=0.15)
    ober.w((-3, 13, 3.8), (6, 4, 1.5), erde)  # Buckel voller Erde
    kopf = m.k("kopf", "oberkoerper", drehpunkt=(0, 17, -1), drehung=(55, 0, 0))
    kopf.w((-3.5, 16, -6), (7, 6, 6), h, deko=gesicht([
        "f.ttt.f",
        ".tmmmt.",
        "tmmrmmt",
        "tmrrrmt",
        ".tmmmt.",
        "..ttt..",
    ], p))  # augenlos, Rundmaul mit Zahnring
    for seite in (1, -1):
        name = "rechts" if seite > 0 else "links"
        x0 = 4.5 if seite > 0 else -7.5
        a = m.k("arm_" + name, "oberkoerper", drehpunkt=(seite * 5.5, 15, 0), drehung=(55, 0, seite * 25))
        a.w((x0, 6, -1.5), (3, 10, 3), h)
        k = m.k("schaufel_" + name, "arm_" + name, drehpunkt=(seite * 6, 6, 0), drehung=(-30, 0, 0))
        k.w((x0 - 1, 4, -2.5), (5, 2, 5), h)
        for dx in (0, 1.5, 3):
            k.w((x0 - 0.75 + dx, 0, -2.5), (1, 4, 1), knochen("3a3025"))
    return m, "Wühler", "Hinterhalt · gräbt sich ein und bricht unter dir hervor; blind, hört jeden Schritt", "1,3 Blöcke"



NEU = ["gefallener_soldat", "glutleib", "seuchenhund", "wuehler", "sauger", "seuchentraeger", "ertrunkener",
       "spaeherdrohne", "lichtmotte", "scheintoter"]
ALLE.update({n: globals()[n] for n in NEU})
ANSICHTEN = {"glutleib": ((-35, 10), (90, 5), (200, 15)), "sauger": ((-35, 10), (90, 5), (200, 15)),
             "seuchentraeger": ((-35, 10), (90, 5), (200, 15)), "lichtmotte": ((-30, 35), (15, 65), (160, 25)), "spaeherdrohne": ((-30, 25), (45, 10), (210, 35)),
             "scheintoter": ((-35, 40), (60, 30), (200, 45))}


# ---------------------------------------------------------------------------
# Bosse: selten, besonders, jeder mit eigener Silhouette

def kleiner_kopf(knoten, x, y, z, s, material, offen=True):
    """Ein menschlicher Kopf, der aus einer Fleischmasse ragt."""
    p = {"e": farbe("d8d6c4"), "E": farbe("1a1410"), "m": farbe("2a0d0d"), "t": farbe("c9c1a0")}
    zeilen = ["......", ".e..e.", ".E..E.", "......", ".mmmm." if offen else "......", ".mttm." if offen else ".mmmm."]
    knoten.w((x, y, z), (s, s, s), material, deko=gesicht(zeilen, p))


def verschmolzener():
    """Boss: Dutzende Infizierte, zu einem Berg aus Körpern verschmolzen. Glühendes Herz in der Brust."""
    m = Modell("verschmolzener", 256, 256)
    h1 = haut("8e9883", flecken_f="6c5a64", adern_f="4b4766", blut=2)
    h2 = haut("a2988a", flecken_f="7d5e5a", adern_f="4f3a5a", blut=2)
    h3 = haut("7a8a7a", flecken_f="5a4a50", adern_f="3a3a5a", blut=1)
    roh = fleisch("6a1818", "9a3a30")
    masse = kombi(h1, lambda mm, r, f, w: mm.flecken(r, farbe("6a1818"), skala=2, schwelle=0.6, staerke=0.9),
                  lambda mm, r, f, w: mm.flecken(r, farbe("a2988a"), skala=3, schwelle=0.55, staerke=0.7))
    herz = leuchtend("ff3a2a", 25)
    koerper = m.k("koerper", drehpunkt=(0, 14, 0))
    for i, (x, z, rot) in enumerate(((-10, -7, (10, 0, 12)), (6, -7, (10, 0, -12)), (-10, 5, (-12, 0, 10)), (6, 5, (-12, 0, -10)))):
        b = m.k(f"bein_{i}", "koerper", drehpunkt=(x + 2.5, 14, z + 2.5), drehung=rot)
        b.w((x, 0, z), (6, 14, 6), [h1, h2, h3][i % 3])
        b.w((x + 1, 4, z - 2.5), (2, 8, 2), h2)  # Bein eines Opfers
    ober = m.k("masse", "koerper", drehpunkt=(0, 14, 0), drehung=(-8, 0, 0))
    ober.w((-14, 12, -11), (28, 26, 22), masse)
    ober.w((-10, 36, -8), (20, 12, 16), kombi(h2, rippen(0.3)))
    ober.w((-17, 18, -6), (5, 14, 12), h3)  # Beulen
    ober.w((12, 24, -5), (5, 12, 10), h2)
    ober.w((-6, 10, 10), (14, 16, 4), h1)
    ober.w((-12, 32, -10), (8, 8, 6), masse)
    ober.w((-5, 22, -12.5), (10, 10, 2), roh)  # aufgerissene Brust
    for x in (-4, -1, 2, 5):
        ober.w((x - 0.5, 21, -13.2), (1, 12, 1), knochen("d8d0b8"))  # Rippen ums Herz
    ober.w((-2.5, 24, -13), (5, 5, 2.5), herz)
    for i, (x, y, z, dreh, s, mat) in enumerate((
            (-13, 29, -14, (20, 30, 0), 8, h2), (7, 32, -14, (10, -40, 10), 8, h1), (-4, 47, -6, (-10, 10, 0), 8, h3),
            (9, 16, -14, (0, -20, -20), 7, h3), (-19, 14, -6, (0, 70, 20), 7, h1), (13, 40, 0, (0, -90, 0), 7, h2),
            (-8, 20, 11, (0, 180, 15), 7, h2), (2, 42, 8, (-30, 160, 0), 7, h1))):
        k = m.k(f"kopf_{i}", "masse", drehpunkt=(x + s / 2, y + s / 2, z + s / 2), drehung=dreh)
        kleiner_kopf(k, x, y, z, s, mat, offen=i % 2 == 0)
    for i, (x, y, z, dreh) in enumerate(((-14, 28, 0, (0, 0, 60)), (14, 26, -2, (0, 0, -70)), (-6, 44, 4, (-30, 0, 20)),
                                         (6, 46, 0, (20, 0, -30)), (0, 34, 11, (60, 0, 0)), (-12, 20, 9, (40, 0, 40)),
                                         (13, 15, 6, (30, 0, -50)), (2, 48, -6, (-40, 0, 10)))):
        a = m.k(f"aermchen_{i}", "masse", drehpunkt=(x, y, z), drehung=dreh)
        a.w((x - 1.25, y, z - 1.25), (2.5, 11, 2.5), [h1, h2, h3][i % 3])
        a.w((x - 1.5, y + 10, z - 1.5), (3, 2, 3), roh)
    for seite in (1, -1):
        name = "rechts" if seite > 0 else "links"
        x0 = 14 if seite > 0 else -22
        a = m.k("arm_" + name, "masse", drehpunkt=(seite * 16, 40, 0), drehung=(4, 0, seite * 12))
        a.w((x0, 12, -4), (8, 28, 8), kombi(h2, rippen(0.2)))
        for j in range(3):
            a.w((x0 + (8 if seite > 0 else -2.5), 16 + j * 7, -1.25 + (j - 1) * 2), (2.5, 2.5, 2.5), h1)
        f = m.k("faust_" + name, "arm_" + name, drehpunkt=(seite * 18, 12, 0))
        f.w((x0, 6, -4), (8, 6, 8), h1)
        for j, (dx, dz, dreh) in enumerate(((0, -4, (-30, 0, 10)), (5, -4, (-30, 0, -10)), (0, 2, (30, 0, 15)),
                                             (5, 2, (30, 0, -15)), (2.5, -1, (0, 0, 0)))):
            hd = m.k(f"hand_{name}_{j}", "faust_" + name, drehpunkt=(x0 + dx + 1.5, 6, dz + 1.5), drehung=dreh)
            hd.w((x0 + dx, -2, dz), (3, 8, 3), [h1, h2, h3][j % 3])
            for k in range(3):
                hd.w((x0 + dx + k * 1.0, -5, dz + 1), (0.7, 3, 0.7), [h1, h2, h3][j % 3])
    return m, "Der Verschmolzene", "Boss · Hordennacht: viele Infizierte verschmelzen zu einem Fleischberg. Herz in der Brust",\
        "4 Blöcke hoch"


def nebelmutter():
    """Boss: riesige, dürre Gestalt im dichten Nebel, ohne Gesicht. Um sie herum entstehen Nebelwesen."""
    m = Modell("nebelmutter", 256, 256)
    haut_ = haut("d8dcd6", flecken_f="b0b8b4", adern_f="8a98a8", blut=0, dunkel_unten=0.3)
    n1 = nebel("cdd8da", 110, 220)
    n2 = nebel("b8c6c9", 50, 150)
    knochen_ = knochen("c8c8bc")
    schlitz = leuchtend("d8fbff", 10)
    koerper = m.k("koerper", drehpunkt=(0, 52, 0))
    for seite in (1, -1):
        name = "rechts" if seite > 0 else "links"
        o = m.k("oberschenkel_" + name, "koerper", drehpunkt=(seite * 3, 52, 0), drehung=(-18, 0, seite * 4))
        o.w((seite * 3 - 1.5, 28, -1.5), (3, 24, 3), haut_)
        u = m.k("unterschenkel_" + name, "oberschenkel_" + name, drehpunkt=(seite * 3, 28.5, 0), drehung=(32, 0, 0))
        u.w((seite * 3 - 1.25, 0, -1.25), (2.5, 29, 2.5), haut_)
        u.w((seite * 3 - 1.5, 0, -4), (3, 1.5, 5), knochen_)
    ober = m.k("oberkoerper", "koerper", drehpunkt=(0, 52, 0), drehung=(-24, 0, 0))
    ober.w((-5, 52, -3), (10, 30, 6), kombi(haut_, rippen(0.4, "9aa4a8")))
    ober.w((-6, 54, -3.5), (12, 14, 7), n2, aufblasen=0.5)
    for i, (x, l, dreh) in enumerate(((-6, 40, (12, 0, 8)), (-2, 48, (16, 0, 2)), (2, 44, (14, 0, -3)), (6, 36, (10, 0, -9)))):
        s = m.k(f"schleier_{i}", "oberkoerper", drehpunkt=(x, 82, 3), drehung=dreh)
        s.w((x - 2, 82 - l, 3), (4, l, 0.5), n1 if i % 2 else n2)
    hals = m.k("hals", "oberkoerper", drehpunkt=(0, 82, 0), drehung=(-50, 0, 0))
    hals.w((-1.5, 82, -1.5), (3, 14, 3), kombi(haut_, rippen(0.3, "9aa4a8")))
    kopf = m.k("kopf", "hals", drehpunkt=(0, 96, 0), drehung=(-60, 0, 8))
    kopf.w((-3.5, 96, -3.5), (7, 13, 7), haut_, deko=gesicht([
        ".......",
        ".......",
        "...s...",
        "...s...",
        "...s...",
        "...s...",
        "...s...",
        "...s...",
        ".......",
    ], {"s": farbe("d8fbff")}, glow="s"))
    kopf.w((-3.5, 96, -3.5), (7, 13, 7), n2, aufblasen=0.8)
    _ = schlitz
    for seite in (1, -1):
        name = "rechts" if seite > 0 else "links"
        x0 = 5 if seite > 0 else -8
        a = m.k("arm_" + name, "oberkoerper", drehpunkt=(seite * 6.5, 80, 0), drehung=(20, 0, seite * 6))
        a.w((x0, 34, -1.5), (3, 46, 3), haut_)
        h = m.k("hand_" + name, "arm_" + name, drehpunkt=(seite * 6.5, 34, 0), drehung=(-10, 0, 0))
        for dz in (-1.5, -0.5, 0.5, 1.5):
            h.w((x0 + 1, 22, dz - 0.4), (0.8, 12, 0.8), knochen_)
    return m, "Die Nebelmutter", "Boss · dichter Nebel, nachts, verseuchte Gebiete. Gebiert Nebelwesen", "7 Blöcke hoch"


def strahlende():
    """Boss: Koloss aus der Strahlenzone. Steuerstäbe im Rücken, ein Arm endet in einem Strahlenfass."""
    m = Modell("strahlende", 256, 256)
    h = haut("7a8460", flecken_f="5a6a3a", adern_f="3a4a2a", blut=1)
    verbrannt = haut("5a5a40", flecken_f="3a3a28", blut=0)
    gruen = leuchtend("9cff4a", 30)
    hell = leuchtend("e0ffb0", 15)
    fass = kombi(metall("c8b030", "6a4a1a"),
                 lambda mm, r, f, w: mm.streifen(r, farbe("2a2a20"), abstand=4, senkrecht=False, staerke=0.0)
                 if f in ("vorne", "hinten", "rechts", "links") else None)
    stab = metall("6a6e70", "4a3a2a", nieten=False)
    p = {"g": farbe("2a2c28"), "G": farbe("9cff4a"), "r": farbe("3a3d3b"), "m": farbe("1a1a14")}
    koerper = m.k("koerper", drehpunkt=(0, 22, 0))
    for seite in (1, -1):
        b = m.k("bein_" + ("rechts" if seite > 0 else "links"), "koerper", drehpunkt=(seite * 6, 22, 0), drehung=(0, 0, seite * 8))
        b.w((seite * 6 - 4, 0, -4), (8, 22, 8), kombi(h, pusteln(1)))
    ober = m.k("oberkoerper", "koerper", drehpunkt=(0, 22, 0), drehung=(-20, 0, 0))
    ober.w((-11, 22, -7), (22, 22, 14), kombi(h, pusteln(1), rippen(0.2)))
    ober.w((-5, 28, -7.6), (10, 10, 1), gruen)  # Kern hinter den Rippen
    for x in (-4, -1.5, 1, 3.5):
        ober.w((x, 27, -8.4), (1.2, 12, 1.2), knochen("c8c0a0"))
    for i, (x, l) in enumerate(((-7, 14), (-2.5, 18), (2, 16), (6.5, 12))):
        s = m.k(f"stab_{i}", "oberkoerper", drehpunkt=(x, 40, 6), drehung=(-25 + i * 6, 0, (i - 1.5) * 10))
        s.w((x - 1.25, 38, 5), (2.5, l, 2.5), stab)
        s.w((x - 1, 38 + l, 5.25), (2, 2, 2), hell)
    kopf = m.k("kopf", "oberkoerper", drehpunkt=(0, 44, -4), drehung=(30, 0, -10))
    kopf.w((-4.5, 42, -11), (9, 8, 8), verbrannt, deko=gesicht([
        ".........",
        ".ggggggg.",
        ".gGgggGg.",
        ".ggggggg.",
        "...grg...",
        "...rrr...",
        "..mmmmm..",
    ], p, glow="G"))
    kopf.w((-1.5, 41, -12.5), (3, 3, 2), metall("3a3d3b", "5a3a22"))  # eingewachsene Gasmaske
    ar = m.k("arm_rechts", "oberkoerper", drehpunkt=(13, 42, 0), drehung=(-10, 0, -8))
    ar.w((11, 16, -4), (8, 26, 8), kombi(h, pusteln(1)))
    fs = m.k("fass", "arm_rechts", drehpunkt=(15, 16, 0))
    fs.w((9, 1, -6), (12, 16, 12), fass)
    fs.w((12, 17, -3), (6, 1, 6), gruen)
    al = m.k("arm_links", "oberkoerper", drehpunkt=(-13, 42, 0), drehung=(14, 0, 18))
    al.w((-18, 22, -3), (6, 20, 6), h)
    al.w((-18.5, 16, -3.5), (7, 7, 7), verbrannt)
    for dz in (-2.5, 0, 2.5):
        al.w((-17, 10, dz - 0.75), (1.5, 7, 1.5), knochen("3a3a2a"))
    return m, "Der Strahlende", "Boss · erscheint, wenn du schwer verstrahlt in einer Strahlenzone bist. Kern in der Brust",\
        "3,8 Blöcke hoch"


def brutkoenigin():
    """Boss: tief in den Höhlen. Riesiger, durchscheinender Eiersack voller Schwärmlinge, Spinnenbeine."""
    m = Modell("brutkoenigin", 256, 256)
    chitin = kombi(metall("3a2a26", "5a2a1a", nieten=False), lambda mm, r, f, w: mm.flecken(r, farbe("1a1210"), skala=2.0, schwelle=0.6, staerke=0.8))
    h = haut("9b827c", flecken_f="6e4b4b", adern_f="4a3040", blut=1)
    sack = kombi(haut("d8c8a0", flecken_f="b8a070", adern_f="8a4a3a", blut=0), pusteln(3, "c8ff6a"))
    p = {"r": farbe("ff3b2b"), "t": farbe("e3dcc0"), "m": farbe("1a0808")}
    koerper = m.k("koerper", drehpunkt=(0, 14, 0))
    koerper.w((-7, 10, -10), (14, 10, 14), chitin)
    s = m.k("eiersack", "koerper", drehpunkt=(0, 16, 4), drehung=(-12, 0, 0))
    s.w((-11, 6, 4), (22, 20, 30), sack)
    s.w((-8, 24, 8), (16, 5, 22), sack, aufblasen=0.2)
    s.w((-13, 9, 12), (4, 12, 16), sack)
    s.w((9, 10, 10), (4, 11, 14), sack)
    s.w((-7, 8, 33), (14, 14, 5), sack)
    ober = m.k("oberkoerper", "koerper", drehpunkt=(0, 18, -8), drehung=(-50, 0, 0))
    ober.w((-5, 18, -11), (10, 16, 7), kombi(h, rippen(0.3)))
    kopf = m.k("kopf", "oberkoerper", drehpunkt=(0, 34, -8), drehung=(40, 0, 0))
    kopf.w((-4, 33, -14), (8, 7, 8), chitin, deko=gesicht(["r.r..r.r", "........", ".r....r.", "tttttttt", "mmmmmmmm"], p, glow="r"))
    for seite in (1, -1):
        mand = m.k("mandibel_" + ("rechts" if seite > 0 else "links"), "kopf", drehpunkt=(seite * 3, 34, -13), drehung=(0, seite * 25, 0))
        mand.w((seite * 3 - 1, 33, -20), (2, 2, 7), knochen("3a2a20"))
        a = m.k("arm_" + ("rechts" if seite > 0 else "links"), "oberkoerper", drehpunkt=(seite * 6, 32, -8), drehung=(60, 0, seite * -20))
        a.w((seite * 6 - 1.25, 18, -9.25), (2.5, 14, 2.5), h)
        a.w((seite * 6 - 0.5, 10, -8.5), (1, 8, 1), knochen("2a2018"))
    for i, z in enumerate((-8, -3, 2)):
        for seite in (1, -1):
            o = m.k(f"bein_{i}_{'r' if seite > 0 else 'l'}", "koerper", drehpunkt=(seite * 7, 16, z),
                    drehung=(0, (i - 1) * 25 * seite, seite * 30))
            x0 = 7 if seite > 0 else -25
            o.w((x0, 15, z - 1.25), (18, 2.5, 2.5), chitin)
            u = m.k(f"fuss_{i}_{'r' if seite > 0 else 'l'}", f"bein_{i}_{'r' if seite > 0 else 'l'}",
                    drehpunkt=(seite * 24, 16, z), drehung=(0, 0, seite * -8))
            u.w((seite * 24 - 1, -8, z - 1), (2, 24, 2), chitin)
            u.w((seite * 24 - 0.5, -11, z - 0.5), (1, 3, 1), knochen("1a1210"))
    return m, "Die Brutkönigin", "Boss · tief in den Höhlen; bringt ständig Schwärmlinge zur Welt", "2,5 Blöcke hoch, 4 lang"


def schlaechter():
    """Boss: Blutmond in der Stadt. Fleischerhaken an der Kette, Hackbeil, Schürze voller Blut, Sackmaske."""
    m = Modell("schlaechter", 256, 128)
    h = haut("b89a88", flecken_f="8a6a5a", adern_f="6a3a4a", blut=3)
    schuerze = kombi(stoff("6a5a48", loecher=1, blut=8, falten=3, schmutz="3a2a1a"),
                     lambda mm, r, f, w: mm.flecken(r, farbe("5a0a0a"), skala=2, schwelle=0.5, staerke=0.9))
    sack = stoff("8a7a5a", loecher=0, blut=4, falten=4, schmutz="4a3a2a")
    stahl = metall("8a8e90", "6a3a1a", nieten=False)
    kette = einfarbig("4a4a48", 10)
    p = {"o": farbe("0a0808"), "r": farbe("ff4a2a"), "n": farbe("5a4a30")}
    koerper = m.k("koerper", drehpunkt=(0, 16, 0))
    for seite in (1, -1):
        b = m.k("bein_" + ("rechts" if seite > 0 else "links"), "koerper", drehpunkt=(seite * 4, 16, 0))
        b.w((seite * 4 - 3, 0, -3), (6, 16, 6), stoff("2a2622", loecher=0, blut=2, falten=3))
        b.w((seite * 4 - 3, 0, -3), (6, 4, 6), einfarbig("1a1612", 6), aufblasen=0.3)
    ober = m.k("oberkoerper", "koerper", drehpunkt=(0, 16, 0), drehung=(-10, 0, 0))
    ober.w((-9, 16, -6), (18, 20, 12), h)  # massiger Bauch
    ober.w((-8, 12, -6.6), (16, 22, 1), schuerze)
    for i, x in enumerate((-7, 0, 6)):  # Haken am Gürtel
        hk = m.k(f"guertelhaken_{i}", "oberkoerper", drehpunkt=(x, 18, -6), drehung=(10, 0, (i - 1) * 12))
        hk.w((x - 0.25, 12, -7.5), (0.5, 6, 0.5), stahl)
        hk.w((x - 1, 9, -8), (2, 3, 2), fleisch("7a1c1c", "a8473a"))
    kopf = m.k("kopf", "oberkoerper", drehpunkt=(0, 36, -2), drehung=(14, 0, -6))
    kopf.w((-4.5, 36, -6), (9, 9, 9), sack, deko=gesicht([
        ".........",
        "..oo.oo..",
        "..or.ro..",
        "..oo.oo..",
        ".........",
        "nnnnnnnnn",
        ".........",
    ], p, glow="r"))
    kopf.w((-5, 39, -6.5), (10, 1, 10), einfarbig("3a2a1a", 5), aufblasen=0.2)  # zugeschnürt
    ar = m.k("arm_rechts", "oberkoerper", drehpunkt=(10, 33, 0), drehung=(-20, 0, -10))
    ar.w((9, 16, -3), (6, 17, 6), h)
    beil = m.k("hackbeil", "arm_rechts", drehpunkt=(12, 16, 0), drehung=(-60, 0, 0))
    beil.w((11.5, 14, -14), (1.5, 2, 12), einfarbig("3a2a1a", 6))
    beil.w((11.75, 9, -22), (1, 10, 9), stahl)
    al = m.k("arm_links", "oberkoerper", drehpunkt=(-10, 33, 0), drehung=(10, 0, 14))
    al.w((-15, 16, -3), (6, 17, 6), h)
    kt = m.k("kette", "arm_links", drehpunkt=(-12, 16, 0), drehung=(-35, 0, 15))
    for j in range(6):
        kt.w((-13, 14 - j * 3.2, -1 + (j % 2) * 0.5), (2, 3, 2), metall("6a6a68", "5a3a22", nieten=False))
    kt.w((-13.5, -6, -2), (1, 4, 1), stahl)
    kt.w((-13.5, -7, -4), (1, 1, 3), stahl)
    kt.w((-13.5, -6, -5), (1, 3, 1), stahl)  # Haken
    return m, "Der Schlächter", "Boss · nur bei Blutmond in Städten; zieht dich mit dem Haken heran", "2,8 Blöcke"


def kommandant():
    """Boss (Mensch): Anführer der Wacht im Exo-Panzer, Raketenwerfer auf der Schulter. Kommt nur zu Todfeinden."""
    m = Modell("kommandant", 256, 128)
    platte = metall("4a5238", "5a4a2a")
    dunkel = metall("2a2c2a", "4a3a22", nieten=False)
    kolben = metall("8a8e90", "5a4a3a", nieten=False)
    umhang = stoff("5a1a1a", loecher=2, blut=0, falten=4, fransen=True, schmutz="2a0a0a")
    visier = leuchtend("ff8a2a", 15)
    koerper = m.k("koerper", drehpunkt=(0, 18, 0))
    for seite in (1, -1):
        name = "rechts" if seite > 0 else "links"
        x0 = seite * 4 - 3
        b = m.k("bein_" + name, "koerper", drehpunkt=(seite * 4, 18, 0), drehung=(seite * 4, 0, seite * 3))
        b.w((x0, 0, -3), (6, 18, 6), dunkel)
        b.w((x0, 8, -3.5), (6, 7, 2), platte, aufblasen=0.3)
        b.w((x0 - 0.5, 0, -4.5), (7, 3, 8), platte)
        b.w((x0 + (6 if seite > 0 else -1), 3, -0.5), (1, 12, 1), kolben)  # Hydraulik
    ober = m.k("oberkoerper", "koerper", drehpunkt=(0, 18, 0))
    ober.w((-7, 18, -4), (14, 16, 8), platte)
    ober.w((-6, 22, -4.8), (12, 10, 1), dunkel)
    ober.w((-5, 20, 4), (10, 12, 5), dunkel)  # Energieeinheit
    ober.w((-4, 32, 6), (2, 4, 2), kolben)
    ober.w((2, 32, 6), (2, 4, 2), kolben)  # Auspuffe
    u = m.k("umhang", "oberkoerper", drehpunkt=(0, 33, 4), drehung=(10, 0, 0))
    u.w((-7, 8, 9), (14, 25, 0.5), umhang)
    kopf = m.k("kopf", "oberkoerper", drehpunkt=(0, 34, 0))
    kopf.w((-4.5, 34, -5), (9, 9, 9), platte, deko=gesicht([
        ".........", ".........", ".........", "vvvvvvvvv", ".........", ".........", "........."],
        {"v": farbe("ff8a2a")}, glow="v"))
    kopf.w((-4.5, 37, -5.6), (9, 1, 1), visier)
    for seite in (1, -1):
        name = "rechts" if seite > 0 else "links"
        x0 = 7 if seite > 0 else -13
        s = m.k("schulter_" + name, "oberkoerper", drehpunkt=(seite * 9, 33, 0))
        s.w((x0 - 0.5, 30, -4.5), (7, 5, 9), platte, aufblasen=0.3)
        a = m.k("arm_" + name, "oberkoerper", drehpunkt=(seite * 10, 31, 0), drehung=(-14 if seite > 0 else 6, 0, seite * 6))
        a.w((x0, 14, -3), (6, 17, 6), dunkel)
        a.w((x0 - 0.5, 14, -3.5), (7, 8, 7), platte, aufblasen=0.2)
    rw = m.k("raketenwerfer", "schulter_rechts", drehpunkt=(10, 36, 0), drehung=(-8, 0, 0))
    rw.w((7, 35, -6), (7, 6, 12), dunkel)
    for dx in (0, 3):
        for dy in (0, 3):
            rw.w((7.75 + dx, 35.75 + dy, -6.6), (2, 2, 1), visier)
    gw = m.k("gewehr", "arm_rechts", drehpunkt=(10, 14, 0), drehung=(-76, 0, 0))
    gw.w((9, 12, -16), (2, 3, 18), dunkel)
    gw.w((9, 9, -6), (2, 4, 2), dunkel)
    return m, "Der Kommandant", "Boss · Anführer der Wacht im Exo-Panzer. Kommt nur, wenn ihr Todfeinde seid", "2,7 Blöcke"


def hirte():
    """Boss: hagere Gestalt mit Hut und Glockenstab. Führt eine ganze Herde Infizierter."""
    m = Modell("hirte", 256, 128)
    mantel = stoff("3a3428", loecher=2, blut=1, falten=4, fransen=True, schmutz="1e1a14")
    mantel_unten = kombi(stoff("3a3428", loecher=3, blut=1, falten=4, fransen=True, schmutz="1e1a14"), zerfetzt(3, 2))
    binden = stoff("b8ac8a", loecher=0, blut=2, falten=5, schmutz="6a5a3a")
    haut_ = haut("9a948a", flecken_f="6a6460", blut=0)
    holz = einfarbig("4a3a26", 8)
    messing = metall("b08a3a", "6a4a1a", nieten=False)
    p = {"a": farbe("ffe08a"), "s": farbe("2a2420"), "b": farbe("8a7a5a")}
    koerper = m.k("koerper", drehpunkt=(0, 22, 0))
    for seite in (1, -1):
        b = m.k("bein_" + ("rechts" if seite > 0 else "links"), "koerper", drehpunkt=(seite * 1.75, 22, 0), drehung=(seite * -5, 0, 0))
        b.w((seite * 1.75 - 1.25, 0, -1.25), (2.5, 22, 2.5), einfarbig("2a2620", 6))
    ober = m.k("oberkoerper", "koerper", drehpunkt=(0, 22, 0), drehung=(-22, 0, 0))
    ober.w((-4, 22, -2.5), (8, 18, 5), mantel)
    ober.w((-5, 6, -3), (10, 18, 6), mantel_unten)  # langer Mantel
    for i, (x, y) in enumerate(((-4.5, 30), (4, 26), (-3, 20))):
        g = m.k(f"gloeckchen_{i}", "oberkoerper", drehpunkt=(x, y, -2.5))
        g.w((x - 0.75, y - 2, -3.8), (1.5, 2, 1.5), messing)
    kopf = m.k("kopf", "oberkoerper", drehpunkt=(0, 40, 0), drehung=(30, 0, 12))
    kopf.w((-3, 40, -3.5), (6, 8, 7), binden, deko=gesicht([
        "......", "......", "s...a.", "......", "bbbbbb", "......", "......", "......"], p, glow="a"))
    kopf.w((-7, 47, -7.5), (14, 1, 14), stoff("2a2420", loecher=1, blut=0, falten=3, fransen=True))  # Hutkrempe
    kopf.w((-3.5, 48, -4), (7, 5, 7), stoff("2a2420", loecher=0, blut=0, falten=3))
    ar = m.k("arm_rechts", "oberkoerper", drehpunkt=(4.5, 38, 0), drehung=(-40, 0, -6))
    ar.w((4, 22, -1), (2, 16, 2), mantel)
    ar.w((4, 20, -1), (2, 2, 2), haut_)
    stab = m.k("stab", "arm_rechts", drehpunkt=(5, 21, 0), drehung=(48, 0, 0))
    stab.w((4.5, -14, -0.5), (1, 50, 1), holz)
    stab.w((4.5, 36, -0.5), (1, 1, 6), holz)  # Krümme
    stab.w((4.5, 31, 4.5), (1, 6, 1), holz)
    glocke = m.k("glocke", "stab", drehpunkt=(5, 31, 5))
    glocke.w((3.5, 27, 3.5), (3, 4, 3), messing)
    glocke.w((4.5, 26, 4.5), (1, 1, 1), einfarbig("3a2a1a", 4))
    al = m.k("arm_links", "oberkoerper", drehpunkt=(-4.5, 38, 0), drehung=(6, 0, 10))
    al.w((-6, 18, -1), (2, 20, 2), mantel)
    for dz in (-0.75, 0, 0.75):
        al.w((-5.5, 13, dz - 0.25), (0.6, 5, 0.6), haut_)
    return m, "Der Hirte", "Boss · führt eine Herde Infizierter übers Land; fällt er, zerstreut sie sich", "3,3 Blöcke"


BOSSE = ["verschmolzener", "nebelmutter", "strahlende", "brutkoenigin", "schlaechter", "kommandant", "hirte"]
ALLE.update({n: globals()[n] for n in BOSSE})
ANSICHTEN.update({"brutkoenigin": ((-40, 20), (90, 8), (200, 25))})

def schlund():
    """Boss: riesiger Wurm unter Wüsten und Ödland. Lärm lockt ihn an, er bricht mit kreisrundem Maul aus dem Boden."""
    m = Modell("schlund", 256, 256)
    panzer = kombi(metall("6a5440", "4a3020", nieten=False),
                   lambda mm, r, f, w: mm.flecken(r, farbe("3a2a1a"), skala=2.0, schwelle=0.58, staerke=0.8))
    haut_ = haut("b08a78", flecken_f="8a5a4a", adern_f="6a2a3a", blut=1)
    rachen = fleisch("5a0a0a", "9a2a20")
    zahn = knochen("e8e0c8")
    erde = kombi(einfarbig("5a4636", 12), lambda mm, r, f, w: mm.spritzer(r, farbe("3a2a1e"), 4, 2))
    basis = m.k("basis", drehpunkt=(0, 0, 0))
    basis.w((-16, 0, -16), (32, 3, 32), erde)  # aufgewühlter Boden
    for i, (x, z) in enumerate(((-14, -6), (10, -12), (8, 10), (-10, 12))):
        basis.w((x, 1, z), (6, 4, 5), erde)  # Erdbrocken
    eltern = "basis"
    y = 2
    breite = 18
    for i, dreh in enumerate(((0, 0, 0), (-7, 0, 3), (-8, 0, -4), (-6, 0, 5), (-4, 0, -3), (5, 0, -4), (8, 0, 3),
                              (10, 0, 2))):
        seg = m.k(f"segment_{i}", eltern, drehpunkt=(0, y, 0), drehung=dreh)
        b = breite - i * 1.0 + (1.5 if i % 2 else 0)
        seg.w((-b / 2, y, -b / 2), (b, 8, b), haut_)
        seg.w((-b / 2 - 0.5, y + 5, -b / 2 - 0.5), (b + 1, 3, b + 1), panzer, aufblasen=0.4)  # Panzerring
        for j, (sx, sz) in enumerate(((0, 1), (1, 0), (0, -1), (-1, 0))):
            if (i + j) % 2 == 0:
                seg.w((sx * b / 2 - 1 + sx, y + 7, sz * b / 2 - 1 + sz), (2, 5, 2), zahn)  # Dornen
        eltern = f"segment_{i}"
        y += 8
    kopf = m.k("kopf", eltern, drehpunkt=(0, y, 0), drehung=(-35, 0, 0))
    kopf.w((-9, y, -9), (18, 6, 18), panzer)
    kopf.w((-7, y + 5.5, -7), (14, 1, 14), rachen)  # Schlund
    for k in range(12):  # Zahnkranz
        w = k * math.pi / 6
        zx = math.cos(w) * 7.5
        zz = math.sin(w) * 7.5
        kopf.w((zx - 0.75, y + 6, zz - 0.75), (1.5, 4, 1.5), zahn)
    for k in range(6):  # innerer Kranz
        w = k * math.pi / 3 + 0.5
        kopf.w((math.cos(w) * 4 - 0.5, y + 5, math.sin(w) * 4 - 0.5), (1, 3, 1), zahn)
    for k, (dx, dz, dreh) in enumerate(((-3, -2, (20, 0, 25)), (3, 1, (-15, 0, -30)), (0, 3, (-30, 0, 5)))):
        t = m.k(f"zunge_{k}", "kopf", drehpunkt=(dx, y + 5, dz), drehung=dreh)
        t.w((dx - 1, y + 5, dz - 1), (2, 14, 2), rachen)
    return m, "Der Schlund", "Boss · Wüste und Ödland; Lärm lockt ihn an, er bricht aus dem Boden", "5 Blöcke hoch"


def pilzvater():
    """Boss: von Pilzen überwucherter Riese in Sümpfen und dunklen Wäldern. Bei Regen stößt er Sporenwolken aus."""
    m = Modell("pilzvater", 256, 256)
    h = haut("7a7064", flecken_f="5a4a3a", adern_f="3a3a2a", blut=0)
    myzel = kombi(einfarbig("d8d0c0", 10), lambda mm, r, f, w: mm.adern(r, farbe("a8a090"), anzahl=3, laenge=6))
    hut = kombi(haut("8a9aa8", flecken_f="5a6a7a", blut=0, dunkel_unten=0.0), pusteln(2, "8affe0"))
    lamellen = einfarbig("c8bca8", 10)
    rinde = kombi(einfarbig("4a3a2a", 10), lambda mm, r, f, w: mm.streifen(r, farbe("2a2018"), 2, True, 0.3))
    p = {"g": farbe("8affe0"), "k": farbe("0a0a08"), "z": farbe("c8c0a0")}
    koerper = m.k("koerper", drehpunkt=(0, 18, 0))
    for seite in (1, -1):
        b = m.k("bein_" + ("rechts" if seite > 0 else "links"), "koerper", drehpunkt=(seite * 5, 18, 0), drehung=(0, 0, seite * 6))
        b.w((seite * 5 - 3.5, 2, -3.5), (7, 16, 7), rinde)
        for k, dreh in enumerate(((0, 0, seite * 50), (40, 0, seite * 20), (-40, 0, seite * 20))):
            w = m.k(f"wurzel_{'r' if seite > 0 else 'l'}_{k}", "bein_" + ("rechts" if seite > 0 else "links"),
                    drehpunkt=(seite * 5, 3, 0), drehung=dreh)
            w.w((seite * 5 - 1, -5, -1), (2, 8, 2), rinde)
    ober = m.k("oberkoerper", "koerper", drehpunkt=(0, 18, 0), drehung=(-30, 0, 8))
    ober.w((-9, 18, -6), (18, 20, 12), kombi(h, rippen(0.2)))
    ober.w((-9, 18, -6), (18, 10, 12), myzel, aufblasen=0.3)
    for i, (x, y, z, groesse, dreh) in enumerate(((-7, 36, 0, 14, (0, 0, 15)), (6, 34, 2, 10, (10, 0, -20)),
                                                  (0, 30, 6, 12, (35, 0, 0)), (-10, 26, 3, 7, (0, 0, 40)))):
        pz = m.k(f"pilz_{i}", "oberkoerper", drehpunkt=(x, y, z), drehung=dreh)
        pz.w((x - 1.5, y, z - 1.5), (3, groesse * 0.5, 3), myzel)  # Stiel
        pz.w((x - groesse / 2, y + groesse * 0.5, z - groesse / 2), (groesse, 3, groesse), hut)
        pz.w((x - groesse / 2 + 1, y + groesse * 0.5 - 0.5, z - groesse / 2 + 1), (groesse - 2, 0.5, groesse - 2), lamellen)
    kopf = m.k("kopf", "oberkoerper", drehpunkt=(0, 38, -3), drehung=(40, 0, -15))
    kopf.w((-4, 37, -9), (8, 8, 8), knochen("c8c0a8"), deko=gesicht([
        "........", "........", ".kk..kk.", ".kg..gk.", "...kk...", "........", ".zzzzzz.", "........"], p, glow="g"))
    kopf.w((-5, 43, -10), (10, 3, 10), hut)
    for seite in (1, -1):
        name = "rechts" if seite > 0 else "links"
        x0 = 9 if seite > 0 else -15
        a = m.k("arm_" + name, "oberkoerper", drehpunkt=(seite * 11, 36, 0), drehung=(34, 0, seite * -6))
        a.w((x0, 14, -3), (6, 22, 6), kombi(h, ))
        a.w((x0, 20, -3), (6, 8, 6), myzel, aufblasen=0.2)
        if seite > 0:
            kl = m.k("schwammkeule", "arm_rechts", drehpunkt=(12, 14, 0))
            kl.w((x0 - 3, 4, -7), (12, 10, 14), hut)  # Arm wird zum Baumpilz
            kl.w((x0 - 2, 3.5, -6), (10, 0.5, 12), lamellen)
        else:
            for dz in (-2, 0, 2):
                a.w((x0 + 2, 6, dz - 0.5), (1, 8, 1), myzel)  # Myzelfäden statt Finger
    return m, "Der Pilzvater", "Boss · Sümpfe und dunkle Wälder bei Regen; Sporenwolken, Wurzeln greifen nach dir",\
        "3,5 Blöcke"


def erfrorene():
    """Boss: im Schneesturm erstarrter Riese. Eisdornen im Rücken, ein Arm ist eine Eisklinge, sein Atem friert."""
    m = Modell("erfrorene", 256, 256)
    h = haut("a8bccb", flecken_f="7a90a8", adern_f="4a5a8a", blut=0, dunkel_unten=0.2)
    eis = nebel("c8ecff", 170, 235)
    eis_dunkel = nebel("8ac0e0", 190, 250)
    plane = stoff("6a7a80", loecher=3, blut=0, falten=4, fransen=True, schmutz="4a5a60")
    reif = kombi(plane, lambda mm, r, f, w: mm.flecken(r, farbe("d8ecf8"), skala=3, schwelle=0.62, staerke=0.5))
    p = {"b": farbe("aef0ff"), "k": farbe("1a2a3a"), "w": farbe("e8f8ff")}
    koerper = m.k("koerper", drehpunkt=(0, 24, 0))
    for seite in (1, -1):
        b = m.k("bein_" + ("rechts" if seite > 0 else "links"), "koerper", drehpunkt=(seite * 3.5, 24, 0), drehung=(seite * -6, 0, seite * 4))
        b.w((seite * 3.5 - 2.5, 0, -2.5), (5, 24, 5), h)
        b.w((seite * 3.5 - 3, 0, -3), (6, 7, 6), eis)  # in Eis gefroren
    ober = m.k("oberkoerper", "koerper", drehpunkt=(0, 24, 0), drehung=(-12, 0, 0))
    ober.w((-7, 24, -4), (14, 22, 8), kombi(h, rippen(0.25, "7a90a8")))
    ober.w((-8, 32, -4.5), (16, 15, 9), reif, aufblasen=0.4)  # gefrorene Plane um die Schultern
    for i, (x, y, z, l, dreh) in enumerate(((-6, 40, 4, 16, (-35, 0, 25)), (-2, 44, 4, 22, (-30, 0, 8)),
                                             (3, 43, 4, 19, (-32, 0, -10)), (7, 38, 4, 13, (-40, 0, -28)),
                                             (0, 34, 4, 11, (-55, 0, 0)), (-9, 46, 0, 10, (0, 0, 50)),
                                             (9, 46, 0, 12, (0, 0, -55)))):
        d = m.k(f"dorn_{i}", "oberkoerper", drehpunkt=(x, y, z), drehung=dreh)
        d.w((x - 1.5, y, z - 1.5), (3, l, 3), eis_dunkel if i % 2 else eis)
        d.w((x - 0.75, y + l, z - 0.75), (1.5, 4, 1.5), eis)
    kopf = m.k("kopf", "oberkoerper", drehpunkt=(0, 46, -1), drehung=(16, 0, 6))
    kopf.w((-4, 46, -5), (8, 9, 8), h, deko=gesicht([
        "........", "........", ".bb..bb.", ".bw..wb.", "........", ".kkkkkk.", ".kwkwkk.", "........", "........"],
        p, glow="bw"))
    kopf.w((-4.5, 51, -5.5), (9, 4, 9), eis, aufblasen=0.2)  # Eiskappe
    for k in range(4):
        kopf.w((-3 + k * 2, 44, -5.6), (0.8, 2.5 + (k % 2), 0.8), eis)  # Eiszapfen am Kinn
    ar = m.k("arm_rechts", "oberkoerper", drehpunkt=(8.5, 44, 0), drehung=(-14, 0, -12))
    ar.w((7, 24, -2.5), (5, 20, 5), h)
    klinge = m.k("eisklinge", "arm_rechts", drehpunkt=(9.5, 24, 0), drehung=(-8, 0, 0))
    klinge.w((8.5, 2, -3), (2, 22, 6), eis_dunkel)
    klinge.w((9, -4, -2), (1, 6, 4), eis)
    al = m.k("arm_links", "oberkoerper", drehpunkt=(-8.5, 44, 0), drehung=(18, 0, 16))
    al.w((-12, 24, -2.5), (5, 20, 5), h)
    al.w((-12.5, 30, -3), (6, 8, 6), eis)
    for dz in (-1.5, 0, 1.5):
        al.w((-11, 17, dz - 0.5), (1, 7, 1), h)
    return m, "Der Erfrorene", "Boss · Schneegebiete im Schneesturm; Eisklinge, sein Atem lässt dich erstarren",\
        "3,8 Blöcke"


BOSSE += ["schlund", "pilzvater", "erfrorene"]
ALLE.update({n: globals()[n] for n in ("schlund", "pilzvater", "erfrorene")})


def wache():
    """Wache einer Zuflucht: zusammengewürfelte Ausrüstung, gelbe Armbinde, Kochtopf-Helm."""
    p = {"h": farbe("3a2a1e"), "e": farbe("1a1410"), "w": farbe("e8e0d0"), "b": farbe("6a4a30"), "m": farbe("5a2a20"),
         "s": farbe("4a5a6a"), "S": farbe("3a4a5a")}
    jacke = stoff("4a5a6a", loecher=1, blut=0, falten=3, schmutz="2a3a4a")
    gesicht_ = ["hhhhhhhh", "h......h", ".we..ew.", "...bb...", "........", ".bmmmmb.", "SsSsSsSs", "sSsSsSsS"]

    def extras(m, k):
        k["kopf"].w((-4.5, 30, -4.5), (9, 3, 9), metall("6a6e70", "5a3a22", nieten=False))  # Topfhelm
        k["ober"].w((-4, 14, -2), (8, 8, 4), stoff("3a3a2a", loecher=0, blut=0, falten=2), aufblasen=0.5)  # Weste
        k["ober"].w((-3.5, 13, 2.4), (7, 8, 2.5), stoff("4a4630", loecher=0, blut=0, falten=3))
        binde = m.k("armbinde", "arm_rechts", drehpunkt=(5, 20, 0))
        binde.w((4, 18, -1.5), (3, 2, 3), einfarbig("d8c020", 8), aufblasen=0.15)
        gewehr = m.k("gewehr", "arm_rechts", drehpunkt=(5, 13, 0), drehung=(-80, 0, 0))
        gewehr.w((4.5, 12.5, -10), (1, 2, 12), einfarbig("2a2018", 6))
        gewehr.w((4.5, 10.5, -4), (1, 2, 1.5), einfarbig("2a2b28", 6))
    m = _mensch("wache", "c4a084", gesicht_, p, jacke, stoff("3a3a32", loecher=0, blut=0, falten=4),
                einfarbig("1c1a17", 6), extras, arm_pose=((-40, -10, 0), (-50, 30, 0)))
    return m, "Wache der Zuflucht", "Freundlich · bewacht Mauer und Tor, schießt auf alles Infizierte", "1,9 Blöcke"


def bewohner():
    """Bewohner einer Zuflucht: Arbeitskleidung, Schürze, Kopftuch."""
    p = {"h": farbe("6a4a2a"), "e": farbe("1a1410"), "w": farbe("e8e0d0"), "b": farbe("8a5a40"), "m": farbe("7a3a30"),
         "t": farbe("8a3a3a")}
    hemd = stoff("8a7a5a", loecher=1, blut=0, falten=3, schmutz="5a4a3a")
    gesicht_ = ["tttttttt", "tt....tt", ".we..ew.", "...bb...", "........", "..mmmm..", "........", "........"]

    def extras(m, k):
        k["kopf"].w((-4, 29, -4), (8, 3, 8), stoff("8a3a3a", loecher=0, blut=0, falten=3), aufblasen=0.4)  # Kopftuch
        k["ober"].w((-3.5, 6, -2.4), (7, 13, 1), stoff("6a6a5a", loecher=0, blut=0, falten=2))  # Schürze
    m = _mensch("bewohner", "c8a888", gesicht_, p, hemd, stoff("4a4a3a", loecher=1, blut=0, falten=4),
                einfarbig("3a2a1a", 6), extras, arm_pose=((-12, 0, 3), (-6, 0, -3)))
    return m, "Bewohner", "Freundlich · arbeitet in der Zuflucht, kennt Gerüchte", "1,9 Blöcke"


def auftraggeber():
    """Verwalter einer Zuflucht: alter Mantel, Brille, Klemmbrett und Funkgerät."""
    p = {"h": farbe("b8b0a8"), "e": farbe("1a1410"), "w": farbe("e8e0d0"), "g": farbe("2a2a2a"), "b": farbe("8a7a6a"),
         "m": farbe("5a3a30")}
    mantel = stoff("3a3a4a", loecher=0, blut=0, falten=3, schmutz="2a2a3a")
    gesicht_ = ["hhhhhhhh", "h......h", "gwegwegg", "...bb...", ".bbbbbb.", "bbmmmmbb", ".bbbbbb.", "........"]

    def extras(m, k):
        k["ober"].w((-4.5, 3, -2.5), (9, 10, 5), stoff("3a3a4a", loecher=0, blut=0, falten=3, fransen=True))  # langer Mantel
        brett = m.k("klemmbrett", "arm_links", drehpunkt=(-5.5, 13, 0), drehung=(-60, 0, 0))
        brett.w((-7, 12, -5), (4, 0.5, 5), einfarbig("8a6a4a", 6))
        brett.w((-6.5, 12.5, -4.5), (3, 0.3, 4), einfarbig("e0d8c0", 4))
        k["ober"].w((2, 20, -2.6), (2, 3, 1), metall("2a2a2a", "4a3a2a", nieten=False))  # Funkgerät
        k["ober"].w((3.2, 23, -2.4), (0.4, 3, 0.4), einfarbig("1a1a1a", 4))
    m = _mensch("auftraggeber", "c4a890", gesicht_, p, mantel, stoff("2a2a2a", loecher=0, blut=0, falten=4),
                einfarbig("2a2018", 6), extras, arm_pose=((-6, 0, 3), (-55, 20, 0)))
    return m, "Auftraggeber", "Freundlich · vergibt Aufträge gegen Militärpatronen", "1,9 Blöcke"


ALLE.update({"wache": wache, "bewohner": bewohner, "auftraggeber": auftraggeber})

def bauen(name):
    modell, titel, unter, groesse = ALLE[name]()
    modell.uv_packen()
    maler = Maler(modell, saat=sum(map(ord, name)))
    maler.alles_malen()
    for ordner in ("geo/entity", "textures/entity"):
        os.makedirs(os.path.join(ASSETS, ordner), exist_ok=True)
    modell.speichern(os.path.join(ASSETS, "geo", "entity", f"{name}.geo.json"))
    maler.speichern(os.path.join(ASSETS, "textures", "entity", f"{name}.png"),
                    os.path.join(ASSETS, "textures", "entity", f"{name}_glowmask.png"))
    os.makedirs(VORSCHAU, exist_ok=True)
    extra = {"ansichten": ANSICHTEN[name]} if name in ANSICHTEN else {}
    b = blatt(modell, maler.bild, maler.glow, titel, unter, groesse_bloecke=groesse, **extra)
    pfad = os.path.join(VORSCHAU, f"{name}.png")
    b.save(pfad)
    return pfad



def begleiter():
    """Begleiter: angeheuerter Überlebender. Großer Rucksack, Schutzbrille auf der Stirn, Gewehr über dem Rücken."""
    p = {"h": farbe("4a3020"), "e": farbe("1a1410"), "w": farbe("e8e0d0"), "b": farbe("7a5a40"),
         "g": farbe("3a4a2a"), "G": farbe("4a5a36")}
    jacke = kombi(stoff("5a5a3a", loecher=1, blut=0, falten=3, schmutz="3a3a24"),
                  lambda mm, r, f, w: mm.flecken(r, farbe("6a4a2a"), dichte=0.2, skala=2, schwelle=0.75, staerke=0.8))
    gesicht_ = ["hhhhhhhh", "hhhhhhhh", ".we..ew.", "...bb...", "gGgGgGgG", "GgGgGgGg", "gGgGgGgG", "........"]

    def extras(m, k):
        k["kopf"].w((-4, 31, -4), (8, 1.5, 8), einfarbig("3a2618", 8), aufblasen=0.2)  # Haare
        k["kopf"].w((-4, 26, 2.6), (8, 5, 1.6), einfarbig("3a2618", 8), aufblasen=0.2)
        k["kopf"].w((-4.4, 27, -2), (0.8, 4, 4.6), einfarbig("3a2618", 8))
        k["kopf"].w((3.6, 27, -2), (0.8, 4, 4.6), einfarbig("3a2618", 8))
        k["kopf"].w((-4.3, 30.3, -4.4), (8.6, 1.2, 8.8), einfarbig("2a2a2a", 4))  # Brillenband
        k["kopf"].w((-3.2, 30, -4.9), (2.6, 1.8, 1), einfarbig("6aa8b8", 12))  # Gläser auf der Stirn
        k["kopf"].w((0.6, 30, -4.9), (2.6, 1.8, 1), einfarbig("6aa8b8", 12))
        ruck = m.k("rucksack", "oberkoerper", drehpunkt=(0, 18, 2))
        ruck.w((-4, 11, 2), (8, 12, 5), stoff("7a6a48", loecher=0, blut=0, falten=3, schmutz="4a3e2a"))
        ruck.w((-3.5, 23, 2.5), (7, 3, 4), einfarbig("6a5a3a", 6))  # Schlafsack
        ruck.w((-5, 13, 3), (1, 6, 3), stoff("3a3a24", loecher=0, blut=0, falten=2))  # Seitentaschen
        ruck.w((4, 13, 3), (1, 6, 3), stoff("3a3a24", loecher=0, blut=0, falten=2))
        gewehr = m.k("gewehr_ruecken", "rucksack", drehpunkt=(0, 18, 7), drehung=(0, 0, 40))
        gewehr.w((-0.5, 8, 7), (1, 18, 1.5), einfarbig("2a2018", 6))
        gewehr.w((-0.6, 20, 6.8), (1.2, 4, 2), einfarbig("2a2b28", 6))
        beine = {kn.name: kn for kn in m.knochen}
        for n, x in (("bein_rechts", 0.5), ("bein_links", -3.5)):
            beine[n].w((x - 0.2, 4, -2.4), (3.4, 3, 1), einfarbig("2a2a2a", 6))  # Knieschoner
    m = _mensch("begleiter", "c09878", gesicht_, p, jacke, stoff("3a3a30", loecher=1, blut=0, falten=4),
                einfarbig("2a2018", 6), extras, arm_pose=((-14, 0, 4), (-10, 0, -4)))
    return m, "Begleiter", "Freundlich · angeheuert, folgt dir und kämpft an deiner Seite", "1,9 Blöcke"


ALLE.update({"begleiter": begleiter})


# ---------------------------------------------------------------------------
# Gegenstände der Zone

def abwurfkiste():
    """Versorgungskiste am Fallschirm (Luftabwurf)."""
    m = Modell("abwurfkiste", 256, 256)
    holz = kombi(stoff("6a5a3a", loecher=0, blut=0, falten=7),
                 lambda mm, r, f, w: mm.streifen(r, farbe("4a3e28"), abstand=4, senkrecht=False, staerke=0.5))
    band = metall("3a3e3a", "5a3a1a")
    schirm = kombi(stoff("6a7048", loecher=0, blut=0, falten=6),
                   lambda mm, r, f, w: mm.streifen(r, farbe("c86a2a"), abstand=10, senkrecht=True, staerke=0.0)
                   if f in ("oben", "unten") else None)
    seil = einfarbig("c9c0a0", 6)
    rot = leuchtend("ff4a2a", 15)
    kiste = m.k("kiste", drehpunkt=(0, 12, 0))
    kiste.w((-6, 0, -6), (12, 12, 12), holz)
    for y in (2, 9):
        kiste.w((-6.2, y, -6.2), (12.4, 1.2, 12.4), band)
    kiste.w((-1.5, 12, -1.5), (3, 1, 3), band)
    kiste.w((5.6, 9, -1), (0.8, 2, 2), rot)  # Blinklicht
    fs = m.k("fallschirm", "kiste", drehpunkt=(0, 13, 0))
    for sx in (1, -1):
        for sz in (1, -1):
            s = m.k(f"seil_{'v' if sz < 0 else 'h'}{'r' if sx > 0 else 'l'}", "fallschirm", drehpunkt=(sx * 5, 12, sz * 5),
                    drehung=(sz * 24, 0, -sx * 24))
            s.w((sx * 5 - 0.25, 12, sz * 5 - 0.25), (0.5, 32, 0.5), seil)
    fs.w((-20, 40, -20), (40, 2, 40), schirm)
    fs.w((-16, 42, -16), (32, 2, 32), stoff("c8642a", loecher=0, blut=0, falten=5))
    fs.w((-10, 44, -10), (20, 1.5, 20), schirm)
    for sx in (1, -1):
        fs.w((19 if sx > 0 else -20, 36, -20), (1, 4, 40), schirm)
    for sz in (1, -1):
        fs.w((-20, 36, 19 if sz > 0 else -20), (40, 4, 1), schirm)
    return m, "Versorgungskiste", "Luftabwurf · schwebt am Fallschirm, rote Rauchsäule", "2,5 Blöcke Schirm"


ALLE.update({"abwurfkiste": abwurfkiste})
ANSICHTEN["abwurfkiste"] = ((-30, 20), (45, 10), (200, 30))

if __name__ == "__main__":
    for n in (sys.argv[1:] or ALLE):
        print(bauen(n))
