package de.deadzonemc.waffen.wurf;

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

/** Wurfwaffe: Rechtsklick wirft. */
public class WurfItem extends Item {

    public final Wurfart art;

    public WurfItem(Wurfart art, Properties p) {
        super(p);
        this.art = art;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player spieler, InteractionHand hand) {
        ItemStack s = spieler.getItemInHand(hand);
        level.playSound(null, spieler.getX(), spieler.getY(), spieler.getZ(), SoundEvents.SNOWBALL_THROW, SoundSource.PLAYERS,
                0.6F, 0.5F);
        if (!level.isClientSide) {
            Wurfkoerper k = new Wurfkoerper(level, spieler, art);
            k.setItem(s.copyWithCount(1));
            k.shootFromRotation(spieler, spieler.getXRot(), spieler.getYRot(), -6.0F, art.kraft * 1.15F, 0.8F);
            level.addFreshEntity(k);
        }
        spieler.getCooldowns().addCooldown(this, 16);
        s.consume(1, spieler);
        return InteractionResultHolder.sidedSuccess(s, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> zeilen, TooltipFlag flag) {
        zeilen.add(Component.translatable("item.deadzonemc." + art.id + ".info").withStyle(ChatFormatting.GRAY));
    }
}
