package de.deadzonemc.zone;

import de.deadzonemc.mensch.MenschTypen;
import de.deadzonemc.mensch.Ueberlebender;
import de.deadzonemc.monster.DeadzoneMonster;
import de.deadzonemc.monster.MonsterTypen;
import de.deadzonemc.welt.feature.AbsturzFeature;
import de.deadzonemc.zuflucht.ZufluchtSchutz;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Dynamische Ereignisse in der Zone (zusätzlich zu Luftabwurf, Emission und Horden):
 * <ul>
 * <li>Absturz: Ein Knall in der Ferne, eine Rauchsäule – ein Hubschrauber ist gerade abgestürzt. Wer schnell ist,
 * plündert ihn vor den Infizierten (und trifft vielleicht den Piloten).</li>
 * <li>Hilferuf: Ein Überlebender funkt um Hilfe, die Infizierten haben ihn eingekreist. Wer ihn rettet (alle
 * Infizierten in der Nähe erledigt), bekommt seinen Vorrat.</li>
 * </ul>
 */
public final class Ereignisse extends SavedData {

    private static final Factory<Ereignisse> FABRIK = new Factory<>(Ereignisse::new, Ereignisse::laden, null);

    /** art (0 Absturz, 1 Hilferuf), x, z, Zeitpunkt; bei laufendem Hilferuf die UUID des Überlebenden. */
    private final List<long[]> offen = new ArrayList<>();
    private final List<UUID> gerettet = new ArrayList<>();
    private UUID opfer;
    private long opferBis;
    private long naechster = -1;

    public static Ereignisse holen(ServerLevel w) {
        return w.getDataStorage().computeIfAbsent(FABRIK, "deadzonemc_ereignisse");
    }

    private static Ereignisse laden(CompoundTag tag, HolderLookup.Provider p) {
        Ereignisse e = new Ereignisse();
        e.naechster = tag.contains("Naechster") ? tag.getLong("Naechster") : -1;
        for (Tag t : tag.getList("Offen", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) t;
            e.offen.add(new long[]{c.getInt("Art"), c.getInt("X"), c.getInt("Z"), c.getLong("Zeit")});
        }
        if (tag.hasUUID("Opfer")) {
            e.opfer = tag.getUUID("Opfer");
            e.opferBis = tag.getLong("OpferBis");
        }
        return e;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider p) {
        tag.putLong("Naechster", naechster);
        ListTag l = new ListTag();
        for (long[] o : offen) {
            CompoundTag c = new CompoundTag();
            c.putInt("Art", (int) o[0]);
            c.putInt("X", (int) o[1]);
            c.putInt("Z", (int) o[2]);
            c.putLong("Zeit", o[3]);
            l.add(c);
        }
        tag.put("Offen", l);
        if (opfer != null) {
            tag.putUUID("Opfer", opfer);
            tag.putLong("OpferBis", opferBis);
        }
        return tag;
    }

    public static void tick(ServerTickEvent.Post event) {
        ServerLevel w = event.getServer().overworld();
        long zeit = w.getGameTime();
        if (zeit % 100 != 71) {
            return;
        }
        Ereignisse e = holen(w);
        if (e.naechster < 0) {
            e.naechster = zeit + 24000L + w.random.nextInt(24000);
            e.setDirty();
        }
        if (zeit >= e.naechster && !w.players().isEmpty() && Emission.stufe(w) == 0) {
            ServerPlayer p = w.players().get(w.random.nextInt(w.players().size()));
            if (w.canSeeSky(p.blockPosition()) && !ZufluchtSchutz.sicher(w, p.blockPosition())) {
                e.ausloesen(w, p, w.random.nextInt(2));
            }
            e.naechster = zeit + 30000L + w.random.nextInt(36000);
            e.setDirty();
        }
        e.offen.removeIf(o -> e.umsetzen(w, o, zeit));
        e.rettungPruefen(w, zeit);
    }

    /** Für den Selbsttest und Befehle: ein Ereignis sofort auslösen. */
    public void ausloesen(ServerLevel w, ServerPlayer p, int art) {
        double winkel = w.random.nextDouble() * Math.PI * 2;
        double r = 80 + w.random.nextDouble() * 70;
        int x = (int) (p.getX() + Math.cos(winkel) * r);
        int z = (int) (p.getZ() + Math.sin(winkel) * r);
        offen.add(new long[]{art, x, z, w.getGameTime()});
        setDirty();
        String richtung = richtung(p.getX(), p.getZ(), x, z);
        Component meldung = art == 0
                ? Component.translatable("ereignis.deadzonemc.absturz", Component.translatable("richtung.deadzonemc." + richtung))
                .withStyle(ChatFormatting.GOLD)
                : Component.translatable("ereignis.deadzonemc.hilferuf", Math.round(x / 25.0F) * 25, Math.round(z / 25.0F) * 25)
                .withStyle(ChatFormatting.YELLOW);
        for (ServerPlayer s : w.players()) {
            s.sendSystemMessage(meldung);
            if (art == 0) {
                // Knall aus der Richtung des Absturzes
                double dx = x - s.getX();
                double dz = z - s.getZ();
                double n = Math.max(1, Math.sqrt(dx * dx + dz * dz));
                SoundEvent knall = SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("deadzonemc", "atmo.explosion"));
                s.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(
                        net.minecraft.core.Holder.direct(knall), SoundSource.AMBIENT,
                        s.getX() + dx / n * 40, s.getY() + 6, s.getZ() + dz / n * 40, 4.0F, 1.0F, w.random.nextLong()));
            } else {
                s.playNotifySound(SoundEvents.NOTE_BLOCK_BIT.value(), SoundSource.PLAYERS, 0.7F, 0.6F);
            }
        }
    }

    private static String richtung(double vx, double vz, double nx, double nz) {
        double w = Math.toDegrees(Math.atan2(nz - vz, nx - vx));
        String[] namen = {"osten", "suedosten", "sueden", "suedwesten", "westen", "nordwesten", "norden", "nordosten"};
        return namen[(int) Math.floorMod(Math.round(w / 45.0), 8)];
    }

    /** Baut das Ereignis, sobald der Ort geladen ist. true = erledigt. */
    private boolean umsetzen(ServerLevel w, long[] o, long zeit) {
        BlockPos pos = new BlockPos((int) o[1], 0, (int) o[2]);
        if (zeit - o[3] > 24000L) {
            return true;
        }
        if (!w.hasChunkAt(pos) || !w.hasChunkAt(pos.offset(16, 0, 16)) || !w.hasChunkAt(pos.offset(-16, 0, -16))) {
            return false;
        }
        BlockPos boden = w.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos);
        if (ZufluchtSchutz.sicher(w, boden) || !w.getFluidState(boden.below()).isEmpty()) {
            return true;
        }
        if (o[0] == 0) {
            AbsturzFeature.testBauen(w, w.random, boden, AbsturzFeature.Art.HUBSCHRAUBER);
            for (int i = 0; i < 4; i++) {
                spawnen(w, MonsterTypen.WANDERER.get(), boden, 12, null);
            }
        } else {
            Ueberlebender u = MenschTypen.UEBERLEBENDER.get().create(w);
            if (u == null) {
                return true;
            }
            u.moveTo(boden.getX() + 0.5, boden.getY(), boden.getZ() + 0.5, w.random.nextFloat() * 360, 0);
            u.finalizeSpawn(w, w.getCurrentDifficultyAt(boden), MobSpawnType.EVENT, null);
            u.freundlichMachen();
            u.setPersistenceRequired();
            w.addFreshEntity(u);
            opfer = u.getUUID();
            opferBis = zeit + 12000L;
            for (int i = 0; i < 5 + w.random.nextInt(3); i++) {
                spawnen(w, i % 4 == 3 ? MonsterTypen.SCHREIER.get() : MonsterTypen.WANDERER.get(), boden, 9, u);
            }
            setDirty();
        }
        return true;
    }

    private static void spawnen(ServerLevel w, EntityType<? extends DeadzoneMonster> typ, BlockPos mitte, int radius,
                                net.minecraft.world.entity.LivingEntity ziel) {
        DeadzoneMonster m = typ.create(w);
        if (m == null) {
            return;
        }
        for (int versuch = 0; versuch < 6; versuch++) {
            int x = mitte.getX() + w.random.nextInt(radius * 2 + 1) - radius;
            int z = mitte.getZ() + w.random.nextInt(radius * 2 + 1) - radius;
            BlockPos p = w.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z));
            m.moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, w.random.nextFloat() * 360, 0);
            if (w.noCollision(m)) {
                m.finalizeSpawn(w, w.getCurrentDifficultyAt(p), MobSpawnType.EVENT, null);
                if (ziel != null) {
                    m.setTarget(ziel);
                }
                w.addFreshEntity(m);
                return;
            }
        }
    }

    /** Hilferuf: Sind alle Infizierten um den Überlebenden erledigt und ein Spieler bei ihm, gibt er seinen Vorrat. */
    private void rettungPruefen(ServerLevel w, long zeit) {
        if (opfer == null) {
            return;
        }
        if (!(w.getEntity(opfer) instanceof Ueberlebender u) || !u.isAlive() || zeit > opferBis) {
            if (zeit > opferBis || (w.getEntity(opfer) instanceof Ueberlebender tot && !tot.isAlive())) {
                opfer = null;
                setDirty();
            }
            return;
        }
        ServerPlayer retter = (ServerPlayer) w.getNearestPlayer(u, 6);
        if (retter == null || !w.getEntitiesOfClass(DeadzoneMonster.class, u.getBoundingBox().inflate(16, 6, 16)).isEmpty()) {
            return;
        }
        BlockPos kiste = u.blockPosition();
        if (w.getBlockState(kiste).canBeReplaced()) {
            w.setBlock(kiste, Blocks.BARREL.defaultBlockState(), 3);
            if (w.getBlockEntity(kiste) instanceof RandomizableContainerBlockEntity be) {
                be.setLootTable(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE,
                        ResourceLocation.fromNamespaceAndPath("deadzonemc", "kiste/versteck")), w.random.nextLong());
            }
            u.moveTo(u.getX() + 1, u.getY(), u.getZ());
        }
        for (ServerPlayer s : w.getEntitiesOfClass(ServerPlayer.class, u.getBoundingBox().inflate(24))) {
            s.sendSystemMessage(Component.literal("<").append(u.getDisplayName()).append("> ")
                    .append(Component.translatable("ereignis.deadzonemc.gerettet")).withStyle(ChatFormatting.WHITE));
        }
        w.playSound(null, kiste, SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8F, 1.2F);
        gerettet.add(opfer);
        opfer = null;
        setDirty();
    }
}
