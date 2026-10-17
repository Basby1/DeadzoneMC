package de.deadzonemc.strahlung;

import de.deadzonemc.ModRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/** Ein Atemfilter: Rechtsklick, während man die Gasmaske trägt, schraubt ihn ein (5 Minuten Schutz). */
public class FilterItem extends Item {

    public static final int DAUER = 6000;
    public static final int HOECHSTENS = 12000;

    public FilterItem(Properties eigenschaften) {
        super(eigenschaften);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player spieler, InteractionHand hand) {
        ItemStack filter = spieler.getItemInHand(hand);
        ItemStack maske = spieler.getItemBySlot(EquipmentSlot.HEAD);
        if (!maske.is(ModRegistry.GASMASKE.get())) {
            spieler.displayClientMessage(Component.translatable("item.deadzonemc.filter.keine_maske").withStyle(ChatFormatting.RED), true);
            return InteractionResultHolder.fail(filter);
        }
        int alt = maske.getOrDefault(ModRegistry.FILTER.get(), 0);
        if (alt >= HOECHSTENS - DAUER / 4) {
            return InteractionResultHolder.fail(filter);
        }
        if (!level.isClientSide) {
            maske.set(ModRegistry.FILTER.get(), Math.min(HOECHSTENS, alt + DAUER));
            level.playSound(null, spieler.blockPosition(), SoundEvents.ARMOR_EQUIP_CHAIN.value(), SoundSource.PLAYERS, 1.0F, 1.3F);
        }
        filter.consume(1, spieler);
        return InteractionResultHolder.sidedSuccess(filter, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> zeilen, TooltipFlag flag) {
        zeilen.add(Component.translatable("item.deadzonemc.filter.info").withStyle(ChatFormatting.GRAY));
    }
}
