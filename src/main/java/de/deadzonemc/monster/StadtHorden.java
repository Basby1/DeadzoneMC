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
        int d = welt.isNight() ? 30 : 18;
        if (DeadzoneMonster.dichterNebel(welt)) {
            d += 8;
        }
        if (welt.getDifficulty() == Difficulty.EASY) {
            d = d * 2 / 3;
        }
        return d;
    }

    public static void tick(ServerTickEvent.Post event) {
        if (event.getServer().getTickCount() % 80 != 40) {
            return;
        }
        ServerLevel welt = event.getServer().overworld();
        if (!welt.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING) || welt.getDifficulty() == Difficulty.PEACEFUL) {
            return;
        }
        for (ServerPlayer p : welt.players()) {
            if (!DeadzoneMonster.echterSpieler(p) || !Staedte.istStadt(welt, p.chunkPosition().x, p.chunkPosition().z)) {
                continue;
            }
            int nah = welt.getEntitiesOfClass(DeadzoneMonster.class, p.getBoundingBox().inflate(64, 32, 64)).size();
            int frei = deckel(welt) - nah;
            if (frei < 3 || welt.random.nextInt(3) == 0) {
                continue;
            }
            gruppeSpawnen(welt, p.getX(), p.getBlockY(), p.getZ(), p, Math.min(frei, gruppengroesse(welt)));
        }
    }

    static int gruppengroesse(ServerLevel welt) {
        int n = welt.isNight() ? 4 + welt.random.nextInt(5) : 3 + welt.random.nextInt(4);
        if (DeadzoneMonster.dichterNebel(welt)) {
            n += 2;
        }
        return n;
    }

    private static EntityType<? extends DeadzoneMonster> mitglied(ServerLevel welt, int nr) {
        int w = welt.random.nextInt(100);
        if (nr == 0 && w < 12) {
            return MonsterTypen.SCHREIER.get(); // ruft die ganze Straße zusammen
        }
        if (w < 10) {
            return MonsterTypen.KRIECHER.get();
        }
        if (welt.isNight() && w < 15) {
            return MonsterTypen.BRUT.get();
        }
        if (nr == 0 && welt.getDifficulty() == Difficulty.HARD && w > 98) {
            return MonsterTypen.JUGGERNAUT.get();
        }
        return MonsterTypen.WANDERER.get();
    }

    /** Spawnt eine Gruppe 28–52 Blöcke entfernt in einem Stadt-Chunk, außer Sicht. Gibt die Anzahl zurück. */
    public static int gruppeSpawnen(ServerLevel welt, double ox, int oy, double oz, @Nullable ServerPlayer p, int anzahl) {
        Basen basen = Basen.holen(welt);
        for (int versuch = 0; versuch < 10; versuch++) {
            double winkel = welt.random.nextDouble() * Math.PI * 2;
            double abstand = 28 + welt.random.nextDouble() * 24;
            int x = Mth.floor(p.getX() + Math.cos(winkel) * abstand);
            int z = Mth.floor(p.getZ() + Math.sin(winkel) * abstand);
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
                DeadzoneMonster e = mitglied(welt, gespawnt).create(welt);
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
