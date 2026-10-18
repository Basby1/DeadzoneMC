"""Allgemeine Animationen für die 20 Dimensionswesen, aus den Knochennamen abgeleitet.

  python3 werkzeuge/monster/dim_anim.py
"""
import json
import os

import animationen as AN
import dimensionen as D
import entwuerfe as E


def anims(name):
    m = E.ALLE[name]()[0]
    kn = [k.name for k in m.knochen]
    wurzel = kn[0]
    rumpf = next((k for k in ("oberkoerper", "rumpf", "leib", "koerper", "masse", "kern") if k in kn), wurzel)
    kopf = next((k for k in kn if k.startswith("kopf")), None)
    beine = [k for k in kn if k.startswith("bein")]
    paare = [k for k in beine if not any(ch.isdigit() for ch in k.split("_")[-1])] or beine
    arme = [k for k in kn if k.startswith(("arm_", "arm")) and "unter" not in k]
    rest = [k for k in kn if k.startswith(("tentakel", "schwanz", "fluegel", "fuehler", "ranke", "kabel", "flosse", "segment"))]
    schwebt = not beine

    idle = AN.Anim("idle", 2.8)
    AN.atmen(idle, rumpf, 2.5)
    if kopf:
        idle.welle(kopf, "y", 8)
    for i, a in enumerate(arme):
        idle.welle(a, "z", 3, phase=i * 0.5)
    for i, r in enumerate(rest):
        idle.welle(r, "x", 10, phase=i * 0.13)
    if schwebt:
        idle.welle(wurzel, "y", 1.5, art="position")

    gang = AN.Anim("walk", 1.2 if not schwebt else 2.0)
    if beine:
        if len(beine) > 2:
            for i, b in enumerate(beine):
                gang.welle(b, "y", 14, phase=(i % 2) * 0.5 + (i // 2) * 0.17)
                gang.welle(b, "x", 10, phase=(i % 2) * 0.5 + (i // 2) * 0.17 + 0.25)
        else:
            AN.gehen(gang, paare, arme, 28, 16, wippen=(wurzel, 0.8))
    else:
        gang.welle(wurzel, "y", 2.5, art="position")
        gang.welle(rumpf, "x", 6)
    for i, r in enumerate(rest):
        gang.welle(r, "x", 18, phase=i * 0.13, frequenz=2)
    if kopf:
        gang.welle(kopf, "z", 5)

    angriff = AN.Anim("attack", 0.7, schleife=False)
    angriff.tasten(rumpf, "rotation", {0: (0, 0, 0), 0.25: (-12, 0, 0), 0.4: (18, 0, 0), 0.7: (0, 0, 0)})
    for a in arme:
        angriff.tasten(a, "rotation", {0: (0, 0, 0), 0.25: (120, 0, 0), 0.4: (20, 0, 0), 0.7: (0, 0, 0)})
    if kopf and not arme:
        angriff.tasten(kopf, "rotation", {0: (0, 0, 0), 0.25: (-25, 0, 0), 0.4: (30, 0, 0), 0.7: (0, 0, 0)})

    spezial = AN.Anim("special", 1.4, schleife=False)
    spezial.tasten(wurzel, "scale", {0: (1, 1, 1), 0.4: (1.12, 1.12, 1.12), 0.9: (1.12, 1.12, 1.12), 1.4: (1, 1, 1)})
    if kopf:
        spezial.tasten(kopf, "rotation", {0: (0, 0, 0), 0.4: (-30, 0, 0), 0.9: (-30, 0, 0), 1.4: (0, 0, 0)})
    for i, a in enumerate(arme):
        s = 1 if "rechts" in a else -1
        spezial.tasten(a, "rotation", {0: (0, 0, 0), 0.4: (40, 0, s * 50), 0.9: (40, 0, s * 50), 1.4: (0, 0, 0)})
    return [idle, gang, angriff, spezial]


def main():
    for n in D.ZONE + D.ORBIT:
        daten = {"format_version": "1.8.0", "animations": {f"animation.{n}.{a.name}": a.json() for a in anims(n)}}
        with open(os.path.join(AN.ZIEL, f"{n}.animation.json"), "w") as f:
            json.dump(daten, f)
    print("Dimensions-Animationen:", len(D.ZONE + D.ORBIT))


if __name__ == "__main__":
    main()
