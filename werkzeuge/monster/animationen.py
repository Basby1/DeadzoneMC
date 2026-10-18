"""Animationen der Monster (GeckoLib/Bedrock-Format).

Winkel in Grad im selben Raum wie die Modelle (Blockbench-Ansicht): bei herabhängenden Gliedern schwingt
positives x nach vorn, beim Oberkörper kippt positives x nach hinten. Beim Export werden x und y wie in der
geo.json gespiegelt.

  python3 animationen.py   – schreibt assets/deadzonemc/animations/entity/<name>.animation.json
"""
import json
import math
import os

WURZEL = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
ZIEL = os.path.join(WURZEL, "src", "main", "resources", "assets", "deadzonemc", "animations", "entity")


class Anim:
    def __init__(self, name, laenge, schleife=True, schritte=8):
        self.name = name
        self.laenge = laenge
        self.schleife = schleife
        self.schritte = schritte
        self.kanaele = {}  # (knochen, art) -> {zeit: [x,y,z]}

    def _kanal(self, knochen, art):
        return self.kanaele.setdefault((knochen, art), {})

    def setze(self, knochen, art, zeit, wert, addieren=True):
        k = self._kanal(knochen, art)
        zeit = round(zeit, 4)
        alt = k.get(zeit, [0.0, 0.0, 0.0] if art != "scale" else [1.0, 1.0, 1.0])
        if addieren:
            k[zeit] = [a + b for a, b in zip(alt, wert)] if art != "scale" else [a * b for a, b in zip(alt, wert)]
        else:
            k[zeit] = list(wert)
        return self

    def welle(self, knochen, achse, amp, phase=0.0, mitte=0.0, art="rotation", frequenz=1, form="sin"):
        """Periodische Bewegung über die ganze Länge (passt nahtlos in die Schleife)."""
        i = "xyz".index(achse)
        for s in range(self.schritte + 1):
            t = self.laenge * s / self.schritte
            w = 2 * math.pi * (frequenz * s / self.schritte + phase)
            v = math.sin(w) if form == "sin" else (abs(math.sin(w)) * 2 - 1)
            wert = [0.0, 0.0, 0.0]
            wert[i] = mitte + amp * v
            if art == "scale":
                wert = [1.0, 1.0, 1.0]
                wert[i] = 1 + mitte + amp * v
                self.setze(knochen, art, t, wert)
            else:
                self.setze(knochen, art, t, wert)
        return self

    def tasten(self, knochen, art, schluessel):
        """Einzelne Schlüsselbilder: {zeit: (x, y, z)}."""
        for t, w in schluessel.items():
            self.setze(knochen, art, t, w)
        return self

    def json(self):
        knochen = {}
        for (kn, art), k in self.kanaele.items():
            werte = {}
            for t in sorted(k):
                x, y, z = k[t]
                if art == "rotation":
                    x, y = -x, -y
                elif art == "position":
                    x = -x
                werte[f"{t:g}"] = [round(x, 2), round(y, 2), round(z, 2)]
            knochen.setdefault(kn, {})[art] = werte
        d = {"animation_length": self.laenge, "bones": knochen}
        if self.schleife:
            d["loop"] = True
        else:
            d["loop"] = "hold_on_last_frame" if self.schleife is None else False
        return d


def gehen(a, beine, arme=(), amp_bein=25, amp_arm=12, arm_mitte=0.0, wippen=None, frequenz=1):
    """Einfacher Gang: Beinpaare gegenläufig, Arme gegen die Beine."""
    for i, b in enumerate(beine):
        a.welle(b, "x", amp_bein, phase=0.5 * i, frequenz=frequenz)
    for i, b in enumerate(arme):
        a.welle(b, "x", amp_arm, phase=0.5 * (i + 1), mitte=arm_mitte, frequenz=frequenz)
    if wippen:
        knochen, hoehe = wippen
        a.welle(knochen, "y", hoehe, art="position", frequenz=2 * frequenz, form="abs")


def atmen(a, knochen, amp=2.0, achse="x"):
    a.welle(knochen, achse, amp)


# ---------------------------------------------------------------------------
def wanderer():
    idle = Anim("idle", 3.2)
    atmen(idle, "oberkoerper", 2.5)
    idle.welle("kopf", "z", 6, frequenz=1)
    idle.welle("kiefer", "x", 6, mitte=0, frequenz=2)
    idle.welle("arm_rechts", "x", 4, phase=0.3)
    idle.welle("arm_links", "x", 3, phase=0.7)
    gang = Anim("walk", 1.8)  # schlurfen, linkes Bein zieht nach
    gang.welle("bein_rechts", "x", 22)
    gang.welle("bein_links", "x", 12, phase=0.5)
    gang.welle("arm_rechts", "x", 8, phase=0.5)
    gang.welle("arm_links", "x", 6)
    gang.welle("oberkoerper", "z", 5)
    gang.welle("kopf", "z", 8, phase=0.25)
    gang.welle("koerper", "y", 0.6, art="position", frequenz=2, form="abs")
    lauf = Anim("run", 0.6)
    gehen(lauf, ["bein_rechts", "bein_links"], ["arm_rechts", "arm_links"], 45, 20, arm_mitte=70,
          wippen=("koerper", 1.2))
    lauf.welle("oberkoerper", "x", 4, mitte=-14, frequenz=2)
    lauf.welle("kopf", "x", 6, mitte=-10, frequenz=2)
    angriff = Anim("attack", 0.55, schleife=False)
    for arm, v in (("arm_rechts", 0), ("arm_links", 0.06)):
        angriff.tasten(arm, "rotation", {0: (0, 0, 0), 0.15 + v: (150, 0, 0), 0.3 + v: (40, 0, 0), 0.55: (0, 0, 0)})
    angriff.tasten("oberkoerper", "rotation", {0: (0, 0, 0), 0.2: (8, 0, 0), 0.35: (-18, 0, 0), 0.55: (0, 0, 0)})
    angriff.tasten("kiefer", "rotation", {0: (0, 0, 0), 0.2: (-25, 0, 0), 0.55: (0, 0, 0)})
    return [idle, gang, lauf, angriff]


def schreier():
    idle = Anim("idle", 2.4)
    atmen(idle, "oberkoerper", 3)
    idle.welle("kopf", "x", 5)
    idle.welle("kiefer", "x", 8, frequenz=2)
    idle.welle("finger_rechts", "x", 10, frequenz=3)
    idle.welle("finger_links", "x", 10, phase=0.3, frequenz=3)
    gang = Anim("walk", 1.2)
    gehen(gang, ["bein_rechts", "bein_links"], ["arm_rechts", "arm_links"], 28, 14, wippen=("koerper", 0.8))
    gang.welle("hals", "z", 6)
    lauf = Anim("run", 0.55)
    gehen(lauf, ["bein_rechts", "bein_links"], ["arm_rechts", "arm_links"], 50, 35, wippen=("koerper", 1.4))
    lauf.welle("oberkoerper", "x", 3, mitte=-12, frequenz=2)
    schrei = Anim("special", 1.8, schleife=False, schritte=18)
    schrei.tasten("kopf", "rotation", {0: (0, 0, 0), 0.25: (-30, 0, 0), 0.45: (25, 0, 0), 1.5: (25, 0, 0), 1.8: (0, 0, 0)})
    schrei.tasten("kiefer", "rotation", {0: (0, 0, 0), 0.4: (-50, 0, 0), 1.5: (-50, 0, 0), 1.8: (0, 0, 0)})
    schrei.tasten("oberkoerper", "rotation", {0: (0, 0, 0), 0.3: (-15, 0, 0), 0.45: (18, 0, 0), 1.5: (18, 0, 0), 1.8: (0, 0, 0)})
    for arm, s in (("arm_rechts", 1), ("arm_links", -1)):
        schrei.tasten(arm, "rotation", {0: (0, 0, 0), 0.45: (-20, 0, s * 55), 1.5: (-20, 0, s * 55), 1.8: (0, 0, 0)})
    for i in range(10):  # Zittern
        t = 0.5 + i * 0.1
        schrei.setze("kopf", "rotation", t, (0, (-1) ** i * 6, (-1) ** i * 4))
    angriff = Anim("attack", 0.5, schleife=False)
    angriff.tasten("arm_rechts", "rotation", {0: (0, 0, 0), 0.15: (130, 0, 10), 0.3: (30, 0, 0), 0.5: (0, 0, 0)})
    angriff.tasten("kiefer", "rotation", {0: (0, 0, 0), 0.2: (-40, 0, 0), 0.5: (0, 0, 0)})
    return [idle, gang, lauf, schrei, angriff]


def lauerer():
    idle = Anim("idle", 2.8)
    atmen(idle, "oberkoerper", 3)
    idle.welle("kopf", "z", 10, frequenz=1)
    idle.welle("krallen_rechts", "x", 12, frequenz=3)
    idle.welle("krallen_links", "x", 12, phase=0.4, frequenz=3)
    gang = Anim("walk", 0.9)  # kriechendes Krabbeln auf allen vieren
    gang.welle("arm_rechts", "x", 22)
    gang.welle("arm_links", "x", 22, phase=0.5)
    gang.welle("oberschenkel_rechts", "x", 14, phase=0.5)
    gang.welle("oberschenkel_links", "x", 14)
    gang.welle("kopf", "z", 12, phase=0.25)
    gang.welle("koerper", "y", 0.8, art="position", frequenz=2, form="abs")
    lauf = Anim("run", 0.5)
    lauf.welle("arm_rechts", "x", 32)
    lauf.welle("arm_links", "x", 32, phase=0.5)
    lauf.welle("oberschenkel_rechts", "x", 20, phase=0.5)
    lauf.welle("oberschenkel_links", "x", 20)
    lauf.welle("koerper", "y", 1.5, art="position", frequenz=2, form="abs")
    schreck = Anim("special", 0.9, schleife=False)  # Hervorspringen
    schreck.tasten("koerper", "position", {0: (0, -4, 0), 0.15: (0, 3, -6), 0.5: (0, 0, -2), 0.9: (0, 0, 0)})
    schreck.tasten("oberkoerper", "rotation", {0: (-20, 0, 0), 0.15: (35, 0, 0), 0.5: (10, 0, 0), 0.9: (0, 0, 0)})
    schreck.tasten("kopf", "rotation", {0: (0, 0, 0), 0.15: (-25, 0, -20), 0.3: (-10, 0, 25), 0.45: (-20, 0, -15), 0.9: (0, 0, 0)})
    for arm, s in (("arm_rechts", 1), ("arm_links", -1)):
        schreck.tasten(arm, "rotation", {0: (0, 0, 0), 0.15: (-60, 0, s * 50), 0.6: (-30, 0, s * 30), 0.9: (0, 0, 0)})
    angriff = Anim("attack", 0.45, schleife=False)
    angriff.tasten("arm_rechts", "rotation", {0: (0, 0, 0), 0.12: (-50, 0, 0), 0.25: (40, 0, 0), 0.45: (0, 0, 0)})
    angriff.tasten("arm_links", "rotation", {0: (0, 0, 0), 0.18: (-50, 0, 0), 0.32: (40, 0, 0), 0.45: (0, 0, 0)})
    return [idle, gang, lauf, schreck, angriff]


def kriecher():
    idle = Anim("idle", 2.0)
    idle.welle("kopf", "z", 14, frequenz=1)
    idle.welle("rumpf", "y", 0.4, art="position", frequenz=2)
    for i, n in enumerate(("v_rechts", "v_links", "h_rechts", "h_links")):
        idle.welle("glied_u_" + n, "z", 3, phase=i * 0.27)
    gang = Anim("walk", 0.7)
    for n, ph in (("v_rechts", 0), ("h_links", 0), ("v_links", 0.5), ("h_rechts", 0.5)):
        gang.welle("glied_o_" + n, "y", 22, phase=ph)
        gang.welle("glied_o_" + n, "z", 8, phase=ph + 0.25)
    gang.welle("rumpf", "y", 0.8, art="position", frequenz=2, form="abs")
    gang.welle("kopf", "z", 10, frequenz=2)
    lauf = Anim("run", 0.4)
    for n, ph in (("v_rechts", 0), ("h_links", 0), ("v_links", 0.5), ("h_rechts", 0.5)):
        lauf.welle("glied_o_" + n, "y", 30, phase=ph)
        lauf.welle("glied_o_" + n, "z", 12, phase=ph + 0.25)
    lauf.welle("rumpf", "y", 1.2, art="position", frequenz=2, form="abs")
    angriff = Anim("attack", 0.5, schleife=False)
    for n in ("v_rechts", "v_links"):
        s = 1 if "rechts" in n else -1
        angriff.tasten("glied_o_" + n, "rotation", {0: (0, 0, 0), 0.15: (-60, 0, s * 30), 0.3: (20, 0, -s * 10), 0.5: (0, 0, 0)})
    angriff.tasten("rumpf", "rotation", {0: (0, 0, 0), 0.15: (15, 0, 0), 0.3: (-8, 0, 0), 0.5: (0, 0, 0)})
    return [idle, gang, lauf, angriff]


def der_stille():
    # Fast reglos: der Schrecken ist, dass er sich nicht bewegt, während man hinsieht.
    idle = Anim("idle", 6.0)
    idle.welle("kopf", "z", 3)
    gang = Anim("walk", 1.6)  # gleitet ohne Schritt, nur ein leichtes Wiegen
    gang.welle("bein_rechts", "x", 6)
    gang.welle("bein_links", "x", 6, phase=0.5)
    gang.welle("oberkoerper", "z", 2)
    gang.welle("kopf", "z", 5, phase=0.25)
    angriff = Anim("attack", 0.35, schleife=False)
    angriff.tasten("arm_rechts", "rotation", {0: (0, 0, 0), 0.1: (120, 0, 0), 0.2: (60, 0, 0), 0.35: (0, 0, 0)})
    angriff.tasten("kopf", "rotation", {0: (0, 0, 0), 0.1: (0, 0, -25), 0.35: (0, 0, 0)})
    return [idle, gang, angriff]


def imitator():
    idle = Anim("idle", 4.0)
    idle.welle("kopf", "z", 8)
    idle.welle("arm_links", "z", 10, frequenz=2)  # winkt langsam
    gang = Anim("walk", 1.0)  # bewusst wie ein Spieler
    gehen(gang, ["bein_rechts", "bein_links"], ["arm_rechts"], 30, 25, wippen=("koerper", 0.5))
    gang.welle("kopf", "z", 6, phase=0.25)
    lauf = Anim("run", 0.45)
    gehen(lauf, ["bein_rechts", "bein_links"], ["arm_rechts", "arm_links"], 55, 50, wippen=("koerper", 1.2))
    lauf.welle("kopf", "x", 25, frequenz=2)
    lauf.welle("hals", "z", 18, frequenz=1)
    angriff = Anim("attack", 0.5, schleife=False)
    angriff.tasten("arm_rechts", "rotation", {0: (0, 0, 0), 0.15: (140, 0, 0), 0.3: (20, 0, 0), 0.5: (0, 0, 0)})
    angriff.tasten("kopf", "rotation", {0: (0, 0, 0), 0.15: (-30, 0, -40), 0.5: (0, 0, 0)})
    return [idle, gang, lauf, angriff]


def nebelwesen():
    idle = Anim("idle", 3.0)
    idle.welle("koerper", "y", 1.0, art="position")
    for i in range(4):
        idle.welle(f"faden_{i}", "x", 10, phase=i * 0.23)
        idle.welle(f"faden_{i}", "z", 6, phase=i * 0.31 + 0.1)
    idle.welle("arm_rechts", "x", 6)
    idle.welle("arm_links", "x", 6, phase=0.4)
    gang = Anim("walk", 2.0)
    gang.welle("koerper", "y", 1.2, art="position")
    gang.welle("koerper", "x", 4, mitte=-6)
    for i in range(4):
        gang.welle(f"faden_{i}", "x", 14, mitte=-15, phase=i * 0.2)
    angriff = Anim("attack", 0.6, schleife=False)
    for arm, v in (("arm_rechts", 0), ("arm_links", 0.1)):
        angriff.tasten(arm, "rotation", {0: (0, 0, 0), 0.15 + v: (-50, 0, 0), 0.35 + v: (45, 0, 0), 0.6: (0, 0, 0)})
    return [idle, gang, angriff]


def schwarm():
    idle = Anim("idle", 1.2)
    idle.welle("schwanz", "y", 15)
    idle.welle("kopf", "x", 6, frequenz=2)
    gang = Anim("walk", 0.3)
    for i in range(3):
        gang.welle(f"bein_{i}_r", "x", 30, phase=0.5 * (i % 2))
        gang.welle(f"bein_{i}_l", "x", 30, phase=0.5 * ((i + 1) % 2))
    gang.welle("schwanz", "y", 20)
    gang.welle("koerper", "y", 0.3, art="position", frequenz=2, form="abs")
    angriff = Anim("attack", 0.3, schleife=False)
    angriff.tasten("kiefer", "rotation", {0: (0, 0, 0), 0.1: (-45, 0, 0), 0.2: (0, 0, 0), 0.3: (0, 0, 0)})
    angriff.tasten("kopf", "rotation", {0: (0, 0, 0), 0.1: (-15, 0, 0), 0.2: (10, 0, 0), 0.3: (0, 0, 0)})
    return [idle, gang, angriff]


def brut():
    idle = Anim("idle", 2.6)
    idle.welle("koerper", "x", 0.04, art="scale")
    idle.welle("koerper", "z", 0.04, art="scale")
    idle.welle("kopf", "z", 8)
    idle.welle("arm_rechts", "z", 8)
    idle.welle("arm_links", "z", 8, phase=0.5)
    gang = Anim("walk", 1.4)  # watscheln
    gang.welle("koerper", "z", 7)
    gang.welle("bein_rechts", "x", 18)
    gang.welle("bein_links", "x", 18, phase=0.5)
    gang.welle("koerper", "y", 0.6, art="position", frequenz=2, form="abs")
    platzen = Anim("special", 1.5, schleife=None)
    platzen.tasten("koerper", "scale", {0: (1, 1, 1), 0.6: (1.12, 1.05, 1.12), 0.9: (1.08, 1.02, 1.08),
                                         1.3: (1.35, 1.2, 1.35), 1.5: (1.45, 1.25, 1.45)})
    for i in range(8):
        platzen.setze("koerper", "rotation", 0.5 + i * 0.12, ((-1) ** i * 3, 0, (-1) ** (i + 1) * 4), addieren=False)
    angriff = Anim("attack", 0.5, schleife=False)
    angriff.tasten("oberkoerper", "rotation", {0: (0, 0, 0), 0.2: (20, 0, 0), 0.35: (-25, 0, 0), 0.5: (0, 0, 0)})
    return [idle, gang, platzen, angriff]


def juggernaut():
    idle = Anim("idle", 3.5)
    atmen(idle, "oberkoerper", 2.5)
    idle.welle("arm_rechts", "z", 2)
    idle.welle("arm_links", "z", 2, phase=0.5)
    idle.welle("kopf", "y", 10, frequenz=1)
    gang = Anim("walk", 2.2)
    gehen(gang, ["bein_rechts", "bein_links"], ["arm_rechts", "arm_links"], 18, 10, wippen=("koerper", 1.0))
    gang.welle("oberkoerper", "z", 4)
    gang.welle("oberkoerper", "y", 6, phase=0.25)
    lauf = Anim("run", 1.2)  # schwerer Ansturm
    gehen(lauf, ["bein_rechts", "bein_links"], ["arm_rechts", "arm_links"], 30, 22, wippen=("koerper", 1.8))
    lauf.welle("oberkoerper", "x", 3, mitte=-10, frequenz=2)
    schlag = Anim("attack", 1.1, schleife=False)
    for arm in ("arm_rechts", "arm_links"):
        schlag.tasten(arm, "rotation", {0: (0, 0, 0), 0.45: (170, 0, 0), 0.65: (10, 0, 0), 1.1: (0, 0, 0)})
    schlag.tasten("oberkoerper", "rotation", {0: (0, 0, 0), 0.45: (18, 0, 0), 0.65: (-25, 0, 0), 1.1: (0, 0, 0)})
    schlag.tasten("koerper", "position", {0: (0, 0, 0), 0.45: (0, 1, 0), 0.65: (0, -2, 0), 1.1: (0, 0, 0)})
    brull = Anim("special", 2.0, schleife=False)
    brull.tasten("oberkoerper", "rotation", {0: (0, 0, 0), 0.4: (25, 0, 0), 1.6: (25, 0, 0), 2.0: (0, 0, 0)})
    brull.tasten("kopf", "rotation", {0: (0, 0, 0), 0.4: (30, 0, 0), 1.6: (30, 0, 0), 2.0: (0, 0, 0)})
    for arm, s in (("arm_rechts", 1), ("arm_links", -1)):
        brull.tasten(arm, "rotation", {0: (0, 0, 0), 0.4: (-20, 0, s * 40), 1.6: (-20, 0, s * 40), 2.0: (0, 0, 0)})
    return [idle, gang, lauf, schlag, brull]


def mensch():
    """Gemeinsame Animationen aller Menschen (Soldat, Kultist, Händler, Überlebender)."""
    idle = Anim("idle", 4.0)
    atmen(idle, "oberkoerper", 1.2)
    idle.welle("kopf", "y", 12, frequenz=1)
    idle.welle("arm_rechts", "z", 2)
    idle.welle("arm_links", "z", 2, phase=0.5)
    gang = Anim("walk", 1.0)
    gehen(gang, ["bein_rechts", "bein_links"], ["arm_rechts", "arm_links"], 30, 22, wippen=("koerper", 0.5))
    lauf = Anim("run", 0.6)
    gehen(lauf, ["bein_rechts", "bein_links"], ["arm_rechts", "arm_links"], 50, 40, wippen=("koerper", 1.0))
    lauf.welle("oberkoerper", "x", 2, mitte=-8, frequenz=2)
    angriff = Anim("attack", 0.5, schleife=False)
    angriff.tasten("arm_rechts", "rotation", {0: (0, 0, 0), 0.12: (110, 0, 0), 0.3: (30, 0, 0), 0.5: (0, 0, 0)})
    angriff.tasten("oberkoerper", "rotation", {0: (0, 0, 0), 0.12: (0, -15, 0), 0.5: (0, 0, 0)})
    schuss = Anim("special", 0.35, schleife=False)  # Rückstoß beim Schießen
    for arm in ("arm_rechts", "arm_links"):
        schuss.tasten(arm, "rotation", {0: (0, 0, 0), 0.05: (12, 0, 0), 0.35: (0, 0, 0)})
    schuss.tasten("oberkoerper", "rotation", {0: (0, 0, 0), 0.05: (4, 0, 0), 0.35: (0, 0, 0)})
    return [idle, gang, lauf, angriff, schuss]


ALLE = {"mensch": mensch, "wanderer": wanderer, "schreier": schreier, "lauerer": lauerer, "kriecher": kriecher,
        "der_stille": der_stille, "imitator": imitator, "nebelwesen": nebelwesen, "schwarm": schwarm,
        "brut": brut, "juggernaut": juggernaut}


# ---------------------------------------------------------------------------
# Zweite Welle

def gefallener_soldat():
    idle = Anim("idle", 3.6)
    atmen(idle, "oberkoerper", 2)
    idle.welle("kopf", "z", 5)
    idle.welle("unterarm_links", "x", 6, phase=0.3)  # der gebrochene Arm baumelt
    idle.welle("gewehr", "x", 3, phase=0.6)
    gang = Anim("walk", 2.0)  # humpelnd, das linke Bein wird nachgezogen
    gang.welle("bein_rechts", "x", 20)
    gang.welle("bein_links", "x", 6, phase=0.5)
    gang.welle("koerper", "z", 4)
    gang.welle("unterarm_links", "x", 14, phase=0.25)
    gang.welle("arm_links", "x", 8, phase=0.5)
    gang.welle("gewehr", "x", 6, phase=0.5)
    gang.welle("kopf", "z", 7, phase=0.25)
    gang.welle("koerper", "y", 0.8, art="position", frequenz=2, form="abs")
    lauf = Anim("run", 1.0)
    gehen(lauf, ["bein_rechts", "bein_links"], ["arm_rechts"], 32, 14, wippen=("koerper", 1.2))
    lauf.welle("unterarm_links", "x", 25, phase=0.3, frequenz=2)
    lauf.welle("oberkoerper", "x", 3, mitte=-12, frequenz=2)
    schlag = Anim("attack", 0.8, schleife=False)  # holt mit dem Gewehrkolben aus
    schlag.tasten("arm_rechts", "rotation", {0: (0, 0, 0), 0.3: (-60, 0, 20), 0.5: (110, 0, -10), 0.8: (0, 0, 0)})
    schlag.tasten("oberkoerper", "rotation", {0: (0, 0, 0), 0.3: (0, 25, 0), 0.5: (-10, -20, 0), 0.8: (0, 0, 0)})
    return [idle, gang, lauf, schlag]


def glutleib():
    idle = Anim("idle", 2.8)
    atmen(idle, "oberkoerper", 3)
    for i in range(3):
        idle.welle(f"schlot_{i}", "z", 4, phase=i * 0.3)
    idle.welle("kopf", "y", 8)
    gang = Anim("walk", 1.6)  # Knöchelgang: Arme und Beine im Kreuzgang
    gehen(gang, ["bein_rechts", "bein_links"], ["arm_links", "arm_rechts"], 22, 26, wippen=("koerper", 1.2))
    gang.welle("oberkoerper", "z", 5)
    lauf = Anim("run", 0.8)
    gehen(lauf, ["bein_rechts", "bein_links"], ["arm_links", "arm_rechts"], 35, 40, wippen=("koerper", 2.0))
    lauf.welle("oberkoerper", "x", 4, mitte=-6, frequenz=2)
    schlag = Anim("attack", 0.9, schleife=False)  # beidhändiger Hammerschlag
    for arm in ("arm_rechts", "arm_links"):
        schlag.tasten(arm, "rotation", {0: (0, 0, 0), 0.35: (-80, 0, 0), 0.55: (40, 0, 0), 0.9: (0, 0, 0)})
    schlag.tasten("oberkoerper", "rotation", {0: (0, 0, 0), 0.35: (20, 0, 0), 0.55: (-15, 0, 0), 0.9: (0, 0, 0)})
    ausbruch = Anim("special", 1.5, schleife=False)  # Glut bricht aus den Schloten
    ausbruch.tasten("oberkoerper", "rotation", {0: (0, 0, 0), 0.5: (30, 0, 0), 1.0: (30, 0, 0), 1.5: (0, 0, 0)})
    for i in range(3):
        ausbruch.tasten(f"schlot_{i}", "scale", {0: (1, 1, 1), 0.5: (1.3, 1.4, 1.3), 1.0: (1.3, 1.4, 1.3), 1.5: (1, 1, 1)})
    return [idle, gang, lauf, schlag, ausbruch]


def seuchenhund():
    idle = Anim("idle", 2.0)
    atmen(idle, "koerper", 1.5)
    idle.welle("kopf", "y", 12)
    idle.welle("kiefer", "x", 8, frequenz=2)
    idle.welle("schwanz", "y", 15, frequenz=2)
    gang = Anim("walk", 0.9)
    for b, ph in (("bein_v_r", 0), ("bein_h_l", 0), ("bein_v_l", 0.5), ("bein_h_r", 0.5)):
        gang.welle(b, "x", 28, phase=ph)
    gang.welle("kopf", "x", 4, frequenz=2)
    gang.welle("koerper", "y", 0.5, art="position", frequenz=2, form="abs")
    lauf = Anim("run", 0.45)  # Galopp
    for b, ph in (("bein_v_r", 0), ("bein_v_l", 0.1), ("bein_h_r", 0.5), ("bein_h_l", 0.6)):
        lauf.welle(b, "x", 50, phase=ph)
    lauf.welle("koerper", "x", 8, frequenz=1)
    lauf.welle("koerper", "y", 1.5, art="position", frequenz=1, form="abs")
    lauf.welle("kiefer", "x", 10, mitte=15, frequenz=2)
    biss = Anim("attack", 0.45, schleife=False)
    biss.tasten("kopf", "rotation", {0: (0, 0, 0), 0.12: (-20, 0, 0), 0.25: (25, 0, 0), 0.45: (0, 0, 0)})
    biss.tasten("kiefer", "rotation", {0: (0, 0, 0), 0.12: (45, 0, 0), 0.25: (-10, 0, 0), 0.45: (0, 0, 0)})
    biss.tasten("koerper", "position", {0: (0, 0, 0), 0.2: (0, 0, -3), 0.45: (0, 0, 0)})
    knurren = Anim("special", 1.2, schleife=False)
    knurren.tasten("kopf", "rotation", {0: (0, 0, 0), 0.3: (15, 0, 0), 0.9: (15, 0, 0), 1.2: (0, 0, 0)})
    knurren.tasten("kiefer", "rotation", {0: (0, 0, 0), 0.3: (25, 0, 0), 0.9: (25, 0, 0), 1.2: (0, 0, 0)})
    knurren.welle("koerper", "z", 2, frequenz=6)
    return [idle, gang, lauf, biss, knurren]


def wuehler():
    idle = Anim("idle", 2.4)
    atmen(idle, "oberkoerper", 3)
    idle.welle("kopf", "y", 18)  # lauscht
    idle.welle("schaufel_rechts", "x", 6)
    gang = Anim("walk", 1.2)
    gehen(gang, ["bein_rechts", "bein_links"], ["arm_rechts", "arm_links"], 25, 30, wippen=("koerper", 1.0))
    lauf = Anim("run", 0.6)
    gehen(lauf, ["bein_rechts", "bein_links"], ["arm_rechts", "arm_links"], 40, 45, wippen=("koerper", 1.6))
    graben = Anim("special", 1.6, schleife=False)  # gräbt sich ein
    graben.welle("arm_rechts", "x", 50, frequenz=3)
    graben.welle("arm_links", "x", 50, phase=0.5, frequenz=3)
    graben.welle("oberkoerper", "x", 8, mitte=12, frequenz=3)
    graben.tasten("koerper", "position", {0: (0, 0, 0), 1.6: (0, -26, 0)})
    schlag = Anim("attack", 0.6, schleife=False)
    for arm, v in (("arm_rechts", 0), ("arm_links", 0.1)):
        schlag.tasten(arm, "rotation", {0: (0, 0, 0), 0.2 + v: (-60, 0, 0), 0.35 + v: (60, 0, 0), 0.6: (0, 0, 0)})
    return [idle, gang, lauf, schlag, graben]


def sauger():
    idle = Anim("idle", 3.0)
    atmen(idle, "oberkoerper", 2)
    for i in range(5):
        idle.welle(f"tentakel_{i}", "x", 10, phase=i * 0.2)
        idle.welle(f"tentakel_{i}", "z", 6, phase=i * 0.2 + 0.25)
    idle.welle("unterarm_rechts", "x", 4)
    idle.welle("unterarm_links", "x", 4, phase=0.5)
    gang = Anim("walk", 1.4)  # pirscht
    gehen(gang, ["schenkel_rechts", "schenkel_links"], (), 18, wippen=("koerper", 0.6))
    gang.welle("kopf", "y", 10, phase=0.25)
    for i in range(5):
        gang.welle(f"tentakel_{i}", "x", 12, phase=i * 0.2)
    lauf = Anim("run", 0.6)
    gehen(lauf, ["schenkel_rechts", "schenkel_links"], (), 40, wippen=("koerper", 1.5))
    lauf.welle("oberkoerper", "x", 4, mitte=-10, frequenz=2)
    zugriff = Anim("attack", 0.7, schleife=False)  # Klauen schnellen vor wie bei einer Gottesanbeterin
    for seite in ("rechts", "links"):
        zugriff.tasten(f"unterarm_{seite}", "rotation", {0: (0, 0, 0), 0.15: (-120, 0, 0), 0.4: (-100, 0, 0), 0.7: (0, 0, 0)})
        zugriff.tasten(f"oberarm_{seite}", "rotation", {0: (0, 0, 0), 0.15: (40, 0, 0), 0.7: (0, 0, 0)})
    saugen = Anim("special", 2.0, schleife=False)  # Tentakel saugen
    for i in range(5):
        saugen.welle(f"tentakel_{i}", "x", 25, mitte=-30, phase=i * 0.15, frequenz=3)
    saugen.welle("kopf", "x", 5, mitte=10, frequenz=3)
    return [idle, gang, lauf, zugriff, saugen]


def seuchentraeger():
    idle = Anim("idle", 3.4)
    atmen(idle, "oberkoerper", 1.5)
    idle.welle("kopf", "y", 10)
    idle.welle("lanze", "x", 4)
    idle.welle("tank", "y", 0.3, art="position", frequenz=2)
    gang = Anim("walk", 1.8)  # schwerfällig, watschelnd
    gehen(gang, ["bein_rechts", "bein_links"], ["arm_links"], 18, 10, wippen=("koerper", 0.8))
    gang.welle("koerper", "z", 6)
    gang.welle("lanze", "x", 5, phase=0.5)
    lauf = Anim("run", 1.0)
    gehen(lauf, ["bein_rechts", "bein_links"], ["arm_links"], 28, 20, wippen=("koerper", 1.4))
    lauf.welle("koerper", "z", 8)
    spruehen = Anim("attack", 1.0, schleife=False)
    spruehen.tasten("arm_rechts", "rotation", {0: (0, 0, 0), 0.2: (25, -15, 0), 0.8: (25, 15, 0), 1.0: (0, 0, 0)})
    spruehen.tasten("lanze", "rotation", {0: (0, 0, 0), 0.2: (-15, 0, 0), 0.8: (-15, 0, 0), 1.0: (0, 0, 0)})
    husten = Anim("special", 1.4, schleife=False)
    husten.tasten("oberkoerper", "rotation", {0: (0, 0, 0), 0.2: (-25, 0, 0), 0.4: (10, 0, 0), 0.6: (-25, 0, 0),
                                              0.8: (10, 0, 0), 1.4: (0, 0, 0)})
    return [idle, gang, lauf, spruehen, husten]


def ertrunkener():
    idle = Anim("idle", 4.0)
    atmen(idle, "oberkoerper", 2)
    idle.welle("kiefer", "x", 8, frequenz=1)
    for i in range(6):
        idle.welle(f"strang_{i}", "z", 8, phase=i * 0.17)
    idle.welle("arm_rechts", "z", 4)
    gang = Anim("walk", 2.4)  # watschelt, der Bauch schwappt
    gehen(gang, ["bein_rechts", "bein_links"], (), 16, wippen=("koerper", 0.6))
    gang.welle("oberkoerper", "z", 7)
    gang.welle("arm_rechts", "x", 12, phase=0.5)
    gang.welle("kopf", "z", 6, phase=0.5)
    lauf = Anim("run", 1.4)
    gehen(lauf, ["bein_rechts", "bein_links"], (), 26, wippen=("koerper", 1.0))
    lauf.welle("oberkoerper", "z", 9)
    packen = Anim("attack", 0.8, schleife=False)
    packen.tasten("arm_rechts", "rotation", {0: (0, 0, 0), 0.25: (-110, 0, 0), 0.5: (-30, 0, 0), 0.8: (0, 0, 0)})
    packen.tasten("kiefer", "rotation", {0: (0, 0, 0), 0.25: (30, 0, 0), 0.8: (0, 0, 0)})
    schwimmen = Anim("special", 1.6, schleife=False)  # unter Wasser
    schwimmen.welle("bein_rechts", "x", 20, frequenz=2)
    schwimmen.welle("bein_links", "x", 20, phase=0.5, frequenz=2)
    schwimmen.welle("arm_rechts", "x", 40, mitte=-60)
    schwimmen.tasten("koerper", "rotation", {0: (-70, 0, 0), 1.6: (-70, 0, 0)})
    return [idle, gang, lauf, packen, schwimmen]


def spaeherdrohne():
    idle = Anim("idle", 0.25)  # Rotoren drehen sich ständig
    for i in range(4):
        idle.tasten(f"rotor_{i}", "rotation", {0: (0, 0, 0), 0.25: (0, 360 if i % 2 else -360, 0)})
    idle.welle("rumpf", "y", 0.4, art="position")
    gang = Anim("walk", 0.25)
    for i in range(4):
        gang.tasten(f"rotor_{i}", "rotation", {0: (0, 0, 0), 0.25: (0, 360 if i % 2 else -360, 0)})
    gang.tasten("rumpf", "rotation", {0: (-10, 0, 0), 0.25: (-10, 0, 0)})
    scan = Anim("special", 2.0, schleife=False)  # Kamera schwenkt
    scan.tasten("kamera", "rotation", {0: (0, -40, 0), 1.0: (0, 40, 0), 2.0: (0, -40, 0)})
    for i in range(4):
        scan.tasten(f"rotor_{i}", "rotation", {0: (0, 0, 0), 2.0: (0, 2880 if i % 2 else -2880, 0)})
    alarm = Anim("attack", 0.6, schleife=False)
    alarm.tasten("kamera", "rotation", {0: (0, 0, 0), 0.1: (20, 0, 0), 0.6: (0, 0, 0)})
    return [idle, gang, scan, alarm]


def lichtmotte():
    idle = Anim("idle", 0.4)  # Flügelschlag
    for seite, s in (("rechts", 1), ("links", -1)):
        idle.welle(f"fluegel_{seite}", "z", 35 * s, phase=0)
        idle.welle(f"hinterfluegel_{seite}", "z", 25 * s, phase=0.1)
    idle.welle("koerper", "y", 1.2, art="position")
    idle.welle("fuehler_rechts", "x", 8)
    idle.welle("fuehler_links", "x", 8, phase=0.5)
    gang = Anim("walk", 0.3)
    for seite, s in (("rechts", 1), ("links", -1)):
        gang.welle(f"fluegel_{seite}", "z", 45 * s)
        gang.welle(f"hinterfluegel_{seite}", "z", 35 * s, phase=0.1)
    gang.tasten("koerper", "rotation", {0: (15, 0, 0), 0.3: (15, 0, 0)})
    staub = Anim("attack", 0.8, schleife=False)
    for seite, s in (("rechts", 1), ("links", -1)):
        staub.tasten(f"fluegel_{seite}", "rotation", {0: (0, 0, 0), 0.2: (0, 0, -70 * s), 0.5: (0, 0, 60 * s), 0.8: (0, 0, 0)})
    return [idle, gang, staub]


def scheintoter():
    idle = Anim("idle", 6.0)  # liegt still, ganz leises Atmen, die Hand zuckt
    idle.welle("oberkoerper", "z", 0.6)
    idle.tasten("hand_rechts", "rotation", {0: (0, 0, 0), 4.6: (0, 0, 0), 4.8: (-25, 0, 0), 5.0: (0, 0, 0), 6.0: (0, 0, 0)})
    gang = Anim("walk", 1.2)  # kriecht auf dem Rücken
    gang.welle("arm_rechts", "x", 30)
    gang.welle("arm_links", "x", 20, phase=0.5)
    gang.welle("bein_rechts", "x", 15, phase=0.5)
    packen = Anim("attack", 0.9, schleife=False)  # richtet sich ruckartig auf und packt zu
    packen.tasten("koerper", "rotation", {0: (0, 0, 0), 0.15: (-55, 0, 0), 0.6: (-55, 0, 0), 0.9: (0, 0, 0)})
    packen.tasten("arm_rechts", "rotation", {0: (0, 0, 0), 0.15: (0, 0, -150), 0.6: (0, 0, -150), 0.9: (0, 0, 0)})
    packen.tasten("kopf", "rotation", {0: (0, 0, 0), 0.15: (0, 65, 0), 0.6: (0, 65, 0), 0.9: (0, 0, 0)})
    steh = Anim("stand", 3.0)
    steh.tasten("koerper", "rotation", {0: (-88, 0, 0), 3.0: (-88, 0, 0)})
    steh.tasten("kopf", "rotation", {0: (0, 65, 0), 3.0: (0, 65, 0)})
    steh.welle("arm_rechts", "z", 6)
    stehgang = Anim("standwalk", 1.6)
    stehgang.tasten("koerper", "rotation", {0: (-88, 0, 0), 1.6: (-88, 0, 0)})
    stehgang.tasten("kopf", "rotation", {0: (0, 65, 0), 1.6: (0, 65, 0)})
    stehgang.welle("bein_rechts", "x", 22)
    stehgang.welle("bein_links", "x", 22, phase=0.5)
    stehgang.welle("arm_rechts", "x", 15, phase=0.5)
    aufstehen = Anim("special", 1.2, schleife=None)
    aufstehen.tasten("koerper", "rotation", {0: (0, 0, 0), 0.4: (-50, 0, 0), 1.2: (-88, 0, 0)})
    aufstehen.tasten("kopf", "rotation", {0: (0, 0, 0), 1.2: (0, 65, 0)})
    return [idle, gang, packen, aufstehen, steh, stehgang]


# ---------------------------------------------------------------------------
# Bosse: jeweils mit eigener Auftritts-Animation ("spawn")

def boss_auftritt(name, knochen="koerper", tiefe=-30, laenge=3.0, extra=None):
    a = Anim("spawn", laenge, schleife=None)
    a.tasten(knochen, "position", {0: (0, tiefe, 0), laenge * 0.8: (0, 2, 0), laenge: (0, 0, 0)})
    if extra:
        extra(a)
    return a


def verschmolzener():
    idle = Anim("idle", 3.0)
    atmen(idle, "masse", 3)
    for i in range(8):
        idle.welle(f"aermchen_{i}", "x", 25, phase=i * 0.13, frequenz=2)
        idle.welle(f"kopf_{i}", "y", 15, phase=i * 0.21)
    gang = Anim("walk", 2.4)
    for i in range(4):
        gang.welle(f"bein_{i}", "x", 18, phase=0.5 * (i % 2))
    gang.welle("masse", "z", 5)
    gang.welle("koerper", "y", 1.5, art="position", frequenz=2, form="abs")
    for i in range(8):
        gang.welle(f"aermchen_{i}", "x", 30, phase=i * 0.13, frequenz=2)
    gang.welle("arm_rechts", "x", 12)
    gang.welle("arm_links", "x", 12, phase=0.5)
    schlag = Anim("attack", 1.2, schleife=False)
    for arm in ("arm_rechts", "arm_links"):
        schlag.tasten(arm, "rotation", {0: (0, 0, 0), 0.5: (-150, 0, 0), 0.75: (20, 0, 0), 1.2: (0, 0, 0)})
    schlag.tasten("masse", "rotation", {0: (0, 0, 0), 0.5: (20, 0, 0), 0.75: (-20, 0, 0), 1.2: (0, 0, 0)})
    # Fusion: die Masse wächst aus einem kleinen Klumpen, Köpfe und Arme stülpen sich nach außen
    fusion = Anim("spawn", 5.0, schleife=None)
    fusion.tasten("koerper", "scale", {0: (0.15, 0.1, 0.15), 1.5: (0.4, 0.35, 0.4), 3.0: (0.75, 0.7, 0.75),
                                       4.2: (1.08, 1.1, 1.08), 5.0: (1, 1, 1)})
    fusion.welle("masse", "z", 10, frequenz=6)
    for i in range(8):
        fusion.tasten(f"kopf_{i}", "scale", {0: (0.1, 0.1, 0.1), 2.0 + i * 0.25: (0.1, 0.1, 0.1), 2.5 + i * 0.25: (1.2, 1.2, 1.2),
                                              5.0: (1, 1, 1)})
        fusion.welle(f"aermchen_{i}", "x", 60, phase=i * 0.1, frequenz=5)
    brull = Anim("special", 2.0, schleife=False)
    brull.tasten("masse", "rotation", {0: (0, 0, 0), 0.4: (25, 0, 0), 1.6: (25, 0, 0), 2.0: (0, 0, 0)})
    for arm, s in (("arm_rechts", 1), ("arm_links", -1)):
        brull.tasten(arm, "rotation", {0: (0, 0, 0), 0.4: (-60, 0, s * 50), 1.6: (-60, 0, s * 50), 2.0: (0, 0, 0)})
    for i in range(8):
        brull.welle(f"aermchen_{i}", "x", 50, phase=i * 0.1, frequenz=6)
    return [idle, gang, schlag, fusion, brull]


def nebelmutter():
    idle = Anim("idle", 5.0)
    atmen(idle, "oberkoerper", 2)
    idle.welle("kopf", "z", 10)
    idle.welle("hals", "x", 4, phase=0.3)
    for i in range(4):
        idle.welle(f"schleier_{i}", "x", 8, phase=i * 0.25)
    idle.welle("arm_rechts", "z", 3)
    idle.welle("arm_links", "z", 3, phase=0.5)
    gang = Anim("walk", 3.0)  # langsame, staksige Schritte
    gang.welle("oberschenkel_rechts", "x", 20)
    gang.welle("oberschenkel_links", "x", 20, phase=0.5)
    gang.welle("arm_rechts", "x", 10, phase=0.5)
    gang.welle("arm_links", "x", 10)
    gang.welle("kopf", "z", 12, phase=0.25)
    for i in range(4):
        gang.welle(f"schleier_{i}", "x", 14, phase=i * 0.25)
    griff = Anim("attack", 1.6, schleife=False)
    griff.tasten("arm_rechts", "rotation", {0: (0, 0, 0), 0.6: (-70, 0, -15), 1.0: (-40, 0, 0), 1.6: (0, 0, 0)})
    griff.tasten("oberkoerper", "rotation", {0: (0, 0, 0), 0.6: (-15, 0, 0), 1.6: (0, 0, 0)})
    gebaeren = Anim("special", 3.0, schleife=False)  # neigt sich, Schleier wallen: Nebelwesen entstehen
    gebaeren.tasten("oberkoerper", "rotation", {0: (0, 0, 0), 1.0: (-30, 0, 0), 2.2: (-30, 0, 0), 3.0: (0, 0, 0)})
    for i in range(4):
        gebaeren.welle(f"schleier_{i}", "x", 35, phase=i * 0.2, frequenz=3)
    auftritt = boss_auftritt("nebelmutter", tiefe=0, laenge=4.0, extra=lambda a: a.tasten(
        "koerper", "scale", {0: (0.6, 0.05, 0.6), 3.0: (1, 1.05, 1), 4.0: (1, 1, 1)}))
    return [idle, gang, griff, gebaeren, auftritt]


def strahlende():
    idle = Anim("idle", 2.6)
    atmen(idle, "oberkoerper", 3)
    for i in range(4):
        idle.welle(f"stab_{i}", "x", 3, phase=i * 0.25)
    idle.welle("kopf", "z", 6)
    gang = Anim("walk", 2.2)
    gehen(gang, ["bein_rechts", "bein_links"], ["arm_links"], 18, 14, wippen=("koerper", 1.6))
    gang.welle("arm_rechts", "x", 8, phase=0.5)  # das Fass schleift
    gang.welle("oberkoerper", "z", 6)
    schlag = Anim("attack", 1.3, schleife=False)  # Fass als Keule
    schlag.tasten("arm_rechts", "rotation", {0: (0, 0, 0), 0.5: (-120, 0, 20), 0.8: (40, 0, -10), 1.3: (0, 0, 0)})
    schlag.tasten("oberkoerper", "rotation", {0: (0, 0, 0), 0.5: (15, 30, 0), 0.8: (-15, -25, 0), 1.3: (0, 0, 0)})
    puls = Anim("special", 2.0, schleife=False)  # Strahlungspuls
    puls.tasten("oberkoerper", "rotation", {0: (0, 0, 0), 0.6: (30, 0, 0), 1.4: (30, 0, 0), 2.0: (0, 0, 0)})
    for i in range(4):
        puls.tasten(f"stab_{i}", "scale", {0: (1, 1, 1), 0.6: (1.2, 1.3, 1.2), 1.4: (1.2, 1.3, 1.2), 2.0: (1, 1, 1)})
    return [idle, gang, schlag, puls, boss_auftritt("strahlende", tiefe=-20)]


def brutkoenigin():
    idle = Anim("idle", 2.2)
    idle.tasten("eiersack", "scale", {0: (1, 1, 1), 1.1: (1.04, 1.06, 1.03), 2.2: (1, 1, 1)})
    idle.welle("oberkoerper", "x", 4)
    for s in ("rechts", "links"):
        idle.welle(f"mandibel_{s}", "y", 10, frequenz=2)
    gang = Anim("walk", 1.4)
    for i in range(3):
        for s, ph in (("r", 0), ("l", 0.5)):
            gang.welle(f"bein_{i}_{s}", "y", 18, phase=ph + i * 0.33)
            gang.welle(f"bein_{i}_{s}", "z", 10, phase=ph + i * 0.33 + 0.25)
    gang.welle("eiersack", "z", 4)
    schlag = Anim("attack", 0.8, schleife=False)
    schlag.tasten("oberkoerper", "rotation", {0: (0, 0, 0), 0.3: (25, 0, 0), 0.5: (-20, 0, 0), 0.8: (0, 0, 0)})
    for s, v in (("rechts", 1), ("links", -1)):
        schlag.tasten(f"mandibel_{s}", "rotation", {0: (0, 0, 0), 0.3: (0, v * 30, 0), 0.5: (0, -v * 10, 0), 0.8: (0, 0, 0)})
    legen = Anim("special", 2.0, schleife=False)  # presst Schwärmlinge heraus
    legen.tasten("eiersack", "scale", {0: (1, 1, 1), 0.8: (1.12, 0.9, 1.15), 1.2: (0.95, 1.05, 0.95), 2.0: (1, 1, 1)})
    legen.tasten("eiersack", "rotation", {0: (0, 0, 0), 0.8: (-10, 0, 0), 2.0: (0, 0, 0)})
    return [idle, gang, schlag, legen, boss_auftritt("brutkoenigin", tiefe=-25)]


def schlaechter():
    idle = Anim("idle", 3.0)
    atmen(idle, "oberkoerper", 2.5)
    idle.welle("kette", "z", 8)
    for i in range(3):
        idle.welle(f"guertelhaken_{i}", "x", 6, phase=i * 0.3)
    idle.welle("kopf", "y", 12)
    gang = Anim("walk", 2.0)
    gehen(gang, ["bein_rechts", "bein_links"], ["arm_links"], 20, 10, wippen=("koerper", 1.2))
    gang.welle("kette", "x", 15, phase=0.5)
    gang.welle("oberkoerper", "z", 5)
    hieb = Anim("attack", 1.0, schleife=False)  # Hackbeil von oben
    hieb.tasten("arm_rechts", "rotation", {0: (0, 0, 0), 0.4: (-170, 0, 0), 0.6: (20, 0, 0), 1.0: (0, 0, 0)})
    hieb.tasten("oberkoerper", "rotation", {0: (0, 0, 0), 0.4: (15, 0, 0), 0.6: (-20, 0, 0), 1.0: (0, 0, 0)})
    wurf = Anim("special", 1.4, schleife=False)  # Haken werfen und heranziehen
    wurf.tasten("arm_links", "rotation", {0: (0, 0, 0), 0.3: (-60, 0, 40), 0.5: (-120, 0, -10), 1.0: (-30, 0, 0), 1.4: (0, 0, 0)})
    wurf.tasten("kette", "rotation", {0: (0, 0, 0), 0.5: (-90, 0, 0), 1.0: (-40, 0, 0), 1.4: (0, 0, 0)})
    wurf.tasten("kette", "scale", {0: (1, 1, 1), 0.5: (1, 2.5, 1), 1.0: (1, 1.5, 1), 1.4: (1, 1, 1)})
    return [idle, gang, hieb, wurf, boss_auftritt("schlaechter", tiefe=0, laenge=2.0, extra=lambda a: a.tasten(
        "kopf", "rotation", {0: (30, 0, 0), 1.5: (30, 0, 0), 2.0: (0, 0, 0)}))]


def kommandant():
    idle = Anim("idle", 3.0)
    atmen(idle, "oberkoerper", 1)
    idle.welle("umhang", "x", 5)
    idle.welle("kopf", "y", 14)
    gang = Anim("walk", 1.4)  # schwere Servo-Schritte
    gehen(gang, ["bein_rechts", "bein_links"], ["arm_links"], 22, 10, wippen=("koerper", 1.4))
    gang.welle("umhang", "x", 10, phase=0.5)
    lauf = Anim("run", 0.9)
    gehen(lauf, ["bein_rechts", "bein_links"], ["arm_links"], 34, 20, wippen=("koerper", 2.0))
    lauf.welle("umhang", "x", 15, mitte=20)
    feuer = Anim("attack", 0.5, schleife=False)  # Feuerstoß
    feuer.tasten("arm_rechts", "rotation", {0: (0, 0, 0), 0.05: (-6, 0, 0), 0.15: (0, 0, 0), 0.25: (-6, 0, 0), 0.5: (0, 0, 0)})
    rakete = Anim("special", 1.2, schleife=False)
    rakete.tasten("raketenwerfer", "rotation", {0: (0, 0, 0), 0.3: (-20, 0, 0), 0.5: (-10, 0, 0), 1.2: (0, 0, 0)})
    rakete.tasten("oberkoerper", "rotation", {0: (0, 0, 0), 0.5: (6, 0, 0), 1.2: (0, 0, 0)})
    return [idle, gang, lauf, feuer, rakete, boss_auftritt("kommandant", tiefe=0, laenge=2.0, extra=lambda a: a.tasten(
        "koerper", "position", {0: (0, 40, 0), 1.2: (0, 0, 0), 1.4: (0, -2, 0), 2.0: (0, 0, 0)}))]


def hirte():
    idle = Anim("idle", 4.0)
    atmen(idle, "oberkoerper", 1.5)
    idle.welle("glocke", "z", 12, frequenz=2)
    idle.welle("kopf", "z", 8)
    gang = Anim("walk", 2.4)  # schreitet mit dem Stab
    gehen(gang, ["bein_rechts", "bein_links"], ["arm_links"], 18, 8, wippen=("koerper", 0.8))
    gang.welle("arm_rechts", "x", 18, phase=0.5)
    gang.welle("glocke", "z", 20, frequenz=2)
    for i in range(3):
        gang.welle(f"gloeckchen_{i}", "z", 15, phase=i * 0.3, frequenz=2)
    schlag = Anim("attack", 1.0, schleife=False)
    schlag.tasten("arm_rechts", "rotation", {0: (0, 0, 0), 0.4: (-60, 0, 30), 0.6: (40, 0, -20), 1.0: (0, 0, 0)})
    laeuten = Anim("special", 2.0, schleife=False)  # läutet die Glocke: die Herde gehorcht
    laeuten.tasten("arm_rechts", "rotation", {0: (0, 0, 0), 0.4: (-80, 0, 0), 1.6: (-80, 0, 0), 2.0: (0, 0, 0)})
    laeuten.welle("glocke", "z", 40, frequenz=6)
    return [idle, gang, schlag, laeuten, boss_auftritt("hirte", tiefe=0, laenge=2.0)]


def schlund():
    idle = Anim("idle", 3.0)
    for i in range(8):
        idle.welle(f"segment_{i}", "x", 3, phase=i * 0.12)
        idle.welle(f"segment_{i}", "z", 3, phase=i * 0.12 + 0.25)
    for i in range(3):
        idle.welle(f"zunge_{i}", "x", 15, phase=i * 0.3)
    gang = Anim("walk", 2.0)
    for i in range(8):
        gang.welle(f"segment_{i}", "x", 6, phase=i * 0.12)
        gang.welle(f"segment_{i}", "z", 6, phase=i * 0.12 + 0.25)
    biss = Anim("attack", 1.2, schleife=False)  # schlägt mit dem Maul nach unten
    biss.tasten("segment_5", "rotation", {0: (0, 0, 0), 0.4: (15, 0, 0), 0.7: (-40, 0, 0), 1.2: (0, 0, 0)})
    biss.tasten("segment_6", "rotation", {0: (0, 0, 0), 0.4: (15, 0, 0), 0.7: (-35, 0, 0), 1.2: (0, 0, 0)})
    biss.tasten("kopf", "rotation", {0: (0, 0, 0), 0.4: (20, 0, 0), 0.7: (-30, 0, 0), 1.2: (0, 0, 0)})
    abtauchen = Anim("special", 2.0, schleife=None)
    abtauchen.tasten("segment_0", "position", {0: (0, 0, 0), 2.0: (0, -70, 0)})
    auftritt = Anim("spawn", 2.5, schleife=None)  # bricht aus dem Boden
    auftritt.tasten("segment_0", "position", {0: (0, -70, 0), 1.6: (0, 4, 0), 2.5: (0, 0, 0)})
    auftritt.tasten("kopf", "rotation", {0: (40, 0, 0), 1.6: (-20, 0, 0), 2.5: (0, 0, 0)})
    return [idle, gang, biss, abtauchen, auftritt]


def pilzvater():
    idle = Anim("idle", 4.0)
    atmen(idle, "oberkoerper", 2)
    for i in range(4):
        idle.welle(f"pilz_{i}", "z", 4, phase=i * 0.25)
    idle.welle("kopf", "z", 6)
    gang = Anim("walk", 2.6)
    gehen(gang, ["bein_rechts", "bein_links"], ["arm_links", "arm_rechts"], 16, 12, wippen=("koerper", 1.2))
    gang.welle("oberkoerper", "z", 5)
    schlag = Anim("attack", 1.4, schleife=False)  # Schwammkeule
    schlag.tasten("arm_rechts", "rotation", {0: (0, 0, 0), 0.6: (-140, 0, 0), 0.9: (30, 0, 0), 1.4: (0, 0, 0)})
    sporen = Anim("special", 2.4, schleife=False)  # Hüte blähen sich auf und stoßen Sporen aus
    for i in range(4):
        sporen.tasten(f"pilz_{i}", "scale", {0: (1, 1, 1), 0.8 + i * 0.1: (1.25, 0.8, 1.25), 1.2 + i * 0.1: (0.9, 1.1, 0.9),
                                              2.4: (1, 1, 1)})
    sporen.tasten("oberkoerper", "rotation", {0: (0, 0, 0), 0.8: (15, 0, 0), 2.4: (0, 0, 0)})
    return [idle, gang, schlag, sporen, boss_auftritt("pilzvater", tiefe=-35, laenge=4.0)]


def erfrorene():
    idle = Anim("idle", 3.6)
    atmen(idle, "oberkoerper", 1.5)
    idle.welle("kopf", "y", 8)
    idle.welle("eisklinge", "x", 3)
    gang = Anim("walk", 2.0)
    gehen(gang, ["bein_rechts", "bein_links"], ["arm_links"], 20, 10, wippen=("koerper", 1.0))
    gang.welle("arm_rechts", "x", 8, phase=0.5)
    lauf = Anim("run", 1.2)
    gehen(lauf, ["bein_rechts", "bein_links"], ["arm_links"], 32, 18, wippen=("koerper", 1.6))
    hieb = Anim("attack", 1.0, schleife=False)  # waagerechter Hieb mit der Eisklinge
    hieb.tasten("arm_rechts", "rotation", {0: (0, 0, 0), 0.35: (-80, 60, 0), 0.6: (-80, -60, 0), 1.0: (0, 0, 0)})
    hieb.tasten("oberkoerper", "rotation", {0: (0, 0, 0), 0.35: (0, 30, 0), 0.6: (0, -30, 0), 1.0: (0, 0, 0)})
    atem = Anim("special", 2.0, schleife=False)  # Frostatem
    atem.tasten("kopf", "rotation", {0: (0, 0, 0), 0.4: (-20, 0, 0), 1.6: (-20, 0, 0), 2.0: (0, 0, 0)})
    atem.tasten("oberkoerper", "rotation", {0: (0, 0, 0), 0.4: (10, 0, 0), 2.0: (0, 0, 0)})
    return [idle, gang, lauf, hieb, atem, boss_auftritt("erfrorene", tiefe=0, laenge=3.0, extra=lambda a: a.tasten(
        "koerper", "scale", {0: (1, 1, 1), 3.0: (1, 1, 1)}))]


ALLE.update({n: globals()[n] for n in (
    "gefallener_soldat", "glutleib", "seuchenhund", "wuehler", "sauger", "seuchentraeger", "ertrunkener",
    "spaeherdrohne", "lichtmotte", "scheintoter", "verschmolzener", "nebelmutter", "strahlende", "brutkoenigin",
    "schlaechter", "kommandant", "hirte", "schlund", "pilzvater", "erfrorene")})


def abwurfkiste():
    """Versorgungskiste: pendelt am Fallschirm, der Schirm atmet im Wind."""
    a = Anim("schweben", 4.0, schritte=16)
    a.welle("kiste", "z", 6)
    a.welle("kiste", "x", 4, phase=0.25)
    a.welle("fallschirm", "z", -3, phase=0.1)
    a.welle("fallschirm", "y", 0.04, art="scale", frequenz=2)
    return [a]


ALLE["abwurfkiste"] = abwurfkiste


def schreiben(name):
    anims = ALLE[name]()
    daten = {"format_version": "1.8.0",
             "animations": {f"animation.{name}.{a.name}": a.json() for a in anims}}
    os.makedirs(ZIEL, exist_ok=True)
    with open(os.path.join(ZIEL, f"{name}.animation.json"), "w") as f:
        json.dump(daten, f, indent=1)
    return anims


if __name__ == "__main__":
    for n in ALLE:
        schreiben(n)
    print("Animationen geschrieben:", ", ".join(ALLE))


def abtasten(anim, t):
    """Werte aller Kanäle zur Zeit t (lineare Interpolation) – für die Vorschau."""
    erg = {}
    for (kn, art), k in anim.kanaele.items():
        zeiten = sorted(k)
        if t <= zeiten[0]:
            w = k[zeiten[0]]
        elif t >= zeiten[-1]:
            w = k[zeiten[-1]]
        else:
            for a, b in zip(zeiten, zeiten[1:]):
                if a <= t <= b:
                    f = (t - a) / (b - a) if b > a else 0
                    w = [x + (y - x) * f for x, y in zip(k[a], k[b])]
                    break
        erg.setdefault(kn, {})[art] = w
    return erg
