package de.deadzonemc.monster;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Psychologisch: trägt den Namen eines Spielers, folgt der Gruppe auf Abstand und ahmt Geräusche nach
 * (Schritte, Türen, Truhen, Schmerzlaute). Wer zu nah kommt oder ihn angreift, wird angefallen.
 */
public class Imitator extends DeadzoneMonster {

    private boolean entlarvt = false;
    private int nachahmen = 100;

    public Imitator(EntityType<? extends Monster> typ, Level level) {
        super(typ, level, "imitator");
    }

    public static AttributeSupplier.Builder attribute() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 20)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 5)
                .add(Attributes.FOLLOW_RANGE, 48);
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance schwierigkeit, MobSpawnType grund,
                                        SpawnGroupData daten) {
        SpawnGroupData erg = super.finalizeSpawn(level, schwierigkeit, grund, daten);
        List<? extends Player> spieler = level.getLevel().players();
        if (!spieler.isEmpty()) {
            setCustomName(Component.literal(spieler.get(random.nextInt(spieler.size())).getGameProfile().getName()));
            setCustomNameVisible(false);
        }
        return erg;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (entlarvt) {
            return;
        }
        setTarget(null);
        Player p = level().getNearestPlayer(this, 48);
        if (p == null || !echterSpieler(p)) {
            return;
        }
        double abstand = distanceTo(p);
        getLookControl().setLookAt(p);
        if (abstand < 7) {
            entlarven(p);
            return;
        }
        if (abstand > 22) {
            getNavigation().moveTo(p, 0.9);
        } else if (abstand < 14) {
            getNavigation().stop();
        }
        if (--nachahmen <= 0) {
            nachahmen = 80 + random.nextInt(160);
            nachahmen();
        }
    }

    private void nachahmen() {
        SoundEvent[] laute = {SoundEvents.WOODEN_DOOR_OPEN, SoundEvents.WOODEN_DOOR_CLOSE, SoundEvents.CHEST_OPEN,
                SoundEvents.PLAYER_HURT, SoundEvents.GENERIC_EAT, SoundEvents.ARROW_SHOOT, SoundEvents.PLAYER_BREATH};
        if (random.nextInt(3) == 0) {
            // ein paar Schritte auf dem Boden unter ihm
            BlockState boden = level().getBlockState(blockPosition().below());
            for (int i = 0; i < 3; i++) {
                level().playSound(null, getX(), getY(), getZ(), boden.getSoundType().getStepSound(), SoundSource.PLAYERS, 0.5F, 1.0F);
            }
            return;
        }
        level().playSound(null, getX(), getY(), getZ(), laute[random.nextInt(laute.length)], SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    private void entlarven(Player p) {
        entlarvt = true;
        setTarget(p);
    }

    @Override
    public boolean hurt(DamageSource quelle, float menge) {
        if (!level().isClientSide && quelle.getEntity() instanceof Player p) {
            entlarven(p);
        }
        return super.hurt(quelle, menge);
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState block) {
        // klingt wie ein Spieler
        playSound(block.getSoundType().getStepSound(), 0.15F, 1.0F);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Entlarvt", entlarvt);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entlarvt = tag.getBoolean("Entlarvt");
    }
}
