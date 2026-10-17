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
