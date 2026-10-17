package de.deadzonemc.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/** Medizin zum Einnehmen: Antibiotika heilen die Infektion, Schmerzmittel lassen langsam heilen. */
public class MedizinItem extends Item {

    public enum Art { ANTIBIOTIKA, SCHMERZMITTEL, ANTI_RAD, WODKA }

    private final Art art;

    public MedizinItem(Art art, Properties properties) {
        super(properties);
        this.art = art;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 32;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return art == Art.WODKA ? UseAnim.DRINK : UseAnim.EAT;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide) {
            switch (art) {
                case ANTIBIOTIKA -> {
                    // Infektion (The Hordes) und Vergiftung entfernen
                    List<Holder<MobEffect>> weg = new ArrayList<>();
                    for (MobEffectInstance e : entity.getActiveEffects()) {
                        var id = BuiltInRegistries.MOB_EFFECT.getKey(e.getEffect().value());
                        if (id != null && (id.getPath().contains("infect") || e.getEffect().is(MobEffects.POISON))) {
                            weg.add(e.getEffect());
                        }
                    }
                    weg.forEach(entity::removeEffect);
                }
                case ANTI_RAD -> {
                    if (entity instanceof net.minecraft.world.entity.player.Player p) {
                        de.deadzonemc.strahlung.Strahlung.senken(p, 300);
                    }
                }
                case WODKA -> {
                    if (entity instanceof net.minecraft.world.entity.player.Player p) {
                        de.deadzonemc.strahlung.Strahlung.senken(p, 60);
                    }
                    entity.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 240, 0));
                }
                case SCHMERZMITTEL -> {
                    entity.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 300, 0));
                    entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 300, 0));
                }
            }
            level.playSound(null, entity.blockPosition(), SoundEvents.GENERIC_DRINK, SoundSource.PLAYERS, 0.6F, 1.4F);
        }
        stack.consume(1, entity);
        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.deadzonemc." + art.name().toLowerCase() + ".info").withStyle(ChatFormatting.GRAY));
    }
}
