package de.deadzonemc.gadget;

import de.deadzonemc.klasse.Klasse;
import de.deadzonemc.klasse.Klassen;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Fernglas: halten und hindurchsehen (vergrößert). Wer ein Monster eine Sekunde lang anvisiert, markiert es –
 * es leuchtet für alle sichtbar durch Wände. Späher markieren länger und alle Monster in der Nähe des Ziels.
 */
public class FernglasItem extends Item {

    public FernglasItem(Properties p) {
        super(p);
    }

    @Override
    public int getUseDuration(ItemStack s, LivingEntity wer) {
        return 1200;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack s) {
        return UseAnim.SPYGLASS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player p, InteractionHand hand) {
        p.playSound(SoundEvents.SPYGLASS_USE, 1.0F, 1.0F);
        p.startUsingItem(hand);
        return InteractionResultHolder.consume(p.getItemInHand(hand));
    }

    @Override
    public void onUseTick(Level level, LivingEntity wer, ItemStack s, int rest) {
        int dauer = getUseDuration(s, wer) - rest;
        if (!(level instanceof ServerLevel sl) || !(wer instanceof ServerPlayer p) || dauer < 20 || dauer % 20 != 0) {
            return;
        }
        Vec3 auge = p.getEyePosition();
        Vec3 ende = auge.add(p.getLookAngle().scale(128));
        EntityHitResult t = ProjectileUtil.getEntityHitResult(sl, p, auge, ende, new AABB(auge, ende).inflate(2),
                e -> e instanceof LivingEntity && e instanceof Enemy && e.isAlive(), 0F);
        if (t == null || !(t.getEntity() instanceof LivingEntity ziel)) {
            return;
        }
        boolean spaeher = Klassen.klasse(p) == Klasse.SPAEHER;
        int ticks = spaeher ? 600 : 300;
        ziel.addEffect(new MobEffectInstance(MobEffects.GLOWING, ticks, 0, false, false));
        if (spaeher) {
            for (LivingEntity n : sl.getEntitiesOfClass(LivingEntity.class, ziel.getBoundingBox().inflate(8), e -> e instanceof Enemy)) {
                n.addEffect(new MobEffectInstance(MobEffects.GLOWING, ticks, 0, false, false));
            }
        }
        sl.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.NOTE_BLOCK_HAT.value(), SoundSource.PLAYERS, 0.6F, 1.8F);
    }

    @Override
    public void releaseUsing(ItemStack s, Level level, LivingEntity wer, int rest) {
        wer.playSound(SoundEvents.SPYGLASS_STOP_USING, 1.0F, 1.0F);
    }

    @Override
    public void appendHoverText(ItemStack s, TooltipContext ctx, List<Component> zeilen, TooltipFlag flag) {
        zeilen.add(Component.translatable("item.deadzonemc.fernglas.info").withStyle(ChatFormatting.GRAY));
    }
}
