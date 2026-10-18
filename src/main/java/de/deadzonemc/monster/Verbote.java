package de.deadzonemc.monster;

import de.deadzonemc.DeadzoneMC;
import de.deadzonemc.waffen.wurf.Wurfkoerper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.GameRules;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityMountEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/**
 * Dinge, die Monster in der Deadzone nicht dürfen (Rückmeldung 18.10.):
 * <ul>
 *     <li>keine Phantome – und damit auch keine Monster, die auf Phantomen durch die Luft reiten,</li>
 *     <li>kein Reiten: Monster steigen auf nichts und niemanden auf (Improved Mobs, Hühnerreiter …),</li>
 *     <li>nichts werfen: keine Granaten, Tränke, TNT, Schneebälle oder Perlen von Monstern.</li>
 * </ul>
 */
public final class Verbote {

    private Verbote() {
    }

    public static void registrieren() {
        NeoForge.EVENT_BUS.addListener(Verbote::beitritt);
        NeoForge.EVENT_BUS.addListener(Verbote::aufsteigen);
        NeoForge.EVENT_BUS.addListener(Verbote::gestartet);
    }

    private static void gestartet(ServerStartedEvent event) {
        // Ohne Schlaflosigkeit gibt es keine Phantome mehr
        event.getServer().getGameRules().getRule(GameRules.RULE_DOINSOMNIA).set(false, event.getServer());
    }

    private static void beitritt(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel)) {
            return;
        }
        Entity e = event.getEntity();
        if (e.getType() == EntityType.PHANTOM) {
            event.setCanceled(true);
            return;
        }
        if (e instanceof Mob m && !(m instanceof Player)) {
            // Monster, die schon mit Reittier oder Reiter erscheinen, werden getrennt
            if (m instanceof Enemy && (m.isPassenger() || m.isVehicle())) {
                m.ejectPassengers();
                m.stopRiding();
            }
            return;
        }
        Entity besitzer = e instanceof Projectile p ? p.getOwner() : e instanceof PrimedTnt t ? t.getOwner() : null;
        boolean vonMonster = besitzer instanceof LivingEntity le && !(le instanceof Player);
        if (!vonMonster) {
            // Eigene Wurfkörper (Granaten …) gibt es nur aus Spielerhand
            if (e instanceof Wurfkoerper && !(besitzer instanceof Player)) {
                event.setCanceled(true);
            }
            return;
        }
        boolean eigenesMonster = DeadzoneMC.MODID.equals(BuiltInRegistries.ENTITY_TYPE.getKey(besitzer.getType()).getNamespace());
        if (e instanceof Wurfkoerper || e instanceof PrimedTnt
                || (!eigenesMonster && e instanceof ThrowableProjectile)) {
            event.setCanceled(true);
        }
    }

    private static void aufsteigen(EntityMountEvent event) {
        if (!event.isMounting()) {
            return;
        }
        Entity reiter = event.getEntityMounting();
        Entity reittier = event.getEntityBeingMounted();
        if (reiter instanceof Player || reittier == null) {
            return;
        }
        if (reiter instanceof Enemy && reittier instanceof LivingEntity) {
            event.setCanceled(true);
        }
        if (reittier.getType() == EntityType.PHANTOM) {
            event.setCanceled(true);
        }
    }
}
