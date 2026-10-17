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
    b = blatt(modell, maler.bild, maler.glow, titel, unter, groesse_bloecke=groesse)
    pfad = os.path.join(VORSCHAU, f"{name}.png")
    b.save(pfad)
    return pfad


if __name__ == "__main__":
    for n in (sys.argv[1:] or ALLE):
        print(bauen(n))
