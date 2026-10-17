"""Animierte Vorschau (GIF) einer Monster-Animation:  python3 gif.py name animation [gier]"""
import os
import sys

from PIL import Image

import animationen
import entwuerfe
from maler import Maler
from render import rendern


def gif(name, anim_name, gier=-35, breite=420, hoehe=560, fps=12, ziel=None, wiederholen=1):
    modell = entwuerfe.ALLE[name]()[0]
    modell.uv_packen()
    maler = Maler(modell, saat=sum(map(ord, name)))
    maler.alles_malen()
    anims = {a.name: a for a in animationen.ALLE[name]()}
    a = anims[anim_name]
    # Maßstab und Mitte aus der Ruhepose, damit nichts springt
    _, skala, mitte = rendern(modell, maler.bild, maler.glow, gier, 10, breite, hoehe, mit_mitte=True)
    skala *= 0.85
    bilder = []
    n = max(2, int(a.laenge * fps))
    for _ in range(wiederholen):
        for i in range(n):
            t = a.laenge * i / n
            modell.pose = animationen.abtasten(a, t)
            b, _ = rendern(modell, maler.bild, maler.glow, gier, 10, breite, hoehe, skala=skala,
                           mitte_x=mitte[0], mitte_y=mitte[1])
            bilder.append(b.convert("P", palette=Image.ADAPTIVE, colors=200))
    if not a.schleife:  # kurz auf dem letzten Bild stehen bleiben, dann Ruhepose
        bilder += [bilder[-1]] * int(fps * 0.6)
    ziel = ziel or os.path.join(entwuerfe.VORSCHAU, f"{name}_{anim_name}.gif")
    bilder[0].save(ziel, save_all=True, append_images=bilder[1:], duration=int(1000 / fps), loop=0, disposal=2)
    return ziel


if __name__ == "__main__":
    print(gif(sys.argv[1], sys.argv[2], int(sys.argv[3]) if len(sys.argv) > 3 else -35))
