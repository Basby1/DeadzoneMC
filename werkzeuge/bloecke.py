"""Block- und Rüstungstexturen samt Modellen: Strahlenfass und Gasmaske (am Kopf getragen).

  python3 werkzeuge/bloecke.py
"""
import json
import os

from PIL import Image

A = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "deadzonemc")
D = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "data", "deadzonemc")


def hexc(s):
    s = s.lstrip("#")
    return tuple(int(s[i:i + 2], 16) for i in (0, 2, 4)) + (255,)


def bild(farben, raster, breite=16, hoehe=16):
    b = Image.new("RGBA", (breite, hoehe), (0, 0, 0, 0))
    for y, zeile in enumerate(raster):
        assert len(zeile) == breite, (len(zeile), zeile)
        for x, c in enumerate(zeile):
            if c != ".":
                b.putpixel((x, y), hexc(farben[c]))
    return b


FASS = {"o": "#1e1f1c", "y": "#c9a43a", "Y": "#e0bf55", "d": "#8a6e22", "k": "#1a1a1a", "r": "#7a3f1c",
        "g": "#7ad44a", "G": "#c4ff7a", "m": "#5c5f4a"}
FASS_SEITE = [
    "oyyyyyyyyyyyyyyo",
    "omyyyyyyyyyyyymo",
    "oddddddddddddddo",
    "oyyYyyyyyyyryyyo",
    "oyyYyykkkyyyyryo",
    "oyyYykykykyyyyyo",
    "oyyYykkgkkyyrryo",
    "oyyYyykkkyyyyyyo",
    "oyyYykyyykyyyyyo",
    "oddddddddddddddo",
    "oyyYyyyyyyrryyyo",
    "oyyYyyryyyyyyyyo",
    "oyyYyyyygyyyyyyo",
    "omyyyyyygGyyyymo",
    "oddddddddgdddddo",
    "oooooooooGoooooo",
]
FASS_OBEN = [
    "oooooooooooooooo",
    "oddddddddddddddo",
    "odyyyyyyyyyyyydo",
    "odyggggggggggydo",
    "odygGGggggGgyydo",
    "odyggGgggggggydo",
    "odygggggGgggyydo",
    "odyggggGGgggyydo",
    "odyggggggggggydo",
    "odygGgggggGgyydo",
    "odyggggggggggydo",
    "odyggggggggggydo",
    "odyyyyyyyyyyyydo",
    "oddddddddddddddo",
    "oyyyyyyyyyyyyyyo",
    "oooooooooooooooo",
]


def gasmaske_ruestung():
    """64×32-Rüstungstextur: nur der Kopf (Box-UV 8×8×8 bei 0,0) bekommt Maske und Riemen."""
    b = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    f = {"r": "#3b3e40", "h": "#5c6164", "o": "#141516", "l": "#9fd4c8", "L": "#d8f2ea", "f": "#6b6f5a",
         "F": "#8c9177", "s": "#26292a"}
    vorne = [
        "........",
        "rrrrrrrr",
        "roLlolLr",
        "rolloOlr".replace("O", "l"),
        "rrrrrrrr",
        ".rrffrr.",
        "..rFfr..",
        "...ss...",
    ]
    vorne[2] = "rLlrrLlr"
    vorne[3] = "rllrrllr"
    for y, z in enumerate(vorne):
        for x, c in enumerate(z):
            if c != ".":
                b.putpixel((8 + x, 8 + y), hexc(f[c]))
    # Riemen um den Kopf: rechts (0,8), links (16,8), hinten (24,8), oben (8,0)
    for x0 in (0, 16, 24):
        for x in range(8):
            for y in (1, 4):
                b.putpixel((x0 + x, 8 + y), hexc(f["s"]))
    for y in range(8):
        b.putpixel((8 + 3, y), hexc(f["s"]))
        b.putpixel((8 + 4, y), hexc(f["s"]))
    return b


def main():
    os.makedirs(os.path.join(A, "textures", "block"), exist_ok=True)
    os.makedirs(os.path.join(A, "textures", "models", "armor"), exist_ok=True)
    os.makedirs(os.path.join(A, "models", "block"), exist_ok=True)
    os.makedirs(os.path.join(A, "blockstates"), exist_ok=True)
    bild(FASS, FASS_SEITE).save(os.path.join(A, "textures", "block", "strahlenfass_seite.png"))
    bild(FASS, FASS_OBEN).save(os.path.join(A, "textures", "block", "strahlenfass_oben.png"))
    gasmaske_ruestung().save(os.path.join(A, "textures", "models", "armor", "gasmaske_layer_1.png"))
    Image.new("RGBA", (64, 32), (0, 0, 0, 0)).save(os.path.join(A, "textures", "models", "armor", "gasmaske_layer_2.png"))
    modell = {
        "parent": "minecraft:block/block",
        "textures": {"particle": "deadzonemc:block/strahlenfass_seite", "seite": "deadzonemc:block/strahlenfass_seite",
                     "oben": "deadzonemc:block/strahlenfass_oben"},
        "elements": [{
            "from": [2, 0, 2], "to": [14, 15, 14],
            "faces": {
                "north": {"uv": [2, 1, 14, 16], "texture": "#seite"},
                "south": {"uv": [2, 1, 14, 16], "texture": "#seite"},
                "east": {"uv": [2, 1, 14, 16], "texture": "#seite"},
                "west": {"uv": [2, 1, 14, 16], "texture": "#seite"},
                "up": {"uv": [2, 2, 14, 14], "texture": "#oben"},
                "down": {"uv": [2, 2, 14, 14], "texture": "#oben", "cullface": "down"},
            },
        }],
    }
    with open(os.path.join(A, "models", "block", "strahlenfass.json"), "w") as f:
        json.dump(modell, f, indent=1)
    with open(os.path.join(A, "blockstates", "strahlenfass.json"), "w") as f:
        json.dump({"variants": {"": {"model": "deadzonemc:block/strahlenfass"}}}, f, indent=1)
    with open(os.path.join(A, "models", "item", "strahlenfass.json"), "w") as f:
        json.dump({"parent": "deadzonemc:block/strahlenfass"}, f, indent=1)
    # Beute: das Fass lässt sich abbauen und mitnehmen
    os.makedirs(os.path.join(D, "loot_table", "blocks"), exist_ok=True)
    with open(os.path.join(D, "loot_table", "blocks", "strahlenfass.json"), "w") as f:
        json.dump({"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [
            {"type": "minecraft:item", "name": "deadzonemc:strahlenfass"}],
            "conditions": [{"condition": "minecraft:survives_explosion"}]}]}, f, indent=1)
    # Rezepte
    os.makedirs(os.path.join(D, "recipe"), exist_ok=True)
    rezepte = {
        "filter": {"type": "minecraft:crafting_shaped", "category": "equipment",
                   "pattern": [" S ", "SKS", " P "],
                   "key": {"S": {"item": "deadzonemc:stoffreste"}, "K": {"item": "minecraft:charcoal"},
                           "P": {"item": "minecraft:paper"}},
                   "result": {"id": "deadzonemc:filter", "count": 1}},
        "gasmaske": {"type": "minecraft:crafting_shaped", "category": "equipment",
                     "pattern": ["LLL", "GSG", " F "],
                     "key": {"L": {"item": "minecraft:leather"}, "G": {"item": "minecraft:glass_pane"},
                             "S": {"item": "deadzonemc:stoffreste"}, "F": {"item": "deadzonemc:filter"}},
                     "result": {"id": "deadzonemc:gasmaske", "count": 1}},
    }
    for name, r in rezepte.items():
        with open(os.path.join(D, "recipe", f"{name}.json"), "w") as f:
            json.dump(r, f, indent=1)
    print("Blöcke, Rüstung, Rezepte geschrieben")


if __name__ == "__main__":
    main()
