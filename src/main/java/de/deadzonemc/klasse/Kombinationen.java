package de.deadzonemc.klasse;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Klassen-Kombinationen für kleine Gruppen: Wer mehrere Klassen spielt, bekommt für jedes Paar einen eigenen Bonus.
 * Jede Klasse passt zu jeder – jede der zehn Paarungen hat ihre Stärke:
 * <ul>
 * <li>Brecher + Schütze – Sturmtruppe: +25 % Rückstoßfestigkeit (in Klassen.anwenden)</li>
 * <li>Brecher + Sanitäter – Feldchirurg: jeder Nahkampf-Kill heilt ein Herz</li>
 * <li>Brecher + Techniker – Bollwerk: +2 Rüstung (in Klassen.anwenden)</li>
 * <li>Brecher + Späher – Raubtier: +5 % Tempo (in Klassen.anwenden)</li>
 * <li>Schütze + Sanitäter – Sanitätsschütze: Fernkampf-Kills geben kurz Regeneration</li>
 * <li>Schütze + Techniker – Waffenschmied: 15 % schneller nachladen (in Klassenvorteile)</li>
 * <li>Schütze + Späher – Scharfschütze: 20 % weniger Streuung (in Klassenvorteile)</li>
 * <li>Sanitäter + Techniker – Feldlazarett: +2 Herzen (in Klassen.anwenden)</li>
 * <li>Sanitäter + Späher – Überlebenskünstler: unter 40 % Leben schneller auf den Beinen</li>
 * <li>Techniker + Späher – Saboteur: 30 % leiser (in Klassenvorteile)</li>
 * </ul>
 */
public final class Kombinationen {

    private Kombinationen() {
    }

    public static void registrieren() {
        NeoForge.EVENT_BUS.addListener(Kombinationen::tod);
        NeoForge.EVENT_BUS.addListener(Kombinationen::tick);
    }

    private static void tod(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof Player p) || !(event.getEntity() instanceof Enemy)) {
            return;
        }
        boolean nahkampf = event.getSource().getDirectEntity() == p
                && !event.getSource().is(net.minecraft.tags.DamageTypeTags.IS_PROJECTILE);
        if (nahkampf && Klassen.kombi(p, Klasse.BRECHER, Klasse.SANITAETER)) {
            p.heal(2F);
        }
        if (!nahkampf && Klassen.kombi(p, Klasse.SCHUETZE, Klasse.SANITAETER)) {
            p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0));
        }
    }

    private static void tick(PlayerTickEvent.Post event) {
        Player p = event.getEntity();
        if (p.level().isClientSide || p.tickCount % 20 != 11) {
            return;
        }
        if (p.getHealth() < p.getMaxHealth() * 0.4F && Klassen.kombi(p, Klasse.SANITAETER, Klasse.SPAEHER)) {
            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 0, true, false, true));
        }
    }
}
