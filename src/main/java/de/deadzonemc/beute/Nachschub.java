package de.deadzonemc.beute;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Nachschub: Die Welt bleibt endlos spielbar. Jeder Behälter mit Beutetabelle, den ein Spieler öffnet, wird
 * vorgemerkt. Nach {@link #TAGE} Spieltagen füllt er sich neu – aber nur, wenn er (fast) leer ist und gerade
 * niemand in der Nähe ist (Überlebende haben die Gegend inzwischen neu durchwühlt und Vorräte versteckt).
 */
public final class Nachschub extends SavedData {

    public static final int TAGE = 5;
    private static final int MAX = 30000;
    private static final Factory<Nachschub> FABRIK = new Factory<>(Nachschub::new, Nachschub::laden, null);

    private record Eintrag(ResourceKey<Level> welt, BlockPos pos, ResourceKey<LootTable> tabelle, long zeit) {
    }

    private final Map<String, Eintrag> eintraege = new LinkedHashMap<>();

    private static Nachschub von(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FABRIK, "deadzonemc_nachschub");
    }

    public static void angeklickt(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        BlockEntity be = level.getBlockEntity(event.getPos());
        if (be instanceof RandomizableContainerBlockEntity rc && rc.getLootTable() != null) {
            Nachschub n = von(level.getServer());
            String k = level.dimension().location() + "|" + event.getPos().asLong();
            n.eintraege.remove(k);
            n.eintraege.put(k, new Eintrag(level.dimension(), event.getPos().immutable(), rc.getLootTable(), level.getGameTime()));
            while (n.eintraege.size() > MAX) {
                Iterator<String> it = n.eintraege.keySet().iterator();
                it.next();
                it.remove();
            }
            n.setDirty();
        }
    }

    public static void tick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % 1200 != 600) {
            return;
        }
        Nachschub n = von(server);
        long jetzt = server.overworld().getGameTime();
        int geprueft = 0;
        Iterator<Map.Entry<String, Eintrag>> it = n.eintraege.entrySet().iterator();
        while (it.hasNext() && geprueft < 400) {
            Eintrag e = it.next().getValue();
            if (jetzt - e.zeit < TAGE * 24000L) {
                break; // geordnet nach Zeit – alle weiteren sind jünger
            }
            geprueft++;
            ServerLevel level = server.getLevel(e.welt);
            if (level == null) {
                it.remove();
                continue;
            }
            if (!level.isLoaded(e.pos) || level.hasNearbyAlivePlayer(e.pos.getX(), e.pos.getY(), e.pos.getZ(), 48)) {
                continue;
            }
            BlockEntity be = level.getBlockEntity(e.pos);
            if (be instanceof TresorBlockEntity t) {
                t.auffuellen(level.random.nextLong());
            } else if (be instanceof RandomizableContainerBlockEntity rc && rc.getLootTable() == null) {
                int belegt = 0;
                for (int i = 0; i < rc.getContainerSize(); i++) {
                    if (!rc.getItem(i).isEmpty()) {
                        belegt++;
                    }
                }
                if (belegt <= 2) {
                    rc.setLootTable(e.tabelle, level.random.nextLong());
                    rc.setChanged();
                }
            }
            it.remove();
            n.setDirty();
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider p) {
        ListTag liste = new ListTag();
        for (Eintrag e : eintraege.values()) {
            CompoundTag t = new CompoundTag();
            t.putString("w", e.welt.location().toString());
            t.putLong("p", e.pos.asLong());
            t.putString("t", e.tabelle.location().toString());
            t.putLong("z", e.zeit);
            liste.add(t);
        }
        tag.put("e", liste);
        return tag;
    }

    private static Nachschub laden(CompoundTag tag, HolderLookup.Provider p) {
        Nachschub n = new Nachschub();
        for (Tag x : tag.getList("e", Tag.TAG_COMPOUND)) {
            CompoundTag t = (CompoundTag) x;
            ResourceKey<Level> w = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(t.getString("w")));
            BlockPos pos = BlockPos.of(t.getLong("p"));
            n.eintraege.put(w.location() + "|" + pos.asLong(), new Eintrag(w, pos,
                    ResourceKey.create(Registries.LOOT_TABLE, ResourceLocation.parse(t.getString("t"))), t.getLong("z")));
        }
        return n;
    }

    @SuppressWarnings("unused")
    private static boolean leer(ItemStack s) {
        return s.isEmpty();
    }
}
