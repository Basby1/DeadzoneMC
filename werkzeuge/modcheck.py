#!/usr/bin/env python3
"""Prüft Mod-Kandidaten auf Modrinth: gibt es eine NeoForge-1.21.1-Version, wie beliebt, wie aktuell.

Ausgabe als GitHub-Annotationen (je ≤ 4 KB), damit man das Ergebnis ohne Log-Download sieht.
"""
import json
import sys
import time
import urllib.parse
import urllib.request

API = "https://api.modrinth.com/v2"
KOPF = {"User-Agent": "Basby1/DeadzoneMC modcheck (github.com/Basby1/DeadzoneMC)"}

# (Name/Slug, Art) – Art: mod, shader, resourcepack
KANDIDATEN = [
    ("parcool", "mod"), ("pufferfish-skills", "mod"), ("handcrafted", "mod"), ("macaws-furniture", "mod"),
    ("macaws-roofs", "mod"), ("macaws-windows", "mod"), ("macaws-lights-and-lamps", "mod"), ("macaws-paths-and-pavings", "mod"),
    ("macaws-bridges", "mod"), ("macaws-fences-and-walls", "mod"), ("macaws-trapdoors", "mod"), ("macaws-stairs", "mod"),
    ("another-furniture", "mod"), ("decorative-blocks", "mod"), ("chipped", "mod"), ("rechiseled", "mod"),
    ("every-compat", "mod"), ("create", "mod"), ("biomes-o-plenty", "mod"), ("terrablender", "mod"),
    ("critters-and-companions", "mod"), ("untamedwilds", "mod"), ("friends-and-foes-forge", "mod"), ("ecologics", "mod"),
    ("wildlife", "mod"), ("fauna", "mod"), ("crabbers-delight", "mod"), ("environmental", "mod"), ("autumnity", "mod"),
    ("farmers-delight", "mod"), ("storage-drawers", "mod"), ("sophisticated-storage", "mod"), ("waystones", "mod"),
    ("xaeros-minimap", "mod"), ("dynamic-trees", "mod"), ("fast-leaf-decay", "mod"), ("better-clouds", "mod"),
    ("skill-tree", "mod"), ("project-mmo", "mod"), ("leveling-up", "mod"), ("level-up-reforged", "mod"),
    ("ftb-quests", "mod"), ("easy-npc", "mod"), ("custom-npcs-unofficial", "mod"), ("villagers-plus", "mod"),
]

LADER = {"mod": ["neoforge"], "shader": ["iris"], "resourcepack": ["minecraft"]}


def hole(pfad):
    for versuch in range(3):
        try:
            with urllib.request.urlopen(urllib.request.Request(API + pfad, headers=KOPF), timeout=20) as a:
                return json.load(a)
        except urllib.error.HTTPError as e:
            if e.code == 404:
                return None
            time.sleep(2)
        except Exception:
            time.sleep(2)
    return None


def suche(name, art):
    facets = json.dumps([[f"project_type:{art}"]])
    q = urllib.parse.urlencode({"query": name.replace("-", " "), "facets": facets, "limit": 3})
    r = hole("/search?" + q)
    if not r or not r.get("hits"):
        return None
    return hole("/project/" + r["hits"][0]["slug"])


def main():
    zeilen = []
    daten = []
    for name, art in KANDIDATEN:
        p = hole("/project/" + name)
        gesucht = False
        if p is None:
            p = suche(name, art)
            gesucht = True
        if p is None:
            zeilen.append(f"FEHLT  {name}: nicht auf Modrinth")
            continue
        q = urllib.parse.urlencode({"loaders": json.dumps(LADER[art]), "game_versions": json.dumps(["1.21.1"])})
        vs = hole(f"/project/{p['id']}/version?{q}") or []
        dl = p.get("downloads", 0)
        seiten = f"c={p.get('client_side', '?')[:3]} s={p.get('server_side', '?')[:3]}"
        aktuell = p.get("updated", "")[:10]
        marke = "(gesucht)" if gesucht else ""
        if vs:
            v = vs[0]
            zeilen.append(f"OK     {p['slug']}{marke}: {dl // 1000}k DL, {seiten}, akt {aktuell}, v{v['version_number']} ({v['date_published'][:10]})")
            daten.append({"slug": p["slug"], "art": art, "titel": p["title"], "downloads": dl,
                          "client": p.get("client_side"), "server": p.get("server_side"),
                          "version": v["version_number"], "datum": v["date_published"][:10]})
        else:
            zeilen.append(f"NEIN   {p['slug']}{marke}: keine NeoForge-1.21.1-Version ({dl // 1000}k DL)")
        time.sleep(0.15)

    with open("modcheck.json", "w") as f:
        json.dump(daten, f, indent=1)
    block = []
    groesse = 0
    nr = 1
    for z in zeilen:
        print(z)
        if groesse + len(z) > 3500:
            print(f"::notice title=Modcheck {nr}::" + "%0A".join(block))
            nr += 1
            block, groesse = [], 0
        block.append(z)
        groesse += len(z) + 3
    if block:
        print(f"::notice title=Modcheck {nr}::" + "%0A".join(block))


if __name__ == "__main__":
    sys.exit(main())
