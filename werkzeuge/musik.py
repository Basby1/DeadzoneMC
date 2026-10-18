"""Musik von Deadzone MC – komplett hier synthetisiert (keine fremden Aufnahmen, keine Lizenzfragen).

  python3 werkzeuge/musik.py            – alle Stücke
  python3 werkzeuge/musik.py nacht tief – nur diese

Schreibt assets/deadzonemc/sounds/musik/<name>.ogg. Welche Musik wann läuft, entscheidet der Client
(de.deadzonemc.client.ClientMusik): Titel, Tag, Nebel, Nacht, unter Tage, Bosskampf/Emission.
"""
import os
import subprocess
import sys
import tempfile

import numpy as np
from scipy.io import wavfile
from scipy.signal import butter, fftconvolve, lfilter, sosfilt

SR = 44100
WURZEL = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ORDNER = os.path.join(WURZEL, "src", "main", "resources", "assets", "deadzonemc", "sounds", "musik")


def hz(note):
    namen = {"C": 0, "C#": 1, "Db": 1, "D": 2, "D#": 3, "Eb": 3, "E": 4, "F": 5, "F#": 6, "Gb": 6, "G": 7,
             "G#": 8, "Ab": 8, "A": 9, "A#": 10, "Bb": 10, "B": 11}
    n, okt = note[:-1], int(note[-1])
    return 440.0 * 2 ** ((namen[n] + 12 * (okt + 1) - 69) / 12)


def tiefpass(x, f, ordnung=2):
    return sosfilt(butter(ordnung, f, "low", fs=SR, output="sos"), x)


def hochpass(x, f, ordnung=2):
    return sosfilt(butter(ordnung, f, "high", fs=SR, output="sos"), x)


def bandpass(x, a, b):
    return sosfilt(butter(2, [a, b], "band", fs=SR, output="sos"), x)


def huellkurve(laenge, anschlag, abklang):
    n = int(laenge * SR)
    e = np.ones(n)
    a = min(n, int(anschlag * SR))
    r = min(n - a, int(abklang * SR))
    if a:
        e[:a] = np.linspace(0, 1, a) ** 2
    if r:
        e[n - r:] = np.linspace(1, 0, r) ** 2
    return e


class Stueck:
    def __init__(self, dauer, saat):
        self.dauer = dauer
        self.n = int(SR * dauer)
        self.t = np.arange(self.n) / SR
        self.spur = np.zeros(self.n)
        self.rng = np.random.default_rng(saat)

    def dazu(self, start, klang, lautst=1.0):
        i = int(start * SR)
        j = min(self.n, i + len(klang))
        if 0 <= i < self.n:
            self.spur[i:j] += klang[:j - i] * lautst

    # ---- Schichten -------------------------------------------------------------------------
    def droehnen(self, grundtoene, lautst, periode=37.0, hell=380):
        x = np.zeros(self.n)
        for f0, a in grundtoene:
            for k in range(1, 9):
                verstimmt = f0 * k * (1 + 0.0015 * np.sin(2 * np.pi * 0.03 * self.t + k))
                phase = 2 * np.pi * np.cumsum(verstimmt) / SR
                x += a / k ** 1.3 * np.sin(phase + self.rng.random() * 6)
        x = tiefpass(x, hell)
        self.spur += x * (0.6 + 0.4 * np.sin(2 * np.pi * self.t / periode - 1.2)) * lautst

    def flaeche(self, noten, laenge, hell=1400, tremolo=0.0):
        n = int(laenge * SR)
        tt = np.arange(n) / SR
        x = np.zeros(n)
        for note in noten:
            f = hz(note)
            for verst in (-0.004, 0.0, 0.005):
                for k, a in ((1, 1.0), (2, 0.35), (3, 0.15), (4, 0.06)):
                    x += a * np.sin(2 * np.pi * f * (1 + verst) * k * tt + self.rng.random() * 6)
        x = tiefpass(x, hell)
        if tremolo:
            x *= 1 - tremolo * (0.5 + 0.5 * np.sin(2 * np.pi * 7 * tt))
        return x * huellkurve(laenge, laenge * 0.35, laenge * 0.45)

    def flaechen(self, akkorde, laenge, lautst, start=4.0, hell=1400, tremolo=0.0):
        i = 0
        while start < self.dauer - 6:
            self.dazu(start, self.flaeche(akkorde[i % len(akkorde)], laenge + 6, hell, tremolo), lautst)
            start += laenge
            i += 1

    def klavier(self, note, laenge=5.0):
        n = int(laenge * SR)
        tt = np.arange(n) / SR
        f = hz(note)
        x = np.zeros(n)
        for k, a in ((1, 1.0), (2, 0.5), (3, 0.22), (4, 0.12), (5, 0.06)):
            x += a * np.sin(2 * np.pi * f * k * (1 + 0.0004 * k * k) * tt) * np.exp(-tt * (1.1 + 0.6 * k))
        return x * np.minimum(1, tt * 400)

    def zupf(self, note, laenge=4.0, daempfung=0.996):
        """Gezupfte Saite (Karplus-Strong), klingt wie eine alte Gitarre."""
        f = hz(note)
        p = int(SR / f)
        n = int(laenge * SR)
        anreg = np.zeros(n)
        anreg[:p] = self.rng.uniform(-1, 1, p)
        anreg[:p] = tiefpass(anreg[:p], 3000)
        a = np.zeros(p + 2)
        a[0] = 1
        a[p] = -daempfung / 2
        a[p + 1] = -daempfung / 2
        return lfilter([1.0], a, anreg)

    def glocke(self, note, laenge=7.0):
        n = int(laenge * SR)
        tt = np.arange(n) / SR
        f = hz(note)
        x = np.zeros(n)
        for r, a in ((1.0, 1.0), (2.0, 0.5), (2.76, 0.4), (4.07, 0.25), (5.4, 0.15)):
            x += a * np.sin(2 * np.pi * f * r * tt) * np.exp(-tt * (0.5 + r * 0.3))
        return x * np.minimum(1, tt * 800)

    def melodie(self, einsaetze, instrument, lautst):
        for start, motiv in einsaetze:
            for note, dt in motiv:
                klang = getattr(self, instrument)(note)
                self.dazu(start + dt + self.rng.normal(0, 0.03), klang, lautst * self.rng.uniform(0.8, 1.0))

    def metall(self, zeiten, lautst):
        for start in zeiten:
            f0 = self.rng.uniform(70, 120)
            n = int(7 * SR)
            tt = np.arange(n) / SR
            x = np.zeros(n)
            for r, a in ((1.0, 1.0), (2.76, 0.6), (5.40, 0.4), (8.93, 0.25), (13.3, 0.12)):
                x += a * np.sin(2 * np.pi * f0 * r * tt) * np.exp(-tt * (0.6 + r * 0.25))
            self.dazu(start, tiefpass(x, 2500), lautst * self.rng.uniform(0.6, 1.0))

    def wind(self, lautst):
        x = bandpass(self.rng.normal(0, 1, self.n), 180, 900)
        lfo = 0.5 + 0.5 * np.sin(2 * np.pi * self.t / 11.0) * np.sin(2 * np.pi * self.t / 29.0 + 1)
        self.spur += x * (0.2 + 0.8 * lfo) * lautst

    def herzschlag(self, bpm, lautst, von=0.0, bis=None):
        bis = bis or self.dauer
        n = int(0.35 * SR)
        tt = np.arange(n) / SR
        schlag = np.sin(2 * np.pi * (48 + 30 * np.exp(-tt * 30)) * tt) * np.exp(-tt * 14)
        t0 = von
        while t0 < bis:
            self.dazu(t0, schlag, lautst)
            self.dazu(t0 + 0.28, schlag, lautst * 0.6)
            t0 += 60.0 / bpm

    def trommel(self, start, tief=1.0, lautst=1.0):
        n = int(1.2 * SR)
        tt = np.arange(n) / SR
        f = (70 + 90 * np.exp(-tt * 18)) * tief
        x = np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-tt * 5)
        x += tiefpass(self.rng.normal(0, 1, n), 1200) * np.exp(-tt * 40) * 0.4
        self.dazu(start, x, lautst)

    def rhythmus(self, bpm, muster, lautst, von, bis):
        """muster: Liste aus (Schlag im Takt in Achteln, Tonhöhenfaktor, Stärke) für einen 4/4-Takt."""
        takt = 4 * 60.0 / bpm
        t0 = von
        while t0 < bis:
            for achtel, tief, st in muster:
                self.trommel(t0 + achtel * takt / 8, tief, lautst * st)
            t0 += takt

    def ostinato(self, noten, bpm, lautst, von, bis):
        achtel = 60.0 / bpm / 2
        t0 = von
        i = 0
        while t0 < bis:
            n = int(achtel * 0.9 * SR)
            tt = np.arange(n) / SR
            f = hz(noten[i % len(noten)])
            x = sum(a * np.sin(2 * np.pi * f * k * tt) for k, a in ((1, 1), (2, 0.5), (3, 0.3)))
            x = tiefpass(x * np.exp(-tt * 6), 900)
            self.dazu(t0, x, lautst)
            t0 += achtel
            i += 1

    def tropfen(self, anzahl, lautst):
        for _ in range(anzahl):
            start = self.rng.uniform(2, self.dauer - 4)
            n = int(0.25 * SR)
            tt = np.arange(n) / SR
            f = self.rng.uniform(1300, 2600) * (1 + 0.6 * tt)
            x = np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-tt * 28)
            self.dazu(start, x, lautst * self.rng.uniform(0.4, 1.0))

    # ---- Ausgabe ---------------------------------------------------------------------------
    def hall(self, x, dauer, saat):
        r = np.random.default_rng(saat)
        n = int(dauer * SR)
        ir = r.normal(0, 1, n) * np.exp(-np.arange(n) / SR * 5.5 / dauer)
        ir = tiefpass(ir, 5000)
        ir /= np.sqrt(np.sum(ir ** 2))
        return fftconvolve(x, ir)[:len(x)]

    def speichern(self, name, hall=4.5, nass=0.6, einblenden=5.0, ausblenden=8.0, pegel=0.7):
        trocken = hochpass(self.spur, 32)
        links = 0.55 * trocken + nass * self.hall(trocken, hall, 1)
        rechts = 0.55 * trocken + nass * self.hall(trocken, hall, 2)
        stereo = np.stack([links, rechts], axis=1) * huellkurve(self.dauer, einblenden, ausblenden)[:self.n, None]
        stereo /= np.max(np.abs(stereo)) / pegel
        os.makedirs(ORDNER, exist_ok=True)
        ziel = os.path.join(ORDNER, f"{name}.ogg")
        with tempfile.TemporaryDirectory() as tmp:
            wav = os.path.join(tmp, "x.wav")
            wavfile.write(wav, SR, (stereo * 32767).astype(np.int16))
            subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", wav, "-c:a", "libvorbis", "-q:a", "2", ziel],
                           check=True)
        print(f"{name}: {self.dauer:.0f} s, {os.path.getsize(ziel) // 1024} KB")


# ---- Die Stücke ----------------------------------------------------------------------------------
DM = [["D3", "F3", "A3", "D4"], ["Bb2", "D3", "F3", "Bb3"], ["G2", "Bb2", "D3", "G3"], ["A2", "C#3", "E3", "A3"]]


def titel():
    """Sektor 4 – Titelmusik (gruseliger): verstimmte Spieluhr, tiefes Dröhnen, Chorhauch, Funkrauschen,
    ein Herzschlag in der Ferne und eine Sirene, die irgendwo anschwillt."""
    s = Stueck(170, 913)
    s.droehnen([(hz("C1"), 1.0), (hz("G1"), 0.45), (hz("C#2"), 0.2)], 0.12, periode=23.0)
    chor = [["C3", "Eb3", "G3"], ["Ab2", "C3", "Eb3"], ["F2", "Ab2", "C3"], ["G2", "B2", "D3"]]
    s.flaechen(chor, 18.0, 0.05, start=6.0, hell=900, tremolo=0.15)
    # Spieluhr: hohe, leicht verstimmte Glöckchen, immer langsamer
    def spieluhr(note, laenge=3.5):
        n = int(laenge * SR)
        tt = np.arange(n) / SR
        f = hz(note) * (1 + 0.006 * np.sin(2 * np.pi * 0.3 * tt))
        x = np.zeros(n)
        for r, a in ((1.0, 1.0), (3.0, 0.25), (5.1, 0.12)):
            x += a * np.sin(2 * np.pi * f * r * tt) * np.exp(-tt * (2.2 + r * 0.6))
        return x * np.minimum(1, tt * 900)
    motiv = ["C6", "Eb6", "G5", "C6", "D6", "Eb6", "D6", "B5", "C6", "G5"]
    t0 = 14.0
    for wdh in range(4):
        schritt = 0.55 + wdh * 0.12
        for i, note in enumerate(motiv):
            s.dazu(t0 + i * schritt + s.rng.normal(0, 0.02), spieluhr(note), 0.09 * (1 - wdh * 0.15))
        t0 += len(motiv) * schritt + 18.0
    s.herzschlag(46, 0.05, von=40.0, bis=150.0)
    # ferne Sirene
    n = int(16 * SR)
    tt = np.arange(n) / SR
    f = 330 + 260 * (0.5 - 0.5 * np.cos(2 * np.pi * tt / 8.0))
    sir = np.sin(2 * np.pi * np.cumsum(f) / SR) * huellkurve(16, 5, 6)
    s.dazu(96.0, tiefpass(sir, 1200), 0.035)
    # Funkrauschen
    for st in (30.0, 72.0, 124.0):
        m = int(3 * SR)
        s.dazu(st, bandpass(s.rng.normal(0, 1, m), 400, 3000) * huellkurve(3, 0.4, 1.2), 0.02)
    s.metall([22.0, 61.0, 108.0, 146.0], 0.05)
    s.wind(0.02)
    s.speichern("titel", hall=5.0, nass=0.65)


def morgen():
    """Grauer Morgen – Tag: gezupfte Gitarre über weichen Flächen, a-Moll, etwas Hoffnung."""
    s = Stueck(150, 11)
    s.droehnen([(hz("A1"), 0.8), (hz("E2"), 0.4)], 0.06, periode=29.0)
    s.flaechen([["A2", "C3", "E3", "A3"], ["F2", "A2", "C3", "F3"], ["C3", "E3", "G3", "C4"], ["G2", "B2", "D3", "G3"]],
               16.0, 0.05, start=2.0, hell=1800)
    figur = [("A3", 0.0), ("E4", 0.5), ("C4", 1.0), ("E4", 1.5), ("A3", 2.0), ("E4", 2.5), ("B3", 3.0), ("C4", 3.5)]
    figur_f = [("F3", 0.0), ("C4", 0.5), ("A3", 1.0), ("C4", 1.5), ("F3", 2.0), ("C4", 2.5), ("G3", 3.0), ("A3", 3.5)]
    einsaetze = []
    for start in range(10, 140, 16):
        einsaetze.append((float(start), figur))
        einsaetze.append((start + 8.0, figur_f))
    s.melodie(einsaetze, "zupf", 0.16)
    lied = [("E4", 0.0), ("D4", 2.0), ("C4", 4.0), ("B3", 6.0), ("C4", 8.0), ("A3", 10.0)]
    s.melodie([(42.0, lied), (90.0, lied)], "klavier", 0.14)
    s.wind(0.012)
    s.speichern("morgen", hall=3.5, nass=0.5)


def nebel():
    """Nebelwand – dichter Nebel und Dämmerung: Cluster-Flächen, ferne Glocken, keine Melodie zum Festhalten."""
    s = Stueck(140, 23)
    s.droehnen([(hz("E1"), 1.0), (hz("B1"), 0.4)], 0.1, periode=23.0, hell=300)
    s.flaechen([["E3", "F3", "B3"], ["D3", "Eb3", "A3"], ["E3", "G3", "Ab3"], ["C3", "Db3", "G3"]], 18.0, 0.05,
               start=3.0, hell=1100)
    for st in (14.0, 37.0, 58.0, 81.0, 104.0, 122.0):
        s.dazu(st, s.glocke(s.rng.choice(["E5", "F5", "B4", "Bb4"])), 0.06)
    s.metall([30.0, 95.0], 0.04)
    s.wind(0.03)
    s.speichern("nebel", hall=6.0, nass=0.75)


def nacht():
    """Nachtwache – Nacht: Herzschlag, tiefe phrygische Flächen, hohe einzelne Klaviertöne."""
    s = Stueck(150, 37)
    s.droehnen([(hz("D1"), 1.0), (hz("Eb2"), 0.25)], 0.1, periode=31.0, hell=320)
    s.flaechen([["D3", "F3", "A3"], ["Eb3", "G3", "Bb3"], ["D3", "F3", "A3"], ["C3", "Eb3", "G3"]], 18.0, 0.045,
               start=3.0, hell=1000)
    s.herzschlag(54, 0.12, von=8.0, bis=140.0)
    hoch = [("A5", 0.0), ("Bb5", 3.0), ("A5", 6.0), ("F5", 10.0)]
    s.melodie([(30.0, hoch), (70.0, hoch), (112.0, hoch[:3])], "klavier", 0.07)
    s.metall([22.0, 61.0, 99.0, 131.0], 0.05)
    s.wind(0.02)
    s.speichern("nacht", hall=5.0, nass=0.65)


def tief():
    """Unter Tage – Höhlen, Metro, Station Null: tiefer Bass, tropfendes Wasser, Rohre, kaum Harmonie."""
    s = Stueck(140, 51)
    s.droehnen([(hz("C1"), 1.0), (hz("Gb1"), 0.35)], 0.13, periode=41.0, hell=260)
    s.flaechen([["C3", "Gb3"], ["Db3", "G3"]], 30.0, 0.03, start=6.0, hell=700)
    s.tropfen(45, 0.05)
    s.metall([12.0, 33.0, 47.0, 78.0, 92.0, 120.0], 0.07)
    s.speichern("tief", hall=7.0, nass=0.8)


def gefecht():
    """Gefecht – Bosskampf und Emission: Trommeln, treibendes Ostinato, Tremolo-Flächen in d-Moll."""
    s = Stueck(120, 77)
    bpm = 96
    s.droehnen([(hz("D1"), 1.0), (hz("A1"), 0.5)], 0.09, periode=9.0)
    s.flaechen([["D3", "F3", "A3"], ["Bb2", "D3", "F3"], ["C3", "E3", "G3"], ["A2", "C#3", "E3"]], 10.0, 0.05,
               start=0.0, hell=1800, tremolo=0.6)
    s.ostinato(["D2", "D2", "D3", "D2", "F2", "D2", "E2", "C#2"], bpm, 0.11, 2.5, 117.0)
    muster = [(0, 0.8, 1.0), (3, 1.0, 0.6), (4, 0.8, 0.9), (6, 1.2, 0.5), (7, 1.2, 0.5)]
    s.rhythmus(bpm, muster, 0.22, 5.0, 115.0)
    s.metall([20.0, 45.0, 70.0, 95.0], 0.05)
    s.speichern("gefecht", hall=2.5, nass=0.4, einblenden=1.5, ausblenden=4.0, pegel=0.8)



def stadt():
    """Tote Stadt – tagsüber zwischen den Ruinen: Industriepuls, ferne Sirene, verstimmtes Klavier, Metall."""
    s = Stueck(150, 61)
    s.droehnen([(hz("F1"), 1.0), (hz("C2"), 0.5)], 0.09, periode=17.0, hell=420)
    s.flaechen([["F2", "Ab2", "C3"], ["Db3", "F3", "Ab3"], ["Eb3", "G3", "Bb3"], ["C3", "E3", "G3"]], 20.0, 0.045,
               start=3.0, hell=1200)
    # Ferne Sirene: langsames Auf und Ab, stark verhallt
    n = int(14 * SR)
    tt = np.arange(n) / SR
    f = 330 + 120 * np.sin(2 * np.pi * tt / 7.0 - np.pi / 2)
    sirene = np.sin(2 * np.pi * np.cumsum(f) / SR) * huellkurve(14, 4, 5)
    sirene = bandpass(sirene, 250, 900)
    s.dazu(35.0, sirene, 0.03)
    s.dazu(108.0, sirene, 0.025)
    # Industriepuls: dumpfes Pochen wie eine kaputte Maschine
    n = int(0.4 * SR)
    tt = np.arange(n) / SR
    poch = tiefpass(np.sin(2 * np.pi * 62 * tt) * np.exp(-tt * 10) + 0.3 * s.rng.normal(0, 1, n) * np.exp(-tt * 30), 400)
    t0 = 12.0
    while t0 < 140:
        s.dazu(t0, poch, 0.13)
        s.dazu(t0 + 0.9, poch, 0.07)
        t0 += 2.4
    verstimmt = [("C5", 0.0), ("Db5", 1.8), ("Ab4", 3.6), ("F4", 6.0)]
    s.melodie([(28.0, verstimmt), (64.0, verstimmt), (98.0, verstimmt[:3]), (126.0, verstimmt)], "klavier", 0.09)
    s.metall([20.0, 50.0, 83.0, 117.0, 140.0], 0.07)
    s.wind(0.016)
    s.speichern("stadt", hall=5.0, nass=0.65)


def wildnis():
    """Weites Land – draußen in der Natur: ruhige Gitarre in e-Moll, Luft, ein Hauch Hoffnung, viel Raum."""
    s = Stueck(160, 83)
    s.droehnen([(hz("E1"), 0.6), (hz("B1"), 0.4)], 0.05, periode=43.0, hell=500)
    s.flaechen([["E3", "G3", "B3", "E4"], ["C3", "E3", "G3", "C4"], ["G2", "B2", "D3", "G3"], ["D3", "F#3", "A3", "D4"]],
               20.0, 0.04, start=2.0, hell=2200)
    a = [("E3", 0.0), ("B3", 0.6), ("E4", 1.2), ("G4", 1.8), ("B3", 2.4), ("E4", 3.0)]
    c = [("C3", 0.0), ("G3", 0.6), ("C4", 1.2), ("E4", 1.8), ("G3", 2.4), ("C4", 3.0)]
    g = [("G2", 0.0), ("D3", 0.6), ("G3", 1.2), ("B3", 1.8), ("D3", 2.4), ("G3", 3.0)]
    d = [("D3", 0.0), ("A3", 0.6), ("D4", 1.2), ("F#4", 1.8), ("A3", 2.4), ("D4", 3.0)]
    einsaetze = []
    t0 = 8.0
    while t0 < 150:
        for figur in (a, a, c, c, g, g, d, d):
            if t0 < 150:
                einsaetze.append((t0, figur))
            t0 += 5.0 / 2
    s.melodie(einsaetze, "zupf", 0.13)
    lied = [("B4", 0.0), ("A4", 2.5), ("G4", 5.0), ("F#4", 7.5), ("E4", 10.0)]
    s.melodie([(48.0, lied), (100.0, lied)], "klavier", 0.1)
    s.wind(0.02)
    s.speichern("wildnis", hall=4.0, nass=0.55)


def zuflucht():
    """Lagerfeuer – in einer Zuflucht: warme Gitarrenakkorde, knisterndes Feuer, endlich etwas Ruhe."""
    s = Stueck(140, 97)
    s.flaechen([["C3", "E3", "G3", "C4"], ["A2", "C3", "E3", "A3"], ["F2", "A2", "C3", "F3"], ["G2", "B2", "D3", "G3"]],
               16.0, 0.035, start=1.0, hell=1600)
    akkorde = [["C3", "E3", "G3", "C4", "E4"], ["A2", "E3", "A3", "C4", "E4"], ["F2", "C3", "F3", "A3", "C4"],
               ["G2", "D3", "G3", "B3", "D4"]]
    t0 = 4.0
    i = 0
    while t0 < 132:
        akk = akkorde[(i // 2) % 4]
        for schlag in (0.0, 2.0):  # langsam gezupft, Ton für Ton
            for k, note in enumerate(akk):
                s.dazu(t0 + schlag + k * 0.07, s.zupf(note, 3.5, 0.995), 0.09)
        t0 += 4.0
        i += 1
    lied = [("E4", 0.0), ("G4", 1.0), ("A4", 2.0), ("G4", 3.5), ("E4", 5.0), ("D4", 6.0), ("C4", 8.0)]
    s.melodie([(36.0, lied), (68.0, lied), (100.0, lied)], "klavier", 0.1)
    # Feuerknistern
    for _ in range(900):
        st = s.rng.uniform(0, s.dauer - 1)
        n = int(0.01 * SR)
        s.dazu(st, hochpass(s.rng.normal(0, 1, n), 2000) * np.exp(-np.arange(n) / SR * 400), s.rng.uniform(0.01, 0.05))
    s.spur += tiefpass(s.rng.normal(0, 1, s.n), 300) * 0.006
    s.speichern("zuflucht", hall=2.5, nass=0.4)


def endkampf():
    """Endkampf – Bosse: schnelle Kriegstrommeln, Blechbläser-Akkordschläge, Chorflächen in c-Moll, Steigerungen."""
    s = Stueck(150, 4242)
    bpm = 132
    takt = 4 * 60.0 / bpm
    s.droehnen([(hz("C1"), 1.0), (hz("G1"), 0.6)], 0.12, periode=6.0, hell=300)
    chor = [["C3", "Eb3", "G3", "C4"], ["Ab2", "C3", "Eb3", "Ab3"], ["Bb2", "D3", "F3", "Bb3"], ["G2", "B2", "D3", "G3"]]
    s.flaechen(chor, takt * 4, 0.06, start=0.0, hell=2600)
    s.ostinato(["C2", "C2", "G2", "C2", "Eb2", "C2", "D2", "B1"], bpm, 0.14, takt * 2, 146.0)
    muster = [(0, 0.7, 1.0), (1.5, 1.0, 0.45), (2, 0.8, 0.8), (3, 1.3, 0.5), (4, 0.7, 1.0), (5.5, 1.0, 0.45),
              (6, 0.8, 0.75), (6.5, 1.3, 0.5), (7, 1.3, 0.6), (7.5, 1.3, 0.7)]
    s.rhythmus(bpm, muster, 0.26, takt * 4, 146.0)
    # Blechbläser-Schläge (kurze, helle Akkorde) auf der Eins, ab dem zweiten Durchgang
    t0 = takt * 8
    i = 0
    while t0 < 140:
        akk = chor[(i // 2) % 4]
        hoch = [n[:-1] + str(int(n[-1]) + 1) for n in akk[:3]]
        s.dazu(t0, s.flaeche(hoch, 0.55, hell=3400), 0.11)
        s.dazu(t0 + takt * 0.75, s.flaeche(hoch, 0.3, hell=3400), 0.07)
        t0 += takt
        i += 1
    # Große Schläge alle 8 Takte und Steigerungen davor
    t0 = takt * 8
    while t0 < 146:
        s.trommel(t0, tief=0.5, lautst=1.2)
        s.trommel(t0 + 0.02, tief=0.8, lautst=0.6)
        n = int(takt * 2 * SR)
        tt = np.arange(n) / SR
        anstieg = bandpass(s.rng.normal(0, 1, n), 800, 6000) * (tt / tt[-1]) ** 3
        s.dazu(t0 - takt * 2, anstieg, 0.05)
        t0 += takt * 8
    s.metall([30.0, 60.0, 90.0, 120.0], 0.06)
    s.speichern("endkampf", hall=3.0, nass=0.45, einblenden=1.0, ausblenden=4.0, pegel=0.85)


ALLE_NEU = {"stadt": stadt, "wildnis": wildnis, "zuflucht": zuflucht, "endkampf": endkampf}

ALLE = {"titel": titel, "morgen": morgen, "nebel": nebel, "nacht": nacht, "tief": tief, "gefecht": gefecht}
ALLE.update(ALLE_NEU)

if __name__ == "__main__":
    for name in (sys.argv[1:] or ALLE):
        ALLE[name]()
