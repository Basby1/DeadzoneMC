package de.deadzonemc.monster;

import de.deadzonemc.DeadzoneMC;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.BreakDoorGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

/** Häufigster Gegner: schlurft ziellos, sprintet plötzlich los, bricht auf „Schwer“ Türen auf. */
public class Wanderer extends DeadzoneMonster {

    private static final ResourceLocation SPRINT = ResourceLocation.fromNamespaceAndPath(DeadzoneMC.MODID, "sprint");
    private int sprint = 0;

    public Wanderer(EntityType<? extends Monster> typ, Level level) {
        super(typ, level, "wanderer");
        if (getNavigation() instanceof GroundPathNavigation nav) {
            nav.setCanOpenDoors(true);
        }
    }

    public static AttributeSupplier.Builder attribute() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 22)
                .add(Attributes.MOVEMENT_SPEED, 0.21)
                .add(Attributes.ATTACK_DAMAGE, 4)
                .add(Attributes.FOLLOW_RANGE, 40)
                .add(Attributes.ARMOR, 2);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(2, new BreakDoorGoal(this, d -> d == Difficulty.HARD));
        goalSelector.addGoal(5, new HerdeGoal(this));
    }

    private int alarmPause = 0;

    /**
     * Rudel: Wer einen Spieler entdeckt, reißt die Infizierten in der Nähe mit. Nur eine Stufe weit, damit
     * nicht die ganze Stadt auf einmal losrennt – aber eine Straße voll reicht.
     */
    @Override
    public void setTarget(@Nullable LivingEntity ziel) {
        boolean neu = ziel instanceof Player && getTarget() != ziel;
        super.setTarget(ziel);
        if (neu && !level().isClientSide && alarmPause <= 0) {
            alarmPause = 100;
            for (Wanderer w : level().getEntitiesOfClass(Wanderer.class, getBoundingBox().inflate(18, 6, 18),
                    w -> w != this && w.getTarget() == null && w.isAlive())) {
                w.alarmPause = 100;
                w.setTargetOhneAlarm(ziel);
            }
        }
    }

    private void setTargetOhneAlarm(LivingEntity ziel) {
        super.setTarget(ziel);
    }

    /** Ohne Ziel zieht es Wanderer zu ihresgleichen – so entstehen schlurfende Gruppen statt Einzelgänger. */
    static class HerdeGoal extends Goal {
        private final Wanderer w;
        private Wanderer anfuehrer;

        HerdeGoal(Wanderer w) {
            this.w = w;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (w.getTarget() != null || w.getRandom().nextInt(60) != 0) {
                return false;
            }
            anfuehrer = null;
            double best = Double.MAX_VALUE;
            for (Wanderer o : w.level().getEntitiesOfClass(Wanderer.class, w.getBoundingBox().inflate(20, 4, 20), o -> o != w)) {
                double d = o.distanceToSqr(w);
                if (d > 36 && d < best) {
                    best = d;
                    anfuehrer = o;
                }
            }
            return anfuehrer != null;
        }

        @Override
        public void start() {
            w.getNavigation().moveTo(anfuehrer, 0.8);
        }

        @Override
        public boolean canContinueToUse() {
            return w.getTarget() == null && anfuehrer.isAlive() && anfuehrer.distanceToSqr(w) > 9 && !w.getNavigation().isDone();
        }

        @Override
        public void stop() {
            anfuehrer = null;
        }
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (alarmPause > 0) {
            alarmPause--;
        }
        AttributeInstance tempo = getAttribute(Attributes.MOVEMENT_SPEED);
        if (tempo == null) {
            return;
        }
        if (sprint > 0) {
            if (--sprint == 0) {
                tempo.removeModifier(SPRINT);
            }
        } else if (getTarget() != null && random.nextInt(140) == 0) {
            // ohne Vorwarnung lossprinten
            sprint = 40 + random.nextInt(40);
            if (!tempo.hasModifier(SPRINT)) {
                tempo.addTransientModifier(new AttributeModifier(SPRINT, 0.35, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
            }
        }
    }
}
