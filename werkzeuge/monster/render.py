"""Kleiner Software-Renderer für die Monster-Vorschau: rendert ein Box-UV-Modell mit seiner Textur
in Minecraft-Optik (flache Flächen, Pixeltextur), mit Licht, Nebel, Schatten und leuchtenden Augen.
"""
import math

import numpy as np
from PIL import Image, ImageDraw, ImageFilter, ImageFont

from modell import drehmatrix, flaechen

SCHRIFT = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"
SCHRIFT_N = "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"


def mat4(r=None, t=None):
    m = np.eye(4)
    if r is not None:
        m[:3, :3] = r
    if t is not None:
        m[:3, 3] = t
    return m


def welt_matrizen(modell, mit_pose=True):
    nach_name = {k.name: k for k in modell.knochen}
    cache = {}

    def berechne(kn):
        if kn.name in cache:
            return cache[kn.name]
        dreh = list(kn.drehung)
        pos = np.zeros(3)
        skal = np.ones(3)
        if mit_pose and kn.name in modell.pose:
            pz = modell.pose[kn.name]
            if isinstance(pz, dict):
                dreh = [a + b for a, b in zip(dreh, pz.get("rotation", (0, 0, 0)))]
                pos = np.array(pz.get("position", (0, 0, 0)), dtype=float)
                skal = np.array(pz.get("scale", (1, 1, 1)), dtype=float)
            else:
                dreh = [a + b for a, b in zip(dreh, pz)]
        p = np.array(kn.drehpunkt)
        lokal = mat4(t=p + pos) @ mat4(r=drehmatrix(dreh) @ np.diag(skal)) @ mat4(t=-p)
        if kn.eltern:
            lokal = berechne(nach_name[kn.eltern]) @ lokal
        cache[kn.name] = lokal
        return lokal

    return {k.name: berechne(k) for k in modell.knochen}


def flaechen_3d(w):
    i = w.aufblasen
    x0, y0, z0 = (a - i for a in w.ursprung)
    x1, y1, z1 = (a + g + i for a, g in zip(w.ursprung, w.groesse))
    # Ecken in der Reihenfolge oben-links, oben-rechts, unten-links, unten-rechts der Texturfläche
    return {
        "vorne": [(x1, y1, z0), (x0, y1, z0), (x1, y0, z0), (x0, y0, z0)],
        "hinten": [(x0, y1, z1), (x1, y1, z1), (x0, y0, z1), (x1, y0, z1)],
        "rechts": [(x1, y1, z1), (x1, y1, z0), (x1, y0, z1), (x1, y0, z0)],
        "links": [(x0, y1, z0), (x0, y1, z1), (x0, y0, z0), (x0, y0, z1)],
        "oben": [(x1, y1, z1), (x0, y1, z1), (x1, y1, z0), (x0, y1, z0)],
        "unten": [(x1, y0, z0), (x0, y0, z0), (x1, y0, z1), (x0, y0, z1)],
    }


def rendern(modell, textur, glow, gier=-30, neigung=12, breite=520, hoehe=720, skala=None, mit_pose=True,
            nebel_farbe=(38, 44, 44), boden=True, mitte_y=None, mitte_x=None, mit_mitte=False):
    """Gibt ein PIL-Bild zurück. gier: Drehung um die Hochachse (Grad), neigung: Blick von oben."""
    ss = 2  # Supersampling
    B, H = breite * ss, hoehe * ss
    mats = welt_matrizen(modell, mit_pose)
    blick = drehmatrix((neigung, 0, 0)) @ drehmatrix((0, gier, 0))

    # Alle Dreiecke sammeln
    tris = []  # (p0,p1,p2 screen), (uv0,uv1,uv2), normal_view
    punkte = []
    for kn, w in modell.alle_wuerfel():
        m = mats[kn.name]
        rects = flaechen(w)
        for name, ecken in flaechen_3d(w).items():
            u, v, bb, hh = rects[name]
            if bb <= 0 or hh <= 0:
                continue
            p = np.array([m @ np.array([*e, 1.0]) for e in ecken])[:, :3]
            pv = (blick @ p.T).T
            uv = np.array([(u, v), (u + bb, v), (u, v + hh), (u + bb, v + hh)], dtype=float)
            n = np.cross(pv[1] - pv[0], pv[2] - pv[0])
            ln = np.linalg.norm(n)
            if ln < 1e-9:
                continue
            n = n / ln
            punkte.append(pv)
            for a, b, c in ((0, 1, 2), (1, 3, 2)):
                tris.append((pv[[a, b, c]], uv[[a, b, c]], n))
    alle = np.concatenate(punkte)
    # Bildausschnitt: Modell einpassen
    xmin, xmax = (-alle[:, 0]).min(), (-alle[:, 0]).max()
    ymin, ymax = alle[:, 1].min(), alle[:, 1].max()
    if skala is None:
        skala = min(B * 0.78 / max(1, xmax - xmin), H * 0.8 / max(1, ymax - ymin))
    else:
        skala *= ss
    cx = (xmin + xmax) / 2 if mitte_x is None else mitte_x
    cy = (ymin + ymax) / 2 if mitte_y is None else mitte_y

    def bild_xy(p):
        return (B / 2 + (-p[..., 0] - cx) * skala, H * 0.52 - (p[..., 1] - cy) * skala)

    farbe = np.zeros((H, B, 3))
    tiefe = np.full((H, B), np.inf)
    leucht = np.zeros((H, B, 3))
    maske = np.zeros((H, B))

    # Hintergrund: Verlauf + Nebelschwaden
    yy = np.linspace(0, 1, H)[:, None]
    hg = np.array(nebel_farbe, dtype=float)
    farbe[:] = hg * (0.55 + 0.6 * yy[..., None] * (1 - yy[..., None]) * 2.2)
    rng = np.random.default_rng(3)
    wolke = np.array(Image.fromarray((rng.random((H // 40 + 2, B // 40 + 2)) * 255).astype(np.uint8)).resize((B, H), Image.BICUBIC)) / 255
    farbe += (wolke[..., None] - 0.5) * 18

    licht = np.array([0.45, 0.6, -0.65])
    licht /= np.linalg.norm(licht)
    tex = textur
    th, tw = tex.shape[:2]

    durchsichtig = []

    def raster(tri, mit_alpha):
        pv, uv, n = tri
        xs, ys = bild_xy(pv)
        z = pv[:, 2]
        x0 = max(int(math.floor(xs.min())), 0)
        x1 = min(int(math.ceil(xs.max())), B - 1)
        y0 = max(int(math.floor(ys.min())), 0)
        y1 = min(int(math.ceil(ys.max())), H - 1)
        if x1 < x0 or y1 < y0:
            return
        gx, gy = np.meshgrid(np.arange(x0, x1 + 1) + 0.5, np.arange(y0, y1 + 1) + 0.5)
        d = (ys[1] - ys[2]) * (xs[0] - xs[2]) + (xs[2] - xs[1]) * (ys[0] - ys[2])
        if abs(d) < 1e-9:
            return
        l0 = ((ys[1] - ys[2]) * (gx - xs[2]) + (xs[2] - xs[1]) * (gy - ys[2])) / d
        l1 = ((ys[2] - ys[0]) * (gx - xs[2]) + (xs[0] - xs[2]) * (gy - ys[2])) / d
        l2 = 1 - l0 - l1
        drin = (l0 >= -1e-4) & (l1 >= -1e-4) & (l2 >= -1e-4)
        if not drin.any():
            return
        zz = l0 * z[0] + l1 * z[1] + l2 * z[2]
        uu = l0 * uv[0, 0] + l1 * uv[1, 0] + l2 * uv[2, 0]
        vv = l0 * uv[0, 1] + l1 * uv[1, 1] + l2 * uv[2, 1]
        ui = np.clip(np.floor(uu - 1e-6).astype(int), 0, tw - 1)
        vi = np.clip(np.floor(vv - 1e-6).astype(int), 0, th - 1)
        texel = tex[vi, ui]
        a = texel[..., 3] / 255.0
        tz = tiefe[y0:y1 + 1, x0:x1 + 1]
        if mit_alpha:
            ok = drin & (a > 0.02) & (zz < tz)
        else:
            ok = drin & (a > 0.5) & (zz < tz)
        if not ok.any():
            return
        # Licht: Minecraft-ähnlich flach + gerichtet + kühler Randlicht
        nn = n if n[2] < 0 else -n  # zur Kamera zeigende Normale
        diff = max(0.0, float(np.dot(nn, licht)))
        rand = (1 - abs(nn[2])) ** 2
        hell = 0.42 + 0.7 * diff
        col = texel[..., :3] * hell + np.array([60, 90, 95]) * rand * 0.35
        # Tiefennebel
        nebelanteil = np.clip((zz + 20) / 120, 0, 0.35)[..., None]
        col = col * (1 - nebelanteil) + hg * nebelanteil
        reg = farbe[y0:y1 + 1, x0:x1 + 1]
        if mit_alpha:
            reg[ok] = reg[ok] * (1 - a[ok, None]) + col[ok] * a[ok, None]
        else:
            reg[ok] = col[ok]
            tz[ok] = zz[ok]
            maske[y0:y1 + 1, x0:x1 + 1][ok] = 1
            leucht[y0:y1 + 1, x0:x1 + 1][ok] = 0  # verdeckt, was vorher dort leuchtete
        g = glow[vi, ui]
        gl = g[..., 3] > 0
        if gl.any():
            sel = ok & gl
            leucht[y0:y1 + 1, x0:x1 + 1][sel] = g[sel][:, :3]
            reg[sel] = g[sel][:, :3]

    for tri in tris:
        u0, v0 = tri[1].mean(axis=0)
        a = tex[min(int(v0), th - 1), min(int(u0), tw - 1), 3]
        if 0 < a < 240:
            durchsichtig.append(tri)
        else:
            raster(tri, False)
    durchsichtig.sort(key=lambda t: -t[0][:, 2].mean())
    for tri in durchsichtig:
        raster(tri, True)

    bild = Image.fromarray(np.clip(farbe, 0, 255).astype(np.uint8))
    # Boden-Schatten unter das Modell legen (vor dem Modell gerendert wäre schöner; hier nachträglich unter die Maske)
    if boden:
        fuss = (blick @ np.array([0, 0, 0])).reshape(1, 3)
        fx, fy = bild_xy(fuss)
        schatten = Image.new("L", (B, H), 0)
        dr = ImageDraw.Draw(schatten)
        rx = min(xmax - xmin, 30) * skala * 0.32
        dr.ellipse([fx[0] - rx, fy[0] - rx * 0.16, fx[0] + rx, fy[0] + rx * 0.16], fill=120)
        schatten = schatten.filter(ImageFilter.GaussianBlur(6 * ss))
        s = np.array(schatten) / 255.0 * (1 - maske)
        arr = np.array(bild).astype(float) * (1 - 0.6 * s[..., None])
        bild = Image.fromarray(np.clip(arr, 0, 255).astype(np.uint8))
    # Leuchten (Bloom)
    if leucht.any():
        lb = Image.fromarray(np.clip(leucht, 0, 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(9 * ss))
        lb2 = Image.fromarray(np.clip(leucht, 0, 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(3 * ss))
        arr = np.array(bild).astype(float) + np.array(lb).astype(float) * 1.6 + np.array(lb2).astype(float) * 0.8
        bild = Image.fromarray(np.clip(arr, 0, 255).astype(np.uint8))
    # Bodennebel
    arr = np.array(bild).astype(float)
    nb = np.clip((yy - 0.72) / 0.28, 0, 1)[..., None] * 0.55
    arr = arr * (1 - nb) + hg * 1.25 * nb
    # Vignette
    vx = np.linspace(-1, 1, B)[None, :]
    vy = np.linspace(-1, 1, H)[:, None]
    vig = np.clip(1 - 0.45 * (vx ** 2 + vy ** 2), 0.35, 1)[..., None]
    arr *= vig
    bild = Image.fromarray(np.clip(arr, 0, 255).astype(np.uint8))
    if mit_mitte:
        return bild.resize((breite, hoehe), Image.LANCZOS), skala / ss, (cx, cy)
    return bild.resize((breite, hoehe), Image.LANCZOS), skala / ss


def blatt(modell, textur, glow, titel, untertitel, ansichten=((-32, 10), (60, 6), (180 + 35, 10)), groesse_bloecke=None,
          nebel_farbe=(38, 44, 44)):
    """Vorschaublatt: drei Ansichten nebeneinander, Titel, Größe im Vergleich zum Spieler."""
    bilder = []
    # gleiche Skala für alle Ansichten
    test, skala = rendern(modell, textur, glow, *ansichten[0], nebel_farbe=nebel_farbe)
    for g, n in ansichten:
        b, _ = rendern(modell, textur, glow, g, n, skala=skala, nebel_farbe=nebel_farbe)
        bilder.append(b)
    bw, bh = bilder[0].size
    rand = 24
    kopf = 120
    W = bw * len(bilder) + rand * (len(bilder) + 1)
    Hh = bh + kopf + rand * 2
    blattbild = Image.new("RGB", (W, Hh), (14, 16, 16))
    for i, b in enumerate(bilder):
        blattbild.paste(b, (rand + i * (bw + rand), kopf + rand))
    d = ImageDraw.Draw(blattbild)
    d.text((rand, 22), titel, font=ImageFont.truetype(SCHRIFT, 46), fill=(226, 220, 205))
    d.text((rand, 78), untertitel, font=ImageFont.truetype(SCHRIFT_N, 22), fill=(150, 160, 155))
    if groesse_bloecke:
        d.text((W - rand - 330, 34), f"Größe: {groesse_bloecke}", font=ImageFont.truetype(SCHRIFT_N, 22),
               fill=(150, 160, 155), align="right")
    return blattbild
