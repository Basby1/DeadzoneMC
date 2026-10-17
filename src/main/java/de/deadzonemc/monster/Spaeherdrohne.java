package de.deadzonemc.monster;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Verwilderte Militärdrohne: kreist über Städten und Straßen und sucht mit rotem Scheinwerfer. Wer zwei
 * Sekunden im Lichtkegel bleibt, löst Alarm aus – alle Infizierten in der Nähe kommen. Deckung oder
 * Abschießen helfen. Wertvolle Elektronik als Beute.
 */
public class Spaeherdrohne extends Flieger {

    private static final DustParticleOptions ROT = new DustParticleOptions(new Vector3f(1.0F, 0.15F, 0.1F), 1.2F);
    private Vec3 kurs;
    private Player erfasst;
    private int erfassung;
    private int alarmPause;

    public Spaeherdrohne(EntityType<? extends Monster> typ, Level level) {
        super(typ, level, "spaeherdrohne");
    }

    public static AttributeSupplier.Builder attribute() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 10)
                .add(Attributes.FLYING_SPEED, 0.5)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FOLLOW_RANGE, 32)
                .add(Attributes.ARMOR, 2);
    }

    @Override
    protected void registerGoals() {
        // eigene Steuerung in customServerAiStep
    }

    @Override
    protected boolean hatSpezial() {
        return true;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!(level() instanceof ServerLevel sl)) {
            return;
        }
        if (alarmPause > 0) {
            alarmPause--;
        }
        if (erfasst == null || !erfasst.isAlive() || erfasst.distanceToSqr(this) > 40 * 40) {
            erfasst = null;
            erfassung = 0;
            // Patrouille: in 10–14 Blöcken Höhe über wechselnde Punkte
            if (kurs == null || position().distanceToSqr(kurs) < 9 || tickCount % 200 == 0) {
                kurs = luftpunkt(getX() + random.nextGaussian() * 24, getZ() + random.nextGaussian() * 24, 10 + random.nextInt(5));
            }
            fliegeZu(kurs, 0.6);
            if (tickCount % 10 == 0) {
                Player p = sl.getNearestPlayer(this, 24);
                if (p != null && echterSpieler(p) && hasLineOfSight(p)) {
                    erfasst = p;
                    triggerAnim("aktion", "special");
                }
            }
        } else {
            Vec3 ueber = luftpunkt(erfasst.getX() + 5, erfasst.getZ() + 5, 9);
            fliegeZu(ueber, 0.8);
            getLookControl().setLookAt(erfasst);
            boolean sicht = hasLineOfSight(erfasst);
            erfassung = sicht ? erfassung + 1 : Math.max(0, erfassung - 2);
            if (erfassung == 0 && !sicht) {
                erfasst = null; // Deckung hat geholfen
                return;
            }
            if (sicht && tickCount % 3 == 0) {
                Vec3 von = getEyePosition();
                Vec3 nach = erfasst.position();
                for (int i = 2; i <= 10; i++) {
                    Vec3 p = von.lerp(nach, i / 10.0);
                    sl.sendParticles(ROT, p.x, p.y, p.z, 1, 0.05 * i, 0.05 * i, 0.05 * i, 0);
                }
            }
            if (erfassung >= 40 && alarmPause == 0) {
                alarm(sl, erfasst);
            }
        }
    }

    private void alarm(ServerLevel sl, Player p) {
        alarmPause = 600;
        triggerAnim("aktion", "attack");
        playSound(MonsterSounds.holen(art, "alarm"), 6.0F, 1.0F);
        p.displayClientMessage(Component.literal("Die Drohne hat dich erfasst – sie kommen!").withStyle(ChatFormatting.RED), true);
        for (Monster m : sl.getEntitiesOfClass(Monster.class, getBoundingBox().inflate(48, 24, 48), m -> m != this && !(m instanceof Flieger))) {
            if (m.getTarget() == null) {
                m.setTarget(p);
            }
        }
    }

    @Override
    public void die(DamageSource quelle) {
        super.die(quelle);
        if (level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.EXPLOSION, getX(), getY(), getZ(), 2, 0.2, 0.2, 0.2, 0);
            sl.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY(), getZ(), 15, 0.3, 0.3, 0.3, 0.02);
        }
    }
}
