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


def reihe(pfad, anzahl, pitch=1.0, volume=1.0, start=1, trenner=""):
    return [{"name": f"minecraft:{pfad}{trenner}{i}", "pitch": pitch, "volume": volume} for i in range(start, start + anzahl)]


def warden(was, anzahl, pitch=1.0, volume=1.0):
    return reihe("mob/warden/" + was, anzahl, pitch, volume, trenner="_")


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
    # ---- zweite Welle ----
    "gefallener_soldat.ambient": reihe("mob/zombie/say", 3, 0.6),
    "gefallener_soldat.hurt": reihe("mob/zombie/hurt", 2, 0.7) + eins("random/anvil_land", 1.8, 0.3),
    "gefallener_soldat.death": eins("mob/zombie/death", 0.6),
    "gefallener_soldat.step": reihe("mob/zombie/step", 5, 0.7) + reihe("block/chain/step", 4, 1.2, 0.4),
    "glutleib.ambient": reihe("mob/blaze/breathe", 4, 0.5),
    "glutleib.hurt": reihe("mob/blaze/hit", 4, 0.6),
    "glutleib.death": eins("mob/blaze/death", 0.5),
    "glutleib.step": reihe("mob/husk/step", 5, 0.6),
    "glutleib.glut": eins("fire/fire", 0.7) + eins("random/fizz", 0.5),
    "seuchenhund.ambient": reihe("mob/wolf/growl", 3, 0.7),
    "seuchenhund.hurt": reihe("mob/wolf/hurt", 3, 0.7),
    "seuchenhund.death": eins("mob/wolf/death", 0.6),
    "seuchenhund.step": reihe("mob/wolf/step", 5, 0.9),
    "seuchenhund.heulen": reihe("mob/wolf/howl", 2, 0.6),
    "wuehler.ambient": reihe("mob/silverfish/say", 4, 0.4),
    "wuehler.hurt": reihe("mob/silverfish/hit", 3, 0.4),
    "wuehler.death": eins("mob/spider/death", 0.4),
    "wuehler.graben": eins("mob/warden/dig", 1.2),
    "sauger.ambient": warden("sniff", 5, 1.3, 0.7),
    "sauger.hurt": reihe("mob/endermen/hit", 4, 0.7),
    "sauger.death": eins("mob/endermen/death", 0.5),
    "sauger.saugen": eins("mob/wolf/panting", 0.4) + reihe("mob/slime/attack", 2, 0.5),
    "seuchentraeger.ambient": reihe("mob/husk/idle", 3, 0.6),
    "seuchentraeger.hurt": reihe("mob/husk/hurt", 2, 0.6),
    "seuchentraeger.death": reihe("mob/husk/death", 2, 0.6),
    "seuchentraeger.spruehen": eins("random/fizz", 0.6) + reihe("mob/llama/spit", 2, 0.5),
    "ertrunkener.ambient": reihe("mob/drowned/idle", 5, 0.6),
    "ertrunkener.hurt": reihe("mob/drowned/hurt", 3, 0.6),
    "ertrunkener.death": reihe("mob/drowned/death", 2, 0.6),
    "ertrunkener.step": reihe("mob/drowned/step", 5, 0.7),
    "spaeherdrohne.ambient": eins("block/beacon/ambient", 1.8, 0.5),
    "spaeherdrohne.hurt": eins("random/anvil_land", 2.0, 0.4),
    "spaeherdrohne.death": reihe("random/explode", 4, 1.6, 0.8),
    "spaeherdrohne.alarm": reihe("block/sculk_shrieker/shriek", 5, 1.5),
    "lichtmotte.ambient": reihe("mob/bat/idle", 4, 0.6),
    "lichtmotte.hurt": reihe("mob/bat/hurt", 4, 0.6),
    "lichtmotte.death": eins("mob/bat/death", 0.5),
    "lichtmotte.flattern": reihe("mob/phantom/flap", 6, 1.4, 0.5),
    "scheintoter.atmen": eins("mob/wolf/panting", 0.35, 0.25),
    "scheintoter.hurt": reihe("mob/zombie/hurt", 2, 0.9),
    "scheintoter.death": eins("mob/zombie/death", 0.8),
    "scheintoter.packen": reihe("mob/zombie/say", 3, 1.2),
    # ---- Bosse ----
    "verschmolzener.ambient": reihe("mob/ravager/idle", 4, 0.45) + reihe("mob/zombie/say", 3, 0.5),
    "verschmolzener.hurt": reihe("mob/zombie/hurt", 2, 0.5),
    "verschmolzener.death": reihe("mob/ravager/death", 3, 0.4),
    "verschmolzener.step": reihe("mob/ravager/step", 5, 0.6),
    "verschmolzener.brullen": reihe("mob/ravager/roar", 4, 0.4),
    "verschmolzener.fusion": reihe("mob/slime/big", 4, 0.5) + reihe("mob/zombie/hurt", 2, 0.4),
    "nebelmutter.ambient": reihe("mob/vex/idle", 4, 0.3) + reihe("ambient/cave/cave", 13, 0.4, 0.6),
    "nebelmutter.hurt": reihe("mob/vex/hurt", 2, 0.35),
    "nebelmutter.death": eins("mob/ghast/death", 0.5),
    "nebelmutter.brullen": reihe("mob/ghast/scream", 5, 0.4),
    "strahlende.ambient": warden("heartbeat", 4, 0.8),
    "strahlende.hurt": reihe("mob/irongolem/hit", 4, 0.6),
    "strahlende.death": eins("mob/irongolem/death", 0.5),
    "strahlende.step": reihe("mob/irongolem/walk", 4, 0.6),
    "strahlende.brullen": warden("roar", 5, 0.7),
    "strahlende.puls": eins("block/beacon/power1", 0.6),
    "brutkoenigin.ambient": reihe("mob/spider/say", 4, 0.4),
    "brutkoenigin.hurt": reihe("mob/silverfish/hit", 3, 0.35),
    "brutkoenigin.death": eins("mob/spider/death", 0.3),
    "brutkoenigin.step": reihe("mob/spider/step", 4, 0.5),
    "brutkoenigin.brullen": reihe("mob/ravager/roar", 4, 1.2),
    "brutkoenigin.legen": reihe("mob/slime/big", 4, 0.4),
    "schlaechter.ambient": reihe("mob/hoglin/idle", 6, 0.6),
    "schlaechter.hurt": reihe("mob/ravager/hurt", 4, 0.9),
    "schlaechter.death": reihe("mob/ravager/death", 3, 0.8),
    "schlaechter.step": reihe("mob/ravager/step", 5, 0.9),
    "schlaechter.brullen": reihe("mob/hoglin/angry", 4, 0.6),
    "schlaechter.kette": reihe("block/chain/break", 4, 0.8) + reihe("block/chain/step", 6, 0.7),
    "kommandant.ambient": reihe("mob/pillager/idle", 4, 0.7),
    "kommandant.hurt": reihe("mob/irongolem/hit", 4, 1.2),
    "kommandant.death": eins("mob/irongolem/death", 0.9),
    "kommandant.step": reihe("mob/irongolem/walk", 4, 0.9),
    "kommandant.brullen": eins("event/raid/raidhorn_01", 1.0),
    "hirte.ambient": reihe("mob/evocation_illager/idle", 4, 0.6),
    "hirte.hurt": reihe("mob/evocation_illager/hurt", 2, 0.6),
    "hirte.death": reihe("mob/evocation_illager/death", 2, 0.6),
    "hirte.glocke": eins("block/bell/bell_use01", 0.8) + eins("block/bell/bell_use02", 0.8),
    "schlund.ambient": warden("ambient", 5, 0.5),
    "schlund.hurt": warden("hurt", 4, 0.6),
    "schlund.death": warden("death", 2, 0.5),
    "schlund.brullen": warden("roar", 5, 0.5),
    "schlund.graben": eins("mob/warden/dig", 0.7),
    "schlund.auftauchen": eins("mob/warden/emerge", 0.8),
    "pilzvater.ambient": reihe("mob/ravager/idle", 4, 0.4),
    "pilzvater.hurt": reihe("mob/slime/big", 4, 0.5),
    "pilzvater.death": reihe("mob/ravager/death", 3, 0.35),
    "pilzvater.step": reihe("mob/irongolem/walk", 4, 0.5),
    "pilzvater.sporen": eins("random/fizz", 0.4) + reihe("mob/slime/big", 4, 0.3),
    "erfrorene.ambient": reihe("mob/stray/idle", 4, 0.5),
    "erfrorene.hurt": reihe("mob/stray/hurt", 4, 0.6),
    "erfrorene.death": reihe("mob/stray/death", 2, 0.5),
    "erfrorene.step": reihe("mob/stray/step", 4, 0.6),
    "erfrorene.brullen": warden("roar", 5, 1.3),
    "erfrorene.atem": reihe("mob/blaze/breathe", 4, 1.6) + reihe("random/glass", 3, 0.7),
}


def schreiben():
    daten = {}
    for name, dateien in SOUNDS.items():
        art = name.split(".")[0]
        eintrag = {"subtitle": f"subtitles.deadzonemc.{name}", "sounds": dateien}
        if name in ("schreier.schrei", "juggernaut.brullen", "spaeherdrohne.alarm", "hirte.glocke") or name.endswith(".brullen"):
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
            pfad = f.split("minecraft:")[1].rstrip("0123456789_")
            tipps.update(k.replace("minecraft/sounds/", "")[:-4] for k in index if k.startswith(f"minecraft/sounds/{pfad}"))
            if not any(t.startswith(pfad) for t in tipps):
                ordner = pfad.rsplit("/", 1)[0]
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
