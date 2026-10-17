package de.deadzonemc.strahlung;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Vector3f;

/** Ein leckendes Fass mit strahlendem Abfall. Leuchtet schwach grün; in seiner Nähe steigt die Strahlung. */
public class StrahlenfassBlock extends Block {

    private static final VoxelShape FORM = Block.box(2, 0, 2, 14, 15, 14);
    private static final DustParticleOptions GRUEN = new DustParticleOptions(new Vector3f(0.45F, 1.0F, 0.2F), 0.9F);

    public StrahlenfassBlock(Properties eigenschaften) {
        super(eigenschaften);
    }

    @Override
    protected VoxelShape getShape(BlockState zustand, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return FORM;
    }

    @Override
    public void animateTick(BlockState zustand, Level level, BlockPos pos, RandomSource zufall) {
        if (zufall.nextInt(3) == 0) {
            level.addParticle(GRUEN, pos.getX() + 0.2 + zufall.nextDouble() * 0.6, pos.getY() + 1.0,
                    pos.getZ() + 0.2 + zufall.nextDouble() * 0.6, 0, 0.02, 0);
        }
    }
}
