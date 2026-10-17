package de.deadzonemc.monster;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Riesige Motte, die nachts auf Licht zufliegt: auf Spieler mit Fackel oder Laterne in der Hand. Ihr Staub
 * blendet. Gegenmittel: Licht weg – oder eine abgestellte Laterne als Köder, die sie lieber umkreist.
 */
public class Lichtmotte extends Flieger {

    private BlockPos koeder;
    private int staub;
    private double winkel;

    public Lichtmotte(EntityType<? extends Monster> typ, Level level) {
        super(typ, level, "lichtmotte");
    }

    public static AttributeSupplier.Builder attribute() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 8)
                .add(Attributes.FLYING_SPEED, 0.55)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 2)
                .add(Attributes.FOLLOW_RANGE, 32);
    }

    @Override
    protected void registerGoals() {
    }

    public static boolean leuchtet(ItemStack s) {
        return s.getItem() instanceof BlockItem b && b.getBlock().defaultBlockState().getLightEmission() >= 10;
    }

    private static boolean mitLicht(Player p) {
        return leuchtet(p.getMainHandItem()) || leuchtet(p.getOffhandItem());
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!(level() instanceof ServerLevel sl)) {
            return;
        }
        if (sl.isDay() && getTarget() == null && tickCount % 100 == 0 && random.nextInt(3) == 0) {
            discard(); // bei Tag verschwindet sie
            return;
        }
        if (tickCount % 20 == 0) {
            Player p = sl.getNearestPlayer(getX(), getY(), getZ(), 28, e -> e instanceof Player s && echterSpieler(s) && mitLicht(s));
            setTarget(p);
            if (p == null && (koeder == null || tickCount % 100 == 0)) {
                koeder = lichtSuchen(sl);
            }
        }
        if (getTarget() instanceof Player p && mitLicht(p)) {
            Vec3 ziel = p.getEyePosition().add(Math.cos(tickCount * 0.15) * 1.5, 0.5, Math.sin(tickCount * 0.15) * 1.5);
            fliegeZu(ziel, 0.9);
            if (distanceToSqr(p) < 3 * 3 && ++staub >= 40) {
                staub = 0;
                triggerAnim("aktion", "attack");
                sl.sendParticles(ParticleTypes.WHITE_ASH, p.getX(), p.getEyeY(), p.getZ(), 40, 0.6, 0.6, 0.6, 0.02);
                p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 80, 0));
                p.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 60, 0));
                p.hurt(damageSources().mobAttack(this), 2);
            }
        } else if (koeder != null) {
            winkel += 0.12;
            fliegeZu(new Vec3(koeder.getX() + 0.5 + Math.cos(winkel) * 2, koeder.getY() + 1.5, koeder.getZ() + 0.5 + Math.sin(winkel) * 2), 0.6);
        } else if (tickCount % 60 == 0) {
            fliegeZu(luftpunkt(getX() + random.nextGaussian() * 10, getZ() + random.nextGaussian() * 10, 4 + random.nextInt(4)), 0.5);
        }
        if (tickCount % 30 == 0) {
            playSound(MonsterSounds.holen(art, "flattern"), 0.6F, 1.0F);
        }
    }

    /** Sucht eine helle Lichtquelle (Laterne, Fackel …) in der Nähe – der Köder. */
    private BlockPos lichtSuchen(ServerLevel sl) {
        BlockPos mitte = blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(mitte.offset(-8, -5, -8), mitte.offset(8, 5, 8))) {
            if (sl.getBlockState(pos).getLightEmission() >= 12) {
                return pos.immutable();
            }
        }
        return null;
    }
}
