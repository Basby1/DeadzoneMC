package de.deadzonemc.bossmod;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Was am Trophäenbrett hängt. */
public class TrophaeenbrettBlockEntity extends BlockEntity {

    private ItemStack ding = ItemStack.EMPTY;

    public TrophaeenbrettBlockEntity(BlockPos pos, BlockState s) {
        super(Bossmods.BRETT_BE.get(), pos, s);
    }

    public ItemStack ding() {
        return ding;
    }

    public void setzen(ItemStack s) {
        ding = s;
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider reg) {
        super.saveAdditional(tag, reg);
        if (!ding.isEmpty()) {
            tag.put("Ding", ding.save(reg));
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider reg) {
        super.loadAdditional(tag, reg);
        ding = tag.contains("Ding") ? ItemStack.parse(reg, tag.getCompound("Ding")).orElse(ItemStack.EMPTY) : ItemStack.EMPTY;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider reg) {
        return saveWithoutMetadata(reg);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
