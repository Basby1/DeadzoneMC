package de.deadzonemc.client.kino;

import de.deadzonemc.charakter.CharakterPayload;
import de.deadzonemc.client.IntroScreen;
import de.deadzonemc.kino.KinoFigur;
import de.deadzonemc.kino.KinoTypen;
import de.deadzonemc.kino.Transporter;
import de.deadzonemc.kino.UrsprungSchatten;
import de.deadzonemc.musik.Musik;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Das Drehbuch des Kino-Intros „Flug Q-7“. Alles läuft nur auf dem Client: Über dem eigenen Startplatz fliegt ein
 * Transporthubschrauber durch die echte Welt, darin sitzen die Charaktere aller, die gemeinsam starten. Zeiten in
 * Sekunden passen zum Soundtrack (werkzeuge/musik.py, kino()).
 */
public final class ClientKino {

    private ClientKino() {
    }

    // ---- Drehbuch (Sekunden) ----
    static final float INNEN1 = 4.0F, AUSSEN = 13.0F, INNEN2 = 20.0F, TUER = 27.0F, BLITZ1 = 29.0F, BLITZ2 = 32.0F,
            SCHWEBEN = 35.0F, REISSEN = 39.0F, STILLE = 43.0F, STURZ = 45.5F, AUFPRALL = 48.0F, ENDE = 54.0F;
    private static final float TEMPO = 0.22F; // Blöcke pro Tick

    /** Sitzplätze in Modellpixeln (x, z) – linke Bank schaut zur Tür (+x), rechte hintere Bank zurück. */
    private static final float[][] SITZE = {{-19, -2}, {-19, 14}, {-19, -18}, {-19, 30}, {-19, -34}, {19, 40}, {-19, 46},
            {19, 56}, {-19, -50}};

    private static boolean aktiv;
    private static int tick;
    private static final List<KinoFigur> FIGUREN = new ArrayList<>();
    private static final List<String> NAMEN = new ArrayList<>();
    private static final List<Vec3> TREIBEN = new ArrayList<>();
    private static Transporter heli;
    private static UrsprungSchatten schatten;
    private static KinoFigur kamera;
    private static Vec3 start;
    private static Vec3 richtung;
    private static Vec3 rechts;
    private static float heliGier;
    private static double boden;
    private static SoundInstance ton;
    private static CameraType alteAnsicht;
    private static boolean altesHud;
    private static Vec3 sturzStart;
    private static int naechsteId = -7000;
    private static final RandomSource ZUFALL = RandomSource.create();

    public static boolean aktiv() {
        return aktiv;
    }

    static float zeit(float teil) {
        return (tick + teil) / 20F;
    }

    static List<String> namen() {
        return NAMEN;
    }

    // ---- Start und Ende -----------------------------------------------------------------------

    /** daten: "uuid,name;uuid,name" */
    public static void starten(String daten) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || aktiv) {
            return;
        }
        List<UUID> leute = new ArrayList<>();
        NAMEN.clear();
        for (String teil : daten.split(";")) {
            String[] t = teil.split(",", 2);
            if (t.length == 2) {
                try {
                    leute.add(UUID.fromString(t[0]));
                    NAMEN.add(t[1]);
                } catch (IllegalArgumentException ignoriert) {
                    // überspringen
                }
            }
        }
        if (leute.isEmpty()) {
            leute.add(mc.player.getUUID());
            NAMEN.add(mc.player.getGameProfile().getName());
        }
        try {
            aufbauen(mc, leute);
        } catch (Exception e) {
            com.mojang.logging.LogUtils.getLogger().error("Kino-Intro konnte nicht starten", e);
            abbauen(mc);
            IntroScreen.starten(String.join(", ", NAMEN));
            fertigMelden();
        }
    }

    private static void aufbauen(Minecraft mc, List<UUID> leute) {
        ClientLevel level = mc.level;
        tick = 0;
        float gier = mc.player.getYRot();
        richtung = new Vec3(-Mth.sin(gier * Mth.DEG_TO_RAD), 0, Mth.cos(gier * Mth.DEG_TO_RAD)).normalize();
        // Modell-+x (Seite mit der Schiebetür) liegt rechts vom Blick (der Renderer dreht den Hubschrauber selbst)
        rechts = new Vec3(-richtung.z, 0, richtung.x);
        heliGier = gier;
        boden = mc.player.getY();
        double hoehe = Math.min(boden + 58, level.getMaxBuildHeight() - 20);
        start = mc.player.position().add(richtung.scale(-95)).add(0, hoehe - boden, 0);

        heli = KinoTypen.TRANSPORTER.get().create(level);
        hinzufuegen(level, heli);
        for (int i = 0; i < leute.size() && i < SITZE.length; i++) {
            KinoFigur f = KinoTypen.FIGUR.get().create(level);
            f.besitzer = leute.get(i);
            f.haltung = KinoFigur.SITZEN;
            hinzufuegen(level, f);
            FIGUREN.add(f);
            double w = ZUFALL.nextDouble() * Math.PI * 2;
            TREIBEN.add(new Vec3(Math.cos(w) * 0.03, 0.012 + ZUFALL.nextDouble() * 0.01, Math.sin(w) * 0.03));
        }
        kamera = KinoTypen.FIGUR.get().create(level);
        kamera.kamera = true;
        kamera.setInvisible(true);
        hinzufuegen(level, kamera);
        schatten = KinoTypen.SCHATTEN.get().create(level);
        hinzufuegen(level, schatten);

        aktiv = true;
        mc.getMusicManager().stopPlaying();
        alteAnsicht = mc.options.getCameraType();
        mc.options.setCameraType(CameraType.FIRST_PERSON);
        altesHud = mc.options.hideGui;
        mc.options.hideGui = true;
        bewegen(0);
        mc.setCameraEntity(kamera);
        ton = new SimpleSoundInstance(Musik.KINO.get().getLocation(), SoundSource.MASTER, 1.0F, 1.0F,
                RandomSource.create(), false, 0, SoundInstance.Attenuation.NONE, 0, 0, 0, true);
        mc.getSoundManager().play(ton);
        mc.setScreen(new KinoScreen());
    }

    private static void hinzufuegen(ClientLevel level, Entity e) {
        e.setId(naechsteId--);
        e.moveTo(start.x, start.y, start.z, heliGier, 0);
        level.addEntity(e);
    }

    /** Überspringen (Leertaste) oder regulär am Ende. */
    static void beenden(boolean uebersprungen) {
        Minecraft mc = Minecraft.getInstance();
        if (!aktiv) {
            return;
        }
        abbauen(mc);
        if (uebersprungen) {
            tonStoppen();
        }
        meldenNachIntro = true; // der Server begrüßt erst, wenn auch das Erwachen vorbei ist
        IntroScreen.starten(String.join(", ", NAMEN));
    }

    private static void abbauen(Minecraft mc) {
        aktiv = false;
        if (mc.player != null) {
            mc.setCameraEntity(mc.player);
        }
        if (alteAnsicht != null) {
            mc.options.setCameraType(alteAnsicht);
        }
        mc.options.hideGui = altesHud;
        if (mc.level != null) {
            for (Entity e : alle()) {
                if (e != null) {
                    mc.level.removeEntity(e.getId(), Entity.RemovalReason.DISCARDED);
                }
            }
        }
        FIGUREN.clear();
        TREIBEN.clear();
        heli = null;
        schatten = null;
        kamera = null;
        sturzStart = null;
    }

    private static List<Entity> alle() {
        List<Entity> l = new ArrayList<>(FIGUREN);
        l.add(heli);
        l.add(schatten);
        l.add(kamera);
        return l;
    }

    public static void tonStoppen() {
        if (ton != null) {
            Minecraft.getInstance().getSoundManager().stop(ton);
            ton = null;
        }
    }

    private static boolean meldenNachIntro;

    private static void fertigMelden() {
        PacketDistributor.sendToServer(new CharakterPayload(CharakterPayload.KINO, ""));
    }

    /** Vom IntroScreen beim Schließen: jetzt darf der Server „Akt I“ einblenden. */
    public static void introVorbei() {
        if (meldenNachIntro) {
            meldenNachIntro = false;
            fertigMelden();
        }
    }

    // ---- Ablauf --------------------------------------------------------------------------------

    public static void tick(ClientTickEvent.Post e) {
        if (!aktiv) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || heli == null || heli.level() != mc.level) {
            abbauen(mc);
            return;
        }
        try {
            tick++;
            float s = tick / 20F;
            ereignisse(mc, s);
            bewegen(tick);
            if (s >= ENDE) {
                beenden(false);
            }
        } catch (Exception ex) {
            com.mojang.logging.LogUtils.getLogger().error("Kino-Intro abgebrochen", ex);
            beenden(true);
        }
    }

    private static boolean bei(float s, float zeitpunkt) {
        return s >= zeitpunkt && s - 0.05F < zeitpunkt;
    }

    private static void ereignisse(Minecraft mc, float s) {
        if (bei(s, 34.6F)) {
            heli.rotorAn = false;
            heli.triggerAnim("rotor", "auslaufen");
        }
        if (bei(s, REISSEN)) {
            heli.triggerAnim("rumpf", "zerreissen");
        }
        if (bei(s, BLITZ2 + 0.2F)) {
            schatten.triggerAnim("aktion", "puls");
        }
        // Schatten: nur in den Blitzen sichtbar, dann bleibt das Auge
        schatten.augeOffen = s >= BLITZ2 + 0.25F && s < SCHWEBEN + 1.0F;
        if (s >= BLITZ1 && s < BLITZ1 + 1.2F) {
            schatten.sichtbar = 1;
        } else if (s >= BLITZ2 && s < SCHWEBEN + 1.0F) {
            schatten.sichtbar = 1;
        } else {
            schatten.sichtbar = 0;
        }
        // Schwerelos: Teilchen treiben in der Kabine
        if (s >= SCHWEBEN && s < STILLE && kamera != null) {
            for (int i = 0; i < 3; i++) {
                Vec3 p = kamera.getEyePosition().add(ZUFALL.nextGaussian() * 3, ZUFALL.nextGaussian() * 2, ZUFALL.nextGaussian() * 3);
                mc.level.addParticle(i == 0 ? ParticleTypes.END_ROD : ParticleTypes.REVERSE_PORTAL, p.x, p.y, p.z,
                        ZUFALL.nextGaussian() * 0.01, 0.01, ZUFALL.nextGaussian() * 0.01);
            }
        }
    }

    // ---- Positionen ----------------------------------------------------------------------------

    /** Wo der Hubschrauber zum Zeitpunkt s ist (bremst in der Schwerelosigkeit ab). */
    private static Vec3 heliPos(float s) {
        float strecke = Math.min(s, SCHWEBEN) * 20 * TEMPO;
        if (s > SCHWEBEN) {
            float d = s - SCHWEBEN;
            strecke += 20 * TEMPO * (d - d * d / 16F) * (d < 8 ? 1 : 0) + (d >= 8 ? 20 * TEMPO * 4 : 0);
        }
        float schwanken = Mth.sin(s * 0.9F) * 0.15F;
        return start.add(richtung.scale(strecke)).add(0, schwanken, 0);
    }

    /** Modellpixel (rechts, hoch, vorwärts negativ wie im Modell) → Welt, relativ zum Hubschrauber bei s. */
    private static Vec3 lokal(float s, double mx, double my, double mz) {
        return heliPos(s).add(rechts.scale(mx / 16.0)).add(0, my / 16.0, 0).add(richtung.scale(-mz / 16.0));
    }

    private static void bewegen(int t) {
        float s = t / 20F;
        setzen(heli, heliPos(s), heliGier, 0);
        // Figuren
        for (int i = 0; i < FIGUREN.size(); i++) {
            KinoFigur f = FIGUREN.get(i);
            float[] sitz = SITZE[i];
            boolean links = sitz[0] < 0;
            Vec3 blick = links ? rechts : rechts.scale(-1);
            float gier = (float) (Mth.atan2(-blick.x, blick.z) * Mth.RAD_TO_DEG);
            Vec3 platz = lokal(s, sitz[0], 17, sitz[1]).add(0, -0.68, 0);
            float kopf = gier;
            if (s >= INNEN2 + 2.5F && s < SCHWEBEN) {
                kopf = gier + Mth.sin(s * 1.3F + i) * 25 + 20; // unruhig, Blick zur Tür
            } else if (s < INNEN2) {
                kopf = gier + Mth.sin(s * 0.4F + i * 2) * 18;
            }
            float nicken = s >= TUER && s < SCHWEBEN ? -10 : 5;
            if (s < SCHWEBEN) {
                f.haltung = KinoFigur.SITZEN;
                f.arme = 0;
                f.roll = 0;
                f.kippen = 0;
                setzen(f, platz, gier, nicken);
                f.yHeadRot = kopf;
            } else {
                f.haltung = KinoFigur.SCHWEBEN;
                float d = s - SCHWEBEN;
                Vec3 tr = TREIBEN.get(i);
                Vec3 weg = new Vec3(0, Math.min(d, 4) * 20 * tr.y, 0);
                if (s >= REISSEN) {
                    // durch den aufgerissenen Rumpf hinaus, jeder in eine andere Richtung
                    float r = s - REISSEN;
                    weg = weg.add(rechts.scale(r * r * 0.35 + r * 0.6)).add(richtung.scale(r * (i % 2 == 0 ? 0.5 : -0.4)))
                            .add(tr.scale(r * 30));
                }
                f.arme = Math.min(1, d / 2.5F);
                f.roll = d * (i % 2 == 0 ? 9 : -7);
                f.kippen = d * (i % 3 == 0 ? 6 : -4);
                setzen(f, platz.add(weg), gier + d * (i % 2 == 0 ? 6 : -8), -20);
                f.yHeadRot = gier + d * (i % 2 == 0 ? 6 : -8) + Mth.sin(d) * 20;
            }
        }
        // Schatten: erst weit draußen, beim zweiten Blitz näher
        double abstand = (t / 20F) >= BLITZ2 ? 72 : 96;
        Vec3 sp = heliPos(31.0F).add(rechts.scale(abstand)).add(richtung.scale((t / 20F) >= BLITZ2 ? 8 : 26));
        int bx = Mth.floor(sp.x), bz = Mth.floor(sp.z);
        double gy = Minecraft.getInstance().level.getHeight(Heightmap.Types.WORLD_SURFACE, bx, bz);
        if (gy <= Minecraft.getInstance().level.getMinBuildHeight() + 1) {
            gy = boden;
        }
        float schattenGier = (float) (Mth.atan2(rechts.x, -rechts.z) * Mth.RAD_TO_DEG); // schaut zum Hubschrauber
        setzen(schatten, new Vec3(sp.x, gy - 2, sp.z), schattenGier, 0);
        // Kamera
        Vec3 auge = kameraPos(s);
        setzen(kamera, auge.subtract(0, kamera.getEyeHeight(), 0), 0, 0);
    }

    private static void setzen(Entity e, Vec3 p, float gier, float nicken) {
        if (e == null) {
            return;
        }
        e.setOldPosAndRot();
        e.setPos(p.x, p.y, p.z);
        e.setYRot(gier);
        e.setXRot(nicken);
        if (e instanceof KinoFigur f) {
            f.yBodyRotO = f.yBodyRot;
            f.yHeadRotO = f.yHeadRot;
            f.yBodyRot = gier;
            f.yHeadRot = gier;
        }
    }

    // ---- Kamera --------------------------------------------------------------------------------

    /** Augenposition der Kamera zum Zeitpunkt s. */
    static Vec3 kameraPos(float s) {
        if (s < INNEN1) {
            return lokal(s, 12, 30, -30);
        }
        if (s < AUSSEN) {
            float a = (s - INNEN1) / (AUSSEN - INNEN1);
            return lokal(s, 12, 30, Mth.lerp(a, -30, 22));
        }
        if (s < INNEN2) {
            float a = (s - AUSSEN) / (INNEN2 - AUSSEN);
            double w = Math.toRadians(15 + 50 * a);
            return lokal(s, 200 * Math.cos(w), 46 - 26 * a, 200 * Math.sin(w) - 10);
        }
        if (s < TUER) {
            float a = (s - INNEN2) / (TUER - INNEN2);
            return lokal(s, 2, 34, Mth.lerp(a, 62, 48));
        }
        if (s < SCHWEBEN) {
            float a = (s - TUER) / (SCHWEBEN - TUER);
            return lokal(s, Mth.lerp(a, -10, 4), 34, Mth.lerp(a, 4, -2));
        }
        if (s < STURZ) {
            float a = Math.min(1, (s - SCHWEBEN) / (STILLE - SCHWEBEN));
            return lokal(s, -4, 34 + 10 * a, 50 - 10 * a);
        }
        if (sturzStart == null) {
            sturzStart = lokal(STURZ, -4, 44, 40);
        }
        float a = Math.min(1, (s - STURZ) / (AUFPRALL - STURZ));
        return sturzStart.add(rechts.scale(a * 6)).add(0, -Math.min(50, boden < sturzStart.y ? (sturzStart.y - boden - 2) : 40) * a * a, 0);
    }

    /** Wohin die Kamera schaut (Punkt in der Welt). */
    static Vec3 kameraZiel(float s) {
        if (s < AUSSEN) {
            Vec3 p = kameraPos(s);
            return p.add(rechts.scale(-1.6)).add(richtung.scale(-0.35)).add(0, -0.25, 0);
        }
        if (s < INNEN2) {
            return lokal(s, 0, 30, -10);
        }
        if (s < TUER) {
            return lokal(s, -10, 26, -20);
        }
        if (s < SCHWEBEN) {
            Vec3 aussen = schatten != null ? schatten.position().add(0, 50, 0) : lokal(s, 400, 0, 0);
            float a = Mth.clamp((s - TUER) / 3F, 0, 1);
            Vec3 tuer = lokal(s, 200, 10, -40);
            return tuer.lerp(aussen, a * 0.85);
        }
        if (s < STURZ) {
            return lokal(s, 60, 50, -10);
        }
        return kameraPos(s).add(rechts.scale(1)).add(0, -2.2, 0);
    }

    public static void winkel(ViewportEvent.ComputeCameraAngles e) {
        if (!aktiv || Minecraft.getInstance().getCameraEntity() != kamera) {
            return;
        }
        float s = zeit((float) e.getPartialTick());
        Vec3 von = kameraPos(s);
        Vec3 zu = kameraZiel(s).subtract(von);
        double waag = Math.sqrt(zu.x * zu.x + zu.z * zu.z);
        float gier = (float) (Mth.atan2(-zu.x, zu.z) * Mth.RAD_TO_DEG);
        float nicken = (float) (-Mth.atan2(zu.y, waag) * Mth.RAD_TO_DEG);
        float wackeln = s < SCHWEBEN ? 0.4F : 0.1F;
        if (s >= INNEN2 + 4 && s < SCHWEBEN) {
            wackeln = 1.3F;
        }
        float roll = Mth.sin(s * 1.7F) * wackeln;
        if (s >= SCHWEBEN && s < STURZ) {
            roll += Math.min(24, (s - SCHWEBEN) * 3.5F);
        }
        if (s >= STURZ) {
            float a = (s - STURZ) / (AUFPRALL - STURZ);
            roll += 24 + a * a * 420;
            nicken += a * 35;
        }
        gier += Mth.sin(s * 2.3F) * wackeln * 0.6F;
        nicken += Mth.sin(s * 3.1F + 1) * wackeln * 0.5F;
        e.setYaw(gier);
        e.setPitch(nicken);
        e.setRoll(roll);
    }

    // ---- Nebel und Licht ----------------------------------------------------------------------

    static float blitz(float s) {
        float b = 0;
        for (float z : new float[]{BLITZ1, BLITZ2}) {
            if (s >= z && s < z + 0.6F) {
                b = Math.max(b, 1 - (s - z) / 0.6F);
            }
        }
        if (s >= BLITZ1 - 0.35F && s < BLITZ1 - 0.25F) {
            b = 0.5F; // Vorblitz
        }
        return b;
    }

    public static void nebel(ViewportEvent.RenderFog e) {
        if (!aktiv) {
            return;
        }
        float s = zeit((float) e.getPartialTick());
        float weit = 46;
        if (s >= AUSSEN && s < INNEN2) {
            weit = 120;
        }
        float b = blitz(s);
        if (s >= BLITZ2 && s < SCHWEBEN + 1) {
            weit = 140;
        } else if (b > 0) {
            weit = 46 + 260 * b;
        }
        if (s >= STILLE) {
            weit = 24;
        }
        e.setNearPlaneDistance(weit * 0.15F);
        e.setFarPlaneDistance(weit);
        e.setCanceled(true);
    }

    public static void nebelFarbe(ViewportEvent.ComputeFogColor e) {
        if (!aktiv) {
            return;
        }
        float s = zeit((float) e.getPartialTick());
        float r = 0.16F, g = 0.18F, b = 0.21F;
        if (s >= BLITZ2 + 0.25F && s < STILLE) {
            r = 0.22F;
            g = 0.10F;
            b = 0.32F;
        }
        float f = blitz(s);
        e.setRed(Mth.lerp(f, r, 0.86F));
        e.setGreen(Mth.lerp(f, g, 0.84F));
        e.setBlue(Mth.lerp(f, b, 0.98F));
    }

    /** Blocklicht für die Darsteller (innen dunkel, beim Blitz grell). */
    static int lichtBlock() {
        if (!aktiv) {
            return 15;
        }
        float s = zeit(0);
        if (blitz(s) > 0.3F) {
            return 15;
        }
        boolean innen = s < AUSSEN || (s >= INNEN2 && s < STURZ);
        if (s >= BLITZ2 + 0.25F && s < STILLE) {
            return 9;
        }
        return innen ? 7 : 0;
    }

    static int lichtHimmel() {
        if (!aktiv) {
            return 15;
        }
        float s = zeit(0);
        if (blitz(s) > 0.3F) {
            return 15;
        }
        boolean innen = s < AUSSEN || (s >= INNEN2 && s < STURZ);
        return innen ? 5 : 12;
    }
}
