"""Modelle der sechs neuen Fraktionen (Menschen mit eigener Kleidung, gemeinsame Mensch-Animationen).

  python3 werkzeuge/monster/fraktionen.py
"""
import sys

import entwuerfe as E
from entwuerfe import _mensch, kapuze_offen
from maler import farbe, kombi, knochen, leuchtend, metall, stoff, einfarbig


def p_(**kw):
    return {k: farbe(v) for k, v in kw.items()}


def bandit():
    """Die Geier: Plünderer mit Schutzmaske aus Autoreifen, Lederweste, Machete."""
    p = p_(h="2a1e16", e="1a1410", w="e8e0d0", m="3a1a14", r="1c1c1c", R="3a3a3a")
    gesicht_ = ["hhhhhhhh", "h......h", ".we..ew.", "........", "RrRrRrRr", "rRrRrRrR", "RrRrRrRr", "........"]

    def extras(m, k):
        k["kopf"].w((-4.5, 29, -4.5), (9, 1, 9), stoff("3a2a1a", loecher=1, blut=0, falten=3))  # Kopftuch
        k["ober"].w((-4, 13, -2), (8, 10, 4), stoff("5a3a22", loecher=1, blut=1, falten=3), aufblasen=0.4)  # Weste
        k["ober"].w((-4.5, 18, -2.6), (2, 6, 1), metall("6a6e70", "7a3a1a"))  # Schulterplatte
        mach = m.k("machete", "arm_rechts", drehpunkt=(5, 13, 0), drehung=(-70, 0, 0))
        mach.w((5, 12.5, -11), (0.6, 2, 11), metall("9a9ea2", "7a3a1a", nieten=False))
        mach.w((4.7, 12.2, -1), (1.2, 2.6, 3), einfarbig("2a2018", 6))
    m = _mensch("bandit", "b08a6a", gesicht_, p, stoff("3a3a38", loecher=2, blut=1, falten=4),
                stoff("2a2a30", loecher=1, blut=1, falten=4), einfarbig("1a1612", 6), extras,
                arm_pose=((70, 0, 0), (10, 0, -4)))
    return m, "Geier (Bandit)", "Fraktion · Plünderer – erst feindlich, aber bestechlich", "1,9 Blöcke"


def totenfluesterer():
    """Totenflüsterer: graue Lumpen, Knochenketten, das Gesicht mit Asche und Blut bemalt."""
    p = p_(a="7a746c", w="d8d8c8", k="1a1818", r="6a1a1a", s="4a4644")
    gesicht_ = ["aaaaaaaa", "aaaaaaaa", "awkaawka", "aaraaraa", "aaaaaaaa", "aarrrraa", "araaaara", "aaaaaaaa"]

    def extras(m, k):
        k["kopf"].w((-4, 24, -4), (8, 8, 8), stoff("4a4642", loecher=1, blut=0, falten=2), aufblasen=0.5, deko=kapuze_offen)
        for i, y in enumerate((20, 17, 14)):
            k["ober"].w((-3 + i, y, -2.6), (2, 1.5, 1), knochen("d8ceb0"))  # Knochenkette
        k["ober"].w((-4.5, 2, -2.5), (9, 11, 5), stoff("5a5652", loecher=3, blut=1, falten=2, fransen=True))
        stab = m.k("knochenstab", "arm_rechts", drehpunkt=(5.5, 13, 0), drehung=(-60, 0, 0))
        stab.w((5, 12.5, -12), (1, 1, 16), knochen("cfc6a8"))
        stab.w((4.5, 12, -13), (2, 2, 2), leuchtend("8aff9a", 14))
    m = _mensch("totenfluesterer", "8a8a80", gesicht_, p, stoff("4e4a46", loecher=3, blut=1, falten=3),
                stoff("3a3632", loecher=2, blut=1, falten=3), einfarbig("2a2622", 5), extras, glow="w",
                arm_pose=((60, 0, 0), (40, -20, -6)))
    return m, "Totenflüsterer", "Fraktion · sprechen mit den Infizierten – und die hören zu", "1,9 Blöcke"


def waldkind():
    """Kinder des Waldes: Kleidung aus Moos und Rinde, Federn im Haar, Bogen auf dem Rücken."""
    p = p_(h="4a3a1e", e="1a1410", w="e8e0d0", g="3a5a2a", m="5a3a2a", f="a8402a")
    gesicht_ = ["hhfhhhhh", "hh.hh..h", ".we..ew.", "...gg...", "........", "..mmmm..", "........", "........"]
    rinde = kombi(stoff("4a5a2a", loecher=1, blut=0, falten=3, schmutz="2a3a1a"))

    def extras(m, k):
        k["kopf"].w((-4, 29, -4), (8, 3, 8), stoff("3a4a22", loecher=0, blut=0, falten=4), aufblasen=0.3)
        k["kopf"].w((2, 30, 2), (1, 5, 1), einfarbig("a8402a", 8))  # Feder
        k["ober"].w((-4, 12, -2), (8, 12, 4), stoff("5a4a2a", loecher=1, blut=0, falten=4), aufblasen=0.3)
        bogen = m.k("bogen", "oberkoerper", drehpunkt=(0, 18, 3), drehung=(0, 0, 35))
        bogen.w((-0.5, 8, 3), (1, 20, 1), einfarbig("6a4a2a", 6))
    m = _mensch("waldkind", "b89070", gesicht_, p, rinde, stoff("3a3a22", loecher=1, blut=0, falten=4),
                einfarbig("3a2a1a", 6), extras, arm_pose=((4, 0, 3), (4, 0, -3)))
    return m, "Kind des Waldes", "Fraktion · leben in Baumhäusern, kennen jedes Kraut", "1,9 Blöcke"


def sandwanderer():
    """Sandwanderer: helle Tücher, Schutzbrille, Wasserschlauch – Nomaden der Wüste."""
    p = p_(t="d8c8a0", T="b8a880", g="2a3a4a", G="6a8aa8", e="1a1410")
    gesicht_ = ["tttttttt", "tTTTTTTt", "gGgttgGg", "tttttttt", "TTTTTTTT", "tTtTtTtT", "TtTtTtTt", "tttttttt"]

    def extras(m, k):
        k["kopf"].w((-4.5, 24, -4.5), (9, 9, 9), stoff("d8c8a0", loecher=0, blut=0, falten=4, schmutz="a89870"),
                    aufblasen=0.2, deko=kapuze_offen)
        k["ober"].w((-4.5, 2, -2.5), (9, 22, 5), stoff("c8b890", loecher=0, blut=0, falten=4, fransen=True))
        k["ober"].w((-5, 14, 2.4), (3, 6, 2), stoff("7a5a3a", loecher=0, blut=0, falten=2))  # Wasserschlauch
    m = _mensch("sandwanderer", "a8784e", gesicht_, p, stoff("d0c098", loecher=0, blut=0, falten=4),
                stoff("b0a078", loecher=0, blut=0, falten=4), einfarbig("6a5030", 6), extras,
                arm_pose=((6, 0, 3), (6, 0, -3)))
    return m, "Sandwanderer", "Fraktion · Nomaden der Wüste: Wasser, Karten, Wege", "1,9 Blöcke"


def tiefenvolk():
    """Unterwasservolk: Taucheranzug aus alten Neoprenresten, Glashelm, Kiemenfilter."""
    p = p_(b="2a5a6a", B="4a8a9a", w="e8f0f0", e="0a1a20", m="3a2a2a")
    gesicht_ = ["bbbbbbbb", "bBBBBBBb", "bwebbewb", "bBBBBBBb", "bBBBBBBb", "bbmmmmbb", "bBBBBBBb", "bbbbbbbb"]

    def extras(m, k):
        k["kopf"].w((-4.5, 23.5, -4.5), (9, 9, 9), kombi(einfarbig("8adaea", 6, alpha=110)), aufblasen=0.3)  # Glashelm
        k["ober"].w((-3, 22, 2), (6, 6, 3), metall("6a7a7a", "3a5a5a", nieten=True))  # Sauerstoffflasche
        k["ober"].w((-2, 12, -2.6), (4, 3, 1), leuchtend("6affea", 12))  # Lampe
    m = _mensch("tiefenvolk", "9ab8b8", gesicht_, p, stoff("1e3a44", loecher=0, blut=0, falten=2),
                stoff("1a2e36", loecher=0, blut=0, falten=2), einfarbig("10202a", 6), extras,
                arm_pose=((8, 0, 4), (8, 0, -4)))
    return m, "Tiefenvolk", "Fraktion · leben in Kuppeln am Meeresgrund", "1,9 Blöcke"


def himmelshueter():
    """Hüter des Himmels: weiße Kutten mit Silberfäden, Gleitschirm auf dem Rücken, Fernrohr."""
    p = p_(w="f0f0f0", s="c8d0d8", S="8a98a8", e="1a2030", a="6ab0ff")
    gesicht_ = ["ssssssss", "sSSSSSSs", "SaeSSeaS", "SSSSSSSS", "SSSSSSSS", "SSeeeeSS", "SSSSSSSS", "ssssssss"]

    def extras(m, k):
        k["kopf"].w((-4, 24, -4), (8, 8, 8), stoff("e8ecf0", loecher=0, blut=0, falten=2, schmutz="b8c0c8"),
                    aufblasen=0.5, deko=kapuze_offen)
        k["ober"].w((-4.5, 2, -2.5), (9, 11, 5), stoff("e0e4e8", loecher=0, blut=0, falten=2, fransen=True))
        schirm = m.k("gleitschirm", "oberkoerper", drehpunkt=(0, 22, 3))
        schirm.w((-7, 14, 2.5), (14, 12, 1), stoff("6a9ad0", loecher=0, blut=0, falten=2))
        schirm.w((-0.5, 12, 2), (1, 16, 1), metall("c0c8d0", "8a8a8a", nieten=False))
    m = _mensch("himmelshueter", "d8c0a8", gesicht_, p, stoff("e8ecf0", loecher=0, blut=0, falten=2),
                stoff("c8d0d8", loecher=0, blut=0, falten=2), einfarbig("8a98a8", 6), extras, glow="a",
                arm_pose=((6, 0, 3), (6, 0, -3)))
    return m, "Hüter des Himmels", "Fraktion · leben über den Wolken, sehen alles", "1,9 Blöcke"


ALLE = {"bandit": bandit, "totenfluesterer": totenfluesterer, "waldkind": waldkind, "sandwanderer": sandwanderer,
        "tiefenvolk": tiefenvolk, "himmelshueter": himmelshueter}
E.ALLE.update(ALLE)

if __name__ == "__main__":
    for n in (sys.argv[1:] or ALLE):
        print(E.bauen(n))
