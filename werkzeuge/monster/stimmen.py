"""Eigene Stimmen der Deadzone-Monster – komplett synthetisiert (Stimmquelle + Formantfilter + Rauheit).

  python3 werkzeuge/monster/stimmen.py

Schreibt assets/deadzonemc/sounds/monster/<art>/<klang>_<n>.ogg (mono) und gibt STIMMEN zurück, die
sounds.py statt der umgefärbten Minecraft-Klänge einträgt. Schritte, Graben, Ketten usw. bleiben Minecraft-Klänge.

Jede Art hat ein Profil: Grundton, Rauheit (Rauschen/Knurren), Formanten (Mundraum), Gurgeln, Hall.
Jeder Klang ist ein Archetyp (Stöhnen, Kreischen, Knurren, Gurgeln, Brüllen, Keuchen, Zirpen, Flüstern)
mit eigener Tonhöhenkurve.
"""
import os
import subprocess
import sys
import tempfile

import numpy as np
from scipy.io import wavfile
from scipy.signal import butter, fftconvolve, sosfilt

SR = 32000
WURZEL = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
ORDNER = os.path.join(WURZEL, "src", "main", "resources", "assets", "deadzonemc", "sounds", "monster")


def bp(x, a, b):
    a = max(30, a)
    b = min(SR / 2 - 100, b)
    return sosfilt(butter(2, [a, b], "band", fs=SR, output="sos"), x)


def tp(x, f):
    return sosfilt(butter(2, min(f, SR / 2 - 100), "low", fs=SR, output="sos"), x)


def hp(x, f):
    return sosfilt(butter(2, f, "high", fs=SR, output="sos"), x)


def huelle(n, an, ab):
    e = np.ones(n)
    a = min(n, int(an * SR))
    r = min(n - a, int(ab * SR))
    if a:
        e[:a] = np.linspace(0, 1, a) ** 1.5
    if r:
        e[n - r:] = np.linspace(1, 0, r) ** 1.5
    return e


def hall(x, dauer, rng, nass):
    if nass <= 0:
        return x
    n = int(dauer * SR)
    ir = tp(rng.normal(0, 1, n) * np.exp(-np.arange(n) / SR * (6.9 / dauer)), 3500)
    w = fftconvolve(x, ir)
    t = np.concatenate([x, np.zeros(len(w) - len(x))])
    w /= np.max(np.abs(w)) + 1e-9
    t /= np.max(np.abs(t)) + 1e-9
    return (1 - nass) * t + nass * w


class Profil:
    def __init__(self, f0, rau=0.4, fry=0.0, formanten=(600, 1200, 2500), gurgeln=0.0, hall=0.15, stimmen=1,
                 hell=6000):
        self.f0 = f0
        self.rau = rau
        self.fry = fry
        self.formanten = formanten
        self.gurgeln = gurgeln
        self.hall = hall
        self.stimmen = stimmen
        self.hell = hell


def stimme(p, dauer, kurve, rng, lautkurve=None, rau_extra=0.0, formant_faktor=1.0):
    """Stimmhafte Quelle mit Tonhöhenkurve (Faktor über die Zeit) durch den Mundraum des Profils."""
    n = int(dauer * SR)
    t = np.arange(n) / SR
    k = np.interp(t / dauer, np.linspace(0, 1, len(kurve)), kurve)
    gesamt = np.zeros(n)
    for v in range(p.stimmen):
        verstimmt = 1 + (v - (p.stimmen - 1) / 2) * 0.07
        f = p.f0 * k * verstimmt * (1 + 0.015 * np.sin(2 * np.pi * rng.uniform(4, 7) * t + rng.uniform(0, 6)))
        f *= 1 + 0.02 * tp(rng.normal(0, 1, n), 30) / 0.2  # Zittern
        ph = np.cumsum(2 * np.pi * np.clip(f, 20, 4000) / SR)
        q = 2 * (ph / (2 * np.pi) % 1) - 1  # Säge
        q += 0.5 * np.sign(np.sin(ph / 2)) * p.fry  # Unterton (Knurren)
        if p.fry:
            q *= 1 - p.fry * 0.6 * (0.5 + 0.5 * np.sign(np.sin(2 * np.pi * rng.uniform(25, 45) * t)))
        rau = np.clip(p.rau + rau_extra, 0, 1.5)
        q = q * (1 - min(rau, 1) * 0.3) + 0.35 * rau * tp(rng.normal(0, 1, n), 4000)
        roh = np.zeros(n)
        for i, fm in enumerate(p.formanten):
            fm = fm * formant_faktor
            if p.gurgeln:
                # Formanten wandern schnell: nasses, blubberndes Gurgeln (Überblenden zwischen drei Lagen)
                w = 0.5 + 0.5 * np.sin(2 * np.pi * rng.uniform(6, 14) * t + rng.uniform(0, 6))
                tief = bp(q, fm * 0.55, fm * 0.95)
                hoch = bp(q, fm * 1.05, fm * 1.6)
                y = (1 - p.gurgeln * w) * bp(q, fm * 0.75, fm * 1.3) + p.gurgeln * (w * tief + (1 - w) * hoch)
            else:
                y = bp(q, fm * 0.75, fm * 1.3)
            roh += y / (i + 1) ** 0.7
        gesamt += roh
    gesamt = np.tanh(gesamt * 1.6 / (np.max(np.abs(gesamt)) + 1e-9))
    if lautkurve is not None:
        gesamt *= np.interp(t / dauer, np.linspace(0, 1, len(lautkurve)), lautkurve)
    return tp(gesamt, p.hell)


# ---- Archetypen -------------------------------------------------------------------------------

def stoehnen(p, rng):
    d = rng.uniform(1.1, 1.9)
    k = [1.0, rng.uniform(1.05, 1.2), rng.uniform(0.9, 1.05), rng.uniform(0.75, 0.9)]
    return stimme(p, d, k, rng, [0, 1, 0.9, 0.6, 0]) * huelle(int(d * SR), 0.15, 0.4)


def schmerz(p, rng):
    d = rng.uniform(0.28, 0.45)
    k = [1.5, 1.9, 1.3]
    return stimme(p, d, k, rng, [0.3, 1, 0.4], rau_extra=0.2) * huelle(int(d * SR), 0.01, 0.15)


def tod(p, rng):
    d = rng.uniform(1.4, 2.0)
    k = [1.6, 1.2, 0.8, 0.5, 0.35]
    s = stimme(p, d, k, rng, [0.8, 1, 0.7, 0.4, 0], rau_extra=0.3)
    # letztes Röcheln
    n = int(d * SR)
    s += 0.35 * bp(rng.normal(0, 1, n), 300, 1800) * np.linspace(0, 1, n) ** 3 * huelle(n, 0.1, 0.3)
    return s * huelle(n, 0.02, 0.5)


def kreischen(p, rng):
    d = rng.uniform(1.4, 2.2)
    k = [1.0, 2.2, 2.6, 2.4, 1.6]
    return stimme(p, d, k, rng, [0, 1, 1, 0.8, 0], rau_extra=0.15, formant_faktor=1.3) * huelle(int(d * SR), 0.05, 0.5)


def knurren(p, rng):
    d = rng.uniform(0.9, 1.5)
    k = [0.9, 1.0, 0.95, 1.05, 0.9]
    alt = p.fry
    p.fry = max(alt, 0.6)
    s = stimme(p, d, k, rng, [0.2, 0.9, 1, 0.8, 0.3], rau_extra=0.25)
    p.fry = alt
    return s * huelle(int(d * SR), 0.1, 0.3)


def bruellen(p, rng):
    d = rng.uniform(2.0, 2.8)
    k = [0.8, 1.3, 1.45, 1.35, 1.0, 0.7]
    alt = p.fry
    p.fry = max(alt, 0.5)
    s = stimme(p, d, k, rng, [0, 0.8, 1, 1, 0.7, 0], rau_extra=0.35)
    p.fry = alt
    n = len(s)
    s += 0.25 * tp(rng.normal(0, 1, n), 400) * huelle(n, 0.3, 1.0)  # Brustkorb
    return s * huelle(n, 0.08, 0.8)


def keuchen(p, rng):
    """Atmen: zwei, drei raue Züge ein und aus."""
    d = rng.uniform(1.6, 2.4)
    n = int(d * SR)
    t = np.arange(n) / SR
    zuege = np.clip(np.sin(2 * np.pi * rng.uniform(1.2, 1.8) * t), 0, 1) ** 1.5
    s = bp(rng.normal(0, 1, n), p.formanten[0] * 0.8, p.formanten[-1] * 1.2) * zuege
    s += 0.3 * stimme(p, d, [0.6, 0.6], rng) * zuege
    return s * huelle(n, 0.1, 0.3)


def gurgeln(p, rng):
    alt = p.gurgeln
    p.gurgeln = max(alt, 0.9)
    d = rng.uniform(1.0, 1.6)
    s = stimme(p, d, [1.0, 0.9, 1.1, 0.85], rng, [0, 1, 0.8, 0.9, 0], rau_extra=0.3)
    p.gurgeln = alt
    n = len(s)
    blasen = np.zeros(n)
    for _ in range(int(d * 14)):
        i = rng.integers(0, n - 800)
        m = 600
        tt = np.arange(m) / SR
        f = rng.uniform(250, 700)
        blasen[i:i + m] += np.sin(2 * np.pi * f * tt * (1 + 3 * tt)) * np.exp(-tt * 60)
    return (s + 0.35 * blasen) * huelle(n, 0.05, 0.3)


def zirpen(p, rng):
    """Viele kleine Klick- und Zirplaute (Schwarm, Brut)."""
    d = rng.uniform(0.9, 1.4)
    n = int(d * SR)
    s = np.zeros(n)
    for _ in range(int(d * 40)):
        i = rng.integers(0, n - 2000)
        m = rng.integers(300, 1500)
        tt = np.arange(m) / SR
        f = p.f0 * rng.uniform(0.8, 1.6)
        s[i:i + m] += np.sign(np.sin(2 * np.pi * f * tt)) * np.exp(-tt * rng.uniform(40, 120)) * rng.uniform(0.3, 1)
    return bp(s, 800, 7000) * huelle(n, 0.05, 0.2)


def fluestern(p, rng):
    d = rng.uniform(1.6, 2.6)
    n = int(d * SR)
    t = np.arange(n) / SR
    silben = np.clip(np.sin(2 * np.pi * rng.uniform(3.5, 5.5) * t + rng.uniform(0, 6)), 0, 1) ** 2
    s = bp(rng.normal(0, 1, n), 1500, 6500) * silben
    return s * huelle(n, 0.2, 0.5)


def husten(p, rng):
    d = 1.4
    n = int(d * SR)
    s = np.zeros(n)
    for k in range(rng.integers(2, 4)):
        i = int((0.05 + k * rng.uniform(0.28, 0.36)) * SR)
        m = int(0.22 * SR)
        tt = np.arange(m) / SR
        stoss = bp(rng.normal(0, 1, m), 300, 2600) * np.exp(-tt * 14)
        stoss += 0.5 * stimme(p, m / SR, [1.2, 0.9], rng) * np.exp(-tt * 10)
        s[i:i + m] += stoss
    return s


def summen(p, rng):
    """Tiefes Brummen/Dröhnen (Nähe des Stillen, Strahlende)."""
    d = rng.uniform(2.0, 3.0)
    n = int(d * SR)
    t = np.arange(n) / SR
    s = np.sin(2 * np.pi * p.f0 * t) + 0.5 * np.sin(2 * np.pi * p.f0 * 2.01 * t) + 0.3 * np.sin(2 * np.pi * p.f0 * 3.03 * t)
    s *= 0.7 + 0.3 * np.sin(2 * np.pi * rng.uniform(0.5, 2) * t)
    s += 0.25 * bp(rng.normal(0, 1, n), 2000, 6000) * (0.5 + 0.5 * np.sin(2 * np.pi * 7 * t))
    return s * huelle(n, 0.5, 0.8)


def platzen(p, rng):
    d = 1.0
    n = int(d * SR)
    t = np.arange(n) / SR
    s = tp(rng.normal(0, 1, n), 1400) * np.exp(-t * 7) + 0.8 * np.sin(2 * np.pi * 55 * t) * np.exp(-t * 9)
    s += 0.5 * gurgeln(p, rng)[:n] * np.linspace(1, 0, n)
    return s


def herzschlag(p, rng):
    """Zwei tiefe, dumpfe Schläge – das Herz der Zone."""
    d = 1.6
    n = int(d * SR)
    s = np.zeros(n)
    for k, st in ((0.05, 1.0), (0.38, 0.7)):
        i = int(k * SR)
        m = int(0.5 * SR)
        tt = np.arange(m) / SR
        schlag = np.sin(2 * np.pi * (32 + 40 * np.exp(-tt * 25)) * tt) * np.exp(-tt * 7)
        schlag += 0.3 * tp(rng.normal(0, 1, m), 300) * np.exp(-tt * 20)
        s[i:i + m] += schlag * st
    return s


ARCHETYPEN = {"herzschlag": herzschlag, "stoehnen": stoehnen, "schmerz": schmerz, "tod": tod, "kreischen": kreischen, "knurren": knurren,
              "bruellen": bruellen, "keuchen": keuchen, "gurgeln": gurgeln, "zirpen": zirpen, "fluestern": fluestern,
              "husten": husten, "summen": summen, "platzen": platzen}

# Art -> (Profil, {Klangname: (Archetyp, Varianten)})
P = Profil
STANDARD = {"ambient": ("stoehnen", 3), "hurt": ("schmerz", 3), "death": ("tod", 2)}

ARTEN = {
    "wanderer": (P(105, rau=0.55, fry=0.35, formanten=(520, 1050, 2300)), STANDARD),
    "schreier": (P(320, rau=0.35, formanten=(950, 1700, 3100), hall=0.2),
                 {"ambient": ("keuchen", 3), "hurt": ("schmerz", 2), "death": ("kreischen", 1), "schrei": ("kreischen", 4)}),
    "lauerer": (P(85, rau=0.9, formanten=(400, 900, 2000), hall=0.25),
                {"hurt": ("schmerz", 2), "death": ("tod", 1), "atmen": ("keuchen", 3), "schreck": ("kreischen", 3)}),
    "kriecher": (P(170, rau=0.5, fry=0.2, formanten=(650, 1400, 2900), gurgeln=0.6), STANDARD),
    "der_stille": (P(42, rau=0.2, formanten=(300, 700, 1800), hall=0.5),
                   {"hurt": ("fluestern", 2), "death": ("fluestern", 1), "naehe": ("summen", 3)}),
    "imitator": (P(150, rau=0.25, formanten=(700, 1250, 2600), hall=0.1),
                 {"hurt": ("schmerz", 3), "death": ("tod", 2)}),
    "nebelwesen": (P(240, rau=0.15, formanten=(800, 1500, 2800), hall=0.6, stimmen=2),
                   {"ambient": ("stoehnen", 3), "hurt": ("schmerz", 2), "death": ("kreischen", 1)}),
    "schwarm": (P(900, rau=0.3, formanten=(1500, 3000, 5000)),
                {"ambient": ("zirpen", 3), "hurt": ("zirpen", 2), "death": ("zirpen", 1)}),
    "brut": (P(140, rau=0.6, formanten=(450, 1000, 2200), gurgeln=0.9),
             {"ambient": ("gurgeln", 3), "hurt": ("schmerz", 2), "death": ("tod", 1), "platzen": ("platzen", 2)}),
    "juggernaut": (P(58, rau=0.5, fry=0.7, formanten=(380, 800, 1900), hall=0.2),
                   {"ambient": ("knurren", 3), "hurt": ("schmerz", 2), "death": ("tod", 1), "brullen": ("bruellen", 2)}),
    "gefallener_soldat": (P(115, rau=0.7, fry=0.2, formanten=(560, 1150, 2500)), STANDARD),
    "glutleib": (P(95, rau=0.85, formanten=(500, 1100, 2600), hell=5000), STANDARD),
    "seuchenhund": (P(260, rau=0.75, fry=0.5, formanten=(700, 1500, 3200)),
                    {"ambient": ("knurren", 3), "hurt": ("schmerz", 2), "death": ("tod", 1), "heulen": ("kreischen", 2)}),
    "wuehler": (P(62, rau=0.6, fry=0.6, formanten=(350, 800, 1700), gurgeln=0.5), STANDARD),
    "sauger": (P(190, rau=0.6, formanten=(600, 1300, 2800), gurgeln=0.8),
               {"ambient": ("gurgeln", 3), "hurt": ("schmerz", 2), "death": ("tod", 1)}),
    "seuchentraeger": (P(110, rau=0.7, formanten=(500, 1100, 2400), gurgeln=0.4),
                       {"ambient": ("husten", 3), "hurt": ("schmerz", 2), "death": ("tod", 1)}),
    "ertrunkener": (P(100, rau=0.5, formanten=(480, 1000, 2200), gurgeln=1.0, hall=0.2),
                    {"ambient": ("gurgeln", 3), "hurt": ("schmerz", 2), "death": ("tod", 1)}),
    "scheintoter": (P(120, rau=0.8, formanten=(520, 1100, 2400)),
                    {"atmen": ("keuchen", 3), "hurt": ("schmerz", 2), "death": ("tod", 1), "packen": ("kreischen", 2)}),
    # Bosse
    "verschmolzener": (P(80, rau=0.55, fry=0.4, formanten=(450, 950, 2100), stimmen=3, hall=0.25),
                       {"ambient": ("stoehnen", 3), "hurt": ("schmerz", 2), "death": ("tod", 1), "brullen": ("bruellen", 2)}),
    "nebelmutter": (P(330, rau=0.2, formanten=(850, 1600, 3000), stimmen=2, hall=0.7),
                    {"ambient": ("stoehnen", 3), "hurt": ("schmerz", 2), "death": ("kreischen", 1), "brullen": ("kreischen", 2)}),
    "strahlende": (P(70, rau=0.35, formanten=(500, 1200, 2700), stimmen=2, hall=0.35),
                   {"ambient": ("summen", 3), "hurt": ("schmerz", 2), "death": ("tod", 1), "brullen": ("bruellen", 2)}),
    "brutkoenigin": (P(210, rau=0.6, fry=0.3, formanten=(700, 1500, 3400), gurgeln=0.6, stimmen=2),
                     {"ambient": ("zirpen", 3), "hurt": ("schmerz", 2), "death": ("kreischen", 1), "brullen": ("kreischen", 2)}),
    "schlaechter": (P(75, rau=0.6, fry=0.6, formanten=(420, 900, 2000)),
                    {"ambient": ("knurren", 3), "hurt": ("schmerz", 2)}),
    "hirte": (P(95, rau=0.45, formanten=(480, 1000, 2300), hall=0.5, stimmen=2),
              {"ambient": ("stoehnen", 3), "hurt": ("schmerz", 2), "death": ("tod", 1)}),
    "schlund": (P(38, rau=0.5, fry=0.8, formanten=(250, 600, 1400), hall=0.3),
                {"ambient": ("knurren", 3), "hurt": ("schmerz", 2), "death": ("tod", 1), "brullen": ("bruellen", 3)}),
    "pilzvater": (P(55, rau=0.6, formanten=(350, 750, 1700), gurgeln=0.7, hall=0.3),
                  {"ambient": ("gurgeln", 3), "hurt": ("schmerz", 2), "death": ("tod", 1)}),
    # Herz der Zone
    "lenker": (P(70, rau=0.3, formanten=(400, 900, 2000), stimmen=3, hall=0.5), STANDARD),
    "spuk": (P(500, rau=0.6, formanten=(1500, 3000, 5000), hall=0.6), {"ambient": ("fluestern", 3), "hurt": ("schmerz", 2), "death": ("kreischen", 1)}),
    "truegruede": (P(240, rau=0.7, fry=0.5, formanten=(700, 1500, 3100)), {"ambient": ("knurren", 3), "hurt": ("schmerz", 2), "death": ("tod", 1)}),
    "entreisser": (P(130, rau=0.4, formanten=(600, 1300, 2700), hall=0.3), STANDARD),
    "chimaere": (P(150, rau=0.7, fry=0.6, formanten=(550, 1200, 2600), stimmen=2), {"ambient": ("knurren", 3), "hurt": ("schmerz", 2), "death": ("tod", 1)}),
    "glasgaenger": (P(420, rau=0.2, formanten=(1800, 3200, 5200), hall=0.5), {"ambient": ("summen", 3), "hurt": ("zirpen", 2), "death": ("kreischen", 1)}),
    "schwebender": (P(90, rau=0.3, formanten=(450, 1000, 2200), hall=0.6, stimmen=2), STANDARD),
    "entlader": (P(110, rau=0.5, formanten=(600, 1400, 3000)), {"ambient": ("summen", 3), "hurt": ("schmerz", 2), "death": ("tod", 1)}),
    "echo": (P(125, rau=0.25, formanten=(650, 1250, 2600), hall=0.7, stimmen=2), STANDARD),
    "zonenhirsch": (P(300, rau=0.4, formanten=(800, 1600, 3200), hall=0.3), {"ambient": ("kreischen", 2), "hurt": ("schmerz", 2), "death": ("tod", 1)}),
    # Orbitalstation
    "leerlaeufer": (P(95, rau=0.6, formanten=(500, 1100, 2400), hall=0.4), {"ambient": ("keuchen", 3), "hurt": ("schmerz", 2), "death": ("tod", 1)}),
    "wartungsspinne": (P(700, rau=0.3, formanten=(1400, 2800, 5000)), {"ambient": ("zirpen", 3), "hurt": ("zirpen", 2), "death": ("zirpen", 1)}),
    "wachkugel": (P(220, rau=0.1, formanten=(900, 2000, 4000)), {"ambient": ("summen", 3), "hurt": ("zirpen", 2), "death": ("summen", 1)}),
    "versuchsobjekt": (P(160, rau=0.7, fry=0.4, formanten=(600, 1300, 2800)), {"ambient": ("knurren", 3), "hurt": ("schmerz", 2), "death": ("kreischen", 1)}),
    "konglomerat": (P(80, rau=0.5, fry=0.4, formanten=(450, 950, 2100), stimmen=4, hall=0.3), STANDARD),
    "kryoschlaefer": (P(110, rau=0.5, formanten=(800, 1600, 3200), hall=0.5), {"ambient": ("keuchen", 3), "hurt": ("schmerz", 2), "death": ("tod", 1)}),
    "leerenaal": (P(60, rau=0.3, formanten=(300, 700, 1600), hall=0.7), {"ambient": ("summen", 3), "hurt": ("schmerz", 2), "death": ("tod", 1)}),
    "klammerer": (P(900, rau=0.6, formanten=(1600, 3200, 5200)), {"ambient": ("zirpen", 3), "hurt": ("zirpen", 2), "death": ("kreischen", 1)}),
    "hologeist": (P(180, rau=0.2, formanten=(900, 1800, 3600), hall=0.6, stimmen=3), {"ambient": ("fluestern", 3), "hurt": ("zirpen", 2), "death": ("summen", 1)}),
    "forscher": (P(135, rau=0.5, formanten=(600, 1250, 2600)), {"ambient": ("husten", 3), "hurt": ("schmerz", 2), "death": ("tod", 1)}),
    "der_ursprung": (P(44, rau=0.4, fry=0.6, formanten=(300, 700, 1700), stimmen=4, hall=0.6),
                     {"ambient": ("summen", 3), "hurt": ("schmerz", 2), "death": ("tod", 1), "brullen": ("bruellen", 3),
                      "puls": ("herzschlag", 2)}),
    "das_herz": (P(36, rau=0.55, fry=0.8, formanten=(260, 620, 1500), stimmen=3, hall=0.5),
                 {"ambient": ("knurren", 3), "hurt": ("schmerz", 2), "death": ("tod", 1), "brullen": ("bruellen", 3),
                  "herzschlag": ("herzschlag", 2)}),
    "erfrorene": (P(280, rau=0.5, formanten=(900, 1800, 3500), hall=0.55, stimmen=2),
                  {"ambient": ("stoehnen", 3), "hurt": ("schmerz", 2), "death": ("kreischen", 1), "brullen": ("kreischen", 2)}),
}


def speichern(pfad, x, rng, nass):
    x = hall(x, 1.4, rng, nass)
    x = hp(x, 35)
    x = x / (np.max(np.abs(x)) + 1e-9) * 0.85
    x *= huelle(len(x), 0.005, 0.08)
    os.makedirs(os.path.dirname(pfad), exist_ok=True)
    with tempfile.TemporaryDirectory() as tmp:
        wav = os.path.join(tmp, "x.wav")
        wavfile.write(wav, SR, (x * 32767).astype(np.int16))
        subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", wav, "-ac", "1", "-c:a", "libvorbis", "-q:a", "2", pfad],
                       check=True)


def eintraege():
    """Für sounds.py: Klangname -> Liste der Dateien (deadzonemc:monster/<art>/<klang>_<n>)."""
    aus = {}
    for art, (_, klaenge) in ARTEN.items():
        for name, (_, anzahl) in klaenge.items():
            aus[f"{art}.{name}"] = [{"name": f"deadzonemc:monster/{art}/{name}_{i}"} for i in range(1, anzahl + 1)]
    return aus


def main(nur=None):
    for art, (profil, klaenge) in ARTEN.items():
        if nur and art not in nur:
            continue
        for name, (arch, anzahl) in klaenge.items():
            for i in range(1, anzahl + 1):
                rng = np.random.default_rng(sum(map(ord, art + name)) * 131 + i)
                x = ARCHETYPEN[arch](profil, rng)
                speichern(os.path.join(ORDNER, art, f"{name}_{i}.ogg"), x, rng, profil.hall)
        print(art, len(klaenge))


if __name__ == "__main__":
    main(sys.argv[1:])
