#!/usr/bin/env python3
"""Eigene Gebäude für Lost Cities: Polizeiwache, Waffenladen, Apotheke, Krankenhaus, Supermarkt.

Jedes Gebäude ist einen Chunk groß (16×16) und besteht aus Stockwerken zu je 6 Schichten:
Schicht 0 Boden, 1–4 Raum, 5 Decke. Jede Kiste bekommt passende Beute (wie in 7 Days to Die).
Außerdem: Spawner in Stadtgebäuden erzeugen unsere Monster, und die Gebäude kommen in den Stadtstil.

  python3 werkzeuge/stadtgebaeude.py
"""
import copy
import json
import os

WURZEL = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DATEN = os.path.join(WURZEL, "src", "main", "resources", "data")
LC = os.path.join(DATEN, "deadzonemc", "lostcities")
LC_ORIG = os.path.join(DATEN, "lostcities", "lostcities")

# Gemeinsame Palette (Zeichen -> Block). Jedes Gebäude kann einzelne Zeichen überschreiben.
GRUND = {
    " ": "minecraft:air",
    "#": "minecraft:light_gray_concrete",   # Wand
    "f": "minecraft:polished_andesite",     # Boden/Decke
    "w": "minecraft:glass_pane",
    "W": "minecraft:gray_stained_glass_pane",
    "I": "minecraft:iron_bars",
    "c": "minecraft:smooth_stone_slab[type=top]",  # Theke
    "t": "minecraft:spruce_slab[type=top]",        # Tisch
    "s": "minecraft:spruce_stairs[facing=south]",  # Stuhl
    "S": "minecraft:spruce_stairs[facing=north]",
    "L": "minecraft:bookshelf",
    "o": "minecraft:lantern[hanging=true]",
    "x": "minecraft:cobweb",
    "r": "minecraft:red_carpet",
    "g": "minecraft:gray_carpet",
    "l": "minecraft:ladder[facing=north]",
    "p": "minecraft:potted_dead_bush",
    "k": "minecraft:cauldron",
    "e": "minecraft:iron_block",
    "a": "minecraft:anvil[facing=east]",
    "R": "minecraft:redstone_lamp",
    "Q": "minecraft:smooth_quartz",
    "~": "minecraft:structure_void",
}


def leer():
    return [[" "] * 16 for _ in range(16)]


def voll(z="f"):
    return [[z] * 16 for _ in range(16)]


def rahmen(raster, wand="#", fenster=None, tueren=()):
    """Außenwand; fenster: Zeichen für Fenster jedes 3. Feld; tueren: Liste (seite, von, bis)."""
    for i in range(16):
        for (x, z) in ((i, 0), (i, 15), (0, i), (15, i)):
            raster[z][x] = wand
    if fenster:
        for i in range(2, 14):
            if i % 3 != 1:
                for (x, z) in ((i, 0), (i, 15), (0, i), (15, i)):
                    raster[z][x] = fenster
    for seite, von, bis in tueren:
        for i in range(von, bis + 1):
            x, z = {"n": (i, 0), "s": (i, 15), "w": (0, i), "o": (15, i)}[seite]
            raster[z][x] = " "
    return raster


def setze(raster, x, z, c):
    raster[z][x] = c


def linie(raster, x0, z0, x1, z1, c):
    for x in range(min(x0, x1), max(x0, x1) + 1):
        for z in range(min(z0, z1), max(z0, z1) + 1):
            raster[z][x] = c


def leiter(schichten, x=14, z=14):
    """Leiter durch alle Schichten (auch Boden und Decke), damit man in den nächsten Stock kommt."""
    for sl in schichten:
        sl[z][x] = "l"


class Stock:
    """Ein Stockwerk: Boden, vier Raumschichten, Decke."""

    def __init__(self, wand="#", fenster="w", tueren=(), boden="f", decke="f", leiter_pos=(14, 14), mit_leiter=True):
        self.s = [voll(boden)] + [rahmen(leer(), wand, None if i in (0, 3) else fenster, tueren if i < 2 else ())
                                    for i in range(4)] + [voll(decke)]
        # Türen sind 2 hoch
        if mit_leiter and leiter_pos:
            leiter(self.s, *leiter_pos)

    def raum(self, y, x, z, c):
        self.s[1 + y][z][x] = c

    def saeule(self, x, z, c, von=0, bis=3):
        for y in range(von, bis + 1):
            self.s[1 + y][z][x] = c

    def innenwand(self, x0, z0, x1, z1, c="#", luecke=None):
        for y in range(4):
            linie(self.s[1 + y], x0, z0, x1, z1, c)
        if luecke:
            for y in range(2):
                self.s[1 + y][luecke[1]][luecke[0]] = " "

    def slices(self):
        return [["".join(r) for r in sl] for sl in self.s]


def teil(name, stock_oder_schichten, palette_extra):
    slices = stock_oder_schichten.slices() if isinstance(stock_oder_schichten, Stock) else \
        [["".join(r) for r in sl] for sl in stock_oder_schichten]
    benutzt = sorted({c for sl in slices for row in sl for c in row})
    pal = dict(GRUND)
    pal.update(palette_extra)
    eintraege = []
    for c in benutzt:
        v = pal[c]
        if isinstance(v, dict):
            e = {"char": c}
            e.update(v)
        else:
            e = {"char": c, "block": v}
        eintraege.append(e)
    for sl in slices:
        assert len(sl) == 16 and all(len(r) == 16 for r in sl), name
    return name, {"xsize": 16, "zsize": 16, "palette": {"palette": eintraege}, "slices": slices}


def kiste(beute, block="minecraft:chest[facing=south]"):
    return {"block": block, "loot": f"deadzonemc:{beute}"}


def spawner(art):
    return {"block": "minecraft:spawner", "mob": art}


TEILE = {}
GEBAEUDE = {}


def gebaeude(name, stockwerke, dach=None, filler="#", minf=None, maxf=None):
    """stockwerke: Liste (teilname, bedingung) – bedingung z. B. {"ground": True} oder {"floor": 1}."""
    teile = []
    for t, bed in stockwerke:
        e = {"part": f"deadzonemc:{t}"}
        e.update(bed)
        teile.append(e)
    if dach:
        teile.append({"top": True, "part": f"deadzonemc:{dach}"})
    d = {"filler": filler, "rubble": "}" if False else filler, "allowDoors": False, "parts": teile}
    if minf is not None:
        d["minfloors"] = minf
    if maxf is not None:
        d["maxfloors"] = maxf
    GEBAEUDE[name] = d


def registrieren(name, schichten, pal):
    n, d = teil(name, schichten, pal)
    TEILE[n] = d


def flachdach(name, extra=None):
    s = [leer() for _ in range(6)]
    rahmen(s[0], "#")  # Brüstung
    if extra:
        extra(s)
    return s


# ---------------------------------------------------------------------------------------------
# Polizeiwache (2 Stockwerke)
# ---------------------------------------------------------------------------------------------
POLIZEI = {"#": "minecraft:light_gray_concrete", "B": "minecraft:blue_concrete", "f": "minecraft:polished_andesite",
           "C": kiste("loot_polizei"), "A": kiste("loot_waffenkammer"), "K": kiste("loot_buero"),
           "b": "minecraft:white_bed[facing=north,part=foot]", "h": "minecraft:white_bed[facing=north,part=head]",
           "1": spawner("deadzonemc:easymobs"), "4": spawner("deadzonemc:hardmobs")}


def polizei_eg():
    st = Stock(tueren=(("n", 7, 8), ("s", 7, 8)))
    # blauer Streifen außen
    for i in range(16):
        for (x, z) in ((i, 0), (i, 15), (0, i), (15, i)):
            if st.s[3][z][x] == "#":
                st.s[3][z][x] = "B"
    # Theke
    linie(st.s[1], 3, 4, 10, 4, "c")
    st.raum(0, 4, 3, "C")
    st.raum(0, 9, 3, "K")
    st.raum(3, 7, 7, "o")
    # Waffenkammer oben rechts (Gitter)
    st.innenwand(11, 1, 11, 5, "I", luecke=(11, 3))
    st.innenwand(11, 5, 14, 5, "#")
    st.raum(0, 14, 1, "A")
    st.raum(0, 13, 1, "A")
    # Zellen rechts unten
    st.innenwand(10, 7, 14, 7, "#")
    st.innenwand(10, 7, 10, 13, "I", luecke=(10, 9))
    st.innenwand(10, 10, 14, 10, "I")
    st.raum(0, 13, 8, "h")
    st.raum(0, 13, 9, "b")
    st.raum(0, 13, 11, "h")
    st.raum(0, 13, 12, "b")
    st.raum(0, 12, 12, "x")
    # Spinde links
    st.innenwand(4, 8, 4, 14, "#", luecke=(4, 10))
    for z in (9, 11, 13):
        st.raum(0, 1, z, "C")
    st.raum(0, 2, 12, "1")
    st.raum(0, 6, 12, "x")
    return st


def polizei_og():
    st = Stock()
    for x0 in (2, 7):
        for z0 in (3, 9):
            linie(st.s[1], x0, z0, x0 + 2, z0, "t")
            st.raum(0, x0 + 1, z0 + 1, "S")
    st.raum(0, 1, 1, "K")
    st.raum(0, 1, 14, "K")
    st.raum(0, 13, 1, "C")
    for z in range(2, 7):
        st.saeule(14, z, "L", 0, 2)
    st.raum(3, 7, 7, "o")
    st.raum(0, 12, 12, "4")
    st.raum(1, 5, 6, "x")
    return st


registrieren("polizei_eg", polizei_eg(), POLIZEI)
registrieren("polizei_og", polizei_og(), POLIZEI)


def polizei_dach_extra(s):
    for y in range(4):
        s[y][3][12] = "I"
    s[4][3][12] = "R"
    s[0][7][7] = "e"


registrieren("polizei_dach", flachdach("polizei_dach", polizei_dach_extra), POLIZEI)
gebaeude("polizeiwache", [("polizei_eg", {"floor": 0}), ("polizei_og", {})], "polizei_dach", minf=2, maxf=2)

# ---------------------------------------------------------------------------------------------
# Waffenladen (1 Stockwerk, vergittert)
# ---------------------------------------------------------------------------------------------
WAFFEN = {"#": "minecraft:dark_oak_planks", "f": "minecraft:spruce_planks", "w": "minecraft:iron_bars",
          "C": kiste("loot_waffenkammer"), "B": kiste("loot_waffenkammer", "minecraft:barrel[facing=up]"),
          "c": "minecraft:dark_oak_slab[type=top]", "T": "minecraft:target", "1": spawner("deadzonemc:easymobs")}


def waffenladen():
    st = Stock(fenster="w", tueren=(("n", 7, 8),), mit_leiter=False)
    linie(st.s[1], 2, 6, 13, 6, "c")
    for x in (3, 6, 9, 12):
        st.raum(0, x, 8, "B")
    for z in (2, 3, 4):
        st.raum(0, 1, z, "C")
        st.raum(1, 1, z, "C") if z == 3 else None
    st.saeule(14, 12, "T", 0, 1)
    st.saeule(14, 13, "T", 0, 1)
    st.raum(3, 8, 8, "o")
    st.raum(0, 8, 13, "1")
    st.raum(2, 4, 12, "x")
    return st


registrieren("waffenladen", waffenladen(), WAFFEN)
registrieren("waffenladen_dach", flachdach("waffenladen_dach"), WAFFEN)
gebaeude("waffenladen", [("waffenladen", {})], "waffenladen_dach", filler="#", minf=1, maxf=1)

# ---------------------------------------------------------------------------------------------
# Apotheke (1 Stockwerk, grünes Kreuz)
# ---------------------------------------------------------------------------------------------
APO = {"#": "minecraft:white_concrete", "G": "minecraft:lime_concrete", "f": "minecraft:smooth_quartz",
       "C": kiste("loot_apotheke"), "B": kiste("loot_apotheke", "minecraft:barrel[facing=up]"),
       "c": "minecraft:quartz_slab[type=top]", "1": spawner("deadzonemc:easymobs")}


def apotheke():
    st = Stock(tueren=(("n", 7, 8),), mit_leiter=False)
    # grünes Kreuz über dem Eingang (außen an der Nordwand, Schicht 3 und Decke)
    for x in (5, 10):
        st.s[4][0][x] = "G"
    linie(st.s[1], 3, 5, 12, 5, "c")
    for x in range(2, 14, 2):
        st.raum(0, x, 9, "B")
        st.raum(1, x, 9, "B")
    st.innenwand(1, 11, 14, 11, "#", luecke=(7, 11))
    st.raum(0, 2, 13, "C")
    st.raum(0, 12, 13, "C")
    st.raum(0, 7, 13, "1")
    st.raum(3, 7, 7, "o")
    return st


registrieren("apotheke", apotheke(), APO)
registrieren("apotheke_dach", flachdach("apotheke_dach", lambda s: [s[1][0].__setitem__(x, "G") for x in range(6, 10)]), APO)
gebaeude("apotheke", [("apotheke", {})], "apotheke_dach", minf=1, maxf=1)

# ---------------------------------------------------------------------------------------------
# Krankenhaus (3 Stockwerke): Empfang, Station, OP
# ---------------------------------------------------------------------------------------------
KH = {"#": "minecraft:white_terracotta", "f": "minecraft:white_concrete", "w": "minecraft:white_stained_glass_pane",
      "C": kiste("loot_apotheke"), "K": kiste("loot_bunker"),
      "b": "minecraft:white_bed[facing=south,part=foot]", "h": "minecraft:white_bed[facing=south,part=head]",
      "v": "minecraft:white_stained_glass_pane", "r": "minecraft:red_carpet", "Y": "minecraft:yellow_concrete",
      "1": spawner("deadzonemc:easymobs"), "4": spawner("deadzonemc:hardmobs")}


def kh_eg():
    st = Stock(tueren=(("n", 6, 9),))
    linie(st.s[1], 4, 4, 11, 4, "c")
    st.raum(0, 5, 3, "C")
    for x in range(2, 14, 3):
        st.raum(0, x, 9, "S")
        st.raum(0, x, 12, "S")
    st.raum(0, 13, 1, "C")
    st.raum(0, 1, 13, "1")
    st.raum(0, 8, 10, "r")
    st.raum(3, 7, 7, "o")
    return st


def kh_station():
    st = Stock()
    for x in (2, 5, 9, 12):
        st.raum(0, x, 2, "h")
        st.raum(0, x, 3, "b")
        st.saeule(x + 1, 2, "v", 0, 1)
    for x in (2, 5, 9, 12):
        st.raum(0, x, 12, "b")
        st.raum(0, x, 13, "h")
        st.saeule(x + 1, 13, "v", 0, 1)
    st.raum(0, 7, 7, "r")
    st.raum(0, 8, 8, "r")
    st.raum(0, 6, 9, "r")
    st.raum(0, 1, 7, "C")
    st.raum(0, 14, 7, "C")
    st.raum(0, 7, 4, "4")
    st.raum(2, 10, 9, "x")
    st.raum(3, 7, 7, "o")
    return st


def kh_op():
    st = Stock()
    st.innenwand(1, 6, 14, 6, "#", luecke=(7, 6))
    linie(st.s[1], 6, 2, 9, 3, "e")
    st.raum(0, 3, 2, "k")
    st.raum(0, 12, 2, "K")
    st.raum(0, 12, 4, "C")
    st.raum(0, 7, 4, "r")
    st.raum(0, 10, 3, "r")
    st.raum(0, 2, 12, "K")
    st.raum(0, 7, 10, "4")
    st.raum(3, 7, 3, "R")
    st.raum(2, 4, 9, "x")
    st.raum(2, 11, 12, "x")
    return st


registrieren("krankenhaus_eg", kh_eg(), KH)
registrieren("krankenhaus_station", kh_station(), KH)
registrieren("krankenhaus_op", kh_op(), KH)


def helipad(s):
    for z in range(4, 12):
        for x in range(4, 12):
            s[0][z][x] = "f"
    for z in range(5, 11):
        s[0][z][6] = "Y"
        s[0][z][9] = "Y"
    s[0][8][7] = "Y"
    s[0][8][8] = "Y"


registrieren("krankenhaus_dach", flachdach("krankenhaus_dach", helipad), KH)
gebaeude("krankenhaus", [("krankenhaus_eg", {"floor": 0}), ("krankenhaus_station", {"floor": 1}),
                         ("krankenhaus_op", {})], "krankenhaus_dach", minf=3, maxf=3)

# ---------------------------------------------------------------------------------------------
# Supermarkt (1 Stockwerk): Regale, Kühltruhen, Kassen
# ---------------------------------------------------------------------------------------------
MARKT = {"#": "minecraft:orange_terracotta", "f": "minecraft:smooth_stone", "w": "minecraft:glass_pane",
         "B": kiste("loot_laden", "minecraft:barrel[facing=up]"), "C": kiste("loot_kuehl"),
         "K": kiste("loot_laden_lager"), "1": spawner("deadzonemc:easymobs")}


def supermarkt():
    st = Stock(tueren=(("n", 3, 5), ("n", 10, 12)), mit_leiter=False)
    for x in (3, 7, 11):
        for z in range(5, 12):
            st.raum(0, x, z, "B" if z % 2 else "#")
            st.raum(1, x, z, "B" if z % 2 == 0 else "#")
    for x in range(2, 14):
        st.raum(0, x, 14, "C" if x % 3 == 0 else "e")
    st.raum(0, 2, 2, "c")
    st.raum(0, 7, 2, "c")
    st.raum(0, 12, 2, "c")
    st.raum(0, 14, 13, "K")
    st.raum(0, 9, 8, "1")
    st.raum(3, 5, 8, "o")
    st.raum(3, 9, 8, "o")
    st.raum(2, 13, 6, "x")
    return st


registrieren("supermarkt", supermarkt(), MARKT)
registrieren("supermarkt_dach", flachdach("supermarkt_dach"), MARKT)
gebaeude("supermarkt", [("supermarkt", {})], "supermarkt_dach", minf=1, maxf=1)

# ---------------------------------------------------------------------------------------------
# Bedingungen: Beute und Spawner
# ---------------------------------------------------------------------------------------------
BEDINGUNGEN = {
    "loot_polizei": [(1, "deadzonemc:kiste/polizei")],
    "loot_waffenkammer": [(1, "deadzonemc:kiste/waffenkammer")],
    "loot_buero": [(1, "deadzonemc:kiste/buero")],
    "loot_apotheke": [(1, "deadzonemc:kiste/apotheke")],
    "loot_bunker": [(1, "deadzonemc:kiste/bunker")],
    "loot_laden": [(3, "deadzonemc:kiste/laden_regal"), (1, "deadzonemc:kiste/stadt_laden")],
    "loot_kuehl": [(1, "deadzonemc:kiste/laden_kuehlregal")],
    "loot_laden_lager": [(1, "deadzonemc:kiste/laden_lager")],
    "easymobs": [(4, "deadzonemc:wanderer"), (2, "deadzonemc:kriecher"), (1, "deadzonemc:schwarm"), (2, "minecraft:zombie")],
    "hardmobs": [(3, "deadzonemc:wanderer"), (2, "deadzonemc:brut"), (2, "deadzonemc:schreier"), (1, "deadzonemc:kriecher")],
}

STADT_GEWICHTE = {"polizeiwache": 0.08, "waffenladen": 0.05, "apotheke": 0.08, "krankenhaus": 0.05, "supermarkt": 0.08}


def schreiben():
    for ordner, inhalt in (("parts", TEILE), ("buildings", GEBAEUDE)):
        z = os.path.join(LC, ordner)
        os.makedirs(z, exist_ok=True)
        for f in os.listdir(z):
            os.remove(os.path.join(z, f))
        for n, d in inhalt.items():
            with open(os.path.join(z, f"{n}.json"), "w") as f:
                json.dump(d, f, indent=1)
    z = os.path.join(LC, "conditions")
    os.makedirs(z, exist_ok=True)
    for n, werte in BEDINGUNGEN.items():
        with open(os.path.join(z, f"{n}.json"), "w") as f:
            json.dump({"values": [{"factor": a, "value": b} for a, b in werte]}, f, indent=1)
    # Spawner in allen Stadtgebäuden von Lost Cities: unsere Monster
    for n in ("easymobs", "hardmobs"):
        with open(os.path.join(LC_ORIG, "conditions", f"{n}.json"), "w") as f:
            json.dump({"values": [{"factor": a, "value": b} for a, b in BEDINGUNGEN[n]]}, f, indent=1)
    # Stadtstil: unsere Gebäude zu den normalen dazu
    stil = json.load(open(os.path.join(WURZEL, "werkzeuge", "lc_citystyle_common_original.json")))
    stil = copy.deepcopy(stil)
    for n, g in STADT_GEWICHTE.items():
        stil["selectors"]["buildings"].append({"factor": g, "value": f"deadzonemc:{n}"})
    os.makedirs(os.path.join(LC_ORIG, "citystyles"), exist_ok=True)
    with open(os.path.join(LC_ORIG, "citystyles", "citystyle_common.json"), "w") as f:
        json.dump(stil, f, indent=2)
    print(f"{len(TEILE)} Teile, {len(GEBAEUDE)} Gebäude geschrieben")


if __name__ == "__main__":
    schreiben()
