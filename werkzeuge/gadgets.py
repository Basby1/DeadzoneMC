"""Bilder, Modelle, Rezepte und Texte der Klassen-Ausrüstung und der Lockrufe.

  python3 werkzeuge/gadgets.py   (danach texte.py)
"""
import json
import os

from PIL import Image

W = os.path.dirname(os.path.abspath(__file__))
R = os.path.join(W, "..", "src", "main", "resources")
A = os.path.join(R, "assets", "deadzonemc")
D = os.path.join(R, "data", "deadzonemc")


def c(h):
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4)) + (255,)


def zeichne(px, farben):
    b = Image.new("RGBA", (16, 16))
    for y, z in enumerate(px):
        for x, ch in enumerate(z):
            if ch != ".":
                b.putpixel((x, y), c(farben[ch]))
    return b


BILDER = {
    "gluehhammer": (["................", "..kkkkkk........", ".kmMMmmok.......", ".kmmmmmoOk......", ".kmmmmmok.......", "..kkkkkbk.......",
                     "......kbk.......", ".....kbk........", "....kbk.........", "...kbk..........", "..kbk...........", ".kbk............",
                     ".kk.............", "................", "................", "................"],
                    {"k": "1a1a1a", "m": "5a5e62", "M": "8a8e92", "o": "e8601a", "O": "ffd84a", "b": "4a3424"}),
    "adrenalinspritze": (["................", "............kk..", "...........kmk..", "..........kmk...", ".........kRRk...", "........kRrRk...",
                          ".......kRrRk....", "......kRrRk.....", ".....kRRRk......", "....kssk........", "...kssk.........", "..kk.k..........",
                          ".k..............", "................", "................", "................"],
                         {"k": "1a1a1a", "m": "a8acb0", "R": "c83a3a", "r": "ff7a5a", "s": "d8d8d8"}),
    "patronengurt": (["................", "................", "...kkkkkkkkkk...", "..kllllllllllk..", "..klbblbblbblk..", "..klbblbblbblk..",
                      "..klyylyylyylk..", "..kllllllllllk..", "...kkkkkkkkkk...", "................", "................", "................",
                      "................", "................", "................", "................"],
                     {"k": "1a1a1a", "l": "6a4a2a", "b": "c8a03a", "y": "8a6a1a"}),
    "sanitaetstasche": (["................", "................", ".....kkkkkk.....", "....k......k....", "..kkkkkkkkkkkk..", "..kwwwwwwwwwwk..",
                         "..kwwwwrrwwwwk..", "..kwwwrrrrwwwk..", "..kwwwrrrrwwwk..", "..kwwwwrrwwwwk..", "..kwwwwwwwwwwk..", "..kggggggggggk..",
                         "..kkkkkkkkkkkk..", "................", "................", "................"],
                        {"k": "1a1a1a", "w": "d8d4c8", "r": "c82a2a", "g": "9a968a"}),
    "fernglas": (["................", "................", "................", "..kkkk..kkkk....", ".kmmmmkkmmmmk...", ".kmggmkkmggmk...",
                  ".kmgGmmmmgGmk...", ".kmmmmkkmmmmk...", ".kmmmmk.kmmmmk..", ".kddddk.kddddk..", ".kddddk.kddddk..", "..kkkk...kkkk...",
                  "................", "................", "................", "................"],
                 {"k": "1a1a1a", "m": "3a4a3a", "g": "4a7aa8", "G": "c8e8ff", "d": "2a2a2a"}),
    "kernresonator": (["................", "......kkkk......", ".....kooook.....", "....kooOOook....", "...kooOYYOook...", "...koOYwwYOok...",
                       "...koOYwwYOok...", "...kooOYYOook...", "....kooOOook....", ".....kooook.....", "......kkkk......", ".....kmmmmk.....",
                       "....kmmmmmmk....", "....kkkkkkkk....", "................", "................"],
                      {"k": "1a1a1a", "o": "8a2a0a", "O": "e8601a", "Y": "ffd84a", "w": "fff8e0", "m": "4a4e52"}),
    "leerensignal": (["................", ".......k........", "......kpk.......", ".......k........", "......kmk.......", ".....kmmmk......",
                      "....kmpppmk.....", "....kmpPpmk.....", "....kmpppmk.....", "....kmmmmmk.....", ".....kmmmk......", "....kkmmmkk.....",
                      "...kmmmmmmmk....", "...kkkkkkkkk....", "................", "................"],
                     {"k": "1a1a1a", "m": "5a5e72", "p": "8a3ac8", "P": "e8b8ff"}),
}

TEXTE = {
    "item.deadzonemc.gluehhammer": "Glühhammer",
    "item.deadzonemc.gluehhammer.info": "Treffer setzen in Brand · geduckt benutzen: Stampfschlag",
    "item.deadzonemc.adrenalinspritze": "Adrenalinspritze",
    "item.deadzonemc.adrenalinspritze.info": "30 s stärker und schneller – danach erschöpft",
    "item.deadzonemc.patronengurt": "Patronengurt",
    "item.deadzonemc.patronengurt.info": "Im Inventar: 20 % schneller nachladen",
    "item.deadzonemc.sanitaetstasche": "Feldsanitätstasche",
    "item.deadzonemc.sanitaetstasche.info": "Rechtsklick auf Mitspieler (geduckt: dich selbst) – verbraucht einen Verband",
    "item.deadzonemc.sanitaetstasche.kein_verband": "Du hast keinen Verband dabei.",
    "item.deadzonemc.fernglas": "Fernglas",
    "item.deadzonemc.fernglas.info": "Hindurchsehen und ein Monster 1 s anvisieren: markiert es für alle",
    "item.deadzonemc.kernresonator": "Kernresonator",
    "item.deadzonemc.kernresonator.info": "Im Nether benutzen: ruft das Herz der Zone erneut (nach dem ersten Sieg)",
    "item.deadzonemc.leerensignal": "Leerensignal",
    "item.deadzonemc.leerensignal.info": "Im End benutzen: weckt den Ursprung erneut (nach dem ersten Sieg)",
    "item.deadzonemc.lockruf.falsche_welt.das_herz": "Der Resonator schwingt nur im Nether.",
    "item.deadzonemc.lockruf.falsche_welt.der_ursprung": "Das Signal erreicht den Ursprung nur im End.",
    "item.deadzonemc.lockruf.noch_nicht": "Du musst ihn erst einmal besiegt haben.",
    "item.deadzonemc.lockruf.schon_da": "Ein Boss ist bereits in der Nähe.",
    "block.deadzonemc.stoersender": "Störsender",
    "block.deadzonemc.stoersender.laufzeit": "Störsender läuft noch %s min",
    "block.deadzonemc.stoersender.leer": "Störsender aus – mit Batterien betreiben (je 3 min)",
    "klassenvorteil.deadzonemc.item.adrenalinspritze": "Wirkt 45 s und macht zusätzlich zäher",
    "klassenvorteil.deadzonemc.item.patronengurt": "35 % schneller nachladen statt 20 %",
    "klassenvorteil.deadzonemc.item.sanitaetstasche": "Heilt mehr, gibt Schutz, kürzere Pause",
    "klassenvorteil.deadzonemc.item.betaeubungspistole": "Längere Betäubung, heilt Mitspieler stärker",
    "klassenvorteil.deadzonemc.item.stoersender": "Reichweite 18 statt 12 Blöcke (vom Techniker aufgestellt)",
    "klassenvorteil.deadzonemc.item.fernglas": "Markiert länger und alle Monster um das Ziel",
    "klassenvorteil.deadzonemc.item.wurfmesser": "+50 % Schaden",
    "klassenvorteil.deadzonemc.item.gluehhammer": "Stampfschlag stärker, größer und öfter; +25 % Schaden",
}


def jd(pfad, obj):
    os.makedirs(os.path.dirname(pfad), exist_ok=True)
    with open(pfad, "w", encoding="utf-8") as f:
        json.dump(obj, f, indent=1, ensure_ascii=False)


def geformt(name, muster, schluessel, anzahl=1):
    key = {k: ({"tag": v[1:]} if v.startswith("#") else {"item": v}) for k, v in schluessel.items()}
    jd(os.path.join(D, "recipe", name + ".json"), {"type": "minecraft:crafting_shaped", "category": "equipment", "pattern": muster,
                                                   "key": key, "result": {"id": "deadzonemc:" + name, "count": anzahl}})


def ungeformt(name, zutaten, anzahl=1):
    jd(os.path.join(D, "recipe", name + ".json"), {"type": "minecraft:crafting_shapeless", "category": "equipment",
                                                   "ingredients": [({"tag": z[1:]} if z.startswith("#") else {"item": z}) for z in zutaten],
                                                   "result": {"id": "deadzonemc:" + name, "count": anzahl}})


def main():
    m = "deadzonemc:"
    for name, (px, fb) in BILDER.items():
        zeichne(px, fb).save(os.path.join(A, "textures", "item", name + ".png"))
        modell = {"parent": "minecraft:item/handheld" if name == "gluehhammer" else "minecraft:item/generated",
                  "textures": {"layer0": f"deadzonemc:item/{name}"}}
        jd(os.path.join(A, "models", "item", name + ".json"), modell)
    # Störsender: kleiner Kasten mit Antenne, Lampe an/aus
    for an in (False, True):
        jd(os.path.join(A, "models", "block", "stoersender" + ("_an" if an else "") + ".json"), {
            "parent": "minecraft:block/block",
            "textures": {"k": "minecraft:block/gray_concrete", "o": "minecraft:block/black_concrete",
                         "l": "minecraft:block/" + ("sea_lantern" if an else "gray_terracotta"), "a": "minecraft:block/iron_block",
                         "particle": "minecraft:block/gray_concrete"},
            "elements": [{"from": [3, 0, 3], "to": [13, 8, 13], "faces": {f: {"texture": "#k"} for f in ("north", "south", "east", "west", "down")}
                          | {"up": {"texture": "#o"}}},
                         {"from": [5, 8, 5], "to": [8, 10, 8], "faces": {f: {"texture": "#l"} for f in ("north", "south", "east", "west", "up", "down")}},
                         {"from": [10, 8, 10], "to": [11, 16, 11], "faces": {f: {"texture": "#a"} for f in ("north", "south", "east", "west", "up", "down")}}]})
    jd(os.path.join(A, "blockstates", "stoersender.json"), {"variants": {"an=false": {"model": "deadzonemc:block/stoersender"},
                                                                        "an=true": {"model": "deadzonemc:block/stoersender_an"}}})
    jd(os.path.join(A, "models", "item", "stoersender.json"), {"parent": "deadzonemc:block/stoersender"})
    jd(os.path.join(D, "loot_table", "blocks", "stoersender.json"),
       {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": "deadzonemc:stoersender"}]}]})
    p = os.path.join(R, "data", "minecraft", "tags", "block", "mineable", "pickaxe.json")
    t = json.load(open(p))
    if "deadzonemc:stoersender" not in t["values"]:
        t["values"].append("deadzonemc:stoersender")
    jd(p, t)
    # Rezepte – sie verwerten genau die Drops, die bisher kaum Verwendung hatten
    geformt("gluehhammer", ["GPG", " S ", " S "], {"G": m + "glutkern", "P": m + "panzerplatte", "S": "minecraft:stick"})
    ungeformt("adrenalinspritze", [m + "mutagen", "minecraft:glass_bottle", m + "sehne"], 2)
    geformt("patronengurt", ["L L", "LNL"], {"L": "minecraft:leather", "N": "minecraft:iron_nugget"})
    geformt("sanitaetstasche", ["SBS", "SVS"], {"S": m + "stoffreste", "B": m + "baumwolle", "V": m + "verband"})
    geformt("fernglas", ["GEG", "IMI"], {"G": "minecraft:glass", "E": m + "elektronik", "I": "minecraft:iron_ingot", "M": m + "mottenstaub"})
    geformt("stoersender", ["DED", "KBK"], {"D": m + "drohnenteile", "E": m + "elektronik", "K": m + "klebeband", "B": m + "batterie"})
    geformt("kernresonator", ["KGK", "GMG", "KGK"], {"K": m + "kernsplitter", "G": m + "glutkern", "M": m + "mutagen"})
    geformt("leerensignal", ["SDS", "DXD", "SDS"], {"S": m + "strahlenkristall", "D": m + "drohnenteile", "X": "minecraft:end_crystal"})
    jd(os.path.join(W, "gadgets_texte.json"), TEXTE)
    print(len(BILDER), "Gadgets")


if __name__ == "__main__":
    main()
