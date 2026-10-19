"""Boss-Köpfe als Itemmodelle: Die Kopf-Knochen (kopf + alle Kinder) aus den GeckoLib-Modellen werden in ein
normales Item-Modell (Block-Elemente mit Box-UV) übersetzt; die Bosstextur wird als Item-Textur mitkopiert.

  python3 werkzeuge/koepfe.py
"""
import json
import os
import shutil

W = os.path.dirname(os.path.abspath(__file__))
A = os.path.join(W, "..", "src", "main", "resources", "assets", "deadzonemc")
BOSSE = ["schlaechter", "erfrorene", "pilzvater", "nebelmutter", "schlund", "brutkoenigin", "strahlende", "hirte",
         "kommandant", "verschmolzener", "das_herz", "der_ursprung"]
NAMEN = {"schlaechter": "Kopf des Schlächters", "erfrorene": "Kopf des Erfrorenen", "pilzvater": "Kopf des Pilzvaters",
         "nebelmutter": "Kopf der Nebelmutter", "schlund": "Maul des Schlunds", "brutkoenigin": "Kopf der Brutkönigin",
         "strahlende": "Kopf des Strahlenden", "hirte": "Kopf des Hirten", "kommandant": "Helm des Kommandanten",
         "verschmolzener": "Köpfe des Verschmolzenen", "das_herz": "Haupt des Herzens der Zone", "der_ursprung": "Haupt des Ursprungs"}
ERLAUBT = [-45, -22.5, 0, 22.5, 45]


def kopf_wurzeln(knochen):
    return [b["name"] for b in knochen if b["name"] == "kopf" or b["name"].startswith("kopf_")]


def nachfahren(knochen, wurzeln):
    alle = set(wurzeln)
    neu = True
    while neu:
        neu = False
        for b in knochen:
            if b.get("parent") in alle and b["name"] not in alle:
                alle.add(b["name"])
                neu = True
    return alle


def uv_box(u, v, w, h, d):
    return {"east": (u, v + d, d, h), "north": (u + d, v + d, w, h), "west": (u + d + w, v + d, d, h),
            "south": (u + 2 * d + w, v + d, w, h), "up": (u + d, v, w, d), "down": (u + d + w, v, w, d)}


def bauen(boss):
    pfad = os.path.join(A, "geo", "entity", f"{boss}.geo.json")
    g = json.load(open(pfad))["minecraft:geometry"][0]
    tw = g["description"]["texture_width"]
    th = g["description"]["texture_height"]
    knochen = g["bones"]
    namen = nachfahren(knochen, kopf_wurzeln(knochen))
    nach_name = {b["name"]: b for b in knochen}
    wuerfel = []
    for b in knochen:
        if b["name"] not in namen:
            continue
        # Drehung des Knochens (nur die eigene, wenn sie um genau eine Achse geht)
        rot = b.get("rotation", [0, 0, 0])
        for c in b.get("cubes", []):
            wuerfel.append((c, rot if b["name"] not in ("kopf",) and not b["name"].startswith("kopf_") else [0, 0, 0],
                            b.get("pivot", [0, 0, 0])))
    if not wuerfel:
        return False
    xs, ys, zs = [], [], []
    for c, _, _ in wuerfel:
        o, s, inf = c["origin"], c["size"], c.get("inflate", 0)
        xs += [o[0] - inf, o[0] + s[0] + inf]
        ys += [o[1] - inf, o[1] + s[1] + inf]
        zs += [o[2] - inf, o[2] + s[2] + inf]
    groesse = max(max(xs) - min(xs), max(ys) - min(ys), max(zs) - min(zs))
    k = 14.0 / groesse
    mx, my, mz = (max(xs) + min(xs)) / 2, min(ys), (max(zs) + min(zs)) / 2

    def p(x, y, z):
        # Bedrock-x ist gespiegelt; Mitte auf 8/1/8 setzen
        return [round(8 - (x - mx) * k, 4), round(1 + (y - my) * k, 4), round(8 + (z - mz) * k, 4)]

    elemente = []
    for c, rot, pivot in wuerfel:
        o, s, inf = c["origin"], c["size"], c.get("inflate", 0)
        a = p(o[0] + s[0] + inf, o[1] - inf, o[2] - inf)
        b = p(o[0] - inf, o[1] + s[1] + inf, o[2] + s[2] + inf)
        von = [min(a[i], b[i]) for i in range(3)]
        bis = [max(a[i], b[i]) for i in range(3)]
        if any(bis[i] - von[i] <= 0 for i in range(3)):
            for i in range(3):
                if bis[i] - von[i] <= 0:
                    bis[i] = von[i] + 0.01
        flaechen = {}
        if isinstance(c.get("uv"), list):
            u, v = c["uv"]
            box = uv_box(u, v, s[0], s[1], s[2])
            for name, (fu, fv, fw, fh) in box.items():
                # gespiegelt: Ost und West tauschen die Seiten
                ziel = {"east": "west", "west": "east"}.get(name, name)
                uv = [fu * 16 / tw, fv * 16 / th, (fu + fw) * 16 / tw, (fv + fh) * 16 / th]
                if name == "down":
                    uv = [uv[0], uv[3], uv[2], uv[1]]
                flaechen[ziel] = {"uv": [round(x, 4) for x in uv], "texture": "#t"}
        elif isinstance(c.get("uv"), dict):
            for name, f in c["uv"].items():
                fu, fv = f["uv"]
                fw, fh = f.get("uv_size", [0, 0])
                ziel = {"east": "west", "west": "east"}.get(name, name)
                flaechen[ziel] = {"uv": [round(fu * 16 / tw, 4), round(fv * 16 / th, 4), round((fu + fw) * 16 / tw, 4),
                                         round((fv + fh) * 16 / th, 4)], "texture": "#t"}
        e = {"from": von, "to": bis, "faces": flaechen}
        achsen = [i for i in range(3) if abs(rot[i]) > 1]
        if len(achsen) == 1:
            i = achsen[0]
            w = -rot[i] if i != 0 else rot[i]
            w = min(ERLAUBT, key=lambda x: abs(x - w))
            if w != 0:
                e["rotation"] = {"angle": w, "axis": "xyz"[i], "origin": p(*pivot)}
        elemente.append(e)
    os.makedirs(os.path.join(A, "textures", "item", "kopf"), exist_ok=True)
    shutil.copy(os.path.join(A, "textures", "entity", f"{boss}.png"), os.path.join(A, "textures", "item", "kopf", f"{boss}.png"))
    modell = {"textures": {"t": f"deadzonemc:item/kopf/{boss}", "particle": f"deadzonemc:item/kopf/{boss}"},
              "elements": elemente,
              "display": {"gui": {"rotation": [25, 210, 0], "translation": [0, 0, 0], "scale": [0.6, 0.6, 0.6]},
                          "fixed": {"rotation": [0, 180, 0], "translation": [0, 0, 0], "scale": [0.9, 0.9, 0.9]},
                          "ground": {"translation": [0, 2, 0], "scale": [0.4, 0.4, 0.4]},
                          "head": {"translation": [0, -4, 0], "scale": [1.0, 1.0, 1.0]},
                          "thirdperson_righthand": {"rotation": [60, 45, 0], "translation": [0, 1, 0], "scale": [0.4, 0.4, 0.4]},
                          "firstperson_righthand": {"rotation": [0, 225, 0], "translation": [0, 2, 0], "scale": [0.45, 0.45, 0.45]}}}
    with open(os.path.join(A, "models", "item", f"kopf_{boss}.json"), "w") as f:
        json.dump(modell, f, indent=1)
    return True


def main():
    texte = {"block.deadzonemc.trophaeenbrett": "Trophäenbrett",
             "block.deadzonemc.trophaeenbrett.info": "Rechtsklick mit einem Boss-Kopf (oder etwas anderem) zum Aufhängen"}
    for b in BOSSE:
        if bauen(b):
            texte[f"item.deadzonemc.kopf_{b}"] = NAMEN[b]
    with open(os.path.join(W, "koepfe_texte.json"), "w", encoding="utf-8") as f:
        json.dump(texte, f, indent=1, ensure_ascii=False)
    print(len(texte) - 2, "Köpfe")


if __name__ == "__main__":
    main()
