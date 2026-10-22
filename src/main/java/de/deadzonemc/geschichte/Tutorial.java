package de.deadzonemc.geschichte;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Erste Schritte: Ein kleines Feld am linken Bildschirmrand holt neue Spieler ab und hakt sich selbst ab
 * (Wrack durchsuchen, Tagebuch, Klasse, Handbuch, Wegweiser folgen, Zuflucht erreichen). Der Client erkennt die
 * Schritte, der Server merkt sie sich pro Spieler.
 */
public final class Tutorial {

    public static final String SCHLUESSEL = "deadzonemc_tutorial";

    private Tutorial() {
    }

    public static void anmelden(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            PacketDistributor.sendToPlayer(p, new TutorialPayload(p.getPersistentData().getInt(SCHLUESSEL)));
        }
    }

    public static void empfangen(TutorialPayload n, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer p)) {
                return;
            }
            int alt = p.getPersistentData().getInt(SCHLUESSEL);
            int neu = alt | n.erledigt();
            if (neu != alt) {
                p.getPersistentData().putInt(SCHLUESSEL, neu);
                p.playNotifySound(SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.4F, 1.4F);
            }
        });
    }
}
