package de.deadzonemc.waffen.feuer;

import de.deadzonemc.ModRegistry;
import de.deadzonemc.beute.Beute;
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

import java.util.List;

/**
 * Taschenlampe: Rechtsklick schaltet sie ein und aus. Eingeschaltet wirft sie einen Lichtkegel dorthin, wohin
 * man schaut – und frisst Batterie (eine Batterie hält 10 Minuten). Schleichen + Rechtsklick legt eine neue ein.
 */
public class TaschenlampeItem extends Item {

    public static final int JE_BATTERIE = 12000;

    public TaschenlampeItem(Properties p) {
        super(p);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player spieler, InteractionHand hand) {
        ItemStack s = spieler.getItemInHand(hand);
        int ladung = s.getOrDefault(Beute.LADUNG.get(), 0);
        if (spieler.isShiftKeyDown() || ladung <= 0) {
            for (int i = 0; i < spieler.getInventory().getContainerSize(); i++) {
                ItemStack b = spieler.getInventory().getItem(i);
                if (b.is(ModRegistry.BATTERIE.get())) {
                    if (!level.isClientSide) {
                        b.shrink(1);
                        s.set(Beute.LADUNG.get(), Math.min(JE_BATTERIE * 3, ladung + JE_BATTERIE));
                        s.set(Feuerwaffen.LICHT.get(), true);
                        level.playSound(null, spieler.blockPosition(), SoundEvents.CROSSBOW_LOADING_END.value(), SoundSource.PLAYERS, 0.7F, 1.8F);
                    }
                    return InteractionResultHolder.sidedSuccess(s, level.isClientSide);
                }
            }
            if (ladung <= 0) {
                spieler.displayClientMessage(Component.translatable("taschenlampe.deadzonemc.leer").withStyle(ChatFormatting.RED), true);
                return InteractionResultHolder.fail(s);
            }
        }
        if (!level.isClientSide) {
            boolean an = !Boolean.TRUE.equals(s.get(Feuerwaffen.LICHT.get()));
            s.set(Feuerwaffen.LICHT.get(), an);
            level.playSound(null, spieler.blockPosition(), SoundEvents.STONE_BUTTON_CLICK_ON, SoundSource.PLAYERS, 0.6F, an ? 1.6F : 1.2F);
        }
        return InteractionResultHolder.sidedSuccess(s, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> zeilen, TooltipFlag flag) {
        int ladung = stack.getOrDefault(Beute.LADUNG.get(), 0);
        zeilen.add(Component.translatable("item.deadzonemc.nachtsichtgeraet.ladung", ladung / 1200, (ladung / 20) % 60)
                .withStyle(ladung > 0 ? ChatFormatting.GREEN : ChatFormatting.RED));
        zeilen.add(Component.translatable("item.deadzonemc.taschenlampe.info").withStyle(ChatFormatting.GRAY));
    }
}
