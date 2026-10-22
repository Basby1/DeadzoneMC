package de.deadzonemc.client.kino;

import de.deadzonemc.client.IntroScreen;
import de.deadzonemc.kino.KinoBoot;
import de.deadzonemc.kino.KinoFigur;
import de.deadzonemc.kino.KinoRuinen;
import de.deadzonemc.kino.KinoTypen;
import de.deadzonemc.kino.UrsprungSchatten;
import de.deadzonemc.musik.Musik;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ViewportEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Das Drehbuch des Boot-Intros „Kilo-2“ (Rückmeldung 22.10.): Nachts nähert sich ein Patrouillenboot mit den
 * Charakteren aller Spieler der Küste von Sektor 4 – Auftrag: herausfinden, was dort passiert ist. Nebel, die zerstörte
 * Stadt am Ufer mit ein paar Lichtern, plötzlicher Regen, zwei Blitze zeigen den Ursprung (erst fern, dann nah mit
 * violetter Aura), eine Druckwelle, das Boot kentert, Unterwasser, Schwarz – und man erwacht gestrandet am Strand.
 *
 * <p>Alles nur auf dem Client. Die Szene spielt an der echten Küste vor dem eigenen Startplatz: Das Boot fährt aus
 * Richtung Meer auf den Strand zu, an dem man später aufwacht. Zeiten in Sekunden passen zum Soundtrack
 * (werkzeuge/musik.py, kino_boot()).</p>
 */
public final class ClientKinoBoot {

    private ClientKinoBoot() {
    }

    // ---- Drehbuch (Sekunden) ----
    static final float DECK = 4.0F, AUSSEN = 15.0F, BUG = 23.0F, REGEN = 23.0F, BLITZ1 = 29.0F, GESICHTER = 30.3F,
            BLITZ2 = 32.0F, WELLE = 33.0F, EINSCHLAG = 35.0F, WASSERLINIE = 36.4F, UEBERBORD = 38.0F, UNTER = 40.0F,
            SCHWARZ = 44.0F, ENDE = 52.0F;
    private static final float TEMPO = 2.2F; // Blöcke pro Sekunde
    private static final float START_ABSTAND = 120F;

    /** Sitzplätze in Modellpixeln (x, z): linke Bank schaut nach rechts, rechte nach links. */
    private static final float[][] SITZE = {{-27, 30}, {27, 46}, {-27, 62}, {27, 30}, {-27, 46}, {27, 62}, {-27, 78}, {27, 78}};

    private static boolean aktiv;
    private static boolean vorbereiten;
    private static int vorbereitet;
    private static int tick;
    private static final List<KinoFigur> FIGUREN = new ArrayList<>();
    private static final List<UUID> WARTEN_AUF = new ArrayList<>();
    private static final List<KinoRuinen> KULISSE = new ArrayList<>();
    private static KinoBoot boot;
    private static UrsprungSchatten schatten;
    private static KinoFigur kamera;
    private static Vec3 ufer;      // Startplatz am Strand
    private static Vec3 land;      // Richtung Meer → Land (Fahrtrichtung)
    private static Vec3 rechts;
    private static float bootGier;
    private static double wasser;
    private static Vec3 ursprungFern;
    private static Vec3 ursprungNah;
    private static SoundInstance ton;
    private static CameraType alteAnsicht;
    private static boolean altesHud;
    private static float alterRegen, alterDonner;
    private static int naechsteId = -7600;
    private static final RandomSource ZUFALL = RandomSource.create();

    public static boolean aktiv() {
        return aktiv || vorbereiten;
    }

    static boolean bereitet() {
        return vorbereiten;
    }

    static float zeit(float teil) {
        return (tick + teil) / 20F;
    }

    // ---- Start und Ende -----------------------------------------------------------------------

    /** daten: "landX,landZ|uuid,name;uuid,name" */
    public static void starten(String daten) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || aktiv()) {
            return;
        }
        String[] teile = daten.split("\\|", 2);
        double lx = 1, lz = 0;
        try {
            String[] r = teile[0].split(",");
            lx = Double.parseDouble(r[0]);
            lz = Double.parseDouble(r[1]);
        } catch (Exception ignoriert) {
            float g = mc.player.getYRot() * Mth.DEG_TO_RAD;
            lx = -Mth.sin(g);
            lz = Mth.cos(g);
        }
        land = new Vec3(lx, 0, lz).normalize();
        List<UUID> leute = new ArrayList<>();
        List<String> namen = ClientKino.namen();
        namen.clear();
        for (String teil : (teile.length > 1 ? teile[1] : "").split(";")) {
            String[] t = teil.split(",", 2);
            if (t.length == 2) {
                try {
                    leute.add(UUID.fromString(t[0]));
                    namen.add(t[1]);
                } catch (IllegalArgumentException ignoriert) {
                    // überspringen
                }
            }
        }
        if (leute.isEmpty()) {
            leute.add(mc.player.getUUID());
            namen.add(mc.player.getGameProfile().getName());
        }
        WARTEN_AUF.clear();
        WARTEN_AUF.addAll(leute);
        vorbereiten = true;
        vorbereitet = 0;
        mc.getMusicManager().stopPlaying();
        mc.setScreen(new KinoBootScreen());
    }

    /** Sind die Chunks entlang der Fahrt und am Ufer geladen und gezeichnet? */
    private static boolean weltBereit(Minecraft mc) {
        ClientLevel level = mc.level;
        Vec3 p = mc.player.position();
        Vec3 r = new Vec3(-land.z, 0, land.x);
        for (int d = -100; d <= 100; d += 20) {
            for (int q = -48; q <= 48; q += 24) {
                Vec3 x = p.add(land.scale(d)).add(r.scale(q));
                int bx = Mth.floor(x.x), bz = Mth.floor(x.z);
                if (!level.hasChunk(bx >> 4, bz >> 4)) {
                    return false;
                }
                int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, bx, bz);
                if (y <= level.getMinBuildHeight() + 1
                        || !mc.levelRenderer.isSectionCompiled(new net.minecraft.core.BlockPos(bx, y - 1, bz))) {
                    return false;
                }
            }
        }
        return true;
    }

    private static void vorbereitenTick(Minecraft mc) {
        vorbereitet++;
        if (vorbereitet < 40 || (!weltBereit(mc) && vorbereitet < 20 * 30)) {
            return;
        }
        vorbereiten = false;
        try {
            aufbauen(mc, new ArrayList<>(WARTEN_AUF));
        } catch (Exception e) {
            com.mojang.logging.LogUtils.getLogger().error("Boot-Intro konnte nicht starten", e);
            abbauen(mc);
            IntroScreen.starten(String.join(", ", ClientKino.namen()));
            ClientKino.fertigMeldenJetzt();
        }
    }

    private static double boden(ClientLevel level, Vec3 p, double sonst) {
        int bx = Mth.floor(p.x), bz = Mth.floor(p.z);
        if (!level.hasChunk(bx >> 4, bz >> 4)) {
            return sonst;
        }
        double y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, bx, bz);
        return y <= level.getMinBuildHeight() + 1 ? sonst : y;
    }

    private static float gierVon(Vec3 v) {
        return (float) (Mth.atan2(-v.x, v.z) * Mth.RAD_TO_DEG);
    }

    private static void aufbauen(Minecraft mc, List<UUID> leute) {
        ClientLevel level = mc.level;
        tick = 0;
        ufer = mc.player.position();
        rechts = new Vec3(-land.z, 0, land.x);
        bootGier = gierVon(land);
        wasser = level.getSeaLevel() - 0.12;

        boot = KinoTypen.BOOT.get().create(level);
        hinzufuegen(level, boot, bootPos(0), bootGier);
        for (int i = 0; i < leute.size() && i < SITZE.length; i++) {
            KinoFigur f = KinoTypen.FIGUR.get().create(level);
            f.besitzer = leute.get(i);
            f.haltung = KinoFigur.SITZEN;
            hinzufuegen(level, f, bootPos(0), bootGier);
            FIGUREN.add(f);
        }
        kamera = KinoTypen.FIGUR.get().create(level);
        kamera.kamera = true;
        kamera.setInvisible(true);
        hinzufuegen(level, kamera, bootPos(0), 0);
        // Die zerstörte Stadt am Ufer: drei Kulissen hintereinander gestaffelt
        float meerGier = gierVon(land.scale(-1));
        double[][] lagen = {{50, -62}, {62, 2}, {55, 66}};
        for (double[] l : lagen) {
            KinoRuinen k = KinoTypen.RUINEN.get().create(level);
            Vec3 p = ufer.add(land.scale(l[0])).add(rechts.scale(l[1]));
            double y = Math.max(wasser, boden(level, p, wasser + 3) - 2);
            hinzufuegen(level, k, new Vec3(p.x, y, p.z), meerGier);
            KULISSE.add(k);
        }
        // Der Ursprung: erst fern zwischen den Ruinen, beim zweiten Blitz am Strand
        Vec3 fern = ufer.add(land.scale(84)).add(rechts.scale(22));
        ursprungFern = new Vec3(fern.x, boden(level, fern, wasser + 4) - 3, fern.z);
        Vec3 nah = ufer.add(land.scale(8)).add(rechts.scale(-14));
        ursprungNah = new Vec3(nah.x, Math.max(wasser - 2, boden(level, nah, wasser + 1) - 2), nah.z);
        schatten = KinoTypen.SCHATTEN.get().create(level);
        hinzufuegen(level, schatten, ursprungFern, meerGier);

        aktiv = true;
        alterRegen = level.getRainLevel(1F);
        alterDonner = level.getThunderLevel(1F);
        mc.getMusicManager().stopPlaying();
        alteAnsicht = mc.options.getCameraType();
        mc.options.setCameraType(CameraType.FIRST_PERSON);
        altesHud = mc.options.hideGui;
        mc.options.hideGui = true;
        bewegen(mc, 0);
        mc.setCameraEntity(kamera);
        ton = new SimpleSoundInstance(Musik.KINO_BOOT.get().getLocation(), SoundSource.MASTER, 1.0F, 1.0F,
                RandomSource.create(), false, 0, SoundInstance.Attenuation.NONE, 0, 0, 0, true);
        mc.getSoundManager().play(ton);
        mc.setScreen(new KinoBootScreen());
    }

    private static void hinzufuegen(ClientLevel level, Entity e, Vec3 p, float gier) {
        e.setId(naechsteId--);
        e.moveTo(p.x, p.y, p.z, gier, 0);
        level.addEntity(e);
    }

    static void beenden(boolean uebersprungen) {
        Minecraft mc = Minecraft.getInstance();
        if (vorbereiten) {
            vorbereiten = false;
            ClientKino.nachIntroMelden();
            IntroScreen.starten(String.join(", ", ClientKino.namen()));
            return;
        }
        if (!aktiv) {
            return;
        }
        abbauen(mc);
        if (uebersprungen) {
            tonStoppen();
        }
        ClientKino.nachIntroMelden();
        IntroScreen.starten(String.join(", ", ClientKino.namen()));
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
            List<Entity> alle = new ArrayList<>(FIGUREN);
            alle.addAll(KULISSE);
            alle.add(boot);
            alle.add(schatten);
            alle.add(kamera);
            for (Entity e : alle) {
                if (e != null) {
                    mc.level.removeEntity(e.getId(), Entity.RemovalReason.DISCARDED);
                }
            }
            mc.level.setRainLevel(alterRegen);
            mc.level.setThunderLevel(alterDonner);
        }
        FIGUREN.clear();
        KULISSE.clear();
        boot = null;
        schatten = null;
        kamera = null;
    }

    public static void tonStoppen() {
        if (ton != null) {
            Minecraft.getInstance().getSoundManager().stop(ton);
            ton = null;
        }
    }

    // ---- Ablauf --------------------------------------------------------------------------------

    public static void tick(Minecraft mc) {
        if (vorbereiten) {
            if (mc.level == null || mc.player == null) {
                vorbereiten = false;
                return;
            }
            vorbereitenTick(mc);
            return;
        }
        if (!aktiv) {
            return;
        }
        if (mc.level == null || mc.player == null || boot == null || boot.level() != mc.level) {
            abbauen(mc);
            return;
        }
        try {
            tick++;
            float s = tick / 20F;
            // Es ist Nacht (nur hier auf dem Client; der Server schickt danach die echte Zeit)
            ((ClientLevel.ClientLevelData) mc.level.getLevelData()).setDayTime(18000);
            ereignisse(mc, s);
            bewegen(mc, tick);
            if (s >= ENDE) {
                beenden(false);
            }
        } catch (Exception ex) {
            com.mojang.logging.LogUtils.getLogger().error("Boot-Intro abgebrochen", ex);
            beenden(true);
        }
    }

    private static boolean bei(float s, float zeitpunkt) {
        return s >= zeitpunkt && s - 0.05F < zeitpunkt;
    }

    private static void ereignisse(Minecraft mc, float s) {
        ClientLevel level = mc.level;
        // Regen setzt plötzlich ein und wird zum Sturm
        float regen = s < REGEN ? 0 : Math.min(1, (s - REGEN) / 1.2F);
        level.setRainLevel(regen);
        level.setThunderLevel(s < BLITZ1 ? 0 : 1);
        if (bei(s, BLITZ1)) {
            blitz(level, ursprungFern.add(rechts.scale(-18)).add(land.scale(10)));
        }
        if (bei(s, BLITZ2)) {
            blitz(level, ursprungNah.add(rechts.scale(16)).add(land.scale(12)));
            schatten.triggerAnim("aktion", "puls");
        }
        if (bei(s, WELLE)) {
            schatten.triggerAnim("aktion", "puls");
        }
        // Der Ursprung: im ersten Blitz fern, ab dem zweiten nah mit offenem Auge und violetter Aura
        if (s >= BLITZ1 && s < BLITZ1 + 1.3F) {
            schatten.sichtbar = 1;
            schatten.augeOffen = false;
        } else if (s >= BLITZ2 && s < UEBERBORD) {
            schatten.sichtbar = 1;
            schatten.augeOffen = true;
        } else {
            schatten.sichtbar = 0;
            schatten.augeOffen = false;
        }
        if (s >= BLITZ2 && s < UEBERBORD) {
            aura(level, schatten.position(), s < EINSCHLAG ? 26 : 10);
        }
        // Die Druckwelle: ein violetter Ring rollt über das Wasser auf das Boot zu
        if (s >= WELLE && s < EINSCHLAG + 0.4F) {
            double r = (s - WELLE) * 30;
            Vec3 m = ursprungNah;
            Vec3 meer = land.scale(-1);
            double basis = Math.atan2(meer.z, meer.x);
            for (int i = 0; i < 70; i++) {
                double w = basis + (ZUFALL.nextDouble() - 0.5) * 2.8;
                double x = m.x + Math.cos(w) * r;
                double z = m.z + Math.sin(w) * r;
                level.addParticle(i % 3 == 0 ? ParticleTypes.CLOUD : ParticleTypes.REVERSE_PORTAL, x, wasser + 0.3 + ZUFALL.nextDouble() * 1.2, z,
                        Math.cos(w) * 0.2, 0.05, Math.sin(w) * 0.2);
            }
        }
        if (bei(s, EINSCHLAG)) {
            boot.motorAn = false;
        }
        if (bei(s, EINSCHLAG + 0.2F)) {
            boot.triggerAnim("licht", "aus");
        }
        // Gischt, solange das Boot kämpft und kentert
        if (s >= EINSCHLAG && s < UNTER) {
            Vec3 b = boot.position();
            for (int i = 0; i < 18; i++) {
                ParticleOptions art = i % 3 == 0 ? ParticleTypes.CLOUD : (i % 3 == 1 ? ParticleTypes.SPLASH : ParticleTypes.FALLING_WATER);
                level.addParticle(art, b.x + ZUFALL.nextGaussian() * 4, wasser + ZUFALL.nextDouble() * 2.5, b.z + ZUFALL.nextGaussian() * 4,
                        ZUFALL.nextGaussian() * 0.15, 0.2 + ZUFALL.nextDouble() * 0.3, ZUFALL.nextGaussian() * 0.15);
            }
        }
        // Unter Wasser: Luftblasen steigen an der Kamera vorbei
        if (s >= UNTER && s < SCHWARZ && kamera != null) {
            Vec3 k = kamera.getEyePosition();
            for (int i = 0; i < 6; i++) {
                level.addParticle(ParticleTypes.BUBBLE, k.x + ZUFALL.nextGaussian() * 1.5, k.y - 1 + ZUFALL.nextDouble(),
                        k.z + ZUFALL.nextGaussian() * 1.5, 0, 0.12, 0);
            }
        }
    }

    private static void blitz(ClientLevel level, Vec3 p) {
        LightningBolt b = EntityType.LIGHTNING_BOLT.create(level);
        if (b == null) {
            return;
        }
        b.setVisualOnly(true);
        b.setId(naechsteId--);
        b.moveTo(p.x, p.y, p.z);
        level.addEntity(b);
    }

    private static void aura(ClientLevel level, Vec3 p, int menge) {
        for (int i = 0; i < menge; i++) {
            double w = ZUFALL.nextDouble() * Math.PI * 2;
            double r = 4 + ZUFALL.nextDouble() * 6;
            double h = ZUFALL.nextDouble() * 34;
            ParticleOptions art = i % 4 == 0 ? ParticleTypes.WITCH : (i % 4 == 1 ? ParticleTypes.PORTAL : ParticleTypes.REVERSE_PORTAL);
            level.addParticle(art, p.x + Math.cos(w) * r, p.y + h, p.z + Math.sin(w) * r, 0, 0.02, 0);
        }
    }

    // ---- Positionen ----------------------------------------------------------------------------

    /** Abstand des Boots vom Ufer zum Zeitpunkt s. */
    private static double abstand(float s) {
        double d = START_ABSTAND - TEMPO * Math.min(s, EINSCHLAG);
        if (s > EINSCHLAG) {
            d -= TEMPO * Math.min(1.5, (s - EINSCHLAG) * 0.6);
        }
        return d;
    }

    static Vec3 bootPos(float s) {
        double hub = Math.sin(s * 1.1) * 0.12 + (s > REGEN ? Math.sin(s * 1.7) * 0.15 : 0);
        double sinken = 0;
        if (s > UEBERBORD) {
            sinken = Math.min(1.0, (s - UEBERBORD) * 0.4);
        }
        return ufer.add(land.scale(-abstand(s))).add(rechts.scale(-6)).with(net.minecraft.core.Direction.Axis.Y, wasser + hub - sinken);
    }

    static float nick(float s) {
        float n = Mth.sin(s * 0.8F) * 2.0F + (s > REGEN ? Mth.sin(s * 1.3F) * 3.0F : 0);
        if (s >= EINSCHLAG && s < WASSERLINIE) {
            n += 12 * Mth.sin((s - EINSCHLAG) / (WASSERLINIE - EINSCHLAG) * Mth.PI);
        }
        return n;
    }

    static float roll(float s) {
        float r = Mth.sin(s * 0.6F + 1) * 2.5F + (s > REGEN ? Mth.sin(s * 1.1F) * 4.0F : 0);
        if (s >= EINSCHLAG && s < WASSERLINIE) {
            float a = (s - EINSCHLAG) / (WASSERLINIE - EINSCHLAG);
            r += a < 0.4F ? -25 * (a / 0.4F) : -25 + 45 * ((a - 0.4F) / 0.6F);
        } else if (s >= WASSERLINIE && s < UEBERBORD) {
            r = 20 + 55 * ((s - WASSERLINIE) / (UEBERBORD - WASSERLINIE));
        } else if (s >= UEBERBORD && s < UNTER) {
            r = 75 + 100 * ((s - UEBERBORD) / (UNTER - UEBERBORD));
        } else if (s >= UNTER) {
            r = 175 + Mth.sin(s) * 2;
        }
        return r;
    }

    /** Modellpixel (x rechts, y hoch, z hinten) → Welt, mit Neigung und Rollen des Boots. */
    static Vec3 lokal(float s, double mx, double my, double mz) {
        double x = mx / 16.0, y = my / 16.0, z = mz / 16.0;
        double r = Math.toRadians(roll(s));
        double x1 = x * Math.cos(r) - y * Math.sin(r);
        double y1 = x * Math.sin(r) + y * Math.cos(r);
        double n = Math.toRadians(nick(s));
        double y2 = y1 * Math.cos(n) - z * Math.sin(n);
        double z2 = y1 * Math.sin(n) + z * Math.cos(n);
        return bootPos(s).add(rechts.scale(x1)).add(0, y2, 0).add(land.scale(-z2));
    }

    private static void bewegen(Minecraft mc, int t) {
        float s = t / 20F;
        setzen(boot, bootPos(s), bootGier, 0);
        boot.nick = nick(s);
        boot.roll = roll(s);
        for (int i = 0; i < FIGUREN.size(); i++) {
            KinoFigur f = FIGUREN.get(i);
            float[] sitz = SITZE[i];
            boolean links = sitz[0] < 0;
            Vec3 blick = links ? rechts : rechts.scale(-1);
            float gier = gierVon(blick);
            Vec3 platz = lokal(s, sitz[0], 25, sitz[1]).add(0, -0.68, 0);
            float kopf = gier + Mth.sin(s * 0.35F + i * 2) * 16;
            if (s >= GESICHTER - 0.6F && s < EINSCHLAG) {
                kopf = gierVon(land) + Mth.sin(s * 2 + i) * 8; // alle starren zur Küste
            }
            if (s < EINSCHLAG) {
                f.haltung = KinoFigur.SITZEN;
                f.arme = 0;
                f.roll = 0;
                f.kippen = 0;
                setzen(f, platz, gier, s >= GESICHTER ? -6 : 6);
                f.yHeadRot = kopf;
            } else {
                // Durchgeschüttelt, dann über Bord
                float d = s - EINSCHLAG;
                f.haltung = KinoFigur.SCHWEBEN;
                f.arme = Math.min(1, d * 1.5F);
                Vec3 weg = Vec3.ZERO;
                double seite = links ? -1 : 1;
                if (d < 1.4F) {
                    weg = rechts.scale(seite * d * 0.6).add(0, Math.sin(d * 6) * 0.25, 0);
                } else {
                    float e = d - 1.4F;
                    weg = rechts.scale(seite * (0.84 + e * 2.2 + i * 0.2)).add(land.scale((i % 2 == 0 ? 1 : -1) * e * 0.5))
                            .add(0, 0.6 * e - 1.4 * e * e, 0);
                    if (platz.y + weg.y < wasser - 0.6) {
                        weg = new Vec3(weg.x, wasser - 0.6 - platz.y, weg.z);
                    }
                }
                f.roll = d * (i % 2 == 0 ? 40 : -35);
                f.kippen = d * (i % 3 == 0 ? 20 : -15);
                setzen(f, platz.add(weg), gier + d * (i % 2 == 0 ? 30 : -30), -10);
                f.yHeadRot = gier + Mth.sin(d * 3 + i) * 40;
                f.setInvisible(s >= UNTER);
            }
        }
        // Der Ursprung wechselt beim zweiten Blitz an den Strand
        Vec3 sp = s >= BLITZ2 ? ursprungNah : ursprungFern;
        setzen(schatten, sp, gierVon(land.scale(-1)), 0);
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

    static Vec3 kameraPos(float s) {
        if (s < AUSSEN) {
            // An Deck: langsam zwischen den Bänken nach vorn
            float a = Mth.clamp((s - DECK) / (AUSSEN - DECK), 0, 1);
            return lokal(s, -22, 44 - 4 * a, Mth.lerp(a, 112, 40));
        }
        if (s < BUG) {
            // Von außen: neben und hinter dem Boot über dem Wasser, die Küste voraus
            float a = (s - AUSSEN) / (BUG - AUSSEN);
            Vec3 b = bootPos(s);
            return b.add(rechts.scale(17 - 6 * a)).add(land.scale(-15 + 4 * a)).add(0, 4.5 - 1.5 * a, 0);
        }
        if (s < GESICHTER) {
            // Über das Steuerhaus hinweg zur Küste (Regen, erster Blitz)
            return lokal(s, 12, 74, 34);
        }
        if (s < BLITZ2) {
            // Die Gesichter: alle starren zur Küste (zur Kamera hin)
            return lokal(s, 0, 44, 22);
        }
        if (s < EINSCHLAG) {
            // Am Bug: der Ursprung am Strand, die Welle kommt
            float a = (s - BLITZ2) / (EINSCHLAG - BLITZ2);
            return lokal(s, -6, 58, Mth.lerp(a, -54, -74));
        }
        if (s < WASSERLINIE) {
            return lokal(s, -20, 46, 100);
        }
        if (s < UEBERBORD) {
            // Von außen, knapp über dem Wasser: das Boot legt sich auf die Seite
            Vec3 b = bootPos(s);
            return b.add(rechts.scale(-13)).add(land.scale(-7)).with(net.minecraft.core.Direction.Axis.Y, wasser + 0.9);
        }
        if (s < UNTER) {
            // Über Bord: man fällt ins Wasser
            float a = (s - UEBERBORD) / (UNTER - UEBERBORD);
            Vec3 von = lokal(UEBERBORD, -30, 46, 40);
            Vec3 zu = bootPos(s).add(rechts.scale(-8)).add(land.scale(-2)).with(net.minecraft.core.Direction.Axis.Y, wasser - 2.2);
            double k = a * a;
            return von.lerp(zu, k);
        }
        // Unter Wasser: man sinkt und sieht das gekenterte Boot über sich
        float d = s - UNTER;
        return bootPos(s).add(rechts.scale(-7)).add(land.scale(-2)).with(net.minecraft.core.Direction.Axis.Y, wasser - 2.6 - d * 0.7);
    }

    static Vec3 kameraZiel(float s) {
        if (s < AUSSEN) {
            float a = Mth.clamp((s - DECK) / (AUSSEN - DECK), 0, 1);
            return lokal(s, 18, 30, Mth.lerp(a, 40, -20));
        }
        if (s < BUG) {
            return bootPos(s).add(land.scale(26)).add(0, 3, 0);
        }
        if (s < GESICHTER) {
            return ufer.add(land.scale(70)).add(0, 14, 0);
        }
        if (s < BLITZ2) {
            return lokal(s, 0, 30, 80);
        }
        if (s < EINSCHLAG) {
            return ursprungNah.add(0, 16, 0);
        }
        if (s < WASSERLINIE) {
            return lokal(s, 14, 30, 30);
        }
        if (s < UNTER) {
            return bootPos(s).add(0, 3, 0);
        }
        return bootPos(s).add(0, 1.5, 0);
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
        float wackeln = s < REGEN ? 0.5F : 1.0F;
        float roll = 0;
        if (s < AUSSEN || (s >= BUG && s < WASSERLINIE)) {
            roll = roll(s) * 0.5F; // an Bord schaukelt man mit
        }
        if (s >= EINSCHLAG && s < UNTER) {
            wackeln = 4.0F;
        }
        if (s >= UNTER) {
            wackeln = 0.3F;
            roll = Mth.sin(s * 0.6F) * 6;
        }
        roll += Mth.sin(s * 1.7F) * wackeln;
        gier += Mth.sin(s * 2.3F) * wackeln * 0.7F + (s >= EINSCHLAG && s < UNTER ? (float) ZUFALL.nextGaussian() * 0.8F : 0);
        nicken += Mth.sin(s * 3.1F + 1) * wackeln * 0.6F;
        e.setYaw(gier);
        e.setPitch(nicken);
        e.setRoll(roll);
    }

    // ---- Nebel und Licht ----------------------------------------------------------------------

    static float blitz(float s) {
        float b = 0;
        for (float z : new float[]{BLITZ1, BLITZ2}) {
            if (s >= z && s < z + 0.7F) {
                b = Math.max(b, 1 - (s - z) / 0.7F);
            }
            if (s >= z + 0.9F && s < z + 1.05F) {
                b = Math.max(b, 0.6F); // Nachblitz
            }
        }
        if (s >= BLITZ1 - 0.35F && s < BLITZ1 - 0.25F) {
            b = 0.5F;
        }
        return b;
    }

    static boolean violett(float s) {
        return s >= BLITZ2 + 0.2F && s < SCHWARZ;
    }

    public static void nebel(ViewportEvent.RenderFog e) {
        if (!aktiv) {
            return;
        }
        float s = zeit((float) e.getPartialTick());
        if (e.getCamera().getFluidInCamera() == FogType.WATER) {
            e.setNearPlaneDistance(0);
            e.setFarPlaneDistance(22);
            e.setCanceled(true);
            return;
        }
        // Nebel zieht auf: erst weit, dann dicht; Blitze reißen ihn kurz auf
        float weit = s < AUSSEN ? 70 : Mth.lerp(Mth.clamp((s - AUSSEN) / 8, 0, 1), 120, 100);
        float b = blitz(s);
        if (b > 0) {
            weit = Math.max(weit, 70 + 180 * b);
        }
        if (s >= BLITZ2 && s < UEBERBORD) {
            weit = Math.max(weit, 95);
        }
        e.setNearPlaneDistance(weit * 0.25F);
        e.setFarPlaneDistance(weit);
        e.setCanceled(true);
    }

    public static void nebelFarbe(ViewportEvent.ComputeFogColor e) {
        if (!aktiv) {
            return;
        }
        float s = zeit((float) e.getPartialTick());
        float r = 0.055F, g = 0.065F, b = 0.09F;
        if (violett(s)) {
            r = 0.15F;
            g = 0.06F;
            b = 0.22F;
        }
        if (e.getCamera().getFluidInCamera() == FogType.WATER) {
            r = 0.07F;
            g = 0.13F;
            b = 0.20F;
            if (violett(s)) {
                r = 0.13F;
                b = 0.24F;
            }
        }
        float f = blitz(s);
        e.setRed(Mth.lerp(f, r, 0.82F));
        e.setGreen(Mth.lerp(f, g, 0.82F));
        e.setBlue(Mth.lerp(f, b, 0.97F));
    }

    static int lichtBlock() {
        if (!aktiv) {
            return 15;
        }
        float s = zeit(0);
        if (blitz(s) > 0.3F) {
            return 15;
        }
        if (violett(s)) {
            return 8;
        }
        return s < EINSCHLAG ? 6 : 3;
    }

    static int lichtHimmel() {
        if (!aktiv) {
            return 15;
        }
        return blitz(zeit(0)) > 0.3F ? 15 : 2;
    }
}
