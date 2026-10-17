package de.deadzonemc.monster.boss;

import de.deadzonemc.monster.DeadzoneMonster;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Führt eine Herde Infizierter übers Land. Seine Glocke hetzt die ganze Herde auf ein Ziel. Fällt er,
 * zerstreut sich die Herde.
 */
public class Hirte extends Boss {

    public Hirte(EntityType<? extends Monster> typ, Level level) {
        super(typ, level, "hirte");
    }

    public static AttributeSupplier.Builder attribute() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 200)
                .add(Attributes.MOVEMENT_SPEED, 0.23)
                .add(Attributes.ATTACK_DAMAGE, 9)
                .add(Attributes.FOLLOW_RANGE, 56)
                .add(Attributes.ARMOR, 4)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6);
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
        return 40;
    }

    @Override
    protected String untertitel() {
        return "Hörst du die Glocke? Dann ist es zu spät.";
    }

    @Override
    protected int faehigkeitTakt() {
        return 300;
    }

    private List<DeadzoneMonster> herde() {
        return level().getEntitiesOfClass(DeadzoneMonster.class, getBoundingBox().inflate(40), m -> m != this && !(m instanceof Boss));
    }

    @Override
    protected void faehigkeit(ServerLevel level, LivingEntity ziel) {
        triggerAnim("aktion", "special");
        playSound(de.deadzonemc.monster.MonsterSounds.holen(art, "glocke"), 5.0F, 0.8F);
        for (DeadzoneMonster m : herde()) {
            m.setTarget(ziel);
        }
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (tickCount % 60 == 0 && getTarget() == null) {
            // ohne Ziel folgt die Herde ihrem Hirten
            for (DeadzoneMonster m : herde()) {
                if (m.getTarget() == null && m.distanceToSqr(this) > 64) {
                    m.getNavigation().moveTo(this, 0.9);
                }
            }
        }
        if (tickCount % 200 == 0) {
            playSound(de.deadzonemc.monster.MonsterSounds.holen(art, "glocke"), 2.0F, 1.0F);
        }
    }

    @Override
    public void die(DamageSource quelle) {
        if (level() instanceof ServerLevel sl) {
            for (DeadzoneMonster m : herde()) {
                m.setTarget(null);
                double w = random.nextDouble() * Math.PI * 2;
                m.getNavigation().moveTo(m.getX() + Math.cos(w) * 40, m.getY(), m.getZ() + Math.sin(w) * 40, 1.2);
            }
            for (Player p : sl.getEntitiesOfClass(Player.class, getBoundingBox().inflate(64))) {
                p.sendSystemMessage(Component.literal("Die Glocke verstummt. Die Herde zerstreut sich.")
                        .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
            }
        }
        super.die(quelle);
    }
}
