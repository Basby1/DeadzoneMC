package de.deadzonemc.client;

import de.deadzonemc.DeadzoneMC;
import de.deadzonemc.geschichte.TutorialPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/** Erste Schritte: das Feld links am Bildschirm, das sich selbst abhakt. */
public final class ClientTutorial {

    private ClientTutorial() {
    }

    /** Reihenfolge der Schritte (Bit = Index). */
    private static final String[] SCHRITTE = {"wrack", "tagebuch", "klasse", "handbuch", "folgen", "zuflucht"};
    private static int erledigt = -1; // -1 = noch nicht vom Server bekommen
    private static Vec3 start;
    private static long fertigZeit = -1;
    private static long abgehaktZeit = -1;

    public static void empfangen(TutorialPayload n) {
        erledigt = n.erledigt();
        start = null;
    }

    private static boolean hat(int i) {
        return (erledigt & (1 << i)) != 0;
    }

    private static void abhaken(int i) {
        if (erledigt < 0 || hat(i)) {
            return;
        }
        erledigt |= 1 << i;
        abgehaktZeit = System.currentTimeMillis();
        PacketDistributor.sendToServer(new TutorialPayload(erledigt));
    }

    private static int aktuell() {
        for (int i = 0; i < SCHRITTE.length; i++) {
            if (!hat(i)) {
                return i;
            }
        }
        return -1;
    }

    public static void tick(ClientTickEvent.Post e) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || erledigt < 0) {
            return;
        }
        if (start == null) {
            start = p.position();
        }
        // Wrack: irgendeinen Behälter geöffnet (nicht das eigene Inventar)
        if (mc.screen instanceof AbstractContainerScreen<?> && !(mc.screen instanceof InventoryScreen)) {
            abhaken(0);
        }
        if (mc.screen instanceof TagebuchScreen) {
            abhaken(1);
        }
        if (!ClientKlasse.gewaehlt().isEmpty()) {
            abhaken(2);
        }
        if (mc.screen instanceof BookViewScreen) {
            abhaken(3);
        }
        if (start != null && p.position().subtract(start).horizontalDistance() > 80) {
            abhaken(4);
        }
        if (ClientGeschichte.schritt >= 1) {
            abhaken(4);
            abhaken(5);
        }
        if (aktuell() < 0 && fertigZeit < 0) {
            fertigZeit = System.currentTimeMillis();
        }
    }

    public static void ebenen(RegisterGuiLayersEvent e) {
        e.registerAbove(VanillaGuiLayers.HOTBAR, ResourceLocation.fromNamespaceAndPath(DeadzoneMC.MODID, "erste_schritte"),
                (g, delta) -> zeichnen(g));
    }

    private static void zeichnen(GuiGraphics g) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || erledigt < 0 || mc.screen != null) {
            return;
        }
        int i = aktuell();
        long jetzt = System.currentTimeMillis();
        if (i < 0 && (fertigZeit < 0 || jetzt - fertigZeit > 12000)) {
            return; // alles erledigt: das Feld verschwindet
        }
        int x = 6;
        int y = g.guiHeight() / 3;
        int breite = 170;
        Component kopf = i < 0 ? Component.translatable("tutorial.deadzonemc.fertig.titel")
                : Component.translatable("tutorial.deadzonemc.titel", Integer.bitCount(erledigt) + 1, SCHRITTE.length);
        Component text = i < 0 ? Component.translatable("tutorial.deadzonemc.fertig")
                : Component.translatable("tutorial.deadzonemc." + SCHRITTE[i]);
        List<FormattedCharSequence> zeilen = mc.font.split(text, breite - 10);
        int hoehe = 16 + zeilen.size() * 10;
        g.fill(x, y, x + breite, y + hoehe, 0x90101418);
        g.fill(x, y, x + 2, y + hoehe, abgehaktZeit > 0 && jetzt - abgehaktZeit < 1500 ? 0xFF8AE08A : 0xFFE0A030);
        g.drawString(mc.font, kopf, x + 6, y + 4, 0xFFE0A030, false);
        int zy = y + 15;
        for (FormattedCharSequence z : zeilen) {
            g.drawString(mc.font, z, x + 6, zy, 0xFFE8E4D6, false);
            zy += 10;
        }
    }
}
