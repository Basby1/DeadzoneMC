package de.deadzonemc.monster;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import org.joml.Vector3f;

/**
 * Mutationsstufen: Je länger die Welt besteht, desto öfter tauchen veränderte Infizierte auf.
 * <ul>
 * <li>Stufe 1 „mutiert“ (ab Tag 3): etwas größer, mehr Leben, härtere Schläge, schneller.</li>
 * <li>Stufe 2 „hochmutiert“ (ab Tag 12): deutlich größer und zäher, dunkelroter Dunst steigt von ihnen auf.</li>
 * </ul>
 * Die Chancen steigen langsam mit den Tagen und sind gedeckelt – normale Infizierte bleiben die Regel.
 * Mutierte lassen öfter Mutagen fallen. Bosse mutieren nicht.
 */
public final class Mutation {

    public static final String STUFE = "deadzonemc_mutation";
    private static final ResourceLocation GROESSE = ResourceLocation.fromNamespaceAndPath("deadzonemc", "mutation_groesse");
    private static final ResourceLocation LEBEN = ResourceLocation.fromNamespaceAndPath("deadzonemc", "mutation_leben");
    private static final ResourceLocation KRAFT = ResourceLocation.fromNamespaceAndPath("deadzonemc", "mutation_kraft");
    private static final ResourceLocation TEMPO = ResourceLocation.fromNamespaceAndPath("deadzonemc", "mutation_tempo");
    private static final DustParticleOptions DUNST = new DustParticleOptions(new Vector3f(0.45F, 0.05F, 0.08F), 1.2F);

    private Mutation() {
    }

    public static void registrieren() {
        NeoForge.EVENT_BUS.addListener(Mutation::erscheinen);
        NeoForge.EVENT_BUS.addListener(Mutation::beute);
    }

    public static int stufe(DeadzoneMonster m) {
        return m.getPersistentData().getInt(STUFE);
    }

    private static void erscheinen(EntityJoinLevelEvent event) {
        if (event.loadedFromDisk() || !(event.getLevel() instanceof ServerLevel sl) || !(event.getEntity() instanceof DeadzoneMonster m)
                || m instanceof de.deadzonemc.monster.boss.Boss || m.getPersistentData().contains(STUFE)) {
            return;
        }
        long tag = sl.getDayTime() / 24000L;
        float zwei = tag >= 12 ? Math.min(0.12F, (tag - 11) * 0.006F) : 0F;
        float eins = tag >= 3 ? Math.min(0.30F, (tag - 2) * 0.015F) : 0F;
        float w = sl.random.nextFloat();
        int stufe = w < zwei ? 2 : w < zwei + eins ? 1 : 0;
        m.getPersistentData().putInt(STUFE, stufe);
        if (stufe == 0) {
            return;
        }
        setzen(m, Attributes.SCALE, GROESSE, stufe == 2 ? 0.2 : 0.08);
        setzen(m, Attributes.MAX_HEALTH, LEBEN, stufe == 2 ? 0.8 : 0.3);
        setzen(m, Attributes.ATTACK_DAMAGE, KRAFT, stufe == 2 ? 0.4 : 0.2);
        setzen(m, Attributes.MOVEMENT_SPEED, TEMPO, stufe == 2 ? 0.15 : 0.08);
        m.setHealth(m.getMaxHealth());
    }

    private static void setzen(DeadzoneMonster m, net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> a,
                               ResourceLocation id, double wert) {
        var inst = m.getAttribute(a);
        if (inst != null && !inst.hasModifier(id)) {
            inst.addPermanentModifier(new AttributeModifier(id, wert, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        }
    }

    /** Hochmutierte dunsten dunkelrot (aus DeadzoneMonster.customServerAiStep). */
    public static void tick(DeadzoneMonster m) {
        if (m.tickCount % 30 == 0 && stufe(m) == 2 && m.level() instanceof ServerLevel sl) {
            sl.sendParticles(DUNST, m.getX(), m.getY() + m.getBbHeight() * 0.8, m.getZ(), 3, 0.3, 0.3, 0.3, 0);
        }
    }

    private static void beute(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof DeadzoneMonster m) || stufe(m) == 0 || m.getRandom().nextFloat() > (stufe(m) == 2 ? 0.6F : 0.2F)) {
            return;
        }
        var item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("deadzonemc", "mutagen"));
        event.getDrops().add(new ItemEntity(m.level(), m.getX(), m.getY(), m.getZ(), new ItemStack(item)));
    }
}
