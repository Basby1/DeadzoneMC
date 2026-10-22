package de.deadzonemc.client.charakter;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/** Zwischen Editor und Intro: wartet, bis alle Mitspieler ihren Charakter fertig haben. */
public class WartenScreen extends Screen {

    private int zeit;

    public WartenScreen() {
        super(Component.literal("Warten"));
    }

    @Override
    public void tick() {
        zeit++;
        // Kommt nach drei Sekunden weder Intro noch Wartenachricht, gibt es nichts zu warten
        if (zeit > 60 && ClientCharakter.wartenAuf.isEmpty()) {
            ClientCharakter.weiter();
        }
    }

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float d) {
        g.fill(0, 0, width, height, 0xF0050607);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float d) {
        renderBackground(g, mx, my, d);
        float t = zeit + d;
        int a = (int) (140 + 100 * Mth.sin(t * 0.12F));
        g.drawCenteredString(font, Component.literal("FLUG Q-7 · BOARDING"), width / 2, height / 2 - 30, (a << 24) | 0xB02818);
        if (ClientCharakter.wartenAuf.isEmpty()) {
            g.drawCenteredString(font, Component.literal("Einen Moment …"), width / 2, height / 2 - 8, 0xFFE8DCB8);
        } else {
            g.drawCenteredString(font, Component.literal("Warte auf Mitspieler, die noch ihren Charakter bauen:"),
                    width / 2, height / 2 - 8, 0xFFE8DCB8);
            g.drawCenteredString(font, Component.literal(ClientCharakter.wartenAuf), width / 2, height / 2 + 6, 0xFFF0D080);
            g.drawCenteredString(font, Component.literal("Das Intro startet für alle gemeinsam."), width / 2, height / 2 + 26, 0xFF7A766C);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
