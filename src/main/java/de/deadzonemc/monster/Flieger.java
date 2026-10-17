package de.deadzonemc.monster;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/** Gemeinsame Grundlage fliegender Gegner (Drohne, Motte): schweben, fliegen, kein Fallschaden. */
public abstract class Flieger extends DeadzoneMonster {

    protected Flieger(EntityType<? extends Monster> typ, Level level, String art) {
        super(typ, level, art);
        this.moveControl = new FlyingMoveControl(this, 20, true);
        setNoGravity(true);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation nav = new FlyingPathNavigation(this, level);
        nav.setCanOpenDoors(false);
        nav.setCanFloat(true);
        nav.setCanPassDoors(true);
        return nav;
    }

    @Override
    protected boolean hatLauf() {
        return false;
    }

    @Override
    public boolean causeFallDamage(float distanz, float faktor, net.minecraft.world.damagesource.DamageSource quelle) {
        return false;
    }

    @Override
    protected void checkFallDamage(double y, boolean amBoden, net.minecraft.world.level.block.state.BlockState zustand, BlockPos pos) {
    }

    /** Ein Ziel in der Luft: {@code hoehe} Blöcke über dem Boden bei (x, z). */
    protected Vec3 luftpunkt(double x, double z, int hoehe) {
        int boden = level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(x), (int) Math.floor(z));
        return new Vec3(x, boden + hoehe, z);
    }

    protected void fliegeZu(Vec3 ziel, double tempo) {
        getMoveControl().setWantedPosition(ziel.x, ziel.y, ziel.z, tempo);
    }
}
