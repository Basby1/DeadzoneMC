"""Handgezeichnete 16x16-Pixel-Texturen für die Items von Deadzone MC.

Aufruf aus dem Projektordner:  python3 werkzeuge/texturen.py
Jedes Bild ist ein Raster aus Zeichen; '.' ist durchsichtig, jedes andere Zeichen eine Farbe.
"""
import json
import os

from PIL import Image

ZIEL = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "deadzonemc")


def hexc(s):
    s = s.lstrip("#")
    return tuple(int(s[i:i + 2], 16) for i in (0, 2, 4)) + (255,)


BILDER = {
    "konservendose": ({"o": "#2b2b2e", "m": "#9aa0a6", "h": "#d4d8dc", "d": "#6b7075",
                       "r": "#a0402c", "l": "#d8c58a", "s": "#7a2e20"}, [
        "................",
        "................",
        "....oooooooo....",
        "...ohhhhhhmmo...",
        "...omhmmmmmdo...",
        "...oooooooooo...",
        "...orrrrrrrso...",
        "...orllllllso...",
        "...orlrrrlrso...",
        "...orllllllso...",
        "...orrrrrrrso...",
        "...oooooooooo...",
        "...ohmmmmmmdo...",
        "....oooooooo....",
        "................",
        "................",
    ]),
    "verband": ({"o": "#7d7466", "w": "#f2ede2", "s": "#d6cfbf", "r": "#a32a2a", "h": "#ffffff"}, [
        "................",
        "................",
        "................",
        ".....oooooo.....",
        "....owwhwwwo....",
        "...owwwwwwsso...",
        "...owwwrrwwso...",
        "...owwrrrrwso...",
        "...owwwrrwwso...",
        "...owwwwwwsso...",
        "....osswwssoooo.",
        ".....oooossswwo.",
        ".........ooooo..",
        "................",
        "................",
        "................",
    ]),
    "antibiotika": ({"o": "#2a2a2e", "w": "#eef0f2", "s": "#c4c8cc", "b": "#2f6fb0", "d": "#1f4f80",
                     "r": "#c23a3a", "k": "#e9e2c8"}, [
        "................",
        "................",
        ".....oooooo.....",
        "....orrrrrro....",
        "....orrrrrro....",
        "....oooooooo....",
        "....owwwwwso....",
        "....obbbbbdo....",
        "....owkkkkso....",
        "....owkbbkso....",
        "....owkkkkso....",
        "....obbbbbdo....",
        "....owwwwwso....",
        "....oooooooo....",
        "................",
        "................",
    ]),
    "schmerzmittel": ({"o": "#4a4a50", "f": "#b8bcc2", "h": "#e2e5e8", "p": "#f4f0e6", "s": "#c9c2b0"}, [
        "................",
        "................",
        "................",
        "..oooooooooooo..",
        "..offfhfffhffo..",
        "..ofpsffpsffpo..",
        "..ofssffssffso..",
        "..offfffffffo...",
        "..ofpsffpsffpo..",
        "..ofssffssffso..",
        "..offfhfffhffo..",
        "..oooooooooooo..",
        "................",
        "................",
        "................",
        "................",
    ]),
    "schrott": ({"o": "#2a2622", "r": "#8a4a26", "b": "#b8662e", "g": "#6e6e6e", "h": "#a8a8a8",
                 "d": "#4a4a4a"}, [
        "................",
        "................",
        "......oo........",
        ".....oggo.......",
        "....oghgdo..oo..",
        "...oggrgdo.obro.",
        "...ogrbrdo.orro.",
        "....odrdoo..oo..",
        "..oo.ooo..ooo...",
        ".obro....ohggo..",
        ".orrbo..ogrbgdo.",
        "..orro..odrrddo.",
        "...oo....odddo..",
        ".........ooooo..",
        "................",
        "................",
    ]),
    "stoffreste": ({"o": "#3e3a34", "a": "#8a7f6a", "b": "#5f6f7a", "c": "#9a5a4a", "h": "#b8ad96"}, [
        "................",
        "................",
        "................",
        "....oooo........",
        "...ohaaao.......",
        "...oaaaaaoooo...",
        "....oaaaoobbbo..",
        ".....oooobbbbbo.",
        "....occco.obbbo.",
        "...occcccooboo..",
        "...occcccco.....",
        "....oooccco.....",
        ".......ooo......",
        "................",
        "................",
        "................",
    ]),
    "batterie": ({"o": "#1e1e1e", "k": "#3a3a3a", "y": "#c9a43a", "g": "#5a7a3a", "m": "#b0b0b0",
                  "h": "#e0d080"}, [
        "................",
        "................",
        "......oooo......",
        "......ommo......",
        ".....oooooo.....",
        ".....oyhyyo.....",
        ".....oyyyyo.....",
        ".....okkkko.....",
        ".....okggko.....",
        ".....okggko.....",
        ".....okkkko.....",
        ".....okkkko.....",
        ".....okkkko.....",
        ".....oooooo.....",
        "................",
        "................",
    ]),
    "elektronik": ({"o": "#1a2a1a", "g": "#2f6b3a", "l": "#4d9a55", "y": "#c9a43a", "k": "#202020",
                    "m": "#9a9a9a"}, [
        "................",
        "................",
        "................",
        "..oooooooooooo..",
        "..oggglggggggo..",
        "..ogkkkgyygmgo..",
        "..ogkkkglllggo..",
        "..ogkkkgyyggmo..",
        "..ogggglggggmo..",
        "..ogyyygggkkgo..",
        "..ogglggggkkgo..",
        "..oggggyyyggyo..",
        "..oooooooooooo..",
        "...m..m..m..m...",
        "................",
        "................",
    ]),
    "notiz": ({"o": "#5a5040", "p": "#e6dcc0", "s": "#c9bc98", "t": "#4a4a50", "f": "#a89060"}, [
        "................",
        "................",
        "...ooooooooo....",
        "...opppppppso...",
        "...opttptttpo...",
        "...opppppppso...",
        "...opttttptpo...",
        "...oppppppppso..",
        "...opttpttttpo..",
        "...oppppppppso..",
        "...opttttpppfo..",
        "...opppppppffo..",
        "...osssssssfo...",
        "...oooooooooo...",
        "................",
        "................",
    ]),
    "handbuch": ({"o": "#2a1f16", "b": "#6b3a26", "h": "#8a4e33", "p": "#e6dcc0", "r": "#a32a2a", "g": "#c9a43a"}, [
        "................",
        "................",
        "...oooooooooo...",
        "..obhhhhhhhhbo..",
        "..obhhhhhhhhbop.",
        "..obhhhrrhhhbop.",
        "..obhhrrrrhhbop.",
        "..obhhhrrhhhbop.",
        "..obhhhhhhhhbop.",
        "..obhggggggbbop.",
        "..obhhhhhhhhbop.",
        "..obhhhhhhhhbop.",
        "..obbbbbbbbbbop.",
        "...ooooooooooop.",
        "....pppppppppp..",
        "................",
    ]),
}


def main():
    ordner = os.path.join(ZIEL, "textures", "item")
    modelle = os.path.join(ZIEL, "models", "item")
    os.makedirs(ordner, exist_ok=True)
    os.makedirs(modelle, exist_ok=True)
    for name, (farben, raster) in BILDER.items():
        assert len(raster) == 16 and all(len(z) == 16 for z in raster), name
        bild = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
        for y, zeile in enumerate(raster):
            for x, zeichen in enumerate(zeile):
                if zeichen != ".":
                    bild.putpixel((x, y), hexc(farben[zeichen]))
        bild.save(os.path.join(ordner, f"{name}.png"))
        with open(os.path.join(modelle, f"{name}.json"), "w", encoding="utf-8") as f:
            json.dump({"parent": "minecraft:item/generated",
                       "textures": {"layer0": f"deadzonemc:item/{name}"}}, f, indent=2)
            f.write("\n")
    print(f"{len(BILDER)} Texturen geschrieben.")


if __name__ == "__main__":
    main()
