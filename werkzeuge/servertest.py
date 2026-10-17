"""Startet einen Testserver mit der Mod, erzeugt eine Welt und probiert alles einmal aus.

Wird von GitHub nach jedem Hochladen ausgeführt (.github/workflows/test.yml).
Schlägt fehl, wenn der Server abstürzt, nicht hochfährt oder Fehler unserer Mod im Log stehen.
"""
import os
import re
import subprocess
import sys
import threading
import time

BEFEHLE = [
    "deadzone wetter",
    "deadzone wetter dichter_nebel",
    "deadzone wetter saurer_regen 2",
    "deadzone wetter aschesturm",
    "deadzone wetter klar",
    "locate biome deadzonemc:vorstadt",
    "locate biome deadzonemc:toter_wald",
    "locate biome deadzonemc:oedland",
    "locate biome deadzonemc:verseuchtes_moor",
    "locate biome deadzonemc:totes_meer",
    "locate structure deadzonemc:siedlung",
    "locate structure deadzonemc:einzelhaus",
    "place structure deadzonemc:siedlung 0 ~ 0",
    "place structure deadzonemc:einzelhaus 200 ~ 200",
    "place structure deadzonemc:siedlung -300 ~ 300",
    "place feature deadzonemc:toter_baum 40 ~ 40",
    "place feature deadzonemc:autowrack 50 ~ 40",
    "place feature deadzonemc:schutt 60 ~ 40",
    "place feature deadzonemc:strommast 70 ~ 40",
    "loot spawn 0 100 0 loot deadzonemc:kiste/haus_kueche",
    "loot spawn 0 100 0 loot deadzonemc:kiste/haus_wohnraum",
    "loot spawn 0 100 0 loot deadzonemc:kiste/haus_schlafzimmer",
    "loot spawn 0 100 0 loot deadzonemc:kiste/haus_keller",
    "loot spawn 0 100 0 loot deadzonemc:kiste/autowrack",
    "save-all",
]

FEHLER_MUSTER = [
    re.compile(r"Exception", re.I),
    re.compile(r"Feature order cycle", re.I),
    re.compile(r"Failed to (load|parse|decode)", re.I),
    re.compile(r"Couldn't parse", re.I),
    re.compile(r"Unknown (registry|element|key)", re.I),
    re.compile(r"No key \w+ in MapLike", re.I),
]


def main():
    os.makedirs("run", exist_ok=True)
    with open("run/eula.txt", "w") as f:
        f.write("eula=true\n")
    with open("run/server.properties", "w") as f:
        f.write("level-seed=20261017\nonline-mode=false\nspawn-protection=0\nmax-tick-time=-1\n")

    gradlew = "./gradlew" if os.name != "nt" else "gradlew.bat"
    prozess = subprocess.Popen([gradlew, "runServer", "--console=plain"], stdin=subprocess.PIPE,
                               stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True, bufsize=1)
    zeilen = []
    bereit = threading.Event()

    def lesen():
        for zeile in prozess.stdout:
            zeilen.append(zeile)
            sys.stdout.write(zeile)
            if "Done (" in zeile and "For help" in zeile:
                bereit.set()

    leser = threading.Thread(target=lesen, daemon=True)
    leser.start()

    if not bereit.wait(timeout=1200):
        prozess.kill()
        print("\n### FEHLER: Server ist nicht hochgefahren.")
        return 1

    start_befehle = len(zeilen)
    for befehl in BEFEHLE:
        print(f"\n### Befehl: /{befehl}", flush=True)
        zeilen.append(f"### Befehl: /{befehl}\n")
        prozess.stdin.write(befehl + "\n")
        prozess.stdin.flush()
        time.sleep(4 if befehl.startswith(("place", "locate")) else 1.5)
    time.sleep(10)
    prozess.stdin.write("stop\n")
    prozess.stdin.flush()
    try:
        prozess.wait(timeout=300)
    except subprocess.TimeoutExpired:
        prozess.kill()

    log = "".join(zeilen)
    probleme = []
    for zeile in zeilen:
        if ("deadzonemc" in zeile or "de.deadzonemc" in zeile) and any(m.search(zeile) for m in FEHLER_MUSTER):
            probleme.append(zeile.strip())
        elif "/ERROR]" in zeile and ("deadzonemc" in zeile or "worldgen" in zeile.lower()):
            probleme.append(zeile.strip())
    befehls_log = "".join(zeilen[start_befehle:])
    for antwort in ("Unknown or incomplete command", "Could not find", "Failed to place",
                    "Unknown structure", "Unknown feature", "Unknown loot table", "Invalid position"):
        for zeile in befehls_log.splitlines():
            if antwort in zeile:
                probleme.append("Befehl: " + zeile.strip())
    if "Crash report" in log or "---- Minecraft Crash Report ----" in log:
        probleme.append("Absturzbericht gefunden")

    # Antworten des Servers auf die Befehle sammeln (für die Anzeige bei GitHub)
    antworten = []
    for zeile in befehls_log.splitlines():
        if zeile.startswith("### Befehl"):
            antworten.append(zeile)
        elif "/INFO]" in zeile and ("Server thread" in zeile or "minecraft/" in zeile):
            text = zeile.split("]: ", 1)[-1].strip()
            if text and not text.startswith(("Saving", "ThreadedAnvilChunkStorage", "Stopping")):
                antworten.append("   " + text[:220])
    melden("notice", "Antworten des Testservers", "\n".join(antworten[:150]))

    print("\n\n========== ZUSAMMENFASSUNG ==========")
    if probleme:
        eindeutig = list(dict.fromkeys(probleme))
        for p in eindeutig:
            print("PROBLEM:", p)
        melden("error", "Probleme im Testserver", "\n".join(p[:400] for p in eindeutig[:60]))
        return 1
    print("Alles in Ordnung: Welt erzeugt, Befehle ausgeführt, keine Fehler unserer Mod im Log.")
    return 0


def melden(art, titel, text):
    """Hängt eine Meldung an den GitHub-Lauf (als Annotation), damit man sie ohne Log-Download lesen kann."""
    if not os.environ.get("GITHUB_ACTIONS"):
        return
    text = text.replace("%", "%25").replace("\r", "").replace("\n", "%0A")
    print(f"::{art} title={titel}::{text}", flush=True)


if __name__ == "__main__":
    sys.exit(main())
