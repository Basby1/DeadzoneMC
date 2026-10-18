package de.deadzonemc.zone;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Bolzen zum Werfen: das wichtigste Werkzeug in einem Anomalienfeld. */
public class BolzenItem extends Item {

    public BolzenItem(Properties eigenschaften) {
        super(eigenschaften);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player spieler, InteractionHand hand) {
        ItemStack stack = spieler.getItemInHand(hand);
        level.playSound(null, spieler.getX(), spieler.getY(), spieler.getZ(), SoundEvents.SNOWBALL_THROW,
                SoundSource.PLAYERS, 0.5F, 0.6F);
        if (!level.isClientSide) {
            Bolzen bolzen = new Bolzen(level, spieler);
            bolzen.setItem(stack.copyWithCount(1));
            bolzen.shootFromRotation(spieler, spieler.getXRot(), spieler.getYRot(), 0.0F, 1.1F, 1.0F);
            level.addFreshEntity(bolzen);
        }
        spieler.awardStat(Stats.ITEM_USED.get(this));
        spieler.getCooldowns().addCooldown(this, 8);
        stack.consume(1, spieler);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
