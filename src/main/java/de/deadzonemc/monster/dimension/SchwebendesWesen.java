package de.deadzonemc.monster.dimension;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

/** Dimensionswesen, das schwebt (Spuk, Schwebender, Leerläufer, Wachkugel, Konglomerat, Leerenaal, Hologeist). */
public class SchwebendesWesen extends Dimensionswesen {

    public SchwebendesWesen(EntityType<? extends Monster> typ, Level level, DimArt dim) {
        super(typ, level, dim);
        this.moveControl = new FlyingMoveControl(this, 12, true);
        setNoGravity(true);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation n = new FlyingPathNavigation(this, level);
        n.setCanOpenDoors(false);
        n.setCanFloat(true);
        n.setCanPassDoors(true);
        return n;
    }

    @Override
    public boolean causeFallDamage(float a, float b, net.minecraft.world.damagesource.DamageSource q) {
        return false;
    }
}
