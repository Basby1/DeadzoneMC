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
    private static final String MODUS = System.getProperty("deadzonemc.selbsttest", "false");
    public static final boolean AKTIV = !MODUS.equals("false");
    /** Test des ganzen Modpacks (Lost Cities usw.) statt nur unserer Mod. */
    private static final boolean PACK = MODUS.equals("pack");

    private static final List<String> PACK_BEFEHLE = List.of(
            "gamerule spawnChunkRadius 8",
            "deadzone wetter dichter_nebel",
            "deadzone nebel aus",
            "deadzone nebel an",
            "#wasser",
            "#uebersicht",
            "#stadtkarte 0 0",
            "#stadtkarte 400 0",
            "#stadtkarte 0 400",
            "#zaehlen",
            "#stadthorde",
            "#zuflucht",
            "loot spawn ~ ~20 ~ loot deadzonemc:kiste/kuehlschrank",
            "#waffen",
            "#versteck",
            "#poi",
            "place feature deadzonemc:absturz_hubschrauber ~30 ~ ~30",
            "place feature deadzonemc:absturz_flugzeug ~-30 ~ ~30",
            "place feature deadzonemc:frachter ~30 ~ ~-30",
            "place feature deadzonemc:panzer ~-30 ~ ~-30",
            "place feature deadzonemc:konvoi ~60 ~ ~0",
            "setblock ~2 ~ ~2 deadzonemc:geschuetzturm",
            "setblock ~4 ~ ~2 deadzonemc:zonenwerkbank",
            "setblock ~6 ~ ~2 deadzonemc:zelt",
            "summon deadzonemc:lenker ~-20 ~2 ~-12",
            "summon deadzonemc:spuk ~-18 ~2 ~-12",
            "summon deadzonemc:truegruede ~-16 ~2 ~-12",
            "summon deadzonemc:entreisser ~-14 ~2 ~-12",
            "summon deadzonemc:chimaere ~-12 ~2 ~-12",
            "summon deadzonemc:glasgaenger ~-10 ~2 ~-12",
            "summon deadzonemc:schwebender ~-8 ~2 ~-12",
            "summon deadzonemc:entlader ~-6 ~2 ~-12",
            "summon deadzonemc:echo ~-4 ~2 ~-12",
            "summon deadzonemc:zonenhirsch ~-2 ~2 ~-12",
            "summon deadzonemc:leerlaeufer ~0 ~2 ~-12",
            "summon deadzonemc:wartungsspinne ~2 ~2 ~-12",
            "summon deadzonemc:wachkugel ~4 ~2 ~-12",
            "summon deadzonemc:versuchsobjekt ~6 ~2 ~-12",
            "summon deadzonemc:konglomerat ~8 ~2 ~-12",
            "summon deadzonemc:kryoschlaefer ~10 ~2 ~-12",
            "summon deadzonemc:leerenaal ~12 ~2 ~-12",
            "summon deadzonemc:klammerer ~14 ~2 ~-12",
            "summon deadzonemc:hologeist ~16 ~2 ~-12",
            "summon deadzonemc:forscher ~18 ~2 ~-12",
            "summon deadzonemc:das_herz ~40 ~2 ~40",
            "summon deadzonemc:wiedergaenger ~-24 ~2 ~-12",
            "setblock ~8 ~ ~2 deadzonemc:stoersender",
            "setblock ~10 ~ ~2 deadzonemc:brandgraben",
            "summon deadzonemc:der_ursprung ~-40 ~2 ~40",
            "deadzone geschichte 0",
            "summon deadzonemc:wanderer ~-15 ~2 ~5",
            "summon deadzonemc:schreier ~-12 ~2 ~5",
            "summon deadzonemc:lauerer ~-9 ~2 ~5",
            "summon deadzonemc:kriecher ~-6 ~2 ~5",
            "summon deadzonemc:der_stille ~-3 ~2 ~5",
            "summon deadzonemc:imitator ~0 ~2 ~5",
            "summon deadzonemc:nebelwesen ~3 ~2 ~5",
            "summon deadzonemc:schwarm ~6 ~2 ~5",
            "summon deadzonemc:brut ~9 ~2 ~5",
            "summon deadzonemc:juggernaut ~12 ~2 ~5",
            "summon deadzonemc:wache ~-10 ~2 ~-8",
            "summon deadzonemc:bewohner ~-14 ~2 ~-8",
            "summon deadzonemc:auftraggeber ~-18 ~2 ~-8",
            "summon deadzonemc:begleiter ~-22 ~2 ~-8",
            "summon deadzonemc:gefallener_soldat ~-18 ~2 ~14",
            "summon deadzonemc:glutleib ~-14 ~2 ~14",
            "summon deadzonemc:seuchenhund ~-10 ~2 ~14",
            "summon deadzonemc:wuehler ~-6 ~2 ~14",
            "summon deadzonemc:sauger ~-2 ~2 ~14",
            "summon deadzonemc:seuchentraeger ~2 ~2 ~14",
            "summon deadzonemc:ertrunkener ~6 ~2 ~14",
            "summon deadzonemc:spaeherdrohne ~10 ~2 ~14",
            "summon deadzonemc:lichtmotte ~14 ~2 ~14",
            "summon deadzonemc:scheintoter ~18 ~2 ~14",
            "summon deadzonemc:verschmolzener ~-18 ~2 ~22",
            "summon deadzonemc:nebelmutter ~-14 ~2 ~22",
            "summon deadzonemc:strahlende ~-10 ~2 ~22",
            "summon deadzonemc:brutkoenigin ~-6 ~2 ~22",
            "summon deadzonemc:schlaechter ~-2 ~2 ~22",
            "summon deadzonemc:kommandant ~2 ~2 ~22",
            "summon deadzonemc:hirte ~6 ~2 ~22",
            "summon deadzonemc:schlund ~10 ~2 ~22",
            "summon deadzonemc:pilzvater ~14 ~2 ~22",
            "summon deadzonemc:erfrorene ~18 ~2 ~22",
            "summon deadzonemc:tarnfresser ~22 ~2 ~14",
            "summon deadzonemc:schwarmmutter ~22 ~2 ~22",
            "summon deadzonemc:pilot ~26 ~2 ~22",
            "#warten", "#warten", "#warten", "#warten", "#warten", "#warten",
            "summon deadzonemc:soldat ~-6 ~2 ~-8",
            "summon deadzonemc:kultist ~-2 ~2 ~-8",
            "summon deadzonemc:ueberlebender ~2 ~2 ~-8",
            "summon deadzonemc:haendler ~6 ~2 ~-8",
            "#warten", "#warten",
            "#monster",
            "kill @e[type=#deadzonemc:monster]",
            "kill @e[type=deadzonemc:soldat]",
            "kill @e[type=deadzonemc:kultist]",
            "kill @e[type=deadzonemc:ueberlebender]",
            "kill @e[type=deadzonemc:haendler]",
            "kill @e[type=deadzonemc:wache]",
            "kill @e[type=deadzonemc:bewohner]",
            "kill @e[type=deadzonemc:auftraggeber]",
            "kill @e[type=deadzonemc:begleiter]",
            "forceload add ~-24 ~-24 ~24 ~24",
            "#fallen",
            "#station",
            "#feature anomalie 30 -40",
            "deadzone anomalie strudel",
            "summon deadzonemc:wanderer ~2 ~1 ~5",
            "deadzone abwurf",
            "deadzone emission start",
            "#emission_schnell",
            "#warten", "#warten", "#warten",
            "#zone",
            "#warten", "#warten", "#warten", "#warten", "#warten", "#warten", "#warten", "#warten",
            "#warten", "#warten", "#warten", "#warten", "#warten", "#warten", "#warten", "#warten",
            "#zone",
            "#fallen pruefen",
            "kill @e[type=#deadzonemc:monster]",
            "#herde",
            "#warten", "#warten",
            "kill @e[type=#deadzonemc:monster]",
            "deadzone basis stufe sicher",
            "setblock ~ ~10 ~40 deadzonemc:notstromgenerator",
            "summon deadzonemc:wanderer ~3 ~11 ~43",
            "summon deadzonemc:juggernaut ~-3 ~11 ~37",
            "#warten", "#warten", "#warten",
            "deadzone basis info",
            "execute positioned ~ ~10 ~40 run #monsterzahl",
            "deadzone basis stufe belagerung",
            "save-all"
    );

    private static final List<String> MOD_BEFEHLE = List.of(
            "gamerule spawnChunkRadius 8",
            "deadzone wetter",
            "deadzone wetter dichter_nebel",
            "deadzone wetter saurer_regen 2",
            "deadzone wetter aschesturm",
            "deadzone wetter klar",
            "deadzone nebel aus",
            "deadzone nebel an",
            "#wasser",
            "#zaehlen",
            "#karte",
            "#feature toter_baum 20 -60",
            "#feature autowrack 30 -60",
            "#feature schutt 40 -60",
            "#feature strommast 50 -60",
            "#feature strahlenfaesser 60 -60",
            "forceload add ~-24 ~-24 ~24 ~24",
            "#fallen",
            "#station",
            "#feature anomalie 30 -40",
            "deadzone anomalie strudel",
            "summon deadzonemc:wanderer ~2 ~1 ~5",
            "deadzone abwurf",
            "deadzone emission start",
            "#emission_schnell",
            "#warten", "#warten", "#warten",
            "#zone",
            "#warten", "#warten", "#warten", "#warten", "#warten", "#warten", "#warten", "#warten",
            "#warten", "#warten", "#warten", "#warten", "#warten", "#warten", "#warten", "#warten",
            "#zone",
            "#fallen pruefen",
            "kill @e[type=#deadzonemc:monster]",
            "loot spawn ~ ~20 ~ loot deadzonemc:kiste/kuehlschrank",
            "loot spawn ~ ~20 ~ loot deadzonemc:kiste/keller",
            "loot spawn ~ ~20 ~ loot deadzonemc:kiste/feuerwehr",
            "loot spawn ~ ~20 ~ loot deadzonemc:kiste/briefkasten",
            "save-all"
    );

    private static final List<String> BEFEHLE = PACK ? PACK_BEFEHLE : MOD_BEFEHLE;

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
        long hoehe = 0;
        double kont = 0;
        for (int x = -4096; x <= 4096; x += 256) {
            for (int z = -4096; z <= 4096; z += 256) {
                alle++;
                int b = h.boden(spawn.getX() + x, spawn.getZ() + z);
                hoehe += b;
                kont += level.getChunkSource().randomState().router().continents().compute(
                        new net.minecraft.world.level.levelgen.DensityFunction.SinglePointContext(spawn.getX() + x, 64, spawn.getZ() + z));
                if (b < level.getSeaLevel() - 1) {
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
        LOG.info("[Selbsttest] Wasseranteil (8000x8000 Blöcke, grob): {} %, mittlere Höhe {}, mittlere Kontinentalität {}, Dörfer in der Nähe gültig: {} von {}",
                nass * 100 / alle, hoehe / alle, String.format(java.util.Locale.ROOT, "%.3f", kont / alle), dorf, doerfer);
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

    /**
     * Höhenkarte über 240×240 Blöcke (1 Zeichen = 3 Blöcke) – zeigt, ob und wo Städte stehen.
     * ~ Wasser, . Gelände, = Straße/Platz, 1–9 Gebäudehöhe in 8er-Schritten über Gelände, # über 72.
     */
    private static void stadtkarte(MinecraftServer server, int ox, int oz) {
        ServerLevel level = server.overworld();
        BlockPos spawn = level.getSharedSpawnPos();
        int mx = spawn.getX() + ox;
        int mz = spawn.getZ() + oz;
        int meer = level.getSeaLevel();
        long start = System.currentTimeMillis();
        int r = 40;
        Map<Character, Integer> anteil = new TreeMap<>();
        java.util.List<String> zeilen = new java.util.ArrayList<>();
        for (int iz = -r; iz <= r; iz++) {
            StringBuilder zeile = new StringBuilder();
            for (int ix = -r; ix <= r; ix++) {
                int x = mx + ix * 3;
                int z = mz + iz * 3;
                level.getChunk(x >> 4, z >> 4);
                int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                BlockState b = level.getBlockState(new BlockPos(x, y - 1, z));
                char c;
                if (!b.getFluidState().isEmpty()) {
                    c = '~';
                } else {
                    String n = BuiltInRegistries.BLOCK.getKey(b.getBlock()).getPath();
                    int ueber = y - Math.max(meer, 64) - 4;
                    boolean natur = n.contains("grass") || n.equals("dirt") || n.equals("sand") || n.equals("stone")
                            || n.endsWith("_log") || n.equals("podzol") || n.equals("gravel") || n.equals("mud")
                            || n.equals("coarse_dirt") || n.equals("snow") || n.equals("snow_block");
                    if (natur || ueber < 4) {
                        c = natur ? '.' : '=';
                    } else if (ueber >= 72) {
                        c = '#';
                    } else {
                        c = (char) ('1' + ueber / 8);
                    }
                }
                anteil.merge(c, 1, Integer::sum);
                zeile.append(c);
            }
            zeilen.add(zeile.toString());
        }
        LOG.info("[Selbsttest] Stadtkarte Nord um {} {} (1 Zeichen = 3 Blöcke, {} s)", mx, mz, (System.currentTimeMillis() - start) / 1000);
        for (int i = 0; i < zeilen.size(); i++) {
            if (i == r + 1) {
                LOG.info("[Selbsttest] Stadtkarte Süd um {} {}", mx, mz);
            }
            LOG.info("[Selbsttest] |{}|", zeilen.get(i));
        }
        LOG.info("[Selbsttest] Anteile der Stadtkarte {} {}: {}", mx, mz, anteil);
    }

    /** Grobe Weltübersicht 3240×3240 Blöcke (1 Zeichen = 40 Blöcke) aus dem Generator: ~ Wasser, . Land, ^ Berge. */
    private static void uebersicht(MinecraftServer server) {
        ServerLevel level = server.overworld();
        var h = de.deadzonemc.welt.stadt.KachelStruktur.hoehen(level.getChunkSource().getGenerator(), level,
                level.getChunkSource().randomState());
        BlockPos spawn = level.getSharedSpawnPos();
        int meer = level.getSeaLevel();
        int r = 40;
        LOG.info("[Selbsttest] Übersicht Nord (1 Zeichen = 40 Blöcke, X = Spawn)");
        for (int iz = -r; iz <= r; iz++) {
            if (iz == 1) {
                LOG.info("[Selbsttest] Übersicht Süd");
            }
            StringBuilder zeile = new StringBuilder();
            for (int ix = -r; ix <= r; ix++) {
                if (ix == 0 && iz == 0) {
                    zeile.append('X');
                    continue;
                }
                int b = h.boden(spawn.getX() + ix * 40, spawn.getZ() + iz * 40);
                zeile.append(b < meer - 1 ? '~' : b > 115 ? '^' : '.');
            }
            LOG.info("[Selbsttest] |{}|", zeile);
        }
    }

    /** Würfelt Waffenbeute aus und prüft über TaCZ selbst, ob die Waffen erkannt werden. */
    /** Schreibt einen Blockquader als JSON nach poi_dump/ – für die Sichtprüfung der Bauten außerhalb des Spiels. */
    private static void blockDump(ServerLevel w, String name, int x, int y, int z, int r, int unten, int oben) {
        try {
            java.nio.file.Path ordner = java.nio.file.Path.of("poi_dump");
            java.nio.file.Files.createDirectories(ordner);
            java.util.Map<String, Integer> palette = new java.util.LinkedHashMap<>();
            StringBuilder daten = new StringBuilder();
            for (int dy = -unten; dy <= oben; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    for (int dx = -r; dx <= r; dx++) {
                        var b = w.getBlockState(new BlockPos(x + dx, y + dy, z + dz));
                        String k = BuiltInRegistries.BLOCK.getKey(b.getBlock()).toString();
                        if (b.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.AXIS)) {
                            k += "[" + b.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.AXIS) + "]";
                        }
                        Integer id = palette.get(k);
                        if (id == null) {
                            id = palette.size();
                            palette.put(k, id);
                        }
                        daten.append(id).append(',');
                    }
                }
            }
            StringBuilder j = new StringBuilder("{\"groesse\":[").append(2 * r + 1).append(',').append(unten + oben + 1)
                    .append(',').append(2 * r + 1).append("],\"boden\":").append(unten).append(",\"palette\":[");
            boolean erst = true;
            for (String k : palette.keySet()) {
                j.append(erst ? "" : ",").append('"').append(k).append('"');
                erst = false;
            }
            j.append("],\"daten\":[").append(daten, 0, Math.max(0, daten.length() - 1)).append("]}");
            java.nio.file.Files.writeString(ordner.resolve(name + ".json"), j);
        } catch (Exception e) {
            LOG.warn("[Selbsttest] Blockdump {} fehlgeschlagen: {}", name, e.toString());
        }
    }

    private static void waffenPruefen(MinecraftServer server) {
        ServerLevel level = server.overworld();
        StringBuilder b = new StringBuilder();
        for (String tabelle : new String[]{"waffen/polizei", "waffen/waffenkammer", "waffen/bunker"}) {
            var schluessel = net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE,
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("deadzonemc", tabelle));
            var t = server.reloadableRegistries().getLootTable(schluessel);
            var params = new net.minecraft.world.level.storage.loot.LootParams.Builder(level)
                    .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN, net.minecraft.world.phys.Vec3.ZERO)
                    .create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.CHEST);
            b.append(tabelle).append(": ");
            for (int i = 0; i < 4; i++) {
                for (net.minecraft.world.item.ItemStack s : t.getRandomItems(params)) {
                    b.append(BuiltInRegistries.ITEM.getKey(s.getItem()).getPath()).append(" x").append(s.getCount());
                    if (s.getItem() instanceof de.deadzonemc.waffen.feuer.SchusswaffeItem) {
                        b.append(" Magazin=").append(s.getOrDefault(de.deadzonemc.waffen.feuer.Feuerwaffen.MAGAZIN.get(), 0));
                    }
                    b.append(", ");
                }
            }
            b.append(" | ");
        }
        LOG.info("[Selbsttest] Waffen-Beute: {}", b);
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
                    var tag = be.saveWithoutMetadata(level.registryAccess());
                    for (String schluessel : new String[]{"LootTable", "lootTable", "table"}) {
                        if (tag.contains(schluessel)) {
                            zaehler.merge("Beute " + tag.getString(schluessel).replace("minecraft:", "").replace("deadzonemc:", ""), 1, Integer::sum);
                        }
                    }
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
                                        || b.is(Blocks.GRAY_CONCRETE) || b.is(Blocks.SOUL_LANTERN) || b.is(Blocks.CHAIN) || b.is(Blocks.RAIL)
                                        || b.getBlock() instanceof net.minecraft.world.level.block.BedBlock) {
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

    private static BlockPos zufluchtOrt;

    /** Öffnet (simuliert) Truhen um einen Ort: würfelt die Beute aus und zählt, was drin ist. */
    private static void truhen(MinecraftServer server, BlockPos um, String name) {
        ServerLevel level = server.overworld();
        var spieler = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);
        spieler.moveTo(um.getX(), um.getY(), um.getZ());
        Map<String, String> ergebnis = new TreeMap<>();
        int leer = 0;
        int voll = 0;
        int ohneTabelle = 0;
        for (int cx = (um.getX() >> 4) - 4; cx <= (um.getX() >> 4) + 4; cx++) {
            for (int cz = (um.getZ() >> 4) - 4; cz <= (um.getZ() >> 4) + 4; cz++) {
                LevelChunk chunk = level.getChunk(cx, cz);
                for (BlockEntity be : new java.util.ArrayList<>(chunk.getBlockEntities().values())) {
                    if (!(be instanceof net.minecraft.world.Container behaelter)) {
                        continue;
                    }
                    String typ = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(be.getType()).toString();
                    var tag = be.saveWithoutMetadata(level.registryAccess());
                    String tabelle = tag.contains("LootTable") ? tag.getString("LootTable") : null;
                    // 1) echter Weg für normale Truhen: Beute auspacken wie beim Öffnen
                    int echt = -1;
                    if (be instanceof net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity r) {
                        r.unpackLootTable(spieler);
                        echt = 0;
                        for (int i = 0; i < behaelter.getContainerSize(); i++) {
                            if (!behaelter.getItem(i).isEmpty()) {
                                echt++;
                            }
                        }
                    }
                    // 2) Tabelle direkt auswürfeln (zeigt, ob die Tabelle selbst etwas liefert)
                    int gewuerfelt = -1;
                    if (tabelle != null) {
                        var key = net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE,
                                net.minecraft.resources.ResourceLocation.parse(tabelle));
                        var tab = server.reloadableRegistries().getLootTable(key);
                        var params = new net.minecraft.world.level.storage.loot.LootParams.Builder(level)
                                .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN,
                                        net.minecraft.world.phys.Vec3.atCenterOf(be.getBlockPos()))
                                .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.THIS_ENTITY, spieler)
                                .create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.CHEST);
                        var sc = new net.minecraft.world.SimpleContainer(27);
                        tab.fill(sc, params, level.random.nextLong());
                        gewuerfelt = 0;
                        for (int i = 0; i < 27; i++) {
                            if (!sc.getItem(i).isEmpty()) {
                                gewuerfelt++;
                            }
                        }
                    } else {
                        ohneTabelle++;
                    }
                    if (echt > 0 || gewuerfelt > 0) {
                        voll++;
                    } else {
                        leer++;
                    }
                    if (ergebnis.size() < 40) {
                        ergebnis.put(typ.replace("minecraft:", "") + "@" + be.getBlockPos().toShortString(),
                                (tabelle == null ? "ohne Tabelle" : tabelle.replace("deadzonemc:", "")) + " echt=" + echt
                                        + " gewürfelt=" + gewuerfelt);
                    }
                }
            }
        }
        LOG.info("[Selbsttest] Truhen {}: voll {}, leer {}, ohne Tabelle {} – {}", name, voll, leer, ohneTabelle, ergebnis);
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
            if (befehl.equals("#herde")) {
                ServerLevel ow = server.overworld();
                StringBuilder d = new StringBuilder();
                for (int i = 0; i < 3; i++) {
                    var h = de.deadzonemc.monster.Herden.herdeSpawnen(ow, net.minecraft.world.phys.Vec3.atCenterOf(ow.getSharedSpawnPos()),
                            null, i == 2);
                    d.append(h == null ? "keine" : de.deadzonemc.monster.Herden.beschreiben(ow, h)).append(" | ");
                }
                LOG.info("[Selbsttest] Herden: {} (aktiv {})", d, de.deadzonemc.monster.Herden.anzahl());
                return;
            }
            if (befehl.startsWith("#fallen")) {
                ServerLevel ow = server.overworld();
                BlockPos sp = ow.getSharedSpawnPos();
                var bloecke = java.util.List.of(de.deadzonemc.falle.Fallen.STACHELDRAHT.get(),
                        de.deadzonemc.falle.Fallen.BAERENFALLE.get(), de.deadzonemc.falle.Fallen.LANDMINE.get());
                StringBuilder d = new StringBuilder();
                for (int i = 0; i < 3; i++) {
                    int x = sp.getX() - 20 + i * 4;
                    int z = sp.getZ() - 20;
                    int y = ow.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                    BlockPos pos = new BlockPos(x, y, z);
                    if (befehl.endsWith("pruefen")) {
                        // die Falle steht eine Ebene tiefer, falls der Boden sich nicht geändert hat
                        BlockPos f = ow.getBlockState(pos.below()).getBlock() == bloecke.get(i) ? pos.below() : pos;
                        d.append(bloecke.get(i).getDescriptionId()).append("=").append(ow.getBlockState(f)).append(" ");
                    } else {
                        ow.setBlock(pos.below(), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3);
                        ow.setBlock(pos, bloecke.get(i).defaultBlockState(), 3);
                        var w = de.deadzonemc.monster.MonsterTypen.WANDERER.get().create(ow);
                        if (w != null) {
                            w.moveTo(x + 0.5, y, z + 0.5);
                            w.setPersistenceRequired();
                            ow.addFreshEntity(w);
                        }
                        d.append(bloecke.get(i).getDescriptionId()).append(" bei ").append(pos.toShortString()).append(" ");
                    }
                }
                LOG.info("[Selbsttest] Fallen: {}", d);
                return;
            }
            if (befehl.equals("#station")) {
                ServerLevel ow = server.overworld();
                BlockPos sp = ow.getSharedSpawnPos();
                int x = 0;
                int y = 0;
                int z = 0;
                int versuche = 0;
                for (int[] o : new int[][]{{160, 40}, {-160, 60}, {60, 180}, {-80, -170}, {220, -120}, {-240, 200}, {300, 300}}) {
                    versuche++;
                    x = sp.getX() + o[0];
                    z = sp.getZ() + o[1];
                    for (int dx = -2; dx <= 2; dx++) {
                        for (int dz = -2; dz <= 2; dz++) {
                            ow.getChunk((x >> 4) + dx, (z >> 4) + dz);
                        }
                    }
                    y = ow.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                    try {
                        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),
                                "place structure deadzonemc:station_null " + x + " " + y + " " + z);
                    } catch (Exception e) {
                        LOG.error("[Selbsttest] Station Null nicht platzierbar", e);
                    }
                    // /place registriert keine Strukturdaten – also an den Blöcken erkennen (Leiter im Schacht)
                    boolean da = false;
                    for (BlockPos b : BlockPos.betweenClosed(x - 26, y - 60, z - 26, x + 26, y + 40, z + 26)) {
                        if (ow.getBlockState(b).is(net.minecraft.world.level.block.Blocks.LADDER)
                                && ow.getBlockState(b.below(20)).is(net.minecraft.world.level.block.Blocks.LADDER)) {
                            x = b.getX();
                            y = b.getY() + 10;
                            z = b.getZ();
                            da = true;
                            break;
                        }
                    }
                    if (da) {
                        break;
                    }
                }
                LOG.info("[Selbsttest] Station Null nach {} Versuchen", versuche);
                java.util.Map<String, Integer> zahl = new java.util.TreeMap<>();
                for (BlockPos b : BlockPos.betweenClosed(x - 30, y - 40, z - 30, x + 30, y + 30, z + 30)) {
                    var st = ow.getBlockState(b);
                    if (st.is(net.minecraft.world.level.block.Blocks.SPAWNER) || st.is(net.minecraft.world.level.block.Blocks.CHEST)
                            || st.is(net.minecraft.world.level.block.Blocks.BARREL) || st.is(net.minecraft.world.level.block.Blocks.LADDER)
                            || st.is(de.deadzonemc.zone.Zone.ANOMALIE.get()) || st.is(net.minecraft.world.level.block.Blocks.IRON_DOOR)) {
                        zahl.merge(st.getBlock().getDescriptionId().replace("block.", ""), 1, Integer::sum);
                    }
                }
                var start = de.deadzonemc.welt.station.StationNull.bei(ow, new BlockPos(x, y, z));
                BlockPos kern = de.deadzonemc.welt.station.StationNull.reaktor(ow, new BlockPos(x, y, z));
                LOG.info("[Selbsttest] Station Null bei {} {} {}: erkannt {}, Reaktor {}, Blöcke {}", x, y, z, start != null,
                        kern == null ? "-" : kern.toShortString(), zahl);
                return;
            }
            if (befehl.equals("#emission_schnell")) {
                de.deadzonemc.zone.Emission.holen(server.overworld()).restSetzen(40);
                return;
            }
            if (befehl.equals("#zone")) {
                ServerLevel ow = server.overworld();
                BlockPos sp = ow.getSharedSpawnPos();
                var e = de.deadzonemc.zone.Emission.holen(ow);
                java.util.Map<String, Integer> anomalien = new java.util.TreeMap<>();
                for (int cx = -5; cx <= 5; cx++) {
                    for (int cz = -5; cz <= 5; cz++) {
                        var ch = ow.getChunk((sp.getX() >> 4) + cx, (sp.getZ() >> 4) + cz);
                        for (var be : ch.getBlockEntities().values()) {
                            if (be instanceof de.deadzonemc.zone.AnomalieBlockEntity a) {
                                anomalien.merge(a.art().id(), 1, Integer::sum);
                            }
                        }
                    }
                }
                var bb = new net.minecraft.world.phys.AABB(sp).inflate(80, 200, 80);
                int kisten = ow.getEntitiesOfClass(de.deadzonemc.zone.Abwurfkiste.class, bb).size();
                int artefakte = ow.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, bb,
                        i -> i.getItem().getItem() instanceof de.deadzonemc.zone.ArtefaktItem).size();
                int faesser = 0;
                for (BlockPos b : BlockPos.betweenClosed(sp.offset(0, -10, 0), sp.offset(16, 40, 16))) {
                    if (ow.getBlockState(b).is(net.minecraft.world.level.block.Blocks.BARREL)) {
                        faesser++;
                    }
                }
                LOG.info("[Selbsttest] Zone: Anomalien {}, Emission Stufe {} (Rest {}, Nummer {}), Abwurfkisten in der Luft {}, "
                        + "gelandete Fässer {}, Artefakte am Boden {}, Herden {}", anomalien, e.stufe(), e.rest(),
                        de.deadzonemc.zone.Emission.nummer(ow), kisten, faesser, artefakte, de.deadzonemc.monster.Herden.anzahl());
                return;
            }
            if (befehl.equals("#zuflucht")) {
                ServerLevel ow = server.overworld();
                var tag = net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.STRUCTURE,
                        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(DeadzoneMC.MODID, "zuflucht"));
                long t0 = System.currentTimeMillis();
                BlockPos z = ow.findNearestMapStructure(tag, ow.getSharedSpawnPos(), 3, false);
                LOG.info("[Selbsttest] Zuflucht-Suche dauerte {} ms, Gründe [Stadt, Wasser, steil, ok] = {}",
                        System.currentTimeMillis() - t0, de.deadzonemc.zuflucht.ZufluchtStruktur.GRUENDE);
                if (z == null) {
                    LOG.info("[Selbsttest] Zuflucht: keine gefunden");
                    return;
                }
                for (int dx = -3; dx <= 3; dx++) {
                    for (int dz = -3; dz <= 3; dz++) {
                        ow.getChunk((z.getX() >> 4) + dx, (z.getZ() >> 4) + dz);
                    }
                }
                int boden = ow.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, z.getX(), z.getZ());
                z = new BlockPos(z.getX(), boden, z.getZ());
                var bb = new net.minecraft.world.phys.AABB(z).inflate(40, 30, 40);
                java.util.Map<String, Integer> zahl = new java.util.TreeMap<>();
                StringBuilder handel = new StringBuilder();
                for (var e : ow.getEntitiesOfClass(net.minecraft.world.entity.Mob.class, bb)) {
                    zahl.merge(e.getType().toShortString(), 1, Integer::sum);
                    if (e instanceof de.deadzonemc.mensch.Haendler h) {
                        handel.append(h.sortiment()).append("=").append(h.getOffers().size()).append(" ");
                    }
                }
                var start = de.deadzonemc.zuflucht.ZufluchtSchutz.bei(ow, z);
                int y = ow.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, z.getX(), z.getZ());
                LOG.info("[Selbsttest] Zuflucht bei {} / {} (Boden {}): Wesen {}, Händler {}, Schutz {}", z.getX(), z.getZ(), y,
                        zahl, handel, start != null ? de.deadzonemc.zuflucht.ZufluchtBau.name(
                                ((de.deadzonemc.zuflucht.ZufluchtPiece) start.getPieces().get(0)).saat()) : "fehlt");
                zufluchtOrt = z;
                truhen(server, z, "Zuflucht");
                stadtkarte(server, z.getX() - ow.getSharedSpawnPos().getX(), z.getZ() - ow.getSharedSpawnPos().getZ());
                return;
            }
            if (befehl.equals("#stadthorde")) {
                ServerLevel ow = server.overworld();
                BlockPos sp = ow.getSharedSpawnPos();
                int stadt = 0;
                int gesamt = 0;
                int treffer = 0;
                int geschaetztGesamt = 0;
                BlockPos fund = null;
                for (int cx = -15; cx <= 15; cx++) {
                    for (int cz = -15; cz <= 15; cz++) {
                        gesamt++;
                        int x = (sp.getX() >> 4) + cx;
                        int z = (sp.getZ() >> 4) + cz;
                        boolean geschaetzt = de.deadzonemc.welt.Staedte.vielleichtStadt(ow, x, z);
                        if (de.deadzonemc.welt.Staedte.istStadt(ow, x, z)) {
                            if (geschaetzt) {
                                treffer++;
                            }
                            stadt++;
                            if (fund == null || Math.abs(cx) + Math.abs(cz) < Math.abs((fund.getX() >> 4) - (sp.getX() >> 4)) + Math.abs((fund.getZ() >> 4) - (sp.getZ() >> 4))) {
                                fund = new BlockPos(x * 16 + 8, 0, z * 16 + 8);
                            }
                        }
                    }
                }
                for (int cx = -15; cx <= 15; cx++) {
                    for (int cz = -15; cz <= 15; cz++) {
                        if (de.deadzonemc.welt.Staedte.vielleichtStadt(ow, (sp.getX() >> 4) + cx, (sp.getZ() >> 4) + cz)) {
                            geschaetztGesamt++;
                        }
                    }
                }
                StringBuilder d = new StringBuilder("Stadt-Chunks um den Spawn: " + stadt + " von " + gesamt
                        + " (Schätzung: " + geschaetztGesamt + ", davon echt " + treffer + ")");
                if (fund != null) {
                    Integer h = de.deadzonemc.welt.Staedte.strassenhoehe(ow, fund.getX() >> 4, fund.getZ() >> 4);
                    d.append(", Straßenhöhe bei ").append(fund.getX()).append("/").append(fund.getZ()).append(": ").append(h);
                    for (int dx = -4; dx <= 4; dx++) {
                        for (int dz = -4; dz <= 4; dz++) {
                            ow.getChunk((fund.getX() >> 4) + dx, (fund.getZ() >> 4) + dz);
                        }
                    }
                    int n = de.deadzonemc.monster.StadtHorden.gruppeSpawnen(ow, fund.getX(), h == null ? 70 : h, fund.getZ(), null, 6);
                    d.append(", Gruppe gespawnt: ").append(n).append(" → ");
                    for (var m : ow.getEntitiesOfClass(de.deadzonemc.monster.DeadzoneMonster.class, new net.minecraft.world.phys.AABB(fund).inflate(64, 400, 64))) {
                        d.append(m.getType().toShortString()).append("@").append(m.blockPosition().toShortString()).append(" ");
                    }
                }
                LOG.info("[Selbsttest] Stadthorde: {}", d);
                return;
            }
            if (befehl.equals("#wasser")) {
                wasserMessen(server);
                return;
            }
            if (befehl.startsWith("#stadtkarte")) {
                String[] t = befehl.split(" ");
                stadtkarte(server, Integer.parseInt(t[1]), Integer.parseInt(t[2]));
                return;
            }
            if (befehl.endsWith("#monsterzahl")) {
                BlockPos sp = server.overworld().getSharedSpawnPos().offset(0, 10, 40);
                int n = server.overworld().getEntitiesOfClass(de.deadzonemc.monster.DeadzoneMonster.class,
                        new net.minecraft.world.phys.AABB(sp).inflate(20)).size();
                LOG.info("[Selbsttest] Monster im Basisbereich nach 4 s: {} (erwartet 0)", n);
                var basen = de.deadzonemc.basis.Basen.holen(server.overworld());
                StringBuilder d = new StringBuilder();
                basen.alle().forEach((pos, r) -> d.append(BlockPos.of(pos).toShortString()).append(" r=").append(r).append("; "));
                for (var m : server.overworld().getEntitiesOfClass(net.minecraft.world.entity.monster.Monster.class,
                        new net.minecraft.world.phys.AABB(sp).inflate(40))) {
                    d.append(m.getType().toShortString()).append(" bei ").append(m.blockPosition().toShortString())
                            .append(m.isPersistenceRequired() ? " fest" : "").append(m.isAlive() ? "" : " tot").append(", ");
                }
                d.append(" Zeit ").append(server.overworld().getGameTime());
                LOG.info("[Selbsttest] Basis-Diagnose: {}", d);
                return;
            }
            if (befehl.equals("#poi")) {
                ServerLevel ow = server.overworld();
                BlockPos sp = ow.getSharedSpawnPos();
                StringBuilder d = new StringBuilder();
                int i = 0;
                for (var art : de.deadzonemc.welt.feature.AbsturzFeature.Art.values()) {
                    int x = sp.getX() + 160 + i * 48;
                    int z = sp.getZ() - 160;
                    i++;
                    for (int cx = -1; cx <= 1; cx++) {
                        for (int cz = -1; cz <= 1; cz++) {
                            ow.getChunk((x >> 4) + cx, (z >> 4) + cz);
                        }
                    }
                    int y = ow.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, x, z);
                    de.deadzonemc.welt.feature.AbsturzFeature.testBauen(ow, ow.random, new BlockPos(x, y, z), art);
                    int kisten = 0;
                    for (BlockPos q : BlockPos.betweenClosed(x - 15, y - 3, z - 15, x + 15, y + 14, z + 15)) {
                        if (ow.getBlockEntity(q) instanceof net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity rc
                                && rc.getLootTable() != null) {
                            kisten++;
                        }
                    }
                    d.append(art).append(" bei ").append(x).append(" ").append(y).append(" ").append(z)
                            .append(": ").append(kisten).append(" Behälter mit Beute; ");
                    blockDump(ow, "poi_" + art.name().toLowerCase(java.util.Locale.ROOT), x, y, z, 17, 4, 16);
                }
                int j = 0;
                for (var art : de.deadzonemc.welt.feature.SiedlungFeature.Art.values()) {
                    int x = sp.getX() - 160 - j * 40;
                    int z = sp.getZ() + 160;
                    j++;
                    for (int cx = -1; cx <= 1; cx++) {
                        for (int cz = -1; cz <= 1; cz++) {
                            ow.getChunk((x >> 4) + cx, (z >> 4) + cz);
                        }
                    }
                    int y = ow.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, x, z);
                    de.deadzonemc.welt.feature.SiedlungFeature.testBauen(ow, ow.random, new BlockPos(x, y, z), art);
                    long leute = ow.getEntitiesOfClass(de.deadzonemc.fraktion.Fraktionsmitglied.class,
                            new net.minecraft.world.phys.AABB(x - 20, y - 20, z - 20, x + 20, y + 40, z + 20)).size();
                    d.append(art).append(": ").append(leute).append(" Bewohner; ");
                    blockDump(ow, "siedlung_" + art.name().toLowerCase(java.util.Locale.ROOT), x, y, z, 18, 4, 26);
                }
                // Orbitalstation probeweise hoch über der Oberwelt
                BlockPos st = new BlockPos(sp.getX() + 400, 250, sp.getZ() + 400);
                for (int cx = -2; cx <= 2; cx++) {
                    for (int cz = -2; cz <= 2; cz++) {
                        ow.getChunk((st.getX() >> 4) + cx, (st.getZ() >> 4) + cz);
                    }
                }
                de.deadzonemc.orbit.Orbitalstation.testBauen(ow, st);
                blockDump(ow, "orbitalstation", st.getX(), st.getY(), st.getZ(), 26, 2, 8);
                LOG.info("[Selbsttest] POIs: {}", d);
                return;
            }
            if (befehl.equals("#versteck")) {
                ServerLevel ow = server.overworld();
                BlockPos sp = ow.getSharedSpawnPos();
                StringBuilder d = new StringBuilder();
                int i = 0;
                for (BlockPos v : de.deadzonemc.welt.Verstecke.umkreis(ow.getSeed(), sp.getX(), sp.getZ(), 1)) {
                    if (i++ >= 3) {
                        break;
                    }
                    ow.getChunk(v.getX() >> 4, v.getZ() >> 4);
                    String fund = "nichts";
                    for (int y = ow.getMaxBuildHeight() - 1; y > ow.getMinBuildHeight(); y--) {
                        var b = ow.getBlockState(new BlockPos(v.getX(), y, v.getZ()));
                        if (b.is(net.minecraft.world.level.block.Blocks.CHEST) || b.is(net.minecraft.world.level.block.Blocks.BARREL)) {
                            fund = BuiltInRegistries.BLOCK.getKey(b.getBlock()).getPath() + " bei y=" + y;
                            break;
                        }
                    }
                    d.append(v.getX()).append("/").append(v.getZ()).append(": ").append(fund).append("; ");
                }
                LOG.info("[Selbsttest] Verstecke: {}", d);
                return;
            }
            if (befehl.equals("#waffen")) {
                waffenPruefen(server);
                return;
            }
            if (befehl.equals("#monster")) {
                StringBuilder b = new StringBuilder();
                for (var e : server.overworld().getAllEntities()) {
                    if (e instanceof de.deadzonemc.monster.DeadzoneMonster m) {
                        b.append(net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(m.getType()).getPath())
                                .append(m.isAlive() ? " lebt" : " tot").append(String.format(" (%.0f HP), ", m.getHealth()));
                    }
                }
                LOG.info("[Selbsttest] Monster nach 9 s: {}", b);
                return;
            }
            if (befehl.equals("#uebersicht")) {
                uebersicht(server);
                return;
            }
            if (befehl.equals("#karte")) {
                karte(server);
                return;
            }
            if (befehl.equals("#zaehlen")) {
                zaehlen(server);
                truhen(server, mitte(server), "Stadt");
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
