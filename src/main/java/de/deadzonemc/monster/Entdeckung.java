package de.deadzonemc.monster;

import de.deadzonemc.ModRegistry;
import de.deadzonemc.netz.EntdecktPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/** Merkt sich, welchen Monstern ein Spieler begegnet ist, und schaltet ihre Seiten im Handbuch frei. */
public final class Entdeckung {

    private Entdeckung() {
    }

    public static void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || p.tickCount % 20 != 7 || p.isSpectator()) {
            return;
        }
        List<String> bekannt = p.getData(ModRegistry.ENTDECKT);
        for (DeadzoneMonster m : p.serverLevel().getEntitiesOfClass(DeadzoneMonster.class, p.getBoundingBox().inflate(14))) {
            String art = m.art;
            if (bekannt.contains(art) || m.isInvisible() || !p.hasLineOfSight(m)) {
                continue;
            }
            List<String> neu = new ArrayList<>(bekannt);
            neu.add(art);
            p.setData(ModRegistry.ENTDECKT, neu);
            PacketDistributor.sendToPlayer(p, new EntdecktPayload(neu));
            p.displayClientMessage(Component.translatable("handbuch.deadzonemc.neuer_eintrag",
                    Component.translatable("entity.deadzonemc." + art)).withStyle(ChatFormatting.GOLD), false);
            p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0F, 0.8F);
            return;
        }
    }

    public static void betreten(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            PacketDistributor.sendToPlayer(p, new EntdecktPayload(p.getData(ModRegistry.ENTDECKT)));
        }
    }

    public static void respawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            PacketDistributor.sendToPlayer(p, new EntdecktPayload(p.getData(ModRegistry.ENTDECKT)));
        }
    }
}
