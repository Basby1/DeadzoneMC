"""Entwürfe für den Nether als „Herz der Zone“ – Herzkammer (Arena des Bosses), Messstation Glut-3 (Forschungsruine)
und die Adern, die durch die Landschaft zum Herzen führen.  python3 werkzeuge/nether/entwurf.py <ausgabeordner>
"""
import math
import os
import random
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from voxel import Bau, vorschau  # noqa: E402


def gelaende(b, r, rad, rnd, hoehe=0):
    for x in range(-rad, rad + 1):
        for z in range(-rad, rad + 1):
            if x * x + z * z > rad * rad:
                continue
            h = hoehe + int(1.2 * math.sin(x * 0.21) + 1.0 * math.cos(z * 0.17) + rnd.random() * 0.6)
            for y in range(h - 2, h + 1):
                b.setze(x, y, z, "netherrack" if rnd.random() > 0.08 else "soul_soil")


def spieler(b, x, y, z):
    b.setze(x, y, z, "spieler")
    b.setze(x, y + 1, z, "spieler")


# ---- Herzkammer ---------------------------------------------------------------------------------

def herzkammer(mit_kuppel=True, schnitt=False):
    rnd = random.Random(3)
    b = Bau()
    R = 22
    gelaende(b, rnd, 34, rnd, -1)
    # Boden: Schwarzstein, rissig; Ring aus Lava als Graben, vier Brücken
    for x in range(-R, R + 1):
        for z in range(-R, R + 1):
            d = math.hypot(x, z)
            if d > R:
                continue
            w = math.atan2(z, x)
            bruecke = min(abs(math.sin(w * 2)), 1) < 0.16
            if 16.5 < d < 18.5 and not bruecke:
                b.setze(x, 0, z, "lava")
                b.setze(x, -1, z, "lava")
            else:
                b.setze(x, 0, z, rnd.choice(["polished_blackstone", "polished_blackstone", "blackstone",
                                             "cracked_polished_blackstone_bricks"]))
            for y in range(1, 3):
                b.setze(x, y, z, None)
    # Adern im Boden: acht Strahlen vom Herzen nach außen, Glutknoten darauf
    for i in range(8):
        w = i * math.pi / 4 + 0.2
        for k in range(6, R):
            x, z = math.cos(w) * k, math.sin(w) * k + math.sin(k * 0.5) * 0.8
            if 16.5 < k < 18.5:
                continue
            b.setze(x, 0, z, "glutader" if k % 5 == 0 else "magma_block")
    # Herzbett: gestufter Sockel aus Fleisch, oben das Herz (Boss) und Arterien, die in die Kuppel wachsen
    b.kugel(0, -1, 0, 7, "nether_wart_block", nur=lambda x, y, z: y >= 0)
    b.kugel(0, 0, 0, 5, "crimson_nylium", nur=lambda x, y, z: y >= 1)
    b.kugel(0, 6, 0, 3.4, "fleisch")
    b.kugel(0, 6, 0, 3.6, "ader", hohl=True, nur=lambda x, y, z: (x + y + z) % 3 == 0)
    for k in range(6):
        w = k * math.pi / 3
        b.linie((math.cos(w) * 2.5, 6, math.sin(w) * 2.5), (math.cos(w) * 7, 1, math.sin(w) * 7), "crimson_hyphae", 0.6)
        if mit_kuppel and not schnitt:
            b.linie((math.cos(w) * 2, 8, math.sin(w) * 2), (math.cos(w + 0.3) * 5, 25, math.sin(w + 0.3) * 5), "crimson_stem", 0.5)
    # Rippenkuppel: zwölf Knochenrippen, zwischen manchen Fleischhäute mit Rissen
    if mit_kuppel:
        H = 27
        rippen = 12
        for i in range(rippen):
            w = i * 2 * math.pi / rippen
            for s in range(0, 101):
                t = s / 100
                r = R * math.cos(t * math.pi / 2) + 3 * t
                y = H * math.sin(t * math.pi / 2)
                if r < 3.5:
                    break
                if schnitt and math.sin(w) > 0.3:
                    continue
                b.kugel(math.cos(w) * r, y, math.sin(w) * r, 0.7, "bone_block")
        for i in range(rippen):
            if i % 3 == 1 or (schnitt and math.sin(i * 2 * math.pi / rippen) > -0.6):
                continue
            w0 = i * 2 * math.pi / rippen
            for s in range(2, 92, 2):
                t = s / 100
                r = R * math.cos(t * math.pi / 2) + 3 * t - 0.3
                y = H * math.sin(t * math.pi / 2)
                for k in range(1, 12):
                    w = w0 + k * (2 * math.pi / rippen) / 12
                    if rnd.random() < 0.18:
                        continue  # Risse
                    b.setze(math.cos(w) * r, y, math.sin(w) * r, "nether_wart_block" if rnd.random() > 0.035 else "shroomlight")
        # Ring aus Wirbeln oben, darin das Auge (offen)
        for k in range(24):
            w = k * 2 * math.pi / 24
            b.setze(math.cos(w) * 4, H, math.sin(w) * 4, "bone_block")
        # Hängende Ranken
        for _ in range(40):
            w = rnd.uniform(0, 2 * math.pi)
            r = rnd.uniform(8, 18)
            top = H * math.sin(math.acos(min(1, r / R)))
            for y in range(int(top) - rnd.randint(3, 9), int(top)):
                b.setze(math.cos(w) * r, y, math.sin(w) * r, "weeping_vines")
    # Tor der Wacht im Süden: Rahmen aus Schwarzsteinziegeln, Fallgitter halb oben, Warnstreifen, Laternen
    for x in range(-4, 5):
        for y in range(1, 10):
            for z in (R, R + 1, R + 2):
                rand = abs(x) == 4 or y == 9
                if rand:
                    b.setze(x, y, z, "polished_blackstone_bricks")
                elif y >= 6 and z == R + 1:
                    b.setze(x, y, z, "iron_bars")
    for x in range(-4, 5):
        b.setze(x, 10, R + 1, "yellow_terracotta" if x % 2 == 0 else "black_concrete")
    b.setze(-5, 4, R + 2, "soul_lantern")
    b.setze(5, 4, R + 2, "soul_lantern")
    for z in range(R, R + 12):
        for x in range(-2, 3):
            b.setze(x, 0, z, "polished_blackstone_bricks")
    spieler(b, 1, 1, R + 5)
    return b


# ---- Messstation Glut-3 ---------------------------------------------------------------------------

def messstation(schnitt=False):
    rnd = random.Random(5)
    b = Bau()
    gelaende(b, rnd, 24, rnd, 0)
    X1, X2, Z1, Z2 = -10, 10, -7, 7
    for x in range(X1 - 1, X2 + 2):
        for z in range(Z1 - 1, Z2 + 2):
            b.setze(x, 0, z, "smooth_basalt" if x in (X1 - 1, X2 + 1) or z in (Z1 - 1, Z2 + 1) else "polished_basalt")
            for y in range(1, 14):
                b.setze(x, y, z, None)
    # vorn links ist die Station eingestürzt (eine Explosion im Labor)
    eingestuerzt = lambda x, y, z: x < -3 and z > 0 and y > 1 + (-x - 3) * 0.0 + max(0, 7 - (-x - 3) - (z - 0)) * 0.9
    for x in range(X1, X2 + 1):
        for z in range(Z1, Z2 + 1):
            for y in range(1, 7):
                wand = x in (X1, X2) or z in (Z1, Z2)
                if not wand or eingestuerzt(x, y, z):
                    continue
                tuer = z == Z2 and 0 <= x <= 2 and y <= 3
                if tuer:
                    continue
                fenster = y in (3, 4) and ((x % 3 == 0 and z in (Z1, Z2)) or (z % 3 == 0 and x in (X1, X2)))
                if fenster:
                    s = "lantern" if (x + z) % 2 == 0 else "tinted_glass"
                elif y == 1:
                    s = "basalt"
                elif y == 6:
                    s = "yellow_terracotta" if (x + z) % 2 == 0 else "black_concrete"  # Warnband unter dem Dach
                else:
                    s = rnd.choice(["polished_blackstone_bricks"] * 4 + ["cracked_polished_blackstone_bricks"])
                b.setze(x, y, z, s)
            if not schnitt and not eingestuerzt(x, 7, z):
                b.setze(x, 7, z, "polished_blackstone")
    # Obergeschoss hinten rechts: Kontrollraum mit Fensterband
    for x in range(1, X2 + 1):
        for z in range(Z1, 1):
            for y in range(8, 12):
                if x in (1, X2) or z in (Z1, 0):
                    b.setze(x, y, z, "lantern" if y == 10 and (x + z) % 3 == 0 else ("tinted_glass" if y == 10 else "polished_blackstone_bricks"))
            if not schnitt:
                b.setze(x, 12, z, "polished_blackstone")
    if not schnitt:
        # Dachtechnik: Lüfter, Rohre, Schüssel
        for x in (-7, -4):
            b.quader(x, 8, -5, x + 1, 8, -4, "iron_block")
        for x in range(-8, 1):
            b.setze(x, 8, -2, "iron_bars")
        b.kugel(6, 13, -3, 2.4, "white_concrete", nur=lambda x, y, z: y >= 0 and x + y <= 1)
        b.setze(6, 13, -3, "iron_block")
    # Türrahmen mit Warnstreifen
    for y in range(1, 5):
        b.setze(-1, y, Z2 + 1, "yellow_terracotta" if y % 2 else "black_concrete")
        b.setze(3, y, Z2 + 1, "black_concrete" if y % 2 else "yellow_terracotta")
    for x in range(-1, 4):
        b.setze(x, 4, Z2 + 1, "iron_block")
    b.setze(1, 5, Z2 + 1, "redstone_lamp")
    # Innen: Labortische, Brauständer, Kessel, Glastanks mit Präparat, Pult mit Logbuch, Kisten, Lampen
    for x in range(-1, 9, 3):
        b.setze(x, 1, -4, "white_concrete")
        b.setze(x, 2, -4, "brewing_stand")
    for x in (2, 5):
        b.setze(x, 1, 3, "cauldron")
    for x in (6, 7, 8, 9):
        b.setze(x, 1, -6, "glass")
        b.setze(x, 2, -6, "glass")
        b.setze(x, 3, -6, "iron_block")
    b.setze(7, 1, -6, "skull")
    b.setze(-1, 1, 1, "lectern")
    b.setze(9, 1, 6, "chest")
    b.setze(-9, 1, -6, "barrel")
    for x in (-6, 0, 6):
        b.setze(x, 6, 0, "lantern")
    # Schutt der eingestürzten Ecke vor dem Gebäude
    for _ in range(55):
        x = rnd.randint(-15, -3)
        z = rnd.randint(1, 12)
        y = 1 if rnd.random() > 0.3 else 2
        b.setze(x, y, z, rnd.choice(["polished_blackstone_bricks", "blackstone", "cracked_polished_blackstone_bricks", "basalt"]))
    # Bohrturm: Die Station hat eine Ader angebohrt – Gerüst, Rohr, darunter glüht es
    for y in range(1, 15):
        for (dx, dz) in ((0, 0), (2, 0), (0, 2), (2, 2)):
            if y % 4 == 0 or (dx, dz) in ((0, 0), (2, 2)) or y < 12:
                b.setze(13 + dx, y, 4 + dz, "iron_bars")
    for y in range(-2, 15):
        b.setze(14, y, 5, "chain" if y > 0 else "glutader")
    b.setze(14, 15, 5, "iron_block")
    for k in range(9):
        b.setze(14 + k, 0, 5 + k // 2, "glutader")
        b.setze(14 + k, 0, 6 + k // 2, "nether_wart_block")
    for x in range(10, 14):
        b.setze(x + 1, 3, 5, "iron_bars")  # Rohr vom Bohrturm in die Station
    # Antennenmast mit Querstreben und rotem Warnlicht
    for y in range(13 if not schnitt else 8, 27):
        b.setze(8, y, -5, "iron_bars")
    for y in (18, 22):
        for k in range(-2, 3):
            b.setze(8 + k, y, -5, "iron_bars")
    b.setze(8, 27, -5, "redstone_lamp")
    b.setze(8, 28, -5, "lightning_rod")
    # Der Nether holt sich die Station: Fleisch an der Westwand, Lichtknollen, Ranken vom Dach
    for _ in range(9):
        b.kugel(rnd.randint(-13, -9), rnd.randint(1, 6), rnd.randint(-7, 0), rnd.uniform(1.2, 2.2), "nether_wart_block")
    for _ in range(4):
        b.setze(rnd.randint(-13, -10), rnd.randint(2, 6), rnd.randint(-6, 0), "shroomlight")
    for _ in range(14):
        x = rnd.choice([X1 - 1, X2 + 1, rnd.randint(X1, X2)])
        z = rnd.choice([Z1 - 1, Z2 + 1]) if x not in (X1 - 1, X2 + 1) else rnd.randint(Z1, Z2)
        for y in range(7 - rnd.randint(1, 4), 7):
            b.setze(x, y, z, "weeping_vines")
    spieler(b, 1, 1, Z2 + 4)
    return b


# ---- Adern und Pulsknoten ----------------------------------------------------------------------------

def adern():
    rnd = random.Random(9)
    b = Bau()
    R = 30
    for x in range(-R, R + 1):
        for z in range(-R, R + 1):
            h = int(2.2 * math.sin(x * 0.13) + 1.8 * math.cos(z * 0.11) + 1.2 * math.sin((x + z) * 0.07))
            lava = x > 14 and z > 12
            for y in range(h - 3, h + 1):
                b.setze(x, y, z, "lava" if lava and y == h else "netherrack")
            if lava:
                b.setze(x, h, z, "lava")

    def boden(x, z):
        for y in range(8, -8, -1):
            if b.hole(round(x), y, round(z)) not in (None, "lava"):
                return y
        return 0

    # Pulsknoten in der Mitte: eine pochende Knolle aus Fleisch mit leuchtendem Kern
    b.kugel(0, boden(0, 0) + 2, 0, 4.2, "nether_wart_block", nur=lambda x, y, z: rnd.random() > 0.12)
    b.kugel(0, boden(0, 0) + 2, 0, 2.2, "shroomlight")
    # Adern: schlängeln sich aus allen Richtungen heran, Glutlinie in der Mitte, alle paar Blöcke ein Knoten
    for i, w in enumerate([0.4, 1.9, 3.1, 4.4, 5.5]):
        for k in range(4, 30):
            x = math.cos(w) * k + math.sin(k * 0.35 + i) * 2.0
            z = math.sin(w) * k + math.cos(k * 0.3 + i) * 2.0
            y = boden(x, z)
            for dx in (-1, 0, 1):
                b.setze(x + dx * math.sin(w), y + 1, z - dx * math.cos(w), "nether_wart_block" if dx else "glutader")
            if k % 7 == 0:
                b.kugel(x, y + 2, z, 1.4, "nether_wart_block")
                b.setze(x, y + 3, z, "shroomlight")
    # Kokons: Fleischhüllen auf Stielen, in einem steckt noch etwas
    for _ in range(6):
        x, z = rnd.randint(-24, 10), rnd.randint(-24, 10)
        y = boden(x, z)
        for k in range(1, 4):
            b.setze(x, y + k, z, "crimson_stem")
        b.kugel(x, y + 5, z, 1.6, "fleisch_dunkel")
    spieler(b, 6, boden(6, 6) + 1, 6)
    return b


if __name__ == "__main__":
    aus = sys.argv[1] if len(sys.argv) > 1 else "."
    os.makedirs(aus, exist_ok=True)
    vorschau(herzkammer(), os.path.join(aus, "1_herzkammer.jpg"), titel="Herzkammer – Arena des Herzens (Entwurf)", nebel=0.25)
    vorschau(herzkammer(schnitt=True), os.path.join(aus, "2_herzkammer_schnitt.jpg"), drehung=1,
             titel="Herzkammer – Schnitt: Herzbett, Lavagraben, vier Brücken, Tor der Wacht", nebel=0.15)
    vorschau(messstation(), os.path.join(aus, "3_messstation.jpg"), titel="Messstation Glut-3 – Forschungsruine (Entwurf)", nebel=0.2)
    vorschau(messstation(schnitt=True), os.path.join(aus, "4_messstation_innen.jpg"), drehung=0,
             titel="Messstation Glut-3 – ohne Dach: Labor, Glastanks, Pult mit Logbuch", nebel=0.1)
    vorschau(adern(), os.path.join(aus, "5_adern.jpg"), titel="Adern und Pulsknoten – führen durch den Nether zum Herzen", nebel=0.25)
    print("fertig")
