package de.deadzonemc.monster.boss;

import de.deadzonemc.DeadzoneMC;
import de.deadzonemc.mensch.Ansehen;
import de.deadzonemc.mensch.Fraktion;
import de.deadzonemc.monster.DeadzoneMonster;
import de.deadzonemc.monster.MonsterTypen;
import de.deadzonemc.strahlung.Strahlung;
import de.deadzonemc.welt.Staedte;
import de.deadzonemc.zuflucht.ZufluchtSchutz;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Bosse sind selten und besonders: Jeder hat seine eigene Bedingung, und nach einem Boss ist für einen
 * Spieler eine ganze Weile Ruhe. Nie zwei Bosse nah beieinander, nie in einer Zuflucht.
 */
public final class BossSpawner {

    /** Ruhe nach einem Boss für den Spieler (1,5 Spieltage) – wiederholbar, aber immer noch etwas Besonderes. */
    private static final long PAUSE_SPIELER = 36000;
    /** Wie lange dieselbe Bossart nicht wiederkommt (3 Spieltage). */
    private static final long PAUSE_ART = 72000;

    private static final Map<UUID, Long> LETZTER = new HashMap<>();
    private static final Map<String, Long> ART = new HashMap<>();

    private BossSpawner() {
    }

    private static TagKey<Biome> tag(String ns, String name) {
        return TagKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath(ns, name));
    }

    /** Darf hier für diesen Spieler gerade ein Boss dieser Art erscheinen? */
    public static boolean darf(ServerLevel w, ServerPlayer p, String art) {
        long jetzt = w.getGameTime();
        if (jetzt - LETZTER.getOrDefault(p.getUUID(), -PAUSE_SPIELER) < PAUSE_SPIELER) {
            return false;
        }
        if (jetzt - ART.getOrDefault(art, -PAUSE_ART) < PAUSE_ART) {
            return false;
        }
        return w.getEntitiesOfClass(Boss.class, p.getBoundingBox().inflate(200, 128, 200)).isEmpty()
                && !ZufluchtSchutz.sicher(w, p.blockPosition());
    }

    public static void vermerken(ServerLevel w, ServerPlayer p, String art) {
        LETZTER.put(p.getUUID(), w.getGameTime());
        ART.put(art, w.getGameTime());
    }

    public static void tick(ServerTickEvent.Post event) {
        if (event.getServer().getTickCount() % 400 != 211) {
            return;
        }
        ServerLevel w = event.getServer().overworld();
        if (!w.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING) || w.getDifficulty() == Difficulty.PEACEFUL) {
            return;
        }
        for (ServerPlayer p : w.players()) {
            if (!DeadzoneMonster.echterSpieler(p)) {
                continue;
            }
            versuchen(w, p);
        }
    }

    private static void versuchen(ServerLevel w, ServerPlayer p) {
        BlockPos pos = p.blockPosition();
        var biom = w.getBiome(pos);
        boolean nacht = w.isNight();
        boolean nebel = DeadzoneMonster.dichterNebel(w);
        int r = w.random.nextInt(1000);
        // Station Null: in der Reaktorhalle wartet die Strahlende
        if (de.deadzonemc.welt.station.StationNull.imLabor(w, pos)) {
            BlockPos kern = de.deadzonemc.welt.station.StationNull.reaktor(w, pos);
            if (kern != null && kern.distSqr(pos) < 18 * 18 && r < 600) {
                erscheinenBei(w, p, "strahlende", MonsterTypen.STRAHLENDE::get, kern);
            }
            return;
        }
        // Metro: in den U-Bahn-Tunneln unter der Stadt nistet die Schwarmmutter
        if (!w.canSeeSky(pos) && Staedte.istStadt(w, pos.getX() >> 4, pos.getZ() >> 4)) {
            Integer strasse = Staedte.strassenhoehe(w, pos.getX() >> 4, pos.getZ() >> 4);
            if (strasse != null && pos.getY() < strasse - 5) {
                if (r < 12) {
                    BlockPos platz = tunnelPlatz(w, p);
                    if (platz != null) {
                        erscheinenBei(w, p, "schwarmmutter", MonsterTypen.SCHWARMMUTTER::get, platz);
                    }
                }
                return;
            }
        }
        // Abgestürzter Transporthubschrauber: der Pilot steigt aus dem Feuer, das Signalfeuer erlischt
        if (w.canSeeSky(pos) && r < 350) {
            BlockPos feuer = hubschrauberFeuer(w, pos);
            if (feuer != null && darf(w, p, "pilot")) {
                BlockPos platz = neben(w, feuer, 4);
                if (platz != null && erscheinenBei(w, p, "pilot", MonsterTypen.PILOT::get, platz) != null) {
                    w.setBlock(feuer, w.getBlockState(feuer).setValue(net.minecraft.world.level.block.CampfireBlock.LIT, false), 3);
                }
                return;
            }
        }
        if (nebel && nacht && (biom.is(tag(DeadzoneMC.MODID, "verseucht")) || biom.is(tag(DeadzoneMC.MODID, "totwald"))
                || biom.is(tag(DeadzoneMC.MODID, "sumpfwald"))) && r < 7) {
            erscheinen(w, p, "nebelmutter", MonsterTypen.NEBELMUTTER::get, 30);
        } else if (Strahlung.dosis(p) >= 500 && biom.is(tag(DeadzoneMC.MODID, "verseucht")) && r < 15) {
            erscheinen(w, p, "strahlende", MonsterTypen.STRAHLENDE::get, 28);
        } else if (pos.getY() < 0 && !w.canSeeSky(pos) && r < 4) {
            erscheinen(w, p, "brutkoenigin", MonsterTypen.BRUTKOENIGIN::get, 20);
        } else if (nacht && w.getMoonPhase() == 0 && Staedte.istStadt(w, pos.getX() >> 4, pos.getZ() >> 4) && r < 12) {
            erscheinen(w, p, "schlaechter", MonsterTypen.SCHLAECHTER::get, 26);
        } else if (Ansehen.holen(p, Fraktion.WACHT) <= -80 && w.canSeeSky(pos) && r < 5) {
            erscheinen(w, p, "kommandant", MonsterTypen.KOMMANDANT::get, 34);
        } else if (biom.is(tag(DeadzoneMC.MODID, "wueste")) && p.isSprinting() && r < 5) {
            erscheinen(w, p, "schlund", MonsterTypen.SCHLUND::get, 14);
        } else if (w.isRainingAt(pos.above(2)) && biom.is(tag(DeadzoneMC.MODID, "sumpfwald")) && r < 7) {
            erscheinen(w, p, "pilzvater", MonsterTypen.PILZVATER::get, 26);
        } else if (biom.is(tag(DeadzoneMC.MODID, "schnee")) && (w.isThundering() || w.isRaining()) && r < 7) {
            erscheinen(w, p, "erfrorene", MonsterTypen.ERFRORENE::get, 26);
        }
    }

    /** Lässt einen Boss außer Sicht in {@code abstand} Blöcken erscheinen. */
    public static Boss erscheinen(ServerLevel w, ServerPlayer p, String art, Supplier<? extends EntityType<? extends Boss>> typ,
                                  int abstand) {
        if (!darf(w, p, art)) {
            return null;
        }
        for (int versuch = 0; versuch < 16; versuch++) {
            double winkel = w.random.nextDouble() * Math.PI * 2;
            int x = Mth.floor(p.getX() + Math.cos(winkel) * abstand);
            int z = Mth.floor(p.getZ() + Math.sin(winkel) * abstand);
            if (!w.hasChunkAt(new BlockPos(x, 0, z))) {
                continue;
            }
            int y = p.getBlockY() < 0 ? p.getBlockY() : w.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos pos = new BlockPos(x, y, z);
            Boss b = typ.get().create(w);
            if (b == null) {
                return null;
            }
            b.moveTo(x + 0.5, y, z + 0.5, w.random.nextFloat() * 360, 0);
            if (!w.noCollision(b) || !w.getBlockState(pos.below()).isSolid()) {
                continue;
            }
            EventHooks.finalizeMobSpawn(b, w, w.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null);
            if (b.isSpawnCancelled()) {
                return null;
            }
            w.addFreshEntity(b);
            vermerken(w, p, art);
            return b;
        }
        return null;
    }

    /** Ein freier Platz im Tunnel, 14–26 Blöcke vom Spieler entfernt in einer der vier Himmelsrichtungen. */
    private static BlockPos tunnelPlatz(ServerLevel w, ServerPlayer p) {
        var richtungen = new java.util.ArrayList<>(net.minecraft.core.Direction.Plane.HORIZONTAL.stream().toList());
        java.util.Collections.shuffle(richtungen, new java.util.Random(w.random.nextLong()));
        for (var richtung : richtungen) {
            for (int d = 26; d >= 14; d -= 3) {
                for (int dy = -1; dy <= 1; dy++) {
                    BlockPos q = p.blockPosition().relative(richtung, d).above(dy);
                    if (w.getBlockState(q.below()).isSolid() && raum(w, q, 2, 2)) {
                        return q;
                    }
                }
            }
        }
        return null;
    }

    /** Ist um {@code q} herum Platz (Breite in jede Richtung, Höhe)? */
    private static boolean raum(ServerLevel w, BlockPos q, int halb, int hoehe) {
        for (int dx = -halb + 1; dx < halb; dx++) {
            for (int dz = -halb + 1; dz < halb; dz++) {
                for (int dy = 0; dy < hoehe; dy++) {
                    if (!w.getBlockState(q.offset(dx, dy, dz)).getCollisionShape(w, q.offset(dx, dy, dz)).isEmpty()) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /** Sucht das brennende Signalfeuer eines abgestürzten Hubschraubers (Lagerfeuer auf Heu, Rotormast daneben). */
    private static BlockPos hubschrauberFeuer(ServerLevel w, BlockPos um) {
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int dx = -20; dx <= 20; dx++) {
            for (int dz = -20; dz <= 20; dz++) {
                int boden = w.getHeight(Heightmap.Types.MOTION_BLOCKING, um.getX() + dx, um.getZ() + dz) - 1;
                m.set(um.getX() + dx, boden, um.getZ() + dz);
                var zustand = w.getBlockState(m);
                if (!zustand.is(net.minecraft.world.level.block.Blocks.CAMPFIRE)
                        || !zustand.getValue(net.minecraft.world.level.block.CampfireBlock.LIT)
                        || !zustand.getValue(net.minecraft.world.level.block.CampfireBlock.SIGNAL_FIRE)
                        || !w.getBlockState(m.below()).is(net.minecraft.world.level.block.Blocks.HAY_BLOCK)) {
                    continue;
                }
                BlockPos f = m.immutable();
                for (BlockPos q : BlockPos.betweenClosed(f.offset(-3, 0, -3), f.offset(3, 0, 3))) {
                    if (w.getBlockState(q).is(net.minecraft.world.level.block.Blocks.IRON_BLOCK)) {
                        return f;
                    }
                }
            }
        }
        return null;
    }

    /** Ein freier Platz auf dem Boden in der Nähe. */
    private static BlockPos neben(ServerLevel w, BlockPos zentrum, int radius) {
        for (int versuch = 0; versuch < 20; versuch++) {
            int x = zentrum.getX() + w.random.nextInt(radius * 2 + 1) - radius;
            int z = zentrum.getZ() + w.random.nextInt(radius * 2 + 1) - radius;
            BlockPos q = new BlockPos(x, w.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
            if (Math.abs(q.getY() - zentrum.getY()) < 6 && w.getBlockState(q.below()).isSolid() && raum(w, q, 1, 3)) {
                return q;
            }
        }
        return null;
    }

    /** Lässt einen Boss genau an dieser Stelle erscheinen (z. B. im Reaktor von Station Null). */
    public static Boss erscheinenBei(ServerLevel w, ServerPlayer p, String art, Supplier<? extends EntityType<? extends Boss>> typ,
                                     BlockPos wo) {
        if (!darf(w, p, art)) {
            return null;
        }
        Boss b = typ.get().create(w);
        if (b == null) {
            return null;
        }
        b.moveTo(wo.getX() + 0.5, wo.getY(), wo.getZ() + 0.5, w.random.nextFloat() * 360, 0);
        EventHooks.finalizeMobSpawn(b, w, w.getCurrentDifficultyAt(wo), MobSpawnType.EVENT, null);
        if (b.isSpawnCancelled()) {
            return null;
        }
        w.addFreshEntity(b);
        vermerken(w, p, art);
        return b;
    }

    /** Ein Hirte für eine Herde (selten): nur wenn die Bedingungen für einen Boss gerade passen. */
    public static Boss hirteFuerHerde(ServerLevel w, BlockPos wo, ServerPlayer p) {
        if (w.random.nextInt(8) != 0 || !darf(w, p, "hirte")) {
            return null;
        }
        Boss b = MonsterTypen.HIRTE.get().create(w);
        if (b == null) {
            return null;
        }
        b.moveTo(wo.getX() + 0.5, wo.getY(), wo.getZ() + 0.5, w.random.nextFloat() * 360, 0);
        EventHooks.finalizeMobSpawn(b, w, w.getCurrentDifficultyAt(wo), MobSpawnType.EVENT, null);
        if (b.isSpawnCancelled()) {
            return null;
        }
        w.addFreshEntity(b);
        vermerken(w, p, "hirte");
        return b;
    }
}
