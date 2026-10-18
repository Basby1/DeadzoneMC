"""Menümusik "Sektor 4": düsteres Ambient-Stück, komplett hier synthetisiert (keine fremden Aufnahmen).

  python3 werkzeuge/musik.py   – schreibt assets/deadzonemc/sounds/musik/titel.ogg

Aufbau (ca. 2:40, d-Moll): tiefer Dröhnton, langsame Flächen (Dm – B – Gm – A), ein einsames Klavier-Motiv,
ferne Metallschläge und Wind. Mit Hall, Ein- und Ausblendung.
"""
import os
import subprocess
import tempfile

import numpy as np
from scipy.io import wavfile
from scipy.signal import butter, fftconvolve, sosfilt

SR = 44100
DAUER = 160.0
WURZEL = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ZIEL = os.path.join(WURZEL, "src", "main", "resources", "assets", "deadzonemc", "sounds", "musik", "titel.ogg")

rng = np.random.default_rng(404)
N = int(SR * DAUER)
t = np.arange(N) / SR


def hz(note):
    """Notenname wie 'D3', 'Bb2', 'C#4' -> Frequenz."""
    namen = {"C": 0, "C#": 1, "Db": 1, "D": 2, "D#": 3, "Eb": 3, "E": 4, "F": 5, "F#": 6, "Gb": 6, "G": 7,
             "G#": 8, "Ab": 8, "A": 9, "A#": 10, "Bb": 10, "B": 11}
    n, okt = note[:-1], int(note[-1])
    return 440.0 * 2 ** ((namen[n] + 12 * (okt + 1) - 69) / 12)


def tiefpass(x, f, ordnung=2):
    return sosfilt(butter(ordnung, f, "low", fs=SR, output="sos"), x)


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


def einfuegen(spur, start, klang):
    i = int(start * SR)
    j = min(len(spur), i + len(klang))
    if i < len(spur):
        spur[i:j] += klang[:j - i]


# ---- Dröhnton ----------------------------------------------------------------------------------
def droehnen():
    x = np.zeros(N)
    for f0, lautst in ((hz("D1"), 1.0), (hz("A1"), 0.5), (hz("D2"), 0.35)):
        for k in range(1, 9):
            verstimmt = f0 * k * (1 + 0.0015 * np.sin(2 * np.pi * 0.03 * t + k))
            phase = 2 * np.pi * np.cumsum(verstimmt) / SR
            x += lautst / k ** 1.3 * np.sin(phase + rng.random() * 6)
    x = tiefpass(x, 380)
    welle = 0.6 + 0.4 * np.sin(2 * np.pi * t / 37.0 - 1.2)
    return x * welle * 0.11


# ---- Flächen -----------------------------------------------------------------------------------
AKKORDE = [["D3", "F3", "A3", "D4"], ["Bb2", "D3", "F3", "Bb3"], ["G2", "Bb2", "D3", "G3"], ["A2", "C#3", "E3", "A3"]]


def flaeche(noten, laenge):
    n = int(laenge * SR)
    tt = np.arange(n) / SR
    x = np.zeros(n)
    for note in noten:
        f = hz(note)
        for verst in (-0.004, 0.0, 0.005):  # Chorus
            ff = f * (1 + verst)
            for k, a in ((1, 1.0), (2, 0.35), (3, 0.15), (4, 0.06)):
                x += a * np.sin(2 * np.pi * ff * k * tt + rng.random() * 6)
    x = tiefpass(x, 1400)
    return x * huellkurve(laenge, laenge * 0.35, laenge * 0.45)


def flaechen():
    x = np.zeros(N)
    laenge = 20.0
    start = 4.0
    i = 0
    while start < DAUER - 6:
        einfuegen(x, start, flaeche(AKKORDE[i % 4], laenge + 6))
        start += laenge
        i += 1
    return x * 0.06


# ---- Klavier-Motiv -----------------------------------------------------------------------------
def klavier(note, laenge=5.0, staerke=1.0):
    n = int(laenge * SR)
    tt = np.arange(n) / SR
    f = hz(note)
    x = np.zeros(n)
    for k, a in ((1, 1.0), (2, 0.5), (3, 0.22), (4, 0.12), (5, 0.06)):
        inh = 1 + 0.0004 * k * k  # leicht verstimmte Obertöne wie bei einem alten Klavier
        x += a * np.sin(2 * np.pi * f * k * inh * tt) * np.exp(-tt * (1.1 + 0.6 * k))
    x *= np.minimum(1, tt * 400)  # weicher Anschlag
    return x * staerke


MOTIV = [("D4", 0.0), ("F4", 1.6), ("E4", 3.2), ("A3", 4.8), ("D4", 8.5), ("C4", 10.1), ("Bb3", 11.7), ("A3", 13.3)]
ANTWORT = [("A4", 0.0), ("G4", 1.6), ("F4", 3.2), ("E4", 4.8), ("D4", 6.4), ("C#4", 9.0), ("D4", 12.0)]


def melodie():
    x = np.zeros(N)
    for start, motiv, st in ((24.0, MOTIV, 0.9), (44.0, ANTWORT, 0.8), (84.0, MOTIV, 1.0), (104.0, ANTWORT, 0.85),
                             (128.0, MOTIV[:4], 0.7)):
        for note, dt in motiv:
            einfuegen(x, start + dt + rng.normal(0, 0.04), klavier(note, staerke=st * rng.uniform(0.8, 1.0)))
    return x * 0.2


# ---- Metall und Wind ---------------------------------------------------------------------------
def metallschlag(f0):
    n = int(7 * SR)
    tt = np.arange(n) / SR
    x = np.zeros(n)
    for r, a in ((1.0, 1.0), (2.76, 0.6), (5.40, 0.4), (8.93, 0.25), (13.3, 0.12)):
        x += a * np.sin(2 * np.pi * f0 * r * tt) * np.exp(-tt * (0.6 + r * 0.25))
    return tiefpass(x, 2500)


def metall():
    x = np.zeros(N)
    for start in (16.0, 51.0, 77.0, 113.0, 139.0):
        einfuegen(x, start, metallschlag(rng.uniform(70, 120)) * rng.uniform(0.6, 1.0))
    return x * 0.06


def wind():
    rauschen = rng.normal(0, 1, N)
    x = bandpass(rauschen, 180, 900)
    lfo = 0.5 + 0.5 * np.sin(2 * np.pi * t / 11.0) * np.sin(2 * np.pi * t / 29.0 + 1)
    return x * (0.2 + 0.8 * lfo) * 0.018


# ---- Hall und Ausgabe --------------------------------------------------------------------------
def hall(x, dauer=4.5, saat=0):
    r = np.random.default_rng(saat)
    n = int(dauer * SR)
    ir = r.normal(0, 1, n) * np.exp(-np.arange(n) / SR * 5.5 / dauer)
    ir = tiefpass(ir, 5000)
    ir /= np.sqrt(np.sum(ir ** 2))
    return fftconvolve(x, ir)[:len(x)]


def main():
    trocken = droehnen() + flaechen() + melodie() + metall() + wind()
    trocken = sosfilt(butter(2, 32, "high", fs=SR, output="sos"), trocken)  # Tiefstbass weg (schont Laptop-Lautsprecher)
    links = 0.55 * trocken + 0.6 * hall(trocken, saat=1)
    rechts = 0.55 * trocken + 0.6 * hall(trocken, saat=2)
    stereo = np.stack([links, rechts], axis=1)
    hulle = huellkurve(DAUER, 5.0, 8.0)[:N]
    stereo *= hulle[:, None]
    stereo /= np.max(np.abs(stereo)) / 0.7
    os.makedirs(os.path.dirname(ZIEL), exist_ok=True)
    with tempfile.TemporaryDirectory() as tmp:
        wav = os.path.join(tmp, "titel.wav")
        wavfile.write(wav, SR, (stereo * 32767).astype(np.int16))
        subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", wav, "-c:a", "libvorbis", "-q:a", "3", ZIEL],
                       check=True)
    print("Musik geschrieben:", ZIEL, os.path.getsize(ZIEL) // 1024, "KB")


if __name__ == "__main__":
    main()
