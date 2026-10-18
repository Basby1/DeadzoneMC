package de.deadzonemc.zone;

import de.deadzonemc.zuflucht.ZufluchtSchutz;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Versorgungsabwürfe: alle ein bis drei Tage meldet sich ein Funkspruch – ein Flugzeug hat irgendwo in der Nähe
 * eine Kiste mit Militärausrüstung abgeworfen. Wer zuerst da ist, gewinnt. Lärm und Rauch locken eine Herde an.
 */
public final class Luftabwurf extends SavedData {

    private static final Factory<Luftabwurf> FABRIK = new Factory<>(Luftabwurf::new, Luftabwurf::laden, null);

    /** Angekündigt, aber noch nicht gelandet: x, z, Zeitpunkt der Ankündigung. */
    private final List<long[]> offen = new ArrayList<>();
    private long naechster = -1;

    public static Luftabwurf holen(ServerLevel oberwelt) {
        return oberwelt.getDataStorage().computeIfAbsent(FABRIK, "deadzonemc_luftabwurf");
    }

    private static Luftabwurf laden(CompoundTag tag, HolderLookup.Provider p) {
        Luftabwurf l = new Luftabwurf();
        l.naechster = tag.contains("Naechster") ? tag.getLong("Naechster") : -1;
        for (Tag t : tag.getList("Offen", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) t;
            l.offen.add(new long[]{c.getInt("X"), c.getInt("Z"), c.getLong("Zeit")});
        }
        return l;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider p) {
        tag.putLong("Naechster", naechster);
        ListTag liste = new ListTag();
        for (long[] o : offen) {
            CompoundTag c = new CompoundTag();
            c.putInt("X", (int) o[0]);
            c.putInt("Z", (int) o[1]);
            c.putLong("Zeit", o[2]);
            liste.add(c);
        }
        tag.put("Offen", liste);
        return tag;
    }

    public static void tick(ServerTickEvent.Post event) {
        ServerLevel welt = event.getServer().overworld();
        long zeit = welt.getGameTime();
        if (zeit % 100 != 57) {
            return;
        }
        Luftabwurf l = holen(welt);
        if (l.naechster < 0) {
            l.naechster = zeit + 30000L + welt.random.nextInt(30000);
            l.setDirty();
        }
        if (zeit >= l.naechster && !welt.players().isEmpty() && Emission.stufe(welt) == 0) {
            ServerPlayer p = welt.players().get(welt.random.nextInt(welt.players().size()));
            l.ankuendigen(welt, p);
            l.naechster = zeit + 36000L + welt.random.nextInt(48000);
        }
        l.offen.removeIf(o -> {
            BlockPos pos = new BlockPos((int) o[0], 0, (int) o[1]);
            if (zeit - o[2] > 96000L) {
                return true; // längst geplündert oder verschollen
            }
            if (!welt.hasChunkAt(pos) || zeit - o[2] < 400) {
                return false;
            }
            BlockPos boden = welt.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos);
            if (ZufluchtSchutz.sicher(welt, boden)) {
                return true;
            }
            if (zeit - o[2] < 3000L) {
                abwerfen(welt, boden);
            } else {
                Abwurfkiste.aufsetzen(welt, boden);
            }
            return true;
        });
        l.setDirty();
    }

    /** Wählt einen Ort 110–240 Blöcke vom Spieler entfernt und meldet ihn per Funk (ungefähr). */
    public BlockPos ankuendigen(ServerLevel welt, ServerPlayer p) {
        double w = welt.random.nextDouble() * Math.PI * 2;
        double r = 110 + welt.random.nextDouble() * 130;
        int x = (int) (p.getX() + Math.cos(w) * r);
        int z = (int) (p.getZ() + Math.sin(w) * r);
        offen.add(new long[]{x, z, welt.getGameTime()});
        setDirty();
        int gx = Math.round(x / 25.0F) * 25;
        int gz = Math.round(z / 25.0F) * 25;
        Component meldung = Component.translatable("zone.deadzonemc.abwurf.funk", gx, gz).withStyle(ChatFormatting.GOLD);
        for (ServerPlayer s : welt.players()) {
            s.sendSystemMessage(meldung);
            s.playNotifySound(SoundEvents.NOTE_BLOCK_BIT.value(), SoundSource.PLAYERS, 0.7F, 0.8F);
        }
        return new BlockPos(x, 0, z);
    }

    /** Lässt die Kiste hoch über dem Ort am Fallschirm erscheinen. */
    public static void abwerfen(ServerLevel welt, BlockPos boden) {
        Abwurfkiste kiste = Zone.ABWURFKISTE.get().create(welt);
        if (kiste == null) {
            Abwurfkiste.aufsetzen(welt, boden);
            return;
        }
        kiste.moveTo(boden.getX() + 0.5, Math.min(welt.getMaxBuildHeight() - 4, boden.getY() + 70), boden.getZ() + 0.5);
        welt.addFreshEntity(kiste);
        welt.playSound(null, kiste.getX(), kiste.getY(), kiste.getZ(), SoundEvents.ELYTRA_FLYING, SoundSource.AMBIENT, 6.0F, 0.4F);
    }

    public int offen() {
        return offen.size();
    }
}
