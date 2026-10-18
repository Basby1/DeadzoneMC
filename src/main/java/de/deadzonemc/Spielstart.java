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
        // Xaero's Minimap in den Fair-Play-Modus schalten (kein Mob-Radar, keine Höhlenansicht) – der Horror
        // lebt davon, dass man nicht weiß, was hinter der nächsten Ecke wartet. Der Code ist unsichtbar.
        spieler.sendSystemMessage(Component.literal("§f§a§i§r§x§a§e§r§o"));
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
        spieler.getInventory().add(new net.minecraft.world.item.ItemStack(ModRegistry.HANDBUCH.get()));
        spieler.getInventory().add(new net.minecraft.world.item.ItemStack(ModRegistry.GENERATOR_ITEM.get()));
        // Eine leere Karte der Umgebung: Sie füllt sich, während man unterwegs ist.
        net.minecraft.world.item.ItemStack karte = net.minecraft.world.item.MapItem.create(oberwelt,
                spieler.getBlockX(), spieler.getBlockZ(), (byte) 2, true, false);
        karte.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,
                Component.translatable("item.deadzonemc.startkarte").withStyle(style -> style.withItalic(false)));
        spieler.getInventory().add(karte);
        spieler.displayClientMessage(Component.translatable("start.deadzonemc.ankunft")
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
    }

    private static final java.util.Map<java.util.UUID, java.util.Set<Long>> GEPEILT = new java.util.HashMap<>();

    /**
     * Funkpeilung: Kommt man in die Nähe einer Zuflucht (ihre Daten liegen schon in den geladenen Chunks),
     * fängt das Funkgerät ihren Ruf auf und verrät grob Richtung und Entfernung. Kostet fast nichts, weil nur
     * bereits geladene Chunks gefragt werden.
     */
    public static void peilen(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer spieler) || spieler.tickCount % 200 != 50
                || spieler.level().dimension() != net.minecraft.world.level.Level.OVERWORLD) {
            return;
        }
        ServerLevel welt = spieler.serverLevel();
        var reg = welt.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.STRUCTURE);
        var zuflucht = reg.get(de.deadzonemc.zuflucht.ZufluchtSchutz.STRUKTUR);
        if (zuflucht == null) {
            return;
        }
        var cp = spieler.chunkPosition();
        for (int dx = -4; dx <= 4; dx += 2) {
            for (int dz = -4; dz <= 4; dz += 2) {
                if (!welt.hasChunk(cp.x + dx, cp.z + dz)) {
                    continue;
                }
                var refs = welt.getChunk(cp.x + dx, cp.z + dz).getReferencesForStructure(zuflucht);
                for (long ref : refs) {
                    if (GEPEILT.computeIfAbsent(spieler.getUUID(), k -> new java.util.HashSet<>()).add(ref)) {
                        var start = new net.minecraft.world.level.ChunkPos(ref);
                        zufluchtVerraten(spieler, new BlockPos(start.getMiddleBlockX(), 0, start.getMiddleBlockZ()));
                    }
                    return;
                }
            }
        }
    }

    /** Ein Funkspruch verrät, wo die Zuflucht liegt (grobe Richtung und Entfernung). */
    public static void zufluchtVerraten(ServerPlayer spieler, BlockPos z) {
        if (z == null) {
            return;
        }
        double dx = z.getX() - spieler.getX();
        double dz = z.getZ() - spieler.getZ();
        int abstand = (int) (Math.sqrt(dx * dx + dz * dz) / 50) * 50;
        String[] namen = {"Süden", "Südwesten", "Westen", "Nordwesten", "Norden", "Nordosten", "Osten", "Südosten"};
        int i = (int) Math.floorMod(Math.round(Math.toDegrees(Math.atan2(-dx, dz)) / 45.0), 8L);
        spieler.sendSystemMessage(Component.literal("Funkgerät, Rauschen: „… Zuflucht … etwa " + Math.max(50, abstand)
                + " Meter Richtung " + namen[i] + " … sicher … Handel … kommt zu uns …“").withStyle(ChatFormatting.DARK_AQUA));
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
