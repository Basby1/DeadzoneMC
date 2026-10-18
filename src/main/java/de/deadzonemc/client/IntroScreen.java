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
        // Schwarzblende am Anfang und Ende
        float ein = Mth.clamp(1 - t / 50F, 0, 1);
        float aus = Mth.clamp((t - (DAUER - 30)) / 30F, 0, 1);
        int a = (int) (Math.max(ein, aus) * 255);
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
            g.drawCenteredString(font, Component.literal("DEADZONE"), 0, 0, ((int) (alpha * 255) << 24) | 0xB02818);
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
    }
}
