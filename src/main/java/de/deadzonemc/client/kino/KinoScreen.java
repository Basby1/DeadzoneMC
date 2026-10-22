package de.deadzonemc.client.kino;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.List;

import static de.deadzonemc.client.kino.ClientKino.*;

/** Bildebene des Intros: Kinobalken, Blenden, Licht (rot, violett, Blitz), Funk-Untertitel und die Namen. */
public class KinoScreen extends Screen {

    private int ueberspringenBis = -1;
    private int zeit;

    public KinoScreen() {
        super(Component.empty());
    }

    @Override
    public void tick() {
        zeit++;
    }

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float d) {
        // keine Abdunklung: man soll die Welt sehen
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
        float s = ClientKino.zeit(d);
        int balken = height / 9;
        // Licht und Farbe
        boolean innen = s < AUSSEN || (s >= INNEN2 && s < SCHWEBEN);
        if (innen && s >= INNEN1) {
            float flackern = s >= INNEN2 + 2 ? (((int) (s * 7.3F) % 5 == 0) ? 0.55F : 0.0F) : 0;
            float alarm = s >= INNEN2 + 4 ? 0.12F + 0.12F * Mth.sin(s * 9) : 0;
            g.fill(0, 0, width, height, a(0.15F + alarm) | 0x5A0606);
            if (flackern > 0) {
                g.fill(0, 0, width, height, a(flackern) | 0x000000);
            }
        }
        if (s >= BLITZ2 + 0.25F && s < STILLE) {
            float v = Mth.clamp((s - BLITZ2 - 0.25F) / 2.5F, 0, 1) * (0.32F + 0.08F * Mth.sin(s * 3));
            g.fill(0, 0, width, height, a(v) | 0x4A1478);
        }
        float blitz = ClientKino.blitz(s);
        if (blitz > 0) {
            g.fill(0, 0, width, height, a(blitz * 0.85F) | 0xF4F0FF);
        }
        // Dunkelheit: Anfang, Schnitte, Stille, Sturz, nach dem Aufprall
        float schwarz = 0;
        if (s < INNEN1) {
            schwarz = 1;
        } else if (s < INNEN1 + 1.2F) {
            schwarz = 1 - (s - INNEN1) / 1.2F;
        }
        for (float schnitt : new float[]{AUSSEN, INNEN2, TUER, SCHWEBEN, STILLE}) {
            if (s >= schnitt - 0.12F && s < schnitt + 0.12F) {
                schwarz = Math.max(schwarz, 1);
            }
        }
        if (s >= STILLE && s < STURZ) {
            // Das violette Licht erlischt: kurz dunkel, dann treibt man still draußen
            float ab = Mth.clamp((s - STILLE) / 0.6F, 0, 1);
            schwarz = Math.max(schwarz, 0.75F - 0.4F * ab);
        }
        if (s >= STURZ) {
            float a = Mth.clamp((s - (AUFPRALL - 0.35F)) / 0.35F, 0, 1);
            schwarz = Math.max(schwarz, 0.3F + 0.7F * a);
        }
        if (s >= AUFPRALL) {
            schwarz = 1;
        }
        if (schwarz > 0) {
            g.fill(0, 0, width, height, a(schwarz));
        }
        // Kinobalken
        g.fill(0, 0, width, balken, 0xFF000000);
        g.fill(0, height - balken, width, height, 0xFF000000);

        // Texte
        int unten = height - balken - 22;
        int n = namen().size();
        funk(g, s, 0.6F, 3.8F, "Q-7", "Q-7 an Leitstelle. " + (n == 1 ? "Ein Passagier" : n + " Passagiere")
                + " an Bord. Kurs Sperrgrenze Nord.", unten, 0xFFC8D8C0);
        funk(g, s, 8.8F, 11.8F, "LEITSTELLE", "Verstanden, Q-7. Halten Sie Abstand zur Nebelwand.", unten, 0xFFC8D8C0);
        funk(g, s, 20.6F, 22.4F, "LEITSTELLE", "Q-7, wir verlieren Sie … wiederholen Sie …", unten, 0xFFC8D8C0);
        stoerung(g, s, 23.0F, 25.6F, "…ihr kommt zu spät…", unten);
        funk(g, s, 25.8F, 27.2F, "PILOT", "Was zum – Instrumente fallen aus!", unten, 0xFFE0B080);
        float t = blende(s, AUSSEN + 0.6F, INNEN2 - 0.4F, 1.0F, 1.0F);
        if (t > 0) {
            g.drawCenteredString(font, Component.literal("SEKTOR 4  ·  TAG 41 NACH DER ABRIEGELUNG"), width / 2, balken + 14,
                    (Math.max(4, (int) (t * 255)) << 24) | 0xE8DCB8);
        }
        // Namen wie im Abspann, während die Kamera an den Plätzen vorbeifährt
        List<String> nm = namen();
        if (!nm.isEmpty()) {
            float fenster = (AUSSEN - INNEN1 - 2.0F) / nm.size();
            for (int i = 0; i < nm.size(); i++) {
                float von = INNEN1 + 1.2F + i * fenster;
                float v = blende(s, von, von + fenster - 0.2F, 0.5F, 0.5F);
                if (v > 0) {
                    int farbe = Math.max(4, (int) (v * 255)) << 24;
                    g.pose().pushPose();
                    g.pose().translate(width * 0.72F, height * 0.62F, 0);
                    g.pose().scale(2, 2, 1);
                    g.drawString(font, nm.get(i).toUpperCase(), 0, 0, farbe | 0xF0D080);
                    g.pose().popPose();
                    g.drawString(font, "Passagier " + (i + 1) + " von " + nm.size(), (int) (width * 0.72F), (int) (height * 0.62F) + 20,
                            farbe | 0x9A958A);
                }
            }
        }
        float z = blende(s, AUFPRALL + 1.5F, ENDE - 0.4F, 1.0F, 1.0F);
        if (z > 0) {
            g.drawCenteredString(font, Component.literal("Der letzte Evakuierungsflug ist nie angekommen."), width / 2, height / 2 - 4,
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
        int a = Math.max(4, (int) (v * 255)) << 24;
        String zeile = "[" + wer + "]  " + text;
        int b = font.width(zeile);
        g.fill(width / 2 - b / 2 - 6, y - 4, width / 2 + b / 2 + 6, y + 12, (Math.max(4, (int) (v * 150)) << 24));
        g.drawCenteredString(font, Component.literal(zeile), width / 2, y, a | (farbe & 0xFFFFFF));
    }

    /** Die fremde Stimme: violett, zitternd, mit kaputten Zeichen. */
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
                ClientKino.beenden(true);
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
