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
            "gamerule spawnChunkRadius 8",
            "deadzone wetter",
            "deadzone wetter dichter_nebel",
            "deadzone wetter saurer_regen 2",
            "deadzone wetter aschesturm",
            "deadzone wetter klar",
            "deadzone nebel aus",
            "deadzone nebel an",
            "#wasser",
            "locate biome deadzonemc:vorstadt",
            "locate biome deadzonemc:toter_wald",
            "locate biome deadzonemc:oedland",
            "locate biome deadzonemc:verseuchtes_moor",
            "locate biome deadzonemc:totes_meer",
            "deadzone orte",
            "#dorf",
            "#zaehlen",
            "#karte",
            "#feature toter_baum 20 -60",
            "#feature autowrack 30 -60",
            "#feature schutt 40 -60",
            "#feature strommast 50 -60",
            "loot spawn ~ ~20 ~ loot deadzonemc:kiste/kuehlschrank",
            "loot spawn ~ ~20 ~ loot deadzonemc:kiste/keller",
            "loot spawn ~ ~20 ~ loot deadzonemc:kiste/feuerwehr",
            "loot spawn ~ ~20 ~ loot deadzonemc:kiste/briefkasten",
            "save-all"
    );

    /** Mittelpunkt für Karte und Zählung (Spawn oder das gefundene Dorf). */
    private static BlockPos ziel = null;

    private static final long START = System.currentTimeMillis();
    private static int tick = 0;
    private static int naechster = 0;

    private Selbsttest() {
    }

    private static BlockPos mitte(MinecraftServer server) {
        return ziel != null ? ziel : server.overworld().getSharedSpawnPos();
    }

    /** Sucht das nächste Dorf, erzeugt es vollständig und merkt es sich als Mittelpunkt. */
    private static void dorfSuchen(MinecraftServer server) {
        ServerLevel level = server.overworld();
        BlockPos spawn = level.getSharedSpawnPos();
        de.deadzonemc.welt.plan.Siedlung dorf = de.deadzonemc.welt.Ortsbefehle.naechste(level,
                de.deadzonemc.welt.plan.SiedlungsTyp.DORF, spawn.getX(), spawn.getZ());
        if (dorf == null) {
            LOG.error("[Selbsttest] Fehler bei /#dorf: kein gültiges Dorf in der Nähe");
            return;
        }
        var h = de.deadzonemc.welt.stadt.KachelStruktur.hoehen(level.getChunkSource().getGenerator(), level,
                level.getChunkSource().randomState());
        var plan = de.deadzonemc.welt.plan.PlanSpeicher.fertig(level.getSeed(), dorf, h, level.getSeaLevel());
        var haupt = plan.strassen.get(0);
        int y = haupt.hoeheBei(haupt.entlangX ? dorf.x() : dorf.z());
        ziel = new BlockPos(dorf.x(), y, dorf.z());
        Map<String, Integer> arten = new TreeMap<>();
        plan.grundstuecke.forEach(g -> arten.merge(g.typ, 1, Integer::sum));
        LOG.info("[Selbsttest] Dorf {} bei {} (Radius {}, {} Blocke vom Spawn): {} Strassen, {} Grundstuecke {}, {} Objekte",
                dorf.name(), ziel.toShortString(), dorf.radius(), (int) dorf.abstand(spawn.getX(), spawn.getZ()),
                plan.strassen.size(), plan.grundstuecke.size(), arten, plan.objekte.size());
        long start = System.currentTimeMillis();
        int r = (dorf.radius() >> 4) + 3;
        for (int cx = (dorf.x() >> 4) - r; cx <= (dorf.x() >> 4) + r; cx++) {
            for (int cz = (dorf.z() >> 4) - r; cz <= (dorf.z() >> 4) + r; cz++) {
                level.getChunk(cx, cz);
            }
        }
        LOG.info("[Selbsttest] Dorf erzeugt in {} s", (System.currentTimeMillis() - start) / 1000);
    }

    /** Anteil der Fläche unter dem Meeresspiegel in einem großen Gebiet um den Spawn. */
    private static void wasserMessen(MinecraftServer server) {
        ServerLevel level = server.overworld();
        var h = de.deadzonemc.welt.stadt.KachelStruktur.hoehen(level.getChunkSource().getGenerator(), level,
                level.getChunkSource().randomState());
        BlockPos spawn = level.getSharedSpawnPos();
        int nass = 0;
        int alle = 0;
        for (int x = -4096; x <= 4096; x += 512) {
            for (int z = -4096; z <= 4096; z += 512) {
                alle++;
                if (h.boden(spawn.getX() + x, spawn.getZ() + z) < level.getSeaLevel() - 1) {
                    nass++;
                }
            }
        }
        int dorf = 0;
        int doerfer = 0;
        for (var s : de.deadzonemc.welt.plan.Siedlungen.naechste(level.getSeed(),
                de.deadzonemc.welt.plan.SiedlungsTyp.DORF, spawn.getX(), spawn.getZ(), 1)) {
            doerfer++;
            if (de.deadzonemc.welt.Ortsbefehle.gueltig(level, s)) {
                dorf++;
            }
        }
        LOG.info("[Selbsttest] Wasseranteil (8000x8000 Blöcke, grob): {} %, Dörfer in der Nähe gültig: {} von {}",
                nass * 100 / alle, dorf, doerfer);
    }

    /** Draufsicht und Grundriss rund um den Spawn als Text – zum Nachsehen, ob die Häuser stimmen. */
    private static void karte(MinecraftServer server) {
        ServerLevel level = server.overworld();
        BlockPos spawn = mitte(server);
        int r = 40;
        for (int z = spawn.getZ() - r; z <= spawn.getZ() + r; z++) {
            if (z == spawn.getZ() - r) {
                LOG.info("[Selbsttest] Draufsicht Nordhälfte (Norden oben), Mitte = X");
            } else if (z == spawn.getZ() + 1) {
                LOG.info("[Selbsttest] Draufsicht Südhälfte");
            }
            StringBuilder zeile = new StringBuilder();
            for (int x = spawn.getX() - r; x <= spawn.getX() + r; x++) {
                if (x == spawn.getX() && z == spawn.getZ()) {
                    zeile.append('X');
                    continue;
                }
                BlockState oben = Blocks.AIR.defaultBlockState();
                for (int y = spawn.getY() + 30; y > spawn.getY() - 30; y--) {
                    BlockState b = level.getBlockState(new BlockPos(x, y, z));
                    if (!b.isAir() && !b.is(Blocks.SHORT_GRASS) && !b.is(Blocks.TALL_GRASS) && !b.is(Blocks.DEAD_BUSH)) {
                        oben = b;
                        break;
                    }
                }
                zeile.append(zeichen(oben, true));
            }
            LOG.info("[Selbsttest] |{}|", zeile);
        }
        for (int dy = 2; dy <= 2; dy++) {
            for (int z = spawn.getZ() - r; z <= spawn.getZ() + r; z++) {
                if (z == spawn.getZ() - r) {
                    LOG.info("[Selbsttest] Grundriss Nordhälfte auf Höhe Mitte+{}", dy);
                } else if (z == spawn.getZ() + 1) {
                    LOG.info("[Selbsttest] Grundriss Südhälfte");
                }
                StringBuilder zeile = new StringBuilder();
                for (int x = spawn.getX() - r; x <= spawn.getX() + r; x++) {
                    zeile.append(zeichen(level.getBlockState(new BlockPos(x, spawn.getY() + dy, z)), false));
                }
                LOG.info("[Selbsttest] |{}|", zeile);
            }
        }
    }

    private static char zeichen(BlockState b, boolean draufsicht) {
        if (b.isAir()) {
            return ' ';
        }
        if (!b.getFluidState().isEmpty()) {
            return '~';
        }
        String n = BuiltInRegistries.BLOCK.getKey(b.getBlock()).getPath();
        if (n.contains("door")) return 'D';
        if (n.contains("stairs")) return '^';
        if (n.contains("pane")) return 'o';
        if (n.equals("ladder")) return 'H';
        if (n.equals("chest") || n.equals("barrel")) return 'C';
        if (n.endsWith("_bed")) return 'b';
        if (n.equals("cobweb")) return 'w';
        if (n.endsWith("_log")) return 'T';
        if (n.endsWith("_wall") || n.equals("end_rod") || n.equals("chain")) return 'i';
        if (n.equals("white_concrete")) return '-';
        if (n.equals("gray_concrete") || n.equals("andesite") || n.equals("tuff") || n.equals("black_concrete_powder")) return '=';
        if (n.contains("concrete") || n.contains("terracotta") || n.contains("brick") || n.equals("packed_mud")
                || n.equals("polished_andesite") || n.equals("deepslate_tiles")) return '#';
        if (n.endsWith("planks") || n.endsWith("slab")) return 'p';
        if (n.contains("cobblestone")) return 'c';
        if (n.equals("gravel") || n.equals("coarse_dirt")) return ':';
        if (n.equals("grass_block") || n.equals("dirt") || n.equals("podzol") || n.equals("mud")) return '.';
        if (n.equals("sand") || n.equals("stone") || n.equals("granite") || n.equals("diorite")) return ',';
        if (n.endsWith("leaves")) return '*';
        return draufsicht ? '?' : '+';
    }

    /** Zählt rund um den Spawn, was die Weltgenerierung gebaut hat. */
    private static void zaehlen(MinecraftServer server) {
        ServerLevel level = server.overworld();
        BlockPos spawn = mitte(server);
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
        LOG.info("[Selbsttest] Zählung um {} (13x13 Chunks): {}", spawn.toShortString(), zaehler);
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
            if (befehl.startsWith("#feature ")) {
                String[] teile = befehl.split(" ");
                ServerLevel level = server.overworld();
                BlockPos spawn = level.getSharedSpawnPos();
                int x = spawn.getX() + Integer.parseInt(teile[2]);
                int z = spawn.getZ() + Integer.parseInt(teile[3]);
                int y = 0;
                // trockenen Boden in der Nähe suchen
                suche:
                for (int versuch = 0; versuch < 40; versuch++) {
                    int px = x + (versuch % 8) * 6 - 21;
                    int pz = z + (versuch / 8) * 6 - 12;
                    level.getChunk(px >> 4, pz >> 4);
                    int py = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, px, pz);
                    BlockState unten = level.getBlockState(new BlockPos(px, py - 1, pz));
                    if (unten.getFluidState().isEmpty() && unten.isSolid() && level.getBlockState(new BlockPos(px, py, pz)).isAir()
                            && !de.deadzonemc.welt.plan.PlanSpeicher.belegt(level.getSeed(), px, pz, 8)) {
                        x = px;
                        y = py;
                        z = pz;
                        break suche;
                    }
                }
                befehl = "place feature deadzonemc:" + teile[1] + " " + x + " " + y + " " + z;
            }
            if (befehl.equals("#dorf")) {
                dorfSuchen(server);
                return;
            }
            if (befehl.equals("#wasser")) {
                wasserMessen(server);
                return;
            }
            if (befehl.equals("#karte")) {
                karte(server);
                return;
            }
            if (befehl.equals("#zaehlen")) {
                zaehlen(server);
                return;
            }
            LOG.info("[Selbsttest] /{} (nach {} s)", befehl, (System.currentTimeMillis() - START) / 1000);
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
