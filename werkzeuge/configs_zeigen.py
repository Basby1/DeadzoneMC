#!/usr/bin/env python3
"""Zeigt die vom Testserver erzeugten Standard-Configs ausgewählter Mods als Annotationen (ohne Kommentare),
damit wir sie gezielt anpassen können, ohne Logs herunterzuladen.

  python3 werkzeuge/configs_zeigen.py <ordner> <muster> [<muster> …]
"""
import fnmatch
import os
import sys


def melden(titel, text):
    text = text.replace("%", "%25").replace("\r", "").replace("\n", "%0A")
    print(f"::notice title={titel}::{text}", flush=True)


def main():
    ordner = sys.argv[1]
    muster = sys.argv[2:]
    funde = []
    for wurzel, _, dateien in os.walk(ordner):
        for d in sorted(dateien):
            pfad = os.path.join(wurzel, d)
            rel = os.path.relpath(pfad, ordner)
            if any(fnmatch.fnmatch(rel.lower(), m.lower()) for m in muster):
                funde.append((rel, pfad))
    alle = [rel for rel, _ in funde]
    print("Gefunden:", alle)
    block, groesse, nr = [], 0, 1
    for rel, pfad in funde:
        try:
            with open(pfad, errors="replace") as f:
                zeilen = [z.rstrip() for z in f if z.strip() and not z.strip().startswith(("#", "//"))]
        except OSError:
            continue
        ganz = f"== {rel}\n" + "\n".join(z.replace("\t", " ").replace("    ", " ") for z in zeilen)
        # lange Dateien auf mehrere Meldungen verteilen
        for i in range(0, len(ganz), 3600):
            text = ganz[i:i + 3600]
            if groesse + len(text) > 3700 and block:
                melden(f"Configs {nr}", "\n".join(block))
                nr += 1
                block, groesse = [], 0
            block.append(text)
            groesse += len(text)
    if block:
        melden(f"Configs {nr}", "\n".join(block))


if __name__ == "__main__":
    main()
