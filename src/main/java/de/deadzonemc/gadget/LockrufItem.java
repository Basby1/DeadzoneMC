package de.deadzonemc.gadget;

import de.deadzonemc.geschichte.Fortschritt;
import de.deadzonemc.monster.MonsterTypen;
import de.deadzonemc.monster.boss.Boss;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.List;

/**
 * Lockruf für einen Endboss, den man schon einmal besiegt hat: Kernresonator (Herz der Zone, im Nether) und
 * Leerensignal (der Ursprung, im End an der Orbitalstation). Wird beim Benutzen verbraucht.
 */
public class LockrufItem extends Item {

    private final String boss;

    public LockrufItem(String boss, Properties p) {
        super(p);
        this.boss = boss;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player p, InteractionHand hand) {
        ItemStack s = p.getItemInHand(hand);
        if (!(level instanceof ServerLevel sl)) {
            return InteractionResultHolder.success(s);
        }
        boolean herz = boss.equals("das_herz");
        if (sl.dimension() != (herz ? Level.NETHER : Level.END)) {
            p.displayClientMessage(Component.translatable("item.deadzonemc.lockruf.falsche_welt." + boss).withStyle(ChatFormatting.RED), true);
            return InteractionResultHolder.fail(s);
        }
        if (!Fortschritt.von(sl.getServer()).hat("tot:" + boss)) {
            p.displayClientMessage(Component.translatable("item.deadzonemc.lockruf.noch_nicht").withStyle(ChatFormatting.RED), true);
            return InteractionResultHolder.fail(s);
        }
        if (!sl.getEntitiesOfClass(Boss.class, p.getBoundingBox().inflate(160, 96, 160)).isEmpty()) {
            p.displayClientMessage(Component.translatable("item.deadzonemc.lockruf.schon_da").withStyle(ChatFormatting.RED), true);
            return InteractionResultHolder.fail(s);
        }
        BlockPos wo;
        if (herz) {
            var blick = p.getLookAngle().multiply(1, 0, 1).normalize().scale(18);
            wo = BlockPos.containing(p.position().add(blick));
            wo = new BlockPos(wo.getX(), p.getBlockY(), wo.getZ());
        } else {
            BlockPos m = de.deadzonemc.orbit.Orbitalstation.MITTE;
            wo = new BlockPos(m.getX(), de.deadzonemc.orbit.Orbitalstation.hoehe(sl) + 1, m.getZ());
        }
        Boss b = (herz ? MonsterTypen.DAS_HERZ.get() : MonsterTypen.DER_URSPRUNG.get()).create(sl);
        if (b == null) {
            return InteractionResultHolder.fail(s);
        }
        for (int i = 0; i < 3; i++) {
            LightningBolt blitz = EntityType.LIGHTNING_BOLT.create(sl);
            if (blitz != null) {
                blitz.moveTo(wo.getX() + sl.random.nextInt(9) - 4, wo.getY(), wo.getZ() + sl.random.nextInt(9) - 4);
                blitz.setVisualOnly(true);
                sl.addFreshEntity(blitz);
            }
        }
        b.moveTo(wo.getX() + 0.5, wo.getY(), wo.getZ() + 0.5, sl.random.nextFloat() * 360, 0);
        b.setPersistenceRequired();
        b.finalizeSpawn(sl, sl.getCurrentDifficultyAt(wo), MobSpawnType.EVENT, null);
        sl.addFreshEntity(b);
        b.auftreten();
        if (b instanceof de.deadzonemc.monster.boss.DerUrsprung u) {
            u.schildAufbauen(4, 21);
        }
        if (!p.getAbilities().instabuild) {
            s.shrink(1);
        }
        return InteractionResultHolder.consume(s);
    }

    @Override
    public void appendHoverText(ItemStack s, TooltipContext ctx, List<Component> zeilen, TooltipFlag flag) {
        zeilen.add(Component.translatable("item.deadzonemc." + (boss.equals("das_herz") ? "kernresonator" : "leerensignal") + ".info")
                .withStyle(ChatFormatting.GRAY));
    }
}
