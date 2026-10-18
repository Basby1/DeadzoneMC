#!/usr/bin/env python3
"""Vanilla-Bauwerke, die nicht in die Deadzone passen, abschalten.

Dorfbewohner-Dörfer, Waldanwesen, Plünderer-Außenposten, Tempel, Iglus, Hexenhütten, Ozeanmonumente,
Trial Chambers, Antike Städte, Pfadruinen, Ruinenportale und vergrabene Schätze verschwinden. An die Stelle
der Dörfer treten unsere eigenen verlassenen Dörfer (welt/plan). Erhalten bleiben: Festungen (Portal zur
Orbitalstation), Minen, Schiffswracks.

Doppelt abgesichert: die Biom-Tags der Bauwerke werden geleert UND ihre Strukturgruppen bekommen keine
Bauwerke mehr – so bleibt es aus, auch wenn eine andere Mod Biome zu den Tags hinzufügt.

  python3 werkzeuge/strukturen.py
"""
import json
import os

WURZEL = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DATEN = os.path.join(WURZEL, "src", "main", "resources", "data", "minecraft")

TAGS_LEEREN = [
    "ancient_city", "buried_treasure", "desert_pyramid", "igloo", "jungle_temple", "ocean_monument",
    "ocean_ruin_cold", "ocean_ruin_warm", "pillager_outpost", "ruined_portal_desert", "ruined_portal_jungle",
    "ruined_portal_mountain", "ruined_portal_ocean", "ruined_portal_standard", "ruined_portal_swamp",
    "swamp_hut", "trail_ruins", "trial_chambers", "village_desert", "village_plains", "village_savanna",
    "village_snowy", "village_taiga", "woodland_mansion",
]

# Strukturgruppe -> (Abstand, Trennung, Salz) wie in Vanilla, damit das Format gültig bleibt
GRUPPEN_LEEREN = {
    "villages": (34, 8, 10387312), "desert_pyramids": (32, 8, 14357617), "igloos": (32, 8, 14357618),
    "jungle_temples": (32, 8, 14357619), "swamp_huts": (32, 8, 14357620), "pillager_outposts": (32, 8, 165745296),
    "ocean_monuments": (32, 5, 10387313), "woodland_mansions": (80, 20, 10387319),
    "buried_treasures": (1, 0, 0), "ruined_portals": (40, 15, 34222645), "ancient_cities": (24, 8, 20083232),
    "trail_ruins": (34, 8, 83469867), "trial_chambers": (34, 12, 94251327), "ocean_ruins": (20, 8, 14357621),
}


def main():
    t = os.path.join(DATEN, "tags", "worldgen", "biome", "has_structure")
    os.makedirs(t, exist_ok=True)
    for n in TAGS_LEEREN:
        with open(os.path.join(t, f"{n}.json"), "w") as f:
            json.dump({"replace": True, "values": []}, f, indent=1)
    s = os.path.join(DATEN, "worldgen", "structure_set")
    os.makedirs(s, exist_ok=True)
    for n, (abstand, trennung, salz) in GRUPPEN_LEEREN.items():
        with open(os.path.join(s, f"{n}.json"), "w") as f:
            json.dump({"placement": {"type": "minecraft:random_spread", "spacing": abstand, "separation": trennung,
                                     "salt": salz}, "structures": []}, f, indent=1)
    print(len(TAGS_LEEREN), "Tags geleert,", len(GRUPPEN_LEEREN), "Strukturgruppen abgeschaltet")


if __name__ == "__main__":
    main()
