package de.deadzonemc.monster;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** Flieht, sobald er einen Spieler sieht, und schreit dabei alles in der Nähe herbei. */
public class Schreier extends DeadzoneMonster {

    private int abklingen = 60;
    private int schreit = 0;

    public Schreier(EntityType<? extends Monster> typ, Level level) {
        super(typ, level, "schreier");
    }

    public static AttributeSupplier.Builder attribute() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 12)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 2)
                .add(Attributes.FOLLOW_RANGE, 48);
    }

    @Override
    protected boolean hatSpezial() {
        return true;
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(2, new AvoidEntityGoal<>(this, Player.class, 12.0F, 1.0, 1.35,
                e -> e instanceof Player p && echterSpieler(p) && getHealth() > 4));
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (schreit > 0) {
            schreit--;
            getNavigation().stop();
            return;
        }
        if (abklingen > 0) {
            abklingen--;
            return;
        }
        Player p = level().getNearestPlayer(this, 22);
        if (p != null && echterSpieler(p) && hasLineOfSight(p)) {
            schreien(p);
        }
    }

    private void schreien(Player p) {
        abklingen = 400 + random.nextInt(200);
        schreit = 36;
        getNavigation().stop();
        getLookControl().setLookAt(p);
        triggerAnim("aktion", "special");
        level().playSound(null, getX(), getY(), getZ(), MonsterSounds.holen(art, "schrei"), SoundSource.HOSTILE, 5.0F,
                0.9F + random.nextFloat() * 0.2F);
        // Alles im Umkreis kommt herbei
        for (Monster m : level().getEntitiesOfClass(Monster.class, getBoundingBox().inflate(40))) {
            if (m != this && m.getTarget() == null) {
                m.setTarget(p);
                m.getNavigation().moveTo(p, 1.1);
            }
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Abklingen", abklingen);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        abklingen = tag.getInt("Abklingen");
    }
}
