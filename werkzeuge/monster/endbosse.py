"""Entwürfe: Nether-Endboss „Das Herz der Zone“ und die Mimik (Enthüllung).

  python3 werkzeuge/monster/endbosse.py [das_herz] [mimik]   – Modelle, Texturen und Vorschaubilder
"""
import sys

import entwuerfe as E
import dimensionen as DIM
from dimensionen import kristall, name_seite
from entwuerfe import gesicht, glutrisse
from maler import farbe, haut, kombi, knochen, leuchtend, metall, einfarbig, stoff
from modell import Modell


def das_herz():
    """Das Herz der Zone: ein Koloss aus verkohltem Fleisch und Kristall, der bis zur Hüfte aus einem Glutsee ragt.
    In der offenen Brust schlägt der Kern (Schwachstelle). Modell in Drittelgröße – im Spiel dreifach skaliert
    (etwa 18 Blöcke hoch, mit Armen 26 breit)."""
    m = Modell("das_herz", 512, 512)
    kohle = kombi(haut("2a1a16", flecken_f="1a100c", blut=0, dunkel_unten=0.35), glutrisse(1))
    kohle_dunkel = kombi(haut("1c120e", flecken_f="120a08", blut=0, dunkel_unten=0.3))
    stein = kombi(einfarbig("3a2e2a", 12), glutrisse(1, f="ff4a1a"))
    kern = leuchtend("ff5a1a", 18)
    kern_hell = leuchtend("ffd27a", 12)
    kristall_rot = kristall("ff3a2a", "ffd0a0", 210)
    knochen_ = knochen("d8c8a8")
    eisen = metall("4a4440", "7a3a1a")
    # Sockel: erstarrte Glut, aus der der Koloss wächst; Wurzelstränge laufen nach außen
    sockel = m.k("sockel", drehpunkt=(0, 0, 0))
    sockel.w((-22, 0, -18), (44, 6, 36), stein)
    sockel.w((-16, 6, -12), (32, 6, 24), kohle_dunkel)
    for i, (x, z, ry) in enumerate(((-20, -14, 35), (20, -14, -35), (-22, 12, 140), (22, 12, -140), (0, 18, 180), (-6, -18, 8))):
        w = m.k(f"wurzel_{i}", "sockel", drehpunkt=(x, 2, z), drehung=(0, ry, 0))
        w.w((x - 2, 0, z - 14), (4, 4, 14), kohle)
        w.w((x - 1.5, 0, z - 22), (3, 2.5, 9), kohle_dunkel)
    # Rumpf
    rumpf = m.k("rumpf", "sockel", drehpunkt=(0, 12, 0))
    rumpf.w((-14, 12, -10), (28, 22, 20), kohle)  # Hüfte, halb im Sockel
    rumpf.w((-18, 34, -12), (36, 24, 24), kohle)  # Brust
    rumpf.w((-22, 52, -10), (44, 10, 20), kohle_dunkel)  # Schultern
    # Offene Brust: Rippen vor dem Kern
    rumpf.w((-9, 37, -12.5), (18, 16, 2), kern)
    for x in (-8, -4, 0, 4):
        rumpf.w((x - 1, 35, -14), (2.5, 20, 2.5), knochen_)
    for y in (36, 45, 54):
        rumpf.w((-10, y, -13.8), (20, 2, 2), knochen_)
    herz = m.k("herzkern", "rumpf", drehpunkt=(0, 45, -13))
    herz.w((-4.5, 40, -15), (9, 10, 4), kristall_rot)
    herz.w((-2, 42.5, -16), (4, 5, 1.5), kern_hell)
    # Rückenstachel aus Kristall
    for i, (x, y, rx, rz, h) in enumerate(((-10, 56, -25, 15, 14), (0, 58, -35, 0, 20), (10, 56, -25, -15, 14),
                                           (-14, 46, -40, 30, 10), (14, 46, -40, -30, 10))):
        st = m.k(f"stachel_{i}", "rumpf", drehpunkt=(x, y, 8), drehung=(rx, 0, rz))
        st.w((x - 2, y, 6), (4, h, 4), kristall_rot)
    # Kopf: gesichtslos, ein senkrechter Spalt voller Augen, darüber eine Krone aus Splittern
    kopf = m.k("kopf", "rumpf", drehpunkt=(0, 62, -2), drehung=(10, 0, 0))
    kopf.w((-9, 62, -9), (18, 18, 16), kohle)
    p = {"k": farbe("1a100c"), "s": farbe("0a0605"), "a": farbe("ff7a1a"), "A": farbe("ffe0a0"), "r": farbe("5a1a10")}
    kopf.w((-7, 64, -9.6), (14, 14, 1), kohle, deko=gesicht([
        "kkkkkrssrkkkkk",
        "kkkkrsaAsrkkkk",
        "kkkkrssssrkkkk",
        "kkkrsaAssAsrkk",
        "kkkrssssssrkkk",
        "kkrsAasssaAsrk",
        "kkrssssssssrkk",
        "kkrsaAssaAssrk",
        "kkkrssssssrkkk",
        "kkkrsAaAssrkkk",
        "kkkkrssssrkkkk",
        "kkkkrsaAsrkkkk",
        "kkkkkrssrkkkkk",
        "kkkkkkrrkkkkkk",
    ], p, glow="aA"))
    for i, (x, z, rz, rx, h) in enumerate(((-6, -4, 25, -15, 9), (0, -2, 0, -20, 13), (6, -4, -25, -15, 9),
                                           (-4, 4, 15, 15, 7), (4, 4, -15, 15, 7))):
        kr = m.k(f"krone_{i}", "kopf", drehpunkt=(x, 80, z), drehung=(rx, 0, rz))
        kr.w((x - 1.5, 79, z - 1.5), (3, h, 3), kristall_rot)
    # Kiefer unter dem Kopf, weit offen
    kiefer = m.k("kiefer", "kopf", drehpunkt=(0, 63, -6), drehung=(20, 0, 0))
    kiefer.w((-7, 58, -10), (14, 5, 10), kohle_dunkel)
    for x in (-6, -3, 0, 3, 5):
        kiefer.w((x, 62.5, -10), (1.2, 3, 1.2), knochen_)
    # Arme: gewaltig, nach vorn greifend; Unterarme enden in Kristallklauen
    for s in (1, -1):
        n = name_seite(s)
        arm = m.k("arm_" + n, "rumpf", drehpunkt=(s * 22, 58, 0), drehung=(40, 0, s * 25))
        arm.w((s * 22 - (0 if s > 0 else 12) - (6 if s > 0 else -6) * 0, 32, -6), (12, 26, 12), kohle)
        arm.w((s * 22 - (1 if s > 0 else 13), 50, -7), (14, 8, 14), eisen)  # Ketten-/Rohrreste an der Schulter
        ua = m.k("unterarm_" + n, "arm_" + n, drehpunkt=(s * 28, 33, 0), drehung=(35, 0, -s * 10))
        ua.w((s * 28 - 5, 10, -5), (10, 23, 10), kohle)
        for j, (dx, rz) in enumerate(((-3.5, 12), (0, 0), (3.5, -12))):
            kl = m.k(f"klaue_{n}_{j}", "unterarm_" + n, drehpunkt=(s * 28 + dx, 10, -2), drehung=(25, 0, rz))
            kl.w((s * 28 + dx - 1.2, 0, -3.2), (2.4, 10, 2.4), kristall_rot)
    # Schwebende Splitter um den Koloss (kreisen in der Animation)
    for i in range(6):
        import math
        w = 2 * math.pi * i / 6
        x, z = math.cos(w) * 34, math.sin(w) * 26
        sp = m.k(f"splitter_{i}", drehpunkt=(x, 50 + (i % 2) * 10, z), drehung=(20 * (i % 3), i * 25, 15))
        sp.w((x - 2, 46 + (i % 2) * 10, z - 2), (4, 9, 4), kristall_rot)
    return m, "Das Herz der Zone", "Nether-Endboss · der Ursprung der Emissionen; Schwachstelle: der Kern in der Brust", \
        "≈ 18 × 26 Blöcke"


def mimik():
    """Die Mimik – Enthüllung: Sie trägt Haut und Namen eines Mitspielers. Ist man allein mit ihr, platzt das
    Gesicht senkrecht auf, die Arme reißen aus den Ärmeln und werden zu langen Klauen. Mit einer
    Spieler-Standardhaut, damit man sieht, was im Spiel durch die echte Haut des Mitspielers ersetzt wird."""
    m = Modell("mimik", 128, 128)
    hemd = stoff("3a8fa8", loecher=2, blut=1, falten=3)
    hose = stoff("3a3a8a", loecher=1, blut=1, falten=3)
    haut_ = haut("c49a7a", flecken_f="8a5a4a", blut=1, dunkel_unten=0.1)
    roh = kombi(haut("8a2a2a", flecken_f="5a1010", blut=2, dunkel_unten=0.2))
    zahn = knochen("e8e0c8")
    klaue = knochen("2a2420")
    koerper = m.k("koerper", drehpunkt=(0, 12, 0))
    for s in (1, -1):
        b = m.k("bein_" + name_seite(s), "koerper", drehpunkt=(s * 2, 12, 0))
        b.w((0 if s > 0 else -4, 0, -2), (4, 12, 4), hose)
    ober = m.k("oberkoerper", "koerper", drehpunkt=(0, 12, 0), drehung=(-12, 0, 0))
    ober.w((-4, 12, -2), (8, 12, 4), hemd)
    ober.w((-3, 20, -2.6), (6, 4, 1), roh)  # aufgerissene Brust
    # Kopf: beide Hälften klappen auseinander, dazwischen das Maul
    for s in (1, -1):
        h = m.k("kopf_" + name_seite(s), "oberkoerper", drehpunkt=(0, 24, 0), drehung=(0, 0, s * -28))
        h.w((0 if s > 0 else -4, 24, -4), (4, 8, 8), haut_, deko=gesicht(
            ["....", "....", ".wp." if s > 0 else ".pw.", "....", "....", "....", "....", "...."],
            {"w": farbe("e8e0d8"), "p": farbe("2a1a10")}))
        for j in range(4):
            h.w(((0.1 if s > 0 else -0.9), 25 + j * 1.6, -4.4), (0.8, 1.0, 1.2), zahn)
    maul = m.k("maul", "oberkoerper", drehpunkt=(0, 24, 0))
    maul.w((-1.5, 23, -3.5), (3, 9, 6), roh)
    zunge = m.k("zunge", "maul", drehpunkt=(0, 26, -3), drehung=(-25, 0, 0))
    zunge.w((-0.75, 26, -11), (1.5, 1, 8), haut("9a2a32", flecken_f="5a1018", blut=1))
    # Arme: viel zu lang, aus den Ärmeln gerissen, Klauen nach vorn
    for s in (1, -1):
        n = name_seite(s)
        a = m.k("arm_" + n, "oberkoerper", drehpunkt=(s * 5, 22, 0), drehung=(62, s * 6, s * 6))
        x0 = 4 if s > 0 else -7
        a.w((x0, 15, -1.5), (3, 7, 3), hemd)  # Ärmelrest
        a.w((x0 + 0.5, 2, -1), (2, 13, 2), roh)
        ua = m.k("unterarm_" + n, "arm_" + n, drehpunkt=(s * 5.5, 2, 0), drehung=(20, 0, 0))
        ua.w((x0 + 0.6, -10, -0.9), (1.8, 12, 1.8), haut_)
        for j, rz in enumerate((-12, 0, 12)):
            k = m.k(f"klaue_{n}_{j}", "unterarm_" + n, drehpunkt=(s * 5.5, -10, 0), drehung=(15, 0, rz))
            k.w((x0 + 1.0, -16, -0.3), (0.8, 6, 0.8), klaue)
    return m, "Die Mimik (Enthüllung)", "Trägt Haut und Namen eines Mitspielers · allein mit ihr: Gesicht reißt auf, schreit, greift an", \
        "1,9 Blöcke (aufgerichtet 2,4)"


E.ALLE.update({"das_herz": das_herz, "mimik": mimik})
E.ANSICHTEN.update({"das_herz": ((-25, 8), (70, 4), (200, 12)), "mimik": ((-30, 8), (60, 4), (200, 10))})

if __name__ == "__main__":
    for n in (sys.argv[1:] or ["das_herz", "mimik"]):
        print(DIM.bauen(n))
