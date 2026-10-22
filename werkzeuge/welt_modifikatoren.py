"""Biom-Modifikatoren: Deadzone-Deko und -Gefahren in den normalen Biomen (Vanilla + Biomes O' Plenty),
mehr Deadzone-Monster, weniger Vanilla-Monster.  python3 werkzeuge/welt_modifikatoren.py
"""
import json
import os

D = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "data", "deadzonemc")


def optional(*ids):
    return [{"id": i, "required": False} if ":" in i and (not i.startswith("#") or i.startswith("#c:")) else i for i in ids]


TAGS = {
    # Verseuchtes Land: Strahlung in der Luft, strahlende Fässer
    "verseucht": optional("minecraft:badlands", "minecraft:eroded_badlands", "minecraft:wooded_badlands",
                          "biomesoplenty:wasteland", "biomesoplenty:wasteland_steppe", "biomesoplenty:volcanic_plains",
                          "biomesoplenty:dead_forest", "biomesoplenty:scrubland", "biomesoplenty:dryland",
                          "regions_unexplored:ashen_woodland", "regions_unexplored:outback"),
    # Giftgas: ohne Gasmaske mit Filter vergiftet man sich
    "giftgas": optional("minecraft:swamp", "biomesoplenty:bog", "biomesoplenty:marsh", "biomesoplenty:wetland",
                        "biomesoplenty:fungal_jungle"),
    # Abgestorbene Wälder
    "totwald": optional("biomesoplenty:dead_forest", "biomesoplenty:wasteland", "biomesoplenty:bog",
                        "biomesoplenty:muskeg", "biomesoplenty:wasteland_steppe", "regions_unexplored:ashen_woodland"),
    # Lebensräume für Herden und Spawns
    # Verseucht oder Totwald – jeder Biom nur einmal, sonst entsteht ein Zyklus in der Feature-Reihenfolge
    "schreinland": ["#deadzonemc:verseucht", "#deadzonemc:totwald"],
    "land": optional("#minecraft:is_forest", "#minecraft:is_taiga", "#minecraft:is_savanna", "#c:is_plains",
                     "#c:is_snowy", "minecraft:plains", "minecraft:meadow"),
    "wasser": optional("#minecraft:is_river", "#minecraft:is_beach", "#c:is_swamp", "minecraft:swamp",
                       "minecraft:mangrove_swamp"),
    "wueste": optional("#c:is_desert", "#minecraft:is_badlands", "minecraft:desert", "regions_unexplored:joshua_desert",
                       "regions_unexplored:saguaro_desert", "regions_unexplored:salt_flats", "regions_unexplored:outback"),
    "schnee": optional("#c:is_snowy", "minecraft:snowy_plains", "minecraft:snowy_taiga", "minecraft:ice_spikes",
                       "minecraft:frozen_peaks", "minecraft:snowy_slopes", "minecraft:grove",
                       "regions_unexplored:frozen_tundra", "regions_unexplored:icy_heights", "regions_unexplored:frozen_pine_taiga"),
    # Lebensräume der Monster (de.deadzonemc.monster.Lebensraum)
    "sumpf": optional("#c:is_swamp", "minecraft:swamp", "minecraft:mangrove_swamp", "biomesoplenty:bog",
                      "biomesoplenty:marsh", "biomesoplenty:wetland", "biomesoplenty:fungal_jungle",
                      "regions_unexplored:fen", "regions_unexplored:bayou", "regions_unexplored:marsh"),
    "grosse_gewaesser": optional("#minecraft:is_river", "#minecraft:is_ocean", "#minecraft:is_deep_ocean"),
    "waldland": optional("#minecraft:is_forest", "#minecraft:is_taiga", "#minecraft:is_jungle", "#c:is_forest",
                         "minecraft:dark_forest", "biomesoplenty:mystic_grove", "biomesoplenty:ominous_woods"),
    "sumpfwald": optional("#c:is_swamp", "minecraft:swamp", "minecraft:mangrove_swamp", "minecraft:dark_forest",
                          "biomesoplenty:bog", "biomesoplenty:marsh", "biomesoplenty:wetland",
                          "biomesoplenty:fungal_jungle", "biomesoplenty:mystic_grove", "biomesoplenty:ominous_woods"),
}

MODIFIKATOREN = {
    # Plakate und Kreidezeichen an Hauswänden – nach allen Bauten, damit die Wände schon stehen
    "deko_waende": {"type": "neoforge:add_features", "biomes": "#minecraft:is_overworld",
                    "features": ["deadzonemc:wanddeko"], "step": "top_layer_modification"},
    "deko_ueberall": {"type": "neoforge:add_features", "biomes": "#minecraft:is_overworld",
                      "features": ["deadzonemc:autowrack_selten", "deadzonemc:schutt_selten",
                                   "deadzonemc:strahlenfaesser_selten", "deadzonemc:versteck",
                                   "deadzonemc:anomalie_selten"],
                      "step": "vegetal_decoration"},
    "siedlung_lager": {"type": "neoforge:add_features", "biomes": "#deadzonemc:land", "features": ["deadzonemc:siedlung_lager"], "step": "surface_structures"},
    "siedlung_schrein": {"type": "neoforge:add_features", "biomes": "#deadzonemc:schreinland", "features": ["deadzonemc:siedlung_schrein"], "step": "surface_structures"},
    "siedlung_baumhaus": {"type": "neoforge:add_features", "biomes": "#minecraft:is_forest", "features": ["deadzonemc:siedlung_baumhaus"], "step": "surface_structures"},
    "siedlung_nomaden": {"type": "neoforge:add_features", "biomes": "#deadzonemc:wueste", "features": ["deadzonemc:siedlung_nomaden"], "step": "surface_structures"},
    "siedlung_kuppel": {"type": "neoforge:add_features", "biomes": "#minecraft:is_ocean", "features": ["deadzonemc:siedlung_kuppel"], "step": "surface_structures"},
    "siedlung_horst": {"type": "neoforge:add_features", "biomes": "#minecraft:is_mountain", "features": ["deadzonemc:siedlung_horst"], "step": "surface_structures"},
    "pois": {"type": "neoforge:add_features", "biomes": "#minecraft:is_overworld",
             "features": ["deadzonemc:absturz_hubschrauber", "deadzonemc:absturz_flugzeug"],
             "step": "surface_structures"},
    "pois_land": {"type": "neoforge:add_features", "biomes": "#deadzonemc:land",
             "features": ["deadzonemc:panzer", "deadzonemc:konvoi", "deadzonemc:evakuierung", "deadzonemc:raststaette", "deadzonemc:funkturm", "deadzonemc:checkpoint", "deadzonemc:stauseedorf", "deadzonemc:bunkerlabor"],
             "step": "surface_structures"},
    "pois_kueste": {"type": "neoforge:add_features", "biomes": "#minecraft:is_beach",
                    "features": ["deadzonemc:frachter"], "step": "surface_structures"},
    "deko_verseucht": {"type": "neoforge:add_features", "biomes": "#deadzonemc:verseucht",
                       "features": ["deadzonemc:strahlenfaesser", "deadzonemc:strommast", "deadzonemc:anomalie"],
                       "step": "vegetal_decoration"},
    "deko_totwald": {"type": "neoforge:add_features", "biomes": "#deadzonemc:totwald",
                     "features": ["deadzonemc:tote_baeume_selten"], "step": "vegetal_decoration"},
    "metro": {"type": "neoforge:add_features", "biomes": "#minecraft:is_overworld",
              "features": ["deadzonemc:metro"], "step": "underground_decoration"},
    # Weicher Übergang am Stadtrand: vor Bäumen und Bauten
    "stadtrand": {"type": "neoforge:add_features", "biomes": "#minecraft:is_overworld",
                  "features": ["deadzonemc:stadtrand"], "step": "raw_generation"},
    "weniger_vanilla": {"type": "neoforge:remove_spawns", "biomes": "#minecraft:is_overworld",
                        "entity_types": ["minecraft:creeper", "minecraft:skeleton", "minecraft:enderman",
                                         "minecraft:witch", "minecraft:spider", "minecraft:slime"]},
    # Jedes Monster dort, wo es hinpasst (Rückmeldung 22.10.). Die Biom-Listen sind grob; die genaue Gegend
    # (Stadt, Höhle, tiefes Wasser) prüft de.deadzonemc.monster.Lebensraum bei jedem Spawn.
    "monster": {"type": "neoforge:add_spawns", "biomes": "#minecraft:is_overworld", "spawners": [
        {"type": "deadzonemc:wanderer", "weight": 40, "minCount": 1, "maxCount": 3},
        {"type": "deadzonemc:nebelwesen", "weight": 30, "minCount": 1, "maxCount": 2},
        {"type": "deadzonemc:der_stille", "weight": 20, "minCount": 1, "maxCount": 1},
        {"type": "deadzonemc:imitator", "weight": 14, "minCount": 1, "maxCount": 1},
        {"type": "deadzonemc:kriecher", "weight": 18, "minCount": 1, "maxCount": 2},
        {"type": "deadzonemc:lauerer", "weight": 16, "minCount": 1, "maxCount": 1},
        {"type": "deadzonemc:wuehler", "weight": 30, "minCount": 1, "maxCount": 1},
        {"type": "deadzonemc:brut", "weight": 10, "minCount": 1, "maxCount": 1},
        {"type": "deadzonemc:schwarm", "weight": 8, "minCount": 4, "maxCount": 7},
        # nur in Städten (Lebensraum prüft das)
        {"type": "deadzonemc:schreier", "weight": 18, "minCount": 1, "maxCount": 1},
        {"type": "deadzonemc:gefallener_soldat", "weight": 18, "minCount": 1, "maxCount": 2},
        {"type": "deadzonemc:scheintoter", "weight": 16, "minCount": 1, "maxCount": 1},
        {"type": "deadzonemc:spaeherdrohne", "weight": 14, "minCount": 1, "maxCount": 1},
        {"type": "deadzonemc:juggernaut", "weight": 4, "minCount": 1, "maxCount": 1},
    ]},
    # Dimensionswesen: Herz der Zone (Nether) und Orbitalstation (End)
    "monster_nether": {"type": "neoforge:add_spawns", "biomes": "#minecraft:is_nether", "spawners": [{"type": "deadzonemc:lenker", "weight": 12, "minCount": 1, "maxCount": 1}, {"type": "deadzonemc:spuk", "weight": 12, "minCount": 1, "maxCount": 1}, {"type": "deadzonemc:truegruede", "weight": 12, "minCount": 1, "maxCount": 2}, {"type": "deadzonemc:entreisser", "weight": 12, "minCount": 1, "maxCount": 1}, {"type": "deadzonemc:chimaere", "weight": 12, "minCount": 1, "maxCount": 1}, {"type": "deadzonemc:glasgaenger", "weight": 12, "minCount": 1, "maxCount": 1}, {"type": "deadzonemc:schwebender", "weight": 12, "minCount": 1, "maxCount": 1}, {"type": "deadzonemc:entlader", "weight": 12, "minCount": 1, "maxCount": 1}, {"type": "deadzonemc:echo", "weight": 12, "minCount": 1, "maxCount": 1}, {"type": "deadzonemc:zonenhirsch", "weight": 6, "minCount": 1, "maxCount": 1}]},
    "monster_end": {"type": "neoforge:add_spawns", "biomes": "#minecraft:is_end", "spawners": [{"type": "deadzonemc:leerlaeufer", "weight": 10, "minCount": 1, "maxCount": 1}, {"type": "deadzonemc:wartungsspinne", "weight": 10, "minCount": 1, "maxCount": 1}, {"type": "deadzonemc:wachkugel", "weight": 10, "minCount": 1, "maxCount": 1}, {"type": "deadzonemc:versuchsobjekt", "weight": 10, "minCount": 1, "maxCount": 1}, {"type": "deadzonemc:konglomerat", "weight": 10, "minCount": 1, "maxCount": 1}, {"type": "deadzonemc:kryoschlaefer", "weight": 10, "minCount": 1, "maxCount": 1}, {"type": "deadzonemc:leerenaal", "weight": 10, "minCount": 1, "maxCount": 1}, {"type": "deadzonemc:klammerer", "weight": 10, "minCount": 1, "maxCount": 3}, {"type": "deadzonemc:hologeist", "weight": 10, "minCount": 1, "maxCount": 1}, {"type": "deadzonemc:forscher", "weight": 10, "minCount": 1, "maxCount": 1}]},
    "monster_land": {"type": "neoforge:add_spawns", "biomes": "#deadzonemc:land", "spawners": [
        {"type": "deadzonemc:seuchenhund", "weight": 24, "minCount": 3, "maxCount": 4},
        {"type": "deadzonemc:sauger", "weight": 16, "minCount": 1, "maxCount": 1},
        {"type": "deadzonemc:tarnfresser", "weight": 18, "minCount": 1, "maxCount": 1},
        {"type": "deadzonemc:lichtmotte", "weight": 16, "minCount": 1, "maxCount": 2},
    ]},
    "monster_sumpf": {"type": "neoforge:add_spawns", "biomes": "#deadzonemc:sumpf", "spawners": [
        {"type": "deadzonemc:schwarm", "weight": 20, "minCount": 5, "maxCount": 8},
        {"type": "deadzonemc:brut", "weight": 20, "minCount": 1, "maxCount": 1},
        {"type": "deadzonemc:tarnfresser", "weight": 18, "minCount": 1, "maxCount": 1},
        {"type": "deadzonemc:sauger", "weight": 16, "minCount": 1, "maxCount": 1},
        {"type": "deadzonemc:lichtmotte", "weight": 18, "minCount": 1, "maxCount": 2},
    ]},
    "monster_wasser": {"type": "neoforge:add_spawns", "biomes": "#deadzonemc:grosse_gewaesser",
        "spawners": [{"type": "deadzonemc:ertrunkener", "weight": 40, "minCount": 1, "maxCount": 2}]},
    "monster_verseucht": {"type": "neoforge:add_spawns", "biomes": "#deadzonemc:verseucht", "spawners": [
        {"type": "deadzonemc:seuchentraeger", "weight": 36, "minCount": 1, "maxCount": 2},
        {"type": "deadzonemc:glutleib", "weight": 24, "minCount": 1, "maxCount": 1},
        {"type": "deadzonemc:juggernaut", "weight": 6, "minCount": 1, "maxCount": 1},
    ]},
    "monster_wueste": {"type": "neoforge:add_spawns", "biomes": "#deadzonemc:wueste", "spawners": [
        {"type": "deadzonemc:glutleib", "weight": 30, "minCount": 1, "maxCount": 2},
        {"type": "deadzonemc:seuchenhund", "weight": 24, "minCount": 3, "maxCount": 4},
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
