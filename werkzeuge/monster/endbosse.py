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

if __name__ == "__main__" and "--anim" not in sys.argv:
    for n in (sys.argv[1:] or ["das_herz", "mimik"]):
        print(DIM.bauen(n))


def herz_muster(AN):
    """Angriffsmuster des Herzens: Glutwelle, Armfeger (einfach/doppelt), Glutstrahl, Glutregen, Erschöpfung, Rufen."""
    anims = []

    def welle(name, laden):
        a = AN.Anim(name, laden + 1.1, schleife=False)
        e = laden + 0.1  # Einschlag
        for arm, s in (("arm_rechts", 1), ("arm_links", -1)):
            a.tasten(arm, "rotation", {0: (0, 0, 0), laden - 0.1: (135, 0, s * 20), e: (-45, 0, s * 10),
                                       e + 0.6: (-40, 0, s * 10), e + 1.0: (0, 0, 0)})
            a.tasten("unterarm_" + arm[4:], "rotation", {0: (0, 0, 0), laden - 0.1: (-30, 0, 0), e: (20, 0, 0), e + 1.0: (0, 0, 0)})
        a.tasten("rumpf", "rotation", {0: (0, 0, 0), laden - 0.1: (-16, 0, 0), e: (26, 0, 0), e + 0.6: (22, 0, 0), e + 1.0: (0, 0, 0)})
        a.tasten("kopf", "rotation", {0: (0, 0, 0), laden - 0.1: (-32, 0, 0), e: (16, 0, 0), e + 1.0: (0, 0, 0)})
        a.tasten("kiefer", "rotation", {0: (0, 0, 0), laden - 0.1: (30, 0, 0), e + 0.3: (30, 0, 0), e + 1.0: (0, 0, 0)})
        schlag = {0: (1, 1, 1)}
        k = 0.0
        while k < laden - 0.2:  # Herz schlägt immer schneller, je näher der Einschlag
            schlag[round(k, 2)] = (1, 1, 1)
            schlag[round(k + 0.08, 2)] = (1.25 + k * 0.15, 1.25 + k * 0.15, 1.25 + k * 0.15)
            k += max(0.16, 0.45 - k * 0.2)
        schlag[round(laden - 0.1, 2)] = (1.6, 1.6, 1.6)
        schlag[round(e + 0.15, 2)] = (0.9, 0.9, 0.9)
        schlag[round(e + 1.0, 2)] = (1, 1, 1)
        a.tasten("herzkern", "scale", schlag)
        a.tasten("sockel", "position", {0: (0, 0, 0), e: (0, -2, 0), e + 0.2: (0, 0, 0)})
        for i in range(6):
            a.tasten(f"splitter_{i}", "position", {0: (0, 0, 0), laden - 0.1: (0, 6, 0), e: (0, -1, 0), e + 1.0: (0, 0, 0)})
        return a

    anims += [welle("welle", 1.5), welle("welle_schnell", 1.1)]

    def fegen(name, warn, doppelt):
        w = warn / 20.0
        laenge = w + (1.55 if doppelt else 0.7)
        a = AN.Anim(name, laenge, schleife=False)
        rumpf = {0: (0, 0, 0), w - 0.05: (-8, 55, 0), w + 0.12: (10, -65, 0)}
        arm_r = {0: (0, 0, 0), w - 0.05: (80, 0, 75), w + 0.12: (35, 0, -40)}
        arm_l = {0: (0, 0, 0), w - 0.05: (20, 0, -30), w + 0.12: (40, 0, -15)}
        if doppelt:
            z = w + 0.9  # zweiter Feger nach hinten
            rumpf.update({z - 0.1: (-6, -140, 0), z + 0.12: (8, -235, 0), laenge: (0, -360, 0)})
            arm_l.update({z - 0.1: (80, 0, -75), z + 0.12: (35, 0, 40), laenge: (0, 0, 0)})
            arm_r.update({z - 0.1: (10, 0, 10), laenge: (0, 0, 0)})
        else:
            rumpf[laenge] = (0, 0, 0)
            arm_r[laenge] = (0, 0, 0)
            arm_l[laenge] = (0, 0, 0)
        a.tasten("rumpf", "rotation", rumpf)
        a.tasten("arm_rechts", "rotation", arm_r)
        a.tasten("arm_links", "rotation", arm_l)
        a.tasten("unterarm_rechts", "rotation", {0: (0, 0, 0), w - 0.05: (-35, 0, 0), w + 0.12: (10, 0, 0), laenge: (0, 0, 0)})
        a.tasten("kopf", "rotation", {0: (0, 0, 0), w - 0.05: (-12, 0, 0), w + 0.12: (8, 0, 0), laenge: (0, 0, 0)})
        a.tasten("kiefer", "rotation", {0: (0, 0, 0), w - 0.05: (25, 0, 0), w + 0.3: (0, 0, 0)})
        return a

    anims += [fegen("fegen", 18, False), fegen("fegen_doppelt", 18, True), fegen("fegen_doppelt_schnell", 13, True)]

    strahl = AN.Anim("strahl", 5.0, schleife=False)
    strahl.tasten("kopf", "rotation", {0: (0, 0, 0), 1.2: (-38, 0, 0), 1.3: (22, 0, 0), 4.4: (18, 0, 0), 5.0: (0, 0, 0)})
    strahl.tasten("kiefer", "rotation", {0: (0, 0, 0), 1.2: (15, 0, 0), 1.3: (50, 0, 0), 4.4: (50, 0, 0), 5.0: (0, 0, 0)})
    strahl.tasten("rumpf", "rotation", {0: (0, 0, 0), 1.2: (-14, 0, 0), 1.3: (12, 0, 0), 4.4: (12, 0, 0), 5.0: (0, 0, 0)})
    for arm, s in (("arm_rechts", 1), ("arm_links", -1)):
        strahl.tasten(arm, "rotation", {0: (0, 0, 0), 1.0: (25, 0, s * 55), 4.4: (25, 0, s * 55), 5.0: (0, 0, 0)})
    pulse = {0: (1, 1, 1), 1.2: (1.5, 1.5, 1.5)}
    k = 1.4
    while k < 4.3:
        pulse[round(k, 2)] = (1.7, 1.7, 1.7)
        pulse[round(k + 0.1, 2)] = (1.45, 1.45, 1.45)
        k += 0.25
    pulse[5.0] = (1, 1, 1)
    strahl.tasten("herzkern", "scale", pulse)
    anims.append(strahl)

    regen = AN.Anim("regen", 2.2, schleife=False)
    for arm, s in (("arm_rechts", 1), ("arm_links", -1)):
        regen.tasten(arm, "rotation", {0: (0, 0, 0), 0.8: (160, 0, s * 35), 1.6: (160, 0, s * 35), 2.2: (0, 0, 0)})
        regen.tasten("unterarm_" + arm[4:], "rotation", {0: (0, 0, 0), 0.8: (-20, 0, 0), 1.6: (-20, 0, 0), 2.2: (0, 0, 0)})
    regen.tasten("kopf", "rotation", {0: (0, 0, 0), 0.8: (-45, 0, 0), 1.6: (-45, 0, 0), 2.2: (0, 0, 0)})
    regen.tasten("kiefer", "rotation", {0: (0, 0, 0), 0.8: (40, 0, 0), 1.6: (40, 0, 0), 2.2: (0, 0, 0)})
    regen.tasten("rumpf", "rotation", {0: (0, 0, 0), 0.8: (-18, 0, 0), 1.6: (-18, 0, 0), 2.2: (0, 0, 0)})
    for i in range(6):
        regen.tasten(f"splitter_{i}", "position", {0: (0, 0, 0), 0.8: (0, 10, 0), 1.6: (0, 10, 0), 2.2: (0, 0, 0)})
    anims.append(regen)

    ersch = AN.Anim("erschoepft", 2.5, schleife=False)
    ersch.tasten("rumpf", "rotation", {0: (0, 0, 0), 0.3: (30, 0, 0), 2.1: (26, 0, 0), 2.5: (0, 0, 0)})
    ersch.tasten("kopf", "rotation", {0: (0, 0, 0), 0.3: (35, 0, 8), 2.1: (32, 0, -6), 2.5: (0, 0, 0)})
    ersch.tasten("kiefer", "rotation", {0: (0, 0, 0), 0.3: (25, 0, 0), 2.1: (25, 0, 0), 2.5: (0, 0, 0)})
    for arm, s in (("arm_rechts", 1), ("arm_links", -1)):
        ersch.tasten(arm, "rotation", {0: (0, 0, 0), 0.3: (-30, 0, s * -20), 2.1: (-28, 0, s * -18), 2.5: (0, 0, 0)})
    ersch.tasten("sockel", "position", {0: (0, 0, 0), 0.3: (0, -3, 0), 2.1: (0, -3, 0), 2.5: (0, 0, 0)})
    offen = {0: (1, 1, 1)}
    k = 0.3
    while k < 2.1:  # freiliegender Kern: groß, unregelmäßig
        offen[round(k, 2)] = (1.6, 1.6, 1.6)
        offen[round(k + 0.15, 2)] = (1.35, 1.35, 1.35)
        k += 0.35
    offen[2.5] = (1, 1, 1)
    ersch.tasten("herzkern", "scale", offen)
    anims.append(ersch)

    rufen = AN.Anim("rufen", 1.8, schleife=False)
    rufen.tasten("kopf", "rotation", {0: (0, 0, 0), 0.5: (-45, 0, 0), 1.3: (-45, 0, 0), 1.8: (0, 0, 0)})
    rufen.tasten("kiefer", "rotation", {0: (0, 0, 0), 0.5: (45, 0, 0), 1.3: (45, 0, 0), 1.8: (0, 0, 0)})
    for arm, s in (("arm_rechts", 1), ("arm_links", -1)):
        rufen.tasten(arm, "rotation", {0: (0, 0, 0), 0.5: (20, 0, s * 70), 1.3: (20, 0, s * 70), 1.8: (0, 0, 0)})
    anims.append(rufen)
    return anims


def ursprung_muster(AN):
    """Angriffsmuster des Ursprungs: Beschwören, Sog mit Nova, Leerestrahlen, Klauen-Kombo, Erschöpfung."""
    anims = []
    besch = AN.Anim("beschwoeren", 1.6, schleife=False)
    for s, v in (("rechts", 1), ("links", -1)):
        besch.tasten(f"arm_{s}", "rotation", {0: (0, 0, 0), 0.5: (70, 0, v * 40), 0.75: (-45, 0, v * 15), 1.6: (0, 0, 0)})
        besch.tasten(f"klaue_{s}", "rotation", {0: (0, 0, 0), 0.5: (-30, 0, 0), 0.75: (25, 0, 0), 1.6: (0, 0, 0)})
    besch.tasten("leib", "rotation", {0: (0, 0, 0), 0.5: (-10, 0, 0), 0.75: (12, 0, 0), 1.6: (0, 0, 0)})
    for i in range(8):
        besch.tasten(f"tentakel_{i}_0", "rotation", {0: (0, 0, 0), 0.5: (-55, 0, 0), 0.8: (35, 0, 0), 1.6: (0, 0, 0)})
        besch.tasten(f"tentakel_{i}_1", "rotation", {0: (0, 0, 0), 0.5: (-20, 0, 0), 0.8: (25, 0, 0), 1.6: (0, 0, 0)})
    besch.tasten("kristallkern", "scale", {0: (1, 1, 1), 0.5: (1.5, 1.5, 1.5), 0.8: (1, 1, 1)})
    anims.append(besch)

    def sog(name, dauer):
        a = AN.Anim(name, dauer + 0.8, schleife=False)
        n = dauer + 0.05  # Nova
        for s, v in (("rechts", 1), ("links", -1)):
            a.tasten(f"arm_{s}", "rotation", {0: (0, 0, 0), 0.6: (25, 0, v * 75), dauer - 0.1: (35, 0, v * 40),
                                              n: (75, 0, v * 115), dauer + 0.8: (0, 0, 0)})
            a.tasten(f"unterarm_{s}", "rotation", {0: (0, 0, 0), 0.6: (-25, 0, 0), n: (20, 0, 0), dauer + 0.8: (0, 0, 0)})
            a.tasten(f"kiefer_{s}", "rotation", {0: (0, 0, 0), 0.6: (0, v * -30, 0), n: (0, v * -45, 0), dauer + 0.8: (0, 0, 0)})
        a.tasten("leib", "rotation", {0: (0, 0, 0), 0.6: (-12, 0, 0), dauer - 0.1: (8, 0, 0), n: (-18, 0, 0), dauer + 0.8: (0, 0, 0)})
        a.tasten("reaktorring", "rotation", {0: (0, 0, 0), dauer: (0, 900, 0), n: (0, 960, 0), dauer + 0.8: (0, 1080, 0)})
        a.tasten("kristallkern", "scale", {0: (1, 1, 1), dauer - 0.1: (2.3, 2.3, 2.3), n: (0.6, 0.6, 0.6), dauer + 0.8: (1, 1, 1)})
        for i in range(8):
            a.tasten(f"tentakel_{i}_0", "rotation", {0: (0, 0, 0), 0.6: (-65, 0, 0), dauer - 0.1: (-50, 0, 0), n: (45, 0, 0),
                                                     dauer + 0.8: (0, 0, 0)})
            a.tasten(f"tentakel_{i}_2", "rotation", {0: (0, 0, 0), 0.6: (-30, 0, 0), n: (30, 0, 0), dauer + 0.8: (0, 0, 0)})
        return a

    anims += [sog("sog", 2.5), sog("sog_schnell", 2.0)]

    strahlen = AN.Anim("strahlen", 6.0, schleife=False)
    for s, v in (("rechts", 1), ("links", -1)):
        strahlen.tasten(f"arm_{s}", "rotation", {0: (0, 0, 0), 0.8: (-20, 0, v * 35), 5.6: (-20, 0, v * 35), 6.0: (0, 0, 0)})
        strahlen.tasten(f"unterarm_{s}", "rotation", {0: (0, 0, 0), 0.8: (0, 0, v * 40), 5.6: (0, 0, v * 40), 6.0: (0, 0, 0)})
    strahlen.tasten("kopf", "rotation", {0: (0, 0, 0), 0.8: (-22, 0, 0), 5.6: (-22, 0, 0), 6.0: (0, 0, 0)})
    strahlen.tasten("reaktorring", "rotation", {0: (0, 0, 0), 1.2: (0, 180, 0), 5.6: (0, 1980, 0), 6.0: (0, 2160, 0)})
    strahlen.tasten("kristallkern", "scale", {0: (1, 1, 1), 1.2: (1.7, 1.7, 1.7), 5.6: (1.7, 1.7, 1.7), 6.0: (1, 1, 1)})
    for i in range(8):
        strahlen.tasten(f"tentakel_{i}_0", "rotation", {0: (0, 0, 0), 1.2: (-45, 0, 0), 5.6: (-45, 0, 0), 6.0: (0, 0, 0)})
        strahlen.welle(f"tentakel_{i}_1", "z", 12, phase=i / 8, frequenz=6)
    anims.append(strahlen)

    def hieb(name, warn, seiten, stark):
        w = warn / 20.0
        a = AN.Anim(name, w + 0.7, schleife=False)
        for s in seiten:
            v = 1 if s == "rechts" else -1
            hoch = 150 if stark else 130
            a.tasten(f"arm_{s}", "rotation", {0: (0, 0, 0), w - 0.05: (hoch, 0, v * -15), w + 0.1: (-45, 0, v * 10),
                                              w + 0.4: (-40, 0, v * 10), w + 0.7: (0, 0, 0)})
            a.tasten(f"unterarm_{s}", "rotation", {0: (0, 0, 0), w - 0.05: (-50, 0, 0), w + 0.1: (30, 0, 0), w + 0.7: (0, 0, 0)})
            a.tasten(f"klaue_{s}", "rotation", {0: (0, 0, 0), w - 0.05: (-35, 0, 0), w + 0.1: (30, 0, 0), w + 0.7: (0, 0, 0)})
        dreh = 0 if len(seiten) == 2 else (18 if seiten[0] == "rechts" else -18)
        a.tasten("leib", "rotation", {0: (0, 0, 0), w - 0.05: (-12, -dreh, 0), w + 0.1: (16 if stark else 10, dreh, 0),
                                      w + 0.7: (0, 0, 0)})
        a.tasten("kopf", "rotation", {0: (0, 0, 0), w - 0.05: (-10, 0, 0), w + 0.1: (12, 0, 0), w + 0.7: (0, 0, 0)})
        if stark:
            a.tasten("leib", "position", {0: (0, 0, 0), w - 0.05: (0, 3, 0), w + 0.1: (0, -2, 0), w + 0.5: (0, 0, 0)})
        return a

    for st, warn in enumerate((14, 12, 9)):
        anims.append(hieb(f"hieb_links_{st}", warn, ["links"], False))
        anims.append(hieb(f"hieb_rechts_{st}", warn, ["rechts"], False))
        anims.append(hieb(f"doppelhieb_{st}", warn, ["rechts", "links"], True))

    ersch = AN.Anim("erschoepft", 3.0, schleife=False)
    ersch.tasten("leib", "rotation", {0: (0, 0, 0), 0.3: (24, 0, 0), 2.6: (20, 0, 4), 3.0: (0, 0, 0)})
    ersch.tasten("leib", "position", {0: (0, 0, 0), 0.3: (0, -5, 0), 2.6: (0, -4, 0), 3.0: (0, 0, 0)})
    ersch.tasten("kopf", "rotation", {0: (0, 0, 0), 0.3: (25, 0, -10), 2.6: (22, 0, 8), 3.0: (0, 0, 0)})
    for s, v in (("rechts", 1), ("links", -1)):
        ersch.tasten(f"arm_{s}", "rotation", {0: (0, 0, 0), 0.3: (-35, 0, v * -25), 2.6: (-32, 0, v * -22), 3.0: (0, 0, 0)})
        flackern = {0: (0, 0, 0)}
        k = 0.3
        i = 0
        while k < 2.6:  # Lider flackern: das Auge ist ungeschützt
            flackern[round(k, 2)] = (0, (v * 2.5) if i % 2 == 0 else 0, 0)
            k += 0.12 + (i % 3) * 0.07
            i += 1
        flackern[3.0] = (0, 0, 0)
        ersch.tasten(f"lid_{s}", "position", flackern)
    ersch.tasten("kristallkern", "scale", {0: (1, 1, 1), 0.3: (0.7, 0.7, 0.7), 2.6: (0.75, 0.75, 0.75), 3.0: (1, 1, 1)})
    for i in range(8):
        ersch.tasten(f"tentakel_{i}_0", "rotation", {0: (0, 0, 0), 0.3: (28, 0, 0), 2.6: (25, 0, 0), 3.0: (0, 0, 0)})
    anims.append(ersch)
    return anims


def herz_animationen():
    """Animationen für das Herz der Zone (schreibt das_herz.animation.json)."""
    import animationen as AN
    idle = AN.Anim("idle", 2.4)
    for t0 in (0.0, 1.2):  # Herzschlag: zwei Schläge pro Sekunde-ish
        idle.tasten("herzkern", "scale", {t0: (1, 1, 1), t0 + 0.12: (1.18, 1.18, 1.18), t0 + 0.3: (1.05, 1.05, 1.05),
                                          t0 + 0.42: (1.12, 1.12, 1.12), t0 + 0.8: (1, 1, 1)})
    AN.atmen(idle, "rumpf", 2)
    idle.welle("kopf", "z", 4)
    idle.welle("arm_rechts", "z", 3)
    idle.welle("arm_links", "z", 3, phase=0.5)
    for i in range(6):
        idle.welle(f"splitter_{i}", "y", 2.5, art="position", phase=i / 6)
        idle.welle(f"splitter_{i}", "y", 40, phase=i / 6)
    gang = AN.Anim("walk", 2.4)  # steht fest im Glutsee – nur Unruhe
    AN.atmen(gang, "rumpf", 3)
    gang.welle("kopf", "y", 10)
    for i in range(6):
        gang.welle(f"splitter_{i}", "y", 3, art="position", phase=i / 6)
    schlag = AN.Anim("attack", 1.4, schleife=False)
    for arm in ("arm_rechts", "arm_links"):
        schlag.tasten(arm, "rotation", {0: (0, 0, 0), 0.6: (110, 0, 0), 0.85: (-25, 0, 0), 1.4: (0, 0, 0)})
    schlag.tasten("rumpf", "rotation", {0: (0, 0, 0), 0.6: (-10, 0, 0), 0.85: (18, 0, 0), 1.4: (0, 0, 0)})
    emission = AN.Anim("special", 2.2, schleife=False)
    emission.tasten("rumpf", "rotation", {0: (0, 0, 0), 0.7: (-18, 0, 0), 1.6: (-18, 0, 0), 2.2: (0, 0, 0)})
    emission.tasten("kopf", "rotation", {0: (0, 0, 0), 0.7: (-25, 0, 0), 1.6: (-25, 0, 0), 2.2: (0, 0, 0)})
    for arm, s in (("arm_rechts", 1), ("arm_links", -1)):
        emission.tasten(arm, "rotation", {0: (0, 0, 0), 0.7: (30, 0, s * 45), 1.6: (30, 0, s * 45), 2.2: (0, 0, 0)})
    emission.tasten("herzkern", "scale", {0: (1, 1, 1), 0.7: (1.5, 1.5, 1.5), 1.6: (1.5, 1.5, 1.5), 2.2: (1, 1, 1)})
    auftritt = AN.boss_auftritt("das_herz", knochen="sockel", tiefe=-60, laenge=5.0)
    anims = [idle, gang, schlag, emission, auftritt] + AN.kampf_anims("das_herz") + herz_muster(AN)
    import json
    import os
    daten = {"format_version": "1.8.0", "animations": {f"animation.das_herz.{a.name}": a.json() for a in anims}}
    with open(os.path.join(AN.ZIEL, "das_herz.animation.json"), "w") as f:
        json.dump(daten, f)
    print("Animation das_herz geschrieben")


if __name__ == "__main__" and "--anim" in sys.argv:
    herz_animationen()


def ursprung_animationen():
    """Animationen für den Ursprung (schreibt der_ursprung.animation.json)."""
    import json
    import os
    import animationen as AN
    idle = AN.Anim("idle", 3.2)
    AN.atmen(idle, "leib", 2.5)
    idle.tasten("kristallkern", "scale", {0: (1, 1, 1), 0.4: (1.15, 1.15, 1.15), 1.0: (1, 1, 1), 3.2: (1, 1, 1)})
    idle.welle("kopf", "z", 4)
    idle.welle("reaktorring", "y", 30, frequenz=1)
    for i in range(8):
        for g in range(4):
            idle.welle(f"tentakel_{i}_{g}", "x", 6 + g * 4, phase=i / 8 + g * 0.1)
            idle.welle(f"tentakel_{i}_{g}", "z", 4 + g * 3, phase=i / 8 + g * 0.1 + 0.25)
    for s in ("rechts", "links"):
        idle.welle(f"arm_{s}", "x", 5, phase=0.3 if s == "rechts" else 0.8)
    gang = AN.Anim("walk", 3.2)
    AN.atmen(gang, "leib", 4)
    gang.welle("leib", "y", 1.5, art="position")
    gang.welle("reaktorring", "y", 60, frequenz=1)
    for i in range(8):
        for g in range(4):
            gang.welle(f"tentakel_{i}_{g}", "x", 12 + g * 5, phase=i / 8 + g * 0.12, frequenz=2)
    schlag = AN.Anim("attack", 1.6, schleife=False)
    for s in ("rechts", "links"):
        schlag.tasten(f"arm_{s}", "rotation", {0: (0, 0, 0), 0.7: (120, 0, 0), 0.95: (-30, 0, 0), 1.6: (0, 0, 0)})
        schlag.tasten(f"unterarm_{s}", "rotation", {0: (0, 0, 0), 0.7: (-40, 0, 0), 0.95: (30, 0, 0), 1.6: (0, 0, 0)})
    schlag.tasten("leib", "rotation", {0: (0, 0, 0), 0.7: (-8, 0, 0), 0.95: (14, 0, 0), 1.6: (0, 0, 0)})
    puls = AN.Anim("special", 2.4, schleife=False)
    puls.tasten("kristallkern", "scale", {0: (1, 1, 1), 0.8: (1.8, 1.8, 1.8), 1.6: (1.8, 1.8, 1.8), 2.4: (1, 1, 1)})
    for s, v in (("rechts", 1), ("links", -1)):
        puls.tasten(f"arm_{s}", "rotation", {0: (0, 0, 0), 0.8: (20, 0, v * 50), 1.6: (20, 0, v * 50), 2.4: (0, 0, 0)})
        puls.tasten(f"kiefer_{s}", "rotation", {0: (0, 0, 0), 0.8: (0, v * -35, 0), 1.6: (0, v * -35, 0), 2.4: (0, 0, 0)})
    for i in range(8):
        puls.tasten(f"tentakel_{i}_0", "rotation", {0: (0, 0, 0), 0.8: (-35, 0, 0), 1.6: (-35, 0, 0), 2.4: (0, 0, 0)})
    # Epischer Auftritt: steigt aus dem Reaktorring, entfaltet Arme und Tentakel, öffnet das Auge
    auftritt = AN.Anim("spawn", 8.0, schleife=None)
    auftritt.tasten("leib", "position", {0: (0, -90, 0), 3.5: (0, -30, 0), 6.0: (0, 4, 0), 8.0: (0, 0, 0)})
    auftritt.tasten("leib", "rotation", {0: (30, 0, 0), 4.0: (20, 0, 0), 6.0: (-12, 0, 0), 8.0: (0, 0, 0)})
    for s, v in (("rechts", 1), ("links", -1)):
        auftritt.tasten(f"arm_{s}", "rotation", {0: (-60, 0, v * -60), 5.0: (-60, 0, v * -60), 6.5: (60, 0, v * 70),
                                                  8.0: (0, 0, 0)})
        auftritt.tasten(f"lid_{s}", "position", {0: (0, 0, 0), 6.5: (0, 0, 0), 7.2: (0, v * 3, 0), 8.0: (0, 0, 0)})
    for i in range(8):
        auftritt.tasten(f"tentakel_{i}_0", "rotation", {0: (-80, 0, 0), 5.5: (-80, 0, 0), 7.0: (20, 0, 0), 8.0: (0, 0, 0)})
    auftritt.tasten("kristallkern", "scale", {0: (0.3, 0.3, 0.3), 6.0: (0.3, 0.3, 0.3), 7.0: (2, 2, 2), 8.0: (1, 1, 1)})
    anims = [idle, gang, schlag, puls, auftritt] + AN.kampf_anims("der_ursprung") + ursprung_muster(AN)
    daten = {"format_version": "1.8.0", "animations": {f"animation.der_ursprung.{a.name}": a.json() for a in anims}}
    with open(os.path.join(AN.ZIEL, "der_ursprung.animation.json"), "w") as f:
        json.dump(daten, f)
    print("Animation der_ursprung geschrieben")


if __name__ == "__main__" and "--anim" in sys.argv:
    ursprung_animationen()
