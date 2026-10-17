package de.deadzonemc;

import com.mojang.logging.LogUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Automatischer Selbsttest für den Testserver bei GitHub. Nur aktiv, wenn der Server mit
 * {@code -Ddeadzonemc.selbsttest=true} gestartet wurde: führt nacheinander Testbefehle aus und
 * fährt den Server danach herunter.
 */
public final class Selbsttest {

    private static final Logger LOG = LogUtils.getLogger();
    public static final boolean AKTIV = Boolean.getBoolean("deadzonemc.selbsttest");

    private static final List<String> BEFEHLE = List.of(
            "deadzone wetter",
            "deadzone wetter dichter_nebel",
            "deadzone wetter saurer_regen 2",
            "deadzone wetter aschesturm",
            "deadzone wetter klar",
            "deadzone wetter",
            "locate biome deadzonemc:vorstadt",
            "locate biome deadzonemc:toter_wald",
            "locate biome deadzonemc:oedland",
            "locate biome deadzonemc:verseuchtes_moor",
            "locate biome deadzonemc:totes_meer",
            "locate structure deadzonemc:siedlung",
            "locate structure deadzonemc:einzelhaus",
            "#zaehlen",
            "gamerule spawnChunkRadius 10",
            "#warten", "#warten", "#warten", "#warten", "#warten", "#warten",
            "place structure deadzonemc:siedlung ~ ~ ~",
            "place structure deadzonemc:einzelhaus ~70 ~ ~70",
            "place structure deadzonemc:einzelhaus ~-70 ~ ~70",
            "place feature deadzonemc:toter_baum ~20 ~ ~-60",
            "place feature deadzonemc:autowrack ~30 ~ ~-60",
            "place feature deadzonemc:schutt ~40 ~ ~-60",
            "place feature deadzonemc:strommast ~50 ~ ~-60",
            "#zaehlen",
            "loot spawn ~ ~20 ~ loot deadzonemc:kiste/haus_kueche",
            "loot spawn ~ ~20 ~ loot deadzonemc:kiste/haus_wohnraum",
            "loot spawn ~ ~20 ~ loot deadzonemc:kiste/haus_schlafzimmer",
            "loot spawn ~ ~20 ~ loot deadzonemc:kiste/haus_keller",
            "loot spawn ~ ~20 ~ loot deadzonemc:kiste/autowrack",
            "save-all"
    );

    private static int tick = 0;
    private static int naechster = 0;

    private Selbsttest() {
    }

    /** Zählt rund um den Spawn, was die Weltgenerierung gebaut hat. */
    private static void zaehlen(MinecraftServer server) {
        ServerLevel level = server.overworld();
        BlockPos spawn = level.getSharedSpawnPos();
        Map<String, Integer> zaehler = new TreeMap<>();
        int radius = 6;
        for (int cx = (spawn.getX() >> 4) - radius; cx <= (spawn.getX() >> 4) + radius; cx++) {
            for (int cz = (spawn.getZ() >> 4) - radius; cz <= (spawn.getZ() >> 4) + radius; cz++) {
                LevelChunk chunk = level.getChunk(cx, cz);
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    zaehler.merge("Behälter " + BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(be.getType()).getPath(), 1, Integer::sum);
                }
                for (int i = 0; i < chunk.getSections().length; i++) {
                    LevelChunkSection teil = chunk.getSections()[i];
                    if (teil.hasOnlyAir()) {
                        continue;
                    }
                    for (int x = 0; x < 16; x++) {
                        for (int y = 0; y < 16; y++) {
                            for (int z = 0; z < 16; z++) {
                                BlockState b = teil.getBlockState(x, y, z);
                                if (b.is(Blocks.SPRUCE_DOOR) || b.is(Blocks.LADDER) || b.is(Blocks.COBWEB)
                                        || b.is(Blocks.COBBLESTONE_WALL) || b.is(Blocks.SKELETON_SKULL)
                                        || b.is(Blocks.GLASS_PANE) || b.is(Blocks.DARK_OAK_LOG)
                                        || b.is(Blocks.STRIPPED_SPRUCE_LOG) || b.is(Blocks.BLACK_CONCRETE)
                                        || b.is(Blocks.GRAY_CONCRETE) || b.getBlock() instanceof net.minecraft.world.level.block.BedBlock) {
                                    zaehler.merge(BuiltInRegistries.BLOCK.getKey(b.getBlock()).getPath(), 1, Integer::sum);
                                }
                            }
                        }
                    }
                }
            }
        }
        LOG.info("[Selbsttest] Zählung um den Spawn {} (13x13 Chunks): {}", spawn.toShortString(), zaehler);
    }

    public static void tick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        tick++;
        if (tick < 100 || tick % 30 != 0) {
            return;
        }
        if (naechster < BEFEHLE.size()) {
            String befehl = BEFEHLE.get(naechster++);
            if (befehl.equals("#warten")) {
                return;
            }
            if (befehl.equals("#zaehlen")) {
                zaehlen(server);
                return;
            }
            LOG.info("[Selbsttest] /{}", befehl);
            try {
                CommandSourceStack quelle = server.createCommandSourceStack();
                server.getCommands().performPrefixedCommand(quelle, befehl);
            } catch (Exception e) {
                LOG.error("[Selbsttest] Fehler bei /{}", befehl, e);
            }
            return;
        }
        if (naechster == BEFEHLE.size()) {
            naechster++;
            LOG.info("[Selbsttest] fertig, Server wird beendet");
            server.halt(false);
        }
    }
}
