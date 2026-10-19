package de.deadzonemc.falle;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Brandgraben: eine mit Brennstoff gefüllte Rinne. Mit Feuerzeug oder Feuerkugel angezündet, brennt sie 45
 * Sekunden lang und setzt alles in Brand, was hindurchläuft. Danach muss man sie neu entzünden.
 */
public class BrandgrabenBlock extends Block {

    public static final BooleanProperty BRENNT = BlockStateProperties.LIT;
    private static final VoxelShape FORM = Block.box(0, 0, 0, 16, 6, 16);
    private static final int DAUER = 900;

    public BrandgrabenBlock(Properties eigenschaften) {
        super(eigenschaften);
        registerDefaultState(stateDefinition.any().setValue(BRENNT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(BRENNT);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return FORM;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player p,
                                              InteractionHand hand, BlockHitResult treffer) {
        if (state.getValue(BRENNT) || !(stack.is(Items.FLINT_AND_STEEL) || stack.is(Items.FIRE_CHARGE))) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide) {
            anzuenden(level, pos, state);
            // benachbarte Gräben fangen mit Feuer (Kette, höchstens 32 Blöcke)
            java.util.ArrayDeque<BlockPos> offen = new java.util.ArrayDeque<>();
            java.util.Set<BlockPos> gesehen = new java.util.HashSet<>();
            offen.add(pos);
            gesehen.add(pos);
            while (!offen.isEmpty() && gesehen.size() < 32) {
                BlockPos q = offen.poll();
                for (var d : net.minecraft.core.Direction.Plane.HORIZONTAL) {
                    BlockPos n = q.relative(d);
                    BlockState s = level.getBlockState(n);
                    if (gesehen.add(n) && s.getBlock() == this && !s.getValue(BRENNT)) {
                        anzuenden(level, n, s);
                        offen.add(n);
                    }
                }
            }
            if (stack.is(Items.FLINT_AND_STEEL)) {
                stack.hurtAndBreak(1, p, net.minecraft.world.entity.LivingEntity.getSlotForHand(hand));
            } else if (!p.isCreative()) {
                stack.shrink(1);
            }
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    private void anzuenden(Level level, BlockPos pos, BlockState state) {
        level.setBlock(pos, state.setValue(BRENNT, true), Block.UPDATE_ALL);
        level.scheduleTick(pos, this, DAUER);
        level.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.8F, 1.0F);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource r) {
        if (state.getValue(BRENNT)) {
            level.setBlock(pos, state.setValue(BRENNT, false), Block.UPDATE_ALL);
            level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.6F, 1.0F);
        }
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity e) {
        if (state.getValue(BRENNT) && e instanceof LivingEntity le && !e.fireImmune()
                && !(e instanceof Player p && (p.isCreative() || p.isSpectator()))) {
            le.igniteForSeconds(5);
            if (!level.isClientSide && le.tickCount % 20 == 0) {
                le.hurt(level.damageSources().inFire(), 1.5F);
            }
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource r) {
        if (state.getValue(BRENNT)) {
            for (int i = 0; i < 2; i++) {
                level.addParticle(ParticleTypes.FLAME, pos.getX() + r.nextDouble(), pos.getY() + 0.4, pos.getZ() + r.nextDouble(), 0, 0.03, 0);
            }
            if (r.nextInt(4) == 0) {
                level.addParticle(ParticleTypes.LARGE_SMOKE, pos.getX() + r.nextDouble(), pos.getY() + 0.5, pos.getZ() + r.nextDouble(), 0, 0.04, 0);
            }
            if (r.nextInt(24) == 0) {
                level.playLocalSound(pos, SoundEvents.FIRE_AMBIENT, SoundSource.BLOCKS, 0.6F, 1.0F, false);
            }
        }
    }
}
