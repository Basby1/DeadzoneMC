package de.deadzonemc.gadget;

import de.deadzonemc.ModRegistry;
import de.deadzonemc.klasse.Klasse;
import de.deadzonemc.klasse.Klassen;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Feldsanitätstasche: Rechtsklick auf einen Mitspieler (oder geduckt auf dich selbst) – verbraucht einen Verband,
 * heilt kräftig und stoppt Gift und Verdorren. Sanitäter heilen mehr und geben zusätzlich Schutz.
 */
public class SanitaetstascheItem extends Item {

    public SanitaetstascheItem(Properties p) {
        super(p);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack s, Player p, LivingEntity ziel, InteractionHand hand) {
        if (!(ziel instanceof Player)) {
            return InteractionResult.PASS;
        }
        if (!p.level().isClientSide) {
            behandeln(p, ziel, s, hand);
        }
        return InteractionResult.sidedSuccess(p.level().isClientSide);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player p, InteractionHand hand) {
        ItemStack s = p.getItemInHand(hand);
        if (!p.isShiftKeyDown()) {
            return InteractionResultHolder.pass(s);
        }
        if (!level.isClientSide) {
            behandeln(p, p, s, hand);
        }
        return InteractionResultHolder.sidedSuccess(s, level.isClientSide);
    }

    private void behandeln(Player p, LivingEntity ziel, ItemStack s, InteractionHand hand) {
        if (p.getCooldowns().isOnCooldown(this)) {
            return;
        }
        ItemStack verband = ItemStack.EMPTY;
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            if (p.getInventory().getItem(i).is(ModRegistry.VERBAND.get())) {
                verband = p.getInventory().getItem(i);
                break;
            }
        }
        if (verband.isEmpty() && !p.getAbilities().instabuild) {
            p.displayClientMessage(Component.translatable("item.deadzonemc.sanitaetstasche.kein_verband").withStyle(ChatFormatting.RED), true);
            return;
        }
        boolean sani = Klassen.klasse(p) == Klasse.SANITAETER;
        ziel.heal(sani ? 12F : 8F);
        ziel.removeEffect(MobEffects.POISON);
        ziel.removeEffect(MobEffects.WITHER);
        if (sani) {
            ziel.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 600, 0));
        }
        if (!p.getAbilities().instabuild) {
            verband.shrink(1);
        }
        s.hurtAndBreak(1, p, LivingEntity.getSlotForHand(hand));
        p.getCooldowns().addCooldown(this, sani ? 60 : 120);
        p.level().playSound(null, ziel.getX(), ziel.getY(), ziel.getZ(), SoundEvents.ARMOR_EQUIP_LEATHER.value(), SoundSource.PLAYERS, 1.0F, 1.2F);
        if (p.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.HEART, ziel.getX(), ziel.getEyeY() + 0.3, ziel.getZ(), 4, 0.3, 0.2, 0.3, 0);
        }
    }

    @Override
    public void appendHoverText(ItemStack s, TooltipContext ctx, List<Component> zeilen, TooltipFlag flag) {
        zeilen.add(Component.translatable("item.deadzonemc.sanitaetstasche.info").withStyle(ChatFormatting.GRAY));
    }
}
