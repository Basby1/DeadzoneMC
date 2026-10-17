package de.deadzonemc;

import de.deadzonemc.item.NotizItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Der erste Moment in der Deadzone: Alle starten im selben Gebiet, aber verstreut – höchstens 400 Blöcke
 * voneinander entfernt. Wer sich finden will, muss suchen.
 */
public final class Spielstart {

    /** Höchstens so weit vom Weltspawn entfernt (in jede Richtung) – zwei Spieler also max. 400 Blöcke. */
    private static final int STREUUNG = 140;

    private Spielstart() {
    }

    public static void beimBetreten(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer spieler)) {
            return;
        }
        if (spieler.getData(ModRegistry.GESTARTET.get())) {
            return;
        }
        spieler.setData(ModRegistry.GESTARTET.get(), true);
        ServerLevel oberwelt = spieler.server.overworld();
        BlockPos ziel = startplatzSuchen(oberwelt, spieler.getRandom());
        if (ziel != null && spieler.level() == oberwelt) {
            spieler.teleportTo(oberwelt, ziel.getX() + 0.5, ziel.getY(), ziel.getZ() + 0.5,
                    spieler.getRandom().nextFloat() * 360F, 0F);
        }
        spieler.getInventory().add(NotizItem.erstellen("start"));
        spieler.displayClientMessage(Component.translatable("start.deadzonemc.ankunft")
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
    }

    private static BlockPos startplatzSuchen(ServerLevel level, RandomSource zufall) {
        BlockPos mitte = level.getSharedSpawnPos();
        for (int versuch = 0; versuch < 24; versuch++) {
            int x = mitte.getX() + zufall.nextInt(2 * STREUUNG + 1) - STREUUNG;
            int z = mitte.getZ() + zufall.nextInt(2 * STREUUNG + 1) - STREUUNG;
            level.getChunk(x >> 4, z >> 4); // Chunk laden/erzeugen
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockState darunter = level.getBlockState(new BlockPos(x, y - 1, z));
            if (darunter.getFluidState().isEmpty() && darunter.isSolid()) {
                return new BlockPos(x, y, z);
            }
        }
        return null;
    }
}
