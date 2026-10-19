package de.deadzonemc.beute;

import de.deadzonemc.monster.Laerm;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Brecheisen: knackt Tresore (5 Sekunden festhalten, laut!), hebelt Motorhauben von Autowracks auf,
 * taugt notfalls als Schlagwaffe.
 */
public class BrecheisenItem extends SwordItem {

    public static final int DAUER = 100;
    private static final Map<UUID, BlockPos> ZIEL = new HashMap<>();

    public BrecheisenItem(Tier tier, Properties p) {
        super(tier, p);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        Player spieler = ctx.getPlayer();
        if (spieler == null || !ziel(level, ctx.getClickedPos())) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            ZIEL.put(spieler.getUUID(), ctx.getClickedPos());
        }
        spieler.startUsingItem(ctx.getHand());
        return InteractionResult.CONSUME;
    }

    /** Tresore, die noch zu sind, und Motorhauben, die noch nicht aufgehebelt wurden. */
    private static boolean ziel(Level level, BlockPos pos) {
        BlockState s = level.getBlockState(pos);
        if (s.getBlock() instanceof MotorraumBlock) {
            return !s.getValue(MotorraumBlock.OFFEN);
        }
        return level.getBlockEntity(pos) instanceof TresorBlockEntity t && !t.istGeknackt();
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity wer) {
        return 72000;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public void onUseTick(Level level, LivingEntity wer, ItemStack stack, int rest) {
        if (!(level instanceof ServerLevel sl) || !(wer instanceof ServerPlayer spieler)) {
            return;
        }
        BlockPos ziel = ZIEL.get(spieler.getUUID());
        HitResult blick = spieler.pick(5.0, 1.0F, false);
        if (ziel == null || blick.getType() != HitResult.Type.BLOCK || !((BlockHitResult) blick).getBlockPos().equals(ziel)
                || !ziel(level, ziel)) {
            spieler.stopUsingItem();
            return;
        }
        int dauer = getUseDuration(stack, wer) - rest;
        if (level.getBlockState(ziel).getBlock() instanceof MotorraumBlock) {
            // Motorhaube aufhebeln: kürzer und leiser als ein Tresor
            if (dauer % 10 == 0) {
                sl.playSound(null, ziel, net.minecraft.sounds.SoundEvents.CHAIN_HIT, SoundSource.BLOCKS, 0.9F, 0.7F + sl.random.nextFloat() * 0.3F);
            }
            if (dauer % 20 == 0) {
                Laerm.machen(sl, Vec3.atCenterOf(ziel), 16);
                spieler.displayClientMessage(Component.translatable("item.deadzonemc.brecheisen.fortschritt",
                        Math.min(100, dauer * 100 / 50)).withStyle(ChatFormatting.GOLD), true);
            }
            if (dauer >= 50) {
                MotorraumBlock.ausschlachten(sl, ziel, spieler);
                stack.hurtAndBreak(1, spieler, EquipmentSlot.MAINHAND);
                spieler.stopUsingItem();
                ZIEL.remove(spieler.getUUID());
            }
            return;
        }
        if (!(level.getBlockEntity(ziel) instanceof TresorBlockEntity t)) {
            return;
        }
        Vec3 mitte = Vec3.atCenterOf(ziel);
        if (dauer % 12 == 0) {
            sl.playSound(null, ziel, Beute.TRESOR_KNACKEN.get(), SoundSource.BLOCKS, 1.0F, 0.85F + sl.random.nextFloat() * 0.3F);
            sl.sendParticles(net.minecraft.core.particles.ParticleTypes.CRIT, mitte.x, mitte.y + 0.2, mitte.z, 6, 0.3, 0.3, 0.3, 0.1);
        }
        if (dauer % 20 == 0) {
            Laerm.machen(sl, mitte, 28);
            spieler.displayClientMessage(Component.translatable("item.deadzonemc.brecheisen.fortschritt",
                    Math.min(100, dauer * 100 / DAUER)).withStyle(ChatFormatting.GOLD), true);
        }
        if (dauer >= DAUER) {
            t.knacken();
            stack.hurtAndBreak(1, spieler, EquipmentSlot.MAINHAND);
            spieler.stopUsingItem();
            ZIEL.remove(spieler.getUUID());
            spieler.openMenu(t);
        }
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity wer, int rest) {
        ZIEL.remove(wer.getUUID());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> zeilen, TooltipFlag flag) {
        zeilen.add(Component.translatable("item.deadzonemc.brecheisen.info").withStyle(ChatFormatting.GRAY));
    }
}
