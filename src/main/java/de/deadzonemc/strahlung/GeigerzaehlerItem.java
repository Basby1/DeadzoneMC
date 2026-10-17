package de.deadzonemc.strahlung;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** Geigerzähler: in der Hand knackt er je nach Strahlung und zeigt Rate und Dosis an. */
public class GeigerzaehlerItem extends Item {

    public GeigerzaehlerItem(Properties eigenschaften) {
        super(eigenschaften);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> zeilen, TooltipFlag flag) {
        zeilen.add(Component.translatable("item.deadzonemc.geigerzaehler.info").withStyle(ChatFormatting.GRAY));
    }
}
