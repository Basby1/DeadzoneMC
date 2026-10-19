"""Wand-Deko: Plakate und Kreidezeichen (Texturen, Modelle, Blockstates, Rezepte, Beute, Texte).

  python3 werkzeuge/wanddeko.py   (danach texte.py)
"""
import json
import os

from PIL import Image, ImageDraw

W = os.path.dirname(os.path.abspath(__file__))
A = os.path.join(W, "..", "src", "main", "resources", "assets", "deadzonemc")
D = os.path.join(W, "..", "src", "main", "resources", "data", "deadzonemc")


def c(h, a=255):
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4)) + (a,)


def papier(d, farbe="d8d0b8", rand="a89a7a"):
    d.rectangle((2, 1, 13, 14), fill=c(farbe))
    d.rectangle((2, 1, 13, 14), outline=c(rand))
    d.point([(13, 1), (12, 1), (13, 2)], fill=(0, 0, 0, 0))  # eingerissene Ecke
    d.point([(3, 13), (8, 14)], fill=c("8a7a5a"))


def plakat(motiv):
    b = Image.new("RGBA", (16, 16))
    d = ImageDraw.Draw(b)
    if motiv == 0:  # VERMISST: Gesicht und Textzeilen
        papier(d)
        d.rectangle((4, 2, 11, 3), fill=c("2a2a2a"))
        d.ellipse((6, 4, 9, 7), fill=c("6a6050"))
        d.rectangle((5, 8, 10, 9), fill=c("6a6050"))
        for y in (11, 13):
            d.line([(4, y), (11, y)], fill=c("4a4a4a"))
    elif motiv == 1:  # Evakuierung: grün mit Pfeil
        papier(d, "3a7a4a", "1e4a2a")
        d.rectangle((3, 3, 12, 4), fill=c("f0f0e0"))
        d.polygon([(4, 9), (9, 9), (9, 7), (12, 10), (9, 13), (9, 11), (4, 11)], fill=c("f0f0e0"))
    elif motiv == 2:  # Quarantäne: gelb-schwarz mit Warnzeichen
        papier(d, "e0c030", "6a5a10")
        for i in range(0, 16, 3):
            d.line([(2 + i, 1), (2 + i - 3, 4)], fill=c("1a1a1a"))
        d.polygon([(8, 6), (12, 13), (4, 13)], outline=c("1a1a1a"))
        d.line([(8, 8), (8, 10)], fill=c("1a1a1a"))
        d.point([(8, 12)], fill=c("1a1a1a"))
    else:  # Aufruf der Wacht: rot mit Abzeichen
        papier(d, "8a1e1e", "4a0a0a")
        d.ellipse((5, 3, 10, 8), outline=c("e8dcc0"))
        d.point([(7, 5), (8, 5), (7, 6), (8, 6)], fill=c("e8dcc0"))
        for y in (10, 12):
            d.line([(4, y), (11, y)], fill=c("e8dcc0"))
    return b


def kreide(motiv):
    b = Image.new("RGBA", (16, 16))
    d = ImageDraw.Draw(b)
    k = c("e8e8e0", 225)
    if motiv == 0:  # großes X
        d.line([(3, 3), (12, 12)], fill=k, width=1)
        d.line([(12, 3), (3, 12)], fill=k, width=1)
    elif motiv == 1:  # Pfeil
        d.line([(2, 8), (12, 8)], fill=k)
        d.line([(12, 8), (9, 5)], fill=k)
        d.line([(12, 8), (9, 11)], fill=k)
    elif motiv == 2:  # Suchmarkierung: X mit Zahlen in den Feldern
        d.line([(2, 2), (13, 13)], fill=k)
        d.line([(13, 2), (2, 13)], fill=k)
        d.point([(7, 3), (8, 3), (3, 7), (3, 8), (12, 7), (12, 8), (7, 12), (8, 12), (8, 11)], fill=k)
    else:  # HILFE – Gekritzel
        for x0, y0 in ((2, 5), (6, 5), (10, 5)):
            d.line([(x0, y0), (x0, y0 + 5)], fill=k)
            d.line([(x0, y0 + 2), (x0 + 2, y0 + 2)], fill=k)
            d.line([(x0 + 2, y0), (x0 + 2, y0 + 5)], fill=k)
        d.line([(2, 12), (13, 12)], fill=k)
    return b


def main():
    os.makedirs(os.path.join(A, "textures", "block"), exist_ok=True)
    os.makedirs(os.path.join(A, "models", "block"), exist_ok=True)
    os.makedirs(os.path.join(A, "blockstates"), exist_ok=True)
    for name, fn in (("plakat", plakat), ("kreidezeichen", kreide)):
        varianten = {}
        for m in range(4):
            fn(m).save(os.path.join(A, "textures", "block", f"{name}_{m}.png"))
            with open(os.path.join(A, "models", "block", f"{name}_{m}.json"), "w") as f:
                json.dump({"parent": "minecraft:block/ladder", "render_type": "minecraft:cutout",
                           "textures": {"particle": f"deadzonemc:block/{name}_{m}", "texture": f"deadzonemc:block/{name}_{m}"}}, f, indent=1)
            for richtung, dreh in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
                v = {"model": f"deadzonemc:block/{name}_{m}"}
                if dreh:
                    v["y"] = dreh
                varianten[f"facing={richtung},motiv={m}"] = v
        with open(os.path.join(A, "blockstates", f"{name}.json"), "w") as f:
            json.dump({"variants": varianten}, f, indent=1)
        with open(os.path.join(A, "models", "item", f"{name}.json"), "w") as f:
            json.dump({"parent": "minecraft:item/generated", "textures": {"layer0": f"deadzonemc:block/{name}_0"}}, f, indent=1)
    os.makedirs(os.path.join(D, "loot_table", "blocks"), exist_ok=True)
    with open(os.path.join(D, "loot_table", "blocks", "plakat.json"), "w") as f:
        json.dump({"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": "deadzonemc:plakat"}],
                                                        "conditions": [{"condition": "minecraft:survives_explosion"}]}]}, f, indent=1)
    os.makedirs(os.path.join(D, "recipe"), exist_ok=True)
    with open(os.path.join(D, "recipe", "plakat.json"), "w") as f:
        json.dump({"type": "minecraft:crafting_shapeless", "category": "building",
                   "ingredients": [{"item": "minecraft:paper"}, {"item": "minecraft:paper"}, {"tag": "c:dyes"}],
                   "result": {"id": "deadzonemc:plakat", "count": 2}}, f, indent=1)
    with open(os.path.join(D, "recipe", "kreidezeichen.json"), "w") as f:
        json.dump({"type": "minecraft:crafting_shapeless", "category": "building",
                   "ingredients": [{"item": "minecraft:bone_meal"}, {"item": "minecraft:flint"}],
                   "result": {"id": "deadzonemc:kreidezeichen", "count": 4}}, f, indent=1)
    texte = {"block.deadzonemc.plakat": "Plakat", "block.deadzonemc.kreidezeichen": "Kreidezeichen",
             "item.deadzonemc.plakat": "Plakat", "item.deadzonemc.kreidezeichen": "Kreidezeichen"}
    with open(os.path.join(W, "wanddeko_texte.json"), "w", encoding="utf-8") as f:
        json.dump(texte, f, indent=1, ensure_ascii=False)
    print("Wanddeko geschrieben")


if __name__ == "__main__":
    main()
