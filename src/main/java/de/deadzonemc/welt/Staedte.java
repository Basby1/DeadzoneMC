package de.deadzonemc.welt;

import com.mojang.logging.LogUtils;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.neoforged.fml.InterModComms;
import net.neoforged.fml.ModList;
import net.neoforged.fml.event.lifecycle.InterModEnqueueEvent;
import org.slf4j.Logger;

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Fragt bei The Lost Cities nach, ob ein Chunk zu einer Stadt gehört (über die offizielle IMC-Schnittstelle,
 * ohne harte Abhängigkeit – ohne Lost Cities gibt es einfach keine Städte).
 */
public final class Staedte {

    private static final Logger LOG = LogUtils.getLogger();
    private static volatile Object lostCities;
    private static Method getLostInfo;
    private static Method getChunkInfo;
    private static Method isCity;
    private static Method getCityLevel;
    private static Method getRealHeight;
    private static boolean kaputt;

    /** Zwischenspeicher: Chunk → Straßenhöhe (Integer.MIN_VALUE = keine Stadt). */
    private static final ConcurrentHashMap<Long, Integer> CACHE = new ConcurrentHashMap<>();

    private Staedte() {
    }

    public static void imc(InterModEnqueueEvent event) {
        if (!ModList.get().isLoaded("lostcities")) {
            return;
        }
        InterModComms.sendTo("lostcities", "getLostCities", () -> (Function<Object, Void>) lc -> {
            lostCities = lc;
            return null;
        });
    }

    private static boolean bereit() {
        if (lostCities == null || kaputt) {
            return false;
        }
        if (getLostInfo == null) {
            try {
                Class<?> api = Class.forName("mcjty.lostcities.api.ILostCities");
                Class<?> info = Class.forName("mcjty.lostcities.api.ILostCityInformation");
                Class<?> chunk = Class.forName("mcjty.lostcities.api.ILostChunkInfo");
                getChunkInfo = info.getMethod("getChunkInfo", int.class, int.class);
                getRealHeight = info.getMethod("getRealHeight", int.class);
                isCity = chunk.getMethod("isCity");
                getCityLevel = chunk.getMethod("getCityLevel");
                getLostInfo = api.getMethod("getLostInfo", Level.class);
            } catch (ReflectiveOperationException e) {
                LOG.warn("Lost-Cities-Schnittstelle nicht nutzbar: {}", e.toString());
                kaputt = true;
                return false;
            }
        }
        return true;
    }

    /** Straßenhöhe der Stadt in diesem Chunk, oder {@code null}, wenn hier keine Stadt ist. */
    public static Integer strassenhoehe(Level level, int chunkX, int chunkZ) {
        if (level.dimension() != Level.OVERWORLD || !bereit()) {
            return null;
        }
        long schluessel = ChunkPos.asLong(chunkX, chunkZ);
        Integer h = CACHE.get(schluessel);
        if (h == null) {
            h = Integer.MIN_VALUE;
            try {
                Object info = getLostInfo.invoke(lostCities, level);
                if (info != null) {
                    Object chunk = getChunkInfo.invoke(info, chunkX, chunkZ);
                    if (chunk != null && (Boolean) isCity.invoke(chunk)) {
                        h = (Integer) getRealHeight.invoke(info, (Integer) getCityLevel.invoke(chunk)) + 1;
                    }
                }
            } catch (ReflectiveOperationException | RuntimeException e) {
                LOG.warn("Lost-Cities-Abfrage fehlgeschlagen: {}", e.toString());
                kaputt = true;
                return null;
            }
            if (CACHE.size() > 20000) {
                CACHE.clear();
            }
            CACHE.put(schluessel, h);
        }
        return h == Integer.MIN_VALUE ? null : h;
    }

    // ---- Schnelle Schätzung (nur Stadtradien, ohne Gelände zu berechnen) -------------------------

    private static volatile boolean faktorKaputt;
    private static Method cityFactor;
    private static java.lang.reflect.Constructor<?> chunkCoord;
    private static Method profilHolen;
    private static java.lang.reflect.Field providerFeld;
    private static java.lang.reflect.Field schwelleFeld;

    /**
     * Ob hier wahrscheinlich eine Stadt liegt – nur aus den Stadtzentren und Radien von Lost Cities, ohne das
     * Gelände zu berechnen. Billig und für die Weltgenerierung geeignet (die genaue Abfrage kostet pro Chunk
     * eine komplette Geländeberechnung und bremst die Strukturplatzierung extrem aus).
     */
    public static boolean vielleichtStadt(Level level, int chunkX, int chunkZ) {
        if (level.dimension() != Level.OVERWORLD || !bereit() || faktorKaputt) {
            return false;
        }
        try {
            Object info = getLostInfo.invoke(lostCities, level);
            if (info == null) {
                return false;
            }
            if (cityFactor == null) {
                Class<?> dim = Class.forName("mcjty.lostcities.worldgen.IDimensionInfo");
                Class<?> coord = Class.forName("mcjty.lostcities.varia.ChunkCoord");
                Class<?> profil = Class.forName("mcjty.lostcities.config.LostCityProfile");
                Class<?> city = Class.forName("mcjty.lostcities.worldgen.lost.City");
                cityFactor = city.getMethod("getCityFactor", coord, dim, profil);
                chunkCoord = coord.getConstructor(net.minecraft.resources.ResourceKey.class, int.class, int.class);
                profilHolen = dim.getMethod("getProfile");
                for (Class<?> k = info.getClass(); k != null && providerFeld == null; k = k.getSuperclass()) {
                    for (java.lang.reflect.Field f : k.getDeclaredFields()) {
                        if (dim.isAssignableFrom(f.getType())) {
                            f.setAccessible(true);
                            providerFeld = f;
                            break;
                        }
                    }
                }
                try {
                    schwelleFeld = profil.getField("CITY_THRESHOLD");
                } catch (NoSuchFieldException e) {
                    schwelleFeld = null;
                }
                if (providerFeld == null) {
                    throw new NoSuchFieldException("IDimensionInfo in " + info.getClass());
                }
            }
            Object provider = providerFeld.get(info);
            Object profil = profilHolen.invoke(provider);
            Object c = chunkCoord.newInstance(level.dimension(), chunkX, chunkZ);
            float faktor = (Float) cityFactor.invoke(null, c, provider, profil);
            double schwelle = schwelleFeld == null ? 0.15 : ((Number) schwelleFeld.get(profil)).doubleValue();
            return faktor > schwelle;
        } catch (ReflectiveOperationException | RuntimeException e) {
            LOG.warn("Lost-Cities-Stadtschätzung nicht nutzbar: {}", e.toString());
            faktorKaputt = true;
            return false;
        }
    }

    public static boolean istStadt(Level level, int chunkX, int chunkZ) {
        return strassenhoehe(level, chunkX, chunkZ) != null;
    }

    /** Wird beim Serverstart geleert (andere Welt, anderer Seed). */
    public static void leeren() {
        CACHE.clear();
    }
}
