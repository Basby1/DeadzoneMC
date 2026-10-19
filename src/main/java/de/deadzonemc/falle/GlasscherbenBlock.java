package de.deadzonemc.falle;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Glasscherben als Lärmmelder: Wer darüberläuft, knirscht hörbar – Infizierte schneiden sich, und das Knirschen
 * lockt andere an (also lieber vor die Basis legen als hinein). Wer schleicht, kommt leise darüber.
 */
public class GlasscherbenBlock extends Block {

    private static final VoxelShape FORM = Block.box(0, 0, 0, 16, 1, 16);

    public GlasscherbenBlock(Properties eigenschaften) {
        super(eigenschaften);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return FORM;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), net.minecraft.core.Direction.UP);
    }

    @Override
    protected BlockState updateShape(BlockState state, net.minecraft.core.Direction richtung, BlockState nachbar,
                                     net.minecraft.world.level.LevelAccessor level, BlockPos pos, BlockPos nachbarPos) {
        return richtung == net.minecraft.core.Direction.DOWN && !canSurvive(state, level, pos)
                ? net.minecraft.world.level.block.Blocks.AIR.defaultBlockState()
                : super.updateShape(state, richtung, nachbar, level, pos, nachbarPos);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity e) {
        if (!(level instanceof ServerLevel sl) || !(e instanceof LivingEntity le) || le.tickCount % 12 != 0) {
            return;
        }
        if (e instanceof Player p && (p.isCreative() || p.isSpectator() || p.isShiftKeyDown())) {
            return;
        }
        Vec3 v = e.getDeltaMovement();
        if (v.x * v.x + v.z * v.z < 0.0004) {
            return;
        }
        sl.playSound(null, pos, SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 0.5F, 1.6F + sl.random.nextFloat() * 0.3F);
        if (e instanceof Enemy) {
            le.hurt(sl.damageSources().cactus(), 1.0F);
        }
        de.deadzonemc.monster.Laerm.machen(sl, Vec3.atCenterOf(pos), 18);
    }
}
