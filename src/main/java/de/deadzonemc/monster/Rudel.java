package de.deadzonemc.monster;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

/**
 * Rudelführer: Jedes Rudel Seuchenhunde und Trugrüden hat einen größeren, stärkeren Leithund. Fällt er,
 * zerstreut sich das Rudel für eine Weile – wer den Anführer zuerst ausschaltet, hat es leichter.
 */
public final class Rudel {

    private static final String ALPHA = "deadzonemc_alpha";
    private static final String FLUCHT = "deadzonemc_flucht_bis";
    private static final ResourceLocation GROESSE = ResourceLocation.fromNamespaceAndPath("deadzonemc", "alpha_groesse");
    private static final ResourceLocation LEBEN = ResourceLocation.fromNamespaceAndPath("deadzonemc", "alpha_leben");
    private static final ResourceLocation KRAFT = ResourceLocation.fromNamespaceAndPath("deadzonemc", "alpha_kraft");

    private Rudel() {
    }

    public static void registrieren() {
        NeoForge.EVENT_BUS.addListener(Rudel::erscheinen);
        NeoForge.EVENT_BUS.addListener(Rudel::tod);
        NeoForge.EVENT_BUS.addListener(Rudel::zielen);
    }

    private static boolean rudeltier(Mob m) {
        String id = BuiltInRegistries.ENTITY_TYPE.getKey(m.getType()).getPath();
        return id.equals("seuchenhund") || id.equals("truegruede");
    }

    private static void erscheinen(EntityJoinLevelEvent event) {
        if (event.loadedFromDisk() || !(event.getLevel() instanceof ServerLevel sl) || !(event.getEntity() instanceof Mob m)
                || !rudeltier(m) || m.getPersistentData().contains(ALPHA)) {
            return;
        }
        // Erster seiner Art in der Gegend → Leithund
        boolean hatFuehrer = !sl.getEntitiesOfClass(Mob.class, m.getBoundingBox().inflate(24, 8, 24),
                o -> o != m && o.getType() == m.getType() && o.getPersistentData().getBoolean(ALPHA)).isEmpty();
        m.getPersistentData().putBoolean(ALPHA, !hatFuehrer);
        if (!hatFuehrer) {
            var g = m.getAttribute(Attributes.SCALE);
            if (g != null && !g.hasModifier(GROESSE)) {
                g.addPermanentModifier(new AttributeModifier(GROESSE, 0.3, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
            }
            var l = m.getAttribute(Attributes.MAX_HEALTH);
            if (l != null && !l.hasModifier(LEBEN)) {
                l.addPermanentModifier(new AttributeModifier(LEBEN, 0.6, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
                m.setHealth(m.getMaxHealth());
            }
            var k = m.getAttribute(Attributes.ATTACK_DAMAGE);
            if (k != null && !k.hasModifier(KRAFT)) {
                k.addPermanentModifier(new AttributeModifier(KRAFT, 0.3, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
            }
        }
    }

    private static void tod(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Mob fuehrer) || !(fuehrer.level() instanceof ServerLevel sl)
                || !fuehrer.getPersistentData().getBoolean(ALPHA)) {
            return;
        }
        long bis = sl.getGameTime() + 400;
        sl.playSound(null, fuehrer.blockPosition(), net.minecraft.sounds.SoundEvents.WOLF_WHINE,
                net.minecraft.sounds.SoundSource.HOSTILE, 2.0F, 0.6F);
        for (Mob m : sl.getEntitiesOfClass(Mob.class, fuehrer.getBoundingBox().inflate(28, 10, 28),
                o -> o != fuehrer && o.getType() == fuehrer.getType() && o.isAlive())) {
            m.getPersistentData().putLong(FLUCHT, bis);
            m.setTarget(null);
            Vec3 weg = m.position().subtract(fuehrer.position()).multiply(1, 0, 1);
            if (weg.lengthSqr() < 0.01) {
                weg = new Vec3(1, 0, 0);
            }
            Vec3 ziel = m.position().add(weg.normalize().scale(24));
            m.getNavigation().moveTo(ziel.x, ziel.y, ziel.z, 1.5);
        }
    }

    private static void zielen(LivingChangeTargetEvent event) {
        if (event.getEntity() instanceof Mob m && event.getNewAboutToBeSetTarget() != null
                && m.getPersistentData().getLong(FLUCHT) > m.level().getGameTime()) {
            event.setCanceled(true);
        }
    }
}
