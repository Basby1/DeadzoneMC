package de.deadzonemc.ausruestung;

import de.deadzonemc.ausruestung.Ausruestung.Satz;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Satzboni (ab 4 Teilen, teils schon ab 2):
 * Schrottpanzer – weniger Schaden von Infizierten. Taktisch – weniger Rückstoß/Streuung (in den Waffen).
 * Sanitäter – Verbände heilen mehr (in VerbandItem), langsame Regeneration. Schutzanzug – Strahlung stark
 * verringert (in Strahlung). Tarnanzug – Infizierte bemerken dich schleichend erst aus nächster Nähe.
 * Mutantenpanzer – Infizierte nehmen Schaden, wenn sie zuschlagen.
 */
public final class Satzboni {

    private Satzboni() {
    }

    public static void schaden(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof Player p && event.getSource().getEntity() instanceof Enemy) {
            if (Ausruestung.traegt(p, Satz.SCHROTTPANZER) >= 4) {
                event.setAmount(event.getAmount() * 0.75F);
            }
            if (Ausruestung.traegt(p, Satz.MUTANTENPANZER) >= 4 && event.getSource().getEntity() instanceof Mob m) {
                m.hurt(p.damageSources().thorns(p), 2.0F);
            }
        }
    }

    public static void zielen(LivingChangeTargetEvent event) {
        if (event.getNewAboutToBeSetTarget() instanceof Player p && event.getEntity() instanceof Enemy
                && p.isShiftKeyDown() && Ausruestung.traegt(p, Satz.TARNANZUG) >= 4
                && event.getEntity().distanceToSqr(p) > 7 * 7 && event.getEntity().getLastHurtByMob() != p) {
            event.setCanceled(true);
        }
    }

    public static void tick(PlayerTickEvent.Post event) {
        Player p = event.getEntity();
        if (p.level().isClientSide || p.tickCount % 100 != 0) {
            return;
        }
        if (Ausruestung.traegt(p, Satz.SANITAETER) >= 4 && p.getHealth() < p.getMaxHealth()) {
            p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0, true, false, true));
        }
    }

    public static boolean taktisch(Player p) {
        return Ausruestung.traegt(p, Satz.TAKTISCH) >= 4;
    }

    public static boolean schutzanzug(Player p) {
        return Ausruestung.traegt(p, Satz.SCHUTZANZUG) >= 4;
    }
}
