package de.deadzonemc.waffen;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import java.util.List;

/** Waffenpflegeset: in der Haupthand, Schusswaffe in der Zweithand – zwei Sekunden reinigen, Verschleiß weg. */
public class PflegesetItem extends Item {

    public PflegesetItem(Properties eigenschaften) {
        super(eigenschaften);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity wer) {
        return 40;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BRUSH;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player p, InteractionHand hand) {
        ItemStack set = p.getItemInHand(hand);
        ItemStack waffe = p.getItemInHand(hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        if (!Waffenpflege.istSchusswaffe(waffe)) {
            if (!level.isClientSide) {
                p.displayClientMessage(Component.translatable("item.deadzonemc.pflegeset.hinweis").withStyle(ChatFormatting.GRAY), true);
            }
            return InteractionResultHolder.fail(set);
        }
        p.startUsingItem(hand);
        return InteractionResultHolder.consume(set);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack set, Level level, LivingEntity wer) {
        if (!level.isClientSide && wer instanceof Player p) {
            InteractionHand hand = p.getUsedItemHand();
            ItemStack waffe = p.getItemInHand(hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
            if (Waffenpflege.istSchusswaffe(waffe)) {
                waffe.remove(Waffenpflege.LADEHEMMUNG.get());
                waffe.remove(Waffenpflege.VERSCHLEISS.get());
                level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ARMOR_EQUIP_IRON.value(), SoundSource.PLAYERS, 1.0F, 1.4F);
                p.displayClientMessage(Component.translatable("item.deadzonemc.pflegeset.fertig").withStyle(ChatFormatting.GREEN), true);
                set.hurtAndBreak(1, p, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
            }
        }
        return set;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> zeilen, TooltipFlag flag) {
        zeilen.add(Component.translatable("item.deadzonemc.pflegeset.info").withStyle(ChatFormatting.GRAY));
    }
}
