package de.deadzonemc.charakter;

import de.deadzonemc.ModRegistry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Speichert das Aussehen aus dem Charakter-Editor und verteilt es an alle Mitspieler. */
public final class Charaktere {

    private Charaktere() {
    }

    public static String code(ServerPlayer p) {
        return p.getData(ModRegistry.CHARAKTER.get());
    }

    public static boolean erstellt(ServerPlayer p) {
        return !code(p).isEmpty();
    }

    /** Gültig: "eigen" oder Ziffern mit Punkten (siehe CharakterTeile auf dem Client). */
    static boolean gueltig(String c) {
        return c.equals("eigen") || (c.length() <= 120 && c.matches("[0-9]+(\\.[0-9]+)*"));
    }

    public static void betreten(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) {
            return;
        }
        allenSenden(p.server);
        if (!erstellt(p)) {
            PacketDistributor.sendToPlayer(p, new CharakterPayload(CharakterPayload.EDITOR, ""));
        }
    }

    public static void abmelden(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            de.deadzonemc.geschichte.Kino.abmelden(p);
        }
    }

    public static void empfangen(CharakterPayload n, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer p)) {
                return;
            }
            if (n.art() == CharakterPayload.ALLE && gueltig(n.daten())) {
                p.setData(ModRegistry.CHARAKTER.get(), n.daten());
                allenSenden(p.server);
                de.deadzonemc.geschichte.Kino.bereit(p);
            } else if (n.art() == CharakterPayload.KINO) {
                de.deadzonemc.geschichte.Kino.fertig(p);
            }
        });
    }

    static void allenSenden(MinecraftServer server) {
        StringBuilder b = new StringBuilder();
        for (ServerPlayer x : server.getPlayerList().getPlayers()) {
            if (erstellt(x)) {
                if (!b.isEmpty()) {
                    b.append(';');
                }
                b.append(x.getUUID()).append('=').append(code(x));
            }
        }
        PacketDistributor.sendToAllPlayers(new CharakterPayload(CharakterPayload.ALLE, b.toString()));
    }
}
