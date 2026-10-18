package de.deadzonemc.waffen.feuer;

import de.deadzonemc.beute.Beute;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Lichtkegel von Taschenlampen (in der Hand oder an der Waffe): Ein unsichtbarer Lichtblock wandert dorthin, wohin
 * der Spieler schaut. So leuchtet die Lampe wirklich in die Dunkelheit – ohne Zusatzmod.
 */
public final class Lichtkegel {

    private static final Map<UUID, BlockPos> LICHT = new HashMap<>();
    private static final BlockState HELL = Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 13);

    private Lichtkegel() {
    }

    private static boolean leuchtet(ServerPlayer p) {
        for (ItemStack s : new ItemStack[]{p.getMainHandItem(), p.getOffhandItem()}) {
            if (s.getItem() instanceof TaschenlampeItem && Boolean.TRUE.equals(s.get(Feuerwaffen.LICHT.get()))
                    && s.getOrDefault(Beute.LADUNG.get(), 0) > 0) {
                if (p.tickCount % 20 == 0) {
                    s.set(Beute.LADUNG.get(), Math.max(0, s.getOrDefault(Beute.LADUNG.get(), 0) - 20));
                }
                return true;
            }
            if (s.getItem() instanceof SchusswaffeItem && Feuerwaffen.hatAufsatz(s, "taschenlampe")) {
                return true;
            }
        }
        return false;
    }

    public static void tick(ServerTickEvent.Post event) {
        if (event.getServer().getTickCount() % 2 != 0) {
            return;
        }
        for (ServerLevel lv : event.getServer().getAllLevels()) {
            for (ServerPlayer p : lv.players()) {
                BlockPos alt = LICHT.get(p.getUUID());
                BlockPos neu = null;
                if (leuchtet(p)) {
                    Vec3 auge = p.getEyePosition();
                    Vec3 ende = auge.add(p.getLookAngle().scale(22));
                    BlockHitResult t = lv.clip(new ClipContext(auge, ende, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
                    Vec3 punkt = t.getType() == HitResult.Type.MISS ? ende : t.getLocation().subtract(p.getLookAngle().scale(0.6));
                    BlockPos kandidat = BlockPos.containing(punkt);
                    BlockState dort = lv.getBlockState(kandidat);
                    if (dort.isAir() || dort.is(Blocks.LIGHT)) {
                        neu = kandidat;
                    }
                }
                if (alt != null && !alt.equals(neu) && lv.getBlockState(alt).is(Blocks.LIGHT)) {
                    lv.setBlock(alt, Blocks.AIR.defaultBlockState(), 3);
                }
                if (neu != null) {
                    if (!lv.getBlockState(neu).is(Blocks.LIGHT)) {
                        lv.setBlock(neu, HELL, 3);
                    }
                    LICHT.put(p.getUUID(), neu);
                } else {
                    LICHT.remove(p.getUUID());
                }
            }
        }
    }
}
