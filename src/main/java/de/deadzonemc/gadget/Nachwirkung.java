package de.deadzonemc.gadget;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Effekte, die mit Verzögerung einsetzen (z. B. Erschöpfung nach Adrenalin). */
public final class Nachwirkung {

    private static final Map<UUID, Object[]> GEPLANT = new HashMap<>();

    private Nachwirkung() {
    }

    public static void registrieren() {
        NeoForge.EVENT_BUS.addListener(Nachwirkung::tick);
    }

    public static void planen(Player p, int ticks, MobEffectInstance effekt) {
        GEPLANT.put(p.getUUID(), new Object[]{p.level().getGameTime() + ticks, effekt});
    }

    private static void tick(PlayerTickEvent.Post event) {
        Player p = event.getEntity();
        if (p.level().isClientSide || p.tickCount % 20 != 0) {
            return;
        }
        Object[] e = GEPLANT.get(p.getUUID());
        if (e != null && p.level().getGameTime() >= (long) e[0]) {
            GEPLANT.remove(p.getUUID());
            p.addEffect(new MobEffectInstance((MobEffectInstance) e[1]));
        }
    }
}
