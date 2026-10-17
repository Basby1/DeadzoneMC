package de.deadzonemc.client;

import de.deadzonemc.klasse.Klasse;
import de.deadzonemc.klasse.KlassePayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/** Klassenwahl: links die fünf Klassen, rechts Rolle, Vorteil, Fähigkeit und Schwäche der gewählten. */
public class KlassenScreen extends Screen {

    private Klasse auswahl;
    private final String aktuell;

    public KlassenScreen(String aktuell) {
        super(Component.translatable("klasse.deadzonemc.titel"));
        this.aktuell = aktuell;
        Klasse k = Klasse.ausId(aktuell);
        this.auswahl = k != null ? k : Klasse.BRECHER;
    }

    @Override
    protected void init() {
        int x = width / 2 - 190;
        int y = height / 2 - 70;
        for (Klasse k : Klasse.values()) {
            addRenderableWidget(Button.builder(Component.translatable("klasse.deadzonemc." + k.id()), b -> auswahl = k)
                    .bounds(x, y, 110, 20).build());
            y += 24;
        }
        addRenderableWidget(Button.builder(Component.translatable("klasse.deadzonemc.waehlen"), b -> {
            PacketDistributor.sendToServer(new KlassePayload(auswahl.id(), false));
            onClose();
        }).bounds(width / 2 + 40, height / 2 + 70, 120, 20).build());
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        super.render(g, mx, my, pt);
        g.drawCenteredString(font, title, width / 2, height / 2 - 100, 0xE8DCC0);
        int x = width / 2 - 60;
        int y = height / 2 - 70;
        String id = auswahl.id();
        g.drawString(font, Component.translatable("klasse.deadzonemc." + id).withStyle(s -> s.withBold(true)), x, y, 0xF0C060);
        y += 14;
        for (String teil : new String[]{"rolle", "vorteil", "faehigkeit", "schwaeche"}) {
            g.drawString(font, Component.translatable("klasse.deadzonemc.feld." + teil), x, y, 0x9AA39A);
            y += 10;
            List<FormattedCharSequence> zeilen = font.split(Component.translatable("klasse.deadzonemc." + id + "." + teil), 240);
            for (FormattedCharSequence z : zeilen) {
                g.drawString(font, z, x + 6, y, 0xE0E0E0);
                y += 10;
            }
            y += 4;
        }
        if (!aktuell.isEmpty()) {
            g.drawString(font, Component.translatable("klasse.deadzonemc.hinweis_basis"), width / 2 - 190, height / 2 + 76, 0x808080);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
