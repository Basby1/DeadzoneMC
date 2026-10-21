"""Erzeugt die Daten der Weltgenerierung von Deadzone MC.

Aufruf aus dem Projektordner:  python3 werkzeuge/welt.py

Geschrieben werden Gebiete (Biome), Deko-Features, der Welttyp, Strukturen und Biom-Tags.
Alle Gebiete teilen sich dieselbe Grundliste an Features in derselben Reihenfolge – Minecraft
stürzt sonst beim Erzeugen der Welt ab ("Feature order cycle").
"""

import json
import os

MOD = "deadzonemc"
WURZEL = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "data")


def schreiben(pfad, daten):
    ziel = os.path.join(WURZEL, pfad)
    os.makedirs(os.path.dirname(ziel), exist_ok=True)
    with open(ziel, "w", encoding="utf-8") as f:
        json.dump(daten, f, indent=2, ensure_ascii=False)
        f.write("\n")


def farbe(hexwert):
    return int(hexwert.lstrip("#"), 16)


# ---------------------------------------------------------------------------------------------
# Eigene Deko-Features (configured + placed)
# ---------------------------------------------------------------------------------------------

def zustand(name, **eigenschaften):
    z = {"Name": name}
    if eigenschaften:
        z["Properties"] = {k: str(v).lower() for k, v in eigenschaften.items()}
    return z


def gewichtet(*paare):
    return {
        "type": "minecraft:weighted_state_provider",
        "entries": [{"data": zustand(n), "weight": w} for n, w in paare],
    }


CONFIGURED = {
    # In Java gebaute Features
    "toter_baum": {"type": f"{MOD}:toter_baum", "config": {}},
    "autowrack": {"type": f"{MOD}:autowrack", "config": {}},
    "wanddeko": {"type": f"{MOD}:wanddeko", "config": {}},
    "absturz_hubschrauber": {"type": f"{MOD}:absturz_hubschrauber", "config": {}},
    "absturz_flugzeug": {"type": f"{MOD}:absturz_flugzeug", "config": {}},
    "frachter": {"type": f"{MOD}:frachter", "config": {}},
    "panzer": {"type": f"{MOD}:panzer", "config": {}},
    "raststaette": {"type": f"{MOD}:raststaette", "config": {}},
    "funkturm": {"type": f"{MOD}:funkturm", "config": {}},
    "konvoi": {"type": f"{MOD}:konvoi", "config": {}},
    "evakuierung": {"type": f"{MOD}:evakuierung", "config": {}},
    **{f"siedlung_{a}": {"type": f"{MOD}:siedlung_{a}", "config": {}} for a in ("lager", "schrein", "baumhaus", "nomaden", "kuppel", "horst")},
    "strahlenfaesser": {"type": f"{MOD}:strahlenfaesser", "config": {}},
    "versteck": {"type": f"{MOD}:versteck", "config": {}},
    "schutt": {"type": f"{MOD}:schutt", "config": {}},
    "strommast": {"type": f"{MOD}:strommast", "config": {}},
    "anomalie": {"type": f"{MOD}:anomalie", "config": {}},
    "metro": {"type": f"{MOD}:metro", "config": {}},
    "stadtrand": {"type": f"{MOD}:stadtrand", "config": {}},
    # Mit Minecraft-Mitteln: Flecken toter Erde auf dem Boden
    "boden_tot": {
        "type": "minecraft:disk",
        "config": {
            "half_height": 1,
            "radius": {"type": "minecraft:uniform", "min_inclusive": 2, "max_inclusive": 5},
            "state_provider": {
                "fallback": gewichtet(("minecraft:coarse_dirt", 5), ("minecraft:dirt", 3),
                                      ("minecraft:gravel", 2), ("minecraft:podzol", 1)),
                "rules": [],
            },
            "target": {"type": "minecraft:matching_blocks", "blocks": ["minecraft:grass_block"]},
        },
    },
    "boden_oede": {
        "type": "minecraft:disk",
        "config": {
            "half_height": 1,
            "radius": {"type": "minecraft:uniform", "min_inclusive": 3, "max_inclusive": 7},
            "state_provider": {
                "fallback": gewichtet(("minecraft:coarse_dirt", 6), ("minecraft:gravel", 3),
                                      ("minecraft:red_sand", 1), ("minecraft:terracotta", 1)),
                "rules": [],
            },
            "target": {"type": "minecraft:matching_blocks", "blocks": ["minecraft:grass_block", "minecraft:dirt"]},
        },
    },
    "boden_moor": {
        "type": "minecraft:disk",
        "config": {
            "half_height": 1,
            "radius": {"type": "minecraft:uniform", "min_inclusive": 2, "max_inclusive": 6},
            "state_provider": {
                "fallback": gewichtet(("minecraft:mud", 6), ("minecraft:muddy_mangrove_roots", 1),
                                      ("minecraft:podzol", 2)),
                "rules": [],
            },
            "target": {"type": "minecraft:matching_blocks", "blocks": ["minecraft:grass_block", "minecraft:dirt"]},
        },
    },
}


def an_der_oberflaeche(anzahl=None, seltenheit=None, wasser_ok=False):
    platz = []
    if anzahl is not None:
        platz.append({"type": "minecraft:count", "count": anzahl})
    if seltenheit is not None:
        platz.append({"type": "minecraft:rarity_filter", "chance": seltenheit})
    platz.append({"type": "minecraft:in_square"})
    if not wasser_ok:
        platz.append({"type": "minecraft:surface_water_depth_filter", "max_water_depth": 0})
    platz.append({"type": "minecraft:heightmap", "heightmap": "WORLD_SURFACE_WG"})
    platz.append({"type": "minecraft:biome"})
    return platz


def zwischen(a, b):
    return {"type": "minecraft:uniform", "min_inclusive": a, "max_inclusive": b}


PLACED = {
    "tote_baeume_dicht": ("toter_baum", an_der_oberflaeche(anzahl=zwischen(3, 7))),
    "tote_baeume_moor": ("toter_baum", an_der_oberflaeche(anzahl=zwischen(1, 3))),
    "tote_baeume_selten": ("toter_baum", an_der_oberflaeche(seltenheit=3)),
    "autowrack_selten": ("autowrack", an_der_oberflaeche(seltenheit=24)),
    "wanddeko": ("wanddeko", [{"type": "minecraft:biome"}]),
    # POIs: selten, aber mit Rauchsäule weithin sichtbar
    "absturz_hubschrauber": ("absturz_hubschrauber", an_der_oberflaeche(seltenheit=140)),
    "absturz_flugzeug": ("absturz_flugzeug", an_der_oberflaeche(seltenheit=260)),
    "frachter": ("frachter", an_der_oberflaeche(seltenheit=30, wasser_ok=True)),
    "panzer": ("panzer", an_der_oberflaeche(seltenheit=90)),
    "raststaette": ("raststaette", an_der_oberflaeche(seltenheit=120)),
    "funkturm": ("funkturm", an_der_oberflaeche(seltenheit=150)),
    "konvoi": ("konvoi", an_der_oberflaeche(seltenheit=220)),
    "evakuierung": ("evakuierung", an_der_oberflaeche(seltenheit=160)),
    # Siedlungen der Fraktionen
    "siedlung_lager": ("siedlung_lager", an_der_oberflaeche(seltenheit=110)),
    "siedlung_schrein": ("siedlung_schrein", an_der_oberflaeche(seltenheit=40)),
    "siedlung_baumhaus": ("siedlung_baumhaus", an_der_oberflaeche(seltenheit=55)),
    "siedlung_nomaden": ("siedlung_nomaden", an_der_oberflaeche(seltenheit=45)),
    "siedlung_kuppel": ("siedlung_kuppel", an_der_oberflaeche(seltenheit=110, wasser_ok=True)),
    "siedlung_horst": ("siedlung_horst", an_der_oberflaeche(seltenheit=35)),
    "strahlenfaesser": ("strahlenfaesser", an_der_oberflaeche(seltenheit=5)),
    "versteck": ("versteck", [{"type": "minecraft:biome"}]),
    "strahlenfaesser_selten": ("strahlenfaesser", an_der_oberflaeche(seltenheit=40)),
    "schutt": ("schutt", an_der_oberflaeche(seltenheit=3)),
    "schutt_selten": ("schutt", an_der_oberflaeche(seltenheit=8)),
    "strommast": ("strommast", an_der_oberflaeche(seltenheit=10)),
    "anomalie": ("anomalie", an_der_oberflaeche(seltenheit=9)),
    "metro": ("metro", [{"type": "minecraft:biome"}]),
    "stadtrand": ("stadtrand", [{"type": "minecraft:biome"}]),
    "anomalie_selten": ("anomalie", an_der_oberflaeche(seltenheit=70)),
    "boden_tot": ("boden_tot", an_der_oberflaeche(anzahl=zwischen(1, 3))),
    "boden_oede": ("boden_oede", an_der_oberflaeche(anzahl=zwischen(2, 4))),
    "boden_moor": ("boden_moor", an_der_oberflaeche(anzahl=zwischen(2, 4), wasser_ok=True)),
}

# ---------------------------------------------------------------------------------------------
# Gebiete
# ---------------------------------------------------------------------------------------------

UNTERGRUND = [
    [],
    ["minecraft:lake_lava_underground", "minecraft:lake_lava_surface"],
    ["minecraft:amethyst_geode"],
    ["minecraft:monster_room", "minecraft:monster_room_deep"],
    [],
    [],
    ["minecraft:ore_dirt", "minecraft:ore_gravel", "minecraft:ore_granite_upper", "minecraft:ore_granite_lower",
     "minecraft:ore_diorite_upper", "minecraft:ore_diorite_lower", "minecraft:ore_andesite_upper",
     "minecraft:ore_andesite_lower", "minecraft:ore_tuff", "minecraft:ore_coal_upper", "minecraft:ore_coal_lower",
     "minecraft:ore_iron_upper", "minecraft:ore_iron_middle", "minecraft:ore_iron_small", "minecraft:ore_gold",
     "minecraft:ore_gold_lower", "minecraft:ore_redstone", "minecraft:ore_redstone_lower", "minecraft:ore_diamond",
     "minecraft:ore_diamond_medium", "minecraft:ore_diamond_large", "minecraft:ore_diamond_buried",
     "minecraft:ore_lapis", "minecraft:ore_lapis_buried", "minecraft:ore_copper", "minecraft:underwater_magma",
     "minecraft:disk_sand", "minecraft:disk_clay", "minecraft:disk_gravel"],
    [],
    ["minecraft:spring_water", "minecraft:spring_lava"],
    None,  # Pflanzen und Deko: je Gebiet unterschiedlich
    ["minecraft:freeze_top_layer"],
]

# Gesamtreihenfolge aller Pflanzen-/Deko-Features. Jedes Gebiet nimmt eine Auswahl daraus,
# immer in dieser Reihenfolge.
DEKO_REIHENFOLGE = [
    f"{MOD}:boden_tot", f"{MOD}:boden_oede", f"{MOD}:boden_moor",
    "minecraft:glow_lichen",
    f"{MOD}:schutt", f"{MOD}:schutt_selten",
    f"{MOD}:strommast",
    f"{MOD}:autowrack_selten",
    f"{MOD}:strahlenfaesser", f"{MOD}:strahlenfaesser_selten", f"{MOD}:versteck",
    f"{MOD}:tote_baeume_dicht", f"{MOD}:tote_baeume_moor", f"{MOD}:tote_baeume_selten",
    "minecraft:patch_grass_plain", "minecraft:patch_grass_forest", "minecraft:patch_grass_badlands",
    "minecraft:patch_dead_bush", "minecraft:patch_dead_bush_2", "minecraft:patch_dead_bush_badlands",
    "minecraft:brown_mushroom_normal", "minecraft:red_mushroom_normal",
    "minecraft:brown_mushroom_swamp", "minecraft:red_mushroom_swamp",
    "minecraft:patch_sugar_cane_swamp", "minecraft:patch_waterlily",
    "minecraft:seagrass_swamp", "minecraft:seagrass_cold", "minecraft:seagrass_simple", "minecraft:kelp_cold",
]


def deko(*namen):
    gewaehlt = set(namen)
    unbekannt = gewaehlt - set(DEKO_REIHENFOLGE)
    if unbekannt:
        raise ValueError(f"nicht in DEKO_REIHENFOLGE: {unbekannt}")
    return [n for n in DEKO_REIHENFOLGE if n in gewaehlt]


def spawn(typ, gewicht, minimum, maximum):
    return {"type": typ, "weight": gewicht, "minCount": minimum, "maxCount": maximum}


MONSTER_GRUND = [
    spawn("minecraft:zombie", 100, 2, 4),
    spawn("minecraft:zombie_villager", 10, 1, 1),
    spawn("minecraft:skeleton", 70, 1, 3),
    spawn("minecraft:spider", 80, 1, 3),
    spawn("minecraft:enderman", 8, 1, 1),
]


def gebiet(*, temperatur, niederschlag, gras, laub, wasser, wasser_nebel, himmel, deko_liste,
           monster, tiere=(), wassertiere=(), stimmung_takt=4000):
    features = [list(stufe) if stufe is not None else deko_liste for stufe in UNTERGRUND]
    effekte = {
        "fog_color": farbe("#8a8c90"),
        "sky_color": farbe(himmel),
        "water_color": farbe(wasser),
        "water_fog_color": farbe(wasser_nebel),
        "grass_color": farbe(gras),
        "foliage_color": farbe(laub),
        "mood_sound": {
            "block_search_extent": 8,
            "offset": 2.0,
            "sound": "minecraft:ambient.cave",
            "tick_delay": stimmung_takt,
        },
    }
    return {
        "has_precipitation": True,
        "temperature": temperatur,
        "downfall": niederschlag,
        "effects": effekte,
        "carvers": {"air": ["minecraft:cave", "minecraft:cave_extra_underground", "minecraft:canyon"]},
        "features": features,
        "spawn_costs": {},
        "spawners": {
            "monster": list(monster),
            "creature": list(tiere),
            "ambient": [spawn("minecraft:bat", 10, 8, 8)],
            "axolotls": [],
            "misc": [],
            "underground_water_creature": [spawn("minecraft:glow_squid", 10, 4, 6)],
            "water_ambient": [],
            "water_creature": list(wassertiere),
        },
    }


GEBIETE = {
    "vorstadt": gebiet(
        temperatur=0.6, niederschlag=0.5, gras="#7d7c58", laub="#6b6a4c",
        wasser="#5b6b6e", wasser_nebel="#1f2628", himmel="#7b7f86",
        deko_liste=deko(f"{MOD}:boden_tot", "minecraft:glow_lichen", f"{MOD}:schutt_selten",
                        f"{MOD}:strommast", f"{MOD}:strahlenfaesser_selten", f"{MOD}:versteck", f"{MOD}:tote_baeume_selten",
                        "minecraft:patch_grass_plain", "minecraft:patch_dead_bush"),
        monster=MONSTER_GRUND,
        tiere=[spawn("minecraft:rabbit", 6, 1, 2), spawn("minecraft:chicken", 4, 1, 2)],
    ),
    "toter_wald": gebiet(
        temperatur=0.5, niederschlag=0.6, gras="#5f5b44", laub="#4b4737",
        wasser="#4d5a58", wasser_nebel="#1a2120", himmel="#70737a",
        deko_liste=deko(f"{MOD}:boden_tot", "minecraft:glow_lichen", f"{MOD}:schutt_selten",
                        f"{MOD}:versteck", f"{MOD}:tote_baeume_dicht", "minecraft:patch_grass_forest", "minecraft:patch_dead_bush_2",
                        "minecraft:brown_mushroom_normal", "minecraft:red_mushroom_normal"),
        monster=MONSTER_GRUND,
        tiere=[spawn("minecraft:wolf", 4, 2, 4), spawn("minecraft:rabbit", 4, 1, 2)],
        stimmung_takt=3000,
    ),
    "oedland": gebiet(
        temperatur=0.9, niederschlag=0.2, gras="#9a8a5c", laub="#7d6f4b",
        wasser="#6b6a55", wasser_nebel="#2a2a20", himmel="#8a847a",
        deko_liste=deko(f"{MOD}:boden_oede", f"{MOD}:schutt", f"{MOD}:strommast", f"{MOD}:autowrack_selten",
                        f"{MOD}:strahlenfaesser", f"{MOD}:versteck",
                        "minecraft:patch_grass_badlands", "minecraft:patch_dead_bush_badlands"),
        monster=MONSTER_GRUND + [spawn("minecraft:husk", 60, 2, 4)],
        tiere=[spawn("minecraft:rabbit", 4, 1, 2)],
    ),
    "verseuchtes_moor": gebiet(
        temperatur=0.8, niederschlag=0.9, gras="#4f5a39", laub="#3e4a2c",
        wasser="#4e6b2e", wasser_nebel="#18240c", himmel="#6c7466",
        deko_liste=deko(f"{MOD}:boden_moor", "minecraft:glow_lichen", f"{MOD}:tote_baeume_moor", f"{MOD}:strahlenfaesser", f"{MOD}:versteck",
                        "minecraft:patch_grass_forest", "minecraft:brown_mushroom_swamp", "minecraft:red_mushroom_swamp",
                        "minecraft:patch_sugar_cane_swamp", "minecraft:patch_waterlily", "minecraft:seagrass_swamp"),
        monster=MONSTER_GRUND + [spawn("minecraft:witch", 5, 1, 1), spawn("minecraft:slime", 40, 1, 3),
                                 spawn("minecraft:drowned", 30, 1, 2)],
        tiere=[spawn("minecraft:frog", 6, 2, 4)],
        stimmung_takt=2500,
    ),
    "totes_meer": gebiet(
        temperatur=0.5, niederschlag=0.5, gras="#6f6e55", laub="#5c5b46",
        wasser="#4a5558", wasser_nebel="#151a1c", himmel="#767a80",
        deko_liste=deko(f"{MOD}:versteck", "minecraft:seagrass_cold", "minecraft:seagrass_simple", "minecraft:kelp_cold"),
        monster=[spawn("minecraft:drowned", 100, 1, 3)] + MONSTER_GRUND,
        wassertiere=[spawn("minecraft:squid", 3, 1, 4), spawn("minecraft:cod", 5, 2, 4)],
    ),
}

# Lage der Gebiete im "Klimaraum": Temperatur, Feuchte, Kontinentalität, Erosion
GANZ = [-1.0, 1.0]


def lage(biom, *, t=GANZ, h=GANZ, c=GANZ, e=GANZ):
    return {
        "biome": f"{MOD}:{biom}",
        "parameters": {
            "temperature": t, "humidity": h, "continentalness": c, "erosion": e,
            "weirdness": GANZ, "depth": 0.0, "offset": 0.0,
        },
    }


LAND = [-0.19, 1.2]
# Die Vorstadt ist das häufigste Gebiet (dort liegen die Siedlungen). Ödland nur, wo es heiß und
# trocken ist; Toter Wald und Moor, wo es feucht ist – das Moor nur in flachen Niederungen.
LAGEN = [
    lage("totes_meer", c=[-1.2, -0.19]),
    lage("vorstadt", t=[-1.0, 0.45], h=[-1.0, 0.1], c=LAND),
    lage("vorstadt", t=[0.45, 1.0], h=[-0.1, 0.1], c=LAND),
    lage("oedland", t=[0.45, 1.0], h=[-1.0, -0.1], c=LAND),
    lage("toter_wald", h=[0.1, 1.0], c=LAND, e=[-1.0, 0.35]),
    lage("verseuchtes_moor", h=[0.1, 1.0], c=LAND, e=[0.35, 1.0]),
]

WELTTYP = {
    "dimensions": {
        "minecraft:overworld": {
            "type": "minecraft:overworld",
            "generator": {
                "type": "minecraft:noise",
                "biome_source": {"type": "minecraft:multi_noise", "biomes": LAGEN},
                "settings": "minecraft:overworld",
            },
        },
        "minecraft:the_end": {
            "type": "minecraft:the_end",
            "generator": {"type": "minecraft:noise", "biome_source": {"type": "minecraft:the_end"},
                          "settings": "minecraft:end"},
        },
        "minecraft:the_nether": {
            "type": "minecraft:the_nether",
            "generator": {"type": "minecraft:noise",
                          "biome_source": {"type": "minecraft:multi_noise", "preset": "minecraft:nether"},
                          "settings": "minecraft:nether"},
        },
    }
}

# ---------------------------------------------------------------------------------------------
# Strukturen
# ---------------------------------------------------------------------------------------------

STRUKTUREN = {
    # Siedlungen werden in 64x64-Kacheln gebaut; jede Kachel schaut im Ortsplan nach, was zu ihr gehört.
    "siedlungskachel": {
        "type": f"{MOD}:siedlungskachel",
        "biomes": f"#{MOD}:hat_siedlung",
        "step": "surface_structures",
        "spawn_overrides": {},
        "terrain_adaptation": "beard_thin",
    },
}

STRUKTUR_SETS = {
    # spacing 4 / separation 3: genau eine Kachel pro 4x4 Chunks, immer in der Ecke
    "siedlungskacheln": {
        "placement": {"type": "minecraft:random_spread", "salt": 731904221, "separation": 3, "spacing": 4},
        "structures": [{"structure": f"{MOD}:siedlungskachel", "weight": 1}],
    },
}

BIOM_TAGS = {
    f"{MOD}:hat_siedlung": [f"{MOD}:{n}" for n in GEBIETE],
    # Minecraft-Tags ergänzen, damit Minen, Schiffswracks usw. auch bei uns entstehen

    "minecraft:has_structure/mineshaft": [f"{MOD}:vorstadt", f"{MOD}:toter_wald", f"{MOD}:oedland",
                                          f"{MOD}:verseuchtes_moor"],
    "minecraft:is_ocean": [f"{MOD}:totes_meer"],
}

SCHADENSARTEN = {
    "saurer_regen": {"message_id": f"{MOD}.saurer_regen", "exhaustion": 0.0, "scaling": "never"},
    "asche": {"message_id": f"{MOD}.asche", "exhaustion": 0.1, "scaling": "never"},
    "blutverlust": {"message_id": f"{MOD}.blutverlust", "exhaustion": 0.0, "scaling": "never"},
    "emission": {"message_id": f"{MOD}.emission", "exhaustion": 0.0, "scaling": "never", "effects": "burning"},
}


def main():
    for name, daten in CONFIGURED.items():
        schreiben(f"{MOD}/worldgen/configured_feature/{name}.json", daten)
    for name, (feature, platz) in PLACED.items():
        schreiben(f"{MOD}/worldgen/placed_feature/{name}.json", {"feature": f"{MOD}:{feature}", "placement": platz})
    for name, daten in GEBIETE.items():
        schreiben(f"{MOD}/worldgen/biome/{name}.json", daten)
    # (Eigener Welttyp entfällt: die Welt kommt jetzt von Lost Cities + Biomes O' Plenty)
    # Früher ersetzte Deadzone den normalen Welttyp. Jetzt: Lost Cities + Biomes O' Plenty,
    # unsere Deko kommt per Biom-Modifikator dazu (werkzeuge/welt_modifikatoren.py).
    for name, daten in STRUKTUREN.items():
        schreiben(f"{MOD}/worldgen/structure/{name}.json", daten)
    for name, daten in STRUKTUR_SETS.items():
        schreiben(f"{MOD}/worldgen/structure_set/{name}.json", daten)
    for tag, werte in BIOM_TAGS.items():
        ns, pfad = tag.split(":")
        schreiben(f"{ns}/tags/worldgen/biome/{pfad}.json", {"replace": False, "values": werte})
    for name, daten in SCHADENSARTEN.items():
        schreiben(f"{MOD}/damage_type/{name}.json", daten)
    print("Weltdaten geschrieben.")


if __name__ == "__main__":
    main()
