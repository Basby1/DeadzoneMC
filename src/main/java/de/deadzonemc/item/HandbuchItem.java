package de.deadzonemc.item;

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
 * Das Handbuch für Überlebende: Grundlagen, Wetter, Gebiete, Orte, Beute und Gegenstände.
 * Monster werden eingetragen, sobald man ihnen begegnet ist (folgt mit den Monstern).
 */
public class HandbuchItem extends Item {

    public HandbuchItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.deadzonemc.handbuch.info").withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            NotizAnzeige.handbuch();
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
