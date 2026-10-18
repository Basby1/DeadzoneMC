package de.deadzonemc.musik;

import de.deadzonemc.welt.Staedte;
import de.deadzonemc.zuflucht.ZufluchtSchutz;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Sagt dem Client, ob der Spieler auf dem Land, in einer Stadt oder in einer Zuflucht ist (nur bei Wechsel). */
public final class OrtMelder {

    private static final Map<UUID, Integer> LETZTER = new HashMap<>();

    private OrtMelder() {
    }

    public static void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || p.tickCount % 40 != 17) {
            return;
        }
        int ort = 0;
        if (p.level().dimension() == Level.OVERWORLD) {
            ServerLevel w = p.serverLevel();
            if (ZufluchtSchutz.sicher(w, p.blockPosition())) {
                ort = 2;
            } else if (Staedte.istStadt(w, p.chunkPosition().x, p.chunkPosition().z)) {
                ort = 1;
            }
        }
        Integer alt = LETZTER.put(p.getUUID(), ort);
        if (alt == null || alt != ort) {
            PacketDistributor.sendToPlayer(p, new OrtPayload(ort));
        }
    }
}
