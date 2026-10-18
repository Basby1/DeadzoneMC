package de.deadzonemc.welt;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import org.slf4j.Logger;

/**
 * Startgebiet einer neuen Welt: am Rand der nächsten Stadt, aber draußen. Man baut sich im Umland etwas auf und
 * zieht zum Plündern in die Stadt – die Silhouette ist vom Start aus schon zu ahnen.
 */
public final class StartGebiet extends SavedData {

    private static final Logger LOG = LogUtils.getLogger();
    private static final Factory<StartGebiet> FABRIK = new Factory<>(StartGebiet::new, StartGebiet::laden, null);
    private boolean erledigt;

    private static StartGebiet laden(CompoundTag tag, HolderLookup.Provider p) {
        StartGebiet s = new StartGebiet();
        s.erledigt = tag.getBoolean("Erledigt");
        return s;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider p) {
        tag.putBoolean("Erledigt", erledigt);
        return tag;
    }

    public static void serverGestartet(ServerStartedEvent event) {
        ServerLevel welt = event.getServer().overworld();
        StartGebiet s = welt.getDataStorage().computeIfAbsent(FABRIK, "deadzonemc_startgebiet");
        if (s.erledigt) {
            return;
        }
        s.erledigt = true;
        s.setDirty();
        BlockPos spawn = welt.getSharedSpawnPos();
        int cx0 = spawn.getX() >> 4;
        int cz0 = spawn.getZ() >> 4;
        if (Staedte.istStadt(welt, cx0, cz0)) {
            // Schon in der Stadt: nach draußen gehen
            BlockPos aussen = nachDraussen(welt, cx0, cz0, 1, 0);
            if (aussen != null) {
                setzen(welt, aussen);
            }
            return;
        }
        // Nächste Stadt suchen (Ringe im Abstand von 4 Chunks, bis ca. 2,5 km)
        for (int r = 4; r <= 160; r += 4) {
            for (int i = 0; i < r * 2; i++) {
                double w = Math.PI * i / r;
                int cx = cx0 + (int) Math.round(Math.cos(w) * r);
                int cz = cz0 + (int) Math.round(Math.sin(w) * r);
                if (Staedte.vielleichtStadt(welt, cx, cz) && Staedte.istStadt(welt, cx, cz)) {
                    // Von der Stadt aus zurück Richtung alter Spawn, bis man draußen ist
                    BlockPos aussen = nachDraussen(welt, cx, cz, cx0 - cx, cz0 - cz);
                    if (aussen != null) {
                        setzen(welt, aussen);
                        LOG.info("[Deadzone] Startgebiet am Rand der Stadt bei Chunk {}/{} → {}", cx, cz, aussen.toShortString());
                        return;
                    }
                }
            }
        }
        LOG.info("[Deadzone] Keine Stadt in der Nähe des Spawns gefunden – Startgebiet bleibt");
    }

    /** Geht von einem Stadt-Chunk in Richtung (dx, dz), bis vier Chunks am Stück keine Stadt mehr sind. */
    private static BlockPos nachDraussen(ServerLevel welt, int cx, int cz, double dx, double dz) {
        double len = Math.sqrt(dx * dx + dz * dz);
        if (len < 0.001) {
            dx = 1;
            dz = 0;
            len = 1;
        }
        dx /= len;
        dz /= len;
        int frei = 0;
        for (int s = 1; s < 60; s++) {
            int x = cx + (int) Math.round(dx * s);
            int z = cz + (int) Math.round(dz * s);
            frei = Staedte.istStadt(welt, x, z) ? 0 : frei + 1;
            if (frei >= 4) {
                int bx = (x << 4) + 8;
                int bz = (z << 4) + 8;
                welt.getChunk(x, z);
                int y = welt.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, bx, bz);
                return new BlockPos(bx, y, bz);
            }
        }
        return null;
    }

    private static void setzen(ServerLevel welt, BlockPos pos) {
        welt.setDefaultSpawnPos(pos, 0.0F);
    }
}
