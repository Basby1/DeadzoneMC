#!/usr/bin/env python3
"""Baut das Modpack „Deadzone MC“ für die CurseForge-App und richtet einen Testserver damit ein.

  python3 werkzeuge/pack.py bauen    – packwiz-Pack aus pack/mods.txt, Export als CurseForge-ZIP
  python3 werkzeuge/pack.py server   – echter NeoForge-Server mit allen Server-Mods in build/server

Läuft bei GitHub (dort gibt es Netz zu CurseForge); packwiz muss im PATH liegen.
"""
import glob
import hashlib
import json
import os
import re
import shutil
import subprocess
import sys
import tomllib
import urllib.parse
import urllib.request
import zipfile

WURZEL = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
PACK = os.path.join(WURZEL, "build", "pack")
SERVER = os.path.join(WURZEL, "build", "server")
AUSGABE = os.path.join(WURZEL, "build", "deadzone-mc-modpack.zip")
KOPF = {"User-Agent": "Basby1/DeadzoneMC packbau"}


def melden(art, titel, text):
    text = text.replace("%", "%25").replace("\r", "").replace("\n", "%0A")
    print(f"::{art} title={titel}::{text}", flush=True)


def laden(url):
    with urllib.request.urlopen(urllib.request.Request(url, headers=KOPF), timeout=120) as a:
        return a.read()


def neoforge_version():
    xml = laden("https://maven.neoforged.net/releases/net/neoforged/neoforge/maven-metadata.xml").decode()
    versionen = [v for v in re.findall(r"<version>([^<]+)</version>", xml) if re.fullmatch(r"21\.1\.\d+", v)]
    return max(versionen, key=lambda v: int(v.split(".")[2]))


def mod_version():
    with open(os.path.join(WURZEL, "gradle.properties")) as f:
        return re.search(r"mod_version=(\S+)", f.read()).group(1)


def liste():
    eintraege = []
    with open(os.path.join(WURZEL, "pack", "mods.txt")) as f:
        for zeile in f:
            zeile = zeile.split("#", 1)[0].strip()
            if zeile:
                teile = zeile.split()
                eintraege.append((teile[0], teile[1] if len(teile) > 1 else "both"))
    return eintraege


def metadateien():
    return set(glob.glob(os.path.join(PACK, "**", "*.pw.toml"), recursive=True))


def bauen():
    shutil.rmtree(PACK, ignore_errors=True)
    os.makedirs(PACK)
    neo = neoforge_version()
    with open(os.path.join(PACK, "pack.toml"), "w") as f:
        f.write(f'''name = "Deadzone MC"
author = "Basby1"
version = "{mod_version()}"
pack-format = "packwiz:1.1.0"

[index]
file = "index.toml"
hash-format = "sha256"
hash = ""

[versions]
minecraft = "1.21.1"
neoforge = "{neo}"

[options]
acceptable-game-versions = ["1.21"]
''')
    with open(os.path.join(PACK, "index.toml"), "w") as f:
        f.write('hash-format = "sha256"\n')
    subprocess.run(["packwiz", "refresh"], cwd=PACK, check=True)

    ok, fehler = [], []
    for pfad, seite in liste():
        vorher = metadateien()
        url = "https://www.curseforge.com/minecraft/" + pfad
        r = subprocess.run(["packwiz", "-y", "curseforge", "add", url], cwd=PACK,
                           capture_output=True, text=True, stdin=subprocess.DEVNULL, timeout=300)
        neu = sorted(metadateien() - vorher)
        if r.returncode != 0 or not neu:
            grund = (r.stderr.strip() or r.stdout.strip()).splitlines()[-1:] or ["?"]
            fehler.append(f"{pfad}: {grund[0][:200]}")
            continue
        for datei in neu:
            with open(datei) as f:
                inhalt = f.read()
            inhalt = re.sub(r'^side = ".*"$', f'side = "{seite}"', inhalt, flags=re.M)
            with open(datei, "w") as f:
                f.write(inhalt)
            # Shader gehören in shaderpacks (packwiz legt sie je nach Version woanders ab)
            if pfad.startswith("shaders/") and os.path.basename(os.path.dirname(datei)) != "shaderpacks":
                ziel = os.path.join(PACK, "shaderpacks")
                os.makedirs(ziel, exist_ok=True)
                shutil.move(datei, os.path.join(ziel, os.path.basename(datei)))
                ok.append(f"  (verschoben aus {os.path.relpath(datei, PACK)})")
        namen = [os.path.basename(d).replace(".pw.toml", "") for d in neu]
        ok.append(f"{pfad} -> {', '.join(namen)}")

    # Eigene Einstellungen (config, defaultconfigs …) als Overrides
    ueber = os.path.join(WURZEL, "pack", "overrides")
    if os.path.isdir(ueber):
        shutil.copytree(ueber, PACK, dirs_exist_ok=True)
    voreinstellungen()
    subprocess.run(["packwiz", "refresh"], cwd=PACK, check=True, capture_output=True, text=True)
    subprocess.run(["packwiz", "curseforge", "export", "-o", AUSGABE], cwd=PACK, check=True, capture_output=True, text=True)

    # Unsere eigene Mod liegt nicht auf CurseForge – sie kommt als Override in den Ordner mods
    jars = [j for j in glob.glob(os.path.join(WURZEL, "build", "libs", "deadzonemc-*.jar"))
            if not j.endswith(("-sources.jar", "-javadoc.jar"))]
    with zipfile.ZipFile(AUSGABE, "a") as z:
        for j in jars:
            z.write(j, "overrides/mods/" + os.path.basename(j))

    with zipfile.ZipFile(AUSGABE) as z:
        manifest = json.loads(z.read("manifest.json"))
        ueberschreibungen = [n for n in z.namelist() if n.startswith("overrides/")]
    melden("notice", "Inhalt der Pack-Datei",
           f"{len(manifest['files'])} CurseForge-Dateien, Loader {manifest['minecraft']['modLoaders']}\n"
           + "\n".join(ueberschreibungen)[:3000])
    melden("notice", f"Pack gebaut (NeoForge {neo}, {len(metadateien())} Dateien)", "\n".join(ok)[:3800])
    if fehler:
        melden("warning", f"Nicht hinzugefügt ({len(fehler)})", "\n".join(fehler)[:3800])
    print("\n".join(ok))
    print("FEHLER:\n" + "\n".join(fehler))


def dateiname(ordner):
    namen = []
    for d in sorted(glob.glob(os.path.join(PACK, ordner, "*.pw.toml"))):
        with open(d, "rb") as f:
            namen.append(tomllib.load(f)["filename"])
    return namen


def voreinstellungen():
    """Shader und Animations-Ressourcenpaket gleich einschalten, Sprache Deutsch (nur beim ersten Start)."""
    shader = dateiname("shaderpacks")
    pakete = dateiname("resourcepacks")
    os.makedirs(os.path.join(PACK, "config"), exist_ok=True)
    if shader:
        with open(os.path.join(PACK, "config", "iris.properties"), "w") as f:
            f.write(f"shaderPack={shader[0]}\nenableShaders=true\n")
    with open(os.path.join(PACK, "options.txt"), "w") as f:
        liste_ = ",".join(['"vanilla"'] + [f'"file/{p}"' for p in pakete])
        f.write(f"lang:de_de\nresourcePacks:[{liste_}]\nrenderDistance:12\nsimulationDistance:8\n"
                "gamma:0.0\nsoundCategory_ambient:1.0\ndarkMojangStudiosBackground:true\n")
    melden("notice", "Voreinstellungen", f"Shader: {shader}\nRessourcenpakete: {pakete}")


def cf_url(datei_id, name, gepolstert):
    teil = f"{datei_id % 1000:03d}" if gepolstert else str(datei_id % 1000)
    return f"https://edge.forgecdn.net/files/{datei_id // 1000}/{teil}/{urllib.parse.quote(name)}"


def server():
    with open(os.path.join(PACK, "pack.toml"), "rb") as f:
        neo = tomllib.load(f)["versions"]["neoforge"]
    shutil.rmtree(SERVER, ignore_errors=True)
    os.makedirs(os.path.join(SERVER, "mods"))
    inst = os.path.join(SERVER, "installer.jar")
    with open(inst, "wb") as f:
        f.write(laden(f"https://maven.neoforged.net/releases/net/neoforged/neoforge/{neo}/neoforge-{neo}-installer.jar"))
    subprocess.run(["java", "-jar", "installer.jar", "--installServer"], cwd=SERVER, check=True,
                   stdout=subprocess.DEVNULL)

    geladen, uebersprungen, fehler = 0, [], []
    for datei in sorted(metadateien()):
        with open(datei, "rb") as f:
            m = tomllib.load(f)
        if m.get("side") == "client" or not datei.startswith(os.path.join(PACK, "mods")):
            uebersprungen.append(m.get("name", datei))
            continue
        cf = m.get("update", {}).get("curseforge", {})
        url = m["download"].get("url")
        kandidaten = [url] if url else [cf_url(cf["file-id"], m["filename"], False), cf_url(cf["file-id"], m["filename"], True)]
        daten = None
        for u in kandidaten:
            try:
                daten = laden(u)
                break
            except Exception:
                continue
        if daten is None:
            fehler.append(f"{m['filename']} ({cf.get('file-id')})")
            continue
        algo = m["download"].get("hash-format", "sha1")
        if hashlib.new(algo, daten).hexdigest() != m["download"].get("hash"):
            fehler.append(f"{m['filename']}: Prüfsumme falsch")
            continue
        with open(os.path.join(SERVER, "mods", m["filename"]), "wb") as f:
            f.write(daten)
        geladen += 1

    for j in glob.glob(os.path.join(WURZEL, "build", "libs", "deadzonemc-*.jar")):
        if not j.endswith(("-sources.jar", "-javadoc.jar")):
            shutil.copy(j, os.path.join(SERVER, "mods"))
    for ordner in ("config", "defaultconfigs"):
        quelle = os.path.join(WURZEL, "pack", "overrides", ordner)
        if os.path.isdir(quelle):
            shutil.copytree(quelle, os.path.join(SERVER, ordner), dirs_exist_ok=True)
    with open(os.path.join(SERVER, "user_jvm_args.txt"), "a") as f:
        f.write("\n-Xmx6G\n-Ddeadzonemc.selbsttest=pack\n")
    melden("notice", f"Testserver: {geladen} Mods geladen, {len(uebersprungen)} nur für den Client",
           "Nur Client: " + ", ".join(uebersprungen))
    if fehler:
        melden("error", "Downloads fehlgeschlagen", "\n".join(fehler))


if __name__ == "__main__":
    try:
        {"bauen": bauen, "server": server}[sys.argv[1]]()
    except subprocess.CalledProcessError as e:
        melden("error", f"Fehler bei {sys.argv[1]}", f"{e.cmd}: {(e.stderr or e.stdout or '')}"[-3000:])
        raise
    except Exception:
        import traceback
        melden("error", f"Fehler bei {sys.argv[1]}", traceback.format_exc()[-3000:])
        raise
