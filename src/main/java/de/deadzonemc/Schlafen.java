package de.deadzonemc;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.CanPlayerSleepEvent;
import net.neoforged.neoforge.event.entity.player.PlayerWakeUpEvent;

/**
 * Schlafen nur in sicheren Räumen: ein Dach über dem Kopf und kein Infizierter in 24 Blöcken. Wer sicher
 * geschlafen hat, wacht ausgeruht auf (kurze Regeneration und Sättigung).
 */
public final class Schlafen {

    private Schlafen() {
    }

    public static void registrieren() {
        NeoForge.EVENT_BUS.addListener(Schlafen::pruefen);
        NeoForge.EVENT_BUS.addListener(Schlafen::aufwachen);
    }

    private static void pruefen(CanPlayerSleepEvent event) {
        ServerPlayer p = event.getEntity();
        if (event.getProblem() != null || p.level().dimension() != Level.OVERWORLD) {
            return;
        }
        BlockPos bett = event.getPos();
        if (p.level().canSeeSky(bett.above())) {
            event.setProblem(Player.BedSleepingProblem.OTHER_PROBLEM);
            p.displayClientMessage(Component.translatable("schlafen.deadzonemc.dach").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        boolean gefahr = !p.level().getEntitiesOfClass(de.deadzonemc.monster.DeadzoneMonster.class,
                new net.minecraft.world.phys.AABB(bett).inflate(24, 10, 24), m -> m.isAlive()).isEmpty();
        if (gefahr) {
            event.setProblem(Player.BedSleepingProblem.OTHER_PROBLEM);
            p.displayClientMessage(Component.translatable("schlafen.deadzonemc.gefahr").withStyle(ChatFormatting.RED), true);
        }
    }

    private static void aufwachen(PlayerWakeUpEvent event) {
        if (event.getEntity() instanceof ServerPlayer p && !event.wakeImmediately() && p.getSleepTimer() >= 100) {
            p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 600, 0));
            p.addEffect(new MobEffectInstance(MobEffects.SATURATION, 20, 0));
            p.displayClientMessage(Component.translatable("schlafen.deadzonemc.ausgeruht").withStyle(ChatFormatting.GREEN), true);
        }
    }
}
