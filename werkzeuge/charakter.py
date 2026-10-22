"""Charakter-Editor: erzeugt die Skin-Ebenen (64x64, Minecraft-Skin-Layout) für den Editor.

  python3 werkzeuge/charakter.py            – alle Ebenen nach assets/deadzonemc/textures/charakter/
  python3 werkzeuge/charakter.py vorschau   – zusätzlich ein Vorschaubild mit Beispielfiguren

Jede Ebene besteht aus einem einfärbbaren Teil (<name>.png, Graustufen – das Spiel multipliziert mit der gewählten
Farbe) und einem festen Teil (<name>_d.png, bunt, wird unverändert darübergelegt). Für schmale Arme gibt es
jeweils eine Variante mit Endung _s. Die Reihenfolge der Kategorien und Farbpaletten muss zu
de.deadzonemc.charakter.CharakterTeile passen.
"""
import os
import random
import sys

from PIL import Image

WURZEL = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ZIEL = os.path.join(WURZEL, "src", "main", "resources", "assets", "deadzonemc", "textures", "charakter")

# ---- Paletten (gleiche Reihenfolge wie in Java) ------------------------------------------------
HAUT = [(255, 226, 200), (240, 196, 160), (222, 170, 126), (196, 138, 94), (160, 106, 70), (122, 80, 52),
        (92, 60, 40), (66, 44, 30)]
AUGEN = [(112, 72, 40), (62, 40, 24), (72, 122, 192), (84, 134, 72), (132, 142, 148), (146, 112, 52)]
HAAR = [(38, 32, 30), (72, 46, 30), (118, 78, 46), (220, 186, 116), (192, 98, 46), (156, 156, 154),
        (232, 230, 222), (64, 94, 176), (196, 108, 150)]
STOFF = [(46, 46, 50), (84, 86, 90), (146, 146, 142), (216, 210, 194), (98, 108, 62), (172, 152, 106),
         (114, 78, 46), (152, 42, 36), (42, 56, 98), (78, 104, 148), (222, 112, 32), (54, 104, 64)]


def kasten(u, v, w, h, d):
    return {"top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d), "right": (u, v + d, d, h),
            "front": (u + d, v + d, w, h), "left": (u + d + w, v + d, d, h), "back": (u + 2 * d + w, v + d, w, h)}


def teile(schmal):
    aw = 3 if schmal else 4
    return {
        "kopf": (0, 0, 8, 8, 8), "kopf_o": (32, 0, 8, 8, 8),
        "rumpf": (16, 16, 8, 12, 4), "rumpf_o": (16, 32, 8, 12, 4),
        "rarm": (40, 16, aw, 12, 4), "rarm_o": (40, 32, aw, 12, 4),
        "larm": (32, 48, aw, 12, 4), "larm_o": (48, 48, aw, 12, 4),
        "rbein": (0, 16, 4, 12, 4), "rbein_o": (0, 32, 4, 12, 4),
        "lbein": (16, 48, 4, 12, 4), "lbein_o": (0, 48, 4, 12, 4),
    }


class F:
    """Ein Pixel auf einer Fläche eines Körperteils."""
    __slots__ = ("teil", "flaeche", "x", "y", "w", "h", "d", "um", "aussen", "innen", "arm", "bein")

    def __init__(self, teil, flaeche, x, y, w, h, d):
        self.teil, self.flaeche, self.x, self.y, self.w, self.h, self.d = teil, flaeche, x, y, w, h, d
        basis = teil.replace("_o", "")
        self.arm = basis in ("rarm", "larm")
        self.bein = basis in ("rbein", "lbein")
        # Umlauf-Koordinate (rechts → vorn → links → hinten), praktisch für Streifen ringsum
        off = {"right": 0, "front": d, "left": d + w, "back": 2 * d + w}.get(flaeche)
        self.um = None if off is None else off + x
        rechts = basis.startswith("r")
        self.aussen = (flaeche == "right") if rechts else (flaeche == "left")
        self.innen = (flaeche == "left") if rechts else (flaeche == "right")

    @property
    def seite(self):
        return self.flaeche in ("right", "left", "front", "back")


class Ebene:
    def __init__(self, name, schmal):
        self.name = name
        self.schmal = schmal
        self.f = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
        self.d = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
        self.zufall = random.Random(name)

    def mal(self, teilnamen, fn, detail=False):
        """fn(F) → None | Grauwert | (Grauwert, Alpha) | (r, g, b) | (r, g, b, a)."""
        ziel = self.d if detail else self.f
        for tn in teilnamen.split():
            u, v, w, h, d = teile(self.schmal)[tn]
            for fl, (fx, fy, fw, fh) in kasten(u, v, w, h, d).items():
                for y in range(fh):
                    for x in range(fw):
                        r = fn(F(tn, fl, x, y, w, h, d))
                        if r is None:
                            continue
                        if isinstance(r, (int, float)):
                            r = (int(r), 255)
                        if len(r) == 2:
                            g, a = r
                            g = max(0, min(255, int(g)))
                            r = (g, g, g, int(a))
                        elif len(r) == 3:
                            r = (int(r[0]), int(r[1]), int(r[2]), 255)
                        ziel.putpixel((fx + x, fy + y), tuple(max(0, min(255, int(c))) for c in r))

    def rausch(self, staerke=10):
        return self.zufall.randint(-staerke, staerke)

    def speichern(self, ordner):
        os.makedirs(ordner, exist_ok=True)
        s = "_s" if self.schmal else ""
        leer = lambda im: im.getbbox() is None
        if not leer(self.f):
            self.f.save(os.path.join(ordner, f"{self.name}{s}.png"))
        if not leer(self.d):
            self.d.save(os.path.join(ordner, f"{self.name}{s}_d.png"))


def schatten(F, g):
    """Leichte Schattierung: Unterseite und hintere Flächen etwas dunkler, unten an Gliedern dunkler."""
    if F.flaeche == "bottom":
        return g - 30
    if F.flaeche == "back":
        g -= 8
    if F.flaeche in ("right", "left") and not F.arm and not F.bein:
        g -= 6
    return g


# ---- Haut --------------------------------------------------------------------------------------

def haut(E):
    def fn(F):
        g = 232 + E.rausch(4)
        if F.teil == "kopf" and F.flaeche == "front":
            if F.y == 5 and F.x in (3, 4):
                g -= 18  # Nase
            if F.y == 7:
                g -= 10
        if (F.arm or F.bein) and F.y >= 10 and F.seite:
            g -= 6
        return schatten(F, g)
    E.mal("kopf rumpf rarm larm rbein lbein", fn)
    # neutrale Unterwäsche (fest), damit nackte Stellen nie peinlich wirken
    E.mal("rumpf", lambda F: (70, 70, 74) if (F.seite and F.y >= 9) else None, detail=True)
    E.mal("rbein lbein", lambda F: (70, 70, 74) if (F.seite and F.y <= 2) or F.flaeche == "top" else None, detail=True)


# ---- Gesicht -----------------------------------------------------------------------------------

AUGENWEISS = (238, 236, 228)
MUND = (70, 30, 28, 150)


def gesicht(E, art):
    """Feste Gesichtsdetails (Augenweiß, Mund, Narben) + Brauen (einfärbbar mit Haarfarbe)."""
    vorn = lambda F: F.teil == "kopf" and F.flaeche == "front"

    brauen = {(1, 3), (2, 3), (5, 3), (6, 3)}
    mund = {(3, 6), (4, 6)}
    extra = {}
    if art == 1:  # müde
        extra.update({(1, 5): (40, 20, 40, 70), (2, 5): (40, 20, 40, 70), (5, 5): (40, 20, 40, 70), (6, 5): (40, 20, 40, 70)})
    elif art == 2:  # Narbe übers rechte Auge
        brauen.discard((2, 3))
        for p in ((2, 2), (2, 3), (1, 5), (1, 6)):
            extra[p] = (196, 116, 112, 210)
    elif art == 3:  # grimmig
        brauen = {(1, 2), (2, 3), (5, 3), (6, 2)}
        mund = {(2, 6), (3, 6), (4, 6), (5, 6)}
    elif art == 4:  # verängstigt
        brauen = {(1, 3), (2, 2), (5, 2), (6, 3)}
        mund = set()
        extra.update({(3, 6): (52, 16, 16, 235), (4, 6): (52, 16, 16, 235), (3, 7): (52, 16, 16, 120), (4, 7): (52, 16, 16, 120)})
    elif art == 5:  # Pflaster
        extra.update({(5, 5): (226, 200, 158, 255), (6, 5): (210, 182, 140, 255)})
    elif art == 6:  # verschmiert
        for p in ((0, 5), (1, 6), (6, 6), (7, 5), (2, 7), (5, 0)):
            extra[p] = (40, 34, 30, 90)

    def detail(F):
        if not vorn(F):
            return None
        p = (F.x, F.y)
        if p in extra:
            return extra[p]
        if p in ((1, 4), (6, 4)):
            return AUGENWEISS
        if p in mund:
            return MUND
        return None

    def braue(F):
        return (150, 255) if vorn(F) and (F.x, F.y) in brauen else None

    E.mal("kopf", detail, detail=True)
    E.mal("kopf", braue)


def augen(E):
    E.mal("kopf", lambda F: 220 if F.teil == "kopf" and F.flaeche == "front" and (F.x, F.y) in ((2, 4), (5, 4)) else None)


# ---- Haare -------------------------------------------------------------------------------------

HAARE = ["glatze", "kurz", "undercut", "lang", "pferdeschwanz", "locken", "irokese", "dutt", "zoepfe", "stoppeln"]


def haare(E, stil):
    if stil == "glatze":
        return
    R = random.Random(stil)
    g0 = lambda: 200 + R.randint(-18, 18)

    def basis(F):
        if F.teil != "kopf":
            return None
        fl, x, y = F.flaeche, F.x, F.y
        if fl == "bottom":
            return None
        if stil == "stoppeln":
            if fl == "top" or (fl in ("back",) and y <= 4) or (fl in ("right", "left") and y <= 2) or (fl == "front" and y == 0):
                return (g0(), 120)
            return None
        if fl == "top":
            if stil == "irokese":
                return (g0(), 110)
            return g0()
        if stil == "irokese":
            if fl == "back" and x in (3, 4) and y <= 5:
                return g0()
            if fl in ("right", "left", "back") and y <= 2:
                return (g0(), 100)
            return None
        tiefe_hinten = {"kurz": 3, "undercut": 1, "lang": 7, "pferdeschwanz": 3, "locken": 5, "dutt": 3, "zoepfe": 6}[stil]
        tiefe_seite = {"kurz": 2, "undercut": 0, "lang": 7, "pferdeschwanz": 2, "locken": 3, "dutt": 2, "zoepfe": 5}[stil]
        if fl == "back":
            if stil == "pferdeschwanz" and x in (3, 4) and y <= 7:
                return g0() - 10
            if stil == "locken" and y == tiefe_hinten and R.random() < 0.5:
                return None
            return g0() if y <= tiefe_hinten else None
        if fl in ("right", "left"):
            # x läuft auf der rechten Seite von hinten nach vorn, auf der linken von vorn nach hinten
            zum_gesicht = x if fl == "right" else 7 - x
            if stil in ("lang", "zoepfe") and zum_gesicht <= 6:
                return g0() if y <= tiefe_seite else None
            if y <= tiefe_seite:
                return g0()
            if y <= tiefe_seite + 2 and zum_gesicht >= 6 and stil in ("kurz", "pferdeschwanz", "dutt"):
                return g0() - 15  # Koteletten
            if stil == "locken" and y == tiefe_seite + 1 and R.random() < 0.5:
                return g0()
            return None
        if fl == "front":
            if stil == "undercut":
                return g0() if (y == 0 or (y == 1 and x <= 5) or (y == 2 and x <= 2)) else None
            if stil in ("lang", "zoepfe"):
                return g0() if (y <= 1 or ((x == 0 or x == 7) and y <= 6)) else None
            if stil == "locken":
                return g0() if (y == 0 or (y == 1 and x % 3 != 1)) else None
            return g0() if (y == 0 or (y == 1 and x in (0, 1, 6, 7))) else None
        return None

    def volumen(F):
        if F.teil != "kopf_o":
            return None
        fl, x, y = F.flaeche, F.x, F.y
        if stil == "irokese":
            if fl == "top" and x in (3, 4):
                return g0()
            if fl == "back" and x in (3, 4) and y <= 3:
                return g0()
            if fl == "front" and x in (3, 4) and y == 0:
                return g0()
            return None
        if stil == "locken":
            if fl == "top" and R.random() < 0.75:
                return g0()
            if fl in ("right", "left", "back") and y <= 2 and R.random() < 0.6:
                return g0()
            return None
        if stil == "dutt":
            if fl == "top" and 2 <= x <= 5 and y >= 6:
                return g0() - 8
            if fl == "back" and 2 <= x <= 5 and y <= 2:
                return g0() - 8
            return None
        if stil == "pferdeschwanz" and fl == "back" and x in (3, 4) and 2 <= y <= 7:
            return g0() - 14
        if stil == "lang" and fl in ("back", "right", "left") and y >= 3 and R.random() < 0.55:
            return g0() - 6
        if stil == "zoepfe" and fl in ("right", "left"):
            zum_gesicht = x if fl == "right" else 7 - x
            if zum_gesicht in (1, 2) and y >= 4:
                return g0() - (20 if y % 2 else 0)
        return None

    E.mal("kopf", basis)
    E.mal("kopf_o", volumen)


BAERTE = ["kein", "stoppeln", "vollbart", "schnurrbart", "kinnbart"]


def bart(E, art):
    if art == "kein":
        return
    R = random.Random(art)

    def fn(F):
        if F.teil != "kopf":
            return None
        fl, x, y = F.flaeche, F.x, F.y
        zum_gesicht = x if fl == "right" else 7 - x if fl == "left" else 0
        if art == "stoppeln":
            if fl == "front" and y >= 6 and 1 <= x <= 6:
                return (150, 110)
            if fl in ("right", "left") and y >= 5 and zum_gesicht >= 5:
                return (150, 90)
            if fl == "bottom" and y <= 3:
                return (150, 90)
        if art == "vollbart":
            if fl == "front" and (y == 7 or (y == 6 and x not in (3, 4)) or (y == 5 and x in (0, 1, 6, 7))):
                return 160 + R.randint(-15, 15)
            if fl in ("right", "left") and y >= 4 and zum_gesicht >= 4:
                return 160 + R.randint(-15, 15)
            if fl == "bottom" and y <= 5:
                return 150
        if art == "schnurrbart" and fl == "front" and y == 5 and 2 <= x <= 5:
            return 160
        if art == "kinnbart":
            if fl == "front" and ((y == 7 and 2 <= x <= 5) or (y == 6 and x in (2, 5))):
                return 160
            if fl == "bottom" and y <= 2 and 2 <= x <= 5:
                return 150
        return None
    E.mal("kopf", fn)


# ---- Kleidung ----------------------------------------------------------------------------------

OBERTEILE = ["tshirt", "kapuzenpulli", "lederjacke", "militaerjacke", "flanellhemd", "sanitaeter", "weste",
             "regenjacke", "tanktop", "strickpulli"]


def oberteil(E, art):
    R = random.Random(art)
    camo = {}

    def tarn(F):
        k = (F.teil, F.flaeche, F.x // 2, F.y // 2)
        if k not in camo:
            camo[k] = R.choice((150, 175, 205, 215))
        return camo[k]

    def stoff(F, g=212):
        g += E.rausch(5)
        if art == "militaerjacke":
            g = tarn(F)
        elif art == "flanellhemd":
            g = 220 if (F.x + (F.um or 0)) % 4 < 2 else 168
            if F.y % 4 == 2:
                g -= 40
        elif art == "strickpulli":
            g = 214 if (F.um if F.um is not None else F.x) % 2 == 0 else 188
        elif art == "lederjacke":
            g = 200 + (22 if (F.x + F.y) % 7 == 0 else 0) + E.rausch(4)
        elif art == "regenjacke":
            g = 214 + (28 if F.y in (2, 3) and F.flaeche == "front" and F.x in (1, 2) else 0)
        return schatten(F, g)

    def fn(F):
        t = F.teil
        if t == "rumpf":
            if F.flaeche == "bottom":
                return None
            if art == "tanktop" and F.flaeche == "top" and F.x in (0, 1, 6, 7):
                return None
            if art == "tanktop" and F.flaeche == "front" and F.y == 0 and F.x in (0, 1, 3, 4, 6, 7):
                return None
            if art in ("tshirt", "tanktop") and F.flaeche == "front" and F.y == 0 and F.x in (3, 4):
                return None
            lang = 11 if art in ("lederjacke", "regenjacke", "militaerjacke", "sanitaeter") else 9
            if F.seite and F.y > lang:
                return None
            if art == "lederjacke" and F.flaeche == "front" and F.x in (3, 4):
                return None  # offen – das Hemd darunter ist fest
            if art == "weste" and F.flaeche == "front" and F.x in (3, 4):
                return None
            if art == "kapuzenpulli" and F.flaeche == "front" and 7 <= F.y <= 9 and 1 <= F.x <= 6:
                return stoff(F) - 28
            return stoff(F)
        if F.arm:
            if art in ("tanktop", "weste"):
                return None
            if F.flaeche == "bottom":
                return None
            aermel = {"tshirt": 3, "flanellhemd": 7}.get(art, 11)
            if F.flaeche == "top":
                return stoff(F)
            if F.y > aermel:
                return None
            g = stoff(F)
            if art == "flanellhemd" and F.y == 7:
                g = 225  # hochgekrempelt
            if art in ("kapuzenpulli", "strickpulli") and F.y >= 10:
                g -= 22  # Bündchen
            return g
        return None

    def ueber(F):
        t = F.teil
        if t == "rumpf_o":
            if art in ("kapuzenpulli", "regenjacke") and F.flaeche == "back" and F.y <= 3:
                return stoff(F) - 15  # Kapuze hängt
            if art in ("kapuzenpulli", "regenjacke") and F.flaeche == "top" and F.y >= 2:
                return stoff(F) - 15
            if art == "lederjacke" and F.flaeche == "front" and F.y <= 1 and F.x in (2, 5):
                return stoff(F) - 10  # Kragen
            if art == "weste":
                if F.flaeche == "bottom" or (F.seite and F.y > 9):
                    return None
                if F.flaeche == "front" and F.x in (3, 4):
                    return None
                return stoff(F)
        return None

    def detail(F):
        t = F.teil
        if t == "rumpf" and F.flaeche == "front":
            if art == "lederjacke" and F.x in (3, 4) and F.y <= 11:
                return (58, 58, 62) if F.y > 0 else None
            if art == "weste" and F.x in (3, 4) and F.y <= 9:
                return (198, 194, 180)
            if art in ("regenjacke", "sanitaeter", "militaerjacke") and F.x == 4 and 1 <= F.y <= 11:
                return (176, 176, 170)
            if art == "kapuzenpulli" and F.x in (3, 4) and 1 <= F.y <= 3:
                return (226, 224, 216)
            if art == "flanellhemd" and F.x == 4 and F.y in (2, 5, 8):
                return (228, 222, 200)
            if art == "militaerjacke" and F.y in (2, 3) and F.x in (1, 2, 5, 6):
                return (40, 44, 30, 110)
            if art == "sanitaeter" and F.y == 8:
                return (232, 230, 196)
        if art == "sanitaeter":
            if t == "rumpf" and F.flaeche == "back" and ((F.x in (3, 4) and 2 <= F.y <= 6) or (F.y == 4 and 2 <= F.x <= 5)):
                return (210, 32, 30)
            if t == "rumpf" and F.flaeche == "back" and F.y == 8:
                return (232, 230, 196)
            if F.arm and F.seite and F.y == 7:
                return (232, 230, 196)
            if t == "rarm" and F.aussen and ((F.x == 1 and 1 <= F.y <= 3) or (F.y == 2 and F.x in (0, 1, 2))):
                return (210, 32, 30)
        if art == "militaerjacke" and t == "rarm" and F.aussen and 1 <= F.y <= 2 and F.x <= 2:
            return (60, 66, 44)
        if art == "regenjacke" and F.arm and F.seite and F.y == 8:
            return (220, 220, 190)
        return None

    E.mal("rumpf rarm larm", fn)
    E.mal("rumpf_o", ueber)
    E.mal("rumpf rarm larm", detail, detail=True)


HOSEN = ["jeans", "cargo", "jogger", "militaer", "shorts", "arbeitshose"]


def hose(E, art):
    R = random.Random(art)
    camo = {}

    def fn(F):
        if F.teil == "rumpf":
            if F.seite and F.y >= 10:
                return schatten(F, 200 + E.rausch(4))
            if F.flaeche == "bottom":
                return 190
            return None
        if not F.bein:
            return None
        if F.flaeche == "bottom":
            return None
        if art == "shorts" and F.seite and F.y > 5:
            return None
        g = 210 + E.rausch(6)
        if art == "jeans":
            g = 204 + E.rausch(9)
            if F.aussen and F.x == 2:
                g += 18
        if art == "militaer":
            k = (F.teil, F.flaeche, F.x // 2, F.y // 2)
            if k not in camo:
                camo[k] = R.choice((150, 175, 205, 215))
            g = camo[k]
        if art == "cargo" and F.aussen and 4 <= F.y <= 7:
            g -= 26 if F.y in (4, 7) or F.x in (0, 3) else 14
        if art == "jogger" and F.seite and F.y >= 10:
            g -= 20
        return schatten(F, g)

    def detail(F):
        if F.teil == "rumpf" and F.seite and F.y == 10:
            if F.flaeche == "front" and F.x in (3, 4):
                return (184, 168, 108)
            return (52, 38, 28)
        if F.bein and art == "jogger" and F.aussen and F.x in (1, 2) and F.y <= 9:
            return (226, 226, 222)
        if F.bein and art == "arbeitshose" and F.flaeche == "front" and F.y in (5, 6):
            return (48, 48, 50)
        if F.bein and art == "cargo" and F.aussen and F.y == 4 and F.x in (1, 2):
            return (40, 40, 36, 160)
        return None

    E.mal("rumpf rbein lbein", fn)
    E.mal("rumpf rbein lbein", detail, detail=True)


SCHUHE = ["stiefel", "turnschuhe", "arbeitsschuhe", "gummistiefel"]


def schuhe(E, art):
    hoehe = {"stiefel": 8, "turnschuhe": 10, "arbeitsschuhe": 9, "gummistiefel": 6}[art]

    def fn(F):
        if not F.bein or F.teil.endswith("_o"):
            return None
        if F.flaeche == "bottom":
            return 150
        if F.seite and F.y >= hoehe:
            g = 205 + E.rausch(5)
            if art == "gummistiefel" and F.flaeche == "front" and F.x == 1:
                g += 30
            if F.y == hoehe and art in ("stiefel", "gummistiefel"):
                g -= 20
            return schatten(F, g)
        return None

    def detail(F):
        if not F.bein or F.teil.endswith("_o"):
            return None
        if F.flaeche == "bottom":
            return (34, 30, 28) if art != "turnschuhe" else (226, 224, 218)
        if F.seite and F.y == 11:
            return (34, 30, 28) if art != "turnschuhe" else (226, 224, 218)
        if F.flaeche == "front" and art in ("stiefel", "turnschuhe") and F.y == hoehe + 1 and F.x in (1, 2):
            return (255, 250, 230, 70) if art == "stiefel" else (236, 236, 236)
        if art == "arbeitsschuhe" and F.flaeche == "front" and F.y == 10:
            return (130, 130, 128)
        return None

    E.mal("rbein lbein", fn)
    E.mal("rbein lbein", detail, detail=True)


# ---- Ausrüstung --------------------------------------------------------------------------------

KOEPFE = ["keine", "gasmaske", "atemschutz", "bandana", "muetze", "helm", "schutzbrille", "kopfverband", "kappe"]


def kopfteil(E, art):
    if art == "keine":
        return

    def zum_gesicht(F):
        return F.x if F.flaeche == "right" else 7 - F.x if F.flaeche == "left" else 0

    def fn(F):  # einfärbbar
        if F.teil != "kopf_o":
            return None
        fl, x, y = F.flaeche, F.x, F.y
        if art == "bandana":
            if fl == "front" and (y in (5, 6) or (y == 7 and 2 <= x <= 5)):
                return 200 + (25 if (x + y) % 3 == 0 else 0)
            if fl in ("right", "left") and y in (5, 6) and zum_gesicht(F) >= 3:
                return 190
            if fl in ("right", "left") and y == 5:
                return 185
            if fl == "back" and y == 5:
                return 185
            if fl == "back" and y in (6, 7) and x in (3, 4):
                return 175
        if art == "muetze":
            if fl == "top":
                return 205 + (12 if x % 2 else 0)
            if fl in ("front", "right", "left", "back") and y <= 2:
                return 205 - (24 if y == 2 else 0) + (12 if x % 2 else 0)
        if art == "helm":
            if fl == "top":
                return 210 + E.rausch(6)
            if fl in ("front",) and y <= 1:
                return 200 - (20 if y == 1 else 0)
            if fl in ("right", "left") and y <= 2:
                return 200
            if fl == "back" and y <= 3:
                return 200 - (20 if y == 3 else 0)
        if art == "kappe":
            if fl == "top":
                return 210
            if fl in ("right", "left", "back") and y <= 1:
                return 200
            if fl == "front" and y <= 1:
                return 170 if y == 1 else 205
        return None

    def detail(F):
        if F.teil != "kopf_o":
            return None
        fl, x, y = F.flaeche, F.x, F.y
        if art == "gasmaske":
            if fl == "front":
                if y == 4 and x in (1, 2, 5, 6):
                    return (46, 58, 54) if x in (1, 6) else (120, 150, 140)
                if y in (3, 5) and 1 <= x <= 6:
                    return (44, 44, 42)
                if y == 4 and x in (0, 3, 4, 7):
                    return (44, 44, 42)
                if y in (6, 7) and 2 <= x <= 5:
                    return (92, 96, 88) if (x in (3, 4)) else (60, 62, 58)
            if fl in ("right", "left") and y in (3, 4):
                return (30, 30, 30)
            if fl == "back" and y in (3, 4):
                return (30, 30, 30)
        if art == "atemschutz":
            if fl == "front" and 5 <= y <= 7 and 1 <= x <= 6 and not (y == 7 and x in (1, 6)):
                return (232, 232, 226) if (x + y) % 4 else (210, 210, 204)
            if fl in ("right", "left") and y == 5 and zum_gesicht(F) >= 2:
                return (232, 232, 226)
        if art == "schutzbrille":
            if fl == "front" and y == 4 and x in (1, 2, 5, 6):
                return (210, 150, 50) if x in (2, 5) else (180, 120, 40)
            if fl == "front" and y == 4 and x in (0, 3, 4, 7):
                return (40, 40, 40)
            if fl in ("right", "left", "back") and y == 4:
                return (38, 38, 38)
        if art == "kopfverband":
            if fl in ("front", "right", "left", "back") and y in (1, 2):
                if fl == "front" and x in (5, 6) and y == 2:
                    return (160, 40, 36)
                return (232, 228, 214) if (x + y) % 3 else (214, 208, 192)
        if art == "helm" and fl in ("right", "left") and y >= 3 and zum_gesicht(F) == 5:
            return (40, 34, 26)
        return None

    E.mal("kopf_o", fn)
    E.mal("kopf_o", detail, detail=True)


EXTRAS = ["keine", "rucksack", "schutzweste", "patronengurt", "armbinde", "verbaende", "holster"]


def extra(E, art):
    if art == "keine":
        return

    def fn(F):  # einfärbbar
        if art == "schutzweste" and F.teil == "rumpf_o":
            if F.flaeche == "bottom" or (F.seite and F.y > 8):
                return None
            if F.flaeche == "top" and F.y in (1, 2):
                return None
            g = 196 + E.rausch(5)
            if F.flaeche == "front" and F.y in (5, 6, 7) and F.x in (0, 1, 3, 4, 6, 7):
                g -= 25  # Taschen
            return schatten(F, g)
        if art == "armbinde" and F.teil == "larm_o" and F.seite and F.y in (2, 3):
            return 210
        if art == "rucksack" and F.teil == "rumpf_o" and F.flaeche == "back" and 1 <= F.y <= 9 and 1 <= F.x <= 6:
            return 196 - (24 if F.y in (5, 9) else 0) + E.rausch(4)
        return None

    def detail(F):
        t = F.teil
        if art == "rucksack":
            if t == "rumpf_o" and F.flaeche == "front" and F.x in (1, 6) and F.y <= 7:
                return (58, 46, 34)
            if t == "rumpf_o" and F.flaeche == "top" and F.x in (1, 6):
                return (58, 46, 34)
        if art == "patronengurt" and t == "rumpf_o":
            if F.flaeche == "front" and abs((7 - F.x) - F.y * 0.65) < 0.8:
                return (190, 150, 60) if F.y % 2 else (70, 52, 34)
            if F.flaeche == "back" and abs(F.x - F.y * 0.65) < 0.8:
                return (70, 52, 34)
        if art == "verbaende":
            if t == "rarm_o" and F.seite and 7 <= F.y <= 10:
                return (160, 44, 40) if (F.y == 8 and F.flaeche == "front") else (228, 224, 210)
            if t == "lbein_o" and F.seite and 3 <= F.y <= 4:
                return (228, 224, 210)
        if art == "holster":
            if t == "rbein_o" and F.aussen and F.y <= 4 and F.x in (1, 2):
                return (54, 40, 30) if F.y else (34, 30, 28)
            if t == "rumpf_o" and F.seite and F.y == 10:
                return (54, 40, 30)
        if art == "armbinde" and t == "larm_o" and F.aussen and F.y in (2, 3) and F.x == 1:
            return (236, 236, 230)
        return None

    E.mal("rumpf_o rarm_o larm_o rbein_o lbein_o", fn)
    E.mal("rumpf_o rarm_o larm_o rbein_o lbein_o", detail, detail=True)


GESICHTER = ["ruhig", "muede", "narbe", "grimmig", "veraengstigt", "pflaster", "verschmiert"]


def alle_ebenen():
    for schmal in (False, True):
        yield "haut", "haut", lambda E: haut(E), schmal
        yield "augen", "augen", lambda E: augen(E), schmal
        for i, n in enumerate(GESICHTER):
            yield "gesicht", n, (lambda i: lambda E: gesicht(E, i))(i), schmal
        for n in HAARE:
            yield "haare", n, (lambda n: lambda E: haare(E, n))(n), schmal
        for n in BAERTE:
            yield "bart", n, (lambda n: lambda E: bart(E, n))(n), schmal
        for n in OBERTEILE:
            yield "oberteil", n, (lambda n: lambda E: oberteil(E, n))(n), schmal
        for n in HOSEN:
            yield "hose", n, (lambda n: lambda E: hose(E, n))(n), schmal
        for n in SCHUHE:
            yield "schuhe", n, (lambda n: lambda E: schuhe(E, n))(n), schmal
        for n in KOEPFE:
            yield "kopf", n, (lambda n: lambda E: kopfteil(E, n))(n), schmal
        for n in EXTRAS:
            yield "extra", n, (lambda n: lambda E: extra(E, n))(n), schmal


def bauen():
    ebenen = {}
    for kat, name, fn, schmal in alle_ebenen():
        E = Ebene(name, schmal)
        fn(E)
        E.speichern(os.path.join(ZIEL, kat))
        ebenen[(kat, name, schmal)] = E
    return ebenen


# ---- Zusammensetzen (wie im Spiel) -------------------------------------------------------------

def faerben(bild, farbe):
    aus = Image.new("RGBA", bild.size)
    px, po = bild.load(), aus.load()
    for y in range(bild.size[1]):
        for x in range(bild.size[0]):
            g, _, _, a = px[x, y]
            if a:
                po[x, y] = (farbe[0] * g // 255, farbe[1] * g // 255, farbe[2] * g // 255, a)
    return aus


def zusammensetzen(ebenen, wahl, schmal=False):
    """wahl: dict wie in Java – siehe CharakterTeile."""
    skin = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    haar = HAAR[wahl["haarfarbe"]]
    reihe = [
        ("haut", "haut", HAUT[wahl["haut"]]),
        ("gesicht", GESICHTER[wahl["gesicht"]], haar),
        ("augen", "augen", AUGEN[wahl["augen"]]),
        ("bart", BAERTE[wahl["bart"]], haar),
        ("hose", HOSEN[wahl["hose"]], STOFF[wahl["hosenfarbe"]]),
        ("schuhe", SCHUHE[wahl["schuhe"]], STOFF[wahl["schuhfarbe"]]),
        ("oberteil", OBERTEILE[wahl["oberteil"]], STOFF[wahl["oberfarbe"]]),
        ("haare", HAARE[wahl["haare"]], haar),
        ("extra", EXTRAS[wahl["extra"]], STOFF[wahl["extrafarbe"]]),
        ("kopf", KOEPFE[wahl["kopf"]], STOFF[wahl["kopffarbe"]]),
    ]
    for kat, name, farbe in reihe:
        E = ebenen[(kat, name, schmal)]
        skin = Image.alpha_composite(skin, faerben(E.f, farbe))
        skin = Image.alpha_composite(skin, E.d)
    return skin


def figur_vorn(skin, schmal=False, hinten=False):
    """Flache Ansicht (vorn oder hinten) eines Skins, 16x32 Pixel."""
    T = teile(schmal)
    bild = Image.new("RGBA", (16, 32), (0, 0, 0, 0))
    fl = "back" if hinten else "front"

    def kleben(tn, ox, oy):
        u, v, w, h, d = T[tn]
        fx, fy, fw, fh = kasten(u, v, w, h, d)[fl]
        stueck = skin.crop((fx, fy, fx + fw, fy + fh))
        bild.alpha_composite(stueck, (ox, oy))

    aw = 3 if schmal else 4
    # vorn: rechter Arm/rechtes Bein erscheinen links im Bild
    links_arm, rechts_arm = ("rarm", "larm") if not hinten else ("larm", "rarm")
    links_bein, rechts_bein = ("rbein", "lbein") if not hinten else ("lbein", "rbein")
    for o in ("", "_o"):
        kleben("kopf" + o, 4, 0)
        kleben("rumpf" + o, 4, 8)
        kleben(links_arm + o, 4 - aw, 8)
        kleben(rechts_arm + o, 12, 8)
        kleben(links_bein + o, 4, 20)
        kleben(rechts_bein + o, 8, 20)
    return bild


def vorschau(ebenen, pfad):
    beispiele = [
        dict(haut=1, augen=2, gesicht=3, haare=2, haarfarbe=1, bart=2, oberteil=3, oberfarbe=4, hose=3, hosenfarbe=4,
             schuhe=0, schuhfarbe=0, kopf=0, kopffarbe=0, extra=2, extrafarbe=4),
        dict(haut=0, augen=3, gesicht=0, haare=4, haarfarbe=4, bart=0, oberteil=1, oberfarbe=7, hose=0, hosenfarbe=9,
             schuhe=1, schuhfarbe=3, kopf=0, kopffarbe=0, extra=1, extrafarbe=4, schmal=True),
        dict(haut=4, augen=0, gesicht=1, haare=1, haarfarbe=0, bart=1, oberteil=5, oberfarbe=10, hose=5, hosenfarbe=1,
             schuhe=2, schuhfarbe=6, kopf=2, kopffarbe=0, extra=5, extrafarbe=0),
        dict(haut=2, augen=5, gesicht=5, haare=5, haarfarbe=2, bart=0, oberteil=2, oberfarbe=0, hose=1, hosenfarbe=5,
             schuhe=0, schuhfarbe=6, kopf=6, kopffarbe=0, extra=3, extrafarbe=0, schmal=True),
        dict(haut=6, augen=1, gesicht=2, haare=6, haarfarbe=0, bart=4, oberteil=8, oberfarbe=3, hose=3, hosenfarbe=4,
             schuhe=0, schuhfarbe=0, kopf=0, kopffarbe=0, extra=6, extrafarbe=0),
        dict(haut=3, augen=4, gesicht=6, haare=3, haarfarbe=5, bart=0, oberteil=7, oberfarbe=11, hose=2, hosenfarbe=2,
             schuhe=3, schuhfarbe=11, kopf=1, kopffarbe=0, extra=0, extrafarbe=0, schmal=True),
        dict(haut=5, augen=0, gesicht=4, haare=7, haarfarbe=8, bart=0, oberteil=9, oberfarbe=8, hose=4, hosenfarbe=0,
             schuhe=1, schuhfarbe=0, kopf=3, kopffarbe=7, extra=4, extrafarbe=7, schmal=True),
        dict(haut=7, augen=0, gesicht=0, haare=0, haarfarbe=0, bart=2, oberteil=4, oberfarbe=7, hose=0, hosenfarbe=8,
             schuhe=2, schuhfarbe=6, kopf=5, kopffarbe=4, extra=1, extrafarbe=6),
    ]
    S = 12
    breite = len(beispiele) * 9 * S + S
    bild = Image.new("RGBA", (breite, 36 * S), (24, 26, 28, 255))
    for i, w in enumerate(beispiele):
        schmal = w.pop("schmal", False)
        skin = zusammensetzen(ebenen, w, schmal)
        for j, hinten in enumerate((False, True)):
            f = figur_vorn(skin, schmal, hinten).resize((16 * S // 2, 32 * S // 2), Image.NEAREST)
            bild.alpha_composite(f, (S + i * 18 * S // 2, S + j * 17 * S))
    bild.save(pfad)
    return breite


if __name__ == "__main__":
    E = bauen()
    n = sum(len(fs) for _, _, fs in os.walk(ZIEL))
    print(f"{n} Ebenen-Dateien geschrieben")
    if "vorschau" in sys.argv:
        ziel = sys.argv[sys.argv.index("vorschau") + 1] if len(sys.argv) > sys.argv.index("vorschau") + 1 else "charakter_vorschau.png"
        vorschau(E, ziel)
        print("Vorschau:", ziel)
