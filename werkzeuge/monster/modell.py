"""Bausteine für Monstermodelle: Knochen mit Würfeln, Box-UV-Atlas, Export als GeckoLib-geo.json.

Einheiten wie in Blockbench: 16 = ein Block, y nach oben, Füße bei y = 0, das Modell schaut nach Norden (-z).
"""
import json
import math

import numpy as np


class Wuerfel:
    def __init__(self, ursprung, groesse, material, deko=None, aufblasen=0.0, uv=None, spiegeln=False):
        self.ursprung = [float(v) for v in ursprung]  # kleinste Ecke
        self.groesse = [float(v) for v in groesse]
        self.material = material
        self.deko = deko  # fn(maler, flaeche, rect) für Gesichter, Wunden …
        self.aufblasen = aufblasen
        self.uv = uv  # wird beim Packen gesetzt
        self.spiegeln = spiegeln


class Knochen:
    def __init__(self, name, eltern=None, drehpunkt=(0, 0, 0), drehung=(0, 0, 0)):
        self.name = name
        self.eltern = eltern
        self.drehpunkt = [float(v) for v in drehpunkt]
        self.drehung = [float(v) for v in drehung]
        self.wuerfel = []

    def w(self, *args, **kw):
        self.wuerfel.append(Wuerfel(*args, **kw))
        return self


class Modell:
    def __init__(self, name, textur_breite=64, textur_hoehe=64):
        self.name = name
        self.tb = textur_breite
        self.th = textur_hoehe
        self.knochen = []
        self.pose = {}  # Knochenname -> zusätzliche Drehung nur für die Vorschau

    def k(self, name, eltern=None, drehpunkt=(0, 0, 0), drehung=(0, 0, 0)):
        kn = Knochen(name, eltern, drehpunkt, drehung)
        self.knochen.append(kn)
        return kn

    def alle_wuerfel(self):
        for kn in self.knochen:
            for w in kn.wuerfel:
                yield kn, w

    # --- Ausrichtung -----------------------------------------------------
    def vorwaerts_richten(self, ausnahmen=()):
        """Arme, die mit negativem x nach hinten hängen, nach vorn spiegeln.

        In GeckoLib schwingt ein herabhängendes Glied bei positivem x nach vorn (Norden, -z). Viele Entwürfe
        hatten die Arme mit negativem x "nach vorn" gestellt – im Spiel zeigten sie (samt Waffe) nach hinten.
        Gespiegelt wird der ganze Teilbaum an der Ebene durch das Schultergelenk: x- und y-Drehung umgedreht,
        Würfel und Drehpunkte in z gespiegelt. So bleibt jede Pose erhalten, nur eben nach vorn.
        """
        kinder = {}
        for kn in self.knochen:
            kinder.setdefault(kn.eltern, []).append(kn)
        gespiegelt = set()

        def teilbaum(kn):
            yield kn
            for c in kinder.get(kn.name, []):
                yield from teilbaum(c)

        for kn in self.knochen:
            if kn.name in ausnahmen or kn.name in gespiegelt:
                continue
            if not kn.name.startswith(("arm_", "hand_", "unterarm_")) or kn.drehung[0] > -3:
                continue
            z0 = kn.drehpunkt[2]
            for t in teilbaum(kn):
                if t.name in ausnahmen:
                    continue
                gespiegelt.add(t.name)
                t.drehung = [-t.drehung[0], -t.drehung[1], t.drehung[2]]
                t.drehpunkt[2] = 2 * z0 - t.drehpunkt[2]
                for w in t.wuerfel:
                    w.ursprung[2] = 2 * z0 - (w.ursprung[2] + w.groesse[2])
                if t.name in self.pose:
                    pz = self.pose[t.name]
                    if isinstance(pz, dict):
                        r = pz.get("rotation")
                        if r:
                            pz["rotation"] = (-r[0], -r[1], r[2])
                    else:
                        self.pose[t.name] = (-pz[0], -pz[1], pz[2])
        return sorted(gespiegelt)

    # --- Box-UV packen ---------------------------------------------------
    def uv_packen(self):
        """Ordnet jedem Würfel einen Platz im Texturatlas zu (Regalverfahren, größte zuerst)."""
        liste = list(self.alle_wuerfel())

        def ausmass(w):
            gx, gy, gz = (max(1, math.ceil(v)) for v in w.groesse)
            return 2 * gz + 2 * gx, gz + gy

        liste.sort(key=lambda kw: -ausmass(kw[1])[1] * 1000 - ausmass(kw[1])[0])
        x = y = zeile = 0
        for _, w in liste:
            b, h = ausmass(w)
            if x + b > self.tb:
                x = 0
                y += zeile
                zeile = 0
            if y + h > self.th:
                raise ValueError(f"{self.name}: Textur {self.tb}x{self.th} zu klein")
            w.uv = (x, y)
            x += b
            zeile = max(zeile, h)

    # --- Export ----------------------------------------------------------
    def geo_json(self):
        knochen = []
        for kn in self.knochen:
            d = {"name": kn.name, "pivot": [-kn.drehpunkt[0], kn.drehpunkt[1], kn.drehpunkt[2]]}
            if kn.eltern:
                d["parent"] = kn.eltern
            if any(kn.drehung):
                # Bedrock speichert x- und y-Drehung mit umgekehrtem Vorzeichen (wie Blockbench beim Export)
                d["rotation"] = [-kn.drehung[0], -kn.drehung[1], kn.drehung[2]]
            cubes = []
            for w in kn.wuerfel:
                # Bedrock spiegelt x gegenüber Blockbench-Ansicht
                ox = -(w.ursprung[0] + w.groesse[0])
                c = {"origin": [ox, w.ursprung[1], w.ursprung[2]], "size": w.groesse, "uv": list(w.uv)}
                if w.aufblasen:
                    c["inflate"] = w.aufblasen
                if w.spiegeln:
                    c["mirror"] = True
                cubes.append(c)
            if cubes:
                d["cubes"] = cubes
            knochen.append(d)
        return {
            "format_version": "1.12.0",
            "minecraft:geometry": [{
                "description": {
                    "identifier": f"geometry.deadzonemc.{self.name}",
                    "texture_width": self.tb, "texture_height": self.th,
                    "visible_bounds_width": 4, "visible_bounds_height": 4, "visible_bounds_offset": [0, 1.5, 0],
                },
                "bones": knochen,
            }],
        }

    def speichern(self, pfad):
        with open(pfad, "w") as f:
            json.dump(self.geo_json(), f, indent=1)


def flaechen(w):
    """Die sechs Flächen eines Würfels im Box-UV-Atlas: name -> (u, v, breite, höhe)."""
    u, v = w.uv
    gx, gy, gz = (max(1, math.ceil(x)) for x in w.groesse)
    return {
        "oben": (u + gz, v, gx, gz),
        "unten": (u + gz + gx, v, gx, gz),
        "rechts": (u, v + gz, gz, gy),        # Osten (+x): die rechte Seite des Modells
        "vorne": (u + gz, v + gz, gx, gy),    # Norden (-z)
        "links": (u + gz + gx, v + gz, gz, gy),
        "hinten": (u + 2 * gz + gx, v + gz, gx, gy),
    }


def drehmatrix(grad):
    x, y, z = (math.radians(a) for a in grad)
    rx = np.array([[1, 0, 0], [0, math.cos(x), -math.sin(x)], [0, math.sin(x), math.cos(x)]])
    ry = np.array([[math.cos(y), 0, math.sin(y)], [0, 1, 0], [-math.sin(y), 0, math.cos(y)]])
    rz = np.array([[math.cos(z), -math.sin(z), 0], [math.sin(z), math.cos(z), 0], [0, 0, 1]])
    return rz @ ry @ rx
