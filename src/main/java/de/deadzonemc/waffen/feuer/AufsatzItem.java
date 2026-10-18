package de.deadzonemc.waffen.feuer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/**
 * Waffenaufsatz. Anbauen: Waffe in die andere Hand nehmen, Aufsatz benutzen (Rechtsklick). Ein vorhandener
 * Aufsatz derselben Art kommt zurück ins Inventar.
 */
public class AufsatzItem extends Item {

    public final String id;

    public AufsatzItem(String id, Properties p) {
        super(p);
        this.id = id;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player spieler, InteractionHand hand) {
        ItemStack aufsatz = spieler.getItemInHand(hand);
        ItemStack waffe = spieler.getItemInHand(hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        if (!(waffe.getItem() instanceof SchusswaffeItem w)) {
            spieler.displayClientMessage(Component.translatable("aufsatz.deadzonemc.anleitung").withStyle(ChatFormatting.GRAY), true);
            return InteractionResultHolder.fail(aufsatz);
        }
        if (!w.art.aufsaetze.contains(id)) {
            spieler.displayClientMessage(Component.translatable("aufsatz.deadzonemc.passt_nicht").withStyle(ChatFormatting.RED), true);
            return InteractionResultHolder.fail(aufsatz);
        }
        if (Feuerwaffen.hatAufsatz(waffe, id)) {
            spieler.displayClientMessage(Component.translatable("aufsatz.deadzonemc.schon").withStyle(ChatFormatting.GRAY), true);
            return InteractionResultHolder.fail(aufsatz);
        }
        if (!level.isClientSide) {
            List<String> neu = new ArrayList<>(waffe.getOrDefault(Feuerwaffen.AUFSAETZE.get(), List.of()));
            neu.add(id);
            waffe.set(Feuerwaffen.AUFSAETZE.get(), neu);
            aufsatz.shrink(1);
            level.playSound(null, spieler.blockPosition(), SoundEvents.ARMOR_EQUIP_IRON.value(), SoundSource.PLAYERS, 1.0F, 1.3F);
            spieler.displayClientMessage(Component.translatable("aufsatz.deadzonemc.angebaut",
                    Component.translatable("item.deadzonemc.aufsatz_" + id)).withStyle(ChatFormatting.GREEN), true);
        }
        return InteractionResultHolder.sidedSuccess(aufsatz, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> zeilen, TooltipFlag flag) {
        zeilen.add(Component.translatable("item.deadzonemc.aufsatz_" + id + ".info").withStyle(ChatFormatting.GRAY));
        zeilen.add(Component.translatable("aufsatz.deadzonemc.anleitung").withStyle(ChatFormatting.DARK_GRAY));
    }
}
