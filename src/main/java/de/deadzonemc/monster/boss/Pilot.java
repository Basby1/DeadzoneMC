package de.deadzonemc.monster.boss;

import de.deadzonemc.monster.GefallenerSoldat;
import de.deadzonemc.monster.MonsterTypen;
import de.deadzonemc.waffen.feuer.Feuerwaffen;
import de.deadzonemc.waffen.feuer.Kaliber;
import de.deadzonemc.waffen.feuer.WaffenArt;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Boss am abgestürzten Transporthubschrauber: der Pilot, mit dem Wrack verwachsen. Auf Abstand Feuerstöße aus dem
 * Bordgeschütz, aus der Nähe wirbelt er mit dem Rotorblatt herum (vorher kurz angekündigt). Ruft seine tote Crew.
 */
public class Pilot extends Boss {

    private int feuer;
    private int wirbelIn = -1;

    public Pilot(EntityType<? extends Monster> typ, Level level) {
        super(typ, level, "pilot");
    }

    public static AttributeSupplier.Builder attribute() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 280)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.ATTACK_DAMAGE, 13)
                .add(Attributes.FOLLOW_RANGE, 48)
                .add(Attributes.ARMOR, 12)
                .add(Attributes.ARMOR_TOUGHNESS, 4)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    protected boolean hatSpezial() {
        return true;
    }

    @Override
    protected int auftrittDauer() {
        return 60;
    }

    @Override
    protected String untertitel() {
        return "Mayday. Mayday. Niemand kommt.";
    }

    @Override
    protected boolean kannAnstuermen() {
        return false;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!(level() instanceof ServerLevel sl) || imAuftritt()) {
            return;
        }
        if (wirbelIn >= 0 && --wirbelIn < 0) {
            wirbeln(sl);
        }
        LivingEntity ziel = getTarget();
        if (ziel == null || wirbelIn >= 0) {
            return;
        }
        double d = distanceTo(ziel);
        boolean sicht = getSensing().hasLineOfSight(ziel);
        // Bordgeschütz: Feuerstöße auf Abstand (in jedem 50-Tick-Takt 20 Ticks Feuer)
        if (sicht && d > 5 && ++feuer % 5 == 0 && (feuer % 50) < 20) {
            triggerAnim("aktion", "attack");
            sl.playSound(null, getX(), getY(), getZ(), SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.HOSTILE, 3.0F, 0.7F);
            Vec3 von = getEyePosition().subtract(0, 0.6, 0);
            Vec3 nach = ziel.getEyePosition().subtract(0, 0.4, 0);
            for (int i = 1; i < 12; i++) {
                Vec3 p = von.lerp(nach, i / 12.0);
                sl.sendParticles(ParticleTypes.CRIT, p.x, p.y, p.z, 1, 0, 0, 0, 0);
            }
            sl.sendParticles(ParticleTypes.SMOKE, von.x, von.y, von.z, 3, 0.1, 0.1, 0.1, 0.01);
            if (random.nextDouble() < Math.max(0.25, 0.85 - d / 50.0)) {
                ziel.hurt(damageSources().mobAttack(this), 3.5F);
            }
        }
    }

    @Override
    protected int faehigkeitTakt() {
        return 150;
    }

    @Override
    protected void faehigkeit(ServerLevel level, LivingEntity ziel) {
        if (distanceTo(ziel) < 6) {
            // Ankündigung: Rotorblatt heben, Pfeifen – 0,75 s Zeit zum Zurückweichen
            wirbelIn = 15;
            getNavigation().stop();
            level.playSound(null, getX(), getY(), getZ(), de.deadzonemc.monster.MonsterSounds.holen(art, "wirbel"),
                    SoundSource.HOSTILE, 2.5F, 0.8F);
            level.sendParticles(new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f(0.9F, 0.1F, 0.1F), 1.5F),
                    getX(), getY() + 0.2, getZ(), 40, 2.5, 0.1, 2.5, 0);
            return;
        }
        // Auf Abstand: Leuchtsignal – die tote Crew steht wieder auf
        int crew = level.getEntitiesOfClass(GefallenerSoldat.class, getBoundingBox().inflate(24)).size();
        if (crew >= 3) {
            return;
        }
        level.playSound(null, getX(), getY(), getZ(), SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.HOSTILE, 3.0F, 0.6F);
        level.sendParticles(ParticleTypes.FLAME, getX(), getY() + 3, getZ(), 30, 0.2, 2.0, 0.2, 0.02);
        for (int i = 0; i < 2; i++) {
            GefallenerSoldat s = MonsterTypen.GEFALLENER_SOLDAT.get().create(level);
            if (s != null) {
                s.moveTo(getX() + random.nextGaussian() * 3, getY(), getZ() + random.nextGaussian() * 3, random.nextFloat() * 360, 0);
                if (level.noCollision(s)) {
                    s.finalizeSpawn(level, level.getCurrentDifficultyAt(blockPosition()), MobSpawnType.MOB_SUMMONED, null);
                    s.setTarget(ziel);
                    level.addFreshEntity(s);
                    level.sendParticles(ParticleTypes.SOUL, s.getX(), s.getY() + 1, s.getZ(), 12, 0.3, 0.6, 0.3, 0.02);
                }
            }
        }
    }

    private void wirbeln(ServerLevel level) {
        triggerAnim("aktion", "special");
        level.playSound(null, getX(), getY(), getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 3.0F, 0.5F);
        for (int i = 0; i < 16; i++) {
            double w = i * Math.PI / 8;
            level.sendParticles(ParticleTypes.SWEEP_ATTACK, getX() + Math.cos(w) * 3, getY() + 1.2, getZ() + Math.sin(w) * 3,
                    1, 0, 0, 0, 0);
        }
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(4.5, 1.5, 4.5),
                e -> e != this && e.isAlive() && !(e instanceof GefallenerSoldat))) {
            if (e.hurt(damageSources().mobAttack(this), 14F)) {
                Vec3 weg = e.position().subtract(position()).normalize();
                e.push(weg.x * 1.6, 0.5, weg.z * 1.6);
                e.hurtMarked = true;
            }
        }
    }

    @Override
    protected void beute(ServerLevel level) {
        if (random.nextInt(3) == 0) {
            spawnAtLocation(Feuerwaffen.waffe(WaffenArt.MG, 0));
        }
        spawnAtLocation(Feuerwaffen.munition(Kaliber.F762, 40 + random.nextInt(41)));
    }

    @Override
    protected net.minecraft.core.particles.ParticleOptions kampfPartikel() {
        return ParticleTypes.SMALL_FLAME;
    }
}
