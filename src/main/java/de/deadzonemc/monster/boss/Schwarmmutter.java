package de.deadzonemc.monster.boss;

import de.deadzonemc.monster.Brut;
import de.deadzonemc.monster.DeadzoneMonster;
import de.deadzonemc.monster.MonsterTypen;
import de.deadzonemc.monster.Schwarm;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;

/**
 * Boss der Metro: aufgeblähte Rattenkönigin auf Spinnenbeinen mit glühendem Brutsack. Flutet die Tunnel Welle um
 * Welle mit ihrer Brut. Sie lebt im Dunkeln – in hellem Licht (Fackeln, Lampen, Leuchtfeuer) ist sie geblendet:
 * keine Brut, langsamer, und sie nimmt 50 % mehr Schaden.
 */
public class Schwarmmutter extends Boss {

    private boolean warGeblendet;

    public Schwarmmutter(EntityType<? extends Monster> typ, Level level) {
        super(typ, level, "schwarmmutter");
    }

    public static AttributeSupplier.Builder attribute() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 300)
                .add(Attributes.MOVEMENT_SPEED, 0.26)
                .add(Attributes.ATTACK_DAMAGE, 11)
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
        return "Die Tunnel gehören ihr.";
    }

    @Override
    protected int faehigkeitTakt() {
        return 170;
    }

    /** Hell genug, um sie zu blenden? (Kunstlicht zählt – Tageslicht gibt es im Tunnel nicht.) */
    public boolean geblendet() {
        return level().getBrightness(LightLayer.BLOCK, blockPosition().above()) >= 10;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (tickCount % 20 != 0 || !(level() instanceof ServerLevel sl)) {
            return;
        }
        boolean jetzt = geblendet();
        if (jetzt) {
            addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 0, false, false));
            sl.sendParticles(ParticleTypes.WHITE_ASH, getX(), getY() + 2, getZ(), 12, 1.2, 0.6, 1.2, 0.01);
        }
        if (jetzt && !warGeblendet) {
            for (ServerPlayer p : sl.getEntitiesOfClass(ServerPlayer.class, getBoundingBox().inflate(32))) {
                p.displayClientMessage(Component.translatable("boss.deadzonemc.schwarmmutter.licht")
                        .withStyle(ChatFormatting.YELLOW), true);
            }
            playSound(de.deadzonemc.monster.MonsterSounds.holen(art, "hurt"), 2.0F, 1.4F);
        }
        warGeblendet = jetzt;
    }

    @Override
    protected void faehigkeit(ServerLevel level, LivingEntity ziel) {
        if (geblendet()) {
            return; // im Licht keine Brut
        }
        triggerAnim("aktion", "special");
        playSound(de.deadzonemc.monster.MonsterSounds.holen(art, "legen"), 2.5F, 0.8F);
        int da = level.getEntitiesOfClass(Schwarm.class, getBoundingBox().inflate(24)).size();
        boolean raserei = getHealth() < getMaxHealth() * 0.5F;
        int anzahl = raserei ? 6 : 4;
        for (int i = 0; i < anzahl && da + i < 18; i++) {
            Schwarm s = MonsterTypen.SCHWARM.get().create(level);
            if (s != null) {
                setzen(level, s, ziel);
            }
        }
        if (raserei && level.getEntitiesOfClass(Brut.class, getBoundingBox().inflate(24)).size() < 2) {
            Brut b = MonsterTypen.BRUT.get().create(level);
            if (b != null) {
                setzen(level, b, ziel);
            }
        }
    }

    private void setzen(ServerLevel level, DeadzoneMonster m, LivingEntity ziel) {
        double w = getYRot() * Math.PI / 180 + Math.PI / 2;
        m.moveTo(getX() - Math.cos(w) * 2.0 + random.nextGaussian() * 0.8, getY() + 0.5,
                getZ() - Math.sin(w) * 2.0 + random.nextGaussian() * 0.8, random.nextFloat() * 360, 0);
        m.finalizeSpawn(level, level.getCurrentDifficultyAt(blockPosition()), MobSpawnType.MOB_SUMMONED, null);
        m.setTarget(ziel);
        level.addFreshEntity(m);
        level.sendParticles(ParticleTypes.ITEM_SLIME, m.getX(), m.getY() + 0.3, m.getZ(), 10, 0.3, 0.2, 0.3, 0.05);
    }

    @Override
    public boolean hurt(DamageSource quelle, float menge) {
        if (!level().isClientSide && geblendet()) {
            menge *= 1.5F;
        }
        return super.hurt(quelle, menge);
    }

    @Override
    protected net.minecraft.core.particles.ParticleOptions kampfPartikel() {
        return ParticleTypes.SQUID_INK;
    }
}
