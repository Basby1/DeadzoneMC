package de.deadzonemc.monster.boss;

import de.deadzonemc.monster.MonsterTypen;
import de.deadzonemc.monster.Nebelwesen;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** Riesige Gestalt ohne Gesicht, nur im dichten Nebel. Gebiert Nebelwesen, vergeht, wenn der Nebel geht. */
public class Nebelmutter extends Boss {

    public Nebelmutter(EntityType<? extends Monster> typ, Level level) {
        super(typ, level, "nebelmutter");
    }

    public static AttributeSupplier.Builder attribute() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 260)
                .add(Attributes.MOVEMENT_SPEED, 0.24)
                .add(Attributes.ATTACK_DAMAGE, 12)
                .add(Attributes.FOLLOW_RANGE, 64)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.STEP_HEIGHT, 2.0);
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
    protected int auftrittDauer() {
        return 80;
    }

    @Override
    protected String untertitel() {
        return "Der Nebel hat eine Mutter.";
    }

    @Override
    protected int faehigkeitTakt() {
        return 400;
    }

    @Override
    protected void faehigkeit(ServerLevel level, LivingEntity ziel) {
        triggerAnim("aktion", "special");
        int da = level.getEntitiesOfClass(Nebelwesen.class, getBoundingBox().inflate(32)).size();
        for (int i = da; i < 6 && i < da + 3; i++) {
            Nebelwesen n = MonsterTypen.NEBELWESEN.get().create(level);
            if (n == null) {
                break;
            }
            n.moveTo(getX() + random.nextGaussian() * 3, getY(), getZ() + random.nextGaussian() * 3, random.nextFloat() * 360, 0);
            n.finalizeSpawn(level, level.getCurrentDifficultyAt(blockPosition()), MobSpawnType.MOB_SUMMONED, null);
            n.setTarget(ziel);
            level.addFreshEntity(n);
            level.sendParticles(ParticleTypes.CLOUD, n.getX(), n.getY() + 1, n.getZ(), 20, 0.5, 1, 0.5, 0.02);
        }
        for (Player p : level.getEntitiesOfClass(Player.class, getBoundingBox().inflate(20))) {
            p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 120, 0));
        }
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (level() instanceof ServerLevel sl && tickCount % 40 == 0 && !dichterNebel(sl)) {
            for (Player p : sl.getEntitiesOfClass(Player.class, getBoundingBox().inflate(48))) {
                p.sendSystemMessage(Component.literal("Der Nebel lichtet sich … die Nebelmutter ist fort.")
                        .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
            }
            sl.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 3, getZ(), 120, 1.5, 3, 1.5, 0.05);
            discard();
        }
    }

    @Override
    protected boolean kannAnstuermen() {
        return false;
    }
}
