"""Kleiner Voxel-Baukasten mit isometrischer Vorschau – für Entwürfe von Bauwerken, bevor sie ins Spiel kommen.

Ein Bauwerk ist ein dict {(x, y, z): blockname}. vorschau() zeichnet es von schräg oben (drei sichtbare Seiten,
schattiert, mit etwas Körnung je Block, damit Flächen nicht flach wirken).
"""
import math
import random

from PIL import Image, ImageDraw, ImageFilter

FARBEN = {
    "blackstone": (42, 35, 41), "polished_blackstone": (54, 48, 58), "polished_blackstone_bricks": (48, 42, 50),
    "cracked_polished_blackstone_bricks": (44, 38, 45), "gilded_blackstone": (70, 52, 30), "basalt": (75, 74, 80),
    "polished_basalt": (95, 93, 100), "netherrack": (110, 52, 52), "nether_bricks": (48, 24, 28),
    "red_nether_bricks": (70, 8, 10), "nether_wart_block": (118, 12, 12), "crimson_nylium": (133, 31, 31),
    "crimson_hyphae": (92, 24, 48), "crimson_stem": (110, 36, 60), "shroomlight": (241, 154, 75),
    "magma_block": (142, 61, 18), "bone_block": (226, 220, 196), "lava": (232, 104, 28), "iron_bars": (138, 138, 138),
    "chain": (61, 67, 81), "crying_obsidian": (40, 10, 70), "obsidian": (20, 16, 32), "soul_lantern": (90, 213, 216),
    "lantern": (230, 170, 80), "tinted_glass": (44, 38, 51), "glass": (180, 210, 220), "yellow_terracotta": (185, 133, 58),
    "black_concrete": (12, 13, 18), "iron_block": (216, 216, 216), "weeping_vines": (143, 27, 27),
    "soul_sand": (82, 62, 51), "soul_soil": (75, 57, 46), "smooth_basalt": (60, 58, 64), "chest": (160, 110, 40),
    "barrel": (130, 92, 50), "lectern": (160, 120, 70), "brewing_stand": (120, 110, 90), "cauldron": (70, 70, 74),
    "iron_trapdoor": (190, 190, 190), "lightning_rod": (190, 110, 70), "redstone_lamp": (180, 90, 50),
    "white_concrete": (205, 210, 212), "red_concrete": (142, 32, 32), "spieler": (60, 90, 150), "skull": (210, 210, 200),
    "ancient_debris": (100, 70, 60), "warped_wart_block": (22, 120, 132), "fleisch": (150, 40, 45),
    "fleisch_dunkel": (100, 20, 28), "sehne": (196, 150, 140), "ader": (200, 40, 30), "glutader": (255, 120, 40),
}
LEUCHTET = {"shroomlight", "lava", "magma_block", "soul_lantern", "lantern", "glutader", "crying_obsidian"}


class Bau:
    def __init__(self):
        self.b = {}

    def setze(self, x, y, z, name):
        x, y, z = int(round(x)), int(round(y)), int(round(z))
        if name is None:
            self.b.pop((x, y, z), None)
        else:
            self.b[(x, y, z)] = name

    def hole(self, x, y, z):
        return self.b.get((int(x), int(y), int(z)))

    def quader(self, x1, y1, z1, x2, y2, z2, name):
        for x in range(min(x1, x2), max(x1, x2) + 1):
            for y in range(min(y1, y2), max(y1, y2) + 1):
                for z in range(min(z1, z2), max(z1, z2) + 1):
                    self.setze(x, y, z, name)

    def kugel(self, cx, cy, cz, r, name, hohl=False, nur=None):
        ri = int(math.ceil(r))
        for x in range(-ri, ri + 1):
            for y in range(-ri, ri + 1):
                for z in range(-ri, ri + 1):
                    d = math.sqrt(x * x + y * y + z * z)
                    if d <= r and (not hohl or d > r - 1.2) and (nur is None or nur(x, y, z)):
                        self.setze(cx + x, cy + y, cz + z, name)

    def linie(self, p, q, name, dicke=0):
        n = int(max(abs(q[0] - p[0]), abs(q[1] - p[1]), abs(q[2] - p[2]))) * 2 + 1
        for i in range(n + 1):
            t = i / n
            x, y, z = (p[k] + (q[k] - p[k]) * t for k in range(3))
            if dicke:
                self.kugel(x, y, z, dicke, name)
            else:
                self.setze(x, y, z, name)


def vorschau(bau, pfad, groesse=16, drehung=0, hintergrund=(38, 10, 12), titel=None, nebel=0.0, breite=1600, hoehe=1000):
    """Isometrische Ansicht. drehung 0..3 dreht um 90°."""
    def dreh(x, z):
        for _ in range(drehung % 4):
            x, z = -z, x
        return x, z

    bloecke = {}
    for (x, y, z), n in bau.b.items():
        dx, dz = dreh(x, z)
        bloecke[(dx, y, dz)] = n
    if not bloecke:
        return
    s = groesse
    w = s * math.cos(math.radians(30))
    h = s * 0.5

    def proj(x, y, z):
        return ((x - z) * w, (x + z) * h - y * s)

    xs = [proj(*k) for k in bloecke]
    minx = min(p[0] for p in xs) - w * 2
    maxx = max(p[0] for p in xs) + w * 2
    miny = min(p[1] for p in xs) - s * 2
    maxy = max(p[1] for p in xs) + s * 2
    skala = min(breite / (maxx - minx), hoehe / (maxy - miny))
    bild = Image.new("RGB", (breite, hoehe), hintergrund)
    # Hintergrund: leichter Verlauf nach unten heller (Glut)
    zeichner = ImageDraw.Draw(bild)
    for yy in range(hoehe):
        f = yy / hoehe
        c = tuple(int(hintergrund[k] * (0.6 + 0.8 * f)) for k in range(3))
        zeichner.line([(0, yy), (breite, yy)], fill=c)
    ox = (breite - (maxx - minx) * skala) / 2 - minx * skala
    oy = (hoehe - (maxy - miny) * skala) / 2 - miny * skala

    def P(x, y, z):
        a, b = proj(x, y, z)
        return (ox + a * skala, oy + b * skala)

    reihenfolge = sorted(bloecke.items(), key=lambda kv: (kv[0][0] + kv[0][2], kv[0][1], kv[0][0]))
    tiefe_max = max(k[0] + k[2] for k in bloecke)
    tiefe_min = min(k[0] + k[2] for k in bloecke)
    rnd = random.Random(7)
    for (x, y, z), n in reihenfolge:
        oben = (x, y + 1, z) in bloecke
        rechts = (x + 1, y, z) in bloecke
        vorn = (x, y, z + 1) in bloecke
        if oben and rechts and vorn:
            continue
        basis = FARBEN.get(n, (255, 0, 255))
        koern = rnd.uniform(0.88, 1.12)
        dunst = 0.0
        if nebel:
            dunst = nebel * (1 - (x + z - tiefe_min) / max(1, tiefe_max - tiefe_min))
        leuchtet = n in LEUCHTET

        def farbe(faktor):
            if leuchtet:
                faktor = max(faktor, 0.95)
            c = [min(255, int(basis[k] * faktor * koern)) for k in range(3)]
            return tuple(int(c[k] * (1 - dunst) + hintergrund[k] * 1.4 * dunst) for k in range(3))

        if not oben:
            zeichner.polygon([P(x, y + 1, z), P(x + 1, y + 1, z), P(x + 1, y + 1, z + 1), P(x, y + 1, z + 1)], fill=farbe(1.0))
        if not rechts:
            zeichner.polygon([P(x + 1, y + 1, z), P(x + 1, y + 1, z + 1), P(x + 1, y, z + 1), P(x + 1, y, z)], fill=farbe(0.72))
        if not vorn:
            zeichner.polygon([P(x, y + 1, z + 1), P(x + 1, y + 1, z + 1), P(x + 1, y, z + 1), P(x, y, z + 1)], fill=farbe(0.55))
    # Leuchten: weicher Schein um helle Blöcke
    schein = Image.new("RGB", bild.size, (0, 0, 0))
    sz = ImageDraw.Draw(schein)
    for (x, y, z), n in bloecke.items():
        if n in LEUCHTET:
            px, py = P(x + 0.5, y + 0.5, z + 0.5)
            r = s * skala * 1.4
            c = FARBEN[n]
            sz.ellipse([px - r, py - r, px + r, py + r], fill=tuple(int(v * 0.35) for v in c))
    schein = schein.filter(ImageFilter.GaussianBlur(s * skala * 1.2))
    bild = Image.fromarray(__import__("numpy").clip(__import__("numpy").asarray(bild, dtype=int)
                                                     + __import__("numpy").asarray(schein, dtype=int), 0, 255).astype("uint8"))
    if titel:
        d2 = ImageDraw.Draw(bild)
        from PIL import ImageFont
        try:
            schrift = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf", 30)
        except OSError:
            schrift = ImageFont.load_default()
        d2.text((32, 26), titel, fill=(240, 226, 205), font=schrift)
    bild.save(pfad, quality=92)
