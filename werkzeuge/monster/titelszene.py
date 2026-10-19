"""Titelbildschirm „Das Tor zur Zone“: gemaltes Rundum-Panorama mit echten Monstern aus dem Spiel im Nebel.

  python3 werkzeuge/monster/titelszene.py   – schreibt panorama_0…5 und eine Vorschau

Gesicht 0: Landstraße zum zerstörten Checkpoint, Signalfackeln, Wagen, ein Überlebender mit Lampe, am Himmel der
glühende Riss. Gesicht 1: toter Wald, aus dem eine Herde kommt – vorneweg der Hirte mit seiner Laterne.
Gesicht 2: abgestürzter Hubschrauber, der Juggernaut und Seuchenhunde. Gesicht 3: Gehöft im Nebel, die Nebelmutter.
"""
import math
import os
import random

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

import entwuerfe as E
import dimensionen  # noqa: F401  (registriert weitere Modelle)
from maler import Maler
from render import rendern

WURZEL = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
ZIEL = os.path.join(WURZEL, "src", "main", "resources", "assets", "minecraft", "textures", "gui", "title", "background")
VORSCHAU = os.path.join(WURZEL, "build", "vorschau")

N = 1024
B, H = 4 * N, N
HORIZONT = 570
NEBEL = np.array([78, 80, 72], dtype=np.float32)
rng = np.random.default_rng(1986)
zr = random.Random(1986)


def rauschen(b, h, skala, saat):
    r = np.random.default_rng(saat)
    klein = r.random((max(2, int(h / skala) + 2), max(2, int(b / skala) + 2)))
    return np.asarray(Image.fromarray((klein * 255).astype(np.uint8)).resize((b, h), Image.BICUBIC), dtype=np.float32) / 255


def weich(maske, radius):
    return np.asarray(Image.fromarray(np.clip(maske * 255, 0, 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(radius)),
                      dtype=np.float32) / 255


# ---- Himmel mit dem Riss ---------------------------------------------------------------------------
def himmel():
    y = np.linspace(0, 1, H)[:, None, None]
    oben = np.array([10, 14, 16], dtype=np.float32)
    unten = np.array([92, 86, 70], dtype=np.float32)
    t = np.clip(y / (HORIZONT / H), 0, 1) ** 1.6
    bild = oben * (1 - t) + unten * t
    bild = np.repeat(bild, B, axis=1)
    wolken = rauschen(B, H, 180, 1) * 0.55 + rauschen(B, H, 60, 2) * 0.3 + rauschen(B, H, 18, 3) * 0.15
    band = np.clip(1.3 - np.abs(y - 0.25) * 3.2, 0, 1)
    bild = bild * (0.75 + 0.5 * wolken[..., None] * band)
    return bild


def riss(bild, cx, oben, unten):
    """Der Riss am Himmel: gezackte, glühende Linie mit grünem Kern und orangem Hof, dazu Lichtstrahlen."""
    maske = Image.new("L", (B, H), 0)
    d = ImageDraw.Draw(maske)
    punkte = []
    x = cx
    for y in range(oben, unten, 14):
        x += zr.randint(-16, 16)
        punkte.append((x, y))
    # Linsenförmig aufgerissen: in der Mitte breit, an den Enden spitz
    links, rechts = [], []
    for i, (px, py) in enumerate(punkte):
        w = 2 + 22 * math.sin(math.pi * i / (len(punkte) - 1)) ** 1.5
        links.append((px - w + zr.randint(-3, 3), py))
        rechts.append((px + w + zr.randint(-3, 3), py))
    d.polygon(links + rechts[::-1], fill=255)
    for i in range(1, len(punkte) - 1):
        if zr.random() < 0.45:  # feine Seitenrisse
            px, py = punkte[i]
            ex, ey = px + zr.choice((-1, 1)) * zr.randint(30, 90), py + zr.randint(-20, 40)
            d.line([(px, py), ((px + ex) / 2 + zr.randint(-8, 8), (py + ey) / 2), (ex, ey)], fill=210, width=2)
    m = np.asarray(maske, dtype=np.float32) / 255
    kern = weich(m, 2)
    hof1 = weich(m, 14)
    hof2 = weich(m, 60)
    bild += kern[..., None] * np.array([230, 255, 200]) * 1.1
    bild += hof1[..., None] * np.array([90, 220, 110]) * 1.6
    bild += hof2[..., None] * np.array([210, 120, 50]) * 1.8
    # Lichtstrahlen fächern nach unten durch den Dunst
    strahlen = Image.new("L", (B, H), 0)
    d = ImageDraw.Draw(strahlen)
    mx, my = punkte[len(punkte) // 2]
    for i in range(14):
        w = math.radians(-60 + i * 9 + zr.uniform(-3, 3))
        laenge = zr.randint(500, 760)
        ex, ey = mx + math.sin(w) * laenge, my + math.cos(w) * laenge
        d.polygon([(mx, my), (ex - 30, ey), (ex + 30, ey)], fill=zr.randint(18, 40))
    s = weich(np.asarray(strahlen, dtype=np.float32) / 255, 12)
    bild += s[..., None] * np.array([160, 200, 120])
    return punkte


# ---- Gelände --------------------------------------------------------------------------------------
def huegel(bild, hoehe_max, farbe, nebel, saat):
    n = rauschen(B, 1, 200, saat)[0] * 0.7 + rauschen(B, 1, 50, saat + 1)[0] * 0.3
    for x in range(B):
        top = int(HORIZONT - n[x] * hoehe_max)
        bild[top:HORIZONT + 6, x] = np.array(farbe) * (1 - nebel) + NEBEL * nebel


def baum(d, x, boden, hoehe, breite, r, tiefe=0):
    """Toter Baum: Stamm und verzweigte, kahle Äste (als Maske)."""
    def ast(x0, y0, laenge, winkel, dicke, stufe):
        if stufe > 5 or laenge < 6:
            return
        x1 = x0 + math.sin(winkel) * laenge
        y1 = y0 - math.cos(winkel) * laenge
        d.line([(x0, y0), (x1, y1)], fill=255, width=max(1, int(dicke)))
        for _ in range(2 if stufe < 2 else r.choice((1, 2, 2, 3))):
            ast(x1, y1, laenge * r.uniform(0.55, 0.78), winkel + r.uniform(-0.75, 0.75), dicke * 0.62, stufe + 1)
    d.polygon([(x - breite, boden), (x + breite, boden), (x + breite * 0.4, boden - hoehe * 0.55), (x - breite * 0.4, boden - hoehe * 0.55)],
              fill=255)
    ast(x, boden - hoehe * 0.5, hoehe * 0.5, r.uniform(-0.15, 0.15), breite * 0.9, 0)


def wald(bild, schichten):
    for farbe, nebel, anzahl, (hmin, hmax), boden, aus in schichten:
        maske = Image.new("L", (B, H), 0)
        d = ImageDraw.Draw(maske)
        for _ in range(anzahl):
            x = zr.randint(0, B)
            if any(a <= x <= b for a, b in aus):
                continue
            h = zr.randint(hmin, hmax)
            baum(d, x, boden + zr.randint(-4, 4), h, max(2, h // 26), zr)
        m = np.asarray(maske, dtype=np.float32)[..., None] / 255
        f = np.array(farbe, dtype=np.float32) * (1 - nebel) + NEBEL * nebel
        bild[:] = bild * (1 - m) + f * m


def boden_malen(bild):
    y = np.arange(H)[:, None]
    unter = (y >= HORIZONT)
    tiefe = np.clip((y - HORIZONT) / (H - HORIZONT), 0, 1)
    gras = np.array([28, 30, 24], dtype=np.float32)
    n = rauschen(B, H, 40, 9)[..., None] * 0.7 + rauschen(B, H, 8, 10)[..., None] * 0.3
    farbe = gras * (0.7 + 0.4 * n)
    nebel = np.exp(-tiefe * 3.2)[..., None] * 0.85
    farbe = farbe * (1 - nebel) + NEBEL * nebel
    bild[:] = np.where(unter[..., None], farbe, bild)


def strasse(bild, vx):
    maske = Image.new("L", (B, H), 0)
    d = ImageDraw.Draw(maske)
    d.polygon([(vx - 4, HORIZONT), (vx + 4, HORIZONT), (vx + 560, H), (vx - 560, H)], fill=255)
    m = np.asarray(maske, dtype=np.float32)[..., None] / 255
    y = np.arange(H)[:, None, None]
    tiefe = np.clip((y - HORIZONT) / (H - HORIZONT), 0, 1)
    asphalt = np.array([30, 30, 32], dtype=np.float32) * (0.85 + 0.3 * rauschen(B, H, 3, 11)[..., None])
    nebel = np.exp(-tiefe * 3.0) * 0.85
    asphalt = asphalt * (1 - nebel) + NEBEL * nebel
    bild[:] = bild * (1 - m) + asphalt * m
    # Mittellinie (gestrichelt, verblichen) und Risse
    linie = Image.new("L", (B, H), 0)
    d = ImageDraw.Draw(linie)
    t = 0.0
    while t < 1:
        t2 = t + 0.04 + t * 0.05
        y0 = HORIZONT + (H - HORIZONT) * t ** 1.6
        y1 = HORIZONT + (H - HORIZONT) * min(1, t + 0.025) ** 1.6
        w = 1 + 7 * t
        d.polygon([(vx - w / 2, y0), (vx + w / 2, y0), (vx + w * 0.6, y1), (vx - w * 0.6, y1)], fill=150)
        t = t2
    for _ in range(40):
        x = vx + zr.randint(-450, 450)
        yy = zr.randint(HORIZONT + 80, H)
        if abs(x - vx) < (yy - HORIZONT) / (H - HORIZONT) * 540:
            d.line([(x, yy), (x + zr.randint(-40, 40), yy + zr.randint(-6, 6))], fill=255, width=1)
    l = np.asarray(linie, dtype=np.float32)[..., None] / 255
    hell = np.where(l > 0.7, np.array([12, 12, 12]), np.array([150, 140, 110]))
    bild[:] = bild * (1 - l * 0.6) + hell * l * 0.6


# ---- Scherenschnitt-Objekte -----------------------------------------------------------------------
def objekt(bild, zeichnen, farbe, nebel):
    maske = Image.new("L", (B, H), 0)
    d = ImageDraw.Draw(maske)
    zeichnen(d)
    m = np.asarray(maske, dtype=np.float32)[..., None] / 255
    f = np.array(farbe, dtype=np.float32) * (1 - nebel) + NEBEL * nebel
    bild[:] = bild * (1 - m) + f * m


def licht(bild, x, y, radius, farbe, staerke):
    yy, xx = np.ogrid[max(0, int(y - radius * 3)):min(H, int(y + radius * 3)), max(0, int(x - radius * 3)):min(B, int(x + radius * 3))]
    d = np.sqrt((xx - x) ** 2 + ((yy - y) * 1.0) ** 2)
    w = np.exp(-(d / radius) ** 2) * staerke
    bild[yy, xx] += w[..., None] * np.array(farbe, dtype=np.float32)


def checkpoint(d, vx):
    y = 640
    # Wachhäuschen links
    d.rectangle((vx - 150, y - 58, vx - 96, y), fill=255)
    d.polygon([(vx - 158, y - 58), (vx - 88, y - 58), (vx - 104, y - 70), (vx - 142, y - 70)], fill=255)
    # Schlagbaum, abgeknickt
    d.rectangle((vx - 80, y - 34, vx - 72, y), fill=255)
    d.line([(vx - 76, y - 30), (vx + 10, y - 44)], fill=255, width=5)
    d.line([(vx + 10, y - 44), (vx + 60, y - 10)], fill=255, width=5)
    # Betonsperren
    for bx in (vx - 40, vx + 70, vx + 120):
        d.polygon([(bx, y), (bx + 26, y), (bx + 20, y - 16), (bx + 6, y - 16)], fill=255)
    # Zaun mit Stacheldraht zu beiden Seiten
    for seite in (-1, 1):
        x0 = vx + seite * 170
        for k in range(14):
            px = x0 + seite * k * 34
            d.line([(px, y + 4), (px, y - 46)], fill=255, width=3)
        for h in (8, 22, 36):
            d.line([(x0, y - h), (x0 + seite * 13 * 34, y - h + seite * 2)], fill=255, width=1)
    # Schildpfosten rechts
    d.line([(vx + 190, y), (vx + 190, y - 80)], fill=255, width=4)


def warnschild(bild, x, y):
    s = Image.new("RGBA", (B, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(s)
    d.polygon([(x, y - 38), (x + 34, y + 20), (x - 34, y + 20)], fill=(196, 160, 40, 235), outline=(30, 24, 10, 255))
    d.ellipse((x - 5, y - 2, x + 5, y + 8), fill=(20, 16, 10, 255))
    for w in (90, 210, 330):
        a = math.radians(w)
        d.pieslice((x - 16, y - 13, x + 16, y + 19), w - 30, w + 30, fill=(20, 16, 10, 255))
    d.ellipse((x - 4, y - 1, x + 4, y + 7), fill=(196, 160, 40, 255))
    d.ellipse((x - 2, y + 1, x + 2, y + 5), fill=(20, 16, 10, 255))
    a = np.asarray(s, dtype=np.float32)
    m = a[..., 3:4] / 255 * 0.85
    farbe = a[..., :3] * 0.7 + NEBEL * 0.3
    bild[:] = bild * (1 - m) + farbe * m


def wagen(d, x, y, laenge):
    h = laenge * 0.34
    d.polygon([(x, y - h * 0.18), (x + laenge * 0.06, y - h * 0.55), (x + laenge * 0.3, y - h * 0.6), (x + laenge * 0.4, y - h),
               (x + laenge * 0.74, y - h), (x + laenge * 0.86, y - h * 0.58), (x + laenge, y - h * 0.5), (x + laenge, y - h * 0.14)], fill=255)
    for rx in (0.2, 0.8):
        d.ellipse((x + laenge * rx - h * 0.24, y - h * 0.3, x + laenge * rx + h * 0.24, y + h * 0.16), fill=255)
    # offene Fahrertür
    d.polygon([(x + laenge * 0.42, y - h * 0.62), (x + laenge * 0.36, y - h * 0.98), (x + laenge * 0.2, y - h * 1.1), (x + laenge * 0.24, y - h * 0.5)],
              fill=255)


def hubschrauber(d, x, y, laenge):
    h = laenge * 0.3
    d.polygon([(x, y - h * 0.2), (x + laenge * 0.12, y - h), (x + laenge * 0.45, y - h * 1.05), (x + laenge * 0.55, y - h * 0.4),
               (x + laenge, y - h * 0.8), (x + laenge, y - h * 0.62), (x + laenge * 0.55, y - h * 0.08), (x + laenge * 0.1, y)], fill=255)
    d.line([(x + laenge * 0.28, y - h * 1.05), (x - laenge * 0.25, y - h * 1.7)], fill=255, width=5)
    d.line([(x + laenge * 0.28, y - h * 1.05), (x + laenge * 0.8, y - h * 1.2)], fill=255, width=5)
    d.line([(x + laenge * 0.97, y - h * 0.72), (x + laenge * 1.03, y - h * 1.25)], fill=255, width=4)


def gehoeft(d, x, y):
    d.rectangle((x, y - 120, x + 220, y), fill=255)
    d.polygon([(x - 16, y - 120), (x + 110, y - 210), (x + 236, y - 120)], fill=255)
    d.rectangle((x + 150, y - 260, x + 172, y - 160), fill=255)  # Schornstein
    d.rectangle((x + 260, y - 90, x + 360, y), fill=255)  # Scheune, halb eingestürzt
    d.polygon([(x + 250, y - 90), (x + 300, y - 140), (x + 340, y - 104), (x + 370, y - 90)], fill=255)


def rauch(bild, x, y, hoehe, breite, dunkel=26):
    for yy in range(max(0, y - hoehe), y):
        k = (y - yy) / hoehe
        w = breite * (0.4 + 2.2 * k)
        dx = int(math.sin(k * 4 + x) * 30 * k + k * k * 220)
        x0, x1 = int(x + dx - w), int(x + dx + w)
        x0, x1 = max(0, x0), min(B, x1)
        if x1 <= x0:
            continue
        xs = np.arange(x0, x1)
        dichte = np.clip(1 - np.abs(xs - (x + dx)) / w, 0, 1) * (0.8 - 0.65 * k) * (0.7 + 0.6 * rng.random(len(xs)))
        bild[yy, x0:x1] = bild[yy, x0:x1] * (1 - dichte[:, None]) + np.array([dunkel] * 3) * dichte[:, None]


# ---- Monster aus dem Spiel ------------------------------------------------------------------------
_MODELLE = {}


def monster_bild(name, hoehe_px, gier):
    if name not in _MODELLE:
        modell, *_ = E.ALLE[name]()
        modell.vorwaerts_richten(E.NICHT_SPIEGELN.get(name, ()))
        modell.uv_packen()
        maler = Maler(modell, saat=sum(map(ord, name)))
        maler.alles_malen()
        _MODELLE[name] = (modell, maler)
    modell, maler = _MODELLE[name]
    h = int(hoehe_px / 0.8)
    b = int(h * 0.9)
    return rendern(modell, maler.bild, maler.glow, gier=gier, neigung=3, breite=b, hoehe=h, boden=False, freigestellt=True)


def monster(bild, name, x, fuss_y, hoehe_px, nebel, gier=0, dunkel=0.42, leucht=1.0):
    rgba, glow = monster_bild(name, hoehe_px, gier)
    a = np.asarray(rgba, dtype=np.float32)
    g = np.asarray(glow, dtype=np.float32)
    h, b = a.shape[:2]
    # Fußpunkt: unterste Modellzeile
    zeilen = np.where(a[..., 3].max(axis=1) > 10)[0]
    if len(zeilen) == 0:
        return
    unten = zeilen.max()
    x0, y0 = int(x - b / 2), int(fuss_y - unten)
    xa, ya = max(0, x0), max(0, y0)
    xb, yb = min(B, x0 + b), min(H, y0 + h)
    if xb <= xa or yb <= ya:
        return
    ausschnitt = a[ya - y0:yb - y0, xa - x0:xb - x0]
    m = ausschnitt[..., 3:4] / 255
    farbe = ausschnitt[..., :3] * dunkel * (1 - nebel) + NEBEL * nebel
    bild[ya:yb, xa:xb] = bild[ya:yb, xa:xb] * (1 - m) + farbe * m
    gl = g[ya - y0:yb - y0, xa - x0:xb - x0]
    if gl.max() > 0:
        bloom = np.asarray(Image.fromarray(np.clip(gl, 0, 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(4)), dtype=np.float32)
        bild[ya:yb, xa:xb] += (gl * 1.2 + bloom * 1.8) * leucht * (1 - nebel * 0.5)


# ---- Komposition ----------------------------------------------------------------------------------
def szene():
    bild = himmel()
    vx = 512  # Fluchtpunkt der Straße (Gesicht 0)
    riss(bild, vx + 40, 70, 360)
    huegel(bild, 60, (40, 42, 38), 0.7, 21)
    huegel(bild, 34, (30, 32, 28), 0.55, 23)
    wald(bild, [((34, 34, 30), 0.72, 260, (60, 120), HORIZONT + 6, [(vx - 200, vx + 200)])])
    boden_malen(bild)
    strasse(bild, vx)
    wald(bild, [((22, 22, 20), 0.5, 120, (130, 240), HORIZONT + 50, [(vx - 330, vx + 330)])])
    # Gesicht 0: Checkpoint, Wagen, Fackeln, Überlebender
    monster(bild, "wanderer", vx - 20, 632, 42, 0.62, gier=10)
    monster(bild, "wanderer", vx + 34, 630, 38, 0.66, gier=-20)
    monster(bild, "schreier", vx + 90, 628, 46, 0.66, gier=-30)
    objekt(bild, lambda d: checkpoint(d, vx), (26, 24, 22), 0.45)
    monster(bild, "kriecher", vx - 124, 572, 22, 0.45, gier=70)
    warnschild(bild, vx + 190, 548)
    objekt(bild, lambda d: wagen(d, vx - 420, 800, 300), (14, 14, 14), 0.18)
    licht(bild, vx - 140, 760, 14, (220, 200, 140), 0.9)  # schwacher Scheinwerfer
    for fx, fy in ((vx - 70, 760), (vx + 60, 880), (vx - 10, 690)):
        licht(bild, fx, fy, 70, (200, 30, 20), 0.55)
        licht(bild, fx, fy, 8, (255, 140, 110), 2.4)
        rauch(bild, fx, fy, 130, 6, dunkel=70)
    # Überlebender mit Lampe, der Lichtkegel sticht in den Nebel
    kegel = Image.new("L", (B, H), 0)
    d = ImageDraw.Draw(kegel)
    sx, sy = vx + 150, 930
    d.polygon([(sx - 6, sy - 70), (vx - 40, 610), (vx + 80, 600)], fill=60)
    k = weich(np.asarray(kegel, dtype=np.float32) / 255, 10)
    bild += k[..., None] * np.array([230, 220, 180])
    objekt(bild, lambda d: (d.ellipse((sx - 9, sy - 104, sx + 9, sy - 86), fill=255),
                            d.polygon([(sx - 14, sy - 86), (sx + 14, sy - 86), (sx + 10, sy - 40), (sx - 10, sy - 40)], fill=255),
                            d.rectangle((sx - 10, sy - 42, sx - 3, sy), fill=255), d.rectangle((sx + 3, sy - 42, sx + 10, sy), fill=255),
                            d.line([(sx - 12, sy - 82), (sx - 18, sy - 64)], fill=255, width=6),
                            d.rectangle((sx - 20, sy - 70, sx - 14, sy - 62), fill=255)), (8, 8, 8), 0.0)
    licht(bild, sx - 17, sy - 66, 6, (255, 240, 200), 3.0)
    # Gesicht 1: toter Wald, die Herde kommt, vorneweg der Hirte mit Laterne
    f1 = N
    herde = []
    for i in range(11):
        tiefe = zr.random()
        herde.append((tiefe, i))
    for tiefe, i in sorted(herde, reverse=True):  # hinten zuerst
        monster(bild, zr.choice(["wanderer", "wanderer", "schreier", "lauerer", "wanderer"]), f1 + 100 + i * 80 + zr.randint(-20, 20),
                int(HORIZONT + 40 + (1 - tiefe) * 170), int(70 + (1 - tiefe) * 90), 0.2 + tiefe * 0.35, gier=zr.randint(-40, 40))
    monster(bild, "hirte", f1 + 520, 760, 230, 0.25, gier=-15)
    wald(bild, [((14, 14, 12), 0.25, 18, (380, 560), H - 60, [(0, N), (2 * N, 4 * N)])])
    # Gesicht 2: abgestürzter Hubschrauber, der Juggernaut und Seuchenhunde
    f2 = 2 * N
    objekt(bild, lambda d: hubschrauber(d, f2 + 120, 690, 360), (22, 22, 20), 0.4)
    rauch(bild, f2 + 300, 640, 480, 18)
    licht(bild, f2 + 290, 650, 40, (230, 110, 40), 0.7)
    monster(bild, "juggernaut", f2 + 640, 800, 260, 0.22, gier=-25)
    for i, (dx, dy, s) in enumerate(((440, 800, 110), (800, 780, 100), (880, 840, 125))):
        monster(bild, "seuchenhund", f2 + dx, dy, s, 0.3, gier=-60 + i * 50)
    # Gesicht 3: Gehöft im Nebel, die Nebelmutter überragt alles
    f3 = 3 * N
    objekt(bild, lambda d: gehoeft(d, f3 + 120, 680), (24, 24, 22), 0.5)
    monster(bild, "nebelmutter", f3 + 640, 720, 520, 0.55, gier=-10, leucht=1.4)
    monster(bild, "nebelwesen", f3 + 820, 700, 120, 0.6, gier=-30)
    monster(bild, "wanderer", f3 + 420, 740, 80, 0.45, gier=20)
    # Nebelschwaden über allem, dazu Filmkorn
    schwaden = rauschen(B, H, 90, 31) * rauschen(B, H, 30, 32)
    y = np.linspace(0, 1, H)[:, None]
    band = np.exp(-((y - HORIZONT / H - 0.05) / 0.09) ** 2)
    bild = bild * (1 - (schwaden * band * 0.55)[..., None]) + NEBEL * (schwaden * band * 0.55)[..., None]
    bild += (rng.random((H, B, 1)) - 0.5) * 8
    return np.clip(bild, 0, 255).astype(np.uint8)


def main():
    bild = Image.fromarray(szene()).filter(ImageFilter.GaussianBlur(0.6))
    os.makedirs(ZIEL, exist_ok=True)
    for i in range(4):
        bild.crop((i * N, 0, (i + 1) * N, N)).save(os.path.join(ZIEL, f"panorama_{i}.png"))
    oben = Image.new("RGB", (N, N), (12, 15, 17))
    oben.save(os.path.join(ZIEL, "panorama_4.png"))
    Image.new("RGB", (N, N), (20, 22, 18)).save(os.path.join(ZIEL, "panorama_5.png"))
    os.makedirs(VORSCHAU, exist_ok=True)
    bild.resize((B // 2, H // 2)).save(os.path.join(VORSCHAU, "titelszene.png"))
    print("Titelszene geschrieben.")


if __name__ == "__main__":
    main()
