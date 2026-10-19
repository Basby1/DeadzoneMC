package de.deadzonemc.gadget;

import de.deadzonemc.klasse.Klasse;
import de.deadzonemc.klasse.Klassen;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Glühhammer: schwerer Hammer mit eingeschmiedetem Glutkern – Treffer setzen in Brand. Geduckt benutzen:
 * Stampfschlag, der alles im Umkreis zurückwirft (Brecher: stärker und öfter).
 */
public class GluehhammerItem extends SwordItem {

    public GluehhammerItem(Tier tier, Properties p) {
        super(tier, p);
    }

    @Override
    public boolean hurtEnemy(ItemStack s, LivingEntity ziel, LivingEntity angreifer) {
        ziel.igniteForSeconds(4);
        return super.hurtEnemy(s, ziel, angreifer);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player p, InteractionHand hand) {
        ItemStack s = p.getItemInHand(hand);
        if (!p.isShiftKeyDown()) {
            return InteractionResultHolder.pass(s);
        }
        boolean brecher = !level.isClientSide && Klassen.klasse(p) == Klasse.BRECHER;
        if (level instanceof ServerLevel sl) {
            double radius = brecher ? 5.5 : 4.0;
            float schaden = brecher ? 9F : 6F;
            for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(radius, 2, radius),
                    e -> e != p && e instanceof Enemy && e.isAlive())) {
                double d = e.distanceTo(p);
                if (d > radius) {
                    continue;
                }
                e.hurt(p.damageSources().playerAttack(p), schaden * (float) (1 - d / radius * 0.5));
                Vec3 weg = e.position().subtract(p.position()).multiply(1, 0, 1).normalize().scale(1.3);
                e.setDeltaMovement(weg.x, 0.55, weg.z);
                e.hurtMarked = true;
                e.igniteForSeconds(3);
            }
            var boden = sl.getBlockState(p.blockPosition().below());
            for (int i = 0; i < 24; i++) {
                double w = i / 24.0 * Math.PI * 2;
                for (double r = 1; r <= radius; r += 1.5) {
                    sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, boden.isAir()
                                    ? net.minecraft.world.level.block.Blocks.DIRT.defaultBlockState() : boden),
                            p.getX() + Math.cos(w) * r, p.getY() + 0.1, p.getZ() + Math.sin(w) * r, 2, 0.1, 0.1, 0.1, 0.1);
                }
            }
            sl.sendParticles(ParticleTypes.FLAME, p.getX(), p.getY() + 0.2, p.getZ(), 30, radius / 2, 0.1, radius / 2, 0.02);
            sl.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.2F, 0.7F);
            de.deadzonemc.monster.Laerm.machen(sl, p.position(), 32);
            s.hurtAndBreak(3, p, LivingEntity.getSlotForHand(hand));
            p.getCooldowns().addCooldown(this, brecher ? 100 : 180);
        }
        p.swing(hand);
        return InteractionResultHolder.sidedSuccess(s, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack s, TooltipContext ctx, List<Component> zeilen, TooltipFlag flag) {
        zeilen.add(Component.translatable("item.deadzonemc.gluehhammer.info").withStyle(ChatFormatting.GRAY));
    }
}
