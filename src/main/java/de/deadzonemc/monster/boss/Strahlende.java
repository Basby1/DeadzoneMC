package de.deadzonemc.monster.boss;

import de.deadzonemc.ModRegistry;
import de.deadzonemc.strahlung.Strahlung;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** Koloss aus der Strahlenzone: strahlt ständig, und sein Puls schleudert alles weg und verstrahlt es. */
public class Strahlende extends Boss {

    private static final DustParticleOptions GRUEN = new DustParticleOptions(new Vector3f(0.6F, 1.0F, 0.3F), 1.5F);

    public Strahlende(EntityType<? extends Monster> typ, Level level) {
        super(typ, level, "strahlende");
    }

    public static AttributeSupplier.Builder attribute() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 320)
                .add(Attributes.MOVEMENT_SPEED, 0.21)
                .add(Attributes.ATTACK_DAMAGE, 16)
                .add(Attributes.FOLLOW_RANGE, 48)
                .add(Attributes.ARMOR, 10)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
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
        return "Der Geigerzähler kreischt.";
    }

    @Override
    protected int faehigkeitTakt() {
        return 240;
    }

    @Override
    protected void faehigkeit(ServerLevel level, LivingEntity ziel) {
        triggerAnim("aktion", "special");
        playSound(de.deadzonemc.monster.MonsterSounds.holen(art, "puls"), 4.0F, 0.8F);
        for (int i = 0; i < 60; i++) {
            double w = i * Math.PI / 30;
            level.sendParticles(GRUEN, getX() + Math.cos(w) * 6, getY() + 1, getZ() + Math.sin(w) * 6, 2, 0.2, 0.3, 0.2, 0);
        }
        for (Player p : level.getEntitiesOfClass(Player.class, getBoundingBox().inflate(8))) {
            Strahlung.bestrahlen(p, 120);
            Vec3 weg = p.position().subtract(position()).normalize().scale(1.6);
            p.push(weg.x, 0.6, weg.z);
            p.hurtMarked = true;
            p.hurt(damageSources().mobAttack(this), 8);
        }
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (level() instanceof ServerLevel sl && tickCount % 20 == 0) {
            for (Player p : sl.getEntitiesOfClass(Player.class, getBoundingBox().inflate(12))) {
                Strahlung.bestrahlen(p, 6);
            }
            sl.sendParticles(GRUEN, getX(), getY() + 2.5, getZ(), 8, 1.2, 1.5, 1.2, 0);
        }
    }

    @Override
    protected void beute(ServerLevel level) {
        spawnAtLocation(new ItemStack(ModRegistry.ANTI_RAD.get(), 3));
        spawnAtLocation(new ItemStack(ModRegistry.GASMASKE.get()));
        spawnAtLocation(new ItemStack(ModRegistry.FILTER_ITEM.get(), 4));
    }

    @Override
    protected net.minecraft.core.particles.ParticleOptions kampfPartikel() {
        return net.minecraft.core.particles.ParticleTypes.GLOW;
    }
}
