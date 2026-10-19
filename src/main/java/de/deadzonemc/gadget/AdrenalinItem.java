package de.deadzonemc.gadget;

import de.deadzonemc.klasse.Klasse;
import de.deadzonemc.klasse.Klassen;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/** Adrenalinspritze (aus Mutagen): kurz stärker, schneller und zäher – danach erschöpft. Brecher halten länger durch. */
public class AdrenalinItem extends Item {

    public AdrenalinItem(Properties p) {
        super(p);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player p, InteractionHand hand) {
        ItemStack s = p.getItemInHand(hand);
        if (!level.isClientSide) {
            boolean brecher = Klassen.klasse(p) == Klasse.BRECHER;
            int dauer = brecher ? 900 : 600;
            p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, dauer, 0));
            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, dauer, 0));
            if (brecher) {
                p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, dauer, 0));
            }
            level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.HONEY_DRINK, SoundSource.PLAYERS, 1.0F, 0.6F);
            p.getCooldowns().addCooldown(this, dauer + 400);
            if (!p.getAbilities().instabuild) {
                s.shrink(1);
            }
            // Nachwirkung: nach dem Rausch kommt die Erschöpfung
            de.deadzonemc.gadget.Nachwirkung.planen(p, dauer, new MobEffectInstance(MobEffects.WEAKNESS, 400, 0));
        }
        return InteractionResultHolder.sidedSuccess(s, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack s, TooltipContext ctx, List<Component> zeilen, TooltipFlag flag) {
        zeilen.add(Component.translatable("item.deadzonemc.adrenalinspritze.info").withStyle(ChatFormatting.GRAY));
    }
}
