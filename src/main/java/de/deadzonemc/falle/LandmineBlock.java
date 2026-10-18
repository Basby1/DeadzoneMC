package de.deadzonemc.falle;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Landmine: explodiert unter Infizierten und unvorsichtigen Spielern. Zerstört keine Blöcke. */
public class LandmineBlock extends Block {

    private static final VoxelShape FORM = Block.box(4, 0, 4, 12, 2, 12);

    public LandmineBlock(Properties eigenschaften) {
        super(eigenschaften);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return FORM;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction richtung, BlockState nachbar, LevelAccessor level,
                                     BlockPos pos, BlockPos nachbarPos) {
        return !canSurvive(state, level, pos) ? Blocks.AIR.defaultBlockState()
                : super.updateShape(state, richtung, nachbar, level, pos, nachbarPos);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity e) {
        if (level.isClientSide || !(e instanceof LivingEntity)) {
            return;
        }
        boolean ausloesen = e instanceof Enemy || e instanceof Player p && !p.isCreative() && !p.isShiftKeyDown();
        if (!ausloesen) {
            return;
        }
        level.removeBlock(pos, false);
        level.playSound(null, pos, SoundEvents.TRIPWIRE_CLICK_ON, SoundSource.BLOCKS, 1.0F, 0.5F);
        level.explode(null, pos.getX() + 0.5, pos.getY() + 0.2, pos.getZ() + 0.5, 3.0F, Level.ExplosionInteraction.NONE);
    }
}
