package de.deadzonemc.client;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

/** Jumpscare-Effekt: die Kamera reißt kurz hin und her, der Bildschirm blitzt dunkelrot auf. */
public final class ClientSchreck {

    private ClientSchreck() {
    }

    private static final int DAUER = 16;
    private static int rest = 0;
    private static int staerke = 1;
    /** false = nur Beben (Bossangriffe), true = Jumpscare mit Aufblitzen. */
    private static boolean blitz = true;
    private static final RandomSource ZUFALL = RandomSource.create();

    /** Positiv: Jumpscare mit Aufblitzen. Negativ: nur Bildschirmbeben in dieser Stärke. */
    public static void start(int s) {
        if (s < 0 && blitz && rest > 0) {
            return; // ein laufender Jumpscare hat Vorrang
        }
        rest = DAUER;
        blitz = s > 0;
        staerke = Math.max(1, Math.abs(s));
    }

    public static void tick(ClientTickEvent.Post event) {
        if (rest > 0) {
            rest--;
        }
    }

    private static float anteil(double teilTick) {
        return rest <= 0 ? 0 : (float) Mth.clamp((rest - teilTick) / DAUER, 0, 1);
    }

    public static void kamera(ViewportEvent.ComputeCameraAngles event) {
        if (!ClientEinstellungen.an(ClientEinstellungen.SCHRECK_EFFEKTE)) {
            return;
        }
        float a = anteil(event.getPartialTick());
        if (a <= 0) {
            return;
        }
        float k = a * a * 6.0F * staerke;
        event.setYaw(event.getYaw() + (ZUFALL.nextFloat() - 0.5F) * k);
        event.setPitch(event.getPitch() + (ZUFALL.nextFloat() - 0.5F) * k);
        event.setRoll(event.getRoll() + (ZUFALL.nextFloat() - 0.5F) * k * 1.5F);
    }

    public static void bild(RenderGuiEvent.Post event) {
        if (!ClientEinstellungen.an(ClientEinstellungen.SCHRECK_EFFEKTE)) {
            return;
        }
        float a = anteil(event.getPartialTick().getGameTimeDeltaPartialTick(true));
        if (a <= 0 || !blitz) {
            return;
        }
        var g = event.getGuiGraphics();
        int b = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int h = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        // erst ein grelles Aufblitzen, dann dunkelrotes Nachglühen
        int alpha = (int) (Math.min(1.0F, a * 1.3F) * (a > 0.8F ? 200 : 140));
        int farbe = a > 0.8F ? 0xE8E0D0 : 0x3A0000;
        g.fill(0, 0, b, h, (alpha << 24) | farbe);
    }
}
