package de.deadzonemc.monster;

import de.deadzonemc.basis.Basen;
import de.deadzonemc.welt.Staedte;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.jetbrains.annotations.Nullable;

/**
 * „The Walking Dead“ in den Städten: Wer durch eine Lost-Cities-Stadt läuft, trifft ständig auf Gruppen von
 * Infizierten – tagsüber wie nachts. Die Gruppen tauchen außer Sicht auf, nachts sind sie größer, im dichten
 * Nebel noch größer. Ein Deckel pro Spieler verhindert, dass der Server erstickt.
 */
public final class StadtHorden {

    private StadtHorden() {
    }

    /** Wie viele Deadzone-Monster höchstens um einen Spieler in der Stadt unterwegs sind. */
    static int deckel(ServerLevel welt) {
        // Tagsüber spielbar, nachts die Hölle (Rückmeldung: Tag leichter, Nacht viel gefährlicher)
        // Rückmeldung 18.10.: nachts sofort überrannt – deutlich weniger, aber immer noch bedrohlich
        int d = welt.isNight() ? 48 : 24;
        if (DeadzoneMonster.dichterNebel(welt)) {
            d += 10;
        }
        if (welt.getDifficulty() == Difficulty.EASY) {
            d = d * 2 / 3;
        }
        return d;
    }

    public static void tick(ServerTickEvent.Post event) {
        if (event.getServer().getTickCount() % 40 != 20) {
            return;
        }
        ServerLevel welt = event.getServer().overworld();
        if (!welt.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING) || welt.getDifficulty() == Difficulty.PEACEFUL) {
            return;
        }
        for (ServerPlayer p : welt.players()) {
            if (!DeadzoneMonster.echterSpieler(p)) {
                continue;
            }
            if (!Staedte.istStadt(welt, p.chunkPosition().x, p.chunkPosition().z)) {
                streuner(welt, p);
                continue;
            }
            Integer strasseHier = Staedte.strassenhoehe(welt, p.chunkPosition().x, p.chunkPosition().z);
            if (strasseHier != null && p.getBlockY() < strasseHier - 5 && !welt.canSeeSky(p.blockPosition())) {
                metro(welt, p);
                continue;
            }
            if (SpawnSchutz.geschuetzt(p)) {
                continue;
            }
            // Zwischen zwei Gruppen vergeht Zeit (tagsüber 40 s, nachts 20 s), damit man Luft holen kann
            long jetzt = welt.getGameTime();
            Long naechste = NAECHSTE_GRUPPE.get(p.getUUID());
            if (naechste != null && jetzt < naechste) {
                continue;
            }
            int nah = welt.getEntitiesOfClass(DeadzoneMonster.class, p.getBoundingBox().inflate(64, 32, 64)).size();
            int frei = deckel(welt) - nah;
            if (frei < 3) {
                continue;
            }
            if (gruppeSpawnen(welt, p.getX(), p.getBlockY(), p.getZ(), p, Math.min(frei, gruppengroesse(welt))) > 0) {
                NAECHSTE_GRUPPE.put(p.getUUID(), jetzt + (welt.isNight() ? 400 : 800) + welt.random.nextInt(200));
            }
        }
    }

    private static final java.util.Map<java.util.UUID, Long> NAECHSTE_GRUPPE = new java.util.HashMap<>();

    private static final java.util.Set<java.util.UUID> IN_DER_METRO = new java.util.HashSet<>();

    /** Unter der Stadt, in den U-Bahn-Tunneln: kleine Gruppen aus Kriechern, Wühlern und Lauerern im Dunkeln. */
    private static void metro(ServerLevel welt, ServerPlayer p) {
        if (IN_DER_METRO.add(p.getUUID())) {
            p.displayClientMessage(net.minecraft.network.chat.Component.translatable("metro.deadzonemc.betreten")
                    .withStyle(net.minecraft.ChatFormatting.DARK_GRAY, net.minecraft.ChatFormatting.ITALIC), true);
        }
        int nah = welt.getEntitiesOfClass(DeadzoneMonster.class, p.getBoundingBox().inflate(40, 12, 40)).size();
        if (nah >= 20) {
            return;
        }
        int anzahl = 3 + welt.random.nextInt(4);
        for (int versuch = 0; versuch < 12 && anzahl > 0; versuch++) {
            double winkel = welt.random.nextDouble() * Math.PI * 2;
            double abstand = 14 + welt.random.nextDouble() * 16;
            int x = Mth.floor(p.getX() + Math.cos(winkel) * abstand);
            int z = Mth.floor(p.getZ() + Math.sin(winkel) * abstand);
            if (!welt.hasChunkAt(new BlockPos(x, 0, z))) {
                continue;
            }
            BlockPos pos = boden(welt, x, z, p.getBlockY());
            if (pos == null || welt.canSeeSky(pos) || welt.getMaxLocalRawBrightness(pos) > 4) {
                continue;
            }
            int w = welt.random.nextInt(100);
            // Kein Wühler in der Metro: der lebt nur in natürlichen Höhlen
            EntityType<? extends DeadzoneMonster> typ = w < 22 ? MonsterTypen.KRIECHER.get()
                    : w < 40 ? MonsterTypen.LAUERER.get() : w < 55 ? MonsterTypen.BRUT.get()
                    : w < 68 ? MonsterTypen.SCHEINTOTER.get() : w < 80 ? MonsterTypen.IMITATOR.get()
                    : MonsterTypen.WANDERER.get();
            DeadzoneMonster e = typ.create(welt);
            if (e == null) {
                return;
            }
            e.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, welt.random.nextFloat() * 360, 0);
            if (p.hasLineOfSight(e) || !welt.noCollision(e)) {
                continue;
            }
            EventHooks.finalizeMobSpawn(e, welt, welt.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null);
            if (e.isSpawnCancelled()) {
                continue;
            }
            welt.addFreshEntity(e);
            anzahl--;
        }
    }

    /**
     * Außerhalb der Städte: Streuner. Kleine gemischte Grüppchen (passend zur Gegend) tauchen außer Sicht auf,
     * damit auch das Land nie leer wirkt. Zusätzlich zu den großen Herden.
     */
    private static void streuner(ServerLevel welt, ServerPlayer p) {
        if (welt.getServer().getTickCount() % 200 != 20 || de.deadzonemc.zuflucht.ZufluchtSchutz.sicher(welt, p.blockPosition())
                || !welt.canSeeSky(p.blockPosition())) {
            return;
        }
        if (SpawnSchutz.geschuetzt(p)) {
            return;
        }
        int deckel = welt.isNight() ? 20 : 10;
        if (DeadzoneMonster.dichterNebel(welt)) {
            deckel += 6;
        }
        int nah = welt.getEntitiesOfClass(DeadzoneMonster.class, p.getBoundingBox().inflate(64, 32, 64)).size();
        if (nah >= deckel) {
            return;
        }
        int anzahl = Math.min(deckel - nah, 1 + welt.random.nextInt(welt.isNight() ? 4 : 3));
        for (int versuch = 0; versuch < 8 && anzahl > 0; versuch++) {
            double winkel = welt.random.nextDouble() * Math.PI * 2;
            double abstand = 26 + welt.random.nextDouble() * 26;
            int x = Mth.floor(p.getX() + Math.cos(winkel) * abstand);
            int z = Mth.floor(p.getZ() + Math.sin(winkel) * abstand);
            if (!welt.hasChunkAt(new BlockPos(x, 0, z))) {
                continue;
            }
            int y = welt.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos mitte = boden(welt, x, z, y);
            if (mitte == null || de.deadzonemc.zuflucht.ZufluchtSchutz.sicher(welt, mitte)) {
                continue;
            }
            for (int i = 0; i < anzahl * 2 && anzahl > 0; i++) {
                BlockPos pos = boden(welt, mitte.getX() + welt.random.nextInt(7) - 3, mitte.getZ() + welt.random.nextInt(7) - 3,
                        mitte.getY());
                if (pos == null) {
                    continue;
                }
                DeadzoneMonster e = Herden.mitglied(welt, pos, false).create(welt);
                if (e == null) {
                    return;
                }
                e.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, welt.random.nextFloat() * 360, 0);
                if (p.hasLineOfSight(e) || !welt.noCollision(e)) {
                    continue;
                }
                EventHooks.finalizeMobSpawn(e, welt, welt.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null);
                if (e.isSpawnCancelled()) {
                    continue;
                }
                welt.addFreshEntity(e);
                anzahl--;
            }
        }
    }

    static int gruppengroesse(ServerLevel welt) {
        int n = welt.isNight() ? 4 + welt.random.nextInt(4) : 3 + welt.random.nextInt(3);
        if (DeadzoneMonster.dichterNebel(welt)) {
            n += 2;
        }
        return n;
    }

    private static EntityType<? extends DeadzoneMonster> mitglied(ServerLevel welt, int nr, BlockPos pos) {
        int w = welt.random.nextInt(100);
        if (nr == 0 && w < 10) {
            return MonsterTypen.SCHREIER.get(); // ruft die ganze Straße zusammen
        }
        if (nr == 0 && welt.getDifficulty() == Difficulty.HARD && w > 98) {
            return MonsterTypen.JUGGERNAUT.get();
        }
        return Herden.mitglied(welt, pos, true);
    }

    /** Spawnt eine Gruppe 28–52 Blöcke entfernt in einem Stadt-Chunk, außer Sicht. Gibt die Anzahl zurück. */
    public static int gruppeSpawnen(ServerLevel welt, double ox, int oy, double oz, @Nullable ServerPlayer p, int anzahl) {
        Basen basen = Basen.holen(welt);
        for (int versuch = 0; versuch < 10; versuch++) {
            double winkel = welt.random.nextDouble() * Math.PI * 2;
            double abstand = 32 + welt.random.nextDouble() * 22;
            int x = Mth.floor(ox + Math.cos(winkel) * abstand);
            int z = Mth.floor(oz + Math.sin(winkel) * abstand);
            if (!welt.hasChunkAt(new BlockPos(x, 0, z))) {
                continue;
            }
            Integer strasse = Staedte.strassenhoehe(welt, x >> 4, z >> 4);
            if (strasse == null || basen.basisBei(x, z, 8) != null) {
                continue;
            }
            BlockPos mitte = boden(welt, x, z, p == null || Math.abs(strasse - oy) < 12 ? strasse : oy);
            if (mitte == null) {
                continue;
            }
            int gespawnt = 0;
            for (int i = 0; i < anzahl * 3 && gespawnt < anzahl; i++) {
                BlockPos pos = i == 0 ? mitte : boden(welt, mitte.getX() + welt.random.nextInt(7) - 3,
                        mitte.getZ() + welt.random.nextInt(7) - 3, mitte.getY());
                if (pos == null) {
                    continue;
                }
                DeadzoneMonster e = mitglied(welt, gespawnt, pos).create(welt);
                if (e == null) {
                    break;
                }
                e.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, welt.random.nextFloat() * 360, 0);
                if ((p != null && p.hasLineOfSight(e)) || !welt.noCollision(e)) {
                    continue;
                }
                EventHooks.finalizeMobSpawn(e, welt, welt.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null);
                if (e.isSpawnCancelled()) {
                    continue;
                }
                welt.addFreshEntity(e);
                gespawnt++;
            }
            if (gespawnt > 0) {
                return gespawnt;
            }
        }
        return 0;
    }

    /** Freier Stehplatz nahe der Höhe {@code y} (fester Boden, zwei Blöcke Luft, kein Wasser). */
    private static BlockPos boden(ServerLevel welt, int x, int z, int y) {
        for (int d = 0; d <= 6; d++) {
            for (int s : d == 0 ? new int[]{0} : new int[]{d, -d}) {
                BlockPos pos = new BlockPos(x, y + s, z);
                BlockState unten = welt.getBlockState(pos.below());
                if (unten.isSolid() && welt.getFluidState(pos.below()).isEmpty()
                        && welt.getBlockState(pos).getCollisionShape(welt, pos).isEmpty() && welt.getFluidState(pos).isEmpty()
                        && welt.getBlockState(pos.above()).getCollisionShape(welt, pos.above()).isEmpty()) {
                    return pos;
                }
            }
        }
        return null;
    }
}
