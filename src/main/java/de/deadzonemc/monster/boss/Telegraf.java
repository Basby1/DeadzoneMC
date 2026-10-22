package de.deadzonemc.monster.boss;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.List;

/**
 * Vorwarnungen für Boss-Angriffsmuster: Ringe, Linien und Kegel aus Partikeln am Boden, damit jeder Angriff lesbar ist –
 * wer hinschaut, kann ausweichen. Außerdem Hilfen für Trefferprüfungen.
 */
final class Telegraf {

    static final DustParticleOptions GLUT = new DustParticleOptions(new Vector3f(1.0F, 0.32F, 0.04F), 1.8F);
    static final DustParticleOptions GLUT_HELL = new DustParticleOptions(new Vector3f(1.0F, 0.85F, 0.3F), 2.2F);
    static final DustParticleOptions LEERE = new DustParticleOptions(new Vector3f(0.62F, 0.12F, 0.95F), 1.8F);
    static final DustParticleOptions LEERE_HELL = new DustParticleOptions(new Vector3f(0.9F, 0.75F, 1.0F), 2.2F);

    private Telegraf() {
    }

    /** Ring am Boden. */
    static void ring(ServerLevel sl, Vec3 mitte, double radius, ParticleOptions p) {
        int n = Math.max(12, (int) (radius * 5));
        for (int i = 0; i < n; i++) {
            double w = i / (double) n * Math.PI * 2;
            sl.sendParticles(p, mitte.x + Math.cos(w) * radius, mitte.y + 0.15, mitte.z + Math.sin(w) * radius, 1, 0, 0, 0, 0);
        }
    }

    /** Linie zwischen zwei Punkten. */
    static void linie(ServerLevel sl, Vec3 von, Vec3 bis, ParticleOptions p, double schritt) {
        Vec3 d = bis.subtract(von);
        int n = Math.max(1, (int) (d.length() / schritt));
        for (int i = 0; i <= n; i++) {
            Vec3 q = von.add(d.scale(i / (double) n));
            sl.sendParticles(p, q.x, q.y, q.z, 1, 0, 0, 0, 0);
        }
    }

    /** Kegel (Fächer) am Boden in Blickrichtung. */
    static void kegel(ServerLevel sl, Vec3 mitte, Vec3 richtung, double halbwinkelGrad, double radius, ParticleOptions p) {
        double basis = Math.atan2(richtung.z, richtung.x);
        double h = Math.toRadians(halbwinkelGrad);
        for (double r = 2.0; r <= radius; r += 1.6) {
            int n = Math.max(3, (int) (r * h * 2 / 1.2));
            for (int i = 0; i <= n; i++) {
                double w = basis - h + 2 * h * i / n;
                sl.sendParticles(p, mitte.x + Math.cos(w) * r, mitte.y + 0.15, mitte.z + Math.sin(w) * r, 1, 0, 0, 0, 0);
            }
        }
    }

    /** Liegt das Ziel im Kegel? */
    static boolean imKegel(Vec3 mitte, Vec3 richtung, double halbwinkelGrad, double radius, Entity e) {
        Vec3 zu = e.position().subtract(mitte);
        if (Math.abs(zu.y) > 5) {
            return false;
        }
        Vec3 flach = new Vec3(zu.x, 0, zu.z);
        double d = flach.length();
        if (d > radius) {
            return false;
        }
        if (d < 1.5) {
            return true;
        }
        Vec3 r = new Vec3(richtung.x, 0, richtung.z).normalize();
        return r.dot(flach.normalize()) >= Math.cos(Math.toRadians(halbwinkelGrad));
    }

    /** Waagrechter Abstand eines Punktes zu einem Strahl (Strecke von, Richtung, Länge); MAX_VALUE wenn außerhalb. */
    static double abstandZumStrahl(Vec3 von, Vec3 richtung, double laenge, Vec3 punkt) {
        Vec3 r = new Vec3(richtung.x, 0, richtung.z).normalize();
        Vec3 zu = new Vec3(punkt.x - von.x, 0, punkt.z - von.z);
        double t = zu.dot(r);
        if (t < 0 || t > laenge) {
            return Double.MAX_VALUE;
        }
        return zu.subtract(r.scale(t)).length();
    }

    static Vec3 richtung(double winkelGrad) {
        double w = Math.toRadians(winkelGrad);
        return new Vec3(Math.cos(w), 0, Math.sin(w));
    }

    static double winkelZu(Entity von, Entity zu) {
        return Math.toDegrees(Math.atan2(zu.getZ() - von.getZ(), zu.getX() - von.getX()));
    }

    /** Dreht einen Winkel höchstens um „schritt“ Grad auf das Ziel zu. */
    static double nachfuehren(double ist, double soll, double schritt) {
        double d = ((soll - ist) % 360 + 540) % 360 - 180;
        return ist + Math.max(-schritt, Math.min(schritt, d));
    }

    /** Alle lebenden Spieler im Überlebensmodus im Umkreis. */
    static List<Player> spieler(ServerLevel sl, Entity boss, double radius) {
        return sl.getEntitiesOfClass(Player.class, boss.getBoundingBox().inflate(radius, 16, radius),
                p -> p.isAlive() && !p.isSpectator() && !p.isCreative());
    }

    /** Kurzer Hinweis über der Hotbar für alle in der Nähe. */
    static void hinweis(ServerLevel sl, Entity boss, String text, ChatFormatting farbe) {
        for (ServerPlayer p : sl.players()) {
            if (p.distanceToSqr(boss) < 64 * 64) {
                p.displayClientMessage(Component.literal(text).withStyle(farbe, ChatFormatting.BOLD), true);
            }
        }
    }
}
