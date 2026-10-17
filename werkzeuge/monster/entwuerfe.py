"""Die Monster von Deadzone MC als Modelle. Jede Funktion baut ein Modell samt Materialien.

  python3 entwuerfe.py [name …]   – erzeugt geo.json, Textur, Glowmask und ein Vorschaublatt
"""
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


def aasgeier():
    """Fliegt: kreist tagsüber über offenem Gelände, stößt auf Verletzte herab und ruft Infizierte zum Aas."""
    m = Modell("aasgeier", 128, 128)
    federn = stoff("4a403a", loecher=0, blut=0, falten=4, fransen=True, schmutz="241e1a")
    schwinge = kombi(stoff("54483e", loecher=0, blut=0, falten=5, fransen=True, schmutz="1a1715"), zerfetzt(4, 2))
    nackt = haut("b0726a", flecken_f="7a3a3a", adern_f="5a2a3a", blut=1)
    p = {"e": farbe("e8e4c0"), "E": farbe("1a0a0a"), "s": farbe("3a3530")}
    koerper = m.k("koerper", drehpunkt=(0, 10, 0), drehung=(8, 0, 0))
    koerper.w((-2.5, 8, -4), (5, 4.5, 9), federn)
    koerper.w((-2, 7.5, -3), (4, 1, 6), nackt)  # kahle Brust
    hals = m.k("hals", "koerper", drehpunkt=(0, 11, -4), drehung=(-35, 0, 0))
    hals.w((-1, 10, -8), (2, 2, 4.5), nackt)
    hals.w((-2, 9.5, -4.5), (4, 3, 1.5), federn, aufblasen=0.2)  # Kragen
    kopf = m.k("kopf", "hals", drehpunkt=(0, 11, -8), drehung=(40, 0, 10))
    kopf.w((-1.5, 9.5, -11), (3, 3, 3.5), nackt, deko=gesicht(["e.e", "E.E"], p, glow="", dy=-1))
    kopf.w((-0.5, 9, -13), (1, 2, 2.2), knochen("3a3530"))
    kopf.w((-0.5, 8.4, -13), (1, 0.8, 0.8), knochen("2a2520"))  # Hakenspitze
    for seite in (1, -1):
        name = "rechts" if seite > 0 else "links"
        x0 = 2.5 if seite > 0 else -12.5
        f = m.k("fluegel_" + name, "koerper", drehpunkt=(seite * 2.5, 11.5, -1), drehung=(0, 0, seite * -12))
        f.w((x0, 11, -3.5), (10, 1, 7), schwinge)
        x1 = 12.5 if seite > 0 else -21.5
        s = m.k("spitze_" + name, "fluegel_" + name, drehpunkt=(seite * 12.5, 11.5, -1), drehung=(0, seite * 10, seite * -8))
        s.w((x1, 11, -3), (9, 0.5, 6), schwinge)
    schwanz = m.k("schwanz", "koerper", drehpunkt=(0, 10, 5), drehung=(-15, 0, 0))
    schwanz.w((-2, 9.5, 5), (4, 1, 5), schwinge)
    for seite in (1, -1):
        b = m.k("fang_" + ("rechts" if seite > 0 else "links"), "koerper", drehpunkt=(seite * 1.2, 8, 1), drehung=(30, 0, 0))
        b.w((seite * 1.2 - 0.5, 4, 0.5), (1, 4, 1), knochen("4a4035"))
        b.w((seite * 1.2 - 1, 3, -0.5), (2, 1, 2), knochen("2a2520"))
    skalieren(m, 1.6)
    return m, "Aasgeier", "Fliegt · kreist über offenem Gelände, stößt auf Verletzte herab", "Spannweite 4 Blöcke"


def hautfluegler():
    """Fliegt: nachts und im Nebel. Packt Spieler, reißt sie in die Höhe und lässt sie fallen."""
    m = Modell("hautfluegler", 128, 64)
    h = haut("4a4440", flecken_f="2e2a28", adern_f="3a2030", blut=1, dunkel_unten=0.25)
    haut_hell = haut("5e5550", flecken_f="3a3330", adern_f="5a2a3a")
    membran = kombi(fleisch("4a2a2a", "6a3a38"), zerfetzt(2, 2),
                    lambda mm, r, f, w: mm.adern(r, farbe("2a1418"), anzahl=max(1, r[2] * r[3] // 30), laenge=7))
    p = {"r": farbe("ff2a1a"), "R": farbe("ffa07a"), "t": farbe("d8d0b0"), "m": farbe("120606"), "n": farbe("2a2220")}
    koerper = m.k("koerper", drehpunkt=(0, 13, 0))
    for seite in (1, -1):
        name = "rechts" if seite > 0 else "links"
        x0 = 0.5 if seite > 0 else -2.5
        o = m.k("bein_" + name, "koerper", drehpunkt=(seite * 1.5, 13, 0), drehung=(-30, 0, seite * 6))
        o.w((x0, 6, -1), (2, 7, 2), h)
        u = m.k("unterbein_" + name, "bein_" + name, drehpunkt=(seite * 1.5, 6, 0), drehung=(60, 0, 0))
        u.w((x0 + 0.25, 0, -0.75), (1.5, 6, 1.5), h)
        u.w((x0 - 0.25, -0.5, -2), (2.5, 1, 3), knochen("2a2622"))
    ober = m.k("oberkoerper", "koerper", drehpunkt=(0, 13, 0), drehung=(-28, 0, 0))
    ober.w((-3, 13, -2), (6, 10, 4), kombi(h, rippen(0.35, "2e2a28")))
    kopf = m.k("kopf", "oberkoerper", drehpunkt=(0, 23, -1), drehung=(30, 0, 0))
    kopf.w((-3, 23, -4.5), (6, 5, 6), haut_hell, deko=gesicht([
        "n....n",
        "rR..Rr",
        "......",
        "tttttt",
        "tmmmmt",
    ], p, glow="rR"))
    kopf.w((-2, 23, -6.5), (4, 3, 2), haut_hell, deko=gesicht(["nn.nn", ".....", "ttttt"], p))  # Schnauze
    for seite in (1, -1):
        ohr = m.k("ohr_" + ("rechts" if seite > 0 else "links"), "kopf", drehpunkt=(seite * 2.5, 28, -1), drehung=(-10, 0, seite * -25))
        ohr.w((seite * 2.5 - 1, 27, -1.5), (2, 5, 1), membran)
    for seite in (1, -1):
        name = "rechts" if seite > 0 else "links"
        x0 = 3 if seite > 0 else -4.5
        a = m.k("arm_" + name, "oberkoerper", drehpunkt=(seite * 3.5, 22, 0), drehung=(0, 0, seite * 125))
        a.w((x0, 8, -0.75), (1.5, 14, 1.5), h)
        f = m.k("finger_" + name, "arm_" + name, drehpunkt=(seite * 3.75, 8, 0), drehung=(0, 0, seite * -25))
        f.w((x0 + 0.25, -6, -0.5), (1, 14, 1), knochen("3a3430"))
        fl = m.k("fluegel_" + name, "arm_" + name, drehpunkt=(seite * 3.5, 22, 0))
        fl.w((x0 + 0.75 - 13 if seite > 0 else x0 + 0.75, -5, 0.25), (13, 27, 0.5), membran)
    return m, "Hautflügler", "Fliegt · Nacht und Nebel; reißt Spieler in die Höhe und lässt sie fallen", "2 Blöcke, 4 Spannweite"


def gefallener_soldat():
    """Tank-light: infizierter Soldat mit Helm und Schutzweste. Körpertreffer prallen ab – nur der Kopf zählt."""
    m = Modell("gefallener_soldat", 128, 64)
    hautf = "7f8a78"
    h = haut(hautf, flecken_f="5a5560", adern_f="3b3a5a", blut=2)
    tarn = kombi(stoff("4f5a3b", haut_f=hautf, loecher=2, blut=3, falten=4, schmutz="2d3022"),
                 lambda mm, r, f, w: mm.flecken(r, farbe("3a4128"), skala=2, schwelle=0.6, staerke=0.8))
    weste = kombi(stoff("2f3326", loecher=0, blut=2, falten=2, schmutz="1e2018"),
                  lambda mm, r, f, w: mm.streifen(r, farbe("23261c"), abstand=3, senkrecht=False, staerke=0.4))
    helm = metall("4a5238", "5a4a2a", nieten=False)
    p = {"e": farbe("d8dccc"), "E": farbe("9aa090"), "m": farbe("2a0d0d"), "t": farbe("c9c1a0"), "b": farbe("5a1010"),
         "s": farbe("3a3d38")}
    koerper = m.k("koerper", drehpunkt=(0, 12, 0))
    for seite in (1, -1):
        b = m.k("bein_" + ("rechts" if seite > 0 else "links"), "koerper", drehpunkt=(seite * 2, 12, 0),
                drehung=(seite * 8, 0, 0))
        b.w((0 if seite > 0 else -4, 0, -2), (4, 12, 4), stoff("4a5038", loecher=1, blut=2, falten=4, haut_f=hautf))
        b.w((0 if seite > 0 else -4, 0, -2), (4, 4, 4), einfarbig("1c1a17", 6), aufblasen=0.25)  # Stiefel
        b.w((0 if seite > 0 else -4, 6, -2.3), (4, 3, 1), metall("3a3f2c", "4a3a22", nieten=False), aufblasen=0.2)  # Knieschoner
    ober = m.k("oberkoerper", "koerper", drehpunkt=(0, 12, 0), drehung=(-10, 0, -4))
    ober.w((-4, 12, -2), (8, 12, 4), tarn)
    ober.w((-4, 14, -2), (8, 9, 4), weste, aufblasen=0.6)
    ober.w((-3, 15, -2.9), (2, 3, 1), stoff("3a3f2c", loecher=0, blut=0, falten=1))  # Magazintaschen
    ober.w((1, 15, -2.9), (2, 3, 1), stoff("3a3f2c", loecher=0, blut=0, falten=1))
    kopf = m.k("kopf", "oberkoerper", drehpunkt=(0, 24, 0), drehung=(18, 8, -12))
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
    kopf.w((-4.5, 26, 1), (9, 4, 3.5), helm, aufblasen=0.3)  # Nackenschutz
    kinn = m.k("kinnriemen", "kopf", drehpunkt=(0, 25, -4))
    kinn.w((3.6, 24, -1), (0.6, 5, 0.6), einfarbig("1e1f1d", 4))
    for seite in (1, -1):
        name = "rechts" if seite > 0 else "links"
        x0 = 4 if seite > 0 else -7
        rot = (-70, 0, 6) if seite > 0 else (-20, 0, -10)
        a = m.k("arm_" + name, "oberkoerper", drehpunkt=(seite * 5, 22, 0), drehung=rot)
        a.w((x0, 10, -1.5), (3, 12, 3), tarn)
        a.w((x0, 9, -1.5), (3, 2, 3), h)
    return m, "Gefallener Soldat", "Tank · Helm und Weste schlucken Körpertreffer, nur Kopfschüsse zählen", "1,9 Blöcke"


def glutleib():
    """Brandopfer: verkohlt, glüht von innen, setzt alles in Brand, was er berührt. Feuer macht ihm nichts."""
    m = Modell("glutleib", 64, 64)
    asche = kombi(haut("2a2220", flecken_f="141010", blut=0, dunkel_unten=0.2), glutrisse(4))
    knochen_ = kombi(knochen("4a4038"), glutrisse(2))
    reste = stoff("1e1a18", loecher=2, blut=0, falten=2, fransen=True, schmutz="0e0c0b")
    p = {"o": farbe("ffb04a"), "O": farbe("ffe7a0"), "k": farbe("100c0a"), "z": farbe("6a5a48")}
    koerper = m.k("koerper", drehpunkt=(0, 12, 0))
    for seite in (1, -1):
        b = m.k("bein_" + ("rechts" if seite > 0 else "links"), "koerper", drehpunkt=(seite * 1.9, 12, 0),
                drehung=(seite * -8, 0, 0))
        b.w((0.15 if seite > 0 else -3.55, 0, -1.6), (3.4, 12, 3.2), asche)
    ober = m.k("oberkoerper", "koerper", drehpunkt=(0, 12, 0), drehung=(-14, 0, 0))
    ober.w((-3.5, 12, -1.75), (7, 11, 3.5), kombi(asche, rippen(0.4, "100c0a")))
    ober.w((-3.5, 12, -1.75), (7, 5, 3.5), reste, aufblasen=0.25)
    kopf = m.k("kopf", "oberkoerper", drehpunkt=(0, 23, 0), drehung=(-8, 0, -18))
    kopf.w((-3.5, 23, -3.5), (7, 7, 7), knochen_, deko=gesicht([
        ".......",
        ".......",
        "kOk.kOk",
        "kok.kok",
        "...k...",
        "zzzzzzz",
        "kzkzkzk",
    ], p, glow="oO"))
    for seite in (1, -1):
        name = "rechts" if seite > 0 else "links"
        x0 = 3.5 if seite > 0 else -6
        a = m.k("arm_" + name, "oberkoerper", drehpunkt=(seite * 4.75, 21.5, 0), drehung=(-60, 0, seite * -10))
        a.w((x0, 9, -1.25), (2.5, 13, 2.5), asche)
        a.w((x0 - 0.25, 7.5, -1.5), (3, 2, 3), knochen_)  # verkrümmte Hand
    return m, "Glutleib", "Brandopfer · glüht von innen, setzt dich in Brand, Feuer macht ihm nichts", "1,9 Blöcke"


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


def sauger():
    """Jäger: wird beim Anschleichen fast unsichtbar, packt zu und saugt Leben aus seinem Opfer."""
    m = Modell("sauger", 128, 64)
    h = haut("8a5a4a", flecken_f="6a3a30", adern_f="4a1a2a", blut=1)
    roh = fleisch("7a2a24", "a8483a")
    lumpen = stoff("2a2622", loecher=2, blut=2, falten=2, fransen=True)
    p = {"e": farbe("ffd84a"), "E": farbe("fff2b0"), "k": farbe("2a1410")}
    koerper = m.k("koerper", drehpunkt=(0, 13, 0))
    for seite in (1, -1):
        b = m.k("bein_" + ("rechts" if seite > 0 else "links"), "koerper", drehpunkt=(seite * 2.25, 13, 0),
                drehung=(-20 if seite > 0 else 10, 0, seite * 4))
        b.w((0.25 if seite > 0 else -4.25, 0, -2), (4, 13, 4), h)
        b.w((0.25 if seite > 0 else -4.25, 9, -2), (4, 4, 4), lumpen, aufblasen=0.25)
    ober = m.k("oberkoerper", "koerper", drehpunkt=(0, 13, 0), drehung=(-32, 0, 0))
    ober.w((-5, 13, -2.5), (10, 12, 5), kombi(h, rippen(0.2, "6a3a30")))
    ober.w((-3, 22, 2.4), (6, 4, 1.5), knochen("b8ae92"))  # Wirbel tritt hervor
    kopf = m.k("kopf", "oberkoerper", drehpunkt=(0, 25, -1), drehung=(32, 0, 0))
    kopf.w((-3.5, 25, -4.5), (7, 7, 7), h, deko=gesicht([
        ".......",
        ".kk.kk.",
        ".Ee.eE.",
        ".......",
        "..kkk..",
        ".kkkkk.",
        ".......",
    ], p, glow="eE"))
    for i, (x, l, r) in enumerate(((-1.75, 6, -8), (-0.6, 8, 4), (0.6, 7, -4), (1.75, 6, 9))):
        t = m.k(f"tentakel_{i}", "kopf", drehpunkt=(x, 27, -4.5), drehung=(10 + i * 4, 0, r))
        t.w((x - 0.5, 27 - l, -5.2), (1, l, 1), roh)
    for seite in (1, -1):
        name = "rechts" if seite > 0 else "links"
        x0 = 5 if seite > 0 else -8
        a = m.k("arm_" + name, "oberkoerper", drehpunkt=(seite * 6.5, 24, 0), drehung=(-50, 0, seite * -12))
        a.w((x0, 9, -1.5), (3, 15, 3), h)
        k = m.k("krallen_" + name, "arm_" + name, drehpunkt=(seite * 6.5, 9, 0), drehung=(-30, 0, 0))
        for dz in (-1, 0, 1):
            k.w((x0 + 1, 4, dz - 0.25), (0.75, 5, 0.75), knochen("2e2620"))
    return m, "Sauger", "Jäger · fast unsichtbar beim Anschleichen, saugt Leben aus", "2,1 Blöcke"


def seuchentraeger():
    """Strahlung: Arbeiter im zerrissenen Schutzanzug. Der leckende Tank auf dem Rücken verstrahlt die Umgebung."""
    m = Modell("seuchentraeger", 128, 64)
    anzug = kombi(stoff("c9b43a", haut_f="7f8a6a", loecher=2, blut=1, falten=4, schmutz="5a5020"),
                  lambda mm, r, f, w: mm.flecken(r, farbe("8a7a28"), skala=2, schwelle=0.62, staerke=0.6))
    gummi = einfarbig("1e1f1c", 6)
    tank = metall("4a5a3a", "6a4a1a")
    gift = leuchtend("9cff4a", 30)
    p = {"v": farbe("1a2a1a"), "V": farbe("6aff6a"), "g": farbe("2a2a26"), "r": farbe("3a3a34")}
    koerper = m.k("koerper", drehpunkt=(0, 12, 0))
    for seite in (1, -1):
        b = m.k("bein_" + ("rechts" if seite > 0 else "links"), "koerper", drehpunkt=(seite * 2, 12, 0),
                drehung=(seite * -6, 0, 0))
        b.w((0 if seite > 0 else -4, 0, -2), (4, 12, 4), anzug)
        b.w((0 if seite > 0 else -4, 0, -2), (4, 4, 4), gummi, aufblasen=0.3)
    ober = m.k("oberkoerper", "koerper", drehpunkt=(0, 12, 0), drehung=(-8, 0, 3))
    ober.w((-4, 12, -2), (8, 12, 4), anzug, aufblasen=0.2)
    ruecken = m.k("tank", "oberkoerper", drehpunkt=(0, 18, 3))
    ruecken.w((-3, 13, 2.5), (6, 10, 4), tank)
    ruecken.w((-1.5, 14, 6.2), (3, 3, 0.6), gift)  # Leck
    ruecken.w((-0.5, 12, 6), (1, 2, 1), gift)
    schlauch = m.k("schlauch", "oberkoerper", drehpunkt=(3, 22, 3), drehung=(-30, 0, 20))
    schlauch.w((2.5, 22, -1), (1, 1, 5), gummi)
    kopf = m.k("kopf", "oberkoerper", drehpunkt=(0, 24, 0), drehung=(14, 0, 16))
    kopf.w((-4, 24, -4), (8, 8, 8), anzug, aufblasen=0.5, deko=gesicht([
        "........",
        "gggggggg",
        "gvVvvVvg",
        "gvvvvvvg",
        "gggggggg",
        "..grrg..",
        "..gggg..",
        "........",
    ], p, glow="V"))
    kopf.w((-1.5, 24, -5.6), (3, 3, 1.5), metall("3a3d3b", "5a3a22"))  # Filter
    for seite in (1, -1):
        name = "rechts" if seite > 0 else "links"
        x0 = 4 if seite > 0 else -7
        rot = (-35, 0, 8) if seite > 0 else (-5, 0, -6)
        a = m.k("arm_" + name, "oberkoerper", drehpunkt=(seite * 5, 22, 0), drehung=rot)
        a.w((x0, 10, -1.5), (3, 12, 3), anzug, aufblasen=0.2)
        a.w((x0, 9, -1.5), (3, 3, 3), gummi, aufblasen=0.3)
    return m, "Seuchenträger", "Strahlung · verstrahlt die Umgebung, hinterlässt eine Giftspur", "1,9 Blöcke"


def keiler():
    """Bestie: mutiertes Wildschwein voller Geschwüre. Stürmt los und schleudert dich durch die Luft."""
    m = Modell("keiler", 128, 64)
    fell = kombi(haut("4a3a2c", flecken_f="2e241c", adern_f=None, blut=0),
                 lambda mm, r, f, w: mm.streifen(r, farbe("2a2018"), abstand=2, senkrecht=True, staerke=0.3))
    nackt = haut("9a6a5a", flecken_f="7a3a34", adern_f="5a2a3a", blut=1)
    p = {"e": farbe("ff4a2a"), "n": farbe("2a1a18"), "N": farbe("5a3a34")}
    koerper = m.k("koerper", drehpunkt=(0, 10, 0))
    koerper.w((-4.5, 6, -8), (9, 9, 16), fell)
    koerper.w((-3.5, 14, -7), (7, 3, 8), fell, aufblasen=0.3)  # Buckel
    geschwuer = kombi(nackt, lambda mm, r, f, w: mm.flecken(r, farbe("c04a3a"), skala=1.5, schwelle=0.55, staerke=0.9),
                      lambda mm, r, f, w: mm.spritzer(r, farbe("e8d070"), 2, 1))
    koerper.w((-4.6, 8, -2), (4, 4, 6), geschwuer, aufblasen=0.4)
    koerper.w((1.5, 10, 2), (3.2, 4, 4), geschwuer, aufblasen=0.5)
    kopf = m.k("kopf", "koerper", drehpunkt=(0, 11, -8), drehung=(18, 0, 0))
    kopf.w((-3.5, 6, -15), (7, 7, 7), fell, deko=gesicht([".......", "e.....e", "......."], p, glow="e", dy=-1))
    kopf.w((-2, 6.5, -17.5), (4, 3.5, 2.5), nackt, deko=gesicht(["NNNN", "nNNn", "NNNN"], p))  # Rüssel
    for seite in (1, -1):
        t = m.k("hauer_" + ("rechts" if seite > 0 else "links"), "kopf", drehpunkt=(seite * 2.2, 7, -16), drehung=(-40, 0, seite * -20))
        t.w((seite * 2.2 - 0.5, 7, -17), (1, 4, 1), knochen("e0d6b8"))
    for z, name in ((-5.5, "v"), (5, "h")):
        for seite in (1, -1):
            b = m.k(f"bein_{name}_{'r' if seite > 0 else 'l'}", "koerper", drehpunkt=(seite * 2.8, 6, z))
            b.w((seite * 2.8 - 1.25, 0, z - 1.25), (2.5, 6, 2.5), fell)
            b.w((seite * 2.8 - 1.25, 0, z - 1.5), (2.5, 1.5, 3), knochen("2a201a"))
    return m, "Keiler", "Bestie · stürmt los und schleudert dich weg; in Wäldern", "1,5 Blöcke, 2,5 lang"


def ertrunkener():
    """Wasser: aufgedunsene Wasserleiche in Flüssen und Kanälen. Zieht Schwimmer unter Wasser."""
    m = Modell("ertrunkener", 128, 64)
    h = haut("7a8a8e", flecken_f="4a6a4a", adern_f="3a4a6a", blut=0, dunkel_unten=0.3)
    algen = kombi(einfarbig("2e4a2a", 10), lambda mm, r, f, w: mm.flecken(r, farbe("1a301a"), skala=2, schwelle=0.55, staerke=0.8))
    kleid = stoff("4a5052", haut_f="7a8a8e", loecher=3, blut=0, falten=4, fransen=True, schmutz="2a3a2a")
    p = {"w": farbe("e8f0ea"), "W": farbe("b8c8c4"), "m": farbe("10181a"), "a": farbe("2e4a2a")}
    koerper = m.k("koerper", drehpunkt=(0, 12, 0))
    for seite in (1, -1):
        b = m.k("bein_" + ("rechts" if seite > 0 else "links"), "koerper", drehpunkt=(seite * 2.2, 12, 0),
                drehung=(seite * 10, 0, 0))
        b.w((0.2 if seite > 0 else -4.2, 0, -2), (4, 12, 4), kleid)
    ober = m.k("oberkoerper", "koerper", drehpunkt=(0, 12, 0), drehung=(-6, 0, 0))
    ober.w((-5, 12, -3), (10, 12, 6), kombi(h, lambda mm, r, f, w: mm.flecken(r, farbe("4a6a4a"), skala=2.5, schwelle=0.62, staerke=0.6)))
    ober.w((-5, 12, -3), (10, 7, 6), kleid, aufblasen=0.3)
    kopf = m.k("kopf", "oberkoerper", drehpunkt=(0, 24, 0), drehung=(-14, 0, 10))
    kopf.w((-4, 24, -4.5), (8, 8, 9), h, deko=gesicht([
        "aaaaaaaa",
        "a......a",
        ".Ww..wW.",
        ".ww..ww.",
        "........",
        "..mmmm..",
        "..mmmm..",
        "...mm...",
    ], p))
    kopf.w((-4, 30, -4.5), (8, 2, 9), algen, aufblasen=0.3)  # Algenhaar
    for i, (x, l) in enumerate(((-3.5, 7), (-1, 9), (2, 6), (3.5, 8))):
        s = m.k(f"strang_{i}", "kopf", drehpunkt=(x, 31, 3), drehung=(15, 0, (i - 1.5) * 6))
        s.w((x - 0.5, 31 - l, 3.5), (1, l, 1), algen)
    for seite in (1, -1):
        name = "rechts" if seite > 0 else "links"
        x0 = 5 if seite > 0 else -8.5
        a = m.k("arm_" + name, "oberkoerper", drehpunkt=(seite * 6.5, 22, 0), drehung=(-75, 0, seite * -4))
        a.w((x0, 9, -1.75), (3.5, 13, 3.5), h)
        a.w((x0, 16, -1.75), (3.5, 4, 3.5), algen, aufblasen=0.15)
    return m, "Ertrunkener", "Wasser · lauert in Flüssen und Kanälen, zieht dich unter Wasser", "2 Blöcke"


ALLE.update({"aasgeier": aasgeier, "hautfluegler": hautfluegler, "gefallener_soldat": gefallener_soldat,
             "glutleib": glutleib, "seuchenhund": seuchenhund, "wuehler": wuehler, "sauger": sauger,
             "seuchentraeger": seuchentraeger, "keiler": keiler, "ertrunkener": ertrunkener})
ANSICHTEN = {"aasgeier": ((-30, 35), (20, 60), (160, 25))}
NEU = ["aasgeier", "hautfluegler", "gefallener_soldat", "glutleib", "seuchenhund", "wuehler", "sauger",
       "seuchentraeger", "keiler", "ertrunkener"]


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


if __name__ == "__main__":
    for n in (sys.argv[1:] or ALLE):
        print(bauen(n))
