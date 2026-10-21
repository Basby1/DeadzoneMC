"""Strom-Netz: Scheinwerfer, Elektrozaun, Panzertür (Texturen, Modelle, Blockstates, Beute, Rezepte, Texte).

  python3 werkzeuge/strom.py   (danach texte.py)
"""
import json
import os

from PIL import Image, ImageDraw

W = os.path.dirname(os.path.abspath(__file__))
A = os.path.join(W, "..", "src", "main", "resources", "assets", "deadzonemc")
D = os.path.join(W, "..", "src", "main", "resources", "data", "deadzonemc")
TX = os.path.join(A, "textures", "block")
MB = os.path.join(A, "models", "block")


def c(h, a=255):
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4)) + (a,)


def jd(pfad, daten):
    os.makedirs(os.path.dirname(pfad), exist_ok=True)
    with open(pfad, "w") as f:
        json.dump(daten, f, indent=1)


def stahl(d, grund="4e5458", rand="2e3236"):
    d.rectangle((0, 0, 15, 15), fill=c(grund))
    d.rectangle((0, 0, 15, 15), outline=c(rand))


def texturen():
    for an in (False, True):
        b = Image.new("RGBA", (16, 16))
        d = ImageDraw.Draw(b)
        stahl(d)
        d.ellipse((2, 2, 13, 13), fill=c("fff4c8" if an else "3a3e3a"), outline=c("1e2022"))
        d.ellipse((5, 5, 10, 10), fill=c("ffffff" if an else "50544e"))
        b.save(os.path.join(TX, f"scheinwerfer_front{'_an' if an else ''}.png"))
    b = Image.new("RGBA", (16, 16))
    d = ImageDraw.Draw(b)
    stahl(d)
    for y in range(3, 14, 3):
        d.line([(2, y), (13, y)], fill=c("383c40"))
    b.save(os.path.join(TX, "scheinwerfer_seite.png"))
    # Elektrozaun: Drähte mit gelben Isolatoren
    b = Image.new("RGBA", (16, 16))
    d = ImageDraw.Draw(b)
    for x in (0, 15):
        d.line([(x, 0), (x, 15)], fill=c("5a5e60"))
    for y in (2, 7, 12):
        d.line([(0, y), (15, y)], fill=c("8a8e90"))
        d.rectangle((6, y - 1, 9, y + 1), fill=c("e0c030"))
    b.save(os.path.join(TX, "elektrozaun.png"))
    # Panzertür: genietete Stahlplatte mit Sichtschlitz
    for teil in ("unten", "oben"):
        b = Image.new("RGBA", (16, 16))
        d = ImageDraw.Draw(b)
        stahl(d, "5a6066", "2a2e32")
        for x, y in ((2, 2), (13, 2), (2, 13), (13, 13), (7, 2), (7, 13)):
            d.point((x, y), fill=c("9aa0a6"))
        if teil == "oben":
            d.rectangle((4, 5, 11, 7), fill=c("16181a"))
        else:
            d.rectangle((11, 3, 13, 5), fill=c("8a7a4a"))  # Riegel
            d.line([(2, 9), (13, 9)], fill=c("3e4246"))
        b.save(os.path.join(TX, f"panzertuer_{teil}.png"))
    b = Image.new("RGBA", (16, 16))
    d = ImageDraw.Draw(b)
    d.rectangle((4, 0, 11, 15), fill=c("5a6066"), outline=c("2a2e32"))
    d.rectangle((5, 3, 10, 4), fill=c("16181a"))
    d.point((10, 9), fill=c("8a7a4a"))
    b.save(os.path.join(A, "textures", "item", "panzertuer.png"))


def modelle():
    # Scheinwerfer
    for an in ("", "_an"):
        jd(os.path.join(MB, f"scheinwerfer{an}.json"), {"parent": "minecraft:block/orientable", "textures": {
            "top": "deadzonemc:block/scheinwerfer_seite", "side": "deadzonemc:block/scheinwerfer_seite",
            "front": f"deadzonemc:block/scheinwerfer_front{an}"}})
    jd(os.path.join(A, "blockstates", "scheinwerfer.json"), {"variants": {
        f"facing={r},strom={s}": dict({"model": f"deadzonemc:block/scheinwerfer{'_an' if s == 'true' else ''}"}, **({"y": y} if y else {}))
        for r, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270)) for s in ("false", "true")}})
    jd(os.path.join(A, "models", "item", "scheinwerfer.json"), {"parent": "deadzonemc:block/scheinwerfer_an"})
    # Elektrozaun (wie Eisengitter)
    tex = {"particle": "deadzonemc:block/elektrozaun", "bars": "deadzonemc:block/elektrozaun", "edge": "deadzonemc:block/elektrozaun"}
    for teil in ("post_ends", "post", "cap", "cap_alt", "side", "side_alt"):
        jd(os.path.join(MB, f"elektrozaun_{teil}.json"), {"parent": f"minecraft:block/iron_bars_{teil}",
                                                          "render_type": "minecraft:cutout", "textures": tex})
    m = lambda t, y=0: dict({"model": f"deadzonemc:block/elektrozaun_{t}"}, **({"y": y} if y else {}))
    f = "false"
    jd(os.path.join(A, "blockstates", "elektrozaun.json"), {"multipart": [
        {"apply": m("post_ends")},
        {"when": {"north": f, "west": f, "south": f, "east": f}, "apply": m("post")},
        {"when": {"north": "true", "west": f, "south": f, "east": f}, "apply": m("cap")},
        {"when": {"north": f, "west": f, "south": f, "east": "true"}, "apply": m("cap", 90)},
        {"when": {"north": f, "west": f, "south": "true", "east": f}, "apply": m("cap_alt")},
        {"when": {"north": f, "west": "true", "south": f, "east": f}, "apply": m("cap_alt", 90)},
        {"when": {"north": "true"}, "apply": m("side")},
        {"when": {"east": "true"}, "apply": m("side", 90)},
        {"when": {"south": "true"}, "apply": m("side_alt")},
        {"when": {"west": "true"}, "apply": m("side_alt", 90)}]})
    jd(os.path.join(A, "models", "item", "elektrozaun.json"), {"parent": "minecraft:item/generated",
                                                               "textures": {"layer0": "deadzonemc:block/elektrozaun"}})
    # Panzertür (wie die Tür im Spiel)
    for half, h in (("lower", "bottom"), ("upper", "top")):
        for hinge in ("left", "right"):
            for offen in ("", "_open"):
                jd(os.path.join(MB, f"panzertuer_{h}_{hinge}{offen}.json"), {
                    "parent": f"minecraft:block/door_{h}_{hinge}{offen}", "render_type": "minecraft:cutout",
                    "textures": {"bottom": "deadzonemc:block/panzertuer_unten", "top": "deadzonemc:block/panzertuer_oben"}})
    varianten = {}
    basis = {"east": 0, "south": 90, "west": 180, "north": 270}
    for richtung, y0 in basis.items():
        for half, h in (("lower", "bottom"), ("upper", "top")):
            for hinge in ("left", "right"):
                for offen in ("false", "true"):
                    y = y0 if offen == "false" else (y0 + (90 if hinge == "left" else 270)) % 360
                    v = {"model": f"deadzonemc:block/panzertuer_{h}_{hinge}{'_open' if offen == 'true' else ''}"}
                    if y:
                        v["y"] = y
                    varianten[f"facing={richtung},half={half},hinge={hinge},open={offen}"] = v
    jd(os.path.join(A, "blockstates", "panzertuer.json"), {"variants": varianten})
    jd(os.path.join(A, "models", "item", "panzertuer.json"), {"parent": "minecraft:item/generated",
                                                              "textures": {"layer0": "deadzonemc:item/panzertuer"}})


def daten():
    lt = os.path.join(D, "loot_table", "blocks")
    for n in ("scheinwerfer", "elektrozaun"):
        jd(os.path.join(lt, f"{n}.json"), {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [
            {"type": "minecraft:item", "name": f"deadzonemc:{n}"}], "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
    jd(os.path.join(lt, "panzertuer.json"), {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [
        {"type": "minecraft:item", "name": "deadzonemc:panzertuer", "conditions": [
            {"condition": "minecraft:block_state_property", "block": "deadzonemc:panzertuer", "properties": {"half": "lower"}}]}],
        "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
    rz = os.path.join(D, "recipe")
    jd(os.path.join(rz, "scheinwerfer.json"), {"type": "minecraft:crafting_shaped", "category": "redstone",
        "pattern": ["IGI", "IBI"], "key": {"I": {"item": "minecraft:iron_ingot"}, "G": {"item": "minecraft:glass"},
                                           "B": {"item": "deadzonemc:batterie"}}, "result": {"id": "deadzonemc:scheinwerfer", "count": 2}})
    jd(os.path.join(rz, "elektrozaun.json"), {"type": "minecraft:crafting_shaped", "category": "redstone",
        "pattern": ["BBB", "BEB"], "key": {"B": {"item": "minecraft:iron_bars"}, "E": {"item": "deadzonemc:elektronik"}},
        "result": {"id": "deadzonemc:elektrozaun", "count": 6}})
    jd(os.path.join(rz, "panzertuer.json"), {"type": "minecraft:crafting_shaped", "category": "redstone",
        "pattern": ["PP", "PI", "PP"], "key": {"P": {"item": "deadzonemc:panzerplatte"}, "I": {"item": "minecraft:iron_ingot"}},
        "result": {"id": "deadzonemc:panzertuer", "count": 1}})
    tag = os.path.join(D, "..", "minecraft", "tags", "block", "mineable", "pickaxe.json")
    t = json.load(open(tag))
    for n in ("scheinwerfer", "elektrozaun", "panzertuer"):
        if f"deadzonemc:{n}" not in t["values"]:
            t["values"].append(f"deadzonemc:{n}")
    jd(tag, t)
    texte = {
        "block.deadzonemc.scheinwerfer": "Scheinwerfer", "item.deadzonemc.scheinwerfer": "Scheinwerfer",
        "block.deadzonemc.elektrozaun": "Elektrozaun", "item.deadzonemc.elektrozaun": "Elektrozaun",
        "block.deadzonemc.panzertuer": "Panzertür", "item.deadzonemc.panzertuer": "Panzertür",
        "tooltip.deadzonemc.scheinwerfer": "Leuchtet hell, wenn er im Bereich eines Notstromgenerators steht.",
        "tooltip.deadzonemc.elektrozaun": "Steht im Bereich eines Notstromgenerators unter Strom: Monster bekommen einen Schlag.",
        "tooltip.deadzonemc.panzertuer": "Infizierte können sie nicht aufbrechen. Öffnen von Hand oder mit Redstone.",
    }
    with open(os.path.join(W, "strom_texte.json"), "w", encoding="utf-8") as f:
        json.dump(texte, f, indent=1, ensure_ascii=False)


if __name__ == "__main__":
    texturen()
    modelle()
    daten()
    print("Strom-Netz geschrieben")
