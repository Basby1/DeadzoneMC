package de.deadzonemc.monster;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

import java.util.Optional;
import java.util.UUID;

/**
 * Der eigene Zombie: Wer stirbt, steht wenig später als Infizierter wieder auf – mit seinem Gesicht, seiner Rüstung
 * und seiner Waffe in der Hand – und bewacht die eigene Leiche. Wer an die Ausrüstung will, muss erst sein altes
 * Ich erledigen. (Die Gegenstände selbst bleiben in der Leiche; der Wiedergänger trägt nur Abbilder.)
 */
public class Wiedergaenger extends Wanderer {

    private static final EntityDataAccessor<Optional<UUID>> VORBILD = SynchedEntityData.defineId(Wiedergaenger.class,
            EntityDataSerializers.OPTIONAL_UUID);

    public Wiedergaenger(EntityType<? extends Monster> typ, Level level) {
        super(typ, level);
        setPersistenceRequired();
    }

    public static void registrieren() {
        NeoForge.EVENT_BUS.addListener(Wiedergaenger::tod);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(VORBILD, Optional.empty());
    }

    public Optional<UUID> vorbild() {
        return entityData.get(VORBILD);
    }

    private static void tod(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || !(p.level() instanceof ServerLevel sl) || p.isSpectator()) {
            return;
        }
        Wiedergaenger w = MonsterTypen.WIEDERGAENGER.get().create(sl);
        if (w == null) {
            return;
        }
        w.moveTo(p.getX(), p.getY(), p.getZ(), p.getYRot(), 0);
        w.finalizeSpawn(sl, sl.getCurrentDifficultyAt(p.blockPosition()), MobSpawnType.EVENT, null);
        w.entityData.set(VORBILD, Optional.of(p.getUUID()));
        w.setCustomName(Component.translatable("monster.deadzonemc.wiedergaenger.name", p.getName()).withStyle(ChatFormatting.DARK_RED));
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR || slot == EquipmentSlot.MAINHAND) {
                w.setItemSlot(slot, p.getItemBySlot(slot).copy());
                w.setDropChance(slot, 0F);
            }
        }
        w.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(40);
        w.setHealth(40);
        sl.addFreshEntity(w);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        vorbild().ifPresent(u -> tag.putUUID("Vorbild", u));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("Vorbild")) {
            entityData.set(VORBILD, Optional.of(tag.getUUID("Vorbild")));
        }
    }

    @Override
    public boolean removeWhenFarAway(double abstand) {
        return false;
    }
}
