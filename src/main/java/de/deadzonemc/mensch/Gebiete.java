package de.deadzonemc.mensch;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Wandernde Fraktionsgebiete: Die Oberwelt ist in Gebiete von 512 × 512 Blöcken geteilt. Jedes gehört der Wacht,
 * den Aschekindern oder niemandem – und jede Spielwoche (7 Tage) verschieben sich die Grenzen. Im Gebiet der Wacht
 * patrouillieren ihre Soldaten auch tagsüber häufig, im Gebiet der Aschekinder ziehen Kultisten sogar bei Tag umher.
 * Wer eine Grenze überquert, sieht es oben am Bildschirm; die Funkstation meldet, wem das Gebiet gerade gehört.
 */
public final class Gebiete {

    public static final int GROESSE = 512;
    private static final Map<UUID, String> ZULETZT = new HashMap<>();

    private Gebiete() {
    }

    /** Wem gehört das Gebiet an dieser Stelle gerade? null = Niemandsland. */
    public static Fraktion herr(ServerLevel w, double x, double z) {
        if (w.dimension() != Level.OVERWORLD) {
            return null;
        }
        long woche = w.getServer().overworld().getDayTime() / (24000L * 7);
        long gx = Math.floorDiv((long) x, GROESSE);
        long gz = Math.floorDiv((long) z, GROESSE);
        long h = w.getSeed() ^ (gx * 341873128712L) ^ (gz * 132897987541L) ^ (woche * 42317861L);
        h ^= h >>> 33;
        h *= 0xff51afd7ed558ccdL;
        h ^= h >>> 33;
        int v = (int) Math.floorMod(h, 100L);
        return v < 35 ? Fraktion.WACHT : v < 60 ? Fraktion.ASCHEKINDER : null;
    }

    public static Component name(Fraktion f) {
        return Component.translatable(f == null ? "gebiet.deadzonemc.niemandsland" : "gebiet.deadzonemc." + f.id());
    }

    /** Grenzübertritt melden. */
    public static void tick(ServerTickEvent.Post event) {
        if (event.getServer().getTickCount() % 40 != 13) {
            return;
        }
        ServerLevel w = event.getServer().overworld();
        for (ServerPlayer p : w.players()) {
            Fraktion f = herr(w, p.getX(), p.getZ());
            String id = f == null ? "-" : f.id();
            String alt = ZULETZT.put(p.getUUID(), id);
            if (alt != null && !alt.equals(id)) {
                ChatFormatting farbe = f == Fraktion.WACHT ? ChatFormatting.AQUA : f == Fraktion.ASCHEKINDER ? ChatFormatting.DARK_RED
                        : ChatFormatting.GRAY;
                p.displayClientMessage(Component.translatable("gebiet.deadzonemc.betreten", name(f)).withStyle(farbe), true);
            }
        }
    }
}
