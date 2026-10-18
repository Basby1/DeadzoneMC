package de.deadzonemc.beute;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;

/**
 * Seltener Fund! Beim Öffnen eines Behälters mit frisch erzeugter, besonderer Beute gibt es einen Klang,
 * Funken und eine Meldung mit dem Namen des besten Stücks. Jede Fundmeldung kommt nur einmal.
 */
public final class Fundmeldung {

    private Fundmeldung() {
    }

    public static void geoeffnet(PlayerContainerEvent.Open event) {
        if (!(event.getEntity() instanceof ServerPlayer spieler)) {
            return;
        }
        AbstractContainerMenu menue = event.getContainer();
        ItemStack bestes = ItemStack.EMPTY;
        int anzahl = 0;
        for (Slot slot : menue.slots) {
            if (slot.container == spieler.getInventory()) {
                continue;
            }
            ItemStack s = slot.getItem();
            if (s.has(Beute.FUND.get())) {
                s.remove(Beute.FUND.get());
                anzahl++;
                if (bestes.isEmpty() || s.getRarity().ordinal() > bestes.getRarity().ordinal()) {
                    bestes = s;
                }
            }
        }
        if (bestes.isEmpty()) {
            return;
        }
        boolean episch = bestes.getRarity() == Rarity.EPIC;
        spieler.level().playSound(null, spieler.blockPosition(), episch ? Beute.FUND_EPISCH.get() : Beute.FUND_SELTEN.get(),
                SoundSource.PLAYERS, 0.9F, 1.0F);
        spieler.serverLevel().sendParticles(spieler, episch ? ParticleTypes.END_ROD : ParticleTypes.HAPPY_VILLAGER, true,
                spieler.getX(), spieler.getEyeY() - 0.3, spieler.getZ(), episch ? 24 : 10, 0.6, 0.4, 0.6, 0.02);
        Component name = bestes.getHoverName().copy().withStyle(bestes.getRarity().color());
        Component text = Component.translatable(episch ? "fund.deadzonemc.episch" : "fund.deadzonemc.selten", name)
                .withStyle(episch ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.AQUA);
        if (anzahl > 1) {
            text = text.copy().append(Component.translatable("fund.deadzonemc.weitere", anzahl - 1).withStyle(ChatFormatting.GRAY));
        }
        spieler.displayClientMessage(text, true);
        menue.broadcastChanges();
    }
}
