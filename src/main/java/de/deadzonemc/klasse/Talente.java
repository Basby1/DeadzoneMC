package de.deadzonemc.klasse;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;

/**
 * Talente aus den Skillbäumen (Pufferfish's Skills), die keine Vanilla-Attribute sind. Der Skillbaum vergibt
 * dafür Spieler-Tags („dz.…“), hier werden sie ausgewertet.
 */
public final class Talente {

    private Talente() {
    }

    public static boolean hat(Player p, String talent) {
        return p.getTags().contains("dz." + talent);
    }

    /** Schusswaffen-Talente: mehr Schaden mit Fernkampf (TaCZ-Kugeln, Pfeile). */
    public static void schaden(LivingIncomingDamageEvent event) {
        Entity quelle = event.getSource().getEntity();
        if (!(quelle instanceof Player p)) {
            return;
        }
        boolean fern = event.getSource().getDirectEntity() != p || event.getSource().is(DamageTypeTags.IS_PROJECTILE);
        if (!fern) {
            return;
        }
        float f = 1.0F;
        for (int i = 1; i <= 3; i++) {
            if (hat(p, "schuetze" + i)) {
                f += 0.08F;
            }
        }
        if (hat(p, "kaltbluetig") && p.getHealth() < p.getMaxHealth() * 0.3F) {
            f += 0.15F;
        }
        if (f != 1.0F) {
            event.setAmount(event.getAmount() * f);
        }
    }

    /** Heilung durch Verbände und Medizin (Feldarzt I/II). */
    public static float heilfaktor(Player p) {
        return hat(p, "feldarzt2") ? 1.5F : hat(p, "feldarzt1") ? 1.25F : 1.0F;
    }

    /** Wie viel Strahlung ankommt (Strahlenfest I/II). */
    public static float strahlenfaktor(Player p) {
        return hat(p, "strahlenfest2") ? 0.5F : hat(p, "strahlenfest1") ? 0.75F : 1.0F;
    }

    /** Eiserner Magen: verdorbenes Essen macht keinen Hunger mehr. */
    public static void effekt(MobEffectEvent.Applicable event) {
        if (event.getEntity() instanceof Player p && event.getEffectInstance().getEffect().equals(MobEffects.HUNGER)
                && hat(p, "magen")) {
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        }
    }

    /** Schleicher: Wer geduckt geht, wird erst aus nächster Nähe bemerkt. */
    public static void ziel(LivingChangeTargetEvent event) {
        if (event.getNewAboutToBeSetTarget() instanceof Player p && event.getEntity() instanceof Enemy
                && p.isCrouching() && hat(p, "leise") && event.getEntity().getLastHurtByMob() != p
                && event.getEntity().distanceToSqr(p) > 7 * 7) {
            event.setCanceled(true);
        }
    }
}
