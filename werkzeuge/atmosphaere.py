"""Horror-Geräuschkulisse von Deadzone MC – komplett synthetisiert (keine fremden Aufnahmen).

  python3 werkzeuge/atmosphaere.py

Schreibt assets/deadzonemc/sounds/atmo/<name>_<n>.ogg (mono, damit sie im Raum verortet werden) und trägt
die Einträge atmo.<name> in sounds.json ein. Wann was zu hören ist, entscheidet der Client
(de.deadzonemc.client.ClientAtmosphaere): ferne Schreie und Schüsse, Sirenen, Heulen, Klopfen, Schritte hinter
einem, Flüstern im Nebel, kreischendes Metall unter Tage, Funkrauschen.
"""
import json
import os
import subprocess
import tempfile

import numpy as np
from scipy.io import wavfile
from scipy.signal import butter, fftconvolve, sosfilt

SR = 44100
WURZEL = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ASSETS = os.path.join(WURZEL, "src", "main", "resources", "assets", "deadzonemc")
ORDNER = os.path.join(ASSETS, "sounds", "atmo")


def tp(x, f):
    return sosfilt(butter(2, f, "low", fs=SR, output="sos"), x)


def hp(x, f):
    return sosfilt(butter(2, f, "high", fs=SR, output="sos"), x)


def bp(x, a, b, ordnung=2):
    return sosfilt(butter(ordnung, [a, b], "band", fs=SR, output="sos"), x)


def t_(dauer):
    return np.arange(int(dauer * SR)) / SR


def huelle(n, an, ab):
    e = np.ones(n)
    a = min(n, int(an * SR))
    r = min(n - a, int(ab * SR))
    if a:
        e[:a] = np.linspace(0, 1, a) ** 2
    if r:
        e[n - r:] = np.linspace(1, 0, r) ** 1.5
    return e


def hall(x, dauer, rng, nass=0.5, hell=3000):
    n = int(dauer * SR)
    ir = rng.normal(0, 1, n) * np.exp(-np.arange(n) / SR * (6.9 / dauer))
    ir = tp(ir, hell)
    nass_sig = fftconvolve(x, ir)[:len(x) + n]
    trocken = np.concatenate([x, np.zeros(len(nass_sig) - len(x))])
    nass_sig /= np.max(np.abs(nass_sig)) + 1e-9
    trocken /= np.max(np.abs(trocken)) + 1e-9
    return (1 - nass) * trocken + nass * nass_sig


def saege(phase):
    return 2 * (phase / (2 * np.pi) % 1) - 1


def speichern(name, x, pegel=0.8):
    x = hp(x, 40)
    x = x / (np.max(np.abs(x)) + 1e-9) * pegel
    # kurz ein- und ausblenden gegen Knacken
    x *= huelle(len(x), 0.01, 0.15)
    os.makedirs(ORDNER, exist_ok=True)
    ziel = os.path.join(ORDNER, f"{name}.ogg")
    with tempfile.TemporaryDirectory() as tmp:
        wav = os.path.join(tmp, "x.wav")
        wavfile.write(wav, SR, (x * 32767).astype(np.int16))
        subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", wav, "-ac", "1", "-c:a", "libvorbis", "-q:a", "3", ziel],
                       check=True)


# ---- Klänge -------------------------------------------------------------------------------------

def schrei(rng):
    """Ferner menschlicher Schrei: Stimmhafter Ton mit Formanten, Tonhöhe reißt hoch und bricht."""
    d = rng.uniform(1.6, 2.6)
    t = t_(d)
    f0 = 380 + 520 * np.sin(np.pi * np.clip(t / d * rng.uniform(0.9, 1.2), 0, 1)) ** 0.7
    f0 *= 1 + 0.03 * np.sin(2 * np.pi * rng.uniform(5, 8) * t) + 0.01 * rng.normal(0, 1, len(t))
    ph = np.cumsum(2 * np.pi * f0 / SR)
    roh = saege(ph) + 0.4 * rng.normal(0, 1, len(t))
    stimme = bp(roh, 600, 1100) * 1.0 + bp(roh, 1100, 1700) * 0.7 + bp(roh, 2400, 3200) * 0.35
    stimme = np.tanh(stimme * 3)
    stimme *= huelle(len(t), 0.08, d * 0.5)
    fern = tp(stimme, 2200)
    return hall(fern, 3.0, rng, nass=0.55, hell=1800)


def schuesse(rng):
    """Ferne Schüsse: einzelne, Salven oder eine Feuerstoß-Folge, mit Echo vom Gelände."""
    d = 4.5
    x = np.zeros(int(d * SR))
    art = rng.integers(0, 3)
    zeiten = ([0.2] if art == 0 else sorted(rng.uniform(0.1, 2.5, rng.integers(2, 5))) if art == 1
              else [0.3 + i * rng.uniform(0.08, 0.11) for i in range(rng.integers(6, 12))])
    for z in zeiten:
        n = int(0.25 * SR)
        k = rng.normal(0, 1, n) * np.exp(-np.arange(n) / SR * 28)
        k = tp(k, 900) + 0.6 * np.sin(2 * np.pi * 55 * np.arange(n) / SR) * np.exp(-np.arange(n) / SR * 18)
        i = int(z * SR)
        for verz, st in ((0.0, 1.0), (0.42, 0.35), (0.95, 0.18)):
            j = i + int(verz * SR)
            if j + n < len(x):
                x[j:j + n] += k * st * rng.uniform(0.8, 1.1)
    return hall(tp(x, 1500), 2.5, rng, nass=0.45, hell=1200)


def sirene(rng):
    """Luftschutzsirene, weit weg, an- und abschwellend."""
    d = 14.0
    t = t_(d)
    f = 340 + 330 * (0.5 - 0.5 * np.cos(2 * np.pi * t / 7.0))
    ph = np.cumsum(2 * np.pi * f / SR)
    x = np.sin(ph) + 0.35 * np.sin(2 * ph) + 0.15 * np.sin(3 * ph)
    x *= 0.8 + 0.2 * np.sin(2 * np.pi * 0.4 * t)
    x *= huelle(len(t), 2.5, 3.5)
    return hall(tp(x, 1600), 4.0, rng, nass=0.6, hell=1400)


def klopfen(rng):
    """Drei (oder vier) dumpfe Schläge gegen Holz – irgendwo im Haus."""
    d = 2.2
    x = np.zeros(int(d * SR))
    anzahl = rng.integers(3, 5)
    abstand = rng.uniform(0.28, 0.42)
    for k in range(anzahl):
        n = int(0.25 * SR)
        tt = np.arange(n) / SR
        schlag = (np.sin(2 * np.pi * rng.uniform(150, 190) * tt) * np.exp(-tt * 30)
                  + 0.6 * np.sin(2 * np.pi * rng.uniform(300, 360) * tt) * np.exp(-tt * 45)
                  + 0.5 * tp(rng.normal(0, 1, n), 2500) * np.exp(-tt * 120))
        i = int((0.1 + k * abstand * rng.uniform(0.9, 1.1)) * SR)
        x[i:i + n] += schlag * (1.0 if k < anzahl - 1 else 1.25)
    return hall(x, 0.9, rng, nass=0.3, hell=2500)


def schritte(rng):
    """Schritte auf Kies, die näher kommen – und dann stehen bleiben."""
    d = 3.2
    x = np.zeros(int(d * SR))
    anzahl = rng.integers(4, 7)
    for k in range(anzahl):
        n = int(0.18 * SR)
        tt = np.arange(n) / SR
        knirsch = bp(rng.normal(0, 1, n), 700, 4500) * np.exp(-tt * 22)
        knirsch *= 1 + 0.8 * (rng.random(n) > 0.97)  # einzelne Steinchen
        i = int((0.15 + k * rng.uniform(0.48, 0.56)) * SR)
        if i + n < len(x):
            x[i:i + n] += knirsch * (0.4 + 0.6 * k / anzahl)
    return hall(x, 0.6, rng, nass=0.2, hell=4000)


def fluestern(rng):
    """Unverständliches Flüstern: gefiltertes Rauschen mit Silbenrhythmus."""
    d = rng.uniform(2.2, 3.4)
    t = t_(d)
    silben = np.clip(np.sin(2 * np.pi * rng.uniform(3.5, 5.5) * t + rng.uniform(0, 6)), 0, 1) ** 2
    silben *= np.clip(np.sin(2 * np.pi * rng.uniform(0.6, 1.0) * t), 0.2, 1)
    roh = rng.normal(0, 1, len(t))
    formant = 1800 + 900 * np.sin(2 * np.pi * 2.3 * t)
    x = bp(roh, 1400, 5200) * silben
    x += 0.4 * bp(roh, 2600, 3400) * silben * (formant / 2700)
    x *= huelle(len(t), 0.3, 0.6)
    return hall(x, 2.0, rng, nass=0.5, hell=5000)


def heulen(rng):
    """Langgezogenes Heulen in der Ferne – ein Hund? Etwas anderes."""
    d = rng.uniform(3.5, 4.8)
    t = t_(d)
    kurve = np.sin(np.pi * np.clip(t / d, 0, 1)) ** 0.6
    f = 380 + 300 * kurve + 40 * np.sin(2 * np.pi * 0.7 * t)
    f *= 1 + 0.012 * np.sin(2 * np.pi * 5.5 * t)
    ph = np.cumsum(2 * np.pi * f / SR)
    x = np.sin(ph) + 0.3 * np.sin(2 * ph) + 0.1 * np.sin(3 * ph) + 0.05 * rng.normal(0, 1, len(t))
    x *= huelle(len(t), 0.6, 1.4)
    return hall(tp(x, 2000), 3.5, rng, nass=0.55, hell=1600)


def metall(rng):
    """Kreischendes, ächzendes Metall in einem Tunnel."""
    d = 3.0
    t = t_(d)
    teile = rng.uniform([180, 410, 870, 1530], [240, 470, 950, 1650])
    x = np.zeros(len(t))
    zitter = tp(rng.normal(0, 1, len(t)), 8)
    zitter /= np.max(np.abs(zitter)) + 1e-9
    for i, f in enumerate(teile):
        x += np.sin(2 * np.pi * f * t * (1 + 0.004 * zitter)) / (i + 1)
    x *= np.clip(0.5 + 0.8 * zitter, 0, 1.5)
    x += 0.25 * bp(rng.normal(0, 1, len(t)), 2000, 6000) * np.clip(zitter, 0, 1)
    x *= huelle(len(t), 0.4, 1.0)
    return hall(x, 3.0, rng, nass=0.6, hell=3500)


def funk(rng):
    """Funkrauschen, abgehackte Töne, ein verzerrter Ruf – niemand antwortet."""
    d = 4.0
    t = t_(d)
    rausch = bp(rng.normal(0, 1, len(t)), 300, 3200) * 0.35
    knack = (rng.random(len(t)) > 0.9993) * rng.normal(0, 3, len(t))
    x = rausch + tp(knack, 3000)
    for k in range(rng.integers(3, 7)):
        st = rng.uniform(0.3, d - 0.6)
        lang = rng.choice([0.08, 0.25])
        i = int(st * SR)
        n = int(lang * SR)
        x[i:i + n] += 0.5 * np.sin(2 * np.pi * 980 * np.arange(n) / SR)
    # verzerrte Stimme
    i = int(rng.uniform(1.2, 2.2) * SR)
    n = int(0.9 * SR)
    tt = np.arange(n) / SR
    ph = np.cumsum(2 * np.pi * (140 + 30 * np.sin(2 * np.pi * 3 * tt)) / SR)
    st = np.tanh(4 * bp(saege(ph), 500, 2500)) * np.clip(np.sin(2 * np.pi * 4 * tt), 0, 1)
    x[i:i + n] += 0.6 * st
    x = np.tanh(2 * x)
    return bp(x, 300, 3400) * huelle(len(t), 0.2, 0.4)


def explosion(rng):
    """Ferne Detonation: dumpfer Schlag, langes Grollen, nachrieselnde Trümmer."""
    d = 7.0
    n = int(d * SR)
    t = t_(d)
    x = tp(rng.normal(0, 1, n), 140, ) * np.exp(-t * 0.9) * 2.5
    x += np.sin(2 * np.pi * (38 + 20 * np.exp(-t * 6)) * t) * np.exp(-t * 2.2)
    x += bp(rng.normal(0, 1, n), 800, 3000) * (rng.random(n) > 0.995) * np.exp(-((t - 1.5) ** 2)) * 0.6
    return hall(np.tanh(x) * huelle(n, 0.005, 2.0), 3.5, rng, nass=0.6, hell=900)


def hubschrauber(rng):
    """Ein Hubschrauber zieht weit weg vorbei: Rotorschlag, der an- und abschwillt."""
    d = 12.0
    t = t_(d)
    rate = rng.uniform(4.5, 5.5)
    schlag = (0.5 + 0.5 * np.sin(2 * np.pi * rate * t)) ** 6
    x = tp(rng.normal(0, 1, len(t)), 500) * schlag + 0.3 * np.sin(2 * np.pi * 70 * t) * schlag
    x += bp(rng.normal(0, 1, len(t)), 2000, 5000) * 0.05
    weg = np.exp(-((t - d / 2) / (d / 4)) ** 2)
    return hall(x * weg * huelle(len(t), 1.0, 2.0), 3.0, rng, nass=0.5, hell=1500)


def horde(rng):
    """Eine riesige Horde weit weg: viele Stimmen stöhnen durcheinander."""
    d = 9.0
    n = int(d * SR)
    t = t_(d)
    x = np.zeros(n)
    for _ in range(40):
        st = rng.uniform(0, d - 2.5)
        laenge = rng.uniform(1.2, 2.5)
        m = int(laenge * SR)
        tt = np.arange(m) / SR
        f0 = rng.uniform(80, 160) * (1 + 0.3 * np.exp(-tt * 1.5) - 0.2 * tt / laenge)
        ph = 2 * np.pi * np.cumsum(f0) / SR
        v = sum(np.sin(k * ph) / k for k in range(1, 14))
        v = bp(v, 300, 1400) * huelle(m, laenge * 0.3, laenge * 0.5)
        i = int(st * SR)
        x[i:i + m] += v[:max(0, min(m, n - i))] * rng.uniform(0.4, 1.0)
    return hall(tp(np.tanh(x * 0.6), 1400) * huelle(n, 2.0, 2.5), 4.0, rng, nass=0.65, hell=1100)


def autoalarm(rng):
    """Eine Autoalarmanlage in den Ruinen – jault ein paar Mal und erstirbt."""
    d = 8.0
    t = t_(d)
    f = np.where((t % 0.5) < 0.25, 900, 600) * (1 - 0.15 * t / d)
    ph = np.cumsum(2 * np.pi * f / SR)
    x = np.sign(np.sin(ph)) * 0.4 + np.sin(ph)
    x *= huelle(len(t), 0.05, 1.5) * (t < d - 0.6)
    return hall(bp(x, 400, 2600), 3.0, rng, nass=0.55, hell=1800)


def glocke(rng):
    """Eine Kirchenglocke schlägt, unregelmäßig – als zöge jemand am Seil, der es nicht mehr kann."""
    d = 12.0
    n = int(d * SR)
    x = np.zeros(n)
    f0 = rng.uniform(180, 240)
    st = 0.2
    while st < d - 5:
        m = int(5 * SR)
        tt = np.arange(m) / SR
        k = sum(a * np.sin(2 * np.pi * f0 * r * tt) * np.exp(-tt * (0.6 + r * 0.4))
                for r, a in ((0.5, 0.6), (1.0, 1.0), (1.19, 0.5), (1.5, 0.35), (2.0, 0.3), (2.74, 0.2)))
        i = int(st * SR)
        x[i:i + m] += k * rng.uniform(0.6, 1.0)
        st += rng.uniform(1.6, 3.2)
    return hall(tp(x, 2500), 4.0, rng, nass=0.6, hell=1600)



KLAENGE = {
    "schrei": (schrei, 4, 96, "Ferner Schrei"),
    "schuesse": (schuesse, 4, 128, "Schüsse in der Ferne"),
    "sirene": (sirene, 1, 160, "Sirene in der Ferne"),
    "klopfen": (klopfen, 3, 24, "Klopfen"),
    "schritte": (schritte, 3, 20, "Schritte hinter dir"),
    "fluestern": (fluestern, 4, 16, "Flüstern"),
    "heulen": (heulen, 3, 112, "Heulen in der Ferne"),
    "metall": (metall, 3, 48, "Ächzendes Metall"),
    "funk": (funk, 2, 24, "Funkrauschen"),
    "explosion": (explosion, 3, 192, "Detonation in der Ferne"),
    "hubschrauber": (hubschrauber, 2, 192, "Hubschrauber in der Ferne"),
    "horde": (horde, 3, 128, "Eine Horde stöhnt in der Ferne"),
    "autoalarm": (autoalarm, 2, 96, "Autoalarm in der Ferne"),
    "glocke": (glocke, 2, 160, "Totenglocke in der Ferne"),
}


def main():
    sounds = os.path.join(ASSETS, "sounds.json")
    daten = json.load(open(sounds)) if os.path.exists(sounds) else {}
    texte = {}
    for name, (fn, anzahl, weite, untertitel) in KLAENGE.items():
        for i in range(1, anzahl + 1):
            speichern(f"{name}_{i}", fn(np.random.default_rng(sum(map(ord, name)) * 97 + i * 31)))
        daten[f"atmo.{name}"] = {"subtitle": f"subtitles.deadzonemc.atmo.{name}", "sounds": [
            {"name": f"deadzonemc:atmo/{name}_{i}", "attenuation_distance": weite} for i in range(1, anzahl + 1)]}
        texte[f"subtitles.deadzonemc.atmo.{name}"] = untertitel
        print(name, anzahl)
    with open(sounds, "w") as f:
        json.dump(daten, f, indent=1)
    with open(os.path.join(WURZEL, "werkzeuge", "atmosphaere_texte.json"), "w", encoding="utf-8") as f:
        json.dump(texte, f, indent=1, ensure_ascii=False)


if __name__ == "__main__":
    main()
