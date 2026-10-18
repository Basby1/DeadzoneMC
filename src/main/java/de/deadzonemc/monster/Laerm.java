package de.deadzonemc.monster;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Lärm lockt die Infizierten an: Schüsse, Explosionen, Fahrzeuge, splitterndes Glas, Rennen. Wer nichts zu tun hat,
 * läuft zur Geräuschquelle; Herden in Hörweite ändern ihre Richtung. Leise sein (schleichen, Nahkampf) lohnt sich.
 */
public final class Laerm {

    private Laerm() {
    }

    /** Ein Geräusch an diesem Ort, hörbar im angegebenen Umkreis (Blöcke). */
    public static void machen(ServerLevel level, Vec3 ort, double radius) {
        for (DeadzoneMonster m : level.getEntitiesOfClass(DeadzoneMonster.class, new AABB(ort, ort).inflate(radius, radius * 0.5, radius))) {
            double d2 = m.position().distanceToSqr(ort);
            if (d2 < radius * radius && m.getTarget() == null) {
                m.laermGehoert(ort);
            }
        }
        Herden.laermGehoert(level, ort, radius * 1.6);
    }

    // ---- Quellen -----------------------------------------------------------------------------

    /** Schuss (TaCZ) – wird von der Waffenpflege aufgerufen, die das Ereignis schon abhört. */
    public static void schuss(Entity schuetze) {
        if (schuetze.level() instanceof ServerLevel sl) {
            machen(sl, schuetze.position(), 56);
        }
    }

    public static void explosion(ExplosionEvent.Detonate event) {
        if (event.getLevel() instanceof ServerLevel sl) {
            machen(sl, event.getExplosion().center(), 72);
        }
    }

    public static void blockBricht(BlockEvent.BreakEvent event) {
        if (event.getLevel() instanceof ServerLevel sl && event.getState().getSoundType() == SoundType.GLASS) {
            machen(sl, net.minecraft.world.phys.Vec3.atCenterOf(event.getPos()), 20);
        }
    }

    public static void spieler(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || p.tickCount % 20 != 9 || p.isSpectator() || p.isCreative()) {
            return;
        }
        ServerLevel sl = p.serverLevel();
        Entity fahrzeug = p.getVehicle();
        if (fahrzeug != null) {
            double tempo = fahrzeug.getDeltaMovement().horizontalDistance();
            if (tempo > 0.12) {
                // Motoren (Flugzeuge, Fahrzeuge aus Mods) sind laut, Boote und Pferde leiser
                String id = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(fahrzeug.getType()).getNamespace();
                double radius = id.equals("minecraft") ? 18 : 48;
                machen(sl, p.position(), radius);
            }
        } else if (p.isSprinting()) {
            machen(sl, p.position(), 10);
        }
    }
}
