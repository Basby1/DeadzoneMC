package de.deadzonemc.client;

import com.mojang.blaze3d.platform.InputConstants;
import de.deadzonemc.klasse.KlassePayload;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/** Die eigenen Klassen auf dem Client, die Taste für die Fähigkeit und die Klassenwahl. */
public final class ClientKlasse {

    private ClientKlasse() {
    }

    /** Gewählte Klassen (Reihenfolge = Wahl). */
    private static List<String> gewaehlt = new ArrayList<>();
    /** Wie viele davon gerade wirken (allein 3, zu zweit 2, sonst 1). */
    private static int plaetze = 1;
    /** Klassenwahl nach dem Intro öffnen. */
    public static boolean klassenwahlDanach;

    public static final KeyMapping FAEHIGKEIT = new KeyMapping("key.deadzonemc.faehigkeit",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, "key.categories.deadzonemc");
    public static final KeyMapping KLASSENWAHL = new KeyMapping("key.deadzonemc.klassenwahl",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_O, "key.categories.deadzonemc");

    /** Hauptklasse (erste gewählte) oder "". */
    public static String klasse() {
        return gewaehlt.isEmpty() ? "" : gewaehlt.get(0);
    }

    public static List<String> gewaehlt() {
        return gewaehlt;
    }

    public static int plaetze() {
        return plaetze;
    }

    /** Wirkt diese Klasse gerade? */
    public static boolean hat(String id) {
        int i = gewaehlt.indexOf(id);
        return i >= 0 && i < plaetze;
    }

    public static void empfangen(KlassePayload n) {
        String[] teile = n.klasse().split(";", 2);
        List<String> neu = new ArrayList<>();
        for (String id : teile[0].split(",")) {
            if (!id.isBlank()) {
                neu.add(id.trim());
            }
        }
        gewaehlt = neu;
        if (teile.length > 1) {
            try {
                plaetze = Integer.parseInt(teile[1].trim());
            } catch (NumberFormatException e) {
                plaetze = 1;
            }
        }
        if (n.oeffnen()) {
            // Läuft gerade das Intro, kommt die Klassenwahl direkt danach (statt es zu verdrängen)
            if (Minecraft.getInstance().screen instanceof IntroScreen) {
                klassenwahlDanach = true;
            } else {
                Minecraft.getInstance().setScreen(new KlassenScreen());
            }
        }
    }

    public static void tasten(RegisterKeyMappingsEvent event) {
        event.register(FAEHIGKEIT);
        event.register(KLASSENWAHL);
    }

    public static void tick(ClientTickEvent.Post event) {
        while (FAEHIGKEIT.consumeClick()) {
            PacketDistributor.sendToServer(new KlassePayload("!", false));
        }
        while (KLASSENWAHL.consumeClick()) {
            Minecraft.getInstance().setScreen(new KlassenScreen());
        }
    }
}
