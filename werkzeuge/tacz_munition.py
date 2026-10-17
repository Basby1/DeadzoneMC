"""Liest aus dem entpackten TaCZ-Standard-Waffenpaket je Waffe: Munition, Magazin, Schaden, Kopfschuss-Faktor, Kadenz."""
import glob
import json
import os
import re
import sys


def zahl(text, schluessel):
    m = re.search(r'"' + schluessel + r'"\s*:\s*([0-9.]+)', text)
    return m.group(1) if m else "?"


for f in sorted(glob.glob(os.path.join(sys.argv[1], "**", "*_data.json"), recursive=True)):
    if "/guns/" not in f:
        continue
    text = open(f, errors="replace").read()
    m = re.search(r'"ammo"\s*:\s*"([^"]+)"', text)
    name = os.path.basename(f)[:-10]
    ammo = m.group(1).replace("tacz:", "") if m else "?"
    print(f"{name}={ammo}/{zahl(text, 'ammo_amount')} d{zahl(text, 'damage')} k{zahl(text, 'head_shot_multiplier')} "
          f"n{zahl(text, 'bullet_amount')} r{zahl(text, 'rpm')}", end="; ")
print()
