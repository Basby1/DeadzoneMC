package de.deadzonemc.client;

import com.mojang.blaze3d.platform.InputConstants;
import de.deadzonemc.klasse.KlassePayload;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/** Die eigene Klasse auf dem Client, die Taste für die Fähigkeit und die Klassenwahl. */
public final class ClientKlasse {

    private ClientKlasse() {
    }

    private static String klasse = "";

    public static final KeyMapping FAEHIGKEIT = new KeyMapping("key.deadzonemc.faehigkeit",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, "key.categories.deadzonemc");
    public static final KeyMapping KLASSENWAHL = new KeyMapping("key.deadzonemc.klassenwahl",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_K, "key.categories.deadzonemc");

    public static String klasse() {
        return klasse;
    }

    public static void empfangen(KlassePayload n) {
        klasse = n.klasse();
        if (n.oeffnen()) {
            Minecraft.getInstance().setScreen(new KlassenScreen(klasse));
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
            Minecraft.getInstance().setScreen(new KlassenScreen(klasse));
        }
    }
}
