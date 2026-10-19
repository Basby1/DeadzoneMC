package de.deadzonemc.client;

import de.deadzonemc.ausruestung.Ausruestung;
import de.deadzonemc.klasse.Klasse;
import de.deadzonemc.klasse.Klassenvorteile;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/** Tooltips: Klassenvorteil einer Waffe/eines Satzes (grün, wenn es deine Klasse ist) und Boss-Modifikation. */
public final class ClientTooltips {

    private ClientTooltips() {
    }

    public static void ergaenzen(ItemTooltipEvent e) {
        ItemStack s = e.getItemStack();
        Klasse liebling = Klassenvorteile.liebling(s.getItem());
        String art = "waffe";
        if (liebling == null && s.getItem() instanceof ArmorItem ai) {
            for (Ausruestung.Satz satz : Ausruestung.Satz.values()) {
                if (ai.getMaterial().is(Ausruestung.MATERIAL.get(satz))) {
                    liebling = Klassenvorteile.liebling(satz);
                    art = "satz." + satz.id;
                }
            }
        }
        if (liebling != null) {
            boolean meine = ClientKlasse.klasse().equals(liebling.id());
            String schluessel = art.equals("waffe") ? "klassenvorteil.deadzonemc.waffe." + liebling.id() : "klassenvorteil.deadzonemc." + art;
            e.getToolTip().add(Component.translatable("klassenvorteil.deadzonemc.kopf",
                            Component.translatable("klasse.deadzonemc." + liebling.id()))
                    .withStyle(meine ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY));
            e.getToolTip().add(Component.literal(" ").append(Component.translatable(schluessel))
                    .withStyle(meine ? ChatFormatting.DARK_GREEN : ChatFormatting.DARK_GRAY));
        }
        de.deadzonemc.bossmod.Bossmods.tooltip(s, e.getToolTip());
    }
}
