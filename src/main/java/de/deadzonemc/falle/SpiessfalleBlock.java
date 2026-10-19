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

/**
 * Spießgrube: angespitzte Pflöcke. Wer hineinläuft, wird gebremst und verletzt; wer hineinfällt (Grube darüber
 * ausheben!), nimmt schweren Schaden.
 */
public class SpiessfalleBlock extends Block {

    private static final VoxelShape FORM = Block.box(1, 0, 1, 15, 10, 15);

    public SpiessfalleBlock(Properties eigenschaften) {
        super(eigenschaften);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return FORM;
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity e) {
        if (!(e instanceof LivingEntity le) || e instanceof Player p && (p.isCreative() || p.isSpectator())) {
            return;
        }
        e.makeStuckInBlock(state, new Vec3(0.45, 0.7, 0.45));
        if (!level.isClientSide && le.tickCount % 20 == 0) {
            le.hurt(level.damageSources().cactus(), le instanceof Player ? 1.5F : 3.0F);
        }
    }

    @Override
    public void fallOn(Level level, BlockState state, BlockPos pos, Entity e, float hoehe) {
        if (!level.isClientSide && e instanceof LivingEntity le && hoehe > 1.5F) {
            le.hurt(level.damageSources().cactus(), Math.min(20F, hoehe * 2.5F));
        }
        super.fallOn(level, state, pos, e, hoehe);
    }
}
