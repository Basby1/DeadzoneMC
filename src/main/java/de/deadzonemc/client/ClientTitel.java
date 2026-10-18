package de.deadzonemc.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.event.ScreenEvent;

/**
 * Bewegter Titelbildschirm: Über das Ruinenpanorama ziehen langsam Nebelbänke, der Rand ist abgedunkelt, und ab
 * und zu flackert fernes Wetterleuchten über den Himmel.
 */
public final class ClientTitel {

    private ClientTitel() {
    }

    public static void danach(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof TitleScreen s)) {
            return;
        }
        GuiGraphics g = event.getGuiGraphics();
        int b = s.width;
        int h = s.height;
        double t = System.currentTimeMillis() / 1000.0;
        // Nebelbänke: weiche, waagerechte Streifen, die unterschiedlich schnell treiben
        for (int i = 0; i < 7; i++) {
            double tempo = 6 + i * 3.5;
            int y = (int) (h * (0.35 + 0.09 * i) + Math.sin(t * 0.21 + i) * 6);
            int hoehe = 18 + (i % 3) * 10;
            int versatz = (int) ((t * tempo + i * 137) % (b + 400)) - 200;
            for (int k = -1; k <= 1; k++) {
                int x0 = versatz + k * (b + 400) / 2;
                for (int s2 = 0; s2 < hoehe; s2 += 3) {
                    double rand = 1 - Math.abs(s2 - hoehe / 2.0) / (hoehe / 2.0);
                    int alpha = (int) (rand * (14 + i * 3));
                    if (alpha <= 0) {
                        continue;
                    }
                    g.fill(Math.max(0, x0), y + s2, Math.min(b, x0 + b / 2 + 200), y + s2 + 3, (alpha << 24) | 0xB8BEC0);
                }
            }
        }
        // Rand abdunkeln
        for (int r = 0; r < 24; r++) {
            int a = (int) (Mth.square(1 - r / 24.0) * 90);
            int col = a << 24;
            g.fill(0, r * 2, b, r * 2 + 2, col);
            g.fill(0, h - r * 2 - 2, b, h - r * 2, col);
        }
        // Wetterleuchten: selten, kurz
        double blitz = (t % 23.0);
        if (blitz < 0.18 || (blitz > 0.3 && blitz < 0.38)) {
            g.fill(0, 0, b, h / 2, 0x22C8D8FF);
        }
    }
}
