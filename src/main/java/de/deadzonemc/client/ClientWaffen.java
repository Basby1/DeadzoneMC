package de.deadzonemc.client;

import com.mojang.blaze3d.platform.InputConstants;
import de.deadzonemc.DeadzoneMC;
import de.deadzonemc.waffen.feuer.Feuerwaffen;
import de.deadzonemc.waffen.feuer.MunitionItem;
import de.deadzonemc.waffen.feuer.SchusswaffeItem;
import de.deadzonemc.waffen.feuer.WaffenArt;
import de.deadzonemc.waffen.feuer.WaffenPayloads;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/**
 * Schusswaffen auf dem Client: Linksklick schießt (bei Automatik solange gedrückt), Rechtsklick zielt (Zoom,
 * weniger Streuung), R lädt nach. Rückstoß reißt die Kamera hoch. Anzeige: Magazin / Vorrat, Nachladebalken,
 * Trefferkreuz, Zielfernrohr.
 */
public final class ClientWaffen {

    private ClientWaffen() {
    }

    public static final KeyMapping NACHLADEN = new KeyMapping("key.deadzonemc.nachladen",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, "key.categories.deadzonemc");

    private static int abklingen;
    private static boolean warGedrueckt;
    private static boolean zielen;
    private static int ladenGesamt;
    private static int ladenRest;
    private static int trefferAnzeige;
    private static int trefferArt;
    private static float rueckstossRest;

    public static void tasten(RegisterKeyMappingsEvent event) {
        event.register(NACHLADEN);
    }

    private static SchusswaffeItem waffe(LocalPlayer p) {
        return p != null && p.getMainHandItem().getItem() instanceof SchusswaffeItem w ? w : null;
    }

    public static boolean zielt() {
        return zielen;
    }

    /** Linksklick/Rechtsklick mit Waffe: kein Schlag, kein Benutzen – das übernimmt der Tick. */
    public static void klick(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft mc = Minecraft.getInstance();
        if (waffe(mc.player) == null) {
            return;
        }
        if (event.isAttack() || event.isUseItem()) {
            event.setCanceled(true);
            event.setSwingHand(false);
        }
    }

    public static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (abklingen > 0) {
            abklingen--;
        }
        if (trefferAnzeige > 0) {
            trefferAnzeige--;
        }
        if (ladenRest > 0) {
            ladenRest--;
        }
        if (rueckstossRest > 0.05F && p != null) {
            float schritt = rueckstossRest * 0.45F;
            p.setXRot(p.getXRot() - schritt);
            rueckstossRest -= schritt;
        }
        SchusswaffeItem w = waffe(p);
        if (w == null || mc.screen != null) {
            zielen = false;
            warGedrueckt = false;
            while (NACHLADEN.consumeClick()) {
                // nichts
            }
            return;
        }
        WaffenArt a = w.art;
        zielen = mc.options.keyUse.isDown();
        while (NACHLADEN.consumeClick()) {
            PacketDistributor.sendToServer(new WaffenPayloads.Laden(0));
        }
        boolean gedrueckt = mc.options.keyAttack.isDown();
        if (gedrueckt && abklingen <= 0 && (a.auto || !warGedrueckt)) {
            ItemStack s = p.getMainHandItem();
            int mag = s.getOrDefault(Feuerwaffen.MAGAZIN.get(), 0);
            PacketDistributor.sendToServer(new WaffenPayloads.Schuss(zielen));
            abklingen = a.takt;
            if (mag > 0 && ladenRest <= 0) {
                float r = a.rueckstoss * (zielen ? 0.6F : 1.0F) * (Feuerwaffen.hatAufsatz(s, "griff") ? 0.6F : 1.0F)
                        * (p.isShiftKeyDown() ? 0.8F : 1.0F) * (de.deadzonemc.ausruestung.Satzboni.taktisch(p) ? 0.75F : 1.0F)
                        * (ClientKlasse.klasse().equals("schuetze") && de.deadzonemc.klasse.Klassenvorteile.liebling(s.getItem())
                        == de.deadzonemc.klasse.Klasse.SCHUETZE ? 0.7F : 1.0F);
                rueckstossRest += r;
                p.setYRot(p.getYRot() + (p.getRandom().nextFloat() - 0.5F) * r * 0.5F);
            }
        }
        warGedrueckt = gedrueckt;
    }

    public static void rueckmeldung(WaffenPayloads.Rueckmeldung n) {
        if (n.treffer() > 0) {
            trefferAnzeige = 8;
            trefferArt = n.treffer();
        }
        if (n.ladenTicks() > 0) {
            ladenGesamt = n.ladenTicks();
            ladenRest = n.ladenTicks();
        } else if (n.ladenTicks() < 0) {
            ladenRest = 0;
        }
    }

    public static void fov(ComputeFovModifierEvent event) {
        LocalPlayer p = Minecraft.getInstance().player;
        if (p != null && p.isUsingItem() && p.getUseItem().is(de.deadzonemc.gadget.Gadgets.FERNGLAS.get())) {
            event.setNewFovModifier(event.getNewFovModifier() * 0.2F);
            return;
        }
        if (!zielen || waffe(p) == null) {
            return;
        }
        boolean fernrohr = Feuerwaffen.hatAufsatz(p.getMainHandItem(), "zielfernrohr");
        event.setNewFovModifier(event.getNewFovModifier() * (fernrohr ? 0.28F : 0.78F));
    }

    public static void ebenen(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR, ResourceLocation.fromNamespaceAndPath(DeadzoneMC.MODID, "waffe"),
                (g, delta) -> zeichnen(g));
    }

    private static void zeichnen(GuiGraphics g) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        SchusswaffeItem w = waffe(p);
        if (w == null || mc.options.hideGui) {
            return;
        }
        int b = g.guiWidth();
        int h = g.guiHeight();
        ItemStack s = p.getMainHandItem();
        if (zielen && Feuerwaffen.hatAufsatz(s, "zielfernrohr")) {
            fernrohr(g, b, h);
        }
        int mag = s.getOrDefault(Feuerwaffen.MAGAZIN.get(), 0);
        int vorrat = 0;
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            ItemStack x = p.getInventory().getItem(i);
            if (x.getItem() instanceof MunitionItem m && m.kaliber == w.art.kaliber) {
                vorrat += x.getCount();
            }
        }
        String text = mag + " / " + vorrat;
        int farbe = mag == 0 ? 0xFFE04040 : mag <= Math.max(1, w.art.magazin / 4) ? 0xFFE0B040 : 0xFFE8E4D8;
        int x = b - mc.font.width(text) - 12;
        int y = h - 24;
        g.drawString(mc.font, text, x, y, farbe, true);
        Component kal = Component.translatable("item.deadzonemc." + w.art.kaliber.itemId());
        g.drawString(mc.font, kal, b - mc.font.width(kal) - 12, y - 11, 0xFF9A9A90, true);
        if (ladenRest > 0 && ladenGesamt > 0) {
            int breite = 60;
            int bx = b / 2 - breite / 2;
            int by = h / 2 + 14;
            g.fill(bx - 1, by - 1, bx + breite + 1, by + 4, 0xAA000000);
            g.fill(bx, by, bx + (int) (breite * (1 - ladenRest / (float) ladenGesamt)), by + 3, 0xFFD8C890);
            Component t = Component.translatable("waffe.deadzonemc.laedt");
            g.drawString(mc.font, t, b / 2 - mc.font.width(t) / 2, by + 6, 0xFFD8C890, true);
        }
        if (trefferAnzeige > 0) {
            int c = trefferArt == 3 ? 0xFFFF3030 : trefferArt == 2 ? 0xFFFFC040 : 0xFFFFFFFF;
            int mx = b / 2;
            int my = h / 2;
            for (int i = 3; i < 7; i++) {
                g.fill(mx - i - 1, my - i - 1, mx - i + 1, my - i + 1, c);
                g.fill(mx + i - 1, my - i - 1, mx + i + 1, my - i + 1, c);
                g.fill(mx - i - 1, my + i - 1, mx - i + 1, my + i + 1, c);
                g.fill(mx + i - 1, my + i - 1, mx + i + 1, my + i + 1, c);
            }
        }
    }

    /** Schwarzer Rand mit rundem Ausschnitt und Fadenkreuz. */
    private static void fernrohr(GuiGraphics g, int b, int h) {
        int r = (int) (Math.min(b, h) * 0.42);
        int mx = b / 2;
        int my = h / 2;
        g.fill(0, 0, b, my - r, 0xFF000000);
        g.fill(0, my + r, b, h, 0xFF000000);
        for (int y = my - r; y < my + r; y++) {
            int dy = y - my;
            int dx = (int) Math.sqrt(Math.max(0, r * r - dy * dy));
            g.fill(0, y, mx - dx, y + 1, 0xFF000000);
            g.fill(mx + dx, y, b, y + 1, 0xFF000000);
        }
        g.fill(mx - r, my, mx + r, my + 1, 0xCC101010);
        g.fill(mx, my - r, mx + 1, my + r, 0xCC101010);
        g.fill(mx - 1, my - 1, mx + 2, my + 2, 0xFFD02020);
    }
}
