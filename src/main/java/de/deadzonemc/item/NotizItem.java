package de.deadzonemc.item;

import de.deadzonemc.ModRegistry;
import de.deadzonemc.client.NotizAnzeige;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Ein Fetzen Papier mit einem Stück der Geschichte. Welche Notiz es ist, steht in einer Daten-Komponente;
 * der Text selbst liegt in den Sprachdateien (notiz.deadzonemc.&lt;id&gt;.titel / .1 / .2 ...).
 */
public class NotizItem extends Item {

    /** Alle Notizen, die es bisher gibt (Reihenfolge wie im Kreativ-Tab). */
    public static final String[] ALLE = {
            "start", "evakuierung", "einkaufszettel", "abschied", "tagebuch_1", "tagebuch_2",
            "halden_flyer", "kinderzettel", "funkspruch", "brief", "pfarrer", "feuerwehr_einsatz",
            "station_null_1", "station_null_2", "station_null_3", "station_null_4",
            "funkprotokoll_1", "funkprotokoll_2", "funkprotokoll_3", "funkprotokoll_4", "vahl_logbuch", "orbit_log_1", "orbit_log_2",
            "spur_1", "spur_2", "spur_3", "spur_4", "spur_5", "spur_6"
    };

    public NotizItem(Properties properties) {
        super(properties);
    }

    public static ItemStack erstellen(String id) {
        ItemStack stack = new ItemStack(ModRegistry.NOTIZ_ITEM.get());
        stack.set(ModRegistry.NOTIZ.get(), id);
        return stack;
    }

    public static String id(ItemStack stack) {
        String id = stack.get(ModRegistry.NOTIZ.get());
        return id == null ? "start" : id;
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("notiz.deadzonemc." + id(stack) + ".titel");
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.deadzonemc.notiz.lesen").withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            NotizAnzeige.oeffnen(id(stack));
        } else if (player instanceof net.minecraft.server.level.ServerPlayer sp) {
            de.deadzonemc.geschichte.Archiv.gelesen(sp, id(stack));
            // Nebenquest: Diese Notizen beschreiben ein Versteck – die Skizze wird zur Karte
            if (id(stack).startsWith("spur_") && level instanceof net.minecraft.server.level.ServerLevel sl) {
                ItemStack karte = SchatzkarteItem.karte(sl, sp);
                if (karte != null) {
                    karte.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, Component.translatable("item.deadzonemc.spur.karte",
                            Component.translatable("notiz.deadzonemc." + id(stack) + ".titel")).withStyle(st -> st.withItalic(false)));
                    sp.displayClientMessage(Component.translatable("item.deadzonemc.spur.gefunden").withStyle(ChatFormatting.GOLD), false);
                    stack.consume(1, sp);
                    if (!sp.getInventory().add(karte)) {
                        sp.drop(karte, false);
                    }
                }
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
