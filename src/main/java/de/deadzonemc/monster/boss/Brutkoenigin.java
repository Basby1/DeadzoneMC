package de.deadzonemc.monster.boss;

import de.deadzonemc.monster.MonsterTypen;
import de.deadzonemc.monster.Schwarm;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

/** Tief in den Höhlen: ein riesiger Eiersack auf Spinnenbeinen, der ständig Schwärmlinge ausstößt. */
public class Brutkoenigin extends Boss {

    public Brutkoenigin(EntityType<? extends Monster> typ, Level level) {
        super(typ, level, "brutkoenigin");
    }

    public static AttributeSupplier.Builder attribute() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 240)
                .add(Attributes.MOVEMENT_SPEED, 0.24)
                .add(Attributes.ATTACK_DAMAGE, 10)
                .add(Attributes.FOLLOW_RANGE, 40)
                .add(Attributes.ARMOR, 8)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.9)
                .add(Attributes.STEP_HEIGHT, 1.5);
    }

    @Override
    protected boolean hatSpezial() {
        return true;
    }

    @Override
    protected boolean hatLauf() {
        return false;
    }

    @Override
    protected String untertitel() {
        return "Aus der Tiefe kriecht die Brut.";
    }

    @Override
    protected int faehigkeitTakt() {
        return 200;
    }

    @Override
    protected void faehigkeit(ServerLevel level, LivingEntity ziel) {
        triggerAnim("aktion", "special");
        playSound(de.deadzonemc.monster.MonsterSounds.holen(art, "legen"), 2.5F, 0.8F);
        int da = level.getEntitiesOfClass(Schwarm.class, getBoundingBox().inflate(24)).size();
        for (int i = 0; i < 4 && da + i < 16; i++) {
            Schwarm s = MonsterTypen.SCHWARM.get().create(level);
            if (s == null) {
                break;
            }
            double w = getYRot() * Math.PI / 180 + Math.PI / 2;
            s.moveTo(getX() + Math.cos(w) * 2.5 + random.nextGaussian() * 0.5, getY() + 0.5,
                    getZ() + Math.sin(w) * 2.5 + random.nextGaussian() * 0.5, random.nextFloat() * 360, 0);
            s.finalizeSpawn(level, level.getCurrentDifficultyAt(blockPosition()), MobSpawnType.MOB_SUMMONED, null);
            s.setTarget(ziel);
            level.addFreshEntity(s);
            level.sendParticles(ParticleTypes.ITEM_SLIME, s.getX(), s.getY() + 0.3, s.getZ(), 10, 0.3, 0.2, 0.3, 0.05);
        }
    }

    @Override
    protected net.minecraft.core.particles.ParticleOptions kampfPartikel() {
        return net.minecraft.core.particles.ParticleTypes.MYCELIUM;
    }
}
