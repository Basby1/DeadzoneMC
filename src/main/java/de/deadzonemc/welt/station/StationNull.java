package de.deadzonemc.welt.station;

import de.deadzonemc.DeadzoneMC;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;

import javax.annotation.Nullable;

/** Abfragen rund um Station Null (für Boss, Aufträge und Funkhinweise). */
public final class StationNull {

    public static final ResourceKey<Structure> STRUKTUR = ResourceKey.create(Registries.STRUCTURE,
            ResourceLocation.fromNamespaceAndPath(DeadzoneMC.MODID, "station_null"));
    public static final TagKey<Structure> TAG = TagKey.create(Registries.STRUCTURE,
            ResourceLocation.fromNamespaceAndPath(DeadzoneMC.MODID, "station_null"));

    private StationNull() {
    }

    @Nullable
    public static StructureStart bei(ServerLevel level, BlockPos pos) {
        if (level.dimension() != Level.OVERWORLD || !level.hasChunkAt(pos)) {
            return null;
        }
        Structure s = level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(STRUKTUR);
        if (s == null) {
            return null;
        }
        StructureStart start = level.structureManager().getStructureAt(pos, s);
        return start.isValid() ? start : null;
    }

    /** Ob die Position unten im Labor liegt (nicht an der Oberfläche). */
    public static boolean imLabor(ServerLevel level, BlockPos pos) {
        StructureStart s = bei(level, pos);
        if (s == null || s.getPieces().isEmpty() || !(s.getPieces().get(0) instanceof StationNullPiece p)) {
            return false;
        }
        return pos.getY() < p.oberflaeche() - 12;
    }

    /** Weltposition der Reaktormitte dieser Station. */
    @Nullable
    public static BlockPos reaktor(ServerLevel level, BlockPos irgendwo) {
        StructureStart s = bei(level, irgendwo);
        if (s == null || s.getPieces().isEmpty() || !(s.getPieces().get(0) instanceof StationNullPiece p)) {
            return null;
        }
        return p.lokal(level, StationNullBau.reaktorMitte());
    }

    /** Ob die Position in der Reaktorhalle liegt. */
    public static boolean imReaktor(ServerLevel level, BlockPos pos) {
        BlockPos r = reaktor(level, pos);
        return r != null && r.distSqr(pos) < 12 * 12 && Math.abs(r.getY() - pos.getY()) < 6;
    }
}
