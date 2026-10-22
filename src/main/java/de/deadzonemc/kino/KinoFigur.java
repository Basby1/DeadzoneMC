package de.deadzonemc.kino;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.UUID;

/**
 * Ein Spielercharakter im Intro (oder die unsichtbare Kamera). Bewegt sich nicht selbst – das Drehbuch
 * (ClientKino) setzt jede Tick Position, Haltung und Drehung.
 */
public class KinoFigur extends LivingEntity {

    public static final int STEHEN = 0, SITZEN = 1, SCHWEBEN = 2;

    public UUID besitzer;
    public boolean kamera;
    public int haltung = SITZEN;
    /** Seitliches Kippen (Grad) – für das Treiben in der Schwerelosigkeit. */
    public float roll, rollO;
    /** Vor- und Zurückkippen des ganzen Körpers (Grad). */
    public float kippen, kippenO;
    /** 0..1: Arme ausbreiten (Schwerelosigkeit, Schreck). */
    public float arme, armeO;

    public KinoFigur(EntityType<? extends LivingEntity> typ, Level level) {
        super(typ, level);
        setNoGravity(true);
        noPhysics = true;
    }

    @Override
    public void tick() {
        rollO = roll;
        kippenO = kippen;
        armeO = arme;
        baseTick(); // keine Physik, keine KI – das Drehbuch bewegt alles
    }

    @Override
    public Iterable<ItemStack> getArmorSlots() {
        return List.of();
    }

    @Override
    public ItemStack getItemBySlot(EquipmentSlot slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public void setItemSlot(EquipmentSlot slot, ItemStack stack) {
    }

    @Override
    public HumanoidArm getMainArm() {
        return HumanoidArm.RIGHT;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean shouldShowName() {
        return false;
    }
}
