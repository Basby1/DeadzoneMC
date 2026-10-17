package de.deadzonemc.monster;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

/**
 * Infizierter Soldat: Helm und Schutzweste schlucken Körpertreffer. Kugeln in den Kopf wirken voll und
 * mehr, alles andere prallt zum Großteil ab.
 */
public class GefallenerSoldat extends DeadzoneMonster {

    public GefallenerSoldat(EntityType<? extends Monster> typ, Level level) {
        super(typ, level, "gefallener_soldat");
    }

    public static AttributeSupplier.Builder attribute() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 30)
                .add(Attributes.MOVEMENT_SPEED, 0.2)
                .add(Attributes.ATTACK_DAMAGE, 6)
                .add(Attributes.FOLLOW_RANGE, 36)
                .add(Attributes.ARMOR, 6)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.4);
    }

    @Override
    public boolean hurt(DamageSource quelle, float menge) {
        Entity direkt = quelle.getDirectEntity();
        if (!level().isClientSide && direkt != null && direkt != quelle.getEntity()) {
            // Geschoss: Kopf oder Weste?
            boolean kopf = direkt.getY() >= getEyeY() - 0.35;
            menge *= kopf ? 1.4F : 0.35F;
            if (!kopf && level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.CRIT, getX(), getY() + 1.2, getZ(), 6, 0.2, 0.2, 0.2, 0.2);
            }
        } else if (!level().isClientSide && quelle.getEntity() != null) {
            menge *= 0.7F; // Nahkampf gegen die Weste
        }
        return super.hurt(quelle, menge);
    }
}
