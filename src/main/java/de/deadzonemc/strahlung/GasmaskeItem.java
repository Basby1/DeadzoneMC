package de.deadzonemc.strahlung;

import de.deadzonemc.ModRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** Gasmaske: schützt mit Filter vor Giftgas und hält den Großteil der Strahlung ab. */
public class GasmaskeItem extends ArmorItem {

    public GasmaskeItem(Holder<ArmorMaterial> material, Properties eigenschaften) {
        super(material, Type.HELMET, eigenschaften);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> zeilen, TooltipFlag flag) {
        int ticks = stack.getOrDefault(ModRegistry.FILTER.get(), 0);
        if (ticks > 0) {
            zeilen.add(Component.translatable("item.deadzonemc.gasmaske.filter", ticks / 20).withStyle(ChatFormatting.GREEN));
        } else {
            zeilen.add(Component.translatable("item.deadzonemc.gasmaske.kein_filter").withStyle(ChatFormatting.RED));
        }
        zeilen.add(Component.translatable("item.deadzonemc.gasmaske.info").withStyle(ChatFormatting.GRAY));
    }
}
