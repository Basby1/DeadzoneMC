"""Biom-Modifikatoren: Deadzone-Deko und -Gefahren in den normalen Biomen (Vanilla + Biomes O' Plenty),
mehr Deadzone-Monster, weniger Vanilla-Monster.  python3 werkzeuge/welt_modifikatoren.py
"""
import json
import os

D = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "data", "deadzonemc")


def optional(*ids):
    return [{"id": i, "required": False} if ":" in i and not i.startswith("#") else i for i in ids]


TAGS = {
    # Verseuchtes Land: Strahlung in der Luft, strahlende Fässer
    "verseucht": optional("minecraft:badlands", "minecraft:eroded_badlands", "minecraft:wooded_badlands",
                          "biomesoplenty:wasteland", "biomesoplenty:wasteland_steppe", "biomesoplenty:volcanic_plains",
                          "biomesoplenty:dead_forest", "biomesoplenty:scrubland", "biomesoplenty:dryland"),
    # Giftgas: ohne Gasmaske mit Filter vergiftet man sich
    "giftgas": optional("minecraft:swamp", "biomesoplenty:bog", "biomesoplenty:marsh", "biomesoplenty:wetland",
                        "biomesoplenty:fungal_jungle"),
    # Abgestorbene Wälder
    "totwald": optional("biomesoplenty:dead_forest", "biomesoplenty:wasteland", "biomesoplenty:bog",
                        "biomesoplenty:muskeg", "biomesoplenty:wasteland_steppe"),
}

MODIFIKATOREN = {
    "deko_ueberall": {"type": "neoforge:add_features", "biomes": "#minecraft:is_overworld",
                      "features": ["deadzonemc:autowrack_selten", "deadzonemc:schutt_selten",
                                   "deadzonemc:strahlenfaesser_selten", "deadzonemc:versteck"],
                      "step": "vegetal_decoration"},
    "deko_verseucht": {"type": "neoforge:add_features", "biomes": "#deadzonemc:verseucht",
                       "features": ["deadzonemc:strahlenfaesser", "deadzonemc:strommast"], "step": "vegetal_decoration"},
    "deko_totwald": {"type": "neoforge:add_features", "biomes": "#deadzonemc:totwald",
                     "features": ["deadzonemc:tote_baeume_selten"], "step": "vegetal_decoration"},
    "weniger_vanilla": {"type": "neoforge:remove_spawns", "biomes": "#minecraft:is_overworld",
                        "entity_types": ["minecraft:creeper", "minecraft:skeleton", "minecraft:enderman",
                                         "minecraft:witch", "minecraft:spider", "minecraft:slime"]},
    "monster": {"type": "neoforge:add_spawns", "biomes": "#minecraft:is_overworld", "spawners": [
        {"type": "deadzonemc:wanderer", "weight": 220, "minCount": 3, "maxCount": 6},
        {"type": "deadzonemc:schreier", "weight": 30, "minCount": 1, "maxCount": 1},
        {"type": "deadzonemc:lauerer", "weight": 40, "minCount": 1, "maxCount": 1},
        {"type": "deadzonemc:kriecher", "weight": 35, "minCount": 1, "maxCount": 2},
        {"type": "deadzonemc:der_stille", "weight": 8, "minCount": 1, "maxCount": 1},
        {"type": "deadzonemc:imitator", "weight": 10, "minCount": 1, "maxCount": 1},
        {"type": "deadzonemc:nebelwesen", "weight": 25, "minCount": 1, "maxCount": 2},
        {"type": "deadzonemc:schwarm", "weight": 20, "minCount": 6, "maxCount": 12},
        {"type": "deadzonemc:brut", "weight": 20, "minCount": 1, "maxCount": 1},
        {"type": "deadzonemc:juggernaut", "weight": 5, "minCount": 1, "maxCount": 1},
    ]},
}


def main():
    t = os.path.join(D, "tags", "worldgen", "biome")
    os.makedirs(t, exist_ok=True)
    for n, werte in TAGS.items():
        with open(os.path.join(t, f"{n}.json"), "w") as f:
            json.dump({"values": werte}, f, indent=1)
    m = os.path.join(D, "neoforge", "biome_modifier")
    os.makedirs(m, exist_ok=True)
    for f in os.listdir(m):
        os.remove(os.path.join(m, f))
    for n, d in MODIFIKATOREN.items():
        with open(os.path.join(m, f"{n}.json"), "w") as f:
            json.dump(d, f, indent=1)
    print(len(TAGS), "Tags,", len(MODIFIKATOREN), "Modifikatoren geschrieben")


if __name__ == "__main__":
    main()
