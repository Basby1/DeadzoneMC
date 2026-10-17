"""Liest aus dem entpackten TaCZ-Standard-Waffenpaket, welche Munition jede Waffe braucht."""
import glob
import os
import re
import sys

for f in sorted(glob.glob(os.path.join(sys.argv[1], "**", "*_data.json"), recursive=True)):
    if "/guns/" not in f:
        continue
    text = open(f, errors="replace").read()
    m = re.search(r'"ammo"\s*:\s*"([^"]+)"', text)
    n = re.search(r'"ammo_amount"\s*:\s*(\d+)', text)
    print(os.path.basename(f)[:-10] + "=" + (m.group(1).replace("tacz:", "") if m else "?") + "/" + (n.group(1) if n else "?"), end=" ")
print()
