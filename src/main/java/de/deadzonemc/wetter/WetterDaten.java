package de.deadzonemc.wetter;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.saveddata.SavedData;

/** Speichert in der Welt, welches Wetter gerade herrscht und wie lange noch. */
public class WetterDaten extends SavedData {

    private static final SavedData.Factory<WetterDaten> FABRIK =
            new SavedData.Factory<>(WetterDaten::new, WetterDaten::laden, null);

    private WetterArt art = WetterArt.LEICHTER_NEBEL;
    private int rest = 12_000;
    private boolean nebel = true;

    public static WetterDaten holen(ServerLevel oberwelt) {
        return oberwelt.getDataStorage().computeIfAbsent(FABRIK, "deadzonemc_wetter");
    }

    private static WetterDaten laden(CompoundTag tag, HolderLookup.Provider provider) {
        WetterDaten daten = new WetterDaten();
        WetterArt gespeichert = WetterArt.ausId(tag.getString("art"));
        if (gespeichert != null) {
            daten.art = gespeichert;
        }
        daten.rest = Math.max(20, tag.getInt("rest"));
        daten.nebel = !tag.contains("nebel") || tag.getBoolean("nebel");
        return daten;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        tag.putString("art", art.id());
        tag.putInt("rest", rest);
        tag.putBoolean("nebel", nebel);
        return tag;
    }

    public WetterArt art() {
        return art;
    }

    /** Ob der Nebel angezeigt wird (zum Testen abschaltbar). */
    public boolean nebel() {
        return nebel;
    }

    public void nebelSetzen(boolean an) {
        nebel = an;
        setDirty();
    }

    public int rest() {
        return rest;
    }

    /**
     * Zählt die Dauer herunter.
     *
     * @return {@code true}, wenn das Wetter gerade umgeschlagen ist
     */
    public boolean ticken(RandomSource zufall) {
        rest--;
        if (rest % 200 == 0) {
            setDirty();
        }
        if (rest > 0) {
            return false;
        }
        setzen(naechstes(zufall), -1, zufall);
        return true;
    }

    /** Setzt ein bestimmtes Wetter; eine Dauer unter 1 bedeutet "zufällige Dauer". */
    public void setzen(WetterArt neu, int dauer, RandomSource zufall) {
        art = neu;
        rest = dauer > 0 ? dauer : neu.minDauer() + zufall.nextInt(neu.maxDauer() - neu.minDauer() + 1);
        setDirty();
    }

    private WetterArt naechstes(RandomSource zufall) {
        int summe = 0;
        for (WetterArt kandidat : WetterArt.values()) {
            if (kandidat != art) {
                summe += kandidat.gewicht();
            }
        }
        int wurf = zufall.nextInt(summe);
        for (WetterArt kandidat : WetterArt.values()) {
            if (kandidat == art) {
                continue;
            }
            wurf -= kandidat.gewicht();
            if (wurf < 0) {
                return kandidat;
            }
        }
        return WetterArt.LEICHTER_NEBEL;
    }
}
