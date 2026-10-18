package de.deadzonemc.verletzung;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import java.util.List;

/** Schiene: richtet ein gebrochenes Bein (drei Sekunden anlegen). */
public class SchieneItem extends Item {

    public SchieneItem(Properties eigenschaften) {
        super(eigenschaften);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity wer) {
        return 60;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player spieler, InteractionHand hand) {
        ItemStack stack = spieler.getItemInHand(hand);
        if (!spieler.hasEffect(Verletzungen.KNOCHENBRUCH)) {
            if (!level.isClientSide) {
                spieler.displayClientMessage(Component.translatable("item.deadzonemc.schiene.nicht_noetig")
                        .withStyle(ChatFormatting.GRAY), true);
            }
            return InteractionResultHolder.fail(stack);
        }
        spieler.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity wer) {
        if (!level.isClientSide) {
            wer.removeEffect(Verletzungen.KNOCHENBRUCH);
            level.playSound(null, wer.blockPosition(), SoundEvents.WOOD_PLACE, SoundSource.PLAYERS, 1.0F, 0.7F);
        }
        stack.consume(1, wer);
        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> zeilen, TooltipFlag flag) {
        zeilen.add(Component.translatable("item.deadzonemc.schiene.info").withStyle(ChatFormatting.GRAY));
    }
}
