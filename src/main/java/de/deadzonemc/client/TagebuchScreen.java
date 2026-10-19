package de.deadzonemc.client;

import de.deadzonemc.geschichte.Geschichte;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

/** Das Tagebuch (J): Akt, aktuelles Ziel mit Text, darunter alles bisher Erlebte. */
public class TagebuchScreen extends Screen {

    private static final int B = 300;
    private static final int H = 220;
    private final int schritt;
    private int versatz;
    private boolean archivAnsicht;
    private final java.util.List<int[]> zeilen = new java.util.ArrayList<>(); // y-Position und Index der Archiv-Einträge

    public TagebuchScreen(int schritt) {
        super(Component.translatable("tagebuch.deadzonemc.titel"));
        this.schritt = schritt;
    }

    @Override
    protected void init() {
        int x = (width - B) / 2;
        int y = (height - H) / 2;
        addRenderableWidget(net.minecraft.client.gui.components.Button.builder(archivTitel(), b -> {
            archivAnsicht = !archivAnsicht;
            versatz = 0;
            b.setMessage(archivTitel());
        }).bounds(x + B - 92, y + H - 22, 84, 16).build());
    }

    private Component archivTitel() {
        return archivAnsicht ? Component.translatable("tagebuch.deadzonemc.bisher_knopf")
                : Component.translatable("tagebuch.deadzonemc.archiv", ClientGeschichte.archiv.size(),
                de.deadzonemc.item.NotizItem.ALLE.length);
    }

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float d) {
        super.renderBackground(g, mx, my, d);
        int x = (width - B) / 2;
        int y = (height - H) / 2;
        g.fill(x, y, x + B, y + H, 0xF0201A12);
        g.fill(x + 2, y + 2, x + B - 2, y + H - 2, 0xF0392F22);
        g.fill(x + B / 2 - 1, y + 8, x + B / 2, y + H - 8, 0x40000000);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float d) {
        super.render(g, mx, my, d);
        int x = (width - B) / 2;
        int y = (height - H) / 2;
        List<Geschichte.Schritt> alle = Geschichte.SCHRITTE;
        int i = Math.min(schritt, alle.size() - 1);
        Geschichte.Schritt s = alle.get(i);
        // links: aktuelles Ziel
        g.drawString(font, Component.translatable("geschichte.deadzonemc.akt." + s.akt()), x + 10, y + 10, 0xFFB03020, false);
        g.drawString(font, Component.translatable("geschichte.deadzonemc.akt." + s.akt() + ".name"), x + 10, y + 21, 0xFFC8B890, false);
        g.drawString(font, Component.translatable("geschichte.deadzonemc." + s.id() + ".titel"), x + 10, y + 40, 0xFFF0D080, false);
        int zy = y + 54;
        for (FormattedCharSequence z : font.split(Component.translatable("geschichte.deadzonemc." + s.id() + ".text"), B / 2 - 22)) {
            g.drawString(font, z, x + 10, zy, 0xFFE0D8C8, false);
            zy += 10;
        }
        if (ClientGeschichte.zielWelt >= 0) {
            g.drawString(font, Component.translatable("tagebuch.deadzonemc.marker"), x + 10, y + H - 20, 0xFF8A8270, false);
        }
        if (archivAnsicht) {
            archivZeichnen(g, x, y, mx, my);
            return;
        }
        // rechts: bisher
        g.drawString(font, Component.translatable("tagebuch.deadzonemc.bisher"), x + B / 2 + 8, y + 10, 0xFFC8B890, false);
        int ry = y + 26 - versatz * 10;
        for (int k = i - 1; k >= 0; k--) {
            Geschichte.Schritt a = alle.get(k);
            for (FormattedCharSequence z : font.split(Component.literal("✔ ").append(
                    Component.translatable("geschichte.deadzonemc." + a.id() + ".titel")), B / 2 - 18)) {
                if (ry >= y + 24 && ry < y + H - 12) {
                    g.drawString(font, z, x + B / 2 + 8, ry, 0xFFA8A090, false);
                }
                ry += 10;
            }
        }
        if (i == 0) {
            g.drawString(font, Component.translatable("tagebuch.deadzonemc.leer"), x + B / 2 + 8, y + 26, 0xFF7A7262, false);
        }
    }

    /** Rechte Seite im Archiv: alle gelesenen Notizen, anklickbar zum erneuten Lesen; ungefundene als „???“. */
    private void archivZeichnen(GuiGraphics g, int x, int y, int mx, int my) {
        g.drawString(font, Component.translatable("tagebuch.deadzonemc.archiv_titel"), x + B / 2 + 8, y + 10, 0xFFC8B890, false);
        zeilen.clear();
        int ry = y + 26 - versatz * 10;
        String[] alle = de.deadzonemc.item.NotizItem.ALLE;
        for (int k = 0; k < alle.length; k++) {
            boolean gefunden = ClientGeschichte.archiv.contains(alle[k]);
            if (ry >= y + 24 && ry < y + H - 28) {
                Component t = gefunden ? Component.translatable("notiz.deadzonemc." + alle[k] + ".titel")
                        : Component.literal("???");
                boolean ueber = gefunden && mx >= x + B / 2 + 8 && mx < x + B - 8 && my >= ry && my < ry + 10;
                String text = font.plainSubstrByWidth(t.getString(), B / 2 - 18);
                g.drawString(font, text, x + B / 2 + 8, ry, !gefunden ? 0xFF5A5446 : ueber ? 0xFFF0D080 : 0xFFA8A090, false);
                if (gefunden) {
                    zeilen.add(new int[]{ry, k});
                }
            }
            ry += 10;
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int knopf) {
        if (archivAnsicht) {
            int x = (width - B) / 2;
            for (int[] z : zeilen) {
                if (mx >= x + B / 2 + 8 && mx < x + B - 8 && my >= z[0] && my < z[0] + 10) {
                    NotizAnzeige.oeffnen(de.deadzonemc.item.NotizItem.ALLE[z[1]]);
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, knopf);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double dx, double dy) {
        versatz = Math.max(0, versatz - (int) Math.signum(dy));
        return true;
    }

    @Override
    public boolean keyPressed(int k, int s, int m) {
        if (ClientGeschichte.TAGEBUCH.matches(k, s)) {
            onClose();
            return true;
        }
        return super.keyPressed(k, s, m);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
