package de.deadzonemc.waffen.feuer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** Munition eines Kalibers. Zeigt im Tooltip, welche Waffen sie verschießen. */
public class MunitionItem extends Item {

    public final Kaliber kaliber;

    public MunitionItem(Kaliber kaliber, Properties p) {
        super(p);
        this.kaliber = kaliber;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> zeilen, TooltipFlag flag) {
        zeilen.add(Component.translatable("waffe.deadzonemc.fuer").withStyle(ChatFormatting.GRAY));
        for (WaffenArt a : WaffenArt.values()) {
            if (a.kaliber == kaliber) {
                zeilen.add(Component.literal("  ").append(Component.translatable("item.deadzonemc." + a.id)).withStyle(ChatFormatting.DARK_GRAY));
            }
        }
    }
}
