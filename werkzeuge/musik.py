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

    def speichern(self, name, hall=4.5, nass=0.6, einblenden=5.0, ausblenden=8.0, pegel=0.7, verdichten=0.0):
        trocken = hochpass(self.spur, 32)
        if verdichten:
            # leise Stellen anheben, Spitzen zähmen (Hüllkurve über ~0,4 s)
            huelle = np.sqrt(tiefpass(trocken ** 2, 2.5, 1)).clip(1e-6)
            ziel = np.max(huelle)
            trocken = trocken * (huelle / ziel) ** -verdichten
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


def sektor4():
    """Sektor 4 – (früher Titelmusik, jetzt draußen auf dem Land und in der Stadt): verstimmte Spieluhr, tiefes Dröhnen, Chorhauch, Funkrauschen,
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
    s.speichern("sektor4", hall=5.0, nass=0.65)


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


def _saege(f, tt, obertoene=28):
    """Bandbegrenzte Sägezahnwelle (blechern, wie tiefe Hörner/Bässe)."""
    x = np.zeros(len(tt))
    for k in range(1, obertoene + 1):
        if f * k > 9000:
            break
        x += np.sin(2 * np.pi * f * k * tt) / k
    return x


def _braam(s, start, noten, laenge=7.0, hell=1800, lautst=1.0):
    """Schweres, verzerrtes Blech wie ein Nebelhorn der Apokalypse: Sägezähne, die sich öffnen und wieder schließen."""
    n = int(laenge * SR)
    tt = np.arange(n) / SR
    x = np.zeros(n)
    for note in noten:
        for verst in (-0.006, 0.0, 0.007):
            x += _saege(hz(note) * (1 + verst), tt + s.rng.random())
    zu = tiefpass(x, 260)
    auf = tiefpass(x, hell)
    offen = np.minimum(1, tt / 0.6) * np.exp(-tt / (laenge * 0.35))
    x = zu * (1 - offen) + auf * offen
    x = np.tanh(x * 0.5)
    s.dazu(start, x * huellkurve(laenge, 0.12, laenge * 0.6), lautst)


def _shepard(s, von, bis, lautst, tempo=-0.018, f_min=40.0, lagen=7):
    """Endlos fallender Ton (Shepard-Glissando): man hat das Gefühl, immer tiefer zu sinken."""
    i, j = int(von * SR), int(bis * SR)
    tt = np.arange(j - i) / SR
    x = np.zeros(j - i)
    for k in range(lagen):
        lage = (k + tempo * tt) % lagen
        f = f_min * 2 ** lage
        gew = np.exp(-0.5 * ((lage - lagen / 2) / (lagen / 5)) ** 2)
        x += gew * np.sin(2 * np.pi * np.cumsum(f) / SR) + 0.3 * gew * np.sin(4 * np.pi * np.cumsum(f * 1.003) / SR)
    x *= huellkurve(bis - von, 6.0, 4.0)[:len(x)]
    s.spur[i:j] += tiefpass(x, 2200) * lautst


def _stoehnen(s, start, lautst, tief=1.0):
    """Eine ferne, stöhnende Stimme der Horde: gleitender Grundton durch Vokalformanten."""
    laenge = s.rng.uniform(1.6, 4.5)
    n = int(laenge * SR)
    tt = np.arange(n) / SR
    f0 = s.rng.uniform(85, 150) * tief
    f = f0 * (1 + 0.35 * np.exp(-tt * 1.5) - 0.25 * tt / laenge) * (1 + 0.025 * np.sin(2 * np.pi * s.rng.uniform(4, 7) * tt))
    phase = 2 * np.pi * np.cumsum(f) / SR
    roh = sum(np.sin(k * phase) / k ** 0.9 for k in range(1, 26))
    roh += bandpass(s.rng.normal(0, 1, n), 300, 3000) * 0.6
    vokal = s.rng.choice([(350, 700, 2400), (600, 1000, 2500), (450, 850, 2600)])
    x = sum(bandpass(roh, v * 0.85, v * 1.15) * a for v, a in zip(vokal, (1.0, 0.6, 0.15)))
    x = np.tanh(x * 2.5) * huellkurve(laenge, laenge * 0.3, laenge * 0.5)
    s.dazu(start, tiefpass(x, 1600), lautst * s.rng.uniform(0.5, 1.0))


def _horde(s, von, bis, dichte, lautst):
    t0 = von
    while t0 < bis:
        _stoehnen(s, t0, lautst, tief=s.rng.uniform(0.8, 1.2))
        t0 += s.rng.exponential(1.0 / dichte)


def _schrei(s, start, lautst):
    """Ein Schreier in der Ferne: kreischender, abfallender Laut."""
    laenge = 2.4
    n = int(laenge * SR)
    tt = np.arange(n) / SR
    f = 1250 * np.exp(-tt * 0.45) * (1 + 0.04 * np.sin(2 * np.pi * 11 * tt))
    phase = 2 * np.pi * np.cumsum(f) / SR
    x = sum(np.sin(k * phase) / k for k in range(1, 9)) + bandpass(s.rng.normal(0, 1, n), 1500, 6000) * 0.8
    x = np.tanh(bandpass(x, 900, 4200) * 3) * huellkurve(laenge, 0.08, 1.6)
    s.dazu(start, tiefpass(x, 3500), lautst)


def _sirene(s, start, laenge, lautst):
    """Luftschutzsirene weit weg: langsam auf- und abschwellend."""
    n = int(laenge * SR)
    tt = np.arange(n) / SR
    hub = 0.5 - 0.5 * np.cos(2 * np.pi * tt / laenge)
    f = 180 + 420 * hub ** 0.7 + 6 * np.sin(2 * np.pi * 0.7 * tt)
    phase = 2 * np.pi * np.cumsum(f) / SR
    x = np.sin(phase) + 0.5 * np.sin(2 * phase) + 0.25 * np.sin(3 * phase) + 0.15 * np.sin(5 * phase)
    x = tiefpass(x, 1400) * (0.3 + 0.7 * hub) * (0.85 + 0.15 * np.sin(2 * np.pi * 0.23 * tt))
    s.dazu(start, x * huellkurve(laenge, 2.0, 3.0), lautst)


def _geiger(s, von, bis, rate, lautst):
    t0 = von
    while t0 < bis:
        n = int(0.004 * SR)
        s.dazu(t0, s.rng.normal(0, 1, n) * np.exp(-np.arange(n) / SR * 1500), lautst)
        t0 += s.rng.exponential(1.0 / rate)


def _funk(s, start, laenge, lautst):
    """Kaputter Funkkanal: Rauschen, Knacken und ein Notrufton, der abreißt."""
    n = int(laenge * SR)
    tt = np.arange(n) / SR
    rausch = bandpass(s.rng.normal(0, 1, n), 600, 3400) * (0.6 + 0.4 * (s.rng.random(n) > 0.997))
    ton = np.sin(2 * np.pi * 1020 * tt) * ((tt % 0.9) < 0.45) * (tt < laenge * 0.6)
    x = (rausch * 0.7 + ton * 0.35) * huellkurve(laenge, 0.05, 0.4)
    s.dazu(start, x, lautst)


def titel_lobby():
    """Titelmusik (Lobby): ultimativ bedrohlich. Ein endlos fallender Ton, schweres verzerrtes Blech wie Nebelhörner,
    eine Luftschutzsirene weit weg und – immer näher – das Stöhnen einer riesigen Horde. Ein Herzschlag, der schneller
    wird, Kriegstrommeln, ein Schreier in der Ferne. Dann bricht alles ab: nur Rauschen, ein Geigerzähler und ein
    Funkspruch, der nicht mehr durchkommt. Läuft nahtlos in Schleife."""
    s = Stueck(180, 6660)
    bpm = 60
    takt = 4.0
    # Fundament: Subbass mit Reibung (D gegen Es), schwillt langsam
    s.droehnen([(hz("D1"), 1.0), (hz("Eb1"), 0.55), (hz("A1"), 0.35)], 0.16, periode=24.0, hell=200)
    s.wind(0.035)
    # Teil 1 (0–40 s): Stille vor dem Sturm – Sirene weit weg, tiefe Glocke, die Totengeläut schlägt
    _sirene(s, 3.0, 26.0, 0.05)
    for t in np.arange(8.0, 150.0, 8.0):
        s.dazu(t, s.klavier("D1", 7.0) + 0.6 * s.klavier("D2", 7.0) + 0.3 * s.klavier("Ab2", 7.0), 0.11)
    s.metall([18.0, 36.0], 0.05)
    _horde(s, 22.0, 60.0, 0.6, 0.018)
    # Teil 2 (32–120 s): das Blech, der fallende Ton, die Horde kommt näher
    for i, t in enumerate(np.arange(32.0, 120.0, 16.0)):
        noten = ["D1", "D2", "A2", "Eb3"] if i % 2 == 0 else ["Bb0", "Bb1", "F2", "B2"]
        _braam(s, t, noten, laenge=9.0, hell=900 + i * 250, lautst=0.22 + i * 0.03)
    _shepard(s, 40.0, 146.0, 0.05)
    _horde(s, 60.0, 100.0, 2.0, 0.03)
    _horde(s, 100.0, 144.0, 6.0, 0.045)
    streicher = [["D3", "Eb3", "A3"], ["C#3", "D3", "G#3"], ["D3", "Eb3", "Bb3"], ["C3", "C#3", "G3"]]
    i = 0
    for t in np.arange(56.0, 144.0, 8.0):
        s.dazu(t, s.flaeche(streicher[i % 4], 10.0, hell=2600, tremolo=0.7), 0.04 + 0.002 * i)
        i += 1
    _sirene(s, 64.0, 30.0, 0.07)
    # Herzschlag, der immer schneller wird
    t = 70.0
    while t < 144.0:
        n = int(0.35 * SR)
        tt = np.arange(n) / SR
        schlag = np.sin(2 * np.pi * (46 + 30 * np.exp(-tt * 30)) * tt) * np.exp(-tt * 14)
        st = 0.06 + 0.06 * (t - 70) / 74
        s.dazu(t, schlag, st)
        s.dazu(t + 0.26, schlag, st * 0.6)
        t += 60.0 / (52 + 70 * ((t - 70) / 74) ** 1.5)
    # Teil 3 (96–144 s): Kriegstrommeln, Sirene ganz nah, Schreier, alles türmt sich
    muster = [(0, 0.45, 1.0), (3, 0.5, 0.5), (4, 0.45, 0.8), (6, 0.6, 0.45), (7, 0.6, 0.6)]
    s.rhythmus(bpm, muster, 0.22, 96.0, 112.0)
    s.rhythmus(bpm * 1.5, muster, 0.28, 112.0, 144.0)
    _schlaege(s, bpm, 112.0, 144.0, alle=2, lautst=1.0)
    _sirene(s, 110.0, 34.0, 0.10)
    for t in (88.0, 118.0, 131.0, 139.0):
        _schrei(s, t, 0.05)
    for t, noten in ((128.0, ["D1", "D2", "A2", "Eb3", "A3"]), (136.0, ["Bb0", "Bb1", "E2", "B2", "F3"])):
        _braam(s, t, noten, laenge=8.0, hell=2600, lautst=0.42)
    s.ostinato(["D2", "Eb2", "D2", "Eb2", "D2", "A1", "D2", "Bb1"], bpm * 2, 0.10, 112.0, 144.0)
    # Abbruch (144 s): ein letzter Schlag, dann Leere
    _braam(s, 144.0, ["D1", "D1", "A1", "D2", "Eb2"], laenge=12.0, hell=3200, lautst=0.6)
    s.trommel(144.0, tief=0.35, lautst=1.6)
    s.metall([144.2], 0.12)
    # Teil 4 (148–180 s): Nachhall – Geigerzähler, Funk, vereinzeltes Stöhnen, zurück zum Anfang
    _geiger(s, 146.0, 176.0, 3.5, 0.06)
    _geiger(s, 4.0, 30.0, 1.2, 0.04)
    _funk(s, 152.0, 6.0, 0.035)
    _funk(s, 166.0, 4.0, 0.03)
    _horde(s, 150.0, 176.0, 0.4, 0.02)
    s.speichern("titel", hall=6.5, nass=0.75, einblenden=3.0, ausblenden=6.0, pegel=0.8, verdichten=0.45)


def _schlaege(s, bpm, von, bis, alle=8, lautst=1.2):
    """Große Schläge alle `alle` Takte mit Rauschanstieg davor."""
    takt = 4 * 60.0 / bpm
    t0 = von
    while t0 < bis:
        s.trommel(t0, tief=0.5, lautst=lautst)
        s.trommel(t0 + 0.02, tief=0.8, lautst=lautst * 0.5)
        n = int(takt * 2 * SR)
        tt = np.arange(n) / SR
        anstieg = bandpass(s.rng.normal(0, 1, n), 800, 6000) * (tt / tt[-1]) ** 3
        s.dazu(t0 - takt * 2, anstieg, 0.05)
        t0 += takt * alle


def _snare(s, start, lautst):
    n = int(0.4 * SR)
    tt = np.arange(n) / SR
    x = bandpass(s.rng.normal(0, 1, n), 1500, 7000) * np.exp(-tt * 22)
    x += 0.6 * np.sin(2 * np.pi * 190 * tt) * np.exp(-tt * 30)
    s.dazu(start, x, lautst)


def boss_frost():
    """Bosse der Kälte und des Nebels (Erfrorene, Nebelmutter): Eisglocken, Streichertremolo, schwere langsame Schläge."""
    s = Stueck(150, 5151)
    bpm = 100
    takt = 4 * 60.0 / bpm
    s.droehnen([(hz("D1"), 1.0), (hz("A1"), 0.5)], 0.12, periode=8.0, hell=260)
    akk = [["D3", "F3", "A3", "D4"], ["Bb2", "D3", "F3", "Bb3"], ["G2", "Bb2", "D3", "G3"], ["A2", "C#3", "E3", "A3"]]
    s.flaechen(akk, takt * 4, 0.07, start=0.0, hell=3200, tremolo=0.55)
    motiv = [("D5", 0.0), ("F5", 0.6), ("A5", 1.2), ("E5", 1.8), ("D5", 3.0), ("C#5", 3.6)]
    s.melodie([(t, motiv) for t in np.arange(takt * 4, 140, takt * 4)], "glocke", 0.05)
    muster = [(0, 0.6, 1.0), (3, 0.9, 0.5), (4, 0.6, 0.9), (6, 1.2, 0.4), (7, 1.2, 0.55)]
    s.rhythmus(bpm, muster, 0.24, takt * 4, 146.0)
    s.ostinato(["D2", "D2", "A2", "D2", "F2", "D2", "E2", "C#2"], bpm, 0.10, takt * 8, 146.0)
    _schlaege(s, bpm, takt * 8, 146)
    s.wind(0.04)
    s.speichern("boss_frost", hall=4.0, nass=0.5, einblenden=1.0, ausblenden=4.0, pegel=0.85)


def boss_fleisch():
    """Bosse aus Fleisch und Fäulnis (Schlächter, Verschmolzener, Brutkönigin, Schlund, Pilzvater): Industrie-Schläge,
    phrygisches Ostinato, Herzschlag, schräge Akkorde."""
    s = Stueck(150, 6262)
    bpm = 140
    takt = 4 * 60.0 / bpm
    s.droehnen([(hz("E1"), 1.0), (hz("F1"), 0.35)], 0.14, periode=4.0, hell=220)
    akk = [["E2", "B2", "F3"], ["E2", "Bb2", "F3"], ["E2", "G2", "C3"], ["E2", "B2", "F#3"]]
    s.flaechen(akk, takt * 4, 0.07, start=0.0, hell=1800)
    s.ostinato(["E2", "E2", "F2", "E2", "G2", "E2", "F2", "D2"], bpm, 0.17, takt * 2, 146.0)
    muster = [(0, 0.6, 1.0), (1, 1.0, 0.4), (2, 0.7, 0.8), (3, 1.4, 0.55), (4, 0.6, 1.0), (4.5, 0.6, 0.6),
              (6, 0.7, 0.8), (7, 1.4, 0.6), (7.5, 1.4, 0.7)]
    s.rhythmus(bpm, muster, 0.27, takt * 4, 146.0)
    s.metall(list(np.arange(takt * 8, 140, takt * 2)), 0.05)
    s.herzschlag(70, 0.08, von=takt * 4, bis=146.0)
    _schlaege(s, bpm, takt * 8, 146, lautst=1.3)
    s.speichern("boss_fleisch", hall=2.6, nass=0.4, einblenden=1.0, ausblenden=4.0, pegel=0.85)


def boss_marsch():
    """Bosse mit Befehlsgewalt (Kommandant, Strahlender, Hirte): Militärmarsch mit kleiner Trommel, tiefes Blech in g-Moll."""
    s = Stueck(150, 7373)
    bpm = 116
    takt = 4 * 60.0 / bpm
    s.droehnen([(hz("G1"), 1.0), (hz("D2"), 0.4)], 0.11, periode=6.0, hell=280)
    akk = [["G2", "Bb2", "D3", "G3"], ["Eb2", "G2", "Bb2", "Eb3"], ["C2", "Eb2", "G2", "C3"], ["D2", "F#2", "A2", "D3"]]
    s.flaechen(akk, takt * 4, 0.08, start=0.0, hell=1300)
    achtel = takt / 8
    t0 = takt * 2
    while t0 < 146:
        for a, st in ((0, 1.0), (2, 0.5), (3, 0.35), (4, 0.9), (6, 0.5), (6.5, 0.35), (7, 0.6), (7.5, 0.7)):
            _snare(s, t0 + a * achtel, 0.10 * st)
        t0 += takt
    s.rhythmus(bpm, [(0, 0.6, 1.0), (4, 0.6, 0.9), (6, 0.8, 0.5)], 0.26, takt * 4, 146.0)
    s.ostinato(["G2", "G2", "D2", "G2", "Bb2", "A2", "G2", "F#2"], bpm, 0.13, takt * 4, 146.0)
    t0 = takt * 8
    i = 0
    while t0 < 140:
        a = akk[(i // 2) % 4]
        hoch = [n[:-1] + str(int(n[-1]) + 1) for n in a[:3]]
        s.dazu(t0, s.flaeche(hoch, 0.6, hell=2600), 0.10)
        t0 += takt * 2
        i += 1
    _schlaege(s, bpm, takt * 8, 146)
    s.speichern("boss_marsch", hall=3.0, nass=0.45, einblenden=1.0, ausblenden=4.0, pegel=0.85)


# ---- Zweite Reihe (19.10.): bedrohlicher, im Geist der Lobby-Musik ----------------------------------------------
def _spieluhr(note, laenge=3.5, verstimmt=0.006):
    n = int(laenge * SR)
    tt = np.arange(n) / SR
    f = hz(note) * (1 + verstimmt * np.sin(2 * np.pi * 0.3 * tt))
    x = np.zeros(n)
    for r, a in ((1.0, 1.0), (3.0, 0.25), (5.1, 0.12)):
        x += a * np.sin(2 * np.pi * np.cumsum(f * r) / SR) * np.exp(-tt * (2.2 + r * 0.6))
    return x * np.minimum(1, tt * 900)


def _fluestern(s, start, lautst):
    laenge = s.rng.uniform(1.2, 2.6)
    n = int(laenge * SR)
    tt = np.arange(n) / SR
    roh = s.rng.normal(0, 1, n)
    v = [(700, 1200), (500, 900), (800, 1500)][int(s.rng.integers(3))]
    x = bandpass(roh, v[0] * 0.8, v[0] * 1.2) + 0.7 * bandpass(roh, v[1] * 0.85, v[1] * 1.15) + 0.5 * bandpass(roh, 3500, 7000)
    silben = 0.5 + 0.5 * np.sin(2 * np.pi * s.rng.uniform(3.5, 6.0) * tt + s.rng.random() * 6)
    s.dazu(start, x * silben ** 2 * huellkurve(laenge, 0.3, 0.6), lautst)


def _kratzen(s, start, lautst):
    laenge = s.rng.uniform(2.0, 3.5)
    n = int(laenge * SR)
    tt = np.arange(n) / SR
    f = s.rng.uniform(380, 520) * (1 - 0.35 * tt / laenge)
    x = np.sin(2 * np.pi * np.cumsum(f) / SR) * (0.6 + 0.4 * s.rng.random(n))
    x += bandpass(s.rng.normal(0, 1, n), 1500, 4500) * 0.5
    s.dazu(start, tiefpass(x, 3000) * huellkurve(laenge, 0.4, 1.0), lautst)


def _klopfen(s, start, lautst, anzahl=3, abstand=0.45):
    n = int(0.5 * SR)
    tt = np.arange(n) / SR
    x = (np.sin(2 * np.pi * 880 * tt) + 0.6 * np.sin(2 * np.pi * 1390 * tt) + 0.3 * np.sin(2 * np.pi * 2210 * tt)) * np.exp(-tt * 18)
    x += s.rng.normal(0, 1, n) * np.exp(-tt * 120) * 0.3
    for i in range(anzahl):
        s.dazu(start + i * abstand + s.rng.normal(0, 0.02), x, lautst * s.rng.uniform(0.7, 1.0))


def _feuer(s, dichte=900):
    for _ in range(dichte):
        st = s.rng.uniform(0, s.dauer - 1)
        n = int(0.01 * SR)
        s.dazu(st, hochpass(s.rng.normal(0, 1, n), 2000) * np.exp(-np.arange(n) / SR * 400), s.rng.uniform(0.01, 0.05))
    s.spur += tiefpass(s.rng.normal(0, 1, s.n), 300) * 0.006


def _herz(s, von, bis, bpm_von, bpm_bis, lautst, tief=44):
    """Herzschlag, der langsam schneller wird."""
    n = int(0.35 * SR)
    tt = np.arange(n) / SR
    schlag = np.sin(2 * np.pi * (tief + 30 * np.exp(-tt * 30)) * tt) * np.exp(-tt * 14)
    t = von
    while t < bis:
        anteil = (t - von) / (bis - von)
        s.dazu(t, schlag, lautst * (0.7 + 0.3 * anteil))
        s.dazu(t + 0.27, schlag, lautst * 0.6 * (0.7 + 0.3 * anteil))
        t += 60.0 / (bpm_von + (bpm_bis - bpm_von) * anteil)


def _alarm(s, start, lautst, wdh=4):
    """Alarm der Station: zwei Töne im Wechsel, blechern und verhallt."""
    n = int(0.45 * SR)
    tt = np.arange(n) / SR
    for i in range(wdh):
        f = 880 if i % 2 == 0 else 622
        x = (np.sign(np.sin(2 * np.pi * f * tt)) * 0.5 + np.sin(2 * np.pi * f * tt)) * huellkurve(0.45, 0.01, 0.1)
        s.dazu(start + i * 0.55, bandpass(x, 400, 2500), lautst)


def _grollen(s, lautst, grenze=120):
    x = tiefpass(s.rng.normal(0, 1, s.n), grenze, 4)
    lfo = 0.4 + 0.6 * (0.5 + 0.5 * np.sin(2 * np.pi * s.t / 13.0)) * (0.5 + 0.5 * np.sin(2 * np.pi * s.t / 7.3 + 1))
    s.spur += x / (np.max(np.abs(x)) + 1e-9) * lfo * lautst


def nacht():
    """Nachtwache – Nacht: Etwas ist da draußen. Subbass mit Reibung, endlos fallender Ton, schweres Blech, die Horde
    stöhnt im Dunkeln, ein Herzschlag wird schneller, Streicher kratzen im Tremolo. Kein Trost, keine Melodie."""
    s = Stueck(180, 1313)
    s.droehnen([(hz("C1"), 1.0), (hz("Db1"), 0.6), (hz("G1"), 0.3)], 0.15, periode=22.0, hell=200)
    _shepard(s, 6.0, 172.0, 0.04, tempo=-0.014)
    _herz(s, 10.0, 170.0, 48, 78, 0.075)
    for i, t in enumerate(np.arange(18.0, 172.0, 19.0)):
        noten = ["C1", "C2", "G2", "Db3"] if i % 2 == 0 else ["Ab0", "Ab1", "Eb2", "A2"]
        _braam(s, t, noten, laenge=10.0, hell=700 + i * 120, lautst=0.16 + i * 0.012)
    cluster = [["C3", "Db3", "G3"], ["B2", "C3", "F#3"], ["C3", "Db3", "Ab3"], ["Bb2", "B2", "F3"]]
    for i, t in enumerate(np.arange(30.0, 170.0, 9.0)):
        s.dazu(t, s.flaeche(cluster[i % 4], 11.0, hell=2600, tremolo=0.75), 0.035)
    _horde(s, 25.0, 90.0, 0.7, 0.016)
    _horde(s, 90.0, 172.0, 2.0, 0.024)
    for t in (64.0, 117.0, 151.0):
        _schrei(s, t, 0.03)
    for t in np.arange(12.0, 172.0, 12.0):
        s.dazu(t, s.klavier("C1", 8.0) + 0.6 * s.klavier("C2", 8.0) + 0.35 * s.klavier("Gb2", 8.0), 0.09)
    s.metall([40.0, 101.0, 160.0], 0.06)
    _funk(s, 80.0, 5.0, 0.02)
    s.wind(0.03)
    s.speichern("nacht", hall=6.5, nass=0.75, einblenden=6.0, ausblenden=8.0, pegel=0.8, verdichten=0.4)


def jagd():
    """Jagd – Nacht: Du wirst verfolgt. Dumpfer Puls in d-Moll, Herzschlag, die Horde kommt näher, Blech, Trommeln."""
    s = Stueck(170, 1704)
    s.droehnen([(hz("D1"), 1.0), (hz("Eb1"), 0.55)], 0.14, periode=18.0, hell=210)
    _herz(s, 8.0, 165.0, 56, 92, 0.07)
    s.ostinato(["D2", "D2", "Eb2", "D2", "D2", "A1", "D2", "Bb1"], 76, 0.07, 24.0, 165.0)
    _horde(s, 30.0, 100.0, 1.0, 0.016)
    _horde(s, 100.0, 166.0, 3.0, 0.026)
    cluster = [["D3", "Eb3", "A3"], ["C#3", "D3", "G#3"]]
    for i, t in enumerate(np.arange(20.0, 162.0, 10.0)):
        s.dazu(t, s.flaeche(cluster[i % 2], 11.0, hell=2400, tremolo=0.7), 0.035)
    for i, t in enumerate(np.arange(36.0, 166.0, 16.0)):
        _braam(s, t, ["D1", "D2", "A2", "Eb3"] if i % 2 == 0 else ["Bb0", "Bb1", "F2", "B2"], laenge=9.0,
               hell=800 + i * 150, lautst=0.16 + i * 0.015)
    s.rhythmus(60, [(0, 0.45, 1.0), (3, 0.5, 0.5), (4, 0.45, 0.8), (6, 0.6, 0.45)], 0.12, 96.0, 166.0)
    for t in (72.0, 128.0):
        _schrei(s, t, 0.03)
    _sirene(s, 50.0, 26.0, 0.04)
    _geiger(s, 140.0, 166.0, 3.0, 0.03)
    s.wind(0.025)
    s.speichern("jagd", hall=6.0, nass=0.7, einblenden=5.0, ausblenden=8.0, pegel=0.8, verdichten=0.4)


def stadt():
    """Tote Stadt – Ruinen: eine kaputte Maschine pocht, Blech dröhnt durch die Häuserschluchten, Sirenen heulen,
    die Horde stöhnt in den Straßen, jemand klopft auf Rohre."""
    s = Stueck(170, 61)
    s.droehnen([(hz("F1"), 1.0), (hz("Gb1"), 0.5), (hz("C2"), 0.3)], 0.13, periode=17.0, hell=260)
    n = int(0.4 * SR)
    tt = np.arange(n) / SR
    poch = tiefpass(np.sin(2 * np.pi * 58 * tt) * np.exp(-tt * 9) + 0.35 * s.rng.normal(0, 1, n) * np.exp(-tt * 30), 400)
    t0 = 8.0
    while t0 < 165:
        s.dazu(t0, poch, 0.16)
        s.dazu(t0 + 0.75, poch, 0.08)
        t0 += 2.0
    _shepard(s, 20.0, 160.0, 0.03, tempo=-0.016)
    for i, t in enumerate(np.arange(24.0, 166.0, 20.0)):
        _braam(s, t, ["F1", "F2", "C3", "Gb3"] if i % 2 == 0 else ["Db1", "Db2", "Ab2", "D3"], laenge=10.0,
               hell=800 + i * 140, lautst=0.15 + i * 0.012)
    cluster = [["F3", "Gb3", "C4"], ["E3", "F3", "B3"]]
    for i, t in enumerate(np.arange(30.0, 166.0, 10.0)):
        s.dazu(t, s.flaeche(cluster[i % 2], 11.0, hell=2600, tremolo=0.7), 0.03)
    _horde(s, 30.0, 100.0, 0.8, 0.018)
    _horde(s, 100.0, 166.0, 2.5, 0.026)
    _sirene(s, 40.0, 28.0, 0.06)
    _sirene(s, 120.0, 30.0, 0.07)
    for t in (22.0, 58.0, 97.0, 140.0):
        _klopfen(s, t, 0.06)
    s.metall([15.0, 50.0, 83.0, 117.0, 150.0], 0.08)
    for t in (76.0, 133.0):
        _schrei(s, t, 0.03)
    s.wind(0.02)
    s.speichern("stadt", hall=6.0, nass=0.7, pegel=0.8, verdichten=0.4)


def betonwueste():
    """Betonwüste – Stadt: Industrie-Ostinato, Kriegstrommeln weit weg, eine verstimmte Spieluhr über allem, Funk, Blech."""
    s = Stueck(170, 1705)
    s.droehnen([(hz("E1"), 1.0), (hz("F1"), 0.5), (hz("Bb1"), 0.3)], 0.12, periode=19.0, hell=260)
    s.ostinato(["E2", "E2", "F2", "E2", "E2", "Bb1", "E2", "C2"], 84, 0.08, 16.0, 165.0)
    s.rhythmus(56, [(0, 0.4, 1.0), (2.5, 0.5, 0.4), (4, 0.4, 0.8), (6.5, 0.55, 0.5)], 0.14, 40.0, 165.0)
    for i, t in enumerate(np.arange(28.0, 166.0, 18.0)):
        _braam(s, t, ["E1", "E2", "B2", "F3"], laenge=9.0, hell=900 + i * 150, lautst=0.15 + i * 0.012)
    motiv = ["E6", "F6", "C6", "A5", "B5", "C6", "A5"]
    for t0, verst in ((20.0, 0.01), (74.0, 0.016), (126.0, 0.025)):
        for i, note in enumerate(motiv):
            s.dazu(t0 + i * 0.75, _spieluhr(note, verstimmt=verst), 0.05)
    _horde(s, 50.0, 166.0, 1.5, 0.02)
    for t in (44.0, 99.0, 151.0):
        _funk(s, t, 4.0, 0.025)
    for t in (35.0, 88.0, 137.0):
        _klopfen(s, t, 0.06, anzahl=4, abstand=0.35)
    s.metall([12.0, 60.0, 112.0, 160.0], 0.07)
    _schrei(s, 118.0, 0.03)
    s.wind(0.02)
    s.speichern("betonwueste", hall=5.5, nass=0.65, pegel=0.8, verdichten=0.4)


def asche():
    """Asche – Land: gezupfte Gitarre in e-Phrygisch über dunklem Dröhnen, ferne Totenglocke, Wind über totem Gras."""
    s = Stueck(170, 1701)
    s.droehnen([(hz("E1"), 0.9), (hz("F1"), 0.35), (hz("B1"), 0.3)], 0.08, periode=29.0, hell=300)
    s.flaechen([["E3", "G3", "B3"], ["F3", "A3", "C4"], ["E3", "G3", "B3"], ["D3", "F3", "A3"]], 20.0, 0.035, start=2.0, hell=1400)
    figuren = [[("E3", 0.0), ("B3", 0.8), ("G3", 1.6), ("E4", 2.4), ("B3", 3.4)],
               [("F3", 0.0), ("C4", 0.8), ("A3", 1.6), ("F4", 2.4), ("C4", 3.4)],
               [("E3", 0.0), ("B3", 0.8), ("G3", 1.6), ("E4", 2.4), ("B3", 3.4)],
               [("D3", 0.0), ("A3", 0.8), ("F3", 1.6), ("D4", 2.4), ("A3", 3.4)]]
    s.melodie([(8.0 + 5.0 * i, figuren[(i // 2) % 4]) for i in range(29)], "zupf", 0.12)
    for i, t in enumerate(np.arange(16.0, 160.0, 22.0)):
        s.dazu(t, s.glocke(["E3", "B2"][i % 2], 9.0), 0.04)
    for t in (60.0, 125.0):
        _braam(s, t, ["E1", "E2", "B2"], laenge=9.0, hell=600, lautst=0.1)
    _horde(s, 80.0, 160.0, 0.3, 0.01)
    s.wind(0.03)
    s.speichern("asche", hall=5.5, nass=0.65, verdichten=0.35)


def sperrzone():
    """Sperrzone – Land: Dröhnen mit Reibung, langsam fallender Ton, Geigerzähler, Funkfetzen, Sirene."""
    s = Stueck(160, 1702)
    s.droehnen([(hz("C#1"), 1.0), (hz("D1"), 0.5), (hz("G#1"), 0.3)], 0.13, periode=23.0, hell=240)
    _shepard(s, 10.0, 152.0, 0.03, tempo=-0.013)
    _geiger(s, 5.0, 155.0, 1.5, 0.03)
    _geiger(s, 70.0, 82.0, 9.0, 0.04)
    for t in np.arange(16.0, 150.0, 14.0):
        s.dazu(t, s.klavier("C#1", 8.0) + 0.7 * s.klavier("D2", 8.0) + 0.4 * s.klavier("G#2", 8.0), 0.08)
    s.flaechen([["C#3", "G#3", "D4"], ["B2", "F#3", "C4"]], 20.0, 0.03, start=6.0, hell=1200, tremolo=0.5)
    for i, t in enumerate((45.0, 95.0, 135.0)):
        _braam(s, t, ["C#1", "C#2", "G#2", "D3"], laenge=9.0, hell=800 + i * 300, lautst=0.13)
    _sirene(s, 60.0, 26.0, 0.05)
    _funk(s, 30.0, 5.0, 0.025)
    _funk(s, 112.0, 4.0, 0.022)
    s.metall([30.0, 95.0, 140.0], 0.05)
    s.wind(0.03)
    s.speichern("sperrzone", hall=6.0, nass=0.7, pegel=0.8, verdichten=0.4)


def verlassen():
    """Verlassen – Land: einsames Klavier in a-Moll über Streichern und Dröhnen; am Ende eine verstimmte Spieluhr."""
    s = Stueck(165, 1703)
    s.droehnen([(hz("A1"), 0.8), (hz("Bb1"), 0.3), (hz("E2"), 0.3)], 0.07, periode=27.0, hell=350)
    s.flaechen([["A2", "C3", "E3", "A3"], ["F2", "A2", "C3", "F3"], ["D3", "F3", "A3"], ["E2", "G#2", "B2", "E3"]],
               14.0, 0.04, start=4.0, hell=1300)
    eins = [("A4", 0.0), ("C5", 1.2), ("B4", 2.4), ("E4", 3.6), ("A4", 6.0), ("G#4", 7.2), ("F4", 8.4), ("E4", 9.6)]
    zwei = [("F4", 0.0), ("A4", 1.2), ("G#4", 2.4), ("D4", 3.6), ("C4", 6.0), ("B3", 7.2), ("C4", 8.4), ("A3", 9.6)]
    s.melodie([(t, eins if i % 2 == 0 else zwei) for i, t in enumerate(np.arange(12.0, 128.0, 14.0))], "klavier", 0.1)
    for i, t in enumerate(np.arange(4.0, 134.0, 7.0)):
        s.dazu(t, s.klavier(["A1", "F1", "D2", "E1"][i % 4], 6.0), 0.07)
    _herz(s, 60.0, 130.0, 46, 54, 0.05)
    t0 = 136.0
    for i, note in enumerate(["A5", "C6", "E6", "B5", "A5", "E5", "G#5", "A5"]):
        s.dazu(t0, _spieluhr(note, verstimmt=0.016), 0.06)
        t0 += 0.6 + i * 0.12
    s.wind(0.02)
    s.speichern("verlassen", hall=5.0, nass=0.6, verdichten=0.35)


def schacht():
    """Schacht – unter Tage: tiefstes Dröhnen, Tropfen, schabendes Metall, Maschinen, ein Stöhnen im Dunkeln."""
    s = Stueck(160, 1706)
    s.droehnen([(hz("B0"), 1.0), (hz("C1"), 0.6), (hz("F1"), 0.3)], 0.15, periode=29.0, hell=220)
    s.tropfen(60, 0.05)
    for t in (25.0, 70.0, 104.0, 138.0):
        _kratzen(s, t, 0.04)
    for t in np.arange(15.0, 150.0, 4.5):
        s.trommel(t, tief=0.4, lautst=0.2)
    _shepard(s, 30.0, 150.0, 0.025, tempo=-0.012)
    for i, t in enumerate((40.0, 84.0, 128.0)):
        _braam(s, t, ["B0", "B1", "F2", "C3"], laenge=10.0, hell=600 + i * 200, lautst=0.14)
    s.metall([20.0, 62.0, 110.0, 150.0], 0.07)
    for t in (55.0, 96.0, 143.0):
        _stoehnen(s, t, 0.02, tief=0.8)
    s.speichern("schacht", hall=7.0, nass=0.8, pegel=0.8, verdichten=0.4)


def glut():
    """Glut – Zuflucht: Fingerpicking in d-Moll, warmes Feuer, eine leise Melodie. Kurz durchatmen."""
    s = Stueck(150, 1707)
    akk = {"Dm": ["D3", "A3", "D4", "F4", "A3", "D4"], "F": ["F2", "C3", "F3", "A3", "C3", "F3"],
           "C": ["C3", "G3", "C4", "E4", "G3", "C4"], "Bb": ["Bb2", "F3", "Bb3", "D4", "F3", "Bb3"]}
    folge = ["Dm", "Dm", "F", "F", "C", "C", "Bb", "Bb"]
    t0, i = 3.0, 0
    while t0 < 140.0:
        for k, note in enumerate(akk[folge[i % 8]]):
            s.dazu(t0 + k * 0.5 + s.rng.normal(0, 0.01), s.zupf(note, 3.0, 0.995), 0.11)
        t0 += 3.0
        i += 1
    s.flaechen([["D3", "F3", "A3"], ["F3", "A3", "C4"], ["C3", "E3", "G3"], ["Bb2", "D3", "F3"]], 6.0, 0.025, start=3.0, hell=1400)
    eins = [("A4", 0.0), ("F4", 1.5), ("G4", 3.0), ("A4", 4.5), ("C5", 6.0), ("A4", 9.0)]
    zwei = [("D5", 0.0), ("C5", 1.5), ("A4", 3.0), ("G4", 4.5), ("F4", 6.0), ("D4", 9.0)]
    s.melodie([(30.0, eins), (54.0, zwei), (78.0, eins), (102.0, zwei)], "klavier", 0.09)
    _feuer(s)
    s.speichern("glut", hall=2.5, nass=0.4)


def nebelstimmen():
    """Nebelstimmen – dichter Nebel: Glasflächen, ein langsam steigender Ton, Flüstern von überall, Blech im Nebel."""
    s = Stueck(150, 1708)
    s.droehnen([(hz("E1"), 0.9), (hz("F1"), 0.5)], 0.12, periode=21.0, hell=260)
    s.flaechen([["E5", "F5", "B5"], ["D5", "Eb5", "A5"]], 20.0, 0.018, start=4.0, hell=5000, tremolo=0.4)
    _shepard(s, 15.0, 145.0, 0.03, tempo=0.012, f_min=60.0)
    for _ in range(28):
        _fluestern(s, s.rng.uniform(8, 140), s.rng.uniform(0.02, 0.04))
    for i, t in enumerate((35.0, 80.0, 120.0)):
        _braam(s, t, ["E1", "E2", "B2", "F3"], laenge=9.0, hell=700 + i * 200, lautst=0.13)
    _herz(s, 50.0, 140.0, 50, 70, 0.06)
    s.wind(0.04)
    s.speichern("nebelstimmen", hall=6.5, nass=0.8, pegel=0.8, verdichten=0.4)


def herzkammer():
    """Herzkammer – Nether: Das Herz der Zone schlägt, riesig und überall. Glut grollt, Chor-Cluster schreien,
    Blech und Kriegstrommeln, ein Ton, der endlos in die Tiefe fällt."""
    s = Stueck(180, 666)
    s.droehnen([(hz("A0"), 1.0), (hz("Bb0"), 0.7), (hz("E1"), 0.4)], 0.18, periode=12.0, hell=180)
    _grollen(s, 0.08)
    _herz(s, 4.0, 176.0, 36, 62, 0.17, tief=36)
    _shepard(s, 10.0, 176.0, 0.04, tempo=-0.02)
    chor = [["A3", "Bb3", "E4", "F4"], ["G#3", "A3", "D#4", "E4"]]
    for i, t in enumerate(np.arange(14.0, 174.0, 8.0)):
        s.dazu(t, s.flaeche(chor[i % 2], 10.0, hell=3000, tremolo=0.45), 0.04)
    for i, t in enumerate(np.arange(20.0, 176.0, 14.0)):
        _braam(s, t, ["A0", "A1", "E2", "Bb2"] if i % 2 == 0 else ["F0", "F1", "C2", "F#2"], laenge=10.0,
               hell=900 + i * 120, lautst=0.2 + i * 0.008)
    muster = [(0, 0.4, 1.0), (1.5, 0.5, 0.5), (3, 0.4, 0.8), (4, 0.4, 1.0), (5.5, 0.55, 0.5), (6, 0.5, 0.7), (7, 0.6, 0.6)]
    s.rhythmus(70, muster, 0.2, 60.0, 176.0)
    for t in (48.0, 102.0, 147.0, 168.0):
        _schrei(s, t, 0.035)
    s.speichern("herzkammer", hall=6.0, nass=0.7, pegel=0.82, verdichten=0.4)


def glutfeld():
    """Glutfeld – Nether: treibendes, verzerrtes Ostinato, Heulen über dem Lavameer, Horde, Trommeln ohne Pause."""
    s = Stueck(170, 667)
    s.droehnen([(hz("G0"), 1.0), (hz("Ab0"), 0.6)], 0.16, periode=9.0, hell=200)
    _grollen(s, 0.07)
    n = int(0.3 * SR)
    tt = np.arange(n) / SR
    t0, i = 6.0, 0
    noten = ["G1", "G1", "Ab1", "G1", "D2", "G1", "Db2", "G1"]
    while t0 < 166:
        x = np.tanh(_saege(hz(noten[i % 8]), tt) * 2.0) * np.exp(-tt * 5)
        s.dazu(t0, tiefpass(x, 900), 0.06)
        t0 += 60.0 / 100 / 2
        i += 1
    s.rhythmus(100, [(0, 0.45, 1.0), (2, 0.5, 0.6), (3, 0.45, 0.8), (4, 0.45, 1.0), (6, 0.55, 0.6), (7, 0.6, 0.7)],
               0.2, 30.0, 166.0)
    for i, t in enumerate(np.arange(24.0, 166.0, 12.0)):
        _braam(s, t, ["G0", "G1", "D2", "Ab2"], laenge=8.0, hell=1000 + i * 120, lautst=0.2)
    for t in (40.0, 90.0, 140.0):
        _sirene(s, t, 18.0, 0.05)
    _horde(s, 20.0, 166.0, 2.5, 0.022)
    for t in (70.0, 122.0, 158.0):
        _schrei(s, t, 0.035)
    s.speichern("glutfeld", hall=5.0, nass=0.6, pegel=0.82, verdichten=0.4)


def leere():
    """Leere – End/Orbitalstation: kalte Glasflächen, Alarm der Station, ein endlos steigender Ton, Flüstern der
    Hologeister, Blech aus dem Nichts, ein Herz, das schneller wird."""
    s = Stueck(180, 777)
    s.droehnen([(hz("F#1"), 1.0), (hz("G1"), 0.5), (hz("C2"), 0.3)], 0.13, periode=19.0, hell=240)
    s.flaechen([["F#5", "G5", "C6"], ["E5", "F5", "B5"]], 16.0, 0.02, start=2.0, hell=6000, tremolo=0.3)
    _shepard(s, 8.0, 176.0, 0.035, tempo=0.015, f_min=50.0)
    for t in np.arange(20.0, 170.0, 22.0):
        _alarm(s, t, 0.03)
    for t in (34.0, 90.0, 150.0):
        _funk(s, t, 4.0, 0.025)
    for i, t in enumerate(np.arange(26.0, 176.0, 18.0)):
        _braam(s, t, ["F#1", "F#2", "C#3", "G3"], laenge=10.0, hell=900 + i * 150, lautst=0.17 + i * 0.01)
    _herz(s, 60.0, 176.0, 50, 82, 0.06)
    for _ in range(18):
        _fluestern(s, s.rng.uniform(10, 170), s.rng.uniform(0.015, 0.03))
    s.metall([15.0, 70.0, 128.0], 0.07)
    s.speichern("leere", hall=7.5, nass=0.85, pegel=0.8, verdichten=0.4)


def orbit():
    """Orbit – End: pulsierendes, verstimmtes Arpeggio wie ein defekter Computer, Trommeln, Alarm, Blech."""
    s = Stueck(170, 778)
    s.droehnen([(hz("F#1"), 1.0), (hz("C2"), 0.4)], 0.12, periode=11.0, hell=240)
    n = int(0.25 * SR)
    tt = np.arange(n) / SR
    t0, i = 4.0, 0
    noten = ["F#3", "C4", "G3", "C#4", "F#3", "G4", "C4", "F4"]
    while t0 < 166:
        f = hz(noten[i % 8]) * (1 + 0.004 * np.sin(i))
        x = (np.sign(np.sin(2 * np.pi * f * tt)) * 0.4 + np.sin(2 * np.pi * f * tt)) * np.exp(-tt * 9)
        s.dazu(t0, tiefpass(x, 2500), 0.035)
        t0 += 60.0 / 112 / 2
        i += 1
    s.rhythmus(56, [(0, 0.4, 1.0), (3, 0.5, 0.5), (4, 0.4, 0.9), (6, 0.6, 0.5)], 0.18, 40.0, 166.0)
    for i, t in enumerate(np.arange(20.0, 166.0, 16.0)):
        _braam(s, t, ["F#1", "F#2", "C#3", "G3"] if i % 2 == 0 else ["D1", "D2", "A2", "D#3"], laenge=9.0,
               hell=1000 + i * 150, lautst=0.18)
    for t in np.arange(30.0, 160.0, 30.0):
        _alarm(s, t, 0.035, wdh=6)
    _shepard(s, 60.0, 166.0, 0.03, tempo=0.02, f_min=50.0)
    s.speichern("orbit", hall=6.0, nass=0.7, pegel=0.82, verdichten=0.4)



def raserei():
    """Raserei – Boss in Phase 2: alles auf einmal. Rasende Trommeln, verzerrtes Ostinato, Blech im Takt, Sirene,
    Chor-Cluster im Tremolo, Schreie. Kein Atemholen mehr."""
    s = Stueck(150, 2222)
    bpm = 152
    takt = 4 * 60.0 / bpm
    s.droehnen([(hz("C1"), 1.0), (hz("Db1"), 0.6)], 0.16, periode=4.0, hell=220)
    n = int(0.2 * SR)
    tt = np.arange(n) / SR
    noten = ["C2", "C2", "Db2", "C2", "G1", "C2", "Gb1", "C2"]
    t0, i = 0.5, 0
    while t0 < 147:
        x = np.tanh(_saege(hz(noten[i % 8]), tt) * 2.5) * np.exp(-tt * 8)
        s.dazu(t0, tiefpass(x, 1200), 0.07)
        t0 += takt / 4
        i += 1
    muster = [(0, 0.45, 1.0), (1, 0.6, 0.5), (2, 0.45, 0.9), (3, 0.7, 0.6), (4, 0.45, 1.0), (4.5, 0.6, 0.5),
              (5, 0.45, 0.8), (6, 0.7, 0.7), (6.5, 0.7, 0.6), (7, 0.8, 0.8), (7.5, 0.8, 0.9)]
    s.rhythmus(bpm, muster, 0.28, 2.0, 147.0)
    for k, t in enumerate(np.arange(4.0, 147.0, takt * 4)):
        _braam(s, t, ["C1", "C2", "G2", "Db3"] if k % 2 == 0 else ["Ab0", "Ab1", "Eb2", "A2"], laenge=takt * 4 + 1.5,
               hell=2400, lautst=0.2)
    chor = [["C4", "Db4", "G4"], ["B3", "C4", "F#4"]]
    for k, t in enumerate(np.arange(2.0, 147.0, takt * 2)):
        s.dazu(t, s.flaeche(chor[k % 2], takt * 2 + 1.0, hell=3600, tremolo=0.9), 0.035)
    _schlaege(s, bpm, takt * 8, 147.0, alle=4, lautst=1.2)
    for t in (20.0, 70.0, 118.0):
        _sirene(s, t, 16.0, 0.05)
    for t in (34.0, 61.0, 96.0, 131.0):
        _schrei(s, t, 0.04)
    s.speichern("raserei", hall=3.0, nass=0.45, einblenden=0.5, ausblenden=3.0, pegel=0.85, verdichten=0.3)



ALLE_NEU = {"stadt": stadt, "wildnis": wildnis, "zuflucht": zuflucht, "endkampf": endkampf,
            "boss_frost": boss_frost, "boss_fleisch": boss_fleisch, "boss_marsch": boss_marsch}

ALLE = {"titel": titel_lobby, "morgen": morgen, "nebel": nebel, "nacht": nacht, "tief": tief, "gefecht": gefecht}
ALLE.update(ALLE_NEU)
ALLE["sektor4"] = sektor4
ALLE.update({n: globals()[n] for n in ("jagd", "betonwueste", "asche", "sperrzone", "verlassen", "schacht", "glut",
                                       "nebelstimmen", "herzkammer", "glutfeld", "leere", "orbit", "raserei")})

if __name__ == "__main__":
    for name in (sys.argv[1:] or ALLE):
        ALLE[name]()
