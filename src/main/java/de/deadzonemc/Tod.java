package de.deadzonemc;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

/**
 * Wer stirbt, verliert nichts endgültig: Rucksack und Inventar bleiben als Leiche am Todesort liegen
 * (Mod „Corpse“). Was man am Körper getragen hat, ist danach aber beschädigt.
 */
public final class Tod {

    private Tod() {
    }

    public static void gestorben(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || p.level().isClientSide) {
            return;
        }
        var z = p.getRandom();
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack s = p.getItemBySlot(slot);
            if (s.isEmpty() || !s.isDamageableItem()) {
                continue;
            }
            int max = s.getMaxDamage();
            int zusatz = (int) (max * (0.10 + z.nextDouble() * 0.15));
            s.setDamageValue(Math.min(max - 1, s.getDamageValue() + zusatz));
        }
    }
}
