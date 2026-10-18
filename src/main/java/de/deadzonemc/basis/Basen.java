package de.deadzonemc.basis;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.LinkedHashMap;
import java.util.Map;

/** Alle Basen (Notstromgeneratoren) einer Dimension mit ihrem Radius, dazu die Sicherheitsstufe. */
public class Basen extends SavedData {

    private static final SavedData.Factory<Basen> FABRIK = new SavedData.Factory<>(Basen::new, Basen::laden, null);

    private final Map<Long, Integer> radien = new LinkedHashMap<>();
    private Sicherheit stufe = Sicherheit.BELAGERUNG;

    public static Basen holen(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(FABRIK, "deadzonemc_basen");
    }

    /** Die Sicherheitsstufe gilt für die ganze Welt und steht in der Oberwelt. */
    public static Sicherheit stufe(ServerLevel level) {
        return holen(level.getServer().overworld()).stufe;
    }

    public static void stufeSetzen(ServerLevel level, Sicherheit s) {
        Basen b = holen(level.getServer().overworld());
        b.stufe = s;
        b.setDirty();
    }

    private static Basen laden(CompoundTag tag, HolderLookup.Provider provider) {
        Basen b = new Basen();
        ListTag liste = tag.getList("basen", Tag.TAG_COMPOUND);
        for (int i = 0; i < liste.size(); i++) {
            CompoundTag e = liste.getCompound(i);
            int r = e.getInt("radius");
            // Alte Spielstände: die Schutzzonen sind seit 18.10. viermal so groß
            r = r == 24 ? 96 : r == 40 ? 160 : r == 64 ? 256 : r;
            b.radien.put(e.getLong("pos"), r);
        }
        Sicherheit s = Sicherheit.ausId(tag.getString("stufe"));
        if (s != null) {
            b.stufe = s;
        }
        return b;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        ListTag liste = new ListTag();
        radien.forEach((pos, r) -> {
            CompoundTag e = new CompoundTag();
            e.putLong("pos", pos);
            e.putInt("radius", r);
            liste.add(e);
        });
        tag.put("basen", liste);
        tag.putString("stufe", stufe.id());
        return tag;
    }

    public void setzen(BlockPos pos, int radius) {
        radien.put(pos.asLong(), radius);
        setDirty();
    }

    public void entfernen(BlockPos pos) {
        if (radien.remove(pos.asLong()) != null) {
            setDirty();
        }
    }

    public Map<Long, Integer> alle() {
        return radien;
    }

    /** Mittelpunkt der Basis, in deren Bereich die Position liegt (zylindrisch), oder null. */
    public BlockPos basisBei(double x, double z, int extra) {
        for (var e : radien.entrySet()) {
            BlockPos p = BlockPos.of(e.getKey());
            double dx = x - (p.getX() + 0.5);
            double dz = z - (p.getZ() + 0.5);
            int r = e.getValue() + extra;
            if (dx * dx + dz * dz <= (double) r * r) {
                return p;
            }
        }
        return null;
    }
}
