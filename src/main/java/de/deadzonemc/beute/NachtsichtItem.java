package de.deadzonemc.beute;

import de.deadzonemc.ModRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.List;

/**
 * Nachtsichtgerät: auf dem Kopf getragen macht es die pechschwarzen Nächte erträglich – solange die Batterie
 * hält. Rechtsklick mit einer Batterie im Inventar lädt es auf (5 Minuten je Batterie).
 */
public class NachtsichtItem extends ArmorItem {

    public static final int JE_BATTERIE = 6000;
    public static final int MAXIMUM = JE_BATTERIE * 4;

    public NachtsichtItem(Holder<ArmorMaterial> material, Properties p) {
        super(material, Type.HELMET, p);
    }

    public static boolean aktiv(Player p) {
        ItemStack kopf = p.getItemBySlot(EquipmentSlot.HEAD);
        return kopf.getItem() instanceof NachtsichtItem && kopf.getOrDefault(Beute.LADUNG.get(), 0) > 0;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player spieler, InteractionHand hand) {
        ItemStack stack = spieler.getItemInHand(hand);
        int ladung = stack.getOrDefault(Beute.LADUNG.get(), 0);
        if (spieler.isShiftKeyDown() && ladung + JE_BATTERIE <= MAXIMUM) {
            for (int i = 0; i < spieler.getInventory().getContainerSize(); i++) {
                ItemStack b = spieler.getInventory().getItem(i);
                if (b.is(ModRegistry.BATTERIE.get())) {
                    if (!level.isClientSide) {
                        b.shrink(1);
                        stack.set(Beute.LADUNG.get(), ladung + JE_BATTERIE);
                        level.playSound(null, spieler.blockPosition(), SoundEvents.CROSSBOW_LOADING_END.value(), SoundSource.PLAYERS, 0.8F, 1.6F);
                    }
                    return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
                }
            }
            return InteractionResultHolder.fail(stack);
        }
        return super.use(level, spieler, hand);
    }

    public static void tick(PlayerTickEvent.Post event) {
        Player p = event.getEntity();
        if (p.level().isClientSide || p.tickCount % 20 != 0) {
            return;
        }
        ItemStack kopf = p.getItemBySlot(EquipmentSlot.HEAD);
        if (!(kopf.getItem() instanceof NachtsichtItem)) {
            return;
        }
        int ladung = kopf.getOrDefault(Beute.LADUNG.get(), 0);
        if (ladung <= 0) {
            return;
        }
        kopf.set(Beute.LADUNG.get(), Math.max(0, ladung - 20));
        p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 260, 0, true, false, false));
        if (ladung - 20 <= 0) {
            p.displayClientMessage(Component.translatable("item.deadzonemc.nachtsichtgeraet.leer").withStyle(ChatFormatting.RED), true);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> zeilen, TooltipFlag flag) {
        int ladung = stack.getOrDefault(Beute.LADUNG.get(), 0);
        zeilen.add(Component.translatable("item.deadzonemc.nachtsichtgeraet.ladung", ladung / 1200, (ladung / 20) % 60)
                .withStyle(ladung > 0 ? ChatFormatting.GREEN : ChatFormatting.RED));
        zeilen.add(Component.translatable("item.deadzonemc.nachtsichtgeraet.info").withStyle(ChatFormatting.GRAY));
    }
}
