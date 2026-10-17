"""Erzeugt assets/deadzonemc/sounds.json: die Monstergeräusche, gebaut aus Minecraft-Klängen,
tiefer oder höher abgespielt.  python3 werkzeuge/monster/sounds.py [--pruefen]

Mit --pruefen werden alle verwendeten Dateien gegen den offiziellen Asset-Index von 1.21.1 geprüft
(braucht Netz, läuft bei GitHub).
"""
import json
import os
import sys
import urllib.request

WURZEL = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
ZIEL = os.path.join(WURZEL, "src", "main", "resources", "assets", "deadzonemc", "sounds.json")


def reihe(pfad, anzahl, pitch=1.0, volume=1.0, start=1):
    return [{"name": f"minecraft:{pfad}{i}", "pitch": pitch, "volume": volume} for i in range(start, start + anzahl)]


def eins(pfad, pitch=1.0, volume=1.0):
    return [{"name": f"minecraft:{pfad}", "pitch": pitch, "volume": volume}]


SOUNDS = {
    "wanderer.ambient": reihe("mob/zombie/say", 3, 0.72),
    "wanderer.hurt": reihe("mob/zombie/hurt", 2, 0.8),
    "wanderer.death": eins("mob/zombie/death", 0.7),
    "wanderer.step": reihe("mob/zombie/step", 5, 0.8, 0.6),
    "schreier.ambient": reihe("mob/husk/idle", 3, 1.35),
    "schreier.hurt": reihe("mob/husk/hurt", 2, 1.4),
    "schreier.death": eins("mob/ghast/death", 1.25),
    "schreier.schrei": reihe("mob/ghast/scream", 5, 0.62) + eins("mob/ghast/affectionate_scream", 0.6),
    "lauerer.hurt": reihe("mob/endermen/hit", 4, 1.25),
    "lauerer.death": eins("mob/endermen/death", 0.8),
    "lauerer.atmen": eins("mob/wolf/panting", 0.45, 0.6),
    "lauerer.schreck": eins("mob/endermen/stare", 1.3) + reihe("mob/endermen/scream", 4, 0.8),
    "kriecher.ambient": reihe("mob/spider/say", 4, 0.6),
    "kriecher.hurt": reihe("mob/silverfish/hit", 3, 0.6),
    "kriecher.death": eins("mob/spider/death", 0.5),
    "kriecher.step": reihe("mob/spider/step", 4, 0.8),
    "der_stille.hurt": reihe("dig/stone", 4, 0.6),
    "der_stille.death": reihe("random/glass", 3, 0.5),
    "der_stille.naehe": reihe("ambient/cave/cave", 13, 0.6, 0.6),
    "imitator.hurt": reihe("damage/hit", 3, 0.9),
    "imitator.death": eins("damage/hit3", 0.5),
    "nebelwesen.ambient": reihe("mob/vex/idle", 4, 0.5),
    "nebelwesen.hurt": reihe("mob/vex/hurt", 2, 0.5),
    "nebelwesen.death": reihe("mob/vex/death", 2, 0.4),
    "schwarm.ambient": reihe("mob/silverfish/say", 4, 0.7),
    "schwarm.hurt": reihe("mob/silverfish/hit", 3, 0.8),
    "schwarm.death": eins("mob/silverfish/kill", 0.8),
    "schwarm.step": reihe("mob/silverfish/step", 4, 1.0, 0.5),
    "brut.ambient": reihe("mob/slime/big", 4, 0.5),
    "brut.hurt": eins("mob/slime/big1", 0.7),
    "brut.death": eins("mob/slime/big2", 0.4),
    "brut.platzen": reihe("random/explode", 4, 1.5) + reihe("mob/slime/attack", 2, 0.5),
    "juggernaut.ambient": reihe("mob/ravager/idle", 4, 0.6),
    "juggernaut.hurt": reihe("mob/ravager/hurt", 4, 0.7),
    "juggernaut.death": reihe("mob/ravager/death", 3, 0.6),
    "juggernaut.step": reihe("mob/ravager/step", 5, 0.7),
    "juggernaut.brullen": reihe("mob/ravager/roar", 4, 0.65),
    "juggernaut.panzer": eins("random/anvil_land", 1.4, 0.8) + eins("random/anvil_break", 1.6, 0.6),
}


def schreiben():
    daten = {}
    for name, dateien in SOUNDS.items():
        art = name.split(".")[0]
        eintrag = {"subtitle": f"subtitles.deadzonemc.{name}", "sounds": dateien}
        if name in ("schreier.schrei", "juggernaut.brullen"):
            for d in dateien:
                d["attenuation_distance"] = 64
        daten[f"monster.{name}"] = eintrag
    with open(ZIEL, "w") as f:
        json.dump(daten, f, indent=1)
    print(len(daten), "Geräusche geschrieben")


def pruefen():
    manifest = json.load(urllib.request.urlopen("https://piston-meta.mojang.com/mc/game/version_manifest_v2.json"))
    v = next(x for x in manifest["versions"] if x["id"] == "1.21.1")
    version = json.load(urllib.request.urlopen(v["url"]))
    index = json.load(urllib.request.urlopen(version["assetIndex"]["url"]))["objects"]
    fehlt = []
    for name, dateien in SOUNDS.items():
        for d in dateien:
            pfad = "minecraft/sounds/" + d["name"].split(":", 1)[1] + ".ogg"
            if pfad not in index:
                fehlt.append(f"{name}: {d['name']}")
    if fehlt:
        # ähnliche vorhandene Namen als Hilfe
        tipps = set()
        for f in fehlt:
            ordner = f.split("minecraft:")[1].rsplit("/", 1)[0]
            tipps.update(k.replace("minecraft/sounds/", "")[:-4] for k in index if k.startswith(f"minecraft/sounds/{ordner}/"))
        text = "\n".join(fehlt) + "\n-- vorhanden in diesen Ordnern:\n" + ", ".join(sorted(tipps))[:2500]
        print("::error title=Fehlende Klangdateien::" + text.replace("\n", "%0A"))
        return 1
    print("::notice title=Klangdateien::alle vorhanden")
    return 0


if __name__ == "__main__":
    if "--pruefen" in sys.argv:
        sys.exit(pruefen())
    schreiben()
