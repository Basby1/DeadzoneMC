package de.deadzonemc.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.settings.KeyModifier;
import org.slf4j.Logger;

import java.util.HashSet;
import java.util.Set;

/**
 * Tastenkonflikte mit anderen Mods lösen: Iris lädt z. B. auf R die Shader neu (massiver Ruckler beim Nachladen!),
 * schaltet auf K die Shader um und öffnet auf O sein Menü. Belegt eine andere Mod eine unserer Tasten, wird ihre
 * Belegung gelöst – Vanilla-Tasten bleiben unangetastet. Einmal pro Spielstart.
 */
public final class ClientTasten {

    private ClientTasten() {
    }

    private static final Logger LOG = com.mojang.logging.LogUtils.getLogger();
    private static final Set<String> VANILLA = Set.of("key.categories.movement", "key.categories.gameplay",
            "key.categories.inventory", "key.categories.creative", "key.categories.multiplayer", "key.categories.ui",
            "key.categories.misc");
    private static final String EIGENE = "key.categories.deadzonemc";
    private static boolean geprueft;

    public static void tick(ClientTickEvent.Post e) {
        if (geprueft) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.options == null) {
            return;
        }
        geprueft = true;
        Set<String> belegt = new HashSet<>();
        for (KeyMapping k : mc.options.keyMappings) {
            if (EIGENE.equals(k.getCategory()) && !k.isUnbound() && k.getKeyModifier() == KeyModifier.NONE) {
                belegt.add(k.saveString());
            }
        }
        // zusätzlich die Shader-Tasten von Iris immer lösen, wenn sie auf R, K oder O liegen (K = Fertigkeiten)
        belegt.add("key.keyboard.k");
        boolean geaendert = false;
        for (KeyMapping k : mc.options.keyMappings) {
            if (EIGENE.equals(k.getCategory()) || VANILLA.contains(k.getCategory()) || k.isUnbound()) {
                continue;
            }
            boolean iris = k.getName().startsWith("iris.") || k.getCategory().startsWith("iris");
            if (k.getKeyModifier() == KeyModifier.NONE && belegt.contains(k.saveString())
                    && (iris || !k.saveString().equals("key.keyboard.k"))) {
                LOG.info("Tastenkonflikt gelöst: {} lag auf {}", k.getName(), k.saveString());
                k.setKey(InputConstants.UNKNOWN);
                geaendert = true;
            }
        }
        if (geaendert) {
            KeyMapping.resetMapping();
            mc.options.save();
        }
    }
}
