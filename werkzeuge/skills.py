#!/usr/bin/env python3
"""Erzeugt die Skillbäume für Pufferfish's Skills (Datapack) und die ParCool-Serverconfig.

Fünf Bäume, jeder mit eigenem Level (Erfahrung aus passenden Tätigkeiten), pro Level ein Punkt:
  Kampf · Schusswaffen · Überleben · Handwerk · Bewegung (Parkour über ParCool)
Keine Magie – nur Attribute, Parkour-Freischaltungen und Deadzone-Talente (Spieler-Tags „dz.…“, siehe Talente.java).
"""
import json
import os
import shutil

MOD = "deadzonemc"
WURZEL = os.path.join(os.path.dirname(__file__), "..")
ZIEL = os.path.join(WURZEL, "src", "main", "resources", "data", MOD, "puffish_skills")
PARCOOL = os.path.join(WURZEL, "pack", "overrides", "defaultconfigs", "parcool-server.toml")
RASTER = 34

AKTIONEN = ["grapple", "wall_jump", "ride_zipline", "wall_run", "fast_run", "vault", "horizontal_wall_run", "fast_swim",
            "long_jump", "dive", "skydive", "trick_jump", "hide_in_block", "crawl", "slide", "hang_on", "climb_up",
            "castaway", "hang_down", "pole_climb", "slide_down", "dodge", "breakfall", "charge_jump"]


# ---- Bausteine ------------------------------------------------------------------------------

def attr(attribut, wert, op="addition"):
    return {"type": "puffish_skills:attribute",
            "data": {"attribute": "minecraft:" + attribut, "value": wert, "operation": op}}


def talent(name):
    return {"type": "puffish_skills:tag", "data": {"tag": "dz." + name}}


def parkour(*aktionen):
    return [{"type": "puffish_skills:command",
             "data": {"command": "", "unlock_command": f"parcool action unlock @s parcool:{a}",
                      "lock_command": f"parcool action lock @s parcool:{a}"}} for a in aktionen]


def var(*ops, fallback=None):
    v = {"operations": [o if isinstance(o, dict) else {"type": o} for o in ops]}
    if fallback is not None:
        v["fallback"] = fallback
        v["required"] = False
    return v


def test(**daten):
    return {"type": "test", "data": daten}


class Baum:
    def __init__(self, id, titel, beschreibung, icon, hintergrund, quellen, limit=20):
        self.id, self.titel, self.beschreibung, self.icon = id, titel, beschreibung, icon
        self.hintergrund, self.quellen, self.limit = hintergrund, quellen, limit
        self.knoten = {}
        self.kanten = []

    def k(self, id, gx, gy, titel, text, icon, belohnungen, kosten=1, rahmen="task", wurzel=False, nach=()):
        self.knoten[id] = dict(gx=gx, gy=gy, titel=titel, text=text, icon=icon, belohnungen=belohnungen,
                               kosten=kosten, rahmen=rahmen, wurzel=wurzel)
        for n in ([nach] if isinstance(nach, str) else nach):
            self.kanten.append([n, id])
        return id

    def schreiben(self):
        ordner = os.path.join(ZIEL, "categories", self.id)
        os.makedirs(ordner, exist_ok=True)
        kategorie = {
            "title": self.titel, "description": self.beschreibung,
            "icon": {"type": "item", "data": {"item": self.icon}},
            "background": {"texture": self.hintergrund, "width": 16, "height": 16, "position": "tile"},
            "unlocked_by_default": True, "starting_points": 1,
        }
        definitionen, skills = {}, {}
        for id, n in self.knoten.items():
            definitionen[id] = {
                "title": n["titel"], "description": n["text"],
                "icon": {"type": "item", "data": {"item": n["icon"]}},
                "frame": {"type": "advancement", "data": {"frame": n["rahmen"]}},
                "rewards": n["belohnungen"], "cost": n["kosten"],
            }
            skills[id] = {"x": n["gx"] * RASTER, "y": n["gy"] * RASTER, "definition": id}
            if n["wurzel"]:
                skills[id]["root"] = True
        erfahrung = {
            "experience_per_level": {"type": "expression",
                                     "data": {"expression": "30 + level * 12 + level * level * 1.2"}},
            "level_limit": self.limit,
            "sources": self.quellen,
        }
        verbindungen = {"normal": {"bidirectional": self.kanten, "unidirectional": []}}
        for name, daten in (("category", kategorie), ("definitions", definitionen), ("skills", skills),
                            ("connections", verbindungen), ("experience", erfahrung)):
            with open(os.path.join(ordner, name + ".json"), "w", encoding="utf-8") as f:
                json.dump(daten, f, ensure_ascii=False, indent=1)
        kosten = sum(n["kosten"] for n in self.knoten.values())
        print(f"{self.id}: {len(self.knoten)} Skills, {kosten} Punkte gesamt, Levelgrenze {self.limit} (+1 Startpunkt)")


# ---- Erfahrungsquellen ----------------------------------------------------------------------

def kill(ausdruck, **variablen):
    v = {"hp": var("get_killed_living_entity", "get_max_health"),
         "xp": var("get_dropped_experience")}
    v.update(variablen)
    return {"type": "puffish_skills:kill_entity",
            "data": {"variables": v, "experience": ausdruck,
                     "anti_farming_per_chunk": {"limit_per_chunk": 15, "reset_after_seconds": 300}}}


NAHKAMPF = kill("min(hp, 80) * (0.15 + 0.07 * min(xp, 5)) * nah",
                nah=var("get_damage_source", "is_melee"))
FERNKAMPF = kill("min(hp, 80) * (0.15 + 0.07 * min(xp, 5)) * max(fern, waffe)",
                 fern=var("get_damage_source", "is_projectile"),
                 waffe=var("get_weapon_item_stack", test(item="tacz:modern_kinetic_gun"), fallback=0))
TREFFER = {"type": "puffish_skills:deal_damage",
           "data": {"variables": {"d": var("get_dealt_damage"), "fern": var("get_damage_source", "is_projectile")},
                    "experience": "min(d, 10) * 0.15 * fern"}}
UEBERLEBEN = [
    {"type": "puffish_skills:eat_food",
     "data": {"variables": {"n": var("get_eaten_item_stack", "get_item", "get_nutrition")}, "experience": "n * 1.5"}},
    {"type": "puffish_skills:take_damage",
     "data": {"variables": {"d": var("get_taken_damage")}, "experience": "min(d, 10) * 1.2"}},
    {"type": "puffish_skills:heal",
     "data": {"variables": {"a": var("get_heal_amount")}, "experience": "min(a, 6) * 0.3"}},
]
HANDWERK = [
    {"type": "puffish_skills:craft_item",
     "data": {"variables": {"c": var("get_crafted_item_stack", "get_count")}, "experience": "min(c, 8) * 0.4"}},
    {"type": "puffish_skills:smelt_item",
     "data": {"variables": {"c": var("get_smelted_item_stack", "get_count")}, "experience": "c * 0.3"}},
    {"type": "puffish_skills:mine_block",
     "data": {"variables": {"h": var("get_mined_block_state", "get_block", "get_hardness")},
              "experience": "min(max(h, 0), 5) * 0.35"}},
]


def stat(name):
    return var("get_stat", test(stat="minecraft.custom:minecraft." + name))


BEWEGUNG = [
    {"type": "puffish_skills:increase_stat",
     "data": {"variables": {"a": var("get_increase_amount"), "sprint": stat("sprint_one_cm"),
                            "klettern": stat("climb_one_cm"), "springen": stat("jump"),
                            "schwimmen": stat("swim_one_cm")},
              "experience": "a * (sprint * 0.0012 + klettern * 0.006 + schwimmen * 0.001 + springen * 0.12)"}},
]

BETON = "minecraft:textures/block/gray_concrete_powder.png"


# ---- Bäume ----------------------------------------------------------------------------------

def kampf():
    b = Baum("kampf", "Kampf", "Nahkampf gegen die Infizierten. Erfahrung durch Nahkampf-Kills.",
             "minecraft:iron_axe", "minecraft:textures/block/stripped_dark_oak_log.png", [NAHKAMPF])
    w = b.k("schlagkraft1", 0, 0, "Schlagkraft I", "+0,5 Nahkampfschaden", "minecraft:wooden_axe",
            [attr("generic.attack_damage", 0.5)], wurzel=True)
    s2 = b.k("schlagkraft2", 0, -2, "Schlagkraft II", "+0,5 Nahkampfschaden", "minecraft:stone_axe",
             [attr("generic.attack_damage", 0.5)], nach=w)
    s3 = b.k("schlagkraft3", 0, -4, "Schlagkraft III", "+1 Nahkampfschaden", "minecraft:iron_axe",
             [attr("generic.attack_damage", 1.0)], kosten=2, nach=s2)
    z1 = b.k("zaeh1", -2, -1, "Zäh I", "+2 maximale Gesundheit (1 Herz)", "minecraft:leather_chestplate",
             [attr("generic.max_health", 2)], nach=w)
    z2 = b.k("zaeh2", -4, -2, "Zäh II", "+2 maximale Gesundheit", "minecraft:chainmail_chestplate",
             [attr("generic.max_health", 2)], nach=z1)
    z3 = b.k("zaeh3", -4, -4, "Zäh III", "+4 maximale Gesundheit", "minecraft:iron_chestplate",
             [attr("generic.max_health", 4)], kosten=2, nach=z2)
    r1 = b.k("ruestung1", -2, -3, "Rüstungskunde I", "+1 Rüstung", "minecraft:iron_helmet",
             [attr("generic.armor", 1)], nach=(z1, s2))
    r2 = b.k("ruestung2", -2, -5, "Rüstungskunde II", "+1 Rüstung, +1 Rüstungshärte", "minecraft:diamond_helmet",
             [attr("generic.armor", 1), attr("generic.armor_toughness", 1)], kosten=2, nach=r1)
    h1 = b.k("hiebe1", 2, -1, "Schnelle Hiebe I", "+5 % Angriffstempo", "minecraft:feather",
             [attr("generic.attack_speed", 0.05, "multiply_base")], nach=w)
    h2 = b.k("hiebe2", 4, -2, "Schnelle Hiebe II", "+5 % Angriffstempo", "minecraft:arrow",
             [attr("generic.attack_speed", 0.05, "multiply_base")], nach=h1)
    st = b.k("standfest", 2, -3, "Standfest", "+20 % Rückstoßwiderstand – Horden schubsen dich nicht so leicht",
             "minecraft:shield", [attr("generic.knockback_resistance", 0.2)], nach=(h1, s2))
    b.k("berserker", 4, -4, "Berserker", "+10 % Nahkampfschaden", "minecraft:netherite_axe",
        [attr("generic.attack_damage", 0.10, "multiply_base")], kosten=3, rahmen="challenge", nach=(h2, st))
    b.k("bollwerk", 0, -6, "Bollwerk", "+15 % Rückstoßwiderstand, +2 Gesundheit", "minecraft:anvil",
        [attr("generic.knockback_resistance", 0.15), attr("generic.max_health", 2)], kosten=3, rahmen="challenge",
        nach=(s3, r2, z3))
    return b


def schusswaffen():
    b = Baum("schusswaffen", "Schusswaffen", "Pistolen, Gewehre, Bögen. Erfahrung durch Fernkampf-Treffer und -Kills.",
             "minecraft:crossbow", "minecraft:textures/block/iron_block.png", [FERNKAMPF, TREFFER])
    t1 = b.k("treffsicher1", 0, 0, "Treffsicher I", "+8 % Schaden mit Schusswaffen und Bögen", "minecraft:arrow",
             [talent("schuetze1")], wurzel=True)
    t2 = b.k("treffsicher2", 0, -2, "Treffsicher II", "+8 % Fernkampfschaden", "minecraft:spectral_arrow",
             [talent("schuetze2")], kosten=2, nach=t1)
    t3 = b.k("treffsicher3", 0, -4, "Treffsicher III", "+8 % Fernkampfschaden", "minecraft:tipped_arrow",
             [talent("schuetze3")], kosten=3, nach=t2)
    p1 = b.k("pluenderer1", -2, -1, "Plünderer I", "+1 Glück: bessere Funde in Kisten", "minecraft:chest",
             [attr("generic.luck", 1)], nach=t1)
    p2 = b.k("pluenderer2", -4, -2, "Plünderer II", "+1 Glück", "minecraft:barrel",
             [attr("generic.luck", 1)], kosten=2, nach=p1)
    p3 = b.k("pluenderer3", -4, -4, "Plünderer III", "+1 Glück", "minecraft:ender_chest",
             [attr("generic.luck", 1)], kosten=2, nach=p2)
    g = b.k("gefechtsbereit", 2, -1, "Gefechtsbereit", "+3 % Lauftempo", "minecraft:leather_boots",
            [attr("generic.movement_speed", 0.03, "multiply_base")], nach=t1)
    k = b.k("kaltbluetig", 2, -3, "Kaltblütig", "Unter 30 % Gesundheit: +15 % Fernkampfschaden",
            "minecraft:blue_ice", [talent("kaltbluetig")], kosten=2, nach=(g, t2))
    b.k("adlerauge", -2, -3, "Adlerauge", "+1 Reichweite beim Interagieren mit Wesen (Wiederbeleben, Handel)",
        "minecraft:spyglass", [attr("player.entity_interaction_range", 1)], nach=(p1, t2))
    b.k("scharfschuetze", 0, -6, "Scharfschütze", "+1 Glück, +3 % Lauftempo", "minecraft:target",
        [attr("generic.luck", 1), attr("generic.movement_speed", 0.03, "multiply_base")], kosten=3,
        rahmen="challenge", nach=(t3, k, p3))
    return b


def ueberleben():
    b = Baum("ueberleben", "Überleben", "Durchhalten in der Deadzone. Erfahrung durch Essen, Heilen und Einstecken.",
             "minecraft:campfire", "minecraft:textures/block/coarse_dirt.png", UEBERLEBEN)
    a = b.k("abgehaertet", 0, 0, "Abgehärtet", "+2 maximale Gesundheit", "minecraft:rotten_flesh",
            [attr("generic.max_health", 2)], wurzel=True)
    m = b.k("magen", -2, -1, "Eiserner Magen", "Verdorbenes Essen macht keinen Hunger mehr", "minecraft:spider_eye",
            [talent("magen")], nach=a)
    f1 = b.k("feldarzt1", 2, -1, "Feldarzt I", "Verbände und Medizin heilen 25 % mehr", "minecraft:paper",
             [talent("feldarzt1")], nach=a)
    f2 = b.k("feldarzt2", 4, -2, "Feldarzt II", "Verbände und Medizin heilen 50 % mehr", "minecraft:golden_apple",
             [talent("feldarzt2")], kosten=2, nach=f1)
    s1 = b.k("strahlenfest1", -2, -3, "Strahlenfest I", "25 % weniger Strahlung", "minecraft:glowstone_dust",
             [talent("strahlenfest1")], nach=m)
    s2 = b.k("strahlenfest2", -4, -4, "Strahlenfest II", "50 % weniger Strahlung", "minecraft:glowstone",
             [talent("strahlenfest2")], kosten=2, nach=s1)
    l = b.k("lunge", -4, -2, "Starke Lunge", "+2 Luft unter Wasser", "minecraft:pufferfish",
            [attr("generic.oxygen_bonus", 2)], nach=m)
    z = b.k("zaehigkeit", 0, -2, "Zähigkeit", "+2 maximale Gesundheit", "minecraft:cooked_beef",
            [attr("generic.max_health", 2)], nach=a)
    fb = b.k("feuerfest", 2, -3, "Feuerfest", "Du brennst 30 % kürzer", "minecraft:magma_cream",
             [attr("generic.burning_time", -0.3, "multiply_base")], nach=(f1, z))
    z2 = b.k("zaehigkeit2", 0, -4, "Unverwüstlich", "+4 maximale Gesundheit", "minecraft:enchanted_golden_apple",
             [attr("generic.max_health", 4)], kosten=2, nach=(z, s1, fb))
    b.k("letzter_mann", 0, -6, "Der letzte Mann", "+2 Gesundheit, +1 Rüstung", "minecraft:totem_of_undying",
        [attr("generic.max_health", 2), attr("generic.armor", 1)], kosten=3, rahmen="challenge", nach=(z2, s2, f2))
    _ = l
    return b


def handwerk():
    b = Baum("handwerk", "Handwerk", "Bauen, Abbauen, Herstellen. Erfahrung durch Abbauen, Herstellen und Schmelzen.",
             "minecraft:crafting_table", "minecraft:textures/block/spruce_planks.png", HANDWERK)
    a = b.k("anpacken", 0, 0, "Anpacken", "+10 % Abbautempo", "minecraft:wooden_pickaxe",
            [attr("player.block_break_speed", 0.10, "multiply_base")], wurzel=True)
    a2 = b.k("anpacken2", 0, -2, "Kräftig zupacken", "+10 % Abbautempo", "minecraft:iron_pickaxe",
             [attr("player.block_break_speed", 0.10, "multiply_base")], nach=a)
    a3 = b.k("anpacken3", 0, -4, "Abrissbirne", "+15 % Abbautempo", "minecraft:diamond_pickaxe",
             [attr("player.block_break_speed", 0.15, "multiply_base")], kosten=2, nach=a2)
    w1 = b.k("werkzeug1", 2, -1, "Werkzeugkunde I", "+1 Abbau-Effizienz (wie Effizienz-Verzauberung)",
             "minecraft:stone_shovel", [attr("player.mining_efficiency", 1)], nach=a)
    w2 = b.k("werkzeug2", 4, -2, "Werkzeugkunde II", "+2 Abbau-Effizienz", "minecraft:golden_shovel",
             [attr("player.mining_efficiency", 2)], kosten=2, nach=w1)
    r1 = b.k("reichweite1", -2, -1, "Lange Arme", "+0,5 Block-Reichweite", "minecraft:stick",
             [attr("player.block_interaction_range", 0.5)], nach=a)
    r2 = b.k("reichweite2", -4, -2, "Baumeister", "+1 Block-Reichweite", "minecraft:scaffolding",
             [attr("player.block_interaction_range", 1.0)], kosten=2, nach=r1)
    t = b.k("taucher", -2, -3, "Unterwasserarbeit", "Unter Wasser 50 % schneller abbauen", "minecraft:prismarine_shard",
            [attr("player.submerged_mining_speed", 0.5, "multiply_base")], nach=(r1, a2))
    lt = b.k("lasttier", 2, -3, "Lasttier", "+3 % Lauftempo", "minecraft:chest_minecart",
             [attr("generic.movement_speed", 0.03, "multiply_base")], nach=(w1, a2))
    b.k("meister", 0, -6, "Meister der Ruinen", "+20 % Abbautempo, +0,5 Reichweite", "minecraft:netherite_pickaxe",
        [attr("player.block_break_speed", 0.20, "multiply_base"), attr("player.block_interaction_range", 0.5)],
        kosten=3, rahmen="challenge", nach=(a3, w2, r2))
    _ = (t, lt)
    return b


def bewegung():
    b = Baum("bewegung", "Bewegung", "Parkour und Flucht. Erfahrung durch Sprinten, Klettern, Springen, Schwimmen.",
             "minecraft:leather_boots", "minecraft:textures/block/bricks.png", BEWEGUNG, limit=24)
    g = b.k("grundlagen", 0, 0, "Grundlagen", "Schnell rennen, kriechen, an Kanten festhalten",
            "minecraft:leather_boots", parkour("fast_run", "crawl", "hang_on"), wurzel=True)
    kl = b.k("klettern", -2, -1, "Klettern", "Kanten hochziehen, an Stangen klettern", "minecraft:ladder",
             parkour("climb_up", "pole_climb"), nach=g)
    ab = b.k("abrollen", 2, -1, "Abrollen", "Stürze abrollen (Taste beim Aufkommen), +1 sichere Fallhöhe",
             "minecraft:hay_block", parkour("breakfall") + [attr("generic.safe_fall_distance", 1)], nach=g)
    ue = b.k("ueberspringen", 0, -2, "Überspringen", "Über Hindernisse flanken", "minecraft:oak_fence",
             parkour("vault"), nach=(kl, ab))
    ru = b.k("rutschen", 2, -3, "Rutschen", "Aus dem Sprint in eine Rutsche", "minecraft:ice",
             parkour("slide"), kosten=2, nach=ue)
    he = b.k("herablassen", -4, -2, "Herablassen", "An Kanten hängen, an Wänden herabrutschen",
             "minecraft:vine", parkour("hang_down", "slide_down"), nach=kl)
    ws = b.k("wandsprung", -2, -3, "Wandsprung", "Von Wänden abspringen", "minecraft:stone_bricks",
             parkour("wall_jump"), kosten=2, nach=(kl, ue))
    wl = b.k("wandlauf", -2, -5, "Wandlauf", "Senkrecht und seitlich an Wänden entlanglaufen", "minecraft:cracked_stone_bricks",
             parkour("wall_run", "horizontal_wall_run"), kosten=3, nach=ws)
    wj = b.k("weitsprung", 0, -4, "Weitsprung", "Weit- und Kraftsprung", "minecraft:rabbit_foot",
             parkour("long_jump", "charge_jump"), kosten=2, nach=(ue, ws, ru))
    ad = b.k("ausweichen", 4, -2, "Ausweichen", "Ausweichrolle zur Seite", "minecraft:phantom_membrane",
             parkour("dodge"), kosten=2, nach=ab)
    sp1 = b.k("sprinter1", 4, -4, "Sprinter I", "+4 % Lauftempo", "minecraft:sugar",
              [attr("generic.movement_speed", 0.04, "multiply_base")], nach=(ad, ru))
    sp2 = b.k("sprinter2", 4, -6, "Sprinter II", "+4 % Lauftempo", "minecraft:sugar",
              [attr("generic.movement_speed", 0.04, "multiply_base")], kosten=2, nach=sp1)
    sw = b.k("schwimmer", -4, -4, "Schwimmer", "Schneller schwimmen, Hechtsprung, Fallschirmsprung",
             "minecraft:heart_of_the_sea", parkour("fast_swim", "dive", "skydive"), nach=he)
    sl = b.k("schleicher", -4, -6, "Schleicher", "Geduckt bemerken dich Monster erst aus nächster Nähe; "
             "in Blöcken (Heu, Laub) verstecken", "minecraft:black_wool", parkour("hide_in_block") + [talent("leise")],
             kosten=3, nach=(sw, wl))
    b.k("freerunner", 0, -7, "Freerunner", "Tricksprünge, Abstoßen, +2 sichere Fallhöhe", "minecraft:elytra",
        parkour("trick_jump", "castaway") + [attr("generic.safe_fall_distance", 2)], kosten=3, rahmen="challenge",
        nach=(wj, wl, sp2))
    _ = sl
    return b


def parcool_config():
    """Lernen nur über unseren Bewegungsbaum: ParCools eigener Baum ist aus, XP-Kosten unbezahlbar."""
    z = ["# Deadzone MC: Parkour wird im Skillbaum „Bewegung“ freigeschaltet (Taste K).", "[game]",
         "\tenable_skill_tree = true", "\tenable_skill_tree_ui = false", ""]
    for a in AKTIONEN:
        if a in ("grapple", "ride_zipline"):
            continue
        z += [f"[action.parcool.{a}]", "\tlearn_cost = 30000", ""]
    os.makedirs(os.path.dirname(PARCOOL), exist_ok=True)
    with open(PARCOOL, "w") as f:
        f.write("\n".join(z))


def main():
    if os.path.isdir(ZIEL):
        shutil.rmtree(ZIEL)
    baeume = [kampf(), schusswaffen(), ueberleben(), handwerk(), bewegung()]
    for b in baeume:
        b.schreiben()
    with open(os.path.join(ZIEL, "config.json"), "w") as f:
        json.dump({"version": 3, "categories": [b.id for b in baeume]}, f, indent=1)
    parcool_config()


if __name__ == "__main__":
    main()
