package de.deadzonemc.client;

import de.deadzonemc.klasse.Klasse;
import de.deadzonemc.klasse.KlassePayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/**
 * Klassenwahl: links die fünf Klassen zum An- und Abwählen (allein bis zu 3, zu zweit 2, sonst 1), rechts Rolle,
 * Vorteil, Fähigkeit und Schwäche der zuletzt angeklickten; unten die Kombinationen der Auswahl.
 */
public class KlassenScreen extends Screen {

    private Klasse ansicht;
    private final List<Klasse> auswahl = new ArrayList<>();
    private final List<Button> knoepfe = new ArrayList<>();
    private Button bestaetigen;

    public KlassenScreen() {
        super(Component.translatable("klasse.deadzonemc.titel"));
        for (String id : ClientKlasse.gewaehlt()) {
            Klasse k = Klasse.ausId(id);
            if (k != null && auswahl.size() < ClientKlasse.plaetze()) {
                auswahl.add(k);
            }
        }
        ansicht = auswahl.isEmpty() ? Klasse.BRECHER : auswahl.get(0);
    }

    @Override
    protected void init() {
        knoepfe.clear();
        int x = width / 2 - 200;
        int y = height / 2 - 80;
        for (Klasse k : Klasse.values()) {
            Button b = Button.builder(beschriftung(k), knopf -> umschalten(k)).bounds(x, y, 120, 20).build();
            knoepfe.add(b);
            addRenderableWidget(b);
            y += 24;
        }
        bestaetigen = addRenderableWidget(Button.builder(Component.translatable("klasse.deadzonemc.waehlen"), b -> {
            PacketDistributor.sendToServer(new KlassePayload(String.join(",", auswahl.stream().map(Klasse::id).toList()), false));
            onClose();
        }).bounds(x, y + 6, 120, 20).build());
        bestaetigen.active = !auswahl.isEmpty();
    }

    private Component beschriftung(Klasse k) {
        int i = auswahl.indexOf(k);
        Component name = Component.translatable("klasse.deadzonemc." + k.id());
        return i >= 0 ? Component.literal("✔ ").append(name).withStyle(s -> s.withColor(0x8AE08A)) : name;
    }

    private void umschalten(Klasse k) {
        ansicht = k;
        if (auswahl.contains(k)) {
            auswahl.remove(k);
        } else {
            if (auswahl.size() >= ClientKlasse.plaetze()) {
                auswahl.remove(ClientKlasse.plaetze() == 1 ? 0 : auswahl.size() - 1);
            }
            auswahl.add(k);
        }
        Klasse[] alle = Klasse.values();
        for (int i = 0; i < alle.length; i++) {
            knoepfe.get(i).setMessage(beschriftung(alle[i]));
        }
        bestaetigen.active = !auswahl.isEmpty();
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        super.render(g, mx, my, pt);
        g.drawCenteredString(font, title, width / 2, height / 2 - 118, 0xE8DCC0);
        g.drawCenteredString(font, Component.translatable("klasse.deadzonemc.plaetze", ClientKlasse.plaetze(), auswahl.size()),
                width / 2, height / 2 - 104, 0xB0B8B0);
        int x = width / 2 - 66;
        int y = height / 2 - 80;
        String id = ansicht.id();
        g.drawString(font, Component.translatable("klasse.deadzonemc." + id).withStyle(s -> s.withBold(true)), x, y, 0xF0C060);
        y += 14;
        for (String teil : new String[]{"rolle", "vorteil", "faehigkeit", "schwaeche"}) {
            g.drawString(font, Component.translatable("klasse.deadzonemc.feld." + teil), x, y, 0x9AA39A);
            y += 10;
            for (FormattedCharSequence z : font.split(Component.translatable("klasse.deadzonemc." + id + "." + teil), 260)) {
                g.drawString(font, z, x + 6, y, 0xE0E0E0);
                y += 10;
            }
            y += 3;
        }
        // Kombinationen der aktuellen Auswahl
        if (auswahl.size() > 1) {
            y += 4;
            g.drawString(font, Component.translatable("klasse.deadzonemc.feld.kombi"), x, y, 0x9AA39A);
            y += 10;
            for (int i = 0; i < auswahl.size(); i++) {
                for (int j = i + 1; j < auswahl.size(); j++) {
                    String schluessel = paar(auswahl.get(i), auswahl.get(j));
                    g.drawString(font, Component.translatable("kombi.deadzonemc." + schluessel).withStyle(s -> s.withBold(true)),
                            x + 6, y, 0x8AE08A);
                    y += 10;
                    for (FormattedCharSequence z : font.split(Component.translatable("kombi.deadzonemc." + schluessel + ".text"), 250)) {
                        g.drawString(font, z, x + 12, y, 0xD0D0D0);
                        y += 10;
                    }
                }
            }
            for (FormattedCharSequence z : font.split(Component.translatable("klasse.deadzonemc.mehrfach_hinweis"), 260)) {
                g.drawString(font, z, x, y + 2, 0x808080);
                y += 10;
            }
        }
        g.drawString(font, Component.translatable("klasse.deadzonemc.hinweis_basis"), width / 2 - 200, height / 2 + 96, 0x808080);
    }

    /** Schlüssel einer Paarung in fester Reihenfolge (z. B. "brecher_schuetze"). */
    private static String paar(Klasse a, Klasse b) {
        return a.ordinal() < b.ordinal() ? a.id() + "_" + b.id() : b.id() + "_" + a.id();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
