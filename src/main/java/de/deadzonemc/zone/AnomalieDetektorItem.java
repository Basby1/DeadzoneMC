package de.deadzonemc.zone;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** In der Hand: piept bei Anomalien in der Nähe und meldet Artefakt-Signale. */
public class AnomalieDetektorItem extends Item {

    public AnomalieDetektorItem(Properties eigenschaften) {
        super(eigenschaften.stacksTo(1));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> zeilen, TooltipFlag flag) {
        zeilen.add(Component.translatable("item.deadzonemc.anomaliedetektor.info").withStyle(ChatFormatting.GRAY));
    }
}
