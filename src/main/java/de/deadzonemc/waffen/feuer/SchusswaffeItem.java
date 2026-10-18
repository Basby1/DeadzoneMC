package de.deadzonemc.waffen.feuer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Eine Schusswaffe. Linksklick schießt, Rechtsklick (gehalten) zielt, R lädt nach (alles in ClientWaffen /
 * Schiessen / Nachladen). Das Item selbst macht beim Benutzen nichts.
 */
public class SchusswaffeItem extends Item {

    public final WaffenArt art;

    public SchusswaffeItem(WaffenArt art, Properties p) {
        super(p);
        this.art = art;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player spieler, InteractionHand hand) {
        return InteractionResultHolder.pass(spieler.getItemInHand(hand));
    }

    @Override
    public boolean onEntitySwing(ItemStack stack, LivingEntity entity) {
        return true; // kein Schwingen beim Schießen
    }

    @Override
    public boolean canAttackBlock(net.minecraft.world.level.block.state.BlockState s, Level l, net.minecraft.core.BlockPos p, Player pl) {
        return false;
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack alt, ItemStack neu, boolean slotGeaendert) {
        return slotGeaendert || alt.getItem() != neu.getItem();
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> zeilen, TooltipFlag flag) {
        int mag = stack.getOrDefault(Feuerwaffen.MAGAZIN.get(), 0);
        zeilen.add(Component.translatable("waffe.deadzonemc.magazin", mag, Feuerwaffen.magazinGroesse(stack, art),
                Component.translatable("item.deadzonemc." + art.kaliber.itemId())).withStyle(ChatFormatting.GRAY));
        zeilen.add(Component.translatable("waffe.deadzonemc.werte", fmt(art.schaden * art.kugeln), art.reichweite,
                Component.translatable(art.auto ? "waffe.deadzonemc.auto" : "waffe.deadzonemc.einzel")).withStyle(ChatFormatting.DARK_GRAY));
        List<String> a = stack.get(Feuerwaffen.AUFSAETZE.get());
        if (a != null && !a.isEmpty()) {
            for (String id : a) {
                zeilen.add(Component.literal(" + ").append(Component.translatable("item.deadzonemc.aufsatz_" + id))
                        .withStyle(ChatFormatting.AQUA));
            }
        }
        if (!art.aufsaetze.isEmpty() && flag.hasShiftDown()) {
            zeilen.add(Component.translatable("waffe.deadzonemc.passt").withStyle(ChatFormatting.DARK_AQUA));
            for (String id : art.aufsaetze) {
                zeilen.add(Component.literal("  ").append(Component.translatable("item.deadzonemc.aufsatz_" + id))
                        .withStyle(ChatFormatting.DARK_GRAY));
            }
        }
        zeilen.add(Component.translatable("waffe.deadzonemc.steuerung").withStyle(ChatFormatting.DARK_GRAY));
    }

    private static String fmt(float f) {
        return f == (int) f ? String.valueOf((int) f) : String.format(java.util.Locale.ROOT, "%.1f", f);
    }
}
