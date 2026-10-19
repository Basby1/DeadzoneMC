package de.deadzonemc.client;

import de.deadzonemc.DeadzoneMC;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * Lärmanzeige: ein kleiner Balken links neben der Schnellleiste zeigt, wie weit dein letztes Geräusch zu hören war
 * (Rennen, Glas, Motor, Schuss, Explosion). So wird Schleichen lesbar. Verblasst nach drei Sekunden.
 */
public final class ClientLaerm {

    private static final int DAUER = 60;
    private static int stufe;
    private static long seit;

    private ClientLaerm() {
    }

    public static void empfangen(int radius) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        int neu = Math.max(1, Math.min(5, (int) Math.ceil(radius / 14.5)));
        long jetzt = mc.level.getGameTime();
        if (neu >= aktuell(jetzt)) {
            stufe = neu;
            seit = jetzt;
        }
    }

    private static int aktuell(long jetzt) {
        return jetzt - seit < DAUER && jetzt >= seit ? stufe : 0;
    }

    public static void ebenen(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR, ResourceLocation.fromNamespaceAndPath(DeadzoneMC.MODID, "laerm"),
                (g, delta) -> zeichnen(g));
    }

    private static void zeichnen(GuiGraphics g) {
        if (!ClientEinstellungen.an(ClientEinstellungen.LAERM_ANZEIGE)) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.options.hideGui) {
            return;
        }
        long jetzt = mc.level.getGameTime();
        int s = aktuell(jetzt);
        if (s == 0) {
            return;
        }
        float rest = 1F - (jetzt - seit) / (float) DAUER;
        int alpha = (int) (255 * Math.min(1F, rest * 2.5F));
        if (alpha < 8) {
            return;
        }
        int x = g.guiWidth() / 2 - 91 - 72; // links neben dem Zweithand-Platz
        int y = g.guiHeight() - 14;
        Component t = Component.translatable("hud.deadzonemc.laerm");
        g.drawString(mc.font, t, x, y - 10, (alpha << 24) | 0xB8B4A8, true);
        for (int i = 0; i < 5; i++) {
            int farbe = i >= s ? 0x40404040 : i < 2 ? 0xD8C890 : i < 4 ? 0xE0A040 : 0xE04030;
            int a = i >= s ? alpha / 3 : alpha;
            g.fill(x + i * 8, y, x + i * 8 + 6, y + 6, (a << 24) | (farbe & 0xFFFFFF));
        }
    }
}
