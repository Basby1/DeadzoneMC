package de.deadzonemc.client;

import de.deadzonemc.DeadzoneMC;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipe;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Feldhandbuch, Teil „Rezepte“: alle Deadzone-Rezepte als Raster mit Zutaten und Ergebnis, seitenweise.
 * Zutaten mit mehreren Möglichkeiten (z. B. beliebige Bretter) wechseln jede Sekunde durch.
 */
public class RezeptbuchScreen extends Screen {

    private static final int PRO_SEITE = 6;
    private static final int B = 300;
    private static final int H = 232;

    private final List<RecipeHolder<?>> rezepte = new ArrayList<>();
    private int seite;
    private String suche = "";

    public RezeptbuchScreen() {
        super(Component.translatable("handbuch.deadzonemc.rezepte"));
    }

    @Override
    protected void init() {
        rezepte.clear();
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            for (RecipeHolder<?> r : mc.level.getRecipeManager().getRecipes()) {
                ItemStack e = r.value().getResultItem(mc.level.registryAccess());
                if (!e.isEmpty() && DeadzoneMC.MODID.equals(BuiltInRegistries.ITEM.getKey(e.getItem()).getNamespace())
                        && !r.value().getIngredients().isEmpty()) {
                    rezepte.add(r);
                }
            }
            rezepte.sort(Comparator.comparing(r -> r.value().getResultItem(mc.level.registryAccess()).getHoverName().getString()));
        }
        int x = (width - B) / 2;
        int y = (height - H) / 2;
        addRenderableWidget(Button.builder(Component.literal("<"), b -> seite = Math.max(0, seite - 1))
                .bounds(x + 8, y + H - 24, 20, 18).build());
        addRenderableWidget(Button.builder(Component.literal(">"), b -> seite = Math.min(seiten() - 1, seite + 1))
                .bounds(x + B - 28, y + H - 24, 20, 18).build());
    }

    private int seiten() {
        return Math.max(1, (rezepte.size() + PRO_SEITE - 1) / PRO_SEITE);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float delta) {
        super.render(g, mx, my, delta);
        int x = (width - B) / 2;
        int y = (height - H) / 2;
        Minecraft mc = Minecraft.getInstance();
        g.drawCenteredString(font, Component.translatable("handbuch.deadzonemc.rezepte_seite", seite + 1, seiten()), x + B / 2,
                y + 8, 0xFFE8DCB8);
        ItemStack tipp = ItemStack.EMPTY;
        int zeit = (int) (System.currentTimeMillis() / 1000);
        for (int i = 0; i < PRO_SEITE; i++) {
            int n = seite * PRO_SEITE + i;
            if (n >= rezepte.size()) {
                break;
            }
            Recipe<?> r = rezepte.get(n).value();
            int rx = x + 10 + (i % 2) * 145;
            int ry = y + 24 + (i / 2) * 58;
            g.fill(rx - 2, ry - 2, rx + 138, ry + 54, 0x40000000);
            NonNullList<Ingredient> zutaten = r.getIngredients();
            int breite = r instanceof ShapedRecipe s ? s.getWidth() : 3;
            for (int z = 0; z < Math.min(9, zutaten.size()); z++) {
                int zx = rx + (z % breite) * 17;
                int zy = ry + (z / breite) * 17;
                g.fill(zx, zy, zx + 16, zy + 16, 0x60FFFFFF);
                ItemStack[] moeglich = zutaten.get(z).getItems();
                if (moeglich.length > 0) {
                    ItemStack st = moeglich[zeit % moeglich.length];
                    g.renderItem(st, zx, zy);
                    if (mx >= zx && mx < zx + 16 && my >= zy && my < zy + 16) {
                        tipp = st;
                    }
                }
            }
            ItemStack ergebnis = r.getResultItem(mc.level.registryAccess());
            g.drawString(font, "→", rx + 56, ry + 20, 0xFFE8DCB8, false);
            g.renderItem(ergebnis, rx + 68, ry + 16);
            g.renderItemDecorations(font, ergebnis, rx + 68, ry + 16);
            if (mx >= rx + 68 && mx < rx + 84 && my >= ry + 16 && my < ry + 32) {
                tipp = ergebnis;
            }
            List<net.minecraft.util.FormattedCharSequence> name = font.split(ergebnis.getHoverName(), 50);
            for (int z = 0; z < Math.min(3, name.size()); z++) {
                g.drawString(font, name.get(z), rx + 88, ry + 8 + z * 10, 0xFFD8D0B8, false);
            }
            String art = rezepte.get(n).value().getType() == net.minecraft.world.item.crafting.RecipeType.CRAFTING ? ""
                    : "⚒";
            if (!art.isEmpty()) {
                g.drawString(font, art, rx + 128, ry + 42, 0xFFA0A0A0, false);
            }
        }
        if (!tipp.isEmpty()) {
            g.renderTooltip(font, tipp, mx, my);
        }
    }

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float delta) {
        super.renderBackground(g, mx, my, delta);
        int x = (width - B) / 2;
        int y = (height - H) / 2;
        g.fill(x, y, x + B, y + H, 0xF0201C16);
        g.fill(x + 2, y + 2, x + B - 2, y + H - 2, 0xF03A3228);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double dx, double dy) {
        seite = Math.max(0, Math.min(seiten() - 1, seite - (int) Math.signum(dy)));
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @SuppressWarnings("unused")
    private String suche() {
        return suche;
    }
}
