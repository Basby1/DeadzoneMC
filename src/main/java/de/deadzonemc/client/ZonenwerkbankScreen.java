package de.deadzonemc.client;

import de.deadzonemc.verzauberung.Zonenverzauberung;
import de.deadzonemc.verzauberung.ZonenwerkbankMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

import java.util.ArrayList;
import java.util.List;

/** Oberfläche der Zonenwerkbank: links Gegenstand und Material, rechts die Liste der möglichen Verbesserungen. */
public class ZonenwerkbankScreen extends AbstractContainerScreen<ZonenwerkbankMenu> {

    private static final int LX = 42;
    private static final int LY = 8;
    private static final int LB = 180;
    private static final int ZEILE = 20;
    private static final int ZEILEN = 5;

    private int versatz;

    public ZonenwerkbankScreen(ZonenwerkbankMenu menu, Inventory inv, Component titel) {
        super(menu, inv, titel);
        imageWidth = 230;
        imageHeight = 200;
        inventoryLabelX = 35;
        inventoryLabelY = 107;
        titleLabelX = 8;
        titleLabelY = -10;
    }

    private List<Holder<Enchantment>> liste() {
        if (minecraft == null || minecraft.level == null) {
            return new ArrayList<>();
        }
        return ZonenwerkbankMenu.passende(minecraft.level.registryAccess(), menu.gegenstand());
    }

    @Override
    protected void renderBg(GuiGraphics g, float delta, int mx, int my) {
        int x = leftPos;
        int y = topPos;
        g.fill(x, y, x + imageWidth, y + imageHeight, 0xF0181A18);
        g.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xF02A2D2A);
        rahmen(g, x + 16, y + 21);
        rahmen(g, x + 16, y + 57);
        g.drawString(font, "⚙", x + 20, y + 44, 0xFF7ADC8A, false);
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 9; c++) {
                rahmen(g, x + 34 + c * 18, y + 117 + r * 18);
            }
        }
        for (int c = 0; c < 9; c++) {
            rahmen(g, x + 34 + c * 18, y + 175);
        }
        g.fill(x + LX - 1, y + LY - 1, x + LX + LB + 1, y + LY + ZEILE * ZEILEN + 1, 0xFF101210);
    }

    private static void rahmen(GuiGraphics g, int x, int y) {
        g.fill(x, y, x + 18, y + 18, 0xFF101210);
        g.fill(x + 1, y + 1, x + 17, y + 17, 0xFF3A3E3A);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float delta) {
        super.render(g, mx, my, delta);
        int x = leftPos + LX;
        int y = topPos + LY;
        List<Holder<Enchantment>> l = liste();
        ItemStack s = menu.gegenstand();
        if (s.isEmpty()) {
            g.drawWordWrap(font, Component.translatable("verzauberung.deadzonemc.hinweis"), x + 6, y + 8, LB - 12, 0xFF9A9A90);
        } else if (l.isEmpty()) {
            g.drawWordWrap(font, Component.translatable("verzauberung.deadzonemc.keine"), x + 6, y + 8, LB - 12, 0xFF9A9A90);
        }
        versatz = Math.max(0, Math.min(versatz, l.size() - ZEILEN));
        Component tipp = null;
        for (int i = 0; i < ZEILEN && i + versatz < l.size(); i++) {
            Holder<Enchantment> h = l.get(i + versatz);
            int zy = y + i * ZEILE;
            int jetzt = EnchantmentHelper.getItemEnchantmentLevel(h, s);
            Component grund = ZonenwerkbankMenu.hindernis(minecraft.player, s, menu.material(), h);
            boolean ueber = mx >= x && mx < x + LB && my >= zy && my < zy + ZEILE;
            int hinter = grund == null ? (ueber ? 0xFF3E6A44 : 0xFF2E4A32) : (ueber ? 0xFF3A3A36 : 0xFF262824);
            g.fill(x, zy, x + LB, zy + ZEILE - 1, hinter);
            Component name = jetzt >= h.value().getMaxLevel() ? Enchantment.getFullname(h, jetzt)
                    : Enchantment.getFullname(h, jetzt + 1);
            g.drawString(font, name.copy().withStyle(net.minecraft.ChatFormatting.RESET), x + 4, zy + 2,
                    grund == null ? 0xFFB8F0B0 : 0xFFC8C8C0, false);
            int neu = jetzt + 1;
            Component kosten = jetzt >= h.value().getMaxLevel() ? Component.translatable("verzauberung.deadzonemc.voll")
                    : Component.translatable("verzauberung.deadzonemc.kosten", ZonenwerkbankMenu.kostenStufen(neu), neu,
                    Zonenverzauberung.material(ZonenwerkbankMenu.id(h)).getDescription());
            g.drawString(font, kosten, x + 4, zy + 11, grund == null ? 0xFF8AD08A : 0xFFB06A5A, false);
            if (ueber) {
                tipp = Component.translatable("enchantment.deadzonemc." + ZonenwerkbankMenu.id(h) + ".desc");
            }
        }
        if (l.size() > ZEILEN) {
            int hoehe = ZEILE * ZEILEN;
            int balken = Math.max(8, hoehe * ZEILEN / l.size());
            int pos = (hoehe - balken) * versatz / Math.max(1, l.size() - ZEILEN);
            g.fill(x + LB - 3, y + pos, x + LB - 1, y + pos + balken, 0xFF8A8E8A);
        }
        renderTooltip(g, mx, my);
        if (tipp != null && menu.getCarried().isEmpty()) {
            g.renderTooltip(font, font.split(tipp, 180), mx, my);
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int knopf) {
        int x = leftPos + LX;
        int y = topPos + LY;
        if (mx >= x && mx < x + LB && my >= y && my < y + ZEILE * ZEILEN && minecraft != null && minecraft.gameMode != null) {
            int i = (int) ((my - y) / ZEILE) + versatz;
            List<Holder<Enchantment>> l = liste();
            if (i < l.size() && ZonenwerkbankMenu.hindernis(minecraft.player, menu.gegenstand(), menu.material(), l.get(i)) == null) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, i);
                return true;
            }
        }
        return super.mouseClicked(mx, my, knopf);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double dx, double dy) {
        versatz = Math.max(0, versatz - (int) Math.signum(dy));
        return true;
    }
}
