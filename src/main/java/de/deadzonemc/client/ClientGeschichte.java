package de.deadzonemc.client;

import com.mojang.blaze3d.platform.InputConstants;
import de.deadzonemc.DeadzoneMC;
import de.deadzonemc.geschichte.GeschichtePayload;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import org.lwjgl.glfw.GLFW;

/** Tagebuch (J) und Zielmarker der Hauptgeschichte. */
public final class ClientGeschichte {

    private ClientGeschichte() {
    }

    public static final KeyMapping TAGEBUCH = new KeyMapping("key.deadzonemc.tagebuch", InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_J, "key.categories.deadzonemc");

    /** Gelesene Notizen (Archiv). */
    public static java.util.List<String> archiv = java.util.List.of();
    static int schritt;
    static int zielX;
    static int zielZ;
    /** -1 = kein Ziel, 0 Oberwelt, 1 Nether, 2 End */
    static int zielWelt = -1;

    public static void empfangen(GeschichtePayload n) {
        schritt = n.schritt();
        zielX = n.x();
        zielZ = n.z();
        zielWelt = n.welt();
    }

    public static void tasten(RegisterKeyMappingsEvent e) {
        e.register(TAGEBUCH);
    }

    public static void tick(ClientTickEvent.Post e) {
        Minecraft mc = Minecraft.getInstance();
        while (TAGEBUCH.consumeClick()) {
            if (mc.screen == null && mc.player != null) {
                mc.setScreen(new TagebuchScreen(schritt));
            }
        }
    }

    public static void ebenen(RegisterGuiLayersEvent e) {
        e.registerAbove(VanillaGuiLayers.HOTBAR, ResourceLocation.fromNamespaceAndPath(DeadzoneMC.MODID, "zielmarker"),
                (g, delta) -> marker(g));
    }

    /** Oben in der Mitte: Pfeil in Zielrichtung und Entfernung. */
    private static void marker(GuiGraphics g) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || mc.options.hideGui || zielWelt < 0 || mc.level == null) {
            return;
        }
        int welt = mc.level.dimension() == Level.NETHER ? 1 : mc.level.dimension() == Level.END ? 2 : 0;
        if (welt != zielWelt) {
            return;
        }
        double dx = zielX + 0.5 - p.getX();
        double dz = zielZ + 0.5 - p.getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);
        float winkelZiel = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float rel = Mth.wrapDegrees(winkelZiel - p.getYRot());
        int b = g.guiWidth();
        int mitte = b / 2;
        int y = 4;
        // Leiste: ±90° sichtbar
        int breite = 120;
        g.fill(mitte - breite / 2, y + 6, mitte + breite / 2, y + 7, 0x66C8C0A8);
        if (Math.abs(rel) <= 90) {
            int x = mitte + Math.round(rel / 90F * breite / 2);
            g.fill(x - 2, y + 2, x + 3, y + 11, 0xFFE0A030);
            g.fill(x - 1, y + 1, x + 2, y + 12, 0xFFFFD27A);
        } else {
            String pfeil = rel < 0 ? "◀" : "▶";
            g.drawString(mc.font, pfeil, rel < 0 ? mitte - breite / 2 - 8 : mitte + breite / 2 + 2, y + 2, 0xFFE0A030, true);
        }
        String entfernung = dist < 1000 ? Math.round(dist) + " m" : String.format("%.1f km", dist / 1000);
        net.minecraft.network.chat.Component titel = schritt < de.deadzonemc.geschichte.Geschichte.SCHRITTE.size()
                ? net.minecraft.network.chat.Component.translatable("geschichte.deadzonemc."
                + de.deadzonemc.geschichte.Geschichte.SCHRITTE.get(schritt).id() + ".titel")
                : net.minecraft.network.chat.Component.empty();
        net.minecraft.network.chat.Component zeile = dist < 24
                ? net.minecraft.network.chat.Component.translatable("ziel.deadzonemc.erreicht", titel)
                : net.minecraft.network.chat.Component.translatable("ziel.deadzonemc.zeile", titel, entfernung);
        g.drawCenteredString(mc.font, zeile, mitte, y + 14, 0xFFE8DCB8);
    }
}
