package de.deadzonemc.falle;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Stacheldraht: bremst stark und reißt Wunden – für Infizierte wie für Unvorsichtige. */
public class StacheldrahtBlock extends Block {

    private static final VoxelShape FORM = Block.box(1, 0, 1, 15, 12, 15);

    public StacheldrahtBlock(Properties eigenschaften) {
        super(eigenschaften);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return FORM;
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity e) {
        if (!(e instanceof LivingEntity le) || e instanceof Player p && p.isCreative()) {
            return;
        }
        e.makeStuckInBlock(state, new Vec3(0.3, 0.5, 0.3));
        if (!level.isClientSide && le.tickCount % 15 == 0) {
            le.hurt(level.damageSources().sweetBerryBush(), le instanceof Player ? 1.0F : 2.5F);
        }
    }
}
