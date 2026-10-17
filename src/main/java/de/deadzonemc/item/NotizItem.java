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
            "halden_flyer", "kinderzettel", "funkspruch", "brief", "pfarrer", "feuerwehr_einsatz"
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
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
