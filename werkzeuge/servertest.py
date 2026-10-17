"""Startet einen Testserver mit der Mod, erzeugt eine Welt und probiert alles einmal aus.

Wird von GitHub nach jedem Hochladen ausgeführt (.github/workflows/test.yml).
Die Testbefehle selbst stehen in der Mod (de.deadzonemc.Selbsttest) und laufen nur, wenn der Server
mit -Pselbsttest gestartet wird. Danach fährt der Server von selbst herunter.

Schlägt fehl, wenn der Server abstürzt, der Selbsttest nicht fertig wird oder Fehler unserer Mod im Log stehen.
"""
import os
import re
import subprocess
import sys

FEHLER_MUSTER = [
    re.compile(r"Exception", re.I),
    re.compile(r"Feature order cycle", re.I),
    re.compile(r"Failed to (load|parse|decode)", re.I),
    re.compile(r"Couldn't parse", re.I),
    re.compile(r"Unknown (registry|element|key)", re.I),
    re.compile(r"No key \w+ in MapLike", re.I),
]

BEFEHLS_FEHLER = [
    "Unknown or incomplete command", "Could not find", "Unknown structure",
    "Unknown feature", "Unknown loot table", "Invalid position", "Incorrect argument",
    "Unbekanntes Wetter", "Fehler bei /", "not loaded",
]


def melden(art, titel, text):
    """Hängt eine Meldung an den GitHub-Lauf (als Annotation), damit man sie ohne Log-Download lesen kann."""
    if not os.environ.get("GITHUB_ACTIONS"):
        return
    text = text.replace("%", "%25").replace("\r", "").replace("\n", "%0A")
    print(f"::{art} title={titel}::{text}", flush=True)


def main():
    os.makedirs("run", exist_ok=True)
    with open("run/eula.txt", "w") as f:
        f.write("eula=true\n")
    with open("run/server.properties", "w") as f:
        f.write("level-seed=20261017\nonline-mode=false\nspawn-protection=0\nmax-tick-time=-1\n")

    gradlew = "./gradlew" if os.name != "nt" else "gradlew.bat"
    prozess = subprocess.Popen([gradlew, "runServer", "-Pselbsttest", "--console=plain"],
                               stdin=subprocess.DEVNULL, stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
                               text=True, bufsize=1)
    zeilen = []
    import threading
    import time

    def wachhund():
        """Hängt der Server, Thread-Dump und letzte Zeilen melden und abbrechen."""
        frist = time.time() + 9 * 60
        while prozess.poll() is None:
            if time.time() > frist:
                melden("error", "Server hängt – letzte Zeilen", "".join(zeilen[-60:])[-3500:])
                try:
                    jps = subprocess.run(["jps", "-l"], capture_output=True, text=True).stdout
                    pids = [z.split()[0] for z in jps.splitlines() if z.strip() and "Jps" not in z
                            and "GradleDaemon" not in z and "GradleWrapperMain" not in z]
                    dumps = []
                    for pid in pids:
                        dump = subprocess.run(["jstack", pid], capture_output=True, text=True, timeout=40).stdout
                        haupt = [b for b in dump.split("\n\n") if "Server thread" in b or "Worker-Main" in b]
                        dumps.append("\n\n".join(haupt)[:3000])
                    melden("error", "Server hängt – Thread-Dump", "\n".join(dumps)[:3500])
                except Exception as e:  # noqa
                    melden("error", "Server hängt", str(e))
                prozess.kill()
                return
            time.sleep(10)

    threading.Thread(target=wachhund, daemon=True).start()
    try:
        for zeile in prozess.stdout:
            zeilen.append(zeile)
            sys.stdout.write(zeile)
        prozess.wait(timeout=60)
    except subprocess.TimeoutExpired:
        prozess.kill()

    log = "".join(zeilen)
    probleme = []
    if "Done (" not in log:
        probleme.append("Server ist nicht hochgefahren")
    if "[Selbsttest] fertig" not in log:
        probleme.append("Selbsttest ist nicht bis zum Ende gelaufen")
    if "---- Minecraft Crash Report ----" in log or "Crash report saved" in log:
        probleme.append("Absturzbericht gefunden")

    antworten = []
    im_test = False
    for zeile in zeilen:
        text = zeile.split("]: ", 1)[-1].strip()
        if "[Selbsttest]" in zeile:
            im_test = True
            antworten.append(zeile.split("[Selbsttest] ", 1)[-1].rstrip("\n"))
        elif im_test and "/INFO]" in zeile and ("MinecraftServer" in zeile or "Selbsttest" in zeile):
            antworten.append("   " + text[:240])
        if ("deadzonemc" in zeile or "de.deadzonemc" in zeile) and any(m.search(zeile) for m in FEHLER_MUSTER):
            probleme.append(text[:400])
        elif "/ERROR]" in zeile and ("deadzonemc" in zeile or "worldgen" in zeile.lower() or im_test):
            probleme.append(text[:400])
        if im_test and any(f in zeile for f in BEFEHLS_FEHLER):
            probleme.append("Befehl: " + text[:300])

    karte = [a for a in antworten if a.startswith(("|", "Draufsicht", "Grundriss"))]
    rest = [a for a in antworten if a not in karte]
    melden("notice", "Antworten des Testservers", "\n".join(rest[:200]))
    # Jede Karte als eigene Meldung (GitHub kürzt lange Meldungen)
    abschnitt = []
    for a in karte + ["Ende"]:
        if not a.startswith("|") and abschnitt:
            melden("notice", abschnitt[0], "\n".join(abschnitt[1:]))
            abschnitt = []
        abschnitt.append(a)
    print("\n\n========== ZUSAMMENFASSUNG ==========")
    for a in antworten:
        print(a)
    if probleme:
        eindeutig = list(dict.fromkeys(probleme))
        for p in eindeutig:
            print("PROBLEM:", p)
        melden("error", "Probleme im Testserver", "\n".join(eindeutig[:60]))
        return 1
    print("Alles in Ordnung: Welt erzeugt, Befehle ausgeführt, keine Fehler unserer Mod im Log.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
