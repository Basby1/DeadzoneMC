"""Malt Texturen für Box-UV-Modelle: Materialien (Haut, Stoff, Metall …) mit Rauschen, Schmutz und Blut,
dazu Pixel-Vorlagen für Gesichter. Leuchtende Pixel (Augen, Pusteln) landen zusätzlich in einer Glowmask.
"""
import math
import os

# Unter diesem Alphawert gilt ein Pixel als Glas/Aura und wird durchscheinend gezeichnet.
GLAS_SCHWELLE = 200
# Diese Wesen werden ohnehin komplett durchscheinend gezeichnet (MonsterRenderer mit durchscheinend=true).
GANZ_DURCHSCHEINEND = {"nebelwesen", "spaeherdrohne", "nebelmutter", "erfrorene", "hologeist", "spuk"}

import numpy as np
from PIL import Image

from modell import flaechen


def farbe(hexwert, a=255):
    hexwert = hexwert.lstrip("#")
    return np.array([int(hexwert[i:i + 2], 16) for i in (0, 2, 4)] + [a], dtype=float)


def rauschen(b, h, skala, rng, oktaven=3):
    """Wertrauschen 0..1 in der Größe b×h."""
    erg = np.zeros((h, b))
    gewicht = 0.0
    amp = 1.0
    for o in range(oktaven):
        s = max(1.0, skala / (2 ** o))
        gb = int(math.ceil(b / s)) + 2
        gh = int(math.ceil(h / s)) + 2
        gitter = rng.random((gh, gb))
        ys = np.arange(h) / s
        xs = np.arange(b) / s
        y0 = ys.astype(int)
        x0 = xs.astype(int)
        fy = (ys - y0)[:, None]
        fx = (xs - x0)[None, :]
        fy = fy * fy * (3 - 2 * fy)
        fx = fx * fx * (3 - 2 * fx)
        a = gitter[y0][:, x0]
        bb = gitter[y0][:, x0 + 1]
        c = gitter[y0 + 1][:, x0]
        d = gitter[y0 + 1][:, x0 + 1]
        erg += amp * ((a * (1 - fx) + bb * fx) * (1 - fy) + (c * (1 - fx) + d * fx) * fy)
        gewicht += amp
        amp *= 0.5
    return erg / gewicht


class Maler:
    def __init__(self, modell, saat=1):
        self.m = modell
        self.bild = np.zeros((modell.th, modell.tb, 4))
        self.glow = np.zeros((modell.th, modell.tb, 4))
        self.rng = np.random.default_rng(saat)

    # --- Grundfunktionen -------------------------------------------------
    def setze(self, x, y, f, glow=False):
        if 0 <= x < self.m.tb and 0 <= y < self.m.th:
            self.bild[y, x] = f
            if glow:
                self.glow[y, x] = f

    def region(self, rect):
        u, v, b, h = rect
        return self.bild[v:v + h, u:u + b]

    def alles_malen(self):
        for kn, w in self.m.alle_wuerfel():
            for name, rect in flaechen(w).items():
                if rect[2] <= 0 or rect[3] <= 0:
                    continue
                w.material(self, rect, name, w)
                if w.deko:
                    w.deko(self, rect, name, w)

    def speichern(self, pfad, glowpfad=None):
        bild = np.clip(self.bild, 0, 255).astype(np.uint8)
        name = os.path.basename(pfad)[:-4]
        glaspfad = pfad[:-4] + "_glas.png"
        # Halbdurchsichtige Stellen (Glas, Auren, Schilde) würden im Spiel als undurchsichtige Blöcke erscheinen,
        # weil Monster ausgeschnitten (cutout) gezeichnet werden. Deshalb kommen sie in eine eigene Textur, die der
        # Renderer in einem zweiten, durchscheinenden Durchgang darüberlegt.
        halb = (bild[..., 3] > 0) & (bild[..., 3] < GLAS_SCHWELLE)
        if name not in GANZ_DURCHSCHEINEND and halb.any():
            glas = np.zeros_like(bild)
            glas[halb] = bild[halb]
            bild = bild.copy()
            bild[halb] = 0
            Image.fromarray(glas, "RGBA").save(glaspfad)
        elif os.path.exists(glaspfad):
            os.remove(glaspfad)
        Image.fromarray(bild, "RGBA").save(pfad)
        if glowpfad and self.glow[..., 3].any():
            Image.fromarray(np.clip(self.glow, 0, 255).astype(np.uint8), "RGBA").save(glowpfad)

    # --- Malhilfen -------------------------------------------------------
    def fuellen(self, rect, basis, streuung=18, skala=3.0, dunkel_unten=0.0, rand=0.12):
        u, v, b, h = rect
        n = rauschen(b, h, skala, self.rng)
        fein = self.rng.random((h, b))
        hell = (n - 0.5) * streuung * 2 + (fein - 0.5) * streuung * 0.6
        verlauf = np.linspace(0, -dunkel_unten, h)[:, None] if h > 1 else 0
        col = basis[None, None, :3] * (1 + verlauf[..., None] if h > 1 else 1) + hell[..., None]
        # Ränder minimal abdunkeln, damit Kanten lesbar bleiben
        if rand and b > 2 and h > 2:
            maske = np.ones((h, b))
            maske[0, :] *= 1 - rand
            maske[-1, :] *= 1 - rand
            maske[:, 0] *= 1 - rand
            maske[:, -1] *= 1 - rand
            col = col * maske[..., None]
        self.bild[v:v + h, u:u + b, :3] = col
        self.bild[v:v + h, u:u + b, 3] = basis[3]

    def flecken(self, rect, f, dichte=0.25, skala=2.5, schwelle=0.62, staerke=1.0):
        u, v, b, h = rect
        n = rauschen(b, h, skala, self.rng)
        maske = (n > schwelle) & (self.rng.random((h, b)) < dichte + 0.7)
        reg = self.bild[v:v + h, u:u + b]
        reg[maske, :3] = reg[maske, :3] * (1 - staerke) + f[:3] * staerke + (self.rng.random((maske.sum(), 1)) - 0.5) * 14

    def spritzer(self, rect, f, anzahl=3, groesse=2):
        u, v, b, h = rect
        for _ in range(anzahl):
            cx = self.rng.integers(0, b)
            cy = self.rng.integers(0, h)
            for _ in range(groesse * 3):
                x = cx + int(self.rng.normal(0, groesse * 0.6))
                y = cy + int(abs(self.rng.normal(0, groesse * 0.8)))  # läuft nach unten
                if 0 <= x < b and 0 <= y < h:
                    self.bild[v + y, u + x, :3] = f[:3] * (0.8 + self.rng.random() * 0.3)

    def adern(self, rect, f, anzahl=2, laenge=6):
        u, v, b, h = rect
        for _ in range(anzahl):
            x = self.rng.integers(0, b)
            y = self.rng.integers(0, h)
            for _ in range(laenge):
                if 0 <= x < b and 0 <= y < h:
                    self.bild[v + y, u + x, :3] = self.bild[v + y, u + x, :3] * 0.4 + f[:3] * 0.6
                x += self.rng.integers(-1, 2)
                y += self.rng.integers(0, 2)

    def streifen(self, rect, f, abstand=3, senkrecht=True, staerke=0.35):
        u, v, b, h = rect
        reg = self.bild[v:v + h, u:u + b]
        if senkrecht:
            for x in range(self.rng.integers(0, abstand), b, abstand):
                reg[:, x, :3] = reg[:, x, :3] * (1 - staerke) + f[:3] * staerke
        else:
            for y in range(self.rng.integers(0, abstand), h, abstand):
                reg[y, :, :3] = reg[y, :, :3] * (1 - staerke) + f[:3] * staerke

    def loecher(self, rect, f, anzahl=2, groesse=2, transparent=False):
        """Risse im Stoff, durch die Haut (f) zu sehen ist – oder echte Löcher (transparent)."""
        u, v, b, h = rect
        for _ in range(anzahl):
            cx = self.rng.integers(0, b)
            cy = self.rng.integers(0, h)
            for dy in range(-groesse, groesse + 1):
                for dx in range(-groesse, groesse + 1):
                    if dx * dx + dy * dy * 1.8 <= groesse * groesse * self.rng.uniform(0.5, 1.2):
                        x, y = cx + dx, cy + dy
                        if 0 <= x < b and 0 <= y < h:
                            if transparent:
                                self.bild[v + y, u + x, 3] = 0
                            else:
                                self.bild[v + y, u + x, :3] = f[:3] + (self.rng.random(3) - 0.5) * 16

    def ausgefranst(self, rect, unten=True, tiefe=2):
        """Unterer Rand ausgerissen (transparent)."""
        u, v, b, h = rect
        for x in range(b):
            t = self.rng.integers(0, tiefe + 1)
            for y in range(t):
                yy = h - 1 - y if unten else y
                self.bild[v + yy, u + x, 3] = 0

    def vorlage(self, rect, zeilen, palette, glow_zeichen="", anker="mitte", dy=0, dx=0):
        """Pixel-Vorlage aus Textzeilen auf eine Fläche setzen. '.' = nichts ändern."""
        u, v, b, h = rect
        hz = len(zeilen)
        bz = max(len(z) for z in zeilen)
        x0 = u + (b - bz) // 2 + dx
        y0 = v + ((h - hz) // 2 if anker == "mitte" else 0) + dy
        for j, z in enumerate(zeilen):
            for i, c in enumerate(z):
                if c == "." or c == " ":
                    continue
                f = palette[c]
                if f is None:
                    self.bild[y0 + j, x0 + i, 3] = 0
                    continue
                self.setze(x0 + i, y0 + j, f, glow=c in glow_zeichen)


# --- Materialien ---------------------------------------------------------
def haut(basis, flecken_f=None, adern_f=None, blut=0, dunkel_unten=0.15):
    b = farbe(basis)
    fl = farbe(flecken_f) if flecken_f else None
    ad = farbe(adern_f) if adern_f else None
    rot = farbe("5a1010")

    def malen(m, rect, flaeche, w):
        m.fuellen(rect, b, streuung=12, skala=3, dunkel_unten=dunkel_unten)
        if fl is not None:
            m.flecken(rect, fl, skala=2.5, schwelle=0.6, staerke=0.55)
        if ad is not None and rect[2] * rect[3] > 12:
            m.adern(rect, ad, anzahl=max(1, rect[2] * rect[3] // 40), laenge=5)
        if blut and flaeche in ("vorne", "rechts", "links"):
            m.spritzer(rect, rot, anzahl=blut, groesse=2)
    return malen


def stoff(basis, haut_f=None, loecher=1, blut=1, falten=3, fransen=False, schmutz="2e2a22"):
    b = farbe(basis)
    hf = farbe(haut_f) if haut_f else None
    rot = farbe("4a0c0c")
    dreck = farbe(schmutz)

    def malen(m, rect, flaeche, w):
        m.fuellen(rect, b, streuung=9, skala=2, dunkel_unten=0.25)
        m.streifen(rect, b * 0.6, abstand=falten, staerke=0.3)
        m.flecken(rect, dreck, skala=3, schwelle=0.65, staerke=0.45)
        if hf is not None and flaeche not in ("oben", "unten"):
            m.loecher(rect, hf, anzahl=loecher, groesse=1 + (rect[3] > 8))
        if blut and flaeche in ("vorne", "hinten", "rechts", "links"):
            m.spritzer(rect, rot, anzahl=blut, groesse=2)
        if fransen and flaeche not in ("oben", "unten"):
            m.ausgefranst(rect, tiefe=2)
    return malen


def fleisch(basis="7a1c1c", hell="b8473a"):
    b = farbe(basis)
    h = farbe(hell)

    def malen(m, rect, flaeche, w):
        m.fuellen(rect, b, streuung=16, skala=2)
        m.streifen(rect, h, abstand=2, senkrecht=w.groesse[1] >= w.groesse[0], staerke=0.35)
        m.flecken(rect, farbe("3a0808"), skala=2, schwelle=0.66, staerke=0.6)
    return malen


def metall(basis="5b6066", rost="7a3f1c", nieten=True):
    b = farbe(basis)
    r = farbe(rost)

    def malen(m, rect, flaeche, w):
        m.fuellen(rect, b, streuung=10, skala=4, rand=0.3)
        m.flecken(rect, r, skala=3, schwelle=0.68, staerke=0.6)
        m.flecken(rect, farbe("3b2414"), skala=2, schwelle=0.78, staerke=0.45)
        u, v, bb, hh = rect
        if nieten and bb >= 4 and hh >= 4:
            for x, y in ((1, 1), (bb - 2, 1), (1, hh - 2), (bb - 2, hh - 2)):
                m.setze(u + x, v + y, farbe("a7a9a5"))
        # Kratzer
        for _ in range(max(1, bb * hh // 30)):
            x = u + m.rng.integers(0, bb)
            y = v + m.rng.integers(0, hh)
            for i in range(m.rng.integers(2, 5)):
                m.setze(x + i, y + i // 2, farbe("8c9094"))
    return malen


def knochen(basis="cfc6a8"):
    b = farbe(basis)

    def malen(m, rect, flaeche, w):
        m.fuellen(rect, b, streuung=10, skala=2, dunkel_unten=0.2)
        m.adern(rect, farbe("6b6250"), anzahl=1, laenge=4)
    return malen


def einfarbig(basis, streuung=8, alpha=255):
    b = farbe(basis, alpha)

    def malen(m, rect, flaeche, w):
        m.fuellen(rect, b, streuung=streuung, skala=3)
    return malen


def nebel(basis="c9d3d6", alpha_min=60, alpha_max=200):
    b = farbe(basis)

    def malen(m, rect, flaeche, w):
        u, v, bb, hh = rect
        m.fuellen(rect, b, streuung=14, skala=3, dunkel_unten=-0.1, rand=0)
        n = rauschen(bb, hh, 2.5, m.rng)
        verlauf = np.linspace(1.0, 0.45, hh)[:, None]
        m.bild[v:v + hh, u:u + bb, 3] = (alpha_min + (alpha_max - alpha_min) * n) * verlauf
    return malen


def leuchtend(basis, streuung=10):
    b = farbe(basis)

    def malen(m, rect, flaeche, w):
        m.fuellen(rect, b, streuung=streuung, skala=2, rand=0)
        u, v, bb, hh = rect
        m.glow[v:v + hh, u:u + bb] = m.bild[v:v + hh, u:u + bb]
    return malen


def kombi(*materialien):
    def malen(m, rect, flaeche, w):
        for mat in materialien:
            mat(m, rect, flaeche, w)
    return malen
