"""Gleicht die Lautstärke aller Geräusche und Musikstücke an (EBU R128), damit es im Spiel nicht mal dröhnt und
mal kaum zu hören ist, und begrenzt die Spitzen (keine übersteuerten Schüsse mehr).

  python3 werkzeuge/lautheit.py            – alles unter assets/deadzonemc/sounds
  python3 werkzeuge/lautheit.py messen     – nur messen, nichts ändern

Ziele (integrierte Lautheit):
  Musik           -23 LUFS, Lautheitsumfang höchstens 8 LU (das Intro behält mehr Dynamik)
  Waffen          -18 LUFS
  Monster         -20 LUFS (Schreie, Gebrüll -18, Umgebungslaute -22)
  Atmosphäre      -22 LUFS
Spitzen werden bei -1,5 dBTP begrenzt. Sehr kurze Klänge (< 0,4 s) werden über die Spitze ausgerichtet.
"""
import json
import os
import re
import subprocess
import sys
import tempfile
from concurrent.futures import ThreadPoolExecutor

WURZEL = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SOUNDS = os.path.join(WURZEL, "src", "main", "resources", "assets", "deadzonemc", "sounds")
TP = -1.5


def messen(pfad):
    o = subprocess.run(["ffmpeg", "-hide_banner", "-nostats", "-i", pfad, "-af", "ebur128=framelog=quiet:peak=true",
                        "-f", "null", "-"], capture_output=True, text=True).stderr
    i = re.findall(r"I:\s+(-?[\d.]+) LUFS", o)
    lra = re.findall(r"LRA:\s+(-?[\d.]+) LU", o)
    p = re.findall(r"Peak:\s+(-?[\d.]+|-inf) dBFS", o)
    d = re.findall(r"Duration: (\d+):(\d+):([\d.]+)", o)
    dauer = float(d[0][2]) + 60 * int(d[0][1]) if d else 0.0
    spitze = float(p[-1]) if p and p[-1] != "-inf" else -99.0
    return (float(i[-1]) if i else -70.0), (float(lra[-1]) if lra else 0.0), spitze, dauer


def ziel(rel):
    """Ziel-Lautheit und Kategorie für eine Datei (Pfad relativ zu sounds/)."""
    name = os.path.basename(rel)
    if rel.startswith("musik/"):
        return (-21.0, "kino") if name.startswith("kino") else (-23.0, "musik")
    if rel.startswith("waffe/"):
        return -18.0, "geraeusch"
    if rel.startswith("atmo/"):
        return -22.0, "geraeusch"
    if any(w in name for w in ("schrei", "brullen", "gebruell", "death")):
        return -18.0, "geraeusch"
    if "ambient" in name or "herzschlag" in name:
        return -22.0, "geraeusch"
    return -20.0, "geraeusch"


def schreiben(quelle, filter_, qualitaet):
    with tempfile.TemporaryDirectory() as tmp:
        aus = os.path.join(tmp, "x.ogg")
        r = subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", quelle, "-af", filter_, "-ar", "44100",
                            "-c:a", "libvorbis", "-q:a", str(qualitaet), aus], capture_output=True, text=True)
        if r.returncode != 0 or not os.path.exists(aus):
            return False
        os.replace(aus, quelle)
    return True


def angleichen(rel):
    pfad = os.path.join(SOUNDS, rel)
    i, lra, spitze, dauer = messen(pfad)
    soll, art = ziel(rel)
    if art in ("musik", "kino"):
        bereich = 12 if art == "kino" else 8
        # zweistufig: messen, dann mit festen Messwerten (dynamische Anpassung nur, wenn der Umfang zu groß ist)
        o = subprocess.run(["ffmpeg", "-hide_banner", "-i", pfad, "-af",
                            f"loudnorm=I={soll}:TP={TP}:LRA={bereich}:print_format=json", "-f", "null", "-"],
                           capture_output=True, text=True).stderr
        m = json.loads(o[o.rindex("{"):o.rindex("}") + 1])
        f = (f"loudnorm=I={soll}:TP={TP}:LRA={bereich}:measured_I={m['input_i']}:measured_TP={m['input_tp']}:"
             f"measured_LRA={m['input_lra']}:measured_thresh={m['input_thresh']}:offset={m['target_offset']}")
        ok = schreiben(pfad, f, 3)
    else:
        if dauer < 0.4 or i <= -69:
            gain = -3.0 - spitze  # kurze Klänge: Spitze auf -3 dBFS
        else:
            gain = soll - i
        gain = max(-30.0, min(24.0, gain))
        f = f"volume={gain:.2f}dB,alimiter=limit={10 ** (TP / 20):.3f}:level=false:attack=2:release=60"
        ok = schreiben(pfad, f, 5)
    if not ok:
        return f"FEHLER {rel}"
    i2, lra2, spitze2, _ = messen(pfad)
    return f"{rel}: {i:.1f} → {i2:.1f} LUFS, Spitze {spitze:.1f} → {spitze2:.1f}, Umfang {lra:.1f} → {lra2:.1f}"


def alle():
    liste = []
    for wurzel, _, dateien in os.walk(SOUNDS):
        for d in dateien:
            if d.endswith(".ogg"):
                liste.append(os.path.relpath(os.path.join(wurzel, d), SOUNDS))
    return sorted(liste)


if __name__ == "__main__":
    dateien = [a for a in sys.argv[1:] if a != "messen"] or alle()
    if "messen" in sys.argv:
        for rel in dateien:
            i, lra, sp, d = messen(os.path.join(SOUNDS, rel))
            print(f"{rel}: {i:.1f} LUFS, Umfang {lra:.1f}, Spitze {sp:.1f}, {d:.1f} s")
        sys.exit(0)
    with ThreadPoolExecutor(max_workers=os.cpu_count() or 4) as pool:
        for zeile in pool.map(angleichen, dateien):
            print(zeile)
