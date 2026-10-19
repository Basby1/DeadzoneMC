package de.deadzonemc.bossmod;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** Trophäe eines Bosses – an der Zonenwerkbank als Boss-Modifikation in Waffen und Rüstung einzubauen. */
public class TrophaeeItem extends Item {

    public final String boss;

    public TrophaeeItem(String boss, Properties p) {
        super(p);
        this.boss = boss;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> zeilen, TooltipFlag flag) {
        zeilen.add(Component.translatable("bossmod.deadzonemc.in_waffe").withStyle(ChatFormatting.GRAY));
        zeilen.add(Component.literal(" ").append(Component.translatable("bossmod.deadzonemc." + boss + ".waffe")).withStyle(ChatFormatting.DARK_PURPLE));
        zeilen.add(Component.translatable("bossmod.deadzonemc.in_ruestung").withStyle(ChatFormatting.GRAY));
        zeilen.add(Component.literal(" ").append(Component.translatable("bossmod.deadzonemc." + boss + ".ruestung")).withStyle(ChatFormatting.DARK_PURPLE));
        zeilen.add(Component.translatable("bossmod.deadzonemc.hinweis").withStyle(ChatFormatting.DARK_GRAY));
    }
}
