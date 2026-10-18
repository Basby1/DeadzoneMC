package de.deadzonemc.geschichte;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;

/** Fortschritt der Hauptgeschichte – gemeinsam für die ganze Gruppe (eine Welt, eine Geschichte). */
public class Fortschritt extends SavedData {

    private static final Factory<Fortschritt> FABRIK = new Factory<>(Fortschritt::new, Fortschritt::laden, null);

    /** Index des aktuellen Schritts in {@link Geschichte#SCHRITTE}. */
    int schritt;
    /** Ob der aktuelle Schritt schon gestartet wurde (Marker gesetzt, Gegner platziert …). */
    boolean gestartet;
    final Set<String> merker = new HashSet<>();
    @Nullable
    BlockPos ziel;
    /** Dimension des Ziels: 0 Oberwelt, 1 Nether, 2 End. */
    int zielWelt;
    long letzterSpawn = -100000;

    public static Fortschritt von(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FABRIK, "deadzonemc_geschichte");
    }

    public int schritt() {
        return schritt;
    }

    public boolean hat(String m) {
        return merker.contains(m);
    }

    public void merken(String m) {
        if (merker.add(m)) {
            setDirty();
        }
    }

    @Nullable
    public BlockPos ziel() {
        return ziel;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider p) {
        tag.putInt("Schritt", schritt);
        tag.putBoolean("Gestartet", gestartet);
        ListTag l = new ListTag();
        for (String m : merker) {
            l.add(StringTag.valueOf(m));
        }
        tag.put("Merker", l);
        if (ziel != null) {
            tag.putLong("Ziel", ziel.asLong());
        }
        tag.putInt("ZielWelt", zielWelt);
        return tag;
    }

    private static Fortschritt laden(CompoundTag tag, HolderLookup.Provider p) {
        Fortschritt f = new Fortschritt();
        f.schritt = tag.getInt("Schritt");
        f.gestartet = tag.getBoolean("Gestartet");
        for (Tag t : tag.getList("Merker", Tag.TAG_STRING)) {
            f.merker.add(t.getAsString());
        }
        if (tag.contains("Ziel")) {
            f.ziel = BlockPos.of(tag.getLong("Ziel"));
        }
        f.zielWelt = tag.getInt("ZielWelt");
        return f;
    }
}
