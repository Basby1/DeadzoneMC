package de.deadzonemc.monster.boss;

import de.deadzonemc.mensch.Ansehen;
import de.deadzonemc.mensch.Fraktion;
import de.deadzonemc.mensch.MenschTypen;
import de.deadzonemc.mensch.Soldat;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Anführer der Wacht im Exo-Panzer. Feuerstöße und Raketen; kommt mit Eskorte zu Todfeinden der Wacht. */
public class Kommandant extends Boss {

    private int feuer;

    public Kommandant(EntityType<? extends Monster> typ, Level level) {
        super(typ, level, "kommandant");
    }

    public static AttributeSupplier.Builder attribute() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 260)
                .add(Attributes.MOVEMENT_SPEED, 0.24)
                .add(Attributes.ATTACK_DAMAGE, 12)
                .add(Attributes.FOLLOW_RANGE, 56)
                .add(Attributes.ARMOR, 16)
                .add(Attributes.ARMOR_TOUGHNESS, 6)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.7));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 16.0F));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
                e -> e instanceof Player p && echterSpieler(p) && Ansehen.krieg(p, Fraktion.WACHT)));
    }

    @Override
    protected boolean hatSpezial() {
        return true;
    }

    @Override
    protected int auftrittDauer() {
        return 40;
    }

    @Override
    protected String untertitel() {
        return "Die Wacht vergisst nicht.";
    }

    @Override
    public void auftreten() {
        super.auftreten();
        if (level() instanceof ServerLevel sl) {
            for (int i = 0; i < 4; i++) {
                Soldat s = MenschTypen.SOLDAT.get().create(sl);
                if (s != null) {
                    s.moveTo(getX() + random.nextGaussian() * 3, getY(), getZ() + random.nextGaussian() * 3, getYRot(), 0);
                    s.finalizeSpawn(sl, sl.getCurrentDifficultyAt(blockPosition()), MobSpawnType.MOB_SUMMONED, null);
                    sl.addFreshEntity(s);
                }
            }
        }
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        LivingEntity ziel = getTarget();
        if (ziel == null || imAuftritt() || !(level() instanceof ServerLevel sl)) {
            return;
        }
        getLookControl().setLookAt(ziel, 30, 30);
        double d = distanceTo(ziel);
        boolean sicht = getSensing().hasLineOfSight(ziel);
        if (d > 14 || !sicht) {
            getNavigation().moveTo(ziel, 1.0);
        } else {
            getNavigation().stop();
        }
        if (sicht && ++feuer % 6 == 0 && (feuer % 48) < 24) { // Feuerstöße
            triggerAnim("aktion", "attack");
            sl.playSound(null, getX(), getY(), getZ(), SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.HOSTILE, 3.0F, 0.5F);
            Vec3 von = getEyePosition();
            Vec3 nach = ziel.getEyePosition().subtract(0, 0.4, 0);
            for (int i = 1; i < 12; i++) {
                Vec3 p = von.lerp(nach, i / 12.0);
                sl.sendParticles(ParticleTypes.CRIT, p.x, p.y, p.z, 1, 0, 0, 0, 0);
            }
            if (random.nextDouble() < Math.max(0.3, 0.9 - d / 50.0)) {
                ziel.hurt(damageSources().mobAttack(this), 4.0F);
            }
        }
    }

    @Override
    protected int faehigkeitTakt() {
        return 240;
    }

    @Override
    protected void faehigkeit(ServerLevel level, LivingEntity ziel) {
        if (!getSensing().hasLineOfSight(ziel)) {
            return;
        }
        triggerAnim("aktion", "special");
        level.playSound(null, getX(), getY(), getZ(), SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.HOSTILE, 3.0F, 0.6F);
        Vec3 z = ziel.position();
        level.explode(this, z.x, z.y, z.z, 2.5F, Level.ExplosionInteraction.NONE);
    }

    @Override
    protected void beute(ServerLevel level) {
        ItemStack waffe = de.deadzonemc.mensch.Haendler.tacz("modern_kinetic_gun", "GunId", "hk416d", 1);
        if (!waffe.isEmpty()) {
            spawnAtLocation(waffe);
            spawnAtLocation(de.deadzonemc.mensch.Haendler.tacz("ammo", "AmmoId", "556x45", 60));
        }
    }
}
