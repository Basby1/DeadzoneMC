package de.deadzonemc.gadget;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** Ein Gegenstand, der nur durch Dabeihaben wirkt; der Tooltip erklärt wie. */
public class HinweisItem extends Item {

    private final String id;

    public HinweisItem(String id, Properties p) {
        super(p);
        this.id = id;
    }

    @Override
    public void appendHoverText(ItemStack s, TooltipContext ctx, List<Component> zeilen, TooltipFlag flag) {
        zeilen.add(Component.translatable("item.deadzonemc." + id + ".info").withStyle(ChatFormatting.GRAY));
    }
}
