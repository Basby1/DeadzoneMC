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


# ---- Ladenschilder (Block deadzonemc:ladenschild, siehe werkzeuge/schilder.py) ----------------
SCHILD_BREITEN = json.load(open(os.path.join(os.path.dirname(os.path.abspath(__file__)), "schild_breiten.json")))
_SCHILD_ZEICHEN = {}
_FACING = {"n": "north", "s": "south", "o": "east", "w": "west"}


def sz(art, facing, teil_):
    k = (art, facing, teil_)
    if k not in _SCHILD_ZEICHEN:
        c = chr(0x100 + len(_SCHILD_ZEICHEN))
        _SCHILD_ZEICHEN[k] = c
        GRUND[c] = f"deadzonemc:ladenschild[art={art},facing={facing},teil={teil_}]"
    return _SCHILD_ZEICHEN[k]


def schild(raster, art, seite, mitte=8):
    """Schild in die Außenwand einer Schicht setzen (lesbar von außen, von links nach rechts)."""
    n = SCHILD_BREITEN[art]
    p0 = max(1, min(15 - n, mitte - n // 2))
    f = _FACING[seite]
    for i in range(n):
        q = p0 + i
        if seite == "n":
            raster[0][q] = sz(art, f, n - 1 - i)
        elif seite == "s":
            raster[15][q] = sz(art, f, i)
        elif seite == "o":
            raster[q][15] = sz(art, f, n - 1 - i)
        else:
            raster[q][0] = sz(art, f, i)


def schild_an(st, art, seite="n", schicht=4):
    schild(st.s[schicht], art, seite)
    return st


def dach_schilder(art, extra=None):
    """Flachdach, dessen Brüstung auf allen vier Seiten das Schild trägt – von Weitem zu sehen."""
    def mach(s):
        for seite in ("n", "s", "o", "w"):
            schild(s[0], art, seite)
        if extra:
            extra(s)
    return mach


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


registrieren("polizei_eg", schild_an(polizei_eg(), "polizei"), POLIZEI)
registrieren("polizei_og", polizei_og(), POLIZEI)


def polizei_dach_extra(s):
    for y in range(4):
        s[y][3][12] = "I"
    s[4][3][12] = "R"
    s[0][7][7] = "e"


registrieren("polizei_dach", flachdach("polizei_dach", dach_schilder("polizei", polizei_dach_extra)), POLIZEI)
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


registrieren("waffenladen", schild_an(waffenladen(), "waffen"), WAFFEN)
registrieren("waffenladen_dach", flachdach("waffenladen_dach", dach_schilder("waffen")), WAFFEN)
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


registrieren("apotheke", schild_an(apotheke(), "apotheke"), APO)
registrieren("apotheke_dach", flachdach("apotheke_dach", dach_schilder("apotheke", lambda s: [s[1][0].__setitem__(x, "G") for x in range(6, 10)])), APO)
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


registrieren("krankenhaus_eg", schild_an(kh_eg(), "krankenhaus"), KH)
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


registrieren("krankenhaus_dach", flachdach("krankenhaus_dach", dach_schilder("krankenhaus", helipad)), KH)
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


registrieren("supermarkt", schild_an(supermarkt(), "supermarkt"), MARKT)
registrieren("supermarkt_dach", flachdach("supermarkt_dach", dach_schilder("supermarkt")), MARKT)
gebaeude("supermarkt", [("supermarkt", {})], "supermarkt_dach", minf=1, maxf=1)


# ---------------------------------------------------------------------------------------------
# Vorort: Einfamilienhäuser mit Garten, Bungalow, Reihenhaus – der weiche Übergang vom Land zur Stadt
# ---------------------------------------------------------------------------------------------
def haus_palette(wand, dach_treppe, dach_platte, giebel, zaun="minecraft:oak_fence"):
    return {
        "_": "minecraft:grass_block", "p": "minecraft:dirt_path", "z": zaun, "#": wand, "F": "minecraft:spruce_planks",
        "w": "minecraft:glass_pane", "N": "minecraft:oak_door[facing=north,half=lower]",
        "M": "minecraft:oak_door[facing=north,half=upper]", "q": "minecraft:short_grass", "j": "minecraft:dead_bush",
        "Y": "minecraft:oak_leaves[persistent=true]", "v": f"{dach_treppe}[facing=south]", "^": f"{dach_treppe}[facing=north]",
        "=": f"{dach_platte}[type=bottom]", "G": giebel, "X": "minecraft:bricks",
        "K": kiste("loot_wohnung"), "k": kiste("loot_kueche", "minecraft:barrel[facing=up]"),
        "u": kiste("loot_kuehlschrank", "minecraft:barrel[facing=north]"),
        "T": kiste("loot_kleiderschrank"), "n": kiste("loot_nachttisch", "minecraft:barrel[facing=up]"),
        "a": kiste("loot_bad", "minecraft:barrel[facing=up]"), "i": kiste("loot_kinder"),
        "h": kiste("loot_schuppen"), "e": kiste("loot_werkzeug", "minecraft:barrel[facing=up]"),
        "b": "minecraft:red_bed[facing=south,part=head]", "B": "minecraft:red_bed[facing=south,part=foot]",
        "y": "minecraft:light_blue_bed[facing=south,part=head]", "Q": "minecraft:light_blue_bed[facing=south,part=foot]",
        "1": spawner("deadzonemc:easymobs"), "W": "minecraft:water_cauldron[level=1]", "f": "minecraft:spruce_planks",
        "C": "minecraft:smooth_stone_slab[type=top]", "O": "minecraft:smoker[facing=south]",
        "s": "minecraft:spruce_stairs[facing=south]", "S": "minecraft:spruce_stairs[facing=north]",
        "t": "minecraft:spruce_slab[type=top]", "L": "minecraft:bookshelf", "o": "minecraft:lantern[hanging=true]",
        "x": "minecraft:cobweb", "l": "minecraft:ladder[facing=south]", "r": "minecraft:red_carpet",
        "H": "minecraft:spruce_planks",
    }


def garten(sl, haus=(3, 4, 12, 12), tor=(7, 8)):
    """Grundstück: Rasen, Zaun mit Tor, Weg zur Haustür, etwas Wildwuchs."""
    for z in range(16):
        for x in range(16):
            sl[0][z][x] = "_"
    x0, z0, x1, z1 = haus
    for z in range(z0, z1 + 1):
        for x in range(x0, x1 + 1):
            sl[0][z][x] = "F"
    for z in range(0, z0):
        for x in tor:
            sl[0][z][x] = "p"
    for i in range(16):
        for (x, z) in ((i, 0), (i, 15), (0, i), (15, i)):
            if not (z == 0 and x in tor):
                sl[1][z][x] = "z"
    for (x, z, c) in ((2, 2, "q"), (13, 2, "j"), (4, 14, "q"), (1, 8, "Y"), (14, 9, "q"), (11, 1, "q"), (2, 13, "j")):
        sl[1][z][x] = c
    return sl


def hauswand(sl, x0, z0, x1, z1, tuer_x=7, fenster=True, unten=1, oben=4):
    for y in range(unten, oben + 1):
        for x in range(x0, x1 + 1):
            for z in (z0, z1):
                sl[y][z][x] = "w" if fenster and y in (2, 3) and (x - x0) % 3 == 2 and x0 < x < x1 else "#"
        for z in range(z0, z1 + 1):
            for x in (x0, x1):
                sl[y][z][x] = "w" if fenster and y in (2, 3) and (z - z0) % 3 == 2 and z0 < z < z1 else "#"
    if tuer_x is not None:
        sl[unten][z0][tuer_x] = "N"
        sl[unten + 1][z0][tuer_x] = "M"


def leer_stock():
    return [leer() for _ in range(6)]


def satteldach(x0, z0, x1, z1, schornstein=(10, 9)):
    """Satteldach über (x0..x1, z0..z1), First in x-Richtung, Giebel an den Seiten."""
    s = [leer() for _ in range(6)]
    tiefe = z1 - z0 + 1
    for k in range(6):
        za, zb = z0 - 1 + k, z1 + 1 - k
        if za > zb:
            break
        for x in range(x0 - 1, x1 + 2):
            if za == zb:
                s[k][za][x] = "="
            else:
                s[k][za][x] = "v"
                s[k][zb][x] = "^"
        for z in range(za + 1, zb):
            s[k][z][x0] = "G"
            s[k][z][x1] = "G"
    sx, sz_ = schornstein
    for k in range(5):
        s[k][sz_][sx] = "X"
    return s


def haus_a_eg():
    sl = garten(leer_stock())
    hauswand(sl, 3, 4, 12, 12)
    for x in range(3, 13):
        for z in range(4, 13):
            sl[5][z][x] = "F"
    # Küche (West)
    for z in (5, 6, 7):
        sl[1][z][4] = "C"
    sl[1][5][4], sl[1][8][4] = "u", "k"
    sl[1][6][4] = "O"
    sl[2][6][4] = " "
    # Wohnzimmer (Ost)
    for x in (8, 9, 10):
        sl[1][11][x] = "S"
    sl[1][9][9] = "t"
    sl[1][5][11] = "L"
    sl[2][5][11] = "L"
    sl[1][6][11] = "K"
    sl[1][8][8] = "r"
    sl[1][9][8] = "r"
    # Treppe nach oben (Leiter)
    for y in range(1, 6):
        sl[y][11][11] = "l"
    sl[4][8][7] = "o"
    sl[1][10][5] = "1"
    # Gartenschuppen
    for x in (12, 13, 14):
        for y in (1, 2):
            sl[y][14][x] = "H"
    sl[1][13][13] = "h"
    sl[1][13][14] = "e"
    return sl


def haus_a_og():
    sl = leer_stock()
    for z in range(16):
        for x in range(16):
            sl[0][z][x] = " "
    for z in range(4, 13):
        for x in range(3, 13):
            sl[0][z][x] = "F"
            sl[5][z][x] = "F"
    hauswand(sl, 3, 4, 12, 12, tuer_x=None, unten=0, oben=4)
    for x in range(3, 13):
        sl[0][4][x] = "#"
        sl[0][12][x] = "#"
    for z in range(4, 13):
        sl[0][z][3] = "#"
        sl[0][z][12] = "#"
    for z in range(4, 13):
        for x in range(3, 13):
            if 3 < x < 12 and 4 < z < 12:
                sl[0][z][x] = "F"
    # Elternschlafzimmer
    sl[1][5][5], sl[1][6][5] = "b", "B"
    sl[1][5][6], sl[1][6][6] = "b", "B"
    sl[1][5][4] = "n"
    sl[1][7][4] = "T"
    # Kinderzimmer
    sl[1][5][9], sl[1][6][9] = "y", "Q"
    sl[1][5][10] = "i"
    # Bad
    for x in range(4, 8):
        for y in range(1, 5):
            sl[y][9][x] = "#"
    sl[1][9][6] = " "
    sl[2][9][6] = " "
    sl[1][11][4] = "W"
    sl[1][10][4] = "a"
    for y in range(0, 6):
        sl[y][11][11] = "l"
    sl[4][7][8] = "o"
    sl[2][6][10] = "x"
    return sl


VORORT = {}


def haus_typ(name, wand, treppe, platte, giebel, zaun="minecraft:oak_fence"):
    pal = haus_palette(wand, treppe, platte, giebel, zaun)
    registrieren(f"{name}_eg", haus_a_eg(), pal)
    registrieren(f"{name}_og", haus_a_og(), pal)
    registrieren(f"{name}_dach", satteldach(3, 4, 12, 12), pal)
    gebaeude(name, [(f"{name}_eg", {"floor": 0}), (f"{name}_og", {})], f"{name}_dach", filler="#", minf=2, maxf=2)


haus_typ("haus_rot", "minecraft:bricks", "minecraft:dark_oak_stairs", "minecraft:dark_oak_slab", "minecraft:bricks")
haus_typ("haus_weiss", "minecraft:white_terracotta", "minecraft:deepslate_tile_stairs", "minecraft:deepslate_tile_slab",
         "minecraft:white_terracotta", "minecraft:spruce_fence")
haus_typ("haus_gelb", "minecraft:yellow_terracotta", "minecraft:brick_stairs", "minecraft:brick_slab",
         "minecraft:yellow_terracotta", "minecraft:dark_oak_fence")


def bungalow():
    sl = garten(leer_stock(), haus=(2, 4, 13, 11))
    hauswand(sl, 2, 4, 13, 11)
    for z in range(4, 12):
        for x in range(2, 14):
            sl[5][z][x] = "="
    for x in range(1, 15):
        sl[5][3][x] = "="
        sl[5][12][x] = "="
    sl[1][5][3], sl[1][6][3] = "b", "B"
    sl[1][5][4] = "n"
    sl[1][9][3] = "T"
    sl[1][10][12] = "u"
    sl[1][9][12] = "k"
    for x in (9, 10, 11):
        sl[1][5][x] = "C"
    sl[1][7][7] = "t"
    sl[1][8][7] = "S"
    sl[1][7][10] = "K"
    sl[1][10][6] = "a"
    sl[4][7][7] = "o"
    sl[1][9][9] = "1"
    for x in (12, 13, 14):
        for y in (1, 2):
            sl[y][14][x] = "H"
    sl[1][13][13] = "h"
    return sl


registrieren("bungalow", bungalow(), haus_palette("minecraft:stripped_spruce_wood", "minecraft:spruce_stairs",
                                                  "minecraft:spruce_slab", "minecraft:stripped_spruce_wood"))
registrieren("bungalow_dach", [leer() for _ in range(6)], haus_palette("minecraft:stripped_spruce_wood", "minecraft:spruce_stairs",
                                                                     "minecraft:spruce_slab", "minecraft:stripped_spruce_wood"))
gebaeude("bungalow", [("bungalow", {})], "bungalow_dach", minf=1, maxf=1)

# ---------------------------------------------------------------------------------------------
# Tankstelle: kleiner Shop, Zapfsäulen unter einem Dach
# ---------------------------------------------------------------------------------------------
TANK = {"#": "minecraft:white_concrete", "A": "minecraft:gray_concrete", "a": "minecraft:black_concrete",
        "P": "minecraft:red_concrete", "Z": "minecraft:white_concrete", "D": "minecraft:smooth_quartz_slab[type=bottom]",
        "K": kiste("loot_tankstelle"), "B": kiste("loot_tankstelle", "minecraft:barrel[facing=up]"),
        "u": kiste("loot_kuehl", "minecraft:barrel[facing=north]"), "1": spawner("deadzonemc:easymobs"),
        "N": "minecraft:iron_door[facing=north,half=lower]", "M": "minecraft:iron_door[facing=north,half=upper]",
        "c": "minecraft:smooth_stone_slab[type=top]", "R": "minecraft:redstone_lamp"}


def tankstelle():
    sl = leer_stock()
    for z in range(16):
        for x in range(16):
            sl[0][z][x] = "a" if (x + z) % 7 else "A"
    # Shop
    for y in range(1, 5):
        for x in range(1, 8):
            for z in (8, 15):
                sl[y][z][x] = "w" if y in (2, 3) and z == 8 and x in (2, 3, 5, 6) else "#"
        for z in range(8, 16):
            for x in (1, 7):
                sl[y][z][x] = "#"
    sl[1][8][4], sl[2][8][4] = "N", "M"
    for z in range(8, 16):
        for x in range(1, 8):
            sl[5][z][x] = "#"
    for x in (2, 3):
        sl[1][10][x] = "c"
    sl[1][11][2] = "K"
    for x in (3, 4, 5):
        sl[1][14][x] = "B"
    sl[1][14][6] = "u"
    sl[1][12][5] = "1"
    sl[4][11][4] = "o"
    # Dach über den Zapfsäulen
    for (x, z) in ((9, 2), (14, 2), (9, 6), (14, 6)):
        for y in range(1, 5):
            sl[y][z][x] = "Z"
    for x in range(8, 16):
        for z in range(1, 8):
            sl[5][z][x] = "D"
    for (x, z) in ((11, 4), (12, 4)):
        sl[1][z][x] = "P"
        sl[2][z][x] = "P"
    return sl


def tankstelle_dach():
    s = [leer() for _ in range(6)]
    for x in range(8, 16):
        s[0][1][x] = "#"
    schild(s[0], "tankstelle", "n", mitte=12)
    rahmen_shop = s[0]
    for x in range(1, 8):
        rahmen_shop[8][x] = "#"
        rahmen_shop[15][x] = "#"
    for z in range(8, 16):
        rahmen_shop[z][1] = "#"
        rahmen_shop[z][7] = "#"
    return s


registrieren("tankstelle", tankstelle(), TANK)
registrieren("tankstelle_dach", tankstelle_dach(), TANK)
gebaeude("tankstelle", [("tankstelle", {})], "tankstelle_dach", minf=1, maxf=1)

# ---------------------------------------------------------------------------------------------
# Werkstatt: Halle mit Rolltor, Werkbänke, Werkzeugkisten
# ---------------------------------------------------------------------------------------------
WERK = {"#": "minecraft:gray_concrete", "f": "minecraft:smooth_stone", "w": "minecraft:glass_pane",
        "K": kiste("loot_werkstatt"), "E": kiste("loot_werkzeug", "minecraft:barrel[facing=up]"),
        "a": "minecraft:anvil[facing=east]", "T": "minecraft:crafting_table", "j": "minecraft:chain",
        "1": spawner("deadzonemc:easymobs"), "D": "minecraft:iron_trapdoor[half=top,open=false,facing=north]",
        "P": "minecraft:black_concrete", "R": "minecraft:red_concrete", "Q": "minecraft:black_wool"}


def werkstatt():
    st = Stock(tueren=(("n", 4, 11),), mit_leiter=False)
    for x in range(4, 12):
        st.s[3][0][x] = "D"
        st.s[4][0][x] = "#"
    # Autowrack auf der Hebebühne
    for x in range(6, 10):
        for z in range(5, 11):
            st.s[1][z][x] = "R" if z not in (5, 10) else "P"
    for (x, z) in ((6, 6), (9, 6), (6, 9), (9, 9)):
        st.s[1][z][x] = "Q"
    for x in range(7, 9):
        for z in range(6, 9):
            st.s[2][z][x] = "w"
    for z in range(2, 14, 2):
        st.raum(0, 1, z, "K" if z % 4 == 0 else "E")
    st.raum(0, 14, 3, "a")
    st.raum(0, 14, 5, "T")
    st.raum(0, 14, 8, "E")
    st.raum(0, 14, 12, "K")
    for z in (4, 11):
        st.saeule(8, z, "j", 2, 3)
    st.raum(0, 12, 13, "1")
    st.raum(3, 8, 7, "o")
    return st


registrieren("werkstatt", schild_an(werkstatt(), "werkstatt", schicht=5), WERK)
registrieren("werkstatt_dach", flachdach("werkstatt_dach", dach_schilder("werkstatt")), WERK)
gebaeude("werkstatt", [("werkstatt", {})], "werkstatt_dach", minf=1, maxf=1)

# ---------------------------------------------------------------------------------------------
# Baumarkt: Regale voller Baumaterial – wichtig für die eigene Basis
# ---------------------------------------------------------------------------------------------
BAU = {"#": "minecraft:orange_concrete", "f": "minecraft:smooth_stone", "w": "minecraft:glass_pane",
       "B": kiste("loot_baumarkt", "minecraft:barrel[facing=up]"), "K": kiste("loot_baumarkt"),
       "H": "minecraft:stripped_oak_log[axis=y]", "c": "minecraft:smooth_stone_slab[type=top]",
       "1": spawner("deadzonemc:easymobs"), "4": spawner("deadzonemc:hardmobs")}


def baumarkt():
    st = Stock(tueren=(("n", 6, 9),), mit_leiter=False)
    for x in (3, 6, 9, 12):
        for z in range(4, 13):
            for y in range(3):
                st.raum(y, x, z, "B" if (z + y + x) % 3 == 0 else "H")
    for x in (2, 5):
        st.raum(0, x, 2, "c")
    st.raum(0, 14, 14, "K")
    st.raum(0, 1, 14, "K")
    st.raum(0, 8, 8, "1")
    st.raum(0, 11, 13, "4")
    st.raum(3, 7, 7, "o")
    st.raum(3, 4, 10, "x")
    return st


registrieren("baumarkt", schild_an(baumarkt(), "baumarkt"), BAU)
registrieren("baumarkt_dach", flachdach("baumarkt_dach", dach_schilder("baumarkt")), BAU)
gebaeude("baumarkt", [("baumarkt", {})], "baumarkt_dach", minf=1, maxf=1)

# ---------------------------------------------------------------------------------------------
# Militärposten: Sandsäcke, Stacheldraht, Zelt, Munitionskisten – die beste Beute, die meisten Gegner
# ---------------------------------------------------------------------------------------------
MIL = {"#": "minecraft:brown_wool", "s": "minecraft:mud_bricks", "_": "minecraft:coarse_dirt",
       "d": "deadzonemc:stacheldraht", "g": "minecraft:green_wool", "G": "minecraft:green_carpet",
       "K": kiste("loot_militaer"), "A": kiste("loot_militaer", "minecraft:barrel[facing=up]"),
       "W": kiste("loot_waffenkammer"), "h": "minecraft:spruce_planks", "l": "minecraft:ladder[facing=north]",
       "4": spawner("deadzonemc:hardmobs"), "5": spawner("deadzonemc:militaer"), "j": "minecraft:chain",
       "R": "minecraft:redstone_lamp", "e": "minecraft:iron_bars", "f": "minecraft:coarse_dirt"}


def militaerposten():
    sl = leer_stock()
    for z in range(16):
        for x in range(16):
            sl[0][z][x] = "_"
    # Sandsackwall mit Durchlass, davor Stacheldraht
    for i in range(1, 15):
        for (x, z) in ((i, 1), (i, 14), (1, i), (14, i)):
            if not (z == 1 and 6 <= x <= 9):
                sl[1][z][x] = "s" if (x + z) % 2 else "#"
                sl[2][z][x] = "#" if (x + z) % 3 else " "
    for x in range(0, 16):
        if not 6 <= x <= 9:
            sl[1][0][x] = "d"
    # Zelt
    for x in range(3, 8):
        for z in range(9, 13):
            sl[1][z][x] = " "
            sl[0][z][x] = "_"
        sl[1][9][x] = "g"
        sl[1][12][x] = "g"
        sl[2][10][x] = "g"
        sl[2][11][x] = "g"
        sl[3][10][x] = "g"
        sl[3][11][x] = "g"
    for z in (10, 11):
        sl[1][z][3] = "g"
    sl[1][10][6] = "K"
    sl[1][11][6] = "A"
    # Wachturm
    for y in range(1, 6):
        for (x, z) in ((11, 10), (13, 10), (11, 12), (13, 12)):
            sl[y][z][x] = "h"
        sl[y][11][12] = "l"
    for x in range(10, 15):
        for z in range(9, 14):
            sl[5][z][x] = "h"
    # Munitionskisten offen herum
    sl[1][4][3] = "W"
    sl[1][4][4] = "A"
    sl[1][5][12] = "K"
    sl[1][6][8] = "4"
    sl[1][3][11] = "5"
    return sl


def militaer_turm():
    s = [leer() for _ in range(6)]
    for x in range(10, 15):
        for z in (9, 13):
            s[0][z][x] = "e"
    for z in range(9, 14):
        s[0][z][10] = "e"
        s[0][z][14] = "e"
    s[0][11][12] = " "
    s[1][11][12] = "R"
    for x in range(1, 4):
        s[0][1][x] = "#"
    schild(s[0], "militaer", "n", mitte=12)
    return s


registrieren("militaerposten", militaerposten(), MIL)
registrieren("militaerposten_dach", militaer_turm(), MIL)
gebaeude("militaerposten", [("militaerposten", {})], "militaerposten_dach", minf=1, maxf=1)

# ---------------------------------------------------------------------------------------------
# Bank: Schalterhalle, dahinter der Tresorraum
# ---------------------------------------------------------------------------------------------
BANKP = {"#": "minecraft:smooth_quartz", "C": "minecraft:quartz_pillar[axis=y]", "f": "minecraft:polished_diorite",
         "K": kiste("loot_buero"), "V": kiste("loot_bank"), "e": "minecraft:iron_bars", "c": "minecraft:quartz_slab[type=top]",
         "N": "minecraft:iron_door[facing=north,half=lower]", "M": "minecraft:iron_door[facing=north,half=upper]",
         "Z": "deadzonemc:tresor[facing=south]", "1": spawner("deadzonemc:easymobs"), "4": spawner("deadzonemc:hardmobs")}


def bank():
    st = Stock(tueren=(("n", 7, 8),), mit_leiter=False)
    for x in (3, 12):
        st.saeule(x, 1, "C")
    linie(st.s[1], 2, 6, 13, 6, "c")
    for x in range(2, 14, 3):
        for y in (1, 2):
            st.raum(y, x, 6, "e")
    st.raum(0, 4, 7, "K")
    st.raum(0, 11, 7, "K")
    st.innenwand(1, 10, 14, 10, "#")
    st.s[1][10][7] = "N"
    st.s[2][10][7] = "M"
    for x in (3, 5, 9, 11):
        st.raum(0, x, 13, "V")
    st.raum(0, 7, 14, "Z")
    st.raum(0, 13, 12, "4")
    st.raum(0, 3, 3, "1")
    st.raum(3, 7, 3, "o")
    st.raum(3, 7, 12, "o")
    return st


registrieren("bank", schild_an(bank(), "bank"), BANKP)
registrieren("bank_dach", flachdach("bank_dach", dach_schilder("bank")), BANKP)
gebaeude("bank", [("bank", {})], "bank_dach", minf=1, maxf=1)

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
    "loot_wohnung": [(3, "deadzonemc:kiste/stadt_wohnung"), (1, "deadzonemc:kiste/wohnzimmer")],
    "loot_kueche": [(1, "deadzonemc:kiste/kueche")],
    "loot_kuehlschrank": [(1, "deadzonemc:kiste/kuehlschrank")],
    "loot_kleiderschrank": [(1, "deadzonemc:kiste/kleiderschrank")],
    "loot_nachttisch": [(1, "deadzonemc:kiste/nachttisch")],
    "loot_bad": [(1, "deadzonemc:kiste/badezimmer")],
    "loot_kinder": [(1, "deadzonemc:kiste/kinderzimmer")],
    "loot_schuppen": [(1, "deadzonemc:kiste/schuppen")],
    "loot_werkzeug": [(1, "deadzonemc:kiste/werkzeug")],
    "loot_werkstatt": [(1, "deadzonemc:kiste/werkstatt")],
    "loot_tankstelle": [(1, "deadzonemc:kiste/tankstelle")],
    "loot_baumarkt": [(1, "deadzonemc:kiste/baumarkt")],
    "loot_militaer": [(1, "deadzonemc:kiste/militaer")],
    "loot_bank": [(1, "deadzonemc:kiste/bank")],
    "militaer": [(4, "deadzonemc:gefallener_soldat"), (2, "deadzonemc:wanderer"), (1, "deadzonemc:juggernaut")],
    "easymobs": [(4, "deadzonemc:wanderer"), (2, "deadzonemc:kriecher"), (1, "deadzonemc:schwarm"), (2, "minecraft:zombie")],
    "hardmobs": [(3, "deadzonemc:wanderer"), (2, "deadzonemc:brut"), (2, "deadzonemc:schreier"), (1, "deadzonemc:kriecher")],
}

STADT_GEWICHTE = {"polizeiwache": 0.09, "waffenladen": 0.06, "apotheke": 0.08, "krankenhaus": 0.05, "supermarkt": 0.08,
                  "baumarkt": 0.05, "werkstatt": 0.05, "bank": 0.05, "militaerposten": 0.04}

# Vororte (Stadtrand): niedrige Häuser mit Garten, dazwischen Läden
VORORT_GEWICHTE = {"haus_rot": 0.5, "haus_weiss": 0.45, "haus_gelb": 0.4, "bungalow": 0.45, "tankstelle": 0.12,
                   "werkstatt": 0.1, "supermarkt": 0.07, "apotheke": 0.06, "polizeiwache": 0.04, "baumarkt": 0.06}


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
    # Vorort-Stil (für den Stadtrand, siehe cityStyleAlternative im Profil)
    vorort = {"style": "standard", "inherit": "citystyle_common",
              "buildingsettings": {"maxfloors": 2, "maxcellars": 1, "buildingchance": 0.6},
              "selectors": {"buildings": [{"factor": g, "value": f"deadzonemc:{n}"} for n, g in VORORT_GEWICHTE.items()]
                            + [{"factor": 0.08, "value": "building1"}],
                            "multibuildings": []}}
    z = os.path.join(LC, "citystyles")
    os.makedirs(z, exist_ok=True)
    with open(os.path.join(z, "citystyle_vorort.json"), "w") as f:
        json.dump(vorort, f, indent=2)
    print(f"{len(TEILE)} Teile, {len(GEBAEUDE)} Gebäude geschrieben")


if __name__ == "__main__":
    schreiben()
