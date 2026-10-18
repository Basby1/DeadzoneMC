"""Liest Texte (String-Konstanten) aus Klassen fremder Mods, um deren Datenformate zu kennen.

Aufruf (bei GitHub): python3 werkzeuge/jarinfo.py "puffish-skills:operation|prototype|builtin" ...
Schreibt build/jarinfo.txt (je Klasse die vorkommenden Texte).
"""
import glob
import os
import re
import shutil
import subprocess
import sys
import tomllib
import zipfile

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import pack  # noqa: E402

ORDNER = os.path.join(pack.WURZEL, "build", "jipack")
JARS = os.path.join(pack.WURZEL, "build", "jars")


def main():
    auftraege = [a.split(":", 1) for a in sys.argv[1:]]
    shutil.rmtree(ORDNER, ignore_errors=True)
    os.makedirs(ORDNER)
    os.makedirs(JARS, exist_ok=True)
    with open(os.path.join(ORDNER, "pack.toml"), "w") as f:
        f.write(f'''name = "ji"
version = "1"
pack-format = "packwiz:1.1.0"
[index]
file = "index.toml"
hash-format = "sha256"
hash = ""
[versions]
minecraft = "1.21.1"
neoforge = "{pack.neoforge_version()}"
[options]
acceptable-game-versions = ["1.21"]
''')
    with open(os.path.join(ORDNER, "index.toml"), "w") as f:
        f.write('hash-format = "sha256"\n')
    subprocess.run(["packwiz", "refresh"], cwd=ORDNER, check=True)
    aus = []
    for slug, muster in auftraege:
        vorher = set(glob.glob(os.path.join(ORDNER, "**", "*.pw.toml"), recursive=True))
        subprocess.run(["packwiz", "-y", "curseforge", "add", "https://www.curseforge.com/minecraft/mc-mods/" + slug],
                       cwd=ORDNER, capture_output=True, text=True, stdin=subprocess.DEVNULL, timeout=300)
        neu = sorted(set(glob.glob(os.path.join(ORDNER, "**", "*.pw.toml"), recursive=True)) - vorher)
        for datei in neu:
            with open(datei, "rb") as f:
                m = tomllib.load(f)
            cf = m.get("update", {}).get("curseforge", {})
            url = m["download"].get("url")
            kand = [url] if url else [pack.cf_url(cf["file-id"], m["filename"], False),
                                      pack.cf_url(cf["file-id"], m["filename"], True)]
            daten = None
            for u in kand:
                try:
                    daten = pack.laden(u)
                    break
                except Exception:  # noqa
                    continue
            if daten is None:
                aus.append(f"### {m['filename']}: Download fehlgeschlagen")
                continue
            jar = os.path.join(JARS, m["filename"])
            with open(jar, "wb") as f:
                f.write(daten)
            aus.append(f"### {m['filename']} (Muster {muster})")
            re_m = re.compile(muster, re.I)
            ziel = os.path.join(JARS, "x_" + m["filename"])
            with zipfile.ZipFile(jar) as z:
                namen = z.namelist()
                aus.append("Dateien: " + str(len(namen)))
                for n in namen:
                    if not n.endswith(".class") and re_m.search(n) and not n.endswith("/"):
                        aus.append("  Datei " + n)
                klassen = [n for n in namen if n.endswith(".class") and re_m.search(n)]
                z.extractall(ziel, klassen)
            for k in klassen:
                r = subprocess.run(["javap", "-c", "-constants", "-p", os.path.join(ziel, k)],
                                   capture_output=True, text=True)
                texte = list(dict.fromkeys(re.findall(r"// String (.+)$", r.stdout, re.M)))
                if texte:
                    aus.append(f"{k}: " + " | ".join(texte))
    os.makedirs(os.path.join(pack.WURZEL, "build"), exist_ok=True)
    with open(os.path.join(pack.WURZEL, "build", "jarinfo.txt"), "w") as f:
        f.write("\n".join(aus) + "\n")
    print("\n".join(aus)[:5000])


if __name__ == "__main__":
    main()
