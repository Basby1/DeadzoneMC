"""Die Bewohner der beiden neuen Dimensionen.

  Herz der Zone (Nether): verzerrtes Spiegelbild der Oberwelt, Anomalien, Dauer-Emission – Psi-Mutanten.
  Orbitalstation (End):    zerstörte Station HALDEN-ORBITAL in schwarzer Leere – hier begann alles.

  python3 dimensionen.py [name …]   – geo.json, Textur, Glowmask, Vorschaublatt (wie entwuerfe.py)
"""
import math
import os
import sys

import numpy as np
from PIL import Image

import entwuerfe as E
from entwuerfe import gesicht, rippen, pusteln, glutrisse, zerfetzt, skalieren, kapuze_offen, kleiner_kopf, _mensch
from maler import Maler, farbe, fleisch, haut, kombi, knochen, leuchtend, metall, nebel, stoff, einfarbig
from modell import Modell
from render import blatt


# ---------------------------------------------------------------------------
# Eigene Materialien

def kristall(basis="7fd6e6", hell="e8ffff", alpha=180, glow=True):
    """Anomalieglas: durchscheinend, mit glimmenden Facettenkanten."""
    b = farbe(basis, alpha)
    hl = farbe(hell)

    def malen(m, rect, flaeche, w):
        m.fuellen(rect, b, streuung=18, skala=2, rand=0.3)
        u, v, bb, hh = rect
        for i in range(max(bb, hh)):
            x = u + (i * bb) // max(bb, hh)
            y = v + (i * hh) // max(bb, hh)
            if m.rng.random() < 0.75:
                m.setze(x, y, hl, glow=glow and m.rng.random() < 0.2)
        for _ in range(max(1, bb * hh // 40)):
            m.setze(u + int(m.rng.integers(0, bb)), v + int(m.rng.integers(0, hh)), hl, glow=glow)
    return malen


def warnstreifen(gelb="d8b020", schwarz="1e1e1a"):
    g = farbe(gelb)
    s = farbe(schwarz)

    def malen(m, rect, flaeche, w):
        u, v, bb, hh = rect
        for y in range(hh):
            for x in range(bb):
                m.setze(u + x, v + y, (g if ((x + y) // 2) % 2 == 0 else s) * (0.9 + 0.15 * m.rng.random()))
    return malen


def raumanzug(basis="d8d4c8", schmutz="6a6458", akzent="c8641e"):
    """Weißer, verdreckter Raumanzug mit orangen Nähten."""
    b = farbe(basis)
    a = farbe(akzent)

    def malen(m, rect, flaeche, w):
        m.fuellen(rect, b, streuung=8, skala=3, dunkel_unten=0.2)
        m.streifen(rect, b * 0.75, abstand=4, senkrecht=False, staerke=0.35)
        m.flecken(rect, farbe(schmutz), skala=3, schwelle=0.66, staerke=0.55)
        m.spritzer(rect, farbe("4a1010"), anzahl=1, groesse=2)
        u, v, bb, hh = rect
        if hh >= 6 and flaeche in ("vorne", "hinten", "rechts", "links"):
            for x in range(bb):
                m.setze(u + x, v + hh // 3, a * (0.85 + 0.2 * m.rng.random()))
    return malen


def holo(basis="2a90d8", alpha=140):
    """Hologramm: Scanlinien, durchscheinend; nur Linien und Störstreifen leuchten."""
    b = farbe(basis, alpha)
    hell = farbe("9ae8ff")

    def malen(m, rect, flaeche, w):
        m.fuellen(rect, b, streuung=20, skala=2, rand=0)
        u, v, bb, hh = rect
        for y in range(0, hh, 2):
            m.bild[v + y, u:u + bb, 3] = alpha * 0.4
        for y in range(1, hh, 4):
            for x in range(bb):
                if m.rng.random() < 0.5:
                    m.setze(u + x, v + y, hell * 0.7, glow=m.rng.random() < 0.2)
        for _ in range(max(1, hh // 8)):  # Störstreifen
            y = v + int(m.rng.integers(0, hh))
            for x in range(bb):
                m.setze(u + x, y, farbe("c8f4ff", 220), glow=m.rng.random() < 0.5)
    return malen


def schatten(basis="0c0a10", rand_f="3e2068"):
    """Echo: fast schwarz, violett flimmernde Ränder."""
    b = farbe(basis)
    r = farbe(rand_f)

    def malen(m, rect, flaeche, w):
        m.fuellen(rect, b, streuung=6, skala=2, rand=0)
        u, v, bb, hh = rect
        for x in range(bb):
            if m.rng.random() < 0.18:
                m.setze(u + x, v, r, glow=m.rng.random() < 0.4)
        for y in range(hh):
            if m.rng.random() < 0.15:
                m.setze(u, v + y, r, glow=m.rng.random() < 0.4)
            if m.rng.random() < 0.15:
                m.setze(u + bb - 1, v + y, r, glow=m.rng.random() < 0.4)
        m.flecken(rect, farbe("1e1630"), skala=2, schwelle=0.6, staerke=0.7)
    return malen


def leuchtpunkte(f="5affe8", anzahl=4):
    c = farbe(f)

    def deko(m, rect, flaeche, w):
        u, v, b, h = rect
        if b < 2 or h < 2 or flaeche == "unten":
            return
        for _ in range(max(1, anzahl * b * h // 80)):
            m.setze(u + int(m.rng.integers(0, b)), v + int(m.rng.integers(0, h)), c, glow=True)
    return deko


def name_seite(n):
    return "rechts" if n > 0 else "links"


# ===========================================================================
# HERZ DER ZONE
# ===========================================================================

def lenker():
    """Psi: aufgeblähter Schädel auf einem ausgezehrten Körper im zerrissenen Laborkittel."""
    m = Modell("lenker", 128, 128)
    h = haut("b4a4a0", flecken_f="8a7680", adern_f="6a2a5a", blut=0, dunkel_unten=0.2)
    schaedel = kombi(haut("c4aea8", flecken_f="9a7a8a", adern_f="7a2a6a"),
                     lambda mm, r, f, w: mm.adern(r, farbe("b04aa0"), anzahl=max(2, r[2] * r[3] // 20), laenge=8))
    kittel = stoff("c8c4b4", haut_f="b4a4a0", loecher=2, blut=2, falten=3, fransen=True)
    hose = stoff("3e3e3a", loecher=1, blut=0, falten=3)
    p = {"e": farbe("e05aff"), "E": farbe("ffd8ff"), "s": farbe("3a2430"), "m": farbe("1a0a10"), "t": farbe("d8ccb0")}
    m.k("koerper", drehpunkt=(0, 12, 0))
    for s in (1, -1):
        b = m.k("bein_" + name_seite(s), "koerper", drehpunkt=(s * 2, 12, 0))
        b.w((s * 2 - 1.25, 0, -1.25), (2.5, 12, 2.5), h)
        b.w((s * 2 - 1.5, 5, -1.5), (3, 7, 3), hose)
    ober = m.k("oberkoerper", "koerper", drehpunkt=(0, 12, 0), drehung=(-14, 0, 0))
    ober.w((-3.5, 12, -2), (7, 11, 4), kombi(h, rippen(0.4, "6a5a5a")))
    ober.w((-4.2, 8, -2.5), (2.6, 15, 5), kittel)  # offener Kittel
    ober.w((1.6, 8, -2.5), (2.6, 15, 5), kittel)
    ober.w((-4.2, 8, 2.1), (8.4, 15, 0.6), kittel)
    ober.w((-1, 22.5, -1), (2, 3, 2), h)  # dürrer Hals
    kopf = m.k("kopf", "oberkoerper", drehpunkt=(0, 25, 0), drehung=(18, 0, 10))
    kopf.w((-3, 25, -3.5), (6, 5, 6), h, deko=gesicht([
        "ss..ss",
        "eE..Ee",
        "..ss..",
        ".mmmm.",
        ".mtmt.",
    ], p, glow="eE"))
    # geschwollener, ungleichmäßig wuchernder Schädel – nach hinten und zur Seite ausgebeult
    kopf.w((-5, 29, -5), (10, 9, 10), schaedel)
    kopf.w((-5.5, 31, -1), (11, 10, 10), schaedel)
    kopf.w((-4, 37.5, -3), (9, 5, 11), schaedel)
    kopf.w((-7.5, 33, 0), (3, 7, 7), schaedel)
    kopf.w((4.5, 30, -3), (3, 6, 6), schaedel)
    kopf.w((-2.5, 41.5, 0), (6, 2.5, 6), schaedel)
    kopf.w((-1.5, 35, -5.8), (4, 3, 1.2), schaedel)  # Beule über der Stirn
    kopf.w((-3, 30, 8.5), (6, 7, 2), schaedel)
    for s in (1, -1):
        n = name_seite(s)
        a = m.k("arm_" + n, "oberkoerper", drehpunkt=(s * 4.6, 22, 0), drehung=(-62, s * 10, s * 18))
        x0 = 3.6 if s > 0 else -5.6
        a.w((x0, 8, -1), (2, 14, 2), h)
        a.w((x0 - 0.3, 15, -1.3), (2.6, 7, 2.6), kittel)
        hd = m.k("hand_" + n, "arm_" + n, drehpunkt=(s * 4.6, 8, 0), drehung=(20, 0, 0))
        hd.w((x0 - 0.3, 6, -1.3), (2.6, 2.5, 2.6), h)
        for i, dx in enumerate((-0.2, 0.8, 1.8)):
            hd.w((x0 + dx - 0.2, 1, -0.4 + (i % 2) * 0.6), (0.6, 5, 0.6), h)
    return m, "Der Lenker", "Herz der Zone · Psi: verzerrt deine Sicht, zieht dich heran und lenkt Infizierte wie Puppen", \
        "2,6 Blöcke"


def spuk():
    """Poltergeist: knisternde Lichtkugel, um die Trümmer kreisen. Wirft mit allem, was herumliegt."""
    m = Modell("spuk", 128, 64)
    kern = m.k("kern", drehpunkt=(0, 22, 0))
    kern.w((-2.5, 19.5, -2.5), (5, 5, 5), leuchtend("bfe8ff", 14))
    kern.w((-4, 18, -4), (8, 8, 8), kombi(nebel("5ab0ff", 40, 150), leuchtpunkte("e8f8ff", 6)))
    for i, (rx, rz, l) in enumerate(((30, 10, 7), (-40, 60, 6), (70, -30, 8), (-20, -110, 6), (110, 40, 5))):
        bl = m.k(f"blitz_{i}", "kern", drehpunkt=(0, 22, 0), drehung=(rx, 0, rz))
        bl.w((-0.4, 22, -0.4), (0.8, l, 0.8), leuchtend("e0f6ff", 6))
        bl.w((-0.3, 22 + l - 0.5, 0.3), (0.6, 3, 0.6), leuchtend("7ac8ff", 6))
    ring = m.k("ring", drehpunkt=(0, 22, 0), drehung=(12, 0, 18))
    stein = kombi(einfarbig("6e6a62", 14), lambda mm, r, f, w: mm.flecken(r, farbe("4a4640"), skala=2, schwelle=0.6))
    holz = kombi(einfarbig("6a4a2e", 10), lambda mm, r, f, w: mm.streifen(r, farbe("4a321e"), abstand=2, staerke=0.4))
    teile = [
        ((9, 21, -2), (4, 3, 3.5), stein, (20, 30, 10)),
        ((-15, 22.5, -1), (8, 1.6, 1.6), metall("5a5e60", "7a3a1a", nieten=False), (0, 25, 15)),
        ((-2, 25, 10), (4, 2, 2), einfarbig("8a3a2a", 12), (40, 0, 20)),
        ((-9, 15, 7), (0.6, 5, 4), einfarbig("a8d4dc", 8, alpha=150), (10, 40, -20)),
        ((3, 17, -13), (3, 3, 3), stein, (-30, 10, 45)),
    ]
    for i, (pos, gr, mat, dreh) in enumerate(teile):
        t = m.k(f"truemmer_{i}", "ring", drehpunkt=(pos[0] + gr[0] / 2, pos[1] + gr[1] / 2, pos[2] + gr[2] / 2), drehung=dreh)
        t.w(pos, gr, mat)
    stuhl = m.k("truemmer_5", "ring", drehpunkt=(-10, 26, -10), drehung=(25, 40, 160))
    stuhl.w((-12, 25, -12), (4, 0.8, 4), holz)
    stuhl.w((-12, 25.8, -8.4), (4, 4, 0.6), holz)
    for dx in (0, 3.4):
        for dz in (0, 3.4):
            stuhl.w((-12 + dx, 21.5, -12 + dz), (0.6, 3.5, 0.6), holz)
    return m, "Spuk", "Herz der Zone · Poltergeist: unsichtbar bis auf das Knistern – schleudert Trümmer, Möbel, Steine", \
        "1 Block Kern, schwebt"


def truegruede():
    """Psi-Hund: haarlos, vier Augen. Erzeugt Trugbilder seiner selbst, die beim Treffer zerplatzen."""
    m = Modell("truegruede", 128, 64)
    h = haut("9a7a72", flecken_f="6a4a44", adern_f="5a2a3a", blut=1, dunkel_unten=0.3)
    p = {"e": farbe("ff4ad8"), "E": farbe("ffd0f4"), "k": farbe("2a1a1a")}
    z = {"t": farbe("e8dcc0"), "m": farbe("2a0808"), "r": farbe("7a1a2a"), "n": farbe("1a1010")}
    k = m.k("koerper", drehpunkt=(0, 13, 0))
    k.w((-4.5, 9, -9), (9, 9, 10), kombi(h, rippen(0.35, "5a3a38")))  # Brustkorb, hoch
    k.w((-3.5, 9, 0.5), (7, 7, 9), h)  # schmale Flanke
    for i, z0 in enumerate((-8, -4, 0, 4)):
        k.w((-1, 17.5 - (i * 0.6), z0), (2, 1.5, 2), knochen("bfae98"))  # Wirbel/Geschwülste
    for s in (1, -1):
        n = name_seite(s)
        v = m.k("vorderbein_" + n, "koerper", drehpunkt=(s * 3, 12, -6))
        v.w((s * 3 - 1.5, 0, -7.5), (3, 12, 3), h)
        v.w((s * 3 - 1.8, 0, -8.4), (3.6, 1.6, 3.6), h)
        hb = m.k("hinterbein_" + n, "koerper", drehpunkt=(s * 2.8, 12, 6), drehung=(-20, 0, 0))
        hb.w((s * 2.8 - 1.4, 4, 4.6), (2.8, 8, 3.2), h)
        hu = m.k("hinterlauf_" + n, "hinterbein_" + n, drehpunkt=(s * 2.8, 5, 6), drehung=(40, 0, 0))
        hu.w((s * 2.8 - 1.2, -1, 5), (2.4, 7, 2.4), h)
    kopf = m.k("kopf", "koerper", drehpunkt=(0, 16, -9), drehung=(10, 0, 0))
    kopf.w((-3.5, 13, -15), (7, 7, 7), h, deko=gesicht(["Ee...eE", ".k.e.k.", ".......", "......."], p,
                                                       glow="eE", dy=-1))
    schnauze = m.k("schnauze", "kopf", drehpunkt=(0, 15, -15))
    schnauze.w((-2, 13.5, -20), (4, 3, 5), h, deko=gesicht(["nnnn", "tttt", "tmmt"], z))
    kiefer = m.k("kiefer", "kopf", drehpunkt=(0, 13.5, -15), drehung=(25, 0, 0))
    kiefer.w((-1.8, 12, -19.5), (3.6, 1.5, 4.5), h, deko=gesicht(["tmrt", "tttt"], z))
    for s in (1, -1):
        o = m.k("ohr_" + name_seite(s), "kopf", drehpunkt=(s * 2.5, 20, -10), drehung=(-20, 0, s * 25))
        o.w((s * 2.5 - 0.75, 19.5, -11), (1.5, 3.5, 1), h)
    sw = m.k("schwanz", "koerper", drehpunkt=(0, 15, 9), drehung=(-40, 0, 0))
    sw.w((-0.7, 14, 9), (1.4, 1.4, 6), h)
    return m, "Trugrüde", "Herz der Zone · Rudel: zeigt sich dreifach – nur einer ist echt, die Trugbilder platzen", \
        "1,3 Blöcke"


def entreisser():
    """Telekinet: kleiner, gedrungener Kuttenträger mit riesigen Händen. Reißt dir die Waffe aus der Hand."""
    m = Modell("entreisser", 128, 64)
    h = haut("6e6252", flecken_f="4a4236", adern_f="3a2a22", blut=0)
    kutte = stoff("4a3a2a", loecher=0, blut=0, falten=2, fransen=True, schmutz="2a2018")
    p = {"e": farbe("ffd24a"), "E": farbe("fff4c0"), "s": farbe("1a140e"), "m": farbe("2a1a10"), "t": farbe("b8a888")}
    m.k("koerper", drehpunkt=(0, 6, 0))
    for s in (1, -1):
        b = m.k("bein_" + name_seite(s), "koerper", drehpunkt=(s * 2.5, 6, 0))
        b.w((s * 2.5 - 1.75, 0, -1.75), (3.5, 6, 3.5), h)
    ober = m.k("oberkoerper", "koerper", drehpunkt=(0, 6, 0), drehung=(-12, 0, 0))
    ober.w((-5, 5, -3.5), (10, 9, 7), h)  # dicker Bauch
    ober.w((-5.5, 3, -4), (11, 12, 8), kutte, aufblasen=0.2)
    ober.w((-5, 2, -4.3), (10, 3, 8.6), kutte)  # Saum
    kopf = m.k("kopf", "oberkoerper", drehpunkt=(0, 15, -1), drehung=(15, 0, 0))
    kopf.w((-3.5, 15, -4.5), (7, 6, 6), h, deko=gesicht([
        "sssssss",
        ".Ee.eE.",
        "...s...",
        ".mtttm.",
        "..mmm..",
    ], p, glow="eE"))
    kopf.w((-4.5, 14.5, -5.2), (9, 8, 8), kutte, deko=kapuze_offen, aufblasen=0.15)
    kopf.w((-3, 21.5, -2), (6, 2, 6), kutte)
    for s in (1, -1):
        n = name_seite(s)
        a = m.k("arm_" + n, "oberkoerper", drehpunkt=(s * 6, 13, -1), drehung=(-75, s * -15, s * 10))
        x0 = 5 if s > 0 else -8
        a.w((x0, 5, -2), (3, 8, 3.5), kutte)
        hd = m.k("hand_" + n, "arm_" + n, drehpunkt=(s * 6.5, 5, 0), drehung=(-30, 0, 0))
        hd.w((x0 - 1.5, 1, -2.5), (6, 5, 2), h)  # riesige, offene Hand
        for i in range(4):
            hd.w((x0 - 1.4 + i * 1.5, -3.5, -2.2), (1.1, 4.5, 1.4), h)
        hd.w((x0 + (5 if s > 0 else -1.5), 2, -2.2), (1.5, 1.2, 1.4), h)  # Daumen
    return m, "Entreißer", "Herz der Zone · Telekinet: reißt dir die Waffe aus der Hand, Kugeln prallen an seinem Schild ab", \
        "1,4 Blöcke"


def chimaere():
    """Zweiköpfige Bestie: wuchtige Front, sehnige Hinterläufe, Knochenkeule am Schwanz. Springt 15 Blöcke weit."""
    m = Modell("chimaere", 256, 128)
    h = haut("8a5a4e", flecken_f="5a3a34", adern_f="3a1a1a", blut=2, dunkel_unten=0.3)
    roh = fleisch("6a2020", "9a3a30")
    p = {"e": farbe("ffb030"), "k": farbe("1a0a0a")}
    z = {"t": farbe("e8dcbc"), "m": farbe("2a0808"), "r": farbe("6a1a1a")}
    k = m.k("koerper", drehpunkt=(0, 18, 0))
    k.w((-7, 13, -12), (14, 13, 12), kombi(h, rippen(0.25, "5a3a34")))  # Front
    k.w((-5, 13, -0.5), (10, 10, 13), h)
    k.w((-3, 16, 12), (6, 6, 4), h)
    for i in range(6):
        k.w((-0.75, 25.5 - i * 0.8, -11 + i * 4), (1.5, 3, 1.5), knochen("d8ccb0"))  # Rückenstacheln
    k.w((-6.5, 19, -12.3), (5, 5, 1), roh)
    for s in (1, -1):
        n = name_seite(s)
        v = m.k("vorderbein_" + n, "koerper", drehpunkt=(s * 6, 16, -7), drehung=(-8, 0, s * 6))
        v.w((s * 6 - 2.5, 3, -9.5), (5, 13, 5), h)
        vp = m.k("vorderpranke_" + n, "vorderbein_" + n, drehpunkt=(s * 6, 3, -7))
        vp.w((s * 6 - 3, 0, -11), (6, 3, 7), h)
        for dx in (-2, 0, 2):
            vp.w((s * 6 + dx - 0.5, 0, -12.5), (1, 1.5, 2), knochen("2a2420"))
        hb = m.k("hinterbein_" + n, "koerper", drehpunkt=(s * 4.5, 16, 8), drehung=(-30, 0, 0))
        hb.w((s * 4.5 - 2, 8, 6), (4, 9, 5), h)
        hl = m.k("hinterlauf_" + n, "hinterbein_" + n, drehpunkt=(s * 4.5, 9, 8), drehung=(60, 0, 0))
        hl.w((s * 4.5 - 1.5, 0, 7), (3, 10, 3), h)
        hf = m.k("hinterfuss_" + n, "hinterlauf_" + n, drehpunkt=(s * 4.5, 1, 8), drehung=(-30, 0, 0))
        hf.w((s * 4.5 - 1.8, -1, 4), (3.6, 2, 5), h)
        kp = m.k("kopf_" + n, "koerper", drehpunkt=(s * 3.5, 22, -12), drehung=(10, s * -22, s * 12))
        kp.w((s * 3.5 - 3, 19, -20), (6, 6, 8), h, deko=gesicht([".k..k.", ".e..e.", "......"], p, glow="e", dy=-1))
        kp.w((s * 3.5 - 2.5, 19, -24), (5, 3, 4), h, deko=gesicht(["ttttt", "mmmmm", "trtrt"], z))
        kf = m.k("kiefer_" + n, "kopf_" + n, drehpunkt=(s * 3.5, 19, -18), drehung=(30, 0, 0))
        kf.w((s * 3.5 - 2.3, 17, -23.5), (4.6, 2, 6), h, deko=gesicht(["trtrt", "mmmmm"], z))
        kp.w((s * 3.5 + s * 2.5 - 0.5, 24, -16), (1, 4, 1), knochen("d8ccb0"))  # Horn
    sw = m.k("schwanz", "koerper", drehpunkt=(0, 19, 15), drehung=(-35, 0, 0))
    sw.w((-1, 18, 15), (2, 2, 10), h)
    sk = m.k("schwanzkeule", "schwanz", drehpunkt=(0, 19, 25), drehung=(-25, 0, 0))
    sk.w((-1, 18, 25), (2, 2, 6), h)
    sk.w((-2.5, 16.5, 30), (5, 5, 5), knochen("cfc0a0"))
    return m, "Chimäre", "Herz der Zone · Jäger: zwei Köpfe, zwei Bisse – springt aus großer Entfernung an", "2,3 Blöcke lang"


def glasgaenger():
    """Kristallmensch: aus Anomalieglas gewachsen, Splitter überall. Kugeln prallen ab – nur Nahkampf bricht ihn."""
    m = Modell("glasgaenger", 128, 128)
    kr = kristall("5ab0d0", "c8f4ff", 185, glow=False)
    dunkel = kristall("2a5080", "8ad0ff", 215)
    kern = leuchtend("c08aff", 14)
    m.k("koerper", drehpunkt=(0, 13, 0))
    for s in (1, -1):
        n = name_seite(s)
        b = m.k("bein_" + n, "koerper", drehpunkt=(s * 2.2, 13, 0), drehung=(0, s * 8, s * 4))
        b.w((s * 2.2 - 1.5, 0, -1.5), (3, 13, 3), kr)
        b.w((s * 2.2 - 0.6 + s, 8, -2.8), (1.2, 4, 1.2), dunkel)
    ober = m.k("oberkoerper", "koerper", drehpunkt=(0, 13, 0), drehung=(-6, 0, 0))
    ober.w((-4.5, 13, -2.5), (9, 12, 5), kr)
    ober.w((-1.5, 17, -3), (3, 3, 1), kern)  # Kern hinter dem Glas
    for i, (x, y, rz, l) in enumerate(((-4, 23, 35, 7), (3, 24, -30, 8), (-1, 25, 8, 6), (4.5, 18, -60, 5), (-5, 16, 70, 4))):
        sp = m.k(f"splitter_{i}", "oberkoerper", drehpunkt=(x, y, 0), drehung=(-15 + i * 7, 0, rz))
        sp.w((x - 0.8, y, -0.8), (1.6, l, 1.6), dunkel if i % 2 else kr)
    kopf = m.k("kopf", "oberkoerper", drehpunkt=(0, 25, 0), drehung=(0, 0, 8))
    kopf.w((-3.5, 25, -3.5), (7, 8, 7), kr, deko=gesicht([".......", "eeeeeee", "......."],
                                                        {"e": farbe("f0d8ff")}, glow="e"))
    kopf.w((-1, 32, -1), (2, 5, 2), dunkel)
    kopf.w((1.5, 30, -2), (1.5, 4, 1.5), kr)
    for s in (1, -1):
        n = name_seite(s)
        a = m.k("arm_" + n, "oberkoerper", drehpunkt=(s * 5.5, 24, 0), drehung=(-20, 0, s * 12))
        x0 = 4.5 if s > 0 else -7.5
        a.w((x0, 13, -1.5), (3, 11, 3), kr)
        kl = m.k("klinge_" + n, "arm_" + n, drehpunkt=(s * 6, 13, 0), drehung=(-10, 0, 0))
        kl.w((x0 + 0.5, 2, -0.5), (2, 11, 1), dunkel)  # Arm endet in einer Glasklinge
        kl.w((x0 + 0.9, -1, -0.3), (1.2, 3, 0.6), kr)
    return m, "Glasgänger", "Herz der Zone · Kristall: Kugeln prallen ab – Nahkampf oder Sprengstoff zerbrechen ihn", \
        "2,3 Blöcke"


def schwebender():
    """Leiche im Schwerkraftfeld: treibt waagerecht in der Luft, Glieder hängen. Hebt dich an und lässt dich fallen."""
    p = {"w": farbe("e8e8dc"), "s": farbe("2a2420"), "m": farbe("1a0c0c"), "h": farbe("3a2a20"), "t": farbe("c8bca0")}
    gesicht_ = ["hhhhhhhh", "h......h", ".ww..ww.", ".ss..ss.", "........", "..mmmm..", "..mttm..", "...mm..."]
    jacke = stoff("4a5a6a", haut_f="9aa0a0", loecher=3, blut=2, falten=3, fransen=True)
    m = _mensch("schwebender", "9aa0a0", gesicht_, p, jacke, stoff("3a3a3a", haut_f="9aa0a0", loecher=2, blut=1),
                einfarbig("2a2420", 6))
    kn = {k.name: k for k in m.knochen}
    kn["koerper"].drehung = [6, 0, 8]  # hängt schlaff in der Luft, Zehen nach unten
    kn["kopf"].drehung = [45, 0, 25]  # Kopf baumelt
    kn["arm_rechts"].drehung = [10, 0, 35]
    kn["arm_links"].drehung = [-5, 0, -28]
    kn["bein_rechts"].drehung = [12, 0, 6]
    kn["bein_links"].drehung = [-8, 0, -5]
    kn["kopf"].w((-4.2, 30.5, -4.2), (8.4, 2, 8.4), einfarbig("2a2018", 10))  # verfilztes Haar
    kn["kopf"].w((-4.2, 26, 2.5), (8.4, 5, 1.8), einfarbig("2a2018", 10))
    for k in m.knochen:  # alles 12 Einheiten anheben: schwebt
        k.drehpunkt[1] += 12
        for w in k.wuerfel:
            w.ursprung[1] += 12
    feld = m.k("feld", drehpunkt=(0, 6, 0))
    feld.w((-9, 3, -9), (18, 1, 18), nebel("8a6aff", 20, 110))
    feld.w((-6, 0.5, -6), (12, 1, 12), nebel("b89aff", 30, 140))
    for i, (x, z, y) in enumerate(((7, -5, 6), (-8, 3, 9), (3, 8, 4), (-4, -9, 12))):
        st = m.k(f"stein_{i}", "feld", drehpunkt=(x, y, z), drehung=(i * 30, i * 20, 0))
        st.w((x - 1, y - 1, z - 1), (2, 2, 2), einfarbig("5e5a54", 14))
    return m, "Schwebender", "Herz der Zone · Schwerkraft: treibt in der Luft, reißt dich hoch – und lässt dich fallen", \
        "schwebt 1–2 Blöcke"


def entlader():
    """Elektro-Mutant: verkohlter Leitungsmonteur, eingewickelt in Kupferkabel, Isolatoren wachsen aus den Schultern."""
    m = Modell("entlader", 128, 128)
    h = kombi(haut("3a3430", flecken_f="1e1a18", blut=0), glutrisse(1, "6ad8ff"))
    kopfhaut = haut("3a3430", flecken_f="1e1a18", blut=0)
    kabel = kombi(metall("b8682a", "3a6a5a", nieten=False),
                  lambda mm, r, f, w: mm.streifen(r, farbe("7a3a14"), abstand=2, senkrecht=False, staerke=0.5))
    iso = kombi(einfarbig("d8d0c0", 8), lambda mm, r, f, w: mm.streifen(r, farbe("8a8478"), abstand=2, senkrecht=False))
    blitz = leuchtend("d0f4ff", 8)
    weste = stoff("c86a1e", loecher=2, blut=0, falten=3, schmutz="2a2018")
    p = {"e": farbe("bff0ff"), "s": farbe("0a0a0a"), "m": farbe("6ad8ff")}
    m.k("koerper", drehpunkt=(0, 13, 0))
    for s in (1, -1):
        n = name_seite(s)
        b = m.k("bein_" + n, "koerper", drehpunkt=(s * 2, 13, 0))
        b.w((s * 2 - 1.5, 0, -1.5), (3, 13, 3), h)
        b.w((s * 2 - 1.7, 4, -1.7), (3.4, 1, 3.4), kabel)
        b.w((s * 2 - 1.7, 8, -1.7), (3.4, 1, 3.4), kabel)
    ober = m.k("oberkoerper", "koerper", drehpunkt=(0, 13, 0), drehung=(-10, 0, -5))
    ober.w((-4, 13, -2.5), (8, 12, 5), kombi(h, rippen(0.3, "1a1614")))
    ober.w((-4.3, 16, -2.8), (8.6, 9, 5.6), weste)  # Warnweste
    ober.w((-1.5, 17, -3.2), (3, 3, 1), blitz)  # Herz: Funkenbogen
    for i, y in enumerate((14, 18, 22)):
        ober.w((-4.6, y, -3.1), (9.2, 1, 6.2), kabel)
    for s in (1, -1):
        for i, (dz, l) in enumerate(((-1, 5), (1.5, 4))):
            ist = m.k(f"isolator_{name_seite(s)}_{i}", "oberkoerper", drehpunkt=(s * 3, 25, dz),
                      drehung=(10 - i * 25, 0, s * -25))
            ist.w((s * 3 - 1, 25, dz - 1), (2, l, 2), iso)
            ist.w((s * 3 - 0.5, 25 + l, dz - 0.5), (1, 1.5, 1), blitz)
    kopf = m.k("kopf", "oberkoerper", drehpunkt=(0, 25, 0), drehung=(10, 0, 12))
    kopf.w((-3.5, 25, -3.5), (7, 7, 7), kopfhaut, deko=gesicht([
        ".......",
        ".ee.ee.",
        ".......",
        "..smm..",
        ".smmms.",
    ], p, glow="em"))
    kopf.w((-3.8, 30, -3.8), (7.6, 2.5, 7.6), stoff("d8b020", loecher=0, blut=0, falten=4))  # Bauhelm
    kopf.w((-3, 30.2, -5), (6, 0.8, 1.4), stoff("d8b020", loecher=0, blut=0, falten=4))
    for s in (1, -1):
        n = name_seite(s)
        a = m.k("arm_" + n, "oberkoerper", drehpunkt=(s * 5, 23, 0), drehung=(-25 if s > 0 else 15, 0, s * 20))
        x0 = 4 if s > 0 else -7
        a.w((x0, 11, -1.5), (3, 12, 3), h)
        a.w((x0 - 0.3, 14, -1.8), (3.6, 1, 3.6), kabel)
        a.w((x0 + 1, 9, -0.3), (1, 2, 0.6), blitz)  # Funken an den Fingern
    for i, (x, z, l, rz) in enumerate(((5, -3, 10, -60), (-6, 2, 8, 70))):
        kb = m.k(f"kabelende_{i}", "oberkoerper", drehpunkt=(x, 15, z), drehung=(20, 0, rz))
        kb.w((x - 0.4, 15 - l, z - 0.4), (0.8, l, 0.8), kabel)
        kb.w((x - 0.5, 14 - l, z - 0.5), (1, 1, 1), blitz)
    return m, "Entlader", "Herz der Zone · Elektro: springt wie ein Blitz von Punkt zu Punkt, entlädt sich in Metall", \
        "2,1 Blöcke"


def echo():
    """Spiegelgänger: schwarze Kopie eines Menschen, trägt dieselbe Waffe wie du, macht dir alles nach."""
    m = Modell("echo", 128, 64)
    sch = schatten()
    rauch = nebel("2a1a3a", 40, 170)
    p = {"w": farbe("f4f0ff"), "W": farbe("ffffff")}
    m.k("koerper", drehpunkt=(0, 12, 0))
    for s in (1, -1):
        n = name_seite(s)
        b = m.k("bein_" + n, "koerper", drehpunkt=(s * 2, 12, 0))
        b.w((0 if s > 0 else -4, 0, -2), (4, 12, 4), sch)
        b.w((0 if s > 0 else -4, -0.5, -2.5), (4, 3, 5), rauch, aufblasen=0.4)
    ober = m.k("oberkoerper", "koerper", drehpunkt=(0, 12, 0))
    ober.w((-4, 12, -2), (8, 12, 4), sch)
    ober.w((-4.3, 12, 1.9), (8.6, 10, 2), rauch)  # zerfasert nach hinten
    kopf = m.k("kopf", "oberkoerper", drehpunkt=(0, 24, 0), drehung=(0, 0, -6))
    kopf.w((-4, 24, -4), (8, 8, 8), sch, deko=gesicht(["........", "........", ".wW..Ww.", "........"], p, glow="wW"))
    kopf.w((-4.4, 28, 2), (8.8, 5, 3), rauch)
    for s in (1, -1):
        n = name_seite(s)
        a = m.k("arm_" + n, "oberkoerper", drehpunkt=(s * 5, 22, 0), drehung=((-60, 0, 0) if s > 0 else (-8, 0, -6)))
        x0 = 4 if s > 0 else -8
        a.w((x0, 12, -2), (4, 12, 4), sch)
        a.w((x0 - 0.3, 11, -2.3), (4.6, 3, 4.6), rauch)
    return m, "Echo", "Herz der Zone · Spiegel: dein schwarzes Ebenbild – trägt deine Waffe und macht dir alles nach", \
        "1,9 Blöcke"


def zonenhirsch():
    """Mutierter Hirsch: Rippen offen, sechs Augen, im Geweih wachsen glühende Artefakte. Scheu – bis er angreift."""
    m = Modell("zonenhirsch", 256, 128)
    fell = kombi(haut("7a6248", flecken_f="5a4434", blut=0, dunkel_unten=0.3),
                 lambda mm, r, f, w: mm.flecken(r, farbe("a07a5a"), skala=2, schwelle=0.7, staerke=0.5))
    h = haut("a8786a", flecken_f="7a4a44", adern_f="5a2a2a", blut=1)
    geweih = knochen("cfc4a8")
    art = leuchtend("ff9a3a", 20)
    art2 = leuchtend("c06aff", 20)
    p = {"e": farbe("ffcf4a"), "k": farbe("1a0e08"), "n": farbe("2a1a14")}
    k = m.k("koerper", drehpunkt=(0, 22, 0))
    k.w((-5, 18, -11), (10, 10, 22), fell)
    k.w((-5.3, 18.5, -8), (2, 8, 9), kombi(h, rippen(0.5, "e0d4bc")))  # offene Rippen
    k.w((-1, 28, -9), (2, 1.5, 16), knochen("bfb498"))  # Wirbelsäule tritt hervor
    for s in (1, -1):
        n = name_seite(s)
        v = m.k("vorderbein_" + n, "koerper", drehpunkt=(s * 3.2, 19, -8))
        v.w((s * 3.2 - 1.2, 0, -9.2), (2.4, 19, 2.4), fell)
        v.w((s * 3.2 - 1.3, 0, -9.5), (2.6, 2, 2.6), einfarbig("2a2018", 6))
        hb = m.k("hinterbein_" + n, "koerper", drehpunkt=(s * 3.2, 20, 8), drehung=(-15, 0, 0))
        hb.w((s * 3.2 - 1.5, 10, 6.5), (3, 10, 4), fell)
        hl = m.k("hinterlauf_" + n, "hinterbein_" + n, drehpunkt=(s * 3.2, 11, 8), drehung=(20, 0, 0))
        hl.w((s * 3.2 - 1.1, 0, 7.5), (2.2, 11, 2.2), fell)
    hals = m.k("hals", "koerper", drehpunkt=(0, 26, -10), drehung=(-40, 0, 0))
    hals.w((-2.5, 24, -14), (5, 12, 5), fell)
    kopf = m.k("kopf", "hals", drehpunkt=(0, 36, -12), drehung=(45, 0, 0))
    kopf.w((-3, 34, -16), (6, 6, 7), fell, deko=gesicht(["e.e..e.e"[:6], "k.ee.k", "......"], p, glow="e", dy=-1))
    kopf.w((-2, 34, -21), (4, 4, 5), fell, deko=gesicht(["nnnn", "....", "kkkk"], p))
    for s in (1, -1):
        n = name_seite(s)
        g = m.k("geweih_" + n, "kopf", drehpunkt=(s * 2, 40, -13), drehung=(-20, 0, s * -30))
        g.w((s * 2 - 0.6, 40, -13.6), (1.2, 9, 1.2), geweih)
        for i, (y, rx, l) in enumerate(((43, -50, 5), (46, 40, 4), (48, -30, 4))):
            sg = m.k(f"spross_{n}_{i}", "geweih_" + n, drehpunkt=(s * 2, y, -13), drehung=(rx, 0, s * -20))
            sg.w((s * 2 - 0.5, y, -13.5), (1, l, 1), geweih)
            if i != 1:
                sg.w((s * 2 - 1, y + l, -14), (2, 2, 2), art if (i + (s > 0)) % 2 else art2)
        g.w((s * 2 - 1.2, 49, -14.2), (2.4, 2.4, 2.4), art2 if s > 0 else art)
    sw = m.k("schwanz", "koerper", drehpunkt=(0, 27, 11), drehung=(-50, 0, 0))
    sw.w((-1, 26, 11), (2, 2, 3), fell)
    return m, "Zonenhirsch", "Herz der Zone · Wild: scheu, im Geweih wachsen Artefakte – in die Enge getrieben greift er an", \
        "3 Blöcke mit Geweih"


# ===========================================================================
# ORBITALSTATION HALDEN-ORBITAL
# ===========================================================================

def leerlaeufer():
    """Infizierter Kosmonaut: Raumanzug, gesprungenes Visier, dahinter ein verwestes Gesicht. Treibt schwerelos."""
    m = Modell("leerlaeufer", 128, 128)
    anzug = raumanzug()
    dunkel = metall("5a5e62", "3a3a3a", nieten=False)
    p = {"g": farbe("1a2228"), "G": farbe("2e3e48"), "h": farbe("8a9a72"), "e": farbe("f4f0d0"), "E": farbe("1a1a10"),
         "m": farbe("2a0a0a"), "c": farbe("e0f0f8"), "t": farbe("d8ccaa")}
    m.k("koerper", drehpunkt=(0, 13, 0))
    for s in (1, -1):
        n = name_seite(s)
        b = m.k("bein_" + n, "koerper", drehpunkt=(s * 2.5, 13, 0), drehung=(s * 14, 0, s * 6))
        b.w((s * 2.5 - 2.25, 0, -2.25), (4.5, 13, 4.5), anzug)
        b.w((s * 2.5 - 2.5, 0, -2.6), (5, 3.5, 5.2), dunkel)  # Stiefel
    ober = m.k("oberkoerper", "koerper", drehpunkt=(0, 13, 0), drehung=(-8, 0, 4))
    ober.w((-5.5, 13, -3.5), (11, 13, 7), anzug)
    ober.w((-2, 20, -4), (4, 3, 0.6), kombi(einfarbig("2a3a6a", 6), leuchtpunkte("ff4a3a", 3)))  # Bedienteil
    ober.w((-4.5, 12, 3.5), (9, 12, 5), dunkel)  # Lebenserhaltung
    ober.w((-3.5, 22, 8.5), (2, 2, 1), leuchtend("ff3a2a", 4))  # Warnlampe
    sch = m.k("schlauch", "oberkoerper", drehpunkt=(4, 20, 4), drehung=(0, 0, -40))
    sch.w((3.5, 14, 3.5), (1.2, 8, 1.2), einfarbig("3a3a3a", 4))
    kopf = m.k("kopf", "oberkoerper", drehpunkt=(0, 26, 0), drehung=(12, 0, -14))
    helm = kombi(einfarbig("e0dcd2", 6), lambda mm, r, f, w: mm.flecken(r, farbe("7a7468"), skala=3, schwelle=0.7))
    kopf.w((-5, 25.5, -5), (10, 10, 10), helm, deko=gesicht([
        "GGGGGGGG",
        "GghhhhcG",
        "GhEehEeG",
        "GhhhchhG",
        "GhmmmmcG",
        "GgmtmthG",
        "GGGGGGGG",
    ], p))
    kopf.w((-1, 35.5, -1), (2, 1.5, 2), dunkel)
    for s in (1, -1):
        n = name_seite(s)
        a = m.k("arm_" + n, "oberkoerper", drehpunkt=(s * 6.5, 24, 0), drehung=((-50, 0, 25) if s > 0 else (-20, 0, -35)))
        x0 = 5.5 if s > 0 else -9.5
        a.w((x0, 13, -2), (4, 11, 4), anzug)
        a.w((x0 - 0.4, 10, -2.4), (4.8, 4, 4.8), dunkel)  # Handschuh
    return m, "Leerläufer", "Orbitalstation · Kosmonaut: treibt schwerelos durch die Gänge, hinter dem Visier verwest ein Gesicht",\
        "2,2 Blöcke"


def wartungsspinne():
    """Reparaturroboter mit sechs Beinen: Schweißbrenner und Trennscheibe. Repariert noch immer – an dir."""
    m = Modell("wartungsspinne", 128, 128)
    gehaeuse = kombi(metall("c8b48a", "6a4a2a"), lambda mm, r, f, w: mm.flecken(r, farbe("3a3a34"), skala=3, schwelle=0.72))
    dunkel = metall("3a3e42", "4a3a2a", nieten=False)
    warn = warnstreifen()
    rot = leuchtend("ff2a1a", 8)
    k = m.k("koerper", drehpunkt=(0, 10, 0))
    k.w((-5, 8, -6), (10, 5, 12), gehaeuse)
    k.w((-4, 13, -4), (8, 2, 8), dunkel)
    k.w((-5.2, 8, -6.2), (10.4, 1.2, 12.4), warn)
    k.w((-3, 9, -7.5), (6, 4, 1.5), dunkel)
    for x, y in ((-2, 10.5), (0.5, 11.5), (2, 9.8)):
        k.w((x - 0.6, y, -8), (1.2, 1.2, 0.6), rot)  # Sensorhaufen
    k.w((-0.3, 15, 1), (0.6, 5, 0.6), dunkel)  # Antenne
    k.w((-0.6, 20, 0.7), (1.2, 1.2, 1.2), leuchtend("ffb030", 6))
    for i, z in enumerate((-4, 0, 4)):
        for s in (1, -1):
            n = f"{name_seite(s)}_{i}"
            o = m.k("bein_" + n, "koerper", drehpunkt=(s * 5, 10, z), drehung=(0, s * (i - 1) * -25, s * -40))
            o.w((s * 5 - (0 if s > 0 else 8), 9.25, z - 0.75), (8, 1.5, 1.5), dunkel)
            u = m.k("unterbein_" + n, "bein_" + n, drehpunkt=(s * 13, 10, z), drehung=(0, 0, s * 75))
            u.w((s * 13 - 0.75, 0, z - 0.75), (1.5, 10, 1.5), gehaeuse)
            u.w((s * 13 - 0.4, -1.5, z - 0.4), (0.8, 1.5, 0.8), dunkel)
    for s in (1, -1):
        n = name_seite(s)
        a = m.k("werkzeugarm_" + n, "koerper", drehpunkt=(s * 3, 11, -6), drehung=(-20, s * 20, 0))
        a.w((s * 3 - 0.75, 10.25, -14), (1.5, 1.5, 8), dunkel)
        if s > 0:
            a.w((s * 3 - 1, 9.5, -16), (2, 3, 2), gehaeuse)  # Schweißbrenner
            a.w((s * 3 - 0.4, 10.4, -18), (0.8, 0.8, 2), leuchtend("8ad8ff", 6))
        else:
            sc = m.k("trennscheibe", "werkzeugarm_" + n, drehpunkt=(s * 3, 11, -15))
            sc.w((s * 3 - 0.3, 7, -18), (0.6, 8, 8), kombi(metall("9aa0a4", "5a3a2a", nieten=False),
                                                         lambda mm, r, f, w: mm.spritzer(r, farbe("6a0a0a"), 2, 2)))
    return m, "Wartungsspinne", "Orbitalstation · Robotik: krabbelt an Decken, schweißt und sägt – repariert noch immer, an dir", \
        "1,2 Blöcke, 2 breit"


def wachkugel():
    """Sicherheitsdrohne: schwebende Kugel mit rotem Objektiv, schaltet Schilde und feuert Laserstöße."""
    m = Modell("wachkugel", 128, 64)
    huelle = kombi(einfarbig("dcdcd4", 6), lambda mm, r, f, w: mm.flecken(r, farbe("8a8a80"), skala=3, schwelle=0.7))
    dunkel = metall("2e3236", "4a3a2a", nieten=False)
    k = m.k("kugel", drehpunkt=(0, 22, 0))
    k.w((-5, 17, -5), (10, 10, 10), huelle)
    k.w((-6, 18.5, -3.5), (12, 7, 7), huelle)
    k.w((-3.5, 18.5, -6), (7, 7, 12), huelle)
    k.w((-3.5, 15.5, -3.5), (7, 13, 7), huelle)
    k.w((-6.2, 21.5, -6.2), (12.4, 1, 12.4), dunkel)  # Fuge
    lin = m.k("linse", "kugel", drehpunkt=(0, 22, -6))
    lin.w((-2.5, 19.5, -6.6), (5, 5, 1), dunkel)
    lin.w((-1.5, 20.5, -7.2), (3, 3, 1), leuchtend("ff2a1a", 6), deko=gesicht(["...", ".W.", "..."], {"W": farbe("ffd0c0")}, glow="W"))
    for s in (1, -1):
        an = m.k("antenne_" + name_seite(s), "kugel", drehpunkt=(s * 4, 27, 2), drehung=(20, 0, s * -25))
        an.w((s * 4 - 0.3, 27, 1.7), (0.6, 5, 0.6), dunkel)
        d = m.k("duese_" + name_seite(s), "kugel", drehpunkt=(s * 5, 16, 0), drehung=(0, 0, s * 15))
        d.w((s * 5 - 1.5, 13, -1.5), (3, 3, 3), dunkel)
        d.w((s * 5 - 1, 11.5, -1), (2, 1.5, 2), leuchtend("4ab0ff", 10))
        d.w((s * 5 - 1.5, 8, -1.5), (3, 3.5, 3), nebel("6ac0ff", 20, 120))
    schild = m.k("schild", "kugel", drehpunkt=(0, 22, 0))
    schild.w((-9, 13, -9), (18, 18, 18), nebel("5ab4ff", 10, 60))
    return m, "Wachkugel", "Orbitalstation · Sicherheit: schwebt, erfasst dich mit rotem Laser – Schild muss erst fallen", \
        "1 Block, schwebt"


def versuchsobjekt():
    """Entlaufene Testperson: groß, ausgemergelt, Zwangsjacke in Fetzen, Infusionsschläuche hängen aus dem Rücken."""
    m = Modell("versuchsobjekt", 128, 128)
    h = haut("b8c4c8", flecken_f="8a9aa4", adern_f="4a5a8a", blut=1, dunkel_unten=0.2)
    jacke = stoff("dcd8cc", haut_f="b8c4c8", loecher=3, blut=2, falten=3, fransen=True)
    riemen = einfarbig("3a3028", 6)
    schlauch = einfarbig("c8e0d0", 4, alpha=200)
    p = {"e": farbe("6af0ff"), "x": farbe("3a2a2a"), "b": farbe("1a1a1a"), "m": farbe("2a0a0a"), "t": farbe("d8d0b8"),
         "n": farbe("7a2a2a")}
    m.k("koerper", drehpunkt=(0, 16, 0))
    for s in (1, -1):
        n = name_seite(s)
        b = m.k("bein_" + n, "koerper", drehpunkt=(s * 2, 16, 0))
        b.w((s * 2 - 1.25, 0, -1.25), (2.5, 16, 2.5), h)
        b.w((s * 2 - 1.5, 10, -1.5), (3, 6, 3), stoff("cfcabc", loecher=0, blut=1))
    ober = m.k("oberkoerper", "koerper", drehpunkt=(0, 16, 0), drehung=(-20, 0, 0))
    ober.w((-3.5, 16, -2), (7, 13, 4), kombi(h, rippen(0.4, "7a8a94")))
    ober.w((-4, 17, -2.5), (8, 10, 5), jacke)
    for y in (19, 23):
        ober.w((-4.3, y, -2.8), (8.6, 1, 5.6), riemen)
    for i, (x, rx, l) in enumerate(((-2, 30, 9), (0.5, 50, 12), (2.5, 20, 7))):
        sl = m.k(f"schlauch_{i}", "oberkoerper", drehpunkt=(x, 25, 2.5), drehung=(rx, 0, (i - 1) * 15))
        sl.w((x - 0.3, 25 - l, 2.2), (0.6, l, 0.6), schlauch)
    beutel = m.k("beutel", "schlauch_1", drehpunkt=(0.5, 13, 2.5))
    beutel.w((-0.5, 10, 1.5), (2.5, 3.5, 1.2), einfarbig("a8ffc0", 6, alpha=190))
    kopf = m.k("kopf", "oberkoerper", drehpunkt=(0, 29, -1), drehung=(25, 0, -18))
    kopf.w((-3, 29, -4), (6, 8, 6), h, deko=gesicht([
        "b.bb.b",
        "xxxxxx",
        "......",
        ".eneen"[:6],
        "......",
        ".mmmm.",
        ".mttm.",
    ], p, glow="e"))
    kopf.w((-3.2, 35.5, -3.2), (6.4, 0.6, 6.4), einfarbig("7a7a7a", 8))  # Klammernaht
    for s in (1, -1):
        n = name_seite(s)
        a = m.k("arm_" + n, "oberkoerper", drehpunkt=(s * 4.5, 27, 0), drehung=(10, 0, s * 8))
        x0 = 3.5 if s > 0 else -5.5
        a.w((x0, 10, -1), (2, 17, 2), h)
        a.w((x0 - 0.5, 20, -1.5), (3, 7, 3), jacke)
        a.w((x0 - 0.2, 21, -1.8), (2.4, 1, 3.6), riemen)
        for i in range(3):
            a.w((x0 + i * 0.7 - 0.1, 5, -0.4 + (i % 2) * 0.5), (0.5, 5, 0.5), h)
    return m, "Versuchsobjekt", "Orbitalstation · Labor: Testperson aus Block A – rennt auf allen vieren, sieht im Dunkeln", \
        "2,6 Blöcke"


def konglomerat():
    """Verschmolzene Besatzung: schwebende Fleischkugel aus mehreren Menschen, Arme greifen nach allem."""
    m = Modell("konglomerat", 256, 128)
    fl = kombi(fleisch("8a3a32", "b85a48"), lambda mm, r, f, w: mm.flecken(r, farbe("c8b8a8"), skala=2.5, schwelle=0.7))
    uniform = stoff("3a4a6a", loecher=1, blut=2, falten=3)
    h = haut("b89a8a", flecken_f="8a6a5a", blut=1)
    k = m.k("masse", drehpunkt=(0, 24, 0))
    k.w((-8, 16, -8), (16, 16, 16), fl)
    k.w((-9.5, 19, -6), (19, 10, 12), fl)
    k.w((-6, 14.5, -6), (12, 19, 12), fl)
    k.w((-11, 22, -3), (4, 7, 7), fl)  # Wucherungen brechen die Kugelform
    k.w((7.5, 15, -2), (4, 6, 6), fl)
    k.w((-4, 31.5, -5), (7, 4, 8), fl)
    k.w((2, 12.5, 1), (6, 3, 6), fl)
    k.w((-8.6, 17, -8.8), (7, 8, 5), uniform)
    k.w((2.5, 26, -9), (6, 6, 4), uniform)
    k.w((5, 18, 7.5), (5, 9, 2), uniform)
    kleiner_kopf(k, -4, 24, -10.5, 6, h)
    kleiner_kopf(k, 3.5, 17.5, -10, 5, h, offen=False)
    kleiner_kopf(k, -11.5, 18, -1, 5, h)
    kleiner_kopf(k, 1, 33.5, -2, 5, h)
    for i, (x, y, z, rx, rz, l) in enumerate(((-8, 20, -4, -60, 40, 9), (8, 22, -3, -70, -50, 10), (6, 16, 4, 10, -120, 8),
                                              (-6, 17, 5, 30, 130, 9), (0, 31, -4, -30, 10, 8), (2, 17, -7, -100, -10, 7))):
        a = m.k(f"arm_{i}", "masse", drehpunkt=(x, y, z), drehung=(rx, 0, rz))
        a.w((x - 1.25, y, z - 1.25), (2.5, l, 2.5), uniform if i % 2 else h)
        hd = m.k(f"hand_{i}", f"arm_{i}", drehpunkt=(x, y + l, z), drehung=(25, 0, 0))
        hd.w((x - 1.5, y + l, z - 1), (3, 3, 2), h)
        for f_ in range(3):
            hd.w((x - 1.4 + f_ * 1.1, y + l + 3, z - 0.6), (0.7, 2.5, 0.7), h)
    return m, "Konglomerat", "Orbitalstation · Masse: verschmolzene Besatzung, schwebt durch die Module und greift mit sechs Armen", \
        "2,5 Blöcke"


def kryoschlaefer():
    """Aus dem Kälteschlaf erwacht: blaugefrorene Haut, Eiskristalle, Splitter der Kapsel stecken noch im Rücken."""
    m = Modell("kryoschlaefer", 128, 128)
    h = haut("9ab4c8", flecken_f="6a84a0", adern_f="3a4a7a", blut=0, dunkel_unten=0.2)
    anzug = stoff("8ab8d0", haut_f="9ab4c8", loecher=2, blut=0, falten=3, schmutz="4a6a80")
    eis = kristall("e4f8ff", "ffffff", 225, glow=True)
    p = {"e": farbe("e0ffff"), "E": farbe("6ad8ff"), "s": farbe("3a4a5a"), "m": farbe("1a2a3a"), "t": farbe("e8f4ff")}
    gesicht_ = ["sssssss.", "s......s", ".eE..Ee.", "........", "..mmmm..", "..mttm..", "........", "........"]
    mm_ = _mensch("kryoschlaefer", "9ab4c8", gesicht_, p, anzug, anzug, einfarbig("5a6a7a", 6), glow="eE",
                  tb=128, th=128, arm_pose=((-70, 10, 6), (-50, -10, -6)))
    m = mm_
    kn = {k.name: k for k in m.knochen}
    kn["oberkoerper"].drehung = [-10, 0, 6]
    kn["kopf"].drehung = [20, 0, -15]
    for i, (x, y, z, l, rx, rz) in enumerate(((-4, 23, -1, 7, -10, 35), (-3, 23, 1, 5, 20, 55), (3.5, 23, -0.5, 8, 5, -30),
                                               (2.5, 22, 1.5, 5, 30, -60), (-1, 31.5, 0, 5, 15, 20), (1.5, 31.5, 1, 4, 25, -25),
                                               (0, 20, 2, 6, 60, 0))):
        el = "oberkoerper" if y < 30 else "kopf"
        sp = m.k(f"eis_{i}", el, drehpunkt=(x, y, z), drehung=(rx, 0, rz))
        sp.w((x - 1, y, z - 1), (2, l, 2), eis)
    kn["kopf"].w((-4.3, 30.5, -4.3), (8.6, 1.2, 8.6), eis)  # Reif auf dem Kopf
    split = m.k("kapselsplitter", "oberkoerper", drehpunkt=(0, 20, 2), drehung=(-20, 0, 25))
    split.w((-3, 15, 2), (6, 10, 0.8), kristall("bfe8f4", "ffffff", 170, glow=False))
    split.w((1, 20, 2.2), (1, 7, 0.6), metall("7a8088", "4a4a4a", nieten=False))
    kn["arm_rechts"].w((4, 10, -1.5), (3, 2, 3), eis)
    kn["arm_links"].w((-7, 10, -1.5), (3, 2, 3), eis)
    atem = m.k("atem", "kopf", drehpunkt=(0, 26, -5))
    atem.w((-2, 24.5, -8), (4, 3, 3), nebel("e0f4ff", 30, 140))
    return m, "Kryoschläfer", "Orbitalstation · Kälte: erwacht, wenn du an den Kapseln vorbeigehst – sein Griff lässt dich erstarren", \
        "1,9 Blöcke"


def leerenaal():
    """Schwimmt durch die schwarze Leere zwischen den Modulen: langer Leib, Leuchtpunkte, Rundmaul mit Zahnring."""
    m = Modell("leerenaal", 256, 128)
    h = kombi(haut("3a4660", flecken_f="2a3048", adern_f="5a6a9a", blut=0, dunkel_unten=0.0), leuchtpunkte("5affe8", 3))
    bauch = haut("8a8a9a", flecken_f="6a6a7a", blut=0)
    flosse = einfarbig("2a3a5a", 10, alpha=190)
    p = {"t": farbe("e8f4ff"), "m": farbe("0a0a10"), "r": farbe("5a1a3a"), "e": farbe("5affe8")}
    eltern = None
    z = -10
    groessen = [(13, 12, 9), (14, 13, 9), (13, 12, 9), (11, 10, 9), (9, 8, 9), (7, 7, 8), (5.5, 5.5, 8), (4, 4, 7)]
    for i, (gx, gy, gz) in enumerate(groessen):
        name = "kopf" if i == 0 else f"glied_{i}"
        dreh = (0, (-14 if i % 2 else 14) if i else 0, 0) if i else (8, 0, 0)
        kk = m.k(name, eltern, drehpunkt=(0, 16, z), drehung=dreh)
        kk.w((-gx / 2, 16 - gy / 2, z), (gx, gy, gz), h)
        kk.w((-gx / 2 + 1, 16 - gy / 2 - 0.4, z + 0.5), (gx - 2, 1, gz - 1), bauch)
        if 0 < i < 6:
            kk.w((-0.5, 16 + gy / 2, z + 1), (1, 5, gz - 2), flosse)  # Rückenflosse
        if i == 0:
            kk.w((-gx / 2, 16 - gy / 2, z - 1), (gx, gy, 1), h, deko=gesicht([
                "...ttttt...",
                ".ttmmmmmtt.",
                ".tmmtttmmt.",
                "tmmtmmmtmmt",
                "tmtmrrrmtmt",
                "tmtmrrrmtmt",
                "tmmtmmmtmmt",
                ".tmmtttmmt.",
                ".ttmmmmmtt.",
                "...ttttt...",
            ], p))
            kk.w((-gx / 2 - 0.5, 18, z + 2), (1, 1.5, 1.5), leuchtend("5affe8"))
            kk.w((gx / 2 - 0.5, 18, z + 2), (1, 1.5, 1.5), leuchtend("5affe8"))
            for s in (1, -1):
                fl = m.k("seitenflosse_" + name_seite(s), "kopf", drehpunkt=(s * gx / 2, 14, z + 6), drehung=(0, s * 30, s * -40))
                fl.w((s * gx / 2 - (0 if s > 0 else 10), 14, z + 3), (10, 0.8, 7), flosse)
            kk.w((-0.3, 20.5, z - 3), (0.6, 0.6, 5), einfarbig("1e2230", 4))  # Köder
            kk.w((-0.8, 20, z - 4.5), (1.6, 1.6, 1.6), leuchtend("aaffff", 6))
        eltern = name
        z += gz
    return m, "Leerenaal", "Orbitalstation · Leere: schwimmt durch das Nichts zwischen den Modulen – lockt mit seinem Licht", \
        "5 Blöcke lang"


def klammerer():
    """Kleiner Parasit: handförmiger Leib, acht Fingerbeine, langer Schwanz. Springt dir ins Gesicht."""
    m = Modell("klammerer", 64, 64)
    h = haut("d8b8a8", flecken_f="b08a7a", adern_f="8a3a4a", blut=0)
    k = m.k("koerper", drehpunkt=(0, 3, 0))
    k.w((-3, 2, -3.5), (6, 2.5, 7), h)
    k.w((-2, 4.5, -2.5), (4, 1, 5), h)
    k.w((-1, 1.4, -1), (2, 0.8, 2), leuchtend("ff4a7a", 4))  # Rüssel unten
    for i, z in enumerate((-3, -1, 1, 3)):
        for s in (1, -1):
            n = f"{name_seite(s)}_{i}"
            b = m.k("finger_" + n, "koerper", drehpunkt=(s * 3, 3, z), drehung=(0, s * (z * -8), s * -25))
            b.w((s * 3 - (0 if s > 0 else 4), 2.6, z - 0.4), (4, 0.8, 0.8), h)
            g = m.k("fingerspitze_" + n, "finger_" + n, drehpunkt=(s * 7, 3, z), drehung=(0, 0, s * 70))
            g.w((s * 7 - 0.4, -1, z - 0.4), (0.8, 4, 0.8), h)
    sw = m.k("schwanz", "koerper", drehpunkt=(0, 3, 3.5), drehung=(-20, 0, 0))
    sw.w((-0.6, 2.4, 3.5), (1.2, 1.2, 8), h)
    sw2 = m.k("schwanzspitze", "schwanz", drehpunkt=(0, 3, 11.5), drehung=(30, 0, 0))
    sw2.w((-0.4, 2.6, 11.5), (0.8, 0.8, 6), h)
    return m, "Klammerer", "Orbitalstation · Parasit: springt dir ins Gesicht – blind, bis du ihn abreißt (Schleichen + F)", \
        "0,4 Blöcke"


def hologeist():
    """Avatar der Stations-KI: flackerndes Hologramm. Nur verwundbar, solange sein Projektor steht."""
    m = Modell("hologeist", 128, 64)
    hl = holo()
    sockel = metall("3a3e44", "4a3a2a")
    p = {"e": farbe("ffffff")}
    proj = m.k("projektor", drehpunkt=(0, 0, 0))
    proj.w((-5, 0, -5), (10, 2, 10), sockel)
    proj.w((-2, 2, -2), (4, 1, 4), leuchtend("8ae8ff", 6))
    proj.w((-4, 3, -4), (8, 4, 8), holo("6ad8ff", 60))  # Lichtkegel
    m.k("koerper", drehpunkt=(0, 18, 0))
    for s in (1, -1):
        n = name_seite(s)
        b = m.k("bein_" + n, "koerper", drehpunkt=(s * 2, 18, 0))
        b.w((s * 2 - 1.75, 7, -1.75), (3.5, 11, 3.5), hl)
    ober = m.k("oberkoerper", "koerper", drehpunkt=(0, 18, 0))
    ober.w((-4, 18, -2), (8, 11, 4), hl)
    st = m.k("stoerung", "oberkoerper", drehpunkt=(0, 23, 0))
    st.w((-5.5, 22, -2.2), (8, 2, 4.4), hl)  # verrutschter Ausschnitt
    kopf = m.k("kopf", "oberkoerper", drehpunkt=(0, 29, 0))
    kopf.w((-3.5, 29, -3.5), (7, 7, 7), hl, deko=gesicht([".......", ".e...e.", ".......", "..eee.."], p, glow="e"))
    kopf.w((-1.5, 30.5, -4.5), (7, 1.5, 7), hl)  # Bildversatz
    for s in (1, -1):
        n = name_seite(s)
        a = m.k("arm_" + n, "oberkoerper", drehpunkt=(s * 5, 28, 0), drehung=(-15 if s > 0 else -80, 0, s * 8))
        x0 = 4 if s > 0 else -7
        a.w((x0, 17, -1.5), (3, 11, 3), hl)
    return m, "Hologeist", "Orbitalstation · KI: Avatar der Stations-KI – flackert, teleportiert, nur verwundbar mit Projektor", \
        "2,3 Blöcke"


def forscher():
    """Infizierter Wissenschaftler: Schädel offen, Neuro-Implantat blinkt, ein Arm ist eine Injektionspistole."""
    p = {"g": farbe("1a1a1a"), "G": farbe("8ad8ff"), "w": farbe("e0dcc8"), "e": farbe("3a1a1a"), "m": farbe("2a0a0a"),
         "t": farbe("c8bc9a"), "b": farbe("6a5a4a")}
    gesicht_ = ["........", "........", "gGGggGGg", ".we..ew.", "...bb...", "..mmmm..", "..mttm..", "........"]
    kittel = stoff("dcd8d0", haut_f="a89888", loecher=2, blut=3, falten=3, fransen=True)

    def extras(m, k):
        k["ober"].w((-4.5, 5, -2.6), (9, 19, 5.2), kittel)  # langer Kittel
        k["ober"].w((1.5, 20.5, -2.8), (2, 2, 0.4), kombi(einfarbig("2a3a6a", 4)))  # Ausweis
        k["kopf"].w((-4, 30.5, -4), (8, 1.5, 8), fleisch("9a4a4a", "d8a0a0"))  # offene Schädeldecke
        k["kopf"].w((-2.5, 31.5, -2.5), (5, 1.5, 5), fleisch("c87a7a", "e8b0b0"))  # Gehirn
        k["kopf"].w((-1, 32.5, -1), (2, 1, 2), kombi(metall("3a3e44", "4a3a2a", nieten=False), leuchtpunkte("4aff7a", 10)))
        for i, (x, l) in enumerate(((-1.5, 5), (1, 6))):
            kb = m.k(f"kabel_{i}", "kopf", drehpunkt=(x, 32, 3), drehung=(60 + i * 20, 0, (i - 0.5) * 30))
            kb.w((x - 0.3, 32, 3), (0.6, l, 0.6), einfarbig("2a2a2a", 4))
        kn = {kk.name: kk for kk in m.knochen}
        sp = m.k("injektor", "arm_rechts", drehpunkt=(5.5, 12, 0))
        sp.w((4, 7, -2), (3, 5, 4), metall("8a9096", "4a3a2a"))
        sp.w((4.5, 3, -1.5), (2, 4, 2), einfarbig("6aff9a", 6, alpha=200))
        sp.w((5.3, -1, -0.7), (0.4, 4, 0.4), einfarbig("e0e0e0", 4))
    m = _mensch("forscher", "a89888", gesicht_, p, kittel, stoff("3a3a44", loecher=1, blut=1), einfarbig("1e1e1e", 6),
                extras, arm_pose=((-50, 0, 6), (-5, 0, -10)))
    kn = {k.name: k for k in m.knochen}
    kn["oberkoerper"].drehung = [-12, 0, 0]
    kn["kopf"].drehung = [14, 0, 18]
    return m, "Forscher", "Orbitalstation · Labor: injiziert anderen Infizierten Stimulanzien – sie werden schneller und stärker", \
        "1,9 Blöcke"


# ===========================================================================
# DER ENDBOSS
# ===========================================================================

def der_ursprung():
    """Der Ursprung (A-0): gigantischer Organismus, um den Reaktorring der Station gewachsen. Ersetzt den Enderdrachen.
    Modell in Drittelgröße – im Spiel dreifach skaliert (etwa 15 Blöcke hoch, 20 breit mit Tentakeln)."""
    m = Modell("der_ursprung", 512, 512)

    def fleischig(basis, hell, adern=True):
        mats = [lambda mm, r, f, w: mm.fuellen(r, farbe(basis), streuung=10, skala=4, dunkel_unten=0.25),
                lambda mm, r, f, w: mm.flecken(r, farbe(hell), skala=4, schwelle=0.62, staerke=0.5)]
        if adern:
            def ad(mm, r, f, w):
                for _ in range(max(1, r[2] * r[3] // 90)):
                    x, y = r[0] + int(mm.rng.integers(0, r[2])), r[1] + int(mm.rng.integers(0, r[3]))
                    for _ in range(int(mm.rng.integers(5, 12))):
                        if r[0] <= x < r[0] + r[2] and r[1] <= y < r[1] + r[3]:
                            mm.setze(x, y, farbe("c060ff"), glow=True)
                        x += int(mm.rng.integers(-1, 2))
                        y += int(mm.rng.integers(0, 2))
            mats.append(ad)
        return kombi(*mats)

    fl = fleischig("3e1832", "5e2a4a")
    fl_dunkel = fleischig("2a1024", "3e1832", adern=False)
    haut_ = kombi(haut("5a4050", flecken_f="3a2834", blut=0, dunkel_unten=0.3))
    metall_ = metall("6a6e72", "7a3a1a")
    solar = kombi(einfarbig("1e2a5a", 8), lambda mm, r, f, w: mm.streifen(r, farbe("8a9ad0"), abstand=3, staerke=0.5),
                  lambda mm, r, f, w: mm.streifen(r, farbe("8a9ad0"), abstand=3, senkrecht=False, staerke=0.5))
    kern = leuchtend("f0c8ff", 16)
    kern2 = kristall("b06aff", "ffffff", 230)
    zahn = knochen("e0d8c0")
    p_auge = {"w": farbe("a89a80"), "W": farbe("8a5a50"), "i": farbe("ff6a1a"), "I": farbe("ffb04a"), "p": farbe("0a0a0a"),
              "r": farbe("5a1a2a")}
    leib = m.k("leib", drehpunkt=(0, 60, 0))
    leib.w((-13, 34, -9), (26, 18, 18), fl)  # Unterleib
    leib.w((-18, 50, -12), (36, 24, 24), fl)  # Brust
    leib.w((-20, 56, -9), (40, 16, 18), fl_dunkel)  # Schulterwulst
    # Brustfenster: Rippen vor dem Kristallkern A-7 (die Schwachstelle)
    leib.w((-7, 53, -13), (14, 14, 2), kern)
    for x in (-6.5, -3.2, 0.1, 3.4):
        leib.w((x, 51, -14.5), (2.4, 18, 2.4), zahn)
    for y in (52, 66):
        leib.w((-8, y, -14.2), (16, 2, 2), zahn)
    kk = m.k("kristallkern", "leib", drehpunkt=(0, 60, -14))
    kk.w((-3, 55, -16), (6, 10, 3), kern2)
    # Kopf: gewaltige Masse mit einem einzigen Riesenauge, gekrönt von Stationstrümmern
    kopf = m.k("kopf", "leib", drehpunkt=(0, 74, -2), drehung=(8, 0, 0))
    kopf.w((-14, 74, -12), (28, 18, 22), fl)
    kopf.w((-11, 92, -9), (22, 6, 17), fl_dunkel)
    kopf.w((-10, 76, -13.5), (20, 14, 2), haut_, deko=gesicht([
        "....rrrrrrrrrrrr....",
        "..rrWWWWWWWWWWWWrr..",
        ".rWwwwwwwwwwwwwwwWr.",
        "rWwwwwwiiiiiiwwwwwWr",
        "rWwwwwiIIIIIIiwwwwWr",
        "rWwwwiIIIppIIIiwwwWr",
        "rWwwwiIIppppIIiwwwWr",
        "rWwwwiIIppppIIiwwwWr",
        "rWwwwiIIIppIIIiwwwWr",
        "rWwwwwiIIIIIIiwwwwWr",
        "rWwwwwwiiiiiiwwwwwWr",
        ".rWwwwwwwwwwwwwwwWr.",
        "..rrWWWWWWWWWWWWrr..",
        "....rrrrrrrrrrrr....",
    ], p_auge, glow="iI"))
    for s_ in (1, -1):
        lid = m.k("lid_" + name_seite(s_), "kopf", drehpunkt=(0, 83, -13))
        lid.w((-11, 89.5 if s_ > 0 else 74.5, -14.5), (22, 3, 3), fl_dunkel)
    for i, (x, y) in enumerate(((-12, 78), (12, 80), (-12, 87), (12, 88), (0, 93))):
        kopf.w((x - 1.5, y - 1.5, -13 if abs(x) > 0 else -9.8), (3, 3, 1), leuchtend("ffb04a", 10))
    for i, (x, z, rz, rx, gr) in enumerate(((-8, -4, 25, -10, (10, 3, 8)), (6, 2, -20, 15, (12, 4, 6)), (0, -6, 5, -30, (6, 10, 4)))):
        tr = m.k(f"kronentruemmer_{i}", "kopf", drehpunkt=(x, 97, z), drehung=(rx, 0, rz))
        tr.w((x - gr[0] / 2, 96, z - gr[2] / 2), gr, metall_)
    for s_ in (1, -1):
        so = m.k("solarfluegel_" + name_seite(s_), "kopf", drehpunkt=(s_ * 13, 92, 2), drehung=(10, s_ * 15, s_ * -30))
        so.w((s_ * 13 - 1, 91, 1), (2, 2, 2), metall_)
        so.w((s_ * 14 - (0 if s_ > 0 else 26), 91.5, -4), (26, 1, 13), solar)
        so.w((s_ * 14 - (0 if s_ > 0 else 26), 91.2, 2), (26, 1.6, 1), metall_)
    # Maul: senkrecht gespaltener Kiefer im Unterleib
    for s_ in (1, -1):
        kf = m.k("kiefer_" + name_seite(s_), "leib", drehpunkt=(s_ * 1, 50, -9), drehung=(0, s_ * -30, 0))
        kf.w((s_ * 1 - (0 if s_ > 0 else 9), 34, -12), (9, 16, 4), haut_)
        for j in range(5):
            kf.w((s_ * 1 - (0 if s_ > 0 else 1.5) + (0 if s_ > 0 else 0), 35 + j * 3, -13.5), (1.5, 2.2, 2), zahn)
    leib.w((-1.5, 35, -10.5), (3, 14, 1), leuchtend("ff4a8a", 8))  # Schlund glimmt
    # Eingewachsene Panzerplatten
    leib.w((-19, 60, -6), (6, 12, 14), metall_)
    leib.w((12, 40, -4), (7, 8, 12), metall_)
    # Reaktorring hinter dem Leib (zerbrochen), leuchtende Spulen
    ring = m.k("reaktorring", "leib", drehpunkt=(0, 66, 18), drehung=(0, 0, 12))
    for i in range(16):
        if i in (4, 11):
            continue
        w = 2 * math.pi * i / 16
        x, y = math.cos(w) * 38, math.sin(w) * 38 + 66
        seg = m.k(f"ringsegment_{i}", "reaktorring", drehpunkt=(x, y, 18), drehung=(0, 0, math.degrees(w) + 90))
        seg.w((x - 8, y - 3, 15), (16, 6, 6), metall_)
        if i % 3 == 0:
            seg.w((x - 2.5, y - 3.5, 14.5), (5, 7, 1), leuchtend("b06aff", 12))
    # Gewaltige Arme mit Klauen
    for s_ in (1, -1):
        n = name_seite(s_)
        a = m.k("arm_" + n, "leib", drehpunkt=(s_ * 20, 68, -2), drehung=(-25, 0, s_ * 55))
        a.w((s_ * 20 - 5, 42, -7), (10, 28, 10), fl)
        a.w((s_ * 20 - 5.5, 56, -7.5), (11, 5, 11), metall_)  # eingewachsenes Stationsrohr
        u = m.k("unterarm_" + n, "arm_" + n, drehpunkt=(s_ * 20, 43, -2), drehung=(-35, 0, s_ * -40))
        u.w((s_ * 20 - 4, 18, -6), (8, 26, 8), haut_)
        kl = m.k("klaue_" + n, "unterarm_" + n, drehpunkt=(s_ * 20, 19, -2), drehung=(-20, 0, 0))
        for j, dx in enumerate((-3, 0, 3)):
            kl.w((s_ * 20 + dx - 1.1, 2, -4 - (j % 2) * 1.5), (2.2, 17, 2.2), zahn)
    # Acht Tentakel, fächern nach unten und außen auf, glühende Spitzen
    for i in range(8):
        wnk = 2 * math.pi * (i + 0.5) / 8
        x, z = math.cos(wnk) * 9, math.sin(wnk) * 7
        rz = -math.cos(wnk) * 35
        rx = math.sin(wnk) * 35
        eltern = "leib"
        y = 36
        dicke = 7.0
        for g in range(4):
            n = f"tentakel_{i}_{g}"
            kn_ = m.k(n, eltern, drehpunkt=(x, y, z), drehung=(rx if g == 0 else rx * 0.3, 0, rz if g == 0 else rz * 0.35))
            l = 15 - g * 2
            kn_.w((x - dicke / 2, y - l, z - dicke / 2), (dicke, l, dicke), fl if g < 2 else haut_)
            if g == 3:
                kn_.w((x - 1.5, y - l - 4, z - 1.5), (3, 4, 3), leuchtend("e08aff", 12))
            eltern = n
            y -= l
            dicke -= 1.4
    return m, "Der Ursprung", "Endboss · ersetzt den Enderdrachen: um den Reaktorring der Station gewachsen. Probe A-0", \
        "≈ 15 × 20 Blöcke"


# ---------------------------------------------------------------------------

ZONE = ["lenker", "spuk", "truegruede", "entreisser", "chimaere", "glasgaenger", "schwebender", "entlader", "echo",
        "zonenhirsch"]
ORBIT = ["leerlaeufer", "wartungsspinne", "wachkugel", "versuchsobjekt", "konglomerat", "kryoschlaefer", "leerenaal",
         "klammerer", "hologeist", "forscher"]
BOSS = ["der_ursprung"]
for n in ZONE + ORBIT + BOSS:
    E.ALLE[n] = globals()[n]

E.ANSICHTEN.update({
    "spuk": ((-30, 15), (60, 25), (200, 10)),
    "chimaere": ((-40, 12), (90, 5), (210, 20)),
    "zonenhirsch": ((-40, 8), (90, 4), (210, 15)),
    "truegruede": ((-40, 12), (90, 5), (210, 20)),
    "schwebender": ((-30, 25), (80, 10), (200, 30)),
    "wartungsspinne": ((-35, 30), (90, 10), (200, 40)),
    "wachkugel": ((-30, 10), (60, 5), (200, 15)),
    "konglomerat": ((-30, 10), (90, 5), (200, 15)),
    "leerenaal": ((-25, 12), (90, 5), (215, 25)),
    "klammerer": ((-30, 40), (90, 15), (200, 60)),
    "der_ursprung": ((-25, 8), (70, 4), (200, 12)),
})


def bauen(name):
    """Wie entwuerfe.bauen, vergrößert die Textur aber selbstständig, wenn der Platz nicht reicht."""
    modell, titel, unter, groesse = E.ALLE[name]()
    modell.vorwaerts_richten(E.NICHT_SPIEGELN.get(name, ()))
    while True:
        try:
            modell.uv_packen()
            break
        except ValueError:
            if modell.th < modell.tb:
                modell.th *= 2
            else:
                modell.tb *= 2
    maler = Maler(modell, saat=sum(map(ord, name)))
    maler.alles_malen()
    for ordner in ("geo/entity", "textures/entity"):
        os.makedirs(os.path.join(E.ASSETS, ordner), exist_ok=True)
    modell.speichern(os.path.join(E.ASSETS, "geo", "entity", f"{name}.geo.json"))
    maler.speichern(os.path.join(E.ASSETS, "textures", "entity", f"{name}.png"),
                    os.path.join(E.ASSETS, "textures", "entity", f"{name}_glowmask.png"))
    os.makedirs(E.VORSCHAU, exist_ok=True)
    extra = {"ansichten": E.ANSICHTEN[name]} if name in E.ANSICHTEN else {}
    b = blatt(modell, maler.bild, maler.glow, titel, unter, groesse_bloecke=groesse, **extra)
    pfad = os.path.join(E.VORSCHAU, f"{name}.png")
    b.save(pfad)
    return pfad


def uebersicht(namen, titel, pfad, spalten=2):
    """Mehrere Vorschaublätter zu einer Übersicht zusammenfügen (verkleinert)."""
    bilder = [Image.open(os.path.join(E.VORSCHAU, f"{n}.png")) for n in namen]
    f = 0.5
    bw, bh = int(bilder[0].width * f), int(bilder[0].height * f)
    zeilen = math.ceil(len(bilder) / spalten)
    blattbild = Image.new("RGB", (bw * spalten, bh * zeilen), (14, 16, 16))
    for i, b in enumerate(bilder):
        blattbild.paste(b.resize((bw, bh), Image.LANCZOS), ((i % spalten) * bw, (i // spalten) * bh))
    blattbild.save(pfad)
    return pfad


if __name__ == "__main__":
    for n in (sys.argv[1:] or ZONE + ORBIT + BOSS):
        print(bauen(n))
