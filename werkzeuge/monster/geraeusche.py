"""Geräusche der Deadzone-Monster – synthetisiert, für jede Art passend zu dem, was sie ist.

  python3 werkzeuge/monster/geraeusche.py [art ...]

Rückmeldung 22.10.: „jedem Mob neue Geräusche geben, die passender zum Thema sind“. Statt eines einzigen
Stimmmodells für alle bekommt jede Art eine eigene Mischung:

  * Stimmen aus einem Stimmlippen-Modell (Glottispulse mit Zittern, Hauch, Knarren) durch echte Vokal-Formanten,
    die sich während des Lauts bewegen (u → o → a …) – Stöhnen, Schreien, Knurren, Bellen, Heulen.
  * Geräusche aus der Welt der Art: Funkrauschen und klappernde Ausrüstung beim Soldaten, Gasmaske, Ventil und
    Geigerzähler beim Seuchenträger, Blasen beim Ertrunkenen, Knistern beim Glutleib, Rotor, Servos und Sirene
    bei der Drohne, Flügelschlag bei der Motte, Holzknarren beim Tarnfresser, Porzellan beim Stillen,
    Chitinklicken beim Kriecher, Rattenfiepen beim Schwarm, Metall beim Juggernaut …

Schreibt assets/deadzonemc/sounds/monster/<art>/<klang>_<n>.ogg (mono). sounds.py trägt sie ein; danach
gleicht werkzeuge/lautheit.py die Lautstärke an.
"""
import os
import shutil
import subprocess
import sys
import tempfile

import numpy as np
from scipy.io import wavfile
from scipy.signal import butter, fftconvolve, lfilter, sosfilt

SR = 32000
WURZEL = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
ORDNER = os.path.join(WURZEL, "src", "main", "resources", "assets", "deadzonemc", "sounds", "monster")


# ==== Grundbausteine ==============================================================================

def n_(d):
    return max(1, int(d * SR))


def zeit(n):
    return np.arange(n) / SR


def bp(x, a, b, ordnung=2):
    a = max(25, a)
    b = min(SR / 2 - 200, b)
    if b <= a * 1.05:
        b = a * 1.05
    return sosfilt(butter(ordnung, [a, b], "band", fs=SR, output="sos"), x)


def tp(x, f, ordnung=2):
    return sosfilt(butter(ordnung, min(f, SR / 2 - 200), "low", fs=SR, output="sos"), x)


def hp(x, f, ordnung=2):
    return sosfilt(butter(ordnung, max(20, f), "high", fs=SR, output="sos"), x)


def huelle(n, an, ab, form=1.5):
    e = np.ones(n)
    a = min(n, int(an * SR))
    r = min(n - a, int(ab * SR))
    if a:
        e[:a] = np.linspace(0, 1, a) ** form
    if r:
        e[n - r:] = np.linspace(1, 0, r) ** form
    return e


def kurve(werte, n):
    """Stützwerte gleichmäßig über die Dauer verteilt → Kurve mit n Werten."""
    return np.interp(np.linspace(0, 1, n), np.linspace(0, 1, len(werte)), werte)


def glatt_rauschen(rng, n, f):
    """Langsam schwankendes Rauschen (Tiefpass), Spitze etwa ±1."""
    x = tp(rng.normal(0, 1, n), f)
    return x / (np.max(np.abs(x)) + 1e-9)


def norm(x):
    return x / (np.max(np.abs(x)) + 1e-9)


def mischen(*teile):
    """Summe von (Signal, Pegel, Versatz in s); Länge = längstes Teil."""
    laenge = max(int(v * SR) + len(s) for s, _, v in teile)
    y = np.zeros(laenge)
    for s, pegel, versatz in teile:
        i = int(versatz * SR)
        y[i:i + len(s)] += pegel * s
    return y


def raum(x, dauer, nass, rng, hell=3500):
    if nass <= 0:
        return x
    n = n_(dauer)
    ir = tp(rng.normal(0, 1, n) * np.exp(-zeit(n) * (6.9 / dauer)), hell)
    w = fftconvolve(x, ir)
    t = np.concatenate([x, np.zeros(len(w) - len(x))])
    return (1 - nass) * norm(t) + nass * norm(w)


def saettigen(x, kraft):
    return np.tanh(x * kraft) / np.tanh(kraft)


# ==== Stimme ======================================================================================

VOKALE = {  # F1..F4 (Hz) und Bandbreiten, männliche Grundlage
    "a": ((730, 1090, 2440, 3400), (90, 110, 160, 200)),
    "o": ((570, 840, 2410, 3300), (80, 100, 150, 200)),
    "u": ((300, 870, 2240, 3300), (70, 100, 150, 200)),
    "e": ((530, 1840, 2480, 3500), (80, 120, 160, 200)),
    "i": ((270, 2290, 3010, 3700), (60, 120, 170, 220)),
    "ae": ((660, 1720, 2410, 3400), (100, 120, 160, 200)),
    "ə": ((500, 1500, 2500, 3500), (90, 120, 160, 200)),
    "ng": ((250, 1000, 2200, 3200), (100, 200, 300, 300)),
}


def glottis(f0, rng, jitter=0.01, shimmer=0.1, fry=0.0, offen=0.6):
    """Glottispuls-Folge (abgeleiteter Rosenberg-Puls) für eine Tonhöhenkurve f0 (Hz je Sample)."""
    n = len(f0)
    f = f0 * (1 + jitter * glatt_rauschen(rng, n, 40))
    ph = np.cumsum(np.clip(f, 15, 3000) / SR)
    p = ph % 1.0
    tp_ = offen * 0.66
    tn = offen * 0.34
    g = np.where(p < tp_, 0.5 * (1 - np.cos(np.pi * p / tp_)),
                 np.where(p < tp_ + tn, np.cos(0.5 * np.pi * (p - tp_) / tn), 0.0))
    d = np.diff(g, prepend=0) * SR / (np.maximum(f, 20))
    zyklus = np.floor(ph).astype(int)
    if shimmer:
        amp = 1 + shimmer * rng.normal(0, 1, zyklus.max() + 2)
        d *= amp[zyklus]
    if fry:
        # Knarren: jeder zweite Zyklus schwächer und verspätet wirkend (Periodenverdopplung)
        d *= 1 - fry * (zyklus % 2)
        d *= 1 - 0.5 * fry * (zyklus % 3 == 0)
    return d, p


def formanten(quelle, vokale, rng, groesse=1.0, verstell=None, pegel=(1.0, 0.55, 0.3, 0.18), block=192):
    """Quelle durch vier Formant-Resonatoren; vokale = Folge von Vokalen über die Dauer (wird überblendet).
    groesse < 1 = größerer Rachen (tiefer), > 1 = kleiner (höher)."""
    n = len(quelle)
    werte = np.array([VOKALE[v][0] for v in vokale], dtype=float)
    bandb = np.array([VOKALE[v][1] for v in vokale], dtype=float)
    pos = np.linspace(0, 1, len(vokale))
    aus = np.zeros(n)
    zust = [np.zeros(2) for _ in range(4)]
    for start in range(0, n, block):
        ende = min(n, start + block)
        u = start / max(1, n - 1)
        for k in range(4):
            fk = np.interp(u, pos, werte[:, k]) * groesse
            if verstell is not None:
                fk *= verstell[start]
            bw = np.interp(u, pos, bandb[:, k]) * (0.8 + 0.4 * groesse)
            fk = min(fk, SR / 2 - 500)
            r = np.exp(-np.pi * bw / SR)
            th = 2 * np.pi * fk / SR
            a = [1, -2 * r * np.cos(th), r * r]
            b = [(1 - r * r) * np.sin(th)]
            y, zust[k] = lfilter(b, a, quelle[start:ende], zi=zust[k])
            aus[start:ende] += pegel[k] * y
    return aus


def stimme(rng, dauer, f0_kurve, vokale, groesse=1.0, hauch=0.25, jitter=0.012, shimmer=0.12, fry=0.0,
           knurr=0.0, rauh=0.0, laut_kurve=None, stimmen=1, verstimmung=0.03, offen=0.6):
    """Ein stimmhafter Laut. f0_kurve: Stützwerte in Hz. knurr: Rachen-Rasseln (AM 25–55 Hz + Geräusch)."""
    n = n_(dauer)
    t = zeit(n)
    gesamt = np.zeros(n)
    for v in range(stimmen):
        faktor = 1 + (v - (stimmen - 1) / 2) * verstimmung
        f0 = kurve(f0_kurve, n) * faktor * (1 + 0.012 * np.sin(2 * np.pi * rng.uniform(4.5, 6.5) * t + rng.uniform(0, 6)))
        q, phase = glottis(f0, rng, jitter, shimmer, fry, offen)
        # Hauch: Rauschen, im Takt der Stimmlippen moduliert
        rausch = rng.normal(0, 1, n)
        q = q * (1 - hauch * 0.5) + hauch * 0.6 * rausch * (0.4 + 0.6 * (phase < 0.6))
        if rauh:
            q += rauh * 0.5 * tp(rng.normal(0, 1, n), 2500)
        if knurr:
            am = 0.5 + 0.5 * np.sign(np.sin(2 * np.pi * rng.uniform(26, 50) * t + rng.uniform(0, 6)))
            q *= 1 - knurr * 0.7 * am
            q += knurr * 0.4 * bp(rng.normal(0, 1, n), 80, 600) * am
        gesamt += formanten(q, vokale, rng, groesse * rng.uniform(0.97, 1.03))
    gesamt = norm(gesamt)
    if laut_kurve is not None:
        gesamt *= kurve(laut_kurve, n)
    return gesamt


# ==== Geräusche aus der Welt =====================================================================

def klick(rng, dauer=0.01, hell=4000, resonanz=None):
    n = n_(dauer)
    x = rng.normal(0, 1, n) * np.exp(-zeit(n) * (5 / dauer))
    if resonanz:
        x = bp(x, resonanz * 0.7, resonanz * 1.4)
    return hp(x, hell * 0.3) if hell else x


def klickfolge(rng, dauer, rate, hell=(2000, 7000), laenge=(0.002, 0.012), unregelmaessig=0.6, pegel_streu=0.6):
    n = n_(dauer)
    y = np.zeros(n)
    t = 0.0
    while t < dauer:
        t += rng.exponential(1 / rate) if unregelmaessig else 1 / rate
        i = int(t * SR)
        if i >= n - 1:
            break
        k = klick(rng, rng.uniform(*laenge), 0, None)
        k = bp(k, *(lambda m: (m * 0.6, m * 1.6))(rng.uniform(*hell)))
        m = min(len(k), n - i)
        y[i:i + m] += k[:m] * (1 - pegel_streu * rng.random())
    return y


def blasen(rng, dauer, dichte=12, tief=180, hoch=900):
    n = n_(dauer)
    y = np.zeros(n)
    for _ in range(int(dauer * dichte)):
        m = n_(rng.uniform(0.02, 0.07))
        i = rng.integers(0, max(1, n - m))
        tt = zeit(m)
        f = rng.uniform(tief, hoch)
        y[i:i + m] += np.sin(2 * np.pi * f * tt * (1 + rng.uniform(2, 6) * tt)) * np.exp(-tt * rng.uniform(40, 90))
    return y


def knistern(rng, dauer, rate=60):
    """Feuer: Knacken und Prasseln über einem dumpfen Rauschen."""
    n = n_(dauer)
    funken = klickfolge(rng, dauer, rate, (1500, 6000), (0.001, 0.006))
    grund = tp(rng.normal(0, 1, n), 500) * (0.6 + 0.4 * glatt_rauschen(rng, n, 3))
    return 0.8 * funken + 0.5 * grund


def zischen(rng, dauer, druck=1.0):
    n = n_(dauer)
    x = bp(rng.normal(0, 1, n), 2500, 9000) * (0.7 + 0.3 * glatt_rauschen(rng, n, 12))
    return x * huelle(n, 0.02 / druck, 0.15)


def funkrauschen(rng, dauer):
    """Funkgerät: knackender Einsatz, Rauschen mit Aussetzern, kurzer Pfeifton, Rauschsperre zu."""
    n = n_(dauer)
    t = zeit(n)
    r = bp(rng.normal(0, 1, n), 400, 3200) * (0.55 + 0.45 * glatt_rauschen(rng, n, 30))
    aus = (glatt_rauschen(rng, n, 6) > -0.55).astype(float)
    r *= tp(aus, 60)
    r += 0.25 * np.sin(2 * np.pi * rng.uniform(900, 1400) * t) * (t < 0.08)
    r += klickfolge(rng, dauer, 30, (1500, 3500), (0.001, 0.004))
    k = np.zeros(n)
    k[:n_(0.004)] = 1
    k[-n_(0.006):] = -1
    return saettigen(r + k, 2.0) * huelle(n, 0.003, 0.02)


def funkstimme(rng, dauer, f0=120):
    """Verstümmelte Stimme aus dem Funkgerät (unverständlich)."""
    n = n_(dauer)
    silben = kurve(rng.uniform(0.2, 1, int(dauer * 7) + 2), n) * (np.sin(2 * np.pi * rng.uniform(4, 6) * zeit(n)) > -0.2)
    v = stimme(rng, dauer, [f0 * rng.uniform(0.9, 1.1) for _ in range(6)],
               [rng.choice(list("aeiou")) for _ in range(int(dauer * 6) + 2)], hauch=0.2)
    v = bp(v * silben, 500, 2800, 3)
    return saettigen(v * 2.5, 2.5)


def ausruestung(rng, dauer):
    """Klappernde Ausrüstung: Karabiner, Magazine, Helmriemen."""
    y = np.zeros(n_(dauer))
    for _ in range(rng.integers(3, 7)):
        i = rng.integers(0, max(1, len(y) - n_(0.15)))
        m = metall(rng, rng.uniform(0.05, 0.15), rng.uniform(1800, 4200), hart=0.5)
        y[i:i + len(m)] += m[:len(y) - i] * rng.uniform(0.3, 1)
    return y


def metall(rng, dauer, grund=600.0, hart=1.0, teile=7):
    """Modale Synthese: unharmonische, abklingende Teiltöne (Blech, Rüstung, Klinge)."""
    n = n_(dauer)
    t = zeit(n)
    y = np.zeros(n)
    for k in range(teile):
        f = grund * (1 + k * rng.uniform(0.9, 1.7)) * rng.uniform(0.97, 1.03)
        if f > SR / 2 - 500:
            break
        y += np.sin(2 * np.pi * f * t + rng.uniform(0, 6)) * np.exp(-t * rng.uniform(4, 18) * (1 + k * 0.4)) / (1 + k * 0.5)
    anschlag = np.zeros(n)
    m = min(n, n_(0.006))
    anschlag[:m] = rng.normal(0, 1, m) * np.exp(-zeit(m) * 900)
    return y + hart * 0.8 * hp(anschlag, 1500)


def schleifen(rng, dauer, grund=180):
    """Metall auf Metall / Stein: rauer Reibklang."""
    n = n_(dauer)
    t = zeit(n)
    rate = grund * (1 + 0.3 * glatt_rauschen(rng, n, 4))
    ph = np.cumsum(rate / SR)
    stoss = (np.diff(np.floor(ph), prepend=0) > 0).astype(float) * (0.5 + rng.random(n))
    y = bp(np.convolve(stoss, np.exp(-zeit(n_(0.01)) * 300), mode="same"), 400, 5000)
    y += 0.4 * bp(rng.normal(0, 1, n), 1500, 7000) * (0.6 + 0.4 * np.sin(2 * np.pi * 9 * t))
    return y


def holzknarren(rng, dauer):
    """Holz unter Spannung: Reibimpulse mit wechselnder Rate durch Holzresonanzen."""
    n = n_(dauer)
    rate = kurve(rng.uniform(25, 90, 6), n)
    ph = np.cumsum(rate / SR)
    stoss = (np.diff(np.floor(ph), prepend=0) > 0).astype(float) * (0.4 + rng.random(n))
    y = np.convolve(stoss, np.exp(-zeit(n_(0.008)) * 500), mode="same")
    y = bp(y, 250, 600) + 0.6 * bp(y, 900, 1600) + 0.3 * bp(y, 2200, 3200)
    return y * huelle(n, 0.1, 0.2)


def knacken(rng, staerke=1.0):
    """Ast / Knochen bricht: harter Anriss, kurzes Splittern."""
    d = 0.25
    n = n_(d)
    t = zeit(n)
    y = rng.normal(0, 1, n) * np.exp(-t * 60)
    y = bp(y, 600, 5000) + 0.7 * bp(rng.normal(0, 1, n) * np.exp(-t * 25), 200, 900)
    y += klickfolge(rng, d, 120, (1500, 5000), (0.001, 0.003)) * np.exp(-t * 15)
    return y * staerke


def rascheln(rng, dauer):
    n = n_(dauer)
    g = np.clip(glatt_rauschen(rng, n, 20), 0, 1) ** 1.5
    return bp(rng.normal(0, 1, n), 1800, 9000) * g + 0.3 * klickfolge(rng, dauer, 80, (3000, 8000), (0.001, 0.003)) * g


def schmatzen(rng, dauer, tief=200):
    """Nasses Fleisch: schmatzende, saugende Impulse."""
    n = n_(dauer)
    y = np.zeros(n)
    for _ in range(int(dauer * 9)):
        m = n_(rng.uniform(0.03, 0.09))
        i = rng.integers(0, max(1, n - m))
        tt = zeit(m)
        f = rng.uniform(tief, tief * 4)
        s = bp(rng.normal(0, 1, m), f, f * 2.5) * np.exp(-tt * rng.uniform(25, 60))
        s += 0.5 * np.sin(2 * np.pi * f * 0.5 * tt * (1 - 2 * tt)) * np.exp(-tt * 40)
        y[i:i + m] += s * rng.uniform(0.4, 1)
    return y + 0.3 * blasen(rng, dauer, 8, tief, tief * 3)


def saugen(rng, dauer):
    """Langer, nasser Sog mit Plopp am Ende."""
    n = n_(dauer)
    t = zeit(n)
    mitte = kurve([300, 900, 1600, 700], n)
    y = np.zeros(n)
    for s in range(0, n, 256):
        e = min(n, s + 256)
        y[s:e] = bp(rng.normal(0, 1, e - s), mitte[s] * 0.7, mitte[s] * 1.5)
    y *= huelle(n, 0.2, 0.1) * (0.6 + 0.4 * glatt_rauschen(rng, n, 15))
    plopp = np.sin(2 * np.pi * 180 * zeit(n_(0.08)) * (1 + 8 * zeit(n_(0.08)))) * np.exp(-zeit(n_(0.08)) * 50)
    return mischen((y, 1, 0), (0.7 * schmatzen(rng, dauer * 0.8), 0.6, 0.1), (plopp, 0.9, dauer - 0.05))


def fluegel(rng, dauer, rate=22):
    """Flügelschlag einer großen Motte: pulsierendes, papierenes Rauschen."""
    n = n_(dauer)
    t = zeit(n)
    r = rate * (1 + 0.1 * glatt_rauschen(rng, n, 2))
    schlag = (0.5 + 0.5 * np.sin(2 * np.pi * np.cumsum(r / SR))) ** 3
    y = bp(rng.normal(0, 1, n), 150, 1800) * schlag + 0.25 * bp(rng.normal(0, 1, n), 3000, 8000) * schlag
    y += 0.2 * np.sin(2 * np.pi * np.cumsum(r / SR) * 1) * schlag
    return y


def rotor(rng, dauer, f=105.0, nah=1.0):
    """Drohnenrotor: Motorsurren mit Obertönen, Schwebung der vier Propeller, Luftrauschen."""
    n = n_(dauer)
    t = zeit(n)
    y = np.zeros(n)
    for k in range(4):
        fk = f * (1 + 0.012 * k) * (1 + 0.01 * glatt_rauschen(rng, n, 1.5))
        ph = np.cumsum(fk / SR)
        saege = 2 * (ph % 1) - 1
        y += saege * (0.7 + 0.3 * np.sin(2 * np.pi * fk * 0.5 * t + k))
    y = tp(y, 2400) + 0.35 * bp(rng.normal(0, 1, n), 500, 4000) * (0.6 + 0.4 * np.sin(2 * np.pi * f * 2 * t))
    return y * nah


def servo(rng, dauer, f_von=900, f_bis=1400):
    n = n_(dauer)
    f = kurve([f_von, f_bis, f_bis * 0.95], n)
    ph = np.cumsum(f / SR)
    y = np.sign(np.sin(2 * np.pi * ph)) * 0.4 + np.sin(2 * np.pi * ph * 2) * 0.4
    return tp(y, 4000) * huelle(n, 0.02, 0.05) + 0.3 * klickfolge(rng, dauer, 40, (3000, 6000), (0.001, 0.002))


def piepen(dauer=0.12, f=2400):
    n = n_(dauer)
    return np.sign(np.sin(2 * np.pi * f * zeit(n))) * 0.5 * huelle(n, 0.003, 0.01)


def sirene(rng, dauer, tief=650, hoch=920):
    n = n_(dauer)
    t = zeit(n)
    f = np.where((t * 2.4) % 1 < 0.5, tief, hoch)
    f = tp(f.astype(float), 30)
    ph = np.cumsum(f / SR)
    y = np.sign(np.sin(2 * np.pi * ph)) * 0.6 + 0.3 * np.sin(2 * np.pi * ph * 3)
    return saettigen(bp(y, 450, 3500, 3) * 2, 2.5) * huelle(n, 0.02, 0.1)


def geiger(rng, dauer, rate=25):
    return klickfolge(rng, dauer, rate, (3000, 6000), (0.0008, 0.002), pegel_streu=0.2)


def atemzug(rng, dauer, ein=True, eng=1.0, vokal="u"):
    """Ein Atemzug durch Mund/Maske: gefiltertes Rauschen in der Form eines Vokals."""
    n = n_(dauer)
    q = rng.normal(0, 1, n) * (kurve([0, 1, 0.8, 0], n) if ein else kurve([0, 0.8, 1, 0.3, 0], n))
    y = formanten(q, [vokal, "ə"] if ein else ["ə", vokal], rng, 1.0 * eng)
    return hp(y, 200)


def maskenatmen(rng, dauer):
    """Gasmaske: hohles Einatmen, Ventil klickt, pfeifendes Ausatmen."""
    zuege = []
    t = 0.0
    while t < dauer - 0.6:
        d = rng.uniform(0.55, 0.85)
        ein = bp(atemzug(rng, d, True, 0.8), 300, 2000)
        ventil = klick(rng, 0.008, 0, 2500)
        aus = bp(atemzug(rng, d * 0.9, False, 1.2, "o"), 600, 3500)
        zuege += [(ein, 1, t), (ventil, 1.2, t + d), (aus, 0.8, t + d + 0.03)]
        t += d * 2 + rng.uniform(0.1, 0.3)
    return mischen(*zuege)


def erdbrocken(rng, dauer, dichte=50):
    """Erde/Kies rieselt und bricht (Wühler, Schlund)."""
    n = n_(dauer)
    g = klickfolge(rng, dauer, dichte, (300, 2500), (0.004, 0.02))
    return g + 0.6 * tp(rng.normal(0, 1, n), 250) * (0.5 + 0.5 * glatt_rauschen(rng, n, 5))


def dumpf(rng, dauer=0.5, f=50):
    n = n_(dauer)
    t = zeit(n)
    return np.sin(2 * np.pi * (f + 60 * np.exp(-t * 20)) * t) * np.exp(-t * 7) + 0.3 * tp(rng.normal(0, 1, n), 300) * np.exp(-t * 25)


def porzellan(rng, dauer=0.6):
    """Porzellan: helle, glasige Teiltöne, ein feiner Riss."""
    n = n_(dauer)
    y = 0.6 * metall(rng, dauer, rng.uniform(2200, 3200), hart=0.2, teile=5)
    riss = klickfolge(rng, 0.15, 200, (4000, 9000), (0.0005, 0.002))
    return mischen((y, 1, 0), (riss, 0.6, rng.uniform(0, 0.1)))[:n]


def zaehneklappern(rng, dauer, rate=14):
    return klickfolge(rng, dauer, rate, (2500, 6000), (0.002, 0.006), unregelmaessig=0.3)


def fiepen(rng, dauer=0.15, f=3200):
    n = n_(dauer)
    t = zeit(n)
    f_k = f * (1 + 0.25 * np.sin(2 * np.pi * rng.uniform(25, 45) * t)) * kurve([1, 1.15, 0.9], n)
    return np.sin(2 * np.pi * np.cumsum(f_k / SR)) * huelle(n, 0.01, 0.05)


# ==== Bausteine für Laute ========================================================================

def stoehnen(rng, f0=100, groesse=1.0, rau=0.3, fry=0.35, knurr=0.0, vokale=None, dauer=None, hauch=0.3, nass=0.0):
    d = dauer or rng.uniform(1.2, 2.0)
    folgen = [["u", "o", "a", "o"], ["ng", "u", "o"], ["o", "a", "ə"], ["u", "ə", "o", "u"]]
    vok = vokale or folgen[rng.integers(len(folgen))]
    k = [f0 * x for x in (1.0, rng.uniform(1.05, 1.25), rng.uniform(0.95, 1.1), rng.uniform(0.75, 0.9))]
    s = stimme(rng, d, k, vok, groesse, hauch=hauch, fry=fry, knurr=knurr, rauh=rau, laut_kurve=[0, 0.8, 1, 0.8, 0.5, 0])
    if nass:
        s = s * (1 - 0.5 * nass) + nass * 0.5 * gurgel_modulation(rng, s)
    return s * huelle(len(s), 0.12, 0.35)


def gurgel_modulation(rng, x):
    """Lässt einen Laut durch Flüssigkeit blubbern (schnell schwankender Tiefpass)."""
    n = len(x)
    y = np.zeros(n)
    w = kurve(rng.uniform(300, 1400, max(4, int(n / SR * 12))), n)
    for s in range(0, n, 128):
        e = min(n, s + 128)
        y[s:e] = tp(x[max(0, s - 64):e], w[s])[-(e - s):]
    return norm(y)


def aechzen(rng, f0=110, groesse=1.0, fry=0.3, knurr=0.0, vokal="ə", hoch=1.6):
    """Kurzer Schmerzlaut: steiler Einsatz, Tonhöhe springt hoch und fällt."""
    d = rng.uniform(0.25, 0.45)
    k = [f0 * hoch, f0 * hoch * 1.15, f0 * 1.1, f0 * 0.9]
    s = stimme(rng, d, k, ["a", vokal] if rng.random() < 0.5 else ["ae", "ə"], groesse, hauch=0.3, fry=fry,
               knurr=knurr, laut_kurve=[0.4, 1, 0.6, 0])
    return s * huelle(len(s), 0.008, 0.12)


def sterben(rng, f0=100, groesse=1.0, fry=0.4, knurr=0.0, roecheln=0.5):
    """Todeslaut: Aufbäumen, absinkend, zum Schluss ein Röcheln."""
    d = rng.uniform(1.3, 2.0)
    k = [f0 * 1.5, f0 * 1.35, f0 * 1.0, f0 * 0.7, f0 * 0.5]
    s = stimme(rng, d, k, ["a", "o", "u", "ə"], groesse, hauch=0.35, fry=fry, knurr=knurr,
               laut_kurve=[0.7, 1, 0.8, 0.4, 0.1])
    n = len(s)
    rasseln = bp(rng.normal(0, 1, n), 250, 1600) * (0.5 + 0.5 * np.sign(np.sin(2 * np.pi * 28 * zeit(n))))
    s = s + roecheln * 0.5 * rasseln * kurve([0, 0, 0.3, 1, 0.6, 0], n)
    return s * huelle(n, 0.01, 0.4)


def schreien(rng, f0=420, groesse=1.25, rau=0.4, dauer=None, stimmen_=2, vokale=("ae", "a", "a", "ə")):
    d = dauer or rng.uniform(1.4, 2.2)
    k = [f0 * x for x in (0.8, 1.6, 1.9, 1.8, 1.3)]
    s = stimme(rng, d, k, list(vokale), groesse, hauch=0.35, rauh=rau, jitter=0.03, shimmer=0.2, stimmen=stimmen_,
               verstimmung=0.05, laut_kurve=[0, 1, 1, 0.9, 0])
    return saettigen(s * huelle(len(s), 0.03, 0.4), 2.2)


def knurren(rng, f0=75, groesse=0.8, dauer=None, vokal="u"):
    d = dauer or rng.uniform(0.9, 1.6)
    k = [f0 * x for x in (0.9, 1.0, 1.05, 0.95, 0.9)]
    s = stimme(rng, d, k, [vokal, "ə", vokal], groesse, hauch=0.4, fry=0.6, knurr=0.8, rauh=0.3,
               laut_kurve=[0.2, 0.9, 1, 0.8, 0.3])
    return s * huelle(len(s), 0.08, 0.25)


def bruellen(rng, f0=70, groesse=0.7, dauer=None, stimmen_=2, metallisch=0.0):
    d = dauer or rng.uniform(2.0, 2.8)
    k = [f0 * x for x in (0.8, 1.3, 1.5, 1.4, 1.0, 0.7)]
    s = stimme(rng, d, k, ["u", "o", "a", "a", "o"], groesse, hauch=0.4, fry=0.5, knurr=0.6, rauh=0.4,
               stimmen=stimmen_, verstimmung=0.04, laut_kurve=[0, 0.8, 1, 1, 0.7, 0])
    n = len(s)
    s += 0.35 * tp(rng.normal(0, 1, n), 300) * huelle(n, 0.3, 1.0)
    if metallisch:
        ir = metall(rng, 0.5, 220, hart=0.0, teile=8)
        s = (1 - metallisch) * s + metallisch * norm(fftconvolve(s, ir)[:n])
    return saettigen(s * huelle(n, 0.06, 0.8), 1.8)


def bellen(rng, f0=420, groesse=1.3):
    d = rng.uniform(0.14, 0.22)
    s = stimme(rng, d, [f0 * 1.1, f0 * 1.25, f0 * 0.8], ["a", "o"], groesse, hauch=0.5, fry=0.2, knurr=0.4, rauh=0.4,
               laut_kurve=[0.5, 1, 0.3])
    return saettigen(s * huelle(len(s), 0.004, 0.06), 2.5)


def heulen(rng, f0=420, groesse=1.3):
    d = rng.uniform(2.2, 3.0)
    k = [f0 * x for x in (0.7, 1.0, 1.15, 1.1, 1.0, 0.75)]
    s = stimme(rng, d, k, ["u", "o", "u", "o", "u"], groesse, hauch=0.25, jitter=0.008, shimmer=0.06,
               laut_kurve=[0, 0.8, 1, 0.9, 0.6, 0])
    return s * huelle(len(s), 0.2, 0.8)


def jaulen(rng, f0=600, groesse=1.4):
    d = rng.uniform(0.3, 0.5)
    s = stimme(rng, d, [f0 * 1.5, f0 * 1.3, f0 * 0.7], ["i", "u"], groesse, hauch=0.3, laut_kurve=[0.6, 1, 0])
    return s * huelle(len(s), 0.005, 0.15)


def keuchen(rng, f0=110, groesse=1.0, zuege=3, vokal="ə", stimmanteil=0.3):
    """Raues Atmen: ein, aus, ein, aus – mit einem Rest Stimme."""
    teile = []
    t = 0.0
    for _ in range(zuege):
        d = rng.uniform(0.35, 0.6)
        ein = atemzug(rng, d, True, groesse, "u")
        aus = atemzug(rng, d * 1.1, False, groesse, vokal)
        if stimmanteil:
            aus = aus + stimmanteil * stimme(rng, d * 1.1, [f0, f0 * 0.9], [vokal, vokal], groesse, hauch=0.6, fry=0.5,
                                             laut_kurve=[0, 1, 0.5, 0])
        teile += [(ein, 0.7, t), (aus, 1.0, t + d + 0.02)]
        t += d * 2.2 + rng.uniform(0.05, 0.2)
    return mischen(*teile)


def fluestern(rng, dauer=None, hell=1.0):
    d = dauer or rng.uniform(1.6, 2.6)
    n = n_(d)
    silben = np.clip(np.sin(2 * np.pi * rng.uniform(3.5, 5.5) * zeit(n) + rng.uniform(0, 6)), 0, 1) ** 2
    q = rng.normal(0, 1, n) * silben
    y = formanten(q, [rng.choice(list("aeiou")) for _ in range(int(d * 5) + 2)], rng, hell)
    return hp(y, 500) * huelle(n, 0.2, 0.5)


def summen(rng, f0=55, dauer=None):
    d = dauer or rng.uniform(2.0, 3.0)
    n = n_(d)
    t = zeit(n)
    s = np.sin(2 * np.pi * f0 * t) + 0.5 * np.sin(2 * np.pi * f0 * 2.01 * t) + 0.3 * np.sin(2 * np.pi * f0 * 3.03 * t)
    s *= 0.7 + 0.3 * np.sin(2 * np.pi * rng.uniform(0.5, 2) * t)
    s += 0.2 * bp(rng.normal(0, 1, n), 2000, 6000) * (0.5 + 0.5 * np.sin(2 * np.pi * 7 * t))
    return s * huelle(n, 0.5, 0.8)


def herzschlag(rng):
    return mischen((dumpf(rng, 0.5, 32), 1, 0.05), (dumpf(rng, 0.5, 30), 0.7, 0.38))


def husten(rng, f0=120, groesse=1.0, maske=False):
    teile = []
    t = 0.05
    for _ in range(rng.integers(2, 4)):
        m = rng.uniform(0.18, 0.26)
        stoss = bp(rng.normal(0, 1, n_(m)), 300, 2600) * np.exp(-zeit(n_(m)) * 14)
        stoss = stoss + 0.6 * stimme(rng, m, [f0 * 1.3, f0 * 0.9], ["a", "ə"], groesse, hauch=0.7, fry=0.5) * np.exp(-zeit(n_(m)) * 10)
        if maske:
            stoss = bp(stoss, 250, 1800)
        teile.append((stoss, 1, t))
        t += m + rng.uniform(0.08, 0.2)
    return mischen(*teile)


def klagen(rng, f0=150, groesse=1.0, hell=1.0):
    """Ein menschlicher Klagelaut (Imitator, Nebelwesen): fast verständlich."""
    d = rng.uniform(1.0, 1.6)
    k = [f0 * x for x in (1.0, 1.2, 1.3, 1.1, 0.85)]
    s = stimme(rng, d, k, ["ə", "a", "a", "o", "u"], groesse * hell, hauch=0.35, jitter=0.02, laut_kurve=[0, 1, 0.9, 0.6, 0])
    return s * huelle(len(s), 0.1, 0.4)


# ==== Die Arten ==================================================================================
# Jede Rezeptfunktion bekommt den Zufallsgenerator und gibt ein Signal zurück. Dazu: Hall-Anteil.

def R(funk, anzahl, nass=0.12):
    return (funk, anzahl, nass)


ARTEN = {}


def art(name):
    def deko(f):
        ARTEN[name] = f()
        return f
    return deko


@art("wanderer")
def _():
    # Die Infizierten: feuchtes, kehliges Stöhnen, mal kurz, mal lang; Schleim im Hals
    return {
        "ambient": R(lambda r: stoehnen(r, r.uniform(88, 112), 0.95, rau=0.35, fry=0.45, knurr=0.15, nass=0.35), 5),
        "hurt": R(lambda r: aechzen(r, r.uniform(100, 125), 0.95, fry=0.4, knurr=0.3), 4),
        "death": R(lambda r: sterben(r, r.uniform(95, 110), 0.95, fry=0.5, roecheln=0.8), 3),
    }


@art("schreier")
def _():
    # Dürr, Mund bis zum Hals: hechelndes Schluchzen, und dann der Schrei, der alles anlockt
    return {
        "ambient": R(lambda r: mischen((keuchen(r, 260, 1.25, 4, "i", 0.4), 1, 0),
                                       (fiepen(r, 0.2, r.uniform(1400, 1900)), 0.15, r.uniform(0.3, 1.0))), 3),
        "hurt": R(lambda r: schreien(r, r.uniform(380, 460), 1.3, dauer=r.uniform(0.35, 0.5)), 3),
        "death": R(lambda r: mischen((schreien(r, 360, 1.3, dauer=1.4), 1, 0), (sterben(r, 220, 1.25, roecheln=1.0), 0.6, 1.1)), 2, 0.2),
        "schrei": R(lambda r: schreien(r, r.uniform(430, 520), 1.35, rau=0.5, dauer=r.uniform(2.2, 2.8), stimmen_=3,
                                       vokale=("ae", "a", "ae", "a", "ə")), 4, 0.3),
    }


@art("lauerer")
def _():
    # Hockt im Schrank: rasselndes, pfeifendes Atmen. Springt er, ein spitzer, abgehackter Schrei
    return {
        "atmen": R(lambda r: mischen((keuchen(r, 85, 0.85, 3, "u", 0.15), 1, 0),
                                     (bp(r.normal(0, 1, n_(2.6)), 2500, 4500) * 0.05, 1, 0)), 4, 0.3),
        "schreck": R(lambda r: mischen((schreien(r, r.uniform(500, 600), 1.4, rau=0.6, dauer=0.9, stimmen_=2), 1, 0),
                                       (knacken(r, 0.6), 0.5, 0)), 3, 0.2),
        "hurt": R(lambda r: aechzen(r, 180, 1.2, fry=0.3, knurr=0.4, hoch=2.0), 3),
        "death": R(lambda r: mischen((sterben(r, 150, 1.15, roecheln=1.0), 1, 0), (knacken(r, 0.5), 0.6, 0.3)), 2),
    }


@art("kriecher")
def _():
    # Verdrehter Körper an Wand und Decke: knackende Gelenke, Chitinklicken, gurgelndes Zischen
    def gelenke(r, d):
        return mischen(*[(knacken(r, r.uniform(0.3, 0.8)), 0.8, r.uniform(0, d - 0.25)) for _ in range(r.integers(2, 5))])
    return {
        "ambient": R(lambda r: mischen((klickfolge(r, 1.6, 22, (1500, 4500), (0.003, 0.01)), 0.7, 0),
                                       (stimme(r, 1.4, [160, 150, 170, 140], ["i", "ə", "i"], 1.3, hauch=0.8, fry=0.6, knurr=0.5,
                                               laut_kurve=[0, 1, 0.7, 0]), 0.6, 0.15),
                                       (gelenke(r, 1.6), 0.8, 0)), 4, 0.15),
        "hurt": R(lambda r: mischen((aechzen(r, 220, 1.3, fry=0.4, knurr=0.6, vokal="i"), 1, 0), (knacken(r, 0.8), 0.7, 0)), 3),
        "death": R(lambda r: mischen((sterben(r, 170, 1.25, fry=0.6, knurr=0.4, roecheln=0.6), 1, 0),
                                     (gelenke(r, 1.6), 0.9, 0.2)), 2),
    }


@art("der_stille")
def _():
    # Porzellanmaske, er bewegt sich nur, wenn niemand hinsieht: kein Laut aus einem Mund – nur Porzellan,
    # Stoff, ein tiefes Dröhnen in der Nähe
    return {
        "naehe": R(lambda r: mischen((summen(r, r.uniform(38, 46), 3.0), 0.8, 0),
                                     (fluestern(r, 2.5, 0.8), 0.25, 0.3), (porzellan(r, 0.6), 0.3, r.uniform(0.5, 2.0))), 3, 0.5),
        "hurt": R(lambda r: mischen((porzellan(r, 0.5), 1, 0), (rascheln(r, 0.4), 0.4, 0)), 3, 0.4),
        "death": R(lambda r: mischen(*[(porzellan(r, 0.5), 0.8, i * r.uniform(0.05, 0.15)) for i in range(6)],
                                     (fluestern(r, 1.6, 0.7), 0.4, 0.4)), 2, 0.5),
    }


@art("imitator")
def _():
    # Sieht aus wie einer von uns und klingt fast so: menschliche Schmerzlaute, die eine Spur falsch sind
    return {
        "hurt": R(lambda r: (lambda s: s * huelle(len(s), 0.005, 0.12))(
            stimme(r, r.uniform(0.3, 0.45), [150, 195, 140, 120], ["a", "a", "ə"], 1.05, hauch=0.25, jitter=0.035,
                   laut_kurve=[0.5, 1, 0.4, 0])), 4),
        "death": R(lambda r: mischen((klagen(r, 140, 1.05), 1, 0),
                                     (stimme(r, 0.8, [140, 70, 45], ["a", "u", "ng"], 0.8, hauch=0.4, fry=0.8, knurr=0.7,
                                             laut_kurve=[1, 0.8, 0]), 0.8, 1.0)), 2, 0.2),
    }


@art("nebelwesen")
def _():
    # Nur im Nebel: hauchige Stimmen von überall, die sich überlagern, viel Hall
    return {
        "ambient": R(lambda r: mischen((klagen(r, r.uniform(200, 260), 1.15, 1.1), 0.7, 0),
                                       (fluestern(r, 2.2, 1.1), 0.6, 0.2),
                                       (klagen(r, r.uniform(150, 190), 1.1), 0.4, r.uniform(0.4, 0.9))), 4, 0.65),
        "hurt": R(lambda r: mischen((fluestern(r, 0.5, 1.3), 1, 0), (jaulen(r, 700, 1.3), 0.4, 0)), 3, 0.55),
        "death": R(lambda r: mischen((schreien(r, 300, 1.2, rau=0.1, dauer=1.6, stimmen_=3), 0.7, 0),
                                     (fluestern(r, 2.0, 1.2), 0.8, 0.5)), 2, 0.7),
    }


@art("schwarm")
def _():
    # Kleine haarlose Kreaturen mit Menschenzähnen: Fiepen wie Ratten, klappernde Zähne, Trippeln
    def fiep(r, d):
        return mischen(*[(fiepen(r, r.uniform(0.06, 0.18), r.uniform(2400, 4200)), r.uniform(0.4, 1), r.uniform(0, d - 0.2))
                         for _ in range(int(d * 8))])
    return {
        "ambient": R(lambda r: mischen((fiep(r, 1.4), 0.8, 0), (zaehneklappern(r, 1.4, 18), 0.6, 0),
                                       (klickfolge(r, 1.4, 50, (2000, 5000), (0.002, 0.005)), 0.25, 0)), 4),
        "hurt": R(lambda r: mischen((fiepen(r, 0.25, 3600), 1, 0), (zaehneklappern(r, 0.3, 30), 0.5, 0)), 3),
        "death": R(lambda r: mischen((fiepen(r, 0.5, 3000) * kurve([1, 1, 0.3], n_(0.5)), 1, 0), (schmatzen(r, 0.3, 300), 0.4, 0.2)), 2),
    }


@art("brut")
def _():
    # Aufgeblähter Körper voller Beulen: blubbernde, gespannte Laute, Haut knarzt; platzt mit nassem Knall
    return {
        "ambient": R(lambda r: mischen((stoehnen(r, r.uniform(120, 150), 1.0, fry=0.3, nass=0.9, vokale=["u", "o", "u"]), 0.8, 0),
                                       (blasen(r, 1.6, 18, 120, 500), 0.6, 0), (holzknarren(r, 0.8) * 0.3, 0.4, 0.5)), 4),
        "hurt": R(lambda r: mischen((aechzen(r, 160, 1.0, fry=0.2), 0.8, 0), (schmatzen(r, 0.4, 180), 0.8, 0)), 3),
        "death": R(lambda r: mischen((sterben(r, 130, 1.0, roecheln=0.4), 0.7, 0), (blasen(r, 1.5, 25, 100, 400), 0.8, 0.3)), 2),
        "platzen": R(lambda r: mischen((dumpf(r, 0.6, 45), 1.0, 0), (tp(r.normal(0, 1, n_(0.8)), 1800) * np.exp(-zeit(n_(0.8)) * 9), 0.9, 0),
                                       (schmatzen(r, 0.9, 150), 0.8, 0.05), (blasen(r, 1.0, 30, 150, 600), 0.4, 0.2)), 3, 0.2),
    }


@art("juggernaut")
def _():
    # Mit Metall verwachsen: tiefes Brüllen durch eine Brust aus Blech, schleifende Platten
    return {
        "ambient": R(lambda r: mischen((knurren(r, r.uniform(55, 65), 0.65, 1.6), 1, 0), (schleifen(r, 1.2, 140), 0.3, 0.2)), 4, 0.2),
        "hurt": R(lambda r: mischen((aechzen(r, 70, 0.7, fry=0.6, knurr=0.6, hoch=1.4), 1, 0), (metall(r, 0.6, 320), 0.6, 0)), 3, 0.2),
        "death": R(lambda r: mischen((bruellen(r, 60, 0.65, 2.4, metallisch=0.3), 1, 0), (metall(r, 1.0, 180), 0.8, 1.6),
                                     (dumpf(r, 0.6, 40), 0.8, 1.8)), 2, 0.25),
        "brullen": R(lambda r: bruellen(r, r.uniform(58, 66), 0.62, 2.6, stimmen_=3, metallisch=0.35), 3, 0.3),
        "panzer": R(lambda r: mischen((metall(r, 0.8, r.uniform(260, 340), hart=1.2), 1, 0), (schleifen(r, 0.3, 200), 0.3, 0.05)), 3, 0.2),
    }


@art("gefallener_soldat")
def _():
    # Ein Soldat der Wacht: Stöhnen unter dem Helm, klappernde Ausrüstung, das Funkgerät rauscht noch
    def unter_helm(r):
        s = stoehnen(r, r.uniform(100, 118), 0.95, fry=0.4, knurr=0.2)
        return bp(s, 120, 2600)
    return {
        "ambient": R(lambda r: (lambda wahl: mischen((unter_helm(r), 1, 0), (ausruestung(r, 1.5), 0.35, 0.1))
                                if wahl else mischen((funkrauschen(r, 1.6), 0.7, 0), (funkstimme(r, 1.0, 120), 0.5, 0.25),
                                                     (unter_helm(r) * 0.5, 0.5, 0.8)))(r.random() < 0.6), 5, 0.1),
        "hurt": R(lambda r: mischen((aechzen(r, 120, 0.95, fry=0.4, knurr=0.3), 1, 0), (metall(r, 0.25, 900, hart=0.8), 0.5, 0)), 3),
        "death": R(lambda r: mischen((sterben(r, 110, 0.95, roecheln=0.8), 1, 0), (ausruestung(r, 0.8), 0.6, 1.0),
                                     (funkrauschen(r, 1.2), 0.4, 1.3)), 2),
    }


@art("glutleib")
def _():
    # Brennt noch immer: verbrannte, pfeifende Kehle, Knistern und Zischen der Glut
    return {
        "ambient": R(lambda r: mischen((stoehnen(r, r.uniform(90, 110), 0.95, rau=0.8, fry=0.5, hauch=0.6), 0.8, 0),
                                       (knistern(r, 2.0, 45), 0.6, 0)), 4),
        "hurt": R(lambda r: mischen((aechzen(r, 120, 0.95, fry=0.5, knurr=0.4), 1, 0), (zischen(r, 0.5), 0.5, 0)), 3),
        "death": R(lambda r: mischen((sterben(r, 100, 0.95, roecheln=0.8), 0.8, 0), (knistern(r, 2.2, 90), 0.8, 0),
                                     (zischen(r, 1.2, 0.5), 0.5, 0.8)), 2),
        "glut": R(lambda r: mischen((knistern(r, 1.2, 70), 1, 0), (tp(r.normal(0, 1, n_(1.2)), 900) * huelle(n_(1.2), 0.1, 0.6), 0.6, 0)), 3),
    }


@art("seuchenhund")
def _():
    # Abgemagerte, infizierte Hunde: Knurren, heiseres Bellen, Jaulen, Heulen im Rudel
    return {
        "ambient": R(lambda r: (lambda w: knurren(r, r.uniform(110, 140), 1.2, r.uniform(1.0, 1.5), "u") if w < 0.6 else
                                mischen(*[(bellen(r, r.uniform(380, 460), 1.3), 1, i * r.uniform(0.25, 0.35)) for i in range(r.integers(2, 4))]))
                  (r.random()), 5),
        "hurt": R(lambda r: jaulen(r, r.uniform(650, 800), 1.45), 3),
        "death": R(lambda r: mischen((jaulen(r, 700, 1.45), 1, 0), (stimme(r, 1.0, [500, 380, 200], ["i", "u", "u"], 1.4, hauch=0.5,
                                                                         laut_kurve=[1, 0.5, 0]), 0.7, 0.3)), 2),
        "heulen": R(lambda r: mischen((heulen(r, r.uniform(400, 470), 1.35), 1, 0),
                                      (heulen(r, r.uniform(330, 380), 1.3), 0.5, r.uniform(0.3, 0.8))), 3, 0.45),
    }


@art("wuehler")
def _():
    # Blind, rundes Maul, im Fels: tiefes Grollen durch Gestein, knirschende Zähne, Erde bricht
    return {
        "ambient": R(lambda r: mischen((knurren(r, r.uniform(40, 50), 0.6, 1.6, "o"), 0.8, 0), (erdbrocken(r, 1.8, 30), 0.6, 0)), 4, 0.35),
        "hurt": R(lambda r: mischen((aechzen(r, 60, 0.6, fry=0.7, knurr=0.7, hoch=1.5), 1, 0), (erdbrocken(r, 0.4, 80), 0.5, 0)), 3, 0.3),
        "death": R(lambda r: mischen((bruellen(r, 45, 0.6, 2.0), 1, 0), (erdbrocken(r, 2.0, 60), 0.7, 0.5)), 2, 0.4),
        "graben": R(lambda r: mischen((erdbrocken(r, 1.2, 90), 1, 0), (dumpf(r, 0.5, 40), 0.6, 0)), 3, 0.3),
    }


@art("sauger")
def _():
    # Tentakel statt Mund: nasses Klicken und Schmatzen; saugt er, ein langer, gieriger Sog
    return {
        "ambient": R(lambda r: mischen((schmatzen(r, 1.4, 220), 0.8, 0), (klickfolge(r, 1.4, 10, (900, 2500), (0.004, 0.012)), 0.5, 0),
                                       (stimme(r, 1.2, [190, 170, 200], ["u", "i", "u"], 1.2, hauch=0.7, fry=0.4, laut_kurve=[0, 0.6, 0]) * 0.4, 1, 0.1)), 4),
        "hurt": R(lambda r: mischen((aechzen(r, 220, 1.25, fry=0.3, vokal="i", hoch=1.9), 0.9, 0), (schmatzen(r, 0.4, 260), 0.7, 0)), 3),
        "death": R(lambda r: mischen((sterben(r, 190, 1.2, roecheln=0.4), 0.8, 0), (schmatzen(r, 1.6, 180), 0.8, 0.2)), 2),
        "saugen": R(lambda r: saugen(r, r.uniform(1.0, 1.4)), 3),
    }


@art("seuchentraeger")
def _():
    # Arbeiter im zerrissenen Schutzanzug: Atmen durch die Gasmaske, Husten, der Geigerzähler tickt, der Tank leckt
    return {
        "ambient": R(lambda r: (lambda w: mischen((maskenatmen(r, 2.4), 1, 0), (geiger(r, 2.4, r.uniform(15, 40)), 0.35, 0))
                                if w else mischen((husten(r, 115, 1.0, maske=True), 1, 0), (geiger(r, 1.6, 50), 0.35, 0),
                                                  (zischen(r, 0.6, 0.6), 0.15, 0.6)))(r.random() < 0.6), 5),
        "hurt": R(lambda r: mischen((bp(aechzen(r, 120, 1.0, fry=0.4), 250, 2000), 1, 0), (geiger(r, 0.5, 120), 0.4, 0)), 3),
        "death": R(lambda r: mischen((bp(sterben(r, 110, 1.0, roecheln=0.9), 200, 2200), 1, 0), (zischen(r, 1.8, 0.4), 0.5, 0.3),
                                     (geiger(r, 2.0, 80), 0.4, 0)), 2),
        "spruehen": R(lambda r: mischen((zischen(r, 1.0, 2.0), 1, 0), (blasen(r, 1.0, 20, 300, 1200), 0.3, 0.05)), 3),
    }


@art("ertrunkener")
def _():
    # Aufgedunsen, mit Algen: Stöhnen durch Wasser in der Lunge, Blasen, Tropfen
    return {
        "ambient": R(lambda r: mischen((stoehnen(r, r.uniform(85, 100), 0.9, fry=0.5, nass=1.0), 0.9, 0),
                                       (blasen(r, 1.8, 16, 150, 700), 0.6, 0)), 4, 0.25),
        "hurt": R(lambda r: mischen((gurgel_modulation(r, aechzen(r, 110, 0.9, fry=0.4)), 1, 0), (blasen(r, 0.5, 30), 0.6, 0)), 3, 0.2),
        "death": R(lambda r: mischen((gurgel_modulation(r, sterben(r, 100, 0.9, roecheln=1.0)), 1, 0), (blasen(r, 2.0, 25, 100, 500), 0.8, 0.3)), 2, 0.3),
    }


@art("scheintoter")
def _():
    # Liegt da wie tot: flaches, rasselndes Atmen. Packt er zu: ein Keuchen, ein heiserer Schrei
    return {
        "atmen": R(lambda r: keuchen(r, 95, 0.95, 2, "ə", 0.5), 4, 0.1),
        "packen": R(lambda r: mischen((atemzug(r, 0.25, True, 1.1, "a"), 0.8, 0), (schreien(r, 300, 1.1, rau=0.7, dauer=0.8), 1, 0.15)), 3),
        "hurt": R(lambda r: aechzen(r, 125, 0.95, fry=0.5, knurr=0.3), 3),
        "death": R(lambda r: sterben(r, 110, 0.95, fry=0.6, roecheln=1.0), 2),
    }


@art("spaeherdrohne")
def _():
    # Militärdrohne, die niemand mehr steuert: Rotoren, Servos der Kamera, Piepen; entdeckt sie dich, Sirene
    return {
        "ambient": R(lambda r: mischen((rotor(r, 2.2, r.uniform(95, 120)), 0.8, 0), (servo(r, 0.35, 800, 1300), 0.4, r.uniform(0.3, 1.5)),
                                       (piepen(0.08, 2600), 0.25, r.uniform(0.2, 1.8))), 4, 0.1),
        "hurt": R(lambda r: mischen((klickfolge(r, 0.4, 300, (2000, 7000), (0.0005, 0.002)), 1, 0), (metall(r, 0.3, 1300, hart=1), 0.6, 0),
                                    (rotor(r, 0.5, 80) * kurve([1, 0.4, 1], n_(0.5)), 0.6, 0)), 3),
        "death": R(lambda r: mischen((rotor(r, 1.6, 110) * kurve([1, 0.7, 0.3, 0], n_(1.6)), 1, 0),
                                     (klickfolge(r, 1.0, 200, (2000, 7000), (0.0005, 0.002)), 0.6, 0),
                                     (metall(r, 0.8, 700, hart=1.2), 0.9, 1.4), (dumpf(r, 0.4, 70), 0.6, 1.4)), 2),
        "alarm": R(lambda r: mischen((sirene(r, 2.4), 1, 0), (piepen(0.1, 3000), 0.5, 0)), 2, 0.25),
    }


@art("lichtmotte")
def _():
    # Motte so groß wie ein Mensch: Flügelschlag, Fühler klicken, ein dünnes Zirpen
    return {
        "ambient": R(lambda r: mischen((fluegel(r, 1.8, r.uniform(16, 24)) * huelle(n_(1.8), 0.3, 0.4), 0.9, 0),
                                       (fiepen(r, 0.12, r.uniform(5000, 6500)), 0.2, r.uniform(0.2, 1.4))), 4),
        "flattern": R(lambda r: fluegel(r, 1.0, r.uniform(26, 34)) * huelle(n_(1.0), 0.05, 0.3), 3),
        "hurt": R(lambda r: mischen((fiepen(r, 0.3, 4800), 0.8, 0), (fluegel(r, 0.4, 40), 0.8, 0)), 3),
        "death": R(lambda r: mischen((fluegel(r, 1.4, 30) * kurve([1, 0.8, 0.3, 0], n_(1.4)), 1, 0), (fiepen(r, 0.6, 4200), 0.5, 0),
                                     (rascheln(r, 0.6), 0.6, 1.0)), 2),
    }


@art("tarnfresser")
def _():
    # Rinde, Moos und Zweige statt Haut: knarrendes Holz, brechende Äste, ein tiefes, hohles Brüllen
    return {
        "knacken": R(lambda r: mischen((knacken(r, 1.0), 1, 0), (rascheln(r, 0.5), 0.5, 0.05)), 4),
        "schreck": R(lambda r: mischen((bruellen(r, 75, 0.7, 1.4, stimmen_=2), 1, 0), (holzknarren(r, 1.2), 0.7, 0),
                                       (rascheln(r, 1.0), 0.8, 0), (knacken(r, 1.0), 0.7, 0)), 3, 0.15),
        "hurt": R(lambda r: mischen((holzknarren(r, 0.5), 1, 0), (knacken(r, 0.7), 0.8, 0), (aechzen(r, 80, 0.7, fry=0.7, knurr=0.6), 0.5, 0)), 3),
        "death": R(lambda r: mischen((holzknarren(r, 1.8), 1, 0), *[(knacken(r, 0.8), 0.7, 0.3 + i * 0.35) for i in range(3)],
                                     (rascheln(r, 1.4), 0.7, 0.8)), 2),
    }


# ---- Bosse -------------------------------------------------------------------------------------

@art("verschmolzener")
def _():
    # Ein Berg aus Körpern: viele Stimmen, die nicht zusammen stöhnen
    def chor(r, f0, d):
        return mischen(*[(stoehnen(r, f0 * r.uniform(0.8, 1.3), r.uniform(0.85, 1.05), fry=0.5, dauer=d), r.uniform(0.5, 1), r.uniform(0, 0.4))
                         for _ in range(5)])
    return {
        "ambient": R(lambda r: chor(r, 90, 1.8), 4, 0.25),
        "hurt": R(lambda r: mischen(*[(aechzen(r, r.uniform(90, 160), 1.0, fry=0.4), 0.8, r.uniform(0, 0.1)) for _ in range(3)],
                                    (schmatzen(r, 0.4), 0.6, 0)), 3, 0.2),
        "death": R(lambda r: mischen(*[(sterben(r, r.uniform(80, 140), 0.95), 0.7, r.uniform(0, 0.5)) for _ in range(4)],
                                     (schmatzen(r, 2.0, 160), 0.6, 0.4)), 2, 0.3),
        "brullen": R(lambda r: mischen((bruellen(r, 65, 0.65, 2.6, stimmen_=3), 1, 0), (chor(r, 110, 2.2), 0.6, 0.2)), 3, 0.3),
        "fusion": R(lambda r: mischen((schmatzen(r, 2.5, 150), 1, 0), (chor(r, 100, 2.0), 0.7, 0.3), *[(knacken(r, 0.8), 0.6, 0.2 + i * 0.4) for i in range(5)]), 2, 0.3),
    }


@art("nebelmutter")
def _():
    return {
        "ambient": R(lambda r: mischen((klagen(r, r.uniform(280, 340), 1.25, 1.1), 0.8, 0), (fluestern(r, 2.4, 1.2), 0.7, 0.2),
                                       (klagen(r, 220, 1.2), 0.4, 0.6)), 4, 0.7),
        "hurt": R(lambda r: schreien(r, 420, 1.3, rau=0.2, dauer=0.5, stimmen_=3), 3, 0.5),
        "death": R(lambda r: mischen((schreien(r, 380, 1.3, rau=0.2, dauer=2.4, stimmen_=4), 1, 0), (fluestern(r, 2.5, 1.2), 0.6, 1.0)), 2, 0.75),
        "brullen": R(lambda r: schreien(r, r.uniform(330, 380), 1.28, rau=0.3, dauer=2.4, stimmen_=4), 3, 0.7),
    }


@art("strahlende")
def _():
    return {
        "ambient": R(lambda r: mischen((summen(r, 70, 2.6), 0.6, 0), (geiger(r, 2.6, 90), 0.5, 0), (knurren(r, 62, 0.65, 2.0), 0.6, 0.3)), 4, 0.35),
        "hurt": R(lambda r: mischen((aechzen(r, 80, 0.7, fry=0.6, knurr=0.5), 1, 0), (geiger(r, 0.5, 300), 0.6, 0)), 3, 0.3),
        "death": R(lambda r: mischen((bruellen(r, 62, 0.65, 2.6, stimmen_=2), 1, 0), (geiger(r, 3.0, 200) * kurve([1, 1, 0.2], n_(3.0)), 0.6, 0)), 2, 0.4),
        "brullen": R(lambda r: mischen((bruellen(r, 66, 0.65, 2.6, stimmen_=3), 1, 0), (geiger(r, 2.6, 160), 0.5, 0)), 3, 0.4),
        # Strahlungspuls: der Zähler rast, ein tiefes Wummern
        "puls": R(lambda r: mischen((geiger(r, 1.6, 400) * kurve([0.2, 1, 0.4, 0], n_(1.6)), 0.8, 0), (dumpf(r, 0.8, 38), 1, 0.1),
                                    (summen(r, 72, 1.6), 0.6, 0)), 2, 0.4),
    }


@art("brutkoenigin")
def _():
    return {
        "ambient": R(lambda r: mischen((klickfolge(r, 2.0, 30, (1200, 4000), (0.003, 0.01)), 0.7, 0), (stoehnen(r, 200, 1.2, nass=0.8), 0.5, 0.2),
                                       (blasen(r, 2.0, 15), 0.4, 0)), 4, 0.3),
        "hurt": R(lambda r: mischen((schreien(r, 380, 1.3, dauer=0.5), 0.9, 0), (schmatzen(r, 0.4), 0.6, 0)), 3, 0.25),
        "death": R(lambda r: mischen((schreien(r, 340, 1.3, dauer=2.0, stimmen_=3), 1, 0), (schmatzen(r, 2.0, 140), 0.7, 0.4)), 2, 0.35),
        "brullen": R(lambda r: schreien(r, r.uniform(300, 360), 1.25, rau=0.6, dauer=2.4, stimmen_=3), 3, 0.35),
    }


@art("schlaechter")
def _():
    return {
        "ambient": R(lambda r: mischen((knurren(r, r.uniform(68, 78), 0.75, 1.6, "o"), 1, 0),
                                       (metall(r, 0.6, 520, hart=0.6), 0.25, r.uniform(0.3, 1.2))), 4, 0.15),
        "hurt": R(lambda r: aechzen(r, 85, 0.75, fry=0.6, knurr=0.6, hoch=1.5), 3),
        "death": R(lambda r: mischen((sterben(r, 80, 0.75, fry=0.7, knurr=0.4), 1, 0), (metall(r, 1.0, 480), 0.7, 1.3)), 2, 0.2),
        "brullen": R(lambda r: bruellen(r, r.uniform(70, 80), 0.72, 2.2), 3, 0.25),
    }


@art("kommandant")
def _():
    # Exo-Panzer: Stimme durch Helmlautsprecher, Servos, Funk
    def lautsprecher(s):
        return saettigen(bp(s, 350, 3200, 3) * 2, 2.5)
    return {
        "ambient": R(lambda r: mischen((lautsprecher(funkstimme(r, 1.2, 110)), 0.8, 0), (funkrauschen(r, 1.5), 0.4, 0),
                                       (servo(r, 0.4, 400, 600), 0.4, 1.0)), 4),
        "hurt": R(lambda r: mischen((lautsprecher(aechzen(r, 115, 0.95, fry=0.4)), 1, 0), (metall(r, 0.4, 600), 0.6, 0)), 3),
        "death": R(lambda r: mischen((lautsprecher(sterben(r, 110, 0.95)), 1, 0), (servo(r, 1.0, 600, 200), 0.6, 0.6),
                                     (metall(r, 1.0, 300), 0.8, 1.4)), 2),
        "brullen": R(lambda r: mischen((lautsprecher(bruellen(r, 90, 0.85, 2.0)), 1, 0), (sirene(r, 1.8, 500, 700), 0.3, 0.3)), 3, 0.2),
    }


@art("hirte")
def _():
    return {
        "ambient": R(lambda r: mischen((stoehnen(r, 95, 0.95, fry=0.2, vokale=["ng", "u", "ng"], hauch=0.2), 0.8, 0),
                                       (stoehnen(r, 142, 0.95, fry=0.2, vokale=["ng", "o", "ng"], hauch=0.2), 0.4, 0.1)), 4, 0.5),
        "hurt": R(lambda r: aechzen(r, 110, 0.95, fry=0.3), 3, 0.3),
        "death": R(lambda r: sterben(r, 95, 0.95, roecheln=0.6), 2, 0.5),
    }


@art("schlund")
def _():
    return {
        "ambient": R(lambda r: mischen((knurren(r, r.uniform(32, 40), 0.5, 2.2, "o"), 1, 0), (erdbrocken(r, 2.2, 25), 0.6, 0)), 4, 0.35),
        "hurt": R(lambda r: mischen((aechzen(r, 50, 0.5, fry=0.8, knurr=0.8, hoch=1.5), 1, 0), (erdbrocken(r, 0.5, 80), 0.5, 0)), 3, 0.3),
        "death": R(lambda r: mischen((bruellen(r, 40, 0.5, 2.8), 1, 0), (erdbrocken(r, 3.0, 70), 0.8, 0.5), (dumpf(r, 0.8, 30), 1, 2.4)), 2, 0.4),
        "brullen": R(lambda r: mischen((bruellen(r, r.uniform(38, 46), 0.5, 2.8, stimmen_=3), 1, 0), (erdbrocken(r, 2.8, 40), 0.5, 0)), 3, 0.4),
        "graben": R(lambda r: mischen((erdbrocken(r, 2.0, 70), 1, 0), (dumpf(r, 0.8, 35), 0.8, 0.2)), 3, 0.3),
        "auftauchen": R(lambda r: mischen((dumpf(r, 1.0, 30), 1, 0), (erdbrocken(r, 2.0, 140), 1, 0.05), (bruellen(r, 42, 0.5, 2.0), 0.7, 0.4)), 2, 0.4),
    }


@art("pilzvater")
def _():
    return {
        "ambient": R(lambda r: mischen((stoehnen(r, 55, 0.6, fry=0.5, nass=0.8, vokale=["u", "o", "u"]), 0.9, 0), (holzknarren(r, 1.6), 0.5, 0.3)), 4, 0.35),
        "hurt": R(lambda r: mischen((aechzen(r, 70, 0.65, fry=0.6, knurr=0.5), 1, 0), (schmatzen(r, 0.4), 0.5, 0)), 3, 0.3),
        "death": R(lambda r: mischen((sterben(r, 60, 0.6, roecheln=0.6), 1, 0), (holzknarren(r, 2.0), 0.6, 0.3), (rascheln(r, 1.5), 0.5, 0.8)), 2, 0.4),
        "sporen": R(lambda r: mischen((tp(r.normal(0, 1, n_(1.4)), 1500) * huelle(n_(1.4), 0.05, 0.9), 1, 0), (zischen(r, 1.0, 0.5), 0.4, 0)), 3, 0.3),
    }


@art("erfrorene")
def _():
    return {
        "ambient": R(lambda r: mischen((klagen(r, r.uniform(250, 300), 1.2, 1.1), 0.7, 0), (porzellan(r, 0.5) * 0.4, 0.5, r.uniform(0.3, 1.2)),
                                       (bp(r.normal(0, 1, n_(2.2)), 2000, 8000) * huelle(n_(2.2), 0.6, 0.8) * 0.3, 1, 0)), 4, 0.55),
        "hurt": R(lambda r: mischen((aechzen(r, 260, 1.2, fry=0.2, hoch=1.6), 1, 0), (porzellan(r, 0.4), 0.6, 0)), 3, 0.4),
        "death": R(lambda r: mischen((schreien(r, 320, 1.2, dauer=1.8, stimmen_=2), 1, 0), *[(porzellan(r, 0.5), 0.6, 0.8 + i * 0.12) for i in range(5)]), 2, 0.55),
        "brullen": R(lambda r: schreien(r, r.uniform(280, 330), 1.2, rau=0.4, dauer=2.3, stimmen_=3), 3, 0.55),
        # Eisatem: ein langer, kalter Hauch, Eis knistert
        "atem": R(lambda r: mischen((atemzug(r, 1.6, False, 1.2, "u"), 1, 0), (zischen(r, 1.6, 0.4), 0.5, 0),
                                    *[(porzellan(r, 0.3), 0.25, r.uniform(0.2, 1.4)) for _ in range(4)]), 3, 0.5),
    }


@art("das_herz")
def _():
    return {
        "ambient": R(lambda r: mischen((knurren(r, 36, 0.5, 2.2, "o"), 0.8, 0), (herzschlag(r), 0.8, 0.4), (schmatzen(r, 2.0, 120), 0.3, 0)), 4, 0.45),
        "hurt": R(lambda r: mischen((aechzen(r, 48, 0.5, fry=0.8, knurr=0.8, hoch=1.5), 1, 0), (schmatzen(r, 0.5, 140), 0.6, 0)), 3, 0.4),
        "death": R(lambda r: mischen((bruellen(r, 38, 0.5, 2.8, stimmen_=3), 1, 0), (herzschlag(r), 1, 2.2), (herzschlag(r), 0.5, 3.4)), 2, 0.5),
        "brullen": R(lambda r: bruellen(r, r.uniform(36, 42), 0.5, 2.8, stimmen_=3), 3, 0.5),
        "herzschlag": R(lambda r: herzschlag(r), 2, 0.3),
    }


@art("der_ursprung")
def _():
    return {
        "ambient": R(lambda r: mischen((summen(r, 44, 2.6), 0.8, 0), (fluestern(r, 2.4, 0.7), 0.5, 0.2), (knurren(r, 42, 0.5, 2.0), 0.5, 0.4)), 4, 0.6),
        "hurt": R(lambda r: mischen((aechzen(r, 55, 0.5, fry=0.7, knurr=0.6, hoch=1.5), 1, 0), (fluestern(r, 0.6, 0.9), 0.5, 0)), 3, 0.5),
        "death": R(lambda r: mischen((bruellen(r, 44, 0.5, 2.8, stimmen_=4), 1, 0), (fluestern(r, 3.0, 0.8), 0.6, 1.0), (summen(r, 40, 3.0), 0.6, 1.5)), 2, 0.7),
        "brullen": R(lambda r: mischen((bruellen(r, r.uniform(42, 48), 0.5, 2.8, stimmen_=4), 1, 0), (fluestern(r, 2.5, 0.8), 0.4, 0.3)), 3, 0.6),
        "puls": R(lambda r: herzschlag(r), 2, 0.4),
    }


@art("schwarmmutter")
def _():
    return {
        "ambient": R(lambda r: mischen((zaehneklappern(r, 2.0, 30), 0.7, 0), *[(fiepen(r, r.uniform(0.1, 0.25), r.uniform(1500, 2500)), 0.6, r.uniform(0, 1.6)) for _ in range(6)],
                                       (stoehnen(r, 230, 1.2, nass=0.6), 0.5, 0.2)), 4, 0.3),
        "hurt": R(lambda r: mischen((schreien(r, 420, 1.3, dauer=0.5), 0.9, 0), (fiepen(r, 0.3, 2400), 0.5, 0)), 3, 0.25),
        "death": R(lambda r: mischen((schreien(r, 380, 1.3, dauer=2.0, stimmen_=3), 1, 0), (schmatzen(r, 2.0, 160), 0.6, 0.4)), 2, 0.35),
        "brullen": R(lambda r: mischen((schreien(r, r.uniform(340, 400), 1.3, rau=0.6, dauer=2.4, stimmen_=3), 1, 0), (zaehneklappern(r, 2.4, 40), 0.5, 0)), 3, 0.35),
    }


@art("pilot")
def _():
    return {
        "ambient": R(lambda r: mischen((keuchen(r, 88, 0.85, 3, "ə", 0.4), 1, 0), (funkrauschen(r, 1.4), 0.4, 1.0),
                                       (metall(r, 0.5, 400, hart=0.5), 0.3, r.uniform(0.2, 1.5))), 4, 0.2),
        "hurt": R(lambda r: mischen((aechzen(r, 95, 0.85, fry=0.5, knurr=0.5), 1, 0), (metall(r, 0.4, 700), 0.6, 0)), 3, 0.2),
        "death": R(lambda r: mischen((sterben(r, 88, 0.85), 1, 0), (rotor(r, 2.0, 60) * kurve([1, 0.5, 0], n_(2.0)), 0.5, 0.4),
                                     (metall(r, 1.0, 260), 0.8, 1.8)), 2, 0.25),
        "brullen": R(lambda r: mischen((bruellen(r, 80, 0.8, 2.4, metallisch=0.3), 1, 0), (rotor(r, 2.4, 70), 0.3, 0)), 3, 0.25),
    }


# ---- Herz der Zone (Nether) ---------------------------------------------------------------------

def _dim(f0, groesse, rau, nass, extra=None, schrei=False):
    def amb(r):
        teile = [(stoehnen(r, f0 * r.uniform(0.9, 1.1), groesse, rau=rau, fry=0.3), 0.8, 0)]
        if extra:
            teile.append((extra(r), 0.6, r.uniform(0, 0.5)))
        return mischen(*teile)
    return {
        "ambient": R(amb, 4, nass),
        "hurt": R(lambda r: aechzen(r, f0 * 1.2, groesse, fry=0.3, knurr=0.3), 3, nass),
        "death": R(lambda r: schreien(r, f0 * 2.5, groesse, dauer=1.6) if schrei else sterben(r, f0, groesse), 2, nass),
    }


ARTEN["lenker"] = _dim(70, 0.7, 0.3, 0.5, lambda r: fluestern(r, 2.0, 0.8))
ARTEN["spuk"] = {
    "ambient": R(lambda r: mischen((fluestern(r, 2.2, 1.3), 1, 0), (klickfolge(r, 2.2, 6, (800, 3000), (0.01, 0.03)), 0.5, 0)), 4, 0.6),
    "hurt": R(lambda r: mischen((fluestern(r, 0.5, 1.4), 1, 0), (knacken(r, 0.6), 0.6, 0)), 3, 0.5),
    "death": R(lambda r: schreien(r, 500, 1.4, rau=0.2, dauer=1.6, stimmen_=3), 2, 0.7),
}
ARTEN["truegruede"] = {
    "ambient": R(lambda r: knurren(r, r.uniform(110, 130), 1.2, 1.4), 4, 0.2),
    "hurt": R(lambda r: jaulen(r, 620, 1.4), 3, 0.2),
    "death": R(lambda r: mischen((jaulen(r, 600, 1.4), 1, 0), (fluestern(r, 1.0, 1.2), 0.5, 0.3)), 2, 0.4),
}
ARTEN["entreisser"] = _dim(130, 1.0, 0.4, 0.3, lambda r: summen(r, 90, 1.5))
ARTEN["chimaere"] = {
    "ambient": R(lambda r: mischen((knurren(r, 95, 1.0, 1.4), 1, 0), (knurren(r, 140, 1.15, 1.2), 0.7, 0.2)), 4, 0.2),
    "hurt": R(lambda r: mischen((jaulen(r, 520, 1.3), 1, 0), (aechzen(r, 150, 1.1, knurr=0.6), 0.7, 0)), 3, 0.2),
    "death": R(lambda r: mischen((sterben(r, 120, 1.0, knurr=0.5), 1, 0), (jaulen(r, 500, 1.3), 0.6, 0.2)), 2, 0.25),
}
ARTEN["glasgaenger"] = {
    "ambient": R(lambda r: mischen((summen(r, 420, 2.2) * 0.4, 1, 0), *[(porzellan(r, 0.6), 0.5, r.uniform(0, 1.6)) for _ in range(3)]), 4, 0.5),
    "hurt": R(lambda r: porzellan(r, 0.5), 3, 0.4),
    "death": R(lambda r: mischen(*[(porzellan(r, 0.6), 0.8, i * 0.07) for i in range(10)]), 2, 0.5),
}
ARTEN["schwebender"] = _dim(90, 0.9, 0.3, 0.6, lambda r: summen(r, 60, 2.0))
ARTEN["entlader"] = {
    "ambient": R(lambda r: mischen((klickfolge(r, 2.0, 60, (2000, 8000), (0.0005, 0.002)), 0.7, 0), (summen(r, 100, 2.0), 0.5, 0)), 4, 0.2),
    "hurt": R(lambda r: mischen((klickfolge(r, 0.4, 400, (2000, 8000), (0.0005, 0.002)), 1, 0), (aechzen(r, 120, 1.0), 0.6, 0)), 3, 0.2),
    "death": R(lambda r: mischen((klickfolge(r, 1.5, 300, (2000, 8000), (0.0005, 0.002)) * kurve([1, 0.5, 0], n_(1.5)), 1, 0),
                                 (sterben(r, 110, 1.0), 0.7, 0.2)), 2, 0.25),
}
ARTEN["echo"] = _dim(125, 1.0, 0.25, 0.7, lambda r: klagen(r, 125, 1.0))
ARTEN["zonenhirsch"] = {
    "ambient": R(lambda r: (lambda s: s * huelle(len(s), 0.1, 0.5))(stimme(r, 1.6, [300, 520, 560, 380], ["u", "a", "a", "o"], 1.3,
                                                                           hauch=0.4, rauh=0.3, laut_kurve=[0, 1, 1, 0])), 3, 0.35),
    "hurt": R(lambda r: jaulen(r, 700, 1.35), 3, 0.25),
    "death": R(lambda r: sterben(r, 260, 1.3, roecheln=0.4), 2, 0.3),
}

# ---- Orbitalstation (End) ----------------------------------------------------------------------

ARTEN["leerlaeufer"] = {
    "ambient": R(lambda r: mischen((bp(keuchen(r, 95, 0.95, 3, "u", 0.3), 200, 2500), 1, 0), (funkrauschen(r, 1.0), 0.3, 1.2)), 4, 0.4),
    "hurt": R(lambda r: bp(aechzen(r, 110, 0.95), 200, 2600), 3, 0.3),
    "death": R(lambda r: mischen((bp(sterben(r, 95, 0.95), 200, 2600), 1, 0), (zischen(r, 1.5, 0.8), 0.5, 0.6)), 2, 0.4),
}
ARTEN["wartungsspinne"] = {
    "ambient": R(lambda r: mischen((servo(r, 0.5, 900, 1300), 0.7, 0), (klickfolge(r, 1.6, 30, (2000, 5000), (0.002, 0.006)), 0.7, 0),
                                   (servo(r, 0.4, 1300, 900), 0.6, 0.9), (zischen(r, 0.3), 0.2, 1.2)), 4, 0.25),
    "hurt": R(lambda r: mischen((metall(r, 0.4, 1100), 1, 0), (klickfolge(r, 0.3, 300, (2000, 7000), (0.0005, 0.002)), 0.6, 0)), 3, 0.2),
    "death": R(lambda r: mischen((servo(r, 1.0, 1200, 300), 1, 0), (metall(r, 0.8, 600), 0.8, 0.9)), 2, 0.25),
}
ARTEN["wachkugel"] = {
    "ambient": R(lambda r: mischen((summen(r, 220, 2.0) * 0.6, 1, 0), (piepen(0.08, 1800), 0.4, r.uniform(0.2, 1.6))), 4, 0.3),
    "hurt": R(lambda r: mischen((klickfolge(r, 0.3, 300, (2000, 7000), (0.0005, 0.002)), 1, 0), (piepen(0.15, 900), 0.6, 0)), 3, 0.2),
    "death": R(lambda r: mischen((summen(r, 220, 1.5) * kurve([1, 0.5, 0], n_(1.5)), 1, 0), (metall(r, 0.6, 800), 0.8, 1.2)), 2, 0.3),
}
ARTEN["versuchsobjekt"] = {
    "ambient": R(lambda r: knurren(r, r.uniform(140, 170), 1.1, 1.3, "i"), 4, 0.25),
    "hurt": R(lambda r: aechzen(r, 180, 1.15, fry=0.4, knurr=0.6, hoch=2.0), 3, 0.2),
    "death": R(lambda r: schreien(r, 380, 1.2, rau=0.6, dauer=1.4), 2, 0.3),
}
ARTEN["konglomerat"] = {k: v for k, v in ARTEN["verschmolzener"].items() if k in ("ambient", "hurt", "death")}
ARTEN["kryoschlaefer"] = {
    "ambient": R(lambda r: mischen((keuchen(r, 110, 1.0, 3, "u", 0.3), 1, 0), (zischen(r, 1.2, 0.5) * 0.4, 0.6, 0.2)), 4, 0.45),
    "hurt": R(lambda r: mischen((aechzen(r, 130, 1.05), 1, 0), (porzellan(r, 0.4), 0.5, 0)), 3, 0.35),
    "death": R(lambda r: mischen((sterben(r, 110, 1.0), 1, 0), *[(porzellan(r, 0.5), 0.5, 0.6 + i * 0.1) for i in range(4)]), 2, 0.45),
}
ARTEN["leerenaal"] = {
    "ambient": R(lambda r: mischen((summen(r, 60, 2.4), 0.8, 0), (heulen(r, 160, 0.9), 0.4, 0.3)), 4, 0.7),
    "hurt": R(lambda r: jaulen(r, 400, 1.1), 3, 0.5),
    "death": R(lambda r: mischen((heulen(r, 140, 0.9), 1, 0), (summen(r, 50, 2.0), 0.6, 0.5)), 2, 0.7),
}
ARTEN["klammerer"] = {
    "ambient": R(lambda r: mischen((klickfolge(r, 1.2, 35, (2500, 6000), (0.002, 0.006)), 1, 0), (fiepen(r, 0.2, 3500), 0.5, 0.4)), 4, 0.2),
    "hurt": R(lambda r: fiepen(r, 0.3, 4200), 3, 0.2),
    "death": R(lambda r: mischen((schreien(r, 700, 1.5, dauer=0.8), 1, 0), (schmatzen(r, 0.5, 300), 0.6, 0.3)), 2, 0.25),
}
ARTEN["hologeist"] = {
    "ambient": R(lambda r: mischen((bp(funkstimme(r, 1.6, 170), 500, 4000), 0.8, 0), (klickfolge(r, 1.6, 20, (3000, 8000), (0.0005, 0.002)), 0.5, 0)), 4, 0.6),
    "hurt": R(lambda r: mischen((klickfolge(r, 0.4, 300, (3000, 9000), (0.0005, 0.002)), 1, 0), (piepen(0.1, 1200), 0.5, 0)), 3, 0.4),
    "death": R(lambda r: mischen((summen(r, 180, 1.6) * kurve([1, 0.4, 0], n_(1.6)), 1, 0), (klickfolge(r, 1.2, 200, (3000, 9000), (0.0005, 0.002)), 0.6, 0)), 2, 0.5),
}
ARTEN["forscher"] = {
    "ambient": R(lambda r: mischen((husten(r, 135, 1.05), 1, 0), (metall(r, 0.3, 2400, hart=0.4), 0.3, r.uniform(0.3, 1.0))), 4, 0.25),
    "hurt": R(lambda r: aechzen(r, 145, 1.05), 3, 0.2),
    "death": R(lambda r: sterben(r, 130, 1.05), 2, 0.25),
}


# ==== Schreiben ===================================================================================

def speichern(pfad, x, rng, nass):
    x = np.asarray(x, dtype=float)
    x = raum(x, 1.4, nass, rng)
    x = hp(x, 35)
    x = norm(x) * 0.85
    x *= huelle(len(x), 0.004, 0.06)
    os.makedirs(os.path.dirname(pfad), exist_ok=True)
    with tempfile.TemporaryDirectory() as tmp:
        wav = os.path.join(tmp, "x.wav")
        wavfile.write(wav, SR, (x * 32767).astype(np.int16))
        subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", wav, "-ac", "1", "-c:a", "libvorbis", "-q:a", "3", pfad],
                       check=True)


def eintraege():
    """Für sounds.py: Klangname -> Liste der Dateien (deadzonemc:monster/<art>/<klang>_<n>)."""
    aus = {}
    for a, klaenge in ARTEN.items():
        for name, (_, anzahl, _) in klaenge.items():
            aus[f"{a}.{name}"] = [{"name": f"deadzonemc:monster/{a}/{name}_{i}"} for i in range(1, anzahl + 1)]
    return aus


def erzeugen(a):
    klaenge = ARTEN[a]
    ordner = os.path.join(ORDNER, a)
    if os.path.isdir(ordner):
        shutil.rmtree(ordner)
    for name, (funk, anzahl, nass) in klaenge.items():
        for i in range(1, anzahl + 1):
            rng = np.random.default_rng(sum(map(ord, a + name)) * 977 + i * 31)
            x = funk(rng)
            speichern(os.path.join(ordner, f"{name}_{i}.ogg"), x, rng, nass)
    return f"{a}: {sum(v[1] for v in klaenge.values())} Klänge"


if __name__ == "__main__":
    from concurrent.futures import ProcessPoolExecutor
    arten = sys.argv[1:] or list(ARTEN)
    with ProcessPoolExecutor(max_workers=os.cpu_count() or 4) as pool:
        for zeile in pool.map(erzeugen, arten):
            print(zeile, flush=True)
