"""Entwürfe: Tarnfresser, Schwarmmutter (Metro-Boss), Der Pilot (Boss am Transporthubschrauber).

  python3 neue_gegner.py [--einbauen]   – ohne Schalter nur Vorschaublätter, mit Schalter auch geo/Textur
"""
import math
import os
import sys

import entwuerfe as E
from entwuerfe import gesicht, nur_flaeche, pusteln, rippen
from maler import farbe, fleisch, haut, kombi, knochen, leuchtend, metall, stoff, einfarbig
from modell import Modell


def tarnfresser():
    """Hinterhalt: Haut wie Rinde und Schutt, bewachsen mit Moos und Ästen – erst aus der Nähe zu erkennen.
    Hockt reglos, schnellt mit langen Hakenarmen vor. Taschenlampe und Nachtsicht verraten ihn."""
    m = Modell("tarnfresser", 128, 128)
    rinde = kombi(haut("5e4e38", flecken_f="3e4a22", adern_f="2a2418", blut=0),
                  lambda mm, r, f, w: mm.streifen(r, farbe("3a2e20"), abstand=2, senkrecht=True, staerke=0.5),
                  lambda mm, r, f, w: mm.flecken(r, farbe("4a6a2a"), skala=1.6, schwelle=0.62, staerke=0.9))
    moos = kombi(einfarbig("4a6a2a", 14), lambda mm, r, f, w: mm.flecken(r, farbe("6a8a3a"), skala=1.2, schwelle=0.6, staerke=0.8))
    ast = knochen("4a3a26")
    p = {"o": farbe("e8e4c8"), "s": farbe("140c08"), "z": farbe("d8cca8"), "r": farbe("3a0a0a")}
    koerper = m.k("koerper", drehpunkt=(0, 10, 0))
    ober = m.k("oberkoerper", "koerper", drehpunkt=(0, 10, 2), drehung=(-62, 0, 0))
    ober.w((-4.5, 10, -2.5), (9, 13, 6), rinde)
    ober.w((-5, 18, 0), (10, 4, 4), moos, aufblasen=0.3)
    for i, (x, z, dreh) in enumerate(((-3, 3, (30, 0, -25)), (2, 3, (20, 0, 30)), (0, 2, (40, 0, 5)))):
        a = m.k(f"zweig_{i}", "oberkoerper", drehpunkt=(x, 20, z), drehung=dreh)
        a.w((x - 0.5, 20, z - 0.5), (1, 9, 1), ast)
        a.w((x - 1.5, 27, z - 1.5), (3, 2, 3), moos)
    kopf = m.k("kopf", "oberkoerper", drehpunkt=(0, 23, 0), drehung=(55, 0, 0))
    kopf.w((-3.5, 23, -4), (7, 8, 7), rinde, deko=gesicht([
        ".......",
        ".o...o.",
        ".......",
        "...s...",
        "..rsr..",
        "..zsz..",
        "...s...",
        "...s...",
    ], p, glow="o"))
    for seite in (1, -1):
        name = "rechts" if seite > 0 else "links"
        ar = m.k("arm_" + name, "oberkoerper", drehpunkt=(seite * 5, 21, 0), drehung=(-15, 0, seite * -12))
        ar.w((seite * 5 - 1.25, 6, -1.25), (2.5, 15, 2.5), rinde)
        kr = m.k("klaue_" + name, "arm_" + name, drehpunkt=(seite * 5, 6, 0), drehung=(-40, 0, 0))
        for k in range(3):
            kr.w((seite * 5 - 1.2 + k * 1.0, 0, -1.5 - k * 0.3), (0.6, 6, 0.6), knochen("2a2018"))
        b = m.k("bein_" + name, "koerper", drehpunkt=(seite * 3, 10, 2), drehung=(-70, 0, seite * 10))
        b.w((seite * 3 - 1.5, 3, 0.5), (3, 8, 3), rinde)
        u = m.k("unterbein_" + name, "bein_" + name, drehpunkt=(seite * 3, 3, 2), drehung=(110, 0, 0))
        u.w((seite * 3 - 1.25, -6, 0.75), (2.5, 9, 2.5), rinde)
    return m, "Tarnfresser", "Hinterhalt · sieht aus wie Laub und Schutt; erst Taschenlampe oder Nachtsicht verraten ihn", "1,4 Blöcke (geduckt)"


def schwarmmutter():
    """Boss der Metro: aufgeblähte Rattenkönigin, halb Insekt. Ein glühender Brutsack auf dem Rücken, aus dem
    ständig Schwärmlinge kriechen. Kriecht durch die Tunnel und flutet sie mit Wellen ihrer Brut."""
    m = Modell("schwarmmutter", 256, 256)
    fell = kombi(haut("6a5a52", flecken_f="4a3a36", adern_f="5a2a3a", blut=2),
                 lambda mm, r, f, w: mm.streifen(r, farbe("3a302c"), abstand=1, senkrecht=False, staerke=0.35))
    chitin = kombi(metall("2a2220", "5a2a1a", nieten=False), lambda mm, r, f, w: mm.flecken(r, farbe("4a3a30"), skala=2, schwelle=0.6, staerke=0.8))
    sack = kombi(haut("c8b090", flecken_f="a88060", adern_f="8a3a3a", blut=1), pusteln(5, "ffb84a"))
    p = {"r": farbe("ff4a2a"), "t": farbe("e8dcb8"), "m": farbe("1a0606"), "n": farbe("8a5a5a")}
    koerper = m.k("koerper", drehpunkt=(0, 10, 0))
    koerper.w((-10, 6, -14), (20, 14, 28), fell)
    koerper.w((-9, 6, -14), (18, 3, 26), chitin, aufblasen=0.2)
    hinter = m.k("hinterleib", "koerper", drehpunkt=(0, 12, 14), drehung=(8, 0, 0))
    hinter.w((-8, 6, 14), (16, 12, 16), fell)
    for i in range(5):  # Schwanz aus Segmenten
        sw = m.k(f"schwanz_{i}", "hinterleib" if i == 0 else f"schwanz_{i - 1}", drehpunkt=(0, 10, 30 + i * 6),
                 drehung=(6, (i % 2 * 2 - 1) * 10, 0))
        sw.w((-2.5 + i * 0.3, 8, 30 + i * 6), (5 - i * 0.6, 4 - i * 0.4, 6), chitin)
    s = m.k("brutsack", "koerper", drehpunkt=(0, 20, 2), drehung=(-8, 0, 0))
    s.w((-11, 19, -8), (22, 14, 24), sack)
    s.w((-7, 31, -4), (14, 5, 16), sack, aufblasen=0.2)
    for i, (x, z) in enumerate(((-6, -6), (5, -2), (-3, 8), (7, 10), (0, 2))):  # Schwärmlinge auf dem Rücken
        j = m.k(f"junges_{i}", "brutsack", drehpunkt=(x, 32, z), drehung=(0, i * 70, 0))
        j.w((x - 1.5, 32, z - 2), (3, 2, 4), fell)
        j.w((x - 1, 32.5, z - 3), (2, 1.5, 1.5), fell, deko=gesicht(["r.r"], p, glow="r"))
    kopf = m.k("kopf", "koerper", drehpunkt=(0, 14, -14), drehung=(15, 0, 0))
    kopf.w((-6, 9, -26), (12, 10, 12), fell, deko=gesicht([
        "r.r..r.r..r.",
        "............",
        ".r..r..r..r.",
        "............",
        "nnnnnnnnnnnn",
        "tttttttttttt",
        "mmmmmmmmmmmm",
    ], p, glow="r"))
    kopf.w((-4, 7, -30), (8, 4, 6), knochen("d8ccb0"))  # Schnauze
    for k in range(4):
        kopf.w((-3.5 + k * 2.1, 5, -30), (1, 3, 1), knochen("f0e8d0"))  # Nagezähne
    for i, z in enumerate((-9, -1, 7)):
        for seite in (1, -1):
            n = f"bein_{i}_{'r' if seite > 0 else 'l'}"
            o = m.k(n, "koerper", drehpunkt=(seite * 9, 14, z), drehung=(0, (i - 1) * 25 * seite, seite * 30))
            x0 = 9 if seite > 0 else -27
            o.w((x0, 12.5, z - 1.5), (18, 3, 3), chitin)
            u = m.k(f"fuss_{i}_{'r' if seite > 0 else 'l'}", n, drehpunkt=(seite * 26, 14, z), drehung=(0, 0, seite * -8))
            u.w((seite * 26 - 1.25, -8, z - 1.25), (2.5, 24, 2.5), chitin)
            u.w((seite * 26 - 0.5, -11, z - 0.5), (1, 3, 1), knochen("1a1210"))
    return m, "Die Schwarmmutter", "Boss · Metro; schickt Welle um Welle ihrer Brut – Strom einschalten schwächt sie", "2 Blöcke hoch, 5 lang"


def pilot():
    """Boss am Transporthubschrauber: der Pilot, verschmolzen mit dem Wrack. Ein Arm ist ein Rotorblatt, der andere
    das Bordgeschütz. Kabel und Blech wachsen aus seinem Rücken, das Visier glüht orange."""
    m = Modell("pilot", 256, 128)
    anzug = stoff("4a5230", loecher=2, blut=3, falten=4, schmutz="2a2a1a")
    h = haut("9a8a7a", flecken_f="6a5a5a", adern_f="4a3a4a", blut=2)
    blech = metall("4a5238", "7a3a1a", nieten=True)
    stahl = metall("6a6e72", "6a3a1a", nieten=False)
    kabel = einfarbig("1e1e1e", 8)
    p = {"v": farbe("ff8a2a"), "V": farbe("ffd08a"), "k": farbe("1a1a1a"), "g": farbe("3a3e40")}
    koerper = m.k("koerper", drehpunkt=(0, 14, 0))
    for seite in (1, -1):
        b = m.k("bein_" + ("rechts" if seite > 0 else "links"), "koerper", drehpunkt=(seite * 3, 14, 0),
                drehung=(seite * 6, 0, 0))
        b.w((seite * 3 - 2.5, 0, -2.5), (5, 14, 5), anzug)
        b.w((seite * 3 - 2.5, 0, -2.5), (5, 4, 5), einfarbig("1e1a14", 6), aufblasen=0.3)
    ober = m.k("oberkoerper", "koerper", drehpunkt=(0, 14, 0), drehung=(-12, 8, 0))
    ober.w((-6, 14, -3.5), (12, 15, 7), anzug)
    ober.w((-6.5, 18, -4), (13, 10, 2), blech)  # Schutzweste aus Rumpfblech
    for i, (x, dreh, laenge) in enumerate(((-4, (40, 0, -30), 12), (0, (55, 0, 0), 16), (4, (35, 0, 25), 10))):
        k = m.k(f"kabel_{i}", "oberkoerper", drehpunkt=(x, 26, 3), drehung=dreh)
        k.w((x - 0.5, 26, 3), (1, laenge, 1), kabel)
    rp = m.k("rumpfteil", "oberkoerper", drehpunkt=(0, 24, 4), drehung=(20, 0, -10))
    rp.w((-7, 20, 3.5), (14, 9, 3), blech)  # Stück Rumpf, im Rücken verwachsen
    kopf = m.k("kopf", "oberkoerper", drehpunkt=(0, 29, 0), drehung=(10, -10, 4))
    kopf.w((-4, 29, -4), (8, 8, 8), h)
    kopf.w((-4.5, 29.5, -4.5), (9, 8, 9), stahl, deko=gesicht([
        "ggggggggg",
        "gkkkkkkkg",
        "gvVvvvVvg",
        "gkvvvvvkg",
        "ggggggggg",
        ".........",
        ".........",
        ".........",
        ".........",
    ], p, glow="vV"))  # Helm mit glühendem Visier
    # rechter Arm: Rotorblatt statt Unterarm – ein riesiges, schartiges Schwert
    ar = m.k("arm_rechts", "oberkoerper", drehpunkt=(7.5, 27, 0), drehung=(-25, 0, -10))
    ar.w((6, 19, -2), (4, 8, 4), anzug)
    ar.w((5.5, 15, -2.5), (5, 5, 5), blech)  # Nabe, mit dem Arm verwachsen
    ar.w((7.5, -14, -3), (1, 30, 6), stahl)  # Blatt, flach und breit
    ar.w((7.25, -16, -2), (1.5, 2, 4), stahl)
    # linker Arm: Bordgeschütz statt Unterarm, nach vorn gerichtet
    al = m.k("arm_links", "oberkoerper", drehpunkt=(-7.5, 27, 0), drehung=(-80, 0, 6))
    al.w((-10, 19, -2), (4, 8, 4), anzug)
    al.w((-10.5, 9, -2.5), (5, 10, 5), stahl)  # Gehäuse
    al.w((-9, -3, -1), (2, 12, 2), metall("2a2a2a", "4a2a1a", nieten=False))  # Lauf
    al.w((-9.5, -4, -1.5), (3, 2, 3), metall("2a2a2a", "4a2a1a", nieten=False))  # Mündung
    al.w((-11, 11, 2.5), (6, 6, 4), blech)  # Munitionskasten
    gurt = m.k("patronengurt", "arm_links", drehpunkt=(-8, 14, 4), drehung=(30, 0, 0))
    gurt.w((-8.5, 4, 4), (1, 10, 2), knochen("b08a3a"))
    return m, "Der Pilot", "Boss · am abgestürzten Transporthubschrauber; Rotorblatt-Arm und Bordgeschütz", "2,6 Blöcke"


ALLE = {"tarnfresser": tarnfresser, "schwarmmutter": schwarmmutter, "pilot": pilot}
E.ALLE.update(ALLE)

if __name__ == "__main__":
    namen = [a for a in sys.argv[1:] if not a.startswith("--")] or list(ALLE)
    for n in namen:
        if "--einbauen" in sys.argv:
            print(E.bauen(n))
        else:
            from maler import Maler
            from render import blatt
            modell, titel, unter, groesse = ALLE[n]()
            modell.vorwaerts_richten(E.NICHT_SPIEGELN.get(n, ()))
            modell.uv_packen()
            maler = Maler(modell, saat=sum(map(ord, n)))
            maler.alles_malen()
            os.makedirs(E.VORSCHAU, exist_ok=True)
            pfad = os.path.join(E.VORSCHAU, f"entwurf_{n}.png")
            blatt(modell, maler.bild, maler.glow, titel, unter, groesse_bloecke=groesse).save(pfad)
            print(pfad)
