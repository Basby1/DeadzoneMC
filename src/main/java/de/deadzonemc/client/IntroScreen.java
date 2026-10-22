package de.deadzonemc.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.event.ViewportEvent;

/**
 * Intro beim ersten Betreten: Schwarzblende, Kinobalken, die Kamera schwenkt langsam über die Absturzstelle,
 * dazu Titel und die Namen aller Überlebenden. Leertaste oder Esc überspringt.
 */
public class IntroScreen extends Screen {

    private static final int DAUER = 20 * 16;
    private static IntroScreen aktiv;
    private final String namen;
    private int zeit;
    private float startGier;

    public IntroScreen(String namen) {
        super(Component.empty());
        this.namen = namen;
    }

    public static void starten(String namen) {
        Minecraft mc = Minecraft.getInstance();
        IntroScreen s = new IntroScreen(namen);
        s.startGier = mc.player != null ? mc.player.getYRot() : 0;
        aktiv = s;
        if (mc.screen instanceof KlassenScreen) {
            ClientKlasse.klassenwahlDanach = true; // erst das Intro, dann die Wahl
        }
        mc.setScreen(s);
    }

    /** Kameraschwenk (nur während des Intros). */
    public static void kamera(ViewportEvent.ComputeCameraAngles e) {
        if (aktiv == null || Minecraft.getInstance().screen != aktiv) {
            aktiv = null;
            return;
        }
        float t = (float) ((aktiv.zeit + e.getPartialTick()) / DAUER);
        e.setYaw(aktiv.startGier - 120 + 160 * t);
        e.setPitch(12 - 10 * t);
    }

    @Override
    public void tick() {
        if (++zeit >= DAUER) {
            onClose();
        }
        // Am Horizont verglimmt eine violette Lichtsäule – dort, wo der Schatten stand
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.player != null && zeit > 30 && zeit < 240) {
            float w = (startGier - 40) * Mth.DEG_TO_RAD;
            double x = mc.player.getX() - Mth.sin(w) * 70;
            double z = mc.player.getZ() + Mth.cos(w) * 70;
            int menge = zeit < 140 ? 6 : (240 - zeit) / 20;
            for (int i = 0; i < menge; i++) {
                double y = mc.player.getY() + mc.level.random.nextDouble() * 70;
                mc.level.addParticle(net.minecraft.core.particles.ParticleTypes.END_ROD, x + mc.level.random.nextGaussian() * 0.6, y,
                        z + mc.level.random.nextGaussian() * 0.6, 0, 0.02, 0);
                mc.level.addParticle(net.minecraft.core.particles.ParticleTypes.REVERSE_PORTAL, x, y, z, 0, 0.05, 0);
            }
        }
    }

    /** Lider: 1 = geschlossen. Erst blinzeln, dann langsam auf. */
    private float lider(float t) {
        if (t < 12) {
            return 1;
        }
        if (t < 22) {
            return 1 - 0.45F * (t - 12) / 10F;
        }
        if (t < 28) {
            return 0.55F + 0.45F * (t - 22) / 6F;
        }
        if (t < 34) {
            return 1;
        }
        if (t < 70) {
            return 1 - (t - 34) / 36F;
        }
        return 0;
    }

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float d) {
        // keine Abdunklung: man soll die Welt sehen
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float d) {
        float t = zeit + d;
        int balken = height / 8;
        g.fill(0, 0, width, balken, 0xFF000000);
        g.fill(0, height - balken, width, height, 0xFF000000);
        // Aufwachen: die Augen öffnen sich (zweimal blinzeln), am Ende Schwarzblende
        float lid = lider(t);
        if (lid > 0) {
            int h = (int) (height / 2F * lid) + 1;
            g.fill(0, 0, width, h, 0xFF000000);
            g.fill(0, height - h, width, height, 0xFF000000);
            g.fill(0, 0, width, height, ((int) (lid * 160) << 24));
        }
        float aus = Mth.clamp((t - (DAUER - 30)) / 30F, 0, 1);
        int a = (int) (aus * 255);
        if (a > 0) {
            g.fill(0, 0, width, height, a << 24);
        }
        zeile(g, t, 30, 120, Component.translatable("intro.deadzonemc.1"), height / 2 - 30, 0xE8DCB8);
        zeile(g, t, 110, 200, Component.translatable("intro.deadzonemc.2"), height / 2 - 10, 0xC8C0A8);
        zeile(g, t, 190, 290, Component.translatable("intro.deadzonemc.ueberlebende", namen), height / 2 + 10, 0xF0D080);
        if (t > 250) {
            float alpha = Mth.clamp((t - 250) / 20F, 0, 1);
            g.pose().pushPose();
            g.pose().translate(width / 2F, height / 2F - 50, 0);
            g.pose().scale(3, 3, 1);
            g.drawCenteredString(font, Component.literal("DEADZONE"), 0, 0, (Math.max(4, (int) (alpha * 255)) << 24) | 0xB02818);
            g.pose().popPose();
        }
        g.drawString(font, Component.translatable("intro.deadzonemc.ueberspringen"), width - 120, height - balken / 2 - 4, 0x60FFFFFF, false);
    }

    private void zeile(GuiGraphics g, float t, int von, int bis, Component text, int y, int farbe) {
        if (t < von || t > bis) {
            return;
        }
        float alpha = Math.min(Mth.clamp((t - von) / 20F, 0, 1), Mth.clamp((bis - t) / 20F, 0, 1));
        int a = Math.max(4, (int) (alpha * 255));
        g.drawCenteredString(font, text, width / 2, y, (a << 24) | farbe);
    }

    @Override
    public boolean keyPressed(int k, int s, int m) {
        if (k == 32 || k == 256) {
            de.deadzonemc.client.kino.ClientKino.tonStoppen();
            onClose();
            return true;
        }
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        aktiv = null;
        super.onClose();
        if (ClientKlasse.klassenwahlDanach) {
            ClientKlasse.klassenwahlDanach = false;
            Minecraft.getInstance().setScreen(new KlassenScreen());
        }
    }
}
