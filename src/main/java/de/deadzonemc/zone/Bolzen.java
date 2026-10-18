package de.deadzonemc.zone;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;

/**
 * Ein geworfener Bolzen. Fliegt er in oder neben eine Anomalie, löst er sie aus – so findet man einen
 * sicheren Weg. Der Bolzen bleibt liegen und kann wieder aufgehoben werden.
 */
public class Bolzen extends ThrowableItemProjectile {

    public Bolzen(EntityType<? extends Bolzen> typ, Level level) {
        super(typ, level);
    }

    public Bolzen(Level level, LivingEntity werfer) {
        super(Zone.BOLZEN_TYP.get(), werfer, level);
    }

    @Override
    protected Item getDefaultItem() {
        return Zone.BOLZEN.get();
    }

    @Override
    public void tick() {
        super.tick();
        if (level() instanceof ServerLevel sl && tickCount % 2 == 0) {
            pruefen(sl, false);
        }
    }

    /** Löst Anomalien im Umkreis aus. */
    private boolean pruefen(ServerLevel sl, boolean ende) {
        BlockPos hier = blockPosition();
        boolean gefunden = false;
        for (BlockPos b : BlockPos.betweenClosed(hier.offset(-2, -2, -2), hier.offset(2, 2, 2))) {
            if (sl.getBlockEntity(b) instanceof AnomalieBlockEntity a) {
                double d = b.distToCenterSqr(position());
                double reichweite = a.art() == AnomalieArt.STRUDEL ? 6.0 : 3.0;
                if (d < reichweite || ende && d < reichweite * 1.6) {
                    a.ausloesen(sl, true);
                    gefunden = true;
                }
            }
        }
        if (gefunden && !ende) {
            liegenLassen(sl);
        }
        return gefunden;
    }

    @Override
    protected void onHit(HitResult treffer) {
        super.onHit(treffer);
        if (level() instanceof ServerLevel sl && !isRemoved()) {
            pruefen(sl, true);
            liegenLassen(sl);
        }
    }

    private void liegenLassen(ServerLevel sl) {
        if (isRemoved()) {
            return;
        }
        ItemEntity item = new ItemEntity(sl, getX(), getY() + 0.1, getZ(), new ItemStack(Zone.BOLZEN.get()));
        item.setDeltaMovement(0, 0.1, 0);
        sl.addFreshEntity(item);
        discard();
    }
}
