package de.deadzonemc.client;

import com.mojang.blaze3d.shaders.FogShape;
import de.deadzonemc.wetter.WetterArt;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FogType;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

/**
 * Der Nebel der Deadzone auf dem Client.
 *
 * <p>Sichtweite und Farbe gleiten langsam zum Zielwert des aktuellen Wetters. Nachts wird der Nebel
 * fast schwarz, in Höhlen ebenfalls – Licht ist dann das Einzige, was hilft.</p>
 */
public final class ClientWetter {

    /** Wie schnell der Nebel sich an ein neues Wetter anpasst (Anteil pro Tick). */
    private static final float ANPASSUNG = 0.015F;

    private static WetterArt art = WetterArt.LEICHTER_NEBEL;

    private static boolean bereit = false;
    private static float sicht = WetterArt.LEICHTER_NEBEL.sicht();
    private static float nah = WetterArt.LEICHTER_NEBEL.nah();
    private static float rot = WetterArt.LEICHTER_NEBEL.rot();
    private static float gruen = WetterArt.LEICHTER_NEBEL.gruen();
    private static float blau = WetterArt.LEICHTER_NEBEL.blau();
    /** 0 = unter freiem Himmel, 1 = tief in einer Höhle. */
    private static float hoehle = 0F;

    private ClientWetter() {
    }

    /** Ob unser Nebel gezeichnet wird (per /deadzone nebel abschaltbar). */
    private static boolean nebelAn = true;

    public static void setzen(int nummer, boolean nebel) {
        art = WetterArt.ausNummer(nummer);
        nebelAn = nebel;
    }

    /** 0 = ruhig, 1 = Emission droht, 2 = Emission tobt. */
    private static int emission;
    private static float emissionAnteil;

    public static void emissionSetzen(int stufe) {
        emission = stufe;
    }

    public static int emission() {
        return emission;
    }

    public static WetterArt art() {
        return art;
    }

    private static boolean aktiv(ClientLevel level) {
        return level != null && level.dimension() == Level.OVERWORLD;
    }

    // ---- jeden Tick: Werte angleichen, Asche rieseln lassen ------------------------------------

    public static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        LocalPlayer spieler = mc.player;
        if (!aktiv(level) || spieler == null) {
            bereit = false;
            return;
        }

        BlockPos kopf = BlockPos.containing(spieler.getEyePosition());
        boolean draussen = level.canSeeSky(kopf);
        float hoehleZiel = !draussen && kopf.getY() < level.getSeaLevel() - 4 ? 1F : 0F;

        float a = bereit ? ANPASSUNG : 1F;
        sicht = Mth.lerp(a, sicht, art.sicht());
        nah = Mth.lerp(a, nah, art.nah());
        rot = Mth.lerp(a, rot, art.rot());
        gruen = Mth.lerp(a, gruen, art.gruen());
        blau = Mth.lerp(a, blau, art.blau());
        hoehle = Mth.lerp(bereit ? 0.03F : 1F, hoehle, hoehleZiel);
        emissionAnteil = Mth.lerp(0.01F, emissionAnteil, emission == 2 ? 1F : emission == 1 ? 0.45F : 0F);
        bereit = true;

        if (art == WetterArt.ASCHESTURM && draussen && !mc.isPaused()) {
            ascheRieseln(level, spieler);
        }
    }

    private static void ascheRieseln(ClientLevel level, LocalPlayer spieler) {
        RandomSource zufall = level.random;
        for (int i = 0; i < 14; i++) {
            double x = spieler.getX() + (zufall.nextDouble() - 0.5) * 20;
            double y = spieler.getY() + zufall.nextDouble() * 8 - 1;
            double z = spieler.getZ() + (zufall.nextDouble() - 0.5) * 20;
            level.addParticle(zufall.nextInt(4) == 0 ? ParticleTypes.WHITE_ASH : ParticleTypes.ASH,
                    x, y, z, 0.08, -0.03, 0.02);
        }
    }

    // ---- Nebel ---------------------------------------------------------------------------------

    /** Helligkeit des Himmels: 1 am Tag, 0 tief in der Nacht. */
    private static float tageslicht(ClientLevel level, float partialTick) {
        float zeit = level.getTimeOfDay(partialTick);
        return Mth.clamp(Mth.cos(zeit * (float) (Math.PI * 2.0)) * 2.0F + 0.5F, 0.0F, 1.0F);
    }

    public static void nebel(ViewportEvent.RenderFog event) {
        ClientLevel level = Minecraft.getInstance().level;
        if (!nebelAn || !aktiv(level) || !bereit || event.getType() != FogType.NONE) {
            return;
        }
        float licht = tageslicht(level, (float) event.getPartialTick());
        float weite = sicht * (0.75F + 0.25F * licht) * ("spaeher".equals(ClientKlasse.klasse()) ? 1.5F : 1.0F);
        weite = Mth.lerp(hoehle, weite, 26F);
        weite = Mth.lerp(emissionAnteil * 0.5F, weite, 40F);
        float anfang = Mth.lerp(hoehle, nah, 0F);

        event.setFarPlaneDistance(Math.min(event.getFarPlaneDistance(), weite));
        event.setNearPlaneDistance(Math.min(anfang, weite * 0.5F));
        event.setFogShape(FogShape.SPHERE);
        event.setCanceled(true);
    }

    public static void nebelFarbe(ViewportEvent.ComputeFogColor event) {
        ClientLevel level = Minecraft.getInstance().level;
        Camera kamera = event.getCamera();
        if (!nebelAn || !aktiv(level) || !bereit || kamera.getFluidInCamera() != FogType.NONE) {
            return;
        }
        float licht = tageslicht(level, (float) event.getPartialTick());
        // Nachts fast schwarz, aber nicht ganz – Umrisse sollen noch erahnbar sein.
        float hell = 0.06F + 0.94F * licht;
        if (level.isRaining()) {
            hell *= 0.85F;
        }
        hell = Mth.lerp(hoehle, hell, 0.03F);
        // Emission: der Himmel färbt sich blutrot und wird heller
        float e = emissionAnteil * (1F - hoehle);
        event.setRed(Mth.lerp(e, rot * hell, 0.62F));
        event.setGreen(Mth.lerp(e, gruen * hell, 0.07F));
        event.setBlue(Mth.lerp(e, blau * hell, 0.05F));
    }
}
