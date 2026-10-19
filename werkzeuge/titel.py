"""Titelbildschirm von Deadzone MC: Schriftzug, Untertitel, Ladebildschirm-Logo, Panorama und Sprüche.

  python3 werkzeuge/titel.py   – schreibt nach src/main/resources/assets/minecraft/…

Alles wird hier gezeichnet (keine fremden Bilder). Die Dateien ersetzen die Minecraft-Originale, weil
Mod-Ressourcen Vorrang vor den Vanilla-Ressourcen haben.
"""
import math
import os
import random

import numpy as np
from PIL import Image, ImageDraw, ImageFilter, ImageFont

WURZEL = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ZIEL = os.path.join(WURZEL, "src", "main", "resources", "assets", "minecraft")
VORSCHAU = os.path.join(WURZEL, "build", "vorschau")

# ---- Klotzschrift (Einheiten-Raster wie beim Minecraft-Logo) --------------------------------------
BUCHSTABEN = {
    "D": ["XXXX.", "XX.XX", "XX.XX", "XX.XX", "XX.XX", "XX.XX", "XXXX."],
    "E": ["XXXXX", "XX...", "XX...", "XXXX.", "XX...", "XX...", "XXXXX"],
    "A": [".XXX.", "XX.XX", "XX.XX", "XXXXX", "XX.XX", "XX.XX", "XX.XX"],
    "Z": ["XXXXX", "...XX", "..XX.", ".XX..", "XX...", "XX...", "XXXXX"],
    "O": [".XXX.", "XX.XX", "XX.XX", "XX.XX", "XX.XX", "XX.XX", ".XXX."],
    "N": ["XX..XX", "XXX.XX", "XXXXXX", "XX.XXX", "XX..XX", "XX..XX", "XX..XX"],
}


def raster(wort):
    """Das Wort als Liste von (x, y)-Einheiten."""
    zellen = []
    x0 = 0
    for b in wort:
        z = BUCHSTABEN[b]
        for y, zeile in enumerate(z):
            for x, c in enumerate(zeile):
                if c == "X":
                    zellen.append((x0 + x, y))
        x0 += len(z[0]) + 1
    return zellen, x0 - 1


def rauschen(b, h, skala, rng):
    """Weiches Wertrauschen (für Flecken)."""
    kb, kh = max(2, int(b / skala) + 2), max(2, int(h / skala) + 2)
    gitter = rng.random((kh, kb))
    bild = Image.fromarray((gitter * 255).astype(np.uint8)).resize((b, h), Image.BICUBIC)
    return np.asarray(bild, dtype=np.float32) / 255.0


def schriftzug(wort, einheit, tiefe, rand=6, glut=True, saat=7):
    """Der Schriftzug als RGBA-Bild: Beton mit Rost und Rissen, dunkle Kante, rotes Glühen dahinter."""
    rng = np.random.default_rng(saat)
    zellen, breite = raster(wort)
    w = breite * einheit + tiefe + 2 * rand
    h = 7 * einheit + tiefe + 2 * rand
    belegt = set(zellen)

    # Oberfläche: Betonfarbe mit Flecken
    grund = np.zeros((h, w, 4), dtype=np.float32)
    fleck = rauschen(w, h, einheit * 0.8, rng)
    fein = rng.random((h, w))
    rost = rauschen(w, h, einheit * 1.6, rng)

    def blockfarbe(px, py, oben, unten):
        t = (py - rand) / (7 * einheit)
        c = np.array(oben) * (1 - t) + np.array(unten) * t
        c = c * (0.86 + 0.18 * fleck[py, px] + 0.06 * (fein[py, px] - 0.5))
        if rost[py, px] > 0.8:
            r = min(0.75, (rost[py, px] - 0.8) * 4)
            c = c * (1 - r) + np.array([118, 62, 36]) * r
        return c

    # Seiten (Extrusion) zuerst
    for (ux, uy) in zellen:
        for d in range(tiefe, 0, -1):
            x0 = rand + ux * einheit + d
            y0 = rand + uy * einheit + d
            for py in range(y0, y0 + einheit):
                for px in range(x0, x0 + einheit):
                    c = np.array([52, 48, 44]) * (0.8 + 0.3 * fleck[py, px])
                    grund[py, px, :3] = c
                    grund[py, px, 3] = 255
    # Vorderseite mit Kantenlicht
    for (ux, uy) in zellen:
        x0 = rand + ux * einheit
        y0 = rand + uy * einheit
        for py in range(y0, y0 + einheit):
            for px in range(x0, x0 + einheit):
                c = blockfarbe(px, py, (196, 192, 180), (104, 100, 92))
                lx, ly = px - x0, py - y0
                k = max(1, einheit // 8)
                if ly < k and (ux, uy - 1) not in belegt:
                    c = c * 1.25
                if lx < k and (ux - 1, uy) not in belegt:
                    c = c * 1.12
                if ly >= einheit - k and (ux, uy + 1) not in belegt:
                    c = c * 0.7
                if lx >= einheit - k and (ux + 1, uy) not in belegt:
                    c = c * 0.78
                grund[py, px, :3] = np.clip(c, 0, 255)
                grund[py, px, 3] = 255
    bild = Image.fromarray(grund.astype(np.uint8), "RGBA")
    zeich = ImageDraw.Draw(bild)
    r = random.Random(saat)
    # Risse
    for _ in range(len(zellen) // 3):
        ux, uy = r.choice(zellen)
        x = rand + ux * einheit + r.randint(0, einheit - 1)
        y = rand + uy * einheit + r.randint(0, einheit - 1)
        punkte = [(x, y)]
        for _ in range(r.randint(3, 7)):
            x += r.randint(-einheit // 3, einheit // 3)
            y += r.randint(0, einheit // 3)
            punkte.append((x, y))
        zeich.line(punkte, fill=(34, 30, 26, 255), width=max(1, einheit // 10))
    # Abgeplatzte Ecken
    for _ in range(len(zellen) // 6):
        ux, uy = r.choice(zellen)
        x = rand + ux * einheit + r.choice([0, einheit - einheit // 3])
        y = rand + uy * einheit + r.choice([0, einheit - einheit // 3])
        s = einheit // 3
        zeich.polygon([(x, y), (x + s, y), (x, y + s)], fill=(60, 55, 50, 255))
    # Rostläufer unter manchen Blöcken
    alpha = np.asarray(bild)[:, :, 3]
    for _ in range(len(zellen) // 4):
        ux, uy = r.choice(zellen)
        if (ux, uy + 1) in belegt:
            continue
        x = rand + ux * einheit + r.randint(1, einheit - 2)
        y = rand + (uy + 1) * einheit - 1
        laenge = r.randint(einheit // 3, einheit)
        zeich.line([(x, y - einheit // 2), (x, y + laenge)], fill=(110, 40, 22, 230), width=max(1, einheit // 8))
    # Dunkle Kontur
    maske = bild.split()[3]
    kontur = maske.filter(ImageFilter.MaxFilter(max(3, (einheit // 6) * 2 + 1)))
    erg = Image.new("RGBA", bild.size, (0, 0, 0, 0))
    schwarz = Image.new("RGBA", bild.size, (16, 13, 11, 255))
    erg.paste(schwarz, (0, 0), kontur)
    if glut:
        glow = kontur.filter(ImageFilter.GaussianBlur(einheit * 0.9))
        rot = Image.new("RGBA", bild.size, (190, 26, 16, 255))
        g = Image.new("RGBA", bild.size, (0, 0, 0, 0))
        g.paste(rot, (0, 0), glow.point(lambda v: int(v * 0.75)))
        erg = Image.alpha_composite(g, erg)
    erg = Image.alpha_composite(erg, bild)
    _ = alpha
    return erg


def logo():
    """textures/gui/title/minecraft.png: 256×64 logisch, das Spiel zeigt den Bereich 256×44."""
    s = 4
    einheit = 5 * s
    zug = schriftzug("DEADZONE", einheit, tiefe=4 * s, rand=2 * s)
    ziel = Image.new("RGBA", (256 * s, 64 * s), (0, 0, 0, 0))
    zug.thumbnail((244 * s, 44 * s), Image.LANCZOS)
    ziel.paste(zug, ((256 * s - zug.width) // 2, (44 * s - zug.height) // 2), zug)
    return ziel


def untertitel():
    """textures/gui/title/edition.png: 128×16 logisch, gezeigt wird 98×14 (unter dem rechten Teil des Logos)."""
    s = 4
    bild = Image.new("RGBA", (128 * s, 16 * s), (0, 0, 0, 0))
    z = ImageDraw.Draw(bild)
    try:
        schrift = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf", 10 * s)
    except OSError:
        schrift = ImageFont.load_default()
    text = "ÜBERLEBE DIE ZONE"
    tb = z.textbbox((0, 0), text, font=schrift)
    breite = tb[2] - tb[0]
    faktor = min(1.0, (98 * s - 4) / breite)
    tmp = Image.new("RGBA", (breite + 8, tb[3] - tb[1] + 8), (0, 0, 0, 0))
    tz = ImageDraw.Draw(tmp)
    tz.text((4 - tb[0] + 1, 4 - tb[1] + 1), text, font=schrift, fill=(20, 8, 6, 255))
    tz.text((4 - tb[0], 4 - tb[1]), text, font=schrift, fill=(214, 70, 40, 255))
    tmp = tmp.resize((int(tmp.width * faktor), int(tmp.height * faktor)), Image.LANCZOS)
    bild.paste(tmp, ((98 * s - tmp.width) // 2, (14 * s - tmp.height) // 2), tmp)
    return bild


def ladelogo():
    """textures/gui/title/mojangstudios.png: 120×120 logisch. Oben die linke Hälfte, unten die rechte.
    Weiße Klotzschrift mit ausgebrochenen Kanten (wird auf schwarzem Grund gezeigt)."""
    s = 4
    e = 5 * s
    zellen, breite = raster("DEADZONE")
    w = Image.new("RGBA", (breite * e, 7 * e), (0, 0, 0, 0))
    z = ImageDraw.Draw(w)
    r = random.Random(11)
    for (ux, uy) in zellen:
        z.rectangle((ux * e, uy * e, ux * e + e - 1, uy * e + e - 1), fill=(255, 255, 255, 255))
    for _ in range(len(zellen) // 3):
        ux, uy = r.choice(zellen)
        x = ux * e + r.randint(0, e - 1)
        y = uy * e + r.randint(0, e - 1)
        pts = [(x, y)]
        for _ in range(r.randint(2, 5)):
            x += r.randint(-e // 3, e // 3)
            y += r.randint(0, e // 3)
            pts.append((x, y))
        z.line(pts, fill=(0, 0, 0, 0), width=max(1, e // 10))
    breit = Image.new("RGBA", (240 * s, 60 * s), (0, 0, 0, 0))
    w.thumbnail((228 * s, 50 * s), Image.LANCZOS)
    breit.paste(w, ((240 * s - w.width) // 2, (60 * s - w.height) // 2), w)
    ziel = Image.new("RGBA", (120 * s, 120 * s), (0, 0, 0, 0))
    ziel.paste(breit.crop((0, 0, 120 * s, 60 * s)), (0, 0))
    ziel.paste(breit.crop((120 * s, 0, 240 * s, 60 * s)), (0, 60 * s))
    return ziel, breit


# ---- Panorama: Ruinenstadt im Nebel bei Dämmerung --------------------------------------------------

def himmel(b, h, rng):
    y = np.linspace(0, 1, h)
    oben = np.array([12, 15, 16], dtype=np.float32)
    mitte = np.array([52, 50, 46], dtype=np.float32)
    glut = np.array([150, 92, 58], dtype=np.float32)
    t = np.clip(y * 1.9, 0, 1)[:, None] ** 1.4
    c = oben * (1 - t) + mitte * t
    g = np.exp(-((y - 0.54) / 0.08) ** 2)[:, None] * 0.8
    c = c * (1 - g) + glut * g
    bild = np.repeat(c[:, None, :], b, axis=1)
    wolken = rauschen(b, h, 220, rng) * 0.6 + rauschen(b, h, 70, rng) * 0.4
    bild *= (0.8 + 0.35 * wolken[..., None])
    return bild


def gebaeude(z, x, boden, bw, bh, r, licht):
    """Ein zerfallenes Hochhaus: gezackte Kante, Fensterraster, ein paar Löcher. Malt in eine L-Maske (255 = Wand)."""
    top = boden - bh
    punkte = [(x, boden + 600), (x, top + r.randint(0, 10))]
    xs = x
    while xs < x + bw:
        xs += r.randint(5, 16)
        einbruch = r.random() < 0.3
        punkte.append((min(xs, x + bw), top + (r.randint(8, 40) if einbruch else r.randint(0, 6))))
    punkte.append((x + bw, boden + 600))
    z.polygon(punkte, fill=255)
    # Fenster: dunkle Löcher (Maskenwert 120 = Fenster)
    fw, fh = max(3, bw // 10), 6
    for fy in range(top + 14, boden - 6, fh * 2):
        for fx in range(x + 4, x + bw - fw - 2, fw * 2):
            if r.random() < 0.85:
                z.rectangle((fx, fy, fx + fw - 1, fy + fh - 1), fill=150 if r.random() < licht else 90)
    if r.random() < 0.3:  # Antenne
        ax = x + r.randint(4, max(5, bw - 4))
        z.line([(ax, top), (ax, top - r.randint(25, 80))], fill=255, width=2)


def skyline(breite, hoehe, horizont, rng, schichten):
    bild = himmel(breite, hoehe, rng)
    r = random.Random(int(rng.integers(0, 1 << 30)))
    for nr, (farbe, minh, maxh, nebel) in enumerate(schichten):
        maske = Image.new("L", (breite, hoehe), 0)
        z = ImageDraw.Draw(maske)
        x = -r.randint(0, 60)
        while x < breite + 60:
            bw = int(r.randint(30, 90) * (1 + nr * 0.4))
            bh = r.randint(minh, maxh)
            gebaeude(z, x, horizont + 40 * nr, bw, bh, r, 0.04 if nr > 0 else 0.0)
            x += bw + r.randint(-8, 26)
        m = np.asarray(maske, dtype=np.float32)
        wand = (m >= 200)[..., None]
        fenster = ((m > 60) & (m < 110))[..., None]
        licht = ((m > 130) & (m < 170))[..., None]
        f = np.array(farbe, dtype=np.float32)
        bild = np.where(wand, f, bild)
        bild = np.where(fenster, f * 0.45, bild)
        bild = np.where(licht, np.array([170, 120, 60], dtype=np.float32), bild)
        # Nebel zwischen den Schichten: unten dichter
        y = np.linspace(0, 1, hoehe)[:, None, None]
        dichte = np.clip((y - (horizont - 260 + 60 * nr) / hoehe) * 2.2, 0, 1) * nebel
        bild = bild * (1 - dichte) + np.array([88, 84, 78]) * dichte
    # Bodennebel
    y = np.linspace(0, 1, hoehe)[:, None, None]
    boden = np.clip((y - (horizont + 80) / hoehe) * 3, 0, 1)
    bild = bild * (1 - boden * 0.6) + np.array([30, 29, 27]) * boden * 0.6
    return np.clip(bild, 0, 255)


def panorama():
    rng = np.random.default_rng(1986)
    n = 1024
    horizont = 600
    streifen = skyline(4 * n, n, horizont, rng, [
        ((70, 66, 62), 180, 360, 0.55),
        ((44, 41, 39), 110, 260, 0.45),
        ((22, 21, 20), 40, 170, 0.25),
    ])
    streifen = np.asarray(Image.fromarray(streifen.astype(np.uint8)).filter(ImageFilter.GaussianBlur(1.0)))
    seiten = [Image.fromarray(streifen[:, i * n:(i + 1) * n]) for i in range(4)]
    oben = himmel(n, n, rng) * 0.35
    oben = Image.fromarray(np.clip(oben, 0, 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(6))
    unten = Image.new("RGB", (n, n), (26, 25, 23))
    return seiten + [oben, unten]


# ---- Panorama 2: Sperrzone bei blutroter Dämmerung ------------------------------------------------

def himmel2(b, h, rng):
    """Blutrote Dämmerung unter schwerer Wolkendecke, oben fahles Grün, ein bleicher Mond im Dunst."""
    y = np.linspace(0, 1, h)[:, None]
    oben = np.array([18, 26, 24], dtype=np.float32)
    mitte = np.array([58, 44, 40], dtype=np.float32)
    glut = np.array([178, 74, 38], dtype=np.float32)
    t = np.clip(y * 1.8, 0, 1) ** 1.3
    c = oben * (1 - t[..., None]) + mitte * t[..., None]
    g = np.exp(-((y - 0.42) / 0.11) ** 2)[..., None]
    c = c * (1 - g * 0.9) + glut * g * 0.9
    bild = np.repeat(c, b, axis=1).astype(np.float32)
    wolken = rauschen(b, h, 260, rng) * 0.55 + rauschen(b, h, 80, rng) * 0.3 + rauschen(b, h, 25, rng) * 0.15
    band = np.clip(1.2 - np.abs(y - 0.33) * 4, 0, 1)
    bild *= (0.72 + 0.45 * wolken[..., None] * band[..., None] + 0.12 * (1 - band[..., None]))
    return bild


def rauchsaeule(bild, x, boden, hoehe, breite, rng, glut=True):
    """Dunkle, sich ausbreitende Rauchsäule; unten glüht es orange."""
    H, B = bild.shape[:2]
    for yy in range(max(0, boden - hoehe), min(H, boden)):
        k = (boden - yy) / hoehe
        w = breite * (0.3 + 1.7 * k)
        drift = int(math.sin(k * 3 + x) * 40 * k) + int(k * k * 160)
        x0, x1 = int(x + drift - w), int(x + drift + w)
        x0, x1 = max(0, x0), min(B, x1)
        if x1 <= x0:
            continue
        xs = np.arange(x0, x1)
        rand = 1 - np.abs((xs - (x + drift)) / max(w, 1))
        dichte = np.clip(rand * 1.6, 0, 1) * (0.95 - 0.5 * k) * (0.8 + 0.4 * rng.random(len(xs)))
        farbe = np.array([28, 25, 24], dtype=np.float32)
        if glut and k < 0.12:
            farbe = np.array([200, 90, 40], dtype=np.float32) * (1 - k / 0.12) + farbe * (k / 0.12)
        bild[yy, x0:x1] = bild[yy, x0:x1] * (1 - dichte[:, None]) + farbe * dichte[:, None]


def silhouette_mensch(z, x, boden, groesse, r, schief=0):
    """Ein Infizierter als Scherenschnitt: schief, hängende Arme."""
    k = groesse / 60.0
    kx = x + schief * k * 6
    z.ellipse((kx - 6 * k, boden - 60 * k, kx + 6 * k, boden - 48 * k), fill=255)
    z.polygon([(kx - 8 * k, boden - 48 * k), (kx + 8 * k, boden - 48 * k), (x + 7 * k, boden - 24 * k), (x - 7 * k, boden - 24 * k)], fill=255)
    z.line([(kx - 8 * k, boden - 46 * k), (kx - 14 * k, boden - 22 * k + r.randint(-4, 4) * k)], fill=255, width=max(1, int(4 * k)))
    z.line([(kx + 8 * k, boden - 46 * k), (kx + 12 * k + r.randint(0, 6) * k, boden - 30 * k)], fill=255, width=max(1, int(4 * k)))
    z.line([(x - 4 * k, boden - 24 * k), (x - 7 * k, boden)], fill=255, width=max(1, int(5 * k)))
    z.line([(x + 4 * k, boden - 24 * k), (x + 6 * k, boden - 2 * k)], fill=255, width=max(1, int(5 * k)))


def auto(z, x, boden, laenge, kopf=False):
    h = laenge * 0.32
    z.polygon([(x, boden - h * 0.15), (x + laenge * 0.05, boden - h * 0.55), (x + laenge * 0.28, boden - h * 0.6),
               (x + laenge * 0.38, boden - h), (x + laenge * 0.72, boden - h), (x + laenge * 0.85, boden - h * 0.6),
               (x + laenge, boden - h * 0.5), (x + laenge, boden - h * 0.12)], fill=255)
    for rx in (0.2, 0.78):
        z.ellipse((x + laenge * rx - h * 0.22, boden - h * 0.3, x + laenge * rx + h * 0.22, boden + h * 0.12), fill=255)


def hubschrauber(z, x, boden, laenge):
    h = laenge * 0.28
    z.polygon([(x, boden - h * 0.2), (x + laenge * 0.1, boden - h), (x + laenge * 0.45, boden - h * 1.05),
               (x + laenge * 0.55, boden - h * 0.4), (x + laenge, boden - h * 0.75), (x + laenge, boden - h * 0.6),
               (x + laenge * 0.55, boden - h * 0.1), (x + laenge * 0.1, boden)], fill=255)
    z.line([(x + laenge * 0.25, boden - h * 1.05), (x - laenge * 0.2, boden - h * 1.6)], fill=255, width=max(2, int(laenge / 60)))
    z.line([(x + laenge * 0.25, boden - h * 1.05), (x + laenge * 0.75, boden - h * 1.3)], fill=255, width=max(2, int(laenge / 60)))
    z.line([(x + laenge * 0.98, boden - h * 0.7), (x + laenge * 1.02, boden - h * 1.2)], fill=255, width=max(2, int(laenge / 80)))


def zaun(z, x0, x1, boden, hoehe, r):
    for x in range(int(x0), int(x1), 46):
        z.line([(x, boden), (x + r.randint(-3, 3), boden - hoehe)], fill=255, width=4)
    for k in range(4):
        y = boden - hoehe * (0.25 + 0.22 * k)
        z.line([(x0, y), (x1, y + r.randint(-4, 4))], fill=255, width=1)
    # Stacheldrahtrolle oben
    for x in range(int(x0), int(x1), 9):
        z.ellipse((x, boden - hoehe - 9, x + 11, boden - hoehe + 2), outline=255, width=1)


def schild(z, x, boden, text):
    z.line([(x + 50, boden), (x + 50, boden - 150)], fill=255, width=6)
    z.rectangle((x - 30, boden - 230, x + 130, boden - 140), fill=200)
    return (x - 30, boden - 230, x + 130, boden - 140, text)


def strommast(z, x, boden, hoehe):
    z.polygon([(x - 14, boden), (x - 3, boden - hoehe), (x + 3, boden - hoehe), (x + 14, boden)], outline=255, width=3)
    for k in (0.75, 0.9):
        z.line([(x - 40, boden - hoehe * k), (x + 40, boden - hoehe * k)], fill=255, width=3)


def panorama2():
    rng = np.random.default_rng(4242)
    r = random.Random(4242)
    n = 1024
    B, H = 4 * n, n
    horizont = 610
    bild = himmel2(B, H, rng)
    # Mond im Dunst
    yy, xx = np.mgrid[0:H, 0:B]
    for mx, my, rad in ((int(n * 1.72), 150, 52),):
        d = np.sqrt((xx - mx) ** 2 + (yy - my) ** 2)
        hof = np.clip(1 - d / (rad * 4), 0, 1) ** 2 * 0.35
        scheibe = (d < rad).astype(np.float32) * 0.85
        bild = bild * (1 - (hof + scheibe)[..., None]) + np.array([214, 206, 188], dtype=np.float32) * (hof + scheibe)[..., None]
    # Ruinenstadt in drei Schichten (wie bisher, aber mit Brandherden)
    for nr, (farbe, minh, maxh, nebel) in enumerate([((66, 54, 50), 200, 380, 0.55), ((40, 34, 32), 120, 280, 0.45),
                                                     ((20, 18, 17), 50, 170, 0.25)]):
        maske = Image.new("L", (B, H), 0)
        z = ImageDraw.Draw(maske)
        x = -r.randint(0, 60)
        while x < B + 60:
            bw = int(r.randint(30, 90) * (1 + nr * 0.4))
            bh = r.randint(minh, maxh)
            gebaeude(z, x, horizont + 30 * nr, bw, bh, r, 0.06 if nr > 0 else 0.0)
            x += bw + r.randint(-8, 26)
        m = np.asarray(maske, dtype=np.float32)
        f = np.array(farbe, dtype=np.float32)
        bild = np.where((m >= 200)[..., None], f, bild)
        bild = np.where(((m > 60) & (m < 110))[..., None], f * 0.45, bild)
        bild = np.where(((m > 130) & (m < 170))[..., None], np.array([210, 110, 40], dtype=np.float32), bild)
        dichte = np.clip((np.linspace(0, 1, H)[:, None, None] - (horizont - 260 + 60 * nr) / H) * 2.2, 0, 1) * nebel
        bild = bild * (1 - dichte) + np.array([96, 70, 60]) * dichte
    # Rauchsäulen über der Stadt
    for x in (int(n * 0.35), int(n * 1.15), int(n * 2.4), int(n * 3.3)):
        rauchsaeule(bild, x, horizont - 120, r.randint(420, 520), r.randint(26, 40), rng)
    # Vordergrund (Scherenschnitte): Straße mit Wracks, Hubschrauber, Infizierte, Zaun, Strommasten
    vg = Image.new("L", (B, H), 0)
    z = ImageDraw.Draw(vg)
    boden = horizont + 150
    z.rectangle((0, boden, B, H), fill=255)
    z.polygon([(0, boden), (B, boden), (B, boden - 6), (0, boden - 10)], fill=255)
    # Gesicht 0: Straßensperre mit Autos und Warnschild
    for i, x in enumerate((20, 330, 690)):
        auto(z, x, boden + 6, 280 + i * 20)
    tafel = schild(z, 560, boden, "SPERRZONE")
    # Gesicht 1: abgestürzter Hubschrauber mit Rauch
    hubschrauber(z, n + 280, boden + 8, 520)
    # Gesicht 2: Strommasten, tote Bäume und eine Herde Infizierter
    for x in (2 * n + 80, 2 * n + 520, 2 * n + 940):
        strommast(z, x, boden, 330)
    for i in range(11):
        silhouette_mensch(z, 2 * n + 120 + i * 78 + r.randint(-18, 18), boden + r.randint(-2, 10), r.randint(90, 130), r,
                          schief=r.choice([-1, 0, 1]))
    # Gesicht 3: Zaun mit Stacheldraht und Wachturm
    zaun(z, 3 * n, 4 * n, boden, 120, r)
    tx = 3 * n + 600
    z.polygon([(tx - 50, boden), (tx - 22, boden - 280), (tx + 22, boden - 280), (tx + 50, boden)], outline=255, width=6)
    z.rectangle((tx - 46, boden - 330, tx + 46, boden - 280), fill=255)
    z.polygon([(tx - 56, boden - 330), (tx, boden - 360), (tx + 56, boden - 330)], fill=255)
    # ein paar Gestalten an der Straßensperre
    for i in range(3):
        silhouette_mensch(z, 250 + i * 160 + r.randint(-20, 20), boden + 4, r.randint(100, 125), r, schief=r.choice([-1, 1]))
    m = np.asarray(vg, dtype=np.float32)
    dunkel = np.array([10, 9, 9], dtype=np.float32)
    bild = np.where((m >= 250)[..., None], dunkel, bild)
    bild = np.where(((m > 150) & (m < 250))[..., None], np.array([150, 120, 30], dtype=np.float32), bild)  # Warntafel
    # Rauch aus dem Hubschrauber
    rauchsaeule(bild, n + 470, boden - 60, 520, 22, rng, glut=True)
    # Bodennebel und Glutschimmer am Horizont
    y = np.linspace(0, 1, H)[:, None, None]
    nebelband = np.exp(-((y - (horizont + 40) / H) / 0.05) ** 2) * 0.45
    bild = bild * (1 - nebelband) + np.array([120, 88, 70]) * nebelband
    img = Image.fromarray(np.clip(bild, 0, 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(0.8))
    # Text auf der Warntafel
    d = ImageDraw.Draw(img)
    try:
        font = ImageFont.truetype("DejaVuSans-Bold.ttf", 22)
    except OSError:
        font = ImageFont.load_default()
    x0, y0, x1, y1, text = tafel
    d.text((x0 + 10, y0 + 8), text, fill=(30, 20, 10), font=font)
    d.text((x0 + 10, y0 + 36), "BETRETEN", fill=(30, 20, 10), font=font)
    d.text((x0 + 10, y0 + 60), "VERBOTEN", fill=(30, 20, 10), font=font)
    seiten = [img.crop((i * n, 0, (i + 1) * n, n)) for i in range(4)]
    oben = Image.fromarray(np.clip(himmel2(n, n, rng)[:200].mean(axis=0, keepdims=True).repeat(n, 0) * 0.6, 0, 255).astype(np.uint8))
    oben = oben.filter(ImageFilter.GaussianBlur(8))
    unten = Image.new("RGB", (n, n), (10, 9, 9))
    return seiten + [oben, unten]


SPRUECHE = [
    "Bleib nicht allein!", "Der Nebel hört zu.", "Antworte nicht, wenn er deinen Namen ruft.",
    "Station Null wartet.", "Hast du Bolzen dabei?", "Emission in 90 Sekunden!", "Kein Signal.",
    "Licht ist Leben.", "Sie kommen nachts.", "Wer schleicht, lebt länger.", "Verbände sind wertvoller als Gold.",
    "Die Zuflucht nimmt noch Leute auf.", "Militärpatronen gegen Konserven.", "Halte Abstand zum Juggernaut.",
    "Vertrau keinem Überlebenden.", "Es hustet. Lauf.", "Projekt AURA – klare Luft für alle!",
    "Die Halden-Werke danken für Ihr Vertrauen.", "Rauch am Horizont: ein Abwurf!", "Nicht in den Nebel gehen.",
    "Der Kern atmet.", "Zähl deine Schuss.", "Schließ die Tür hinter dir.", "Heute schon gefroren?",
    "Funkstille ist kein gutes Zeichen.", "Ruhe vor dem Sturm.", "Die Stadt schläft nie.", "Nur noch ein Streifzug ...",
    "Willkommen in Sektor 4.", "Atme flach.",
]


def speichern(bild, pfad):
    os.makedirs(os.path.dirname(pfad), exist_ok=True)
    bild.save(pfad)


def main():
    t = os.path.join(ZIEL, "textures", "gui", "title")
    lg = logo()
    speichern(lg, os.path.join(t, "minecraft.png"))
    ed = untertitel()
    speichern(ed, os.path.join(t, "edition.png"))
    ll, breit = ladelogo()
    speichern(ll, os.path.join(t, "mojangstudios.png"))
    pano = panorama2()
    # Das Panorama selbst malt jetzt werkzeuge/monster/titelszene.py („Das Tor zur Zone“, mit echten Monstern)
    import subprocess
    subprocess.run(["python3", "titelszene.py"], cwd=os.path.join(WURZEL, "werkzeuge", "monster"), check=True)
    os.makedirs(os.path.join(ZIEL, "texts"), exist_ok=True)
    with open(os.path.join(ZIEL, "texts", "splashes.txt"), "w", encoding="utf-8") as f:
        f.write("\n".join(SPRUECHE) + "\n")

    # Vorschau: so ungefähr sieht der Titelbildschirm aus (GUI-Maßstab 2)
    os.makedirs(VORSCHAU, exist_ok=True)
    v = pano[0].resize((1280, 1280)).crop((0, 280, 1280, 1000)).filter(ImageFilter.GaussianBlur(2))
    v = Image.blend(v, Image.new("RGB", v.size, (0, 0, 0)), 0.25).convert("RGBA")
    logo_bild = lg.crop((0, 0, lg.width, lg.height * 44 // 64)).resize((512 * 1, 88 * 1), Image.LANCZOS)
    lx = (1280 - 512) // 2
    v.alpha_composite(logo_bild, (lx, 60))
    ed_bild = ed.crop((0, 0, ed.width * 98 // 128, ed.height * 14 // 16)).resize((196, 28), Image.LANCZOS)
    v.alpha_composite(ed_bild, (lx + 176, 60 + 74))
    z = ImageDraw.Draw(v)
    schrift = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf", 22)
    for i, txt in enumerate(["Einzelspieler", "Mehrspieler", "Mods"]):
        y = 260 + i * 60
        z.rectangle((440, y, 840, y + 44), fill=(110, 110, 110, 255), outline=(0, 0, 0, 255), width=3)
        z.text((640, y + 22), txt, fill=(240, 240, 240, 255), font=schrift, anchor="mm")
    gelb = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf", 20)
    spruch = Image.new("RGBA", (420, 40), (0, 0, 0, 0))
    ImageDraw.Draw(spruch).text((0, 5), "Der Nebel hört zu.", fill=(255, 255, 0, 255), font=gelb)
    spruch = spruch.rotate(20, expand=True)
    v.alpha_composite(spruch, (780, 70))
    v.convert("RGB").save(os.path.join(VORSCHAU, "titelbildschirm.png"))
    lade = Image.new("RGB", (960, 360), (239, 50, 61))
    b2 = breit.resize((720, 180), Image.LANCZOS)
    lade.paste(b2, (120, 90), b2)
    lade.save(os.path.join(VORSCHAU, "ladebildschirm.png"))
    print("Titelbildschirm geschrieben.")


if __name__ == "__main__":
    main()
