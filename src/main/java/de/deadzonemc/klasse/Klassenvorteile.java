package de.deadzonemc.klasse;

import de.deadzonemc.ausruestung.Ausruestung;
import de.deadzonemc.ausruestung.Ausruestung.Satz;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * Jede Klasse darf jede Waffe und jede Rüstung benutzen – aber jede Waffe und jeder Satz hat eine Lieblingsklasse,
 * und nur die bekommt den Extra-Vorteil:
 * <ul>
 * <li>Brecher: Wuchtwaffen +25 % Schaden, Schrotflinten +15 %; Schrottpanzer: noch weniger Schaden, rückstoßfest</li>
 * <li>Schütze: Gewehre +15 % Schaden, −30 % Rückstoß, −20 % Streuung; Taktisch: 20 % schneller nachladen</li>
 * <li>Sanitäter: Pistolen +15 % Schaden, 35 % schneller nachladen; Sanitäter-Satz: heilt Mitspieler in der Nähe</li>
 * <li>Techniker: Eigenbau-Waffen +20 % Schaden; Schutzanzug: immun gegen Gift und Verdorren</li>
 * <li>Späher: leise Waffen +15 % Schaden, halber Lärm; Tarnanzug: wird schleichend erst aus 4 Blöcken bemerkt</li>
 * </ul>
 */
public final class Klassenvorteile {

    private static final Map<String, Klasse> LIEBLING = new HashMap<>();

    static {
        for (String id : new String[]{"vorschlaghammer", "feuerwehraxt", "nagelschlaeger", "baseballschlaeger", "knochenkeule",
                "gluehhammer", "pumpgun", "doppelflinte", "adrenalinspritze"}) {
            LIEBLING.put(id, Klasse.BRECHER);
        }
        for (String id : new String[]{"sturmgewehr", "kalaschnikow", "jagdgewehr", "praezisionsgewehr", "mg", "karabiner", "patronengurt"}) {
            LIEBLING.put(id, Klasse.SCHUETZE);
        }
        for (String id : new String[]{"p9", "revolver", "betaeubungspistole", "sanitaetstasche"}) {
            LIEBLING.put(id, Klasse.SANITAETER);
        }
        for (String id : new String[]{"rohrpistole", "rohrgewehr", "nagelpistole", "armbrust", "stoersender"}) {
            LIEBLING.put(id, Klasse.TECHNIKER);
        }
        for (String id : new String[]{"mp9", "kampfmesser", "machete", "speer", "wurfmesser", "fernglas", "mutantenklinge"}) {
            LIEBLING.put(id, Klasse.SPAEHER);
        }
    }

    private Klassenvorteile() {
    }

    public static void registrieren() {
        NeoForge.EVENT_BUS.addListener(Klassenvorteile::nahkampf);
        NeoForge.EVENT_BUS.addListener(Klassenvorteile::satzTick);
    }

    /** Lieblingsklasse einer Waffe/eines Gegenstands (null = keine). */
    public static Klasse liebling(Item item) {
        var key = BuiltInRegistries.ITEM.getKey(item);
        return "deadzonemc".equals(key.getNamespace()) ? LIEBLING.get(key.getPath()) : null;
    }

    /** Lieblingsklasse eines Rüstungssatzes (null = keine). */
    public static Klasse liebling(Satz s) {
        return s.klasse.isEmpty() ? null : Klasse.ausId(s.klasse);
    }

    /** Hat der Spieler die passende Klasse für diesen Gegenstand? (nur serverseitig zuverlässig) */
    public static boolean passt(Player p, ItemStack s) {
        Klasse l = liebling(s.getItem());
        return Klassen.hat(p, l);
    }

    /** Hat der Spieler die Lieblingsklasse des Satzes und trägt ihn vollständig? */
    public static boolean satz(Player p, Satz s) {
        Klasse l = liebling(s);
        return Klassen.hat(p, l) && Ausruestung.traegt(p, s) >= 4;
    }

    /** Schadensfaktor für Schusswaffen. */
    public static float schussSchaden(Player p, ItemStack waffe) {
        if (!passt(p, waffe)) {
            return 1F;
        }
        return liebling(waffe.getItem()) == Klasse.TECHNIKER ? 1.2F : 1.15F;
    }

    /** Streuungsfaktor (Schütze mit Gewehr: −20 %). */
    public static float streuung(Player p, ItemStack waffe) {
        float f = passt(p, waffe) && liebling(waffe.getItem()) == Klasse.SCHUETZE ? 0.8F : 1F;
        return Klassen.kombi(p, Klasse.SCHUETZE, Klasse.SPAEHER) ? f * 0.8F : f; // Scharfschütze
    }

    /** Nachladefaktor: Sanitäter mit Pistole −35 %, Schütze im vollen Taktisch-Satz −20 %. */
    public static float nachladen(Player p, ItemStack waffe) {
        float f = passt(p, waffe) && liebling(waffe.getItem()) == Klasse.SANITAETER ? 0.65F : 1F;
        if (Klassen.kombi(p, Klasse.SCHUETZE, Klasse.TECHNIKER)) {
            f *= 0.85F; // Waffenschmied
        }
        if (satz(p, Satz.TAKTISCH)) {
            f *= 0.8F;
        }
        return f;
    }

    /** Lärmfaktor: Späher mit seinen Waffen halb so laut. */
    public static float laerm(Player p, ItemStack waffe) {
        float f = passt(p, waffe) && liebling(waffe.getItem()) == Klasse.SPAEHER ? 0.5F : 1F;
        return Klassen.kombi(p, Klasse.TECHNIKER, Klasse.SPAEHER) ? f * 0.7F : f; // Saboteur
    }

    private static void nahkampf(LivingIncomingDamageEvent event) {
        if (event.getSource().getDirectEntity() instanceof Player p && event.getSource().getEntity() == p) {
            ItemStack s = p.getMainHandItem();
            if (passt(p, s) && !(s.getItem() instanceof de.deadzonemc.waffen.feuer.SchusswaffeItem)) {
                event.setAmount(event.getAmount() * (liebling(s.getItem()) == Klasse.BRECHER ? 1.25F : 1.15F));
            }
        }
        if (event.getEntity() instanceof Player p && event.getSource().getEntity() instanceof net.minecraft.world.entity.monster.Enemy
                && satz(p, Satz.SCHROTTPANZER)) {
            event.setAmount(event.getAmount() * 0.85F); // zusätzlich zum Satzbonus
        }
    }

    private static void satzTick(PlayerTickEvent.Post event) {
        Player p = event.getEntity();
        if (p.level().isClientSide || p.tickCount % 40 != 7) {
            return;
        }
        if (satz(p, Satz.SANITAETER)) {
            for (Player n : p.level().getEntitiesOfClass(Player.class, p.getBoundingBox().inflate(6), x -> x != p && x.isAlive())) {
                if (n.getHealth() < n.getMaxHealth()) {
                    n.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0, true, false, true));
                }
            }
        }
        if (satz(p, Satz.SCHUTZANZUG)) {
            p.removeEffect(MobEffects.POISON);
            p.removeEffect(MobEffects.WITHER);
        }
        var r = p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE);
        var id = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("deadzonemc", "klasse_schrott");
        if (r != null) {
            boolean soll = satz(p, Satz.SCHROTTPANZER);
            if (soll && !r.hasModifier(id)) {
                r.addTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(id, 0.5,
                        net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE));
            } else if (!soll && r.hasModifier(id)) {
                r.removeModifier(id);
            }
        }
    }
}
