package de.deadzonemc.welt.plan;

import net.minecraft.core.Direction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Plant ein Dorf: eine Hauptstraße mit Ausfallstraßen an beiden Enden, oft eine Querstraße,
 * Grundstücke links und rechts, eine Bushaltestelle, manchmal eine Tankstelle vor dem Ort,
 * Ortsschilder, parkende Autos und ein Stau von Leuten, die fliehen wollten.
 */
public final class Dorfplaner {

    private Dorfplaner() {
    }

    /** Ein Grundstückstyp mit Maßen und Regeln. */
    private record Art(String typ, int breiteMin, int breiteMax, int tiefeMin, int tiefeMax, int gewicht, int max,
                       double zoneMin, double zoneMax) {
    }

    private static final Art[] ARTEN = {
            new Art("einfamilienhaus", 17, 23, 22, 28, 40, 99, 0.0, 1.2),
            new Art("bungalow", 19, 24, 20, 24, 14, 99, 0.0, 1.2),
            new Art("bauernhof", 38, 46, 40, 48, 9, 3, 0.45, 1.2),
            new Art("kirche", 26, 30, 40, 46, 0, 1, 0.0, 0.6),
            new Art("dorfladen", 16, 20, 18, 22, 6, 2, 0.0, 0.65),
            new Art("gasthaus", 18, 22, 22, 26, 6, 1, 0.0, 0.65),
            new Art("feuerwehr", 19, 23, 22, 26, 5, 1, 0.0, 1.0),
            new Art("werkstatt", 17, 21, 20, 24, 5, 1, 0.0, 1.2),
            new Art("ruine", 14, 20, 16, 22, 8, 99, 0.0, 1.2),
            new Art("garten", 12, 18, 14, 20, 6, 99, 0.0, 1.2),
    };

    // ---- Geometrie (nur aus dem Startwert) ---------------------------------------------------

    public static Ortsplan geometrie(Siedlung s) {
        Ortsplan plan = new Ortsplan(s);
        Random r = new Random(s.saat());
        int R = s.radius();
        boolean entlangX = r.nextBoolean();
        int mitteLaengs = entlangX ? s.x() : s.z();
        int mitteQuer = entlangX ? s.z() : s.x();

        int ausfallMinus = 48 + r.nextInt(44);
        int ausfallPlus = 48 + r.nextInt(44);
        Strasse haupt = new Strasse(entlangX, mitteQuer, mitteLaengs - R - ausfallMinus, mitteLaengs + R + ausfallPlus,
                Strasse.Typ.DORFSTRASSE, mitteLaengs - R, mitteLaengs + R, r.nextLong(), "Hauptstraße");
        plan.strassen.add(haupt);

        // Querstraße (manchmal nur zu einer Seite: T-Kreuzung)
        if (r.nextDouble() < 0.7) {
            int pos = mitteLaengs + r.nextInt(R) - R / 2;
            int lang1 = r.nextInt(3) == 0 ? 0 : (int) (R * (0.6 + r.nextDouble() * 0.35));
            int lang2 = (int) (R * (0.6 + r.nextDouble() * 0.35));
            if (r.nextBoolean()) {
                int t = lang1;
                lang1 = lang2;
                lang2 = t;
            }
            int von = mitteQuer - Math.max(lang1, haupt.maxHalbeBreite() + 1);
            int bis = mitteQuer + Math.max(lang2, haupt.maxHalbeBreite() + 1);
            long saat = r.nextLong();
            plan.strassen.add(new Strasse(!entlangX, pos, von, bis, Strasse.Typ.DORFSTRASSE, von, bis, saat,
                    Ortsnamen.strasse(saat)));
        }

        Map<String, Integer> anzahl = new HashMap<>();

        // Bushaltestelle nahe der Mitte
        {
            int seite = r.nextBoolean() ? 1 : -1;
            int a = mitteLaengs + 4 + r.nextInt(10);
            Grundstueck bus = grundstueck(haupt, 0, seite, a, 6, 4, "bushaltestelle", r.nextLong(), 0);
            if (frei(plan, bus.flaeche())) {
                plan.grundstuecke.add(bus);
            }
        }

        // Tankstelle vor dem Ort an einer Ausfallstraße
        if (r.nextDouble() < 0.7) {
            boolean plus = r.nextBoolean();
            int seite = r.nextBoolean() ? 1 : -1;
            int breite = 26 + r.nextInt(5);
            int a = plus ? haupt.ortBis + 6 : haupt.ortVon - 6 - breite;
            Grundstueck tanke = grundstueck(haupt, 0, seite, a, breite, 22 + r.nextInt(5), "tankstelle", r.nextLong(), 0);
            if (a >= haupt.von && a + breite <= haupt.bis && frei(plan, tanke.flaeche())) {
                plan.grundstuecke.add(tanke);
            }
        }

        // Grundstücke an allen Straßen
        int nummer = 1;
        for (int si = 0; si < plan.strassen.size(); si++) {
            Strasse st = plan.strassen.get(si);
            for (int seite : new int[]{-1, 1}) {
                int cursor = st.ortVon + 2 + r.nextInt(4);
                while (cursor < st.ortBis - 10) {
                    double zone = zone(s, st, cursor);
                    Art art = waehlen(r, zone, anzahl);
                    int breite = art.breiteMin + r.nextInt(art.breiteMax - art.breiteMin + 1);
                    int tiefe = art.tiefeMin + r.nextInt(art.tiefeMax - art.tiefeMin + 1);
                    Grundstueck g = grundstueck(st, si, seite, cursor, breite, tiefe, art.typ, r.nextLong(), nummer);
                    double abstand = s.abstand(g.mitteX(), g.mitteZ());
                    if (cursor + breite > st.ortBis + 2 || abstand > R + 14 || !frei(plan, g.flaeche())) {
                        cursor += 4;
                        continue;
                    }
                    plan.grundstuecke.add(g);
                    anzahl.merge(art.typ, 1, Integer::sum);
                    nummer++;
                    int luecke = r.nextInt(5) == 0 ? 6 + r.nextInt(10) : 1 + r.nextInt(4);
                    cursor += breite + luecke;
                }
            }
        }

        autosPlanen(plan, haupt, r);
        schilderPlanen(plan, haupt);
        return plan;
    }

    private static double zone(Siedlung s, Strasse st, int laengs) {
        int x = st.x(laengs, 0);
        int z = st.z(laengs, 0);
        return s.abstand(x, z) / s.radius();
    }

    private static Art waehlen(Random r, double zone, Map<String, Integer> anzahl) {
        // Die Kirche bekommt in der Ortsmitte eine gute Chance, solange es noch keine gibt.
        if (zone < 0.5 && anzahl.getOrDefault("kirche", 0) == 0 && r.nextInt(4) == 0) {
            return ARTEN[3];
        }
        int summe = 0;
        for (Art a : ARTEN) {
            if (passt(a, zone, anzahl)) {
                summe += a.gewicht;
            }
        }
        int wurf = r.nextInt(Math.max(1, summe));
        for (Art a : ARTEN) {
            if (!passt(a, zone, anzahl)) {
                continue;
            }
            wurf -= a.gewicht;
            if (wurf < 0) {
                return a;
            }
        }
        return ARTEN[0];
    }

    private static boolean passt(Art a, double zone, Map<String, Integer> anzahl) {
        return a.gewicht > 0 && zone >= a.zoneMin && zone <= a.zoneMax && anzahl.getOrDefault(a.typ, 0) < a.max;
    }

    /** Grundstück an einer Straßenseite (seite = +1: größere Quer-Koordinate). */
    private static Grundstueck grundstueck(Strasse st, int index, int seite, int laengsStart, int breite, int tiefe,
                                           String typ, long saat, int nummer) {
        int hb = st.halbeBreite(laengsStart + breite / 2);
        int q1 = seite > 0 ? st.quer + hb + 1 : st.quer - hb - tiefe;
        int q2 = seite > 0 ? st.quer + hb + tiefe : st.quer - hb - 1;
        int l1 = laengsStart;
        int l2 = laengsStart + breite - 1;
        Direction vorne;
        Grundstueck g;
        if (st.entlangX) {
            vorne = seite > 0 ? Direction.NORTH : Direction.SOUTH;
            g = new Grundstueck(l1, q1, l2, q2, vorne, typ, saat, st.name, nummer);
        } else {
            vorne = seite > 0 ? Direction.WEST : Direction.EAST;
            g = new Grundstueck(q1, l1, q2, l2, vorne, typ, saat, st.name, nummer);
        }
        g.strassenIndex = index;
        g.laengsMitte = laengsStart + breite / 2;
        return g;
    }

    private static boolean frei(Ortsplan plan, int[] f) {
        for (Strasse s : plan.strassen) {
            if (Strasse.ueberlappt(f, s.flaeche(), 1)) {
                return false;
            }
        }
        for (Grundstueck g : plan.grundstuecke) {
            if (Strasse.ueberlappt(f, g.flaeche(), 1)) {
                return false;
            }
        }
        for (Objekt o : plan.objekte) {
            if (Strasse.ueberlappt(f, o.flaeche(), 1)) {
                return false;
            }
        }
        return true;
    }

    /** Ob eine Fläche eine andere Fahrbahn als die eigene berührt (Kreuzungen frei halten). */
    private static boolean aufKreuzung(Ortsplan plan, Strasse eigene, int[] f, int rand) {
        for (Strasse s : plan.strassen) {
            if (s != eigene && Strasse.ueberlappt(f, s.flaeche(), rand)) {
                return true;
            }
        }
        return false;
    }

    private static int[] autoFlaeche(Strasse st, int laengs, int querVersatz) {
        // Auto: 5 lang, 3 breit; querVersatz ist die erste Querreihe
        return st.entlangX
                ? new int[]{laengs, st.quer + querVersatz, laengs + 4, st.quer + querVersatz + 2}
                : new int[]{st.quer + querVersatz, laengs, st.quer + querVersatz + 2, laengs + 4};
    }

    private static void autoHinzu(Ortsplan plan, Strasse st, int laengs, int querVersatz, String typ, long saat) {
        int[] f = autoFlaeche(st, laengs, querVersatz);
        if (laengs < st.von || laengs + 4 > st.bis || aufKreuzung(plan, st, f, 4)) {
            return;
        }
        for (Grundstueck g : plan.grundstuecke) {
            if (Strasse.ueberlappt(f, g.flaeche(), 0)) {
                return;
            }
        }
        for (Objekt o : plan.objekte) {
            if (Strasse.ueberlappt(f, o.flaeche(), 1)) {
                return;
            }
        }
        plan.objekte.add(new Objekt(typ, f[0], f[1], st.entlangX, 0, saat, ""));
    }

    private static void autosPlanen(Ortsplan plan, Strasse haupt, Random r) {
        // Geparkt am Fahrbahnrand
        for (Strasse st : plan.strassen) {
            for (int a = st.ortVon + 3; a < st.ortBis - 6; a += 9 + r.nextInt(8)) {
                if (r.nextInt(10) < 3) {
                    autoHinzu(plan, st, a, r.nextBoolean() ? -2 : 0, "auto", r.nextLong());
                }
            }
        }
        // Stau auf einer Ausfallstraße: alle wollten weg
        if (r.nextDouble() < 0.45) {
            boolean plus = r.nextBoolean();
            int spur = plus ? 0 : -2;
            int anzahl = 4 + r.nextInt(5);
            for (int i = 0; i < anzahl; i++) {
                int a = plus ? haupt.ortBis + 2 + i * 6 : haupt.ortVon - 7 - i * 6;
                autoHinzu(plan, haupt, a, spur, r.nextInt(6) == 0 ? "auto_brand" : "auto", r.nextLong());
                if (r.nextInt(3) == 0) {
                    autoHinzu(plan, haupt, a + r.nextInt(3), -spur - 2, "auto", r.nextLong());
                }
            }
        }
        // Unfall an der Kreuzung
        if (plan.strassen.size() > 1 && r.nextBoolean()) {
            Strasse quer = plan.strassen.get(1);
            int a = quer.quer - quer.maxHalbeBreite() - 6;
            int[] f = autoFlaeche(haupt, a, -2);
            plan.objekte.add(new Objekt("auto_brand", f[0], f[1], haupt.entlangX, 0, r.nextLong(), ""));
        }
    }

    private static void schilderPlanen(Ortsplan plan, Strasse haupt) {
        String name = plan.siedlung.name();
        int abstand = haupt.typ.halb + 2;
        // Am Ortseingang rechts (aus Sicht der Ankommenden)
        plan.objekte.add(new Objekt("ortsschild", haupt.x(haupt.ortVon - 3, abstand), haupt.z(haupt.ortVon - 3, abstand),
                haupt.entlangX, 1, 0, name));
        plan.objekte.add(new Objekt("ortsschild", haupt.x(haupt.ortBis + 3, -abstand), haupt.z(haupt.ortBis + 3, -abstand),
                haupt.entlangX, 0, 0, name));
    }

    // ---- Ins Gelände einpassen ---------------------------------------------------------------

    public static Ortsplan einpassen(Siedlung s, Hoehen h, int meer) {
        Ortsplan plan = geometrie(s);
        if (h.boden(s.x(), s.z()) < meer) {
            plan.gueltig = false;
            return plan;
        }
        Strasse haupt = plan.strassen.get(0);
        int[] kreuzungen = new int[plan.strassen.size() - 1];
        for (int i = 1; i < plan.strassen.size(); i++) {
            kreuzungen[i - 1] = plan.strassen.get(i).quer;
        }
        haupt.hoehe = strassenHoehen(haupt, h, meer, kreuzungen, false, 0);
        // Zu viel Wasser auf der Hauptstraße im Ort -> kein Dorf
        int nass = 0;
        int gesamt = 0;
        for (int a = haupt.ortVon; a <= haupt.ortBis; a += 8) {
            gesamt++;
            if (h.boden(haupt.x(a, 0), haupt.z(a, 0)) < meer - 1) {
                nass++;
            }
        }
        if (nass * 10 > gesamt * 3) {
            plan.gueltig = false;
            return plan;
        }
        for (int i = 1; i < plan.strassen.size(); i++) {
            Strasse st = plan.strassen.get(i);
            int anschluss = st.quer; // Längskoordinate der Hauptstraße an der Kreuzung
            int zielHoehe = haupt.hoeheBei(anschluss);
            st.hoehe = strassenHoehen(st, h, meer, new int[]{haupt.quer}, true, zielHoehe);
        }

        List<Grundstueck> behalten = new ArrayList<>();
        for (Grundstueck g : plan.grundstuecke) {
            Strasse st = plan.strassen.get(Math.max(0, g.strassenIndex));
            g.y = st.hoeheBei(g.laengsMitte);
            int mitte = h.boden(g.mitteX(), g.mitteZ());
            if (mitte < meer - 1) {
                continue; // im Wasser
            }
            int abweichung = 0;
            for (int[] p : new int[][]{{g.minX + 2, g.minZ + 2}, {g.maxX - 2, g.minZ + 2}, {g.minX + 2, g.maxZ - 2},
                    {g.maxX - 2, g.maxZ - 2}, {g.mitteX(), g.mitteZ()}}) {
                abweichung = Math.max(abweichung, Math.abs(Math.max(meer, h.boden(p[0], p[1])) - g.y));
            }
            if (abweichung > 11) {
                continue; // zu steil
            }
            behalten.add(g);
        }
        plan.grundstuecke.clear();
        plan.grundstuecke.addAll(behalten);

        for (Objekt o : plan.objekte) {
            Strasse naechste = haupt;
            for (Strasse st : plan.strassen) {
                if (Strasse.enthaelt(st.flaeche(), o.x, o.z) || Strasse.ueberlappt(st.flaeche(), o.flaeche(), 2)) {
                    naechste = st;
                    break;
                }
            }
            int laengs = naechste.entlangX ? o.x : o.z;
            o.y = naechste.hoeheBei(laengs);
        }
        return plan;
    }

    /**
     * Höhenverlauf einer Straße: Gelände alle 4 Blöcke abtasten, glätten, Steigung auf einen Block
     * je 4 Blöcke begrenzen. An Kreuzungen (flach) bleibt die Straße auf einer Höhe; mit
     * {@code mitZiel} trifft sie an der ersten Kreuzung genau die Höhe {@code ziel}.
     */
    static int[] strassenHoehen(Strasse st, Hoehen h, int meer, int[] flach, boolean mitZiel, int ziel) {
        int laenge = st.bis - st.von + 1;
        int n = laenge / 4 + 2;
        double[] t = new double[n];
        for (int k = 0; k < n; k++) {
            int a = Math.min(st.bis, st.von + k * 4);
            t[k] = Math.max(meer, h.boden(st.x(a, 0), st.z(a, 0)));
        }
        for (int durchgang = 0; durchgang < 4; durchgang++) {
            double[] neu = new double[n];
            for (int k = 0; k < n; k++) {
                double links = t[Math.max(0, k - 1)];
                double rechts = t[Math.min(n - 1, k + 1)];
                neu[k] = (links + 2 * t[k] + rechts) / 4.0;
            }
            t = neu;
        }
        int[] knoten = new int[n];
        for (int k = 0; k < n; k++) {
            knoten[k] = (int) Math.round(t[k]);
        }
        int anker = -1;
        for (int i = 0; i < flach.length; i++) {
            int k0 = Math.max(0, Math.min(n - 1, (flach[i] - st.von) / 4));
            int wert = (i == 0 && mitZiel) ? ziel : knoten[k0];
            if (i == 0 && mitZiel) {
                int delta = ziel - knoten[k0];
                for (int k = Math.max(0, k0 - 6); k <= Math.min(n - 1, k0 + 6); k++) {
                    knoten[k] += (int) Math.round(delta * (1.0 - Math.abs(k - k0) / 7.0));
                }
            }
            for (int k = Math.max(0, k0 - 3); k <= Math.min(n - 1, k0 + 3); k++) {
                knoten[k] = wert;
            }
            if (anker < 0) {
                anker = k0;
            }
        }
        // Steigung begrenzen, vom Anker (Kreuzung) aus nach beiden Seiten
        int start = Math.max(0, anker);
        for (int k = start + 1; k < n; k++) {
            knoten[k] = Math.max(knoten[k - 1] - 1, Math.min(knoten[k - 1] + 1, knoten[k]));
        }
        for (int k = start - 1; k >= 0; k--) {
            knoten[k] = Math.max(knoten[k + 1] - 1, Math.min(knoten[k + 1] + 1, knoten[k]));
        }
        int[] hoehe = new int[laenge];
        for (int i = 0; i < laenge; i++) {
            hoehe[i] = knoten[Math.min(n - 1, i / 4)];
        }
        return hoehe;
    }
}
