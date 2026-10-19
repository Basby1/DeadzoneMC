package de.deadzonemc.gadget;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/** Laufzeit und Wirkung des Störsenders. */
public class StoersenderBlockEntity extends BlockEntity {

    private int rest;
    boolean techniker;

    public StoersenderBlockEntity(BlockPos pos, BlockState s) {
        super(Gadgets.STOERSENDER_BE.get(), pos, s);
    }

    public void laden(int ticks) {
        rest = Math.min(rest + ticks, 36000);
        setChanged();
    }

    public int restMinuten() {
        return (rest + 1199) / 1200;
    }

    public static void tick(Level level, BlockPos pos, BlockState s, StoersenderBlockEntity be) {
        boolean an = be.rest > 0;
        if (s.getValue(StoersenderBlock.AN) != an) {
            level.setBlock(pos, s.setValue(StoersenderBlock.AN, an), Block.UPDATE_ALL);
        }
        if (!an) {
            return;
        }
        be.rest--;
        if (be.rest % 20 != 0 || !(level instanceof ServerLevel sl)) {
            return;
        }
        be.setChanged();
        double r = be.techniker ? 18 : 12;
        for (Mob m : sl.getEntitiesOfClass(Mob.class, new AABB(pos).inflate(r, 8, r), e -> e instanceof Enemy && e.isAlive())) {
            String id = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(m.getType()).getPath();
            if (id.equals("spaeherdrohne") || id.equals("wachkugel")) {
                m.setNoGravity(false);
                m.hurt(sl.damageSources().lightningBolt(), 3F);
            }
            if (m.getTarget() != null && sl.random.nextFloat() < 0.35F && !(m instanceof de.deadzonemc.monster.boss.Boss)) {
                m.setTarget(null);
                m.getNavigation().stop();
            }
        }
        if (be.rest % 60 == 0) {
            sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 6, 0.2, 0.2, 0.2, 0.05);
            sl.playSound(null, pos, SoundEvents.BEACON_AMBIENT, SoundSource.BLOCKS, 0.3F, 1.8F);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider reg) {
        super.saveAdditional(tag, reg);
        tag.putInt("Rest", rest);
        tag.putBoolean("Techniker", techniker);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider reg) {
        super.loadAdditional(tag, reg);
        rest = tag.getInt("Rest");
        techniker = tag.getBoolean("Techniker");
    }
}
