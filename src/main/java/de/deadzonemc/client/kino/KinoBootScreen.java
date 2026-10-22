package de.deadzonemc.client.kino;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.List;

import static de.deadzonemc.client.kino.ClientKinoBoot.*;

/** Bildebene des Boot-Intros: Kinobalken, Blenden, Blitze, violettes Licht, Unterwasser, Funk und die Namen. */
public class KinoBootScreen extends Screen {

    private int ueberspringenBis = -1;
    private int zeit;

    public KinoBootScreen() {
        super(Component.empty());
    }

    @Override
    public void tick() {
        zeit++;
    }

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float d) {
        // keine Abdunklung: man soll die Szene sehen
    }

    private int a(float wert) {
        return Mth.clamp((int) (wert * 255), 0, 255) << 24;
    }

    private static float blende(float s, float von, float bis, float ein, float aus) {
        if (s < von || s > bis) {
            return 0;
        }
        return Math.min(Mth.clamp((s - von) / ein, 0, 1), Mth.clamp((bis - s) / aus, 0, 1));
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float d) {
        if (ClientKinoBoot.bereitet()) {
            g.fill(0, 0, width, height, 0xFF000000);
            int al = (int) (120 + 100 * Mth.sin(zeit * 0.15F));
            g.drawCenteredString(font, Component.literal("[KILO-2]  Funkverbindung wird aufgebaut …"), width / 2, height / 2 - 4,
                    (al << 24) | 0xC8D8C0);
            return;
        }
        float s = ClientKinoBoot.zeit(d);
        int balken = height / 9;
        // Violettes Licht nach dem zweiten Blitz, Blitze, Unterwasser
        if (ClientKinoBoot.violett(s) && s < UNTER) {
            float v = Mth.clamp((s - BLITZ2 - 0.2F) / 2.0F, 0, 1) * (0.22F + 0.06F * Mth.sin(s * 3));
            g.fill(0, 0, width, height, a(v) | 0x4A1478);
        }
        if (s >= UNTER && s < SCHWARZ) {
            g.fill(0, 0, width, height, a(0.18F) | 0x0A2030);
        }
        float blitz = ClientKinoBoot.blitz(s);
        if (blitz > 0) {
            g.fill(0, 0, width, height, a(blitz * 0.8F) | 0xF4F0FF);
        }
        // Dunkelheit: Anfang, Schnitte, Ende
        float schwarz = 0;
        if (s < DECK) {
            schwarz = 1;
        } else if (s < DECK + 1.4F) {
            schwarz = 1 - (s - DECK) / 1.4F;
        }
        for (float schnitt : new float[]{AUSSEN, BUG, GESICHTER, BLITZ2, EINSCHLAG, WASSERLINIE, UEBERBORD}) {
            if (s >= schnitt - 0.08F && s < schnitt + 0.08F) {
                schwarz = Math.max(schwarz, schnitt >= EINSCHLAG ? 0.7F : 1);
            }
        }
        if (s >= UNTER - 0.15F && s < UNTER + 0.2F) {
            schwarz = Math.max(schwarz, 0.9F); // Eintauchen
        }
        if (s >= SCHWARZ - 1.2F) {
            schwarz = Math.max(schwarz, Mth.clamp((s - (SCHWARZ - 1.2F)) / 1.2F, 0, 1));
        }
        if (schwarz > 0) {
            g.fill(0, 0, width, height, a(schwarz));
        }
        // Kinobalken
        g.fill(0, 0, width, balken, 0xFF000000);
        g.fill(0, height - balken, width, height, 0xFF000000);

        int unten = height - balken - 22;
        int n = ClientKino.namen().size();
        funk(g, s, 0.6F, 3.9F, "KILO-2", "Kilo-2 an Kommando. Küste von Sektor 4 voraus. " + n + (n == 1 ? " Person" : " Personen")
                + " an Bord.", unten, 0xFFC8D8C0);
        funk(g, s, 5.0F, 8.8F, "KOMMANDO", "Verstanden. Ihr Auftrag: Finden Sie heraus, was dort passiert ist.", unten, 0xFFC8D8C0);
        funk(g, s, 9.3F, 12.6F, "KOMMANDO", "Melden Sie sich alle zehn Minuten. Ohne Meldung kommt niemand nach.", unten, 0xFFC8D8C0);
        funk(g, s, 16.8F, 19.8F, "KILO-2", "Nebel zieht auf. Sicht unter hundert Meter.", unten, 0xFFC8D8C0);
        funk(g, s, 24.4F, 26.4F, "KILO-2", "Da brennt Licht … da drüben lebt noch jemand.", unten, 0xFFC8D8C0);
        stoerung(g, s, 26.9F, 28.8F, "…kehrt um…", unten);
        funk(g, s, 30.5F, 31.9F, "KILO-2", "Was war das?!", unten, 0xFFE0B080);
        funk(g, s, 35.3F, 36.6F, "KILO-2", "Festhalten!", unten, 0xFFE0B080);
        stoerung(g, s, 44.4F, 47.2F, "…ihr hättet nicht kommen sollen…", height / 2 - 4);
        float t = blende(s, AUSSEN + 0.5F, BUG - 0.6F, 1.0F, 1.0F);
        if (t > 0) {
            int al = Math.max(4, (int) (t * 255)) << 24;
            g.drawCenteredString(font, Component.literal("SEKTOR 4  ·  TAG 41 NACH DER ABRIEGELUNG"), width / 2, balken + 14, al | 0xE8DCB8);
            g.drawCenteredString(font, Component.literal("Auftrag: Aufklärung"), width / 2, balken + 28, al | 0x9A958A);
        }
        // Namen wie im Abspann, während die Kamera an den Bänken vorbeifährt
        List<String> nm = ClientKino.namen();
        if (!nm.isEmpty()) {
            float fenster = (AUSSEN - DECK - 2.4F) / nm.size();
            for (int i = 0; i < nm.size(); i++) {
                float von = DECK + 1.6F + i * fenster;
                float v = blende(s, von, von + fenster - 0.2F, 0.5F, 0.5F);
                if (v > 0) {
                    int farbe = Math.max(4, (int) (v * 255)) << 24;
                    g.pose().pushPose();
                    g.pose().translate(width * 0.07F, height * 0.62F, 0);
                    g.pose().scale(2, 2, 1);
                    g.drawString(font, nm.get(i).toUpperCase(), 0, 0, farbe | 0xF0D080);
                    g.pose().popPose();
                    g.drawString(font, "An Bord, " + (i + 1) + " von " + nm.size(), (int) (width * 0.07F), (int) (height * 0.62F) + 20,
                            farbe | 0x9A958A);
                }
            }
        }
        float z = blende(s, SCHWARZ + 3.4F, ENDE - 0.3F, 1.0F, 1.0F);
        if (z > 0) {
            g.drawCenteredString(font, Component.literal("Von Kilo-2 kam nie wieder ein Funkspruch."), width / 2, height / 2 - 4,
                    (Math.max(4, (int) (z * 255)) << 24) | 0xC8C0A8);
        }
        if (ueberspringenBis > zeit) {
            g.drawString(font, "Nochmal Leertaste: überspringen", width - 170, height - balken / 2 - 4, 0x90FFFFFF, false);
        } else {
            g.drawString(font, "Leertaste: überspringen", width - 120, height - balken / 2 - 4, 0x40FFFFFF, false);
        }
    }

    private void funk(GuiGraphics g, float s, float von, float bis, String wer, String text, int y, int farbe) {
        float v = blende(s, von, bis, 0.15F, 0.3F);
        if (v <= 0) {
            return;
        }
        int al = Math.max(4, (int) (v * 255)) << 24;
        String zeile = "[" + wer + "]  " + text;
        int b = font.width(zeile);
        g.fill(width / 2 - b / 2 - 6, y - 4, width / 2 + b / 2 + 6, y + 12, (Math.max(4, (int) (v * 150)) << 24));
        g.drawCenteredString(font, Component.literal(zeile), width / 2, y, al | (farbe & 0xFFFFFF));
    }

    private void stoerung(GuiGraphics g, float s, float von, float bis, String text, int y) {
        float v = blende(s, von, bis, 0.3F, 0.6F);
        if (v <= 0) {
            return;
        }
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            b.append(((int) (s * 20) + i * 7) % 11 == 0 ? "▓░▒".charAt(i % 3) : text.charAt(i));
        }
        int dx = (int) (Mth.sin(s * 37) * 2);
        g.drawCenteredString(font, Component.literal(b.toString()), width / 2 + dx, y, (Math.max(4, (int) (v * 255)) << 24) | 0xB070FF);
    }

    @Override
    public boolean keyPressed(int k, int scan, int mod) {
        if (k == 32 || k == 256) {
            if (ueberspringenBis > zeit) {
                ClientKinoBoot.beenden(true);
            } else {
                ueberspringenBis = zeit + 50;
            }
        }
        return true;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
