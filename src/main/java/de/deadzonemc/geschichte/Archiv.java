package de.deadzonemc.geschichte;

import de.deadzonemc.ModRegistry;
import de.deadzonemc.netz.ArchivPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/** Sammelbares Archiv: Jede gelesene Notiz landet im Tagebuch (J) und lässt sich dort jederzeit wieder lesen. */
public final class Archiv {

    private Archiv() {
    }

    public static void registrieren() {
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer p) {
                senden(p);
            }
        });
    }

    public static void gelesen(ServerPlayer p, String id) {
        List<String> alt = p.getData(ModRegistry.ARCHIV.get());
        if (alt.contains(id)) {
            return;
        }
        List<String> neu = new ArrayList<>(alt);
        neu.add(id);
        p.setData(ModRegistry.ARCHIV.get(), List.copyOf(neu));
        senden(p);
    }

    private static void senden(ServerPlayer p) {
        PacketDistributor.sendToPlayer(p, new ArchivPayload(p.getData(ModRegistry.ARCHIV.get())));
    }
}
